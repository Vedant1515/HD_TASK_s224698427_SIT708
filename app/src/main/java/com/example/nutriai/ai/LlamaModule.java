package com.example.nutriai.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps the ExecuTorch LlmModule to run Llama 3.2 1B on-device.
 *
 * Real inference path  : executorch.aar present + model + tokenizer pushed to device.
 * Mock / demo fallback : any of the above missing → simulated streaming at 50 ms/token.
 *
 * Push files to device once before running:
 *   adb push Llama-3.2-1B-Instruct.pte  /sdcard/Android/data/com.example.nutriai/files/
 *   adb push tokenizer.model             /sdcard/Android/data/com.example.nutriai/files/
 */
public class LlamaModule {

    private static final String TAG = "LlamaModule";

    public interface TokenCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(String error);
    }

    private static LlamaModule instance;

    // Real ExecuTorch module — typed as Object so class loads even without AAR at runtime
    private Object llmModule;
    private boolean realModelReady = false;
    private boolean mockMode = false;

    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private final Handler        mainHandler = new Handler(Looper.getMainLooper());

    private LlamaModule() {}

    public static synchronized LlamaModule getInstance() {
        if (instance == null) instance = new LlamaModule();
        return instance;
    }

    // ── Model loading ─────────────────────────────────────────────────────────

    public void loadModel(String modelPath) {
        // Expect tokenizer.model in the same directory as the .pte file
        int lastSlash = modelPath.lastIndexOf('/');
        String dir = lastSlash >= 0 ? modelPath.substring(0, lastSlash + 1) : "";
        loadModel(modelPath, dir + "tokenizer.model");
    }

    public void loadModel(String modelPath, String tokenizerPath) {
        executor.execute(() -> {
            try {
                // org.pytorch.executorch.extension.llm.LlmModule
                // Constructor: LlmModule(String modulePath, String tokenizerPath, float temperature)
                Class<?> clazz = Class.forName(
                        "org.pytorch.executorch.extension.llm.LlmModule");
                llmModule = clazz
                        .getConstructor(String.class, String.class, float.class)
                        .newInstance(modelPath, tokenizerPath, 0.6f);

                // Force-load weights into memory now (otherwise load happens on first generate)
                clazz.getMethod("load").invoke(llmModule);

                realModelReady = true;
                mockMode = false;
                Log.i(TAG, "LlmModule loaded — real inference active");

            } catch (ClassNotFoundException e) {
                Log.w(TAG, "ExecuTorch AAR not found — running in mock mode");
                mockMode = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to load model (" + e.getMessage() + ") — mock mode");
                mockMode = true;
            }
        });
    }

    public boolean isModelLoaded() {
        return realModelReady || mockMode;
    }

    // ── Inference ─────────────────────────────────────────────────────────────

    public void runInference(String prompt, TokenCallback callback) {
        if (!realModelReady || mockMode) {
            runMockInference(prompt, callback);
            return;
        }
        runRealInference(prompt, callback);
    }

    private void runRealInference(String prompt, TokenCallback callback) {
        executor.execute(() -> {
            try {
                Class<?> moduleClass   = llmModule.getClass();
                Class<?> callbackClass = Class.forName(
                        "org.pytorch.executorch.extension.llm.LlmCallback");

                StringBuilder accumulated = new StringBuilder();

                // Build a dynamic proxy that implements LlmCallback
                Object cbProxy = java.lang.reflect.Proxy.newProxyInstance(
                        callbackClass.getClassLoader(),
                        new Class[]{callbackClass},
                        (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "onResult":
                                    // onResult(String token) — one token per call
                                    if (args != null && args.length > 0) {
                                        String tok = (String) args[0];
                                        accumulated.append(tok);
                                        mainHandler.post(() -> callback.onToken(tok));
                                    }
                                    break;
                                case "onStats":
                                    // onStats(String jsonStats) — called once at end
                                    String full = accumulated.toString();
                                    mainHandler.post(() -> callback.onComplete(full));
                                    break;
                                case "onError":
                                    String msg = args != null && args.length > 1
                                            ? (String) args[1] : "Unknown error";
                                    mainHandler.post(() -> callback.onError(msg));
                                    break;
                            }
                            return null;
                        });

                // generate(String prompt, int seqLen, LlmCallback llmCallback)
                moduleClass
                        .getMethod("generate", String.class, int.class, callbackClass)
                        .invoke(llmModule, prompt, 1024, cbProxy);

            } catch (Exception e) {
                Log.e(TAG, "Inference error: " + e.getMessage());
                mockMode = true;
                realModelReady = false;
                runMockInference(prompt, callback);
            }
        });
    }

    public void stop() {
        if (realModelReady && llmModule != null) {
            try {
                llmModule.getClass().getMethod("stop").invoke(llmModule);
            } catch (Exception ignored) {}
        }
    }

    public void release() {
        stop();
        if (llmModule != null) {
            try {
                llmModule.getClass().getMethod("close").invoke(llmModule);
            } catch (Exception ignored) {}
        }
        realModelReady = false;
        llmModule = null;
    }

    // ── Mock / demo fallback ─────────────────────────────────────────────────

    private void runMockInference(String prompt, TokenCallback callback) {
        executor.execute(() -> {
            // "[MEAL_PLAN]" only appears in buildMealPlanPrompt — never in the chat prompt
            boolean isMealPlan = prompt.contains("[MEAL_PLAN]");
            streamWords(isMealPlan ? getMockMealPlanResponse()
                                   : getMockChatResponse(prompt), callback);
        });
    }

    private void streamWords(String text, TokenCallback callback) {
        String[] words = text.split(" ");
        StringBuilder acc = new StringBuilder();
        for (String word : words) {
            acc.append(word).append(" ");
            final String tok = word + " ";
            mainHandler.post(() -> callback.onToken(tok));
            try { Thread.sleep(20); } catch (InterruptedException ignored) {}
        }
        final String full = acc.toString().trim();
        mainHandler.post(() -> callback.onComplete(full));
    }

    // ── Mock responses ────────────────────────────────────────────────────────

    private String getMockMealPlanResponse() {
        return "[MEAL_PLAN]\n"
                + "Day 1:\n"
                + "Breakfast: Oatmeal with banana (350 kcal) [Oats, Banana, Almond Butter, Honey]\n"
                + "Lunch: Grilled chicken salad (450 kcal) [Chicken, Lettuce, Tomato, Olive Oil]\n"
                + "Dinner: Salmon with broccoli and rice (550 kcal) [Salmon, Broccoli, Brown Rice, Lemon]\n"
                + "Snack: Greek yogurt with berries (150 kcal) [Greek Yogurt, Mixed Berries]\n\n"
                + "Day 2:\n"
                + "Breakfast: Scrambled eggs with spinach toast (380 kcal) [Eggs, Spinach, Whole Grain Bread]\n"
                + "Lunch: Lentil soup with bread (420 kcal) [Lentils, Carrot, Celery, Bread]\n"
                + "Dinner: Stir-fried tofu with quinoa (480 kcal) [Tofu, Quinoa, Peppers, Soy Sauce]\n"
                + "Snack: Apple with peanut butter (200 kcal) [Apple, Peanut Butter]\n\n"
                + "Day 3:\n"
                + "Breakfast: Greek yogurt parfait (320 kcal) [Greek Yogurt, Granola, Mixed Berries]\n"
                + "Lunch: Turkey avocado wrap (460 kcal) [Turkey, Avocado, Lettuce, Tortilla]\n"
                + "Dinner: Baked chicken with sweet potato (520 kcal) [Chicken, Sweet Potato, Green Beans]\n"
                + "Snack: Mixed nuts (180 kcal) [Almonds, Walnuts, Dried Cranberries]\n\n"
                + "Day 4:\n"
                + "Breakfast: Whole grain pancakes (340 kcal) [Whole Grain Flour, Eggs, Milk, Strawberries]\n"
                + "Lunch: Chickpea curry with rice (490 kcal) [Chickpeas, Tomato, Coconut Milk, Brown Rice]\n"
                + "Dinner: Grilled salmon with asparagus (530 kcal) [Salmon, Asparagus, Quinoa, Lemon]\n"
                + "Snack: Hummus with vegetables (150 kcal) [Hummus, Carrot, Cucumber]\n\n"
                + "Day 5:\n"
                + "Breakfast: Smoothie bowl (360 kcal) [Banana, Spinach, Protein Powder, Almond Milk]\n"
                + "Lunch: Tuna salad sandwich (400 kcal) [Tuna, Whole Grain Bread, Tomato, Lettuce]\n"
                + "Dinner: Beef stir-fry with noodles (510 kcal) [Lean Beef, Broccoli, Peppers, Noodles]\n"
                + "Snack: Cottage cheese with pineapple (160 kcal) [Cottage Cheese, Pineapple]\n\n"
                + "Day 6:\n"
                + "Breakfast: Avocado toast with eggs (420 kcal) [Avocado, Sourdough, Eggs, Chilli Flakes]\n"
                + "Lunch: Minestrone soup (430 kcal) [Vegetables, Beans, Tomato, Pasta]\n"
                + "Dinner: Herb-crusted cod (490 kcal) [Cod, Herbs, Roasted Vegetables, Mash]\n"
                + "Snack: Dark chocolate and almonds (170 kcal) [Dark Chocolate, Almonds]\n\n"
                + "Day 7:\n"
                + "Breakfast: Overnight oats with mango (350 kcal) [Oats, Chia Seeds, Almond Milk, Mango]\n"
                + "Lunch: Halloumi and pepper salad (470 kcal) [Halloumi, Roasted Peppers, Spinach, Pita]\n"
                + "Dinner: Lentil dal with basmati rice (500 kcal) [Lentils, Tomato, Spices, Basmati Rice]\n"
                + "Snack: Banana with walnuts (190 kcal) [Banana, Walnuts]\n";
    }

    private String getMockChatResponse(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("protein"))
            return "Protein is essential for muscle repair and growth. Good sources include lean meats, "
                    + "fish, eggs, dairy, legumes, and tofu. Most adults need around 0.8–1.2g per kg of "
                    + "body weight daily. Spreading protein across meals helps maximise muscle protein synthesis.";
        if (lower.contains("calor"))
            return "Calories are units of energy from food and drinks. Your daily needs depend on age, "
                    + "weight, height, and activity level. A deficit of 300–500 kcal per day is recommended "
                    + "for healthy weight loss. Focus on nutrient-dense foods within your calorie budget.";
        if (lower.contains("carb"))
            return "Carbohydrates are your body's primary energy source. Complex carbs from whole grains "
                    + "and vegetables provide sustained energy. Limit simple carbs from sugary foods. "
                    + "Aim for 45–65% of total calories from whole-food carb sources.";
        if (lower.contains("fat"))
            return "Healthy fats are vital for hormones, brain function, and vitamin absorption. Focus on "
                    + "unsaturated fats from avocados, nuts, seeds, and olive oil. Limit saturated fats "
                    + "and avoid trans fats. Fats should make up around 20–35% of total daily calories.";
        return "That's a great nutrition question! A balanced diet rich in whole foods, lean proteins, "
                + "healthy fats, and complex carbohydrates is the foundation of good health. Individual "
                + "needs vary — feel free to ask me about specific nutrients, meal timing, or food choices!";
    }
}
