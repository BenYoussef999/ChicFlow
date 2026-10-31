package com.chicflow.tunisia.sync;

import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.network.ApiException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synchronises the clothing items between SQLite and MySQL.
 *
 *  1. PUSH: send what changed on the phone (creates, then updates, then deletes).
 *  2. PULL: download the server's list and make the phone match it, without ever overwriting
 *     a change that is still waiting to be pushed.
 *
 * Rules worth knowing:
 *  - If the connection fails, the sync stops and every change stays pending. Nothing is lost.
 *  - If the server refuses ONE change (for example a validation error), the others still go through.
 *  - "Create" is safe to repeat: the server recognises the client_uuid and returns the existing item.
 *  - If the same item was changed on two devices, the last one to sync wins.
 *  - Must run on a background thread (it blocks while waiting for the network).
 */
public class SyncEngine {

    private final LocalStore store;
    private final RemoteApi remote;

    public SyncEngine(LocalStore store, RemoteApi remote) {
        this.store = store;
        this.remote = remote;
    }

    public SyncResult sync(int userId) {
        SyncResult result = new SyncResult();
        try {
            push(userId, result);
            pull(userId, result);
            result.completed = true;
        } catch (ApiException e) {
            result.message = e.getMessage();
            if (e.isAuthError()) {
                result.authExpired = true;
            } else {
                result.offline = true;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ PUSH

    private void push(int userId, SyncResult result) throws ApiException {
        List<ClothingItem> rows = store.getAllRows(userId);

        for (ClothingItem row : rows) {
            if (SyncStatus.PENDING_CREATE.equals(row.getSyncStatus())) {
                pushCreate(row, result);
            }
        }
        for (ClothingItem row : rows) {
            if (SyncStatus.PENDING_UPDATE.equals(row.getSyncStatus())) {
                pushUpdate(row, result);
            }
        }
        for (ClothingItem row : rows) {
            if (SyncStatus.PENDING_DELETE.equals(row.getSyncStatus()) && row.getRemoteId() != 0) {
                pushDelete(row, result);
            }
        }
    }

    private void pushCreate(ClothingItem row, SyncResult result) throws ApiException {
        try {
            ClothingItem created = remote.createItem(row);
            store.markCreated(row.getId(), row.getVersion(), created.getRemoteId());
            result.pushed++;
        } catch (ApiException e) {
            skipOrAbort(e, result);
        }
    }

    private void pushUpdate(ClothingItem row, SyncResult result) throws ApiException {
        if (row.getRemoteId() == 0) {          // should not happen, but a create is the safe answer
            pushCreate(row, result);
            return;
        }
        try {
            remote.updateItem(row);
            store.markUpdated(row.getId(), row.getVersion());
            result.pushed++;
        } catch (ApiException e) {
            if (e.isNotFound()) {              // somebody deleted it on the server: the server wins
                store.deleteRow(row.getId());
                result.pulled++;
            } else {
                skipOrAbort(e, result);
            }
        }
    }

    private void pushDelete(ClothingItem row, SyncResult result) throws ApiException {
        try {
            remote.deleteItem(row.getRemoteId());
        } catch (ApiException e) {
            if (!e.isNotFound()) {             // 404 = already gone, which is what we wanted
                skipOrAbort(e, result);
                return;
            }
        }
        store.deleteRow(row.getId());
        result.pushed++;
    }

    /**
     * A problem with ONE change: if the connection or the login is the problem, stop everything
     * (rethrow). If the server simply refused this change, count it and carry on with the others.
     */
    private void skipOrAbort(ApiException e, SyncResult result) throws ApiException {
        if (e.isAuthError() || e.isNetworkError() || e.isServerError()) {
            throw e;
        }
        result.failed++;
    }

    // ------------------------------------------------------------------ PULL

    private void pull(int userId, SyncResult result) throws ApiException {
        List<ClothingItem> remoteItems = remote.pullItems();
        List<ClothingItem> rows = store.getAllRows(userId);     // read AFTER the push: statuses changed

        Map<Integer, ClothingItem> localByRemoteId = new HashMap<>();
        Map<String, ClothingItem> localByUuid = new HashMap<>();
        for (ClothingItem row : rows) {
            if (row.getRemoteId() != 0) localByRemoteId.put(row.getRemoteId(), row);
            if (row.getClientUuid() != null) localByUuid.put(row.getClientUuid(), row);
        }

        Set<Integer> remoteIds = new HashSet<>();
        Map<String, ClothingItem> remoteByUuid = new HashMap<>();

        for (ClothingItem r : remoteItems) {
            remoteIds.add(r.getRemoteId());
            if (r.getClientUuid() != null) remoteByUuid.put(r.getClientUuid(), r);

            ClothingItem local = localByRemoteId.get(r.getRemoteId());
            if (local == null && r.getClientUuid() != null) {
                local = localByUuid.get(r.getClientUuid());     // our create reached the server, but we never heard back
            }

            if (local == null) {
                store.insertFromRemote(userId, r);               // created on another device
                result.pulled++;
            } else if (SyncStatus.PENDING_DELETE.equals(local.getSyncStatus())) {
                continue;                                        // we are deleting it: do not bring it back
            } else if (local.getRemoteId() == 0) {
                store.linkToRemote(local.getId(), r.getRemoteId());
            } else if (SyncStatus.SYNCED.equals(local.getSyncStatus())) {
                if (differs(local, r)) {
                    store.overwriteFromRemote(local.getId(), local.getVersion(), r);
                    result.pulled++;
                }
            }
            // else: pending_create / pending_update -> our change is newer, it is pushed next time
        }

        // Rows the server no longer has were deleted on another device
        for (ClothingItem local : rows) {
            if (local.getRemoteId() != 0 && !remoteIds.contains(local.getRemoteId())) {
                store.deleteRow(local.getId());
                result.pulled++;
            }
        }

        // Rows deleted on the phone BEFORE we ever learned their server id: the server may still have them
        for (ClothingItem local : rows) {
            if (!SyncStatus.PENDING_DELETE.equals(local.getSyncStatus()) || local.getRemoteId() != 0) continue;

            ClothingItem onServer = local.getClientUuid() == null ? null : remoteByUuid.get(local.getClientUuid());
            if (onServer != null) {
                try {
                    remote.deleteItem(onServer.getRemoteId());
                } catch (ApiException e) {
                    if (!e.isNotFound()) {
                        skipOrAbort(e, result);
                        continue;
                    }
                }
            }
            store.deleteRow(local.getId());
        }
    }

    private static boolean differs(ClothingItem a, ClothingItem b) {
        return !eq(a.getName(), b.getName())
                || !eq(a.getCategory(), b.getCategory())
                || !eq(a.getColor(), b.getColor())
                || !eq(a.getDateAdded(), b.getDateAdded())
                || Double.compare(a.getPrice(), b.getPrice()) != 0;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
