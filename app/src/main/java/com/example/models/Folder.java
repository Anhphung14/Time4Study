package com.example.models;

import com.google.firebase.Timestamp;

public class Folder {
    private String id;
    private String name;
    private String uid;
    private Timestamp timestamp;

    public Folder() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
} 