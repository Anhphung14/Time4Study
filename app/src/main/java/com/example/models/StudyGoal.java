package com.example.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class StudyGoal {
    private String id;
    private String title;
    private String description;
    private Timestamp startDate;
    private Timestamp endDate;
    private int targetDurationMinutes;
    private int actualDurationMinutes;
    private boolean completed;
    private String progress;
    private String uid;
    private List<GoalTask> tasks;

    public StudyGoal() {}

    public StudyGoal(String title, String description, String uid) {
        this.title = title;
        this.description = description;
        this.completed = false;
        this.uid = uid;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    public int getTargetDurationMinutes() { return targetDurationMinutes; }
    public void setTargetDurationMinutes(int targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
    }

    public int getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(int actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public List<GoalTask> getTasks() { return tasks; }
    public void setTasks(List<GoalTask> tasks) { this.tasks = tasks; }
}
