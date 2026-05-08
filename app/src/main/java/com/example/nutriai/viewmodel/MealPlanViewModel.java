package com.example.nutriai.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.nutriai.ai.LlamaModule;
import com.example.nutriai.ai.PromptBuilder;
import com.example.nutriai.ai.ResponseParser;
import com.example.nutriai.data.MealPlan;
import com.example.nutriai.data.NutriRepository;
import com.example.nutriai.data.UserProfile;

import java.util.concurrent.Executors;

public class MealPlanViewModel extends AndroidViewModel {

    private final NutriRepository repository;
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ResponseParser responseParser = new ResponseParser();

    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<ResponseParser.MealPlanResult> mealPlanResult = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private String currentMealPlan = "";
    private String currentIngredients = "";

    public MealPlanViewModel(@NonNull Application application) {
        super(application);
        repository = new NutriRepository(application);
    }

    public void generateMealPlan(Context context) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        Executors.newSingleThreadExecutor().execute(() -> {
            UserProfile profile = repository.getUserProfileSync();
            if (profile == null) {
                isLoading.postValue(false);
                errorMessage.postValue("Please set up your profile first.");
                return;
            }

            String prompt = promptBuilder.buildMealPlanPrompt(profile);
            LlamaModule.getInstance().runInference(prompt, new LlamaModule.TokenCallback() {
                private final StringBuilder sb = new StringBuilder();

                @Override
                public void onToken(String token) {
                    sb.append(token);
                }

                @Override
                public void onComplete(String fullResponse) {
                    String response = fullResponse;

                    // Llama 3 template: model output begins after the last assistant header
                    final String ASST = "<|start_header_id|>assistant<|end_header_id|>";
                    int hdrIdx = response.lastIndexOf(ASST);
                    if (hdrIdx != -1) {
                        response = response.substring(hdrIdx + ASST.length()).trim();
                        int eot = response.indexOf("<|eot_id|>");
                        if (eot >= 0) response = response.substring(0, eot).trim();
                    } else {
                        // Fallback for plain-text echo: strip up to and including [READY]
                        response = response.replaceAll("<\\|[a-zA-Z0-9_]+\\|>", "").trim();
                        int sentinel = response.indexOf("[READY]");
                        if (sentinel != -1) {
                            response = response.substring(sentinel + "[READY]".length()).trim();
                        }
                    }

                    ResponseParser.MealPlanResult result = responseParser.parseMealPlan(response);
                    currentMealPlan = result.mealPlan;
                    currentIngredients = result.ingredients;
                    mealPlanResult.postValue(result);
                    isLoading.postValue(false);
                }

                @Override
                public void onError(String error) {
                    errorMessage.postValue(error);
                    isLoading.postValue(false);
                }
            });
        });
    }

    public void savePlan() {
        if (currentMealPlan.isEmpty()) return;
        MealPlan plan = new MealPlan(currentMealPlan, currentIngredients);
        repository.insertMealPlan(plan);
    }
}
