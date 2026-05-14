package com.maestro.autoworks.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.utils.MediaPickerHelper;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.User;

import com.maestro.autoworks.utils.DatePickerHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;

/**
 * RegisterActivity — 7-step registration with license verification.
 *
 * Step 1 — Terms & CAPTCHA
 * Step 2 — License Gate (do you have a valid DL?)
 * Step 3 — Personal Information (name, username, birthdate, gender, address, contact, email)
 * Step 4 — License Details (DL number, expiry, optional Conductor's)
 * Step 5 — License Photo + OCR cross-check
 * Step 6 — Review & Confirm (summary card)
 * Step 8 — Password + Create Account
 */
public class RegisterActivity extends AppCompatActivity {

    // ── Step containers ───────────────────────────────────────────────────────
    private LinearLayout layoutStep1, layoutStep2, layoutStep3,
            layoutStep4, layoutStep5, layoutStep6, layoutStep8, layoutStep9;

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

    // ── Step 3: Personal Information (name, DOB, gender, address, contact, email) ──
    private EditText etFirstName, etLastName, etUsername, etBirthdate;
    private EditText etAddress;
    private RadioGroup rgGender;

    // ── Step 3 also holds contact/email fields ────────────────────────────────
    private EditText etEmail, etEmailConfirm, etPhone;
    private TextView tvEmailValidation;

    // ── Step 4: Vehicle & Verification Details ────────────────────────────────
    //   Text input fields
    private EditText etLicensePlate;   // Philippine plate, e.g. "ABC 1234"
    private EditText etMvFileNumber;   // 15-digit LTO MV file number
    private EditText etVehicleMake;    // e.g. "Toyota"
    private EditText etVehicleModel;   // e.g. "Vios 1.3 XLE MT"

    //   Inline validation feedback TextViews (one per field)
    private TextView tvLicensePlateError;
    private TextView tvMvFileNumberError;
    private TextView tvVehicleMakeError;
    private TextView tvVehicleModelError;

    //   Document upload URI holders — stored as String paths for DB persistence
    private android.net.Uri dlUploadUri;  // Driver's License gallery pick
    private android.net.Uri orUploadUri;  // Official Receipt
    private android.net.Uri crUploadUri;  // Certificate of Registration

    //   Upload status indicators (show thumbnail / "uploaded" label per document)
    private android.widget.ImageView imgDlPreview;
    private android.widget.ImageView imgOrPreview;
    private android.widget.ImageView imgCrPreview;
    private TextView tvDlUploadStatus;
    private TextView tvOrUploadStatus;
    private TextView tvCrUploadStatus;

    // ── Step 4: Validation-state booleans ─────────────────────────────────────
    // Each flips to true only when the corresponding field passes its regex/rule.
    // btnStep4Next is enabled only when ALL four are true AND all three docs uploaded.
    private boolean isLicensePlateValid = false;
    private boolean isMvFileNumberValid = false;
    private boolean isVehicleMakeValid  = false;
    private boolean isVehicleModelValid = false;
    private boolean isDlUploaded        = false;
    private boolean isOrUploaded        = false;
    private boolean isCrUploaded        = false;

    // ── Step 4: Regex / format rules ──────────────────────────────────────────
    /**
     * Matches current (2014+) Philippine license plates: 3 letters, space, 4 digits.
     * e.g. "ABC 1234"
     * Also accepts pre-2014 format: 2 letters, space, 4 digits. e.g. "AB 1234"
     */
    private static final java.util.regex.Pattern PLATE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z]{2,3}\\s[0-9]{4}$");

    /**
     * Matches the LTO Motor Vehicle File Number: exactly 15 numeric digits.
     * e.g. "123456789012345"
     */
    private static final java.util.regex.Pattern MV_FILE_PATTERN =
            java.util.regex.Pattern.compile("^[0-9]{15}$");

    /**
     * Vehicle make: 2–40 characters, letters/spaces/hyphens only.
     * Rejects purely numeric or blank input.
     */
    private static final java.util.regex.Pattern VEHICLE_MAKE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z][A-Za-z\\s\\-]{1,39}$");

    /**
     * Vehicle model: 2–60 characters, alphanumeric + common punctuation.
     * Permits model strings like "Vios 1.3 XLE MT" or "CRV 4WD (2022)".
     */
    private static final java.util.regex.Pattern VEHICLE_MODEL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\s.\\-/()']{1,59}$");

    // ── Step 4: License Details ───────────────────────────────────────────────
    private EditText     etDriversLicNo, etDriversExpiry;
    private CheckBox     cbHasConductors;
    private LinearLayout layoutConductors;
    private EditText     etConductorsLicNo, etConductorsExpiry;
    private Button       btnStep4Next;

    // ── Step 5: License Photo + OCR ───────────────────────────────────────────
    private ImageView    imgLicensePreview;
    private LinearLayout layoutLicensePlaceholder;
    private TextView     tvOcrResult, tvOcrStatus;
    private Button       btnCaptureLicense, btnStep5Next;
    private Uri          licenseImageUri;
    private String       licenseImagePath;

    // ── Step 6: Review Card ──────────────────────────────────────────────────
    private TextView tvSummaryName, tvSummaryUsername, tvSummaryBirthdate,
            tvSummaryGender, tvSummaryEmail, tvSummaryPhone, tvSummaryLicense;

    // ── Step 8: Password ─────────────────────────────────────────────────────
    private EditText etPassword, etConfirmPassword;
    private TextView tvValidation, tvCharCount;
    private TextView btnTogglePassword, btnToggleConfirmPassword;
    private boolean  passwordVisible = false, confirmPasswordVisible = false;
    private Button   btnCreateAccount;

    // ── Misc ──────────────────────────────────────────────────────────────────
    // REQUEST_CAMERA_PERMISSION removed — now handled by MediaPickerHelper (licensePicker).
    private TextView tvStepIndicator;
    private static final int TOTAL_STEPS = 8;
    private DatabaseHelper db;

    // ─────────────────────────────────────────────────────────────────────────
    //  Activity Result Launchers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * MediaPickerHelper for Step 5 — License Photo capture.
     *
     * Handles Tasks 1-3 of Stage 3:
     *   Task 1: FileProvider URI (API ≤ 28) / MediaStore URI (API ≥ 29)
     *   Task 2: CAMERA (+ WRITE on API ≤ 28) permission request on button tap
     *   Task 3: Camera-vs-Gallery chooser dialog
     */
    private final MediaPickerHelper licensePicker = new MediaPickerHelper(
            this,
            "LIC",                    // filename prefix → LIC_20250515_143022.jpg
            (uri, path) -> {          // delivered on the main thread after pick/capture
                licenseImageUri  = uri;
                licenseImagePath = path;
                showLicensePreview(uri);
                runOcr(uri);
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
        layoutStep8 = findViewById(R.id.layoutStep8);
        layoutStep9 = findViewById(R.id.layoutStep9);

        tvStepIndicator = findViewById(R.id.tvStepIndicator);

        setupStep1();
        setupStep2();
        setupStep3();
        setupStep4();
        setupStep5();
        setupStep6();
        setupStep8();

        showStep(1);

        // "Already have an account?" link lives in Step 8 (Password).
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
        layoutStep8.setVisibility(step == 8 ? View.VISIBLE : View.GONE);
        layoutStep9.setVisibility(step == 9 ? View.VISIBLE : View.GONE);
        if (step == 9) {
            tvStepIndicator.setText("Registration Complete");
        } else {
            tvStepIndicator.setText("Step " + step + " of " + TOTAL_STEPS);
        }
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

        LinearLayout layoutLicenseReady = findViewById(R.id.layoutLicenseReady);

        rgHasLicense.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLicenseNo) {
                layoutBlocked.setVisibility(View.VISIBLE);
                layoutLicenseReady.setVisibility(View.GONE);
                btnStep2Next.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbLicenseYes) {
                layoutBlocked.setVisibility(View.GONE);
                layoutLicenseReady.setVisibility(View.VISIBLE);
                btnStep2Next.setVisibility(View.VISIBLE);
            } else {
                layoutBlocked.setVisibility(View.GONE);
                layoutLicenseReady.setVisibility(View.GONE);
                btnStep2Next.setVisibility(View.GONE);
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
    //  STEP 3 — Personal Information (name, DOB, gender, address, contact, email)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep3() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        etUsername  = findViewById(R.id.etUsername);
        etBirthdate = findViewById(R.id.etBirthdate);
        rgGender    = findViewById(R.id.rgGender);
        etAddress   = findViewById(R.id.etAddress);
        etPhone     = findViewById(R.id.etPhone);
        etEmail        = findViewById(R.id.etEmail);
        etEmailConfirm = findViewById(R.id.etEmailConfirm);
        tvEmailValidation = findViewById(R.id.tvEmailValidation);

        // Attach universal DatePickerHelper for Birthdate (min age 16)
        DatePickerHelper.attach(this, etBirthdate,
                /* minYear */ 1920, /* maxYear */ Calendar.getInstance().get(Calendar.YEAR) - 16);

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

        Button btnStep3Next = findViewById(R.id.btnStep3Next);
        Button btnStep3Back = findViewById(R.id.btnStep3Back);

        if (btnStep3Back != null) btnStep3Back.setOnClickListener(v -> showStep(2));

        if (btnStep3Next != null) btnStep3Next.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName  = etLastName.getText().toString().trim();
            String username  = etUsername.getText().toString().trim();
            String birthdate = etBirthdate.getText().toString().trim();
            int    genderId  = rgGender.getCheckedRadioButtonId();
            String address   = etAddress.getText().toString().trim();
            String phone     = etPhone.getText().toString().trim();
            String email     = etEmail.getText().toString().trim();
            String confirm   = etEmailConfirm.getText().toString().trim();

            // — Name & identity —
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

            // — Address —
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter your residential address.", Toast.LENGTH_SHORT).show();
                return;
            }

            // — Contact number —
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your contact number.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Normalise and validate: accept 09XXXXXXXXX (11 digits) or +639XXXXXXXXX (13 chars)
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            boolean validLocal = digitsOnly.startsWith("0") && digitsOnly.length() == 11;
            boolean validIntl  = phone.startsWith("+63") && digitsOnly.length() == 12;
            if (!validLocal && !validIntl) {
                Toast.makeText(this,
                        "Please enter a valid Philippine mobile number (e.g. 09171234567).",
                        Toast.LENGTH_LONG).show();
                etPhone.requestFocus();
                return;
            }
            // Normalise to 09XXXXXXXXX format before saving
            if (validIntl) {
                phone = "0" + digitsOnly.substring(2); // strip country code, add leading 0
            } else {
                phone = digitsOnly; // already stripped of spaces/dashes
            }

            // — Email —
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

            showStep(4);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 4 — License Details (DL number, expiry, optional Conductor's)
    // ─────────────────────────────────────────────────────────────────────────

    // ── Step 4: which document card is pending a gallery pick ────────────────
    private enum DocTarget { DL, OR, CR }

    // Activity result launcher for Step 4 document gallery picks
    /**
     * MediaPickerHelpers for Step 4 document upload cards (Task 1-3 of Stage 3).
     * Each helper handles FileProvider URI creation, runtime permissions, and the
     * camera-vs-gallery chooser dialog independently for its document slot.
     */
    private final MediaPickerHelper dlPicker = new MediaPickerHelper(
            this, "DL",
            (uri, path) -> applyDocUpload(DocTarget.DL, uri)
    );
    private final MediaPickerHelper orPicker = new MediaPickerHelper(
            this, "OR",
            (uri, path) -> applyDocUpload(DocTarget.OR, uri)
    );
    private final MediaPickerHelper crPicker = new MediaPickerHelper(
            this, "CR",
            (uri, path) -> applyDocUpload(DocTarget.CR, uri)
    );

    /** Called when user picks a gallery image for a document card. */
    private void applyDocUpload(DocTarget target, android.net.Uri uri) {
        switch (target) {
            case DL:
                dlUploadUri = uri;
                isDlUploaded = true;
                imgDlPreview.setImageURI(uri);
                imgDlPreview.setVisibility(View.VISIBLE);
                findViewById(R.id.tvDlPlaceholder).setVisibility(View.GONE);
                tvDlUploadStatus.setText("✔  Uploaded");
                tvDlUploadStatus.setTextColor(getResources().getColor(R.color.success, null));
                break;
            case OR:
                orUploadUri = uri;
                isOrUploaded = true;
                imgOrPreview.setImageURI(uri);
                imgOrPreview.setVisibility(View.VISIBLE);
                findViewById(R.id.tvOrPlaceholder).setVisibility(View.GONE);
                tvOrUploadStatus.setText("✔  Uploaded");
                tvOrUploadStatus.setTextColor(getResources().getColor(R.color.success, null));
                break;
            case CR:
                crUploadUri = uri;
                isCrUploaded = true;
                imgCrPreview.setImageURI(uri);
                imgCrPreview.setVisibility(View.VISIBLE);
                findViewById(R.id.tvCrPlaceholder).setVisibility(View.GONE);
                tvCrUploadStatus.setText("✔  Uploaded");
                tvCrUploadStatus.setTextColor(getResources().getColor(R.color.success, null));
                break;
        }
        refreshStep4Continue();
    }

    /** Enables Continue only when all fields are valid AND all 3 docs uploaded. */
    private void refreshStep4Continue() {
        boolean allDocsUploaded = isDlUploaded && isOrUploaded && isCrUploaded;
        View banner = findViewById(R.id.layoutAllDocsUploaded);
        if (banner != null)
            banner.setVisibility(allDocsUploaded ? View.VISIBLE : View.GONE);

        boolean canContinue = isLicensePlateValid && isMvFileNumberValid
                && isVehicleMakeValid && isVehicleModelValid
                && allDocsUploaded;
        if (btnStep4Next != null) btnStep4Next.setEnabled(canContinue);
    }

    /** Show the camera-vs-gallery picker for the given document target. */
    private void showDocPicker(DocTarget target) {
        switch (target) {
            case DL: dlPicker.showPickerDialog(); break;
            case OR: orPicker.showPickerDialog(); break;
            case CR: crPicker.showPickerDialog(); break;
        }
    }

    private void setupStep4() {
        // ── License fields (existing) ─────────────────────────────────────
        etDriversLicNo    = findViewById(R.id.etDriversLicNo);
        etDriversExpiry   = findViewById(R.id.etDriversExpiry);
        cbHasConductors   = findViewById(R.id.cbHasConductors);
        layoutConductors  = findViewById(R.id.layoutConductors);
        etConductorsLicNo = findViewById(R.id.etConductorsLicNo);
        etConductorsExpiry= findViewById(R.id.etConductorsExpiry);
        btnStep4Next      = findViewById(R.id.btnStep4Next);

        // ── Vehicle text fields ───────────────────────────────────────────
        etLicensePlate    = findViewById(R.id.etLicensePlate);
        etMvFileNumber    = findViewById(R.id.etMvFileNumber);
        etVehicleMake     = findViewById(R.id.etVehicleMake);
        etVehicleModel    = findViewById(R.id.etVehicleModel);

        // ── Inline error TextViews ────────────────────────────────────────
        tvLicensePlateError  = findViewById(R.id.tvLicensePlateError);
        tvMvFileNumberError  = findViewById(R.id.tvMvFileNumberError);
        tvVehicleMakeError   = findViewById(R.id.tvVehicleMakeError);
        tvVehicleModelError  = findViewById(R.id.tvVehicleModelError);

        // ── Document upload card views ────────────────────────────────────
        imgDlPreview      = findViewById(R.id.imgDlPreview);
        imgOrPreview      = findViewById(R.id.imgOrPreview);
        imgCrPreview      = findViewById(R.id.imgCrPreview);
        tvDlUploadStatus  = findViewById(R.id.tvDlUploadStatus);
        tvOrUploadStatus  = findViewById(R.id.tvOrUploadStatus);
        tvCrUploadStatus  = findViewById(R.id.tvCrUploadStatus);

        // ── Date pickers ──────────────────────────────────────────────────
        DatePickerHelper.attach(this, etDriversExpiry,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.YEAR) + 10);
        DatePickerHelper.attach(this, etConductorsExpiry,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.YEAR) + 10);

        // ── Conductor's license toggle ────────────────────────────────────
        cbHasConductors.setOnCheckedChangeListener((btn, checked) ->
                layoutConductors.setVisibility(checked ? View.VISIBLE : View.GONE));

        // ── MV File Number help tooltip (Toast) ───────────────────────────
        TextView tvMvHelp = findViewById(R.id.tvMvHelp);
        if (tvMvHelp != null) {
            tvMvHelp.setOnClickListener(v ->
                    Toast.makeText(this,
                            "Find your MV File Number on the top-right of your Official Receipt (OR) or Certificate of Registration (CR).\n\nIt is a 15-digit number printed beside the label \"MV File No.\"",
                            Toast.LENGTH_LONG).show());
        }

        // ── Vehicle field real-time validation ────────────────────────────
        etLicensePlate.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    isLicensePlateValid = false;
                    tvLicensePlateError.setVisibility(View.GONE);
                } else if (PLATE_PATTERN.matcher(val).matches()) {
                    isLicensePlateValid = true;
                    tvLicensePlateError.setVisibility(View.GONE);
                } else {
                    isLicensePlateValid = false;
                    tvLicensePlateError.setText("Format: 3 letters, space, 4 digits — e.g. ABC 1234");
                    tvLicensePlateError.setVisibility(View.VISIBLE);
                }
                refreshStep4Continue();
            }
        });

        etMvFileNumber.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    isMvFileNumberValid = false;
                    tvMvFileNumberError.setVisibility(View.GONE);
                } else if (MV_FILE_PATTERN.matcher(val).matches()) {
                    isMvFileNumberValid = true;
                    tvMvFileNumberError.setVisibility(View.GONE);
                } else {
                    isMvFileNumberValid = false;
                    tvMvFileNumberError.setText("Must be exactly 15 digits");
                    tvMvFileNumberError.setVisibility(View.VISIBLE);
                }
                refreshStep4Continue();
            }
        });

        etVehicleMake.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    isVehicleMakeValid = false;
                    tvVehicleMakeError.setVisibility(View.GONE);
                } else if (VEHICLE_MAKE_PATTERN.matcher(val).matches()) {
                    isVehicleMakeValid = true;
                    tvVehicleMakeError.setVisibility(View.GONE);
                } else {
                    isVehicleMakeValid = false;
                    tvVehicleMakeError.setText("Letters only, 2–40 characters — e.g. Toyota");
                    tvVehicleMakeError.setVisibility(View.VISIBLE);
                }
                refreshStep4Continue();
            }
        });

        etVehicleModel.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    isVehicleModelValid = false;
                    tvVehicleModelError.setVisibility(View.GONE);
                } else if (VEHICLE_MODEL_PATTERN.matcher(val).matches()) {
                    isVehicleModelValid = true;
                    tvVehicleModelError.setVisibility(View.GONE);
                } else {
                    isVehicleModelValid = false;
                    tvVehicleModelError.setText("2–60 characters, letters/numbers/spaces — e.g. Vios 1.3 XLE MT");
                    tvVehicleModelError.setVisibility(View.VISIBLE);
                }
                refreshStep4Continue();
            }
        });

        // ── Document upload card click listeners ──────────────────────────
        findViewById(R.id.cardUploadDl).setOnClickListener(v -> showDocPicker(DocTarget.DL));
        findViewById(R.id.cardUploadOr).setOnClickListener(v -> showDocPicker(DocTarget.OR));
        findViewById(R.id.cardUploadCr).setOnClickListener(v -> showDocPicker(DocTarget.CR));

        // ── Continue button ───────────────────────────────────────────────
        btnStep4Next.setEnabled(false);
        btnStep4Next.setOnClickListener(v -> {
            String dlNo     = etDriversLicNo.getText().toString().trim();
            String dlExpiry = etDriversExpiry.getText().toString().trim();

            // — Driver's License —
            if (dlNo.isEmpty()) {
                Toast.makeText(this, "Please enter your Driver's License number.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dlExpiry.isEmpty()) {
                Toast.makeText(this, "Please enter your Driver's License expiry date.", Toast.LENGTH_SHORT).show();
                return;
            }

            // — Conductor's License (optional) —
            if (cbHasConductors.isChecked()) {
                String clNo     = etConductorsLicNo.getText().toString().trim();
                String clExpiry = etConductorsExpiry.getText().toString().trim();
                if (clNo.isEmpty()) {
                    Toast.makeText(this, "Please enter your Conductor's License number.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (clExpiry.isEmpty()) {
                    Toast.makeText(this, "Please enter your Conductor's License expiry date.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // — Vehicle fields (should already be valid, guarded by button enable state) —
            if (!isLicensePlateValid || !isMvFileNumberValid
                    || !isVehicleMakeValid || !isVehicleModelValid) {
                Toast.makeText(this, "Please complete all vehicle fields correctly.", Toast.LENGTH_SHORT).show();
                return;
            }

            // — Documents —
            if (!isDlUploaded || !isOrUploaded || !isCrUploaded) {
                Toast.makeText(this, "Please upload all 3 required documents.", Toast.LENGTH_SHORT).show();
                return;
            }

            showStep(5);
        });

        Button btnStep4Back = findViewById(R.id.btnStep4Back);
        if (btnStep4Back != null) btnStep4Back.setOnClickListener(v -> showStep(3));

        refreshStep4Continue();
    }

    /**
     * Shows the camera-vs-gallery picker for the license photo (Step 5).
     * Delegates to MediaPickerHelper which handles permission checks internally.
     */
    private void showPhotoPicker() {
        licensePicker.showPickerDialog();
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
        btnStep5Next.setEnabled(true);
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

    // onRequestPermissionsResult — delegated to MediaPickerHelper (licensePicker).
    // No override needed here.

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 5 — License Photo + OCR cross-check
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep5() {
        imgLicensePreview        = findViewById(R.id.imgLicensePreview);
        layoutLicensePlaceholder = findViewById(R.id.layoutLicensePlaceholder);
        tvOcrResult              = findViewById(R.id.tvOcrResult);
        tvOcrStatus              = findViewById(R.id.tvOcrStatus);
        btnCaptureLicense        = findViewById(R.id.btnCaptureLicense);
        btnStep5Next             = findViewById(R.id.btnStep5Next);

        // Disabled until a photo is captured and OCR simulation completes.
        btnStep5Next.setEnabled(false);

        btnCaptureLicense.setOnClickListener(v -> showPhotoPicker());

        btnStep5Next.setOnClickListener(v -> {
            if (licenseImageUri == null) {
                Toast.makeText(this, "Please capture or upload a photo of your license.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Object tag = tvOcrStatus.getTag();
            if ("mismatch".equals(tag)) {
                new AlertDialog.Builder(this)
                        .setTitle("License Number Mismatch")
                        .setMessage("The license number in your photo does not match what you entered in Step 4.\n\nPlease retake the photo or go back and correct the number.")
                        .setPositiveButton("Retake Photo", (d, w) -> showPhotoPicker())
                        .setNegativeButton("Fix Number",   (d, w) -> showStep(4))
                        .show();
                return;
            }
            populateSummary();
            showStep(6);
        });

        Button btnStep5Back = findViewById(R.id.btnStep5Back);
        if (btnStep5Back != null) btnStep5Back.setOnClickListener(v -> showStep(4));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 6 — Review & Confirm (summary card)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupStep6() {
        tvSummaryName      = findViewById(R.id.tvSummaryName);
        tvSummaryUsername  = findViewById(R.id.tvSummaryUsername);
        tvSummaryBirthdate = findViewById(R.id.tvSummaryBirthdate);
        tvSummaryGender    = findViewById(R.id.tvSummaryGender);
        tvSummaryEmail     = findViewById(R.id.tvSummaryEmail);
        tvSummaryPhone     = findViewById(R.id.tvSummaryPhone);
        tvSummaryLicense   = findViewById(R.id.tvSummaryLicense);

        Button btnStep6Back = findViewById(R.id.btnStep6Back);
        Button btnStep6Next = findViewById(R.id.btnStep6Next);

        if (btnStep6Back != null) btnStep6Back.setOnClickListener(v -> showStep(5));
        if (btnStep6Next != null) btnStep6Next.setOnClickListener(v -> showStep(8));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STEP 7 — Password + Create Account
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
        if (btnStep8Back != null) btnStep8Back.setOnClickListener(v -> showStep(6));

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

    /** Fills the Review card (Step 6) with values collected in Steps 3 (Personal Info) and 4 (License Details). */
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
            showStep(3);
            return;
        }
        if (db.emailExists(email)) {
            Toast.makeText(this, "Email already registered.", Toast.LENGTH_SHORT).show();
            showStep(3);
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
            showConfirmation(firstName);
        } else {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Transitions to the Step 9 confirmation screen after a successful DB insert.
     * Personalises the headline with the user's first name and wires the
     * "Go to Sign In" button to navigate to {@link LoginActivity}.
     */
    private void showConfirmation(String firstName) {
        showStep(9);

        TextView tvHeadline = findViewById(R.id.tvSuccessHeadline);
        TextView tvBody     = findViewById(R.id.tvSuccessBody);
        Button   btnLogin   = findViewById(R.id.btnGoToLogin);

        tvHeadline.setText("Welcome, " + firstName + "!");
        tvBody.setText("Your registration details have been successfully submitted. "
                + "You can now sign in with your email and password.");

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

}