/*
 * Copyright (C) 2006 The Android Open Source Project
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

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.camera.util.Log;

public class RotateTextView extends TextView implements Rotatable {

    public static final String TAG = "SnapCam_RotateTextView";

    private float mReferX;
    private float mReferY;

    private float mReferRadius;

    private Rect mDisplayRegion;

    private float mOffsetX;
    private float mOffsetY;

    public RotateTextView(Context context) {
        super(context);
    }

    public RotateTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RotateTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RotateTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        Log.d(TAG, "setOrientation " + orientation + ", animation " + animation);
        setPivotX(0f);
        setPivotY(0f);
        setRotation(-orientation);
        float offset = getTextSize() * 2;
        float X = 0f;
        float Y = 0f;
        if (orientation == 0) {
            X = mReferX - mReferRadius;
            Y = mReferY - mReferRadius - offset;
            if (Y < mDisplayRegion.top) {
                Y = mReferY - Y - getHeight() + mReferY;
                Y += mOffsetY;
            }
        } else if (orientation == 90) {
            X = mReferX - mReferRadius - offset;
            Y = mReferY + mReferRadius;
            if (X < mDisplayRegion.left) {
                X = mReferX - X - getHeight() + mReferX;
            }
        } else if (orientation == 270) {
            X = mReferX + mReferRadius + offset;
            Y = mReferY - mReferRadius;
            if (X > (mDisplayRegion.left + mDisplayRegion.width())) {
                X = mReferX - (X - mReferX - getHeight());
            }
        } else if (orientation == 180) {
            X = mReferX + mReferRadius;
            Y = mReferY + mReferRadius + offset;
            if (Y > (mDisplayRegion.top + mDisplayRegion.height())) {
                Y = mReferY - (Y - mReferY - getHeight());
                Y -= mOffsetY;
            }
        }
        setX(X);
        setY(Y);
    }

    public void setReferCircle(float x, float y, float radius) {
        mReferX = x;
        mReferY = y;
        mReferRadius = radius;
    }

    public void setDisplayRegion(Rect region) {
        mDisplayRegion = region;
    }

    public void setExtraOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }
}
