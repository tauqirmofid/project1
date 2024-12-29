package com.example.unimate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
    private Context context;
    private List<RequestModel> requestList;
    private List<RequestModel> allRequests; // Keep track of the full list for filtering
    private RequestActionListener actionListener;

    public RequestAdapter(Context context, List<RequestModel> requestList, RequestActionListener actionListener) {
        this.context = context;
        this.requestList = requestList;
        this.allRequests = new ArrayList<>(requestList); // Make a copy of the original list
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        if (request.getEmail() != null) {
            holder.emailTextView.setText(request.getEmail());
        } else {
            holder.emailTextView.setText("No Email Provided");
        }

        if (request.getId() != null) {
            holder.idTextView.setText("ID: " + request.getId());
        } else {
            holder.idTextView.setText("No ID Available");
        }

        if (request.getPhone() != null) {
            holder.phoneTextView.setText("Phone: " + request.getPhone());
        } else {
            holder.phoneTextView.setText("No Phone Provided");
        }

        holder.viewDetailsButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewDetails(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    // Method to update data dynamically
    public void updateData(List<RequestModel> newRequests) {
        this.requestList = newRequests;
        this.allRequests = new ArrayList<>(newRequests); // Update the full list for filtering
        notifyDataSetChanged();
    }

    // Method to filter accepted or rejected requests
    public void filterAcceptedRejectedRequests(boolean isAccepted) {
        List<RequestModel> filteredList = new ArrayList<>();
        for (RequestModel request : allRequests) {
            if (request.isVerified() == isAccepted) {
                filteredList.add(request);
            }
        }
        this.requestList = filteredList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView, idTextView, phoneTextView, extraDetailsTextView;
        Button viewDetailsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            idTextView = itemView.findViewById(R.id.idTextView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);
            extraDetailsTextView = itemView.findViewById(R.id.extraDetailsTextView);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }

    public interface RequestActionListener {
        void onViewDetails(RequestModel request);
    }
}
