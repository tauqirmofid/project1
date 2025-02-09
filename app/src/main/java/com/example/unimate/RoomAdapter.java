package com.example.unimate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.SearchViewHolder> {

    private List<RoomModel> roomList;

    public RoomAdapter(RoomsActivity roomsActivity, List<RoomModel> roomList) {
        this.roomList = roomList;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rooms_location_card, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        RoomModel room = roomList.get(position);

        // Set data
        holder.building.setText(room.getBuilding());
        holder.roomNum.setText(room.getFloor() + " - " + room.getRoomNumber());

        // Load image using Glide
        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + room.getImageKey();
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.loading_pic) // Replace with a local image for testing
                .into(holder.roomPic);

    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView building, roomNum;
        ImageView roomPic;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            building = itemView.findViewById(R.id.building);
            roomNum = itemView.findViewById(R.id.roomNum);
            roomPic = itemView.findViewById(R.id.room_pic);
        }
    }
}
