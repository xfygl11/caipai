package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sources", indices = {
    @Index(value = {"base"}, unique = true)
})
public class SourceEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "base")
    public String base;

    @ColumnInfo(name = "is_builtin")
    public int isBuiltin;

    @ColumnInfo(name = "added_time")
    public long addedTime;

    @ColumnInfo(name = "last_sync_time")
    public long lastSyncTime;

    @ColumnInfo(name = "status")
    public int status;
}
