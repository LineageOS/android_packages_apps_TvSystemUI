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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;

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
    private View mItemView;
    private RadioButton mRadioButton;

    private final int mFocusedRadioTint;
    private final int mUnfocusedRadioTint;
    private final int mCheckedRadioTint;

    public RadioSlicePreference(Context context, SliceActionImpl action) {
        super(context,  action);
        setLayoutResource(R.layout.radio_slice_pref);

        Resources res = context.getResources();
        mFocusedRadioTint = res.getColor(R.color.media_dialog_radio_button_focused);
        mUnfocusedRadioTint = res.getColor(R.color.media_dialog_radio_button_unfocused);
        mCheckedRadioTint = res.getColor(R.color.media_dialog_radio_button_checked);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mItemView = holder.itemView;
        mItemView.setOnFocusChangeListener((v, hasFocus) -> updateColors());

        RadioControlWidget widget = (RadioControlWidget) mItemView;
        widget.setEnabled(this.isEnabled());
        widget.setTooltipConfig(mTooltipConfig);

        mRadioButton = mItemView.findViewById(android.R.id.checkbox);
        mRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> updateColors());
        updateColors();
    }

    private void updateColors() {
        Drawable drawable = mRadioButton.getButtonDrawable();
        if (mItemView.hasFocus()) {
            drawable.setTint(mRadioButton.isChecked() ? mCheckedRadioTint : mFocusedRadioTint);
        } else {
            drawable.setTint(mUnfocusedRadioTint);
        }
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
