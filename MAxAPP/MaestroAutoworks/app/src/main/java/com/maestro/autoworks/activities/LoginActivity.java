package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.User;

/**
 * LoginActivity — Step 9: Initial login screen.
 * ─────────────────────────────────────────────────────────────────────────────
 * The user enters their registered email address and password to sign in.
 *
 * Additional features:
 *   • Role selector tabs: CUSTOMER (default) | ADMIN
 *   • Admin notice strip shown when Admin tab is active
 *   • Register link hidden for Admin mode
 *   • Sign-in button label changes per selected role
 *   • Routes admin → AdminDashboardActivity, customer → HomeActivity
 *   • Password eye-toggle (show / hide password)
 *   • Forgot Password → ForgotPasswordActivity (OTP via email)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class LoginActivity extends AppCompatActivity {

    // ── UI refs ──────────────────────────────────────────────────────────────
    private EditText      etUsername, etPassword;
    private TextView      btnTogglePassword;
    private LinearLayout  tabCustomer, tabAdmin, layoutAdminNotice;
    private TextView      tvGoRegister;
    private Button        btnSignIn;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isAdminMode      = false;
    private boolean passwordVisible  = false;

    // ── DB / Session ─────────────────────────────────────────────────────────
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        // ── Bind views ────────────────────────────────────────────────────
        etUsername        = findViewById(R.id.etUsername);
        etPassword        = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSignIn         = findViewById(R.id.btnSignIn);
        tabCustomer       = findViewById(R.id.tabCustomer);
        tabAdmin          = findViewById(R.id.tabAdmin);
        layoutAdminNotice = findViewById(R.id.layoutAdminNotice);
        tvGoRegister      = findViewById(R.id.tvGoRegister);

        // ── Role tab listeners ────────────────────────────────────────────
        tabCustomer.setOnClickListener(v -> setRole(false));
        tabAdmin.setOnClickListener(v   -> setRole(true));
        setRole(false); // default to Customer

        // ── Password eye-toggle ───────────────────────────────────────────
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // ── Sign In ───────────────────────────────────────────────────────
        btnSignIn.setOnClickListener(v -> attemptLogin());

        // ── Register link ─────────────────────────────────────────────────
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // ── Forgot Password link ──────────────────────────────────────────
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password eye-toggle
    // ─────────────────────────────────────────────────────────────────────────

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            // Show password
            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setText("\uD83D\uDE48"); // 🙈
        } else {
            // Hide password
            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setText("\uD83D\uDC41");  // 👁
        }
        // Keep yellow text colour and move cursor to end
        etPassword.setTextColor(getColor(R.color.yellow));
        etPassword.setSelection(etPassword.getText().length());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Role switching
    // ─────────────────────────────────────────────────────────────────────────

    private void setRole(boolean adminMode) {
        isAdminMode = adminMode;

        if (adminMode) {
            tabAdmin.setBackgroundColor(getColor(R.color.yellow));
            tabCustomer.setBackgroundColor(getColor(R.color.black_card));
            ((TextView) tabAdmin.getChildAt(1)).setTextColor(getColor(R.color.black));
            ((TextView) tabCustomer.getChildAt(1)).setTextColor(getColor(R.color.muted));
            layoutAdminNotice.setVisibility(View.VISIBLE);
            btnSignIn.setText("SIGN IN AS ADMIN");
            tvGoRegister.setVisibility(View.GONE);
        } else {
            tabCustomer.setBackgroundColor(getColor(R.color.yellow));
            tabAdmin.setBackgroundColor(getColor(R.color.black_card));
            ((TextView) tabCustomer.getChildAt(1)).setTextColor(getColor(R.color.black));
            ((TextView) tabAdmin.getChildAt(1)).setTextColor(getColor(R.color.muted));
            layoutAdminNotice.setVisibility(View.GONE);
            btnSignIn.setText("SIGN IN AS CUSTOMER");
            tvGoRegister.setVisibility(View.VISIBLE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login logic
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your email address and password", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = db.loginUser(username, password);

        if (user == null) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Role mismatch guard
        if (isAdminMode && !user.isAdmin()) {
            Toast.makeText(this,
                    "This account does not have admin privileges.\nPlease use the Customer login.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!isAdminMode && user.isAdmin()) {
            Toast.makeText(this,
                    "Admin accounts must use the Admin login tab.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Admins skip 2FA — save session and go straight to the dashboard.
        // OTP verification is for customer accounts only.
        if (user.isAdmin()) {
            session.saveSession(user.id, user.getFullName(), user.username, user.role);
            Toast.makeText(this, "Welcome, Admin " + user.firstName + "! \uD83D\uDD27", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        // Customer — route to Identity Verification (OTP / 2FA).
        String maskedContact = buildMaskedContact(user.phone, user.email);

        Intent intent = new Intent(this, IdentityVerificationActivity.class);
        intent.putExtra(IdentityVerificationActivity.EXTRA_USER_ID,        user.id);
        intent.putExtra(IdentityVerificationActivity.EXTRA_USERNAME,       user.username);
        intent.putExtra(IdentityVerificationActivity.EXTRA_FULL_NAME,      user.getFullName());
        intent.putExtra(IdentityVerificationActivity.EXTRA_ROLE,           user.role);
        intent.putExtra(IdentityVerificationActivity.EXTRA_FIRST_NAME,     user.firstName);
        intent.putExtra(IdentityVerificationActivity.EXTRA_MASKED_CONTACT, maskedContact);
        intent.putExtra(IdentityVerificationActivity.EXTRA_PHONE_NUMBER,   user.phone);
        intent.putExtra(IdentityVerificationActivity.EXTRA_EMAIL,          user.email);
        startActivity(intent);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mask the user's phone or email for display on the verify screen
    // ─────────────────────────────────────────────────────────────────────────

    private String buildMaskedContact(String phone, String email) {
        if (phone != null && phone.length() >= 4) {
            String last4 = phone.substring(phone.length() - 4);
            return "+63 9** ***" + last4;
        }
        if (email != null && email.contains("@")) {
            int atIdx = email.indexOf('@');
            String local  = email.substring(0, atIdx);
            String domain = email.substring(atIdx);
            String maskedLocal = local.length() <= 1 ? local : local.charAt(0) + "***";
            return maskedLocal + domain;
        }
        return "your registered contact";
    }
}