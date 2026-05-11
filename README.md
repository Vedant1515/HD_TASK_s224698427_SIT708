# NutriAI

An on-device AI nutrition assistant for Android. All inference runs locally using a Llama 3.2 1B model via ExecuTorch. No internet connection is required and no user data leaves the device.

---

## Features

- On-device AI chat for nutrition and meal planning questions
- Personalised 7-day meal plan generation based on user profile
- Dietary goal and restriction settings (weight loss, muscle gain, keto, vegan, etc.)
- Saved meal plan history with favouriting and deletion
- Safety filter that blocks non-nutrition queries
- Per-user data isolation with login and signup
- Demo mode when no model file is present

---

## Requirements

- Android 8.0 or higher (minSdk 26)
- Android Studio Hedgehog or newer
- JDK 8
- Llama 3.2 1B ExecuTorch model file (`.pte`) and `tokenizer.model`

---

## Model Setup

The app supports two ways to load the model files.

### Option 1: ADB Push (recommended for development)

Push both files into the app's external files directory:

```bash
adb push llama3_2.pte /sdcard/Android/data/com.example.nutriai/files/
adb push tokenizer.model /sdcard/Android/data/com.example.nutriai/files/
```

The app detects any `.pte` file in that directory automatically on next launch.

### Option 2: File Picker

On first launch, if no model files are detected, the app opens a setup screen where you can select both files from device storage. The URIs are persisted so the app never asks again.

### Demo Mode

If neither option is completed the app runs in demo mode, returning mock responses so the UI can be explored without a model file.

---

## Building

1. Clone the repository
2. Open the project in Android Studio
3. Let Gradle sync complete
4. Run on a physical device or emulator (API 26+)

No API keys or external services are needed. The `executorch.aar` is included in `app/libs/`.

---

## Project Structure

```
app/src/main/java/com/example/nutriai/
    ai/                  ExecuTorch integration (LlamaModule, ModelManager, PromptBuilder, ResponseParser)
    data/                Room database, DAOs, entities (User, UserProfile, MealPlan)
    fragment/            Chat, MealPlanner, History, Profile, Login, Signup fragments
    viewmodel/           ViewModels for each fragment and auth
    adapter/             RecyclerView adapters
    AuthActivity.java    Login and signup host
    MainActivity.java    Navigation host with bottom nav and model initialisation
    ModelSetupActivity.java  First-run file picker for model and tokenizer
    SessionManager.java  SharedPreferences-based session persistence
```

---

## Tech Stack

| Component | Library / Version |
|-----------|------------------|
| Language | Java 8 |
| Minimum SDK | 26 (Android 8.0) |
| AI Runtime | ExecuTorch (local AAR) |
| AI Model | Llama 3.2 1B |
| Database | Room 2.6.1 |
| Navigation | Jetpack Navigation 2.7.7 |
| UI | Material Components 1.12.0 |
| Architecture | MVVM with LiveData |

---

## Privacy

All data including meal plans, user profiles, and chat history is stored locally in a Room database on the device. Passwords are stored as SHA-256 hashes. No data is transmitted to any server.
