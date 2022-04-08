/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.camera.util.PersistUtil;
import com.android.camera.SettingsManager;
import java.util.List;
import org.codeaurora.snapcam.R;
import java.math.BigDecimal;
import java.util.ArrayList;

public class ProMode extends View {
    public static final int NO_MODE = -1;
    public static final int EXPOSURE_MODE = 0;
    public static final int MANUAL_MODE = 1;
    public static final int WHITE_BALANCE_MODE = 2;
    public static final int ISO_MODE = 3;
    public static final int EXPOSURE_TIME_MODE = 4;
    private static final int DRAG_Y_THRESHOLD = 100;
    private static final int DRAG_X_THRESHOLD = 30;
    private static final int BLUE = 0xff4693fb;
    private static final int SELECTED_DOT_SIZE = 20;
    private static final int DOT_SIZE = 10;
    private String TAG = "Promode";
    private static final int[] wbIcons = {R.drawable.auto, R.drawable.incandecent,
            R.drawable.fluorescent, R.drawable.sunlight, R.drawable.cloudy};
    private static final int[] wbIconsBlue = {R.drawable.auto_blue, R.drawable.incandecent_blue,
            R.drawable.fluorescent_blue, R.drawable.sunlight_blue, R.drawable.cloudy_blue};
    private static final int WB_ICON_SIZE = 80;
    private PathMeasure mCurveMeasure;
    private int mCurveLeft;
    private int mCurveRight;
    private float mSlider = -1;
    private float mFocusSlider = -1;
    private float mExpTmSlider = -1;
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
    private long []mExposureTime;
    private double mExpTmConvert = 1000000000;
    private long mLongExpTm = 100000000;
    private double mExpTmInterval = 1.1;

    private List<String>mExpTmList;
    private String mCurrentExposuretime;
    private TextView mAutoText;
    private int mIsoIndex = 0;


    public ProMode(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPaint.setStrokeWidth(7f);
        mSettingsManager = SettingsManager.getInstance();
    }

    private void init() {
        mExposureTime = mSettingsManager.getExposureRangeValues();
        if(mExposureTime != null ) setExposuretimeList();
        initExpousreTime();
        init(EXPOSURE_MODE);
        init(WHITE_BALANCE_MODE);
        init(ISO_MODE);
        mUI.updateProModeText(MANUAL_MODE, "Manual");
    }
   private void initExpousreTime() {
       mCurrentExposuretime = mSettingsManager.getKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE);
       if (mCurrentExposuretime.equals("auto") || mCurrentExposuretime.equals(""))
           mUI.updateProModeText(EXPOSURE_TIME_MODE, "Auto");
       else {
           long mExposurTime = PersistUtil.strToLong(mCurrentExposuretime, mLongExpTm);
           mUI.updateProModeText(EXPOSURE_TIME_MODE, getExposureTimeStr(mExposurTime));
       }
   }
   private int getIndexOfValue(String str,List<String>valueList){
        for (int i = 0;i < valueList.size();i++ ){
            if (str.equals(valueList.get(i))) return i;
        }
        return 0;
   }
    private void init(int mode) {
        String key = getKey(mode);
        if (key == null) return;
        int index = mSettingsManager.getValueIndex(key);
        CharSequence[] cc = mSettingsManager.getEntries(key);
        mIsoIndex = mSettingsManager.getValueIndex(SettingsManager.KEY_ISO);
        if (mode == EXPOSURE_MODE && mCurrentExposuretime != null && !mCurrentExposuretime.equals("auto") && !mCurrentExposuretime.equals("") && mIsoIndex > 0) {
            mUI.updateProModeText(mode, "0");
            mUI.setModeEnable(EXPOSURE_MODE, false);
            mSettingsManager.setValueIndex(SettingsManager.KEY_EXPOSURE, 0);
        } else {
            mUI.updateProModeText(mode, cc[index].toString());
        }
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
        if (mMode == MANUAL_MODE || mMode == EXPOSURE_TIME_MODE) {
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(mCurveLeft, mCurveY, DOT_SIZE, mPaint);
            canvas.drawCircle(mCurveRight, mCurveY, DOT_SIZE, mPaint);
            mPaint.setColor(BLUE);
            if(mMode == MANUAL_MODE) mSlider = mFocusSlider;
            else mSlider = mExpTmSlider;
            if (mSlider >= 0f) {
                mCurveMeasure.getPosTan(mCurveMeasure.getLength() * mSlider, curveCoordinate, null);
                canvas.drawCircle(curveCoordinate[0], curveCoordinate[1], SELECTED_DOT_SIZE,
                        mPaint);
            }
        } else {
            for (int i = 0; i < mNums; i++) {
                if (i == mIndex) {
                    mPaint.setColor(BLUE);
                    canvas.drawCircle(mPoints[i].x, mPoints[i].y, SELECTED_DOT_SIZE, mPaint);
                } else {
                    mPaint.setColor(Color.WHITE);
                    canvas.drawCircle(mPoints[i].x, mPoints[i].y, DOT_SIZE, mPaint);
                }
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
        mCurveY = (int) (mHeight * 0.70);

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
        int lastMode = mMode;
        mMode = mode;
        if (lastMode == MANUAL_MODE && mode != MANUAL_MODE) {
            //set key focus value to notify capture module to reset focus mode
            mSettingsManager.setFocusSliderValue(SettingsManager.KEY_FOCUS_DISTANCE, true,
                    -1f);
        }
        removeViews();
        if (mMode == NO_MODE) {
            setVisibility(INVISIBLE);
            return;
        } else {
            setVisibility(VISIBLE);
        }
        mIndex = -1;
        String key = currentKey();
        if (mMode == MANUAL_MODE || mMode == EXPOSURE_TIME_MODE) {
            if (mMode == MANUAL_MODE) {
                float value = mSettingsManager.getFocusSliderValue(SettingsManager.KEY_FOCUS_DISTANCE);
                setSlider(value, true);
                int stride = mCurveRight - mCurveLeft;
                setSlideText("infinity", 0, stride);
                setSlideText("macro", 1, stride);
            } else if (mMode == EXPOSURE_TIME_MODE) {
                int index = getIndexOfValue(mCurrentExposuretime,mExpTmList);
                mExpTmSlider = (float) index/mExpTmList.size();
                int stride = mCurveRight - mCurveLeft;
                setSlideText("Auto", -1, stride);
                for (int i = 0; i < mExposureTime.length; i++) {
                    setSlideText(getExposureTimeStr(mExposureTime[i]), i, stride);
                }
                initExpousreTime();
                invalidate();
            }
        }else {
            if (key == null) return;
            CharSequence[] cc = mSettingsManager.getEntries(key);
            int length = mSettingsManager.getEntryValues(key).length;
            int index = mSettingsManager.getValueIndex(key);
            updateSlider(length);
            for (int i = 0; i < length; i++) {
                View v;
                if (mMode == WHITE_BALANCE_MODE) {
                    v = new ImageView(mContext);
                    ((ImageView) v).setImageResource(wbIcons[i]);
                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                            WB_ICON_SIZE, WB_ICON_SIZE);
                    v.setLayoutParams(lp);
                    v.setX(mPoints[i].x - WB_ICON_SIZE / 2);
                    v.setY(mPoints[i].y - 2 * WB_ICON_SIZE);
                } else {
                    v = new TextView(mContext);
                    ((TextView) v).setText(cc[i]);
                    ((TextView) v).setTextColor(Color.WHITE);
                    v.measure(0, 0);
                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(v.getMeasuredWidth(),
                            v.getMeasuredHeight());
                    v.setLayoutParams(lp);
                    v.setX(mPoints[i].x - v.getMeasuredWidth() / 2);
                    v.setY(mPoints[i].y - 2 * v.getMeasuredHeight());
                }
                mParent.addView(v);
                mAddedViews.add(v);
            }
            setIndex(index, true);

        }
        setOrientation(mOrientation);
    }
        private String getExposureTimeStr (long exposuretime){
            double value = exposuretime / mExpTmConvert;
            if (value < 0.1) {
                double tmpvalue = mExpTmConvert / exposuretime;
                if(Math.round(tmpvalue) == 1 ) return "1";
                return ("1/" + Math.round(tmpvalue));
            } else if(exposuretime <= mExposureTime[1]) {
                BigDecimal bigDecimal = new BigDecimal(value);
                double bg = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                return String.valueOf(bg);
            } else {
                return String.valueOf(Math.round(value));
            }
    }
    private void setSlideText(String text, int i,int stride) {
        TextView v = new TextView(mContext);
        v.setText(text);
        v.measure(0, 0);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(v.getMeasuredWidth(),
                v.getMeasuredHeight());
        v.setLayoutParams(lp);
        if (i == -1) {
            v.setX(mCurveLeft - v.getMeasuredWidth() - 5);
            v.setY(mCurveY + 25);
            if(mCurrentExposuretime.equals("auto") ){
                v.setTextColor(BLUE);
            }else{
                v.setTextColor(Color.WHITE);
            }
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View vw) {
                    mSettingsManager.setKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE,true,"auto");
                    v.setTextColor(BLUE);
                    mUI.updateProModeText(EXPOSURE_TIME_MODE, "Auto");
                    mExpTmSlider = -1;
                    if(!mCurrentExposuretime.equals("auto")){
                        mUI.setModeEnable(EXPOSURE_MODE,true);
                        mCurrentExposuretime = "auto";
                    }
                    invalidate();
                }
                });
            mAutoText = v;
        } else {
            v.setTextColor(Color.WHITE);
            v.setX(mCurveLeft + i * stride - v.getMeasuredWidth() / 2);
            v.setY(mCurveY - 2 * v.getMeasuredHeight());
        }

        mParent.addView(v);
        mAddedViews.add(v);

    }
    private String getKey(int mode) {
        switch (mode) {
            case EXPOSURE_MODE:
                return SettingsManager.KEY_EXPOSURE;
            case WHITE_BALANCE_MODE:
                return SettingsManager.KEY_WHITE_BALANCE;
            case ISO_MODE:
                return SettingsManager.KEY_ISO;
            case EXPOSURE_TIME_MODE:
                return SettingsManager.KEY_MANUAL_EXPOSURE_VALUE;
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
    private void setExposuretimeList() {
        long exposuretime = mExposureTime[0];
        mExpTmList = new ArrayList<String>();
        int i = 0;
        while (exposuretime <= mExposureTime[1]) {
            BigDecimal bigDecimal = new BigDecimal(exposuretime);
            mExpTmList.add(String.valueOf(exposuretime));
            if(exposuretime < mLongExpTm) i++;
            exposuretime = Math.round(exposuretime * mExpTmInterval);
        }
        mExpTmList.add(String.valueOf(mExposureTime[1]));
    }
    public void setSlider(float slider,boolean forceNotify) {
        if(mMode == MANUAL_MODE) {
            mFocusSlider = slider;
            mSettingsManager.setFocusSliderValue(SettingsManager.KEY_FOCUS_DISTANCE, forceNotify,
                    mFocusSlider);
            mUI.updateProModeText(mMode, "Manual");
        }else if(mMode == EXPOSURE_TIME_MODE){
            mExpTmSlider = slider;
            int index =(int) (slider * mExpTmList.size());
            if (index > mExpTmList.size() -1 ) index = mExpTmList.size() -1;
            String valuestr = mExpTmList.get(index);
            Long value =PersistUtil.strToLong(valuestr,mLongExpTm);
            mUI.updateProModeText(mMode, getExposureTimeStr(value));
            mSettingsManager.setKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE,forceNotify,valuestr);
            mAutoText.setTextColor(Color.WHITE);
            if(mIsoIndex > 0 ) resetEVAndDisable();
            else if (mIsoIndex == 0) mUI.setModeEnable(EXPOSURE_MODE,true);

            mCurrentExposuretime = valuestr;
        }
        invalidate();
    }

    private void setIndex(int index, boolean force) {
        if (mIndex == index && !force) return;
        if (mIndex != -1 && mAddedViews != null && mAddedViews.size() > 0) {
            View v = mAddedViews.get(mIndex);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(Color.WHITE);
            } else if (v instanceof ImageView) {
                if (mMode == WHITE_BALANCE_MODE) {
                    ((ImageView) v).setImageResource(wbIcons[mIndex]);
                }
            }
        }

        mIndex = index;
        String key = currentKey();
        View v = mAddedViews.get(mIndex);
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(BLUE);
        } else if (v instanceof ImageView) {
            if (mMode == WHITE_BALANCE_MODE) {
                ((ImageView) v).setImageResource(wbIconsBlue[mIndex]);
            }
        }
        if (key != null) mSettingsManager.setValueIndex(key, mIndex);
        CharSequence[] cc = mSettingsManager.getEntries(key);
        mUI.updateProModeText(mMode, cc[mIndex].toString());
        if(mMode == ISO_MODE) {
            if (mCurrentExposuretime != null && !mCurrentExposuretime.equals("auto") && !mCurrentExposuretime.equals("") && index > 0) {
                resetEVAndDisable();
            } else {
                mUI.setModeEnable(EXPOSURE_MODE, true);
            }
            mIsoIndex = index;
        }
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
        if (mMode == MANUAL_MODE || mMode == EXPOSURE_TIME_MODE) {
            float slider = getSlider(event.getX(), event.getY());
            if (slider >= 0) {
                setSlider(slider, false);
            }
        } else {
            int idx = findButton(event.getX(), event.getY());
            if (idx != -1) {
                setIndex(idx, false);
            }
            return true;
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

    private void resetEVAndDisable(){
        mUI.setModeEnable(EXPOSURE_MODE,false);
        if (mSettingsManager != null) {
            String defaultEV = getResources().getString(
                    R.string.pref_exposure_default);
            mSettingsManager.setValue(SettingsManager.KEY_EXPOSURE,defaultEV);
            mUI.updateProModeText(EXPOSURE_MODE,"0");
        }
    }
}
