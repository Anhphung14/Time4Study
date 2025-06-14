package com.example.time4study;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.adapters.DocumentAdapter;
import com.example.adapters.FolderAdapter;
import com.example.models.Document;
import com.example.models.Folder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.Timestamp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentsActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentActionListener {
    private RecyclerView recyclerViewFolders, recyclerViewDocuments;
    private FolderAdapter folderAdapter;
    private DocumentAdapter documentAdapter;
    private List<Folder> folders = new ArrayList<>();
    private List<Document> documents = new ArrayList<>();
    private FloatingActionButton fabAddDocument, fabToggleView;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String uid;
    private boolean isGrid = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerViewFolders = findViewById(R.id.recyclerViewFolders);
        recyclerViewDocuments = findViewById(R.id.recyclerViewDocuments);
        fabAddDocument = findViewById(R.id.fabAddDocument);
        fabToggleView = findViewById(R.id.fabToggleView);

        folderAdapter = new FolderAdapter(folders, new FolderAdapter.OnFolderActionListener() {
            @Override
            public void onFolderClick(Folder folder) {
                openFolder(folder);
            }

            @Override
            public void onRenameFolder(Folder folder) {
                showRenameFolderDialog(folder);
            }

            @Override
            public void onDeleteFolder(Folder folder) {
                showDeleteFolderDialog(folder);
            }
        });
        recyclerViewFolders.setAdapter(folderAdapter);
        recyclerViewFolders.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        documentAdapter = new DocumentAdapter(this, documents, this);
        recyclerViewDocuments.setAdapter(documentAdapter);
        recyclerViewDocuments.setLayoutManager(new LinearLayoutManager(this));

        fabAddDocument.setOnClickListener(v -> showAddDocumentDialog());
        fabToggleView.setOnClickListener(v -> {
            isGrid = !isGrid;
            updateLayoutManager();
            updateToggleIcon();
        });

        loadFolders();
        loadDocuments();
        updateLayoutManager();
        updateToggleIcon();
    }

    private void loadFolders() {
        db.collection("folders")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading folders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    folders.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Folder folder = doc.toObject(Folder.class);
                            folder.setId(doc.getId());
                            folders.add(folder);
                        }
                    }
                    folderAdapter.notifyDataSetChanged();
                });
    }

    private void refreshFolders() {
        db.collection("folders")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    folders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Folder folder = doc.toObject(Folder.class);
                        folder.setId(doc.getId());
                        folders.add(folder);
                    }
                    folderAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error refreshing folders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDocuments() {
        db.collection("documents")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading documents: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    documents.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Document document = doc.toObject(Document.class);
                            document.setId(doc.getId());
                            documents.add(document);
                        }
                    }
                    documentAdapter.notifyDataSetChanged();
                });
    }

    private void refreshDocuments() {
        db.collection("documents")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    documents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Document document = doc.toObject(Document.class);
                        document.setId(doc.getId());
                        documents.add(document);
                    }
                    documentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error refreshing documents: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddDocumentDialog() {
        String[] options = {"Upload PDF", "Upload Image", "Add Google Drive Link", "Create Folder"};
        new AlertDialog.Builder(this)
                .setTitle("Add Document")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // PDF
                            pickFile("application/pdf", "pdf");
                            break;
                        case 1: // Image
                            pickFile("image/*", "image");
                            break;
                        case 2: // Google Drive Link
                            showGoogleDriveLinkDialog();
                            break;
                        case 3: // Create Folder
                            showCreateFolderDialog();
                            break;
                    }
                })
                .show();
    }

    private void pickFile(String mimeType, String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(Intent.createChooser(intent, "Select File"), type.equals("pdf") ? 101 : 102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == 101) {
                uploadFile(uri, "pdf");
            } else if (requestCode == 102) {
                uploadFile(uri, "image");
            }
        }
    }

    private void uploadFile(Uri fileUri, String type) {
        if (fileUri == null) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }
        try (InputStream input = getContentResolver().openInputStream(fileUri)) {
            if (input == null) {
                Toast.makeText(this, "Cannot open file!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot access file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFileName = getFileName(fileUri);
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = timestamp + fileExtension;
        StorageReference storageRef = storage.getReference().child("documents").child(uid).child(uniqueFileName);
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Uploading...")
                .setMessage("Please wait while we upload your file")
                .setCancelable(false)
                .create();
        progressDialog.show();
        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Document document = new Document();
                    document.setName(originalFileName != null ? originalFileName : uniqueFileName);
                    document.setUrl(uri.toString());
                    document.setType(type);
                    document.setUid(uid);
                    document.setTimestamp(Timestamp.now());
                    db.collection("documents").add(document)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                refreshDocuments(); // Refresh after successful upload
                            })
                            .addOnFailureListener(e -> {
                                storageRef.delete();
                                Toast.makeText(this, "Error saving document info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            });
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void showGoogleDriveLinkDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_google_drive_link, null);
        EditText editTextLink = dialogView.findViewById(R.id.editTextDriveLink);
        EditText editTextName = dialogView.findViewById(R.id.editTextDocumentName);

        new AlertDialog.Builder(this)
                .setTitle("Add Google Drive Link")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String link = editTextLink.getText().toString();
                    String name = editTextName.getText().toString();
                    if (!link.isEmpty() && !name.isEmpty()) {
                        saveGoogleDriveLink(name, link);
                    } else {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveGoogleDriveLink(String name, String link) {
        Document document = new Document();
        document.setName(name);
        document.setUrl(link);
        document.setType("drive");
        document.setUid(uid);
        document.setTimestamp(Timestamp.now());
        db.collection("documents")
                .add(document)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Document added successfully", Toast.LENGTH_SHORT).show();
                    refreshDocuments(); // Refresh after successful save
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showCreateFolderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null);
        EditText editTextFolderName = dialogView.findViewById(R.id.editTextFolderName);

        new AlertDialog.Builder(this)
                .setTitle("Create Folder")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String folderName = editTextFolderName.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        createFolder(folderName);
                    } else {
                        Toast.makeText(this, "Please enter a folder name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createFolder(String folderName) {
        Folder folder = new Folder();
        folder.setName(folderName);
        folder.setUid(uid);
        folder.setTimestamp(com.google.firebase.Timestamp.now());

        db.collection("folders")
                .add(folder)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Folder created successfully", Toast.LENGTH_SHORT).show();
                    refreshFolders(); // Refresh after successful folder creation
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openFolder(Folder folder) {
        if (folder.getId() == null) {
            Toast.makeText(this, "Folder ID is null!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FolderDocumentsActivity.class);
        intent.putExtra("folderId", folder.getId());
        startActivity(intent);
    }

    private void updateLayoutManager() {
        if (isGrid) {
            recyclerViewDocuments.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        } else {
            recyclerViewDocuments.setLayoutManager(new LinearLayoutManager(this));
        }
        recyclerViewDocuments.setAdapter(documentAdapter);
    }

    private void updateToggleIcon() {
        if (isGrid) {
            fabToggleView.setImageResource(R.drawable.ic_list_view);
        } else {
            fabToggleView.setImageResource(R.drawable.ic_grid_view);
        }
    }

    @Override
    public void onDeleteDocument(Document document) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("documents").document(document.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show();
                                refreshDocuments(); // Refresh after successful deletion
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRenameDocument(Document document) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_document, null);
        EditText editTextName = dialogView.findViewById(R.id.editTextDocumentName);
        editTextName.setText(document.getName());

        new AlertDialog.Builder(this)
                .setTitle("Rename Document")
                .setView(dialogView)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editTextName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.collection("documents").document(document.getId())
                                .update("name", newName)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Document renamed successfully", Toast.LENGTH_SHORT).show();
                                    refreshDocuments(); // Refresh after successful rename
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error renaming document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteFolderDialog(Folder folder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete this folder? All documents inside will be moved to root.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Move all documents to root
                    db.collection("documents")
                            .whereEqualTo("folderId", folder.getId())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().update("folderId", null);
                                }
                                // Delete folder
                                db.collection("folders").document(folder.getId()).delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show();
                                            refreshFolders();
                                            refreshDocuments();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error deleting folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error moving documents: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameFolderDialog(Folder folder) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_folder, null);
        EditText editTextFolderName = dialogView.findViewById(R.id.editTextFolderName);
        editTextFolderName.setText(folder.getName());

        new AlertDialog.Builder(this)
                .setTitle("Rename Folder")
                .setView(dialogView)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editTextFolderName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.collection("folders").document(folder.getId())
                                .update("name", newName)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Folder renamed", Toast.LENGTH_SHORT).show();
                                    refreshFolders();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error renaming folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}