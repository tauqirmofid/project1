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

    public TasksAdapter(List<UniTask> taskList, OnTaskRemoveListener removeListener) {
        this.taskList = taskList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        UniTask task = taskList.get(position);
        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDetails.setText(task.getTaskDetails());

        // Remove Task Button
        holder.removeButton.setOnClickListener(v -> {
            int removedPosition = taskList.indexOf(task);
            if (removedPosition != -1) {
                removeListener.onTaskRemoved(task); // Call interface to notify parent class
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // ✅ Instant removal animation
    public void removeTask(UniTask task) {
        int position = taskList.indexOf(task);
        if (position != -1) {
            taskList.remove(position);
            notifyItemRemoved(position);

            // ✅ If no more tasks, refresh the whole list after animation
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

    public interface OnTaskRemoveListener {
        void onTaskRemoved(UniTask task);
    }
}
