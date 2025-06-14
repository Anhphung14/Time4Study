package com.example.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.models.StudyGoal;
import com.example.adapters.TasksAdapter;
import com.example.time4study.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import com.example.models.GoalTask;
import java.util.ArrayList;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.TextUtils;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.DialogInterface;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GoalDetailBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_GOAL_TITLE = "goal_title";
    private static final String ARG_GOAL_DESC = "goal_desc";
    private static final String ARG_GOAL_START = "goal_start";
    private static final String ARG_GOAL_END = "goal_end";
    private static final String ARG_GOAL_PROGRESS = "goal_progress";
    private static final String ARG_GOAL_COMPLETED = "goal_completed";
    private static final String ARG_GOAL_TASKS = "goal_tasks";

    private RecyclerView tasksRecyclerView;
    private TasksAdapter tasksAdapter;
    private LinearProgressIndicator progressBar;
    private TextView progressTextView;
    private ArrayList<GoalTask> currentTasks = new ArrayList<>();
    private String goalId;
    private FirebaseFirestore db;

    public interface OnGoalChangedListener {
        void onGoalChanged();
    }
    private OnGoalChangedListener goalChangedListener;
    public void setOnGoalChangedListener(OnGoalChangedListener listener) {
        this.goalChangedListener = listener;
    }

    public static GoalDetailBottomSheet newInstance(StudyGoal goal) {
        GoalDetailBottomSheet fragment = new GoalDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_GOAL_TITLE, goal.getTitle());
        args.putString(ARG_GOAL_DESC, goal.getDescription());
        args.putString(ARG_GOAL_START, goal.getStartDate() != null ? goal.getStartDate().toDate().toString() : "");
        args.putString(ARG_GOAL_END, goal.getEndDate() != null ? goal.getEndDate().toDate().toString() : "");
        args.putString(ARG_GOAL_PROGRESS, goal.getProgress());
        args.putBoolean(ARG_GOAL_COMPLETED, goal.isCompleted());
        args.putSerializable(ARG_GOAL_TASKS, goal.getTasks() != null ? new ArrayList<>(goal.getTasks()) : new ArrayList<GoalTask>());
        args.putString("goal_id", goal.getId());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_goal_detail, container, false);
        Bundle args = getArguments();
        db = FirebaseFirestore.getInstance();
        if (args != null) {
            goalId = args.getString("goal_id", null);
        }
        MaterialCardView cardView = new MaterialCardView(requireContext());
        cardView.setCardElevation(8f);
        cardView.setRadius(18f);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.card));
        cardView.setUseCompatPadding(true);
        cardView.addView(view);

        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView descriptionTextView = view.findViewById(R.id.descriptionTextView);
        TextView startDateTextView = view.findViewById(R.id.startDateTextView);
        TextView endDateTextView = view.findViewById(R.id.endDateTextView);
        TextView progressTextView = view.findViewById(R.id.progressTextView);
        TextView completedStatus = view.findViewById(R.id.completedStatusTextView);
        progressBar = view.findViewById(R.id.progressBar);
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        tasksAdapter = new TasksAdapter();
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRecyclerView.setAdapter(tasksAdapter);
        view.findViewById(R.id.addTaskButton).setOnClickListener(v -> showAddTaskDialog());

        if (args != null) {
            titleTextView.setText(args.getString(ARG_GOAL_TITLE, ""));
            descriptionTextView.setText(args.getString(ARG_GOAL_DESC, ""));
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String startDateStr = args.getString(ARG_GOAL_START, "");
            String endDateStr = args.getString(ARG_GOAL_END, "");
            if (startDateStr != null && !startDateStr.isEmpty() && !startDateStr.equals("null")) {
                try {
                    java.util.Date date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(startDateStr);
                    startDateTextView.setText(df.format(date));
                } catch (Exception e) {
                    startDateTextView.setText("No start date");
                }
            } else {
                startDateTextView.setText("No start date");
            }
            if (endDateStr != null && !endDateStr.isEmpty() && !endDateStr.equals("null")) {
                try {
                    java.util.Date date = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(endDateStr);
                    endDateTextView.setText(df.format(date));
                } catch (Exception e) {
                    endDateTextView.setText("No end date");
                }
            } else {
                endDateTextView.setText("No end date");
            }
            completedStatus.setVisibility(args.getBoolean(ARG_GOAL_COMPLETED, false) ? View.VISIBLE : View.GONE);
            if (args.getBoolean(ARG_GOAL_COMPLETED, false)) {
                completedStatus.setText("✓ Completed");
                completedStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                completedStatus.setText("");
            }
            if (args.getSerializable(ARG_GOAL_TASKS) != null) {
                currentTasks = (ArrayList<GoalTask>) args.getSerializable(ARG_GOAL_TASKS);
                tasksAdapter.setTasks(currentTasks);
            }
            updateProgressUI(progressTextView);
        }
        tasksAdapter.setOnTaskEditListener(this::showEditTaskDialog);
        tasksAdapter.setOnTaskDeleteListener(this::showDeleteTaskConfirmation);
        return cardView;
    }

    private void updateProgressUI(TextView progressTextView) {
        int total = currentTasks.size();
        int completed = 0;
        for (GoalTask t : currentTasks) {
            if (t.isCompleted()) completed++;
        }
        int percent = (total > 0) ? (completed * 100 / total) : 0;
        progressBar.setProgress(percent);
        progressTextView.setText(percent + "%");
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add new task");
        final EditText input = new EditText(requireContext());
        input.setHint("Task name");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String taskTitle = input.getText().toString().trim();
            if (!TextUtils.isEmpty(taskTitle)) {
                if (goalId == null) {
                    Toast.makeText(requireContext(), "Goal not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, Object> taskData = new HashMap<>();
                taskData.put("title", taskTitle);
                taskData.put("isCompleted", false);
                taskData.put("plannedMinutes", 0);
                taskData.put("actualMinutes", 0);
                db.collection("studyGoals").document(goalId)
                    .collection("goalTasks")
                    .add(taskData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(requireContext(), "Task added!", Toast.LENGTH_SHORT).show();
                        loadGoalTasksAndUpdateUI();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Error adding task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(requireContext(), "Please enter a task name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateTaskCompletion(GoalTask task, boolean isCompleted) {
        if (goalId == null || task.getId() == null) {
            Toast.makeText(requireContext(), "Goal or task not found", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("studyGoals").document(goalId)
            .collection("goalTasks").document(task.getId())
            .update("isCompleted", isCompleted)
            .addOnSuccessListener(aVoid -> {
                task.setCompleted(isCompleted);
                loadGoalTasksAndUpdateUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error updating task status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadGoalTasksAndUpdateUI() {
        if (goalId == null) return;
        db.collection("studyGoals").document(goalId)
            .collection("goalTasks")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                ArrayList<GoalTask> tasks = new ArrayList<>();
                int completedCount = 0;
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    GoalTask task = doc.toObject(GoalTask.class);
                    task.setId(doc.getId());
                    Boolean isCompleted = doc.getBoolean("isCompleted");
                    task.setCompleted(isCompleted != null && isCompleted);
                    tasks.add(task);
                    if (task.isCompleted()) completedCount++;
                }
                currentTasks = tasks;
                tasksAdapter.setTasks(currentTasks);
                int percent = (tasks.size() > 0) ? (completedCount * 100 / tasks.size()) : 0;
                progressBar.setProgress(percent);
                if (getView() != null) {
                    TextView progressTextView = getView().findViewById(R.id.progressTextView);
                    progressTextView.setText(percent + "%");
                }
                // Cập nhật trạng thái completed của goal
                boolean isCompleted = (tasks.size() > 0 && completedCount == tasks.size());
                db.collection("studyGoals").document(goalId)
                    .update("completed", isCompleted)
                    .addOnSuccessListener(aVoid -> {
                        if (getView() != null) {
                            TextView completedStatus = getView().findViewById(R.id.completedStatusTextView);
                            completedStatus.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
                            if (isCompleted) {
                                completedStatus.setText("✓ Completed");
                                completedStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            } else {
                                completedStatus.setText("");
                            }
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Lỗi khi tải công việc", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tasksAdapter.setOnTaskCompletionListener(this::updateTaskCompletion);
    }

    private void showEditTaskDialog(GoalTask task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit task");
        final EditText input = new EditText(requireContext());
        input.setText(task.getTitle());
        input.setHint("Task name");
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String taskTitle = input.getText().toString().trim();
            if (!TextUtils.isEmpty(taskTitle)) {
                updateTaskTitle(task, taskTitle);
            } else {
                Toast.makeText(requireContext(), "Please enter a task name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateTaskTitle(GoalTask task, String newTitle) {
        if (goalId == null || task.getId() == null) return;
        db.collection("studyGoals").document(goalId)
            .collection("goalTasks").document(task.getId())
            .update("title", newTitle)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(requireContext(), "Task updated!", Toast.LENGTH_SHORT).show();
                loadGoalTasksAndUpdateUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showDeleteTaskConfirmation(GoalTask task) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteTask(task);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteTask(GoalTask task) {
        if (goalId == null || task.getId() == null) return;
        db.collection("studyGoals").document(goalId)
            .collection("goalTasks").document(task.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(requireContext(), "Task deleted!", Toast.LENGTH_SHORT).show();
                loadGoalTasksAndUpdateUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (goalChangedListener != null) {
            goalChangedListener.onGoalChanged();
        }
    }
} 