package com.example.time4study;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {
    private SharedPreferences userPrefs;
    private EditText edtName;
    private ImageView imgProfile;
    private ImageButton btnBack, btnEditImage;
    private Button btnSave, btnCancel;
    private String userUid, name, avatarUrl;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        userPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int themeMode = userPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEditLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        edtName = findViewById(R.id.edtName);
        imgProfile = findViewById(R.id.imgProfile);
        btnBack = findViewById(R.id.btnBack);
        btnEditImage = findViewById(R.id.btnEditImage);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        Intent intent = getIntent();
        userUid = intent.getStringExtra("userUid");
        name = intent.getStringExtra("name");
        avatarUrl = intent.getStringExtra("avatarUrl");

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadImageToCloudinary(imageUri);
                    }
                }
        );

        loadData();

        btnBack.setOnClickListener(v -> finish());
        btnEditImage.setOnClickListener(v -> pickImageFromGallery());
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadData() {
        if (name != null) {
            edtName.setText(name);
        } else {
            edtName.setText(userPrefs.getString("username", "Chưa đặt tên"));
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.pho)
                    .error(R.drawable.pho)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.pho);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri != null && userUid != null && mAuth.getCurrentUser() != null) {
            String imageName = "avatar_" + userUid + "_" + UUID.randomUUID().toString();

            MediaManager.get().upload(imageUri)
                    .option("public_id", imageName)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Toast.makeText(EditProfileActivity.this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String newAvatarUrl = resultData.get("secure_url").toString();
                            avatarUrl = newAvatarUrl;
                            Glide.with(EditProfileActivity.this).load(avatarUrl).into(imgProfile);
                            Toast.makeText(EditProfileActivity.this, "Ảnh đã được chọn", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditProfileActivity.this, "Lỗi upload ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập hoặc kiểm tra lại dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAvatarInFirestore(String newAvatarUrl) {
        if (userUid != null) {
            db.collection("users").document(userUid)
                    .update("link_avatar", newAvatarUrl)
                    .addOnSuccessListener(aVoid -> {
                        avatarUrl = newAvatarUrl;
                        Glide.with(this).load(avatarUrl).into(imgProfile);
                        Toast.makeText(this, "Ảnh đã được cập nhật", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi cập nhật ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveChanges() {
        String newName = edtName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userUid != null && mAuth.getCurrentUser() != null) {
            db.collection("users").document(userUid)
                    .update("name", newName, "link_avatar", avatarUrl)
                    .addOnSuccessListener(aVoid -> {
                        userPrefs.edit().putString("username", newName).apply();
                        Intent intent = new Intent();
                        intent.putExtra("newName", newName);
                        intent.putExtra("avatarUrl", avatarUrl);
                        setResult(RESULT_OK, intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập hoặc kiểm tra lại dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }
}
