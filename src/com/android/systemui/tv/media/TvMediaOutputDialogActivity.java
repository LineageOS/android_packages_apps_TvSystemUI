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

package com.android.systemui.tv.media;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.MediaRouter2;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.systemui.tv.media.settings.SliceFragment;
import com.android.systemui.tv.res.R;
import com.android.tv.twopanelsettings.slices.SlicesConstants;

import java.util.Collections;

import javax.inject.Inject;

/**
 * A TV specific variation of the {@link com.android.systemui.media.dialog.MediaOutputDialog}. This
 * activity allows the user to select a default audio output, which is not based on the currently
 * playing media. There are two entry points for the dialog, either by sending a broadcast via the
 * {@link com.android.systemui.media.dialog.MediaOutputDialogReceiver} or by calling {@link
 * MediaRouter2#showSystemOutputSwitcher()}
 */
public class TvMediaOutputDialogActivity extends FragmentActivity {

    private FragmentManager mFragmentManager;
    private final OutputDevicesFragment mOutputDevicesFragment;

    @Inject
    public TvMediaOutputDialogActivity(OutputDevicesFragment outputDevicesFragment) {
        mOutputDevicesFragment = outputDevicesFragment;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_output_dialog);

        Resources res = getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int marginVerticalPx = res.getDimensionPixelSize(R.dimen.media_dialog_margin_vertical);
        int marginEndPx = res.getDimensionPixelSize(R.dimen.media_dialog_margin_end);

        final Window window = getWindow();
        final WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.getAbsoluteGravity(Gravity.TOP | Gravity.END,
                res.getConfiguration().getLayoutDirection());
        lp.width = res.getDimensionPixelSize(R.dimen.media_dialog_width);
        lp.height = screenHeight - 2 * marginVerticalPx;
        lp.horizontalMargin = ((float) marginEndPx) / screenWidth;
        lp.verticalMargin = ((float) marginVerticalPx) / screenHeight;
        window.setBackgroundDrawableResource(R.drawable.media_dialog_background);
        window.setAttributes(lp);
        window.setElevation(getWindow().getElevation() + 5);
        window.setTitle(getString(
                com.android.systemui.res.R.string.media_output_dialog_accessibility_title));

        window.getDecorView().addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> findViewById(android.R.id.content).setUnrestrictedPreferKeepClearRects(
                        Collections.singletonList(new Rect(left, top, right, bottom))));

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.setFragmentResultListener(
                "deviceSettings",
                this,
                (key, bundle) -> {
                    if (key.equals("deviceSettings")) {
                        CharSequence title = bundle.getString("title");
                        CharSequence subtitle = bundle.getCharSequence("subtitle");
                        String uri = bundle.getString("uri");

                        SliceFragment sliceFragment = new SliceFragment();
                        Bundle args = new Bundle();
                        args.putString(SlicesConstants.TAG_TARGET_URI, uri);
                        args.putCharSequence(SlicesConstants.TAG_SCREEN_TITLE, title);
                        args.putCharSequence(SliceFragment.TAG_SCREEN_SUBTITLE, subtitle);
                        sliceFragment.setArguments(args);

                        mFragmentManager
                                .beginTransaction()
                                .replace(R.id.media_output_fragment, sliceFragment)
                                .addToBackStack("device")
                                .commit();
                    }
                });

        showMainFragment();
    }

    private void showMainFragment() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.replace(R.id.media_output_fragment, mOutputDevicesFragment);
        transaction.commit();
    }

}
