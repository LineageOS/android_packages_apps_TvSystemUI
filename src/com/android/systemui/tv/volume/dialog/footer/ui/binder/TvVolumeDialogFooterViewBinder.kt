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

package com.android.systemui.tv.volume.dialog.footer.ui.binder

import android.view.View
import android.widget.ImageView
import com.android.app.tracing.coroutines.launchInTraced
import com.android.systemui.res.R
import com.android.systemui.tv.volume.dialog.footer.ui.viewmodel.TvVolumeDialogFooterViewModel
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialogScope
import com.android.systemui.volume.dialog.ui.binder.ViewBinder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.onEach

@VolumeDialogScope
class TvVolumeDialogFooterViewBinder @Inject constructor(
    private val viewModel: TvVolumeDialogFooterViewModel
) :
    ViewBinder {

    override fun CoroutineScope.bind(view: View) {
        val volumeRowIcon = view.requireViewById<ImageView>(R.id.volume_row_icon)
        viewModel.icon.onEach {
            volumeRowIcon.setImageDrawable(it)
        }.launchInTraced("TVDFVB#icon", this)
    }
}
