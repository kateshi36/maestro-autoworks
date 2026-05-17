-- ============================================================================
--  Maestro Autoworks — users table migration (v1 → v2)
--  Run this ONCE on any database that was created from the original
--  maestro_db.sql (before the registration overhaul).
--
--  Safe to re-run: every statement uses IF NOT EXISTS / column existence
--  checks so it won't fail on a database that's already partially migrated.
--
--  Order matters — columns are added in logical registration-step order.
-- ============================================================================

USE maestro_db;

-- ── Step 3: Personal information ─────────────────────────────────────────────

ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `birthdate` date DEFAULT NULL
    AFTER `role`,

  ADD COLUMN IF NOT EXISTS `gender` enum('Male','Female','Other') DEFAULT NULL
    AFTER `birthdate`,

  -- `phone` already existed in v1 — keep it in place, just ensure it's there
  ADD COLUMN IF NOT EXISTS `phone` varchar(30) DEFAULT NULL
    AFTER `gender`,

  ADD COLUMN IF NOT EXISTS `address` text DEFAULT NULL
    AFTER `phone`;

-- ── Step 4: Driver's license ──────────────────────────────────────────────────

ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `drivers_license_no` varchar(20) DEFAULT NULL
    AFTER `address`,

  ADD COLUMN IF NOT EXISTS `drivers_license_issuance` date DEFAULT NULL
    AFTER `drivers_license_no`,

  ADD COLUMN IF NOT EXISTS `drivers_license_expiry` date DEFAULT NULL
    AFTER `drivers_license_issuance`,

  -- Comma-separated restriction codes, e.g. 'A,B,B2'
  ADD COLUMN IF NOT EXISTS `dl_codes` varchar(50) DEFAULT NULL
    AFTER `drivers_license_expiry`;

-- ── Step 4: Conductor's license (optional) ───────────────────────────────────

ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `conductors_license_no` varchar(20) DEFAULT NULL
    AFTER `dl_codes`,

  ADD COLUMN IF NOT EXISTS `conductors_license_issuance` date DEFAULT NULL
    AFTER `conductors_license_no`,

  ADD COLUMN IF NOT EXISTS `conductors_license_expiry` date DEFAULT NULL
    AFTER `conductors_license_issuance`;

-- ── Step 4: Vehicle information ───────────────────────────────────────────────

ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `license_plate` varchar(15) DEFAULT NULL
    AFTER `conductors_license_expiry`,

  ADD COLUMN IF NOT EXISTS `mv_file_number` varchar(15) DEFAULT NULL
    AFTER `license_plate`,

  ADD COLUMN IF NOT EXISTS `vehicle_make` varchar(40) DEFAULT NULL
    AFTER `mv_file_number`,

  ADD COLUMN IF NOT EXISTS `vehicle_model` varchar(60) DEFAULT NULL
    AFTER `vehicle_make`;

-- ── Step 5: Document upload paths ────────────────────────────────────────────

ALTER TABLE `users`
  -- license_image_path mirrors the app's COL_LIC_IMAGE for backward compat
  ADD COLUMN IF NOT EXISTS `license_image_path` varchar(255) DEFAULT NULL
    AFTER `vehicle_model`,

  ADD COLUMN IF NOT EXISTS `dl_upload_path` varchar(255) DEFAULT NULL
    AFTER `license_image_path`,

  ADD COLUMN IF NOT EXISTS `or_image_path` varchar(255) DEFAULT NULL
    AFTER `dl_upload_path`,

  ADD COLUMN IF NOT EXISTS `cr_image_path` varchar(255) DEFAULT NULL
    AFTER `or_image_path`;

-- ── Verify ────────────────────────────────────────────────────────────────────
-- Uncomment to confirm all columns are present after running:
-- DESCRIBE `users`;

-- ============================================================================
--  appointments table migration — add fuel_type (item 13)
-- ============================================================================

ALTER TABLE `appointments`
  ADD COLUMN IF NOT EXISTS `fuel_type` enum('Gasoline','Diesel') DEFAULT NULL
    AFTER `plate_no`;

-- ── vehicle_concerns column (item 14) ────────────────────────────────────────
-- Stores comma-joined selected concern labels, e.g.:
-- "🔧  Engine — knocking, misfires, or rough idle, 🛑  Brakes — squealing..."
-- NULL means no concerns were selected (valid — concerns are optional).

ALTER TABLE `appointments`
  ADD COLUMN IF NOT EXISTS `vehicle_concerns` text DEFAULT NULL
    AFTER `fuel_type`;

-- ── OR/CR upload columns (item 1 — ORCR Upload) ──────────────────────────────
-- orcr_status mirrors Appointment.orcrStatus: "Yes (photo captured)" | "No"
-- orcr_image_path mirrors Appointment.orcrImagePath: relative path to uploaded file

ALTER TABLE `appointments`
  ADD COLUMN IF NOT EXISTS `orcr_status` enum('Yes (photo captured)','No') NOT NULL DEFAULT 'No'
    AFTER `notes`,
  ADD COLUMN IF NOT EXISTS `orcr_image_path` varchar(255) DEFAULT NULL
    AFTER `orcr_status`;

-- ============================================================================
--  appointments table — app-compatible alias columns  (item 17)
--
--  Background
--  ----------
--  The Android app (DatabaseHelper.java) uses different column names for three
--  fields that the website already stores under different names:
--
--    App constant          App column name    Website column name
--    COL_APPT_CAR_MODEL    car_model          vehicle_model
--    COL_APPT_YEAR_MODEL   year_model         vehicle_year
--    COL_APPT_NOTES        additional_notes   notes
--
--  Rather than rename the website's working columns (which would require
--  touching every PHP file that reads them), we add MariaDB VIRTUAL generated
--  columns as read-only aliases.  They occupy no extra storage, stay
--  automatically in sync, and can be queried by either name.
--
--  Safe to run on databases created from the OLD maestro_db.sql (where these
--  three columns are genuinely absent) AND on databases created from the NEW
--  maestro_db.sql (where they already exist as GENERATED columns) — the
--  IF NOT EXISTS guard handles both cases.
-- ============================================================================

ALTER TABLE `appointments`
  ADD COLUMN IF NOT EXISTS `car_model`
      varchar(80) GENERATED ALWAYS AS (`vehicle_model`) VIRTUAL
      COMMENT 'App alias for vehicle_model (COL_APPT_CAR_MODEL)',

  ADD COLUMN IF NOT EXISTS `year_model`
      year(4) GENERATED ALWAYS AS (`vehicle_year`) VIRTUAL
      COMMENT 'App alias for vehicle_year (COL_APPT_YEAR_MODEL)',

  ADD COLUMN IF NOT EXISTS `additional_notes`
      text GENERATED ALWAYS AS (`notes`) VIRTUAL
      COMMENT 'App alias for notes (COL_APPT_NOTES)';

-- ── Verify ───────────────────────────────────────────────────────────────────
-- Uncomment to confirm all alias columns are present:
-- DESCRIBE `appointments`;

-- ============================================================================
--  appointments table — rating column  (item 18)
--
--  Background
--  ----------
--  The Android app (BookActivity / HomeActivity) collects a 1–5 star rating
--  from the user via RatingBar before submission.  The value is stored as an
--  integer (COL_APPT_RATING = "rating") in DatabaseHelper.java.
--  0 = not rated (default), 1–5 = star count.
-- ============================================================================

ALTER TABLE `appointments`
  ADD COLUMN IF NOT EXISTS `rating` tinyint(1) NOT NULL DEFAULT 0
    COMMENT '0 = not rated; 1–5 mirrors BookActivity RatingBar (COL_APPT_RATING)'
    AFTER `admin_notes`;

-- ── Verify ───────────────────────────────────────────────────────────────────
-- Uncomment to confirm rating column is present:
-- DESCRIBE `appointments`;
