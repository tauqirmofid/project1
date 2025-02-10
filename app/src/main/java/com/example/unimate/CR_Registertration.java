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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class CR_Registertration extends AppCompatActivity {
    private EditText nameEditText, emailEditText, studentIdEditText, phoneEditText;
    private TextInputEditText passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView, batchTextView, sectionTextView;
    private ScrollView scrollView;

    private List<String> departmentList = Arrays.asList("CSE", "EEE", "BBA");
    private List<String> batchList = Arrays.asList("57","58","59", "60", "61","62","63","64","65");
    private List<String> sectionList = Arrays.asList("A", "B", "C","D","E","F","G","H","I","A+B","B+C");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cr_registertration);
        scrollView = findViewById(R.id.register_scroll);
        nameEditText = findViewById(R.id.CRNameEditText);
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
        scrollView.post(() -> {
            view.requestFocus(); // Request focus for the view
            scrollView.smoothScrollTo(0, view.getTop()); // Smoothly scroll to the view's position
        });
    }

    private void validateAndRegister() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String department = departmentTextView.getText().toString().trim();
        String batch = batchTextView.getText().toString().trim();
        String section = sectionTextView.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(name)) {
            showError(nameEditText, "Name is required");
            scrollToView(nameEditText);
            return;
        }
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

        // Check for duplicate email or student ID
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isDuplicate = false;

                // Check in CR
                for (DataSnapshot batchSnapshot : snapshot.child("CR").getChildren()) {
                    for (DataSnapshot sectionSnapshot : batchSnapshot.getChildren()) {
                        for (DataSnapshot crSnapshot : sectionSnapshot.getChildren()) {
                            CRData existingCR = crSnapshot.getValue(CRData.class);
                            if (existingCR != null &&
                                    (existingCR.email.equalsIgnoreCase(email) || existingCR.studentId.equalsIgnoreCase(studentId))) {
                                isDuplicate = true;
                                break;
                            }
                        }
                    }
                    if (isDuplicate) break;
                }

                if (isDuplicate) {
                    Toast.makeText(CR_Registertration.this, "Duplicate registration detected: Email or Student ID already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // Save data to Firebase
                    DatabaseReference crRef = rootRef.child("CR").child(batch).child(section);

                    // Save hashed password for security
                    String hashedPassword = hashPassword(password);
                    CRData crData = new CRData(name, email, studentId, phone, department, batch, section, hashedPassword, false); // isVerified = false

                    crRef.child(studentId).setValue(crData).addOnSuccessListener(aVoid -> {
                        Toast.makeText(CR_Registertration.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(CR_Registertration.this, CrLoginActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(CR_Registertration.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CR_Registertration.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage);
        } else if (view instanceof TextView) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomDialog(String title, List<String> items, TextView targetView) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_list);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setLayout(900, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText(title);

        ListView listView = dialog.findViewById(R.id.listViewItems);
        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);

        BatchListAdapter adapter = new BatchListAdapter(this, items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            adapter.setSelectedPosition(position);
            dialog.dismiss();
            String selectedItem = items.get(position);
            targetView.setText(selectedItem);
        });

        closeDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
