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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class TeacherRegistrationActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText,acronymEditText, teacherIdEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView, designationTextView;

    private List<String> departmentList = Arrays.asList("CSE");
    private List<String> designationList = Arrays.asList("Adjunct Lecturer", "Lecturer", "Assistant Professor", "Associate Professor");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_registration);

        // Initialize Views
        nameEditText = findViewById(R.id.teachernameEditText);
        emailEditText = findViewById(R.id.teacheremailEditText);
        teacherIdEditText = findViewById(R.id.teacherIdEditText);
        phoneEditText = findViewById(R.id.teacherPhoneEditText);
        departmentTextView = findViewById(R.id.teacherdepTextView);
        designationTextView = findViewById(R.id.teacherdesignation);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConPassword);
        acronymEditText = findViewById(R.id.Acronymedittext);

        // Set initial text
        departmentTextView.setText("Select Department");
        designationTextView.setText("Select Designation");

        // Set click listeners for department and designation TextViews
        departmentTextView.setOnClickListener(v -> showCustomDialog("Select Department", departmentList, departmentTextView));
        designationTextView.setOnClickListener(v -> showCustomDialog("Select Designation", designationList, designationTextView));

        findViewById(R.id.Teacher_register_Button).setOnClickListener(v -> validateAndRegister());

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void validateAndRegister() {
        String name = nameEditText.getText().toString().trim();
        String acronym = acronymEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String teacherId = teacherIdEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String department = departmentTextView.getText().toString().trim();
        String designation = designationTextView.getText().toString().trim();
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
        if (TextUtils.isEmpty(acronym)) {
            showError(acronymEditText, "Acronym is required");
            scrollToView(acronymEditText);
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
        if (TextUtils.isEmpty(designation) || designation.equals("Select Designation")) {
            showError(designationTextView, "Designation is required");
            scrollToView(designationTextView);
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

        // Hash the password
        String hashedPassword = hashPassword(password);

        // Check for duplicate email or teacher ID
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isDuplicate = false;

                // Check in Teachers
                for (DataSnapshot deptSnapshot : snapshot.child("Teachers").getChildren()) {
                    for (DataSnapshot desigSnapshot : deptSnapshot.getChildren()) {
                        for (DataSnapshot teacherSnapshot : desigSnapshot.getChildren()) {
                            TeacherData existingTeacher = teacherSnapshot.getValue(TeacherData.class);
                            if (existingTeacher != null &&
                                    (existingTeacher.email.equalsIgnoreCase(email) || existingTeacher.teacherId.equalsIgnoreCase(teacherId))) {
                                isDuplicate = true;
                                break;
                            }
                        }
                    }
                    if (isDuplicate) break;
                }

                if (isDuplicate) {
                    Toast.makeText(TeacherRegistrationActivity.this, "Duplicate registration detected: Email or Teacher ID already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // Save data to Firebase
                    DatabaseReference teacherRef = rootRef.child("Teachers").child(teacherId);
                    TeacherData teacherData = new TeacherData(name, email,acronym, teacherId, phone, department, designation, hashedPassword, false);

                    teacherRef.setValue(teacherData).addOnSuccessListener(aVoid -> {
                        Toast.makeText(TeacherRegistrationActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TeacherRegistrationActivity.this, TeacherLoginActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(TeacherRegistrationActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherRegistrationActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage);
        } else if (view instanceof TextView) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToView(View view) {
        view.requestFocus();
        view.getParent().requestChildFocus(view, view);
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
        BatchListAdapter adapter = new BatchListAdapter(this, items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            adapter.setSelectedPosition(position);
            dialog.dismiss();
            String selectedItem = items.get(position);
            targetView.setText(selectedItem);
        });

        ImageView closeDialog = dialog.findViewById(R.id.closeDialog);
        closeDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
