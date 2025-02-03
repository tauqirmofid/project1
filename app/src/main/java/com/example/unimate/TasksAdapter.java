package com.example.unimate;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {
    private List<UniTask> taskList;
    private OnTaskRemoveListener removeListener;
    private OnTaskClickListener clickListener; // New click listener

    // Existing interface (keep unchanged)
    public interface OnTaskRemoveListener {
        void onTaskRemoved(UniTask task);
    }

    // New interface for click events
    public interface OnTaskClickListener {
        void onTaskClick(UniTask task);
        void onDeleteClick(UniTask task);
    }

    // Existing constructor (maintain for backward compatibility)
    public TasksAdapter(List<UniTask> taskList, OnTaskRemoveListener removeListener) {
        this.taskList = taskList;
        this.removeListener = removeListener;
    }

    // New constructor with both listeners
    public TasksAdapter(List<UniTask> taskList,
                        OnTaskRemoveListener removeListener,
                        OnTaskClickListener clickListener) {
        this.taskList = taskList;
        this.removeListener = removeListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        UniTask task = taskList.get(position);
        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDetails.setText(task.getTaskDetails());

        // Existing remove button functionality (keep unchanged)
        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onTaskRemoved(task);
            }
        });

        // New click handling for entire item
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onTaskClick(task));

            // Update delete button to use new listener
            holder.removeButton.setOnClickListener(v -> clickListener.onDeleteClick(task));
        }
    }

    // Existing methods (keep unchanged)
    @Override
    public int getItemCount() { return taskList.size(); }

    public void removeTask(UniTask task) {
        int position = taskList.indexOf(task);
        if (position != -1) {
            taskList.remove(position);
            notifyItemRemoved(position);
            new Handler().postDelayed(() -> notifyDataSetChanged(), 300);
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDetails;
        Button removeButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitleText);
            taskDetails = itemView.findViewById(R.id.taskDetailsText);
            removeButton = itemView.findViewById(R.id.removeTaskBtn);
        }
    }
}