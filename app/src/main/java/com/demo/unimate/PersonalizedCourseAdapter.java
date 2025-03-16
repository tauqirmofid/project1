package com.demo.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for showing existing personalized courses.
 * Has a "Remove" button to delete from Firestore.
 */
public class PersonalizedCourseAdapter extends RecyclerView.Adapter<PersonalizedCourseAdapter.ViewHolder> {

    private final List<Course> courseList;
    private final Consumer<Course> onRemoveClick;

    public PersonalizedCourseAdapter(List<Course> courseList, Consumer<Course> onRemoveClick) {
        this.courseList = courseList;
        this.onRemoveClick = onRemoveClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    // We re-use the same layout "item_course.xml".
    // Or you can make a new one if you prefer a different look for the "Remove" button.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Basic text
        holder.tvCourseName.setText(course.getCourseName());
        holder.tvDetails.setText(course.getDay() + "  |  "
                + course.getTimeSlot() + "  |  "
                + course.getInstructor() + "  |  "
                + course.getRoom());

        // We rename the button to "Remove"
        holder.btnAdd.setText("Remove");
        holder.btnAdd.setEnabled(true);
        holder.btnAdd.setBackgroundTintList(ContextCompat.getColorStateList(
                holder.btnAdd.getContext(),
                R.color.colorError // or any color you want for "Remove"
        ));

        // No warning text needed; we can hide it
        holder.tvWarning.setVisibility(View.GONE);

        // On click, pass back to the Activity so we can remove from Firestore
        holder.btnAdd.setOnClickListener(v -> {
            if (onRemoveClick != null) {
                onRemoveClick.accept(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName, tvDetails, tvWarning;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}
