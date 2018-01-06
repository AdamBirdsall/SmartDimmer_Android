package com.adambirdsall.smartdimmer.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AdamBirdsall on 11/22/17.
 * @author Adam Birdsall
 */

public class DeviceDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "DEVICES";
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
        db.execSQL("create table " + TABLE_NAME + " (" +
                COL_1_ADDRESS + " TEXT," +
                COL_2_NAME + " TEXT," +
                COL_3_BRIGHTNESS + " TEXT," +
                COL_4_PREVIOUS + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Adding new contact
    public void addDevice(DeviceObject newDevice) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_1_ADDRESS, newDevice.getMacAddress());
        values.put(COL_2_NAME, newDevice.getDeviceName());
        values.put(COL_3_BRIGHTNESS, newDevice.getBrightnessValue());
        values.put(COL_4_PREVIOUS, newDevice.getPreviousValue());

        // Inserting Row
        db.insert(TABLE_NAME, null, values);
        db.close(); // Closing database connection
    }

    // Getting single contact
    public DeviceObject getDevice(String macAddress) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, new String[] {
                        COL_1_ADDRESS, COL_2_NAME, COL_3_BRIGHTNESS, COL_4_PREVIOUS }, COL_1_ADDRESS + "=?",
                new String[] { String.valueOf(macAddress) }, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        DeviceObject deviceObject = new DeviceObject(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));

        // return contact
        return deviceObject;
    }

    // Getting All Contacts
    public List<DeviceObject> getAllDevices() {

        List<DeviceObject> deviceList = new ArrayList<DeviceObject>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                DeviceObject deviceObject = new DeviceObject();
                deviceObject.setMacAddress(cursor.getString(0));
                deviceObject.setDeviceName(cursor.getString(1));
                deviceObject.setBrightnessValue(cursor.getString(2));
                deviceObject.setPreviousValue(cursor.getString(3));

                // Adding contact to list
                deviceList.add(deviceObject);
            } while (cursor.moveToNext());
        }

        // return contact list
        return deviceList;
    }

    // Getting contacts Count
    public int getDevicesCount() {

        String countQuery = "SELECT * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    // Updating single contact
    public int updateDevice(DeviceObject deviceObject) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_2_NAME, deviceObject.getDeviceName());
        values.put(COL_3_BRIGHTNESS, deviceObject.getBrightnessValue());
        values.put(COL_4_PREVIOUS, deviceObject.getPreviousValue());

        // updating row
        return db.update(TABLE_NAME, values, COL_1_ADDRESS + " = ?",
                new String[] { String.valueOf(deviceObject.getMacAddress()) });
    }

    public int updateDeviceBrightness(DeviceObject deviceObject) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_2_NAME, deviceObject.getDeviceName());
        values.put(COL_3_BRIGHTNESS, deviceObject.getBrightnessValue());
        values.put(COL_4_PREVIOUS, deviceObject.getPreviousValue());

        // updating row
        return db.update(TABLE_NAME, values, COL_1_ADDRESS + " = ?",
                new String[] { String.valueOf(deviceObject.getMacAddress()) });
    }

    // Deleting single contact
    public void deleteDevice(DeviceObject deviceObject) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_1_ADDRESS + " = ?",
                new String[] { String.valueOf(deviceObject.getMacAddress()) });
        db.close();
    }
}
