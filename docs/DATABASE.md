# MelhoreApp – Database Architecture & Documentation

## Overview

MelhoreApp uses **Room** (Android's SQLite abstraction layer) for local data persistence. The database is designed to support reminders (Melhores), categories (Tags), lists, and checklist items with proper relationships, indexes, and data integrity constraints.

**Current Database Version:** 7  
**Database Name:** `melhore_db`  
**Location:** `/data/data/com.melhoreapp/databases/melhore_db`

---

## Database Schema

### Entities

#### 1. ReminderEntity (`reminders` table)

The core entity representing reminders (Melhores) and routines (Rotinas).

**Fields:**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key (auto-generated) |
| `userId` | String | No | Owner user ID (Firebase UID or 'local' for pre-sign-in data; Sprint 17) |
| `title` | String | No | Reminder title |
| `notes` | String | No | Additional notes (default: "") |
| `type` | RecurrenceType | No | Recurrence pattern (NONE, DAILY, WEEKLY, BIWEEKLY, MONTHLY, CUSTOM) |
| `dueAt` | Long | No | Due date/time (epoch milliseconds) |
| `categoryId` | Long | Yes | Foreign key to `categories` table |
| `listId` | Long | Yes | Foreign key to `lists` table |
| `priority` | Priority | No | Priority level (LOW, MEDIUM, HIGH, URGENT) |
| `snoozedUntil` | Long | Yes | Snooze expiration time (epoch milliseconds) |
| `status` | ReminderStatus | No | Status (ACTIVE, COMPLETED, CANCELLED) |
| `isActive` | Boolean | No | Backward compatibility flag (synced with status) |
| `isRoutine` | Boolean | No | True if this is a Rotina (routine) reminder |
| `customRecurrenceDays` | String | Yes | Custom recurrence days (comma-separated, e.g., "MONDAY,WEDNESDAY,FRIDAY") |
| `parentReminderId` | Long | Yes | Foreign key to parent Rotina reminder (for task reminders) |
| `startTime` | Long | Yes | Task start time (epoch milliseconds, for child tasks) |
| `checkupFrequencyHours` | Int | Yes | Checkup frequency in hours (for task reminders) |
| `isTask` | Boolean | No | True if this is a task reminder (child of Rotina) |
| `createdAt` | Long | No | Creation timestamp (epoch milliseconds) |
| `updatedAt` | Long | No | Last update timestamp (epoch milliseconds) |

**Foreign Keys:**
- `categoryId` → `categories.id` (SET NULL on delete)
- `listId` → `lists.id` (SET NULL on delete)
- `parentReminderId` → `reminders.id` (CASCADE on delete)

**Indexes:**
- Single-column: `categoryId`, `listId`, `dueAt`, `parentReminderId`, `isTask`, `status`, `startTime`, `userId`
- Composite: `(status, dueAt)`, `(userId, status, dueAt)`, `(isTask, status)`, `(parentReminderId, startTime, dueAt)`

**Relationships:**
- One-to-many with `CategoryEntity` (optional)
- One-to-many with `ListEntity` (optional)
- Self-referencing: parent-child relationship for Rotina → tasks
- One-to-many with `ChecklistItemEntity`

#### 2. CategoryEntity (`categories` table)

Represents tags/categories used to organize reminders.

**Fields:**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key (auto-generated) |
| `userId` | String | No | Owner user ID (Sprint 17) |
| `name` | String | No | Category name |
| `colorArgb` | Int | Yes | Color ARGB value |
| `sortOrder` | Int | No | Sort order for display |

**Indexes:** None (small table, queries by primary key)

#### 3. ListEntity (`lists` table)

Represents lists for organizing reminders (currently unused in UI but maintained for backward compatibility).

**Fields:**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key (auto-generated) |
| `name` | String | No | List name |
| `colorArgb` | Int | Yes | Color ARGB value |
| `sortOrder` | Int | No | Sort order for display |

**Indexes:** None (small table, queries by primary key)

#### 4. ChecklistItemEntity (`checklist_items` table)

Represents checklist items (sub-tasks) for reminders.

**Fields:**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key (auto-generated) |
| `userId` | String | No | Owner user ID (Sprint 17) |
| `reminderId` | Long | No | Foreign key to `reminders` table |
| `label` | String | No | Checklist item text |
| `sortOrder` | Int | No | Sort order within reminder |
| `checked` | Boolean | No | Completion status |

**Foreign Keys:**
- `reminderId` → `reminders.id` (CASCADE on delete)

**Indexes:**
- Single-column: `reminderId`

---

## Data Access Objects (DAOs)

### ReminderDao

**Key Queries:**

| Method | Query Pattern | Index Used | Purpose |
|--------|---------------|------------|---------|
| `getAllReminders(userId)` | `SELECT * FROM reminders WHERE userId = :userId ORDER BY dueAt ASC` | `userId`, `dueAt` | Get all reminders for user |
| `getReminderById(id)` | `SELECT * FROM reminders WHERE id = :id` | Primary key | Get single reminder (used by receivers; no userId) |
| `getUpcomingActiveReminders(userId, afterMillis)` | `WHERE userId = :userId AND status = 'ACTIVE' AND dueAt > :afterMillis ...` | `(userId, status, dueAt)` | Get active reminders after timestamp |
| `getActiveReminders(userId)` | `WHERE userId = :userId AND status = 'ACTIVE' ...` | `userId`, `status` | Get all active reminders (e.g. boot reschedule) |
| `getTasksByParentReminderId(userId, parentReminderId)` | `WHERE userId = :userId AND parentReminderId = :id ...` | `(parentReminderId, startTime, dueAt)` | Get child tasks for Rotina |
| `getAllRemindersExcludingTasks(userId)` | `WHERE userId = :userId AND isTask = 0 ...` | `userId`, `isTask` | Get reminders excluding tasks |
| `getRemindersByCategoryIds(userId, categoryIds)` | `WHERE userId = :userId AND categoryId IN (:ids) ...` | `userId`, `categoryId` | Filter by multiple categories |

### CategoryDao

**Key Queries:**
- `getAllCategories(userId)` - Get all categories for user, ordered by sortOrder and name
- `getCategoryById(userId, id)` - Get single category by ID for user
- `migrateLocalUserIdTo(newUserId)` - Assign rows with `userId = 'local'` to signed-in user (Sprint 17)

### ChecklistItemDao

**Key Queries:**
- `getItemsByReminderId(userId, reminderId)` - Get checklist items for a reminder (indexed by `reminderId`)
- `getAllItems(userId)` - Get all checklist items for user (for list progress calculation)
- `deleteByReminderId(userId, reminderId)` - Delete all items for a reminder
- `migrateLocalUserIdTo(newUserId)` - Assign rows with `userId = 'local'` to signed-in user (Sprint 17)

### ListDao

**Key Queries:**
- `getAllLists()` - Get all lists (currently unused in UI)

---

## Migration History

### Migration 1→2 (Sprint 5.5)
- Added `checklist_items` table
- Added foreign key and index on `reminderId`

### Migration 2→3 (Sprint 13)
- Added `status` column (TEXT, NOT NULL, DEFAULT 'ACTIVE')
- Migrated existing `isActive` values to `status` enum

### Migration 3→4 (Sprint 12)
- Added `isRoutine` column (INTEGER, NOT NULL, DEFAULT 0)
- Added `customRecurrenceDays` column (TEXT, nullable)

### Migration 4→5 (Sprint 12.1)
- Added `parentReminderId` column (INTEGER, nullable)
- Added `startTime` column (INTEGER, nullable)
- Added `checkupFrequencyHours` column (INTEGER, nullable)
- Added `isTask` column (INTEGER, NOT NULL, DEFAULT 0)
- Added indexes: `parentReminderId`, `isTask`

### Migration 5→6 (Sprint 12.1 - Optimization)
- Added index on `status` column
- Added composite index on `(status, dueAt)`
- Added composite index on `(isTask, status)`
- Added index on `startTime` column
- Added composite index on `(parentReminderId, startTime, dueAt)`

### Migration 6→7 (Sprint 17 - User scoping)
- Added `userId` column (TEXT, nullable in migration) to `reminders`, `categories`, `checklist_items`
- Backfilled existing rows with `userId = 'local'`
- Added index on `reminders.userId` and composite index on `(userId, status, dueAt)` for user-scoped queries
- All DAO list/scope queries now take `userId`; `getReminderById(id)` unchanged for broadcast receivers
- On sign-in, app runs one-off migration: rows with `userId = 'local'` are updated to the signed-in user's ID

**Migration Strategy:**
- All migrations use `IF NOT EXISTS` for idempotency
- Backward compatible (adds columns/indexes, never removes)
- Foreign key constraints enforced via entity annotations (Room handles enforcement)

---

## Query Performance & Indexes

### Index Strategy

The database uses a strategic indexing approach to optimize common query patterns:

1. **Single-column indexes** for frequently filtered columns:
   - `status` - Used in `getActiveReminders()` and status filtering
   - `startTime` - Used for task ordering
   - `isTask` - Used to exclude tasks from main list
   - `dueAt` - Used for sorting and date range queries
   - Foreign keys (`categoryId`, `listId`, `parentReminderId`) - Used for joins

2. **Composite indexes** for multi-column queries:
   - `(status, dueAt)` - Optimizes `getUpcomingActiveReminders()` queries
   - `(isTask, status)` - Optimizes queries filtering by both task type and status
   - `(parentReminderId, startTime, dueAt)` - Optimizes parent-child queries with ordering

### Query Optimization Notes

- SQLite's query planner automatically selects optimal indexes
- Composite indexes are used when queries filter/order by multiple columns
- Indexes are maintained automatically by SQLite
- Write performance impact is minimal due to selective indexing

---

## Data Integrity

### Foreign Key Constraints

1. **Category → Reminders**: SET NULL on delete (reminders keep their data if category deleted)
2. **List → Reminders**: SET NULL on delete (reminders keep their data if list deleted)
3. **Reminder → Tasks**: CASCADE on delete (child tasks deleted when parent Rotina deleted)
4. **Reminder → Checklist Items**: CASCADE on delete (checklist items deleted when reminder deleted)

### Data Consistency

- `status` and `isActive` are kept in sync (application-level logic)
- `parentReminderId` must reference valid reminder ID or be NULL
- `customRecurrenceDays` must be valid comma-separated day names when `type = CUSTOM`
- `isTask = true` implies `parentReminderId IS NOT NULL`

---

## Current Limitations & Technical Debt

### Known Issues

1. **User Scoping**: Currently missing `userId` field (planned for Sprint 17)
   - All data is local-only, no multi-user support
   - Will require migration when authentication is added

2. **Backward Compatibility Field**: `isActive` field kept for compatibility
   - Should be removed in future migration once all code paths use `status`
   - Currently synced with `status` in application code

3. **List Entity**: Currently unused in UI but maintained in schema
   - Consider deprecation or removal in future version

4. **No Soft Deletes**: Deleted records are permanently removed
   - May want to add `deletedAt` timestamp for recovery/audit

5. **No Data Versioning**: No tracking of entity versions for conflict resolution
   - Will be needed for cloud sync (planned Sprint 18)

---

## Database Best Practices Applied

✅ **Normalization**: Proper foreign key relationships, no data duplication  
✅ **Indexing**: Strategic indexes matching query patterns  
✅ **Migration Safety**: Idempotent migrations with `IF NOT EXISTS`  
✅ **Data Integrity**: Foreign key constraints and application-level validation  
✅ **Performance**: Composite indexes for multi-column queries  
✅ **Scalability**: Indexes designed to prevent full table scans as data grows  

---

## Future Roadmap

See [DATABASE_ROADMAP.md](DATABASE_ROADMAP.md) for detailed post-launch improvement plans.

---

## Maintenance & Monitoring

### Database Size Considerations

- **Estimated Growth**: ~1KB per reminder (with checklist items)
- **Index Overhead**: ~20-30% additional storage
- **Recommended Limits**: 
  - Monitor database size if > 50MB
  - Consider archiving old completed reminders

### Backup & Recovery

- Room database files can be backed up via Android Backup Service
- Consider implementing export/import functionality for user data portability
- Cloud sync (Sprint 18) will provide additional backup mechanism

---

## References

- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [Database Architecture](ARCHITECTURE.md)
- [Sprint Documentation](SPRINTS.md)
