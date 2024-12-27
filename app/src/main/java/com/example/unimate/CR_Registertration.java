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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;
public class CR_Registertration extends AppCompatActivity {
    private EditText emailEditText, studentIdEditText, phoneEditText;
    private TextInputEditText passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView, batchTextView, sectionTextView;
    private ScrollView scrollView;

    private List<String> departmentList = Arrays.asList("CSE", "EEE", "BBA"); // Example departments
    private List<String> batchList = Arrays.asList("59th", "60th", "61st");  // Example batches
    private List<String> sectionList = Arrays.asList("A", "B", "C");         // Example sections

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cr_registertration);
        // Initialize views
        scrollView = findViewById(R.id.register_scroll);
        emailEditText = findViewById(R.id.CRemailEditText);
        studentIdEditText = findViewById(R.id.CRIdEditText);
        phoneEditText = findViewById(R.id.CrPhoneEditText);
        departmentTextView = findViewById(R.id.CrdepTextView);
        batchTextView = findViewById(R.id.CrbatchTextView);
        sectionTextView = findViewById(R.id.CrsectionTextView);
        passwordEditText = findViewById(R.id.CreditTextPassword);
        confirmPasswordEditText = findViewById(R.id.CreditTextConPassword);

        findViewById(R.id.CRregisterButton).setOnClickListener(v -> validateAndRegister());

        batchTextView.setOnClickListener(v -> showCustomDialog("Select Batch", batchList, batchTextView));
        departmentTextView.setOnClickListener(v -> showCustomDialog("Select Department", departmentList, departmentTextView));
        sectionTextView.setOnClickListener(v -> showCustomDialog("Select Section", sectionList, sectionTextView));
    }

    private void scrollToView(View view) {
        view.requestFocus(); // Request focus for the view
        view.getParent().requestChildFocus(view, view); // Ensure the view gets focused in ScrollView
    }


    private void validateAndRegister() {
        String email = emailEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String department = departmentTextView.getText().toString().trim();
        String batch = batchTextView.getText().toString().trim();
        String section = sectionTextView.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            showError(emailEditText, "Email is required");
            scrollToView(emailEditText);
            return;
        }
        if (TextUtils.isEmpty(studentId)) {
            showError(studentIdEditText, "Student ID is required");
            scrollToView(studentIdEditText);
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            showError(phoneEditText, "Phone number is required");
            scrollToView(phoneEditText);
            return;
        }
        if (TextUtils.isEmpty(department) || department.equals("Select Department")) {
            showError(departmentTextView, "Please select a department");
            scrollToView(departmentTextView);
            return;
        }
        if (TextUtils.isEmpty(batch) || batch.equals("Select Batch")) {
            showError(batchTextView, "Please select a batch");
            scrollToView(batchTextView);
            return;
        }
        if (TextUtils.isEmpty(section) || section.equals("Select Section")) {
            showError(sectionTextView, "Please select a section");
            scrollToView(sectionTextView);
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

        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
        //startActivity(new Intent(CR_Registertration.this, CRHomepage.class));
        finish();
    }

    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage);
        } else if (view instanceof TextView) {
            //((TextView) view).setTextColor(Color.RED);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
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