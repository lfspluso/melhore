# Changelog

All notable changes to MelhoreApp are documented here. Format is by version or sprint for pre-release.

## [Unreleased]

- Sprint 4–6: Categories and lists, priority and polish, documentation and release prep (see [docs/SPRINTS.md](docs/SPRINTS.md)).

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
