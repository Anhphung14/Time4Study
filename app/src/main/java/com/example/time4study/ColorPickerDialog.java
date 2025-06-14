package com.example.time4study;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class ColorPickerDialog extends Dialog {

    private final String currentColor;
    private final OnColorSelectedListener listener;
    private final int[] colors = {
            Color.WHITE,
            Color.parseColor("#F28B82"), // Red
            Color.parseColor("#FBBC04"), // Yellow
            Color.parseColor("#FFF475"), // Light Yellow
            Color.parseColor("#CCFF90"), // Light Green
            Color.parseColor("#A7FFEB"), // Teal
            Color.parseColor("#CBF0F8"), // Light Blue
            Color.parseColor("#AECBFA"), // Blue
            Color.parseColor("#D7AEFB"), // Purple
            Color.parseColor("#FDCFE8"), // Pink
            Color.parseColor("#E6C9A8"), // Brown
            Color.parseColor("#E8EAED")  // Gray
    };

    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public ColorPickerDialog(@NonNull Context context, String currentColor, OnColorSelectedListener listener) {
        super(context);
        this.currentColor = currentColor;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_picker);

        GridLayout colorGrid = findViewById(R.id.color_grid);
        colorGrid.setColumnCount(4);

        for (int color : colors) {
            ImageView colorView = new ImageView(getContext());
            colorView.setBackgroundColor(color);
            colorView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Add border if this is the current color
            if (currentColor != null && !currentColor.isEmpty()) {
                try {
                    int currentColorInt = Color.parseColor(currentColor);
                    if (currentColorInt == color) {
                        colorView.setBackgroundResource(R.drawable.color_selected_background);
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            // Set size and margins
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(48);
            params.height = dpToPx(48);
            params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            colorView.setLayoutParams(params);

            // Set click listener
            colorView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(String.format("#%06X", (0xFFFFFF & color)));
                }
                dismiss();
            });

            colorGrid.addView(colorView);
        }
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}