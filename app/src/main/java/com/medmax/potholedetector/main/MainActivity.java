package com.medmax.potholedetector.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.medmax.potholedetector.R;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private Button mBtnPotholeAnalyzer;
    private Button mBtnRegisterNewPothole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnPotholeAnalyzer = (Button) findViewById(R.id.btn_pothole_analyzer);
        mBtnRegisterNewPothole = (Button) findViewById(R.id.btn_register_pothole);
        mBtnPotholeAnalyzer.setOnClickListener(this);
        mBtnRegisterNewPothole.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        Intent intent;
        switch (viewId){
            case R.id.btn_pothole_analyzer:
                intent = new Intent(MainActivity.this, AnalyzerActivity.class);
                break;

            case R.id.btn_register_pothole:
                intent = new Intent(MainActivity.this, RegisterActivity.class);
                break;
            default:
                // TODO: change to settings or something else
                intent = new Intent(MainActivity.this, AnalyzerActivity.class);
                break;
        }

        startActivity(intent);
    }

}
