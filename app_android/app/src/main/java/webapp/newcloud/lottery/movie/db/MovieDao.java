package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import webapp.newcloud.lottery.movie.model.Movie;

@Dao
public interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Movie movie);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Movie> movies);

    @Update
    void update(Movie movie);

    @Delete
    void delete(Movie movie);

    @Query("DELETE FROM movies WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM movies WHERE source_id = :sourceId AND category = :category")
    void deleteBySourceAndCategory(String sourceId, String category);

    @Query("SELECT * FROM movies WHERE source_id = :sourceId AND category = :category")
    List<Movie> getBySourceAndCategory(String sourceId, String category);

    @Query("SELECT * FROM movies WHERE source_id = :sourceId")
    List<Movie> getBySourceId(String sourceId);

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    Movie getById(String id);

    @Query("DELETE FROM movies")
    void clearAll();
}
