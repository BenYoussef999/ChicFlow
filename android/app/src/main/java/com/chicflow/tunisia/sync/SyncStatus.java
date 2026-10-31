package com.chicflow.tunisia.sync;

/** What still has to be sent to the server for a local row. Stored as text in SQLite. */
public final class SyncStatus {

    private SyncStatus() { }

    public static final String SYNCED = "synced";                 // identical on the phone and on the server
    public static final String PENDING_CREATE = "pending_create"; // created on the phone, server does not know it
    public static final String PENDING_UPDATE = "pending_update"; // changed on the phone
    public static final String PENDING_DELETE = "pending_delete"; // deleted on the phone (hidden from the screens)
}
