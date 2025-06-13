package com.example.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.models.StudyGoal;
import com.example.time4study.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddGoalActivity extends AppCompatActivity {

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private MaterialButton saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private Date selectedStartDate;
    private Date selectedEndDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private String goalId; // For editing mode
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        saveButton = findViewById(R.id.saveButton);

        // Check if we're in edit mode
        goalId = getIntent().getStringExtra("GOAL_ID");
        if (goalId != null) {
            isEditMode = true;
            loadGoalData();
        }

        // Set up date pickers
        setupDatePickers();

        // Set up save button
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isEditMode) {
                    updateGoal();
                } else {
                    saveGoal();
                }
            }
        });
    }

    private void loadGoalData() {
        if (goalId == null) return;

        db.collection("studyGoals").document(goalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        StudyGoal goal = documentSnapshot.toObject(StudyGoal.class);
                        if (goal != null) {
                            titleInput.setText(goal.getTitle());
                            descriptionInput.setText(goal.getDescription());

                            if (goal.getStartDate() != null) {
                                selectedStartDate = goal.getStartDate().toDate();
                                startDateInput.setText(dateFormat.format(selectedStartDate));
                            }

                            if (goal.getEndDate() != null) {
                                selectedEndDate = goal.getEndDate().toDate();
                                endDateInput.setText(dateFormat.format(selectedEndDate));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading goal information: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGoal() {
        if (goalId == null) return;

        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        Map<String, Object> goalData = new HashMap<>();
        goalData.put("title", title);
        goalData.put("description", description);
        goalData.put("startDate", new Timestamp(selectedStartDate));
        goalData.put("endDate", new Timestamp(selectedEndDate));

        db.collection("studyGoals").document(goalId)
                .update(goalData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddGoalActivity.this, "Goal updated successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddGoalActivity.this, "Error updating goal: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDatePickers() {
        // Handle start date selection
        startDateInput.setOnClickListener(v -> showDatePicker(true));
        // Handle end date selection
        endDateInput.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        final Calendar calendar = Calendar.getInstance();
        Date initDate = isStart ? selectedStartDate : selectedEndDate;
        if (initDate != null) calendar.setTime(initDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dateDialog = new DatePickerDialog(this, (view, y, m, d) -> {
            calendar.set(y, m, d);
            // After selecting the date, select the time
            showTimePicker(isStart, calendar);
        }, year, month, day);
        dateDialog.show();
    }

    private void showTimePicker(boolean isStart, Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timeDialog = new TimePickerDialog(this, (view, h, m) -> {
            calendar.set(Calendar.HOUR_OF_DAY, h);
            calendar.set(Calendar.MINUTE, m);
            calendar.set(Calendar.SECOND, 0);
            if (isStart) {
                selectedStartDate = calendar.getTime();
                startDateInput.setText(dateFormat.format(selectedStartDate));
            } else {
                selectedEndDate = calendar.getTime();
                endDateInput.setText(dateFormat.format(selectedEndDate));
            }
        }, hour, minute, true);
        timeDialog.show();
    }

    private boolean validateInputs() {
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        String description = descriptionInput.getText() != null ? descriptionInput.getText().toString().trim() : "";

        // Validate input
        if (title.isEmpty()) {
            titleInput.setError("Please enter a title");
            return false;
        }
        if (selectedStartDate == null) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedEndDate == null) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedEndDate.before(selectedStartDate)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveGoal() {
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        String description = descriptionInput.getText() != null ? descriptionInput.getText().toString().trim() : "";

        // Validate input
        if (title.isEmpty()) {
            titleInput.setError("Please enter a title");
            return;
        }
        if (selectedStartDate == null) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedEndDate == null) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedEndDate.before(selectedStartDate)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get current user
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Please log in to add a goal", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get current time as createdAt
        Timestamp now = Timestamp.now();
        // Calculate minutes between startDate and endDate
        long diffMillis = selectedEndDate.getTime() - selectedStartDate.getTime();
        int targetDurationMinutes = (int) (diffMillis / (60 * 1000));
        // Create Map to save all fields
        Map<String, Object> goalData = new HashMap<>();
        goalData.put("title", title);
        goalData.put("description", description);
        goalData.put("uid", userId);
        goalData.put("createdAt", now);
        goalData.put("startDate", new Timestamp(selectedStartDate));
        goalData.put("endDate", new Timestamp(selectedEndDate));
        goalData.put("targetDurationMinutes", targetDurationMinutes);
        goalData.put("actualDurationMinutes", 0); // Default to 0 when newly created
        goalData.put("completed", false);
        goalData.put("progress", "0% (actual/target)");
        db.collection("studyGoals")
                .add(goalData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddGoalActivity.this, "Goal added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddGoalActivity.this, "Error adding goal: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}