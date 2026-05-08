package com.example.nutriai.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nutriai.AuthActivity;
import com.example.nutriai.R;
import com.example.nutriai.SessionManager;
import com.example.nutriai.data.UserProfile;
import com.example.nutriai.viewmodel.ProfileViewModel;
import com.google.android.material.snackbar.Snackbar;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private Spinner spinnerGoal;
    private Spinner spinnerRestrictions;
    private EditText etCaloricTarget;
    private EditText etAllergies;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        spinnerGoal = view.findViewById(R.id.spinnerGoal);
        spinnerRestrictions = view.findViewById(R.id.spinnerRestrictions);
        etCaloricTarget = view.findViewById(R.id.etCaloricTarget);
        etAllergies = view.findViewById(R.id.etAllergies);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        setupSpinners();

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) populateFields(profile);
        });

        btnSave.setOnClickListener(v -> saveProfile(view));
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        new SessionManager(requireContext()).logout();
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupSpinners() {
        String[] goals = getResources().getStringArray(R.array.dietary_goals);
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, goals);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoal.setAdapter(goalAdapter);

        String[] restrictions = getResources().getStringArray(R.array.dietary_restrictions);
        ArrayAdapter<String> restrictionAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, restrictions);
        restrictionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRestrictions.setAdapter(restrictionAdapter);
    }

    private void populateFields(UserProfile profile) {
        String[] goals = getResources().getStringArray(R.array.dietary_goals);
        for (int i = 0; i < goals.length; i++) {
            if (goals[i].equals(profile.dietaryGoal)) {
                spinnerGoal.setSelection(i);
                break;
            }
        }

        String[] restrictions = getResources().getStringArray(R.array.dietary_restrictions);
        for (int i = 0; i < restrictions.length; i++) {
            if (restrictions[i].equals(profile.restrictions)) {
                spinnerRestrictions.setSelection(i);
                break;
            }
        }

        if (profile.caloricTarget > 0) {
            etCaloricTarget.setText(String.valueOf(profile.caloricTarget));
        }
        if (profile.allergies != null) {
            etAllergies.setText(profile.allergies);
        }
    }

    private void saveProfile(View view) {
        String caloricStr = etCaloricTarget.getText().toString().trim();
        if (caloricStr.isEmpty()) {
            etCaloricTarget.setError(getString(R.string.error_caloric_required));
            return;
        }

        int calories;
        try {
            calories = Integer.parseInt(caloricStr);
            if (calories <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etCaloricTarget.setError(getString(R.string.error_caloric_invalid));
            return;
        }

        String goal = spinnerGoal.getSelectedItem().toString();
        String restrictions = spinnerRestrictions.getSelectedItem().toString();
        String allergies = etAllergies.getText().toString().trim();

        UserProfile profile = new UserProfile(goal, restrictions, calories, allergies);
        viewModel.saveProfile(profile);

        Snackbar.make(view, R.string.profile_saved, Snackbar.LENGTH_SHORT).show();
    }
}
