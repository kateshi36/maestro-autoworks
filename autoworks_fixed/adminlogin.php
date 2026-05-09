<?php

session_start();

// If already logged in as admin, redirect to dashboard
if (isset($_SESSION['user_id']) && isset($_SESSION['role']) && $_SESSION['role'] === 'admin') {
    header('Location: admin_dashboard.php');
    exit;
}

$error = $_SESSION['error'] ?? '';
unset($_SESSION['error']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Maestro Autoworks — Admin Login</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
    <style>
        /* ── ADMIN BADGE OVERRIDE ── */
        .left-panel { background: #0d0d0d; }
        .left-panel::before {
            background:
                linear-gradient(160deg, rgba(245,166,35,0.1) 0%, transparent 50%),
                repeating-linear-gradient(
                    -45deg,
                    transparent, transparent 40px,
                    rgba(245,166,35,0.04) 40px, rgba(245,166,35,0.04) 41px
                );
        }

        /* Yellow top stripe on right panel for admin */
        .right-panel::before {
            background: var(--yellow);
            height: 4px;
        }

        .admin-badge {
            display: inline-flex; align-items: center; gap: 8px;
            background: rgba(245,166,35,0.12);
            border: 1px solid rgba(245,166,35,0.35);
            border-radius: 6px; padding: 8px 14px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 11px; font-weight: 700;
            letter-spacing: 2.5px; text-transform: uppercase;
            color: var(--yellow); margin-bottom: 18px;
        }
        .admin-badge svg { width: 13px; height: 13px; fill: var(--yellow); }

        .admin-notice {
            display: flex; align-items: flex-start; gap: 10px;
            background: rgba(245,166,35,0.06);
            border: 1px solid rgba(245,166,35,0.2);
            border-radius: 8px; padding: 12px 14px;
            margin-bottom: 24px;
            font-size: 12px; color: var(--muted); line-height: 1.6;
        }
        .admin-notice svg { width: 15px; height: 15px; fill: var(--yellow); flex-shrink: 0; margin-top: 1px; }

        .back-to-login {
            display: inline-flex; align-items: center; gap: 6px;
            font-size: 13px; color: var(--muted);
            text-decoration: none; transition: color .2s;
            margin-top: 18px; justify-content: center; width: 100%;
        }
        .back-to-login:hover { color: var(--yellow); }
        .back-to-login svg { width: 14px; height: 14px; fill: currentColor; }
    </style>
</head>
<body>

<div class="page-wrapper">

    <div class="left-panel">
        <svg class="gear-bg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="white">
            <path d="M43.1 5.1l-3.2 9.5c-2.1.6-4.1 1.4-6 2.4L25 12.5l-9.9 9.9 4.5 8.9c-1 1.9-1.8 3.9-2.4 6L7.7 40.5v14l9.5 3.2c.6 2.1 1.4 4.1 2.4 6L15.1 72.5l9.9 9.9 8.9-4.5c1.9 1 3.9 1.8 6 2.4l3.2 9.5h14l3.2-9.5c2.1-.6 4.1-1.4 6-2.4l8.9 4.5 9.9-9.9-4.5-8.9c1-1.9 1.8-3.9 2.4-6l9.5-3.2v-14l-9.5-3.2c-.6-2.1-1.4-4.1-2.4-6l4.5-8.9-9.9-9.9-8.9 4.5c-1.9-1-3.9-1.8-6-2.4l-3.2-9.5h-14zm7 20c13.8 0 25 11.2 25 25s-11.2 25-25 25-25-11.2-25-25 11.2-25 25-25zm0 10c-8.3 0-15 6.7-15 15s6.7 15 15 15 15-6.7 15-15-6.7-15-15-15z"/>
        </svg>
        <div class="inner">
            <span class="tag">Restricted Area</span>
            <h2>Admin &amp;<br><span>Staff Portal</span></h2>
            <p>For authorized Maestro Autoworks personnel only. Manage appointments, services, and customer records.</p>
            <div class="features">
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 4l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V7.67L12 5z"/></svg>
                    </div>
                    <span>Secure Admin Access</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>
                    </div>
                    <span>Full Dashboard Control</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                    </div>
                    <span>Manage Appointments</span>
                </div>
            </div>
        </div>
    </div>

    <div class="right-panel">

        <div class="top-bar">
            <a href="index.php" class="logo">
                <div class="logo-icon">
                    <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6zm-8-4H7v-2h4v2zm6 0h-4v-2h4v2z"/></svg>
                </div>
                Maestro Autoworks
            </a>
            <a href="login.php" class="back-link">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                Back to Login
            </a>
        </div>

        <div class="card">
            <div class="card-head">
                <div class="admin-badge">
                    <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                    Admin &amp; Staff Only
                </div>
                <div class="card-title">Admin<br>Sign In</div>
                <div class="card-sub">Enter your admin credentials</div>
            </div>

            <?php if ($error): ?>
            <div class="alert alert-error">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($error) ?>
            </div>
            <?php endif; ?>

            <div class="admin-notice">
                <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                This login is for authorized staff only. Unauthorized access attempts are logged and monitored.
            </div>

            <form method="POST" action="login_process.php">
                <input type="hidden" name="admin_login" value="1">

                <div class="form-group">
                    <label for="username">Admin Username</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                        <input type="text" id="username" name="username" placeholder="Enter admin username" autocomplete="username" required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Admin Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="password" name="password" placeholder="Enter admin password" autocomplete="current-password" required>
                        <button type="button" class="toggle-pw" onclick="togglePassword()" title="Show/hide password">
                            <svg id="eye-icon" viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                        </button>
                    </div>
                </div>

                <div class="form-meta">
                    <label class="checkbox-wrap">
                        <input type="checkbox" name="remember">
                        <div class="custom-check">
                            <svg viewBox="0 0 12 12"><path d="M1 6l4 4L11 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        Keep me signed in
                    </label>
                </div>

                <button type="submit" class="btn-login">
                    <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                    Sign In to Admin Panel
                </button>

            </form>

            <a href="login.php" class="back-to-login">
                <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                Not an admin? Go to Customer Login
            </a>

        </div>
    </div>
</div>

<script>
function togglePassword() {
    const input = document.getElementById('password');
    const icon  = document.getElementById('eye-icon');
    if (input.type === 'password') {
        input.type = 'text';
        icon.innerHTML = '<path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>';
    } else {
        input.type = 'password';
        icon.innerHTML = '<path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>';
    }
}
</script>

</body>
</html>
