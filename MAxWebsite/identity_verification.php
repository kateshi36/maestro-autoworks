<?php
// identity_verification.php — Post-login identity verification
// Mirrors IdentityVerificationActivity: Email OTP (6 boxes, countdown, resend, 3 attempts) + Security Question fallback
// Must only be reached via login_process.php redirect; direct access bounces to login.

session_start();
require 'db.php';

// ── Guard: must arrive from login_process with a pending verification session ──
if (empty($_SESSION['iv_user_id'])) {
    header('Location: login.php');
    exit;
}

// ── Pull user data set by login_process ──────────────────────────────────────
$userId       = (int)$_SESSION['iv_user_id'];
$username     = $_SESSION['iv_username']   ?? '';
$firstName    = $_SESSION['iv_first_name'] ?? $username;
$role         = $_SESSION['iv_role']       ?? 'customer';
$email        = $_SESSION['iv_email']      ?? '';

// Masked email for display  (e.g. j***@gmail.com)
function maskEmail(string $e): string {
    if (!str_contains($e, '@')) return 'your email';
    [$local, $domain] = explode('@', $e, 2);
    $masked = strlen($local) <= 1 ? $local : $local[0] . '***';
    return $masked . '@' . $domain;
}
$maskedEmail = maskEmail($email);

// ── OTP config ────────────────────────────────────────────────────────────────
define('OTP_EXPIRY_SECS', 300);   // 5 minutes
define('MAX_ATTEMPTS',    3);
define('RESEND_COOLDOWN', 60);    // seconds before resend allowed

// ── Handle POST actions ───────────────────────────────────────────────────────
$action = $_POST['action'] ?? '';

// ── Action: Send / Resend OTP ─────────────────────────────────────────────────
if ($action === 'send_otp') {
    // Cooldown check
    $lastSent = $_SESSION['iv_otp_sent_at'] ?? 0;
    if (time() - $lastSent < RESEND_COOLDOWN) {
        $_SESSION['iv_msg']      = 'Please wait before requesting another code.';
        $_SESSION['iv_msg_type'] = 'error';
        header('Location: identity_verification.php');
        exit;
    }

    // Generate 6-digit OTP
    $otp = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);

    // Store in session (never expose to page)
    $_SESSION['iv_otp']         = password_hash($otp, PASSWORD_DEFAULT);
    $_SESSION['iv_otp_sent_at'] = time();
    $_SESSION['iv_attempts']    = 0;

    // Send via mail()
    // ── Swap mail() for PHPMailer + SMTP in production ──
    $to      = $email;
    $subject = 'Your Maestro Autoworks Verification Code';
    $body    = "Hi {$firstName},\n\n"
             . "Your one-time verification code is:\n\n"
             . "  {$otp}\n\n"
             . "This code expires in 5 minutes. Do not share it with anyone.\n\n"
             . "If you didn't request this, you can safely ignore this email.\n\n"
             . "— Maestro Autoworks";
    $headers = "From: noreply@maestroautoworks.ph\r\nContent-Type: text/plain; charset=UTF-8";

    $sent = mail($to, $subject, $body, $headers);

    if ($sent) {
        $_SESSION['iv_msg']      = "A 6-digit code was sent to {$maskedEmail}.";
        $_SESSION['iv_msg_type'] = 'success';
    } else {
        // Dev fallback: expose code only on localhost
        $host = $_SERVER['HTTP_HOST'] ?? '';
        if ($host === 'localhost' || $host === '127.0.0.1') {
            $_SESSION['iv_msg']      = "[DEV] mail() failed. OTP is: {$otp}";
        } else {
            $_SESSION['iv_msg']      = 'Failed to send code. Please try again.';
        }
        $_SESSION['iv_msg_type'] = 'error';
    }

    header('Location: identity_verification.php');
    exit;
}

// ── Action: Verify OTP ────────────────────────────────────────────────────────
if ($action === 'verify_otp') {
    // Assemble the 6 boxes
    $entered = '';
    for ($i = 1; $i <= 6; $i++) {
        $entered .= preg_replace('/\D/', '', $_POST["d{$i}"] ?? '');
    }

    $storedHash  = $_SESSION['iv_otp']         ?? '';
    $sentAt      = $_SESSION['iv_otp_sent_at'] ?? 0;
    $attempts    = (int)($_SESSION['iv_attempts']    ?? 0);

    // Lockout check
    if ($attempts >= MAX_ATTEMPTS) {
        $_SESSION['iv_locked'] = true;
        header('Location: identity_verification.php');
        exit;
    }

    // Expiry check
    if (time() - $sentAt > OTP_EXPIRY_SECS) {
        $_SESSION['iv_msg']      = 'Code expired. Please request a new one.';
        $_SESSION['iv_msg_type'] = 'error';
        header('Location: identity_verification.php');
        exit;
    }

    if (strlen($entered) !== 6) {
        $_SESSION['iv_msg']      = 'Please enter all 6 digits.';
        $_SESSION['iv_msg_type'] = 'error';
        header('Location: identity_verification.php');
        exit;
    }

    if (password_verify($entered, $storedHash)) {
        // ── SUCCESS — promote session to fully authenticated ──────────────────
        unset(
            $_SESSION['iv_user_id'],
            $_SESSION['iv_username'],
            $_SESSION['iv_first_name'],
            $_SESSION['iv_role'],
            $_SESSION['iv_email'],
            $_SESSION['iv_otp'],
            $_SESSION['iv_otp_sent_at'],
            $_SESSION['iv_attempts'],
            $_SESSION['iv_locked'],
            $_SESSION['iv_msg'],
            $_SESSION['iv_msg_type']
        );
        $_SESSION['user_id']  = $userId;
        $_SESSION['username'] = $username;
        $_SESSION['role']     = $role;

        header('Location: home.php');
        exit;
    } else {
        $_SESSION['iv_attempts'] = $attempts + 1;
        $left = MAX_ATTEMPTS - $_SESSION['iv_attempts'];

        if ($left <= 0) {
            $_SESSION['iv_locked'] = true;
            $_SESSION['iv_msg']      = 'Too many incorrect attempts.';
            $_SESSION['iv_msg_type'] = 'error';
        } else {
            $_SESSION['iv_msg']      = "Incorrect code. {$left} attempt(s) remaining.";
            $_SESSION['iv_msg_type'] = 'error';
        }
        header('Location: identity_verification.php');
        exit;
    }
}

// ── Action: Security question answer ─────────────────────────────────────────
if ($action === 'security_question') {
    $answer = strtolower(trim($_POST['sq_answer'] ?? ''));
    // Hardcoded fallback — same as app: "What is the name of your first car?" → "maestro"
    if ($answer === 'maestro') {
        unset(
            $_SESSION['iv_user_id'],   $_SESSION['iv_username'],
            $_SESSION['iv_first_name'],  $_SESSION['iv_role'],
            $_SESSION['iv_email'],     $_SESSION['iv_otp'],
            $_SESSION['iv_otp_sent_at'], $_SESSION['iv_attempts'],
            $_SESSION['iv_locked'],    $_SESSION['iv_msg'],
            $_SESSION['iv_msg_type']
        );
        $_SESSION['user_id']  = $userId;
        $_SESSION['username'] = $username;
        $_SESSION['role']     = $role;
        header('Location: home.php');
        exit;
    } else {
        $_SESSION['iv_msg']      = 'Incorrect answer. Please try again.';
        $_SESSION['iv_msg_type'] = 'error';
        $_SESSION['iv_show_sq']  = true;
        header('Location: identity_verification.php');
        exit;
    }
}

// ── Read flash state ──────────────────────────────────────────────────────────
$flashMsg     = $_SESSION['iv_msg']      ?? '';
$flashType    = $_SESSION['iv_msg_type'] ?? '';
$isLocked     = !empty($_SESSION['iv_locked']);
$showSQ       = !empty($_SESSION['iv_show_sq']);
$attempts     = (int)($_SESSION['iv_attempts']    ?? 0);
$attemptsLeft = max(0, MAX_ATTEMPTS - $attempts);
$otpSentAt    = $_SESSION['iv_otp_sent_at'] ?? 0;
$otpAlreadySent = $otpSentAt > 0;
$secondsLeft  = $otpAlreadySent ? max(0, OTP_EXPIRY_SECS - (time() - $otpSentAt)) : 0;
$resendAvailable = (time() - $otpSentAt) >= RESEND_COOLDOWN || !$otpAlreadySent;

unset($_SESSION['iv_msg'], $_SESSION['iv_msg_type'], $_SESSION['iv_show_sq']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Your Identity — Maestro Autoworks</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
    <style>
        /* ── OTP digit boxes ─────────────────────────────────────────────────── */
        .otp-row {
            display: flex;
            gap: 10px;
            justify-content: center;
            margin: 24px 0 10px;
        }

        .otp-box {
            width: 48px; height: 58px;
            background: var(--black-input);
            border: 1.5px solid rgba(255,255,255,0.10);
            border-radius: 10px;
            text-align: center;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.7rem; font-weight: 700;
            color: var(--white);
            caret-color: var(--yellow);
            outline: none;
            transition: border-color .2s, background .2s, transform .15s;
        }

        .otp-box:focus {
            border-color: var(--yellow);
            background: rgba(26,26,26,0.95);
            transform: scale(1.05);
        }

        .otp-box.filled {
            border-color: rgba(245,166,35,0.5);
        }

        /* ── Countdown bar ────────────────────────────────────────────────────── */
        .countdown-bar-track {
            height: 3px;
            background: rgba(255,255,255,0.07);
            border-radius: 3px;
            overflow: hidden;
            margin-bottom: 8px;
        }

        .countdown-bar-fill {
            height: 100%;
            border-radius: 3px;
            background: var(--yellow);
            transition: width 1s linear, background-color .5s;
        }

        .countdown-label {
            font-size: 12px;
            color: var(--muted);
            text-align: center;
            min-height: 18px;
            letter-spacing: .3px;
        }

        /* ── Attempts indicator ───────────────────────────────────────────────── */
        .attempts-row {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            margin: 14px 0 20px;
        }

        .attempt-dot {
            width: 9px; height: 9px;
            border-radius: 50%;
            background: rgba(255,255,255,0.12);
            transition: background .3s;
        }

        .attempt-dot.used { background: var(--danger); }

        .attempts-label {
            font-size: 12px;
            color: var(--muted);
            margin-left: 4px;
        }

        /* ── Action links row ─────────────────────────────────────────────────── */
        .iv-links {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 16px;
        }

        .iv-link-btn {
            background: none;
            border: none;
            padding: 0;
            cursor: pointer;
            font-family: 'Barlow', sans-serif;
            font-size: 13px;
            color: var(--yellow);
            text-decoration: none;
            transition: opacity .2s;
        }

        .iv-link-btn:hover { opacity: .75; }
        .iv-link-btn:disabled { color: var(--muted); cursor: default; opacity: 1; }

        /* ── Masked contact chip ──────────────────────────────────────────────── */
        .contact-chip {
            display: inline-flex;
            align-items: center;
            gap: 7px;
            background: rgba(245,166,35,0.08);
            border: 1px solid rgba(245,166,35,0.25);
            border-radius: 20px;
            padding: 5px 14px;
            font-size: 13px;
            color: var(--yellow);
            font-weight: 600;
            margin: 12px auto 0;
        }

        .contact-chip svg { width: 13px; height: 13px; fill: var(--yellow); }

        /* ── Lockout box ──────────────────────────────────────────────────────── */
        .lockout-box {
            text-align: center;
            padding: 24px 0 8px;
        }

        .lockout-icon {
            width: 56px; height: 56px;
            background: rgba(224,82,82,0.12);
            border: 1px solid rgba(224,82,82,0.3);
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            margin: 0 auto 16px;
        }

        .lockout-icon svg { width: 26px; height: 26px; fill: var(--danger); }

        .lockout-title {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.3rem; font-weight: 800;
            color: var(--danger); margin-bottom: 8px;
        }

        .lockout-sub { font-size: 13px; color: var(--muted); line-height: 1.6; }

        /* ── Security question panel ──────────────────────────────────────────── */
        .sq-panel { padding-top: 4px; }

        .sq-question {
            background: rgba(245,166,35,0.06);
            border: 1px solid rgba(245,166,35,0.2);
            border-radius: 8px;
            padding: 12px 16px;
            font-size: 14px;
            color: var(--text);
            margin-bottom: 18px;
            line-height: 1.55;
        }

        /* ── Send OTP initial state ───────────────────────────────────────────── */
        .send-otp-box {
            text-align: center;
            padding: 12px 0 24px;
        }

        .send-otp-icon {
            width: 64px; height: 64px;
            background: rgba(245,166,35,0.10);
            border: 1px solid rgba(245,166,35,0.25);
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            margin: 0 auto 18px;
        }

        .send-otp-icon svg { width: 30px; height: 30px; fill: var(--yellow); }

        .send-otp-label {
            font-size: 14px; color: var(--muted);
            line-height: 1.6; margin-bottom: 24px;
        }
    </style>
</head>
<body>

<div class="page-wrapper">

    <!-- Left panel -->
    <div class="left-panel">
        <svg class="gear-bg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="white">
            <path d="M43.1 5.1l-3.2 9.5c-2.1.6-4.1 1.4-6 2.4L25 12.5l-9.9 9.9 4.5 8.9c-1 1.9-1.8 3.9-2.4 6L7.7 40.5v14l9.5 3.2c.6 2.1 1.4 4.1 2.4 6L15.1 72.5l9.9 9.9 8.9-4.5c1.9 1 3.9 1.8 6 2.4l3.2 9.5h14l3.2-9.5c2.1-.6 4.1-1.4 6-2.4l8.9 4.5 9.9-9.9-4.5-8.9c1-1.9 1.8-3.9 2.4-6l9.5-3.2v-14l-9.5-3.2c-.6-2.1-1.4-4.1-2.4-6l4.5-8.9-9.9-9.9-8.9 4.5c-1.9-1-3.9-1.8-6-2.4l-3.2-9.5h-14zm7 20c13.8 0 25 11.2 25 25s-11.2 25-25 25-25-11.2-25-25 11.2-25 25-25zm0 10c-8.3 0-15 6.7-15 15s6.7 15 15 15 15-6.7 15-15-6.7-15-15-15z"/>
        </svg>
        <div class="inner">
            <span class="tag">Security Check</span>
            <h2>One Last<br>Step to<br><span>Verify You</span></h2>
            <p>We sent a one-time code to your registered email. Enter it to complete sign-in and keep your account safe.</p>
            <div class="features">
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
                    </div>
                    <span>Protects Your Account</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                    </div>
                    <span>Code Sent to Your Email</span>
                </div>
                <div class="feature-pill">
                    <div class="icon">
                        <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm4.24 16L12 15.45 7.77 18l1.12-4.81-3.73-3.23 4.92-.42L12 5l1.92 4.53 4.92.42-3.73 3.23L16.23 18z"/></svg>
                    </div>
                    <span>Expires in 5 Minutes</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Right panel -->
    <div class="right-panel">

        <div class="top-bar">
            <a href="index.php" class="logo">
                <div class="logo-icon">
                    <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6zm-8-4H7v-2h4v2zm6 0h-4v-2h4v2z"/></svg>
                </div>
                Maestro Autoworks
            </a>
            <a href="logout.php" class="back-link">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                Not you? Sign out
            </a>
        </div>

        <div class="card" style="max-width:440px;">

            <div class="card-head">
                <div class="card-label">Identity Verification</div>
                <div class="card-title" id="panelTitle">
                    <?= $showSQ ? 'Security<br>Question' : 'Verify<br>It\'s You' ?>
                </div>
                <div class="card-sub">
                    Hey <strong><?= htmlspecialchars($firstName) ?></strong> — <?= $showSQ
                        ? 'Answer your security question to continue.'
                        : 'Enter the 6-digit code sent to your email.' ?>
                </div>
            </div>

            <?php if ($flashMsg): ?>
            <div class="alert alert-<?= $flashType === 'success' ? 'success' : 'error' ?>">
                <svg viewBox="0 0 24 24" fill="currentColor">
                    <?= $flashType === 'success'
                        ? '<path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/>'
                        : '<path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>'
                    ?>
                </svg>
                <?= htmlspecialchars($flashMsg) ?>
            </div>
            <?php endif; ?>


            <?php if ($isLocked && !$showSQ): ?>
            <!-- ── LOCKOUT STATE ─────────────────────────────────────────────── -->
            <div class="lockout-box">
                <div class="lockout-icon">
                    <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                </div>
                <div class="lockout-title">Too Many Attempts</div>
                <div class="lockout-sub">You've used all <?= MAX_ATTEMPTS ?> attempts.<br>Use the security question below to verify instead.</div>
            </div>

            <form method="POST" action="identity_verification.php" class="sq-panel" style="margin-top:20px;">
                <input type="hidden" name="action" value="security_question">
                <div class="sq-question">
                    🔒 &nbsp;What is the name of your first car?
                </div>
                <div class="form-group">
                    <label for="sq_answer">Your Answer</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
                        <input type="text" id="sq_answer" name="sq_answer" placeholder="Type your answer" autocomplete="off" required>
                    </div>
                </div>
                <button type="submit" class="btn-login">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Submit Answer
                </button>
            </form>

            <?php elseif ($showSQ): ?>
            <!-- ── SECURITY QUESTION STATE ────────────────────────────────────── -->
            <form method="POST" action="identity_verification.php" class="sq-panel">
                <input type="hidden" name="action" value="security_question">
                <div class="sq-question">
                    🔒 &nbsp;What is the name of your first car?
                </div>
                <div class="form-group">
                    <label for="sq_answer">Your Answer</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
                        <input type="text" id="sq_answer" name="sq_answer" placeholder="Type your answer" autocomplete="off" required>
                    </div>
                </div>
                <button type="submit" class="btn-login">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Submit Answer
                </button>
                <div class="iv-links" style="margin-top:16px;">
                    <a href="identity_verification.php" class="iv-link-btn">← Back to OTP</a>
                </div>
            </form>

            <?php elseif (!$otpAlreadySent): ?>
            <!-- ── INITIAL STATE: OTP not yet sent ────────────────────────────── -->
            <div class="send-otp-box">
                <div class="send-otp-icon">
                    <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                </div>
                <div class="send-otp-label">
                    We'll send a 6-digit code to<br>
                    <div class="contact-chip" style="margin:10px auto 0;">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <?= htmlspecialchars($maskedEmail) ?>
                    </div>
                </div>
                <form method="POST" action="identity_verification.php">
                    <input type="hidden" name="action" value="send_otp">
                    <button type="submit" class="btn-login">
                        <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
                        Send Verification Code
                    </button>
                </form>
            </div>
            <div class="iv-links" style="margin-top:8px;">
                <span></span>
                <button type="button" class="iv-link-btn" onclick="showSQ()">Use security question instead</button>
            </div>

            <?php else: ?>
            <!-- ── OTP ENTRY STATE ─────────────────────────────────────────────── -->

            <div style="text-align:center;">
                <div class="contact-chip">
                    <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                    <?= htmlspecialchars($maskedEmail) ?>
                </div>
            </div>

            <form method="POST" action="identity_verification.php" id="otpForm">
                <input type="hidden" name="action" value="verify_otp">

                <!-- 6 OTP boxes -->
                <div class="otp-row">
                    <?php for ($i = 1; $i <= 6; $i++): ?>
                    <input
                        type="text"
                        class="otp-box"
                        id="d<?= $i ?>"
                        name="d<?= $i ?>"
                        maxlength="1"
                        inputmode="numeric"
                        pattern="[0-9]"
                        autocomplete="one-time-code"
                        <?= $i === 1 ? 'autofocus' : '' ?>
                    >
                    <?php endfor; ?>
                </div>

                <!-- Countdown bar -->
                <div class="countdown-bar-track">
                    <div class="countdown-bar-fill" id="cntBar" style="width:<?= $secondsLeft > 0 ? round(($secondsLeft / OTP_EXPIRY_SECS) * 100) : 0 ?>%;"></div>
                </div>
                <div class="countdown-label" id="cntLabel">
                    <?= $secondsLeft > 0
                        ? 'Code expires in ' . floor($secondsLeft / 60) . ':' . str_pad($secondsLeft % 60, 2, '0', STR_PAD_LEFT)
                        : 'Code expired — request a new one.' ?>
                </div>

                <!-- Attempt dots -->
                <div class="attempts-row">
                    <?php for ($i = 0; $i < MAX_ATTEMPTS; $i++): ?>
                        <div class="attempt-dot <?= $i < $attempts ? 'used' : '' ?>"></div>
                    <?php endfor; ?>
                    <span class="attempts-label"><?= $attemptsLeft ?> attempt<?= $attemptsLeft !== 1 ? 's' : '' ?> left</span>
                </div>

                <button type="submit" class="btn-login" id="verifyBtn">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Verify Code
                </button>
            </form>

            <div class="iv-links">
                <!-- Resend -->
                <form method="POST" action="identity_verification.php" style="margin:0;">
                    <input type="hidden" name="action" value="send_otp">
                    <button
                        type="submit"
                        class="iv-link-btn"
                        id="resendBtn"
                        <?= $resendAvailable ? '' : 'disabled' ?>
                    >
                        <?= $resendAvailable ? 'Resend code' : 'Resend in <span id="resendTimer">' . (RESEND_COOLDOWN - (time() - $otpSentAt)) . '</span>s' ?>
                    </button>
                </form>

                <!-- Security question fallback -->
                <button type="button" class="iv-link-btn" onclick="showSQ()">Use security question</button>
            </div>
            <?php endif; ?>

        </div><!-- /.card -->

    </div><!-- /.right-panel -->
</div><!-- /.page-wrapper -->

<!-- Hidden SQ form (shown via JS without page reload for smooth UX) -->
<?php if (!$showSQ && !$isLocked): ?>
<div id="sqOverlay" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,0.6);z-index:999;align-items:center;justify-content:center;">
    <div style="background:var(--black-card);border:1px solid rgba(255,255,255,0.07);border-radius:16px;padding:42px 40px;width:100%;max-width:400px;margin:24px;animation:fadeUp .3s ease;">
        <div class="card-head">
            <div class="card-label">Security Fallback</div>
            <div class="card-title" style="font-size:1.6rem;">Security<br>Question</div>
            <div class="card-sub">Answer correctly to verify your identity.</div>
        </div>
        <form method="POST" action="identity_verification.php">
            <input type="hidden" name="action" value="security_question">
            <div class="sq-question">🔒 &nbsp;What is the name of your first car?</div>
            <div class="form-group">
                <label for="sq_answer_modal">Your Answer</label>
                <div class="input-wrap">
                    <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>
                    <input type="text" id="sq_answer_modal" name="sq_answer" placeholder="Type your answer" autocomplete="off" required>
                </div>
            </div>
            <button type="submit" class="btn-login">
                <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                Submit Answer
            </button>
        </form>
        <div class="iv-links" style="margin-top:16px;">
            <button type="button" class="iv-link-btn" onclick="hideSQ()">← Back to OTP</button>
        </div>
    </div>
</div>
<?php endif; ?>

<script>
// ── OTP box auto-advance & backspace ─────────────────────────────────────────
(function () {
    const boxes = Array.from(document.querySelectorAll('.otp-box'));
    if (!boxes.length) return;

    boxes.forEach((box, i) => {
        // Paste handler — distribute digits across boxes
        box.addEventListener('paste', e => {
            e.preventDefault();
            const digits = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
            digits.split('').forEach((d, j) => {
                if (boxes[j]) { boxes[j].value = d; boxes[j].classList.add('filled'); }
            });
            const next = boxes[Math.min(digits.length, 5)];
            if (next) next.focus();
        });

        box.addEventListener('input', e => {
            const val = box.value.replace(/\D/g, '');
            box.value = val.slice(-1);
            box.classList.toggle('filled', box.value !== '');
            if (box.value && i < boxes.length - 1) boxes[i + 1].focus();
            // Auto-submit when last box is filled
            if (i === boxes.length - 1 && box.value) {
                const allFilled = boxes.every(b => b.value !== '');
                if (allFilled) document.getElementById('otpForm')?.submit();
            }
        });

        box.addEventListener('keydown', e => {
            if (e.key === 'Backspace' && !box.value && i > 0) {
                boxes[i - 1].value = '';
                boxes[i - 1].classList.remove('filled');
                boxes[i - 1].focus();
            }
            if (e.key === 'ArrowLeft'  && i > 0)              boxes[i - 1].focus();
            if (e.key === 'ArrowRight' && i < boxes.length-1) boxes[i + 1].focus();
        });

        // Only allow digits
        box.addEventListener('keypress', e => {
            if (!/[0-9]/.test(e.key)) e.preventDefault();
        });
    });
})();

// ── Countdown timer ──────────────────────────────────────────────────────────
(function () {
    const bar   = document.getElementById('cntBar');
    const label = document.getElementById('cntLabel');
    if (!bar || !label) return;

    let remaining = <?= (int)$secondsLeft ?>;
    const total   = <?= OTP_EXPIRY_SECS ?>;

    if (remaining <= 0) return;

    const tick = () => {
        if (remaining <= 0) {
            bar.style.width = '0%';
            bar.style.backgroundColor = 'var(--danger)';
            label.textContent = 'Code expired — request a new one.';
            const resend = document.getElementById('resendBtn');
            if (resend) resend.disabled = false;
            return;
        }
        const pct = (remaining / total) * 100;
        bar.style.width = pct + '%';
        // Colour shifts: green → yellow → red
        bar.style.backgroundColor = pct > 50 ? 'var(--yellow)'
                                  : pct > 20 ? '#F5A623'
                                  :            'var(--danger)';
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        label.textContent = 'Code expires in ' + m + ':' + String(s).padStart(2, '0');
        remaining--;
        setTimeout(tick, 1000);
    };
    setTimeout(tick, 1000);
})();

// ── Resend cooldown timer ────────────────────────────────────────────────────
(function () {
    const btn = document.getElementById('resendBtn');
    const timerSpan = document.getElementById('resendTimer');
    if (!btn || !timerSpan) return;

    let cd = parseInt(timerSpan.textContent, 10) || 0;
    if (cd <= 0) return;

    const tick = () => {
        if (cd <= 0) {
            btn.disabled = false;
            btn.innerHTML = 'Resend code';
            return;
        }
        timerSpan.textContent = cd;
        cd--;
        setTimeout(tick, 1000);
    };
    setTimeout(tick, 1000);
})();

// ── Security question overlay ─────────────────────────────────────────────────
function showSQ() {
    const el = document.getElementById('sqOverlay');
    if (el) { el.style.display = 'flex'; document.getElementById('sq_answer_modal')?.focus(); }
    else { window.location = 'identity_verification.php?sq=1'; }
}
function hideSQ() {
    const el = document.getElementById('sqOverlay');
    if (el) el.style.display = 'none';
}
</script>

</body>
</html>
