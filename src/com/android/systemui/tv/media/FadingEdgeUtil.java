/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.tv.media;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.systemui.tv.res.R;

public class FadingEdgeUtil {

    @Nullable
    public static Drawable getForegroundDrawable(RecyclerView recyclerView, Context context) {
        if (shouldShowTopFadingEdge(recyclerView) && shouldShowBottomFadingEdge(recyclerView)) {
            return context.getDrawable(R.drawable.both_end_fading_edge);
        } else if (shouldShowBottomFadingEdge(recyclerView)) {
            return context.getDrawable(R.drawable.bottom_fading_edge);
        } else if (shouldShowTopFadingEdge(recyclerView)) {
            return context.getDrawable(R.drawable.top_fading_edge);
        }
        return null;
    }

    @SuppressWarnings("nullness")
    private static boolean shouldShowTopFadingEdge(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        View firstVisibleChildView = layoutManager.getChildAt(0);
        if (firstVisibleChildView == null) {
            return false;
        }
        int positionOfCurrentFistView = layoutManager.getPosition(firstVisibleChildView);
        boolean isFirstAdapterItemVisible = (positionOfCurrentFistView == 0);
        if (!isFirstAdapterItemVisible) {
            return true;
        }
        int top = firstVisibleChildView.getTop();
        return top < 0;
    }

    @SuppressWarnings("nullness")
    private static boolean shouldShowBottomFadingEdge(RecyclerView recyclerView) {
        // Get adapter's last child View.
        View lastChildView = null;
        RecyclerView.ViewHolder holder =
                recyclerView.findViewHolderForAdapterPosition(
                        recyclerView.getAdapter().getItemCount() - 1);
        if (holder != null) {
            lastChildView = holder.itemView;
        }
        if (lastChildView == null) {
            // If last child is not inflated yet, then it definitely is not visible.
            return true;
        }
        if (lastChildView.getTop() >= recyclerView.getBottom()) {
            // If last child is below dashboard's bottom edge, then it should be invisible
            // currently.
            // Sometimes even if the last item is not visible, it still get counted in
            // layoutManager's
            // children. So use the coordinates would be more precise.
            return true;
        }
        int rvBottom = recyclerView.getBottom();
        int bottom = lastChildView.getBottom();
        int bottomMargin =
                ((ViewGroup.MarginLayoutParams) lastChildView.getLayoutParams()).bottomMargin;
        return bottom + bottomMargin > rvBottom;
    }

}
