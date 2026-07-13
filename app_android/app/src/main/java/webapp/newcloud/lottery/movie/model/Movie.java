package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "movies")
public class Movie implements Serializable {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "source_id")
    public String sourceId;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "vod_id")
    public String vodId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "pic")
    public String pic;

    @ColumnInfo(name = "tag")
    public String tag;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "year")
    public String year;

    @ColumnInfo(name = "area")
    public String area;

    @ColumnInfo(name = "actor")
    public String actor;

    @ColumnInfo(name = "director")
    public String director;

    @ColumnInfo(name = "score")
    public String score;

    @ColumnInfo(name = "quality")
    public String quality;

    @ColumnInfo(name = "play")
    public String play;

    @ColumnInfo(name = "desc")
    public String desc;

    @ColumnInfo(name = "raw")
    public String raw;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
