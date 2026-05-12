package com.maestro.autoworks.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.Service;

import java.util.List;

/**
 * BookActivity — Book an appointment.
 * Demonstrates: Spinner & ArrayAdapter, RadioButton & BG Color,
 *               CheckBox & Text Color, RatingBar, AlertDialog, Toast.
 */
public class BookActivity extends AppCompatActivity {

    private Spinner spinnerService;
    private EditText etCarPlate, etDate;
    private RadioGroup rgTimeSlot;
    private LinearLayout layoutTimeSlot;
    private CheckBox cbOilCheck, cbCarWash, cbInspection;
    private RatingBar ratingBar;
    private TextView tvTotal, tvRatingLabel;

    private List<Service> services;
    private double basePrice = 0;

    // Add-on prices
    private static final double PRICE_OIL_CHECK  = 0;
    private static final double PRICE_CAR_WASH   = 150;
    private static final double PRICE_INSPECTION = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        services = ServiceData.getAll();

        spinnerService  = findViewById(R.id.spinnerService);
        etCarPlate      = findViewById(R.id.etCarPlate);
        etDate          = findViewById(R.id.etDate);
        rgTimeSlot      = findViewById(R.id.rgTimeSlot);
        layoutTimeSlot  = findViewById(R.id.layoutTimeSlot);
        cbOilCheck      = findViewById(R.id.cbOilCheck);
        cbCarWash       = findViewById(R.id.cbCarWash);
        cbInspection    = findViewById(R.id.cbInspection);
        ratingBar       = findViewById(R.id.ratingBar);
        tvTotal         = findViewById(R.id.tvTotal);
        tvRatingLabel   = findViewById(R.id.tvRatingLabel);
        Button btnBookNow = findViewById(R.id.btnBookNow);

        // ── SPINNER: bind services with ArrayAdapter ──
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

        // Pre-select service if passed via Intent
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
            public void onItemSelected(AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                if (position > 0) {
                    basePrice = services.get(position - 1).price;
                } else {
                    basePrice = 0;
                }
                updateTotal();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── RADIO BUTTONS: change background color ──
        rgTimeSlot.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMorning) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A2A1A")); // dark green
                Toast.makeText(this, "Morning slot — background: dark green", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbAfternoon) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A1A2A")); // dark blue
                Toast.makeText(this, "Afternoon slot — background: dark blue", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbEarlyBird) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#2A1A00")); // dark amber
                Toast.makeText(this, "Early Bird slot — background: dark amber", Toast.LENGTH_SHORT).show();
            }
        });

        // ── CHECKBOXES: change text color when checked ──
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

        // ── RATINGBAR: label the rating ──
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            String[] labels = {"", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🌟"};
            int idx = (int) rating;
            tvRatingLabel.setText(idx > 0 ? labels[idx] : "Tap a star to rate");
            tvRatingLabel.setTextColor(idx >= 4
                    ? getColor(R.color.success) : getColor(R.color.muted));
        });

        // ── BUTTON → ALERTDIALOG: confirm booking ──
        btnBookNow.setOnClickListener(v -> {
            String plate = etCarPlate.getText().toString().trim();
            String date  = etDate.getText().toString().trim();
            int spinPos  = spinnerService.getSelectedItemPosition();

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

            String serviceName = services.get(spinPos - 1).name;
            String timeSlot    = getSelectedTime();
            double total       = calculateTotal();

            // ── ALERTDIALOG: booking summary ──
            new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(
                    "Service: " + serviceName + "\n" +
                    "Plate:   " + plate + "\n" +
                    "Date:    " + date + "\n" +
                    "Time:    " + timeSlot + "\n" +
                    "Rating:  " + (int) ratingBar.getRating() + "★\n\n" +
                    "Total:   ₱" + String.format("%.2f", total)
                )
                .setPositiveButton("Book Now", (dialog, which) -> {
                    saveAppointment(serviceName, plate, date, timeSlot, total);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
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
        if (cbOilCheck.isChecked())  total += PRICE_OIL_CHECK;
        if (cbCarWash.isChecked())   total += PRICE_CAR_WASH;
        if (cbInspection.isChecked()) total += PRICE_INSPECTION;
        return total;
    }

    private void updateTotal() {
        tvTotal.setText("Estimated Total:  ₱" + String.format("%.2f", calculateTotal()));
    }

    private void saveAppointment(String serviceName, String plate,
                                  String date, String time, double total) {
        SessionManager session = new SessionManager(this);
        DatabaseHelper db = new DatabaseHelper(this);

        Appointment appt = new Appointment();
        appt.userId      = session.getUserId();
        appt.serviceName = serviceName;
        appt.carPlate    = plate;
        appt.date        = date;
        appt.time        = time;
        appt.totalPrice  = total;
        appt.status      = "pending";
        appt.rating      = (int) ratingBar.getRating();

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
