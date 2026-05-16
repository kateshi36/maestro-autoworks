package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

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

        drawerLayout = findViewById(R.id.drawerLayout);

        // ── Time-aware greeting ──────────────────────────────────────────
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12)      tvGreeting.setText("GOOD MORNING! \uD83D\uDC4B");
        else if (hour < 18) tvGreeting.setText("GOOD AFTERNOON! \uD83D\uDC4B");
        else                tvGreeting.setText("GOOD EVENING! \uD83D\uDC4B");

        // ── First name in amber ──────────────────────────────────────────
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        String fullName = session.getFullName();
        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0] : (fullName != null ? fullName : "User");
        tvWelcome.setText(firstName + "!");

        // ── Drawer header — full name + username ─────────────────────────
        TextView drawerUserName  = findViewById(R.id.drawerUserName);
        TextView drawerUserEmail = findViewById(R.id.drawerUserEmail);
        drawerUserName.setText(fullName != null ? fullName : "User");
        drawerUserEmail.setText(session.getUsername());

        // ── Hero CTA buttons ─────────────────────────────────────────────
        Button btnBook     = findViewById(R.id.btnBook);
        Button btnServices = findViewById(R.id.btnServices);
        btnBook.setOnClickListener(v -> startActivity(new Intent(this, BookActivity.class)));
        btnServices.setOnClickListener(v -> startActivity(new Intent(this, ServicesActivity.class)));

        // ── Topbar icons ─────────────────────────────────────────────────
        findViewById(R.id.btnHamburger).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        // ── Bottom nav bar ───────────────────────────────────────────────
        LinearLayout navHome         = findViewById(R.id.navHome);
        LinearLayout navServices     = findViewById(R.id.navServices);
        LinearLayout navBook         = findViewById(R.id.navBook);
        LinearLayout navAppointments = findViewById(R.id.navAppointments);
        LinearLayout navProfile      = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> { /* already on Home */ });
        navServices.setOnClickListener(v ->
                startActivity(new Intent(this, ServicesActivity.class)));
        navBook.setOnClickListener(v ->
                startActivity(new Intent(this, BookActivity.class)));
        navAppointments.setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        // ── Drawer nav items ─────────────────────────────────────────────
        findViewById(R.id.drawerNavHome).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // Already on Home — no navigation needed
        });

        findViewById(R.id.drawerNavServices).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ServicesActivity.class));
        });

        findViewById(R.id.drawerNavBook).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, BookActivity.class));
        });

        findViewById(R.id.drawerNavAppointments).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AppointmentsActivity.class));
        });

        findViewById(R.id.drawerNavProfile).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AboutActivity.class));
        });

        findViewById(R.id.drawerNavLogout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            session.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // ── Hidden stubs preserved for panel migration ───────────────────
        Button btnAppointments = findViewById(R.id.btnAppointments);
        Button btnSearch       = findViewById(R.id.btnSearch);
        Button btnAbout        = findViewById(R.id.btnAbout);
        Button btnLogout       = findViewById(R.id.btnLogout);

        btnAppointments.setOnClickListener(v -> startActivity(new Intent(this, AppointmentsActivity.class)));
        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        btnAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        btnLogout.setOnClickListener(v -> {
            session.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    // ── Close drawer on back press if it is open ─────────────────────────
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

