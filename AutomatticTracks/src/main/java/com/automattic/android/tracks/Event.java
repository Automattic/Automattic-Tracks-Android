package com.automattic.android.tracks;

import android.text.TextUtils;
import android.util.Log;

import com.automattic.android.tracks.Exceptions.EventDetailsException;
import com.automattic.android.tracks.Exceptions.EventNameException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Event implements Serializable {

    public static final String LOGTAG = "NosaraEvent";
    private static final String EVENT_NAME_REGEXP = "^(([a-z0-9]+)_){2}([a-z0-9_]+)$";
    private static final Pattern eventNameRegExpPattern = Pattern.compile(EVENT_NAME_REGEXP);

    private static final String PROPERTY_NAME_REGEXP = "^[a-z_][a-z0-9_]*$";
    private static final Pattern propertyNameRegExpPattern = Pattern.compile(PROPERTY_NAME_REGEXP);

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
                 String userAgent, long timeStamp) throws EventNameException, EventDetailsException {

        checkEventName(mEventName);
        if (TextUtils.isEmpty(userID)) {
            throw new EventDetailsException("Username cannot be empty!");
        }

        if (uType == null) {
            throw new EventDetailsException("NosaraUserType cannot be null!");
        }

        if (TextUtils.isEmpty(userAgent)) {
            Log.w(LOGTAG, "User Agent string is empty!");
        }

        this.mEventName = mEventName;
        this.mUser = userID;
        this.mUserType = uType;
        this.mUserAgent = StringUtils.notNullStr(userAgent);
        this.mTimeStamp = timeStamp;
    }

    private void checkEventName(String name) throws EventNameException {
        if (name.equals(MessageBuilder.ALIAS_USER_EVENT_NAME)) {
            // "_aliasUser is a special case. No validation on it.
            return;
        }

        if (TextUtils.isEmpty(name)) {
            throw new EventNameException("Event name must not ne empty or null");
        }

        if (name.contains("-")) {
            String errorMessage = "Event name must not contains dashes.";
            throw new EventNameException(errorMessage);
        }

        if (StringUtils.containsWhiteSpace(name)) {
            throw new EventNameException("Event name must not contains whitespace.");
        }

        Matcher matcher = eventNameRegExpPattern.matcher(name);
        if (!matcher.matches()) {
            throw new EventNameException("Event name must match: " + EVENT_NAME_REGEXP);
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
        if (!key.toLowerCase(Locale.ROOT).equals(key)) {
            Log.w(LOGTAG, "Properties should have lowercase name: "+ key);
        }

        if (key.startsWith("_")) {
            Log.e(LOGTAG, "Cannot add the property: " + key + " to the event. Leading underscores are reserved.");
            return false;
        }

        if (MessageBuilder.isReservedKeyword(key)) {
            Log.e(LOGTAG, "Cannot add the property: " + key + " to the event. It's a reserved keyword.");
            return false;
        }

        if (getEventName().equals(MessageBuilder.ALIAS_USER_EVENT_NAME) &&
                key.equals(MessageBuilder.ALIAS_USER_ANONID_PROP_NAME)) {
            // We need to exclude the validation on the property "anonId" for the event "_aliasUser".
        } else {
            Matcher matcher = propertyNameRegExpPattern.matcher(key);
            if (!matcher.matches()) {
                Log.e(LOGTAG, "Cannot add the property: " + key + " to the event. Property name must match: " + PROPERTY_NAME_REGEXP);
                return false;
            }
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

    public void setCustomProperties(JSONObject customProperties) {
        this.mCustomEventProps = customProperties;
    }
}
