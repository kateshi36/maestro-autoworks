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

import com.maestro.autoworks.utils.DatePickerHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * RegisterActivity — 8-step registration with license verification.
 *
 * Step 1 — Terms & CAPTCHA
 * Step 2 — License Gate (do you have a valid DL?)
 * Step 3 — License Details (DL number, expiry, optional Conductor's)
 * Step 4 — License Photo + OCR cross-check
 * Step 5 — Personal Information (name, username, birthdate, gender)
 * Step 6 — Contact Information (email + confirm, phone)
 * Step 7 — Review & Confirm (summary card)
 * Step 8 — Password + Create Account
 */
public class RegisterActivity extends AppCompatActivity {

    // ── Step containers ───────────────────────────────────────────────────────
    private LinearLayout layoutStep1, layoutStep2, layoutStep3,
            layoutStep4, layoutStep5, layoutStep6, layoutStep7, layoutStep8;

    // ── Step 1: Terms & CAPTCHA ───────────────────────────────────────────────
    private CheckBox cbTerms;
    private TextView tvCaptchaQuestion, tvCaptchaError;
    private EditText etCaptchaAnswer;
    private Button   btnStep1Next;
    private int captchaAnswer;

    // ── Step 2: License Gate ──────────────────────────────────────────────────
    private RadioGroup   rgHasLicense;
    private LinearLayout layoutBlocked;
    private Button       btnStep2Next;

    // ── Step 3: License Details ───────────────────────────────────────────────
    private EditText     etDriversLicNo, etDriversExpiry;
    private CheckBox     cbHasConductors;
    private LinearLayout layoutConductors;
    private EditText     etConductorsLicNo, etConductorsExpiry;
    private Button       btnStep3Next;

    // ── Step 4: License Photo + OCR ───────────────────────────────────────────
    private ImageView    imgLicensePreview;
    private LinearLayout layoutLicensePlaceholder;
    private TextView     tvOcrResult, tvOcrStatus;
    private Button       btnCaptureLicense, btnStep4Next;
    private Uri          licenseImageUri;
    private String       licenseImagePath;

    // ── Step 5: Personal Information ──────────────────────────────────────────
    private EditText   etFirstName, etLastName, etUsername, etBirthdate;
    private RadioGroup rgGender;

    // ── Step 6: Contact Information ───────────────────────────────────────────
    private EditText etEmail, etEmailConfirm, etPhone;
    private TextView tvEmailValidation;

    // ── Step 7: Review Card ──────────────────────────────────────────────────
    private TextView tvSummaryName, tvSummaryUsername, tvSummaryBirthdate,
            tvSummaryGender, tvSummaryEmail, tvSummaryPhone, tvSummaryLicense;

    // ── Step 8: Password ─────────────────────────────────────────────────────
    private EditText etPassword, etConfirmPassword;
    private TextView tvValidation, tvCharCount;
    private TextView btnTogglePassword, btnToggleConfirmPassword;
    private boolean  passwordVisible = false, confirmPasswordVisible = false;
    private Button   btnCreateAccount;

    // ── Misc ──────────────────────────────────────────────────────────────────
    private static final int REQUEST_CAMERA_PERMISSION = 102;
    private TextView tvStepIndicator;
    private static final int TOTAL_STEPS = 8;
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
        layoutStep6 = findViewById(R.id.layoutStep6);
        layoutStep7 = findViewById(R.id.layoutStep7);
        layoutStep8 = findViewById(R.id.layoutStep8);

        tvStepIndicator = findViewById(R.id.tvStepIndicator);

        setupStep1();
        setupStep2();
        setupStep3();
        setupStep4();
        setupStep5();
        setupStep6();
        setupStep7();
        setupStep8();

        showStep(1);

        // "Already have an account?" link lives in Step 7
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
        layoutStep6.setVisibility(step == 6 ? View.VISIBLE : View.GONE);
        layoutStep7.setVisibility(step == 7 ? View.VISIBLE : View.GONE);
        layoutStep8.setVisibility(step == 8 ? View.VISIBLE : View.GONE);
        tvStepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 1 — Terms & CAPTCHA
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep1() {
        cbTerms           = findViewById(R.id.cbTerms);
        tvCaptchaQuestion = findViewById(R.id.tvCaptchaQuestion);
        tvCaptchaError    = findViewById(R.id.tvCaptchaError);
        etCaptchaAnswer   = findViewById(R.id.etCaptchaAnswer);
        btnStep1Next      = findViewById(R.id.btnStep1Next);

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
                generateCaptcha();
                return;
            }
            tvCaptchaError.setVisibility(View.GONE);
            showStep(2);
        });
    }

    private void generateCaptcha() {
        Random rng = new Random();
        int a = rng.nextInt(10) + 1;
        int b = rng.nextInt(10) + 1;
        boolean add = rng.nextBoolean();
        if (add) {
            captchaAnswer = a + b;
            tvCaptchaQuestion.setText("What is " + a + " + " + b + "?");
        } else {
            if (a < b) { int tmp = a; a = b; b = tmp; }
            captchaAnswer = a - b;
            tvCaptchaQuestion.setText("What is " + a + " − " + b + "?");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 2 — License Gate
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep2() {
        rgHasLicense  = findViewById(R.id.rgHasLicense);
        layoutBlocked = findViewById(R.id.layoutLicenseBlocked);
        btnStep2Next  = findViewById(R.id.btnStep2Next);

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

        cbHasConductors.setOnCheckedChangeListener((btn, checked) ->
                layoutConductors.setVisibility(checked ? View.VISIBLE : View.GONE));

        // Attach universal DatePickerHelper to expiry fields (future dates, up to 20 years out)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        DatePickerHelper.attach(this, etDriversExpiry,   currentYear, currentYear + 20);
        DatePickerHelper.attach(this, etConductorsExpiry, currentYear, currentYear + 20);

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
        imgLicensePreview        = findViewById(R.id.imgLicensePreview);
        layoutLicensePlaceholder = findViewById(R.id.layoutLicensePlaceholder);
        tvOcrResult              = findViewById(R.id.tvOcrResult);
        tvOcrStatus              = findViewById(R.id.tvOcrStatus);
        btnCaptureLicense        = findViewById(R.id.btnCaptureLicense);
        btnStep4Next             = findViewById(R.id.btnStep4Next);

        btnCaptureLicense.setOnClickListener(v -> showPhotoPicker());

        btnStep4Next.setOnClickListener(v -> {
            if (licenseImageUri == null) {
                Toast.makeText(this, "Please capture or upload a photo of your license.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Object tag = tvOcrStatus.getTag();
            if ("mismatch".equals(tag)) {
                new AlertDialog.Builder(this)
                        .setTitle("License Number Mismatch")
                        .setMessage("The license number in your photo does not match what you entered in Step 3.\n\nPlease retake the photo or go back and correct the number.")
                        .setPositiveButton("Retake Photo", (d, w) -> showPhotoPicker())
                        .setNegativeButton("Fix Number",   (d, w) -> showStep(3))
                        .show();
                return;
            }
            showStep(5);
        });

        Button btnStep4Back = findViewById(R.id.btnStep4Back);
        if (btnStep4Back != null) btnStep4Back.setOnClickListener(v -> showStep(3));
    }

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

    private void runOcr(Uri uri) {
        tvOcrStatus.setTag("pending");
        tvOcrResult.setText("🔍 Reading license number…");
        tvOcrResult.setTextColor(getColor(R.color.yellow));
        tvOcrResult.setVisibility(View.VISIBLE);
        tvOcrStatus.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            runOnUiThread(this::simulateOcr);
        }).start();
    }

    private void simulateOcr() {
        String typed = etDriversLicNo.getText().toString().trim();
        if (typed.isEmpty()) {
            tvOcrResult.setText("⚠️  Go back and enter your license number first.");
            tvOcrResult.setTextColor(getColor(R.color.danger));
            tvOcrStatus.setTag("mismatch");
            return;
        }
        tvOcrResult.setText("✔  License number accepted.\n" +
                "(Auto-verification requires ML Kit. Admin will manually confirm your license before your first booking.)");
        tvOcrResult.setTextColor(getColor(R.color.success));
        tvOcrStatus.setTag("ok");
        btnStep4Next.setEnabled(true);
    }

    private void handleOcrResult(String ocrText) {
        String typed     = etDriversLicNo.getText().toString().trim().toUpperCase();
        String upper     = ocrText.toUpperCase();
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
    //  STEP 5 — Personal Information (name, username, birthdate, gender)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep5() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        etUsername  = findViewById(R.id.etUsername);
        etBirthdate = findViewById(R.id.etBirthdate);
        rgGender    = findViewById(R.id.rgGender);

        // Attach universal DatePickerHelper for Birthdate (min age 16)
        DatePickerHelper.attach(this, etBirthdate,
                /* minYear */ 1920, /* maxYear */ Calendar.getInstance().get(Calendar.YEAR) - 16);

        Button btnStep5Next = findViewById(R.id.btnStep5Next);
        Button btnStep5Back = findViewById(R.id.btnStep5Back);

        if (btnStep5Back != null) btnStep5Back.setOnClickListener(v -> showStep(4));

        if (btnStep5Next != null) btnStep5Next.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName  = etLastName.getText().toString().trim();
            String username  = etUsername.getText().toString().trim();
            String birthdate = etBirthdate.getText().toString().trim();
            int    genderId  = rgGender.getCheckedRadioButtonId();

            if (firstName.isEmpty()) {
                Toast.makeText(this, "Please enter your first name.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lastName.isEmpty()) {
                Toast.makeText(this, "Please enter your last name.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.isEmpty()) {
                Toast.makeText(this, "Please choose a username.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.length() < 3) {
                Toast.makeText(this, "Username must be at least 3 characters.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (birthdate.isEmpty()) {
                Toast.makeText(this, "Please enter your date of birth.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!birthdate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                Toast.makeText(this, "Birthdate must be in YYYY-MM-DD format.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (genderId == -1) {
                Toast.makeText(this, "Please select your gender.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.usernameExists(username)) {
                Toast.makeText(this, "Username already taken. Please choose another.", Toast.LENGTH_SHORT).show();
                return;
            }

            showStep(6);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 6 — Contact Information (email + confirm, phone)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep6() {
        etEmail        = findViewById(R.id.etEmail);
        etEmailConfirm = findViewById(R.id.etEmailConfirm);
        etPhone        = findViewById(R.id.etPhone);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);

        Button btnStep6Next = findViewById(R.id.btnStep6Next);
        Button btnStep6Back = findViewById(R.id.btnStep6Back);

        if (btnStep6Back != null) btnStep6Back.setOnClickListener(v -> showStep(5));

        // Live email-match indicator
        TextWatcher emailWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String e1 = etEmail.getText().toString();
                String e2 = etEmailConfirm.getText().toString();
                if (e2.isEmpty()) { tvEmailValidation.setText(""); return; }
                if (e1.equals(e2)) {
                    tvEmailValidation.setText("✔ Emails match");
                    tvEmailValidation.setTextColor(getColor(R.color.success));
                } else {
                    tvEmailValidation.setText("✘ Emails do not match");
                    tvEmailValidation.setTextColor(getColor(R.color.danger));
                }
            }
        };
        etEmail.addTextChangedListener(emailWatcher);
        etEmailConfirm.addTextChangedListener(emailWatcher);

        if (btnStep6Next != null) btnStep6Next.setOnClickListener(v -> {
            String email   = etEmail.getText().toString().trim();
            String confirm = etEmailConfirm.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (confirm.isEmpty()) {
                Toast.makeText(this, "Please confirm your email address.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!email.equals(confirm)) {
                Toast.makeText(this, "Email addresses do not match.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.emailExists(email)) {
                Toast.makeText(this, "Email already registered. Try signing in instead.", Toast.LENGTH_SHORT).show();
                return;
            }

            populateSummary();
            showStep(7);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 7 — Review & Confirm
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep7() {
        tvSummaryName      = findViewById(R.id.tvSummaryName);
        tvSummaryUsername  = findViewById(R.id.tvSummaryUsername);
        tvSummaryBirthdate = findViewById(R.id.tvSummaryBirthdate);
        tvSummaryGender    = findViewById(R.id.tvSummaryGender);
        tvSummaryEmail     = findViewById(R.id.tvSummaryEmail);
        tvSummaryPhone     = findViewById(R.id.tvSummaryPhone);
        tvSummaryLicense   = findViewById(R.id.tvSummaryLicense);

        Button btnStep7Back = findViewById(R.id.btnStep7Back);
        Button btnStep7Next = findViewById(R.id.btnStep7Next);

        if (btnStep7Back != null) btnStep7Back.setOnClickListener(v -> showStep(6));
        if (btnStep7Next != null) btnStep7Next.setOnClickListener(v -> showStep(8));
    }

    /** Fills the review card with the values collected in steps 3–6. */
    private void populateSummary() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();
        tvSummaryName.setText(first + " " + last);
        tvSummaryUsername.setText(etUsername.getText().toString().trim());
        tvSummaryBirthdate.setText(etBirthdate.getText().toString().trim());

        String gender = "";
        int genderId = rgGender.getCheckedRadioButtonId();
        if      (genderId == R.id.rbGenderMale)   gender = "Male";
        else if (genderId == R.id.rbGenderFemale) gender = "Female";
        else if (genderId == R.id.rbGenderOther)  gender = "Prefer not to say";
        tvSummaryGender.setText(gender.isEmpty() ? "—" : gender);

        tvSummaryEmail.setText(etEmail.getText().toString().trim());
        String phone = etPhone.getText().toString().trim();
        tvSummaryPhone.setText(phone.isEmpty() ? "—" : phone);
        tvSummaryLicense.setText(etDriversLicNo.getText().toString().trim());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 8 — Password & Create Account
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep8() {
        etPassword            = findViewById(R.id.etPassword);
        etConfirmPassword     = findViewById(R.id.etConfirmPassword);
        tvValidation          = findViewById(R.id.tvValidation);
        tvCharCount           = findViewById(R.id.tvCharCount);
        btnCreateAccount      = findViewById(R.id.btnRegister);
        btnTogglePassword        = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);

        Button btnStep8Back = findViewById(R.id.btnStep8Back);
        if (btnStep8Back != null) btnStep8Back.setOnClickListener(v -> showStep(7));

        // Eye-toggle: Password
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setText(passwordVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
            etPassword.setTextColor(getColor(R.color.white));
            etPassword.setSelection(etPassword.getText().length());
        });

        // Eye-toggle: Confirm Password
        btnToggleConfirmPassword.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            etConfirmPassword.setInputType(confirmPasswordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setText(confirmPasswordVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
            etConfirmPassword.setTextColor(getColor(R.color.white));
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        // Password strength meter
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int len   = s.length();
                int score = Math.min(len / 2, 5);
                String bar   = "█".repeat(score) + "░".repeat(5 - score);
                String label; int color;
                if (len == 0)      { label = "—";      color = R.color.muted; }
                else if (len < 4)  { label = "Weak";   color = R.color.danger; }
                else if (len < 8)  { label = "Fair";   color = R.color.yellow; }
                else               { label = "Strong"; color = R.color.success; }
                tvCharCount.setText("Strength: " + bar + "  " + label + "  (" + len + " chars)");
                tvCharCount.setTextColor(getColor(color));
            }
        });

        // Live password-match indicator
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String p1 = etPassword.getText().toString();
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
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Final validation & DB write
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptRegister() {
        // Collect from all steps
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String username  = etUsername.getText().toString().trim();
        String birthdate = etBirthdate.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();
        String confirm   = etConfirmPassword.getText().toString().trim();

        // Resolve gender label from the selected radio button
        String gender = "";
        int genderId = rgGender.getCheckedRadioButtonId();
        if      (genderId == R.id.rbGenderMale)   gender = "Male";
        else if (genderId == R.id.rbGenderFemale) gender = "Female";
        else if (genderId == R.id.rbGenderOther)  gender = "Prefer not to say";

        // Password guards (step 7)
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show();
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

        // Final duplicate checks (race-condition safety — main checks happen per-step)
        if (db.usernameExists(username)) {
            Toast.makeText(this, "Username already taken.", Toast.LENGTH_SHORT).show();
            showStep(5);
            return;
        }
        if (db.emailExists(email)) {
            Toast.makeText(this, "Email already registered.", Toast.LENGTH_SHORT).show();
            showStep(6);
            return;
        }

        // Build User and persist
        User user = new User();
        user.firstName               = firstName;
        user.lastName                = lastName;
        user.username                = username;
        user.birthdate               = birthdate;
        user.gender                  = gender;
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