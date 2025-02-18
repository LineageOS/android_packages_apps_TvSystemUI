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

package com.android.systemui.tv.volume.dialog.dagger.module

import com.android.systemui.tv.volume.dialog.footer.ui.binder.TvVolumeDialogFooterViewBinder
import com.android.systemui.tv.volume.dialog.header.ui.binder.TvVolumeDialogHeaderViewBinder
import com.android.systemui.tv.volume.dialog.slider.ui.binder.TvVolumeDialogSliderViewBinder
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialog
import com.android.systemui.volume.dialog.ringer.data.repository.VolumeDialogRingerFeedbackRepository
import com.android.systemui.volume.dialog.ringer.data.repository.VolumeDialogRingerFeedbackRepositoryImpl
import com.android.systemui.volume.dialog.sliders.dagger.VolumeDialogSliderComponent
import com.android.systemui.volume.dialog.ui.binder.ViewBinder
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(subcomponents = [VolumeDialogSliderComponent::class])
interface TvVolumeDialogModule {

    @Binds
    fun bindVolumeDialogRingerFeedbackRepository(
        ringerFeedbackRepository: VolumeDialogRingerFeedbackRepositoryImpl
    ): VolumeDialogRingerFeedbackRepository

    companion object {

        @Provides
        @VolumeDialog
        fun provideViewBinders(
            slidersViewBinder: TvVolumeDialogSliderViewBinder,
            headerViewBinder: TvVolumeDialogHeaderViewBinder,
            footerViewBinder: TvVolumeDialogFooterViewBinder,
        ): List<ViewBinder> = listOf(slidersViewBinder, headerViewBinder, footerViewBinder)
    }
}
