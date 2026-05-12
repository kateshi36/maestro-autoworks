package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.User;

/**
 * RegisterActivity — Create account.
 * Demonstrates: EditText, TextView, Button, Toast, Simple Computation & Validation.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFirst, etLast, etUsername, etEmail, etPhone, etPass, etConfirm;
    private TextView tvValidation, tvCharCount;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);

        etFirst    = findViewById(R.id.etFirstName);
        etLast     = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPhone    = findViewById(R.id.etPhone);
        etPass     = findViewById(R.id.etPassword);
        etConfirm  = findViewById(R.id.etConfirmPassword);
        tvValidation = findViewById(R.id.tvValidation);
        tvCharCount  = findViewById(R.id.tvCharCount);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        // ── Simple Computation: password strength based on char count ──
        etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int len = s.length();
                // Computation: strength score = len / 2 (max 5)
                int score = Math.min(len / 2, 5);
                String bar  = "█".repeat(score) + "░".repeat(5 - score);
                String label;
                int color;
                if (len == 0)       { label = "—"; color = R.color.muted; }
                else if (len < 4)   { label = "Weak";   color = R.color.danger; }
                else if (len < 8)   { label = "Fair";   color = R.color.yellow; }
                else                { label = "Strong"; color = R.color.success; }

                tvCharCount.setText("Password strength: " + bar + "  " + label + "  (" + len + " chars)");
                tvCharCount.setTextColor(getColor(color));
            }
        });

        // ── Validation: check passwords match live ──
        etConfirm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String p1 = etPass.getText().toString();
                String p2 = s.toString();
                if (p2.isEmpty()) {
                    tvValidation.setText("");
                } else if (p1.equals(p2)) {
                    tvValidation.setText("✔ Passwords match");
                    tvValidation.setTextColor(getColor(R.color.success));
                } else {
                    tvValidation.setText("✘ Passwords do not match");
                    tvValidation.setTextColor(getColor(R.color.danger));
                }
            }
        });

        // ── Button + Validation + Toast + SQLite insert ──
        btnRegister.setOnClickListener(v -> {
            String firstName = etFirst.getText().toString().trim();
            String lastName  = etLast.getText().toString().trim();
            String username  = etUsername.getText().toString().trim();
            String email     = etEmail.getText().toString().trim();
            String phone     = etPhone.getText().toString().trim();
            String password  = etPass.getText().toString().trim();
            String confirm   = etConfirm.getText().toString().trim();

            // Validation checks
            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                    || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.usernameExists(username)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.emailExists(email)) {
                Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to SQLite
            User user = new User();
            user.firstName = firstName;
            user.lastName  = lastName;
            user.username  = username;
            user.email     = email;
            user.phone     = phone;
            user.password  = password;

            long id = db.insertUser(user);
            if (id > 0) {
                Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        tvGoLogin.setOnClickListener(v ->
            startActivity(new Intent(this, LoginActivity.class)));
    }
}
