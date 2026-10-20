package com.chicflow.tunisia.models;

/**
 * One clothing item. Field names match the MySQL table "clothing_items"
 * (category = enum key, photoUrl = photo_url, dateAdded = date_added "yyyy-MM-dd").
 */
public class ClothingItem {

    private int id;              // 0 = not saved yet
    private String name;
    private String category;     // shirt, trousers, jacket, shoes, accessory, other
    private String color;        // one of ClothingOptions.COLOR_NAMES
    private double price;
    private String photoUrl;     // null until photos are added (bonus feature)
    private String dateAdded;    // yyyy-MM-dd

    public ClothingItem(int id, String name, String category, String color,
                        double price, String photoUrl, String dateAdded) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.color = color;
        this.price = price;
        this.photoUrl = photoUrl;
        this.dateAdded = dateAdded;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }
}
