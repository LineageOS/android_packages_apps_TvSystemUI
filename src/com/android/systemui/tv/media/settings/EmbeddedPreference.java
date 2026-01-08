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

import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.EmbeddedSlicePreference;

/**
 * Slice preference for one panel settings which shows a setting like the {@link
 * BasicSlicePreference}, but takes its content from another slice.
 */
public class EmbeddedPreference extends EmbeddedSlicePreference {

    public EmbeddedPreference(Context context, String uri) {
        super(context, uri);
        setLayoutResource(R.layout.basic_slice_pref);
    }

}
