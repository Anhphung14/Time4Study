package com.example.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.adapters.ChatAdapter;
import com.example.models.ChatHistory;
import com.example.models.ChatMessage;
import com.example.models.StudyGoal;
import com.example.models.GoalTask;
import com.example.utils.GeminiManager;
import com.example.utils.FirestoreDataProcessor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AISupportActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton historyButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private GeminiManager geminiManager;
    private FirebaseFirestore db;
    private String currentChatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_support);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        historyButton = findViewById(R.id.historyButton);

        // Initialize Gemini
        geminiManager = GeminiManager.getInstance();

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Get chatId from Intent if available
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CHAT_ID")) {
            currentChatId = intent.getStringExtra("CHAT_ID");
            loadChatHistoryById(currentChatId); // Reload chat history
        } else {
            // If no chatId, just display a welcome message
            addMessage("Hello! I'm your AI study assistant powered by Gemini. How can I help you today?", false);
        }

        // Setup buttons
        sendButton.setOnClickListener(v -> sendMessage());
        historyButton.setOnClickListener(v -> showChatHistory());

        // Enable/disable send button based on input
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void createNewChat() {
        try {
            // Only create a new chat if there's at least one user message
            if (!hasUserMessages()) {
                Log.d("ChatSupport", "Skipping chat creation - no user messages yet");
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d("ChatSupport", "Creating new chat for user: " + uid);

            // Create a title based on the first user message
            String title = "New Chat";
            for (ChatMessage msg : messages) {
                if (msg.isUser() && msg.getMessage() != null && !msg.getMessage().trim().isEmpty()) {
                    title = msg.getMessage();
                    break;
                }
            }

            // Ensure messages is always a List (array)
            if (messages == null) {
                messages = new ArrayList<>();
            }

            DocumentReference docRef = db.collection("chatHistories").document();
            currentChatId = docRef.getId();

            ChatHistory chatHistory = new ChatHistory(uid, messages);
            chatHistory.setId(currentChatId);
            chatHistory.setTitle(title);

            docRef.set(chatHistory)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ChatSupport", "New chat created with ID: " + currentChatId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ChatSupport", "Error creating new chat", e);
                        Toast.makeText(this, "Error creating chat history: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("ChatSupport", "Error in createNewChat", e);
            Toast.makeText(this, "Error creating new chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateChatHistory() {
        if (currentChatId != null && !messages.isEmpty()) {
            try {
                Log.d("ChatSupport", "Updating chat history for ID: " + currentChatId);
                DocumentReference docRef = db.collection("chatHistories").document(currentChatId);
                ChatMessage lastMessage = messages.get(messages.size() - 1);

                Map<String, Object> updates = new HashMap<>();
                updates.put("updatedAt", FieldValue.serverTimestamp());

                docRef.update("messages", FieldValue.arrayUnion(lastMessage), "updatedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ChatSupport", "Chat history updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ChatSupport", "Error updating chat history", e);
                            Toast.makeText(this, "Error updating chat history: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            // If the error is due to messages not existing, create a new one
                            if (e.getMessage() != null && e.getMessage().contains("No document to update")) {
                                createNewChat();
                            }
                        });
            } catch (Exception e) {
                Log.e("ChatSupport", "Error in updateChatHistory", e);
                Toast.makeText(this, "Update error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showChatHistory() {
        Intent intent = new Intent(this, ChatHistoryActivity.class);
        startActivity(intent);
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            try {
                Log.d("ChatSupport", "Sending message: " + message);
                // Add user message to local list and UI
                addMessage(message, true);
                messageInput.setText("");

                // Recognize special questions about the number of completed goals
                if (message.toLowerCase().contains("count completed goals")) {
                    askGeminiCompletedGoals();
                } else {
                    getAIResponse(message);
                }
            } catch (Exception e) {
                Log.e("ChatSupport", "Error sending message", e);
                Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addMessage(String message, boolean isUser) {
        try {
            Log.d("ChatSupport", "Adding message to list: " + message.substring(0, Math.min(50, message.length())) + "...");
            ChatMessage chatMessage = new ChatMessage(message, isUser);
            messages.add(chatMessage);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);

            // Only update chat history if it's a user message or currentChatId exists
            if (currentChatId != null || (isUser && hasUserMessages())) {
                if (currentChatId != null) {
                    updateChatHistory();
                } else {
                    createNewChat();
                }
            }
        } catch (Exception e) {
            Log.e("ChatSupport", "Error adding message", e);
            Toast.makeText(this, "Error adding message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Check if there are any messages from the user
    private boolean hasUserMessages() {
        for (ChatMessage message : messages) {
            if (message.isUser()) {
                return true;
            }
        }
        return false;
    }

    private void getAIResponse(String userMessage) {
        // Disable input while waiting for response
        sendButton.setEnabled(false);
        messageInput.setEnabled(false);

        // Create a context-aware prompt
        String prompt = "You are an AI study assistant. Please reply in Vietnamese, friendly and easy to understand. " +
                "If the question is in English, please answer in Vietnamese. " +
                "User's question: " + userMessage + "\n" +
                "Please provide helpful answers, focusing on study tips, learning strategies, or academic advice.";

        // Get response from Gemini
        geminiManager.generateResponse(prompt, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    addMessage(response, false);
                    updateChatHistory();
                    sendButton.setEnabled(true);
                    messageInput.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    addMessage("Sorry, I'm having connection issues. Please try again later.", false);
                    updateChatHistory();
                    Toast.makeText(AISupportActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
                    messageInput.setEnabled(true);
                });
            }
        });
    }

    // Function to reload chat history from Firestore
    private void loadChatHistoryById(String chatId) {
        db.collection("chatHistories")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ChatHistory chatHistory = documentSnapshot.toObject(ChatHistory.class);
                        if (chatHistory != null) {
                            chatHistory.setId(documentSnapshot.getId());
                        }
                        if (chatHistory != null && chatHistory.getMessages() != null) {
                            messages.clear();
                            messages.addAll(chatHistory.getMessages());
                            chatAdapter.notifyDataSetChanged();
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading chat history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void askGeminiAboutGoals(String userQuestion) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("studyGoals")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder summary = new StringBuilder();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        Boolean completedObj = doc.getBoolean("completed");
                        boolean completed = completedObj != null && completedObj;
                        String progress = doc.getString("progress");
                        summary.append("- ")
                                .append(title != null ? title : "(No name)")
                                .append(": ")
                                .append(completed ? "Completed" : "Not completed")
                                .append(", progress ")
                                .append(progress != null ? progress : "0%")
                                .append("\n");
                    }
                    String prompt = "Here is my list of study goals:\n" + summary.toString() +
                            "\nPlease answer the question: " + userQuestion + " (answer in Vietnamese)";
                    getAIResponse(prompt);
                })
                .addOnFailureListener(e -> {
                    addMessage("Unable to retrieve goal list: " + e.getMessage(), false);
                });
    }

    // Example: Count completed and uncompleted goals, and pass to AI
    private void askGeminiAboutStudyGoalsSummary() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("studyGoals")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completedCount = 0;
                    int uncompletedCount = 0;
                    StringBuilder summary = new StringBuilder();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        boolean completed = doc.getBoolean("completed") != null && doc.getBoolean("completed");
                        if (completed) completedCount++; else uncompletedCount++;
                        summary.append("- ").append(title != null ? title : "(No name)")
                                .append(": ")
                                .append(completed ? "Completed" : "Not completed")
                                .append("\n");
                    }
                    String prompt = "I have " + completedCount + " completed goals and " + uncompletedCount + " uncompleted goals.\n"
                            + "Goal list:\n" + summary.toString()
                            + "Please comment, encourage, or suggest ways for me to complete these goals.";
                    getAIResponse(prompt);
                })
                .addOnFailureListener(e -> {
                    addMessage("Unable to retrieve goal list: " + e.getMessage(), false);
                });
    }

    // Call this function when you want AI to answer based on the number of completed goals
    private void askGeminiCompletedGoals() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirestoreDataProcessor.getCompletedGoalsSummary(uid, new FirestoreDataProcessor.FirestoreCallback() {
            @Override
            public void onDataReady(String prompt) {
                String finalPrompt = prompt + "\nPlease respond to this in a user-friendly manner.";
                getAIResponse(finalPrompt);
            }
            @Override
            public void onError(String error) {
                addMessage("Firestore error: " + error, false);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}