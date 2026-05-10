<?php
// ─── Output buffering (prevents "headers already sent" errors) ───────────────
ob_start();

// ─── Database Connection ────────────────────────────────────────────────────
$host   = 'localhost';
$dbname = 'maestro_db';
$user   = 'root';
$pass   = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    die('Database connection failed: ' . $e->getMessage());
}

/*
 ══════════════════════════════════════════════════════════════════════════════
   MAESTRO AUTOWORKS — FULL DATABASE SCHEMA  (run once to initialise)
 ══════════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS maestro_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE maestro_db;

-- P1: User Authentication
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('customer','admin') NOT NULL DEFAULT 'customer',
    phone      VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- P2: Services catalog
CREATE TABLE IF NOT EXISTS services (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    duration_hr DECIMAL(4,1) NOT NULL DEFAULT 1.0,
    price       DECIMAL(10,2),
    category    VARCHAR(80),
    active      TINYINT(1) NOT NULL DEFAULT 1
);

INSERT INTO services (name, description, duration_hr, price, category) VALUES
('Oil Change',                       'Full synthetic or conventional oil & filter replacement.',  0.5,  650.00,  'Maintenance'),
('Brake Inspection & Pad Replacement','Inspect rotors, calipers, and replace pads.',              2.0, 2500.00,  'Brakes'),
('Tire Rotation & Balancing',        'Rotate all four tires and balance for even wear.',          1.0,  800.00,  'Tires'),
('Battery Replacement',              'Test and replace car battery with warranty.',               0.5,  950.00,  'Electrical'),
('Full Tune-Up',                     'Spark plugs, air filter, fuel filter, and timing check.',   3.0, 3500.00,  'Maintenance'),
('Air Conditioning Service',         'Refrigerant recharge, leak check, compressor inspection.', 2.0, 1800.00,  'Comfort'),
('Wheel Alignment',                  'Four-wheel computerised alignment to manufacturer specs.',  1.5, 1200.00,  'Tires'),
('Transmission Service',             'Fluid flush and filter change for automatic or manual.',    2.5, 2800.00,  'Drivetrain'),
('Engine Diagnostics',               'OBD-II scan and full fault-code analysis report.',          1.0,  750.00,  'Diagnostics'),
('Suspension Check & Repair',        'Inspect shocks, struts, tie rods, and bushings.',           3.0, 4500.00,  'Suspension');

-- P3/P4: Appointments
CREATE TABLE IF NOT EXISTS appointments (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    service_id    INT NOT NULL,
    vehicle_make  VARCHAR(80),
    vehicle_model VARCHAR(80),
    vehicle_year  YEAR,
    plate_no      VARCHAR(20),
    appt_date     DATE NOT NULL,
    appt_time     TIME NOT NULL,
    notes         TEXT,
    status        ENUM('pending','confirmed','declined','completed','cancelled') NOT NULL DEFAULT 'pending',
    admin_notes   TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
);

-- P5: Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    appt_id    INT,
    type       ENUM('booking_received','booking_confirmed','booking_declined',
                    'booking_completed','booking_cancelled','reminder') NOT NULL,
    message    TEXT NOT NULL,
    is_read    TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (appt_id) REFERENCES appointments(id) ON DELETE SET NULL
);

-- Default admin account  (password: Admin@1234)
INSERT IGNORE INTO users (first_name, last_name, username, email, password, role)
VALUES ('Maestro','Admin','admin','admin@maestroautoworks.ph',
        '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','admin');

*/
?>
