<?php
// P2: Full Services Catalog — services.php
session_start();
require 'db.php';
$pageTitle = 'Services — Maestro Autoworks';
require 'partials/header.php';

$services = $pdo->query(
    "SELECT * FROM services WHERE active = 1 ORDER BY category, name"
)->fetchAll();

$categories = array_unique(array_column($services, 'category'));
?>

<main class="page-shell">
    <div class="page-header">
        <div class="page-label">What We Do</div>
        <div class="page-title">Our Services</div>
        <div class="page-sub">Browse our full catalog of repair and maintenance offerings.</div>
    </div>

    <!-- Category filter tabs -->
    <div class="tabs" id="catTabs">
        <button class="tab-btn active" data-filter="all">All Services</button>
        <?php foreach ($categories as $cat): ?>
            <button class="tab-btn" data-filter="<?= htmlspecialchars($cat) ?>">
                <?= htmlspecialchars($cat) ?>
            </button>
        <?php endforeach; ?>
    </div>

    <div class="services-grid" id="servicesGrid">
        <?php foreach ($services as $svc): ?>
        <div class="service-card" data-category="<?= htmlspecialchars($svc['category']) ?>">
            <div class="service-category"><?= htmlspecialchars($svc['category']) ?></div>
            <div class="service-name"><?= htmlspecialchars($svc['name']) ?></div>
            <div class="service-desc"><?= htmlspecialchars($svc['description']) ?></div>
            <div class="service-meta">
                <div class="service-price">₱<?= number_format($svc['price'],2) ?></div>
                <div class="service-dur">
                    <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                    <?= number_format($svc['duration_hr'],1) ?> hr<?= $svc['duration_hr'] != 1 ? 's' : '' ?>
                </div>
            </div>
            <div style="margin-top:16px;">
                <a href="book.php?service=<?= $svc['id'] ?>" class="btn btn-primary btn-sm" style="width:100%;justify-content:center;">
                    <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                    Book This Service
                </a>
            </div>
        </div>
        <?php endforeach; ?>
    </div>
</main>

<script>
// Client-side category filter
document.querySelectorAll('#catTabs .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('#catTabs .tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const filter = btn.dataset.filter;
        document.querySelectorAll('#servicesGrid .service-card').forEach(card => {
            card.style.display = (filter === 'all' || card.dataset.category === filter) ? '' : 'none';
        });
    });
});
</script>

<?php require 'partials/footer.php'; ?>
