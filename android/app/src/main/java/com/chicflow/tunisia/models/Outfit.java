package com.chicflow.tunisia.models;

import java.util.ArrayList;
import java.util.List;

/**
 * A combination of clothing items. Matches the MySQL tables "outfits" + "outfit_items"
 * (itemIds = the rows of outfit_items).
 */
public class Outfit {

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_RULES = "rules";
    public static final String SOURCE_AI = "ai";

    private int id;                  // 0 = not saved yet
    private String name;
    private String occasion;         // university, work, casual, event
    private String explanation;
    private String source;           // manual, rules, ai
    private boolean favorite;
    private final List<Integer> itemIds;

    public Outfit(int id, String name, String occasion, String explanation,
                  String source, boolean favorite, List<Integer> itemIds) {
        this.id = id;
        this.name = name;
        this.occasion = occasion;
        this.explanation = explanation;
        this.source = source;
        this.favorite = favorite;
        this.itemIds = new ArrayList<>(itemIds);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getOccasion() { return occasion; }
    public String getExplanation() { return explanation; }
    public String getSource() { return source; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public List<Integer> getItemIds() { return itemIds; }
}
