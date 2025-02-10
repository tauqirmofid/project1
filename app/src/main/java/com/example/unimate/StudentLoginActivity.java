package com.example.unimate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentLoginActivity extends AppCompatActivity {
    private Button std_reg;
    private Button lgnButton;

    // Login fields
    private EditText emailEditText;
    private EditText passwordEditText;

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Students");

        // Floating back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Initialize the UI fields
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        // Login button
        lgnButton = findViewById(R.id.lgnButton);
        lgnButton.setOnClickListener(v -> {
            // Show the LoadingActivity
            startActivity(new Intent(StudentLoginActivity.this, LoadingActivity.class));

            // Immediately override the transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            // Attempt login
            attemptLogin();
        });


        // Register button
        std_reg = findViewById(R.id.RegButton);
        std_reg.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            sendCloseLoadingBroadcast();

            emailEditText.setError("Email is required!");
            emailEditText.requestFocus();

            return;
        }
        if (password.isEmpty()) {
            sendCloseLoadingBroadcast();

            passwordEditText.setError("Password is required!");
            passwordEditText.requestFocus();

            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                             checkVerificationInDatabase(user);
                            sendCloseLoadingBroadcast();
                        }
                    } else {
                        // Close loading screen before showing the toast
                        sendCloseLoadingBroadcast();

                        // Show login failed message
                        Toast.makeText(StudentLoginActivity.this,
                                "Login failed: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void checkVerificationInDatabase(FirebaseUser user) {
        String email = user.getEmail();
        mDatabase.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        sendCloseLoadingBroadcast();

                        if (snapshot.exists()) {
                            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                Boolean isVerified = childSnapshot.child("isVerified").getValue(Boolean.class);
                                String stdName = childSnapshot.child("name").getValue(String.class);
                                String stdBatch = childSnapshot.child("batch").getValue(String.class);
                                String stdSection = childSnapshot.child("section").getValue(String.class);

                                if (Boolean.TRUE.equals(isVerified)) {
                                    navigateToHomePage(
                                            stdName != null ? stdName : "Student",
                                            stdBatch != null ? stdBatch : "N/A",
                                            stdSection != null ? stdSection : "N/A"
                                    );
                                } else {
                                    Toast.makeText(StudentLoginActivity.this, "Please verify your account first.", Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Toast.makeText(StudentLoginActivity.this, "User data not found in the database.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        sendCloseLoadingBroadcast();
                        Toast.makeText(StudentLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHomePage(String stdName, String stdBatch, String stdSection) {
        SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("isStudentLoggedIn", true);
        editor.putString("studentEmail", mAuth.getCurrentUser().getEmail());
        editor.putString("studentName", stdName);
        editor.putString("studentBatch", stdBatch);
        editor.putString("studentSection", stdSection);
        editor.apply();

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(StudentLoginActivity.this, StudentHomePage.class);
        intent.putExtra("STUDENT_NAME", stdName);
        intent.putExtra("STUDENT_BATCH", stdBatch);
        intent.putExtra("STUDENT_SECTION", stdSection);
        intent.putExtra("STUDENT_EMAIL", mAuth.getCurrentUser().getEmail());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    // Collapse keyboard when tapped elsewhere
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v != null && v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);

                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void sendCloseLoadingBroadcast() {
        LocalBroadcastManager.getInstance(StudentLoginActivity.this)
                .sendBroadcast(new Intent("CLOSE_LOADING"));
    }
}
