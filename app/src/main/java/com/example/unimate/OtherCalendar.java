package com.example.unimate;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class OtherCalendar extends AppCompatActivity {
    private static final String TAG = "OtherCalendar";
    private TasksAdapterOther TasksAdapterOther;
    private CalendarView calendarView;
    private RecyclerView classRecycler;
    private Spinner batchSpinner, sectionSpinner;
    private LinearLayout loadingLayout;
    private ProgressBar loadingProgressBar;
    private TextView loadingText;
    private final Set<Date> taskDates = new HashSet<>();



    private TextView loadingPercentage;



    private FirebaseFirestore db;
    String selectedBatch = "64";   // default
    String selectedSection = "B";  // default

    // This map will link a specific Date -> list of ClassWithTasks (for that day).
    // We'll fill this for the next 60 days from "today".
    private final Map<Date, List<ClassWithTasks>> classTaskMap = new HashMap<>();
    Date currentSelectedDate;

    private static final int COLOR_TASK_ONLY = R.color.taskOrange;
    private static final int COLOR_CLASS_ONLY = R.color.green_button_color;
    private static final int COLOR_CLASS_AND_TASK = R.drawable.taskclass;
    private LinearLayout emptyDayContainer;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_calendar);

        emptyDayContainer = findViewById(R.id.emptyDayContainer);




        db = FirebaseFirestore.getInstance();

        // Initialize UI
        calendarView = findViewById(R.id.calendarView);
        classRecycler = findViewById(R.id.classRecycler);
        classRecycler.setLayoutManager(new LinearLayoutManager(this));

        batchSpinner = findViewById(R.id.batchSpinner);
        sectionSpinner = findViewById(R.id.sectionSpinner);

        // ✅ Initialize loading views
        loadingLayout = findViewById(R.id.loadingLayout);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingPercentage = findViewById(R.id.loadingPercentage);
        loadingText = findViewById(R.id.loadingText);

        setupSpinners();
        setupCalendar();

        // Load schedules/tasks when we first open
        //  loadAllDataForRange();
    }





    private Date stripTime(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")); // UTC+6
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }



    //tauqir tauqir tauqir tauqir Tauqir tauqir
    // tauqir tauqir eediting time start

    /**
     * Setup the batch & section spinners
     */
    private void setupSpinners() {
        // Initialize with empty adapters
        batchSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, new ArrayList<>()));
        sectionSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, new ArrayList<>()));

        fetchBatchesAndSections();
    }

    private void fetchBatchesAndSections() {
        List<String> days = Arrays.asList("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday");
        int totalDays = days.size();
        AtomicInteger completedDays = new AtomicInteger(0);

        Set<String> allBatches = new HashSet<>();
        Map<String, Set<String>> batchToSections = new HashMap<>();

        for (String day : days) {
            db.collection("schedules").document(day)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> dayData = documentSnapshot.getData();
                            if (dayData != null) {
                                for (String key : dayData.keySet()) {
                                    if (key.startsWith("batch_")) {
                                        String batchName = key.substring(6); // Remove "batch_" prefix
                                        allBatches.add(batchName);

                                        // Extract sections for this batch
                                        Map<String, Object> batchData = (Map<String, Object>) dayData.get(key);
                                        Set<String> sections = batchToSections.computeIfAbsent(batchName, k -> new HashSet<>());
                                        sections.addAll(batchData.keySet());
                                    }
                                }
                            }
                        }

                        if (completedDays.incrementAndGet() == totalDays) {
                            updateSpinners(allBatches, batchToSections);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching day: " + day, e);
                        if (completedDays.incrementAndGet() == totalDays) {
                            updateSpinners(allBatches, batchToSections);
                        }
                    });
        }
    }

    private void updateSpinners(Set<String> allBatches, Map<String, Set<String>> batchToSections) {
        List<String> batchList = new ArrayList<>(allBatches);
        Collections.sort(batchList);

        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, batchList
        );
        batchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        batchSpinner.setAdapter(batchAdapter);

        if (!batchList.isEmpty()) {
            selectedBatch = batchList.get(0);
            batchSpinner.setSelection(0);

            // Set up batch selection listener
            batchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedBatch = parent.getItemAtPosition(position).toString();
                    updateSectionSpinner(batchToSections.get(selectedBatch));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Initial section spinner setup
            updateSectionSpinner(batchToSections.get(selectedBatch));
        }
    }

    private void updateSectionSpinner(Set<String> sections) {
        List<String> sectionList = new ArrayList<>(sections != null ? sections : Collections.emptyList());
        Collections.sort(sectionList);

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, sectionList
        );
        sectionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sectionSpinner.setAdapter(sectionAdapter);

        if (!sectionList.isEmpty()) {
            selectedSection = sectionList.get(0);
            sectionSpinner.setSelection(0);

            // Set up section selection listener
            sectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSection = parent.getItemAtPosition(position).toString();
                    loadAllDataForRange();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Load data for initial section
            loadAllDataForRange();
        }
    }



    /**
     * Set up the calendar click listener
     */
    private void setupCalendar() {
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clicked = eventDay.getCalendar();
            // Convert clicked date to local timezone
            Calendar localCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
            localCal.setTimeInMillis(clicked.getTimeInMillis());
            Date normalized = stripTime(localCal.getTime());
            currentSelectedDate = normalized;
            calendarView.setDate(normalized);
            displayClassesForDate(normalized);
        });
    }

    /**
     * This method loads the class schedules for the next 30 days (including today),
     * plus all tasks for that same range.
     */
    void loadAllDataForRange() {
        // ✅ Show loading UI
        showLoading();
        taskDates.clear();

        // Step 1: Clear old data
        classTaskMap.clear();
        currentSelectedDate = null;
        classRecycler.setAdapter(null);

        // Step 2: Build a list of all dates (Next 30 Days)
        List<Date> dateList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 60; i++) {
            Date dayDate = stripTime(cal.getTime());
            dateList.add(dayDate);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // ✅ Step 3: Start loading classes first, then tasks
        loadClassSchedules(dateList, () -> loadTasksForRange(dateList));
    }


    /**
     * Show the loading layout while data is being fetched.
     */
    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
        classRecycler.setVisibility(View.GONE);
    }

    /**
     * Hide the loading layout and show the RecyclerView once data is ready.
     */
    private void hideLoading() {
        loadingLayout.setVisibility(View.GONE);
        classRecycler.setVisibility(View.VISIBLE);
    }


    /**
     * Load class schedules first, then trigger task loading.
     */
    private void loadClassSchedules(List<Date> dateList, Runnable onComplete) {
        int totalDays = dateList.size();
        final int[] completedRequests = {0}; // Track loaded items

        for (Date dayDate : dateList) {
            String dayName = getDayName(dayDate).toLowerCase();

            db.collection("schedules")
                    .document(dayName)
                    .get()
                    .addOnSuccessListener(doc -> {
                        Date normalized = stripTime(dayDate);
                        if (doc.exists() && doc.getData() != null) {
                            parseClassData(doc.getData(), normalized);
                        } else {
                            classTaskMap.put(normalized, new ArrayList<>());
                        }

                        // ✅ Update Progress
                        updateLoadingProgress(++completedRequests[0], totalDays);

                        // ✅ If all schedules are loaded, move to tasks
                        if (completedRequests[0] == totalDays) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading schedule for " + dayName, e);
                        classTaskMap.put(dayDate, new ArrayList<>());

                        // ✅ Update Progress
                        updateLoadingProgress(++completedRequests[0], totalDays);

                        if (completedRequests[0] == totalDays) {
                            onComplete.run();
                        }
                    });
        }
    }



    /**
     * Load all tasks **after** schedules have been loaded.
     */
    private void loadTasksForRange(List<Date> dateList) {
        // Convert local start/end dates to UTC-based ranges
        Date localStartDate = dateList.get(0);
        Date localEndDate = dateList.get(dateList.size() - 1);

        // Convert local midnight (UTC+6) to UTC start (previous day 18:00 UTC)
        Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calStart.setTime(localStartDate);
        calStart.add(Calendar.HOUR_OF_DAY, -6); // Adjust to UTC
        Date utcStartDate = calStart.getTime();

        // Convert local midnight (UTC+6) to UTC end (same day 18:00 UTC)
        Calendar calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calEnd.setTime(localEndDate);
        calEnd.add(Calendar.HOUR_OF_DAY, 18); // Cover entire local day in UTC
        Date utcEndDate = calEnd.getTime();

        db.collection("tasks")
                .whereEqualTo("batch", selectedBatch)
                .whereEqualTo("section", selectedSection)
                .whereGreaterThanOrEqualTo("date", utcStartDate)
                .whereLessThanOrEqualTo("date", utcEndDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalTasks = querySnapshot.size();
                    final int[] completedTasks = {0};

                    Log.d(TAG, "Found " + totalTasks + " task(s).");

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UniTask task = doc.toObject(UniTask.class);
                        attachTaskToClass(task);

                        // ✅ Update Progress
                        updateLoadingProgress(++completedTasks[0], totalTasks);
                    }

                    // ✅ Hide loading screen after tasks are loaded
                    hideLoading();
                    updateCalendarAppearance();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading tasks", e);
                    hideLoading();
                });
    }



    private void updateLoadingProgress(int completed, int total) {
        if (total == 0) return;

        int percentage = (int) ((completed / (float) total) * 100);
        loadingProgressBar.setProgress(percentage);
        loadingPercentage.setText(percentage + "%");
        loadingText.setText("Loading " + completed + " of " + total);


    }




    /**
     * Convert the Firestore data (nested map) into ClassWithTasks for a specific date
     * and store them in classTaskMap.
     */
    private void parseClassData(Map<String, Object> dayData, Date dayDate) {
        // We'll store classes for "thisDate"
        Date normalized = stripTime(dayDate);

        List<ClassWithTasks> classList = new ArrayList<>();

        String batchKey = "batch_" + selectedBatch;
        if (dayData.containsKey(batchKey)) {
            Object batchObj = dayData.get(batchKey);
            if (batchObj instanceof Map) {
                Map<String, Object> batchMap = (Map<String, Object>) batchObj;

                if (batchMap.containsKey(selectedSection)) {
                    Object sectionObj = batchMap.get(selectedSection);
                    if (sectionObj instanceof Map) {
                        Map<String, Object> timeslotMap = (Map<String, Object>) sectionObj;
                        for (Map.Entry<String, Object> entry : timeslotMap.entrySet()) {
                            String timeSlot = entry.getKey();
                            Object slotVal = entry.getValue();
                            if (slotVal instanceof Map) {
                                Map<String, Object> slotData = (Map<String, Object>) slotVal;

                                String course     = safeGetString(slotData, "course");
                                String instructor = safeGetString(slotData, "instructor");
                                String room       = safeGetString(slotData, "room");

                                ClassWithTasks aClass = new ClassWithTasks(
                                        timeSlot, course, instructor, room
                                );
                                classList.add(aClass);
                            }
                        }
                    }
                }
            }
        }

        // Put it in our map
        classTaskMap.put(normalized, classList);
        updateCalendarAppearance();
    }

    /**
     * Attach a UniTask to the matching class/time for the correct date in classTaskMap.
     */
    private void attachTaskToClass(UniTask task) {
        if (task.getDate() == null) return;

        Date taskDay = stripTime(task.getDate());
        taskDates.add(taskDay);

        Log.d(TAG, "Attaching task => " + task.getTaskTitle()
                + ", date=" + taskDay
                + ", timeSlot=" + task.getClassTime());

        List<ClassWithTasks> dayClasses = classTaskMap.get(taskDay);
        if (dayClasses != null) {
            boolean attached = false;
            for (ClassWithTasks c : dayClasses) {
                if (c.getTimeSlot().equals(task.getClassTime())) {

                    // ✅ Prevent adding the same task multiple times
                    boolean alreadyExists = false;
                    for (UniTask existingTask : c.getTasks()) {
                        if (existingTask.getTaskId().equals(task.getTaskId())) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        c.addTask(task);
                        attached = true;
                    }
                    break;
                }
            }
            Log.d(TAG, "Attached? " + attached);
        } else {
            Log.d(TAG, "No classes found for date=" + taskDay);
        }
    }


    /**
     * Update the calendar dots or day colors based on whether that day has tasks.
     * If a day has at least one task, we color it orange; otherwise, green if it has classes.
     * If it has no classes at all, we won’t add an event (or you can pick a color).
     */
    private void updateCalendarAppearance() {
        List<EventDay> events = new ArrayList<>();

        // Get all dates that have either classes or tasks
        Set<Date> allDates = new HashSet<>();
        allDates.addAll(classTaskMap.keySet());
        allDates.addAll(taskDates);

        for (Date date : allDates) {
            List<ClassWithTasks> classes = classTaskMap.getOrDefault(date, new ArrayList<>());
            boolean hasClass = !classes.isEmpty();
            boolean hasTask = taskDates.contains(date);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int color;
            if (hasClass && hasTask) {
                color = COLOR_CLASS_AND_TASK;
            } else if (hasClass) {
                color = COLOR_CLASS_ONLY;
            } else if (hasTask) {
                color = COLOR_TASK_ONLY;
            } else {
                continue; // Skip dates with neither
            }

            events.add(new EventDay(cal, color));
        }

        calendarView.setEvents(events);


    }






    /**
     * When the user picks a day on the calendar, show the classes for that day in the RecyclerView.
     */
    private void displayClassesForDate(Date date) {
        Date stripped = stripTime(date);
        List<ClassWithTasks> classes = classTaskMap.getOrDefault(stripped, new ArrayList<>());

        classRecycler.setAdapter(null);
        emptyDayContainer.setVisibility(View.GONE);
        classRecycler.setVisibility(View.GONE);

        if (!classes.isEmpty()) {
            showClassesWithTasks(classes);

        } else {
            fetchStandaloneTasks(stripped);
        }
    }

    private void showClassesWithTasks(List<ClassWithTasks> classes) {
        // Sort classes by time slot
        List<String> timeSlotOrder = Arrays.asList(
                "09:00-10:20AM",
                "10:20-11:40AM",
                "11:40-1:00PM",
                "1:00-1:30PM",
                "1:30-2:50PM",
                "2:50-4:10PM",
                "7:00-8:20PM"
        );

        classes.sort((c1, c2) ->
                Integer.compare(
                        timeSlotOrder.indexOf(c1.getTimeSlot().trim()),
                        timeSlotOrder.indexOf(c2.getTimeSlot().trim())
                )
        );

        ClassAdapterOther ClassAdapterOther = new ClassAdapterOther(classes, new ClassAdapterOther.OnClassClickListener() {
            @Override
            public void onClassClick(ClassWithTasks classItem) {
                handleClassClick(classItem);
            }

            @Override
            public void onDeleteClass(ClassWithTasks classItem) {

            }
        }, this);

        classRecycler.setAdapter(ClassAdapterOther);
        classRecycler.setVisibility(View.VISIBLE);
    }

    private void handleClassClick(ClassWithTasks classItem) {
        if (classItem.hasTasks()) {
            showTasksDialog(classItem);
        }
    }


    private void fetchStandaloneTasks(Date date) {
        db.collection("tasks")
                .whereEqualTo("date", date)
                .whereEqualTo("batch", selectedBatch)
                .whereEqualTo("section", selectedSection)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UniTask> standaloneTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UniTask task = doc.toObject(UniTask.class);
                        if (!isTaskAttachedToClass(task)) {
                            standaloneTasks.add(task);
                        }
                    }

                    if (!standaloneTasks.isEmpty()) {
                        showStandaloneTasks(standaloneTasks);


                    }
                });
    }

    private void showStandaloneTasks(List<UniTask> tasks) {
        // Group tasks by time slot
        Map<String, List<UniTask>> groupedTasks = new HashMap<>();
        for (UniTask task : tasks) {
            String timeSlot = task.getClassTime();
            if (!groupedTasks.containsKey(timeSlot)) {
                groupedTasks.put(timeSlot, new ArrayList<>());
            }
            groupedTasks.get(timeSlot).add(task);
        }

        // Convert to ClassWithTasks objects for consistent display
        List<ClassWithTasks> virtualClasses = new ArrayList<>();
        for (Map.Entry<String, List<UniTask>> entry : groupedTasks.entrySet()) {
            ClassWithTasks virtualClass = new ClassWithTasks(
                    entry.getKey(),
                    "Click to See the task",
                    "",
                    ""
            );
            virtualClass.setTasks(entry.getValue());
            virtualClasses.add(virtualClass);
        }

        showClassesWithTasks(virtualClasses);

    }


    private void deleteStandaloneTask(UniTask task) {
        String docId = task.getTaskId();
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "Can't remove task without doc ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task removed", Toast.LENGTH_SHORT).show();

                    // ✅ Check if other standalone tasks exist for this date before removing it
                    Date taskDate = stripTime(task.getDate());

                    db.collection("tasks")
                            .whereEqualTo("date", taskDate)
                            .whereEqualTo("batch", selectedBatch)
                            .whereEqualTo("section", selectedSection)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    taskDates.remove(taskDate); // Only remove if no standalone tasks exist
                                }
                                fetchAllStandaloneTasksAndUpdateCalendar();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error checking remaining tasks", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing task", e);
                    Toast.makeText(this, "Failed to remove task", Toast.LENGTH_SHORT).show();
                });
    }








    private void showTaskDetailsDialog(UniTask task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task_details, null);

        TextView title = dialogView.findViewById(R.id.taskTitle);
        TextView details = dialogView.findViewById(R.id.taskDetails);
        TextView date = dialogView.findViewById(R.id.taskDate);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy • hh:mm a", Locale.getDefault());

        title.setText(task.getTaskTitle());
        details.setText(task.getTaskDetails());
        date.setText(sdf.format(task.getDate()));

        builder.setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private boolean isTaskAttachedToClass(UniTask task) {
        List<ClassWithTasks> classes = classTaskMap.get(stripTime(task.getDate()));
        if (classes != null) {
            for (ClassWithTasks classItem : classes) {
                if (classItem.getTimeSlot().equals(task.getClassTime())) {
                    return true;
                }
            }
        }
        return false;
    }















    private void checkAvailableSlots(Date date, OnSlotCheckListener listener) {
        db.collection("schedules")
                .document(getDayName(date).toLowerCase())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> allTimeSlots = Arrays.asList(
                            "09:00-10:20AM",
                            "10:20-11:40AM",
                            "11:40-1:00PM",
                            "1:00-1:30PM",
                            "1:30-2:50PM",
                            "2:50-4:10PM",
                            "7:00-8:20PM"
                    );

                    List<String> occupiedSlots = new ArrayList<>();
                    if (documentSnapshot.exists()) {
                        Map<String, Object> dayData = documentSnapshot.getData();
                        if (dayData != null) {
                            String batchKey = "batch_" + selectedBatch;
                            if (dayData.containsKey(batchKey)) {
                                Map<String, Object> batchMap = (Map<String, Object>) dayData.get(batchKey);
                                if (batchMap.containsKey(selectedSection)) {
                                    Map<String, Object> sectionMap = (Map<String, Object>) batchMap.get(selectedSection);
                                    occupiedSlots.addAll(sectionMap.keySet());
                                }
                            }
                        }
                    }

                    // Calculate available slots
                    List<String> vacantSlots = new ArrayList<>(allTimeSlots);
                    vacantSlots.removeAll(occupiedSlots);

                    // Callback with result
                    listener.onSlotChecked(!vacantSlots.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching available slots", e);
                    listener.onSlotChecked(false); // Assume no slots available in case of failure
                });
    }

    // Callback Interface
    interface OnSlotCheckListener {
        void onSlotChecked(boolean isVacant);
    }









    private void showAddClassDialog(Date date) {
        db.collection("schedules")
                .document(getDayName(date).toLowerCase())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> allTimeSlots = Arrays.asList(
                            "09:00-10:20AM",
                            "10:20-11:40AM",
                            "11:40-1:00PM",
                            "1:00-1:30PM",
                            "1:30-2:50PM",
                            "2:50-4:10PM",
                            "7:00-8:20PM"
                    );

                    List<String> occupiedSlots = new ArrayList<>();
                    if (documentSnapshot.exists()) {
                        Map<String, Object> dayData = documentSnapshot.getData();
                        if (dayData != null) {
                            String batchKey = "batch_" + selectedBatch;
                            if (dayData.containsKey(batchKey)) {
                                Map<String, Object> batchMap = (Map<String, Object>) dayData.get(batchKey);
                                if (batchMap.containsKey(selectedSection)) {
                                    Map<String, Object> sectionMap = (Map<String, Object>) batchMap.get(selectedSection);
                                    occupiedSlots.addAll(sectionMap.keySet());
                                }
                            }
                        }
                    }
// TAUQIR
                    // TAUQIR// TAUQIR// TAUQIR
                    // TAUQIR// TAUQIR// TAUQIR
                    // TAUQIR// TAUQIR// TAUQIR

                    // TAUQIR// TAUQIR// TAUQIR
                    // TAUQIR// TAUQIR// TAUQIR
                    // Calculate available slots
                    List<String> vacantSlots = new ArrayList<>(allTimeSlots);
                    vacantSlots.removeAll(occupiedSlots);

                    if (vacantSlots.isEmpty()) {
                        Toast.makeText(this, "No vacant time slots available.", Toast.LENGTH_SHORT).show();
                    } else {
                        showAddClassDialogWithAvailableSlots(date, vacantSlots);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching schedule", e);
                    Toast.makeText(this, "Failed to check vacant slots.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays the add class dialog with available time slots.
     */
    private void showAddClassDialogWithAvailableSlots(Date date, List<String> vacantSlots) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_class, null);

        Spinner timeSlotSpinner = dialogView.findViewById(R.id.timeSlotSpinner);
        EditText courseInput = dialogView.findViewById(R.id.courseEditText);
        EditText instructorInput = dialogView.findViewById(R.id.instructorEditText);
        EditText roomInput = dialogView.findViewById(R.id.roomEditText);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vacantSlots);
        timeSlotSpinner.setAdapter(adapter);

        builder.setView(dialogView)
                .setPositiveButton("Add Class", (dialog, which) -> {
                    String selectedTimeSlot = timeSlotSpinner.getSelectedItem().toString();
                    addClassToFirestore(date, selectedTimeSlot, courseInput.getText().toString(),
                            instructorInput.getText().toString(), roomInput.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void addClassToFirestore(Date date, String timeSlot, String course,
                                     String instructor, String room) {
        String dayName = getDayName(date).toLowerCase();

        // Create nested map structure
        Map<String, Object> classData = new HashMap<>();
        classData.put("course", course);
        classData.put("instructor", instructor);
        classData.put("room", room);

        Map<String, Object> timeSlotMap = new HashMap<>();
        timeSlotMap.put(timeSlot, classData);

        Map<String, Object> sectionMap = new HashMap<>();
        sectionMap.put(selectedSection, timeSlotMap);

        Map<String, Object> batchMap = new HashMap<>();
        batchMap.put("batch_" + selectedBatch, sectionMap);

        db.collection("schedules").document(dayName)
                .set(batchMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Class added successfully", Toast.LENGTH_SHORT).show();
                    loadAllDataForRange();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }





    /**
     * Shows a simple dialog with tasks or just the count.
     */
    private void showTaskDetails(ClassWithTasks classItem) {
        // We want to show a dialog with all tasks for this class/time.
        showTasksDialog(classItem);
    }

    private void showTasksDialog(ClassWithTasks classItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_show_tasks, null);

        RecyclerView tasksRecycler = dialogView.findViewById(R.id.tasksRecycler);
        View closeIcon = dialogView.findViewById(R.id.closeIcon);
        View addTaskButton = dialogView.findViewById(R.id.addTaskButton);

        addTaskButton.setVisibility(View.GONE);

        // ✅ Remove duplicate tasks before displaying them
        List<UniTask> uniqueTasks = new ArrayList<>();
        for (UniTask task : classItem.getTasks()) {
            boolean alreadyExists = false;
            for (UniTask existingTask : uniqueTasks) {
                if (existingTask.getTaskId().equals(task.getTaskId())) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                uniqueTasks.add(task);
            }
        }

        // ✅ Use unique tasks for the RecyclerView
        TasksAdapterOther = new TasksAdapterOther(uniqueTasks, taskToRemove -> {
            
        });

        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(TasksAdapterOther);



        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Close icon
        closeIcon.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

















    /**
     * Show a dialog that lets the user add a new task for this class/time.
     */

    //need to make auth for cr to add tasks






    private void fetchAllStandaloneTasksAndUpdateCalendar() {
        Set<Date> tempDates = new HashSet<>(taskDates);

        db.collection("tasks")
                .whereEqualTo("batch", selectedBatch)
                .whereEqualTo("section", selectedSection)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tempDates.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UniTask task = doc.toObject(UniTask.class);
                        Date taskDate = stripTime(task.getDate());
                        if (!isTaskAttachedToClass(task)) {
                            tempDates.add(taskDate);
                        }
                    }
                    taskDates.clear();
                    taskDates.addAll(tempDates);
                    updateCalendarAppearance();

                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching standalone tasks", e));
    }





    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns day of week name for a Date, e.g. "sunday", "monday", ...
     */
    private String getDayName(Date date) {
        return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
    }

    /**
     * Safely get a string from a map
     */
    private String safeGetString(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            Object val = map.get(key);
            if (val instanceof String) {
                return (String) val;
            }
        }
        return "";
    }
}
