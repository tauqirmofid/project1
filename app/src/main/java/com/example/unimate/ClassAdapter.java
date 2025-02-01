package com.example.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimate.R;

import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private List<ClassWithTasks> classes;
    private final OnClassClickListener clickListener;

    public interface OnClassClickListener {
        void onClassClick(ClassWithTasks classItem);
    }

    public ClassAdapter(List<ClassWithTasks> classes, OnClassClickListener listener) {
        this.classes = classes != null ? classes : new ArrayList<>();
        this.clickListener = listener;
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

        if (item.hasTasks()) {
            holder.taskIndicator.setVisibility(View.VISIBLE);
            holder.taskIndicator.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.taskOrange));
        } else {
            holder.taskIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView classTime, className;
        View taskIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            classTime = itemView.findViewById(R.id.classTime);
            className = itemView.findViewById(R.id.className);
            taskIndicator = itemView.findViewById(R.id.taskIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onClassClick(classes.get(position));
                }
            });
        }
    }
}