package com.demo.unimate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomsActivity extends AppCompatActivity {

    private static final String TAG = "RoomsActivity";
    private FirebaseFirestore db;
    private NestedScrollView scrollView;

    // UI elements for RKB floors
    private ImageView rkbG, rkb1, rkb2, rkb3, rabG, rab1, rab2, rab3;
    private EditText searchEditText;
    private RecyclerView roomRecyclerView;
    private LinearLayout rkbs;

    // Adapter and list for search results
    private RoomAdapter roomAdapter;
    private Button gotoFloor,gotoSearch;
    private List<RoomModel> roomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        ImageView backButton = findViewById(R.id.leftNavBarImage);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

//        // Bind UI elements
        rkbG = findViewById(R.id.rkb_g);
        rkb1 = findViewById(R.id.rkb_1);
        rkb2 = findViewById(R.id.rkb_2);
        rkb3 = findViewById(R.id.rkb_3);
        rabG = findViewById(R.id.rab_g);
        rab1 = findViewById(R.id.rab_1);
        rab2 = findViewById(R.id.rab_2);
        rab3 = findViewById(R.id.rab_3);

        scrollView=findViewById(R.id.scrollView);
        rkbs=findViewById(R.id.rkbs);

        gotoSearch=findViewById(R.id.gototop);
        gotoSearch.setOnClickListener(v->{
            if (scrollView != null && searchEditText != null) {
                int targetScrollY = searchEditText.getTop();

                // Scroll smoothly to the calculated Y position
                scrollView.post(() -> scrollView.smoothScrollTo(0, targetScrollY));
            }

        });


        gotoFloor=findViewById(R.id.gotoFloor);
        gotoFloor.setOnClickListener(v->{

            if (scrollView != null && rkbs != null) {
                // Calculate the Y position of carouselRecyclerView relative to the parent NestedScrollView
                int targetScrollY = rkbs.getTop();

                // Scroll smoothly to the calculated Y position
                scrollView.post(() -> scrollView.smoothScrollTo(0, targetScrollY));
            }

        });

        // Bind UI elements
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRooms(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        roomRecyclerView = findViewById(R.id.RecyclerView);

        // Set up RecyclerView for search results
        roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomAdapter = new RoomAdapter(this, roomList);
        roomRecyclerView.setAdapter(roomAdapter);



        // Fetch and display RKB and RAB floors
        fetchRabFloors();
        fetchRkbFloors();


        // Fetch rooms for both buildings
        fetchAllRooms();

    }
    private void filterRooms(String query) {
        List<RoomModel> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(); // Convert the query to lowercase

        for (RoomModel room : roomList) {
            if (room.getRoomNumber().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(room);
            }
        }
        roomAdapter.updateList(filteredList);
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
    private void fetchAllRooms() {
        // Define the buildings and floors
        String[] buildings = {"RAB", "RKB"};
        String[] floors = {"G", "1st", "2nd", "3rd"};

        // Loop through each building and floor
        for (String building : buildings) {
            for (String floor : floors) {
                fetchRoomsForBuildingAndFloor(building, floor);
            }
        }
    }

    private void fetchRoomsForBuildingAndFloor(String building, String floor) {
        db.collection("rooms").document(building).collection(floor)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot roomDoc : queryDocumentSnapshots) {
                        String roomNumber = roomDoc.getId();
                        String imageKey = roomDoc.getString("description");

                        if (imageKey != null && !imageKey.isEmpty()) {
                            RoomModel room = new RoomModel(building, floor, roomNumber, imageKey);
                            roomList.add(room);
                            Log.d(TAG, "Room added: " + roomNumber + " in " + building + " floor " + floor);
                        } else {
                            Log.w(TAG, "Room " + roomNumber + " in " + building + " floor " + floor + " has no image key.");
                        }
                    }
                    // Update the RecyclerView after fetching all rooms for the current floor
                    roomAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching rooms for building: " + building + ", floor: " + floor, e);
                });
    }

    private void loadImageIntoView(String imageKey, ImageView imageView) {
        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + imageKey;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic) // Placeholder while loading
                .error(R.drawable.database_error) // Error image if load fails
                .into(imageView);
    }


}
