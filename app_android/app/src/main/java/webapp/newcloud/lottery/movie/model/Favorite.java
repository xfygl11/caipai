package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class Favorite {
    @PrimaryKey
    @ColumnInfo(name = "movie_id")
    @NonNull
    public String movieId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "pic")
    public String pic;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "tag")
    public String tag;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
