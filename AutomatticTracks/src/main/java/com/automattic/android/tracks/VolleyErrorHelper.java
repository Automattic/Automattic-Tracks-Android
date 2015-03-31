package com.automattic.android.tracks;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

public class VolleyErrorHelper {

    public static synchronized void logVolleyErrorDetails(final VolleyError volleyError) {
        if (volleyError == null) {
            Log.e(TracksClient.LOGTAG, "Tried to log a VolleyError, but the error obj was null!");
            return;
        }
        if (volleyError.networkResponse != null) {
            NetworkResponse networkResponse = volleyError.networkResponse;
            Log.e(TracksClient.LOGTAG, "Network status code: " + networkResponse.statusCode);
            if (networkResponse.data != null) {
                Log.e(TracksClient.LOGTAG, "Network data: " + new String(networkResponse.data));
            }
        }

        // TODO Log the headers here

        Log.e(TracksClient.LOGTAG, "Volley Error details: " + volleyError.getMessage(), volleyError);
    }

    /**
     *  Socket timeout, either server is too busy to handle the request or there is some network latency issue.
     * @param error
     * @return
     */
    public static boolean isSocketTimeoutProblem(VolleyError error) {
        return (error instanceof TimeoutError);
    }

    /**
     * Determines whether the error is related to network.
     *
     * NetworkError - Socket disconnection, server down, DNS issues might result in this error.
     * NoConnectionError - When device does not have internet connection, your error handling logic can club.
     * @param error
     * @return
     */
    public static boolean isNetworkProblem(VolleyError error) {
        return (error instanceof NetworkError) || (error instanceof NoConnectionError);
    }

    /**
     * Determines whether the error is related to server
     *
     * The server responded with an error, most likely with 4xx or 5xx HTTP status codes.
     *
     * @param error
     * @return
     */
    public static boolean isServerProblem(VolleyError error) {
        return (error instanceof ServerError) || (error instanceof AuthFailureError);
    }
}