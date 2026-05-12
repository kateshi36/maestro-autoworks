<?php
// Admin Services Management — admin_services.php
session_start();
require 'db.php';
$pageTitle = 'Manage Services — Maestro Autoworks';
require 'partials/header.php';

if ($me['role'] !== 'admin') { header('Location: dashboard.php'); exit; }

// Handle toggle availability
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['toggle_id'])) {
    $toggleId = (int)$_POST['toggle_id'];
    $newActive = (int)$_POST['new_active'];
    $stmt = $pdo->prepare("UPDATE services SET active = ? WHERE id = ?");
    $stmt->execute([$newActive, $toggleId]);
    $_SESSION['flash'] = [
        'type' => 'success',
        'msg'  => $newActive ? 'Service marked as available.' : 'Service marked as unavailable.'
    ];
    header('Location: admin_services.php');
    exit;
}

// Handle add service
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add') {
    $name        = trim($_POST['name'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $category    = trim($_POST['category'] ?? '');
    $price       = (float)($_POST['price'] ?? 0);
    $duration    = (float)($_POST['duration_hr'] ?? 1);
    $active      = isset($_POST['active']) ? 1 : 0;

    if ($name && $category && $price > 0) {
        $stmt = $pdo->prepare("INSERT INTO services (name, description, category, price, duration_hr, active) VALUES (?,?,?,?,?,?)");
        $stmt->execute([$name, $description, $category, $price, $duration, $active]);
        $_SESSION['flash'] = ['type' => 'success', 'msg' => "Service \"$name\" added successfully."];
    } else {
        $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Please fill in all required fields.'];
    }
    header('Location: admin_services.php');
    exit;
}

// Handle edit service
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'edit') {
    $id          = (int)$_POST['edit_id'];
    $name        = trim($_POST['name'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $category    = trim($_POST['category'] ?? '');
    $price       = (float)($_POST['price'] ?? 0);
    $duration    = (float)($_POST['duration_hr'] ?? 1);

    if ($id && $name && $category && $price > 0) {
        $stmt = $pdo->prepare("UPDATE services SET name=?, description=?, category=?, price=?, duration_hr=? WHERE id=?");
        $stmt->execute([$name, $description, $category, $price, $duration, $id]);
        $_SESSION['flash'] = ['type' => 'success', 'msg' => "Service updated successfully."];
    } else {
        $_SESSION['flash'] = ['type' => 'error', 'msg' => 'Please fill in all required fields.'];
    }
    header('Location: admin_services.php');
    exit;
}

// Fetch all services (active and inactive)
$services   = $pdo->query("SELECT * FROM services ORDER BY category, name")->fetchAll();
$categories = array_unique(array_column($services, 'category'));

$flash = $_SESSION['flash'] ?? null;
unset($_SESSION['flash']);
?>

<main class="page-shell">

    <?php if ($flash): ?>
        <div class="alert alert-<?= $flash['type'] ?>" data-auto-close>
            <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
            <?= htmlspecialchars($flash['msg']) ?>
        </div>
    <?php endif; ?>

    <div class="page-header" style="display:flex;align-items:flex-end;justify-content:space-between;flex-wrap:wrap;gap:16px;">
        <div>
            <div class="page-label">Admin · Services</div>
            <div class="page-title">Manage Services</div>
            <div class="page-sub">Control which services are available to customers for booking.</div>
        </div>
        <button class="btn btn-primary" onclick="openAddModal()">
            <svg viewBox="0 0 24 24"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/></svg>
            Add New Service
        </button>
    </div>

    <!-- Stats strip -->
    <div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:28px;">
        <?php
        $total     = count($services);
        $available = count(array_filter($services, fn($s) => $s['active']));
        $unavail   = $total - $available;
        ?>
        <div class="stat-tile" style="flex:1;min-width:140px;">
            <div class="stat-icon" style="background:#4CAF7D22;">
                <svg viewBox="0 0 24 24" style="fill:#4CAF7D;"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
            </div>
            <div class="stat-val"><?= $available ?></div>
            <div class="stat-label">Available</div>
        </div>
        <div class="stat-tile" style="flex:1;min-width:140px;">
            <div class="stat-icon" style="background:#E0525222;">
                <svg viewBox="0 0 24 24" style="fill:#E05252;"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/></svg>
            </div>
            <div class="stat-val"><?= $unavail ?></div>
            <div class="stat-label">Unavailable</div>
        </div>
        <div class="stat-tile" style="flex:1;min-width:140px;">
            <div class="stat-icon" style="background:#7B61FF22;">
                <svg viewBox="0 0 24 24" style="fill:#7B61FF;"><path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/></svg>
            </div>
            <div class="stat-val"><?= $total ?></div>
            <div class="stat-label">Total Services</div>
        </div>
    </div>

    <!-- Services Table -->
    <div class="card" style="padding:0;">
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Service</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Duration</th>
                        <th style="text-align:center;">Status</th>
                        <th style="text-align:center;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                <?php foreach ($services as $svc): ?>
                    <tr style="<?= !$svc['active'] ? 'opacity:0.6;' : '' ?>">
                        <td>
                            <strong><?= htmlspecialchars($svc['name']) ?></strong>
                            <?php if ($svc['description']): ?>
                                <br><span style="font-size:12px;color:var(--muted);"><?= htmlspecialchars(mb_strimwidth($svc['description'], 0, 70, '…')) ?></span>
                            <?php endif; ?>
                        </td>
                        <td>
                            <span style="background:var(--navy-2);padding:3px 10px;border-radius:20px;font-size:12px;font-weight:600;">
                                <?= htmlspecialchars($svc['category']) ?>
                            </span>
                        </td>
                        <td style="font-weight:700;">₱<?= number_format($svc['price'], 2) ?></td>
                        <td><?= number_format($svc['duration_hr'], 1) ?> hr<?= $svc['duration_hr'] != 1 ? 's' : '' ?></td>
                        <td style="text-align:center;">
                            <?php if ($svc['active']): ?>
                                <span class="badge badge-confirmed">
                                    <span class="badge-dot"></span>Available
                                </span>
                            <?php else: ?>
                                <span class="badge badge-declined">
                                    <span class="badge-dot"></span>Unavailable
                                </span>
                            <?php endif; ?>
                        </td>
                        <td>
                            <div style="display:flex;gap:6px;justify-content:center;flex-wrap:wrap;">
                                <!-- Edit -->
                                <button class="btn btn-secondary btn-sm"
                                    onclick="openEditModal(<?= htmlspecialchars(json_encode($svc), ENT_QUOTES) ?>)">
                                    <svg viewBox="0 0 24 24" style="width:13px;height:13px;fill:currentColor;"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                                    Edit
                                </button>
                                <!-- Toggle availability -->
                                <form method="POST" style="display:inline;">
                                    <input type="hidden" name="toggle_id"   value="<?= $svc['id'] ?>">
                                    <input type="hidden" name="new_active"  value="<?= $svc['active'] ? 0 : 1 ?>">
                                    <?php if ($svc['active']): ?>
                                        <button type="submit" class="btn btn-danger btn-sm"
                                            onclick="return confirm('Mark this service as unavailable? Customers will see it as not available.')">
                                            <svg viewBox="0 0 24 24" style="width:13px;height:13px;fill:currentColor;"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/></svg>
                                            Set Unavailable
                                        </button>
                                    <?php else: ?>
                                        <button type="submit" class="btn btn-success btn-sm">
                                            <svg viewBox="0 0 24 24" style="width:13px;height:13px;fill:currentColor;"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                                            Set Available
                                        </button>
                                    <?php endif; ?>
                                </form>
                            </div>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>

</main>

<!-- ADD SERVICE MODAL -->
<div class="modal-overlay" id="addModal">
    <div class="modal" style="max-width:540px;">
        <div class="modal-head">
            <div class="modal-title">Add New Service</div>
            <button class="modal-close" onclick="closeModal('addModal')">
                <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
            </button>
        </div>
        <form method="POST">
            <input type="hidden" name="action" value="add">
            <div class="form-group">
                <label>Service Name <span style="color:var(--yellow)">*</span></label>
                <input type="text" name="name" required placeholder="e.g. Engine Diagnostic">
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
                <div class="form-group">
                    <label>Category <span style="color:var(--yellow)">*</span></label>
                    <input type="text" name="category" required placeholder="e.g. Maintenance"
                        list="cat-list">
                    <datalist id="cat-list">
                        <?php foreach ($categories as $cat): ?>
                            <option value="<?= htmlspecialchars($cat) ?>">
                        <?php endforeach; ?>
                    </datalist>
                </div>
                <div class="form-group">
                    <label>Price (₱) <span style="color:var(--yellow)">*</span></label>
                    <input type="number" name="price" required min="1" step="0.01" placeholder="0.00">
                </div>
            </div>
            <div class="form-group">
                <label>Duration (hours)</label>
                <input type="number" name="duration_hr" value="1" min="0.5" step="0.5">
            </div>
            <div class="form-group">
                <label>Description</label>
                <textarea name="description" placeholder="Brief description of the service…" style="min-height:80px;"></textarea>
            </div>
            <div class="form-group" style="display:flex;align-items:center;gap:10px;">
                <input type="checkbox" name="active" id="add_active" checked style="width:auto;accent-color:var(--yellow);">
                <label for="add_active" style="margin:0;cursor:pointer;">Available immediately</label>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeModal('addModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Add Service</button>
            </div>
        </form>
    </div>
</div>

<!-- EDIT SERVICE MODAL -->
<div class="modal-overlay" id="editModal">
    <div class="modal" style="max-width:540px;">
        <div class="modal-head">
            <div class="modal-title">Edit Service</div>
            <button class="modal-close" onclick="closeModal('editModal')">
                <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
            </button>
        </div>
        <form method="POST" id="editForm">
            <input type="hidden" name="action"  value="edit">
            <input type="hidden" name="edit_id" id="edit_id">
            <div class="form-group">
                <label>Service Name <span style="color:var(--yellow)">*</span></label>
                <input type="text" name="name" id="edit_name" required>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
                <div class="form-group">
                    <label>Category <span style="color:var(--yellow)">*</span></label>
                    <input type="text" name="category" id="edit_category" required list="cat-list">
                </div>
                <div class="form-group">
                    <label>Price (₱) <span style="color:var(--yellow)">*</span></label>
                    <input type="number" name="price" id="edit_price" required min="1" step="0.01">
                </div>
            </div>
            <div class="form-group">
                <label>Duration (hours)</label>
                <input type="number" name="duration_hr" id="edit_duration" min="0.5" step="0.5">
            </div>
            <div class="form-group">
                <label>Description</label>
                <textarea name="description" id="edit_description" style="min-height:80px;"></textarea>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Changes</button>
            </div>
        </form>
    </div>
</div>

<script>
function openAddModal() {
    document.getElementById('addModal').classList.add('open');
}

function openEditModal(svc) {
    document.getElementById('edit_id').value          = svc.id;
    document.getElementById('edit_name').value        = svc.name;
    document.getElementById('edit_category').value    = svc.category;
    document.getElementById('edit_price').value       = svc.price;
    document.getElementById('edit_duration').value    = svc.duration_hr;
    document.getElementById('edit_description').value = svc.description || '';
    document.getElementById('editModal').classList.add('open');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('open');
}

// Close on backdrop click
['addModal','editModal'].forEach(id => {
    document.getElementById(id).addEventListener('click', function(e) {
        if (e.target === this) this.classList.remove('open');
    });
});

// Auto-close flash alerts
document.querySelectorAll('[data-auto-close]').forEach(el => {
    setTimeout(() => el.style.display = 'none', 4000);
});
</script>

<?php require 'partials/footer.php'; ?>
