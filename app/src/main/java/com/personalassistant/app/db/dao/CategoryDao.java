package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.personalassistant.app.db.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId ORDER BY name ASC")
    List<CategoryEntity> getBySourceId(int sourceId);

    @Query("SELECT DISTINCT name FROM categories ORDER BY name ASC")
    List<String> getDistinctNames();

    @Query("DELETE FROM categories WHERE sourceId = :sourceId")
    void deleteBySourceId(int sourceId);
}
