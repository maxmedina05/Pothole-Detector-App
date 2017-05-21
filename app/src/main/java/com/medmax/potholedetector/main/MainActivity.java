package com.medmax.potholedetector.main;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.medmax.potholedetector.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public String LOG_TAG = "MainActivity";
    public String fileName = "POTHOLE";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private File mFile;

    private TextView xAxis;
    private TextView yAxis;
    private TextView zAxis;
    private long lastUpdate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xAxis = (TextView) findViewById(R.id.xaxis);
        yAxis = (TextView) findViewById(R.id.yaxis);
        zAxis = (TextView) findViewById(R.id.zaxis);

        
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datenow = sdf.format(c.getTime());
        fileName += datenow + ".csv";

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mFile = new File(
                this.getExternalFilesDir(Environment.MEDIA_MOUNTED),
                fileName);


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                xAxis.setText(Float.toString(x));
                yAxis.setText(Float.toString(y));
                zAxis.setText(Float.toString(z));

                try (BufferedWriter bwriter = new BufferedWriter(new FileWriter(mFile, true))) {
                    bwriter.write(String.format("%d, NEXUS_5X, %f, %f, %f", curTime, x, y, z));
                    bwriter.newLine();
                    bwriter.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.toString());
                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
