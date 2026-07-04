package com.personalassistant.app.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.personalassistant.app.db.dao.CategoryDao;
import com.personalassistant.app.db.dao.FavoriteDao;
import com.personalassistant.app.db.dao.HistoryDao;
import com.personalassistant.app.db.dao.LiveSourceDao;
import com.personalassistant.app.db.dao.MovieDao;
import com.personalassistant.app.db.dao.ParseConfigDao;
import com.personalassistant.app.db.dao.SourceDao;
import com.personalassistant.app.db.entity.CategoryEntity;
import com.personalassistant.app.db.entity.FavoriteEntity;
import com.personalassistant.app.db.entity.HistoryEntity;
import com.personalassistant.app.db.entity.LiveSourceEntity;
import com.personalassistant.app.db.entity.MovieEntity;
import com.personalassistant.app.db.entity.ParseConfigEntity;
import com.personalassistant.app.db.entity.SourceEntity;

@Database(entities = {
    SourceEntity.class, CategoryEntity.class, MovieEntity.class,
    HistoryEntity.class, FavoriteEntity.class,
    LiveSourceEntity.class, ParseConfigEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract SourceDao sourceDao();
    public abstract CategoryDao categoryDao();
    public abstract MovieDao movieDao();
    public abstract HistoryDao historyDao();
    public abstract FavoriteDao favoriteDao();
    public abstract LiveSourceDao liveSourceDao();
    public abstract ParseConfigDao parseConfigDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "personal_assistant_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
