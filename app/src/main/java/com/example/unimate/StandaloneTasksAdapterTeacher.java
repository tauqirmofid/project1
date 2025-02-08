package com.example.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StandaloneTasksAdapterTeacher extends RecyclerView.Adapter<StandaloneTasksAdapterTeacher.ViewHolder> {
    private static List<UniTask> tasks;
    private static OnTaskClickListener listener;
    private String teacherAcronym;

    public interface OnTaskClickListener {
        void onTaskClick(UniTask task);
        void onDeleteClick(UniTask task);
    }
    // Modified constructor
    public StandaloneTasksAdapterTeacher(List<UniTask> tasks, String teacherAcronym, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.teacherAcronym = teacherAcronym;
        this.listener = listener;
    }

    public StandaloneTasksAdapterTeacher(List<UniTask> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standalone_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UniTask task = tasks.get(position);
        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDetails.setText(task.getTaskDetails());

        // Safe null handling
        String taskInstructor = task.getInstructorAcronym();
        boolean isOwnTask = teacherAcronym != null &&
                taskInstructor != null &&
                teacherAcronym.equalsIgnoreCase(taskInstructor);

        holder.deleteButton.setVisibility(isOwnTask ? View.VISIBLE : View.GONE);

        if (isOwnTask) {
            holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(task));
        }

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDetails;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDetails = itemView.findViewById(R.id.taskDetails);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(tasks.get(position));
                }
            });
        }
    }
}
