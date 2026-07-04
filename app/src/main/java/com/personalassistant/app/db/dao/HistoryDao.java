package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.personalassistant.app.db.entity.HistoryEntity;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(HistoryEntity entity);

    @Update
    void update(HistoryEntity entity);

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    Flowable<List<HistoryEntity>> getRecentHistory();

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    List<HistoryEntity> getRecentHistoryBlocking();

    @Query("SELECT * FROM history WHERE siteName = :siteName ORDER BY timestamp DESC LIMIT 50")
    Flowable<List<HistoryEntity>> getHistoryBySite(String siteName);

    @Query("DELETE FROM history WHERE movieId = :movieId AND siteName = :siteName")
    void delete(String movieId, String siteName);

    @Query("DELETE FROM history")
    void clearAll();

    @Query("SELECT * FROM history WHERE movieId = :movieId AND siteName = :siteName AND playFrom = :playFrom LIMIT 1")
    HistoryEntity getLatest(String movieId, String siteName, String playFrom);
}
