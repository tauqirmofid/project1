package com.demo.unimate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;

import android.widget.Button;
import android.widget.ImageView;

import androidx.drawerlayout.widget.DrawerLayout;

public class Guest_HomePage extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ImageView leftNavBarImage;

    private CardView routinecard,rooms,teachersInfo,maps,teacherRoutine;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_home_page);

        rooms=findViewById(R.id.guest_roomsCardView);
        rooms.setOnClickListener(v -> {
            Intent intent = new Intent(Guest_HomePage.this, RoomsActivity.class);
            startActivity(intent);
        });
        teachersInfo=findViewById(R.id.guest_teachersInfoCard);
        teachersInfo.setOnClickListener(v -> {
            Intent intent = new Intent(Guest_HomePage.this, Teacher_infoActivity.class);
            startActivity(intent);
        });
        maps=findViewById(R.id.university_map);
        maps.setOnClickListener(v->{
            Intent intent = new Intent(Guest_HomePage.this, MapActivity.class);
            startActivity(intent);
        });
        teacherRoutine=findViewById(R.id.teacherRoutine);
        teacherRoutine.setOnClickListener(v -> {
            Intent intent = new Intent(Guest_HomePage.this, TeacherRoutineActivity.class);
            startActivity(intent);
        });


        drawerLayout = findViewById(R.id.drawerLayout);
        leftNavBarImage = findViewById(R.id.leftNavBarImage);

        if (leftNavBarImage != null) {
            leftNavBarImage.setOnClickListener(view -> {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
        setUpNavigationButtons();

        routinecard = findViewById(R.id.guest_routineCardView);
        routinecard.setOnClickListener(v -> {
            Intent intent = new Intent(Guest_HomePage.this, OthersRoutine.class);
            startActivity(intent);
        });


        animateCards();

    }
    private void animateCards() {
        CardView[] cards = {
                findViewById(R.id.guest_routineCardView),
                findViewById(R.id.teacherRoutine),
                findViewById(R.id.guest_teachersInfoCard),
                findViewById(R.id.university_map),
                findViewById(R.id.guest_roomsCardView)
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

    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navLoginButton = findViewById(R.id.navChooseRoleButton);
        Button contacUs = findViewById(R.id.contacUs);
        if (navHomeButton != null) {
            navHomeButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
        if (navLoginButton != null) {
            navLoginButton.setOnClickListener(v ->{
                    Intent intent = new Intent(Guest_HomePage.this, MainActivity.class);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
        if (contacUs != null) {
            contacUs.setOnClickListener(v ->{
                Intent intent = new Intent(Guest_HomePage.this, ContactDevelopersActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

    }
}
