package com.example.unimate;


import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class StudentProfile extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView userNameTextView, userEmailTextView;
    private TextView studentIdTextView, departmentTextView, batchTextView, sectionTextView;
    private Button editProfileButton, logoutButton;

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

        // Suppose these values are fetched from a database or shared preferences
        String userName = "John Doe";
        String userEmail = "john.doe@example.com";
        String studentId = "123456";
        String department = "Computer Science";
        String batch = "2022";
        String section = "A";

        // Set user info
        userNameTextView.setText(userName);
        userEmailTextView.setText(userEmail);
        studentIdTextView.setText("ID: " + studentId);
        departmentTextView.setText("Department: " + department);
        batchTextView.setText("Batch: " + batch);
        sectionTextView.setText("Section: " + section);

        // Click listeners
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle edit profile action
                Toast.makeText(StudentProfile.this, "Edit Profile clicked!", Toast.LENGTH_SHORT).show();
                // You can start an EditProfileActivity here
                // Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                // startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle logout action
                Toast.makeText(StudentProfile.this, "Logout clicked!", Toast.LENGTH_SHORT).show();
                // You could clear user data and go back to a login screen
                // finish();
            }
        });
    }
}
