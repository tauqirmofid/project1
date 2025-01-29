package com.example.unimate;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OthersRoutine extends AppCompatActivity {

    private Spinner spinnerBatch, spinnerSection;
    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;

    // We’ll store 7 DayModel objects, one for each day
    private List<DayModel> dayList;
    private final String[] daysOfWeek = {
            "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"
    };

    // Firestore
    private FirebaseFirestore db;

    // Timeslot keys in Firestore vs. how we place them in each row
    private final String[] timeKeys = {
            "9:00-10:20AM",
            "10:20-11:40AM",
            "11:40-1:00PM",
            "1:00-1:30PM",
            "1:30-2:50PM",
            "2:50-4:10PM",
            "7:00-8:20PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_others_routine);

        db = FirebaseFirestore.getInstance();

        // 1) Setup spinners
        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerSection = findViewById(R.id.spinnerSection);

        // The batch array - 59 is at index 3
        String[] batchArray = {"56","57","58","59","60","61","62","63","64","65"};
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, batchArray
        );
        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBatch.setAdapter(batchAdapter);

        // The section array - B is at index 1
        String[] sectionArray = {"A","B","C","D","E","F","G","H","I"};
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sectionArray
        );
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);

        // 2) Find the RecyclerView
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);

        // Initialize dayList with default "No Class"
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        // Create and set the adapter
        dayAdapter = new DayAdapter(dayList);
        carouselRecyclerView.setAdapter(dayAdapter);

        // Custom LayoutManager
        CarouselLayoutManager layoutManager =
                new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        // SnapHelper for centering
        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        // Infinity scroll trick
        int halfMaxValue = Integer.MAX_VALUE / 2;
        int midPos = halfMaxValue - (halfMaxValue % dayList.size());
        carouselRecyclerView.scrollToPosition(midPos);
        carouselRecyclerView.post(() -> {
            carouselRecyclerView.smoothScrollToPosition(midPos);
        });

        // Overlap decoration
        OverlapDecoration decoration = new OverlapDecoration(300);
        carouselRecyclerView.addItemDecoration(decoration);

        // Spinner listener - when user changes batch/section, re-fetch
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchAllDays();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerBatch.setOnItemSelectedListener(spinnerListener);
        spinnerSection.setOnItemSelectedListener(spinnerListener);

        // 3) Set default selection: batch=59, section=B
        // batch 59 is at index 3, section B is at index 1
        spinnerBatch.setSelection(3, false);
        spinnerSection.setSelection(1, false);

        // Finally, fetch with these defaults
        fetchAllDays();
    }

    /**
     * Fetch the schedule for all 7 days, for the currently selected batch & section.
     */
    private void fetchAllDays() {
        final String selectedBatch = spinnerBatch.getSelectedItem().toString(); // e.g. "59"
        final String selectedSection = spinnerSection.getSelectedItem().toString(); // e.g. "B"

        // Re-create dayList each time
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName)); // default "No Class"
        }

        fetchDay(0, selectedBatch, selectedSection);
    }

    /**
     * Recursively fetch day i from Firestore, fill dayList[i], then move on to i+1
     */
    private void fetchDay(int dayIndex, String batch, String section) {
        if (dayIndex >= daysOfWeek.length) {
            // All done, update adapter
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);
            return;
        }

        String dayName = daysOfWeek[dayIndex];
        String docName = dayName.toLowerCase();  // e.g. "monday"

        db.collection("schedules")
                .document(docName)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists() && docSnapshot.getData() != null) {
                        Map<String, Object> topLevel = docSnapshot.getData();

                        String batchKey = "batch_" + batch;
                        if (topLevel.containsKey(batchKey)) {
                            Object batchVal = topLevel.get(batchKey);
                            if (batchVal instanceof Map) {
                                Map<String, Object> sectionsMap = (Map<String, Object>) batchVal;
                                if (sectionsMap.containsKey(section)) {
                                    Object sectionVal = sectionsMap.get(section);
                                    if (sectionVal instanceof Map) {
                                        Map<String, Object> timeslotMap = (Map<String, Object>) sectionVal;

                                        DayModel dayModel = dayList.get(dayIndex);

                                        // Fill dayModel timeslot fields
                                        for (int i = 0; i < timeKeys.length; i++) {
                                            String tKey = timeKeys[i];
                                            String classInfo = "No Class";
                                            if (timeslotMap.containsKey(tKey)) {
                                                Object slotVal = timeslotMap.get(tKey);
                                                if (slotVal instanceof Map) {
                                                    Map<String, Object> cMap = (Map<String, Object>) slotVal;
                                                    String course = safeGetString(cMap, "course");
                                                    String instructor = safeGetString(cMap, "instructor");
                                                    String room = safeGetString(cMap, "room");
                                                    classInfo = course + "\n" + instructor + "\n" + room;
                                                }
                                            }
                                            // place into dayModel
                                            switch (i) {
                                                case 0: dayModel.setClass1(classInfo); break;
                                                case 1: dayModel.setClass2(classInfo); break;
                                                case 2: dayModel.setClass3(classInfo); break;
                                                case 3: dayModel.setClass4(classInfo); break;
                                                case 4: dayModel.setClass5(classInfo); break;
                                                case 5: dayModel.setClass6(classInfo); break;
                                                case 6: dayModel.setClass7(classInfo); break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Fetch next day
                    fetchDay(dayIndex + 1, batch, section);
                })
                .addOnFailureListener(e -> {
                    // Even on failure, move on
                    fetchDay(dayIndex + 1, batch, section);
                });
    }

    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "N/A" : val.toString();
    }
}
