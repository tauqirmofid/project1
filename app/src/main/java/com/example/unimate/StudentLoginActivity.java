package com.example.unimate;

import android.content.Context;
import android.content.Intent;
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
        lgnButton.setOnClickListener(v -> attemptLogin());

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
            emailEditText.setError("Email is required!");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
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
                        }
                    } else {
                        Toast.makeText(StudentLoginActivity.this,
                                "Login failed: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkVerificationInDatabase(FirebaseUser user) {
        String userId = user.getUid();
        mDatabase.orderByChild("email").equalTo(user.getEmail())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                Boolean isVerified = childSnapshot.child("isVerified").getValue(Boolean.class);
                                if (Boolean.TRUE.equals(isVerified)) {
                                    navigateToHomePage();
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
                        Toast.makeText(StudentLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHomePage() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(StudentLoginActivity.this, StudentHomePage.class);
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
}
