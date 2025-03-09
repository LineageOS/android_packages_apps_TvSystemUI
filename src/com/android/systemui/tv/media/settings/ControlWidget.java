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

package com.android.systemui.tv.media.settings;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Objects;

import com.android.systemui.tv.res.R;

/**
 * Base control widget that has default tooltip functionality.
 **/
public class ControlWidget extends FrameLayout
        implements View.OnFocusChangeListener, View.OnKeyListener {
    private static final String TAG = ControlWidget.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int TOOLTIP_DELAY_MS = 2000;
    private TooltipConfig mTooltipConfig;
    private View mTooltipView;
    private PopupWindow mTooltipWindow;
    @Nullable
    private OnKeyListener mExternalOnKeyListener;
    @Nullable
    private OnFocusChangeListener mExternalOnFocusChangeListener;

    private final CountDownTimer mTooltipTimer;

    public ControlWidget(Context context) {
        this(context, /* attrs= */ null);
    }

    public ControlWidget(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, /* defStyleAttr= */ 0);
    }

    @SuppressWarnings("nullness")
    public ControlWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setOnFocusChangeListener(this);
        super.setOnKeyListener(this);
        this.mTooltipTimer = createTooltipTimer(TOOLTIP_DELAY_MS);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.isEnabled() == enabled) {
            return;
        }
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.6f);
    }

    @Override
    public void setOnKeyListener(@Nullable OnKeyListener onKeyListener) {
        this.mExternalOnKeyListener = onKeyListener;
    }

    @Override
    public void setOnFocusChangeListener(@Nullable OnFocusChangeListener onFocusChangeListener) {
        this.mExternalOnFocusChangeListener = onFocusChangeListener;
    }

    public void setTooltipConfig(TooltipConfig tooltipConfig) {
        if (Objects.equals(this.mTooltipConfig, tooltipConfig)) {
            return;
        }

        this.mTooltipConfig = tooltipConfig;

        if (tooltipConfig == null) {
            dismissTooltipWindow();
        } else {
            // reflect tool tip config changes
            if (!tooltipConfig.getShouldShowTooltip()) {
                dismissTooltipWindow();
            }

            if (mTooltipView != null && mTooltipWindow != null && mTooltipWindow.isShowing()) {
                loadText();
                loadSummary();
                loadImage();
            }
        }
    }

    private boolean shouldAbortShowingTooltip() {
        return !isFocused() || mTooltipConfig == null || !mTooltipConfig.getShouldShowTooltip()
                || !isAttachedToWindow();
    }

    private void showTooltipView() {
        if (shouldAbortShowingTooltip()) {
            return;
        }
        int width = getContext().getResources().getDimensionPixelSize(R.dimen.tooltip_window_width);

        // Construct tooltip pop-up window.
        mTooltipView = View.inflate(this.getContext(), R.layout.tooltip_window, null);
        mTooltipView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        if (DEBUG) Log.d(TAG, "onGlobalLayoutListener");
                        mTooltipView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (shouldAbortShowingTooltip()) {
                            return;
                        }
                        // Calculate tooltip location on screen.
                        Rect location = locateView(ControlWidget.this);
                        int[] position = ControlWidget.this.calculateWindowOffset(location);
                        if (DEBUG) {
                            Log.d(TAG,
                                    "new position, x=" + position[0] + ", y=" + position[1]);
                        }

                        // Only update the position, not the size
                        mTooltipWindow.update(position[0], position[1], -1, -1);
                        mTooltipView.postDelayed(() -> {
                            if (shouldAbortShowingTooltip()) {
                                return;
                            }
                            if (DEBUG) Log.d(TAG, "postDelayed, make visible");
                            mTooltipView.setVisibility(VISIBLE);
                        }, 100);
                    }
                });

        mTooltipWindow = new PopupWindow(mTooltipView, width, ViewGroup.LayoutParams.WRAP_CONTENT,
                false);
        mTooltipWindow.setAnimationStyle(R.style.ControlWidgetTooltipWindowAnimation);
        mTooltipView.setVisibility(INVISIBLE);

        // Load image and text.
        loadImage();
        loadSummary();
        loadText();

        // Calculate tooltip location on screen.
        Rect location = locateView(this);
        int[] position = calculateWindowOffset(location);

        // Display tooltip window.
        mTooltipWindow.showAtLocation(this, Gravity.NO_GRAVITY, position[0], position[1]);
    }

    public void dismissTooltipWindow() {
        if (mTooltipWindow != null && mTooltipWindow.isShowing()) {
            mTooltipWindow.dismiss();
        } else {
            mTooltipTimer.cancel();
        }
    }

    private static Rect locateView(View view) {
        int[] locationInt = new int[2];
        view.getLocationOnScreen(locationInt);
        Rect location = new Rect();
        location.left = locationInt[0];
        location.top = locationInt[1];
        location.right = location.left + view.getWidth();
        location.bottom = location.top + view.getHeight();
        return location;
    }

    private int[] calculateWindowOffset(Rect focusedRect) {
        int[] windowOffset = new int[2];
        int tooltipWidth = getContext().getResources().getDimensionPixelSize(
                R.dimen.tooltip_window_width);
        boolean isRtl =
                getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        // X offset
        if (isRtl) {
            // controlWidget.width  + margin
            windowOffset[0] = focusedRect.right
                    + getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.tooltip_window_horizontal_margin);
        } else {
            windowOffset[0] = -tooltipWidth
                    - getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.tooltip_window_horizontal_margin);
        }
        // Y offset
        if (mTooltipView.getMeasuredHeight() <= 0) {
            // Height unknown -> fixed offset
            windowOffset[1] = focusedRect.top
                    - getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.media_dialog_margin_vertical)
                    + getContext().getResources().getDimensionPixelSize(
                    R.dimen.tooltip_window_vertical_margin);
        } else {
            // Height known -> calculate centered position
            windowOffset[1] = (focusedRect.top + focusedRect.bottom) / 2
                    - mTooltipView.getMeasuredHeight() / 2
                    - getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.media_dialog_margin_vertical);
        }

        return windowOffset;
    }

    private void loadText() {
        CharSequence text = mTooltipConfig.getTooltipText();
        TextView textView = mTooltipView.requireViewById(R.id.tooltip_text);

        if (text == null || text.isEmpty()) {
            textView.setVisibility(GONE);
        } else {
            textView.setVisibility(VISIBLE);
            textView.setText(text);
        }
    }

    private void loadSummary() {
        CharSequence summary = mTooltipConfig.getTooltipSummary();
        TextView summaryView = mTooltipView.requireViewById(R.id.tooltip_summary);

        if (summary == null || summary.isEmpty()) {
            summaryView.setVisibility(GONE);
        } else {
            summaryView.setVisibility(VISIBLE);
            summaryView.setText(summary);
        }
    }

    private void loadImage() {
        ImageView tooltipImage = mTooltipView.requireViewById(R.id.tooltip_image);
        Drawable imageDrawable = mTooltipConfig.getImageDrawable();
        if (imageDrawable == null) {
            tooltipImage.setVisibility(GONE);
        } else {
            tooltipImage.setImageDrawable(imageDrawable);
            tooltipImage.setVisibility(VISIBLE);
        }
    }

    private CountDownTimer createTooltipTimer(long delayMs) {
        return new CountDownTimer(delayMs, delayMs) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                showTooltipView();
            }
        };
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (mExternalOnFocusChangeListener != null) {
            mExternalOnFocusChangeListener.onFocusChange(v, hasFocus);
        }
        if (mTooltipConfig == null || !mTooltipConfig.getShouldShowTooltip()) {
            return;
        }
        if (hasFocus) {
            mTooltipTimer.start();
        } else {
            if (mTooltipWindow != null && mTooltipWindow.isShowing()) {
                mTooltipWindow.dismiss();
            } else {
                mTooltipTimer.cancel();
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mTooltipWindow != null && mTooltipWindow.isShowing()) {
                mTooltipWindow.dismiss();
            } else {
                mTooltipTimer.cancel();
            }
            if (mTooltipConfig != null
                    && mTooltipConfig.getShouldShowTooltip()
                    && isFocused()) {
                // Start the timer in case user hover 3 seconds again.
                mTooltipTimer.start();
            }
        }
        if (mExternalOnKeyListener != null) {
            return mExternalOnKeyListener.onKey(v, keyCode, event);
        }
        return false;
    }

    /** Wrapper class for callers to set tool tip related attributes. */
    public static final class TooltipConfig {
        private boolean mShouldShowTooltip;
        private Drawable mImageDrawable;
        private CharSequence mTooltipText;
        private CharSequence mTooltipSummary;

        public void setShouldShowTooltip(boolean shouldShowTooltip) {
            this.mShouldShowTooltip = shouldShowTooltip;
        }

        public boolean getShouldShowTooltip() {
            return mShouldShowTooltip;
        }

        public void setImageDrawable(Drawable imageDrawable) {
            this.mImageDrawable = imageDrawable;
        }

        public Drawable getImageDrawable() {
            return mImageDrawable;
        }

        public void setTooltipText(CharSequence tooltipText) {
            this.mTooltipText = tooltipText;
        }

        public CharSequence getTooltipText() {
            return mTooltipText;
        }

        public void setTooltipSummary(CharSequence tooltipSummary) {
            this.mTooltipSummary = tooltipSummary;
        }

        public CharSequence getTooltipSummary() {
            return mTooltipSummary;
        }
    }
}
