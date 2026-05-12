<?php
// PMS handler — update a repair task's status with timestamp tracking
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }
$meStmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$meStmt->execute([$_SESSION['user_id']]);
$me = $meStmt->fetch();
if (!$me || $me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }
if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: admin_pms.php'); exit; }

$taskId    = (int)($_POST['task_id'] ?? 0);
$apptId    = (int)($_POST['appt_id'] ?? 0);
$newStatus = trim($_POST['status']   ?? '');

$validStatuses = ['pending', 'in_progress', 'testing', 'completed'];
if (!$taskId || !$apptId || !in_array($newStatus, $validStatuses, true)) {
    $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Invalid request.'];
    header('Location: admin_pms.php');
    exit;
}

// Fetch the task's current state
$taskStmt = $pdo->prepare("SELECT * FROM repair_tasks WHERE id = ? AND appt_id = ?");
$taskStmt->execute([$taskId, $apptId]);
$task = $taskStmt->fetch();

if (!$task) {
    $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Task not found.'];
    header('Location: admin_pms.php');
    exit;
}

$now = date('Y-m-d H:i:s');

// Build timestamp fields based on transition
// started_at  → set on first move to in_progress (never overwrite)
// completed_at → set when status = completed, clear if moving back
$startedAt   = $task['started_at'];
$completedAt = $task['completed_at'];

if ($newStatus === 'in_progress' && !$startedAt) {
    $startedAt = $now;
}

if ($newStatus === 'completed') {
    if (!$startedAt) $startedAt = $now;   // edge case: jumped straight to completed
    $completedAt = $now;
} else {
    // Moving back from completed clears the completed timestamp
    $completedAt = null;
}

$pdo->prepare("
    UPDATE repair_tasks
    SET    status       = ?,
           started_at   = ?,
           completed_at = ?,
           updated_at   = ?
    WHERE  id = ?
")->execute([$newStatus, $startedAt, $completedAt, $now, $taskId]);

// ── If this task is now complete, check whether ALL tasks for the job are done
// and fire a 'vehicle_ready' notification to the customer ─────────────────
if ($newStatus === 'completed') {
    $openStmt = $pdo->prepare("
        SELECT COUNT(*) FROM repair_tasks
        WHERE  appt_id = ? AND status != 'completed'
    ");
    $openStmt->execute([$apptId]);
    $openCount = (int)$openStmt->fetchColumn();

    if ($openCount === 0) {
        // Fetch appointment + customer + service details for the notification
        $apptRow = $pdo->prepare("
            SELECT a.*, u.first_name, u.last_name, u.email,
                   s.name AS service_name
            FROM   appointments a
            JOIN   users    u ON u.id = a.user_id
            JOIN   services s ON s.id = a.service_id
            WHERE  a.id = ?
        ");
        $apptRow->execute([$apptId]);
        $appt = $apptRow->fetch();

        if ($appt) {
            $dateStr = date('F j, Y', strtotime($appt['appt_date']));
            $msg     = "Great news, {$appt['first_name']}! All repair tasks for your "
                     . "{$appt['service_name']} (booked for {$dateStr}) are complete. "
                     . "Your vehicle is ready for pickup at Maestro Autoworks!";

            $pdo->prepare("
                INSERT INTO notifications (user_id, appt_id, type, message)
                VALUES (?, ?, 'vehicle_ready', ?)
            ")->execute([$appt['user_id'], $apptId, $msg]);
        }
    }
}

$statusLabel = ucwords(str_replace('_', ' ', $newStatus));
$_SESSION['flash'] = ['type' => 'success',
    'msg' => "Task updated to \"{$statusLabel}\" successfully."];
header('Location: admin_pms.php');
exit;
