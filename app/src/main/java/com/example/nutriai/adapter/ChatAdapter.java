package com.example.nutriai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;
import com.example.nutriai.viewmodel.ChatViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    private final List<ChatViewModel.ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final TextView tvTimestamp;

        UserViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
        }
    }

    public static class AiViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final TextView tvTimestamp;

        AiViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
        }
    }

    public void setMessages(List<ChatViewModel.ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    // Efficient streaming update — only rebinds the last item, no full redraw
    public void streamLastMessage(ChatViewModel.ChatMessage msg) {
        if (messages.isEmpty()) return;
        int last = messages.size() - 1;
        messages.set(last, msg);
        notifyItemChanged(last);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_chat_ai, parent, false);
            return new AiViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatViewModel.ChatMessage msg = messages.get(position);
        String time = timeFormat.format(new Date(msg.timestamp));
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(msg.text);
            ((UserViewHolder) holder).tvTimestamp.setText(time);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).tvMessage.setText(msg.text);
            ((AiViewHolder) holder).tvTimestamp.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
