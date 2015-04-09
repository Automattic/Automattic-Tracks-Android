package com.automattic.android.tracks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.automattic.android.tracks.Exceptions.EventNameException;
import com.automattic.android.tracks.datasets.EventTable;
import com.automattic.android.tracks.datasets.TracksDatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TracksClient {
    public static final String LOGTAG = "NosaraClient";

    public static final String LIB_VERSION = BuildConfig.VERSION_NAME;
    protected static final String DEFAULT_USER_AGENT = "Nosara Client for Android" + "/" + LIB_VERSION;
    protected static final String NOSARA_REST_API_ENDPOINT_URL_V1_1 = "https://public-api.wordpress.com/rest/v1.1/";
    protected static final int DEFAULT_EVENTS_QUEUE_THREESHOLD = 10;

    public static enum NosaraUserType {ANON, WPCOM}

    /**
     * Socket timeout in milliseconds for rest requests
     */
    public static final int REST_TIMEOUT_MS = 30000;

    /** Default charset for JSON request. */
    final static String PROTOCOL_CHARSET = "utf-8";

    /** Content type for request. */
    final static String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    private final Context mContext;
    private String mUserAgent = TracksClient.DEFAULT_USER_AGENT;
    private final RequestQueue mQueue;
    private final TracksDatabaseHelper mDatabaseHelper;
    private String mRestApiEndpointURL;
    private final String mTracksRestEndpointURL;
    private DeviceInformation deviceInformation;
    private JSONObject mUserProperties = new JSONObject();

    // This is the main queue of events we need to lazy-write to the database
    private final LinkedList<Event> mInsertEventsQueue = new LinkedList<>();

    // Database monitor
    private final static Object mDbLock = new Object();

    // This is the queue of events we're sending on the wire
    private final LinkedList<NetworkRequestObject> mNetworkQueue = new LinkedList<>();


    private boolean mPendingFlush = false;


    public static TracksClient getClient(Context ctx) {
        if (null == ctx || !checkBasicConfiguration(ctx)) {
            return null;
        }

        return new TracksClient(ctx);
    }

    private TracksClient(Context ctx) {
        mContext = ctx;
        mDatabaseHelper = TracksDatabaseHelper.getDatabase(ctx);
        mQueue = Volley.newRequestQueue(ctx);
        mRestApiEndpointURL = NOSARA_REST_API_ENDPOINT_URL_V1_1;
        mTracksRestEndpointURL = getAbsoluteURL("tracks/record");
        deviceInformation = new DeviceInformation(ctx);

        // This is the thread that reads from the "fast" (in-memory) input events queue and actually writes data to the DB.
        Thread bufferCopyThread = new Thread(new Runnable() {
            public void run() {
                LinkedList<Event> shadowCopyEventList = new LinkedList<>();
                while (true) {
                    // 1. copy events from the input queue to a temporary queue and release the lock over the input queue.
                    synchronized (mInsertEventsQueue) {
                        try {
                            if (mInsertEventsQueue.size() == 0) {
                                mInsertEventsQueue.wait();
                            }
                            // copy the events and release the lock asap
                            shadowCopyEventList.addAll(mInsertEventsQueue);
                            mInsertEventsQueue.clear();
                        } catch (InterruptedException err) {
                            Log.e(LOGTAG, "Something went wrong while waiting on the input queue of events", err);
                        }
                    }
                    if (shadowCopyEventList.size() > 0) {
                        //2.  get the lock over the DB and write data
                        synchronized (mDbLock) {
                            for (Event currentEvent : shadowCopyEventList) {
                                EventTable.insertEvent(mContext, currentEvent);
                            }
                            mDbLock.notifyAll();
                        }
                        shadowCopyEventList.clear();
                    }
                }
            }
        });
        bufferCopyThread.setPriority(Thread.MIN_PRIORITY);
        bufferCopyThread.start();

        // This is the thread that reads from the DB and enqueues the request to the network queue
        Thread sendToWireThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    NetworkRequestObject req = null;
                    synchronized (mDbLock) {
                        try {
                            if ((mPendingFlush || EventTable.getEventsCount(mContext) > DEFAULT_EVENTS_QUEUE_THREESHOLD)
                                    && NetworkUtils.isNetworkAvailable(mContext)) {

                                mPendingFlush = false; // We can remove the flushing flag now.
                                try {
                                    JSONArray events = new JSONArray();
                                    List<Event> eventsList = EventTable.getAndDeleteEvents(mContext, 0);

                                    if (eventsList != null && eventsList.size() > 0) {
                                        // Create common props here. Then check later at "single event" layer if one of these props changed in that event.
                                        JSONObject commonProps = MessageBuilder.createRequestCommonPropsJSONObject(deviceInformation,
                                                mUserProperties, getUserAgent());

                                        // Create single event obj here
                                        for (Event singleEvent : eventsList) {
                                            JSONObject singleEventJSON = MessageBuilder.createEventJSONObject(singleEvent, commonProps);
                                            if (singleEventJSON != null) {
                                                events.put(singleEventJSON);
                                            }
                                        }

                                        JSONObject requestJSONObject = new JSONObject();
                                        requestJSONObject.put("events", events);
                                        requestJSONObject.put("commonProps", commonProps);

                                        req = new NetworkRequestObject();
                                        req.requestObj = requestJSONObject;
                                        req.src = eventsList;
                                    }
                                } catch (JSONException err) {
                                    Log.e(LOGTAG, "Exception creating the request JSON object", err);
                                }
                            } else {
                                mDbLock.wait();
                            }
                        } catch (InterruptedException err) {
                            Log.e(LOGTAG, "Something went wrong while waiting on the database lock", err);
                        }
                    }

                    if (req != null) {
                        synchronized (mNetworkQueue) {
                            mNetworkQueue.add(req);
                            mNetworkQueue.notifyAll();
                        }
                    }
                }
            }
        });
        sendToWireThread.setPriority(Thread.MIN_PRIORITY);
        sendToWireThread.start();

        // This is the thread that sends the request to the server and wait for the response.
        // single network connection model.
        Thread networkThread = new Thread(new Runnable() {
            public void run() {

                while (true) {
                    NetworkRequestObject currentRequest = null;
                    // 1. copy request from the networkQueue queue to a temporary queue and release the lock over the network queue.
                    synchronized (mNetworkQueue) {
                        try {
                            if (mNetworkQueue.size() == 0) {
                                mNetworkQueue.wait();
                            }
                            // copy the request and release the lock asap
                            if (mNetworkQueue.size() > 0) {
                                currentRequest = mNetworkQueue.removeFirst();
                            }
                        } catch (InterruptedException err) {
                            Log.e(LOGTAG, "Something went wrong while waiting on the network queue", err);
                        }
                    }

                    if (currentRequest != null) {
                        boolean isErrorResponse = false;
                        // send the request

                        HttpURLConnection conn = null;
                        try {
                            URL requestURL = new URL(mTracksRestEndpointURL);
                            conn = (HttpURLConnection) requestURL.openConnection();
                            conn.setRequestProperty("Content-Type", PROTOCOL_CONTENT_TYPE);
                            conn.setReadTimeout(REST_TIMEOUT_MS);
                            conn.setConnectTimeout(REST_TIMEOUT_MS);
                            conn.setUseCaches(false);
                            conn.setRequestProperty("Connection", "close");
                            conn.setRequestProperty("User-Agent", getUserAgent());
                            conn.setRequestMethod("POST");
                            conn.setDoInput(true);
                            conn.setDoOutput(true);

                            //Send request
                            OutputStream wr = conn.getOutputStream ();
                            wr.write(currentRequest.requestObj.toString().getBytes(PROTOCOL_CHARSET));
                            wr.flush();
                            wr.close ();

                            // Read the request
                            int respCode = conn.getResponseCode();
                            if (respCode!= HttpURLConnection.HTTP_OK && respCode != HttpURLConnection.HTTP_ACCEPTED) {
                                isErrorResponse = true;
                                // read the response of the server in case of errors
                                InputStream is = conn.getInputStream();
                                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                                String line;
                                StringBuffer response = new StringBuffer();
                                while((line = rd.readLine()) != null) {
                                    response.append(line);
                                    response.append('\r');
                                }
                                Log.e(LOGTAG, "Server error response: " + response.toString());
                                rd.close();
                                is.close();
                            }
                        } catch (MalformedURLException e) {
                            Log.e(LOGTAG, "The REST endpoint URL is not valid!?!?! This should never happen", e);
                            isErrorResponse = true;
                        } catch (IOException e) {
                            Log.e(LOGTAG, "Error while sending the events to the server", e);
                            isErrorResponse = true;
                        } catch (Exception e) {
                            Log.e(LOGTAG, "Error while sending the events to the server", e);
                            isErrorResponse = true;
                        } finally {
                            try {
                                if (conn != null) {
                                    conn.disconnect();
                                }
                            } catch (Exception e){
                            }
                            if (isErrorResponse) {
                                // Loop on events and keep those events that we must re-enqueue
                                LinkedList<Event> mustKeepEventsList = new LinkedList<>(); // events we're re-enqueuing
                                for (Event singleEvent : currentRequest.src) {
                                    if (isStillValid(singleEvent)) {
                                        singleEvent.addRetryCount();
                                        mustKeepEventsList.add(singleEvent);
                                    }
                                }
                                if (mustKeepEventsList.size() > 0) {
                                    synchronized (mInsertEventsQueue) {
                                        mInsertEventsQueue.addAll(mustKeepEventsList);
                                        mInsertEventsQueue.notifyAll();
                                    }
                                }
                            }
                        }
                    }
                } // end-while
            }
        });
        networkThread.setPriority(Thread.NORM_PRIORITY);
        networkThread.start();
    }

    private final class NetworkRequestObject {
        JSONObject requestObj;
        List<Event> src;
    }

    private static boolean checkBasicConfiguration(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final String packageName = context.getPackageName();

        if (PackageManager.PERMISSION_GRANTED != packageManager.checkPermission("android.permission.INTERNET", packageName)) {
            Log.w(LOGTAG, "Package does not have permission android.permission.INTERNET - Nosara Client will not work at all!");
            Log.i(LOGTAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n" +
                    "<uses-permission android:name=\"android.permission.INTERNET\" />");
            return false;
        }

        if (PackageManager.PERMISSION_GRANTED != packageManager.checkPermission("android.permission.ACCESS_NETWORK_STATE", packageName)) {
            Log.w(LOGTAG, "Package does not have permission android.permission.ACCESS_NETWORK_STATE - Nosara Client will not work at all!");
            Log.i(LOGTAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n" +
                    "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />");
            return false;
        }

        return true;
    }

    public void registerUserProperties(JSONObject props) {
        this.mUserProperties = props;
    }

    public void clearUserProperties() {
        this.mUserProperties = null;
    }

    public void flush() {
        // we need to get the lock over the DB to awake the writing thread, and to be sure the flush is done asap.
        // Since we need the lock over the DB is better to do that in a thread. Otherwise the caller should wait until the DB is ready.
        Thread flushingThread = new Thread(new Runnable() {
            public void run() {
                synchronized (mDbLock) {
                    mPendingFlush = true;
                    mDbLock.notifyAll();
                }
            }
        });
        flushingThread.setPriority(Thread.MIN_PRIORITY);
        flushingThread.start();
    }



    public void track(String eventName, String user, NosaraUserType userType) {
        this.track(eventName, null, user, userType);
    }

    public void track(String eventName, JSONObject customProps, String user, NosaraUserType userType) {
        Event event = null;
        try {
            event = new Event(
                    eventName,
                    user,
                    userType,
                    getUserAgent(),
                    System.currentTimeMillis()
            );
        } catch (EventNameException e) {
            Log.e(LOGTAG, "Cannot create the event: " +eventName, e);
            return;
        }

        JSONObject deviceInfo = deviceInformation.getMutableDeviceInfo();
        if (deviceInfo != null && deviceInfo.length() > 0) {
            event.setDeviceInfo(deviceInfo);
        }

        if (mUserProperties != null && mUserProperties.length() > 0) {
            event.setUserProperties(mUserProperties);
        }

        if (customProps != null && customProps.length() > 0) {
            Iterator<String> iter = customProps.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    Object value = customProps.get(key);
                    event.addCustomEventProperty(key, value);
                } catch (JSONException e) {
                    Log.e(LOGTAG, "Cannot add the property '" + key + "' to the event");
                }
            }
        }

        // Write in the insertEventQueue and notify the thread that actually write the data to the DB
        synchronized (mInsertEventsQueue) {
            mInsertEventsQueue.add(event);
            mInsertEventsQueue.notify();
        }
    }


    public void trackAliasUser(String user, String anonUser) {
        JSONObject customProps = new JSONObject();
        try {
            customProps.put("anonId", anonUser);
        } catch (JSONException e) {
            Log.e(LOGTAG, "Cannot track _aliasUser with the following anonUser " + anonUser);
            return;
        }

        track(MessageBuilder.ALIAS_USER_EVENT_NAME, customProps, user, TracksClient.NosaraUserType.WPCOM);
    }


    private String getAbsoluteURL(String url) {
        // if it already starts with our endpoint, let it pass through
        if (url.indexOf(mRestApiEndpointURL) == 0) {
            return url;
        }
        // if it has a leading slash, remove it
        if (url.indexOf("/") == 0) {
            url = url.substring(1);
        }
        // prepend the endpoint
        return String.format("%s%s", mRestApiEndpointURL, url);
    }

    //Sets the User-Agent header to be sent with each future request.
    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    private boolean isStillValid(Event event) {
        // Should we discard events > 5 days? See event.getTimeStamp()
        // Should we consider the retry count?
        return true;
    }
}