package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;

/**
 * MainActivity — Public Landing Page.
 * Mirrors the website's index.php:
 *   • Hero stats (15+ yrs, 12 mechanics, 50+ cars/day, 98% satisfaction)
 *   • Feature highlights (Certified · Fast · Online Booking · Transparent Pricing)
 *   • Services preview count from DB
 *   • About section stats
 *   • CTA: Sign In / Sign Up / Visit Website
 */
public class MainActivity extends AppCompatActivity {

    /** Maestro Autoworks website — update when deployed. */
    private static final String WEBSITE_URL = "https://maestroautoworks.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already logged in, route appropriately
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            Intent intent = session.isAdmin()
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Live service count badge
        DatabaseHelper db = new DatabaseHelper(this);
        TextView tvSvcCount = findViewById(R.id.tvServicesCount);
        if (tvSvcCount != null) {
            int catalogCount = com.maestro.autoworks.db.ServiceData.getAll().size();
            tvSvcCount.setText(catalogCount + "+ Services Available");
        }

        // Buttons
        Button btnLogin        = findViewById(R.id.btnLogin);
        Button btnRegister     = findViewById(R.id.btnRegister);
        Button btnServices     = findViewById(R.id.btnExploreServices);
        Button btnVisitWebsite = findViewById(R.id.btnVisitWebsite);

        btnLogin.setOnClickListener(v ->
            startActivity(new Intent(this, LoginActivity.class)));

        btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        if (btnServices != null) {
            btnServices.setOnClickListener(v ->
                startActivity(new Intent(this, ServicesActivity.class)));
        }

        // ── Visit Website ─────────────────────────────────────────────────

    }

    /** Called by the Sign Up FREE button in the CTA banner (onClick="onClickRegister"). */
    public void onClickRegister(android.view.View v) {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
