package com.example.unimate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminHomePage extends AppCompatActivity {
    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home_page);

        // Find the RecyclerView
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

        // Create and set a custom LayoutManager
        CarouselLayoutManager layoutManager =
                new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        // Attach a SnapHelper for centering
        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        // Jump to the middle of the huge list so user can scroll left or right
        int halfMaxValue = Integer.MAX_VALUE / 2;
        int midPos = halfMaxValue - (halfMaxValue % dayList.size());
        carouselRecyclerView.scrollToPosition(midPos);

        carouselRecyclerView.post(() -> {
            carouselRecyclerView.smoothScrollToPosition(midPos);
        });

        // OverlapDecoration for overlapping cards; adjust the overlap as needed
        OverlapDecoration decoration = new OverlapDecoration(300);
        //try with 150 on right
        carouselRecyclerView.addItemDecoration(decoration);
    }
}