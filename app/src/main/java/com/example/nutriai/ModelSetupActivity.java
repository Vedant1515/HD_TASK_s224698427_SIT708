package com.example.nutriai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutriai.ai.ModelManager;

/**
 * One-time setup screen shown when model files are not yet reachable.
 *
 * Two ways to supply the files — whichever the user prefers:
 *
 *  A. ADB push (emulator / USB debug):
 *       adb push "<model>.pte"      <externalFilesDir>/
 *       adb push "tokenizer.model"  <externalFilesDir>/
 *     The exact target path is shown on screen. After a push the user taps
 *     "I've copied the files" and the app detects them automatically.
 *
 *  B. File picker:
 *     Browse to wherever the files are on the device — no ADB needed.
 *     URIs are persisted; re-selection is never required.
 *
 * No files are copied inside the app regardless of which path is used.
 */
public class ModelSetupActivity extends AppCompatActivity {

    private TextView tvModelStatus;
    private TextView tvTokenizerStatus;
    private Button   btnPickModel;
    private Button   btnPickTokenizer;
    private Button   btnContinue;
    private Button   btnAutoDetect;

    private final ActivityResultLauncher<String[]> modelPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> { if (uri != null) onModelPicked(uri); });

    private final ActivityResultLauncher<String[]> tokenizerPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> { if (uri != null) onTokenizerPicked(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_setup);

        tvModelStatus     = findViewById(R.id.tvModelStatus);
        tvTokenizerStatus = findViewById(R.id.tvTokenizerStatus);
        btnPickModel      = findViewById(R.id.btnPickModel);
        btnPickTokenizer  = findViewById(R.id.btnPickTokenizer);
        btnContinue       = findViewById(R.id.btnContinue);
        btnAutoDetect     = findViewById(R.id.btnAutoDetect);
        TextView tvAdbCommand = findViewById(R.id.tvAdbCommand);
        TextView tvDemoMode   = findViewById(R.id.tvDemoMode);

        ModelManager mm = ModelManager.getInstance();

        // Show the exact ADB push target path for this device
        String targetDir = mm.getAdbTargetDir(this);
        tvAdbCommand.setText(
                "adb push \"<model>.pte\"     " + targetDir + "/\n" +
                "adb push \"tokenizer.model\"  " + targetDir + "/");

        // Auto-detect on open (handles the case where files were already pushed)
        runAutoDetect();

        // Also restore previously picked SAF URIs
        if (!isModelReady()) {
            if (mm.hasModelUri(this))     markModelReady();
            if (mm.hasTokenizerUri(this)) markTokenizerReady();
            refreshContinueButton();
        }

        btnAutoDetect.setOnClickListener(v -> runAutoDetect());

        btnPickModel.setOnClickListener(v ->
                modelPicker.launch(new String[]{"*/*"}));

        btnPickTokenizer.setOnClickListener(v ->
                tokenizerPicker.launch(new String[]{"*/*"}));

        btnContinue.setOnClickListener(v -> launchMain());

        tvDemoMode.setOnClickListener(v -> {
            mm.setDemoMode(this, true);
            launchMain();
        });
    }

    // ── Auto-detection ────────────────────────────────────────────────────────

    private void runAutoDetect() {
        ModelManager mm = ModelManager.getInstance();
        boolean modelFound     = mm.autoDetectModelPath(this) != null;
        boolean tokenizerFound = mm.autoDetectTokenizerPath(this) != null;

        if (modelFound)     markModelReady();
        if (tokenizerFound) markTokenizerReady();
        refreshContinueButton();

        if (modelFound && tokenizerFound) {
            Toast.makeText(this, "Both files found — tap Continue!", Toast.LENGTH_SHORT).show();
        } else if (!modelFound && !tokenizerFound) {
            Toast.makeText(this,
                    "No files found. Push them using the ADB command above, or use Browse.",
                    Toast.LENGTH_LONG).show();
        } else if (!modelFound) {
            Toast.makeText(this, "Tokenizer found, but model (.pte) not found.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Model found, but tokenizer.model not found.", Toast.LENGTH_LONG).show();
        }
    }

    // ── Picker callbacks ──────────────────────────────────────────────────────

    private void onModelPicked(Uri uri) {
        ModelManager.getInstance().saveModelUri(this, uri);
        markModelReady();
        refreshContinueButton();
    }

    private void onTokenizerPicked(Uri uri) {
        ModelManager.getInstance().saveTokenizerUri(this, uri);
        markTokenizerReady();
        refreshContinueButton();
    }

    // ── UI state ──────────────────────────────────────────────────────────────

    private boolean modelReady     = false;
    private boolean tokenizerReady = false;

    private boolean isModelReady() { return modelReady; }

    private void markModelReady() {
        modelReady = true;
        tvModelStatus.setText("Found ✓");
        tvModelStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        btnPickModel.setText("Change model file…");
    }

    private void markTokenizerReady() {
        tokenizerReady = true;
        tvTokenizerStatus.setText("Found ✓");
        tvTokenizerStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        btnPickTokenizer.setText("Change tokenizer file…");
    }

    private void refreshContinueButton() {
        boolean ready = modelReady && tokenizerReady;
        btnContinue.setEnabled(ready);
        btnContinue.setAlpha(ready ? 1.0f : 0.5f);
    }

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
