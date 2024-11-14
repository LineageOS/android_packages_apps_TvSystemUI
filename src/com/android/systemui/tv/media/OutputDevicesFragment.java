/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerExemptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.internal.widget.LinearLayoutManager;
import com.android.internal.widget.RecyclerView;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.MediaDevice;
import com.android.systemui.animation.DialogTransitionAnimator;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.media.dialog.MediaSwitchingController;
import com.android.systemui.media.nearby.NearbyMediaDevicesManager;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.notification.collection.notifcollection.CommonNotifCollection;
import com.android.systemui.tv.res.R;
import com.android.systemui.volume.panel.domain.interactor.VolumePanelGlobalStateInteractor;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class OutputDevicesFragment extends Fragment
        implements MediaSwitchingController.Callback, TvMediaOutputAdapter.PanelCallback {

    private static final String TAG = OutputDevicesFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private TvMediaOutputController mMediaOutputController;
    private TvMediaOutputAdapter mAdapter;

    private final MediaSessionManager mMediaSessionManager;
    private final LocalBluetoothManager mLocalBluetoothManager;
    private final ActivityStarter mActivityStarter;
    private final CommonNotifCollection mCommonNotifCollection;
    private final DialogTransitionAnimator mDialogTransitionAnimator;
    private final NearbyMediaDevicesManager mNearbyMediaDevicesManager;
    private final AudioManager mAudioManager;
    private final PowerExemptionManager mPowerExemptionManager;
    private final KeyguardManager mKeyguardManager;
    private final FeatureFlags mFeatureFlags;
    private final VolumePanelGlobalStateInteractor mVolumePanelGlobalStateInteractor;
    private final UserTracker mUserTracker;

    protected final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private String mActiveDeviceId;

    @Inject
    public OutputDevicesFragment(
            MediaSessionManager mediaSessionManager,
            @Nullable LocalBluetoothManager localBluetoothManager,
            ActivityStarter activityStarter,
            CommonNotifCollection commonNotifCollection,
            DialogTransitionAnimator dialogTransitionAnimator,
            NearbyMediaDevicesManager nearbyMediaDevicesManager,
            AudioManager audioManager,
            PowerExemptionManager powerExemptionManager,
            KeyguardManager keyguardManager,
            FeatureFlags featureFlags,
            VolumePanelGlobalStateInteractor volumePanelGlobalStateInteractor,
            UserTracker userTracker) {
        mMediaSessionManager = mediaSessionManager;
        mLocalBluetoothManager = localBluetoothManager;
        mActivityStarter = activityStarter;
        mCommonNotifCollection = commonNotifCollection;
        mDialogTransitionAnimator = dialogTransitionAnimator;
        mNearbyMediaDevicesManager = nearbyMediaDevicesManager;
        mAudioManager = audioManager;
        mPowerExemptionManager = powerExemptionManager;
        mKeyguardManager = keyguardManager;
        mFeatureFlags = featureFlags;
        mVolumePanelGlobalStateInteractor = volumePanelGlobalStateInteractor;
        mUserTracker = userTracker;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaOutputController =
                new TvMediaOutputController(
                        getContext(),
                        getContext().getPackageName(),
                        mMediaSessionManager,
                        mLocalBluetoothManager,
                        mActivityStarter,
                        mCommonNotifCollection,
                        mDialogTransitionAnimator,
                        mNearbyMediaDevicesManager,
                        mAudioManager,
                        mPowerExemptionManager,
                        mKeyguardManager,
                        mFeatureFlags,
                        mVolumePanelGlobalStateInteractor,
                        mUserTracker);
        mAdapter = new TvMediaOutputAdapter(getContext(), mMediaOutputController, this);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_output_main_fragment, null);

        RecyclerView devicesRecyclerView = view.findViewById(R.id.device_list);
        devicesRecyclerView.setLayoutManager(new LayoutManagerWrapper(view.getContext()));
        devicesRecyclerView.setAdapter(mAdapter);

        int itemSpacingPx = getResources().getDimensionPixelSize(R.dimen.media_dialog_item_spacing);
        devicesRecyclerView.addItemDecoration(new SpacingDecoration(itemSpacingPx));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaOutputController.start(this);
    }

    @Override
    public void onStop() {
        mMediaOutputController.stop();
        super.onStop();
    }

    private void refresh(boolean deviceSetChanged) {
        if (DEBUG) Log.d(TAG, "refresh: deviceSetChanged " + deviceSetChanged);
        // If the dialog is going away or is already refreshing, do nothing.
        if (mMediaOutputController.isRefreshing()) {
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
        if (getActivity() != null) {
            getActivity().finish();
        }
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
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = mMarginPx;
            }
            outRect.bottom = mMarginPx;
        }
    }
}
