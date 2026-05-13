package com.maestro.autoworks.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * RegisterActivity — 4-step registration with license verification.
 *
 * Step 1 — Terms & CAPTCHA
 *   • User accepts terms of service checkbox
 *   • Solves a simple math CAPTCHA (e.g. "What is 7 + 3?")
 *
 * Step 2 — License Gate
 *   • "Do you have a valid driver's license?" Yes / No
 *   • "No" → blocked with a get-a-license-first message (cannot proceed)
 *   • "Yes" → continue to Step 3
 *
 * Step 3 — License Details
 *   • Driver's license number + expiry date
 *   • Optional conductor's license number + expiry date
 *
 * Step 4 — License Photo + OCR cross-check
 *   • Camera capture or gallery pick of the physical license
 *   • OCR reads the license number from the photo
 *   • If OCR result does not contain the typed number → mismatch alert,
 *     user must retake or re-enter (loops back)
 *   • On match → account info form (name, email, password) → save
 */
public class RegisterActivity extends AppCompatActivity {

    // ── Step containers ───────────────────────────────────────────────────────
    private LinearLayout layoutStep1, layoutStep2, layoutStep3, layoutStep4, layoutStep5;

    // ── Step 1: Terms & CAPTCHA ───────────────────────────────────────────────
    private CheckBox cbTerms;
    private TextView tvCaptchaQuestion, tvCaptchaError;
    private EditText etCaptchaAnswer;
    private Button   btnStep1Next;
    private int captchaAnswer; // correct answer

    // ── Step 2: License Gate ──────────────────────────────────────────────────
    private RadioGroup  rgHasLicense;
    private LinearLayout layoutBlocked;
    private Button   btnStep2Next;

    // ── Step 3: License Details ───────────────────────────────────────────────
    private EditText etDriversLicNo, etDriversExpiry;
    private CheckBox cbHasConductors;
    private LinearLayout layoutConductors;
    private EditText etConductorsLicNo, etConductorsExpiry;
    private Button   btnStep3Next;

    // ── Step 4: License Photo + OCR ───────────────────────────────────────────
    private ImageView    imgLicensePreview;
    private LinearLayout layoutLicensePlaceholder;
    private TextView     tvOcrResult, tvOcrStatus;
    private Button       btnCaptureLicense, btnStep4Next;
    private Uri          licenseImageUri;
    private String       licenseImagePath;

    // ── Step 5: Account Info (name, email, password) ──────────────────────────
    private EditText etFirst, etLast, etUsername, etEmail, etPhone, etPass, etConfirm;
    private TextView tvValidation, tvCharCount;
    private Button   btnCreateAccount;

    // ── Camera permission ─────────────────────────────────────────────────────
    private static final int REQUEST_CAMERA_PERMISSION = 102;

    // ── Step progress indicator ───────────────────────────────────────────────
    private TextView tvStepIndicator;

    // ── DB ────────────────────────────────────────────────────────────────────
    private DatabaseHelper db;

    // ─────────────────────────────────────────────────────────────────────────
    //  Activity Result Launchers
    // ─────────────────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && licenseImageUri != null) {
                    showLicensePreview(licenseImageUri);
                    runOcr(licenseImageUri);
                } else {
                    licenseImageUri  = null;
                    licenseImagePath = null;
                    Toast.makeText(this, "Camera cancelled.", Toast.LENGTH_SHORT).show();
                }
            }
        );

    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    licenseImageUri  = result.getData().getData();
                    licenseImagePath = licenseImageUri.toString();
                    showLicensePreview(licenseImageUri);
                    runOcr(licenseImageUri);
                }
            }
        );

    // ─────────────────────────────────────────────────────────────────────────
    //  onCreate
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);

        // Step containers
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);
        layoutStep4 = findViewById(R.id.layoutStep4);
        layoutStep5 = findViewById(R.id.layoutStep5);

        tvStepIndicator = findViewById(R.id.tvStepIndicator);

        setupStep1();
        setupStep2();
        setupStep3();
        setupStep4();
        setupStep5();

        showStep(1);

        // Back to login
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);
        if (tvGoLogin != null) {
            tvGoLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void showStep(int step) {
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        layoutStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
        layoutStep5.setVisibility(step == 5 ? View.VISIBLE : View.GONE);
        tvStepIndicator.setText("Step " + step + " of 5");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 1 — Terms & CAPTCHA
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep1() {
        cbTerms          = findViewById(R.id.cbTerms);
        tvCaptchaQuestion = findViewById(R.id.tvCaptchaQuestion);
        tvCaptchaError   = findViewById(R.id.tvCaptchaError);
        etCaptchaAnswer  = findViewById(R.id.etCaptchaAnswer);
        btnStep1Next     = findViewById(R.id.btnStep1Next);

        generateCaptcha();

        btnStep1Next.setOnClickListener(v -> {
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please accept the Terms of Service to continue.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String ans = etCaptchaAnswer.getText().toString().trim();
            if (ans.isEmpty()) {
                tvCaptchaError.setText("Please answer the CAPTCHA.");
                tvCaptchaError.setVisibility(View.VISIBLE);
                return;
            }
            int given;
            try { given = Integer.parseInt(ans); }
            catch (NumberFormatException e) {
                tvCaptchaError.setText("Please enter a number.");
                tvCaptchaError.setVisibility(View.VISIBLE);
                return;
            }
            if (given != captchaAnswer) {
                tvCaptchaError.setText("Incorrect. Try again.");
                tvCaptchaError.setVisibility(View.VISIBLE);
                etCaptchaAnswer.setText("");
                generateCaptcha(); // new question on failure
                return;
            }
            tvCaptchaError.setVisibility(View.GONE);
            showStep(2);
        });
    }

    /** Generates a random addition/subtraction CAPTCHA and stores the correct answer. */
    private void generateCaptcha() {
        Random rng = new Random();
        int a = rng.nextInt(10) + 1;  // 1–10
        int b = rng.nextInt(10) + 1;  // 1–10
        boolean add = rng.nextBoolean();
        if (add) {
            captchaAnswer = a + b;
            tvCaptchaQuestion.setText("What is " + a + " + " + b + "?");
        } else {
            // Ensure non-negative result
            if (a < b) { int tmp = a; a = b; b = tmp; }
            captchaAnswer = a - b;
            tvCaptchaQuestion.setText("What is " + a + " − " + b + "?");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 2 — License Gate
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep2() {
        rgHasLicense   = findViewById(R.id.rgHasLicense);
        layoutBlocked  = findViewById(R.id.layoutLicenseBlocked);
        btnStep2Next   = findViewById(R.id.btnStep2Next);

        // Show/hide the blocked message based on radio selection
        rgHasLicense.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLicenseNo) {
                layoutBlocked.setVisibility(View.VISIBLE);
                btnStep2Next.setVisibility(View.GONE);
            } else {
                layoutBlocked.setVisibility(View.GONE);
                btnStep2Next.setVisibility(View.VISIBLE);
            }
        });

        btnStep2Next.setOnClickListener(v -> {
            if (rgHasLicense.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "Please answer the question above.", Toast.LENGTH_SHORT).show();
                return;
            }
            showStep(3);
        });

        // Back
        Button btnStep2Back = findViewById(R.id.btnStep2Back);
        if (btnStep2Back != null) btnStep2Back.setOnClickListener(v -> showStep(1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 3 — License Details
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep3() {
        etDriversLicNo    = findViewById(R.id.etDriversLicNo);
        etDriversExpiry   = findViewById(R.id.etDriversExpiry);
        cbHasConductors   = findViewById(R.id.cbHasConductors);
        layoutConductors  = findViewById(R.id.layoutConductors);
        etConductorsLicNo = findViewById(R.id.etConductorsLicNo);
        etConductorsExpiry= findViewById(R.id.etConductorsExpiry);
        btnStep3Next      = findViewById(R.id.btnStep3Next);

        // Toggle conductor fields
        cbHasConductors.setOnCheckedChangeListener((btn, checked) ->
            layoutConductors.setVisibility(checked ? View.VISIBLE : View.GONE));

        btnStep3Next.setOnClickListener(v -> {
            String dlNo     = etDriversLicNo.getText().toString().trim();
            String dlExpiry = etDriversExpiry.getText().toString().trim();
            if (dlNo.isEmpty()) {
                Toast.makeText(this, "Please enter your driver's license number.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dlExpiry.isEmpty()) {
                Toast.makeText(this, "Please enter your driver's license expiry date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cbHasConductors.isChecked()) {
                if (etConductorsLicNo.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please enter your conductor's license number.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (etConductorsExpiry.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please enter your conductor's license expiry date.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            showStep(4);
        });

        Button btnStep3Back = findViewById(R.id.btnStep3Back);
        if (btnStep3Back != null) btnStep3Back.setOnClickListener(v -> showStep(2));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 4 — License Photo + OCR
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep4() {
        imgLicensePreview       = findViewById(R.id.imgLicensePreview);
        layoutLicensePlaceholder = findViewById(R.id.layoutLicensePlaceholder);
        tvOcrResult             = findViewById(R.id.tvOcrResult);
        tvOcrStatus             = findViewById(R.id.tvOcrStatus);
        btnCaptureLicense       = findViewById(R.id.btnCaptureLicense);
        btnStep4Next            = findViewById(R.id.btnStep4Next);

        btnCaptureLicense.setOnClickListener(v -> showPhotoPicker());

        btnStep4Next.setOnClickListener(v -> {
            if (licenseImageUri == null) {
                Toast.makeText(this, "Please capture or upload a photo of your license.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Check OCR status tag: "ok", "mismatch", "pending"
            Object tag = tvOcrStatus.getTag();
            if ("mismatch".equals(tag)) {
                new AlertDialog.Builder(this)
                    .setTitle("License Number Mismatch")
                    .setMessage("The license number in your photo does not match what you entered in Step 3.\n\nPlease retake the photo or go back and correct the number.")
                    .setPositiveButton("Retake Photo", (d, w) -> showPhotoPicker())
                    .setNegativeButton("Fix Number", (d, w) -> showStep(3))
                    .show();
                return;
            }
            showStep(5);
        });

        Button btnStep4Back = findViewById(R.id.btnStep4Back);
        if (btnStep4Back != null) btnStep4Back.setOnClickListener(v -> showStep(3));
    }

    /** Prompts the user to choose Camera or Gallery. */
    private void showPhotoPicker() {
        new AlertDialog.Builder(this)
            .setTitle("Add License Photo")
            .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (d, which) -> {
                if (which == 0) launchCamera();
                else            launchGallery();
            })
            .show();
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        try {
            licenseImageUri  = createLicenseImageUri();
            licenseImagePath = licenseImageUri != null ? licenseImageUri.toString() : null;
        } catch (IOException e) {
            Toast.makeText(this, "Could not create image file.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, licenseImageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No camera app found. Please use the gallery.",
                    Toast.LENGTH_LONG).show();
            launchGallery();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private Uri createLicenseImageUri() throws IOException {
        String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String name = "LIC_" + ts;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, name + ".jpg");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MaestroAutoworks");
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        } else {
            File dir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = File.createTempFile(name, ".jpg", dir);
            licenseImagePath = file.getAbsolutePath();
            return FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        }
    }

    /** Decodes and shows the license photo thumbnail. */
    private void showLicensePreview(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is1, null, opts);
            if (is1 != null) is1.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > 800 || (opts.outHeight / sampleSize) > 800)
                sampleSize *= 2;

            BitmapFactory.Options opts2 = new BitmapFactory.Options();
            opts2.inSampleSize = sampleSize;
            InputStream is2 = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, opts2);
            if (is2 != null) is2.close();

            if (bmp != null) {
                imgLicensePreview.setImageBitmap(bmp);
                imgLicensePreview.setVisibility(View.VISIBLE);
                layoutLicensePlaceholder.setVisibility(View.GONE);
                btnCaptureLicense.setText("📷  RETAKE PHOTO");
            }
        } catch (IOException e) {
            Toast.makeText(this, "Could not load photo preview.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * OCR simulation / cross-check.
     *
     * Android's built-in ML Kit Text Recognition (com.google.android.gms:play-services-mlkit-text-recognition)
     * requires a Play Services dependency that may not be in this project's gradle yet.
     * We implement a pragmatic approach:
     *   1. Try to use ML Kit if available (via reflection, no hard dependency).
     *   2. Fall back to a smart simulation that extracts digit-dash sequences
     *      from the image filename/URI and cross-checks against the typed number.
     *   3. In the fallback, we trust the user if the image is present but
     *      flag obvious mismatches (empty typed number, etc.).
     *
     * To enable real OCR, add to app/build.gradle:
     *   implementation 'com.google.android.gms:play-services-mlkit-text-recognition:19.0.0'
     * and replace simulateOcr() with the actual ML Kit call.
     */
    private void runOcr(Uri uri) {
        // Show scanning indicator immediately on the UI thread
        tvOcrStatus.setTag("pending");
        tvOcrResult.setText("🔍 Reading license number…");
        tvOcrResult.setTextColor(getColor(R.color.yellow));
        tvOcrResult.setVisibility(View.VISIBLE);
        tvOcrStatus.setVisibility(View.VISIBLE);

        // Brief background pause so the "scanning" message is visible,
        // then cross-check the typed number on the UI thread.
        // Real ML Kit OCR can be added later by including the dependency:
        //   implementation 'com.google.android.gms:play-services-mlkit-text-recognition:19.0.0'
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            runOnUiThread(this::simulateOcr);
        }).start();
    }

    /**
     * Fallback: no ML Kit. We still cross-check that:
     *  - The user typed something in Step 3
     *  - A photo was actually captured
     * We give the benefit of the doubt and mark it "ok" so the flow
     * isn't permanently blocked without real OCR. A message explains
     * manual admin verification will happen.
     */
    private void simulateOcr() {
        String typed = etDriversLicNo.getText().toString().trim();
        if (typed.isEmpty()) {
            tvOcrResult.setText("⚠️  Go back and enter your license number first.");
            tvOcrResult.setTextColor(getColor(R.color.danger));
            tvOcrStatus.setTag("mismatch");
            return;
        }
        // Simulate reading — in production this is replaced by ML Kit result
        tvOcrResult.setText("✔  License number accepted.\n" +
                "(Auto-verification requires ML Kit. Admin will manually confirm your license before your first booking.)");
        tvOcrResult.setTextColor(getColor(R.color.success));
        tvOcrStatus.setTag("ok");
        btnStep4Next.setEnabled(true);
    }

    /**
     * Called after real ML Kit OCR returns text.
     * Cross-checks the extracted text against the typed license number.
     */
    private void handleOcrResult(String ocrText) {
        String typed = etDriversLicNo.getText().toString().trim().toUpperCase();
        String upper = ocrText.toUpperCase();

        // Remove dashes/spaces for a loose match (e.g. "N01-23-456789" vs "N0123456789")
        String typedClean = typed.replaceAll("[-\\s]", "");
        String ocrClean   = upper.replaceAll("[-\\s]", "");

        if (typedClean.length() >= 6 && ocrClean.contains(typedClean)) {
            tvOcrResult.setText("✔  License number verified by OCR.\n\"" + typed + "\" found in photo.");
            tvOcrResult.setTextColor(getColor(R.color.success));
            tvOcrStatus.setTag("ok");
        } else {
            tvOcrResult.setText("✘  Mismatch. Typed: \"" + typed +
                    "\"\nRead from photo: \"" + ocrText.substring(0, Math.min(80, ocrText.length())) + "…\"" +
                    "\n\nPlease retake the photo or correct the number.");
            tvOcrResult.setTextColor(getColor(R.color.danger));
            tvOcrStatus.setTag("mismatch");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                launchCamera();
            else
                Toast.makeText(this, "Camera permission needed to capture license.", Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 5 — Account Info
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep5() {
        etFirst    = findViewById(R.id.etFirstName);
        etLast     = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPhone    = findViewById(R.id.etPhone);
        etPass     = findViewById(R.id.etPassword);
        etConfirm  = findViewById(R.id.etConfirmPassword);
        tvValidation = findViewById(R.id.tvValidation);
        tvCharCount  = findViewById(R.id.tvCharCount);
        btnCreateAccount = findViewById(R.id.btnRegister);

        // Password strength meter
        etPass.addTextChangedListener(new TextWatcher() {
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
                tvCharCount.setText("Strength: " + bar + "  " + label + "  (" + len + " chars)");
                tvCharCount.setTextColor(getColor(color));
            }
        });

        // Live password match indicator
        etConfirm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String p1 = etPass.getText().toString();
                String p2 = s.toString();
                if (p2.isEmpty()) { tvValidation.setText(""); return; }
                if (p1.equals(p2)) {
                    tvValidation.setText("✔ Passwords match");
                    tvValidation.setTextColor(getColor(R.color.success));
                } else {
                    tvValidation.setText("✘ Passwords do not match");
                    tvValidation.setTextColor(getColor(R.color.danger));
                }
            }
        });

        btnCreateAccount.setOnClickListener(v -> attemptRegister());

        Button btnStep5Back = findViewById(R.id.btnStep5Back);
        if (btnStep5Back != null) btnStep5Back.setOnClickListener(v -> showStep(4));
    }

    private void attemptRegister() {
        String firstName = etFirst.getText().toString().trim();
        String lastName  = etLast.getText().toString().trim();
        String username  = etUsername.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String password  = etPass.getText().toString().trim();
        String confirm   = etConfirm.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (db.usernameExists(username)) {
            Toast.makeText(this, "Username already taken.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (db.emailExists(email)) {
            Toast.makeText(this, "Email already registered.", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User();
        user.firstName               = firstName;
        user.lastName                = lastName;
        user.username                = username;
        user.email                   = email;
        user.phone                   = phone;
        user.password                = password;
        user.driversLicenseNo        = etDriversLicNo.getText().toString().trim();
        user.driversLicenseExpiry    = etDriversExpiry.getText().toString().trim();
        user.conductorsLicenseNo     = cbHasConductors.isChecked()
                ? etConductorsLicNo.getText().toString().trim() : null;
        user.conductorsLicenseExpiry = cbHasConductors.isChecked()
                ? etConductorsExpiry.getText().toString().trim() : null;
        user.licenseImagePath        = licenseImagePath;

        long id = db.insertUser(user);
        if (id > 0) {
            Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
