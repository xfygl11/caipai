package com.personalassistant.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories", indices = {
    @Index(value = {"sourceId", "name"}, unique = true),
    @Index(value = {"sourceId", "typeId"})
})
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "sourceId")
    public int sourceId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "rawName")
    public String rawName;

    @ColumnInfo(name = "typeId")
    public String typeId;

    @ColumnInfo(name = "parentId")
    public String parentId;

    @ColumnInfo(name = "updateTime")
    public long updateTime;
}
