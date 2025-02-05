package com.example.unimate;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.CollectionReference;
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
        Button uploadPicture = findViewById(R.id.uploadPicButton);
        uploadPicture.setOnClickListener(view -> showUploadDialog());
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
    private void showLoadingDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false); // Prevent dialog from being dismissed by the user
            progressDialog.setMessage(message);
        }
        progressDialog.show();
    }
    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_upload_rooms_pic, null);
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

            uploadImageToCloudinary(selectedImageUri, selectedBuilding, selectedFloor);
        });

        uploadDialog.show();
    }
    private void uploadImageToCloudinary(Uri imageUri, String building, String floor) {
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
        if (requestCode == PICK_IMAGE_FILE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                String fileName = selectedImageUri.getLastPathSegment();
                fileNameTextView.setText(fileName); // Show file name below button
            }
        }

    }
}
