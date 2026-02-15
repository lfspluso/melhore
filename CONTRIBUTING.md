# Contributing to MelhoreApp

Short guidelines for anyone changing or extending the app.

## Before you start

1. **Read the docs:** [docs/CONTEXT.md](docs/CONTEXT.md) (what the app does, where things live) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (data flow, modules, key decisions).
2. **Check the sprint plan:** [docs/SPRINTS.md](docs/SPRINTS.md) for current goals and done criteria.

## Making changes

- Follow the existing layer structure: UI → ViewModel → use case/repository → database or scheduling.
- Place new code in the module and package that owns that concern (see CONTEXT “Where to find what”).
- Conventions: screens like `ReminderListScreen`, ViewModels like `ReminderListViewModel`, state/events like `ReminderListState` / `ReminderListEvent`.

## Testing and validation

- Use [docs/TESTING.md](docs/TESTING.md) to validate behaviour manually for the sprint you’re working on.
- When you add automated tests, document how to run them in TESTING.md (e.g. `./gradlew test`, `connectedCheck`) and what they cover.

## Updating documentation

- When you add or change modules, data flow, or scheduling: update [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- When you add or change features: update [docs/CONTEXT.md](docs/CONTEXT.md) and [docs/SPRINTS.md](docs/SPRINTS.md) as needed.
- When you change acceptance criteria or add new flows: update [docs/TESTING.md](docs/TESTING.md) so it stays in sync with SPRINTS.

## Build and run

See [README.md](README.md) for requirements, Gradle commands, and run instructions.
