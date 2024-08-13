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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/** An ImageSpan for a Drawable that is centered vertically in the line. */
public class CenteredImageSpan extends ImageSpan {

    private final Drawable mDrawable;

    public CenteredImageSpan(Drawable drawable) {
        super(drawable);
        mDrawable = drawable;
    }

    @Override
    public int getSize(
            Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetrics) {
        final Rect rect = mDrawable.getBounds();

        if (fontMetrics != null) {
            Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
            int fontHeight = fmPaint.descent - fmPaint.ascent;
            int drHeight = rect.bottom - rect.top;
            int centerY = fmPaint.ascent + fontHeight / 2;

            fontMetrics.ascent = centerY - drHeight / 2;
            fontMetrics.top = fontMetrics.ascent;
            fontMetrics.bottom = centerY + drHeight / 2;
            fontMetrics.descent = fontMetrics.bottom;
        }
        return rect.right;
    }

    @Override
    public void draw(
            Canvas canvas,
            CharSequence text,
            int start,
            int end,
            float x,
            int top,
            int y,
            int bottom,
            Paint paint) {
        canvas.save();
        final int transY = (bottom - mDrawable.getBounds().bottom) / 2;
        canvas.translate(x, transY);
        mDrawable.draw(canvas);
        canvas.restore();
    }
}
