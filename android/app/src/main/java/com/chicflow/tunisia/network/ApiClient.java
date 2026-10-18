package com.chicflow.tunisia.network;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.chicflow.tunisia.utils.SessionManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends JSON requests to the PHP API and unwraps the standard answer:
 *   { "success": true,  "data": {...} }   -> callback.onSuccess(data)
 *   { "success": false, "message": "..." } -> callback.onError(message, httpStatus)
 *
 * The login token (if any) is added automatically as "Authorization: Bearer ...".
 */
public class ApiClient {

    public interface Callback {
        void onSuccess(JSONObject data);
        void onError(String message, int httpStatus);
    }

    private static ApiClient instance;

    private final RequestQueue queue;
    private final SessionManager session;

    private ApiClient(Context context) {
        Context app = context.getApplicationContext();
        queue = Volley.newRequestQueue(app);
        session = new SessionManager(app);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    public void get(String endpoint, Callback callback) {
        send(Request.Method.GET, endpoint, null, callback);
    }

    public void post(String endpoint, JSONObject body, Callback callback) {
        send(Request.Method.POST, endpoint, body, callback);
    }

    private void send(int method, String endpoint, JSONObject body, final Callback callback) {
        String url = ApiConfig.BASE_URL + endpoint;

        JsonObjectRequest request = new JsonObjectRequest(method, url, body,
                response -> {
                    if (response.optBoolean("success", false)) {
                        JSONObject data = response.optJSONObject("data");
                        callback.onSuccess(data != null ? data : new JSONObject());
                    } else {
                        callback.onError(response.optString("message", "Unknown error"), 200);
                    }
                },
                error -> {
                    int status = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                    callback.onError(describeError(error), status);
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                String token = session.getToken();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        // 15 s timeout, NO automatic retry (a retried POST could create duplicates)
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
        queue.add(request);
    }

    /** Turns a VolleyError into a message a user (or you) can understand. */
    private String describeError(VolleyError error) {
        // The server answered with an error status (401, 422, 500...) and a JSON message
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String text = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                String message = new JSONObject(text).optString("message", "");
                if (!message.isEmpty()) {
                    return message;
                }
            } catch (Exception ignored) {
                // body was not JSON, fall through
            }
            return "Server error (HTTP " + error.networkResponse.statusCode + ")";
        }
        if (error instanceof TimeoutError) {
            return "The server took too long to answer.";
        }
        if (error instanceof NoConnectionError) {
            return "Cannot reach the server. Check your internet connection and BASE_URL.";
        }
        if (error instanceof ParseError) {
            return "The server answer is not valid JSON. Check BASE_URL.";
        }
        return "Network error.";
    }
}
