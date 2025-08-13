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

import static android.media.RoutingChangeInfo.ENTRY_POINT_TV_OUTPUT_SWITCHER;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaRoute2Info;
import android.media.RoutingChangeInfo;
import android.os.PowerExemptionManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.InfoMediaManager;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.tv.res.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

/**
 * Keeps track of output devices and sorts and groups them to be displayed in the
 * OutputDevicesFragment.
 */
public class TvMediaOutputController implements LocalMediaManager.DeviceCallback {

    private static final String TAG = TvMediaOutputController.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String SETTINGS_PACKAGE = "com.android.tv.settings";

    private static final long POWER_ALLOWLIST_DURATION_MS = 20_000;
    private static final String POWER_ALLOWLIST_REASON = "mediaoutput:remote_transfer";

    private final Context mContext;
    private final AudioManager mAudioManager;
    protected final Object mMediaDevicesLock = new Object();
    private final InfoMediaManager mInfoMediaManager;
    private final LocalMediaManager mLocalMediaManager;
    private final PowerExemptionManager mPowerExemptionManager;
    private final String mPackageName;
    private final TvOutputMediaItemListProxy mOutputMediaItemListProxy;
    private final List<MediaDevice> mCachedMediaDevices = new CopyOnWriteArrayList<>();
    private Callback mCallback;
    private boolean mIsRefreshing;
    private boolean mNeedRefresh;

    @Inject
    public TvMediaOutputController(
            @NotNull Context context,
            @Nullable LocalBluetoothManager localBluetoothManager,
            AudioManager audioManager,
            PowerExemptionManager powerExemptionManager) {
        mContext = context;
        mAudioManager = audioManager;
        mPowerExemptionManager = powerExemptionManager;
        mPackageName = mContext.getPackageName();

        mInfoMediaManager =
                InfoMediaManager.createInstance(mContext, mPackageName,
                        /* userHandle= */ null, localBluetoothManager, /* token= */ null);
        mLocalMediaManager = new LocalMediaManager(mContext, localBluetoothManager,
                mInfoMediaManager, mPackageName);
        mOutputMediaItemListProxy = new TvOutputMediaItemListProxy(context);
    }

    static String getBluetoothSettingsSliceUri(Context context) {
        String uri = null;
        Resources res;

        try {
            res = context.getPackageManager().getResourcesForApplication(SETTINGS_PACKAGE);
            int resourceId = res.getIdentifier(
                    SETTINGS_PACKAGE + ":string/connected_devices_slice_uri", null, null);
            if (resourceId != 0) {
                uri = res.getString(resourceId);
            }
        } catch (NameNotFoundException exception) {
            Log.e(TAG, "Could not find TvSettings package: " + exception);
        }
        return uri;
    }

    protected void start(@NotNull Callback cb) {
        synchronized (mMediaDevicesLock) {
            mCachedMediaDevices.clear();
            mOutputMediaItemListProxy.clear();
        }
        mCallback = cb;
        mLocalMediaManager.registerCallback(this);
        mLocalMediaManager.startScan();
    }

    protected void stop() {
        mLocalMediaManager.unregisterCallback(this);
        mLocalMediaManager.stopScan();
        synchronized (mMediaDevicesLock) {
            mCachedMediaDevices.clear();
            mOutputMediaItemListProxy.clear();
        }
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    public void setRefreshing(boolean refreshing) {
        mIsRefreshing = refreshing;
    }

    public void refreshDataSetIfNeeded() {
        if (mNeedRefresh) {
            buildMediaItems(mCachedMediaDevices);
            mCallback.onDeviceListChanged();
            mNeedRefresh = false;
        }
    }

    private void buildMediaItems(List<MediaDevice> devices) {
        synchronized (mMediaDevicesLock) {
            mOutputMediaItemListProxy.updateMediaDevices(devices);
        }
    }

    /**
     * Returns a list of media items to be rendered in the device list.
     */
    public List<TvMediaItem> getMediaItemList() {
        synchronized (mMediaDevicesLock) {
            return mOutputMediaItemListProxy.getOutputMediaItemList();
        }
    }

    public boolean isAnyDeviceTransferring() {
        synchronized (mMediaDevicesLock) {
            for (TvMediaItem mediaItem : mOutputMediaItemListProxy.getOutputMediaItemList()) {
                if (mediaItem.getMediaDevice().isPresent()
                        && mediaItem.getMediaDevice().get().getState()
                        == LocalMediaManager.MediaDeviceState.STATE_CONNECTING) {
                    return true;
                }
            }
        }
        return false;
    }

    public MediaDevice getCurrentConnectedMediaDevice() {
        return mLocalMediaManager.getCurrentConnectedDevice();
    }

    public List<MediaDevice> getSelectedMediaDevice() {
        return mOutputMediaItemListProxy.getOutputMediaItemList().stream()
                .filter(item -> item.getMediaDevice().isPresent())
                .map(item -> item.getMediaDevice().get())
                .filter(MediaDevice::isSelected)
                .toList();
    }

    protected void setTemporaryAllowListExceptionIfNeeded() {
        if (mPowerExemptionManager == null || mPackageName == null) {
            Log.w(TAG, "powerExemptionManager or package name is null");
            return;
        }
        mPowerExemptionManager.addToTemporaryAllowList(mPackageName,
                PowerExemptionManager.REASON_MEDIA_NOTIFICATION_TRANSFER,
                POWER_ALLOWLIST_REASON,
                POWER_ALLOWLIST_DURATION_MS);
    }

    protected void connectDevice(MediaDevice device) {
        mInfoMediaManager.setDeviceState(
                device, LocalMediaManager.MediaDeviceState.STATE_CONNECTING);

        if (DEBUG) {
            Log.d(TAG, "initiate switching from " + getCurrentConnectedMediaDevice()
                    + " to " + device);
        }

        ThreadUtils.postOnBackgroundThread(
                () -> {
                    mLocalMediaManager.connectDevice(
                            device,
                            new RoutingChangeInfo(
                                    ENTRY_POINT_TV_OUTPUT_SWITCHER, /* isSuggested= */ false));
                });
    }

    // Extending DeviceCallback

    void showVolumeDialog() {
        mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        if (!mIsRefreshing) {
            buildMediaItems(devices);
            mCallback.onDeviceListChanged();
        } else {
            synchronized (mMediaDevicesLock) {
                mNeedRefresh = true;
                mCachedMediaDevices.clear();
                mCachedMediaDevices.addAll(devices);
            }
        }
    }

    @Override
    public void onSelectedDeviceStateChanged(
            MediaDevice device, @LocalMediaManager.MediaDeviceState int state) {
        if (DEBUG) Log.d(TAG, "Successfully switched output to " + device.getName());
        mCallback.onRouteChanged();
    }

    @Override
    public void onDeviceAttributesChanged() {
        mCallback.onRouteChanged();
    }

    @Override
    public void onRequestFailed(int reason) {
        if (DEBUG) Log.d(TAG, "Failed to switch output: " + reason);
        mCallback.onRouteChanged();
    }

    static String getSettingsBaseUriForDevice(Context context, MediaDevice device) {
        int resourceId;

        int deviceType = device.getDeviceType();
        int routeType = device.getRouteType();

        if (deviceType == MediaDevice.MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE) {
            switch (routeType) {
                case MediaRoute2Info.TYPE_HDMI ->
                        resourceId = R.string.audio_output_hdmi_slice_uri;
                case MediaRoute2Info.TYPE_HDMI_ARC,
                     MediaRoute2Info.TYPE_HDMI_EARC ->
                        resourceId = R.string.audio_output_hdmi_e_arc_slice_uri;
                case MediaRoute2Info.TYPE_USB_HEADSET,
                     MediaRoute2Info.TYPE_USB_DEVICE,
                     MediaRoute2Info.TYPE_USB_ACCESSORY ->
                        resourceId = R.string.audio_output_usb_slice_uri;
                default -> {
                    return null;
                }
            }
        } else if (deviceType == MediaDevice.MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE) {
            resourceId = switch (routeType) {
                case MediaRoute2Info.TYPE_AUX_LINE ->
                        R.string.audio_output_aux_slice_uri;
                case MediaRoute2Info.TYPE_LINE_ANALOG ->
                        R.string.audio_output_line_analog_slice_uri;
                case MediaRoute2Info.TYPE_LINE_DIGITAL ->
                        R.string.audio_output_line_digital_slice_uri;
                default -> R.string.audio_output_wired_headphone_slice_uri;
            };
        } else {
            switch (deviceType) {
                case MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE ->
                        resourceId = R.string.audio_output_builtin_speaker_slice_uri;
                case MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE ->
                        resourceId = R.string.audio_output_bluetooth_slice_uri;
                case MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE ->
                        resourceId = R.string.audio_output_cast_device_slice_uri;
                case MediaDevice.MediaDeviceType.TYPE_CAST_GROUP_DEVICE ->
                        resourceId = R.string.audio_output_cast_group_slice_uri;
                case MediaDevice.MediaDeviceType.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER ->
                        resourceId = R.string.audio_output_remote_avr_slice_uri;
                default -> {
                    return null;
                }
            }
        }

        return context.getString(resourceId);
    }

    public interface Callback {
        /**
         * Override to handle the device status or attributes updating.
         */
        void onRouteChanged();

        /**
         * Override to handle the devices set updating.
         */
        void onDeviceListChanged();

        /**
         * Override to dismiss dialog.
         */
        void dismissDialog();
    }
}
