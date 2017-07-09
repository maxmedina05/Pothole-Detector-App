package com.medmax.potholedetector.views;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.data.analyzer.PotholeFinder;

/**
 * Created by Max Medina on 2017-07-06.
 */

public class FinderActivity extends BaseSensorActivity {
    // Constants
    public final static String LOG_TAG = FinderActivity.class.getSimpleName();

    // Analyzer
    private PotholeFinder finder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finder = new PotholeFinder();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getResources().getString(R.string.pref_zthresh_key);
        float ztreshvalue = Float.parseFloat(sharedPrefs.getString(key, "1.4"));
        finder.setzThreshValue(ztreshvalue);
    }

    @Override
    public void myOnClick(View v) {

    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if(startLogger) {
            finder.handleState(z);
            if(finder.isThereAPothole()){
                sendToast("I Pothole was found!");
            }
        }

//        Log.d(LOG_TAG, String.format("StartLogger: %s", String.valueOf(startLogger)));
    }
}
