package com.example.squawker.fcm;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.squawker.MainActivity;
import com.example.squawker.R;
import com.example.squawker.provider.SquawkContract;
import com.example.squawker.provider.SquawkProvider;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final int NOTIFICATION_MAX_CHARACTERS = 30;

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.i("xz", "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    // This is triggered when app is in foreground or background
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        if (data.size() > 0) {
            Log.i("xz", "Message data payload: " + data);

            sendNotification(data);
            insertSquawk(data);
        }

    }


    /**
     * Inserts a single squawk into the database;
     *
     * @param data Map which has the message data in it
     */
    private void insertSquawk(final Map<String, String> data) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(SquawkContract.COLUMN_DATE, data.get("date"));
                values.put(SquawkContract.COLUMN_AUTHOR_KEY, data.get("authorKey"));
                values.put(SquawkContract.COLUMN_AUTHOR, data.get("author"));
                values.put(SquawkContract.COLUMN_MESSAGE, data.get("message"));
                getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, values);
            }
        };

        ExecutorService service = Executors.newSingleThreadExecutor();
        Future future = service.submit(runnable); // Another thread starts

    }


    /**
     * Create and show a simple notification containing the received FCM message
     *
     * @param data Map which has the message data in it
     */
    private void sendNotification(Map<String, String> data) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String author = data.get("author");
        String message = data.get("message");

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        if (message.length() > NOTIFICATION_MAX_CHARACTERS) {
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "channel_1")
                .setSmallIcon(R.drawable.ic_duck)
                .setContentTitle(author)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, notificationBuilder.build());

    }


    public void sendRegistrationToServer(String token) {

    }
}
