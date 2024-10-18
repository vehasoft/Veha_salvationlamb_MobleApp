package com.veha.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.veha.activity.ApproveRequestActivity;
import com.veha.activity.R;
import com.veha.activity.ViewPostActivity;
import com.veha.activity.ViewProfileActivity;
import com.veha.util.NotificationType;
import com.veha.util.Permission;
import com.veha.util.PermissionType;
import com.veha.util.Util;

import java.util.Map;
import java.util.Objects;

public class NotificationHelper {

/*
    private static final String CHANNEL_ID = "VEHA";
    private static final String CHANNEL_NAME = "VEHA";
    private static final String CHANNEL_DESC = "veha notification";*/

    public static void displayNotification(Context context, String title, String body, Map data) {
        PendingIntent pendingIntent = null;
        if (Objects.equals(data.get("type"), NotificationType.POST.getValue())) {
            if (Util.hasPermission(PermissionType.POST.getValue(), Permission.READ.getValue())) {
                Intent intent = new Intent(context, ViewPostActivity.class);
                intent.putExtra("postId", data.get("id").toString());
                intent.putExtra("type", NotificationType.POST.getValue());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            }
        } else if (Objects.equals(data.get("type"), NotificationType.USER.getValue())) {
            Intent intent = new Intent(context, ViewProfileActivity.class);
            intent.putExtra("userId", data.get("id").toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else if (Objects.equals(data.get("type"), NotificationType.ANNOUNCEMENT.getValue())) {
            if (Util.hasPermission(PermissionType.ANNOUNCEMENT.getValue(), Permission.READ.getValue())) {
                Intent intent = new Intent(context, ViewPostActivity.class);
                intent.putExtra("type", NotificationType.ANNOUNCEMENT.getValue());
                intent.putExtra("postId", data.get("id").toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            }
        } else if (Objects.equals(data.get("type"), NotificationType.WARRIOR.getValue())) {
            if (Util.hasPermission(PermissionType.USER.getValue(), Permission.EDIT.getValue())) {
                Intent intent = new Intent(context, ApproveRequestActivity.class);
                intent.putExtra("userId", data.get("id").toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            }
        }
        //Bitmap icon = BitmapFactory.decodeResource(context.getResources(),R.drawable.logo);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Util.CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.fav_icon_round);
        builder.setColor(context.getColor(R.color.primary_blue));
        //builder.setLargeIcon(icon);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setContentIntent(pendingIntent);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManagerCompat.notify(1, notification);
        }
    }
}
