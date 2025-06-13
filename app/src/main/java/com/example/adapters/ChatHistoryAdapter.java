package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.models.ChatHistory;
import com.example.time4study.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.HistoryViewHolder> {
    private List<ChatHistory> historyList;
    private OnHistoryClickListener listener;
    private OnDeleteClickListener deleteListener;
    private SimpleDateFormat dateFormat;

    public interface OnHistoryClickListener {
        void onHistoryClick(ChatHistory history);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ChatHistory history);
    }

    public ChatHistoryAdapter(List<ChatHistory> historyList, OnHistoryClickListener listener, OnDeleteClickListener deleteListener) {
        this.historyList = historyList;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ChatHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(List<ChatHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView dateTextView;
        private TextView previewTextView;
        private ImageButton deleteButton;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            previewTextView = itemView.findViewById(R.id.previewTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryClick(historyList.get(position));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    deleteListener.onDeleteClick(historyList.get(position));
                }
            });
        }

        void bind(ChatHistory history) {
            titleTextView.setText(history.getTitle());
            if (history.getUpdatedAt() != null) {
                dateTextView.setText(dateFormat.format(history.getUpdatedAt().toDate()));
            }

            // Get preview from first user message
            String preview = "";
            if (history.getMessages() != null && !history.getMessages().isEmpty()) {
                for (com.example.models.ChatMessage message : history.getMessages()) {
                    if (message.isUser()) {
                        preview = message.getMessage();
                        break;
                    }
                }
            }
            previewTextView.setText(preview);
        }
    }
}