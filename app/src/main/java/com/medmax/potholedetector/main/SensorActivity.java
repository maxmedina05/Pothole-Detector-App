package com.medmax.potholedetector.main;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.AccelerometerDataContract.AccelerometerReading;
import com.medmax.potholedetector.multithreading.ThreadPoolManager;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.PotholeDbHelper;
import com.medmax.potholedetector.utilities.TimeHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

public class SensorActivity extends AppCompatActivity implements OnClickListener, SensorEventListener, LocationListener {

    // constants
    public static final String LOG_TAG = SensorActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    private boolean mIsSaving = false;
    private boolean mIsFilterEn = true;
    private float mOutput[] = {0, 0, 0};
    private PotholeDbHelper mDbHelper;
    private String mDeviceName;
    private File mFile;
    private double mLastKnownLatitude;
    private double mLastKnownLongitude;
    float mCurrentTime = System.nanoTime();
    float mLaunchTime = System.nanoTime();
    int count = 0;

    // UI Components
    private ToggleButton mButton;
    private ToggleButton mBtnFilter;
    private TextView mTvAccAxisX;
    private TextView mTvAccAxisY;
    private TextView mTvAccAxisZ;
    private TextView mTvLongitude;
    private TextView mTvLatitude;
    private TextView mTvSamplingRate;
    private TextView mTvStartTime;
    private TextView mTvEndTime;

    // Sensor's variables
    private SensorManager mSensorManager;
    private Sensor mSensor;

    // Multithreading
    private ThreadPoolManager mThreadPool;
//    private AccelerometerWorker accelerometerWorker;

    //GPS & Google API
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mBtnFilter = (ToggleButton) findViewById(R.id.btn_en_filter);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);

        mTvLongitude = (TextView) findViewById(R.id.tv_longitude);
        mTvLatitude = (TextView) findViewById(R.id.tv_latitude);
        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);
        mTvStartTime = (TextView) findViewById(R.id.tv_start_time);
        mTvEndTime = (TextView) findViewById(R.id.tv_end_time);

        mButton.setOnClickListener(this);
        mBtnFilter.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);

        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());
        String fileName = AppSettings.ACCELEROMETER_RAW_DATA_CSV_FILE + TimeHelper.getCurrentDateTime("yyyyMMdd") + ".csv";

        mFile = new File(
                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
                fileName);

        // Create the LocationRequest object
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        handleNewLocation(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        mThreadPool = ThreadPoolManager.getsInstance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    protected void onDestroy() {
        mThreadPool.cancelAllTasks();
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnToggle:
                mIsSaving = !mIsSaving;

                if(mIsSaving){
                    mTvStartTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
                } else {
                    mTvEndTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
                }
                break;
            case R.id.btn_en_filter:
                mIsFilterEn = !mIsFilterEn;
                break;

            default:
                break;
        }

    }

    /**
     * This method reads the data from the accelerometer, processes it and then it saves it to a database.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        long startTime = System.nanoTime();
        // Capture Data
        mCurrentTime = System.nanoTime();

        // The event timestamps are irregular so we average to determine the
        // update frequency instead of measuring deltas.
        double frequency = count++ / ((mCurrentTime - mLaunchTime) / 1000000000.0f);

        // Process Data
        if(mIsFilterEn) {
            applyLowPassFilter(event);
//            Log.d(LOG_TAG, "Low Pass Filter applied!");
        }

        // Store the data in SQL using a background process.
        final float[] output = (mIsFilterEn) ? mOutput.clone() : event.values.clone();
        final long timeStamp =  System.nanoTime();
        final double longitude = mLastKnownLongitude;
        final double latitude = mLastKnownLatitude;

        if(mIsSaving) {
            mThreadPool.addCallable(new Callable() {
                @Override
                public Object call() throws Exception {
                    saveDataToCSV(timeStamp, mDeviceName, output[0], output[1], output[2], longitude, latitude);
                    saveDatatoDB(timeStamp, mDeviceName, output[0], output[1], output[2], longitude, latitude);
//                    Log.i(LOG_TAG, "onSensorChanged - background task done!");
                    return null;
                }
            });
        }

        long endTime = System.nanoTime();
//        Log.i(LOG_TAG, String.format("Total execution time: %d ns", (endTime-startTime)));

        // Displays it in the UI
        updateUI(mOutput[0], mOutput[1], mOutput[2], frequency);
    }

    private void applyLowPassFilter(SensorEvent event) {
        final float alpha = 0.82f;
        mOutput[0] = alpha * mOutput[0] + (1 - alpha) * event.values[0];
        mOutput[1] = alpha * mOutput[1] + (1 - alpha) * event.values[1];
        mOutput[2] = alpha * mOutput[2] + (1 - alpha) * event.values[2];
    }

    private synchronized void saveDatatoDB(long timestamp, String deviceName, float x, float y, float z, double longitude, double latitude) {
        if(mDbHelper != null) {
            ContentValues values = new ContentValues();
            values.put(AccelerometerReading.COLUMN_NAME_TIME, timestamp);
            values.put(AccelerometerReading.COLUMN_NAME_DEVICE_NAME, deviceName);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_X_AXIS, x);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Y_AXIS, y);
            values.put(AccelerometerReading.COLUMN_NAME_ACC_Z_AXIS, z);

            values.put(AccelerometerReading.COLUMN_NAME_LONGITUDE, longitude);
            values.put(AccelerometerReading.COLUMN_NAME_LATITUDE, latitude);

            long newId = mDbHelper.getWritableDatabase().insert(AccelerometerReading.TABLE_NAME, null, values);
        }
    }

    private synchronized void saveDataToCSV(long timespan, String deviceName, float x, float y, float z, double longitude, double latitude) {
        try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {
            bwriter.write(String.format("%d, %s, %f, %f, %f, %f, %f", timespan, deviceName, x, y, z, longitude, latitude));
            bwriter.newLine();
            bwriter.close();


        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
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

    private void updateUI(float x, float y, float z, double frequency){
        mTvAccAxisX.setText(String.valueOf(x));
        mTvAccAxisY.setText(String.valueOf(y));
        mTvAccAxisZ.setText(String.valueOf(z));
        mTvSamplingRate.setText(String.valueOf(frequency));
    }

    private void handleNewLocation(Location location) {
        if(location != null) {
//            Log.d(LOG_TAG, location.toString());

            mLastKnownLatitude = location.getLatitude();
            mLastKnownLongitude = location.getLongitude();

            mTvLatitude.setText(String.valueOf(mLastKnownLatitude));
            mTvLongitude.setText(String.valueOf(mLastKnownLongitude));
        }
    }
}
