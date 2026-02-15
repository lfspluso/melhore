# MelhoreApp – Project Context

One-page context for developers and AI-assisted development.

## What the app does

**Melhore** is a routine-improvement Android app with:

- **Reminders:** One-time and recurring (daily, weekly); optional categories, lists, and priority. Each reminder can have **checklist items** (sub-tasks) that can be added, removed, and checked off.
- **Notifications:** Local notifications at reminder time (exact alarm; fire even when app was killed); snooze support (Sprint 3).
- **Organization:** Categories, lists, priority levels (Low, Medium, High, Urgent).
- **Storage:** Local only (Room); no backend. May be published later; initially for 1–3 users.

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3), **Hilt**, **Room**, **AlarmManager** (reminder scheduling), **Kotlin Coroutines + Flow**.

## Where to find what

| Concern | Location |
|--------|----------|
| App entry, theme, navigation | `app/` – `MelhoreApplication`, `MainActivity`, `ui/navigation/`, `ui/theme/` |
| Boot reschedule (after device reboot) | `app/` – `BootReceiver.kt` (receives BOOT_COMPLETED, calls scheduler to reschedule all reminders) |
| Reminders UI & logic | `feature/reminders/` – `ui/list/` (list + ViewModel, tap card to edit), `ui/addedit/` (add/edit reminder + checklist; edit via route `reminders/edit/{reminderId}`) |
| Categories UI & logic | `feature/categories/` – `ui/list/`, `ui/addedit/` |
| Lists UI & logic | `feature/lists/` – `ui/list/`, `ui/addedit/` |
| Settings UI | `feature/settings/` – `ui/` |
| Database (entities, DAOs, DB) | `core/database/` – `entity/` (Reminder, Category, List, ChecklistItem), `dao/`, `MelhoreDatabase.kt`, `DatabaseModule.kt` |
| Notifications (channels, posting) | `core/notifications/` – `NotificationHelper.kt` |
| Scheduling (AlarmManager, boot reschedule, snooze) | `core/scheduling/` – `ReminderScheduler.kt`, `ReminderAlarmReceiver.kt`, `SnoozeReceiver.kt`, `RecurrenceHelper.kt`, `SchedulingContext` (interface); `app/` – `BootReceiver.kt` |
| Shared types (e.g. Result) | `core/common/` – `Result.kt` |
| App preferences (default snooze, etc.) | `core/common/` – `preferences/AppPreferences.kt` |

## Current sprint

**Sprint 0** through **Sprint 5.5** are done: app shows reminder list (filter by list/category, sort by date or priority, priority badge and checklist progress on each item; tap card to edit), add/edit reminder with **checklist** (sub-tasks), categories and lists CRUD via bottom nav, notifications at due time with snooze support, Settings screen with default snooze duration (persisted).  
Next: **Sprint 6** – Documentation and release prep.

## Conventions

- **Screens:** `ReminderListScreen`, `CategoryListScreen`, etc.
- **ViewModels:** `ReminderListViewModel`, `AddReminderViewModel`, etc.
- **State/events:** `ReminderListState`, `ReminderListEvent` (or one-shot events in ViewModel).
- New code: follow the layer (UI → Domain → Data) and place in the module/package that owns that concern.

For architecture and data flow, see [ARCHITECTURE.md](ARCHITECTURE.md).  
For sprint goals and test criteria, see [SPRINTS.md](SPRINTS.md).
