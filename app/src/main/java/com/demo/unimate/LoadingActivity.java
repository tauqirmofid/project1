package com.demo.unimate;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LoadingActivity extends AppCompatActivity {

    private BroadcastReceiver closeReceiver;
    private ImageView logoColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Make status bar transparent
        getWindow().setStatusBarColor(
                ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().setNavigationBarColor(
                ContextCompat.getColor(this, android.R.color.transparent));

        logoColor = findViewById(R.id.logoColor);

        // 1. Register broadcast to close this LoadingActivity
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_LOADING".equals(intent.getAction())) {
                    finish();
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(closeReceiver, new IntentFilter("CLOSE_LOADING"));

        // 2. Start the fade-in animation for the color logo
        fadeInColorLogo();
    }

    // Fade color logo from alpha=0 to alpha=1 over 2 seconds
    private void fadeInColorLogo() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(logoColor, "alpha", 0f, 1f);
        animator.setDuration(2000); // 2 seconds
        animator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(closeReceiver);
    }
}

