package com.AC.Measure.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.AC.Measure.model.Image;

import java.text.SimpleDateFormat;

public class ImageHelper {

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 格式化時間日期

    private static interface TableInfo { // 資料表資訊
        String TABLE = "image",
                NAME = "name",
                X = "x",
                RESULT = "result",
                MESSAGE = "message",
                FORM[] = {NAME, X, RESULT, MESSAGE};
    }
    private SQLiteDatabase db;

    public ImageHelper(Context context) {
        Database database = new Database(context);
        db = database.getWritableDatabase();
    }

    private ContentValues createContentValues(Image image) { // 新增資料動作的設定
        ContentValues values = new ContentValues();
        values.put(TableInfo.NAME, image.getName());
        values.put(TableInfo.X, image.getX());
        values.put(TableInfo.RESULT, image.getResult());
        values.put(TableInfo.MESSAGE, image.getMessage());

        return values;
    }

    private Image getEntity(Cursor cursor) { // 取得資料動作的設定
        Image image = new Image();
        image.setName(cursor.getString(0));
        image.setX(cursor.getFloat(1));
        image.setResult(cursor.getString(2));
        image.setMessage(cursor.getString(3));

        return image;
    }

    public Image[] getAll() { // 取得所有資料
        Cursor cursor = db.query(TableInfo.TABLE, null, null, null, null, null, TableInfo.NAME + " desc");
        int size = cursor.getCount();
        Image images[] = null;
        if (size != 0) {
            cursor.moveToFirst();
            images = new Image[size];
            if (size > 0) {
                for (int i=0; i<size; i++) {
                    images[i] = getEntity(cursor);
                    cursor.moveToNext();
                }
            }
        }

        cursor.close();

        return images;
    }

    public Image[] get(String name) { // 取得指定資料
        Cursor cursor = db.query(TableInfo.TABLE, TableInfo.FORM, TableInfo.NAME + " = '" + name + "'", null, null, null, TableInfo.NAME);
        cursor.moveToFirst();

        Image images[]= null;
        if (cursor.getCount() > 0) {
            images = new Image[cursor.getCount()];
            for (int i=0; i<images.length; i++) {
                images[i] = getEntity(cursor);
                cursor.moveToNext();
            }
        }

        cursor.close();

        return images;
    }

    public Image[] getGroup(String name) { // 以群取得的資料
        Cursor cursor = db.query(TableInfo.TABLE, TableInfo.FORM, TableInfo.NAME + " like '" + name.substring(0, name.lastIndexOf("-") - 1) + "%'", null, null, null, TableInfo.NAME);
        cursor.moveToFirst();

        Image images[]= null;
        if (cursor.getCount() > 0) {
            images = new Image[cursor.getCount()];
            for (int i=0; i<images.length; i++) {
                images[i] = new Image();
                images[i].setName(cursor.getString(0));
                cursor.moveToNext();
            }
        }

        cursor.close();

        return images;
    }

    public void setAll(Image[] images) { // 輸入所有資料
        db.beginTransaction();

        for (int i=0; i<images.length; i++)
            db.insert(TableInfo.TABLE, null, createContentValues(images[i]));

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void set(Image image) { // 輸入指定資料
        if (get(image.getName()) == null) {
            db.beginTransaction();
            db.insert(TableInfo.TABLE, null, createContentValues(image));
            db.setTransactionSuccessful();
            db.endTransaction();
        } else
            update(image);
    }

    public void update(Image image) { // 更新指定資料
        db.beginTransaction();
        db.update(TableInfo.TABLE, createContentValues(image), TableInfo.NAME + " = '" + image.getName() + "'", null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void deleteAll() { // 刪除所有資料
        db.beginTransaction();
        db.delete(TableInfo.TABLE, null, null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void delete(Image image) { // 刪除指定資料
        db.beginTransaction();
        db.delete(TableInfo.TABLE, TableInfo.NAME + " = '" + image.getName() + "'", null);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void close() { // 關閉資料庫連結
        db.close();
    }

}

