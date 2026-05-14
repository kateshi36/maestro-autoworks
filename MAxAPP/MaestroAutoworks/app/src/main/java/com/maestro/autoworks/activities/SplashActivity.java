package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maestro.autoworks.R;

/**
 * SplashActivity — Animated launch screen.
 *
 * Sequence:
 *   0 ms  → Logo scales + fades in
 *   500 ms → Brand name + tagline slide up & fade in
 *   1000 ms → "Let's Start!" button fades up
 *
 * Tapping "Let's Start!" routes to:
 *   - AdminDashboardActivity  (if admin session active)
 *   - HomeActivity            (if customer session active)
 *   - MainActivity            (public landing page)
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar on this screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);

        ImageView logo       = findViewById(R.id.splashLogo);
        TextView  brandName  = findViewById(R.id.splashBrandName);
        TextView  tagline    = findViewById(R.id.splashTagline);
        Button    btnStart   = findViewById(R.id.btnLetsStart);

        // ── Load animations ──────────────────────────────────────────────
        Animation logoAnim   = AnimationUtils.loadAnimation(this, R.anim.splash_logo_in);
        Animation textAnim   = AnimationUtils.loadAnimation(this, R.anim.splash_text_in);
        Animation btnAnim    = AnimationUtils.loadAnimation(this, R.anim.splash_button_in);

        // ── Step 1: Show logo ─────────────────────────────────────────────
        logo.setVisibility(View.VISIBLE);
        logo.startAnimation(logoAnim);

        // ── Step 2: Show brand name + tagline after logo starts ───────────
        brandName.setVisibility(View.VISIBLE);
        brandName.startAnimation(textAnim);

        tagline.setVisibility(View.VISIBLE);
        tagline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_in));

        // ── Step 3: Show button last ──────────────────────────────────────
        btnStart.setVisibility(View.VISIBLE);
        btnStart.startAnimation(btnAnim);

        // ── Button click: always go to Login ─────────────────────────────
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}
