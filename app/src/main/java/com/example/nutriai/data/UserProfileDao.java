package com.example.nutriai.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    LiveData<UserProfile> getUserProfile(int userId);

    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    UserProfile getUserProfileSync(int userId);
}
