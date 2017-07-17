package com.medmax.potholedetector.data.analyzer;

import android.util.Log;

/**
 * Created by Max Medina on 2017-07-08.
 */

public class PotholeFinder {
    private static final String LOG_TAG = PotholeFinder.class.getSimpleName();
    private boolean currentState = false;
    private boolean previousState = false;
    private float zThreshValue = 1.4f;

    public void setzThreshValue(float value) {
        zThreshValue = value;
    }

    public void handleState(float v) {
        // Cayo en el hoyo -> está en el hoyo
        if(currentState && !previousState) {
            previousState = true;
        }

        // Salio del hoyo -> flujo normal
        if(!currentState && previousState) {
            previousState = false;
        }

        // está en el hoyo -> Salio del hoyo
        if(currentState && previousState && v < zThreshValue) {
            previousState = currentState;
            currentState = false;
        }

        // Flujo normal -> Cayo en un hoyo
        if(!currentState && !previousState && v >= zThreshValue){
            previousState = false;
            currentState = true;
        }

        Log.d(LOG_TAG, String.format("State Machine: State: %b\nPrevious: %b %s", currentState, previousState, stateString(new Boolean[] {currentState, previousState})));

    }

    private boolean hasFallenInPothole(){
        return (currentState && !previousState);
    }

    private boolean hasExitPothole() {
        return (!currentState && previousState);
    }

    private static String stateString(Boolean[] states){
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

    public boolean isThereAPothole() {
        return hasExitPothole();
    }
}
