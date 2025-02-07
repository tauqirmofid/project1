package com.example.unimate;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentHomePage extends AppCompatActivity {
    private ImageView leftNavBarImage;
    private RecyclerView carouselRecyclerView;
    private List<DayModel> dayList;
    private DayAdapter dayAdapter;
    private CardView rooms,otherRoutine,task,maps,teacherInfo,routine;

    private TextView studentNameText, studentBatchText, studentSectionText;
    private DrawerLayout drawerLayout;
    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
    }; private FirebaseFirestore db;

    // The time slot keys you use in Firestore
    private final String[] timeKeys = {
            "9:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
            "1:00-1:30PM", "1:30-2:50PM", "2:50-4:10PM", "7:00-8:20PM"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();


        setContentView(R.layout.activity_student_home_page);
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);

        studentNameText = findViewById(R.id.std_nameText);
        studentBatchText = findViewById(R.id.tv_std_batch);
        studentSectionText = findViewById(R.id.tv_std_section);
        leftNavBarImage = findViewById(R.id.leftNavBarImage);
        drawerLayout = findViewById(R.id.drawerLayout);
        otherRoutine=findViewById(R.id.othersRoutineCard);
        otherRoutine.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, OthersRoutine.class);
            startActivity(intent);

        });
        task=findViewById(R.id.upcomingTaskCard);
        task.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, OtherCalendar.class);
            startActivity(intent);
        });
        rooms = findViewById(R.id.st_roomsCardView);
        rooms.setOnClickListener(v -> {
                    Intent intent = new Intent(StudentHomePage.this, RoomsActivity.class);
                    startActivity(intent);
                });


        Intent intent = getIntent();
        String stdName = intent.getStringExtra("STUDENT_NAME");
        String stdBatch = intent.getStringExtra("STUDENT_BATCH");
        String stdSection = intent.getStringExtra("STUDENT_SECTION");

        // If Intent data is null (app restarted), fetch from SharedPreferences
        if (stdName == null || stdBatch == null || stdSection == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
            stdName = sharedPreferences.getString("studentName", "Student");
            stdBatch = sharedPreferences.getString("studentBatch", "N/A");
            stdSection = sharedPreferences.getString("studentSection", "N/A");
        }

        studentNameText.setText(stdName);
        studentBatchText.setText(stdBatch);
        studentSectionText.setText(stdSection);
        Toast.makeText(this, "Welcome, " + stdName, Toast.LENGTH_SHORT).show();

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
        // Convert batch from "59th" -> "batch_59"
        stdBatch = convertBatchToFirestoreFormat(stdBatch);
        // Convert section from "Bth" -> "B"
        stdSection = convertSectionToFirestoreFormat(stdSection);

        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }
        dayAdapter = new DayAdapter(dayList);
        carouselRecyclerView.setAdapter(dayAdapter);

        CarouselLayoutManager layoutManager =
                new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        OverlapDecoration decoration = new OverlapDecoration(300);
        carouselRecyclerView.addItemDecoration(decoration);

        // Fetch the routine for the CR's batch & section
        fetchAllDays(stdBatch, stdSection);

    }

    private String convertBatchToFirestoreFormat(String batch) {
        if (batch == null) return "batch_unknown";
        batch = batch.trim();
        if (batch.endsWith("th") || batch.endsWith("TH")) {
            batch = batch.replaceAll("(?i)th$", ""); // remove 'th' at end, case-insensitive
        }
        // If the user typed "59", "Batch_59", etc., handle as needed
        if (!batch.startsWith("batch_")) {
            batch = "batch_" + batch;
        }
        return batch;
    }


    private String convertSectionToFirestoreFormat(String section) {
        if (section == null) return "A"; // fallback
        section = section.trim();

        // If user typed "Sec-B", remove "Sec-"
        if (section.toLowerCase().startsWith("sec-")) {
            section = section.substring(4); // remove "sec-"
        }
        // If ends with "th", remove it (like "Bth")
        if (section.toLowerCase().endsWith("th")) {
            section = section.replaceAll("(?i)th$", "");
        }
        // Possibly uppercase
        section = section.toUpperCase(); // "b" -> "B"

        return section;
    }


    private void fetchAllDays(String batch, String section) {
        if (batch == null || section == null) {
            Toast.makeText(this, "Error: Student Batch or Section Missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-init dayList with "No Class"
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        fetchDay(0, batch, section);
    }
    private void fetchDay(int dayIndex, String batch, String section) {
        if (dayIndex >= daysOfWeek.length) {
            // Done, refresh adapter
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);

            return;
        }

        String dayName = daysOfWeek[dayIndex];
        String docName = dayName.toLowerCase(); // e.g. "monday"

        db.collection("schedules")
                .document(docName)
                .get()
                .addOnSuccessListener(docSnapshot -> {

                    DayModel dayModel = dayList.get(dayIndex);

                    // Initialize each timeslot to "No Class"
                    for (int i = 0; i < timeKeys.length; i++) {
                        setClassInfo(dayModel, i, "No Class");
                    }

                    if (docSnapshot.exists() && docSnapshot.getData() != null) {
                        Map<String, Object> topLevel = docSnapshot.getData();

                        // Check if "batch_59" etc. exists
                        if (topLevel.containsKey(batch)) {
                            Object batchVal = topLevel.get(batch);
                            if (batchVal instanceof Map) {
                                Map<String, Object> sectionsMap = (Map<String, Object>) batchVal;

                                // For debugging: see what's in that batch
                                // e.g. sections: A,B,C...
                                // StringBuilder availableSections = new StringBuilder("Sections: ");
                                // for (String s : sectionsMap.keySet()) {
                                //     availableSections.append(s).append(" ");
                                // }
                                // Toast.makeText(this, availableSections.toString(), Toast.LENGTH_SHORT).show();

                                // Check if desired section is present
                                if (sectionsMap.containsKey(section)) {
                                    Object sectionVal = sectionsMap.get(section);
                                    if (sectionVal instanceof Map) {
                                        Map<String, Object> timeslotMap = (Map<String, Object>) sectionVal;

                                        // Fill timeslots
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
                                            setClassInfo(dayModel, i, classInfo);
                                        }
                                    } else {
                                        //Toast.makeText(this, "Section data not a map: " + section, Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    //Toast.makeText(this, "Section '" + section + "' not found in " + batch + " for " + dayName, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                //Toast.makeText(this, "Batch data not a map: " + batch, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            //Toast.makeText(this, "Batch '" + batch + "' not found in " + dayName, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // No doc for this day
                        //Toast.makeText(this, "No schedule for " + dayName, Toast.LENGTH_LONG).show();
                    }

                    // Next day
                    fetchDay(dayIndex + 1, batch, section);
                })
                .addOnFailureListener(e -> {
                    // Toast.makeText(this, "Failed day: " + dayName + " => " + e.getMessage(), Toast.LENGTH_LONG).show();
                    fetchDay(dayIndex + 1, batch, section);
                });
    }
    private void setClassInfo(DayModel dayModel, int timeSlotIndex, String classInfo) {
        switch (timeSlotIndex) {
            case 0: dayModel.setClass1(classInfo); break;
            case 1: dayModel.setClass2(classInfo); break;
            case 2: dayModel.setClass3(classInfo); break;
            case 3: dayModel.setClass4(classInfo); break;
            case 4: dayModel.setClass5(classInfo); break;
            case 5: dayModel.setClass6(classInfo); break;
            case 6: dayModel.setClass7(classInfo); break;
        }
    }
    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "N/A" : val.toString();
    }

    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navLogoutButton = findViewById(R.id.navLogoutButton);

        if (navHomeButton != null) {
            navHomeButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navLogoutButton != null) {
            navLogoutButton.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // Clear login state
                editor.apply();
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(StudentHomePage.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

}