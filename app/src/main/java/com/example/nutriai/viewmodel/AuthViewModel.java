package com.example.nutriai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.nutriai.SessionManager;
import com.example.nutriai.data.NutriRepository;
import com.example.nutriai.data.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

public class AuthViewModel extends AndroidViewModel {

    public static class AuthResult {
        public final boolean success;
        public final String error;
        public AuthResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    public final MutableLiveData<AuthResult> loginResult = new MutableLiveData<>();
    public final MutableLiveData<AuthResult> signupResult = new MutableLiveData<>();

    private final NutriRepository repository;
    private final SessionManager sessionManager;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new NutriRepository(application);
        sessionManager = new SessionManager(application);
    }

    public void login(String email, String password) {
        String emailTrimmed = email.trim().toLowerCase();
        if (emailTrimmed.isEmpty()) {
            loginResult.setValue(new AuthResult(false, "Email is required"));
            return;
        }
        if (password.isEmpty()) {
            loginResult.setValue(new AuthResult(false, "Password is required"));
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = repository.findUserByEmail(emailTrimmed);
            if (user == null) {
                loginResult.postValue(new AuthResult(false, "No account found with that email"));
                return;
            }
            if (!user.passwordHash.equals(hash(password))) {
                loginResult.postValue(new AuthResult(false, "Incorrect password"));
                return;
            }
            sessionManager.login(user.id, user.name);
            loginResult.postValue(new AuthResult(true, null));
        });
    }

    public void signup(String name, String email, String password, String confirmPassword) {
        String nameTrimmed = name.trim();
        String emailTrimmed = email.trim().toLowerCase();

        if (nameTrimmed.isEmpty()) {
            signupResult.setValue(new AuthResult(false, "Name is required"));
            return;
        }
        if (emailTrimmed.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            signupResult.setValue(new AuthResult(false, "Enter a valid email address"));
            return;
        }
        if (password.length() < 6) {
            signupResult.setValue(new AuthResult(false, "Password must be at least 6 characters"));
            return;
        }
        if (!password.equals(confirmPassword)) {
            signupResult.setValue(new AuthResult(false, "Passwords do not match"));
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            User existing = repository.findUserByEmail(emailTrimmed);
            if (existing != null) {
                signupResult.postValue(new AuthResult(false, "An account with this email already exists"));
                return;
            }
            User user = new User();
            user.name = nameTrimmed;
            user.email = emailTrimmed;
            user.passwordHash = hash(password);
            long id = repository.insertUser(user);
            if (id <= 0) {
                signupResult.postValue(new AuthResult(false, "Failed to create account. Try again."));
                return;
            }
            sessionManager.login((int) id, nameTrimmed);
            signupResult.postValue(new AuthResult(true, null));
        });
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return input;
        }
    }
}
