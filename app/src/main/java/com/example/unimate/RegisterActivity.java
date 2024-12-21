package com.example.unimate;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Window;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, studentIdEditText, passwordEditText, confirmPasswordEditText;
    private Spinner departmentSpinner, sectionSpinner; // We'll keep these as actual spinners
    private Button registerButton;

    // Pseudo-spinner for batch
    private TextView batchTextView;
    // The custom batch list items
    private List<String> batchList = Arrays.asList("53","54","55","56","58","59", "60", "61", "62", "63", "64", "65");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        studentIdEditText = findViewById(R.id.studentIdEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        // Department & Section are still spinners
        departmentSpinner = findViewById(R.id.departmentSpinner);
        sectionSpinner = findViewById(R.id.sectionSpinner);

        registerButton = findViewById(R.id.registerButton);

        // 1) Populate the department and section spinners
        populateSpinners();

        // 2) Setup pseudo-spinner TextView for batch
        batchTextView = findViewById(R.id.batchTextView);
        // Set initial text so user knows to click
        batchTextView.setText("Select Batch");
        // When tapped, show our custom dialog
        batchTextView.setOnClickListener(v -> showBatchDialog());

        // Register button
        registerButton.setOnClickListener(v -> validateInputs());
    }

    /**
     * Shows a custom dialog with a ListView of batch items.
     * The user can pick one, or tap the X to close.
     */
    private void showBatchDialog() {
        // Create a custom dialog without the full-screen black overlay
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_list);

        // Make the dialog background transparent so we can style it ourselves
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Make sure we DO NOT remove dim behind
            // dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); // Remove this line

            // Instead, ensure the window uses the dim background
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // 60% dim overlay
            dialog.getWindow().setDimAmount(0.6f);

            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setLayout(900, WindowManager.LayoutParams.WRAP_CONTENT);
        }


        // Find views in dialog
        ListView listView = dialog.findViewById(R.id.listViewItems);
        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);

        // Create and set adapter
        BatchListAdapter adapter = new BatchListAdapter(this, batchList);
        listView.setAdapter(adapter);

        // Remove all default dividers
        //listView.setDivider(new ColorDrawable(Color.WHITE));
        //listView.setDividerHeight(10);

        // On item click
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Mark item as selected in the adapter
            adapter.setSelectedPosition(position);

            // Close dialog
            dialog.dismiss();

            // Update the pseudo-spinner text
            String selectedBatch = batchList.get(position);
            batchTextView.setText(selectedBatch);
        });

        // Close (X) button
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    /**
     * Populate department & section spinners as normal.
     * We no longer use a batchSpinner.
     */
    private void populateSpinners() {
        // Department dropdown
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Department", "CSE", "EEE", "CEE"});
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departmentSpinner.setAdapter(departmentAdapter);

        // Section dropdown
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Section", "A", "B", "C", "D", "E"});
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(sectionAdapter);
    }

    private void validateInputs() {
        boolean isValid = true; // Flag to track if all inputs are valid

        String email = emailEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String selectedBatch = batchTextView.getText().toString();

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

        // Department spinner validation
        if (departmentSpinner.getSelectedItemPosition() == 0) {
            isValid = false;
            Toast.makeText(this, "Please select a department!", Toast.LENGTH_SHORT).show();
        }

        // Batch TextView validation (since we replaced the spinner)
        if ("Select Batch".equals(selectedBatch)) {
            isValid = false;
            Toast.makeText(this, "Please select a batch!", Toast.LENGTH_SHORT).show();
        }

        // Section spinner validation
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
