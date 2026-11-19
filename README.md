# Lukuman Free Chat

An original **deep-blue** mobile chat application built with **Kotlin** and **Jetpack Compose**, featuring bilingual **Hausa (default)** and **English** support, Google Sign-In, real-time 1-to-1 Firestore messaging, and strict Firestore security rules.

> **Created by Lukuman Muhammad Abubakar Fagge**

---

## Version 1 Features

1. **Google Sign-In (Firebase Auth)** with language selector (`Hausa` default / `English`) directly on the login screen.
2. **Profile**: Displays user name and photo with language toggle and sign-out.
3. **One-to-One Real-Time Text Chat (Cloud Firestore)**: Instant message delivery with server timestamps.
4. **Chat List**: Shows each conversation's contact photo, name, last message preview, and time.
5. **Find Users**: Search registered users by **name** or **email** to start a 1-to-1 chat.
6. **Strict Firestore Security Rules** (`firestore.rules`): Users can only read and write chats in which they are one of the two participants.

---

## How to Put `google-services.json` in `app/`

Before running the app with your own Firebase project (or signing in with Google on a real Android device), place your Firebase configuration file in `app/google-services.json`:

1. **Open Firebase Console**: Go to [https://console.firebase.google.com/](https://console.firebase.google.com/) and open your project (or create a new Firebase project).
2. **Register the Android App**:
   - Click **Add app** -> choose the **Android** icon.
   - Enter Android package name: `com.lukuman.freechat`
   - Enter App nickname: `Lukuman Free Chat`
   - Add your **Debug signing certificate SHA-1** (required for Google Sign-In). You can obtain it locally by running:
     ```bash
     ./gradlew signingReport
     ```
     or on Linux/macOS:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Click **Register app**.
3. **Download `google-services.json`**:
   - Click **Download google-services.json**.
4. **Place the file inside `app/`**:
   - Move or copy the downloaded file directly to:
     ```text
     app/google-services.json
     ```
   - Verify the path is `app/google-services.json` (next to `app/build.gradle.kts`, **not** inside `app/src/` or the project root).
5. **Enable Firebase Authentication & Firestore**:
   - In Firebase Console -> **Authentication** -> **Sign-in method** -> enable **Google** and select a support email.
   - In Firebase Console -> **Firestore Database** -> create database -> deploy the rules from `firestore.rules` in this repository.

---

## Building the Debug APK

### Locally
Ensure JDK 17 and Android SDK (API 35) are installed, `app/google-services.json` is in place, and run:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

The generated APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions (`.github/workflows/build-apk.yml`)
- Push to `main` or `master` (or trigger **Build Android Debug APK** manually via `workflow_dispatch`).
- Optionally store the contents of `app/google-services.json` in a GitHub Repository Secret named `GOOGLE_SERVICES_JSON` so CI builds your APK wired directly to your live Firebase project.
- Download `lukuman-free-chat-debug-apk` from the workflow **Artifacts** section.

---

## Firestore Security Rules Note

This project includes hardened security rules in `firestore.rules` (isolated user PII in `/users/{userId}`, non-PII public lookup in `/user_directory/{userId}` and `/email_lookup/{emailHash}`, and participant-locked 1-to-1 chats in `/chats/{chatId}`). Please review and test `firestore.rules` in your Firebase Console before sharing broadly.
