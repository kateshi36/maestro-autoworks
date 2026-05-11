package com.maestro.autoworks.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;

/**
 * AboutActivity — App appearance settings.
 * Demonstrates: RadioButton & Background Color, CheckBox & Text Color.
 */
public class AboutActivity extends AppCompatActivity {

    private LinearLayout layoutRoot;
    private TextView tvPreview;
    private CheckBox cbBold, cbYellow, cbLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        layoutRoot = findViewById(R.id.layoutRoot);
        tvPreview  = findViewById(R.id.tvPreview);
        cbBold     = findViewById(R.id.cbBold);
        cbYellow   = findViewById(R.id.cbYellow);
        cbLarge    = findViewById(R.id.cbLarge);

        RadioGroup rgBg = findViewById(R.id.rgBackground);

        // ── RADIO BUTTONS: change background color ──
        rgBg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDark) {
                layoutRoot.setBackgroundColor(Color.parseColor("#0A0A0A"));
                Toast.makeText(this, "Theme: Dark #0A0A0A", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbGray) {
                layoutRoot.setBackgroundColor(Color.parseColor("#2A2A2A"));
                Toast.makeText(this, "Theme: Gray #2A2A2A", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbDarkBlue) {
                layoutRoot.setBackgroundColor(Color.parseColor("#0A0A1A"));
                Toast.makeText(this, "Theme: Dark Blue #0A0A1A", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbDarkGreen) {
                layoutRoot.setBackgroundColor(Color.parseColor("#0A1A0A"));
                Toast.makeText(this, "Theme: Dark Green #0A1A0A", Toast.LENGTH_SHORT).show();
            }
        });

        // ── CHECKBOXES: change text color & style of preview text ──
        cbBold.setOnCheckedChangeListener((btn, checked) -> {
            updatePreview();
            cbBold.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
        });
        cbYellow.setOnCheckedChangeListener((btn, checked) -> {
            updatePreview();
            cbYellow.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
        });
        cbLarge.setOnCheckedChangeListener((btn, checked) -> {
            updatePreview();
            cbLarge.setTextColor(checked
                    ? getColor(R.color.yellow) : getColor(R.color.muted));
        });
    }

    /** Update preview TextView based on checkbox state. */
    private void updatePreview() {
        // Text color
        if (cbYellow.isChecked()) {
            tvPreview.setTextColor(getColor(R.color.yellow));
        } else {
            tvPreview.setTextColor(getColor(R.color.white));
        }

        // Bold
        int style = cbBold.isChecked() ? Typeface.BOLD : Typeface.NORMAL;
        tvPreview.setTypeface(null, style);

        // Font size
        tvPreview.setTextSize(cbLarge.isChecked() ? 22f : 16f);
    }
}
