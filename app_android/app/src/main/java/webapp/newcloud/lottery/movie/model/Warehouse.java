package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouses")
public class Warehouse {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "spider")
    public String spider;

    @ColumnInfo(name = "logo")
    public String logo;

    @ColumnInfo(name = "wallpaper")
    public String wallpaper;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
