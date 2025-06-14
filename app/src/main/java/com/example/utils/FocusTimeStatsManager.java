package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FocusTimeStatsManager {
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private FirebaseAuth mAuth;
    private String currentUid;

    public static class Stats {
        public long totalFocusMinutes;
        public String lastFocusDate;
        public int currentStreak;
        public int longestStreak;
        public Stats() {
            this(0, "", 0, 0);
        }
        public Stats(long totalFocusMinutes, String lastFocusDate, int currentStreak, int longestStreak) {
            this.totalFocusMinutes = totalFocusMinutes;
            this.lastFocusDate = lastFocusDate;
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
        }
    }

    public static class MinuteStats {
        public String date;
        public int minuteOfDay;
        public int minutes;
        public MinuteStats(String date, int minuteOfDay, int minutes) {
            this.date = date;
            this.minuteOfDay = minuteOfDay;
            this.minutes = minutes;
        }
    }

    public static class DailyStats {
        public String date;
        public int minutes;
        public DailyStats(String date, int minutes) {
            this.date = date;
            this.minutes = minutes;
        }
    }

    public FocusTimeStatsManager(Context context) {
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "guest";
        sharedPreferences = context.getSharedPreferences("PomodoroStats_" + currentUid, Context.MODE_PRIVATE);
    }

    public String getCurrentUid() {
        return currentUid;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void updateStats(int minutes) {
        String today = LocalDate.now().toString();
        int minuteOfDay = LocalDateTime.now().getHour() * 60 + LocalDateTime.now().getMinute();
        Stats stats = loadStats();
        stats.totalFocusMinutes += minutes;
        if (!stats.lastFocusDate.equals(today)) {
            if (stats.lastFocusDate.isEmpty() || ChronoUnit.DAYS.between(LocalDate.parse(stats.lastFocusDate), LocalDate.now()) == 1) {
                stats.currentStreak++;
                if (stats.currentStreak > stats.longestStreak) {
                    stats.longestStreak = stats.currentStreak;
                }
            } else {
                stats.currentStreak = 1;
            }
            stats.lastFocusDate = today;
        }
        sharedPreferences.edit()
                .putLong("totalFocusMinutes", stats.totalFocusMinutes)
                .putString("lastFocusDate", stats.lastFocusDate)
                .putInt("currentStreak", stats.currentStreak)
                .putInt("longestStreak", stats.longestStreak)
                .apply();
        updateMinuteStats(today, minuteOfDay, minutes);
        updateDailyStats(today, minutes);
    }

    private void updateMinuteStats(String date, int minuteOfDay, int minutes) {
        List<MinuteStats> minuteStats = new ArrayList<>(loadMinuteStats());
        MinuteStats existingStat = null;
        for (MinuteStats stat : minuteStats) {
            if (stat.date.equals(date) && stat.minuteOfDay == minuteOfDay) {
                existingStat = stat;
                break;
            }
        }
        if (existingStat != null) {
            minuteStats.remove(existingStat);
            minuteStats.add(new MinuteStats(date, minuteOfDay, existingStat.minutes + minutes));
        } else {
            minuteStats.add(new MinuteStats(date, minuteOfDay, minutes));
        }
        // Sort by date, minuteOfDay
        Collections.sort(minuteStats, (a, b) -> {
            int cmp = a.date.compareTo(b.date);
            if (cmp != 0) return cmp;
            return Integer.compare(a.minuteOfDay, b.minuteOfDay);
        });
        // Keep only last 2 days
        String today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now().toString();
        }
        String yesterday = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            yesterday = LocalDate.now().minusDays(1).toString();
        }
        List<MinuteStats> filtered = new ArrayList<>();
        for (MinuteStats stat : minuteStats) {
            if (stat.date.equals(today) || stat.date.equals(yesterday)) {
                filtered.add(stat);
            }
        }
        sharedPreferences.edit().putString("minuteStats", gson.toJson(filtered)).apply();
    }

    private void updateDailyStats(String date, int minutes) {
        List<DailyStats> dailyStats = new ArrayList<>(loadDailyStats());
        DailyStats existingStat = null;
        for (DailyStats stat : dailyStats) {
            if (stat.date.equals(date)) {
                existingStat = stat;
                break;
            }
        }
        if (existingStat != null) {
            dailyStats.remove(existingStat);
            dailyStats.add(new DailyStats(date, existingStat.minutes + minutes));
        } else {
            dailyStats.add(new DailyStats(date, minutes));
        }
        // Sort by date
        Collections.sort(dailyStats, (a, b) -> a.date.compareTo(b.date));
        // Keep only last 30 days
        while (dailyStats.size() > 30) {
            dailyStats.remove(0);
        }
        sharedPreferences.edit().putString("dailyStats", gson.toJson(dailyStats)).apply();
    }

    public Stats loadStats() {
        return new Stats(
                sharedPreferences.getLong("totalFocusMinutes", 0),
                sharedPreferences.getString("lastFocusDate", ""),
                sharedPreferences.getInt("currentStreak", 0),
                sharedPreferences.getInt("longestStreak", 0)
        );
    }

    public int[] getFormattedTotalFocusTime() {
        long totalMinutes = loadStats().totalFocusMinutes;
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);
        return new int[]{hours, minutes};
    }

    public List<MinuteStats> loadMinuteStats() {
        String json = sharedPreferences.getString("minuteStats", "[]");
        Type type = new TypeToken<List<MinuteStats>>() {}.getType();
        List<MinuteStats> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    public List<DailyStats> loadDailyStats() {
        String json = sharedPreferences.getString("dailyStats", "[]");
        Type type = new TypeToken<List<DailyStats>>() {}.getType();
        List<DailyStats> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    // Debug methods to verify data
    public void printStoredData() {
        Stats stats = loadStats();
        System.out.println("Total Focus Minutes: " + stats.totalFocusMinutes);
        System.out.println("Last Focus Date: " + stats.lastFocusDate);
        System.out.println("Current Streak: " + stats.currentStreak);
        System.out.println("Longest Streak: " + stats.longestStreak);
        System.out.println("Minute Stats: " + loadMinuteStats());
        System.out.println("Daily Stats: " + loadDailyStats());
    }

    public void clearAllStats() {
        sharedPreferences.edit().clear().apply();
    }
} 