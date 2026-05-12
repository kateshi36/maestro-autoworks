package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.SessionManager;

/**
 * MainActivity — Public landing page.
 * Demonstrates: TextView, Button, Intent.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already logged in, go straight to home
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Button btnLogin    = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Intent: navigate to LoginActivity
        btnLogin.setOnClickListener(v ->
            startActivity(new Intent(this, LoginActivity.class)));

        // Intent: navigate to RegisterActivity
        btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
    }
}
