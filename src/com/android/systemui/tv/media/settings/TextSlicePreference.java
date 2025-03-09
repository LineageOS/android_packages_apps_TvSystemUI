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

import androidx.preference.PreferenceViewHolder;

import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.CustomContentDescriptionPreference;

/**
 * Slice preference for one panel settings like the {@link BasicSlicePreference}, but not focusable.
 */
public class TextSlicePreference extends CustomContentDescriptionPreference {

    public TextSlicePreference(Context context) {
        this(context, null);
    }

    public TextSlicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.text_slice_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setFocusable(false);
    }
}
