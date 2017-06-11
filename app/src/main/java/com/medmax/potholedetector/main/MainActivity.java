package com.medmax.potholedetector.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.medmax.potholedetector.R;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    // UI Components
    private Button mBtnPotholeAnalyzer;
    private Button mBtnRegisterNewPothole;
    private Button mBtnExport;
    private Button mBtnSettings;
    private Button mBtnPotholeDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mBtnPotholeAnalyzer = (Button) findViewById(R.id.btn_analyze);
        mBtnRegisterNewPothole = (Button) findViewById(R.id.btn_register);
        mBtnExport = (Button) findViewById(R.id.btn_export);
        mBtnSettings = (Button) findViewById(R.id.btn_settings);
        mBtnPotholeDetector = (Button) findViewById(R.id.btn_detector);

        mBtnPotholeAnalyzer.setOnClickListener(this);
        mBtnRegisterNewPothole.setOnClickListener(this);
        mBtnExport.setOnClickListener(this);
        mBtnSettings.setOnClickListener(this);
        mBtnPotholeDetector.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        Intent intent;
        switch (viewId){
            case R.id.btn_analyze:
                intent = new Intent(MainActivity.this, AnalyzerActivity.class);
                break;

            case R.id.btn_register:
                intent = new Intent(MainActivity.this, RegisterActivity.class);
                break;

            case R.id.btn_export:
                intent = new Intent(MainActivity.this, RegisterActivity.class);
                break;

            case R.id.btn_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                break;

            case R.id.btn_detector:
                intent = new Intent(MainActivity.this, PotholeDetectorActivity.class);
                break;
            default:
                // TODO: change to settings or something else
                intent = new Intent(MainActivity.this, AnalyzerActivity.class);
                break;
        }

        startActivity(intent);
    }

}
