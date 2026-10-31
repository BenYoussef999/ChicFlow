package com.chicflow.tunisia.db;

import java.util.ArrayList;
import java.util.List;

/**
 * All the SQL of the local "clothing_items" table, in one place (and free of Android classes,
 * so it can be tested on a computer).
 *
 * Columns:  local_id  = id on the phone (what the screens use)
 *           remote_id = id on the server (NULL until the server knows the item)
 *           sync_status, version = see SyncStatus / ClothingItem
 */
public final class ItemSql {

    private ItemSql() { }

    public static final String TABLE = "clothing_items";

    public static final String COLUMNS =
            "local_id, user_id, remote_id, client_uuid, name, category, color, price, photo_url, "
            + "date_added, sync_status, version";

    // ---------------------------------------------------------------- schema
    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS clothing_items ("
            + " local_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " user_id INTEGER NOT NULL,"
            + " remote_id INTEGER,"
            + " client_uuid TEXT NOT NULL,"
            + " name TEXT NOT NULL,"
            + " category TEXT NOT NULL,"
            + " color TEXT NOT NULL,"
            + " price REAL NOT NULL DEFAULT 0,"
            + " photo_url TEXT,"
            + " date_added TEXT NOT NULL,"
            + " sync_status TEXT NOT NULL DEFAULT 'pending_create',"
            + " version INTEGER NOT NULL DEFAULT 1)";

    public static final String CREATE_INDEX_UUID =
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_items_uuid ON clothing_items (user_id, client_uuid)";

    public static final String CREATE_INDEX_REMOTE =
            "CREATE INDEX IF NOT EXISTS idx_items_remote ON clothing_items (user_id, remote_id)";

    // ---------------------------------------------------------------- changes made by the user
    /** args: name, category, color, price, date_added, local_id, user_id */
    public static final String UPDATE_LOCAL =
            "UPDATE clothing_items SET name = ?, category = ?, color = ?, price = ?, date_added = ?,"
            + " version = version + 1,"
            + " sync_status = CASE WHEN sync_status = 'pending_create' THEN 'pending_create' ELSE 'pending_update' END"
            + " WHERE local_id = ? AND user_id = ? AND sync_status <> 'pending_delete'";

    /** args: local_id, user_id.  The row stays (hidden) until the server has been told. */
    public static final String MARK_DELETED =
            "UPDATE clothing_items SET sync_status = 'pending_delete', version = version + 1"
            + " WHERE local_id = ? AND user_id = ? AND sync_status <> 'pending_delete'";

    // ---------------------------------------------------------------- changes made by the sync
    /** args: remote_id, expected_version, local_id */
    public static final String MARK_CREATED =
            "UPDATE clothing_items SET remote_id = ?,"
            + " sync_status = CASE"
            + "   WHEN version = ? THEN 'synced'"
            + "   WHEN sync_status = 'pending_create' THEN 'pending_update'"
            + "   ELSE sync_status END"
            + " WHERE local_id = ?";

    /** args: local_id, expected_version */
    public static final String MARK_UPDATED =
            "UPDATE clothing_items SET sync_status = 'synced'"
            + " WHERE local_id = ? AND version = ? AND sync_status = 'pending_update'";

    /** args: remote_id, local_id */
    public static final String LINK_REMOTE =
            "UPDATE clothing_items SET remote_id = ?,"
            + " sync_status = CASE WHEN sync_status = 'pending_create' THEN 'pending_update' ELSE sync_status END"
            + " WHERE local_id = ?";

    /** args: remote_id, name, category, color, price, photo_url, date_added, local_id, expected_version */
    public static final String OVERWRITE_FROM_REMOTE =
            "UPDATE clothing_items SET remote_id = ?, name = ?, category = ?, color = ?, price = ?,"
            + " photo_url = ?, date_added = ?, sync_status = 'synced'"
            + " WHERE local_id = ? AND version = ? AND sync_status = 'synced'";

    // ---------------------------------------------------------------- queries
    public static final String SELECT_ALL_ROWS =
            "SELECT " + COLUMNS + " FROM clothing_items WHERE user_id = ? ORDER BY local_id";

    public static final String SELECT_VISIBLE =
            "SELECT " + COLUMNS + " FROM clothing_items WHERE user_id = ? AND sync_status <> 'pending_delete'"
            + " ORDER BY local_id";

    public static final String SELECT_ONE =
            "SELECT " + COLUMNS + " FROM clothing_items WHERE local_id = ? AND user_id = ?"
            + " AND sync_status <> 'pending_delete'";

    public static final String COUNT_VISIBLE =
            "SELECT COUNT(*) FROM clothing_items WHERE user_id = ? AND sync_status <> 'pending_delete'";

    public static final String COUNT_PENDING =
            "SELECT COUNT(*) FROM clothing_items WHERE user_id = ? AND sync_status <> 'synced'";

    // ---------------------------------------------------------------- the wardrobe screen query
    public static final int SORT_NEWEST = 0;
    public static final int SORT_NAME = 1;
    public static final int SORT_PRICE_LOW = 2;
    public static final int SORT_PRICE_HIGH = 3;

    /** SQL text + its "?" values, for the search / filter / sort of the wardrobe screen. */
    public static final class Query {
        public final String sql;
        public final String[] args;

        Query(String sql, List<String> args) {
            this.sql = sql;
            this.args = args.toArray(new String[0]);
        }
    }

    /**
     * @param search   text looked for in name or color ("" or null = no search)
     * @param category category key or null for all
     * @param color    color name or null for all
     * @param sort     one of the SORT_* constants
     */
    public static Query wardrobeQuery(int userId, String search, String category, String color, int sort) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM clothing_items"
                + " WHERE user_id = ? AND sync_status <> 'pending_delete'");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        if (category != null) {
            sql.append(" AND category = ?");
            args.add(category);
        }
        if (color != null) {
            sql.append(" AND color = ?");
            args.add(color);
        }
        String q = search == null ? "" : search.trim();
        if (!q.isEmpty()) {
            // % _ and \ typed by the user are plain characters, not wildcards
            String like = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            sql.append(" AND (name LIKE ? ESCAPE '\\' OR color LIKE ? ESCAPE '\\')");
            args.add(like);
            args.add(like);
        }

        // ORDER BY comes from this fixed list, never from user input
        if (sort == SORT_NAME) {
            sql.append(" ORDER BY name COLLATE NOCASE ASC, local_id ASC");
        } else if (sort == SORT_PRICE_LOW) {
            sql.append(" ORDER BY price ASC, local_id ASC");
        } else if (sort == SORT_PRICE_HIGH) {
            sql.append(" ORDER BY price DESC, local_id ASC");
        } else {
            sql.append(" ORDER BY date_added DESC, local_id DESC");
        }
        return new Query(sql.toString(), args);
    }
}
