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

package com.android.systemui.tv.media;

import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_CAST_GROUP_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_FAST_PAIR_BLUETOOTH_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE;
import static com.android.settingslib.media.MediaDevice.MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.media.MediaDevice;
import com.android.systemui.tv.res.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

public class TvOutputMediaItemListProxy {
    private static final String TAG = "TvOutputMediaItemListProxy";
    private final Context mContext;

    private final List<TvMediaItem> mCurrentMediaItems;
    private final List<TvMediaItem> mOldMediaItems;

    private boolean mHideInternalSpeaker;

    @Inject
    public TvOutputMediaItemListProxy(Context context) {
        mContext = context;
        mCurrentMediaItems = new CopyOnWriteArrayList<>();
        mOldMediaItems = new CopyOnWriteArrayList<>();
        mHideInternalSpeaker =
                mContext.getResources()
                        .getBoolean(R.bool.config_audioOutputHideInternalSpeakerFromUI);
    }

    /**
     * Assigns lower priorities to devices that should be shown higher up in the list.
     */
    static int getDevicePriorityGroup(MediaDevice mediaDevice) {
        int mediaDeviceType = mediaDevice.getDeviceType();
        return switch (mediaDeviceType) {
            case TYPE_PHONE_DEVICE -> 1;
            case TYPE_USB_C_AUDIO_DEVICE -> 2;
            case TYPE_3POINT5_MM_AUDIO_DEVICE -> 3;
            case TYPE_CAST_DEVICE, TYPE_CAST_GROUP_DEVICE, TYPE_BLUETOOTH_DEVICE,
                 TYPE_FAST_PAIR_BLUETOOTH_DEVICE -> 5;
            default -> 4;
        };
    }

    public List<TvMediaItem> getOutputMediaItemList() {
        return mCurrentMediaItems;
    }

    public void updateMediaDevices(List<MediaDevice> devices) {
        if (mCurrentMediaItems.isEmpty()) {
            mCurrentMediaItems.addAll(buildInitialList(devices));
            return;
        }

        mOldMediaItems.clear();
        mOldMediaItems.addAll(mCurrentMediaItems);
        mCurrentMediaItems.clear();
        mCurrentMediaItems.addAll(buildBetterSubsequentList(mOldMediaItems, devices));
    }

    private List<TvMediaItem> buildInitialList(List<MediaDevice> devices) {
        sortMediaDevices(devices);

        List<TvMediaItem> finalMediaItems = new ArrayList<>();
        boolean disconnectedDevicesAdded = false;
        for (MediaDevice device : devices) {
            if (!shouldShowItem(device)) {
                continue;
            }
            // Add divider before first disconnected device
            if (!device.isConnected() && !disconnectedDevicesAdded) {
                addOtherDevicesDivider(finalMediaItems);
                disconnectedDevicesAdded = true;
            }
            finalMediaItems.add(TvMediaItem.createDeviceMediaItem(device));
        }
        addConnectAnotherDeviceItem(finalMediaItems);
        return finalMediaItems;
    }

    /**
     * Keep devices that have not changed their connection state in the same order. If there is a
     * new connected device, put it at the bottom of the connected devices list and if there is a
     * newly disconnected device, add it at the top of the disconnected devices.
     */
    private List<TvMediaItem> buildBetterSubsequentList(
            List<TvMediaItem> previousMediaItems, List<MediaDevice> devices) {

        final List<TvMediaItem> targetMediaItems = new ArrayList<>();
        // Only use the actual devices, not the dividers etc.
        List<TvMediaItem> oldMediaItems =
                previousMediaItems.stream()
                        .filter(mediaItem -> mediaItem.getMediaDevice().isPresent())
                        .toList();
        addItemsBasedOnConnection(
                targetMediaItems, oldMediaItems, devices, /* isConnected= */ true);
        addItemsBasedOnConnection(
                targetMediaItems, oldMediaItems, devices, /* isConnected= */ false);

        addConnectAnotherDeviceItem(targetMediaItems);
        return targetMediaItems;
    }

    private void addItemsBasedOnConnection(
            List<TvMediaItem> targetMediaItems,
            List<TvMediaItem> oldMediaItems,
            List<MediaDevice> devices,
            boolean isConnected) {

        List<MediaDevice> matchingMediaDevices = new ArrayList<>();
        for (TvMediaItem originalMediaItem : oldMediaItems) {
            // Only go through the device items
            MediaDevice oldDevice = originalMediaItem.getMediaDevice().get();

            for (MediaDevice newDevice : devices) {
                if (TextUtils.equals(oldDevice.getId(), newDevice.getId())
                        && oldDevice.isConnected() == isConnected
                        && newDevice.isConnected() == isConnected) {
                    matchingMediaDevices.add(newDevice);
                    break;
                }
            }
        }
        devices.removeAll(matchingMediaDevices);

        List<MediaDevice> newMediaDevices = new ArrayList<>();
        for (MediaDevice remainingDevice : devices) {
            if (remainingDevice.isConnected() == isConnected) {
                newMediaDevices.add(remainingDevice);
            }
        }
        devices.removeAll(newMediaDevices);

        // Add new connected devices at the end, add new disconnected devices at the start
        if (isConnected) {
            targetMediaItems.addAll(
                    matchingMediaDevices.stream()
                            .filter(this::shouldShowItem)
                            .map(TvMediaItem::createDeviceMediaItem)
                            .toList());
            targetMediaItems.addAll(
                    newMediaDevices.stream()
                            .filter(this::shouldShowItem)
                            .map(TvMediaItem::createDeviceMediaItem)
                            .toList());
        } else {
            if (!matchingMediaDevices.isEmpty() || !newMediaDevices.isEmpty()) {
                addOtherDevicesDivider(targetMediaItems);
            }
            targetMediaItems.addAll(
                    newMediaDevices.stream()
                            .filter(this::shouldShowItem)
                            .map(TvMediaItem::createDeviceMediaItem)
                            .toList());
            targetMediaItems.addAll(
                    matchingMediaDevices.stream()
                            .filter(this::shouldShowItem)
                            .map(TvMediaItem::createDeviceMediaItem)
                            .toList());
        }
    }

    private boolean shouldShowItem(MediaDevice mediaDevice) {
        return !(mHideInternalSpeaker && mediaDevice.getDeviceType() == TYPE_PHONE_DEVICE);
    }

    private void addOtherDevicesDivider(List<TvMediaItem> mediaItems) {
        mediaItems.add(
                TvMediaItem.createGroupDividerMediaItem(
                        mContext.getString(R.string.media_output_dialog_other_devices)));
    }

    private void addConnectAnotherDeviceItem(List<TvMediaItem> mediaItems) {
        if (TvMediaOutputController.getBluetoothSettingsSliceUri(mContext) == null) {
            Log.d(TAG, "No bluetooth slice set.");
            return;
        }
        mediaItems.add(TvMediaItem.createGroupDividerMediaItem(/* title */ null));
        mediaItems.add(TvMediaItem.createPairNewDeviceMediaItem());
    }

    private void sortMediaDevices(List<MediaDevice> mediaDevices) {
        mediaDevices.sort((device1, device2) -> {
            int priority1 = getDevicePriorityGroup(device1);
            int priority2 = getDevicePriorityGroup(device2);

            if (priority1 != priority2) {
                return (priority1 < priority2) ? -1 : 1;
            }
            // Show connected before disconnected devices
            if (device1.isConnected() != device2.isConnected()) {
                return device1.isConnected() ? -1 : 1;
            }
            return device1.getName().compareToIgnoreCase(device2.getName());
        });
    }

    public void clear() {
        mCurrentMediaItems.clear();
    }

    public boolean isEmpty() {
        return mCurrentMediaItems.isEmpty();
    }
}
