package com.chicflow.tunisia.repository;

import com.chicflow.tunisia.models.Purchase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monthly budgets and purchases. In memory for now (SQLite + PHP API later).
 * A month is a text like "2026-10".
 */
public class BudgetRepository {

    private static BudgetRepository instance;

    private final Map<String, Double> budgets = new HashMap<>();
    private final List<Purchase> purchases = new ArrayList<>();
    private int nextId = 1;

    public static synchronized BudgetRepository getInstance() {
        if (instance == null) {
            instance = new BudgetRepository();
        }
        return instance;
    }

    private BudgetRepository() {
        // Demo data for the current month: 300 TND budget, 180 TND spent = 60 %
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        budgets.put(month, 300.0);
        addPurchase(new Purchase(0, "Grey Hoodie", 60.00, month + "-03"));
        addPurchase(new Purchase(0, "Burgundy Knit Sweater", 80.00, month + "-08"));
        addPurchase(new Purchase(0, "Brown Leather Belt", 40.00, month + "-12"));
    }

    /** The budget of a month, or null if none was set. */
    public Double getBudget(String month) {
        return budgets.get(month);
    }

    public void setBudget(String month, double amount) {
        budgets.put(month, amount);
    }

    /** Purchases of a month, newest first. */
    public List<Purchase> purchasesForMonth(String month) {
        List<Purchase> result = new ArrayList<>();
        for (Purchase p : purchases) {
            if (p.getDate().startsWith(month)) result.add(p);
        }
        Collections.sort(result, (a, b) -> {
            int byDate = b.getDate().compareTo(a.getDate());
            return byDate != 0 ? byDate : Integer.compare(b.getId(), a.getId());
        });
        return result;
    }

    /** Total spent in a month: calculated, never stored. */
    public double totalSpent(String month) {
        double total = 0;
        for (Purchase p : purchases) {
            if (p.getDate().startsWith(month)) total += p.getAmount();
        }
        return total;
    }

    public Purchase addPurchase(Purchase purchase) {
        purchase.setId(nextId++);
        purchases.add(purchase);
        return purchase;
    }

    public void deletePurchase(int id) {
        for (int i = 0; i < purchases.size(); i++) {
            if (purchases.get(i).getId() == id) {
                purchases.remove(i);
                return;
            }
        }
    }
}
