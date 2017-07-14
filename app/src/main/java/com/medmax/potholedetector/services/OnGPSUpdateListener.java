package com.medmax.potholedetector.services;

import android.location.Location;

/**
 * Created by Max Medina on 2017-07-10.
 */

public interface OnGPSUpdateListener {

    void onGPSUpdate(Location location);
}
