package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import webapp.newcloud.lottery.movie.model.History;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(History history);

    @Delete
    void delete(History history);

    @Query("DELETE FROM history WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 200")
    List<History> getAll();

    @Query("SELECT * FROM history WHERE movie_id = :movieId LIMIT 1")
    History getByMovieId(String movieId);

    @Query("DELETE FROM history")
    void clearAll();
}
