package com.example;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import com.example.time4study.NotesActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.time4study.LoginActivity;


import com.example.GridViewMenuAdapter;
import com.example.Menu;
import com.example.time4study.R;

import java.util.ArrayList;

public class FragmentMenu extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        GridView gridView = view.findViewById(R.id.GridViewMenu);
        Button buttonLogout = view.findViewById(R.id.buttonLogout); // Gắn nút Logout

        // Gán dữ liệu cho GridView
        ArrayList<Menu> listMenu = new ArrayList<>();
        listMenu.add(new Menu("My Goals", R.drawable.dart));
        listMenu.add(new Menu("Timeline", R.drawable.timeline));
        listMenu.add(new Menu("Daily Report", R.drawable.daily_report));
        listMenu.add(new Menu("Calendar", R.drawable.calendar));
        listMenu.add(new Menu("Notes", R.drawable.note));
        listMenu.add(new Menu("Study Schedule", R.drawable.schedule));
        listMenu.add(new Menu("Global Ranking", R.drawable.global_ranking));
        listMenu.add(new Menu("Friend Ranking", R.drawable.friend_ranking));
        listMenu.add(new Menu("Study Log", R.drawable.study_log));

        GridViewMenuAdapter adapter = new GridViewMenuAdapter(getActivity(), R.layout.custom_gridview, listMenu);
        gridView.setAdapter(adapter);

        // Add click listener for grid items
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            Menu selectedMenu = listMenu.get(position);
            if (selectedMenu.getTitle().equals("Notes")) {
                Intent intent = new Intent(getActivity(), NotesActivity.class);
                startActivity(intent);
            }
            // Add other menu item clicks here if needed
        });

        // Xử lý sự kiện Logout
        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish(); // Đóng MainActivity
        });

        return view;
    }
}