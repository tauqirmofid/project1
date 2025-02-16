package com.demo.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ContactDevelopersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_developers);

        ImageView backButton = findViewById(R.id.leftNavBarImage);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Set up toolbar
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setTitle("");
//        }

        // Email click handler
        TextView contactEmail = findViewById(R.id.contact_email);
        contactEmail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:info.teamunimate@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Unimate App Feedback");
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        });

        // Add animation
        animateCards();
    }



    private void animateCards() {
        CardView[] cards = {
                findViewById(R.id.card_developer1),
                findViewById(R.id.card_developer2),
                findViewById(R.id.card_developer3)
        };

        for (int i = 0; i < cards.length; i++) {
            cards[i].setAlpha(0);
            cards[i].setTranslationY(50);
            cards[i].animate()
                    .alpha(1)
                    .translationY(0)
                    .setStartDelay(i * 150)
                    .setDuration(500)
                    .start();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}