<?php


session_start();
require 'db.php';

$error = '';
$success = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';


    if (empty($username) || empty($password)) {
        $error = 'Please fill in all fields.';
    } else {

        $stmt = $pdo->prepare("SELECT * FROM users WHERE username = ?");
        $stmt->execute([$username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user && password_verify($password, $user['password'])) {
            $_SESSION['user_id'] = $user['id'];
            $_SESSION['username'] = $user['username'];
            header('Location: dashboard.php'); // Change to your page
            exit;
        } else {
            $error = 'Invalid username or password.';
        }
    }
}

$_SESSION['error'] = $error;
header('Location: login.php');
exit;
?>
