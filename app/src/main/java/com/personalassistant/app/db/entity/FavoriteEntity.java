package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites", indices = {
    @Index(value = {"siteName", "movieId"}, unique = true)
})
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "movieId")
    public String movieId;

    @ColumnInfo(name = "movieTitle")
    public String movieTitle;

    @ColumnInfo(name = "siteName")
    public String siteName;

    @ColumnInfo(name = "siteBase")
    public String siteBase;

    @ColumnInfo(name = "categoryName")
    public String categoryName;

    @ColumnInfo(name = "pic")
    public String pic;

    @ColumnInfo(name = "tag")
    public String tag;

    @ColumnInfo(name = "year")
    public String year;

    @ColumnInfo(name = "actor")
    public String actor;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
