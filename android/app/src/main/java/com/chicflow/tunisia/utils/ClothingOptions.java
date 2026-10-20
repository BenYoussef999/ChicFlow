package com.chicflow.tunisia.utils;

import android.graphics.Color;

/**
 * Fixed lists used by the wardrobe screens.
 * CATEGORY_KEYS must match the ENUM in the MySQL table clothing_items.
 */
public final class ClothingOptions {

    private ClothingOptions() { }

    public static final String[] CATEGORY_KEYS = {
            "shirt", "trousers", "jacket", "shoes", "accessory", "other"};

    public static final String[] CATEGORY_LABELS = {
            "Shirt", "Trousers", "Jacket", "Shoes", "Accessory", "Other"};

    // Emoji (written as unicode escapes so the file encoding never matters)
    public static final String[] CATEGORY_EMOJI = {
            "\uD83D\uDC55",             // shirt
            "\uD83D\uDC56",             // trousers
            "\uD83E\uDDE5",             // jacket
            "\uD83D\uDC5F",             // shoes
            "\uD83D\uDC5C",             // accessory (bag)
            "\uD83C\uDFF7\uFE0F"};      // other (tag)

    public static final String[] COLOR_NAMES = {
            "Black", "White", "Grey", "Beige", "Brown", "Navy",
            "Blue", "Green", "Red", "Burgundy", "Pink", "Yellow"};

    private static final String[] COLOR_HEX = {
            "#212121", "#FAFAFA", "#9E9E9E", "#D7C4A3", "#6D4C41", "#1F2A44",
            "#3F6FB5", "#4C7A4F", "#C0392B", "#800020", "#E8A0B4", "#E6C34A"};

    public static int categoryIndex(String key) {
        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            if (CATEGORY_KEYS[i].equals(key)) return i;
        }
        return CATEGORY_KEYS.length - 1;   // "other"
    }

    public static String categoryLabel(String key) {
        return CATEGORY_LABELS[categoryIndex(key)];
    }

    public static String categoryEmoji(String key) {
        return CATEGORY_EMOJI[categoryIndex(key)];
    }

    /** Label shown in the dropdown -> key stored in the item. Null if not a valid label. */
    public static String categoryKeyForLabel(String label) {
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            if (CATEGORY_LABELS[i].equals(label)) return CATEGORY_KEYS[i];
        }
        return null;
    }

    public static boolean isValidColor(String name) {
        for (String c : COLOR_NAMES) {
            if (c.equals(name)) return true;
        }
        return false;
    }

    /** Android color int for a color name (grey if unknown). */
    public static int colorInt(String name) {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            if (COLOR_NAMES[i].equals(name)) return Color.parseColor(COLOR_HEX[i]);
        }
        return Color.parseColor("#9E9E9E");
    }
}
