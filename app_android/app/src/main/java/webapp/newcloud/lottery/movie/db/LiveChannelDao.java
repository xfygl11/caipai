package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import webapp.newcloud.lottery.movie.model.LiveChannel;

import java.util.List;

@Dao
public interface LiveChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LiveChannel channel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<LiveChannel> channels);

    @Delete
    void delete(LiveChannel channel);

    @Query("DELETE FROM live_channels WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM live_channels")
    List<LiveChannel> getAll();

    @Query("SELECT DISTINCT channel_group FROM live_channels WHERE channel_group IS NOT NULL AND channel_group != '' ORDER BY channel_group")
    List<String> getGroups();

    @Query("SELECT * FROM live_channels WHERE channel_group = :group")
    List<LiveChannel> getByGroup(String group);

    @Query("SELECT * FROM live_channels WHERE source = :source")
    List<LiveChannel> getBySource(String source);

    @Query("DELETE FROM live_channels WHERE source = :source")
    void deleteBySource(String source);

    @Query("DELETE FROM live_channels")
    void clearAll();
}
