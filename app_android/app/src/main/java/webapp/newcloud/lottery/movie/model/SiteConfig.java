package webapp.newcloud.lottery.movie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "site_configs")
public class SiteConfig {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "warehouse_id")
    public String warehouseId;

    @ColumnInfo(name = "source_type")
    public String sourceType;

    @ColumnInfo(name = "key")
    public String key;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "api")
    public String api;

    @ColumnInfo(name = "searchable")
    public int searchable;

    @ColumnInfo(name = "quick_search")
    public int quickSearch;

    @ColumnInfo(name = "filterable")
    public int filterable;

    @ColumnInfo(name = "type_flag")
    public String type_flag;

    @ColumnInfo(name = "player_type")
    public int playerType;

    @ColumnInfo(name = "ext")
    public String ext;

    @ColumnInfo(name = "pass")
    public boolean pass;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public transient java.util.Map<String, String> categoryMap;
}
