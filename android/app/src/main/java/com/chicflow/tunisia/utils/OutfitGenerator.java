package com.chicflow.tunisia.utils;

import com.chicflow.tunisia.models.ClothingItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * NOT AI: a small rule-based outfit builder and scorer.
 *  - suggest()  builds an outfit for an occasion (used by Smart Outfit)
 *  - evaluate() scores an outfit picked by the user (used by the 60-second challenge)
 * The real AI call (through PHP) is added in a later stage and will be labeled "AI".
 */
public final class OutfitGenerator {

    private OutfitGenerator() { }

    public static class Suggestion {
        public final List<ClothingItem> items;
        public final String explanation;

        Suggestion(List<ClothingItem> items, String explanation) {
            this.items = items;
            this.explanation = explanation;
        }
    }

    public static class Evaluation {
        public final int score;                  // 0..100
        public final List<String> feedback;

        Evaluation(int score, List<String> feedback) {
            this.score = score;
            this.feedback = feedback;
        }
    }

    private static final Set<String> NEUTRALS = new HashSet<>(Arrays.asList(
            "Black", "White", "Grey", "Beige", "Brown", "Navy"));
    private static final Set<String> DARK = new HashSet<>(Arrays.asList(
            "Black", "Navy", "Burgundy", "Brown"));
    private static final String[] FORMAL_WORDS = {
            "oxford", "blazer", "chino", "loafer", "dress", "knit", "sweater", "suit", "belt", "leather"};
    private static final String[] CASUAL_WORDS = {
            "hoodie", "sneaker", "jogger", "t-shirt", "tee", "jeans", "sweatshirt", "shorts"};

    private static final Random RANDOM = new Random();

    public static boolean isNeutral(String color) {
        return NEUTRALS.contains(color);
    }

    private static boolean containsAny(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w)) return true;
        }
        return false;
    }

    /** How well one item fits the occasion (higher = better). */
    private static int styleScore(ClothingItem item, String occasion) {
        String name = item.getName().toLowerCase(Locale.ROOT);
        boolean formal = containsAny(name, FORMAL_WORDS);
        boolean casual = containsAny(name, CASUAL_WORDS);

        if ("work".equals(occasion) || "event".equals(occasion)) {
            int score = (formal ? 2 : 0) - (casual ? 2 : 0);
            if ("event".equals(occasion) && DARK.contains(item.getColor())) score += 1;
            return score;
        }
        return casual ? 1 : 0;       // university / casual outing: relaxed pieces are welcome
    }

    /** How well a color goes with the colors already chosen. */
    private static double colorFit(ClothingItem candidate, List<ClothingItem> chosen) {
        if (isNeutral(candidate.getColor())) return 2;
        double fit = 1;
        for (ClothingItem other : chosen) {
            if (!isNeutral(other.getColor()) && !other.getColor().equals(candidate.getColor())) {
                fit -= 4;            // two different accent colors clash
            }
        }
        return fit;
    }

    private static ClothingItem pickBest(List<ClothingItem> candidates, List<ClothingItem> chosen,
                                         String occasion) {
        ClothingItem best = null;
        double bestScore = -1000;
        for (ClothingItem c : candidates) {
            // the random part makes "Try another" give different results
            double score = styleScore(c, occasion) * 2 + colorFit(c, chosen) + RANDOM.nextDouble() * 3.0;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private static List<ClothingItem> byCategory(List<ClothingItem> items, String category) {
        List<ClothingItem> result = new ArrayList<>();
        for (ClothingItem i : items) {
            if (category.equals(i.getCategory())) result.add(i);
        }
        return result;
    }

    private static Set<String> accentColors(List<ClothingItem> items) {
        Set<String> accents = new LinkedHashSet<>();
        for (ClothingItem i : items) {
            if (!isNeutral(i.getColor())) accents.add(i.getColor());
        }
        return accents;
    }

    /** Builds an outfit, or returns null when the wardrobe has no shirt or no trousers. */
    public static Suggestion suggest(List<ClothingItem> wardrobe, String occasion) {
        List<ClothingItem> tops = byCategory(wardrobe, "shirt");
        List<ClothingItem> bottoms = byCategory(wardrobe, "trousers");
        List<ClothingItem> shoes = byCategory(wardrobe, "shoes");
        List<ClothingItem> jackets = byCategory(wardrobe, "jacket");
        List<ClothingItem> accessories = byCategory(wardrobe, "accessory");
        if (tops.isEmpty() || bottoms.isEmpty()) return null;

        boolean formalDay = "work".equals(occasion) || "event".equals(occasion);

        List<ClothingItem> chosen = new ArrayList<>();
        ClothingItem top = pickBest(tops, chosen, occasion);
        chosen.add(top);
        ClothingItem bottom = pickBest(bottoms, chosen, occasion);
        chosen.add(bottom);

        ClothingItem shoe = null;
        if (!shoes.isEmpty()) {
            shoe = pickBest(shoes, chosen, occasion);
            chosen.add(shoe);
        }
        ClothingItem jacket = null;
        if (!jackets.isEmpty() && (formalDay || RANDOM.nextDouble() < 0.5)) {
            jacket = pickBest(jackets, chosen, occasion);
            chosen.add(jacket);
        }
        ClothingItem accessory = null;
        if (!accessories.isEmpty() && (formalDay || RANDOM.nextDouble() < 0.3)) {
            accessory = pickBest(accessories, chosen, occasion);
            chosen.add(accessory);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("For ").append(Occasions.phrase(occasion)).append(": the ").append(top.getName())
                .append(" with the ").append(bottom.getName());
        if (shoe != null) sb.append(", grounded by the ").append(shoe.getName());
        if (jacket != null) sb.append(", layered with the ").append(jacket.getName());
        if (accessory != null) sb.append(", finished with the ").append(accessory.getName());
        sb.append(". ");

        Set<String> accents = accentColors(chosen);
        if (accents.isEmpty()) {
            sb.append("All neutral colors, so it is easy to wear.");
        } else if (accents.size() == 1) {
            sb.append("The ").append(accents.iterator().next().toLowerCase(Locale.ROOT))
                    .append(" piece is the single accent color.");
        } else {
            sb.append("Colors were matched as closely as your wardrobe allows.");
        }

        return new Suggestion(chosen, sb.toString());
    }

    /** Scores an outfit out of 100: completeness 45 + color harmony 25 + occasion fit 30. */
    public static Evaluation evaluate(List<ClothingItem> picked, String occasion) {
        List<String> feedback = new ArrayList<>();
        if (picked.isEmpty()) {
            feedback.add("You did not pick anything.");
            return new Evaluation(0, feedback);
        }

        // 1) Completeness: one top, one pair of trousers, one pair of shoes (15 points each)
        String[] keys = {"shirt", "trousers", "shoes"};
        String[] names = {"a top", "trousers", "shoes"};
        int completeness = 0;
        for (int i = 0; i < keys.length; i++) {
            int count = byCategory(picked, keys[i]).size();
            if (count == 0) {
                feedback.add("Missing: " + names[i] + ".");
            } else if (count == 1) {
                completeness += 15;
            } else {
                completeness += 8;
                feedback.add("You picked more than one of " + names[i] + ": one is enough.");
            }
        }

        // 2) Color harmony: at most one accent color
        Set<String> accents = accentColors(picked);
        int harmony = accents.size() <= 1 ? 25 : Math.max(0, 25 - 10 * (accents.size() - 1));
        if (accents.size() > 1) {
            feedback.add("Too many accent colors (" + accents + "): keep it to one.");
        } else if (accents.isEmpty()) {
            feedback.add("Nice neutral palette.");
        } else {
            feedback.add("Good color balance with one accent.");
        }

        // 3) Occasion fit
        int total = 0;
        boolean tooCasual = false;
        for (ClothingItem item : picked) {
            int s = styleScore(item, occasion);
            total += s;
            if (s < 0) tooCasual = true;
        }
        double average = (double) total / picked.size();
        int fit;
        if (average >= 1) fit = 30;
        else if (average >= 0.5) fit = 26;
        else if (average >= 0) fit = 22;      // nothing special either way
        else if (average >= -0.5) fit = 12;
        else fit = 4;                         // mostly the wrong style for this occasion
        if (tooCasual) {
            feedback.add("Some pieces are too casual for " + Occasions.label(occasion).toLowerCase(Locale.ROOT) + ".");
        }

        return new Evaluation(completeness + harmony + fit, feedback);
    }
}
