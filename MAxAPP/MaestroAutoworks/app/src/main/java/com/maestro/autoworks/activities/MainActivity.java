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

        // If already logged in as admin, route to admin dashboard
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn() && session.isAdmin()) {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
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

        // ── Buttons ──────────────────────────────────────────────────────
        Button btnBookNow    = findViewById(R.id.btnBookNow);    // hero button
        Button btnBookNowCta = findViewById(R.id.btnBookNowCta); // CTA banner button

        // Book Now: go to BookActivity if logged in, otherwise go to Login first
        android.view.View.OnClickListener bookNowClick = v -> {
            if (session.isLoggedIn()) {
                startActivity(new Intent(this, BookActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        };

        if (btnBookNow    != null) btnBookNow.setOnClickListener(bookNowClick);
        if (btnBookNowCta != null) btnBookNowCta.setOnClickListener(bookNowClick);
    }
}
