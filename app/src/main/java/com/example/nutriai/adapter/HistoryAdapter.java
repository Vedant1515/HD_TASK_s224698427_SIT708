package com.example.nutriai.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;
import com.example.nutriai.data.MealPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private static final int COLOR_BOOKMARKED = Color.parseColor("#F9A825");

    public interface HistoryListener {
        void onFavourite(int id, int current);
        void onDelete(int id);
        void onClick(MealPlan plan);
    }

    private final List<MealPlan> plans = new ArrayList<>();
    private HistoryListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public void setPlans(List<MealPlan> newPlans) {
        plans.clear();
        if (newPlans != null) plans.addAll(newPlans);
        notifyDataSetChanged();
    }

    public void setListener(HistoryListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View      viewTopBar;
        final TextView  tvDate;
        final TextView  tvPreview;
        final TextView  tvFavBadge;
        final ImageButton btnFavourite;
        final ImageButton btnDelete;

        ViewHolder(View v) {
            super(v);
            viewTopBar   = v.findViewById(R.id.viewTopBar);
            tvDate       = v.findViewById(R.id.tvDate);
            tvPreview    = v.findViewById(R.id.tvPreview);
            tvFavBadge   = v.findViewById(R.id.tvFavBadge);
            btnFavourite = v.findViewById(R.id.btnFavourite);
            btnDelete    = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealPlan plan = plans.get(position);
        holder.tvDate.setText(dateFormat.format(new Date(plan.createdAt)));

        String preview = plan.planContent != null && plan.planContent.length() > 120
                ? plan.planContent.substring(0, 120) + "…"
                : (plan.planContent != null ? plan.planContent : "");
        holder.tvPreview.setText(preview);

        boolean bookmarked = plan.isFavourited == 1;

        // Top accent bar: amber when bookmarked, green gradient otherwise
        if (bookmarked) {
            holder.viewTopBar.setBackgroundColor(COLOR_BOOKMARKED);
        } else {
            holder.viewTopBar.setBackgroundResource(R.drawable.bg_button_primary);
        }

        // Bookmark badge: visible amber pill with star when starred
        holder.tvFavBadge.setVisibility(bookmarked ? View.VISIBLE : View.GONE);

        // Star icon: filled amber when bookmarked, outline otherwise
        if (bookmarked) {
            holder.btnFavourite.setImageResource(R.drawable.ic_star_filled);
            holder.btnFavourite.setColorFilter(COLOR_BOOKMARKED);
        } else {
            holder.btnFavourite.setImageResource(R.drawable.ic_star);
            holder.btnFavourite.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        }
        holder.btnFavourite.setContentDescription(
                bookmarked ? "Remove bookmark" : "Bookmark this plan");

        holder.btnFavourite.setOnClickListener(v -> {
            if (listener != null) listener.onFavourite(plan.id, plan.isFavourited);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(plan.id);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(plan);
        });
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }
}
