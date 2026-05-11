<?php
// partials/header.php — included at top of every authenticated page
// Requires: $pdo, session_start() already called, $pageTitle set by caller

if (!isset($_SESSION['user_id'])) {
    header('Location: login.php');
    exit;
}

$currentUser = $pdo->prepare("SELECT id, first_name, last_name, username, role FROM users WHERE id = ?");
$currentUser->execute([$_SESSION['user_id']]);
$me = $currentUser->fetch();
if (!$me) { session_destroy(); header('Location: login.php'); exit; }

// Unread notification count
$unreadStmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0");
$unreadStmt->execute([$me['id']]);
$unreadCount = (int)$unreadStmt->fetchColumn();

// Latest 6 notifications for dropdown
$notifStmt = $pdo->prepare("SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 6");
$notifStmt->execute([$me['id']]);
$notifs = $notifStmt->fetchAll();

$initials = strtoupper(substr($me['first_name'],0,1) . substr($me['last_name'],0,1));
$isAdmin  = ($me['role'] === 'admin');

$currentPage = basename($_SERVER['PHP_SELF'], '.php');
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= htmlspecialchars($pageTitle ?? 'Maestro Autoworks') ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800;900&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="app.css">
</head>
<body>
<div class="app-body">

<header class="topbar">
    <a href="<?= $isAdmin ? 'admin_dashboard.php' : 'dashboard.php' ?>" class="topbar-logo">
        <div class="logo-badge">
            <img src="logo.png" alt="Maestro Autoworks Logo">
        </div>
        Maestro<span style="color:var(--yellow)">Autoworks</span>
    </a>

    <nav class="topbar-nav">
        <?php if ($isAdmin): ?>
            <a href="admin_dashboard.php"    class="<?= $currentPage==='admin_dashboard'    ?'active':'' ?>">Dashboard</a>
            <a href="admin_appointments.php" class="<?= $currentPage==='admin_appointments' ?'active':'' ?>">Appointments</a>
            <a href="admin_services.php"     class="<?= $currentPage==='admin_services'     ?'active':'' ?>">Services</a>
            <a href="admin_reports.php"      class="<?= $currentPage==='admin_reports'      ?'active':'' ?>">Reports</a>
            <a href="admin_pms.php"          class="<?= $currentPage==='admin_pms'          ?'active':'' ?>">Repair Tracker</a>
        <?php else: ?>
            <a href="home.php"      class="<?= $currentPage==='home'      ?'active':'' ?>">Home</a>
            <a href="services.php"  class="<?= $currentPage==='services'  ?'active':'' ?>">Services</a>
            <a href="dashboard.php" class="<?= $currentPage==='dashboard' ?'active':'' ?>">My Dashboard</a>
            <a href="book.php"      class="<?= $currentPage==='book'      ?'active':'' ?>">Book Service</a>
        <?php endif; ?>
    </nav>

    <div class="topbar-right">
        <!-- P5: Notification Bell -->
        <div style="position:relative;">
            <button class="notif-btn" id="notifBtn" aria-label="Notifications">
                <svg viewBox="0 0 24 24"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6V11c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
                <?php if ($unreadCount > 0): ?>
                    <span class="notif-badge"><?= $unreadCount > 9 ? '9+' : $unreadCount ?></span>
                <?php endif; ?>
            </button>

            <div class="notif-panel" id="notifPanel">
                <div class="notif-panel-head">
                    <span class="notif-panel-title">Notifications</span>
                    <?php if ($unreadCount > 0): ?>
                        <a href="mark_read.php" style="font-size:12px;color:var(--yellow);">Mark all read</a>
                    <?php endif; ?>
                </div>
                <?php if (empty($notifs)): ?>
                    <div style="padding:28px;text-align:center;color:var(--muted);font-size:14px;">No notifications yet.</div>
                <?php else: ?>
                    <?php foreach ($notifs as $n): ?>
                        <div class="notif-item <?= $n['is_read'] ? '' : 'unread' ?>">
                            <div class="notif-dot"></div>
                            <div>
                                <div class="notif-msg"><?= htmlspecialchars($n['message']) ?></div>
                                <div class="notif-time"><?= date('M j, g:ia', strtotime($n['created_at'])) ?></div>
                            </div>
                        </div>
                    <?php endforeach; ?>
                    <div style="padding:12px 18px;text-align:center;border-top:1px solid var(--border-sub);">
                        <a href="notifications.php" style="font-size:13px;">View all</a>
                    </div>
                <?php endif; ?>
            </div>
        </div>

        <!-- User chip -->
        <div style="position:relative;">
            <div class="user-chip" id="userChip">
                <div class="user-avatar"><?= $initials ?></div>
                <div>
                    <div class="user-name"><?= htmlspecialchars($me['first_name']) ?></div>
                    <div class="user-role"><?= ucfirst($me['role']) ?></div>
                </div>
            </div>
            <div class="dropdown-menu" id="userMenu">
                <a href="<?= $isAdmin ? 'admin_dashboard.php' : 'dashboard.php' ?>">
                    <svg viewBox="0 0 24 24"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>
                    Dashboard
                </a>
                <div class="dropdown-divider"></div>
                <a href="logout.php">
                    <svg viewBox="0 0 24 24"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
                    Sign Out
                </a>
            </div>
        </div>
    </div>
</header>
