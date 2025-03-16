package com.demo.unimate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewRetakesActivity extends AppCompatActivity {

    private static final String TAG = "ViewRetakesActivity";

    private RecyclerView recyclerView;
    private PersonalizedCourseAdapter adapter;
    private List<Course> courseList = new ArrayList<>();

    private FirebaseFirestore db;
    private String userId;
    private String currentBatch;   // e.g. "batch_59"
    private String currentSection; // e.g. "A"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_retakes);

        recyclerView = findViewById(R.id.recyclerViewPersonalized);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PersonalizedCourseAdapter(courseList, this::removeCourseFromPersonalized);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Pull user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UnimatePrefs", MODE_PRIVATE);
        userId = prefs.getString("studentEmail", "");
        currentBatch = "batch_" + prefs.getString("studentBatch", "");
        currentSection = prefs.getString("studentSection", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "No user found. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadAllPersonalizedCourses();
    }

    /**
     * Fetch all personalized schedule docs for this user, across all days.
     * For each dayDoc, read the sub-collection "batch_<currentBatch> -> <currentSection>"
     * Then gather each timeslot as a Course object.
     */
    private void loadAllPersonalizedCourses() {
        // Clear any old data
        courseList.clear();

        db.collection("personalized_schedules")
                .document(userId)
                .collection("schedules")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No personalized schedules found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // We'll do nested calls for each dayDoc
                    for (DocumentSnapshot dayDoc : querySnapshot) {
                        String dayId = dayDoc.getId(); // e.g. "Sunday" or "monday"

                        dayDoc.getReference()
                                .collection(currentBatch)
                                .document(currentSection)
                                .get()
                                .addOnSuccessListener(sectionDoc -> {
                                    if (sectionDoc.exists()) {
                                        Map<String, Object> sectionData = sectionDoc.getData();
                                        if (sectionData != null) {
                                            for (String timeSlot : sectionData.keySet()) {
                                                Map<String, Object> courseData =
                                                        (Map<String, Object>) sectionData.get(timeSlot);

                                                if (courseData != null) {
                                                    // Build a Course object
                                                    String cName = safeGet(courseData, "course");
                                                    String cInstructor = safeGet(courseData, "instructor");
                                                    String cRoom = safeGet(courseData, "room");

                                                    Course course = new Course(
                                                            dayId,         // doc ID (e.g. "Sunday")
                                                            timeSlot,      // timeslot key
                                                            cName,
                                                            cInstructor,
                                                            cRoom
                                                    );
                                                    courseList.add(course);
                                                }
                                            }
                                            // After finishing this day, update the adapter
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to load sub-collection", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch personalized schedules", e);
                    Toast.makeText(this, "Error loading personalized schedules", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Called when the user taps "Remove" on a Course.
     * This will remove the timeslot from the relevant doc in Firestore,
     * then remove it from our local list.
     */
    private void removeCourseFromPersonalized(@NonNull Course course) {
        // Path: personalized_schedules -> userId -> schedules -> <dayId> -> batch_<currentBatch> -> <currentSection>
        // We want to remove the field with key = course.getTimeSlot()
        db.collection("personalized_schedules")
                .document(userId)
                .collection("schedules")
                .document(course.getDay()) // Must match exactly
                .collection(currentBatch)
                .document(currentSection)
                .update(course.getTimeSlot(), FieldValue.delete())  // <-- Use FieldValue.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Removed course successfully.", Toast.LENGTH_SHORT).show();
                    // Remove from local list
                    courseList.remove(course);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private String safeGet(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key);
        return val == null ? "" : val.toString();
    }
}
