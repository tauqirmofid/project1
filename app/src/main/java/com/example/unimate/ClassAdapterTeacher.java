package com.example.unimate;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassAdapterTeacher extends RecyclerView.Adapter<ClassAdapterTeacher.ViewHolder> {

    private final List<ClassWithTasks> classes;
    private final OnClassClickListener clickListener;
    private final Context context;
    private final String teacherAcronym;


    public interface OnClassClickListener {
        void onClassClick(ClassWithTasks classItem);
        void onDeleteClass(ClassWithTasks classItem);
    }

    public ClassAdapterTeacher(List<ClassWithTasks> classes, OnClassClickListener listener,
                        Context context, String teacherAcronym) {
        this.classes = classes != null ? classes : new ArrayList<>();
        this.clickListener = listener;
        this.context = context;
        this.teacherAcronym = teacherAcronym;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassWithTasks item = classes.get(position);
        holder.classTime.setText(item.getTimeSlot());
        holder.className.setText(item.getCourse());

        // Show/hide UI elements based on instructor match
        boolean isOwnClass = item.getInstructor().equals(teacherAcronym);
        holder.deleteIcon.setVisibility(isOwnClass ? View.VISIBLE : View.GONE);
       // holder.taskCountBadge.setVisibility(isOwnClass ? View.VISIBLE : View.GONE);

        // Update click listeners only for own classes
        // Show the task badge if tasks exist
        if (item.hasTasks()) {
            holder.taskCountBadge.setVisibility(View.VISIBLE);
            holder.taskCountBadge.setText(String.valueOf(item.getTasks().size()));
        } else {
            holder.taskCountBadge.setVisibility(View.GONE);
        }

        // Set click listeners only if it's the teacher’s class
        if (isOwnClass) {
            holder.itemView.setOnClickListener(v -> showOptionsDialog(item));
            holder.deleteIcon.setOnClickListener(v -> showDeleteConfirmation(item));
        } else {
            holder.itemView.setOnClickListener(null);
            holder.deleteIcon.setOnClickListener(null);
        }





//        holder.itemView.setOnClickListener(v -> showOptionsDialog(item));
//        holder.deleteIcon.setOnClickListener(v -> showDeleteConfirmation(item));
    }

    private void showOptionsDialog(ClassWithTasks classItem) {
        // Directly trigger the click event instead of showing the dialog
        clickListener.onClassClick(classItem);
    }


    private void showDeleteConfirmation(ClassWithTasks classItem) {
        // Add validation to delete method
        if (!classItem.getInstructor().equals(teacherAcronym)) {
            Toast.makeText(context, "You can only delete your own classes", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_delete_class, null);

        EditText confirmInput = dialogView.findViewById(R.id.confirmEditText);
        MaterialButton deleteBtn = dialogView.findViewById(R.id.deleteButton);
        MaterialButton cancelBtn = dialogView.findViewById(R.id.cancelButton);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        deleteBtn.setEnabled(false);
        confirmInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                deleteBtn.setEnabled(s.toString().equalsIgnoreCase("DELETE"));
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        deleteBtn.setOnClickListener(v -> {
            deleteClassFromFirestore(classItem);
            dialog.dismiss();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteClassFromFirestore(ClassWithTasks classItem) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date date = ((CalendarActivity) context).currentSelectedDate;
        String dayName = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date).toLowerCase();

        Map<String, Object> updates = new HashMap<>();
        String path = "batch_" + ((CalendarActivity) context).selectedBatch + "."
                + ((CalendarActivity) context).selectedSection + "."
                + classItem.getTimeSlot();

        updates.put(path, FieldValue.delete());

        db.collection("schedules").document(dayName)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Class deleted", Toast.LENGTH_SHORT).show();
                    ((CalendarActivity) context).loadAllDataForRange();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Add actionButton declaration
        MaterialButton actionButton;
        TextView classTime, className, taskCountBadge;
        ImageView deleteIcon;

        public ViewHolder(View itemView) {
            super(itemView);
//            // Add actionButton initialization
//            actionButton = itemView.findViewById(R.id.actionButton);
            classTime = itemView.findViewById(R.id.classTime);
            className = itemView.findViewById(R.id.className);
            taskCountBadge = itemView.findViewById(R.id.taskCountBadge);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }

}