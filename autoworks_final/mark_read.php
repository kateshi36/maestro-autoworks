<?php
// mark_read.php
session_start();
require 'db.php';
if (isset($_SESSION['user_id'])) {
    $pdo->prepare("UPDATE notifications SET is_read=1 WHERE user_id=?")->execute([$_SESSION['user_id']]);
}
header('Location: ' . ($_SERVER['HTTP_REFERER'] ?? 'dashboard.php'));
exit;
?>
