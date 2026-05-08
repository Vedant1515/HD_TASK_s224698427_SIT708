package com.example.nutriai.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;
import com.example.nutriai.adapter.MealDayAdapter;
import com.example.nutriai.viewmodel.MealPlanViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashMap;
import java.util.Map;

public class MealPlannerFragment extends Fragment {

    private static final String[] LOADING_MESSAGES = {
            "Analysing your nutrition profile…",
            "Planning your weekly meals…",
            "Selecting breakfast options…",
            "Choosing lunch combinations…",
            "Adding dinner recipes…",
            "Finalising your 7-day plan…",
            "Almost ready…"
    };

    private MealPlanViewModel viewModel;
    private MealDayAdapter mealDayAdapter;
    private Button btnGenerate;
    private Button btnSave;
    private View cardLoading;
    private TextView tvStatus;
    private View cardMealPlan;

    // Shopping list views
    private View cardShoppingList;
    private View headerShoppingList;
    private LinearLayout containerShoppingList;
    private ImageView chevronShopping;
    private TextView tvIngredientCount;
    private boolean shoppingExpanded = false;

    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private Runnable loadingRunnable;
    private int loadingMsgIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal_planner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MealPlanViewModel.class);

        btnGenerate  = view.findViewById(R.id.btnGenerate);
        btnSave      = view.findViewById(R.id.btnSave);
        cardLoading  = view.findViewById(R.id.cardLoading);
        tvStatus     = view.findViewById(R.id.tvStatus);
        cardMealPlan = view.findViewById(R.id.cardMealPlan);

        // Shopping list
        cardShoppingList    = view.findViewById(R.id.cardShoppingList);
        headerShoppingList  = view.findViewById(R.id.headerShoppingList);
        containerShoppingList = view.findViewById(R.id.containerShoppingList);
        chevronShopping     = view.findViewById(R.id.chevronShopping);
        tvIngredientCount   = view.findViewById(R.id.tvIngredientCount);

        RecyclerView rvMealDays = view.findViewById(R.id.rvMealDays);
        mealDayAdapter = new MealDayAdapter();
        rvMealDays.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMealDays.setAdapter(mealDayAdapter);
        rvMealDays.setNestedScrollingEnabled(false);
        rvMealDays.setItemAnimator(null);

        btnGenerate.setOnClickListener(v -> generatePlan());
        btnSave.setOnClickListener(v -> savePlan(view));
        headerShoppingList.setOnClickListener(v -> toggleShoppingList());

        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingMessages();
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            cardLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnGenerate.setAlpha(loading ? 0.55f : 1.0f);
            if (loading) {
                startLoadingMessages();
                mealDayAdapter.setMealPlanText(null);
                cardMealPlan.setVisibility(View.GONE);
                cardShoppingList.setVisibility(View.GONE);
                btnSave.setVisibility(View.GONE);
                shoppingExpanded = false;
            } else {
                stopLoadingMessages();
            }
        });

        viewModel.mealPlanResult.observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                mealDayAdapter.setMealPlanText(result.mealPlan);
                cardMealPlan.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.VISIBLE);
                try {
                    buildShoppingList();
                } catch (Exception e) {
                    cardShoppingList.setVisibility(View.GONE);
                }
            }
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void toggleShoppingList() {
        shoppingExpanded = !shoppingExpanded;
        containerShoppingList.setVisibility(shoppingExpanded ? View.VISIBLE : View.GONE);
        chevronShopping.setRotation(shoppingExpanded ? 180f : 0f);
    }

    private void buildShoppingList() {
        LinkedHashMap<String, Integer> ingredients = mealDayAdapter.getConsolidatedIngredients();
        containerShoppingList.removeAllViews();

        if (ingredients.isEmpty()) {
            cardShoppingList.setVisibility(View.GONE);
            return;
        }

        Context ctx = requireContext();
        float dp = ctx.getResources().getDisplayMetrics().density;

        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (6 * dp), 0, (int) (6 * dp));

            View dot = new View(ctx);
            int dotSize = (int) (7 * dp);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMarginEnd((int) (12 * dp));
            dot.setLayoutParams(dotParams);
            dot.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_badge));

            TextView tvName = new TextView(ctx);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);
            tvName.setText(entry.getKey());
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            tvName.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextPrimary));

            row.addView(dot);
            row.addView(tvName);

            // Only show the ×N badge when the ingredient appears in more than one meal
            if (entry.getValue() > 1) {
                TextView tvCount = new TextView(ctx);
                tvCount.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvCount.setText("×" + entry.getValue());
                tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                tvCount.setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary));
                row.addView(tvCount);
            }

            containerShoppingList.addView(row);
        }

        tvIngredientCount.setText(ingredients.size() + " items");
        shoppingExpanded = false;
        containerShoppingList.setVisibility(View.GONE);
        chevronShopping.setRotation(0f);
        cardShoppingList.setVisibility(View.VISIBLE);
    }

    private void startLoadingMessages() {
        loadingMsgIndex = 0;
        loadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvStatus != null) {
                    tvStatus.setText(LOADING_MESSAGES[loadingMsgIndex % LOADING_MESSAGES.length]);
                    loadingMsgIndex++;
                    loadingHandler.postDelayed(this, 3500);
                }
            }
        };
        loadingHandler.post(loadingRunnable);
    }

    private void stopLoadingMessages() {
        if (loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
            loadingRunnable = null;
        }
    }

    private void generatePlan() {
        viewModel.generateMealPlan(requireContext());
    }

    private void savePlan(View view) {
        viewModel.savePlan();
        Snackbar.make(view, R.string.plan_saved, Snackbar.LENGTH_SHORT).show();
        btnSave.setVisibility(View.GONE);
    }
}
