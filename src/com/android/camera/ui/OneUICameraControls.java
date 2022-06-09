/*
 * Copyright (c) 2016-2017 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.util.Log;

import com.android.camera.CaptureModule;
import com.android.camera.imageprocessor.filter.BeautificationFilter;
import com.android.camera.SettingsManager;
import com.android.camera.Storage;
import com.android.camera.ui.FlashToggleButton;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.DashPathEffect;

import org.codeaurora.snapcam.R;

public class OneUICameraControls extends RotatableLayout {

    private static final String TAG = "CAM_Controls";

    private static final float TOP_PANEL_SPACE_NUM = 4f;
    private static final float BOTTOM_PANEL_SPACE_NUM = 5f;
    private static final float PANEL_INDEX_0 = 0f;
    private static final float PANEL_INDEX_1 = 1f;
    private static final float PANEL_INDEX_2 = 2f;
    private static final float PANEL_INDEX_3 = 3f;
    private static final float PANEL_INDEX_4 = 4f;
    private View mShutter;
    private ShutterButtonAnim mShutterAnim;
    private int mTotalProgress;
    public RectF mShutterAnimRect;
    public boolean mShowButtonAnim = false;
    private View mVideoShutter;
    private View mExitBestPhotpMode;
    private View mPauseButton;
    private FlashToggleButton mFlashButton;
    private View mMute;
    private View mFrontBackSwitcher;
    private View mTsMakeupSwitcher;
    private View mPreview;
    private View mSceneModeSwitcher;
    private View mSceneModeHDR;
    private View mFilterModeSwitcher;
    private View mMakeupSeekBar;
    private View mMakeupSeekBarLowText;
    private View mMakeupSeekBarHighText;
    private View mMakeupSeekBarLayout;
    private View mCancelButton;
    private ViewGroup mProModeLayout;
    private View mSettingsButton;

    private ArrowTextView mRefocusToast;

    private static final int WIDTH_GRID = 5;
    private static final int HEIGHT_GRID = 7;
    private View[] mViews;
    private boolean mHideRemainingPhoto = false;
    private LinearLayout mRemainingPhotos;
    private TextView mRemainingPhotosText;
    private int mCurrentRemaining = -1;
    private int mOrientation;

    private static int mTop = 0;
    private static int mBottom = 0;

    private Paint mPaint;

    private static final int LOW_REMAINING_PHOTOS = 20;
    private static final int HIGH_REMAINING_PHOTOS = 1000000;
    private int mWidth;
    private int mHeight;
    private boolean mVisible;
    private boolean mIsVideoMode = false;
    private int mBottomLargeSize;
    private int mBottomSmallSize;

    private int mIntentMode = CaptureModule.INTENT_MODE_NORMAL;
    private ProMode mProMode;
    private TextView mExposureText;
    private TextView mManualText;
    private TextView mWhiteBalanceText;
    private TextView mIsoText;
    private TextView mExposure;
    private TextView mFocusDistance;
    private TextView mWhiteBalance;
    private TextView mIso;
    private TextView mShutterSpeedText;
    private TextView mShutterSpeed;
    private boolean mProModeOn = false;
    private RotateLayout mExposureRotateLayout;
    private RotateLayout mManualRotateLayout;
    private RotateLayout mWhiteBalanceRotateLayout;
    private RotateLayout mIsoRotateLayout;
    private RotateLayout mShutterSpeedLayout;
    private LinearLayout mExposureLayout;
    private LinearLayout mManualLayout;
    private LinearLayout mWBLayout;
    private LinearLayout mISOLayout;
    private LinearLayout mShutterLayout;
    private static final int BLUE = 0xff4693fb;
    private static final int GREY = 0xff808080;
    private TextView[] mProViews;
    private boolean isExposureEnable = true;
    private boolean mBlurModeOn = false;
    private ViewGroup mAIBlurLayout;
    private AIBlurConfigSlide mAIBlurSlide;
    private TextView[] mAIBlurViews;
    private TextView mBlurShapeText;
    private TextView mBlurStrengthText;
    private TextView mBlurFocusDistanceText;
    private TextView mBlurLumaText;
    private TextView mBlurChromaUText;
    private TextView mBlurChromaVText;
    private TextView mBlurShape;
    private TextView mBlurStrength;
    private TextView mBlurFocusDistance;
    private TextView mBlurLuma;
    private TextView mBlurChromaU;
    private TextView mBlurChromaV;

    public OneUICameraControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);

        mRefocusToast = new ArrowTextView(context);
        addView(mRefocusToast);
        setClipChildren(false);

        setMeasureAllChildren(true);
        mPaint.setColor(getResources().getColor(R.color.camera_control_bg_transparent));

        mTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        mBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        mVisible = true;

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;
    }

    public OneUICameraControls(Context context) {
        this(context, null);
    }

    public void setIntentMode(int mode) {
        mIntentMode = mode;
    }

    public int getIntentMode() {
        return mIntentMode;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mShutter = findViewById(R.id.shutter_button);
        mShutterAnim = findViewById(R.id.shutterbutton_anim);
        mVideoShutter = findViewById(R.id.video_button);
        mExitBestPhotpMode = findViewById(R.id.exit_best_mode);
        mPauseButton = findViewById(R.id.video_pause);
        mFrontBackSwitcher = findViewById(R.id.front_back_switcher);
        mTsMakeupSwitcher = findViewById(R.id.ts_makeup_switcher);
        mSettingsButton = findViewById(R.id.settings);
        mMakeupSeekBarLowText = findViewById(R.id.makeup_low_text);
        mMakeupSeekBarHighText = findViewById(R.id.makeup_high_text);
        mMakeupSeekBar = findViewById(R.id.makeup_seekbar);
        mMakeupSeekBarLayout = findViewById(R.id.makeup_seekbar_layout);
        ((SeekBar) mMakeupSeekBar).setMax(100);
        mFlashButton = (FlashToggleButton)findViewById(R.id.flash_button);
        mMute = findViewById(R.id.mute_button);
        mPreview = findViewById(R.id.preview_thumb);
        mSceneModeSwitcher = findViewById(R.id.scene_mode_switcher);
        mSceneModeHDR = findViewById(R.id.scene_mode_hdr);
        mFilterModeSwitcher = findViewById(R.id.filter_mode_switcher);
        mRemainingPhotos = (LinearLayout) findViewById(R.id.remaining_photos);
        mRemainingPhotosText = (TextView) findViewById(R.id.remaining_photos_text);
        mCancelButton = findViewById(R.id.cancel_button);
        mProModeLayout = (ViewGroup) findViewById(R.id.pro_mode_layout);

        mExposureText = (TextView) findViewById(R.id.exposure_value);
        mManualText = (TextView) findViewById(R.id.manual_value);
        mWhiteBalanceText = (TextView) findViewById(R.id.white_balance_value);
        mIsoText = (TextView) findViewById(R.id.iso_value);
        mShutterSpeedText = (TextView) findViewById(R.id.shutterspeed_value);
        mProMode = (ProMode) findViewById(R.id.promode_slider);

        mExposure = (TextView) findViewById(R.id.exposure_text);
        mFocusDistance = (TextView) findViewById(R.id.focusdistance_text);
        mWhiteBalance = (TextView) findViewById(R.id.whitebalance_text);
        mIso = (TextView) findViewById(R.id.iso_text);
        mShutterSpeed = (TextView) findViewById(R.id.shutterspeed_text);
        mProMode.initialize(this);

        mExposureRotateLayout = (RotateLayout) findViewById(R.id.exposure_rotate_layout);
        mManualRotateLayout = (RotateLayout) findViewById(R.id.manual_rotate_layout);
        mWhiteBalanceRotateLayout = (RotateLayout) findViewById(R.id.white_balance_rotate_layout);
        mIsoRotateLayout = (RotateLayout) findViewById(R.id.iso_rotate_layout);
        mShutterSpeedLayout = (RotateLayout) findViewById(R.id.shutterspeed_rotate_layout);
        mExposureLayout = (LinearLayout) findViewById(R.id.exposure_layout);
        mManualLayout = (LinearLayout) findViewById(R.id.manual_layout);
        mWBLayout = (LinearLayout) findViewById(R.id.wb_layout);
        mISOLayout = (LinearLayout) findViewById(R.id.iso_layout);
        mShutterLayout = (LinearLayout) findViewById(R.id.shutterspeed_layout);
        mProViews = new TextView[]{
                mExposure, mExposureText, mFocusDistance,
                mManualText, mShutterSpeed, mShutterSpeedText,
                mWhiteBalance, mWhiteBalanceText, mIso, mIsoText
        };
        mShutterAnim.setObject(this);
        mExposure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isExposureEnable) return;
                resetProModeIcons();
                int mode = mProMode.getMode();
                if (mode == ProMode.EXPOSURE_MODE) {
                    mProMode.setMode(ProMode.NO_MODE);
                    mExposure.setTextColor(Color.WHITE);
                    mExposureText.setTextColor(Color.WHITE);
                } else {
                    mExposureText.setSelected(true);
                    mProMode.setMode(ProMode.EXPOSURE_MODE);
                    setProModeUi(mExposure, mExposureText);
                }
            }
        });
        mFocusDistance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetProModeIcons();
                int mode = mProMode.getMode();
                if (mode == ProMode.MANUAL_MODE) {
                    mProMode.setMode(ProMode.NO_MODE);
                    mFocusDistance.setTextColor(Color.WHITE);
                    mManualText.setTextColor(Color.WHITE);
                } else {
                    mManualText.setSelected(true);
                    mProMode.setMode(ProMode.MANUAL_MODE);
                    setProModeUi(mFocusDistance, mManualText);
                }
            }
        });
        mShutterSpeed.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetProModeIcons();
                int mode = mProMode.getMode();
                if (mode == ProMode.EXPOSURE_TIME_MODE) {
                    mProMode.setMode(ProMode.NO_MODE);
                    mShutterSpeedText.setTextColor(Color.WHITE);
                    mShutterSpeed.setTextColor(Color.WHITE);
                } else {
                    mShutterSpeedText.setSelected(true);
                    mProMode.setMode(ProMode.EXPOSURE_TIME_MODE);
                    setProModeUi(mShutterSpeed, mShutterSpeedText);
                }
            }
        });
        mWhiteBalance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetProModeIcons();
                int mode = mProMode.getMode();
                if (mode == ProMode.WHITE_BALANCE_MODE) {
                    mProMode.setMode(ProMode.NO_MODE);
                    mWhiteBalance.setTextColor(Color.WHITE);
                    mWhiteBalanceText.setTextColor(Color.WHITE);
                } else {
                    mWhiteBalanceText.setSelected(true);
                    mProMode.setMode(ProMode.WHITE_BALANCE_MODE);
                    setProModeUi(mWhiteBalance, mWhiteBalanceText);
                }
            }
        });
        mIso.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetProModeIcons();
                int mode = mProMode.getMode();
                if (mode == ProMode.ISO_MODE) {
                    mProMode.setMode(ProMode.NO_MODE);
                    mIso.setTextColor(Color.WHITE);
                    mIsoText.setTextColor(Color.WHITE);
                } else {
                    mIsoText.setSelected(true);
                    mProMode.setMode(ProMode.ISO_MODE);
                    setProModeUi(mIso, mIsoText);
                }
            }
        });

        mViews = new View[]{
                mSceneModeHDR, mFilterModeSwitcher, mFrontBackSwitcher,
                mFlashButton, mShutter,
                mPreview, mPauseButton, mCancelButton, mSettingsButton
        };
        mBottomLargeSize = getResources().getDimensionPixelSize(
                R.dimen.one_ui_bottom_large);
        mBottomSmallSize = getResources().getDimensionPixelSize(
                R.dimen.one_ui_bottom_small);
        if (!BeautificationFilter.isSupportedStatic()) {
            mTsMakeupSwitcher.setEnabled(false);
            mTsMakeupSwitcher.setVisibility(View.GONE);
        }
        mSceneModeSwitcher.setVisibility(View.GONE);
        setProModeParameters();

        mAIBlurLayout = (ViewGroup) findViewById(R.id.ai_blur_layout);
        mBlurShapeText = (TextView) findViewById(R.id.blur_shape_value);
        mBlurStrengthText = (TextView) findViewById(R.id.blur_strength_value);
        mBlurFocusDistanceText = (TextView) findViewById(R.id.blur_focus_distance_value);
        mBlurLumaText = (TextView) findViewById(R.id.blur_luma_value);
        mBlurChromaUText = (TextView) findViewById(R.id.blur_chromaU_value);
        mBlurChromaVText = (TextView) findViewById(R.id.blur_chromaV_value);

        mBlurShape = (TextView) findViewById(R.id.blur_shape_text);
        mBlurStrength = (TextView) findViewById(R.id.blur_strength_text);
        mBlurFocusDistance = (TextView) findViewById(R.id.blur_focus_distance_text);
        mBlurLuma = (TextView) findViewById(R.id.blur_luma_text);
        mBlurChromaU = (TextView) findViewById(R.id.blur_chromaU_text);
        mBlurChromaV = (TextView) findViewById(R.id.blur_chromaV_text);
        mAIBlurSlide = (AIBlurConfigSlide) findViewById(R.id.aiblur_slide);
        mAIBlurSlide.initialize(this);
        mAIBlurViews = new TextView[]{
                mBlurShape, mBlurShapeText, mBlurStrength,mBlurStrengthText,mBlurFocusDistance,mBlurFocusDistanceText,
                mBlurLuma, mBlurLumaText,mBlurChromaU,mBlurChromaUText,mBlurChromaV,mBlurChromaVText};
        mBlurShape.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_SHAPR_MODE) {
                    mBlurShapeText.setSelected(true);
                    setAIBlurUi(mBlurShape, mBlurShapeText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_SHAPR_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurShape.setTextColor(Color.WHITE);
                    mBlurShapeText.setTextColor(Color.WHITE);
                }
            }
        });
        mBlurStrength.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_STRENGTH_MODE) {
                    mBlurStrengthText.setSelected(true);
                    setAIBlurUi(mBlurStrength, mBlurStrengthText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_STRENGTH_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurStrength.setTextColor(Color.WHITE);
                    mBlurStrengthText.setTextColor(Color.WHITE);
                }
            }
        });
        mBlurFocusDistance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_FOCUS_DISTANCE_MODE) {
                    mBlurFocusDistanceText.setSelected(true);
                    setAIBlurUi(mBlurFocusDistance, mBlurFocusDistanceText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_FOCUS_DISTANCE_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurFocusDistance.setTextColor(Color.WHITE);
                    mBlurFocusDistanceText.setTextColor(Color.WHITE);
                }
            }
        });
        mBlurLuma.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_LUMA_MODE) {
                    mBlurLumaText.setSelected(true);
                    setAIBlurUi(mBlurLuma, mBlurLumaText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_LUMA_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurLuma.setTextColor(Color.WHITE);
                    mBlurLumaText.setTextColor(Color.WHITE);
                }
            }
        });
        mBlurChromaU.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_CHROMEU_MODE) {
                    mBlurChromaUText.setSelected(true);
                    setAIBlurUi(mBlurChromaU, mBlurChromaUText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_CHROMEU_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurChromaU.setTextColor(Color.WHITE);
                    mBlurChromaUText.setTextColor(Color.WHITE);
                }
            }
        });
        mBlurChromaV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAIBlurConfigIcons();
                if (mAIBlurSlide.getMode() != AIBlurConfigSlide.BLUR_CHROMAV_MODE) {
                    mBlurChromaVText.setSelected(true);
                    setAIBlurUi(mBlurChromaV, mBlurChromaVText);
                    mAIBlurSlide.setMode(AIBlurConfigSlide.BLUR_CHROMAV_MODE);
                }else {
                    mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
                    mBlurChromaV.setTextColor(Color.WHITE);
                    mBlurChromaVText.setTextColor(Color.WHITE);
                }
            }
        });
        setAIBlurConfigParameters();
        setChromaEnable(mAIBlurSlide.isChromaMode());
    }

    public void setChromaEnable(boolean value){
        mBlurChromaU.setEnabled(value);
        mBlurChromaV.setEnabled(value);
        if(!value) {
            mBlurChromaU.setTextColor(GREY);
            mBlurChromaUText.setTextColor(GREY);
            mBlurChromaV.setTextColor(GREY);
            mBlurChromaVText.setTextColor(GREY);
        }else{
            if(mAIBlurSlide.getMode() == AIBlurConfigSlide.BLUR_CHROMEU_MODE){
                mBlurChromaU.setTextColor(BLUE);
                mBlurChromaUText.setTextColor(BLUE);
            }else{
                mBlurChromaU.setTextColor(Color.WHITE);
                mBlurChromaUText.setTextColor(Color.WHITE);
            }
            if(mAIBlurSlide.getMode() == AIBlurConfigSlide.BLUR_CHROMAV_MODE){
                mBlurChromaV.setTextColor(BLUE);
                mBlurChromaVText.setTextColor(BLUE);
            }else{
                mBlurChromaV.setTextColor(Color.WHITE);
                mBlurChromaVText.setTextColor(Color.WHITE);
            }
        }
    }

    private void setProModeUi(TextView v1,TextView v2) {
        for (TextView v : mProViews) {
            if (v != null && (v == v1 || v == v2)) {
                v.setTextColor(BLUE);
            } else {
                v.setTextColor(Color.WHITE);
                if (v == mExposure && !isExposureEnable) v.setTextColor(GREY);
                else
                    v.setTextColor(Color.WHITE);
            }
        }

    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
        if (mMakeupSeekBar != null) {
            mMakeupSeekBar.setMinimumWidth(mWidth / 2);
        }
        setProModeParameters();
        setAIBlurConfigParameters();
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        r = r - l;
        b = b - t;
        l = 0;
        t = 0;

        setLocation(r - l, b - t);
        layoutRemaingPhotos();
        initializeProMode(mProModeOn);
        initializeAIBlurSlide(mBlurModeOn);
    }

    public boolean isControlRegion(int x, int y) {
        return y <= mTop || y >= (mHeight - mBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mVisible) {
            int rotation = getUnifiedRotation();
            int w = canvas.getWidth(), h = canvas.getHeight();
            switch (rotation) {
                case 90:
                    canvas.drawRect(0, 0, mTop, h, mPaint);
                    canvas.drawRect(w - mBottom, 0, w, h, mPaint);
                    break;
                case 180:
                    canvas.drawRect(0, 0, w, mBottom, mPaint);
                    canvas.drawRect(0, h - mTop, w, h, mPaint);
                    break;
                case 270:
                    canvas.drawRect(0, 0, mBottom, h, mPaint);
                    canvas.drawRect(w - mTop, 0, w, h, mPaint);
                    break;
                default:
                    canvas.drawRect(0, 0, w, mTop, mPaint);
                    canvas.drawRect(0, h - mBottom, w, h, mPaint);
                    break;
            }
        }
    }

    private void setLocation(View v, boolean top, float idx) {
        if (v == null) {
            return;
        }
        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();
        if (top) {
            v.setY((mTop - h) / 2);
        } else {
            v.setY(mHeight - mBottom + (mBottom - h) / 2);
        }
        float bW;
        if (top) {
            bW = mWidth / TOP_PANEL_SPACE_NUM;
        } else {
            bW = mWidth / BOTTOM_PANEL_SPACE_NUM;
        }
        v.setX(bW * idx + (bW - w) / 2);
        if (v == mShutter) {
            mShutterAnimRect = new RectF();
            mShutterAnimRect.left = bW * idx + (bW - w) / 2 + 5;
            mShutterAnimRect.top = mHeight - mBottom + (mBottom - h) / 2 + 5;
            mShutterAnimRect.right = mShutterAnimRect.left + w - 7;
            mShutterAnimRect.bottom = mShutterAnimRect.top + h - 7;
        }
    }

    public void showAnim() {
        mShowButtonAnim = true;
        if (mShutterAnim != null) {
            mShutterAnim.setVisibility(View.VISIBLE);
            mShutterAnim.refleshProgress();
        }
    }

    public void hidenAnim() {
        mShowButtonAnim = false;
        if (mShutterAnim != null && mShutterAnim.getVisibility() == View.VISIBLE) {
            mShutterAnim.setVisibility(View.INVISIBLE);
        }
    }

    public void setShutterProgress(int progress, int totalProgress) {
        if (progress <= totalProgress && progress > 0) {
            mShutterAnim.setProgress(progress, totalProgress);
        }
    }

    private void setLocationCustomBottom(View v, float x, float y) {
        if (v == null) {
            return;
        }
        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();
        int bW = mWidth / 5;
        int bH = mHeight / 6;
        v.setY(mHeight - mBottom + (mBottom - h) / 2 - bH * y);
        v.setX(bW * x);
    }

    private void setLocation(int w, int h) {
        int rotation = getUnifiedRotation();
        setLocation(mSceneModeHDR, true, PANEL_INDEX_0);
        setLocation(mFilterModeSwitcher, true, PANEL_INDEX_1);
        if (mIsVideoMode) {
            setLocation(mMute, true, PANEL_INDEX_1);
            setLocation(mFlashButton, true, PANEL_INDEX_2);
            setLocation(mSettingsButton, true, PANEL_INDEX_3);
            setLocation(mPauseButton, false, 3.15f);
            setLocation(mShutter, false, 0.85f);
            setLocation(mVideoShutter, false, PANEL_INDEX_2);
            setLocation(mExitBestPhotpMode, false, PANEL_INDEX_4);
        } else {
            setLocation(mFlashButton, true, PANEL_INDEX_2);
            setLocation(mSettingsButton, true, PANEL_INDEX_3);
            setLocation(mFrontBackSwitcher, false, 3.15f);
            if (mIntentMode == CaptureModule.INTENT_MODE_CAPTURE) {
                setLocation(mShutter, false, PANEL_INDEX_2);
                setLocation(mCancelButton, false, 0.85f);
            } else if (mIntentMode == CaptureModule.INTENT_MODE_VIDEO) {
                setLocation(mVideoShutter, false, PANEL_INDEX_2);
                setLocation(mCancelButton, false, 0.85f);
            } else {
                setLocation(mVideoShutter, false, PANEL_INDEX_2);
                setLocation(mShutter, false, PANEL_INDEX_2);
                setLocation(mPreview, false, 0.85f);
            }
            setLocation(mExitBestPhotpMode, false, PANEL_INDEX_4);
        }
        setLocationCustomBottom(mMakeupSeekBarLayout, 0, 1);

        layoutToast(mRefocusToast, w, h, rotation);
    }

    private void setBottomButtionSize(View view, int width, int height) {
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) view.getLayoutParams();
        layout.height = height;
        layout.width = width;
        view.setLayoutParams(layout);
    }

    private void layoutToast(final View v, int w, int h, int rotation) {
        int tw = v.getMeasuredWidth();
        int th = v.getMeasuredHeight();
        int l, t, r, b, c;
        switch (rotation) {
            case 90:
                c = (int) (h / WIDTH_GRID * (WIDTH_GRID - 0.5));
                t = c - th / 2;
                b = c + th / 2;
                r = (int) (w / HEIGHT_GRID * (HEIGHT_GRID - 1.25));
                l = r - tw;
                mRefocusToast.setArrow(tw, th / 2, tw + th / 2, th, tw, th);
                break;
            case 180:
                t = (int) (h / HEIGHT_GRID * 1.25);
                b = t + th;
                r = (int) (w / WIDTH_GRID * (WIDTH_GRID - 0.25));
                l = r - tw;
                mRefocusToast.setArrow(tw - th / 2, 0, tw, 0, tw, -th / 2);
                break;
            case 270:
                c = (int) (h / WIDTH_GRID * 0.5);
                t = c - th / 2;
                b = c + th / 2;
                l = (int) (w / HEIGHT_GRID * 1.25);
                r = l + tw;
                mRefocusToast.setArrow(0, 0, 0, th / 2, -th / 2, 0);
                break;
            default:
                l = w / WIDTH_GRID / 4;
                b = (int) (h / HEIGHT_GRID * (HEIGHT_GRID - 1.25));
                r = l + tw;
                t = b - th;
                mRefocusToast.setArrow(0, th, th / 2, th, 0, th * 3 / 2);
                break;
        }
        mRefocusToast.layout(l, t, r, b);
    }

    public void hideUI() {
        for (View v : mViews) {
            if (v != null)
                v.setVisibility(View.INVISIBLE);
        }
        mVisible = false;
    }

    public void showUI() {
        for (View v : mViews) {
            if (v != null)
                v.setVisibility(View.VISIBLE);
        }
        mVisible = true;
    }

    public void updateProUIForTest(String key, String value) {
        resetProModeIcons();
        int mode = mProMode.getMode();
        if (key.equals(SettingsManager.KEY_FOCUS_DISTANCE)) {
            mManualText.setSelected(true);
            mProMode.setMode(ProMode.MANUAL_MODE);
        } else if (key.equals(SettingsManager.KEY_WHITE_BALANCE)) {
            mWhiteBalanceText.setSelected(true);
            mProMode.setMode(ProMode.WHITE_BALANCE_MODE);
        } else if (key.equals(SettingsManager.KEY_EXPOSURE)) {
            mExposureText.setSelected(true);
            mProMode.setMode(ProMode.EXPOSURE_MODE);
        } else if (key.equals(SettingsManager.KEY_ISO)) {
            mIsoText.setSelected(true);
            mProMode.setMode(ProMode.ISO_MODE);
        }
    }

    public void setVideoMode(boolean videoMode) {
        mIsVideoMode = videoMode;
        if (mIsVideoMode) {
            setBottomButtionSize(mVideoShutter, mBottomLargeSize, mBottomLargeSize);
            setBottomButtionSize(mShutter, mBottomSmallSize, mBottomSmallSize);
        } else {
            if (mIntentMode == CaptureModule.INTENT_MODE_CAPTURE) {
                setBottomButtionSize(mShutter, mBottomLargeSize, mBottomLargeSize);
            } else if (mIntentMode == CaptureModule.INTENT_MODE_VIDEO) {
                setBottomButtionSize(mVideoShutter, mBottomLargeSize, mBottomLargeSize);
            } else {
                setBottomButtionSize(mShutter, mBottomLargeSize, mBottomLargeSize);
            }
        }
    }

    private void layoutRemaingPhotos() {
        int rl = mPreview.getLeft();
        int rt = mPreview.getTop();
        int rr = mPreview.getRight();
        int rb = mPreview.getBottom();
        int w = mRemainingPhotos.getMeasuredWidth();
        int h = mRemainingPhotos.getMeasuredHeight();
        int m = getResources().getDimensionPixelSize(R.dimen.remaining_photos_margin);

        int hc = (rl + rr) / 2;
        int vc = (rt + rb) / 2 - m;
        if (mOrientation == 90 || mOrientation == 270) {
            vc -= w / 2;
        }
        if (hc < w / 2) {
            mRemainingPhotos.layout(0, vc - h / 2, w, vc + h / 2);
        } else {
            mRemainingPhotos.layout(hc - w / 2, vc - h / 2, hc + w / 2, vc + h / 2);
        }
        mRemainingPhotos.setRotation(-mOrientation);
    }

    public void updateRemainingPhotos(int remaining) {
        long remainingStorage = Storage.getAvailableSpace() - Storage.LOW_STORAGE_THRESHOLD_BYTES;
        if ((remaining < 0 && remainingStorage <= 0) || mHideRemainingPhoto) {
            mRemainingPhotos.setVisibility(View.GONE);
        } else {
            for (int i = mRemainingPhotos.getChildCount() - 1; i >= 0; --i) {
                mRemainingPhotos.getChildAt(i).setVisibility(View.VISIBLE);
            }
            if (remaining < LOW_REMAINING_PHOTOS) {
                mRemainingPhotosText.setText("<" + LOW_REMAINING_PHOTOS + " ");
            } else if (remaining >= HIGH_REMAINING_PHOTOS) {
                mRemainingPhotosText.setText(">" + HIGH_REMAINING_PHOTOS);
            } else {
                mRemainingPhotosText.setText(remaining + " ");
            }
        }
        mCurrentRemaining = remaining;
    }

    public void showRefocusToast(boolean show) {
        mRefocusToast.setVisibility(show ? View.VISIBLE : View.GONE);
        if ((mCurrentRemaining > 0) && !mHideRemainingPhoto) {
            mRemainingPhotos.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        mOrientation = orientation;
        View[] views = {
                mSceneModeHDR, mFilterModeSwitcher, mFrontBackSwitcher,
                mFlashButton, mSettingsButton, mPreview,
                mMute, mShutter, mVideoShutter, mMakeupSeekBarLowText, mMakeupSeekBarHighText,
                mPauseButton, mExitBestPhotpMode
        };

        for (View v : views) {
            if (v != null) {
                ((Rotatable) v).setOrientation(orientation, animation);
            }
        }
/*        mExposureRotateLayout.setOrientation(orientation, animation);
        mManualRotateLayout.setOrientation(orientation, animation);
        mWhiteBalanceRotateLayout.setOrientation(orientation, animation);
        mIsoRotateLayout.setOrientation(orientation, animation);
        mShutterSpeedLayout.setOrientation(orientation, animation);*/
        mProMode.setOrientation(orientation);
        layoutRemaingPhotos();
    }

    private class ArrowTextView extends TextView {
        private static final int TEXT_SIZE = 14;
        private static final int PADDING_SIZE = 18;
        private static final int BACKGROUND = 0x80000000;

        private Paint mPaint;
        private Path mPath;

        public ArrowTextView(Context context) {
            super(context);

            setText(context.getString(R.string.refocus_toast));
            setBackgroundColor(BACKGROUND);
            setVisibility(View.GONE);
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            setTextSize(TEXT_SIZE);
            setPadding(PADDING_SIZE, PADDING_SIZE, PADDING_SIZE, PADDING_SIZE);

            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(BACKGROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mPath != null) {
                canvas.drawPath(mPath, mPaint);
            }
        }

        public void setArrow(float x1, float y1, float x2, float y2, float x3, float y3) {
            mPath = new Path();
            mPath.reset();
            mPath.moveTo(x1, y1);
            mPath.lineTo(x2, y2);
            mPath.lineTo(x3, y3);
            mPath.lineTo(x1, y1);
        }
    }

    public void setProMode(boolean promode) {
        mProModeOn = promode;
        initializeProMode(mProModeOn);
        mProMode.reinit();
        resetProModeIcons();
    }

    public int getPromode() {
        return mProMode != null ? mProMode.getMode() : -99;
    }

    private void resetProModeIcons() {
        mExposureText.setSelected(false);
        mManualText.setSelected(false);
        mWhiteBalanceText.setSelected(false);
        mIsoText.setSelected(false);
    }

    private void setProModeParameters() {
        int width = (mWidth > mHeight) ? mHeight : mWidth;
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(width / 5, width / 15);
        for (TextView v : mProViews) {
            v.setLayoutParams(llp);
        }
    }

    private void initializeProMode(boolean promode) {
        if (!promode) {
            mProMode.setMode(ProMode.NO_MODE);
            mProModeLayout.setVisibility(INVISIBLE);
            return;
        }
        mProModeLayout.setVisibility(VISIBLE);
        mProModeLayout.setY(mHeight - mBottom - mProModeLayout.getHeight() - 100);
    }
    public void setModeEnable(int mode,boolean isEnable) {
        switch (mode) {
            case ProMode.EXPOSURE_MODE:
                if(!isEnable) {
                    mExposure.setTextColor(GREY);
                    isExposureEnable = false;

                } else {
                    mExposure.setTextColor(Color.WHITE);
                    isExposureEnable = true;
                }
                ;
                break;
        }
    }
    public void closeFlashForPro( boolean isHiden){
        android.util.Log.d(TAG,"zcl closeFlashForPro isHiden="+isHiden);
        if(isHiden) {
            mFlashButton.closeFlash(true);
            mFlashButton.setVisibility(View.INVISIBLE);
        }else{
            mFlashButton.setVisibility(View.VISIBLE);
        }
    }

    public void updateProModeText(int mode, String value) {
        switch (mode) {
            case ProMode.EXPOSURE_MODE:
                mExposureText.setText(value);
                break;
            case ProMode.MANUAL_MODE:
                mManualText.setText(value);
                break;
            case ProMode.WHITE_BALANCE_MODE:
                mWhiteBalanceText.setText(value);
                break;
            case ProMode.ISO_MODE:
                mIsoText.setText(value);
                break;
            case ProMode.EXPOSURE_TIME_MODE:
                mShutterSpeedText.setText(value);
        }
    }

    private void setAIBlurUi(TextView v1,TextView v2) {
        for (TextView v : mAIBlurViews) {
            if (v != null && (v == v1 || v == v2)) {
                v.setTextColor(BLUE);
            } else {
                v.setTextColor(Color.WHITE);
            }
        }

    }

    public void setBlurMode(boolean blurMode) {
        mBlurModeOn = blurMode;
        initializeAIBlurSlide(mBlurModeOn);
        mAIBlurSlide.reinit();
        resetAIBlurConfigIcons();
    }

    private void resetAIBlurConfigIcons() {
        mBlurShapeText.setSelected(false);
        mBlurStrengthText.setSelected(false);
        mBlurFocusDistanceText.setSelected(false);
        mBlurLumaText.setSelected(false);
        mBlurChromaUText.setSelected(false);
        mBlurChromaVText.setSelected(false);
    }

    private void setAIBlurConfigParameters() {
        int width = (mWidth > mHeight) ? mHeight : mWidth;
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(width / 6, width / 15);
        for (TextView v : mAIBlurViews) {
            v.setLayoutParams(llp);
        }
    }

    private void initializeAIBlurSlide(boolean blurMode) {
        if (!blurMode) {
            mAIBlurSlide.setMode(AIBlurConfigSlide.NO_MODE);
            mAIBlurLayout.setVisibility(INVISIBLE);
            return;
        }
        mAIBlurLayout.setVisibility(VISIBLE);
        mAIBlurLayout.setY(mHeight - mBottom - mAIBlurLayout.getHeight() - 250);
    }

    public void updateBlurModeText(int mode, String value) {
        if((mode == AIBlurConfigSlide.BLUR_CHROMEU_MODE|| mode == AIBlurConfigSlide.BLUR_CHROMAV_MODE)){
            Float text = Float.parseFloat(value);
            text = text-0.5f;
            value = String.format("%.2f", text);
        }
        switch (mode) {
            case AIBlurConfigSlide.BLUR_SHAPR_MODE:
                mBlurShapeText.setText(value);
                break;
            case AIBlurConfigSlide.BLUR_STRENGTH_MODE:
                mBlurStrengthText.setText(value);
                break;
            case AIBlurConfigSlide.BLUR_FOCUS_DISTANCE_MODE:
                mBlurFocusDistanceText.setText(value);
                break;
            case AIBlurConfigSlide.BLUR_LUMA_MODE:
                mBlurLumaText.setText(value);
                break;
            case AIBlurConfigSlide.BLUR_CHROMEU_MODE:
                mBlurChromaUText.setText(value);
                break;
            case AIBlurConfigSlide.BLUR_CHROMAV_MODE:
                mBlurChromaVText.setText(value);
                break;
        }
    }
}

class ShutterButtonAnim extends View {
    private OneUICameraControls mcontrol;
    private int mProgress;
    private int mTotalProgress;

    public ShutterButtonAnim(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mcontrol.mShowButtonAnim) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(new DashPathEffect(new float[]{5, 10}, 0));
            paint.setStrokeWidth(8);
            canvas.drawArc(mcontrol.mShutterAnimRect, -90, ((float) mProgress / mTotalProgress) * 360, false, paint); //
        } else {
            super.onDraw(canvas);
            return;
        }
    }

    public void setObject(OneUICameraControls control) {
        mcontrol = control;
    }

    public void setProgress(int progress, int totalProgress) {
        mProgress = progress;
        mTotalProgress = totalProgress;
        postInvalidate();
    }

    public void refleshProgress() {
        invalidate();
    }
}
