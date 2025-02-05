package com.example.unimate;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout teacherLayout, crLayout, studentLayout, guestLayout, adminLayout;
    private Button startButton;
    private String selectedRole = null;  // Tracks which role is currently chosen

    // We’ll also keep references to each role’s TextView
    private TextView teacherText, crText, studentText, guestText, adminText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check if the teacher is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        boolean isTeacherLoggedIn = sharedPreferences.getBoolean("isTeacherLoggedIn", false);
        boolean isAdminLoggedIn = sharedPreferences.getBoolean("isAdminLoggedIn", false);
        boolean isCRLoggedIn = sharedPreferences.getBoolean("isCRLoggedIn", false);
        boolean isStudentLoggedIn = sharedPreferences.getBoolean("isStudentLoggedIn", false);

        if (isTeacherLoggedIn) {
            // Get stored teacher email
            String teacherEmail = sharedPreferences.getString("teacherEmail", "");

            // Redirect to TeacherHomepage
            Intent intent = new Intent(MainActivity.this, TeacherHomepage.class);
            intent.putExtra("teacherEmail", teacherEmail);
            startActivity(intent);
            finish(); // Close MainActivity
        }  else if (isAdminLoggedIn) {
            // Redirect to AdminHomepage
            String adminEmail = sharedPreferences.getString("adminEmail", "");
            Intent intent = new Intent(MainActivity.this, AdminHomePage.class);
            intent.putExtra("adminEmail", adminEmail);
            startActivity(intent);
            finish(); // Close MainActivity
        } else if (isCRLoggedIn) {
            // Redirect to CR_HomePage
            String crEmail = sharedPreferences.getString("crEmail", "");
            String crName = sharedPreferences.getString("crName", "Unknown CR");
            String crBatch = sharedPreferences.getString("crBatch", "N/A");
            String crSection = sharedPreferences.getString("crSection", "N/A");

            Intent intent = new Intent(MainActivity.this, CR_HomePage.class);
            intent.putExtra("CR_NAME", crName);
            intent.putExtra("CR_BATCH", crBatch);
            intent.putExtra("CR_SECTION", crSection);
            startActivity(intent);
            finish();
        }
        else if (isStudentLoggedIn) {
            String stdName = sharedPreferences.getString("studentName", "Student");
            String stdBatch = sharedPreferences.getString("studentBatch", "N/A");
            String stdSection = sharedPreferences.getString("studentSection", "N/A");

            Intent intent = new Intent(MainActivity.this, StudentHomePage.class);
            intent.putExtra("STUDENT_NAME", stdName);
            intent.putExtra("STUDENT_BATCH", stdBatch);
            intent.putExtra("STUDENT_SECTION", stdSection);
            startActivity(intent);
            finish();
        } else {

            // Show normal MainActivity layout if not logged in
            setContentView(R.layout.activity_main);
        }
        // ImageView with slide-down animation
        ImageView selectRoleTopImage = findViewById(R.id.selectRoleTopImage);
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        selectRoleTopImage.startAnimation(slideDown);

        // Bottom layout with slide-up animation
        LinearLayout bottomLayout = findViewById(R.id.bottomLayout);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        bottomLayout.startAnimation(slideUp);

        // Find role layouts
        teacherLayout = findViewById(R.id.teacher);
        crLayout = findViewById(R.id.cr);
        studentLayout = findViewById(R.id.student);
        guestLayout = findViewById(R.id.guest);
        adminLayout = findViewById(R.id.admin);

        // Find the "Let’s Start" button
        startButton = findViewById(R.id.btnLetsStart);

        // Find TextViews inside each role layout:
        // The text is the second child in each layout (childAt(1)).
        teacherText = (TextView) teacherLayout.getChildAt(1);
        crText = (TextView) crLayout.getChildAt(1);
        studentText = (TextView) studentLayout.getChildAt(1);
        guestText = (TextView) guestLayout.getChildAt(1);
        adminText = (TextView) adminLayout.getChildAt(1);

        // Set click listeners for each role layout
        teacherLayout.setOnClickListener(v -> selectRole("teacher"));
        crLayout.setOnClickListener(v -> selectRole("cr"));
        studentLayout.setOnClickListener(v -> selectRole("student"));
        guestLayout.setOnClickListener(v -> selectRole("guest"));
        adminLayout.setOnClickListener(v -> selectRole("admin"));

        // "Let’s Start" button logic
        startButton.setOnClickListener(v -> {
            if (selectedRole == null) {
                // Show a Toast if no role is selected
                Toast.makeText(this, "Please select a role first", Toast.LENGTH_SHORT).show();
            } else {
                // Go to the chosen role’s login page
                switch (selectedRole) {
                    case "teacher":
                        startActivity(new Intent(MainActivity.this, TeacherLoginActivity.class));
                        break;
                    case "cr":
                        startActivity(new Intent(MainActivity.this, CrLoginActivity.class));
                        break;
                    case "student":
                        startActivity(new Intent(MainActivity.this, StudentLoginActivity.class));
                        break;
                    case "guest":
                        startActivity(new Intent(MainActivity.this, Guest_HomePage.class));
                        break;
                    case "admin":
                        startActivity(new Intent(MainActivity.this, AdminLoginActivity.class));
                        break;
                }
            }
        });
    }

    /**
     * Called when a user taps a role. This highlights the chosen layout
     * and updates 'selectedRole'.
     */
    private void selectRole(String role) {
        selectedRole = role;

        // Reset backgrounds and text colors first
        resetAllBackgrounds();

        // Highlight the layout that was tapped
        switch (role) {
            case "teacher":
                highlightSelectedRole(teacherLayout, teacherText);
                break;
            case "cr":
                highlightSelectedRole(crLayout, crText);
                break;
            case "student":
                highlightSelectedRole(studentLayout, studentText);
                break;
            case "guest":
                highlightSelectedRole(guestLayout, guestText);
                break;
            case "admin":
                highlightSelectedRole(adminLayout, adminText);
                break;
        }
    }

    /**
     * Applies a rounded green background to the layout
     * and makes the text green to show it's selected.
     */
    private void highlightSelectedRole(LinearLayout layout, TextView textView) {
        // Create a rounded drawable
        GradientDrawable highlightDrawable = new GradientDrawable();
        highlightDrawable.setShape(GradientDrawable.RECTANGLE);
        highlightDrawable.setColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        highlightDrawable.setCornerRadius(30f); // Adjust corner radius as desired

        // Set the layout background
        layout.setBackground(highlightDrawable);

        // Change text color to green (holo_green_dark or a color of your choice)
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    /**
     * Resets every role layout to a transparent background
     * and its text color back to white.
     */
    private void resetAllBackgrounds() {
        // Reset teacher
        teacherLayout.setBackground(null);
        teacherText.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        // Reset CR
        crLayout.setBackground(null);
        crText.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        // Reset Student
        studentLayout.setBackground(null);
        studentText.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        // Reset Guest
        guestLayout.setBackground(null);
        guestText.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        // Reset Admin
        adminLayout.setBackground(null);
        adminText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }
}
