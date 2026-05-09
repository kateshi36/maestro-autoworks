<?php
// P5: Notification System — notifications.php
session_start();
require 'db.php';
$pageTitle = 'Notifications — Maestro Autoworks';
require 'partials/header.php';

// Mark all as read
$pdo->prepare("UPDATE notifications SET is_read = 1 WHERE user_id = ?")
    ->execute([$me['id']]);

// Fetch all with pagination
$page    = max(1, (int)($_GET['page'] ?? 1));
$perPage = 20;
$offset  = ($page - 1) * $perPage;

$total = (int)$pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ?")
    ->execute([$me['id']]) ? $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ?")->execute([$me['id']]) : 0;

// 1. Fetch total count for pagination
$countStmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ?");
$countStmt->execute([$me['id']]);
$total = (int)$countStmt->fetchColumn();

// 2. Prepare the notification fetch query
$notifStmt = $pdo->prepare("
    SELECT n.*, a.appt_date, a.appt_time, s.name AS service_name
    FROM notifications n
    LEFT JOIN appointments a ON a.id = n.appt_id
    LEFT JOIN services     s ON s.id = a.service_id
    WHERE n.user_id = :user_id
    ORDER BY n.created_at DESC
    LIMIT :limit OFFSET :offset
");

// 3. Bind values specifically as Integers to avoid quotes
$notifStmt->bindValue(':user_id', $me['id'], PDO::PARAM_INT);
$notifStmt->bindValue(':limit', (int)$perPage, PDO::PARAM_INT);
$notifStmt->bindValue(':offset', (int)$offset, PDO::PARAM_INT);

$notifStmt->execute();
$notifs = $notifStmt->fetchAll();

$totalPages = max(1, ceil($total / $perPage));

// Notification type → icon + colour
function notifStyle(string $type): array {
    return match($type) {
        'booking_received'  => ['#F5A623', 'M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13z'],
        'booking_confirmed' => ['#4CAF7D', 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'],
        'booking_declined'  => ['#E05252', 'M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z'],
        'booking_completed' => ['#4A90D9', 'M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z'],
        'booking_cancelled' => ['#7A8EA8', 'M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z'],
        'reminder'          => ['#F5A623', 'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6V11c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z'],
        default             => ['#7A8EA8', 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'],
    };
}

function notifLabel(string $type): string {
    return match($type) {
        'booking_received'  => 'Booking Received',
        'booking_confirmed' => 'Booking Confirmed',
        'booking_declined'  => 'Booking Declined',
        'booking_completed' => 'Service Completed',
        'booking_cancelled' => 'Booking Cancelled',
        'reminder'          => 'Reminder',
        default             => 'Notification',
    };
}
?>

<main class="page-shell">

    <div class="page-header" style="display:flex;align-items:flex-end;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div>
            <div class="page-label">P5 · Notification System</div>
            <div class="page-title">Notifications</div>
            <div class="page-sub"><?= $total ?> total notification<?= $total!=1?'s':'' ?></div>
        </div>
        <?php if ($total > 0): ?>
            <form method="POST" action="clear_notifications.php">
                <button type="submit" class="btn btn-secondary btn-sm"
                    onclick="return confirm('Clear all notifications?')">
                    Clear All
                </button>
            </form>
        <?php endif; ?>
    </div>

    <?php if (empty($notifs)): ?>
        <div class="card">
            <div class="empty-state">
                <svg viewBox="0 0 24 24"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6V11c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
                <p>You're all caught up — no notifications yet.</p>
            </div>
        </div>
    <?php else: ?>
        <div class="card" style="padding:0;">
            <?php foreach ($notifs as $n):
                [$color, $iconPath] = notifStyle($n['type']);
            ?>
            <div style="display:flex;gap:16px;padding:18px 22px;
                border-bottom:1px solid var(--border-sub);
                transition:background .15s;"
                onmouseover="this.style.background='rgba(255,255,255,0.02)'"
                onmouseout="this.style.background=''">

                <!-- Icon -->
                <div style="flex-shrink:0;width:42px;height:42px;border-radius:10px;
                    background:<?= $color ?>22;
                    display:flex;align-items:center;justify-content:center;margin-top:2px;">
                    <svg viewBox="0 0 24 24" style="width:18px;height:18px;fill:<?= $color ?>;">
                        <path d="<?= $iconPath ?>"/>
                    </svg>
                </div>

                <!-- Content -->
                <div style="flex:1;min-width:0;">
                    <div style="display:flex;align-items:center;gap:10px;margin-bottom:5px;flex-wrap:wrap;">
                        <span style="font-family:'Barlow Condensed',sans-serif;font-size:13px;
                            font-weight:700;letter-spacing:.8px;text-transform:uppercase;color:<?= $color ?>;">
                            <?= notifLabel($n['type']) ?>
                        </span>
                        <?php if ($n['appt_date']): ?>
                            <span style="font-size:12px;color:var(--muted);">
                                <?= date('M j, Y', strtotime($n['appt_date'])) ?>
                                <?= $n['appt_time'] ? '· '.date('g:i A', strtotime($n['appt_time'])) : '' ?>
                            </span>
                        <?php endif; ?>
                    </div>
                    <div style="font-size:14px;color:var(--text);line-height:1.6;">
                        <?= htmlspecialchars($n['message']) ?>
                    </div>
                    <div style="font-size:12px;color:var(--muted);margin-top:6px;">
                        <?= date('F j, Y \a\t g:i A', strtotime($n['created_at'])) ?>
                    </div>
                </div>

                <?php if ($n['appt_id']): ?>
                <div style="flex-shrink:0;align-self:center;">
                    <a href="<?= $me['role']==='admin' ? 'admin_appointments.php' : 'dashboard.php' ?>"
                       class="btn btn-secondary btn-sm">View</a>
                </div>
                <?php endif; ?>
            </div>
            <?php endforeach; ?>
        </div>

        <!-- Pagination -->
        <?php if ($totalPages > 1): ?>
        <div style="display:flex;gap:6px;justify-content:center;margin-top:24px;">
            <?php for ($p = 1; $p <= $totalPages; $p++): ?>
                <a href="?page=<?= $p ?>"
                   class="btn <?= $p===$page?'btn-primary':'btn-secondary' ?> btn-sm">
                    <?= $p ?>
                </a>
            <?php endfor; ?>
        </div>
        <?php endif; ?>
    <?php endif; ?>

</main>

<?php require 'partials/footer.php'; ?>
