package com.personalassistant.app.db;

import android.content.Context;

import com.personalassistant.app.data.model.ParseConfig;
import com.personalassistant.app.db.entity.ParseConfigEntity;

import java.util.Arrays;
import java.util.List;

public class ParserSeeder {
    public static void seedIfEmpty(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<ParseConfigEntity> existing = db.parseConfigDao().getAllConfigsBlocking();
        if (existing != null && !existing.isEmpty()) return;

        List<ParseConfig> defaults = getDefaultParsers();
        for (int i = 0; i < defaults.size(); i++) {
            ParseConfig pc = defaults.get(i);
            ParseConfigEntity entity = new ParseConfigEntity();
            entity.name = pc.name;
            entity.url = pc.url;
            entity.type = pc.type;
            entity.sortOrder = i;
            db.parseConfigDao().insert(entity);
        }
    }

    private static List<ParseConfig> getDefaultParsers() {
        return Arrays.asList(
                new ParseConfig.Builder().name("默认").url("").type(0).build(),
                new ParseConfig.Builder().name("虾米解析").url("https://jx.xmflv.com/?url=").type(0).build(),
                new ParseConfig.Builder().name("爱豆").url("https://jx.aidouer.net/?url=").type(0).build(),
                new ParseConfig.Builder().name("M3U8").url("https://jx.m3u8.tv/jiexi/?url=").type(0).build(),
                new ParseConfig.Builder().name("冰豆").url("https://bd.jx.cn/?url=").type(0).build(),
                new ParseConfig.Builder().name("听乐").url("https://jx.jialucm.top/?v=").type(0).build()
        );
    }
}
