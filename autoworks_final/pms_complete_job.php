<?php
// PMS handler — mark entire appointment as completed once all tasks are done
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }
$meStmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$meStmt->execute([$_SESSION['user_id']]);
$me = $meStmt->fetch();
if (!$me || $me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: admin_pms.php'); exit; }

$apptId = (int)($_POST['appt_id'] ?? 0);
if (!$apptId) {
    header('Location: admin_pms.php');
    exit;
}

// Safety check: refuse if any tasks are still open
$openStmt = $pdo->prepare("
    SELECT COUNT(*) FROM repair_tasks
    WHERE  appt_id = ? AND status != 'completed'
");
$openStmt->execute([$apptId]);
$openCount = (int)$openStmt->fetchColumn();

if ($openCount > 0) {
    $_SESSION['flash'] = [
        'type' => 'error',
        'msg'  => "{$openCount} task(s) are still open. Complete all tasks before closing the job.",
    ];
    header('Location: admin_pms.php');
    exit;
}

// Fetch appointment details for the completion notification
$apptStmt = $pdo->prepare("
    SELECT a.*, u.first_name, u.last_name, u.id AS customer_id,
           s.name AS service_name
    FROM   appointments a
    JOIN   users    u ON u.id = a.user_id
    JOIN   services s ON s.id = a.service_id
    WHERE  a.id = ? AND a.status = 'confirmed'
");
$apptStmt->execute([$apptId]);
$appt = $apptStmt->fetch();

if (!$appt) {
    $_SESSION['flash'] = [
        'type' => 'error',
        'msg'  => 'Appointment not found or already closed.',
    ];
    header('Location: admin_pms.php');
    exit;
}

// Mark the appointment as completed
$pdo->prepare("
    UPDATE appointments SET status = 'completed' WHERE id = ?
")->execute([$apptId]);

// Fire the standard booking_completed notification (mirrors what update_appointment.php does)
$dateStr = date('F j, Y', strtotime($appt['appt_date']));
$msg     = "Your {$appt['service_name']} service on {$dateStr} has been marked as completed. "
         . "Thank you for choosing Maestro Autoworks!";

$pdo->prepare("
    INSERT INTO notifications (user_id, appt_id, type, message)
    VALUES (?, ?, 'booking_completed', ?)
")->execute([$appt['customer_id'], $apptId, $msg]);

// Also notify admin for the record
$pdo->prepare("
    INSERT INTO notifications (user_id, appt_id, type, message)
    VALUES (1, ?, 'booking_completed', ?)
")->execute([
    $apptId,
    "Job closed: {$appt['service_name']} for {$appt['first_name']} {$appt['last_name']} on {$dateStr}."
]);

$_SESSION['flash'] = [
    'type' => 'success',
    'msg'  => "Job marked complete. {$appt['first_name']} {$appt['last_name']} has been notified.",
];
header('Location: admin_pms.php');
exit;
