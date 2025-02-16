package com.demo.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class UploadTeacherInfoActivity extends AppCompatActivity {
    private static final int PICK_CSV_FILE = 100;
    Button info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_teacher_info);

        info = findViewById(R.id.uploadTeacherCsvButton);
        info.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/csv");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select CSV File"), PICK_CSV_FILE);
            } catch (Exception e) {
                Toast.makeText(this, "File picker not found", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CSV_FILE && resultCode == RESULT_OK && data != null) {
            Uri csvFileUri = data.getData();

            if (csvFileUri != null) {
                try (InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    String line;
                    int rowCounter = 0; // Track the current row number

                    while ((line = reader.readLine()) != null) {
                        rowCounter++;

                        // Skip the first three rows (header)
                        if (rowCounter <= 3) continue;

                        // Split the line into fields
                        String[] fields = line.split(",");

                        // Ensure the line has at least the expected number of columns
                        if (fields.length >= 8) {
                            String acronym = fields[2].trim().isEmpty() ? "empty" : fields[2].trim();
                            String fullName = fields[3].trim().isEmpty() ? "empty" : fields[3].trim();
                            String department = fields[4].trim();
                            String designation = fields[5].trim(); // Corrected to take the 6th column
                            String cell = fields[6].trim();       // Corrected to take the 7th column
                            String email = fields[7].trim();      // Corrected to take the 8th column

                            // Upload valid rows to Firestore
                            if (!acronym.equals("empty")) {
                                uploadTeacherInfoToFirestore(acronym, fullName, department, designation, cell, email);
                            } else {
                                Log.e("Firestore", "Skipping row with invalid acronym: " + line);
                            }
                        } else {
                            Log.e("CSV_PROCESS", "Skipping malformed row: " + line);
                        }
                    }

                    Toast.makeText(this, "CSV data uploaded successfully", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("CSV_ERROR", "Error reading CSV file", e);
                    Toast.makeText(this, "Failed to read the file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Parses a CSV line respecting quoted fields.
     */
    private String[] parseCSVLine(String line) {
        boolean inQuotes = false;
        StringBuilder fieldBuilder = new StringBuilder();
        java.util.List<String> fields = new java.util.ArrayList<>();

        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes; // Toggle the state
            } else if (ch == ',' && !inQuotes) {
                // Comma outside quotes indicates a field boundary
                fields.add(fieldBuilder.toString().trim());
                fieldBuilder.setLength(0); // Clear the builder
            } else {
                fieldBuilder.append(ch);
            }
        }

        // Add the last field
        fields.add(fieldBuilder.toString().trim());

        return fields.toArray(new String[0]);
    }

    private void uploadTeacherInfoToFirestore(String acronym, String fullName, String department, String designation, String cell, String email) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Prepare the teacher data
        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("full_name", fullName);
        teacherData.put("department", department);
        teacherData.put("designation", designation);
        teacherData.put("cell", cell);
        teacherData.put("email", email);

        // Firestore path: teacher_info -> acronym
        firestore.collection("teacher_info")
                .document(acronym)
                .set(teacherData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Teacher info uploaded successfully for " + acronym))
                .addOnFailureListener(e -> Log.e("Firestore", "Error uploading teacher info for " + acronym, e));
    }
}
