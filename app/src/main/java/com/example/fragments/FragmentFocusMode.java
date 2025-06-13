package com.example.fragments;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.airbnb.lottie.LottieAnimationView;
import com.example.fragments.FmFocusMode.FocusTimeLongBreakFragment;
import com.example.fragments.FmFocusMode.FocusTimeShortBreakFragment;
import com.example.fragments.FmFocusMode.FocusTimeTimerFragment;
import com.example.time4study.AcFocusMode.FocusTimeSettingsActivity;
import com.example.time4study.R;
import com.example.services.FocusTimeTimerService;
import java.io.File;

public class FragmentFocusMode extends Fragment {
    private ImageView settings_btn;
    private FrameLayout frame_layout;
    private TextView sessionsTxt;
    private ImageView musicBtn;
    private LottieAnimationView musicAnim;
    private MediaPlayer mediaPlayer = null;
    private boolean isMusicPlaying = false;

    private ActivityResultLauncher<Intent> settingsLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_focus_mode, container, false);

        // Initialize views
        sessionsTxt = view.findViewById(R.id.sessions_txt);
        settings_btn = view.findViewById(R.id.settings_btn);
        musicBtn = view.findViewById(R.id.music_btn);
        musicAnim = view.findViewById(R.id.music_animated_btn);
        frame_layout = view.findViewById(R.id.frame_layout);

        // Initialize MediaPlayer with the audio file
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.music);

        // ActivityResultLauncher for settings
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == FocusTimeSettingsActivity.RESULT_TIMER_SETTINGS_CHANGED) {
                        android.content.SharedPreferences sp = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
                        sp.edit().putInt("currentSession", 1).putBoolean("wereTimerSettingsModified", false).apply();
                        showTimerFragment();
                    }
                }
        );

        // ActivityResultLauncher for notification permission
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        createNotificationChannel();
                    } else {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Notification Permission Required")
                                .setMessage("This app needs notification permission to alert you when timers complete. Please enable it in Settings.")
                                .setPositiveButton("Open Settings", (dialog, which) -> {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                                    startActivity(intent);
                                })
                                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
        );

        // Check and request notification permission
        checkNotificationPermission();

        // Restore fragment from notification if needed
        Bundle args = getArguments();
        if (args != null) {
            String fragmentType = args.getString("fragmentType");
            String sessionInfo = args.getString("sessionInfo");
            if (fragmentType != null && sessionInfo != null) {
                switch (fragmentType) {
                    case "focus": {
                        String[] sessionParts = sessionInfo.split("/");
                        int currentSession = 1;
                        int totalSessions = 4;
                        if (sessionParts.length == 2) {
                            try {
                                currentSession = Integer.parseInt(sessionParts[0]);
                            } catch (Exception ignored) {}
                            try {
                                totalSessions = Integer.parseInt(sessionParts[1]);
                            } catch (Exception ignored) {}
                        }

                        FocusTimeTimerFragment fragment = FocusTimeTimerFragment.newInstance(currentSession, totalSessions, false, false);

                        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
                        break;
                    }
                    case "shortBreak": {
                        FocusTimeShortBreakFragment fragment = new FocusTimeShortBreakFragment();
                        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
                        break;
                    }
                    case "longBreak": {
                        FocusTimeLongBreakFragment fragment = new FocusTimeLongBreakFragment();
                        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
                        break;
                    }
                }
                FocusTimeTimerService.appOpened(requireContext());
            } else {
                getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, new FocusTimeTimerFragment()).commit();
            }
        } else {
            getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, new FocusTimeTimerFragment()).commit();
        }

        settings_btn.setOnClickListener(v -> {
            vibrate();
            Intent intent = new Intent(requireContext(), FocusTimeSettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        musicBtn.setOnClickListener(v -> {
            vibrate();
            if (!isMusicPlaying) {
                playSelectedMusic();
            } else {
                stopMusic();
            }
        });

        musicAnim.setOnClickListener(v -> {
            vibrate();
            stopMusic();
        });

        return view;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                createNotificationChannel();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationPermissionRationale();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            createNotificationChannel();
        }
    }

    private void showNotificationPermissionRationale() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Notification Permission Required")
                .setMessage("This app needs notification permission to alert you when timers complete. Please grant the permission to continue.")
                .setPositiveButton("Grant Permission", (dialog, which) -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "timer_notifications",
                    "Timer Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for timer completion");
            channel.enableVibration(true);
            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setMainBackgroundForFragment(String fragmentType) {
        View mainLayout = requireView().findViewById(R.id.focusTimeMain);
        switch (fragmentType) {
            case "timer":
                mainLayout.setBackgroundResource(R.color.light_red);
                break;
            case "short_break":
                mainLayout.setBackgroundResource(R.color.light_green);
                break;
            case "long_break":
                mainLayout.setBackgroundResource(R.color.light_blue);
                break;
        }
    }

    private void showTimerFragment() {
        setMainBackgroundForFragment("timer");
        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, new FocusTimeTimerFragment()).commit();
    }

    private void showShortBreakFragment() {
        setMainBackgroundForFragment("short_break");
        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, new FocusTimeShortBreakFragment()).commit();
    }

    private void showLongBreakFragment() {
        setMainBackgroundForFragment("long_break");
        getParentFragmentManager().beginTransaction().replace(R.id.frame_layout, new FocusTimeLongBreakFragment()).commit();
    }

    private File getMusicFile(String musicName) {
        File musicDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "pomodoro_music");
        Log.d("DEBUG", "musicDir: " + musicDir);
        return new File(musicDir, musicName + ".mp3");
    }

    private void playSelectedMusic() {
        android.content.SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        String selectedMusic = sharedPreferences.getString("selected_music", null);
        if (selectedMusic != null) {
            File musicFile = getMusicFile(selectedMusic);
            if (musicFile.exists()) {
                try {
                    if (mediaPlayer != null) mediaPlayer.release();
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(musicFile.getAbsolutePath());
                    mediaPlayer.setLooping(true);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    isMusicPlaying = true;
                    musicBtn.setVisibility(View.GONE);
                    musicAnim.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Audio file not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Select audio from settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        mediaPlayer = null;
        isMusicPlaying = false;
        musicAnim.setVisibility(View.GONE);
        musicBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void vibrate() {
        android.content.SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("hapticFeedback", true)) return;
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(vibrationEffect);
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.frame_layout);
        if (currentFragment != null) {
            outState.putString("currentFragment", currentFragment.getClass().getSimpleName());
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String fragmentType = savedInstanceState.getString("currentFragment");
            if (fragmentType != null) {
                switch (fragmentType) {
                    case "TimerFragment":
                        setMainBackgroundForFragment("timer");
                        break;
                    case "ShortBreakFragment":
                        setMainBackgroundForFragment("short_break");
                        break;
                    case "LongBreakFragment":
                        setMainBackgroundForFragment("long_break");
                        break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMusic();
    }
}