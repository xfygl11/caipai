package com.personalassistant.app.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.personalassistant.app.db.dao.CategoryDao;
import com.personalassistant.app.db.dao.MovieDao;
import com.personalassistant.app.db.dao.SourceDao;
import com.personalassistant.app.db.entity.CategoryEntity;
import com.personalassistant.app.db.entity.MovieEntity;
import com.personalassistant.app.db.entity.SourceEntity;

@Database(entities = {SourceEntity.class, CategoryEntity.class, MovieEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract SourceDao sourceDao();
    public abstract CategoryDao categoryDao();
    public abstract MovieDao movieDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "personal_assistant_db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
