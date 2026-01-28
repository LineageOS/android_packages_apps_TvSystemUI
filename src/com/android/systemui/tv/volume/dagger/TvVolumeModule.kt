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

package com.android.systemui.tv.volume.dagger

import android.content.BroadcastReceiver
import com.android.systemui.CoreStartable
import com.android.systemui.plugins.VolumeDialog
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.tv.volume.dialog.dagger.TvVolumeDialogPluginComponent
import com.android.systemui.volume.VolumeComponent
import com.android.systemui.volume.VolumeDialogComponent
import com.android.systemui.volume.VolumePanelDialogReceiver
import com.android.systemui.volume.VolumeUI
import com.android.systemui.volume.dagger.AncModule
import com.android.systemui.volume.dagger.AudioModule
import com.android.systemui.volume.dagger.AudioSharingModule
import com.android.systemui.volume.dagger.CaptioningModule
import com.android.systemui.volume.dagger.MediaDevicesModule
import com.android.systemui.volume.dagger.SpatializerModule
import com.android.systemui.volume.dialog.VolumeDialogPlugin
import com.android.systemui.volume.dialog.dagger.factory.VolumeDialogPluginComponentFactory
import com.android.systemui.volume.panel.dagger.VolumePanelComponent
import com.android.systemui.volume.panel.dagger.factory.VolumePanelComponentFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet

@Module(
    includes =
        [
            AudioModule::class,
            AudioSharingModule::class,
            AncModule::class,
            CaptioningModule::class,
            MediaDevicesModule::class,
            SpatializerModule::class,
        ],
    subcomponents = [VolumePanelComponent::class, TvVolumeDialogPluginComponent::class],
)
interface TvVolumeModule {

    @Binds
    @IntoMap
    @ClassKey(VolumePanelDialogReceiver::class)
    fun bindVolumePanelDialogReceiver(receiver: VolumePanelDialogReceiver): BroadcastReceiver

    @Binds
    @IntoMap
    @ClassKey(VolumeUI::class)
    fun bindVolumeUIStartable(impl: VolumeUI): CoreStartable

    @Binds
    @IntoSet
    fun bindVolumeUIConfigChanges(impl: VolumeUI): ConfigurationController.ConfigurationListener

    @Binds fun provideVolumeComponent(volumeDialogComponent: VolumeDialogComponent): VolumeComponent

    @Binds
    fun bindVolumePanelComponentFactory(
        impl: VolumePanelComponent.Factory
    ): VolumePanelComponentFactory

    @Binds
    fun bindVolumeDialogPluginComponentFactory(
        impl: TvVolumeDialogPluginComponent.Factory
    ): VolumeDialogPluginComponentFactory

    @Binds fun bindVolumeDialog(impl: VolumeDialogPlugin): VolumeDialog
}
