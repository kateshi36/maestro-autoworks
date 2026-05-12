<?php
// P4: Manage Appointments — admin_appointments.php
session_start();
require 'db.php';
$pageTitle = 'Manage Appointments — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

// ── Filters ───────────────────────────────────────────────────────────────
$filterStatus = $_GET['status'] ?? 'all';
$filterDate   = $_GET['date']   ?? '';
$search       = trim($_GET['q'] ?? '');

$where  = ['1=1'];
$params = [];

if ($filterStatus !== 'all') {
    $where[]  = 'a.status = ?';
    $params[] = $filterStatus;
}
if ($filterDate) {
    $where[]  = 'a.appt_date = ?';
    $params[] = $filterDate;
}
if ($search) {
    $where[]  = '(u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ? OR s.name LIKE ?)';
    $like = "%$search%";
    array_push($params, $like, $like, $like, $like);
}

$sql = "
    SELECT a.*, u.first_name, u.last_name, u.email,
           s.name AS service_name, s.price, s.duration_hr
    FROM appointments a
    JOIN users    u ON u.id = a.user_id
    JOIN services s ON s.id = a.service_id
    WHERE " . implode(' AND ', $where) . "
    ORDER BY a.appt_date DESC, a.appt_time DESC
";

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$appointments = $stmt->fetchAll();

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
            <div class="page-label">P4 · Administrative Portal</div>
            <div class="page-title">Manage Appointments</div>
            <div class="page-sub"><?= count($appointments) ?> record<?= count($appointments)!=1?'s':'' ?> found</div>
        </div>
        <a href="admin_reports.php" class="btn btn-secondary">
            <svg viewBox="0 0 24 24" style="width:15px;height:15px;fill:currentColor;"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
            Generate Reports
        </a>
    </div>

    <!-- FILTERS -->
    <div class="card card-sm" style="margin-bottom:24px;">
        <form method="GET" style="display:flex;gap:14px;flex-wrap:wrap;align-items:flex-end;">
            <div style="flex:1;min-width:180px;">
                <label class="lbl">Search</label>
                <input type="text" name="q" placeholder="Name, email, or service…" value="<?= htmlspecialchars($search) ?>">
            </div>
            <div style="min-width:160px;">
                <label class="lbl">Status</label>
                <select name="status">
                    <?php foreach (['all','pending','confirmed','completed','declined','cancelled'] as $s): ?>
                        <option value="<?= $s ?>" <?= $filterStatus===$s?'selected':'' ?>><?= ucfirst($s) ?></option>
                    <?php endforeach; ?>
                </select>
            </div>
            <div style="min-width:160px;">
                <label class="lbl">Date</label>
                <input type="date" name="date" value="<?= htmlspecialchars($filterDate) ?>">
            </div>
            <button type="submit" class="btn btn-primary btn-sm">Filter</button>
            <a href="admin_appointments.php" class="btn btn-secondary btn-sm">Reset</a>
        </form>
    </div>

    <!-- STATUS TABS -->
    <div style="display:flex;gap:6px;margin-bottom:20px;flex-wrap:wrap;">
        <?php
        $statuses = ['all','pending','confirmed','completed','declined','cancelled'];
        foreach ($statuses as $s):
            $active = ($filterStatus === $s) ? 'active' : '';
        ?>
            <a href="?status=<?= $s ?><?= $filterDate ? '&date='.$filterDate : '' ?><?= $search ? '&q='.urlencode($search) : '' ?>"
               class="tab-btn <?= $active ?>">
                <?= ucfirst($s) ?>
            </a>
        <?php endforeach; ?>
    </div>

    <!-- TABLE -->
    <div class="card" style="padding:0;">
        <?php if (empty($appointments)): ?>
            <div class="empty-state">
                <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                <p>No appointments match your filters.</p>
            </div>
        <?php else: ?>
        <div class="table-wrap">
            <table>
                <thead><tr>
                    <th>#</th><th>Customer</th><th>Service</th>
                    <th>Vehicle</th><th>Scheduled</th>
                    <th>Status</th><th>Notes</th><th>Actions</th>
                </tr></thead>
                <tbody>
                <?php foreach ($appointments as $row): ?>
                    <tr>
                        <td style="font-size:12px;color:var(--muted);">#<?= $row['id'] ?></td>
                        <td>
                            <strong><?= htmlspecialchars($row['first_name'].' '.$row['last_name']) ?></strong><br>
                            <span style="font-size:12px;color:var(--muted);"><?= htmlspecialchars($row['email']) ?></span>
                        </td>
                        <td>
                            <?= htmlspecialchars($row['service_name']) ?><br>
                            <span style="font-size:12px;color:var(--muted);">₱<?= number_format($row['price'],2) ?></span>
                        </td>
                        <td style="font-size:13px;">
                            <?= htmlspecialchars($row['vehicle_year'].' '.$row['vehicle_make'].' '.$row['vehicle_model']) ?>
                            <?php if ($row['plate_no']): ?>
                                <br><span style="color:var(--muted);"><?= htmlspecialchars($row['plate_no']) ?></span>
                            <?php endif; ?>
                        </td>
                        <td>
                            <?= date('M j, Y', strtotime($row['appt_date'])) ?><br>
                            <span style="color:var(--muted);font-size:13px;"><?= date('g:i A', strtotime($row['appt_time'])) ?></span>
                        </td>
                        <td>
                            <span class="badge badge-<?= $row['status'] ?>">
                                <span class="badge-dot"></span>
                                <?= ucfirst($row['status']) ?>
                            </span>
                        </td>
                        <td style="max-width:160px;font-size:12px;color:var(--muted);">
                            <?php if ($row['notes']): ?>
                                <em>Customer:</em> <?= htmlspecialchars(mb_substr($row['notes'],0,60)) ?>…<br>
                            <?php endif; ?>
                            <?= $row['admin_notes'] ? '<em>Admin:</em> '.htmlspecialchars(mb_substr($row['admin_notes'],0,60)) : '' ?>
                        </td>
                        <td>
                            <div style="display:flex;gap:5px;flex-wrap:wrap;">
                                <?php if ($row['status'] === 'pending'): ?>
                                    <button class="btn btn-success btn-sm"
                                        onclick="openAction(<?= $row['id'] ?>,'confirm',
                                            '<?= htmlspecialchars($row['first_name'].' '.$row['last_name'],ENT_QUOTES) ?>',
                                            '<?= htmlspecialchars($row['service_name'],ENT_QUOTES) ?>')">
                                        ✓
                                    </button>
                                    <button class="btn btn-danger btn-sm"
                                        onclick="openAction(<?= $row['id'] ?>,'decline',
                                            '<?= htmlspecialchars($row['first_name'].' '.$row['last_name'],ENT_QUOTES) ?>',
                                            '<?= htmlspecialchars($row['service_name'],ENT_QUOTES) ?>')">
                                        ✗
                                    </button>
                                <?php elseif ($row['status'] === 'confirmed'): ?>
                                    <form method="POST" action="update_appointment.php">
                                        <input type="hidden" name="id"     value="<?= $row['id'] ?>">
                                        <input type="hidden" name="action" value="complete">
                                        <button type="submit" class="btn btn-secondary btn-sm">Complete</button>
                                    </form>
                                <?php else: ?>
                                    <span style="color:var(--muted);font-size:12px;">—</span>
                                <?php endif; ?>
                            </div>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        </div>
        <?php endif; ?>
    </div>

</main>

<!-- ACTION MODAL -->
<div class="modal-overlay" id="actionModal">
    <div class="modal">
        <div class="modal-head">
            <div class="modal-title" id="actionModalTitle">Confirm Appointment</div>
            <button class="modal-close" onclick="document.getElementById('actionModal').classList.remove('open')">
                <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
            </button>
        </div>
        <div id="actionModalDesc" style="font-size:14px;color:var(--muted);margin-bottom:20px;"></div>
        <form method="POST" action="update_appointment.php">
            <input type="hidden" name="id"     id="actionId">
            <input type="hidden" name="action" id="actionType">
            <div class="form-group">
                <label>Notes to Customer <small style="color:var(--muted);">(optional)</small></label>
                <textarea name="admin_notes" id="adminNotes" placeholder="Add a note for the customer…" style="min-height:80px;"></textarea>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary"
                    onclick="document.getElementById('actionModal').classList.remove('open')">Cancel</button>
                <button type="submit" class="btn btn-primary" id="actionSubmitBtn">Submit</button>
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
    document.getElementById('actionModalTitle').textContent = isConfirm ? 'Confirm Appointment' : 'Decline Appointment';
    document.getElementById('actionModalDesc').textContent  = isConfirm
        ? `Confirming booking for ${name} — ${service}. They will be notified immediately.`
        : `Declining booking for ${name} — ${service}. Please provide a reason below.`;
    const btn = document.getElementById('actionSubmitBtn');
    btn.textContent = isConfirm ? '✓ Confirm Booking' : '✗ Decline Booking';
    btn.className   = isConfirm ? 'btn btn-success' : 'btn btn-danger';
    document.getElementById('actionModal').classList.add('open');
}
document.getElementById('actionModal').addEventListener('click', function(e){
    if(e.target===this) this.classList.remove('open');
});
</script>

<?php require 'partials/footer.php'; ?>
