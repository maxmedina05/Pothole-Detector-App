package com.medmax.potholedetector.views.Helpers;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Max Medina on 2017-07-08.
 */

public class ToastHelper {
    public static void sendToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
