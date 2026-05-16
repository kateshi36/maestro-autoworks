package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.db.SessionManager;

/**
 * MainActivity — pure session router, no UI of its own.
 *
 * On launch it checks the saved session and forwards immediately:
 *   • Admin user  → AdminDashboardActivity
 *   • Regular user → HomeActivity
 *   • Not logged in → LoginActivity
 *
 * activity_main.xml is intentionally removed; this activity never calls setContentView.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);

        final Intent next;
        if (session.isLoggedIn()) {
            if (session.isAdmin()) {
                next = new Intent(this, AdminDashboardActivity.class);
            } else {
                next = new Intent(this, HomeActivity.class);
            }
        } else {
            next = new Intent(this, LoginActivity.class);
        }

        next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(next);
        finish();
    }
}
