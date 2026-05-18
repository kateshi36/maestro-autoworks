package com.maestro.autoworks.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AdminAppointmentAdapter;
import com.maestro.autoworks.adapters.VerificationAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.Service;
import com.maestro.autoworks.models.User;
import com.maestro.autoworks.utils.NotificationHelper;
import com.maestro.autoworks.utils.ReceiptEmailSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminDashboardActivity — Unified 5-panel admin shell.
 *
 * Panels (hosted inside #adminPanelContainer via visibility toggling):
 *   1. panelDashboard    — stat strip + pending requests + today's schedule
 *   2. panelAppointments — filter chips + full appointments list
 *   3. panelServices     — add button + services list
 *   4. panelReports      — stat cards + most-booked services list
 *   5. panelPms          — search + filter + repair tracker cards
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;

    /** Cached admin user ID — resolved once in onCreate via the DB, used for notification badge. */
    private int adminUserId = -1;

    // Panels
    private View panelDashboard, panelAppointments, panelServices, panelReports, panelPms,
            panelVerifications;
    private DrawerLayout drawerLayout;

    // Panel 1 – Dashboard
    private TextView tvStatTotal, tvStatPending, tvStatConfirmed, tvStatToday;
    private ListView listDashboardPending, listDashboardToday;
    private TextView tvNoPending, tvNoToday;

    // Panel 2 – Appointments
    private RecyclerView rvAppointments;
    private TextView tvNoAppointments;
    private TextView chipAll, chipPending, chipConfirmed, chipDeclined;
    private String currentApptFilter = "all";

    // Panel 3 – Services
    private RecyclerView rvServices;
    private TextView tvNoServices;
    private TextView tvSvcStatsAvailable, tvSvcStatsUnavailable, tvSvcStatsTotal;

    // Panel 4 – Reports
    private TextView tvReportTotalAppts, tvReportCompleted, tvReportPending, tvReportDeclined;
    private TextView tvReportConfirmed, tvReportCancelled;
    private TextView tvCompletionRate;
    private LinearLayout completionBarBg, completionBarFill;
    private LinearLayout topServicesContainer, dailyActivityContainer;
    private TextView tvNoReportData;

    // Panel 5 – PMS
    private RecyclerView rvPmsRecords;
    private TextView tvNoPmsRecords;
    private EditText etPmsSearch;
    private TextView tvPmsStatsActive, tvPmsStatsDone, tvPmsStatsInProgress, tvPmsStatsQueued;
    private String pmsStatusFilter = "all";
    private String pmsSearchQuery  = "";

    // Panel 6 – Verifications
    private RecyclerView rvVerifications;
    private TextView tvNoVerifications;
    private TextView tvVerifPendingCount;
    private boolean verifShowAll = false;

    private int currentPanel = 1;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        // Resolve the admin's user-table row ID once, for notification badge queries.
        adminUserId = db.getAdminUserId();
        if (adminUserId == -1) adminUserId = session.getUserId(); // fallback to session

        if (!session.isAdmin()) {
            Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensure admin_services table exists (needed by Panel 3)
        db.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS admin_services " +
                        "(id INTEGER PRIMARY KEY, active INTEGER NOT NULL DEFAULT 1, " +
                        "name TEXT, description TEXT, category TEXT, price REAL, duration_hr REAL)"
        );

        bindViews();
        wireDrawer();
        wireBottomNav();

        // Drawer header
        ((TextView) findViewById(R.id.drawerAdminName)).setText(session.getFullName());

        // Hamburger opens drawer
        findViewById(R.id.btnHamburger).setOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START));

        // Bell always opens the admin notification dialog, regardless of badge count
        findViewById(R.id.btnNotifications).setOnClickListener(
                v -> openAdminNotificationsDialog());

        showPanel(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentPanel();
        refreshAdminNotifBadge();
    }

    /**
     * Refreshes the pending-appointment badge visible in the drawer / hamburger area.
     * Shows the count of unread admin notifications so the admin always sees the latest
     * number when returning to the screen — even after acting on a booking elsewhere.
     */
    private void refreshAdminNotifBadge() {
        if (adminUserId == -1) return;
        int unread = db.countUnreadNotifications(adminUserId);
        // tvAdminNotifBadge is an optional overlay TextView on the hamburger button.
        // If the layout doesn't include it yet, this block is a safe no-op.
        View badge = findViewById(R.id.tvAdminNotifBadge);
        if (badge instanceof TextView) {
            if (unread > 0) {
                ((TextView) badge).setText(unread > 99 ? "99+" : String.valueOf(unread));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
            // Tap target is the bell (btnNotifications); badge is visual only.
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);

        panelDashboard    = findViewById(R.id.panelDashboard);
        panelAppointments = findViewById(R.id.panelAppointments);
        panelServices     = findViewById(R.id.panelServices);
        panelReports      = findViewById(R.id.panelReports);
        panelPms          = findViewById(R.id.panelPms);
        panelVerifications = findViewById(R.id.panelVerifications);

        // Dashboard
        tvStatTotal          = findViewById(R.id.tvStatTotal);
        tvStatPending        = findViewById(R.id.tvStatPending);
        tvStatConfirmed      = findViewById(R.id.tvStatConfirmed);
        tvStatToday          = findViewById(R.id.tvStatToday);
        listDashboardPending = findViewById(R.id.listDashboardPending);
        listDashboardToday   = findViewById(R.id.listDashboardToday);
        tvNoPending          = findViewById(R.id.tvNoPending);
        tvNoToday            = findViewById(R.id.tvNoToday);

        // Appointments
        rvAppointments   = findViewById(R.id.rvAppointments);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);
        chipAll          = findViewById(R.id.chipAll);
        chipPending      = findViewById(R.id.chipPending);
        chipConfirmed    = findViewById(R.id.chipConfirmed);
        chipDeclined     = findViewById(R.id.chipDeclined);

        chipAll.setOnClickListener(v -> { currentApptFilter = "all";       updateApptChips(); loadPanelAppointments(); });
        chipPending.setOnClickListener(v -> { currentApptFilter = "pending";    updateApptChips(); loadPanelAppointments(); });
        chipConfirmed.setOnClickListener(v -> { currentApptFilter = "confirmed"; updateApptChips(); loadPanelAppointments(); });
        chipDeclined.setOnClickListener(v -> { currentApptFilter = "declined";  updateApptChips(); loadPanelAppointments(); });

        // Services
        rvServices   = findViewById(R.id.rvServices);
        tvNoServices = findViewById(R.id.tvNoServices);
        tvSvcStatsAvailable   = findViewById(R.id.tvSvcStatsAvailable);
        tvSvcStatsUnavailable = findViewById(R.id.tvSvcStatsUnavailable);
        tvSvcStatsTotal       = findViewById(R.id.tvSvcStatsTotal);
        findViewById(R.id.btnAddService).setOnClickListener(v -> showAddServiceDialog());

        // Reports
        tvReportTotalAppts  = findViewById(R.id.tvReportTotalAppts);
        tvReportCompleted   = findViewById(R.id.tvReportCompleted);
        tvReportPending     = findViewById(R.id.tvReportPending);
        tvReportDeclined    = findViewById(R.id.tvReportDeclined);
        tvReportConfirmed   = findViewById(R.id.tvReportConfirmed);
        tvReportCancelled   = findViewById(R.id.tvReportCancelled);
        tvCompletionRate    = findViewById(R.id.tvCompletionRate);
        completionBarBg     = findViewById(R.id.completionBarBg);
        completionBarFill   = findViewById(R.id.completionBarFill);
        topServicesContainer  = findViewById(R.id.topServicesContainer);
        dailyActivityContainer = findViewById(R.id.dailyActivityContainer);
        tvNoReportData      = findViewById(R.id.tvNoReportData);

        // PMS
        rvPmsRecords    = findViewById(R.id.rvPmsRecords);
        tvNoPmsRecords  = findViewById(R.id.tvNoPmsRecords);
        etPmsSearch     = findViewById(R.id.etPmsSearch);
        tvPmsStatsActive     = findViewById(R.id.tvPmsStatsActive);
        tvPmsStatsDone       = findViewById(R.id.tvPmsStatsDone);
        tvPmsStatsInProgress = findViewById(R.id.tvPmsStatsInProgress);
        tvPmsStatsQueued     = findViewById(R.id.tvPmsStatsQueued);

        etPmsSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                pmsSearchQuery = s.toString().trim();
                loadPanelPms();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // PMS filter chips
        findViewById(R.id.pmsChipAll).setOnClickListener(v ->       { pmsStatusFilter = "all";         updatePmsChips(); loadPanelPms(); });
        findViewById(R.id.pmsChipInProgress).setOnClickListener(v -> { pmsStatusFilter = "in_progress"; updatePmsChips(); loadPanelPms(); });
        findViewById(R.id.pmsChipDone).setOnClickListener(v ->      { pmsStatusFilter = "done";         updatePmsChips(); loadPanelPms(); });

        // PMS add — not applicable here (appointments come from booking flow)
        findViewById(R.id.btnAddPmsRecord).setOnClickListener(v ->
                Toast.makeText(this, "Appointments are added via the customer booking flow.", Toast.LENGTH_SHORT).show());

        // Panel 6 – Verifications
        rvVerifications    = findViewById(R.id.rvVerifications);
        tvNoVerifications  = findViewById(R.id.tvNoVerifications);
        tvVerifPendingCount = findViewById(R.id.tvVerifPendingCount);

        findViewById(R.id.verifChipPending).setOnClickListener(v -> {
            verifShowAll = false; updateVerifChips(); loadPanelVerifications();
        });
        findViewById(R.id.verifChipAll).setOnClickListener(v -> {
            verifShowAll = true; updateVerifChips(); loadPanelVerifications();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void wireBottomNav() {
        findViewById(R.id.navAdminDashboard).setOnClickListener(v    -> showPanel(1));
        findViewById(R.id.navAdminAppointments).setOnClickListener(v -> showPanel(2));
        findViewById(R.id.navAdminServices).setOnClickListener(v     -> showPanel(3));
        findViewById(R.id.navAdminReports).setOnClickListener(v      -> showPanel(4));
        findViewById(R.id.navAdminPms).setOnClickListener(v          -> showPanel(5));
        findViewById(R.id.navAdminVerif).setOnClickListener(v        -> showPanel(6));
    }

    private void wireDrawer() {
        findViewById(R.id.drawerNavDashboard).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(1); });
        findViewById(R.id.drawerNavAppointments).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(2); });
        findViewById(R.id.drawerNavServices).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(3); });
        findViewById(R.id.drawerNavReports).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(4); });
        findViewById(R.id.drawerNavPms).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(5); });
        findViewById(R.id.drawerNavVerif).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START); showPanel(6); });
        findViewById(R.id.drawerNavLogout).setOnClickListener(v -> {
            session.logout();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void showPanel(int panel) {
        currentPanel = panel;
        panelDashboard.setVisibility(panel == 1 ? View.VISIBLE : View.GONE);
        panelAppointments.setVisibility(panel == 2 ? View.VISIBLE : View.GONE);
        panelServices.setVisibility(panel == 3 ? View.VISIBLE : View.GONE);
        panelReports.setVisibility(panel == 4 ? View.VISIBLE : View.GONE);
        panelPms.setVisibility(panel == 5 ? View.VISIBLE : View.GONE);
        panelVerifications.setVisibility(panel == 6 ? View.VISIBLE : View.GONE);
        updateBottomNavHighlight(panel);
        updateTopbarTitle(panel);
        refreshCurrentPanel();
    }

    private void refreshCurrentPanel() {
        switch (currentPanel) {
            case 1: loadPanelDashboard();      break;
            case 2: loadPanelAppointments();   break;
            case 3: loadPanelServices();       break;
            case 4: loadPanelReports();        break;
            case 5: loadPanelPms();            break;
            case 6: loadPanelVerifications();  break;
        }
    }

    private void updateBottomNavHighlight(int active) {
        int[] labelIds = {
                R.id.navAdminDashboardLabel, R.id.navAdminAppointmentsLabel,
                R.id.navAdminServicesLabel,  R.id.navAdminReportsLabel,
                R.id.navAdminPmsLabel,       R.id.navAdminVerifLabel
        };
        for (int i = 0; i < labelIds.length; i++) {
            ((TextView) findViewById(labelIds[i])).setTextColor(
                    (i + 1 == active)
                            ? getResources().getColor(R.color.yellow, null)
                            : getResources().getColor(R.color.muted,  null));
        }
    }

    private void updateTopbarTitle(int panel) {
        View logoGroup = findViewById(R.id.topbarLogoGroup);
        TextView tvTitle = findViewById(R.id.tvTopbarTitle);
        if (panel == 1) {
            logoGroup.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.GONE);
        } else {
            logoGroup.setVisibility(View.GONE);
            tvTitle.setVisibility(View.VISIBLE);
            String[] titles = {"", "Dashboard", "Appointments", "Services", "Reports",
                    "Repair Tracker", "Verifications"};
            tvTitle.setText(titles[panel]);
        }
    }

    // =========================================================================
    // PANEL 1 — DASHBOARD
    // =========================================================================
    private void loadPanelDashboard() {
        // Stats strip
        int pending   = db.countByStatus("pending");
        int confirmed = db.countByStatus("confirmed");
        int total     = db.countByStatus("all");
        String today  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int todayCount = db.getTodayAppointments(today).size();

        tvStatTotal.setText(String.valueOf(total));
        tvStatPending.setText(String.valueOf(pending));
        tvStatConfirmed.setText(String.valueOf(confirmed));
        tvStatToday.setText(String.valueOf(todayCount));

        // Pending requests list
        List<Appointment> pendingList = db.getAllAppointments("pending");
        if (pendingList.isEmpty()) {
            listDashboardPending.setVisibility(View.GONE);
            tvNoPending.setVisibility(View.VISIBLE);
        } else {
            tvNoPending.setVisibility(View.GONE);
            listDashboardPending.setVisibility(View.VISIBLE);
            AdminAppointmentAdapter a = new AdminAppointmentAdapter(
                    this, pendingList, db, this::loadPanelDashboard);
            listDashboardPending.setAdapter(a);
            setListViewHeightBasedOnChildren(listDashboardPending);
        }

        // Today's schedule list
        List<Appointment> todayList = db.getTodayAppointments(today);
        if (todayList.isEmpty()) {
            listDashboardToday.setVisibility(View.GONE);
            tvNoToday.setVisibility(View.VISIBLE);
        } else {
            tvNoToday.setVisibility(View.GONE);
            listDashboardToday.setVisibility(View.VISIBLE);
            AdminAppointmentAdapter a = new AdminAppointmentAdapter(
                    this, todayList, db, this::loadPanelDashboard);
            listDashboardToday.setAdapter(a);
            setListViewHeightBasedOnChildren(listDashboardToday);
        }
    }

    // =========================================================================
    // PANEL 2 — APPOINTMENTS
    // =========================================================================
    private void loadPanelAppointments() {
        List<Appointment> list = db.getAllAppointments(currentApptFilter);
        if (list.isEmpty()) {
            rvAppointments.setVisibility(View.GONE);
            tvNoAppointments.setVisibility(View.VISIBLE);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
            rvAppointments.setVisibility(View.VISIBLE);
            rvAppointments.setLayoutManager(new LinearLayoutManager(this));
            rvAppointments.setAdapter(new ApptRvAdapter(list));
        }
    }

    private void updateApptChips() {
        resetChipStyle(chipAll);
        resetChipStyle(chipPending);
        resetChipStyle(chipConfirmed);
        resetChipStyle(chipDeclined);
        TextView active;
        switch (currentApptFilter) {
            case "pending":   active = chipPending;   break;
            case "confirmed": active = chipConfirmed; break;
            case "declined":  active = chipDeclined;  break;
            default:          active = chipAll;        break;
        }
        active.setBackgroundColor(getResources().getColor(R.color.yellow, null));
        active.setTextColor(getResources().getColor(R.color.black, null));
        active.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetChipStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_form_card);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    /**
     * Panel 2 RecyclerView adapter — full port of AdminAppointmentAdapter logic.
     * Inflates list_item_admin_appointment.xml; shows Confirm / Decline / Complete
     * buttons based on status.  Confirm and Decline open dialog_admin_action.xml
     * so the admin can add an optional note before committing the action.
     * On confirmation: sends receipt email, inserts in-app notification, posts
     * Android push notification — matching AdminAppointmentsActivity exactly.
     */
    private class ApptRvAdapter extends RecyclerView.Adapter<ApptRvAdapter.VH> {
        private final List<Appointment> data;
        ApptRvAdapter(List<Appointment> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_admin_appointment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) { h.bind(data.get(pos)); }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }

            void bind(Appointment a) {
                String customerDisplay = (a.customerName != null && !a.customerName.trim().isEmpty())
                        ? a.customerName : "User #" + a.userId;

                ((TextView) itemView.findViewById(R.id.tvAdminApptCustomer)).setText(customerDisplay);
                ((TextView) itemView.findViewById(R.id.tvAdminApptService)).setText(a.serviceName);
                ((TextView) itemView.findViewById(R.id.tvAdminApptDate)).setText(a.date + "  " + a.time);
                ((TextView) itemView.findViewById(R.id.tvAdminApptPlate)).setText(
                        (a.carPlate != null && !a.carPlate.isEmpty()) ? "Plate: " + a.carPlate : "");
                ((TextView) itemView.findViewById(R.id.tvAdminApptPrice)).setText(
                        String.format("₱%.2f", a.totalPrice));

                // Status badge colour
                TextView tvStatus = itemView.findViewById(R.id.tvAdminApptStatus);
                tvStatus.setText(a.status.toUpperCase());
                switch (a.status) {
                    case "confirmed":  tvStatus.setTextColor(0xFF4CAF7D); break;
                    case "completed":  tvStatus.setTextColor(0xFF7B61FF); break;
                    case "declined":
                    case "cancelled":  tvStatus.setTextColor(0xFFE05252); break;
                    default:           tvStatus.setTextColor(0xFFF5A623); break; // pending
                }

                // Action button visibility
                Button btnConfirm  = itemView.findViewById(R.id.btnAdminConfirm);
                Button btnDecline  = itemView.findViewById(R.id.btnAdminDecline);
                Button btnComplete = itemView.findViewById(R.id.btnAdminComplete);
                btnConfirm.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                btnComplete.setVisibility(View.GONE);

                if ("pending".equals(a.status)) {
                    btnConfirm.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.VISIBLE);
                } else if ("confirmed".equals(a.status)) {
                    btnComplete.setVisibility(View.VISIBLE);
                }

                // ── Confirm → dialog with note ────────────────────────────────
                btnConfirm.setOnClickListener(v -> showApptActionDialog(
                        a, "confirmed", "Confirm Appointment",
                        "Confirm booking for " + customerDisplay + " — " + a.serviceName + "?"));

                // ── Decline → dialog with note ────────────────────────────────
                btnDecline.setOnClickListener(v -> showApptActionDialog(
                        a, "declined", "Decline Appointment",
                        "Decline booking for " + customerDisplay + " — " + a.serviceName + "?"));

                // ── Mark Complete (no dialog needed) ──────────────────────────
                btnComplete.setOnClickListener(v -> {
                    db.updateAppointmentStatus(a.id, "completed", null);
                    Toast.makeText(AdminDashboardActivity.this,
                            "Marked as Completed.", Toast.LENGTH_SHORT).show();
                    loadPanelAppointments();
                });
            }
        }
    }

    // =========================================================================
    // PANEL 2 — Confirm / Decline action dialog (full port from AdminAppointmentAdapter)
    // =========================================================================

    /**
     * Shows dialog_admin_action.xml so the admin can write an optional note,
     * then commits the status change.  On "confirmed", also fires receipt email,
     * in-app notification, and Android push notification.
     */
    private void showApptActionDialog(Appointment a, String newStatus,
                                      String title, String message) {
        View dlgView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_action, null);
        ((TextView) dlgView.findViewById(R.id.tvDialogMessage)).setText(message);
        EditText etNote = dlgView.findViewById(R.id.etAdminNote);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dlgView)
                .setPositiveButton("confirmed".equals(newStatus) ? "✓ Confirm" : "✗ Decline",
                        (dlg, w) -> {
                            String note = etNote.getText().toString().trim();
                            db.updateAppointmentStatus(a.id, newStatus,
                                    note.isEmpty() ? null : note);

                            // ── Receipt + notification on confirmation only ──
                            if ("confirmed".equals(newStatus)) {
                                a.status    = "confirmed";
                                a.adminNote = note.isEmpty() ? null : note;

                                User customer = db.getUserById(a.userId);
                                if (customer != null) {
                                    // 1. Receipt email
                                    ReceiptEmailSender.sendReceipt(
                                            customer.email, customer.firstName, a,
                                            new ReceiptEmailSender.SendCallback() {
                                                @Override public void onSuccess() {
                                                    Toast.makeText(AdminDashboardActivity.this,
                                                            "Receipt emailed to " + customer.email,
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                                @Override public void onFailure(String err) {
                                                    Toast.makeText(AdminDashboardActivity.this,
                                                            "Email failed: " + err,
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });

                                    // 2. In-app notification → customer (AppointmentsActivity bell)
                                    db.insertNotification(a.userId, a.id,
                                            "Booking Confirmed — Receipt #" + a.id,
                                            buildApptReceiptMessage(a));

                                    // 3. Android push → customer (status bar)
                                    NotificationHelper.postBookingConfirmed(this, a);

                                    // 4. In-app notification → admin (dashboard badge)
                                    db.insertAdminNotification(a.id,
                                            "Booking Confirmed — #" + a.id,
                                            "You confirmed appointment #" + a.id
                                                    + " for " + nullSafeStr(a.serviceName)
                                                    + " on " + nullSafeStr(a.date) + ".");
                                }
                            } else if ("declined".equals(newStatus)) {
                                a.status    = newStatus;
                                a.adminNote = note.isEmpty() ? null : note;

                                // In-app notification → admin (dashboard badge)
                                db.insertAdminNotification(a.id,
                                        "Appointment Declined — #" + a.id,
                                        "You declined appointment #" + a.id
                                                + " for " + nullSafeStr(a.serviceName)
                                                + " on " + nullSafeStr(a.date) + ".");

                                // Android push → admin (status bar)
                                NotificationHelper.postStatusChangeToAdmin(this, a, newStatus);
                            }
                            // ────────────────────────────────────────────────

                            // Refresh the notification badge so the count is current
                            refreshAdminNotifBadge();

                            Toast.makeText(this,
                                    "Appointment " + newStatus + ".",
                                    Toast.LENGTH_SHORT).show();
                            loadPanelAppointments();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Builds the in-app notification body shown in AppointmentsActivity. */
    private String buildApptReceiptMessage(Appointment a) {
        return  "Your booking has been confirmed!\n\n"
                + "Receipt No. : #" + a.id                       + "\n"
                + "Service     : " + nullSafeStr(a.serviceName)  + "\n"
                + "Date        : " + nullSafeStr(a.date)          + "\n"
                + "Time        : " + nullSafeStr(a.time)          + "\n"
                + "Plate No.   : " + nullSafeStr(a.carPlate)      + "\n"
                + String.format("Total       : \u20b1%.2f", a.totalPrice)
                + (a.adminNote != null && !a.adminNote.isEmpty()
                ? "\n\nAdmin Note  : " + a.adminNote : "")
                + "\n\nA receipt has also been sent to your registered email.";
    }

    private String nullSafeStr(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }

    // =========================================================================
    // PANEL 3 — SERVICES
    // =========================================================================
    private void loadPanelServices() {
        List<Service> services = ServiceData.getAll();
        for (Service s : services) {
            android.database.Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT active FROM admin_services WHERE id=?", new String[]{String.valueOf(s.id)});
            s.active = (!c.moveToFirst()) || (c.getInt(0) == 1);
            c.close();
        }

        // Stats strip
        int available   = (int) services.stream().filter(s -> s.active).count();
        int unavailable = services.size() - available;
        if (tvSvcStatsAvailable   != null) tvSvcStatsAvailable.setText(String.valueOf(available));
        if (tvSvcStatsUnavailable != null) tvSvcStatsUnavailable.setText(String.valueOf(unavailable));
        if (tvSvcStatsTotal       != null) tvSvcStatsTotal.setText(String.valueOf(services.size()));

        if (services.isEmpty()) {
            rvServices.setVisibility(View.GONE);
            tvNoServices.setVisibility(View.VISIBLE);
        } else {
            tvNoServices.setVisibility(View.GONE);
            rvServices.setVisibility(View.VISIBLE);
            rvServices.setLayoutManager(new LinearLayoutManager(this));
            rvServices.setAdapter(new SvcRvAdapter(services));
        }
    }

    private void showAddServiceDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null);
        new AlertDialog.Builder(this)
                .setTitle("Add New Service")
                .setView(v)
                .setPositiveButton("Add", (dlg, w) -> {
                    String name     = ((EditText) v.findViewById(R.id.etSvcName)).getText().toString().trim();
                    String cat      = ((EditText) v.findViewById(R.id.etSvcCategory)).getText().toString().trim();
                    String priceStr = ((EditText) v.findViewById(R.id.etSvcPrice)).getText().toString().trim();
                    String durStr   = ((EditText) v.findViewById(R.id.etSvcDuration)).getText().toString().trim();
                    String desc     = ((EditText) v.findViewById(R.id.etSvcDesc)).getText().toString().trim();
                    if (name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name, category and price are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double price = Double.parseDouble(priceStr);
                    double dur   = durStr.isEmpty() ? 1.0 : Double.parseDouble(durStr);
                    int newId    = ServiceData.getAll().stream().mapToInt(s2 -> s2.id).max().orElse(0) + 1;
                    Service ns   = new Service(newId, name, desc, cat, price, dur);
                    ServiceData.addService(ns);
                    ContentValues cv = new ContentValues();
                    cv.put("id", newId); cv.put("active", 1);
                    cv.put("name", name); cv.put("description", desc);
                    cv.put("category", cat); cv.put("price", price); cv.put("duration_hr", dur);
                    db.getWritableDatabase().insertWithOnConflict("admin_services", null, cv,
                            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                    Toast.makeText(this, "Service \"" + name + "\" added!", Toast.LENGTH_SHORT).show();
                    loadPanelServices();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private class SvcRvAdapter extends RecyclerView.Adapter<SvcRvAdapter.VH> {
        private final List<Service> data;
        SvcRvAdapter(List<Service> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_admin_service, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) { h.bind(data.get(pos)); }
        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }

            void bind(Service s) {
                ((TextView) itemView.findViewById(R.id.tvSvcName)).setText(s.name);
                ((TextView) itemView.findViewById(R.id.tvSvcCat)).setText(s.category);
                ((TextView) itemView.findViewById(R.id.tvSvcPrice)).setText(
                        String.format("₱%.2f", s.price));
                TextView tvDur = itemView.findViewById(R.id.tvSvcDur);
                if (tvDur != null) tvDur.setText(s.durationHr + " hr" + (s.durationHr != 1 ? "s" : ""));

                TextView tvStatus = itemView.findViewById(R.id.tvSvcStatus);
                if (tvStatus != null) {
                    if (s.active) {
                        tvStatus.setText("AVAILABLE");
                        tvStatus.setTextColor(0xFF4CAF7D);
                    } else {
                        tvStatus.setText("UNAVAILABLE");
                        tvStatus.setTextColor(0xFFE05252);
                    }
                }
                itemView.setAlpha(s.active ? 1f : 0.6f);

                Button btnToggle = itemView.findViewById(R.id.btnToggleService);
                if (btnToggle != null) {
                    btnToggle.setText(s.active ? "Set Unavailable" : "Set Available");
                    btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            s.active ? 0xFFE05252 : 0xFF4CAF7D));
                    btnToggle.setOnClickListener(v -> {
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put("id", s.id);
                        cv.put("active", s.active ? 0 : 1);
                        db.getWritableDatabase().insertWithOnConflict("admin_services", null, cv,
                                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                        loadPanelServices();
                    });
                }

                Button btnEdit = itemView.findViewById(R.id.btnEditService);
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> showEditServiceDialog(s));
                }
            }
        }
    }

    private void showEditServiceDialog(Service s) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null);
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
        new AlertDialog.Builder(this)
                .setTitle("Edit Service")
                .setView(v)
                .setPositiveButton("Save Changes", (dlg, w) -> {
                    String name     = etName.getText().toString().trim();
                    String cat      = etCat.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    String durStr   = etDur.getText().toString().trim();
                    String desc     = etDesc.getText().toString().trim();
                    if (name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name, category, and price required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    s.name        = name;
                    s.category    = cat;
                    s.price       = Double.parseDouble(priceStr);
                    s.durationHr  = durStr.isEmpty() ? s.durationHr : Double.parseDouble(durStr);
                    s.description = desc;
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("id", s.id); cv.put("active", s.active ? 1 : 0);
                    cv.put("name", s.name); cv.put("description", s.description);
                    cv.put("category", s.category); cv.put("price", s.price);
                    cv.put("duration_hr", s.durationHr);
                    db.getWritableDatabase().insertWithOnConflict("admin_services", null, cv,
                            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                    Toast.makeText(this, "Service updated!", Toast.LENGTH_SHORT).show();
                    loadPanelServices();
                })
                .setNegativeButton("Cancel", null).show();
    }

    // =========================================================================
    // PANEL 4 — REPORTS
    // =========================================================================
    private void loadPanelReports() {
        Map<String, Integer> counts = db.getStatusCounts();
        int total     = db.countByStatus("all");
        int completed = counts.getOrDefault("completed", 0);
        int pending   = counts.getOrDefault("pending",   0);
        int confirmed = counts.getOrDefault("confirmed", 0);
        int declined  = counts.getOrDefault("declined",  0);
        int cancelled = counts.getOrDefault("cancelled", 0);

        tvReportTotalAppts.setText(String.valueOf(total));
        tvReportCompleted.setText(String.valueOf(completed));
        tvReportPending.setText(String.valueOf(pending));
        tvReportDeclined.setText(String.valueOf(declined));
        if (tvReportConfirmed != null) tvReportConfirmed.setText(String.valueOf(confirmed));
        if (tvReportCancelled != null) tvReportCancelled.setText(String.valueOf(cancelled));

        // Completion rate bar
        int doneOrCancel = completed + declined + cancelled;
        float rate = doneOrCancel > 0 ? (float) completed / doneOrCancel * 100f : 0f;
        if (tvCompletionRate != null)
            tvCompletionRate.setText(String.format("%.0f%% Completion Rate  (%d completed / %d closed)",
                    rate, completed, doneOrCancel));
        if (completionBarBg != null && completionBarFill != null) {
            completionBarBg.post(() -> {
                int fullWidth = completionBarBg.getWidth();
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) completionBarFill.getLayoutParams();
                lp.width = (int) (fullWidth * rate / 100f);
                completionBarFill.setLayoutParams(lp);
            });
        }

        // Top services horizontal bar chart
        loadReportsTopServices();

        // Daily activity
        loadReportsDailyActivity();
    }

    private void loadReportsTopServices() {
        if (topServicesContainer == null) return;
        List<String[]> top = db.getTopServices(8);
        topServicesContainer.removeAllViews();
        if (top.isEmpty()) {
            if (tvNoReportData != null) tvNoReportData.setVisibility(View.VISIBLE);
            return;
        }
        if (tvNoReportData != null) tvNoReportData.setVisibility(View.GONE);

        int maxCount = 1;
        for (String[] row : top) maxCount = Math.max(maxCount, Integer.parseInt(row[1]));

        for (String[] row : top) {
            String name    = row[0];
            int    count   = Integer.parseInt(row[1]);
            double revenue = row.length > 2 ? Double.parseDouble(row[2]) : 0;

            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dpToPx(12));
            rowView.setLayoutParams(rowLp);

            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvName2 = new TextView(this);
            tvName2.setText(name);
            tvName2.setTextColor(Color.WHITE);
            tvName2.setTextSize(14);
            tvName2.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvCount2 = new TextView(this);
            tvCount2.setText(count + " booking" + (count != 1 ? "s" : "") +
                    (revenue > 0 ? " · ₱" + String.format("%,.0f", revenue) : ""));
            tvCount2.setTextColor(0xFFF5A623);
            tvCount2.setTextSize(12);
            labelRow.addView(tvName2);
            labelRow.addView(tvCount2);
            rowView.addView(labelRow);

            LinearLayout barBg2 = new LinearLayout(this);
            barBg2.setBackgroundColor(0xFF1A1A1A);
            LinearLayout.LayoutParams bgLp2 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
            bgLp2.setMargins(0, dpToPx(6), 0, 0);
            barBg2.setLayoutParams(bgLp2);
            LinearLayout barFill2 = new LinearLayout(this);
            barFill2.setBackgroundColor(0xFFF5A623);
            final int mc = maxCount, cnt2 = count;
            barBg2.post(() -> {
                int barW = (int) ((float) cnt2 / mc * barBg2.getWidth());
                barFill2.setLayoutParams(new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT));
            });
            barBg2.addView(barFill2);
            rowView.addView(barBg2);
            topServicesContainer.addView(rowView);
        }
    }

    private void loadReportsDailyActivity() {
        if (dailyActivityContainer == null) return;
        List<String[]> daily = db.getDailyBookings();
        dailyActivityContainer.removeAllViews();
        if (daily.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No booking history yet.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(13);
            dailyActivityContainer.addView(empty);
            return;
        }
        int maxCount = 1;
        for (String[] row : daily) maxCount = Math.max(maxCount, Integer.parseInt(row[1]));

        for (String[] row : daily) {
            String date = row[0];
            int    count = Integer.parseInt(row[1]);

            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(32));
            rowLp.setMargins(0, 0, 0, dpToPx(6));
            rowView.setLayoutParams(rowLp);

            TextView tvDate = new TextView(this);
            tvDate.setText(date);
            tvDate.setTextColor(0xFF888888);
            tvDate.setTextSize(12);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(100), LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout barBg3 = new LinearLayout(this);
            barBg3.setBackgroundColor(0xFF1A1A1A);
            LinearLayout.LayoutParams bgLp3 = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            bgLp3.setMargins(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));
            barBg3.setLayoutParams(bgLp3);

            final int maxC3 = maxCount, cnt3 = count;
            LinearLayout barFill3 = new LinearLayout(this);
            barFill3.setBackgroundColor(0xFF7B61FF);
            barBg3.post(() -> {
                int barW = (int) ((float) cnt3 / maxC3 * barBg3.getWidth());
                barFill3.setLayoutParams(new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT));
            });
            barBg3.addView(barFill3);

            TextView tvCount3 = new TextView(this);
            tvCount3.setText(String.valueOf(count));
            tvCount3.setTextColor(Color.WHITE);
            tvCount3.setTextSize(13);
            tvCount3.setGravity(android.view.Gravity.CENTER_VERTICAL);

            rowView.addView(tvDate);
            rowView.addView(barBg3);
            rowView.addView(tvCount3);
            dailyActivityContainer.addView(rowView);
        }
    }

    // =========================================================================
    // PANEL 5 — PMS (Repair Tracker)
    // =========================================================================
    private void loadPanelPms() {
        List<Appointment> all = db.getAllAppointments("confirmed");

        // Compute summary stats across the full unfiltered list
        int statsDone = 0, statsInProgress = 0, statsQueued = 0;
        for (Appointment a : all) {
            List<DatabaseHelper.RepairTask> tasks = db.getTasksForAppointment(a.id);
            if (tasks.isEmpty()) { statsQueued++; continue; }
            long doneCount = tasks.stream().filter(t -> "completed".equals(t.status)).count();
            if (doneCount == tasks.size()) statsDone++;
            else if (doneCount > 0) statsInProgress++;
            else statsQueued++;
        }
        if (tvPmsStatsActive     != null) tvPmsStatsActive.setText(String.valueOf(all.size()));
        if (tvPmsStatsDone       != null) tvPmsStatsDone.setText(String.valueOf(statsDone));
        if (tvPmsStatsInProgress != null) tvPmsStatsInProgress.setText(String.valueOf(statsInProgress));
        if (tvPmsStatsQueued     != null) tvPmsStatsQueued.setText(String.valueOf(statsQueued));

        // Search filter
        if (!pmsSearchQuery.isEmpty()) {
            String q = pmsSearchQuery.toLowerCase(Locale.getDefault());
            all = all.stream().filter(a ->
                    (a.carPlate    != null && a.carPlate.toLowerCase(Locale.getDefault()).contains(q)) ||
                            (a.customerName != null && a.customerName.toLowerCase(Locale.getDefault()).contains(q))
            ).collect(Collectors.toList());
        }

        // Status chip filter
        if (!"all".equals(pmsStatusFilter)) {
            List<Appointment> filtered = new ArrayList<>();
            for (Appointment a : all) {
                List<DatabaseHelper.RepairTask> tasks = db.getTasksForAppointment(a.id);
                if ("done".equals(pmsStatusFilter)) {
                    if (!tasks.isEmpty() && tasks.stream().allMatch(t -> "completed".equals(t.status)))
                        filtered.add(a);
                } else if ("in_progress".equals(pmsStatusFilter)) {
                    long done = tasks.stream().filter(t -> "completed".equals(t.status)).count();
                    if (!tasks.isEmpty() && done > 0 && done < tasks.size())
                        filtered.add(a);
                }
            }
            all = filtered;
        }

        if (all.isEmpty()) {
            rvPmsRecords.setVisibility(View.GONE);
            tvNoPmsRecords.setVisibility(View.VISIBLE);
        } else {
            tvNoPmsRecords.setVisibility(View.GONE);
            rvPmsRecords.setVisibility(View.VISIBLE);
            rvPmsRecords.setLayoutManager(new LinearLayoutManager(this));
            rvPmsRecords.setAdapter(new PmsRvAdapter(all));
        }
    }

    private void updatePmsChips() {
        TextView chipAll        = findViewById(R.id.pmsChipAll);
        TextView chipInProgress = findViewById(R.id.pmsChipInProgress);
        TextView chipDone       = findViewById(R.id.pmsChipDone);
        resetChipStyle2(chipAll); resetChipStyle2(chipInProgress); resetChipStyle2(chipDone);
        TextView active;
        switch (pmsStatusFilter) {
            case "in_progress": active = chipInProgress; break;
            case "done":        active = chipDone;        break;
            default:            active = chipAll;          break;
        }
        active.setBackgroundColor(getResources().getColor(R.color.yellow, null));
        active.setTextColor(getResources().getColor(R.color.black, null));
        active.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetChipStyle2(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_form_card);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    /**
     * Panel 5 RecyclerView adapter — full port of AdminPmsActivity.buildJobCard() +
     * renderTasks().  Adds: assigned-to sub-label, in_progress dot colour (orange),
     * delete (✕) button per task, "No tasks yet" empty state per card.
     */
    private class PmsRvAdapter extends RecyclerView.Adapter<PmsRvAdapter.VH> {
        private final List<Appointment> jobs;
        PmsRvAdapter(List<Appointment> jobs) { this.jobs = jobs; }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int vt) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFF1A1A1A);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(4));
            card.setLayoutParams(lp);
            card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            return new VH(card);
        }

        @Override public void onBindViewHolder(VH h, int pos) { h.bind(jobs.get(pos)); }
        @Override public int getItemCount() { return jobs.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }

            void bind(Appointment job) {
                LinearLayout card = (LinearLayout) itemView;
                card.removeAllViews();

                List<DatabaseHelper.RepairTask> tasks = db.getTasksForAppointment(job.id);
                int total = tasks.size();
                long done = tasks.stream().filter(t -> "completed".equals(t.status)).count();

                // ── Job header ────────────────────────────────────────────────
                TextView tvHeader = new TextView(AdminDashboardActivity.this);
                tvHeader.setText("Job #" + job.id + "  ·  " + job.serviceName);
                tvHeader.setTextColor(0xFFF5A623);
                tvHeader.setTextSize(15);
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                card.addView(tvHeader);

                // ── Customer + date ───────────────────────────────────────────
                TextView tvCustomer = new TextView(AdminDashboardActivity.this);
                String custName = (job.customerName != null && !job.customerName.trim().isEmpty())
                        ? job.customerName : "Unknown Customer";
                tvCustomer.setText(custName + "  ·  " + job.date + " " + job.time);
                tvCustomer.setTextColor(0xFF888888);
                tvCustomer.setTextSize(12);
                LinearLayout.LayoutParams custLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                custLp.setMargins(0, dpToPx(2), 0, dpToPx(8));
                tvCustomer.setLayoutParams(custLp);
                card.addView(tvCustomer);

                // ── Plate ─────────────────────────────────────────────────────
                if (job.carPlate != null && !job.carPlate.isEmpty()) {
                    TextView tvPlate = new TextView(AdminDashboardActivity.this);
                    tvPlate.setText("🚗 " + job.carPlate);
                    tvPlate.setTextColor(0xFFCCCCCC);
                    tvPlate.setTextSize(13);
                    LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    pLp.setMargins(0, 0, 0, dpToPx(10));
                    tvPlate.setLayoutParams(pLp);
                    card.addView(tvPlate);
                }

                // ── Progress bar ──────────────────────────────────────────────
                if (total > 0) {
                    TextView tvProg = new TextView(AdminDashboardActivity.this);
                    tvProg.setText(done + " / " + total + " tasks completed");
                    tvProg.setTextColor(0xFF888888);
                    tvProg.setTextSize(12);
                    card.addView(tvProg);

                    LinearLayout barBg = new LinearLayout(AdminDashboardActivity.this);
                    barBg.setBackgroundColor(0xFF2A2A2A);
                    LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6));
                    bgLp.setMargins(0, dpToPx(4), 0, dpToPx(12));
                    barBg.setLayoutParams(bgLp);
                    final float pct = (float) done / total;
                    LinearLayout barFill = new LinearLayout(AdminDashboardActivity.this);
                    barFill.setBackgroundColor(0xFF4CAF7D);
                    barBg.post(() -> {
                        int w = (int) (barBg.getWidth() * pct);
                        barFill.setLayoutParams(new LinearLayout.LayoutParams(
                                w, LinearLayout.LayoutParams.MATCH_PARENT));
                    });
                    barBg.addView(barFill);
                    card.addView(barBg);
                }

                // ── Section divider ───────────────────────────────────────────
                View div = new View(AdminDashboardActivity.this);
                div.setBackgroundColor(0xFF222222);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divLp.setMargins(0, 0, 0, dpToPx(10));
                div.setLayoutParams(divLp);
                card.addView(div);

                // ── Task rows ─────────────────────────────────────────────────
                if (tasks.isEmpty()) {
                    TextView tvEmpty = new TextView(AdminDashboardActivity.this);
                    tvEmpty.setText("No tasks yet. Add tasks to track repair progress.");
                    tvEmpty.setTextColor(0xFF666666);
                    tvEmpty.setTextSize(12);
                    card.addView(tvEmpty);
                } else {
                    for (DatabaseHelper.RepairTask task : tasks) {
                        LinearLayout row = new LinearLayout(AdminDashboardActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        rowLp.setMargins(0, 0, 0, dpToPx(8));
                        row.setLayoutParams(rowLp);

                        // Status dot — green=completed, orange=in_progress, grey=pending
                        View dot = new View(AdminDashboardActivity.this);
                        int dotColor = "completed".equals(task.status) ? 0xFF4CAF7D
                                : "in_progress".equals(task.status) ? 0xFFF5A623
                                : 0xFF888888;
                        dot.setBackgroundColor(dotColor);
                        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                                dpToPx(8), dpToPx(8));
                        dotLp.setMargins(0, 0, dpToPx(10), 0);
                        dot.setLayoutParams(dotLp);
                        row.addView(dot);

                        // Task name + assigned-to sub-label (stacked)
                        LinearLayout textCol = new LinearLayout(AdminDashboardActivity.this);
                        textCol.setOrientation(LinearLayout.VERTICAL);
                        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                        TextView tvTask = new TextView(AdminDashboardActivity.this);
                        tvTask.setText(task.taskName);
                        tvTask.setTextColor("completed".equals(task.status) ? 0xFF555555 : 0xFFCCCCCC);
                        tvTask.setTextSize(13);
                        if ("completed".equals(task.status))
                            tvTask.setPaintFlags(tvTask.getPaintFlags() |
                                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        textCol.addView(tvTask);

                        if (task.assignedTo != null && !task.assignedTo.isEmpty()) {
                            TextView tvAssigned = new TextView(AdminDashboardActivity.this);
                            tvAssigned.setText("Assigned: " + task.assignedTo);
                            tvAssigned.setTextColor(0xFF666666);
                            tvAssigned.setTextSize(11);
                            textCol.addView(tvAssigned);
                        }
                        row.addView(textCol);

                        // Done / Undo toggle
                        Button btnToggle = new Button(AdminDashboardActivity.this);
                        boolean isDone = "completed".equals(task.status);
                        btnToggle.setText(isDone ? "Undo" : "Done");
                        btnToggle.setTextColor(isDone ? 0xFF888888 : 0xFF4CAF7D);
                        btnToggle.setBackgroundColor(0xFF222222);
                        btnToggle.setTextSize(11);
                        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                                dpToPx(60), LinearLayout.LayoutParams.WRAP_CONTENT);
                        toggleLp.setMargins(dpToPx(6), 0, 0, 0);
                        btnToggle.setLayoutParams(toggleLp);
                        final String newStatus = isDone ? "pending" : "completed";
                        btnToggle.setOnClickListener(v -> {
                            db.updateTaskStatus(task.id, newStatus);
                            loadPanelPms();
                        });
                        row.addView(btnToggle);

                        // Delete (✕) button
                        Button btnDel = new Button(AdminDashboardActivity.this);
                        btnDel.setText("✕");
                        btnDel.setTextColor(0xFFE05252);
                        btnDel.setBackgroundColor(0xFF1A1A1A);
                        btnDel.setTextSize(12);
                        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                                dpToPx(40), LinearLayout.LayoutParams.WRAP_CONTENT);
                        delLp.setMargins(dpToPx(4), 0, 0, 0);
                        btnDel.setLayoutParams(delLp);
                        btnDel.setOnClickListener(v -> {
                            db.deleteRepairTask(task.id);
                            loadPanelPms();
                        });
                        row.addView(btnDel);

                        card.addView(row);
                    }
                }

                // ── Add Task button ───────────────────────────────────────────
                Button btnAddTask = new Button(AdminDashboardActivity.this);
                btnAddTask.setText("+ Add Task");
                btnAddTask.setTextColor(0xFFF5A623);
                btnAddTask.setBackgroundColor(0xFF222222);
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnLp.setMargins(0, dpToPx(8), 0, 0);
                btnAddTask.setLayoutParams(btnLp);
                btnAddTask.setOnClickListener(v -> showAddTaskDialog(job.id));
                card.addView(btnAddTask);
            }
        }
    }

    private void showAddTaskDialog(int jobId) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0);
        EditText etName = new EditText(this);
        etName.setHint("Task name (e.g. Replace brake pads)");
        etName.setTextColor(0xFFFFFFFF); etName.setHintTextColor(0xFF888888);
        layout.addView(etName);
        EditText etAssigned = new EditText(this);
        etAssigned.setHint("Assigned mechanic (optional)");
        etAssigned.setTextColor(0xFFFFFFFF); etAssigned.setHintTextColor(0xFF888888);
        layout.addView(etAssigned);
        new AlertDialog.Builder(this)
                .setTitle("Add Repair Task")
                .setView(layout)
                .setPositiveButton("Add Task", (dlg, w) -> {
                    String taskName = etName.getText().toString().trim();
                    String assigned = etAssigned.getText().toString().trim();
                    if (taskName.isEmpty()) {
                        Toast.makeText(this, "Task name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.insertRepairTask(jobId, taskName, assigned.isEmpty() ? null : assigned);
                    Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
                    loadPanelPms();
                })
                .setNegativeButton("Cancel", null).show();
    }

    // =========================================================================
    // PANEL 6 — VERIFICATIONS
    // =========================================================================
    private void loadPanelVerifications() {
        // Pending count badge (always reflects true pending count)
        int pendingCount = db.countPendingVerifications();
        if (tvVerifPendingCount != null) tvVerifPendingCount.setText(String.valueOf(pendingCount));

        // Bottom nav badge
        View navBadge = findViewById(R.id.navAdminVerifBadge);
        if (navBadge instanceof TextView) {
            if (pendingCount > 0) {
                ((TextView) navBadge).setText(pendingCount > 99 ? "99+" : String.valueOf(pendingCount));
                navBadge.setVisibility(View.VISIBLE);
            } else {
                navBadge.setVisibility(View.GONE);
            }
        }

        // Load list
        // Both methods now exclude the heavy image columns from the list query
        // to prevent Android's 2 MB CursorWindow limit from silently nulling
        // Base64 image strings (the "Image unavailable" bug).
        // Images are loaded lazily per-user when the review dialog is opened.
        List<User> users;
        if (verifShowAll) {
            users = db.getAllVerificationUsers();
        } else {
            users = db.getPendingVerificationUsers();
        }

        if (users.isEmpty()) {
            rvVerifications.setVisibility(View.GONE);
            tvNoVerifications.setVisibility(View.VISIBLE);
        } else {
            tvNoVerifications.setVisibility(View.GONE);
            rvVerifications.setVisibility(View.VISIBLE);
            rvVerifications.setLayoutManager(new LinearLayoutManager(this));
            rvVerifications.setAdapter(new VerificationAdapter(
                    this, users, db, this::loadPanelVerifications));
        }
    }

    private void updateVerifChips() {
        TextView chipPending = findViewById(R.id.verifChipPending);
        TextView chipAll     = findViewById(R.id.verifChipAll);
        if (verifShowAll) {
            chipAll.setBackgroundColor(getResources().getColor(R.color.yellow, null));
            chipAll.setTextColor(getResources().getColor(R.color.black, null));
            chipAll.setTypeface(null, android.graphics.Typeface.BOLD);
            chipPending.setBackgroundResource(R.drawable.bg_form_card);
            chipPending.setTextColor(getResources().getColor(R.color.white, null));
            chipPending.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            chipPending.setBackgroundColor(getResources().getColor(R.color.yellow, null));
            chipPending.setTextColor(getResources().getColor(R.color.black, null));
            chipPending.setTypeface(null, android.graphics.Typeface.BOLD);
            chipAll.setBackgroundResource(R.drawable.bg_form_card);
            chipAll.setTextColor(getResources().getColor(R.color.white, null));
            chipAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    // =========================================================================
    // ADMIN NOTIFICATION DIALOG
    // =========================================================================

    /**
     * Opens a scrollable list dialog showing all in-app notifications addressed to
     * the admin account.  Each row is prefixed with a type icon:
     *   👤  Registration alert  (apptId == 0)
     *   📅  Booking / appointment event  (apptId > 0)
     * Unread items are additionally prefixed with a blue dot (🔵).
     * Tapping a row opens the full message and marks it read.
     * "Mark All Read" clears the badge instantly.
     */
    private void openAdminNotificationsDialog() {
        if (adminUserId == -1) return;

        List<DatabaseHelper.AppNotification> notifications =
                db.getNotificationsForUser(adminUserId);

        if (notifications.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Admin Notifications")
                    .setMessage("No notifications yet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] items = new String[notifications.size()];
        for (int i = 0; i < notifications.size(); i++) {
            DatabaseHelper.AppNotification n = notifications.get(i);
            // Type icon: registration (apptId == 0) vs booking/appointment (apptId > 0)
            String typeIcon = (n.apptId == 0) ? "\uD83D\uDC64 " : "\uD83D\uDCC5 ";
            // Unread dot prepended before type icon
            String unreadDot = n.isRead ? "   " : "\uD83D\uDD35 ";
            items[i] = unreadDot + typeIcon + n.title + "\n      " + n.createdAt;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Admin Notifications")
                .setItems(items, (dialog, which) ->
                        openAdminNotificationDetail(notifications.get(which)))
                .setNeutralButton("Mark All Read", (d, w) -> {
                    db.markAllNotificationsRead(adminUserId);
                    refreshAdminNotifBadge();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Opens a detail dialog for a single admin notification, marks it read,
     * and refreshes the badge count.
     *
     * @param notif The notification to display.
     */
    private void openAdminNotificationDetail(DatabaseHelper.AppNotification notif) {
        db.markNotificationRead(notif.id);
        refreshAdminNotifBadge();

        // Type label for the dialog title prefix
        String typeLabel = (notif.apptId == 0) ? "\uD83D\uDC64 " : "\uD83D\uDCC5 ";

        new android.app.AlertDialog.Builder(this)
                .setTitle(typeLabel + notif.title)
                .setMessage(notif.message)
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /** Sizes a nested ListView to fit all its children (avoids scrolling inside a ScrollView). */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        if (listView.getAdapter() == null) return;
        int totalHeight = 0;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            View item = listView.getAdapter().getView(i, null, listView);
            item.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += item.getMeasuredHeight();
        }
        android.view.ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight +
                (listView.getDividerHeight() * (listView.getAdapter().getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}