package com.example.models;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.Calendar;

public class Event {
    private String id;
    private String title;
    private Timestamp startTime;
    private Timestamp endTime;
    private String calendarId;
    private String uid;

    public Event() {
    }

    public Event(String id, String title, Timestamp startTime, Timestamp endTime, String calendarId, String uid) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.calendarId = calendarId;
        this.uid = uid;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
