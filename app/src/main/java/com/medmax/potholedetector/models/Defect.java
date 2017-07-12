package com.medmax.potholedetector.models;

/**
 * Created by Max Medina on 2017-07-11.
 */

public class Defect {
    // window stats
    private double xMean    = 0;
    private double yMean    = 0;
    private double zMean    = 0;
    private double xStd     = 0;
    private double yStd     = 0;
    private double zStd     = 0;

    // small window stats
    private double sm_xMean = 0;
    private double sm_yMean = 0;
    private double sm_zMean = 0;
    private double sm_xStd  = 0;
    private double sm_yStd  = 0;
    private double sm_zStd  = 0;

    private int classType = 0;

    public double getxMean() {
        return xMean;
    }

    public void setxMean(double xMean) {
        this.xMean = xMean;
    }

    public double getyMean() {
        return yMean;
    }

    public void setyMean(double yMean) {
        this.yMean = yMean;
    }

    public double getzMean() {
        return zMean;
    }

    public void setzMean(double zMean) {
        this.zMean = zMean;
    }

    public double getxStd() {
        return xStd;
    }

    public void setxStd(double xStd) {
        this.xStd = xStd;
    }

    public double getyStd() {
        return yStd;
    }

    public void setyStd(double yStd) {
        this.yStd = yStd;
    }

    public double getzStd() {
        return zStd;
    }

    public void setzStd(double zStd) {
        this.zStd = zStd;
    }

    public double getSm_xMean() {
        return sm_xMean;
    }

    public void setSm_xMean(double sm_xMean) {
        this.sm_xMean = sm_xMean;
    }

    public double getSm_yMean() {
        return sm_yMean;
    }

    public void setSm_yMean(double sm_yMean) {
        this.sm_yMean = sm_yMean;
    }

    public double getSm_zMean() {
        return sm_zMean;
    }

    public void setSm_zMean(double sm_zMean) {
        this.sm_zMean = sm_zMean;
    }

    public double getSm_xStd() {
        return sm_xStd;
    }

    public void setSm_xStd(double sm_xStd) {
        this.sm_xStd = sm_xStd;
    }

    public double getSm_yStd() {
        return sm_yStd;
    }

    public void setSm_yStd(double sm_yStd) {
        this.sm_yStd = sm_yStd;
    }

    public double getSm_zStd() {
        return sm_zStd;
    }

    public void setSm_zStd(double sm_zStd) {
        this.sm_zStd = sm_zStd;
    }

    public int getClassType() {
        return classType;
    }

    public void setClassType(int classType) {
        this.classType = classType;
    }

    public static class ClassType {
        public static final int NOT_POTHOLE = 0;
        public static final int POTHOLE = 1;
        public static final int SPEEDBUMP = 2;
    }
}
