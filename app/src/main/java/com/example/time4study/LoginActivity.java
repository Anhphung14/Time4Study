package com.example.time4study;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText login_email, login_password;
    private Button login_button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        mAuth = FirebaseAuth.getInstance();

        login_button.setOnClickListener(view -> {
            String email = login_email.getText().toString();
            String password = login_password.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                signInUser(email, password);
            }
        });
    }

    public void init () {
        login_button = findViewById(R.id.login_button);
        login_email = findViewById(R.id.login_email);
        login_password = findViewById(R.id.login_password);
    }

    public void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user.getUid();
                        Log.d("Login", uid);
                        if (user != null) {
                            Log.d("Login", "Login thanh cong");
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}