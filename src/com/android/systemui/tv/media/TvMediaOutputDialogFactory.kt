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

package com.android.systemui.tv.media

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.PowerExemptionManager
import android.util.Log
import android.view.View
import com.android.internal.jank.InteractionJankMonitor
import com.android.internal.logging.UiEventLogger
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.media.flags.Flags
import com.android.systemui.animation.DialogCuj
import com.android.systemui.animation.DialogLaunchAnimator
import com.android.systemui.broadcast.BroadcastSender
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.media.dialog.MediaOutputDialogFactory
import com.android.systemui.media.nearby.NearbyMediaDevicesManager
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.notification.collection.notifcollection.CommonNotifCollection
import javax.inject.Inject

/**
 * Factory to create [TvMediaOutputDialog] objects.
 */
class TvMediaOutputDialogFactory @Inject constructor(
        private val context: Context,
        private val mediaSessionManager: MediaSessionManager,
        private val lbm: LocalBluetoothManager?,
        private val starter: ActivityStarter,
        private val broadcastSender: BroadcastSender,
        private val notifCollection: CommonNotifCollection,
        private val uiEventLogger: UiEventLogger,
        private val dialogLaunchAnimator: DialogLaunchAnimator,
        private val nearbyMediaDevicesManager: NearbyMediaDevicesManager,
        private val audioManager: AudioManager,
        private val powerExemptionManager: PowerExemptionManager,
        private val keyGuardManager: KeyguardManager,
        private val featureFlags: FeatureFlags,
        private val userTracker: UserTracker
) : MediaOutputDialogFactory(context, mediaSessionManager, lbm, starter, broadcastSender,
        notifCollection, uiEventLogger, dialogLaunchAnimator, nearbyMediaDevicesManager,
        audioManager, powerExemptionManager, keyGuardManager, featureFlags, userTracker) {
    companion object {
        private const val INTERACTION_JANK_TAG = "media_output"
        private const val TAG = "TvMediaOutputDialogFactory"
    }

    private var mediaOutputDialog: TvMediaOutputDialog? = null

    /** Creates a [TvMediaOutputDialog]. */
    override fun create(packageName: String, aboveStatusBar: Boolean, view: View?) {
        if (!Flags.enableTvMediaOutputDialog()) {
            // Not showing any media output dialog since the mobile version is not navigable on TV.
            Log.w(TAG, "enable_tv_media_output_dialog flag is disabled")
            return
        }
        // Dismiss the previous dialog, if any.
        mediaOutputDialog?.dismiss()

        val controller = TvMediaOutputController(
                context, packageName,
                mediaSessionManager, lbm, starter, notifCollection,
                dialogLaunchAnimator, nearbyMediaDevicesManager, audioManager,
                powerExemptionManager, keyGuardManager, featureFlags, userTracker)
        val dialog = TvMediaOutputDialog(context, controller, broadcastSender)
        mediaOutputDialog = dialog

        // Show the dialog.
        if (view != null) {
            dialogLaunchAnimator.showFromView(
                    dialog, view,
                    cuj = DialogCuj(InteractionJankMonitor.CUJ_SHADE_DIALOG_OPEN,
                            INTERACTION_JANK_TAG)
            )
        } else {
            dialog.show()
        }
    }

    /** Dismiss the [TvMediaOutputDialog] if it exists. */
    override fun dismiss() {
        mediaOutputDialog?.dismiss()
        mediaOutputDialog = null
    }
}