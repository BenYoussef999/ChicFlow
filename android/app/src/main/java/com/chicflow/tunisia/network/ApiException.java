package com.chicflow.tunisia.network;

/** A failed call to the server. status = HTTP code, or -1 when there was no answer (offline, timeout...). */
public class ApiException extends Exception {

    private final int status;

    public ApiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }

    /** No answer at all: offline, timeout, unreadable answer. */
    public boolean isNetworkError() { return status == -1; }
    public boolean isServerError() { return status >= 500; }
    public boolean isAuthError() { return status == 401; }
    public boolean isNotFound() { return status == 404; }
}
