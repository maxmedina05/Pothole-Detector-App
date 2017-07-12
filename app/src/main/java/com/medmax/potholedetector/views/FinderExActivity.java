package com.medmax.potholedetector.views;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.medmax.potholedetector.BaseSensorActivity;
import com.medmax.potholedetector.R;

/**
 * Created by Max Medina on 2017-07-11.
 */

public class FinderExActivity extends BaseSensorActivity {

    ListView mListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] StringArray = {"1", "2", "3"};
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, StringArray);

        mListView = (ListView)findViewById(R.id.list_view_defects);
        mListView.setAdapter(adapter);
    }

    @Override
    protected void setUILayout() {
        setContentView(R.layout.activity_finder_ex);
    }

    @Override
    public void myOnClick(View v) {

    }

    @Override
    public void myOnSensorChanged(SensorEvent event) {

    }
}
