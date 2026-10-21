package com.chicflow.tunisia.repository;

import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.models.Outfit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Saved outfits. Same idea as WardrobeRepository: in memory for now,
 * SQLite + PHP API later (the screens will not change).
 */
public class OutfitRepository {

    private static OutfitRepository instance;

    private final List<Outfit> outfits = new ArrayList<>();
    private int nextId = 1;

    public static synchronized OutfitRepository getInstance() {
        if (instance == null) {
            instance = new OutfitRepository();
        }
        return instance;
    }

    private OutfitRepository() { }

    /** Saved outfits, newest first. Outfits whose clothes were all deleted are removed. */
    public List<Outfit> getAll(boolean favoritesOnly) {
        Iterator<Outfit> it = outfits.iterator();
        while (it.hasNext()) {
            if (itemsOf(it.next()).isEmpty()) it.remove();
        }

        List<Outfit> result = new ArrayList<>();
        for (int i = outfits.size() - 1; i >= 0; i--) {
            Outfit o = outfits.get(i);
            if (!favoritesOnly || o.isFavorite()) result.add(o);
        }
        return result;
    }

    public Outfit getById(int id) {
        for (Outfit o : outfits) {
            if (o.getId() == id) return o;
        }
        return null;
    }

    public Outfit add(Outfit outfit) {
        outfit.setId(nextId++);
        outfits.add(outfit);
        return outfit;
    }

    public void setFavorite(int id, boolean favorite) {
        Outfit o = getById(id);
        if (o != null) o.setFavorite(favorite);
    }

    public void delete(int id) {
        Outfit o = getById(id);
        if (o != null) outfits.remove(o);
    }

    /** The clothing items of an outfit (items deleted from the wardrobe are skipped). */
    public List<ClothingItem> itemsOf(Outfit outfit) {
        WardrobeRepository wardrobe = WardrobeRepository.getInstance();
        List<ClothingItem> result = new ArrayList<>();
        for (int itemId : outfit.getItemIds()) {
            ClothingItem item = wardrobe.getById(itemId);
            if (item != null) result.add(item);
        }
        return result;
    }
}
