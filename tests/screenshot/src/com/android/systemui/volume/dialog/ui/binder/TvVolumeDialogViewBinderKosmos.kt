/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.systemui.volume.dialog.ui.binder

import com.android.systemui.kosmos.Kosmos
import com.android.systemui.volume.dialog.footer.ui.viewbinder.tvVolumeDialogFooterViewBinder
import com.android.systemui.volume.dialog.header.ui.binder.tvVolumeDialogHeaderViewBinder
import com.android.systemui.volume.dialog.slider.ui.binder.tvVolumeDialogSliderViewBinder

val Kosmos.tvSystemUiVolumeDialogViewBinders: List<ViewBinder> by
    Kosmos.Fixture {
        listOf(
            tvVolumeDialogSliderViewBinder,
            tvVolumeDialogFooterViewBinder,
            tvVolumeDialogHeaderViewBinder,
        )
    }
