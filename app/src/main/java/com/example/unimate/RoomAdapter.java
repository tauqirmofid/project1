package com.example.unimate;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private Context context;
    private List<RoomModel> roomList;

    public RoomAdapter(Context context, List<RoomModel> roomList) {
        this.context = context;
        this.roomList = roomList;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rooms_location_card, parent, false);
        return new RoomViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomModel room = roomList.get(position);

        holder.buildingName.setText(room.getBuilding());
        holder.floor.setText(room.getFloor()); // Set floor number
        holder.roomNumber.setText(room.getRoomNumber());

        String imageUrl = "https://res.cloudinary.com/dp4ha5cws/image/upload/" + room.getImageKey();

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic)
                .error(R.drawable.database_error)
                .into(holder.roomImage);
        // Add click listener for the card
        holder.itemView.setOnClickListener(v -> showFullscreenImage(imageUrl));
        //Log.d(TAG, "Loaded Room: " + room.getRoomNumber() + ", Floor: " + room.getFloor() + ", Image URL: " + imageUrl);
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {

        TextView buildingName, floor, roomNumber;
        ImageView roomImage;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            buildingName = itemView.findViewById(R.id.building);
            floor = itemView.findViewById(R.id.floor); // Find floor TextView
            roomNumber = itemView.findViewById(R.id.roomNum);
            roomImage = itemView.findViewById(R.id.room_pic);
        }
    }
    private void showFullscreenImage(String imageUrl) {
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_image, null);

        // Initialize dialog components
        ImageView fullscreenImage = dialogView.findViewById(R.id.fullscreenImage);
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);

        // Load the image into the fullscreen ImageView
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.loading_pic)
                .error(R.drawable.database_error)
                .into(fullscreenImage);

        // Create the dialog
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Close button listener
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }
    public void updateList(List<RoomModel> newList) {
        roomList = newList;
        notifyDataSetChanged();
    }

}
