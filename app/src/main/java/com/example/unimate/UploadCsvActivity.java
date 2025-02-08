package com.example.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadCsvActivity extends AppCompatActivity {

    private static final int FILE_PICKER_CODE = 101;

    private Spinner spinnerDays;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnCheckData, btnUploadCsv;

    // Now an ExpandableListView, not a ListView
    private ExpandableListView lvData;

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

        // Note: now we cast lvData as ExpandableListView
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
                // Also fetch data to display in the ExpandableListView
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
                    if (docSnapshot.exists() && docSnapshot.getData() != null && !docSnapshot.getData().isEmpty()) {
                        tvStatus.setText("Data already exists for " + selectedDay
                                + ". Uploading a new CSV will overwrite it.");
                    } else {
                        tvStatus.setText("No data found for " + selectedDay
                                + ". Please upload a CSV.");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Failed to check data: " + e.getMessage());
                });
    }

    /** 2) "View Stored Data" or auto fetching -> now sets ExpandableListView instead of ListView */
    private void fetchAndDisplayDataForDay() {
        String selectedDay = getSelectedDayDocName();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Fetching data for " + selectedDay + "...");

        db.collection("schedules")
                .document(selectedDay)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (!docSnapshot.exists() || docSnapshot.getData() == null
                            || docSnapshot.getData().isEmpty()) {
                        tvStatus.setText("No data found for " + selectedDay);
                        lvData.setAdapter((ExpandableListAdapter) null);
                        return;
                    }

                    Map<String, Object> topLevel = docSnapshot.getData();
                    // Populate the ExpandableListView
                    populateExpandableData(topLevel);

                    tvStatus.setText("Data fetched for " + selectedDay);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Error fetching data: " + e.getMessage());
                });
    }

    /**
     * Instead of building a single list of strings, we build a two-level structure:
     *   Groups (batches) -> Children (multiline section data).
     */
    private void populateExpandableData(Map<String, Object> topLevel) {
        // We'll store the group names (batches) and a map to each batch's list of section strings
        List<String> batchList = new ArrayList<>();
        Map<String, List<String>> batchToSectionsMap = new HashMap<>();

        // Example topLevel:
        //   "batch_64" -> {
        //     "A" -> {"09:00-10:20AM" -> {...}, ... },
        //     "B" -> {...}
        //   },
        //   "batch_65" -> {...}

        for (Map.Entry<String, Object> batchEntry : topLevel.entrySet()) {
            String batchKey = batchEntry.getKey(); // e.g. "batch_64"
            Object batchVal = batchEntry.getValue();
            if (!(batchVal instanceof Map)) {
                continue; // skip if not a map
            }

            batchList.add(batchKey);

            // For each batch, we build a list of multiline strings (one per section)
            Map<String, Object> sectionMap = (Map<String, Object>) batchVal;
            List<String> sectionStrings = new ArrayList<>();

            // sectionMap example:
            //   "A" -> { "09:00-10:20AM" -> {course=..., instructor=..., room=...}, ...}
            //   "B" -> { ... }
            for (Map.Entry<String, Object> sectionEntry : sectionMap.entrySet()) {
                String sectionName = sectionEntry.getKey(); // e.g. "A"
                Object timeslotVal = sectionEntry.getValue();
                if (!(timeslotVal instanceof Map)) {
                    continue;
                }

                // Build a multiline string for this section
                Map<String, Object> timeslotMap = (Map<String, Object>) timeslotVal;
                StringBuilder sb = new StringBuilder();
                sb.append("Section ").append(sectionName).append(":\n");

                // timeslotMap: "09:00-10:20AM" -> {course=..., instructor=..., room=...}
                for (Map.Entry<String, Object> tEntry : timeslotMap.entrySet()) {
                    String timeSlot = tEntry.getKey();
                    Object classVal = tEntry.getValue();
                    if (!(classVal instanceof Map)) {
                        continue;
                    }

                    Map<String, Object> classInfo = (Map<String, Object>) classVal;
                    String course = safeGetString(classInfo, "course");
                    String instructor = safeGetString(classInfo, "instructor");
                    String room = safeGetString(classInfo, "room");

                    sb.append("   ")
                            .append(timeSlot).append(" -> ")
                            .append(course).append(" ")
                            .append(instructor).append(" ")
                            .append(room).append("\n");
                }

                sectionStrings.add(sb.toString().trim());
            }

            batchToSectionsMap.put(batchKey, sectionStrings);
        }

        // Now create and set an ExpandableListAdapter
        MyExpandableListAdapter adapter = new MyExpandableListAdapter(this, batchList, batchToSectionsMap);
        lvData.setAdapter(adapter);
    }

    // A helper to safely get strings from classInfo
    private String safeGetString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val == null) ? "N/A" : val.toString();
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

                // Read full CSV content to handle multi-line quoted fields
                StringBuilder csvBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    csvBuilder.append(line).append("\n");  // Preserve newlines
                }

                // Smartly split lines while preserving quoted multi-line values
                String csvContent = csvBuilder.toString();
                String[] lines = csvContent.split("\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                boolean isDataRow = false;
                Map<String, Map<String, Map<String, Map<String, Object>>>> batches = new HashMap<>();

                for (String fullLine : lines) {
                    fullLine = fullLine.trim();
                    if (fullLine.isEmpty()) continue;

                    // Skip headers
                    if (fullLine.startsWith("SUNDAY,Batch") ||
                            fullLine.startsWith("MONDAY,Batch") ||
                            fullLine.startsWith("TUESDAY,Batch") ||
                            fullLine.startsWith("WEDNESDAY,Batch") ||
                            fullLine.startsWith("THURSDAY,Batch") ||
                            fullLine.startsWith("FRIDAY,Batch") ||
                            fullLine.startsWith("SATURDAY,Batch")) {
                        isDataRow = true;
                        continue;
                    }
                    if (!isDataRow || fullLine.contains("BUS TIME")) {
                        continue;
                    }

                    // Handle CSV splitting while keeping quoted fields intact
                    String[] cells = fullLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (cells.length < 10) continue;

                    // Extract batch & section
                    String batch = cells[1].trim();
                    String section = cells[2].trim();
                    if (batch.isEmpty() || section.isEmpty()) continue;

                    Map<String, Map<String, Object>> sectionData = new HashMap<>();

                    // Process each timeslot
                    for (int i = 0; i < timeColumns.length; i++) {
                        int colIndex = timeColumns[i];
                        if (colIndex >= cells.length) continue;

                        String cellValue = cells[colIndex].replace("\"", "").trim();

                        // ✅ Fix: Merge multi-line values
                        if (cellValue.endsWith(",")) {
                            cellValue += cells[colIndex + 1].replace("\"", "").trim();
                        }

                        if (cellValue.isEmpty() || cellValue.equalsIgnoreCase("B")) {
                            continue;
                        }

                        // Split into components (Course, Instructor, Room)
                        String[] parts = cellValue.split("\\s+");

                        Map<String, Object> classInfo = new HashMap<>();
                        if (parts.length >= 3) {
                            classInfo.put("course", parts[0]);
                            classInfo.put("instructor", parts[1]);
                            classInfo.put("room", parts[2]);
                        } else {
                            classInfo.put("course", "N/A");
                            classInfo.put("instructor", "N/A");
                            classInfo.put("room", cellValue);
                        }

                        sectionData.put(timeSlots[i], classInfo);
                    }

                    if (!sectionData.isEmpty()) {
                        batches
                                .computeIfAbsent(batch, k -> new HashMap<>())
                                .computeIfAbsent(section, k -> new HashMap<>())
                                .putAll(sectionData);
                    }
                }

                // ✅ Upload fixed structured data
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

    // -------------------------------------------------------------
    // INTERNAL ExpandableListAdapter for 2-level display:
    //  1) Group = batch_64, batch_65, ...
    //  2) Child = multiline string showing "Section A" and timeslots
    // -------------------------------------------------------------
    private static class MyExpandableListAdapter extends BaseExpandableListAdapter {

        private final LayoutInflater inflater;
        private final List<String> batchList;  // Group list
        private final Map<String, List<String>> batchToSectionsMap; // Child map

        public MyExpandableListAdapter(
                android.content.Context context,
                List<String> batchList,
                Map<String, List<String>> batchToSectionsMap
        ) {
            this.inflater = LayoutInflater.from(context);
            this.batchList = batchList;
            this.batchToSectionsMap = batchToSectionsMap;
        }

        @Override
        public int getGroupCount() {
            return batchList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            String batchName = batchList.get(groupPosition);
            List<String> sections = batchToSectionsMap.get(batchName);
            return (sections == null) ? 0 : sections.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return batchList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String batchName = batchList.get(groupPosition);
            return batchToSectionsMap.get(batchName).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        // Display the batch (group)
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            String batchName = (String) getGroup(groupPosition);
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }
            TextView textGroup = convertView.findViewById(android.R.id.text1);
            textGroup.setText(batchName); // e.g. "batch_64"
            return convertView;
        }

        // Display the multiline section info (child)
        @Override
        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            String sectionInfo = (String) getChild(groupPosition, childPosition);
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textChild = convertView.findViewById(android.R.id.text1);
            // e.g. "Section A:\n   09:00-10:20AM -> CSE-1151 STA RAB-306\n   ..."
            textChild.setText(sectionInfo);

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
