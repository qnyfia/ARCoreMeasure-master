package com.AC.Measure.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper { // 實作與資料庫溝通的物件

    private final static int VERSION = 1; // 資料庫版本號

    public Database(Context context) {
        super(context, context.getPackageName(), null, VERSION); // 傳遞資料給父類別建立資料庫
    }

    @Override
    public void onCreate(SQLiteDatabase db) { // 當第一次建立資料庫時觸發建立
        // TODO Auto-generated method stub
        db.execSQL("create table image(name varchar(255) not null, x float, result varchar(255), message varchar(255))"); // 建立圖片資料表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { // 當資料庫版本號更新時觸發
        // TODO Auto-generated method stub

    }

}
