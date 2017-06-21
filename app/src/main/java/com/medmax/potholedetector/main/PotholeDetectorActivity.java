package com.medmax.potholedetector.main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.models.SensorState;
import com.medmax.potholedetector.utilities.AppSettings;
import com.medmax.potholedetector.utilities.PotholeDbHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Max Medina on 2017-06-11.
 */

public class PotholeDetectorActivity extends BaseSensorActivity {
    private float mThresh;

    //State Machine
    protected boolean currentState = false;
    protected boolean previousState = false;

    private String startDate = "";
    private String endDate = "";

    @Override
    protected void configInit() {
        super.configInit();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mThresh = Float.parseFloat(sharedPrefs.getString("pref_thresh", "2"));
    }

    @Override
    protected void logData() {
        super.logData();

        float y = acceleration[1] / AppSettings.GRAVITY_CONSTANT;;
        handleState(y, mThresh);

        yThreshAlgorithm(acceleration, mThresh);
    }

    private void yThreshAlgorithm(float[] data, float thresh) {

    }

    private void handleState(float thresh, float y) {

        // Cayo en el hoyo -> está en el hoyo
        if(currentState && !previousState) {
            previousState = true;
        }

        // Salio del hoyo -> flujo normal
        if(!currentState && previousState) {
            previousState = false;
        }

        // está en el hoyo -> Salio del hoyo
        if(currentState && previousState && y < thresh) {
            previousState = currentState;
            currentState = false;
        }

        // Flujo normal -> Cayo en un hoyo
        if(!currentState && !previousState && y >= thresh){
            previousState = false;
            currentState = true;
        }

        Log.d(LOG_TAG, String.format("State Machine: State: %b\nPrevious: %b %s", currentState, previousState, stateString(new Boolean[] {currentState, previousState})));
    }

    public static String stateString(Boolean[] states){
        if(!states[0] && !states[1])
            return "Flujo Normal";
        if(states[0] && !states[1])
            return "Cayo en un hoyo";
        if(!states[0] && states[1])
            return "Salio del hoyo";
        if(states[0] && states[1])
            return "esta dentro del hoyo";

        return "UNKNOWN STATE";
    }
}
