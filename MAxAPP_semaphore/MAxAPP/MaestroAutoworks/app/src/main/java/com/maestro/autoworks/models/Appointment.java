package com.maestro.autoworks.models;

public class Appointment {
    public int    id;
    public int    userId;
    // ── Vehicle details (Step 2 flow) ──
    public String carModel;      // e.g. "Toyota"
    public String yearModel;     // e.g. "2022"
    public String fuelType;      // "Gasoline" | "Diesel"
    // ── OR/CR verification ──
    public String orcrStatus;    // e.g. "Yes (photo captured)" | "No"
    public String orcrImagePath; // content URI string of the captured photo, or null
    // ── Service booking ──
    public String serviceName;
    public String date;
    public String time;
    public String carPlate;
    public double totalPrice;
    public String status;
    public int    rating;
    // ── Admin extras (joined from users table) ──
    public String customerName;
    public String adminNote;
}
