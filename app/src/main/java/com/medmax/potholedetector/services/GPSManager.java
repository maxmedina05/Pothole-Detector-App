package com.medmax.potholedetector.services;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by Max Medina on 2017-07-11.
 */

public class GPSManager implements LocationListener {

    public static final int GPS_MIN_UPDATE_TIME = 500;
    public static final int GPS_MIN_UPDATE_DISTANCE = 0;

    private LocationManager mLocationManager;
    private OnGPSUpdateListener mListener;

    public GPSManager(Context context) {
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void requestLocationUpdates(){
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_MIN_UPDATE_TIME, GPS_MIN_UPDATE_DISTANCE, this);
    }

    public void removeLocationUpdates(){
        mLocationManager.removeUpdates(this);
    }

    public void setOnGPSUpdateListener(OnGPSUpdateListener listener) {
        mListener = listener;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(mListener != null) {
            mListener.onGPSUpdate(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
