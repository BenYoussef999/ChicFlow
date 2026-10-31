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
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.chicflow.tunisia.utils.SessionManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends JSON requests to the PHP API and unwraps the standard answer:
 *   { "success": true,  "data": {...} }   -> data
 *   { "success": false, "message": "..." } -> an error with the message and the HTTP status
 *
 * Two ways to use it:
 *  - get() / post() with a Callback: for screens (answer arrives on the main thread)
 *  - asTransport(): blocking calls, for the sync engine (background thread ONLY)
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

    // ================================================================= for screens (callbacks)

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
                return buildHeaders();
            }
        };

        // 15 s timeout, NO automatic retry (a retried POST could create duplicates)
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
        queue.add(request);
    }

    // ================================================================= for the sync engine (blocking)

    /** Blocking version of get()/post(). Never call it from the main thread. */
    public Transport asTransport() {
        return new Transport() {
            @Override
            public JSONObject get(String endpoint) throws ApiException {
                return executeBlocking(Request.Method.GET, endpoint, null);
            }

            @Override
            public JSONObject post(String endpoint, JSONObject body) throws ApiException {
                return executeBlocking(Request.Method.POST, endpoint, body);
            }
        };
    }

    private JSONObject executeBlocking(int method, String endpoint, JSONObject body) throws ApiException {
        String url = ApiConfig.BASE_URL + endpoint;

        // The RequestFuture is both the "success" and the "error" listener: get() waits for either
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(method, url, body, future, future) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return buildHeaders();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
        future.setRequest(queue.add(request));

        try {
            JSONObject response = future.get(30, TimeUnit.SECONDS);
            if (response.optBoolean("success", false)) {
                JSONObject data = response.optJSONObject("data");
                return data != null ? data : new JSONObject();
            }
            throw new ApiException(response.optString("message", "Unknown error"), 200);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof VolleyError) {
                VolleyError error = (VolleyError) cause;
                int status = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                throw new ApiException(describeError(error), status);
            }
            throw new ApiException("Network error.", -1);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ApiException("The server took too long to answer.", -1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Interrupted.", -1);
        }
    }

    // ================================================================= shared

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        String token = session.getToken();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
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
