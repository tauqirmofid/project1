package com.example.unimate;

import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText,nameEditText, studentIdEditText, passwordEditText, confirmPasswordEditText;
    private TextView departmentTextView, sectionTextView, batchTextView;
    private Button registerButton;

    // Lists for pseudo-spinners
    private List<String> batchList = Arrays.asList("57","58","59", "60", "61","62","63","64","65");
    private List<String> sectionList = Arrays.asList("A", "B", "C","D","E","F","G","H","I","A+B","B+C");
    private List<String> departmentList = Arrays.asList("CSE");

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase references
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Students");

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText1);
        nameEditText = findViewById(R.id.std_nameText);
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


        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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

    private void scrollToView(View view) {
        view.requestFocus();
        view.getParent().requestChildFocus(view, view);
    }

    private void showError(View view, String errorMessage) {
        if (view instanceof EditText) {
            ((EditText) view).setError(errorMessage);
        } else if (view instanceof TextView) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
        scrollToView(view);
    }

    private void validateInputs() {
        String email = emailEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String studentId = studentIdEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String selectedBatch = batchTextView.getText().toString();
        String selectedDepartment = departmentTextView.getText().toString();
        String selectedSection = sectionTextView.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Valid email is required");
            emailEditText.requestFocus();
            return ;
        }
        if (TextUtils.isEmpty(name)) {
            showError(nameEditText, "Name is required!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(emailEditText, "Enter a valid email address!");
            return;
        }
        if (TextUtils.isEmpty(studentId)) {
            showError(studentIdEditText, "Student ID is required!");
            return;
        }
        if (!studentId.matches("\\d+")) {
            showError(studentIdEditText, "Student ID must contain only numbers!");
            return;
        }
        if ("Select Department".equals(selectedDepartment)) {
            showError(departmentTextView, "Please select a department!");
            return;
        }
        if ("Select Batch".equals(selectedBatch)) {
            showError(batchTextView, "Please select a batch!");
            return;
        }
        if ("Select Section".equals(selectedSection)) {
            showError(sectionTextView, "Please select a section!");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError(passwordEditText, "Password is required!");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            showError(confirmPasswordEditText, "Confirm Password is required!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordEditText, "Passwords do not match!");
            return;
        }

        String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                // Add user data to Realtime Database
                StudentModel newStudent = new StudentModel(email, name, studentId, selectedDepartment, selectedBatch, selectedSection, verificationCode, false);
                mDatabase.child(studentId).setValue(newStudent).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        // Send verification email
                        sendVerificationEmail(email, verificationCode);

                        // Navigate to VerificationActivity
                        Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("name", name);
                        intent.putExtra("studentId", studentId);
                        intent.putExtra("verificationCode", verificationCode);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save user data: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                    // User already exists
                    Toast.makeText(this, "User already exists. Please log in.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "" + authTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Sends the verification code via email using JavaMail.
     */
    private void sendVerificationEmail(String email, String verificationCode) {
        new Thread(() -> {
            try {
                String fromEmail = "info.teamunimate@gmail.com";
                String fromPassword = "zojc tfga rhrj cxvk";

                Properties properties = new Properties();
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.host", "smtp.gmail.com");
                properties.put("mail.smtp.port", "587");

                Session session = Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromEmail, fromPassword);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Verification Code");
                message.setText("Your verification code is: " + verificationCode);

                Transport.send(message);
                runOnUiThread(() -> Toast.makeText(this, "Verification code sent to email.", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}