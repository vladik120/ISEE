package com.example.isee;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ForegroundForSpeechService extends Service {
    public NotificationManager notificationManager = null;
    public Notification.Builder notificationBuilder = null;
    public int NOTIFICATION_ID_1 = 1;
    private Thread onGoing = null;
    private boolean threadAction = false;
    private boolean thread = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        initForeground();
        initThread();
    }

    private void initThread() {
        if(!thread) {
            thread = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (threadAction) {
                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        updateNotification("ISEE is on.");
                    }
                }
            }).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("onStartCommand", "in");
        switch(intent.getExtras().getString("command")){
            case "ON":
                Log.e("onStartCommand", "ON");
                initForeground();
                initThread();
                startForegroundThread();
                break;
            case "OFF":
                Log.e("onStartCommand", "OFF");
                stopForegroundThread();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundThread() {
        startForeground(NOTIFICATION_ID_1 , updateNotification ("ISEE is on."));
        initThread();
        threadAction = true;
    }

    private void stopForegroundThread() {
        threadAction = false;
        stopForeground(true);
    }

    private void initForeground(){
        String CHANNEL_ID = "my_channel_01";
        if (notificationManager == null)
            notificationManager = (NotificationManager) getSystemService (Context. NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My_main_channel",
                NotificationManager. IMPORTANCE_DEFAULT);
        ((NotificationManager ) getSystemService (Context. NOTIFICATION_SERVICE))
                .createNotificationChannel (channel);
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent( this , MainActivity. class);
        intent.setFlags( Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity (this , 0 , intent, PendingIntent.FLAG_MUTABLE);
        notificationBuilder = new Notification.Builder(this , CHANNEL_ID)
                .setContentTitle("")
                .setSmallIcon (R.drawable.app_notification_icon)
                .setColor(getResources().getColor(R.color.white))
                .setContentIntent(pendingIntent);
    }

    public Notification updateNotification (String details){
        notificationBuilder.setContentText(details).setOnlyAlertOnce(false);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        notificationManager.notify(NOTIFICATION_ID_1 , notification);
        return notification;
    }

}
