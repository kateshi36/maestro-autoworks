package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.User;
import com.maestro.autoworks.utils.EmailOtpSender;

/**
 * ForgotPasswordActivity — OTP-based password reset.
 * ─────────────────────────────────────────────────────────────────────────────
 * Step 1: User enters their email or phone number → account is looked up.
 * Step 2: A 6-digit OTP is emailed; user enters it here (5-minute countdown).
 * Step 3: User sets and confirms a new password → DB is updated.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    // ── Step containers ───────────────────────────────────────────────────────
    private View layoutFpStep1, layoutFpStep2, layoutFpStep3;
    private TextView tvFpStepIndicator;

    // ── Step 1 ────────────────────────────────────────────────────────────────
    private EditText etFpEmailOrPhone;

    // ── Step 2 ────────────────────────────────────────────────────────────────
    private EditText  etFpOtp;
    private TextView  tvFpOtpSentTo, tvFpCountdown, tvFpResend;
    private String    pendingOtp;
    private CountDownTimer countDownTimer;

    // ── Step 3 ────────────────────────────────────────────────────────────────
    private EditText etFpNewPassword, etFpConfirmPassword;
    private TextView tvFpPasswordStrength, tvFpPasswordMatch;
    private TextView btnToggleNewPassword, btnToggleConfirmPassword;
    private boolean  newPasswordVisible     = false;
    private boolean  confirmPasswordVisible = false;

    // ── Shared state ──────────────────────────────────────────────────────────
    private User           foundUser;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = new DatabaseHelper(this);

        // Step containers
        layoutFpStep1    = findViewById(R.id.layoutFpStep1);
        layoutFpStep2    = findViewById(R.id.layoutFpStep2);
        layoutFpStep3    = findViewById(R.id.layoutFpStep3);
        tvFpStepIndicator = findViewById(R.id.tvFpStepIndicator);

        setupStep1();
        setupStep2();
        setupStep3();

        showStep(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void showStep(int step) {
        layoutFpStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutFpStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutFpStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        tvFpStepIndicator.setText("Step " + step + " of 3");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Find account & send OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep1() {
        etFpEmailOrPhone = findViewById(R.id.etFpEmailOrPhone);

        Button btnNext = findViewById(R.id.btnFpStep1Next);
        btnNext.setOnClickListener(v -> attemptSendOtp());

        TextView tvBack = findViewById(R.id.tvFpBackToLogin);
        tvBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptSendOtp() {
        String input = etFpEmailOrPhone.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter your email or phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Look up the user
        foundUser = db.getUserByEmailOrPhone(input);
        if (foundUser == null) {
            Toast.makeText(this,
                "No account found for that email or phone number.",
                Toast.LENGTH_LONG).show();
            return;
        }

        // Require a valid email to send OTP
        String email = foundUser.email;
        if (email == null || email.isEmpty()) {
            Toast.makeText(this,
                "This account has no email address on file. Please contact support.",
                Toast.LENGTH_LONG).show();
            return;
        }

        // Disable button while sending
        Button btnNext = findViewById(R.id.btnFpStep1Next);
        btnNext.setEnabled(false);
        btnNext.setText("SENDING…");

        EmailOtpSender.sendOtp(email, foundUser.firstName, new EmailOtpSender.SendCallback() {
            @Override
            public void onSuccess(String otp) {
                pendingOtp = otp;
                btnNext.setEnabled(true);
                btnNext.setText("SEND OTP VIA EMAIL");

                // Update Step 2 label with masked email
                String masked = maskEmail(email);
                tvFpOtpSentTo.setText(
                    "A 6-digit OTP has been sent to " + masked +
                    ".\n\nEnter it below within 5 minutes.");

                startOtpCountdown();
                showStep(2);
            }

            @Override
            public void onFailure(String errorMessage) {
                btnNext.setEnabled(true);
                btnNext.setText("SEND OTP VIA EMAIL");
                Toast.makeText(ForgotPasswordActivity.this,
                    "Could not send OTP: " + errorMessage,
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Enter & verify OTP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep2() {
        etFpOtp        = findViewById(R.id.etFpOtp);
        tvFpOtpSentTo  = findViewById(R.id.tvFpOtpSentTo);
        tvFpCountdown  = findViewById(R.id.tvFpCountdown);
        tvFpResend     = findViewById(R.id.tvFpResend);

        Button btnVerify = findViewById(R.id.btnFpVerifyOtp);
        btnVerify.setOnClickListener(v -> verifyOtp());

        Button btnBack = findViewById(R.id.btnFpStep2Back);
        btnBack.setOnClickListener(v -> {
            cancelCountdown();
            showStep(1);
        });

        tvFpResend.setOnClickListener(v -> resendOtp());
    }

    private void verifyOtp() {
        String entered = etFpOtp.getText().toString().trim();
        if (entered.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendingOtp == null) {
            Toast.makeText(this, "Session expired. Please start again.", Toast.LENGTH_SHORT).show();
            showStep(1);
            return;
        }
        if (!entered.equals(pendingOtp)) {
            Toast.makeText(this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        // OTP matches — advance
        cancelCountdown();
        pendingOtp = null; // invalidate so it cannot be reused
        showStep(3);
    }

    private void startOtpCountdown() {
        cancelCountdown();
        tvFpResend.setVisibility(View.GONE);
        countDownTimer = new CountDownTimer(5 * 60 * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvFpCountdown.setText(
                    String.format("OTP expires in %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvFpCountdown.setText("OTP expired.");
                pendingOtp = null;
                tvFpResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void resendOtp() {
        if (foundUser == null || foundUser.email == null) return;
        tvFpResend.setVisibility(View.GONE);
        tvFpCountdown.setText("Resending…");
        EmailOtpSender.sendOtp(foundUser.email, foundUser.firstName, new EmailOtpSender.SendCallback() {
            @Override
            public void onSuccess(String otp) {
                pendingOtp = otp;
                Toast.makeText(ForgotPasswordActivity.this,
                    "A new OTP has been sent.", Toast.LENGTH_SHORT).show();
                startOtpCountdown();
            }

            @Override
            public void onFailure(String errorMessage) {
                tvFpCountdown.setText("Resend failed. Try again.");
                tvFpResend.setVisibility(View.VISIBLE);
                Toast.makeText(ForgotPasswordActivity.this,
                    "Failed to resend: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3 — Set new password
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep3() {
        etFpNewPassword      = findViewById(R.id.etFpNewPassword);
        etFpConfirmPassword  = findViewById(R.id.etFpConfirmPassword);
        tvFpPasswordStrength = findViewById(R.id.tvFpPasswordStrength);
        tvFpPasswordMatch    = findViewById(R.id.tvFpPasswordMatch);
        btnToggleNewPassword     = findViewById(R.id.btnToggleNewPassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);

        // Eye toggles
        btnToggleNewPassword.setOnClickListener(v -> {
            newPasswordVisible = !newPasswordVisible;
            etFpNewPassword.setInputType(newPasswordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleNewPassword.setText(newPasswordVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
            etFpNewPassword.setSelection(etFpNewPassword.getText().length());
        });

        btnToggleConfirmPassword.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            etFpConfirmPassword.setInputType(confirmPasswordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setText(confirmPasswordVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
            etFpConfirmPassword.setSelection(etFpConfirmPassword.getText().length());
        });

        // Strength meter for new password
        etFpNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int len   = s.length();
                int score = Math.min(len / 2, 5);
                String bar = "█".repeat(score) + "░".repeat(5 - score);
                String label; int color;
                if (len == 0)      { label = "—";      color = R.color.muted; }
                else if (len < 4)  { label = "Weak";   color = R.color.danger; }
                else if (len < 8)  { label = "Fair";   color = R.color.yellow; }
                else               { label = "Strong"; color = R.color.success; }
                tvFpPasswordStrength.setText("Strength: " + bar + "  " + label + "  (" + len + " chars)");
                tvFpPasswordStrength.setTextColor(getColor(color));
                // Re-validate match
                updatePasswordMatchIndicator();
            }
        });

        // Live match indicator
        etFpConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updatePasswordMatchIndicator(); }
        });

        Button btnReset = findViewById(R.id.btnFpResetPassword);
        btnReset.setOnClickListener(v -> attemptResetPassword());

        Button btnBack = findViewById(R.id.btnFpStep3Back);
        btnBack.setOnClickListener(v -> showStep(2));
    }

    private void updatePasswordMatchIndicator() {
        String p1 = etFpNewPassword.getText().toString();
        String p2 = etFpConfirmPassword.getText().toString();
        if (p2.isEmpty()) { tvFpPasswordMatch.setText(""); return; }
        if (p1.equals(p2)) {
            tvFpPasswordMatch.setText("✔ Passwords match");
            tvFpPasswordMatch.setTextColor(getColor(R.color.success));
        } else {
            tvFpPasswordMatch.setText("✘ Passwords do not match");
            tvFpPasswordMatch.setTextColor(getColor(R.color.danger));
        }
    }

    private void attemptResetPassword() {
        String newPass     = etFpNewPassword.getText().toString().trim();
        String confirmPass = etFpConfirmPassword.getText().toString().trim();

        if (newPass.isEmpty()) {
            Toast.makeText(this, "Please enter a new password.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (foundUser == null) {
            Toast.makeText(this, "Session error. Please start again.", Toast.LENGTH_SHORT).show();
            showStep(1);
            return;
        }

        boolean updated = db.updatePassword(foundUser.id, newPass);
        if (updated) {
            Toast.makeText(this,
                "Password reset successfully! Please sign in with your new password.",
                Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this,
                "Failed to reset password. Please try again.",
                Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a display-safe masked email, e.g. "j***@gmail.com". */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "your email";
        int atIdx     = email.indexOf('@');
        String local  = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        String masked = local.length() <= 1 ? local : local.charAt(0) + "***";
        return masked + domain;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountdown();
    }
}
