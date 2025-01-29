package com.example.unimate;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Guest_HomePage extends AppCompatActivity {

    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_home_page);

        // Find the RecyclerView
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);



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
