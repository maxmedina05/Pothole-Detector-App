package com.medmax.potholedetector.utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.medmax.potholedetector.data.PotholeDataContract;

import static com.medmax.potholedetector.data.AccelerometerDataContract.AccelerometerReading;

/**
 * Created by Max Medina on 2017-05-20.
 */

public class PotholeDbHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = PotholeDbHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "potholeDB.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 9;
    private static final int BUFFER_SIZE = 2048;
    private static final String SQL_CREATE_ACCELEROMETER_TABLE =
            "CREATE TABLE IF NOT EXISTS " + AccelerometerReading.TABLE_NAME + " (" +
                    AccelerometerReading._ID + " INTEGER PRIMARY KEY," +
                    AccelerometerReading.COLUMN_NAME_TIME + " INTEGER," +
                    AccelerometerReading.COLUMN_NAME_DEVICE_NAME + " TEXT," +
                    AccelerometerReading.COLUMN_NAME_ACC_X_AXIS + " REAL," +
                    AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS + " REAL," +
                    AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS + " REAL" +
                    ")";
    private static final String SQL_CREATE_POTHOLE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + PotholeDataContract.PossiblePothole.TABLE_NAME + " (" +
                    PotholeDataContract.PossiblePothole._ID + " INTEGER PRIMARY KEY," +
                    PotholeDataContract.PossiblePothole.DATE_CREATED + " TEXT," +
                    PotholeDataContract.PossiblePothole.DEVICE_NAME + " TEXT," +
                    PotholeDataContract.PossiblePothole.LATITUDE + " REAL," +
                    PotholeDataContract.PossiblePothole.LONGITUDE + " REAL," +
                    PotholeDataContract.PossiblePothole.TYPE + " TEXT" +
                    ")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AccelerometerReading.TABLE_NAME + ";" +
                    "DROP TABLE IF EXISTS " + PotholeDataContract.PossiblePothole.TABLE_NAME + ";"
            ;

    private static SQLiteDatabase db;
    private static PotholeDbHelper instance;
    private static Context context;

    private PotholeDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        PotholeDbHelper.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ACCELEROMETER_TABLE);
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
