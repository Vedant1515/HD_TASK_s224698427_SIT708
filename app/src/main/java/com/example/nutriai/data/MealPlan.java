package com.example.nutriai.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "meal_plan")
public class MealPlan {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    public String planContent;
    public String ingredients;
    public long createdAt;
    public int isFavourited;

    public MealPlan() {
        this.createdAt = System.currentTimeMillis();
        this.isFavourited = 0;
    }

    @Ignore
    public MealPlan(String planContent, String ingredients) {
        this.planContent = planContent;
        this.ingredients = ingredients;
        this.createdAt = System.currentTimeMillis();
        this.isFavourited = 0;
    }
}
