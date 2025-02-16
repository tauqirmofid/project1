package com.demo.unimate;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class RoomCSVUpload extends AppCompatActivity {
    private static final int PICK_IMAGE_FILE = 2;
    private Uri selectedImageUri;
    private TextView fileNameTextView;
    private AlertDialog uploadDialog;
    private ProgressDialog progressDialog;


    private static final int PICK_CSV_FILE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_upload_page);

        Button uploadCsvButton = findViewById(R.id.uploadCsvButton);
        Button uploadFloorPicture = findViewById(R.id.uploadfloorPicButton);
        Button uploadRoomPicture = findViewById(R.id.uploadroomPicButton);
        uploadRoomPicture.setOnClickListener(v->{
            showRoomUploadDialog();
        });
        uploadFloorPicture.setOnClickListener(view -> showfloorUploadDialog());
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

    private void showRoomUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_upload_room_pic, null);
        builder.setView(dialogView);
        uploadDialog = builder.create();

        // Initialize UI Elements
        Spinner buildingSpinner = dialogView.findViewById(R.id.buildingSpinner);
        Spinner floorSpinner = dialogView.findViewById(R.id.floorSpinner);
        Spinner roomSpinner = dialogView.findViewById(R.id.roomSpinner);
        Button pickImageButton = dialogView.findViewById(R.id.pickImageButton);
        fileNameTextView = dialogView.findViewById(R.id.fileNameTextView);
        Button uploadButton = dialogView.findViewById(R.id.uploadButton);

        // Load predefined buildings from strings.xml
        ArrayAdapter<CharSequence> buildingAdapter = ArrayAdapter.createFromResource(
                this, R.array.building_options, android.R.layout.simple_spinner_dropdown_item);
        buildingSpinner.setAdapter(buildingAdapter);

        // Load predefined floors from strings.xml
        ArrayAdapter<CharSequence> floorAdapter = ArrayAdapter.createFromResource(
                this, R.array.floor_options, android.R.layout.simple_spinner_dropdown_item);
        floorSpinner.setAdapter(floorAdapter);

        // Handle floor selection to load rooms dynamically
        floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBuilding = buildingSpinner.getSelectedItem().toString();
                String selectedFloor = floorSpinner.getSelectedItem().toString();
                loadRoomsForFloor(selectedBuilding, selectedFloor, roomSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set up the image picker for the "Pick Image" button
        pickImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_FILE);
        });
        uploadButton.setOnClickListener(v -> {
            String selectedBuilding = buildingSpinner.getSelectedItem().toString();
            String selectedFloor = floorSpinner.getSelectedItem().toString();
            String selectedRoom = roomSpinner.getSelectedItem().toString();

            if (selectedImageUri == null) {
                Toast.makeText(RoomCSVUpload.this, "Please select an image first!", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadRoomsImageToCloudinary(selectedImageUri, selectedBuilding, selectedFloor, selectedRoom);
        });

        uploadDialog.show();
    }

    private void uploadRoomsImageToCloudinary(Uri selectedImageUri, String selectedBuilding, String selectedFloor, String selectedRoom) {
        if (selectedImageUri == null || selectedBuilding == null || selectedFloor == null || selectedRoom == null) {
            Toast.makeText(this, "Please ensure all fields and image are selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        String cloudinaryUrl = "https://api.cloudinary.com/v1_1/dp4ha5cws/image/upload";

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] imageData = new byte[inputStream.available()];
            inputStream.read(imageData);
            inputStream.close();

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, cloudinaryUrl,
                    response -> {
                        Log.d("CloudinaryUpload", "Response: " + response);

                        // Extract the public_id (image key) from Cloudinary's response
                        String imageKey = extractImageKey(response);

                        if (imageKey != null) {
                            // Save the image key to Firestore
                            saveImageKeyToRoomDescription(selectedBuilding, selectedFloor, selectedRoom, imageKey);
                        } else {
                            Toast.makeText(RoomCSVUpload.this, "Failed to extract image key!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e("CloudinaryUpload", "Upload Error: " + error.toString());
                        Toast.makeText(RoomCSVUpload.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                    }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("upload_preset", "ml_default");
                    params.put("file", "data:image/jpeg;base64," + android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT));
                    return params;
                }
            };

            requestQueue.add(stringRequest);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveImageKeyToRoomDescription(String building, String floor, String room, String imageKey) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("rooms")
                .document(building)
                .collection(floor)
                .document(room)
                .update("description", imageKey) // Update the description field with the image key
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreUpdate", "Image key added to description successfully for: " + room);
                    Toast.makeText(RoomCSVUpload.this, "Image uploaded and description updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreUpdate", "Error updating description: " + e.getMessage(), e);
                    Toast.makeText(RoomCSVUpload.this, "Failed to update description!", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRoomsForFloor(String building, String floor, Spinner roomSpinner) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("rooms").document(building).collection(floor)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> rooms = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String roomName = document.getId();
                        if (!roomName.equalsIgnoreCase("Image")) { // Skip "Image" field
                            rooms.add(roomName);
                        }
                    }
                    if (rooms.isEmpty()) {
                        rooms.add("No Rooms Available"); // Show message if no rooms are found
                    }

                    // Update roomSpinner
                    ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rooms);
                    roomSpinner.setAdapter(roomAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show();
                });
    }


    private void showfloorUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_upload_floors_pic, null);
        builder.setView(dialogView);
        uploadDialog = builder.create();

        Spinner buildingSpinner = dialogView.findViewById(R.id.buildingSpinner);
        Spinner floorSpinner = dialogView.findViewById(R.id.floorSpinner);
        Button pickImageButton = dialogView.findViewById(R.id.pickImageButton);
        fileNameTextView = dialogView.findViewById(R.id.fileNameTextView);
        Button uploadButton = dialogView.findViewById(R.id.uploadButton);

        // Dropdown Setup
        ArrayAdapter<CharSequence> buildingAdapter = ArrayAdapter.createFromResource(
                this, R.array.building_options, android.R.layout.simple_spinner_dropdown_item);
        buildingSpinner.setAdapter(buildingAdapter);

        ArrayAdapter<CharSequence> floorAdapter = ArrayAdapter.createFromResource(
                this, R.array.floor_options, android.R.layout.simple_spinner_dropdown_item);
        floorSpinner.setAdapter(floorAdapter);

        // Pick Image Button Click
        pickImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_FILE);
        });

        // Upload Button Click (Upload to Cloudinary)
        uploadButton.setOnClickListener(v -> {
            String selectedBuilding = buildingSpinner.getSelectedItem().toString();
            String selectedFloor = floorSpinner.getSelectedItem().toString();

            if (selectedImageUri == null) {
                Toast.makeText(RoomCSVUpload.this, "Please select an image first!", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadfloorImageToCloudinary(selectedImageUri, selectedBuilding, selectedFloor);
        });

        uploadDialog.show();
    }
    private void uploadfloorImageToCloudinary(Uri imageUri, String building, String floor) {
        String cloudinaryUrl = "https://api.cloudinary.com/v1_1/dp4ha5cws/image/upload";

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageData = new byte[inputStream.available()];
            inputStream.read(imageData);
            inputStream.close();

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, cloudinaryUrl,
                    response -> {
                        Log.d("CloudinaryUpload", "Response: " + response);

                        // Extract the public_id (image key) from Cloudinary's response
                        String imageKey = extractImageKey(response);

                        if (imageKey != null) {
                            // Save the image key to Firestore
                            saveImageKeyToFirestore(building, floor, imageKey);
                        } else {
                            Toast.makeText(RoomCSVUpload.this, "Failed to extract image key!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e("CloudinaryUpload", "Upload Error: " + error.toString());
                        Toast.makeText(RoomCSVUpload.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                    }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("upload_preset", "ml_default");
                    params.put("file", "data:image/jpeg;base64," + android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT));
                    return params;
                }
            };

            requestQueue.add(stringRequest);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
        }
    }
    private String extractImageKey(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.getString("public_id"); // Extract the public_id from the response
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void saveImageKeyToFirestore(String building, String floor, String imageKey) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Prepare data for Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("imageKey", imageKey); // Store the image key

        // Set document name as "Image" and overwrite if it exists
        firestore.collection("rooms")
                .document(building)
                .collection(floor)
                .document("Image") // Explicitly set the document name to "Image"
                .set(data) // This will overwrite the existing document
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreSave", "Image key saved successfully under: " + building + " -> " + floor + " -> Image");
                    Toast.makeText(RoomCSVUpload.this, "Image key saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreSave", "Error saving image key: " + e.getMessage(), e);
                    Toast.makeText(RoomCSVUpload.this, "Failed to save image key!", Toast.LENGTH_SHORT).show();
                });
    }
    private String getFileNameFromUri(Uri uri) {
        String fileName = "Unknown";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FileNameError", "Error retrieving file name: " + e.getMessage());
            }
        } else {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
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
                            String building = fields[0].trim();
                            String floor = fields[1].trim();
                            String room = fields[2].trim();
                            String description = fields[3].trim();

                            // Update last known values for building and floor
                            if (!building.isEmpty()) {
                                lastBuilding = building;
                            }
                            if (!floor.isEmpty()) {
                                lastFloor = floor;
                            }

                            // Use the last known values if current ones are empty
                            if (building.isEmpty()) {
                                building = lastBuilding != null ? lastBuilding : "UnknownBuilding";
                            }
                            if (floor.isEmpty()) {
                                floor = lastFloor != null ? lastFloor : "UnknownFloor";
                            }

                            // Log final values
                            Log.d("CSV_FINAL", "Uploading - Building: " + building + ", Floor: " + floor + ", Room: " + room);

                            // Check for empty room names
                            if (room.isEmpty()) {
                                Log.e("CSV_ERROR", "Skipping entry with empty room name.");
                                continue;
                            }

                            // Prepare Firestore data
                            Map<String, Object> roomData = new HashMap<>();
                            roomData.put("description", description);

                            // Add to Firestore
                            roomsCollection
                                    .document(building)
                                    .collection(floor)
                                    .document(room)
                                    .set(roomData)
                                    .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Room added: " + room))
                                    .addOnFailureListener(e -> Log.e("FIRESTORE_ERROR", "Error adding room: " + room, e));
                        }
                        else {
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
        else if (requestCode == PICK_IMAGE_FILE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                String fileName = getFileNameFromUri(selectedImageUri);
                if (fileNameTextView != null) {
                    fileNameTextView.setText(fileName);
                }
            }
        }

    }
}
