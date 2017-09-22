package com.medmax.potholedetector.models;

/**
 * Created by maxme on 2017-09-21.
 */

public class StreetDefect {

    private String deviceName = "";
    private float xMean = 0;
    private float yMean = 0;
    private float zMean = 0;

    private float xStd = 0;
    private float yStd = 0;
    private float zStd = 0;
    private float latitude = 0;
    private float longitude = 0;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public float getxMean() {
        return xMean;
    }

    public void setxMean(float xMean) {
        this.xMean = xMean;
    }

    public float getyMean() {
        return yMean;
    }

    public void setyMean(float yMean) {
        this.yMean = yMean;
    }

    public float getzMean() {
        return zMean;
    }

    public void setzMean(float zMean) {
        this.zMean = zMean;
    }

    public float getxStd() {
        return xStd;
    }

    public void setxStd(float xStd) {
        this.xStd = xStd;
    }

    public float getyStd() {
        return yStd;
    }

    public void setyStd(float yStd) {
        this.yStd = yStd;
    }

    public float getzStd() {
        return zStd;
    }

    public void setzStd(float zStd) {
        this.zStd = zStd;
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
}
