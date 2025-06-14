package com.example.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class ChatMessage {
    private String message;
    private boolean user;  // Trùng tên với Firestore field
    @ServerTimestamp
    private Timestamp timestamp;

    public ChatMessage() {}

    public ChatMessage(String message, boolean user) {
        this.message = message;
        this.user = user;
        this.timestamp = Timestamp.now();
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUser() {
        return user;
    }
    public void setUser(boolean user) {
        this.user = user;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
