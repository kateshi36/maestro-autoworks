<?php
// search.php — Search services and appointments
// Mirrors SearchActivity: autocomplete on service names, shows service detail card + book link.
// Extended for web: also searches the user's own appointment history.

session_start();
require 'db.php';
$pageTitle = 'Search — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] === 'admin') { header('Location: admin_dashboard.php'); exit; }

// ── Fetch all active services for autocomplete + search ──────────────────────
$allServices = $pdo->query(
    "SELECT * FROM services ORDER BY category, name"
)->fetchAll();

// Build autocomplete name list (JSON-safe)
$serviceNames = array_map(fn($s) => $s['name'], $allServices);

// ── Handle search query ───────────────────────────────────────────────────────
$q              = trim($_GET['q'] ?? '');
$filterType     = $_GET['type'] ?? 'all';   // 'all' | 'services' | 'appointments'

$serviceResults     = [];
$appointmentResults = [];

if ($q !== '') {
    $like = '%' . $q . '%';

    // ── Service search (name, description, category) ──────────────────────────
    if ($filterType !== 'appointments') {
        $stmt = $pdo->prepare(
            "SELECT * FROM services
             WHERE (name LIKE ? OR description LIKE ? OR category LIKE ?)
             ORDER BY active DESC, category, name"
        );
        $stmt->execute([$like, $like, $like]);
        $serviceResults = $stmt->fetchAll();
    }

    // ── Appointment search (own records only) ─────────────────────────────────
    if ($filterType !== 'services') {
        $stmt = $pdo->prepare(
            "SELECT a.*, s.name as service_name, s.price, s.duration_hr, s.category
             FROM appointments a
             JOIN services s ON s.id = a.service_id
             WHERE a.user_id = ?
               AND (s.name LIKE ? OR a.vehicle_make LIKE ? OR a.vehicle_model LIKE ?
                    OR a.plate_no LIKE ? OR a.status LIKE ? OR a.notes LIKE ?)
             ORDER BY a.appt_date DESC"
        );
        $stmt->execute([
            $me['id'],
            $like, $like, $like, $like, $like, $like
        ]);
        $appointmentResults = $stmt->fetchAll();
    }
}

$totalResults = count($serviceResults) + count($appointmentResults);
?>

<main class="page-shell">

    <div class="page-header">
        <div class="page-label">Search</div>
        <div class="page-title">Find a Service<br>or Appointment</div>
        <div class="page-sub">Search by service name, category, vehicle, plate number, or booking status.</div>
    </div>

    <!-- ── Search bar ──────────────────────────────────────────────────────── -->
    <div class="search-bar-wrap">
        <form method="GET" action="search.php" id="searchForm" autocomplete="off">
            <div class="search-bar">
                <div class="search-icon">
                    <svg viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
                </div>

                <input
                    type="text"
                    id="searchInput"
                    name="q"
                    class="search-input"
                    placeholder="e.g. Oil Change, brake, Toyota, ABC-1234…"
                    value="<?= htmlspecialchars($q) ?>"
                    list="serviceAutocomplete"
                    autofocus
                >
                <!-- Datalist for native browser autocomplete on service names -->
                <datalist id="serviceAutocomplete">
                    <?php foreach ($serviceNames as $name): ?>
                        <option value="<?= htmlspecialchars($name) ?>">
                    <?php endforeach; ?>
                </datalist>

                <?php if ($q !== ''): ?>
                <button type="button" class="search-clear" onclick="clearSearch()" title="Clear">
                    <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                </button>
                <?php endif; ?>

                <button type="submit" class="btn btn-primary search-go">
                    Search
                </button>
            </div>

            <!-- Filter pills -->
            <div class="search-filters">
                <span class="filter-label">Show:</span>
                <?php
                $types = [
                    'all'          => 'All Results',
                    'services'     => 'Services Only',
                    'appointments' => 'My Bookings Only',
                ];
                foreach ($types as $val => $label): ?>
                    <label class="filter-pill <?= $filterType === $val ? 'active' : '' ?>">
                        <input type="radio" name="type" value="<?= $val ?>"
                               <?= $filterType === $val ? 'checked' : '' ?>
                               onchange="this.form.submit()">
                        <?= $label ?>
                    </label>
                <?php endforeach; ?>
            </div>
        </form>
    </div>

    <!-- ── Autocomplete suggestion dropdown (JS-powered) ──────────────────── -->
    <div id="suggestBox" class="suggest-box" style="display:none;"></div>

    <!-- ── Results ─────────────────────────────────────────────────────────── -->
    <?php if ($q === ''): ?>
    <!-- Empty state — no query yet -->
    <div class="search-empty">
        <div class="search-empty-icon">
            <svg viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        </div>
        <div class="search-empty-title">Start Searching</div>
        <div class="search-empty-sub">Type a service name, category, vehicle, or booking status above to get started.</div>
        <!-- Quick-launch chips -->
        <div class="quick-chips">
            <?php
            $quickTerms = ['Oil Change', 'Brake', 'Tune-Up', 'Alignment', 'Engine', 'pending', 'completed'];
            foreach ($quickTerms as $term): ?>
                <a href="search.php?q=<?= urlencode($term) ?>&type=all" class="quick-chip"><?= htmlspecialchars($term) ?></a>
            <?php endforeach; ?>
        </div>
    </div>

    <?php elseif ($totalResults === 0): ?>
    <!-- No results -->
    <div class="search-empty">
        <div class="search-empty-icon" style="background:rgba(224,82,82,0.08);border-color:rgba(224,82,82,0.2);">
            <svg viewBox="0 0 24 24" style="fill:var(--danger)"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/></svg>
        </div>
        <div class="search-empty-title">No results for "<?= htmlspecialchars($q) ?>"</div>
        <div class="search-empty-sub">Try a different keyword — service name, category, vehicle make, plate number, or status.</div>
        <div class="quick-chips">
            <?php foreach (['Oil Change', 'Brake', 'Tune-Up', 'Alignment'] as $t): ?>
                <a href="search.php?q=<?= urlencode($t) ?>&type=all" class="quick-chip"><?= $t ?></a>
            <?php endforeach; ?>
        </div>
    </div>

    <?php else: ?>
    <!-- Results header -->
    <div class="results-header">
        <span class="results-count">
            <?= $totalResults ?> result<?= $totalResults !== 1 ? 's' : '' ?> for
            "<strong><?= htmlspecialchars($q) ?></strong>"
        </span>
        <a href="search.php" class="btn btn-secondary btn-sm">Clear search</a>
    </div>

    <!-- ── SERVICE RESULTS ──────────────────────────────────────────────────── -->
    <?php if (!empty($serviceResults)): ?>
    <div class="section-title" style="margin-bottom:16px;">
        <svg viewBox="0 0 24 24"><path d="M19.43 12.98c.04-.32.07-.64.07-.98 0-.34-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.23-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98 0 .33.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.23.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z"/></svg>
        Services
        <span class="results-section-count"><?= count($serviceResults) ?></span>
    </div>

    <div class="services-grid" style="margin-bottom:40px;">
        <?php foreach ($serviceResults as $svc):
            $isAvail = (bool)$svc['active'];
        ?>
        <div class="service-card" style="position:relative;">
            <?php if (!$isAvail): ?>
            <div style="position:absolute;top:14px;right:14px;background:#E05252;color:#fff;
                font-size:10px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;
                padding:3px 10px;border-radius:20px;">Unavailable</div>
            <?php endif; ?>

            <div class="service-category"><?= htmlspecialchars($svc['category']) ?></div>
            <div class="service-name"><?= highlight($svc['name'], $q) ?></div>
            <div class="service-desc"><?= highlight(htmlspecialchars($svc['description']), $q) ?></div>
            <div class="service-meta">
                <div class="service-price">₱<?= number_format($svc['price'], 2) ?></div>
                <div class="service-dur">
                    <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/></svg>
                    <?= number_format($svc['duration_hr'], 1) ?> hr<?= $svc['duration_hr'] != 1 ? 's' : '' ?>
                </div>
            </div>
            <div style="margin-top:16px;">
                <?php if ($isAvail): ?>
                    <a href="book.php?service=<?= $svc['id'] ?>" class="btn btn-primary btn-sm" style="width:100%;justify-content:center;">
                        <svg viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H5V8h14v13zM7 10h5v5H7z"/></svg>
                        Book This Service
                    </a>
                <?php else: ?>
                    <button class="btn btn-secondary btn-sm" style="width:100%;justify-content:center;cursor:not-allowed;opacity:.6;" disabled>Currently Unavailable</button>
                <?php endif; ?>
            </div>
        </div>
        <?php endforeach; ?>
    </div>
    <?php endif; ?>

    <!-- ── APPOINTMENT RESULTS ───────────────────────────────────────────────── -->
    <?php if (!empty($appointmentResults)): ?>
    <div class="section-title" style="margin-bottom:16px;">
        <svg viewBox="0 0 24 24"><path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/></svg>
        My Bookings
        <span class="results-section-count"><?= count($appointmentResults) ?></span>
    </div>

    <div style="display:flex;flex-direction:column;gap:12px;margin-bottom:40px;">
        <?php foreach ($appointmentResults as $appt):
            $statusClass = 'badge-' . $appt['status'];
            $vehicle = trim(($appt['vehicle_make'] ?? '') . ' ' . ($appt['vehicle_model'] ?? ''));
            if (!$vehicle) $vehicle = '—';
        ?>
        <div class="card card-sm appt-result-row">
            <div class="appt-result-left">
                <div class="appt-result-service"><?= highlight(htmlspecialchars($appt['service_name']), $q) ?></div>
                <div class="appt-result-meta">
                    <span>
                        <svg viewBox="0 0 24 24"><path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/></svg>
                        <?= date('M j, Y', strtotime($appt['appt_date'])) ?>
                        &nbsp;at&nbsp;
                        <?= date('g:i A', strtotime($appt['appt_time'])) ?>
                    </span>
                    <span>
                        <svg viewBox="0 0 24 24"><path d="M18.92 5.01C18.72 4.42 18.16 4 17.5 4h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-6.99z"/></svg>
                        <?= highlight(htmlspecialchars($vehicle), $q) ?>
                        <?php if ($appt['plate_no']): ?>
                            &nbsp;·&nbsp; <?= highlight(htmlspecialchars($appt['plate_no']), $q) ?>
                        <?php endif; ?>
                    </span>
                    <?php if ($appt['notes']): ?>
                    <span class="appt-notes-snip">
                        <svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
                        <?= highlight(htmlspecialchars(mb_strimwidth($appt['notes'], 0, 80, '…')), $q) ?>
                    </span>
                    <?php endif; ?>
                </div>
            </div>
            <div class="appt-result-right">
                <span class="badge <?= $statusClass ?>">
                    <span class="badge-dot"></span>
                    <?= ucfirst($appt['status']) ?>
                </span>
                <a href="dashboard.php" class="btn btn-secondary btn-sm" style="margin-top:10px;">
                    View in Dashboard
                </a>
            </div>
        </div>
        <?php endforeach; ?>
    </div>
    <?php endif; ?>

    <?php endif; // end results block ?>

</main>

<?php
// ── Helper: wrap matched text in a highlight span ─────────────────────────────
function highlight(string $haystack, string $needle): string {
    if ($needle === '') return $haystack;
    $safe   = preg_quote(htmlspecialchars($needle), '/');
    return preg_replace(
        '/(' . $safe . ')/i',
        '<mark class="hl">$1</mark>',
        $haystack
    );
}
?>

<style>
/* ── Search bar ──────────────────────────────────────────────────────────── */
.search-bar-wrap {
    max-width: 720px;
    margin-bottom: 32px;
}

.search-bar {
    display: flex;
    align-items: center;
    gap: 0;
    background: var(--black-card);
    border: 1.5px solid var(--border);
    border-radius: 12px;
    overflow: hidden;
    transition: border-color .2s, box-shadow .2s;
}

.search-bar:focus-within {
    border-color: var(--yellow);
    box-shadow: 0 0 0 3px rgba(245,166,35,0.12);
}

.search-icon {
    padding: 0 16px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
}

.search-icon svg { width: 18px; height: 18px; fill: var(--muted); }

.search-input {
    flex: 1;
    background: none;
    border: none;
    outline: none;
    padding: 16px 0;
    font-family: 'Barlow', sans-serif;
    font-size: 15px;
    color: var(--text);
}

.search-input::placeholder { color: var(--muted); }

.search-clear {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0 12px;
    display: flex;
    align-items: center;
}

.search-clear svg { width: 16px; height: 16px; fill: var(--muted); }
.search-clear:hover svg { fill: var(--danger); }

.search-go {
    border-radius: 0 10px 10px 0 !important;
    padding: 16px 24px !important;
    font-size: 13px !important;
    flex-shrink: 0;
}

/* ── Filter pills ────────────────────────────────────────────────────────── */
.search-filters {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    flex-wrap: wrap;
}

.filter-label {
    font-size: 12px;
    color: var(--muted);
    font-weight: 600;
    letter-spacing: .5px;
    text-transform: uppercase;
    margin-right: 4px;
}

.filter-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 14px;
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.10);
    background: var(--black-card);
    font-size: 12px;
    color: var(--muted);
    cursor: pointer;
    transition: all .2s;
    user-select: none;
}

.filter-pill input { display: none; }

.filter-pill:hover { border-color: rgba(245,166,35,0.35); color: var(--text); }

.filter-pill.active {
    background: rgba(245,166,35,0.10);
    border-color: rgba(245,166,35,0.45);
    color: var(--yellow);
    font-weight: 600;
}

/* ── JS autocomplete suggestion box ─────────────────────────────────────── */
.suggest-box {
    position: absolute;
    z-index: 200;
    background: var(--black-card);
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 16px 40px rgba(0,0,0,0.5);
    max-width: 720px;
    width: 100%;
}

.suggest-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 18px;
    cursor: pointer;
    transition: background .15s;
    font-size: 14px;
    color: var(--text);
}

.suggest-item:hover, .suggest-item.active {
    background: rgba(245,166,35,0.08);
}

.suggest-item-cat {
    font-size: 11px;
    color: var(--yellow);
    font-weight: 700;
    letter-spacing: 1px;
    text-transform: uppercase;
    flex-shrink: 0;
    min-width: 80px;
}

/* ── Empty / zero-query states ───────────────────────────────────────────── */
.search-empty {
    text-align: center;
    padding: 64px 24px;
}

.search-empty-icon {
    width: 72px; height: 72px;
    border-radius: 50%;
    background: rgba(245,166,35,0.08);
    border: 1px solid rgba(245,166,35,0.2);
    display: flex; align-items: center; justify-content: center;
    margin: 0 auto 20px;
}

.search-empty-icon svg { width: 32px; height: 32px; fill: var(--yellow); }

.search-empty-title {
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 1.5rem; font-weight: 800;
    color: var(--white); margin-bottom: 8px;
}

.search-empty-sub {
    font-size: 14px; color: var(--muted); max-width: 400px;
    margin: 0 auto 28px; line-height: 1.65;
}

.quick-chips {
    display: flex; flex-wrap: wrap;
    gap: 8px; justify-content: center;
}

.quick-chip {
    padding: 6px 16px;
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.10);
    background: var(--black-card);
    font-size: 13px; color: var(--muted);
    text-decoration: none; transition: all .2s;
}

.quick-chip:hover {
    border-color: rgba(245,166,35,0.4);
    color: var(--yellow);
    background: rgba(245,166,35,0.06);
}

/* ── Results header ──────────────────────────────────────────────────────── */
.results-header {
    display: flex; align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
    flex-wrap: wrap; gap: 10px;
}

.results-count {
    font-size: 14px; color: var(--muted);
}

.results-count strong { color: var(--text); }

.results-section-count {
    display: inline-flex; align-items: center; justify-content: center;
    width: 22px; height: 22px; border-radius: 50%;
    background: rgba(245,166,35,0.15);
    color: var(--yellow); font-size: 11px; font-weight: 700;
    margin-left: 8px;
}

/* ── Section title override ──────────────────────────────────────────────── */
.section-title {
    display: flex; align-items: center; gap: 10px;
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 13px; font-weight: 700; letter-spacing: 2px;
    text-transform: uppercase; color: var(--muted);
}

.section-title svg { width: 15px; height: 15px; fill: var(--yellow); }

/* ── Appointment result row ──────────────────────────────────────────────── */
.appt-result-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
}

.appt-result-left { flex: 1; min-width: 0; }

.appt-result-service {
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 1.05rem; font-weight: 700; color: var(--white);
    margin-bottom: 8px;
}

.appt-result-meta {
    display: flex; flex-direction: column; gap: 5px;
}

.appt-result-meta span {
    display: flex; align-items: center; gap: 7px;
    font-size: 13px; color: var(--muted);
}

.appt-result-meta svg { width: 13px; height: 13px; fill: var(--muted); flex-shrink: 0; }

.appt-notes-snip { font-style: italic; opacity: .8; }

.appt-result-right {
    display: flex; flex-direction: column;
    align-items: flex-end; flex-shrink: 0;
}

/* ── Highlight mark ──────────────────────────────────────────────────────── */
mark.hl {
    background: rgba(245,166,35,0.25);
    color: var(--yellow);
    border-radius: 2px;
    padding: 0 1px;
}

@media (max-width: 600px) {
    .search-go { padding: 16px 16px !important; }
    .appt-result-row { flex-direction: column; }
    .appt-result-right { align-items: flex-start; flex-direction: row; gap: 10px; flex-wrap: wrap; }
}
</style>

<script>
// ── JS autocomplete suggestion dropdown ───────────────────────────────────────
(function () {
    const input      = document.getElementById('searchInput');
    const box        = document.getElementById('suggestBox');
    const allNames   = <?= json_encode($serviceNames, JSON_HEX_TAG) ?>;
    let   activeIdx  = -1;

    // Position the dropdown under the search bar
    function positionBox() {
        const wrap = input.closest('.search-bar-wrap');
        const rect = wrap.getBoundingClientRect();
        box.style.top    = (rect.bottom + window.scrollY + 4) + 'px';
        box.style.left   = rect.left + 'px';
        box.style.width  = rect.width + 'px';
        box.style.position = 'absolute';
    }

    function buildSuggestions(q) {
        if (!q || q.length < 1) { box.style.display = 'none'; return; }
        const lower = q.toLowerCase();

        // Find all services from the PHP-rendered allServices data
        const services = <?= json_encode(array_map(fn($s) => [
            'name'     => $s['name'],
            'category' => $s['category'],
        ], $allServices), JSON_HEX_TAG) ?>;

        const matches = services.filter(s =>
            s.name.toLowerCase().includes(lower) ||
            s.category.toLowerCase().includes(lower)
        ).slice(0, 6);

        if (!matches.length) { box.style.display = 'none'; return; }

        box.innerHTML = matches.map((s, i) => `
            <div class="suggest-item" data-idx="${i}" data-name="${s.name.replace(/"/g,'&quot;')}">
                <span class="suggest-item-cat">${s.category}</span>
                <span>${s.name}</span>
            </div>
        `).join('');

        positionBox();
        box.style.display = 'block';
        activeIdx = -1;

        box.querySelectorAll('.suggest-item').forEach(item => {
            item.addEventListener('mousedown', e => {
                e.preventDefault();
                input.value = item.dataset.name;
                box.style.display = 'none';
                input.closest('form').submit();
            });
        });
    }

    input.addEventListener('input', () => buildSuggestions(input.value.trim()));

    input.addEventListener('keydown', e => {
        const items = box.querySelectorAll('.suggest-item');
        if (!items.length || box.style.display === 'none') return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIdx = Math.min(activeIdx + 1, items.length - 1);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIdx = Math.max(activeIdx - 1, -1);
        } else if (e.key === 'Enter' && activeIdx >= 0) {
            e.preventDefault();
            input.value = items[activeIdx].dataset.name;
            box.style.display = 'none';
            input.closest('form').submit();
        } else if (e.key === 'Escape') {
            box.style.display = 'none';
        }
        items.forEach((item, i) => item.classList.toggle('active', i === activeIdx));
        if (activeIdx >= 0) input.value = items[activeIdx].dataset.name;
    });

    document.addEventListener('click', e => {
        if (!box.contains(e.target) && e.target !== input) {
            box.style.display = 'none';
        }
    });

    window.addEventListener('resize', () => { if (box.style.display !== 'none') positionBox(); });
})();

// ── Clear search helper ────────────────────────────────────────────────────────
function clearSearch() {
    document.getElementById('searchInput').value = '';
    window.location = 'search.php';
}
</script>

<?php require 'partials/footer.php'; ?>
