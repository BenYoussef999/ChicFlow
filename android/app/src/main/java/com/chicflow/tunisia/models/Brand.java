package com.chicflow.tunisia.models;

/** A Tunisian clothing brand of the "Local Style" catalog. Matches the MySQL table "brands". */
public class Brand {

    private int id;                       // 0 = not saved yet
    private final String name;
    private final String category;
    private final String priceRange;      // may be empty
    private final String websiteUrl;      // may be empty

    public Brand(int id, String name, String category, String priceRange, String websiteUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priceRange = priceRange;
        this.websiteUrl = websiteUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPriceRange() { return priceRange; }
    public String getWebsiteUrl() { return websiteUrl; }

    public boolean hasWebsite() {
        return websiteUrl != null && !websiteUrl.isEmpty();
    }
}
