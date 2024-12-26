package com.example.unimate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TeacherLoginActivity extends AppCompatActivity {

    private Button teacher_reg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);

        // Floating back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        //register
        teacher_reg= findViewById(R.id.TeaRegButton);
        teacher_reg.setOnClickListener(v->{
            Intent intent = new Intent(TeacherLoginActivity.this, TeacherRegistrationActivity.class);
            startActivity(intent);

        });
    }
}