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
$fuel_type     = trim($_POST['fuel_type']      ?? '');   // 'Gasoline' | 'Diesel'
$notes         = trim($_POST['notes']          ?? '');
$rating        = max(0, min(5, (int)($_POST['rating'] ?? 0)));  // 0 = not rated, 1–5

// vehicle_concerns — checkbox array, joined to comma-separated string (mirrors vehicleConcerns in BookActivity)
$rawConcerns      = $_POST['vehicle_concerns'] ?? [];
$vehicle_concerns = is_array($rawConcerns)
    ? implode(', ', array_map('trim', $rawConcerns))
    : '';

$_SESSION['book_old'] = compact('service_id','appt_date','appt_time','vehicle_make','vehicle_model','vehicle_year','plate_no','fuel_type','notes','vehicle_concerns','rating');

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

// Fuel type — required, must be exactly 'Gasoline' or 'Diesel' (mirrors BookActivity validation)
if (!in_array($fuel_type, ['Gasoline', 'Diesel'], true)) {
    bookError('Please select a fuel type (Gasoline or Diesel).');
}

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

$capacityStmt = $pdo->prepare("
    SELECT COUNT(*) 
    FROM appointments
    WHERE appt_date = ?
    AND status IN ('pending','confirmed')
");

$capacityStmt->execute([$appt_date]);

$currentBookings = $capacityStmt->fetchColumn();

if ($currentBookings >= 5) {
    bookError('Maximum vehicle capacity reached for this date.');
}

// ── OR/CR image upload ────────────────────────────────────────────────────
// Mirrors Appointment.orcrStatus: "Yes (photo captured)" | "No"
$orcr_status     = 'No';
$orcr_image_path = null;

if (!empty($_FILES['orcr_image']['name']) && $_FILES['orcr_image']['error'] === UPLOAD_ERR_OK) {
    $allowed_mime = ['image/jpeg', 'image/png', 'image/webp'];
    $finfo        = new finfo(FILEINFO_MIME_TYPE);
    $mime         = $finfo->file($_FILES['orcr_image']['tmp_name']);

    if (!in_array($mime, $allowed_mime, true)) {
        bookError('Invalid OR/CR file type. Please upload a JPG, PNG, or WEBP image.');
    }
    if ($_FILES['orcr_image']['size'] > 5 * 1024 * 1024) {
        bookError('OR/CR image is too large (max 5 MB).');
    }

    // Save to uploads/orcr/ directory (create if needed)
    $uploadDir = __DIR__ . '/uploads/orcr/';
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    $ext        = pathinfo($_FILES['orcr_image']['name'], PATHINFO_EXTENSION);
    $safeExt    = strtolower(in_array(strtolower($ext), ['jpg','jpeg','png','webp']) ? $ext : 'jpg');
    $filename   = 'orcr_' . $_SESSION['user_id'] . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $safeExt;
    $destPath   = $uploadDir . $filename;

    if (move_uploaded_file($_FILES['orcr_image']['tmp_name'], $destPath)) {
        $orcr_status     = 'Yes (photo captured)';
        $orcr_image_path = 'uploads/orcr/' . $filename;
    } else {
        bookError('Failed to save OR/CR image. Please try again.');
    }
}

// ── Insert appointment ────────────────────────────────────────────────────
$ins = $pdo->prepare("
    INSERT INTO appointments
        (user_id, service_id, vehicle_make, vehicle_model, vehicle_year, plate_no,
         fuel_type, vehicle_concerns, appt_date, appt_time, notes,
         orcr_status, orcr_image_path, rating, status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
");
$ins->execute([
    $me['id'], $service_id, $vehicle_make, $vehicle_model,
    $vehicle_year ?: null, $plate_no ?: null, $fuel_type,
    $vehicle_concerns ?: null,
    $appt_date, $appt_time, $notes ?: null,
    $orcr_status, $orcr_image_path, $rating,
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
