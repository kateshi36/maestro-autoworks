package com.maestro.autoworks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.maestro.autoworks.R;
import com.maestro.autoworks.models.Service;

import java.util.List;

/**
 * Custom ArrayAdapter for displaying Service items in a ListView.
 */
public class ServiceAdapter extends ArrayAdapter<Service> {

    private final Context context;
    private final List<Service> services;

    public ServiceAdapter(Context context, List<Service> services) {
        super(context, 0, services);
        this.context  = context;
        this.services = services;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_service, parent, false);
        }

        Service svc = services.get(position);

        TextView tvName     = convertView.findViewById(R.id.tvServiceName);
        TextView tvPrice    = convertView.findViewById(R.id.tvServicePrice);
        TextView tvDuration = convertView.findViewById(R.id.tvServiceDuration);
        TextView tvCategory = convertView.findViewById(R.id.tvServiceCategory);

        tvName.setText(svc.name);
        tvPrice.setText(String.format("₱%.2f", svc.price));
        tvDuration.setText(svc.durationHr + " hr" + (svc.durationHr != 1 ? "s" : ""));
        tvCategory.setText(svc.category);

        return convertView;
    }
}
