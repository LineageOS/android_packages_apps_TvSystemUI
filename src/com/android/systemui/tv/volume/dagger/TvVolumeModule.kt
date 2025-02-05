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
import android.content.Context
import android.media.AudioManager
import android.os.Looper
import com.android.internal.jank.InteractionJankMonitor
import com.android.systemui.CoreStartable
import com.android.systemui.Flags
import com.android.systemui.dump.DumpManager
import com.android.systemui.media.dialog.MediaOutputDialogManager
import com.android.systemui.plugins.VolumeDialog
import com.android.systemui.plugins.VolumeDialogController
import com.android.systemui.statusbar.VibratorHelper
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.DevicePostureController
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.util.settings.SecureSettings
import com.android.systemui.util.time.SystemClock
import com.android.systemui.volume.CsdWarningDialog
import com.android.systemui.volume.VolumeComponent
import com.android.systemui.volume.VolumeDialogComponent
import com.android.systemui.volume.VolumeDialogImpl
import com.android.systemui.volume.VolumePanelDialogReceiver
import com.android.systemui.volume.VolumeUI
import com.android.systemui.volume.dagger.AncModule
import com.android.systemui.volume.dagger.AudioModule
import com.android.systemui.volume.dagger.AudioSharingModule
import com.android.systemui.volume.dagger.CaptioningModule
import com.android.systemui.volume.dagger.MediaDevicesModule
import com.android.systemui.volume.dagger.SpatializerModule
import com.android.systemui.volume.dialog.VolumeDialogPlugin
import com.android.systemui.volume.dialog.dagger.VolumeDialogPluginComponent
import com.android.systemui.volume.dialog.dagger.factory.VolumeDialogPluginComponentFactory
import com.android.systemui.volume.domain.interactor.VolumeDialogInteractor
import com.android.systemui.volume.domain.interactor.VolumePanelNavigationInteractor
import com.android.systemui.volume.panel.dagger.VolumePanelComponent
import com.android.systemui.volume.panel.dagger.factory.VolumePanelComponentFactory
import com.android.systemui.volume.panel.shared.flag.VolumePanelFlag
import com.android.systemui.volume.ui.navigation.VolumeNavigator
import com.google.android.msdl.domain.MSDLPlayer
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
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
    subcomponents = [VolumePanelComponent::class, VolumeDialogPluginComponent::class],
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

    @Binds
    fun provideVolumeComponent(volumeDialogComponent: VolumeDialogComponent): VolumeComponent

    @Binds
    fun bindVolumePanelComponentFactory(
        impl: VolumePanelComponent.Factory
    ): VolumePanelComponentFactory

    @Binds
    fun bindVolumeDialogPluginComponentFactory(
        impl: VolumeDialogPluginComponent.Factory
    ): VolumeDialogPluginComponentFactory

    companion object {
        @Provides
        fun provideVolumeDialog(
            volumeDialogProvider: Lazy<VolumeDialogPlugin>,
            context: Context,
            volumeDialogController: VolumeDialogController,
            accessibilityManagerWrapper: AccessibilityManagerWrapper,
            deviceProvisionedController: DeviceProvisionedController,
            configurationController: ConfigurationController,
            mediaOutputDialogManager: MediaOutputDialogManager,
            interactionJankMonitor: InteractionJankMonitor,
            volumePanelNavigationInteractor: VolumePanelNavigationInteractor,
            volumeNavigator: VolumeNavigator,
            csdFactory: CsdWarningDialog.Factory,
            devicePostureController: DevicePostureController,
            volumePanelFlag: VolumePanelFlag,
            dumpManager: DumpManager,
            secureSettings: Lazy<SecureSettings>,
            vibratorHelper: VibratorHelper,
            msdlPlayer: MSDLPlayer,
            systemClock: SystemClock,
            interactor: VolumeDialogInteractor,
        ): VolumeDialog {
            if (Flags.volumeRedesign()) {
                return volumeDialogProvider.get()
            } else {
                val impl =
                    VolumeDialogImpl(
                        context,
                        volumeDialogController,
                        accessibilityManagerWrapper,
                        deviceProvisionedController,
                        configurationController,
                        mediaOutputDialogManager,
                        interactionJankMonitor,
                        volumePanelNavigationInteractor,
                        volumeNavigator,
                        true,
                        csdFactory,
                        devicePostureController,
                        Looper.getMainLooper(),
                        volumePanelFlag,
                        dumpManager,
                        secureSettings,
                        vibratorHelper,
                        msdlPlayer,
                        systemClock,
                        interactor,
                    )
                impl.setStreamImportant(AudioManager.STREAM_SYSTEM, false)
                impl.setAutomute(true)
                impl.setSilentMode(false)
                return impl
            }
        }
    }
}
