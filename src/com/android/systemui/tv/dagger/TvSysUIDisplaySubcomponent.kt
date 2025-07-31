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

package com.android.systemui.tv.dagger

import com.android.systemui.display.dagger.PerDisplaySystemUIModule
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent.DisplayId
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent.PerDisplaySingleton
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * TV specific [Subcomponent] for SysUI classes that should be instantiated once per display.
 *
 * For more information, see documentation on the parent [SystemUIDisplaySubcomponent].
 */
@PerDisplaySingleton
@Subcomponent(modules = [PerDisplaySystemUIModule::class, PerDisplayTvSystemUIModule::class])
interface TvSysUIDisplaySubcomponent : SystemUIDisplaySubcomponent {

    @Subcomponent.Factory
    interface Factory : SystemUIDisplaySubcomponent.Factory {

        override fun create(@BindsInstance @DisplayId displayId: Int): TvSysUIDisplaySubcomponent
    }
}
