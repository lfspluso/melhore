# Changelog

All notable changes to MelhoreApp are documented here. Format is by version or sprint for pre-release.

## [Unreleased]

- **Sprint 6 – Filtering and sorting:** Reminder list supports filter by multiple tags (OR), by priority (multi-select), and by date range ("Próximos 7 dias", "Este mês"); sort by due date, priority, title, creation date (asc/desc). Last-used filter and sort persist in AppPreferences and are restored on app launch. Empty state when filtered shows "Nenhum melhore com esses filtros" and "Limpar filtros". See [docs/SPRINTS.md](docs/SPRINTS.md).
- Removed Lists tab from bottom navigation; Tags only (Categories renamed to Tags in the UI). Reminder list filter by tag only; add/edit reminder has Tag dropdown only.
- Sprint restructure: Sprint 6 (documentation and release prep) delayed to **Sprint 10**. New **Sprints 6–9** added: filtering and sorting improvements, visualization grouping by tag, "Chegando em breve" screen for templates, Integrações tab (Telegram, Slack, WhatsApp). See [docs/SPRINTS.md](docs/SPRINTS.md).

## Sprint 3 – Recurring reminders and snooze

- Recurring reminders (daily, weekly) with reschedule on each fire.
- Snooze actions on notifications (5 min, 15 min, 1 hour, 1 day).
- Boot reschedule for all active reminders including snoozed.

## Sprint 2 – Notifications and scheduling

- One-time reminders trigger notification at due time via AlarmManager.
- BootReceiver reschedules after device reboot.
- POST_NOTIFICATIONS and SCHEDULE_EXACT_ALARM handling.

## Sprint 1 – Core data and one-time reminders

- Room database; Reminder, Category, List entities and DAOs.
- Add reminder screen (title, date/time, category, list, priority).
- Reminder list screen with delete.

## Sprint 0 – Project bootstrap

- Module structure (app, core, feature).
- Compose, Hilt, Room wired; empty Reminders screen.
