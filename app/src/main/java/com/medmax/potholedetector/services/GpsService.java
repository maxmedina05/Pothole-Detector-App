package com.medmax.potholedetector.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Max Medina on 2017-07-10.
 */

public class GpsService extends Service {

    public static final int GPS_UPDATE_DELAY = 2000;
    private static final String LOG_TAG = GpsService.class.getSimpleName();
    private static OnGpsUpdateListener mListener;
    private LocationManager mLocationManager;
    private Location lastLocation;

    private double currentLongitude = 0;
    private double currentLatitude = 0;
    private double lastLongitude = 0;
    private double lastLatitude = 0;

    private double currentSpeed = 0;
    private double acceleration = 5;

    private Handler mHandler;
    private Runnable mRunnable;
    private boolean isMoving = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRunnable = this;
                currentSpeed += acceleration;
//                mListener.onGPSUpdate(currentSpeed);
                Log.d(LOG_TAG, "Speed: " + currentSpeed);
                mHandler.postDelayed(mRunnable, GPS_UPDATE_DELAY);
            }
        }, GPS_UPDATE_DELAY);
    }

    public void setOnGpsUpdateListener(OnGpsUpdateListener listener) {
        mListener = listener;
    }

    public void start(){

    }

    @Override
    public void onDestroy() {
//        mLocationManager.removeUpdates(this);
//        mLocationManager.removeGpsStatusListener(this);
//        stopForeground(true);
        super.onDestroy();
    }
}
