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

import androidx.annotation.Nullable;

import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.SliceSwitchPreference;
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;

/**
 * Slice preference for one panel settings which shows a switch/toggle in addition to the
 * capabilities of the {@link BasicSlicePreference}.
 */
public class SwitchSlicePreference extends SliceSwitchPreference {

    public SwitchSlicePreference(Context context, SliceActionImpl action) {
        this(context, null, action);
    }

    public SwitchSlicePreference(Context context, @Nullable AttributeSet attrs,
            SliceActionImpl action) {
        super(context, attrs, action);
        setLayoutResource(R.layout.switch_slice_preference);
    }

}
