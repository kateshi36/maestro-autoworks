-- =====================================================================
-- MAESTRO AUTOWORKS — PMS MIGRATION
-- Run this ONCE against maestro_db after the main schema is imported.
-- Safe to re-run: uses IF NOT EXISTS / IF EXISTS guards throughout.
-- =====================================================================

USE maestro_db;

-- ── 1. Repair Tasks table ─────────────────────────────────────────────────
--    Links tasks to confirmed appointments with lifecycle timestamps.
CREATE TABLE IF NOT EXISTS repair_tasks (
    id           INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    appt_id      INT           NOT NULL,
    task_name    VARCHAR(150)  NOT NULL,
    assigned_to  VARCHAR(100)  DEFAULT NULL,
    status       ENUM('pending','in_progress','testing','completed')
                               NOT NULL DEFAULT 'pending',
    notes        TEXT          DEFAULT NULL,
    sort_order   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    started_at   DATETIME      DEFAULT NULL,
    completed_at DATETIME      DEFAULT NULL,
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_appt (appt_id),
    INDEX idx_status (status),

    CONSTRAINT fk_rt_appt
        FOREIGN KEY (appt_id)
        REFERENCES appointments(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 2. Extend notification types to include PMS events ───────────────────
--    The ALTER fails silently if the column already includes these values,
--    so we recreate the ENUM with the full set.
ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'booking_received',
        'booking_confirmed',
        'booking_declined',
        'booking_completed',
        'booking_cancelled',
        'reminder',
        'task_started',
        'vehicle_ready'
    ) NOT NULL;


-- ── 3. Verify (optional — remove before production) ──────────────────────
-- SELECT 'repair_tasks created' AS status;
-- DESCRIBE repair_tasks;
-- SHOW COLUMNS FROM notifications LIKE 'type';
