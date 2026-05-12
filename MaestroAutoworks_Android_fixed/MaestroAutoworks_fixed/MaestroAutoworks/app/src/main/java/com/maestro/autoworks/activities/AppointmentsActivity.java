package com.maestro.autoworks.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AppointmentAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import java.util.List;

/**
 * AppointmentsActivity — My appointments.
 * Demonstrates: SQLite read + ListView display.
 */
public class AppointmentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        SessionManager session = new SessionManager(this);
        DatabaseHelper  db     = new DatabaseHelper(this);

        ListView listAppts = findViewById(R.id.listAppointments);
        TextView tvEmpty   = findViewById(R.id.tvEmpty);

        // Read from SQLite
        List<Appointment> appointments = db.getAppointmentsByUser(session.getUserId());

        if (appointments.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listAppts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            AppointmentAdapter adapter = new AppointmentAdapter(this, appointments);
            listAppts.setAdapter(adapter);
        }
    }
}
