package com.demo.unimate;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This ItemDecoration creates negative spacing between items
 * so that each new card overlaps the previous one horizontally.
 */
public class OverlapDecoration extends RecyclerView.ItemDecoration {

    private final int overlapWidth;

    public OverlapDecoration(int overlapWidth) {
        this.overlapWidth = overlapWidth;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        // Position of the item in the adapter
        int itemPosition = parent.getChildAdapterPosition(view);

        // If it's not the first item, move it left by overlapWidth.
        // A negative left offset means it overlaps the previous card.
        if (itemPosition != 0) {
            outRect.set(-overlapWidth, 50, 0, 0);
        }
    }
}
