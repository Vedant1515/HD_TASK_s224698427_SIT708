package com.example.nutriai.ai;

import com.example.nutriai.data.UserProfile;

public class PromptBuilder {

    public static final String SYSTEM_PROMPT =
            "You are NutriAI, a helpful and knowledgeable nutrition and meal planning assistant. " +
            "You ONLY answer questions about nutrition, meal planning, dietary advice, healthy eating, " +
            "food ingredients, cooking methods, and related wellness topics. " +
            "If asked about anything outside nutrition and meal planning — such as politics, coding, " +
            "medical diagnosis, self-harm, or unrelated topics — politely refuse and redirect the user " +
            "to nutrition questions. Never provide medical diagnoses or prescribe treatments. " +
            "Always recommend consulting a healthcare professional for medical concerns. " +
            "IMPORTANT: When the user sends a greeting (hi, hello, hey, etc.), respond ONLY with a " +
            "brief friendly greeting and ask how you can help with their nutrition goals. " +
            "Do NOT proactively mention, reference, or discuss the user's profile settings, dietary " +
            "goals, caloric targets, or restrictions unless the user explicitly asks about them.";

    public String buildMealPlanPrompt(UserProfile profile) {
        String goal = profile.dietaryGoal != null ? profile.dietaryGoal : "General Health";
        String restrictions = profile.restrictions != null ? profile.restrictions : "None";
        String allergies = profile.allergies != null && !profile.allergies.isEmpty()
                ? profile.allergies : "None";
        int calories = profile.caloricTarget > 0 ? profile.caloricTarget : 2000;

        // Llama 3 native chat template so the model follows instructions reliably
        return "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
                + "You are a meal planning assistant. Output ONLY the 7-day meal plan with no "
                + "preamble, explanation, or sign-off. Use exactly this format for every meal:\n"
                + "MealType: Name (X kcal) [ingredient1, ingredient2, ingredient3]\n"
                + "Begin the response immediately with [MEAL_PLAN] on its own line, then Day 1: "
                + "through Day 7:, each containing Breakfast, Lunch, Dinner, Snack."
                + "<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
                + "Generate a 7-day meal plan.\n"
                + "Goal: " + goal + "\n"
                + "Diet: " + restrictions + "\n"
                + "Daily calories: " + calories + " kcal\n"
                + "Avoid: " + allergies + "\n"
                + "<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n";
    }

    public String buildChatPrompt(UserProfile profile, String userMessage, String chatHistory) {
        StringBuilder prompt = new StringBuilder();

        // Llama 3 native chat template — the model is trained on this exact format
        prompt.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n");
        prompt.append(SYSTEM_PROMPT);

        if (profile != null) {
            prompt.append("\n\n[Background: use only when the user explicitly asks about their personal goals or restrictions]\n");
            if (profile.dietaryGoal != null) prompt.append("- Goal: ").append(profile.dietaryGoal).append("\n");
            if (profile.restrictions != null) prompt.append("- Diet: ").append(profile.restrictions).append("\n");
            if (profile.caloricTarget > 0) prompt.append("- Calories: ").append(profile.caloricTarget).append(" kcal\n");
            if (profile.allergies != null && !profile.allergies.isEmpty()) {
                prompt.append("- Allergies: ").append(profile.allergies).append("\n");
            }
        }

        prompt.append("<|eot_id|>");

        // Prior turns already formatted as Llama 3 user/assistant headers
        if (chatHistory != null && !chatHistory.isEmpty()) {
            prompt.append(chatHistory);
        }

        // Current user turn — model generates after the assistant header
        prompt.append("<|start_header_id|>user<|end_header_id|>\n\n");
        prompt.append(userMessage);
        prompt.append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n");

        return prompt.toString();
    }
}
