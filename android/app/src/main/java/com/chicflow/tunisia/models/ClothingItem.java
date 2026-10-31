package com.chicflow.tunisia.models;

import com.chicflow.tunisia.sync.SyncStatus;

/**
 * One clothing item. The first fields match the MySQL table "clothing_items".
 * The last four are only used for synchronisation between SQLite (on the phone) and MySQL (server).
 */
public class ClothingItem {

    private int id;                  // LOCAL id (SQLite). 0 = not saved yet
    private String name;
    private String category;         // shirt, trousers, jacket, shoes, accessory, other
    private String color;            // one of ClothingOptions.COLOR_NAMES
    private double price;
    private String photoUrl;         // null until photos are added (bonus feature)
    private String dateAdded;        // yyyy-MM-dd

    // ---- synchronisation ----
    private int remoteId;            // id on the server. 0 = the server does not know this item yet
    private String clientUuid;       // generated on the phone: lets the server ignore a repeated "create"
    private String syncStatus = SyncStatus.PENDING_CREATE;
    private int version = 1;         // +1 at every local change (detects edits made while a sync is running)

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

    public int getRemoteId() { return remoteId; }
    public void setRemoteId(int remoteId) { this.remoteId = remoteId; }

    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
