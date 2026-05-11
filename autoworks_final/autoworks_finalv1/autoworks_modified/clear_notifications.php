<?php
// clear_notifications.php
session_start();
require 'db.php';
if (isset($_SESSION['user_id']) && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $pdo->prepare("DELETE FROM notifications WHERE user_id=?")->execute([$_SESSION['user_id']]);
}
header('Location: notifications.php');
exit;
?>
