package com.medmax.potholedetector.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toolbar;

import com.medmax.potholedetector.R;

/**
 *  Max Medina
 */
public class MainActivity extends Activity implements View.OnClickListener {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    // UI Components
    Button btnLogger;
    Button btnFinder;
    Button btnFinderEx;
    Button btnVirtualOrientation;
    Button btnVoFinder;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setActionBar(myToolbar);

        btnLogger = (Button)findViewById(R.id.btn_logger);
        btnFinder = (Button)findViewById(R.id.btn_finder);
        btnFinderEx = (Button)findViewById(R.id.btn_finder_ex);
//        btnVirtualOrientation = (Button) findViewById(R.id.btn_virtual_orientation);
//        btnVoFinder = (Button) findViewById(R.id.btn_vo_finder);

        btnLogger.setOnClickListener(this);
        btnFinder.setOnClickListener(this);
        btnFinderEx.setOnClickListener(this);
//        btnVirtualOrientation.setOnClickListener(this);
//        btnVoFinder.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        Intent intent;
        switch (v.getId()){

            case R.id.btn_finder:
                Log.d(LOG_TAG, "btn_finder");
                intent = new Intent(MainActivity.this, FinderActivity.class);
                break;

            case R.id.btn_finder_ex:
                Log.d(LOG_TAG, "btn_finder_ex");
                intent = new Intent(MainActivity.this, FinderExActivity.class);
                break;

            case R.id.btn_logger:
                Log.d(LOG_TAG, "btn_logger");
                intent = new Intent(MainActivity.this, LoggerActivity.class);
                break;

//            case R.id.btn_virtual_orientation:
//                Log.d(LOG_TAG, "btn_virtual_orientation");
//                intent = new Intent(MainActivity.this, VirtualOLoggerActivity.class);
//                break;
//
//            case R.id.btn_vo_finder:
//                Log.d(LOG_TAG, "btn_vo_finder");
//                intent = new Intent(MainActivity.this, VirtualOFinderActivity.class);
//                break;
            default:
                intent = new Intent(MainActivity.this, LoggerActivity.class);
                break;
        }

        startActivity(intent);
    }

}
