package com.veha.service;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.veha.activity.R;
import com.veha.util.Util;

import java.util.Map;

public class NotificationHelper {

/*
    private static final String CHANNEL_ID = "VEHA";
    private static final String CHANNEL_NAME = "VEHA";
    private static final String CHANNEL_DESC = "veha notification";*/

    public static void displayNotification(Context context, String title, String body, Map data) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Util.CHANNEL_ID);
        builder.setSmallIcon(R.drawable.logo);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManagerCompat.notify(1, builder.build());
        }
    }
}
