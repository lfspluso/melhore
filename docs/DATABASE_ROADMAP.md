# MelhoreApp – Database Improvement Roadmap

This document outlines planned database improvements for post-launch phases, prioritized by impact and feasibility.

---

## Phase 1: User Scoping & Multi-User Support (High Priority)

**Timeline:** Sprint 17 (Post-Launch)  
**Status:** Planned

### Goals
- Add user authentication and data scoping
- Support multiple users on same device (future)
- Prepare for cloud sync

### Changes Required

#### Database Migration 6→7

**Add `userId` column to all entities:**

```sql
ALTER TABLE reminders ADD COLUMN userId TEXT NOT NULL DEFAULT 'local';
ALTER TABLE categories ADD COLUMN userId TEXT NOT NULL DEFAULT 'local';
ALTER TABLE checklist_items ADD COLUMN userId TEXT NOT NULL DEFAULT 'local';
```

**Update indexes:**
- Add composite indexes including `userId`:
  - `(userId, status, dueAt)` - For user-scoped active reminder queries
  - `(userId, isTask, status)` - For user-scoped task filtering
  - `(userId, parentReminderId, startTime, dueAt)` - For user-scoped parent-child queries
  - `(userId, categoryId)` - For user-scoped category filtering

**Update all DAOs:**
- Add `userId: String` parameter to all query methods
- Add `WHERE userId = :userId` to all queries
- Update insert methods to require `userId`

**Migration Strategy:**
- Assign temporary `userId = "local_${deviceId}"` for existing data
- On first sign-in, migrate local data to authenticated user's ID
- Provide migration dialog for user choice (upload local, merge with cloud, start fresh)

### Benefits
- Data isolation between users
- Foundation for cloud sync
- Multi-device support preparation

### Risks & Mitigation
- **Risk**: Breaking existing queries if `userId` not provided  
  **Mitigation**: Make `userId` non-null with default, update all call sites
- **Risk**: Data loss during migration  
  **Mitigation**: Comprehensive migration testing, backup before migration

---

## Phase 2: Cloud Sync & Conflict Resolution (High Priority)

**Timeline:** Sprint 18 (Post-Launch)  
**Status:** Planned  
**Dependencies:** Phase 1 (User Scoping)

### Goals
- Sync data across devices via Firebase Firestore
- Handle offline scenarios gracefully
- Resolve conflicts intelligently

### Changes Required

#### Database Migration 7→8

**Add sync metadata fields:**

```sql
ALTER TABLE reminders ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1;
ALTER TABLE reminders ADD COLUMN lastSyncedAt INTEGER;
ALTER TABLE reminders ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED';
ALTER TABLE reminders ADD COLUMN cloudId TEXT;

ALTER TABLE categories ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1;
ALTER TABLE categories ADD COLUMN lastSyncedAt INTEGER;
ALTER TABLE categories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED';
ALTER TABLE categories ADD COLUMN cloudId TEXT;

ALTER TABLE checklist_items ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1;
ALTER TABLE checklist_items ADD COLUMN lastSyncedAt INTEGER;
ALTER TABLE checklist_items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED';
ALTER TABLE checklist_items ADD COLUMN cloudId TEXT;
```

**Add indexes:**
- `(userId, syncStatus)` - For finding unsynced items
- `(userId, lastSyncedAt)` - For incremental sync queries
- `cloudId` - For mapping local to cloud IDs

**Sync Status Enum:**
- `SYNCED` - Successfully synced with cloud
- `PENDING` - Local changes not yet synced
- `CONFLICT` - Conflict detected, needs resolution
- `ERROR` - Sync failed, needs retry

### Benefits
- Multi-device data access
- Automatic backup
- Offline-first architecture

### Risks & Mitigation
- **Risk**: Sync conflicts causing data loss  
  **Mitigation**: Last-write-wins with version tracking, conflict resolution UI
- **Risk**: Performance impact from sync operations  
  **Mitigation**: Background sync, batch operations, incremental sync

---

## Phase 3: Data Archiving & Retention (Medium Priority)

**Timeline:** Post-Launch (3-6 months)  
**Status:** Future Consideration

### Goals
- Archive old completed reminders
- Improve query performance on active data
- Provide data retention policies

### Changes Required

#### Database Migration 8→9

**Add archiving fields:**

```sql
ALTER TABLE reminders ADD COLUMN archivedAt INTEGER;
ALTER TABLE reminders ADD COLUMN archiveReason TEXT;
```

**Create archive table (optional):**

```sql
CREATE TABLE reminders_archive (
    id INTEGER PRIMARY KEY,
    -- Copy all reminder fields
    archivedAt INTEGER NOT NULL
);
```

**Add indexes:**
- `(status, archivedAt)` - For finding non-archived active reminders
- `archivedAt` - For archive queries

### Archiving Strategy

1. **Automatic Archiving:**
   - Archive COMPLETED reminders older than 90 days
   - Archive CANCELLED reminders older than 30 days
   - User-configurable retention period

2. **Manual Archiving:**
   - User can archive reminders manually
   - Archived reminders hidden from main list but recoverable

3. **Query Optimization:**
   - Default queries exclude archived items
   - Separate query for archive view
   - Consider moving to separate table for better performance

### Benefits
- Improved query performance on active data
- Reduced database size
- Better user experience (less clutter)

### Risks & Mitigation
- **Risk**: Users losing access to old data  
  **Mitigation**: Archive view, restore functionality, clear retention policy
- **Risk**: Performance impact during archiving  
  **Mitigation**: Background job, batch processing, off-peak hours

---

## Phase 4: Soft Deletes & Audit Trail (Medium Priority)

**Timeline:** Post-Launch (6-12 months)  
**Status:** Future Consideration

### Goals
- Enable data recovery
- Track data changes for debugging
- Support undo operations

### Changes Required

#### Database Migration 9→10

**Add soft delete fields:**

```sql
ALTER TABLE reminders ADD COLUMN deletedAt INTEGER;
ALTER TABLE reminders ADD COLUMN deletedBy TEXT;

ALTER TABLE categories ADD COLUMN deletedAt INTEGER;
ALTER TABLE categories ADD COLUMN deletedBy TEXT;

ALTER TABLE checklist_items ADD COLUMN deletedAt INTEGER;
```

**Add audit log table:**

```sql
CREATE TABLE audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entityType TEXT NOT NULL,
    entityId INTEGER NOT NULL,
    action TEXT NOT NULL, -- CREATE, UPDATE, DELETE, RESTORE
    userId TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    changes TEXT -- JSON of changed fields
);

CREATE INDEX idx_audit_log_entity ON audit_log(entityType, entityId);
CREATE INDEX idx_audit_log_user ON audit_log(userId, timestamp);
```

**Update queries:**
- Default queries exclude soft-deleted items (`WHERE deletedAt IS NULL`)
- Add `getDeletedReminders()` query for recovery view
- Add `restoreReminder()` method

### Benefits
- Data recovery capability
- Debugging and support tools
- User confidence (undo operations)

### Risks & Mitigation
- **Risk**: Database bloat from keeping deleted records  
  **Mitigation**: Permanent deletion after retention period (e.g., 30 days)
- **Risk**: Performance impact from additional WHERE clauses  
  **Mitigation**: Index on `deletedAt`, partial indexes if supported

---

## Phase 5: Advanced Analytics & Reporting (Low Priority)

**Timeline:** Post-Launch (12+ months)  
**Status:** Future Consideration

### Goals
- Track completion rates
- Analyze reminder patterns
- Provide insights to users

### Changes Required

#### Database Migration 10→11

**Add analytics tables:**

```sql
CREATE TABLE reminder_analytics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reminderId INTEGER NOT NULL,
    userId TEXT NOT NULL,
    completedAt INTEGER,
    completionDuration INTEGER, -- Time from creation to completion
    snoozeCount INTEGER DEFAULT 0,
    notificationCount INTEGER DEFAULT 0,
    FOREIGN KEY(reminderId) REFERENCES reminders(id) ON DELETE CASCADE
);

CREATE TABLE daily_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId TEXT NOT NULL,
    date INTEGER NOT NULL, -- Date as epoch (start of day)
    remindersCreated INTEGER DEFAULT 0,
    remindersCompleted INTEGER DEFAULT 0,
    remindersCancelled INTEGER DEFAULT 0,
    averageCompletionTime INTEGER,
    UNIQUE(userId, date)
);

CREATE INDEX idx_analytics_reminder ON reminder_analytics(reminderId);
CREATE INDEX idx_analytics_user ON reminder_analytics(userId, completedAt);
CREATE INDEX idx_daily_stats_user_date ON daily_stats(userId, date);
```

### Benefits
- User insights and motivation
- Product analytics
- Feature usage tracking

### Risks & Mitigation
- **Risk**: Privacy concerns with analytics  
  **Mitigation**: Opt-in, anonymization, local-only option
- **Risk**: Performance impact from additional writes  
  **Mitigation**: Batch writes, background processing

---

## Phase 6: Performance Optimizations (Ongoing)

**Timeline:** Continuous  
**Status:** Ongoing

### Current Optimizations
- ✅ Strategic indexes on frequently queried columns
- ✅ Composite indexes for multi-column queries
- ✅ Foreign key indexes for join performance

### Future Optimizations

1. **Query Optimization:**
   - Analyze slow queries using SQLite EXPLAIN QUERY PLAN
   - Add missing indexes based on query patterns
   - Consider materialized views for complex aggregations

2. **Database Maintenance:**
   - Periodic VACUUM operations (via Room's built-in support)
   - ANALYZE for query planner optimization
   - Consider FTS (Full-Text Search) for reminder search

3. **Caching Strategy:**
   - In-memory cache for frequently accessed data
   - Cache invalidation strategy
   - Consider Room's built-in caching

4. **Batch Operations:**
   - Batch inserts/updates for better performance
   - Transaction optimization
   - Reduce N+1 query problems

### Monitoring Metrics

- Database size
- Query execution time (log slow queries)
- Index usage statistics
- Migration success rate
- Sync performance (post Phase 2)

---

## Phase 7: Data Export/Import (Low Priority)

**Timeline:** Post-Launch (6+ months)  
**Status:** Future Consideration

### Goals
- Allow users to export their data
- Support data portability
- Enable backup/restore functionality

### Implementation

**Export Format:**
- JSON format for human readability
- Include all user data (reminders, categories, checklist items)
- Include metadata (timestamps, sync status)

**Import Format:**
- Support JSON import
- Validate data integrity
- Handle conflicts (merge vs replace)

**Features:**
- Export to file (Share intent)
- Import from file
- Cloud backup integration (Google Drive, etc.)

---

## Migration Best Practices

### General Guidelines

1. **Always test migrations:**
   - Test on sample data
   - Test rollback scenarios
   - Test on different Android versions

2. **Backward compatibility:**
   - Never remove columns without deprecation period
   - Use nullable columns when adding required fields
   - Provide defaults for new non-null columns

3. **Performance:**
   - Use transactions for multi-step migrations
   - Batch operations when possible
   - Consider background migration for large datasets

4. **Documentation:**
   - Document migration purpose and changes
   - Update schema documentation
   - Update query documentation if indexes change

5. **Rollback plan:**
   - Always have rollback strategy
   - Test rollback procedures
   - Consider feature flags for gradual rollout

---

## Priority Matrix

| Phase | Priority | Impact | Effort | Dependencies |
|-------|----------|--------|--------|--------------|
| Phase 1: User Scoping | High | High | Medium | None |
| Phase 2: Cloud Sync | High | High | High | Phase 1 |
| Phase 3: Archiving | Medium | Medium | Medium | None |
| Phase 4: Soft Deletes | Medium | Medium | Low | None |
| Phase 5: Analytics | Low | Low | High | Phase 1 |
| Phase 6: Performance | Ongoing | Medium | Low | None |
| Phase 7: Export/Import | Low | Low | Medium | None |

---

## Success Metrics

### Phase 1 (User Scoping)
- ✅ All entities have `userId` field
- ✅ All queries filter by `userId`
- ✅ Migration runs successfully on existing data
- ✅ No data loss during migration

### Phase 2 (Cloud Sync)
- ✅ Data syncs across devices within 5 seconds
- ✅ Offline changes sync on reconnect
- ✅ Conflict resolution works correctly
- ✅ <1% sync failure rate

### Phase 3 (Archiving)
- ✅ Query performance improves by 20%+
- ✅ Database size reduced by 30%+ (after archiving)
- ✅ Users can access archived data

### Phase 4 (Soft Deletes)
- ✅ Deleted items recoverable for 30 days
- ✅ Audit log captures all changes
- ✅ No performance degradation

---

## References

- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [SQLite Performance Tuning](https://www.sqlite.org/performance.html)
- [Firebase Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Database Architecture](DATABASE.md)
- [Sprint Documentation](SPRINTS.md)
