<?php
// PMS: Preventive Maintenance System — admin_pms.php
session_start();
require 'db.php';
$pageTitle = 'Repair Tracker — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

// ── Flash messages ────────────────────────────────────────────────────────
$flash = $_SESSION['flash'] ?? null;
unset($_SESSION['flash']);

// ── Active confirmed jobs on the shop floor ───────────────────────────────
$jobs = $pdo->query("
    SELECT  a.*,
            u.first_name, u.last_name, u.email,
            s.name        AS service_name,
            s.duration_hr,
            s.category
    FROM    appointments a
    JOIN    users    u ON u.id = a.user_id
    JOIN    services s ON s.id = a.service_id
    WHERE   a.status = 'confirmed'
    ORDER   BY a.appt_date ASC, a.appt_time ASC
")->fetchAll();

// ── Fetch tasks for every active job in one query ─────────────────────────
$tasksByAppt = [];
if (!empty($jobs)) {
    $apptIds  = array_column($jobs, 'id');
    $in       = implode(',', array_fill(0, count($apptIds), '?'));
    $taskStmt = $pdo->prepare("
        SELECT * FROM repair_tasks
        WHERE  appt_id IN ($in)
        ORDER  BY appt_id, sort_order, id
    ");
    $taskStmt->execute($apptIds);
    foreach ($taskStmt->fetchAll() as $t) {
        $tasksByAppt[$t['appt_id']][] = $t;
    }
}

// ── Shop capacity & summary stats ─────────────────────────────────────────
$CAPACITY    = 5;
$activeCount = count($jobs);
$slotsLeft   = max(0, $CAPACITY - $activeCount);

$jobsDone   = 0;
$jobsActive = 0;
$jobsQueued = 0;
foreach ($jobs as $j) {
    $tasks = $tasksByAppt[$j['id']] ?? [];
    if (empty($tasks)) { $jobsQueued++; continue; }
    $done = count(array_filter($tasks, fn($t) => $t['status'] === 'completed'));
    if ($done === count($tasks)) $jobsDone++;
    elseif ($done > 0)          $jobsActive++;
    else                        $jobsQueued++;
}
?>

<style>
/* ── PMS dark-mode overrides ─────────────────────────────────────────────── */
.pms-input {
    width: 100%;
    padding: 9px 13px;
    background: var(--black-input);
    border: 1px solid var(--border-sub);
    border-radius: 8px;
    font-size: 14px;
    color: var(--text);
    font-family: 'Barlow', sans-serif;
    transition: border-color .2s;
}
.pms-input:focus  { outline: none; border-color: var(--yellow); }
.pms-input::placeholder { color: var(--muted); }

.pms-select {
    padding: 7px 10px;
    font-size: 13px;
    border: 1px solid var(--border-sub);
    border-radius: 7px;
    background: var(--black-input);
    color: var(--text);
    cursor: pointer;
    font-family: 'Barlow', sans-serif;
    transition: border-color .2s;
}
.pms-select:focus { outline: none; border-color: var(--yellow); }

.pms-label {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: .5px;
    text-transform: uppercase;
    display: block;
    margin-bottom: 6px;
    color: var(--muted);
}

.job-card {
    background: var(--black-card);
    border: 1px solid var(--border-sub);
    border-radius: 14px;
    overflow: hidden;
    transition: border-color .2s;
}
.job-card:hover { border-color: rgba(245,166,35,0.2); }

.job-header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--border-sub);
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
}

.job-meta {
    font-size: 13px;
    color: var(--muted);
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
}

.pms-badge {
    display: inline-flex;
    align-items: center;
    padding: 3px 11px;
    border-radius: 99px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: .4px;
    text-transform: uppercase;
}

.progress-bar-wrap {
    padding: 11px 24px;
    background: rgba(255,255,255,0.025);
    border-bottom: 1px solid var(--border-sub);
    display: flex;
    align-items: center;
    gap: 14px;
}

.progress-track {
    flex: 1;
    background: rgba(255,255,255,0.08);
    border-radius: 99px;
    height: 7px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    border-radius: 99px;
    transition: width .5s ease;
}

.add-task-panel {
    display: none;
    padding: 20px 24px;
    background: rgba(255,255,255,0.02);
    border-top: 1px solid var(--border-sub);
}
.add-task-panel.open { display: block; }

.capacity-bay {
    width: 42px; height: 42px;
    border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    font-size: 13px; font-weight: 800;
    transition: transform .15s;
}
.capacity-bay.filled {
    background: var(--yellow); color: var(--black);
    border: 2px solid var(--yellow);
}
.capacity-bay.empty {
    background: rgba(255,255,255,0.04); color: var(--muted);
    border: 2px dashed rgba(255,255,255,0.12);
}

.stat-pill {
    display: flex; align-items: center; gap: 10px;
    padding: 10px 18px; border-radius: 10px;
    background: rgba(255,255,255,0.04);
    border: 1px solid var(--border-sub);
}
</style>

<main class="page-shell">

<?php if ($flash): ?>
    <div class="alert alert-<?= $flash['type'] ?>" data-auto-close style="margin-bottom:24px;">
        <svg viewBox="0 0 24 24">
            <?php if ($flash['type'] === 'success'): ?>
                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            <?php else: ?>
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
            <?php endif; ?>
        </svg>
        <?= htmlspecialchars($flash['msg']) ?>
    </div>
<?php endif; ?>

<!-- ── Page Header ─────────────────────────────────────────────────── -->
<div class="page-header" style="display:flex;align-items:flex-end;
     justify-content:space-between;flex-wrap:wrap;gap:16px;margin-bottom:28px;">
    <div>
        <div class="page-label">PMS · Shop Floor</div>
        <div class="page-title">Repair Tracker</div>
        <div class="page-sub"><?= date('l, F j, Y') ?> &nbsp;·&nbsp; Live job board</div>
    </div>
    <a href="admin_dashboard.php" class="btn btn-secondary">
        <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        Back to Dashboard
    </a>
</div>

<!-- ── Capacity + Summary Row ─────────────────────────────────────── -->
<div class="card" style="margin-bottom:28px;padding:22px 26px;">
    <div style="display:flex;gap:32px;flex-wrap:wrap;align-items:center;">

        <div style="flex:1;min-width:220px;">
            <div style="font-family:'Barlow Condensed',sans-serif;font-size:11px;
                        font-weight:700;letter-spacing:2px;text-transform:uppercase;
                        color:var(--muted);margin-bottom:12px;">Shop Bay Capacity</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;">
                <?php for ($i = 1; $i <= $CAPACITY; $i++): ?>
                    <div class="capacity-bay <?= $i <= $activeCount ? 'filled' : 'empty' ?>"><?= $i ?></div>
                <?php endfor; ?>
            </div>
            <div style="margin-top:10px;font-size:13px;color:var(--muted);">
                <span style="color:<?= $slotsLeft > 0 ? 'var(--success)' : 'var(--danger)' ?>;
                             font-weight:700;font-size:16px;"><?= $slotsLeft ?></span>
                &nbsp;bay<?= $slotsLeft !== 1 ? 's' : '' ?> available
                &nbsp;·&nbsp; <?= $activeCount ?> vehicle<?= $activeCount !== 1 ? 's' : '' ?> in shop
            </div>
        </div>

        <div style="width:1px;background:var(--border-sub);align-self:stretch;"></div>

        <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;">
            <div class="stat-pill">
                <div style="width:8px;height:8px;border-radius:50%;background:#9ca3af;"></div>
                <div>
                    <div style="font-size:22px;font-weight:800;color:var(--white);line-height:1;"><?= $jobsQueued ?></div>
                    <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;">Queued</div>
                </div>
            </div>
            <div class="stat-pill">
                <div style="width:8px;height:8px;border-radius:50%;background:#2196F3;"></div>
                <div>
                    <div style="font-size:22px;font-weight:800;color:var(--white);line-height:1;"><?= $jobsActive ?></div>
                    <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;">In Progress</div>
                </div>
            </div>
            <div class="stat-pill">
                <div style="width:8px;height:8px;border-radius:50%;background:var(--success);"></div>
                <div>
                    <div style="font-size:22px;font-weight:800;color:var(--white);line-height:1;"><?= $jobsDone ?></div>
                    <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;">All Done</div>
                </div>
            </div>
        </div>

    </div>
</div>

<!-- ── Job Cards ──────────────────────────────────────────────────── -->
<?php if (empty($jobs)): ?>
    <div class="card" style="text-align:center;padding:72px 24px;">
        <svg viewBox="0 0 24 24" style="width:52px;height:52px;fill:var(--muted);
             opacity:.35;display:block;margin:0 auto 20px;">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
        </svg>
        <div style="font-size:22px;font-weight:800;color:var(--white);
                    font-family:'Barlow Condensed',sans-serif;margin-bottom:8px;">
            No active jobs on the floor
        </div>
        <div style="font-size:14px;color:var(--muted);max-width:360px;margin:0 auto 24px;">
            Confirmed appointments will appear here once they are ready for the shop floor.
        </div>
        <a href="admin_appointments.php?status=pending"
           class="btn btn-secondary" style="display:inline-flex;">
            View Pending Appointments
        </a>
    </div>

<?php else: ?>
    <div style="display:grid;gap:20px;">
    <?php foreach ($jobs as $job):
        $tasks   = $tasksByAppt[$job['id']] ?? [];
        $total   = count($tasks);
        $done    = count(array_filter($tasks, fn($t) => $t['status'] === 'completed'));
        $pct     = $total > 0 ? round($done / $total * 100) : 0;
        $allDone = $total > 0 && $done === $total;

        [$jColor, $jBg, $jLabel] = match(true) {
            $total === 0 => ['#9ca3af', 'rgba(156,163,175,0.1)', 'No Tasks'],
            $allDone     => ['var(--success)', 'rgba(76,175,125,0.12)', 'All Done'],
            $done > 0    => ['#2196F3', 'rgba(33,150,243,0.12)', 'In Progress'],
            default      => ['var(--yellow)', 'var(--yellow-lt)', 'Not Started'],
        };

        $catColor = match($job['category'] ?? '') {
            'Maintenance' => 'var(--yellow)',
            'Brakes'      => 'var(--danger)',
            'Tires'       => '#7B61FF',
            'Electrical'  => '#4A90D9',
            'Diagnostics' => '#F5A623',
            'Comfort'     => 'var(--success)',
            'Drivetrain'  => '#E57373',
            'Suspension'  => '#26C6DA',
            default       => 'var(--muted)',
        };
    ?>
        <div class="job-card">

            <!-- Category colour strip -->
            <div style="height:3px;background:<?= $catColor ?>;opacity:.75;"></div>

            <!-- Job Header -->
            <div class="job-header">
                <div>
                    <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:6px;">
                        <svg viewBox="0 0 24 24" style="width:17px;height:17px;fill:<?= $catColor ?>;flex-shrink:0;">
                            <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
                        </svg>
                        <span style="font-weight:800;font-size:17px;color:var(--white);
                                     font-family:'Barlow Condensed',sans-serif;letter-spacing:.3px;">
                            <?= htmlspecialchars($job['vehicle_year'] . ' ' . $job['vehicle_make'] . ' ' . $job['vehicle_model']) ?>
                        </span>
                        <span style="font-size:11px;padding:3px 10px;border-radius:99px;
                            font-weight:700;background:rgba(255,255,255,0.07);
                            color:var(--muted);letter-spacing:.5px;">
                            <?= htmlspecialchars($job['plate_no']) ?>
                        </span>
                        <span class="pms-badge" style="color:<?= $jColor ?>;background:<?= $jBg ?>;">
                            <?= $jLabel ?>
                        </span>
                    </div>

                    <div class="job-meta">
                        <strong style="color:var(--text);"><?= htmlspecialchars($job['service_name']) ?></strong>
                        <span style="color:var(--border-sub);">|</span>
                        <?= htmlspecialchars($job['first_name'] . ' ' . $job['last_name']) ?>
                        <span style="font-size:12px;">&lt;<?= htmlspecialchars($job['email']) ?>&gt;</span>
                        <span style="color:var(--border-sub);">|</span>
                        <?= date('M j, Y', strtotime($job['appt_date'])) ?>
                        at <?= date('g:i A', strtotime($job['appt_time'])) ?>
                        <span style="color:var(--border-sub);">|</span>
                        Est. <?= $job['duration_hr'] ?> hr<?= $job['duration_hr'] != 1 ? 's' : '' ?>
                    </div>
                </div>

                <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                    <?php if ($allDone): ?>
                        <form method="POST" action="pms_complete_job.php" style="display:inline;">
                            <input type="hidden" name="appt_id" value="<?= $job['id'] ?>">
                            <button class="btn btn-success btn-sm"
                                onclick="return confirm('Mark this vehicle as complete and notify the customer?')">
                                <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:currentColor;">
                                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                                </svg>
                                Mark Job Complete
                            </button>
                        </form>
                    <?php endif; ?>
                    <button class="btn btn-secondary btn-sm"
                            id="add-btn-<?= $job['id'] ?>"
                            onclick="toggleAddTask(<?= $job['id'] ?>)">
                        <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:currentColor;">
                            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                        </svg>
                        Add Task
                    </button>
                </div>
            </div>

            <!-- Progress Bar -->
            <?php if ($total > 0): ?>
            <div class="progress-bar-wrap">
                <div class="progress-track">
                    <div class="progress-fill"
                         style="width:<?= $pct ?>%;
                                background:<?= $pct === 100 ? 'var(--success)' : '#2196F3' ?>;"></div>
                </div>
                <div style="font-size:12px;font-weight:700;color:var(--muted);white-space:nowrap;">
                    <?= $done ?> / <?= $total ?> tasks &nbsp;·&nbsp;
                    <span style="color:<?= $pct === 100 ? 'var(--success)' : 'var(--text)' ?>;"><?= $pct ?>%</span>
                </div>
            </div>
            <?php endif; ?>

            <!-- Task Table -->
            <?php if (!empty($tasks)): ?>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Task</th>
                        <th>Assigned To</th>
                        <th>Status</th>
                        <th>Started</th>
                        <th>Completed</th>
                        <th style="min-width:185px;">Update</th>
                    </tr></thead>
                    <tbody>
                    <?php foreach ($tasks as $t):
                        [$sc, $sb] = match($t['status']) {
                            'pending'     => ['var(--yellow)',   'var(--yellow-lt)'],
                            'in_progress' => ['#2196F3',         'rgba(33,150,243,0.12)'],
                            'testing'     => ['#7B61FF',         'rgba(123,97,255,0.12)'],
                            'completed'   => ['var(--success)',  'var(--success-lt)'],
                            default       => ['var(--muted)',    'rgba(122,142,168,0.1)'],
                        };
                        $statusLabel = ucwords(str_replace('_', ' ', $t['status']));
                    ?>
                        <tr style="<?= $t['status'] === 'completed' ? 'opacity:.5;' : '' ?>">
                            <td>
                                <span style="font-weight:600;
                                    <?= $t['status'] === 'completed'
                                        ? 'text-decoration:line-through;color:var(--muted);'
                                        : 'color:var(--text);' ?>">
                                    <?= htmlspecialchars($t['task_name']) ?>
                                </span>
                                <?php if ($t['notes']): ?>
                                    <div style="font-size:12px;color:var(--muted);margin-top:3px;">
                                        <?= htmlspecialchars($t['notes']) ?>
                                    </div>
                                <?php endif; ?>
                            </td>
                            <td style="font-size:13px;">
                                <?php if ($t['assigned_to']): ?>
                                    <span style="color:var(--text);">
                                        <?= htmlspecialchars($t['assigned_to']) ?>
                                    </span>
                                <?php else: ?>
                                    <span style="color:var(--muted);font-size:12px;font-style:italic;">Unassigned</span>
                                <?php endif; ?>
                            </td>
                            <td>
                                <span class="pms-badge"
                                      style="color:<?= $sc ?>;background:<?= $sb ?>;">
                                    <?= $statusLabel ?>
                                </span>
                            </td>
                            <td style="font-size:12px;color:var(--muted);">
                                <?= $t['started_at']
                                    ? date('M j, g:i A', strtotime($t['started_at']))
                                    : '<span style="color:rgba(255,255,255,0.15);">—</span>' ?>
                            </td>
                            <td style="font-size:12px;color:var(--muted);">
                                <?= $t['completed_at']
                                    ? date('M j, g:i A', strtotime($t['completed_at']))
                                    : '<span style="color:rgba(255,255,255,0.15);">—</span>' ?>
                            </td>
                            <td>
                                <?php if ($t['status'] !== 'completed'): ?>
                                <form method="POST" action="pms_update_task.php"
                                      style="display:inline-flex;gap:6px;align-items:center;">
                                    <input type="hidden" name="task_id"  value="<?= $t['id'] ?>">
                                    <input type="hidden" name="appt_id"  value="<?= $job['id'] ?>">
                                    <select name="status" class="pms-select">
                                        <?php foreach (['pending','in_progress','testing','completed'] as $s): ?>
                                            <option value="<?= $s ?>"
                                                <?= $t['status'] === $s ? 'selected' : '' ?>>
                                                <?= ucwords(str_replace('_', ' ', $s)) ?>
                                            </option>
                                        <?php endforeach; ?>
                                    </select>
                                    <button class="btn btn-secondary btn-sm">Save</button>
                                </form>
                                <?php else: ?>
                                    <span style="font-size:12px;color:var(--success);font-weight:700;
                                                 display:inline-flex;align-items:center;gap:4px;">
                                        <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:currentColor;">
                                            <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                                        </svg>
                                        Done
                                    </span>
                                <?php endif; ?>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            </div>

            <?php else: ?>
                <div style="padding:28px 24px;text-align:center;
                            background:rgba(255,255,255,0.015);">
                    <div style="font-size:14px;color:var(--muted);">
                        No tasks yet. Click
                        <strong style="color:var(--text);">+ Add Task</strong>
                        to break this job into steps.
                    </div>
                </div>
            <?php endif; ?>

            <!-- Add Task Panel -->
            <div id="add-task-<?= $job['id'] ?>" class="add-task-panel">
                <div style="font-size:12px;font-weight:700;letter-spacing:1.5px;
                            text-transform:uppercase;color:var(--yellow);margin-bottom:14px;">
                    New Task
                </div>
                <form method="POST" action="pms_add_task.php"
                      style="display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end;">
                    <input type="hidden" name="appt_id" value="<?= $job['id'] ?>">

                    <div style="flex:2;min-width:180px;">
                        <label class="pms-label">
                            Task Name <span style="color:var(--danger);">*</span>
                        </label>
                        <input type="text" name="task_name" class="pms-input"
                               placeholder="e.g. Drain old oil, Inspect brake pads…"
                               required>
                    </div>
                    <div style="flex:1;min-width:140px;">
                        <label class="pms-label">Assigned To</label>
                        <input type="text" name="assigned_to" class="pms-input"
                               placeholder="Mechanic name">
                    </div>
                    <div style="flex:2;min-width:180px;">
                        <label class="pms-label">Notes</label>
                        <input type="text" name="notes" class="pms-input"
                               placeholder="Any specific instructions…">
                    </div>
                    <div style="display:flex;gap:8px;">
                        <button type="submit" class="btn btn-primary btn-sm">
                            <svg viewBox="0 0 24 24" style="width:14px;height:14px;fill:currentColor;">
                                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                            </svg>
                            Add Task
                        </button>
                        <button type="button" class="btn btn-secondary btn-sm"
                                onclick="toggleAddTask(<?= $job['id'] ?>)">Cancel</button>
                    </div>
                </form>
            </div>

        </div><!-- /.job-card -->
    <?php endforeach; ?>
    </div>
<?php endif; ?>

</main>

<script>
function toggleAddTask(apptId) {
    const panel = document.getElementById('add-task-' + apptId);
    const btn   = document.getElementById('add-btn-'  + apptId);
    if (!panel) return;
    const isOpen = panel.classList.toggle('open');
    if (btn) btn.style.background = isOpen ? 'rgba(245,166,35,0.1)' : '';
    if (isOpen) panel.querySelector('input[name="task_name"]')?.focus();
}

// Auto-dismiss flash after 4 s
document.querySelectorAll('[data-auto-close]').forEach(el => {
    setTimeout(() => {
        el.style.transition = 'opacity .4s';
        el.style.opacity    = '0';
        setTimeout(() => el.remove(), 400);
    }, 4000);
});
</script>

<?php require 'partials/footer.php'; ?>
