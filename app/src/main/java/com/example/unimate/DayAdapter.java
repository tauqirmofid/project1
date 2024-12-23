package com.example.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private List<DayModel> dayList;

    public DayAdapter(List<DayModel> dayList) {
        this.dayList = dayList;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_card, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        // Map the big position to our 7-day list
        int actualPosition = position % dayList.size();
        DayModel currentDay = dayList.get(actualPosition);

        holder.tvDayName.setText(currentDay.getDayName());
        holder.tvTasks.setText(currentDay.getTasks());

        // Assign colors by day:
        switch (currentDay.getDayName()) {
            case "Monday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;

            case "Tuesday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenLight)
                );
                break;

            case "Wednesday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;

            case "Thursday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenLight)
                );
                break;

            case "Friday":
                // Friday has a special color: White
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorWhite)
                );
                break;

            case "Saturday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;

            case "Sunday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenLight)
                );
                break;

            default:
                // Fallback color if day is unknown
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;
        }

        // Decide text color for tvDayName
        switch (currentDay.getDayName()) {
            case "Monday":
            case "Wednesday":
            case "Friday":
            case "Saturday":
                // #97C93C
                holder.tvDayName.setTextColor(
                        ContextCompat.getColor(
                                holder.itemView.getContext(), R.color.colorGreenLight
                        )
                );
                break;

            default:
                // #006A00
                holder.tvDayName.setTextColor(
                        ContextCompat.getColor(
                                holder.itemView.getContext(), R.color.colorGreenDark
                        )
                );
                break;
        }
    }




    @Override
    public int getItemCount() {
        // Return a large number so it appears infinite
        return Integer.MAX_VALUE;
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDayName;
        TextView tvTasks;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.dayCardView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvTasks = itemView.findViewById(R.id.time1);
        }
    }

}
