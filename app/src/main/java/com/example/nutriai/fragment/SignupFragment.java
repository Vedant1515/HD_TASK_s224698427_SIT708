package com.example.nutriai.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nutriai.AuthActivity;
import com.example.nutriai.MainActivity;
import com.example.nutriai.R;
import com.example.nutriai.viewmodel.AuthViewModel;

public class SignupFragment extends Fragment {

    private AuthViewModel viewModel;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        tvError = view.findViewById(R.id.tvError);
        Button btnSignup = view.findViewById(R.id.btnSignup);
        TextView tvGoLogin = view.findViewById(R.id.tvGoLogin);

        btnSignup.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            viewModel.signup(
                    etName.getText().toString(),
                    etEmail.getText().toString(),
                    etPassword.getText().toString(),
                    etConfirmPassword.getText().toString()
            );
        });

        tvGoLogin.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        viewModel.signupResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.success) {
                startActivity(new Intent(requireActivity(), MainActivity.class));
                requireActivity().finish();
            } else {
                tvError.setText(result.error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}
