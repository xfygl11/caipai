package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "parse_configs", indices = {
    @Index(value = {"name"}, unique = true)
})
public class ParseConfigEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "flags")
    public String flags;

    @ColumnInfo(name = "headers")
    public String headers;

    @ColumnInfo(name = "sort_order")
    public int sortOrder;
}
