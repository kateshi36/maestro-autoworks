package com.maestro.autoworks.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.utils.DatePickerHelper;
import com.maestro.autoworks.utils.MediaPickerHelper;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BookActivity — Book an appointment.
 * Demonstrates: Spinner & ArrayAdapter, RadioButton & BG Color,
 *               CheckBox & Text Color, RatingBar, AlertDialog, Toast.
 *
 * New fields added (Step 2 flow):
 *   • spinnerCarModel  — car brand/model selection
 *   • spinnerYearModel — year model selection
 *   • rgFuelType       — Gasoline / Diesel radio group
 *
 * OR/CR Camera Flow:
 *   • rgOrCr           — Yes / No radio group
 *   • layoutOrCrCapture— revealed when "Yes" is selected
 *   • btnCaptureOrCr   — launches camera (or gallery fallback)
 *   • imgOrCrPreview   — shows the captured photo thumbnail
 *   • orcrImageUri     — URI of captured image sent to admin
 */
public class BookActivity extends AppCompatActivity {

    // ── Step 2: Car Details ──
    private Spinner    spinnerCarModel;
    private Spinner    spinnerYearModel;
    private RadioGroup rgFuelType;

    // ── OR/CR Verification ──
    private RadioGroup   rgOrCr;
    private LinearLayout layoutOrCrCapture;
    private LinearLayout layoutOrCrPlaceholder;
    private ImageView    imgOrCrPreview;
    private Button       btnCaptureOrCr;

    /** URI of the photo file created for the camera intent (ACTION_IMAGE_CAPTURE). */
    private Uri    orcrImageUri  = null;
    /** Absolute path of the same file, stored in the appointment record. */
    private String orcrImagePath = null;

    /**
     * Centralised media picker (Task 1-3).
     * Handles FileProvider URI creation, runtime permission requests, and the
     * camera-vs-gallery chooser dialog.  Must be instantiated before onCreate
     * returns (field initialiser or early onCreate) so its ActivityResultLaunchers
     * are registered in time.
     */
    private final MediaPickerHelper orcrPicker = new MediaPickerHelper(
            this,
            "ORCR",           // filename prefix → e.g. ORCR_20250515_143022.jpg
            (uri, path) -> {  // ImagePickedCallback — runs on the main thread
                orcrImageUri  = uri;
                orcrImagePath = path;
                showOrCrPreview(uri);
                Toast.makeText(this,
                        "OR/CR photo saved. Admin will verify before confirming.",
                        Toast.LENGTH_LONG).show();
            }
    );

    // ── Existing fields ──
    private Spinner      spinnerService;
    private EditText     etCarPlate, etDate;
    private Button       btnPickDate;
    private TextView     tvSelectedDate;
    private RadioGroup   rgTimeSlot;
    private LinearLayout layoutTimeSlot;
    private CheckBox     cbOilCheck, cbCarWash, cbInspection;
    private RatingBar    ratingBar;
    private TextView     tvTotal, tvRatingLabel;

    private List<Service> services;
    private double basePrice = 0;

    // Add-on prices
    private static final double PRICE_OIL_CHECK  = 0;
    private static final double PRICE_CAR_WASH   = 150;
    private static final double PRICE_INSPECTION = 250;

    // ── Car model options ──
    private static final String[] CAR_MODELS = {
        "— Select car model —",
        "Toyota", "Honda", "Mitsubishi", "Nissan", "Ford",
        "Hyundai", "Kia", "Suzuki", "Isuzu", "Mazda",
        "BMW", "Mercedes-Benz", "Chevrolet", "Subaru", "Other"
    };

    // ── Year model options (2000 – 2025) ──
    private static final String[] YEAR_MODELS;
    static {
        int currentYear = 2025;
        int startYear   = 2000;
        int count       = currentYear - startYear + 2;
        YEAR_MODELS = new String[count];
        YEAR_MODELS[0] = "— Select year model —";
        for (int i = 1; i < count; i++) {
            YEAR_MODELS[i] = String.valueOf(currentYear - i + 1);
        }
    }

    // ─────────────────────────────────────────────
    //  NOTE: Camera / gallery / permission launchers are now encapsulated in
    //        the `orcrPicker` MediaPickerHelper field declared above.
    //        See MediaPickerHelper.java for the full implementation.
    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    //  onCreate
    // ─────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        services = ServiceData.getAll();

        // ── Bind Step 2 car-detail views ──
        spinnerCarModel  = findViewById(R.id.spinnerCarModel);
        spinnerYearModel = findViewById(R.id.spinnerYearModel);
        rgFuelType       = findViewById(R.id.rgFuelType);

        // ── Bind OR/CR views ──
        rgOrCr                = findViewById(R.id.rgOrCr);
        layoutOrCrCapture     = findViewById(R.id.layoutOrCrCapture);
        layoutOrCrPlaceholder = findViewById(R.id.layoutOrCrPlaceholder);
        imgOrCrPreview        = findViewById(R.id.imgOrCrPreview);
        btnCaptureOrCr        = findViewById(R.id.btnCaptureOrCr);

        // ── Bind existing views ──
        spinnerService  = findViewById(R.id.spinnerService);
        etCarPlate      = findViewById(R.id.etCarPlate);
        etDate          = findViewById(R.id.etDate);
        btnPickDate     = findViewById(R.id.btnPickDate);
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        rgTimeSlot      = findViewById(R.id.rgTimeSlot);
        layoutTimeSlot  = findViewById(R.id.layoutTimeSlot);
        cbOilCheck      = findViewById(R.id.cbOilCheck);
        cbCarWash       = findViewById(R.id.cbCarWash);
        cbInspection    = findViewById(R.id.cbInspection);
        ratingBar       = findViewById(R.id.ratingBar);
        tvTotal         = findViewById(R.id.tvTotal);
        tvRatingLabel   = findViewById(R.id.tvRatingLabel);
        Button btnBookNow = findViewById(R.id.btnBookNow);

        setupCarModelSpinner();
        setupYearModelSpinner();
        setupServiceSpinner();
        setupCalendarPicker();
        setupTimeSlotRadio();
        setupCheckBoxes();
        setupRatingBar();
        setupOrCrSection();

        btnBookNow.setOnClickListener(v -> attemptBooking());
    }

    // ─────────────────────────────────────────────
    //  Setup helpers
    // ─────────────────────────────────────────────

    private void setupCarModelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CAR_MODELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCarModel.setAdapter(adapter);
    }

    private void setupYearModelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, YEAR_MODELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearModel.setAdapter(adapter);
    }

    /**
     * Wires up the date picker button.
     * Opens a DatePickerDialog pre-set to today; past dates are disabled.
     * On confirm: formats the date for display and stores YYYY-MM-DD in etDate.
     */
    private void setupCalendarPicker() {
        // Both the row and the button open the picker
        findViewById(R.id.layoutSelectedDate).setOnClickListener(v -> openDatePicker());
        btnPickDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        // Dismiss keyboard and open the universal date picker.
        // On selection DatePickerHelper writes YYYY-MM-DD to etDate;
        // we then update the human-readable tvSelectedDate label.
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);

        android.app.DatePickerDialog.OnDateSetListener onDateSet = (view, y, m, d) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(y, m, d);
            java.text.SimpleDateFormat displayFmt =
                new java.text.SimpleDateFormat("EEEE, MMMM d yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat storageFmt =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            tvSelectedDate.setText(displayFmt.format(selected.getTime()));
            tvSelectedDate.setTextColor(getColor(R.color.yellow));
            etDate.setText(storageFmt.format(selected.getTime()));
        };

        // Build dialog via DatePickerHelper then override the listener so we can
        // also populate tvSelectedDate with the friendly display format.
        Calendar today = Calendar.getInstance();
        int year  = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day   = today.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(
            this, onDateSet, year, month, day);

        // Allow today up to one year in the future — no past bookings
        dialog.getDatePicker().setMinDate(today.getTimeInMillis());
        Calendar maxCal = Calendar.getInstance();
        maxCal.set(todayYear + 1, Calendar.DECEMBER, 31);
        dialog.getDatePicker().setMaxDate(maxCal.getTimeInMillis());

        // Hide soft keyboard before showing dialog
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager)
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etDate.getWindowToken(), 0);

        dialog.show();
    }

    private void setupServiceSpinner() {
        String[] serviceNames = new String[services.size() + 1];
        serviceNames[0] = "— Select a service —";
        for (int i = 0; i < services.size(); i++) {
            serviceNames[i + 1] = services.get(i).name + "  ₱" +
                    String.format("%.2f", services.get(i).price);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, serviceNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(spinnerAdapter);

        String preSelected = getIntent().getStringExtra("service_name");
        if (preSelected != null) {
            for (int i = 0; i < services.size(); i++) {
                if (services.get(i).name.equals(preSelected)) {
                    spinnerService.setSelection(i + 1);
                    break;
                }
            }
        }

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                basePrice = (position > 0) ? services.get(position - 1).price : 0;
                updateTotal();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTimeSlotRadio() {
        rgTimeSlot.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMorning) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A2A1A"));
                Toast.makeText(this, "Morning slot — background: dark green", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbAfternoon) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A1A2A"));
                Toast.makeText(this, "Afternoon slot — background: dark blue", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbEarlyBird) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#2A1A00"));
                Toast.makeText(this, "Early Bird slot — background: dark amber", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCheckBoxes() {
        cbOilCheck.setOnCheckedChangeListener((btn, checked) -> {
            cbOilCheck.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateTotal();
        });
        cbCarWash.setOnCheckedChangeListener((btn, checked) -> {
            cbCarWash.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateTotal();
        });
        cbInspection.setOnCheckedChangeListener((btn, checked) -> {
            cbInspection.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateTotal();
        });
    }

    private void setupRatingBar() {
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            String[] labels = {"", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🌟"};
            int idx = (int) rating;
            tvRatingLabel.setText(idx > 0 ? labels[idx] : "Tap a star to rate");
            tvRatingLabel.setTextColor(idx >= 4
                    ? getColor(R.color.success) : getColor(R.color.muted));
        });
    }

    // ─────────────────────────────────────────────
    //  OR/CR Section
    // ─────────────────────────────────────────────

    /**
     * Sets up the OR/CR radio group and the camera capture button.
     *
     * Flow:
     *  1. User selects "Yes" → layoutOrCrCapture slides in (VISIBLE).
     *  2. User taps btnCaptureOrCr → camera opens (or gallery if no camera).
     *  3. Photo returned → thumbnail shown in imgOrCrPreview.
     *  4. orcrImagePath stored on the Appointment for admin review.
     *
     * If the user selects "No" → capture area is hidden and any previously
     * captured image is cleared. Admin is notified via the orcrStatus field.
     */
    private void setupOrCrSection() {
        rgOrCr.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOrCrYes) {
                layoutOrCrCapture.setVisibility(View.VISIBLE);
            } else {
                layoutOrCrCapture.setVisibility(View.GONE);
                clearOrCrPhoto();
            }
        });

        // Task 2 & 3: single call to MediaPickerHelper — shows the
        // camera-vs-gallery dialog and handles permissions internally.
        btnCaptureOrCr.setOnClickListener(v -> orcrPicker.showPickerDialog());
    }


    /**
     * Decodes the captured photo into a memory-safe thumbnail and
     * displays it in imgOrCrPreview. Uses inSampleSize to avoid OOM
     * on high-megapixel camera outputs.
     */
    private void showOrCrPreview(Uri uri) {
        try {
            // Pass 1: measure dimensions without allocating pixels
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is1, null, opts);
            if (is1 != null) is1.close();

            // Calculate sample size to keep the bitmap within ~800px on longest side
            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > 800 || (opts.outHeight / sampleSize) > 800) {
                sampleSize *= 2;
            }

            // Pass 2: decode the scaled bitmap
            BitmapFactory.Options opts2 = new BitmapFactory.Options();
            opts2.inSampleSize = sampleSize;
            InputStream is2 = getContentResolver().openInputStream(uri);
            Bitmap thumbnail = BitmapFactory.decodeStream(is2, null, opts2);
            if (is2 != null) is2.close();

            if (thumbnail != null) {
                imgOrCrPreview.setImageBitmap(thumbnail);
                imgOrCrPreview.setVisibility(View.VISIBLE);
                layoutOrCrPlaceholder.setVisibility(View.GONE);
                btnCaptureOrCr.setText("📷  RETAKE OR/CR PHOTO");
            }

        } catch (IOException e) {
            Toast.makeText(this, "Could not load photo preview.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Clears any captured OR/CR photo and resets the capture UI to its initial state. */
    private void clearOrCrPhoto() {
        orcrImageUri  = null;
        orcrImagePath = null;
        imgOrCrPreview.setImageBitmap(null);
        imgOrCrPreview.setVisibility(View.GONE);
        layoutOrCrPlaceholder.setVisibility(View.VISIBLE);
        btnCaptureOrCr.setText("📷  CAPTURE OR/CR PHOTO");
    }

    // ─────────────────────────────────────────────
    //  Runtime Permission Result
    // ─────────────────────────────────────────────
    //  Delegated to MediaPickerHelper via ActivityResultLauncher.
    //  No onRequestPermissionsResult override needed.
    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    //  Booking logic
    // ─────────────────────────────────────────────

    private void attemptBooking() {
        // Validate car details
        int carModelPos  = spinnerCarModel.getSelectedItemPosition();
        int yearModelPos = spinnerYearModel.getSelectedItemPosition();
        int fuelTypeId   = rgFuelType.getCheckedRadioButtonId();

        if (carModelPos == 0) {
            Toast.makeText(this, "Please select a car model", Toast.LENGTH_SHORT).show();
            return;
        }
        if (yearModelPos == 0) {
            Toast.makeText(this, "Please select a year model", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fuelTypeId == -1) {
            Toast.makeText(this, "Please select a fuel type (Gasoline or Diesel)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate OR/CR answer
        int orCrId = rgOrCr.getCheckedRadioButtonId();
        if (orCrId == -1) {
            Toast.makeText(this, "Please indicate if you have your OR/CR",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // If "Yes" but no photo yet, prompt the user
        if (orCrId == R.id.rbOrCrYes && orcrImageUri == null) {
            new AlertDialog.Builder(this)
                .setTitle("OR/CR Photo Required")
                .setMessage("You indicated you have your OR/CR. " +
                        "Please capture a photo so admin can verify your car details.\n\n" +
                        "Tap \"Take Photo\" to proceed, or tap \"Skip\" to continue without a photo.")
                .setPositiveButton("Take Photo", (d, w) -> orcrPicker.showPickerDialog())
                .setNegativeButton("Skip", (d, w) -> proceedWithBookingValidation())
                .show();
            return;
        }

        proceedWithBookingValidation();
    }

    /** Validates remaining form fields and shows the confirmation AlertDialog. */
    private void proceedWithBookingValidation() {
        String plate   = etCarPlate.getText().toString().trim();
        String date    = etDate.getText().toString().trim();
        int    spinPos = spinnerService.getSelectedItemPosition();

        if (spinPos == 0) {
            Toast.makeText(this, "Please select a service", Toast.LENGTH_SHORT).show();
            return;
        }
        if (plate.isEmpty()) {
            Toast.makeText(this, "Please enter your car plate number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Please enter a preferred date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgTimeSlot.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String carModel    = CAR_MODELS[spinnerCarModel.getSelectedItemPosition()];
        String yearModel   = YEAR_MODELS[spinnerYearModel.getSelectedItemPosition()];
        String fuelType    = getSelectedFuelType();
        String orCrStatus  = getOrCrStatus();
        String serviceName = services.get(spinPos - 1).name;
        String timeSlot    = getSelectedTime();
        double total       = calculateTotal();

        String orCrLine = "OR/CR:     " + orCrStatus;
        if (orcrImageUri != null) orCrLine += " (photo attached)";

        new AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage(
                "Car Model: " + carModel    + "\n" +
                "Year:      " + yearModel   + "\n" +
                "Fuel:      " + fuelType    + "\n" +
                orCrLine                    + "\n" +
                "Service:   " + serviceName + "\n" +
                "Plate:     " + plate       + "\n" +
                "Date:      " + date        + "\n" +
                "Time:      " + timeSlot    + "\n" +
                "Rating:    " + (int) ratingBar.getRating() + "★\n\n" +
                "Total:     ₱" + String.format("%.2f", total)
            )
            .setPositiveButton("Book Now", (dialog, which) ->
                saveAppointment(carModel, yearModel, fuelType, orCrStatus,
                                serviceName, plate, date, timeSlot, total))
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private String getSelectedFuelType() {
        int id = rgFuelType.getCheckedRadioButtonId();
        if (id == R.id.rbGasoline) return "Gasoline";
        if (id == R.id.rbDiesel)   return "Diesel";
        return "";
    }

    private String getOrCrStatus() {
        int id = rgOrCr.getCheckedRadioButtonId();
        if (id == R.id.rbOrCrYes) return orcrImageUri != null ? "Yes (photo captured)" : "Yes (no photo)";
        if (id == R.id.rbOrCrNo)  return "No";
        return "Not answered";
    }

    private String getSelectedTime() {
        int id = rgTimeSlot.getCheckedRadioButtonId();
        if (id == R.id.rbMorning)    return "Morning (8AM–12PM)";
        if (id == R.id.rbAfternoon)  return "Afternoon (1PM–5PM)";
        if (id == R.id.rbEarlyBird)  return "Early Bird (8AM–9AM)";
        return "";
    }

    private double calculateTotal() {
        double total = basePrice;
        if (cbOilCheck.isChecked())   total += PRICE_OIL_CHECK;
        if (cbCarWash.isChecked())    total += PRICE_CAR_WASH;
        if (cbInspection.isChecked()) total += PRICE_INSPECTION;
        return total;
    }

    private void updateTotal() {
        tvTotal.setText("Estimated Total:  ₱" + String.format("%.2f", calculateTotal()));
    }

    /**
     * Persists the appointment to the local SQLite database.
     *
     * New fields written:
     *   carModel      — brand selected from spinnerCarModel
     *   yearModel     — year selected from spinnerYearModel
     *   fuelType      — "Gasoline" or "Diesel"
     *   orcrStatus    — human-readable OR/CR answer
     *   orcrImagePath — content URI string of the captured photo (or null)
     *
     * The admin-side activity reads orcrImagePath to display the OR/CR
     * photo and mark the appointment as verified or rejected.
     */
    private void saveAppointment(String carModel, String yearModel, String fuelType,
                                  String orCrStatus, String serviceName,
                                  String plate, String date, String time, double total) {
        SessionManager session = new SessionManager(this);
        DatabaseHelper  db     = new DatabaseHelper(this);

        Appointment appt   = new Appointment();
        appt.userId        = session.getUserId();
        appt.carModel      = carModel;
        appt.yearModel     = yearModel;
        appt.fuelType      = fuelType;
        appt.orcrStatus    = orCrStatus;
        appt.orcrImagePath = orcrImagePath; // null if not captured — admin will follow up
        appt.serviceName   = serviceName;
        appt.carPlate      = plate;
        appt.date          = date;
        appt.time          = time;
        appt.totalPrice    = total;
        appt.status        = "pending";
        appt.rating        = (int) ratingBar.getRating();

        long id = db.insertAppointment(appt);
        if (id > 0) {
            Toast.makeText(this,
                    "Appointment booked! We'll confirm within 24 hrs.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, AppointmentsActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Booking failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
