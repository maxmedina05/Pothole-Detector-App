package com.medmax.potholedetector.utilities;

/**
 * Copied by Max Medina on 2017-05-27.
 * From http://www.kircherelectronics.com/blog/index.php/11-android/sensors/8-low-pass-filter-the-basics
 */

/**
 * This is a low pass filter class
 * It filter data prevents from the accelerometer above a certain frequency
 * This is needed to removed the noise generated by the accelerometer and also because every
 * android phone has a different sampling rate.
 *
 */
public class LowPassFilter {
    // Time constant in seconds
    static final float timeConstant = 0.297f;
    private float alpha = 0.0f;
    private float timestamp = System.nanoTime();
    private float timestampOld = System.nanoTime();
    private int count = 0;

    public void dynamicLowPass(float[] input, float[] output)
    {
        timestamp = System.nanoTime();

        // Find the sample period (between updates).
        // Convert from nanoseconds to seconds
        float dt = 1 / (count / ((timestamp - timestampOld) / 1000000000.0f));

        count++;

        // Calculate alpha
        alpha = timeConstant / (timeConstant + dt);

        output[0] = alpha * output[0] + (1 - alpha) * input[0];
        output[1] = alpha * output[1] + (1 - alpha) * input[1];
        output[2] = alpha * output[2] + (1 - alpha) * input[2];
    }
}

