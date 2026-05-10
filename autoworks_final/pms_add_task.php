<?php
// PMS handler — add a repair task to an appointment
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }
$meStmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$meStmt->execute([$_SESSION['user_id']]);
$me = $meStmt->fetch();
if (!$me || $me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: admin_pms.php'); exit; }

$apptId   = (int)($_POST['appt_id']    ?? 0);
$taskName = trim($_POST['task_name']   ?? '');
$assignTo = trim($_POST['assigned_to'] ?? '');
$notes    = trim($_POST['notes']       ?? '');

if (!$apptId || !$taskName) {
    $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Task name is required.'];
    header('Location: admin_pms.php');
    exit;
}

// Verify the appointment exists and is confirmed
$appt = $pdo->prepare("SELECT id FROM appointments WHERE id = ? AND status = 'confirmed'");
$appt->execute([$apptId]);
if (!$appt->fetch()) {
    $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Appointment not found or not confirmed.'];
    header('Location: admin_pms.php');
    exit;
}

// Get next sort order for this appointment
$maxStmt = $pdo->prepare("SELECT COALESCE(MAX(sort_order), -1) FROM repair_tasks WHERE appt_id = ?");
$maxStmt->execute([$apptId]);
$nextOrder = (int)$maxStmt->fetchColumn() + 1;

$pdo->prepare("
    INSERT INTO repair_tasks (appt_id, task_name, assigned_to, notes, sort_order)
    VALUES (?, ?, ?, ?, ?)
")->execute([
    $apptId,
    $taskName,
    $assignTo ?: null,
    $notes    ?: null,
    $nextOrder
]);

$_SESSION['flash'] = ['type' => 'success', 'msg' => "Task \"{$taskName}\" added successfully."];
header('Location: admin_pms.php');
exit;
