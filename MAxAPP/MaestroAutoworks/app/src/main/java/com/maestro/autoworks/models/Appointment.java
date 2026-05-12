package com.maestro.autoworks.models;

public class Appointment {
    public int    id;
    public int    userId;
    public String serviceName;
    public String date;
    public String time;
    public String carPlate;
    public double totalPrice;
    public String status;
    public int    rating;
    // Admin extras
    public String customerName; // joined from users table
    public String adminNote;
}
