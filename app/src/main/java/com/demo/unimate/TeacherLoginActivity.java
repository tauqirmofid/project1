package com.demo.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TeacherLoginActivity extends AppCompatActivity {
    private Button teacher_reg, teacher_login;
    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);

        // Floating back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Registration button
        teacher_reg = findViewById(R.id.TeaRegButton);
        teacher_reg.setOnClickListener(v -> {

            Intent intent = new Intent(TeacherLoginActivity.this, TeacherRegistrationActivity.class);
            startActivity(intent);
        });

        // Initialize login fields
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        // Login button
        teacher_login = findViewById(R.id.TealgnButton);
        teacher_login.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Email is required");
                emailEditText.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                passwordEditText.requestFocus();
                return;
            }

            // Show the LoadingActivity
            startActivity(new Intent(TeacherLoginActivity.this, LoadingActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            validateLogin(email, hashPassword(password));
        });
    }

    private void validateLogin(String email, String hashedPassword) {
        DatabaseReference acceptedRequestsRef = FirebaseDatabase.getInstance()
                .getReference("AcceptedRequests").child("Teachers");
        DatabaseReference teachersRef = FirebaseDatabase.getInstance()
                .getReference("Teachers");

        acceptedRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sendCloseLoadingBroadcast();
                boolean isValidUser = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String storedEmail = userSnapshot.child("email").getValue(String.class);
                    String storedPassword = userSnapshot.child("password").getValue(String.class);

                    if (storedEmail != null && storedPassword != null &&
                            storedEmail.equals(email) && storedPassword.equals(hashedPassword)) {
                        isValidUser = true;
                        break;
                    }
                }

                if (isValidUser) {
                    sendCloseLoadingBroadcast();
                    Toast.makeText(TeacherLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isTeacherLoggedIn", true);
                    editor.putString("teacherEmail", email); // Store email for fetching details
                    editor.apply();

                    Intent intent = new Intent(TeacherLoginActivity.this, TeacherHomepage.class);
                    intent.putExtra("teacherEmail", email); // Pass email for fetching details
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();

                } else {
                    // Check if the user exists in the Teachers table
                    teachersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                            sendCloseLoadingBroadcast();
                            boolean existsInTeachers = false;

                            for (DataSnapshot teacherUser : teacherSnapshot.getChildren()) {
                                String teacherEmail = teacherUser.child("email").getValue(String.class);
                                if (teacherEmail != null && teacherEmail.equals(email)) {
                                    existsInTeachers = true;
                                    break;
                                }
                            }

                            if (existsInTeachers) {
                                sendCloseLoadingBroadcast();
                                Toast.makeText(TeacherLoginActivity.this, "Please wait for admin approval", Toast.LENGTH_SHORT).show();
                            } else {
                                sendCloseLoadingBroadcast();
                                Toast.makeText(TeacherLoginActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            sendCloseLoadingBroadcast();
                            Toast.makeText(TeacherLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendCloseLoadingBroadcast();
                Toast.makeText(TeacherLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hashedBytes) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendCloseLoadingBroadcast() {
        LocalBroadcastManager.getInstance(TeacherLoginActivity.this)
                .sendBroadcast(new Intent("CLOSE_LOADING"));
    }
}
