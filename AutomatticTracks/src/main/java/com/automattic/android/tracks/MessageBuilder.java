package com.automattic.android.tracks;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

class MessageBuilder {

    private static final String USER_INFO_PREFIX = "user_info_";
    private static final String DEVICE_INFO_PREFIX = "device_info_";

    private static final String EVENT_NAME_KEY = "_en";
    private static final String USER_AGENT_NAME_KEY = "_via_ua";
    private static final String EVENT_TIMESTAMP_KEY = "_ts";
    private static final String REQUEST_TIMESTAMP_KEY = "_rt";
    private static final String USER_TYPE_KEY = "_ut";
    private static final String USER_TYPE_ANON= "anon";
    private static final String USER_ID_KEY = "_ui";
    private static final String USER_LOGIN_NAME_KEY = "_ul";
    private static final String DEVICE_HEIGHT_PIXELS_KEY = "_ht";
    private static final String DEVICE_WIDTH_PIXELS_KEY = "_wd";
    private static final String DEVICE_LANG_KEY = "_lg";

    public static final String ALIAS_USER_EVENT_NAME = "_aliasUser";
    public static final String ALIAS_USER_ANONID_PROP_NAME = "anonId";

    public static synchronized boolean isReservedKeyword(String keyToTest) {
        String keyToTestLowercase = keyToTest.toLowerCase();
        if (keyToTestLowercase.equals(EVENT_NAME_KEY) ||
                keyToTestLowercase.equals(USER_AGENT_NAME_KEY) ||
                keyToTestLowercase.equals(EVENT_TIMESTAMP_KEY) ||
                keyToTestLowercase.equals(REQUEST_TIMESTAMP_KEY) ||
                keyToTestLowercase.equals(USER_TYPE_KEY) ||
                keyToTestLowercase.equals(USER_ID_KEY) ||
                keyToTestLowercase.equals(DEVICE_WIDTH_PIXELS_KEY) ||
                keyToTestLowercase.equals(DEVICE_HEIGHT_PIXELS_KEY) ||
                keyToTestLowercase.equals(DEVICE_LANG_KEY) ||
                keyToTestLowercase.equals(USER_LOGIN_NAME_KEY)
                ) {
            return true;
        }

        if (keyToTestLowercase.startsWith(USER_INFO_PREFIX) ||
                keyToTestLowercase.startsWith(DEVICE_INFO_PREFIX)) {
            return true;
        }

        return false;
    }

    public static synchronized JSONObject createRequestCommonPropsJSONObject(DeviceInformation deviceInformation,
                                                                             JSONObject userProperties,
                                                                             String userAgent) {
        JSONObject commonProps = new JSONObject();
        try {
            commonProps.put(USER_AGENT_NAME_KEY, userAgent);
        } catch (JSONException e) {
            Log.e(TracksClient.LOGTAG, "Cannot add the "+  USER_AGENT_NAME_KEY + " property to request commons.");
        }

        try {
            commonProps.put(DEVICE_WIDTH_PIXELS_KEY, deviceInformation.getDeviceWidthPixels());
            commonProps.put(DEVICE_HEIGHT_PIXELS_KEY, deviceInformation.getDeviceHeightPixels());
        } catch (JSONException e) {
            Log.e(TracksClient.LOGTAG, "Cannot add the device width/height properties to request commons.");
        }

        try {
            commonProps.put(DEVICE_LANG_KEY, deviceInformation.getDeviceLanguage());
        } catch (JSONException e) {
            Log.e(TracksClient.LOGTAG, "Cannot add the device language property to request commons.");
        }

        unfolderProperties(deviceInformation.getImmutableDeviceInfo(), DEVICE_INFO_PREFIX, commonProps);
        unfolderProperties(deviceInformation.getMutableDeviceInfo(), DEVICE_INFO_PREFIX, commonProps);
        unfolderProperties(userProperties, USER_INFO_PREFIX, commonProps);
        try {
            commonProps.put(REQUEST_TIMESTAMP_KEY, System.currentTimeMillis());
        } catch (JSONException e) {
            Log.e(TracksClient.LOGTAG, "Cannot add the _rt property to the request." +
                    " Current batch request will be discarded on the server side", e);
        }
        return commonProps;
    }

    public static synchronized JSONObject createEventJSONObject(Event event, JSONObject commonProps) {
        //TODO: check event timestamp and see if it's still valid? See TracksCleint.isStillValid
        try {
            JSONObject eventJSON = new JSONObject();
            eventJSON.put(EVENT_NAME_KEY, event.getEventName());

            if (!commonProps.has(USER_AGENT_NAME_KEY) ||
                    !commonProps.getString(USER_AGENT_NAME_KEY).equals(event.getUserAgent())) {
                eventJSON.put(USER_AGENT_NAME_KEY, event.getUserAgent());
            }

            eventJSON.put(EVENT_TIMESTAMP_KEY, event.getTimeStamp());

            if (event.getUserType() == TracksClient.NosaraUserType.ANON) {
                eventJSON.put(USER_TYPE_KEY, USER_TYPE_ANON);
                eventJSON.put(USER_ID_KEY, event.getUser());
            } else {
                eventJSON.put(USER_LOGIN_NAME_KEY, event.getUser());
                // no need to put the user type key here. default wpcom is used on the server. 'wpcom:user_id'
            }

            unfolderPropertiesNotAvailableInCommon(event.getUserProperties(), USER_INFO_PREFIX, eventJSON, commonProps);
            unfolderPropertiesNotAvailableInCommon(event.getDeviceInfo(), DEVICE_INFO_PREFIX, eventJSON, commonProps);
            unfolderProperties(event.getCustomEventProperties(), "", eventJSON);

            // Property names need to be lowercase and use underscores instead of dashes,
            // but for a particular event/prop this is not the case
            if (event.getEventName().equals(ALIAS_USER_EVENT_NAME)) {
                String anonID = eventJSON.getString(ALIAS_USER_ANONID_PROP_NAME.toLowerCase());
                eventJSON.put(ALIAS_USER_ANONID_PROP_NAME, anonID);
                eventJSON.remove(ALIAS_USER_ANONID_PROP_NAME.toLowerCase());
            }

           return eventJSON;
        } catch (JSONException err) {
            Log.e(TracksClient.LOGTAG, "Cannot write the JSON representation of the event object", err);
            return null;
        }
    }

    // Nosara only strings property values. Don't convert JSON objs by calling toString()
    // otherwise they will be likely un-queryable
    private static void unfolderPropertiesNotAvailableInCommon(JSONObject objectToFlatten, String flattenPrefix,
                                                                 JSONObject targetJSONObject, JSONObject commonProps) {
        if (objectToFlatten == null || targetJSONObject == null) {
            return;
        }

        if (flattenPrefix == null) {
            Log.w(TracksClient.LOGTAG, " Unfolding props with an empty key. Make sure the keys are unique!");
            flattenPrefix = "";
        }

        Iterator<String> iter = objectToFlatten.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            String flattenKey = String.valueOf(flattenPrefix + key).toLowerCase();
            try {
                Object value = objectToFlatten.get(key);
                String valueString;
                if (value != null) {
                    valueString = String.valueOf(value);
                } else {
                    valueString = "";
                }

                String valueStringInCommons = null;
                // Check if the same key/value is already available in common props
                if (commonProps != null && commonProps.has(flattenKey)) {
                    Object valueInCommons = commonProps.get(flattenKey);
                    if (valueInCommons != null) {
                        valueStringInCommons = String.valueOf(valueInCommons);
                    }
                }

                // Add the value at event level only if it's different from common
                if (valueStringInCommons == null || !valueStringInCommons.equals(valueString)) {
                    targetJSONObject.put(flattenKey, valueString);
                }
            } catch (JSONException e) {
                // Something went wrong!
                Log.e(TracksClient.LOGTAG, "Cannot write the flatten JSON representation of the JSON object", e);
            }
        }
    }

    // Nosara only strings property values. Don't convert JSON objs by calling toString()
    // otherwise they will be likely un-queryable
    private static void unfolderProperties(JSONObject objectToFlatten, String flattenPrefix, JSONObject targetJSONObject) {
        unfolderPropertiesNotAvailableInCommon(objectToFlatten, flattenPrefix, targetJSONObject, null);
    }
}