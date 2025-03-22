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

package com.android.systemui.tv.volume.dialog.dagger

import com.android.systemui.tv.volume.dialog.dagger.module.TvVolumeDialogModule
import com.android.systemui.volume.dialog.dagger.VolumeDialogComponent
import com.android.systemui.volume.dialog.dagger.factory.VolumeDialogComponentFactory
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialog
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialogScope
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope

@VolumeDialogScope
@Subcomponent(modules = [TvVolumeDialogModule::class])
interface TvVolumeDialogComponent : VolumeDialogComponent {

    @Subcomponent.Factory
    interface Factory : VolumeDialogComponentFactory {

        override fun create(
            @BindsInstance @VolumeDialog scope: CoroutineScope
        ): TvVolumeDialogComponent
    }
}
