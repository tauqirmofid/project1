package com.demo.unimate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;  // For debugging
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AddRetakeActivity extends AppCompatActivity {
    private static final String TAG = "AddRetakeActivity";

    private String currentBatch, currentSection;

    // UI Components
    private Spinner batchSpinner, sectionSpinner;
    private CourseAdapter adapter;
    private Button loadCoursesButton;
    private RecyclerView coursesRecyclerView;

    // Data
    private List<String> batchList = new ArrayList<>();
    private List<String> sectionList = new ArrayList<>();
    private Map<String, Set<String>> batchToSectionsMap = new HashMap<>();
    private List<Course> courseList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_retake);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get current student info
        SharedPreferences prefs = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        currentBatch = prefs.getString("studentBatch", "");
        currentSection = prefs.getString("studentSection", "");

        // Initialize UI components
        batchSpinner = findViewById(R.id.batchSpinner);
        sectionSpinner = findViewById(R.id.sectionSpinner);
        loadCoursesButton = findViewById(R.id.loadCoursesButton);
        coursesRecyclerView = findViewById(R.id.coursesRecyclerView);

        // Setup RecyclerView
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(courseList, this::addCourseToPersonalized);
        coursesRecyclerView.setAdapter(adapter);

        // Load data and setup spinners
        fetchBatchesAndSections();

        // Button click listener
        loadCoursesButton.setOnClickListener(v -> loadCourses());
    }

    /**
     * Fetches all available batches and the sections within each batch from the default schedules.
     */
    private void fetchBatchesAndSections() {
        List<String> days = Arrays.asList(
                "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
        );
        // You might have these doc IDs capitalized in Firestore – update if needed.

        AtomicInteger completedDays = new AtomicInteger(0);
        Set<String> allBatches = new HashSet<>();

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

                                        Map<String, Object> batchData =
                                                (Map<String, Object>) dayData.get(key);
                                        if (batchData != null) {
                                            Set<String> sections = batchToSectionsMap
                                                    .computeIfAbsent(batchName, k -> new HashSet<>());
                                            sections.addAll(batchData.keySet());
                                        }
                                    }
                                }
                            }
                        }
                        if (completedDays.incrementAndGet() == days.size()) {
                            batchList.clear();
                            batchList.addAll(allBatches);
                            Collections.sort(batchList);
                            setupSpinners();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch day " + day, e);
                        if (completedDays.incrementAndGet() == days.size()) {
                            batchList.clear();
                            batchList.addAll(allBatches);
                            Collections.sort(batchList);
                            setupSpinners();
                        }
                    });
        }
    }

    /**
     * Sets up the batch and section spinners with the data we have collected.
     */
    private void setupSpinners() {
        // Batch Spinner
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                batchList
        );
        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        batchSpinner.setAdapter(batchAdapter);

        // Set default batch selection if it matches
        if (!currentBatch.isEmpty() && batchList.contains(currentBatch)) {
            int defaultPosition = batchList.indexOf(currentBatch);
            batchSpinner.setSelection(defaultPosition);
        }

        // Section Spinner logic
        batchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id
            ) {
                String selectedBatch = batchList.get(position);
                updateSectionSpinner(selectedBatch);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!batchList.isEmpty()) {
            updateSectionSpinner(batchList.get(0));
        }
    }

    /**
     * Updates the section spinner whenever a new batch is selected.
     */
    private void updateSectionSpinner(String batch) {
        Set<String> sections = batchToSectionsMap.getOrDefault(batch, new HashSet<>());
        sectionList.clear();
        sectionList.addAll(sections);
        Collections.sort(sectionList);

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sectionList
        );
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(sectionAdapter);

        // Set default section selection if it matches
        if (!currentSection.isEmpty() && sectionList.contains(currentSection)) {
            int defaultPosition = sectionList.indexOf(currentSection);
            sectionSpinner.setSelection(defaultPosition);
        }
    }

    /**
     * Loads the courses for the chosen batch and section, then checks for conflicts/existing.
     */
    private void loadCourses() {
        String selectedBatch = "batch_" + batchSpinner.getSelectedItem().toString();
        String selectedSection = sectionSpinner.getSelectedItem().toString();
        courseList.clear();

        db.collection("schedules")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot dayDoc : querySnapshot) {
                        if (!dayDoc.exists()) continue;

                        Map<String, Object> dayData = dayDoc.getData();
                        if (dayData == null) continue;

                        Map<String, Object> batchData =
                                (Map<String, Object>) dayData.get(selectedBatch);
                        if (batchData == null) continue;

                        Map<String, Object> sectionData =
                                (Map<String, Object>) batchData.get(selectedSection);
                        if (sectionData == null) continue;

                        // For each timeSlot
                        for (String timeSlot : sectionData.keySet()) {
                            Map<String, Object> courseData =
                                    (Map<String, Object>) sectionData.get(timeSlot);
                            if (courseData == null) continue;

                            // *** CHANGED ***
                            // We are NOT lowercasing the doc ID. We use it exactly as Firestore has it.
                            String dayId = dayDoc.getId();

                            Course course = new Course(
                                    dayId,                         // Use dayDoc ID as is
                                    timeSlot,
                                    courseData.get("course").toString(),
                                    courseData.get("instructor").toString(),
                                    courseData.get("room").toString()
                            );
                            courseList.add(course);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // After loading the list, check conflicts in both default + personalized.
                    checkConflictsAndExistingCourses();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load courses", e);
                    Toast.makeText(this, "Failed to load courses", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Checks for conflicts and already-added courses in default schedule & user's personalized schedule.
     */
    private void checkConflictsAndExistingCourses() {
        SharedPreferences prefs = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        String userId = prefs.getString("studentEmail", "");
        String userBatch = "batch_" + prefs.getString("studentBatch", "");
        String userSection = prefs.getString("studentSection", "");

        Set<String> existingCourses = new HashSet<>();
        Set<String> conflictingSlots = new HashSet<>();

        AtomicInteger completionCounter = new AtomicInteger(0);

        // 1. Check default schedule
        db.collection("schedules").get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot dayDoc : querySnapshot) {
                if (!dayDoc.exists()) continue;

                // *** CHANGED ***
                // No more day = dayDoc.getId().toLowerCase() or .replaceAll().
                // We keep the doc ID EXACT as Firestore stores it:
                String day = dayDoc.getId();

                Map<String, Object> dayData = dayDoc.getData();
                if (dayData != null && dayData.containsKey(userBatch)) {
                    Map<String, Object> batchData =
                            (Map<String, Object>) dayData.get(userBatch);
                    if (batchData != null && batchData.containsKey(userSection)) {
                        Map<String, Object> sectionData =
                                (Map<String, Object>) batchData.get(userSection);
                        if (sectionData != null) {
                            for (String timeSlot : sectionData.keySet()) {
                                Map<String, Object> courseData =
                                        (Map<String, Object>) sectionData.get(timeSlot);
                                if (courseData == null) continue;

                                // We CAN still normalize the strings for building keys,
                                // but let's not modify the doc ID itself:
                                String normDay = normalizeString(day);
                                String normTimeSlot = normalizeString(timeSlot);
                                String courseName = normalizeString(
                                        courseData.get("course").toString()
                                );

                                // Build keys
                                String slotKey = generateSlotKey(normDay, normTimeSlot);
                                String courseKey = generateCourseKey(normDay, normTimeSlot, courseName);

                                conflictingSlots.add(slotKey);
                                existingCourses.add(courseKey);
                            }
                        }
                    }
                }
            }
            if (completionCounter.incrementAndGet() == 2) {
                updateAdapter(existingCourses, conflictingSlots);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch default schedule", e);
            if (completionCounter.incrementAndGet() == 2) {
                updateAdapter(existingCourses, conflictingSlots);
            }
        });

        // 2. Check personalized schedule
        if (!userId.isEmpty()) {
            fetchPersonalizedSchedule(
                    userId, userBatch, userSection,
                    existingCourses, conflictingSlots, completionCounter
            );
        } else {
            // If there's no user, skip
            if (completionCounter.incrementAndGet() == 2) {
                updateAdapter(existingCourses, conflictingSlots);
            }
        }
    }

    /**
     * Fetches user personalized schedule. Key is to match doc ID EXACTLY, like in your fillDayModel.
     */
    private void fetchPersonalizedSchedule(
            String userId,
            String userBatch,
            String userSection,
            Set<String> existingCourses,
            Set<String> conflictingSlots,
            AtomicInteger completionCounter
    ) {
        db.collection("personalized_schedules")
                .document(userId)
                .collection("schedules")
                .get()
                .addOnSuccessListener(personalizedQuery -> {
                    if (personalizedQuery.isEmpty()) {
                        if (completionCounter.incrementAndGet() == 2) {
                            updateAdapter(existingCourses, conflictingSlots);
                        }
                        return;
                    }

                    AtomicInteger docCounter = new AtomicInteger(0);
                    int totalDocs = personalizedQuery.size();

                    for (DocumentSnapshot dayDoc : personalizedQuery.getDocuments()) {
                        // *** CHANGED ***
                        // Keep EXACT doc name, e.g. "Sunday", not lowercased
                        String dayId = dayDoc.getId();

                        dayDoc.getReference()
                                .collection(userBatch)
                                .document(userSection)
                                .get()
                                .addOnSuccessListener(sectionDoc -> {
                                    if (sectionDoc.exists()) {
                                        Map<String, Object> sectionData = sectionDoc.getData();
                                        if (sectionData != null) {
                                            for (String timeSlot : sectionData.keySet()) {
                                                Map<String, Object> courseData =
                                                        (Map<String, Object>) sectionData.get(timeSlot);
                                                if (courseData == null) continue;

                                                // We do STILL want to normalize for building keys
                                                String normDay = normalizeString(dayId);
                                                String normTimeSlot = normalizeString(timeSlot);
                                                String courseName = normalizeString(
                                                        courseData.get("course").toString()
                                                );

                                                // Build keys
                                                String slotKey = generateSlotKey(normDay, normTimeSlot);
                                                String courseKey = generateCourseKey(normDay, normTimeSlot, courseName);

                                                conflictingSlots.add(slotKey);
                                                existingCourses.add(courseKey);
                                            }
                                        }
                                    }
                                    if (docCounter.incrementAndGet() == totalDocs) {
                                        if (completionCounter.incrementAndGet() == 2) {
                                            updateAdapter(existingCourses, conflictingSlots);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed sub-doc for day " + dayId, e);
                                    if (docCounter.incrementAndGet() == totalDocs) {
                                        if (completionCounter.incrementAndGet() == 2) {
                                            updateAdapter(existingCourses, conflictingSlots);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch personalized schedules for " + userId, e);
                    if (completionCounter.incrementAndGet() == 2) {
                        updateAdapter(existingCourses, conflictingSlots);
                    }
                });
    }

    /**
     * Update the adapter once both default + personalized queries are done.
     */
    private void updateAdapter(Set<String> existingCourses, Set<String> conflictingSlots) {
        Log.d(TAG, "updateAdapter(): existing=" + existingCourses.size()
                + ", conflicts=" + conflictingSlots.size());
        runOnUiThread(() -> adapter.updateCourseStatus(existingCourses, conflictingSlots));
    }

    /**
     * Called when the user taps 'Add' on a course. This updates the personalized schedule in Firestore.
     */
    private void addCourseToPersonalized(Course course) {
        SharedPreferences prefs = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        String userId = prefs.getString("studentEmail", "");
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // doc ID from the schedule is used EXACTLY as Firestore has it
        // BUT we only normalize for building the courseKey/slotKey, not the doc path
        String dayDocId = course.getDay(); // e.g. "Sunday" or "sunday"
        // If your Firestore doc is "Sunday", store it exactly that way in course.getDay().

        String normDay = normalizeString(dayDocId);
        String normTimeSlot = normalizeString(course.getTimeSlot());
        String normCourseName = normalizeString(course.getCourseName());

        String courseKey = generateCourseKey(normDay, normTimeSlot, normCourseName);
        String slotKey = generateSlotKey(normDay, normTimeSlot);

        // If already in set, skip
        if (adapter.existingCourseKeys.contains(courseKey)) {
            Toast.makeText(this, "This course is already in your schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("course", course.getCourseName());
        courseData.put("instructor", course.getInstructor());
        courseData.put("room", course.getRoom());

        // Immediately update UI
        adapter.existingCourseKeys.add(courseKey);
        adapter.conflictingSlotKeys.add(slotKey);
        adapter.notifyDataSetChanged();

        // Write to Firestore
        // *** CHANGED ***
        // Use dayDocId EXACTLY as used in "schedules" collection.
        // If your doc is named "Sunday" in default schedules, do the same here.
        FirebaseFirestore.getInstance()
                .collection("personalized_schedules")
                .document(userId)
                .collection("schedules")
                .document(dayDocId)  // EXACT doc name (e.g. "Sunday")
                // Step A: create the doc with any field
                .set(Collections.singletonMap("initialized", true), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Step B: now set the timeslot in the sub-collection
                    db.collection("personalized_schedules")
                            .document(userId)
                            .collection("schedules")
                            .document(dayDocId)
                            .collection("batch_" + currentBatch)
                            .document(currentSection)
                            .set(Collections.singletonMap(course.getTimeSlot(), courseData), SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Course added successfully!", Toast.LENGTH_SHORT).show();                            })
                            .addOnFailureListener(e -> {
                                // rollback if you like
                            });
                })
                .addOnFailureListener(e -> {
                    // Roll back on failure
                    adapter.existingCourseKeys.remove(courseKey);
                    adapter.conflictingSlotKeys.remove(slotKey);
                    adapter.notifyDataSetChanged();
                    Log.e(TAG, "Failed to add course: " + courseKey, e);
                    Toast.makeText(this, "Failed to add course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Helper: we still do normalizing for timeSlot/courseName because those are used as keys.
    private String normalizeString(String input) {
        if (input == null) return "";
        return input.toLowerCase().replaceAll("\\s+", "");
    }

    private String generateCourseKey(String day, String timeSlot, String courseName) {
        return (day + "_" + timeSlot + "_" + courseName)
                .toLowerCase()
                .replaceAll("\\s+", "");
    }

    private String generateSlotKey(String day, String timeSlot) {
        return (day + "_" + timeSlot)
                .toLowerCase()
                .replaceAll("\\s+", "");
    }
}
