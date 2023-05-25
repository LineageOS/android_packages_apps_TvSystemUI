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

import android.content.Context
import android.hardware.SensorPrivacyManager
import android.os.Handler
import com.android.internal.logging.UiEventLogger
import com.android.keyguard.KeyguardViewController
import com.android.systemui.Dependency
import com.android.systemui.dagger.ReferenceSystemUIModule
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.dock.DockManager
import com.android.systemui.dock.DockManagerImpl
import com.android.systemui.doze.DozeHost
import com.android.systemui.tv.hdmi.HdmiModule
import com.android.systemui.navigationbar.gestural.GestureModule
import com.android.systemui.plugins.qs.QSFactory
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.power.dagger.PowerModule
import com.android.systemui.privacy.MediaProjectionPrivacyItemMonitor
import com.android.systemui.privacy.PrivacyItemMonitor
import com.android.systemui.qs.dagger.QSModule
import com.android.systemui.qs.tileimpl.QSFactoryImpl
import com.android.systemui.recents.Recents
import com.android.systemui.recents.RecentsImplementation
import com.android.systemui.screenshot.ReferenceScreenshotModule
import com.android.systemui.tv.sensorprivacy.TvSensorPrivacyModule
import com.android.systemui.settings.dagger.MultiUserUtilsModule
import com.android.systemui.shade.NotificationShadeWindowControllerImpl
import com.android.systemui.shade.ShadeEmptyImplModule
import com.android.systemui.shade.ShadeExpansionStateManager
import com.android.systemui.statusbar.CommandQueue
import com.android.systemui.statusbar.KeyboardShortcutsModule
import com.android.systemui.statusbar.NotificationListener
import com.android.systemui.statusbar.NotificationLockscreenUserManager
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl
import com.android.systemui.statusbar.NotificationShadeWindowController
import com.android.systemui.statusbar.events.StatusBarEventsModule
import com.android.systemui.statusbar.notification.collection.provider.VisualStabilityProvider
import com.android.systemui.statusbar.notification.collection.render.GroupMembershipManager
import com.android.systemui.statusbar.phone.DozeServiceHost
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone
import com.android.systemui.statusbar.phone.KeyguardBypassController
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper
import com.android.systemui.statusbar.policy.AospPolicyModule
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl
import com.android.systemui.statusbar.policy.HeadsUpManager
import com.android.systemui.statusbar.policy.HeadsUpManagerLogger
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyController
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyControllerImpl
import com.android.systemui.statusbar.policy.SensorPrivacyController
import com.android.systemui.statusbar.policy.SensorPrivacyControllerImpl
import com.android.systemui.tv.notifications.TvNotificationHandler
import com.android.systemui.tv.notifications.TvNotificationsModule
import com.android.systemui.volume.dagger.VolumeModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Named

/**
 * A TV specific version of [ReferenceSystemUIModule].
 *
 * Code here should be specific to the TV variant of SystemUI and will not be included in other
 * variants of SystemUI.
 */
@Module(includes = [
    AospPolicyModule::class,
    GestureModule::class,
    HdmiModule::class,
    MultiUserUtilsModule::class,
    PowerModule::class,
    QSModule::class,
    ReferenceScreenshotModule::class,
    ShadeEmptyImplModule::class,
    StatusBarEventsModule::class,
    TvNotificationsModule::class,
    TvSensorPrivacyModule::class,
    VolumeModule::class,
    KeyboardShortcutsModule::class,
])
abstract class TvSystemUIModule {
    @Binds
    abstract fun bindNotificationLockscreenUserManager(
            notificationLockscreenUserManager: NotificationLockscreenUserManagerImpl): NotificationLockscreenUserManager

    @Binds
    @SysUISingleton
    abstract fun bindQSFactory(qsFactoryImpl: QSFactoryImpl): QSFactory

    @Binds
    abstract fun bindDockManager(dockManager: DockManagerImpl): DockManager

    @Binds
    abstract fun bindHeadsUpManagerPhone(headsUpManagerPhone: HeadsUpManagerPhone): HeadsUpManager

    @Binds
    abstract fun bindKeyguardViewController(
            statusBarKeyguardViewManager: StatusBarKeyguardViewManager): KeyguardViewController

    @Binds
    abstract fun bindNotificationShadeController(
            notificationShadeWindowController: NotificationShadeWindowControllerImpl): NotificationShadeWindowController

    @OptIn(ExperimentalCoroutinesApi::class)
    @Binds
    abstract fun provideDozeHost(dozeServiceHost: DozeServiceHost): DozeHost

    /**
     * Binds [MediaProjectionPrivacyItemMonitor] into the set of [PrivacyItemMonitor].
     */
    @Binds
    @IntoSet
    abstract fun bindMediaProjectionPrivacyItemMonitor(
            mediaProjectionPrivacyItemMonitor: MediaProjectionPrivacyItemMonitor): PrivacyItemMonitor

    companion object {
        @SysUISingleton
        @Provides
        @Named(Dependency.LEAK_REPORT_EMAIL_NAME)
        fun provideLeakReportEmail(): String = ""

        @Provides
        @SysUISingleton
        fun provideSensorPrivacyController(
                sensorPrivacyManager: SensorPrivacyManager): SensorPrivacyController =
                SensorPrivacyControllerImpl(sensorPrivacyManager).apply {
                    init()
                }

        @Provides
        @SysUISingleton
        fun provideIndividualSensorPrivacyController(
                sensorPrivacyManager: SensorPrivacyManager): IndividualSensorPrivacyController =
                IndividualSensorPrivacyControllerImpl(sensorPrivacyManager).apply {
                    init()
                }

        @SysUISingleton
        @Provides
        @Named(Dependency.ALLOW_NOTIFICATION_LONG_PRESS_NAME)
        fun provideAllowNotificationLongPress(): Boolean = true

        @SysUISingleton
        @Provides
        fun provideHeadsUpManagerPhone(
                context: Context,
                headsUpManagerLogger: HeadsUpManagerLogger,
                statusBarStateController: StatusBarStateController,
                bypassController: KeyguardBypassController,
                groupManager: GroupMembershipManager,
                visualStabilityProvider: VisualStabilityProvider,
                configurationController: ConfigurationController,
                @Main handler: Handler,
                accessibilityManagerWrapper: AccessibilityManagerWrapper,
                uiEventLogger: UiEventLogger,
                shadeExpansionStateManager: ShadeExpansionStateManager): HeadsUpManagerPhone {
            return HeadsUpManagerPhone(
                    context,
                    headsUpManagerLogger,
                    statusBarStateController,
                    bypassController,
                    groupManager,
                    visualStabilityProvider,
                    configurationController,
                    handler,
                    accessibilityManagerWrapper,
                    uiEventLogger,
                    shadeExpansionStateManager
            )
        }

        @Provides
        @SysUISingleton
        fun provideRecents(context: Context,
                           recentsImplementation: RecentsImplementation,
                           commandQueue: CommandQueue): Recents =
                Recents(context, recentsImplementation, commandQueue)

        @SysUISingleton
        @Provides
        fun providesDeviceProvisionedController(
                deviceProvisionedController: DeviceProvisionedControllerImpl): DeviceProvisionedController {
            deviceProvisionedController.init()
            return deviceProvisionedController
        }

        @Provides
        @SysUISingleton
        fun provideTvNotificationHandler(
                notificationListener: NotificationListener): TvNotificationHandler =
                TvNotificationHandler(notificationListener)
    }
}