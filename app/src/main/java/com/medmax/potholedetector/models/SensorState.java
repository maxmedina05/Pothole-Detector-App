package com.medmax.potholedetector.models;

/**
 * Created by maxme on 2017-06-19.
 */

public class SensorState {
    private boolean mState;
    private boolean mPreviousState;

    public SensorState() {
        mState = false;
        mPreviousState = true;
    }

    public boolean isState() {
        return mState;
    }

    public void setState(boolean state) {
        mState = state;
    }

    public boolean isPreviousState() {
        return mPreviousState;
    }

    public void setPreviousState(boolean previousState) {
        mPreviousState = previousState;
    }
}
