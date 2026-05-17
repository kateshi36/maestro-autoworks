<?php
// notif_count.php — AJAX endpoint: returns unread notification count for the logged-in user
session_start();
require 'db.php';

header('Content-Type: application/json');

if (!isset($_SESSION['user_id'])) {
    echo json_encode(['count' => 0]);
    exit;
}

$stmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0");
$stmt->execute([$_SESSION['user_id']]);
$count = (int) $stmt->fetchColumn();

echo json_encode(['count' => $count]);
