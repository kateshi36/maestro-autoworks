package com.maestro.autoworks.models;

public class Service {
    public int    id;
    public String name;
    public String description;
    public String category;
    public double price;
    public double durationHr;
    public boolean active = true;

    public Service() {}

    public Service(int id, String name, String description, String category, double price, double durationHr) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.category    = category;
        this.price       = price;
        this.durationHr  = durationHr;
        this.active      = true;
    }
}
