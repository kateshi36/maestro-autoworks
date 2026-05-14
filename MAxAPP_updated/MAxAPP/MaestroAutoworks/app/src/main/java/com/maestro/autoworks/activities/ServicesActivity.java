package com.maestro.autoworks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.ServiceAdapter;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.models.Service;
import java.util.List;

/**
 * ServicesActivity — Browse all services.
 * Demonstrates: ListView & ArrayAdapter (custom ServiceAdapter).
 */
public class ServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        List<Service> services = ServiceData.getAll();

        ListView listServices = findViewById(R.id.listServices);

        // ArrayAdapter (custom) — binds Service list to list_item_service layout
        ServiceAdapter adapter = new ServiceAdapter(this, services);
        listServices.setAdapter(adapter);

        // List item click → navigate to BookActivity with service name
        listServices.setOnItemClickListener((parent, view, position, id) -> {
            Service selected = services.get(position);
            Toast.makeText(this, "Selected: " + selected.name, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, BookActivity.class);
            intent.putExtra("service_name", selected.name);
            startActivity(intent);
        });
    }
}
