package com.example.nutriai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.nutriai.data.NutriRepository;
import com.example.nutriai.data.UserProfile;

public class ProfileViewModel extends AndroidViewModel {

    private final NutriRepository repository;
    private final LiveData<UserProfile> userProfile;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new NutriRepository(application);
        userProfile = repository.getUserProfile();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public void saveProfile(UserProfile profile) {
        repository.insertUserProfile(profile);
    }

    public boolean hasProfile() {
        UserProfile p = userProfile.getValue();
        return p != null && p.dietaryGoal != null;
    }
}
