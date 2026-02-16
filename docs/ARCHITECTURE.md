# MelhoreApp – Architecture

## Overview

The app uses a **layered architecture** with **MVVM** at the UI layer:

- **UI layer:** Compose screens and ViewModels; displays data and handles user input.
- **Domain layer:** Use cases (optional; can live inside feature modules for now).
- **Data layer:** Repositories (or direct DAO usage), Room database, and scheduling (AlarmManager for one-time reminders).

The UI layer does not talk to the database or scheduling directly; it goes through ViewModels and use cases/repositories.

## Data flow

```mermaid
flowchart LR
  subgraph ui [UI Layer]
    Screen[Compose Screens]
    VM[ViewModels]
  end
  subgraph domain [Domain]
    UC[Use Cases]
  end
  subgraph data [Data Layer]
    Repo[Repositories]
    Room[Room DB]
    Sched[Scheduling]
  end
  Screen --> VM
  VM --> UC
  UC --> Repo
  Repo --> Room
  Repo --> Sched
  Sched --> Notif[Notifications]
```

### Create reminder and notify (simplified)

```mermaid
sequenceDiagram
  participant User
  participant Screen
  participant VM as ViewModel
  participant Scheduler as ReminderScheduler
  participant DB as Room DB
  participant System as Android System
  participant Receiver as ReminderAlarmReceiver
  participant Notif as NotificationHelper

  User->>Screen: Save reminder
  Screen->>VM: save()
  VM->>DB: insert reminder
  VM->>Scheduler: schedule(reminder)
  Scheduler->>System: setExactAndAllowWhileIdle
  Note over System: At due time
  System->>Receiver: onReceive
  Receiver->>DB: get reminder
  Receiver->>Notif: show notification
  Receiver->>DB: update dueAt if recurring
  Receiver->>Scheduler: schedule next if recurring
```

### Snooze flow (simplified)

```mermaid
sequenceDiagram
  participant User
  participant Notif as Notification
  participant SnoozeRec as SnoozeReceiver
  participant DB as Room DB
  participant Scheduler as ReminderScheduler
  participant System as Android System
  participant AlarmRec as ReminderAlarmReceiver

  User->>Notif: Tap snooze e.g. 15 min
  Notif->>SnoozeRec: onReceive duration
  SnoozeRec->>DB: set snoozedUntil = now + 15 min
  SnoozeRec->>Scheduler: schedule alarm at snoozedUntil
  Scheduler->>System: setExactAndAllowWhileIdle
  Note over System: At snoozedUntil
  System->>AlarmRec: onReceive isSnoozeFire
  AlarmRec->>DB: clear snoozedUntil
  AlarmRec->>Notif: show notification
  AlarmRec->>Scheduler: schedule next if recurring
```

### "Fazendo" snooze flow (Sprint 14)

```mermaid
sequenceDiagram
  participant User
  participant Notif as Notification
  participant SnoozeRec as SnoozeReceiver
  participant DB as Room DB
  participant Scheduler as ReminderScheduler
  participant System as Android System
  participant AlarmRec as ReminderAlarmReceiver
  participant CompCheck as CompletionCheckReceiver

  User->>Notif: Tap "Fazendo"
  Notif->>SnoozeRec: onReceive EXTRA_IS_FAZENDO=true
  SnoozeRec->>Scheduler: schedule follow-up in 1 hour (isFazendoFollowup=true)
  Scheduler->>System: setExactAndAllowWhileIdle (1 hour)
  Note over System: After 1 hour
  System->>AlarmRec: onReceive isFazendoFollowup=true
  AlarmRec->>Notif: show "Você estava fazendo {title}, você completou?"
  Note over Notif: Actions: "Sim", "+15 min", "+1 hora", "Personalizar"
  User->>Notif: Tap "Sim"
  Notif->>CompCheck: onReceive ACTION_COMPLETE
  CompCheck->>DB: set status = COMPLETED
  CompCheck->>Scheduler: cancel all alarms
  User->>Notif: Tap "+15 min" (alternative)
  Notif->>CompCheck: onReceive ACTION_SNOOZE (15 min)
  CompCheck->>DB: set snoozedUntil = now + 15 min
  CompCheck->>Scheduler: schedule alarm at snoozedUntil
```

## Module dependencies

- **app** → all feature modules, all core modules.
- **feature:reminders** → core:common, core:database, core:scheduling, core:auth.
- **feature:categories** (Tags in UI) → core:common, core:database, core:auth.
- **feature:auth** → core:common, core:auth.
- **feature:integrations** → core:common.
- **feature:settings** → core:common, core:notifications, core:auth.
- **core:scheduling** → core:common, core:notifications, core:database.
- **core:sync** → core:common, core:database, Firebase Firestore.
- **core:auth** → core:common, Firebase Auth.
- **core:database** → core:common; provides Room DB and DAOs via Hilt.
- **core:notifications** → core:common.
- **core:common** → no internal app modules.

## Key decisions

- **WorkManager vs AlarmManager:** One-time and recurring reminders use **AlarmManager** (`setExactAndAllowWhileIdle`) so notifications fire at due time even when the app is killed. Requires `SCHEDULE_EXACT_ALARM` (API 31+); on Android 14+ this permission may need to be granted in settings. Recurring reminders reschedule on each fire: when an alarm fires, `ReminderAlarmReceiver` shows the notification, then if the reminder is DAILY/WEEKLY it computes the next occurrence, updates `dueAt` in the DB, and schedules the next alarm. Exact alarms have minor battery impact; Google Play allows them for calendar/alarm/reminder use cases.
- **Database:** Room in **core:database**; single `MelhoreDatabase` with Reminder, Category, List, and **ChecklistItem** entities; provided via Hilt `DatabaseModule` in the same module. Reminders can have **checklist items** (one-to-many); `ChecklistItemDao` provides getItemsByReminderId, insert/update/delete, and getAllItems for list progress. **ReminderStatus (Sprint 13):** Reminders have a `status` field (`ReminderStatus` enum: ACTIVE, COMPLETED, CANCELLED) instead of just `isActive` boolean; database migration 2→3 adds `status` column (existing records default to ACTIVE).
- **Notifications:** Channels created in `Application.onCreate` via `NotificationHelper.createChannels()`; reminders use channel `reminders`; snoozed reminders can use the same or `snoozed` channel. Reminder notifications include **snooze actions** (Sprint 14: "15 minutos", "1 hora", "Personalizar")—the user chooses one of these three. Android limits notifications to 3 visible actions. When the intent has no duration extra, `SnoozeReceiver` uses the **default snooze duration** from `AppPreferences` (core:common), which the user can set in Settings. `SnoozeReceiver` (core:scheduling) receives the chosen duration via intent extra. **Snooze options settings (Sprint 15):** User can enable/disable snooze options in Settings; `ReminderAlarmReceiver` reads enabled options from `AppPreferences` and only shows enabled actions. **"Fazendo" special flow (Sprint 14):** "Fazendo" is not shown in regular notifications due to Android's 3-action limit, but the follow-up flow remains available. When "Fazendo" follow-up fires (`isFazendoFollowup=true`), `ReminderAlarmReceiver` shows a completion check notification: "Você estava fazendo {title}, você completou?" with actions "Sim" (mark complete), "+15 min", "+1 hora", "Personalizar". `CompletionCheckReceiver` handles these actions: "Sim" marks reminder as COMPLETED and cancels alarms; other actions snooze with respective durations.
- **Scheduling:** Reminders are scheduled via `ReminderScheduler` in **core:scheduling** using AlarmManager. At trigger time the system delivers to `ReminderAlarmReceiver`, which shows the notification (with snooze actions), then for recurring reminders advances `dueAt` and reschedules the next alarm. **Snooze flow (Sprint 14):** User taps a snooze option ("Fazendo", "15 min", "1 hora", "Personalizar") on the notification → `SnoozeReceiver` handles the action. For "Fazendo", it schedules a special follow-up notification in 1 hour with `isFazendoFollowup=true`. For other options, it sets `snoozedUntil = now + chosen duration`, updates DB, schedules one alarm at `snoozedUntil` with `isSnoozeFire=true`. When that alarm fires, the receiver clears `snoozedUntil` and, if recurring, schedules the next occurrence. **"Fazendo" follow-up flow (Sprint 14):** When follow-up fires (`isFazendoFollowup=true`), `ReminderAlarmReceiver` shows completion check notification with message "Você estava fazendo {title}, você completou?" and actions handled by `CompletionCheckReceiver`. **30-minute recurring notifications (Sprint 13):** After showing a notification, if reminder status is ACTIVE, `ReminderAlarmReceiver` schedules the next alarm in 30 minutes. This continues until the reminder is marked as COMPLETED or CANCELLED. For recurring reminders, both the 30-minute reminders and the next occurrence (daily/weekly/etc.) are scheduled. **Completion/Cancellation flow (Sprint 13):** Users can mark reminders as COMPLETED (with confirmation modal or via "Fazendo" follow-up "Sim" action - Sprint 14) or CANCELLED (from edit screen with confirmation). When marked as COMPLETED or CANCELLED, all scheduled alarms are cancelled and no more notifications are sent. **Boot reschedule:** `BootReceiver` (app) runs on BOOT_COMPLETED and calls `rescheduleAllUpcomingReminders()`: loads all ACTIVE reminders, for each computes trigger time (snoozedUntil if set and in future, else dueAt; for recurring with past dueAt advances to next occurrence and updates DB), then schedules. `SchedulingContext` (interface) is implemented by the Application so receivers can access database and scheduler without Hilt.
- **Notification permission:** On API 33+ (Android 13), `POST_NOTIFICATIONS` is requested at runtime from the app layer (MainActivity) when the main UI is shown.
- **App preferences:** App-level settings (e.g. default snooze duration) are stored in **core:common** via `AppPreferences` (SharedPreferences, name `melhore_app_prefs`). **feature:settings** writes preferences from the Settings screen; **core:scheduling** (e.g. `SnoozeReceiver`) reads them when the process runs (receivers use `AppPreferences(context.applicationContext)`; no Hilt in receivers). **Reminder list filter, sort, and group-by** (Sprint 6–7): last-used filter (category IDs, priority ordinals, date range from/to), sort order, and "group by tag" toggle are stored in `AppPreferences` and restored in **ReminderListViewModel** on load; the ViewModel persists them whenever the user changes them. **Show completed reminders filter (Sprint 13):** Preference to show/hide completed reminders in the list; persisted and restored on app start. **Delete after completion (Sprint 13, reworked from Sprint 11.5):** Setting to automatically delete COMPLETED reminders (not just notified ones); when enabled, all existing COMPLETED reminders are deleted immediately, and newly completed reminders are deleted when marked as complete. **Snooze options settings (Sprint 15):** Preference to enable/disable snooze options ("15_min", "1_hour", "personalizar") shown in reminder notifications; stored in `AppPreferences`; `ReminderAlarmReceiver` reads enabled options and only shows enabled actions; at least one option must be enabled (validation in SettingsViewModel).
- **Integrations:** The **Integrações** tab (feature:integrations) uses the **system share sheet** (Intent.ACTION_SEND) to send reminder or message text to Telegram, Slack, WhatsApp, or any app that accepts text. The user taps "Enviar para…" on a card; the app launches `Intent.createChooser` with a default message (e.g. "Lembrete do MelhoreApp"). There are no out-of-app APIs or bot integrations; share-only keeps scope simple and works without API keys.
- **Reminder list filter, sort, and grouping:** The reminder list supports **filter** by multiple tags (OR semantics; `ReminderDao.getRemindersByCategoryIds` when non-empty), by priority (in-memory), by date range (in-memory), and by status (show/hide completed/cancelled - Sprint 13). **Sort** options: due date (asc), priority then due date, title (asc), creation date (asc or desc). When **group by tag** is enabled, the list is shown in sections by tag name ("Sem tag" for untagged), with section headers that have heading semantics for accessibility; when disabled, a flat list is shown. Grouping is computed in the ViewModel from the filtered/sorted list and categories. Priority is always visible on each reminder card (badge). **Filter/sort UI (Sprint 10):** Advanced filters (tag, priority, date range, group-by) are hidden by default behind "Filtros avançados" toggle; sort options are always visible. Default sort is "Por data" (closest to notify first). **Next notification date display (Sprint 11.5):** Reminder list shows next notification date for each reminder (calculated next occurrence for recurring, "MELHORADO" for completed non-recurring, snooze time if snoozed). **Completion/Cancellation UI (Sprint 13):** Completed reminders show low-contrast styling, "MELHORADO" tag, and delete button (only when completed); cancelled reminders show "CANCELADO" tag; ACTIVE reminders show checkmark button to mark as complete; completion requires confirmation modal ("Você tem certeza?").
- **Routine type (Sprint 12):** New reminder type `ROUTINE` in `RecurrenceType` enum. When Routine reminder fires, notification tap navigates user to task setup screen. Routine reminders behave similarly to one-time reminders but with special navigation action.
- **Authentication (Sprint 14):** Google Sign-In via Firebase Authentication. `AuthRepository` (core:auth) manages user session; `currentUser: StateFlow<CurrentUser?>` provides current user state. Login screen shown when not signed in; main app shown when signed in. User session persists across app restarts.
- **User scoping (Sprint 15):** All entities (`ReminderEntity`, `CategoryEntity`, `ChecklistItemEntity`) have `userId: String` field. All DAOs filter queries by `userId`. All ViewModels inject `AuthRepository` and pass current `userId` to DAOs. Database migration 2→3 adds `userId` column; existing data assigned temporary userId or migrated on first sign-in.
- **Cloud sync (Sprint 16-17):** Firebase Firestore syncs reminders, categories, and checklist items across devices. Collections: `/users/{userId}/reminders`, `/users/{userId}/categories`, `/users/{userId}/checklistItems`. `SyncRepository` (core:sync) handles upload/download/merge. Conflict resolution: cloud data wins (last-write-wins). Firestore offline persistence enables offline support. On first sign-in, user chooses migration strategy: upload local data, merge with cloud, or start fresh.

## Dependency injection and receivers

- **Hilt:** The app and core modules use Hilt for dependency injection (e.g. `DatabaseModule`, `SchedulingModule`). ViewModels and screens get dependencies via constructor injection. Broadcast receivers (`ReminderAlarmReceiver`, `SnoozeReceiver`, `BootReceiver`) do not use Hilt; they obtain database and scheduler via `SchedulingContext`, which the Application implements and provides when the process runs.

## Error handling

- **Scheduling failures:** If exact-alarm permission is missing (e.g. on Android 14+ when not granted in settings), scheduling may throw `ExactAlarmPermissionRequiredException`. The app layer (e.g. AddReminderViewModel or MainActivity) should catch this and surface a message to the user (e.g. "Allow exact alarms in settings to get reminders on time") or open the relevant settings screen. Other scheduling failures (e.g. invalid time) are handled in the layer that calls `ReminderScheduler` and can be shown as a snackbar or error state in the UI.

## Package structure (per module)

- **app:** `com.melhoreapp` – Application, MainActivity, `ui/theme/`, `ui/navigation/`.
- **core:common:** `com.melhoreapp.core.common` – `Result.kt`, `preferences/AppPreferences.kt` (SharedPreferences for default snooze etc.).
- **core:database:** `com.melhoreapp.core.database` – `entity/`, `dao/`, `MelhoreDatabase`, `DatabaseModule`. All entities have `userId: String` field (Sprint 15). `RecurrenceType` enum includes `ROUTINE` (Sprint 12). `ReminderStatus` enum (ACTIVE, COMPLETED, CANCELLED) added in Sprint 13; `ReminderEntity` has `status` field.
- **core:auth:** `com.melhoreapp.core.auth` – `AuthRepository` (Google Sign-In), `CurrentUser` (data class), `AuthModule` (Hilt). Provides user session management (Sprint 14).
- **core:sync:** `com.melhoreapp.core.sync` – `SyncRepository`, `FirestoreSyncService`, `SyncModule` (Hilt). Handles Firebase Firestore sync (Sprint 16-17).
- **core:notifications:** `com.melhoreapp.core.notifications` – `NotificationHelper`, resources.
- **core:scheduling:** `com.melhoreapp.core.scheduling` – `ReminderScheduler`, `ReminderAlarmReceiver` (30-minute recurring notifications for ACTIVE reminders - Sprint 13), `SnoozeReceiver`, `RecurrenceHelper` (next occurrence; supports DAILY, WEEKLY, BIWEEKLY, MONTHLY, ROUTINE - Sprint 11-12), `SchedulingContext` (interface), `ReminderWorker`, `SchedulingModule` (AlarmManager + scheduler provided via Hilt).
- **feature:** `com.melhoreapp.feature.<name>` – `ui/<screen>/`, `data/`, `domain/` (optional). **feature:auth** – `ui/LoginScreen.kt`, `LoginViewModel`; Google Sign-In flow (Sprint 14). **feature:categories** (shown as Tags in the app) uses `ui/list/` (list screen + ViewModel) and `ui/addedit/` (add/edit screen + ViewModel); filters by `userId` (Sprint 15). **feature:reminders** reminder list screen includes **multi-tag filter**, **priority filter**, **date range filter** (e.g. Próximos 7 dias, Este mês), **status filter** (show/hide completed - Sprint 13), **sort** by date, priority, title, or creation date, and **group by tag** vs flat list toggle (ViewModel uses ReminderDao, ChecklistItemDao; filter/sort/group-by state and persistence via AppPreferences); **"Filtros avançados" toggle** hides/shows advanced filters (Sprint 10); **next notification date display** shows calculated next occurrence or "MELHORADO"/"CANCELADO" based on status (Sprint 13); **completion flow** with confirmation modal ("Você tem certeza?") and low-contrast UI for completed reminders (Sprint 13); **cancellation flow** from edit screen with confirmation (Sprint 13); when grouped, sections by tag name with "Sem tag" for untagged and heading semantics; priority badge and checklist progress "checked/total" on each item; add/edit reminder screen supports **checklist items** (add/remove/toggle) and **edit mode** (route `reminders/edit/{reminderId}`; load and save reminder + checklist); recurrence dropdown includes **BIWEEKLY**, **MONTHLY**, and **ROUTINE** options (Sprint 11-12); filters by `userId` (Sprint 15). **Templates** coming-soon screen in `ui/templates/TemplatesComingSoonScreen.kt`; route `reminders/templates` from app navigation; entry from Reminders screen TopAppBar (Modelos de lembretes icon). **feature:integrations** – `ui/IntegrationsScreen.kt`; cards for Telegram, Slack, WhatsApp with "Enviar para…" launching the system share sheet; bottom tab "Integrações". **feature:settings** – `ui/SettingsScreen.kt`, `SettingsViewModel`; reads/writes `AppPreferences` for default snooze duration and delete after completion (Sprint 13, reworked from Sprint 11.5); Settings is a bottom-nav tab (Reminders, Tags, Integrações, Settings); includes "Sign out" option (Sprint 17).

Update this document when adding new modules or changing data/scheduling flow.
