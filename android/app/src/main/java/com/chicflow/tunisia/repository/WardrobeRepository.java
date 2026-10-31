package com.chicflow.tunisia.repository;

import android.content.Context;

import com.chicflow.tunisia.db.ClothingDao;
import com.chicflow.tunisia.db.DatabaseHelper;
import com.chicflow.tunisia.db.ItemSql;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.sync.SyncManager;
import com.chicflow.tunisia.utils.SessionManager;

import java.util.List;
import java.util.UUID;

/**
 * THE ONLY place where the screens read or write the wardrobe.
 *
 * Data lives in SQLite (so the app works offline). Every change is saved locally first and marked
 * as "pending"; the SyncManager then sends it to the server in the background.
 * Every call only sees the items of the logged-in user.
 */
public class WardrobeRepository {

    public static final int SORT_NEWEST = ItemSql.SORT_NEWEST;
    public static final int SORT_NAME = ItemSql.SORT_NAME;
    public static final int SORT_PRICE_LOW = ItemSql.SORT_PRICE_LOW;
    public static final int SORT_PRICE_HIGH = ItemSql.SORT_PRICE_HIGH;

    private static WardrobeRepository instance;

    private final ClothingDao dao;
    private final SessionManager session;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new WardrobeRepository(context.getApplicationContext());
        }
    }

    public static synchronized WardrobeRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WardrobeRepository.init() was not called (see ChicFlowApp)");
        }
        return instance;
    }

    private WardrobeRepository(Context context) {
        this.dao = new ClothingDao(DatabaseHelper.getInstance(context));
        this.session = new SessionManager(context);
    }

    private int userId() {
        return session.getUserId();
    }

    public int count() {
        return dao.count(userId());
    }

    /** All items in creation order. */
    public List<ClothingItem> getAll() {
        return dao.getVisible(userId());
    }

    /** A copy of the item (null if it does not exist or was deleted). Change it, then call update(). */
    public ClothingItem getById(int id) {
        return dao.getById(userId(), id);
    }

    /**
     * Search + filter + sort.
     * @param search   text looked for in the name or color (null or "" = no search)
     * @param category category key, or null for all
     * @param color    color name, or null for all
     * @param sort     one of the SORT_* constants
     */
    public List<ClothingItem> query(String search, String category, String color, int sort) {
        return dao.query(userId(), search, category, color, sort);
    }

    /** Saves a new item locally, then syncs. Sets the item's id. */
    public ClothingItem add(ClothingItem item) {
        item.setClientUuid(UUID.randomUUID().toString());
        item.setId(dao.insert(userId(), item));
        SyncManager.getInstance().requestSync();
        return item;
    }

    /** Saves the changes of an item (name, category, color, price, date). */
    public void update(ClothingItem item) {
        dao.updateLocal(userId(), item);
        SyncManager.getInstance().requestSync();
    }

    public void delete(int id) {
        dao.markDeleted(userId(), id);
        SyncManager.getInstance().requestSync();
    }

    /** Adds 7 example clothes (handy for a demo). They are normal items: they sync and can be deleted. */
    public void addSampleItems() {
        add(new ClothingItem(0, "White Oxford Shirt", "shirt", "White", 65.00, null, "2026-09-02"));
        add(new ClothingItem(0, "Navy Chinos", "trousers", "Navy", 89.90, null, "2026-09-10"));
        add(new ClothingItem(0, "Black Leather Jacket", "jacket", "Black", 240.00, null, "2026-08-21"));
        add(new ClothingItem(0, "White Sneakers", "shoes", "White", 150.00, null, "2026-09-28"));
        add(new ClothingItem(0, "Burgundy Knit Sweater", "shirt", "Burgundy", 79.00, null, "2026-10-05"));
        add(new ClothingItem(0, "Brown Leather Belt", "accessory", "Brown", 35.00, null, "2026-07-30"));
        add(new ClothingItem(0, "Grey Hoodie", "shirt", "Grey", 59.90, null, "2026-10-12"));
    }
}
