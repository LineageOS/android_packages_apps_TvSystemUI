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

package com.android.systemui.tv.volume.dialog.footer.ui.viewmodel

import android.graphics.drawable.Drawable
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialog
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialogScope
import com.android.systemui.volume.dialog.sliders.ui.viewmodel.VolumeDialogSlidersViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

@VolumeDialogScope
class TvVolumeDialogFooterViewModel
@Inject
constructor(
    slidersViewModel: VolumeDialogSlidersViewModel,
    @VolumeDialog private val coroutineScope: CoroutineScope,
) {

    val icon: Flow<Drawable> =
        slidersViewModel.sliders
            .flatMapLatest { it.sliderComponent.sliderViewModel().state }
            .map { it.icon }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
            .mapNotNull { it?.drawable }
}
