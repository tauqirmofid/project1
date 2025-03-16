package com.demo.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {
    private final List<Course> courseList;
    private final Consumer<Course> onAddClickListener;
    final Set<String> existingCourseKeys = new HashSet<>();
    final Set<String> conflictingSlotKeys = new HashSet<>();

    public CourseAdapter(List<Course> courseList, Consumer<Course> onAddClickListener) {
        this.courseList = courseList;
        this.onAddClickListener = onAddClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Generate unique identifiers
        String courseKey = generateCourseKey(
                course.getDay().toLowerCase(),
                course.getTimeSlot(),
                course.getCourseName()
        );

        String slotKey = generateSlotKey(
                course.getDay().toLowerCase(),
                course.getTimeSlot()
        );

        // Format display text
        String formattedDay = formatDayName(course.getDay());
        holder.tvCourseName.setText(String.format("%s: %s", formattedDay, course.getCourseName()));
        holder.tvDetails.setText(String.format("%s • %s • %s",
                course.getTimeSlot(),
                course.getInstructor(),
                course.getRoom()));

        // Determine course status
        boolean isAlreadyAdded = existingCourseKeys.contains(courseKey);
        boolean hasConflict = conflictingSlotKeys.contains(slotKey);

        // Update UI elements
        updateButtonState(holder.btnAdd, isAlreadyAdded);
        updateWarningText(holder.tvWarning, hasConflict);

        holder.btnAdd.setOnClickListener(v -> {
            if (!isAlreadyAdded && onAddClickListener != null) {
                onAddClickListener.accept(course);
            }
        });
    }

    private String formatDayName(String rawDay) {
        if (rawDay == null || rawDay.isEmpty()) return "";
        return rawDay.substring(0, 1).toUpperCase() + rawDay.substring(1).toLowerCase();
    }

    private void updateButtonState(Button button, boolean isAlreadyAdded) {
        button.setText(isAlreadyAdded ? "Already Added" : "Add");
        button.setEnabled(!isAlreadyAdded);
        button.setBackgroundTintList(ContextCompat.getColorStateList(
                button.getContext(),
                isAlreadyAdded ? R.color.colorError : R.color.themeGreen
        ));
    }

    private void updateWarningText(TextView warningText, boolean hasConflict) {
        if (hasConflict) {
            warningText.setVisibility(View.VISIBLE);
            warningText.setText("Timeslot conflict! Adding this will replace existing schedule.");
        } else {
            warningText.setVisibility(View.GONE);
        }
    }

    public void updateCourseStatus(Set<String> existingCourses, Set<String> conflictingSlots) {
        existingCourseKeys.clear();
        conflictingSlotKeys.clear();
        existingCourseKeys.addAll(existingCourses);
        conflictingSlotKeys.addAll(conflictingSlots);
        notifyDataSetChanged();
    }

    // Key generation helpers
    String generateCourseKey(String day, String timeSlot, String courseName) {
        return (day + "_" + timeSlot + "_" + courseName)
                .toLowerCase()
                .replaceAll("\\s+", "");
    }

    String generateSlotKey(String day, String timeSlot) {
        return (day + "_" + timeSlot)
                .toLowerCase()
                .replaceAll("\\s+", "");
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