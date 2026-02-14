# MelhoreApp – Project Context

One-page context for developers and AI-assisted development.

## What the app does

**Melhore** is a routine-improvement Android app with:

- **Reminders:** One-time and recurring (daily, weekly); optional categories, lists, and priority.
- **Notifications:** Local notifications at reminder time; snooze support.
- **Organization:** Categories, lists, priority levels (Low, Medium, High, Urgent).
- **Storage:** Local only (Room); no backend. May be published later; initially for 1–3 users.

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3), **Hilt**, **Room**, **WorkManager**, **Kotlin Coroutines + Flow**.

## Where to find what

| Concern | Location |
|--------|----------|
| App entry, theme, navigation | `app/` – `MelhoreApplication`, `MainActivity`, `ui/navigation/`, `ui/theme/` |
| Reminders UI & logic | `feature/reminders/` – `ui/list/` (list + ViewModel), `ui/addedit/` (add reminder screen + ViewModel) |
| Categories UI & logic | `feature/categories/` – `ui/` |
| Lists UI & logic | `feature/lists/` – `ui/` |
| Settings UI | `feature/settings/` – `ui/` |
| Database (entities, DAOs, DB) | `core/database/` – `entity/`, `dao/`, `MelhoreDatabase.kt`, `DatabaseModule.kt` |
| Notifications (channels, posting) | `core/notifications/` – `NotificationHelper.kt` |
| Scheduling (WorkManager, workers) | `core/scheduling/` – `ReminderWorker.kt` |
| Shared types (e.g. Result) | `core/common/` – `Result.kt` |

## Current sprint

**Sprint 0** and **Sprint 1** are done: app shows reminder list (empty state or items from Room), FAB opens add-reminder screen; one-time reminders can be created with title, date/time, optional category, list, and priority; data persists in Room.  
Next: **Sprint 2** – Notifications and scheduling (one-time reminder triggers notification at due time).

## Conventions

- **Screens:** `ReminderListScreen`, `CategoryListScreen`, etc.
- **ViewModels:** `ReminderListViewModel`, `AddReminderViewModel`, etc.
- **State/events:** `ReminderListState`, `ReminderListEvent` (or one-shot events in ViewModel).
- New code: follow the layer (UI → Domain → Data) and place in the module/package that owns that concern.

For architecture and data flow, see [ARCHITECTURE.md](ARCHITECTURE.md).  
For sprint goals and test criteria, see [SPRINTS.md](SPRINTS.md).
