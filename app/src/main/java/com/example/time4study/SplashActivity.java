package com.example.time4study;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        splashScreen.setKeepOnScreenCondition(() -> true);

        GifImageView gifImageView = findViewById(R.id.gifImageView);

//        new Handler().postDelayed(() -> {
//            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//            finish();
//        }, SPLASH_DELAY);

//        ImageView gifView = findViewById(R.id.splash_gif);

//        Glide.with(this)
//                .load(R.drawable.gaumeo)// Hình dự phòng nếu GIF lỗi
//                .into(gifView);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, 2000);
    }
}