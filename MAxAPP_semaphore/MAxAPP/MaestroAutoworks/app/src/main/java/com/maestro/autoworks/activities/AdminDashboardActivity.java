package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AdminAppointmentAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminDashboardActivity — Operations Dashboard for admin.
 * Mirrors the website's admin_dashboard.php:
 *   - Stat tiles: today's jobs, pending, confirmed, completed, total customers, all bookings
 *   - Pending requests list (with confirm/decline actions)
 *   - Today's schedule list
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        if (!session.isAdmin()) {
            Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        // Greet
        TextView tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminName.setText("Welcome, " + session.getFullName());

        loadStats();
        loadPending();
        loadToday();

        // Nav buttons
        findViewById(R.id.btnManageAppointments).setOnClickListener(v ->
            startActivity(new Intent(this, AdminAppointmentsActivity.class)));
        findViewById(R.id.btnManageServices).setOnClickListener(v ->
            startActivity(new Intent(this, AdminServicesActivity.class)));
        findViewById(R.id.btnViewReports).setOnClickListener(v ->
            startActivity(new Intent(this, AdminReportsActivity.class)));
        findViewById(R.id.btnRepairTracker).setOnClickListener(v ->
            startActivity(new Intent(this, AdminPmsActivity.class)));
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            session.logout();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        loadPending();
        loadToday();
    }

    private void loadStats() {
        int pending   = db.countByStatus("pending");
        int confirmed = db.countByStatus("confirmed");
        int completed = db.countByStatus("completed");
        int total     = db.countByStatus("all");
        int customers = db.countCustomers();
        String today  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int todayCount = db.getTodayAppointments(today).size();

        ((TextView) findViewById(R.id.statToday)).setText(String.valueOf(todayCount));
        ((TextView) findViewById(R.id.statPending)).setText(String.valueOf(pending));
        ((TextView) findViewById(R.id.statConfirmed)).setText(String.valueOf(confirmed));
        ((TextView) findViewById(R.id.statCompleted)).setText(String.valueOf(completed));
        ((TextView) findViewById(R.id.statCustomers)).setText(String.valueOf(customers));
        ((TextView) findViewById(R.id.statTotal)).setText(String.valueOf(total));
    }

    private void loadPending() {
        List<Appointment> pendingList = db.getAllAppointments("pending");
        ListView lvPending = findViewById(R.id.lvPending);
        TextView tvNoPending = findViewById(R.id.tvNoPending);

        if (pendingList.isEmpty()) {
            lvPending.setVisibility(android.view.View.GONE);
            tvNoPending.setVisibility(android.view.View.VISIBLE);
        } else {
            tvNoPending.setVisibility(android.view.View.GONE);
            lvPending.setVisibility(android.view.View.VISIBLE);
            AdminAppointmentAdapter adapter = new AdminAppointmentAdapter(this, pendingList, db, this::onResume);
            lvPending.setAdapter(adapter);
            // fix height for nested ListView
            setListViewHeightBasedOnChildren(lvPending);
        }
    }

    private void loadToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Appointment> todayList = db.getTodayAppointments(today);
        ListView lvToday = findViewById(R.id.lvToday);
        TextView tvNoToday = findViewById(R.id.tvNoToday);
        TextView tvTodayCount = findViewById(R.id.tvTodayCount);

        tvTodayCount.setText(todayList.size() + " appointment" + (todayList.size() != 1 ? "s" : ""));

        if (todayList.isEmpty()) {
            lvToday.setVisibility(android.view.View.GONE);
            tvNoToday.setVisibility(android.view.View.VISIBLE);
        } else {
            tvNoToday.setVisibility(android.view.View.GONE);
            lvToday.setVisibility(android.view.View.VISIBLE);
            AdminAppointmentAdapter adapter = new AdminAppointmentAdapter(this, todayList, db, this::onResume);
            lvToday.setAdapter(adapter);
            setListViewHeightBasedOnChildren(lvToday);
        }
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        if (listView.getAdapter() == null) return;
        int totalHeight = 0;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            android.view.View listItem = listView.getAdapter().getView(i, null, listView);
            listItem.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
            totalHeight += listItem.getMeasuredHeight();
        }
        android.view.ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listView.getAdapter().getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
