package com.example.models;

import com.google.firebase.Timestamp;

public class Task {
    private String id;
    private String title;
    private Timestamp date;
    private boolean status;
    private String uid;

    public Task() {

    }
    public Task(String id, String title, Timestamp date, boolean status, String uid) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.status = status;
        this.uid = uid;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getDate() {
        return date;
    }

    public boolean isStatus() {
        return status;
    }

    public String getUid() {
        return uid;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
