package com.demo.unimate;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class OthersRoutine extends AppCompatActivity {

    private Spinner spinnerBatch, spinnerSection;
    private RecyclerView carouselRecyclerView;
    private DayAdapter dayAdapter;
    private ProgressDialog progressDialog;
    private TextView tvCurrentClass, tvNextClass, tvPreviousClass;

    // We'll store 7 DayModel objects, one for each day
    private List<DayModel> dayList;
    private final String[] daysOfWeek = {
            "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"
    };

    private Map<String, Set<String>> batchToSectionsMap = new HashMap<>();
    // Firestore
    private FirebaseFirestore db;

    // Timeslot keys in Firestore vs. row order in item_day_card.xml
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
            "09:00AM",
            "10:20AM",
            "11:40AM",
            "01:00PM",
            "01:30PM",
            "02:50PM",
            "07:00PM"
    };
    private final String[] timeSlotEnd = {
            "10:20AM",
            "11:40AM",
            "01:00PM",
            "01:30PM",
            "02:50PM",
            "04:10PM",
            "08:20PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_others_routine);

        ImageView backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading schedules...");
        progressDialog.setCancelable(false);

        // TextViews for current/next/previous class
        tvCurrentClass = findViewById(R.id.tvCurrentClass);
        tvNextClass = findViewById(R.id.tvNextClass);
        tvPreviousClass = findViewById(R.id.tvPreviousClass);

        // RecyclerView
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

        // SnapHelper
        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(carouselRecyclerView);

        // Overlap decoration
        OverlapDecoration decoration = new OverlapDecoration(300);
        carouselRecyclerView.addItemDecoration(decoration);

        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerSection = findViewById(R.id.spinnerSection);
        setupSpinners();
        fetchBatchesAndSections();

        // Fetch schedule initially
        fetchAllDays();
    }

    private void setupSpinners() {
        spinnerBatch.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, new ArrayList<>()));
        spinnerSection.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, new ArrayList<>()));
    }

    private void fetchBatchesAndSections() {
        List<String> days = Arrays.asList("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday");
        int totalDays = days.size();
        AtomicInteger completedDays = new AtomicInteger(0);

        Set<String> allBatches = new HashSet<>();
        batchToSectionsMap.clear();
        progressDialog.show();

        for (String day : days) {
            db.collection("schedules").document(day)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> dayData = documentSnapshot.getData();
                            if (dayData != null) {
                                for (String key : dayData.keySet()) {
                                    if (key.startsWith("batch_")) {
                                        String batchName = key.substring(6);
                                        allBatches.add(batchName);

                                        // Extract sections
                                        Map<String, Object> batchData = (Map<String, Object>) dayData.get(key);
                                        Set<String> sections = batchToSectionsMap.computeIfAbsent(batchName, k -> new HashSet<>());
                                        sections.addAll(batchData.keySet());
                                    }
                                }
                            }
                        }

                        if (completedDays.incrementAndGet() == totalDays) {
                            updateSpinners(new ArrayList<>(allBatches));
                            progressDialog.dismiss();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completedDays.incrementAndGet() == totalDays) {
                            updateSpinners(new ArrayList<>(allBatches));
                            progressDialog.dismiss();
                        }
                    });
        }
    }

    private void updateSpinners(List<String> batches) {
        Collections.sort(batches);
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, batches);
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatch.setAdapter(batchAdapter);

        if (!batches.isEmpty()) {
            spinnerBatch.setSelection(0);
            updateSectionSpinner(batches.get(0));
        }

        spinnerBatch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getSelectedItem() != null) {
                    String selectedBatch = parent.getSelectedItem().toString();
                    updateSectionSpinner(selectedBatch);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSectionSpinner(String batch) {
        Set<String> sections = batchToSectionsMap.getOrDefault(batch, new HashSet<>());
        List<String> sectionList = new ArrayList<>(sections);
        Collections.sort(sectionList);

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, sectionList);
        sectionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);

        if (!sectionList.isEmpty()) {
            spinnerSection.setSelection(0);
        }

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getSelectedItem() != null) {
                    fetchAllDays();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Fetch the schedule for all 7 days, for the currently selected batch & section.
     */
    private void fetchAllDays() {
        if (spinnerBatch.getSelectedItem() == null || spinnerSection.getSelectedItem() == null) {
            return;
        }
        progressDialog.show();

        final String selectedBatch = spinnerBatch.getSelectedItem().toString();
        final String selectedSection = spinnerSection.getSelectedItem().toString();

        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }
        // Start recursion
        fetchDay(0, selectedBatch, selectedSection);
    }

    private void fetchDay(int dayIndex, String batch, String section) {
        if (dayIndex >= daysOfWeek.length) {
            // All done
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);

            int todayIndex = getCurrentDayIndex();
            centerCarouselOn(todayIndex);

            displayCurrentNextPrev(todayIndex);
            progressDialog.dismiss();
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
                    fetchDay(dayIndex + 1, batch, section);
                })
                .addOnFailureListener(e -> {
                    // Even if fail, move on
                    fetchDay(dayIndex + 1, batch, section);
                });
    }

    /** Scroll the carousel to the infinite-mid position for dayIndex. */
    private void centerCarouselOn(int dayIndex) {

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

    /**
     * Sunday=1, Monday=2, ... Saturday=7.
     * daysOfWeek= ["Monday"(0),"Tuesday"(1),...,"Sunday"(6)].
     * We'll map (dayOfWeek+5)%7 so Monday=0...Sunday=6
     */
    private int getCurrentDayIndex() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek + 5) % 7;
    }

    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "N/A" : val.toString();
    }

    // -----------------------------------------
    //    SHOW CURRENT, NEXT, AND PREVIOUS
    // -----------------------------------------
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
        // We'll check from 1 to 7 days ahead
        for (int offset = 1; offset <= 7; offset++) {
            int nextDayIndex = (todayIndex + offset) % 7;
            DayModel nextDay = dayList.get(nextDayIndex);

            int nextSlot = findNextNonEmptySlot(nextDay, 0);
            if (nextSlot != -1) {
                tvNextClass.setText(buildClassDisplay(
                        daysOfWeek[nextDayIndex],
                        offset,
                        nextSlot,
                        getClassInfoByIndex(nextDay, nextSlot)
                ));
                return;
            }
        }
        tvNextClass.setText("None");
    }

    /**
     * Look up to 7 days behind to find the previous day/slot that had a class.
     */
    private void checkPreviousDaysForClass(int todayIndex) {
        // We'll check from 1 to 7 days behind
        for (int offset = 1; offset <= 7; offset++) {
            int prevDayIndex = (todayIndex - offset + 7) % 7;
            DayModel prevDay = dayList.get(prevDayIndex);

            int prevSlot = findPreviousNonEmptySlot(prevDay, timeSlotEnd.length - 1);
            if (prevSlot != -1) {
                tvPreviousClass.setText(buildClassDisplay(
                        daysOfWeek[prevDayIndex],
                        -offset,
                        prevSlot,
                        getClassInfoByIndex(prevDay, prevSlot)
                ));
                return;
            }
        }
        tvPreviousClass.setText("None");
    }

    /**
     * Used to find the next slot that is not "No Class", starting from 'startIndex'.
     */
    private int findNextNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i < timeSlotStart.length; i++) {
            String classInfo = getClassInfoByIndex(dayModel, i);
            if (!classInfo.equals("No Class")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Used to find the previous slot that is not "No Class", going backwards from 'startIndex'.
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
     * Build a display string showing:
     *   - Day name
     *   - Date (based on offset from today)
     *   - Time slot (e.g. "09:00-10:20AM")
     *   - Class info (course, instructor, room)
     */
    private String buildClassDisplay(String dayName, int dayOffset, int slotIndex, String classInfo) {
        // Format the date for the offset from "today"
        String dateStr = getDateWithOffset(dayOffset);

        // e.g. "Monday, 02 Feb 2025\n09:00-10:20AM\nCourse...\nInstructor..."
        return dayName + ", " + dateStr
                + "\n" + timeKeys[slotIndex]
                + "\n" + classInfo;
    }

    /**
     * Returns a string representing today's date plus the given offset (can be negative).
     * Example format: "02 Feb 2025"
     */
    private String getDateWithOffset(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(c.getTime());
    }

    /**
     * Parse times like "09:00AM", "10:20AM", "01:00PM", "07:00PM" into minutes from midnight.
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
        return -1; // error
    }

    /**
     * Returns the correct class info field from the model by slot index.
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
}
