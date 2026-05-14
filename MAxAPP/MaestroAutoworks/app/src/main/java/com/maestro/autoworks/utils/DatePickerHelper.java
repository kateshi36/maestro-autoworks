package com.maestro.autoworks.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

/**
 * DatePickerHelper — Universal date-picker utility for MaestroAutoworks.
 *
 * <p>Centralises all DatePickerDialog logic so every date field in the app
 * behaves identically:
 * <ul>
 *   <li>Tapping the field hides the soft keyboard immediately.</li>
 *   <li>A native DatePickerDialog is shown, clamped to the supplied year range.</li>
 *   <li>The chosen date is written to the target EditText as {@code YYYY-MM-DD}.</li>
 *   <li>If the field already holds a valid {@code YYYY-MM-DD} value the dialog
 *       opens pre-selected to that date; otherwise it defaults to today.</li>
 * </ul>
 *
 * <h3>Usage — attach once per field in setupStepN():</h3>
 * <pre>
 *   // Birthdate  (allow ages 16-105)
 *   int todayYear = Calendar.getInstance().get(Calendar.YEAR);
 *   DatePickerHelper.attach(this, etBirthdate, 1920, todayYear - 16);
 *
 *   // Driver's licence expiry (future dates, up to 20 years out)
 *   DatePickerHelper.attach(this, etDriversExpiry, todayYear, todayYear + 20);
 *
 *   // Conductor's licence expiry
 *   DatePickerHelper.attach(this, etConductorsExpiry, todayYear, todayYear + 20);
 * </pre>
 */
public final class DatePickerHelper {

    private DatePickerHelper() { /* static utility — no instances */ }

    // ──────────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Attaches a date-picker interaction to {@code field}.
     *
     * <p>After this call the field becomes effectively non-editable by keyboard;
     * tapping it will always open the date picker instead.
     *
     * @param context  The hosting Activity or Fragment context.
     * @param field    The {@link EditText} to wire up.
     * @param minYear  Earliest selectable calendar year (inclusive).
     * @param maxYear  Latest  selectable calendar year (inclusive).
     */
    public static void attach(Context context, EditText field, int minYear, int maxYear) {
        // Prevent the soft keyboard from appearing when the field is tapped
        field.setFocusable(false);
        field.setFocusableInTouchMode(false);
        field.setClickable(true);
        field.setCursorVisible(false);

        field.setOnClickListener(v -> show(context, field, minYear, maxYear));
        // Also handle focus-gained event for accessibility / tab navigation
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) show(context, field, minYear, maxYear);
        });
    }

    /**
     * Programmatically shows the DatePickerDialog for the given field without
     * requiring the field to be focusable. Useful when you need to open the
     * picker in response to a button press rather than a field tap.
     *
     * @param context  The hosting Activity or Fragment context.
     * @param field    The {@link EditText} that will receive the date string.
     * @param minYear  Earliest selectable calendar year (inclusive).
     * @param maxYear  Latest  selectable calendar year (inclusive).
     */
    public static void show(Context context, EditText field, int minYear, int maxYear) {
        // Dismiss keyboard if it happens to be visible
        dismissKeyboard(context, field);

        Calendar cal = initialCalendar(field, minYear, maxYear);

        DatePickerDialog dialog = new DatePickerDialog(
            context,
            (picker, year, month, day) -> {
                // Validate against supplied bounds (user can sometimes scroll
                // the spinner past the min/max date in some OS versions)
                if (year < minYear || year > maxYear) {
                    Toast.makeText(context,
                        "Please pick a year between " + minYear + " and " + maxYear + ".",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                String formatted = String.format(Locale.US,
                    "%04d-%02d-%02d", year, month + 1, day);
                field.setText(formatted);
                // Move cursor to end for visual clarity
                field.setSelection(formatted.length());
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        );

        // Clamp the calendar widget's scrollable range
        Calendar minCal = Calendar.getInstance();
        minCal.set(minYear, Calendar.JANUARY, 1, 0, 0, 0);
        minCal.set(Calendar.MILLISECOND, 0);

        Calendar maxCal = Calendar.getInstance();
        maxCal.set(maxYear, Calendar.DECEMBER, 31, 23, 59, 59);
        maxCal.set(Calendar.MILLISECOND, 999);

        dialog.getDatePicker().setMinDate(minCal.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(maxCal.getTimeInMillis());

        dialog.show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a Calendar pre-set to:
     * <ol>
     *   <li>the date already stored in {@code field} (if valid YYYY-MM-DD), or</li>
     *   <li>today clamped into [minYear, maxYear].</li>
     * </ol>
     */
    private static Calendar initialCalendar(EditText field, int minYear, int maxYear) {
        Calendar cal = Calendar.getInstance();

        String existing = field.getText() != null ? field.getText().toString().trim() : "";
        if (existing.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = existing.split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based
                int d = Integer.parseInt(parts[2]);
                if (y >= minYear && y <= maxYear) {
                    cal.set(y, m, d);
                    return cal;
                }
            } catch (Exception ignored) {
                // Fall through to default
            }
        }

        // Clamp today into the valid range
        int year = cal.get(Calendar.YEAR);
        if (year < minYear) {
            cal.set(minYear, Calendar.JANUARY, 1);
        } else if (year > maxYear) {
            cal.set(maxYear, Calendar.DECEMBER, 31);
        }
        return cal;
    }

    /** Hides the soft keyboard if it is currently showing. */
    private static void dismissKeyboard(Context context, EditText field) {
        InputMethodManager imm = (InputMethodManager)
            context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
    }
}
