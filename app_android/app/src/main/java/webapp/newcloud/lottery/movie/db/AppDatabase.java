package webapp.newcloud.lottery.movie.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import webapp.newcloud.lottery.movie.model.*;

@Database(entities = {
    Warehouse.class,
    SiteConfig.class,
    Movie.class,
    LiveChannel.class,
    History.class,
    Favorite.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract WarehouseDao warehouseDao();
    public abstract SiteConfigDao siteConfigDao();
    public abstract MovieDao movieDao();
    public abstract LiveChannelDao liveChannelDao();
    public abstract HistoryDao historyDao();
    public abstract FavoriteDao favoriteDao();
}
