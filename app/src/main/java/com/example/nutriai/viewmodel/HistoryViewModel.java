package com.example.nutriai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.nutriai.data.MealPlan;
import com.example.nutriai.data.NutriRepository;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final NutriRepository repository;
    private final LiveData<List<MealPlan>> allMealPlans;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new NutriRepository(application);
        allMealPlans = repository.getAllMealPlans();
    }

    public LiveData<List<MealPlan>> getAllMealPlans() {
        return allMealPlans;
    }

    public void toggleFavourite(int id, int currentValue) {
        int newValue = currentValue == 1 ? 0 : 1;
        repository.updateFavourite(id, newValue);
    }

    public void deletePlan(int id) {
        repository.deletePlan(id);
    }

    public void deleteAllPlans() {
        repository.deleteAllMealPlans();
    }
}
