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
            <form action="book_process.php" method="POST" id="bookForm" enctype="multipart/form-data">

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

                <!-- Step 4: Vehicle Concerns checklist -->
                <!-- Mirrors BookActivity.java setupCheckBoxes() / updateConcernSummary() -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:6px;">
                        <span style="color:var(--yellow);">04</span> &nbsp;Vehicle Concerns
                    </div>
                    <p style="font-size:13px;color:var(--muted);margin:0 0 16px;line-height:1.6;">
                        Select all concerns that apply to your vehicle:
                    </p>

                    <?php
                    // Exact labels from activity_book.xml — order preserved
                    $concerns = [
                        'Engine'     => '🔧  Engine — knocking, misfires, or rough idle',
                        'Brakes'     => '🛑  Brakes — squealing, grinding, or soft pedal',
                        'Aircon'     => '❄️  Air-Con — not cooling or unusual odour',
                        'Electrical' => '⚡  Electrical — battery, lights, or dashboard warnings',
                        'Tires'      => '🛞  Tires / Wheels — vibration, pulling, or uneven wear',
                        'Oil'        => '🛢️  Oil / Fluids — leaks or low fluid levels',
                        'Steering'   => '🔄  Steering / Suspension — stiffness or clunking',
                        'Exhaust'    => '💨  Exhaust — smoke colour changes or loud noise',
                    ];
                    // Restore checked state after a validation error redirect
                    $savedConcerns = array_filter(
                        array_map('trim', explode(',', $old['vehicle_concerns'] ?? ''))
                    );
                    ?>

                    <div id="concerns-wrap" style="display:flex;flex-direction:column;gap:8px;">
                        <?php foreach ($concerns as $key => $label):
                            $isChecked = in_array($label, $savedConcerns);
                        ?>
                        <label id="concern-label-<?= $key ?>"
                            style="display:flex;align-items:center;gap:14px;
                                padding:13px 16px;border-radius:10px;cursor:pointer;
                                border:2px solid <?= $isChecked ? 'var(--yellow)' : 'var(--border)' ?>;
                                background:<?= $isChecked ? 'rgba(251,189,35,0.07)' : 'transparent' ?>;
                                transition:border-color .15s,background .15s;user-select:none;">
                            <input type="checkbox"
                                name="vehicle_concerns[]"
                                value="<?= htmlspecialchars($label) ?>"
                                id="concern-<?= $key ?>"
                                <?= $isChecked ? 'checked' : '' ?>
                                style="accent-color:var(--yellow);width:17px;height:17px;flex-shrink:0;"
                                onchange="toggleConcern(this, '<?= $key ?>')">
                            <span style="font-size:14px;color:var(--text);line-height:1.4;">
                                <?= htmlspecialchars($label) ?>
                            </span>
                        </label>
                        <?php endforeach; ?>
                    </div>

                    <!-- Live concern summary — mirrors layoutConcernSummary / tvConcernSummaryText -->
                    <!-- Hidden until at least one concern is ticked -->
                    <div id="concern-summary" style="display:none;margin-top:14px;
                        background:var(--black-input);border:1px solid rgba(251,189,35,0.3);
                        border-radius:10px;padding:14px 16px;">
                        <div style="font-size:11px;font-weight:700;letter-spacing:1.5px;
                            text-transform:uppercase;color:var(--yellow);margin-bottom:10px;">
                            Selected Concerns
                        </div>
                        <ul id="concern-summary-list"
                            style="margin:0;padding-left:18px;color:var(--muted);
                                font-size:13px;line-height:2;"></ul>
                    </div>
                </div>

                <hr style="border:none;border-top:1px solid var(--border-sub);margin-bottom:28px;">

                <!-- Step 5: Additional Notes -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:16px;">
                        <span style="color:var(--yellow);">05</span> &nbsp;Additional Notes
                    </div>
                    <div class="form-group" style="margin-bottom:0;">
                        <label for="notes">Any other details <small style="color:var(--muted);">(optional)</small></label>
                        <textarea name="notes" id="notes"
                            placeholder="e.g. Sound only happens when turning left at speed..."
                            ><?= htmlspecialchars($old['notes'] ?? '') ?></textarea>
                    </div>
                </div>

                <hr style="border:none;border-top:1px solid var(--border-sub);margin-bottom:28px;">

                <!-- Step 6: OR/CR Upload — mirrors Appointment.orcrStatus / orcrImagePath -->
                <!-- App hardcodes "N/A" / null because the UI was never wired; we implement it properly here -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:6px;">
                        <span style="color:var(--yellow);">06</span> &nbsp;Official Receipt / Certificate of Registration
                        <small style="color:var(--muted);font-weight:400;margin-left:6px;">(optional)</small>
                    </div>
                    <p style="font-size:13px;color:var(--muted);margin:0 0 16px;line-height:1.6;">
                        Upload a photo of your OR/CR if available. Accepted: JPG, PNG, WEBP — max 5 MB.
                    </p>

                    <!-- Upload drop-zone -->
                    <div id="orcr-dropzone"
                        onclick="document.getElementById('orcr_image').click()"
                        style="border:2px dashed var(--border);border-radius:12px;padding:28px 20px;
                            text-align:center;cursor:pointer;transition:border-color .2s,background .2s;
                            background:transparent;" role="button" tabindex="0"
                        onkeydown="if(event.key==='Enter'||event.key===' ')this.click()"
                        ondragover="event.preventDefault();this.style.borderColor='var(--yellow)'"
                        ondragleave="this.style.borderColor='var(--border)'"
                        ondrop="handleOrcrDrop(event)">
                        <div style="font-size:32px;margin-bottom:8px;">📄</div>
                        <div style="font-weight:600;color:var(--text);font-size:14px;">Click or drag to upload OR/CR</div>
                        <div style="font-size:12px;color:var(--muted);margin-top:4px;">JPG · PNG · WEBP · max 5 MB</div>
                    </div>
                    <input type="file" name="orcr_image" id="orcr_image"
                        accept="image/jpeg,image/png,image/webp"
                        style="display:none;" onchange="previewOrcr(this)">

                    <!-- Preview (hidden until file chosen) -->
                    <div id="orcr-preview" style="display:none;margin-top:14px;
                        background:var(--navy-input);border:1px solid rgba(251,189,35,0.3);
                        border-radius:10px;padding:14px 16px;display:none;
                        align-items:center;gap:14px;">
                        <img id="orcr-thumb" src="" alt="OR/CR preview"
                            style="width:72px;height:56px;object-fit:cover;border-radius:6px;flex-shrink:0;">
                        <div style="flex:1;min-width:0;">
                            <div id="orcr-filename" style="font-size:13px;font-weight:600;
                                color:var(--text);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"></div>
                            <div id="orcr-filesize" style="font-size:12px;color:var(--muted);margin-top:2px;"></div>
                        </div>
                        <button type="button" onclick="removeOrcr()"
                            style="background:rgba(239,68,68,0.12);border:1px solid rgba(239,68,68,0.3);
                                color:var(--danger);border-radius:6px;padding:6px 12px;font-size:12px;
                                cursor:pointer;flex-shrink:0;">Remove</button>
                    </div>
                    <div id="orcr-error" style="color:var(--danger);font-size:12px;margin-top:6px;display:none;"></div>
                </div>

                <!-- Step 7: Rating — mirrors BookActivity ratingBar (1–5 stars) -->
                <!-- Labels: ["", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🌟"] -->
                <div style="margin-bottom:28px;">
                    <div class="section-title" style="margin-bottom:6px;">
                        <span style="color:var(--yellow);">07</span> &nbsp;Rate Your Experience
                        <small style="color:var(--muted);font-weight:400;margin-left:6px;">(optional)</small>
                    </div>
                    <p style="font-size:13px;color:var(--muted);margin:0 0 16px;line-height:1.6;">
                        How would you rate the service so far? You can always update this after your appointment.
                    </p>

                    <input type="hidden" name="rating" id="ratingInput"
                        value="<?= (int)($old['rating'] ?? 0) ?>">

                    <!-- Star row -->
                    <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                        <?php
                        $savedRating = (int)($old['rating'] ?? 0);
                        $starLabels  = ['', 'Poor 😞', 'Fair 😐', 'Good 🙂', 'Great 😊', 'Excellent 🌟'];
                        for ($s = 1; $s <= 5; $s++):
                            $filled = $savedRating >= $s;
                        ?>
                        <button type="button" id="star-btn-<?= $s ?>" onclick="setRating(<?= $s ?>)"
                            aria-label="<?= $starLabels[$s] ?>"
                            style="background:none;border:none;cursor:pointer;padding:4px;
                                font-size:32px;line-height:1;transition:transform .15s;
                                color:<?= $filled ? 'var(--yellow)' : 'var(--border)' ?>;"
                            onmouseover="hoverRating(<?= $s ?>)"
                            onmouseout="unhoverRating()">★</button>
                        <?php endfor; ?>
                        <span id="ratingLabel" style="font-size:14px;font-weight:600;color:var(--text);margin-left:4px;">
                            <?= $savedRating ? $starLabels[$savedRating] : '' ?>
                        </span>
                    </div>
                    <div id="err-rating" style="color:var(--danger);font-size:12px;margin-top:6px;display:none;"></div>
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

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Concerns</div>
                    <div id="sumConcerns" style="color:var(--text);font-weight:600;margin-top:4px;font-size:13px;line-height:1.7;">—</div>
                </div>

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">OR/CR</div>
                    <div id="sumOrcr" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
                </div>

                <div style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border-sub);">
                    <div style="font-size:13px;color:var(--muted);">Rating</div>
                    <div id="sumRating" style="color:var(--text);font-weight:600;margin-top:4px;">—</div>
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

        // Vehicle concerns — strip emoji+key prefix, show short labels in sidebar
        const checkedConcerns = document.querySelectorAll('input[name="vehicle_concerns[]"]:checked');
        const sumConcernsEl   = document.getElementById('sumConcerns');
        if (checkedConcerns.length === 0) {
            sumConcernsEl.textContent = '—';
        } else {
            // Shorten to just the category name for the sidebar (e.g. "Engine", "Brakes")
            const shortNames = Array.from(checkedConcerns).map(cb => {
                // Value format: "🔧  Engine — knocking..." → grab text before " —"
                const match = cb.value.match(/—\s*(.+)/) ? cb.value : cb.value;
                // Extract just the category word (after emoji, before "—")
                const cat = cb.value.replace(/^[\p{Emoji}\s]+/u, '').split('—')[0].trim();
                return cat;
            });
            sumConcernsEl.textContent = shortNames.join(', ');
        }

        // OR/CR upload status — mirrors orcrStatus "Yes (photo captured)" | "No"
        const orcrInput = document.getElementById('orcr_image');
        const sumOrcrEl = document.getElementById('sumOrcr');
        if (orcrInput && orcrInput.files && orcrInput.files[0]) {
            sumOrcrEl.innerHTML = '<span style="color:#4ade80;">✓ Photo attached</span>';
        } else {
            sumOrcrEl.textContent = 'Not uploaded';
        }

        // Rating — mirrors BookActivity ratingBar labels
        const ratingVal = parseInt(document.getElementById('ratingInput').value || '0', 10);
        const ratingLabels = ['', 'Poor 😞', 'Fair 😐', 'Good 🙂', 'Great 😊', 'Excellent 🌟'];
        const sumRatingEl = document.getElementById('sumRating');
        if (ratingVal > 0) {
            sumRatingEl.innerHTML =
                '<span style="color:var(--yellow);">' + '★'.repeat(ratingVal) +
                '<span style="color:var(--border);">' + '★'.repeat(5 - ratingVal) + '</span></span>' +
                ' <span style="font-size:12px;color:var(--muted);">' + ratingLabels[ratingVal] + '</span>';
        } else {
            sumRatingEl.textContent = 'Not rated';
        }
    }

    // ── Rating helpers — mirrors BookActivity ratingBar.setOnRatingBarChangeListener ─
    // Labels: ["", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🌟"]
    const RATING_LABELS = ['', 'Poor 😞', 'Fair 😐', 'Good 🙂', 'Great 😊', 'Excellent 🌟'];

    window.setRating = function(val) {
        document.getElementById('ratingInput').value = val;
        paintStars(val);
        document.getElementById('ratingLabel').textContent = RATING_LABELS[val] || '';
        updateSummary();
    };

    window.hoverRating = function(val) {
        paintStars(val, true);
    };

    window.unhoverRating = function() {
        const cur = parseInt(document.getElementById('ratingInput').value || '0', 10);
        paintStars(cur);
    };

    function paintStars(val, hover) {
        for (let i = 1; i <= 5; i++) {
            const btn = document.getElementById('star-btn-' + i);
            if (!btn) continue;
            btn.style.color = i <= val ? 'var(--yellow)' : 'var(--border)';
            btn.style.transform = (hover && i === val) ? 'scale(1.2)' : 'scale(1)';
        }
    }

    // Restore star state from server-reflected value (error redirect)
    (function () {
        const saved = parseInt(document.getElementById('ratingInput').value || '0', 10);
        if (saved > 0) paintStars(saved);
    })();

    // ── End rating helpers ────────────────────────────────────────────────────

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

    // Concern chip visual toggle + live summary card
    // Mirrors BookActivity setupCheckBoxes() / updateConcernSummary()
    window.toggleConcern = function(cb, key) {
        const lbl = document.getElementById('concern-label-' + key);
        if (lbl) {
            lbl.style.borderColor = cb.checked ? 'var(--yellow)' : 'var(--border)';
            lbl.style.background  = cb.checked ? 'rgba(251,189,35,0.07)' : 'transparent';
        }

        // Rebuild the live summary card beneath the checklist
        const checked = Array.from(document.querySelectorAll('input[name="vehicle_concerns[]"]:checked'));
        const summaryDiv  = document.getElementById('concern-summary');
        const summaryList = document.getElementById('concern-summary-list');

        if (checked.length === 0) {
            summaryDiv.style.display = 'none';
        } else {
            summaryDiv.style.display = 'block';
            summaryList.innerHTML = checked
                .map(c => '<li>' + c.value + '</li>')
                .join('');
        }

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
    document.querySelectorAll('input[name="vehicle_concerns[]"]').forEach(el => el.addEventListener('change', updateSummary));
    document.getElementById('ratingInput').addEventListener('change', updateSummary);

    // Restore chip styles if server reflected an old value back after error
    const preselectedFuel = document.querySelector('input[name="fuel_type"]:checked');
    if (preselectedFuel) selectFuel(preselectedFuel.value.toLowerCase());

    // Restore concern chip styles for any server-reflected checked boxes
    document.querySelectorAll('input[name="vehicle_concerns[]"]:checked').forEach(cb => {
        const key = cb.id.replace('concern-', '');
        toggleConcern(cb, key);
    });

    // ── OR/CR upload helpers ──────────────────────────────────────────────────
    // Mirrors Appointment.orcrStatus: "Yes (photo captured)" | "No"
    window.previewOrcr = function(input) {
        const file = input.files[0];
        const errEl = document.getElementById('orcr-error');
        if (!file) return;

        // Validate type
        const allowed = ['image/jpeg','image/png','image/webp'];
        if (!allowed.includes(file.type)) {
            errEl.textContent = 'Invalid file type. Please upload a JPG, PNG, or WEBP image.';
            errEl.style.display = 'block';
            input.value = '';
            return;
        }
        // Validate size (5 MB)
        if (file.size > 5 * 1024 * 1024) {
            errEl.textContent = 'File is too large (max 5 MB).';
            errEl.style.display = 'block';
            input.value = '';
            return;
        }
        errEl.style.display = 'none';

        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('orcr-thumb').src     = e.target.result;
            document.getElementById('orcr-filename').textContent = file.name;
            document.getElementById('orcr-filesize').textContent =
                (file.size / 1024 < 1024)
                    ? (file.size / 1024).toFixed(1) + ' KB'
                    : (file.size / 1048576).toFixed(2) + ' MB';

            const dropzone = document.getElementById('orcr-dropzone');
            const preview  = document.getElementById('orcr-preview');
            dropzone.style.display = 'none';
            preview.style.display  = 'flex';
            updateSummary();
        };
        reader.readAsDataURL(file);
    };

    window.handleOrcrDrop = function(e) {
        e.preventDefault();
        const dropzone = document.getElementById('orcr-dropzone');
        dropzone.style.borderColor = 'var(--border)';
        const dt = e.dataTransfer;
        if (dt.files && dt.files[0]) {
            const input = document.getElementById('orcr_image');
            // DataTransfer is read-only; assign via new DataTransfer object
            try {
                const dtt = new DataTransfer();
                dtt.items.add(dt.files[0]);
                input.files = dtt.files;
            } catch(_) {}
            previewOrcr(input);
        }
    };

    window.removeOrcr = function() {
        const input = document.getElementById('orcr_image');
        input.value = '';
        document.getElementById('orcr-thumb').src = '';
        document.getElementById('orcr-preview').style.display = 'none';
        document.getElementById('orcr-dropzone').style.display = '';
        updateSummary();
    };

    // ── End OR/CR helpers ─────────────────────────────────────────────────────

    // Disable Sundays on date input (HTML min/max don't block DOW; we use the change handler + warn)
    updateSummary();
})();
</script>

<?php require 'partials/footer.php'; ?>
