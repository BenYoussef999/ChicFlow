package com.chicflow.tunisia.sync;

import com.chicflow.tunisia.models.ClothingItem;

import java.util.List;

/**
 * What the sync engine needs from the local database.
 * The real implementation is ClothingDao (SQLite). Keeping this as an interface lets the
 * sync logic be tested without a phone.
 *
 * "expectedVersion" protects against edits made while a sync is running: if the user changed
 * the row in the meantime (its version went up), the row stays "pending" and is sent again.
 */
public interface LocalStore {

    /** Every row of this user, whatever its status (including rows waiting to be deleted). */
    List<ClothingItem> getAllRows(int userId);

    /** The server created the item: remember its id. Synced, unless the row was edited meanwhile. */
    void markCreated(int localId, int expectedVersion, int remoteId);

    /** The server accepted our update. Synced, unless the row was edited meanwhile. */
    void markUpdated(int localId, int expectedVersion);

    /** The server already had this item (found by client_uuid): remember its id, push our data next. */
    void linkToRemote(int localId, int remoteId);

    /** Replace a synced row by the server's version (does nothing if the row was edited meanwhile). */
    void overwriteFromRemote(int localId, int expectedVersion, ClothingItem remote);

    /** A new item from the server (created on another device). */
    void insertFromRemote(int userId, ClothingItem remote);

    /** Really removes the row. */
    void deleteRow(int localId);
}
