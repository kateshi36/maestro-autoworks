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
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.User;

/**
 * LoginActivity — Sign in screen.
 * Demonstrates: EditText (textPassword inputType), TextColor, Button, Toast, Intent.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvPasswordInfo;
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        etUsername    = findViewById(R.id.etUsername);
        etPassword    = findViewById(R.id.etPassword);
        tvPasswordInfo= findViewById(R.id.tvPasswordInfo);
        Button btnSignIn    = findViewById(R.id.btnSignIn);
        TextView tvGoReg    = findViewById(R.id.tvGoRegister);
        TextView tvBack     = findViewById(R.id.tvBack);

        // ── TextColor: change password info label color based on length ──
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
                    tvPasswordInfo.setText("Password length OK ✔");
                    tvPasswordInfo.setTextColor(getColor(R.color.success));
                }
            }
        });

        // ── Button + Toast + SQLite validation ──
        btnSignIn.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = db.loginUser(username, password);
            if (user != null) {
                session.saveSession(user.id, user.getFullName(), user.username);
                Toast.makeText(this, "Welcome back, " + user.firstName + "! 🔧", Toast.LENGTH_SHORT).show();
                // Intent: go to HomeActivity
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Intent: go to RegisterActivity
        tvGoReg.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        // Intent: go back to MainActivity
        tvBack.setOnClickListener(v -> finish());
    }
}
