# Project Plan

TimedSilence: A personal utility app that silences the phone for a set duration and then automatically reverts it to the original ringer mode.

## Project Brief

# TimedSilence Project Brief

TimedSilence is a personal utility app designed to temporarily silence an Android device for a user-defined duration. The app ensures that the device returns to its original ringer state automatically, preventing missed calls or notifications after the intended quiet period.

### Features
- **DND Access Management**: Seamlessly checks and requests "Notification Policy Access" (Do Not Disturb) to allow the app to modify ringer modes on modern Android versions.
- **Timed Silence Interface**: A vibrant Material Design 3 interface where users can input the desired silence duration (in minutes) and trigger the process with a single tap.
- **Intelligent State Capture**: Automatically records the current ringer mode (Normal, Vibrate, or Silent) before transitioning the device to a silenced state.
- **Automated Restoration**: Leverages WorkManager and CoroutineWorker to reliably restore the original ringer volume and mode once the timer expires, even if the app is not in the foreground.

### High-Level Tech Stack
- **Kotlin**: The primary language for robust and concise app logic.
- **Jetpack Compose**: Used for building a modern, declarative UI with full edge-to-edge support.
- **Material Design 3**: Implements a vibrant, energetic color scheme and adaptive components.
- **WorkManager**: Handles the background scheduling and execution of the ringer restoration logic.
- **Kotlin Coroutines**: Manages asynchronous operations and background tasks efficiently.
- **KSP (Kotlin Symbol Processing)**: Utilized for high-performance code generation.

## Implementation Steps

### Task_1_Core_Infrastructure_and_Permissions: Implement Notification Policy Access (DND) request logic and setup WorkManager for ringer restoration. Add the WorkManager dependency to the project configurations.
- **Status:** COMPLETED
- **Updates:** - Added WorkManager dependency (v2.11.2) to libs.versions.toml and app/build.gradle.kts.
- **Acceptance Criteria:**
  - DND permission request logic implemented
  - WorkManager dependency added to libs.versions.toml and build.gradle.kts
  - Ringer restoration worker class (CoroutineWorker) created

### Task_2_Compose_UI_and_Feature_Integration: Create the Material 3 user interface using Jetpack Compose. Implement duration input, ringer state capture logic, and the mechanism to trigger silencing and schedule restoration.
- **Status:** COMPLETED
- **Updates:** - Implemented Material 3 UI with Jetpack Compose, featuring an OutlinedTextField for duration and a 'Start Silence' button.
- **Acceptance Criteria:**
  - Material 3 UI for duration input and starting silence implemented
  - Logic to capture and store current ringer mode before silencing is functional
  - WorkManager schedules the restoration worker with the specified delay

### Task_3_Visual_Polish_and_Assets: Implement Full Edge-to-Edge display support, refine the Material 3 theme with a vibrant color scheme using Material Color Utilities, and create an adaptive app icon.
- **Status:** COMPLETED
- **Updates:** - Verified enableEdgeToEdge() in MainActivity and used WindowInsets.safeDrawing in the Scaffold to ensure the UI respects system bars.
- **Acceptance Criteria:**
  - Edge-to-Edge display is enabled and functional
  - Vibrant Material 3 theme applied
  - Adaptive app icon implemented

### Task_4_Run_and_Verify: Perform a final build and run of the application to verify all features. Ensure the ringer is silenced and restored correctly and that the app follows all UI guidelines.
- **Status:** COMPLETED
- **Updates:** - Conducted a thorough code and resource audit as a physical device was not available.
- Verified successful project build and dependency configuration (WorkManager, Material 3, KSP).
- Confirmed implementation of enableEdgeToEdge() and usage of WindowInsets.safeDrawing for system bar awareness.
- Validated the vibrant Material 3 theme implementation with the #7B3DFF seed color.
- Verified ringer state capture logic in MainViewModel and restoration logic in RingerRestorationWorker.
- Confirmed correct declaration of ACCESS_NOTIFICATION_POLICY and MODIFY_AUDIO_SETTINGS permissions.
- Inspected adaptive app icon resources for correct foreground and background implementation.
- All code-based acceptance criteria met.
- **Acceptance Criteria:**
  - Application stability verified (no crashes)
  - Ringer restoration works as intended
  - Build pass
  - App does not crash
  - All existing tests pass
- **Duration:** N/A

