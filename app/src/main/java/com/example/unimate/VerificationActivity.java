package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class VerificationActivity extends AppCompatActivity {

    private EditText etDigit1, etDigit2, etDigit3, etDigit4, etDigit5, etDigit6;

    private String email;
    private String studentId;
    private String sentCode;
    private String password;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etDigit1 = findViewById(R.id.et_digit1);
        etDigit2 = findViewById(R.id.et_digit2);
        etDigit3 = findViewById(R.id.et_digit3);
        etDigit4 = findViewById(R.id.et_digit4);
        etDigit5 = findViewById(R.id.et_digit5);
        etDigit6 = findViewById(R.id.et_digit6);

        // Retrieve data from Intent
        email = getIntent().getStringExtra("email");
        studentId = getIntent().getStringExtra("studentId");
        sentCode = getIntent().getStringExtra("verificationCode");
        password = getIntent().getStringExtra("password");

        // Setup auto-focus movement for input fields
        setupCodeInput();
    }

    private void setupCodeInput() {
        EditText[] editTexts = {etDigit1, etDigit2, etDigit3, etDigit4, etDigit5, etDigit6};

        for (int i = 0; i < editTexts.length; i++) {
            final int index = i;

            editTexts[index].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < editTexts.length - 1) {
                        editTexts[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        editTexts[index - 1].requestFocus();
                    }

                    if (isCodeFullyEntered()) {
                        validateCode(getVerificationCode());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private String getVerificationCode() {
        return etDigit1.getText().toString().trim() +
                etDigit2.getText().toString().trim() +
                etDigit3.getText().toString().trim() +
                etDigit4.getText().toString().trim() +
                etDigit5.getText().toString().trim() +
                etDigit6.getText().toString().trim();
    }

    private boolean isCodeFullyEntered() {
        return !etDigit1.getText().toString().isEmpty()
                && !etDigit2.getText().toString().isEmpty()
                && !etDigit3.getText().toString().isEmpty()
                && !etDigit4.getText().toString().isEmpty()
                && !etDigit5.getText().toString().isEmpty()
                && !etDigit6.getText().toString().isEmpty();
    }

    private void validateCode(String enteredCode) {
        if (enteredCode.equals(sentCode)) {
            DatabaseReference studentRef = FirebaseDatabase.getInstance()
                    .getReference("Students")
                    .child(studentId);

            studentRef.child("isVerified").setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    authenticateUser();
                } else {
                    Toast.makeText(this, "Failed to update verification status.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Invalid verification code! Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void authenticateUser() {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    updateEmailVerifiedStatus(user);
                }
            } else {
                Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmailVerifiedStatus(FirebaseUser user) {
        user.reload().addOnCompleteListener(reloadTask -> {
            if (reloadTask.isSuccessful()) {
                user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                    if (verificationTask.isSuccessful()) {
                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Toast.makeText(this, "Error verifying email.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Failed to reload user.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(VerificationActivity.this, StudentHomePage.class);
        startActivity(intent);
        finish();
    }
}
