package com.example.unimate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomsActivity extends AppCompatActivity {

    private static final String TAG = "RoomsActivity";
    private FirebaseFirestore db;

    // UI elements for RKB floors
    private ImageView rkbG, rkb1, rkb2, rkb3, rabG, rab1, rab2, rab3;
    private EditText searchEditText;
    private RecyclerView roomRecyclerView;

    // Adapter and list for search results
    private RoomAdapter roomAdapter;
    private List<RoomModel> roomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        rkbG = findViewById(R.id.rkb_g);
        rkb1 = findViewById(R.id.rkb_1);
        rkb2 = findViewById(R.id.rkb_2);
        rkb3 = findViewById(R.id.rkb_3);
        rabG = findViewById(R.id.rab_g);
        rab1 = findViewById(R.id.rab_1);
        rab2 = findViewById(R.id.rab_2);
        rab3 = findViewById(R.id.rab_3);
        searchEditText = findViewById(R.id.searchEditText);
        roomRecyclerView = findViewById(R.id.recyclerViewSearchResults);

        // Set up RecyclerView for search results
        roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomAdapter = new RoomAdapter(this, roomList);
        roomRecyclerView.setAdapter(roomAdapter);

        // Fetch and display RKB and RAB floors
        fetchRabFloors();
        fetchRkbFloors();

        // Set up search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fetchRooms(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchRabFloors() {
        String[] floors = {"G", "1st", "2nd", "3rd"};

        Map<String, ImageView> floorToImageView = new HashMap<>();
        floorToImageView.put("G", rabG);
        floorToImageView.put("1st", rab1);
        floorToImageView.put("2nd", rab2);
        floorToImageView.put("3rd", rab3);

        for (String floor : floors) {
            Log.d(TAG, "Checking Firestore: RAB → " + floor + " → Image");

            db.collection("rooms").document("RAB").collection(floor)
                    .document("Image")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imageKey = documentSnapshot.getString("imageKey");

                            if (imageKey != null && !imageKey.isEmpty()) {
                                Log.d(TAG, "Image found for " + floor + ": " + imageKey);
                                loadImageIntoView(imageKey, floorToImageView.get(floor));
                            } else {
                                Log.w(TAG, "No imageKey found for floor: " + floor);
                            }
                        } else {
                            Log.w(TAG, "No Image document found for floor: " + floor);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching image for floor: " + floor, e);
                    });
        }
    }

    private void fetchRkbFloors() {
        String[] floors = {"G", "1st", "2nd", "3rd"};

        Map<String, ImageView> floorToImageView = new HashMap<>();
        floorToImageView.put("G", rkbG);
        floorToImageView.put("1st", rkb1);
        floorToImageView.put("2nd", rkb2);
        floorToImageView.put("3rd", rkb3);

        for (String floor : floors) {
            db.collection("rooms").document("RKB").collection(floor)
                    .document("Image")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imageKey = documentSnapshot.getString("imageKey");
                            if (imageKey != null && !imageKey.isEmpty()) {
                                loadImageIntoView(imageKey, floorToImageView.get(floor));
                            } else {
                                Log.w(TAG, "No imageKey found for floor: " + floor);
                            }
                        } else {
                            Log.w(TAG, "No Image document found for floor: " + floor);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching image for floor: " + floor, e);
                    });
        }
    }

    private void loadImageIntoView(String imageKey, ImageView imageView) {
        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + imageKey;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic) // Placeholder while loading
                .error(R.drawable.database_error) // Error image if load fails
                .into(imageView);
    }

    private void fetchRooms(String query) {
        roomList.clear();
        if (query.isEmpty()) {
            roomAdapter.notifyDataSetChanged();
            return;
        }

        db.collection("rooms").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot buildingDoc : task.getResult()) {
                    String building = buildingDoc.getId();
                    buildingDoc.getReference().get().addOnCompleteListener(floorTask -> {
                        if (floorTask.isSuccessful() && floorTask.getResult().exists()) {
                            for (String floor : floorTask.getResult().getData().keySet()) {
                                buildingDoc.getReference().collection(floor)
                                        .get()
                                        .addOnCompleteListener(roomTask -> {
                                            if (roomTask.isSuccessful()) {
                                                for (QueryDocumentSnapshot roomDoc : roomTask.getResult()) {
                                                    String roomNumber = roomDoc.getId();
                                                    if (roomNumber.contains(query)) {
                                                        String imageKey = roomDoc.getString("imageKey");
                                                        roomList.add(new RoomModel(building, floor, roomNumber, imageKey));
                                                    }
                                                }
                                                roomAdapter.notifyDataSetChanged();
                                            }
                                        });
                            }
                        }
                    });
                }
            } else {
                Toast.makeText(RoomsActivity.this, "Error fetching data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
