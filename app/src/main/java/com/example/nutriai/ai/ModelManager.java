package com.example.nutriai.ai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves the on-device model files using two strategies, tried in order:
 *
 *  1. Auto-detect — scans getExternalFilesDir() for any *.pte and tokenizer.model.
 *     Push files there once and the app finds them automatically:
 *       adb push "<model>.pte"      /sdcard/Android/data/com.example.nutriai/files/
 *       adb push "tokenizer.model"  /sdcard/Android/data/com.example.nutriai/files/
 *
 *  2. File picker — user selects files via Android's SAF picker in ModelSetupActivity.
 *     URIs are persisted with takePersistableUriPermission so the app never asks again.
 *     Files are accessed via /proc/self/fd/<n>: zero bytes copied regardless of size.
 *
 * In both cases the resolved paths are passed to LlamaModule — no other class changes.
 */
public class ModelManager {

    private static final String TAG            = "ModelManager";
    private static final String PREFS          = "nutri_model_setup";
    private static final String KEY_MODEL_URI  = "model_uri";
    private static final String KEY_TOKER_URI  = "tokenizer_uri";
    private static final String KEY_DEMO_MODE  = "demo_mode";

    private static ModelManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // PFDs kept open while ExecuTorch holds the underlying mmap (SAF path only)
    private ParcelFileDescriptor modelPfd;
    private ParcelFileDescriptor tokenizerPfd;

    // Resolved tokenizer path, fetched by MainActivity after copyModelIfNeeded completes
    private String lastTokenizerPath;

    public interface CopyCallback {
        void onProgress(int percent);
        void onComplete(String modelPath);   // "" → demo / mock mode
        void onError(String error);
    }

    private ModelManager() {}

    public static synchronized ModelManager getInstance() {
        if (instance == null) instance = new ModelManager();
        return instance;
    }

    // ── Setup state ───────────────────────────────────────────────────────────

    /** True when the app can reach the model without showing the setup screen. */
    public boolean isSetupComplete(Context context) {
        if (prefs(context).getBoolean(KEY_DEMO_MODE, false)) return true;
        // Auto-detect passes without any prior picker interaction
        if (autoDetectModelPath(context) != null
                && autoDetectTokenizerPath(context) != null) return true;
        // Or the user has already picked both files via SAF
        return prefs(context).getString(KEY_MODEL_URI, null) != null
                && prefs(context).getString(KEY_TOKER_URI, null) != null;
    }

    public boolean isDemoMode(Context context) {
        return prefs(context).getBoolean(KEY_DEMO_MODE, false);
    }

    public void setDemoMode(Context context, boolean demo) {
        prefs(context).edit().putBoolean(KEY_DEMO_MODE, demo).apply();
    }

    public boolean hasModelUri(Context context) {
        return autoDetectModelPath(context) != null
                || prefs(context).getString(KEY_MODEL_URI, null) != null;
    }

    public boolean hasTokenizerUri(Context context) {
        return autoDetectTokenizerPath(context) != null
                || prefs(context).getString(KEY_TOKER_URI, null) != null;
    }

    // ── Auto-detection from app's external files dir ──────────────────────────

    /**
     * Scans getExternalFilesDir() for any *.pte file.
     * Works with any filename — handles spaces, version suffixes, etc.
     */
    public String autoDetectModelPath(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null || !dir.exists()) return null;
        File[] pteFiles = dir.listFiles((d, name) -> name.endsWith(".pte"));
        if (pteFiles != null && pteFiles.length > 0) {
            return pteFiles[0].getAbsolutePath();
        }
        return null;
    }

    /** Looks for tokenizer.model in getExternalFilesDir(). */
    public String autoDetectTokenizerPath(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return null;
        File f = new File(dir, "tokenizer.model");
        return (f.exists() && f.length() > 0) ? f.getAbsolutePath() : null;
    }

    /** The directory the user should ADB-push files into. */
    public String getAdbTargetDir(Context context) {
        File dir = context.getExternalFilesDir(null);
        return dir != null ? dir.getAbsolutePath() : "/sdcard/Android/data/com.example.nutriai/files";
    }

    // ── SAF URI persistence (file-picker path) ────────────────────────────────

    public void saveModelUri(Context context, Uri uri) {
        takePersistablePermission(context, uri);
        prefs(context).edit().putString(KEY_MODEL_URI, uri.toString()).apply();
    }

    public void saveTokenizerUri(Context context, Uri uri) {
        takePersistablePermission(context, uri);
        prefs(context).edit().putString(KEY_TOKER_URI, uri.toString()).apply();
    }

    private void takePersistablePermission(Context context, Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.w(TAG, "takePersistableUriPermission failed: " + e.getMessage());
        }
    }

    // ── Path resolution ───────────────────────────────────────────────────────

    /** Returns the resolved model path (direct or fd), or null. */
    private String resolveModelPath(Context context) {
        // Priority 1: direct file in app's external dir (ADB push)
        String direct = autoDetectModelPath(context);
        if (direct != null) return direct;
        // Priority 2: SAF URI from file picker
        return openFdPath(context, KEY_MODEL_URI, true);
    }

    /** Returns the resolved tokenizer path (direct or fd), or null. */
    private String resolveTokenizerPath(Context context) {
        String direct = autoDetectTokenizerPath(context);
        if (direct != null) return direct;
        return openFdPath(context, KEY_TOKER_URI, false);
    }

    /** Opens a SAF URI as a PFD and returns its /proc/self/fd/<n> path. */
    private String openFdPath(Context context, String prefKey, boolean isModel) {
        String uriStr = prefs(context).getString(prefKey, null);
        if (uriStr == null) return null;
        try {
            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(Uri.parse(uriStr), "r");
            if (pfd == null) return null;
            if (isModel) { closePfd(modelPfd); modelPfd = pfd; }
            else          { closePfd(tokenizerPfd); tokenizerPfd = pfd; }
            return "/proc/self/fd/" + pfd.getFd();
        } catch (Exception e) {
            Log.e(TAG, "Cannot open URI (" + prefKey + "): " + e.getMessage());
            return null;
        }
    }

    // ── Tokenizer path for MainActivity ──────────────────────────────────────

    public String getLastTokenizerPath() { return lastTokenizerPath; }

    // ── MainActivity-compatible API ───────────────────────────────────────────

    /**
     * Resolves model + tokenizer paths and fires onComplete(modelPath).
     * MainActivity fetches getLastTokenizerPath() separately for the two-arg
     * LlamaModule.loadModel(model, tokenizer) call.
     * onComplete("") means demo / mock mode — no code downstream changes.
     */
    public void copyModelIfNeeded(Context context, CopyCallback callback) {
        executor.execute(() -> {
            if (isDemoMode(context)) {
                callback.onProgress(100);
                callback.onComplete("");
                return;
            }
            String modelPath     = resolveModelPath(context);
            String tokenizerPath = resolveTokenizerPath(context);
            if (modelPath != null && tokenizerPath != null) {
                lastTokenizerPath = tokenizerPath;
                Log.i(TAG, "Model: " + modelPath);
                Log.i(TAG, "Tokenizer: " + tokenizerPath);
                callback.onProgress(100);
                callback.onComplete(modelPath);
            } else {
                Log.w(TAG, "Model files not found — demo mode");
                callback.onComplete("");
            }
        });
    }

    public void releaseFds() {
        closePfd(modelPfd);
        closePfd(tokenizerPfd);
        modelPfd = null;
        tokenizerPfd = null;
    }

    /** Clears all saved URIs and flags, returning the app to the setup screen on next launch. */
    public void clearSetup(Context context) {
        prefs(context).edit()
                .remove(KEY_MODEL_URI)
                .remove(KEY_TOKER_URI)
                .remove(KEY_DEMO_MODE)
                .apply();
        releaseFds();
        lastTokenizerPath = null;
    }

    private void closePfd(ParcelFileDescriptor pfd) {
        if (pfd != null) { try { pfd.close(); } catch (IOException ignored) {} }
    }

    private SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                      .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
