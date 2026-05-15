package com.example.nutriai.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.nutriai.ai.ModelManager;

public class ModelSetupViewModel extends AndroidViewModel {

    public final MutableLiveData<Boolean> modelReady      = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> tokenizerReady  = new MutableLiveData<>(false);
    public final MutableLiveData<String>  toastMessage    = new MutableLiveData<>();

    public ModelSetupViewModel(@NonNull Application application) {
        super(application);
        ModelManager mm = ModelManager.getInstance();
        if (mm.hasModelUri(application))     modelReady.setValue(true);
        if (mm.hasTokenizerUri(application)) tokenizerReady.setValue(true);
    }

    public void runAutoDetect() {
        Application app = getApplication();
        ModelManager mm = ModelManager.getInstance();
        boolean modelFound     = mm.autoDetectModelPath(app) != null;
        boolean tokenizerFound = mm.autoDetectTokenizerPath(app) != null;

        if (modelFound)     modelReady.setValue(true);
        if (tokenizerFound) tokenizerReady.setValue(true);

        if (modelFound && tokenizerFound) {
            toastMessage.setValue("Both files found — tap Continue!");
        } else if (!modelFound && !tokenizerFound) {
            toastMessage.setValue("No files found. Push via ADB or use Browse.");
        } else if (!modelFound) {
            toastMessage.setValue("Tokenizer found, but model (.pte) not found.");
        } else {
            toastMessage.setValue("Model found, but tokenizer.model not found.");
        }
    }

    public void onModelPicked(Uri uri) {
        ModelManager.getInstance().saveModelUri(getApplication(), uri);
        modelReady.setValue(true);
    }

    public void onTokenizerPicked(Uri uri) {
        ModelManager.getInstance().saveTokenizerUri(getApplication(), uri);
        tokenizerReady.setValue(true);
    }

    public void enterDemoMode() {
        ModelManager.getInstance().setDemoMode(getApplication(), true);
    }

    public boolean isReadyToContinue() {
        return Boolean.TRUE.equals(modelReady.getValue())
                && Boolean.TRUE.equals(tokenizerReady.getValue());
    }
}
