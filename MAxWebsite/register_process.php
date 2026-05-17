<?php
session_start();
require 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: register.php');
    exit;
}

$action = trim($_POST['action'] ?? '');

// ══════════════════════════════════════════════════════════════════════════════
//  ACTION: captcha
//  Mirrors RegisterActivity.java setupStep1() — validates the math CAPTCHA
//  answer stored in $_SESSION['captcha_answer']. On success, sets
//  $_SESSION['captcha_passed'] so register.php shows the full form.
//  On failure, regenerates a fresh question (same as generateCaptcha()).
// ══════════════════════════════════════════════════════════════════════════════
if ($action === 'captcha') {

    $given = trim($_POST['captcha_answer'] ?? '');

    // Guard: must be a numeric string
    if ($given === '' || !is_numeric($given)) {
        $_SESSION['captcha_error'] = 'Please enter a number.';
        _regenerateCaptcha();
        header('Location: register.php');
        exit;
    }

    $given  = (int) $given;
    $expect = (int) ($_SESSION['captcha_answer'] ?? PHP_INT_MIN);

    if ($given !== $expect) {
        $_SESSION['captcha_error'] = 'Incorrect — try the new question.';
        _regenerateCaptcha();
        header('Location: register.php');
        exit;
    }

    // Correct — mark passed, clear used question
    $_SESSION['captcha_passed'] = true;
    unset($_SESSION['captcha_answer'], $_SESSION['captcha_question']);
    header('Location: register.php');
    exit;
}

// ══════════════════════════════════════════════════════════════════════════════
//  ACTION: license
//  Mirrors RegisterActivity.java setupStep2() / rgHasLicense listener.
//  Validates that the user selected "Yes" (has a valid DL).
//  "Yes" → sets $_SESSION['license_passed'], proceeds to the registration form.
//  "No"  → hard block: clears any stale license_passed and returns an error.
//  Direct POST without captcha_passed is also bounced.
// ══════════════════════════════════════════════════════════════════════════════
if ($action === 'license') {

    // Must have passed the CAPTCHA gate first
    if (empty($_SESSION['captcha_passed'])) {
        header('Location: register.php');
        exit;
    }

    $hasLicense = trim($_POST['has_license'] ?? '');

    if ($hasLicense === '') {
        // Nothing selected — same as the app's "Please answer the question above." Toast
        $_SESSION['license_error'] = 'Please answer the question above.';
        header('Location: register.php');
        exit;
    }

    if ($hasLicense === 'no') {
        // Hard block — matches the app's layoutLicenseBlocked behaviour:
        // the Continue button is hidden and the blocked strip is shown.
        // A direct POST with has_license=no is also caught here.
        unset($_SESSION['license_passed']);
        $_SESSION['license_error'] = 'A valid driver\'s license is required to register. Please obtain one from the LTO first.';
        header('Location: register.php');
        exit;
    }

    if ($hasLicense !== 'yes') {
        // Tampered value
        $_SESSION['license_error'] = 'Invalid selection. Please try again.';
        header('Location: register.php');
        exit;
    }

    // ── "Yes" — gate passed ───────────────────────────────────────────────────
    $_SESSION['license_passed'] = true;
    header('Location: register.php');
    exit;
}

// ══════════════════════════════════════════════════════════════════════════════
//  ACTION: register
//  Only reachable after both captcha_passed AND license_passed are set.
// ══════════════════════════════════════════════════════════════════════════════
if ($action === 'register') {

    // Server-side gate — both steps must be cleared
    if (empty($_SESSION['captcha_passed']) || empty($_SESSION['license_passed'])) {
        header('Location: register.php');
        exit;
    }

    $first_name       = trim($_POST['first_name']       ?? '');
    $last_name        = trim($_POST['last_name']        ?? '');
    $username         = trim($_POST['username']         ?? '');
    $email            = trim($_POST['email']            ?? '');
    $confirm_email    = trim($_POST['confirm_email']    ?? '');
    $birthdate        = trim($_POST['birthdate']        ?? '');
    $gender           = trim($_POST['gender']           ?? '');
    $phone            = trim($_POST['phone']            ?? '');
    $address          = trim($_POST['address']          ?? '');

    // ── Step 4: License & Vehicle fields ─────────────────────────────────────
    $drivers_license_no        = strtoupper(trim($_POST['drivers_license_no']        ?? ''));
    $drivers_license_issuance  = trim($_POST['drivers_license_issuance']             ?? '');
    $drivers_license_expiry    = trim($_POST['drivers_license_expiry']               ?? '');
    $dl_codes_raw              = $_POST['dl_codes'] ?? [];
    $has_conductors            = !empty($_POST['has_conductors']);
    $conductors_license_no     = $has_conductors ? strtoupper(trim($_POST['conductors_license_no']    ?? '')) : null;
    $conductors_license_issuance = $has_conductors ? trim($_POST['conductors_license_issuance']       ?? '') : null;
    $conductors_license_expiry   = $has_conductors ? trim($_POST['conductors_license_expiry']         ?? '') : null;
    $license_plate             = strtoupper(trim($_POST['license_plate']             ?? ''));
    $mv_file_number            = trim($_POST['mv_file_number']                       ?? '');
    $vehicle_make              = trim($_POST['vehicle_make']                         ?? '');
    $vehicle_model             = trim($_POST['vehicle_model']                        ?? '');

    // Whitelist DL restriction codes
    $valid_dl_codes = ['A','A1','B','B1','B2','C','D','BE','CE'];
    $dl_codes = array_values(array_intersect((array)$dl_codes_raw, $valid_dl_codes));
    $dl_codes_str = implode(',', $dl_codes);

    $password         = $_POST['password']              ?? '';
    $confirm_password = $_POST['confirm_password']      ?? '';
    $terms            = isset($_POST['terms']);

    $_SESSION['reg_old'] = compact(
        'first_name', 'last_name', 'username', 'email', 'confirm_email',
        'birthdate', 'gender', 'phone', 'address',
        'drivers_license_no', 'drivers_license_issuance', 'drivers_license_expiry',
        'has_conductors', 'conductors_license_no', 'conductors_license_issuance', 'conductors_license_expiry',
        'license_plate', 'mv_file_number', 'vehicle_make', 'vehicle_model'
    );
    $_SESSION['reg_old']['dl_codes'] = $dl_codes_str;

    // ── Required-field checks ─────────────────────────────────────────────────
    if (empty($first_name) || empty($last_name) || empty($username) || empty($email) ||
        empty($confirm_email) || empty($birthdate) || empty($gender) ||
        empty($phone) || empty($address) || empty($password)) {
        $_SESSION['reg_error'] = 'Please fill in all fields.';
        header('Location: register.php');
        exit;
    }

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $_SESSION['reg_error'] = 'Please enter a valid email address.';
        header('Location: register.php');
        exit;
    }

    if ($email !== $confirm_email) {
        $_SESSION['reg_error'] = 'Email addresses do not match.';
        header('Location: register.php');
        exit;
    }

    $bd = DateTime::createFromFormat('Y-m-d', $birthdate);
    if (!$bd || $bd->format('Y-m-d') !== $birthdate) {
        $_SESSION['reg_error'] = 'Please enter a valid date of birth.';
        header('Location: register.php');
        exit;
    }
    $age = (new DateTime())->diff($bd)->y;
    if ($age < 16) {
        $_SESSION['reg_error'] = 'You must be at least 16 years old to register.';
        header('Location: register.php');
        exit;
    }

    if (!in_array($gender, ['Male', 'Female', 'Other'], true)) {
        $_SESSION['reg_error'] = 'Please select a valid gender option.';
        header('Location: register.php');
        exit;
    }

    // ── Step 4 validation ─────────────────────────────────────────────────────
    $dl_pattern   = '/^[A-Z][0-9]{2}-[0-9]{2}-[0-9]{6}$/';
    $plate_pattern = '/^[A-Z]{2,3} [0-9]{4}$/';
    $mv_pattern   = '/^[0-9]{15}$/';
    $make_pattern  = '/^[A-Za-z][A-Za-z\s\-]{1,39}$/';
    $model_pattern = '/^[A-Za-z0-9][A-Za-z0-9\s.\-\/()\'"]{1,59}$/';

    if (!preg_match($dl_pattern, $drivers_license_no)) {
        $_SESSION['reg_error'] = "Invalid Driver's License number format (e.g. D01-00-123456).";
        header('Location: register.php');
        exit;
    }

    $today = date('Y-m-d');
    if (empty($drivers_license_issuance) || $drivers_license_issuance > $today) {
        $_SESSION['reg_error'] = "Driver's License issuance date must be today or in the past.";
        header('Location: register.php');
        exit;
    }
    if (empty($drivers_license_expiry) || $drivers_license_expiry <= $today) {
        $_SESSION['reg_error'] = "Driver's License expiry date must be a future date.";
        header('Location: register.php');
        exit;
    }

    if (empty($dl_codes)) {
        $_SESSION['reg_error'] = 'Please select at least one DL restriction code.';
        header('Location: register.php');
        exit;
    }

    // Conductor's license — validate only if the checkbox was ticked
    if ($has_conductors) {
        if (empty($conductors_license_no) || !preg_match($dl_pattern, $conductors_license_no)) {
            $_SESSION['reg_error'] = 'Invalid Conductor\'s License number format.';
            header('Location: register.php');
            exit;
        }
        if (empty($conductors_license_issuance) || $conductors_license_issuance > $today) {
            $_SESSION['reg_error'] = "Conductor's License issuance date must be today or in the past.";
            header('Location: register.php');
            exit;
        }
        if (empty($conductors_license_expiry) || $conductors_license_expiry <= $today) {
            $_SESSION['reg_error'] = "Conductor's License expiry date must be a future date.";
            header('Location: register.php');
            exit;
        }
    }

    if (!preg_match($plate_pattern, $license_plate)) {
        $_SESSION['reg_error'] = 'Invalid license plate format (e.g. ABC 1234).';
        header('Location: register.php');
        exit;
    }

    if (!preg_match($mv_pattern, $mv_file_number)) {
        $_SESSION['reg_error'] = 'MV File Number must be exactly 15 digits.';
        header('Location: register.php');
        exit;
    }

    if (!preg_match($make_pattern, $vehicle_make)) {
        $_SESSION['reg_error'] = 'Vehicle make must be letters only, 2–40 characters (e.g. Toyota).';
        header('Location: register.php');
        exit;
    }

    if (!preg_match($model_pattern, $vehicle_model)) {
        $_SESSION['reg_error'] = 'Vehicle model must be 2–60 characters (e.g. Vios 1.3 XLE MT).';
        header('Location: register.php');
        exit;
    }

    if (strlen($password) < 8) {
        $_SESSION['reg_error'] = 'Password must be at least 8 characters.';
        header('Location: register.php');
        exit;
    }

    if ($password !== $confirm_password) {
        $_SESSION['reg_error'] = 'Passwords do not match.';
        header('Location: register.php');
        exit;
    }

    if (!$terms) {
        $_SESSION['reg_error'] = 'You must agree to the Terms & Conditions.';
        header('Location: register.php');
        exit;
    }

    $stmt = $pdo->prepare("SELECT id FROM users WHERE username = ? OR email = ?");
    $stmt->execute([$username, $email]);
    if ($stmt->fetch()) {
        $_SESSION['reg_error'] = 'Username or email is already taken.';
        header('Location: register.php');
        exit;
    }

    // ── Step 5: Document uploads ──────────────────────────────────────────────
    $allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
    $maxBytes     = 5 * 1024 * 1024;   // 5 MB
    $uploadDir    = __DIR__ . '/uploads/documents/';

    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    $docSlots = [
        'dl_upload' => ['col' => 'dl_upload_path',  'label' => "Driver's License"],
        'or_upload' => ['col' => 'or_image_path',   'label' => 'Official Receipt'],
        'cr_upload' => ['col' => 'cr_image_path',   'label' => 'Certificate of Registration'],
    ];
    $docPaths = [];

    foreach ($docSlots as $field => $meta) {
        if (empty($_FILES[$field]['tmp_name']) || $_FILES[$field]['error'] !== UPLOAD_ERR_OK) {
            $_SESSION['reg_error'] = "Please upload your {$meta['label']} image.";
            header('Location: register.php');
            exit;
        }
        $tmp  = $_FILES[$field]['tmp_name'];
        $size = $_FILES[$field]['size'];
        $mime = mime_content_type($tmp);

        if (!in_array($mime, $allowedMimes, true)) {
            $_SESSION['reg_error'] = "{$meta['label']}: only JPG, PNG, or WEBP images are accepted.";
            header('Location: register.php');
            exit;
        }
        if ($size > $maxBytes) {
            $_SESSION['reg_error'] = "{$meta['label']}: file must be smaller than 5 MB.";
            header('Location: register.php');
            exit;
        }

        $ext      = match ($mime) {
            'image/jpeg' => 'jpg',
            'image/png'  => 'png',
            'image/webp' => 'webp',
        };
        $filename  = $field . '_' . bin2hex(random_bytes(8)) . '.' . $ext;
        $destPath  = $uploadDir . $filename;

        if (!move_uploaded_file($tmp, $destPath)) {
            $_SESSION['reg_error'] = "Failed to save {$meta['label']} — please try again.";
            header('Location: register.php');
            exit;
        }
        // Store the web-accessible relative path
        $docPaths[$meta['col']] = 'uploads/documents/' . $filename;
    }

    $hashed = password_hash($password, PASSWORD_DEFAULT);
    $stmt   = $pdo->prepare(
        "INSERT INTO users
            (first_name, last_name, username, email, password,
             birthdate, gender, phone, address,
             drivers_license_no, drivers_license_issuance, drivers_license_expiry, dl_codes,
             conductors_license_no, conductors_license_issuance, conductors_license_expiry,
             license_plate, mv_file_number, vehicle_make, vehicle_model,
             dl_upload_path, or_image_path, cr_image_path)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $stmt->execute([
        $first_name, $last_name, $username, $email, $hashed,
        $birthdate, $gender, $phone, $address,
        $drivers_license_no, $drivers_license_issuance, $drivers_license_expiry, $dl_codes_str,
        $conductors_license_no, $conductors_license_issuance ?: null, $conductors_license_expiry ?: null,
        $license_plate, $mv_file_number, $vehicle_make, $vehicle_model,
        $docPaths['dl_upload_path'], $docPaths['or_image_path'], $docPaths['cr_image_path'],
    ]);

    unset(
        $_SESSION['reg_old'],
        $_SESSION['captcha_passed'],
        $_SESSION['license_passed'],
        $_SESSION['captcha_answer'],
        $_SESSION['captcha_question']
    );

    $_SESSION['reg_success'] = 'Account created successfully! You can now sign in.';
    header('Location: register.php');
    exit;
}

// Unknown action
header('Location: register.php');
exit;

function _regenerateCaptcha(): void {
    $a = random_int(1, 10);
    $b = random_int(1, 10);
    if (rand(0, 1)) {
        $_SESSION['captcha_answer']   = $a + $b;
        $_SESSION['captcha_question'] = "What is {$a} + {$b}?";
    } else {
        if ($a < $b) { [$a, $b] = [$b, $a]; }
        $_SESSION['captcha_answer']   = $a - $b;
        $_SESSION['captcha_question'] = "What is {$a} \xe2\x88\x92 {$b}?";
    }
}
?>
