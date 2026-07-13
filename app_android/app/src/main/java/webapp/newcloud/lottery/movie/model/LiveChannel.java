package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "live_channels")
public class LiveChannel {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "channel_group")
    public String group;

    @ColumnInfo(name = "logo")
    public String logo;

    @ColumnInfo(name = "source")
    public String source;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
