<?php
// P2: Informational Interface — home.php
session_start();
require 'db.php';
$pageTitle = 'Maestro Autoworks — Home';
require 'partials/header.php';

// Fetch all active services
$services = $pdo->query("SELECT * FROM services WHERE active = 1 ORDER BY category, name")->fetchAll();

// Group by category
$byCategory = [];
foreach ($services as $s) {
    $byCategory[$s['category']][] = $s;
}
?>

<main class="page-shell">

    <!-- HERO -->
    <section class="hero">
        <span class="hero-tag">Est. 2010 · Quezon City, Philippines</span>
        <h1>Your Car Deserves<br><span>Expert Hands</span></h1>
        <p>Maestro Autoworks delivers honest, high-quality auto repair and maintenance services — from routine oil changes to complex engine overhauls.</p>
        <div class="hero-actions">
            <a href="book.php" class="btn btn-primary">
                <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                Book an Appointment
            </a>
            <a href="#services" class="btn btn-secondary">
                <svg viewBox="0 0 24 24"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>
                Explore Services
            </a>
        </div>
    </section>

    <!-- ABOUT -->
    <section style="padding:40px 0 60px;" id="about">
        <div class="about-grid">
            <div>
                <div class="page-label">About the Shop</div>
                <h2 style="font-family:'Barlow Condensed',sans-serif;font-size:clamp(2rem,3vw,2.6rem);font-weight:800;color:var(--white);line-height:1.1;margin-bottom:18px;">
                    Trusted by Thousands<br>of Filipino Motorists
                </h2>
                <p style="color:var(--muted);line-height:1.8;margin-bottom:18px;">
                    Founded in 2010, Maestro Autoworks has grown from a small garage in Quezon City into one of the most trusted auto repair shops in Metro Manila. Our team of certified mechanics combines modern diagnostic technology with old-school craftsmanship.
                </p>
                <p style="color:var(--muted);line-height:1.8;">
                    We believe in transparent pricing, honest assessments, and repairs done right the first time — no upsells, no guesswork.
                </p>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;">
                <?php
                $highlights = [
                    ['15+', 'Years in Business'],
                    ['12',  'Certified Mechanics'],
                    ['50+', 'Cars Serviced Daily'],
                    ['98%', 'Customer Satisfaction'],
                ];
                foreach ($highlights as [$val, $lbl]):
                ?>
                <div class="stat-tile" style="text-align:center;padding:28px 20px;">
                    <div class="stat-val" style="font-size:2.8rem;"><?= $val ?></div>
                    <div class="stat-label" style="margin-top:6px;"><?= $lbl ?></div>
                </div>
                <?php endforeach; ?>
            </div>
        </div>
    </section>

    <!-- SERVICES -->
    <section id="services">
        <div class="section-head">
            <div>
                <div class="page-label">What We Offer</div>
                <div class="section-title" style="font-size:1.8rem;">Our Services</div>
            </div>
            <a href="services.php" class="btn btn-secondary btn-sm">View All</a>
        </div>

        <?php foreach ($byCategory as $cat => $items): ?>
            <div style="margin-bottom:36px;">
                <h3 style="font-family:'Barlow Condensed',sans-serif;font-size:1.1rem;font-weight:700;
                    color:var(--yellow);letter-spacing:2px;text-transform:uppercase;margin-bottom:14px;">
                    <?= htmlspecialchars($cat) ?>
                </h3>
                <div class="services-grid">
                    <?php foreach ($items as $svc): ?>
                    <div class="service-card">
                        <div class="service-name"><?= htmlspecialchars($svc['name']) ?></div>
                        <div class="service-desc"><?= htmlspecialchars($svc['description']) ?></div>
                        <div class="service-meta">
                            <div class="service-price">₱<?= number_format($svc['price'],2) ?></div>
                            <div class="service-dur">
                                <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                                <?= number_format($svc['duration_hr'],1) ?> hr<?= $svc['duration_hr'] != 1 ? 's' : '' ?>
                            </div>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        <?php endforeach; ?>
    </section>

    <!-- CONTACT -->
    <section id="contact" style="margin-top:40px;">
        <div class="card" style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:28px;align-items:start;">
            <div>
                <div class="page-label">Location</div>
                <div style="color:var(--text);line-height:1.7;">
                    123 Kalayaan Avenue<br>
                    Diliman, Quezon City<br>
                    Metro Manila, Philippines
                </div>
            </div>
            <div>
                <div class="page-label">Operating Hours</div>
                <div style="color:var(--text);line-height:1.7;">
                    Monday – Saturday<br>
                    8:00 AM – 6:00 PM<br>
                    <span style="color:var(--muted);">Closed Sundays & Holidays</span>
                </div>
            </div>
            <div>
                <div class="page-label">Contact Us</div>
                <div style="color:var(--text);line-height:1.7;">
                    📞 (02) 8-123-4567<br>
                    📱 0917-123-4567<br>
                    ✉️ info@maestroautoworks.ph
                </div>
            </div>
        </div>
    </section>

</main>

<?php require 'partials/footer.php'; ?>
