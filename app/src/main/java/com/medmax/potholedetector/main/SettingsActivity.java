package com.medmax.potholedetector.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.medmax.potholedetector.R;

/**
 * Created by Max Medina on 2017-06-11.
 */

public class SettingsActivity extends PreferenceActivity {

    public final static String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
