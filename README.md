# MelhoreApp

Routine-improvement Android app: one-time and recurring reminders, notifications, tags, priority levels, and snooze. Data is stored locally (Room). Simple Jetpack Compose UI and background scheduling (AlarmManager for reminders).

## Requirements

- **Android Studio** (Ladybug or newer recommended) or compatible IDE
- **JDK 17**
- **Android SDK:** compileSdk 34, minSdk 26, targetSdk 34

## Getting started

1. **Open the project** in Android Studio (or open the root folder containing `build.gradle.kts` and `settings.gradle.kts`).
2. **Sync Gradle.** If the Gradle wrapper is missing, run from the project root:
   - With Gradle installed: `gradle wrapper --gradle-version=8.7`
   - Or let Android Studio use its embedded Gradle and sync.
3. **Build:** Run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows), or use **Build → Make Project** in the IDE. If the terminal says `java` is not recognized, set `JAVA_HOME` to your JDK (e.g. Android Studio’s `jbr`: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` in PowerShell) or add the JDK `bin` folder to your PATH.
4. **Run:** Select a device or emulator and run the **app** configuration. On first launch you see the Reminders list (empty or with saved reminders); use the + FAB to add a one-time reminder.

### Firebase (Google Sign-In)

To use Google Sign-In you must:

1. Add **`google-services.json`** from the Firebase Console into the **`app/`** directory.
2. Set the **Web client ID** in **`app/src/main/res/values/strings.xml`** (see `default_web_client_id`).

**Full step-by-step:** [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) — Firebase project, Android app registration, SHA-1, enabling Google Sign-In, and where to get the OAuth Web client ID.

## Project structure

- **app** – Application class, MainActivity, theme, navigation, app icon (adaptive icon since Sprint 21.3; depends on all features and core).
- **core/common** – Shared utilities and types (e.g. `Result`).
- **core/auth** – Google Sign-In and user session (AuthRepository, Hilt module).
- **core/database** – Room database, entities, DAOs, Hilt module.
- **core/notifications** – Notification channels and posting.
- **core/scheduling** – AlarmManager for reminder scheduling; ReminderWorker (WorkManager) also available.
- **feature/reminders** – Reminder list, add/edit, detail (Compose).
- **feature/auth** – Login screen (Google Sign-In).
- **feature/categories** – Tags (CRUD and list; shown as "Tags" in the app).
- **feature/settings** – App/settings screens.

## Documentation

- **[docs/CONTEXT.md](docs/CONTEXT.md)** – One-page project context and “where to find what.”
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** – Architecture, data flow, and main decisions.
- **[docs/SPRINTS.md](docs/SPRINTS.md)** – Sprint plan and test criteria for each sprint.
- **[docs/TESTING.md](docs/TESTING.md)** – How to test and validate each step (sprint) with expected behaviour.
- **[docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md)** – Firebase and OAuth setup for Google Sign-In (step-by-step).
- **[docs/DATABASE.md](docs/DATABASE.md)** – Database schema, entities, indexes, and current architecture.
- **[docs/DATABASE_ROADMAP.md](docs/DATABASE_ROADMAP.md)** – Post-launch database improvement roadmap and migration plans.

This project uses **Cursor rules** (`.cursor/rules/`) for AI-assisted development; the rules point to [docs/CONTEXT.md](docs/CONTEXT.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for context. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines; [CHANGELOG.md](CHANGELOG.md) for version/sprint history.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Hilt, Room, AlarmManager (reminder scheduling), Coroutines, Flow.

## License

Private use; may be published later.
