package com.example.time4study.AcFocusMode;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.time4study.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.example.utils.FocusTimeStatsManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FocusTimeStatsActivity extends AppCompatActivity {
    private CardView todayCard, weekCard;
    private TextView todayTxt, weekTxt, hrsTxt, minsTxt, longestStreakTxt, currentStreakTxt;
    private RelativeLayout todayBg, weekBg;
    private BarChart todayChart, weekChart;
    private FocusTimeStatsManager focusTimeStatsManager;
    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.focustime_activity_stats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.focusTimeStats), (v, insets) -> {
            android.graphics.Insets systemBars = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return insets;
        });
        focusTimeStatsManager = new FocusTimeStatsManager(this);
        todayCard = findViewById(R.id.today_card);
        weekCard = findViewById(R.id.month_card);
        todayTxt = findViewById(R.id.today_txt);
        weekTxt = findViewById(R.id.month_txt);
        todayBg = findViewById(R.id.today_bg);
        weekBg = findViewById(R.id.month_bg);
        todayChart = findViewById(R.id.today_chart);
        weekChart = findViewById(R.id.month_chart);
        hrsTxt = findViewById(R.id.hrs_txt);
        minsTxt = findViewById(R.id.mins_txt);
        backBtn = findViewById(R.id.back_btn);
        longestStreakTxt = findViewById(R.id.longest_streak_txt);
        currentStreakTxt = findViewById(R.id.current_streak_txt);
        updateStatsDisplay();
        setupCharts();
        todayCard.setOnClickListener(v -> {
            vibrate();
            todayTxt.setTextColor(getResources().getColor(R.color.alt_light_red, null));
            todayBg.setBackgroundColor(getResources().getColor(R.color.deep_red, null));
            weekTxt.setTextColor(getResources().getColor(R.color.deep_red, null));
            weekBg.setBackgroundColor(getResources().getColor(R.color.alt_light_red, null));
            todayChart.setVisibility(View.VISIBLE);
            weekChart.setVisibility(View.GONE);
        });
        weekCard.setOnClickListener(v -> {
            vibrate();
            todayTxt.setTextColor(getResources().getColor(R.color.deep_red, null));
            todayBg.setBackgroundColor(getResources().getColor(R.color.alt_light_red, null));
            weekTxt.setTextColor(getResources().getColor(R.color.alt_light_red, null));
            weekBg.setBackgroundColor(getResources().getColor(R.color.deep_red, null));
            todayChart.setVisibility(View.GONE);
            weekChart.setVisibility(View.VISIBLE);
        });
        backBtn.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatsDisplay();
        refreshCharts();
    }

    private void setupCharts() {
        setupTodayChart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupWeekChart();
        }
    }

    private void setupTodayChart() {
        List<FocusTimeStatsManager.MinuteStats> minuteStats = focusTimeStatsManager.loadMinuteStats();
        String today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now().toString();
        }
        List<FocusTimeStatsManager.MinuteStats> todayStats = new ArrayList<>();
        for (FocusTimeStatsManager.MinuteStats stat : minuteStats) {
            if (today.equals(stat.date)) {
                todayStats.add(stat);
            }
        }
        // Aggregate by hour
        java.util.Map<Integer, Integer> hourMap = new java.util.HashMap<>();
        for (FocusTimeStatsManager.MinuteStats stat : todayStats) {
            int hour = stat.minuteOfDay / 60;
            hourMap.put(hour, hourMap.getOrDefault(hour, 0) + stat.minutes);
        }
        List<Integer> nonZeroHours = new ArrayList<>(hourMap.keySet());
        Collections.sort(nonZeroHours);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < nonZeroHours.size(); i++) {
            int hour = nonZeroHours.get(i);
            entries.add(new BarEntry(i, hourMap.get(hour)));
            String ampm = hour < 12 ? "am" : "pm";
            int hour12 = (hour == 0) ? 12 : (hour > 12 ? hour - 12 : hour);
            labels.add(String.format("%d:00 %s", hour12, ampm));
        }
        BarDataSet dataSet = new BarDataSet(entries, "Focus Time (minutes)");
        dataSet.setColor(getResources().getColor(R.color.deep_red, null));
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.3f);
        todayChart.setData(data);
        todayChart.getDescription().setEnabled(false);
        todayChart.getLegend().setEnabled(false);
        todayChart.setDrawGridBackground(false);
        todayChart.setDrawBarShadow(false);
        todayChart.setDrawValueAboveBar(true);
        todayChart.setScaleEnabled(false);
        todayChart.setPinchZoom(false);
        todayChart.setDrawBorders(false);
        todayChart.setBackgroundColor(Color.TRANSPARENT);
        todayChart.getAxisLeft().setDrawGridLines(false);
        todayChart.getAxisLeft().setAxisMinimum(0f);
        todayChart.getAxisLeft().setTextColor(getResources().getColor(R.color.deep_red, null));
        todayChart.getAxisRight().setEnabled(false);
        XAxis xAxis = todayChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.deep_red, null));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        xAxis.setLabelRotationAngle(0f);
        todayChart.setVisibleXRangeMaximum(5f);
        todayChart.setDragEnabled(true);
        todayChart.setScaleEnabled(false);
        todayChart.animateY(1000);
        todayChart.invalidate();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void setupWeekChart() {
        List<FocusTimeStatsManager.DailyStats> dailyStats = focusTimeStatsManager.loadDailyStats();
        LocalDate today = LocalDate.now();
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            days.add(today.minusDays(6 - i));
        }
        java.util.Map<LocalDate, Integer> dayToMinutes = new java.util.HashMap<>();
        for (FocusTimeStatsManager.DailyStats stat : dailyStats) {
            dayToMinutes.put(LocalDate.parse(stat.date), stat.minutes);
        }
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            LocalDate date = days.get(i);
            Integer minutes = dayToMinutes.get(date);
            entries.add(new BarEntry(i, minutes != null ? minutes : 0));
            String dayName = date.getDayOfWeek().name();
            labels.add(dayName.substring(0, 1) + dayName.substring(1, 3).toLowerCase());
        }
        BarDataSet dataSet = new BarDataSet(entries, "Focus Time (minutes)");
        dataSet.setColor(getResources().getColor(R.color.deep_red, null));
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.4f);
        weekChart.setData(data);
        weekChart.getDescription().setEnabled(false);
        weekChart.getLegend().setEnabled(false);
        weekChart.setDrawGridBackground(false);
        weekChart.setDrawBarShadow(false);
        weekChart.setDrawValueAboveBar(true);
        weekChart.setScaleEnabled(false);
        weekChart.setPinchZoom(false);
        weekChart.setDrawBorders(false);
        weekChart.setBackgroundColor(Color.TRANSPARENT);
        weekChart.getAxisLeft().setDrawGridLines(false);
        weekChart.getAxisLeft().setAxisMinimum(0f);
        weekChart.getAxisLeft().setTextColor(getResources().getColor(R.color.deep_red, null));
        weekChart.getAxisRight().setEnabled(false);
        XAxis xAxis = weekChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.deep_red, null));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        xAxis.setLabelRotationAngle(0f);
        weekChart.setVisibleXRangeMaximum(5f);
        weekChart.setDragEnabled(true);
        weekChart.setScaleEnabled(false);
        weekChart.animateY(1000);
        weekChart.invalidate();
        int todayIndex = days.indexOf(today);
        if (todayIndex != -1) {
            weekChart.moveViewToX(todayIndex);
        }
    }

    private void updateStatsDisplay() {
        int[] time = focusTimeStatsManager.getFormattedTotalFocusTime();
        hrsTxt.setText(String.valueOf(time[0]));
        minsTxt.setText(String.valueOf(time[1]));
        FocusTimeStatsManager.Stats stats = focusTimeStatsManager.loadStats();
        longestStreakTxt.setText(String.valueOf(stats.longestStreak));
        currentStreakTxt.setText(String.valueOf(stats.currentStreak));
    }

    @SuppressLint("NewApi")
    private void refreshCharts() {
        setupTodayChart();
        setupWeekChart();
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    }
}