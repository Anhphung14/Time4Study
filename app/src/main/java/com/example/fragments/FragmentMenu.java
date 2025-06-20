package com.example.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.Menu;
import com.example.MenuAdapter;
import com.example.time4study.DocumentsActivity;
import com.example.time4study.R;
import com.example.time4study.StudySchedule.StudyScheduleActivity;
import com.example.activities.AISupportActivity;
import com.example.activities.MyGoalsActivity;
import com.example.time4study.HoSoActivity;
import com.example.time4study.NotesActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMenu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMenu extends Fragment {
    private TextView textTen, textLink;
    private ImageView imageHinh;
    private Button buttonViewProfile;
    private FirebaseFirestore db;
    private String uid;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private MenuAdapter menuAdapter;

    public FragmentMenu() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentMenu.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentMenu newInstance(String param1, String param2) {
        FragmentMenu fragment = new FragmentMenu();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public void updateName(String name) {
        View view = getView();
        if (view != null) {
            View profileLayout = view.findViewById(R.id.profileLayout);
            if (profileLayout != null) {
                // Tìm textTen trong profileLayout
                TextView textTen = profileLayout.findViewById(R.id.textTen);
                if (textTen != null) {
                    textTen.setText(name);
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (uid != null) {
            loadUserData(uid);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_menu, container, false);
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        recyclerView = view.findViewById(R.id.GridViewMenu);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        textTen = view.findViewById(R.id.textTen);
        textLink = view.findViewById(R.id.textLink);
        imageHinh = view.findViewById(R.id.imageHinh);
        buttonViewProfile = view.findViewById(R.id.buttonViewProfile);

        db = FirebaseFirestore.getInstance();

        uid = getActivity().getIntent().getStringExtra("userUid");
        if (uid != null) {
            loadUserData(uid);
        } else {
            Toast.makeText(getContext(), "Lỗi rồi", Toast.LENGTH_SHORT).show();
        }

        ArrayList<Menu> listMenu = new ArrayList<>();
        listMenu.add(new Menu("My Goals", R.drawable.dart));
        listMenu.add(new Menu("Notes", R.drawable.note));
        listMenu.add(new Menu("Study Schedule", R.drawable.schedule));
        listMenu.add(new Menu("AI Support", R.drawable.ai_assistant));
        listMenu.add(new Menu("Documents", R.drawable.folder));

        menuAdapter = new MenuAdapter(listMenu, menu -> {
            switch (menu.getTitle()) {
                case "Notes":
                    startActivity(new Intent(getActivity(), NotesActivity.class));
                    break;
                case "Study Schedule":
                    startActivity(new Intent(getActivity(), StudyScheduleActivity.class));
                    break;
                case "My Goals":
                    startActivity(new Intent(getActivity(), MyGoalsActivity.class));
                    break;
                case "AI Support":
                    startActivity(new Intent(getActivity(), AISupportActivity.class));
                    break;
                case "Documents":
                    startActivity(new Intent(getActivity(), DocumentsActivity.class));
                    break;
            }
        });

        recyclerView.setAdapter(menuAdapter);

        buttonViewProfile.setOnClickListener(v -> {
            if (uid != null) {
                loadUserDataForEdit(uid);
            } else {
                Toast.makeText(getContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadUserData(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String avatarUrl = documentSnapshot.getString("link_avatar");

                        if (name != null) textTen.setText(name);
                        if (email != null) textLink.setText(email);

                        if (avatarUrl != null) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.pho)
                                    .error(R.drawable.pho)
                                    .into(imageHinh);
                        }
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserDataForEdit(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String avatarUrl = documentSnapshot.getString("link_avatar");

                        Intent intent = new Intent(getActivity(), HoSoActivity.class);
                        intent.putExtra("name", name);
                        intent.putExtra("email", email);
                        intent.putExtra("avatarUrl", avatarUrl);
                        intent.putExtra("userUid", uid);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}