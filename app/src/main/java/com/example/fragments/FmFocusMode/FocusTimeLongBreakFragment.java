package com.example.fragments.FmFocusMode;

import android.Manifest;
import android.annotation.SuppressLint;
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

import com.example.services.FocusTimeTimerService;
import com.example.time4study.R;


public class FocusTimeLongBreakFragment extends Fragment {
    private TextView backToTimer, minTxt, secTxt, longBreakTxt, sessionsTxt;
    private CardView playBtn, pauseBtn;
    private ImageView resetBtn, skipBtn, coffee;
    private LinearLayout longBreakCardBg;
    private MediaPlayer mediaPlayer = null;
    private CountDownTimer countDownTimer = null;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;
    private int currentSession = 1;
    private int totalSessions = 4;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            timeLeftInMillis = savedInstanceState.getLong("timeLeftInMillis");
            timerRunning = savedInstanceState.getBoolean("timerRunning");
            currentSession = savedInstanceState.getInt("currentSession");
            totalSessions = savedInstanceState.getInt("totalSessions");
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
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
        timeLeftInMillis = sharedPreferences.getInt("longBreak", 10) * 60 * 1000L;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.focustime_fragment_long_break, container, false);
        backToTimer = view.findViewById(R.id.back_to_timer_txt);
        minTxt = view.findViewById(R.id.min_txt);
        secTxt = view.findViewById(R.id.sec_txt);
        playBtn = view.findViewById(R.id.play_btn);
        pauseBtn = view.findViewById(R.id.pause_btn);
        resetBtn = view.findViewById(R.id.reset_btn);
        skipBtn = view.findViewById(R.id.skip_btn);
        coffee = view.findViewById(R.id.coffee);
        longBreakCardBg = view.findViewById(R.id.long_break_card_bg);
        longBreakTxt = view.findViewById(R.id.long_break_txt);
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
            loadTimerFragment();
        });
        if (timerRunning) {
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
            resetBtn.setVisibility(View.VISIBLE);
            skipBtn.setVisibility(View.VISIBLE);
            startTimer();
        }
        return view;
    }

    private void loadTimerFragment() {
        FocusTimeTimerFragment fragment = FocusTimeTimerFragment.newInstance(1, totalSessions, false, false);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
                sharedPreferences.edit().putLong("timeLeftInMillis", timeLeftInMillis).apply();
            }
            @Override
            public void onFinish() {
                timerRunning = false;
                updateTimerState(false);
                playAlarm();
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE);
                sharedPreferences.edit()
                    .putInt("currentSession", 0)
                    .putBoolean("isFromShortBreak", false)
                    .apply();
                loadFocusTimeTimerFragment();
            }
        }.start();
        timerRunning = true;
        updateTimerState(true);
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
        resetBtn.setVisibility(View.VISIBLE);
        skipBtn.setVisibility(View.VISIBLE);
        FocusTimeTimerService.startTimer(requireContext(), "longBreak", null);
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        updateTimerState(false);
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
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

    private void updateTimerState(boolean isActive) {
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
                    .setContentTitle("Long Break Complete")
                    .setContentText("Rest or reset?")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            notificationManager.notify(3, builder.build());
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

    private void loadFocusTimeTimerFragment() {
        FocusTimeTimerFragment fragment = FocusTimeTimerFragment.newInstance(0, totalSessions, false, false);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    public void setSessionInfo(int currentSession, int totalSessions) {
        this.currentSession = currentSession;
        this.totalSessions = totalSessions;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = null;
    }
} 