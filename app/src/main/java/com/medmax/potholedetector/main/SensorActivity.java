package com.medmax.potholedetector.main;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;

public class SensorActivity extends AppCompatActivity implements OnClickListener, SensorEventListener {

    // constants
    public static final String LOG_TAG = SensorActivity.class.getSimpleName();
    private int SAMPLING_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    // Variables
    private boolean mIsSaving = false;
    private float mOutput[] = { 0, 0, 0 };

    // UI Components
    private ToggleButton mButton;
    private TextView mTvAccAxisX;
    private TextView mTvAccAxisY;
    private TextView mTvAccAxisZ;

    // Sensor's variables
    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        mButton = (ToggleButton) findViewById(R.id.btnToggle);
        mTvAccAxisX = (TextView) findViewById(R.id.acc_axis_x);
        mTvAccAxisY = (TextView) findViewById(R.id.acc_axis_y);
        mTvAccAxisZ = (TextView) findViewById(R.id.acc_axis_z);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SAMPLING_RATE);
    }

    @Override
    public void onClick(View v) {
        mIsSaving = !mIsSaving;
    }

    /**
     * This method reads the data from the accelerometer, processes it and then it saves it to a database.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Capture Data
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Process Data
        applyLowPassFilter(event);

        // Store the data in SQL using a background process.

        // Displays it in the UI
        updateUI(mOutput[0], mOutput[1], mOutput[2]);
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

    private void updateUI(float x, float y, float z){
        mTvAccAxisX.setText(String.valueOf(x));
        mTvAccAxisY.setText(String.valueOf(y));
        mTvAccAxisZ.setText(String.valueOf(z));
    }
}
