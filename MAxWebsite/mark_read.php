<?php
// mark_read.php — marks all notifications read for the current user.
// Supports both:
//   POST (AJAX from JS panel open) → returns JSON { success: true }
//   GET  (legacy <a href> fallback) → redirects back to referrer
session_start();
require 'db.php';

if (isset($_SESSION['user_id'])) {
    $pdo->prepare("UPDATE notifications SET is_read = 1 WHERE user_id = ?")
        ->execute([$_SESSION['user_id']]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json');
    echo json_encode(['success' => true]);
    exit;
}

// GET fallback
header('Location: ' . ($_SERVER['HTTP_REFERER'] ?? 'dashboard.php'));
exit;
