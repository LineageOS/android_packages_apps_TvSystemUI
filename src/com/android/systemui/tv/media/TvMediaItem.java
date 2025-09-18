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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.settingslib.media.MediaDevice;
import com.android.systemui.tv.res.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

/**
 * TvMediaItem represents an item in output switcher list (could be a MediaDevice, group divider or
 * connect new device item).
 */
public class TvMediaItem {

    private final Optional<MediaDevice> mMediaDeviceOptional;
    private final String mTitle;
    @TvMediaItem.MediaItemType
    private final int mMediaItemType;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TvMediaItem.MediaItemType.TYPE_DEVICE,
            TvMediaItem.MediaItemType.TYPE_GROUP_DIVIDER,
            TvMediaItem.MediaItemType.TYPE_PAIR_NEW_DEVICE})
    public @interface MediaItemType {
        int TYPE_DEVICE = 0;
        int TYPE_GROUP_DIVIDER = 1;
        int TYPE_PAIR_NEW_DEVICE = 2;
    }

    private TvMediaItem(@Nullable MediaDevice device, @Nullable String title,
            @TvMediaItem.MediaItemType int type) {
        this.mMediaDeviceOptional = Optional.ofNullable(device);
        this.mTitle = title;
        this.mMediaItemType = type;
    }

    /**
     * Returns a new {@link TvMediaItem.MediaItemType#TYPE_DEVICE} {@link TvMediaItem} with its
     * {@link #getMediaDevice() media device} set to {@code device} and its title set to
     * {@code device}'s name.
     */
    public static TvMediaItem createDeviceMediaItem(@NonNull MediaDevice device) {
        return new TvMediaItem(device, device.getName(), MediaItemType.TYPE_DEVICE);
    }

    /**
     * Returns a new {@link TvMediaItem.MediaItemType#TYPE_PAIR_NEW_DEVICE} {@link TvMediaItem} with
     * both {@link #getMediaDevice() media device} and title set to {@code null}.
     */
    public static TvMediaItem createPairNewDeviceMediaItem() {
        return new TvMediaItem(/* device */ null,
                /* title */ null, TvMediaItem.MediaItemType.TYPE_PAIR_NEW_DEVICE);
    }

    /**
     * Returns a new {@link TvMediaItem.MediaItemType#TYPE_GROUP_DIVIDER} {@link TvMediaItem} with
     * the specified title and a {@code null} {@link #getMediaDevice() media device}.
     */
    public static TvMediaItem createGroupDividerMediaItem(@Nullable String title) {
        return new TvMediaItem(/* device */ null, title,
                TvMediaItem.MediaItemType.TYPE_GROUP_DIVIDER);
    }

    /** Get layout id based on media item Type. */
    public static int getMediaLayoutId(@TvMediaItem.MediaItemType int mediaItemType) {
        return switch (mediaItemType) {
            case TvMediaItem.MediaItemType.TYPE_DEVICE,
                 TvMediaItem.MediaItemType.TYPE_PAIR_NEW_DEVICE ->
                    R.layout.media_output_list_item_advanced;
            default -> R.layout.media_output_list_group_divider;
        };
    }

    public Optional<MediaDevice> getMediaDevice() {
        return mMediaDeviceOptional;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getMediaItemType() {
        return mMediaItemType;
    }

}
