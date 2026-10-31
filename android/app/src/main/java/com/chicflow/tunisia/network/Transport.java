package com.chicflow.tunisia.network;

import org.json.JSONObject;

/**
 * Sends one request and returns the "data" part of the server's answer.
 * Blocks until the answer arrives, so call it from a background thread only.
 * (The app uses Volley; the automatic tests use a plain HTTP connection.)
 */
public interface Transport {

    JSONObject get(String endpoint) throws ApiException;

    JSONObject post(String endpoint, JSONObject body) throws ApiException;
}
