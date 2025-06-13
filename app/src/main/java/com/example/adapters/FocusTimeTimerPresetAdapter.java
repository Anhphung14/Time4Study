package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.models.FocusTimeTimerPreset;

import java.util.ArrayList;
import java.util.List;

public class FocusTimeTimerPresetAdapter extends RecyclerView.Adapter<FocusTimeTimerPresetAdapter.PresetViewHolder> {
    private List<FocusTimeTimerPreset> presets;
    private OnPresetSelectedListener onPresetSelectedListener;
    private OnLongClickListener onLongClickListener;

    public interface OnPresetSelectedListener {
        void onPresetSelected(FocusTimeTimerPreset preset);
    }
    public interface OnLongClickListener {
        void onLongClick(FocusTimeTimerPreset preset);
    }

    public FocusTimeTimerPresetAdapter(List<FocusTimeTimerPreset> presets, OnPresetSelectedListener listener) {
        this.presets = presets != null ? presets : new ArrayList<>();
        this.onPresetSelectedListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        this.onLongClickListener = listener;
    }

    public void updatePresets(List<FocusTimeTimerPreset> newPresets) {
        this.presets = newPresets != null ? newPresets : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class PresetViewHolder extends RecyclerView.ViewHolder {
        TextView presetNameText, focusTimeText, shortBreakTimeText, longBreakTimeText;
        public PresetViewHolder(@NonNull View itemView) {
            super(itemView);
            presetNameText = itemView.findViewById(R.id.preset_name);
            focusTimeText = itemView.findViewById(R.id.preset_focus_time);
            shortBreakTimeText = itemView.findViewById(R.id.preset_short_break_time);
            longBreakTimeText = itemView.findViewById(R.id.preset_long_break_time);
        }
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.focustime_item_timer_preset, parent, false);
        return new PresetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        FocusTimeTimerPreset preset = presets.get(position);
        holder.presetNameText.setText(preset.name);
        holder.focusTimeText.setText("Focus: " + preset.focusMinutes + " min");
        holder.shortBreakTimeText.setText("Short Break: " + preset.shortBreakMinutes + " min");
        holder.longBreakTimeText.setText("Long Break: " + preset.longBreakMinutes + " min");
        holder.itemView.setOnClickListener(v -> {
            if (onPresetSelectedListener != null) {
                onPresetSelectedListener.onPresetSelected(preset);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (onLongClickListener != null) {
                onLongClickListener.onLongClick(preset);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }
} 