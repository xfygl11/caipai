package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personalassistant.app.db.entity.LiveSourceEntity;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface LiveSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LiveSourceEntity entity);

    @Update
    void update(LiveSourceEntity entity);

    @Query("SELECT * FROM live_sources ORDER BY is_builtin DESC, added_time DESC")
    Flowable<List<LiveSourceEntity>> getAllSources();

    @Query("SELECT * FROM live_sources ORDER BY is_builtin DESC, added_time DESC")
    List<LiveSourceEntity> getAllSourcesBlocking();

    @Query("SELECT * FROM live_sources WHERE name = :name LIMIT 1")
    LiveSourceEntity getByName(String name);

    @Query("DELETE FROM live_sources WHERE name = :name")
    void deleteByName(String name);

    @Query("DELETE FROM live_sources")
    void clearAll();
}
