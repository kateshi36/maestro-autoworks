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
    private static final int    DB_VERSION = 2;

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
    public static final String COL_APPT_ADMIN_NOTE = "admin_notes";

    private static final String CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
            COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_FIRST_NAME + " TEXT NOT NULL, " +
            COL_LAST_NAME  + " TEXT NOT NULL, " +
            COL_USERNAME   + " TEXT UNIQUE NOT NULL, " +
            COL_EMAIL      + " TEXT UNIQUE NOT NULL, " +
            COL_PHONE      + " TEXT, " +
            COL_PASSWORD   + " TEXT NOT NULL, " +
            COL_ROLE       + " TEXT NOT NULL DEFAULT 'customer')";

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
            COL_APPT_RATING     + " INTEGER DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_APPOINTMENTS);
        // Seed default admin: username=admin  password=admin123
        db.execSQL("INSERT INTO " + TABLE_USERS +
                " (first_name,last_name,username,email,password,role) VALUES " +
                "('Maestro','Admin','admin','admin@maestroautoworks.ph','admin123','admin')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add role column if upgrading from v1
            try { db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_ROLE + " TEXT NOT NULL DEFAULT 'customer'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " + COL_APPT_ADMIN_NOTE + " TEXT"); } catch (Exception ignored) {}
            // Ensure admin account exists
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_USERS +
                    " (first_name,last_name,username,email,password,role) VALUES " +
                    "('Maestro','Admin','admin','admin@maestroautoworks.ph','admin123','admin')");
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
        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null,
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        db.close();
        return user;
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
        return a;
    }
}
