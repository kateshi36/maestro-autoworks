<?php
session_start();
require 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_SESSION['user_id'])) {
    $user_id = $_SESSION['user_id'];
    $service = $_POST['service_type'];
    $car = trim($_POST['car_model']);
    $date = $_POST['booking_date'];

    if (!empty($car) && !empty($date)) {
        $stmt = $pdo->prepare("INSERT INTO bookings (user_id, service_type, car_model, booking_date) VALUES (?, ?, ?, ?)");
        $stmt->execute([$user_id, $service, $car, $date]);

        $_SESSION['booking_msg'] = "Request submitted! We will contact you shortly.";
    } else {
        $_SESSION['booking_msg'] = "Please fill all fields.";
    }
}

header('Location: booking.php');
exit;