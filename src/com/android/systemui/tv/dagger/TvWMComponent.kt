/*
 * Copyright (C) 2023 The Android Open Source Project
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

import com.android.systemui.dagger.WMComponent
import com.android.wm.shell.dagger.TvWMShellModule
import com.android.wm.shell.dagger.WMSingleton
import dagger.Subcomponent

/**
 * Dagger Subcomponent for WindowManager.
 */
@WMSingleton
@Subcomponent(modules = [TvWMShellModule::class])
interface TvWMComponent : WMComponent {
    /**
     * Builder for a SysUIComponent.
     */
    @Subcomponent.Builder
    interface Builder : WMComponent.Builder {
        override fun build(): TvWMComponent
    }
}