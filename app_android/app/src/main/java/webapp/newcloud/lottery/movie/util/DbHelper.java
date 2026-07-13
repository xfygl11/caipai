package webapp.newcloud.lottery.movie.util;

import android.content.Context;

import androidx.room.Room;

import webapp.newcloud.lottery.movie.App;
import webapp.newcloud.lottery.movie.db.AppDatabase;

public class DbHelper {
    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (DbHelper.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "tvbox_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }

    public static void destroyInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
