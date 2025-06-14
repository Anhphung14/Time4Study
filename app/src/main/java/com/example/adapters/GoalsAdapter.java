package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;
import android.util.Log; // Import Log for debugging

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.models.GoalTask;
import com.example.models.StudyGoal;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {

    private static final String TAG = "GoalsAdapter"; // For logging

    private List<StudyGoal> goals;
    private OnGoalCompletionListener goalCompletionListener;
    private OnTaskCompletionListener taskCompletionListener;
    private OnGoalClickListener goalClickListener;
    private OnGoalEditListener goalEditListener;
    private OnGoalDeleteListener goalDeleteListener;
    private SimpleDateFormat dateFormat;
    private boolean isDetailView;

    public interface OnGoalCompletionListener {
        void onGoalCompletionChanged(StudyGoal goal, boolean isCompleted);
    }

    public interface OnTaskCompletionListener {
        void onTaskCompletionChanged(StudyGoal goal, GoalTask task, boolean isCompleted);
    }

    public interface OnGoalClickListener {
        void onGoalClick(StudyGoal goal);
    }

    public interface OnGoalEditListener {
        void onGoalEdit(StudyGoal goal);
    }

    public interface OnGoalDeleteListener {
        void onGoalDelete(StudyGoal goal);
    }

    public GoalsAdapter(List<StudyGoal> goals, OnGoalCompletionListener goalListener, 
                       OnTaskCompletionListener taskListener, OnGoalClickListener clickListener,
                       boolean isDetailView) {
        this.goals = goals;
        this.goalCompletionListener = goalListener;
        this.taskCompletionListener = taskListener;
        this.goalClickListener = clickListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.isDetailView = isDetailView;
    }

    public void setOnGoalEditListener(OnGoalEditListener listener) {
        this.goalEditListener = listener;
    }

    public void setOnGoalDeleteListener(OnGoalDeleteListener listener) {
        this.goalDeleteListener = listener;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        StudyGoal goal = goals.get(position);
        holder.bind(goal);
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    class GoalViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView descriptionTextView;
        private TextView timeTextView;
        private TextView progressTextView;
        private MaterialCheckBox goalCheckBox;
        private LinearProgressIndicator progressBar;
        private RecyclerView tasksRecyclerView;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private TextView completedStatusTextView;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            progressTextView = itemView.findViewById(R.id.progressTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            tasksRecyclerView = itemView.findViewById(R.id.tasksRecyclerView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            completedStatusTextView = itemView.findViewById(R.id.completedStatusTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && goalClickListener != null) {
                    goalClickListener.onGoalClick(goals.get(position));
                }
            });

            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && goalEditListener != null) {
                    goalEditListener.onGoalEdit(goals.get(position));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && goalDeleteListener != null) {
                    goalDeleteListener.onGoalDelete(goals.get(position));
                }
            });
        }

        void bind(StudyGoal goal) {
            titleTextView.setText(goal.getTitle() != null ? goal.getTitle() : "");
            descriptionTextView.setText(goal.getDescription() != null ? goal.getDescription() : "");
            if (goalCheckBox != null) {
                goalCheckBox.setVisibility(View.GONE);
            }

            List<GoalTask> tasks = goal.getTasks();
            int progress = 0;
            
            if (tasks != null && !tasks.isEmpty()) {
                int completedTasks = 0;
                for (GoalTask task : tasks) {
                    if (task != null && task.isCompleted()) {
                        completedTasks++;
                    }
                }
                progress = (completedTasks * 100) / tasks.size();
                progressBar.setProgress(progress);
                progressTextView.setText(progress + "%");
            } else {
                progressBar.setProgress(0);
                progressTextView.setText("0%");
            }

            // Chỉ hiển thị trạng thái hoàn thành khi có tasks và progress = 100%
            if (tasks != null && !tasks.isEmpty() && progress == 100) {
                completedStatusTextView.setText("✓ Complete");
                completedStatusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                completedStatusTextView.setVisibility(View.VISIBLE);
            } else {
                completedStatusTextView.setVisibility(View.GONE);
            }

            // Hiển thị ngày bắt đầu và kết thúc
            TextView startDateTextView = itemView.findViewById(R.id.startDateTextView);
            TextView endDateTextView = itemView.findViewById(R.id.endDateTextView);

            if (goal.getStartDate() != null) {
                startDateTextView.setText(dateFormat.format(goal.getStartDate().toDate()));
            } else {
                startDateTextView.setText("Chưa có ngày bắt đầu");
            }

            if (goal.getEndDate() != null) {
                endDateTextView.setText(dateFormat.format(goal.getEndDate().toDate()));
            } else {
                endDateTextView.setText("Chưa có ngày kết thúc");
            }

            if (tasksRecyclerView != null) tasksRecyclerView.setVisibility(View.GONE);
        }
    }
}