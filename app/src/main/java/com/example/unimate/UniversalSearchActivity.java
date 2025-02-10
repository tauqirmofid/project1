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
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UniversalSearchActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private UniversalSearchAdapter adapter;
    private List<SearchItem> searchItems = new ArrayList<>();
    private List<DocumentSnapshot> teacherList = new ArrayList<>();
    private List<RoomModel> roomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_search);

        db = FirebaseFirestore.getInstance();
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.recyclerView);
        ImageButton closeButton = findViewById(R.id.closeButton);

        // Setup RecyclerView
        adapter = new UniversalSearchAdapter(searchItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Close button
        closeButton.setOnClickListener(v -> finish());

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterResults(s.toString().trim());
            }
        });

        // Load data
        fetchTeachers();
        fetchRooms();
    }

    private void fetchTeachers() {
        db.collection("teacher_info").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                teacherList = task.getResult().getDocuments();
                checkAndMergeData();
            }
        });
    }

    private void fetchRooms() {
        String[] buildings = {"RAB", "RKB"};
        String[] floors = {"G", "1st", "2nd", "3rd"};
        AtomicInteger pendingFetches = new AtomicInteger(buildings.length * floors.length);

        for (String building : buildings) {
            for (String floor : floors) {
                db.collection("rooms").document(building).collection(floor)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot doc : task.getResult()) {
                                    String roomNumber = doc.getId();
                                    String imageKey = doc.getString("description"); // Match Firestore field
                                    if (imageKey != null) {
                                        // Use buildingName as first parameter
                                        roomList.add(new RoomModel(
                                                building,
                                                floor,
                                                roomNumber,
                                                imageKey
                                        ));
                                    }
                                }
                                if (pendingFetches.decrementAndGet() == 0) {
                                    checkAndMergeData();
                                }
                            }
                        });
            }
        }
    }

    private void checkAndMergeData() {
        if (!teacherList.isEmpty() || !roomList.isEmpty()) {
            filterResults(searchEditText.getText().toString().trim());
        }
    }

    private void filterResults(String query) {
        List<SearchItem> filtered = new ArrayList<>();

        // Filter teachers
        List<DocumentSnapshot> filteredTeachers = new ArrayList<>();
        for (DocumentSnapshot teacher : teacherList) {
            String fullName = teacher.getString("full_name");
            String acronym = teacher.getId();
            if (fullName.toLowerCase().contains(query.toLowerCase()) ||
                    acronym.toLowerCase().contains(query.toLowerCase())) {
                filteredTeachers.add(teacher);
            }
        }

        // Filter rooms
        List<RoomModel> filteredRooms = new ArrayList<>();
        for (RoomModel room : roomList) {
            if (room.getRoomNumber().toLowerCase().contains(query.toLowerCase())) {
                filteredRooms.add(room);
            }
        }

        // Build search items
        searchItems.clear();
        if (!filteredTeachers.isEmpty()) {
            searchItems.add(new SearchItem(SearchItem.TYPE_HEADER, "Teachers"));
            for (DocumentSnapshot teacher : filteredTeachers) {
                searchItems.add(new SearchItem(SearchItem.TYPE_TEACHER, teacher));
            }
        }
        if (!filteredRooms.isEmpty()) {
            searchItems.add(new SearchItem(SearchItem.TYPE_HEADER, "Rooms"));
            for (RoomModel room : filteredRooms) {
                searchItems.add(new SearchItem(SearchItem.TYPE_ROOM, room));
            }
        }

        adapter.notifyDataSetChanged();
    }

    // SearchItem class
    public static class SearchItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_TEACHER = 1;
        public static final int TYPE_ROOM = 2;

        public int type;
        public String header;
        public DocumentSnapshot teacher;
        public RoomModel room;

        public SearchItem(int type, String header) {
            this.type = type;
            this.header = header;
        }

        public SearchItem(int type, DocumentSnapshot teacher) {
            this.type = type;
            this.teacher = teacher;
        }

        public SearchItem(int type, RoomModel room) {
            this.type = type;
            this.room = room;
        }
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

    private void showRoomImage(RoomModel room) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_image, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        ImageView fullscreenImage = dialogView.findViewById(R.id.fullscreenImage);
        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + room.getImageKey();

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic)
                .error(R.drawable.database_error)
                .into(fullscreenImage);

        dialogView.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }






    // Adapter class
    private class UniversalSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<SearchItem> items;

        public UniversalSearchAdapter(List<SearchItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == SearchItem.TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.item_search_header, parent, false));
            } else if (viewType == SearchItem.TYPE_TEACHER) {
                return new TeacherViewHolder(inflater.inflate(R.layout.item_teacher, parent, false));
            } else {
                return new RoomViewHolder(inflater.inflate(R.layout.item_room, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SearchItem item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).headerText.setText(item.header);
            } else if (holder instanceof TeacherViewHolder) {
                DocumentSnapshot teacher = item.teacher;
                ((TeacherViewHolder) holder).nameText.setText(teacher.getString("full_name"));
                ((TeacherViewHolder) holder).acronymText.setText(teacher.getId());
                holder.itemView.setOnClickListener(v -> showTeacherDialog(teacher));
            } else if (holder instanceof RoomViewHolder) {
                RoomModel room = item.room;
                ((RoomViewHolder) holder).roomNumberText.setText(room.getRoomNumber());
                ((RoomViewHolder) holder).buildingText.setText(room.getBuilding());
                holder.itemView.setOnClickListener(v -> showRoomImage(room));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type;
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView headerText;
            HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = itemView.findViewById(R.id.headerText);
            }
        }

        class TeacherViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, acronymText;
            TeacherViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.nameText);
                acronymText = itemView.findViewById(R.id.acronymText);
            }
        }

        class RoomViewHolder extends RecyclerView.ViewHolder {
            TextView roomNumberText, buildingText;
            RoomViewHolder(View itemView) {
                super(itemView);
                roomNumberText = itemView.findViewById(R.id.roomNumberText);
                buildingText = itemView.findViewById(R.id.buildingText);
            }
        }
    }
}