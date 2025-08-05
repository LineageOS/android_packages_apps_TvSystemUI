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

import com.android.systemui.tv.volume.dialog.dagger.TvVolumeDialogComponent
import com.android.systemui.volume.dialog.dagger.factory.VolumeDialogComponentFactory
import com.android.systemui.volume.dialog.shared.model.CsdWarningConfigModel
import com.android.systemui.volume.dialog.utils.VolumeTracer
import com.android.systemui.volume.dialog.utils.VolumeTracerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(subcomponents = [TvVolumeDialogComponent::class])
interface TvVolumeDialogPluginModule {

    @Binds
    fun bindVolumeDialogComponentFactory(
        factory: TvVolumeDialogComponent.Factory
    ): VolumeDialogComponentFactory

    @Binds fun bindVolumeTracer(volumeTracer: VolumeTracerImpl): VolumeTracer

    companion object {

        @Provides
        fun provideCsdWarningConfigModel(): CsdWarningConfigModel =
            CsdWarningConfigModel(emptyList())
    }
}
