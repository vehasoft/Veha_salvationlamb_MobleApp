package com.veha.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.veha.util.UserPreferences;
import com.veha.util.Util;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (message.getNotification() != null) {
            Log.e("notification", message.getNotification().getTitle());
            Log.e("notification", message.getNotification().getBody());
            Log.e("notification", message.getData().toString());
            String title = message.getNotification().getTitle();
            String text = message.getNotification().getBody();
            message.getData();
            NotificationHelper.displayNotification(getApplicationContext(), title, text, message.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateToken(token);
    }

    void updateToken(String token) {
        try {
            UserPreferences userPreferences = new UserPreferences(this);
            JsonObject data = new JsonObject();
            data.addProperty("userID", Util.userId);
            data.addProperty("token", token);
            data.addProperty("oldToken", String.valueOf(userPreferences.getFcmToken()));
            //userPreferences.savefcmToken(token);

        } catch (Exception e) {
            Log.e("error while updating token", e.toString());
        }
    }
}
