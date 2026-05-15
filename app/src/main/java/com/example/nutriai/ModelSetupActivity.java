package com.example.nutriai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.nutriai.ai.ModelManager;
import com.example.nutriai.viewmodel.ModelSetupViewModel;

public class ModelSetupActivity extends AppCompatActivity {

    private TextView tvModelStatus;
    private TextView tvTokenizerStatus;
    private Button   btnPickModel;
    private Button   btnPickTokenizer;
    private Button   btnContinue;

    private ModelSetupViewModel viewModel;

    private final ActivityResultLauncher<String[]> modelPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> { if (uri != null) viewModel.onModelPicked(uri); });

    private final ActivityResultLauncher<String[]> tokenizerPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> { if (uri != null) viewModel.onTokenizerPicked(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_setup);

        viewModel = new ViewModelProvider(this).get(ModelSetupViewModel.class);

        tvModelStatus     = findViewById(R.id.tvModelStatus);
        tvTokenizerStatus = findViewById(R.id.tvTokenizerStatus);
        btnPickModel      = findViewById(R.id.btnPickModel);
        btnPickTokenizer  = findViewById(R.id.btnPickTokenizer);
        btnContinue       = findViewById(R.id.btnContinue);
        Button   btnAutoDetect = findViewById(R.id.btnAutoDetect);
        TextView tvAdbCommand  = findViewById(R.id.tvAdbCommand);
        TextView tvDemoMode    = findViewById(R.id.tvDemoMode);

        String targetDir = ModelManager.getInstance().getAdbTargetDir(this);
        tvAdbCommand.setText(
                "adb push \"<model>.pte\"     " + targetDir + "/\n" +
                "adb push \"tokenizer.model\"  " + targetDir + "/");

        viewModel.modelReady.observe(this, ready -> {
            if (Boolean.TRUE.equals(ready)) {
                tvModelStatus.setText("Found ✓");
                tvModelStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                btnPickModel.setText("Change model file…");
            }
            refreshContinueButton();
        });

        viewModel.tokenizerReady.observe(this, ready -> {
            if (Boolean.TRUE.equals(ready)) {
                tvTokenizerStatus.setText("Found ✓");
                tvTokenizerStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                btnPickTokenizer.setText("Change tokenizer file…");
            }
            refreshContinueButton();
        });

        viewModel.toastMessage.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });

        btnAutoDetect.setOnClickListener(v -> viewModel.runAutoDetect());
        btnPickModel.setOnClickListener(v -> modelPicker.launch(new String[]{"*/*"}));
        btnPickTokenizer.setOnClickListener(v -> tokenizerPicker.launch(new String[]{"*/*"}));
        btnContinue.setOnClickListener(v -> launchMain());
        tvDemoMode.setOnClickListener(v -> {
            viewModel.enterDemoMode();
            launchMain();
        });

        if (!viewModel.isReadyToContinue()) {
            viewModel.runAutoDetect();
        }
    }

    private void refreshContinueButton() {
        boolean ready = viewModel.isReadyToContinue();
        btnContinue.setEnabled(ready);
        btnContinue.setAlpha(ready ? 1.0f : 0.5f);
    }

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
