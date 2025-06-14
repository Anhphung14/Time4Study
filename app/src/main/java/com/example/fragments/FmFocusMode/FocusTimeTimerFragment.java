package com.example.fragments.FmFocusMode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.time4study.R;
import com.example.adapters.FocusTimeTimerPresetAdapter;
import com.example.models.FocusTimeTimerPreset;
import com.example.services.FocusTimeTimerService;
import com.example.utils.FocusTimeStatsManager;
import com.google.firebase.auth.FirebaseAuth;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FocusTimeTimerFragment extends Fragment {
    private CardView focusCard, playBtn, pauseBtn;
    private TextView minTxt, secTxt, focusTxt, sessionsTxt;
    private ImageView resetBtn, skipBtn, brain;
    private LinearLayout focusCardBg;
    private MediaPlayer mediaPlayer = null;
    private SharedPreferences sharedPreferences;
    private FocusTimeStatsManager focusTimeStatsManager;
    private CountDownTimer countDownTimer = null;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;
    private int currentSession = 0;
    private int totalSessions = 4;
    private boolean autoStart = false;
    private boolean isFromShortBreak = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        focusTimeStatsManager = new FocusTimeStatsManager(requireContext());
        if (savedInstanceState != null) {
            timeLeftInMillis = savedInstanceState.getLong("timeLeftInMillis");
            timerRunning = savedInstanceState.getBoolean("timerRunning");
            currentSession = savedInstanceState.getInt("currentSession");
            totalSessions = savedInstanceState.getInt("totalSessions");
            autoStart = savedInstanceState.getBoolean("autoStart");
            isFromShortBreak = savedInstanceState.getBoolean("isFromShortBreak");
        } else {
            loadSettings();
        }
    }

    private void loadSettings() {
        timeLeftInMillis = sharedPreferences.getInt("focusedTime", 25) * 60 * 1000L;
        totalSessions = sharedPreferences.getInt("sessions", 4);
        autoStart = sharedPreferences.getBoolean("autoStart", false);
        if (sharedPreferences.getBoolean("wereTimerSettingsModified", false)) {
            sharedPreferences.edit().putString("focusText", "Focus").apply();
            sharedPreferences.edit().putBoolean("wereTimerSettingsModified", false).apply();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.focustime_fragment_timer, container, false);
        focusCard = view.findViewById(R.id.focus_card);
        minTxt = view.findViewById(R.id.min_txt);
        secTxt = view.findViewById(R.id.sec_txt);
        playBtn = view.findViewById(R.id.play_btn);
        pauseBtn = view.findViewById(R.id.pause_btn);
        resetBtn = view.findViewById(R.id.reset_btn);
        skipBtn = view.findViewById(R.id.skip_btn);
        focusCardBg = view.findViewById(R.id.focus_card_bg);
        focusTxt = view.findViewById(R.id.focus_txt);
        brain = view.findViewById(R.id.brain);
        sessionsTxt = requireActivity().findViewById(R.id.sessions_txt);
        focusTxt.setText(sharedPreferences.getString("focusText", "Focus"));
        updateCountdownText();
        updateSessionsText();
        focusCard.setOnClickListener(v -> {
            vibrate();
            showPresetDialog();
        });
        playBtn.setOnClickListener(v -> {
            vibrate();
            startTimer();
        });
        pauseBtn.setOnClickListener(v -> {
            vibrate();
            pauseTimer();
        });
        resetBtn.setOnClickListener(v -> {
            vibrate();
            resetTimer();
        });
        skipBtn.setOnClickListener(v -> {
            vibrate();
            loadShortBreakFragment();
        });
        if (timerRunning) {
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
            resetBtn.setVisibility(View.VISIBLE);
            skipBtn.setVisibility(View.VISIBLE);
            startTimer();
        } else if (autoStart && isFromShortBreak) {
            startTimer();
        } else {
            playBtn.setVisibility(View.VISIBLE);
            pauseBtn.setVisibility(View.GONE);
            resetBtn.setVisibility(View.GONE);
            skipBtn.setVisibility(View.GONE);
            updateCountdownText();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FocusTimeTimerService.appOpened(requireContext());
        boolean isServiceRunning = sharedPreferences.getBoolean("timerRunning", false);
        if (isServiceRunning) {
            timeLeftInMillis = sharedPreferences.getLong("timeLeftInMillis", 0);
            updateCountdownText();
            timerRunning = true;
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
            resetBtn.setVisibility(View.VISIBLE);
            skipBtn.setVisibility(View.VISIBLE);
            if (countDownTimer == null) {
                startTimer();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.edit().putBoolean("isAppInForeground", false).apply();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
                sharedPreferences.edit().putLong("timeLeftInMillis", timeLeftInMillis).apply();
            }
            @Override
            public void onFinish() {
                timerRunning = false;
                updateTimerState(false);
                playAlarm();
                int focusMinutes = sharedPreferences.getInt("focusedTime", 25);

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "guest";
                if (currentUid.equals(focusTimeStatsManager.getCurrentUid())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        focusTimeStatsManager.updateStats(focusMinutes);
                    }
                }

                if (currentSession <= totalSessions) {
                    currentSession++;
                    sharedPreferences.edit().putInt("currentSession", currentSession).apply();
                    loadShortBreakFragment();
                } else {
                    Toast.makeText(requireContext(), "All sessions completed", Toast.LENGTH_SHORT).show();
                    loadLongBreakFragment();
                }
            }
        }.start();
        timerRunning = true;
        updateTimerState(true);
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
        resetBtn.setVisibility(View.VISIBLE);
        skipBtn.setVisibility(View.VISIBLE);
        String sessionInfo = currentSession + "/" + totalSessions;
        FocusTimeTimerService.startTimer(requireContext(), "focus", sessionInfo);
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        updateTimerState(false);
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
        FocusTimeTimerService.stopTimer(requireContext());
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        loadSettings();
        timerRunning = false;
        updateTimerState(false);
        updateCountdownText();
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
        resetBtn.setVisibility(View.GONE);
        skipBtn.setVisibility(View.GONE);
        FocusTimeTimerService.stopTimer(requireContext());
    }

    private void updateCountdownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        minTxt.setText(String.format("%02d", minutes));
        secTxt.setText(String.format("%02d", seconds));
    }

    private void updateSessionsText() {
        sessionsTxt.setText(currentSession + "/" + totalSessions);
    }

    @SuppressLint("SupportAnnotationUsage")
    @RequiresPermission(Manifest.permission.VIBRATE)
    private void vibrate() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
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
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = null;
    }

    private void loadShortBreakFragment() {
        RelativeLayout parentLayout = requireActivity().findViewById(R.id.focusTimeMain);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        boolean amoledMode = sharedPreferences.getBoolean("amoledMode", false);
        if (amoledMode) {
            parentLayout.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else if (darkMode) {
            parentLayout.setBackgroundResource(R.color.deep_green);
        } else {
            parentLayout.setBackgroundResource(R.color.light_green);
        }
        FocusTimeShortBreakFragment fragment = new FocusTimeShortBreakFragment();
        fragment.setSessionInfo(currentSession, totalSessions, autoStart, true);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void loadLongBreakFragment() {
        RelativeLayout parentLayout = requireActivity().findViewById(R.id.focusTimeMain);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        boolean amoledMode = sharedPreferences.getBoolean("amoledMode", false);
        if (amoledMode) {
            parentLayout.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else if (darkMode) {
            parentLayout.setBackgroundResource(R.color.deep_blue);
        } else {
            parentLayout.setBackgroundResource(R.color.light_blue);
        }
        FocusTimeLongBreakFragment fragment = new FocusTimeLongBreakFragment();
        fragment.setSessionInfo(1, totalSessions);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void updateTimerState(boolean isRunning) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean("isTimerRunning", isRunning)
                .putBoolean("isBreakActive", false)
                .apply();
    }

    private void playAlarm() {
        try {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
            long alarmDuration = sharedPreferences.getInt("alarmDuration", 3) * 1000L;
            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "timer_notifications")
                    .setSmallIcon(R.drawable.brain)
                    .setContentTitle("Timer Complete")
                    .setContentText("Your timer is up. Good job!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            notificationManager.notify(1, builder.build());
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect vibrationEffect = VibrationEffect.createWaveform(new long[]{0, 500, 500}, 0);
                    vibrator.vibrate(vibrationEffect);
                } else {
                    vibrator.vibrate(new long[]{0, 500, 500}, 0);
                }
            }
            new Handler().postDelayed(() -> {
                if (vibrator != null) vibrator.cancel();
            }, alarmDuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeLeftInMillis", timeLeftInMillis);
        outState.putBoolean("timerRunning", timerRunning);
        outState.putInt("currentSession", currentSession);
        outState.putInt("totalSessions", totalSessions);
        outState.putBoolean("autoStart", autoStart);
        outState.putBoolean("isFromShortBreak", isFromShortBreak);
    }

    private void showPresetDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View presetView = inflater.inflate(R.layout.focustime_preset_layout, null);
        RecyclerView presetRecyclerView = presetView.findViewById(R.id.preset_rv);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(presetView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        presetRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<FocusTimeTimerPreset> presets = new ArrayList<>(loadPresets());
        FocusTimeTimerPresetAdapter adapter = new FocusTimeTimerPresetAdapter(presets, preset -> {
            vibrate();
            if (timerRunning) {
                pauseTimer();
            }
            sharedPreferences.edit()
                    .putInt("focusedTime", preset.focusMinutes)
                    .putInt("shortBreak", preset.shortBreakMinutes)
                    .putInt("longBreak", preset.longBreakMinutes)
                    .putString("focusText", preset.name)
                    .apply();
            timeLeftInMillis = preset.focusMinutes * 60 * 1000L;
            updateCountdownText();
            focusTxt.setText(preset.name);
            playBtn.setVisibility(View.VISIBLE);
            pauseBtn.setVisibility(View.GONE);
            resetBtn.setVisibility(View.GONE);
            skipBtn.setVisibility(View.GONE);
            dialog.dismiss();
        });
        adapter.setOnLongClickListener(preset -> {
            showDeleteConfirmationDialog(preset, () -> {
                List<FocusTimeTimerPreset> updatedPresets = loadPresets();
                adapter.updatePresets(updatedPresets);
            });
        });
        presetRecyclerView.setAdapter(adapter);
        presetView.findViewById(R.id.add_new_timer_btn).setOnClickListener(v -> {
            vibrate();
            dialog.dismiss();
            showAddTimerDialog(() -> {
                List<FocusTimeTimerPreset> updatedPresets = loadPresets();
                adapter.updatePresets(updatedPresets);
            });
        });
        dialog.show();
    }

    private void showAddTimerDialog(Runnable onPresetAdded) {
        View dialogView = getLayoutInflater().inflate(R.layout.focustime_dialog_add_timer, null);
        EditText nameInput = dialogView.findViewById(R.id.timer_name_input);
        EditText durationInput = dialogView.findViewById(R.id.timer_duration_input);
        EditText shortBreakInput = dialogView.findViewById(R.id.short_break_input);
        EditText longBreakInput = dialogView.findViewById(R.id.long_break_input);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add New Timer")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String name = nameInput.getText().toString();
                    Integer duration = parseInt(durationInput.getText().toString());
                    Integer shortBreak = parseInt(shortBreakInput.getText().toString());
                    Integer longBreak = parseInt(longBreakInput.getText().toString());
                    if (!name.isEmpty() && duration != null && shortBreak != null && longBreak != null) {
                        if (duration < 1 || shortBreak < 1 || longBreak < 1) {
                            Toast.makeText(requireContext(), "Duration, short break and long break must be at least 1 minute", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FocusTimeTimerPreset newPreset = new FocusTimeTimerPreset(name, duration, shortBreak, longBreak);
                        savePreset(newPreset);
                        onPresetAdded.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void showDeleteConfirmationDialog(FocusTimeTimerPreset preset, Runnable onPresetDeleted) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Timer")
                .setMessage("Are you sure you want to delete " + preset.name + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    deletePreset(preset);
                    onPresetDeleted.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<FocusTimeTimerPreset> loadPresets() {
        String presetsJson = sharedPreferences.getString("timer_presets", null);
        if (presetsJson == null) {
            List<FocusTimeTimerPreset> defaultPresets = new ArrayList<>();
            defaultPresets.add(new FocusTimeTimerPreset("Pomodoro", 25, 5, 15));
            defaultPresets.add(new FocusTimeTimerPreset("Quick Focus", 15, 3, 10));
            defaultPresets.add(new FocusTimeTimerPreset("Deep Work", 45, 10, 20));
            defaultPresets.add(new FocusTimeTimerPreset("Study Session", 30, 5, 15));
            sharedPreferences.edit().putString("timer_presets", new Gson().toJson(defaultPresets)).apply();
            return defaultPresets;
        }
        Type type = new TypeToken<List<FocusTimeTimerPreset>>() {}.getType();
        List<FocusTimeTimerPreset> result = new Gson().fromJson(presetsJson, type);
        return result != null ? result : new ArrayList<>();
    }

    private void savePreset(FocusTimeTimerPreset preset) {
        String presetsJson = sharedPreferences.getString("timer_presets", "[]");
        Type type = new TypeToken<List<FocusTimeTimerPreset>>() {}.getType();
        List<FocusTimeTimerPreset> presets = new Gson().fromJson(presetsJson, type);
        if (presets == null) presets = new ArrayList<>();
        presets.add(preset);
        sharedPreferences.edit().putString("timer_presets", new Gson().toJson(presets)).apply();
    }

    private void deletePreset(FocusTimeTimerPreset preset) {
        String presetsJson = sharedPreferences.getString("timer_presets", "[]");
        Type type = new TypeToken<List<FocusTimeTimerPreset>>() {}.getType();
        List<FocusTimeTimerPreset> presets = new Gson().fromJson(presetsJson, type);
        if (presets == null) presets = new ArrayList<>();
        String currentFocusText = sharedPreferences.getString("focusText", "Focus");
        if (currentFocusText.equals(preset.name)) {
            sharedPreferences.edit()
                    .putInt("focusedTime", 25)
                    .putInt("shortBreak", 5)
                    .putInt("longBreak", 15)
                    .putString("focusText", "Focus")
                    .apply();
            timeLeftInMillis = 25 * 60 * 1000L;
            updateCountdownText();
            focusTxt.setText("Focus");
        }
        List<FocusTimeTimerPreset> toRemove = new ArrayList<>();
        for (FocusTimeTimerPreset p : presets) {
            if (p.name.equals(preset.name)) toRemove.add(p);
        }
        presets.removeAll(toRemove);
        sharedPreferences.edit().putString("timer_presets", new Gson().toJson(presets)).apply();
    }

    public static FocusTimeTimerFragment newInstance(int currentSession, int totalSessions, boolean autoStart, boolean isFromShortBreak) {
        FocusTimeTimerFragment fragment = new FocusTimeTimerFragment();
        fragment.currentSession = currentSession;
        fragment.totalSessions = totalSessions;
        fragment.autoStart = autoStart;
        fragment.isFromShortBreak = isFromShortBreak;
        return fragment;
    }
} 