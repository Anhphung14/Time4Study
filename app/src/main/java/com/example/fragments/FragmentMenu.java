package com.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.GridViewMenuAdapter;
import com.example.Menu;
import com.example.time4study.R;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMenu#newInstance} factory method to
 * create an instance of this fragment.
 */
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_menu, container, false);
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        GridView gridView = view.findViewById(R.id.GridViewMenu);

        ArrayList<Menu> listMenu = new ArrayList<>();
        listMenu.add(new Menu("My Goals", R.drawable.dart));
        listMenu.add(new Menu("Timeline", R.drawable.timeline));
        listMenu.add(new Menu("Daily Report", R.drawable.daily_report));
        listMenu.add(new Menu("Calendar", R.drawable.calendar));
        listMenu.add(new Menu("D-Day", R.drawable.d_day));
        listMenu.add(new Menu("Study Schedule", R.drawable.schedule));
        listMenu.add(new Menu("Global Ranking", R.drawable.global_ranking));
        listMenu.add(new Menu("Friend Ranking", R.drawable.friend_ranking));
        listMenu.add(new Menu("Study Log", R.drawable.study_log));


        GridViewMenuAdapter adapter = new GridViewMenuAdapter(getActivity(), R.layout.custom_gridview, listMenu);
        gridView.setAdapter(adapter);

        return view;

    }
}