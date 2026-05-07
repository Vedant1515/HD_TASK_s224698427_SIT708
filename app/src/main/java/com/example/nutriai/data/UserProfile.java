package com.example.nutriai.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public int id;
    public String dietaryGoal;
    public String restrictions;
    public int caloricTarget;
    public String allergies;

    public UserProfile() {}

    @Ignore
    public UserProfile(String dietaryGoal, String restrictions, int caloricTarget, String allergies) {
        this.dietaryGoal = dietaryGoal;
        this.restrictions = restrictions;
        this.caloricTarget = caloricTarget;
        this.allergies = allergies;
    }
}
