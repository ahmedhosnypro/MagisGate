# RootInjector - Application Blueprint

## 1. Overview

RootInjector is a utility app for power-users on rooted Android devices. It provides a clean, fast, and modern interface to grant a permanent "Allow" policy to any installed application within Magisk's root access manager. This eliminates the need for repeated "Grant" prompts for trusted applications.

## 2. Core Features & Logic

*   **Root Verification:** The app immediately checks for root access (`su`) on launch. If root is not found, it displays a clear error screen, as the app's core functionality depends on it.
*   **Application Fetching:** It asynchronously loads a complete list of all user and system applications, including their icon, name, and package name.
*   **On-Demand UID Fetching:** To ensure a fast startup, the User ID (UID) required for the injection command is only fetched when the user selects a specific app. This is done by executing a `dumpsys` shell command for the selected package.
*   **Magisk Policy Injection:** Upon user confirmation, the app executes a root command to directly write a `policy=2` (ALLOW) rule into the Magisk SQLite database (`/data/adb/magisk.db`) for the target app's UID.
*   **State Management:** The application state (app lists, search queries, loading states) is managed reactively using the `flutter_riverpod` package.

## 3. UI/UX Design - "Cyberpunk Power-User" Aesthetic

*   **Theme:** A true-black dark theme (`#000000`) is used for maximum contrast and battery savings on OLED screens. A high-contrast "Electric Purple" is used as the accent color for all interactive elements, creating a striking neon glow effect.
*   **Typography:** The clean, geometric `Space Grotesk` font from Google Fonts is used for its modern and technical feel.
*   **Layout:**
    *   **Search Bar:** A floating, pill-shaped search bar is pinned to the top, allowing for instant filtering of the app list. It features a subtle glow effect.
    *   **App List:** A `ListView.builder` efficiently renders the list of applications. Each item is a custom-styled card with a subtle, dark glassmorphism effect, making it feel layered and premium.
*   **Micro-interactions:**
    *   **Confirmation:** Tapping an app reveals a sleek modal bottom sheet to confirm the injection, preventing accidental changes.
    *   **Feedback:** The confirmation button transforms into a loading spinner during the root command execution. A successful operation is confirmed with both a haptic vibration and a floating `SnackBar`.

## 4. Technical Implementation

*   **Dependencies:**
    *   `flutter_riverpod`: For state management.
    *   `device_apps`: To get the list of installed applications.
    *   `google_fonts`: For the custom `Space Grotesk` font.
    *   `vibration`: To provide haptic feedback.
    *   `path_provider`: (Implicitly used) for shell command access.
*   **Permissions:** Requires `android.permission.QUERY_ALL_PACKAGES` in `AndroidManifest.xml` to fetch the app list on Android 11+.

This blueprint outlines a clear path to creating a visually impressive and functionally robust utility.