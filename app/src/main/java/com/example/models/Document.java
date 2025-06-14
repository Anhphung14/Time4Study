package com.example.models;

import com.google.firebase.Timestamp;

public class Document {
    private String id;
    private String name;
    private String url;
    private String type; // "pdf", "image", "drive"
    private String uid;
    private Timestamp timestamp;

    public Document() {
        // Required empty constructor for Firestore
    }

    public Document(String id, String name, String url, String type, String uid, Timestamp timestamp) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
        this.uid = uid;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
} 