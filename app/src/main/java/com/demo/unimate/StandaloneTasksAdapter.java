package com.demo.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StandaloneTasksAdapter extends RecyclerView.Adapter<StandaloneTasksAdapter.ViewHolder> {
    private static List<UniTask> tasks;
    private static OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(UniTask task);
        void onDeleteClick(UniTask task);
    }

    public StandaloneTasksAdapter(List<UniTask> tasks, OnTaskClickListener listener) {
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

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(task));
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
