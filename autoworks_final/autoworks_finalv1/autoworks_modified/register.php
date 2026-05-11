<?php


session_start();

$error   = $_SESSION['reg_error']   ?? '';
$success = $_SESSION['reg_success'] ?? '';
$old     = $_SESSION['reg_old']     ?? [];

unset($_SESSION['reg_error'], $_SESSION['reg_success'], $_SESSION['reg_old']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Maestro Autoworks — Create Account</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
</head>
<body>

<div class="page-wrapper">

    <div class="left-panel">
        <svg class="gear-bg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="white">
            <path d="M43.1 5.1l-3.2 9.5c-2.1.6-4.1 1.4-6 2.4L25 12.5l-9.9 9.9 4.5 8.9c-1 1.9-1.8 3.9-2.4 6L7.7 40.5v14l9.5 3.2c.6 2.1 1.4 4.1 2.4 6L15.1 72.5l9.9 9.9 8.9-4.5c1.9 1 3.9 1.8 6 2.4l3.2 9.5h14l3.2-9.5c2.1-.6 4.1-1.4 6-2.4l8.9 4.5 9.9-9.9-4.5-8.9c1-1.9 1.8-3.9 2.4-6l9.5-3.2v-14l-9.5-3.2c-.6-2.1-1.4-4.1-2.4-6l4.5-8.9-9.9-9.9-8.9 4.5c-1.9-1-3.9-1.8-6-2.4l-3.2-9.5h-14zm7 20c13.8 0 25 11.2 25 25s-11.2 25-25 25-25-11.2-25-25 11.2-25 25-25zm0 10c-8.3 0-15 6.7-15 15s6.7 15 15 15 15-6.7 15-15-6.7-15-15-15z"/>
        </svg>

        <div class="inner">
            <span class="tag">Join Us Today</span>
            <h2>Get Full<br><span>Access</span> to<br>Our Services</h2>
            <p>Create a free account, and enjoy seamless booking and service tracking.</p>

            <div class="features">
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/></svg>
                    </div>
                    <span>Easy Online Booking</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M20 8h-3V4H3c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34 3 3 3s3-1.34 3-3h2v-5l-3-4zM6 18.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm13.5-9l1.96 2.5H17V9.5h2.5zm-1.5 9c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>
                    </div>
                    <span>Real-Time Service Updates</span>
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
                Back to login
            </a>
        </div>

        <div class="card reg-card">
            <div class="card-head">
                <div class="card-label">New Account</div>
                <div class="card-title">Create Your<br>Free Account</div>
                <div class="card-sub">Fill in your details to get started</div>
            </div>

            <?php if ($error): ?>
            <div class="alert alert-error">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($error) ?>
            </div>
            <?php endif; ?>

            <?php if ($success): ?>
            <div class="alert alert-success">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/></svg>
                <?= htmlspecialchars($success) ?>
                <a href="login.php" style="color:inherit;margin-left:6px;font-weight:600;">Sign in →</a>
            </div>
            <?php endif; ?>

            <form method="POST" action="register_process.php" id="reg-form">

                <div class="form-row">
                    <div class="form-group">
                        <label for="first_name">First Name</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                            <input type="text" id="first_name" name="first_name"
                                placeholder="Juan"
                                value="<?= htmlspecialchars($old['first_name'] ?? '') ?>"
                                required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="last_name">Last Name</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                            <input type="text" id="last_name" name="last_name"
                                placeholder="dela Cruz"
                                value="<?= htmlspecialchars($old['last_name'] ?? '') ?>"
                                required>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <label for="username">Username</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z"/></svg>
                        <input type="text" id="username" name="username"
                            placeholder="e.g. juan_dc"
                            value="<?= htmlspecialchars($old['username'] ?? '') ?>"
                            required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="email">Email Address</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <input type="email" id="email" name="email"
                            placeholder="juan@email.com"
                            value="<?= htmlspecialchars($old['email'] ?? '') ?>"
                            required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="password" name="password"
                            placeholder="Min. 8 characters"
                            oninput="checkStrength(this.value)"
                            required>
                        <button type="button" class="toggle-pw" onclick="togglePw('password','eye1')" title="Show/hide">
                            <svg id="eye1" viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                        </button>
                    </div>
                    <!-- Password strength bar -->
                    <div class="strength-bar">
                        <div class="strength-fill" id="strength-fill"></div>
                    </div>
                    <div class="strength-label" id="strength-label"></div>
                </div>

                <div class="form-group">
                    <label for="confirm_password">Confirm Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="confirm_password" name="confirm_password"
                            placeholder="Re-enter password"
                            required>
                        <button type="button" class="toggle-pw" onclick="togglePw('confirm_password','eye2')" title="Show/hide">
                            <svg id="eye2" viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                        </button>
                    </div>
                </div>

                <!-- Terms -->
                <div class="form-group terms-row">
                    <label class="checkbox-wrap">
                        <input type="checkbox" name="terms" required>
                        <div class="custom-check">
                            <svg viewBox="0 0 12 12"><path d="M1 6l4 4L11 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        I agree to the <a href="#" class="forgot-link" style="margin-left:4px;">Terms &amp; Conditions</a>
                    </label>
                </div>

                <button type="submit" class="btn-login">
                    <svg viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                    Create Account
                </button>

            </form>

            <div class="divider">or</div>

            <div class="register-row">
                Already have an account? <a href="login.php">Sign in</a>
            </div>
        </div>

    </div>
</div>

<script>

function togglePw(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon  = document.getElementById(iconId);
    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    icon.innerHTML = isHidden
        ? '<path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>'
        : '<path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>';
}

function checkStrength(val) {
    const fill  = document.getElementById('strength-fill');
    const label = document.getElementById('strength-label');
    let score = 0;
    if (val.length >= 8)              score++;
    if (/[A-Z]/.test(val))            score++;
    if (/[0-9]/.test(val))            score++;
    if (/[^A-Za-z0-9]/.test(val))     score++;

    const levels = [
        { pct: '0%',   color: 'transparent', text: '' },
        { pct: '25%',  color: '#E05252',      text: 'Weak' },
        { pct: '50%',  color: '#F5A623',      text: 'Fair' },
        { pct: '75%',  color: '#6fcf97',      text: 'Good' },
        { pct: '100%', color: '#27AE60',      text: 'Strong' },
    ];

    fill.style.width           = levels[score].pct;
    fill.style.backgroundColor = levels[score].color;
    label.textContent          = levels[score].text;
    label.style.color          = levels[score].color;
}
</script>

</body>
</html>
