package com.chicflow.tunisia.models;

/** A clothing purchase. Matches the MySQL table "purchases". */
public class Purchase {

    private int id;                  // 0 = not saved yet
    private final String description;
    private final double amount;
    private final String date;       // yyyy-MM-dd

    public Purchase(int id, String description, double amount, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
}
