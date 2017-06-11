package com.medmax.potholedetector.models;

import java.util.Calendar;

/**
 * Created by maxme on 2017-06-11.
 */

public class Pothole {
    private float mLatitude;
    private float mLongitude;
    private String mDeviceName;
    private String mTypeHole;
    private Calendar mDateCreated;

    public Pothole(float latitude, float longitude, String deviceName, String typeHole, Calendar dateCreated) {
        mLatitude = latitude;
        mLongitude = longitude;
        mDeviceName = deviceName;
        mTypeHole = typeHole;
        mDateCreated = dateCreated;
    }

    public float getLatitude() {
        return mLatitude;
    }

    public void setLatitude(float latitude) {
        mLatitude = latitude;
    }

    public float getLongitude() {
        return mLongitude;
    }

    public void setLongitude(float longitude) {
        mLongitude = longitude;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public String getTypeHole() {
        return mTypeHole;
    }

    public void setTypeHole(String typeHole) {
        mTypeHole = typeHole;
    }

    public Calendar getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(Calendar dateCreated) {
        mDateCreated = dateCreated;
    }
}
