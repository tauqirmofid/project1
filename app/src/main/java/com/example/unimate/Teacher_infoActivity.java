package com.example.unimate;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Teacher_infoActivity extends AppCompatActivity {

    private RecyclerView teacherRecyclerView;
    private FacultyAdapter teacherAdapter;
    private List<DocumentSnapshot> teacherList = new ArrayList<>();
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_info);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        teacherRecyclerView = findViewById(R.id.teacherRecyclerView);
        teacherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        teacherAdapter = new FacultyAdapter(teacherList);
        teacherRecyclerView.setAdapter(teacherAdapter);

        // Load data from Firestore
        loadTeacherData();
    }

    private void loadTeacherData() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        RecyclerView recyclerView = findViewById(R.id.teacherRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Ensure LayoutManager is set

        firestore.collection("teacher_info")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> facultyList = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            // Filter invalid or empty documents
                            if (doc.getString("full_name") != null && !doc.getString("full_name").isEmpty()) {
                                facultyList.add(doc);
                            }
                        }
                        // Log list size for debugging
                        Log.d("FacultyListSize", "Number of Teachers: " + facultyList.size());

                        FacultyAdapter facultyAdapter = new FacultyAdapter(facultyList);
                        recyclerView.setAdapter(facultyAdapter);
                    } else {
                        Log.e("Firestore", "Error fetching data", task.getException());
                    }
                });

    }
}
