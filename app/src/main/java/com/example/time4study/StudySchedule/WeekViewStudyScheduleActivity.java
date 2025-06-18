package com.example.time4study.StudySchedule;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.models.Event;
import com.example.time4study.R;
import com.google.android.material.navigation.NavigationView;
import com.google.api.Distribution;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class WeekViewStudyScheduleActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private Calendar currentWeekStart;

    // Layout
    private TextView tv_day1, tv_day2, tv_day3, tv_day4, tv_day5, tv_day6, tv_day7;
    private TextView textDay3_weekview, textToday_weekview;


    RelativeLayout relativelayout_timeline_table_day1, relativelayout_timeline_table_day2, relativelayout_timeline_table_day3,
            relativelayout_timeline_table_day4, relativelayout_timeline_table_day5, relativelayout_timeline_table_day6,
            relativelayout_timeline_table_day7;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton imgButtonBack;


    // khac
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private String uid;
    private List<RelativeLayout> listRelativeLayout = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_week_view_study_schedule);

        init();

        currentWeekStart = getMondayOfCurrentWeek();
        updateCurrentDate();
        setupWeek();
        updateEvent();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (diffX > 100 && Math.abs(velocityX) > 100) {
                    currentWeekStart.add(Calendar.DAY_OF_YEAR, -7);
                    updateEvent();
                } else if (diffX < -100 && Math.abs(velocityX) > 100) {
                    currentWeekStart.add(Calendar.DAY_OF_YEAR, 7);
                    updateEvent();
                }

                setupWeek();
                return true;
            }
        });

        Calendar calendar = Calendar.getInstance();

        textDay3_weekview.setOnClickListener(v -> {
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
//                        currentWeekStart.set(year, month, dayOfMonth);
//                        updateCurrentDate();

                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                        selectedDate.set(Calendar.MINUTE, 0);
                        selectedDate.set(Calendar.SECOND, 0);
                        selectedDate.set(Calendar.MILLISECOND, 0);

                        // Đặt currentWeekStart về Thứ Hai của tuần chứa ngày được chọn
                        currentWeekStart.setTime(selectedDate.getTime());
                        currentWeekStart.setFirstDayOfWeek(Calendar.MONDAY); // Đảm bảo Thứ Hai là ngày đầu tuần
                        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

                        updateCurrentDate();
                        setupWeek();
                        updateEvent();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        textToday_weekview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar selectedDate = Calendar.getInstance();
//                selectedDate.set(year, month, dayOfMonth);
                selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                selectedDate.set(Calendar.MINUTE, 0);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);

                // Đặt currentWeekStart về Thứ Hai của tuần chứa ngày được chọn
                currentWeekStart.setTime(selectedDate.getTime());
                currentWeekStart.setFirstDayOfWeek(Calendar.MONDAY); // Đảm bảo Thứ Hai là ngày đầu tuần
                currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

                setupWeek();
                updateCurrentDate();
                updateEvent();
            }
        });

        imgButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("back_to_dayview", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        drawerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });
    }


    public void init() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();

        imgButtonBack = findViewById(R.id.btn_back_weekview);
        drawerLayout = findViewById(R.id.calendar_view_week_drawer);
        textDay3_weekview = findViewById(R.id.textDay3_weekview);
        textToday_weekview = findViewById(R.id.textToday_weekview);
        tv_day1 = findViewById(R.id.day1ofweek);
        tv_day2 = findViewById(R.id.day2ofweek);
        tv_day3 = findViewById(R.id.day3ofweek);
        tv_day4 = findViewById(R.id.day4ofweek);
        tv_day5 = findViewById(R.id.day5ofweek);
        tv_day6 = findViewById(R.id.day6ofweek);
        tv_day7 = findViewById(R.id.day7ofweek);

        relativelayout_timeline_table_day1 = findViewById(R.id.relativelayout_timeline_table_day1);
        relativelayout_timeline_table_day2 = findViewById(R.id.relativelayout_timeline_table_day2);
        relativelayout_timeline_table_day3 = findViewById(R.id.relativelayout_timeline_table_day3);
        relativelayout_timeline_table_day4 = findViewById(R.id.relativelayout_timeline_table_day4);
        relativelayout_timeline_table_day5 = findViewById(R.id.relativelayout_timeline_table_day5);
        relativelayout_timeline_table_day6 = findViewById(R.id.relativelayout_timeline_table_day6);
        relativelayout_timeline_table_day7 = findViewById(R.id.relativelayout_timeline_table_day7);

        listRelativeLayout.add(relativelayout_timeline_table_day1);
        listRelativeLayout.add(relativelayout_timeline_table_day2);
        listRelativeLayout.add(relativelayout_timeline_table_day3);
        listRelativeLayout.add(relativelayout_timeline_table_day4);
        listRelativeLayout.add(relativelayout_timeline_table_day5);
        listRelativeLayout.add(relativelayout_timeline_table_day6);
        listRelativeLayout.add(relativelayout_timeline_table_day7);

    }

    public void updateCurrentDate() {

        SimpleDateFormat day1Format = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat day2Format = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat day3Format = new SimpleDateFormat("MMMM", Locale.getDefault());

        String day1 = day1Format.format(currentWeekStart.getTime());
        String day2 = day2Format.format(currentWeekStart.getTime());
        String day3 = day3Format.format(currentWeekStart.getTime());

//        textDay1.setText(day1);
//        textDay2.setText(day2);
        textDay3_weekview.setText(day3);

//        Calendar currentDate = Calendar.getInstance();

//        boolean isToday = calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
//                calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) &&
//                calendar.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH);
//
//        if (isToday) {
//            textDay2.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
//        } else {
//            textDay2.setBackgroundResource(0);
//        }
    }

    private Calendar getMondayOfCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private void setupWeek() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        Calendar day = (Calendar) currentWeekStart.clone();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Xóa background cũ trước khi cập nhật
        tv_day1.setBackgroundResource(0);
        tv_day2.setBackgroundResource(0);
        tv_day3.setBackgroundResource(0);
        tv_day4.setBackgroundResource(0);
        tv_day5.setBackgroundResource(0);
        tv_day6.setBackgroundResource(0);
        tv_day7.setBackgroundResource(0);

        // Cập nhật ngày và kiểm tra ngày hôm nay
        tv_day1.setText(sdf.format(day.getTime())); // Thứ Hai
        if (isSameDay(day, today)) tv_day1.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day2.setText(sdf.format(day.getTime())); // Thứ Ba
        if (isSameDay(day, today)) tv_day2.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day3.setText(sdf.format(day.getTime())); // Thứ Tư
        if (isSameDay(day, today)) tv_day3.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day4.setText(sdf.format(day.getTime())); // Thứ Năm
        if (isSameDay(day, today)) tv_day4.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day5.setText(sdf.format(day.getTime())); // Thứ Sáu
        if (isSameDay(day, today)) tv_day5.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day6.setText(sdf.format(day.getTime())); // Thứ Bảy
        if (isSameDay(day, today)) tv_day6.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        day.add(Calendar.DAY_OF_YEAR, 1);

        tv_day7.setText(sdf.format(day.getTime())); // Chủ Nhật
        if (isSameDay(day, today)) tv_day7.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public void updateEvent() {
        List<View> eventRemove = new ArrayList<>(); // Đảm bảo eventRemove là ArrayList để thêm phần tử

        for (RelativeLayout view : listRelativeLayout) {
            for (int i = 0; i < view.getChildCount(); i++) {
                View child = view.getChildAt(i);
                if (child instanceof TextView) {
                    eventRemove.add(child);
                }
            }
        }

        for (RelativeLayout linear : listRelativeLayout) {
            for (View view : eventRemove) {
                linear.removeView(view);
            }
        }

        Calendar startCal = (Calendar) currentWeekStart.clone();
        Calendar endCal = (Calendar) currentWeekStart.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 6); // Thêm 6 ngày để bao gồm cả Chủ Nhật
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        Date start = startCal.getTime();
        Date end = endCal.getTime();

        Log.d("start, end", String.valueOf(startCal.getTime()) + " " + String.valueOf(endCal.getTime()));

        db.collection("events")
                .whereEqualTo("uid", uid)
                .whereGreaterThanOrEqualTo("startTime", startCal.getTime())
                .whereLessThanOrEqualTo("endTime", endCal.getTime())
                .get()
                .addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {

                        for (QueryDocumentSnapshot document : task2.getResult()) {
                            Event event = new Event();
                            Date startTimestamp = document.getDate("startTime");
                            Date endTimestamp = document.getDate("endTime");

                            event.setId(document.getId());
                            event.setTitle(document.getString("title"));
                            event.setStartTime(new Timestamp(startTimestamp));
                            event.setEndTime(new Timestamp(endTimestamp));
                            event.setCalendarId(document.getString("calendarId"));
                            event.setUid(uid);

                            Log.d("eventColor", event.getCalendarId());

                            addEventToLayout(event);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("❌ Lỗi khi lấy dữ liệu: ", e.getMessage());
                });
    }

    public void addEventToLayout(Event event) {
        Date startTimestamp = event.getStartTime().toDate();
        Date endTimestamp = event.getEndTime().toDate();

        Calendar calStart = Calendar.getInstance();
        calStart.setTime(startTimestamp);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(endTimestamp);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);

        // 👉 Tính giờ bắt đầu và kết thúc dạng số thực
        int startHour = calStart.get(Calendar.HOUR_OF_DAY);
        int startMinute = calStart.get(Calendar.MINUTE);
        double startTime = startHour + (startMinute / 60.0);

        int endHour = calEnd.get(Calendar.HOUR_OF_DAY);
        int endMinute = calEnd.get(Calendar.MINUTE);
        double endTime = endHour + (endMinute / 60.0);

        // 👉 Tính chênh lệch thời gian
        long diffMillis = calEnd.getTimeInMillis() - calStart.getTimeInMillis();
        long diffMinutes = diffMillis / (60 * 1000);
        double duration = diffMinutes / 60.0;

        Log.d("duration", event.getTitle() + " " + String.valueOf(duration));
        if (duration <= 10.0 / 60) {
            duration = 15.0 / 60;
        }

        int heightInPx = (int) (duration * 60 * getResources().getDisplayMetrics().density);
        int marginTopInPx = (int) (startTime * 60 * getResources().getDisplayMetrics().density);
        int marginRightInPx = (int) (5 * getResources().getDisplayMetrics().density);
        int paddingLeftInPx = (int) (10 * getResources().getDisplayMetrics().density);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                heightInPx
        );
        params.setMargins(0, marginTopInPx, marginRightInPx, 0);

        TextView eventText = new TextView(this);
        eventText.setText(event.getTitle());
        eventText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        eventText.setTextColor(getResources().getColor(R.color.white));
        eventText.setTypeface(Typeface.DEFAULT_BOLD);
        eventText.setBackgroundResource(R.drawable.custom_backgroud_timeline);
        eventText.setPadding(paddingLeftInPx, 0, 0, 0);

        final String[] colorCode = {""};
        db.collection("calendars")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getId().equals(event.getCalendarId())) {
                                colorCode[0] = document.getString("color");
                                Log.d("eventColor", ">>>>>>>>" + event.getCalendarId() + " " + colorCode[0].toString());
                                Log.d("event", "760 Màu của calendar1: " + colorCode[0]);
                                eventText.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorCode[0])));
                                break;
                            }
                        }
                        if (task.getResult().isEmpty()) {
                            Log.d("event", "Không tìm thấy document nào thỏa mãn");
                        }
                    } else {
                        Log.d("event", "Lỗi: " + task.getException().getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("❌ Lỗi khi lấy dữ liệu: ", e.getMessage());
                });

        eventText.setLayoutParams(params);

        // Xác định ngày của sự kiện và thêm vào RelativeLayout tương ứng
        Calendar eventDay = Calendar.getInstance();
        eventDay.setTime(startTimestamp);
        eventDay.set(Calendar.HOUR_OF_DAY, 0);
        eventDay.set(Calendar.MINUTE, 0);
        eventDay.set(Calendar.SECOND, 0);
        eventDay.set(Calendar.MILLISECOND, 0);

        Calendar weekStart = (Calendar) currentWeekStart.clone(); // Sử dụng calendar từ updateEvent
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = -1;
        for (int i = 0; i < 7; i++) {
            if (isSameDay(eventDay, weekStart)) {
                dayOfWeek = i;
                break;
            }
            weekStart.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (dayOfWeek >= 0 && dayOfWeek < listRelativeLayout.size()) {
            RelativeLayout layout = listRelativeLayout.get(dayOfWeek);
            if (layout != null) {
                layout.addView(eventText);
            } else {
                Log.d("Debug", "RelativeLayout for day " + dayOfWeek + " is null");
            }
        } else {
            Log.d("Debug", "Event day not in current week or invalid index");
        }
    }

}