package com.example.unimate;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, studentIdEditText, passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView, sectionTextView, batchTextView;
    private Button registerButton;

    // Lists for pseudo-spinners
    private List<String> batchList = Arrays.asList("53", "54", "55", "56", "58", "59", "60", "61", "62", "63", "64", "65");
    private List<String> departmentList = Arrays.asList("CSE");
    private List<String> sectionList = Arrays.asList("A", "B", "C", "D", "E", "F", "I");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        studentIdEditText = findViewById(R.id.studentIdEditText);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConPassword);

        departmentTextView = findViewById(R.id.depTextView);
        sectionTextView = findViewById(R.id.sectionTextView);
        batchTextView = findViewById(R.id.batchTextView);
        registerButton = findViewById(R.id.registerButton);

        // Set initial text
        batchTextView.setText("Select Batch");
        departmentTextView.setText("Select Department");
        sectionTextView.setText("Select Section");

        // Set click listeners for dialogs
        batchTextView.setOnClickListener(v -> showCustomDialog("Select Batch", batchList, batchTextView));
        departmentTextView.setOnClickListener(v -> showCustomDialog("Select Department", departmentList, departmentTextView));
        sectionTextView.setOnClickListener(v -> showCustomDialog("Select Section", sectionList, sectionTextView));

        // Register button click
        registerButton.setOnClickListener(v -> validateInputs());
    }

    /**
     * Shows a custom dialog with a ListView for selection.
     *
     * @param title        Dialog title (e.g., Select Batch)
     * @param items        List of items to display
     * @param targetView   TextView to update with selected value
     */
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

    /**
     * Validates all input fields and displays appropriate error messages.
     */
    private void scrollToView(View view) {
        view.requestFocus(); // Request focus for the view
        view.getParent().requestChildFocus(view, view); // Ensure the view gets focused in ScrollView
    }
    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage); // Inline error for EditText
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(Color.RED); // Change TextView text color to red
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show(); // Show Toast message
        }
        scrollToView(view); // Scroll to the view
    }


    private void validateInputs() {
        String email = emailEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String selectedBatch = batchTextView.getText().toString();
        String selectedDepartment = departmentTextView.getText().toString();
        String selectedSection = sectionTextView.getText().toString();

        // Email validation
        if (TextUtils.isEmpty(email)) {
            showError(emailEditText, "Email is required!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(emailEditText, "Enter a valid email address!");
            return;
        }

        // Student ID validation
        if (TextUtils.isEmpty(studentId)) {
            showError(studentIdEditText, "Student ID is required!");
            return;
        }
        if (!studentId.matches("\\d+")) {
            showError(studentIdEditText, "Student ID must contain only numbers!");
            return;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            showError(passwordEditText, "Password is required!");
            return;
        }

        // Confirm Password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            showError(confirmPasswordEditText, "Confirm Password is required!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordEditText, "Passwords do not match!");
            return;
        }

        // Department validation
        if ("Select Department".equals(selectedDepartment)) {
            showError(departmentTextView, "Please select a department!");
            return;
        } else {
            departmentTextView.setTextColor(Color.BLACK); // Reset to default color
        }


        // Batch validation
        if ("Select Batch".equals(selectedBatch)) {
            showError(batchTextView, "Please select a batch!");
            return;
        } else {
            batchTextView.setTextColor(Color.BLACK); // Reset to default color
        }


        // Section validation
        if ("Select Section".equals(selectedSection)) {
            showError(sectionTextView, "Please select a section!");
            return;
        } else {
            sectionTextView.setTextColor(Color.BLACK); // Reset to default color
        }


        // If all validations pass
        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
        // Proceed with registration logic (e.g., saving data, navigating to another activity)
    }





}
