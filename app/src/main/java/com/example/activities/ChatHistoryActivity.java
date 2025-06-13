package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapters.ChatHistoryAdapter;
import com.example.models.ChatHistory;
import com.example.time4study.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryActivity extends AppCompatActivity
        implements ChatHistoryAdapter.OnHistoryClickListener,
        ChatHistoryAdapter.OnDeleteClickListener {
    private RecyclerView historyRecyclerView;
    private TextView emptyView;
    private ChatHistoryAdapter adapter;
    private FirebaseFirestore db;
    private List<ChatHistory> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Initialize RecyclerView
        historyList = new ArrayList<>();
        adapter = new ChatHistoryAdapter(historyList, this, this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(adapter);

        // Load chat history
        loadChatHistory();
    }

    private void loadChatHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("ChatHistory", "Loading chat history for user: " + userId);

        db.collection("chatHistories")
                .whereEqualTo("uid", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("ChatHistory", "Query successful. Documents count: " + queryDocumentSnapshots.size());
                    historyList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            ChatHistory history = doc.toObject(ChatHistory.class);
                            history.setId(doc.getId());
                            historyList.add(history);
                            Log.d("ChatHistory", "Added document: " + doc.getId() + " with title: " + history.getTitle());
                        } catch (Exception e) {
                            Log.e("ChatHistory", "Error converting document: " + doc.getId(), e);
                        }
                    }
                    adapter.updateData(historyList);
                    updateEmptyView();
                    Log.d("ChatHistory", "Final history list size: " + historyList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatHistory", "Error loading chat history", e);
                    Toast.makeText(this, "Error loading chat history: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload chat history when activity resumes
        loadChatHistory();
    }

    private void updateEmptyView() {
        if (historyList.isEmpty()) {
            Log.d("ChatHistory", "History list is empty, showing empty view");
            emptyView.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            Log.d("ChatHistory", "History list has items, showing recycler view");
            emptyView.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onHistoryClick(ChatHistory history) {
        Intent intent = new Intent(this, AISupportActivity.class);
        intent.putExtra("CHAT_ID", history.getId());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onDeleteClick(ChatHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteChat(history))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChat(ChatHistory history) {
        if (history.getId() != null) {
            db.collection("chatHistories")
                    .document(history.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Remove from list and update UI
                        historyList.remove(history);
                        adapter.updateData(historyList);
                        updateEmptyView();
                        Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error deleting: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
}