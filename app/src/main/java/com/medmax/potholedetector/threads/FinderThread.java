package com.medmax.potholedetector.threads;

/**
 * Created by maxme on 2017-08-29.
 */

public class FinderThread extends Thread {
    private boolean isCancel = false;

    public boolean isCancel() {
        return isCancel;
    }

    public void setCancel(boolean cancel) {
        isCancel = cancel;
    }

    public void run() {
        while(!isCancel) {

        }
    }
}
