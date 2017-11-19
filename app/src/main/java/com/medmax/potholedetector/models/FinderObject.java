package com.medmax.potholedetector.models;

/**
 * Created by maxme on 2017-10-18.
 */

public class FinderObject {
    private float startTime;
    private float currentTime;
    private float latitude;
    private float longitude;
    private boolean defectFound;
    private StreetDefect streetDefect;

    public FinderObject(float startTime, float currentTime, float latitude, float longitude, boolean defectFound, StreetDefect streetDefect) {
        this.startTime = startTime;
        this.currentTime = currentTime;
        this.defectFound = defectFound;
        this.latitude = latitude;
        this.longitude = longitude;
        this.streetDefect = streetDefect;
    }

    public float getStartTime() {
        return startTime;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public boolean wasDefectFound() {
        return defectFound;
    }

    public void setDefectFound(boolean defectFound) {
        this.defectFound = defectFound;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public StreetDefect getStreetDefect() {
        return streetDefect;
    }
}