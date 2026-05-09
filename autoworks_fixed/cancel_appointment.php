<?php
// cancel_appointment.php — customer cancels their own pending appointment
session_start();
require 'db.php';

if (!isset($_SESSION['user_id'])) { header('Location: login.php'); exit; }

$id = (int)($_GET['id'] ?? 0);

if ($id) {
    // Only allow cancelling own pending appointments
    $stmt = $pdo->prepare("
        UPDATE appointments SET status = 'cancelled'
        WHERE id = ? AND user_id = ? AND status = 'pending'
    ");
    $stmt->execute([$id, $_SESSION['user_id']]);

    if ($stmt->rowCount()) {
        // P5: Notify the customer
        $appt = $pdo->prepare("SELECT a.*, s.name AS service_name FROM appointments a JOIN services s ON s.id=a.service_id WHERE a.id=?");
        $appt->execute([$id]);
        $a = $appt->fetch();
        if ($a) {
            $pdo->prepare("INSERT INTO notifications (user_id,appt_id,type,message) VALUES(?,?,'booking_cancelled',?)")
                ->execute([$_SESSION['user_id'], $id,
                    "Your booking for {$a['service_name']} on ".date('F j, Y',strtotime($a['appt_date']))." has been cancelled."]);
        }
        $_SESSION['flash'] = ['type'=>'success','msg'=>'Appointment cancelled successfully.'];
    } else {
        $_SESSION['flash'] = ['type'=>'error','msg'=>'Could not cancel this appointment.'];
    }
}

header('Location: dashboard.php');
exit;
?>
