package com.maestro.autoworks.db;

import com.maestro.autoworks.models.Service;
import java.util.ArrayList;
import java.util.List;

/** Static services catalog mirroring the PHP app's services table. Admin can add to this at runtime. */
public class ServiceData {

    private static final List<Service> services = new ArrayList<>();

    static {
        services.add(new Service(1,  "Oil Change & Filter",       "Full synthetic or conventional oil change with new filter and 27-point inspection.", "Maintenance", 699,   1.0));
        services.add(new Service(2,  "Brake Pad Replacement",     "Front or rear disc brake pad swap using OEM-grade compound with road test.",          "Brakes",      2500,  2.0));
        services.add(new Service(3,  "Air-Con Regas & Check",     "Refrigerant top-up, leak check, and cabin filter inspection.",                         "Aircon",      1200,  1.5));
        services.add(new Service(4,  "Engine Tune-Up",            "Spark plugs, air filter, fuel injector cleaning, and timing check.",                   "Engine",      3800,  3.0));
        services.add(new Service(5,  "Wheel Alignment & Balance", "4-wheel computer alignment and dynamic wheel balancing.",                               "Tires",       950,   1.5));
        services.add(new Service(6,  "Transmission Service",      "ATF or MTF flush, filter replacement, and computer scan.",                             "Drivetrain",  4200,  2.5));
        services.add(new Service(7,  "Battery Replacement",       "OEM or aftermarket battery swap with charging system test.",                            "Electrical",  3500,  0.5));
        services.add(new Service(8,  "Coolant System Flush",      "Full coolant drain, flush, and refill with premium antifreeze.",                       "Maintenance", 1500,  1.5));
        services.add(new Service(9,  "Timing Belt Replacement",   "Belt, tensioner, and idler pulley replacement with water pump check.",                 "Engine",      8500,  4.0));
        services.add(new Service(10, "Suspension Check & Repair", "Shock absorbers, struts, ball joints, and bushings inspection.",                       "Suspension",  5000,  3.0));
        services.add(new Service(11, "Exhaust System Repair",     "Muffler, catalytic converter, and pipe inspection and replacement.",                   "Exhaust",     2800,  2.0));
        services.add(new Service(12, "Full Car Detailing",        "Interior vacuuming, exterior wash, wax, and trim restoration.",                        "Detailing",   2500,  3.0));
    }

    public static List<Service> getAll() { return services; }

    public static void addService(Service s) { services.add(s); }

    public static String[] getNames() {
        String[] names = new String[services.size()];
        for (int i = 0; i < services.size(); i++) names[i] = services.get(i).name;
        return names;
    }
}
