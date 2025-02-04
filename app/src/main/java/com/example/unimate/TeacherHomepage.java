package com.example.unimate;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TeacherHomepage extends AppCompatActivity {
    private TextView tvTeacherName, tvDesignation, tvAcronym, tvEmail;
    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;
    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private final String[] timeKeys = {
            "9:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
            "1:00-1:30PM", "1:30-2:50PM", "2:50-4:10PM", "7:00-8:20PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_homepage);

        // Initialize TextViews and RecyclerView
        tvTeacherName = findViewById(R.id.tv_teacher_name);
        tvDesignation = findViewById(R.id.tv_designation);
        tvAcronym = findViewById(R.id.tv_acronym);
        tvEmail = findViewById(R.id.tv_email);
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);

        // Get the email passed from the login activity
        String teacherEmail = getIntent().getStringExtra("teacherEmail");

        if (teacherEmail != null) {
            fetchTeacherDetails(teacherEmail);
        } else {
            Toast.makeText(this, "Failed to get teacher email", Toast.LENGTH_SHORT).show();
        }

        // Initialize the day list and setup adapter
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


    }

    private void fetchTeacherDetails(String email) {
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("AcceptedRequests").child("Teachers");

        teachersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String storedEmail = teacherSnapshot.child("email").getValue(String.class);
                    if (storedEmail != null && storedEmail.equals(email)) {
                        // Fetch teacher details
                        String name = teacherSnapshot.child("name").getValue(String.class);
                        String designation = teacherSnapshot.child("designation").getValue(String.class);
                        String acronym = teacherSnapshot.child("acronym").getValue(String.class);

                        // Update TextViews
                        tvTeacherName.setText(name != null ? name : "N/A");
                        tvDesignation.setText(designation != null ? designation : "N/A");
                        tvAcronym.setText(acronym != null ? acronym : "N/A");

                        tvEmail.setText(storedEmail != null ? storedEmail : "N/A");

                        // Fetch routine by acronym
                        if (acronym != null) {
                            //fetchRoutineByAcronym(acronym);
                        }
                        return;
                    }
                }

                // If no teacher found
                Toast.makeText(TeacherHomepage.this, "No teacher details found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherHomepage.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

//    private void fetchRoutineByAcronym(String acronym) {
//        DatabaseReference schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
//
//        for (int dayIndex = 0; dayIndex < daysOfWeek.length; dayIndex++) {
//            String dayName = daysOfWeek[dayIndex];
//            int finalDayIndex = dayIndex;
//
//            schedulesRef.child(dayName.toLowerCase())
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (!snapshot.exists()) {
//                                Log.d("Routine", "No data for day: " + dayName);
//                                return;
//                            }
//
//                            DayModel dayModel = dayList.get(finalDayIndex);
//                            // Initialize timeslots to "No Class"
//                            for (int i = 0; i < timeKeys.length; i++) {
//                                setClassInfo(dayModel, i, "No Class");
//                            }
//
//                            // Loop through batches
//                            for (DataSnapshot batchSnapshot : snapshot.getChildren()) {
//                                Log.d("BatchKey", "Batch: " + batchSnapshot.getKey());
//
//                                // Loop through sections
//                                for (DataSnapshot sectionSnapshot : batchSnapshot.getChildren()) {
//                                    Log.d("SectionKey", "Section: " + sectionSnapshot.getKey());
//
//                                    // Loop through timeslots
//                                    for (DataSnapshot timeslotSnapshot : sectionSnapshot.getChildren()) {
//                                        String timeKey = timeslotSnapshot.getKey();
//                                        String instructor = timeslotSnapshot.child("instructor").getValue(String.class);
//
//                                        Log.d("TimeSlot", "Time: " + timeKey + ", Instructor: " + instructor);
//
//                                        if (instructor != null && instructor.equals(acronym)) {
//                                            String course = timeslotSnapshot.child("course").getValue(String.class);
//                                            String room = timeslotSnapshot.child("room").getValue(String.class);
//
//                                            String classInfo = (course != null ? course : "N/A") + "\n" +
//                                                    (instructor != null ? instructor : "N/A") + "\n" +
//                                                    (room != null ? room : "N/A");
//
//                                            // Find the correct time slot index
//                                            int timeIndex = getTimeSlotIndex(timeKey);
//                                            if (timeIndex != -1) {
//                                                setClassInfo(dayModel, timeIndex, classInfo);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            // Notify adapter to update the view
//                            dayAdapter.notifyItemChanged(finalDayIndex);
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            Toast.makeText(TeacherHomepage.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        }
//    }
//
    private int getTimeSlotIndex(String timeKey) {
        for (int i = 0; i < timeKeys.length; i++) {
            if (timeKeys[i].equals(timeKey)) {
                return i;
            }
        }
        return -1; // If no matching time slot is found
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
}
