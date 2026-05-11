<?php
// P4: Administrative Dashboard — admin_dashboard.php
session_start();
require 'db.php';
$pageTitle = 'Admin Dashboard — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

// ── Overview stats ────────────────────────────────────────────────────────
$stats = $pdo->query("
    SELECT
        COUNT(*)                          AS total,
        SUM(status='pending')             AS pending,
        SUM(status='confirmed')           AS confirmed,
        SUM(status='completed')           AS completed,
        SUM(status='declined')            AS declined,
        SUM(appt_date = CURDATE())        AS today,
        SUM(appt_date = CURDATE()+INTERVAL 1 DAY) AS tomorrow
    FROM appointments
")->fetch();

// Total customers
$custCount = (int)$pdo->query("SELECT COUNT(*) FROM users WHERE role='customer'")->fetchColumn();

// Today's schedule
$todayAppts = $pdo->query("
    SELECT a.*, u.first_name, u.last_name, u.email, s.name AS service_name, s.duration_hr
    FROM appointments a
    JOIN users    u ON u.id = a.user_id
    JOIN services s ON s.id = a.service_id
    WHERE a.appt_date = CURDATE()
    ORDER BY a.appt_time ASC
")->fetchAll();

// Pending requests (newest first)
$pending = $pdo->query("
    SELECT a.*, u.first_name, u.last_name, u.email, s.name AS service_name, s.price, s.duration_hr
    FROM appointments a
    JOIN users    u ON u.id = a.user_id
    JOIN services s ON s.id = a.service_id
    WHERE a.status = 'pending'
    ORDER BY a.created_at ASC
    LIMIT 20
")->fetchAll();

$flash = $_SESSION['flash'] ?? null;
unset($_SESSION['flash']);
?>

<main class="page-shell">

    <?php if ($flash): ?>
        <div class="alert alert-<?= $flash['type'] ?>" data-auto-close>
            <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
            <?= htmlspecialchars($flash['msg']) ?>
        </div>
    <?php endif; ?>

    <div class="page-header" style="display:flex;align-items:flex-end;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div>
            <div class="page-label">P4 · Admin Portal</div>
            <div class="page-title">Operations Dashboard</div>
            <div class="page-sub"><?= date('l, F j, Y') ?></div>
        </div>
        <a href="admin_appointments.php" class="btn btn-secondary">
            <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
            Manage All Appointments
        </a>
    </div>

    <!-- STAT TILES -->
    <div class="stats-grid">
        <?php
        $tiles = [
            ['Today\'s Jobs',    $stats['today'],     '#2196F3', 'M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z'],
            ['Pending Requests', $stats['pending'],   '#F5A623', 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'],
            ['Confirmed',        $stats['confirmed'], '#4CAF7D', 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'],
            ['Completed Total',  $stats['completed'], '#7B61FF', 'M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z'],
            ['Total Customers',  $custCount,          '#E05252', 'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z'],
            ['All Bookings',     $stats['total'],     '#F5A623', 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z'],
        ];
        foreach ($tiles as [$lbl, $val, $color, $path]):
        ?>
        <div class="stat-tile">
            <div class="stat-icon" style="background:<?= $color ?>22;">
                <svg viewBox="0 0 24 24" style="fill:<?= $color ?>;"><path d="<?= $path ?>"/></svg>
            </div>
            <div class="stat-val"><?= (int)$val ?></div>
            <div class="stat-label"><?= $lbl ?></div>
        </div>
        <?php endforeach; ?>
    </div>

    <!-- PENDING REQUESTS -->
    <?php if (!empty($pending)): ?>
    <div style="margin-bottom:32px;">
        <div class="section-head">
            <div>
                <div class="section-title">
                    Pending Requests
                    <span style="display:inline-flex;align-items:center;justify-content:center;
                        width:22px;height:22px;border-radius:50%;background:var(--yellow);
                        color:var(--navy);font-size:12px;font-weight:800;margin-left:8px;">
                        <?= count($pending) ?>
                    </span>
                </div>
                <div class="section-sub">Review and accept or decline incoming bookings</div>
            </div>
            <a href="admin_appointments.php?status=pending" class="btn btn-secondary btn-sm">View All</a>
        </div>

        <div class="card" style="padding:0;">
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Customer</th><th>Service</th><th>Vehicle</th>
                        <th>Requested For</th><th>Submitted</th><th>Actions</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($pending as $row): ?>
                        <tr>
                            <td>
                                <strong><?= htmlspecialchars($row['first_name'].' '.$row['last_name']) ?></strong><br>
                                <span style="font-size:12px;color:var(--muted);"><?= htmlspecialchars($row['email']) ?></span>
                            </td>
                            <td>
                                <?= htmlspecialchars($row['service_name']) ?><br>
                                <span style="font-size:12px;color:var(--muted);">
                                    ₱<?= number_format($row['price'],2) ?> · <?= $row['duration_hr'] ?>hr
                                </span>
                            </td>
                            <td><?= htmlspecialchars($row['vehicle_year'].' '.$row['vehicle_make'].' '.$row['vehicle_model']) ?></td>
                            <td>
                                <?= date('M j, Y', strtotime($row['appt_date'])) ?><br>
                                <span style="color:var(--muted);font-size:13px;"><?= date('g:i A', strtotime($row['appt_time'])) ?></span>
                            </td>
                            <td style="font-size:13px;color:var(--muted);">
                                <?= date('M j, g:ia', strtotime($row['created_at'])) ?>
                            </td>
                            <td>
                                <div style="display:flex;gap:6px;flex-wrap:wrap;">
                                    <button class="btn btn-success btn-sm"
                                        onclick="openAction(<?= $row['id'] ?>,'confirm',
                                            '<?= htmlspecialchars($row['first_name'].' '.$row['last_name'],ENT_QUOTES) ?>',
                                            '<?= htmlspecialchars($row['service_name'],ENT_QUOTES) ?>')">
                                        ✓ Confirm
                                    </button>
                                    <button class="btn btn-danger btn-sm"
                                        onclick="openAction(<?= $row['id'] ?>,'decline',
                                            '<?= htmlspecialchars($row['first_name'].' '.$row['last_name'],ENT_QUOTES) ?>',
                                            '<?= htmlspecialchars($row['service_name'],ENT_QUOTES) ?>')">
                                        ✗ Decline
                                    </button>
                                </div>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <?php endif; ?>

    <!-- TODAY'S SCHEDULE -->
    <div>
        <div class="section-head">
            <div>
                <div class="section-title">Today's Schedule</div>
                <div class="section-sub"><?= date('l, F j') ?> · <?= count($todayAppts) ?> appointment<?= count($todayAppts)!=1?'s':'' ?></div>
            </div>
            <a href="admin_reports.php" class="btn btn-secondary btn-sm">
                <svg viewBox="0 0 24 24" style="width:13px;height:13px;fill:currentColor;"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 3c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm7 13H5v-.23c0-.62.28-1.2.76-1.58C7.47 15.82 9.64 15 12 15s4.53.82 6.24 2.19c.48.38.76.97.76 1.58V19z"/></svg>
                View Reports
            </a>
        </div>

        <div class="card" style="padding:0;">
            <?php if (empty($todayAppts)): ?>
                <div class="empty-state">
                    <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                    <p>No appointments scheduled for today.</p>
                </div>
            <?php else: ?>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Time</th><th>Customer</th><th>Service</th><th>Vehicle</th><th>Status</th><th>Action</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($todayAppts as $row): ?>
                        <tr>
                            <td style="font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:1rem;">
                                <?= date('g:i A', strtotime($row['appt_time'])) ?>
                            </td>
                            <td><?= htmlspecialchars($row['first_name'].' '.$row['last_name']) ?></td>
                            <td><?= htmlspecialchars($row['service_name']) ?></td>
                            <td><?= htmlspecialchars($row['vehicle_year'].' '.$row['vehicle_make'].' '.$row['vehicle_model']) ?></td>
                            <td><span class="badge badge-<?= $row['status'] ?>"><span class="badge-dot"></span><?= ucfirst($row['status']) ?></span></td>
                            <td>
                                <?php if ($row['status'] === 'confirmed'): ?>
                                <form method="POST" action="update_appointment.php" style="display:inline;">
                                    <input type="hidden" name="id"     value="<?= $row['id'] ?>">
                                    <input type="hidden" name="action" value="complete">
                                    <button type="submit" class="btn btn-secondary btn-sm">Mark Complete</button>
                                </form>
                                <?php else: ?>
                                <span style="color:var(--muted);font-size:13px;">—</span>
                                <?php endif; ?>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
            <?php endif; ?>
        </div>
    </div>

</main>

<!-- ACTION MODAL (Confirm / Decline) -->
<div class="modal-overlay" id="actionModal">
    <div class="modal">
        <div class="modal-head">
            <div class="modal-title" id="actionModalTitle">Confirm Appointment</div>
            <button class="modal-close" data-modal-close>
                <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
            </button>
        </div>

        <div id="actionModalDesc" style="font-size:14px;color:var(--muted);margin-bottom:20px;"></div>

        <form method="POST" action="update_appointment.php" id="actionForm">
            <input type="hidden" name="id"     id="actionId">
            <input type="hidden" name="action" id="actionType">
            <div class="form-group">
                <label for="adminNotes">Notes to Customer <small style="color:var(--muted);">(optional)</small></label>
                <textarea name="admin_notes" id="adminNotes" placeholder="e.g. Slot confirmed. Please arrive 10 minutes early." style="min-height:80px;"></textarea>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-modal-close>Cancel</button>
                <button type="submit" class="btn btn-primary" id="actionSubmitBtn">Confirm</button>
            </div>
        </form>
    </div>
</div>

<script>
function openAction(id, action, name, service) {
    document.getElementById('actionId').value   = id;
    document.getElementById('actionType').value = action;
    document.getElementById('adminNotes').value = '';

    const isConfirm = action === 'confirm';
    document.getElementById('actionModalTitle').textContent =
        isConfirm ? 'Confirm Appointment' : 'Decline Appointment';
    document.getElementById('actionModalDesc').textContent =
        isConfirm
            ? `You are confirming the booking for ${name} (${service}). They will be notified immediately.`
            : `You are declining the booking for ${name} (${service}). Please provide a reason below.`;
    const btn = document.getElementById('actionSubmitBtn');
    btn.textContent = isConfirm ? '✓ Confirm Booking' : '✗ Decline Booking';
    btn.className   = isConfirm ? 'btn btn-success' : 'btn btn-danger';

    document.getElementById('actionModal').classList.add('open');
}

document.querySelector('#actionModal .modal-overlay, [data-modal-close]');
document.getElementById('actionModal').addEventListener('click', function(e) {
    if (e.target === this) this.classList.remove('open');
});
document.querySelectorAll('[data-modal-close]').forEach(b => {
    b.addEventListener('click', () => document.getElementById('actionModal').classList.remove('open'));
});
</script>

<?php require 'partials/footer.php'; ?>
