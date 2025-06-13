package com.example.time4study.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.time4study.R;
import com.example.time4study.models.studyNotes;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private Context context;
    private List<studyNotes> notes;
    private OnNoteClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnNoteClickListener {
        void onNoteClick(studyNotes note);
    }

    public NotesAdapter(Context context, List<studyNotes> notes, OnNoteClickListener listener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notes, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        studyNotes note = notes.get(position);

        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());

        Timestamp timestamp = note.getCreatedAt();
        if (timestamp != null) {
            holder.date.setText(dateFormat.format(timestamp.toDate()));
        }

        // Set background color
        if (note.getColor() != null && !note.getColor().isEmpty()) {
            try {
                holder.background.setBackgroundColor(Color.parseColor(note.getColor()));
            } catch (IllegalArgumentException e) {
                holder.background.setBackgroundColor(Color.WHITE); // fallback if color is invalid
            }
        } else {
            holder.background.setBackgroundColor(Color.WHITE);
        }

        // Load first image from images list (if exists)
        List<String> images = note.getImages();
        if (images != null && !images.isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(images.get(0))
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
        }

        // Show pin indicator if note is pinned
        holder.pinIndicator.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<studyNotes> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        LinearLayout background;
        TextView title;
        TextView content;
        ImageView image;
        TextView date;
        ImageView pinIndicator;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            background = itemView.findViewById(R.id.note_background);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            image = itemView.findViewById(R.id.note_image);
            date = itemView.findViewById(R.id.note_date);
            pinIndicator = itemView.findViewById(R.id.pin_indicator);
        }
    }
}
