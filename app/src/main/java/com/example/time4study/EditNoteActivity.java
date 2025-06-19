package com.example.time4study;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.time4study.models.studyNotes;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditNoteActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText contentEditText;
    private EditText subjectEditText;
    private ImageButton btnAddImage;
    private ImageButton btnColor;
    private ImageButton btnPin;
    private String noteId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private String currentColor;
    private boolean isPinned;
    private List<String> imageUrls = new ArrayList<>();
    private LinearLayout imagesContainer;
    private HorizontalScrollView imagesScroll;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        noteId = getIntent().getStringExtra("note_id");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(noteId == null ? "Tạo ghi chú mới" : "Chỉnh sửa ghi chú");
        }

        titleEditText = findViewById(R.id.note_title);
        contentEditText = findViewById(R.id.note_content);
        subjectEditText = findViewById(R.id.note_subject);
        btnAddImage = findViewById(R.id.btn_add_image);
        btnColor = findViewById(R.id.btn_color);
        btnPin = findViewById(R.id.btn_pin);
        imagesContainer = findViewById(R.id.images_container);
        imagesScroll = findViewById(R.id.images_scroll);

        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnColor.setOnClickListener(v -> showColorPicker());
        btnPin.setOnClickListener(v -> togglePin());

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
                            subjectEditText.setText(note.getSubject());
                            currentColor = note.getColor();
                            isPinned = note.isPinned();
                            imageUrls = note.getImages() != null ? new ArrayList<>(note.getImages()) : new ArrayList<>();
                            updateColorButton();
                            updatePinButton();
                            updateImagesView();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    uploadImage(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                uploadImage(imageUri);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String imageId = UUID.randomUUID().toString();
        StorageReference imageRef = storage.getReference()
                .child("note_images")
                .child(currentUser.getUid())
                .child(imageId);
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            imageUrls.add(uri.toString());
                            updateImagesView();
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateImagesView() {
        imagesContainer.removeAllViews();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imagesScroll.setVisibility(HorizontalScrollView.VISIBLE);
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                View imageView = getLayoutInflater().inflate(R.layout.item_note_image, imagesContainer, false);
                ImageView noteImage = imageView.findViewById(R.id.note_image);
                ImageButton btnDelete = imageView.findViewById(R.id.btn_delete_image);

                Glide.with(this).load(url).into(noteImage);

                final int position = i;
                btnDelete.setOnClickListener(v -> {
                    imageUrls.remove(position);
                    updateImagesView();
                });

                imagesContainer.addView(imageView);
            }
        } else {
            imagesScroll.setVisibility(HorizontalScrollView.GONE);
        }
    }

    private void showColorPicker() {
        ColorPickerDialog dialog = new ColorPickerDialog(this, currentColor, color -> {
            currentColor = color;
            updateColorButton();
        });
        dialog.show();
    }

    private void updateColorButton() {
        if (currentColor != null && !currentColor.isEmpty()) {
            try {
                btnColor.setColorFilter(Color.parseColor(currentColor));
            } catch (IllegalArgumentException e) {
                btnColor.setColorFilter(Color.LTGRAY);
            }
        } else {
            btnColor.setColorFilter(Color.LTGRAY);
        }
    }

    private void togglePin() {
        isPinned = !isPinned;
        updatePinButton();
    }

    private void updatePinButton() {
        btnPin.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if (isPinned) {
            btnPin.setImageResource(R.drawable.ic_unpin);
            btnPin.setColorFilter(null);
        } else {
            btnPin.setImageResource(R.drawable.ic_pin);
            btnPin.setColorFilter(null);
        }
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String subject = subjectEditText.getText().toString().trim();
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
        note.setColor(currentColor);
        note.setPinned(isPinned);
        note.setUid(currentUser.getUid());
        note.setCreatedAt(Timestamp.now());
        note.setUpdatedAt(Timestamp.now());
        note.setImages(imageUrls);
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
                    .update(
                            "title", title,
                            "content", content,
                            "subject", subject,
                            "color", currentColor,
                            "pinned", isPinned,
                            "updatedAt", Timestamp.now(),
                            "images", imageUrls
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật ghi chú", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        } else if (item.getItemId() == R.id.action_delete && noteId != null) {
            deleteNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteNote() {
        db.collection("studyNotes")
                .document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi xóa ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
