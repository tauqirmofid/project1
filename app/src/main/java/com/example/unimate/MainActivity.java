package com.example.unimate;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the ImageView
        ImageView selectRoleTopImage = findViewById(R.id.selectRoleTopImage);

        // Load the slide-down animation
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        // Start the animation
        selectRoleTopImage.startAnimation(slideDown);

        // Find the bottom layout
        LinearLayout bottomLayout = findViewById(R.id.bottomLayout);

        // Load the slide-up animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Start the animation
        bottomLayout.startAnimation(slideUp);
    }
}


