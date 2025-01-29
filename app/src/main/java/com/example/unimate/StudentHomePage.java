package com.example.unimate;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentHomePage extends AppCompatActivity {

    private CardView rooms;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);
//        rooms = findViewById(R.id.roomsCardView);
//        rooms.setOnClickListener(v->{
//            Intent intent = new Intent(StudentHomePage.this, RoomsActivity.class);
//            startActivity(intent);
//        });
    }
}