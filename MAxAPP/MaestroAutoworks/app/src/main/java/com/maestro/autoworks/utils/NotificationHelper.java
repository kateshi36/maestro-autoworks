package com.maestro.autoworks.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.maestro.autoworks.R;
import com.maestro.autoworks.activities.AdminDashboardActivity;
import com.maestro.autoworks.activities.AppointmentsActivity;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.User;

/**
 * NotificationHelper
 * ─────────────────────────────────────────────────────────────────────────────
 * Posts Android push notifications for both the customer and the admin.
 *
 * Customer channel : BOOKING_CHANNEL_ID  — status confirmations/declines
 * Admin channel    : ADMIN_CHANNEL_ID    — new bookings & status changes
 *
 * Both channels use IMPORTANCE_HIGH so they appear as heads-up banners.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class NotificationHelper {

    public static final String BOOKING_CHANNEL_ID   = "maestro_booking_channel";
    public static final String BOOKING_CHANNEL_NAME = "Booking Confirmations";

    public static final String ADMIN_CHANNEL_ID   = "maestro_admin_channel";
    public static final String ADMIN_CHANNEL_NAME = "Admin — Booking Alerts";

    /**
     * Creates both notification channels (safe to call repeatedly; no-op on < API 26).
     * Call once in Application.onCreate() or before posting any notification.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            // Customer channel
            NotificationChannel customerCh = new NotificationChannel(
                    BOOKING_CHANNEL_ID,
                    BOOKING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            customerCh.setDescription("Appointment booking status updates from Maestro Autoworks");
            nm.createNotificationChannel(customerCh);

            // Admin channel
            NotificationChannel adminCh = new NotificationChannel(
                    ADMIN_CHANNEL_ID,
                    ADMIN_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            adminCh.setDescription("New bookings and appointment status changes for the admin");
            nm.createNotificationChannel(adminCh);
        }
    }

    // ── ADMIN PUSH NOTIFICATIONS ─────────────────────────────────────────────

    /**
     * Posts a heads-up push notification to the admin's device when a customer
     * submits a new booking. Tapping the notification opens AdminDashboardActivity.
     *
     * Call this from BookActivity immediately after a successful insertAppointment().
     *
     * @param context      Application or Activity context.
     * @param appointment  The newly created appointment.
     */
    public static void postNewBookingToAdmin(Context context, Appointment appointment) {
        createChannel(context);

        Intent tapIntent = new Intent(context, AdminDashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Use a negative request code offset to avoid colliding with customer notification IDs
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                -(appointment.id),           // unique, non-colliding request code
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Stage 3: include customer name in both the summary line and the expanded body
        String customerDisplay = (appointment.customerName != null
                && !appointment.customerName.trim().isEmpty())
                ? appointment.customerName.trim()
                : "Customer #" + appointment.userId;

        String bigText =
                "A new appointment request is waiting for your review.\n\n"
                        + "Customer: " + customerDisplay + "\n"
                        + "Service : " + nullSafe(appointment.serviceName) + "\n"
                        + "Date    : " + nullSafe(appointment.date) + "\n"
                        + "Time    : " + nullSafe(appointment.time) + "\n"
                        + "Plate   : " + nullSafe(appointment.carPlate) + "\n"
                        + String.format("Total   : \u20b1%.2f", appointment.totalPrice);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New Booking — " + customerDisplay + " (#" + appointment.id + ")")
                .setContentText(nullSafe(appointment.serviceName) + " · " + nullSafe(appointment.date))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        postSafely(context, appointment.id + 100_000, builder);   // offset avoids customer ID clash
    }

    /**
     * Posts a heads-up push notification to the admin's device when an
     * appointment status changes (e.g. customer cancels, or admin marks complete).
     *
     * @param context      Application or Activity context.
     * @param appointment  The appointment whose status changed.
     * @param newStatus    The new status string (e.g. "declined", "completed").
     */
    public static void postStatusChangeToAdmin(Context context, Appointment appointment,
                                               String newStatus) {
        createChannel(context);

        Intent tapIntent = new Intent(context, AdminDashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                -(appointment.id + 200_000),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String statusLabel = newStatus.substring(0, 1).toUpperCase()
                + newStatus.substring(1).toLowerCase();

        String bigText =
                "Appointment #" + appointment.id + " has been marked as \"" + statusLabel + "\".\n\n"
                        + "Service : " + nullSafe(appointment.serviceName) + "\n"
                        + "Date    : " + nullSafe(appointment.date) + "\n"
                        + "Plate   : " + nullSafe(appointment.carPlate);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Appointment " + statusLabel + " — #" + appointment.id)
                .setContentText(nullSafe(appointment.serviceName) + " · " + nullSafe(appointment.date))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        postSafely(context, appointment.id + 200_000, builder);
    }

    /** Internal helper — catches SecurityException if POST_NOTIFICATIONS not granted. */
    private static void postSafely(Context context, int notifId,
                                   NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } catch (SecurityException e) {
            android.util.Log.w("NotificationHelper",
                    "POST_NOTIFICATIONS not granted; skipping push (id=" + notifId + ").");
        }
    }

    /**
     * Posts a heads-up push notification to the admin's device when a new user
     * completes registration and submits documents for review.
     * Tapping the notification opens AdminDashboardActivity.
     *
     * Call this from RegisterActivity after a successful insertUser().
     *
     * @param context  Application or Activity context.
     * @param user     The newly registered user.
     */
    public static void postNewRegistrationToAdmin(Context context, User user) {
        createChannel(context);

        Intent tapIntent = new Intent(context, AdminDashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Use a large fixed offset so the request code never collides with appointment IDs
        int requestCode = (user.username != null ? user.username.hashCode() : 0) + 300_000;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String displayName = (user.firstName != null ? user.firstName : "")
                + " " + (user.lastName != null ? user.lastName : "");
        displayName = displayName.trim();
        if (displayName.isEmpty()) displayName = nullSafe(user.username);

        String bigText =
                "A new user has registered and submitted documents for review.\n\n"
                        + "Name     : " + displayName + "\n"
                        + "Username : @" + nullSafe(user.username) + "\n"
                        + "Email    : " + nullSafe(user.email) + "\n"
                        + "Phone    : " + nullSafe(user.phone);

        int notifId = Math.abs(requestCode);   // unique, stable per username

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New Registration \u2014 @" + nullSafe(user.username))
                .setContentText(displayName + " has submitted documents for review.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        postSafely(context, notifId, builder);
    }

    /**
     * Posts a "Booking Confirmed" push notification to the user's device.
     * Safe to call from any thread (NotificationManagerCompat handles threading).
     *
     * @param context      Application or Activity context.
     * @param appointment  The confirmed appointment (used for receipt details).
     */
    public static void postBookingConfirmed(Context context, Appointment appointment) {
        createChannel(context);

        // Tapping the notification opens the user's appointments list
        Intent tapIntent = new Intent(context, AppointmentsActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appointment.id,          // unique request code per appointment
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String bigText =
                "Your appointment has been confirmed!\n\n"
                        + "Service : " + nullSafe(appointment.serviceName) + "\n"
                        + "Date    : " + nullSafe(appointment.date) + "\n"
                        + "Time    : " + nullSafe(appointment.time) + "\n"
                        + String.format("Total   : \u20b1%.2f", appointment.totalPrice)
                        + "\n\nA receipt has been sent to your email.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BOOKING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Booking Confirmed — Maestro Autoworks")
                .setContentText("Your " + nullSafe(appointment.serviceName) + " appointment is confirmed!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        postSafely(context, appointment.id, builder);
    }

    private static String nullSafe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}