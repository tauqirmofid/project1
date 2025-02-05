package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CrLoginActivity extends AppCompatActivity {
    private Button CR_reg, CR_login;
    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cr_login);

        // Floating back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Registration button
        CR_reg = findViewById(R.id.CRregButton);
        CR_reg.setOnClickListener(v -> {
            Intent intent = new Intent(CrLoginActivity.this, CR_Registertration.class);
            startActivity(intent);
        });

        // Initialize login fields
        emailEditText = findViewById(R.id.CRemailEditText);
        passwordEditText = findViewById(R.id.CRpasswordEditText);

        // Login button
        CR_login = findViewById(R.id.CRlgnButton);
        CR_login.setOnClickListener(v -> {
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

            validateLogin(email, hashPassword(password));
        });
    }

    private void validateLogin(String email, String hashedPassword) {
        DatabaseReference acceptedRequestsRef = FirebaseDatabase.getInstance()
                .getReference("AcceptedRequests")
                .child("CR");

        acceptedRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isValidUser = false;
                String crName = "Unknown CR";  // Default value to prevent null
                String crBatch = "N/A";
                String crSection = "N/A";

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String storedEmail = userSnapshot.child("email").getValue(String.class);
                    String storedPassword = userSnapshot.child("password").getValue(String.class);

                    if (storedEmail != null && storedPassword != null &&
                            storedEmail.equals(email) && storedPassword.equals(hashedPassword)) {
                        isValidUser = true;
                        crName = userSnapshot.child("name").getValue(String.class);
                        crBatch = userSnapshot.child("batch").getValue(String.class);
                        crSection = userSnapshot.child("section").getValue(String.class);

                        // Ensure no null values
                        crName = (crName != null) ? crName : "Unknown CR";
                        crBatch = (crBatch != null) ? crBatch : "N/A";
                        crSection = (crSection != null) ? crSection : "N/A";
                        break;
                    }
                }

                if (isValidUser) {
                    SharedPreferences sharedPreferences = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isCRLoggedIn", true);
                    editor.putString("crEmail", email);
                    editor.putString("crName", crName);  // Store CR name
                    editor.putString("crBatch", crBatch); // Store CR batch
                    editor.putString("crSection", crSection); // Store CR section
                    editor.apply();

                    Toast.makeText(CrLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CrLoginActivity.this, CR_HomePage.class);
                    intent.putExtra("CR_NAME", crName);
                    intent.putExtra("CR_BATCH", crBatch);
                    intent.putExtra("CR_SECTION", crSection);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CrLoginActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CrLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
}
