package com.medmax.potholedetector.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.utilities.FrequencyCalculator;
import com.medmax.potholedetector.utilities.MyPreferenceManager;

/**
 * Created by Max Medina on 2017-10-17.
 */

public class BaseSensorService implements SensorEventListener, OnGPSUpdateListener {

    // Variables
    protected Context mContext;
    protected char mGravityAxis = 'Y';
    protected float mTimestamp = 0;
    protected String mDeviceName = "";

    // Sensor Variables
    private FrequencyCalculator mFrequencyCalculator;
    protected SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    protected volatile float[] mRawAccelerometerValues = new float[3];
    protected volatile float[] mRawMagneticValues = new float[3];

    private GPSManager mGPSManager;
    protected float mCarMovingSpeed = 0;
    protected float lastLongitude = 0;
    protected float lastLatitude = 0;

    // Preferences
    protected MyPreferenceManager mPreferenceManager;

    public BaseSensorService(Context context) {
        mContext = context;
    }

    public void onCreate(){
        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
        mFrequencyCalculator = new FrequencyCalculator();

        mPreferenceManager = MyPreferenceManager.getInstance();
        mPreferenceManager.loadPreferenceParameters(mContext.getResources(), mContext);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometerSensor, AppSettings.SAMPLING_RATE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), AppSettings.SAMPLING_RATE);

        mGPSManager = new GPSManager(mContext);
        mGPSManager.setOnGPSUpdateListener(this);
        mGPSManager.requestLocationUpdates();
    }

    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        mGPSManager.removeLocationUpdates();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                onAccelerometerSensorChanged(event.values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                onMagneticSensorChanged(event.values);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onGPSUpdate(Location location) {
        if (location.hasSpeed()) {
            mCarMovingSpeed = location.getSpeed() * AppSettings.SPEED_CONSTANT;
        }
        lastLatitude = (float) location.getLatitude();
        lastLongitude = (float) location.getLongitude();
    }

    protected void onAccelerometerSensorChanged(float[] values) {
        mFrequencyCalculator.calculateFrequency();
        System.arraycopy(values, 0, mRawAccelerometerValues, 0, values.length);
    }

    protected void onMagneticSensorChanged(float[] values) {
        System.arraycopy(values, 0, mRawMagneticValues, 0, values.length);

    }
}
