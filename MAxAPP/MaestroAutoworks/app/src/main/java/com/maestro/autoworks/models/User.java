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

    // ── License fields (added in registration Step 3/4) ──
    public String driversLicenseNo;       // e.g. "D01-00-123456"
    public String driversLicenseIssuance; // "YYYY-MM-DD"
    public String driversLicenseExpiry;   // "YYYY-MM-DD"
    /** Comma-separated DL codes, e.g. "A,B,BE" */
    public String driversLicenseCodes;
    public String conductorsLicenseNo;       // null if not a conductor
    public String conductorsLicenseIssuance; // "YYYY-MM-DD"
    public String conductorsLicenseExpiry;   // "YYYY-MM-DD"
    public String licenseImagePath;          // content URI of captured license photo

    // ── Vehicle & verification fields (added in registration Step 4) ──
    /** Philippine license plate, e.g. "ABC 1234" or "AB 1234" (pre-2014). */
    public String licensePlate;

    /**
     * Motor Vehicle File Number — 15-digit numeric string assigned by the LTO,
     * e.g. "123456789012345".
     */
    public String mvFileNumber;

    /** Vehicle make (manufacturer), e.g. "Toyota". */
    public String vehicleMake;

    /** Vehicle model, e.g. "Vios 1.3 XLE MT". */
    public String vehicleModel;

    // ── Document image URIs / file paths (Step 4 uploads) ──
    /**
     * Local content URI or file path for the uploaded Driver's License photo.
     * Distinct from {@link #licenseImagePath} which is captured live via camera in Step 3;
     * this is the document upload in Step 4 and may be selected from the gallery.
     */
    public String dlUploadPath;

    /**
     * Local content URI or file path for the uploaded Official Receipt (OR).
     * This is the payment receipt issued by the LTO when the vehicle was registered/renewed.
     */
    public String orImagePath;

    /**
     * Local content URI or file path for the uploaded Certificate of Registration (CR).
     * This is the LTO document certifying ownership and vehicle details.
     */
    public String crImagePath;

    public String getFullName() { return firstName + " " + lastName; }
    public boolean isAdmin()    { return "admin".equals(role); }

    // ── Document verification fields (DB_VERSION 11) ──────────────────────────
    /**
     * Admin-controlled verification state for this user's uploaded documents.
     * Values: {@code "pending"} (default) | {@code "verified"} | {@code "rejected"}.
     */
    public String verificationStatus = "pending";

    /**
     * Free-text note written by the admin when rejecting this user's documents.
     * {@code null} when the account is pending or verified.
     */
    public String adminRejectionNote;
}