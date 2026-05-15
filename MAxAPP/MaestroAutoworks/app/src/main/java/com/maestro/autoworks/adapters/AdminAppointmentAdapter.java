package com.maestro.autoworks.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.User;
import com.maestro.autoworks.utils.NotificationHelper;
import com.maestro.autoworks.utils.ReceiptEmailSender;

import java.util.List;

/** ListView adapter for admin's appointment list with confirm/decline/complete actions. */
public class AdminAppointmentAdapter extends ArrayAdapter<Appointment> {

    private final DatabaseHelper db;
    private final Runnable onRefresh;

    public AdminAppointmentAdapter(Context ctx, List<Appointment> list, DatabaseHelper db, Runnable onRefresh) {
        super(ctx, 0, list);
        this.db        = db;
        this.onRefresh = onRefresh;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_admin_appointment, parent, false);

        Appointment a = getItem(position);

        ((TextView) convertView.findViewById(R.id.tvAdminApptCustomer)).setText(
            (a.customerName != null && !a.customerName.trim().isEmpty()) ? a.customerName : "User #" + a.userId);
        ((TextView) convertView.findViewById(R.id.tvAdminApptService)).setText(a.serviceName);
        ((TextView) convertView.findViewById(R.id.tvAdminApptDate)).setText(a.date + "  " + a.time);
        ((TextView) convertView.findViewById(R.id.tvAdminApptPlate)).setText(
            (a.carPlate != null && !a.carPlate.isEmpty()) ? "Plate: " + a.carPlate : "");
        ((TextView) convertView.findViewById(R.id.tvAdminApptPrice)).setText(
            String.format("₱%.2f", a.totalPrice));

        TextView tvStatus = convertView.findViewById(R.id.tvAdminApptStatus);
        tvStatus.setText(a.status.toUpperCase());
        int statusColor;
        switch (a.status) {
            case "confirmed":  statusColor = 0xFF4CAF7D; break;
            case "completed":  statusColor = 0xFF7B61FF; break;
            case "declined":
            case "cancelled":  statusColor = 0xFFE05252; break;
            default:           statusColor = 0xFFF5A623; break; // pending
        }
        tvStatus.setTextColor(statusColor);

        Button btnConfirm  = convertView.findViewById(R.id.btnAdminConfirm);
        Button btnDecline  = convertView.findViewById(R.id.btnAdminDecline);
        Button btnComplete = convertView.findViewById(R.id.btnAdminComplete);

        btnConfirm.setVisibility(View.GONE);
        btnDecline.setVisibility(View.GONE);
        btnComplete.setVisibility(View.GONE);

        if ("pending".equals(a.status)) {
            btnConfirm.setVisibility(View.VISIBLE);
            btnDecline.setVisibility(View.VISIBLE);
        } else if ("confirmed".equals(a.status)) {
            btnComplete.setVisibility(View.VISIBLE);
        }

        btnConfirm.setOnClickListener(v -> showActionDialog(a, "confirmed", "Confirm Appointment",
            "Confirm booking for " + displayName(a) + " — " + a.serviceName + "?"));
        btnDecline.setOnClickListener(v -> showActionDialog(a, "declined", "Decline Appointment",
            "Decline booking for " + displayName(a) + " — " + a.serviceName + "?"));
        btnComplete.setOnClickListener(v -> {
            db.updateAppointmentStatus(a.id, "completed", null);
            Toast.makeText(getContext(), "Marked as Completed.", Toast.LENGTH_SHORT).show();
            onRefresh.run();
        });

        return convertView;
    }

    private String displayName(Appointment a) {
        return (a.customerName != null && !a.customerName.trim().isEmpty()) ? a.customerName : "User #" + a.userId;
    }

    private void showActionDialog(Appointment a, String newStatus, String title, String message) {
        View dlgView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_action, null);
        ((TextView) dlgView.findViewById(R.id.tvDialogMessage)).setText(message);
        EditText etNote = dlgView.findViewById(R.id.etAdminNote);

        new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setView(dlgView)
            .setPositiveButton(newStatus.equals("confirmed") ? "✓ Confirm" : "✗ Decline", (dlg, w) -> {
                String note = etNote.getText().toString().trim();
                db.updateAppointmentStatus(a.id, newStatus, note.isEmpty() ? null : note);

                // ── Step 5: Receipt & Notification (confirmation only) ────────
                if ("confirmed".equals(newStatus)) {
                    // Attach the admin note to the appointment object for the receipt
                    a.status    = "confirmed";
                    a.adminNote = note.isEmpty() ? null : note;

                    // Look up the customer's email from the users table
                    User customer = db.getUserById(a.userId);
                    if (customer != null) {
                        // 1. Send receipt email
                        ReceiptEmailSender.sendReceipt(
                            customer.email,
                            customer.firstName,
                            a,
                            new ReceiptEmailSender.SendCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(getContext(),
                                        "Receipt emailed to " + customer.email,
                                        Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(String err) {
                                    Toast.makeText(getContext(),
                                        "Email failed: " + err,
                                        Toast.LENGTH_LONG).show();
                                }
                            }
                        );

                        // 2. In-app notification (stored in SQLite, shown in AppointmentsActivity)
                        String notifTitle   = "Booking Confirmed — Receipt #" + a.id;
                        String notifMessage = buildInAppReceiptMessage(a);
                        db.insertNotification(a.userId, a.id, notifTitle, notifMessage);

                        // 3. Android push notification (visible in status bar)
                        NotificationHelper.postBookingConfirmed(getContext(), a);
                    }
                }
                // ─────────────────────────────────────────────────────────────

                Toast.makeText(getContext(),
                    "Appointment " + newStatus + ".", Toast.LENGTH_SHORT).show();
                onRefresh.run();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /** Builds the in-app notification message shown in AppointmentsActivity. */
    private String buildInAppReceiptMessage(Appointment a) {
        return  "Your booking has been confirmed!\n\n"
              + "Receipt No. : #" + a.id + "\n"
              + "Service     : " + nullSafe(a.serviceName) + "\n"
              + "Date        : " + nullSafe(a.date) + "\n"
              + "Time        : " + nullSafe(a.time) + "\n"
              + "Plate No.   : " + nullSafe(a.carPlate) + "\n"
              + String.format("Total       : \u20b1%.2f", a.totalPrice)
              + (a.adminNote != null && !a.adminNote.isEmpty()
                    ? "\n\nAdmin Note  : " + a.adminNote : "")
              + "\n\nA receipt has also been sent to your registered email.";
    }

    private String nullSafe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}
