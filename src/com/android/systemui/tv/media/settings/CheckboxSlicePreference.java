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
import android.widget.CheckBox;

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
public class CheckboxSlicePreference extends SliceCheckboxPreference {
    private View mItemView;
    private CheckBox mCheckBox;

    private final int mFocusedCheckboxTint;
    private final int mUnfocusedCheckboxTint;
    private final int mCheckedCheckboxTint;

    public CheckboxSlicePreference(Context context, SliceActionImpl action) {
        this(context, null, action);
    }

    public CheckboxSlicePreference(Context context, @Nullable AttributeSet attrs,
            SliceActionImpl action) {
        super(context, attrs, action);
        setLayoutResource(R.layout.checkbox_slice_preference);

        Resources res = context.getResources();
        mFocusedCheckboxTint = res.getColor(R.color.media_dialog_radio_button_focused);
        mUnfocusedCheckboxTint = res.getColor(R.color.media_dialog_radio_button_unfocused);
        mCheckedCheckboxTint = res.getColor(R.color.media_dialog_radio_button_checked);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mItemView = holder.itemView;
        mItemView.setOnFocusChangeListener((v, hasFocus) -> updateColors());

        mCheckBox = mItemView.findViewById(android.R.id.checkbox);
        mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateColors());
        updateColors();
    }

    private void updateColors() {
        Drawable drawable = mCheckBox.getButtonDrawable();
        if (mItemView.hasFocus()) {
            drawable.setTint(mCheckBox.isChecked() ? mCheckedCheckboxTint : mFocusedCheckboxTint);
        } else {
            drawable.setTint(mUnfocusedCheckboxTint);
        }
    }

}
