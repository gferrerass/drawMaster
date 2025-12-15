# Copilot Coding Agent Instructions

**Last Updated**: January 2025  
**Project Version**: 1.0 (MVP)

---

## 📌 High-Level Repository Overview

### What This Repository Does

This is an **Android drawing game application** built in **Kotlin** that challenges players to replicate randomly fetched images within a time limit. The app features both single-player (with ML-based scoring) and multiplayer (competitive with friends) game modes. It integrates with Unsplash API for random images, Firebase for real-time multiplayer sync and authentication, and a machine learning model for drawing similarity evaluation.

### Project Type & Stack

- **Type**: Native Android Application (Mobile)
- **Language**: Kotlin 100%
- **Architecture**: MVVM + Clean Architecture (3-layer: Presentation, Domain, Data)
- **Target SDK**: Android 13+ (API 33+)
- **Min SDK**: Android 11 (API 30)
- **Build System**: Gradle (Kotlin DSL)

### Key Technologies

- **UI Framework**: Jetpack Compose (modern declarative UI)
- **Real-time DB**: Firebase Realtime Database (multiplayer sync)
- **Authentication**: Firebase Authentication
- **Networking**: Retrofit + OkHttp (Unsplash API calls)
- **ML/AI**: TensorFlow Lite (MobileNet v2 for image similarity)
- **Sensors**: Android SensorManager (accelerometer for shake detection)
- **State Management**: ViewModel + StateFlow + Coroutines
- **Dependency Injection**: Hilt
- **Testing**: JUnit 4 + Mockito + Turbine

---

## 🏗️ Project Layout & Architecture

### Directory Structure

```
drawing-game-app/
├── .github/
│   └── copilot-instructions.md (this file)
├── .gitignore
├── build.gradle.kts (project-level build config)
├── settings.gradle.kts
├── gradle.properties
├── README.md
├── app/
│   ├── build.gradle.kts (app-level build config)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml (permissions, activities)
│   │   │   ├── java/com/drawingapp/
│   │   │   │   ├── MainActivity.kt (entry point)
│   │   │   │   ├── di/ (Hilt DI modules)
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── RepositoryModule.kt
│   │   │   │   │   └── DataSourceModule.kt
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── ui/screens/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── GameScreen.kt
│   │   │   │   │   │   ├── ResultScreen.kt
│   │   │   │   │   │   ├── FriendsScreen.kt
│   │   │   │   │   │   └── MultiplayerLobbyScreen.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── DrawingCanvas.kt
│   │   │   │   │   │   ├── TimerWidget.kt
│   │   │   │   │   │   └── ScoreDisplay.kt
│   │   │   │   │   ├── viewmodels/
│   │   │   │   │   │   ├── GameViewModel.kt
│   │   │   │   │   │   ├── FriendsViewModel.kt
│   │   │   │   │   │   └── MultiplayerViewModel.kt
│   │   │   │   │   └── navigation/
│   │   │   │   │       └── NavGraph.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── Game.kt
│   │   │   │   │   │   ├── Drawing.kt
│   │   │   │   │   │   └── User.kt
│   │   │   │   │   ├── repositories/
│   │   │   │   │   │   ├── GameRepository.kt (interface)
│   │   │   │   │   │   └── FriendsRepository.kt (interface)
│   │   │   │   │   └── usecases/
│   │   │   │   │       ├── GetRandomImageUseCase.kt
│   │   │   │   │       ├── EvaluateDrawingUseCase.kt
│   │   │   │   │       └── SyncGameStateUseCase.kt
│   │   │   │   └── data/
│   │   │   │       ├── remote/
│   │   │   │       │   ├── api/
│   │   │   │       │   │   └── UnsplashApiService.kt
│   │   │   │       │   ├── firebase/
│   │   │   │       │   │   ├── FirebaseAuthService.kt
│   │   │   │       │   │   └── FirebaseDbService.kt
│   │   │   │       │   └── datasources/
│   │   │   │       │       ├── RemoteGameDataSource.kt
│   │   │   │       │       └── RemoteUserDataSource.kt
│   │   │   │       └── repository/
│   │   │   │           ├── GameRepositoryImpl.kt
│   │   │   │           └── FriendsRepositoryImpl.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── drawable/
│   │   │   │   └── layout/ (if using XML layouts)
│   │   │   └── assets/
│   │   │       └── models/
│   │   │           └── mobilenet_v2.tflite
│   │   └── test/
│   │       └── java/com/drawingapp/
│   │           ├── viewmodels/
│   │           │   └── GameViewModelTest.kt
│   │           ├── domain/
│   │           │   └── usecases/
│   │           │       └── EvaluateDrawingUseCaseTest.kt
│   │           └── utils/
│   │               └── ImageUtilsTest.kt
│   └── androidTest/
│       └── java/com/drawingapp/
│           └── ui/
│               └── GameScreenTest.kt
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.jar
└── local.properties (not in repo, local only)
```

### Key Configuration Files

- **`build.gradle.kts` (project)**: Gradle version, plugin versions, repository definitions
- **`app/build.gradle.kts`**: App dependencies, SDK versions, build flavors, signing config
- **`gradle.properties`**: Gradle daemon settings, JVM args
- **`settings.gradle.kts`**: Module inclusion
- **`local.properties`** (local only): SDK path, local build settings
- **`AndroidManifest.xml`**: App permissions (INTERNET, SENSORS, CAMERA if applicable), activities, Firebase config

---

## 🛠️ Build & Validation Commands

### Prerequisites

- **Java**: JDK 11+ (recommend 17)
- **Android SDK**: API 33+ (auto-downloaded via Gradle)
- **Gradle**: 8.0+ (auto-installed via wrapper)
- **Kotlin**: 1.9.0+

### 1. Initial Setup (First Time Only)

```bash
# Clone repo and navigate
git clone <repo-url>
cd drawing-game-app

# Install Java 17 (if not present)
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk
# Windows: Download from oracle.com or use scoop

# Set Java home (optional if JDK 17 is default)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # Linux

# Verify setup
./gradlew --version
java -version
```

### 2. Clean & Build from Scratch

**Always run this before building if you've switched branches or had build errors:**

```bash
./gradlew clean
./gradlew build -x test  # Build without running tests first
```

**Expected time**: 3-5 minutes (first time may take longer due to dependency downloads)

### 3. Run Unit Tests

```bash
./gradlew test
```

**What it does**:

- Runs all tests in `app/src/test/`
- Generates coverage report in `app/build/reports/tests/`

**Expected time**: 1-2 minutes  
**Exit code**: 0 if all tests pass

**Troubleshooting**:

- If tests fail with "Cannot find symbol": Run `./gradlew clean` first
- If timeout: Increase JVM memory in `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m`

### 4. Run Instrumented Tests (Android Device Required)

```bash
./gradlew connectedAndroidTest
```

**Requirements**: Physical device or Android emulator running API 30+

### 5. Lint & Static Analysis

```bash
./gradlew lint                     # Android Lint
./gradlew ktlint                   # Kotlin style rules
./gradlew detekt                   # Code smells & complexity
```

**Expected**: All should pass with 0 warnings before merging

### 6. Build Debug APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 7. Build Release APK (Requires Signing Config)

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

**Note**: Requires signing key configured in `app/build.gradle.kts`

### 8. Build & Install to Emulator/Device

```bash
./gradlew installDebug
# Then run: adb shell am start -n com.drawingapp/.MainActivity
```

### 9. Run Application (Emulator Required)

```bash
# Start emulator first
emulator -avd Pixel_6_API_33 &

# Wait for boot (30-60 seconds)
adb wait-for-device

# Install and run
./gradlew installDebug
./gradlew runDebug
```

### 10. Full Validation (Mimics CI Pipeline)

```bash
./gradlew clean build ktlint detekt lint test
```

**Expected time**: 5-10 minutes  
**Exit code**: Must be 0 for PR to pass CI

---

## ⚠️ Critical Code Patterns & Non-Negotiables

### ❌ NEVER DO (Professor's Requirements & Best Practices)

1. **No state in UI layers**: Never use Activity/Fragment vars to hold state. Always use ViewModel + StateFlow.
2. **No nested callbacks**: Never chain `.then().then()` with callbacks. Use Coroutines + suspend functions.
3. **No GlobalScope**: Always use `viewModelScope` in ViewModels.
4. **No Firebase listeners without cleanup**: Always remove listeners in ViewModel.onCleared().
5. **No hardcoded strings**: Use `strings.xml` resources.
6. **No `!!` (not-null assertions)**: Use `?.let`, `?:`, or nullable returns.
7. **No raw coroutine launches**: Always wrap in try-catch for remote calls.
8. **No unscoped Bitmaps**: Always recycle Bitmap objects to prevent memory leaks.

### ✅ ALWAYS DO

1. **ViewModels for state**: All UI state lives in ViewModels using MutableStateFlow.
2. **StateFlow + Coroutines**: Use for async operations and reactive updates.
3. **Repository pattern**: Data access only through interfaces in domain layer.
4. **Hilt for DI**: Inject all dependencies via @Inject or @Provides.
5. **Error handling**: Wrap Firebase calls in try-catch, emit errors to StateFlow.
6. **Logging**: Use Timber or Log.d() for debugging, never System.out.println().
7. **Resource cleanup**: Close DB connections, remove listeners, recycle bitmaps in onCleared().
8. **Language**: Always use English for everything (code, comments, docs, UI, etc).

### Code Template: ViewModel with State Management

```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(
    private val getRandomImageUseCase: GetRandomImageUseCase,
    private val evaluateDrawingUseCase: EvaluateDrawingUseCase
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameState>(GameState.Loading)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    fun startSinglePlayer() {
        viewModelScope.launch {
            try {
                _gameState.value = GameState.Loading
                val image = getRandomImageUseCase()
                _gameState.value = GameState.Ready(image, timeRemaining = 30)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Always cleanup here
    }
}

sealed class GameState {
    object Loading : GameState()
    data class Ready(val image: Bitmap, val timeRemaining: Int) : GameState()
    data class Result(val score: Float, val won: Boolean) : GameState()
}
```

---

## 🔐 Environment & Credentials

### Firebase Setup (Required)

1. Create Firebase project at https://console.firebase.google.com
2. Download `google-services.json` and place in `app/`
3. Enable: Authentication, Realtime Database, Storage
4. Create test credentials in `gradle.properties`:
   ```properties
   FIREBASE_DATABASE_URL=https://your-project.firebaseio.com
   FIREBASE_PROJECT_ID=your-project-id
   ```

### Unsplash API Setup (Required)

1. Register at https://unsplash.com/developers
2. Create app and get `accessKey`
3. Add to `local.properties`:
   ```properties
   unsplash_access_key=YOUR_KEY_HERE
   ```
4. Reference in code: `BuildConfig.UNSPLASH_ACCESS_KEY`

### TensorFlow Lite Model (Required)

1. Download pre-trained MobileNet v2 `.tflite` from TensorFlow Hub
2. Place in `app/src/main/assets/models/mobilenet_v2.tflite`
3. Model size: ~14MB (already lightweight for mobile)

---

## 📋 Before Committing Code

**Checklist every agent-generated PR must pass:**

1. ✅ Run `./gradlew clean build ktlint detekt lint test` - must exit with code 0
2. ✅ No ViewModel state in composables (all state from parameters)
3. ✅ All Firebase calls wrapped in try-catch
4. ✅ All Coroutines use viewModelScope
5. ✅ No `!!` null-coalescing operators
6. ✅ All resource cleanup in onCleared()
7. ✅ Strings hardcoded? Move to `strings.xml`
8. ✅ New classes/functions have KDoc comments
9. ✅ Commit message follows: `feat/fix/docs: Brief description`

---

## 🆘 Troubleshooting Common Issues

| Issue                                               | Solution                                                                         |
| --------------------------------------------------- | -------------------------------------------------------------------------------- |
| `Gradle sync failed: Could not determine the class` | Run `./gradlew clean` and resync                                                 |
| `Firebase initialization failed`                    | Verify `google-services.json` in `app/` root                                     |
| `Unsplash API 401 Unauthorized`                     | Check `UNSPLASH_ACCESS_KEY` in `local.properties`                                |
| `TensorFlow model not found`                        | Ensure `mobilenet_v2.tflite` exists in `app/src/main/assets/models/`             |
| `Emulator: Could not launch`                        | Increase RAM allocation: Android Studio → Device Manager → Edit → RAM 4GB+       |
| `Tests timeout`                                     | Increase JVM: `org.gradle.jvmargs=-Xmx4096m` in `gradle.properties`              |
| `Null pointer in drawing canvas`                    | Verify Bitmap initialization before drawing                                      |
| `StateFlow not updating UI`                         | Ensure collecting as `collectAsState()` in Compose, not observing with lifecycle |

---

## 📖 Important Notes for Agent

**Trust these instructions.** Only search the codebase if:

- Information here conflicts with actual code and needs verification
- A new technology/library is introduced that isn't mentioned
- Build steps have changed

**Before exploring**, check:

1. Does this doc answer the question?
2. Is there a code template above?
3. Has a command been documented?

**When adding code**, always:

- Follow the directory structure → place in correct layer
- Use existing patterns (see templates above)
- Run validation commands before committing
- Add comments explaining the "why", not the "what"

**Common agent mistakes to avoid**:

- Don't create state in Composables (it dies on recompose)
- Don't forget to handle errors in viewModelScope launches
- Don't forget to unsubscribe from Firebase listeners
- Don't use `System.out.println()` - use proper logging
- Don't put business logic in UI layer - keep it in domain/usecases
