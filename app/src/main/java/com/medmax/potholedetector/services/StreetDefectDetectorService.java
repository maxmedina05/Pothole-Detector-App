package com.medmax.potholedetector.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class StreetDefectDetectorService extends Service {

    public static String LOG_TAG = StreetDefectDetectorService.class.getSimpleName();
    FinderService mSensorService;

    public StreetDefectDetectorService() {
        Log.i(LOG_TAG, "StreetDefectDetectorService created!");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "onStartCommand");
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind");
        throw null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind");
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorService = new FinderService(getApplicationContext());
        mSensorService.onCreate();
        Log.i(LOG_TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        mSensorService.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(LOG_TAG, "onTaskRemoved");
        mSensorService.onDestroy();
        super.onTaskRemoved(rootIntent);
    }
}
