package com.maestro.autoworks.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminPmsActivity — Repair Tracker (mirrors admin_pms.php).
 * Lists all confirmed appointments on the shop floor.
 * For each job, mechanics can add/complete/delete repair tasks.
 */
public class AdminPmsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private LinearLayout jobsContainer;
    private TextView tvStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_pms);

        db = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        if (!session.isAdmin()) { finish(); return; }

        jobsContainer = findViewById(R.id.pmsJobsContainer);
        tvStats       = findViewById(R.id.pmsSummaryStats);

        findViewById(R.id.btnPmsBack).setOnClickListener(v -> finish());
        loadJobs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJobs();
    }

    private void loadJobs() {
        jobsContainer.removeAllViews();
        List<Appointment> jobs = db.getAllAppointments("confirmed");

        // Stats
        int done = 0, active = 0, queued = 0;
        for (Appointment job : jobs) {
            List<DatabaseHelper.RepairTask> tasks = db.getTasksForAppointment(job.id);
            if (tasks.isEmpty()) { queued++; continue; }
            long doneCount = tasks.stream().filter(t -> "completed".equals(t.status)).count();
            if (doneCount == tasks.size()) done++;
            else if (doneCount > 0) active++;
            else queued++;
        }

        tvStats.setText("🔧 " + jobs.size() + " Active Jobs  ·  ✅ " + done + " Done  ·  ⚙️ " + active + " In Progress  ·  🕐 " + queued + " Queued");

        if (jobs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No confirmed jobs currently on the floor.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(14);
            empty.setPadding(0, dpToPx(32), 0, 0);
            empty.setGravity(android.view.Gravity.CENTER);
            jobsContainer.addView(empty);
            return;
        }

        for (Appointment job : jobs) {
            jobsContainer.addView(buildJobCard(job));
        }
    }

    private View buildJobCard(Appointment job) {
        List<DatabaseHelper.RepairTask> tasks = db.getTasksForAppointment(job.id);
        int totalTasks = tasks.size();
        long doneTasks = tasks.stream().filter(t -> "completed".equals(t.status)).count();

        // Card background
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1A1A1A);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dpToPx(16));
        card.setLayoutParams(cardLp);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Job header
        TextView tvJobHeader = new TextView(this);
        tvJobHeader.setText("Job #" + job.id + "  ·  " + job.serviceName);
        tvJobHeader.setTextColor(0xFFF5A623);
        tvJobHeader.setTextSize(15);
        tvJobHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvJobHeader);

        // Customer + date
        TextView tvCustomer = new TextView(this);
        String customerName = (job.customerName != null && !job.customerName.trim().isEmpty())
            ? job.customerName : "Unknown Customer";
        tvCustomer.setText(customerName + "  ·  " + job.date + " " + job.time);
        tvCustomer.setTextColor(0xFF888888);
        tvCustomer.setTextSize(12);
        LinearLayout.LayoutParams custLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        custLp.setMargins(0, dpToPx(2), 0, dpToPx(8));
        tvCustomer.setLayoutParams(custLp);
        card.addView(tvCustomer);

        // Vehicle / plate
        if (job.carPlate != null && !job.carPlate.isEmpty()) {
            TextView tvPlate = new TextView(this);
            tvPlate.setText("🚗 " + job.carPlate);
            tvPlate.setTextColor(0xFFCCCCCC);
            tvPlate.setTextSize(13);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pLp.setMargins(0, 0, 0, dpToPx(10));
            tvPlate.setLayoutParams(pLp);
            card.addView(tvPlate);
        }

        // Progress bar
        if (totalTasks > 0) {
            TextView tvProgress = new TextView(this);
            tvProgress.setText(doneTasks + " / " + totalTasks + " tasks completed");
            tvProgress.setTextColor(0xFF888888);
            tvProgress.setTextSize(12);
            card.addView(tvProgress);

            LinearLayout barBg = new LinearLayout(this);
            barBg.setBackgroundColor(0xFF2A2A2A);
            LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6));
            bgLp.setMargins(0, dpToPx(4), 0, dpToPx(12));
            barBg.setLayoutParams(bgLp);

            LinearLayout barFill = new LinearLayout(this);
            barFill.setBackgroundColor(0xFF4CAF7D);
            float pct = totalTasks > 0 ? (float) doneTasks / totalTasks : 0f;
            barBg.post(() -> {
                int w = (int) (barBg.getWidth() * pct);
                barFill.setLayoutParams(new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.MATCH_PARENT));
            });
            barBg.addView(barFill);
            card.addView(barBg);
        }

        // Divider
        View div = new View(this);
        div.setBackgroundColor(0xFF222222);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(0, 0, 0, dpToPx(10));
        div.setLayoutParams(divLp);
        card.addView(div);

        // Tasks list
        LinearLayout tasksContainer = new LinearLayout(this);
        tasksContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(tasksContainer);

        renderTasks(tasksContainer, tasks, job.id, card);

        // Add Task button
        Button btnAddTask = new Button(this);
        btnAddTask.setText("+ Add Task");
        btnAddTask.setTextColor(0xFFF5A623);
        btnAddTask.setBackgroundColor(0xFF222222);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, dpToPx(8), 0, 0);
        btnAddTask.setLayoutParams(btnLp);
        btnAddTask.setOnClickListener(v -> showAddTaskDialog(job.id, card));
        card.addView(btnAddTask);

        return card;
    }

    private void renderTasks(LinearLayout container, List<DatabaseHelper.RepairTask> tasks,
                              int jobId, LinearLayout parentCard) {
        container.removeAllViews();
        if (tasks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No tasks yet. Add tasks to track repair progress.");
            empty.setTextColor(0xFF666666);
            empty.setTextSize(12);
            container.addView(empty);
            return;
        }

        for (DatabaseHelper.RepairTask task : tasks) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dpToPx(8));
            row.setLayoutParams(rowLp);

            // Status dot
            View dot = new View(this);
            int dotColor = "completed".equals(task.status) ? 0xFF4CAF7D :
                           "in_progress".equals(task.status) ? 0xFFF5A623 : 0xFF888888;
            dot.setBackgroundColor(dotColor);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            dotLp.setMargins(0, 0, dpToPx(10), 0);
            dot.setLayoutParams(dotLp);
            row.addView(dot);

            // Task name + assigned
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(task.taskName);
            tvName.setTextColor("completed".equals(task.status) ? 0xFF555555 : 0xFFCCCCCC);
            tvName.setTextSize(13);
            if ("completed".equals(task.status)) {
                tvName.setPaintFlags(tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
            textCol.addView(tvName);

            if (task.assignedTo != null && !task.assignedTo.isEmpty()) {
                TextView tvAssigned = new TextView(this);
                tvAssigned.setText("Assigned: " + task.assignedTo);
                tvAssigned.setTextColor(0xFF666666);
                tvAssigned.setTextSize(11);
                textCol.addView(tvAssigned);
            }
            row.addView(textCol);

            // Toggle status button
            Button btnToggle = new Button(this);
            btnToggle.setText("completed".equals(task.status) ? "Undo" : "Done");
            btnToggle.setTextColor("completed".equals(task.status) ? 0xFF888888 : 0xFF4CAF7D);
            btnToggle.setBackgroundColor(0xFF222222);
            btnToggle.setTextSize(11);
            LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                dpToPx(60), LinearLayout.LayoutParams.WRAP_CONTENT);
            toggleLp.setMargins(dpToPx(6), 0, 0, 0);
            btnToggle.setLayoutParams(toggleLp);
            final String newStatus = "completed".equals(task.status) ? "pending" : "completed";
            btnToggle.setOnClickListener(v -> {
                db.updateTaskStatus(task.id, newStatus);
                loadJobs();
            });
            row.addView(btnToggle);

            // Delete button
            Button btnDel = new Button(this);
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
                loadJobs();
            });
            row.addView(btnDel);

            container.addView(row);
        }
    }

    private void showAddTaskDialog(int jobId, LinearLayout card) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Repair Task");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), 0);

        EditText etTaskName   = new EditText(this);
        etTaskName.setHint("Task name (e.g. Replace brake pads)");
        etTaskName.setTextColor(0xFFFFFFFF);
        etTaskName.setHintTextColor(0xFF888888);
        dialogLayout.addView(etTaskName);

        EditText etAssigned   = new EditText(this);
        etAssigned.setHint("Assigned mechanic (optional)");
        etAssigned.setTextColor(0xFFFFFFFF);
        etAssigned.setHintTextColor(0xFF888888);
        dialogLayout.addView(etAssigned);

        builder.setView(dialogLayout);
        builder.setPositiveButton("Add Task", (dialog, which) -> {
            String name     = etTaskName.getText().toString().trim();
            String assigned = etAssigned.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Task name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            db.insertRepairTask(jobId, name, assigned.isEmpty() ? null : assigned);
            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
            loadJobs();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
