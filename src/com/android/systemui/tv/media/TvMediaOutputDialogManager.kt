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

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.UserHandle
import android.util.Log
import android.view.View
import com.android.internal.logging.UiEventLogger
import com.android.settingslib.media.flags.Flags
import com.android.systemui.animation.DialogTransitionAnimator
import com.android.systemui.broadcast.BroadcastSender
import com.android.systemui.media.dialog.MediaOutputController
import com.android.systemui.media.dialog.MediaOutputDialogManager
import javax.inject.Inject

/**
 * Manager to create and show [TvMediaOutputDialogActivity].
 */
class TvMediaOutputDialogManager @Inject constructor(
        private val context: Context,
        broadcastSender: BroadcastSender,
        uiEventLogger: UiEventLogger,
        dialogTransitionAnimator: DialogTransitionAnimator,
        mediaOutputControllerFactory: MediaOutputController.Factory,
) : MediaOutputDialogManager(
        context,
        broadcastSender,
        uiEventLogger,
        dialogTransitionAnimator,
        mediaOutputControllerFactory
) {
    companion object {
        private const val TAG = "TvMediaOutputDialogFactory"
    }

    /**
     * Creates a [TvMediaOutputDialog].
     *
     * <p>Note that neither the package name nor the user handle are used. The reason is that the TV
     * output dialog does not control a specific app, but rather the system's media output.
     */
    override fun createAndShow(
        unusedPackageName: String,
        aboveStatusBar: Boolean,
        view: View?,
        unusedUserHandle: UserHandle?,
        token: MediaSession.Token?,
    ) {
        if (!Flags.enableTvMediaOutputDialog()) {
            // Not showing any media output dialog since the mobile version is not navigable on TV.
            Log.w(TAG, "enable_tv_media_output_dialog flag is disabled")
            return
        }

        val intent = Intent(context, TvMediaOutputDialogActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Dismiss the [TvMediaOutputDialog] if it exists. */
    override fun dismiss() {
        // NOOP
    }
}
