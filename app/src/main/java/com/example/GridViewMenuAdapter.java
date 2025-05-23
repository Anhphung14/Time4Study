package com.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.time4study.R;

import java.util.List;

public class GridViewMenuAdapter extends BaseAdapter {
    private Context context;
    private int layout;
    private List<Menu> listMenu;

    public GridViewMenuAdapter(Context context, int layout, List<Menu> listMenu) {
        this.context = context;
        this.layout = layout;
        this.listMenu = listMenu;
    }

    @Override
    public int getCount() {
        return listMenu.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = inflater.inflate(layout, null);

        ImageView iconItemMenu = view.findViewById(R.id.itemMenu);
        TextView titleItemMenu = view.findViewById(R.id.titleItemMenu);

        Menu menu = listMenu.get(i);

        iconItemMenu.setImageResource(menu.getIcon());
        titleItemMenu.setText(menu.getTitle());

        return view;
    }
}