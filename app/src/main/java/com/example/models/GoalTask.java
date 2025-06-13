package com.example.models;
import java.io.Serializable;

public class GoalTask implements Serializable {
    private String id;
    private String title;
    private int plannedMinutes;
    private int actualMinutes;
    private boolean isCompleted;

    // Constructor không tham số
    public GoalTask() {}

    // Getter và Setter cho isCompleted
    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    // Các getter/setter khác ...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(int plannedMinutes) { this.plannedMinutes = plannedMinutes; }

    public int getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(int actualMinutes) { this.actualMinutes = actualMinutes; }
}
