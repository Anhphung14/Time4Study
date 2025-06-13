package com.example.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.Date;

public class FirestoreDataProcessor {
    public interface FirestoreCallback {
        void onDataReady(String prompt);
        void onError(String error);
    }

    public static void getCompletedGoalsSummary(String uid, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("studyGoals")
          .whereEqualTo("uid", uid)
          .whereEqualTo("completed", true)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              int count = queryDocumentSnapshots.size();
              String contextPrompt = "Bạn đã hoàn thành " + count + " mục tiêu. Hãy nhắc lại số lượng này trong câu trả lời và động viên người dùng.";
              callback.onDataReady(contextPrompt);
          })
          .addOnFailureListener(e -> {
              callback.onError("Lỗi truy vấn Firestore: " + e.getMessage());
          });
    }
} 