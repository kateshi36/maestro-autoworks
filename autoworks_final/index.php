<?php
// index.php — Public Landing Page (no auth required)
session_start();

// If already logged in, redirect to appropriate dashboard
if (isset($_SESSION['user_id'])) {
    require_once 'db.php';
    $stmt = $pdo->prepare("SELECT role FROM users WHERE id = ?");
    $stmt->execute([$_SESSION['user_id']]);
    $u = $stmt->fetch();
    header('Location: ' . ($u && $u['role'] === 'admin' ? 'admin_dashboard.php' : 'home.php'));
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Maestro Autoworks — Expert Auto Repair in Metro Manila</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:ital,wght@0,400;0,600;0,700;0,800;0,900;1,700&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <style>
        /* ── RESET & BASE ─────────────────────────────────────────────────── */
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --black:      #0a0a0a;
            --black-mid:  #111111;
            --black-card: #1a1a1a;
            --black-ele:  #222222;
            --black-input:#2a2a2a;
            --yellow:     #F5A623;
            --yellow-dk:  #D4891A;
            --yellow-lt:  rgba(245,166,35,0.12);
            --yellow-glow:rgba(245,166,35,0.25);
            --border:     rgba(245,166,35,0.2);
            --border-sub: rgba(255,255,255,0.07);
            --text:       #E8EDF5;
            --muted:      #888888;
            --white:      #ffffff;
            --danger:     #E05252;
            --success:    #4CAF7D;
            --nav-h:      68px;
        }

        html { scroll-behavior: smooth; }

        body {
            font-family: 'Barlow', sans-serif;
            background: var(--black);
            color: var(--text);
            font-size: 15px;
            line-height: 1.6;
            overflow-x: hidden;
        }

        a { color: var(--yellow); text-decoration: none; }
        a:hover { opacity: .85; }

        /* ── NOISE TEXTURE OVERLAY ────────────────────────────────────────── */
        body::before {
            content: '';
            position: fixed; inset: 0; z-index: 0; pointer-events: none;
            background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.03'/%3E%3C/svg%3E");
            opacity: .4;
        }

        /* ── TOPBAR / NAV ─────────────────────────────────────────────────── */
        .nav {
            position: fixed; top: 0; left: 0; right: 0; z-index: 200;
            height: var(--nav-h);
            background: rgba(10,10,10,0.92);
            backdrop-filter: blur(16px);
            border-bottom: 1px solid var(--border-sub);
            display: flex; align-items: center;
            padding: 0 32px; gap: 0;
        }

        /* Yellow left accent stripe */
        .nav::before {
            content: '';
            position: absolute; left: 0; top: 0; bottom: 0;
            width: 4px;
            background: var(--yellow);
        }

        .nav-logo {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 22px; font-weight: 900; letter-spacing: 1px;
            color: var(--white); display: flex; align-items: center; gap: 10px;
            text-decoration: none; white-space: nowrap; margin-right: 36px;
        }

        .nav-logo-badge {
            width: 36px; height: 36px;
            background: var(--yellow);
            border-radius: 6px;
            display: flex; align-items: center; justify-content: center;
        }

        .nav-logo-badge { width:38px;height:38px;border-radius:50%;overflow:hidden;background:#000;flex-shrink:0; } .nav-logo-badge svg { display:none; }

        .nav-links {
            display: flex; align-items: center; gap: 4px; flex: 1;
        }

        .nav-links a {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 14px; font-weight: 700; letter-spacing: 1.5px;
            text-transform: uppercase;
            color: var(--muted); text-decoration: none;
            padding: 6px 14px; border-radius: 4px;
            transition: color .2s, background .2s;
        }

        .nav-links a:hover,
        .nav-links a.active { color: var(--yellow); background: var(--yellow-lt); }

        .nav-actions {
            display: flex; align-items: center; gap: 10px; margin-left: auto;
        }

        /* Buttons */
        .btn {
            display: inline-flex; align-items: center; gap: 7px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 14px; font-weight: 700; letter-spacing: 1px;
            text-transform: uppercase;
            padding: 9px 20px; border-radius: 6px; cursor: pointer;
            text-decoration: none; border: none;
            transition: all .2s; white-space: nowrap;
        }

        .btn svg { width: 16px; height: 16px; fill: currentColor; flex-shrink: 0; }

        .btn-primary {
            background: var(--yellow); color: var(--black);
        }
        .btn-primary:hover {
            background: var(--yellow-dk); opacity: 1;
            transform: translateY(-1px);
            box-shadow: 0 6px 24px var(--yellow-glow);
        }

        .btn-outline {
            background: transparent;
            border: 1.5px solid var(--border);
            color: var(--text);
        }
        .btn-outline:hover {
            border-color: var(--yellow); color: var(--yellow);
            background: var(--yellow-lt); opacity: 1;
        }

        .btn-ghost {
            background: transparent; color: var(--muted);
            padding: 9px 14px;
        }
        .btn-ghost:hover { color: var(--text); background: var(--black-ele); opacity: 1; }

        .btn-lg { font-size: 16px; padding: 13px 28px; }
        .btn-sm { font-size: 12px; padding: 7px 14px; }

        /* Admin button – subtler */
        .btn-admin {
            background: var(--black-ele);
            border: 1px solid var(--border-sub);
            color: var(--muted);
            font-size: 12px; padding: 8px 14px;
        }
        .btn-admin:hover {
            border-color: var(--yellow); color: var(--yellow);
            background: var(--yellow-lt); opacity: 1;
        }
        .btn-admin svg { width: 14px; height: 14px; }

        /* Admin divider line */
        .nav-divider {
            width: 1px; height: 28px;
            background: var(--border-sub);
            margin: 0 6px;
        }

        /* ── HERO ─────────────────────────────────────────────────────────── */
        .hero {
            position: relative; z-index: 1;
            min-height: 100vh;
            padding: calc(var(--nav-h) + 60px) 32px 80px;
            max-width: 1200px; margin: 0 auto;
            display: flex; flex-direction: column;
            justify-content: center;
        }

        /* Diagonal yellow accent background strip */
        .hero-bg {
            position: fixed; top: 0; right: 0; bottom: 0;
            width: 45%;
            background: linear-gradient(135deg, transparent 0%, rgba(245,166,35,0.03) 100%);
            clip-path: polygon(15% 0, 100% 0, 100% 100%, 0 100%);
            pointer-events: none; z-index: 0;
        }

        /* Racing diagonal stripes — decorative */
        .hero-stripes {
            position: absolute; top: 0; right: 0; bottom: 0; width: 46%;
            overflow: hidden; pointer-events: none; z-index: 0;
            opacity: .035;
        }
        .hero-stripes::after {
            content: '';
            position: absolute; inset: -100%;
            background: repeating-linear-gradient(
                -55deg,
                var(--yellow) 0px, var(--yellow) 30px,
                transparent 30px, transparent 80px
            );
        }

        .hero-eyebrow {
            display: inline-flex; align-items: center; gap: 10px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 12px; font-weight: 700;
            letter-spacing: 3px; text-transform: uppercase;
            color: var(--yellow); margin-bottom: 24px;
        }

        .hero-eyebrow::before {
            content: '';
            display: block; width: 32px; height: 2px;
            background: var(--yellow);
        }

        .hero h1 {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: clamp(3.5rem, 7vw, 6rem);
            font-weight: 900; line-height: 1.0;
            color: var(--white);
            letter-spacing: -1px;
            max-width: 720px;
            margin-bottom: 6px;
        }

        .hero h1 .accent { color: var(--yellow); }

        .hero-sub {
            font-size: 16px; font-weight: 300;
            color: var(--muted); max-width: 500px;
            line-height: 1.8; margin-bottom: 44px;
            margin-top: 20px;
        }

        .hero-actions {
            display: flex; align-items: center;
            flex-wrap: wrap; gap: 12px;
            margin-bottom: 60px;
        }

        .hero-or {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 12px; font-weight: 600;
            letter-spacing: 2px; text-transform: uppercase;
            color: var(--muted); padding: 0 4px;
        }

        /* Stats strip in hero */
        .hero-stats {
            display: flex; gap: 0;
            border: 1px solid var(--border-sub);
            border-radius: 8px; overflow: hidden;
            background: var(--black-mid);
            width: fit-content;
        }

        .hero-stat {
            padding: 20px 36px;
            border-right: 1px solid var(--border-sub);
        }
        .hero-stat:last-child { border-right: none; }

        .hero-stat-val {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 2rem; font-weight: 900; color: var(--yellow);
            line-height: 1;
        }

        .hero-stat-lbl {
            font-size: 12px; font-weight: 400;
            color: var(--muted); margin-top: 4px;
            letter-spacing: .5px;
        }

        /* ── SECTION COMMONS ──────────────────────────────────────────────── */
        .section {
            position: relative; z-index: 1;
            max-width: 1200px; margin: 0 auto;
            padding: 80px 32px;
        }

        .section-label {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 11px; font-weight: 700;
            letter-spacing: 3px; text-transform: uppercase;
            color: var(--yellow); margin-bottom: 12px;
            display: flex; align-items: center; gap: 10px;
        }
        .section-label::before {
            content: '';
            display: block; width: 24px; height: 2px;
            background: var(--yellow);
        }

        .section-title {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: clamp(2rem, 3.5vw, 2.8rem);
            font-weight: 800; line-height: 1.1;
            color: var(--white); margin-bottom: 16px;
        }

        .section-desc {
            color: var(--muted); max-width: 540px;
            line-height: 1.8; font-size: 15px; font-weight: 300;
        }

        /* ── FEATURES STRIP ───────────────────────────────────────────────── */
        .features-strip {
            background: var(--black-mid);
            border-top: 1px solid var(--border-sub);
            border-bottom: 1px solid var(--border-sub);
            position: relative; z-index: 1;
        }

        .features-grid {
            max-width: 1200px; margin: 0 auto; padding: 0 32px;
            display: grid; grid-template-columns: repeat(4, 1fr);
        }

        .feature-item {
            padding: 36px 28px;
            border-right: 1px solid var(--border-sub);
            display: flex; flex-direction: column; gap: 14px;
        }
        .feature-item:last-child { border-right: none; }

        .feature-icon {
            width: 44px; height: 44px;
            background: var(--yellow-lt);
            border: 1px solid var(--border);
            border-radius: 8px;
            display: flex; align-items: center; justify-content: center;
        }

        .feature-icon svg { width: 22px; height: 22px; fill: var(--yellow); }

        .feature-title {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 16px; font-weight: 700;
            color: var(--white); letter-spacing: .5px;
        }

        .feature-desc { font-size: 13px; color: var(--muted); line-height: 1.7; }

        /* ── SERVICES SECTION ─────────────────────────────────────────────── */
        .services-head {
            display: flex; justify-content: space-between;
            align-items: flex-end; margin-bottom: 36px;
        }

        .services-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
            gap: 16px;
        }

        .svc-card {
            background: var(--black-card);
            border: 1px solid var(--border-sub);
            border-radius: 10px; padding: 24px;
            transition: border-color .2s, transform .2s;
        }

        .svc-card:hover {
            border-color: var(--yellow);
            transform: translateY(-2px);
        }

        .svc-name {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 17px; font-weight: 700;
            color: var(--white); margin-bottom: 8px;
        }

        .svc-desc { font-size: 13px; color: var(--muted); line-height: 1.6; margin-bottom: 16px; }

        .svc-meta {
            display: flex; justify-content: space-between;
            align-items: center; margin-top: auto;
            padding-top: 16px;
            border-top: 1px solid var(--border-sub);
        }

        .svc-price {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.2rem; font-weight: 800; color: var(--yellow);
        }

        .svc-dur {
            display: flex; align-items: center; gap: 5px;
            font-size: 12px; color: var(--muted);
        }
        .svc-dur svg { width: 14px; height: 14px; fill: var(--muted); }

        /* ── ABOUT / CTA SECTION ──────────────────────────────────────────── */
        .about-grid {
            display: grid; grid-template-columns: 1fr 1fr; gap: 64px;
            align-items: center;
        }

        .stat-grid {
            display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
        }

        .stat-tile {
            background: var(--black-card);
            border: 1px solid var(--border-sub);
            border-radius: 10px; padding: 28px 20px;
            text-align: center;
            transition: border-color .2s;
        }
        .stat-tile:hover { border-color: var(--yellow); }

        .stat-val {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 2.8rem; font-weight: 900; color: var(--yellow);
            line-height: 1;
        }

        .stat-lbl {
            font-size: 13px; color: var(--muted);
            margin-top: 8px; font-weight: 400;
        }

        /* ── CTA BANNER ───────────────────────────────────────────────────── */
        .cta-banner {
            position: relative; z-index: 1;
            margin: 0 32px;
            max-width: 1200px; margin: 0 auto;
            padding: 0 32px 80px;
        }

        .cta-inner {
            background: var(--yellow);
            border-radius: 12px; padding: 56px 52px;
            display: flex; justify-content: space-between; align-items: center;
            gap: 32px; overflow: hidden; position: relative;
        }

        .cta-inner::before {
            content: '';
            position: absolute; top: -80px; right: -80px;
            width: 320px; height: 320px;
            background: rgba(0,0,0,0.08);
            border-radius: 50%;
        }

        .cta-inner::after {
            content: '';
            position: absolute; bottom: -60px; right: 160px;
            width: 200px; height: 200px;
            background: rgba(0,0,0,0.05);
            border-radius: 50%;
        }

        .cta-text { position: relative; z-index: 1; }

        .cta-text h2 {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: clamp(1.8rem, 3vw, 2.4rem);
            font-weight: 900; color: var(--black);
            line-height: 1.1; margin-bottom: 10px;
        }

        .cta-text p { font-size: 15px; color: rgba(0,0,0,0.6); max-width: 400px; }

        .cta-actions { display: flex; gap: 12px; position: relative; z-index: 1; }

        .btn-dark {
            background: var(--black); color: var(--yellow);
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 15px; font-weight: 700; letter-spacing: 1px;
            text-transform: uppercase;
            padding: 13px 28px; border-radius: 6px;
            display: inline-flex; align-items: center; gap: 8px;
            text-decoration: none; border: none; cursor: pointer;
            transition: all .2s;
        }
        .btn-dark:hover { background: #1a1a1a; transform: translateY(-1px); opacity: 1; }
        .btn-dark svg { width: 17px; height: 17px; fill: var(--yellow); }

        .btn-dark-outline {
            background: transparent; color: var(--black);
            border: 2px solid rgba(0,0,0,0.25);
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 15px; font-weight: 700; letter-spacing: 1px;
            text-transform: uppercase;
            padding: 13px 28px; border-radius: 6px;
            display: inline-flex; align-items: center; gap: 8px;
            text-decoration: none; transition: all .2s;
        }
        .btn-dark-outline:hover {
            background: rgba(0,0,0,0.1); border-color: rgba(0,0,0,0.4); opacity: 1;
        }
        .btn-dark-outline svg { width: 17px; height: 17px; fill: var(--black); }

        /* ── CONTACT STRIP ────────────────────────────────────────────────── */
        .contact-strip {
            border-top: 1px solid var(--border-sub);
            position: relative; z-index: 1;
        }

        .contact-grid {
            max-width: 1200px; margin: 0 auto; padding: 56px 32px;
            display: grid; grid-template-columns: repeat(3, 1fr); gap: 40px;
        }

        .contact-item-label {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 11px; font-weight: 700; letter-spacing: 2.5px;
            text-transform: uppercase; color: var(--yellow); margin-bottom: 14px;
        }

        .contact-item-body { color: var(--text); line-height: 1.8; font-size: 14px; }

        .contact-item-body span { color: var(--muted); }

        /* ── FOOTER ───────────────────────────────────────────────────────── */
        .footer {
            background: var(--black-mid);
            border-top: 1px solid var(--border-sub);
            position: relative; z-index: 1;
        }

        .footer-inner {
            max-width: 1200px; margin: 0 auto; padding: 28px 32px;
            display: flex; align-items: center; justify-content: space-between;
            gap: 20px; flex-wrap: wrap;
        }

        .footer-copy {
            font-size: 13px; color: var(--muted);
        }

        .footer-links {
            display: flex; align-items: center; gap: 20px;
        }

        .footer-links a {
            font-size: 13px; color: var(--muted);
            text-decoration: none; transition: color .2s;
        }
        .footer-links a:hover { color: var(--yellow); opacity: 1; }

        /* Admin portal footer link — subtle emphasis */
        .footer-admin-link {
            display: inline-flex; align-items: center; gap: 6px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 12px; font-weight: 700; letter-spacing: 1px;
            text-transform: uppercase;
            color: var(--muted); text-decoration: none;
            padding: 5px 12px;
            border: 1px solid var(--border-sub);
            border-radius: 4px;
            transition: all .2s;
        }
        .footer-admin-link:hover {
            color: var(--yellow); border-color: var(--border);
            background: var(--yellow-lt); opacity: 1;
        }
        .footer-admin-link svg { width: 13px; height: 13px; fill: currentColor; }

        /* ── ADMIN MODAL ──────────────────────────────────────────────────── */
        .modal-overlay {
            position: fixed; inset: 0; z-index: 500;
            background: rgba(0,0,0,0.85);
            backdrop-filter: blur(8px);
            display: none; align-items: center; justify-content: center;
            padding: 24px;
        }
        .modal-overlay.open { display: flex; }

        .modal {
            background: var(--black-card);
            border: 1px solid var(--border);
            border-radius: 12px; width: 100%; max-width: 420px;
            padding: 40px 36px;
            position: relative;
            animation: slideUp .25s ease;
        }

        @keyframes slideUp {
            from { transform: translateY(20px); opacity: 0; }
            to   { transform: translateY(0);    opacity: 1; }
        }

        .modal-close {
            position: absolute; top: 16px; right: 16px;
            background: var(--black-ele); border: none; cursor: pointer;
            width: 32px; height: 32px; border-radius: 6px;
            display: flex; align-items: center; justify-content: center;
            color: var(--muted); transition: all .2s;
        }
        .modal-close:hover { color: var(--text); background: var(--black-input); }
        .modal-close svg { width: 16px; height: 16px; fill: currentColor; }

        .modal-badge {
            display: inline-flex; align-items: center; gap: 8px;
            background: var(--yellow-lt); border: 1px solid var(--border);
            border-radius: 6px; padding: 8px 14px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 12px; font-weight: 700; letter-spacing: 2px;
            text-transform: uppercase; color: var(--yellow);
            margin-bottom: 20px;
        }
        .modal-badge svg { width: 14px; height: 14px; fill: var(--yellow); }

        .modal h3 {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 1.8rem; font-weight: 800;
            color: var(--white); line-height: 1.1; margin-bottom: 10px;
        }

        .modal p { font-size: 13px; color: var(--muted); line-height: 1.7; margin-bottom: 28px; }

        .modal-actions { display: flex; flex-direction: column; gap: 10px; }

        .modal-actions a {
            display: flex; align-items: center; justify-content: center; gap: 8px;
            padding: 14px 20px; border-radius: 7px;
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 15px; font-weight: 700; letter-spacing: 1px;
            text-transform: uppercase; text-decoration: none;
            transition: all .2s;
        }
        .modal-actions a svg { width: 17px; height: 17px; fill: currentColor; }

        .modal-btn-primary {
            background: var(--yellow); color: var(--black);
        }
        .modal-btn-primary:hover {
            background: var(--yellow-dk); opacity: 1;
            transform: translateY(-1px);
            box-shadow: 0 6px 24px var(--yellow-glow);
        }

        .modal-btn-outline {
            background: transparent;
            border: 1.5px solid var(--border);
            color: var(--text);
        }
        .modal-btn-outline:hover {
            border-color: var(--yellow); color: var(--yellow);
            background: var(--yellow-lt); opacity: 1;
        }

        .modal-note {
            margin-top: 20px; padding-top: 20px;
            border-top: 1px solid var(--border-sub);
            font-size: 12px; color: var(--muted); text-align: center;
        }

        /* ── SCROLL ANIMATIONS ────────────────────────────────────────────── */
        .reveal {
            opacity: 0; transform: translateY(24px);
            transition: opacity .6s ease, transform .6s ease;
        }
        .reveal.visible { opacity: 1; transform: translateY(0); }

        /* ── RESPONSIVE ───────────────────────────────────────────────────── */
        @media (max-width: 900px) {
            .features-grid { grid-template-columns: repeat(2, 1fr); }
            .about-grid { grid-template-columns: 1fr; gap: 36px; }
            .contact-grid { grid-template-columns: 1fr; gap: 28px; }
            .cta-inner { flex-direction: column; padding: 40px 32px; }
            .hero h1 { font-size: clamp(2.8rem, 8vw, 4rem); }
            .hero-stats { flex-wrap: wrap; }
            .hero-stat { border-right: none; border-bottom: 1px solid var(--border-sub); }
        }

        @media (max-width: 640px) {
            .nav { padding: 0 18px; }
            .nav-links { display: none; }
            .hero { padding: calc(var(--nav-h) + 40px) 18px 60px; }
            .section { padding: 60px 18px; }
            .features-grid { grid-template-columns: 1fr; }
            .services-head { flex-direction: column; align-items: flex-start; gap: 16px; }
            .footer-inner { flex-direction: column; align-items: flex-start; }
        }
    </style>
</head>
<body>

<!-- ═══════════════════════════════════ NAVBAR ═══════════════════════════════ -->
<nav class="nav" role="navigation">
    <a href="index.php" class="nav-logo">
        <div class="nav-logo-badge">
            <img src="logo.png" alt="Maestro Autoworks Logo" style="width:38px;height:38px;object-fit:cover;border-radius:50%;display:block;">
        </div>
        Maestro<span style="color:var(--yellow)">Autoworks</span>
    </a>

    <div class="nav-links">
        <a href="#about" class="active">About</a>
        <a href="#services">Services</a>
        <a href="#contact">Contact</a>
    </div>

    <div class="nav-actions">
        <!-- User auth buttons -->
        <a href="login.php" class="btn btn-outline">
            <svg viewBox="0 0 24 24"><path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z"/></svg>
            Log In
        </a>
        <a href="register.php" class="btn btn-primary">
            <svg viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            Sign Up
        </a>
    </div>
</nav>

<!-- ═══════════════════════════════════ HERO ════════════════════════════════ -->
<div class="hero-bg"></div>
<div class="hero-stripes"></div>

<section class="hero">
    <div class="hero-eyebrow">Est. 2010 · Quezon City, Philippines</div>
    <h1>
        Your Car Deserves<br>
        <span class="accent">Expert Hands</span>
    </h1>
    <p class="hero-sub">
        Maestro Autoworks delivers honest, high-quality auto repair and maintenance services — 
        from routine oil changes to complex engine overhauls. Trusted by thousands of Filipino motorists.
    </p>

    <div class="hero-actions">
        <a href="register.php" class="btn btn-primary btn-lg">
            <svg viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            Create Free Account
        </a>

        <span class="hero-or">or</span>

        <a href="login.php" class="btn btn-outline btn-lg">
            <svg viewBox="0 0 24 24"><path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z"/></svg>
            Sign In
        </a>

        <a href="#services" class="btn btn-ghost btn-lg" style="margin-left:4px;">
            Explore Services ↓
        </a>
    </div>

    <!-- Stats -->
    <div class="hero-stats">
        <div class="hero-stat">
            <div class="hero-stat-val">15+</div>
            <div class="hero-stat-lbl">Years in Business</div>
        </div>
        <div class="hero-stat">
            <div class="hero-stat-val">12</div>
            <div class="hero-stat-lbl">Certified Mechanics</div>
        </div>
        <div class="hero-stat">
            <div class="hero-stat-val">50+</div>
            <div class="hero-stat-lbl">Cars Serviced Daily</div>
        </div>
        <div class="hero-stat">
            <div class="hero-stat-val">98%</div>
            <div class="hero-stat-lbl">Customer Satisfaction</div>
        </div>
    </div>
</section>

<!-- ═══════════════════════════════ FEATURES STRIP ══════════════════════════ -->
<div class="features-strip">
    <div class="features-grid">
        <div class="feature-item reveal">
            <div class="feature-icon">
                <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l6 2.67V11c0 3.7-2.5 7.17-6 8.38-3.5-1.21-6-4.68-6-8.38V5.85l6-2.67z"/></svg>
            </div>
            <div>
                <div class="feature-title">Certified Mechanics</div>
                <div class="feature-desc">All repairs handled by factory-trained and ASE-certified technicians.</div>
            </div>
        </div>
        <div class="feature-item reveal" style="transition-delay:.1s">
            <div class="feature-icon">
                <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
            </div>
            <div>
                <div class="feature-title">Fast Turnaround</div>
                <div class="feature-desc">Most jobs completed same-day. We respect your time and schedule.</div>
            </div>
        </div>
        <div class="feature-item reveal" style="transition-delay:.2s">
            <div class="feature-icon">
                <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
            </div>
            <div>
                <div class="feature-title">Online Booking</div>
                <div class="feature-desc">Schedule your appointment online anytime, 24/7, from any device.</div>
            </div>
        </div>
        <div class="feature-item reveal" style="transition-delay:.3s">
            <div class="feature-icon">
                <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"/></svg>
            </div>
            <div>
                <div class="feature-title">Transparent Pricing</div>
                <div class="feature-desc">No hidden fees. Get a clear quote before any work begins.</div>
            </div>
        </div>
    </div>
</div>

<!-- ═══════════════════════════════ ABOUT ══════════════════════════════════ -->
<section class="section" id="about">
    <div class="about-grid">
        <div class="reveal">
            <div class="section-label">About the Shop</div>
            <h2 class="section-title">Trusted by Thousands<br>of Filipino Motorists</h2>
            <p class="section-desc">
                Founded in 2010, Maestro Autoworks has grown from a small garage in Quezon City into 
                one of the most trusted auto repair shops in Metro Manila. Our team of certified mechanics 
                combines modern diagnostic technology with old-school craftsmanship.
            </p>
            <p style="color:var(--muted);line-height:1.8;font-size:15px;font-weight:300;margin-top:16px;max-width:540px;">
                We believe in transparent pricing, honest assessments, and repairs done right the first time 
                — no upsells, no guesswork.
            </p>
            <div style="margin-top:32px;display:flex;gap:12px;flex-wrap:wrap;">
                <a href="register.php" class="btn btn-primary">Get Started Free</a>
                <a href="#services" class="btn btn-outline">View Services</a>
            </div>
        </div>
        <div class="stat-grid reveal" style="transition-delay:.15s">
            <div class="stat-tile">
                <div class="stat-val">15+</div>
                <div class="stat-lbl">Years in Business</div>
            </div>
            <div class="stat-tile">
                <div class="stat-val">12</div>
                <div class="stat-lbl">Certified Mechanics</div>
            </div>
            <div class="stat-tile">
                <div class="stat-val">50+</div>
                <div class="stat-lbl">Cars Serviced Daily</div>
            </div>
            <div class="stat-tile">
                <div class="stat-val">98%</div>
                <div class="stat-lbl">Customer Satisfaction</div>
            </div>
        </div>
    </div>
</section>

<!-- ═══════════════════════════════ SERVICES ═══════════════════════════════ -->
<section class="section" id="services" style="padding-top:0;">
    <div class="services-head">
        <div>
            <div class="section-label">What We Offer</div>
            <h2 class="section-title">Our Services</h2>
        </div>
        <a href="login.php?next=services.php" class="btn btn-outline btn-sm" title="Sign in to see all services">
            View All →
        </a>
    </div>

    <?php
    // Try to fetch services from DB; gracefully fall back to static previews if DB unavailable
    $services = [];
    try {
        if (file_exists(__DIR__ . '/db.php')) {
            require_once 'db.php';
            $services = $pdo->query("SELECT * FROM services WHERE active = 1 ORDER BY category, name LIMIT 8")->fetchAll();
        }
    } catch (\Throwable $e) { /* silently fall back */ }

    if (!empty($services)):
        $byCategory = [];
        foreach ($services as $s) {
            $byCategory[$s['category']][] = $s;
        }
        foreach ($byCategory as $cat => $items):
    ?>
        <div style="margin-bottom:36px;" class="reveal">
            <h3 style="font-family:'Barlow Condensed',sans-serif;font-size:11px;font-weight:700;
                color:var(--yellow);letter-spacing:3px;text-transform:uppercase;margin-bottom:14px;
                display:flex;align-items:center;gap:10px;">
                <span style="display:block;width:20px;height:2px;background:var(--yellow);"></span>
                <?= htmlspecialchars($cat) ?>
            </h3>
            <div class="services-grid">
                <?php foreach ($items as $svc): ?>
                <div class="svc-card">
                    <div class="svc-name"><?= htmlspecialchars($svc['name']) ?></div>
                    <div class="svc-desc"><?= htmlspecialchars($svc['description']) ?></div>
                    <div class="svc-meta">
                        <div class="svc-price">₱<?= number_format($svc['price'], 2) ?></div>
                        <div class="svc-dur">
                            <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                            <?= number_format($svc['duration_hr'], 1) ?> hr<?= $svc['duration_hr'] != 1 ? 's' : '' ?>
                        </div>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        </div>
    <?php
        endforeach;
    else:
    // Static fallback cards shown before DB is connected
    $staticServices = [
        ['name'=>'Oil Change & Filter',      'desc'=>'Full synthetic or conventional oil change with new filter and 27-point inspection.',        'price'=>'699.00',  'dur'=>'1.0'],
        ['name'=>'Brake Pad Replacement',    'desc'=>'Front or rear disc brake pad swap using OEM-grade compound with road test.',                'price'=>'2,500.00','dur'=>'2.0'],
        ['name'=>'Air-Con Regas & Check',    'desc'=>'Refrigerant top-up, leak check, and cabin filter inspection for a cool ride.',              'price'=>'1,200.00','dur'=>'1.5'],
        ['name'=>'Engine Tune-Up',           'desc'=>'Spark plugs, air filter, fuel injector cleaning, and timing check.',                        'price'=>'3,800.00','dur'=>'3.0'],
        ['name'=>'Wheel Alignment & Balance','desc'=>'4-wheel computer alignment and dynamic wheel balancing for optimal handling.',               'price'=>'950.00',  'dur'=>'1.5'],
        ['name'=>'Transmission Service',     'desc'=>'ATF or MTF flush, filter replacement, and computer scan for smooth shifting.',               'price'=>'4,200.00','dur'=>'2.5'],
    ];
    ?>
    <div class="services-grid reveal">
        <?php foreach ($staticServices as $svc): ?>
        <div class="svc-card">
            <div class="svc-name"><?= $svc['name'] ?></div>
            <div class="svc-desc"><?= $svc['desc'] ?></div>
            <div class="svc-meta">
                <div class="svc-price">₱<?= $svc['price'] ?></div>
                <div class="svc-dur">
                    <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                    <?= $svc['dur'] ?> hrs
                </div>
            </div>
        </div>
        <?php endforeach; ?>
    </div>
    <?php endif; ?>
</section>

<!-- ═══════════════════════════════ CTA BANNER ════════════════════════════ -->
<div class="cta-banner reveal">
    <div class="cta-inner">
        <div class="cta-text">
            <h2>Ready to Book Your<br>Next Appointment?</h2>
            <p>Create a free account and manage all your service bookings in one place.</p>
        </div>
        <div class="cta-actions">
            <a href="register.php" class="btn-dark">
                <svg viewBox="0 0 24 24"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                Sign Up Free
            </a>
            <a href="login.php" class="btn-dark-outline">
                <svg viewBox="0 0 24 24"><path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z"/></svg>
                Already have an account
            </a>
        </div>
    </div>
</div>

<!-- ═══════════════════════════════ CONTACT ═══════════════════════════════ -->
<div class="contact-strip" id="contact">
    <div class="contact-grid">
        <div class="reveal">
            <div class="contact-item-label">Location</div>
            <div class="contact-item-body">
                123 Kalayaan Avenue<br>
                Diliman, Quezon City<br>
                Metro Manila, Philippines
            </div>
        </div>
        <div class="reveal" style="transition-delay:.1s">
            <div class="contact-item-label">Operating Hours</div>
            <div class="contact-item-body">
                Monday – Saturday<br>
                8:00 AM – 6:00 PM<br>
                <span>Closed Sundays & Holidays</span>
            </div>
        </div>
        <div class="reveal" style="transition-delay:.2s">
            <div class="contact-item-label">Contact Us</div>
            <div class="contact-item-body">
                📞 (02) 8-123-4567<br>
                📱 0917-123-4567<br>
                ✉️ info@maestroautoworks.ph
            </div>
        </div>
    </div>
</div>

<!-- ═══════════════════════════════ FOOTER ════════════════════════════════ -->
<footer class="footer">
    <div class="footer-inner">
        <div class="footer-copy">
            © <?= date('Y') ?> Maestro Autoworks. All rights reserved.
        </div>
        <div class="footer-links">
            <a href="#about">About</a>
            <a href="#services">Services</a>
            <a href="#contact">Contact</a>
            <a href="register.php">Sign Up</a>
            <a href="login.php">Log In</a>
        </div>
    </div>
</footer>



<script>
// ── Scroll Reveal ──────────────────────────────────────────────────────────
const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
        }
    });
}, { threshold: 0.1 });

document.querySelectorAll('.reveal').forEach(el => observer.observe(el));

// ── Smooth active nav link highlight ─────────────────────────────────────
const sections = document.querySelectorAll('section[id], div[id]');
const navLinks = document.querySelectorAll('.nav-links a');

window.addEventListener('scroll', () => {
    let current = '';
    sections.forEach(section => {
        if (window.scrollY >= section.offsetTop - 120) {
            current = section.id;
        }
    });
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === '#' + current) {
            link.classList.add('active');
        }
    });
}, { passive: true });
</script>
</body>
</html>
