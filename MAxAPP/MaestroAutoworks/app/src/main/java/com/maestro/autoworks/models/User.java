package com.maestro.autoworks.models;

public class User {
    public int    id;
    public String firstName;
    public String lastName;
    public String username;
    public String email;
    public String phone;
    public String password;
    public String role; // "customer" | "admin"

    // ── Personal info fields (added in registration Step 5) ──
    public String birthdate; // "YYYY-MM-DD"
    public String gender;    // "Male" | "Female" | "Prefer not to say"

    // ── License fields (added in registration Step 3) ──
    public String driversLicenseNo;     // e.g. "N01-23-456789"
    public String driversLicenseExpiry; // "YYYY-MM-DD"
    public String conductorsLicenseNo;  // null if not a conductor
    public String conductorsLicenseExpiry;
    public String licenseImagePath;     // content URI of captured license photo

    public String getFullName() { return firstName + " " + lastName; }
    public boolean isAdmin()    { return "admin".equals(role); }
}
