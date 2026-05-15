package com.maestro.autoworks.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AppointmentAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.DatabaseHelper.AppNotification;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;

import java.util.List;

/**
 * AppointmentsActivity — My Appointments + In-App Notifications.
 *
 * Step 5 additions:
 *  - Notification bell badge shows unread count.
 *  - Tapping the badge opens a dialog listing all notifications (newest first).
 *  - Tapping any individual notification shows the full receipt message and marks it read.
 *  - "Mark all as read" clears the badge.
 */
public class AppointmentsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private TextView       tvNotifBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        session = new SessionManager(this);
        db      = new DatabaseHelper(this);

        ListView listAppts = findViewById(R.id.listAppointments);
        TextView tvEmpty   = findViewById(R.id.tvEmpty);
        tvNotifBadge       = findViewById(R.id.tvNotifBadge);

        // Appointments list
        List<Appointment> appointments = db.getAppointmentsByUser(session.getUserId());

        if (appointments.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listAppts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            AppointmentAdapter adapter = new AppointmentAdapter(this, appointments);
            listAppts.setAdapter(adapter);
        }

        // Notification bell
        refreshNotifBadge();
        tvNotifBadge.setOnClickListener(v -> openNotificationsDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotifBadge();
    }

    private void refreshNotifBadge() {
        int unread = db.countUnreadNotifications(session.getUserId());
        if (unread > 0) {
            tvNotifBadge.setVisibility(View.VISIBLE);
            tvNotifBadge.setText("\uD83D\uDD14 " + unread + " new notification" + (unread > 1 ? "s" : ""));
        } else {
            tvNotifBadge.setVisibility(View.GONE);
        }
    }

    private void openNotificationsDialog() {
        List<AppNotification> notifications =
                db.getNotificationsForUser(session.getUserId());

        if (notifications.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setMessage("You have no notifications yet.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String[] items = new String[notifications.size()];
        for (int i = 0; i < notifications.size(); i++) {
            AppNotification n = notifications.get(i);
            items[i] = (n.isRead ? "   " : "\uD83D\uDD35 ") + n.title + "\n      " + n.createdAt;
        }

        new AlertDialog.Builder(this)
            .setTitle("Notifications")
            .setItems(items, (dialog, which) ->
                    openNotificationDetail(notifications.get(which)))
            .setNeutralButton("Mark All Read", (d, w) -> {
                db.markAllNotificationsRead(session.getUserId());
                refreshNotifBadge();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void openNotificationDetail(AppNotification notif) {
        db.markNotificationRead(notif.id);
        refreshNotifBadge();

        new AlertDialog.Builder(this)
            .setTitle(notif.title)
            .setMessage(notif.message)
            .setPositiveButton("OK", null)
            .show();
    }
}
