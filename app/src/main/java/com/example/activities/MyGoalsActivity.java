package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.adapters.GoalsAdapter;
import com.example.models.GoalTask;
import com.example.models.StudyGoal;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyGoalsActivity extends AppCompatActivity implements GoalsAdapter.OnGoalCompletionListener, GoalsAdapter.OnTaskCompletionListener, GoalsAdapter.OnGoalClickListener, GoalsAdapter.OnGoalEditListener, GoalsAdapter.OnGoalDeleteListener {

    private static final String TAG = "MyGoalsActivity";

    private RecyclerView goalsRecyclerView;
    private GoalsAdapter goalsAdapter;
    private List<StudyGoal> goalsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration goalsListenerRegistration; // To manage listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_goals);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            db = FirebaseFirestore.getInstance();
        } else {
            Toast.makeText(this, "You need to log in to view goals.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        goalsRecyclerView = findViewById(R.id.goalsRecyclerView);
        goalsList = new ArrayList<>();
        // Pass 'false' for 'showTasks' initially, as tasks are loaded later
        goalsAdapter = new GoalsAdapter(goalsList, this, this, this, false);
        goalsAdapter.setOnGoalEditListener(this);
        goalsAdapter.setOnGoalDeleteListener(this);
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalsRecyclerView.setAdapter(goalsAdapter);

        FloatingActionButton fabAddGoal = findViewById(R.id.fabAddGoal);
        fabAddGoal.setOnClickListener(v -> {
            Intent intent = new Intent(MyGoalsActivity.this, AddGoalActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach Firestore listener to prevent memory leaks
        if (goalsListenerRegistration != null) {
            goalsListenerRegistration.remove();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Handle back button press
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads study goals for the current user from Firestore.
     * It also fetches associated tasks for each goal and calculates progress.
     */
    private void loadGoals() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // Exit if no user is logged in

        db.collection("studyGoals")
                .whereEqualTo("uid", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    goalsList.clear(); // Clear existing list before loading new data

                    if (queryDocumentSnapshots.isEmpty()) {
                        goalsAdapter.notifyDataSetChanged(); // Update UI if no goals found
                        return;
                    }

                    int totalGoals = queryDocumentSnapshots.size();
                    final int[] loadedCount = {0}; // Counter for loaded goals and their tasks

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        StudyGoal goal = document.toObject(StudyGoal.class);
                        if (goal == null) {
                            loadedCount[0]++; // Increment count even if goal is null
                            continue;
                        }

                        goal.setId(document.getId());

                        // Check if goal has a startDate
                        if (goal.getStartDate() == null) {
                            // If no startDate, add it to the end of the list
                            goalsList.add(goal);
                            loadedCount[0]++;
                            if (loadedCount[0] == totalGoals) {
                                sortAndUpdateGoals(); // Sort and update adapter once all goals are loaded
                            }
                            continue;
                        }

                        // Fetch tasks for the current goal
                        db.collection("studyGoals")
                                .document(goal.getId())
                                .collection("goalTasks")
                                .get()
                                .addOnSuccessListener(taskSnapshots -> {
                                    List<GoalTask> tasks = new ArrayList<>();
                                    int completedCount = 0;

                                    for (QueryDocumentSnapshot taskDoc : taskSnapshots) {
                                        GoalTask task = taskDoc.toObject(GoalTask.class);
                                        if (task == null) continue;

                                        task.setId(taskDoc.getId());
                                        Boolean isCompleted = taskDoc.getBoolean("isCompleted");
                                        task.setCompleted(isCompleted != null && isCompleted);
                                        tasks.add(task);

                                        if (task.isCompleted()) completedCount++;
                                    }

                                    goal.setTasks(tasks); // Set tasks for the goal

                                    int percent = (tasks.size() > 0) ? (completedCount * 100 / tasks.size()) : 0;
                                    goal.setProgress(percent + "%");
                                    goal.setCompleted(completedCount == tasks.size()); // Mark goal as completed if all tasks are

                                    goalsList.add(goal); // Add goal to the list

                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalGoals) {
                                        sortAndUpdateGoals(); // Sort and update adapter once all goals and their tasks are loaded
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalGoals) {
                                        sortAndUpdateGoals();
                                    }
                                    Log.e(TAG, "Error loading tasks for goal " + goal.getId() + ": " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading goals: " + e.getMessage());
                    Toast.makeText(this, "Error loading goals: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sorts the goals list by start date and updates the RecyclerView adapter.
     */
    private void sortAndUpdateGoals() {
        // Sort the list by startDate
        Collections.sort(goalsList, (g1, g2) -> {
            if (g1.getStartDate() == null && g2.getStartDate() == null) return 0;
            if (g1.getStartDate() == null) return 1; // Null dates go to the end
            if (g2.getStartDate() == null) return -1; // Null dates go to the end
            return g1.getStartDate().compareTo(g2.getStartDate());
        });
        goalsAdapter.notifyDataSetChanged(); // Notify adapter of data change
    }

    @Override
    public void onGoalCompletionChanged(StudyGoal goal, boolean isCompleted) {
        if (goal.getId() != null) {
            // If goal is marked as completed, update all its tasks to completed
            if (isCompleted && goal.getTasks() != null) {
                for (GoalTask task : goal.getTasks()) {
                    if (task.getId() != null) {
                        db.collection("studyGoals").document(goal.getId())
                                .collection("goalTasks").document(task.getId())
                                .update("isCompleted", true)
                                .addOnSuccessListener(taskVoid -> {
                                    task.setCompleted(true); // Update local task object
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating task completion: " + e.getMessage());
                                });
                    }
                }
                // Update progress to 100% when goal is completed
                goal.setProgress("100%");
            }
            // Update the goal's completion status in Firestore
            db.collection("studyGoals").document(goal.getId())
                    .update("completed", isCompleted)
                    .addOnSuccessListener(aVoid -> {
                        goalsAdapter.notifyDataSetChanged();
                        Toast.makeText(this,
                                isCompleted ? "Goal completed!" : "Goal marked as incomplete",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating goal completion: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onTaskCompletionChanged(StudyGoal goal, GoalTask task, boolean isCompleted) {
        if (goal.getId() != null && task.getId() != null) {
            db.collection("studyGoals").document(goal.getId())
                    .collection("goalTasks").document(task.getId())
                    .update("isCompleted", isCompleted)
                    .addOnSuccessListener(aVoid -> {
                        // Update task status in the goal's local task list
                        if (goal.getTasks() != null) {
                            for (GoalTask t : goal.getTasks()) {
                                if (t.getId().equals(task.getId())) {
                                    t.setCompleted(isCompleted);
                                    break;
                                }
                            }
                            // Recalculate progress based on completed tasks
                            int total = goal.getTasks().size();
                            int completed = 0;
                            for (GoalTask t : goal.getTasks()) {
                                if (t.isCompleted()) completed++;
                            }
                            int percent = (total > 0) ? (completed * 100 / total) : 0;
                            goal.setProgress(percent + "%");
                            // Also update the goal's completed status if all tasks are done
                            goal.setCompleted(completed == total);
                            // Update the goal's 'completed' field in Firestore
                            db.collection("studyGoals").document(goal.getId())
                                    .update("completed", goal.isCompleted());
                        }
                        goalsAdapter.notifyDataSetChanged(); // Update UI
                        Toast.makeText(this,
                                isCompleted ? "Task completed!" : "Task marked as incomplete",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating task: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onGoalClick(StudyGoal goal) {
        // Open the GoalDetailBottomSheet when a goal is clicked
        GoalDetailBottomSheet bottomSheet = GoalDetailBottomSheet.newInstance(goal);
        bottomSheet.setOnGoalChangedListener(this::loadGoals); // Set a listener to reload goals if changes occur in bottom sheet
        bottomSheet.show(getSupportFragmentManager(), "GoalDetailBottomSheet");
    }

    @Override
    public void onGoalEdit(StudyGoal goal) {
        // Start AddGoalActivity in edit mode with existing goal data
        Intent intent = new Intent(this, AddGoalActivity.class);
        intent.putExtra("GOAL_ID", goal.getId());
        intent.putExtra("GOAL_TITLE", goal.getTitle());
        intent.putExtra("GOAL_DESCRIPTION", goal.getDescription());
        if (goal.getStartDate() != null) {
            intent.putExtra("GOAL_START_DATE", goal.getStartDate().toDate().getTime());
        }
        if (goal.getEndDate() != null) {
            intent.putExtra("GOAL_END_DATE", goal.getEndDate().toDate().getTime());
        }
        intent.putExtra("GOAL_TARGET_DURATION", goal.getTargetDurationMinutes());
        startActivity(intent);
    }

    @Override
    public void onGoalDelete(StudyGoal goal) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete this goal? All associated tasks will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (goal.getId() != null) {
                        // First delete all tasks within the goal's subcollection
                        db.collection("studyGoals").document(goal.getId())
                                .collection("goalTasks")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        document.getReference().delete(); // Delete each task document
                                    }
                                    // Then delete the goal itself
                                    db.collection("studyGoals").document(goal.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(MyGoalsActivity.this,
                                                        "Goal deleted successfully", Toast.LENGTH_SHORT).show();
                                                loadGoals(); // Reload the goals list to reflect changes
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(MyGoalsActivity.this,
                                                        "Error deleting goal: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MyGoalsActivity.this,
                                            "Error deleting tasks: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload goals when returning to this activity to ensure updated data
        loadGoals();
    }
}
