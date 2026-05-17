<?php
require 'db.php';
session_start();
$pageTitle = 'About Us — Maestro Autoworks';
require 'partials/header.php';
?>

<main class="page-shell">

    <!-- PAGE HEADER -->
    <div class="page-header">
        <div class="page-label">Our Story</div>
        <h1 class="page-title">About Maestro<br><span style="color:var(--yellow)">Autoworks</span></h1>
        <p class="page-sub">Trusted by thousands of Filipino motorists since 2010.</p>
    </div>

    <!-- HERO BLURB + STATS -->
    <section style="margin-bottom:56px;">
        <div class="about-grid">
            <div>
                <p style="color:var(--muted);line-height:1.9;font-size:15px;margin-bottom:18px;">
                    Founded in 2010, Maestro Autoworks has grown from a small garage in Quezon City into
                    one of the most trusted auto repair shops in Metro Manila. Our team of certified
                    mechanics combines modern diagnostic technology with old-school craftsmanship.
                </p>
                <p style="color:var(--muted);line-height:1.9;font-size:15px;margin-bottom:18px;">
                    We believe in transparent pricing, honest assessments, and repairs done right the
                    first time — no upsells, no guesswork.
                </p>
                <p style="color:var(--muted);line-height:1.9;font-size:15px;">
                    From routine oil changes to complex engine overhauls, every job gets the same level
                    of attention, care, and accountability that has defined our reputation for over a decade.
                </p>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;align-content:start;">
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
                    <div class="stat-label" style="margin-top:8px;"><?= $lbl ?></div>
                </div>
                <?php endforeach; ?>
            </div>
        </div>
    </section>

    <!-- WHY CHOOSE US -->
    <section style="margin-bottom:56px;">
        <div class="page-label">Why Choose Us</div>
        <h2 style="font-family:'Barlow Condensed',sans-serif;font-size:clamp(1.6rem,2.5vw,2.2rem);
            font-weight:800;color:var(--white);margin-bottom:32px;line-height:1.1;">
            What Sets Us Apart
        </h2>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:20px;">
            <?php
            $pillars = [
                [
                    'icon' => '<path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>',
                    'title' => 'Certified Mechanics',
                    'desc'  => 'Every technician is certified and trained on modern diagnostic equipment. No shortcuts — just skilled hands on your car.',
                ],
                [
                    'icon' => '<path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm4.24 16L12 15.45 7.77 18l1.12-4.81-3.73-3.23 4.92-.42L12 5l1.92 4.53 4.92.42-3.73 3.23L16.23 18z"/>',
                    'title' => 'Transparent Pricing',
                    'desc'  => 'You\'ll know the cost before we start. No hidden fees, no surprise charges — just honest quotes and honest work.',
                ],
                [
                    'icon' => '<path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>',
                    'title' => 'Warranty on Work',
                    'desc'  => 'Every repair comes backed by our service guarantee. If something\'s not right, we\'ll make it right — at no extra cost.',
                ],
                [
                    'icon' => '<path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/>',
                    'title' => 'Easy Online Booking',
                    'desc'  => 'Schedule your service in minutes — pick a date, choose a time, describe your concern, and we handle the rest.',
                ],
                [
                    'icon' => '<path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>',
                    'title' => 'Real-Time Updates',
                    'desc'  => 'Get notified at every step — from booking confirmation to job completion — through the app and your dashboard.',
                ],
                [
                    'icon' => '<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>',
                    'title' => 'Quezon City Based',
                    'desc'  => 'Conveniently located in Metro Manila, with ample parking and a comfortable waiting area while we service your vehicle.',
                ],
            ];
            foreach ($pillars as $p):
            ?>
            <div class="card" style="display:flex;gap:18px;align-items:flex-start;">
                <div class="stat-icon" style="flex-shrink:0;">
                    <svg viewBox="0 0 24 24"><?= $p['icon'] ?></svg>
                </div>
                <div>
                    <div class="card-title" style="margin-bottom:8px;"><?= $p['title'] ?></div>
                    <p style="color:var(--muted);font-size:14px;line-height:1.7;margin:0;"><?= $p['desc'] ?></p>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
    </section>

    <!-- TIMELINE -->
    <section style="margin-bottom:56px;">
        <div class="page-label">Our Journey</div>
        <h2 style="font-family:'Barlow Condensed',sans-serif;font-size:clamp(1.6rem,2.5vw,2.2rem);
            font-weight:800;color:var(--white);margin-bottom:36px;line-height:1.1;">
            From Garage to Metro Manila's Most Trusted
        </h2>
        <div style="position:relative;padding-left:32px;">
            <!-- vertical line -->
            <div style="position:absolute;left:11px;top:8px;bottom:8px;width:2px;background:var(--border-sub);"></div>
            <?php
            $timeline = [
                ['2010', 'Founded', 'Maestro Autoworks opens its doors in a small garage in Quezon City, founded by a team of three certified mechanics with a simple mission: honest repairs, fair prices.'],
                ['2013', 'Expanded the Bay', 'Grew to a 6-bay facility to accommodate increasing demand. Added a dedicated diagnostic station with OBD-II scanning capability.'],
                ['2016', 'Certified Team', 'All technicians completed TESDA NC II certification. Introduced a formal quality-check process before every vehicle handover.'],
                ['2019', 'Digital Booking', 'Launched online appointment scheduling, reducing wait times and giving customers more control over when they come in.'],
                ['2022', 'Launched the App', 'Released the Maestro Autoworks mobile app, bringing real-time appointment tracking and service updates to every customer\'s pocket.'],
                ['Today', 'Still Growing', 'Serving 50+ vehicles daily with a team of 12 certified mechanics — and the same commitment to quality that started in 2010.'],
            ];
            foreach ($timeline as $idx => [$year, $title, $desc]):
            ?>
            <div style="position:relative;margin-bottom:36px;display:flex;gap:24px;align-items:flex-start;">
                <!-- dot -->
                <div style="position:absolute;left:-32px;top:4px;width:22px;height:22px;border-radius:50%;
                    background:<?= $idx === count($timeline)-1 ? 'var(--yellow)' : 'var(--black-card)' ?>;
                    border:2px solid <?= $idx === count($timeline)-1 ? 'var(--yellow)' : 'var(--border)' ?>;
                    flex-shrink:0;"></div>
                <div style="padding-top:0;">
                    <div style="font-family:'Barlow Condensed',sans-serif;font-size:11px;font-weight:700;
                        letter-spacing:2px;text-transform:uppercase;color:var(--yellow);margin-bottom:4px;">
                        <?= $year ?>
                    </div>
                    <div style="font-family:'Barlow Condensed',sans-serif;font-size:1.1rem;font-weight:700;
                        color:var(--white);margin-bottom:6px;">
                        <?= $title ?>
                    </div>
                    <p style="color:var(--muted);font-size:14px;line-height:1.7;margin:0;"><?= $desc ?></p>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
    </section>

    <!-- CTA -->
    <section>
        <div class="card" style="text-align:center;padding:48px 32px;background:linear-gradient(135deg,var(--black-card) 0%,rgba(255,200,0,0.06) 100%);border-color:var(--border);">
            <div class="page-label" style="text-align:center;">Ready to Get Started?</div>
            <h2 style="font-family:'Barlow Condensed',sans-serif;font-size:clamp(1.8rem,3vw,2.6rem);
                font-weight:800;color:var(--white);margin:8px 0 16px;line-height:1.1;">
                Book a Service Today
            </h2>
            <p style="color:var(--muted);font-size:15px;max-width:480px;margin:0 auto 28px;line-height:1.7;">
                Schedule your appointment online in under 2 minutes. Our team will confirm your slot
                and keep you updated every step of the way.
            </p>
            <div style="display:flex;gap:14px;justify-content:center;flex-wrap:wrap;">
                <a href="book.php" class="btn btn-primary">
                    <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                    Book an Appointment
                </a>
                <a href="services.php" class="btn btn-secondary">
                    <svg viewBox="0 0 24 24"><path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/></svg>
                    Explore Services
                </a>
            </div>
        </div>
    </section>

</main>

<?php require 'partials/footer.php'; ?>
