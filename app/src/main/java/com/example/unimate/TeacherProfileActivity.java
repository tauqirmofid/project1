package com.example.unimate;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

// Declare Cloudinary instance

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TeacherProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImageEdit,profileImageView;

    private Button changePass;
    private String teacherAcronym;
    private Uri selectedImageUri;
    private TextView nameTextView, emailTextView, acronymView, department, phone, TeacherId, designationView;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile);

        ImageView backButton = findViewById(R.id.leftNavBarImage);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        profileImageView = findViewById(R.id.profileImageView);

        // Initialize TextViews
        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        emailTextView.setOnClickListener(v->{
            showUpdateEmailDialog();
        });
        acronymView = findViewById(R.id.acronymView);
        department = findViewById(R.id.department);
        phone = findViewById(R.id.phone);
        TeacherId = findViewById(R.id.TeacherId);
        designationView = findViewById(R.id.designationView);
        profileImageEdit = findViewById(R.id.editProfilePicture);
        teacherAcronym = getIntent().getStringExtra("acronym");
        databaseReference = FirebaseDatabase.getInstance().getReference("AcceptedRequests/Teachers");
        fetchTeacherDataByAcronym(teacherAcronym);
        // Handle profile image click
        profileImageEdit.setOnClickListener(v -> openGallery());
        loadProfileImage();
        changePass=findViewById(R.id.changePass);
        changePass.setOnClickListener(v->{
            showChangePasswordDialog();
        });
    }
    private void showChangePasswordDialog() {
        // Create a dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_t_change_password);
        dialog.setCancelable(false);

        // Find dialog views
        EditText currentPasswordInput = dialog.findViewById(R.id.currentPasswordInput);
        EditText newPasswordInput = dialog.findViewById(R.id.newPasswordInput);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button updateButton = dialog.findViewById(R.id.updateButton);

        // Handle Cancel button
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Handle Update button
        updateButton.setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(TeacherProfileActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyCurrentPassword(currentPassword, newPassword, dialog);
        });

        dialog.show();
    }
    private void verifyCurrentPassword(String currentPassword, String newPassword, Dialog dialog) {
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("AcceptedRequests").child("Teachers");

        teachersRef.orderByChild("email").equalTo(emailTextView.getText().toString().trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                                String storedHashedPassword = teacherSnapshot.child("password").getValue(String.class);

                                // Check if the entered password matches the stored password
                                if (storedHashedPassword != null && storedHashedPassword.equals(hashPassword(currentPassword))) {
                                    // Allow the user to change the password
                                    updatePassword(teacherSnapshot.getRef(), newPassword, dialog);
                                } else {
                                    Toast.makeText(TeacherProfileActivity.this, "Incorrect current password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(TeacherProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TeacherProfileActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updatePassword(DatabaseReference teacherRef, String newPassword, Dialog dialog) {
        String hashedNewPassword = hashPassword(newPassword);

        teacherRef.child("password").setValue(hashedNewPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TeacherProfileActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TeacherProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                });
    }
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadProfileImage() {
        if (teacherAcronym != null && !teacherAcronym.isEmpty()) {
            databaseReference.orderByChild("acronym").equalTo(teacherAcronym).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                            String imageUrl = teacherSnapshot.child("imageUrl").getValue(String.class);

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                // Use Glide to load the image into the ImageView
                                Glide.with(TeacherProfileActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.sample_image) // Add a default placeholder
                                        .error(R.drawable.sample_image) // Add an error image if loading fails
                                        .into(profileImageView);
                            } else {
                                Toast.makeText(TeacherProfileActivity.this, "No profile image found.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(TeacherProfileActivity.this, "No teacher found with the given acronym.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(TeacherProfileActivity.this, "Failed to load profile image: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Invalid teacher acronym.", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchTeacherDataByAcronym(String acronym) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean teacherFound = false;

                // Iterate through all teachers to find the matching acronym
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String teacherAcronym = teacherSnapshot.child("acronym").getValue(String.class);

                    if (teacherAcronym != null && teacherAcronym.equalsIgnoreCase(acronym)) {
                        // Teacher found; retrieve and display their details
                        teacherFound = true;

                        String name = teacherSnapshot.child("name").getValue(String.class);
                        String email = teacherSnapshot.child("email").getValue(String.class);
                        String dept = teacherSnapshot.child("department").getValue(String.class);
                        String phoneNumber = teacherSnapshot.child("phone").getValue(String.class); // Ensure key is correct
                        String id = teacherSnapshot.getKey();
                        String designation = teacherSnapshot.child("designation").getValue(String.class);

                        // Update TextViews
                        nameTextView.setText(name);
                        emailTextView.setText(email);
                        acronymView.setText(teacherAcronym);
                        department.setText(dept);
                        phone.setText(phoneNumber);
                        TeacherId.setText(id);
                        designationView.setText(designation);

                        break; // Stop searching once the teacher is found
                    }
                }

                if (!teacherFound) {
                    Toast.makeText(TeacherProfileActivity.this, "Teacher not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherProfileActivity.this, "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showUpdateEmailDialog() {
        // Create a dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_update_t_email);
        dialog.setCancelable(false);

        // Find views in the dialog
        EditText newEmailInput = dialog.findViewById(R.id.newEmailInput);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button updateButton = dialog.findViewById(R.id.updateButton);

        // Set click listeners for buttons
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            String updatedEmail = newEmailInput.getText().toString().trim();

            if (!updatedEmail.isEmpty()) {
                // Update email in Firebase
                DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("AcceptedRequests").child("Teachers");
                teachersRef.orderByChild("email").equalTo(emailTextView.getText().toString().trim())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                                        // Update the email field in Firebase
                                        teacherSnapshot.getRef().child("email").setValue(updatedEmail)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(TeacherProfileActivity.this, "Email updated successfully!", Toast.LENGTH_SHORT).show();
                                                    emailTextView.setText(updatedEmail); // Update the UI
                                                    dialog.dismiss(); // Close the dialog
                                                    String updatedEmail = newEmailInput.getText().toString().trim();
                                                    updateemailfirestore(updatedEmail);
                                                    Intent intent = new Intent(TeacherProfileActivity.this, TeacherLoginActivity.class);
                                                    startActivity(intent);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(TeacherProfileActivity.this, "Failed to update email.", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                } else {
                                    Toast.makeText(TeacherProfileActivity.this, "Teacher not found.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(TeacherProfileActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(TeacherProfileActivity.this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateemailfirestore(String updatedEmail) {


        if (!updatedEmail.isEmpty()) {
            // Reference to Firestore teacher_info collection
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference teacherRef = firestore.collection("teacher_info").document(teacherAcronym);

            teacherRef.update("email", updatedEmail)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(TeacherProfileActivity.this, "Email updated successfully!", Toast.LENGTH_SHORT).show();
                        emailTextView.setText(updatedEmail); // Update the UI


                        // Redirect to the login activity
                        Intent intent = new Intent(TeacherProfileActivity.this, TeacherLoginActivity.class);
                        startActivity(intent);
                        finish(); // Close the current activity
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TeacherProfileActivity.this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(TeacherProfileActivity.this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
        }
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Show the dialog with the selected image
            showImageDialog(selectedImageUri);
        }
    }

    private void showImageDialog(Uri imageUri) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_preview);
        dialog.setCancelable(false);

        // Access views in the dialog
        ImageView previewImageView = dialog.findViewById(R.id.profileImageView);
        TextView uploadButton = dialog.findViewById(R.id.done);
        TextView cancelButton = dialog.findViewById(R.id.cancel);

        // Load the image into the ImageView
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            previewImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null && teacherAcronym != null) {
                uploadTeacherImageToCloudinary(selectedImageUri, teacherAcronym);
            } else {
                Toast.makeText(this, "Please select an image and ensure acronym is valid!", Toast.LENGTH_SHORT).show();
            }
        });


        // Handle Cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void uploadTeacherImageToCloudinary(Uri selectedImageUri, String acronym) {
        if (selectedImageUri == null || acronym == null || acronym.isEmpty()) {
            Toast.makeText(this, "Please select an image and ensure acronym is valid!", Toast.LENGTH_SHORT).show();
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

                        // Extract the secure URL (image key) from Cloudinary's response
                        String imageKey = extractImageKey(response);

                        if (imageKey != null) {
                            // Save the image key to Realtime Firebase
                            saveImageKeyToFirebase(acronym, imageKey);
                        } else {
                            Toast.makeText(this, "Failed to extract image key!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e("CloudinaryUpload", "Upload Error: " + error.toString());
                        Toast.makeText(this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                    }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("upload_preset", "ml_default"); // Replace with your unsigned preset
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

    private void saveImageKeyToFirebase(String acronym, String imageKey) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("AcceptedRequests").child("Teachers");

        databaseReference.orderByChild("acronym").equalTo(acronym).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                        teacherSnapshot.getRef().child("imageUrl").setValue(imageKey)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirebaseUpdate", "Image key added successfully!");
                                    Toast.makeText(TeacherProfileActivity.this, "Image uploaded and updated successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirebaseUpdate", "Error updating image URL: " + e.getMessage(), e);
                                    Toast.makeText(TeacherProfileActivity.this, "Failed to update image URL!", Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Toast.makeText(TeacherProfileActivity.this, "No matching teacher found with the acronym!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseQuery", "Error querying database: " + databaseError.getMessage());
            }
        });
    }

    private String extractImageKey(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getString("secure_url"); // Extract the secure URL from the Cloudinary response
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}