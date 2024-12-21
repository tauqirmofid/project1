package com.example.unimate;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class BatchListAdapter extends BaseAdapter {
    private Context context;
    private List<String> items;
    private int selectedPosition = -1;

    public BatchListAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_list_row, parent, false);
        }

        TextView tvItem = convertView.findViewById(R.id.tvItem);
        String batchNumber = items.get(position);
        tvItem.setText(batchNumber);

        // If this item is selected, make it dark green
        if (position == selectedPosition) {
            tvItem.setBackgroundColor(Color.parseColor("#008000")); // dark green
            tvItem.setTextColor(Color.WHITE);
        } else {
            tvItem.setBackgroundColor(Color.parseColor("#97C93C")); // light green
            tvItem.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}

