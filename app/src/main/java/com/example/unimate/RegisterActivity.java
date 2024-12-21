package com.example.unimate;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, studentIdEditText, passwordEditText, confirmPasswordEditText;
    private Spinner departmentSpinner, batchSpinner, sectionSpinner;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        studentIdEditText = findViewById(R.id.studentIdEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        departmentSpinner = findViewById(R.id.departmentSpinner);
        batchSpinner = findViewById(R.id.batchSpinner);
        sectionSpinner = findViewById(R.id.sectionSpinner);
        registerButton = findViewById(R.id.registerButton);

        // Populate spinners
        populateSpinners();

        // Set register button click listener
        registerButton.setOnClickListener(v -> validateInputs());
    }

    private void populateSpinners() {
        // Department dropdown
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"CSE", "EEE", "CEE"});
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentSpinner.setAdapter(departmentAdapter);

        // Batch dropdown
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"56", "57", "58", "59"});
        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        batchSpinner.setAdapter(batchAdapter);

        // Section dropdown
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"A", "B", "C", "D", "E"});
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(sectionAdapter);
    }

    private void validateInputs() {
        boolean isValid = true; // Flag to track if all inputs are valid

        String email = emailEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Email validation
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required!");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address!");
            isValid = false;
        } else {
            emailEditText.setError(null); // Clear previous error
        }

        // Student ID validation
        if (TextUtils.isEmpty(studentId)) {
            studentIdEditText.setError("Student ID is required!");
            isValid = false;
        } else if (!studentId.matches("\\d+")) {
            studentIdEditText.setError("Student ID must contain only numbers!");
            isValid = false;
        } else {
            studentIdEditText.setError(null);
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required!");
            isValid = false;
        } else {
            passwordEditText.setError(null);
        }

        // Confirm Password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Confirm Password is required!");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match!");
            isValid = false;
        } else {
            confirmPasswordEditText.setError(null);
        }

        // Spinner validations (Department, Batch, Section)
        if (departmentSpinner.getSelectedItemPosition() == 0) {
            isValid = false;
            Toast.makeText(this, "Please select a department!", Toast.LENGTH_SHORT).show();
        }

        if (batchSpinner.getSelectedItemPosition() == 0) {
            isValid = false;
            Toast.makeText(this, "Please select a batch!", Toast.LENGTH_SHORT).show();
        }

        if (sectionSpinner.getSelectedItemPosition() == 0) {
            isValid = false;
            Toast.makeText(this, "Please select a section!", Toast.LENGTH_SHORT).show();
        }

        // If all validations pass
        if (isValid) {
            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
        }
    }
}
