package com.example.nutriai.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser {

    public static class MealPlanResult {
        public final String mealPlan;
        public final String ingredients;

        public MealPlanResult(String mealPlan, String ingredients) {
            this.mealPlan = mealPlan;
            this.ingredients = ingredients;
        }
    }

    private static final List<String> UNSAFE_KEYWORDS = Arrays.asList(
            "suicide", "self-harm", "self harm", "kill myself", "end my life",
            "diagnose", "diagnosis", "prescribe", "prescription", "medication",
            "drug dosage", "overdose", "inject", "syringe", "illegal",
            "cancer treatment", "cure cancer", "treat diabetes", "treat disease",
            "medical advice", "doctor advice"
    );

    // Matches "Day 1", "Day 1:", "**Day 1**", "**Day 1:**" etc.
    private static final Pattern DAY1_PATTERN = Pattern.compile("(?i)\\*{0,2}Day\\s+1\\b");

    public MealPlanResult parseMealPlan(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return new MealPlanResult("", "");
        }
        String mealPlan = "";
        String ingredients = "";
        try {
            // Find where actual Day 1 content begins — handle plain "Day 1:", bold "**Day 1**", etc.
            int contentStart = -1;

            // 1. Exact match with colon (fastest)
            contentStart = rawResponse.indexOf("Day 1:");
            if (contentStart == -1) {
                // 2. Any markdown variant of "Day 1"
                Matcher m = DAY1_PATTERN.matcher(rawResponse);
                if (m.find()) contentStart = m.start();
            }
            if (contentStart == -1) {
                // 3. [MEAL_PLAN] marker fallback
                int mp = rawResponse.lastIndexOf("[MEAL_PLAN]");
                if (mp != -1) contentStart = mp + "[MEAL_PLAN]".length();
            }
            if (contentStart == -1) {
                // Nothing recognisable — return raw (adapter will attempt its own parsing)
                return new MealPlanResult(rawResponse.trim(), "");
            }

            String content = rawResponse.substring(contentStart);

            // Accept both [INGREDIENTS] and *INGREDIENTS* — model may use either
            int ingrIdx = content.indexOf("[INGREDIENTS]");
            int ingrLen = "[INGREDIENTS]".length();
            if (ingrIdx == -1) {
                ingrIdx = content.indexOf("*INGREDIENTS*");
                ingrLen = "*INGREDIENTS*".length();
            }
            if (ingrIdx != -1) {
                mealPlan = content.substring(0, ingrIdx).trim();
                ingredients = content.substring(ingrIdx + ingrLen).trim();
                int readyIdx = ingredients.indexOf("[READY]");
                if (readyIdx != -1) ingredients = ingredients.substring(0, readyIdx).trim();
            } else {
                mealPlan = content.trim();
            }
        } catch (Exception e) {
            mealPlan = rawResponse.trim();
        }
        return new MealPlanResult(mealPlan, ingredients);
    }

    public String parseIngredients(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) return "";
        int index = rawResponse.indexOf("[INGREDIENTS]");
        if (index == -1) return "";
        return rawResponse.substring(index + "[INGREDIENTS]".length()).trim();
    }

    public boolean isSafeQuery(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) return true;
        String lower = userMessage.toLowerCase(Locale.getDefault());
        for (String keyword : UNSAFE_KEYWORDS) {
            if (lower.contains(keyword)) return false;
        }
        return true;
    }
}
