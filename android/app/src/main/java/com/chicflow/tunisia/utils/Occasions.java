package com.chicflow.tunisia.utils;

import com.chicflow.tunisia.models.Outfit;

/** The four occasions. KEYS must match the ENUM in the MySQL table outfits. */
public final class Occasions {

    private Occasions() { }

    public static final String[] KEYS = {"university", "work", "casual", "event"};
    public static final String[] LABELS = {"University", "Work", "Casual outing", "Special event"};
    private static final String[] PHRASES = {
            "a university day", "a day at work", "a casual outing", "a special event"};

    private static int indexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    public static String label(String key) {
        return LABELS[indexOf(key)];
    }

    public static String phrase(String key) {
        return PHRASES[indexOf(key)];
    }

    /** Text of the badge: it must always be clear whether a suggestion is real AI or not. */
    public static String sourceLabel(String source) {
        if (Outfit.SOURCE_AI.equals(source)) return "AI suggestion";
        if (Outfit.SOURCE_RULES.equals(source)) return "Rule-based suggestion";
        return "Created by you";
    }
}
