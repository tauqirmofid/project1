package com.example.unimate;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminHomePage extends AppCompatActivity {

    private RecyclerView requestsRecyclerView;
    private RequestAdapter requestAdapter;
    private List<RequestModel> requestList;
    private CardView roomCardView, routineCardView;

    // Drawer references
    private DrawerLayout drawerLayout;
    private ImageView leftNavBarImage;

    // Approval section toggle buttons
    private Button teacherToggleButton, crToggleButton;

    private String currentRole = "Teacher"; // Default role for filtering requests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home_page);

        roomCardView = findViewById(R.id.roomsCardView);
        roomCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Upload Activity
                Intent intent = new Intent(AdminHomePage.this, Upload.class);
                startActivity(intent);
            }
        });

        routineCardView = findViewById(R.id.routineCardView);
        routineCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Upload Activity
                Intent intent = new Intent(AdminHomePage.this, UploadCsvActivity.class);
                startActivity(intent);
            }
        });

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout);
        leftNavBarImage = findViewById(R.id.leftNavBarImage);

        if (leftNavBarImage != null) {
            leftNavBarImage.setOnClickListener(view -> {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }

        // Navigation buttons setup
        setUpNavigationButtons();

        // Approval section setup
        teacherToggleButton = findViewById(R.id.teacherToggleButton);
        crToggleButton = findViewById(R.id.crToggleButton);

        teacherToggleButton.setOnClickListener(view -> {
            currentRole = "Teacher";
            updateToggleButtons();
            fetchApprovalRequests();
        });

        crToggleButton.setOnClickListener(view -> {
            currentRole = "CR";
            updateToggleButtons();
            fetchApprovalRequests();
        });

        // Setup RecyclerView
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, requestList, this::showRequestDetailsDialog);
        requestsRecyclerView.setAdapter(requestAdapter);

        // Fetch initial data
        fetchApprovalRequests();
    }

    private void setUpNavigationButtons() {
        Button navHomeButton = findViewById(R.id.navHomeButton);
        Button navProfileButton = findViewById(R.id.navProfileButton);
        Button navLogoutButton = findViewById(R.id.navLogoutButton);

        if (navHomeButton != null) {
            navHomeButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
        if (navProfileButton != null) {
            navProfileButton.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(AdminHomePage.this, Upload.class);
                startActivity(intent);
            });
        }

        if (navLogoutButton != null) {
            navLogoutButton.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(AdminHomePage.this, AdminLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void fetchApprovalRequests() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(currentRole.equals("CR") ? "CR" : "Teachers");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                for (DataSnapshot batchSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot sectionSnapshot : batchSnapshot.getChildren()) {
                        for (DataSnapshot userSnapshot : sectionSnapshot.getChildren()) {
                            RequestModel request = userSnapshot.getValue(RequestModel.class);
                            if (request != null && !request.isVerified()) {
                                request.setId(userSnapshot.getKey()); // Set Firebase key as ID
                                requestList.add(request);
                            }
                        }
                    }
                }
                requestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminHomePage.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateToggleButtons() {
        if (currentRole.equals("Teacher")) {
            teacherToggleButton.setBackgroundTintList(getResources().getColorStateList(R.color.Theme_green));
            crToggleButton.setBackgroundTintList(getResources().getColorStateList(R.color.Theme_Lightgreen));
        } else {
            crToggleButton.setBackgroundTintList(getResources().getColorStateList(R.color.Theme_green));
            teacherToggleButton.setBackgroundTintList(getResources().getColorStateList(R.color.Theme_Lightgreen));
        }
    }

    public void showRequestDetailsDialog(RequestModel request) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_request_details);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView emailTextView = dialog.findViewById(R.id.emailTextView);
        TextView idTextView = dialog.findViewById(R.id.idTextView);
        TextView phoneTextView = dialog.findViewById(R.id.phoneTextView);
        TextView extraDetailsTextView = dialog.findViewById(R.id.extraDetailsTextView);
        Button acceptButton = dialog.findViewById(R.id.acceptButton);
        Button rejectButton = dialog.findViewById(R.id.rejectButton);

        emailTextView.setText(request.getEmail());
        idTextView.setText("ID: " + request.getId());
        phoneTextView.setText("Phone: " + request.getPhone());
        extraDetailsTextView.setText(currentRole.equals("CR")
                ? "Batch: " + request.getBatch() + "\nSection: " + request.getSection()
                : "Department: " + request.getDepartment() + "\nDesignation: " + request.getDesignation());

        acceptButton.setOnClickListener(v -> {
            moveRequestToAnotherDatabase(request, true);
            dialog.dismiss();
        });

        rejectButton.setOnClickListener(v -> {
            moveRequestToAnotherDatabase(request, false);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void moveRequestToAnotherDatabase(RequestModel request, boolean isAccepted) {
        DatabaseReference currentRef = FirebaseDatabase.getInstance()
                .getReference(currentRole.equals("CR") ? "CR" : "Teachers")
                .child(currentRole.equals("CR") ? request.getBatch() : request.getDepartment())
                .child(currentRole.equals("CR") ? request.getSection() : request.getDesignation())
                .child(request.getId());

        DatabaseReference targetRef = FirebaseDatabase.getInstance()
                .getReference(isAccepted ? "AcceptedRequests" : "RejectedRequests")
                .child(currentRole.equals("CR") ? "CR" : "Teachers")
                .child(request.getId());

        currentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                targetRef.setValue(snapshot.getValue()).addOnSuccessListener(aVoid -> {
                    currentRef.removeValue().addOnSuccessListener(aVoid1 -> {
                        Toast.makeText(AdminHomePage.this, isAccepted ? "Request approved and moved!" : "Request rejected and moved!", Toast.LENGTH_SHORT).show();
                        requestList.remove(request);
                        requestAdapter.notifyDataSetChanged();
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminHomePage.this, "Failed to move request", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
