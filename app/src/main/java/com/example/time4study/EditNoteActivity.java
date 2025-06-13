package com.example.time4study;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.time4study.models.studyNotes;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditNoteActivity extends AppCompatActivity {

    private Spinner subjectSpinner;
    private EditText titleEditText;
    private EditText contentEditText;
    private List<String> subjectList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String noteId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Firebase init
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get noteId from intent
        noteId = getIntent().getStringExtra("note_id");

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(noteId == null ? "Tạo ghi chú mới" : "Chỉnh sửa ghi chú");
            Log.d("EditNoteActivity", "Toolbar set up");
        }

        // Views
        subjectSpinner = findViewById(R.id.subject_spinner);
        titleEditText = findViewById(R.id.note_title);
        contentEditText = findViewById(R.id.note_content);

        // Spinner adapter setup
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(spinnerAdapter);

        loadSubjectsFromFirestore();

        if (noteId != null) {
            loadNoteData();
        }
    }

    private void loadNoteData() {
        db.collection("studyNotes").document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        studyNotes note = documentSnapshot.toObject(studyNotes.class);
                        if (note != null) {
                            titleEditText.setText(note.getTitle());
                            contentEditText.setText(note.getContent());
                            int subjectIndex = subjectList.indexOf(note.getSubject());
                            if (subjectIndex != -1) {
                                subjectSpinner.setSelection(subjectIndex);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSubjectsFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("studyNotes")
                .whereEqualTo("uid", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> subjectSet = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String subject = doc.getString("subject");
                        if (subject != null && !subject.isEmpty()) {
                            subjectSet.add(subject);
                        }
                    }

                    subjectList.clear();
                    subjectList.addAll(subjectSet);
                    spinnerAdapter.notifyDataSetChanged();

                    if (noteId == null) {
                        String passedSubject = getIntent().getStringExtra("subject_name");
                        if (passedSubject != null) {
                            int index = subjectList.indexOf(passedSubject);
                            if (index != -1) {
                                subjectSpinner.setSelection(index);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải môn học: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String subject = subjectSpinner.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        studyNotes note = new studyNotes();
        note.setTitle(title);
        note.setContent(content);
        note.setSubject(subject);
        note.setUid(currentUser.getUid());
        note.setCreatedAt(Timestamp.now());

        if (noteId == null) {
            db.collection("studyNotes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi lưu ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            db.collection("studyNotes")
                    .document(noteId)
                    .set(note)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật ghi chú", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    // Inflate menu có nút lưu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_note, menu);
        Log.d("EditNoteActivity", "Menu inflated");
        return true;
    }

    // Xử lý sự kiện menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("EditNoteActivity", "onOptionsItemSelected called with id: " + item.getItemId());

        if (item.getItemId() == android.R.id.home) {
            Log.d("EditNoteActivity", "Home button clicked");
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            Log.d("EditNoteActivity", "Save button clicked");
            saveNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish(); // Không lưu tự động nữa, tránh lỗi dữ liệu
    }
}
