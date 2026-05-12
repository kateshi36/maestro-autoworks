package com.maestro.autoworks.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.maestro.autoworks.R;

/**
 * WebViewActivity — Embedded browser for the Maestro Autoworks website.
 * ─────────────────────────────────────────────────────────────────────────────
 * Accepts extras:
 *   • EXTRA_URL   (String) — URL to load (defaults to MAESTRO_URL)
 *   • EXTRA_TITLE (String) — toolbar title override
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class WebViewActivity extends AppCompatActivity {

    /** Intent extra keys — use these when starting this Activity. */
    public static final String EXTRA_URL   = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    /** Default URL — replace with your deployed website address. */
    private static final String MAESTRO_URL = "https://maestroautoworks.com";

    private WebView     webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView     = findViewById(R.id.webView);
        Button   btnBack    = findViewById(R.id.btnWebBack);
        Button   btnRefresh = findViewById(R.id.btnWebRefresh);
        TextView tvTitle    = findViewById(R.id.tvWebTitle);
        progressBar         = findViewById(R.id.webProgressBar);

        // Resolve URL and title from intent extras or defaults
        String url   = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (url   == null || url.isEmpty())   url   = MAESTRO_URL;
        if (title == null || title.isEmpty()) title = "Maestro Autoworks";
        tvTitle.setText(title);

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Progress + title tracking
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onReceivedTitle(WebView view, String pageTitle) {
                if (pageTitle != null && !pageTitle.isEmpty()) tvTitle.setText(pageTitle);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                String t = view.getTitle();
                if (t != null && !t.isEmpty()) tvTitle.setText(t);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                // Only show error for the main frame
                if (request.isForMainFrame()) {
                    view.loadData(
                        "<html><body style='background:#111;color:#aaa;font-family:sans-serif;"
                        + "text-align:center;padding:40px'>"
                        + "<h2 style='color:#f5a623'>&#9888; Could Not Load Page</h2>"
                        + "<p>Make sure you are connected to the internet and the website URL is correct.</p>"
                        + "<p style='font-size:12px;color:#555'>" + request.getUrl() + "</p>"
                        + "</body></html>",
                        "text/html", "UTF-8");
                }
            }
        });

        webView.loadUrl(url);

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
