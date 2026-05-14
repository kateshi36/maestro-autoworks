package com.maestro.autoworks.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * EmailOtpSender
 * ─────────────────────────────────────────────────────────────────────────────
 * Generates a 6-digit OTP and sends it to the user's registered email address
 * via Gmail SMTP on a background thread.
 *
 * ⚠️  SETUP REQUIRED before use:
 *   1. Enable 2-Step Verification on your Gmail account.
 *   2. Create an App Password at https://myaccount.google.com/apppasswords
 *      (Select app: Mail, device: Android → Generate).
 *   3. Replace SENDER_EMAIL and SENDER_APP_PASSWORD below with your values.
 *   4. Add JavaMail dependencies to app/build.gradle:
 *
 *       implementation 'com.sun.mail:android-mail:1.6.7'
 *       implementation 'com.sun.mail:android-activation:1.6.7'
 *
 *   5. Add INTERNET permission to AndroidManifest.xml (if not already present):
 *       <uses-permission android:name="android.permission.INTERNET"/>
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class EmailOtpSender {

    private static final String TAG = "EmailOtpSender";

    // ── CONFIGURE THESE ───────────────────────────────────────────────────────
    /** The Gmail address that sends OTP emails. */
    private static final String SENDER_EMAIL        = "ilardeclyde@gmail.com";
    /** Gmail App Password (NOT your regular Gmail password). */
    private static final String SENDER_APP_PASSWORD = "yhhe vtmi ssmd fbdm";
    // ─────────────────────────────────────────────────────────────────────────

    /** Callback interface for send result. */
    public interface SendCallback {
        void onSuccess(String otp);
        void onFailure(String errorMessage);
    }

    /**
     * Generates a 6-digit OTP, sends it to {@code recipientEmail}, then calls
     * the callback on the calling thread's handler (via AsyncTask).
     *
     * @param recipientEmail  The user's registered email address.
     * @param recipientName   First name used in the email greeting.
     * @param callback        Called on the UI thread with result.
     */
    public static void sendOtp(String recipientEmail,
                               String recipientName,
                               SendCallback callback) {

        String otp = generateOtp();
        new SendEmailTask(recipientEmail, recipientName, otp, callback).execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP generation
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a zero-padded 6-digit OTP string. */
    public static String generateOtp() {
        int code = 100_000 + new Random().nextInt(900_000);
        return String.valueOf(code);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Background email task
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation") // AsyncTask is fine for a small utility
    private static class SendEmailTask extends AsyncTask<Void, Void, String> {

        private final String      to;
        private final String      name;
        private final String      otp;
        private final SendCallback cb;
        private       boolean     success = false;

        SendEmailTask(String to, String name, String otp, SendCallback cb) {
            this.to   = to;
            this.name = name;
            this.otp  = otp;
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
                msg.setSubject("Your Maestro Autoworks Verification Code");
                msg.setText(buildEmailBody(name, otp));

                Transport.send(msg);
                success = true;
                return null;

            } catch (MessagingException e) {
                Log.e(TAG, "SMTP error", e);
                return "SMTP error: " + e.getMessage();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending email", e);
                return "Unexpected error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String errorMsg) {
            if (success) {
                cb.onSuccess(otp);
            } else {
                cb.onFailure(errorMsg != null ? errorMsg : "Failed to send email.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email body
    // ─────────────────────────────────────────────────────────────────────────

    private static String buildEmailBody(String name, String otp) {
        String greeting = (name != null && !name.isEmpty()) ? name : "Valued Customer";
        return  "Hi " + greeting + ",\n\n"
              + "Your Maestro Autoworks verification code is:\n\n"
              + "    " + otp + "\n\n"
              + "This code expires in 5 minutes. Do not share it with anyone.\n\n"
              + "If you did not request this code, please ignore this email.\n\n"
              + "— Maestro Autoworks Team";
    }
}
