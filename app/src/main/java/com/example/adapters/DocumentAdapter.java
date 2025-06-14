package com.example.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.models.Document;
import com.example.time4study.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {
    private List<Document> documents;
    private Context context;
    private OnDocumentActionListener listener;
    private boolean isGrid = false;

    public interface OnDocumentActionListener {
        void onDeleteDocument(Document document);
        void onRenameDocument(Document document);
    }

    public DocumentAdapter(Context context, List<Document> documents, OnDocumentActionListener listener) {
        this.context = context;
        this.documents = documents;
        this.listener = listener;
    }

    public void setGridMode(boolean isGrid) {
        this.isGrid = isGrid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isGrid) {
            view = LayoutInflater.from(context).inflate(R.layout.item_document_grid, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        }
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);
        String fileType = document.getType().toLowerCase();
        String fileName = document.getName();
        holder.textViewDocumentName.setText(fileName);

        int iconResId = R.drawable.ic_file;
        if (fileType.contains("pdf")) iconResId = R.drawable.ic_pdf;
        else if (fileType.contains("word")) iconResId = R.drawable.ic_word;
        else if (fileType.contains("excel")) iconResId = R.drawable.ic_excel;
        else if (fileType.contains("txt")) iconResId = R.drawable.ic_txt;
        else if (fileType.contains("image")) iconResId = R.drawable.ic_images;

        if (isGrid) {
            // Grid: show thumbnail or icon
            if (fileType.contains("image")) {
                Glide.with(context)
                        .load(document.getUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.imageViewDocumentThumb);
            } else {
                holder.imageViewDocumentThumb.setImageResource(iconResId);
            }
            holder.imageViewTypeIcon.setImageResource(iconResId);
        } else {
            // List: show icon
            holder.imageViewDocumentType.setImageResource(iconResId);
        }

        // Set click listeners
        if (isGrid) {
            holder.itemView.setOnClickListener(v -> openDocument(document));
            holder.itemView.setOnLongClickListener(v -> { showPopupMenu(v, document); return true; });
        } else {
            holder.buttonMore.setOnClickListener(v -> showPopupMenu(v, document));
            holder.itemView.setOnClickListener(v -> openDocument(document));
        }
    }

    private void showPopupMenu(View view, Document document) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.menu_document_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_download) {
                downloadDocument(document);
                return true;
            } else if (itemId == R.id.menu_rename) {
                listener.onRenameDocument(document);
                return true;
            } else if (itemId == R.id.menu_delete) {
                listener.onDeleteDocument(document);
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    private void downloadDocument(Document document) {
        if (document.getType().equals("drive")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(document.getUrl()));
            context.startActivity(intent);
        } else {
            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(document.getUrl());
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                context.startActivity(intent);
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openDocument(Document document) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(document.getUrl()), getMimeType(document.getType()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String type) {
        type = type.toLowerCase();
        if (type.contains("pdf")) return "application/pdf";
        if (type.contains("word")) return "application/msword";
        if (type.contains("excel")) return "application/vnd.ms-excel";
        if (type.contains("txt")) return "text/plain";
        if (type.contains("image")) return "image/*";
        return "*/*";
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        // List
        ImageView imageViewDocumentType;
        ImageButton buttonMore;
        TextView textViewDocumentName;
        // Grid
        ImageView imageViewDocumentThumb;
        ImageView imageViewTypeIcon;

        DocumentViewHolder(View itemView) {
            super(itemView);
            // List
            imageViewDocumentType = itemView.findViewById(R.id.imageViewDocumentType);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            textViewDocumentName = itemView.findViewById(R.id.textViewDocumentName);
            // Grid
            imageViewDocumentThumb = itemView.findViewById(R.id.imageViewDocumentThumb);
            imageViewTypeIcon = itemView.findViewById(R.id.imageViewTypeIcon);
        }
    }
} 