package com.automattic.android.tracks;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds the ability to set custom headers to {@link JsonObjectRequest}
 *
 * A request for retrieving a {@link org.json.JSONObject} response body at a given URL, allowing for an
 * optional {@link org.json.JSONObject} to be passed in as part of the request body.
 *
 */

public class RestRequest extends JsonObjectRequest {
    public static final String USER_AGENT_HEADER = "User-Agent";

    private final Response.Listener<JSONObject> mListener;
    private final Map<String, String> mHeaders = new HashMap<String, String>(2);

    public RestRequest(int method, String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        mListener = listener;
    }

    public void setUserAgent(String userAgent) {
        mHeaders.put(USER_AGENT_HEADER, userAgent);
    }

    @Override
    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            if (response.statusCode == HttpStatus.SC_ACCEPTED) {
                JSONObject responseObject202 = new JSONObject();
                responseObject202.put("Accepted", true);
                return Response.success(responseObject202, HttpHeaderParser.parseCacheHeaders(response));
            }

            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}