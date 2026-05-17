<?php
// P3: Booking Engine — book.php
session_start();
require 'db.php';
$pageTitle = 'Book Appointment — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] === 'admin') { header('Location: admin_dashboard.php'); exit; }

$services = $pdo->query("SELECT * FROM services WHERE active = 1 ORDER BY category, name")->fetchAll();

$error   = $_SESSION['book_error'] ?? '';
$old     = $_SESSION['book_old']   ?? [];
unset($_SESSION['book_error'], $_SESSION['book_old']);

$preselect = (int)($_GET['service'] ?? 0);
$today     = date('Y-m-d');
$maxDate   = date('Y-m-d', strtotime('+60 days'));

// Generate time slots 8am-5pm every 30 min
$slots = [];
$base = strtotime('08:00');
while ($base <= strtotime('17:00')) {
    $slots[] = date('H:i', $base);
    $base += 1800;
}

// Fully booked dates (>=5 pending/confirmed bookings)
$fullDatesStmt = $pdo->prepare("
    SELECT appt_date
    FROM   appointments
    WHERE  appt_date BETWEEN ? AND ?
      AND  status IN ('pending','confirmed')
    GROUP  BY appt_date
    HAVING COUNT(*) >= 5
");
$fullDatesStmt->execute([$today, $maxDate]);
$fullDates = array_column($fullDatesStmt->fetchAll(), 'appt_date');
?>

<main class="page-shell">
    <div class="page-header">
        <div class="page-label">P3 · Booking Engine</div>
        <div class="page-title">Book an Appointment</div>
        <div class="page-sub">Fill in the details below and we'll confirm your slot within 24 hours.</div>
    </div>

    <?php if ($error): ?>
        <div class="alert alert-error" data-auto-close>
            <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
            <?= htmlspecialchars($error) ?>
        </div>
    <?php endif; ?>

    <div style="display:grid;grid-template-columns:1fr 360px;gap:28px;align-items:start;">

        <!-- FORM -->
        <div class="card">
            <form action="book_process.php" method="POST" id="bookForm">

                <!-- Step 1: Service -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:16px;">
                        <span style="color:var(--yellow);">01</span> &nbsp;Choose a Service
                    </div>
                    <div class="form-group">
                        <label for="service_id">Service</label>
                        <select name="service_id" id="service_id" required>
                            <option value="">— Select a service —</option>
                            <?php foreach ($services as $svc): ?>
                                <option value="<?= $svc['id'] ?>"
                                    data-price="<?= $svc['price'] ?>"
                                    data-dur="<?= $svc['duration_hr'] ?>"
                                    data-cat="<?= htmlspecialchars($svc['category']) ?>"
                                    <?= (($old['service_id'] ?? $preselect) == $svc['id']) ? 'selected' : '' ?>>
                                    [<?= htmlspecialchars($svc['category']) ?>] <?= htmlspecialchars($svc['name']) ?> — ₱<?= number_format($svc['price'],2) ?>
                                </option>
                            <?php endforeach; ?>
                        </select>
                    </div>
                    <!-- Service preview card (populated by JS) -->
                    <div id="servicePreview" style="display:none;background:var(--navy-input);border-radius:10px;padding:16px;margin-top:12px;
                        border:1px solid var(--border);display:none;gap:14px;align-items:center;">
                        <div>
                            <div id="prevName" style="font-family:'Barlow Condensed',sans-serif;font-weight:700;font-size:1.1rem;color:var(--white);"></div>
                            <div id="prevMeta" style="font-size:13px;color:var(--muted);margin-top:4px;"></div>
                        </div>
                    </div>
                </div>

                <hr style="border:none;border-top:1px solid var(--border-sub);margin-bottom:28px;">

                <!-- Step 2: Schedule -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:16px;">
                        <span style="color:var(--yellow);">02</span> &nbsp;Pick a Date &amp; Time
                    </div>
                    <div class="form-row-2">
                        <div class="form-group" style="margin-bottom:0;">
                            <label for="appt_date">Date <small style="color:var(--muted);">(Mon–Sat)</small></label>
                            <input type="date" name="appt_date" id="appt_date"
                                   min="<?= $today ?>" max="<?= $maxDate ?>"
                                   value="<?= htmlspecialchars($old['appt_date'] ?? '') ?>" required>
                        </div>
                        <div class="form-group" style="margin-bottom:0;">
                            <label for="appt_time">Time Slot</label>
                            <select name="appt_time" id="appt_time" required>
                                <option value="">— Select time —</option>
                                <?php foreach ($slots as $slot): ?>
                                    <option value="<?= $slot ?>" <?= ($old['appt_time'] ?? '') === $slot ? 'selected' : '' ?>>
                                        <?= date('g:i A', strtotime($slot)) ?>
                                    </option>
                                <?php endforeach; ?>
                            </select>
                        </div>
                    </div>
                    <div id="dayWarning" class="alert alert-warning" style="display:none;margin-top:14px;margin-bottom:0;">
                        <svg viewBox="0 0 24 24"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
                        We are closed on Sundays. Please choose another day.
                    </div>
                    <div id="fullBookedWarning" class="alert alert-error" style="display:none;margin-top:14px;margin-bottom:0;">
                        <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                        This date is fully booked (max 5 vehicles reached). Please choose another date.
                    </div>
                </div>

                <hr style="border:none;border-top:1px solid var(--border-sub);margin-bottom:28px;">

                <!-- Step 3: Vehicle -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:16px;">
                        <span style="color:var(--yellow);">03</span> &nbsp;Vehicle Details
                    </div>
                    <div class="form-row-3">
                        <div class="form-group" style="margin-bottom:0;">
                            <label>Make</label>
                            <input type="text" name="vehicle_make" placeholder="e.g. Toyota"
                                   value="<?= htmlspecialchars($old['vehicle_make'] ?? '') ?>" required>
                        </div>
                        <div class="form-group" style="margin-bottom:0;">
                            <label>Model</label>
                            <input type="text" name="vehicle_model" placeholder="e.g. Vios"
                                   value="<?= htmlspecialchars($old['vehicle_model'] ?? '') ?>" required>
                        </div>
                        <div class="form-group" style="margin-bottom:0;">
                            <label>Year</label>
                            <input type="number" name="vehicle_year" placeholder="<?= date('Y') ?>"
                                   min="1960" max="<?= date('Y')+1 ?>"
                                   value="<?= htmlspecialchars($old['vehicle_year'] ?? '') ?>">
                        </div>
                    </div>
                    <div class="form-group" style="margin-top:16px;margin-bottom:0;">
                        <label>Plate Number <small style="color:var(--muted);">(optional)</small></label>
                        <input type="text" name="plate_no" placeholder="e.g. ABC 1234"
                               value="<?= htmlspecialchars($old['plate_no'] ?? '') ?>"
                               style="text-transform:uppercase;">
                    </div>

                    <!-- Fuel Type — mirrors BookActivity.java rgFuelType (required) -->
                    <div class="form-group" style="margin-top:16px;margin-bottom:0;">
                        <label>Fuel Type <span style="color:var(--danger);margin-left:2px;">*</span></label>
                        <div style="display:flex;gap:12px;margin-top:8px;" id="fuel-type-wrap">
                            <?php
                            $savedFuel = $old['fuel_type'] ?? '';
                            foreach (['Gasoline', 'Diesel'] as $fuel):
                                $icon  = $fuel === 'Gasoline' ? '⛽' : '🛢️';
                                $isChk = $savedFuel === $fuel;
                            ?>
                            <label id="fuel-label-<?= strtolower($fuel) ?>"
                                style="flex:1;display:flex;align-items:center;gap:12px;
                                    padding:14px 16px;border-radius:10px;cursor:pointer;
                                    border:2px solid <?= $isChk ? 'var(--yellow)' : 'var(--border)' ?>;
                                    background:<?= $isChk ? 'rgba(251,189,35,0.08)' : 'transparent' ?>;
                                    transition:border-color .15s,background .15s;">
                                <input type="radio" name="fuel_type" value="<?= $fuel ?>"
                                    id="fuel-<?= strtolower($fuel) ?>"
                                    <?= $isChk ? 'checked' : '' ?>
                                    style="accent-color:var(--yellow);width:16px;height:16px;flex-shrink:0;"
                                    onchange="selectFuel('<?= strtolower($fuel) ?>')">
                                <span style="font-size:18px;"><?= $icon ?></span>
                                <span style="font-weight:600;font-size:14px;color:var(--text);"><?= $fuel ?></span>
                            </label>
                            <?php endforeach; ?>
                        </div>
                        <div id="err-fuel_type" style="color:var(--danger);font-size:12px;
                            margin-top:6px;display:none;">Please select a fuel type.</div>
                    </div>
                </div>

                <hr style="border:none;border-top:1px solid var(--border-sub);margin-bottom:28px;">

                <!-- Step 4: Notes -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:16px;">
                        <span style="color:var(--yellow);">04</span> &nbsp;Additional Notes
                    </div>
                    <div class="form-group" style="margin-bottom:0;">
                        <label for="notes">Describe your concern <small style="color:var(--muted);">(optional)</small></label>
                        <textarea name="notes" id="notes" placeholder="e.g. Car makes a knocking sound when braking..."><?= htmlspecialchars($old['notes'] ?? '') ?></textarea>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;padding:15px;">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                    Submit Booking Request
                </button>
            </form>
        </div>

        <!-- SIDEBAR SUMMARY -->
        <div>
            <div class="card" id="summaryCard">
                <div class="page-label" style="margin-bottom:16px;">Booking Summary</div>

                <div id="summaryService" style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Service</div>
                    <div id="sumSvc" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
                </div>

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Date &amp; Time</div>
                    <div id="sumDate" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
                    <div id="sumTime" style="color:var(--muted);font-size:13px;margin-top:2px;">—</div>
                </div>

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Vehicle</div>
                    <div id="sumVehicle" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
                </div>

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Fuel Type</div>
                    <div id="sumFuel" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
                </div>

                <div style="display:flex;justify-content:space-between;align-items:center;">
                    <span style="font-size:13px;color:var(--muted);">Estimated Price</span>
                    <span id="sumPrice" style="font-family:'Barlow Condensed',sans-serif;font-size:1.4rem;font-weight:800;color:var(--yellow);">—</span>
                </div>

                <div style="margin-top:20px;padding:12px 14px;background:var(--yellow-lt);border:1px solid var(--border);border-radius:8px;font-size:13px;color:var(--muted);line-height:1.6;">
                    ⚡ Appointments are confirmed within <strong style="color:var(--yellow);">24 hours</strong> of submission.
                </div>
            </div>

            <div class="card" style="margin-top:16px;">
                <div class="page-label" style="margin-bottom:12px;">Shop Hours</div>
                <div style="font-size:14px;color:var(--muted);line-height:2;">
                    Monday – Saturday &nbsp;<strong style="color:var(--text);">8:00 AM – 6:00 PM</strong><br>
                    Sunday &nbsp;<strong style="color:var(--danger);">Closed</strong>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
(function () {
    const svcSel   = document.getElementById('service_id');
    const dateSel  = document.getElementById('appt_date');
    const timeSel  = document.getElementById('appt_time');
    const makeI    = document.querySelector('[name=vehicle_make]');
    const modelI   = document.querySelector('[name=vehicle_model]');
    const yearI    = document.querySelector('[name=vehicle_year]');
    const dayWarn  = document.getElementById('dayWarning');
    const submitBtn = document.querySelector('#bookForm [type=submit]');
    const fullBookedWarn = document.getElementById('fullBookedWarning');
    const fullDates = <?php echo json_encode($fullDates); ?>;

    function updateSummary () {
        const opt = svcSel.options[svcSel.selectedIndex];
        document.getElementById('sumSvc').textContent  = opt.value ? opt.text.replace(/\[.*?\]\s*/,'') : '—';
        document.getElementById('sumPrice').textContent = opt.value ? '₱' + parseFloat(opt.dataset.price).toLocaleString('en-PH',{minimumFractionDigits:2}) : '—';

        const d = dateSel.value;
        document.getElementById('sumDate').textContent = d ? new Date(d+'T00:00').toLocaleDateString('en-PH',{weekday:'short',month:'short',day:'numeric',year:'numeric'}) : '—';

        const t = timeSel.value;
        if (t) {
            const [h,m] = t.split(':');
            const dt = new Date(); dt.setHours(+h,+m);
            document.getElementById('sumTime').textContent = dt.toLocaleTimeString('en-PH',{hour:'numeric',minute:'2-digit'});
        } else {
            document.getElementById('sumTime').textContent = '—';
        }

        const make = makeI.value.trim(), model = modelI.value.trim(), yr = yearI.value.trim();
        document.getElementById('sumVehicle').textContent =
            (make || model) ? [yr, make, model].filter(Boolean).join(' ') : '—';

        // Fuel type
        const fuelChk = document.querySelector('input[name="fuel_type"]:checked');
        const fuelEl  = document.getElementById('sumFuel');
        if (fuelChk) {
            fuelEl.textContent = fuelChk.value === 'Gasoline' ? '⛽ Gasoline' : '🛢️ Diesel';
        } else {
            fuelEl.textContent = '—';
        }
    }

    // Fuel chip visual toggle — mirrors BookActivity rgFuelType listener
    window.selectFuel = function(type) {
        ['gasoline', 'diesel'].forEach(function(t) {
            const lbl = document.getElementById('fuel-label-' + t);
            const isSelected = t === type;
            lbl.style.borderColor = isSelected ? 'var(--yellow)' : 'var(--border)';
            lbl.style.background  = isSelected ? 'rgba(251,189,35,0.08)' : 'transparent';
        });
        document.getElementById('err-fuel_type').style.display = 'none';
        updateSummary();
    };

    // Client-side guard: fuel type is required before submit
    document.getElementById('bookForm').addEventListener('submit', function(e) {
        const fuelChk = document.querySelector('input[name="fuel_type"]:checked');
        if (!fuelChk) {
            e.preventDefault();
            const errEl = document.getElementById('err-fuel_type');
            errEl.style.display = 'block';
            errEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    });

    function checkDay () {
        if (!dateSel.value) {
            dayWarn.style.display       = 'none';
            fullBookedWarn.style.display = 'none';
            submitBtn.disabled = false;
            return;
        }
        const dow    = new Date(dateSel.value + 'T00:00').getDay(); // 0=Sun
        const isSun  = dow === 0;
        const isFull = fullDates.includes(dateSel.value);
        dayWarn.style.display        = isSun  ? 'flex' : 'none';
        fullBookedWarn.style.display = isFull ? 'flex' : 'none';
        submitBtn.disabled = isSun || isFull;
    }

    [svcSel, dateSel, timeSel].forEach(el => el.addEventListener('change', () => { updateSummary(); checkDay(); }));
    [makeI, modelI, yearI].forEach(el => el.addEventListener('input', updateSummary));
    document.querySelectorAll('input[name="fuel_type"]').forEach(el => el.addEventListener('change', updateSummary));

    // Restore chip styles if server reflected an old value back after error
    const preselectedFuel = document.querySelector('input[name="fuel_type"]:checked');
    if (preselectedFuel) selectFuel(preselectedFuel.value.toLowerCase());

    // Disable Sundays on date input (HTML min/max don't block DOW; we use the change handler + warn)
    updateSummary();
})();
</script>

<?php require 'partials/footer.php'; ?>
