package com.medmax.potholedetector.utilities;

/**
 * Created by maxme on 2017-08-27.
 */

public class FrequencyCalculator {
    private int fqCount = 0;
    private float fqsTime = 0;
    private float fqcTime = 0;
    private float fqHz = 0;

    public void reset() {
        fqCount = 0;
        fqsTime = 0;
        fqcTime = 0;
        fqHz = 0;
    }

    public void calculateFrequency() {
        if (fqsTime == 0) {
            fqsTime = System.nanoTime();
        }
        fqcTime = System.nanoTime();
        fqHz = (fqCount++ / ((fqcTime - fqsTime) / 1000000000.0f));
    }

    public float getFqHz() {
        return fqHz;
    }
}
