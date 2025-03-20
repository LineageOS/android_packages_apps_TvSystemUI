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
import com.android.tv.twopanelsettings.slices.SliceRadioPreference;
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;

/**
 * Slice preference for one panel settings which shows a radio button in addition to the
 * capabilities of the {@link BasicSlicePreference}.
 */
public class RadioSlicePreference extends SliceRadioPreference implements TooltipPreference {
    private ControlWidget.TooltipConfig mTooltipConfig = new ControlWidget.TooltipConfig();

    public RadioSlicePreference(Context context, SliceActionImpl action) {
        super(context,  action);
        setLayoutResource(R.layout.radio_slice_pref);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        RadioControlWidget widget = (RadioControlWidget) holder.itemView;
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

    static class RadioControlWidget extends ControlWidget {

        public RadioControlWidget(Context context) {
            this(context, /* attrs= */ null);
        }

        public RadioControlWidget(Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, /* defStyleAttr= */ 0);
        }

        public RadioControlWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            View.inflate(context, R.layout.radio_control_widget, /* root= */ this);
        }
    }

}
