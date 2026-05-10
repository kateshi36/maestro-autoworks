<?php
/**
 * One-time admin password reset script.
 * Open this in your browser ONCE, then DELETE this file immediately.
 * URL: http://localhost/autoworks/reset_admin.php
 */

require_once 'db.php';

$new_password = 'Admin@1234';
$hash = password_hash($new_password, PASSWORD_BCRYPT);

$stmt = $pdo->prepare("UPDATE users SET password = ? WHERE username = 'admin' AND role = 'admin'");
$stmt->execute([$hash]);

if ($stmt->rowCount() > 0) {
    echo "<h2 style='color:green;font-family:sans-serif;'>✅ Admin password updated successfully!</h2>";
    echo "<p style='font-family:sans-serif;'>Username: <strong>admin</strong><br>Password: <strong>Admin@1234</strong></p>";
    echo "<p style='color:red;font-family:sans-serif;'><strong>⚠️ DELETE this file (reset_admin.php) now for security!</strong></p>";
} else {
    echo "<h2 style='color:red;font-family:sans-serif;'>❌ No admin user found. Make sure the DB SQL has been imported first.</h2>";
}
?>
