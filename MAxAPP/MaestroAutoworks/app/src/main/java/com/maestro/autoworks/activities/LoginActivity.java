package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.User;

/**
 * LoginActivity — Sign in screen.
 * ─────────────────────────────────────────────────────────────────────────────
 * Features:
 *   • Role selector tabs: CUSTOMER (default) | ADMIN
 *   • Admin notice strip shown when Admin tab is active
 *   • Register link hidden for Admin mode
 *   • Sign-in button label changes per selected role
 *   • Routes admin → AdminDashboardActivity, customer → HomeActivity
 *   • "Visit Website" button opens WebViewActivity with the Maestro website
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class LoginActivity extends AppCompatActivity {

    // ── UI refs ──────────────────────────────────────────────────────────────
    private EditText      etUsername, etPassword;
    private TextView      tvPasswordInfo;
    private LinearLayout  tabCustomer, tabAdmin, layoutAdminNotice;
    private TextView      tvGoRegister;
    private Button        btnSignIn;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isAdminMode = false;   // false = Customer, true = Admin

    // ── DB / Session ─────────────────────────────────────────────────────────
    private DatabaseHelper db;
    private SessionManager session;

    // ── Website URL ───────────────────────────────────────────────────────────
    /** Replace with your deployed website URL. */
    private static final String WEBSITE_URL = "https://maestroautoworks.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        // Bind views
        etUsername        = findViewById(R.id.etUsername);
        etPassword        = findViewById(R.id.etPassword);
        tvPasswordInfo    = findViewById(R.id.tvPasswordInfo);
        btnSignIn         = findViewById(R.id.btnSignIn);
        tabCustomer       = findViewById(R.id.tabCustomer);
        tabAdmin          = findViewById(R.id.tabAdmin);
        layoutAdminNotice = findViewById(R.id.layoutAdminNotice);
        tvGoRegister      = findViewById(R.id.tvGoRegister);
        TextView tvBack         = findViewById(R.id.tvBack);
        Button   btnVisitWebsite = findViewById(R.id.btnVisitWebsite);

        // ── Role tab click listeners ───────────────────────────────────────
        tabCustomer.setOnClickListener(v -> setRole(false));
        tabAdmin.setOnClickListener(v   -> setRole(true));

        // Start in Customer mode
        setRole(false);

        // ── Password strength indicator ───────────────────────────────────
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int len = s.length();
                if (len == 0) {
                    tvPasswordInfo.setText("Password text color: YELLOW (secure mode)");
                    tvPasswordInfo.setTextColor(getColor(R.color.yellow));
                } else if (len < 6) {
                    tvPasswordInfo.setText("Password too short (< 6 chars)");
                    tvPasswordInfo.setTextColor(getColor(R.color.danger));
                } else {
                    tvPasswordInfo.setText("Password length OK \u2714");
                    tvPasswordInfo.setTextColor(getColor(R.color.success));
                }
            }
        });

        // ── Sign In ───────────────────────────────────────────────────────
        btnSignIn.setOnClickListener(v -> attemptLogin());

        // ── Register link ─────────────────────────────────────────────────
        tvGoRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        // ── Visit Website ─────────────────────────────────────────────────


        // ── Back ──────────────────────────────────────────────────────────
        tvBack.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Role switching
    // ─────────────────────────────────────────────────────────────────────────

    private void setRole(boolean adminMode) {
        isAdminMode = adminMode;

        if (adminMode) {
            // Admin tab active
            tabAdmin.setBackgroundColor(getColor(R.color.yellow));
            tabCustomer.setBackgroundColor(getColor(R.color.black_card));

            // Update tab label colours
            ((TextView) tabAdmin.getChildAt(1)).setTextColor(getColor(R.color.black));
            ((TextView) tabCustomer.getChildAt(1)).setTextColor(getColor(R.color.muted));

            layoutAdminNotice.setVisibility(View.VISIBLE);
            btnSignIn.setText("SIGN IN AS ADMIN");
            tvGoRegister.setVisibility(View.GONE);
        } else {
            // Customer tab active
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
            Toast.makeText(this, "Please fill in your email/username and password", Toast.LENGTH_SHORT).show();
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

        // ── Step 10: Route to Identity Verification ──────────────────────
        // Session is saved by IdentityVerificationActivity after OTP is confirmed.
        String maskedContact = buildMaskedContact(user.phone, user.email);

        Intent intent = new Intent(this, IdentityVerificationActivity.class);
        intent.putExtra(IdentityVerificationActivity.EXTRA_USER_ID,        user.id);
        intent.putExtra(IdentityVerificationActivity.EXTRA_USERNAME,       user.username);
        intent.putExtra(IdentityVerificationActivity.EXTRA_FULL_NAME,      user.getFullName());
        intent.putExtra(IdentityVerificationActivity.EXTRA_ROLE,           user.role);
        intent.putExtra(IdentityVerificationActivity.EXTRA_IS_ADMIN,       user.isAdmin());
        intent.putExtra(IdentityVerificationActivity.EXTRA_FIRST_NAME,     user.firstName);
        intent.putExtra(IdentityVerificationActivity.EXTRA_MASKED_CONTACT, maskedContact);
        intent.putExtra(IdentityVerificationActivity.EXTRA_PHONE_NUMBER,   user.phone);
        intent.putExtra(IdentityVerificationActivity.EXTRA_EMAIL,          user.email);  // ← NEW
        startActivity(intent);
        finish(); // clear LoginActivity from back stack
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mask the user's phone or email for display on the verify screen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a masked string such as "+63 9** ***1234" or "j***@gmail.com".
     * Falls back to a generic label when both are absent.
     */
    private String buildMaskedContact(String phone, String email) {
        if (phone != null && phone.length() >= 4) {
            // Show last 4 digits of phone
            String last4 = phone.substring(phone.length() - 4);
            return "+63 9** ***" + last4;
        }
        if (email != null && email.contains("@")) {
            int atIdx = email.indexOf('@');
            String local  = email.substring(0, atIdx);
            String domain = email.substring(atIdx);
            String maskedLocal = local.length() <= 1
                    ? local
                    : local.charAt(0) + "***";
            return maskedLocal + domain;
        }
        return "your registered contact";
    }
}
