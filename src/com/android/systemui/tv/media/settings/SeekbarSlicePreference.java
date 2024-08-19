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

package com.android.systemui.tv.media.settings;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD;

import android.content.Context;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.SliceSeekbarPreference;
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;

/**
 * Slice preference for one panel settings a small icon, title, seekbar and the current seekbar
 * value. Large non-themed icons/images are not supported.
 */
public class SeekbarSlicePreference extends SliceSeekbarPreference implements TooltipPreference {
    private ControlWidget.TooltipConfig mTooltipConfig = new ControlWidget.TooltipConfig();
    private SeekBar mSeekbar;

    private SeekBar.OnSeekBarChangeListener mChangeListener;

    public SeekbarSlicePreference(Context context, SliceActionImpl action, int min, int max,
            int value) {
        this(context, null, action, min, max, value);
    }

    public SeekbarSlicePreference(Context context, AttributeSet attrs, SliceActionImpl action,
            int min, int max, int value) {
        super(context, attrs, action, min, max, value);
        setLayoutResource(R.layout.seekbar_slice_pref);
        setShowSeekBarValue(true);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        SeekbarControlWidget seekbarControlWidget = (SeekbarControlWidget) holder.itemView;
        seekbarControlWidget.setEnabled(this.isEnabled());
        seekbarControlWidget.setTooltipConfig(mTooltipConfig);

        mSeekbar = holder.itemView.requireViewById(R.id.seekbar);

        // Set outline of the seekbar to clip the thumb when getting closer to 0.
        mSeekbar.setOutlineProvider(
                new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(
                                /* left= */ 0,
                                /* top= */ 0,
                                view.getWidth(),
                                view.getHeight(),
                                getContext().getResources().getDimensionPixelOffset(
                                        R.dimen.seekbar_widget_track_corner_radius));
                    }
                });
        mSeekbar.setClipToOutline(true);

        holder.itemView.setAccessibilityDelegate(
                new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                            @NonNull AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.addAction(ACTION_SET_PROGRESS);
                        info.setRangeInfo(getCurrentRange());
                    }

                    @Override
                    public boolean performAccessibilityAction(@NonNull View host, int action,
                            Bundle args) {
                        if (action == ACTION_SCROLL_FORWARD.getId()) {
                            return increaseValue();
                        }
                        if (action == ACTION_SCROLL_BACKWARD.getId()) {
                            return decreaseValue();
                        }
                        return super.performAccessibilityAction(host, action, args);
                    }
                });

        mSeekbar.setOnSeekBarChangeListener(mChangeListener);
    }

    public void setOnSeekbarChangedListener(SeekBar.OnSeekBarChangeListener listener) {
        mChangeListener = listener;
        if (mSeekbar != null) {
            mSeekbar.setOnSeekBarChangeListener(listener);
        }
    }

    public AccessibilityNodeInfo.RangeInfo getCurrentRange() {
        return new AccessibilityNodeInfo.RangeInfo(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, getMin(), getMax(), getValue());
    }

    private boolean increaseValue() {
        int newProgress = getValue() + getSeekBarIncrement();

        if (newProgress <= getMax()) {
            setValue(newProgress);
            callChangeListener(newProgress);
            return true;
        }
        return false;
    }

    private boolean decreaseValue() {
        int newProgress = getValue() - getSeekBarIncrement();
        if (newProgress >= getMin()) {
            setValue(newProgress);
            callChangeListener(newProgress);
            return true;
        }
        return false;
    }

    /** Set tool tip related attributes. */
    @Override
    public void setTooltipConfig(ControlWidget.TooltipConfig tooltipConfig) {
        if (!this.mTooltipConfig.equals(tooltipConfig)) {
            this.mTooltipConfig = tooltipConfig;
            notifyChanged();
        }
    }

    static class SeekbarControlWidget extends ControlWidget {
        public SeekbarControlWidget(Context context) {
            this(context, /* attrs= */ null);
        }

        public SeekbarControlWidget(Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, /* defStyleAttr= */ 0);
        }

        @SuppressWarnings("nullness")
        public SeekbarControlWidget(Context context, @Nullable AttributeSet attrs,
                int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            View.inflate(context, R.layout.seekbar_control_widget, /* root= */ this);
        }
    }
}
