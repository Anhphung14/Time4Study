package com.example.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.time4study.R;
import com.example.utils.FocusTimeStatsManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentStatistics#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentStatistics extends Fragment {

    private LinearLayout goalContainer;
    private BarChart barChart;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FragmentStatistics() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentStatistics.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentStatistics newInstance(String param1, String param2) {
        FragmentStatistics fragment = new FragmentStatistics();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        goalContainer = view.findViewById(R.id.goalContainer);
        barChart = view.findViewById(R.id.barChart);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadGoals(); // Load dữ liệu khi fragment được tạo
        setupBarChart();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupBarChart(); // Load lại chart
    }

    private void loadGoals() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Xử lý trường hợp người dùng chưa đăng nhập
            return;
        }
        String uid = currentUser.getUid();

        LocalDate currentDate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();
        } else {
            currentDate = null;
        }
        DateTimeFormatter formatter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a z");
        } else {
            formatter = null;
        }

        db.collection("studyGoals")
                .whereEqualTo("uid", uid) // Lọc theo uid của người dùng
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    goalContainer.removeAllViews(); // Xóa các view cũ trước khi thêm mới
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        String progressStr = document.getString("progress");
                        Timestamp startTimestamp = document.getTimestamp("startDate");
                        Timestamp endTimestamp = document.getTimestamp("endDate");

                        // Chuyển đổi chuỗi ngày thành LocalDate
                        LocalDate startDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && startTimestamp != null) {
                            startDate = startTimestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        }
                        LocalDate endDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && endTimestamp != null) {
                            endDate = endTimestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        }

                        // Gọi phương thức mới để tính và hiển thị tiến độ dựa trên goalTasks
                        calculateAndDisplayGoalProgress(document.getId(), title, startDate, endDate, currentDate);
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi nếu có
                    Log.e("FragmentStatistics", "Lỗi khi tải studyGoals: ", e);
                });
    }

    private void calculateAndDisplayGoalProgress(String goalId, String goalTitle, LocalDate startDate, LocalDate endDate, LocalDate currentDate) {
        db.collection("studyGoals")
                .document(goalId)
                .collection("goalTasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalTasks = queryDocumentSnapshots.size();
                    int uncompletedTasks = 0;

                    for (QueryDocumentSnapshot taskDocument : queryDocumentSnapshots) {
                        // Kiểm tra cả 'isCompleted' và 'completed'
                        Boolean isCompleted = taskDocument.getBoolean("isCompleted");
                        Boolean completed = taskDocument.getBoolean("completed");

                        if ((isCompleted != null && !isCompleted) || (completed != null && !completed)) {
                            uncompletedTasks++;
                        }
                    }

                    // Chỉ hiển thị goal nếu nó có task con và có ít nhất một task chưa hoàn thành
                    if (totalTasks > 0 && uncompletedTasks > 0) {
                        // Tính toán phần trăm chưa hoàn thành
                        String progressDisplay = String.format("Còn %d nhiệm vụ chưa hoàn thành", uncompletedTasks);

                        // Thêm điều kiện lọc ngày hiện tại trong khoảng thời gian của mục tiêu
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (startDate != null && endDate != null && !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                                addGoalCard(goalTitle, progressDisplay);
                            }
                        } else {
                            // Xử lý cho các phiên bản Android cũ hơn nếu cần, hoặc bỏ qua việc lọc ngày
                            addGoalCard(goalTitle, progressDisplay);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FragmentStatistics", "Lỗi khi tải goalTasks: ", e);
                });
    }

    private void addGoalCard(String title, String progress) {
        // Tạo CardView
        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8); // Khoảng cách giữa các card
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(4f);
        cardView.setRadius(8f);
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card));

        // Tạo LinearLayout bên trong CardView
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(12, 12, 12, 12);

        // Thêm TextView cho tên goal
        TextView tvGoalName = new TextView(requireContext());
        tvGoalName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvGoalName.setText(title);
        tvGoalName.setTextSize(16f);
        tvGoalName.setTypeface(null,Typeface.BOLD);
        tvGoalName.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));

        // Thêm TextView cho progress
        TextView tvProgress = new TextView(requireContext());
        tvProgress.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvProgress.setText(progress);
        tvProgress.setTextSize(14f);
        tvProgress.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));

        // Thêm các TextView vào layout
        layout.addView(tvGoalName);
        layout.addView(tvProgress);

        // Thêm layout vào CardView
        cardView.addView(layout);

        // Thêm CardView vào container
        goalContainer.addView(cardView);
    }
    private void setupBarChart() {
        FocusTimeStatsManager focusTimeStatsManager = new FocusTimeStatsManager(requireContext());
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
        Map<Integer, Integer> hourMap = new HashMap<>();
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
        dataSet.setColor(ContextCompat.getColor(requireContext(), isDarkMode() ? R.color.colorPrimary : R.color.deep_red));
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.3f);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawBorders(false);
        barChart.setBackgroundColor(Color.TRANSPARENT);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextColor(getResources().getColor(android.R.color.holo_purple, null));
        barChart.getAxisRight().setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(android.R.color.holo_purple, null));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        xAxis.setLabelRotationAngle(0f);
        barChart.setVisibleXRangeMaximum(5f);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private boolean isDarkMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}