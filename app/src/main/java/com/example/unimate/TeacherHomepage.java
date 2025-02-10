package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherHomepage extends AppCompatActivity {
    private TextView tvTeacherName, tvDesignation, tvAcronym, tvEmail;
    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private CardView task,t_othersRoutineCard,profile;
    private List<DayModel> dayList;
    private ImageView leftNavBarImage, ProfileImage;
    private DrawerLayout drawerLayout;
    private CardView routine,rooms,otherRoutine,teacherInfo;
    private DatabaseReference databaseReference;
    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private final String[] timeKeys = {
            "9:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
            "1:00-1:30PM", "1:30-2:50PM", "2:50-4:10PM", "7:00-8:20PM"
    };

    private FirebaseFirestore db;
    private String teacherAcronym;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_homepage);

        // Initialize UI elements
        ProfileImage=findViewById(R.id.statusIcon);
        tvTeacherName = findViewById(R.id.tv_teacher_name);
        tvDesignation = findViewById(R.id.tv_designation);
        tvAcronym = findViewById(R.id.tv_acronym);
        tvEmail = findViewById(R.id.tv_email);
        task=findViewById(R.id.t_upcomingTaskCard);
        otherRoutine=findViewById( R.id.t_othersRoutineCard);
        routine=findViewById(R.id.t_routineCardView);
       // teacherAcronym = getIntent().getStringExtra("acronym");

        routine.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherHomepage.this, TeacherRoutineActivity.class);
            intent.putExtra("acronym", teacherAcronym);
            startActivity(intent);
        });

        otherRoutine.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherHomepage.this, OtherCalendar.class);
            startActivity(intent);
        });

        task.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherHomepage.this, TeacherCalendarActivity.class);
            intent.putExtra("acronym", teacherAcronym);
            startActivity(intent);
        });
        profile=findViewById(R.id.statusCardView);
        profile.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherHomepage.this, TeacherProfileActivity.class);
            intent.putExtra("acronym", teacherAcronym);
            startActivity(intent);
        });
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);

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


        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get the email passed from the login activity
        String teacherEmail = getIntent().getStringExtra("teacherEmail");

        if (teacherEmail != null) {
            fetchTeacherDetailsFromRealtimeDB(teacherEmail);
        } else {
            Toast.makeText(this, "Failed to get teacher email", Toast.LENGTH_SHORT).show();
        }

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
                Intent intent = new Intent(TeacherHomepage.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }


    private void fetchTeacherDetailsFromRealtimeDB(String email) {
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("AcceptedRequests").child("Teachers");

        teachersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String storedEmail = teacherSnapshot.child("email").getValue(String.class);
                    Log.d("TeacherDetails", "Checking teacher with email: " + storedEmail);

                    if (storedEmail != null && storedEmail.equals(email)) {
                        // Teacher found
                        String name = teacherSnapshot.child("name").getValue(String.class);
                        String designation = teacherSnapshot.child("designation").getValue(String.class);
                        teacherAcronym = teacherSnapshot.child("acronym").getValue(String.class);
                        String imageUrl = teacherSnapshot.child("imageUrl").getValue(String.class);

                        tvTeacherName.setText(name != null ? name : "N/A");
                        tvDesignation.setText(designation != null ? designation : "N/A");
                        tvAcronym.setText(teacherAcronym != null ? teacherAcronym : "N/A");
                        tvEmail.setText(storedEmail);

                        // Load the profile image into the statusIcon
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(TeacherHomepage.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.teacher_info) // Add a default placeholder
                                    .error(R.drawable.teacher_info) // Add an error image if loading fails
                                    .into(ProfileImage);
                            Log.d("ImageLoad", "Profile image loaded: " + imageUrl);
                        } else {
                            Log.e("ImageLoad", "No image URL found for teacher: " + teacherAcronym);
                        }

                        // Fetch and store routine if acronym exists
                        if (teacherAcronym != null) {
                            fetchRoutineAndStoreInTeacherSchedule(teacherAcronym);
                        } else {
                            Log.e("AcronymError", "Acronym not found for the teacher.");
                        }
                        return; // Exit the loop once the teacher is found
                    }
                }

                Log.e("TeacherDetails", "No teacher found with email: " + email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherHomepage.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRoutineAndStoreInTeacherSchedule(String acronym) {
        for (String dayName : daysOfWeek) {
            db.collection("schedules")
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

                                                        // ✅ Store into teacher_schedule
                                                        storeTeacherSchedule(acronym, dayName, batchEntry.getKey(), sectionEntry.getKey(), timeEntry.getKey(), course, room);
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
    private void storeTeacherSchedule(String acronym, String day, String batch, String section, String timeSlot, String course, String room) {
        Map<String, Object> classData = new HashMap<>();
        classData.put("course_name", course);
        classData.put("room", room);
        classData.put("details", "Class for " + course);

        db.collection("teacher_schedule")
                .document(acronym)
                .collection(day)
                .document(batch)
                .collection(section)
                .document(timeSlot)
                .set(classData)
                .addOnSuccessListener(aVoid -> Log.d("TeacherSchedule", "✅ Stored: " + acronym + " - " + day + " - " + timeSlot))
                .addOnFailureListener(e -> Log.e("FirestoreError", "❌ Failed to store teacher schedule", e));
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
