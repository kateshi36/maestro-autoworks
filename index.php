<?php
session_start();
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Maestro Autoworks — Professional Auto Care</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;600;700;800&family=Barlow:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
    <style>
        /* Specific layouts for the informational page */
        .hero {
            padding: 120px 5% 60px;
            text-align: center;
            background: radial-gradient(circle at top, var(--navy-mid) 0%, var(--navy) 100%);
        }
        .section-title {
            font-family: 'Barlow Condensed', sans-serif;
            font-size: 2.5rem;
            color: var(--white);
            margin-bottom: 40px;
            text-align: center;
        }
        .section-title span { color: var(--yellow); }
        
        .services-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 24px;
            padding: 40px 5%;
            max-width: 1200px;
            margin: 0 auto;
        }
        .info-card {
            background: var(--navy-card);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 30px;
            transition: transform 0.3s ease;
        }
        .info-card:hover { transform: translateY(-5px); }
        .info-card h3 {
            font-family: 'Barlow Condensed', sans-serif;
            color: var(--yellow);
            font-size: 1.5rem;
            margin-bottom: 15px;
        }
        .shop-history {
            max-width: 800px;
            margin: 60px auto;
            padding: 0 5%;
            line-height: 1.8;
            color: var(--muted);
            text-align: center;
        }
        .nav-buttons {
            margin-top: 30px;
            display: flex;
            justify-content: center;
            gap: 15px;
        }
        .btn-outline {
            padding: 12px 24px;
            border: 2px solid var(--yellow);
            color: var(--yellow);
            text-decoration: none;
            font-family: 'Barlow Condensed', sans-serif;
            font-weight: 700;
            border-radius: 8px;
            transition: all 0.3s;
        }
        .btn-outline:hover {
            background: var(--yellow);
            color: var(--navy);
        }
    </style>
</head>
<body>

<div class="top-bar">
    <a href="index.php" class="logo">
        <div class="logo-icon">
            <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99zM6.85 6h10.29l1.08 4H5.77l1.08-4zM19 18H5v-6h14v6zm-8-4H7v-2h4v2zm6 0h-4v-2h4v2z"/></svg>
        </div>
        Maestro Autoworks
    </a>
    <div class="auth-links">
        <?php if(isset($_SESSION['user_id'])): ?>
            <a href="dashboard.php" class="back-link">Dashboard</a>
        <?php else: ?>
            <a href="login.php" class="back-link">Sign In</a>
        <?php endif; ?>
    </div>
</div>

<header class="hero">
    <span class="tag" style="color: var(--yellow); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; font-size: 14px;">Est. 1998</span>
    <h1 style="font-family: 'Barlow Condensed', sans-serif; font-size: 4rem; font-weight: 800; color: var(--white); margin: 10px 0;">Mastery in Every <span>Gear</span></h1>
    <p style="max-width: 600px; margin: 20px auto; color: var(--muted);">Premium automotive care tailored for those who value performance and reliability.</p>
    <div class="nav-buttons">
        <a href="register.php" class="btn-login" style="width: auto; padding: 12px 30px;">Book an Appointment</a>
        <a href="#services" class="btn-outline">Our Services</a>
    </div>
</header>

<section class="shop-history">
    <h2 class="section-title">Our <span>Legacy</span></h2>
    <p>Founded over two decades ago, Maestro Autoworks began as a small two-bay garage with a singular vision: to provide dealership-quality service with the personal touch of a local shop. Today, we stand as the region's leading independent repair facility, combining advanced diagnostics with old-school craftsmanship.</p>
</section>

<section id="services" class="services-grid">
    <div class="info-card">
        <h3>Precision Tuning</h3>
        <p>Advanced ECU remapping and performance adjustments to ensure your engine runs at peak efficiency and power.</p>
    </div>
    <div class="info-card">
        <h3>Routine Maintenance</h3>
        <p>Comprehensive 50-point inspections, oil changes, and fluid flushes to keep your daily driver in showroom condition.</p>
    </div>
    <div class="info-card">
        <h3>Brake & Suspension</h3>
        <p>From ceramic pad replacements to full coilover installations, we ensure your safety and handling are never compromised.</p>
    </div>
    <div class="info-card">
        <h3>Diagnostic Care</h3>
        <p>Specialized electrical troubleshooting using industry-leading scanning tools to find issues others miss.</p>
    </div>
</section>

<footer style="padding: 60px 0; text-align: center; border-top: 1px solid rgba(255,255,255,0.05); margin-top: 60px;">
    <p style="font-size: 13px; color: var(--muted);">&copy; 2026 Maestro Autoworks. All rights reserved.</p>
</footer>

</body>
</html>