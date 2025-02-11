package com.example.unimate;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Teacher_infoActivity extends AppCompatActivity implements FacultyAdapter.OnTeacherClickListener {

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


        ImageView backButton = findViewById(R.id.leftNavBarImage);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        teacherRecyclerView = findViewById(R.id.teacherRecyclerView);

        // Setup RecyclerView
        teacherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        teacherAdapter = new FacultyAdapter(filteredList, this); // Pass the filtered list
        teacherRecyclerView.setAdapter(teacherAdapter);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load data from Firestore
        loadTeacherData();

        // Setup search functionality
        setupSearch();
    }
    @Override
    public void onTeacherClick(DocumentSnapshot teacherDoc) {
        showTeacherDialog(teacherDoc);
    }

    private void showTeacherDialog(DocumentSnapshot teacherDoc) {
        // Manually extract fields to match Firestore document structure
        TeacherData teacher = new TeacherData(
                teacherDoc.getString("full_name"),  // Map Firestore's 'full_name' to TeacherData.name
                teacherDoc.getString("email"),
                teacherDoc.getId(),                  // Acronym is document ID
                teacherDoc.getString("teacherId"),
                teacherDoc.getString("cell"),       // Map Firestore's 'cell' to TeacherData.phone
                teacherDoc.getString("department"),
                teacherDoc.getString("designation"),
                "",                                  // Password not needed
                teacherDoc.getBoolean("isVerified") != null ? teacherDoc.getBoolean("isVerified") : false
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_details, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();


        TextView name = dialogView.findViewById(R.id.dialog_teacher_name);
        TextView acronym = dialogView.findViewById(R.id.dialog_teacher_acronym);
        TextView email = dialogView.findViewById(R.id.dialog_teacher_email);
        TextView phone = dialogView.findViewById(R.id.dialog_teacher_phone);
        TextView department = dialogView.findViewById(R.id.dialog_teacher_department);
        TextView designation = dialogView.findViewById(R.id.dialog_teacher_designation);

        // Set all fields
        name.setText(teacher.name);
        acronym.setText(teacher.acronym);
        email.setText(teacher.email);
        phone.setText(teacher.phone);
        department.setText(teacher.department);
        designation.setText(teacher.designation);


        // Make links clickable
        // email.setMovementMethod(LinkMovementMethod.getInstance());
        //   phone.setMovementMethod(LinkMovementMethod.getInstance());

        // Add click actions

        email.setOnClickListener(v -> {
            if(teacher.email != null && !teacher.email.isEmpty()) {
                showActionDialog("Email address:", teacher.email, "email");
            }
        });

        phone.setOnClickListener(v -> {
            if(teacher.phone != null && !teacher.phone.isEmpty()) {
                showActionDialog("Phone number:", teacher.phone, "phone");
            }
        });

        dialogView.findViewById(R.id.dialog_close_button).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showActionDialog(String title, String value, String actionType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_action_choice, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        ImageView actionIcon = dialogView.findViewById(R.id.action_execute);
        TextView actionLabel = dialogView.findViewById(R.id.action_label);

        // Set dynamic content based on action type
        if(actionType.equals("email")) {
            titleView.setText(getString(R.string.email_action_title, value));
            actionIcon.setImageResource(R.drawable.ic_email);
            actionLabel.setText(R.string.email_action);
        } else {
            titleView.setText(getString(R.string.phone_action_title, value));
            actionIcon.setImageResource(R.drawable.ic_call);
            actionLabel.setText(R.string.call_action);
        }

        dialogView.findViewById(R.id.action_copy).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("contact_info", value);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.action_execute).setOnClickListener(v -> {
            Intent intent = new Intent();
            if(actionType.equals("email")) {
                intent.setAction(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + value));
            } else {
                intent.setAction(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + value));
            }
            startActivity(intent);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.action_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
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
