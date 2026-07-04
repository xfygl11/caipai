package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "live_sources", indices = {
    @Index(value = {"name"}, unique = true)
})
public class LiveSourceEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "epg")
    public String epg;

    @ColumnInfo(name = "logo")
    public String logo;

    @ColumnInfo(name = "is_builtin")
    public int isBuiltin;

    @ColumnInfo(name = "added_time")
    public long addedTime;
}
