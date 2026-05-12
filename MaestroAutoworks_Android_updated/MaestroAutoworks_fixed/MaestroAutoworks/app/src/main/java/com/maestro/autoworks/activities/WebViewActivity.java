package com.maestro.autoworks.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;

/**
 * WebViewActivity — Embedded browser.
 * Demonstrates: WebView.
 */
public class WebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);
        Button btnBack    = findViewById(R.id.btnWebBack);
        Button btnRefresh = findViewById(R.id.btnWebRefresh);
        TextView tvTitle  = findViewById(R.id.tvWebTitle);

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                tvTitle.setText(view.getTitle() != null ? view.getTitle() : url);
            }
        });

        // Load Maestro Autoworks info page (Google Maps as fallback demo)
        webView.loadUrl("https://www.google.com/maps/search/auto+repair+quezon+city");

        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else finish();
        });

        btnRefresh.setOnClickListener(v -> webView.reload());
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
