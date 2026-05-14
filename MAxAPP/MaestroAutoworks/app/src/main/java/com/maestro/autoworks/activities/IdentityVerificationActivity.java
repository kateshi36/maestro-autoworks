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
import com.maestro.autoworks.utils.EmailOtpSender;

import java.util.concurrent.TimeUnit;

/**
 * IdentityVerificationActivity — Step 10: Post-Login Identity Verification
 * ────────────────────────────────────────────────────────────────────────────
 * Channels:
 *   • SMS OTP   — Firebase Phone Auth sends a real SMS to the registered phone.
 *   • Email OTP — A 6-digit code is generated locally and emailed to the
 *                 user's registered email via JavaMail / Gmail SMTP.
 *
 * Extras passed in via Intent:
 *   EXTRA_USER_ID, EXTRA_USERNAME, EXTRA_FULL_NAME, EXTRA_ROLE,
 *   EXTRA_IS_ADMIN, EXTRA_FIRST_NAME, EXTRA_MASKED_CONTACT,
 *   EXTRA_PHONE_NUMBER, EXTRA_EMAIL  (NEW)
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
    public static final String EXTRA_EMAIL          = "email";  // ← NEW

    // ── OTP config ────────────────────────────────────────────────────────────
    private static final int  OTP_LENGTH    = 6;
    private static final int  MAX_ATTEMPTS  = 3;
    private static final long RESEND_MILLIS = 60_000L;

    // ── OTP channel ───────────────────────────────────────────────────────────
    private enum OtpChannel { SMS, EMAIL }
    private OtpChannel activeChannel = OtpChannel.SMS;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private TextView     tvGreeting, tvSubtitle, tvMaskedContact, tvOtpHint;
    private TextView     tvTimer, tvAttempts;
    private EditText[]   otpBoxes = new EditText[OTP_LENGTH];
    private Button       btnVerify, btnResend;
    private TextView     btnUseSecurityQuestion;
    private LinearLayout layoutOtp, layoutSecurityQuestion, layoutLockout;
    private EditText     etSecurityAnswer;
    private TextView     tvSecurityQuestion;
    private LinearLayout tabSms, tabEmail;

    // ── State ─────────────────────────────────────────────────────────────────
    private int            attemptsLeft = MAX_ATTEMPTS;
    private CountDownTimer countDownTimer;
    private boolean        timerRunning = false;

    // ── Firebase SMS ──────────────────────────────────────────────────────────
    private FirebaseAuth                          mAuth;
    private String                                mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private boolean                               smsCodeSent = false;

    // ── Email OTP ─────────────────────────────────────────────────────────────
    private String  emailOtpCode   = null;
    private boolean emailOtpSent   = false;
    private long    emailOtpSentAt = 0L;
    private static final long EMAIL_OTP_EXPIRY_MS = 5 * 60 * 1000L;

    // ── Session ───────────────────────────────────────────────────────────────
    private SessionManager session;

    // ── User data ─────────────────────────────────────────────────────────────
    private int     userId;
    private String  username, fullName, role, firstName, maskedContact, phoneNumber, email;
    private boolean isAdmin;

    // ── Security question ─────────────────────────────────────────────────────
    private static final String SECURITY_QUESTION = "What is the name of your first car?";
    private static final String SECURITY_ANSWER   = "maestro";

    // ─────────────────────────────────────────────────────────────────────────
    // Firebase callbacks
    // ─────────────────────────────────────────────────────────────────────────

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            onVerificationSuccess();
        }
        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            runOnUiThread(() -> {
                String msg = e.getMessage() != null ? e.getMessage() : "Verification failed.";
                Toast.makeText(IdentityVerificationActivity.this,
                        "SMS error: " + msg, Toast.LENGTH_LONG).show();
                tvTimer.setText("Tap Resend to try again.");
                btnResend.setEnabled(true);
                timerRunning = false;
            });
        }
        @Override
        public void onCodeSent(@NonNull String verificationId,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            mVerificationId = verificationId;
            mResendToken    = token;
            smsCodeSent     = true;
            runOnUiThread(() -> {
                tvSubtitle.setText("Enter the 6-digit code sent to:");
                tvOtpHint.setVisibility(View.GONE);
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

        Intent in = getIntent();
        userId        = in.getIntExtra(EXTRA_USER_ID, -1);
        username      = in.getStringExtra(EXTRA_USERNAME);
        fullName      = in.getStringExtra(EXTRA_FULL_NAME);
        role          = in.getStringExtra(EXTRA_ROLE);
        isAdmin       = in.getBooleanExtra(EXTRA_IS_ADMIN, false);
        firstName     = in.getStringExtra(EXTRA_FIRST_NAME);
        maskedContact = in.getStringExtra(EXTRA_MASKED_CONTACT);
        phoneNumber   = in.getStringExtra(EXTRA_PHONE_NUMBER);
        email         = in.getStringExtra(EXTRA_EMAIL);

        if (maskedContact == null || maskedContact.isEmpty()) maskedContact = "your registered contact";

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
        tabSms                 = findViewById(R.id.tabOtpSms);
        tabEmail               = findViewById(R.id.tabOtpEmail);

        otpBoxes[0] = findViewById(R.id.etOtp1);
        otpBoxes[1] = findViewById(R.id.etOtp2);
        otpBoxes[2] = findViewById(R.id.etOtp3);
        otpBoxes[3] = findViewById(R.id.etOtp4);
        otpBoxes[4] = findViewById(R.id.etOtp5);
        otpBoxes[5] = findViewById(R.id.etOtp6);
        setupOtpBoxes();

        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;
        tvGreeting.setText("Hey " + greetName + ", verify it's you \uD83D\uDD10");
        tvMaskedContact.setText(maskedContact);
        tvSecurityQuestion.setText(SECURITY_QUESTION);
        tvOtpHint.setVisibility(View.GONE);

        // Hide Email tab if no email registered
        if ((email == null || email.isEmpty()) && tabEmail != null) {
            tabEmail.setVisibility(View.GONE);
        }

        if (tabSms   != null) tabSms.setOnClickListener(v   -> switchChannel(OtpChannel.SMS));
        if (tabEmail != null) tabEmail.setOnClickListener(v -> switchChannel(OtpChannel.EMAIL));

        switchChannel(OtpChannel.SMS);

        btnVerify.setOnClickListener(v -> verifyOtp());

        btnResend.setOnClickListener(v -> {
            if (!timerRunning) {
                attemptsLeft = MAX_ATTEMPTS;
                updateAttemptsLabel();
                if (activeChannel == OtpChannel.SMS) sendSmsOtp(true);
                else sendEmailOtp();
            }
        });

        btnUseSecurityQuestion.setOnClickListener(v -> showSecurityQuestion());

        View btnLockoutToSecurityQ = findViewById(R.id.btnLockoutToSecurityQ);
        if (btnLockoutToSecurityQ != null) btnLockoutToSecurityQ.setOnClickListener(v -> showSecurityQuestion());

        findViewById(R.id.btnSubmitSecurityAnswer).setOnClickListener(v -> verifySecurityAnswer());
        updateAttemptsLabel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Channel switching
    // ─────────────────────────────────────────────────────────────────────────

    private void switchChannel(OtpChannel channel) {
        activeChannel = channel;

        if (countDownTimer != null) { countDownTimer.cancel(); timerRunning = false; }
        attemptsLeft = MAX_ATTEMPTS;
        clearOtpBoxes();
        updateAttemptsLabel();
        btnResend.setEnabled(false);

        int activeColor   = getColor(R.color.yellow);
        int inactiveColor = getColor(R.color.black_card);
        int activeText    = getColor(R.color.black);
        int inactiveText  = getColor(R.color.muted);

        if (tabSms != null && tabEmail != null) {
            if (channel == OtpChannel.SMS) {
                tabSms.setBackgroundColor(activeColor);
                tabEmail.setBackgroundColor(inactiveColor);
                ((TextView) tabSms.getChildAt(0)).setTextColor(activeText);
                ((TextView) tabEmail.getChildAt(0)).setTextColor(inactiveText);
            } else {
                tabEmail.setBackgroundColor(activeColor);
                tabSms.setBackgroundColor(inactiveColor);
                ((TextView) tabEmail.getChildAt(0)).setTextColor(activeText);
                ((TextView) tabSms.getChildAt(0)).setTextColor(inactiveText);
            }
        }

        if (channel == OtpChannel.SMS) {
            tvMaskedContact.setText(maskedContact);
            sendSmsOtp(false);
        } else {
            tvMaskedContact.setText(buildMaskedEmail(email));
            sendEmailOtp();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMS OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void sendSmsOtp(boolean isResend) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number on file. Please use Email OTP.", Toast.LENGTH_LONG).show();
            return;
        }
        tvSubtitle.setText("Sending SMS code to:");
        btnResend.setEnabled(false);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(toE164Philippines(phoneNumber))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks);

        if (isResend && mResendToken != null) builder.setForceResendingToken(mResendToken);
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private String toE164Philippines(String raw) {
        if (raw == null) return "";
        String d = raw.replaceAll("[^0-9+]", "");
        if (d.startsWith("+"))  return d;
        if (d.startsWith("63")) return "+" + d;
        if (d.startsWith("0"))  return "+63" + d.substring(1);
        return "+" + d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void sendEmailOtp() {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email address on file. Please use SMS OTP.", Toast.LENGTH_LONG).show();
            return;
        }
        tvSubtitle.setText("Sending code to your email:");
        tvMaskedContact.setText(buildMaskedEmail(email));
        btnResend.setEnabled(false);
        btnVerify.setEnabled(false);

        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;

        EmailOtpSender.sendOtp(email, greetName, new EmailOtpSender.SendCallback() {
            @Override
            public void onSuccess(String otp) {
                emailOtpCode   = otp;
                emailOtpSent   = true;
                emailOtpSentAt = System.currentTimeMillis();

                tvSubtitle.setText("Enter the 6-digit code sent to:");
                tvOtpHint.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                startCountdown();
                clearOtpBoxes();
                showOtpLayout();
                Toast.makeText(IdentityVerificationActivity.this,
                        "OTP sent to your email.", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String errorMessage) {
                btnVerify.setEnabled(true);
                tvTimer.setText("Failed to send. Tap Resend.");
                btnResend.setEnabled(true);
                timerRunning = false;
                Toast.makeText(IdentityVerificationActivity.this,
                        "Could not send email OTP: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildMaskedEmail(String rawEmail) {
        if (rawEmail == null || !rawEmail.contains("@")) return "your email";
        int atIdx = rawEmail.indexOf('@');
        String local  = rawEmail.substring(0, atIdx);
        String domain = rawEmail.substring(atIdx);
        String masked = local.length() <= 1 ? local : local.charAt(0) + "***";
        return masked + domain;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer
    // ─────────────────────────────────────────────────────────────────────────

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        btnResend.setEnabled(false);
        timerRunning = true;

        countDownTimer = new CountDownTimer(RESEND_MILLIS, 1000) {
            @Override public void onTick(long ms) {
                tvTimer.setText(String.format("Code expires in %d:%02d", ms / 60000, (ms % 60000) / 1000));
            }
            @Override public void onFinish() {
                timerRunning = false;
                tvTimer.setText("Code expired. Tap Resend.");
                btnResend.setEnabled(true);
                if (activeChannel == OtpChannel.EMAIL) { emailOtpSent = false; emailOtpCode = null; }
            }
        }.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP boxes
    // ─────────────────────────────────────────────────────────────────────────

    private void setupOtpBoxes() {
        for (int i = 0; i < OTP_LENGTH; i++) {
            final int idx = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < OTP_LENGTH - 1) otpBoxes[idx + 1].requestFocus();
                    if (s.length() == 0 && idx > 0)              otpBoxes[idx - 1].requestFocus();
                }
            });
        }
    }

    private void clearOtpBoxes() {
        for (EditText b : otpBoxes) b.setText("");
        if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
    }

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText b : otpBoxes) sb.append(b.getText().toString().trim());
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify
    // ─────────────────────────────────────────────────────────────────────────

    private void verifyOtp() {
        String entered = getEnteredOtp();
        if (entered.length() < OTP_LENGTH) {
            Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (activeChannel == OtpChannel.SMS) verifySmsOtp(entered);
        else                                 verifyEmailOtp(entered);
    }

    private void verifySmsOtp(String entered) {
        if (!smsCodeSent || mVerificationId == null) {
            Toast.makeText(this, "OTP not received yet. Please wait or tap Resend.", Toast.LENGTH_SHORT).show();
            return;
        }
        PhoneAuthCredential cred = PhoneAuthProvider.getCredential(mVerificationId, entered);
        mAuth.signInWithCredential(cred)
                .addOnSuccessListener(this, r -> onVerificationSuccess())
                .addOnFailureListener(this, e -> {
                    attemptsLeft--;
                    updateAttemptsLabel();
                    if (attemptsLeft <= 0) showLockout();
                    else {
                        Toast.makeText(this,
                                "Invalid code. " + attemptsLeft + " attempt(s) remaining.",
                                Toast.LENGTH_SHORT).show();
                        clearOtpBoxes();
                    }
                });
    }

    private void verifyEmailOtp(String entered) {
        if (!emailOtpSent || emailOtpCode == null) {
            Toast.makeText(this, "Email code not sent yet. Please tap Resend.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() - emailOtpSentAt > EMAIL_OTP_EXPIRY_MS) {
            emailOtpSent = false; emailOtpCode = null;
            Toast.makeText(this, "Code has expired. Please tap Resend.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (entered.equals(emailOtpCode)) {
            onVerificationSuccess();
        } else {
            attemptsLeft--;
            updateAttemptsLabel();
            if (attemptsLeft <= 0) showLockout();
            else {
                Toast.makeText(this,
                        "Incorrect code. " + attemptsLeft + " attempt(s) remaining.",
                        Toast.LENGTH_SHORT).show();
                clearOtpBoxes();
            }
        }
    }

    private void updateAttemptsLabel() {
        tvAttempts.setText("Attempts remaining: " + attemptsLeft + " / " + MAX_ATTEMPTS);
        tvAttempts.setTextColor(attemptsLeft <= 1 ? getColor(R.color.danger) : getColor(R.color.muted));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security question
    // ─────────────────────────────────────────────────────────────────────────

    private void showSecurityQuestion() {
        layoutOtp.setVisibility(View.GONE);
        layoutLockout.setVisibility(View.GONE);
        layoutSecurityQuestion.setVisibility(View.VISIBLE);
        tvSubtitle.setText("Answer your security question to continue:");
    }

    private void verifySecurityAnswer() {
        String answer = etSecurityAnswer.getText().toString().trim().toLowerCase();
        if (answer.isEmpty()) { Toast.makeText(this, "Please enter your answer.", Toast.LENGTH_SHORT).show(); return; }
        if (answer.equals(SECURITY_ANSWER)) onVerificationSuccess();
        else Toast.makeText(this, "Incorrect answer. Please try again.", Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout helpers
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
    // Success
    // ─────────────────────────────────────────────────────────────────────────

    private void onVerificationSuccess() {
        if (countDownTimer != null) countDownTimer.cancel();
        session.saveSession(userId, fullName, username, role);
        String greetName = (firstName != null && !firstName.isEmpty()) ? firstName : username;
        if (isAdmin) {
            Toast.makeText(this, "Identity verified! Welcome, Admin " + greetName + " \uD83D\uDD27", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, AdminDashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } else {
            Toast.makeText(this, "Identity verified! Welcome back, " + greetName + " \uD83D\uDD27", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, HomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
