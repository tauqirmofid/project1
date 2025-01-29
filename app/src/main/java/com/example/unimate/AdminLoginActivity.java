package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Admin");

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.adminlgnButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get input values
                String inputEmail = emailEditText.getText().toString().trim();
                String inputPassword = passwordEditText.getText().toString().trim();

                // Validate inputs
                if (TextUtils.isEmpty(inputEmail)) {
                    emailEditText.setError("Email is required");
                    return;
                }

                if (TextUtils.isEmpty(inputPassword)) {
                    passwordEditText.setError("Password is required");
                    return;
                }




                // Call the login method
                login(inputEmail, inputPassword);
            }
        });
    }

    private void login(String inputEmail, String inputPassword) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {


                if (snapshot.exists()) {
                    String storedEmail = snapshot.child("email").getValue(String.class);
                    String storedPassword = snapshot.child("password").getValue(String.class);

                    if (inputEmail.equals(storedEmail) && inputPassword.equals(storedPassword)) {
                        // Login successful
                        Toast.makeText(AdminLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        // Navigate to Home Activity
                        Intent intent = new Intent(AdminLoginActivity.this, AdminHomePage.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Invalid credentials
                        Toast.makeText(AdminLoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Admin node not found
                    Toast.makeText(AdminLoginActivity.this, "Admin data not found in database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
