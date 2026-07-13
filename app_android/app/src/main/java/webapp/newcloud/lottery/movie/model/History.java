package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class History {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "movie_id")
    public String movieId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "tag")
    public String tag;

    @ColumnInfo(name = "pic")
    public String pic;

    @ColumnInfo(name = "play_url")
    public String playUrl;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
