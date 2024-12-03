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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaRoute2Info;
import android.net.Uri;
import android.os.Bundle;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.media.BluetoothMediaDevice;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaDevice.MediaDeviceType;
import com.android.systemui.media.dialog.MediaItem;
import com.android.systemui.tv.media.settings.CenteredImageSpan;
import com.android.systemui.tv.media.settings.ControlWidget;

import com.android.systemui.tv.res.R;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adapter for showing the {@link MediaItem}s in the {@link TvMediaOutputDialogActivity}.
 */
public class TvMediaOutputAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = TvMediaOutputAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final TvMediaOutputController mMediaOutputController;
    private final PanelCallback mCallback;
    private final Context mContext;
    protected List<MediaItem> mMediaItemList = new CopyOnWriteArrayList<>();

    private final int mFocusedRadioTint;
    private final int mUnfocusedRadioTint;
    private final int mCheckedRadioTint;

    private final CharSequence mTooltipText;
    private String mSavedDeviceId;

    private final boolean mIsRtl;

    TvMediaOutputAdapter(Context context, TvMediaOutputController mediaOutputController,
            PanelCallback callback) {
        mContext = context;
        mMediaOutputController = mediaOutputController;
        mCallback = callback;

        Resources res = mContext.getResources();
        mFocusedRadioTint = res.getColor(R.color.media_dialog_radio_button_focused);
        mUnfocusedRadioTint = res.getColor(R.color.media_dialog_radio_button_unfocused);
        mCheckedRadioTint = res.getColor(R.color.media_dialog_radio_button_checked);

        mIsRtl = res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        mTooltipText = createTooltipText();

        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mMediaItemList.size()) {
            Log.e(TAG, "Incorrect position for item type: " + position);
            return MediaItem.MediaItemType.TYPE_GROUP_DIVIDER;
        }
        return mMediaItemList.get(position).getMediaItemType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mHolderView = LayoutInflater.from(mContext)
                .inflate(MediaItem.getMediaLayoutId(viewType), parent, false);

        switch (viewType) {
            case MediaItem.MediaItemType.TYPE_GROUP_DIVIDER:
                return new DividerViewHolder(mHolderView);
            case MediaItem.MediaItemType.TYPE_PAIR_NEW_DEVICE:
            case MediaItem.MediaItemType.TYPE_DEVICE:
                return new DeviceViewHolder(mHolderView);
            default:
                Log.e(TAG, "unknown viewType: " + viewType);
                return new DeviceViewHolder(mHolderView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position >= getItemCount()) {
            Log.e(TAG, "Tried to bind at position > list size (" + getItemCount() + ")");
        }

        MediaItem currentMediaItem = mMediaItemList.get(position);
        switch (currentMediaItem.getMediaItemType()) {
            case MediaItem.MediaItemType.TYPE_GROUP_DIVIDER ->
                    ((DividerViewHolder) viewHolder).onBind(currentMediaItem.getTitle());
            case MediaItem.MediaItemType.TYPE_PAIR_NEW_DEVICE ->
                    ((DeviceViewHolder) viewHolder).onBindNewDevice();
            case MediaItem.MediaItemType.TYPE_DEVICE -> ((DeviceViewHolder) viewHolder).onBind(
                    currentMediaItem.getMediaDevice().get(), position);
            default -> Log.d(TAG, "Incorrect position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return mMediaItemList.size();
    }

    /**
     * Returns position of the MediaDevice with the saved device id.
     */
    protected int getFocusPosition() {
        if (DEBUG) Log.d(TAG, "getFocusPosition, deviceId: " + mSavedDeviceId);
        if (mSavedDeviceId == null) {
            return 0;
        }
        for (int i = 0; i < mMediaItemList.size(); i++) {
            MediaItem item = mMediaItemList.get(i);
            if (item.getMediaDevice().isPresent()) {
                if (item.getMediaDevice().get().getId().equals(mSavedDeviceId)) {
                    mSavedDeviceId = null;
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * Replaces the dpad action with an icon.
     */
    private CharSequence createTooltipText() {
        Resources res = mContext.getResources();
        final SpannedString tooltipText = (SpannedString) res.getText(mIsRtl
                ? R.string.audio_device_tooltip_right : R.string.audio_device_tooltip_left);
        final SpannableString spannableString = new SpannableString(tooltipText);
        Arrays.stream(tooltipText.getSpans(0, tooltipText.length(), Annotation.class)).findFirst()
                .ifPresent(annotation -> {
                    final Drawable icon =
                            res.getDrawable(R.drawable.dpad_right, mContext.getTheme());
                    icon.setLayoutDirection(
                            mContext.getResources().getConfiguration().getLayoutDirection());
                    icon.mutate();
                    icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                    spannableString.setSpan(new CenteredImageSpan(icon),
                            tooltipText.getSpanStart(annotation),
                            tooltipText.getSpanEnd(annotation),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                });

        return spannableString;
    }

    @Override
    public long getItemId(int position) {
        MediaItem item = mMediaItemList.get(position);
        if (item.getMediaDevice().isPresent()) {
            return item.getMediaDevice().get().getId().hashCode();
        }
        if (item.getMediaItemType() == MediaItem.MediaItemType.TYPE_GROUP_DIVIDER) {
            if (item.getTitle() == null || item.getTitle().isEmpty()) {
                return MediaItem.MediaItemType.TYPE_GROUP_DIVIDER;
            }
            return item.getTitle().hashCode();
        }
        return item.getMediaItemType();
    }

    public void updateItems() {
        mMediaItemList.clear();
        mMediaItemList.addAll(mMediaOutputController.getMediaItemList());
        if (DEBUG) {
            Log.d(TAG, "updateItems");
            for (MediaItem mediaItem : mMediaItemList) {
                Log.d(TAG, mediaItem.toString());
            }
        }
        notifyDataSetChanged();
    }

    private class DeviceViewHolder extends RecyclerView.ViewHolder {
        final ImageView mIcon;
        final TextView mTitle;
        final TextView mSubtitle;
        final RadioButton mRadioButton;
        MediaDevice mMediaDevice;

        DeviceViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.requireViewById(R.id.media_output_item_icon);
            mTitle = itemView.requireViewById(R.id.media_dialog_item_title);
            mSubtitle = itemView.requireViewById(R.id.media_dialog_item_subtitle);
            mRadioButton = itemView.requireViewById(R.id.media_dialog_radio_button);
            itemView.setOnKeyListener(
                    (v, keyCode, event) -> {
                        if (event.getAction() != KeyEvent.ACTION_UP) {
                            return false;
                        }
                        int dpadArrow = mIsRtl ?
                                KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                        if (mMediaDevice != null
                                && (keyCode == dpadArrow
                                || (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                                && event.isLongPress()))) {

                            String baseUri = getBaseUriForDevice(mContext, mMediaDevice);
                            if (baseUri == null || baseUri.isEmpty()) {
                                return false;
                            }
                            Uri uri = Uri.parse(baseUri);
                            if (mMediaDevice.getDeviceType()
                                    == MediaDeviceType.TYPE_BLUETOOTH_DEVICE) {
                                uri =
                                        Uri.withAppendedPath(
                                                uri,
                                                ((BluetoothMediaDevice) mMediaDevice)
                                                        .getCachedDevice()
                                                        .getAddress());
                            }

                            mSavedDeviceId = mMediaDevice.getId();
                            mCallback.openDeviceSettings(
                                    uri.toString(),
                                    mTitle.getText(),
                                    getSummary(mMediaDevice, /* focused= */ false),
                                    mMediaDevice.getId());

                            return true;
                        }
                        return false;
                    });
        }

        void onBind(MediaDevice mediaDevice, int position) {
            mMediaDevice = mediaDevice;
            // Title
            mTitle.setText(mediaDevice.getName());

            // Subtitle
            setSummary(mediaDevice);

            // Icon
            Drawable icon;
            if (mediaDevice.getState()
                    == LocalMediaManager.MediaDeviceState.STATE_CONNECTING_FAILED) {
                icon =
                        mContext.getDrawable(
                                com.android.systemui.res.R.drawable.media_output_status_failed);
            } else {
                icon = mediaDevice.getIconWithoutBackground();
            }
            if (icon == null) {
                if (DEBUG) Log.d(TAG, "Using default icon for " + mediaDevice);
                icon = mContext.getDrawable(
                        com.android.settingslib.R.drawable.ic_media_speaker_device);
            }
            mIcon.setImageDrawable(icon);

            mRadioButton.setVisibility(mediaDevice.isConnected() ? View.VISIBLE : View.GONE);
            mRadioButton.setChecked(isCurrentlyConnected(mediaDevice));
            setRadioButtonColor();

            itemView.setOnFocusChangeListener((view, focused) -> {
                setSummary(mediaDevice);
                setRadioButtonColor();
                mTitle.setSelected(focused);
                mSubtitle.setSelected(focused);
            });

            itemView.setOnClickListener(v -> transferOutput(mediaDevice));

            OutputDeviceControlWidget widget = (OutputDeviceControlWidget) itemView;
            String baseUri = getBaseUriForDevice(mContext, mMediaDevice);
            if (baseUri != null && !baseUri.isEmpty()) {
                ControlWidget.TooltipConfig toolTipConfig = new ControlWidget.TooltipConfig();
                toolTipConfig.setShouldShowTooltip(true);
                toolTipConfig.setTooltipText(mTooltipText);
                widget.setTooltipConfig(toolTipConfig);
            }
        }

        private void setRadioButtonColor() {
            if (itemView.hasFocus()) {
                mRadioButton.getButtonDrawable().setTint(
                        mRadioButton.isChecked() ? mCheckedRadioTint : mFocusedRadioTint);
            } else {
                mRadioButton.getButtonDrawable().setTint(mUnfocusedRadioTint);
            }
        }

        private void setSummary(MediaDevice mediaDevice) {
            CharSequence summary = getSummary(mediaDevice, itemView.hasFocus());
            if (mediaDevice.getDeviceType() == MediaDeviceType.TYPE_PHONE_DEVICE
                    && mContext.getResources().getBoolean(
                    com.android.systemui.tv.res.R.bool.
                            config_audioOutputInternalSpeakerGroupedWithSpdif)) {
                mSubtitle.setText(mContext.getResources().getString(
                        R.string.media_output_internal_speaker_spdif_subtitle));
            } else {
                mSubtitle.setText(summary);
            }
            mSubtitle.setVisibility(summary == null || summary.isEmpty()
                    ? View.GONE : View.VISIBLE);
        }

        private CharSequence getSummary(MediaDevice mediaDevice, boolean focused) {
            if (mediaDevice.getState()
                    == LocalMediaManager.MediaDeviceState.STATE_CONNECTING_FAILED) {
                return mContext.getString(
                        com.android.systemui.res.R.string.media_output_dialog_connect_failed);
            } else {
                return mediaDevice.getSummaryForTv(focused
                        ? R.color.media_dialog_low_battery_focused
                        : R.color.media_dialog_low_battery_unfocused);
            }
        }

        private void transferOutput(MediaDevice mediaDevice) {
            if (mMediaOutputController.isAnyDeviceTransferring()) {
                // Don't interrupt ongoing transfer
                return;
            }
            if (isCurrentlyConnected(mediaDevice)) {
                if (DEBUG) Log.d(TAG, "Device is already selected as the active output");
                return;
            }
            mMediaOutputController.setTemporaryAllowListExceptionIfNeeded(mediaDevice);
            mMediaOutputController.connectDevice(mediaDevice);
            mediaDevice.setState(LocalMediaManager.MediaDeviceState.STATE_CONNECTING);
            notifyDataSetChanged();
        }

        /**
         * The single currentConnected device or the only selected device
         */
        boolean isCurrentlyConnected(MediaDevice device) {
            return TextUtils.equals(device.getId(),
                    mMediaOutputController.getCurrentConnectedMediaDevice().getId())
                    || (mMediaOutputController.getSelectedMediaDevice().size() == 1
                    && isDeviceIncluded(mMediaOutputController.getSelectedMediaDevice(), device));
        }

        void onBindNewDevice() {
            mIcon.setImageResource(com.android.systemui.res.R.drawable.ic_add);
            mTitle.setText(R.string.media_output_dialog_pairing_new);
            mSubtitle.setVisibility(View.GONE);
            mRadioButton.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> launchBluetoothSettings());
        }

        private void launchBluetoothSettings() {
            mCallback.dismissDialog();

            Intent bluetoothIntent = new Intent("android.settings.SLICE_SETTINGS");
            Bundle extra = new Bundle();
            extra.putString("slice_uri",
                    "content://com.google.android.tv.btservices.settings.sliceprovider/general");
            bluetoothIntent.putExtras(extra);
            bluetoothIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(bluetoothIntent);
        }

        private boolean isDeviceIncluded(List<MediaDevice> deviceList, MediaDevice targetDevice) {
            for (MediaDevice device : deviceList) {
                if (TextUtils.equals(device.getId(), targetDevice.getId())) {
                    return true;
                }
            }
            return false;
        }

        static String getBaseUriForDevice(Context context, MediaDevice device) {
            int resourceId;

            int deviceType = device.getDeviceType();

            if (deviceType == MediaDeviceType.TYPE_USB_C_AUDIO_DEVICE) {
                int routeType = device.getRouteType();
                switch (routeType) {
                    case MediaRoute2Info.TYPE_HDMI:
                        resourceId = R.string.audio_output_hdmi_slice_uri;
                        break;
                    case MediaRoute2Info.TYPE_HDMI_ARC:
                    case MediaRoute2Info.TYPE_HDMI_EARC:
                        resourceId = R.string.audio_output_hdmi_e_arc_slice_uri;
                        break;
                    case MediaRoute2Info.TYPE_USB_HEADSET:
                    case MediaRoute2Info.TYPE_USB_DEVICE:
                    case MediaRoute2Info.TYPE_USB_ACCESSORY:
                        resourceId = R.string.audio_output_usb_slice_uri;
                        break;
                    default:
                        return null;
                }
            } else {
                switch (deviceType) {
                    case MediaDeviceType.TYPE_PHONE_DEVICE:
                        resourceId = R.string.audio_output_builtin_speaker_slice_uri;
                        break;
                    case MediaDeviceType.TYPE_BLUETOOTH_DEVICE:
                        resourceId = R.string.audio_output_bluetooth_slice_uri;
                        break;
                    case MediaDeviceType.TYPE_3POINT5_MM_AUDIO_DEVICE:
                        resourceId = R.string.audio_output_wired_headphone_slice_uri;
                        break;
                    case MediaDeviceType.TYPE_CAST_DEVICE:
                        resourceId = R.string.audio_output_cast_device_slice_uri;
                        break;
                    case MediaDeviceType.TYPE_CAST_GROUP_DEVICE:
                        resourceId = R.string.audio_output_cast_group_slice_uri;
                        break;
                    case MediaDeviceType.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER:
                        resourceId = R.string.audio_output_remote_avr_slice_uri;
                        break;
                    default:
                        return null;
                }
            }

            return context.getString(resourceId);
        }
    }

    private static class DividerViewHolder extends RecyclerView.ViewHolder {
        final TextView mHeaderText;
        final View mDividerLine;

        DividerViewHolder(@NonNull View itemView) {
            super(itemView);
            mHeaderText = itemView.requireViewById(R.id.media_output_group_header);
            mDividerLine = itemView.requireViewById(R.id.media_output_divider_line);
        }

        void onBind(String groupDividerTitle) {
            boolean hasText = groupDividerTitle != null && !groupDividerTitle.isEmpty();
            mHeaderText.setVisibility(hasText ? View.VISIBLE : View.GONE);
            mDividerLine.setVisibility(hasText ? View.GONE : View.VISIBLE);
            if (hasText) {
                mHeaderText.setText(groupDividerTitle);
            }
        }

    }

    interface PanelCallback {
        void openDeviceSettings(String uri, CharSequence title, CharSequence subtitle, String id);

        void dismissDialog();
    }
}
