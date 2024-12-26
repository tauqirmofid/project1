package com.example.unimate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TeacherRegistrationActivity extends AppCompatActivity {

    private EditText emailEditText, teacherIdEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_registration); // Ensure the XML layout name is correct

        // Initialize Views
        emailEditText = findViewById(R.id.teacheremailEditText);
        teacherIdEditText = findViewById(R.id.teacherIdEditText);
        phoneEditText = findViewById(R.id.teacherPhoneEditText);
        departmentTextView = findViewById(R.id.teacherdepTextView);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConPassword);

        findViewById(R.id.Teacher_register_Button).setOnClickListener(v -> validateAndRegister());
    }
    private void scrollToView(View view) {
        view.requestFocus(); // Request focus for the view
        view.getParent().requestChildFocus(view, view); // Ensure the view gets focused in ScrollView
    }

    private void validateAndRegister() {
        // Get input values
        String email = emailEditText.getText().toString().trim();
        String teacherId = teacherIdEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String department = departmentTextView.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Check for empty fields and focus on the first invalid field
        if (TextUtils.isEmpty(email)) {
            showError(emailEditText, "Email is required");
            scrollToView(emailEditText);
            return;
        }
        if (TextUtils.isEmpty(teacherId)) {
            showError(teacherIdEditText, "Teacher ID is required");
            scrollToView(teacherIdEditText);
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            showError(phoneEditText, "Phone number is required");
            scrollToView(phoneEditText);
            return;
        }
        if (TextUtils.isEmpty(department) || department.equals("Select Department")) {
            showError(departmentTextView, "Department is required");
            scrollToView(departmentTextView);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError(passwordEditText, "Password is required");
            scrollToView(passwordEditText);
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            showError(confirmPasswordEditText, "Confirm Password is required");
            scrollToView(confirmPasswordEditText);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordEditText, "Passwords do not match");
            scrollToView(confirmPasswordEditText);
            return;
        }

        // Successful registration
        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

        // Navigate to TeacherHomepage activity
        Intent intent = new Intent(TeacherRegistrationActivity.this, TeacherHomepage.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }

    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage);
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.RED);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
