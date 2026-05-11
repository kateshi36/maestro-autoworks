package com.maestro.autoworks.models;

public class User {
    public int    id;
    public String firstName;
    public String lastName;
    public String username;
    public String email;
    public String phone;
    public String password;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
