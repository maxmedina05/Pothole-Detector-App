package com.medmax.potholedetector.multithreading;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.medmax.potholedetector.data.AccelerometerDataContract.AccelerometerReading;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AccelerometerWorker {

    private final WorkerThread workerThread;
    private PotholeDbHelper potholeDbHelper;
    private File mFile;

    public AccelerometerWorker(PotholeDbHelper potholeDbHelper, File file) {
        this.potholeDbHelper = potholeDbHelper;
        mFile = file;
        this.workerThread = new WorkerThread(potholeDbHelper, file);
        this.workerThread.start();
    }

    public void addNewData(AccelerometerData accelerometerData) {
        Handler handler = this.workerThread.getHandler();
        Message message = handler.obtainMessage();
        message.obj = accelerometerData;
        handler.sendMessage(message);
    }

    public void close() {
        this.workerThread.handler.getLooper().quitSafely();
        this.potholeDbHelper.getWritableDatabase().close();
    }

    private static class WorkerThread extends Thread implements Handler.Callback {
        private Handler handler;
        private PotholeDbHelper potholeDbHelper;
        private File mFile;

        WorkerThread(PotholeDbHelper potholeDbHelper, File file) {
            this.potholeDbHelper = potholeDbHelper;
            mFile = file;
        }

        Handler getHandler() {
            return this.handler;
        }

        @Override
        public void run() {
            super.run();
            Looper.prepare();
            this.handler = new Handler(this);
            Looper.loop();
        }

        @Override
        public boolean handleMessage(Message msg) {
            AccelerometerData data = (AccelerometerData)msg.obj;
            SQLiteDatabase db = potholeDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(AccelerometerReading.COLUMN_NAME_TIME, data.timestamp);
            values.put(AccelerometerReading.COLUMN_NAME_DEVICE_NAME, data.deviceName);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, data.x);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, data.y);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, data.z);

            values.put(AccelerometerReading.COLUMN_NAME_LONGITUDE, data.longitude);
            values.put(AccelerometerReading.COLUMN_NAME_LATITUDE, data.latitude);
            long result = db.insert(AccelerometerReading.TABLE_NAME, null, values);
            Log.d("AccelerometerWorker", "New accelerometer data. result=" + String.valueOf(result));

            saveDataToCSV(data);
            return true;
        }

        private void saveDataToCSV(AccelerometerData data) {
            try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {
                bwriter.write(String.format("%d, %s, %f, %f, %f, %f, %f", data.timestamp, data.deviceName, data.x, data.y, data.z,
                        data.longitude, data.latitude));
                bwriter.newLine();
                bwriter.close();


            } catch (IOException e) {
                Log.e("AccelerometerWorker", e.toString());
            }
        }

    }
}
