package com.maestro.autoworks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.maestro.autoworks.R;
import com.maestro.autoworks.models.Appointment;

import java.util.List;

public class AppointmentAdapter extends ArrayAdapter<Appointment> {

    private final Context context;
    private final List<Appointment> appointments;

    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        super(context, 0, appointments);
        this.context      = context;
        this.appointments = appointments;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_appointment, parent, false);
        }

        Appointment appt = appointments.get(position);

        TextView tvService = convertView.findViewById(R.id.tvApptService);
        TextView tvDate    = convertView.findViewById(R.id.tvApptDate);
        TextView tvStatus  = convertView.findViewById(R.id.tvApptStatus);
        TextView tvPlate   = convertView.findViewById(R.id.tvApptPlate);

        tvService.setText(appt.serviceName);
        tvDate.setText(appt.date + "  " + appt.time);
        tvStatus.setText(appt.status.toUpperCase());
        tvPlate.setText("Plate: " + appt.carPlate +
                (appt.totalPrice > 0 ? "   Total: ₱" + String.format("%.2f", appt.totalPrice) : ""));

        // Color code status
        int color;
        switch (appt.status) {
            case "confirmed": color = context.getColor(R.color.success); break;
            case "cancelled": color = context.getColor(R.color.danger);  break;
            default:          color = context.getColor(R.color.yellow);  break;
        }
        tvStatus.setTextColor(color);

        return convertView;
    }
}
