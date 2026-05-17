<?php
session_start();

// ── Reset gates only when explicitly requested ────────────────────────────────
if (isset($_GET['reset'])) {
    unset($_SESSION['captcha_passed'], $_SESSION['license_passed'],
          $_SESSION['captcha_answer'], $_SESSION['captcha_question']);
}

// ── Back from Step 3 → go to Step 2 (unset license only) ─────────────────────
if (isset($_GET['back']) && $_GET['back'] === '2') {
    unset($_SESSION['license_passed']);
}

// ── CAPTCHA: generate a fresh math question if none exists in session ─────────
// Mirrors the app's generateCaptcha() in RegisterActivity.java:
//   random add or subtract of two 1–10 numbers, answer always ≥ 0.
if (empty($_SESSION['captcha_answer'])) {
    $a = random_int(1, 10);
    $b = random_int(1, 10);
    if (rand(0, 1)) {
        $_SESSION['captcha_answer']   = $a + $b;
        $_SESSION['captcha_question'] = "What is {$a} + {$b}?";
    } else {
        if ($a < $b) { [$a, $b] = [$b, $a]; } // ensure a ≥ b so result ≥ 0
        $_SESSION['captcha_answer']   = $a - $b;
        $_SESSION['captcha_question'] = "What is {$a} − {$b}?";
    }
}

// ── Determine which screen to show ───────────────────────────────────────────
// captcha_passed  → Step 1 cleared (math CAPTCHA solved)
// license_passed  → Step 2 cleared (user confirmed they have a valid DL)
$captchaPassed  = !empty($_SESSION['captcha_passed']);
$licensePassed  = !empty($_SESSION['license_passed']);

$error   = $_SESSION['reg_error']   ?? '';
$success = $_SESSION['reg_success'] ?? '';
$old     = $_SESSION['reg_old']     ?? [];
$captchaError = $_SESSION['captcha_error'] ?? '';
$licenseError = $_SESSION['license_error'] ?? '';

unset($_SESSION['reg_error'], $_SESSION['reg_success'], $_SESSION['reg_old'],
      $_SESSION['captcha_error'], $_SESSION['license_error']);
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
    <style>
        .field-error {
            color: var(--danger, #e05252);
            font-size: 12px;
            margin-top: 5px;
            line-height: 1.4;
        }
        .dl-chip-label { transition: border-color .15s, background .15s, color .15s; }

        /* ── Wizard Shell ──────────────────────────────────────────────────── */
        .wizard-card {
            max-width: 480px;
            width: 100%;
            background: var(--black-card);
            border: 1px solid rgba(255,255,255,0.07);
            border-radius: 16px;
            padding: 0;          /* header + body each have own padding */
            overflow: hidden;
        }

        /* Top accent stripe (matches left-panel brand line) */
        .wizard-card::before {
            content: '';
            display: block;
            height: 3px;
            background: linear-gradient(90deg, var(--yellow-dk), var(--yellow), rgba(245,166,35,0.2));
        }

        /* ── Step Progress Bar ─────────────────────────────────────────────── */
        .wiz-progress {
            display: flex;
            align-items: center;
            gap: 0;
            padding: 22px 36px 0;
        }

        .wiz-pip {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 5px;
            position: relative;
            flex: 1;
        }

        /* Connecting line between pips */
        .wiz-pip:not(:last-child)::after {
            content: '';
            position: absolute;
            top: 14px;                         /* centre of the circle */
            left: calc(50% + 14px);
            right: calc(-50% + 14px);
            height: 2px;
            background: rgba(255,255,255,0.08);
            transition: background .35s;
            z-index: 0;
        }
        .wiz-pip.done:not(:last-child)::after {
            background: var(--yellow);
        }

        .wiz-pip-circle {
            width: 28px;
            height: 28px;
            border-radius: 50%;
            border: 2px solid rgba(255,255,255,0.12);
            background: var(--black-input);
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 12px;
            font-weight: 700;
            color: var(--muted);
            transition: border-color .35s, background .35s, color .35s, transform .25s;
            position: relative;
            z-index: 1;
        }
        .wiz-pip.active .wiz-pip-circle {
            border-color: var(--yellow);
            background: var(--yellow);
            color: var(--black);
            transform: scale(1.15);
            box-shadow: 0 0 0 4px rgba(245,166,35,0.18);
        }
        .wiz-pip.done .wiz-pip-circle {
            border-color: var(--yellow);
            background: rgba(245,166,35,0.15);
            color: var(--yellow);
        }
        /* ✔ checkmark inside done pip */
        .wiz-pip.done .wiz-pip-circle::after {
            content: '✓';
            font-size: 12px;
        }
        .wiz-pip.done .wiz-pip-num { display: none; }

        .wiz-pip-label {
            font-size: 9px;
            font-weight: 700;
            letter-spacing: 1px;
            text-transform: uppercase;
            color: var(--muted);
            text-align: center;
            white-space: nowrap;
            transition: color .35s;
        }
        .wiz-pip.active .wiz-pip-label { color: var(--yellow); }
        .wiz-pip.done  .wiz-pip-label { color: rgba(245,166,35,0.6); }

        /* ── Wizard header area (inside card, below progress) ──────────────── */
        .wiz-header {
            padding: 20px 36px 0;
        }
        .wiz-step-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 10px;
        }
        .wiz-step-tag {
            background: var(--yellow);
            color: var(--black);
            font-size: 11px;
            font-weight: 700;
            padding: 3px 10px;
            border-radius: 20px;
            letter-spacing: 1px;
        }
        .wiz-verified-tag {
            font-size: 12px;
            color: var(--success);
            display: flex;
            align-items: center;
            gap: 4px;
        }
        .wiz-title {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.85rem;
            font-weight: 800;
            color: var(--white);
            line-height: 1.1;
            margin-bottom: 6px;
        }
        .wiz-sub {
            font-size: 13px;
            color: var(--muted);
            margin-bottom: 0;
        }

        /* ── Step panels ───────────────────────────────────────────────────── */
        .wiz-body {
            padding: 24px 36px 32px;
        }

        .wizard-step {
            display: none;
            animation: wizFadeIn .28s ease;
        }
        .wizard-step.active {
            display: block;
        }
        @keyframes wizFadeIn {
            from { opacity: 0; transform: translateY(8px); }
            to   { opacity: 1; transform: translateY(0); }
        }

        /* ── Next / Back button bar ────────────────────────────────────────── */
        .wiz-nav {
            display: flex;
            gap: 12px;
            margin-top: 28px;
            align-items: center;
        }
        .wiz-nav-back {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 13px 20px;
            background: transparent;
            border: 1.5px solid rgba(255,255,255,0.12);
            border-radius: 8px;
            color: var(--muted);
            font-family: 'Barlow', sans-serif;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: border-color .2s, color .2s;
            white-space: nowrap;
        }
        .wiz-nav-back:hover {
            border-color: var(--yellow);
            color: var(--yellow);
        }
        .wiz-nav-back svg { width: 14px; height: 14px; fill: currentColor; }

        .wiz-nav-next {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 15px;
            background: rgba(245,166,35,0.18);
            color: rgba(245,166,35,0.4);
            font-family: 'Barlow', sans-serif;
            font-size: 15px;
            font-weight: 700;
            border: 1.5px solid rgba(245,166,35,0.2);
            border-radius: 8px;
            cursor: not-allowed;
            transition: background .2s, transform .15s, color .2s, border-color .2s;
            letter-spacing: .3px;
            pointer-events: none;
        }
        .wiz-nav-next.ready {
            background: var(--yellow);
            color: var(--black);
            border-color: transparent;
            cursor: pointer;
            pointer-events: auto;
        }
        .wiz-nav-next.ready svg { fill: var(--black); }
        .wiz-nav-next.ready:hover  { background: var(--yellow-dk); transform: translateY(-1px); }
        .wiz-nav-next.ready:active { transform: translateY(0); }
        .wiz-nav-next svg { width: 16px; height: 16px; fill: currentColor; }

        /* ── Footer link row ───────────────────────────────────────────────── */
        .wiz-footer {
            padding: 0 36px 28px;
            text-align: center;
            font-size: 13px;
            color: var(--muted);
        }
        .wiz-footer a { color: var(--yellow); font-weight: 600; margin-left: 4px; }

        /* ── Responsive ────────────────────────────────────────────────────── */
        @media (max-width: 500px) {
            .wizard-card { border-radius: 12px; }
            .wiz-progress { padding: 18px 20px 0; }
            .wiz-header   { padding: 16px 20px 0; }
            .wiz-body     { padding: 20px 20px 28px; }
            .wiz-footer   { padding: 0 20px 24px; }
            .wiz-pip-label { display: none; }  /* labels hidden on very small screens */
        }
    </style>
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

        <?php if (!$captchaPassed): ?>
        <!-- ══════════════════════════════════════════════════════════════════
             STEP 1 — CAPTCHA GATE
             Mirrors RegisterActivity.java setupStep1() / generateCaptcha().
             The user must solve the math question before the registration
             form is shown. A correct answer sets $_SESSION['captcha_passed'].
        ══════════════════════════════════════════════════════════════════ -->
        <div class="card reg-card" style="max-width:420px;">
            <div class="card-head">
                <!-- Mini 8-step progress bar for gate pages -->
                <div style="display:flex;align-items:center;gap:0;margin-bottom:18px;">
                    <?php for($i=1;$i<=8;$i++): ?>
                    <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;position:relative;">
                        <?php if($i < 8): ?>
                        <div style="position:absolute;top:10px;left:calc(50% + 10px);right:calc(-50% + 10px);height:2px;
                            background:<?= $i < 1 ? 'var(--yellow)' : 'rgba(255,255,255,0.08)' ?>;z-index:0;"></div>
                        <?php endif; ?>
                        <div style="width:20px;height:20px;border-radius:50%;border:2px solid <?= $i === 1 ? 'var(--yellow)' : 'rgba(255,255,255,0.12)' ?>;
                            background:<?= $i === 1 ? 'var(--yellow)' : 'var(--black-input)' ?>;
                            display:flex;align-items:center;justify-content:center;
                            font-family:'Barlow Condensed',sans-serif;font-size:10px;font-weight:700;
                            color:<?= $i === 1 ? 'var(--black)' : 'var(--muted)' ?>;position:relative;z-index:1;">
                            <?= $i ?>
                        </div>
                    </div>
                    <?php endfor; ?>
                </div>
                <div class="card-label" style="display:flex;align-items:center;gap:8px;margin-bottom:12px;">
                    <span style="background:var(--yellow);color:var(--black);font-size:11px;font-weight:700;
                        padding:3px 10px;border-radius:20px;letter-spacing:1px;">STEP 1 OF 8</span>
                </div>
                <div class="card-title" style="font-size:1.7rem;line-height:1.15;">
                    Verify You're<br>Human
                </div>
                <div class="card-sub" style="margin-top:8px;">
                    Solve the simple math question below to continue.
                </div>
            </div>

            <?php if ($captchaError): ?>
            <div class="alert alert-error" style="margin-bottom:20px;">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($captchaError) ?>
            </div>
            <?php endif; ?>

            <form method="POST" action="register_process.php" id="captcha-form">
                <input type="hidden" name="action" value="captcha">

                <!-- Terms of Service card — mirrors app's layoutStep1 Terms card -->
                <div style="background:var(--black-input);border:1px solid var(--border);
                    border-radius:12px;padding:20px;margin-bottom:20px;">
                    <div style="font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;
                        color:var(--yellow);margin-bottom:14px;">Terms of Service</div>

                    <!-- ToS body — exact text from app's activity_register.xml -->
                    <p style="font-size:13px;color:var(--muted);line-height:1.7;margin:0 0 16px;">
                        By creating an account with Maestro Autoworks, you agree to provide accurate
                        vehicle and identity information, present valid documents upon request, and
                        abide by our appointment and cancellation policies. Falsified details may
                        result in account suspension.
                    </p>

                    <label class="checkbox-wrap" style="align-items:flex-start;gap:12px;">
                        <input type="checkbox" name="terms_step1" id="terms_step1">
                        <div class="custom-check" style="margin-top:1px;flex-shrink:0;">
                            <svg viewBox="0 0 12 12"><path d="M1 6l4 4L11 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        <span style="font-size:13px;color:var(--white);line-height:1.6;">
                            I have read and agree to the Terms of Service
                        </span>
                    </label>
                    <div id="err-terms-step1" style="color:var(--danger);font-size:12px;
                        margin-top:8px;display:none;">Please accept the Terms of Service to continue.</div>
                </div>

                <!-- CAPTCHA question card -->
                <div style="background:var(--black-input);border:1px solid var(--border);
                    border-radius:12px;padding:24px;margin-bottom:24px;text-align:center;">
                    <div style="font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;
                        color:var(--muted);margin-bottom:12px;">Math Verification</div>
                    <div id="captcha-question" style="font-family:'Barlow Condensed',sans-serif;
                        font-size:2rem;font-weight:800;color:var(--white);margin-bottom:4px;">
                        <?= htmlspecialchars($_SESSION['captcha_question']) ?>
                    </div>
                    <div style="font-size:12px;color:var(--muted);">Enter the answer in the field below</div>
                </div>

                <div class="form-group">
                    <label for="captcha_answer">Your Answer</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14l-5-5 1.41-1.41L12 14.17l7.59-7.59L21 8l-9 9z"/></svg>
                        <input type="number" id="captcha_answer" name="captcha_answer"
                            placeholder="Type a number"
                            autocomplete="off"
                            autofocus
                            style="letter-spacing:2px;font-size:1.1rem;"
                            required>
                    </div>
                    <div id="captcha-inline-error" style="color:var(--danger);font-size:12px;
                        margin-top:6px;display:none;">Please enter a number.</div>
                </div>

                <button type="submit" class="btn-login" id="captcha-submit">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Verify &amp; Continue
                </button>
            </form>

            <div style="margin-top:20px;text-align:center;font-size:13px;color:var(--muted);">
                Already have an account?
                <a href="login.php" style="color:var(--yellow);font-weight:600;margin-left:4px;">Sign in</a>
            </div>
        </div>

        <?php elseif (!$licensePassed): ?>
        <!-- ══════════════════════════════════════════════════════════════════
             STEP 2 — DRIVER'S LICENSE GATE
             Mirrors RegisterActivity.java setupStep2() / layoutStep2.
             User must confirm they hold a valid DL before continuing.
             "No" hard-blocks registration (same as the app's layoutLicenseBlocked).
             "Yes" reveals a green prep-strip and enables the Continue button.
             A correct answer sets $_SESSION['license_passed'].
        ══════════════════════════════════════════════════════════════════ -->
        <div class="card reg-card" style="max-width:460px;">
            <div class="card-head">
                <!-- Mini 8-step progress bar for gate pages -->
                <div style="display:flex;align-items:center;gap:0;margin-bottom:18px;">
                    <?php for($i=1;$i<=8;$i++): ?>
                    <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;position:relative;">
                        <?php if($i < 8): ?>
                        <div style="position:absolute;top:10px;left:calc(50% + 10px);right:calc(-50% + 10px);height:2px;
                            background:<?= $i < 2 ? 'var(--yellow)' : 'rgba(255,255,255,0.08)' ?>;z-index:0;"></div>
                        <?php endif; ?>
                        <div style="width:20px;height:20px;border-radius:50%;
                            border:2px solid <?= $i === 2 ? 'var(--yellow)' : ($i < 2 ? 'var(--yellow)' : 'rgba(255,255,255,0.12)') ?>;
                            background:<?= $i === 2 ? 'var(--yellow)' : ($i < 2 ? 'rgba(245,166,35,0.15)' : 'var(--black-input)') ?>;
                            display:flex;align-items:center;justify-content:center;
                            font-family:'Barlow Condensed',sans-serif;font-size:10px;font-weight:700;
                            color:<?= $i === 2 ? 'var(--black)' : ($i < 2 ? 'var(--yellow)' : 'var(--muted)') ?>;
                            position:relative;z-index:1;">
                            <?= $i < 2 ? '✓' : $i ?>
                        </div>
                    </div>
                    <?php endfor; ?>
                </div>
                <div class="card-label" style="display:flex;align-items:center;gap:8px;margin-bottom:12px;">
                    <span style="background:var(--yellow);color:var(--black);font-size:11px;font-weight:700;
                        padding:3px 10px;border-radius:20px;letter-spacing:1px;">STEP 2 OF 8</span>
                    <span style="font-size:12px;color:var(--success);">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style="vertical-align:-2px;">
                            <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                        </svg>
                        Verified
                    </span>
                </div>
                <div class="card-title" style="font-size:1.7rem;line-height:1.15;">
                    License<br>Verification
                </div>
                <div class="card-sub" style="margin-top:8px;">
                    A valid driver's license is required to book services.
                </div>
            </div>

            <?php if ($licenseError): ?>
            <div class="alert alert-error" style="margin-bottom:20px;">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                <?= htmlspecialchars($licenseError) ?>
            </div>
            <?php endif; ?>

            <!-- Gate card — mirrors the app's bg_login_card inner panel -->
            <div style="background:var(--black-input);border:1px solid var(--border);
                border-radius:12px;padding:24px;margin-bottom:20px;">

                <div style="display:flex;align-items:center;gap:10px;margin-bottom:12px;">
                    <span style="font-size:20px;">🪪</span>
                    <span style="font-family:'Barlow Condensed',sans-serif;font-size:1rem;
                        font-weight:700;color:var(--white);">License Verification</span>
                </div>

                <p style="color:var(--muted);font-size:14px;line-height:1.7;margin-bottom:20px;">
                    Maestro Autoworks requires a valid driver's license to book services.
                    This helps us verify your identity and ensure the safety of all our customers.
                </p>

                <p style="color:var(--white);font-size:14px;font-weight:600;margin-bottom:14px;">
                    Do you have a valid driver's license?
                </p>

                <!-- Radio options — styled to match the website's checkbox-wrap aesthetic -->
                <div style="display:flex;flex-direction:column;gap:10px;" id="license-options">

                    <label id="label-yes" style="display:flex;align-items:center;gap:14px;
                        padding:14px 16px;border-radius:10px;cursor:pointer;
                        border:2px solid var(--border);transition:border-color .2s,background .2s;">
                        <input type="radio" name="has_license" value="yes" id="rb-yes"
                            onchange="licenseSelect('yes')"
                            style="accent-color:var(--yellow);width:18px;height:18px;flex-shrink:0;">
                        <span style="color:var(--white);font-size:14px;">
                            ✔&nbsp; Yes, I have a valid driver's license
                        </span>
                    </label>

                    <label id="label-no" style="display:flex;align-items:center;gap:14px;
                        padding:14px 16px;border-radius:10px;cursor:pointer;
                        border:2px solid var(--border);transition:border-color .2s,background .2s;">
                        <input type="radio" name="has_license" value="no" id="rb-no"
                            onchange="licenseSelect('no')"
                            style="accent-color:var(--danger);width:18px;height:18px;flex-shrink:0;">
                        <span style="color:var(--muted);font-size:14px;">
                            ✘&nbsp; No, I don't have one
                        </span>
                    </label>

                </div>
            </div>

            <!-- Ready strip — hidden until "Yes" is selected (mirrors layoutLicenseReady, #0A2A0A) -->
            <div id="strip-ready" style="display:none;background:#0a2a0a;
                border:1px solid rgba(76,175,125,0.3);border-radius:10px;
                padding:16px;margin-bottom:20px;">
                <div style="font-weight:700;color:var(--success);font-size:14px;margin-bottom:8px;">
                    🪪&nbsp; Please Prepare Your Driver's License
                </div>
                <p style="color:var(--success);font-size:13px;line-height:1.7;margin:0;">
                    Your driver's license will be required in the next steps. Please have it ready — you will need to:<br><br>
                    &bull;&nbsp; Enter your license number and expiry date<br>
                    &bull;&nbsp; Take a clear photo of your license for verification<br><br>
                    Make sure your license is physically on hand before continuing.
                </p>
            </div>

            <!-- Blocked strip — hidden until "No" is selected (mirrors layoutLicenseBlocked, #3A0000) -->
            <div id="strip-blocked" style="display:none;background:#3a0000;
                border:1px solid rgba(224,82,82,0.35);border-radius:10px;
                padding:16px;margin-bottom:20px;">
                <div style="font-weight:700;color:var(--danger);font-size:14px;margin-bottom:8px;">
                    🚫&nbsp; Registration Not Available
                </div>
                <p style="color:var(--danger);font-size:13px;line-height:1.7;margin:0;">
                    A valid driver's license is required to register with Maestro Autoworks.<br><br>
                    Please obtain your driver's license from the Land Transportation Office (LTO) first,
                    then return to complete your registration.
                </p>
            </div>

            <!-- Nav row: Back always visible, Continue lights up when Yes selected -->
            <form method="POST" action="register_process.php" id="license-form">
                <input type="hidden" name="action" value="license">
                <input type="hidden" name="has_license" id="hidden-has-license" value="">
                <div class="wiz-nav" style="margin-top:8px;">
                    <a href="register.php?reset=1" class="wiz-nav-back"
                        style="display:inline-flex;align-items:center;gap:6px;text-decoration:none;white-space:nowrap;padding:13px 20px;">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </a>
                    <button type="submit" class="wiz-nav-next" id="btn-license-continue">
                        Continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>
            </form>

            <div style="margin-top:20px;text-align:center;font-size:13px;color:var(--muted);">
                Already have an account?
                <a href="login.php" style="color:var(--yellow);font-weight:600;margin-left:4px;">Sign in</a>
            </div>
        </div>

        <?php else: ?>
        <!-- ══════════════════════════════════════════════════════════════════
             STEPS 3–8 — Wizard Registration Form
             Only shown after both captcha_passed AND license_passed are set.
             Sub-steps are JS-driven; only the final submit talks to the server.
        ══════════════════════════════════════════════════════════════════ -->
        <div class="wizard-card">

            <!-- ── Progress pip bar ────────────────────────────────────────── -->
            <nav class="wiz-progress" aria-label="Registration steps" id="wiz-progress">
                <?php
                $wizSteps = [
                    1 => 'Verify',
                    2 => 'License',
                    3 => 'Account',
                    4 => 'Vehicle',
                    5 => 'Docs',
                    6 => 'Review',
                    7 => 'Security',
                    8 => 'Confirm',
                ];
                foreach ($wizSteps as $n => $lbl): ?>
                <div class="wiz-pip <?= $n < 3 ? 'done' : ($n === 3 ? 'active' : '') ?>" id="wiz-pip-<?= $n ?>" data-pip="<?= $n ?>">
                    <div class="wiz-pip-circle">
                        <span class="wiz-pip-num"><?= $n ?></span>
                    </div>
                    <span class="wiz-pip-label"><?= $lbl ?></span>
                </div>
                <?php endforeach; ?>
            </nav>

            <!-- ── Dynamic header (title/subtitle swap per step) ───────────── -->
            <div class="wiz-header">
                <div class="wiz-step-badge">
                    <span class="wiz-step-tag" id="wiz-step-tag">STEP 3 OF 8</span>
                    <span class="wiz-verified-tag">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                        </svg>
                        Verified &amp; Licensed
                    </span>
                </div>
                <div class="wiz-title" id="wiz-title">Create Your<br>Free Account</div>
                <div class="wiz-sub"  id="wiz-sub">Fill in your details to get started.</div>
            </div>

            <?php if ($error): ?>
            <div style="padding:16px 36px 0;">
                <div class="alert alert-error">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                    <?= htmlspecialchars($error) ?>
                </div>
            </div>
            <?php endif; ?>

            <?php if ($success): ?>
            <div style="padding:16px 36px 0;">
                <div class="alert alert-success">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/></svg>
                    <?= htmlspecialchars($success) ?>
                    <a href="login.php" style="color:inherit;margin-left:6px;font-weight:600;">Sign in →</a>
                </div>
            </div>
            <?php endif; ?>

            <!-- All step panels live inside one <form> so a single submit ────-->
            <!-- sends every field to register_process.php on Step 8.          -->
            <form method="POST" action="register_process.php" id="reg-form"
                  enctype="multipart/form-data">
                <input type="hidden" name="action" value="register">

            <!-- ══════════════════════════════════════════════════════════════
                 WIZARD BODY — each .wizard-step panel shown/hidden by JS.
                 Sub-stage 1 placeholder: all content in one step-3 wrapper.
                 Sub-stages 2–5 will split fields into their own panels.
            ══════════════════════════════════════════════════════════════ -->
            <div class="wiz-body">
            <div class="wizard-step active" data-step="3" id="wiz-step-3">

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

                <!-- ── Personal details section ──────────────────────────── -->
                <div style="margin:20px 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Personal Information</span>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="birthdate">Date of Birth</label>
                        <!-- No custom SVG icon — date inputs already have a native calendar icon;
                             we style it amber via CSS. Padding-left reduced to match no-icon fields. -->
                        <input type="date" id="birthdate" name="birthdate"
                            style="padding-left:14px;"
                            value="<?= htmlspecialchars($old['birthdate'] ?? '') ?>"
                            max="<?= date('Y-m-d', strtotime('-16 years')) ?>"
                            required>
                    </div>
                    <div class="form-group">
                        <label for="gender">Gender</label>
                        <!-- Custom select wrapper with a chevron icon on the right -->
                        <div class="input-wrap" style="position:relative;">
                            <select id="gender" name="gender" required
                                style="-webkit-appearance:none;-moz-appearance:none;appearance:none;
                                    background:var(--black-input);border:1.5px solid rgba(255,255,255,0.08);
                                    border-radius:8px;padding:13px 40px 13px 14px;
                                    color:var(--text);font-family:'Barlow',sans-serif;
                                    font-size:14px;cursor:pointer;outline:none;width:100%;
                                    transition:border-color .2s,background .2s;">
                                <option value="" disabled <?= empty($old['gender']) ? 'selected' : '' ?>>Select gender</option>
                                <option value="Male"   <?= ($old['gender'] ?? '') === 'Male'   ? 'selected' : '' ?>>Male</option>
                                <option value="Female" <?= ($old['gender'] ?? '') === 'Female' ? 'selected' : '' ?>>Female</option>
                                <option value="Other"  <?= ($old['gender'] ?? '') === 'Other'  ? 'selected' : '' ?>>Other / Prefer not to say</option>
                            </select>
                            <!-- Custom chevron arrow -->
                            <svg viewBox="0 0 24 24" style="position:absolute;right:12px;top:50%;
                                transform:translateY(-50%);width:16px;height:16px;
                                fill:var(--muted);pointer-events:none;">
                                <path d="M7 10l5 5 5-5z"/>
                            </svg>
                        </div>
                    </div>
                </div>

                <!-- ── Contact & address section ─────────────────────────── -->
                <div style="margin:20px 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Contact &amp; Address</span>
                </div>

                <div class="form-group">
                    <label for="email">Email Address</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <input type="email" id="email" name="email"
                            placeholder="juan@email.com"
                            value="<?= htmlspecialchars($old['email'] ?? '') ?>"
                            oninput="checkEmailMatch()"
                            required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="confirm_email">Confirm Email</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                        <input type="email" id="confirm_email" name="confirm_email"
                            placeholder="Re-enter email"
                            value="<?= htmlspecialchars($old['confirm_email'] ?? '') ?>"
                            oninput="checkEmailMatch()"
                            required>
                    </div>
                    <div id="email-match-msg" style="font-size:12px;margin-top:6px;display:none;"></div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="phone">Phone Number</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>
                            <input type="tel" id="phone" name="phone"
                                placeholder="+63 912 345 6789"
                                value="<?= htmlspecialchars($old['phone'] ?? '') ?>"
                                maxlength="20"
                                required>
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <label for="address">Home Address</label>
                    <div class="input-wrap" style="align-items:flex-start;">
                        <svg viewBox="0 0 24 24" style="margin-top:14px;flex-shrink:0;"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
                        <textarea id="address" name="address" rows="2"
                            placeholder="House No., Street, Barangay, City"
                            style="background:transparent;border:none;color:inherit;
                                width:100%;outline:none;font-size:14px;resize:none;
                                padding:12px 0;font-family:inherit;line-height:1.5;"
                            required><?= htmlspecialchars($old['address'] ?? '') ?></textarea>
                    </div>
                </div>

                <!-- ── Step 3 Navigation ───────────────────────────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="window.location.href='register.php?back=2'">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="button" class="wiz-nav-next" id="btn-step3-next" onclick="validateStep3()">
                        Continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>

            </div><!-- /wiz-step-3 -->

            <!-- ══════════════════════════════════════════════════════════
                 STEP 4 — VEHICLE & LICENSE DETAILS
                 Mirrors RegisterActivity.java setupStep4().
                 Sub-section A: Driver's License
                 Sub-section B: Vehicle Information
            ══════════════════════════════════════════════════════════ -->
            <div class="wizard-step" data-step="4" id="wiz-step-4">

                <!-- ── Step 4 section header ───────────────────────────── -->
                <div style="margin:0 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Driver's License Details</span>
                </div>

                <!-- DL Number -->
                <div class="form-group">
                    <label for="drivers_license_no">Driver's License Number</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-9 4h2v2h-2V8zm0 3h2v5h-2v-5zm-4-3h2v7H7V8zm10 7h-2v-2h2v2zm0-3h-2V8h2v4z"/></svg>
                        <input type="text" id="drivers_license_no" name="drivers_license_no"
                            placeholder="e.g. D01-00-123456"
                            value="<?= htmlspecialchars($old['drivers_license_no'] ?? '') ?>"
                            maxlength="15"
                            oninput="this.value=this.value.toUpperCase(); validateField('drivers_license_no')"
                            required>
                    </div>
                    <div id="err-drivers_license_no" class="field-error" style="display:none;">
                        Format: 1 letter, 2 digits, dash, 2 digits, dash, 6 digits — e.g. D01-00-123456
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="drivers_license_issuance">Issuance Date</label>
                            <input type="date" id="drivers_license_issuance" name="drivers_license_issuance"
                        style="padding-left:14px;"
                                value="<?= htmlspecialchars($old['drivers_license_issuance'] ?? '') ?>"
                                max="<?= date('Y-m-d') ?>"
                                min="1960-01-01"
                                onchange="validateField('drivers_license_issuance')"
                                required>
                        <div id="err-drivers_license_issuance" class="field-error" style="display:none;">
                            Issuance date must be today or in the past.
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="drivers_license_expiry">Expiry Date</label>
                            <input type="date" id="drivers_license_expiry" name="drivers_license_expiry"
                        style="padding-left:14px;"
                                value="<?= htmlspecialchars($old['drivers_license_expiry'] ?? '') ?>"
                                min="<?= date('Y-m-d') ?>"
                                max="<?= date('Y-m-d', strtotime('+10 years')) ?>"
                                onchange="validateField('drivers_license_expiry')"
                                required>
                        <div id="err-drivers_license_expiry" class="field-error" style="display:none;">
                            Expiry date must be a future date.
                        </div>
                    </div>
                </div>

                <!-- DL Restriction Codes -->
                <div class="form-group">
                    <label>Restriction Code(s)
                        <span style="font-size:11px;color:var(--muted);font-weight:400;margin-left:4px;">
                            — select at least one
                        </span>
                    </label>
                    <div id="dl-codes-wrap" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:6px;">
                        <?php
                        $savedCodes = array_filter(explode(',', $old['dl_codes'] ?? ''));
                        foreach (['A','A1','B','B1','B2','C','D','BE','CE'] as $code):
                            $checked = in_array($code, $savedCodes) ? 'checked' : '';
                        ?>
                        <label style="display:flex;align-items:center;gap:6px;padding:7px 14px;
                            border:2px solid var(--border);border-radius:20px;cursor:pointer;
                            font-size:13px;font-weight:600;transition:border-color .15s,background .15s;"
                            class="dl-chip-label" data-code="<?= $code ?>">
                            <input type="checkbox" name="dl_codes[]" value="<?= $code ?>" <?= $checked ?>
                                class="dl-chip-cb" style="display:none;"
                                onchange="refreshDlChip(this)">
                            <?= $code ?>
                        </label>
                        <?php endforeach; ?>
                    </div>
                    <div id="err-dl_codes" class="field-error" style="display:none;margin-top:6px;">
                        Please select at least one restriction code.
                    </div>
                </div>

                <!-- Conductor's License (optional) -->
                <div class="form-group" style="margin-top:4px;">
                    <label class="checkbox-wrap">
                        <input type="checkbox" id="cb_conductors" name="has_conductors"
                            value="1" onchange="toggleConductors(this.checked)"
                            <?= !empty($old['has_conductors']) ? 'checked' : '' ?>>
                        <div class="custom-check">
                            <svg viewBox="0 0 12 12"><path d="M1 6l4 4L11 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        I also hold a Conductor's License (optional)
                    </label>
                </div>

                <div id="conductors-block" style="display:<?= !empty($old['has_conductors']) ? 'block' : 'none' ?>;">
                    <div class="form-group">
                        <label for="conductors_license_no">Conductor's License Number</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-9 4h2v2h-2V8zm0 3h2v5h-2v-5zm-4-3h2v7H7V8zm10 7h-2v-2h2v2zm0-3h-2V8h2v4z"/></svg>
                            <input type="text" id="conductors_license_no" name="conductors_license_no"
                                placeholder="e.g. C01-00-123456"
                                value="<?= htmlspecialchars($old['conductors_license_no'] ?? '') ?>"
                                maxlength="15"
                                oninput="this.value=this.value.toUpperCase(); validateField('conductors_license_no')">
                        </div>
                        <div id="err-conductors_license_no" class="field-error" style="display:none;">
                            Format: 1 letter, 2 digits, dash, 2 digits, dash, 6 digits
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="conductors_license_issuance">Issuance Date</label>
                                <input type="date" id="conductors_license_issuance" name="conductors_license_issuance"
                        style="padding-left:14px;"
                                    value="<?= htmlspecialchars($old['conductors_license_issuance'] ?? '') ?>"
                                    max="<?= date('Y-m-d') ?>" min="1960-01-01"
                                    onchange="validateField('conductors_license_no')">
                        </div>
                        <div class="form-group">
                            <label for="conductors_license_expiry">Expiry Date</label>
                                <input type="date" id="conductors_license_expiry" name="conductors_license_expiry"
                        style="padding-left:14px;"
                                    value="<?= htmlspecialchars($old['conductors_license_expiry'] ?? '') ?>"
                                    min="<?= date('Y-m-d') ?>" max="<?= date('Y-m-d', strtotime('+10 years')) ?>"
                                    onchange="validateField('conductors_license_no')">
                        </div>
                    </div>
                </div>

                <!-- ── Vehicle Information sub-section ────────────────────── -->
                <div style="margin:20px 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Vehicle Information</span>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="license_plate">License Plate</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6z"/></svg>
                            <input type="text" id="license_plate" name="license_plate"
                                placeholder="e.g. ABC 1234"
                                value="<?= htmlspecialchars($old['license_plate'] ?? '') ?>"
                                maxlength="9"
                                oninput="this.value=this.value.toUpperCase(); validateField('license_plate')"
                                required>
                        </div>
                        <div id="err-license_plate" class="field-error" style="display:none;">
                            Format: 2–3 letters, space, 4 digits — e.g. ABC 1234
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="mv_file_number">
                            MV File Number
                            <button type="button" id="mv-help-btn"
                                title="Where to find it"
                                onclick="toggleMvHelp()"
                                style="background:none;border:none;cursor:pointer;padding:0 4px;
                                    color:var(--yellow);vertical-align:middle;">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/>
                                </svg>
                            </button>
                        </label>
                        <div id="mv-help-box" style="display:none;background:#0a1a2a;border:1px solid rgba(251,189,35,0.3);
                            border-radius:8px;padding:12px;margin-bottom:10px;font-size:12px;
                            color:var(--muted);line-height:1.6;">
                            Find your <strong style="color:var(--white);">MV File No.</strong> on the top-right of your
                            Official Receipt (OR) or Certificate of Registration (CR).<br>
                            It is a <strong style="color:var(--white);">15-digit number</strong> printed beside the label "MV File No."
                        </div>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
                            <input type="text" id="mv_file_number" name="mv_file_number"
                                placeholder="15-digit LTO number"
                                value="<?= htmlspecialchars($old['mv_file_number'] ?? '') ?>"
                                maxlength="15"
                                inputmode="numeric"
                                oninput="this.value=this.value.replace(/\D/g,''); validateField('mv_file_number')"
                                required>
                        </div>
                        <div id="err-mv_file_number" class="field-error" style="display:none;">
                            Must be exactly 15 digits.
                        </div>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="vehicle_make">Vehicle Make</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6zm-8-4H7v-2h4v2zm6 0h-4v-2h4v2z"/></svg>
                            <input type="text" id="vehicle_make" name="vehicle_make"
                                placeholder="e.g. Toyota"
                                value="<?= htmlspecialchars($old['vehicle_make'] ?? '') ?>"
                                maxlength="40"
                                oninput="validateField('vehicle_make')"
                                required>
                        </div>
                        <div id="err-vehicle_make" class="field-error" style="display:none;">
                            Letters only, 2–40 characters — e.g. Toyota
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="vehicle_model">Vehicle Model</label>
                        <div class="input-wrap">
                            <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6zm-8-4H7v-2h4v2zm6 0h-4v-2h4v2z"/></svg>
                            <input type="text" id="vehicle_model" name="vehicle_model"
                                placeholder="e.g. Vios 1.3 XLE MT"
                                value="<?= htmlspecialchars($old['vehicle_model'] ?? '') ?>"
                                maxlength="60"
                                oninput="validateField('vehicle_model')"
                                required>
                        </div>
                        <div id="err-vehicle_model" class="field-error" style="display:none;">
                            2–60 characters, letters/numbers/spaces — e.g. Vios 1.3 XLE MT
                        </div>
                    </div>
                </div>

                <!-- ── Step 4 Navigation ───────────────────────────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="wizBack()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="button" class="wiz-nav-next" id="btn-step4-next" onclick="validateStep4()">
                        Continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>

            </div><!-- /wiz-step-4 -->

            <!-- ══════════════════════════════════════════════════════════
                 STEP 5 — DOCUMENT UPLOADS
                 Mirrors RegisterActivity.java applyDocUpload() + refreshStep4Continue().
                 All three documents are required before the form can be submitted.
                 Each card shows a live thumbnail + "✔ Uploaded" badge once selected.
            ══════════════════════════════════════════════════════════ -->
            <div class="wizard-step" data-step="5" id="wiz-step-5">

                <!-- ── Step 5 section header ───────────────────────────── -->
                <div style="margin:0 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Document Uploads</span>
                </div>

                <p style="color:var(--muted);font-size:13px;line-height:1.7;margin:0 0 20px;">
                    Upload clear photos of your documents. Accepted formats: JPG, PNG, WEBP — max 5 MB each.
                </p>

                <!-- Three upload cards — DL, OR, CR -->
                <?php
                $docCards = [
                    ['id'=>'dl_upload',  'label'=>"Driver's License",        'icon'=>'🪪',
                     'hint'=>'Front face of your LTO Driver\'s License card.'],
                    ['id'=>'or_upload',  'label'=>'Official Receipt (OR)',    'icon'=>'🧾',
                     'hint'=>'Latest OR issued by LTO for your vehicle.'],
                    ['id'=>'cr_upload',  'label'=>'Certificate of Registration (CR)', 'icon'=>'📋',
                     'hint'=>'Current CR card issued by LTO for your vehicle.'],
                ];
                foreach ($docCards as $doc):
                ?>
                <div class="upload-card" id="card-<?= $doc['id'] ?>"
                     onclick="triggerUpload('<?= $doc['id'] ?>')"
                     style="background:var(--black-input);border:2px dashed var(--border);
                         border-radius:12px;padding:20px;margin-bottom:14px;cursor:pointer;
                         transition:border-color .2s,background .2s;position:relative;">

                    <!-- Hidden real file input -->
                    <input type="file" id="<?= $doc['id'] ?>" name="<?= $doc['id'] ?>"
                        accept="image/jpeg,image/png,image/webp"
                        style="display:none;"
                        onchange="handleDocUpload(this, '<?= $doc['id'] ?>')"
                        required>

                    <!-- Placeholder state (hidden once uploaded) -->
                    <div id="placeholder-<?= $doc['id'] ?>" style="display:flex;align-items:center;gap:16px;">
                        <div style="font-size:32px;flex-shrink:0;"><?= $doc['icon'] ?></div>
                        <div style="flex:1;min-width:0;">
                            <div style="font-weight:700;color:var(--white);font-size:14px;margin-bottom:3px;">
                                <?= $doc['label'] ?>
                            </div>
                            <div style="font-size:12px;color:var(--muted);line-height:1.5;">
                                <?= $doc['hint'] ?>
                            </div>
                            <div style="margin-top:8px;display:inline-flex;align-items:center;gap:6px;
                                font-size:12px;font-weight:600;color:var(--yellow);">
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"/>
                                </svg>
                                Tap to upload
                            </div>
                        </div>
                        <!-- Status badge — idle -->
                        <div id="badge-<?= $doc['id'] ?>" style="flex-shrink:0;font-size:11px;
                            font-weight:700;padding:4px 10px;border-radius:20px;
                            background:rgba(255,255,255,0.06);color:var(--muted);">
                            Required
                        </div>
                    </div>

                    <!-- Preview state (shown once uploaded) -->
                    <div id="preview-<?= $doc['id'] ?>" style="display:none;align-items:center;gap:16px;">
                        <img id="thumb-<?= $doc['id'] ?>"
                            style="width:72px;height:72px;object-fit:cover;border-radius:8px;
                                border:1px solid var(--border);flex-shrink:0;" alt="Preview">
                        <div style="flex:1;min-width:0;">
                            <div style="font-weight:700;color:var(--white);font-size:14px;margin-bottom:3px;">
                                <?= $doc['label'] ?>
                            </div>
                            <div id="fname-<?= $doc['id'] ?>"
                                style="font-size:12px;color:var(--muted);white-space:nowrap;
                                    overflow:hidden;text-overflow:ellipsis;max-width:180px;"></div>
                            <div style="margin-top:6px;font-size:12px;color:var(--muted);">
                                Tap to replace
                            </div>
                        </div>
                        <div id="badge-done-<?= $doc['id'] ?>"
                            style="flex-shrink:0;font-size:11px;font-weight:700;
                                padding:4px 10px;border-radius:20px;
                                background:rgba(76,175,125,0.15);color:var(--success);">
                            ✔&nbsp;Uploaded
                        </div>
                    </div>
                </div>
                <?php endforeach; ?>

                <!-- All-docs-uploaded banner (mirrors refreshStep4Continue layoutAllDocsUploaded) -->
                <div id="banner-all-docs" style="display:none;background:#0a2a0a;
                    border:1px solid rgba(76,175,125,0.3);border-radius:10px;
                    padding:14px 18px;margin-bottom:4px;
                    display:none;align-items:center;gap:12px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="var(--success)">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/>
                    </svg>
                    <span style="color:var(--success);font-size:13px;font-weight:600;">
                        All documents uploaded — you're good to go!
                    </span>
                </div>
                <div id="err-docs" class="field-error" style="display:none;margin-bottom:12px;">
                    Please upload all three documents before submitting.
                </div>

                <!-- ── Step 5 Navigation ───────────────────────────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="wizBack()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="button" class="wiz-nav-next" id="btn-step5-next" onclick="validateStep5()">
                        Continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>

            </div><!-- /wiz-step-5 -->

            <!-- ══════════════════════════════════════════════════════════
                 STEP 6 — REVIEW & CONFIRM
                 Mirrors RegisterActivity.java populateSummary().
                 A live-updating card that reads the form fields in real-time
                 via JS and displays a final summary before the user submits.
                 Goes beyond the app: also shows address, vehicle, doc status.
            ══════════════════════════════════════════════════════════ -->
            <div class="wizard-step" data-step="6" id="wiz-step-6">

                <!-- ── Step 6 section header ───────────────────────────── -->
                <div style="margin:0 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Review Your Information</span>
                </div>

                <p style="color:var(--muted);font-size:13px;line-height:1.7;margin:0 0 16px;">
                    Please review your details before creating your account. Everything updates live as you type.
                </p>

                <!-- Summary card — styled to match the app's bg_login_card aesthetic -->
                <div id="summary-card" style="background:var(--black-input);border:1px solid var(--border);
                    border-radius:14px;padding:22px;margin-bottom:24px;position:relative;overflow:hidden;">

                    <!-- Subtle accent strip at top -->
                    <div style="position:absolute;top:0;left:0;right:0;height:3px;
                        background:linear-gradient(90deg,var(--yellow),rgba(251,189,35,0.2));
                        border-radius:14px 14px 0 0;"></div>

                    <!-- Section: Personal -->
                    <div style="font-size:10px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--yellow);margin-bottom:14px;margin-top:4px;">
                        Personal Information
                    </div>

                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 20px;margin-bottom:18px;">
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Full Name</div>
                            <div id="rv-name" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Username</div>
                            <div id="rv-username" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Date of Birth</div>
                            <div id="rv-birthdate" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Gender</div>
                            <div id="rv-gender" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                    </div>

                    <!-- Divider -->
                    <div style="border-top:1px solid var(--border);margin:14px 0;"></div>

                    <!-- Section: Contact -->
                    <div style="font-size:10px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--yellow);margin-bottom:14px;">
                        Contact &amp; Address
                    </div>

                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 20px;margin-bottom:18px;">
                        <div style="grid-column:1/-1;">
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Email</div>
                            <div id="rv-email" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);word-break:break-all;min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Phone</div>
                            <div id="rv-phone" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Address</div>
                            <div id="rv-address" class="rv-value" style="font-size:13px;font-weight:600;color:var(--white);min-height:18px;line-height:1.5;">—</div>
                        </div>
                    </div>

                    <!-- Divider -->
                    <div style="border-top:1px solid var(--border);margin:14px 0;"></div>

                    <!-- Section: Driver's License -->
                    <div style="font-size:10px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--yellow);margin-bottom:14px;">
                        Driver's License
                    </div>

                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 20px;margin-bottom:18px;">
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">License No.</div>
                            <div id="rv-dl-no" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);font-family:monospace;letter-spacing:1px;min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Restriction Codes</div>
                            <div id="rv-dl-codes" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Issued</div>
                            <div id="rv-dl-issued" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Expires</div>
                            <div id="rv-dl-expiry" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                    </div>

                    <!-- Divider -->
                    <div style="border-top:1px solid var(--border);margin:14px 0;"></div>

                    <!-- Section: Vehicle -->
                    <div style="font-size:10px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--yellow);margin-bottom:14px;">
                        Vehicle
                    </div>

                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 20px;margin-bottom:18px;">
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Make</div>
                            <div id="rv-make" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Model</div>
                            <div id="rv-model" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">Plate No.</div>
                            <div id="rv-plate" class="rv-value" style="font-size:14px;font-weight:600;color:var(--white);font-family:monospace;letter-spacing:1px;min-height:18px;">—</div>
                        </div>
                        <div>
                            <div style="font-size:11px;color:var(--muted);margin-bottom:3px;">MV File No.</div>
                            <div id="rv-mvfile" class="rv-value" style="font-size:13px;font-weight:600;color:var(--white);font-family:monospace;letter-spacing:1px;min-height:18px;word-break:break-all;">—</div>
                        </div>
                    </div>

                    <!-- Divider -->
                    <div style="border-top:1px solid var(--border);margin:14px 0;"></div>

                    <!-- Section: Documents -->
                    <div style="font-size:10px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--yellow);margin-bottom:14px;">
                        Uploaded Documents
                    </div>

                    <div style="display:flex;flex-direction:column;gap:8px;">
                        <div style="display:flex;align-items:center;justify-content:space-between;">
                            <span style="font-size:13px;color:var(--muted);">🪪 Driver's License</span>
                            <span id="rv-doc-dl" style="font-size:12px;font-weight:700;
                                padding:3px 10px;border-radius:20px;
                                background:rgba(255,255,255,0.06);color:var(--muted);">Pending</span>
                        </div>
                        <div style="display:flex;align-items:center;justify-content:space-between;">
                            <span style="font-size:13px;color:var(--muted);">🧾 Official Receipt (OR)</span>
                            <span id="rv-doc-or" style="font-size:12px;font-weight:700;
                                padding:3px 10px;border-radius:20px;
                                background:rgba(255,255,255,0.06);color:var(--muted);">Pending</span>
                        </div>
                        <div style="display:flex;align-items:center;justify-content:space-between;">
                            <span style="font-size:13px;color:var(--muted);">📋 Certificate of Registration (CR)</span>
                            <span id="rv-doc-cr" style="font-size:12px;font-weight:700;
                                padding:3px 10px;border-radius:20px;
                                background:rgba(255,255,255,0.06);color:var(--muted);">Pending</span>
                        </div>
                    </div>

                    <!-- Completeness indicator — shown when all fields + docs are filled -->
                    <div id="rv-complete-strip" style="display:none;margin-top:18px;
                        background:#0a2a0a;border:1px solid rgba(76,175,125,0.3);
                        border-radius:10px;padding:12px 16px;
                        display:none;align-items:center;gap:10px;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="var(--success)">
                            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14l-4-4 1.41-1.41L10 13.17l6.59-6.59L18 8l-8 8z"/>
                        </svg>
                        <span style="color:var(--success);font-size:13px;font-weight:600;">
                            Everything looks good — ready to create your account!
                        </span>
                    </div>
                </div>

                <!-- ── Step 6 Navigation ───────────────────────────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="wizBack()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="button" class="wiz-nav-next ready" onclick="showStep(7)">
                        Looks good, continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>

            </div><!-- /wiz-step-6 -->

            <!-- ══════════════════════════════════════════════════════════
                 STEP 7 — PASSWORD
                 Mirrors RegisterActivity.java setupStep7().
                 Password + Confirm Password with strength bar.
                 Validates min-length and match before advancing.
            ══════════════════════════════════════════════════════════ -->
            <div class="wizard-step" data-step="7" id="wiz-step-7">

                <!-- ── Step 7 section header ───────────────────────────── -->
                <div style="margin:0 0 14px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Account Security</span>
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
                    <div id="pw-match-msg" style="font-size:12px;margin-top:6px;display:none;"></div>
                </div>

                <!-- ── Step 7 Navigation ───────────────────────────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="wizBack()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="button" class="wiz-nav-next" id="btn-step7-next" onclick="validateStep7()">
                        Continue
                        <svg viewBox="0 0 24 24"><path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/></svg>
                    </button>
                </div>

            </div><!-- /wiz-step-7 -->

            <!-- ══════════════════════════════════════════════════════════
                 STEP 8 — TERMS & SUBMIT
                 Mirrors RegisterActivity.java setupStep8().
                 Only step that does a real form POST to register_process.php.
            ══════════════════════════════════════════════════════════ -->
            <div class="wizard-step" data-step="8" id="wiz-step-8">

                <!-- ── Step 8 section header ───────────────────────────── -->
                <div style="margin:0 0 20px;padding-bottom:10px;border-bottom:1px solid var(--border);">
                    <span style="font-size:11px;font-weight:700;letter-spacing:2px;
                        text-transform:uppercase;color:var(--muted);">Terms &amp; Conditions</span>
                </div>

                <!-- ToS card — mirrors app's layoutStep1 Terms card text -->
                <div style="background:var(--black-input);border:1px solid var(--border);
                    border-radius:12px;padding:20px;margin-bottom:20px;">
                    <div style="font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;
                        color:var(--yellow);margin-bottom:14px;">Terms of Service</div>
                    <p style="font-size:13px;color:var(--muted);line-height:1.7;margin:0 0 16px;">
                        By creating an account with Maestro Autoworks, you agree to provide accurate
                        vehicle and identity information, present valid documents upon request, and
                        abide by our appointment and cancellation policies. Falsified details may
                        result in account suspension.
                    </p>
                </div>

                <!-- Terms checkbox -->
                <div class="form-group terms-row">
                    <label class="checkbox-wrap">
                        <input type="checkbox" name="terms" id="terms" required>
                        <div class="custom-check">
                            <svg viewBox="0 0 12 12"><path d="M1 6l4 4L11 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        I have read and agree to the Terms of Service
                    </label>
                    <div id="err-terms" class="field-error" style="display:none;margin-top:6px;">
                        You must agree to the Terms of Service to create an account.
                    </div>
                </div>

                <!-- ── Step 8 Navigation — Back + final submit ─────────── -->
                <div class="wiz-nav">
                    <button type="button" class="wiz-nav-back" onclick="wizBack()">
                        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        Back
                    </button>
                    <button type="submit" class="wiz-nav-next" id="btn-step8-submit" onclick="checkTerms(event)">
                        <svg viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                        Create Account
                    </button>
                </div>

            </div><!-- /wiz-step-8 -->
            </div><!-- /.wiz-body -->

            </form>

            <!-- ── Footer sign-in link ────────────────────────────────────── -->
            <div class="wiz-footer">
                Already have an account? <a href="login.php">Sign in</a>
            </div>

        </div><!-- /.wizard-card -->
        <?php endif; ?>

    </div><!-- /.right-panel -->
</div><!-- /.page-wrapper -->

<script>

// ── Step 2: License Gate — radio listeners ───────────────────────────────────
function licenseSelect(val) {
    var labelYes    = document.getElementById('label-yes');
    var labelNo     = document.getElementById('label-no');
    var stripReady  = document.getElementById('strip-ready');
    var stripBlock  = document.getElementById('strip-blocked');
    var btn         = document.getElementById('btn-license-continue');
    var hidden      = document.getElementById('hidden-has-license');

    if (!btn) return;

    if (val === 'yes') {
        if (labelYes) { labelYes.style.borderColor = 'var(--success)'; labelYes.style.background = 'rgba(76,175,125,0.08)'; }
        if (labelNo)  { labelNo.style.borderColor  = 'var(--border)';  labelNo.style.background  = 'transparent'; }
        if (stripReady)  stripReady.style.display  = 'block';
        if (stripBlock)  stripBlock.style.display  = 'none';
        btn.classList.add('ready');
        if (hidden) hidden.value = 'yes';
    } else {
        if (labelNo)  { labelNo.style.borderColor  = 'var(--danger)';  labelNo.style.background  = 'rgba(224,82,82,0.08)'; }
        if (labelYes) { labelYes.style.borderColor = 'var(--border)';  labelYes.style.background = 'transparent'; }
        if (stripBlock)  stripBlock.style.display  = 'block';
        if (stripReady)  stripReady.style.display  = 'none';
        btn.classList.remove('ready');
        if (hidden) hidden.value = 'no';
    }
}

// ── CAPTCHA form: client-side guard (terms + number-only, non-empty) ──────────
// Mirrors app's setupStep1(): cbTerms checked first, then CAPTCHA validated.
(function () {
    const form     = document.getElementById('captcha-form');
    const input    = document.getElementById('captcha_answer');
    const err      = document.getElementById('captcha-inline-error');
    const termsCb  = document.getElementById('terms_step1');
    const termsErr = document.getElementById('err-terms-step1');
    if (!form || !input) return;

    form.addEventListener('submit', function (e) {
        // 1. Terms must be accepted — mirrors app's cbTerms.isChecked() guard
        if (termsCb && !termsCb.checked) {
            e.preventDefault();
            if (termsErr) termsErr.style.display = 'block';
            termsCb.focus();
            return;
        }
        if (termsErr) termsErr.style.display = 'none';

        // 2. CAPTCHA answer must be a number
        const val = input.value.trim();
        if (val === '' || isNaN(parseInt(val, 10))) {
            e.preventDefault();
            err.style.display = 'block';
            input.focus();
        } else {
            err.style.display = 'none';
        }
    });

    if (termsCb) {
        termsCb.addEventListener('change', function () {
            if (termsErr) termsErr.style.display = 'none';
        });
    }

    input.addEventListener('input', function () {
        err.style.display = 'none';
    });
})();

function togglePw(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon  = document.getElementById(iconId);
    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    icon.innerHTML = isHidden
        ? '<path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>'
        : '<path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>';
}

// ── Step 5: Document upload cards ────────────────────────────────────────────
const DOC_IDS     = ['dl_upload', 'or_upload', 'cr_upload'];
const uploadedSet = new Set();   // tracks which slots have a valid file chosen

function triggerUpload(docId) {
    document.getElementById(docId)?.click();
}

function handleDocUpload(input, docId) {
    const file = input.files?.[0];
    if (!file) return;

    // Type guard
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) {
        showDocError(docId, 'Only JPG, PNG, or WEBP images are accepted.');
        input.value = '';
        return;
    }

    // Size guard — 5 MB
    if (file.size > 5 * 1024 * 1024) {
        showDocError(docId, 'File is too large. Maximum size is 5 MB.');
        input.value = '';
        return;
    }

    // Show thumbnail preview
    const reader = new FileReader();
    reader.onload = (e) => {
        const thumb = document.getElementById('thumb-' + docId);
        if (thumb) thumb.src = e.target.result;
    };
    reader.readAsDataURL(file);

    // Show filename (truncated)
    const fnameEl = document.getElementById('fname-' + docId);
    if (fnameEl) fnameEl.textContent = file.name;

    // Swap placeholder ↔ preview
    document.getElementById('placeholder-' + docId).style.display = 'none';
    document.getElementById('preview-'     + docId).style.display = 'flex';

    // Green card border
    const card = document.getElementById('card-' + docId);
    if (card) {
        card.style.borderColor  = 'var(--success)';
        card.style.background   = 'rgba(76,175,125,0.05)';
        card.style.borderStyle  = 'solid';
    }

    // Track & refresh banner
    uploadedSet.add(docId);
    refreshDocBanner();
}

function showDocError(docId, msg) {
    const card = document.getElementById('card-' + docId);
    if (card) {
        card.style.borderColor = 'var(--danger)';
        card.style.borderStyle = 'solid';
    }
    // Briefly flash a toast-like alert under the card
    const errEl = document.getElementById('err-docs');
    if (errEl) { errEl.textContent = msg; errEl.style.display = 'block'; }
}

function refreshDocBanner() {
    const allDone = DOC_IDS.every(id => uploadedSet.has(id));
    const banner  = document.getElementById('banner-all-docs');
    const errEl   = document.getElementById('err-docs');
    if (banner) banner.style.display = allDone ? 'flex' : 'none';
    if (errEl && allDone) errEl.style.display = 'none';
}

// ── Step 4: Regex patterns (mirror RegisterActivity.java constants) ──────────
const FIELD_RULES = {
    drivers_license_no: {
        pattern: /^[A-Z][0-9]{2}-[0-9]{2}-[0-9]{6}$/,
        msg: 'Format: 1 letter, 2 digits, dash, 2 digits, dash, 6 digits — e.g. D01-00-123456'
    },
    conductors_license_no: {
        pattern: /^[A-Z][0-9]{2}-[0-9]{2}-[0-9]{6}$/,
        msg: 'Format: 1 letter, 2 digits, dash, 2 digits, dash, 6 digits'
    },
    license_plate: {
        pattern: /^[A-Z]{2,3} [0-9]{4}$/,
        msg: 'Format: 2–3 letters, space, 4 digits — e.g. ABC 1234'
    },
    mv_file_number: {
        pattern: /^[0-9]{15}$/,
        msg: 'Must be exactly 15 digits.'
    },
    vehicle_make: {
        pattern: /^[A-Za-z][A-Za-z\s\-]{1,39}$/,
        msg: 'Letters only, 2–40 characters — e.g. Toyota'
    },
    vehicle_model: {
        pattern: /^[A-Za-z0-9][A-Za-z0-9\s.\-/()']{1,59}$/,
        msg: '2–60 characters, letters/numbers/spaces — e.g. Vios 1.3 XLE MT'
    },
    drivers_license_issuance: { type: 'past' },
    drivers_license_expiry:   { type: 'future' },
};

function validateField(fieldId) {
    const input = document.getElementById(fieldId);
    const errEl = document.getElementById('err-' + fieldId);
    if (!input || !errEl) return true;

    const rule = FIELD_RULES[fieldId];
    if (!rule) return true;
    const val  = input.value.trim();

    if (val === '') { errEl.style.display = 'none'; return false; }

    let ok = false;
    if (rule.pattern) {
        ok = rule.pattern.test(val);
    } else if (rule.type === 'past') {
        ok = val <= new Date().toISOString().slice(0, 10);
    } else if (rule.type === 'future') {
        ok = val > new Date().toISOString().slice(0, 10);
    }

    if (ok) {
        errEl.style.display = 'none';
        input.style.borderColor = '';
    } else {
        errEl.textContent   = rule.msg || errEl.textContent;
        errEl.style.display = 'block';
        input.style.borderColor = 'var(--danger)';
    }
    return ok;
}

// ── DL Restriction Code chips ────────────────────────────────────────────────
function refreshDlChip(cb) {
    const label = cb.closest('.dl-chip-label');
    if (cb.checked) {
        label.style.borderColor = 'var(--yellow)';
        label.style.background  = 'rgba(251,189,35,0.12)';
        label.style.color       = 'var(--yellow)';
    } else {
        label.style.borderColor = 'var(--border)';
        label.style.background  = 'transparent';
        label.style.color       = '';
    }
    const anyChecked = document.querySelectorAll('.dl-chip-cb:checked').length > 0;
    const errEl = document.getElementById('err-dl_codes');
    if (errEl) errEl.style.display = anyChecked ? 'none' : 'block';
}

// Initialise chips on page load (restoring old values after error redirect)
document.querySelectorAll('.dl-chip-cb').forEach(cb => refreshDlChip(cb));

// ── Conductor's License block toggle ─────────────────────────────────────────
function toggleConductors(show) {
    const block = document.getElementById('conductors-block');
    if (!block) return;
    block.style.display = show ? 'block' : 'none';

    // Clear fields when hiding so they don't accidentally submit
    if (!show) {
        ['conductors_license_no','conductors_license_issuance','conductors_license_expiry']
            .forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = '';
            });
    }
}

// ── MV File Number help tooltip ───────────────────────────────────────────────
function toggleMvHelp() {
    const box = document.getElementById('mv-help-box');
    if (box) box.style.display = box.style.display === 'none' ? 'block' : 'none';
}

// ── Final reg-form submit guard ───────────────────────────────────────────────
// All per-step validation already ran via validateStep3()…validateStep7() and
// checkTerms() before the user reached the submit button. This listener is a
// last-resort safety net: it only blocks if the terms checkbox is somehow
// unchecked at submit time (e.g. browser autofill edge case).
// Per-step re-validation is intentionally NOT repeated here — it would block
// legitimate submits because some data (e.g. uploadedSet) is client-side only.
document.getElementById('reg-form')?.addEventListener('submit', function (e) {
    const terms = document.getElementById('terms');
    const errEl = document.getElementById('err-terms');
    if (terms && !terms.checked) {
        e.preventDefault();
        if (errEl) errEl.style.display = 'block';
        terms.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
});

// ── Email match indicator ─────────────────────────────────────────────────────
// Mirrors RegisterActivity.java's etConfirmEmail TextWatcher.
function checkEmailMatch() {
    const email   = document.getElementById('email');
    const confirm = document.getElementById('confirm_email');
    const msg     = document.getElementById('email-match-msg');
    if (!email || !confirm || !msg) return;

    const v1 = email.value.trim();
    const v2 = confirm.value.trim();

    if (v2 === '') {
        msg.style.display = 'none';
        return;
    }

    if (v1 === v2) {
        msg.style.display = 'block';
        msg.style.color   = 'var(--success)';
        msg.textContent   = '✔ Emails match';
    } else {
        msg.style.display = 'block';
        msg.style.color   = 'var(--danger)';
        msg.textContent   = '✘ Emails do not match';
    }
}

// ── Step 6: Live Review Card ──────────────────────────────────────────────────
// Mirrors RegisterActivity.java populateSummary().
// Reads all form fields in real-time and populates the summary card.
(function () {
    // Map of summary element id → how to get the value
    const textBindings = [
        { el: 'rv-name',      get: () => {
            const fn = v('first_name'), ln = v('last_name');
            return (fn || ln) ? [fn, ln].filter(Boolean).join(' ') : '';
        }},
        { el: 'rv-username',  get: () => v('username') },
        { el: 'rv-birthdate', get: () => {
            const raw = v('birthdate');
            if (!raw) return '';
            const d = new Date(raw + 'T00:00:00');
            return isNaN(d) ? raw : d.toLocaleDateString('en-PH', { year:'numeric', month:'long', day:'numeric' });
        }},
        { el: 'rv-gender',    get: () => v('gender') },
        { el: 'rv-email',     get: () => v('email') },
        { el: 'rv-phone',     get: () => v('phone') },
        { el: 'rv-address',   get: () => v('address') },
        { el: 'rv-dl-no',     get: () => v('drivers_license_no') },
        { el: 'rv-dl-codes',  get: () => {
            const cbs = document.querySelectorAll('.dl-chip-cb:checked');
            const codes = Array.from(cbs).map(c => c.value);
            return codes.length ? codes.join(', ') : '';
        }},
        { el: 'rv-dl-issued', get: () => fmtDate(v('drivers_license_issuance')) },
        { el: 'rv-dl-expiry', get: () => fmtDate(v('drivers_license_expiry')) },
        { el: 'rv-make',      get: () => v('vehicle_make') },
        { el: 'rv-model',     get: () => v('vehicle_model') },
        { el: 'rv-plate',     get: () => v('license_plate') },
        { el: 'rv-mvfile',    get: () => v('mv_file_number') },
    ];

    // Document badge bindings
    const docBadges = [
        { el: 'rv-doc-dl', docId: 'dl_upload' },
        { el: 'rv-doc-or', docId: 'or_upload' },
        { el: 'rv-doc-cr', docId: 'cr_upload' },
    ];

    function v(id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : '';
    }

    function fmtDate(raw) {
        if (!raw) return '';
        const d = new Date(raw + 'T00:00:00');
        return isNaN(d) ? raw : d.toLocaleDateString('en-PH', { year:'numeric', month:'short', day:'numeric' });
    }

    function setRvText(elId, val) {
        const el = document.getElementById(elId);
        if (!el) return;
        el.textContent = val || '—';
        el.style.color = val ? 'var(--white)' : 'rgba(255,255,255,0.2)';
    }

    function updateDocBadge(elId, docId) {
        const el = document.getElementById(elId);
        if (!el) return;
        const uploaded = uploadedSet && uploadedSet.has(docId);
        el.textContent         = uploaded ? '✔ Uploaded' : 'Pending';
        el.style.background    = uploaded ? 'rgba(76,175,125,0.15)' : 'rgba(255,255,255,0.06)';
        el.style.color         = uploaded ? 'var(--success)' : 'var(--muted)';
    }

    function checkComplete() {
        const strip = document.getElementById('rv-complete-strip');
        if (!strip) return;

        // Required text fields (must be non-empty)
        const reqFields = [
            'first_name','last_name','username','birthdate','gender',
            'email','phone','address',
            'drivers_license_no','drivers_license_issuance','drivers_license_expiry',
            'license_plate','mv_file_number','vehicle_make','vehicle_model'
        ];
        const allText = reqFields.every(id => v(id) !== '');

        // At least one DL code
        const hasCodes = document.querySelectorAll('.dl-chip-cb:checked').length > 0;

        // All 3 docs uploaded
        const allDocs = uploadedSet && ['dl_upload','or_upload','cr_upload'].every(id => uploadedSet.has(id));

        const complete = allText && hasCodes && allDocs;
        strip.style.display = complete ? 'flex' : 'none';
    }

    function refreshAll() {
        textBindings.forEach(b => setRvText(b.el, b.get()));
        docBadges.forEach(b => updateDocBadge(b.el, b.docId));
        checkComplete();
    }

    // ── Wire up listeners ─────────────────────────────────────────────────────
    // Watch every relevant input / select / textarea
    const watchIds = [
        'first_name','last_name','username','birthdate','gender',
        'email','phone','address',
        'drivers_license_no','drivers_license_issuance','drivers_license_expiry',
        'license_plate','mv_file_number','vehicle_make','vehicle_model'
    ];
    watchIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('input', refreshAll);
        if (el) el.addEventListener('change', refreshAll);
    });

    // DL restriction code chips — use event delegation on the chip container
    document.getElementById('dl-codes-wrap')?.addEventListener('change', refreshAll);

    // Doc uploads — piggyback on the existing handleDocUpload; we'll also call
    // refreshAll after the uploadedSet is updated.
    // NOTE: a second patch is applied below in the wire-up block for step refresh.
    var _origHandleDocUpload = handleDocUpload;
    window.handleDocUpload = function(input, docId) {
        _origHandleDocUpload(input, docId);
        refreshAll();
    };

    // Run once on load to populate with any server-reflected old values
    refreshAll();
})();

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

// ── Step N: refresh Continue button enabled state ─────────────────────────────
// Mirrors the app's refreshStep4Continue() pattern — the button lights up yellow
// only when every required field is filled (and valid where a regex applies).
// No error messages are shown here; those only appear on an explicit attempt.
// ─────────────────────────────────────────────────────────────────────────────

function refreshStep3() {
    // Required fields for step 3 only — email confirm checked separately
    const ids = ['first_name','last_name','username','birthdate',
                 'gender','email','phone','address'];
    const allFilled = ids.every(function(id) {
        const el = document.getElementById(id);
        return el && el.value.trim() !== '';
    });
    // confirm_email must be filled AND match email
    const emailEl  = document.getElementById('email');
    const cEmailEl = document.getElementById('confirm_email');
    const emailConfirmOk = emailEl && cEmailEl
        && cEmailEl.value.trim() !== ''
        && emailEl.value.trim() === cEmailEl.value.trim();
    const btn = document.getElementById('btn-step3-next');
    if (btn) btn.classList.toggle('ready', allFilled && emailConfirmOk);
}

function refreshStep4() {
    // License number + dates + codes
    const dlNoEl  = document.getElementById('drivers_license_no');
    const dlIssEl = document.getElementById('drivers_license_issuance');
    const dlExpEl = document.getElementById('drivers_license_expiry');
    const today   = new Date().toISOString().slice(0,10);

    const dlNoOk  = dlNoEl  && /^[A-Z][0-9]{2}-[0-9]{2}-[0-9]{6}$/.test(dlNoEl.value.trim());
    const dlIssOk = dlIssEl && dlIssEl.value !== '' && dlIssEl.value <= today;
    const dlExpOk = dlExpEl && dlExpEl.value !== '' && dlExpEl.value > today;
    const codesOk = document.querySelectorAll('.dl-chip-cb:checked').length > 0;

    // Vehicle fields
    const plateEl = document.getElementById('license_plate');
    const mvEl    = document.getElementById('mv_file_number');
    const makeEl  = document.getElementById('vehicle_make');
    const modelEl = document.getElementById('vehicle_model');

    const plateOk = plateEl && /^[A-Z]{2,3} [0-9]{4}$/.test(plateEl.value.trim());
    const mvOk    = mvEl    && /^[0-9]{15}$/.test(mvEl.value.trim());
    const makeOk  = makeEl  && /^[A-Za-z][A-Za-z\s\-]{1,39}$/.test(makeEl.value.trim());
    const modelOk = modelEl && /^[A-Za-z0-9][A-Za-z0-9\s.\-/()']{1,59}$/.test(modelEl.value.trim());

    // Conductor's (only required when checkbox ticked)
    let condOk = true;
    const condCb = document.getElementById('cb_conductors');
    if (condCb && condCb.checked) {
        const condNo  = document.getElementById('conductors_license_no');
        const condIss = document.getElementById('conductors_license_issuance');
        const condExp = document.getElementById('conductors_license_expiry');
        const condNoOk  = condNo  && /^[A-Z][0-9]{2}-[0-9]{2}-[0-9]{6}$/.test(condNo.value.trim());
        const condIssOk = condIss && condIss.value !== '' && condIss.value <= today;
        const condExpOk = condExp && condExp.value !== '' && condExp.value > today;
        condOk = condNoOk && condIssOk && condExpOk;
    }

    // Docs are uploaded on Step 5, not required here
    const can = dlNoOk && dlIssOk && dlExpOk && codesOk
             && plateOk && mvOk && makeOk && modelOk
             && condOk;

    const btn = document.getElementById('btn-step4-next');
    if (btn) btn.classList.toggle('ready', can);
}

function refreshStep5() {
    const allDocs = ['dl_upload','or_upload','cr_upload'].every(function(id) {
        return uploadedSet && uploadedSet.has(id);
    });
    const btn = document.getElementById('btn-step5-next');
    if (btn) btn.classList.toggle('ready', allDocs);
}

function refreshStep7() {
    const pw  = document.getElementById('password');
    const cpw = document.getElementById('confirm_password');
    const ok  = pw && cpw && pw.value.length >= 8 && pw.value === cpw.value;
    const btn = document.getElementById('btn-step7-next');
    if (btn) btn.classList.toggle('ready', !!ok);
}

function refreshStep8() {
    const cb  = document.getElementById('terms');
    const btn = document.getElementById('btn-step8-submit');
    if (btn) btn.classList.toggle('ready', !!(cb && cb.checked));
}

// ── Step 3 Validation ─────────────────────────────────────────────────────────
// Validates Account Basics fields before advancing to Step 4.
// ─────────────────────────────────────────────────────────────────────────────
function validateStep3() {
    // Button is only clickable (.ready) when all fields pass refreshStep3(),
    // so just advance — no need to re-validate here.
    showStep(4);
}

// ── Step 4 Validation ─────────────────────────────────────────────────────────
function validateStep4() {
    // Button only becomes clickable when all conditions pass (refreshStep4 gates it),
    // so if we're here the form is valid — just advance.
    showStep(5);
}

// ── Step 5 Validation ─────────────────────────────────────────────────────────
function validateStep5() {
    if (typeof refreshAll === 'function') refreshAll();
    showStep(6);
}

// ── Step 7 Validation ─────────────────────────────────────────────────────────
function validateStep7() {
    showStep(8);
}

// ── Step 8: Terms guard ───────────────────────────────────────────────────────
function checkTerms(e) {
    const cb = document.getElementById('terms');
    if (cb && !cb.checked) { e.preventDefault(); }
    // If checked: let native form submit proceed
}

// ── Wizard Engine ─────────────────────────────────────────────────────────────
// showStep(n)  — switches the visible wizard panel to step n (3–8).
// updatePips() — marks pips as done/active/idle and updates the header text.
// Sub-stages 2–5 will add per-step validation before calling showStep().
// ─────────────────────────────────────────────────────────────────────────────
(function () {

    // Step metadata: tag label, title HTML, subtitle
    const WIZ_META = {
        3: { tag: 'STEP 3 OF 8', title: 'Create Your<br>Free Account',       sub: 'Fill in your personal details to get started.' },
        4: { tag: 'STEP 4 OF 8', title: "Driver's License<br>& Vehicle",      sub: 'Enter your license details and vehicle information.' },
        5: { tag: 'STEP 5 OF 8', title: 'Upload<br>Documents',                sub: 'Provide clear photos of your required documents.' },
        6: { tag: 'STEP 6 OF 8', title: 'Review Your<br>Information',         sub: 'Confirm everything looks correct before continuing.' },
        7: { tag: 'STEP 7 OF 8', title: 'Set Your<br>Password',               sub: 'Choose a strong password to secure your account.' },
        8: { tag: 'STEP 8 OF 8', title: 'Almost<br>Done!',                    sub: 'Agree to the terms and create your account.' },
    };

    const FIRST_STEP = 3;
    const LAST_STEP  = 8;
    let   currentStep = FIRST_STEP;

    // ── Core show function ────────────────────────────────────────────────────
    window.showStep = function (n) {
        if (n < FIRST_STEP || n > LAST_STEP) return;

        // Hide all panels, show target
        document.querySelectorAll('.wizard-step').forEach(el => el.classList.remove('active'));
        const target = document.getElementById('wiz-step-' + n);
        if (target) target.classList.add('active');

        currentStep = n;

        // Update pip states
        updatePips(n);

        // Update header
        const meta = WIZ_META[n];
        if (meta) {
            const tagEl   = document.getElementById('wiz-step-tag');
            const titleEl = document.getElementById('wiz-title');
            const subEl   = document.getElementById('wiz-sub');
            if (tagEl)   tagEl.textContent  = meta.tag;
            if (titleEl) titleEl.innerHTML  = meta.title;
            if (subEl)   subEl.textContent  = meta.sub;
        }

        // Re-evaluate button state when entering a step
        const refreshMap = { 3: refreshStep3, 4: refreshStep4, 5: refreshStep5, 7: refreshStep7, 8: refreshStep8 };
        if (refreshMap[n]) refreshMap[n]();

        // Scroll wizard card top into view smoothly
        const card = document.querySelector('.wizard-card');
        if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
    };

    // ── Pip updater ───────────────────────────────────────────────────────────
    function updatePips(active) {
        for (let n = 1; n <= LAST_STEP; n++) {
            const pip = document.getElementById('wiz-pip-' + n);
            if (!pip) continue;
            pip.classList.remove('active', 'done');
            if (n < active)  pip.classList.add('done');
            if (n === active) pip.classList.add('active');
        }
    }

    // ── Expose helpers for sub-stages ─────────────────────────────────────────
    window.wizCurrentStep = () => currentStep;
    window.wizNext = () => showStep(currentStep + 1);
    window.wizBack = () => showStep(currentStep - 1);

    // Initialise pips on load
    updatePips(FIRST_STEP);

})();
// ── Wire refreshStepN() listeners ─────────────────────────────────────────────
// Each relevant field calls the appropriate refresh so the Continue button
// lights up instantly as the user fills things in — mirroring the app's
// TextWatcher / OnCheckedChangeListener pattern.
(function() {

    // Step 3 fields — keyup covers cases where input fires late (mobile, autofill)
    var s3ids = ['first_name','last_name','username','birthdate',
                 'gender','email','confirm_email','phone','address'];
    s3ids.forEach(function(id) {
        var el = document.getElementById(id);
        if (el) {
            el.addEventListener('input',  refreshStep3);
            el.addEventListener('change', refreshStep3);
            el.addEventListener('keyup',  refreshStep3);
        }
    });

    // Step 4 fields
    var s4ids = ['drivers_license_no','drivers_license_issuance','drivers_license_expiry',
                 'license_plate','mv_file_number','vehicle_make','vehicle_model',
                 'conductors_license_no','conductors_license_issuance','conductors_license_expiry',
                 'cb_conductors'];
    s4ids.forEach(function(id) {
        var el = document.getElementById(id);
        if (el) {
            el.addEventListener('input',  refreshStep4);
            el.addEventListener('change', refreshStep4);
        }
    });
    // DL restriction code chips
    document.getElementById('dl-codes-wrap')?.addEventListener('change', refreshStep4);

    // Step 7 password fields
    ['password','confirm_password'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('input', refreshStep7);
    });

    // Step 8 terms checkbox
    var termsCb = document.getElementById('terms');
    if (termsCb) termsCb.addEventListener('change', refreshStep8);

    // Patch handleDocUpload to also refresh Step 4 & 5 buttons
    // (chains from the review-card patch already applied above in the review IIFE)
    var _prevHandleDocUpload = window.handleDocUpload;
    window.handleDocUpload = function(input, docId) {
        _prevHandleDocUpload(input, docId);
        refreshStep4();
        refreshStep5();
    };

    // Initial state
    refreshStep3();
    refreshStep4();
    refreshStep5();
    refreshStep7();
    refreshStep8();
})();


</script>

</body>
</html>
