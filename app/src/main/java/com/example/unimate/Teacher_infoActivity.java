package com.example.unimate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Teacher_infoActivity extends AppCompatActivity {

    private RecyclerView teacherRecyclerView;
    private FacultyAdapter teacherAdapter;
    private EditText searchEditText;
    private List<DocumentSnapshot> teacherList = new ArrayList<>();
    private List<DocumentSnapshot> filteredList = new ArrayList<>();
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_info);

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        teacherRecyclerView = findViewById(R.id.teacherRecyclerView);

        // Setup RecyclerView
        teacherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        teacherAdapter = new FacultyAdapter(filteredList); // Pass the filtered list
        teacherRecyclerView.setAdapter(teacherAdapter);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load data from Firestore
        loadTeacherData();

        // Setup search functionality
        setupSearch();
    }

    private void loadTeacherData() {
        firestore.collection("teacher_info")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        teacherList.clear();
                        teacherList.addAll(task.getResult().getDocuments());
                        filteredList.clear();
                        filteredList.addAll(teacherList); // Initially show all teachers
                        teacherAdapter.notifyDataSetChanged(); // Refresh RecyclerView
                    } else {
                        Log.e("Firestore", "Error fetching data", task.getException());
                    }
                });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterResults(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });
    }

    private void filterResults(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            // Show all results when query is empty
            filteredList.addAll(teacherList);
        } else {
            // Filter based on full name or acronym
            for (DocumentSnapshot doc : teacherList) {
                String fullName = doc.getString("full_name");
                String acronym = doc.getId(); // Assuming acronym is the document ID

                if ((fullName != null && fullName.toLowerCase().contains(query.toLowerCase())) ||
                        (acronym != null && acronym.toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(doc);
                }
            }
        }

        // Notify adapter about data changes
        teacherAdapter.notifyDataSetChanged();
    }
}
