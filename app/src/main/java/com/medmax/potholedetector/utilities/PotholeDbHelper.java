package com.medmax.potholedetector.utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.medmax.potholedetector.data.AccelerometerReadingContract.AccelerometerReading;

/**
 * Created by Max Medina on 2017-05-20.
 */

public class PotholeDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "PotholeDbHelper";
    private static final String DATABASE_NAME = "potholeDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final int BUFFER_SIZE = 2048;
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AccelerometerReading.TABLE_NAME + " (" +
                    AccelerometerReading._ID + " INTEGER PRIMARY KEY," +
                    AccelerometerReading.COLUMN_NAME_TIMESPAN + " INTEGER," +
                    AccelerometerReading.COLUMN_NAME_DEVICE_MODEL + " TEXT," +
                    AccelerometerReading.COLUMN_NAME_ACC_X_AXIS + " REAL," +
                    AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS + " REAL," +
                    AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS + " REAL" +
                    ")";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AccelerometerReading.TABLE_NAME;

    private static SQLiteDatabase db;
    private static PotholeDbHelper instance;
    private static Context context;

    private PotholeDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        PotholeDbHelper.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public synchronized void close() {
        if (instance != null)
            db.close();
    }

    public static synchronized PotholeDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PotholeDbHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
            db = instance.getWritableDatabase();
        }

        return instance;
    }
}
