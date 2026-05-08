package com.example.nutriai.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.nutriai.ai.LlamaModule;
import com.example.nutriai.ai.PromptBuilder;
import com.example.nutriai.ai.ResponseParser;
import com.example.nutriai.data.NutriRepository;
import com.example.nutriai.data.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChatViewModel extends AndroidViewModel {

    public static class ChatMessage {
        public final String text;
        public final boolean isUser;
        public final long timestamp;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }

        public ChatMessage(String text, boolean isUser, long timestamp) {
            this.text = text;
            this.isUser = isUser;
            this.timestamp = timestamp;
        }
    }

    private final NutriRepository repository;
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ResponseParser responseParser = new ResponseParser();

    public final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> unsafeQuery = new MutableLiveData<>(false);

    private int streamingMessageIndex = -1;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new NutriRepository(application);
    }

    public void sendMessage(String userMessage, Context context) {
        if (userMessage.trim().isEmpty()) return;

        if (!responseParser.isSafeQuery(userMessage)) {
            unsafeQuery.setValue(true);
            return;
        }

        addMessage(new ChatMessage(userMessage, true));
        isLoading.setValue(true);

        // Placeholder for streaming AI response
        ChatMessage streamingPlaceholder = new ChatMessage("", false);
        List<ChatMessage> current = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        current.add(streamingPlaceholder);
        streamingMessageIndex = current.size() - 1;
        messages.setValue(current);

        Executors.newSingleThreadExecutor().execute(() -> {
            UserProfile profile = repository.getUserProfileSync();

            // Build history from previous turns only.
            // snapshot = [...previous turns..., currentUserMsg, streamingPlaceholder]
            // We stop at size-2 so the current user message is NOT included here
            // (it is appended explicitly at the end of the prompt).
            // Keeping only the last 6 messages (3 turns) prevents context overflow on the 1B model.
            StringBuilder historyBuilder = new StringBuilder();
            List<ChatMessage> snapshot = messages.getValue();
            if (snapshot != null && snapshot.size() > 2) {
                int historyEnd = snapshot.size() - 2; // exclude current user msg + placeholder
                int start = Math.max(0, historyEnd - 6);
                for (int i = start; i < historyEnd; i++) {
                    ChatMessage msg = snapshot.get(i);
                    if (msg.text == null || msg.text.isEmpty()) continue;
                    // Llama 3 native format so the model stays in role
                    if (msg.isUser) {
                        historyBuilder.append("<|start_header_id|>user<|end_header_id|>\n\n")
                                .append(msg.text).append("<|eot_id|>");
                    } else {
                        historyBuilder.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
                                .append(msg.text).append("<|eot_id|>");
                    }
                }
            }

            String prompt = promptBuilder.buildChatPrompt(profile, userMessage, historyBuilder.toString());
            final StringBuilder accumulated = new StringBuilder();

            LlamaModule.getInstance().runInference(prompt, new LlamaModule.TokenCallback() {
                @Override
                public void onToken(String token) {
                    accumulated.append(token);
                    updateStreamingMessage(stripEcho(accumulated.toString()));
                }

                @Override
                public void onComplete(String fullResponse) {
                    updateStreamingMessage(stripEcho(fullResponse));
                    isLoading.postValue(false);
                    streamingMessageIndex = -1;
                }

                @Override
                public void onError(String error) {
                    updateStreamingMessage("Sorry, I encountered an error. Please try again.");
                    isLoading.postValue(false);
                    streamingMessageIndex = -1;
                }
            });
        });
    }

    private void addMessage(ChatMessage msg) {
        List<ChatMessage> current = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        current.add(msg);
        messages.setValue(current);
    }

    private void updateStreamingMessage(String text) {
        List<ChatMessage> current = messages.getValue();
        if (current == null || streamingMessageIndex < 0 || streamingMessageIndex >= current.size()) return;
        List<ChatMessage> updated = new ArrayList<>(current);
        long ts = current.get(streamingMessageIndex).timestamp;
        updated.set(streamingMessageIndex, new ChatMessage(text, false, ts));
        messages.postValue(updated);
    }

    private String stripEcho(String text) {
        if (text == null) return "";

        // Llama 3 native format: model output follows the last assistant header
        final String ASST_HEADER = "<|start_header_id|>assistant<|end_header_id|>";
        int idx = text.lastIndexOf(ASST_HEADER);
        if (idx != -1) {
            String after = text.substring(idx + ASST_HEADER.length()).trim();
            // Stop at end-of-turn token
            int eot = after.indexOf("<|eot_id|>");
            if (eot >= 0) after = after.substring(0, eot);
            // Stop if model hallucinates the next user/system header
            int nextHeader = after.indexOf("<|start_header_id|>");
            if (nextHeader > 0) after = after.substring(0, nextHeader);
            after = stripSpecialTokens(after.trim());
            return after.isEmpty() ? "..." : after;
        }

        // Fallback: plain-text echo format (mock mode)
        int plainIdx = text.lastIndexOf("NutriAI:");
        if (plainIdx != -1) {
            String after = text.substring(plainIdx + "NutriAI:".length()).trim();
            int userTurn = after.indexOf("\nUser:");
            if (userTurn > 0) after = after.substring(0, userTurn);
            after = stripSpecialTokens(after.trim());
            return after.isEmpty() ? "..." : after;
        }

        return stripSpecialTokens(text.trim());
    }

    private static String stripSpecialTokens(String text) {
        return text.replaceAll("<\\|[a-zA-Z0-9_]+\\|>", "").trim();
    }

    public void clearHistory() {
        messages.setValue(new ArrayList<>());
    }
}
