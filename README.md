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
   - Add the **Debug signing certificate SHA-1** (and optionally **SHA-256**) from `lukuman-debug.keystore` in the repository root (required for Google Sign-In):
     - **SHA-1**: `3B:56:10:11:F1:7D:1C:EC:0E:D2:9E:2C:B3:00:04:4E:4B:C8:09:BA`
     - **SHA-256**: `2B:79:EB:4B:51:CB:D9:17:AC:70:D9:FD:A2:37:2B:64:C4:81:B9:79:19:26:A5:25:0F:CB:16:B5:01:F2:2E:0E`
     - Or inspect locally / in CI logs via:
       ```bash
       keytool -list -v -keystore lukuman-debug.keystore -alias lukumandebug -storepass android -keypass android -storetype PKCS12
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

The generated APK is signed with `lukuman-debug.keystore` and output to:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions (`.github/workflows/build-apk.yml`)
- Push to `main` or `master` (or trigger **Build Android Debug APK** manually via `workflow_dispatch`).
- Store the contents of `app/google-services.json` in a GitHub Repository Secret named `GOOGLE_SERVICES_JSON` so CI builds your APK wired directly to your live Firebase project.
- The workflow prints the `lukuman-debug.keystore` SHA-1 and SHA-256 in the logs, uploads `Lukuman-Free-Chat.apk` as an artifact, and publishes a **GitHub Release** with a direct download link for `Lukuman-Free-Chat.apk`.

---

## Firestore Security Rules Note

This project includes hardened security rules in `firestore.rules` (isolated user PII in `/users/{userId}`, non-PII public lookup in `/user_directory/{userId}` and `/email_lookup/{emailHash}`, and participant-locked 1-to-1 chats in `/chats/{chatId}`). Please review and test `firestore.rules` in your Firebase Console before sharing broadly.
