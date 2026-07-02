package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "movies", indices = {
    @Index(value = {"sourceId", "category", "vodId"}, unique = true),
    @Index(value = {"sourceId", "category"}),
    @Index(value = {"vodId"})
})
public class MovieEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "sourceId")
    public int sourceId;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "vodId")
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

    @ColumnInfo(name = "rawData")
    public String rawData;

    @ColumnInfo(name = "updateTime")
    public long updateTime;
}
