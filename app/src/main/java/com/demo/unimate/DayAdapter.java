package com.demo.unimate;

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
        // Map the big position to our 7-day list (infinite scrolling)
        int actualPosition = position % dayList.size();
        DayModel currentDay = dayList.get(actualPosition);

        // 1) The day name at the top
        holder.tvDayName.setText(currentDay.getDayName());

        // 2) Timeslot rows
        // We already set android:text="9:00-10:20AM" etc. in XML for time1..time7.
        // Now we fill the "class" text for each row:
        holder.class1.setText(currentDay.getClass1());
        holder.class2.setText(currentDay.getClass2());
        holder.class3.setText(currentDay.getClass3());
        holder.class4.setText(currentDay.getClass4());
        holder.class5.setText(currentDay.getClass5());
        holder.class6.setText(currentDay.getClass6());
        holder.class7.setText(currentDay.getClass7());

        // Assign colors by day (same logic you had before)
        switch (currentDay.getDayName()) {
            case "Monday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;

            case "Tuesday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.green_dark)
                );
                break;

            case "Wednesday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.colorGreenDark)
                );
                break;

            case "Thursday":
                holder.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.green_dark)
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
                        ContextCompat.getColor(holder.cardView.getContext(), R.color.green_dark)
                );
                break;

            default:
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
                                holder.itemView.getContext(), R.color.colorGreenLight
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

        // The 7 "class" fields in item_day_card.xml
        TextView class1, class2, class3, class4, class5, class6, class7;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.dayCardView);
            tvDayName = itemView.findViewById(R.id.tvDayName);

            // Each row: timeX is fixed in XML, classX is what we fill
            class1 = itemView.findViewById(R.id.class1);
            class2 = itemView.findViewById(R.id.class2);
            class3 = itemView.findViewById(R.id.class3);
            class4 = itemView.findViewById(R.id.class4);
            class5 = itemView.findViewById(R.id.class5);
            class6 = itemView.findViewById(R.id.class6);
            class7 = itemView.findViewById(R.id.class7);
        }
    }
}
