package com.chicflow.tunisia.repository;

import com.chicflow.tunisia.models.ClothingItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * THE ONLY place where wardrobe data is read or written.
 *
 * FRONTEND PHASE: data lives in memory (it is lost when the app is closed) and starts with
 * a few demo items so the screens are not empty.
 * LATER: the body of these methods will use SQLite + the PHP API. The screens will not change.
 */
public class WardrobeRepository {

    public static final int SORT_NEWEST = 0;
    public static final int SORT_NAME = 1;
    public static final int SORT_PRICE_LOW = 2;
    public static final int SORT_PRICE_HIGH = 3;

    private static WardrobeRepository instance;

    private final List<ClothingItem> items = new ArrayList<>();
    private int nextId = 1;

    public static synchronized WardrobeRepository getInstance() {
        if (instance == null) {
            instance = new WardrobeRepository();
        }
        return instance;
    }

    private WardrobeRepository() {
        // Demo data (will be removed when real data comes from the server)
        add(new ClothingItem(0, "White Oxford Shirt", "shirt", "White", 65.00, null, "2026-09-02"));
        add(new ClothingItem(0, "Navy Chinos", "trousers", "Navy", 89.90, null, "2026-09-10"));
        add(new ClothingItem(0, "Black Leather Jacket", "jacket", "Black", 240.00, null, "2026-08-21"));
        add(new ClothingItem(0, "White Sneakers", "shoes", "White", 150.00, null, "2026-09-28"));
        add(new ClothingItem(0, "Burgundy Knit Sweater", "shirt", "Burgundy", 79.00, null, "2026-10-05"));
        add(new ClothingItem(0, "Brown Leather Belt", "accessory", "Brown", 35.00, null, "2026-07-30"));
        add(new ClothingItem(0, "Grey Hoodie", "shirt", "Grey", 59.90, null, "2026-10-12"));
    }

    public int count() {
        return items.size();
    }

    /** All items (a copy of the list, in insertion order). */
    public List<ClothingItem> getAll() {
        return new ArrayList<>(items);
    }

    public ClothingItem getById(int id) {
        for (ClothingItem item : items) {
            if (item.getId() == id) return item;
        }
        return null;
    }

    /**
     * Search + filter + sort.
     * @param search   text looked for in the name or color (null or "" = no search)
     * @param category category key, or null for all
     * @param color    color name, or null for all
     * @param sort     one of the SORT_* constants
     */
    public List<ClothingItem> query(String search, String category, String color, int sort) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<ClothingItem> result = new ArrayList<>();
        for (ClothingItem item : items) {
            if (category != null && !category.equals(item.getCategory())) continue;
            if (color != null && !color.equals(item.getColor())) continue;
            if (!q.isEmpty()
                    && !item.getName().toLowerCase(Locale.ROOT).contains(q)
                    && !item.getColor().toLowerCase(Locale.ROOT).contains(q)) continue;
            result.add(item);
        }

        Collections.sort(result, comparatorFor(sort));
        return result;
    }

    private Comparator<ClothingItem> comparatorFor(int sort) {
        if (sort == SORT_NAME) {
            return (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        } else if (sort == SORT_PRICE_LOW) {
            return (a, b) -> Double.compare(a.getPrice(), b.getPrice());
        } else if (sort == SORT_PRICE_HIGH) {
            return (a, b) -> Double.compare(b.getPrice(), a.getPrice());
        }
        // newest first (dates are yyyy-MM-dd, so text order = date order)
        return (a, b) -> {
            int byDate = b.getDateAdded().compareTo(a.getDateAdded());
            return byDate != 0 ? byDate : Integer.compare(b.getId(), a.getId());
        };
    }

    /** Adds the item and gives it an id. */
    public ClothingItem add(ClothingItem item) {
        item.setId(nextId++);
        items.add(item);
        return item;
    }

    /** The item object is already in the list, so there is nothing to copy for now. */
    public void update(ClothingItem item) {
        // later: save to SQLite / send to the server
    }

    public void delete(int id) {
        ClothingItem item = getById(id);
        if (item != null) {
            items.remove(item);
        }
    }
}
