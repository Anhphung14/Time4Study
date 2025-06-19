package com.example.time4study;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText login_email, login_password;
    private Button login_button;
    private TextView signupRedirectText, forgotPassword;
    private SignInButton googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "app_prefs";
    private static final String THEME_KEY = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load theme
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int themeMode = preferences.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Kiểm tra trạng thái đăng nhập
        if (preferences.getBoolean("isLoggedIn", false)) {
            String uid = preferences.getString("userUid", null);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("userUid", uid);
            startActivity(intent);
            finish();
            return;
        }

        init();
        setupFirebase();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void init() {
        login_button = findViewById(R.id.login_button);
        login_email = findViewById(R.id.login_email);
        login_password = findViewById(R.id.login_password);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        forgotPassword = findViewById(R.id.forgotPassword);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
    }

    private void setupFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupGoogleSignIn() {
        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Launcher cho Google Sign-In
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Google Sign In thành công: " + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google Sign In thất bại", e);
                        String errorMessage = "Đăng nhập Google thất bại: ";
                        switch (e.getStatusCode()) {
                            case 7:
                                errorMessage += "Không có kết nối internet";
                                break;
                            case 10:
                                errorMessage += "Cấu hình Google Sign-In không hợp lệ";
                                break;
                            case 12:
                                errorMessage += "Đăng nhập bị hủy";
                                break;
                            default:
                                errorMessage += e.getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        // Đăng nhập bằng email/password
        login_button.setOnClickListener(v -> loginUser());

        // Đăng nhập Google
        googleSignInButton.setOnClickListener(view -> {
            // Đăng xuất Google trước khi đăng nhập để cho phép chọn tài khoản
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                signInLauncher.launch(signInIntent);
            });
        });

        // Chuyển qua trang đăng ký
        signupRedirectText.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Quên mật khẩu
        forgotPassword.setOnClickListener(view ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Kiểm tra user đã tồn tại trong Firestore chưa
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putBoolean("isLoggedIn", true);
                                        editor.putString("userUid", user.getUid());
                                        editor.putString("email", user.getEmail());
                                        if (documentSnapshot.exists()) {
                                            // User đã tồn tại, lấy thông tin và chuyển sang MainActivity
                                            String name = documentSnapshot.getString("name");
                                            String photoUrl = documentSnapshot.getString("photoUrl");
                                            editor.putString("username", name);
                                            if (photoUrl != null) editor.putString("avatar_url", photoUrl);
                                            editor.apply();

                                            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(this, MainActivity.class);
                                            intent.putExtra("userUid", user.getUid());
                                            intent.putExtra("userName", name);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // User chưa tồn tại, tạo mới
                                            Map<String, Object> userProfile = new HashMap<>();
                                            userProfile.put("name", user.getDisplayName());
                                            userProfile.put("email", user.getEmail());
                                            userProfile.put("link_avatar", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                                            userProfile.put("lastLogin", System.currentTimeMillis());

                                            db.collection("users")
                                                    .document(user.getUid())
                                                    .set(userProfile)
                                                    .addOnSuccessListener(aVoid -> {
                                                        editor.putString("username", user.getDisplayName());
                                                        if (user.getPhotoUrl() != null) editor.putString("avatar_url", user.getPhotoUrl().toString());
                                                        editor.apply();
                                                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(this, MainActivity.class);
                                                        intent.putExtra("userUid", user.getUid());
                                                        intent.putExtra("userName", user.getDisplayName());
                                                        startActivity(intent);
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Lỗi khi lưu thông tin người dùng", e);
                                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Lỗi khi kiểm tra user Firestore", e);
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.w(TAG, "Đăng nhập thất bại", task.getException());
                        Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser() {
        String email = login_email.getText().toString().trim();
        String password = login_password.getText().toString().trim();

        if (email.isEmpty()) {
            login_email.setError("Vui lòng nhập email");
            login_email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            login_email.setError("Email không hợp lệ");
            login_email.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            login_password.setError("Vui lòng nhập mật khẩu");
            login_password.requestFocus();
            return;
        }

        if (password.length() < 6) {
            login_password.setError("Mật khẩu tối thiểu 6 ký tự");
            login_password.requestFocus();
            return;
        }

        // Hiển thị loading
        login_button.setEnabled(false);
        login_button.setText("Đang đăng nhập...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Lưu trạng thái đăng nhập
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userUid", user.getUid());
                            editor.putString("email", user.getEmail());
                            editor.apply();

                            // Lấy thông tin user từ Firestore
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String name = documentSnapshot.getString("name");
                                            editor.putString("username", name);
                                            editor.apply();

                                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            intent.putExtra("userUid", user.getUid());
                                            intent.putExtra("userName", name);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Nếu không tìm thấy thông tin trong Firestore
                                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            intent.putExtra("userUid", user.getUid());
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Nếu có lỗi khi lấy thông tin từ Firestore
                                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.putExtra("userUid", user.getUid());
                                        startActivity(intent);
                                        finish();
                                    });
                        }
                    } else {
                        // Xử lý lỗi đăng nhập
                        String errorMessage;
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            errorMessage = "Email không tồn tại";
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            errorMessage = "Mật khẩu không đúng";
                        } catch (Exception e) {
                            errorMessage = "Đăng nhập thất bại: " + e.getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                    // Reset trạng thái nút đăng nhập
                    login_button.setEnabled(true);
                    login_button.setText("Đăng nhập");
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("userUid", currentUser.getUid());
            startActivity(intent);
            finish();
        }
    }
}
