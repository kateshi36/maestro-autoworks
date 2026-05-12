package com.maestro.autoworks.db;

import android.content.Context;
import android.content.SharedPreferences;

/** Manages login session using SharedPreferences. */
public class SessionManager {

    private static final String PREF_NAME   = "maestro_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME    = "full_name";
    private static final String KEY_UNAME   = "username";
    private static final String KEY_ROLE    = "role";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(int userId, String fullName, String username, String role) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_NAME,  fullName);
        editor.putString(KEY_UNAME, username);
        editor.putString(KEY_ROLE,  role);
        editor.apply();
    }

    /** Legacy overload kept for backward compatibility. */
    public void saveSession(int userId, String fullName, String username) {
        saveSession(userId, fullName, username, "customer");
    }

    public boolean isLoggedIn() {
        return prefs.getInt(KEY_USER_ID, -1) != -1;
    }

    public int getUserId()    { return prefs.getInt(KEY_USER_ID, -1); }
    public String getFullName() { return prefs.getString(KEY_NAME, ""); }
    public String getUsername() { return prefs.getString(KEY_UNAME, ""); }
    public String getRole()     { return prefs.getString(KEY_ROLE, "customer"); }
    public boolean isAdmin()    { return "admin".equals(getRole()); }

    public void logout() { editor.clear().apply(); }
}
