package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "history", indices = {
    @Index(value = {"siteName", "movieId", "playFrom", "episodeIndex"}, unique = true)
})
public class HistoryEntity {
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

    @ColumnInfo(name = "playFrom")
    public String playFrom;

    @ColumnInfo(name = "episodeIndex")
    public int episodeIndex;

    @ColumnInfo(name = "episodeName")
    public String episodeName;

    @ColumnInfo(name = "position")
    public long position;

    @ColumnInfo(name = "duration")
    public long duration;

    @ColumnInfo(name = "pic")
    public String pic;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
