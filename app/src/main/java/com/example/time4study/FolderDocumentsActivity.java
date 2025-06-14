package com.example.time4study;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapters.DocumentAdapter;
import com.example.models.Document;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FolderDocumentsActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentActionListener {

    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private List<Document> documents = new ArrayList<>();
    private FirebaseFirestore db;
    private String folderId;
    private String uid;
    private FloatingActionButton fabAddDocument;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_documents); // Sử dụng lại layout

        recyclerView = findViewById(R.id.recyclerViewDocuments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DocumentAdapter(this, documents, this);
        recyclerView.setAdapter(adapter);

        fabAddDocument = findViewById(R.id.fabAddDocument);
        fabAddDocument.setOnClickListener(v -> showAddDocumentDialog());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Nút back
        toolbar.setNavigationOnClickListener(v -> onBackPressed()); // Xử lý khi bấm

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        folderId = getIntent().getStringExtra("folderId");
        if (folderId == null) {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDocumentsInFolder();
    }

    private void loadDocumentsInFolder() {
        db.collection("folders").document(folderId).collection("documents")
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
                    adapter.notifyDataSetChanged();
                });
    }

    private void refreshDocuments() {
        db.collection("folders").document(folderId).collection("documents")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    documents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Document document = doc.toObject(Document.class);
                        document.setId(doc.getId());
                        documents.add(document);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error refreshing documents: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteDocument(Document document) {
        db.collection("folders").document(folderId).collection("documents").document(document.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int position = documents.indexOf(document);
                    documents.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                        db.collection("folders").document(folderId).collection("documents").document(document.getId())
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

    private void showAddDocumentDialog() {
        String[] options = {"Upload PDF", "Upload Image", "Add Google Drive Link"};
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
                    }
                })
                .show();
    }

    private void pickFile(String mimeType, String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(Intent.createChooser(intent, "Select File"), type.equals("pdf") ? 201 : 202);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == 201) {
                uploadFile(uri, "pdf");
            } else if (requestCode == 202) {
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
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("folders").child(folderId).child(uniqueFileName);
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
                    document.setTimestamp(com.google.firebase.Timestamp.now());
                    db.collection("folders").document(folderId).collection("documents").add(document)
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
        document.setTimestamp(com.google.firebase.Timestamp.now());
        db.collection("folders").document(folderId).collection("documents")
                .add(document)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Document added successfully", Toast.LENGTH_SHORT).show();
                    refreshDocuments(); // Refresh after successful save
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
