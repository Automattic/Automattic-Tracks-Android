package com.automattic.android.tracks;

import android.util.Log;

import com.automattic.android.tracks.Exceptions.EventNameException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Event implements Serializable {

    public static final String LOGTAG = "NosaraEvent";

    private final String mEventName;
    private final String mUser;
    private final String mUserAgent;
    private final long mTimeStamp;
    private final TracksClient.NosaraUserType mUserType;

    private int mRetryCount = 0;

    private JSONObject mUserProperties;
    private JSONObject mDeviceInfo;
    private JSONObject mCustomEventProps;

    public Event(String mEventName, String userID, TracksClient.NosaraUserType uType,
                 String userAgent, long timeStamp) throws EventNameException {
        checkEventName(mEventName);
        this.mEventName = mEventName;
        this.mUser = userID;
        this.mUserType = uType;
        this.mUserAgent = userAgent;
        this.mTimeStamp = timeStamp;
    }

    private void checkEventName(String name) throws EventNameException {
        if (name.contains("-")) {
            String errorMessage = "Event name must not contains dashes.";
            throw new EventNameException(errorMessage);
        }

        if (StringUtils.containsWhiteSpace(name)) {
            throw new EventNameException("Event name must not contains whitespace.");
        }

        if (!name.matches("^[a-z_][a-z0-9_]*$")) {
            throw new EventNameException("Event name must match: ^[a-z_][a-z0-9_]*$");
        }
    }

    public String getEventName() {
        return mEventName;
    }

    public String getUser() {
        return mUser;
    }

    public TracksClient.NosaraUserType getUserType() {
        return mUserType;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public int getRetryCount() {
        return mRetryCount;
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    public void addRetryCount() {
        mRetryCount+= 1;
    }

    public void setUserProperties(JSONObject userProperties) {
        this.mUserProperties = userProperties;
    }

    public void setDeviceInfo(JSONObject deviceInfo) {
        this.mDeviceInfo = deviceInfo;
    }

    public JSONObject getUserProperties() {
        return mUserProperties;
    }

    public JSONObject getDeviceInfo() {
        return mDeviceInfo;
    }

    public JSONObject getCustomEventProperties() {
        return mCustomEventProps;
    }

    public boolean addCustomEventProperty(String key, Object value) {
        if (StringUtils.isBlank(key)) {
            Log.e(LOGTAG, "Cannot add a property that has an empty key to the event");
            return false;
        }
        if (this.mCustomEventProps == null) {
            mCustomEventProps = new JSONObject();
        }

        // Emit a warning if the property isn't lowercase.
        if (!key.toLowerCase().equals(key)) {
            Log.w(LOGTAG, "Properties should have lowercase name: "+ key);
        }

        if (MessageBuilder.isReservedKeyword(key)) {
            Log.e(LOGTAG, "Cannot add the property: " + key + " to the event. It's a reserved keyword.");
            return false;
        }
        try {
            String valueString;
            if (value != null) {
                valueString = String.valueOf(value);
            } else {
                valueString = "";
            }
            mCustomEventProps.put(key, valueString);
        } catch (JSONException e) {
            Log.e(LOGTAG, "Cannot add the property to the event");
            return false;
        }
        return true;
    }
}
