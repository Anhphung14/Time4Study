package com.example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;

import java.util.ArrayList;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private ArrayList<Menu> menuList;
    private OnMenuClickListener listener;

    public interface OnMenuClickListener {
        void onMenuClick(Menu menu);
    }

    public MenuAdapter(ArrayList<Menu> menuList, OnMenuClickListener listener) {
        this.menuList = menuList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_gridview, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        Menu menu = menuList.get(position);
        holder.imageView.setImageResource(menu.getIcon());
        holder.textView.setText(menu.getTitle());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuClick(menu);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        MenuViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemMenu);
            textView = itemView.findViewById(R.id.titleItemMenu);
        }
    }
} 