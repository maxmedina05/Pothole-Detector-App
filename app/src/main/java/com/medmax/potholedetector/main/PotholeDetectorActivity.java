package com.medmax.potholedetector.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.medmax.potholedetector.R;

/**
 * Created by maxme on 2017-06-11.
 */

public class PotholeDetectorActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detector);
    }
}
