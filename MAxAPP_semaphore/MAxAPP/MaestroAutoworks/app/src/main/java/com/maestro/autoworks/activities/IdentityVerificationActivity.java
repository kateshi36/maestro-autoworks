package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.SessionManager;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

/**
 * IdentityVerificationActivity — Step 10: Post-Login Identity Verification
 * ────────────────────────────────────────────────────────────────────────────
 * OTP delivery: Semaphore SMS API (semaphore.co/api/v4/otp)
 * No Firebase billing required — works on any network plan.
 *
 * Flow:
 *   1. A 6-digit OTP is generated locally and sent via Semaphore SMS.
 *   2. The user enters the code in the split 6-box input.
 *   3. A 120-second resend cooldown runs.
 *   4. On correct OTP → routes to home/dashboard based on role.
 *   5. On 3 failed attempts → lockout + security-question fallback.
 *
 * ── SETUP ────────────────────────────────────────────────────────────────────
 * 1. Sign up at https://semaphore.co  (free account = free credits to start)
 * 2. Go to your Dashboard → API Keys → copy your API key
 * 3. Replace YOUR_SEMAPHORE_API_KEY below with your actual key
 * ────────────────────────────────────────────────────────────────────────────
 */
public class IdentityVerificationActivity extends AppCompatActivity {

    // ── !! REPLACE THIS WITH YOUR SEMAPHORE API KEY !! ────────────────────────
    private static final String SEMAPHORE_API_KEY    = "YOUR_SEMAPHORE_API_KEY";
    private static final String SEMAPHORE_SENDER     = "MAESTRO"; // max 11 chars, no spaces
    private static final String SEMAPHORE_OTP_URL    = "https://semaphore.co/api/v4/otp";
    // ─────────────────────────────────────────────────────────────────────────

    // ── Intent extras ─────────────────────────────────────────────────────────
    public static final String EXTRA_USER_ID        = "user_id";
    public static final String EXTRA_USERNAME       = "username";
    public static final String EXTRA_FULL_NAME      = "full_name";
    public static final String EXTRA_ROLE           = "role";
    public static final String EXTRA_IS_ADMIN       = "is_admin";
    public static final String EXTRA_FIRST_NAME     = "first_name";
    public static final String EXTRA_MASKED_CONTACT = "masked_contact";
    public static final String EXTRA_PHONE_NUMBER   = "phone_number";

    // ── OTP config ────────────────────────────────────────────────────────────
    private static final int  OTP_LENGTH    = 6;
    private static final int  MAX_ATTEMPTS  = 3;
    private static final long RESEND_MILLIS = 120_000L; // 2 minutes

    // ── UI refs ───────────────────────────────────────────────────────────────
    private TextView     tvGreeting, tvSubtitle, tvMaskedContact;
    private TextView     tvOtpHint;   // hidden in production — kept for layout compat
    private EditText[]   otpBoxes = new EditText[OTP_LENGTH];
    private Button       btnVerify, btnResend;
    private TextView     btnUseSecurityQuestion;
    private TextView     tvTimer, tvAttempts;
    private LinearLayout layoutOtp, layoutSecurityQuestion, layoutLockout;
    private EditText     etSecurityAnswer;
    private TextView     tvSecurityQuestion;

    // ── State ─────────────────────────────────────────────────────────────────
    private String         currentOtp;
    private int            attemptsLeft = MAX_ATTEMPTS;
    private CountDownTimer countDownTimer;
    private boolean        timerRunning = false;

    // ── Session ───────────────────────────────────────────────────────────────
    private SessionManager session;

    // ── User data from intent ─────────────────────────────────────────────────
    private int     userId;
    private String  username, fullName, role, firstName, maskedContact, phoneNumber;
    private boolean isAdmin;

    // ── Security question fallback ────────────────────────────────────────────
    private static final String SECURITY_QUESTION = "What is the name of your first car?";
    private static final String SECURITY_ANSWER   = "maestro";

    // ─────────────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_verification);

        session = new SessionManager(this);

        // ── Read intent extras ─────────────────────────────────────────────
        Intent in = getIntent();
        userId        = in.getIntExtra(EXTRA_USER_ID, -1);
        username      = in.getStringExtra(EXTRA_USERNAME);
        fullName      = in.getStringExtra(EXTRA_FULL_NAME);
        role          = in.getStringExtra(EXTRA_ROLE);
        isAdmin       = in.getBooleanExtra(EXTRA_IS_ADMIN, false);
        firstName     = in.getStringExtra(EXTRA_FIRST_NAME);
        maskedContact = in.getStringExtra(EXTRA_MASKED_CONTACT);
        phoneNumber   = in.getStringExtra(EXTRA_PHONE_NUMBER);

        if (maskedContact == null || maskedContact.isEmpty()) {
            maskedContact = "your registered contact";
        }

        // ── Bind views ─────────────────────────────────────────────────────
        tvGreeting             = findViewById(R.id.tvVerifyGreeting);
        tvSubtitle             = findViewById(R.id.tvVerifySubtitle);
        tvMaskedContact        = findViewById(R.id.tvMaskedContact);
        tvOtpHint              = findViewById(R.id.tvOtpHint);
        tvTimer                = findViewById(R.id.tvTimer);
        tvAttempts             = findViewById(R.id.tvAttempts);
        btnVerify              = findViewById(R.id.btnVerifyOtp);
        btnResend              = findViewById(R.id.btnResendOtp);
        btnUseSecurityQuestion = findViewById(R.id.btnUseSecurityQuestion);
        layoutOtp              = findViewById(R.id.layoutOtpSection);
        layoutSecurityQuestion = findViewById(R.id.layoutSecurityQuestion);
        layoutLockout          = findViewById(R.id.layoutLockout);
        etSecurityAnswer       = findViewById(R.id.etSecurityAnswer);
        tvSecurityQuestion     = findViewById(R.id.tvSecurityQuestion);

        otpBoxes[0] = findViewById(R.id.etOtp1);
        otpBoxes[1] = findViewById(R.id.etOtp2);
        otpBoxes[2] = findViewById(R.id.etOtp3);
        otpBoxes[3] = findViewById(R.id.etOtp4);
        otpBoxes[4] = findViewById(R.id.etOtp5);
        otpBoxes[5] = findViewById(R.id.etOtp6);

        setupOtpBoxes();

        // ── Greeting ───────────────────────────────────────────────────────
        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;
        tvGreeting.setText("Hey " + greetName + ", verify it's you \uD83D\uDD10");
        tvMaskedContact.setText(maskedContact);
        tvSecurityQuestion.setText(SECURITY_QUESTION);
        tvOtpHint.setVisibility(View.GONE);

        // ── Send OTP ───────────────────────────────────────────────────────
        generateAndSendOtp();

        // ── Button listeners ───────────────────────────────────────────────
        btnVerify.setOnClickListener(v -> verifyOtp());

        btnResend.setOnClickListener(v -> {
            if (!timerRunning) {
                attemptsLeft = MAX_ATTEMPTS;
                generateAndSendOtp();
                updateAttemptsLabel();
            }
        });

        btnUseSecurityQuestion.setOnClickListener(v -> showSecurityQuestion());

        View btnLockoutToSecurityQ = findViewById(R.id.btnLockoutToSecurityQ);
        if (btnLockoutToSecurityQ != null) {
            btnLockoutToSecurityQ.setOnClickListener(v -> showSecurityQuestion());
        }

        findViewById(R.id.btnSubmitSecurityAnswer).setOnClickListener(v -> verifySecurityAnswer());

        updateAttemptsLabel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP generation + Semaphore API call
    // ─────────────────────────────────────────────────────────────────────────

    private void generateAndSendOtp() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this,
                    "No phone number on file. Please contact support.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Generate a fresh 6-digit OTP
        currentOtp = String.format("%06d", new Random().nextInt(1_000_000));

        tvSubtitle.setText("Sending OTP to:");
        btnVerify.setEnabled(false);
        btnResend.setEnabled(false);

        // Send via Semaphore on a background thread
        String otpToSend = currentOtp;
        String recipient = normalisePhone(phoneNumber);

        new Thread(() -> {
            boolean success = sendViaSemaphore(recipient, otpToSend);
            runOnUiThread(() -> {
                btnVerify.setEnabled(true);
                if (success) {
                    tvSubtitle.setText("Enter the 6-digit code sent to:");
                    Toast.makeText(this, "OTP sent via SMS.", Toast.LENGTH_SHORT).show();
                } else {
                    tvSubtitle.setText("SMS failed. Check your number or try again.");
                    btnResend.setEnabled(true);
                }
                startCountdown();
                clearOtpBoxes();
                showOtpLayout();
            });
        }).start();
    }

    /**
     * POST to Semaphore OTP endpoint.
     * Returns true on HTTP 200, false on any error.
     *
     * Semaphore auto-formats the message as:
     *   "Your OTP is <code>. Use it within X minutes."
     * so we only need to pass the code as the `message` body.
     */
    private boolean sendViaSemaphore(String number, String otp) {
        try {
            String message = "Your Maestro Autoworks OTP is " + otp
                    + ". Valid for 2 minutes. Do not share this code.";

            String params =
                    "apikey="     + URLEncoder.encode(SEMAPHORE_API_KEY, "UTF-8")
                    + "&number="  + URLEncoder.encode(number, "UTF-8")
                    + "&message=" + URLEncoder.encode(message, "UTF-8")
                    + "&sendername=" + URLEncoder.encode(SEMAPHORE_SENDER, "UTF-8");

            URL url = new URL(SEMAPHORE_OTP_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            conn.disconnect();
            return (code == 200 || code == 201);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Normalise Philippine numbers to the format Semaphore expects: 09XXXXXXXXX
     * Semaphore accepts local format (09xx) directly — no need for E.164.
     * e.g. "+639171234567" → "09171234567"
     *      "639171234567"  → "09171234567"
     *      "09171234567"   → "09171234567"  (unchanged)
     */
    private String normalisePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("63") && digits.length() == 12) {
            return "0" + digits.substring(2); // 639xx → 09xx
        }
        return digits; // already 09xx or other format
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Countdown timer
    // ─────────────────────────────────────────────────────────────────────────

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        btnResend.setEnabled(false);
        timerRunning = true;

        countDownTimer = new CountDownTimer(RESEND_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long mins = millisUntilFinished / 60000;
                long secs = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("Code expires in %d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                tvTimer.setText("Code expired. Tap Resend.");
                btnResend.setEnabled(true);
            }
        }.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP box wiring — auto-advance focus
    // ─────────────────────────────────────────────────────────────────────────

    private void setupOtpBoxes() {
        for (int i = 0; i < OTP_LENGTH; i++) {
            final int index = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < OTP_LENGTH - 1) otpBoxes[index + 1].requestFocus();
                    if (s.length() == 0 && index > 0)              otpBoxes[index - 1].requestFocus();
                }
            });
        }
    }

    private void clearOtpBoxes() {
        for (EditText box : otpBoxes) box.setText("");
        if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
    }

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : otpBoxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void verifyOtp() {
        String entered = getEnteredOtp();

        if (entered.length() < OTP_LENGTH) {
            Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (entered.equals(currentOtp)) {
            onVerificationSuccess();
        } else {
            attemptsLeft--;
            updateAttemptsLabel();
            if (attemptsLeft <= 0) {
                showLockout();
            } else {
                Toast.makeText(this,
                        "Incorrect code. " + attemptsLeft + " attempt(s) remaining.",
                        Toast.LENGTH_SHORT).show();
                clearOtpBoxes();
            }
        }
    }

    private void updateAttemptsLabel() {
        tvAttempts.setText("Attempts remaining: " + attemptsLeft + " / " + MAX_ATTEMPTS);
        tvAttempts.setTextColor(getColor(attemptsLeft <= 1 ? R.color.danger : R.color.muted));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security question fallback
    // ─────────────────────────────────────────────────────────────────────────

    private void showSecurityQuestion() {
        layoutOtp.setVisibility(View.GONE);
        layoutLockout.setVisibility(View.GONE);
        layoutSecurityQuestion.setVisibility(View.VISIBLE);
        tvSubtitle.setText("Answer your security question to continue:");
    }

    private void verifySecurityAnswer() {
        String answer = etSecurityAnswer.getText().toString().trim().toLowerCase();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Please enter your answer.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (answer.equals(SECURITY_ANSWER)) {
            onVerificationSuccess();
        } else {
            Toast.makeText(this, "Incorrect answer. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout visibility helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showOtpLayout() {
        layoutOtp.setVisibility(View.VISIBLE);
        layoutSecurityQuestion.setVisibility(View.GONE);
        layoutLockout.setVisibility(View.GONE);
    }

    private void showLockout() {
        if (countDownTimer != null) countDownTimer.cancel();
        layoutOtp.setVisibility(View.GONE);
        layoutSecurityQuestion.setVisibility(View.GONE);
        layoutLockout.setVisibility(View.VISIBLE);
        tvSubtitle.setText("Too many failed attempts.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Success — save session and route
    // ─────────────────────────────────────────────────────────────────────────

    private void onVerificationSuccess() {
        if (countDownTimer != null) countDownTimer.cancel();

        session.saveSession(userId, fullName, username, role);

        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;

        if (isAdmin) {
            Toast.makeText(this,
                    "Identity verified! Welcome, Admin " + greetName + " \uD83D\uDD27",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this,
                    "Identity verified! Welcome back, " + greetName + " \uD83D\uDD27",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
