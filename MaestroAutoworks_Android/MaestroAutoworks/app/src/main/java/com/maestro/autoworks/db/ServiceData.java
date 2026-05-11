package com.maestro.autoworks.db;

import com.maestro.autoworks.models.Service;
import java.util.Arrays;
import java.util.List;

/** Static services catalog mirroring the PHP app's services table. */
public class ServiceData {

    public static List<Service> getAll() {
        return Arrays.asList(
            new Service("Oil Change & Filter",       "Maintenance", 699,   1.0, "Full synthetic or conventional oil change with new filter and 27-point inspection."),
            new Service("Brake Pad Replacement",     "Brakes",      2500,  2.0, "Front or rear disc brake pad swap using OEM-grade compound with road test."),
            new Service("Air-Con Regas & Check",     "Aircon",      1200,  1.5, "Refrigerant top-up, leak check, and cabin filter inspection for a cool ride."),
            new Service("Engine Tune-Up",            "Engine",      3800,  3.0, "Spark plugs, air filter, fuel injector cleaning, and timing check."),
            new Service("Wheel Alignment & Balance", "Tires",        950,  1.5, "4-wheel computer alignment and dynamic wheel balancing for optimal handling."),
            new Service("Transmission Service",      "Drivetrain",  4200,  2.5, "ATF or MTF flush, filter replacement, and computer scan for smooth shifting."),
            new Service("Battery Replacement",       "Electrical",  3500,  0.5, "OEM or aftermarket battery swap with charging system test."),
            new Service("Coolant System Flush",      "Maintenance", 1500,  1.5, "Full coolant drain, flush, and refill with premium antifreeze."),
            new Service("Timing Belt Replacement",   "Engine",      8500,  4.0, "Belt, tensioner, and idler pulley replacement with water pump check."),
            new Service("Suspension Check & Repair", "Suspension",  5000,  3.0, "Shock absorbers, struts, ball joints, and bushings inspection and repair."),
            new Service("Exhaust System Repair",     "Exhaust",     2800,  2.0, "Muffler, catalytic converter, and pipe inspection and replacement."),
            new Service("Full Car Detailing",        "Detailing",   2500,  3.0, "Interior vacuuming, exterior wash, wax, and trim restoration.")
        );
    }

    public static String[] getNames() {
        List<Service> services = getAll();
        String[] names = new String[services.size()];
        for (int i = 0; i < services.size(); i++) {
            names[i] = services.get(i).name;
        }
        return names;
    }
}
