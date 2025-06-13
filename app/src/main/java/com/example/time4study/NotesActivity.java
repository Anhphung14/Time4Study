package com.example.time4study;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.adapters.NotesAdapter;
import com.example.time4study.models.studyNotes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<studyNotes> notes;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private Spinner subjectSpinner;
    private String selectedSubjectName = null;

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
        subjectSpinner = findViewById(R.id.subject_spinner);
        FloatingActionButton fabAddNote = findViewById(R.id.fab_add_note);

        notes = new ArrayList<>();
        adapter = new NotesAdapter(this, notes, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        loadSubjectsFromNotes();

        // nếu không có ghi chú
        TextView emptyText = findViewById(R.id.empty_text);

        if (notes.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSubjectName = position == 0 ? null : parent.getItemAtPosition(position).toString();
                loadNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, EditNoteActivity.class);
            if (selectedSubjectName != null) {
                intent.putExtra("subject_name", selectedSubjectName);
            }
            startActivity(intent);
        });
    }

    private void loadSubjectsFromNotes() {
        db.collection("studyNotes")
                .whereEqualTo("uid", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> uniqueSubjects = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        studyNotes note = doc.toObject(studyNotes.class);
                        if (note != null && note.getSubject() != null && !note.getSubject().isEmpty()) {
                            uniqueSubjects.add(note.getSubject());
                        }
                    }

                    List<String> subjectNames = new ArrayList<>();
                    subjectNames.add("All Subjects");
                    subjectNames.addAll(uniqueSubjects);

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            subjectNames
                    );
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    subjectSpinner.setAdapter(spinnerAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadNotes() {
        Query query = db.collection("studyNotes").whereEqualTo("uid", currentUserId);
        if (selectedSubjectName != null) {
            query = query.whereEqualTo("subject", selectedSubjectName);
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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading notes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
    public void onNoteClick(studyNotes note) {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }
}