<?php
// forgot_password.php — OTP-based password reset (3-step)
// Step 1: Enter email → look up account → send OTP via PHP mail()
// Step 2: Enter 6-digit OTP (5-minute window, resend option)
// Step 3: Set new password

session_start();
require 'db.php';

// If already logged in, redirect away
if (isset($_SESSION['user_id'])) {
    header('Location: home.php');
    exit;
}

// ── Pull any flash messages set by POST handlers below ────────────────────────
$fp_error   = $_SESSION['fp_error']   ?? '';
$fp_success = $_SESSION['fp_success'] ?? '';
$fp_step    = $_SESSION['fp_step']    ?? 1;   // 1 | 2 | 3
$fp_email   = $_SESSION['fp_email']   ?? '';  // masked email shown on step 2
unset($_SESSION['fp_error'], $_SESSION['fp_success']);

// ── POST handler — all three steps submit to this same page ──────────────────
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    // ── STEP 1: Look up account, generate & send OTP ─────────────────────────
    if ($action === 'send_otp') {
        $input = trim($_POST['email_or_phone'] ?? '');

        if (empty($input)) {
            $_SESSION['fp_error'] = 'Please enter your email address.';
            $_SESSION['fp_step']  = 1;
            header('Location: forgot_password.php'); exit;
        }

        // Look up by email OR phone
        $stmt = $pdo->prepare("SELECT * FROM users WHERE email = ? OR phone = ? LIMIT 1");
        $stmt->execute([$input, $input]);
        $user = $stmt->fetch();

        if (!$user) {
            $_SESSION['fp_error'] = 'No account found for that email or phone number.';
            $_SESSION['fp_step']  = 1;
            header('Location: forgot_password.php'); exit;
        }

        if (empty($user['email'])) {
            $_SESSION['fp_error'] = 'This account has no email address on file. Please contact support.';
            $_SESSION['fp_step']  = 1;
            header('Location: forgot_password.php'); exit;
        }

        // Generate 6-digit OTP
        $otp     = str_pad(random_int(100000, 999999), 6, '0', STR_PAD_LEFT);
        $expires = time() + 300; // 5 minutes

        // Store in session (never expose to the page)
        $_SESSION['fp_otp']         = $otp;
        $_SESSION['fp_otp_expires'] = $expires;
        $_SESSION['fp_user_id']     = $user['id'];
        $_SESSION['fp_user_name']   = $user['first_name'];
        $_SESSION['fp_user_email']  = $user['email'];
        $_SESSION['fp_step']        = 2;

        // Mask email for display: j***@gmail.com
        $emailRaw = $user['email'];
        $atPos    = strpos($emailRaw, '@');
        $local    = substr($emailRaw, 0, $atPos);
        $domain   = substr($emailRaw, $atPos);
        $masked   = (strlen($local) <= 1) ? $local : ($local[0] . '***');
        $_SESSION['fp_email'] = $masked . $domain;

        // Send email via PHP mail()
        $to      = $user['email'];
        $name    = $user['first_name'] ?? 'Valued Customer';
        $subject = 'Your Maestro Autoworks Verification Code';
        $body    = "Hi {$name},\r\n\r\n"
                 . "Your Maestro Autoworks password reset code is:\r\n\r\n"
                 . "    {$otp}\r\n\r\n"
                 . "This code expires in 5 minutes. Do not share it with anyone.\r\n\r\n"
                 . "If you did not request this, please ignore this email.\r\n\r\n"
                 . "— Maestro Autoworks Team";
        $headers = "From: noreply@maestroautoworks.ph\r\nX-Mailer: PHP/" . phpversion();

        mail($to, $subject, $body, $headers);
        // NOTE: mail() failure is silently swallowed — user sees step 2 regardless.
        // On a production server, swap mail() for PHPMailer + SMTP (same credentials
        // as the Android app: smtp.gmail.com:587, STARTTLS).

        header('Location: forgot_password.php'); exit;
    }

    // ── STEP 1 (resend): Regenerate OTP and re-send ───────────────────────────
    if ($action === 'resend_otp') {
        if (empty($_SESSION['fp_user_email'])) {
            $_SESSION['fp_step']  = 1;
            $_SESSION['fp_error'] = 'Session expired. Please start again.';
            header('Location: forgot_password.php'); exit;
        }

        $otp     = str_pad(random_int(100000, 999999), 6, '0', STR_PAD_LEFT);
        $expires = time() + 300;

        $_SESSION['fp_otp']         = $otp;
        $_SESSION['fp_otp_expires'] = $expires;
        $_SESSION['fp_step']        = 2;

        $to      = $_SESSION['fp_user_email'];
        $name    = $_SESSION['fp_user_name'] ?? 'Valued Customer';
        $subject = 'Your Maestro Autoworks Verification Code (Resent)';
        $body    = "Hi {$name},\r\n\r\n"
                 . "Here is your new Maestro Autoworks verification code:\r\n\r\n"
                 . "    {$otp}\r\n\r\n"
                 . "This code expires in 5 minutes.\r\n\r\n"
                 . "— Maestro Autoworks Team";
        $headers = "From: noreply@maestroautoworks.ph\r\nX-Mailer: PHP/" . phpversion();
        mail($to, $subject, $body, $headers);

        $_SESSION['fp_success'] = 'A new OTP has been sent to your email.';
        header('Location: forgot_password.php'); exit;
    }

    // ── STEP 2: Verify OTP ────────────────────────────────────────────────────
    if ($action === 'verify_otp') {
        $entered = trim($_POST['otp'] ?? '');

        if (empty($_SESSION['fp_otp'])) {
            $_SESSION['fp_error'] = 'Session expired. Please start again.';
            $_SESSION['fp_step']  = 1;
            unset($_SESSION['fp_otp'], $_SESSION['fp_otp_expires'], $_SESSION['fp_user_id'],
                  $_SESSION['fp_user_name'], $_SESSION['fp_user_email'], $_SESSION['fp_email']);
            header('Location: forgot_password.php'); exit;
        }

        if (time() > ($_SESSION['fp_otp_expires'] ?? 0)) {
            $_SESSION['fp_error'] = 'OTP has expired. Please request a new one.';
            $_SESSION['fp_step']  = 2;
            unset($_SESSION['fp_otp'], $_SESSION['fp_otp_expires']);
            header('Location: forgot_password.php'); exit;
        }

        if ($entered !== $_SESSION['fp_otp']) {
            $_SESSION['fp_error'] = 'Incorrect OTP. Please try again.';
            $_SESSION['fp_step']  = 2;
            header('Location: forgot_password.php'); exit;
        }

        // OTP correct — advance to step 3, invalidate OTP
        unset($_SESSION['fp_otp'], $_SESSION['fp_otp_expires']);
        $_SESSION['fp_step']        = 3;
        $_SESSION['fp_otp_verified'] = true;
        header('Location: forgot_password.php'); exit;
    }

    // ── STEP 3: Set new password ──────────────────────────────────────────────
    if ($action === 'reset_password') {
        if (empty($_SESSION['fp_otp_verified']) || empty($_SESSION['fp_user_id'])) {
            $_SESSION['fp_error'] = 'Session error. Please start the process again.';
            $_SESSION['fp_step']  = 1;
            header('Location: forgot_password.php'); exit;
        }

        $newPass  = $_POST['new_password']     ?? '';
        $confPass = $_POST['confirm_password'] ?? '';

        if (strlen($newPass) < 8) {
            $_SESSION['fp_error'] = 'Password must be at least 8 characters.';
            $_SESSION['fp_step']  = 3;
            header('Location: forgot_password.php'); exit;
        }

        if ($newPass !== $confPass) {
            $_SESSION['fp_error'] = 'Passwords do not match.';
            $_SESSION['fp_step']  = 3;
            header('Location: forgot_password.php'); exit;
        }

        $hashed = password_hash($newPass, PASSWORD_DEFAULT);
        $stmt   = $pdo->prepare("UPDATE users SET password = ? WHERE id = ?");
        $stmt->execute([$hashed, $_SESSION['fp_user_id']]);

        // Clear all forgot-password session keys
        foreach (['fp_step','fp_otp','fp_otp_expires','fp_user_id','fp_user_name',
                  'fp_user_email','fp_email','fp_otp_verified'] as $k) {
            unset($_SESSION[$k]);
        }

        $_SESSION['error'] = ''; // clear any login error
        // Flash a success on the login page
        $_SESSION['fp_reset_done'] = 'Password reset successfully! You can now sign in.';
        header('Location: login.php'); exit;
    }

    // Unknown action — back to step 1
    $_SESSION['fp_step'] = 1;
    header('Location: forgot_password.php'); exit;
}

// Normalise step
$step = (int)($fp_step ?? 1);
if ($step < 1 || $step > 3) $step = 1;

// OTP expiry for JS countdown (seconds remaining)
$otpSecondsLeft = max(0, (int)(($_SESSION['fp_otp_expires'] ?? 0) - time()));
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Maestro Autoworks — Reset Password</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
    <style>
        /* ── Step indicator ───────────────────────────────────────────────── */
        .step-track {
            display: flex;
            align-items: center;
            gap: 0;
            margin-bottom: 32px;
        }

        .step-dot {
            width: 28px; height: 28px;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 13px; font-weight: 800;
            border: 2px solid rgba(255,255,255,0.1);
            background: var(--black-input);
            color: var(--muted);
            flex-shrink: 0;
            transition: all .3s;
            position: relative;
            z-index: 1;
        }

        .step-dot.active {
            border-color: var(--yellow);
            background: var(--yellow);
            color: var(--black);
        }

        .step-dot.done {
            border-color: var(--success);
            background: var(--success);
            color: #fff;
        }

        .step-line {
            flex: 1;
            height: 2px;
            background: rgba(255,255,255,0.07);
            transition: background .3s;
        }

        .step-line.done { background: var(--success); }

        .step-label {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 10px; font-weight: 700;
            letter-spacing: 1.5px; text-transform: uppercase;
            color: var(--muted);
            position: absolute;
            top: 34px; left: 50%;
            transform: translateX(-50%);
            white-space: nowrap;
        }

        .step-dot.active .step-label { color: var(--yellow); }
        .step-dot.done  .step-label  { color: var(--success); }

        .step-dot-wrap {
            position: relative;
            padding-bottom: 22px;
            display: flex; align-items: center; justify-content: center;
        }

        .step-track-outer {
            display: flex;
            align-items: flex-start;
            gap: 0;
            margin-bottom: 40px;
        }

        /* ── OTP input row ────────────────────────────────────────────────── */
        .otp-row {
            display: flex;
            gap: 10px;
            justify-content: center;
            margin-bottom: 20px;
        }

        .otp-cell {
            width: 48px; height: 58px;
            background: var(--black-input);
            border: 2px solid rgba(255,255,255,0.08);
            border-radius: 10px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.6rem; font-weight: 800;
            color: var(--white);
            text-align: center;
            outline: none;
            caret-color: var(--yellow);
            transition: border-color .2s;
        }

        .otp-cell:focus { border-color: var(--yellow); }

        /* Hidden real input (receives the full OTP string) */
        #otp-hidden { display: none; }

        /* ── Countdown ────────────────────────────────────────────────────── */
        .countdown-row {
            display: flex; align-items: center; gap: 8px;
            font-size: 13px; color: var(--muted);
            margin-bottom: 20px; justify-content: center;
        }

        .countdown-timer {
            font-family: 'Barlow Condensed', sans-serif;
            font-weight: 700; font-size: 15px;
            color: var(--yellow);
            min-width: 42px;
        }

        .countdown-timer.expired { color: var(--danger); }

        .resend-link {
            background: none; border: none; cursor: pointer;
            color: var(--yellow); font-size: 13px; font-weight: 600;
            padding: 0; text-decoration: underline;
            font-family: 'Barlow', sans-serif;
            display: none;
        }

        .resend-link:hover { opacity: .75; }

        /* ── Password strength ────────────────────────────────────────────── */
        .strength-bar {
            margin-top: 8px; height: 4px;
            background: rgba(255,255,255,0.07);
            border-radius: 4px; overflow: hidden;
        }

        .strength-fill {
            height: 100%; width: 0%;
            border-radius: 4px;
            transition: width .35s, background-color .35s;
        }

        .match-hint {
            margin-top: 6px;
            font-size: 12px; font-weight: 600;
            min-height: 16px;
            letter-spacing: .3px;
        }

        /* ── Buttons ──────────────────────────────────────────────────────── */
        .btn-primary {
            width: 100%;
            background: var(--yellow); color: var(--black);
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 16px; font-weight: 700;
            letter-spacing: 1.5px; text-transform: uppercase;
            border: none; border-radius: 8px; padding: 15px;
            cursor: pointer;
            display: flex; align-items: center; justify-content: center; gap: 10px;
            transition: background .2s, transform .15s, box-shadow .2s;
            box-shadow: 0 8px 24px rgba(245,166,35,0.25);
            margin-top: 8px;
        }

        .btn-primary:hover {
            background: var(--yellow-dk);
            transform: translateY(-2px);
            box-shadow: 0 12px 32px rgba(245,166,35,0.35);
        }

        .btn-primary:active { transform: translateY(0); }
        .btn-primary:disabled { opacity: .5; cursor: not-allowed; transform: none; }
        .btn-primary svg { width: 16px; height: 16px; fill: var(--black); }

        .btn-ghost {
            width: 100%;
            background: transparent;
            border: 1.5px solid rgba(255,255,255,0.1);
            color: var(--muted);
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 14px; font-weight: 700;
            letter-spacing: 1px; text-transform: uppercase;
            border-radius: 8px; padding: 13px;
            cursor: pointer;
            display: flex; align-items: center; justify-content: center; gap: 8px;
            transition: all .2s;
            margin-top: 10px;
        }

        .btn-ghost:hover { border-color: rgba(255,255,255,0.25); color: var(--text); }
        .btn-ghost svg { width: 14px; height: 14px; fill: currentColor; }

        /* ── OTP sent-to label ────────────────────────────────────────────── */
        .otp-sent-label {
            text-align: center;
            font-size: 14px; color: var(--muted);
            line-height: 1.7;
            margin-bottom: 24px;
        }

        .otp-sent-label strong { color: var(--text); }

        /* ── Success icon for step 3 ──────────────────────────────────────── */
        .success-icon-wrap {
            width: 60px; height: 60px;
            border-radius: 50%;
            background: rgba(76,175,125,0.12);
            border: 2px solid rgba(76,175,125,0.35);
            display: flex; align-items: center; justify-content: center;
            margin: 0 auto 20px;
        }

        .success-icon-wrap svg { width: 28px; height: 28px; fill: var(--success); }

        /* ── Card wider for this page ─────────────────────────────────────── */
        .fp-card { max-width: 440px; }

        input[type="email"] {
            width: 100%;
            background: var(--black-input);
            border: 1.5px solid rgba(255,255,255,0.08);
            border-radius: 8px;
            padding: 13px 14px 13px 44px;
            color: var(--text);
            font-family: 'Barlow', sans-serif;
            font-size: 15px; outline: none;
            transition: border-color .2s, background .2s;
        }
        input[type="email"]:focus {
            border-color: var(--yellow);
            background: rgba(26,26,26,0.9);
        }
        input[type="email"]::placeholder { color: var(--muted); font-size: 14px; }
    </style>
</head>
<body>

<div class="page-wrapper">

    <!-- LEFT PANEL -->
    <div class="left-panel">
        <svg class="gear-bg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="white">
            <path d="M43.1 5.1l-3.2 9.5c-2.1.6-4.1 1.4-6 2.4L25 12.5l-9.9 9.9 4.5 8.9c-1 1.9-1.8 3.9-2.4 6L7.7 40.5v14l9.5 3.2c.6 2.1 1.4 4.1 2.4 6L15.1 72.5l9.9 9.9 8.9-4.5c1.9 1 3.9 1.8 6 2.4l3.2 9.5h14l3.2-9.5c2.1-.6 4.1-1.4 6-2.4l8.9 4.5 9.9-9.9-4.5-8.9c1-1.9 1.8-3.9 2.4-6l9.5-3.2v-14l-9.5-3.2c-.6-2.1-1.4-4.1-2.4-6l4.5-8.9-9.9-9.9-8.9 4.5c-1.9-1-3.9-1.8-6-2.4l-3.2-9.5h-14zm7 20c13.8 0 25 11.2 25 25s-11.2 25-25 25-25-11.2-25-25 11.2-25 25-25zm0 10c-8.3 0-15 6.7-15 15s6.7 15 15 15 15-6.7 15-15-6.7-15-15-15z"/>
        </svg>
        <div class="inner">
            <span class="tag">Account Recovery</span>
            <h2>Forgot Your<br><span>Password?</span></h2>
            <p>No worries. Enter your registered email and we'll send you a one-time code to securely reset it.</p>
            <div class="features">
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                    </div>
                    <span>OTP Sent to Your Email</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                    </div>
                    <span>Expires in 5 Minutes</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                    </div>
                    <span>Secure Password Reset</span>
                </div>
            </div>
        </div>
    </div>

    <!-- RIGHT PANEL -->
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

        <div class="card fp-card">

            <!-- Step indicator -->
            <div class="step-track-outer">
                <div class="step-dot-wrap">
                    <div class="step-dot <?= $step === 1 ? 'active' : 'done' ?>">
                        <?= $step > 1
                            ? '<svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:#fff"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>'
                            : '1' ?>
                        <span class="step-label">Email</span>
                    </div>
                </div>
                <div class="step-line <?= $step > 1 ? 'done' : '' ?>"></div>
                <div class="step-dot-wrap">
                    <div class="step-dot <?= $step === 2 ? 'active' : ($step > 2 ? 'done' : '') ?>">
                        <?= $step > 2
                            ? '<svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:#fff"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>'
                            : '2' ?>
                        <span class="step-label">OTP</span>
                    </div>
                </div>
                <div class="step-line <?= $step > 2 ? 'done' : '' ?>"></div>
                <div class="step-dot-wrap">
                    <div class="step-dot <?= $step === 3 ? 'active' : '' ?>">3
                        <span class="step-label">New Password</span>
                    </div>
                </div>
            </div>

            <!-- Alert -->
            <?php if ($fp_error): ?>
            <div class="alert alert-error">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($fp_error) ?>
            </div>
            <?php endif; ?>

            <?php if ($fp_success): ?>
            <div class="alert alert-success">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/></svg>
                <?= htmlspecialchars($fp_success) ?>
            </div>
            <?php endif; ?>

            <!-- ════════════════════════════════════════════════════════════ -->
            <!-- STEP 1 — Enter email                                        -->
            <!-- ════════════════════════════════════════════════════════════ -->
            <?php if ($step === 1): ?>
            <div class="card-head">
                <div class="card-label">Step 1 of 3</div>
                <div class="card-title">Find Your<br>Account</div>
                <div class="card-sub">Enter the email address linked to your account.</div>
            </div>

            <form method="POST" action="forgot_password.php" id="step1-form">
                <input type="hidden" name="action" value="send_otp">

                <div class="form-group">
                    <label for="email_or_phone">Email Address</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <input type="email" id="email_or_phone" name="email_or_phone"
                               placeholder="your@email.com"
                               autocomplete="email" required>
                    </div>
                </div>

                <button type="submit" class="btn-primary" id="step1-btn">
                    <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                    <span id="step1-label">Send OTP via Email</span>
                </button>

                <a href="login.php" class="btn-ghost">
                    <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                    Back to Login
                </a>
            </form>

            <!-- ════════════════════════════════════════════════════════════ -->
            <!-- STEP 2 — Enter OTP                                          -->
            <!-- ════════════════════════════════════════════════════════════ -->
            <?php elseif ($step === 2): ?>
            <div class="card-head">
                <div class="card-label">Step 2 of 3</div>
                <div class="card-title">Enter<br>Your Code</div>
                <div class="card-sub">We sent a 6-digit code to your email.</div>
            </div>

            <div class="otp-sent-label">
                Code sent to <strong><?= htmlspecialchars($fp_email ?: 'your email') ?></strong>.<br>
                Enter it below — it expires in <strong>5 minutes</strong>.
            </div>

            <form method="POST" action="forgot_password.php" id="step2-form">
                <input type="hidden" name="action" value="verify_otp">
                <input type="hidden" name="otp" id="otp-hidden">

                <!-- 6 individual digit boxes -->
                <div class="otp-row">
                    <?php for ($i = 0; $i < 6; $i++): ?>
                    <input type="text" class="otp-cell"
                           maxlength="1" inputmode="numeric" pattern="[0-9]"
                           autocomplete="one-time-code"
                           data-index="<?= $i ?>">
                    <?php endfor; ?>
                </div>

                <!-- Countdown -->
                <div class="countdown-row">
                    <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:var(--muted)"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                    <span id="countdown" class="countdown-timer"><?php
                        $m = floor($otpSecondsLeft / 60);
                        $s = $otpSecondsLeft % 60;
                        echo sprintf('%d:%02d', $m, $s);
                    ?></span>

                    <!-- Resend via POST form -->
                    <form method="POST" action="forgot_password.php" style="display:inline;">
                        <input type="hidden" name="action" value="resend_otp">
                        <button type="submit" class="resend-link" id="resend-btn">Resend code</button>
                    </form>
                </div>

                <button type="submit" class="btn-primary" id="verify-btn">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Verify Code
                </button>

                <!-- Back to step 1 via POST (clears session properly) -->
                <form method="POST" action="forgot_password.php" style="margin-top:10px;">
                    <input type="hidden" name="action" value="back_to_step1">
                    <button type="button" class="btn-ghost" onclick="backToStep1()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Use a Different Email
                    </button>
                </form>

            </form>

            <!-- ════════════════════════════════════════════════════════════ -->
            <!-- STEP 3 — New password                                       -->
            <!-- ════════════════════════════════════════════════════════════ -->
            <?php elseif ($step === 3): ?>
            <div class="card-head">
                <div class="success-icon-wrap">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                </div>
                <div class="card-label">Step 3 of 3</div>
                <div class="card-title">Set New<br>Password</div>
                <div class="card-sub">Identity verified. Choose a strong new password.</div>
            </div>

            <form method="POST" action="forgot_password.php" id="step3-form">
                <input type="hidden" name="action" value="reset_password">

                <div class="form-group">
                    <label for="new_password">New Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="new_password" name="new_password"
                               placeholder="Min. 8 characters"
                               autocomplete="new-password"
                               oninput="checkStrength(this.value); checkMatch()">
                        <button type="button" class="toggle-pw" onclick="togglePw('new_password','eye1')" title="Show/hide">
                            <svg id="eye1" viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                        </button>
                    </div>
                    <div class="strength-bar"><div class="strength-fill" id="strength-fill"></div></div>
                    <div class="strength-label" id="strength-label" style="margin-top:5px;font-size:11px;font-weight:600;letter-spacing:.5px;text-transform:uppercase;min-height:16px;"></div>
                </div>

                <div class="form-group">
                    <label for="confirm_password">Confirm Password</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                        <input type="password" id="confirm_password" name="confirm_password"
                               placeholder="Repeat your password"
                               autocomplete="new-password"
                               oninput="checkMatch()">
                        <button type="button" class="toggle-pw" onclick="togglePw('confirm_password','eye2')" title="Show/hide">
                            <svg id="eye2" viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                        </button>
                    </div>
                    <div class="match-hint" id="match-hint"></div>
                </div>

                <button type="submit" class="btn-primary">
                    <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                    Reset Password
                </button>
            </form>
            <?php endif; ?>

        </div><!-- .card -->
    </div><!-- .right-panel -->
</div><!-- .page-wrapper -->

<script>
// ── Step 1: Loading state on submit ─────────────────────────────────────────
const step1Form = document.getElementById('step1-form');
if (step1Form) {
    step1Form.addEventListener('submit', () => {
        const btn   = document.getElementById('step1-btn');
        const label = document.getElementById('step1-label');
        btn.disabled    = true;
        label.textContent = 'Sending…';
    });
}

// ── Step 2: OTP cell auto-advance & hidden field assembly ───────────────────
(function () {
    const cells  = document.querySelectorAll('.otp-cell');
    const hidden = document.getElementById('otp-hidden');
    const form   = document.getElementById('step2-form');
    if (!cells.length) return;

    // Auto-focus first cell
    cells[0].focus();

    cells.forEach((cell, i) => {
        cell.addEventListener('input', e => {
            const v = e.target.value.replace(/\D/g, '');
            e.target.value = v.slice(-1);
            if (v && i < cells.length - 1) cells[i + 1].focus();
            assembleOtp();
        });

        cell.addEventListener('keydown', e => {
            if (e.key === 'Backspace' && !e.target.value && i > 0) {
                cells[i - 1].focus();
                cells[i - 1].value = '';
                assembleOtp();
            }
        });

        cell.addEventListener('paste', e => {
            e.preventDefault();
            const paste = (e.clipboardData || window.clipboardData)
                            .getData('text').replace(/\D/g, '').slice(0, 6);
            paste.split('').forEach((ch, j) => {
                if (cells[j]) cells[j].value = ch;
            });
            const nextEmpty = [...cells].findIndex(c => !c.value);
            (nextEmpty >= 0 ? cells[nextEmpty] : cells[cells.length - 1]).focus();
            assembleOtp();
        });
    });

    function assembleOtp() {
        hidden.value = [...cells].map(c => c.value).join('');
    }

    // Submit on last digit
    cells[cells.length - 1].addEventListener('input', () => {
        const otp = [...cells].map(c => c.value).join('');
        if (otp.length === 6) {
            hidden.value = otp;
            // Short delay so user sees the last digit before submit
            setTimeout(() => form.requestSubmit(), 350);
        }
    });
})();

// ── Step 2: Countdown timer ──────────────────────────────────────────────────
(function () {
    const el = document.getElementById('countdown');
    const resendBtn = document.getElementById('resend-btn');
    if (!el) return;

    let remaining = <?= $otpSecondsLeft ?>;

    function tick() {
        if (remaining <= 0) {
            el.textContent = 'Expired';
            el.classList.add('expired');
            if (resendBtn) resendBtn.style.display = 'inline';
            return;
        }
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        el.textContent = m + ':' + String(s).padStart(2, '0');
        remaining--;
        setTimeout(tick, 1000);
    }

    tick();
})();

// ── "Use a Different Email" — clear session via direct link ──────────────────
function backToStep1() {
    // Send a GET request that resets the session to step 1
    window.location.href = 'forgot_password.php?reset=1';
}

// Handle ?reset=1 on GET — only works if PHP reads it (handled below via meta-refresh trick)
// Actually we handle this by detecting it server-side — see PHP above for GET handler.

// ── Step 3: Password strength ────────────────────────────────────────────────
function checkStrength(val) {
    const fill  = document.getElementById('strength-fill');
    const label = document.getElementById('strength-label');
    if (!fill) return;
    const len = val.length;
    let pct, color, text;
    if (len === 0)      { pct = 0;   color = '#333';            text = ''; }
    else if (len < 4)   { pct = 20;  color = 'var(--danger)';   text = 'Too short'; }
    else if (len < 8)   { pct = 45;  color = 'var(--yellow)';   text = 'Fair'; }
    else if (len < 12)  { pct = 70;  color = 'var(--yellow)';   text = 'Good'; }
    else                { pct = 100; color = 'var(--success)';   text = 'Strong'; }
    fill.style.width           = pct + '%';
    fill.style.backgroundColor = color;
    label.textContent          = text;
    label.style.color          = color;
}

function checkMatch() {
    const p1   = document.getElementById('new_password')?.value     || '';
    const p2   = document.getElementById('confirm_password')?.value || '';
    const hint = document.getElementById('match-hint');
    if (!hint) return;
    if (!p2) { hint.textContent = ''; return; }
    if (p1 === p2) {
        hint.textContent  = '✔ Passwords match';
        hint.style.color  = 'var(--success)';
    } else {
        hint.textContent  = '✘ Passwords do not match';
        hint.style.color  = 'var(--danger)';
    }
}

// ── Eye toggle (shared) ──────────────────────────────────────────────────────
function togglePw(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon  = document.getElementById(iconId);
    if (input.type === 'password') {
        input.type = 'text';
        icon.innerHTML = '<path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.65 0 .65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>';
    } else {
        input.type = 'password';
        icon.innerHTML = '<path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>';
    }
}
</script>

<?php
// Handle GET ?reset=1 — clear session and restart at step 1
if (isset($_GET['reset'])) {
    foreach (['fp_step','fp_otp','fp_otp_expires','fp_user_id','fp_user_name',
              'fp_user_email','fp_email','fp_otp_verified','fp_error','fp_success'] as $k) {
        unset($_SESSION[$k]);
    }
    echo '<script>window.location.replace("forgot_password.php");</script>';
}
?>
</body>
</html>
