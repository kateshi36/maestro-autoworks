package com.maestro.autoworks.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.maestro.autoworks.models.Appointment;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * ReceiptEmailSender
 * ─────────────────────────────────────────────────────────────────────────────
 * Sends an electronic booking confirmation receipt to the customer's registered
 * email address once an admin approves their appointment.
 *
 * Reuses the same Gmail SMTP credentials already configured in EmailOtpSender.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ReceiptEmailSender {

    private static final String TAG = "ReceiptEmailSender";

    // ── Same credentials used by EmailOtpSender ───────────────────────────────
    private static final String SENDER_EMAIL        = "ilardeclyde@gmail.com";
    private static final String SENDER_APP_PASSWORD = "yhhe vtmi ssmd fbdm";
    // ─────────────────────────────────────────────────────────────────────────

    /** Callback interface for send result. */
    public interface SendCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Sends a booking confirmation receipt email on a background thread.
     *
     * @param recipientEmail  User's registered email address.
     * @param recipientName   User's first name for the greeting.
     * @param appointment     The confirmed appointment details.
     * @param callback        UI-thread callback for success / failure.
     */
    public static void sendReceipt(String recipientEmail,
                                   String recipientName,
                                   Appointment appointment,
                                   SendCallback callback) {
        new SendReceiptTask(recipientEmail, recipientName, appointment, callback).execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Background task
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // AsyncTask is fine for this small utility
    private static class SendReceiptTask extends AsyncTask<Void, Void, String> {

        private final String      to;
        private final String      name;
        private final Appointment appt;
        private final SendCallback cb;
        private       boolean     success = false;

        SendReceiptTask(String to, String name, Appointment appt, SendCallback cb) {
            this.to   = to;
            this.name = name;
            this.appt = appt;
            this.cb   = cb;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
                    }
                });

                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL, "Maestro Autoworks"));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                msg.setSubject("Booking Confirmed — Maestro Autoworks Receipt #" + appt.id);
                msg.setText(buildReceiptBody(name, appt));

                Transport.send(msg);
                success = true;
                return null;

            } catch (MessagingException e) {
                Log.e(TAG, "SMTP error sending receipt", e);
                return "SMTP error: " + e.getMessage();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending receipt", e);
                return "Unexpected error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String errorMsg) {
            if (success) {
                cb.onSuccess();
            } else {
                cb.onFailure(errorMsg != null ? errorMsg : "Failed to send receipt.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Receipt body builder
    // ─────────────────────────────────────────────────────────────────────────

    private static String buildReceiptBody(String name, Appointment a) {
        String greeting = (name != null && !name.isEmpty()) ? name : "Valued Customer";
        String line     = "─────────────────────────────────────";

        return  "Hi " + greeting + ",\n\n"
              + "Great news! Your booking request has been CONFIRMED.\n"
              + "Below is your official electronic receipt.\n\n"
              + line + "\n"
              + "     MAESTRO AUTOWORKS\n"
              + "     BOOKING RECEIPT\n"
              + line + "\n\n"
              + "Receipt No. : #" + a.id + "\n"
              + "Status      : CONFIRMED\n\n"
              + "── SERVICE DETAILS ──────────────────\n"
              + "Service     : " + nullSafe(a.serviceName) + "\n"
              + "Date        : " + nullSafe(a.date) + "\n"
              + "Time Slot   : " + nullSafe(a.time) + "\n\n"
              + "── VEHICLE DETAILS ──────────────────\n"
              + "Car Model   : " + nullSafe(a.carModel) + "\n"
              + "Year        : " + nullSafe(a.yearModel) + "\n"
              + "Fuel Type   : " + nullSafe(a.fuelType) + "\n"
              + "Plate No.   : " + nullSafe(a.carPlate) + "\n\n"
              + "── PAYMENT SUMMARY ──────────────────\n"
              + String.format("Total       : \u20b1%.2f\n\n", a.totalPrice)
              + (a.adminNote != null && !a.adminNote.isEmpty()
                  ? "── ADMIN NOTE ───────────────────────\n" + a.adminNote + "\n\n"
                  : "")
              + line + "\n\n"
              + "Please arrive 10 minutes before your scheduled time.\n"
              + "Bring a valid ID and your vehicle's OR/CR.\n\n"
              + "For questions, reply to this email or visit us.\n\n"
              + "— Maestro Autoworks Team";
    }

    private static String nullSafe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}
