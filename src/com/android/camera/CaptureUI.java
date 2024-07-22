/*
 * Copyright (c) 2016-2017 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project
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
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera.Face;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.android.camera.gles.CameraRender;
import com.android.camera.ui.RotateTextView;
import com.android.camera.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.camera.app.FilmstripBottomPanel;
import com.android.camera.imageprocessor.filter.BeautificationFilter;
import com.android.camera.data.Camera2ModeAdapter;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.ui.AutoFitSurfaceView;
import com.android.camera.ui.AutoFitTextureView;
import com.android.camera.ui.Camera2FaceView;
import com.android.camera.ui.CameraControls;
import com.android.camera.ui.FocusAssistImageView;
import com.android.camera.ui.FocusAssistLayout;
import com.android.camera.ui.MenuHelp;
import com.android.camera.ui.OneUICameraControls;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.FlashToggleButton;
import com.android.camera.ui.FocusIndicator;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.SelfieFlashView;
import com.android.camera.ui.TrackingFocusRenderer;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.ui.TouchTrackFocusRenderer;
import com.android.camera.ui.StateNNTrackFocusRenderer;
import com.android.camera.ui.AFView;
import com.android.camera.util.CameraUtil;
import com.android.camera.deepportrait.GLCameraPreview;
import com.android.camera.util.PersistUtil;
import com.android.camera.widget.Cling;
import com.android.camera.widget.FilmstripLayout;
import android.hardware.camera2.CameraCharacteristics;
import com.android.camera.ui.VerticalSeekBar;
import android.hardware.camera2.CameraAccessException;
import org.codeaurora.snapcam.R;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CaptureUI implements FocusOverlayManager.FocusUI,
        PreviewGestures.SingleTapListener,
        CameraManager.CameraFaceDetectionCallback,
        SettingsManager.Listener,
        PauseButton.OnPauseButtonListener {
    private static final int HIGHLIGHT_COLOR = 0xff33b5e5;
    private static final String TAG = "SnapCam_CaptureUI";
    private static final boolean DEV_LEVEL_ALL =
            PersistUtil.getDevOptionLevel() == PersistUtil.CAMERA2_DEV_OPTION_ALL;
    private static final int FILTER_MENU_NONE = 0;
    private static final int FILTER_MENU_IN_ANIMATION = 1;
    private static final int FILTER_MENU_ON = 2;
    private static final int ANIMATION_DURATION = 300;
    private static final int CLICK_THRESHOLD = 200;
    private static final int AUTOMATIC_MODE = 0;
    private static final int ZOOM_SMOOTH_FRAME = PersistUtil.getZoomFrameValue();
    private static final int ZOOM_SMOOTH_FRAME_MAX = 2 * ZOOM_SMOOTH_FRAME;
    private static final String[] AWB_INFO_TITLE = {" R gain "," G gain "," B gain "," CCT "};
    private static final String[] AEC_INFO_TITLE = {" Lux "," Gain "," Sensitivity "," Exp Time "};
    private static final String[] AFD_INFO_TITLE = {" HNum "," VNum "," Visible bands "," RS time "," Antibanding mode "," Lines/Frame "," Avg Conf "," Avg Energy "};
    private static final String[] AF_INFO_TITLE = {" PD Enable "," PD Type "," Sparse HW "," DualPD HW "," LCR HW "," LCR SW "," Lens Pos "};
    private static final String[] AEC_IDS_INFO_TITLE = {" Request id "," Camera id "};
    private static final String[] STATS_EXTENSION_TITLE = {" RatioLongtoShort "," RatioLongtoSafe ",
            " RatioSafetoShort "," CompenADRCGain "," CompenDarkBoostGain "};
    private static final String[] STATS_NN_RESULT_TITLE = {" Width "," Height "," MapData "," NumROI "," ROIData "," ROIWeight "};
    public static final String[] PERFORMANCE_DEBUG_TITLE = {" Flush "," Close "," Open ", " Setup "," Configure ", " Preview ",
            " Snapshot ", " Shutter ", " Burst fps ", " Zoom ", " AF ", " AEC ", " AWB ", " Preview fps "};

    private CameraActivity mActivity;
    private View mRootView;
    private View mPreviewCover;
    private CaptureModule mModule;
    private final FrameLayout mCameraRootView;

    private final Object mSurfaceTextureLock = new Object();
    private SurfaceTexture mSurfaceTexture;
    private AutoFitTextureView mTextureView;
    private AutoFitSurfaceView mSurfaceView;
    private AutoFitSurfaceView mSurfaceViewMono;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceHolder mSurfaceHolderMono;
    private GLCameraPreview mGLSurfaceView = null;
    private int mOrientation;
    private int mFilterMenuStatus;
    private PreviewGestures mGestures;
    private boolean mUIhidden = false;
    private SettingsManager mSettingsManager;
    private TrackingFocusRenderer mTrackingFocusRenderer;
    private TouchTrackFocusRenderer mT2TFocusRenderer;
    private StateNNTrackFocusRenderer mStatsNNFocusRenderer;
    private ImageView mThumbnail;
    public final FilmstripLayout mFilmstripLayout;
    private final FilmstripBottomPanel mFilmstripBottomControls;
    private final FilmstripContentPanel mFilmstripPanel;
    private Camera2FaceView mFaceView;
    private Point mDisplaySize = new Point();
    private SelfieFlashView mSelfieView;
    private float mScreenBrightness = 0.0f;
    private ProgressBar mProgressBar;
    private boolean mZoomRatioSupport = false;
    private int[] mScreenHDRIcon = {R.drawable.ic_hdr_off, R.drawable.ic_hdr};
    private int mScreenHDRindex;
    private SeekBar mEvSeekBar;
    private SeekBar mFlashLevelBar;
    private boolean isEvChanging;
    private int mCurrentProgress;
    private int mTotalProgress;
    private AFView mAFViewRender;
    private RelativeLayout mTorchLayout;
    private TextView mTorchLevel;
    private TextView mTorchReadText;
    private TextView mTorchBarLevel;
    private TextView mTorchLevelApply;
    private TextView mTorchCloseText;
    private TextView mTorchOpenText;
    private TextView mLowLightText;
    private VerticalSeekBar mTorchbar;
    private VerticalSeekBar mVerticalEvBar;
    private VerticalSeekBar mAICameraSeekBar;

    private boolean mIsTorchOn;
    private int mTorchLen ;
    private int mTorchSection ;
    private TextView mEvValue;

    private FocusAssistImageView mFAImageView;
    private RotateTextView mFocusAssistTextView;
    private ViewStub mFAViewStub;
    private FocusAssistLayout mFALayout;
    private TextureView mFATextureView;

    private SurfaceHolder.Callback callbackMono = new SurfaceHolder.Callback() {
        // SurfaceHolder callbacks
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG,"width="+width+",height="+height);
            mSurfaceHolderMono = holder;
            if(mMonoDummyOutputAllocation != null) {
                mMonoDummyOutputAllocation.setSurface(mSurfaceHolderMono.getSurface());
            }
        }
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    private class PhysicalCallBack implements SurfaceHolder.Callback{
        private int mIndex;

        PhysicalCallBack(int index){
            this.mIndex = index;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG,"PhysicalCallback "+mIndex+" surfaceChanged width="+width+" height="+height);
        }
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mPhysicalHolders[mIndex] = holder;
            mSurfaceReady[mIndex] = true;
            checkSurfaceReady();
            Log.d(TAG,"PhysicalCallback surfaceCreated " + mIndex);
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPhysicalHolders[mIndex] = null;
            mSurfaceReady[mIndex] = false;
            Log.d(TAG,"PhysicalCallback surfaceDestroyed"+ mIndex);
        }
    }

    private class PhysicalSurfaceTextureListener implements TextureView.SurfaceTextureListener,
            View.OnLayoutChangeListener {

        private final int mIndex;
        private boolean mUpdateBufferSizeOnNextSurfaceTextureUpdate = false;

        public PhysicalSurfaceTextureListener(int index) {
            mIndex = index;
        }

        private void updateBufferSize(SurfaceTexture surfaceTexture) {
            if (mIndex != 0) {
                surfaceTexture.setDefaultBufferSize(mPhysicalPreviewSizes.get(mIndex).getWidth(), mPhysicalPreviewSizes.get(mIndex).getHeight());
            } else {
                surfaceTexture.setDefaultBufferSize(mLogicalPreviewSize.getWidth(), mLogicalPreviewSize.getHeight());
            }
        }

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "PhysicalSurfaceTextureListener::onSurfaceTextureAvailable width =" + width + ", height = " + height + ", mIndex " + mIndex);
            updateBufferSize(surfaceTexture);
            mPhysicalTextureViewReady.put(mIndex, true);
            checkSurfaceReady();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "PhysicalSurfaceTextureListener::onSurfaceTextureSizeChanged width =" + width + ", height = " + height + ", mIndex " + mIndex);
            updateBufferSize(surfaceTexture);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            Log.i(TAG, "PhysicalSurfaceTextureListener::onSurfaceTextureDestroyed, mIndex " + mIndex);
            mPhysicalTextureViewReady.put(mIndex, false);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            if (mUpdateBufferSizeOnNextSurfaceTextureUpdate) {
                mUpdateBufferSizeOnNextSurfaceTextureUpdate = false;
                Log.i(TAG, "PhysicalSurfaceTextureListener::onSurfaceTextureUpdated, mIndex " + mIndex);
                updateBufferSize(surfaceTexture);
                mPhysicalTextureViewReady.put(mIndex, true);
                checkSurfaceReady();
            }
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            mUpdateBufferSizeOnNextSurfaceTextureUpdate = true;
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable width =" + width + ", height = " + height);
            synchronized (mSurfaceTextureLock) {
                mSurfaceTexture = surfaceTexture;
                mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
            }
            previewUIReady();
            setSurfaceDim();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged: width =" + width + ", height = " + height);
            synchronized (mSurfaceTextureLock) {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                }
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            Log.i(TAG, "onSurfaceTextureDestroyed");
            synchronized (mSurfaceTextureLock) {
                mSurfaceTexture = null;
            }
            if (mDeepZoomModeRect != null) {
                mDeepZoomModeRect.setVisibility(View.GONE);
            }
            previewUIDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            if (mUpdateBufferSizeOnNextSurfaceTextureUpdate) {
                Log.i(TAG, "onSurfaceTextureUpdated update buffer size " + mPreviewWidth+ " " + mPreviewHeight);
                mUpdateBufferSizeOnNextSurfaceTextureUpdate = false;
                previewUIReady();
                synchronized (mSurfaceTextureLock) {
                    mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                }
            }
        }
    };

    private void setSurfaceDim() {
        int left;
        int top;
        int right;
        int bottom;
        if (USE_TEXTURE_VIEW_TO_PREVIEW) {
            left = mTextureView.getLeft();
            top = mTextureView.getTop();
            right = mTextureView.getRight();
            bottom = mTextureView.getBottom();
        } else {
            left = mSurfaceView.getLeft();
            top = mSurfaceView.getTop();
            right = mSurfaceView.getRight();
            bottom = mSurfaceView.getBottom();
        }
        if(mTrackingFocusRenderer != null && mTrackingFocusRenderer.isVisible()) {
            mTrackingFocusRenderer.setSurfaceDim(left, top, right, bottom);
        }
        if(mT2TFocusRenderer != null && mT2TFocusRenderer.isShown()) {
            mT2TFocusRenderer.setSurfaceDim(left, top, right, bottom);
        }
        if(mStatsNNFocusRenderer != null && mStatsNNFocusRenderer.isShown()) {
            mStatsNNFocusRenderer.setSurfaceDim(left, top, right, bottom);
        }
        if(mAFViewRender != null && mAFViewRender.isShown()) {
            mAFViewRender.setSurfaceDim(left, top, right, bottom);
        }
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        // SurfaceHolder callbacks
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "surfaceChanged:size=" + width + "x" + height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated");
            mSurfaceHolder = holder;
            previewUIReady();
            setSurfaceDim();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.v(TAG, "surfaceDestroyed");
            mSurfaceHolder = null;
            if (mDeepZoomModeRect != null) {
                mDeepZoomModeRect.setVisibility(View.GONE);
            }
            previewUIDestroyed();
        }
    };

    private ShutterButton mShutterButton;
    private ImageView mVideoButton;
    private RenderOverlay mRenderOverlay;
    private FlashToggleButton mFlashButton;
    private CountDownView mCountDownView;
    private OneUICameraControls mCameraControls;
    private MenuHelp mMenuHelp;
    private PieRenderer mPieRenderer;
    private ZoomRenderer mZoomRenderer;
    private Allocation mMonoDummyAllocation;
    private Allocation mMonoDummyOutputAllocation;
    private boolean mIsMonoDummyAllocationEverUsed = false;
    private boolean mIsTouchAF = false;

    private Point mFocusPoint = new Point();
    private Point mFocusPointInPreview = new Point();

    private int mScreenRatio = CameraUtil.RATIO_UNKNOWN;
    private int mTopMargin = 0;
    private int mBottomMargin = 0;
    private ViewGroup mFilterLayout;
    private float mZoomFixedValue = 1.0f;
    private float mZoomBarRatio = 0.2f;
    private float mZoomMaxValue = 10.0f;

    private View mFilterModeSwitcher;
    private View mSceneModeSwitcher;
    private ImageView mSceneModeHDR;
    private View mFrontBackSwitcher;
    private ImageView mMakeupButton;
    private SeekBar mMakeupSeekBar;
    private SeekBar mDeepportraitSeekBar;
    private SeekBar mZoomSeekBar;
    private View mMakeupSeekBarLayout;
    private View mSeekbarBody;
    private TextView mMFNRSwitch;
    private SeekBar mMfnrSeekBar;
    private TextView mMFNRText;
    private TextView mRecordingTimeView;
    private View mTimeLapseLabel;
    private RotateLayout mRecordingTimeRect;
    private PauseButton mPauseButton;
    private RotateImageView mMuteButton;
    private ImageView mSeekbarToggleButton;
    private RotateLayout mSceneModeLabelRect;
    private LinearLayout mSceneModeLabelView;
    private TextView mSceneModeName;
    private ImageView mExitBestMode;
    private RotateLayout mDeepZoomModeRect;
    private TextView mDeepzoomSetName;
    private int mDeepZoomIndex = 0;
    private float mDeepZoomValue = 1.0f;
    private ImageView mSettingsIcon;
    private ArrayList<TextView> mCameraModeTexts = new ArrayList<>();
    private RecyclerView mModeSelectLayout;
    private Camera2ModeAdapter mCameraModeAdapter;

    private ImageView mSceneModeLabelCloseIcon;
    private AlertDialog  mSceneModeInstructionalDialog = null;

    private ImageView mCancelButton;
    private View mReviewCancelButton;
    private View mReviewDoneButton;
    private View mReviewRetakeButton;
    private View mReviewPlayButton;
    private FrameLayout mPreviewLayout;
    private ImageView mReviewImage;
    private int mDownSampleFactor = 4;
    private DecodeImageForReview mDecodeTaskForReview = null;

    private View mStatsAwbInfo;
    private TextView mStatsAwbText;
    private TextView mZoomValueText;

    private View mStatsAecInfo;
    private TextView mStatsAecText;

    private View mStatsAfdInfo;
    private TextView mStatsAfdText;

    private View mStatsAfInfo;
    private TextView mStatsAfText;

    private View mStatsAecIdsInfo;
    private TextView mStatsAecIdsText;

    private View mStatsNNResult;
    private TextView mStatsNNResultText;

    private View mPerformancDebugInfo;
    private TextView mDebugPerformancText;

    private LinearLayout mZoomLinearLayout;
    private RelativeLayout mManualFlashLayout;
    private TextView flashLevelTxt;

    private int mZoomIndex = 0;

    private TextView mOfflineDumpTrigger;
    private int mOfflineDumpTriIndex = 0;
    private boolean mZoomIncrease = true;
    private boolean mShowFocusCircle = true;
    private boolean mFaceUpdated = false;

    private GridLayout mPhysicalPreviewContainer;

    private final SparseArray<TextureView> mPhysicalTextureViews = new SparseArray<>(CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT);
    private final SparseBooleanArray mPhysicalTextureViewReady = new SparseBooleanArray(CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT);

    private Size mLogicalPreviewSize;
    private SparseArray<Size> mPhysicalPreviewSizes = new SparseArray<>(CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT);

    private boolean[] mSurfaceReady = {false,false,false,false};
    private AutoFitSurfaceView[] mPhysicalViews = new AutoFitSurfaceView[CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];
    private SurfaceHolder[] mPhysicalHolders = new SurfaceHolder[CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];
    List<Surface> mPreviewSurfaces = new ArrayList<>();
    private int mPreviewCount = 0;
    int mPreviewWidth;
    int mPreviewHeight;
    private boolean mIsVideoUI = false;
    private boolean mIsSceneModeLabelClose = false;
    private LinearLayout mGridLineView;
    private boolean mIsZoomKeyChanged = false;

    private void showThumbnail() {
        if ((mIsVideoUI || mModule.getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL)
                && mThumbnail != null && mModule.getCurrentIntentMode() != CaptureModule.INTENT_MODE_STILL_IMAGE_CAMERA) {
            mThumbnail.setVisibility(View.INVISIBLE);
            mThumbnail = null;
            mActivity.updateThumbnail(mThumbnail);
        } else if (!mIsVideoUI && (mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_NORMAL
                || mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_STILL_IMAGE_CAMERA)) {
            if (mThumbnail == null)
                mThumbnail = (ImageView) mRootView.findViewById(R.id.preview_thumb);
            mActivity.updateThumbnail(mThumbnail);
        }
    }

    private void previewUIReady() {
        if (mSettingsManager.getPhysicalCameraId() == null &&
                mSettingsManager.getSinglePhysicalCamera() == null) {
            mModule.onPreviewUIReady();
        } else {
            checkSurfaceReady();
        }

    }

    private void checkSurfaceReady(){
        String physical_id = mSettingsManager.getSinglePhysicalCamera();
        if (physical_id != null) {
            mPreviewCount = mSettingsManager.getAllPhysicalCameraId().size()+1;
        } else {
            mPreviewCount = mSettingsManager.getPhysicalCameraId().size()+1;
        }

        Log.i(TAG, "mPreviewCount " + mPreviewCount);

        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            Log.d(TAG,"checkSurfaceReady SurfaceReady="+ Arrays.toString(mSurfaceReady));
            for (int i = 0; i< mPreviewCount; i++){
                if (!mSurfaceReady[i])
                    return;
            }
            if(physical_id != null && !mSurfaceHolder.getSurface().isValid()){
                return;
            }
        } else {
            Log.d(TAG,"checkSurfaceReady SurfaceReady="+ mPhysicalTextureViewReady);
            for (int i = 0; i< mPreviewCount; i++){
                if (!mPhysicalTextureViewReady.get(i))
                    return;
            }
            if(physical_id != null && !mTextureView.isAvailable()){
                return;
            }
        }
        mModule.onPreviewUIReady();
    }

    public void initThumbnail() {
        if (mThumbnail == null)
            mThumbnail = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mActivity.updateThumbnail(mThumbnail);
    }

    private void previewUIDestroyed() {
        mModule.onPreviewUIDestroyed();
    }

    public TrackingFocusRenderer getTrackingFocusRenderer() {
        return mTrackingFocusRenderer;
    }

    public TouchTrackFocusRenderer getT2TFocusRenderer() {
        return mT2TFocusRenderer;
    }

    public StateNNTrackFocusRenderer getStatsNNFocusRenderer() {
        return mStatsNNFocusRenderer;
    }

    public void updateT2TCameraBound(Rect cameraBound) {
        float zoomValue = mModule.getZoomValue();
        if(getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV()) {
            zoomValue = 1.0f;
        }
        mT2TFocusRenderer.setZoom(zoomValue);
    }

    public void updateStatsNNCameraBound(Rect cameraBound) {
        float zoomValue = mModule.getZoomValue();
        if(getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV()) {
            zoomValue = 1.0f;
        }
        mStatsNNFocusRenderer.setZoom(zoomValue);
    }

    public AFView getAFRenderer() {
        return mAFViewRender;
    }

    public void updateAFBound(Rect cameraBound) {
        float zoomValue = mModule.getZoomValue();
        if(getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV()) {
            zoomValue = 1.0f;
        }
        mAFViewRender.setZoom(zoomValue);
    }

    public Point getDisplaySize() {
        return mDisplaySize;
    }

    public static final boolean USE_TEXTURE_VIEW_TO_PREVIEW = PersistUtil.useTextureViewToPreview();

    private void initPreviewContentView() {
        Log.d(TAG, "initPreviewContentView");
        ViewStub stub = mRootView.findViewById(R.id.preview_view_stub);
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            stub.setLayoutResource(R.layout.preview_surface_views);
            stub.inflate();
            // display the view
            mSurfaceView = mRootView.findViewById(R.id.mdp_preview_content);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(mSurfaceHolderCallback);
            mSurfaceView.addOnLayoutChangeListener(mOnPreviewLayoutChangeListener);

            mSurfaceViewMono = mRootView.findViewById(R.id.mdp_preview_content_mono);
            mSurfaceViewMono.setZOrderMediaOverlay(true);
            mSurfaceHolderMono = mSurfaceViewMono.getHolder();
            mSurfaceHolderMono.addCallback(callbackMono);
        } else {
            stub.setLayoutResource(R.layout.preview_texture_views);
            stub.inflate();
            mTextureView = mRootView.findViewById(R.id.preview_texture_view);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            mTextureView.addOnLayoutChangeListener(mOnPreviewLayoutChangeListener);
        }
    }

    private boolean mUpdateBufferSizeOnNextSurfaceTextureUpdate = false;

    private final View.OnLayoutChangeListener mOnPreviewLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Log.d(TAG, "onLayoutChange, " + left + " " + top + " " + right + " " + bottom + ", " +
                    oldLeft + " " + oldTop + " " + oldRight + " " + oldBottom);
            int width = right - left;
            int height = bottom - top;
            if (USE_TEXTURE_VIEW_TO_PREVIEW) {
                mUpdateBufferSizeOnNextSurfaceTextureUpdate = true;
                synchronized (mSurfaceTextureLock) {
                    if (mSurfaceTexture != null) {
                        mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                    }
                }
            }
            if (mFaceView != null) {
                mFaceView.onSurfaceTextureSizeChanged(width, height);
            }
            if (mStatsNNFocusRenderer != null) {
                mStatsNNFocusRenderer.onSurfaceTextureSizeChanged(width, height);
            }
            if (mT2TFocusRenderer != null) {
                mT2TFocusRenderer.onSurfaceTextureSizeChanged(width, height);
            }
            if (mAFViewRender != null) {
                mAFViewRender.onSurfaceTextureSizeChanged(width, height);
            }
        }
    };

    public boolean isPreviewSurfaceValid() {
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            return mSurfaceHolder != null && mSurfaceHolder.getSurface().isValid();
        } else {
            return false;
        }
    }

    private void initPreviewContentViewForPhysicalCamera() {
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mPhysicalPreviewContainer = mRootView.findViewById(R.id.grid_preview);
            mPhysicalViews[0] = mRootView.findViewById(R.id.mdp_preview_physical_0);
            mPhysicalHolders[0] = mPhysicalViews[0].getHolder();
            mPhysicalHolders[0].addCallback(new PhysicalCallBack(0));
            mPhysicalViews[1] = mRootView.findViewById(R.id.mdp_preview_physical_1);
            mPhysicalHolders[1] = mPhysicalViews[1].getHolder();
            mPhysicalHolders[1].addCallback(new PhysicalCallBack(1));
            mPhysicalViews[2] = mRootView.findViewById(R.id.mdp_preview_physical_2);
            mPhysicalHolders[2] = mPhysicalViews[2].getHolder();
            mPhysicalHolders[2].addCallback(new PhysicalCallBack(2));
            mPhysicalViews[3] = mRootView.findViewById(R.id.mdp_preview_physical_3);
            mPhysicalHolders[3] = mPhysicalViews[3].getHolder();
            mPhysicalHolders[3].addCallback(new PhysicalCallBack(3));
        } else {
            mPhysicalPreviewContainer = mRootView.findViewById(R.id.grid_preview_texture_views);
            mPhysicalTextureViews.put(0, mRootView.findViewById(R.id.physical_preview_texture_view_0));
            PhysicalSurfaceTextureListener physicalSurfaceTextureListener_0 = new PhysicalSurfaceTextureListener(0);
            mPhysicalTextureViews.get(0).setSurfaceTextureListener(physicalSurfaceTextureListener_0);
            mPhysicalTextureViews.get(0).addOnLayoutChangeListener(physicalSurfaceTextureListener_0);
            mPhysicalTextureViews.put(1, mRootView.findViewById(R.id.physical_preview_texture_view_1));
            PhysicalSurfaceTextureListener physicalSurfaceTextureListener_1 = new PhysicalSurfaceTextureListener(1);
            mPhysicalTextureViews.get(1).setSurfaceTextureListener(physicalSurfaceTextureListener_1);
            mPhysicalTextureViews.get(1).addOnLayoutChangeListener(physicalSurfaceTextureListener_1);
            mPhysicalTextureViews.put(2, mRootView.findViewById(R.id.physical_preview_texture_view_2));
            PhysicalSurfaceTextureListener physicalSurfaceTextureListener_2 = new PhysicalSurfaceTextureListener(2);
            mPhysicalTextureViews.get(2).setSurfaceTextureListener(physicalSurfaceTextureListener_2);
            mPhysicalTextureViews.get(2).addOnLayoutChangeListener(physicalSurfaceTextureListener_2);
            mPhysicalTextureViews.put(3, mRootView.findViewById(R.id.physical_preview_texture_view_3));
            PhysicalSurfaceTextureListener physicalSurfaceTextureListener_3 = new PhysicalSurfaceTextureListener(3);
            mPhysicalTextureViews.get(3).setSurfaceTextureListener(physicalSurfaceTextureListener_3);
            mPhysicalTextureViews.get(3).addOnLayoutChangeListener(physicalSurfaceTextureListener_3);
        }
    }


    public CaptureUI(CameraActivity activity, final CaptureModule module, FrameLayout rootView, View parent) {
        mActivity = activity;
        mModule = module;
        mRootView = parent;
        mCameraRootView = rootView;
        mSettingsManager = SettingsManager.getInstance();
        mSettingsManager.registerListener(this);
        mActivity.getLayoutInflater().inflate(R.layout.capture_module,
                (ViewGroup) mRootView, true);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);

        initPreviewContentView();

        mGridLineView = (LinearLayout) mRootView.findViewById(R.id.grid_line);

        initPreviewContentViewForPhysicalCamera();

        mProgressBar = (ProgressBar) mRootView.findViewById(R.id.progress_bar);
        mRenderOverlay = (RenderOverlay) mRootView.findViewById(R.id.render_overlay);
        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mVideoButton = (ImageView) mRootView.findViewById(R.id.video_button);
        mExitBestMode = (ImageView) mRootView.findViewById(R.id.exit_best_mode);
        mFilterModeSwitcher = mRootView.findViewById(R.id.filter_mode_switcher);
        mSceneModeSwitcher = mRootView.findViewById(R.id.scene_mode_switcher);
        mSceneModeHDR = (ImageView)mRootView.findViewById(R.id.scene_mode_hdr);
        mFrontBackSwitcher = mRootView.findViewById(R.id.front_back_switcher);
        mMakeupButton = (ImageView) mRootView.findViewById(R.id.ts_makeup_switcher);
        mMakeupSeekBarLayout = mRootView.findViewById(R.id.makeup_seekbar_layout);
        mSeekbarBody = mRootView.findViewById(R.id.seekbar_body);
        mSeekbarToggleButton = (ImageView) mRootView.findViewById(R.id.seekbar_toggle);
        mSceneModeSwitcher.setVisibility(View.GONE);
        mSurfaceView.setActivity(mActivity);
        for (int i = 0; i < CaptureModule.MAX_LOGICAL_PHYSICAL_CAMERA_COUNT; i++) {
            mPhysicalViews[i].setActivity(mActivity);
        }
        mSeekbarToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSeekbarBody.getVisibility() == View.VISIBLE) {
                    mSeekbarBody.setVisibility(View.GONE);
                    mSeekbarToggleButton.setImageResource(R.drawable.seekbar_show);
                } else {
                    mSeekbarBody.setVisibility(View.VISIBLE);
                    mSeekbarToggleButton.setImageResource(R.drawable.seekbar_hide);
                }
            }
        });
        mMakeupSeekBar = (SeekBar)mRootView.findViewById(R.id.makeup_seekbar);
        mMakeupSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                if ( progresValue != 0 ) {
                    int value = 10 + 9 * progresValue / 10;
                    mSettingsManager.setValue(SettingsManager.KEY_MAKEUP, value + "");
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mDeepportraitSeekBar = (SeekBar)mRootView.findViewById(R.id.deepportrait_seekbar);
        mDeepportraitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                 if (mModule.getCamGLRender() != null) {
                     module.getCamGLRender().setBlurLevel(progress);
                 }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                final SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(mActivity);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(SettingsManager.KEY_DEEPPORTRAIT_VALUE, seekBar.getProgress());
                editor.commit();
            }
        });
        mMakeupButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (module != null && !module.isAllSessionClosed()) {
                    toggleMakeup();
                    updateMenus();
                }
            }
        });
        setMakeupButtonIcon();
        initZoomSeekBar();
        initAICameraSeekBar();
        if(PersistUtil.showVerticalEvBar()) {
            initVerticalEvBar();
        }
        mFlashButton = (FlashToggleButton) mRootView.findViewById(R.id.flash_button);
        mModeSelectLayout = (RecyclerView) mRootView.findViewById(R.id.mode_select_layout);
        mModeSelectLayout.setLayoutManager(new LinearLayoutManager(mActivity,
                LinearLayoutManager.HORIZONTAL, false));
        mCameraModeAdapter = new Camera2ModeAdapter(mModule.getCameraModeList());
        mCameraModeAdapter.setOnItemClickListener(mModule.getModeItemClickListener());
        mModeSelectLayout.setAdapter(mCameraModeAdapter);
        mSettingsIcon = (ImageView) mRootView.findViewById(R.id.settings);
        mSettingsIcon.setImageResource(R.drawable.settings);
        mSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettingsMenu();
            }
        });

        initFilterModeButton();
        initSceneModeHDR();
        initCameraSwitcher();
        initFlashButton();
        updateMenus();

        mRecordingTimeView = (TextView) mRootView.findViewById(R.id.recording_time);
        mRecordingTimeRect = (RotateLayout) mRootView.findViewById(R.id.recording_time_rect);
        mTimeLapseLabel = mRootView.findViewById(R.id.time_lapse_label);
        mPauseButton = (PauseButton) mRootView.findViewById(R.id.video_pause);
        mPauseButton.setOnPauseButtonListener(this);

        mStatsAwbInfo = mRootView.findViewById(R.id.stats_awb_info);
        mStatsAwbText = mRootView.findViewById(R.id.stats_awb_text);

        mStatsAecInfo = mRootView.findViewById(R.id.stats_aec_info);
        mStatsAecText = mRootView.findViewById(R.id.stats_aec_text);

        mStatsAfdInfo = mRootView.findViewById(R.id.stats_afd_info);
        mStatsAfdText = mRootView.findViewById(R.id.stats_afd_text);

        mStatsAfInfo = mRootView.findViewById(R.id.stats_af_info);
        mStatsAfText = mRootView.findViewById(R.id.stats_af_text);

        mStatsAecIdsInfo = mRootView.findViewById(R.id.stats_camera_id_info);
        mStatsAecIdsText = mRootView.findViewById(R.id.stats_camera_id_text);

        mPerformancDebugInfo = mRootView.findViewById(R.id.debug_performance_info);
        mDebugPerformancText = mRootView.findViewById(R.id.debug_performance_text);

        mStatsNNResult = mRootView.findViewById(R.id.stats_nn_result_info);
        mStatsNNResultText= mRootView.findViewById(R.id.stats_nn_result_text);
        mLowLightText = mRootView.findViewById(R.id.lowlightboost_text);

        mMuteButton = (RotateImageView)mRootView.findViewById(R.id.mute_button);
        mMuteButton.setVisibility(View.VISIBLE);
        setMuteButtonResource(!mModule.isAudioMute());
        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isEnabled = !mModule.isAudioMute();
                mModule.setMute(isEnabled, true);
                setMuteButtonResource(!isEnabled);
            }
        });

        mFilmstripLayout = (FilmstripLayout) rootView.findViewById(R.id.filmstrip_layout);
        mFilmstripBottomControls = new FilmstripBottomPanel(
                (ViewGroup) rootView.findViewById(R.id.filmstrip_bottom_panel));
        mFilmstripPanel = (FilmstripContentPanel) rootView.findViewById(R.id.filmstrip_layout);
        mExitBestMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsManager.getInstance().setValueIndex(SettingsManager.KEY_SCENE_MODE,
                        AUTOMATIC_MODE);
            }
        });

        RotateImageView muteButton = (RotateImageView) mRootView.findViewById(R.id.mute_button);
        muteButton.setVisibility(View.GONE);

        mDeepZoomModeRect = (RotateLayout)mRootView.findViewById(R.id.deepzoom_set_layout);
        mDeepzoomSetName = (TextView)mRootView.findViewById(R.id.deepzoom_set);
        mDeepzoomSetName.setText("Zoom OFF");
        mDeepzoomSetName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeepZoomIndex = (mDeepZoomIndex + 1) % 3;
                updateDeepZoomIndex();
            }
        });
        mSceneModeLabelRect = (RotateLayout)mRootView.findViewById(R.id.scene_mode_label_rect);
        mSceneModeName = (TextView)mRootView.findViewById(R.id.scene_mode_label);
        mSceneModeLabelCloseIcon = (ImageView)mRootView.findViewById(R.id.scene_mode_label_close);
        mSceneModeLabelCloseIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsSceneModeLabelClose = true;
                mSceneModeLabelRect.setVisibility(View.GONE);
            }
        });

        mCameraControls = (OneUICameraControls) mRootView.findViewById(R.id.camera_controls);
        mFaceView = (Camera2FaceView) mRootView.findViewById(R.id.face_view);
        mFaceView.initMode();

        //Touch track focus
        mT2TFocusRenderer = (TouchTrackFocusRenderer) mRootView.findViewById(R.id.touch_track_focus);
        mT2TFocusRenderer.init(mActivity, mModule, this);
        if (mModule.isT2TFocusSettingOn()) {
            mT2TFocusRenderer.setVisible(true);
        } else {
            mT2TFocusRenderer.setVisible(false);
        }
        mStatsNNFocusRenderer = (StateNNTrackFocusRenderer) mRootView.findViewById(R.id.statsnn_track_focus);
        mStatsNNFocusRenderer.init(mActivity, mModule, this);
        if (mModule.isSateNNFocusSettingOn()) {
            mStatsNNFocusRenderer.setVisible(true);
        } else {
            mStatsNNFocusRenderer.setVisible(false);
        }
        mAFViewRender = (AFView) mRootView.findViewById(R.id.af_view);
        mAFViewRender.init(mActivity, mModule, this);
        if (mModule.isSateAFSettingOn()) {
            mAFViewRender.setVisible(true);
        } else {
            mAFViewRender.setVisible(false);
        }
        mOfflineDumpTrigger = (TextView)mRootView.findViewById(R.id.offline_dump_trigger);
        updateOfflineDumpTrigger(View.GONE);
        mOfflineDumpTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] entries = mActivity.getResources().getStringArray(
                        R.array.camera2_offline_dump_trigger_entries);
                String[] values = mActivity.getResources().getStringArray(
                        R.array.camera2_offline_dump_trigger_entryvalues);
                mOfflineDumpTriIndex = mOfflineDumpTriIndex + 1;
                if (mOfflineDumpTriIndex > values.length -1)
                    mOfflineDumpTriIndex = 0;
                int trigger = Integer.valueOf(values[mOfflineDumpTriIndex]);
                module.updateOfflineDumpTriggerStatus(trigger);
                mOfflineDumpTrigger.setText(entries[mOfflineDumpTriIndex]);
            }
        });

        mCancelButton = (ImageView) mRootView.findViewById(R.id.cancel_button);
        final int intentMode = mModule.getCurrentIntentMode();
        if (intentMode != CaptureModule.INTENT_MODE_NORMAL) {
            mModeSelectLayout.setVisibility(View.GONE);
            mCameraControls.setIntentMode(intentMode);
            mCameraControls.setVideoMode(false);
            if(intentMode != CaptureModule.INTENT_MODE_STILL_IMAGE_CAMERA){
                mCancelButton.setVisibility(View.VISIBLE);
            }
            mReviewCancelButton = mRootView.findViewById(R.id.preview_btn_cancel);
            mReviewDoneButton = mRootView.findViewById(R.id.done_button);
            mReviewRetakeButton = mRootView.findViewById(R.id.preview_btn_retake);
            mReviewPlayButton = mRootView.findViewById(R.id.preview_play);
            mPreviewLayout = (FrameLayout)mRootView.findViewById(R.id.preview_of_intent);
            mReviewImage = (ImageView)mRootView.findViewById(R.id.preview_content);
            mReviewCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mActivity.setResultEx(Activity.RESULT_CANCELED, new Intent());
                    mActivity.finish();
                }
            });
            mReviewRetakeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPreviewLayout.setVisibility(View.GONE);
                    mReviewImage.setImageBitmap(null);
                    mModule.setJpegImageData(null);
                    if (intentMode == CaptureModule.INTENT_MODE_VIDEO) {
                        mModule.onRetakeVideo();
                    }
                }
            });
            mReviewDoneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (intentMode == CaptureModule.INTENT_MODE_CAPTURE || intentMode == CaptureModule.INTENT_MODE_CAPTURE_SECURE) {
                        mModule.onCaptureDone();
                    } else if (intentMode == CaptureModule.INTENT_MODE_VIDEO) {
                        mModule.onRecordingDone(true);
                    }
                }
            });
            mReviewPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mModule.startPlayVideoActivity();
                }
            });
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mModule.cancelCapture();
                }
            });
        }

        mActivity.getWindowManager().getDefaultDisplay().getSize(mDisplaySize);
        mScreenRatio = CameraUtil.determineRatio(mDisplaySize.x, mDisplaySize.y);
        if (mScreenRatio == CameraUtil.RATIO_16_9) {
            int l = mDisplaySize.x > mDisplaySize.y ? mDisplaySize.x : mDisplaySize.y;
            int tm = mActivity.getResources().getDimensionPixelSize(R.dimen.preview_top_margin);
            int bm = mActivity.getResources().getDimensionPixelSize(R.dimen.preview_bottom_margin);
            mTopMargin = l / 4 * tm / (tm + bm);
            mBottomMargin = l / 4 - mTopMargin;
        }

        if (mPieRenderer == null) {
            mPieRenderer = new PieRenderer(mActivity);
            mRenderOverlay.addRenderer(mPieRenderer);
            mPieRenderer.setCapureUi(this);
        }

        if (mZoomRenderer == null) {
            mZoomRenderer = new ZoomRenderer(mActivity);
            mRenderOverlay.addRenderer(mZoomRenderer);
        }

        if(mTrackingFocusRenderer == null) {
            mTrackingFocusRenderer = new TrackingFocusRenderer(mActivity, mModule, this);
            mRenderOverlay.addRenderer(mTrackingFocusRenderer);
        }
        if(mModule.isTrackingFocusSettingOn()) {
            mTrackingFocusRenderer.setVisible(true);
        } else {
            mTrackingFocusRenderer.setVisible(false);
        }

        if (mGestures == null) {
            // this will handle gesture disambiguation and dispatching
            mGestures = new PreviewGestures(mActivity, this, mZoomRenderer, mPieRenderer,
                    mTrackingFocusRenderer);
            mRenderOverlay.setGestures(mGestures);
        }

        mGestures.setRenderOverlay(mRenderOverlay);
        mRenderOverlay.requestLayout();

        mActivity.setPreviewGestures(mGestures);
        mRecordingTimeRect.setVisibility(View.GONE);
        showFirstTimeHelp();
    }

    private void initAICameraSeekBar(){
        mAICameraSeekBar = (VerticalSeekBar) mRootView.findViewById(R.id.aicamera_seekbar);
        mAICameraSeekBar.setProgress(100);
        mModule.updateAIStrengthValue(128);
        mAICameraSeekBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(VerticalSeekBar seekBar, int progresValue, boolean fromUser) {
                int value = (int)(128*progresValue/100);
                mModule.updateAIStrengthValue(value);
            }
        });
    }
    public void hidenMFNRtext(){
        if(mMFNRText != null) mMFNRText.setVisibility(View.INVISIBLE);
        if(mMfnrSeekBar != null) mMfnrSeekBar.setVisibility(View.INVISIBLE);
        if (mMFNRSwitch != null) mMFNRSwitch.setVisibility(View.INVISIBLE);
    }
    public void showMFNRtext(){
        if (mMFNRSwitch != null) mMFNRSwitch.setVisibility(View.VISIBLE);
        if (mSettingsManager.isMFNREnabled()) {
            if (mMFNRText != null) mMFNRText.setVisibility(View.VISIBLE);
            if (mMfnrSeekBar != null) mMfnrSeekBar.setVisibility(View.VISIBLE);
        }else{
            if (mMFNRText != null) mMFNRText.setVisibility(View.INVISIBLE);
            if (mMfnrSeekBar != null) mMfnrSeekBar.setVisibility(View.INVISIBLE);
        }

    }
    public void initMfnrSeekBar() {
        mMFNRSwitch = (TextView) mRootView.findViewById(R.id.mfnr_switch);
        mMFNRText = (TextView) mRootView.findViewById(R.id.mfnr_text);
        mMfnrSeekBar = (SeekBar) mRootView.findViewById(R.id.mfnr_seekbar);
        mMfnrSeekBar = (SeekBar) mRootView.findViewById(R.id.mfnr_seekbar);
        int minFrame = 3;
        int maxFrame = 8;
        int[] frameRange = mSettingsManager.getMFNRFrameRange(mModule.getMainCameraId());
        if (frameRange != null && frameRange[0] != 0 && frameRange[1] != 0) {
            minFrame = frameRange[0];
            maxFrame = frameRange[1];
        }
        if (mSettingsManager.isMFNREnabled()) {
            mMFNRSwitch.setText("ON");
        } else {
            mMFNRSwitch.setText("OFF");
        }
        mMFNRSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSettingsManager.isMFNREnabled()) {
                    mSettingsManager.setValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE, "0");
                    mMFNRSwitch.setText("OFF");
                    mMfnrSeekBar.setVisibility(View.INVISIBLE);
                    mMFNRText.setVisibility(View.INVISIBLE);
                } else {
                    mSettingsManager.setValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE, "1");
                    mMFNRSwitch.setText("ON");
                    mMfnrSeekBar.setVisibility(View.VISIBLE);
                    mMFNRText.setVisibility(View.VISIBLE);
                }
            }
        });
        final int length = maxFrame - minFrame + 1;
        final int min = minFrame;
        String mfnrFrame = mSettingsManager.getKeyValue(SettingsManager.KEY_CAPTURE_MFNR_FRAME);
        int frameValue = minFrame;
        frameValue = CameraUtil.strToInt(mfnrFrame,frameValue);
        final int section = 100 / length;
        int mProgress = (frameValue - minFrame) * section;
        mMfnrSeekBar.setProgress(mProgress);
        String frameStr = String.valueOf(frameValue);
        mMFNRText.setText(frameStr);
        mMfnrSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int index = progress / section;
                if (index > length - 1) index = length - 1;
                int value = index + min;
                String str = String.valueOf(value);
                mMFNRText.setText(str);
                mSettingsManager.setKeyValue(SettingsManager.KEY_CAPTURE_MFNR_FRAME, true, str);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isEvChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isEvChanging = false;
            }
        });
    }

    public void showTorchUI() {
        if(mTorchLayout == null) {
            mTorchLayout = (RelativeLayout) mRootView.findViewById(R.id.torch_mode_body);
            mTorchReadText = (TextView) mRootView.findViewById(R.id.torch_read_text);
            mTorchLevel = (TextView) mRootView.findViewById(R.id.torch_level_value);
            mTorchBarLevel = (TextView) mRootView.findViewById(R.id.torch_bar_text);
            mTorchLevelApply = (TextView) mRootView.findViewById(R.id.torch_level_apply);
            mTorchCloseText = (TextView) mRootView.findViewById(R.id.torch_close_text);
            mTorchOpenText = (TextView) mRootView.findViewById(R.id.torch_open_text);
            mTorchbar = (VerticalSeekBar) mRootView.findViewById(R.id.torch_verticalbar);
        }
        mTorchLayout.setVisibility(View.VISIBLE);
        mIsTorchOn =false;
        String currentId = String.valueOf(mModule.getMainCameraId());
        android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        openTorch(currentId,manager);
        mTorchOpenText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTorch(currentId,manager);
            }
        });
        mTorchReadText.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = 33)
            @Override
            public void onClick(View v) {
                try {
                    int torchStrength = manager.getTorchStrengthLevel(currentId);
                    mTorchLevel.setText(String.valueOf(torchStrength));
                } catch (CameraAccessException | IllegalStateException | IllegalArgumentException | NoSuchMethodError e) {
                    Log.e(TAG, "getTorchStrengthLevel  e=" + e);
                }
            }
        });
        mTorchCloseText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    manager.setTorchMode(currentId, false);
                    mIsTorchOn = false;
                }catch (CameraAccessException | IllegalStateException | IllegalArgumentException |NoSuchMethodError e){
                    Log.e(TAG, "setTorchMode e=" + e);
                }
            }
        });

        mTorchLevelApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyLevel = mSettingsManager.getKeyValue(SettingsManager.KEY_TORCH_VALUE);
                int currentValue = CameraUtil.strToInt(keyLevel, 3);
                try {
                    if (mIsTorchOn) {
                        int currentLevel = manager.getTorchStrengthLevel(currentId);
                        if (currentLevel != currentValue)
                            manager.turnOnTorchWithStrengthLevel(currentId, currentValue);
                    } else {
                        Toast.makeText(mActivity, "openTorch first", Toast.LENGTH_SHORT).show();
                    }
                } catch (CameraAccessException | IllegalStateException | IllegalArgumentException | NoSuchMethodError e) {
                    Log.e(TAG, "turnOnTorchWithStrengthLevel e=" + e);
                }
            }
        });

        mTorchbar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
                int index = progress / mTorchSection;
                if (index > mTorchLen - 1 ) index = mTorchLen - 1;
                String level = mSettingsManager.getKeyValue(SettingsManager.KEY_TORCH_VALUE);
                int currentLevel = CameraUtil.strToInt(level,3);
                if (currentLevel != index + 1) {
                    currentLevel = index + 1;
                    String str = String.valueOf(currentLevel);
                    mTorchBarLevel.setText(str);
                    mSettingsManager.setKeyValue(SettingsManager.KEY_TORCH_VALUE,true,str);
                }
            }
        });

    }
    private void openTorch(String currentId, android.hardware.camera2.CameraManager manager) {
        int maxLevel = 4;
        int defaultLevel = 3;
        int minLevel = 1;
        try {
            CameraCharacteristics pc = manager.getCameraCharacteristics(currentId);
            if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) != null) {
                defaultLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
            }
            if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) != null) {
                maxLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
            }
            manager.turnOnTorchWithStrengthLevel(currentId, defaultLevel);
            mIsTorchOn = true;
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException | NoSuchFieldError e) {
            Log.d(TAG, e.getMessage());
        }
        mTorchLen = maxLevel - minLevel + 1;
        mTorchSection = 100 / mTorchLen;
        int progress = mTorchSection * (defaultLevel - 1);
        if (progress > 100) progress = 100;
        mTorchbar.setProgress(progress);
        float scale = (float) progress / 100;
        mTorchbar.freshProgress(scale);
        String defalutStr = String.valueOf(defaultLevel);
        mTorchBarLevel.setText(defalutStr);
        mSettingsManager.setKeyValue(SettingsManager.KEY_TORCH_VALUE, true, defalutStr);
    }
    private void hideVerticalEv(){
        if (mEvValue != null && mEvValue.getVisibility() == View.VISIBLE)
            mEvValue.setVisibility(View.INVISIBLE);
        if (mVerticalEvBar != null && mVerticalEvBar.getVisibility() == View.VISIBLE){
            mVerticalEvBar.setVisibility(View.INVISIBLE);
            resetEv();
            mVerticalEvBar = null;
        }
    }
    private void initVerticalEvBar() {
        if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.PRO_MODE || mScreenHDRindex == 1) {
            hideVerticalEv();
            return;
        }
        final int length = mSettingsManager.getEntryValues(SettingsManager.KEY_EXPOSURE).length;
        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
        String value = mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE);
        if (mEvValue == null) mEvValue = (TextView) mRootView.findViewById(R.id.ev_value);
        mEvValue.setText(value);
        mEvValue.setVisibility(View.VISIBLE);
        final int section = 100 / length;
        if (mVerticalEvBar == null) {
            mVerticalEvBar = (VerticalSeekBar) mRootView.findViewById(R.id.ev_verticalbar);
            mVerticalEvBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
                    int index = progress / section;
                    if (index > length - 1) index = length - 1;
                    int currentIndex = mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
                    if (currentIndex != index) {
                        mSettingsManager.setValueIndex(SettingsManager.KEY_EXPOSURE, index);
                        String value = mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE);
                        mEvValue.setText(value);
                    }
                }
            });
        }
        mVerticalEvBar.setVisibility(View.VISIBLE);
        int progress = section * (index);
        if (progress > 100) progress = 100;
        mVerticalEvBar.setProgress(progress);
        float scale = (float) progress / 100;
        mVerticalEvBar.freshProgress(scale);
    }
    public boolean getIsEvChanging() {
        return isEvChanging;
    }
    private void resetEv() {
        String defaultEV = mActivity.getResources().getString(
                R.string.pref_exposure_default);
        mSettingsManager.setValue(SettingsManager.KEY_EXPOSURE, defaultEV);
        if(PersistUtil.showVerticalEvBar() && mVerticalEvBar != null) {
            initVerticalEvBar();
        }
    }
    public void updateFlashBar() {
        if(mManualFlashLayout == null){
            mManualFlashLayout= (RelativeLayout) mRootView.findViewById(R.id.manual_flash_layout);
        }
        if(!mSettingsManager.applyManualFlash()){
            mManualFlashLayout.setVisibility(View.INVISIBLE);
            return;
        }
        int maxLevel = mSettingsManager.getMaxFlashLevel();

        mManualFlashLayout.setVisibility(View.VISIBLE);
        int defaultValue = mSettingsManager.getDefaultFlashLevel();
        String keyvalue = mSettingsManager.getValue(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL);
        if(keyvalue == null){
            keyvalue = String.valueOf(defaultValue);
            mSettingsManager.setValue(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL,keyvalue);
        }
        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL);
        if(flashLevelTxt == null) {
            flashLevelTxt = (TextView) mRootView.findViewById(R.id.flash_text);
        }
        flashLevelTxt.setText(keyvalue);
        final int section = 100/maxLevel;
        if (section == 0){
            return;
        }
        if (mFlashLevelBar == null) {
            mFlashLevelBar = (SeekBar) mRootView.findViewById(R.id.flash_seekbar);
            mFlashLevelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int mindex = progress / section;
                    if (mindex > maxLevel - 1) mindex = maxLevel - 1;
                    int currentIndex = mSettingsManager.getValueIndex(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL);
                    if (currentIndex != mindex) {
                        mSettingsManager.setValueIndex(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL, mindex);
                        String value = mSettingsManager.getValue(SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL);
                        flashLevelTxt.setText(value);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isEvChanging = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isEvChanging = false;
                }
            });
        }
        setEvBarProgress(index, section, mFlashLevelBar);
    }
    private void initEvSeekBar() {
        final int length = mSettingsManager.getEntryValues(SettingsManager.KEY_EXPOSURE).length;
        resetEv();
        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
        final int section = 100 / length;
        if (mEvSeekBar == null) {
            mEvSeekBar = (SeekBar) mRootView.findViewById(R.id.ev_seekbar);
            mEvSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int index = progress / section;
                    if (index > length - 1) index = length - 1;
                    int currentIndex = mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
                    if (currentIndex != index) {
                        mSettingsManager.setValueIndex(SettingsManager.KEY_EXPOSURE, index);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isEvChanging = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isEvChanging = false;
                }
            });
        }
        setEvBarProgress(index, section, mEvSeekBar);
    }
    private void setEvBarProgress(int evIndex, int section, SeekBar seekBar) {
        int progress = section * (evIndex);
        if (progress >100) progress = 100;
        seekBar.setProgress(progress);
    }

    public void setSurfaceHolder(){
        mSurfaceHolder = mSurfaceView.getHolder();
    }
    private void initZoomSeekBar() {
        mZoomLinearLayout = (LinearLayout) mRootView.findViewById(R.id.zoom_linearlayout);
        mZoomValueText = (TextView) mRootView.findViewById(R.id.zoom_value_text);
        mZoomSeekBar = (SeekBar) mRootView.findViewById(R.id.zoom_seekbar);
        Float zoomMax = mSettingsManager.getMaxZoom(mModule.getMainCameraId());
        float[] zoomRatioRange = mSettingsManager.getSupportedRatioZoomRange(
                mModule.getMainCameraId());
        if(mModule.getCurrenCameraMode() == CaptureModule.CameraMode.RTB ||
                (mSettingsManager.isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
            zoomRatioRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                    mModule.getMainCameraId());
        }
        mZoomValueText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mModule.getCurrenCameraMode() == CaptureModule.CameraMode.RTB ||
                        (mSettingsManager.isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())){
                    float[] zoomRTBRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                           mModule.getMainCameraId());
                    if(zoomRTBRange[0] > 1 ) {
                        return;
                    }
                }
                String[] entries;
                String[] values;
                float[] zoomRatioRange = mSettingsManager.getSupportedRatioZoomRange(
                        mModule.getMainCameraId());
                if (zoomRatioRange != null && zoomRatioRange[0] <1){
                    if(mSettingsManager.isSHDRLimited() && zoomRatioRange[0] < 0.9 &&
                            PersistUtil.getModelInfo().contains("8750")){
                        zoomRatioRange[0] = 0.9f;
                    }
                    String minZoomRatio = String.valueOf(zoomRatioRange[0]);
                    entries = mActivity.getResources().getStringArray(
                            R.array.pref_camera2_zomm_switch_wide_entries);
                    values = mActivity.getResources().getStringArray(
                            R.array.pref_camera2_zomm_switch_wide_entryvalues);


                    entries[0] = minZoomRatio + "x";
                    values[0] = minZoomRatio;

                } else {
                    entries = mActivity.getResources().getStringArray(
                            R.array.pref_camera2_zomm_switch_entries);
                    values = mActivity.getResources().getStringArray(
                            R.array.pref_camera2_zomm_switch_entryvalues);
                }
                float from  = mModule.getZoomValue();
                for(int i = 0; i<values.length; i++){
                    if(from >= Float.valueOf(values[i])) continue;
                    mZoomIndex =i;
                    break;
                }
                if(from >= Float.valueOf(values[values.length - 1])){
                    mZoomIndex = 0;
                }
                float to = Float.valueOf(values[mZoomIndex]);
                float range = to - from;
                int frame = Math.abs((int)range * ZOOM_SMOOTH_FRAME);
                if(frame == 0)
                    frame = ZOOM_SMOOTH_FRAME;
                else if(frame > ZOOM_SMOOTH_FRAME_MAX)
                    frame = ZOOM_SMOOTH_FRAME_MAX;
                mModule.updateZoomSmooth(from,to,frame);
                if(mModule.onZoomChanged(to)) {
                    mZoomValueText.setText(entries[mZoomIndex]);
                    if (mZoomRenderer != null) {
                        mZoomRenderer.setZoom(to);
                    }
                }
            }
        });

        if (mModule.isExtendedMaxZoomEnable()) {
            float maxZoom = mSettingsManager.getSupportedExtendedMaxZoom(
                    mModule.getMainCameraId());
            Log.v(TAG, "initZoomSeekBar maxZoom :" + maxZoom);
            if (zoomRatioRange != null) {
                if (maxZoom > zoomRatioRange[1]) {
                    zoomRatioRange[1] = maxZoom;
                }
            } else {
                if (maxZoom > zoomMax) {
                    zoomMax = maxZoom;
                }
            }
        }
        float zoomMin = 1.0f;
        if(zoomRatioRange != null) {
            mZoomFixedValue = zoomRatioRange[0];
            if (mZoomRenderer != null) {
                mZoomRenderer.setZoomMin(zoomRatioRange[0]);
                mZoomRenderer.setZoomMax(zoomRatioRange[1]);
            }
            Log.i(TAG, "initZoomSeekBar min:" + zoomRatioRange[0] + ", max :" + zoomRatioRange[1]);
            mZoomSeekBar.setMax((int)((zoomRatioRange[1] -zoomRatioRange[0]) * 100));
            if (zoomRatioRange[0] > zoomMin) {
                zoomMin = zoomRatioRange[0];
            }
            mZoomMaxValue = zoomRatioRange[1];
            mZoomRatioSupport = true;
        } else {
            mZoomFixedValue = 1.0f;
            mZoomMaxValue = zoomMax;
            mZoomSeekBar.setMax(zoomMax.intValue() * 100 - 100);
            mZoomRatioSupport = false;
        }
        if(mZoomFixedValue < 1) {
            mZoomIndex = 1;
            mZoomIncrease = true;
        } else {
            mZoomIndex = 0;
            mZoomIncrease = true;
        }
        updateZoomSeekBar(zoomMin);
        mZoomLinearLayout.setVisibility(View.VISIBLE);
        mZoomSeekBar.setVisibility(View.VISIBLE);
        mZoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float zoomValue = getZoomValue(progress,seekBar);
                mModule.updateZoomChanged(zoomValue);
                if (mZoomRenderer != null) {
                    mZoomRenderer.setZoom(zoomValue);
                }
                String txt = getZoomTxt(zoomValue);
                if (mZoomValueText != null && !mIsZoomKeyChanged) {
                    mZoomValueText.setText(txt);
                }
                mIsZoomKeyChanged = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float zoomValue = getZoomValue(seekBar.getProgress(),seekBar);
                mModule.updateZoomChanged(zoomValue);
                if (mZoomRenderer != null) {
                    mZoomRenderer.setZoom(zoomValue);
                }
                String txt = getZoomTxt(zoomValue);
                if (mZoomValueText != null) {
                    mZoomValueText.setText(txt);
                }
            }
        });
    }
    private String getZoomTxt(float zoomValue) {
        int zoomSig = Math.round(zoomValue * 100) / 100;
        int zoomFraction = Math.round(zoomValue * 100) % 100;
        String txt = "";
        if (zoomFraction < 10) {
            txt = zoomSig + "." + "0" + zoomFraction + "x";
        } else {
            txt = zoomSig + "." + zoomFraction + "x";
        }
        return txt;
    }

    private float getZoomValue(int progress,SeekBar seekBar){
        float zoomProgress = progress + mZoomFixedValue * 100;
        if (mZoomRatioSupport && mZoomFixedValue <1){
            int max = seekBar.getMax();
            int min = seekBar.getMin();
            int range = max - min;
            float delta = ((int)(range*mZoomBarRatio))*1.0f;
            if (progress <= delta){
                zoomProgress = (mZoomFixedValue + (1-mZoomFixedValue)*(progress/delta))*100f;
            } else {
                zoomProgress = 100f+((progress - delta)/(range-delta))*(mZoomMaxValue-1)*100f;
            }
        }

        float zoomValue =zoomProgress / 100f;
        return zoomValue;
    }

    private void setZoomBarProgress(float zoomValue, SeekBar seekBar){
        int zoomSig = Math.round(zoomValue * 100) / 100;
        int zoomFraction = Math.round(zoomValue * 100) % 100;
        int  progress = zoomSig * 100 + zoomFraction - ((int)(100 * mZoomFixedValue));
        if (mZoomRatioSupport && mZoomFixedValue <1){
            int max = seekBar.getMax();
            int min = seekBar.getMin();
            int range = max - min;
            float delta = ((int)(range*mZoomBarRatio))*1.0f;
            if (zoomValue < 1.0){
                progress = (int)((zoomValue-mZoomFixedValue)/(1.0f-mZoomFixedValue)*delta);
            } else {
                progress = (int)((zoomValue-1.0f)/(mZoomMaxValue - 1.0f)*(range-delta)+delta);
            }
        }
        seekBar.setProgress(progress);
    }

    public void enableZoomSeekBar(boolean enable) {
       if (mZoomSeekBar != null)
           mZoomSeekBar.setEnabled(enable);
       if(mZoomValueText != null)
           mZoomValueText.setEnabled(enable);
    }

    public boolean getZoomFixedSupport() {
        return mZoomRatioSupport && CaptureModule.MCXMODE;
    }


    public void updateOfflineDumpTrigger(int status) {
        if (mOfflineDumpTrigger != null) {
            String offlineDumpTrigger = mSettingsManager.getValue(
                    SettingsManager.KEY_OFFLINE_DUMP_TRIGGER);
            if ("1".equals(offlineDumpTrigger)){
                mOfflineDumpTrigger.setVisibility(status);
            } else {
                mOfflineDumpTrigger.setVisibility(View.GONE);
            }
        }
    }

    public void hideAICameraSeekBar() {
        if (mAICameraSeekBar != null) {
            mAICameraSeekBar.setVisibility(View.GONE);
        }
    }

    public void showAICameraSeekBar() {
        if (mAICameraSeekBar != null) {
            mAICameraSeekBar.setVisibility(View.VISIBLE);
        }
    }

    public void hideZoomSeekBar() {
        if (mZoomLinearLayout != null) {
            mZoomLinearLayout.setVisibility(View.GONE);
        }
        if (mZoomValueText != null) {
            mZoomValueText.setVisibility(View.GONE);
        }
        if (mZoomSeekBar != null) {
            mZoomSeekBar.setVisibility(View.GONE);
        }
    }

    public void showZoomSeekBar() {
        if (mZoomLinearLayout != null) {
            mZoomLinearLayout.setVisibility(View.VISIBLE);
        }
        if (mZoomValueText != null) {
            mZoomValueText.setVisibility(View.VISIBLE);
        }
        if (mZoomSeekBar != null) {
            mZoomSeekBar.setVisibility(View.VISIBLE);
        }
        if(mFilterMenuStatus == FILTER_MENU_ON){
            hideZoomSeekBar();
        }
    }
    public void updateZoomSeekBar(float zoomValue) {
        String txt = getZoomTxt(zoomValue);
        if (mZoomValueText != null) {
            mZoomValueText.setText(txt);
            mIsZoomKeyChanged =true;
        }
        if (mZoomSeekBar != null) {
            setZoomBarProgress(zoomValue, mZoomSeekBar);
        }
    }

    private void selectModeText(View view) {
        for (TextView textView : mCameraModeTexts) {
            textView.setSelected(textView == view);
        }
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation) {
        mDecodeTaskForReview = new CaptureUI.DecodeImageForReview(jpegData, orientation);
        mDecodeTaskForReview.execute();
        if (getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL) {
            if (mFilterMenuStatus == FILTER_MENU_ON) {
                removeFilterMenu(false);
            }
            mPreviewLayout.setVisibility(View.VISIBLE);
            CameraUtil.fadeIn(mReviewDoneButton);
            CameraUtil.fadeIn(mReviewRetakeButton);
        }
    }

    protected void showRecordVideoForReview(Bitmap preview) {
        if (getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL) {
            if (mFilterMenuStatus == FILTER_MENU_ON) {
                removeFilterMenu(false);
            }
            mReviewImage.setImageBitmap(preview);
            mPreviewLayout.setVisibility(View.VISIBLE);
            mReviewPlayButton.setVisibility(View.VISIBLE);
            CameraUtil.fadeIn(mReviewDoneButton);
            CameraUtil.fadeIn(mReviewRetakeButton);
        }
    }

    public void updateAwbInfoText(String[] info) {
        if (info == null || info.length <4)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AWB_INFO_TITLE[0]+info[0]).append("\r\n")
                .append(AWB_INFO_TITLE[2]+info[2]).append("\r\n")
                .append(AWB_INFO_TITLE[3]+info[3]);
        mStatsAwbText.setText(stringBuilder.toString());
    }

    public void updateAecInfoText(String[] info) {
        if (info == null || info.length <15)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AEC_INFO_TITLE[0]+info[0]).append("\r\n")
                .append(AEC_INFO_TITLE[1]+info[1]+" "+info[2]).append("\r\n")
                .append(" "+info[3]).append("\r\n")
                .append(AEC_INFO_TITLE[2]+info[4]).append("\r\n")
                .append( " "+info[5]+" "+info[6]).append("\r\n")
                .append(AEC_INFO_TITLE[3]+info[7]).append("\r\n")
                .append(" "+info[8]+" "+info[9]).append("\r\n")
                .append(STATS_EXTENSION_TITLE[0]+" "+info[10]).append("\r\n")
                .append(STATS_EXTENSION_TITLE[1]+" "+info[11]).append("\r\n")
                .append(STATS_EXTENSION_TITLE[2]+" "+info[12]).append("\r\n")
                .append(STATS_EXTENSION_TITLE[3]+" "+info[13]).append("\r\n")
                .append(STATS_EXTENSION_TITLE[4]+" "+info[14]);
        mStatsAecText.setText(stringBuilder.toString());
    }
    public void updateLowLightText(CaptureResult result) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_LOWLIGHT_BOOST);
        if (value != null && value.equals("1")) {
            try {
                int aemode = result.get(CaptureResult.CONTROL_AE_MODE);
                int lowLightBoostState = result.get(CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE);
                if (lowLightBoostState == CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE) {
                    mLowLightText.setText("LowLightBoost_Active");
                } else if (lowLightBoostState == CameraMetadata.CONTROL_LOW_LIGHT_BOOST_STATE_INACTIVE) {
                    mLowLightText.setText("LowLightBoost_InActive");
                } else {
                    mLowLightText.setText("LowLightBoost_Unknown");
                }
            } catch (NullPointerException e) {
                mLowLightText.setText("LowLightBoost_Unknown");
            }
            mLowLightText.setVisibility(View.VISIBLE);
        } else {
            mLowLightText.setVisibility(View.INVISIBLE);
        }
    }


    public void updateStatsNNResultText(byte statsNNWidth, byte statsNNHeight, byte statsNNMapdata, byte statsNNNumroi, int[] statsNNRoiData, int statsNNRoiWeight) {
        mStatsNNResultText.setText(STATS_NN_RESULT_TITLE[0]+Byte.toString(statsNNWidth) +" " + "\r\n" +
                                 STATS_NN_RESULT_TITLE[1]+Byte.toString(statsNNHeight) +" " + "\r\n" +
                                 STATS_NN_RESULT_TITLE[2]+Byte.toString(statsNNMapdata) +" " + "\r\n" +
                                 STATS_NN_RESULT_TITLE[3]+Byte.toString(statsNNNumroi) +" " + "\r\n" +
                                 STATS_NN_RESULT_TITLE[4]+String.valueOf(statsNNRoiData[0]) +"," +
                                 String.valueOf(statsNNRoiData[1]) +"," + String.valueOf(statsNNRoiData[2]) +"," + 
                                 String.valueOf(statsNNRoiData[3]) +" "+"\r\n" +
                                 STATS_NN_RESULT_TITLE[5]+String.valueOf(statsNNRoiWeight));
    }

    public void updatePerformanceDebugInfoText(String[] info) {
        if (info == null)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < PERFORMANCE_DEBUG_TITLE.length; i++) {
            if(!info[i].equals("0") && !info[i].equals("0.0"))stringBuilder.append(PERFORMANCE_DEBUG_TITLE[i]+info[i]).append("\r\n");
        }
        mDebugPerformancText.setText(stringBuilder.toString());
    }

    public void updatePerformanceDebugInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mPerformancDebugInfo != null) {
                    mPerformancDebugInfo.setVisibility(visibility);
                }
            }
        });
    }

    public void updateAfdInfoText(String[] info) {
        if (info == null || info.length <16)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AFD_INFO_TITLE[0]+info[0]).append("\r\n")
                .append(AFD_INFO_TITLE[1]+info[1]).append("\r\n")
                .append(AFD_INFO_TITLE[2]+info[2]).append("\r\n")
                .append(AFD_INFO_TITLE[3]+info[3]+" "+info[4]).append("\r\n")
                .append(" "+info[5]+" "+info[6]).append("\r\n")
                .append(AFD_INFO_TITLE[4]+info[7]).append("\r\n")
                .append(AFD_INFO_TITLE[5]+info[8]+" "+info[9]+" "+info[10]+" "+info[11]).append("\r\n")
                .append(AFD_INFO_TITLE[6]+info[12]+" "+info[13]).append("\r\n")
                .append(AFD_INFO_TITLE[7]+info[14]+" "+info[15]);
        mStatsAfdText.setText(stringBuilder.toString());
    }

    public void updateAfInfoText(String[] info) {
        if (info == null || info.length <7)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AF_INFO_TITLE[0]+info[0]).append("\r\n")
                .append(AF_INFO_TITLE[1]+info[1]).append("\r\n")
                .append(AF_INFO_TITLE[2]+info[2]).append("\r\n")
                .append(AF_INFO_TITLE[3]+info[3]).append("\r\n")
                .append(AF_INFO_TITLE[4]+info[4]).append("\r\n")
                .append(AF_INFO_TITLE[5]+info[5]).append("\r\n")
                .append(AF_INFO_TITLE[6]+info[6]);
        mStatsAfText.setText(stringBuilder.toString());
    }

    public void updateAecIdsInfoText(String[] info) {
        if (info == null || info.length <2)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AEC_IDS_INFO_TITLE[0]+info[0]).append("\r\n")
                .append(AEC_IDS_INFO_TITLE[1]+info[1]);
        mStatsAecIdsText.setText(stringBuilder.toString());
    }

    public void updateAWBInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsAwbInfo != null) {
                    mStatsAwbInfo.setVisibility(visibility);
                }
            }
        });
    }

    public void updateAFDInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsAfdInfo != null) {
                    mStatsAfdInfo.setVisibility(visibility);
                }
            }
        });
    }

    public void updateAFInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsAfInfo != null) {
                    mStatsAfInfo.setVisibility(visibility);
                }
            }
        });
    }

    public void updateAECIdInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsAecIdsInfo != null) {
                    mStatsAecIdsInfo.setVisibility(visibility);
                }
            }
        });
    }

    public void updateStatsNNVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsNNResult != null) {
                    mStatsNNResult.setVisibility(visibility);
                }
            }
        });
    }
    public void updateAECInfoVisibility(int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mStatsAecInfo != null) {
                    mStatsAecInfo.setVisibility(visibility);
                }
            }
        });
    }
    private int getCurrentIntentMode() {
        return mModule.getCurrentIntentMode();
    }

    private void updateDeepZoomIndex() {
        switch(mDeepZoomIndex) {
            case 0:
                mDeepzoomSetName.setText("Zoom OFF");
                mDeepZoomValue = 1.0f;
                break;
            case 1:
                mDeepzoomSetName.setText("Zoom 2X");
                mDeepZoomValue = 2.0f;
                break;
            case 2:
                mDeepzoomSetName.setText("Zoom 4X");
                mDeepZoomValue = 4.0f;
                break;
            default:
                mDeepZoomValue = 1.0f;
                mDeepzoomSetName.setText("Zoom OFF");
                break;
        }
        mModule.updateDeepZoomIndex(mDeepZoomValue);
    }

    private void toggleMakeup() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        if(value != null && !mIsVideoUI) {
            if(value.equals("0")) {
                mSettingsManager.setValue(SettingsManager.KEY_MAKEUP, "50");
                mMakeupSeekBar.setProgress(50);
                mMakeupSeekBarLayout.setVisibility(View.VISIBLE);
                mSeekbarBody.setVisibility(View.VISIBLE);
                mSeekbarToggleButton.setImageResource(R.drawable.seekbar_hide);
            } else {
                mSettingsManager.setValue(SettingsManager.KEY_MAKEUP, "0");
                mMakeupSeekBar.setProgress(0);
                mMakeupSeekBarLayout.setVisibility(View.GONE);
            }
            setMakeupButtonIcon();
            mModule.restartSession(true);
        }
    }

    private void setMakeupButtonIcon() {
        final String value = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(value != null && !value.equals("0")) {
                    mMakeupButton.setImageResource(R.drawable.beautify_on);
                    mMakeupSeekBarLayout.setVisibility(View.GONE);
                } else {
                    mMakeupButton.setImageResource(R.drawable.beautify);
                    mMakeupSeekBarLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    public float getDeepZoomValue() {
        return mDeepZoomValue;
    }

    public void onCameraOpened(int cameraId) {
        mGestures.setCaptureUI(this);
        if (mModule.isDeepZoom() ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.DEPTH) {
            mGestures.setZoomEnabled(false);
        } else {
            mGestures.setZoomEnabled(mSettingsManager.isZoomSupported(cameraId));
            initializeZoom(cameraId);
        }
    }

    public void reInitUI() {
        initSceneModeHDR();
        initFilterModeButton();
        initFlashButton();
        initZoomSeekBar();
        if(PersistUtil.showVerticalEvBar()) {
            initVerticalEvBar();
        }
        setMakeupButtonIcon();
        updateMenus();
        if(mModule.isTrackingFocusSettingOn()) {
            mTrackingFocusRenderer.setVisible(false);
            mTrackingFocusRenderer.setVisible(true);
        } else {
            mTrackingFocusRenderer.setVisible(false);
        }

        if (mModule.isT2TFocusSettingOn()) {
            mT2TFocusRenderer.setVisible(false);
            mT2TFocusRenderer.setVisible(true);
        } else {
            mT2TFocusRenderer.setVisible(false);
        }

        if (mModule.isSateNNFocusSettingOn()) {
            mStatsNNFocusRenderer.setVisible(false);
            mStatsNNFocusRenderer.setVisible(true);
        } else {
            mStatsNNFocusRenderer.setVisible(false);
        }

        if (mModule.isSateAFSettingOn()) {
            mAFViewRender.setVisible(false);
            mAFViewRender.setVisible(true);
        } else {
            mAFViewRender.setVisible(false);
        }
        if (mSurfaceViewMono != null) {
            if (mSettingsManager != null && mSettingsManager.getValue(SettingsManager.KEY_MONO_PREVIEW) != null
                    && mSettingsManager.getValue(SettingsManager.KEY_MONO_PREVIEW).equalsIgnoreCase("on")) {
                mSurfaceViewMono.setVisibility(View.VISIBLE);
            } else {
                mSurfaceViewMono.setVisibility(View.GONE);
            }
        }

        mZoomIncrease = true;
        mFaceView.initMode();
        if (mModule.getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL) {
            mModeSelectLayout.setVisibility(View.GONE);
        }
    }

    public void initializeProMode(boolean promode) {
        mCameraControls.setProMode(promode);
        if (promode) {
            mVideoButton.setVisibility(View.INVISIBLE);
            //mFlashButton.setVisibility(View.INVISIBLE);
        } else if (mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_NORMAL &&
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.VIDEO) {
            mVideoButton.setVisibility(View.VISIBLE);
        } else if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.RTB ||
                mSettingsManager.isRTBModeInSelectMode()){
        }
    }

    public void initializeBlurConfigSlide(boolean blurmode) {
        mCameraControls.setBlurMode(blurmode);
    }

    // called from onResume but only the first time
    public void initializeFirstTime() {
        // Initialize shutter button.
        int intentMode = mModule.getCurrentIntentMode();
        if (intentMode == CaptureModule.INTENT_MODE_CAPTURE) {
            mVideoButton.setVisibility(View.INVISIBLE);
        } else if (intentMode == CaptureModule.INTENT_MODE_VIDEO) {
            mShutterButton.setVisibility(View.INVISIBLE);
        } else {
            mShutterButton.setVisibility(View.VISIBLE);
            mVideoButton.setVisibility(View.INVISIBLE);
        }
        mShutterButton.setOnShutterButtonListener(mModule);
        mShutterButton.setImageResource(R.drawable.one_ui_shutter_anim);
        mShutterButton.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View v) {
                    doShutterAnimation();
            }
        });
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCountDown();
                mModule.onVideoButtonClick();
            }
        });
    }
    private void initializeZoom(int id) {
        if (!mSettingsManager.isZoomSupported(id) || (mZoomRenderer == null))
            return;

        Float zoomMax = mSettingsManager.getMaxZoom(id);
        float[] zoomRatioRange = mSettingsManager.getSupportedRatioZoomRange(
                mModule.getMainCameraId());
        if(mModule.getCurrenCameraMode() == CaptureModule.CameraMode.RTB ||
                (mSettingsManager.isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
            zoomRatioRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                    mModule.getMainCameraId());
        }
        if (mModule.isExtendedMaxZoomEnable()) {
            float maxZoom = mSettingsManager.getSupportedExtendedMaxZoom(
                    mModule.getMainCameraId());
            Log.v(TAG, "initializeZoom maxZoom :" + maxZoom);
            if (zoomRatioRange != null) {
                if (maxZoom > zoomRatioRange[1]) {
                    zoomRatioRange[1] = maxZoom;
                }
            } else {
                if (maxZoom > zoomMax) {
                    zoomMax = maxZoom;
                }
            }
        }
        float zoomMin = 1.0f;
        if(zoomRatioRange != null) {
            mZoomRenderer.setZoomMin(zoomRatioRange[0]);
            mZoomRenderer.setZoomMax(zoomRatioRange[1]);
            Log.v(TAG, " zoomRatioRange min: " + zoomRatioRange[0] + ", max :" + zoomRatioRange[1]);
            if (zoomRatioRange[0] > zoomMin) {
                zoomMin = zoomRatioRange[0];
            }
        } else {
            mZoomRenderer.setZoomMin(1f);
            mZoomRenderer.setZoomMax(zoomMax);
        }
        String zoomStr = mSettingsManager.getValue(SettingsManager.KEY_ZOOM);
        float zoom = Float.parseFloat(zoomStr);

        mZoomRenderer.setZoom(zoom > zoomMin ? zoom : zoomMin);
        mZoomRenderer.setOnZoomChangeListener(new ZoomChangeListener());
    }

    public void enableGestures(boolean enable) {
        if (mGestures != null) {
            mGestures.setEnabled(enable);
        }
    }

    public boolean isPreviewMenuBeingShown() {
        return mFilterMenuStatus == FILTER_MENU_ON;
    }

    public void removeFilterMenu(boolean animate) {
        if (animate) {
            animateSlideOut(mFilterLayout);
        } else {
            mFilterMenuStatus = FILTER_MENU_NONE;
            if (mFilterLayout != null) {
                ((ViewGroup) mRootView).removeView(mFilterLayout);
                mFilterLayout = null;
            }
            if (mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_NORMAL && !mModule.isRecordingVideo()) {
                mModeSelectLayout.setVisibility(View.VISIBLE);
            }
            mModule.updateZoomSeekBarVisible();
        }
        updateMenus();
    }

    public void openSettingsMenu() {
        if (mPreviewLayout != null && mPreviewLayout.getVisibility() == View.VISIBLE) {
            return;
        }
        clearFocus();
        removeFilterMenu(false);
        Intent intent = new Intent(mActivity, SettingsActivity.class);
        intent.putExtra(SettingsActivity.CAMERA_MODULE, mModule.getCurrenCameraMode());
        intent.putExtra(SettingsActivity.IS_SIGNGLE_CAMERA_MODULE, mModule.isSingleCameraMode());
        intent.putExtra(SettingsActivity.OPEN_DEVOPTION, mActivity.getDevOption());
        mActivity.startActivity(intent);
    }

    public void initCameraSwitcher() {
        mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        String value = mSettingsManager.getValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
        Log.d(TAG,"value of KEY_FRONT_REAR_SWITCHER_VALUE is null? " + (value==null));
        if (value == null)
            return;

        mFrontBackSwitcher.setVisibility(View.VISIBLE);
        mFrontBackSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mActivity.getPerformenceTest()) {
                    mModule.setStartedTime(System.currentTimeMillis());
                }
                switchFrontBackCamera();
            }
        });
    }

    public void switchFrontBackCamera() {
        if (mIsVideoUI || !mModule.getCameraModeSwitcherAllowed()
                || !isSupportFrontCamera(mModule.getCurrenCameraMode())) {
            return;
        }
        mModule.setCameraModeSwitcherAllowed(false);
        removeFilterMenu(false);

        String value = mSettingsManager.getValue(
                SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
        if (value == null)
            return;

        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
        index = (index + 1) % 2;
        if (index == 1 && (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.RTB ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.SAT)) {
            switchToPhotoModeDueToError(false);
        }
        mSettingsManager.setValueIndex(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE, index);
    }

    private boolean isSupportFrontCamera(CaptureModule.CameraMode mode) {
        return mode != CaptureModule.CameraMode.PRO_MODE && mode != CaptureModule.CameraMode.DEPTH;
    }

    public void initFlashButton() {
        mFlashButton.init(mModule.getCurrenCameraMode() == CaptureModule.CameraMode.VIDEO ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.PRO_MODE ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.HFR ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC);
    }
    public void updateFlashButton(boolean enable){
        if (mFlashButton.getVisibility()== View.VISIBLE) {
            mFlashButton.setEnabled(enable);
        }
    }
    public void changeFlashMode(boolean isVideoFlash){
        mFlashButton.changeFlashMode(isVideoFlash);
    }
    public void  showFlashButton(){
        mFlashButton.setVisibility(View.VISIBLE);
        updateFlashButton(true);
    }
    public void hideFlashButton() {
        mFlashButton.setVisibility(View.GONE);
        String key;
        boolean isVideoFlash = mModule.getCurrenCameraMode() == CaptureModule.CameraMode.VIDEO ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.PRO_MODE ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.HFR ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC;
        if (isVideoFlash) {
            key = SettingsManager.KEY_VIDEO_FLASH_MODE;
        } else {
            key = SettingsManager.KEY_FLASH_MODE;
        }
        mSettingsManager.setValue(key, "off");
    }

    public void initSceneModeButton() {
        mSceneModeSwitcher.setVisibility(View.INVISIBLE);
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (value == null) return;
        mSceneModeSwitcher.setVisibility(View.VISIBLE);
        if (mSettingsManager.isMultiCameraEnabled()){
            mSceneModeSwitcher.setEnabled(false);
        } else {
            mSceneModeSwitcher.setEnabled(true);
            mSceneModeSwitcher.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearFocus();
                    removeFilterMenu(false);
                    Intent intent = new Intent(mActivity, SceneModeActivity.class);
                    intent.putExtra(CameraUtil.KEY_IS_SECURE_CAMERA, mActivity.isSecureCamera());
                    intent.putExtra(SettingsActivity.CAMERA_MODULE, mModule.getCurrenCameraMode());
                    mActivity.startActivity(intent);
                }
            });
        }
    }

    public boolean showHDRScene() {
        CaptureModule.CameraMode currentMode = mModule.getCurrenCameraMode();
        if (CaptureModule.CameraMode.DEPTH == currentMode) {
            return false;
        }
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        String hdrmode = mSettingsManager.getVideoHdrMode();
        String multiCam = mSettingsManager.getValue(SettingsManager.KEY_MULTI_CAMERAS_MODE);
        if (value == null || mSettingsManager.getQuadBayerSensorPrefEnabled() ||
                (multiCam != null && multiCam.equals("on"))) return false;

        if (CaptureModule.CameraMode.DEFAULT != currentMode && CaptureModule.CameraMode.RTB != currentMode && CaptureModule.CameraMode.SAT != currentMode) {
            return false;
        }
        if (CaptureModule.CameraMode.RTB == currentMode) {
            String mfnrValue = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
            if (mfnrValue != null && !mfnrValue.equals("disable") && Integer.parseInt(mfnrValue) == 1)
                return false;
        }
        if(hdrmode != null && !hdrmode.equals("off")){
            return false;
        }
        String qllStr = mSettingsManager.getValue(SettingsManager.KEY_QLL);
        if (qllStr != null && qllStr.equals("1")) {
            return false;
        }
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        if (selectMode != null && selectMode.equals("single_rear_cameraid")) {
            boolean previewStabilizationOn = "enable".equals(mSettingsManager.
                    getValue(SettingsManager.KEY_PREVIEW_STABILIZATION));
            return !previewStabilizationOn;
        }
        return true;
    }
    public void initSceneModeHDR() {
        mSceneModeHDR.setVisibility(View.INVISIBLE);
        if(!showHDRScene()) return;
        mSceneModeHDR.setVisibility(View.VISIBLE);
        mScreenHDRindex = mSettingsManager.getValueIndex(SettingsManager.KEY_SCENE_MODE);
        mSceneModeHDR.setImageResource(mScreenHDRIcon[mScreenHDRindex]);
        if (mSettingsManager.isMultiCameraEnabled()){
            mSceneModeHDR.setEnabled(false);
        } else {
            mSceneModeHDR.setEnabled(true);
            mSceneModeHDR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!mModule.getCameraModeSwitcherAllowed()){
                        return;
                    }
                    mScreenHDRindex = (mScreenHDRindex + 1) % mScreenHDRIcon.length;
                    mSettingsManager.setValueIndex(SettingsManager.KEY_SCENE_MODE, mScreenHDRindex);
                    mSceneModeHDR.setImageResource(mScreenHDRIcon[mScreenHDRindex]);

                }
            });
        }
    }
    private void initFilterModeButton() {
        mFilterModeSwitcher.setVisibility(View.INVISIBLE);
        String value = mSettingsManager.getValue(SettingsManager.KEY_COLOR_EFFECT);
        if (value == null) return;

        enableView(mFilterModeSwitcher, SettingsManager.KEY_COLOR_EFFECT);

        mFilterModeSwitcher.setVisibility(View.VISIBLE);
        mFilterModeSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFilterMode();
                adjustOrientation();
                updateMenus();
                mModeSelectLayout.setVisibility(View.GONE);
                hideZoomSeekBar();
            }
        });
    }

    private void enableView(View view, String key) {
        Map<String, SettingsManager.Values> map = mSettingsManager.getValuesMap();
        SettingsManager.Values values = map.get(key);
        if ( values != null ) {
            boolean enabled = values.overriddenValue == null;
            view.setEnabled(enabled);
        }
    }

    public void showTimeLapseUI(boolean enable) {
        if (mTimeLapseLabel != null) {
            mTimeLapseLabel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public void showRecordingUI(boolean recording, boolean highspeed) {
        if (recording) {
            if (highspeed) {
                mFlashButton.setVisibility(View.GONE);
            } else {
                if (mModule.isAFLocked() ||
                        mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC){
                    hideFlashButton();
                } else {
                    mFlashButton.init(true);
                }
            }
            mVideoButton.setImageResource(R.drawable.video_stop);
            mRecordingTimeView.setText("00:00");
            mRecordingTimeRect.setVisibility(View.VISIBLE);
            updateOfflineDumpTrigger(View.VISIBLE);
            mMuteButton.setVisibility(showMuteButton()? View.VISIBLE : View.INVISIBLE);
            setMuteButtonResource(!mModule.isAudioMute());
        } else {
            mFlashButton.setVisibility(View.VISIBLE);
//            mSettingsManager.setValue(SettingsManager.KEY_VIDEO_FLASH_MODE, "off");
            if (mModule.isAFLocked() ||
                    mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC){
                hideFlashButton();
            } else {
                mFlashButton.init(true);
            }
            mVideoButton.setImageResource(R.drawable.video_capture);
            mRecordingTimeRect.setVisibility(View.GONE);
            mMuteButton.setVisibility(View.INVISIBLE);
            updateOfflineDumpTrigger(View.GONE);
        }
    }
    private boolean showMuteButton(){
        String audioSelected = mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER);
        if((mModule.isHSRMode() || mModule.getHighSpeedCaptureRate() < 60) &&
                !audioSelected.equals("off")){
            return  true;
        }
        return  false;
    }
    private void setMuteButtonResource(boolean isUnMute) {
        if(isUnMute) {
            mMuteButton.setImageResource(R.drawable.ic_unmuted_button);
        } else {
            mMuteButton.setImageResource(R.drawable.ic_muted_button);
        }
    }

    private boolean needShowInstructional() {
        boolean needShow = true;
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getGlobalSharedPreferencesName(mActivity), Context.MODE_PRIVATE);
        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_SCENE_MODE);
        if ( index < 1 ) {
            needShow = false;
        }else{
            final String instructionalKey = SettingsManager.KEY_SCENE_MODE + "_" + index;
            needShow = pref.getBoolean(instructionalKey, false) ? false : true;
        }

        return needShow;

    }

    private void showSceneInstructionalDialog(int orientation) {
        int layoutId = R.layout.scene_mode_instructional;
        if ( orientation == 90 || orientation == 270 ) {
            layoutId = R.layout.scene_mode_instructional_landscape;
        }
        LayoutInflater inflater =
                (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layoutId, null);

       final int index = mSettingsManager.getValueIndex(SettingsManager.KEY_SCENE_MODE);
        TextView name = (TextView)view.findViewById(R.id.scene_mode_name);
        CharSequence sceneModeNameArray[] =
                mSettingsManager.getEntries(SettingsManager.KEY_SCENE_MODE);
        name.setText(sceneModeNameArray[index]);

        ImageView icon = (ImageView)view.findViewById(R.id.scene_mode_icon);
        int[] resId = mSettingsManager.getResource(SettingsManager.KEY_SCEND_MODE_INSTRUCTIONAL,
                SettingsManager.RESOURCE_TYPE_THUMBNAIL);
        icon.setImageResource(resId[index]);

        TextView instructional = (TextView)view.findViewById(R.id.scene_mode_instructional);
        CharSequence instructionalArray[] =
                mSettingsManager.getEntries(SettingsManager.KEY_SCEND_MODE_INSTRUCTIONAL);
        if ( instructionalArray[index].length() == 0 ) {
            //For now, not all scene mode has instructional
            return;
        }
        instructional.setText(instructionalArray[index]);

        final CheckBox remember = (CheckBox)view.findViewById(R.id.remember_selected);
        Button ok = (Button)view.findViewById(R.id.scene_mode_instructional_ok);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if ( remember.isChecked()) {
                    final SharedPreferences pref = mActivity.getSharedPreferences(
                            ComboPreferences.getGlobalSharedPreferencesName(mActivity),
                            Context.MODE_PRIVATE);

                    String instructionalKey = SettingsManager.KEY_SCENE_MODE + "_" + index;
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean(instructionalKey, true);
                    editor.commit();
                }
                mSceneModeInstructionalDialog.dismiss();
                mSceneModeInstructionalDialog = null;
            }
        });

        mSceneModeInstructionalDialog =
                new AlertDialog.Builder(mActivity, AlertDialog.THEME_HOLO_LIGHT)
                        .setView(view).create();
        try {
            mSceneModeInstructionalDialog.show();
        }catch(Exception e) {
            Log.w(TAG,e.toString());
            return;
        }
        if ( orientation != 0 ) {
            rotationSceneModeInstructionalDialog(view, orientation);
        }
    }

    private int getScreenWidth() {
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels < metric.heightPixels ? metric.widthPixels : metric.heightPixels;
    }

    private void rotationSceneModeInstructionalDialog(View view, int orientation) {
        view.setRotation(-orientation);
        int screenWidth = getScreenWidth();
        int dialogSize = screenWidth*9/10;
        Window dialogWindow = mSceneModeInstructionalDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER);
        lp.width = lp.height = dialogSize;
        dialogWindow.setAttributes(lp);
        RelativeLayout layout = (RelativeLayout)view.findViewById(R.id.mode_layout_rect);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dialogSize, dialogSize);
        layout.setLayoutParams(params);
    }

    private void showSceneModeLabel() {
        mIsSceneModeLabelClose = false;
        int index = mSettingsManager.getValueIndex(SettingsManager.KEY_SCENE_MODE);
        CharSequence sceneModeNameArray[] = mSettingsManager.getEntries(SettingsManager.KEY_SCENE_MODE);
        if (mModule.isDeepPortraitMode()) {
            mSceneModeLabelRect.setVisibility(View.GONE);
            mExitBestMode.setVisibility(View.GONE);
            return;
        }
        if ( index > 0 && index < sceneModeNameArray.length) {
            mSceneModeName.setText(sceneModeNameArray[index]);
            mSceneModeLabelRect.setVisibility(View.VISIBLE);
            mExitBestMode.setVisibility(View.VISIBLE);
            if (mModule.isDeepZoom()) {
                mDeepZoomModeRect.setVisibility(View.VISIBLE);
            } else {
                mDeepZoomModeRect.setVisibility(View.GONE);
            }
        }else{
            mSceneModeLabelRect.setVisibility(View.GONE);
            mExitBestMode.setVisibility(View.GONE);
            mDeepZoomModeRect.setVisibility(View.GONE);
        }
    }


    public void resetTrackingFocus() {
        if(mModule.isTrackingFocusSettingOn()) {
            mTrackingFocusRenderer.setVisible(false);
            mTrackingFocusRenderer.setVisible(true);
        }
    }

    public void resetTouchTrackingFocus() {
        if (mModule.isT2TFocusSettingOn()) {
            mT2TFocusRenderer.setVisible(false);
            mT2TFocusRenderer.setVisible(true);
        }
    }

    public void resetStatsNNTrackingFocus() {
        if (mModule.isT2TFocusSettingOn()) {
            mStatsNNFocusRenderer.setVisible(false);
            mStatsNNFocusRenderer.setVisible(true);
        }
    }

    public void resetAFRender() {
        if (mModule.isSateAFSettingOn()) {
            mAFViewRender.setVisible(false);
            mAFViewRender.setVisible(true);
        }
    }
    public void hideUIinTorchMode() {
        mModeSelectLayout.setVisibility(View.INVISIBLE);
        mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        mFilterModeSwitcher.setVisibility(View.INVISIBLE);
        mSceneModeHDR.setVisibility(View.INVISIBLE);
        mFlashButton.setVisibility(View.INVISIBLE);
        mSettingsIcon.setVisibility(View.INVISIBLE);
        mShutterButton.setVisibility(View.INVISIBLE);
        mThumbnail.setVisibility(View.INVISIBLE);
        hideZoomSeekBar();
    }
    private void hideUIInDepth(){
        mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        mFilterModeSwitcher.setVisibility(View.INVISIBLE);
        mSceneModeHDR.setVisibility(View.INVISIBLE);
        mFlashButton.setVisibility(View.INVISIBLE);
        mSettingsIcon.setVisibility(View.INVISIBLE);
        mShutterButton.setVisibility(View.INVISIBLE);
        mThumbnail.setVisibility(View.INVISIBLE);
        hideZoomSeekBar();
    }

    public void hideUIwhileRecording() {
        mCameraControls.setVideoMode(true);
        mModeSelectLayout.setVisibility(View.INVISIBLE);
        mSceneModeLabelRect.setVisibility(View.INVISIBLE);
        mDeepZoomModeRect.setVisibility(View.INVISIBLE);
        mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        mFilterModeSwitcher.setVisibility(View.INVISIBLE);
        mSceneModeHDR.setVisibility(View.INVISIBLE);
        mSettingsIcon.setVisibility(View.INVISIBLE);
        String value = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        if(value != null && value.equals("0")) {
            mMakeupButton.setVisibility(View.GONE);
        }
        mIsVideoUI = true;
        if (!mModule.isSSMEnabled()) {
            mPauseButton.setVisibility(View.VISIBLE);
        }
        if (mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_NORMAL) {
            mShutterButton.setVisibility(View.VISIBLE);
            mThumbnail.setVisibility(View.INVISIBLE);
        }
    }

    public void showUIafterRecording() {
        mCameraControls.setVideoMode(false);
        mFrontBackSwitcher.setVisibility(View.VISIBLE);
        if (!DEV_LEVEL_ALL && mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC) {
            mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        }
        mSettingsIcon.setVisibility(View.VISIBLE);
        mIsVideoUI = false;
        mPauseButton.setVisibility(View.INVISIBLE);
        if (mModule.getCurrentIntentMode() == CaptureModule.INTENT_MODE_NORMAL) {
            mShutterButton.setVisibility(View.INVISIBLE);
            mModeSelectLayout.setVisibility(View.VISIBLE);
            mThumbnail.setVisibility(View.VISIBLE);
        }
        String hdrmode = mSettingsManager.getVideoHdrMode();
        if (hdrmode != null && hdrmode.equals("off")) {
            mFilterModeSwitcher.setVisibility(View.VISIBLE);
        }
        if (mFilterMenuStatus == FILTER_MENU_ON) {
            removeFilterMenu(true);
        }
    }

    public void showRelatedIcons(CaptureModule.CameraMode mode) {
        //common settings
        mShutterButton.setVisibility(View.VISIBLE);
        mFrontBackSwitcher.setVisibility(View.VISIBLE);
        mMakeupButton.setVisibility(View.INVISIBLE);
        mSettingsIcon.setVisibility(View.VISIBLE);
        showThumbnail();
        //settings for each mode
        switch (mode) {
            case DEFAULT:
                mFilterModeSwitcher.setVisibility(View.VISIBLE);
                mVideoButton.setVisibility(View.INVISIBLE);
                mMuteButton.setVisibility(View.INVISIBLE);
                mPauseButton.setVisibility(View.INVISIBLE);
                break;
            case RTB:
            case SAT:
                mFilterModeSwitcher.setVisibility(View.VISIBLE);
                mVideoButton.setVisibility(View.INVISIBLE);
                if(!CaptureModule.MCXMODE) mFlashButton.setVisibility(View.INVISIBLE);
                mMuteButton.setVisibility(View.INVISIBLE);
                mPauseButton.setVisibility(View.INVISIBLE);
                if (!DEV_LEVEL_ALL) {
                    mFrontBackSwitcher.setVisibility(View.INVISIBLE);
                }
                break;
            case VIDEO:
            case HFR:
                mVideoButton.setVisibility(View.VISIBLE);
                mFilterModeSwitcher.setVisibility(View.VISIBLE);
                mShutterButton.setVisibility(View.INVISIBLE);
                break;
            case CINEMATIC:
                mVideoButton.setVisibility(View.VISIBLE);
                mFilterModeSwitcher.setVisibility(View.VISIBLE);
                mShutterButton.setVisibility(View.INVISIBLE);
                if (!DEV_LEVEL_ALL) {
                    mFrontBackSwitcher.setVisibility(View.INVISIBLE);
                }
                break;
            case PRO_MODE:
                mFilterModeSwitcher.setVisibility(View.INVISIBLE);
                mVideoButton.setVisibility(View.INVISIBLE);
                mFrontBackSwitcher.setVisibility(View.INVISIBLE);
                mMuteButton.setVisibility(View.INVISIBLE);
                mPauseButton.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        String value = mSettingsManager.getValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
        if (value == null) {
            mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        }
        if(mModule.mMFNREnable && mModule.getMainCameraId() ==  android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT){
            mFilterModeSwitcher.setVisibility(View.INVISIBLE);
            mSettingsManager.setValue(SettingsManager.KEY_COLOR_EFFECT,"0");
        }
        String hdrmode = mSettingsManager.getVideoHdrMode();
        if (hdrmode != null && !hdrmode.equals("off")) {
            mFilterModeSwitcher.setVisibility(View.INVISIBLE);
        }
    }
    private int mFilterHight,mFilterWidth;
    public int getFilterHight(){
        return  mFilterHight;
    }
    public int getFilterWidth(){
        return  mFilterWidth;
    }
    public void addFilterMode() {
        if (mSettingsManager.getValue(SettingsManager.KEY_COLOR_EFFECT) == null)
            return;

        int rotation = CameraUtil.getDisplayRotation(mActivity);
        boolean mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(mActivity);
        if (!mIsDefaultToPortrait) {
            rotation = (rotation + 90) % 360;
        }
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        CharSequence[] entries = mSettingsManager.getEntries(SettingsManager.KEY_COLOR_EFFECT);

        Resources r = mActivity.getResources();
        int height = (int) (r.getDimension(R.dimen.filter_mode_height) + 2
                * r.getDimension(R.dimen.filter_mode_padding) + 1);
        int width = (int) (r.getDimension(R.dimen.filter_mode_width) + 2
                * r.getDimension(R.dimen.filter_mode_padding) + 1);

        int gridRes;
        boolean portrait = (rotation == 0) || (rotation == 180);
        int size = height;
        if (!portrait) {
            gridRes = R.layout.vertical_grid;
            size = width;
        } else {
            gridRes = R.layout.horiz_grid;
        }

        int[] thumbnails = mSettingsManager.getResource(SettingsManager.KEY_COLOR_EFFECT,
                SettingsManager.RESOURCE_TYPE_THUMBNAIL);
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout gridOuterLayout = (FrameLayout) inflater.inflate(
                gridRes, null, false);
        gridOuterLayout.setBackgroundColor(android.R.color.transparent);
        removeFilterMenu(false);
        mFilterMenuStatus = FILTER_MENU_ON;
        mFilterLayout = new LinearLayout(mActivity);

        ViewGroup.LayoutParams params = null;
        if (!portrait) {
            params = new ViewGroup.LayoutParams(size, FrameLayout.LayoutParams.MATCH_PARENT);
            mFilterLayout.setLayoutParams(params);
            ((ViewGroup) mRootView).addView(mFilterLayout);
        } else {
            params = new ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, size);
            mFilterLayout.setLayoutParams(params);
            ((ViewGroup) mRootView).addView(mFilterLayout);
            mFilterLayout.setY(display.getHeight() - 2 * size);
            if(mActivity.getAutoTest()) {
                mFilterHight = display.getHeight() - 2 * size;
            }
        }
        gridOuterLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams
                .MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout gridLayout = (LinearLayout) gridOuterLayout.findViewById(R.id.layout);
        final View[] views = new View[entries.length];

        int init = mSettingsManager.getValueIndex(SettingsManager.KEY_COLOR_EFFECT);
        for (int i = 0; i < entries.length; i++) {
            RotateLayout filterBox = (RotateLayout) inflater.inflate(
                    R.layout.filter_mode_view, null, false);
            ImageView imageView = (ImageView) filterBox.findViewById(R.id.image);

            final int j = i;

            filterBox.setOnTouchListener(new View.OnTouchListener() {
                private long startTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (System.currentTimeMillis() - startTime < CLICK_THRESHOLD) {
                            mSettingsManager.setValueIndex(SettingsManager
                                    .KEY_COLOR_EFFECT, j);
                            for (View v1 : views) {
                                v1.setBackground(null);
                            }
                            ImageView image = (ImageView) v.findViewById(R.id.image);
                            image.setBackgroundColor(HIGHLIGHT_COLOR);
                        }
                    }
                    return true;
                }
            });

            views[j] = imageView;
            if (i == init)
                imageView.setBackgroundColor(HIGHLIGHT_COLOR);
            TextView label = (TextView) filterBox.findViewById(R.id.label);

            imageView.setImageResource(thumbnails[i]);
            if(mActivity.getAutoTest() && i ==0 ){
                mFilterWidth = imageView.getMeasuredWidth();
            }


            label.setText(entries[i]);
            gridLayout.addView(filterBox);
        }
        mFilterLayout.addView(gridOuterLayout);
    }

    public void removeAndCleanUpFilterMenu() {
        removeFilterMenu(false);
        cleanUpMenus();
    }

    public void animateFadeIn(View v) {
        ViewPropertyAnimator vp = v.animate();
        vp.alpha(0.85f).setDuration(ANIMATION_DURATION);
        vp.start();
    }

    private void animateSlideOut(final View v) {
        if (v == null || mFilterMenuStatus == FILTER_MENU_IN_ANIMATION)
            return;
        mFilterMenuStatus = FILTER_MENU_IN_ANIMATION;

        ViewPropertyAnimator vp = v.animate();
        if (View.LAYOUT_DIRECTION_RTL == TextUtils
                .getLayoutDirectionFromLocale(Locale.getDefault())) {
            vp.translationXBy(v.getWidth()).setDuration(ANIMATION_DURATION);
        } else {
            vp.translationXBy(-v.getWidth()).setDuration(ANIMATION_DURATION);
        }
        vp.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeAndCleanUpFilterMenu();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                removeAndCleanUpFilterMenu();
            }
        });
        vp.start();
    }

    public void animateSlideIn(View v, int delta, boolean forcePortrait) {
        int orientation = getOrientation();
        if (!forcePortrait)
            orientation = 0;

        ViewPropertyAnimator vp = v.animate();
        float dest;
        if (View.LAYOUT_DIRECTION_RTL == TextUtils
                .getLayoutDirectionFromLocale(Locale.getDefault())) {
            switch (orientation) {
                case 0:
                    dest = v.getX();
                    v.setX(-(dest - delta));
                    vp.translationX(dest);
                    break;
                case 90:
                    dest = v.getY();
                    v.setY(-(dest + delta));
                    vp.translationY(dest);
                    break;
                case 180:
                    dest = v.getX();
                    v.setX(-(dest + delta));
                    vp.translationX(dest);
                    break;
                case 270:
                    dest = v.getY();
                    v.setY(-(dest - delta));
                    vp.translationY(dest);
                    break;
            }
        } else {
            switch (orientation) {
                case 0:
                    dest = v.getX();
                    v.setX(dest - delta);
                    vp.translationX(dest);
                    break;
                case 90:
                    dest = v.getY();
                    v.setY(dest + delta);
                    vp.translationY(dest);
                    break;
                case 180:
                    dest = v.getX();
                    v.setX(dest + delta);
                    vp.translationX(dest);
                    break;
                case 270:
                    dest = v.getY();
                    v.setY(dest - delta);
                    vp.translationY(dest);
                    break;
            }
        }
        vp.setDuration(ANIMATION_DURATION).start();
    }

    public void hideUIWhileCountDown() {
        hideCameraControls(true);
        mGestures.setZoomOnly(true);
    }

    public void showUIAfterCountDown() {
        hideCameraControls(false);
        mGestures.setZoomOnly(false);
        updateMenus();
    }

    public void hideCameraControls(boolean hide) {
        final boolean status = !hide;
        if (mFlashButton != null){
            mFlashButton.setEnabled(status && !mModule.isLongShotSettingEnabled());
            if (!hide) {
                mFlashButton.init(mModule.getCurrenCameraMode() == CaptureModule.CameraMode.VIDEO ||
                        mModule.getCurrenCameraMode() == CaptureModule.CameraMode.PRO_MODE);
            }
        }
        if (mFrontBackSwitcher != null) mFrontBackSwitcher.setEnabled(status);
        if (mSceneModeHDR != null) mSceneModeHDR.setEnabled(status);
        if (mFilterModeSwitcher != null) mFilterModeSwitcher.setEnabled(status);
        if (mMakeupButton != null) mMakeupButton.setVisibility(View.GONE);
        if(!status){
            if (mShutterButton != null) mShutterButton.setEnabled(status);
        }
    }

    public void initializeControlByIntent() {
        mThumbnail = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CameraControls.isAnimating() && !mModule.isTakingPicture() &&
                        !mModule.isRecordingVideo()) {
                    //mActivity.gotoGallery();
                    mFilmstripLayout.showFilmstrip();
                    showBottomControls();
                    if (mFilmstripLayout.getVisibility() == View.VISIBLE) {
                        mModule.updateFlashMode(true);
                    }
                }
            }
        });
        if (mModule.getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL) {
            mCameraControls.setIntentMode(mModule.getCurrentIntentMode());
        }
    }
    public String getTitleFromFilm(int index){
       return mFilmstripLayout.getTitleFromFilm(index);
    }
    public void doShutterAnimation() {
        AnimationDrawable frameAnimation = (AnimationDrawable) mShutterButton.getDrawable();
        frameAnimation.stop();
        frameAnimation.start();
    }

    public void showUI() {
        if (!mUIhidden)
            return;
        mUIhidden = false;
        mPieRenderer.setBlockFocus(false);
        mCameraControls.showUI();
    }

    public void hideUI() {
        if (mUIhidden)
            return;
        mUIhidden = true;
        mPieRenderer.setBlockFocus(true);
        mCameraControls.hideUI();
    }

    public void cleanUpMenus() {
        showUI();
        updateMenus();
        mActivity.setSystemBarsVisibility(false);
    }

    public void updateProUIForTest(String key, String value) {
        mCameraControls.updateProUIForTest(key, value);
    }

    public void startDeepPortraitMode(Size preview) {
        mSurfaceView.setVisibility(View.GONE);
        mSurfaceViewMono.setVisibility(View.GONE);
        mGLSurfaceView = new GLCameraPreview(
                    mActivity, preview.getWidth(), preview.getHeight(), mModule);
        FrameLayout layout = (FrameLayout) mActivity.findViewById(R.id.camera_glpreview);
        layout.addView(mGLSurfaceView);
        mGLSurfaceView.setVisibility(View.VISIBLE);
        mRootView.requestLayout();
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(mActivity);
        int progress = prefs.getInt(SettingsManager.KEY_DEEPPORTRAIT_VALUE,50);
        mDeepportraitSeekBar.setProgress(progress);
        mDeepportraitSeekBar.setVisibility(View.VISIBLE);
        mRenderOverlay.setVisibility(View.GONE);
    }

    public void stopDeepPortraitMode() {
        FrameLayout layout = (FrameLayout)mActivity.findViewById(R.id.camera_glpreview);
        if (mGLSurfaceView != null) {
            mGLSurfaceView.setVisibility(View.GONE);
            layout.removeAllViews();
            mGLSurfaceView = null;
        }
        mDeepportraitSeekBar.setVisibility(View.GONE);
        mRenderOverlay.setVisibility(View.VISIBLE);
    }

    public GLCameraPreview getGLCameraPreview() {
        return  mGLSurfaceView;
    }

    public void updateMenus() {
        boolean enableMakeupMenu = true;
        boolean enableFilterMenu = true;
        boolean enableSceneMenu = true;
        String makeupValue = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        int colorEffect = mSettingsManager.getValueIndex(SettingsManager.KEY_COLOR_EFFECT);
        String sceneMode = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (makeupValue != null && !makeupValue.equals("0")) {
            enableSceneMenu = false;
            enableFilterMenu = false;
        } else if (colorEffect != 0 || mFilterMenuStatus == FILTER_MENU_ON){
            enableSceneMenu = false;
            enableMakeupMenu = false;
        }else if ( sceneMode != null && !sceneMode.equals("0")){
             enableMakeupMenu = false;
             enableFilterMenu = false;
        }
        if(!BeautificationFilter.isSupportedStatic()) {
            enableMakeupMenu = false;
        }
        mMakeupButton.setEnabled(enableMakeupMenu);
        if(!BeautificationFilter.isSupportedStatic()) {
            mMakeupButton.setVisibility(View.GONE);
        }
        mFilterModeSwitcher.setEnabled(enableFilterMenu);
        if (mSettingsManager.isMultiCameraEnabled()){
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    mSceneModeHDR.setEnabled(false);
                }
            });

        } else {
            mSceneModeHDR.setEnabled(enableSceneMenu);
        }
    }

    public void toggleProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public boolean arePreviewControlsVisible() {
        return !mUIhidden;
    }

    public void onOrientationChanged() {
    }

    /**
     * Enables or disables the shutter button.
     */
    public void enableShutter(boolean enabled) {
        if (mShutterButton != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mShutterButton.setEnabled(enabled);
                }
            });
        }
        if(!enabled || !mModule.isLongExpTmCaptrure()) stopShutterAnim();
    }
    public void startShutterAnim(long totalProgress) {
        mCurrentProgress = 0;
        mTotalProgress = (int) totalProgress;
        mCameraControls.showAnim();
        new Thread(new ProgressRunable()).start();
    }
    public void stopShutterAnim() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mCameraControls.hidenAnim();
            }
        });
    }
    public boolean isShutterEnabled() {
        return mShutterButton.isEnabled();
    }

    /**
     * Enables or disables the video button.
     */
    public void enableVideo(boolean enabled) {
        if (mVideoButton != null) {
            mVideoButton.setEnabled(enabled);
        }
    }

    private boolean handleBackKeyOnMenu() {
        if (mFilterMenuStatus == FILTER_MENU_ON) {
            removeFilterMenu(true);
            return true;
        }
        return false;
    }

    public FilmstripLayout getFilmstripLayout() {
        return mFilmstripLayout;
    }

    public boolean onBackPressed() {
        if (mFilmstripLayout.getVisibility() == View.VISIBLE) {
            boolean hide = mFilmstripLayout.onBackPressed();
            if(hide){
                mModule.updateFlashMode(false);
            }
            return hide;
        }
        if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.DEPTH) {
            switchToPhotoModeDueToError(true);
            return true;
        }
        if (handleBackKeyOnMenu()) return true;
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
            return true;
        }

        if (!mModule.isCameraIdle()) {
            // ignore backs while we're taking a picture
            return true;
        }
        return false;
    }


    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }

    public Surface getPreviewSurface() {
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            return mSurfaceHolder.getSurface();
        } else {
            synchronized (mSurfaceTextureLock) {
                if (mSurfaceTexture != null) {
                    Surface surface = new Surface(mSurfaceTexture);
                    return surface;
                } else {
                    Log.w(TAG, "getPreviewSurface return null");
                    return null;
                }
            }
        }
    }

    private class MonoDummyListener implements Allocation.OnBufferAvailableListener {
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        public MonoDummyListener(ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic) {
            this.yuvToRgbIntrinsic = yuvToRgbIntrinsic;
        }

        @Override
        public void onBufferAvailable(Allocation a) {
            if(mMonoDummyAllocation != null) {
                mMonoDummyAllocation.ioReceive();
                mIsMonoDummyAllocationEverUsed = true;
                if(mSurfaceViewMono.getVisibility() == View.VISIBLE) {
                    try {
                        yuvToRgbIntrinsic.forEach(mMonoDummyOutputAllocation);
                        mMonoDummyOutputAllocation.ioSend();
                    } catch(Exception e)
                    {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }
    }

    public void buildPhysicalSurfaces(){
        mPreviewSurfaces.clear();
        for (int i = 0; i< mPreviewCount; i++){
            if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
                if (mPhysicalViews[i] != null && mPhysicalViews[i].getVisibility() == View.VISIBLE) {
                    mPreviewSurfaces.add(mPhysicalHolders[i].getSurface());
                }
            } else {
                if (mPhysicalTextureViews.get(i) != null &&
                        mPhysicalTextureViews.get(i).getVisibility() == View.VISIBLE &&
                        mPhysicalTextureViews.get(i).getSurfaceTexture() != null) {
                    mPreviewSurfaces.add(new Surface(mPhysicalTextureViews.get(i).getSurfaceTexture()));
                }
            }
        }
        Log.i(TAG, "buildPhysicalSurfaces " + mPreviewSurfaces.size());
    }

    public List<Surface> getPhysicalSurfaces(){
        return mPreviewSurfaces;
    }

    public void initPhysicalSurfaces(Size logicalPreviewSize,Size[] physicalPreviewSizes){
        String physical_id = mSettingsManager.getSinglePhysicalCamera();
        if (mSettingsManager.getPhysicalCameraId() == null && physical_id == null)
            return;
        Set<String> physicalIds;
        if (physical_id != null) {
            physicalIds = mSettingsManager.getAllPhysicalCameraId();
            mPreviewCount = mSettingsManager.getAllPhysicalCameraId().size()+1;
        } else {
            physicalIds = mSettingsManager.getPhysicalCameraId();
            mPreviewCount = mSettingsManager.getPhysicalCameraId().size()+1;
        }

        Log.d(TAG,"initPhysicalSurfaces count="+mPreviewCount);

        Log.d(TAG, "initPhysicalSurfaces, logicalPreviewSize " + logicalPreviewSize
                + ", physicalPreviewSizes " + Arrays.toString(physicalPreviewSizes));

        if (logicalPreviewSize != null){
            mLogicalPreviewSize = new Size(logicalPreviewSize.getWidth(),logicalPreviewSize.getHeight());
        } else {
            mLogicalPreviewSize = new Size(mPreviewWidth/2,mPreviewHeight/2);
        }
        Log.d(TAG, "logical surface " + 0 + " preview size=" + mLogicalPreviewSize.toString());
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mPhysicalPreviewContainer.setVisibility(View.VISIBLE);
            mSurfaceView.setZOrderMediaOverlay(physical_id != null);
            mPhysicalHolders[0] = mPhysicalViews[0].getHolder();
            mPhysicalHolders[0].setFixedSize(mLogicalPreviewSize.getWidth(), mLogicalPreviewSize.getHeight());
            mPhysicalViews[0].setAspectRatio(mLogicalPreviewSize.getHeight(),mLogicalPreviewSize.getWidth());
            mPhysicalViews[0].setVisibility(View.VISIBLE);
            int i = 1;
            for (String id : physicalIds) {
                if (mPhysicalViews[i] != null) {
                    mPhysicalHolders[i] = mPhysicalViews[i].getHolder();
                    Size preview;
                    int physicalSizeIndex = mModule.getIndexByPhysicalId(id);
                    if (physicalSizeIndex < physicalPreviewSizes.length
                            && physicalPreviewSizes[physicalSizeIndex] != null) {
                        preview = new Size(physicalPreviewSizes[physicalSizeIndex].getWidth(),
                                physicalPreviewSizes[physicalSizeIndex].getHeight());
                    } else if (physical_id != null) {
                        preview = new Size(mPreviewWidth, mPreviewHeight);
                    } else {
                        preview = new Size(mPreviewWidth / 2, mPreviewHeight / 2);
                    }
                    Log.d(TAG, "physical surface " + i + " preview size=" + preview.toString());
                    mPhysicalHolders[i].setFixedSize(preview.getWidth(), preview.getHeight());
                    mPhysicalViews[i].setAspectRatio(preview.getHeight(),preview.getWidth());

                    mPhysicalViews[i].setVisibility(View.VISIBLE);
                }
                i++;
            }
        } else {
            mPhysicalPreviewContainer.setVisibility(View.VISIBLE);
            TextureView textureView0 = mPhysicalTextureViews.get(0);
            if (textureView0 != null) {
                if (physical_id == null) {
                    GridLayout.LayoutParams glp = (GridLayout.LayoutParams) textureView0.getLayoutParams();
                    glp.width = mLogicalPreviewSize.getWidth();
                    glp.height = mLogicalPreviewSize.getHeight();
                    textureView0.setLayoutParams(glp);
                } else {
                    mTextureView.bringToFront();
                }
                textureView0.setVisibility(View.VISIBLE);
            }
            int i = 1;
            for (String id : physicalIds) {
                TextureView textureView = mPhysicalTextureViews.get(i);
                if (textureView != null) {
                    Size preview;
                    int physicalSizeIndex = mModule.getIndexByPhysicalId(id);
                    if (physicalSizeIndex < physicalPreviewSizes.length
                            && physicalPreviewSizes[physicalSizeIndex] != null) {
                        preview = new Size(physicalPreviewSizes[physicalSizeIndex].getHeight(),
                                physicalPreviewSizes[physicalSizeIndex].getWidth());
                    } else if (physical_id != null) {
                        preview = new Size(mPreviewHeight, mPreviewWidth);
                    } else {
                        preview = new Size(mPreviewHeight / 2, mPreviewWidth / 2);
                    }
                    Log.d(TAG, "physical surface " + i + " preview size=" + preview.toString());
                    GridLayout.LayoutParams glp = (GridLayout.LayoutParams) textureView.getLayoutParams();
                    if (physical_id == null) {
                        glp.width = preview.getWidth();
                        glp.height = preview.getHeight();
                    } else {
                        glp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    textureView.setLayoutParams(glp);
                    textureView.setVisibility(View.VISIBLE);
                    mPhysicalPreviewSizes.put(i, preview);
                }
                i++;
            }
        }

    }

    public void hidePhysicalSurfaces() {
        Log.d(TAG,"hidePhysicalSurfaces");
        mPhysicalPreviewContainer.setVisibility(View.GONE);
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            for (SurfaceView view : mPhysicalViews){
                if (view != null){
                    view.setVisibility(View.GONE);
                }
            }
            Arrays.fill(mSurfaceReady, false);
        } else {
            for (int i =0; i < mPhysicalTextureViews.size(); i++) {
                TextureView textureView = mPhysicalTextureViews.get(i);
                if (textureView != null) {
                    textureView.setVisibility(View.GONE);
                }
                mPhysicalTextureViewReady.put(i, false);
            }
        }
        mPreviewCount = 0;
    }

    public void hideLogicalSurface(){
        Log.d(TAG,"hideLogicalSurface");
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            if (mPhysicalViews[0] != null) {
                mPhysicalViews[0].setVisibility(View.INVISIBLE);
            }
        } else {
            if (mPhysicalTextureViews.get(0) != null) {
                mPhysicalTextureViews.get(0).setVisibility(View.INVISIBLE);
            }
        }
    }

    public Surface getMonoDummySurface() {
        if (mMonoDummyAllocation == null) {
            RenderScript rs = RenderScript.create(mActivity);
            Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.YUV(rs));
            yuvTypeBuilder.setX(mPreviewWidth);
            yuvTypeBuilder.setY(mPreviewHeight);
            yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888);
            mMonoDummyAllocation = Allocation.createTyped(rs, yuvTypeBuilder.create(), Allocation.USAGE_IO_INPUT|Allocation.USAGE_SCRIPT);
            ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs));
            yuvToRgbIntrinsic.setInput(mMonoDummyAllocation);

            if(mSettingsManager.getValue(SettingsManager.KEY_MONO_PREVIEW).equalsIgnoreCase("on")) {
                Type.Builder rgbTypeBuilder = new Type.Builder(rs, Element.RGBA_8888(rs));
                rgbTypeBuilder.setX(mPreviewWidth);
                rgbTypeBuilder.setY(mPreviewHeight);
                mMonoDummyOutputAllocation = Allocation.createTyped(rs, rgbTypeBuilder.create(), Allocation.USAGE_SCRIPT | Allocation.USAGE_IO_OUTPUT);
                mMonoDummyOutputAllocation.setSurface(mSurfaceHolderMono.getSurface());
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mSurfaceHolderMono.setFixedSize(mPreviewWidth, mPreviewHeight);
                        mSurfaceViewMono.setVisibility(View.VISIBLE);
                    }
                });
            }
            mMonoDummyAllocation.setOnBufferAvailableListener(new MonoDummyListener(yuvToRgbIntrinsic));

            mIsMonoDummyAllocationEverUsed = false;
        }
        return mMonoDummyAllocation.getSurface();
    }

    public void showPreviewCover() {
        mPreviewCover.setVisibility(View.VISIBLE);
    }



    public void hidePreviewCover() {
        Log.i(TAG, "hidePreviewCover");
        // Hide the preview cover if need.
        if (mPreviewCover.getVisibility() != View.GONE) {
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    private void initializeCountDown() {
        mActivity.getLayoutInflater().inflate(R.layout.count_down_to_capture,
                (ViewGroup) mRootView, true);
        mCountDownView = (CountDownView) (mRootView.findViewById(R.id.count_down_to_capture));
        mCountDownView.setCountDownFinishedListener((CountDownView.OnCountDownFinishedListener) mModule);
        mCountDownView.bringToFront();
        mCountDownView.setOrientation(mOrientation);
    }

    public boolean isCountingDown() {
        return mCountDownView != null && mCountDownView.isCountingDown();
    }

    public void cancelCountDown() {
        if (mCountDownView == null) return;
        mCountDownView.cancelCountDown();
        showUIAfterCountDown();
    }

    public void initCountDownView() {
        if (mCountDownView == null) {
            initializeCountDown();
        } else {
            mCountDownView.initSoundPool();
        }
    }

    public void releaseSoundPool() {
        if (mCountDownView != null) {
            mCountDownView.releaseSoundPool();
        }
    }

    public void startCountDown(int sec, boolean playSound) {
        mCountDownView.startCountDown(sec, playSound);
        hideUIWhileCountDown();
    }

    public void onPause() {
        cancelCountDown();
        collapseCameraControls();

        if (mFaceView != null) mFaceView.clear();
        if(mTrackingFocusRenderer != null) {
            mTrackingFocusRenderer.setVisible(false);
        }
        if (mT2TFocusRenderer != null) {
            mT2TFocusRenderer.setVisible(false);
        }
        if (mStatsNNFocusRenderer != null) {
            mStatsNNFocusRenderer.setVisible(false);
        }
        if (mAFViewRender != null) {
            mAFViewRender.setVisible(false);
        }
        if (mMonoDummyAllocation != null && mIsMonoDummyAllocationEverUsed) {
            mMonoDummyAllocation.setOnBufferAvailableListener(null);
            mMonoDummyAllocation.destroy();
            mMonoDummyAllocation = null;
        }
        if (mMonoDummyOutputAllocation != null && mIsMonoDummyAllocationEverUsed) {
            mMonoDummyOutputAllocation.destroy();
            mMonoDummyOutputAllocation = null;
        }
        if (mSurfaceViewMono != null) {
            mSurfaceViewMono.setVisibility(View.GONE);
        }
    }

    public boolean collapseCameraControls() {
        // Remove all the popups/dialog boxes
        boolean ret = false;
        mCameraControls.showRefocusToast(false);
        return ret;
    }

    public void showRefocusToast(boolean show) {
        mCameraControls.showRefocusToast(show);
    }
    public void showFocusCircle(boolean show){
        if(show) mShowFocusCircle = true;
        else {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            mShowFocusCircle = false;
        }
    }
    public boolean isChangeFocus(){
        if(mFaceView != null && mFaceView.faceExists()) {
            return mFaceUpdated;
        }
        return false;
    }
    private ArrayList<FocusIndicator> getFocusIndicator() {
        ArrayList<FocusIndicator> foucusList =new ArrayList<>();
        if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.DEPTH) {
            return foucusList;
        }
        if (mModule.isTrackingFocusSettingOn()) {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            foucusList.add(mTrackingFocusRenderer);
        }
        String value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS);
        if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.CINEMATIC) {
            value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS_FOR_CINEMATIC);
        }
        if (value != null && value.equals("on")) {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            foucusList.add(mT2TFocusRenderer);
        }
        if (mModule.isSateNNFocusSettingOn()) {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            foucusList.add(mStatsNNFocusRenderer);
        }
        if (mModule.isSateAFSettingOn()) {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            foucusList.add(mAFViewRender);
        }
        FocusIndicator focusIndicator;
        if (mFaceView != null && mFaceView.faceExists() && !mIsTouchAF) {
            if (mPieRenderer != null) {
                mPieRenderer.clear();
            }
            foucusList.add(mFaceView);
            mFaceUpdated = false;

        } else {
            if(mShowFocusCircle)
            foucusList.add(mPieRenderer);
        }
        return foucusList;
    }

    public void showFocusAssistText() {
//        Log.d(TAG, "showFocusAssistText");
        if (mFocusAssistTextView == null) {
            mFocusAssistTextView = mRootView.findViewById(R.id.focus_assist_tv);
        }
        mFocusAssistTextView.setOnClickListener(v -> {
            Log.d(TAG, "Focus Assist TextView clicked");
            hideFocusAssistText();
            mModule.onFocusAssistModeStart(mFocusPointInPreview.x, mFocusPointInPreview.y);
        });
        int circleSize = mPieRenderer.getSize() / 2;
        mFocusAssistTextView.setReferCircle(mFocusPoint.x, mFocusPoint.y, circleSize);
        mFocusAssistTextView.setVisibility(View.VISIBLE);
        if (mFAImageView == null) {
            mFAImageView = mRootView.findViewById(R.id.focus_assist_iv);
        }
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(mPreviewHeight / 2, mPreviewWidth / 2);
        int leftMargin = mFocusPoint.x - mPreviewHeight / 2 / 2;
        int topMargin = mFocusPoint.y - mPreviewWidth / 2 / 2;
        int[] surfaceViewLocation = new int[2];
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getLocationInWindow(surfaceViewLocation);
        } else {
            mTextureView.getLocationInWindow(surfaceViewLocation);
        }
        if (leftMargin < surfaceViewLocation[0]) {
            leftMargin = surfaceViewLocation[0];
        } else if (leftMargin > (mPreviewHeight / 2 + surfaceViewLocation[0])) {
            leftMargin = mPreviewHeight / 2 + surfaceViewLocation[0];
        }
        if (topMargin < surfaceViewLocation[1]) {
            topMargin = surfaceViewLocation[1];
        } else if (topMargin > (mPreviewWidth / 2 + surfaceViewLocation[1])) {
            topMargin = mPreviewWidth / 2 + surfaceViewLocation[1];
        }
        params.leftMargin = leftMargin;
        params.topMargin = topMargin;
        mFAImageView.setLayoutParams(params);
        mFAImageView.setVisibility(View.VISIBLE);

        Rect displayRegion = new Rect(surfaceViewLocation[0], surfaceViewLocation[1], mPreviewHeight, mPreviewWidth);
        mFocusAssistTextView.setDisplayRegion(displayRegion);
        if (mEvSeekBar != null) {
            mFocusAssistTextView.setExtraOffset(0f, mEvSeekBar.getHeight());
        }
        mFocusAssistTextView.setOrientation(mOrientation, false);
    }

    public void hideFocusAssistText() {
        if (mFocusAssistTextView != null && mFocusAssistTextView.getVisibility() != View.GONE) {
            mFocusAssistTextView.setVisibility(View.GONE);
        }
        if (mFAImageView != null && mFAImageView.getVisibility() != View.GONE) {
            mFAImageView.setVisibility(View.GONE);
        }
    }

    private void animateFAImageIn() {

        FocusAssistImageView imageView = new FocusAssistImageView(mActivity);
        imageView.setLayoutParams(mFAImageView.getLayoutParams());
        ((ViewGroup)mRootView).addView(imageView);

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        mFAImageView.getGlobalVisibleRect(startBounds);

        Bitmap bitmap = Bitmap.createBitmap(startBounds.width(), startBounds.height(), Bitmap.Config.ARGB_8888);

        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getGlobalVisibleRect(finalBounds, globalOffset);
        } else {
            mTextureView.getGlobalVisibleRect(finalBounds, globalOffset);
        }


        final Rect cropRect = new Rect(startBounds);
        cropRect.offset(-globalOffset.x, -globalOffset.y);

        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            PixelCopy.request(mSurfaceView, cropRect, bitmap, copyResult -> {
                Log.d(TAG, "PixelCopy " + copyResult);
                if (copyResult == PixelCopy.SUCCESS) {
                    imageView.setBitmap(bitmap);
                }
            }, new Handler(Looper.getMainLooper()));
        } else {
            Bitmap bitmap1 = mTextureView.getBitmap();
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap1,
                    cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
            imageView.setBitmap(bitmap2);
        }

        imageView.setPivotX(0.5f);
        imageView.setPivotY(0.5f);

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(imageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(imageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(imageView, View.SCALE_X,
                        1f, 2f))
                .with(ObjectAnimator.ofFloat(imageView,
                        View.SCALE_Y, 1f, 2f));
        set.setDuration(500L);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Animator animator = ObjectAnimator.ofFloat(mFALayout, View.ALPHA, 0f, 1f);
                animator.setDuration(500L);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ((ViewGroup)mRootView).removeView(imageView);
                    }
                });
                animator.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                ((ViewGroup)mRootView).removeView(imageView);
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });
        set.start();


    }

    public void showFocusAssistView(TextureView.SurfaceTextureListener listener,
                                    float cropRegionXs, float cropRegionYs) {
        if (mFAViewStub == null) {
            mFAViewStub = mRootView.findViewById(R.id.focus_assist_view_stub);
            mFALayout = (FocusAssistLayout) mFAViewStub.inflate();
        }
        mFALayout.setListener(new FocusAssistLayout.Listener() {
            @Override
            public void onExit() {
                mModule.onFocusAssistModeStop();
            }

            @Override
            public void onFocus(float x, float y) {
                mModule.onFocusAssistRefocus(x, y);
            }

            @Override
            public void onStartPointChange(float xs, float ys) {
                mModule.onFocusAssistStartPointChange(xs, ys);
            }
        });
        mFALayout.setCropRegionStartPoint(cropRegionXs, cropRegionYs);
        mFATextureView = new TextureView(mActivity);
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        mFATextureView.setLayoutParams(params);
        mFALayout.addView(mFATextureView, 0);
        mFALayout.setPreviewTexSize(mPreviewHeight, mPreviewWidth);
        mFALayout.setVisibility(View.VISIBLE);
        mFATextureView.setSurfaceTextureListener(listener);
        mFATextureView.setVisibility(View.VISIBLE);

        mFALayout.setAlpha(0f);
        animateFAImageIn();
    }

    private void animateFAImageOut() {
        CameraRender cameraRender = mModule.getCameraRender();
        if (cameraRender == null) {
            Log.w(TAG, "CameraRender is null when animateFAImageOut");
            if (mFATextureView != null && mFALayout != null) {
                mFALayout.removeView(mFATextureView);
            }
            if (mFALayout != null) {
                mFALayout.setVisibility(View.GONE);
            }
            return;
        }
        Rect viewport = cameraRender.getViewport();

        FocusAssistImageView imageView = new FocusAssistImageView(mActivity);
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(viewport.width(), viewport.height());
        params.leftMargin = viewport.left;
        params.topMargin = viewport.top;
        imageView.setLayoutParams(params);
        ((ViewGroup)mRootView).addView(imageView);

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        mFAImageView.getGlobalVisibleRect(finalBounds);

        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getGlobalVisibleRect(startBounds, globalOffset);
        } else {
            mTextureView.getGlobalVisibleRect(startBounds, globalOffset);
        }

        Bitmap bitmap = Bitmap.createBitmap(startBounds.width(), startBounds.height(), Bitmap.Config.ARGB_8888);

        PixelCopy.request(new Surface(cameraRender.getDisplaySurfaceTexture()), viewport, bitmap, copyResult -> {
            Log.d(TAG, "PixelCopy " + copyResult);
            if (copyResult == PixelCopy.SUCCESS) {
                imageView.setBitmap(bitmap);
            }
        }, new Handler(Looper.getMainLooper()));

        imageView.setPivotX(0.5f);
        imageView.setPivotY(0.5f);

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(imageView, View.X,
                startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(imageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(imageView, View.SCALE_X,
                        1f, 0.5f))
                .with(ObjectAnimator.ofFloat(imageView,
                        View.SCALE_Y, 1, 0.5f));
        set.setDuration(500L);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ((ViewGroup)mRootView).removeView(imageView);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                ((ViewGroup)mRootView).removeView(imageView);
            }

            @Override
            public void onAnimationStart(Animator animation) {

            }
        });

        Animator animator = ObjectAnimator.ofFloat(mFALayout, View.ALPHA, 1f, 0f);
        animator.setDuration(500L);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mFATextureView != null && mFALayout != null) {
                    mFALayout.removeView(mFATextureView);
                }
                if (mFALayout != null) {
                    mFALayout.setVisibility(View.GONE);
                }
                set.start();
            }
        });
        animator.start();
    }

    public void hideFocusAssistView() {
        if (mFALayout != null) {
            animateFAImageOut();
        }
    }

    private TextureView mDepthTextureView;
    private ViewGroup.LayoutParams mPreviewLayoutParams;
    private Switch mDepthSwitch;

    private TextView mDepthFps;

    private SeekBar mDepthSeekBar;

    private ImageView mDepthSetting;
    private int[] mSwitchMargin = new int[2];
    private int[] mSettingMargin = new int[2];
    private int[] mSeekBarMargin = new int[2];
    public int[] getSwitchMargin(){
        return mSwitchMargin;
    }
    public int[] getSettingMargin(){
        return mSettingMargin;
    }
    public int[] getBarMargin(){
        return mSeekBarMargin;
    }
    public Switch getDepthSwitch(){
        return mDepthSwitch;
    }

    public void showDepthView(TextureView.SurfaceTextureListener listener) {
        if (mDepthTextureView == null) {
            ViewStub viewStub = mRootView.findViewById(R.id.depth_view_stub);
            viewStub.inflate();
            mDepthTextureView = mRootView.findViewById(R.id.depth_preview_texture_view);
        }
        hideUIInDepth();

        FrameLayout.LayoutParams params1 =
                new FrameLayout.LayoutParams(
                        mPreviewHeight,
                        mPreviewWidth,
                        Gravity.CENTER);
        mDepthTextureView.setLayoutParams(params1);
        mDepthTextureView.setSurfaceTextureListener(listener);
        mDepthTextureView.setVisibility(View.VISIBLE);

        if (mPreviewLayoutParams == null) {
            Point p = getPointInScreen(0, 0);
            int height = p.y;
            int width = (int) (height * 1.0f  / mPreviewWidth * mPreviewHeight);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            width,
                            height,
                            Gravity.TOP | Gravity.START);
            params.setMargins(0, height, 0, 0);

            if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
                mPreviewLayoutParams = mSurfaceView.getLayoutParams();
                mSurfaceView.setLayoutParams(params);
            } else {
                mPreviewLayoutParams = mTextureView.getLayoutParams();
                mTextureView.setLayoutParams(params);
            }
        }

       // mCameraControls.setVisibility(View.GONE);
        mGestures.setEnabled(false);

        if (mDepthSetting == null) {
            mDepthSetting = new ImageView(mActivity);
            FrameLayout.LayoutParams params_ =
                    new FrameLayout.LayoutParams(
                            CameraUtil.dpToPixel(25),
                            CameraUtil.dpToPixel(25),
                            Gravity.TOP | Gravity.END);
            params_.topMargin = 100;
            params_.rightMargin = 50;

            mDepthSetting.setLayoutParams(params_);
            mDepthSetting.setImageResource(R.drawable.settings);
            mDepthSetting.setOnClickListener((view) -> {
                openSettingsMenu();
            });
            ((ViewGroup) mRootView).addView(mDepthSetting);
            mSettingMargin[0]= params_.rightMargin;
            mSettingMargin[1]= params_.topMargin;;
        }

        if (mDepthSwitch == null) {
            mDepthSwitch = new Switch(mActivity);
            mDepthSwitch.setTextOn("HW");
            mDepthSwitch.setTextOff("SW");
            FrameLayout.LayoutParams params2 =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP | Gravity.END);
            params2.topMargin = 100;
            params2.rightMargin = 200;
            mDepthSwitch.setLayoutParams(params2);
            mSwitchMargin[0] =  params2.rightMargin;
            mSwitchMargin[1] =  params2.topMargin;
            mDepthSwitch.setChecked(mSettingsManager.getDepthMode() == 2);
            ((ViewGroup) mRootView).addView(mDepthSwitch);
            mDepthSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mSettingsManager.setDepthMode(isChecked ? 2 : 0);
                    if (mDepthTextureView != null) {
                        mDepthTextureView.setVisibility(View.GONE);
                    }
                    mModule.onDepthEngineChanged(isChecked ? 2 : 0);
                }
            });
        }

        if (mDepthFps == null) {
            mDepthFps = new TextView(mActivity);
            FrameLayout.LayoutParams params2 =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP | Gravity.START);
            params2.topMargin = 100;
            params2.leftMargin = 100;
            mDepthFps.setLayoutParams(params2);
            mDepthFps.setTextColor(Color.RED);
            mDepthFps.setTextSize(18f);
            ((ViewGroup) mRootView).addView(mDepthFps);
        }

        if (mDepthSeekBar == null) {
            mDepthSeekBar = new SeekBar(mActivity);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM | Gravity.START);
            params.bottomMargin = 500;
            params.leftMargin = 100;
            params.rightMargin = 100;
            mDepthSeekBar.setLayoutParams(params);
            mDepthSeekBar.setMax(3000);
            mDepthSeekBar.setMin(500);
            mSeekBarMargin[0] = params.rightMargin;
            mSeekBarMargin[1] = params.bottomMargin;
            mDepthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Log.i(TAG, "onProgressChanged " + progress);
                    mModule.onDepthFocusChanged(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            ((ViewGroup) mRootView).addView(mDepthSeekBar);
            mDepthSeekBar.setProgress(1000);
        }

    }

    public void hideDepthView() {
        if (mDepthTextureView != null) {
            mDepthTextureView.setVisibility(View.GONE);
        }

        if (mPreviewLayoutParams != null) {
            if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
                mSurfaceView.setLayoutParams(mPreviewLayoutParams);
            } else {
                mTextureView.setLayoutParams(mPreviewLayoutParams);
            }
            mPreviewLayoutParams = null;
        }

        if (mDepthSetting != null) {
            ((ViewGroup) mRootView).removeView(mDepthSetting);
            mDepthSetting = null;
        }

        if (mDepthSwitch != null) {
            ((ViewGroup) mRootView).removeView(mDepthSwitch);
            mDepthSwitch = null;
        }

        if (mDepthFps != null) {
            ((ViewGroup) mRootView).removeView(mDepthFps);
            mDepthFps = null;
        }

        if (mDepthSeekBar != null) {
            ((ViewGroup) mRootView).removeView(mDepthSeekBar);
            mDepthSeekBar = null;
        }

        mGestures.setEnabled(true);
    }

    public void showControlUI() {
        if (mCameraControls != null &&
                mModule.getCurrenCameraMode() != CaptureModule.CameraMode.DEPTH &&
                mCameraControls.getVisibility() != View.VISIBLE) {
            mCameraControls.setVisibility(View.VISIBLE);
        }
    }

    public void updateDepthFps(float fps) {
        Log.v(TAG, "updateDepthFps " + fps);
        if (mDepthFps != null) {
            if (mSettingsManager.getDepthMode() == 0) {
                fps = fps / 2f;
            }
            if (fps == 0f) {
                mDepthFps.setText("");
            } else {
                mDepthFps.setText(String.format(Locale.getDefault(), "FPS:%.2f%n", fps));
            }
        }
    }

    public int getDepthProgress() {
        if (mDepthSeekBar != null) {
            return mDepthSeekBar.getProgress();
        }
        return 1000;
    }

    public void showEvSeekbar(int x, int y) {
        if (mModule.getCurrenCameraMode() == CaptureModule.CameraMode.PRO_MODE ||
                mModule.getCurrenCameraMode() == CaptureModule.CameraMode.DEPTH) return;
        initEvSeekBar();
        int evX = x - mPieRenderer.getSize() / 2 - mPieRenderer.getSize() / 5;
        int evY = y + mPieRenderer.getSize() / 2;
        int zoombarlocation[] = new int[2];
        mZoomLinearLayout.getLocationOnScreen(zoombarlocation);
        if (evY > zoombarlocation[1]) {
            evY = y - mPieRenderer.getSize();
        }
        mEvSeekBar.setX(evX);
        mEvSeekBar.setY(evY);
        mEvSeekBar.setVisibility(View.VISIBLE);
    }

    public void hideEvSeekbar() {
        if (mEvSeekBar != null && mEvSeekBar.getVisibility() == View.VISIBLE) {
            mEvSeekBar.setVisibility(View.GONE);
            resetEv();
        }
        hideFocusAssistText();
    }
    @Override
    public boolean hasFaces() {
        return (mFaceView != null && mFaceView.faceExists());
    }

    public void clearFaces() {
        if (mFaceView != null) mFaceView.clear();
    }

    @Override
    public void clearFocus() {
        ArrayList<FocusIndicator> indicators = getFocusIndicator();
        for (FocusIndicator indicator : indicators) {
            if (indicator != null) indicator.clear();
        }
        mIsTouchAF = false;
    }
    @Override
    public void resetFocus() {
        ArrayList<FocusIndicator> indicators = getFocusIndicator();
        for (FocusIndicator indicator : indicators) {
            if (indicator != null) indicator.reset();
        }
        mIsTouchAF = false;
    }

    @Override
    public void setFocusPosition(int x, int y) {
        mPieRenderer.setFocus(x, y);
        mIsTouchAF = true;
        mFocusPoint = new Point(x, y);
    }

    public void setFocusPointInPreview(int x, int y) {
        mFocusPointInPreview = new Point(x, y);
    }

    public Point getPointInScreen(int x, int y) {
        int[] surfaceViewLocation = new int[2];
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getLocationInWindow(surfaceViewLocation);
        } else {
            mTextureView.getLocationInWindow(surfaceViewLocation);
        }
        int surfaceViewX = surfaceViewLocation[0];
        int surfaceViewY = surfaceViewLocation[1];
        return new Point(surfaceViewX + x, surfaceViewY + y);
    }

    @Override
    public void onFocusStarted() {
        ArrayList<FocusIndicator> indicators = getFocusIndicator();
        for (FocusIndicator indicator : indicators) {
            if (indicator != null) indicator.showStart();
        }
    }

    @Override
    public void onFocusFailed(boolean timeOut) {
        ArrayList<FocusIndicator> indicators = getFocusIndicator();
        for (FocusIndicator indicator : indicators) {
            if (indicator != null) indicator.showFail(timeOut);
        }
    }

    @Override
    public void onFocusSucceeded(boolean timeout) {
        ArrayList<FocusIndicator> indicators = getFocusIndicator();
        for (FocusIndicator indicator : indicators) {
            if (indicator != null) indicator.showSuccess(timeout);
        }
    }

    @Override
    public void pauseFaceDetection() {

    }

    @Override
    public void resumeFaceDetection() {
    }

    public void onStartFaceDetection(int orientation, boolean mirror, Rect cameraBound,
                                     Rect originalCameraBound) {
        mFaceView.setBlockDraw(false);
        mFaceView.clear();
        mFaceView.setVisibility(View.VISIBLE);
        mFaceView.setDisplayOrientation(orientation);
        mFaceView.setMirror(mirror);
        mFaceView.setCameraBound(cameraBound);
        mFaceView.setOriginalCameraBound(originalCameraBound);
        mFaceView.setZoomRationSupported(getZoomFixedSupport());
        float zoomValue = mModule.getZoomValue();
        if (zoomValue < 1.0f) {
            zoomValue = 1.0f;
        }
        if(getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV()) {
            zoomValue = 1.0f;
        }
        mFaceView.setZoom(zoomValue);
        mFaceView.resume();
        mFaceUpdated = true;
    }

    public void updateFaceViewCameraBound(Rect cameraBound) {
        mFaceView.setCameraBound(cameraBound);
        float zoomValue = mModule.getZoomValue();
        if (zoomValue < 1.0f) {
            zoomValue = 1.0f;
        }
        if(getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV()) {
            zoomValue = 1.0f;
        }
        mFaceView.setZoom(zoomValue);
    }

    public void onStopFaceDetection() {
        if (mFaceView != null) {
            mFaceView.setBlockDraw(true);
            mFaceView.clear();
        }
    }

    @Override
    public void onFaceDetection(Face[] faces, CameraManager.CameraProxy camera) {
    }

    public void onFaceDetection(android.hardware.camera2.params.Face[] faces,
                                ExtendedFace[] extendedFaces) {
        mFaceView.setFaces(faces,extendedFaces);
    }

    public void onFacialMaskDetection(int[] facialMasks, int maskNums) {
        mFaceView.setFacialMasks(facialMasks, maskNums);
    }
    public void onUpperBodyDetection(int headNums, int[] headInts,
                                     int[] torsoValidInts, int[] torsoInts) {
        mFaceView.setUpperBodys(headNums, headInts, torsoValidInts, torsoInts);
    }
    public void onPetDetection(int[] headInts,int[] torsoInts, int[] markInts) {
        mFaceView.setPetParams(headInts, torsoInts, markInts);
    }

    public Point getSurfaceViewSize() {
        Point point = new Point();
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            if (mSurfaceView != null)
                point.set(mSurfaceView.getWidth(), mSurfaceView.getHeight());
        } else {
            if (mTextureView != null) {
                point.set(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }
        return point;
    }

    public void adjustOrientation() {
        setOrientation(mOrientation, true);
    }

    public void setOrientation(int orientation, boolean animation) {
        mOrientation = orientation;
        mCameraControls.setOrientation(orientation, animation);
        if (mMenuHelp != null) {
            mMenuHelp.setOrientation(orientation, animation);
        }
        if (mFilterLayout != null) {
            ViewGroup vg = (ViewGroup) mFilterLayout.getChildAt(0);
            if (vg != null)
                vg = (ViewGroup) vg.getChildAt(0);
            if (vg != null) {
                for (int i = vg.getChildCount() - 1; i >= 0; --i) {
                    RotateLayout l = (RotateLayout) vg.getChildAt(i);
                    l.setOrientation(orientation, animation);
                }
            }
        }
        if (mRecordingTimeRect != null) {
            mRecordingTimeView.setRotation(-orientation);
            if (mTimeLapseLabel != null && mTimeLapseLabel.getVisibility() == View.VISIBLE) {
                mTimeLapseLabel.setRotation(-orientation);
            }
        }
        if (mFaceView != null) {
            mFaceView.setDisplayRotation(orientation);
        }
        if (mCountDownView != null)
            mCountDownView.setOrientation(orientation);
        RotateTextToast.setOrientation(orientation);
        if (mZoomRenderer != null) {
            mZoomRenderer.setOrientation(orientation);
        }

        if ( mSceneModeLabelRect != null ) {
            if (orientation == 180) {
                mSceneModeName.setRotation(180);
                mSceneModeLabelCloseIcon.setRotation(180);
                mSceneModeLabelRect.setOrientation(0, false);
            } else {
                mSceneModeName.setRotation(0);
                mSceneModeLabelCloseIcon.setRotation(0);
                mSceneModeLabelRect.setOrientation(orientation, false);
            }
        }
        if ( mDeepZoomModeRect != null ) {
            if (orientation == 180) {
                mDeepzoomSetName.setRotation(180);
                mDeepZoomModeRect.setOrientation(0, false);
            } else {
                mDeepzoomSetName.setRotation(0);
                mDeepZoomModeRect.setOrientation(orientation, false);
            }
        }

        if ( mSceneModeInstructionalDialog != null && mSceneModeInstructionalDialog.isShowing()) {
            mSceneModeInstructionalDialog.dismiss();
            mSceneModeInstructionalDialog = null;
            showSceneInstructionalDialog(orientation);
        }

        if (mFocusAssistTextView != null && mFocusAssistTextView.getVisibility() == View.VISIBLE) {
            mFocusAssistTextView.setOrientation(orientation, animation);
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void showFirstTimeHelp() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean isMenuShown = prefs.getBoolean(CameraSettings.KEY_SHOW_MENU_HELP, false);
        if(!isMenuShown && isShowHelp()) {
            showFirstTimeHelp(mTopMargin, mBottomMargin);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(CameraSettings.KEY_SHOW_MENU_HELP, true);
            editor.apply();
        }
    }

    private void showFirstTimeHelp(int topMargin, int bottomMargin) {
        mMenuHelp = (MenuHelp) mRootView.findViewById(R.id.menu_help);
        mMenuHelp.setForCamera2(true);
        mMenuHelp.setVisibility(View.VISIBLE);
        mMenuHelp.setMargins(topMargin, bottomMargin);
        mMenuHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenuHelp != null) {
                    mMenuHelp.setVisibility(View.GONE);
                    mMenuHelp = null;
                }
            }
        });
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        hideFocusAssistText();
        mModule.onSingleTapUp(view, x, y);
    }

    @Override
    public void onLongPress(View view, int x, int y) {
        mModule.onLongPress(view, x, y);
    }

    public boolean isOverControlRegion(int[] xy) {
        int x = xy[0];
        int y = xy[1];
        return mCameraControls.isControlRegion(x, y);
    }

    public boolean isOverSurfaceView(int[] xy) {
        int x = xy[0];
        int y = xy[1];
        int[] surfaceViewLocation = new int[2];
        int width = 0;
        int height =0;
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getLocationInWindow(surfaceViewLocation);
            width = mSurfaceView.getWidth();
            height = mSurfaceView.getHeight();
        } else {
            mTextureView.getLocationInWindow(surfaceViewLocation);
            width = mTextureView.getWidth();
            height= mTextureView.getHeight();
        }
        int surfaceViewX = surfaceViewLocation[0];
        int surfaceViewY = surfaceViewLocation[1];
        xy[0] = x - surfaceViewX;
        xy[1] = y - surfaceViewY;
        return (x > surfaceViewX) && (x < surfaceViewX + width)
                && (y > surfaceViewY) && (y < surfaceViewY + height);
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (mFaceView != null) {
            mFaceView.setBlockDraw(!previewFocused);
        }
        if (mGestures != null) {
            mGestures.setEnabled(previewFocused);
        }
        if (mRenderOverlay != null && !mModule.isDeepPortraitMode()) {
            // this can not happen in capture mode
            mRenderOverlay.setVisibility(previewFocused ? View.VISIBLE : View.GONE);
        }
        if (mPieRenderer != null) {
            mPieRenderer.setBlockFocus(!previewFocused);
        }
        if (!previewFocused && mCountDownView != null) mCountDownView.cancelCountDown();
    }

    public boolean isShutterPressed() {
        return mShutterButton.isPressed();
    }

    public void pressShutterButton() {
        if (mShutterButton.isInTouchMode()) {
            mShutterButton.requestFocusFromTouch();
        } else {
            mShutterButton.requestFocus();
        }
        mShutterButton.setPressed(true);
    }

    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        mRecordingTimeView.setTextColor(color);
    }

    public void resetPauseButton() {
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_recording_indicator, 0, 0, 0);
        mPauseButton.setPaused(false);
    }

    @Override
    public void onButtonPause() {
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pausing_indicator, 0, 0, 0);
        mModule.onButtonPause();
    }

    @Override
    public void onButtonContinue() {
        mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_recording_indicator, 0, 0, 0);
        mModule.onButtonContinue();
    }
    private boolean isShowHelp(){
        if(PersistUtil.isPerfTestRunning() || PersistUtil.isFuncTestRunning() ||
                PersistUtil.isStressTestRunning()){
            return false;
        }
        return true;
    }
    @Override
    public void onSettingsChanged(List<SettingsManager.SettingState> settings) {
        for( SettingsManager.SettingState state : settings) {
            if (state.key.equals(SettingsManager.KEY_COLOR_EFFECT)) {
                enableView(mFilterModeSwitcher, SettingsManager.KEY_COLOR_EFFECT);
            } else if (state.key.equals(SettingsManager.KEY_SCENE_MODE)) {
                String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
                if ( value.equals("104") ) {//panorama
                    mSceneModeLabelRect.setVisibility(View.GONE);
                }else{
                    if ( needShowInstructional() && isShowHelp() ) {
                        showSceneInstructionalDialog(mOrientation);
                    }
                    if(value.equals("18")) {//hdr
                        hideVerticalEv();
                    }
                }
            }else if(state.key.equals(SettingsManager.KEY_FLASH_MODE) ) {
                enableView(mFlashButton, SettingsManager.KEY_FLASH_MODE);
            }else if (state.key.equals(SettingsManager.KEY_FOCUS_DISTANCE)) {
                if (mPieRenderer != null)
                    mPieRenderer.setVisible(false);
            }
        }
    }

    public void startSelfieFlash() {
        if (mSelfieView == null)
            mSelfieView = (SelfieFlashView) (mRootView.findViewById(R.id.selfie_flash));
        mSelfieView.bringToFront();
        mSelfieView.open();
        mScreenBrightness = setScreenBrightness(1F);
    }

    public void stopSelfieFlash() {
        if (mSelfieView == null)
            mSelfieView = (SelfieFlashView) (mRootView.findViewById(R.id.selfie_flash));
        mSelfieView.close();
        if (mScreenBrightness != 0.0f)
            setScreenBrightness(mScreenBrightness);
    }

    private float setScreenBrightness(float brightness) {
        float originalBrightness;
        Window window = mActivity.getWindow();
        WindowManager.LayoutParams layout = window.getAttributes();
        originalBrightness = layout.screenBrightness;
        layout.screenBrightness = brightness;
        window.setAttributes(layout);
        return originalBrightness;
    }

    public void hideSurfaceView() {
        Log.d(TAG, "hideSurfaceView");
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.setVisibility(View.GONE);
        } else {
            mTextureView.setVisibility(View.GONE);
        }
    }

    public void showSurfaceView() {
        Log.d(TAG, "surfceView-setFixedSize = " + mPreviewWidth + "x" + mPreviewHeight);
        if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
            mSurfaceView.getHolder().setFixedSize(mPreviewWidth, mPreviewHeight);
            mSurfaceView.setAspectRatio(mPreviewHeight, mPreviewWidth);
            mSurfaceView.setVisibility(View.VISIBLE);
        } else {
            mTextureView.setAspectRatio(mPreviewHeight, mPreviewWidth);
            mTextureView.setVisibility(View.VISIBLE);
        }
        mIsVideoUI = false;
    }
    public void hideGridLineView() {
        mGridLineView.setVisibility(View.INVISIBLE);
    }

    public void updateGridLine(){
        String value = mSettingsManager.getValue(SettingsManager.KEY_GRIDLINE);
        if (value != null && value.equals("on")){
            mGridLineView.setVisibility(View.VISIBLE);
            int height = getScreenWidth() * mPreviewWidth / mPreviewHeight;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getScreenWidth(), height);
            mGridLineView.setLayoutParams(params);
            int top = 0;
            if (!USE_TEXTURE_VIEW_TO_PREVIEW) {
                top = mSurfaceView.getTop();
            } else {
                top = mTextureView.getTop();
            }
            mGridLineView.setY(top);
        }
    }

    public boolean setPreviewSize(int width, int height) {
        Log.i(TAG, "setPreviewSize " + width + "x" + height + ", old " + mPreviewWidth + "x" + mPreviewHeight);
        boolean changed = (width != mPreviewWidth) || (height != mPreviewHeight);
        mPreviewWidth = width;
        mPreviewHeight = height;
        if (changed) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    showSurfaceView();
                }
            });
        }
        return changed;
    }

    private class ZoomChangeListener implements ZoomRenderer.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(float mZoomValue) {
            if(mModule.onZoomChanged(mZoomValue)) {
                if (mZoomRenderer != null) {
                    mZoomRenderer.setZoom(mZoomValue);
                }
            }
        }

        @Override
        public void onZoomStart() {
            if (mPieRenderer != null) {
                mPieRenderer.hide();
                mPieRenderer.setBlockFocus(true);
            }
        }

        @Override
        public void onZoomEnd() {
            if (mPieRenderer != null) {
                mPieRenderer.setBlockFocus(false);
            }
            mModule.onZoomEnd();
        }

        @Override
        public void onZoomValueChanged(int index) {

        }
    }

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private int mOrientation;
        public DecodeTask(byte[] data, int orientation) {
            mData = data;
            mOrientation = orientation;
        }
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = CameraUtil.downSample(mData, mDownSampleFactor);
            // Decode image in background.
            if ((mOrientation != 0) && (bitmap != null)) {
                Matrix m = new Matrix();
                m.preRotate(mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                        false);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
        }
    }

    private class DecodeImageForReview extends CaptureUI.DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation) {
            super(data, orientation);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                return;
            }
            mReviewImage.setImageBitmap(bitmap);
            mReviewImage.setVisibility(View.VISIBLE);
            mDecodeTaskForReview = null;
        }
    }
    private class ProgressRunable implements Runnable {
        @Override
        public void run() {
            while (mCurrentProgress <= mTotalProgress) {
                mCameraControls.setShutterProgress(mCurrentProgress, mTotalProgress);
                try {
                    Thread.sleep(200);
                    mCurrentProgress += 200;
                    if (mCurrentProgress > mTotalProgress) {
                        mCurrentProgress = mTotalProgress;
                        mCameraControls.setShutterProgress(mCurrentProgress, mTotalProgress);
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG,e);
                }
            }
        }
    }

    public ImageView getVideoButton() {
        return mVideoButton;
    }

    public int getCurrentProMode() {
        return mCameraControls.getPromode();
    }

    public void swipeCameraMode(int move) {
        if(mActivity.getPerformenceTest()) {
            mModule.setStartedTime(System.currentTimeMillis());
        }
        if (mIsVideoUI || !mModule.getCameraModeSwitcherAllowed() ||
                mModule.getCurrentIntentMode() != CaptureModule.INTENT_MODE_NORMAL) {
            return;
        }
        int index = mModule.getCurrentModeIndex() + move;
        int modeListSize = mModule.getCameraModeList().size();
        if (index >= modeListSize || index == -1) {
            return;
        }
        int mode = index % modeListSize;
        mModule.setCameraModeSwitcherAllowed(false);
        mCameraModeAdapter.setSelectedPosition(mode);
        mModeSelectLayout.smoothScrollToPosition(mode);
        mModule.selectCameraMode(mode);
    }

    public void smoothSelectedPosition(int mode ) {
        mModeSelectLayout.smoothScrollToPosition(mode);
    }

    public void switchToPhotoModeDueToError(boolean switchCamera) {
        int photoModeIndex = 1;
        List<String> modeList = mModule.getCameraModeList();
        for (; photoModeIndex < modeList.size(); photoModeIndex++) {
            if (modeList.get(photoModeIndex).equals(
                    mModule.getSelectableModes()[CaptureModule.CameraMode.DEFAULT.ordinal()])) {
                break;
            }
        }
        mCameraModeAdapter.setSelectedPosition(photoModeIndex);
        mModeSelectLayout.smoothScrollToPosition(photoModeIndex);
        if (switchCamera) {
            mModule.selectCameraMode(photoModeIndex);
        } else {
            mModule.setNextSceneMode(photoModeIndex);
        }
    }
    public void setSoundEffectsForRecording(boolean enabled) {
        if (mShutterButton != null) {
            mShutterButton.setSoundEffectsEnabled(enabled);
        }
        if (mMuteButton != null) {
            mMuteButton.setSoundEffectsEnabled(enabled);
        }
        if (mFlashButton != null) {
            mFlashButton.setSoundEffectsEnabled(enabled);
        }
        if (mThumbnail != null) {
            mThumbnail.setSoundEffectsEnabled(enabled);
        }
    }

    public OneUICameraControls getmCameraControls(){
        return mCameraControls;
    }

    public FilmstripContentPanel getFilmstripContentPanel() {
        return mFilmstripPanel;
    }

    /**
     * Call to stop the preview from being rendered. Sets the entire capture
     * root view to invisible which includes the preview plus focus indicator
     * and any other auxiliary views for capture modes.
     */
    public void pausePreviewRendering() {
        mCameraRootView.setVisibility(View.INVISIBLE);
    }

    /**
     * Call to begin rendering the preview and auxiliary views again.
     */
    public void resumePreviewRendering() {
        mCameraRootView.setVisibility(View.VISIBLE);
    }

    /**
     * @return The {@link com.android.camera.app.CameraAppUI.BottomPanel} on the
     * bottom of the filmstrip.
     */
    public BottomPanel getFilmstripBottomControls() {
        return mFilmstripBottomControls;
    }

    public void showBottomControls() {
        mFilmstripBottomControls.show();
    }

    public void hideBottomControls() {
        mFilmstripBottomControls.hide();
    }

    /**
     * @param listener The listener for bottom controls.
     */
    public void setFilmstripBottomControlsListener(BottomPanel.Listener listener) {
        mFilmstripBottomControls.setListener(listener);
    }

    /**
     * Clears the listeners for the cling and remove it from the view hierarchy.
     *
     * @param viewerType defines which viewer the cling is for.
     */
    public void clearClingForViewer(int viewerType) {
        Cling clingToBeRemoved = mFilmstripBottomControls.getClingForViewer(viewerType);
        if (clingToBeRemoved == null) {
            // No cling is created for the specific viewer type.
            return;
        }
        mFilmstripBottomControls.clearClingForViewer(viewerType);
        clingToBeRemoved.setVisibility(View.GONE);
        mCameraRootView.removeView(clingToBeRemoved);
    }

    /**
     * The bottom controls on the filmstrip.
     */
    public static interface BottomPanel {
        /** Values for the view state of the button. */
        public final int VIEWER_NONE = 0;
        public final int VIEWER_PHOTO_SPHERE = 1;
        public final int VIEWER_REFOCUS = 2;
        public final int VIEWER_OTHER = 3;

        /**
         * Sets a new or replaces an existing listener for bottom control events.
         */
        void setListener(Listener listener);

        /**
         * Sets cling for external viewer button.
         */
        void setClingForViewer(int viewerType, Cling cling);

        /**
         * Clears cling for external viewer button.
         */
        void clearClingForViewer(int viewerType);

        /**
         * Returns a cling for the specified viewer type.
         */
        Cling getClingForViewer(int viewerType);

        /**
         * Set if the bottom controls are visible.
         * @param visible {@code true} if visible.
         */
        void setVisible(boolean visible);

        /**
         * @param visible Whether the button is visible.
         */
        void setEditButtonVisibility(boolean visible);

        /**
         * @param enabled Whether the button is enabled.
         */
        void setEditEnabled(boolean enabled);

        /**
         * Sets the visibility of the view-photosphere button.
         *
         * @param state one of {@link #VIEWER_NONE}, {@link #VIEWER_PHOTO_SPHERE},
         *            {@link #VIEWER_REFOCUS}.
         */
        void setViewerButtonVisibility(int state);

        /**
         * @param enabled Whether the button is enabled.
         */
        void setViewEnabled(boolean enabled);

        /**
         * @param enabled Whether the button is enabled.
         */
        void setTinyPlanetEnabled(boolean enabled);

        /**
         * @param visible Whether the button is visible.
         */
        void setDeleteButtonVisibility(boolean visible);

        /**
         * @param enabled Whether the button is enabled.
         */
        void setDeleteEnabled(boolean enabled);

        /**
         * @param visible Whether the button is visible.
         */
        void setShareButtonVisibility(boolean visible);

        /**
         * @param enabled Whether the button is enabled.
         */
        void setShareEnabled(boolean enabled);

        /**
         * Sets the texts for progress UI.
         *
         * @param text The text to show.
         */
        void setProgressText(CharSequence text);

        /**
         * Sets the progress.
         *
         * @param progress The progress value. Should be between 0 and 100.
         */
        void setProgress(int progress);

        /**
         * Replaces the progress UI with an error message.
         */
        void showProgressError(CharSequence message);

        /**
         * Hide the progress error message.
         */
        void hideProgressError();

        /**
         * Shows the progress.
         */
        void showProgress();

        /**
         * Hides the progress.
         */
        void hideProgress();

        /**
         * Shows the controls.
         */
        void showControls();

        /**
         * Hides the controls.
         */
        void hideControls();

        /**
         * Classes implementing this interface can listen for events on the bottom
         * controls.
         */
        public static interface Listener {
            /**
             * Called when the user pressed the "view" button to e.g. view a photo
             * sphere or RGBZ image.
             */
            public void onExternalViewer();

            /**
             * Called when the "edit" button is pressed.
             */
            public void onEdit();

            /**
             * Called when the "tiny planet" button is pressed.
             */
            public void onTinyPlanet();

            /**
             * Called when the "delete" button is pressed.
             */
            public void onDelete();

            /**
             * Called when the "share" button is pressed.
             */
            public void onShare();

            /**
             * Called when the progress error message is clicked.
             */
            public void onProgressErrorClicked();
        }
    }
}
