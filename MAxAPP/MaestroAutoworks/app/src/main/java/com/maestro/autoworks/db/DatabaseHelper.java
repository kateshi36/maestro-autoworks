package com.maestro.autoworks.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite helper — stores users and appointments locally.
 * v2: added role column to users; admin queries for all appointments.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "maestro_autoworks.db";
    private static final int    DB_VERSION = 8;

    // Tables
    public static final String TABLE_USERS        = "users";
    public static final String TABLE_APPOINTMENTS = "appointments";

    // Users columns
    public static final String COL_ID         = "id";
    public static final String COL_FIRST_NAME = "first_name";
    public static final String COL_LAST_NAME  = "last_name";
    public static final String COL_USERNAME   = "username";
    public static final String COL_EMAIL      = "email";
    public static final String COL_PHONE      = "phone";
    public static final String COL_PASSWORD   = "password";
    public static final String COL_ROLE       = "role";   // "customer" | "admin"
    // Personal info fields (added in DB_VERSION 7)
    public static final String COL_BIRTHDATE  = "birthdate";
    public static final String COL_GENDER     = "gender";
    // License fields (added in DB_VERSION 6)
    public static final String COL_DL_NO      = "drivers_license_no";
    public static final String COL_DL_EXPIRY  = "drivers_license_expiry";
    public static final String COL_CL_NO      = "conductors_license_no";
    public static final String COL_CL_EXPIRY  = "conductors_license_expiry";
    public static final String COL_LIC_IMAGE  = "license_image_path";
    // Vehicle & verification fields (added in DB_VERSION 8)
    public static final String COL_LICENSE_PLATE = "license_plate";   // e.g. "ABC 1234"
    public static final String COL_MV_FILE_NO    = "mv_file_number";  // 15-digit LTO MV file no.
    public static final String COL_VEHICLE_MAKE  = "vehicle_make";    // e.g. "Toyota"
    public static final String COL_VEHICLE_MODEL = "vehicle_model";   // e.g. "Vios 1.3 XLE MT"
    public static final String COL_DL_UPLOAD     = "dl_upload_path";  // gallery-picked DL image
    public static final String COL_OR_IMAGE      = "or_image_path";   // Official Receipt upload
    public static final String COL_CR_IMAGE      = "cr_image_path";   // Certificate of Registration upload

    // Appointments columns
    public static final String COL_APPT_ID         = "id";
    public static final String COL_APPT_USER_ID    = "user_id";
    public static final String COL_APPT_SERVICE    = "service_name";
    public static final String COL_APPT_DATE       = "appt_date";
    public static final String COL_APPT_TIME       = "appt_time";
    public static final String COL_APPT_PLATE      = "car_plate";
    public static final String COL_APPT_TOTAL      = "total_price";
    public static final String COL_APPT_STATUS     = "status";
    public static final String COL_APPT_RATING     = "rating";
    public static final String COL_APPT_ADMIN_NOTE  = "admin_notes";
    // New columns added in DB_VERSION 4
    public static final String COL_APPT_CAR_MODEL   = "car_model";
    public static final String COL_APPT_YEAR_MODEL  = "year_model";
    public static final String COL_APPT_FUEL_TYPE   = "fuel_type";
    public static final String COL_APPT_ORCR_STATUS = "orcr_status";
    public static final String COL_APPT_ORCR_IMAGE  = "orcr_image_path";

    private static final String CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_FIRST_NAME + " TEXT NOT NULL, " +
                    COL_LAST_NAME  + " TEXT NOT NULL, " +
                    COL_USERNAME   + " TEXT UNIQUE NOT NULL, " +
                    COL_EMAIL      + " TEXT UNIQUE NOT NULL, " +
                    COL_PHONE      + " TEXT, " +
                    COL_PASSWORD   + " TEXT NOT NULL, " +
                    COL_ROLE       + " TEXT NOT NULL DEFAULT 'customer', " +
                    COL_BIRTHDATE + " TEXT, " +
                    COL_GENDER    + " TEXT, " +
                    COL_DL_NO     + " TEXT, " +
                    COL_DL_EXPIRY + " TEXT, " +
                    COL_CL_NO     + " TEXT, " +
                    COL_CL_EXPIRY + " TEXT, " +
                    COL_LIC_IMAGE + " TEXT, " +
                    COL_LICENSE_PLATE + " TEXT, " +
                    COL_MV_FILE_NO    + " TEXT, " +
                    COL_VEHICLE_MAKE  + " TEXT, " +
                    COL_VEHICLE_MODEL + " TEXT, " +
                    COL_DL_UPLOAD     + " TEXT, " +
                    COL_OR_IMAGE      + " TEXT, " +
                    COL_CR_IMAGE      + " TEXT)";

    private static final String CREATE_APPOINTMENTS =
            "CREATE TABLE " + TABLE_APPOINTMENTS + " (" +
                    COL_APPT_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_APPT_USER_ID    + " INTEGER, " +
                    COL_APPT_SERVICE    + " TEXT NOT NULL, " +
                    COL_APPT_DATE       + " TEXT NOT NULL, " +
                    COL_APPT_TIME       + " TEXT NOT NULL, " +
                    COL_APPT_PLATE      + " TEXT, " +
                    COL_APPT_TOTAL      + " REAL, " +
                    COL_APPT_STATUS     + " TEXT DEFAULT 'pending', " +
                    COL_APPT_ADMIN_NOTE + " TEXT, " +
                    COL_APPT_RATING     + " INTEGER DEFAULT 0, " +
                    COL_APPT_CAR_MODEL   + " TEXT, " +
                    COL_APPT_YEAR_MODEL  + " TEXT, " +
                    COL_APPT_FUEL_TYPE   + " TEXT, " +
                    COL_APPT_ORCR_STATUS + " TEXT, " +
                    COL_APPT_ORCR_IMAGE  + " TEXT)";

    // Repair tasks (PMS)
    public static final String TABLE_REPAIR_TASKS  = "repair_tasks";
    public static final String COL_TASK_ID         = "id";
    public static final String COL_TASK_APPT_ID    = "appt_id";
    public static final String COL_TASK_NAME       = "task_name";
    public static final String COL_TASK_ASSIGNED   = "assigned_to";
    public static final String COL_TASK_STATUS     = "status";
    public static final String COL_TASK_SORT       = "sort_order";

    private static final String CREATE_REPAIR_TASKS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_TASKS + " (" +
                    COL_TASK_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TASK_APPT_ID  + " INTEGER NOT NULL, " +
                    COL_TASK_NAME     + " TEXT NOT NULL, " +
                    COL_TASK_ASSIGNED + " TEXT, " +
                    COL_TASK_STATUS   + " TEXT NOT NULL DEFAULT 'pending', " +
                    COL_TASK_SORT     + " INTEGER DEFAULT 0)";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_APPOINTMENTS);
        db.execSQL(CREATE_REPAIR_TASKS);
        // Seed default admin: username=admin  password=Admin@1234
        db.execSQL("INSERT INTO " + TABLE_USERS +
                " (first_name,last_name,username,email,password,role) VALUES " +
                "('Maestro','Admin','admin','admin@maestroautoworks.ph','Admin@1234','admin')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_ROLE + " TEXT NOT NULL DEFAULT 'customer'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ADMIN_NOTE + " TEXT"); } catch (Exception ignored) {}
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_USERS +
                    " (first_name,last_name,username,email,password,role) VALUES " +
                    "('Maestro','Admin','admin','admin@maestroautoworks.ph','Admin@1234','admin')");
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_REPAIR_TASKS);
            db.execSQL("UPDATE " + TABLE_USERS + " SET password='Admin@1234' WHERE username='admin' AND role='admin'");
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_CAR_MODEL   + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_YEAR_MODEL  + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_FUEL_TYPE   + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ORCR_STATUS + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ORCR_IMAGE  + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_CAR_MODEL   + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_YEAR_MODEL  + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_FUEL_TYPE   + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ORCR_STATUS + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ORCR_IMAGE  + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 6) {
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_DL_NO     + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_DL_EXPIRY + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CL_NO     + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CL_EXPIRY + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LIC_IMAGE + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_BIRTHDATE + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_GENDER    + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            // Vehicle & verification fields for Step 4 of registration
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LICENSE_PLATE + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_MV_FILE_NO    + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_VEHICLE_MAKE  + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_VEHICLE_MODEL + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_DL_UPLOAD     + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_OR_IMAGE      + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CR_IMAGE      + " TEXT"); } catch (Exception ignored) {}
        }
    }

    // ── USER OPERATIONS ──────────────────────────────────────────────────────

    public long insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FIRST_NAME, user.firstName);
        cv.put(COL_LAST_NAME,  user.lastName);
        cv.put(COL_USERNAME,   user.username);
        cv.put(COL_EMAIL,      user.email);
        cv.put(COL_PHONE,      user.phone);
        cv.put(COL_PASSWORD,   user.password);
        cv.put(COL_ROLE,       "customer");
        cv.put(COL_BIRTHDATE,  user.birthdate);
        cv.put(COL_GENDER,     user.gender);
        cv.put(COL_DL_NO,     user.driversLicenseNo);
        cv.put(COL_DL_EXPIRY, user.driversLicenseExpiry);
        cv.put(COL_CL_NO,     user.conductorsLicenseNo);
        cv.put(COL_CL_EXPIRY, user.conductorsLicenseExpiry);
        cv.put(COL_LIC_IMAGE, user.licenseImagePath);
        // Vehicle & verification fields (Step 4)
        cv.put(COL_LICENSE_PLATE, user.licensePlate);
        cv.put(COL_MV_FILE_NO,    user.mvFileNumber);
        cv.put(COL_VEHICLE_MAKE,  user.vehicleMake);
        cv.put(COL_VEHICLE_MODEL, user.vehicleModel);
        cv.put(COL_DL_UPLOAD,     user.dlUploadPath);
        cv.put(COL_OR_IMAGE,      user.orImagePath);
        cv.put(COL_CR_IMAGE,      user.crImagePath);
        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    public User loginUser(String usernameOrEmail, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null,
                "(" + COL_USERNAME + "=? OR " + COL_EMAIL + "=?) AND " + COL_PASSWORD + "=?",
                new String[]{usernameOrEmail, usernameOrEmail, password}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        db.close();
        return user;
    }

    /**
     * Looks up a user by their email address OR phone number.
     * Used by the Forgot Password flow to verify the account exists.
     *
     * @param input Email address or phone number entered by the user.
     * @return Matching {@link User}, or {@code null} if not found.
     */
    public User getUserByEmailOrPhone(String input) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null,
                COL_EMAIL + "=? OR " + COL_PHONE + "=?",
                new String[]{input, input}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        db.close();
        return user;
    }

    /**
     * Updates the password for the given user ID.
     * Used after OTP verification in the Forgot Password flow.
     *
     * @param userId      ID of the user whose password should be reset.
     * @param newPassword Plain-text new password (store as-is to match existing convention).
     * @return {@code true} if the row was updated, {@code false} otherwise.
     */
    public boolean updatePassword(int userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, cv,
                COL_ID + "=?", new String[]{String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    private User cursorToUser(Cursor c) {
        User user = new User();
        user.id        = c.getInt(c.getColumnIndexOrThrow(COL_ID));
        user.firstName = c.getString(c.getColumnIndexOrThrow(COL_FIRST_NAME));
        user.lastName  = c.getString(c.getColumnIndexOrThrow(COL_LAST_NAME));
        user.username  = c.getString(c.getColumnIndexOrThrow(COL_USERNAME));
        user.email     = c.getString(c.getColumnIndexOrThrow(COL_EMAIL));
        user.phone     = c.getString(c.getColumnIndexOrThrow(COL_PHONE));
        int roleIdx = c.getColumnIndex(COL_ROLE);
        user.role = (roleIdx >= 0) ? c.getString(roleIdx) : "customer";
        // License fields (DB_VERSION 6)
        int dlNoIdx = c.getColumnIndex(COL_DL_NO);
        if (dlNoIdx >= 0) user.driversLicenseNo = c.getString(dlNoIdx);
        int dlExIdx = c.getColumnIndex(COL_DL_EXPIRY);
        if (dlExIdx >= 0) user.driversLicenseExpiry = c.getString(dlExIdx);
        int clNoIdx = c.getColumnIndex(COL_CL_NO);
        if (clNoIdx >= 0) user.conductorsLicenseNo = c.getString(clNoIdx);
        int clExIdx = c.getColumnIndex(COL_CL_EXPIRY);
        if (clExIdx >= 0) user.conductorsLicenseExpiry = c.getString(clExIdx);
        int licImgIdx = c.getColumnIndex(COL_LIC_IMAGE);
        if (licImgIdx >= 0) user.licenseImagePath = c.getString(licImgIdx);
        // Vehicle & verification fields (DB_VERSION 8)
        int plateIdx = c.getColumnIndex(COL_LICENSE_PLATE);
        if (plateIdx >= 0) user.licensePlate = c.getString(plateIdx);
        int mvIdx = c.getColumnIndex(COL_MV_FILE_NO);
        if (mvIdx >= 0) user.mvFileNumber = c.getString(mvIdx);
        int makeIdx = c.getColumnIndex(COL_VEHICLE_MAKE);
        if (makeIdx >= 0) user.vehicleMake = c.getString(makeIdx);
        int modelIdx = c.getColumnIndex(COL_VEHICLE_MODEL);
        if (modelIdx >= 0) user.vehicleModel = c.getString(modelIdx);
        int dlUpIdx = c.getColumnIndex(COL_DL_UPLOAD);
        if (dlUpIdx >= 0) user.dlUploadPath = c.getString(dlUpIdx);
        int orIdx = c.getColumnIndex(COL_OR_IMAGE);
        if (orIdx >= 0) user.orImagePath = c.getString(orIdx);
        int crIdx = c.getColumnIndex(COL_CR_IMAGE);
        if (crIdx >= 0) user.crImagePath = c.getString(crIdx);
        // Personal info fields (DB_VERSION 7)
        int bdIdx = c.getColumnIndex(COL_BIRTHDATE);
        if (bdIdx >= 0) user.birthdate = c.getString(bdIdx);
        int gnIdx = c.getColumnIndex(COL_GENDER);
        if (gnIdx >= 0) user.gender = c.getString(gnIdx);
        return user;
    }

    public boolean usernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_ID},
                COL_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close(); db.close();
        return exists;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_ID},
                COL_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close(); db.close();
        return exists;
    }

    // ── APPOINTMENT OPERATIONS ───────────────────────────────────────────────

    public long insertAppointment(Appointment appt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_APPT_USER_ID, appt.userId);
        cv.put(COL_APPT_SERVICE, appt.serviceName);
        cv.put(COL_APPT_DATE,    appt.date);
        cv.put(COL_APPT_TIME,    appt.time);
        cv.put(COL_APPT_PLATE,   appt.carPlate);
        cv.put(COL_APPT_TOTAL,   appt.totalPrice);
        cv.put(COL_APPT_STATUS,  appt.status);
        cv.put(COL_APPT_RATING,  appt.rating);
        cv.put(COL_APPT_CAR_MODEL,   appt.carModel);
        cv.put(COL_APPT_YEAR_MODEL,  appt.yearModel);
        cv.put(COL_APPT_FUEL_TYPE,   appt.fuelType);
        cv.put(COL_APPT_ORCR_STATUS, appt.orcrStatus);
        cv.put(COL_APPT_ORCR_IMAGE,  appt.orcrImagePath);
        long id = db.insert(TABLE_APPOINTMENTS, null, cv);
        db.close();
        return id;
    }

    public List<Appointment> getAppointmentsByUser(int userId) {
        List<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_APPOINTMENTS, null,
                COL_APPT_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COL_APPT_ID + " DESC");
        while (c.moveToNext()) list.add(cursorToAppointment(c));
        c.close(); db.close();
        return list;
    }

    /** Admin: get ALL appointments joined with user name. */
    public List<Appointment> getAllAppointments(String statusFilter) {
        List<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String where = statusFilter == null || statusFilter.equals("all") ? "" :
                " WHERE a.status='" + statusFilter.replace("'","''") + "'";
        String sql =
                "SELECT a.*, u.first_name, u.last_name, u.username " +
                        "FROM " + TABLE_APPOINTMENTS + " a " +
                        "LEFT JOIN " + TABLE_USERS + " u ON u.id = a.user_id" +
                        where +
                        " ORDER BY a.id DESC";
        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            Appointment a = cursorToAppointment(c);
            int fnIdx = c.getColumnIndex("first_name");
            int lnIdx = c.getColumnIndex("last_name");
            if (fnIdx >= 0) a.customerName = c.getString(fnIdx) + " " + c.getString(lnIdx);
            list.add(a);
        }
        c.close(); db.close();
        return list;
    }

    /** Admin: update appointment status + optional admin note. */
    public void updateAppointmentStatus(int apptId, String status, String adminNote) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_APPT_STATUS, status);
        if (adminNote != null) cv.put(COL_APPT_ADMIN_NOTE, adminNote);
        db.update(TABLE_APPOINTMENTS, cv, COL_APPT_ID + "=?",
                new String[]{String.valueOf(apptId)});
        db.close();
    }

    /** Admin: count per status for dashboard stats. */
    public int countByStatus(String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_APPOINTMENTS +
                        (status.equals("all") ? "" : " WHERE status='" + status + "'"), null);
        int count = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); db.close();
        return count;
    }

    public int countCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_USERS + " WHERE role='customer'", null);
        int count = c.moveToFirst() ? c.getInt(0) : 0;
        c.close(); db.close();
        return count;
    }

    /** Admin: get today's appointments (date = today's date string yyyy-MM-dd). */
    public List<Appointment> getTodayAppointments(String todayDate) {
        List<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT a.*, u.first_name, u.last_name " +
                        "FROM " + TABLE_APPOINTMENTS + " a " +
                        "LEFT JOIN " + TABLE_USERS + " u ON u.id = a.user_id " +
                        "WHERE a.appt_date=? ORDER BY a.appt_time ASC";
        Cursor c = db.rawQuery(sql, new String[]{todayDate});
        while (c.moveToNext()) {
            Appointment a = cursorToAppointment(c);
            int fnIdx = c.getColumnIndex("first_name");
            int lnIdx = c.getColumnIndex("last_name");
            if (fnIdx >= 0) a.customerName = c.getString(fnIdx) + " " + c.getString(lnIdx);
            list.add(a);
        }
        c.close(); db.close();
        return list;
    }

    private Appointment cursorToAppointment(Cursor c) {
        Appointment a = new Appointment();
        a.id          = c.getInt(c.getColumnIndexOrThrow(COL_APPT_ID));
        a.userId      = c.getInt(c.getColumnIndexOrThrow(COL_APPT_USER_ID));
        a.serviceName = c.getString(c.getColumnIndexOrThrow(COL_APPT_SERVICE));
        a.date        = c.getString(c.getColumnIndexOrThrow(COL_APPT_DATE));
        a.time        = c.getString(c.getColumnIndexOrThrow(COL_APPT_TIME));
        a.carPlate    = c.getString(c.getColumnIndexOrThrow(COL_APPT_PLATE));
        a.totalPrice  = c.getDouble(c.getColumnIndexOrThrow(COL_APPT_TOTAL));
        a.status      = c.getString(c.getColumnIndexOrThrow(COL_APPT_STATUS));
        a.rating      = c.getInt(c.getColumnIndexOrThrow(COL_APPT_RATING));
        int anIdx = c.getColumnIndex(COL_APPT_ADMIN_NOTE);
        if (anIdx >= 0) a.adminNote = c.getString(anIdx);
        int cmIdx = c.getColumnIndex(COL_APPT_CAR_MODEL);
        if (cmIdx >= 0) a.carModel = c.getString(cmIdx);
        int ymIdx = c.getColumnIndex(COL_APPT_YEAR_MODEL);
        if (ymIdx >= 0) a.yearModel = c.getString(ymIdx);
        int ftIdx = c.getColumnIndex(COL_APPT_FUEL_TYPE);
        if (ftIdx >= 0) a.fuelType = c.getString(ftIdx);
        int osIdx = c.getColumnIndex(COL_APPT_ORCR_STATUS);
        if (osIdx >= 0) a.orcrStatus = c.getString(osIdx);
        int oiIdx = c.getColumnIndex(COL_APPT_ORCR_IMAGE);
        if (oiIdx >= 0) a.orcrImagePath = c.getString(oiIdx);
        return a;
    }

    // ── REPORTING METHODS ───────────────────────────────────────────────────

    public java.util.Map<String, Integer> getStatusCounts() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] statuses = {"pending","confirmed","completed","declined","cancelled"};
        for (String s : statuses) {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_APPOINTMENTS + " WHERE status=?", new String[]{s});
            map.put(s, c.moveToFirst() ? c.getInt(0) : 0);
            c.close();
        }
        db.close();
        return map;
    }

    public java.util.List<String[]> getTopServices(int limit) {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT service_name, COUNT(*) AS cnt, SUM(total_price) AS rev " +
                        "FROM " + TABLE_APPOINTMENTS +
                        " WHERE status IN ('confirmed','completed') " +
                        "GROUP BY service_name ORDER BY cnt DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            list.add(new String[]{c.getString(0), String.valueOf(c.getInt(1)), String.valueOf(c.getDouble(2))});
        }
        c.close(); db.close();
        return list;
    }

    public java.util.List<String[]> getDailyBookings() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT appt_date, COUNT(*) as cnt FROM " + TABLE_APPOINTMENTS +
                        " GROUP BY appt_date ORDER BY appt_date DESC LIMIT 14", null);
        while (c.moveToNext()) {
            list.add(new String[]{c.getString(0), String.valueOf(c.getInt(1))});
        }
        c.close(); db.close();
        return list;
    }

    // ── REPAIR TASKS (PMS) ──────────────────────────────────────────────────

    public java.util.List<RepairTask> getTasksForAppointment(int apptId) {
        java.util.List<RepairTask> list = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_REPAIR_TASKS, null,
                COL_TASK_APPT_ID + "=?", new String[]{String.valueOf(apptId)},
                null, null, COL_TASK_SORT + ", " + COL_TASK_ID);
        while (c.moveToNext()) {
            RepairTask t = new RepairTask();
            t.id         = c.getInt(c.getColumnIndexOrThrow(COL_TASK_ID));
            t.apptId     = c.getInt(c.getColumnIndexOrThrow(COL_TASK_APPT_ID));
            t.taskName   = c.getString(c.getColumnIndexOrThrow(COL_TASK_NAME));
            t.assignedTo = c.getString(c.getColumnIndexOrThrow(COL_TASK_ASSIGNED));
            t.status     = c.getString(c.getColumnIndexOrThrow(COL_TASK_STATUS));
            t.sortOrder  = c.getInt(c.getColumnIndexOrThrow(COL_TASK_SORT));
            list.add(t);
        }
        c.close(); db.close();
        return list;
    }

    public long insertRepairTask(int apptId, String taskName, String assignedTo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_APPT_ID, apptId);
        cv.put(COL_TASK_NAME, taskName);
        cv.put(COL_TASK_ASSIGNED, assignedTo);
        cv.put(COL_TASK_STATUS, "pending");
        long id = db.insert(TABLE_REPAIR_TASKS, null, cv);
        db.close();
        return id;
    }

    public void updateTaskStatus(int taskId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_STATUS, status);
        db.update(TABLE_REPAIR_TASKS, cv, COL_TASK_ID + "=?", new String[]{String.valueOf(taskId)});
        db.close();
    }

    public void deleteRepairTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REPAIR_TASKS, COL_TASK_ID + "=?", new String[]{String.valueOf(taskId)});
        db.close();
    }

    // ── INNER MODEL ──────────────────────────────────────────────────────────
    public static class RepairTask {
        public int id, apptId, sortOrder;
        public String taskName, assignedTo, status;
    }
}