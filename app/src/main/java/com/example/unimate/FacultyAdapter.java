package com.example.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private List<DocumentSnapshot> facultyList;
    private OnTeacherClickListener listener;
    public interface OnTeacherClickListener {
        void onTeacherClick(DocumentSnapshot teacherDoc);
    }

    public FacultyAdapter(List<DocumentSnapshot> facultyList, OnTeacherClickListener listener) {
        this.facultyList = facultyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.teacher_card, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        DocumentSnapshot faculty = facultyList.get(position);

        // Populate data from Firestore
        holder.facultyName.setText(faculty.getString("full_name"));
        holder.facultyAcronym.setText(faculty.getId());
        holder.facultyEmail.setText(faculty.getString("email"));
        holder.facultyDesignation.setText(faculty.getString("designation"));
        holder.facultyCell.setText(faculty.getString("cell"));
        holder.facultyDept.setText(faculty.getString("department"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTeacherClick(faculty);
            }
        });
    }


    @Override
    public int getItemCount() {
        return facultyList == null ? 0 : facultyList.size(); // Return the correct size of the list
    }


    public static class FacultyViewHolder extends RecyclerView.ViewHolder {
        TextView facultyName, facultyAcronym, facultyEmail, facultyDesignation, facultyCell, facultyDept;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            facultyName = itemView.findViewById(R.id.teacher_nameText);
            facultyAcronym = itemView.findViewById(R.id.teacher_acronym);
            facultyEmail = itemView.findViewById(R.id.teacher_email);
            facultyDesignation = itemView.findViewById(R.id.Designation);
            facultyCell = itemView.findViewById(R.id.Cell);
            facultyDept = itemView.findViewById(R.id.dept);
        }
    }
}
