package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.models.Service;
import java.util.List;

/**
 * SearchActivity — Search services with auto-complete.
 * Demonstrates: AutoCompleteTextView.
 */
public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        AutoCompleteTextView autoComplete = findViewById(R.id.autoComplete);
        Button btnSearchGo               = findViewById(R.id.btnSearchGo);
        TextView tvResult                = findViewById(R.id.tvSearchResult);

        List<Service> services = ServiceData.getAll();
        String[] serviceNames  = ServiceData.getNames();

        // AutoCompleteTextView + ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, serviceNames);
        autoComplete.setAdapter(adapter);

        // When user picks a suggestion, show service details
        autoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            showResult(selected, services, tvResult);
        });

        // Search button
        btnSearchGo.setOnClickListener(v -> {
            String query = autoComplete.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Type a service name to search", Toast.LENGTH_SHORT).show();
                return;
            }
            showResult(query, services, tvResult);
        });
    }

    private void showResult(String query, List<Service> services, TextView tvResult) {
        for (Service s : services) {
            if (s.name.equalsIgnoreCase(query) ||
                s.name.toLowerCase().contains(query.toLowerCase())) {

                tvResult.setVisibility(android.view.View.VISIBLE);
                tvResult.setText(
                    "📋  " + s.name + "\n" +
                    "📂  Category: " + s.category + "\n" +
                    "💰  Price: ₱" + String.format("%.2f", s.price) + "\n" +
                    "⏱  Duration: " + s.durationHr + " hr(s)\n\n" +
                    s.description
                );
                tvResult.setTextColor(getColor(R.color.white));

                Toast.makeText(this, "Found: " + s.name, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        tvResult.setVisibility(android.view.View.VISIBLE);
        tvResult.setText("No service found for \"" + query + "\"");
        tvResult.setTextColor(getColor(R.color.danger));
        Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show();
    }
}
