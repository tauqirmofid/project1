package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import android.content.SharedPreferences;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CR_HomePage extends AppCompatActivity {
    private ImageView leftNavBarImage;
    private DrawerLayout drawerLayout;
    private RecyclerView carouselRecyclerView;
    private CardView teacherInfo,othersRoutine,rooms,map,task,editRoutine,tcrRtn;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;

    private String crBatch, crSection;
    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
    };

    private FirebaseFirestore db;

    // The time slot keys you use in Firestore
    private final String[] timeKeys = {
            "09:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
            "1:00-1:30PM", "1:30-2:50PM", "2:50-4:10PM", "7:00-8:20PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cr_home_page);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        teacherInfo=findViewById(R.id.cr_teachersInfoCard);
        teacherInfo.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,Teacher_infoActivity.class);
            startActivity(i);
        });

        rooms=findViewById(R.id.CR_roomsCardView);
        rooms.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,RoomsActivity.class);
            startActivity(i);

        });

        map=findViewById(R.id.universityMapCard);

        map.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,MapActivity.class);
            startActivity(i);
        });

        task=findViewById(R.id.upcomingTaskCard);

        task.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,OtherCalendar.class);
            i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
            i.putExtra("CR_SECTION", crSection);
            startActivity(i);
        });


        editRoutine=findViewById(R.id.editRtncard);

        task.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,CrCalendar.class);
            i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
            i.putExtra("CR_SECTION", crSection);
            startActivity(i);
        });


        // Bind UI elements
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        TextView crNameTextView = findViewById(R.id.CR_nameText);
        TextView crBatchTextView = findViewById(R.id.tv_cr_batch);
        TextView crSectionTextView = findViewById(R.id.tv_cr_section);

        leftNavBarImage = findViewById(R.id.leftNavBarImage);
        drawerLayout = findViewById(R.id.drawerLayout);

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

        // Get CR details from Intent
        Intent intent = getIntent();
        if (intent != null) {
            String crName = intent.getStringExtra("CR_NAME");
            crBatch = intent.getStringExtra("CR_BATCH");   // e.g. "59th"
            crSection = intent.getStringExtra("CR_SECTION");  // e.g. "Bth"

            if (crName == null || crBatch == null || crSection == null) {
                Toast.makeText(this, "Error: Missing CR Data", Toast.LENGTH_SHORT).show();
                crName = "Unknown CR";
                crBatch = "N/A";
                crSection = "N/A";
            }

            // Set CR details in UI
            crNameTextView.setText(crName);
            crBatchTextView.setText(crBatch);     // will show "batch_59"
            crSectionTextView.setText(crSection); // will show "B"

            // Convert batch from "59th" -> "batch_59"
            crBatch = convertBatchToFirestoreFormat(crBatch);
            // Convert section from "Bth" -> "B"
            crSection = convertSectionToFirestoreFormat(crSection);


        } else {
            Toast.makeText(this, "Error: Intent Data Not Found", Toast.LENGTH_SHORT).show();
        }

        // Initialize dayList with default "No Class"
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        // Setup adapter & carousel
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
        fetchAllDays(crBatch, crSection);



        LinearLayout otherRoutineLayout = findViewById(R.id.otherRoutine);
        if (otherRoutineLayout != null) {
            otherRoutineLayout.setOnClickListener(v -> {
                Intent i = new Intent(CR_HomePage.this, CrCalendar.class);
                i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
                i.putExtra("CR_SECTION", crSection);
                startActivity(i);
            });
        }

        LinearLayout editRoutineLayout = findViewById(R.id.tcrRoutine);
        if (editRoutineLayout != null) {
            editRoutineLayout.setOnClickListener(v -> {
                Intent i = new Intent(CR_HomePage.this, CrCalendar.class);
                i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
                i.putExtra("CR_SECTION", crSection);
                startActivity(i);
            });
        }
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
                Intent intent = new Intent(CR_HomePage.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
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
            Toast.makeText(this, "Error: CR Batch or Section Missing", Toast.LENGTH_SHORT).show();
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
}
