package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.personalassistant.app.db.entity.MovieEntity;

import java.util.List;

@Dao
public interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(MovieEntity movie);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<MovieEntity> movies);

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId AND category = :category ORDER BY updateTime DESC LIMIT :limit OFFSET :offset")
    List<MovieEntity> getBySourceAndCategory(int sourceId, String category, int limit, int offset);

    @Query("SELECT COUNT(*) FROM movies WHERE sourceId = :sourceId AND category = :category")
    int countBySourceAndCategory(int sourceId, String category);

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId AND category = :category AND vodId = :vodId LIMIT 1")
    MovieEntity findByVodId(int sourceId, String category, String vodId);

    @Query("DELETE FROM movies WHERE sourceId = :sourceId AND category = :category")
    void deleteBySourceAndCategory(int sourceId, String category);

    @Query("DELETE FROM movies WHERE sourceId = :sourceId")
    void deleteBySourceId(int sourceId);

    @Query("SELECT COUNT(*) FROM movies WHERE sourceId = :sourceId")
    int countBySourceId(int sourceId);
}
