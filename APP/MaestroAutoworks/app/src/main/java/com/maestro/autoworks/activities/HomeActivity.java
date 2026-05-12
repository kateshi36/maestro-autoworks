package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.SessionManager;

/**
 * HomeActivity — Dashboard after login.
 * Demonstrates: Intent to all other activities.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + session.getFullName() + "! 👋");

        Button btnBook         = findViewById(R.id.btnBook);
        Button btnAppointments = findViewById(R.id.btnAppointments);
        Button btnServices     = findViewById(R.id.btnServices);
        Button btnSearch       = findViewById(R.id.btnSearch);
        Button btnAbout        = findViewById(R.id.btnAbout);
        Button btnWeb          = findViewById(R.id.btnWeb);
        Button btnLogout       = findViewById(R.id.btnLogout);

        // Intent: BookActivity
        btnBook.setOnClickListener(v ->
            startActivity(new Intent(this, BookActivity.class)));

        // Intent: AppointmentsActivity
        btnAppointments.setOnClickListener(v ->
            startActivity(new Intent(this, AppointmentsActivity.class)));

        // Intent: ServicesActivity
        btnServices.setOnClickListener(v ->
            startActivity(new Intent(this, ServicesActivity.class)));

        // Intent: SearchActivity
        btnSearch.setOnClickListener(v ->
            startActivity(new Intent(this, SearchActivity.class)));

        // Intent: AboutActivity
        btnAbout.setOnClickListener(v ->
            startActivity(new Intent(this, AboutActivity.class)));

        // Intent: WebViewActivity

        // Logout
        btnLogout.setOnClickListener(v -> {
            session.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
