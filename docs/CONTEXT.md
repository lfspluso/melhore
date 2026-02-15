# MelhoreApp – Project Context

One-page context for developers and AI-assisted development.

## What the app does

**Melhore** is a routine-improvement Android app with:

- **Reminders:** One-time and recurring (daily, weekly); optional tags and priority. Each reminder can have **checklist items** (sub-tasks) that can be added, removed, and checked off.
- **Reminder list:** Filter by multiple tags, priority, and optional date range (e.g. "Próximos 7 dias", "Este mês"); sort by due date, priority, title, or creation date (newest/oldest). List can be shown **grouped by tag** (sections with "Sem tag" for untagged) or as a **flat list**; the choice is persisted and restored. Last-used filter, sort, and group-by view are **persisted** and restored when the app reopens.
- **Notifications:** Local notifications at reminder time (exact alarm; fire even when app was killed); snooze support (Sprint 3).
- **Organization:** Tags, priority levels (Low, Medium, High, Urgent).
- **Storage:** Local only (Room); no backend. May be published later; initially for 1–3 users.

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3), **Hilt**, **Room**, **AlarmManager** (reminder scheduling), **Kotlin Coroutines + Flow**.

## Where to find what

| Concern | Location |
|--------|----------|
| App entry, theme, navigation | `app/` – `MelhoreApplication`, `MainActivity`, `ui/navigation/`, `ui/theme/` |
| Boot reschedule (after device reboot) | `app/` – `BootReceiver.kt` (receives BOOT_COMPLETED, calls scheduler to reschedule all reminders) |
| Reminders UI & logic | `feature/reminders/` – `ui/list/` (list + ViewModel, tap card to edit), `ui/addedit/` (add/edit reminder + checklist; edit via route `reminders/edit/{reminderId}`) |
| Tags UI & logic (shown as “Tags” in app; Category in code/DB) | `feature/categories/` – `ui/list/`, `ui/addedit/` |
| Settings UI | `feature/settings/` – `ui/` |
| Database (entities, DAOs, DB) | `core/database/` – `entity/` (Reminder, Category, List, ChecklistItem), `dao/`, `MelhoreDatabase.kt`, `DatabaseModule.kt` |
| Notifications (channels, posting) | `core/notifications/` – `NotificationHelper.kt` |
| Scheduling (AlarmManager, boot reschedule, snooze) | `core/scheduling/` – `ReminderScheduler.kt`, `ReminderAlarmReceiver.kt`, `SnoozeReceiver.kt`, `RecurrenceHelper.kt`, `SchedulingContext` (interface); `app/` – `BootReceiver.kt` |
| Shared types (e.g. Result) | `core/common/` – `Result.kt` |
| App preferences (default snooze, etc.) | `core/common/` – `preferences/AppPreferences.kt` |
| Templates (coming-soon placeholder) | `feature/reminders/` – `ui/templates/TemplatesComingSoonScreen.kt`; entry from Reminders screen (app bar icon "Modelos de lembretes") |
| Integrations (share to Telegram, Slack, WhatsApp) | `feature/integrations/` – `ui/IntegrationsScreen.kt`; bottom tab "Integrações" |

## Current sprint

**Sprint 0** through **Sprint 9** are done: app shows reminder list with **multi-tag filter**, **priority filter**, **optional date range** (e.g. Próximos 7 dias, Este mês), and **sort** by date, priority, title, or creation date; list can be **grouped by tag** or flat (toggle persisted); filter, sort, and group-by **persist** across restarts; priority badge and checklist progress on each item; tap card to edit; add/edit reminder with **checklist** (sub-tasks); Tags CRUD via bottom nav (Reminders, Tags, Integrações, Settings); **Integrações** tab with share to Telegram, Slack, WhatsApp via system share sheet; **Templates** coming-soon screen reachable from Reminders app bar (Modelos de lembretes); notifications at due time with snooze support; Settings screen with default snooze duration (persisted).  
Next: **Sprint 10** – Documentation and release prep.

## Conventions

- **Screens:** `ReminderListScreen`, `CategoryListScreen`, etc.
- **ViewModels:** `ReminderListViewModel`, `AddReminderViewModel`, etc.
- **State/events:** `ReminderListState`, `ReminderListEvent` (or one-shot events in ViewModel).
- New code: follow the layer (UI → Domain → Data) and place in the module/package that owns that concern.

For architecture and data flow, see [ARCHITECTURE.md](ARCHITECTURE.md).  
For sprint goals and test criteria, see [SPRINTS.md](SPRINTS.md).
