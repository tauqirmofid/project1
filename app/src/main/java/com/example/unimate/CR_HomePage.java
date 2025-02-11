package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import android.content.SharedPreferences;
import android.content.Intent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CR_HomePage extends AppCompatActivity {
    private ImageView leftNavBarImage;
    private DrawerLayout drawerLayout;
    private RecyclerView carouselRecyclerView;
    private CardView teacherInfo,teacher_routine,rooms,map,task,editRoutine,routine;
    private DayAdapter dayAdapter;
    private List<DayModel> dayList;
    private TextView st_today,st_class,st_task;
    private TextView tvCurrentClass, tvNextClass, tvPreviousClass;
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

    private final String[] timeSlotStart = {
            "09:00AM", "10:20AM", "11:40AM",
            "01:00PM", "01:30PM", "02:50PM", "07:00PM"
    };
    private final String[] timeSlotEnd = {
            "10:20AM", "11:40AM", "01:00PM",
            "01:30PM", "02:50PM", "04:10PM", "08:20PM"
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
        teacher_routine=findViewById(R.id.tcrRoutineCard);
        teacher_routine.setOnClickListener(v->{
            Intent i=new Intent(CR_HomePage.this,TeacherRoutineActivity.class);
            startActivity(i);
        });
        routine=findViewById(R.id.cr_routineCardView);
        routine.setOnClickListener(v->{
            NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView); // Ensure this is the correct ID
            View carouselRecyclerView = findViewById(R.id.carouselRecyclerView); // Ensure this is the correct ID

            if (nestedScrollView != null && carouselRecyclerView != null) {
                // Calculate the Y position of carouselRecyclerView relative to the parent NestedScrollView
                int targetScrollY = carouselRecyclerView.getTop();

                // Scroll smoothly to the calculated Y position
                nestedScrollView.post(() -> nestedScrollView.smoothScrollTo(0, targetScrollY));
            }
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

        editRoutine.setOnClickListener(v->{
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
                Intent i = new Intent(CR_HomePage.this, OthersRoutine.class);
                i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
                i.putExtra("CR_SECTION", crSection);
                startActivity(i);
            });
        }

//        LinearLayout editRoutineLayout = findViewById(R.id.tcrRoutine);
//        if (editRoutineLayout != null) {
//            editRoutineLayout.setOnClickListener(v -> {
//                Intent i = new Intent(CR_HomePage.this, CrCalendar.class);
//                i.putExtra("CR_BATCH", crBatch.replace("batch_", "")); // Remove "batch_"
//                i.putExtra("CR_SECTION", crSection);
//                startActivity(i);
//            });
//        }

        // TextViews for current/next/previous class
        tvCurrentClass = findViewById(R.id.tvCurrentClass);
        tvNextClass = findViewById(R.id.tvNextClass);
        tvPreviousClass = findViewById(R.id.tvPreviousClass);

        st_today = findViewById(R.id.st_today);
        st_class = findViewById(R.id.st_class);
        st_task  = findViewById(R.id.st_task);


        ImageView searchEditText = findViewById(R.id.searchIcon);
        searchEditText.setOnClickListener(v -> {
            startActivity(new Intent(CR_HomePage.this, UniversalSearchActivity.class));
        });
    }
    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navLogoutButton = findViewById(R.id.navLogoutButton);
        Button contacUs = findViewById(R.id.contacUs);

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
        if (contacUs != null) {
            contacUs.setOnClickListener(v ->{
                Intent intent = new Intent(CR_HomePage.this, ContactDevelopersActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
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

        // Reset the dayList to "No Class"
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }

        // Start recursion with dayIndex=0
        fetchDay(0, batch, section);
    }


    private void fetchDay(int dayIndex, String batch, String section) {
        if (dayIndex >= daysOfWeek.length) {
            // All days fetched, update adapter
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);


            // 2) Center on current day
            int todayIndex = getCurrentDayIndex();
            centerCarouselOn(todayIndex);

            // 3) Display current/next/prev
            displayCurrentNextPrev(todayIndex);

            // 4) Show how many classes are available today
            showClassesToday(todayIndex);

            // 5) Fetch the student’s tasks for *today* and show the count
            //String todayStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            fetchTasksForToday(batch, section);

            return;
        }

        // -- otherwise, normal Firestore fetching for each day --
        String dayName = daysOfWeek[dayIndex].toLowerCase();// e.g. "monday", "tuesday"

        db.collection("schedules").document(dayName)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    DayModel dayModel = dayList.get(dayIndex);

                    // Initialize timeslots to "No Class"
                    for (int i = 0; i < timeKeys.length; i++) {
                        setClassInfo(dayModel, i, "No Class");
                    }

                    if (docSnapshot.exists() && docSnapshot.getData() != null) {
                        Map<String, Object> topLevel = docSnapshot.getData();

                        if (topLevel.containsKey(batch)) {
                            Object batchVal = topLevel.get(batch);
                            if (batchVal instanceof Map) {
                                Map<String, Object> sectionsMap = (Map<String, Object>) batchVal;
                                if (sectionsMap.containsKey(section)) {
                                    Object sectionVal = sectionsMap.get(section);
                                    if (sectionVal instanceof Map) {
                                        Map<String, Object> timeslotMap = (Map<String, Object>) sectionVal;

                                        // Fill each timeslot
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
                                    }
                                }
                            }
                        }
                    }

                    // Move on to the next day
                    fetchDay(dayIndex + 1, batch, section);
                })
                .addOnFailureListener(e -> {
                    // Even if fail, move on
                    fetchDay(dayIndex + 1, batch, section);
                });
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




    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "N/A" : val.toString();
    }


    private int getCurrentDayIndex() {
        // Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7
        // We'll shift it so Monday=0..Sunday=6
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek + 5) % 7; // e.g., Sunday=1 => index=6, Monday=2 => index=0, etc.
    }

    private void centerCarouselOn(int dayIndex) {

        // Safety check for nuris phone: If dayIndex is out of range or something is off, skip or default to 0
        if (dayIndex < 0 || dayIndex >= dayList.size()) {


            // ...Or default to 0 (Monday):
            dayIndex = 0;
        }

        int halfMaxValue = Integer.MAX_VALUE / 2;
        int midPos = halfMaxValue - (halfMaxValue % dayList.size());
        int targetPos = midPos + dayIndex;

        carouselRecyclerView.scrollToPosition(targetPos);
        carouselRecyclerView.post(() -> carouselRecyclerView.smoothScrollToPosition(targetPos));
    }

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
    private int findNextNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i < timeKeys.length; i++) {
            String classInfo = getClassInfoByIndex(dayModel, i);
            if (!classInfo.equals("No Class")) {
                return i;
            }
        }
        return -1;
    }
    private int findPreviousNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i >= 0; i--) {
            String classInfo = getClassInfoByIndex(dayModel, i);
            if (!classInfo.equals("No Class")) {
                return i;
            }
        }
        return -1;
    }
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
    private String buildClassDisplay(String dayName, int dayOffset, int slotIndex, String classInfo) {
        String dateStr = getDateWithOffset(dayOffset);
        return dayName + ", " + dateStr
                + "\n" + timeKeys[slotIndex]
                + "\n" + classInfo;
    }
    private String getDateWithOffset(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(c.getTime());
    }
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
