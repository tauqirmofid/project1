package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminHomePage extends AppCompatActivity {

    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;

    // New references for the drawer
    private DrawerLayout drawerLayout;
    private ImageView leftNavBarImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home_page);

        // 1) Find the DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

        // 2) Find the nav bar image in your top card
        leftNavBarImage = findViewById(R.id.leftNavBarImage);

        // 3) Set click listener to open/close the drawer
        if (leftNavBarImage != null) {
            leftNavBarImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                }
            });
        }

        // 4) (Optional) Handle nav menu button clicks
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navProfileButton = findViewById(R.id.navProfileButton);
        Button navLogoutButton = findViewById(R.id.navLogoutButton);

        if (navHomeButton != null) {
            navHomeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Example: close the drawer when "Home" is clicked
                    drawerLayout.closeDrawer(GravityCompat.START);

                    // TODO: Add your actual home logic here

                }
            });
        }

        if (navProfileButton != null) {
            navProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // TODO: Add your profile logic here
                    Intent intent = new Intent(AdminHomePage.this, Upload.class);
                    startActivity(intent);
                }
            });
        }

        if (navLogoutButton != null) {
            navLogoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // TODO: Add your logout logic here
                    Intent intent = new Intent(AdminHomePage.this, AdminLoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        // ===================================
        // Your existing code (unchanged)
        // ===================================
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);

        // Create your 7-day items with dummy tasks
        dayList = new ArrayList<>();
        dayList.add(new DayModel("Monday", "Task A, Task B, Task C"));
        dayList.add(new DayModel("Tuesday", "Task D, Task E"));
        dayList.add(new DayModel("Wednesday", "Task F, Task G, Task H"));
        dayList.add(new DayModel("Thursday", "Task I"));
        dayList.add(new DayModel("Friday", "Task J, Task K, Task L"));
        dayList.add(new DayModel("Saturday", "Task M, Task N"));
        dayList.add(new DayModel("Sunday", "Task O, Task P, Task Q"));

        // Create and set the adapter
        dayAdapter = new DayAdapter(dayList);
        carouselRecyclerView.setAdapter(dayAdapter);

        // Create and set the custom LayoutManager
        CarouselLayoutManager layoutManager = new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        // SnapHelper for centering
        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        // Jump to the middle of the huge list
        int halfMaxValue = Integer.MAX_VALUE / 2;
        int midPos = halfMaxValue - (halfMaxValue % dayList.size());
        carouselRecyclerView.scrollToPosition(midPos);

        carouselRecyclerView.post(() -> {
            carouselRecyclerView.smoothScrollToPosition(midPos);
        });

        // OverlapDecoration
        OverlapDecoration decoration = new OverlapDecoration(300);
        carouselRecyclerView.addItemDecoration(decoration);


    }
}
