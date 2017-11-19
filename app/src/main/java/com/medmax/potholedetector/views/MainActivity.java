package com.medmax.potholedetector.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toolbar;
import android.Manifest;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.medmax.potholedetector.R;
import com.medmax.potholedetector.config.AppSettings;
import com.medmax.potholedetector.services.StreetDefectDetectorService;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Max Medina on 2017-10-17.
 */

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    public static String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 9001;
    private static final int MY_PERMISSIONS_REQUEST_ACCOUNTS = 100;

    private boolean isServiceRunning = false;
    private GoogleApiClient mGoogleApiClient;

    private TextView displayNameTextView;
    private SignInButton signInButton;
    private Button logoutButton;
    private ToggleButton toggleButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        toggleButton = (ToggleButton) findViewById(R.id.pothole_detector_toggle);
        displayNameTextView = (TextView) findViewById(R.id.display_name_text_view);
        logoutButton = (Button) findViewById(R.id.sign_out_button);
        signInButton = (SignInButton) findViewById(R.id.sign_in_button);

        setActionBar(toolbar);
        signInButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);
        toggleButton.setOnClickListener(this);

        if(!checkAndRequestPermissions()) {
            this.finish();
        }

        setupGoogleOauth2();
    }

    private void setupGoogleOauth2() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                updateLoginButtons(false);
            }
        });
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String token = null;
            if (acct != null) {
                displayNameTextView.setText(String.format("Hello %s!", acct.getDisplayName()));
                token = acct.getIdToken();
                Log.d(LOG_TAG, token);

                SharedPreferences sharedPref = this.getSharedPreferences(AppSettings.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.saved_token_id), token);
                editor.commit();
                updateLoginButtons(true);
            }
        } else {
            this.finish();
        }
    }

    private boolean checkAndRequestPermissions() {
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> permissionsNeeded = new ArrayList<>();

        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(
                            new String[permissionsNeeded.size()]),
                    MY_PERMISSIONS_REQUEST_ACCOUNTS);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCOUNTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Permission Granted Successfully. Write working code here.
                } else {
                    //You did not accept the request can not use the functionality.
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("NO PERMISSION");
                    builder.setMessage("The app will not work if the required permissions are not enabled!");
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            MainActivity.this.finish();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;
        }
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
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pothole_detector_toggle:
                onPotholeDetectorToggleClicked();
                break;
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
        }
    }

    private void onPotholeDetectorToggleClicked() {
        isServiceRunning = !isServiceRunning;

        Intent intent = new Intent(getApplicationContext(), StreetDefectDetectorService.class);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (isServiceRunning) {
            startService(intent);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentTitle("Pothole Finder")
                    .setContentText("service is running!")
                    .setOngoing(true);
            notificationManager.notify(AppSettings.STREET_DEFECT_DETECTOR_NOTIFY_ID, builder.build());
        } else {
            stopService(intent);
            notificationManager.cancelAll();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void updateLoginButtons(boolean isLoggedIn){
        if(isLoggedIn) {
            signInButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            toggleButton.setEnabled(true);

        } else {
            signInButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            toggleButton.setEnabled(false);
        }
    }
}
