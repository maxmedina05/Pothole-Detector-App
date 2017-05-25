package com.medmax.potholedetector.main;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.medmax.potholedetector.R;

public class SensorReaderActivity extends AppCompatActivity {

    private Button mBtnSensorApp;
    private Button mBtnPotholeRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_reader);

        mBtnSensorApp = (Button)findViewById(R.id.btn_sensor_view);
        mBtnPotholeRegister = (Button)findViewById(R.id.btn_form_view);

        mBtnSensorApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SensorReaderActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        mBtnPotholeRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SensorReaderActivity.this, PotholeRegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
