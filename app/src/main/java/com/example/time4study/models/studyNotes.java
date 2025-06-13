package com.example.time4study.models;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;

public class studyNotes {
    private String id;
    private String title;
    private String content;
    private String url;
    private String subject;
    private String color;
    private boolean isPinned;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String uid;
    private List<String> images;

    public studyNotes() {
        // Empty constructor needed for Firestore
    }

    public studyNotes(String id, String title, String content, String url, String subject, String color, boolean isPinned, Timestamp createdAt, Timestamp updatedAt, String uid, List<String> images) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.url = url;
        this.subject = subject;
        this.color = color;
        this.isPinned = isPinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.uid = uid;
        this.images = images;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}