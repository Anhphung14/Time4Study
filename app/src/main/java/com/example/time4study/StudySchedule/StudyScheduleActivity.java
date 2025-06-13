package com.example.time4study.StudySchedule;

import android.app.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.time4study.R;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudyScheduleActivity extends AppCompatActivity {

    // LAYOUT RIGHT THERE
    private RelativeLayout relativeLayout_timeline_table;
    private LinearLayout linearLayout_task;
    private ImageButton buttonMenu;
    private TextView textView, textDay1, textDay2, textDay3;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton fab_add_new_task, fab_add_new_event;

    // ANOTHER
    private float dY;
    private float initialY;
    private Drawable originalBackground;
    private boolean isDraggingAllowed = false;
    private final long DELAY_MILLIS = 500;
    private HashMap<String, Integer> dynamicIdMap = new HashMap<>();
    private Calendar calendar;
    private GestureDetector gestureDetector;
    private ImageView previousCheckedColor = null;
    private HashMap<String, String> calendarIds = new HashMap<>();
    private Calendar startCalendar;
    private HashMap<String, String> calendarMap = new HashMap<>();

    //Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_study_schedule);

        init();

        calendar = Calendar.getInstance();
        updateCurrentDate();
        updateTask();
        updateCalendar();
        updateEvent();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.open();
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.navViewDay:
                        Log.d("nav", "navviewday");
                        return true;
                    case R.id.navViewWeek:
                        Log.d("nav", "navviewweek");
                        return true;
                    case R.id.navViewMonth:
                        Log.d("nav", "navviewmonth");
                        return true;
                }

                if (item.getTitle().equals("Add new Calendar")) {
                    addCalendar();
                }

                View actionView = item.getActionView();
                if (actionView != null) {
                    CheckBox checkbox = actionView.findViewById(R.id.checkbox);
                    checkbox.setChecked(!checkbox.isChecked());
                }

                return false;
            }
        });



        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (diffX > 100 && Math.abs(velocityX) > 100) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    updateCurrentDate();
                    updateTask();
                    updateEvent();
                } else if (diffX < -100 && Math.abs(velocityX) > 100) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    updateCurrentDate();
                    updateTask();
                    updateEvent();
                }
                return true;
            }
        });

        fab_add_new_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogAddEvent();
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
        textView = findViewById(R.id.textView_event);
        textDay1 = findViewById(R.id.textDay1);
        textDay2 = findViewById(R.id.textDay2);
        textDay3 = findViewById(R.id.textDay3);

        relativeLayout_timeline_table = findViewById(R.id.relativelayout_timeline_table);
        linearLayout_task = findViewById(R.id.linearlayout_tasks);
        buttonMenu = findViewById(R.id.iconMenu);
        drawerLayout = findViewById(R.id.calendar_view_day_drawer);
        navigationView = findViewById(R.id.navigation_view);

        fab_add_new_task = findViewById(R.id.fab_add_new_task);
        fab_add_new_event = findViewById(R.id.fab_add_new_event);

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void addTasks(LinearLayout linearLayout_task, int taskId, String taskName) {
        int paddingStart = (int) (10 * getResources().getDisplayMetrics().density);
        int padding = (int) (2 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, (int) (3 * getResources().getDisplayMetrics().density));

        TextView textView = new TextView(StudyScheduleActivity.this);

        textView.setId(taskId);
        textView.setLayoutParams(params);
        textView.setBackgroundResource(R.drawable.custom_background_task);
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPaddingRelative(paddingStart,padding, padding, padding);
        textView.setText(taskName);

        linearLayout_task.addView(textView);
    }

    public int generateId (String taskIdString) {
        Integer id = dynamicIdMap.get(taskIdString);
        if (id == null) {
            id = View.generateViewId();
            dynamicIdMap.put(taskIdString, id);
        }

        return id;
    }

    public void updateCurrentDate() {

        SimpleDateFormat day1Format = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat day2Format = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat day3Format = new SimpleDateFormat("MMMM", Locale.getDefault());

        String day1 = day1Format.format(calendar.getTime());
        String day2 = day2Format.format(calendar.getTime());
        String day3 = day3Format.format(calendar.getTime());

        textDay1.setText(day1);
        textDay2.setText(day2);
        textDay3.setText(day3);

        Calendar currentDate = Calendar.getInstance();

        boolean isToday = calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH);

        if (isToday) {
            textDay2.setBackgroundResource(R.drawable.custom_background_day_study_schedule);
        } else {
            textDay2.setBackgroundResource(0);
        }
    }

    public void updateTask() {
        linearLayout_task.removeAllViews();

        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        ArrayList<String> listTask = new ArrayList<>();

        db.collection("tasks")
                .whereEqualTo("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62")
                .whereGreaterThanOrEqualTo("date", start.getTime())
                .whereLessThanOrEqualTo("date", end.getTime())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            Date date = document.getDate("date");
                            listTask.add(title);
//                            Log.d("Firestore", "Task: title=" + title + ", date=" + date);
                        }

                        for (int i = 0; i < listTask.size(); i++) {
                            String stringTaskId = "Task" + (i + 1);
                            addTasks(linearLayout_task, generateId(stringTaskId), listTask.get(i));
                        }

                    } else {
                        Log.d("Firestore", "Loi khi truy van ", task.getException());
                    }
                });
    }

    public void updateCalendar() {
        Menu menu = navigationView.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getGroupId() == R.id.group2 || item.getGroupId() == R.id.group3) {
                menu.removeItem(item.getItemId());
                i--;
            }
        }

        HashMap<String, Integer> hashMapCalendar = new HashMap<>();


        db.collection("calendars")
                .whereEqualTo("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("Calendar", document.getString("title") + " " + document.getString("color"));
                            hashMapCalendar.put(document.getString("title"), Color.parseColor(document.getString("color")));
                            calendarIds.put(document.getString("title"), document.getId());
                        }

                        for (Map.Entry<String, Integer> entry : hashMapCalendar.entrySet()) {
                            MenuItem item = menu.add(R.id.group2, Menu.NONE, Menu.NONE, entry.getKey());
                            item.setCheckable(true);
                            item.setActionView(R.layout.menu_item_checkbox); // Layout chứa checkbox và text

                            // Thiết lập màu (custom view)
                            View customView = item.getActionView();
                            CheckBox checkBox = customView.findViewById(R.id.checkbox);
//                            TextView textView = customView.findViewById(R.id.title);
//                            textView.setText(labels.get(i));
                            checkBox.setButtonTintList(ColorStateList.valueOf(entry.getValue()));

                            ImageButton recycleBin = customView.findViewById(R.id.delete_calendar);
                            ImageButton editPen = customView.findViewById(R.id.edit_calendar);

                            final Handler handler = new Handler();
                            final long startTime = System.currentTimeMillis();
                            final long delay = 500; // 1.5 giây

                            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                Log.d("Task", ">>>>>>> " + item.getTitle().toString() + " checked: " + isChecked);

                                // Xử lý logic dựa trên tiêu đề và trạng thái checkbox
                                if (item.getTitle().toString().equals("Task") && isChecked) {
                                    linearLayout_task.setVisibility(View.VISIBLE);
                                } else if (item.getTitle().toString().equals("Task") && !isChecked) {
                                    linearLayout_task.setVisibility(View.GONE);
                                }

                                Log.d("task", "<<<<<<<<<<<<<< " + item.getTitle() + ": " + calendarIds.get(item.getTitle()));
                            });


                            // xoa calendar
                            recycleBin.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Log.d("recycleBin", "da vao day " + item.getTitle().toString() + ": " + calendarIds.get(item.getTitle()));
                                    deleteCalendar(item.getTitle().toString(), calendarIds.get(item.getTitle()));
                                }
                            });


                            // sua calendar
                            editPen.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Log.d("editpen", "da vao day " + item.getTitle().toString() + ": " + calendarIds.get(item.getTitle()));
                                    editCalendar(item.getTitle().toString(), calendarIds.get(item.getTitle()));
                                }
                            });
                        }

                        MenuItem item = menu.add(R.id.group3, Menu.NONE, Menu.NONE, "Add new Calendar");
                        item.setIcon(R.drawable.ic_add_calendar);

                    } else {
                        Log.d("Calendar", "Loi khi truy van " + task.getException());
                    }
                });
    }

    public void addCalendar() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_dialog_add_calendar, null);

        EditText title = dialogView.findViewById(R.id.title_new_calendar);
        GridLayout gridLayout = dialogView.findViewById(R.id.gridlayout);

        final String[] colorCode = {""};
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            FrameLayout itemCalendar = (FrameLayout) gridLayout.getChildAt(i);

            ImageButton imgbtnn = (ImageButton) itemCalendar.getChildAt(0);
            ImageView imgvieww = (ImageView) itemCalendar.getChildAt(1);

            itemCalendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int color = imgbtnn.getBackgroundTintList().getDefaultColor();

                    colorCode[0] = String.format("#%06X", 0xFFFFFF & color);
                    Log.d("colorCode", colorCode[0]);

                    if (previousCheckedColor != null && previousCheckedColor != imgvieww) {
                        previousCheckedColor.setVisibility(View.GONE);
                    }

                    if (imgvieww.getVisibility() == View.VISIBLE) {
                        imgvieww.setVisibility(View.GONE);
                        previousCheckedColor = null;
                        colorCode[0] = "";
                    } else {
                        imgvieww.setVisibility(View.VISIBLE);
                        previousCheckedColor = imgvieww;
                    }

                }
            });
        }

        new AlertDialog.Builder(this)
                .setTitle("Add new Calendar")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    Map<String, Object> newCalendar = new HashMap<>();

                    if (colorCode[0] == null || colorCode[0] == "") {
                        colorCode[0] = "#F06292";
                    }
                    newCalendar.put("color", colorCode[0]);
                    newCalendar.put("title", title.getText().toString());
                    newCalendar.put("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62");

                    db.collection("calendars")
                            .add(newCalendar)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("addCalendar", "Da them tai lieu moi voi id " + documentReference.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.d("addCalendar", "Loi roi " + e.getMessage());
                            });

                    updateCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

    public void deleteCalendar(String title, String calendarId) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch?")
                .setMessage("Bạn có chắc muốn xóa " + title + " không?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("calendars")
                            .document(calendarId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Deleted Calendar " + title, Toast.LENGTH_SHORT).show();
                                Log.d("Calendar", "Đã xóa calendar với ID: " + calendarId);
                                updateCalendar();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting!", Toast.LENGTH_SHORT).show();
                                Log.d("Calendar", "Lỗi khi xóa: " + e.getMessage());
                            });

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void editCalendar(String oldTitle, String calendarId) {

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_dialog_add_calendar, null);

        EditText title = dialogView.findViewById(R.id.title_new_calendar);
        GridLayout gridLayout = dialogView.findViewById(R.id.gridlayout);

        title.setText(oldTitle);

        final String[] colorCode = {""};
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            FrameLayout itemCalendar = (FrameLayout) gridLayout.getChildAt(i);

            ImageButton imgbtnn = (ImageButton) itemCalendar.getChildAt(0);
            ImageView imgvieww = (ImageView) itemCalendar.getChildAt(1);

            itemCalendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int color = imgbtnn.getBackgroundTintList().getDefaultColor();

                    colorCode[0] = String.format("#%06X", 0xFFFFFF & color);
                    Log.d("colorCode", colorCode[0]);

                    if (previousCheckedColor != null && previousCheckedColor != imgvieww) {
                        previousCheckedColor.setVisibility(View.GONE);
                    }

                    if (imgvieww.getVisibility() == View.VISIBLE) {
                        imgvieww.setVisibility(View.GONE);
                        previousCheckedColor = null;
                        colorCode[0] = "";
                    } else {
                        imgvieww.setVisibility(View.VISIBLE);
                        previousCheckedColor = imgvieww;
                    }

                }
            });
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Calendar")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = title.getText().toString();

                    Map<String, Object> updates = new HashMap<>();

                    if (colorCode[0] == null || colorCode[0] == "") {
                        colorCode[0] = "#F06292";
                    }

                    updates.put("title", newTitle);
                    updates.put("color", colorCode[0]);

                    db.collection("calendars")
                            .document(calendarId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Saved Calendar", Toast.LENGTH_SHORT).show();
                                updateCalendar(); // reload lại menu
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error updating!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

    public void updateEvent() {

        List<View> eventRemove = new ArrayList<>();

        for (int i = 0; i < relativeLayout_timeline_table.getChildCount(); i++) {
            View child = relativeLayout_timeline_table.getChildAt(i);
            if (child instanceof TextView) {
                eventRemove.add(child);
            }
        }

        for (View view : eventRemove) {
            relativeLayout_timeline_table.removeView(view);
        }


        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);


        db.collection("events")
                .whereEqualTo("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62")
                .whereGreaterThanOrEqualTo("startTime", start.getTime())
                .whereLessThanOrEqualTo("endTime", end.getTime())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String calendarId = document.getString("calendarId");
                            Date startTimestamp = document.getDate("startTime");
                            Date endTimestamp = document.getDate("endTime");
                            String eventId = document.getId();

                            Calendar calStart = Calendar.getInstance();
                            calStart.setTime(startTimestamp);
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
                            double startDecimal = startHour + (startMinute / 60.0);

                            int endHour = calEnd.get(Calendar.HOUR_OF_DAY);
                            int endMinute = calEnd.get(Calendar.MINUTE);
                            double endDecimal = endHour + (endMinute / 60.0);

                            // 👉 Tính chênh lệch thời gian
                            long diffMillis = calEnd.getTimeInMillis() - calStart.getTimeInMillis();
                            long diffMinutes = diffMillis / (60 * 1000);
                            double diffHours = diffMinutes / 60.0;

                            Log.d("event", calendarId + " | " + title + " | Start: " + startDecimal + " | End: " + endDecimal +
                                    " | Duration: " + diffMinutes + " phút = " + diffHours + " giờ");

                            addEventToLayout(eventId, title, calendarId, startDecimal, endDecimal, diffHours);

                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("❌ Lỗi khi lấy dữ liệu: ", e.getMessage());
                });
    }

    public void addEventToLayout(String eventId, String title, String calendarId, double startTime, double endTime, double duration) {
        Log.d("duration", title + " " + String.valueOf(duration));
        if (duration <= 10.0 / 60) {
            duration = 15.0 / 60;
        }


        int heightInPx = (int) (duration * 60 * getResources().getDisplayMetrics().density);
        int marginTopInPx = (int) (startTime * 60 * getResources().getDisplayMetrics().density);
        int marginRightInPx = (int) (5 * getResources().getDisplayMetrics().density);
        int paddingLeftInPx = (int) (10 * getResources().getDisplayMetrics().density);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, // hoặc WRAP_CONTENT nếu bạn muốn tự co chiều ngang
                heightInPx
        );

        params.setMargins(0, marginTopInPx, marginRightInPx, 0);

        TextView event = new TextView(this);
        event.setText(title);
        if (duration == 15.0 / 60) {
            event.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        }
        event.setTextColor(getResources().getColor(R.color.white));
        event.setTypeface(Typeface.DEFAULT_BOLD);
        event.setBackgroundResource(R.drawable.custom_backgroud_timeline);
        event.setPadding(paddingLeftInPx, 0, 0, 0);

        final String[] colorCode = {""};
        db.collection("calendars")
                .whereEqualTo("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getId().equals(calendarId)) {
                                colorCode[0] = document.getString("color");
                                Log.d("event", "Màu của calendar1: " + colorCode[0]);

                                event.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorCode[0])));
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

        event.setLayoutParams(params);

        event.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("event", "Dang click " + title);
                showEventOptionDialog(eventId);
                return false;
            }
        });

        relativeLayout_timeline_table.addView(event);
    }

    private void showDialogAddEvent() {
        // Inflate layout dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        // Khởi tạo các thành phần
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        TextView tvDate = dialogView.findViewById(R.id.tv_date);
        TextView tvStartTime = dialogView.findViewById(R.id.tv_start_time);
        TextView tvEndTime = dialogView.findViewById(R.id.tv_end_time);
        Spinner spinner = dialogView.findViewById(R.id.spinner);

        getAllCalendarTitle("DWbD8vyeaZgcyST9VpisMGSrcZ62", new OnCalendarLoadedListener() {
            @Override
            public void onCalendarLoaded(HashMap<String, String> calendarMap) {
                StudyScheduleActivity.this.calendarMap.clear(); // Xóa dữ liệu cũ
                StudyScheduleActivity.this.calendarMap.putAll(calendarMap);

                ArrayList<String> spinnerItems = new ArrayList<>(calendarMap.values());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(StudyScheduleActivity.this, android.R.layout.simple_spinner_item, spinnerItems);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("spinnerCalendar", "Loi roi");
            }
        }, spinner);

        // Cài đặt Spinner
//        List<String> spinnerItems = Arrays.asList("calendar1", "calendar2", "calendar3", "calendar4", "calendar5");
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinnerItems);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);

        // Khởi tạo calendar
        startCalendar = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();

        // Cài đặt DatePicker
        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        startCalendar.set(year, month, dayOfMonth);
                        tvDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Cài đặt Start TimePicker
        tvStartTime.setOnClickListener(v -> {
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view, hourOfDay, minute, second) -> {
                        minute = (minute / 5) * 5;
                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startCalendar.set(Calendar.MINUTE, minute);
                        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    (calendar.get(Calendar.MINUTE) / 5) * 5,
                    true
            );
            tpd.setTimeInterval(1, 5);
            tpd.show(this.getSupportFragmentManager(), "StartTimePickerDialog");
        });

        // Cài đặt End TimePicker
        tvEndTime.setOnClickListener(v -> {
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view, hourOfDay, minute, second) -> {
                        minute = (minute / 5) * 5;
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    (calendar.get(Calendar.MINUTE) / 5) * 5,
                    true
            );
            if (!tvStartTime.getText().toString().isEmpty()) {
                tpd.setMinTime(
                        startCalendar.get(Calendar.HOUR_OF_DAY),
                        startCalendar.get(Calendar.MINUTE) + 5,
                        0
                );
            }
            tpd.setTimeInterval(1, 5);
            tpd.show(this.getSupportFragmentManager(), "EndTimePickerDialog");
        });

        // Tạo AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add new event")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String title = etTitle.getText().toString();
                    String date = tvDate.getText().toString();
                    String startTime = tvStartTime.getText().toString();
                    String endTime = tvEndTime.getText().toString();
                    String calendarItem = spinner.getSelectedItem().toString();

                    // Kiểm tra dữ liệu hợp lệ
                    if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                        Toast.makeText(this, "Vui lòng điền đầy đủ các trường", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Chuyển đổi thành Timestamp
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    try {
                        Date startDateTime = sdf.parse(date + " " + startTime);
                        Date endDateTime = sdf.parse(date + " " + endTime);
                        if (!endDateTime.after(startDateTime)) {
                            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Tạo Timestamp cho Firestore
                        Timestamp startTimestamp = new Timestamp(startDateTime);
                        Timestamp endTimestamp = new Timestamp(endDateTime);

                        String calendarId = getKeyByValue(calendarMap, calendarItem);
                        Log.d("calendarId", calendarId);

                        addEvent(title, startTimestamp, endTimestamp, calendarId, "DWbD8vyeaZgcyST9VpisMGSrcZ62");

                    } catch (ParseException e) {
                        Toast.makeText(this, "Định dạng ngày hoặc giờ không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }

    public void showDialogEditEvent(String eventId, String oldTitle, String oldDate, String oldStartTime, String oldEndTime) {
        // Inflate layout dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        // Khởi tạo các thành phần
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        TextView tvDate = dialogView.findViewById(R.id.tv_date);
        TextView tvStartTime = dialogView.findViewById(R.id.tv_start_time);
        TextView tvEndTime = dialogView.findViewById(R.id.tv_end_time);
        Spinner spinner = dialogView.findViewById(R.id.spinner);

        etTitle.setText(oldTitle);
//        tvDate.setText(oldDate);
//        tvStartTime.setText(oldStartTime);
//        tvEndTime.setText(oldEndTime);

        getAllCalendarTitle("DWbD8vyeaZgcyST9VpisMGSrcZ62", new OnCalendarLoadedListener() {
            @Override
            public void onCalendarLoaded(HashMap<String, String> calendarMap) {
                StudyScheduleActivity.this.calendarMap.clear(); // Xóa dữ liệu cũ
                StudyScheduleActivity.this.calendarMap.putAll(calendarMap);

                ArrayList<String> spinnerItems = new ArrayList<>(calendarMap.values());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(StudyScheduleActivity.this, android.R.layout.simple_spinner_item, spinnerItems);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("spinnerCalendar", "Loi roi");
            }
        }, spinner);

        // Khởi tạo calendar
        startCalendar = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();

        // Cài đặt DatePicker
        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        startCalendar.set(year, month, dayOfMonth);
                        tvDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Cài đặt Start TimePicker
        tvStartTime.setOnClickListener(v -> {
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view, hourOfDay, minute, second) -> {
                        minute = (minute / 5) * 5;
                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startCalendar.set(Calendar.MINUTE, minute);
                        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    (calendar.get(Calendar.MINUTE) / 5) * 5,
                    true
            );
            tpd.setTimeInterval(1, 5);
            tpd.show(this.getSupportFragmentManager(), "StartTimePickerDialog");
        });

        // Cài đặt End TimePicker
        tvEndTime.setOnClickListener(v -> {
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view, hourOfDay, minute, second) -> {
                        minute = (minute / 5) * 5;
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    (calendar.get(Calendar.MINUTE) / 5) * 5,
                    true
            );
            if (!tvStartTime.getText().toString().isEmpty()) {
                tpd.setMinTime(
                        startCalendar.get(Calendar.HOUR_OF_DAY),
                        startCalendar.get(Calendar.MINUTE) + 5,
                        0
                );
            }
            tpd.setTimeInterval(1, 5);
            tpd.show(this.getSupportFragmentManager(), "EndTimePickerDialog");
        });

        // Tạo AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit event")
                .setView(dialogView)
                .setPositiveButton("Edit", (d, which) -> {
                    String title = etTitle.getText().toString();
                    String date = tvDate.getText().toString();
                    String startTime = tvStartTime.getText().toString();
                    String endTime = tvEndTime.getText().toString();
                    String calendarItem = spinner.getSelectedItem().toString();

                    // Kiểm tra dữ liệu hợp lệ
                    if (title.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                        Toast.makeText(this, "Vui lòng điền đầy đủ các trường", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Chuyển đổi thành Timestamp
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    try {
                        Date startDateTime = sdf.parse(date + " " + startTime);
                        Date endDateTime = sdf.parse(date + " " + endTime);
                        if (!endDateTime.after(startDateTime)) {
                            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Tạo Timestamp cho Firestore
                        Timestamp startTimestamp = new Timestamp(startDateTime);
                        Timestamp endTimestamp = new Timestamp(endDateTime);

                        String calendarId = getKeyByValue(calendarMap, calendarItem);
                        Log.d("calendarId", calendarId);

                        Log.d("editEvent", eventId + " " + title + " " + startTimestamp.toString() + " " + endTimestamp.toString() + " " + calendarId);

                        editEvent(eventId, title, startTimestamp, endTimestamp, calendarId);

                    } catch (ParseException e) {
                        Log.d("error",  e.getMessage());
                        return;
                    }
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }

    public void addEvent(String title, Timestamp startTimestamp, Timestamp endTimestamp, String calendarId, String uid) {
        Map<String, Object> event = new HashMap<>();

        event.put("title", title);
        event.put("startTime", startTimestamp);
        event.put("endTime", endTimestamp);
        event.put("calendarId", calendarId);
        event.put("uid", "DWbD8vyeaZgcyST9VpisMGSrcZ62");
        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "update sự kiện thành công", Toast.LENGTH_SHORT).show();
                    updateEvent();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi update sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public interface OnCalendarLoadedListener {
        void onCalendarLoaded(HashMap<String, String> calendarMap);
        void onError(String errorMessage);
    }

    public void getAllCalendarTitle(String uid, OnCalendarLoadedListener listener, Spinner spinner) {
        HashMap<String, String> mapAllCalendar = new HashMap<>();
        ArrayList<String> spinnerItems = new ArrayList<>();

        db.collection("calendars")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("allCalendar", document.getId() + " " + document.getString("title"));

                            mapAllCalendar.put(document.getId(), document.getString("title"));
                        }

                        spinnerItems.addAll(mapAllCalendar.values());
                        listener.onCalendarLoaded(mapAllCalendar);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("allCalendar", "Co loi khi truy van allCalendar " + e.getMessage());
                });
    }

    public void deleteEvent(String eventId) {
        db.collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    updateEvent();
                    Log.d("Firestore", "Event deleted with ID: " + eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting event", e);
                });
    }

    public void editEvent(String eventId, String title, Timestamp startTimestamp, Timestamp endTimestamp, String calendarId) {
        Map<String, Object> event = new HashMap<>();

        event.put("title", title);
        event.put("startTime", startTimestamp);
        event.put("endTime", endTimestamp);
        event.put("calendarId", calendarId);

        db.collection("events")
                .document(eventId)
                .update(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Sự kiện đã được update thanh cong", Toast.LENGTH_SHORT).show();
                    updateEvent();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi update sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getKeyByValue(HashMap<String, String> map, String value) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;

    }

    public void showEventOptionDialog(String eventId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog_remove_edit_event, null);

        TextView btnEdit = dialogView.findViewById(R.id.txtEditEvent);
        TextView btnDelete = dialogView.findViewById(R.id.txtDeleteEvent);

        // Xử lý nhấn edit
        btnEdit.setOnClickListener(v -> {
            Log.d("Dialog", "Edit clicked for event ID: " + eventId);

            db.collection("events")
                    .document(eventId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();

                            String oldTitle = document.getString("title");
                            Date oldStartTime = document.getDate("startTime");
                            Date oldEnTime = document.getDate("endTime");

                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy", Locale.getDefault());
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                            String oldDate = dateFormat.format(oldStartTime);
                            String startTime = timeFormat.format(oldStartTime);
                            String endTime = timeFormat.format(oldEnTime);

                            showDialogEditEvent(eventId, oldTitle, oldDate, startTime, endTime);
                        }

                    });
        });

        // Xử lý nhấn delete
        btnDelete.setOnClickListener(v -> {
            deleteEvent(eventId); // Gọi hàm xóa
            updateEvent();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}