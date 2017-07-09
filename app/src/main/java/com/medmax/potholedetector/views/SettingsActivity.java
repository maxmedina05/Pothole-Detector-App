package com.medmax.potholedetector.views;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.medmax.potholedetector.R;

/**
 * Created by maxme on 2017-07-08.
 */

public class SettingsActivity extends PreferenceActivity {

    public final static String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
