package com.maestro.autoworks.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.maestro.autoworks.R;
import com.maestro.autoworks.adapters.AppointmentAdapter;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.db.DatabaseHelper.AppNotification;
import com.maestro.autoworks.db.ServiceData;
import com.maestro.autoworks.db.SessionManager;
import com.maestro.autoworks.models.Appointment;
import com.maestro.autoworks.models.Service;
import com.maestro.autoworks.models.User;

import java.util.Calendar;
import java.util.List;

/**
 * HomeActivity — Single-controller for all 5 user-facing panels.
 *
 * Sub-stage 5 implementation:
 *   • showPanel(int) hides every panel/section then reveals the target.
 *   • Topbar adapts: Home → hamburger + centred logo; others → back arrow + title.
 *   • Bottom nav label colours track the active panel.
 *   • Drawer menu items call showPanel() then close the drawer.
 *   • Hero CTA buttons ("BOOK AN APPOINTMENT", "BROWSE SERVICES") call showPanel().
 *   • panelBook   wires every BookActivity control in-place (spinners, date picker,
 *                 radio buttons, checkboxes, rating bar, plate validation, booking save).
 *   • panelAppointments loads real rows from DB + notification badge.
 *   • panelProfile   loads real user data from DB.
 *   • panelServices  wires each card click → showPanel(PANEL_BOOK) with pre-selection.
 *   • Back press:  non-Home panel → return to Home; drawer open → close drawer;
 *                  Home → default system back.
 */
public class HomeActivity extends AppCompatActivity {

    // ── Panel IDs ────────────────────────────────────────────────────────────
    private static final int PANEL_HOME         = 0;
    private static final int PANEL_SERVICES     = 1;
    private static final int PANEL_BOOK         = 2;
    private static final int PANEL_APPOINTMENTS = 3;
    private static final int PANEL_PROFILE      = 4;

    private int currentPanel = PANEL_HOME;

    // ── Core layout refs ─────────────────────────────────────────────────────
    private DrawerLayout drawerLayout;

    // Topbar
    private ImageView  btnHamburger;
    private ImageView  btnBack;          // back arrow (hidden on Home)
    private TextView   tvTopbarTitle;    // panel title (hidden on Home)
    private View       topbarLogoGroup;  // logo + "MAESTRO" group (shown on Home)
    private ImageView  btnProfile;

    // Home sections (hidden when a non-home panel is active)
    private View sectionHero;
    private View statsBar;
    private View sectionFeatures;
    private View sectionAbout;
    private View sectionCta;
    private View sectionContact;

    // Panels
    private View panelServices;
    private View panelBook;
    private View panelAppointments;
    private View panelProfile;

    // Bottom nav items (for label colour toggling)
    private LinearLayout navHome;
    private LinearLayout navServices;
    private LinearLayout navBook;
    private LinearLayout navAppointments;
    private LinearLayout navProfile;

    // ── Session / DB ─────────────────────────────────────────────────────────
    private SessionManager session;
    private DatabaseHelper  db;

    // ── Book panel fields (mirrors BookActivity exactly) ─────────────────────
    private Spinner      spinnerService;
    private Spinner      spinnerCarModel;
    private Spinner      spinnerYearModel;
    private RadioGroup   rgFuelType;
    private EditText     etCarPlate;
    private EditText     etDate;
    private Button       btnPickDate;
    private TextView     tvSelectedDate;
    private RadioGroup   rgTimeSlot;
    private LinearLayout layoutTimeSlot;
    private CheckBox     cbOilCheck, cbCarWash, cbInspection;
    private RatingBar    ratingBar;
    private TextView     tvTotal, tvRatingLabel;
    private CheckBox     cbConcernEngine, cbConcernBrakes, cbConcernAircon,
            cbConcernElectrical, cbConcernTires, cbConcernOil,
            cbConcernSteering, cbConcernExhaust;
    private EditText     etAdditionalNotes;
    private LinearLayout layoutConcernSummary;
    private TextView     tvConcernSummaryText;
    private TextView     tvCarPlateError;

    private List<Service> services;
    private double bookBasePrice = 0;

    private static final double PRICE_OIL_CHECK  = 0;
    private static final double PRICE_CAR_WASH   = 150;
    private static final double PRICE_INSPECTION = 250;

    private static final java.util.regex.Pattern PLATE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z]{2,3}\\s[0-9]{4}$");

    private static final String[] CAR_MODELS = {
            "— Select car model —",
            "Toyota", "Honda", "Mitsubishi", "Nissan", "Ford",
            "Hyundai", "Kia", "Suzuki", "Isuzu", "Mazda",
            "BMW", "Mercedes-Benz", "Chevrolet", "Subaru", "Other"
    };

    private static final String[] YEAR_MODELS;
    static {
        int currentYear = 2025;
        int startYear   = 2000;
        int count       = currentYear - startYear + 2;
        YEAR_MODELS = new String[count];
        YEAR_MODELS[0] = "— Select year model —";
        for (int i = 1; i < count; i++) {
            YEAR_MODELS[i] = String.valueOf(currentYear - i + 1);
        }
    }

    // ── Appointments panel ───────────────────────────────────────────────────
    private TextView     tvNotifBadge;

    // ── Profile panel ────────────────────────────────────────────────────────
    private TextView tvProfileName, tvProfileUsername;
    private TextView tvProfileApprovedCount, tvProfilePendingCount;
    private TextView tvProfileDob, tvProfilePhone, tvProfileEmail;
    private TextView tvProfileCarType, tvProfilePlate;
    private TextView tvProfileDlNo, tvProfileDlExpiry;
    private View     tabPersonalInfoUnderline, tabOtherInfoUnderline;
    private TextView tabPersonalInfoLabel, tabOtherInfoLabel;
    private View     layoutPersonalInfo, layoutOtherInfo;

    // Other Info fields
    private TextView tvOtherCarBrand, tvOtherYearModel, tvOtherFuelType, tvOtherPlate;
    private TextView tvOtherUsername, tvOtherRole, tvOtherMemberSince;

    // ══════════════════════════════════════════════════════════════════════════
    //  onCreate
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);

        // ── Bind layout refs ─────────────────────────────────────────────────
        drawerLayout     = findViewById(R.id.drawerLayout);
        btnHamburger     = findViewById(R.id.btnHamburger);
        btnProfile       = findViewById(R.id.btnProfile);
        topbarLogoGroup  = findViewById(R.id.topbarLogoGroup);
        tvTopbarTitle    = findViewById(R.id.tvTopbarTitle);

        // Back arrow + title (we add these dynamically since original topbar
        // doesn't have them — we re-use btnHamburger slot for the arrow and
        // the logo-group slot for the title by swapping visibility)
        // The topbar in the layout is:
        //   [btnHamburger] [spacer] [logoGroup] [spacer] [btnProfile]
        // On non-Home panels we:
        //   • hide btnHamburger, show btnBack (same position, same id slot)
        //   • hide topbarLogoGroup, show tvTopbarTitle
        // We achieve this by treating btnHamburger as the back button too
        // (swapping its drawable and listener) — simpler, no layout change needed.

        // Home sections
        sectionHero     = findViewById(R.id.sectionHero);
        statsBar        = findViewById(R.id.statsBar);
        sectionFeatures = findViewById(R.id.sectionFeatures);
        sectionAbout    = findViewById(R.id.sectionAbout);
        sectionCta      = findViewById(R.id.sectionCta);
        sectionContact  = findViewById(R.id.sectionContact);

        // Panels
        panelServices     = findViewById(R.id.panelServices);
        panelBook         = findViewById(R.id.panelBook);
        panelAppointments = findViewById(R.id.panelAppointments);
        panelProfile      = findViewById(R.id.panelProfile);

        // Bottom nav
        navHome         = findViewById(R.id.navHome);
        navServices     = findViewById(R.id.navServices);
        navBook         = findViewById(R.id.navBook);
        navAppointments = findViewById(R.id.navAppointments);
        navProfile      = findViewById(R.id.navProfile);

        // ── Greeting & name ──────────────────────────────────────────────────
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12)      tvGreeting.setText("GOOD MORNING! \uD83D\uDC4B");
        else if (hour < 18) tvGreeting.setText("GOOD AFTERNOON! \uD83D\uDC4B");
        else                tvGreeting.setText("GOOD EVENING! \uD83D\uDC4B");

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        String fullName  = session.getFullName();
        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0] : (fullName != null ? fullName : "User");
        tvWelcome.setText(firstName + "!");

        // ── Drawer header ────────────────────────────────────────────────────
        TextView drawerUserName  = findViewById(R.id.drawerUserName);
        TextView drawerUserEmail = findViewById(R.id.drawerUserEmail);
        drawerUserName.setText(fullName != null ? fullName : "User");
        drawerUserEmail.setText(session.getUsername());

        // ── Topbar wiring ────────────────────────────────────────────────────
        // On Home: hamburger opens drawer. On sub-panels: acts as back arrow.
        btnHamburger.setOnClickListener(v -> {
            if (currentPanel == PANEL_HOME) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                showPanel(PANEL_HOME);
            }
        });
        btnProfile.setOnClickListener(v -> openNotificationsDialog());

        // ── Hero CTA buttons ─────────────────────────────────────────────────
        Button btnBook     = findViewById(R.id.btnBook);
        Button btnServices = findViewById(R.id.btnServices);
        btnBook.setOnClickListener(v -> showPanel(PANEL_BOOK));
        btnServices.setOnClickListener(v -> showPanel(PANEL_SERVICES));

        // ── Bottom nav ───────────────────────────────────────────────────────
        navHome.setOnClickListener(v         -> showPanel(PANEL_HOME));
        navServices.setOnClickListener(v     -> showPanel(PANEL_SERVICES));
        navBook.setOnClickListener(v         -> showPanel(PANEL_BOOK));
        navAppointments.setOnClickListener(v -> showPanel(PANEL_APPOINTMENTS));
        navProfile.setOnClickListener(v      -> showPanel(PANEL_PROFILE));

        // ── Drawer nav items ─────────────────────────────────────────────────
        findViewById(R.id.drawerNavHome).setOnClickListener(v -> {
            showPanel(PANEL_HOME);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawerNavServices).setOnClickListener(v -> {
            showPanel(PANEL_SERVICES);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawerNavBook).setOnClickListener(v -> {
            showPanel(PANEL_BOOK);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawerNavAppointments).setOnClickListener(v -> {
            showPanel(PANEL_APPOINTMENTS);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawerNavProfile).setOnClickListener(v -> {
            showPanel(PANEL_PROFILE);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawerNavLogout).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            session.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // ── Hidden stubs (kept to avoid layout crash) ────────────────────────
        Button btnAppointments = findViewById(R.id.btnAppointments);
        Button btnSearch       = findViewById(R.id.btnSearch);
        Button btnAbout        = findViewById(R.id.btnAbout);
        Button btnLogout       = findViewById(R.id.btnLogout);
        btnAppointments.setOnClickListener(v -> showPanel(PANEL_APPOINTMENTS));
        btnSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        btnAbout.setOnClickListener(v -> showPanel(PANEL_PROFILE));
        btnLogout.setOnClickListener(v -> {
            session.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // ── Bind Book panel views ─────────────────────────────────────────────
        bindBookPanelViews();
        setupBookPanel();

        // ── Bind Services panel card clicks ───────────────────────────────────
        setupServicesPanel();

        // ── Bind Appointments panel ───────────────────────────────────────────
        bindAppointmentsPanelViews();

        // ── Bind Profile panel ────────────────────────────────────────────────
        bindProfilePanelViews();

        // ── Start on Home ─────────────────────────────────────────────────────
        showPanel(PANEL_HOME);

        // ── Handle deep-link into Book with a pre-selected service ────────────
        String preService = getIntent().getStringExtra("service_name");
        if (preService != null) {
            showPanel(PANEL_BOOK);
            preselectService(preService);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  showPanel — single switch for all panels
    // ══════════════════════════════════════════════════════════════════════════

    private void showPanel(int panelId) {
        currentPanel = panelId;

        // Hide everything first
        sectionHero.setVisibility(View.GONE);
        statsBar.setVisibility(View.GONE);
        sectionFeatures.setVisibility(View.GONE);
        sectionAbout.setVisibility(View.GONE);
        sectionCta.setVisibility(View.GONE);
        sectionContact.setVisibility(View.GONE);
        panelServices.setVisibility(View.GONE);
        panelBook.setVisibility(View.GONE);
        panelAppointments.setVisibility(View.GONE);
        panelProfile.setVisibility(View.GONE);

        // Reset bottom nav label colours
        setNavLabelColor(navHome,         R.color.muted);
        setNavLabelColor(navServices,     R.color.muted);
        setNavLabelColor(navBook,         R.color.muted);
        setNavLabelColor(navAppointments, R.color.muted);
        setNavLabelColor(navProfile,      R.color.muted);

        // Scroll back to top
        View bodyScroll = findViewById(R.id.bodyScroll);
        if (bodyScroll instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) bodyScroll).scrollTo(0, 0);
        }

        switch (panelId) {
            case PANEL_HOME:
                sectionHero.setVisibility(View.VISIBLE);
                statsBar.setVisibility(View.VISIBLE);
                sectionFeatures.setVisibility(View.VISIBLE);
                sectionAbout.setVisibility(View.VISIBLE);
                sectionCta.setVisibility(View.VISIBLE);
                sectionContact.setVisibility(View.VISIBLE);
                setNavLabelColor(navHome, R.color.yellow);
                adaptTopbarForHome();
                break;

            case PANEL_SERVICES:
                panelServices.setVisibility(View.VISIBLE);
                setNavLabelColor(navServices, R.color.yellow);
                adaptTopbarForPanel("Services");
                break;

            case PANEL_BOOK:
                panelBook.setVisibility(View.VISIBLE);
                setNavLabelColor(navBook, R.color.yellow);
                adaptTopbarForPanel("Book Appointment");
                preFillCarPlate();
                break;

            case PANEL_APPOINTMENTS:
                panelAppointments.setVisibility(View.VISIBLE);
                setNavLabelColor(navAppointments, R.color.yellow);
                adaptTopbarForPanel("My Appointments");
                loadAppointmentsPanel();
                break;

            case PANEL_PROFILE:
                panelProfile.setVisibility(View.VISIBLE);
                setNavLabelColor(navProfile, R.color.yellow);
                adaptTopbarForPanel("Profile");
                loadProfilePanel();
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Topbar adaption
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Home topbar: hamburger icon (opens drawer) + centred logo.
     */
    private void adaptTopbarForHome() {
        btnHamburger.setImageResource(R.drawable.ic_menu);
        btnHamburger.setContentDescription("Open navigation drawer");
        topbarLogoGroup.setVisibility(View.VISIBLE);
        if (tvTopbarTitle != null) tvTopbarTitle.setVisibility(View.GONE);
    }

    /**
     * Non-home topbar: back arrow + panel title centred in place of logo.
     */
    private void adaptTopbarForPanel(String title) {
        btnHamburger.setImageResource(R.drawable.ic_arrow_back);
        btnHamburger.setContentDescription("Go back");
        topbarLogoGroup.setVisibility(View.GONE);
        if (tvTopbarTitle != null) {
            tvTopbarTitle.setText(title);
            tvTopbarTitle.setVisibility(View.VISIBLE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Bottom nav label colour helper
    // ══════════════════════════════════════════════════════════════════════════

    private void setNavLabelColor(LinearLayout navItem, int colorRes) {
        // The label TextView is the second child (index 1) of each nav LinearLayout.
        // navBook has a nested LinearLayout as child 0 then TextView as child 1.
        if (navItem.getChildCount() >= 2) {
            View child = navItem.getChildAt(navItem.getChildCount() - 1);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(getColor(colorRes));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SERVICES PANEL — card click wiring
    // ══════════════════════════════════════════════════════════════════════════

    private void setupServicesPanel() {
        // Map each service card ID → service name (matches ServiceData.java order)
        int[] cardIds = {
                R.id.cardOilChange, R.id.cardBrakePad, R.id.cardAirCon,
                R.id.cardEngineTuneUp, R.id.cardWheelAlignment, R.id.cardTransmission,
                R.id.cardBattery, R.id.cardCoolant, R.id.cardTimingBelt,
                R.id.cardSuspension, R.id.cardExhaust, R.id.cardDetailing
        };
        String[] serviceNames = {
                "Oil Change & Filter", "Brake Pad Replacement", "Air-Con Regas & Check",
                "Engine Tune-Up", "Wheel Alignment & Balance", "Transmission Service",
                "Battery Replacement", "Coolant System Flush", "Timing Belt Replacement",
                "Suspension Check & Repair", "Exhaust System Repair", "Full Car Detailing"
        };

        for (int i = 0; i < cardIds.length; i++) {
            final String svcName = serviceNames[i];
            View card = findViewById(cardIds[i]);
            if (card != null) {
                card.setOnClickListener(v -> {
                    showPanel(PANEL_BOOK);
                    preselectService(svcName);
                });
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOK PANEL — full BookActivity logic embedded
    // ══════════════════════════════════════════════════════════════════════════

    private void bindBookPanelViews() {
        spinnerService      = findViewById(R.id.spinnerService);
        spinnerCarModel     = findViewById(R.id.spinnerCarModel);
        spinnerYearModel    = findViewById(R.id.spinnerYearModel);
        rgFuelType          = findViewById(R.id.rgFuelType);
        etCarPlate          = findViewById(R.id.etCarPlate);
        etDate              = findViewById(R.id.etDate);
        btnPickDate         = findViewById(R.id.btnPickDate);
        tvSelectedDate      = findViewById(R.id.tvSelectedDate);
        rgTimeSlot          = findViewById(R.id.rgTimeSlot);
        layoutTimeSlot      = findViewById(R.id.layoutTimeSlot);
        cbOilCheck          = findViewById(R.id.cbOilCheck);
        cbCarWash           = findViewById(R.id.cbCarWash);
        cbInspection        = findViewById(R.id.cbInspection);
        ratingBar           = findViewById(R.id.ratingBar);
        tvTotal             = findViewById(R.id.tvTotal);
        tvRatingLabel       = findViewById(R.id.tvRatingLabel);
        cbConcernEngine     = findViewById(R.id.cbConcernEngine);
        cbConcernBrakes     = findViewById(R.id.cbConcernBrakes);
        cbConcernAircon     = findViewById(R.id.cbConcernAircon);
        cbConcernElectrical = findViewById(R.id.cbConcernElectrical);
        cbConcernTires      = findViewById(R.id.cbConcernTires);
        cbConcernOil        = findViewById(R.id.cbConcernOil);
        cbConcernSteering   = findViewById(R.id.cbConcernSteering);
        cbConcernExhaust    = findViewById(R.id.cbConcernExhaust);
        etAdditionalNotes   = findViewById(R.id.etAdditionalNotes);
        layoutConcernSummary = findViewById(R.id.layoutConcernSummary);
        tvConcernSummaryText = findViewById(R.id.tvConcernSummaryText);
        tvCarPlateError      = findViewById(R.id.tvCarPlateError);
    }

    private void setupBookPanel() {
        services = ServiceData.getAll();

        setupCarModelSpinner();
        setupYearModelSpinner();
        setupServiceSpinner();
        setupCalendarPicker();
        setupTimeSlotRadio();
        setupBookCheckBoxes();
        setupInspectionChecklist();
        setupRatingBar();
        setupCarPlateValidation();

        Button btnBookNow = findViewById(R.id.btnBookNow);
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> attemptBooking());
        }
    }

    // ── Spinners ─────────────────────────────────────────────────────────────

    private void setupCarModelSpinner() {
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CAR_MODELS);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCarModel.setAdapter(a);
    }

    private void setupYearModelSpinner() {
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, YEAR_MODELS);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearModel.setAdapter(a);
    }

    private void setupServiceSpinner() {
        String[] names = new String[services.size() + 1];
        names[0] = "— Select a service —";
        for (int i = 0; i < services.size(); i++) {
            names[i + 1] = services.get(i).name + "  ₱" +
                    String.format("%.2f", services.get(i).price);
        }
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(a);

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                bookBasePrice = (pos > 0) ? services.get(pos - 1).price : 0;
                updateBookTotal();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void preselectService(String name) {
        if (services == null) return;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).name.equals(name)) {
                spinnerService.setSelection(i + 1);
                break;
            }
        }
    }

    // ── Date picker ──────────────────────────────────────────────────────────

    private void setupCalendarPicker() {
        View layoutSelectedDate = findViewById(R.id.layoutSelectedDate);
        if (layoutSelectedDate != null) {
            layoutSelectedDate.setOnClickListener(v -> openDatePicker());
        }
        btnPickDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);

        android.app.DatePickerDialog.OnDateSetListener onDateSet = (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d);
            java.text.SimpleDateFormat disp =
                    new java.text.SimpleDateFormat("EEEE, MMMM d yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat stor =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            tvSelectedDate.setText(disp.format(sel.getTime()));
            tvSelectedDate.setTextColor(getColor(R.color.yellow));
            etDate.setText(stor.format(sel.getTime()));
        };

        Calendar today = Calendar.getInstance();
        android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(
                this, onDateSet,
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH));

        dlg.getDatePicker().setMinDate(today.getTimeInMillis());
        Calendar maxCal = Calendar.getInstance();
        maxCal.set(todayYear + 1, Calendar.DECEMBER, 31);
        dlg.getDatePicker().setMaxDate(maxCal.getTimeInMillis());

        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etDate.getWindowToken(), 0);

        dlg.show();
    }

    // ── Time slot ────────────────────────────────────────────────────────────

    private void setupTimeSlotRadio() {
        rgTimeSlot.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbMorning) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A2A1A"));
                Toast.makeText(this, "Morning slot — background: dark green", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.rbAfternoon) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#1A1A2A"));
                Toast.makeText(this, "Afternoon slot — background: dark blue", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.rbEarlyBird) {
                layoutTimeSlot.setBackgroundColor(Color.parseColor("#2A1A00"));
                Toast.makeText(this, "Early Bird slot — background: dark amber", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Add-on checkboxes ────────────────────────────────────────────────────

    private void setupBookCheckBoxes() {
        cbOilCheck.setOnCheckedChangeListener((btn, chk) -> {
            cbOilCheck.setTextColor(chk ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateBookTotal();
        });
        cbCarWash.setOnCheckedChangeListener((btn, chk) -> {
            cbCarWash.setTextColor(chk ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateBookTotal();
        });
        cbInspection.setOnCheckedChangeListener((btn, chk) -> {
            cbInspection.setTextColor(chk ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateBookTotal();
        });
    }

    // ── Inspection checklist ─────────────────────────────────────────────────

    private void setupInspectionChecklist() {
        CheckBox[] concerns = {
                cbConcernEngine, cbConcernBrakes, cbConcernAircon, cbConcernElectrical,
                cbConcernTires, cbConcernOil, cbConcernSteering, cbConcernExhaust
        };
        android.widget.CompoundButton.OnCheckedChangeListener listener = (btn, chk) -> {
            btn.setTextColor(chk ? getColor(R.color.yellow) : getColor(R.color.muted));
            updateConcernSummary(concerns);
        };
        for (CheckBox cb : concerns) cb.setOnCheckedChangeListener(listener);

        etAdditionalNotes.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updateConcernSummary(concerns);
            }
        });
    }

    private void updateConcernSummary(CheckBox[] concerns) {
        java.util.List<String> sel = new java.util.ArrayList<>();
        for (CheckBox cb : concerns) if (cb.isChecked()) sel.add(cb.getText().toString());
        String notes    = etAdditionalNotes.getText().toString().trim();
        boolean hasContent = !sel.isEmpty() || !notes.isEmpty();
        layoutConcernSummary.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        if (hasContent) {
            StringBuilder sb = new StringBuilder();
            if (!sel.isEmpty()) {
                sb.append("Selected concerns:\n");
                for (String s : sel) sb.append("  • ").append(s).append("\n");
            }
            if (!notes.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Additional notes:\n  ").append(notes);
            }
            tvConcernSummaryText.setText(sb.toString().trim());
        }
    }

    private String getSelectedConcerns() {
        CheckBox[] concerns = {
                cbConcernEngine, cbConcernBrakes, cbConcernAircon, cbConcernElectrical,
                cbConcernTires, cbConcernOil, cbConcernSteering, cbConcernExhaust
        };
        java.util.List<String> sel = new java.util.ArrayList<>();
        for (CheckBox cb : concerns) if (cb.isChecked()) sel.add(cb.getText().toString());
        return android.text.TextUtils.join(", ", sel);
    }

    // ── Rating bar ───────────────────────────────────────────────────────────

    private void setupRatingBar() {
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            String[] labels = {"", "Poor 😞", "Fair 😐", "Good 🙂", "Great 😊", "Excellent 🌟"};
            int idx = (int) rating;
            tvRatingLabel.setText(idx > 0 ? labels[idx] : "Tap a star to rate");
            tvRatingLabel.setTextColor(idx >= 4
                    ? getColor(R.color.success) : getColor(R.color.muted));
        });
    }

    // ── Car plate validation ─────────────────────────────────────────────────

    private void setupCarPlateValidation() {
        etCarPlate.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String current = s.toString();
                String upper   = current.toUpperCase();
                if (!current.equals(upper)) {
                    etCarPlate.removeTextChangedListener(this);
                    etCarPlate.setText(upper);
                    etCarPlate.setSelection(upper.length());
                    etCarPlate.addTextChangedListener(this);
                    return;
                }
                String val = upper.trim();
                if (val.isEmpty()) {
                    tvCarPlateError.setVisibility(View.GONE);
                } else if (PLATE_PATTERN.matcher(val).matches()) {
                    tvCarPlateError.setVisibility(View.GONE);
                } else {
                    tvCarPlateError.setText("Format: 3 letters, space, 4 digits — e.g. ABC 1234");
                    tvCarPlateError.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void preFillCarPlate() {
        String plate = session.getLicensePlate();
        if (plate != null && !plate.isEmpty() && etCarPlate != null) {
            etCarPlate.setText(plate);
            etCarPlate.setSelection(plate.length());
        }
    }

    // ── Total & helpers ───────────────────────────────────────────────────────

    private double bookCalculateTotal() {
        double total = bookBasePrice;
        if (cbOilCheck.isChecked())   total += PRICE_OIL_CHECK;
        if (cbCarWash.isChecked())    total += PRICE_CAR_WASH;
        if (cbInspection.isChecked()) total += PRICE_INSPECTION;
        return total;
    }

    private void updateBookTotal() {
        tvTotal.setText("Estimated Total:  ₱" + String.format("%.2f", bookCalculateTotal()));
    }

    private String getSelectedFuelType() {
        int id = rgFuelType.getCheckedRadioButtonId();
        if (id == R.id.rbGasoline) return "Gasoline";
        if (id == R.id.rbDiesel)   return "Diesel";
        return "";
    }

    private String getSelectedTime() {
        int id = rgTimeSlot.getCheckedRadioButtonId();
        if (id == R.id.rbMorning)   return "Morning (8AM–12PM)";
        if (id == R.id.rbAfternoon) return "Afternoon (1PM–5PM)";
        if (id == R.id.rbEarlyBird) return "Early Bird (8AM–9AM)";
        return "";
    }

    // ── Booking attempt & save ────────────────────────────────────────────────

    private void attemptBooking() {
        if (spinnerCarModel.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a car model", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerYearModel.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a year model", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgFuelType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a fuel type (Gasoline or Diesel)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String plate  = etCarPlate.getText().toString().trim();
        String date   = etDate.getText().toString().trim();
        int    spinPos = spinnerService.getSelectedItemPosition();

        if (spinPos == 0) {
            Toast.makeText(this, "Please select a service", Toast.LENGTH_SHORT).show();
            return;
        }
        if (plate.isEmpty()) {
            Toast.makeText(this, "Please enter your car plate number", Toast.LENGTH_SHORT).show();
            etCarPlate.requestFocus();
            return;
        }
        if (!PLATE_PATTERN.matcher(plate.toUpperCase()).matches()) {
            tvCarPlateError.setText("Format: 3 letters, space, 4 digits — e.g. ABC 1234");
            tvCarPlateError.setVisibility(View.VISIBLE);
            etCarPlate.requestFocus();
            return;
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Please enter a preferred date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgTimeSlot.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String carModel    = CAR_MODELS[spinnerCarModel.getSelectedItemPosition()];
        String yearModel   = YEAR_MODELS[spinnerYearModel.getSelectedItemPosition()];
        String fuelType    = getSelectedFuelType();
        String concerns    = getSelectedConcerns();
        String notes       = etAdditionalNotes.getText().toString().trim();
        String serviceName = services.get(spinPos - 1).name;
        String timeSlot    = getSelectedTime();
        double total       = bookCalculateTotal();

        String concernLine = concerns.isEmpty() ? "None" : concerns;
        String notesLine   = notes.isEmpty()    ? "None" : notes;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(
                        "Car Model: " + carModel    + "\n" +
                                "Year:      " + yearModel   + "\n" +
                                "Fuel:      " + fuelType    + "\n" +
                                "Concerns:  " + concernLine + "\n" +
                                "Notes:     " + notesLine   + "\n" +
                                "Service:   " + serviceName + "\n" +
                                "Plate:     " + plate       + "\n" +
                                "Date:      " + date        + "\n" +
                                "Time:      " + timeSlot    + "\n" +
                                "Rating:    " + (int) ratingBar.getRating() + "★\n\n" +
                                "Total:     ₱" + String.format("%.2f", total)
                )
                .setPositiveButton("Book Now", (dialog, which) ->
                        saveAppointment(carModel, yearModel, fuelType,
                                concerns, notes,
                                serviceName, plate, date, timeSlot, total))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAppointment(String carModel, String yearModel, String fuelType,
                                 String concerns, String notes, String serviceName,
                                 String plate, String date, String time, double total) {
        Appointment appt     = new Appointment();
        appt.userId          = session.getUserId();
        appt.carModel        = carModel;
        appt.yearModel       = yearModel;
        appt.fuelType        = fuelType;
        appt.orcrStatus      = "N/A";
        appt.orcrImagePath   = null;
        appt.vehicleConcerns = concerns;
        appt.additionalNotes = notes;
        appt.serviceName     = serviceName;
        appt.carPlate        = plate;
        appt.date            = date;
        appt.time            = time;
        appt.totalPrice      = total;
        appt.status          = "pending";
        appt.rating          = (int) ratingBar.getRating();

        long id = db.insertAppointment(appt);
        if (id > 0) {
            session.saveLicensePlate(plate);
            new AlertDialog.Builder(this)
                    .setTitle("Booking Submitted")
                    .setMessage(
                            "Your booking request has been successfully submitted.\n\n" +
                                    "Please wait for the administrator's confirmation."
                    )
                    .setCancelable(false)
                    .setPositiveButton("Book Again", (dlg, w) -> {
                        // Reset the Book panel
                        resetBookPanel();
                    })
                    .setNegativeButton("View My Appointments", (dlg, w) -> {
                        showPanel(PANEL_APPOINTMENTS);
                    })
                    .show();
        } else {
            Toast.makeText(this, "Booking failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resets all Book panel fields back to their defaults so the user can
     * submit another booking without re-navigating.
     */
    private void resetBookPanel() {
        spinnerService.setSelection(0);
        spinnerCarModel.setSelection(0);
        spinnerYearModel.setSelection(0);
        rgFuelType.check(R.id.rbGasoline);
        rgTimeSlot.clearCheck();
        layoutTimeSlot.setBackground(null);
        etDate.setText("");
        tvSelectedDate.setText("Tap to choose a date");
        tvSelectedDate.setTextColor(getColor(R.color.muted));
        cbOilCheck.setChecked(false);
        cbCarWash.setChecked(false);
        cbInspection.setChecked(false);
        cbConcernEngine.setChecked(false);
        cbConcernBrakes.setChecked(false);
        cbConcernAircon.setChecked(false);
        cbConcernElectrical.setChecked(false);
        cbConcernTires.setChecked(false);
        cbConcernOil.setChecked(false);
        cbConcernSteering.setChecked(false);
        cbConcernExhaust.setChecked(false);
        etAdditionalNotes.setText("");
        ratingBar.setRating(0);
        tvRatingLabel.setText("Tap a star to rate");
        tvRatingLabel.setTextColor(getColor(R.color.muted));
        layoutConcernSummary.setVisibility(View.GONE);
        tvCarPlateError.setVisibility(View.GONE);
        bookBasePrice = 0;
        updateBookTotal();
        preFillCarPlate();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  APPOINTMENTS PANEL — load real data from DB
    // ══════════════════════════════════════════════════════════════════════════

    private void bindAppointmentsPanelViews() {
        tvNotifBadge = findViewById(R.id.tvNotifBadge);
        if (tvNotifBadge != null) {
            tvNotifBadge.setOnClickListener(v -> openNotificationsDialog());
        }
    }

    private void loadAppointmentsPanel() {
        // Refresh notification badge
        refreshNotifBadge();

        // Load real appointment list
        // The panelAppointments layout has static placeholder cards and tvEmpty.
        // We hide the static cards and instead inject the real adapter into the
        // existing listAppointments ListView (if the layout has one), or we
        // clear the static cards and rebuild programmatically.
        //
        // Since the layout only has static card LinearLayouts (no ListView with
        // id=listAppointments), we hide those and show the real list via a
        // dynamically-added ListView, OR we simply replace the static-card
        // container content. The simplest correct approach:
        // find the card container, clear it, and fill with the adapter rows.

        View apptCardContainer = panelAppointments.findViewWithTag("apptCardContainer");

        // We use the existing LinearLayout container (child of panelAppointments)
        // that holds the two static cards. Find it by its position.
        // More robustly: add a tag in the layout, or just find it by id.
        // The static container has no id; we'll locate it by finding the
        // LinearLayout that is a direct child of panelAppointments and contains
        // the static cards (paddingStart=14dp, paddingEnd=14dp, paddingBottom=8dp).
        // Instead of a fragile traversal, we populate the listAppointments
        // ListView by adding it programmatically below panelAppointments' header
        // if it doesn't already exist.

        // Simpler approach: use the existing tvEmpty and manipulate the static
        // cards' container. We'll find the first LinearLayout child of
        // panelAppointments that wraps the cards (index 2 after header + badge).
        // Safer: just add a ListView to the panel on first load.

        loadAppointmentsIntoPanel();
    }

    /** Injects a real ListView into panelAppointments (replaces static cards). */
    private void loadAppointmentsIntoPanel() {
        // Tag the dynamic ListView so we don't add it twice
        final String TAG_LISTVIEW = "dynamic_appt_list";

        LinearLayout panelLL = (LinearLayout) panelAppointments;

        // Check if we already added the dynamic ListView
        View existingList = panelLL.findViewWithTag(TAG_LISTVIEW);
        if (existingList != null) {
            // Already inserted — just refresh the adapter
            refreshAppointmentList((ListView) existingList);
            return;
        }

        // Hide the static-card container (it is the 3rd child, index 2, after
        // section-header LinearLayout and tvNotifBadge).
        // We identify it by child count: find the first child LinearLayout that
        // holds the static cards.
        for (int i = 0; i < panelLL.getChildCount(); i++) {
            View child = panelLL.getChildAt(i);
            // The cards container is a LinearLayout with paddingBottom set
            // and paddingStart=14dp. It contains the two static appointment cards.
            if (child instanceof LinearLayout && child.getId() == View.NO_ID) {
                LinearLayout ll = (LinearLayout) child;
                if (ll.getChildCount() >= 2) {
                    ll.setVisibility(View.GONE);
                    break;
                }
            }
        }

        // Create and add a dynamic ListView
        ListView listView = new ListView(this);
        listView.setTag(TAG_LISTVIEW);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        listView.setLayoutParams(params);

        // Insert before tvEmpty (last meaningful child) and spacer
        // Insert it at position 2 (after header + badge)
        int insertAt = Math.min(2, panelLL.getChildCount());
        panelLL.addView(listView, insertAt);

        refreshAppointmentList(listView);
    }

    private void refreshAppointmentList(ListView listView) {
        List<Appointment> appointments = db.getAppointmentsByUser(session.getUserId());
        TextView tvEmpty = panelAppointments.findViewById(R.id.tvEmpty);

        if (appointments.isEmpty()) {
            listView.setVisibility(View.GONE);
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
        } else {
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            com.maestro.autoworks.adapters.AppointmentAdapter adapter =
                    new com.maestro.autoworks.adapters.AppointmentAdapter(this, appointments);
            listView.setAdapter(adapter);
            // Make ListView show all rows without internal scrolling
            setListViewHeightBasedOnItems(listView);
        }
    }

    /** Expands ListView to show all rows (the outer NestedScrollView handles scrolling). */
    private void setListViewHeightBasedOnItems(ListView lv) {
        ListAdapter adapter = lv.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, lv);
            item.measure(
                    View.MeasureSpec.makeMeasureSpec(lv.getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        android.view.ViewGroup.LayoutParams params = lv.getLayoutParams();
        params.height = totalHeight + (lv.getDividerHeight() * (adapter.getCount() - 1));
        lv.setLayoutParams(params);
        lv.requestLayout();
    }

    private void refreshNotifBadge() {
        if (tvNotifBadge == null) return;
        int unread = db.countUnreadNotifications(session.getUserId());
        if (unread > 0) {
            tvNotifBadge.setVisibility(View.VISIBLE);
            tvNotifBadge.setText("\uD83D\uDD14 " + unread + " new notification" + (unread > 1 ? "s" : ""));
        } else {
            tvNotifBadge.setVisibility(View.GONE);
        }
    }

    private void openNotificationsDialog() {
        List<AppNotification> notifications = db.getNotificationsForUser(session.getUserId());
        if (notifications.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Notifications")
                    .setMessage("You have no notifications yet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] items = new String[notifications.size()];
        for (int i = 0; i < notifications.size(); i++) {
            AppNotification n = notifications.get(i);
            items[i] = (n.isRead ? "   " : "\uD83D\uDD35 ") + n.title + "\n      " + n.createdAt;
        }
        new AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setItems(items, (dlg, which) ->
                        openNotificationDetail(notifications.get(which)))
                .setNeutralButton("Mark All Read", (d, w) -> {
                    db.markAllNotificationsRead(session.getUserId());
                    refreshNotifBadge();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openNotificationDetail(AppNotification notif) {
        db.markNotificationRead(notif.id);
        refreshNotifBadge();
        new AlertDialog.Builder(this)
                .setTitle(notif.title)
                .setMessage(notif.message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROFILE PANEL — load real user data from DB
    // ══════════════════════════════════════════════════════════════════════════

    private void bindProfilePanelViews() {
        tvProfileName          = panelProfile.findViewById(R.id.tvProfileName);
        tvProfileUsername      = panelProfile.findViewById(R.id.tvProfileUsername);
        tvProfileApprovedCount = panelProfile.findViewById(R.id.tvProfileApprovedCount);
        tvProfilePendingCount  = panelProfile.findViewById(R.id.tvProfilePendingCount);
        tvProfileDob           = panelProfile.findViewById(R.id.tvProfileDob);
        tvProfilePhone         = panelProfile.findViewById(R.id.tvProfilePhone);
        tvProfileEmail         = panelProfile.findViewById(R.id.tvProfileEmail);
        tvProfileCarType       = panelProfile.findViewById(R.id.tvProfileCarType);
        tvProfilePlate         = panelProfile.findViewById(R.id.tvProfilePlate);
        tvProfileDlNo          = panelProfile.findViewById(R.id.tvProfileDlNo);
        tvProfileDlExpiry      = panelProfile.findViewById(R.id.tvProfileDlExpiry);

        tabPersonalInfoUnderline = panelProfile.findViewById(R.id.tabPersonalInfoUnderline);
        tabOtherInfoUnderline    = panelProfile.findViewById(R.id.tabOtherInfoUnderline);

        layoutPersonalInfo = panelProfile.findViewById(R.id.layoutPersonalInfo);
        layoutOtherInfo    = panelProfile.findViewById(R.id.layoutOtherInfo);

        // Other Info fields
        tvOtherCarBrand    = panelProfile.findViewById(R.id.tvOtherCarBrand);
        tvOtherYearModel   = panelProfile.findViewById(R.id.tvOtherYearModel);
        tvOtherFuelType    = panelProfile.findViewById(R.id.tvOtherFuelType);
        tvOtherPlate       = panelProfile.findViewById(R.id.tvOtherPlate);
        tvOtherUsername    = panelProfile.findViewById(R.id.tvOtherUsername);
        tvOtherRole        = panelProfile.findViewById(R.id.tvOtherRole);
        tvOtherMemberSince = panelProfile.findViewById(R.id.tvOtherMemberSince);

        // Tab labels (for colour toggling)
        View tabPersonalInfo = panelProfile.findViewById(R.id.tabPersonalInfo);
        View tabOtherInfo    = panelProfile.findViewById(R.id.tabOtherInfo);

        if (tabPersonalInfo != null) {
            tabPersonalInfo.setOnClickListener(v -> switchProfileTab(true));
        }
        if (tabOtherInfo != null) {
            tabOtherInfo.setOnClickListener(v -> switchProfileTab(false));
        }

    }

    private void switchProfileTab(boolean showPersonal) {
        if (showPersonal) {
            layoutPersonalInfo.setVisibility(View.VISIBLE);
            layoutOtherInfo.setVisibility(View.GONE);
            tabPersonalInfoUnderline.setBackgroundColor(getColor(R.color.yellow));
            tabOtherInfoUnderline.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            layoutPersonalInfo.setVisibility(View.GONE);
            layoutOtherInfo.setVisibility(View.VISIBLE);
            tabOtherInfoUnderline.setBackgroundColor(getColor(R.color.yellow));
            tabPersonalInfoUnderline.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void loadProfilePanel() {
        // Always start on Personal Info tab when profile is opened
        switchProfileTab(true);

        // Header: name + username from session
        String fullName = session.getFullName();
        if (tvProfileName != null)     tvProfileName.setText(fullName != null ? fullName : "—");
        if (tvProfileUsername != null)  tvProfileUsername.setText(session.getUsername());

        // Appointment stats
        List<Appointment> appts = db.getAppointmentsByUser(session.getUserId());
        int approved = 0, pending = 0;
        for (Appointment a : appts) {
            if ("approved".equalsIgnoreCase(a.status) || "confirmed".equalsIgnoreCase(a.status)) approved++;
            else if ("pending".equalsIgnoreCase(a.status)) pending++;
        }
        if (tvProfileApprovedCount != null) tvProfileApprovedCount.setText(String.valueOf(approved));
        if (tvProfilePendingCount  != null) tvProfilePendingCount.setText(String.valueOf(pending));

        // Deep user data from DB
        User user = db.getUserById(session.getUserId());

        String dash = "—";

        if (user != null) {
            if (tvProfileDob != null)
                tvProfileDob.setText(notEmpty(user.birthdate) ? user.birthdate : dash);
            if (tvProfilePhone != null)
                tvProfilePhone.setText(notEmpty(user.phone) ? user.phone : dash);
            if (tvProfileEmail != null)
                tvProfileEmail.setText(notEmpty(user.email) ? user.email : dash);

            String make  = notEmpty(user.vehicleMake)  ? user.vehicleMake  : "";
            String model = notEmpty(user.vehicleModel) ? user.vehicleModel : "";
            String carType = (make + " " + model).trim();
            if (tvProfileCarType != null)
                tvProfileCarType.setText(notEmpty(carType) ? carType : dash);

            String plate = notEmpty(user.licensePlate) ? user.licensePlate : session.getLicensePlate();
            if (tvProfilePlate != null)
                tvProfilePlate.setText(notEmpty(plate) ? plate : dash);
            if (tvProfileDlNo != null)
                tvProfileDlNo.setText(notEmpty(user.driversLicenseNo) ? user.driversLicenseNo : dash);
            if (tvProfileDlExpiry != null)
                tvProfileDlExpiry.setText(notEmpty(user.driversLicenseExpiry) ? user.driversLicenseExpiry : dash);

            // ── Other Info tab data ──
            // Vehicle info: use last booking's car model/year/fuel if available
            String carBrand = notEmpty(user.vehicleMake) ? user.vehicleMake : dash;
            String yearModel = dash;
            String fuelType  = dash;
            String plateOther = notEmpty(user.licensePlate) ? user.licensePlate
                    : (notEmpty(session.getLicensePlate()) ? session.getLicensePlate() : dash);

            // Pull from most recent appointment if user fields are empty
            if (!appts.isEmpty()) {
                Appointment latest = appts.get(0);
                if (!notEmpty(user.vehicleMake) && notEmpty(latest.carModel))
                    carBrand = latest.carModel;
                if (notEmpty(latest.yearModel))
                    yearModel = latest.yearModel;
                if (notEmpty(latest.fuelType))
                    fuelType = latest.fuelType;
                if (!notEmpty(user.licensePlate) && notEmpty(latest.carPlate))
                    plateOther = latest.carPlate;
            }

            if (tvOtherCarBrand    != null) tvOtherCarBrand.setText(carBrand);
            if (tvOtherYearModel   != null) tvOtherYearModel.setText(yearModel);
            if (tvOtherFuelType    != null) tvOtherFuelType.setText(fuelType);
            if (tvOtherPlate       != null) tvOtherPlate.setText(plateOther);
        }

        // Account info (always available from session)
        if (tvOtherUsername != null)
            tvOtherUsername.setText(notEmpty(session.getUsername()) ? session.getUsername() : dash);
        if (tvOtherRole != null) {
            String role = (user != null && notEmpty(user.role))
                    ? capitalize(user.role) : "Customer";
            tvOtherRole.setText(role);
        }

        // Member Since — no createdAt on User model, show current month/year
        if (tvOtherMemberSince != null) {
            String memberSince = new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Calendar.getInstance().getTime());
            tvOtherMemberSince.setText(memberSince);
        }
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Back press
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentPanel != PANEL_HOME) {
            showPanel(PANEL_HOME);
        } else {
            super.onBackPressed();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  onResume — refresh badge if appointments panel is showing
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onResume() {
        super.onResume();
        if (currentPanel == PANEL_APPOINTMENTS) {
            refreshNotifBadge();
        }
    }
}