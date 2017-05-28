package com.medmax.potholedetector.multithreading;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.medmax.potholedetector.data.AccelerometerDataContract;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

public class AccelerometerWorker {

    private final WorkerThread workerThread;
    private PotholeDbHelper potholeDbHelper;

    public AccelerometerWorker(PotholeDbHelper potholeDbHelper) {
        this.potholeDbHelper = potholeDbHelper;
        this.workerThread = new WorkerThread(potholeDbHelper);
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

        WorkerThread(PotholeDbHelper potholeDbHelper) {
            this.potholeDbHelper = potholeDbHelper;
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
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, data.x);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, data.y);
            values.put(AccelerometerDataContract.AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, data.z);
            long result = db.insert(AccelerometerDataContract.AccelerometerReading.TABLE_NAME, null, values);
            Log.d("AccelerometerWorker", "New accelerometer data. result=" + String.valueOf(result));
            return true;
        }
    }
}
