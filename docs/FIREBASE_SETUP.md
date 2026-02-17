# Firebase and OAuth setup for Google Sign-In

This guide walks you through configuring Firebase and OAuth so that **Google Sign-In** works in MelhoreApp.

---

## 1. Prerequisites

- A **Google account**
- **Android Studio** with the project open (you will need the **SHA-1** of your signing key)
- Package name used by the app: **`com.melhoreapp`** (from `app/build.gradle.kts`)

---

## 2. Create or use a Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing project).
3. Follow the steps: name the project (e.g. "MelhoreApp"), enable/disable Google Analytics as you prefer, then create the project.

---

## 3. Add the Android app and get `google-services.json`

1. In the Firebase project, click the **Android** icon (or **Add app** → **Android**).
2. **Android package name:** enter **`com.melhoreapp`** (must match `applicationId` in `app/build.gradle.kts`).
3. **App nickname** (optional): e.g. "MelhoreApp".
4. **Debug signing certificate SHA-1** (required for Google Sign-In on debug builds):
   - In Android Studio: open **Gradle** tool window → **MelhoreApp** → **app** → **Tasks** → **android** → run **signingReport**.
   - Or in a terminal at the project root:
     - Windows: `m`
     - macOS/Linux: `./gradlew signingReport`
   - Copy the **SHA-1** from the **debug** variant (e.g. `SHA1: AB:CD:...`) and paste it into the Firebase form.
5. Click **Register app**.
6. Download **`google-services.json`** and place it in the **`app/`** directory of MelhoreApp (same folder as `app/build.gradle.kts`), **replacing** the placeholder file if present.
7. Click **Next** through the remaining steps (the project already has the Gradle plugin and dependencies).

---

## 4. Enable Google Sign-In in Firebase Authentication

1. In the Firebase Console, go to **Build** → **Authentication**.
2. Open the **Sign-in method** tab.
3. Click **Google** in the list of providers.
4. Turn **Enable** on.
5. Set **Project support email** to your email.
6. Click **Save**.

---

## 5. Get the Web client ID (OAuth 2.0)

Google Sign-In on Android needs a **Web application** OAuth 2.0 client ID (used to get an ID token for Firebase Auth). Firebase creates this when you enable Google sign-in; you just need to copy it.

1. In the Firebase Console, go to **Project settings** (gear icon) → **General**.
2. Scroll to **Your apps**.
3. Under your **Android** app, find the section **"Web API Key"** or scroll until you see **OAuth 2.0** / client IDs.
4. Alternatively, open [Google Cloud Console](https://console.cloud.google.com/), select the **same project** as your Firebase project (project name at the top).
5. Go to **APIs & Services** → **Credentials**.
6. In **OAuth 2.0 Client IDs** you should see:
   - An **Android** client (e.g. "Web client (auto created by Google Service)") – do **not** use this for `default_web_client_id`.
   - A **Web application** client (e.g. "Web client (auto created by Google Service)") – **this** is the one you need.
7. Click the **Web application** client and copy its **Client ID** (looks like `123456789-xxxx.apps.googleusercontent.com`).

---

## 6. Put the Web client ID in the app

1. Open **`app/src/main/res/values/strings.xml`** in MelhoreApp.
2. Replace the placeholder value of **`default_web_client_id`** with the **Web application** Client ID you copied:

```xml
<string name="default_web_client_id" translatable="false">123456789-xxxxxxxxxxxxx.apps.googleusercontent.com</string>
```

3. Save the file.

**Important:** Use the **Web application** client ID, not the Android client ID. The app uses this to request an ID token that Firebase Authentication accepts.

---

## 7. Firestore (for cloud sync)

The app syncs reminders, tags, and checklist items to Firestore (Sprint 18). **Both** steps below are required or you will see "database does not exist" or "Cloud Firestore API has not been used... or it is disabled" in logs and nothing will appear in Firestore.

### 7.1 Create the Firestore database

1. In the [Firebase Console](https://console.firebase.google.com/), select your project (e.g. **melhoreapp**).
2. Go to **Build** → **Firestore Database**.
3. Click **Create database**.
4. Choose **Start in test mode** for quick setup (you can switch to production rules later), or **Production mode** and add the rules below.
5. Select a **location** (e.g. `us-central1`) and click **Enable**. Wait until the database is created.

### 7.2 Enable the Cloud Firestore API

The Firestore API must be enabled in Google Cloud for your project:

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select the **same project** as your Firebase project (check the project name at the top).
2. Go to **APIs & Services** → **Library** (or open: [Firestore API page](https://console.developers.google.com/apis/api/firestore.googleapis.com/overview)).
3. Search for **Cloud Firestore API**.
4. Click it and click **Enable**. If it already says "Manage", the API is already enabled.

After both steps, wait a minute if you just enabled the API, then run the app again. Data should start syncing to Firestore.

**Security rules (recommended for production):** In Firebase Console → Firestore Database → **Rules**, restrict access so each user can only read/write their own data:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

This allows authenticated users to read and write only under `users/{their-uid}/...` (reminders, categories, checklistItems).

### 7.3 Why is Firestore not populated?

Firestore only gets data when **all** of the following are true:

1. **Firestore database exists** (section 7.1). If it doesn’t, you’ll see `The database (default) does not exist` in logcat and nothing will be written.
2. **Cloud Firestore API is enabled** (section 7.2). If it isn’t, you’ll see `Cloud Firestore API has not been used... or it is disabled` and writes will fail.
3. **You are signed in** in the app. Sync runs only for signed-in users. If you see the **login screen** ("Entrar com Google") and never reach the main app (reminders list), you are not signed in, so no sync runs and Firestore stays empty.
4. **Sync implementation:** The app must wait for Firestore operations to finish (not only the first Flow emission). If you see "Upload complete" or "syncAll complete" in logcat but no documents under `users/{uid}/reminders` (or categories/checklistItems), this was previously caused by collecting only the first Flow value (Loading) and cancelling before the actual writes; the fix is to wait for the terminal result (Success/Error).

**About DEVELOPER_ERROR / "Unknown calling package name 'com.google.android.gms'":** Those log messages come from **Google Play Services**, not from Firestore. They do **not** by themselves prevent Firestore from being written to. However, if they cause **Google Sign-In to fail**, then you never get to a signed-in state, so (3) above fails and Firestore stays empty. So: fix Sign-In (e.g. SHA-1, emulator with Google Play, or use a physical device) so you can reach the main app; then ensure (1) and (2) are done so that once signed in, data can sync to Firestore.

**Quick check:** In the app, do you see the **reminders list** (Melhores) and can you add a reminder? If yes, you’re signed in—then Firestore is empty only if the database wasn’t created or the API wasn’t enabled. If you’re stuck on the login screen, fix Sign-In first (section 10.1).

### 7.4 Troubleshooting: log messages and fixes

| Log message | Cause | Fix |
|-------------|--------|-----|
| `The database (default) does not exist for project melhoreapp` | Firestore database was never created. | Do **section 7.1** above: Firebase Console → Build → Firestore Database → **Create database** → choose mode and location → Enable. |
| `Cloud Firestore API has not been used in project melhoreapp before or it is disabled` | The Firestore API is off for the Google Cloud project. | Do **section 7.2** above: [Enable Cloud Firestore API](https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=melhoreapp) (replace `melhoreapp` with your project ID if different). |
| `DEVELOPER_ERROR` / `Unknown calling package name 'com.google.android.gms'` | From Google Play Services; can block **Sign-In**. | If you can’t sign in, see **section 10.1**. If you are signed in and reminders work, these logs are often harmless; Firestore being empty is then due to (1) or (2) above. |
| Data still not in Firestore after creating DB and enabling API | Not signed in, or propagation delay. | You must be **signed in** (main app visible). Wait 2–5 minutes after enabling the API, then add a reminder or tag and check Firestore under `users/{your-uid}/reminders` or `.../categories`. |
| "Upload complete" / "syncAll complete" in logcat but no data in Firestore | Sync was using `Flow.first()` so only Loading was received and the flow was cancelled before Firestore writes. | Use `.first { it !is Result.Loading }` in SyncRepository when collecting upload/download Flows (already fixed in codebase; this row is for reference). |

---

## 8. Summary checklist

| Step | What | Where |
|------|------|--------|
| 1 | Firebase project | [Firebase Console](https://console.firebase.google.com/) |
| 2 | Android app registered (package `com.melhoreapp`) | Firebase Console → Project settings → Your apps |
| 3 | Debug SHA-1 added | Firebase Console → Android app config |
| 4 | `google-services.json` | **`app/google-services.json`** (replace placeholder) |
| 5 | Google Sign-In enabled | Firebase Console → Authentication → Sign-in method → Google |
| 6 | Firestore database created | Firebase Console → Firestore Database (see section 7) |
| 7 | Web client ID | Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 → **Web application** client |
| 8 | `default_web_client_id` | **`app/src/main/res/values/strings.xml`** |

---

## 9. Release builds (optional)

For **release** builds (e.g. Play Store), add your **release** signing certificate SHA-1 in Firebase:

1. Firebase Console → Project settings → Your apps → your Android app.
2. Click **Add fingerprint**, paste the **release** SHA-1, save.

Get the release SHA-1 from your keystore (e.g. `keytool -list -v -keystore your-release-key.keystore`).

---

## 10. Verify

1. Sync Gradle and build: `gradlew.bat assembleDebug` (or **Build → Make Project**).
2. Run the app on a device or emulator.
3. You should see the **login screen** with **"Entrar com Google"**.
4. Tap it: the **Google account picker** (or browser) should appear.
5. After choosing an account, you should be signed in and see the **main app** (Melhores list).

If you see errors such as **"Developer Error"** or **"10"** in the UI, see the checklist above. If you see **DEVELOPER_ERROR** or **Unknown calling package name 'com.google.android.gms'** in logcat, see the next section.

### 10.1 Fix: DEVELOPER_ERROR / Unknown calling package name 'com.google.android.gms'

These log messages usually mean Google Play Services (or the emulator) cannot verify your app. Try the following in order:

**1. Use an emulator with Google Play (not only "Google APIs")**

- In Android Studio: **Device Manager** → Create Device (or edit existing) → pick a system image that includes **"Google Play"** in the name (e.g. "Tiramisu" with "Google Play"), not "Google APIs" only.
- Images with **Google Play** have proper Play Services and reduce this error. Recreate the AVD if needed and run the app again.

**2. Add your debug SHA-1 in Firebase**

- In a terminal at the project root, run:  
  **Windows:** `gradlew.bat signingReport`  
  **macOS/Linux:** `./gradlew signingReport`
- Copy the **SHA-1** for the **debug** variant (e.g. `SHA1: AB:CD:EF:...`).
- In [Firebase Console](https://console.firebase.google.com/) → your project → **Project settings** (gear) → **Your apps** → select your **Android** app (`com.melhoreapp`).
- Under **SHA certificate fingerprints**, click **Add fingerprint**, paste the SHA-1, save.
- Rebuild and reinstall the app, then try again.

**3. Try a physical device**

- Install the app on a **real Android device** signed in with a Google account. If Sign-In and Firestore work there, the issue is likely the emulator / Play Services image.

**4. Confirm Web client ID**

- In `app/src/main/res/values/strings.xml`, `default_web_client_id` must be the **Web application** OAuth 2.0 client ID from Google Cloud (same project as Firebase), not the Android client ID. See section 5.

**Note:** The "Unknown calling package name" and "Phenotype.API is not available" messages can still appear occasionally on some emulators even when Sign-In and Firestore work. If you can sign in and data syncs to Firestore, you can ignore those logs.

---

## 11. References

- [Firebase: Add Firebase to your Android project](https://firebase.google.com/docs/android/setup)
- [Firebase: Authenticate with Google on Android](https://firebase.google.com/docs/auth/android/google-signin)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
