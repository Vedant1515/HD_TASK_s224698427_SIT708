package com.example.nutriai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;

import java.util.ArrayList;
import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {

    private final List<String> ingredients = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIngredient;

        ViewHolder(View v) {
            super(v);
            tvIngredient = v.findViewById(R.id.tvIngredient);
        }
    }

    public void setIngredients(String ingredientsText) {
        ingredients.clear();
        if (ingredientsText == null || ingredientsText.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        String[] lines = ingredientsText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                // Strip leading "- " bullet
                if (trimmed.startsWith("- ")) trimmed = trimmed.substring(2);
                ingredients.add(trimmed);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvIngredient.setText(ingredients.get(position));
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }
}
