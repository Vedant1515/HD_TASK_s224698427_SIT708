package com.example.nutriai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.nutriai.ai.LlamaModule;
import com.example.nutriai.ai.ModelManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private ProgressBar progressBar;
    private TextView tvInitStatus;
    private boolean backPressedOnce = false;
    private final Handler backHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        if (!ModelManager.getInstance().isSetupComplete(this)) {
            startActivity(new Intent(this, ModelSetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        progressBar    = findViewById(R.id.progressBar);
        tvInitStatus   = findViewById(R.id.tvInitStatus);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Obtain NavController from the NavHostFragment
        FragmentManager fm = getSupportFragmentManager();
        Fragment navHost = fm.findFragmentById(R.id.navHostFragment);
        if (navHost instanceof NavHostFragment) {
            navController = ((NavHostFragment) navHost).getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        registerBackCallback();
        initModel();
    }

    // ── Model initialisation ──────────────────────────────────────────────────

    private void initModel() {
        setLoadingVisible(true);
        ModelManager.getInstance().copyModelIfNeeded(this, new ModelManager.CopyCallback() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> progressBar.setProgress(percent));
            }

            @Override
            public void onComplete(String path) {
                if (path != null && !path.isEmpty()) {
                    String tokenizerPath = ModelManager.getInstance().getLastTokenizerPath();
                    if (tokenizerPath != null && !tokenizerPath.isEmpty()) {
                        LlamaModule.getInstance().loadModel(path, tokenizerPath);
                    } else {
                        LlamaModule.getInstance().loadModel(path);
                    }
                }
                runOnUiThread(() -> setLoadingVisible(false));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvInitStatus.setText(getString(R.string.init_error));
                    setLoadingVisible(false);
                });
            }
        });
    }

    private void setLoadingVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(v);
        tvInitStatus.setVisibility(v);
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    private void registerBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController == null) {
                    finish();
                    return;
                }

                int destId = -1;
                if (navController.getCurrentDestination() != null) {
                    destId = navController.getCurrentDestination().getId();
                }

                if (destId == R.id.profileFragment) {
                    if (backPressedOnce) {
                        finish();
                    } else {
                        backPressedOnce = true;
                        Toast.makeText(MainActivity.this,
                                getString(R.string.press_back_again),
                                Toast.LENGTH_SHORT).show();
                        backHandler.postDelayed(() -> backPressedOnce = false, 2000);
                    }
                } else {
                    navController.navigate(R.id.profileFragment);
                }
            }
        });
    }
}
