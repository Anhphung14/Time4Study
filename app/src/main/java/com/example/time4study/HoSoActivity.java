package com.example.time4study;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HoSoActivity extends AppCompatActivity {
    private static final int REQUEST_EDIT_PROFILE = 1;
    private SharedPreferences userPrefs;
    private TextView tvUsername, tvEmail;
    private Button btnChangeInfor, btnLogout;
    private ImageButton btnBack;
    private ImageView imgAvatar;
    private Switch switchDarkMode;
    private LinearLayout btnChangePasswordCard;
    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        userPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int themeMode = userPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ho_so);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        imgAvatar = findViewById(R.id.imgAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        btnChangeInfor = findViewById(R.id.btnChangeInfor);
        btnLogout = findViewById(R.id.btnLogout);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnChangePasswordCard = findViewById(R.id.btnChangePasswordCard);
        btnBack = findViewById(R.id.btnBack);

        loadUserData();

        btnChangeInfor.setOnClickListener(view -> {
            Intent intent = new Intent(HoSoActivity.this, EditProfileActivity.class);
            intent.putExtra("userUid", getIntent().getStringExtra("userUid"));
            intent.putExtra("name", tvUsername.getText().toString());
            intent.putExtra("avatarUrl", getIntent().getStringExtra("avatarUrl"));
            startActivityForResult(intent, REQUEST_EDIT_PROFILE);
        });
        btnChangePasswordCard.setOnClickListener(view -> changePasswordDialog());
        switchDarkMode.setChecked(themeMode == AppCompatDelegate.MODE_NIGHT_YES);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> toggleTheme(isChecked));
        btnLogout.setOnClickListener(view -> logout());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String avatarUrl = getIntent().getStringExtra("avatarUrl");
        android.util.Log.d("HoSoActivity", "avatarUrl: " + avatarUrl);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            userPrefs.edit().putString("avatar_url", avatarUrl).apply();
        } else {
            avatarUrl = userPrefs.getString("avatar_url", null);
        }

        if (name == null) name = userPrefs.getString("username", "Chưa đặt tên");
        if (email == null) email = userPrefs.getString("email", "example@email.com");

        tvUsername.setText(name);
        tvEmail.setText(email);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.pho)
                    .error(R.drawable.pho)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("GlideError", "Lỗi tải ảnh: ", e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("GlideSuccess", "Ảnh tải thành công: ");
                            return false;
                        }
                    })
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.pho);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == RESULT_OK && data != null) {
            String newName = data.getStringExtra("newName");
            String newAvatarUrl = data.getStringExtra("avatarUrl");

            if (newName != null) {
                tvUsername.setText(newName);
                userPrefs.edit().putString("username", newName).apply();
                // Cập nhật vào Intent hiện tại
                getIntent().putExtra("name", newName);
            }

            if (newAvatarUrl != null) {
                userPrefs.edit().putString("avatar_url", newAvatarUrl).apply();
                Glide.with(this).load(newAvatarUrl).into(imgAvatar);
                // Cập nhật vào Intent hiện tại
                getIntent().putExtra("avatarUrl", newAvatarUrl);
            }

            // Gọi lại loadUserData() để chắc chắn cập nhật
            loadUserData();
        }
    }

    private void editUsernameDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập tên mới");

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa tên người dùng")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        userPrefs.edit().putString("username", newName).apply();
                        tvUsername.setText(newName);
                        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void changePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText inputOld = new EditText(this);
        inputOld.setHint("Mật khẩu cũ");
        inputOld.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputOld);

        EditText inputNew = new EditText(this);
        inputNew.setHint("Mật khẩu mới");
        inputNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNew);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Đổi", (dialog, which) -> {
                    String oldPass = inputOld.getText().toString().trim();
                    String newPass = inputNew.getText().toString().trim();

                    // Kiểm tra đầu vào
                    if (oldPass.isEmpty() || newPass.isEmpty()) {
                        Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Kiểm tra độ dài mật khẩu mới
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Xác thực và đổi mật khẩu
                    reauthenticateAndChangePassword(oldPass, newPass);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reauthenticateAndChangePassword(String oldPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Lấy email của người dùng hiện tại
            String email = user.getEmail();

            // Tạo thông tin xác thực với email và mật khẩu hiện tại
            AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);

            // Xác thực lại người dùng
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Xác thực thành công, tiến hành đổi mật khẩu
                                user.updatePassword(newPassword)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(HoSoActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(HoSoActivity.this, "Lỗi khi đổi mật khẩu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            } else {
                                // Xác thực thất bại
                                Toast.makeText(HoSoActivity.this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(HoSoActivity.this, "Không có người dùng nào đang đăng nhập hoặc tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleTheme(boolean nightModeEnabled) {
        int newMode = nightModeEnabled ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        userPrefs.edit().putInt("theme_mode", newMode).apply();
        AppCompatDelegate.setDefaultNightMode(newMode);
        recreate();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    if (mAuth.getCurrentUser() != null) {
                        mAuth.signOut();

                        // Xóa toàn bộ thông tin đăng nhập đã lưu
                        SharedPreferences.Editor editor = userPrefs.edit();
                        editor.remove("isLoggedIn");
                        editor.remove("userUid");
                        editor.remove("username");
                        editor.remove("email");
                        editor.remove("password");
                        editor.remove("avatar_url");
                        editor.apply();

                        // Quay lại LoginActivity và xóa toàn bộ backstack
                        Intent intent = new Intent(HoSoActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Không có người dùng đang đăng nhập", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

}