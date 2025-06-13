package com.example.fragments.FmFocusMode;

import android.Manifest;
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

import com.example.time4study.R;

public class FocusTimeShortBreakFragment extends Fragment {
    private TextView backToTimer, minTxt, secTxt, shortBreakTxt, sessionsTxt;
    private CardView playBtn, pauseBtn;
    private ImageView resetBtn, skipBtn, coffee;
    private LinearLayout shortBreakCardBg;
    private MediaPlayer mediaPlayer = null;
    private CountDownTimer countDownTimer = null;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;
    private int currentSession = 1;
    private int totalSessions = 4;
    private boolean autoStart = false;
    private boolean isFromTimer = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            timeLeftInMillis = savedInstanceState.getLong("timeLeftInMillis");
            timerRunning = savedInstanceState.getBoolean("timerRunning");
            currentSession = savedInstanceState.getInt("currentSession");
            totalSessions = savedInstanceState.getInt("totalSessions");
            autoStart = savedInstanceState.getBoolean("autoStart");
            isFromTimer = savedInstanceState.getBoolean("isFromTimer");
        } else {
            loadSettings();
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
        outState.putBoolean("isFromTimer", isFromTimer);
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        timeLeftInMillis = sharedPreferences.getInt("shortBreak", 5) * 60 * 1000L;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.focustime_fragment_short_break, container, false);
        backToTimer = view.findViewById(R.id.back_to_timer_txt);
        minTxt = view.findViewById(R.id.min_txt);
        secTxt = view.findViewById(R.id.sec_txt);
        playBtn = view.findViewById(R.id.play_btn);
        pauseBtn = view.findViewById(R.id.pause_btn);
        resetBtn = view.findViewById(R.id.reset_btn);
        skipBtn = view.findViewById(R.id.skip_btn);
        coffee = view.findViewById(R.id.coffee);
        shortBreakCardBg = view.findViewById(R.id.short_break_card_bg);
        shortBreakTxt = view.findViewById(R.id.short_break_txt);
        sessionsTxt = requireActivity().findViewById(R.id.sessions_txt);

        updateCountdownText();
        updateSessionsText();
        backToTimer.setOnClickListener(v -> {
            vibrate();
            loadTimerFragment();
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
            loadLongBreakFragment();
        });
        if (timerRunning) {
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
            resetBtn.setVisibility(View.VISIBLE);
            skipBtn.setVisibility(View.VISIBLE);
            startTimer();
        } else if (autoStart && isFromTimer) {
            startTimer();
        }
        return view;
    }

    private void loadTimerFragment() {
        FocusTimeTimerFragment fragment = FocusTimeTimerFragment.newInstance(currentSession, totalSessions, autoStart, false);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }
            @Override
            public void onFinish() {
                timerRunning = false;
                updateBreakState(false);
                playAlarm();
                if (currentSession < totalSessions) {
                    loadTimerFragment();
                } else {
                    Toast.makeText(requireContext(), "All sessions completed", Toast.LENGTH_SHORT).show();
                    loadLongBreakFragment();
                }
            }
        }.start();
        timerRunning = true;
        updateBreakState(true);
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
        resetBtn.setVisibility(View.VISIBLE);
        skipBtn.setVisibility(View.VISIBLE);
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        updateBreakState(false);
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        loadSettings();
        timerRunning = false;
        updateBreakState(false);
        updateCountdownText();
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
        resetBtn.setVisibility(View.GONE);
        skipBtn.setVisibility(View.GONE);
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

    private void loadLongBreakFragment() {
        FocusTimeLongBreakFragment fragment = new FocusTimeLongBreakFragment();
        fragment.setSessionInfo(1, totalSessions);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = null;
    }

    public void setSessionInfo(int currentSession, int totalSessions, boolean autoStart, boolean isFromTimer) {
        this.currentSession = currentSession;
        this.totalSessions = totalSessions;
        this.autoStart = autoStart;
        this.isFromTimer = isFromTimer;
    }

    private void updateBreakState(boolean isActive) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean("isTimerRunning", false)
                .putBoolean("isBreakActive", isActive)
                .apply();
    }

    private void playAlarm() {
        try {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
            long alarmDuration = sharedPreferences.getInt("alarmDuration", 3) * 1000L;
            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "timer_notifications")
                    .setSmallIcon(R.drawable.coffee)
                    .setContentTitle("Break Time Over")
                    .setContentText("Break time is over. Back to work!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            notificationManager.notify(2, builder.build());
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
} 