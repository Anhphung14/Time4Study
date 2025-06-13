package com.example.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.fragments.FragmentFocusMode;
import com.example.time4study.R;

public class FocusTimeTimerService extends Service {
    private SharedPreferences sharedPreferences;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    public static final String CHANNEL_ID = "timer_channel";
    public static final int NOTIFICATION_ID = 1;
    private static final String ACTION_START_TIMER = "com.arijit.pomodoro.START_TIMER";
    private static final String ACTION_STOP_TIMER = "com.arijit.pomodoro.STOP_TIMER";
    private static final String ACTION_APP_OPENED = "com.arijit.pomodoro.APP_OPENED";
    private static final String EXTRA_FRAGMENT_TYPE = "fragment_type";
    private static final String EXTRA_SESSION_INFO = "session_info";

    public static void startTimer(Context context, String fragmentType, String sessionInfo) {
        Intent intent = new Intent(context, FocusTimeTimerService.class);
        intent.setAction(ACTION_START_TIMER);
        intent.putExtra(EXTRA_FRAGMENT_TYPE, fragmentType);
        intent.putExtra(EXTRA_SESSION_INFO, sessionInfo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        }
    }

    public static void stopTimer(Context context) {
        Intent intent = new Intent(context, FocusTimeTimerService.class);
        intent.setAction(ACTION_STOP_TIMER);
        context.startService(intent);
    }

    public static void appOpened(Context context) {
        Intent intent = new Intent(context, FocusTimeTimerService.class);
        intent.setAction(ACTION_APP_OPENED);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        createNotificationChannel();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sharedPreferences.getBoolean("isAppInForeground", true)) {
                    updateNotification();
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_START_TIMER.equals(action)) {
                    String fragmentType = intent.getStringExtra(EXTRA_FRAGMENT_TYPE);
                    String sessionInfo = intent.getStringExtra(EXTRA_SESSION_INFO);
                    if (fragmentType == null) fragmentType = "focus";
                    if (sessionInfo == null) sessionInfo = "";
                    startService(fragmentType, sessionInfo);
                } else if (ACTION_STOP_TIMER.equals(action)) {
                    stopService();
                } else if (ACTION_APP_OPENED.equals(action)) {
                    sharedPreferences.edit().putBoolean("isAppInForeground", true).apply();
                    updateNotification();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void startService(String fragmentType, String sessionInfo) {
        sharedPreferences.edit()
                .putString("currentFragment", fragmentType)
                .putString("sessionInfo", sessionInfo)
                .putBoolean("timerRunning", true)
                .apply();
        startForeground(NOTIFICATION_ID, createNotification(fragmentType, sessionInfo));
        handler.post(updateRunnable);
    }

    private void stopService() {
        handler.removeCallbacks(updateRunnable);
        sharedPreferences.edit().putBoolean("timerRunning", false).apply();
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows ongoing timer status");
            channel.enableVibration(false);
            channel.setSound(null, null);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String fragmentType, String sessionInfo) {
        Intent notificationIntent = new Intent(this, FragmentFocusMode.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.putExtra("fragmentType", fragmentType);
        notificationIntent.putExtra("sessionInfo", sessionInfo);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long timeLeftInMillis = sharedPreferences.getLong("timeLeftInMillis", 0);
        long minutes = (timeLeftInMillis / 1000) / 60;
        long seconds = (timeLeftInMillis / 1000) % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        String title;
        if ("focus".equals(fragmentType)) {
            title = "Focus Timer";
        } else if ("shortBreak".equals(fragmentType)) {
            title = "Short Break";
        } else if ("longBreak".equals(fragmentType)) {
            title = "Long Break";
        } else {
            title = "Timer";
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(timeText)
                .setSmallIcon(R.drawable.brain)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String fragmentType = sharedPreferences.getString("currentFragment", "focus");
        String sessionInfo = sharedPreferences.getString("sessionInfo", "");
        notificationManager.notify(NOTIFICATION_ID, createNotification(fragmentType, sessionInfo));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
} 