package com.maestro.autoworks.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AdminServiceAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Service;

import java.util.List;

/**
 * AdminServicesActivity — mirrors admin_services.php.
 * View all services, toggle availability (active/inactive), edit details, add new.
 * Uses the existing static ServiceData list but overlays an "active" state
 * stored in SQLite via a new admin_services table.
 */
public class AdminServicesActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView lvServices;
    private TextView tvAvailable, tvUnavailable, tvTotalSvc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_services);

        db = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        if (!session.isAdmin()) { finish(); return; }

        lvServices   = findViewById(R.id.lvAdminServices);
        tvAvailable  = findViewById(R.id.tvSvcAvailable);
        tvUnavailable= findViewById(R.id.tvSvcUnavailable);
        tvTotalSvc   = findViewById(R.id.tvSvcTotal);

        Button btnBack   = findViewById(R.id.btnAdminSvcBack);
        Button btnAddSvc = findViewById(R.id.btnAddService);
        btnBack.setOnClickListener(v -> finish());
        btnAddSvc.setOnClickListener(v -> showAddServiceDialog());

        // Ensure admin_services table exists with active column
        db.getWritableDatabase().execSQL(
            "CREATE TABLE IF NOT EXISTS admin_services " +
            "(id INTEGER PRIMARY KEY, active INTEGER NOT NULL DEFAULT 1, " +
            "name TEXT, description TEXT, category TEXT, price REAL, duration_hr REAL)"
        );

        loadServices();
    }

    @Override
    protected void onResume() { super.onResume(); loadServices(); }

    void loadServices() {
        List<Service> services = ServiceData.getAll();
        // Sync active flags from DB
        for (Service s : services) {
            android.database.Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT active FROM admin_services WHERE id=?", new String[]{String.valueOf(s.id)});
            if (c.moveToFirst()) {
                s.active = c.getInt(0) == 1;
            } else {
                s.active = true; // default active
            }
            c.close();
        }

        int avail  = (int) services.stream().filter(s -> s.active).count();
        int unavail = services.size() - avail;
        tvAvailable.setText(String.valueOf(avail));
        tvUnavailable.setText(String.valueOf(unavail));
        tvTotalSvc.setText(String.valueOf(services.size()));

        AdminServiceAdapter adapter = new AdminServiceAdapter(this, services, db, this::loadServices);
        lvServices.setAdapter(adapter);
        AdminDashboardActivity.setListViewHeightBasedOnChildren(lvServices);
    }

    private void showAddServiceDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null);
        new AlertDialog.Builder(this)
            .setTitle("Add New Service")
            .setView(v)
            .setPositiveButton("Add", (dlg, w) -> {
                String name  = ((EditText) v.findViewById(R.id.etSvcName)).getText().toString().trim();
                String cat   = ((EditText) v.findViewById(R.id.etSvcCategory)).getText().toString().trim();
                String priceStr = ((EditText) v.findViewById(R.id.etSvcPrice)).getText().toString().trim();
                String durStr   = ((EditText) v.findViewById(R.id.etSvcDuration)).getText().toString().trim();
                String desc  = ((EditText) v.findViewById(R.id.etSvcDesc)).getText().toString().trim();

                if (name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(this, "Name, category and price are required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double price = Double.parseDouble(priceStr);
                double dur   = durStr.isEmpty() ? 1.0 : Double.parseDouble(durStr);

                // Generate a new id > existing max
                int newId = ServiceData.getAll().stream().mapToInt(s -> s.id).max().orElse(0) + 1;
                Service ns = new Service(newId, name, desc, cat, price, dur);
                ServiceData.addService(ns);

                // Persist active flag and data in admin_services
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("id", newId); cv.put("active", 1);
                cv.put("name", name); cv.put("description", desc);
                cv.put("category", cat); cv.put("price", price); cv.put("duration_hr", dur);
                db.getWritableDatabase().insertWithOnConflict("admin_services", null, cv,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);

                Toast.makeText(this, "Service \"" + name + "\" added!", Toast.LENGTH_SHORT).show();
                loadServices();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
