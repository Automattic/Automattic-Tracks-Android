package com.automattic.android.tracks;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

class MessageBuilder {

    private static final String USER_INFO_PREFIX = "user_info_";
    private static final String DEVICE_INFO_PREFIX = "device_info_";

    private static final String EVENT_NAME_KEY = "_en";
    private static final String USER_AGENT_NAME_KEY = "_via_ua";
    private static final String EVENT_TIMESTAMP_KEY = "_ts";
    private static final String REQUEST_TIMESTAMP_KEY = "_rt";
    private static final String USER_TYPE_KEY = "_ut";
    private static final String USER_TYPE_ANON = "anon";
    private static final String USER_TYPE_WPCOM = "wpcom:user_id";
    private static final String USER_TYPE_SIMPLENOTE = "simplenote:user_id";
    private static final String USER_TYPE_POCKETCASTS = "pocketcasts:user_id";
    private static final String USER_TYPE_DAYONE = "dayone:user_id";
    private static final String USER_ID_KEY = "_ui";
    private static final String USER_LANG_KEY = "_lg";
    private static final String USER_LOGIN_NAME_KEY = "_ul";

    public static final String ALIAS_USER_EVENT_NAME = "_aliasUser";
    public static final String ALIAS_USER_ANONID_PROP_NAME = "anonId";

    public static synchronized boolean isReservedKeyword(String keyToTest) {
        String keyToTestLowercase = keyToTest.toLowerCase(Locale.ROOT);
        if (keyToTestLowercase.equals(EVENT_NAME_KEY) ||
                keyToTestLowercase.equals(USER_AGENT_NAME_KEY) ||
                keyToTestLowercase.equals(EVENT_TIMESTAMP_KEY) ||
                keyToTestLowercase.equals(REQUEST_TIMESTAMP_KEY) ||
                keyToTestLowercase.equals(USER_TYPE_KEY) ||
                keyToTestLowercase.equals(USER_ID_KEY) ||
                keyToTestLowercase.equals(USER_LANG_KEY) ||
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

    public static synchronized JSONObject createRequestCommonPropsJSONObject(Context ctx,
                                                                             DeviceInformation deviceInformation,
                                                                             JSONObject userProperties,
                                                                             String userAgent) {
        JSONObject commonProps = new JSONObject();
        try {
            commonProps.put(USER_AGENT_NAME_KEY, userAgent);
        } catch (JSONException e) {
            Log.e(TracksClient.LOGTAG, "Cannot add the "+  USER_AGENT_NAME_KEY + " property to request commons.");
        }

        try {
            commonProps.put(USER_LANG_KEY, ctx.getResources().getConfiguration().locale.toString());
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

            switch (event.getUserType()) {
                case ANON:
                    eventJSON.put(USER_ID_KEY, event.getUser());
                    eventJSON.put(USER_TYPE_KEY, USER_TYPE_ANON);
                    break;
                case WPCOM:
                    eventJSON.put(USER_LOGIN_NAME_KEY, event.getUser());
                    eventJSON.put(USER_TYPE_KEY, USER_TYPE_WPCOM);
                    break;
                case SIMPLENOTE:
                    eventJSON.put(USER_LOGIN_NAME_KEY, event.getUser());
                    eventJSON.put(USER_TYPE_KEY, USER_TYPE_SIMPLENOTE);
                    break;
                case POCKETCASTS:
                    eventJSON.put(USER_ID_KEY, event.getUser());
                    eventJSON.put(USER_TYPE_KEY, USER_TYPE_POCKETCASTS);
                    break;
                case DAYONE:
                    eventJSON.put(USER_ID_KEY, event.getUser());
                    eventJSON.put(USER_TYPE_KEY, USER_TYPE_DAYONE);
            }

            unfolderPropertiesNotAvailableInCommon(event.getUserProperties(), USER_INFO_PREFIX, eventJSON, commonProps);
            unfolderPropertiesNotAvailableInCommon(event.getDeviceInfo(), DEVICE_INFO_PREFIX, eventJSON, commonProps);
            unfolderProperties(event.getCustomEventProperties(), "", eventJSON);

            // Property names need to be lowercase and use underscores instead of dashes,
            // but for a particular event/prop this is not the case
            if (event.getEventName().equals(ALIAS_USER_EVENT_NAME)) {
                String anonID = eventJSON.getString(ALIAS_USER_ANONID_PROP_NAME.toLowerCase(Locale.ROOT));
                eventJSON.put(ALIAS_USER_ANONID_PROP_NAME, anonID);
                eventJSON.remove(ALIAS_USER_ANONID_PROP_NAME.toLowerCase(Locale.ROOT));
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
            String flattenKey = String.valueOf(flattenPrefix + key).toLowerCase(Locale.ROOT);
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
