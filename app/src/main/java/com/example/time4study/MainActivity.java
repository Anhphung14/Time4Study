package com.example.time4study;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.FragmentMenu;
import com.example.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;
    TextView textView_nameUser;

    private FirebaseFirestore db;

    BottomNavigationView bottomNavigationView;
    TextView textTen;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomnavigation);
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        bottomNavigationView.setItemIconTintList(null);

        db = FirebaseFirestore.getInstance();

        String uid = getIntent().getStringExtra("userUid");
        if (uid != null) {
            loadUserName(uid);
        } else {
            Toast.makeText(MainActivity.this, "Loi roi", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadUserName(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        Toast.makeText(MainActivity.this, "Welcome back " + name, Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Toast.makeText(MainActivity.this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
