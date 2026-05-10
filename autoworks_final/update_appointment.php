<?php
// P4 + P5: Update Appointment Status + Send Notification — update_appointment.php
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: admin_dashboard.php'); exit; }

$userStmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$userStmt->execute([$_SESSION['user_id']]);
$me = $userStmt->fetch();

if (!$me || $me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

$id         = (int)($_POST['id']          ?? 0);
$action     = trim($_POST['action']       ?? '');
$adminNotes = trim($_POST['admin_notes']  ?? '');

$allowedActions = ['confirm', 'decline', 'complete', 'cancel'];
if (!$id || !in_array($action, $allowedActions)) {
    $_SESSION['flash'] = ['type'=>'error','msg'=>'Invalid action.'];
    header('Location: admin_dashboard.php');
    exit;
}

// Fetch appointment with customer and service details
$apptStmt = $pdo->prepare("
    SELECT a.*, u.id AS customer_id, u.first_name, u.last_name, u.email,
           s.name AS service_name
    FROM appointments a
    JOIN users    u ON u.id = a.user_id
    JOIN services s ON s.id = a.service_id
    WHERE a.id = ?
");
$apptStmt->execute([$id]);
$appt = $apptStmt->fetch();

if (!$appt) {
    $_SESSION['flash'] = ['type'=>'error','msg'=>'Appointment not found.'];
    header('Location: admin_dashboard.php');
    exit;
}

// Map action → new status
$statusMap = [
    'confirm'  => 'confirmed',
    'decline'  => 'declined',
    'complete' => 'completed',
    'cancel'   => 'cancelled',
];
$newStatus = $statusMap[$action];

// ── Update appointment ────────────────────────────────────────────────────
$upd = $pdo->prepare("
    UPDATE appointments
    SET status = ?, admin_notes = ?, updated_at = NOW()
    WHERE id = ?
");
$upd->execute([$newStatus, $adminNotes ?: null, $id]);

// ── P5: Build notification message ────────────────────────────────────────
$dateStr = date('F j, Y', strtotime($appt['appt_date']));
$timeStr = date('g:i A',  strtotime($appt['appt_time']));
$name    = $appt['service_name'];

$notifTypeMap = [
    'confirm'  => 'booking_confirmed',
    'decline'  => 'booking_declined',
    'complete' => 'booking_completed',
    'cancel'   => 'booking_cancelled',
];
$notifType = $notifTypeMap[$action];

$messageMap = [
    'confirm'  => "Great news! Your {$name} appointment on {$dateStr} at {$timeStr} has been confirmed. Please arrive 10 minutes early.",
    'decline'  => "We're sorry — your {$name} appointment on {$dateStr} at {$timeStr} could not be accommodated." . ($adminNotes ? " Reason: {$adminNotes}" : " Please book a new slot."),
    'complete' => "Your {$name} service on {$dateStr} has been marked as completed. Thank you for choosing Maestro Autoworks!",
    'cancel'   => "Your {$name} appointment on {$dateStr} at {$timeStr} has been cancelled." . ($adminNotes ? " Note: {$adminNotes}" : ''),
];
$message = $messageMap[$action];

// Insert notification for the customer
$notifIns = $pdo->prepare("
    INSERT INTO notifications (user_id, appt_id, type, message)
    VALUES (?, ?, ?, ?)
");
$notifIns->execute([$appt['customer_id'], $id, $notifType, $message]);

// Flash feedback for admin
$verbMap = ['confirm'=>'confirmed','decline'=>'declined','complete'=>'completed','cancel'=>'cancelled'];
$_SESSION['flash'] = [
    'type' => 'success',
    'msg'  => "Appointment #{$id} has been {$verbMap[$action]}. Customer notified.",
];

// Return to the right page
$ref = $_SERVER['HTTP_REFERER'] ?? '';
if (str_contains($ref, 'admin_appointments')) {
    header('Location: admin_appointments.php');
} else {
    header('Location: admin_dashboard.php');
}
exit;
?>
