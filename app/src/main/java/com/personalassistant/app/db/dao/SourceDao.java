package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.personalassistant.app.db.entity.SourceEntity;

import java.util.List;

@Dao
public interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SourceEntity source);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SourceEntity> sources);

    @Query("SELECT * FROM sources ORDER BY id ASC")
    List<SourceEntity> getAll();

    @Query("SELECT * FROM sources WHERE base = :base LIMIT 1")
    SourceEntity getByBase(String base);

    @Query("SELECT * FROM sources ORDER BY id ASC")
    List<SourceEntity> getAllSourcesBlocking();

    @Query("SELECT * FROM sources WHERE base = :base LIMIT 1")
    SourceEntity getByBaseBlocking(String base);

    @Query("SELECT * FROM sources WHERE id = :id LIMIT 1")
    SourceEntity getByIdBlocking(long id);

    @Query("UPDATE sources SET last_sync_time = :syncTime WHERE id = :id")
    void updateLastSync(int id, long syncTime);

    @Query("DELETE FROM sources WHERE id = :id")
    void deleteById(int id);
}
