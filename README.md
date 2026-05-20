# NutriAI

An on-device AI nutrition assistant for Android. All inference runs locally using a Llama 3.2 1B Instruct model via ExecuTorch. No internet connection is required and no user data ever leaves the device.

---

## Features

- On-device AI chat for nutrition and meal planning questions
- Personalised 7-day meal plan generation based on user profile
- Dietary goal and restriction settings (weight loss, muscle gain, keto, vegan, etc.)
- Collapsible shopping list automatically extracted from each generated meal plan
- Saved meal plan history with favouriting and deletion
- Safety filter that blocks non-nutrition and unsafe queries
- Per-user data isolation with login and signup
- Demo mode when no model file is present

---

## Requirements

| Requirement | Value |
|---|---|
| Android Studio | Panda 1 (2025.3.1 Patch 1) or later stable |
| AGP | 8.9.x (compatible with Android Studio Panda) |
| JDK | 8 |
| compileSdk | 36 |
| targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Model file | Llama 3.2 1B ExecuTorch (`.pte`) + `tokenizer.model` |

---

## LLM Integration

### Model

**Llama 3.2 1B Instruct** running via **ExecuTorch** (Meta's on-device inference runtime).

**Why Llama 3.2 1B?**
- Small enough (≈2 GB) to fit in mobile RAM with ExecuTorch quantisation
- Instruction-tuned variant follows the NutriAI system prompt reliably
- Native Llama 3 chat template (`<|begin_of_text|>` / `<|start_header_id|>`) keeps the model in role without fine-tuning
- Fully open weights — no API key, no subscription, no usage fees

**Integration mode:** On-device inference (preferred). No data ever leaves the device.

### What data leaves the device?

Nothing. The model runs entirely on-device via ExecuTorch. Meal plans, user profiles, and chat messages are stored in a Room SQLite database on the local device. Passwords are stored as SHA-256 hashes. No network calls are made anywhere in the app.

### Offline capability

The app works with no internet connection after installation. The only external dependency is the model file, which must be pushed to the device once via ADB or selected through the file picker.

### Safety handling

`ResponseParser.isSafeQuery()` screens every user message before it reaches the model. A keyword blocklist covers self-harm, medical prescriptions, eating disorders, and dangerous diet extremes. Blocked queries show a short warning banner; the model is never called. The model's own system prompt also instructs it to refuse off-topic requests.

---

## Model Setup

### Option 1: ADB Push (recommended for development)

```bash
adb push Llama-3.2-1B-Instruct.pte /sdcard/Android/data/com.example.nutriai/files/
adb push tokenizer.model            /sdcard/Android/data/com.example.nutriai/files/
```

The app detects any `.pte` file in that directory automatically on next launch.

### Option 2: File Picker

On first launch, if no model files are detected, the app opens a setup screen where you can select both files from device storage. The URIs are persisted with `takePersistableUriPermission` so the app never asks again.

### Demo Mode

If neither option is completed the app runs in demo mode, returning mock responses so the UI can be explored without a model file. Select "Use Demo Mode" on the setup screen.

---

## Building

1. Clone the repository
2. Open the project in Android Studio Panda 1 or later
3. Let Gradle sync complete
4. Run on a physical device or emulator (API 26+)

No API keys or external services are needed. The `executorch.aar` is included in `app/libs/`.

---

## Android 16+ Compatibility

### Back Navigation

The app uses the Jetpack `OnBackPressedDispatcher` API (`androidx.activity:activity:1.9.0`) throughout. `MainActivity` registers an `OnBackPressedCallback` that handles navigation between fragments and a double-press-to-exit pattern on the home tab.

`android:enableOnBackInvokedCallback="true"` is declared in `AndroidManifest.xml`, enabling the predictive back gesture on Android 13+ without breaking older devices.

No deprecated `onBackPressed()` overrides are used anywhere in the codebase.

### Native Library Alignment

ExecuTorch's pre-built `.so` files do not yet ship with 16 KB ELF LOAD alignment. `useLegacyPackaging = true` in `build.gradle.kts` (and `android:extractNativeLibs="true"` in the manifest) instruct the installer to copy native libraries to disk rather than memory-mapping them from the APK, satisfying Android 15+ compatibility. This will be removed once ExecuTorch ships a 16 KB-aligned AAR.

---

## Screenshots

| File | Screen |
|---|---|
| `screenshots/login.jpg` | Sign In screen — NutriAI branding, email / password fields |
| `screenshots/signup.jpg` | Create Account screen — full name, email, password, confirm |
| `screenshots/model_setup.png` | First-run model setup — ADB option A and file picker option B |
| `screenshots/chat.jpg` | Chat (Nutrition Q&A) — user greeting, AI streaming response |
| `screenshots/meal_planner.jpg` | Meal Planner — generated 7-day plan with kcal per meal |
| `screenshots/shopping_list.jpg` | Shopping List — 54 consolidated ingredients with ×count badges |
| `screenshots/history.jpg` | Saved Meal Plans — bookmarked plan with star and delete actions |
| `screenshots/profile.jpg` | Profile — dietary goal, restrictions, caloric target, offline notice |
| `screenshots/api35_screenshot.png` | App running on API 35 (Android 15) emulator |
| `screenshots/api36_screenshot.png` | App running on API 36 (Android 16) emulator |

---

## Testing Evidence

| API Level | Platform | Screenshot |
|---|---|---|
| 35 — Android 15 "VanillaIceCream" | Emulator — Pixel 9 | `screenshots/api35_screenshot.png` |
| 36.1 — Android 16 "Baklava" | Emulator — Pixel 10 | `screenshots/api36_screenshot.png` |

Layout, navigation, and back gesture all behave correctly on both API levels.

---

---

## Project Structure

```
app/src/main/java/com/example/nutriai/
    ai/                  ExecuTorch integration (LlamaModule, ModelManager, PromptBuilder, ResponseParser)
    data/                Room database, DAOs, entities (User, UserProfile, MealPlan)
    fragment/            Chat, MealPlanner, History, Profile, Login, Signup fragments
    viewmodel/           ViewModels for each fragment and auth
    adapter/             RecyclerView adapters (Chat, History, MealDay, Ingredient)
    AuthActivity.java    Login and signup host
    MainActivity.java    Navigation host with bottom nav, model init, back-press handling
    ModelSetupActivity.java  First-run file picker for model and tokenizer
    SessionManager.java  SharedPreferences-based session persistence
```

---

## Tech Stack

| Component | Library / Version |
|---|---|
| Language | Java 8 |
| compileSdk | 36 |
| targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| AI Runtime | ExecuTorch (local AAR) |
| AI Model | Llama 3.2 1B Instruct |
| Database | Room 2.6.1 |
| Navigation | Jetpack Navigation 2.7.7 |
| UI | Material Components 1.12.0 |
| Architecture | MVVM with LiveData |
| Back Navigation | OnBackPressedDispatcher (activity:1.9.0) |

---

## Privacy

All data including meal plans, user profiles, and chat history is stored locally in a Room database on the device. Passwords are stored as SHA-256 hashes. No data is transmitted to any server. The app functions entirely offline after installation.
