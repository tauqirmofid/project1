package com.example.unimate;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        WebView webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript for interactive map

        webView.setWebViewClient(new WebViewClient()); // Keep navigation inside WebView

        // Load Google Maps with the location
        String mapUrl = "https://www.google.com/maps/place/Leading+University/@24.8693875,91.8049219,17z";
        webView.loadUrl(mapUrl);
    }
}
