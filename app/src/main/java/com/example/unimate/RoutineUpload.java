package com.example.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RoutineUpload extends AppCompatActivity {

    private static final int PICK_CSV_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_upload);

        Button uploadCsvButton = findViewById(R.id.uploadCsvButton);

        uploadCsvButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/csv");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Routine CSV"), PICK_CSV_FILE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CSV_FILE && resultCode == RESULT_OK && data != null) {
            Uri csvFileUri = data.getData();
            if (csvFileUri != null) {
                parseAndUploadRoutine(csvFileUri);
            }
        }
    }

    private void parseAndUploadRoutine(Uri csvFileUri) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference routineCollection = firestore.collection("routine");

        try {
            InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            boolean isFirstRow = true;
            String[] timeSlots = null; // To store time slot headers
            String currentDay = null;
            String currentBatch = null;

            Log.d("CSV_PROCESS", "Started reading the CSV file");

            while ((line = reader.readLine()) != null) {
                try {
                    Log.d("CSV_PROCESS", "Processing line: " + line);
                    String[] fields = line.split(",");

                    // Handle the first row (header) to extract time slots
                    if (isFirstRow) {
                        isFirstRow = false;

                        // Validate the header row
                        if (fields.length < 3) {
                            throw new IllegalArgumentException("CSV header row must have at least 3 columns (Day, Batch, Section).");
                        }

                        // Extract time slot headers
                        timeSlots = new String[fields.length - 3];
                        System.arraycopy(fields, 3, timeSlots, 0, fields.length - 3); // Copy D-N columns
                        Log.d("CSV_PROCESS", "Extracted time slots: " + String.join(", ", timeSlots));
                        continue;
                    }

                    // Extract values from the CSV
                    if (fields.length >= 3) {
                        String day = fields[0].trim();
                        String batch = fields[1].trim();
                        String section = fields[2].trim().isEmpty() ? "NoSection" : fields[2].trim();

                        // Update current day and batch if provided
                        if (!day.isEmpty()) {
                            currentDay = day;
                        }
                        if (!batch.isEmpty()) {
                            currentBatch = batch;
                        }

                        // Validate required fields
                        if (currentDay == null || currentBatch == null) {
                            Log.e("CSV_PROCESS", "Missing required fields: Day or Batch is null. Skipping row.");
                            continue;
                        }

                        // Create a map for the time slots
                        Map<String, String> timeSlotMap = new HashMap<>();
                        for (int i = 3; i < fields.length; i++) {
                            String timeSlot = timeSlots[i - 3].trim(); // Map to dynamic time slot
                            String classDetails = fields[i].trim().isEmpty() ? null : fields[i].trim();
                            timeSlotMap.put(timeSlot, classDetails);
                        }

                        Log.d("CSV_PROCESS", "Time slot map for section: " + section + ", Time slots: " + timeSlotMap);

                        // Upload to Firestore
                        String finalCurrentDay = currentDay;
                        String finalCurrentBatch = currentBatch;

                        routineCollection
                                .document(currentDay) // Day document
                                .collection(currentBatch) // Batch collection
                                .document(section) // Section document
                                .set(timeSlotMap) // Time slots map
                                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Routine uploaded for Day: " + finalCurrentDay + ", Batch: " + finalCurrentBatch + ", Section: " + section))
                                .addOnFailureListener(e -> Log.e("FIRESTORE_ERROR", "Error uploading routine for Day: " + finalCurrentDay + ", Batch: " + finalCurrentBatch + ", Section: " + section, e));
                    } else {
                        Log.e("CSV_PROCESS", "Malformed row: " + line);
                    }
                } catch (Exception rowException) {
                    Log.e("ROW_ERROR", "Error processing row: " + line, rowException);
                }
            }

            inputStream.close();
            Toast.makeText(this, "Routine uploaded successfully", Toast.LENGTH_SHORT).show();
            Log.d("CSV_PROCESS", "Routine upload completed");

        } catch (Exception e) {
            Log.e("CSV_ERROR", "Error parsing CSV file", e);
            Toast.makeText(this, "Failed to parse and upload routine", Toast.LENGTH_SHORT).show();
        }
    }

}
