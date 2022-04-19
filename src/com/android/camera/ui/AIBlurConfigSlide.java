/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the
 * disclaimer below) provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *
 *     * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
 * GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 */

package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.camera.SettingsManager;

import java.util.ArrayList;

public class AIBlurConfigSlide extends View {
    public static final int NO_MODE = -1;
    public static final int BLUR_SHAPR_MODE = 0;
    public static final int BLUR_STRENGTH_MODE = 1;
    public static final int BLUR_FOCUS_DISTANCE_MODE = 2;
    public static final int BLUR_LUMA_MODE = 3;
    public static final int BLUR_CHROMEU_MODE = 4;
    public static final int BLUR_CHROMAV_MODE = 5;

    private static final int DRAG_Y_THRESHOLD = 100;
    private static final int DRAG_X_THRESHOLD = 30;
    private static final int BLUE = 0xff4693fb;
    private static final int SELECTED_DOT_SIZE = 20;
    private static final int DOT_SIZE = 10;
    private PathMeasure mCurveMeasure;
    private int mCurveLeft;
    private int mCurveRight;
    private float mSlider = -1;
    private Paint mPaint = new Paint();
    private int mNums;
    private int mIndex;
    private Point[] mPoints;
    private float mClickThreshold;
    private int mStride;
    private SettingsManager mSettingsManager;
    private int mMode = NO_MODE;
    private Context mContext;
    private ViewGroup mParent;
    private OneUICameraControls mUI;
    private int mWidth;
    private int mHeight;
    private int mCurveY;
    private ArrayList<View> mAddedViews;
    private float curveCoordinate[] = new float[2];
    private Path mCurvePath = new Path();
    private int mCurveHeight;
    private int mOrientation;

    public AIBlurConfigSlide(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPaint.setStrokeWidth(7f);
        mSettingsManager = SettingsManager.getInstance();
    }

    private void init() {
        init(BLUR_SHAPR_MODE);
        mUI.updateBlurModeText(BLUR_STRENGTH_MODE, "Strength");
        mUI.updateBlurModeText(BLUR_FOCUS_DISTANCE_MODE, "Distance");
        mUI.updateBlurModeText(BLUR_LUMA_MODE, "Luma");
        mUI.updateBlurModeText(BLUR_CHROMEU_MODE, "ChromaU");
        mUI.updateBlurModeText(BLUR_CHROMAV_MODE, "ChromaV");
    }

    private void init(int mode) {
        String key = getKey(mode);
        if (key == null) return;
        int index = mSettingsManager.getValueIndex(key);
        CharSequence[] cc = mSettingsManager.getEntries(key);
        mUI.updateBlurModeText(mode, cc[index].toString());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mMode != NO_MODE) {
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(mCurvePath, mPaint);
        }
        mPaint.setStyle(Paint.Style.FILL);
        if (mMode == BLUR_SHAPR_MODE) {
            for (int i = 0; i < mNums; i++) {
                if (i == mIndex) {
                    mPaint.setColor(BLUE);
                    canvas.drawCircle(mPoints[i].x, mPoints[i].y, SELECTED_DOT_SIZE, mPaint);
                } else {
                    mPaint.setColor(Color.WHITE);
                    canvas.drawCircle(mPoints[i].x, mPoints[i].y, DOT_SIZE, mPaint);
                }
            }
        } else {
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(mCurveLeft, mCurveY, DOT_SIZE, mPaint);
            canvas.drawCircle(mCurveRight, mCurveY, DOT_SIZE, mPaint);
            mPaint.setColor(BLUE);
            if (mSlider >= 0f) {
                mCurveMeasure.getPosTan(mCurveMeasure.getLength() * mSlider, curveCoordinate, null);
                canvas.drawCircle(curveCoordinate[0], curveCoordinate[1], SELECTED_DOT_SIZE, mPaint);
            }
        }
    }

    public void initialize(OneUICameraControls ui) {
        mParent = (ViewGroup) getParent();
        mUI = ui;
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;

        mCurveLeft = mWidth / 10;
        mCurveRight = mWidth - mCurveLeft;
        mCurveHeight = mWidth / 7;
        mCurveY = (int) (mHeight * 0.67);

        float cx = (mCurveLeft + mCurveRight) / 2;
        mCurvePath.reset();
        mCurvePath.moveTo(mCurveLeft, mCurveY);
        mCurvePath.quadTo(cx, mCurveY - mCurveHeight, mCurveRight, mCurveY);
        mCurveMeasure = new PathMeasure(mCurvePath, false);
    }

    public void reinit() {
        init();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        if (mAddedViews != null) {
            int rotation = mOrientation;
            if (rotation == 90 || rotation == 270) rotation += 180;
            rotation %= 360;
            for (View v : mAddedViews) {
                v.setRotation(rotation);
            }
        }
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mode) {
        mMode = mode;
        removeViews();
        if (mMode == NO_MODE) {
            setVisibility(INVISIBLE);
            return;
        } else {
            setVisibility(VISIBLE);
        }
        mIndex = -1;
        String key = currentKey();
        if (mMode == BLUR_SHAPR_MODE) {
            if (key == null) return;
            CharSequence[] cc = mSettingsManager.getEntries(key);
            int length = mSettingsManager.getEntryValues(key).length;
            int index = mSettingsManager.getValueIndex(key);
            updateSlider(length);
            for (int i = 0; i < length; i++) {
                View v = new TextView(mContext);
                ((TextView) v).setText(cc[i]);
                ((TextView) v).setTextColor(Color.WHITE);
                v.measure(0, 0);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(v.getMeasuredWidth(),
                        v.getMeasuredHeight());
                v.setLayoutParams(lp);
                v.setX(mPoints[i].x - v.getMeasuredWidth() / 2);
                v.setY(mPoints[i].y - 2 * v.getMeasuredHeight());
                mParent.addView(v);
                mAddedViews.add(v);
            }
            setIndex(index, true);
        } else if ( mode != NO_MODE){
            float value = mSettingsManager.geBlurSliderValue(key);
            setSlider(value,true);
            int stride = mCurveRight - mCurveLeft;
            for (int i = 0; i < 2; i++) {
                TextView v = new TextView(mContext);
                String s = "0";
                if(mMode == BLUR_STRENGTH_MODE){
                    if (i == 1) s = "7";
                } else if(mMode == BLUR_FOCUS_DISTANCE_MODE){
                    if (i == 1) s = "1";
                } else if(mMode == BLUR_LUMA_MODE){
                    if (i == 1) s = "1";
                }else{
                    s = "-0.5";
                    if (i == 1) s = "0.5";
                }
                v.setText(s);
                v.setTextColor(Color.WHITE);
                v.measure(0, 0);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(v.getMeasuredWidth(),
                        v.getMeasuredHeight());
                v.setLayoutParams(lp);
                v.setX(mCurveLeft + i * stride - v.getMeasuredWidth() / 2);
                v.setY(mCurveY - 2 * v.getMeasuredHeight());
                mParent.addView(v);
                mAddedViews.add(v);
            }
        }
        setOrientation(mOrientation);
    }

    private String getKey(int mode) {
        switch (mode) {
            case BLUR_SHAPR_MODE:
                return SettingsManager.KEY_AI_BLUR_SHAPE;
            case BLUR_STRENGTH_MODE:
                return SettingsManager.KEY_AI_BLUR_STRENGTH;
            case BLUR_FOCUS_DISTANCE_MODE:
                return SettingsManager.KEY_AI_BLUR_DISTANCE;
            case BLUR_LUMA_MODE:
                return SettingsManager.KEY_AI_BLUR_LUMA;
            case BLUR_CHROMEU_MODE:
                return SettingsManager.KEY_AI_BLUR_CHROMAU;
            case BLUR_CHROMAV_MODE:
                return SettingsManager.KEY_AI_BLUR_CHROMAV;
        }
        return null;
    }

    private String currentKey() {
        return getKey(mMode);
    }

    private void updateSlider(int n) {
        mNums = n;
        mStride = (mCurveRight - mCurveLeft) / (mNums - 1);
        mClickThreshold = mStride * 0.45f;
        mPoints = new Point[mNums];

        float slide = 1f / (mNums - 1);
        for (int i = 0; i < mNums; i++) {
            mCurveMeasure.getPosTan(mCurveMeasure.getLength() * (slide * i), curveCoordinate, null);
            mPoints[i] = new Point((int) curveCoordinate[0], (int) curveCoordinate[1]);
        }
        invalidate();
    }

    public void setSlider(float slider,boolean forceNotify) {
        mSlider = slider;
        mSettingsManager.setBlurSliderValue(getKey(mMode), forceNotify, mSlider);
        invalidate();
    }

    private void setIndex(int index, boolean force) {
        if (mIndex == index && !force) return;
        if (mIndex != -1) {
            View v = mAddedViews.get(mIndex);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(Color.WHITE);
            }
        }

        mIndex = index;
        String key = currentKey();
        View v = mAddedViews.get(mIndex);
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(BLUE);
        }
        if (key != null) mSettingsManager.setValueIndex(key, mIndex);
        CharSequence[] cc = mSettingsManager.getEntries(key);
        mUI.updateBlurModeText(mMode, cc[mIndex].toString());
        invalidate();
    }

    private void removeViews() {
        ViewGroup vg = (ViewGroup) getParent();
        if (mAddedViews != null) {
            for (int i = 0; i < mAddedViews.size(); i++) {
                vg.removeView(mAddedViews.get(i));
            }
        }
        mAddedViews = new ArrayList<View>();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mMode == BLUR_SHAPR_MODE) {
            int idx = findButton(event.getX(), event.getY());
            if (idx != -1) {
                setIndex(idx, false);
            }
        } else {
            float slider = getSlider(event.getX(), event.getY());
            if (slider >= 0) {
                setSlider(slider,false);
            }
        }
        return true;
    }

    private int findButton(float x, float y) {
        for (int i = 0; i < mNums; i++) {
            float xdiff = Math.abs(mPoints[i].x - x);
            float ydiff = Math.abs(mPoints[i].y - y);
            float dist = xdiff * xdiff + ydiff * ydiff;
            if (dist < mClickThreshold * mClickThreshold) return i;
        }
        return -1;
    }

    private float getSlider(float x, float y) {
        if (x > mCurveLeft - DRAG_X_THRESHOLD && x < mCurveRight + DRAG_X_THRESHOLD
                && y > mCurveY - mCurveHeight - DRAG_Y_THRESHOLD
                && y < mCurveY + DRAG_Y_THRESHOLD) {
            if (x < mCurveLeft) x = mCurveLeft;
            if (x > mCurveRight) x = mCurveRight;
            return (x - mCurveLeft) / (mCurveRight - mCurveLeft);
        } else {
            return -1;
        }
    }
}
