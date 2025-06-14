package com.example.time4study;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.adapters.NotesAdapter;
import com.example.time4study.models.studyNotes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener, NotesAdapter.OnNoteActionListener {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<studyNotes> notes;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private TabLayout tabLayout;
    private boolean showPinnedOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.notes_recycler_view);
        tabLayout = findViewById(R.id.tab_layout);
        FloatingActionButton fabAddNote = findViewById(R.id.fab_add_note);

        notes = new ArrayList<>();
        adapter = new NotesAdapter(this, notes, this);
        adapter.setActionListener(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // nếu không có ghi chú
        TextView emptyText = findViewById(R.id.empty_text);

        if (notes.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPinnedOnly = tab.getPosition() == 1;
                loadNotes();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, EditNoteActivity.class);
            startActivity(intent);
        });
    }

    private void loadNotes() {
        Query query = db.collection("studyNotes")
                .whereEqualTo("uid", currentUserId);

        if (showPinnedOnly) {
            query = query.whereEqualTo("pinned", true);
        }

        query.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notes.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        studyNotes note = doc.toObject(studyNotes.class);
                        if (note != null) {
                            note.setId(doc.getId());
                            notes.add(note);
                        }
                    }
                    adapter.updateNotes(notes);
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    notes.clear();
                    adapter.updateNotes(notes);
                    updateEmptyView();
                });
    }

    private void updateEmptyView() {
        TextView emptyText = findViewById(R.id.empty_text);
        if (notes.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(showPinnedOnly ? "Chưa có ghi chú nào được ghim" : "Chưa có ghi chú nào");
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            loadNotes();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    @Override
    public void onNoteClick(studyNotes note) {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        startActivityForResult(intent, 1);
    }

    @Override
    public void onPinClick(studyNotes note) {
        db.collection("studyNotes")
                .document(note.getId())
                .update("pinned", !note.isPinned())
                .addOnSuccessListener(aVoid -> {
                    note.setPinned(!note.isPinned());
                    adapter.updateNotes(notes);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating note: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onColorClick(studyNotes note) {
        // Show color picker dialog
        ColorPickerDialog dialog = new ColorPickerDialog(this, note.getColor(), color -> {
            db.collection("studyNotes")
                    .document(note.getId())
                    .update("color", color)
                    .addOnSuccessListener(aVoid -> {
                        note.setColor(color);
                        adapter.updateNotes(notes);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error updating note color: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
        dialog.show();
    }
}