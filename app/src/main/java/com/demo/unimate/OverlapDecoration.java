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
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int itemPosition = parent.getChildAdapterPosition(view);
        if (itemPosition != 0) {
            outRect.set(-overlapWidth, 0, 0, 0); // Adjust horizontal overlap
        }
    }
}
