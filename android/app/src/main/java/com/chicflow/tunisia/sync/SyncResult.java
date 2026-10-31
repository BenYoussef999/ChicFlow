package com.chicflow.tunisia.sync;

/** The outcome of one synchronisation. */
public class SyncResult {

    /** Both phases (push and pull) finished. */
    public boolean completed = false;
    /** Stopped because the server could not be reached (or answered with a server error). */
    public boolean offline = false;
    /** Stopped because the login token is no longer valid: the user must log in again. */
    public boolean authExpired = false;

    /** Local changes accepted by the server. */
    public int pushed = 0;
    /** Rows added, changed or removed locally because of what the server has. */
    public int pulled = 0;
    /** Single changes the server refused (validation error): they stay pending, the rest went on. */
    public int failed = 0;

    public String message = "";
}
