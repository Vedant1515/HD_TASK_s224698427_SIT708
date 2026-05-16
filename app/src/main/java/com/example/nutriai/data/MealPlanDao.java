package com.example.nutriai.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MealPlanDao {

    @Insert
    void insert(MealPlan plan);

    @Query("SELECT * FROM meal_plan WHERE userId = :userId ORDER BY isFavourited DESC, createdAt DESC")
    LiveData<List<MealPlan>> getMealPlansByUser(int userId);

    @Query("UPDATE meal_plan SET isFavourited = :isFavourited WHERE id = :id")
    void updateFavourite(int id, int isFavourited);

    @Query("DELETE FROM meal_plan WHERE id = :id")
    void deletePlan(int id);

    @Query("DELETE FROM meal_plan WHERE userId = :userId")
    void deleteAllByUser(int userId);
}
