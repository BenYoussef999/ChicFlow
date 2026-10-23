package com.chicflow.tunisia.models;

import java.util.ArrayList;
import java.util.List;

/** One row of the week planner: a day (1 = Monday ... 7 = Sunday) and its outfit, if any. */
public class DayPlan {

    private final int day;
    private final Outfit outfit;                 // null = nothing planned
    private final List<ClothingItem> items;

    public DayPlan(int day, Outfit outfit, List<ClothingItem> items) {
        this.day = day;
        this.outfit = outfit;
        this.items = new ArrayList<>(items);
    }

    public int getDay() { return day; }
    public Outfit getOutfit() { return outfit; }
    public List<ClothingItem> getItems() { return items; }
    public boolean isPlanned() { return outfit != null; }
}
