/*
Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted (subject to the limitations in the
disclaimer below) provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

     Copyright 2015 The Android Open Source Project
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
package com.android.camera.ui;
import android.widget.AbsSeekBar;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.graphics.Rect;

public class VerticalSeekBar extends AbsSeekBar {

    private Drawable mThumbDraw;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean isPressed);
    }

    private OnSeekBarChangeListener mbarListener;

    public VerticalSeekBar(Context context) {
        this(context, null);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.seekBarStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style,0);
    }
    public VerticalSeekBar(Context context, AttributeSet attrs, int styleAttr, int styleRes) {
                 super(context, attrs, styleAttr, styleRes);
             }
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mbarListener = listener;
    }

    public void freshProgress(float thumbscale) {
        Drawable thumb = mThumbDraw;
        if (thumb != null) {
            setThumbPos(getHeight(), thumb, thumbscale, Integer.MIN_VALUE);
            invalidate();
        }
        if (mbarListener != null) {
            mbarListener.onProgressChanged(this, getProgress(), isPressed());
        }
    }
    private void setThumbPos(int w, Drawable thumb, float thumbscale, int offset) {
        int availableWidth = w - getPaddingLeft() - getPaddingRight();
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();
        availableWidth -= thumbWidth;
        // The extra space for the thumb to move on the track
        availableWidth += getThumbOffset() * 2;
        int left = (int) (thumbscale * availableWidth);
        int right = left + thumbWidth;
        int top, bottom;
        if (offset == Integer.MIN_VALUE) {
            Rect thumbBounds = thumb.getBounds();
            top = thumbBounds.top;
            bottom = thumbBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }
        thumb.setBounds(left, top, right, bottom);
    }
    private void trackTouchEvent(MotionEvent event) {
        float thumbScale = 0;
        float progress = 0;
        final int barheight = getHeight();
        final int availableheight = barheight - getPaddingBottom() - getPaddingTop();
        int cureentY = (int) event.getY();
        if (cureentY > barheight - getPaddingBottom()) {
            thumbScale = 0.0f;
        } else if (cureentY < getPaddingTop()) {
            thumbScale = 1.0f;
        } else {
            thumbScale = (float) (barheight - getPaddingBottom() - cureentY) / (float) availableheight;
        }
        progress = thumbScale * getMax();
        setProgress((int) progress);
        freshProgress(thumbScale);
    }
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.rotate(-90);
        canvas.translate(-getHeight(), 0);
        super.onDraw(canvas);
    }

    @Override
    protected synchronized void onMeasure(int width, int height) {
        super.onMeasure(height, width);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    public void setThumb(Drawable thumb) {
        mThumbDraw = thumb;
        super.setThumb(thumb);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldw, oldh);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                setPressed(true);
                trackTouchEvent(event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                trackTouchEvent(event);
                attemptClaimDrag();
                break;
            }
            case MotionEvent.ACTION_UP: {
                trackTouchEvent(event);
                setPressed(false);
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                setPressed(false);
                invalidate(); // see above explanation
                break;
            }
            default:
                break;
        }
        return true;
    }
}