package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import webapp.newcloud.lottery.movie.model.Favorite;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Favorite favorite);

    @Delete
    void delete(Favorite favorite);

    @Query("DELETE FROM favorites WHERE movie_id = :movieId")
    void deleteById(String movieId);

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    List<Favorite> getAll();

    @Query("SELECT * FROM favorites WHERE movie_id = :movieId LIMIT 1")
    Favorite getById(String movieId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE movie_id = :movieId)")
    boolean isFavorite(String movieId);

    @Query("DELETE FROM favorites")
    void clearAll();
}
