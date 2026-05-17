<?php
// P1/P3: Customer Dashboard — dashboard.php
session_start();
require 'db.php';
$pageTitle = 'My Dashboard — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] === 'admin') { header('Location: admin_dashboard.php'); exit; }

// Stats
$stats = $pdo->prepare("
    SELECT
        COUNT(*) as total,
        SUM(status='pending')   as pending,
        SUM(status='confirmed') as confirmed,
        SUM(status='completed') as completed
    FROM appointments WHERE user_id = ?
");
$stats->execute([$me['id']]);
$st = $stats->fetch();

// Upcoming appointments
$upcoming = $pdo->prepare("
    SELECT a.*, s.name as service_name, s.price, s.duration_hr
    FROM appointments a
    JOIN services s ON s.id = a.service_id
    WHERE a.user_id = ? AND a.status IN ('pending','confirmed')
      AND a.appt_date >= CURDATE()
    ORDER BY a.appt_date ASC, a.appt_time ASC
    LIMIT 10
");
$upcoming->execute([$me['id']]);
$upcomingRows = $upcoming->fetchAll();

// Past appointments
$past = $pdo->prepare("
    SELECT a.*, s.name as service_name, s.price
    FROM appointments a
    JOIN services s ON s.id = a.service_id
    WHERE a.user_id = ? AND (a.status IN ('completed','declined','cancelled') OR a.appt_date < CURDATE())
    ORDER BY a.appt_date DESC
    LIMIT 10
");
$past->execute([$me['id']]);
$pastRows = $past->fetchAll();

// Flash messages
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
            <div class="page-label">Customer Portal</div>
            <div class="page-title">Welcome back, <?= htmlspecialchars($me['first_name']) ?></div>
            <div class="page-sub">Here's a summary of your service history and upcoming bookings.</div>
        </div>
        <a href="book.php" class="btn btn-primary">
            <svg viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            New Booking
        </a>
    </div>

    <!-- STAT TILES -->
    <div class="stats-grid">
        <?php
        $tiles = [
            ['Total Bookings',  $st['total'],     'M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z'],
            ['Pending',         $st['pending'],   'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'],
            ['Confirmed',       $st['confirmed'], 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'],
            ['Completed',       $st['completed'], 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z'],
        ];
        foreach ($tiles as [$lbl, $val, $path]):
        ?>
        <div class="stat-tile">
            <div class="stat-icon">
                <svg viewBox="0 0 24 24"><path d="<?= $path ?>"/></svg>
            </div>
            <div class="stat-val"><?= (int)$val ?></div>
            <div class="stat-label"><?= $lbl ?></div>
        </div>
        <?php endforeach; ?>
    </div>

    <!-- UPCOMING APPOINTMENTS -->
    <div style="margin-bottom:32px;">
        <div class="section-head">
            <div>
                <div class="section-title">Upcoming Appointments</div>
                <div class="section-sub">Pending &amp; confirmed bookings</div>
            </div>
            <a href="book.php" class="btn btn-secondary btn-sm">+ Book</a>
        </div>

        <div class="card" style="padding:0;">
            <?php if (empty($upcomingRows)): ?>
                <div class="empty-state">
                    <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                    <p>No upcoming appointments.<br><a href="book.php">Book your first service →</a></p>
                </div>
            <?php else: ?>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Service</th><th>Vehicle</th>
                        <th>Date &amp; Time</th><th>Status</th><th>Action</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($upcomingRows as $row): ?>
                        <tr>
                            <td>
                                <strong><?= htmlspecialchars($row['service_name']) ?></strong><br>
                                <span style="font-size:12px;color:var(--muted);">₱<?= number_format($row['price'],2) ?></span>
                            </td>
                            <td>
                                <?= htmlspecialchars($row['vehicle_year'].' '.$row['vehicle_make'].' '.$row['vehicle_model']) ?>
                                <?php if ($row['plate_no']): ?>
                                    <br><span style="font-size:12px;color:var(--muted);"><?= htmlspecialchars($row['plate_no']) ?></span>
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
                                <?php if (!empty($row['admin_notes'])): ?>
                                <div style="margin-top:8px;padding:8px 10px;
                                    background:rgba(251,189,35,0.08);
                                    border-left:3px solid var(--yellow);
                                    border-radius:0 6px 6px 0;
                                    font-size:12px;color:var(--text);line-height:1.5;max-width:220px;">
                                    <span style="font-size:11px;font-weight:700;
                                        text-transform:uppercase;letter-spacing:.5px;
                                        color:var(--yellow);display:block;margin-bottom:3px;">
                                        Shop Note
                                    </span>
                                    <?= htmlspecialchars($row['admin_notes']) ?>
                                </div>
                                <?php endif; ?>
                            </td>
                            <td>
                                <?php if ($row['status'] === 'pending'): ?>
                                <a href="cancel_appointment.php?id=<?= $row['id'] ?>"
                                   class="btn btn-danger btn-sm"
                                   onclick="return confirm('Cancel this appointment?')">Cancel</a>
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

    <!-- PAST APPOINTMENTS -->
    <div>
        <div class="section-head">
            <div>
                <div class="section-title">Service History</div>
                <div class="section-sub">Completed, declined &amp; cancelled records</div>
            </div>
        </div>

        <div class="card" style="padding:0;">
            <?php if (empty($pastRows)): ?>
                <div class="empty-state">
                    <svg viewBox="0 0 24 24"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
                    <p>No past service records yet.</p>
                </div>
            <?php else: ?>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Service</th><th>Vehicle</th>
                        <th>Date</th><th>Status</th><th>Rating</th><th>Shop Note</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($pastRows as $row): ?>
                        <tr>
                            <td>
                                <strong><?= htmlspecialchars($row['service_name']) ?></strong><br>
                                <span style="font-size:12px;color:var(--muted);">₱<?= number_format($row['price'],2) ?></span>
                            </td>
                            <td><?= htmlspecialchars($row['vehicle_year'].' '.$row['vehicle_make'].' '.$row['vehicle_model']) ?></td>
                            <td><?= date('M j, Y', strtotime($row['appt_date'])) ?></td>
                            <td>
                                <span class="badge badge-<?= $row['status'] ?>">
                                    <span class="badge-dot"></span>
                                    <?= ucfirst($row['status']) ?>
                                </span>
                            </td>
                            <td style="white-space:nowrap;">
                                <?php if (!empty($row['rating']) && (int)$row['rating'] > 0):
                                    $r = (int)$row['rating'];
                                    $ratingLabels = ['','Poor 😞','Fair 😐','Good 🙂','Great 😊','Excellent 🌟'];
                                ?>
                                <span style="color:var(--yellow);letter-spacing:1px;"><?= str_repeat('★', $r) ?></span><span style="color:var(--border);"><?= str_repeat('★', 5 - $r) ?></span>
                                <br><span style="font-size:11px;color:var(--muted);"><?= htmlspecialchars($ratingLabels[$r]) ?></span>
                                <?php else: ?>
                                <span style="color:var(--muted);font-size:13px;">—</span>
                                <?php endif; ?>
                            </td>
                            <td style="max-width:200px;">
                                <?php if (!empty($row['admin_notes'])): ?>
                                <div style="font-size:12px;line-height:1.5;">
                                    <span style="font-size:11px;font-weight:700;text-transform:uppercase;
                                        letter-spacing:.5px;color:var(--yellow);">Shop&nbsp;◆</span>
                                    <span style="color:var(--text);">
                                        <?= htmlspecialchars($row['admin_notes']) ?>
                                    </span>
                                </div>
                                <?php else: ?>
                                <span style="color:var(--muted);font-size:13px;">—</span>
                                <?php endif; ?>
                                <?php if (!empty($row['notes'])): ?>
                                <div style="font-size:12px;line-height:1.5;margin-top:5px;
                                    padding-top:5px;border-top:1px solid var(--border-sub);">
                                    <span style="font-size:11px;font-weight:700;text-transform:uppercase;
                                        letter-spacing:.5px;color:var(--muted);">You&nbsp;◆</span>
                                    <span style="color:var(--muted);">
                                        <?= htmlspecialchars($row['notes']) ?>
                                    </span>
                                </div>
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

<?php require 'partials/footer.php'; ?>
