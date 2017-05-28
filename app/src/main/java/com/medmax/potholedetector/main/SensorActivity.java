package com.medmax.potholedetector.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.multithreading.AccelerometerData;
import com.medmax.potholedetector.multithreading.AccelerometerWorker;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.PotholeDbHelper;
import com.medmax.potholedetector.utilities.TimeHelper;
import java.io.File;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class SensorActivity extends AppCompatActivity implements OnClickListener, SensorEventListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    // constants
    public static final String LOG_TAG = SensorActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final int PERMISSION_REQUEST_CODE = 100;

    // Variables
    private boolean mIsSaving = false;
    private float mOutput[] = { 0, 0, 0 };
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
    private AccelerometerWorker accelerometerWorker;

    //GPS & Google API
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);

        mTvLongitude = (TextView) findViewById(R.id.tv_longitude);
        mTvLatitude = (TextView) findViewById(R.id.tv_latitude);
        mTvSamplingRate = (TextView) findViewById(R.id.tv_sampling_rate);
        mTvStartTime = (TextView) findViewById(R.id.tv_start_time);
        mTvEndTime = (TextView) findViewById(R.id.tv_end_time);

        mButton.setOnClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);

        mDbHelper = PotholeDbHelper.getInstance(this.getApplicationContext());
        String fileName = AppSettings.ACCELEROMETER_RAW_DATA_CSV_FILE + TimeHelper.getCurrentDateTime("yyyyMMdd") + ".csv";

        mFile = new File(
                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
                fileName);

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        accelerometerWorker = new AccelerometerWorker(mDbHelper, mFile);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        accelerometerWorker.close();
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        mIsSaving = !mIsSaving;

        if(mIsSaving){
            mTvStartTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
        } else {
            mTvEndTime.setText(TimeHelper.getCurrentDateTime("yyyy-MM-dd hh:mm:ss"));
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
        applyLowPassFilter(event);

        // Store the data in SQL using a background process.
        final float[] output = mOutput.clone();
        final long timeStamp = event.timestamp;
        final double longitude = mLastKnownLongitude;
        final double latitude = mLastKnownLatitude;

        if(mIsSaving) {
            AccelerometerData data = new AccelerometerData();
            data.timestamp = timeStamp;
            data.deviceName = mDeviceName;
            data.x = mOutput[0];
            data.y = mOutput[1];
            data.z = mOutput[2];
            data.longitude = longitude;
            data.latitude = latitude;

            accelerometerWorker.addNewData(data);
        }

        long endTime = System.nanoTime();
        Log.i(LOG_TAG, String.format("Total execution mCurrentTime: %d ns", (endTime-startTime)));

        // Displays it in the UI
        updateUI(mOutput[0], mOutput[1], mOutput[2], frequency);
    }

    private void applyLowPassFilter(SensorEvent event) {
        final float alpha = 0.82f;
        mOutput[0] = alpha * mOutput[0] + (1 - alpha) * event.values[0];
        mOutput[1] = alpha * mOutput[1] + (1 - alpha) * event.values[1];
        mOutput[2] = alpha * mOutput[2] + (1 - alpha) * event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void updateUI(float x, float y, float z, double frequency){
        mTvAccAxisX.setText(String.valueOf(x));
        mTvAccAxisY.setText(String.valueOf(y));
        mTvAccAxisZ.setText(String.valueOf(z));
        mTvSamplingRate.setText(String.valueOf(frequency));
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(LOG_TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(LOG_TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
        Log.d(LOG_TAG, location.toString());

        mLastKnownLatitude = location.getLatitude();
        mLastKnownLongitude = location.getLongitude();

        mTvLatitude.setText(String.valueOf(mLastKnownLatitude));
        mTvLongitude.setText(String.valueOf(mLastKnownLongitude));
    }
}
