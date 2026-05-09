<?php
// P6: Reporting Engine — admin_reports.php
session_start();
require 'db.php';
$pageTitle = 'Reports — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

// ── Date range filter ─────────────────────────────────────────────────────
$rangePreset = $_GET['range'] ?? '30';
$customFrom  = $_GET['from']  ?? '';
$customTo    = $_GET['to']    ?? '';

if ($rangePreset === 'custom' && $customFrom && $customTo) {
    $dateFrom = $customFrom;
    $dateTo   = $customTo;
} else {
    $days     = (int)$rangePreset ?: 30;
    $dateFrom = date('Y-m-d', strtotime("-{$days} days"));
    $dateTo   = date('Y-m-d');
}

// ── Summary stats ─────────────────────────────────────────────────────────
$summary = $pdo->prepare("
    SELECT
        COUNT(*)                          AS total,
        SUM(status='pending')             AS pending,
        SUM(status='confirmed')           AS confirmed,
        SUM(status='completed')           AS completed,
        SUM(status='declined')            AS declined,
        SUM(status='cancelled')           AS cancelled
    FROM appointments
    WHERE appt_date BETWEEN ? AND ?
");
$summary->execute([$dateFrom, $dateTo]);
$sum = $summary->fetch();

// ── Bookings per day (for chart) ──────────────────────────────────────────
$dailyStmt = $pdo->prepare("
    SELECT appt_date, COUNT(*) AS count
    FROM appointments
    WHERE appt_date BETWEEN ? AND ?
    GROUP BY appt_date
    ORDER BY appt_date ASC
");
$dailyStmt->execute([$dateFrom, $dateTo]);
$daily = $dailyStmt->fetchAll();

// ── Top services ──────────────────────────────────────────────────────────
$topSvcStmt = $pdo->prepare("
    SELECT s.name, COUNT(*) AS bookings,
           SUM(s.price) AS revenue
    FROM appointments a
    JOIN services s ON s.id = a.service_id
    WHERE a.appt_date BETWEEN ? AND ?
      AND a.status IN ('confirmed','completed')
    GROUP BY s.id, s.name
    ORDER BY bookings DESC
    LIMIT 10
");
$topSvcStmt->execute([$dateFrom, $dateTo]);
$topServices = $topSvcStmt->fetchAll();

// ── Busiest days of week ──────────────────────────────────────────────────
$dowStmt = $pdo->prepare("
    SELECT DAYNAME(appt_date) AS day_name, COUNT(*) AS count
    FROM appointments
    WHERE appt_date BETWEEN ? AND ?
    GROUP BY DAYOFWEEK(appt_date), DAYNAME(appt_date)
    ORDER BY DAYOFWEEK(appt_date)
");
$dowStmt->execute([$dateFrom, $dateTo]);
$dowData = $dowStmt->fetchAll();

// ── Busiest time slots ────────────────────────────────────────────────────
$timeStmt = $pdo->prepare("
    SELECT HOUR(appt_time) AS hour, COUNT(*) AS count
    FROM appointments
    WHERE appt_date BETWEEN ? AND ?
    GROUP BY HOUR(appt_time)
    ORDER BY count DESC
    LIMIT 5
");
$timeStmt->execute([$dateFrom, $dateTo]);
$timeSlots = $timeStmt->fetchAll();

// ── Full appointment list for export ─────────────────────────────────────
$listStmt = $pdo->prepare("
    SELECT a.id, u.first_name, u.last_name, u.email,
           s.name AS service, s.price,
           a.vehicle_year, a.vehicle_make, a.vehicle_model, a.plate_no,
           a.appt_date, a.appt_time, a.status, a.admin_notes, a.created_at
    FROM appointments a
    JOIN users    u ON u.id = a.user_id
    JOIN services s ON s.id = a.service_id
    WHERE a.appt_date BETWEEN ? AND ?
    ORDER BY a.appt_date ASC, a.appt_time ASC
");
$listStmt->execute([$dateFrom, $dateTo]);
$allAppts = $listStmt->fetchAll();

// ── CSV export ────────────────────────────────────────────────────────────
if (isset($_GET['export']) && $_GET['export'] === 'csv') {
    header('Content-Type: text/csv');
    header('Content-Disposition: attachment; filename="maestro_report_'.$dateFrom.'_to_'.$dateTo.'.csv"');
    $out = fopen('php://output', 'w');
    fputcsv($out, ['#','First Name','Last Name','Email','Service','Price',
                    'Vehicle Year','Make','Model','Plate','Date','Time','Status','Admin Notes','Booked At']);
    foreach ($allAppts as $r) {
        fputcsv($out, [
            $r['id'],$r['first_name'],$r['last_name'],$r['email'],$r['service'],$r['price'],
            $r['vehicle_year'],$r['vehicle_make'],$r['vehicle_model'],$r['plate_no'],
            $r['appt_date'],$r['appt_time'],$r['status'],$r['admin_notes'],$r['created_at'],
        ]);
    }
    fclose($out);
    exit;
}

// Serialise data for charts
$chartLabels = json_encode(array_column($daily, 'appt_date'));
$chartData   = json_encode(array_column($daily, 'count'));
$dowLabels   = json_encode(array_column($dowData, 'day_name'));
$dowCounts   = json_encode(array_column($dowData, 'count'));
?>

<main class="page-shell">

    <div class="page-header" style="display:flex;align-items:flex-end;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div>
            <div class="page-label">P6 · Reporting Engine</div>
            <div class="page-title">Operational Reports</div>
            <div class="page-sub">
                <?= date('M j, Y', strtotime($dateFrom)) ?> — <?= date('M j, Y', strtotime($dateTo)) ?>
            </div>
        </div>
        <div style="display:flex;gap:8px;">
            <a href="?range=<?= $rangePreset ?>&from=<?= $dateFrom ?>&to=<?= $dateTo ?>&export=csv"
               class="btn btn-secondary">
                <svg viewBox="0 0 24 24" style="width:15px;height:15px;fill:currentColor;"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                Export CSV
            </a>
            <button onclick="window.print()" class="btn btn-secondary">
                <svg viewBox="0 0 24 24" style="width:15px;height:15px;fill:currentColor;"><path d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z"/></svg>
                Print
            </button>
        </div>
    </div>

    <!-- DATE RANGE FILTER -->
    <div class="card card-sm" style="margin-bottom:24px;">
        <form method="GET" style="display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end;">
            <div>
                <label class="lbl">Quick Range</label>
                <select name="range" onchange="this.form.submit()">
                    <option value="7"      <?= $rangePreset==='7'     ?'selected':'' ?>>Last 7 days</option>
                    <option value="30"     <?= $rangePreset==='30'    ?'selected':'' ?>>Last 30 days</option>
                    <option value="90"     <?= $rangePreset==='90'    ?'selected':'' ?>>Last 90 days</option>
                    <option value="365"    <?= $rangePreset==='365'   ?'selected':'' ?>>Last 12 months</option>
                    <option value="custom" <?= $rangePreset==='custom'?'selected':'' ?>>Custom range</option>
                </select>
            </div>
            <?php if ($rangePreset === 'custom'): ?>
            <div>
                <label class="lbl">From</label>
                <input type="date" name="from" value="<?= $customFrom ?>">
            </div>
            <div>
                <label class="lbl">To</label>
                <input type="date" name="to" value="<?= $customTo ?>">
            </div>
            <button type="submit" class="btn btn-primary btn-sm">Apply</button>
            <?php endif; ?>
        </form>
    </div>

    <!-- KPI TILES -->
    <div class="stats-grid" style="margin-bottom:32px;">
        <?php
        $kpis = [
            ['Total Bookings',  $sum['total'],     '#F5A623'],
            ['Pending',         $sum['pending'],   '#4A90D9'],
            ['Confirmed',       $sum['confirmed'], '#4CAF7D'],
            ['Completed',       $sum['completed'], '#7B61FF'],
            ['Declined',        $sum['declined'],  '#E05252'],
            ['Cancelled',       $sum['cancelled'], '#7A8EA8'],
        ];
        foreach ($kpis as [$lbl, $val, $col]):
        ?>
        <div class="stat-tile">
            <div style="width:4px;height:40px;border-radius:2px;background:<?= $col ?>;margin-bottom:14px;"></div>
            <div class="stat-val"><?= (int)$val ?></div>
            <div class="stat-label"><?= $lbl ?></div>
        </div>
        <?php endforeach; ?>
    </div>

    <!-- CHARTS ROW -->
    <div style="display:grid;grid-template-columns:2fr 1fr;gap:20px;margin-bottom:32px;">
        <!-- Daily bookings line chart -->
        <div class="card">
            <div class="section-title" style="margin-bottom:18px;">Bookings Over Time</div>
            <canvas id="dailyChart" height="200"></canvas>
        </div>

        <!-- Day-of-week bar chart -->
        <div class="card">
            <div class="section-title" style="margin-bottom:18px;">Busiest Days</div>
            <canvas id="dowChart" height="200"></canvas>
        </div>
    </div>

    <!-- TWO COLUMNS: Top services + Time slots -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:32px;">

        <!-- Top services -->
        <div class="card" style="padding:0;">
            <div style="padding:20px 22px;border-bottom:1px solid var(--border-sub);">
                <div class="section-title">Top Services by Bookings</div>
                <div class="section-sub">Confirmed &amp; completed only</div>
            </div>
            <?php if (empty($topServices)): ?>
                <div class="empty-state" style="padding:32px;"><p>No data in range.</p></div>
            <?php else: ?>
                <?php
                $maxBook = max(array_column($topServices,'bookings')) ?: 1;
                foreach ($topServices as $svc):
                    $pct = round($svc['bookings'] / $maxBook * 100);
                ?>
                <div style="padding:14px 22px;border-bottom:1px solid rgba(255,255,255,0.04);">
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                        <span style="font-size:14px;font-weight:600;"><?= htmlspecialchars($svc['name']) ?></span>
                        <span style="font-size:13px;color:var(--muted);">
                            <?= $svc['bookings'] ?> booking<?= $svc['bookings']!=1?'s':'' ?>
                            · ₱<?= number_format($svc['revenue'],0) ?>
                        </span>
                    </div>
                    <div style="height:6px;background:var(--border-sub);border-radius:3px;overflow:hidden;">
                        <div style="height:100%;width:<?= $pct ?>%;background:var(--yellow);border-radius:3px;
                            transition:width .6s ease;"></div>
                    </div>
                </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>

        <!-- Busiest time slots + workload summary -->
        <div>
            <div class="card" style="margin-bottom:16px;">
                <div class="section-title" style="margin-bottom:16px;">Busiest Time Slots</div>
                <?php if (empty($timeSlots)): ?>
                    <div style="color:var(--muted);font-size:14px;">No data in range.</div>
                <?php else: ?>
                    <?php foreach ($timeSlots as $ts):
                        $h = (int)$ts['hour'];
                        $label = date('g:i A', mktime($h,0));
                    ?>
                    <div style="display:flex;justify-content:space-between;align-items:center;
                        padding:10px 0;border-bottom:1px solid var(--border-sub);">
                        <span style="font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:1rem;color:var(--white);">
                            <?= $label ?>
                        </span>
                        <span class="badge badge-confirmed"><?= $ts['count'] ?> booking<?= $ts['count']!=1?'s':'' ?></span>
                    </div>
                    <?php endforeach; ?>
                <?php endif; ?>
            </div>

            <div class="card">
                <div class="section-title" style="margin-bottom:14px;">Completion Rate</div>
                <?php
                $denom = max(1, $sum['total'] - $sum['pending']);
                $rate  = $sum['total'] > 0
                    ? round($sum['completed'] / $sum['total'] * 100, 1)
                    : 0;
                ?>
                <div style="font-family:'Barlow Condensed',sans-serif;font-size:3rem;font-weight:900;color:var(--yellow);line-height:1;">
                    <?= $rate ?>%
                </div>
                <div style="font-size:13px;color:var(--muted);margin-top:6px;">
                    of all bookings in this period were completed
                </div>
                <div style="margin-top:14px;height:8px;background:var(--border-sub);border-radius:4px;overflow:hidden;">
                    <div style="height:100%;width:<?= $rate ?>%;background:var(--yellow);border-radius:4px;"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- FULL DATA TABLE -->
    <div>
        <div class="section-head">
            <div>
                <div class="section-title">Full Appointment Log</div>
                <div class="section-sub"><?= count($allAppts) ?> records · <?= date('M j', strtotime($dateFrom)) ?> – <?= date('M j, Y', strtotime($dateTo)) ?></div>
            </div>
        </div>

        <div class="card" style="padding:0;">
            <?php if (empty($allAppts)): ?>
                <div class="empty-state"><p>No appointments in this period.</p></div>
            <?php else: ?>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>#</th><th>Customer</th><th>Service</th>
                        <th>Vehicle</th><th>Date</th><th>Time</th><th>Status</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($allAppts as $r): ?>
                        <tr>
                            <td style="color:var(--muted);font-size:12px;">#<?= $r['id'] ?></td>
                            <td>
                                <?= htmlspecialchars($r['first_name'].' '.$r['last_name']) ?><br>
                                <span style="font-size:12px;color:var(--muted);"><?= htmlspecialchars($r['email']) ?></span>
                            </td>
                            <td>
                                <?= htmlspecialchars($r['service']) ?><br>
                                <span style="font-size:12px;color:var(--muted);">₱<?= number_format($r['price'],2) ?></span>
                            </td>
                            <td style="font-size:13px;">
                                <?= htmlspecialchars($r['vehicle_year'].' '.$r['vehicle_make'].' '.$r['vehicle_model']) ?>
                                <?= $r['plate_no'] ? '<br><span style="color:var(--muted);">'.htmlspecialchars($r['plate_no']).'</span>' : '' ?>
                            </td>
                            <td><?= date('M j, Y', strtotime($r['appt_date'])) ?></td>
                            <td><?= date('g:i A',  strtotime($r['appt_time'])) ?></td>
                            <td><span class="badge badge-<?= $r['status'] ?>"><span class="badge-dot"></span><?= ucfirst($r['status']) ?></span></td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
            <?php endif; ?>
        </div>
    </div>

</main>

<!-- Chart.js via CDN -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.0/chart.umd.min.js"></script>
<script>
(function () {
    const labels  = <?= $chartLabels ?>;
    const data    = <?= $chartData ?>;
    const dowLbls = <?= $dowLabels ?>;
    const dowData = <?= $dowCounts ?>;

    const gridColor  = 'rgba(255,255,255,0.06)';
    const textColor  = '#7A8EA8';
    const accentColor = '#F5A623';

    Chart.defaults.color = textColor;
    Chart.defaults.font.family = "'Barlow', sans-serif";

    // Daily bookings
    new Chart(document.getElementById('dailyChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'Bookings',
                data,
                borderColor: accentColor,
                backgroundColor: 'rgba(245,166,35,0.08)',
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: accentColor,
                pointRadius: 4,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: gridColor }, ticks: { maxTicksLimit: 8 } },
                y: { grid: { color: gridColor }, beginAtZero: true, ticks: { stepSize: 1 } }
            }
        }
    });

    // Day of week
    new Chart(document.getElementById('dowChart'), {
        type: 'bar',
        data: {
            labels: dowLbls,
            datasets: [{
                label: 'Bookings',
                data: dowData,
                backgroundColor: 'rgba(245,166,35,0.7)',
                borderColor: accentColor,
                borderWidth: 1,
                borderRadius: 6,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: gridColor } },
                y: { grid: { color: gridColor }, beginAtZero: true, ticks: { stepSize: 1 } }
            }
        }
    });
})();
</script>

<?php require 'partials/footer.php'; ?>
