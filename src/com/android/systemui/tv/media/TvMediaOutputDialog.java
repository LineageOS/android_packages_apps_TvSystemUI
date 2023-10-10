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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.MediaRouter2;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.widget.LinearLayoutManager;
import com.android.internal.widget.RecyclerView;
import com.android.settingslib.media.MediaDevice;
import com.android.systemui.broadcast.BroadcastSender;
import com.android.systemui.media.dialog.MediaOutputController;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.tv.res.R;

import java.util.Collections;

/**
 * A TV specific variation of the {@link com.android.systemui.media.dialog.MediaOutputDialog}.
 * This dialog allows the user to select a default audio output, which is not based on the
 * currently playing media.
 * There are two entry points for the dialog, either by sending a broadcast via the
 * {@link com.android.systemui.media.dialog.MediaOutputDialogReceiver} or by calling
 * {@link MediaRouter2#showSystemOutputSwitcher()}
 */
public class TvMediaOutputDialog extends SystemUIDialog implements MediaOutputController.Callback {
    private static final String TAG = TvMediaOutputDialog.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final TvMediaOutputController mMediaOutputController;
    private final BroadcastSender mBroadcastSender;

    private View mDialogView;
    private final TvMediaOutputAdapter mAdapter;

    protected final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private boolean mDismissing;

    private String mActiveDeviceId;

    TvMediaOutputDialog(Context context,
            TvMediaOutputController mediaOutputController,
            BroadcastSender broadcastSender) {
        super(context);
        mContext = context;
        mMediaOutputController = mediaOutputController;
        mBroadcastSender = broadcastSender;

        mAdapter = new TvMediaOutputAdapter(mContext, mMediaOutputController, this);

        final BroadcastReceiver closeSystemDialogsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Close this dialog.
                cancel();
            }
        };
        mContext.registerReceiverForAllUsers(closeSystemDialogsBroadcastReceiver,
                new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS),
                null /* permission */, null /* scheduler */, Context.RECEIVER_EXPORTED);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "package name: " + mContext.getPackageName());

        mDialogView = LayoutInflater.from(mContext).inflate(R.layout.media_output_dialog, null);

        Resources res = mContext.getResources();
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
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        window.setBackgroundDrawableResource(R.drawable.media_dialog_background);
        window.setAttributes(lp);
        window.setElevation(getWindow().getElevation() + 5);
        window.setContentView(mDialogView);
        window.setTitle(
                mContext.getString(
                        com.android.systemui.R.string.media_output_dialog_accessibility_title));
        window.setWindowAnimations(R.style.TvMediaOutputDialog);

        window.getDecorView().addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> mDialogView.setUnrestrictedPreferKeepClearRects(
                        Collections.singletonList(new Rect(left, top, right, bottom))));

        RecyclerView devicesRecyclerView = mDialogView.requireViewById(R.id.device_list);
        devicesRecyclerView.setLayoutManager(new LayoutManagerWrapper(mContext));
        devicesRecyclerView.setAdapter(mAdapter);

        int itemSpacingPx = mContext.getResources()
                .getDimensionPixelSize(R.dimen.media_dialog_item_spacing);
        devicesRecyclerView.addItemDecoration(new SpacingDecoration(itemSpacingPx));
    }

    @Override
    public void dismiss() {
        // Turning refresh() into a no-op.
        mDismissing = true;
        super.dismiss();
    }

    @Override
    public void start() {
        mMediaOutputController.start(this);
    }

    @Override
    public void stop() {
        mMediaOutputController.stop();
    }

    private void refresh(boolean deviceSetChanged) {
        if (DEBUG) Log.d(TAG, "refresh: deviceSetChanged " + deviceSetChanged);
        // If the dialog is going away or is already refreshing, do nothing.
        if (mDismissing || mMediaOutputController.isRefreshing()) {
            return;
        }
        mMediaOutputController.setRefreshing(true);
        mAdapter.updateItems();
    }

    @Override
    public void onMediaChanged() {
        // NOOP
    }

    @Override
    public void onMediaStoppedOrPaused() {
        // NOOP
    }

    @Override
    public void onRouteChanged() {
        mMainThreadHandler.post(() -> refresh(/* deviceSetChanged= */ false));
        MediaDevice activeDevice = mMediaOutputController.getCurrentConnectedMediaDevice();
        if (mActiveDeviceId != null && !mActiveDeviceId.equals(activeDevice.getId())) {
            mMediaOutputController.showVolumeDialog();
        }
        mActiveDeviceId = activeDevice.getId();
    }

    @Override
    public void onDeviceListChanged() {
        mMainThreadHandler.post(() -> refresh(/* deviceSetChanged= */ true));
        if (mActiveDeviceId == null
                && mMediaOutputController.getCurrentConnectedMediaDevice() != null) {
            mActiveDeviceId = mMediaOutputController.getCurrentConnectedMediaDevice().getId();
        }
    }

    @Override
    public void dismissDialog() {
        if (DEBUG) Log.d(TAG, "dismissDialog");
        mBroadcastSender.closeSystemDialogs();
    }

    private class LayoutManagerWrapper extends LinearLayoutManager {
        LayoutManagerWrapper(Context context) {
            super(context);
        }

        @Override
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            mMediaOutputController.setRefreshing(false);
            mMediaOutputController.refreshDataSetIfNeeded();
        }
    }

    private static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int mMarginPx;

        SpacingDecoration(int marginPx) {
            mMarginPx = marginPx;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = mMarginPx;
            }
            outRect.bottom = mMarginPx;
        }
    }
}
