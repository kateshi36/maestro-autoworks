package com.maestro.autoworks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maestro.autoworks.R;
import com.maestro.autoworks.models.Appointment;

import java.util.List;

/**
 * AppointmentAdapter  —  Stage 4D
 * ──────────────────────────────────────────────────────────────────────────
 * Migrated from ArrayAdapter (ListView) → RecyclerView.Adapter + ViewHolder.
 *
 * Binds every view in the Stage 4C rich-card layout (list_item_appointment):
 *   • tvApptIcon     — emoji auto-selected from serviceName
 *   • tvApptService  — service name (bold white)
 *   • tvApptStatus   — text + background drawable swapped per status
 *   • tvApptDate     — "date · time" row
 *   • tvApptPlate    — plate number (split from price as of 4C)
 *   • tvApptPrice    — total price, GONE when totalPrice == 0
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    // ── Status → drawable resource name mapping ────────────────────────────
    // drawables: bg_status_pill_pending | bg_status_pill_confirmed |
    //            bg_status_pill_cancelled  (all created in Stage 4A)
    private static final int STATUS_BG_PENDING   = R.drawable.bg_status_pill_pending;
    private static final int STATUS_BG_CONFIRMED = R.drawable.bg_status_pill_confirmed;
    private static final int STATUS_BG_CANCELLED = R.drawable.bg_status_pill_cancelled;

    private final Context          context;
    private final List<Appointment> appointments;

    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context      = context;
        this.appointments = appointments;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ViewHolder
    // ══════════════════════════════════════════════════════════════════════

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon;
        final TextView tvService;
        final TextView tvStatus;
        final TextView tvDate;
        final TextView tvPlate;
        final TextView tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon    = itemView.findViewById(R.id.tvApptIcon);
            tvService = itemView.findViewById(R.id.tvApptService);
            tvStatus  = itemView.findViewById(R.id.tvApptStatus);
            tvDate    = itemView.findViewById(R.id.tvApptDate);
            tvPlate   = itemView.findViewById(R.id.tvApptPlate);
            tvPrice   = itemView.findViewById(R.id.tvApptPrice);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Adapter overrides
    // ══════════════════════════════════════════════════════════════════════

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
        Appointment appt = appointments.get(position);

        // ── 1. Service icon (emoji based on service name) ──────────────────
        vh.tvIcon.setText(resolveServiceIcon(appt.serviceName));

        // ── 2. Service name ────────────────────────────────────────────────
        vh.tvService.setText(appt.serviceName);

        // ── 3. Status pill — hidden when status is null/blank ─────────────
        String statusLabel = (appt.status != null) ? appt.status.trim() : "";
        if (statusLabel.isEmpty()) {
            vh.tvStatus.setVisibility(View.GONE);
        } else {
            vh.tvStatus.setVisibility(View.VISIBLE);
            vh.tvStatus.setText(statusLabel.toUpperCase());
            applyStatusPill(vh.tvStatus, appt.status);
        }

        // ── 4. Date & time row ─────────────────────────────────────────────
        String dateTime = nullSafe(appt.date);
        if (appt.time != null && !appt.time.isEmpty()) {
            dateTime += "  ·  " + appt.time;
        }
        vh.tvDate.setText(dateTime);

        // ── 5. Plate number ────────────────────────────────────────────────
        String plate = (appt.carPlate != null && !appt.carPlate.isEmpty())
                ? appt.carPlate
                : "—";
        vh.tvPlate.setText(plate);

        // ── 6. Total price (hidden when zero) ─────────────────────────────
        if (appt.totalPrice > 0) {
            vh.tvPrice.setVisibility(View.VISIBLE);
            vh.tvPrice.setText(String.format("₱%.2f", appt.totalPrice));
        } else {
            vh.tvPrice.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns the emoji that best represents the service type.
     *
     * Matching is case-insensitive and substring-based so it handles
     * full names like "Oil Change & Filter" or "Engine Tune-Up".
     */
    private String resolveServiceIcon(String serviceName) {
        if (serviceName == null) return "🔧";
        String lower = serviceName.toLowerCase();
        if (lower.contains("oil"))              return "🛢️";
        if (lower.contains("tune") ||
                lower.contains("engine"))           return "⚙️";
        if (lower.contains("tyre") ||
                lower.contains("tire") ||
                lower.contains("wheel"))            return "🔄";
        if (lower.contains("brake"))            return "🛑";
        if (lower.contains("battery"))          return "🔋";
        if (lower.contains("wash") ||
                lower.contains("detail"))           return "🧹";
        if (lower.contains("inspect") ||
                lower.contains("check"))            return "🔍";
        if (lower.contains("ac") ||
                lower.contains("air"))              return "❄️";
        return "🔧"; // fallback — general repair
    }

    /**
     * Swaps the status pill background drawable and text colour
     * to match the appointment status.
     *
     * Drawables (created in Stage 4A):
     *   bg_status_pill_pending    — amber stroke
     *   bg_status_pill_confirmed  — green stroke
     *   bg_status_pill_cancelled  — red stroke
     */
    private void applyStatusPill(TextView tv, String status) {
        if (status == null) status = "pending";
        switch (status.toLowerCase()) {
            case "confirmed":
            case "completed":
                tv.setBackgroundResource(STATUS_BG_CONFIRMED);
                tv.setTextColor(context.getColor(R.color.success));
                break;
            case "cancelled":
            case "declined":
                tv.setBackgroundResource(STATUS_BG_CANCELLED);
                tv.setTextColor(context.getColor(R.color.danger));
                break;
            default: // "pending" + anything unrecognised
                tv.setBackgroundResource(STATUS_BG_PENDING);
                tv.setTextColor(context.getColor(R.color.yellow));
                break;
        }
    }

    private String nullSafe(String s) {
        return (s != null && !s.isEmpty()) ? s : "—";
    }
}