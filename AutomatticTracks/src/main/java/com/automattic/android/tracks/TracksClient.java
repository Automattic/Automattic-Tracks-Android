package com.automattic.android.tracks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.automattic.android.tracks.Exceptions.EventNameException;
import com.automattic.android.tracks.datasets.EventTable;
import com.automattic.android.tracks.datasets.TracksDatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TracksClient {
    public static final String LOGTAG = "NosaraClient";

    public static final String LIB_VERSION = "0.0.1";
    protected static final String DEFAULT_USER_AGENT = "Nosara Client for Android";
    protected static final String NOSARA_REST_API_ENDPOINT_URL_V1_1 = "https://public-api.wordpress.com/rest/v1.1/";
    protected static final int DEFAULT_EVENTS_QUEUE_THREESHOLD = 10;


    public static enum NosaraUserType {ANON, WPCOM}

    /**
     * Socket timeout in milliseconds for rest requests
     */
    public static final int REST_TIMEOUT_MS = 30000;

    /**
     * Default number of retries for POST rest requests
     */
    public static final int REST_MAX_RETRIES_POST = 0;

    /**
     * Default number of retries for GET rest requests
     */
    public static final int REST_MAX_RETRIES_GET = 3;

    /**
     * Default backoff multiplier for rest requests
     */
    public static final float REST_BACKOFF_MULT = 2f;

    private final Context mContext;

    private String mUserAgent = TracksClient.DEFAULT_USER_AGENT;
    private final RequestQueue mQueue;
    private final TracksDatabaseHelper mDatabaseHelper;
    private String mRestApiEndpointURL;
    private DeviceInformation deviceInformation;
    private JSONObject mUserProperties = new JSONObject();

    // This is the main queue of events we need to lazy-write to the database
    private final LinkedList<Event> mInsertEventsQueue = new LinkedList<>();
    // Database monitor
    private final static Object mDbLock = new Object();

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
        deviceInformation = new DeviceInformation(ctx);

        // This is the thread that read from the "fast" input events queue and actually write data to the DB.
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

        // This is the thread that read from the DB and enqueue the request to Volley
        Thread sendToWireThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (mDbLock) {
                        try {
                            if ((mPendingFlush || EventTable.getEventsCount(mContext) > DEFAULT_EVENTS_QUEUE_THREESHOLD)
                                    && NetworkUtils.isNetworkAvailable(mContext)) {
                                sendRequests();
                            }
                            mDbLock.wait();
                        } catch (InterruptedException err) {
                            Log.e(LOGTAG, "Something went wrong while waiting on the database lock", err);
                        }
                    }
                }
            }
        });
        sendToWireThread.setPriority(Thread.MIN_PRIORITY);
        sendToWireThread.start();
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

    private void sendRequests() {
        if (!NetworkUtils.isNetworkAvailable(mContext)) {
            return;
        }
        synchronized (mDbLock) {
            mPendingFlush = false; // We can remove the flushing flag now.
            if (!EventTable.hasEvents(mContext)) {
                return;
            }
            try {
                JSONArray events = new JSONArray();
                LinkedList<Event> currentEventsList = new LinkedList<>(); // events we're sending on the wire

                // Create common props here. Then check later at "single event" layer if one of these props changed.
                JSONObject commonProps = MessageBuilder.createRequestCommonPropsJSONObject(deviceInformation, mUserProperties);

                List<Event> newEventsList = EventTable.getAndDeleteEvents(mContext, 0);

                // Create single event obj here
                for (Event singleEvent : newEventsList) {
                    JSONObject singleEventJSON = MessageBuilder.createEventJSONObject(singleEvent, commonProps);
                    if (singleEventJSON != null) {
                        events.put(singleEventJSON);
                        currentEventsList.add(singleEvent);
                    }
                }

                if (currentEventsList.size() > 0) {
                    JSONObject requestJSONObject = new JSONObject();
                    requestJSONObject.put("events", events);
                    requestJSONObject.put("commonProps", commonProps);
                    String path = "tracks/record";
                    NosaraRestListener nosaraRestListener = new NosaraRestListener(currentEventsList);
                    RestRequest request = post(path, requestJSONObject, nosaraRestListener, nosaraRestListener);
                    request.setShouldCache(false); // do not cache
                    mQueue.add(request);
                }
            } catch (JSONException err) {
                Log.e(LOGTAG, "Exception creating the request JSON object", err);
                return;
            } catch (Exception e) {
                Log.e(LOGTAG, "Exception creating the request JSON object", e);
            }
        }
    }

    /*
    public NosaraClient(Context ctx, String endpointURL) {
        this(ctx);
        mRestApiEndpointURL = endpointURL;
    }
*/

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


    /* private NosaraRestRequest get(String path, Listener<JSONObject> listener, ErrorListener errorListener) {
         return makeRequest(Method.GET, getAbsoluteURL(path), null, listener, errorListener);
     }
 */
    private RestRequest post(String path, JSONObject jsonRequest, Listener<JSONObject> listener, ErrorListener errorListener) {
        return this.post(path, jsonRequest, null, listener, errorListener);
    }

    private RestRequest post(final String path, JSONObject jsonRequest, RetryPolicy retryPolicy, Listener<JSONObject> listener, ErrorListener errorListener) {
        final RestRequest request = makeRequest(Method.POST, getAbsoluteURL(path), jsonRequest, listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST,
                    REST_BACKOFF_MULT); //Do not retry on failure
        }
        request.setRetryPolicy(retryPolicy);
        return request;
    }

    private RestRequest makeRequest(int method, String url, JSONObject jsonRequest, Listener<JSONObject> listener,
                                          ErrorListener errorListener) {
        RestRequest request = new RestRequest(method, url, jsonRequest, listener, errorListener);
        request.setUserAgent(mUserAgent);
        return request;
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


    private class NosaraRestListener implements Response.Listener<JSONObject>, Response.ErrorListener {
        private final LinkedList<Event> mEventsList;  // Keep a reference to the events sent on the wire.

        public NosaraRestListener(final LinkedList<Event> eventsList) {
            this.mEventsList = eventsList;
        }

        @Override
        public void onResponse(final JSONObject response) {
            Log.d(LOGTAG, response.toString());
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            VolleyErrorHelper.logVolleyErrorDetails(volleyError);

            // TODO should we add some logic here?
            if (VolleyErrorHelper.isSocketTimeoutProblem(volleyError)) {

            } else if (VolleyErrorHelper.isServerProblem(volleyError)) {

            } else if (VolleyErrorHelper.isNetworkProblem(volleyError)) {

            }

            // Loop on events and keep those events that we must re-enqueue
            LinkedList<Event> mustKeepEventsList = new LinkedList(); // events we're re-enqueuing
            for (Event singleEvent : mEventsList) {
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

    private boolean isStillValid(Event event) {
        // Should we discard events > 5 days? See event.getTimeStamp()
        // Should we consider the retry count?
        return true;
    }
}