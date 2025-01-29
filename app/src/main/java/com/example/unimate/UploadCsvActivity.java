package com.example.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UploadCsvActivity extends AppCompatActivity {

    private static final int FILE_PICKER_CODE = 101;

    private Spinner spinnerDays;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnCheckData, btnUploadCsv;
    private ListView lvData;

    private FirebaseFirestore db;

    // Days for the spinner
    private final String[] daysOfWeek = {
            "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday"
    };

    // Example time slots (you can adjust)
    private final String[] timeSlots = {
            "09:00-10:20AM",  // Column 3
            "10:20-11:40AM",  // Column 4
            "11:40-1:00PM",   // Column 5
            "1:30-2:50PM",    // Column 7
            "2:50-4:10PM",    // Column 8
            "7:00-8:20PM"     // Column 9
    };
    // Matching columns for each slot
    private final int[] timeColumns = {3, 4, 5, 7, 8, 9};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_csv);

        db = FirebaseFirestore.getInstance();

        spinnerDays = findViewById(R.id.spinnerDays);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnCheckData = findViewById(R.id.btnCheckData);
        btnUploadCsv = findViewById(R.id.btnUploadCsv);
        lvData = findViewById(R.id.lvData);

        // Set up spinner
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, daysOfWeek
        );
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDays.setAdapter(dayAdapter);

        // Listen for spinner selections
        spinnerDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Once day is selected, automatically check if data exists
                checkIfDataExistsForDay();
                tvStatus.setText("Selected day: " + daysOfWeek[position]);
                fetchAndDisplayDataForDay();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvStatus.setText("No day selected");
            }
        });

        // Button to view the actual data stored for the selected day
        btnCheckData.setOnClickListener(v -> fetchAndDisplayDataForDay());

        // Button to upload CSV for the selected day
        btnUploadCsv.setOnClickListener(v -> openFilePicker());
    }

    /** 1) Auto-check if data exists whenever the spinner day changes */
    private void checkIfDataExistsForDay() {
        String selectedDay = getSelectedDayDocName();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Checking data for " + selectedDay + "...");

        db.collection("schedules")
                .document(selectedDay)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (docSnapshot.exists() && !docSnapshot.getData().isEmpty()) {
                        // Positive vibe
                        tvStatus.setText("Data already exists for " + selectedDay
                                + ". Uploading a new CSV will overwrite it.");
                    } else {
                        // Negative vibe
                        tvStatus.setText("No data found for " + selectedDay
                                + ". Please upload a CSV.");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Failed to check data: " + e.getMessage());
                });
    }

    /** 2) "View Stored Data" button fetches the doc from Firestore and displays it in a ListView */
    private void fetchAndDisplayDataForDay() {
        String selectedDay = getSelectedDayDocName();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Fetching data for " + selectedDay + "...");

        db.collection("schedules")
                .document(selectedDay)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (!docSnapshot.exists() || docSnapshot.getData() == null ||
                            docSnapshot.getData().isEmpty()) {
                        tvStatus.setText("No data found for " + selectedDay);
                        lvData.setAdapter(null);
                        return;
                    }

                    // We expect a structure like:
                    // {
                    //   "batch_64": {
                    //       "A": {
                    //           "09:00-10:20AM": { "course":"CSE-1151","instructor":"STA","room":"RAB-306"},
                    //           ...
                    //       },
                    //       ...
                    //   },
                    //   "batch_65": { ... }
                    // }
                    Map<String, Object> topLevel = docSnapshot.getData();

                    // Convert map to list of strings
                    ArrayList<String> displayList = parseScheduleData(topLevel);

                    // Show the data in the ListView
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_list_item_1, displayList
                    );
                    lvData.setAdapter(arrayAdapter);
                    tvStatus.setText("Data fetched for " + selectedDay);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Error fetching data: " + e.getMessage());
                });
    }

    /**
     * Recursively or iteratively parse the nested structure and produce strings.
     * The doc shape is:
     *   "batch_XX" -> { "Section" -> { "TimeSlot" -> { "course", "instructor", "room" } } }
     */
    private ArrayList<String> parseScheduleData(Map<String, Object> topLevel) {
        ArrayList<String> result = new ArrayList<>();

        // topLevel keys: "batch_64", "batch_65", ...
        for (Map.Entry<String, Object> batchEntry : topLevel.entrySet()) {
            String batchKey = batchEntry.getKey(); // "batch_64"
            Object batchVal = batchEntry.getValue();

            if (!(batchVal instanceof Map)) continue;  // skip if not a map
            Map<String, Object> sectionMap = (Map<String, Object>) batchVal;

            // sectionMap keys: "A", "B", "C", ...
            for (Map.Entry<String, Object> sectionEntry : sectionMap.entrySet()) {
                String sectionKey = sectionEntry.getKey(); // e.g. "A"
                Object sectionVal = sectionEntry.getValue();

                if (!(sectionVal instanceof Map)) continue;
                Map<String, Object> timeMap = (Map<String, Object>) sectionVal;

                // timeMap keys: "09:00-10:20AM", "10:20-11:40AM", ...
                for (Map.Entry<String, Object> timeEntry : timeMap.entrySet()) {
                    String timeSlot = timeEntry.getKey();
                    Object classVal = timeEntry.getValue();

                    if (!(classVal instanceof Map)) continue;
                    Map<String, Object> classInfo = (Map<String, Object>) classVal;

                    // Example: {course=CSE-1151, instructor=STA, room=RAB-306}
                    String course = safeGetString(classInfo, "course");
                    String instructor = safeGetString(classInfo, "instructor");
                    String room = safeGetString(classInfo, "room");

                    // Build a string for ListView
                    String itemStr = batchKey + " | Section " + sectionKey
                            + " | " + timeSlot
                            + " => [course=" + course + ", instructor=" + instructor + ", room=" + room + "]";

                    result.add(itemStr);
                }
            }
        }
        return result;
    }

    // Helper to safely get string from map
    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "N/A" : val.toString();
    }

    /** 3) "Upload CSV File" button triggers the file picker */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    /** Once a file is picked, parse and upload it */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                parseAndUploadCsv(uri);
            }
        }
    }

    /**
     * Reads CSV, builds nested structure, overwrites the doc for that day.
     */
    private void parseAndUploadCsv(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Uploading CSV...");

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                if (inputStream == null) {
                    throw new IOException("Cannot open inputStream for: " + uri);
                }
                String line;
                boolean isDataRow = false;
                Map<String, Map<String, Map<String, Map<String, Object>>>> batches = new HashMap<>();

                while ((line = reader.readLine()) != null) {
                    // Example of skipping headers or special lines
                    if (line.startsWith("SUNDAY,Batch") ||
                            line.startsWith("MONDAY,Batch") ||
                            line.startsWith("TUESDAY,Batch") ||
                            line.startsWith("WEDNESDAY,Batch") ||
                            line.startsWith("THURSDAY,Batch") ||
                            line.startsWith("FRIDAY,Batch") ||
                            line.startsWith("SATURDAY,Batch")) {
                        isDataRow = true;
                        continue;
                    }
                    if (!isDataRow || line.trim().isEmpty() || line.contains("BUS TIME")) {
                        continue;
                    }

                    // Split CSV properly
                    String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (cells.length < 10) continue;

                    // columns: 0->Day, 1->Batch, 2->Section, 3->09:00..., 4->10:20..., 5->11:40..., 6->break or empty
                    // 7->1:30..., 8->2:50..., 9->7:00-8:20
                    String batch = cells[1].trim();
                    String section = cells[2].trim();
                    if (batch.isEmpty() || section.isEmpty()) continue;

                    Map<String, Map<String, Object>> sectionData = new HashMap<>();

                    // Process each timeslot
                    for (int i = 0; i < timeColumns.length; i++) {
                        int colIndex = timeColumns[i];
                        if (colIndex >= cells.length) continue;

                        String cellValue = cells[colIndex].replace("\"", "").trim();
                        if (cellValue.isEmpty() || cellValue.equalsIgnoreCase("B")) {
                            // break or empty
                            continue;
                        }

                        String[] parts = cellValue.split("\\s+");
                        if (parts.length < 3) {
                            // Possibly "OL Class 7:00pm-8:20pm" or similar
                            Map<String, Object> classInfo = new HashMap<>();
                            classInfo.put("course", "N/A");
                            classInfo.put("instructor", "N/A");
                            classInfo.put("room", cellValue);
                            sectionData.put(timeSlots[i], classInfo);
                            continue;
                        }

                        Map<String, Object> classInfo = new HashMap<>();
                        classInfo.put("course", parts[0]);
                        classInfo.put("instructor", parts[1]);
                        classInfo.put("room", parts[2]);
                        sectionData.put(timeSlots[i], classInfo);
                    }

                    if (!sectionData.isEmpty()) {
                        batches
                                .computeIfAbsent(batch, k -> new HashMap<>())
                                .computeIfAbsent(section, k -> new HashMap<>())
                                .putAll(sectionData);
                    }
                }

                // Overwrite data in Firestore
                uploadToFirestore(batches);

            } catch (IOException e) {
                Log.e("CSV", "Error reading CSV: ", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Error reading CSV: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Writes the nested structure to Firestore,
     * overwriting any existing data for that day (no merges).
     */
    private void uploadToFirestore(Map<String, Map<String, Map<String, Map<String, Object>>>> data) {
        // Flatten top level
        Map<String, Object> firestoreData = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, Object>>>> batchEntry : data.entrySet()) {
            String batchKey = "batch_" + batchEntry.getKey();
            firestoreData.put(batchKey, batchEntry.getValue());
        }

        String docName = getSelectedDayDocName();  // e.g. "sunday"

        db.collection("schedules")
                .document(docName)
                .set(firestoreData) // Overwrites old data
                .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("CSV uploaded successfully for " + docName + "!");
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Upload failed: " + e.getMessage());
                }));
    }

    /** Utility: convert spinner's selection to lowercase for docName. */
    private String getSelectedDayDocName() {
        String selectedDay = spinnerDays.getSelectedItem().toString(); // e.g. "Sunday"
        return selectedDay.toLowerCase();                              // -> "sunday"
    }
}
