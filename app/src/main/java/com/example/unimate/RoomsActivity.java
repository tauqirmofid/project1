package com.example.unimate;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class RoomsActivity extends AppCompatActivity {

    private static final String TAG = "RoomsActivity";
    private FirebaseFirestore db;

    // UI elements for RKB floors
    private ImageView rkbG, rkb1, rkb2, rkb3,rabG,rab1,rab2,rab3;

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
        // Fetch and display RKB floors
        fetchRabFloors();

        fetchRkbFloors();
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
        // Define the floors to be fetched
        String[] floors = {"G", "1st", "2nd", "3rd"};

        // Map floor names to corresponding ImageViews
        Map<String, ImageView> floorToImageView = new HashMap<>();
        floorToImageView.put("G", rkbG);
        floorToImageView.put("1st", rkb1);
        floorToImageView.put("2nd", rkb2);
        floorToImageView.put("3rd", rkb3);

        for (String floor : floors) {
            db.collection("rooms").document("RKB").collection(floor)
                    .document("Image")  // The document inside each floor containing "imageKey"
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
        // Replace with your Cloudinary URL or image loader
        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + imageKey;

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic) // Placeholder while loading
                .error(R.drawable.database_error) // Error image if load fails
                .into(imageView);
    }
}
