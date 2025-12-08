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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.media.MediaDevice;
import com.android.systemui.tv.res.R;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class OutputDevicesFragment extends Fragment
        implements TvMediaOutputController.Callback, TvMediaOutputAdapter.PanelCallback {

    private static final String TAG = OutputDevicesFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final TvMediaOutputController mMediaOutputController;
    private TvMediaOutputAdapter mAdapter;
    private RecyclerView mDevicesRecyclerView;

    protected final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private String mActiveDeviceId;

    @Inject
    public OutputDevicesFragment(TvMediaOutputController mediaOutputController) {
        mMediaOutputController = mediaOutputController;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new TvMediaOutputAdapter(getContext(), mMediaOutputController, this);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_output_main_fragment, null);

        mDevicesRecyclerView = view.requireViewById(R.id.device_list);
        mDevicesRecyclerView.setLayoutManager(new LayoutManagerWrapper(view.getContext()));
        mDevicesRecyclerView.setAdapter(mAdapter);

        mDevicesRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        Drawable foreground = FadingEdgeUtil.getForegroundDrawable(
                                recyclerView, requireContext());
                        if (foreground != recyclerView.getForeground()) {
                            recyclerView.setForeground(foreground);
                        }
                    }
                });

        int itemSpacingPx = getResources().getDimensionPixelSize(R.dimen.media_dialog_item_spacing);
        mDevicesRecyclerView.addItemDecoration(new SpacingDecoration(itemSpacingPx));

        mDevicesRecyclerView.setAccessibilityDelegate(
                new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            @NonNull View host, @NonNull AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        List<TvMediaItem> mediaItems = mMediaOutputController.getMediaItemList();
                        boolean hasSettings = false;
                        int importantItemsCount = 0;
                        for (TvMediaItem item : mediaItems) {
                            if (item.getMediaItemType()
                                    != TvMediaItem.MediaItemType.TYPE_GROUP_DIVIDER
                                    || (item.getTitle() != null && !item.getTitle().isEmpty())) {
                                // Everything except for the divider lines are important
                                importantItemsCount++;
                            }
                            if (!hasSettings && item.getMediaItemType()
                                    == TvMediaItem.MediaItemType.TYPE_DEVICE) {
                                String baseUri =
                                        TvMediaOutputController.getSettingsBaseUriForDevice(
                                                getContext(), item.getMediaDevice().get());
                                if (baseUri != null && !baseUri.isEmpty()) {
                                    hasSettings = true;
                                }
                            }
                        }

                        info.setCollectionInfo(
                                new AccessibilityNodeInfo.CollectionInfo.Builder()
                                        .setRowCount(mediaItems.size())
                                        .setColumnCount(hasSettings ? 2 : 1)
                                        .setItemCount(mediaItems.size())
                                        .setImportantForAccessibilityItemCount(importantItemsCount)
                                        .build()
                        );
                    }
                });

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

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "resuming OutputDevicesFragment");
        int position = mAdapter.getFocusPosition();
        mDevicesRecyclerView.getLayoutManager().scrollToPosition(position);
        // Ensure layout is complete before requesting focus.
        mDevicesRecyclerView.post(() -> {
            View itemToFocus = mDevicesRecyclerView.getLayoutManager().findViewByPosition(position);
            if (itemToFocus != null) {
                itemToFocus.requestFocus();
            }
        });
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

    @Override
    public void openDeviceSettings(
            String uri, CharSequence title, CharSequence subtitle, String id) {
        FragmentManager fragmentManager = getParentFragmentManager();
        Bundle deviceInfo = new Bundle();
        deviceInfo.putString("uri", uri);
        deviceInfo.putCharSequence("title", title);
        deviceInfo.putCharSequence("subtitle", subtitle);
        deviceInfo.putString("deviceId", id);
        fragmentManager.setFragmentResult("deviceSettings", deviceInfo);
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
