package com.example.models;

public class CalendarModel {
    private String id;
    private String title;
    private String colorCode;
    private boolean isChecked;
    private String uid;

    public CalendarModel() {
    }
    public CalendarModel(String id, String title, String colorCode, String uid) {
        this.id = id;
        this.title = title;
        this.colorCode = colorCode;
        this.uid = uid;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getColorCode() {
        return colorCode;
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

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
