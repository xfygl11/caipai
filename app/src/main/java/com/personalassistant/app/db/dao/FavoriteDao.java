package com.personalassistant.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.personalassistant.app.db.entity.FavoriteEntity;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(FavoriteEntity entity);

    @Delete
    void delete(FavoriteEntity entity);

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    Flowable<List<FavoriteEntity>> getAllFavorites();

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    List<FavoriteEntity> getAllFavoritesBlocking();

    @Query("SELECT * FROM favorites WHERE siteName = :siteName ORDER BY timestamp DESC")
    Flowable<List<FavoriteEntity>> getFavoritesBySite(String siteName);

    @Query("DELETE FROM favorites WHERE movieId = :movieId AND siteName = :siteName")
    void remove(String movieId, String siteName);

    @Query("DELETE FROM favorites")
    void clearAll();
}
