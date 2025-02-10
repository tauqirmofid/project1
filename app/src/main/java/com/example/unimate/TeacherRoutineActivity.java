package com.example.unimate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeacherRoutineActivity extends AppCompatActivity {
    private DayAdapter dayAdapter;
    private ProgressBar loadingSpinner;

    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private final String[] timeKeys = {
            "09:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
            "1:00-1:30PM", "1:30-2:50PM", "2:50-4:10PM", "7:00-8:20PM"
    };
    private List<DayModel> dayList;
    private ImageButton reloadButton;
    private Spinner spinnerSection;
    private RecyclerView carouselRecyclerView;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_routine);

        // Initialize Firestore and UI elements
        firestore = FirebaseFirestore.getInstance();
        spinnerSection = findViewById(R.id.spinnerSection);
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        reloadButton=findViewById(R.id.reload);
        loadingSpinner = findViewById(R.id.loadingSpinner);


        // Load acronyms from Firestore
        loadAcronymsFromFirestore();
        reloadButton.setOnClickListener(v -> reloadPage());
        // Initialize day list
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        // Setup adapter & carousel
        dayAdapter = new DayAdapter(dayList);
        carouselRecyclerView.setAdapter(dayAdapter);

        CarouselLayoutManager layoutManager = new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        OverlapDecoration decoration = new OverlapDecoration(300);
        carouselRecyclerView.addItemDecoration(decoration);

        // Set Spinner onItemSelectedListener
        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedAcronym = parent.getItemAtPosition(position).toString();
                if (!selectedAcronym.equals("Select Acronym")) {
                    fetchRoutineAndDisplay(selectedAcronym);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }
    private void reloadPage() {
        // Show loading spinner
        loadingSpinner.setVisibility(View.VISIBLE);

        // Reload acronyms and reset the spinner
        loadAcronymsFromFirestore();

        // Hide the spinner after 2 seconds (simulate reload)
        carouselRecyclerView.postDelayed(() -> loadingSpinner.setVisibility(View.GONE), 2000);
    }
    private void loadAcronymsFromFirestore() {
        firestore.collection("teacher_info")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> acronymsList = new ArrayList<>();
                    acronymsList.add("Select Acronym"); // Default option

                    // Extract acronyms from Firestore
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String acronym = document.getId();
                        acronymsList.add(acronym);
                    }

                    // Populate Spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            TeacherRoutineActivity.this,
                            android.R.layout.simple_spinner_item,
                            acronymsList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSection.setAdapter(adapter);
                    Toast.makeText(this, "Acronyms reloaded successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reload acronyms.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchRoutineAndDisplay(String acronym) {
        for (String dayName : daysOfWeek) {
            firestore.collection("schedules")
                    .document(dayName.toLowerCase())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            Log.d("Routine", "No data for day: " + dayName);
                            return;
                        }

                        boolean found = false;
                        DayModel dayModel = dayList.get(getDayIndex(dayName));

                        // Initialize each timeslot to "No Class"
                        for (int i = 0; i < timeKeys.length; i++) {
                            setClassInfo(dayModel, i, "No Class");
                        }

                        Map<String, Object> dayData = documentSnapshot.getData();
                        if (dayData != null) {
                            for (Map.Entry<String, Object> batchEntry : dayData.entrySet()) {
                                if (batchEntry.getValue() instanceof Map) {
                                    Map<String, Object> sections = (Map<String, Object>) batchEntry.getValue();

                                    for (Map.Entry<String, Object> sectionEntry : sections.entrySet()) {
                                        if (sectionEntry.getValue() instanceof Map) {
                                            Map<String, Object> timeslots = (Map<String, Object>) sectionEntry.getValue();

                                            for (Map.Entry<String, Object> timeEntry : timeslots.entrySet()) {
                                                if (timeEntry.getValue() instanceof Map) {
                                                    Map<String, Object> classDetails = (Map<String, Object>) timeEntry.getValue();
                                                    String instructor = (String) classDetails.get("instructor");

                                                    if (instructor != null && instructor.equals(acronym)) {
                                                        found = true;
                                                        String course = (String) classDetails.get("course");
                                                        String room = (String) classDetails.get("room");

                                                        String classInfo = (course != null ? course : "N/A") + "\n" +
                                                                instructor + "\n" +
                                                                (room != null ? room : "N/A");

                                                        int timeIndex = getTimeSlotIndex(timeEntry.getKey());
                                                        if (timeIndex != -1) {
                                                            setClassInfo(dayModel, timeIndex, classInfo);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (found) {
                            Log.d("Routine", "✅ Instructor " + acronym + " found on " + dayName);
                        } else {
                            Log.d("Routine", "❌ Instructor " + acronym + " NOT found on " + dayName);
                        }

                        dayAdapter.notifyItemChanged(getDayIndex(dayName));
                    })
                    .addOnFailureListener(e -> Log.e("FirestoreError", "❌ Firestore Error: " + e.getMessage()));
        }
    }

    private int getDayIndex(String dayName) {
        for (int i = 0; i < daysOfWeek.length; i++) {
            if (daysOfWeek[i].equalsIgnoreCase(dayName)) {
                return i;
            }
        }
        return -1;
    }

    private int getTimeSlotIndex(String timeKey) {
        for (int i = 0; i < timeKeys.length; i++) {
            if (timeKeys[i].equals(timeKey)) {
                return i;
            }
        }
        return -1;
    }

    private void setClassInfo(DayModel dayModel, int timeSlotIndex, String classInfo) {
        switch (timeSlotIndex) {
            case 0:
                dayModel.setClass1(classInfo);
                break;
            case 1:
                dayModel.setClass2(classInfo);
                break;
            case 2:
                dayModel.setClass3(classInfo);
                break;
            case 3:
                dayModel.setClass4(classInfo);
                break;
            case 4:
                dayModel.setClass5(classInfo);
                break;
            case 5:
                dayModel.setClass6(classInfo);
                break;
            case 6:
                dayModel.setClass7(classInfo);
                break;
        }
    }
}
