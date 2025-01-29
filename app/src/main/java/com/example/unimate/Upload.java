package com.example.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

public class Upload extends AppCompatActivity {

    private static final int PICK_CSV_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_upload_page);

        Button uploadCsvButton = findViewById(R.id.uploadCsvButton);

        uploadCsvButton.setOnClickListener(view -> {
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
                try {
                    InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    boolean isFirstRow = true;
                    String lastBuilding = null;
                    String lastFloor = null;

                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    CollectionReference roomsCollection = firestore.collection("rooms");

                    while ((line = reader.readLine()) != null) {
                        if (isFirstRow) {
                            isFirstRow = false; // Skip header row
                            Log.d("CSV_PROCESS", "Skipping header row");
                            continue;
                        }

                        // Split CSV line into fields
                        String[] fields = line.split(",");

                        if (fields.length >= 4) {
                            // Get the values, trimming spaces
                            String building = fields[0].trim();
                            String floor = fields[1].trim();
                            String room = fields[2].trim();
                            String description = fields[3].trim();

                            // Log current values
                            Log.d("CSV_PROCESS", "Read values - Building: " + building + ", Floor: " + floor + ", Room: " + room + ", Description: " + description);

                            // Update the last building and floor if current values are not empty
                            if (!building.isEmpty()) {
                                lastBuilding = building;
                            }
                            if (!floor.isEmpty()) {
                                lastFloor = floor;
                            }

                            // Use the last known building and floor if current values are empty
                            if (building.isEmpty()) {
                                building = lastBuilding;
                            }
                            if (floor.isEmpty()) {
                                floor = lastFloor;
                            }

                            // Log final values to be used
                            Log.d("CSV_PROCESS", "Final values - Building: " + building + ", Floor: " + floor + ", Room: " + room);

                            // Prepare data for Firestore
                            Map<String, Object> roomData = new HashMap<>();
                            roomData.put("description", description);

                            try {
                                // Store data in Firestore using the hierarchical structure
                                roomsCollection
                                        .document(building)
                                        .collection(floor)
                                        .document(room)
                                        .set(roomData)
                                        .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Room added successfully: " + room))
                                        .addOnFailureListener(e -> Log.e("FIRESTORE_ERROR", "Error adding room: " + room, e));
                            } catch (Exception e) {
                                Log.e("FIRESTORE_ERROR", "Exception while adding room", e);
                            }
                        } else {
                            // Log if the row is malformed
                            Log.e("CSV_PROCESS", "Skipping malformed row: " + line);
                        }
                    }


                    inputStream.close();
                    Toast.makeText(this, "CSV data uploaded successfully", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("CSV_ERROR", "Error reading CSV file", e);
                    Toast.makeText(this, "Failed to read the file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
