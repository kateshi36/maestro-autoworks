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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.SessionManager;

import java.util.concurrent.TimeUnit;

/**
 * IdentityVerificationActivity — Step 10: Post-Login Identity Verification
 * ────────────────────────────────────────────────────────────────────────────
 * Flow:
 *   1. Firebase sends a real SMS OTP to the user's registered phone number.
 *   2. The user enters the 6-digit code in the split 6-box input.
 *   3. A 60-second resend cooldown runs; the user may resend once it expires.
 *   4. On correct OTP → Firebase credential verified → routes to home/dashboard.
 *   5. On 3 failed attempts → locks out and shows a security-question fallback.
 *
 * Extras passed in via Intent:
 *   EXTRA_USER_ID        (int)     — authenticated user's DB id
 *   EXTRA_USERNAME       (String)  — display name / username
 *   EXTRA_FULL_NAME      (String)  — e.g. "Juan dela Cruz"
 *   EXTRA_ROLE           (String)  — "customer" | "admin"
 *   EXTRA_IS_ADMIN       (boolean) — convenience flag
 *   EXTRA_FIRST_NAME     (String)  — first name for greeting
 *   EXTRA_MASKED_CONTACT (String)  — masked phone shown to user
 *   EXTRA_PHONE_NUMBER   (String)  — full E.164 phone, e.g. "+639171234567"
 * ────────────────────────────────────────────────────────────────────────────
 */
public class IdentityVerificationActivity extends AppCompatActivity {

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
    private static final long RESEND_MILLIS = 60_000L; // 60 seconds

    // ── UI refs ───────────────────────────────────────────────────────────────
    private TextView   tvGreeting, tvSubtitle, tvMaskedContact;
    private TextView   tvOtpHint;   // kept for layout compatibility — hidden in production
    private EditText[] otpBoxes = new EditText[OTP_LENGTH];
    private Button     btnVerify, btnResend;
    private TextView   btnUseSecurityQuestion;
    private TextView   tvTimer, tvAttempts;
    private LinearLayout layoutOtp, layoutSecurityQuestion, layoutLockout;
    private EditText   etSecurityAnswer;
    private TextView   tvSecurityQuestion;

    // ── State ─────────────────────────────────────────────────────────────────
    private int     attemptsLeft = MAX_ATTEMPTS;
    private CountDownTimer countDownTimer;
    private boolean timerRunning = false;

    // ── Firebase Phone Auth ───────────────────────────────────────────────────
    private FirebaseAuth         mAuth;
    private String               mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private boolean              codeSent = false;

    // ── Session / DB ──────────────────────────────────────────────────────────
    private SessionManager session;

    // ── User data from intent ─────────────────────────────────────────────────
    private int     userId;
    private String  username, fullName, role, firstName, maskedContact, phoneNumber;
    private boolean isAdmin;

    // ── Security question fallback ────────────────────────────────────────────
    // In a real app these would be stored per-user in the DB.
    private static final String SECURITY_QUESTION = "What is the name of your first car?";
    private static final String SECURITY_ANSWER   = "maestro"; // lowercase comparison

    // ─────────────────────────────────────────────────────────────────────────
    // Firebase callbacks
    // ─────────────────────────────────────────────────────────────────────────

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        /**
         * Instant verification (same device) — sign in automatically without
         * asking the user to type anything.
         */
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            // Auto-retrieved or instant verification succeeded
            onVerificationSuccess();
        }

        /**
         * Firebase failed to verify the phone number (invalid number, quota
         * exceeded, network error, etc.).
         */
        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            runOnUiThread(() -> {
                String msg = e.getMessage() != null ? e.getMessage() : "Verification failed.";
                Toast.makeText(IdentityVerificationActivity.this,
                        "SMS error: " + msg, Toast.LENGTH_LONG).show();
                // Allow resend immediately on failure
                tvTimer.setText("Tap Resend to try again.");
                btnResend.setEnabled(true);
                timerRunning = false;
            });
        }

        /**
         * SMS code has been sent — store the verification ID so we can
         * construct a credential when the user submits their code.
         */
        @Override
        public void onCodeSent(@NonNull String verificationId,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            mVerificationId = verificationId;
            mResendToken    = token;
            codeSent        = true;

            runOnUiThread(() -> {
                tvSubtitle.setText("Enter the 6-digit code sent to:");
                tvOtpHint.setVisibility(View.GONE); // production — no hint shown
                startCountdown();
                clearOtpBoxes();
                showOtpLayout();
                Toast.makeText(IdentityVerificationActivity.this,
                        "OTP sent via SMS.", Toast.LENGTH_SHORT).show();
            });
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_verification);

        mAuth   = FirebaseAuth.getInstance();
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

        // ── OTP input boxes ────────────────────────────────────────────────
        otpBoxes[0] = findViewById(R.id.etOtp1);
        otpBoxes[1] = findViewById(R.id.etOtp2);
        otpBoxes[2] = findViewById(R.id.etOtp3);
        otpBoxes[3] = findViewById(R.id.etOtp4);
        otpBoxes[4] = findViewById(R.id.etOtp5);
        otpBoxes[5] = findViewById(R.id.etOtp6);

        setupOtpBoxes();

        // ── Populate greeting ──────────────────────────────────────────────
        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;
        tvGreeting.setText("Hey " + greetName + ", verify it's you \uD83D\uDD10");
        tvMaskedContact.setText(maskedContact);
        tvSecurityQuestion.setText(SECURITY_QUESTION);

        // ── Hide demo hint (production mode) ───────────────────────────────
        tvOtpHint.setVisibility(View.GONE);

        // ── Send OTP via Firebase ──────────────────────────────────────────
        sendOtp(false);

        // ── Button listeners ───────────────────────────────────────────────
        btnVerify.setOnClickListener(v -> verifyOtp());

        btnResend.setOnClickListener(v -> {
            if (!timerRunning) {
                attemptsLeft = MAX_ATTEMPTS;
                sendOtp(true); // use resend token
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
    // Firebase: send / resend OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggers Firebase to send (or resend) an SMS OTP.
     *
     * @param isResend  true → use mResendToken for a forced resend;
     *                  false → fresh send on first call.
     */
    private void sendOtp(boolean isResend) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this,
                    "No phone number on file. Please contact support.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Normalise Philippine local format → E.164
        // e.g. "09171234567" → "+639171234567"
        String e164 = toE164Philippines(phoneNumber);

        tvSubtitle.setText("Sending OTP to:");
        btnResend.setEnabled(false);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(e164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks);

        if (isResend && mResendToken != null) {
            builder.setForceResendingToken(mResendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    /**
     * Converts a Philippine mobile number to E.164.
     * Handles: "09171234567", "+639171234567", "639171234567".
     * Leaves non-PH E.164 numbers unchanged.
     */
    private String toE164Philippines(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;          // already E.164
        if (digits.startsWith("63")) return "+" + digits;   // 63 prefix
        if (digits.startsWith("0"))  return "+63" + digits.substring(1); // 09xx
        return "+" + digits; // fallback
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
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < OTP_LENGTH - 1) {
                        otpBoxes[index + 1].requestFocus();
                    }
                    if (s.length() == 0 && index > 0) {
                        otpBoxes[index - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void clearOtpBoxes() {
        for (EditText box : otpBoxes) {
            box.setText("");
        }
        if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
    }

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : otpBoxes) {
            sb.append(box.getText().toString().trim());
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify OTP via Firebase credential
    // ─────────────────────────────────────────────────────────────────────────

    private void verifyOtp() {
        String entered = getEnteredOtp();

        if (entered.length() < OTP_LENGTH) {
            Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!codeSent || mVerificationId == null) {
            Toast.makeText(this, "OTP not received yet. Please wait or tap Resend.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build credential from the code the user typed
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, entered);

        // Sign in with Firebase using this credential
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, authResult -> onVerificationSuccess())
                .addOnFailureListener(this, e -> {
                    attemptsLeft--;
                    updateAttemptsLabel();

                    if (attemptsLeft <= 0) {
                        showLockout();
                    } else {
                        String msg = (e.getMessage() != null) ? e.getMessage() : "Incorrect code.";
                        Toast.makeText(this,
                                "Invalid code: " + msg + "\n"
                                        + attemptsLeft + " attempt(s) remaining.",
                                Toast.LENGTH_SHORT).show();
                        clearOtpBoxes();
                    }
                });
    }

    private void updateAttemptsLabel() {
        tvAttempts.setText("Attempts remaining: " + attemptsLeft + " / " + MAX_ATTEMPTS);
        if (attemptsLeft <= 1) {
            tvAttempts.setTextColor(getColor(R.color.danger));
        } else {
            tvAttempts.setTextColor(getColor(R.color.muted));
        }
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

        // Save local session (SQLite-based)
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
