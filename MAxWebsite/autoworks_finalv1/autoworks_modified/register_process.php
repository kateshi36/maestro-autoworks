<?php

session_start();
require 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: register.php');
    exit;
}

$first_name       = trim($_POST['first_name']       ?? '');
$last_name        = trim($_POST['last_name']        ?? '');
$username         = trim($_POST['username']         ?? '');
$email            = trim($_POST['email']            ?? '');
$password         = $_POST['password']              ?? '';
$confirm_password = $_POST['confirm_password']      ?? '';
$terms            = isset($_POST['terms']);

$_SESSION['reg_old'] = compact('first_name', 'last_name', 'username', 'email');

if (empty($first_name) || empty($last_name) || empty($username) || empty($email) || empty($password)) {
    $_SESSION['reg_error'] = 'Please fill in all fields.';
    header('Location: register.php');
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    $_SESSION['reg_error'] = 'Please enter a valid email address.';
    header('Location: register.php');
    exit;
}

if (strlen($password) < 8) {
    $_SESSION['reg_error'] = 'Password must be at least 8 characters.';
    header('Location: register.php');
    exit;
}

if ($password !== $confirm_password) {
    $_SESSION['reg_error'] = 'Passwords do not match.';
    header('Location: register.php');
    exit;
}

if (!$terms) {
    $_SESSION['reg_error'] = 'You must agree to the Terms & Conditions.';
    header('Location: register.php');
    exit;
}

$stmt = $pdo->prepare("SELECT id FROM users WHERE username = ? OR email = ?");
$stmt->execute([$username, $email]);

if ($stmt->fetch()) {
    $_SESSION['reg_error'] = 'Username or email is already taken.';
    header('Location: register.php');
    exit;
}

$hashed = password_hash($password, PASSWORD_DEFAULT);

$stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, username, email, password) VALUES (?, ?, ?, ?, ?)");
$stmt->execute([$first_name, $last_name, $username, $email, $hashed]);

unset($_SESSION['reg_old']);

$_SESSION['reg_success'] = 'Account created successfully! You can now sign in.';
header('Location: register.php');
exit;
?>
