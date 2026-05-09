<?php

session_start();
require 'db.php';

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username      = trim($_POST['username'] ?? '');
    $password      = $_POST['password'] ?? '';
    $selected_role = $_POST['role'] ?? 'customer'; // 'customer' or 'admin'

    if (empty($username) || empty($password)) {
        $error = 'Please fill in all fields.';
    } else {
        $stmt = $pdo->prepare("SELECT * FROM users WHERE username = ?");
        $stmt->execute([$username]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user && password_verify($password, $user['password'])) {
            // Admin trying to log in as customer — block it
            if ($selected_role === 'customer' && $user['role'] === 'admin') {
                $error = 'Admin accounts must log in using the Admin role.';
            }
            // Customer trying to log in as admin — block it
            elseif ($selected_role === 'admin' && $user['role'] !== 'admin') {
                $error = 'Access denied. Your account does not have admin privileges.';
            }
            // Role matches — allow login
            else {
                $_SESSION['user_id']  = $user['id'];
                $_SESSION['username'] = $user['username'];
                $_SESSION['role']     = $user['role'];

                if ($user['role'] === 'admin') {
                    header('Location: admin_dashboard.php');
                } else {
                    header('Location: home.php');
                }
                exit;
            }
        } else {
            $error = 'Invalid username or password.';
        }
    }
}

$_SESSION['error'] = $error;
header('Location: login.php');
exit;
?>
