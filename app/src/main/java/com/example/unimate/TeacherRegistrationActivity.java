package com.example.unimate;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class TeacherRegistrationActivity extends AppCompatActivity {

    private EditText emailEditText, teacherIdEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView;
    private List<String> departmentList = Arrays.asList("CSE");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_registration);

        // Initialize Views
        emailEditText = findViewById(R.id.teacheremailEditText);
        teacherIdEditText = findViewById(R.id.teacherIdEditText);
        phoneEditText = findViewById(R.id.teacherPhoneEditText);
        departmentTextView = findViewById(R.id.teacherdepTextView);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConPassword);

        // Set initial text
        departmentTextView.setText("Select Department");

        // Set click listener for department TextView
        departmentTextView.setOnClickListener(v -> showCustomDialog("Select Department", departmentList, departmentTextView));

        findViewById(R.id.Teacher_register_Button).setOnClickListener(v -> validateAndRegister());
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
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
    private void scrollToView(View view) {
        view.requestFocus(); // Request focus for the view
        view.getParent().requestChildFocus(view, view); // Ensure the view gets focused in ScrollView
    }



    private void showCustomDialog(String title, List<String> items, TextView targetView) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_list);

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setLayout(900, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Set dialog title
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText(title);

        // Find views in dialog
        ListView listView = dialog.findViewById(R.id.listViewItems);
        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);

        // Create and set adapter
        BatchListAdapter adapter = new BatchListAdapter(this, items);
        listView.setAdapter(adapter);

        // On item click
        listView.setOnItemClickListener((parent, view, position, id) -> {
            adapter.setSelectedPosition(position);
            dialog.dismiss();
            String selectedItem = items.get(position);
            targetView.setText(selectedItem);
        });

        // Close button
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
