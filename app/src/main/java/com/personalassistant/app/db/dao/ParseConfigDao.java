package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personalassistant.app.db.entity.ParseConfigEntity;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface ParseConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ParseConfigEntity entity);

    @Update
    void update(ParseConfigEntity entity);

    @Query("SELECT * FROM parse_configs ORDER BY sort_order ASC")
    Flowable<List<ParseConfigEntity>> getAllConfigs();

    @Query("SELECT * FROM parse_configs ORDER BY sort_order ASC")
    List<ParseConfigEntity> getAllConfigsBlocking();

    @Query("SELECT * FROM parse_configs WHERE name = :name LIMIT 1")
    ParseConfigEntity getByName(String name);

    @Query("DELETE FROM parse_configs WHERE name = :name")
    void deleteByName(String name);

    @Query("DELETE FROM parse_configs")
    void clearAll();
}
