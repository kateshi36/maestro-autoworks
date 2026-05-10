-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 10, 2026 at 08:37 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `maestro_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `appointments`
--

CREATE TABLE `appointments` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `service_id` int(11) NOT NULL,
  `vehicle_make` varchar(80) DEFAULT NULL,
  `vehicle_model` varchar(80) DEFAULT NULL,
  `vehicle_year` year(4) DEFAULT NULL,
  `plate_no` varchar(20) DEFAULT NULL,
  `appt_date` date NOT NULL,
  `appt_time` time NOT NULL,
  `notes` text DEFAULT NULL,
  `status` enum('pending','confirmed','declined','completed','cancelled') NOT NULL DEFAULT 'pending',
  `admin_notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `appointments`
--

INSERT INTO `appointments` (`id`, `user_id`, `service_id`, `vehicle_make`, `vehicle_model`, `vehicle_year`, `plate_no`, `appt_date`, `appt_time`, `notes`, `status`, `admin_notes`, `created_at`, `updated_at`) VALUES
(1, 3, 2, 'Toyota', 'Vios', '2020', 'UMAK220', '2026-05-11', '14:30:00', 'Car makes a breaking sound when we are passing a hump', 'completed', NULL, '2026-05-09 10:36:12', '2026-05-09 12:17:28'),
(2, 4, 2, 'Toyota', 'Vios', '2020', 'HERON1', '2026-05-13', '11:00:00', 'test break', 'completed', NULL, '2026-05-09 12:32:33', '2026-05-09 12:54:00'),
(3, 5, 8, 'Toyota', 'Vios', '2025', 'HERON2', '2026-05-13', '12:00:00', 'Transmission Problem', 'completed', NULL, '2026-05-09 12:39:02', '2026-05-09 12:53:59'),
(4, 6, 1, 'Toyota', 'Vios', '2018', 'HERON3', '2026-05-13', '13:00:00', 'Oil Change Problem', 'completed', NULL, '2026-05-09 12:41:14', '2026-05-09 12:53:58'),
(5, 7, 10, 'Toyota', 'Vios', '2017', 'HERON4', '2026-05-13', '14:00:00', 'Suspension Check Repair Problem', 'completed', NULL, '2026-05-09 12:46:14', '2026-05-09 12:53:57'),
(7, 3, 8, 'Toyota', 'Vios', NULL, 'UMAK2202', '2026-05-14', '14:30:00', 'Transmitter', 'declined', 'Too many customers', '2026-05-09 12:56:57', '2026-05-09 12:57:31'),
(8, 3, 3, 'Toyota', 'Vios', '2020', 'HEON2', '2026-05-12', '13:30:00', NULL, 'cancelled', NULL, '2026-05-09 13:01:39', '2026-05-09 13:01:47'),
(9, 3, 5, 'Toyota', 'Vios', '2023', 'HEON1', '2026-05-12', '15:00:00', NULL, 'completed', NULL, '2026-05-09 13:02:26', '2026-05-09 13:14:32'),
(10, 3, 2, 'Toyota', 'Vios', '2020', 'HERON13', '2026-05-11', '13:00:00', 'Break Problem', 'completed', NULL, '2026-05-10 12:46:22', '2026-05-10 17:29:10'),
(11, 3, 6, 'Toyota', 'Vios', '1960', 'TEST1', '2026-05-11', '11:30:00', NULL, 'completed', NULL, '2026-05-10 17:31:54', '2026-05-10 18:11:10'),
(12, 5, 8, 'Toyota', 'Vios', '1974', 'TEST2', '2026-05-20', '14:00:00', NULL, 'confirmed', NULL, '2026-05-10 17:34:05', '2026-05-10 17:38:13'),
(13, 6, 5, 'Toyota', 'Vios', '1979', 'TEST3', '2026-05-11', '14:00:00', NULL, 'completed', NULL, '2026-05-10 17:35:25', '2026-05-10 18:17:58'),
(14, 7, 3, 'Toyota', 'Vios', '1984', 'TEST4', '2026-05-11', '14:00:00', NULL, 'confirmed', NULL, '2026-05-10 17:36:03', '2026-05-10 17:38:25'),
(15, 8, 3, 'Toyota', 'Vios', '1986', 'TEST5', '2026-05-11', '15:30:00', NULL, 'confirmed', NULL, '2026-05-10 17:36:45', '2026-05-10 17:38:27'),
(16, 3, 10, 'Toyota', 'Vios', '2005', 'TEST6', '2026-05-11', '16:00:00', NULL, 'confirmed', NULL, '2026-05-10 17:39:39', '2026-05-10 17:39:55'),
(17, 3, 7, 'Toyota', 'Vios', '2000', 'TEST7', '2026-05-11', '15:30:00', 'testingggg', 'pending', NULL, '2026-05-10 18:22:04', '2026-05-10 18:22:04');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `appt_id` int(11) DEFAULT NULL,
  `type` enum('booking_received','booking_confirmed','booking_declined','booking_completed','booking_cancelled','reminder','task_started','vehicle_ready') NOT NULL,
  `message` text NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `user_id`, `appt_id`, `type`, `message`, `is_read`, `created_at`) VALUES
(1, 3, 1, 'booking_received', 'Your booking for Brake Inspection & Pad Replacement on May 11, 2026 at 2:30 PM has been received. We\'ll confirm it within 24 hours.', 1, '2026-05-09 10:36:12'),
(2, 1, 1, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Brake Inspection & Pad Replacement on May 11, 2026 at 2:30 PM.', 1, '2026-05-09 10:36:12'),
(3, 3, 1, 'booking_confirmed', 'Great news! Your Brake Inspection & Pad Replacement appointment on May 11, 2026 at 2:30 PM has been confirmed. Please arrive 10 minutes early.', 1, '2026-05-09 12:17:15'),
(4, 3, 1, 'booking_completed', 'Your Brake Inspection & Pad Replacement service on May 11, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 1, '2026-05-09 12:17:28'),
(5, 4, 2, 'booking_received', 'Your booking for Brake Inspection & Pad Replacement on May 13, 2026 at 11:00 AM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-09 12:32:33'),
(6, 1, 2, 'booking_received', 'New booking request from Patrick Dalupang: Brake Inspection & Pad Replacement on May 13, 2026 at 11:00 AM.', 1, '2026-05-09 12:32:34'),
(7, 5, 3, 'booking_received', 'Your booking for Transmission Service on May 13, 2026 at 12:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-09 12:39:02'),
(8, 1, 3, 'booking_received', 'New booking request from Yza David: Transmission Service on May 13, 2026 at 12:00 PM.', 1, '2026-05-09 12:39:02'),
(9, 6, 4, 'booking_received', 'Your booking for Oil Change on May 13, 2026 at 1:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-09 12:41:14'),
(10, 1, 4, 'booking_received', 'New booking request from Kat Eshi: Oil Change on May 13, 2026 at 1:00 PM.', 1, '2026-05-09 12:41:15'),
(11, 7, 5, 'booking_received', 'Your booking for Suspension Check & Repair on May 13, 2026 at 2:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-09 12:46:14'),
(12, 1, 5, 'booking_received', 'New booking request from pat man: Suspension Check & Repair on May 13, 2026 at 2:00 PM.', 1, '2026-05-09 12:46:14'),
(13, 8, NULL, 'booking_received', 'Your booking for Battery Replacement on May 13, 2026 at 3:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-09 12:48:21'),
(14, 1, NULL, 'booking_received', 'New booking request from bry bryan: Battery Replacement on May 13, 2026 at 3:00 PM.', 1, '2026-05-09 12:48:21'),
(15, 4, 2, 'booking_confirmed', 'Great news! Your Brake Inspection & Pad Replacement appointment on May 13, 2026 at 11:00 AM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-09 12:50:11'),
(16, 5, 3, 'booking_confirmed', 'Great news! Your Transmission Service appointment on May 13, 2026 at 12:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-09 12:50:17'),
(17, 6, 4, 'booking_confirmed', 'Great news! Your Oil Change appointment on May 13, 2026 at 1:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-09 12:50:19'),
(18, 7, 5, 'booking_confirmed', 'Great news! Your Suspension Check & Repair appointment on May 13, 2026 at 2:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-09 12:50:21'),
(19, 8, NULL, 'booking_confirmed', 'Great news! Your Battery Replacement appointment on May 13, 2026 at 3:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-09 12:50:35'),
(20, 8, NULL, 'booking_completed', 'Your Battery Replacement service on May 13, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-09 12:53:56'),
(21, 7, 5, 'booking_completed', 'Your Suspension Check & Repair service on May 13, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-09 12:53:57'),
(22, 6, 4, 'booking_completed', 'Your Oil Change service on May 13, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-09 12:53:58'),
(23, 5, 3, 'booking_completed', 'Your Transmission Service service on May 13, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-09 12:53:59'),
(24, 4, 2, 'booking_completed', 'Your Brake Inspection & Pad Replacement service on May 13, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-09 12:54:00'),
(25, 3, 7, 'booking_received', 'Your booking for Transmission Service on May 14, 2026 at 2:30 PM has been received. We\'ll confirm it within 24 hours.', 1, '2026-05-09 12:56:57'),
(26, 1, 7, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Transmission Service on May 14, 2026 at 2:30 PM.', 1, '2026-05-09 12:56:57'),
(27, 3, 7, 'booking_declined', 'We\'re sorry — your Transmission Service appointment on May 14, 2026 at 2:30 PM could not be accommodated. Reason: Too many customers', 1, '2026-05-09 12:57:31'),
(28, 3, 8, 'booking_received', 'Your booking for Tire Rotation & Balancing on May 12, 2026 at 1:30 PM has been received. We\'ll confirm it within 24 hours.', 1, '2026-05-09 13:01:39'),
(29, 1, 8, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Tire Rotation & Balancing on May 12, 2026 at 1:30 PM.', 1, '2026-05-09 13:01:39'),
(30, 3, 8, 'booking_cancelled', 'Your booking for Tire Rotation & Balancing on May 12, 2026 has been cancelled.', 1, '2026-05-09 13:01:47'),
(31, 3, 9, 'booking_received', 'Your booking for Full Tune-Up on May 12, 2026 at 3:00 PM has been received. We\'ll confirm it within 24 hours.', 1, '2026-05-09 13:02:26'),
(32, 1, 9, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Full Tune-Up on May 12, 2026 at 3:00 PM.', 1, '2026-05-09 13:02:26'),
(33, 3, 9, 'booking_confirmed', 'Great news! Your Full Tune-Up appointment on May 12, 2026 at 3:00 PM has been confirmed. Please arrive 10 minutes early.', 1, '2026-05-09 13:13:52'),
(34, 3, 9, 'booking_completed', 'Your Full Tune-Up service on May 12, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 1, '2026-05-09 13:14:32'),
(35, 3, 10, 'booking_received', 'Your booking for Brake Inspection & Pad Replacement on May 11, 2026 at 1:00 PM has been received. We\'ll confirm it within 24 hours.', 1, '2026-05-10 12:46:22'),
(36, 1, 10, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Brake Inspection & Pad Replacement on May 11, 2026 at 1:00 PM.', 1, '2026-05-10 12:46:22'),
(37, 3, 10, 'booking_confirmed', 'Great news! Your Brake Inspection & Pad Replacement appointment on May 11, 2026 at 1:00 PM has been confirmed. Please arrive 10 minutes early.', 1, '2026-05-10 12:48:20'),
(38, 3, 10, 'booking_completed', 'Your Brake Inspection & Pad Replacement service on May 11, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 1, '2026-05-10 17:29:10'),
(39, 3, 11, 'booking_received', 'Your booking for Air Conditioning Service on May 11, 2026 at 11:30 AM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:31:54'),
(40, 1, 11, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Air Conditioning Service on May 11, 2026 at 11:30 AM.', 0, '2026-05-10 17:31:54'),
(41, 5, 12, 'booking_received', 'Your booking for Transmission Service on May 20, 2026 at 2:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:34:05'),
(42, 1, 12, 'booking_received', 'New booking request from Yza David: Transmission Service on May 20, 2026 at 2:00 PM.', 0, '2026-05-10 17:34:05'),
(43, 6, 13, 'booking_received', 'Your booking for Full Tune-Up on May 11, 2026 at 2:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:35:25'),
(44, 1, 13, 'booking_received', 'New booking request from Kat Eshi: Full Tune-Up on May 11, 2026 at 2:00 PM.', 0, '2026-05-10 17:35:25'),
(45, 7, 14, 'booking_received', 'Your booking for Tire Rotation & Balancing on May 11, 2026 at 2:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:36:03'),
(46, 1, 14, 'booking_received', 'New booking request from pat man: Tire Rotation & Balancing on May 11, 2026 at 2:00 PM.', 0, '2026-05-10 17:36:03'),
(47, 8, 15, 'booking_received', 'Your booking for Tire Rotation & Balancing on May 11, 2026 at 3:30 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:36:45'),
(48, 1, 15, 'booking_received', 'New booking request from bry bryan: Tire Rotation & Balancing on May 11, 2026 at 3:30 PM.', 0, '2026-05-10 17:36:45'),
(49, 5, 12, 'booking_confirmed', 'Great news! Your Transmission Service appointment on May 20, 2026 at 2:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:38:13'),
(50, 3, 11, 'booking_confirmed', 'Great news! Your Air Conditioning Service appointment on May 11, 2026 at 11:30 AM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:38:19'),
(51, 6, 13, 'booking_confirmed', 'Great news! Your Full Tune-Up appointment on May 11, 2026 at 2:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:38:22'),
(52, 7, 14, 'booking_confirmed', 'Great news! Your Tire Rotation & Balancing appointment on May 11, 2026 at 2:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:38:25'),
(53, 8, 15, 'booking_confirmed', 'Great news! Your Tire Rotation & Balancing appointment on May 11, 2026 at 3:30 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:38:27'),
(54, 3, 16, 'booking_received', 'Your booking for Suspension Check & Repair on May 11, 2026 at 4:00 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 17:39:39'),
(55, 1, 16, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Suspension Check & Repair on May 11, 2026 at 4:00 PM.', 0, '2026-05-10 17:39:39'),
(56, 3, 16, 'booking_confirmed', 'Great news! Your Suspension Check & Repair appointment on May 11, 2026 at 4:00 PM has been confirmed. Please arrive 10 minutes early.', 0, '2026-05-10 17:39:55'),
(57, 3, 11, 'vehicle_ready', 'Great news, Clyde Ezekiel! All repair tasks for your Air Conditioning Service (booked for May 11, 2026) are complete. Your vehicle is ready for pickup at Maestro Autoworks!', 0, '2026-05-10 18:00:49'),
(58, 3, 11, 'booking_completed', 'Your Air Conditioning Service service on May 11, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-10 18:11:10'),
(59, 1, 11, 'booking_completed', 'Job closed: Air Conditioning Service for Clyde Ezekiel Ilarde on May 11, 2026.', 0, '2026-05-10 18:11:10'),
(60, 6, 13, 'booking_completed', 'Your Full Tune-Up service on May 11, 2026 has been marked as completed. Thank you for choosing Maestro Autoworks!', 0, '2026-05-10 18:17:58'),
(61, 3, 17, 'booking_received', 'Your booking for Wheel Alignment on May 11, 2026 at 3:30 PM has been received. We\'ll confirm it within 24 hours.', 0, '2026-05-10 18:22:04'),
(62, 1, 17, 'booking_received', 'New booking request from Clyde Ezekiel Ilarde: Wheel Alignment on May 11, 2026 at 3:30 PM.', 0, '2026-05-10 18:22:04');

-- --------------------------------------------------------

--
-- Table structure for table `repair_tasks`
--

CREATE TABLE `repair_tasks` (
  `id` int(11) NOT NULL,
  `appt_id` int(11) NOT NULL,
  `task_name` varchar(150) NOT NULL,
  `assigned_to` varchar(100) DEFAULT NULL,
  `status` enum('pending','in_progress','testing','completed') NOT NULL DEFAULT 'pending',
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `sort_order` tinyint(3) UNSIGNED NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `repair_tasks`
--

INSERT INTO `repair_tasks` (`id`, `appt_id`, `task_name`, `assigned_to`, `status`, `started_at`, `completed_at`, `notes`, `sort_order`, `created_at`, `updated_at`) VALUES
(1, 11, 'Drain old oil', 'maestro', 'completed', '2026-05-10 20:00:16', '2026-05-10 20:00:49', 'Becareful', 0, '2026-05-10 17:59:30', '2026-05-10 12:00:49'),
(2, 14, 'Inspect Break Pads', 'Meastro', 'completed', '2026-05-10 20:19:33', '2026-05-10 20:27:17', 'Don\'t forget to clean it', 0, '2026-05-10 18:19:24', '2026-05-10 12:27:17'),
(3, 14, 'Change Oil', 'Maestro', 'pending', NULL, NULL, 'Don\'t Spill it', 1, '2026-05-10 18:27:08', '2026-05-10 18:27:08');

-- --------------------------------------------------------

--
-- Table structure for table `services`
--

CREATE TABLE `services` (
  `id` int(11) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `duration_hr` decimal(4,1) NOT NULL DEFAULT 1.0,
  `price` decimal(10,2) DEFAULT NULL,
  `category` varchar(80) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `services`
--

INSERT INTO `services` (`id`, `name`, `description`, `duration_hr`, `price`, `category`, `active`) VALUES
(1, 'Oil Change', 'Full synthetic or conventional oil & filter replacement.', 0.5, 650.00, 'Maintenance', 1),
(2, 'Brake Inspection & Pad Replacement', 'Inspect rotors, calipers, and replace pads.', 2.0, 2500.00, 'Brakes', 1),
(3, 'Tire Rotation & Balancing', 'Rotate all four tires and balance for even wear.', 1.0, 800.00, 'Tires', 1),
(4, 'Battery Replacement', 'Test and replace car battery with warranty.', 0.5, 950.00, 'Electrical', 1),
(5, 'Full Tune-Up', 'Spark plugs, air filter, fuel filter, and timing check.', 3.0, 3500.00, 'Maintenance', 1),
(6, 'Air Conditioning Service', 'Refrigerant recharge, leak check, compressor inspection.', 2.0, 1800.00, 'Comfort', 1),
(7, 'Wheel Alignment', 'Four-wheel computerised alignment to manufacturer specs.', 1.5, 1200.00, 'Tires', 1),
(8, 'Transmission Service', 'Fluid flush and filter change for automatic or manual.', 2.5, 2800.00, 'Drivetrain', 1),
(9, 'Engine Diagnostics', 'OBD-II scan and full fault-code analysis report.', 1.0, 750.00, 'Diagnostics', 1),
(10, 'Suspension Check & Repair', 'Inspect shocks, struts, tie rods, and bushings.', 3.0, 4500.00, 'Suspension', 1);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `username` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('customer','admin') NOT NULL DEFAULT 'customer',
  `phone` varchar(30) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `first_name`, `last_name`, `username`, `email`, `password`, `role`, `phone`, `created_at`) VALUES
(1, 'Maestro', 'Admin', 'admin', 'admin@maestroautoworks.ph', '$2y$10$d..SP5jnXMX7Ke8nMylAtuUeLRA6Q7EI02Ef1BDuRPdtssFNycU8G', 'admin', NULL, '2026-05-09 10:27:48'),
(2, 'Juan', 'Dela Cruz', 'juan', 'juan@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer', '09171234567', '2026-05-09 10:27:48'),
(3, 'Clyde Ezekiel', 'Ilarde', 'clyde_esi', 'ilardeclyde@gmail.com', '$2y$10$XdccXrATJzBnXrWsAdlhTuP5RhQwReXJcQlLFpCO.BG7f6II.XGVa', 'customer', NULL, '2026-05-09 10:32:43'),
(4, 'Patrick', 'Dalupang', 'pat', 'patreng@gmail.com', '$2y$10$ws5P1PZ0btIfkfQrQeMfcOkZy1awilscTBDT4mzVFz5lPhZ.Nqvja', 'customer', NULL, '2026-05-09 12:25:52'),
(5, 'Yza', 'David', 'yza', 'yza@gmail.com', '$2y$10$rwTvQcMSJ8jdtshyiNMa7eatDodgPH8MtZhganKK/esS/F9pzwhqa', 'customer', NULL, '2026-05-09 12:36:55'),
(6, 'Kat', 'Eshi', 'kateshi', 'kateshi@gmail.com', '$2y$10$NUxwuHzCgTeVg3A80Gtm5.FNQrdk1GfFip8joVe/JBRsFWWcR.B9.', 'customer', NULL, '2026-05-09 12:40:31'),
(7, 'pat', 'man', 'patman', 'patman@gmail.com', '$2y$10$ZvHGH8TaWDryggf0wLgitu.f/iCI/t6sweTeV.JyFrH2wfpm/irW.', 'customer', NULL, '2026-05-09 12:42:33'),
(8, 'bry', 'bryan', 'bry', 'bry@gmail.com', '$2y$10$jRE3sOkhrTpMs0MAdPLdfuVMLYKEwSLV98ZvYiTsjO1/nPpE/tMqG', 'customer', NULL, '2026-05-09 12:47:18');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `appointments`
--
ALTER TABLE `appointments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_appointments_user` (`user_id`),
  ADD KEY `fk_appointments_service` (`service_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_notifications_user` (`user_id`),
  ADD KEY `fk_notifications_appointment` (`appt_id`);

--
-- Indexes for table `repair_tasks`
--
ALTER TABLE `repair_tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_tasks_appt` (`appt_id`);

--
-- Indexes for table `services`
--
ALTER TABLE `services`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `appointments`
--
ALTER TABLE `appointments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=63;

--
-- AUTO_INCREMENT for table `repair_tasks`
--
ALTER TABLE `repair_tasks`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `services`
--
ALTER TABLE `services`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `appointments`
--
ALTER TABLE `appointments`
  ADD CONSTRAINT `fk_appointments_service` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`),
  ADD CONSTRAINT `fk_appointments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_appointment` FOREIGN KEY (`appt_id`) REFERENCES `appointments` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `repair_tasks`
--
ALTER TABLE `repair_tasks`
  ADD CONSTRAINT `fk_tasks_appt` FOREIGN KEY (`appt_id`) REFERENCES `appointments` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
