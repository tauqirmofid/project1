package com.example.unimate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
    // We assume these EXACT strings exist in Firestore:
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
    // "9:00-10:20AM" -> start= "9:00AM", end="10:20AM"
    // We’ll parse them as 24-hour times to compare with device clock.
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


    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_others_routine);


        button = findViewById(R.id.btn1);
//        button.setOnClickListener(v -> {
//            Intent intent = new Intent(OthersRoutine.this, CalendarActivity.class);
//            startActivity(intent);
//        });

        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading schedules...");
        progressDialog.setCancelable(false);

        // 2) The three new TextViews for current/next/previous class
        tvCurrentClass = findViewById(R.id.tvCurrentClass);
        tvNextClass = findViewById(R.id.tvNextClass);
        tvPreviousClass = findViewById(R.id.tvPreviousClass);

        // 3) Setup the RecyclerView
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

        // Spinner listeners
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchAllDays();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerBatch = findViewById(R.id.spinnerBatch);
        spinnerSection = findViewById(R.id.spinnerSection);
        setupSpinners();
        fetchBatchesAndSections();



        // Fetch initially
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
        // Show the loading dialog
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
                            progressDialog.dismiss(); // Dismiss dialog when done

                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completedDays.incrementAndGet() == totalDays) {
                            updateSpinners(new ArrayList<>(allBatches));
                            progressDialog.dismiss(); // Dismiss dialog on error

                        }
                    });
        }
    }

    private void updateSpinners(List<String> batches) {
        Collections.sort(batches);
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, batches
        );
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerBatch.setAdapter(batchAdapter);

        // Set default selection only if batches exist
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateSectionSpinner(String batch) {
        Set<String> sections = batchToSectionsMap.getOrDefault(batch, new HashSet<>());
        List<String> sectionList = new ArrayList<>(sections);
        Collections.sort(sectionList);

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, sectionList
        );
        sectionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);

        // Set default selection only if sections exist
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }














    // tauqir // tauqir // tauqir // tauqir
    //tauqir
    //tauqir

    /**
     * Fetch the schedule for all 7 days, for the currently selected batch & section.
     */
    private void fetchAllDays() {
        // Add null checks for spinner selections
        if (spinnerBatch.getSelectedItem() == null || spinnerSection.getSelectedItem() == null) {
            return;
        }
        progressDialog.show(); // Show the loading dialog

        final String selectedBatch = spinnerBatch.getSelectedItem().toString();
        final String selectedSection = spinnerSection.getSelectedItem().toString();

        // Rest of the method remains the same
        dayList = new ArrayList<>();
        for (String dayName : daysOfWeek) {
            dayList.add(new DayModel(dayName));
        }
        fetchDay(0, selectedBatch, selectedSection);
    }

    private void fetchDay(int dayIndex, String batch, String section) {
        if (dayIndex >= daysOfWeek.length) {
            // All done, update adapter & center on current day, then find current/next/previous
            dayAdapter = new DayAdapter(dayList);
            carouselRecyclerView.setAdapter(dayAdapter);

            int todayIndex = getCurrentDayIndex();
            centerCarouselOn(todayIndex);

            // After we loaded today's data, let's find current/next/previous
            displayCurrentNextPrev(todayIndex);
            progressDialog.dismiss(); // Dismiss the loading dialog

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
                                            // put classInfo in dayModel
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
                    // Move on to next day
                    fetchDay(dayIndex + 1, batch, section);
                })
                .addOnFailureListener(e -> {
                    // Even if fail, move on
                    fetchDay(dayIndex + 1, batch, section);
                });
    }

    // --- Time & Day Utils ---

    /** Scroll the carousel to the infinite-mid position for dayIndex. */
    private void centerCarouselOn(int dayIndex) {
        int halfMaxValue = Integer.MAX_VALUE / 2;
        int midPos = halfMaxValue - (halfMaxValue % dayList.size());
        int targetPos = midPos + dayIndex;

        carouselRecyclerView.scrollToPosition(targetPos);
        carouselRecyclerView.post(() -> {
            carouselRecyclerView.smoothScrollToPosition(targetPos);
        });
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

    /**
     * Once we know today's day data, let's parse the time slots and see
     * which slot is "current" (if any), next, and previous.
     */
    private void displayCurrentNextPrev(int todayIndex) {
        DayModel todayModel = dayList.get(todayIndex);
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int currentSlotIndex = -1;

        for (int i = 0; i < timeSlotStart.length; i++) {
            int startMins = parseTimeToMinutes(timeSlotStart[i]);
            int endMins = parseTimeToMinutes(timeSlotEnd[i]);
            if (currentMinutes >= startMins && currentMinutes < endMins) {
                currentSlotIndex = i;
                break;
            }
        }

        if (currentSlotIndex >= 0) {
            String currentClass = getClassInfoByIndex(todayModel, currentSlotIndex);
            tvCurrentClass.setText(currentClass);

            int nextSlot = findNextNonEmptySlot(todayModel, currentSlotIndex + 1);
            if (nextSlot != -1) {
                tvNextClass.setText(getClassInfoByIndex(todayModel, nextSlot));
            } else {
                checkNextDaysForClass(todayIndex);
            }

            int prevSlot = findPreviousNonEmptySlot(todayModel, currentSlotIndex - 1);
            if (prevSlot != -1) {
                tvPreviousClass.setText(getClassInfoByIndex(todayModel, prevSlot));
            } else {
                checkPreviousDaysForClass(todayIndex);
            }
        } else {
            tvCurrentClass.setText("No ongoing class");

            if (currentMinutes < parseTimeToMinutes(timeSlotStart[0])) {
                int nextSlot = findNextNonEmptySlot(todayModel, 0);
                if (nextSlot != -1) {
                    tvNextClass.setText(getClassInfoByIndex(todayModel, nextSlot));
                } else {
                    checkNextDaysForClass(todayIndex);
                }
                // Check previous days for the last class
                checkPreviousDaysForClass(todayIndex); // Added line
            } else if (currentMinutes >= parseTimeToMinutes(timeSlotEnd[timeSlotEnd.length - 1])) {
                int prevSlot = findPreviousNonEmptySlot(todayModel, timeSlotEnd.length - 1);
                if (prevSlot != -1) {
                    tvPreviousClass.setText(getClassInfoByIndex(todayModel, prevSlot));
                } else {
                    checkPreviousDaysForClass(todayIndex);
                }
                tvNextClass.setText("None");
            } else {
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
                        tvNextClass.setText(getClassInfoByIndex(todayModel, actualNextSlot));
                    } else {
                        checkNextDaysForClass(todayIndex);
                    }
                } else {
                    tvNextClass.setText("None");
                }

                int prevDayIndex = (todayIndex - 1 + 7) % 7;
                DayModel prevDay = dayList.get(prevDayIndex);
                int prevSlot = findPreviousNonEmptySlot(prevDay, timeSlotEnd.length - 1);
                if (prevSlot != -1) {
                    tvPreviousClass.setText(getClassInfoByIndex(prevDay, prevSlot));
                } else {
                    checkPreviousDaysForClass(todayIndex);
                }
            }
        }
    }

    private void checkNextDaysForClass(int todayIndex) {
        int nextDayIndex = (todayIndex + 1) % 7;
        for (int i = 0; i < 7; i++) {
            DayModel nextDay = dayList.get(nextDayIndex);
            int nextSlot = findNextNonEmptySlot(nextDay, 0);
            if (nextSlot != -1) {
                tvNextClass.setText(getClassInfoByIndex(nextDay, nextSlot));
                return;
            }
            nextDayIndex = (nextDayIndex + 1) % 7;
        }
        tvNextClass.setText("None");
    }

    private void checkPreviousDaysForClass(int todayIndex) {
        int prevDayIndex = (todayIndex - 1 + 7) % 7;
        for (int i = 0; i < 7; i++) {
            DayModel prevDay = dayList.get(prevDayIndex);
            int prevSlot = findPreviousNonEmptySlot(prevDay, timeSlotEnd.length - 1);
            if (prevSlot != -1) {
                tvPreviousClass.setText(getClassInfoByIndex(prevDay, prevSlot));
                return;
            }
            prevDayIndex = (prevDayIndex - 1 + 7) % 7;
        }
        tvPreviousClass.setText("None");
    }


    private int findNextNonEmptySlot(DayModel dayModel, int startIndex) {
        for (int i = startIndex; i < timeSlotStart.length; i++) {
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

    /**
     * Parse times like "09:00AM", "10:20AM", "01:00PM", "07:00PM" into minutes from midnight.
     * We'll use a SimpleDateFormat with "hh:mma".
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
     * Return dayModel.getClassX by index:
     *   0->getClass1(), 1->getClass2()...
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
