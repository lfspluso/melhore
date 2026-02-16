# MelhoreApp – Project Context

One-page context for developers and AI-assisted development.

## What the app does

**Melhore** is a routine-improvement Android app with:

- **Reminders:** One-time and recurring (daily, weekly, biweekly, monthly); optional tags and priority. Each reminder can have **checklist items** (sub-tasks) that can be added, removed, and checked off. Reminders have a **status** (ACTIVE, COMPLETED, CANCELLED) and never deactivate themselves; they notify every 30 minutes until manually completed or cancelled (Sprint 13).
- **Reminder list:** Filter by multiple tags, priority, optional date range (e.g. "Próximos 7 dias", "Este mês"), and status (show/hide completed - Sprint 13); sort by due date (default: closest first), priority, title, or creation date (newest/oldest). Advanced filters (tag, priority, date range, group-by, completed filter) hidden behind "Filtros avançados" toggle; sort options always visible. List can be shown **grouped by tag** (sections with "Sem tag" for untagged) or as a **flat list**; the choice is persisted and restored. Completed reminders show low-contrast styling, "MELHORADO" tag, and delete button (Sprint 13). Last-used filter, sort, and group-by view are **persisted** and restored when the app reopens.
- **Notifications:** Local notifications at reminder time (exact alarm; fire even when app was killed); snooze support (Sprint 3). **New snooze options (Sprint 14):** "Fazendo" (1-hour follow-up with completion check), "15 minutos", "1 hora", "Personalizar" (custom duration). **"Fazendo" special flow (Sprint 14):** When user taps "Fazendo", a follow-up notification appears in 1 hour asking "Você estava fazendo {task name}, você completou?" with actions "Sim" (mark complete), "+15 min", "+1 hora", "Personalizar". **30-minute recurring notifications (Sprint 13):** Reminders notify every 30 minutes until marked as COMPLETED or CANCELLED.
- **Completion/Cancellation (Sprint 13-14):** Users can mark reminders as complete (with confirmation modal "Você tem certeza?" or via "Fazendo" follow-up "Sim" action - Sprint 14) or cancel them (from edit screen with confirmation). Completed reminders stop notifying and show "MELHORADO" tag; cancelled reminders show "CANCELADO" tag.
- **Organization:** Tags, priority levels (Low, Medium, High, Urgent).
- **Accounts & Sync:** Google Sign-In authentication; data syncs across devices via Firebase Firestore. Each user's reminders, tags, and checklist items are scoped to their account.
- **Storage:** Local Room database + Firebase Firestore cloud sync. Data persists locally and syncs across devices when signed in.

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3), **Hilt**, **Room**, **AlarmManager** (reminder scheduling), **Firebase Authentication** (Google Sign-In), **Firebase Firestore** (cloud sync), **Kotlin Coroutines + Flow**.

## Where to find what

| Concern | Location |
|--------|----------|
| App entry, theme, navigation | `app/` – `MelhoreApplication`, `MainActivity`, `ui/navigation/`, `ui/theme/` (default dark mode - Sprint 10) |
| Authentication (Google Sign-In) | `core/auth/` – `AuthRepository.kt`, `CurrentUser.kt`, `AuthModule.kt`; `feature/auth/` – `ui/LoginScreen.kt`, `LoginViewModel.kt` (Sprint 14) |
| Cloud sync (Firebase Firestore) | `core/sync/` – `SyncRepository.kt`, `FirestoreSyncService.kt`, `SyncModule.kt` (Sprint 16-17) |
| Boot reschedule (after device reboot) | `app/` – `BootReceiver.kt` (receives BOOT_COMPLETED, calls scheduler to reschedule all reminders) |
| Reminders UI & logic | `feature/reminders/` – `ui/list/` (list + ViewModel with "Filtros avançados" toggle - Sprint 10, next notification date display - Sprint 11.5, completion/cancellation flow - Sprint 13, completed filter - Sprint 13, user-scoped - Sprint 15), `ui/addedit/` (add/edit reminder + checklist; edit via route `reminders/edit/{reminderId}`; cancellation option - Sprint 13; recurrence includes biweekly/monthly/routine - Sprint 11-12) |
| Tags UI & logic (shown as "Tags" in app; Category in code/DB) | `feature/categories/` – `ui/list/`, `ui/addedit/` (user-scoped - Sprint 15) |
| Settings UI | `feature/settings/` – `ui/` (includes delete after completion setting - Sprint 13, Sign out - Sprint 17) |
| Database (entities, DAOs, DB) | `core/database/` – `entity/` (Reminder with ReminderStatus enum - Sprint 13, Category, List, ChecklistItem - all have userId - Sprint 15), `dao/` (all filter by userId), `MelhoreDatabase.kt` (version 3 with migration 2→3 adding status field - Sprint 13), `DatabaseModule.kt` |
| Notifications (channels, posting) | `core/notifications/` – `NotificationHelper.kt` |
| Scheduling (AlarmManager, boot reschedule, snooze) | `core/scheduling/` – `ReminderScheduler.kt`, `ReminderAlarmReceiver.kt` (30-minute recurring notifications - Sprint 13), `SnoozeReceiver.kt`, `RecurrenceHelper.kt` (supports BIWEEKLY, MONTHLY, ROUTINE - Sprint 11-12), `SchedulingContext` (interface); `app/` – `BootReceiver.kt` |
| Shared types (e.g. Result) | `core/common/` – `Result.kt` |
| App preferences (default snooze, filter/sort, etc.) | `core/common/` – `preferences/AppPreferences.kt` (includes showAdvancedFilters - Sprint 10, showCompletedReminders - Sprint 13, deleteAfterCompletion - Sprint 13) |
| Templates (coming-soon placeholder) | `feature/reminders/` – `ui/templates/TemplatesComingSoonScreen.kt`; entry from Reminders screen (app bar icon "Modelos de lembretes") |
| Integrations (share to Telegram, Slack, WhatsApp) | `feature/integrations/` – `ui/IntegrationsScreen.kt`; bottom tab "Integrações" |

## Current sprint

**Sprint 0** through **Sprint 14** are done: app shows reminder list with **multi-tag filter**, **priority filter**, **optional date range** (e.g. Próximos 7 dias, Este mês), **status filter** (show/hide completed), and **sort** by date, priority, title, or creation date; list can be **grouped by tag** or flat (toggle persisted); filter, sort, and group-by **persist** across restarts; priority badge and checklist progress on each item; tap card to edit; add/edit reminder with **checklist** (sub-tasks) and **recurrence options** (daily, weekly, biweekly, monthly); **completion flow** with confirmation modal; **cancellation flow** from edit screen; completed reminders show low-contrast styling and "MELHORADO" tag; reminders notify every 30 minutes until completed or cancelled; Tags CRUD via bottom nav (Reminders, Tags, Integrações, Settings); **Integrações** tab with share to Telegram, Slack, WhatsApp via system share sheet; **Templates** coming-soon screen reachable from Reminders app bar (Modelos de lembretes); notifications at due time with **new snooze options** ("15 minutos", "1 hora", "Personalizar") and "Fazendo" follow-up completion check; Settings screen with default snooze duration and delete after completion (persisted).

**Current sprint:** **Sprint 15** – Snooze options settings; customize which options appear in notifications.

**Next sprints:** **Sprint 12** (Routine type for Melhores), then **Sprint 16+** (Authentication, user scoping, cloud sync, and data migration polish). See [SPRINTS.md](SPRINTS.md) for details.

## Conventions

- **Screens:** `ReminderListScreen`, `CategoryListScreen`, etc.
- **ViewModels:** `ReminderListViewModel`, `AddReminderViewModel`, etc.
- **State/events:** `ReminderListState`, `ReminderListEvent` (or one-shot events in ViewModel).
- New code: follow the layer (UI → Domain → Data) and place in the module/package that owns that concern.

For architecture and data flow, see [ARCHITECTURE.md](ARCHITECTURE.md).  
For sprint goals and test criteria, see [SPRINTS.md](SPRINTS.md).
