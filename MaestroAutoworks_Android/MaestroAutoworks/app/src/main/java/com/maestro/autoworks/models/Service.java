package com.maestro.autoworks.models;

public class Service {
    public String name;
    public String category;
    public double price;
    public double durationHr;
    public String description;

    public Service(String name, String category, double price,
                   double durationHr, String description) {
        this.name        = name;
        this.category    = category;
        this.price       = price;
        this.durationHr  = durationHr;
        this.description = description;
    }

    @Override
    public String toString() {
        return name + "  —  ₱" + String.format("%.2f", price);
    }
}
