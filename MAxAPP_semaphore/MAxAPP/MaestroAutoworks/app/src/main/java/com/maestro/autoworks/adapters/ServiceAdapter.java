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

public class ServiceAdapter extends ArrayAdapter<Service> {

    public ServiceAdapter(Context context, List<Service> services) {
        super(context, 0, services);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.list_item_service, parent, false);

        Service s = getItem(position);
        ((TextView) convertView.findViewById(R.id.tvServiceName)).setText(s.name);
        ((TextView) convertView.findViewById(R.id.tvServiceCategory)).setText(s.category);
        ((TextView) convertView.findViewById(R.id.tvServicePrice)).setText(
            String.format("₱%.2f", s.price));
        ((TextView) convertView.findViewById(R.id.tvServiceDuration)).setText(
            s.durationHr + " hr" + (s.durationHr != 1 ? "s" : ""));
        ((TextView) convertView.findViewById(R.id.tvServiceDesc)).setText(s.description);
        return convertView;
    }
}
