package com.maestro.autoworks.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.Service;

import java.util.Calendar;
import java.util.List;

/**
 * BookActivity — Book an appointment.
 * Demonstrates: Spinner & ArrayAdapter, RadioButton & BG Color,
 *               CheckBox & Text Color, RatingBar, AlertDialog, Toast.
 */
public class BookActivity extends AppCompatActivity {

    // ── Step 2: Car Details ──
    private Spinner    spinnerCarModel;
    private Spinner    spinnerYearModel;
    private RadioGroup rgFuelType;

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

    // ── Step 2: Vehicle Inspection Checklist ──
    private CheckBox     cbConcernEngine, cbConcernBrakes, cbConcernAircon,
            cbConcernElectrical, cbConcernTires, cbConcernOil,
            cbConcernSteering, cbConcernExhaust;
    private EditText     etAdditionalNotes;
    private LinearLayout layoutConcernSummary;
    private TextView     tvConcernSummaryText;

    private List<Service> services;
    private double basePrice = 0;

    // Add-on prices
    private static final double PRICE_OIL_CHECK  = 0;
    private static final double PRICE_CAR_WASH   = 150;
    private static final double PRICE_INSPECTION = 250;

    /**
     * Matches current (2014+) Philippine license plates: 3 letters, space, 4 digits.
     * e.g. "ABC 1234"
     * Also accepts pre-2014 format: 2 letters, space, 4 digits. e.g. "AB 1234"
     * Same pattern used in RegisterActivity.
     */
    private static final java.util.regex.Pattern PLATE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z]{2,3}\\s[0-9]{4}$");

    private TextView tvCarPlateError;
    private boolean  isCarPlateValid = false;

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

        // ── Bind Step 2 inspection checklist views ──
        cbConcernEngine     = findViewById(R.id.cbConcernEngine);
        cbConcernBrakes     = findViewById(R.id.cbConcernBrakes);
        cbConcernAircon     = findViewById(R.id.cbConcernAircon);
        cbConcernElectrical = findViewById(R.id.cbConcernElectrical);
        cbConcernTires      = findViewById(R.id.cbConcernTires);
        cbConcernOil        = findViewById(R.id.cbConcernOil);
        cbConcernSteering   = findViewById(R.id.cbConcernSteering);
        cbConcernExhaust    = findViewById(R.id.cbConcernExhaust);
        etAdditionalNotes   = findViewById(R.id.etAdditionalNotes);
        layoutConcernSummary = findViewById(R.id.layoutConcernSummary);
        tvConcernSummaryText = findViewById(R.id.tvConcernSummaryText);
        tvCarPlateError      = findViewById(R.id.tvCarPlateError);

        setupCarModelSpinner();
        setupYearModelSpinner();
        setupServiceSpinner();
        setupCalendarPicker();
        setupTimeSlotRadio();
        setupCheckBoxes();
        setupInspectionChecklist();
        setupRatingBar();
        setupCarPlateValidation();

        // ── Pre-fill car plate from the user's registration data ──
        preFillCarPlate();

        btnBookNow.setOnClickListener(v -> attemptBooking());
    }

    // ─────────────────────────────────────────────
    //  Pre-fill helpers
    // ─────────────────────────────────────────────

    /**
     * Pre-fills Car Plate Number from the session.
     * The plate is saved to the session at login time (from the user's DB profile),
     * so no extra DB query is needed here.
     */
    private void preFillCarPlate() {
        SessionManager session = new SessionManager(this);
        String plate = session.getLicensePlate();
        if (plate != null && !plate.isEmpty()) {
            etCarPlate.setText(plate);
            etCarPlate.setSelection(plate.length());
        }
    }

    /** Updates the saved plate whenever the user completes a booking. */
    private void savePlateToPrefs(int userId, String plate) {
        new SessionManager(this).saveLicensePlate(plate);
    }

    /**
     * Attaches a real-time TextWatcher to etCarPlate that mirrors the same
     * Philippine plate validation used in RegisterActivity.
     *
     * Valid formats:
     *   - 3 letters + space + 4 digits  (2014+ plates)  e.g. "ABC 1234"
     *   - 2 letters + space + 4 digits  (pre-2014)      e.g. "AB 1234"
     *
     * Input is auto-uppercased so the user doesn't have to worry about case.
     */
    private void setupCarPlateValidation() {
        etCarPlate.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                // Auto-uppercase as the user types
                String current = s.toString();
                String upper   = current.toUpperCase();
                if (!current.equals(upper)) {
                    etCarPlate.removeTextChangedListener(this);
                    etCarPlate.setText(upper);
                    etCarPlate.setSelection(upper.length());
                    etCarPlate.addTextChangedListener(this);
                    return;
                }

                String val = upper.trim();
                if (val.isEmpty()) {
                    isCarPlateValid = false;
                    tvCarPlateError.setVisibility(View.GONE);
                } else if (PLATE_PATTERN.matcher(val).matches()) {
                    isCarPlateValid = true;
                    tvCarPlateError.setVisibility(View.GONE);
                } else {
                    isCarPlateValid = false;
                    tvCarPlateError.setText("Format: 3 letters, space, 4 digits — e.g. ABC 1234");
                    tvCarPlateError.setVisibility(View.VISIBLE);
                }
            }
        });
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

    private void setupCalendarPicker() {
        findViewById(R.id.layoutSelectedDate).setOnClickListener(v -> openDatePicker());
        btnPickDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
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

        Calendar today = Calendar.getInstance();
        int year  = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);
        int day   = today.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(
                this, onDateSet, year, month, day);

        dialog.getDatePicker().setMinDate(today.getTimeInMillis());
        Calendar maxCal = Calendar.getInstance();
        maxCal.set(todayYear + 1, Calendar.DECEMBER, 31);
        dialog.getDatePicker().setMaxDate(maxCal.getTimeInMillis());

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

    // ─────────────────────────────────────────────
    //  Step 2: Vehicle Inspection Checklist
    // ─────────────────────────────────────────────

    private void setupInspectionChecklist() {
        CheckBox[] concerns = {
                cbConcernEngine, cbConcernBrakes, cbConcernAircon, cbConcernElectrical,
                cbConcernTires, cbConcernOil, cbConcernSteering, cbConcernExhaust
        };
        android.widget.CompoundButton.OnCheckedChangeListener listener = (btn, checked) -> {
            btn.setTextColor(checked ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateConcernSummary(concerns);
        };
        for (CheckBox cb : concerns) cb.setOnCheckedChangeListener(listener);

        etAdditionalNotes.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updateConcernSummary(concerns);
            }
        });
    }

    private void updateConcernSummary(CheckBox[] concerns) {
        java.util.List<String> selected = new java.util.ArrayList<>();
        for (CheckBox cb : concerns) {
            if (cb.isChecked()) selected.add(cb.getText().toString());
        }
        String notes = etAdditionalNotes.getText().toString().trim();
        boolean hasContent = !selected.isEmpty() || !notes.isEmpty();
        layoutConcernSummary.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        if (hasContent) {
            StringBuilder sb = new StringBuilder();
            if (!selected.isEmpty()) {
                sb.append("Selected concerns:\n");
                for (String s : selected) sb.append("  • ").append(s).append("\n");
            }
            if (!notes.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Additional notes:\n  ").append(notes);
            }
            tvConcernSummaryText.setText(sb.toString().trim());
        }
    }

    private String getSelectedConcerns() {
        CheckBox[] concerns = {
                cbConcernEngine, cbConcernBrakes, cbConcernAircon, cbConcernElectrical,
                cbConcernTires, cbConcernOil, cbConcernSteering, cbConcernExhaust
        };
        java.util.List<String> selected = new java.util.ArrayList<>();
        for (CheckBox cb : concerns) {
            if (cb.isChecked()) selected.add(cb.getText().toString());
        }
        return android.text.TextUtils.join(", ", selected);
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
    //  Booking logic
    // ─────────────────────────────────────────────

    private void attemptBooking() {
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

        proceedWithBookingValidation();
    }

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
            etCarPlate.requestFocus();
            return;
        }
        if (!PLATE_PATTERN.matcher(plate.toUpperCase()).matches()) {
            tvCarPlateError.setText("Format: 3 letters, space, 4 digits — e.g. ABC 1234");
            tvCarPlateError.setVisibility(View.VISIBLE);
            etCarPlate.requestFocus();
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
        String concerns    = getSelectedConcerns();
        String notes       = etAdditionalNotes.getText().toString().trim();
        String serviceName = services.get(spinPos - 1).name;
        String timeSlot    = getSelectedTime();
        double total       = calculateTotal();

        String concernLine = concerns.isEmpty() ? "None" : concerns;
        String notesLine   = notes.isEmpty()    ? "None" : notes;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(
                        "Car Model: " + carModel    + "\n" +
                                "Year:      " + yearModel   + "\n" +
                                "Fuel:      " + fuelType    + "\n" +
                                "Concerns:  " + concernLine + "\n" +
                                "Notes:     " + notesLine   + "\n" +
                                "Service:   " + serviceName + "\n" +
                                "Plate:     " + plate       + "\n" +
                                "Date:      " + date        + "\n" +
                                "Time:      " + timeSlot    + "\n" +
                                "Rating:    " + (int) ratingBar.getRating() + "★\n\n" +
                                "Total:     ₱" + String.format("%.2f", total)
                )
                .setPositiveButton("Book Now", (dialog, which) ->
                        saveAppointment(carModel, yearModel, fuelType,
                                concerns, notes,
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

    // ─────────────────────────────────────────────
    //  Step 4: Booking Confirmation Dialog
    // ─────────────────────────────────────────────

    private void showBookingSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Booking Submitted")
                .setMessage(
                        "Your booking request has been successfully submitted.\n\n" +
                                "Please wait for the administrator's confirmation."
                )
                .setCancelable(false)
                .setPositiveButton("Book Again", (dialog, which) -> {
                    // Restart BookActivity so the user can submit another request
                    startActivity(new Intent(this, BookActivity.class));
                    finish();
                })
                .setNegativeButton("View My Appointments", (dialog, which) -> {
                    startActivity(new Intent(this, AppointmentsActivity.class));
                    finish();
                })
                .show();
    }

    private void saveAppointment(String carModel, String yearModel, String fuelType,
                                 String concerns, String notes, String serviceName,
                                 String plate, String date, String time, double total) {
        SessionManager session = new SessionManager(this);
        DatabaseHelper  db     = new DatabaseHelper(this);

        Appointment appt   = new Appointment();
        appt.userId        = session.getUserId();
        appt.carModel      = carModel;
        appt.yearModel     = yearModel;
        appt.fuelType      = fuelType;
        appt.orcrStatus    = "N/A";
        appt.orcrImagePath = null;
        appt.vehicleConcerns = concerns;
        appt.additionalNotes = notes;
        appt.serviceName   = serviceName;
        appt.carPlate      = plate;
        appt.date          = date;
        appt.time          = time;
        appt.totalPrice    = total;
        appt.status        = "pending";
        appt.rating        = (int) ratingBar.getRating();

        // Stage 3: populate customerName from session so it appears in the
        // admin notification title and body.  The field is already declared in
        // the Appointment model but was never set here before the DB insert.
        String fullName = session.getFullName();
        appt.customerName = (fullName != null && !fullName.trim().isEmpty())
                ? fullName.trim() : "Customer #" + appt.userId;

        long id = db.insertAppointment(appt);
        if (id > 0) {
            appt.id = (int) id;   // populate ID so notification can reference it

            // ── Notify the admin of the new pending booking ──────────────────
            // 1. In-app notification → admin dashboard badge
            db.insertAdminNotification(appt.id,
                    "New Booking — " + appt.customerName + " (#" + appt.id + ")",
                    "A new appointment request is waiting for your review.\n\n"
                            + "Customer: " + appt.customerName + "\n"
                            + "Service : " + (appt.serviceName != null ? appt.serviceName : "N/A") + "\n"
                            + "Date    : " + (appt.date        != null ? appt.date        : "N/A") + "\n"
                            + "Time    : " + (appt.time        != null ? appt.time        : "N/A") + "\n"
                            + "Plate   : " + (appt.carPlate    != null ? appt.carPlate    : "N/A") + "\n"
                            + String.format("Total   : \u20b1%.2f", appt.totalPrice));

            // 2. Android push → admin device (heads-up)
            com.maestro.autoworks.utils.NotificationHelper.postNewBookingToAdmin(this, appt);
            // ─────────────────────────────────────────────────────────────────

            // Remember the plate for next time (helps existing accounts with null DB plate)
            savePlateToPrefs(appt.userId, plate);
            showBookingSuccessDialog();
        } else {
            Toast.makeText(this, "Booking failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}