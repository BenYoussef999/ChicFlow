package com.chicflow.tunisia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.sync.LocalStore;
import com.chicflow.tunisia.sync.SyncStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads and writes the clothing items in SQLite.
 * Used by the screens (through WardrobeRepository) and by the sync engine (as its LocalStore).
 * The SQL itself is in ItemSql.
 */
public class ClothingDao implements LocalStore {

    private final DatabaseHelper helper;

    public ClothingDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    // ================================================================= used by the screens

    /** Adds a new item (status pending_create). Returns its local id. */
    public int insert(int userId, ClothingItem item) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("client_uuid", item.getClientUuid());
        values.put("name", item.getName());
        values.put("category", item.getCategory());
        values.put("color", item.getColor());
        values.put("price", item.getPrice());
        values.put("photo_url", item.getPhotoUrl());
        values.put("date_added", item.getDateAdded());
        values.put("sync_status", SyncStatus.PENDING_CREATE);
        values.put("version", 1);
        return (int) helper.getWritableDatabase().insertOrThrow(ItemSql.TABLE, null, values);
    }

    public void updateLocal(int userId, ClothingItem item) {
        helper.getWritableDatabase().execSQL(ItemSql.UPDATE_LOCAL, new Object[]{
                item.getName(), item.getCategory(), item.getColor(), item.getPrice(),
                item.getDateAdded(), item.getId(), userId});
    }

    public void markDeleted(int userId, int localId) {
        helper.getWritableDatabase().execSQL(ItemSql.MARK_DELETED, new Object[]{localId, userId});
    }

    public ClothingItem getById(int userId, int localId) {
        List<ClothingItem> rows = read(ItemSql.SELECT_ONE,
                new String[]{String.valueOf(localId), String.valueOf(userId)});
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Every visible item (rows waiting to be deleted are hidden), in creation order. */
    public List<ClothingItem> getVisible(int userId) {
        return read(ItemSql.SELECT_VISIBLE, new String[]{String.valueOf(userId)});
    }

    public List<ClothingItem> query(int userId, String search, String category, String color, int sort) {
        ItemSql.Query q = ItemSql.wardrobeQuery(userId, search, category, color, sort);
        return read(q.sql, q.args);
    }

    public int count(int userId) {
        return scalar(ItemSql.COUNT_VISIBLE, userId);
    }

    /** Rows that still have something to send to the server (create, update or delete). */
    public int countPending(int userId) {
        return scalar(ItemSql.COUNT_PENDING, userId);
    }

    // ================================================================= used by the sync engine

    @Override
    public List<ClothingItem> getAllRows(int userId) {
        return read(ItemSql.SELECT_ALL_ROWS, new String[]{String.valueOf(userId)});
    }

    @Override
    public void markCreated(int localId, int expectedVersion, int remoteId) {
        helper.getWritableDatabase().execSQL(ItemSql.MARK_CREATED,
                new Object[]{remoteId, expectedVersion, localId});
    }

    @Override
    public void markUpdated(int localId, int expectedVersion) {
        helper.getWritableDatabase().execSQL(ItemSql.MARK_UPDATED, new Object[]{localId, expectedVersion});
    }

    @Override
    public void linkToRemote(int localId, int remoteId) {
        helper.getWritableDatabase().execSQL(ItemSql.LINK_REMOTE, new Object[]{remoteId, localId});
    }

    @Override
    public void overwriteFromRemote(int localId, int expectedVersion, ClothingItem remote) {
        helper.getWritableDatabase().execSQL(ItemSql.OVERWRITE_FROM_REMOTE, new Object[]{
                remote.getRemoteId(), remote.getName(), remote.getCategory(), remote.getColor(),
                remote.getPrice(), remote.getPhotoUrl(), remote.getDateAdded(), localId, expectedVersion});
    }

    @Override
    public void insertFromRemote(int userId, ClothingItem remote) {
        // Items created before client_uuid existed have none: give them a local one
        String uuid = remote.getClientUuid() != null ? remote.getClientUuid() : UUID.randomUUID().toString();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("remote_id", remote.getRemoteId());
        values.put("client_uuid", uuid);
        values.put("name", remote.getName());
        values.put("category", remote.getCategory());
        values.put("color", remote.getColor());
        values.put("price", remote.getPrice());
        values.put("photo_url", remote.getPhotoUrl());
        values.put("date_added", remote.getDateAdded());
        values.put("sync_status", SyncStatus.SYNCED);
        values.put("version", 1);
        helper.getWritableDatabase().insertOrThrow(ItemSql.TABLE, null, values);
    }

    @Override
    public void deleteRow(int localId) {
        helper.getWritableDatabase().delete(ItemSql.TABLE, "local_id = ?",
                new String[]{String.valueOf(localId)});
    }

    // ================================================================= helpers

    private List<ClothingItem> read(String sql, String[] args) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<ClothingItem> result = new ArrayList<>();
        Cursor c = db.rawQuery(sql, args);
        try {
            int iLocal = c.getColumnIndexOrThrow("local_id");
            int iRemote = c.getColumnIndexOrThrow("remote_id");
            int iUuid = c.getColumnIndexOrThrow("client_uuid");
            int iName = c.getColumnIndexOrThrow("name");
            int iCategory = c.getColumnIndexOrThrow("category");
            int iColor = c.getColumnIndexOrThrow("color");
            int iPrice = c.getColumnIndexOrThrow("price");
            int iPhoto = c.getColumnIndexOrThrow("photo_url");
            int iDate = c.getColumnIndexOrThrow("date_added");
            int iStatus = c.getColumnIndexOrThrow("sync_status");
            int iVersion = c.getColumnIndexOrThrow("version");

            while (c.moveToNext()) {
                ClothingItem item = new ClothingItem(
                        c.getInt(iLocal), c.getString(iName), c.getString(iCategory), c.getString(iColor),
                        c.getDouble(iPrice), c.isNull(iPhoto) ? null : c.getString(iPhoto), c.getString(iDate));
                item.setRemoteId(c.isNull(iRemote) ? 0 : c.getInt(iRemote));
                item.setClientUuid(c.getString(iUuid));
                item.setSyncStatus(c.getString(iStatus));
                item.setVersion(c.getInt(iVersion));
                result.add(item);
            }
        } finally {
            c.close();
        }
        return result;
    }

    private int scalar(String sql, int userId) {
        Cursor c = helper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)});
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }
}
