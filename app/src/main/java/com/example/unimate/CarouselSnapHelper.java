package com.example.unimate;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class CarouselSnapHelper extends PagerSnapHelper {
    private OrientationHelper orientationHelper;

    // Customize this to shift cards left/right after snapping
    private final int offsetAdjustmentPx = -180; // positive = shift right, negative = shift left

    @Override
    public int[] calculateDistanceToFinalSnap(
            @NonNull RecyclerView.LayoutManager layoutManager,
            @NonNull View targetView) {

        if (layoutManager.canScrollHorizontally()) {
            if (orientationHelper == null) {
                orientationHelper = OrientationHelper.createHorizontalHelper(layoutManager);
            }

            int childCenter = (orientationHelper.getDecoratedStart(targetView)
                    + orientationHelper.getDecoratedEnd(targetView)) / 2;
            int containerCenter = orientationHelper.getStartAfterPadding()
                    + orientationHelper.getTotalSpace() / 2;

            // Base distance
            int xDistance = childCenter - containerCenter;
            // Apply offset if needed
            xDistance += offsetAdjustmentPx;

            return new int[] {xDistance, 0};
        }
        return new int[] {0, 0};
    }
}

