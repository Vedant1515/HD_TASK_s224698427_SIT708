package com.example.nutriai.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;
import com.example.nutriai.adapter.HistoryAdapter;
import com.example.nutriai.adapter.MealDayAdapter;
import com.example.nutriai.data.MealPlan;
import com.example.nutriai.viewmodel.HistoryViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment implements HistoryAdapter.HistoryListener {

    private HistoryViewModel viewModel;
    private HistoryAdapter historyAdapter;
    private View tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        tvEmpty = view.findViewById(R.id.tvEmpty);
        RecyclerView rvHistory = view.findViewById(R.id.rvHistory);
        historyAdapter = new HistoryAdapter();
        historyAdapter.setListener(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(historyAdapter);

        viewModel.getAllMealPlans().observe(getViewLifecycleOwner(), plans -> {
            historyAdapter.setPlans(plans);
            tvEmpty.setVisibility(plans == null || plans.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onFavourite(int id, int current) {
        viewModel.toggleFavourite(id, current);
    }

    @Override
    public void onDelete(int id) {
        viewModel.deletePlan(id);
    }

    @Override
    public void onClick(MealPlan plan) {
        showPlanBottomSheet(plan);
    }

    private void showPlanBottomSheet(MealPlan plan) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_plan, null);

        // Date header
        TextView tvDate = sheetView.findViewById(R.id.tvSheetDate);
        String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(plan.createdAt));
        tvDate.setText(dateStr);

        // Formatted day cards — same adapter / same rendering as the live Meal Planner page
        RecyclerView rv = sheetView.findViewById(R.id.rvSheetMealDays);
        MealDayAdapter adapter = new MealDayAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        rv.setNestedScrollingEnabled(false);
        adapter.setMealPlanText(plan.planContent);

        dialog.setContentView(sheetView);

        // Open fully expanded so user can scroll through all 7 days
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        dialog.show();
    }
}
