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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.SliceCheckboxPreference;
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;

/**
 * Slice preference for one panel settings which shows a checkbox in addition to the capabilities of
 * {@link BasicSlicePreference}.
 */
public class CheckboxSlicePreference extends SliceCheckboxPreference implements TooltipPreference {
    private ControlWidget.TooltipConfig mTooltipConfig = new ControlWidget.TooltipConfig();

    public CheckboxSlicePreference(Context context, SliceActionImpl action) {
        this(context, null, action);
    }

    public CheckboxSlicePreference(Context context, @Nullable AttributeSet attrs,
            SliceActionImpl action) {
        super(context, attrs, action);
        setLayoutResource(R.layout.checkbox_slice_pref);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        CheckboxControlWidget widget = (CheckboxControlWidget) holder.itemView;
        widget.setEnabled(this.isEnabled());
        widget.setTooltipConfig(mTooltipConfig);
    }

    /** Set tool tip related attributes. */
    @Override
    public void setTooltipConfig(ControlWidget.TooltipConfig tooltipConfig) {
        if (!this.mTooltipConfig.equals(tooltipConfig)) {
            this.mTooltipConfig = tooltipConfig;
            notifyChanged();
        }
    }

    static class CheckboxControlWidget extends ControlWidget {

        public CheckboxControlWidget(Context context) {
            this(context, /* attrs= */ null);
        }

        public CheckboxControlWidget(Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, /* defStyleAttr= */ 0);
        }

        public CheckboxControlWidget(Context context, @Nullable AttributeSet attrs,
                int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            View.inflate(context, R.layout.checkbox_control_widget, /* root= */ this);
        }
    }

}
