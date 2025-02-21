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

import com.android.systemui.CoreStartable
import com.android.systemui.SliceBroadcastRelayHandler
import com.android.systemui.accessibility.Magnification
import com.android.systemui.dagger.qualifiers.PerUser
import com.android.systemui.globalactions.GlobalActionsComponent
import com.android.systemui.keyboard.KeyboardUI
import com.android.systemui.media.RingtonePlayer
import com.android.systemui.media.dialog.MediaOutputSwitcherDialogUI
import com.android.systemui.media.systemsounds.HomeSoundEffectController
import com.android.systemui.shortcut.ShortcutKeyDispatcher
import com.android.systemui.statusbar.notification.InstantAppNotifier
import com.android.systemui.toast.ToastUI
import com.android.systemui.tv.notifications.TvNotificationHandler
import com.android.systemui.tv.notifications.TvNotificationPanel
import com.android.systemui.tv.statusbar.TvStatusBar
import com.android.systemui.tv.vpn.VpnStatusObserver
import com.android.systemui.usb.StorageNotification
import com.android.systemui.util.NotificationChannels
import com.android.systemui.wmshell.WMShell
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/**
 * DEPRECATED: DO NOT ADD THINGS TO THIS FILE.
 *
 * Add a feature specific dagger module for what you are working on. Bind your CoreStartable there.
 * Include that module where it is needed.
 *
 * @deprecated
 */
@Module
abstract class TVSystemUICoreStartableModule {
    /** Inject into GlobalActionsComponent.  */
    @Binds
    @IntoMap
    @ClassKey(GlobalActionsComponent::class)
    abstract fun bindGlobalActionsComponent(sysui: GlobalActionsComponent): CoreStartable

    /** Inject into HomeSoundEffectController.  */
    @Binds
    @IntoMap
    @ClassKey(HomeSoundEffectController::class)
    abstract fun bindHomeSoundEffectController(sysui: HomeSoundEffectController): CoreStartable

    /** Inject into InstantAppNotifier.  */
    @Binds
    @IntoMap
    @ClassKey(InstantAppNotifier::class)
    abstract fun bindInstantAppNotifier(sysui: InstantAppNotifier): CoreStartable

    /** Inject into KeyboardUI.  */
    @Binds
    @IntoMap
    @ClassKey(KeyboardUI::class)
    abstract fun bindKeyboardUI(sysui: KeyboardUI): CoreStartable

    /** Inject into MediaOutputSwitcherDialogUI.  */
    @Binds
    @IntoMap
    @ClassKey(MediaOutputSwitcherDialogUI::class)
    abstract fun bindMediaOutputSwitcherDialogUI(sysui: MediaOutputSwitcherDialogUI): CoreStartable

    /** Inject into NotificationChannels.  */
    @Binds
    @IntoMap
    @ClassKey(NotificationChannels::class)
    @PerUser
    abstract fun bindNotificationChannels(sysui: NotificationChannels): CoreStartable

    /** Inject into RingtonePlayer.  */
    @Binds
    @IntoMap
    @ClassKey(RingtonePlayer::class)
    abstract fun bind(sysui: RingtonePlayer): CoreStartable

    /** Inject into ShortcutKeyDispatcher.  */
    @Binds
    @IntoMap
    @ClassKey(ShortcutKeyDispatcher::class)
    abstract fun bindShortcutKeyDispatcher(sysui: ShortcutKeyDispatcher): CoreStartable

    /** Inject into SliceBroadcastRelayHandler.  */
    @Binds
    @IntoMap
    @ClassKey(SliceBroadcastRelayHandler::class)
    abstract fun bindSliceBroadcastRelayHandler(sysui: SliceBroadcastRelayHandler): CoreStartable

    /** Inject into StorageNotification.  */
    @Binds
    @IntoMap
    @ClassKey(StorageNotification::class)
    abstract fun bindStorageNotification(sysui: StorageNotification): CoreStartable

    /** Inject into ToastUI.  */
    @Binds
    @IntoMap
    @ClassKey(ToastUI::class)
    abstract fun bindToastUI(service: ToastUI): CoreStartable

    /** Inject into TvNotificationHandler.  */
    @Binds
    @IntoMap
    @ClassKey(TvNotificationHandler::class)
    abstract fun bindTvNotificationHandler(sysui: TvNotificationHandler): CoreStartable

    /** Inject into TvNotificationPanel.  */
    @Binds
    @IntoMap
    @ClassKey(TvNotificationPanel::class)
    abstract fun bindTvNotificationPanel(sysui: TvNotificationPanel): CoreStartable

    /** Inject into TvStatusBar.  */
    @Binds
    @IntoMap
    @ClassKey(TvStatusBar::class)
    abstract fun bindTvStatusBar(sysui: TvStatusBar): CoreStartable

    /** Inject into VpnStatusObserver.  */
    @Binds
    @IntoMap
    @ClassKey(VpnStatusObserver::class)
    abstract fun bindVpnStatusObserver(sysui: VpnStatusObserver): CoreStartable

    /** Inject into Magnification.  */
    @Binds
    @IntoMap
    @ClassKey(Magnification::class)
    abstract fun bindMagnification(sysui: Magnification): CoreStartable

    /** Inject into WMShell.  */
    @Binds
    @IntoMap
    @ClassKey(WMShell::class)
    abstract fun bindWMShell(sysui: WMShell): CoreStartable
}
