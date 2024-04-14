package com.veha.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (message.getNotification() != null){
            String title = message.getNotification().getTitle();
            String text = message.getNotification().getBody();
            message.getData();
            Log.e("message", message.getData().toString());
            NotificationHelper.displayNotification(getApplicationContext(),title,text,message.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

    }
}
