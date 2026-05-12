package com.maestro.autoworks.adapters;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.Service;

import java.util.List;

/** ListView adapter for admin's service list with toggle/edit actions. */
public class AdminServiceAdapter extends ArrayAdapter<Service> {

    private final DatabaseHelper db;
    private final Runnable onRefresh;

    public AdminServiceAdapter(Context ctx, List<Service> list, DatabaseHelper db, Runnable onRefresh) {
        super(ctx, 0, list);
        this.db        = db;
        this.onRefresh = onRefresh;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_admin_service, parent, false);

        Service s = getItem(position);

        TextView tvName   = convertView.findViewById(R.id.tvSvcName);
        TextView tvCat    = convertView.findViewById(R.id.tvSvcCat);
        TextView tvPrice  = convertView.findViewById(R.id.tvSvcPrice);
        TextView tvDur    = convertView.findViewById(R.id.tvSvcDur);
        TextView tvStatus = convertView.findViewById(R.id.tvSvcStatus);
        Button btnToggle  = convertView.findViewById(R.id.btnToggleService);
        Button btnEdit    = convertView.findViewById(R.id.btnEditService);

        tvName.setText(s.name);
        tvCat.setText(s.category);
        tvPrice.setText(String.format("₱%.2f", s.price));
        tvDur.setText(s.durationHr + " hr" + (s.durationHr != 1 ? "s" : ""));

        if (s.active) {
            tvStatus.setText("AVAILABLE");
            tvStatus.setTextColor(0xFF4CAF7D);
            btnToggle.setText("Set Unavailable");
            btnToggle.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE05252));
        } else {
            tvStatus.setText("UNAVAILABLE");
            tvStatus.setTextColor(0xFFE05252);
            btnToggle.setText("Set Available");
            btnToggle.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF7D));
        }

        convertView.setAlpha(s.active ? 1f : 0.6f);

        btnToggle.setOnClickListener(v -> {
            int newActive = s.active ? 0 : 1;
            SQLiteDatabase wdb = db.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", s.id); cv.put("active", newActive);
            wdb.insertWithOnConflict("admin_services", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
            Toast.makeText(getContext(),
                "\"" + s.name + "\" marked as " + (newActive == 1 ? "available" : "unavailable") + ".",
                Toast.LENGTH_SHORT).show();
            onRefresh.run();
        });

        btnEdit.setOnClickListener(v -> showEditDialog(s));

        return convertView;
    }

    private void showEditDialog(Service s) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_service, null);
        EditText etName  = v.findViewById(R.id.etSvcName);
        EditText etCat   = v.findViewById(R.id.etSvcCategory);
        EditText etPrice = v.findViewById(R.id.etSvcPrice);
        EditText etDur   = v.findViewById(R.id.etSvcDuration);
        EditText etDesc  = v.findViewById(R.id.etSvcDesc);

        etName.setText(s.name);
        etCat.setText(s.category);
        etPrice.setText(String.valueOf(s.price));
        etDur.setText(String.valueOf(s.durationHr));
        etDesc.setText(s.description);

        new AlertDialog.Builder(getContext())
            .setTitle("Edit Service")
            .setView(v)
            .setPositiveButton("Save Changes", (dlg, w) -> {
                String name  = etName.getText().toString().trim();
                String cat   = etCat.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String durStr   = etDur.getText().toString().trim();
                String desc  = etDesc.getText().toString().trim();

                if (name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(getContext(), "Name, category, and price required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                s.name        = name;
                s.category    = cat;
                s.price       = Double.parseDouble(priceStr);
                s.durationHr  = durStr.isEmpty() ? s.durationHr : Double.parseDouble(durStr);
                s.description = desc;

                // Persist
                SQLiteDatabase wdb = db.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("id", s.id); cv.put("active", s.active ? 1 : 0);
                cv.put("name", s.name); cv.put("description", s.description);
                cv.put("category", s.category); cv.put("price", s.price);
                cv.put("duration_hr", s.durationHr);
                wdb.insertWithOnConflict("admin_services", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

                Toast.makeText(getContext(), "Service updated!", Toast.LENGTH_SHORT).show();
                onRefresh.run();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
