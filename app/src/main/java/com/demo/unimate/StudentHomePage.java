package com.demo.unimate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Source;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class StudentHomePage extends AppCompatActivity {
    private ImageView leftNavBarImage;
    private RecyclerView carouselRecyclerView;
    private List<DayModel> dayList;
    private DayAdapter dayAdapter;
    private LinearLayout routine;
    private DrawerLayout drawerLayout;
    private CardView rooms,otherRoutine,task,maps,teacherInfo,profile,teacherRoutine;

    // Cards / UI elements
    private TextView studentNameText, studentBatchText, studentSectionText;
    private TextView tvCurrentClass, tvNextClass, tvPreviousClass;  // For the current/next/previous classes
    private TextView st_today,st_class,st_task;
    // Firebase
    private FirebaseFirestore db;

    private SwipeRefreshLayout swipeRefreshLayout;


    // Days of the week (in the same order as in OthersRoutine)
    private final String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
    };

    // Time keys as stored in Firestore
    private final String[] timeKeys = {
            "09:00-10:20AM",
            "10:20-11:40AM",
            "11:40-1:00PM",
            "1:00-1:30PM",
            "1:30-2:50PM",
            "2:50-4:10PM",
            "7:00-8:20PM"
    };

    // For easily parsing start/end times, define them in parallel arrays:
    private final String[] timeSlotStart = {
            "09:05AM",
            "10:30AM",
            "11:55AM",
            "01:15PM",
            "02:00PM",
            "03:25PM",
            "07:00PM"
    };
    private final String[] timeSlotEnd = {
            "10:25AM",
            "11:50AM",
            "01:15PM",
            "02:00PM",
            "03:20PM",
            "04:45PM",
            "08:20PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        // Enable Firestore offline data
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Find views
        carouselRecyclerView = findViewById(R.id.carouselRecyclerView);
        leftNavBarImage = findViewById(R.id.leftNavBarImage);
        drawerLayout = findViewById(R.id.drawerLayout);
        routine = findViewById(R.id.routineCardView);
        teacherRoutine=findViewById(R.id.teachersroutine);
        teacherRoutine.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, TeacherRoutineActivity.class);
            startActivity(intent);
        });

        // The TextViews for showing student info
        studentNameText = findViewById(R.id.std_nameText);
        studentBatchText = findViewById(R.id.tv_std_batch);
        studentSectionText = findViewById(R.id.tv_std_section);
        leftNavBarImage = findViewById(R.id.leftNavBarImage);
        drawerLayout = findViewById(R.id.drawerLayout);
        otherRoutine=findViewById(R.id.othersRoutineCard);

        // TextViews for current/next/previous class
        tvCurrentClass = findViewById(R.id.tvCurrentClass);
        tvNextClass = findViewById(R.id.tvNextClass);
        tvPreviousClass = findViewById(R.id.tvPreviousClass);

        st_today = findViewById(R.id.st_today);
        st_class = findViewById(R.id.st_class);
        st_task  = findViewById(R.id.st_task);

        // Handle “Routine” button: scroll to the carousel
        routine.setOnClickListener(v -> {
            // Scroll to the position of carouselRecyclerView
            NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView); // Ensure this is the correct ID
            View carouselRecyclerView = findViewById(R.id.carouselRecyclerView); // Ensure this is the correct ID

            if (nestedScrollView != null && carouselRecyclerView != null) {
                // Calculate the Y position of carouselRecyclerView relative to the parent NestedScrollView
                int targetScrollY = carouselRecyclerView.getTop();

                // Scroll smoothly to the calculated Y position
                nestedScrollView.post(() -> nestedScrollView.smoothScrollTo(0, targetScrollY));
            }
        });
        otherRoutine.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, OthersRoutine.class);
            startActivity(intent);

        });



        teacherInfo=findViewById(R.id.teachersInfoCard);
        teacherInfo.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, Teacher_infoActivity.class);
            startActivity(intent);
        });

        maps=findViewById(R.id.universityMapCard);
        maps.setOnClickListener(v->{
            Intent intent = new Intent(StudentHomePage.this, MapActivity.class);
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

        // Read student info from Intent or SharedPreferences
        Intent intent = getIntent();
        String stdName = intent.getStringExtra("STUDENT_NAME");
        String stdBatch = intent.getStringExtra("STUDENT_BATCH");
        String stdSection = intent.getStringExtra("STUDENT_SECTION");
        String stemail = intent.getStringExtra("STUDENT_EMAIL");
        // If null, get from SharedPreferences
        if (stdName == null || stdBatch == null || stdSection == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
            stdName = sharedPreferences.getString("studentName", "Student");
            stdBatch = sharedPreferences.getString("studentBatch", "N/A");
            stdSection = sharedPreferences.getString("studentSection", "N/A");
        }

        profile =findViewById(R.id.statusCardView);
        profile.setOnClickListener(v -> {
            Intent intent2 = new Intent(StudentHomePage.this, StudentProfile.class);

            // Retrieve email from SharedPreferences if needed
            SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
            String storedEmail = sharedPreferences.getString("studentEmail", null);

            // Pass email to StudentProfile
            if (storedEmail != null) {
                intent2.putExtra("STUDENT_EMAIL", storedEmail);
            } else {
                Toast.makeText(this, "No email found!", Toast.LENGTH_SHORT).show();
            }

            startActivity(intent2);
        });



        ImageView searchEditText = findViewById(R.id.searchIcon);
        searchEditText.setOnClickListener(v -> {
            startActivity(new Intent(StudentHomePage.this, UniversalSearchActivity.class));
        });

        // Set UI
        studentNameText.setText(stdName);
        studentBatchText.setText(stdBatch);
        studentSectionText.setText(stdSection);
        //Toast.makeText(this, "Welcome, " + stdName, Toast.LENGTH_SHORT).show();

        // Nav Drawer image
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

        // Convert user’s batch/section input to Firestore format
        stdBatch = convertBatchToFirestoreFormat(stdBatch);
        stdSection = convertSectionToFirestoreFormat(stdSection);

        // Prepare day list (7 days)
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }
        // Set up our adapter and custom layout manager
        dayAdapter = new DayAdapter(dayList);
        carouselRecyclerView.setAdapter(dayAdapter);

        CarouselLayoutManager layoutManager =
                new CarouselLayoutManager(this, RecyclerView.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);

        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        // In StudentHomePage's onCreate()
// Remove existing decorations first
        if (carouselRecyclerView.getItemDecorationCount() > 0) {
            carouselRecyclerView.removeItemDecorationAt(0);
        }

// Calculate overlap based on screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int overlap = (int) (screenWidth * 0.3); // 30% of screen width

        carouselRecyclerView.addItemDecoration(new OverlapDecoration(overlap));

        // Finally, fetch the schedule based on this student's batch/section
        //fetchAllDays(stdBatch, stdSection);

        String todayStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        st_today.setText("Today: " + todayStr);

        // ... (all your other setup, reading student batch/section, setting up carousel, etc.) ...

        // Convert batch and section as you do
        String firestoreBatch  = convertBatchToFirestoreFormat(stdBatch);
        String firestoreSection = convertSectionToFirestoreFormat(stdSection);

        // The dayList is built, then we call fetchAllDays(...)
        fetchAllDays(firestoreBatch, firestoreSection);


        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // This method is called when user does the "pull down" gesture

            // Reload everything - just like when the app first loads
            // For example:
            String stdBatchg = convertBatchToFirestoreFormat(studentBatchText.getText().toString());
            String stdSectiong = convertSectionToFirestoreFormat(studentSectionText.getText().toString());

            // Clear old data or re-init dayList if you wish
            // Then call fetchAllDays again:
            fetchAllDays(stdBatchg, stdSectiong);
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return (netInfo != null && netInfo.isConnected());
        }
        return false;
    }



    // --- Convert batch string to "batch_59" etc. ---
    private String convertBatchToFirestoreFormat(String batch) {
        if (batch == null) return "batch_unknown";
        batch = batch.trim();
        // Remove trailing "th" if present
        if (batch.toLowerCase().endsWith("th")) {
            batch = batch.replaceAll("(?i)th$", "");
        }
    if (!batch.startsWith("batch_")) {
           batch = "batch_" + batch;
       }
        return batch;
    }

    // --- Convert section string to something like "A", "B", etc. ---
    private String convertSectionToFirestoreFormat(String section) {
        if (section == null) return "A";
        section = section.trim();
        if (section.toLowerCase().startsWith("sec-")) {
            section = section.substring(4);
        }
        if (section.toLowerCase().endsWith("th")) {
            section = section.replaceAll("(?i)th$", "");
        }
        return section.toUpperCase();
    }

    // --- Fetch routine for all 7 days ---
    private void fetchAllDays(String batch, String section) {
        if (batch == null || section == null) {
            Toast.makeText(this, "Error: Student Batch or Section Missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset the dayList to "No Class"
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        // Start recursion with dayIndex=0
        fetchDay(0, batch, section);
    }

    // --- Recursively fetch each day's schedule from Firestore ---
    private void fetchDay(int dayIndex, String batch, String section) {
        // If we've fetched all days, finish up
        if (dayIndex >= daysOfWeek.length) {
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);

            // Center on current day
            int todayIndex = getCurrentDayIndex();
            centerCarouselOn(todayIndex);

            // Show current/next/previous classes
            displayCurrentNextPrev(todayIndex);

            // Show how many classes are available today
            showClassesToday(todayIndex);

            // Show how many tasks are for today
            fetchTasksForToday(batch, section);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // The day name (lowercase in Firestore)
        String dayName = daysOfWeek[dayIndex].toLowerCase();

        // 1) Try to get from CACHE first
        db.collection("schedules").document(dayName)
                .get(Source.CACHE)
                .addOnSuccessListener(docSnapshot -> {
                    // Fill the dayModel from cache if it exists
                    fillDayModel(docSnapshot, dayIndex, batch, section);

                    // If we have internet, also fetch from SERVER to refresh
                    if (isConnected()) {
                        db.collection("schedules").document(dayName)
                                .get(Source.SERVER)
                                .addOnSuccessListener(serverDoc -> {
                                    fillDayModel(serverDoc, dayIndex, batch, section);
                                    fetchDay(dayIndex + 1, batch, section);
                                })
                                .addOnFailureListener(err -> {
                                    // If server fetch fails, just move on
                                    fetchDay(dayIndex + 1, batch, section);
                                });
                    } else {
                        // No internet => show Toast, use cached data
                        Toast.makeText(this, "No Internet. Showing cached data.", Toast.LENGTH_SHORT).show();
                        fetchDay(dayIndex + 1, batch, section);
                    }
                })
                .addOnFailureListener(e -> {
                    // CACHE fetch failed (maybe no cached data)
                    if (isConnected()) {
                        // Try SERVER if connected
                        db.collection("schedules").document(dayName)
                                .get(Source.SERVER)
                                .addOnSuccessListener(serverDoc -> {
                                    fillDayModel(serverDoc, dayIndex, batch, section);
                                    fetchDay(dayIndex + 1, batch, section);
                                })
                                .addOnFailureListener(err -> {
                                    // Server also failed
                                    fetchDay(dayIndex + 1, batch, section);
                                });
                    } else {
                        // No cache AND no internet
                        Toast.makeText(this, "No data in cache and no Internet!", Toast.LENGTH_SHORT).show();
                        fetchDay(dayIndex + 1, batch, section);
                    }
                });
    }


    private void fillDayModel(DocumentSnapshot docSnapshot, int dayIndex, String batch, String section) {
        DayModel dayModel = dayList.get(dayIndex);

        // Reset all timeslots first
        for (int i = 0; i < timeKeys.length; i++) {
            setClassInfo(dayModel, i, "No Class");
        }

        // 1. Load default schedule
        if (docSnapshot.exists() && docSnapshot.getData() != null) {
            Map<String, Object> topLevel = docSnapshot.getData();
            try {
                Map<String, Object> batchData = (Map<String, Object>) topLevel.get(batch);
                if (batchData != null) {
                    Map<String, Object> sectionData = (Map<String, Object>) batchData.get(section);
                    if (sectionData != null) {
                        for (int i = 0; i < timeKeys.length; i++) {
                            String tKey = timeKeys[i];
                            if (sectionData.containsKey(tKey)) {
                                Map<String, Object> slotData = (Map<String, Object>) sectionData.get(tKey);
                                String classInfo = formatClassInfo(slotData);
                                setClassInfo(dayModel, i, classInfo);
                            }
                        }
                    }
                }
            } catch (ClassCastException e) {
                Log.e("Schedule", "Data format error in default schedule", e);
            }
        }

        // 2. Load personalized schedule
        String userId = getSharedPreferences("UnimatePrefs", MODE_PRIVATE)
                .getString("studentEmail", "");

        if (!userId.isEmpty()) {
            // Get personalized data using same structure as default schedules
            db.collection("personalized_schedules").document(userId)
                    .collection("schedules").document(docSnapshot.getId())
                    .collection(batch).document(section)
                    .get()
                    .addOnSuccessListener(personalizedDoc -> {
                        if (personalizedDoc.exists()) {
                            Map<String, Object> personalizedData = personalizedDoc.getData();
                            if (personalizedData != null) {
                                // Merge personalized data with default
                                for (int i = 0; i < timeKeys.length; i++) {
                                    String tKey = timeKeys[i];
                                    if (personalizedData.containsKey(tKey)) {
                                        Map<String, Object> slotData = (Map<String, Object>) personalizedData.get(tKey);
                                        String classInfo = formatClassInfo(slotData);
                                        setClassInfo(dayModel, i, classInfo);
                                    }
                                }
                                // Update UI after merging
                                runOnUiThread(() -> {
                                    dayAdapter.notifyItemChanged(dayIndex);
                                    displayCurrentNextPrev(getCurrentDayIndex());
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Schedule", "Error loading personalized data", e));
        }
    }

    private void loadPersonalizedSchedule(DayModel dayModel, int dayIndex) {
        SharedPreferences prefs = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        String userId = prefs.getString("studentEmail", null);
        if (userId == null) return;

        String dayName = daysOfWeek[dayIndex].toLowerCase();

        db.collection("personalized_schedules").document(userId)
                .collection("days").document(dayName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot personalizedDoc = task.getResult();
                        if (personalizedDoc.exists()) {
                            Map<String, Object> personalizedData = personalizedDoc.getData();
                            if (personalizedData != null) {
                                for (int i = 0; i < timeKeys.length; i++) {
                                    String tKey = timeKeys[i];
                                    if (personalizedData.containsKey(tKey)) {
                                        try {
                                            Map<String, Object> slotData = (Map<String, Object>) personalizedData.get(tKey);
                                            String classInfo = formatClassInfo(slotData);
                                            setClassInfo(dayModel, i, classInfo);
                                        } catch (ClassCastException e) {
                                            Log.e("Schedule", "Invalid personalized data format", e);
                                        }
                                    }
                                }
                                // Update UI
                                runOnUiThread(() -> dayAdapter.notifyItemChanged(dayIndex));
                            }
                        }
                    }
                });
    }

    private String formatClassInfo(Map<String, Object> slotData) {
        String course = safeGetString(slotData, "course");
        String instructor = safeGetString(slotData, "instructor");
        String room = safeGetString(slotData, "room");
        return String.format("%s\n%s\n%s", course, instructor, room);
    }

    private String safeGetString(Map<String, Object> map, String key) {
        // Handle null map case first
        if (map == null) {
            return "N/A";
        }

        // Handle missing key case
        Object value = map.get(key);
        if (value == null) {
            return "N/A";
        }

        // Handle potential null value.toString()
        return value.toString() != null ? value.toString() : "N/A";
    }



    private void showClassesToday(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= dayList.size()) {
            // Safety: if dayIndex is invalid, just show 0 or skip
            st_class.setText("Classes Today: 0");
            return;
        }

        DayModel todayModel = dayList.get(dayIndex);
        int count = 0;
        if (!todayModel.getClass1().equals("No Class")) count++;
        if (!todayModel.getClass2().equals("No Class")) count++;
        if (!todayModel.getClass3().equals("No Class")) count++;
        if (!todayModel.getClass4().equals("No Class")) count++;
        if (!todayModel.getClass5().equals("No Class")) count++;
        if (!todayModel.getClass6().equals("No Class")) count++;
        if (!todayModel.getClass7().equals("No Class")) count++;

        st_class.setText("Classes Today: " + count);
    }

    private void fetchTasksForToday(String fullBatch, String section) {
        // STEP 1: Create "local midnight" for today's date
        String shortBatch = fullBatch;
        if (fullBatch.startsWith("batch_")) {
            shortBatch = fullBatch.substring(6);
            // e.g. "batch_59" -> "59"
        }
        Calendar localMidnight = Calendar.getInstance();
        localMidnight.set(Calendar.HOUR_OF_DAY, 0);
        localMidnight.set(Calendar.MINUTE, 0);
        localMidnight.set(Calendar.SECOND, 0);
        localMidnight.set(Calendar.MILLISECOND, 0);
        Date localStartDate = localMidnight.getTime();

        // STEP 2: Create "local midnight" for tomorrow (end of today range)
        Calendar localTomorrow = (Calendar) localMidnight.clone();
        localTomorrow.add(Calendar.DAY_OF_YEAR, 1);
        Date localEndDate = localTomorrow.getTime();

        // STEP 3: Convert both to UTC
        // (We do the same approach you used: local -> subtract 6 hours if you're UTC+6, etc.)
        // For a robust approach, we can do:
        Calendar calStartUTC = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calStartUTC.setTime(localStartDate);
        // If you’re at UTC+6, that might mean subtracting 6 hours,
        // but to be safe, we just do:
        calStartUTC.add(Calendar.MILLISECOND,
                -calStartUTC.getTimeZone().getOffset(calStartUTC.getTimeInMillis()));

        Calendar calEndUTC = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calEndUTC.setTime(localEndDate);
        calEndUTC.add(Calendar.MILLISECOND,
                -calEndUTC.getTimeZone().getOffset(calEndUTC.getTimeInMillis()));

        // Our final UTC Date objects
        Date utcStart = calStartUTC.getTime();
        Date utcEnd   = calEndUTC.getTime();

        // STEP 4: Query Firestore with the UTC range
        db.collection("tasks")
                .whereGreaterThanOrEqualTo("date", utcStart)
                .whereLessThan("date", utcEnd)
                .whereEqualTo("batch", shortBatch)
                .whereEqualTo("section", section)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int taskCount = querySnapshot.size();
                    st_task.setText("Tasks Today: " + taskCount);
                })
                .addOnFailureListener(e -> {
                    st_task.setText("Tasks Today: 0");
                });
    }



    // --- Helper to store a particular timeslot's class info in our DayModel ---
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

    // --- Read string safely from a Map ---
//    private String safeGetString(Map<String, Object> map, String key) {
//        Object val = map.get(key);
//        return (val == null) ? "N/A" : val.toString();
//    }

    // --- Navigation drawer buttons ---
    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navLogoutButton = findViewById(R.id.navLogoutButton);
        Button contacUs = findViewById(R.id.contacUs);
        Button navAddRetakeButton = findViewById(R.id.navAddRetakeButton);

        if (navHomeButton != null) {
            navHomeButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navLogoutButton != null) {
            navLogoutButton.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                drawerLayout.closeDrawer(GravityCompat.START);

                Intent intent = new Intent(StudentHomePage.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
        if (contacUs != null) {
            contacUs.setOnClickListener(v ->{
                Intent intent = new Intent(StudentHomePage.this, ContactDevelopersActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }


        navAddRetakeButton.setOnClickListener(v -> {
            startActivity(new Intent(StudentHomePage.this, AddRetakeActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        Button navViewPersonalizedButton = findViewById(R.id.navViewPersonalizedButton);
        navViewPersonalizedButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewRetakesActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

    }

    // ------------------------------------------------------
    //   BELOW ARE THE METHODS FOR CURRENT/NEXT/PREV CLASS
    //   (Same logic as in OthersRoutine)
    // ------------------------------------------------------

    /**
     * Returns an index 0..6, where Monday=0, Tuesday=1, ..., Sunday=6.
     */
    private int getCurrentDayIndex() {
        // Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7
        // We'll shift it so Monday=0..Sunday=6
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek + 5) % 7; // e.g., Sunday=1 => index=6, Monday=2 => index=0, etc.
    }

    /**
     * Center the carousel on the given index by adjusting
     * the position to a spot near the middle of Integer range.
     */
    // In StudentHomePage class
    private void centerCarouselOn(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= dayList.size()) {
            dayIndex = 0; // Default to first item if invalid
        }
        carouselRecyclerView.smoothScrollToPosition(dayIndex);
    }

    /**
     * Shows what's happening 'now', plus the next/previous class.
     */
    private void displayCurrentNextPrev(int todayIndex) {
        DayModel todayModel = dayList.get(todayIndex);
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int currentSlotIndex = -1;

        // Find if we are currently within a timeslot
        for (int i = 0; i < timeSlotStart.length; i++) {
            int startMins = parseTimeToMinutes(timeSlotStart[i]);
            int endMins = parseTimeToMinutes(timeSlotEnd[i]);
            if (currentMinutes >= startMins && currentMinutes < endMins) {
                currentSlotIndex = i;
                break;
            }
        }

        if (currentSlotIndex >= 0) {
            // We are inside some slot
            tvCurrentClass.setText(buildClassDisplay(
                    daysOfWeek[todayIndex],
                    0,
                    currentSlotIndex,
                    getClassInfoByIndex(todayModel, currentSlotIndex)));

            // Next
            int nextSlot = findNextNonEmptySlot(todayModel, currentSlotIndex + 1);
            if (nextSlot != -1) {
                tvNextClass.setText(buildClassDisplay(
                        daysOfWeek[todayIndex],
                        0,
                        nextSlot,
                        getClassInfoByIndex(todayModel, nextSlot)));
            } else {


                checkNextDaysForClass(todayIndex);
                Log.d("TAUQIR", "checkNextDaysForClass: " + todayIndex);
            }

            // Previous
            int prevSlot = findPreviousNonEmptySlot(todayModel, currentSlotIndex - 1);
            if (prevSlot != -1) {
                tvPreviousClass.setText(buildClassDisplay(
                        daysOfWeek[todayIndex],
                        0,
                        prevSlot,
                        getClassInfoByIndex(todayModel, prevSlot)));
            } else {
                checkPreviousDaysForClass(todayIndex);
            }

        } else {
            // No ongoing class right now
            Log.d("TAUQIR", "No ongoing class: " + todayIndex);

            tvCurrentClass.setText("No ongoing class");

            // If it's before the first slot
            if (currentMinutes < parseTimeToMinutes(timeSlotStart[0])) {
                // Next slot
                int nextSlot = findNextNonEmptySlot(todayModel, 0);
                if (nextSlot != -1) {
                    tvNextClass.setText(buildClassDisplay(
                            daysOfWeek[todayIndex],
                            0,
                            nextSlot,
                            getClassInfoByIndex(todayModel, nextSlot)));
                } else {
                    Log.d("TAUQIR", "checkNextDaysForClass: " + todayIndex);

                    checkNextDaysForClass(todayIndex);
                }
                checkPreviousDaysForClass(todayIndex);

            }
            // If it's after the last slot
            else if (currentMinutes >= parseTimeToMinutes(timeSlotEnd[timeSlotEnd.length - 1])) {
                int prevSlot = findPreviousNonEmptySlot(todayModel, timeSlotEnd.length - 1);
                if (prevSlot != -1) {
                    tvPreviousClass.setText(buildClassDisplay(
                            daysOfWeek[todayIndex],
                            0,
                            prevSlot,
                            getClassInfoByIndex(todayModel, prevSlot)));
                } else {
                    checkPreviousDaysForClass(todayIndex);
                }
                // Check for the next class in subsequent days
                checkNextDaysForClass(todayIndex);
            }
            // Otherwise, we are between two slots
            else {
                // Find the upcoming slot
                int nextSlot = -1;
                for (int i = 0; i < timeSlotStart.length; i++) {
                    int sMins = parseTimeToMinutes(timeSlotStart[i]);
                    if (currentMinutes < sMins) {
                        nextSlot = i;
                        break;
                    }
                }
                if (nextSlot != -1) {
                    int actualNextSlot = findNextNonEmptySlot(todayModel, nextSlot);
                    if (actualNextSlot != -1) {
                        tvNextClass.setText(buildClassDisplay(
                                daysOfWeek[todayIndex],
                                0,
                                actualNextSlot,
                                getClassInfoByIndex(todayModel, actualNextSlot)));
                    } else {
                        Log.d("TAUQIR", "checkNextDaysForClass: " + todayIndex);

                        checkNextDaysForClass(todayIndex);
                    }
                } else {

                    tvNextClass.setText("None");
                }

                // For previous class, look at the previous day or same day
                int prevDayIndex = (todayIndex - 1 + 7) % 7;
                DayModel prevDay = dayList.get(prevDayIndex);
                int prevSlot = findPreviousNonEmptySlot(prevDay, timeSlotEnd.length - 1);
                if (prevSlot != -1) {
                    tvPreviousClass.setText(buildClassDisplay(
                            daysOfWeek[prevDayIndex],
                            -1,
                            prevSlot,
                            getClassInfoByIndex(prevDay, prevSlot)));
                } else {
                    checkPreviousDaysForClass(todayIndex);
                }
            }
        }
    }
    /**
     * Look up to 7 days ahead to find the next day/slot that has a class.
     */
    private void checkNextDaysForClass(int todayIndex) {
        for (int offset = 1; offset <= 7; offset++) {
            int nextDayIndex = (todayIndex + offset) % 7;
            DayModel nextDay = dayList.get(nextDayIndex);
            int nextSlot = findNextNonEmptySlot(nextDay, 0);

            if (nextSlot != -1) {
                tvNextClass.setText(buildClassDisplay(
                        daysOfWeek[nextDayIndex],
                        offset,
                        nextSlot,
                        getClassInfoByIndex(nextDay, nextSlot)));
                return;
            }
        }
        tvNextClass.setText("None");
    }

    /**
     * Look up to 7 days behind to find the previous day/slot that had a class.
     */
    private void checkPreviousDaysForClass(int todayIndex) {
        for (int offset = 1; offset <= 7; offset++) {
            int prevDayIndex = (todayIndex - offset + 7) % 7;
            DayModel prevDay = dayList.get(prevDayIndex);
            int prevSlot = findPreviousNonEmptySlot(prevDay, timeSlotEnd.length - 1);

            if (prevSlot != -1) {
                tvPreviousClass.setText(buildClassDisplay(
                        daysOfWeek[prevDayIndex],
                        -offset,
                        prevSlot,
                        getClassInfoByIndex(prevDay, prevSlot)));
                return;
            }
        }
        tvPreviousClass.setText("None");
    }

    /**
     * Find the next slot (starting at startIndex) that isn't "No Class".
     */
    private int findNextNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i < timeKeys.length; i++) {
            String classInfo = getClassInfoByIndex(dayModel, i);
            if (!classInfo.equals("No Class")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the previous slot (going backward from startIndex) that isn't "No Class".
     */
    private int findPreviousNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i >= 0; i--) {
            String classInfo = getClassInfoByIndex(dayModel, i);
            if (!classInfo.equals("No Class")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the class info from the DayModel by index 0..6
     */
    private String getClassInfoByIndex(DayModel model, int slotIndex) {
        switch (slotIndex) {
            case 0: return model.getClass1();
            case 1: return model.getClass2();
            case 2: return model.getClass3();
            case 3: return model.getClass4();
            case 4: return model.getClass5();
            case 5: return model.getClass6();
            case 6: return model.getClass7();
            default: return "No Class";
        }
    }

    /**
     * Build a display string that looks like:
     *  Monday, 10 Feb 2025
     *  09:00-10:20AM
     *  CourseName
     *  TeacherName
     *  RoomNo
     */
    private String buildClassDisplay(String dayName, int dayOffset, int slotIndex, String classInfo) {
        String dateStr = getDateWithOffset(dayOffset);
        return dayName + ", " + dateStr
                + "\n" + timeSlotStart[slotIndex] + " - " + timeSlotEnd[slotIndex]
                + "\n" + classInfo;
    }

    /**
     * Returns a string for today's date plus 'offsetDays', e.g. "10 Feb 2025".
     */
    private String getDateWithOffset(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(c.getTime());
    }

    /**
     * Parses a time string like "09:00AM" into minutes from midnight.
     */
    private int parseTimeToMinutes(String timeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mma", Locale.US);
        try {
            Date date = sdf.parse(timeStr);
            if (date != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                return hour * 60 + minute;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
