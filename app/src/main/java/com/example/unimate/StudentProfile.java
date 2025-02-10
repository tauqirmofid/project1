package com.example.unimate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class StudentProfile extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView userNameTextView, userEmailTextView, studentIdTextView, departmentTextView, batchTextView, sectionTextView;
    private Button editProfileButton, logoutButton;

    private DatabaseReference studentDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);
        studentIdTextView = findViewById(R.id.studentIdTextView);
        departmentTextView = findViewById(R.id.departmentTextView);
        batchTextView = findViewById(R.id.batchTextView);
        sectionTextView = findViewById(R.id.sectionTextView);

        editProfileButton = findViewById(R.id.editProfileButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Initialize Firebase database reference
        studentDatabase = FirebaseDatabase.getInstance().getReference("Students");

        // Get email from Intent
        String studentEmail = getIntent().getStringExtra("STUDENT_EMAIL");

        // Fetch student details by email
        if (studentEmail != null && !studentEmail.isEmpty()) {
            fetchStudentDetails(studentEmail);
        } else {
            Toast.makeText(this, "No email provided!", Toast.LENGTH_SHORT).show();
        }

        // Set click listeners
        editProfileButton.setOnClickListener(v -> {
            // Handle edit profile action
            Toast.makeText(StudentProfile.this, "Edit Profile clicked!", Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> {
            // Handle logout action
            Toast.makeText(StudentProfile.this, "Logout clicked!", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchStudentDetails(String email) {
        // Query to find student by email
        Query query = studentDatabase.orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Loop through results (though there should be only one result)
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        // Fetch student details
                        String name = studentSnapshot.child("name").getValue(String.class);
                        String studentId = studentSnapshot.getKey(); // Assuming ID is the key
                        String department = studentSnapshot.child("department").getValue(String.class);
                        String batch = studentSnapshot.child("batch").getValue(String.class);
                        String section = studentSnapshot.child("section").getValue(String.class);
                        String fetchedEmail = studentSnapshot.child("email").getValue(String.class);

                        // Set data to views
                        userNameTextView.setText(name != null ? name : "N/A");
                        userEmailTextView.setText(fetchedEmail != null ? fetchedEmail : "N/A");
                        studentIdTextView.setText("ID: " + (studentId != null ? studentId : "N/A"));
                        departmentTextView.setText("Department: " + (department != null ? department : "N/A"));
                        batchTextView.setText("Batch: " + (batch != null ? batch : "N/A"));
                        sectionTextView.setText("Section: " + (section != null ? section : "N/A"));
                    }
                } else {
                    Toast.makeText(StudentProfile.this, "Student not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentProfile.this, "Error fetching student details: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
