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
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "maestro_autoworks.db";
    private static final int    DB_VERSION = 1;

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

    // Appointments columns
    public static final String COL_APPT_ID       = "id";
    public static final String COL_APPT_USER_ID  = "user_id";
    public static final String COL_APPT_SERVICE  = "service_name";
    public static final String COL_APPT_DATE     = "appt_date";
    public static final String COL_APPT_TIME     = "appt_time";
    public static final String COL_APPT_PLATE    = "car_plate";
    public static final String COL_APPT_TOTAL    = "total_price";
    public static final String COL_APPT_STATUS   = "status";
    public static final String COL_APPT_RATING   = "rating";

    private static final String CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
            COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_FIRST_NAME + " TEXT NOT NULL, " +
            COL_LAST_NAME  + " TEXT NOT NULL, " +
            COL_USERNAME   + " TEXT UNIQUE NOT NULL, " +
            COL_EMAIL      + " TEXT UNIQUE NOT NULL, " +
            COL_PHONE      + " TEXT, " +
            COL_PASSWORD   + " TEXT NOT NULL)";

    private static final String CREATE_APPOINTMENTS =
            "CREATE TABLE " + TABLE_APPOINTMENTS + " (" +
            COL_APPT_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_APPT_USER_ID + " INTEGER, " +
            COL_APPT_SERVICE + " TEXT NOT NULL, " +
            COL_APPT_DATE    + " TEXT NOT NULL, " +
            COL_APPT_TIME    + " TEXT NOT NULL, " +
            COL_APPT_PLATE   + " TEXT, " +
            COL_APPT_TOTAL   + " REAL, " +
            COL_APPT_STATUS  + " TEXT DEFAULT 'pending', " +
            COL_APPT_RATING  + " INTEGER DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_APPOINTMENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
        onCreate(db);
    }

    // ── USER OPERATIONS ──────────────────────────────────────────────────────

    /** Insert a new user. Returns row id or -1 on failure. */
    public long insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FIRST_NAME, user.firstName);
        cv.put(COL_LAST_NAME,  user.lastName);
        cv.put(COL_USERNAME,   user.username);
        cv.put(COL_EMAIL,      user.email);
        cv.put(COL_PHONE,      user.phone);
        cv.put(COL_PASSWORD,   user.password);
        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    /** Check login credentials. Returns User or null. */
    public User loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null,
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = new User();
            user.id        = c.getInt(c.getColumnIndexOrThrow(COL_ID));
            user.firstName = c.getString(c.getColumnIndexOrThrow(COL_FIRST_NAME));
            user.lastName  = c.getString(c.getColumnIndexOrThrow(COL_LAST_NAME));
            user.username  = c.getString(c.getColumnIndexOrThrow(COL_USERNAME));
            user.email     = c.getString(c.getColumnIndexOrThrow(COL_EMAIL));
            user.phone     = c.getString(c.getColumnIndexOrThrow(COL_PHONE));
        }
        c.close();
        db.close();
        return user;
    }

    /** Check if username already exists. */
    public boolean usernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_ID},
                COL_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        db.close();
        return exists;
    }

    /** Check if email already exists. */
    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_ID},
                COL_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        db.close();
        return exists;
    }

    // ── APPOINTMENT OPERATIONS ───────────────────────────────────────────────

    /** Insert a new appointment. */
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

    /** Get all appointments for a user. */
    public List<Appointment> getAppointmentsByUser(int userId) {
        List<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_APPOINTMENTS, null,
                COL_APPT_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COL_APPT_ID + " DESC");
        if (c.moveToFirst()) {
            do {
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
                list.add(a);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}
