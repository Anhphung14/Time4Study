package com.example.time4study.AcFocusMode;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.example.time4study.R;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

public class FocusTimeSettingsActivity extends AppCompatActivity {
    private static final String TAG = "FocusTimeSettings";
    private ImageView backBtn;
    private EditText focusedTimeTxt, shortBreakTxt, longBreakTxt, sessionsTxt, alarmTxt;
    private Slider focusedTimeSlider, shortBreakSlider, longBreakSlider, sessionsSlider, alarmSlider;
    private SwitchCompat autoStartSessions, brownNoiseToggle, whiteNoiseToggle, rainfallToggle, lightJazzToggle, keepScreenAwakeToggle, hapticFeedbackToggle;
    private SharedPreferences sharedPreferences;
    private CardView githubCard, supportCard, statsCard;
    private TextView settingsTxt, uiSettingsTxt, aboutTheAppTxt, runningTimerTxt, madeWithLoveTxt;
    private LinearLayout uiSettingsComponents, timerSettingsComponents;
    private final String CHANNEL_ID = "download_channel";
    private final int NOTIFICATION_ID = 1;
    private PowerManager.WakeLock wakeLock = null;
    private boolean isUpdatingSlider = false;
    public static final int RESULT_TIMER_SETTINGS_CHANGED = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting activity");

        sharedPreferences = getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        setContentView(R.layout.focustime_activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.focusTimeSettings), (v, insets) -> {
            android.graphics.Insets systemBars = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return insets;
        });

        initializeViews();
        loadSavedSettings();
        setupListeners();
        applyTheme();
        checkTimerState();
        createNotificationChannel();
        initializeMusicToggles();
        setupMusicToggleListeners();

        statsCard.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(FocusTimeSettingsActivity.this, FocusTimeStatsActivity.class));
        });

        Log.d(TAG, "onCreate: Activity created successfully");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: Configuration changed");

        applyTheme();
    }

    private void checkTimerState() {
        boolean isTimerRunning = sharedPreferences.getBoolean("isTimerRunning", false);
        boolean isBreakActive = sharedPreferences.getBoolean("isBreakActive", false);
        Log.d(TAG, "checkTimerState: TimerRunning=" + isTimerRunning + ", BreakActive=" + isBreakActive);

        if (isTimerRunning || isBreakActive) {
            timerSettingsComponents.setVisibility(View.GONE);
            uiSettingsTxt.setVisibility(View.GONE);
            uiSettingsComponents.setVisibility(View.GONE);
            runningTimerTxt.setVisibility(View.VISIBLE);
            Log.d(TAG, "checkTimerState: Timer is running, hiding settings");
        } else {
            timerSettingsComponents.setVisibility(View.VISIBLE);
            uiSettingsTxt.setVisibility(View.VISIBLE);
            uiSettingsComponents.setVisibility(View.VISIBLE);
            runningTimerTxt.setVisibility(View.GONE);
            Log.d(TAG, "checkTimerState: Timer is not running, showing settings");
        }
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews: Initializing all views");

        backBtn = findViewById(R.id.back_btn);
        focusedTimeTxt = findViewById(R.id.focused_time_txt);
        focusedTimeSlider = findViewById(R.id.slider_focused_time);
        shortBreakTxt = findViewById(R.id.short_break_txt);
        shortBreakSlider = findViewById(R.id.slider_short_break);
        longBreakTxt = findViewById(R.id.long_break_txt);
        longBreakSlider = findViewById(R.id.slider_long_break);
        sessionsTxt = findViewById(R.id.sessions_txt);
        sessionsSlider = findViewById(R.id.slider_sessions);
        autoStartSessions = findViewById(R.id.auto_start_toggle);
        settingsTxt = findViewById(R.id.settings_txt);
        alarmTxt = findViewById(R.id.alarm_txt);
        alarmSlider = findViewById(R.id.slider_alarm);
        uiSettingsTxt = findViewById(R.id.ui_settings_txt);
        runningTimerTxt = findViewById(R.id.running_timer_txt);
        uiSettingsComponents = findViewById(R.id.ui_settings_components);
        timerSettingsComponents = findViewById(R.id.timer_settings_components);
        brownNoiseToggle = findViewById(R.id.brown_noise_toggle);
        whiteNoiseToggle = findViewById(R.id.white_noise_toggle);
        rainfallToggle = findViewById(R.id.rainfall_toggle);
        lightJazzToggle = findViewById(R.id.light_jazz_toggle);
        keepScreenAwakeToggle = findViewById(R.id.keep_screen_awake_toggle);
        hapticFeedbackToggle = findViewById(R.id.haptic_feedback_toggle);
        statsCard = findViewById(R.id.stats_card);

        Log.d(TAG, "initializeViews: All views initialized");
    }

    private void loadSavedSettings() {
        Log.d(TAG, "loadSavedSettings: Loading saved settings from SharedPreferences");

        focusedTimeSlider.setValue(Math.max(1, sharedPreferences.getInt("focusedTime", 25)));
        shortBreakSlider.setValue(Math.max(1, sharedPreferences.getInt("shortBreak", 5)));
        longBreakSlider.setValue(Math.max(1, sharedPreferences.getInt("longBreak", 10)));
        sessionsSlider.setValue(Math.max(1, sharedPreferences.getInt("sessions", 4)));
        alarmSlider.setValue(Math.max(1, sharedPreferences.getInt("alarmDuration", 3)));
        autoStartSessions.setChecked(sharedPreferences.getBoolean("autoStart", false));
        boolean keepScreenAwake = sharedPreferences.getBoolean("keepScreenAwake", false);
        keepScreenAwakeToggle.setChecked(keepScreenAwake);
        updateWakeLock(keepScreenAwake);
        hapticFeedbackToggle.setChecked(sharedPreferences.getBoolean("hapticFeedback", true));
        updateTexts();

        Log.d(TAG, "loadSavedSettings: Settings loaded successfully");
    }

    private void applyTheme() {
        ScrollView mainLayout = findViewById(R.id.focusTimeSettings);
        mainLayout.setBackgroundColor(getResources().getColor(R.color.white));
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up all listeners");

        backBtn.setOnClickListener(v -> {
            vibrate();
            finish();
        });

        keepScreenAwakeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("keepScreenAwake", isChecked).apply();
            updateWakeLock(isChecked);
        });

        hapticFeedbackToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("hapticFeedback", isChecked).apply();
        });

        autoStartSessions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("autoStart", isChecked).apply();
            markTimerSettingsModified();
        });

        focusedTimeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!isUpdatingSlider) {
                focusedTimeTxt.setText(String.valueOf((int) value));
                sharedPreferences.edit().putInt("focusedTime", (int) value).apply();
                markTimerSettingsModified();
            }
        });

        shortBreakSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!isUpdatingSlider) {
                shortBreakTxt.setText(String.valueOf((int) value));
                sharedPreferences.edit().putInt("shortBreak", (int) value).apply();
                markTimerSettingsModified();
            }
        });

        longBreakSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!isUpdatingSlider) {
                longBreakTxt.setText(String.valueOf((int) value));
                sharedPreferences.edit().putInt("longBreak", (int) value).apply();
                markTimerSettingsModified();
            }
        });

        sessionsSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!isUpdatingSlider) {
                sessionsTxt.setText(String.valueOf((int) value));
                sharedPreferences.edit().putInt("sessions", (int) value).apply();
                markTimerSettingsModified();
            }
        });

        alarmSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!isUpdatingSlider) {
                alarmTxt.setText(String.valueOf((int) value));
                sharedPreferences.edit().putInt("alarmDuration", (int) value).apply();
            }
        });

        Log.d(TAG, "setupListeners: All listeners set up");
    }

    private void markTimerSettingsModified() {
        sharedPreferences.edit().putBoolean("wereTimerSettingsModified", true).apply();
        setResult(RESULT_TIMER_SETTINGS_CHANGED);
    }

    private void updateTexts() {
        focusedTimeTxt.setText(String.valueOf((int) focusedTimeSlider.getValue()));
        shortBreakTxt.setText(String.valueOf((int) shortBreakSlider.getValue()));
        longBreakTxt.setText(String.valueOf((int) longBreakSlider.getValue()));
        sessionsTxt.setText(String.valueOf((int) sessionsSlider.getValue()));
        alarmTxt.setText(String.valueOf((int) alarmSlider.getValue()));
    }

    private void saveSettings() {
        sharedPreferences.edit()
                .putInt("focusedTime", (int) focusedTimeSlider.getValue())
                .putInt("shortBreak", (int) shortBreakSlider.getValue())
                .putInt("longBreak", (int) longBreakSlider.getValue())
                .putInt("sessions", (int) sessionsSlider.getValue())
                .putInt("alarmDuration", (int) alarmSlider.getValue())
                .putBoolean("autoStart", autoStartSessions.isChecked())
                .putBoolean("keepScreenAwake", keepScreenAwakeToggle.isChecked())
                .putBoolean("hapticFeedback", hapticFeedbackToggle.isChecked())
                .apply();
    }

    private void vibrate() {
        if (!sharedPreferences.getBoolean("hapticFeedback", true)) return;
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(vibrationEffect);
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private boolean isExternalStorageAvailable() {
        Log.d(TAG, "isExternalStorageAvailable: Checking external storage availability");
        String state = Environment.getExternalStorageState();
        boolean isAvailable = Environment.MEDIA_MOUNTED.equals(state);
        Log.d(TAG, "isExternalStorageAvailable: External storage available: " + isAvailable);
        return isAvailable;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications for music downloads");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showDownloadNotification(String title, String content) {
        Log.d(TAG, "showDownloadNotification: " + title + " - " + content);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void dismissDownloadNotification() {
        Log.d(TAG, "dismissDownloadNotification: Dismissing download notification");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private File getMusicFile(String musicName) {
        File musicDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "pomodoro_music");
        if (!musicDir.exists()) {
            boolean created = musicDir.mkdirs();
            Log.d(TAG, "getMusicFile: Music directory created: " + created);
        }
        File musicFile = new File(musicDir, musicName + ".mp3");
        Log.d(TAG, "getMusicFile: Music file path: " + musicFile.getAbsolutePath());
        return musicFile;
    }

    private void downloadMusic(String urlString, String musicName) {
        Log.d(TAG, "downloadMusic: Starting download for: " + musicName);
        Log.d(TAG, "downloadMusic: URL: " + urlString);

        new Thread(() -> {
            try {
                Log.d(TAG, "downloadMusic: Showing download notification");
                runOnUiThread(() -> showDownloadNotification("Downloading Music", musicName + " is downloading..."));

                Log.d(TAG, "downloadMusic: Creating URL object");
                URL url = new URL(urlString);

                File musicFile = getMusicFile(musicName);
                Log.d(TAG, "downloadMusic: Target file: " + musicFile.getAbsolutePath());
                Log.d(TAG, "downloadMusic: Parent directory exists: " + musicFile.getParentFile().exists());

                Log.d(TAG, "downloadMusic: Opening streams");
                try (java.io.InputStream in = url.openStream();
                     FileOutputStream out = new FileOutputStream(musicFile)) {

                    Log.d(TAG, "downloadMusic: Starting file transfer");
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;

                        // Log every 100KB downloaded
                        if (totalBytes % (100 * 1024) == 0) {
                            Log.d(TAG, "downloadMusic: Downloaded " + (totalBytes / 1024) + " KB");
                        }
                    }

                    Log.d(TAG, "downloadMusic: Download completed. Total bytes: " + totalBytes);
                }

                runOnUiThread(() -> {
                    Log.d(TAG, "downloadMusic: Showing success toast");
                    Toast.makeText(this, musicName + " downloaded successfully", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "downloadMusic: Download failed for " + musicName, e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                runOnUiThread(() -> {
                    Log.d(TAG, "downloadMusic: Dismissing download notification");
                    dismissDownloadNotification();
                });
            }
        }).start();
    }

    private void handleMusicToggle() {
        Log.d(TAG, "handleMusicToggle: Starting music toggle handling");

        // Kiểm tra external storage có available không
        if (!isExternalStorageAvailable()) {
            Log.e(TAG, "handleMusicToggle: External storage not available");
            Toast.makeText(this, "External storage not available", Toast.LENGTH_SHORT).show();
            return;
        }

        SwitchCompat[] toggles = {brownNoiseToggle, whiteNoiseToggle, rainfallToggle, lightJazzToggle};
        String[] musicNames = {"key_board", "water_stream", "rainfall", "light_jazz"};
        String[] musicUrls = {
                "https://drive.google.com/uc?export=download&id=1bEcdlI_s2tBEpDGyow4bFjS7P0aSgCVD",
                "https://drive.google.com/uc?export=download&id=1Bm2_zYaKet0OUyfK9LZo87AOM6Wr8cul",
                "https://drive.google.com/uc?export=download&id=1bEcdlI_s2tBEpDGyow4bFjS7P0aSgCVD",
                "https://drive.google.com/uc?export=download&id=1bEcdlI_s2tBEpDGyow4bFjS7P0aSgCVD"
        };

        Log.d(TAG, "handleMusicToggle: Checking toggle states");
        for (int i = 0; i < toggles.length; i++) {
            Log.d(TAG, "handleMusicToggle: Toggle " + i + " (" + musicNames[i] + ") is checked: " + toggles[i].isChecked());

            if (toggles[i].isChecked()) {
                Log.d(TAG, "handleMusicToggle: Setting selected music to: " + musicNames[i]);
                sharedPreferences.edit().putString("selected_music", musicNames[i]).apply();

                File musicFile = getMusicFile(musicNames[i]);
                Log.d(TAG, "handleMusicToggle: Music file path: " + musicFile.getAbsolutePath());
                Log.d(TAG, "handleMusicToggle: Music file exists: " + musicFile.exists());

                if (!musicFile.exists()) {
                    Log.d(TAG, "handleMusicToggle: Starting download for: " + musicNames[i]);
                    downloadMusic(musicUrls[i], musicNames[i]);
                } else {
                    Log.d(TAG, "handleMusicToggle: Music file already exists, no download needed");
                }
            } else {
                // Nếu tắt thì bỏ chọn nhạc
                String currentSelectedMusic = sharedPreferences.getString("selected_music", "");
                Log.d(TAG, "handleMusicToggle: Current selected music: " + currentSelectedMusic);

                if (currentSelectedMusic.equals(musicNames[i])) {
                    Log.d(TAG, "handleMusicToggle: Removing selected music: " + musicNames[i]);
                    sharedPreferences.edit().remove("selected_music").apply();
                }
            }
        }

        // Đảm bảo chỉ 1 toggle được bật
        Log.d(TAG, "handleMusicToggle: Ensuring only one toggle is active");
        for (int i = 0; i < toggles.length; i++) {
            if (toggles[i].isChecked()) {
                Log.d(TAG, "handleMusicToggle: Found active toggle at index: " + i);
                for (int j = 0; j < toggles.length; j++) {
                    if (i != j && toggles[j].isChecked()) {
                        Log.d(TAG, "handleMusicToggle: Disabling toggle at index: " + j);
                        toggles[j].setChecked(false);
                    }
                }
                break;
            }
        }

        Log.d(TAG, "handleMusicToggle: Music toggle handling completed");
    }

    private void initializeMusicToggles() {
        Log.d(TAG, "initializeMusicToggles: Initializing music toggles");

        String selectedMusic = sharedPreferences.getString("selected_music", "");
        Log.d(TAG, "initializeMusicToggles: Currently selected music: " + selectedMusic);

        brownNoiseToggle.setChecked("key_board".equals(selectedMusic));
        whiteNoiseToggle.setChecked("water_stream".equals(selectedMusic));
        rainfallToggle.setChecked("rainfall".equals(selectedMusic));
        lightJazzToggle.setChecked("light_jazz".equals(selectedMusic));

        Log.d(TAG, "initializeMusicToggles: Toggle states set");
    }

    private void setupMusicToggleListeners() {
        Log.d(TAG, "setupMusicToggleListeners: Setting up music toggle listeners");

        SwitchCompat[] toggles = {brownNoiseToggle, whiteNoiseToggle, rainfallToggle, lightJazzToggle};
        String[] toggleNames = {"brownNoise", "whiteNoise", "rainfall", "lightJazz"};

        for (int i = 0; i < toggles.length; i++) {
            final int index = i;
            toggles[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "setupMusicToggleListeners: Toggle " + toggleNames[index] + " changed to: " + isChecked);

                // Không cần kiểm tra permission, gọi trực tiếp handleMusicToggle
                Log.d(TAG, "setupMusicToggleListeners: Calling handleMusicToggle directly");
                handleMusicToggle();
            });
        }

        Log.d(TAG, "setupMusicToggleListeners: All listeners set up");
    }

    private void updateWakeLock(boolean keepAwake) {
        if (keepAwake) {
            if (wakeLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Pomodoro:WakeLock");
                wakeLock.acquire();
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}