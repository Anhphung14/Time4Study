package com.example.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.List;
import java.util.Map;

public class ChatHistory {
    @DocumentId
    private String id;
    private String uid;  // nhất quán tên trường
    private List<ChatMessage> messages;
    @ServerTimestamp
    private Timestamp createdAt;
    @ServerTimestamp
    private Timestamp updatedAt;
    private String title;

    public ChatHistory() {}

    public ChatHistory(String uid, List<ChatMessage> messages) {
        this.uid = uid;
        this.messages = messages;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // getter & setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public List<ChatMessage> getMessages() { return messages;  }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
