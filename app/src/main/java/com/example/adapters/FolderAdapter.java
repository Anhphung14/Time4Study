package com.example.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.models.Folder;
import com.example.time4study.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    public interface OnFolderActionListener {
        void onFolderClick(Folder folder);
        void onRenameFolder(Folder folder);
        void onDeleteFolder(Folder folder);
    }

    private List<Folder> folders;
    private OnFolderActionListener listener;
    private Context context;

    public FolderAdapter(List<Folder> folders, OnFolderActionListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.textViewFolderName.setText(folder.getName());
        
        holder.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
        holder.buttonMore.setOnClickListener(v -> showPopupMenu(v, folder));
    }

    private void showPopupMenu(View view, Folder folder) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.menu_folder_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_rename) {
                listener.onRenameFolder(folder);
                return true;
            } else if (itemId == R.id.menu_delete) {
                listener.onDeleteFolder(folder);
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFolderName;
        ImageButton buttonMore;

        FolderViewHolder(View itemView) {
            super(itemView);
            textViewFolderName = itemView.findViewById(R.id.textViewFolderName);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
} 