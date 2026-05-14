package com.maestro.autoworks.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.SessionManager;

import java.util.List;
import java.util.Map;

/**
 * AdminReportsActivity — mirrors admin_reports.php.
 * Shows: status summary tiles, top services by bookings,
 * recent daily booking counts, and a completion rate indicator.
 */
public class AdminReportsActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        db = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        if (!session.isAdmin()) { finish(); return; }

        findViewById(R.id.btnReportsBack).setOnClickListener(v -> finish());

        loadSummary();
        loadTopServices();
        loadDailyActivity();
    }

    private void loadSummary() {
        Map<String, Integer> counts = db.getStatusCounts();
        int total     = db.countByStatus("all");
        int completed = counts.getOrDefault("completed", 0);
        int pending   = counts.getOrDefault("pending",   0);
        int confirmed = counts.getOrDefault("confirmed", 0);
        int declined  = counts.getOrDefault("declined",  0);
        int cancelled = counts.getOrDefault("cancelled", 0);

        setTile(R.id.rptTotal,     total,     "All Bookings");
        setTile(R.id.rptPending,   pending,   "Pending");
        setTile(R.id.rptConfirmed, confirmed, "Confirmed");
        setTile(R.id.rptCompleted, completed, "Completed");
        setTile(R.id.rptDeclined,  declined,  "Declined");
        setTile(R.id.rptCancelled, cancelled, "Cancelled");

        // Completion rate bar
        int doneOrCancel = completed + declined + cancelled;
        float rate = doneOrCancel > 0 ? (float) completed / doneOrCancel * 100f : 0f;
        TextView tvRate = findViewById(R.id.tvCompletionRate);
        tvRate.setText(String.format("%.0f%% Completion Rate  (%d completed / %d closed)", rate, completed, doneOrCancel));

        LinearLayout barFill = findViewById(R.id.completionBarFill);
        LinearLayout barBg   = findViewById(R.id.completionBarBg);
        barBg.post(() -> {
            int fullWidth = barBg.getWidth();
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) barFill.getLayoutParams();
            lp.width = (int) (fullWidth * rate / 100f);
            barFill.setLayoutParams(lp);
        });
    }

    private void setTile(int viewId, int value, String label) {
        LinearLayout tile = findViewById(viewId);
        ((TextView) tile.getChildAt(0)).setText(String.valueOf(value));
        ((TextView) tile.getChildAt(1)).setText(label);
    }

    private void loadTopServices() {
        List<String[]> top = db.getTopServices(8);
        LinearLayout container = findViewById(R.id.topServicesContainer);
        container.removeAllViews();

        if (top.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No confirmed/completed bookings yet.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(13);
            container.addView(empty);
            return;
        }

        // Find max count for bar width scaling
        int maxCount = 1;
        for (String[] row : top) maxCount = Math.max(maxCount, Integer.parseInt(row[1]));

        for (String[] row : top) {
            String name    = row[0];
            int    count   = Integer.parseInt(row[1]);
            double revenue = Double.parseDouble(row[2]);

            // Row wrapper
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dpToPx(12));
            rowView.setLayoutParams(rowLp);

            // Label row
            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(14);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameLp);

            TextView tvCount = new TextView(this);
            tvCount.setText(count + " bookings · ₱" + String.format("%,.0f", revenue));
            tvCount.setTextColor(0xFFF5A623);
            tvCount.setTextSize(12);
            labelRow.addView(tvName);
            labelRow.addView(tvCount);
            rowView.addView(labelRow);

            // Bar
            LinearLayout barBg = new LinearLayout(this);
            barBg.setBackgroundColor(0xFF1A1A1A);
            LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
            bgLp.setMargins(0, dpToPx(6), 0, 0);
            barBg.setLayoutParams(bgLp);

            LinearLayout barFill = new LinearLayout(this);
            barFill.setBackgroundColor(0xFFF5A623);
            int barW = (int) ((float) count / maxCount * getResources().getDisplayMetrics().widthPixels * 0.85f);
            barFill.setLayoutParams(new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT));
            barBg.addView(barFill);
            rowView.addView(barBg);

            container.addView(rowView);
        }
    }

    private void loadDailyActivity() {
        List<String[]> daily = db.getDailyBookings();
        LinearLayout container = findViewById(R.id.dailyActivityContainer);
        container.removeAllViews();

        if (daily.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No booking history yet.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(13);
            container.addView(empty);
            return;
        }

        int maxCount = 1;
        for (String[] row : daily) maxCount = Math.max(maxCount, Integer.parseInt(row[1]));

        for (String[] row : daily) {
            String date  = row[0];
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
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(dpToPx(100),
                LinearLayout.LayoutParams.WRAP_CONTENT);
            tvDate.setLayoutParams(dateLp);

            LinearLayout barBg = new LinearLayout(this);
            barBg.setBackgroundColor(0xFF1A1A1A);
            LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            bgLp.setMargins(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));
            barBg.setLayoutParams(bgLp);

            final int maxC = maxCount;
            final int cnt  = count;
            LinearLayout barFill = new LinearLayout(this);
            barFill.setBackgroundColor(0xFF7B61FF);
            barBg.post(() -> {
                int barW = (int) ((float) cnt / maxC * barBg.getWidth());
                barFill.setLayoutParams(new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT));
            });
            barBg.addView(barFill);

            TextView tvCount = new TextView(this);
            tvCount.setText(String.valueOf(count));
            tvCount.setTextColor(Color.WHITE);
            tvCount.setTextSize(13);
            tvCount.setGravity(android.view.Gravity.CENTER_VERTICAL);

            rowView.addView(tvDate);
            rowView.addView(barBg);
            rowView.addView(tvCount);
            container.addView(rowView);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
