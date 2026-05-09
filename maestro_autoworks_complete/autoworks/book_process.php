<?php
// P3 + P5: Book Appointment Processor — book_process.php
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: book.php'); exit; }

// ── Fetch current user ────────────────────────────────────────────────────
$userStmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$userStmt->execute([$_SESSION['user_id']]);
$me = $userStmt->fetch();
if (!$me || $me['role'] === 'admin') { header('Location: dashboard.php'); exit; }

// ── Sanitise inputs ───────────────────────────────────────────────────────
$service_id    = (int)($_POST['service_id']    ?? 0);
$appt_date     = trim($_POST['appt_date']      ?? '');
$appt_time     = trim($_POST['appt_time']      ?? '');
$vehicle_make  = trim($_POST['vehicle_make']   ?? '');
$vehicle_model = trim($_POST['vehicle_model']  ?? '');
$vehicle_year  = (int)($_POST['vehicle_year']  ?? 0);
$plate_no      = strtoupper(trim($_POST['plate_no'] ?? ''));
$notes         = trim($_POST['notes']          ?? '');

$_SESSION['book_old'] = compact('service_id','appt_date','appt_time','vehicle_make','vehicle_model','vehicle_year','plate_no','notes');

function bookError(string $msg): void {
    $_SESSION['book_error'] = $msg;
    header('Location: book.php');
    exit;
}

// ── Validation ────────────────────────────────────────────────────────────
if (!$service_id)    bookError('Please select a service.');
if (empty($appt_date)) bookError('Please choose a date.');
if (empty($appt_time)) bookError('Please choose a time slot.');
if (empty($vehicle_make))  bookError('Please enter the vehicle make.');
if (empty($vehicle_model)) bookError('Please enter the vehicle model.');

// Date must not be in the past
if (strtotime($appt_date) < strtotime('today')) bookError('Please choose a future date.');

// No Sundays
$dow = (int)date('w', strtotime($appt_date));
if ($dow === 0) bookError('We are closed on Sundays. Please choose another day.');

// Validate service exists
$svcStmt = $pdo->prepare("SELECT * FROM services WHERE id = ? AND active = 1");
$svcStmt->execute([$service_id]);
$service = $svcStmt->fetch();
if (!$service) bookError('Invalid service selected.');

// No double booking for same user on same date/time
$dbl = $pdo->prepare("
    SELECT id FROM appointments
    WHERE user_id = ? AND appt_date = ? AND appt_time = ?
      AND status NOT IN ('cancelled','declined')
");
$dbl->execute([$me['id'], $appt_date, $appt_time]);
if ($dbl->fetch()) bookError('You already have a booking at this date and time.');

// ── Insert appointment ────────────────────────────────────────────────────
$ins = $pdo->prepare("
    INSERT INTO appointments
        (user_id, service_id, vehicle_make, vehicle_model, vehicle_year, plate_no, appt_date, appt_time, notes, status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
");
$ins->execute([
    $me['id'], $service_id, $vehicle_make, $vehicle_model,
    $vehicle_year ?: null, $plate_no ?: null,
    $appt_date, $appt_time, $notes ?: null,
]);
$apptId = (int)$pdo->lastInsertId();

// ── P5: Notify customer (booking_received) ────────────────────────────────
$dateFormatted = date('F j, Y', strtotime($appt_date));
$timeFormatted = date('g:i A',  strtotime($appt_time));
$customerMsg   = "Your booking for {$service['name']} on {$dateFormatted} at {$timeFormatted} has been received. We'll confirm it within 24 hours.";

$notifIns = $pdo->prepare("
    INSERT INTO notifications (user_id, appt_id, type, message)
    VALUES (?, ?, 'booking_received', ?)
");
$notifIns->execute([$me['id'], $apptId, $customerMsg]);

// ── P5: Notify all admins ─────────────────────────────────────────────────
$adminRows = $pdo->query("SELECT id FROM users WHERE role = 'admin'")->fetchAll();
$adminMsg  = "New booking request from {$me['first_name']} {$me['last_name']}: {$service['name']} on {$dateFormatted} at {$timeFormatted}.";
foreach ($adminRows as $admin) {
    $notifIns->execute([$admin['id'], $apptId, $adminMsg]);
}

// ── Clear old data & redirect ─────────────────────────────────────────────
unset($_SESSION['book_old']);
$_SESSION['flash'] = [
    'type' => 'success',
    'msg'  => "Booking submitted! You'll receive a confirmation soon.",
];
header('Location: dashboard.php');
exit;
?>
