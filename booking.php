<?php
session_start();
if (!isset($_SESSION['user_id'])) {
    header('Location: login.php');
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Book Service — Maestro Autoworks</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .booking-container { max-width: 600px; margin: 80px auto; padding: 20px; }
        .booking-card { background: var(--navy-card); border: 1px solid var(--border); padding: 40px; border-radius: 12px; }
        select, input[type="date"] {
            width: 100%;
            background: var(--navy-input);
            border: 1.5px solid rgba(255,255,255,0.08);
            border-radius: 8px;
            padding: 13px;
            color: var(--text);
            margin-bottom: 20px;
            outline: none;
        }
        select:focus, input:focus { border-color: var(--yellow); }
    </style>
</head>
<body>

<div class="top-bar">
    <a href="index.php" class="logo">Maestro Autoworks</a>
    <a href="dashboard.php" class="back-link">Back to Dashboard</a>
</div>

<div class="booking-container">
    <div class="booking-card">
        <h2 style="font-family: 'Barlow Condensed'; font-size: 2rem; color: var(--yellow); margin-bottom: 20px;">Book an Appointment</h2>
        
        <?php if(isset($_SESSION['booking_msg'])): ?>
            <div class="status-msg" style="color: var(--success); margin-bottom: 15px;">
                <?php echo $_SESSION['booking_msg']; unset($_SESSION['booking_msg']); ?>
            </div>
        <?php endif; ?>

        <form action="booking_process.php" method="POST">
            <label>Select Service</label>
            <select name="service_type" required>
                <option value="Precision Tuning">Precision Tuning</option>
                <option value="Routine Maintenance">Routine Maintenance</option>
                <option value="Brake & Suspension">Brake & Suspension</option>
                <option value="Diagnostic Care">Diagnostic Care</option>
            </select>

            <label>Car Brand & Model</label>
            <input type="text" name="car_model" placeholder="e.g. BMW M3" style="width:100%; background:var(--navy-input); border:1.5px solid rgba(255,255,255,0.08); border-radius:8px; padding:13px; color:var(--text); margin-bottom:20px;" required>

            <label>Preferred Date</label>
            <input type="date" name="booking_date" min="<?php echo date('Y-m-d'); ?>" required>

            <button type="submit" class="btn-login" style="width: 100%;">Submit Booking Request</button>
        </form>
    </div>
</div>

</body>
</html>