package com.example.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.time4study.R;
import com.example.models.ChatMessage;

import java.util.List;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<ChatMessage> messages;
    private Markwon markwon;

    public ChatAdapter(List<ChatMessage> messages, Context context) {
        this.messages = messages;
        this.markwon = Markwon.create(context);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (message.isUser()) {
            holder.messageText.setText(message.getMessage());
        } else {
            markwon.setMarkdown(holder.messageText, message.getMessage());
        }

        // Align messages based on sender
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (message.isUser()) {
            params.gravity = android.view.Gravity.END;
            holder.messageText.setBackgroundResource(R.drawable.message_background_user);
        } else {
            params.gravity = android.view.Gravity.START;
            holder.messageText.setBackgroundResource(R.drawable.message_background);
        }
        holder.messageContainer.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView messageText;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
} 