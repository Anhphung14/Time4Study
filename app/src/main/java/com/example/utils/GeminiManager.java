package com.example.utils;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException;

public class GeminiManager {
    private static final String TAG = "GeminiManager";
    private static final String API_KEY = ""; // 🔐 Nhớ ẩn API key khi push code
    private static GeminiManager instance;
    private final GenerativeModel model;
    private static final int MAX_RETRIES = 3;
    private static final long TIMEOUT_MS = 60000; // 60 seconds timeout

    private GeminiManager() {
        try {
            model = new GenerativeModel("gemini-1.5-flash", API_KEY);
            Log.d(TAG, "Gemini model initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gemini model", e);
            throw new RuntimeException("Failed to initialize Gemini model: " + e.getMessage());
        }
    }

    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void generateResponse(String prompt, GeminiCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Prompt cannot be empty");
            }
            return;
        }

        try {
            Log.d(TAG, "Generating response for prompt: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
            GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);

            // Tạo Content từ prompt
            Content content = new Content.Builder()
                    .addPart(new TextPart(prompt))
                    .build();

            // Gửi yêu cầu bất đồng bộ với retry logic
            sendRequestWithRetry(modelFutures, content, callback, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error creating content", e);
            if (callback != null) {
                callback.onError("Failed to create request: " + e.getMessage());
            }
        }
    }

    private void sendRequestWithRetry(GenerativeModelFutures modelFutures, Content content, 
                                    GeminiCallback callback, int retryCount) {
        try {
            Log.d(TAG, "Sending request to Gemini API (attempt " + (retryCount + 1) + ")");
            ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

            // Xử lý callback với timeout
            Futures.addCallback(
                    response,
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            try {
                                if (result == null || result.getText() == null) {
                                    Log.e(TAG, "Empty response from Gemini API");
                                    if (callback != null) {
                                        callback.onError("Empty response from Gemini API");
                                    }
                                    return;
                                }
                                String responseText = result.getText();
                                Log.d(TAG, "Received response from Gemini API: " + 
                                    responseText.substring(0, Math.min(100, responseText.length())) + "...");
                                if (callback != null) {
                                    callback.onSuccess(responseText);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing Gemini response", e);
                                if (callback != null) {
                                    callback.onError("Error processing response: " + e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "Error generating response", t);
                            
                            // Kiểm tra nếu là lỗi có thể retry
                            if (retryCount < MAX_RETRIES && isRetryableError(t)) {
                                Log.d(TAG, "Retrying request, attempt " + (retryCount + 1));
                                // Thêm delay trước khi retry
                                try {
                                    Thread.sleep(1000 * (retryCount + 1)); // Exponential backoff
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                                sendRequestWithRetry(modelFutures, content, callback, retryCount + 1);
                            } else {
                                String errorMessage = getErrorMessage(t);
                                Log.e(TAG, "Final error after " + (retryCount + 1) + " attempts: " + errorMessage);
                                if (callback != null) {
                                    callback.onError(errorMessage);
                                }
                            }
                        }
                    },
                    Runnable::run
            );
        } catch (Exception e) {
            Log.e(TAG, "Error in sendRequestWithRetry", e);
            if (callback != null) {
                callback.onError("Failed to send request: " + e.getMessage());
            }
        }
    }

    private boolean isRetryableError(Throwable t) {
        if (t instanceof GoogleGenerativeAIException) {
            String message = t.getMessage();
            return message != null && (
                message.contains("timeout") ||
                message.contains("network") ||
                message.contains("connection") ||
                message.contains("retry") ||
                message.contains("rate limit") ||
                message.contains("quota") ||
                message.contains("busy") ||
                message.contains("unavailable")
            );
        }
        return false;
    }

    private String getErrorMessage(Throwable t) {
        if (t instanceof GoogleGenerativeAIException) {
            String message = t.getMessage();
            if (message.contains("quota") || message.contains("rate limit")) {
                return "API rate limit exceeded. Please try again later.";
            } else if (message.contains("invalid")) {
                return "Invalid request. Please check your input.";
            } else {
                return "Gemini API error: " + message;
            }
        } else if (t instanceof java.util.concurrent.CancellationException) {
            return "Request was cancelled. Please try again.";
        } else if (t instanceof java.util.concurrent.TimeoutException) {
            return "Request timed out. Please try again.";
        } else {
            return "An unexpected error occurred: " + t.getMessage();
        }
    }

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
