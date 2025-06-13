package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.models.GoalTask;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private List<GoalTask> tasks;
    private OnTaskCompletionListener taskCompletionListener;
    private OnTaskEditListener taskEditListener;
    private OnTaskDeleteListener taskDeleteListener;

    public interface OnTaskCompletionListener {
        void onTaskCompletionChanged(GoalTask task, boolean isCompleted);
    }

    public interface OnTaskEditListener {
        void onTaskEdit(GoalTask task);
    }

    public interface OnTaskDeleteListener {
        void onTaskDelete(GoalTask task);
    }

    public TasksAdapter() {
        this.tasks = new ArrayList<>();
    }

    public void setTasks(List<GoalTask> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnTaskCompletionListener(OnTaskCompletionListener listener) {
        this.taskCompletionListener = listener;
    }

    public void setOnTaskEditListener(OnTaskEditListener listener) {
        this.taskEditListener = listener;
    }

    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.taskDeleteListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        GoalTask task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private MaterialCheckBox taskCheckBox;
        private ImageButton editButton;
        private ImageButton deleteButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && taskCompletionListener != null) {
                    taskCompletionListener.onTaskCompletionChanged(tasks.get(position), isChecked);
                }
            });

            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && taskEditListener != null) {
                    taskEditListener.onTaskEdit(tasks.get(position));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && taskDeleteListener != null) {
                    taskDeleteListener.onTaskDelete(tasks.get(position));
                }
            });
        }

        void bind(GoalTask task) {
            titleTextView.setText(task.getTitle());
            taskCheckBox.setChecked(task.isCompleted());
        }
    }
} 