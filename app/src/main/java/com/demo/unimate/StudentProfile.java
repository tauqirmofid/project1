package com.demo.unimate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class StudentProfile extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView userNameTextView, userEmailTextView, studentIdTextView, departmentTextView, batchTextView, sectionTextView;
    private Button editProfileButton, changePass;

    private DatabaseReference studentDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        ImageView backButton = findViewById(R.id.leftNavBarImage);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);
        studentIdTextView = findViewById(R.id.studentIdTextView);
        departmentTextView = findViewById(R.id.departmentTextView);
        batchTextView = findViewById(R.id.batchTextView);
        sectionTextView = findViewById(R.id.sectionTextView);

        editProfileButton = findViewById(R.id.editProfileButton);
        changePass=findViewById(R.id.change_password);
//        changePass.setOnClickListener(v -> {
//            showChangePasswordDialog();
//        });

        // Initialize Firebase database reference
        studentDatabase = FirebaseDatabase.getInstance().getReference("Students");

        // Get email from Intent
        String studentEmail = getIntent().getStringExtra("STUDENT_EMAIL");
        if (studentEmail == null || studentEmail.isEmpty()) {
            // Try retrieving from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
            studentEmail = sharedPreferences.getString("studentEmail", null);
        }
        // Fetch student details by email
        if (studentEmail != null && !studentEmail.isEmpty()) {
            fetchStudentDetails(studentEmail);
        } else {
            Toast.makeText(this, "No email provided!", Toast.LENGTH_SHORT).show();
        }

        editProfileButton.setOnClickListener(v -> {
            // Create a dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit Profile");

            // Inflate custom layout
            View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
            builder.setView(view);

            // Get references to EditText fields
            EditText etName = view.findViewById(R.id.et_name);
            EditText etBatch = view.findViewById(R.id.et_batch);
            EditText etSection = view.findViewById(R.id.et_section);

            // Get current values from TextViews (or from SharedPreferences)
            String currentName = userNameTextView.getText().toString();
            String currentBatch = batchTextView.getText().toString();
            String currentSection = sectionTextView.getText().toString();

            // Set current values to EditTexts
            etName.setText(currentName);
            etBatch.setText(currentBatch);
            etSection.setText(currentSection);

            // Set up buttons
            builder.setPositiveButton("Update", (dialog, which) -> {
                // Get updated values
                String newName = etName.getText().toString().trim();
                String newBatch = etBatch.getText().toString().trim();
                String newSection = etSection.getText().toString().trim();

                // Validate input
                if (!newName.isEmpty() && !newBatch.isEmpty() && !newSection.isEmpty()) {
                    updateStudentProfile(newName, newBatch, newSection);
                } else {
                    Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        });



    }

    private void showChangePasswordDialog() {
        // Create a dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_s_change_password);
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
                Toast.makeText(StudentProfile.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(StudentProfile.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyCurrentPassword(currentPassword, newPassword, dialog);
        });

        dialog.show();
    }
    private void verifyCurrentPassword(String currentPassword, String newPassword, Dialog dialog) {
        String studentEmail = userEmailTextView.getText().toString().trim();

        if (studentEmail.isEmpty()) {
            Toast.makeText(this, "Error: No email found!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        studentsRef.orderByChild("email").equalTo(studentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                String storedPassword = studentSnapshot.child("password").getValue(String.class);

                                // Ensure we are comparing hashed passwords
                                String hashedInputPassword = hashPassword(currentPassword);

                                if (storedPassword != null && storedPassword.equals(hashedInputPassword)) {
                                    updatePassword(studentSnapshot.getRef(), newPassword, dialog);
                                } else {
                                    Toast.makeText(StudentProfile.this, "Incorrect current password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(StudentProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentProfile.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password; // Return plain password if hashing fails (not recommended)
        }
    }

    private void updatePassword(DatabaseReference studentRef, String newPassword, Dialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("password", hashPassword(newPassword));  // Store hashed password

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StudentProfile.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentProfile.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStudentProfile(String newName, String newBatch, String newSection) {
        String email = userEmailTextView.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Error: No email found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove "Batch " and "Section " prefixes
        newBatch = newBatch.replace("Batch ", "").trim();
        newSection = newSection.replace("Section ", "").trim();

        // Query to find student by email
        Query query = studentDatabase.orderByChild("email").equalTo(email);

        String finalNewBatch = newBatch;
        String finalNewSection = newSection;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        String studentId = studentSnapshot.getKey(); // Get the student's unique ID

                        if (studentId != null) {
                            // Prepare the updated data
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("name", newName);
                            updates.put("batch", finalNewBatch);  // Now only stores "60"
                            updates.put("section", finalNewSection); // Now only stores "B"

                            // Update in database
                            studentDatabase.child(studentId).updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update UI
                                        userNameTextView.setText(newName);
                                        batchTextView.setText(finalNewBatch); // Display "60"
                                        sectionTextView.setText(finalNewSection); // Display "B"

                                        Toast.makeText(StudentProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentProfile.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                } else {
                    Toast.makeText(StudentProfile.this, "Student not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentProfile.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchStudentDetails(String studentEmail) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Students");

        studentRef.orderByChild("email").equalTo(studentEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String batch = snapshot.child("batch").getValue(String.class);
                        String section = snapshot.child("section").getValue(String.class);
                        String department = snapshot.child("department").getValue(String.class);

                        // Set values to UI
                        userNameTextView.setText(name);
                        userEmailTextView.setText(studentEmail);
                        studentIdTextView.setText( snapshot.getKey());
                        departmentTextView.setText( department);
                        batchTextView.setText(batch);
                        sectionTextView.setText( section);
                    }
                } else {
                    Toast.makeText(StudentProfile.this, "Student not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentProfile.this, "Error fetching student details!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
