package com.example.nutriai.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, UserProfile.class, MealPlan.class}, version = 2, exportSchema = false)
public abstract class NutriDatabase extends RoomDatabase {

    private static volatile NutriDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract UserProfileDao userProfileDao();
    public abstract MealPlanDao mealPlanDao();

    public static NutriDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NutriDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    NutriDatabase.class,
                                    "nutri_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
