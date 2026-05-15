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
import com.maestro.autoworks.activities.AppointmentsActivity;
import com.maestro.autoworks.models.Appointment;

/**
 * NotificationHelper
 * ─────────────────────────────────────────────────────────────────────────────
 * Posts an Android push notification to the customer's device when the admin
 * confirms their booking. Tapping the notification opens AppointmentsActivity.
 *
 * Channel: BOOKING_CHANNEL_ID  (importance = HIGH so it appears as a heads-up)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class NotificationHelper {

    public static final String BOOKING_CHANNEL_ID   = "maestro_booking_channel";
    public static final String BOOKING_CHANNEL_NAME = "Booking Confirmations";

    /**
     * Creates the notification channel (safe to call repeatedly; no-op on < API 26).
     * Call once in Application.onCreate() or the first time you need to post.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BOOKING_CHANNEL_ID,
                    BOOKING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Appointment booking status updates from Maestro Autoworks");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
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

        try {
            NotificationManagerCompat.from(context)
                    .notify(appointment.id, builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted — silently ignore;
            // the in-app notification badge in AppointmentsActivity still works.
            android.util.Log.w("NotificationHelper",
                    "POST_NOTIFICATIONS not granted; skipping push notification.");
        }
    }

    private static String nullSafe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}
