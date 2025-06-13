package com.example.time4study;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.example.SharedViewModel;
import com.example.fragments.FragmentMenu;
import com.example.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;
    TextView textView_nameUser;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

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

        init();

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomnavigation);
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        bottomNavigationView.setItemIconTintList(null);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

//        String uid = getIntent().getStringExtra("userUid");
        if (currentUser != null) {
            loadUserName(currentUser);
        } else {
            Toast.makeText(MainActivity.this, "Loi roi", Toast.LENGTH_SHORT).show();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.focus_mode:
                        viewPager.setCurrentItem(1);
                        break;
                }

                return true;
            }
        });
    }

    public void init() {
        bottomNavigationView = findViewById(R.id.bottomnavigation);
    }


    private void loadUserName(FirebaseUser currentUser) {
        SharedViewModel viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String avatar_link = documentSnapshot.getString("link_avatar");

                        viewModel.setUserName(name);
                        viewModel.setUserEmail(email);
                        viewModel.setAvatarLink(avatar_link);

                        adapter.setShowMenuFragment();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                        adapter.setShowMenuFragment();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.setShowMenuFragment();
                });
    }
}
