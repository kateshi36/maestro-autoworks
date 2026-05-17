<?php

session_start();

$error      = $_SESSION['error']        ?? '';
$fp_success = $_SESSION['fp_reset_done'] ?? '';
unset($_SESSION['error'], $_SESSION['fp_reset_done']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Welcome to Maestro Autoworks! — Sign In</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
    <style>
        .role-selector {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            margin-bottom: 28px;
        }
        .role-option { position: relative; cursor: pointer; }
        .role-option input[type="radio"] { position: absolute; opacity: 0; width: 0; height: 0; }
        .role-card {
            display: flex; flex-direction: column;
            align-items: center; justify-content: center;
            gap: 10px; padding: 18px 12px;
            border-radius: 10px;
            border: 2px solid rgba(255,255,255,0.08);
            background: var(--black-input);
            transition: all .2s; text-align: center; user-select: none;
        }
        .role-card svg { width: 28px; height: 28px; fill: var(--muted); transition: fill .2s; }
        .role-card-label {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 14px; font-weight: 700;
            letter-spacing: 1px; text-transform: uppercase;
            color: var(--muted); transition: color .2s;
        }
        .role-card-sub { font-size: 11px; color: rgba(255,255,255,0.3); line-height: 1.4; }
        .role-option input:checked + .role-card {
            border-color: var(--yellow);
            background: rgba(245,166,35,0.08);
        }
        .role-option input:checked + .role-card svg { fill: var(--yellow); }
        .role-option input:checked + .role-card .role-card-label { color: var(--yellow); }
        .role-card:hover { border-color: rgba(245,166,35,0.4); background: rgba(245,166,35,0.05); }

        .role-section-label {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 11px; font-weight: 700;
            letter-spacing: 2.5px; text-transform: uppercase;
            color: var(--muted); margin-bottom: 14px;
            display: flex; align-items: center; gap: 10px;
        }
        .role-section-label::before {
            content: ''; display: block; width: 16px; height: 2px;
            background: var(--yellow); flex-shrink: 0;
        }

        /* Admin notice strip shown when admin role is selected */
        .admin-notice {
            display: none;
            align-items: flex-start; gap: 10px;
            background: rgba(245,166,35,0.06);
            border: 1px solid rgba(245,166,35,0.25);
            border-radius: 8px; padding: 12px 14px;
            margin-bottom: 20px;
            font-size: 12px; color: var(--muted); line-height: 1.6;
        }
        .admin-notice.visible { display: flex; }
        .admin-notice svg { width: 15px; height: 15px; fill: var(--yellow); flex-shrink: 0; margin-top: 1px; }
    </style>
</head>
<body>

<div class="page-wrapper">

    <div class="left-panel">
        <svg class="gear-bg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="white">
            <path d="M43.1 5.1l-3.2 9.5c-2.1.6-4.1 1.4-6 2.4L25 12.5l-9.9 9.9 4.5 8.9c-1 1.9-1.8 3.9-2.4 6L7.7 40.5v14l9.5 3.2c.6 2.1 1.4 4.1 2.4 6L15.1 72.5l9.9 9.9 8.9-4.5c1.9 1 3.9 1.8 6 2.4l3.2 9.5h14l3.2-9.5c2.1-.6 4.1-1.4 6-2.4l8.9 4.5 9.9-9.9-4.5-8.9c1-1.9 1.8-3.9 2.4-6l9.5-3.2v-14l-9.5-3.2c-.6-2.1-1.4-4.1-2.4-6l4.5-8.9-9.9-9.9-8.9 4.5c-1.9-1-3.9-1.8-6-2.4l-3.2-9.5h-14zm7 20c13.8 0 25 11.2 25 25s-11.2 25-25 25-25-11.2-25-25 11.2-25 25-25zm0 10c-8.3 0-15 6.7-15 15s6.7 15 15 15 15-6.7 15-15-6.7-15-15-15z"/>
        </svg>
        <div class="inner">
            <span class="tag">We are Maestro Autoworks,</span>
            <h2>Your Trusted<br><span>Auto Repair Shop</span></h2>
            <p>Access your appointments, service history, and account details — all in one place.</p>
            <div class="features">
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 4l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V7.67L12 5z"/></svg>
                    </div>
                    <span>Secure Account Access</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                    </div>
                    <span>Book Appointments</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M13 2.05v2.02c3.95.49 7 3.85 7 7.93 0 3.21-1.81 6-4.72 7.28L13 17v5h5l-1.22-1.22C19.91 19.07 22 15.76 22 12c0-5.18-3.95-9.45-9-9.95zM11 2.05C5.95 2.55 2 6.82 2 12c0 3.76 2.09 7.07 5.22 8.78L6 22h5V2.05zM11 17.92c-3-.49-5-3.07-5-5.92s2-5.43 5-5.92v11.84z"/></svg>
                    </div>
                    <span>Track Service History</span>
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
            <a href="index.php" class="back-link">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                Back to site
            </a>
        </div>

        <div class="card">
            <div class="card-head">
                <div class="card-label">Portal Access</div>
                <div class="card-title" id="card-title">Sign In to<br>Your Account</div>
                <div class="card-sub">Choose your role to continue</div>
            </div>

            <?php if ($error): ?>
            <div class="alert alert-error">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($error) ?>
            </div>
            <?php endif; ?>

            <?php if ($fp_success): ?>
            <div class="alert alert-success">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/></svg>
                <?= htmlspecialchars($fp_success) ?>
            </div>
            <?php endif; ?>

            <!-- Role Selector -->
            <div class="role-section-label">I am a</div>
            <div class="role-selector">
                <label class="role-option">
                    <input type="radio" name="role_choice" value="customer" id="role-customer" checked>
                    <div class="role-card">
                        <svg viewBox="0 0 24 24"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                        <div class="role-card-label">Customer</div>
                        <div class="role-card-sub">Book services &amp; track history</div>
                    </div>
                </label>
                <label class="role-option">
                    <input type="radio" name="role_choice" value="admin" id="role-admin">
                    <div class="role-card">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                        <div class="role-card-label">Admin</div>
                        <div class="role-card-sub">Staff &amp; management access</div>
                    </div>
                </label>
            </div>

            <!-- Admin notice (shown when admin is selected) -->
            <div class="admin-notice" id="admin-notice">
                <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                Admin access is restricted to authorized Maestro Autoworks staff only.
            </div>

            <!-- Single unified form — role is passed as a hidden field -->
            <form method="POST" action="login_process.php">
                <input type="hidden" name="role" id="role-input" value="customer">

                <div class="form-group">
                    <label for="username">Username</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                        <input type="text" id="username" name="username" placeholder="Enter your username" autocomplete="username" required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="password" name="password" placeholder="Enter your password" autocomplete="current-password" required>
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
                        Remember me
                    </label>
                    <a href="forgot_password.php" class="forgot-link">Forgot password?</a>
                </div>

                <button type="submit" class="btn-login" id="submit-btn">
                    <svg viewBox="0 0 24 24"><path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z"/></svg>
                    <span id="submit-label">Sign In</span>
                </button>

            </form>

            <div class="divider">or</div>

            <div class="register-row">
                Don't have an account? <a href="register.php?reset=1">Create one free</a>
            </div>
        </div>

    </div>
</div>

<script>
const radios = document.querySelectorAll('input[name="role_choice"]');
const roleInput = document.getElementById('role-input');
const adminNotice = document.getElementById('admin-notice');
const submitLabel = document.getElementById('submit-label');
const cardTitle = document.getElementById('card-title');

radios.forEach(radio => {
    radio.addEventListener('change', () => {
        roleInput.value = radio.value;
        if (radio.value === 'admin') {
            adminNotice.classList.add('visible');
            submitLabel.textContent = 'Sign In as Admin';
            cardTitle.innerHTML = 'Admin<br>Sign In';
        } else {
            adminNotice.classList.remove('visible');
            submitLabel.textContent = 'Sign In';
            cardTitle.innerHTML = 'Sign In to<br>Your Account';
        }
    });
});

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