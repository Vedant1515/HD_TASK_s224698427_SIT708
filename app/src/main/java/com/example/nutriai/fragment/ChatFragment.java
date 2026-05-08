package com.example.nutriai.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;
import com.example.nutriai.adapter.ChatAdapter;
import com.example.nutriai.viewmodel.ChatViewModel;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvSafetyWarning;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        tvSafetyWarning = view.findViewById(R.id.tvSafetyWarning);

        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> sendMessage());

        observeViewModel();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");
        viewModel.sendMessage(text, requireContext());
    }

    private int lastMessageCount = 0;

    private void observeViewModel() {
        viewModel.messages.observe(getViewLifecycleOwner(), messages -> {
            if (messages == null || messages.isEmpty()) {
                chatAdapter.setMessages(messages);
                lastMessageCount = 0;
                return;
            }
            if (messages.size() == lastMessageCount && !messages.isEmpty()) {
                // Streaming update — only the last message text changed
                chatAdapter.streamLastMessage(messages.get(messages.size() - 1));
            } else {
                // New message added — full update + scroll to bottom
                chatAdapter.setMessages(messages);
                rvChat.scrollToPosition(messages.size() - 1);
                lastMessageCount = messages.size();
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            btnSend.setEnabled(!loading);
        });

        viewModel.unsafeQuery.observe(getViewLifecycleOwner(), unsafe -> {
            if (unsafe != null && unsafe) {
                tvSafetyWarning.setVisibility(View.VISIBLE);
                handler.postDelayed(() -> {
                    tvSafetyWarning.setVisibility(View.GONE);
                    viewModel.unsafeQuery.setValue(false);
                }, 3000);
            }
        });
    }
}
