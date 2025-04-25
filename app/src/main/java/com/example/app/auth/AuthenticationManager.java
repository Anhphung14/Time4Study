package com.example.app.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthenticationManager {
    private FirebaseAuth mAuth;

    public AuthenticationManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Đăng ký người dùng
    public void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Đăng ký thành công
                    FirebaseUser user = mAuth.getCurrentUser();
                } else {
                    // Xử lý lỗi
                }
            });
    }

    // Đăng nhập người dùng
    public void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Đăng nhập thành công
                    FirebaseUser user = mAuth.getCurrentUser();
                } else {
                    // Xử lý lỗi
                }
            });
    }
}
