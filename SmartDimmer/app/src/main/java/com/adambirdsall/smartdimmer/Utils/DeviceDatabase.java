package com.adambirdsall.smartdimmer.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by AdamBirdsall on 11/22/17.
 */

public class DeviceDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Devices.db";
    public static final String TABLE_NAME = "SAVED_DEVICES";
    public static final String COL_1_ADDRESS = "MAC_ADDRESS";
    public static final String COL_2_NAME = "NAME";
    public static final String COL_3_BRIGHTNESS = "BRIGHTNESS_VALUE";
    public static final String COL_4_PREVIOUS = "PREVIOUS_VALUE";

    public DeviceDatabase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT,NAME TEXT,SURNAME TEXT,MARKS INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String macAddress, String name, String brightnessValue, String previousBrightness) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL_1_ADDRESS, macAddress);
        contentValues.put(COL_2_NAME, name);
        contentValues.put(COL_3_BRIGHTNESS, brightnessValue);
        contentValues.put(COL_4_PREVIOUS, previousBrightness);

        long result = db.insert(TABLE_NAME, null, contentValues);

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }

    public boolean updateData(String macAddress, String name, String brightnessValue, String previousBrightness) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_ADDRESS, macAddress);
        contentValues.put(COL_2_NAME, name);
        contentValues.put(COL_3_BRIGHTNESS, brightnessValue);
        contentValues.put(COL_4_PREVIOUS, previousBrightness);

        db.update(TABLE_NAME, contentValues, "BRIGHTNESS_VALUE = ?", new String[] { brightnessValue });
        db.update(TABLE_NAME, contentValues, "PREVIOUS_VALUE = ?", new String[] { previousBrightness });

        return true;
    }

    public Integer deleteData (String macAddress) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "MAC_ADDRESS = ?", new String[] { macAddress });
    }
}
