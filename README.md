# TimedSilence

### Effortless temporary phone silencing.

**TimedSilence** is a personal utility app designed to temporarily silence your Android device for a user-defined duration. The app ensures that the device returns to its original ringer state automatically, preventing missed calls or notifications after the intended quiet period.

## 🌟 Key Features

*   **Material 3 UI**: A vibrant, energetic interface built with Jetpack Compose, featuring full edge-to-edge support for a modern look and feel.
*   **Smart Minute Picker**: A high-performance, smooth-scrolling wheel to select your silence duration from 1 to 120 minutes.
*   **Mode Choice**: Easily toggle between **Silent** and **Vibrate** modes for the timed period.
*   **Status Notification**: An ongoing notification keeps you informed of the active session, showing the exact time your ringer will be restored.
*   **Reliability**: Original ringer settings (mode and volume) are persisted and restored automatically via **WorkManager**, ensuring restoration even if the app process is killed or the device is restarted.
*   **DND Access**: Seamlessly handles the "Notification Policy Access" required to modify ringer modes on modern Android versions.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material Design 3)
*   **Background Tasks**: WorkManager
*   **Persistence**: SharedPreferences
*   **Asynchrony**: Kotlin Coroutines & Flow
*   **Dependency Injection**: ViewModel with AndroidViewModel

## 🏗️ Architecture

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern:
*   **View**: Jetpack Compose screens that react to state changes.
*   **ViewModel**: Manages UI state, captures ringer settings, and triggers background work.
*   **WorkManager**: Handles the "Source of Truth" for restoration logic, ensuring it survives system constraints.

## 🔄 How it Works

1.  **Capture**: When you start a session, the app captures your current ringer mode and volume level.
2.  **Silence**: The device is set to your chosen target mode (Silent or Vibrate).
3.  **Schedule**: A `RingerRestorationWorker` is scheduled via WorkManager with a delay matching your selected duration.
4.  **Notify**: An ongoing notification is displayed showing the end time.
5.  **Restore**: Once the timer expires, the worker restores the original captured state and cleans up notifications and stored data.

## 🔐 Permissions

To function correctly, TimedSilence requires:
*   **Notification Policy Access (DND)**: To change the ringer mode.
*   **Post Notifications**: To show the active session status (Android 13+).

---
*Developed with a focus on Material Design 3 principles and high-performance Compose UI.*
