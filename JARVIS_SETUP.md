# JARVIS AI — Android starter

This repository now contains the first Kotlin + Jetpack Compose Android application shell.

## Current status

The current screen is a UI prototype. The text field and button demonstrate the interface only; speech recognition, text-to-speech, wake-word detection, cloud AI, local AI, and phone actions are not implemented yet.

## Requirements

- JDK 17
- Android SDK Platform 36
- Gradle 8.13
- Android 8.0+ device (API 26+)

## Build locally

If Gradle 8.13 is installed:

    gradle assembleDebug

APK output: app/build/outputs/apk/debug/app-debug.apk

The GitHub Actions workflow installs Gradle directly, so a Gradle wrapper is not required for CI.

## Planned milestones

1. Runtime microphone permission and speech recognition.
2. Android TextToSpeech.
3. Kannada, English, and Hindi language handling.
4. Local command engine and optional on-device model.
5. Secure cloud AI through a backend (never embed secret API keys in the APK).
6. Permission-compliant wake-word foreground operation.
7. User-approved Android actions, alarms, reminders, and app launching.
