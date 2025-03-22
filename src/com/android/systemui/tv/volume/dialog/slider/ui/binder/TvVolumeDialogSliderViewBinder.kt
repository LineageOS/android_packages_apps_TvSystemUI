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

package com.android.systemui.tv.volume.dialog.slider.ui.binder

import android.view.View
import android.widget.SeekBar
import com.android.app.tracing.coroutines.launchInTraced
import com.android.systemui.res.R
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialogScope
import com.android.systemui.volume.dialog.sliders.ui.viewmodel.VolumeDialogSliderViewModel
import com.android.systemui.volume.dialog.sliders.ui.viewmodel.VolumeDialogSlidersViewModel
import com.android.systemui.volume.dialog.ui.binder.ViewBinder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.onEach

@VolumeDialogScope
class TvVolumeDialogSliderViewBinder
@Inject
constructor(private val slidersViewModel: VolumeDialogSlidersViewModel) : ViewBinder {

    override fun CoroutineScope.bind(view: View) {
        val seekBar: SeekBar = view.requireViewById(R.id.volume_dialog_slider)
        slidersViewModel.sliders
            .onEach { bindSlider(seekBar, it.sliderComponent.sliderViewModel()) }
            .launchInTraced("TVDSVB#sliders", this)
    }

    private suspend fun bindSlider(seekBar: SeekBar, viewModel: VolumeDialogSliderViewModel) =
        coroutineScope {
            seekBar.setOnSeekBarChangeListener(viewModel.createSeekBarListener())

            viewModel.state
                .onEach { state ->
                    with(seekBar) {
                        // disable value animation due to performance limitations of lower end TVs
                        setProgress(state.value.toInt(), false)
                        max = state.valueRange.endInclusive.toInt()
                        min = state.valueRange.start.toInt()
                    }
                }
                .launchInTraced("TVDSVB#state", this)
        }

    private fun VolumeDialogSliderViewModel.createSeekBarListener() =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                setStreamVolume(value.toFloat(), fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onSliderDragStarted()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSliderDragFinished()
                onSliderChangeFinished(seekBar.progress.toFloat())
            }
        }
}
