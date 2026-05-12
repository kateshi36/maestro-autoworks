package com.maestro.autoworks.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AdminAppointmentAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;

import java.util.List;

/**
 * AdminAppointmentsActivity — mirrors admin_appointments.php.
 * Filter by status via a Spinner; list all appointments with confirm/decline actions.
 */
public class AdminAppointmentsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView lvAppointments;
    private TextView tvCount, tvEmpty;
    private Spinner spinnerStatus;
    private String[] statuses = {"all", "pending", "confirmed", "completed", "declined", "cancelled"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointments);

        db = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        if (!session.isAdmin()) { finish(); return; }

        lvAppointments = findViewById(R.id.lvAdminAppointments);
        tvCount        = findViewById(R.id.tvApptCount);
        tvEmpty        = findViewById(R.id.tvApptEmpty);
        spinnerStatus  = findViewById(R.id.spinnerStatus);
        Button btnFilter = findViewById(R.id.btnFilter);
        Button btnBack   = findViewById(R.id.btnAdminApptBack);

        // Status spinner
        String[] labels = {"All", "Pending", "Confirmed", "Completed", "Declined", "Cancelled"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, labels);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinAdapter);

        btnFilter.setOnClickListener(v -> loadAppointments());
        btnBack.setOnClickListener(v -> finish());

        // Check if launched with a filter from dashboard
        String filterExtra = getIntent().getStringExtra("status_filter");
        if (filterExtra != null) {
            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equals(filterExtra)) { spinnerStatus.setSelection(i); break; }
            }
        }

        loadAppointments();
    }

    @Override
    protected void onResume() { super.onResume(); loadAppointments(); }

    private void loadAppointments() {
        int pos = spinnerStatus.getSelectedItemPosition();
        String filter = statuses[pos];
        List<Appointment> list = db.getAllAppointments(filter);

        tvCount.setText(list.size() + " record" + (list.size() != 1 ? "s" : "") + " found");

        if (list.isEmpty()) {
            lvAppointments.setVisibility(android.view.View.GONE);
            tvEmpty.setVisibility(android.view.View.VISIBLE);
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
            lvAppointments.setVisibility(android.view.View.VISIBLE);
            AdminAppointmentAdapter adapter = new AdminAppointmentAdapter(this, list, db, this::loadAppointments);
            lvAppointments.setAdapter(adapter);
            AdminDashboardActivity.setListViewHeightBasedOnChildren(lvAppointments);
        }
    }
}
