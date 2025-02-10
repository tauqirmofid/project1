package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.ArrayList;
import java.util.List;

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
    }

    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navLoginButton = findViewById(R.id.navChooseRoleButton);
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

    }
}
