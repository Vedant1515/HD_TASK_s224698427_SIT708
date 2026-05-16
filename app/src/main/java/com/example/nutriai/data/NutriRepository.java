package com.example.nutriai.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.nutriai.SessionManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NutriRepository {

    private final UserDao userDao;
    private final UserProfileDao userProfileDao;
    private final MealPlanDao mealPlanDao;
    private final SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NutriRepository(Context context) {
        NutriDatabase db = NutriDatabase.getInstance(context);
        userDao = db.userDao();
        userProfileDao = db.userProfileDao();
        mealPlanDao = db.mealPlanDao();
        sessionManager = new SessionManager(context);
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    public long insertUser(User user) {
        return userDao.insert(user);
    }

    public User findUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    public void insertUserProfile(UserProfile profile) {
        profile.id = sessionManager.getUserId();
        executor.execute(() -> userProfileDao.insert(profile));
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfileDao.getUserProfile(sessionManager.getUserId());
    }

    public UserProfile getUserProfileSync() {
        return userProfileDao.getUserProfileSync(sessionManager.getUserId());
    }

    // ── Meal Plans ────────────────────────────────────────────────────────────

    public void insertMealPlan(MealPlan plan) {
        plan.userId = sessionManager.getUserId();
        executor.execute(() -> mealPlanDao.insert(plan));
    }

    public LiveData<List<MealPlan>> getAllMealPlans() {
        return mealPlanDao.getMealPlansByUser(sessionManager.getUserId());
    }

    public void updateFavourite(int id, int isFavourited) {
        executor.execute(() -> mealPlanDao.updateFavourite(id, isFavourited));
    }

    public void deletePlan(int id) {
        executor.execute(() -> mealPlanDao.deletePlan(id));
    }

    public void deleteAllMealPlans() {
        int userId = sessionManager.getUserId();
        executor.execute(() -> mealPlanDao.deleteAllByUser(userId));
    }
}
