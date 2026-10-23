package com.chicflow.tunisia.repository;

import com.chicflow.tunisia.models.Outfit;

import java.util.HashMap;
import java.util.Map;

/**
 * The weekly plan: day (1 = Monday ... 7 = Sunday) -> outfit id.
 * In memory for now (MySQL table weekly_plans later). One outfit per day, like the UNIQUE key there.
 */
public class WeekRepository {

    private static WeekRepository instance;

    private final Map<Integer, Integer> plan = new HashMap<>();

    public static synchronized WeekRepository getInstance() {
        if (instance == null) {
            instance = new WeekRepository();
        }
        return instance;
    }

    private WeekRepository() { }

    /** The outfit planned for a day, or null. A deleted outfit is removed from the plan. */
    public Outfit getPlannedOutfit(int day) {
        Integer outfitId = plan.get(day);
        if (outfitId == null) return null;

        OutfitRepository outfits = OutfitRepository.getInstance();
        Outfit outfit = outfits.getById(outfitId);
        if (outfit == null || outfits.itemsOf(outfit).isEmpty()) {
            plan.remove(day);
            return null;
        }
        return outfit;
    }

    public void setPlan(int day, int outfitId) {
        plan.put(day, outfitId);
    }

    public void removePlan(int day) {
        plan.remove(day);
    }

    public int plannedCount() {
        int count = 0;
        for (int day = 1; day <= 7; day++) {
            if (getPlannedOutfit(day) != null) count++;
        }
        return count;
    }
}
