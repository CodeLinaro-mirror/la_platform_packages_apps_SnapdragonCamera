/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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
 *
 */
/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2022-2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */


package com.android.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothLeAudio;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.MultiResolutionImageReader;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.MultiResolutionStreamConfigurationMap;
import android.hardware.camera2.params.MultiResolutionStreamInfo;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.LensShadingMap;
import android.location.Location;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.VideoCapabilities;
import android.media.MediaCodecList;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import com.android.camera.gles.DepthRender;
import com.android.camera.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Pair;

import com.android.camera.app.CameraApp;
import com.android.camera.data.Camera2ModeAdapter.OnItemClickListener;
import com.android.camera.deepportrait.CamGLRenderObserver;
import com.android.camera.deepportrait.CamGLRenderer;
import com.android.camera.deepportrait.DPImage;
import com.android.camera.deepportrait.GLCameraPreview;
import com.android.camera.gles.CameraRender;
import com.android.camera.imageprocessor.filter.BlurbusterFilter;
import com.android.camera.imageprocessor.filter.ChromaflashFilter;
import com.android.camera.imageprocessor.filter.DeepPortraitFilter;
import com.android.camera.imageprocessor.filter.ImageFilter;
import com.android.camera.imageprocessor.PostProcessor;
import com.android.camera.imageprocessor.FrameProcessor;
import com.android.camera.PhotoModule.NamedImages;
import com.android.camera.PhotoModule.NamedImages.NamedEntity;
import com.android.camera.imageprocessor.filter.SharpshooterFilter;
import com.android.camera.imageprocessor.filter.StillmoreFilter;
import com.android.camera.imageprocessor.filter.UbifocusFilter;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.ui.ProMode;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.TrackingFocusRenderer;
import com.android.camera.ui.TouchTrackFocusRenderer;
import com.android.camera.ui.StateNNTrackFocusRenderer;
import com.android.camera.util.AccessibilityUtils;
import com.android.camera.ui.AFView;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.AutoTestUtil;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PersistUtil;
import com.android.camera.util.SettingTranslation;

import com.android.camera.util.VendorTagUtil;
import com.android.camera.aide.AideUtil;
import com.android.camera.aide.AideUtil.*;

import org.codeaurora.snapcam.R;
import org.codeaurora.snapcam.filter.ClearSightImageProcessor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.Set;
import java.util.HashMap;
import android.widget.SeekBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.annotation.NonNull;
import androidx.heifwriter.HeifWriter;
import com.android.camera.ui.OneUICameraControls;
import qti.video.QMediaCodecCapabilities;
import android.hardware.camera2.CameraDevice.CameraDeviceSetup;
import android.os.Build;
import java.util.Collections;

public class CaptureModule implements CameraModule, PhotoController,
        MediaSaveService.Listener, ClearSightImageProcessor.Callback,
        SettingsManager.Listener, LocationManager.Listener,
        CountDownView.OnCountDownFinishedListener,
        MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener,
        CamGLRenderObserver {
    public static final int DUAL_MODE = 0;
    public static final int BAYER_MODE = 1;
    public static final int MONO_MODE = 2;
    public static final int SWITCH_MODE = 3;
    public static final int BAYER_ID = 0;
    public static int CURRENT_ID = 0;
    public static CameraMode CURRENT_MODE = CameraMode.DEFAULT;
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_RTB = 1;
    public static final int TYPE_SAT = 2;
    public static final int TYPE_VR360 = 3;
    public static int MONO_ID = -1;
    public static int FRONT_ID = -1;
    public static int SWITCH_ID = -1;
    public static int LOGICAL_ID = -1;
    public static final int INTENT_MODE_NORMAL = 0;
    public static final int INTENT_MODE_CAPTURE = 1;
    public static final int INTENT_MODE_VIDEO = 2;
    public static final int INTENT_MODE_CAPTURE_SECURE = 3;
    public static final int INTENT_MODE_STILL_IMAGE_CAMERA = 4;
    private static final int BACK_MODE = 0;
    private static final int FRONT_MODE = 1;
    private static final int CANCEL_TOUCH_FOCUS_DELAY = PersistUtil.getCancelTouchFocusDelay();
    private static final int OPEN_CAMERA = 0;
    private static final int CANCEL_TOUCH_FOCUS = 1;
    private static final int MAX_NUM_CAM = 16;
    private static final int CONTOUR_POINTS_COUNT = 89;
    private String DEPTH_CAM_ID = null;
    private static final MeteringRectangle[] ZERO_WEIGHT_3A_REGION = new MeteringRectangle[]{
            new MeteringRectangle(0, 0, 0, 0, 0)};
    private static final String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";
    private static final int SESSION_REGULAR = 0;
    private static final int SESSION_HIGH_SPEED = 1;
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_AF_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be locked.
     */
    private static final int STATE_WAITING_AE_LOCK = 3;
    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    /**
     * Camera state: Waiting for the touch-to-focus to converge.
     */
    private static final int STATE_WAITING_TOUCH_FOCUS = 5;
    /**
     * Camera state: Focus and exposure has been locked and converged.
     */
    private static final int STATE_AF_AE_LOCKED = 6;
    private static final int STATE_WAITING_AF_LOCKING = 7;
    private static final int STATE_WAITING_AF_AE_LOCK = 8;
    private static final int STATE_WAITING_AE_PRECAPTURE = 9;
    private static final int STATE_WAITING_AF_AE_RELEASE = 10;
    private static final String TAG = "SnapCam_CaptureModule";

    // Used for check memory status for longshot mode
    // Currently, this cancel threshold selection is based on test experiments,
    // we can change it based on memory status or other requirements.
    private static final int LONGSHOT_CANCEL_THRESHOLD = 40 * 1024 * 1024;

    private static final int NORMAL_SESSION_MAX_FPS = 90;
    private static final int HIGH_SESSION_MAX_FPS = 120;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private static int mShotNum = PersistUtil.getLongshotShotLimit();
    private boolean mLongshoting = false;
    private AtomicInteger mNumFramesArrived = new AtomicInteger(0);
    private AtomicInteger mNumImageArrived = new AtomicInteger(0);
    private final int MAX_IMAGEREADERS = 10;

    private long mVideoFrameNumber = 0;
    private boolean mIsRTBCameraId = false;
    private boolean mIsFacialMaskSupported = true;
    private boolean mIsUpperBodySupported = true;
    private boolean mIsPetDetectionSupported = true;
    private int mIsValidNum = 0;

    /** For temporary save warmstart gains and cct value*/
    private float mRGain = -1.0f;
    private float mGGain = -1.0f;
    private float mBGain = -1.0f;
    private float mCctAWB = -1.0f;
    private float[] mAWBDecisionAfterTC = new float[2];
    private float[] mAECSensitivity = new float[3];
    private float mAECLuxIndex = -1.0f;
    private float lux_index_threadhold = 320;
    private float mAdrcGain = -1.0f;
    private float mDarkBoostGain = -1.0f;
    private int mExposureCount = -1;
    private int mAECCameraId = -1;

    private long[] mAecFramecontrolExosureTime = new long[3];
    private float[] mAecFramecontrolLinearGain = new float[3];
    private float[] mAecFramecontrolSensitivity = new float[3];
    private int mAntiBandingMode = -1;
    private int mIsFickerDetected = -1;
    private float mAecFramecontrolLuxIndex = -1.0f;
    private boolean isflashRequired;
    public static final int MAX_LOGICAL_PHYSICAL_CAMERA_COUNT = 4;

    public static final int PHYSICAL_CAMERA_COUNT = MAX_LOGICAL_PHYSICAL_CAMERA_COUNT - 1;

    /** Add for EIS and FOVC Configuration */
    private int mStreamConfigOptMode = 0;
    private static final int STREAM_CONFIG_MODE_FOVC = 0xF010;
    private static final int STREAM_CONFIG_MODE_ZZHDR  = 0xF002;
    private static final int STREAM_CONFIG_MODE_FS2    =  0xF040;
    /** Add for SSM Configuration */
    private static final int STREAM_CONFIG_SSM = 0xF080;
    private int mCaptureCompleteCount = 0;
    private boolean mSSMCaptureCompleteFlag = false;
    private static final boolean TRACE_DEBUG = PersistUtil.getTraceDebug();

    private static final int FD_LOG = PersistUtil.CAMERA2_DEBUG_FD;
    private static final int MEDIACODEC_AUDIO_LOG = PersistUtil.CAMERA2_DEBUG_MEDIACODEC_AUDIO;
    private static final int MEDIACODEC_VIDEO_LOG = PersistUtil.CAMERA2_DEBUG_MEDIACODEC_VIDEO;
    private static final int EXCEPTION_LOG = PersistUtil.CAMERA2_DEBUG_EXCEPTION;
    private static final int BIG_LOG = PersistUtil.CAMERA2_DEBUG_BIGLOG;

    private static final String FD_TAG = "SnapCam_FD";
    private static final String HFR_RATE = PersistUtil.getHFRRate();

    private static long tapUpFrameNumber = 0;

    MeteringRectangle[][] mAFRegions = new MeteringRectangle[MAX_NUM_CAM][];
    MeteringRectangle[][] mAERegions = new MeteringRectangle[MAX_NUM_CAM][];
    MeteringRectangle[][] mT2TrackRegions = new MeteringRectangle[MAX_NUM_CAM][];
    CaptureRequest.Key<Byte> BayerMonoLinkEnableKey =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.dualcam_link_meta_data.enable",
                    Byte.class);
    CaptureRequest.Key<Byte> BayerMonoLinkMainKey =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.dualcam_link_meta_data.is_main",
                    Byte.class);
    CaptureRequest.Key<Integer> BayerMonoLinkSessionIdKey =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.dualcam_link_meta_data" +
                    ".related_camera_id", Integer.class);
    public static CameraCharacteristics.Key<Byte> MetaDataMonoOnlyKey =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.sensor_meta_data.is_mono_only",
                    Byte.class);
    public static CameraCharacteristics.Key<int[]> InstantAecAvailableModes =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.instant_aec.instant_aec_available_modes", int[].class);
    public static final CaptureRequest.Key<Integer> INSTANT_AEC_MODE =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.instant_aec.instant_aec_mode", Integer.class);
    public static final CaptureRequest.Key<Integer> SATURATION=
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.saturation.use_saturation", Integer.class);
    public static final CaptureRequest.Key<Byte> histMode =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.histogram.enable", byte.class);
    public static final CaptureRequest.Key<Byte> bgStatsMode =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_grid.enable", byte.class);
    public static final CaptureRequest.Key<Byte> beStatsMode =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_exposure.enable", byte.class);

    public static final CaptureRequest.Key<Integer> INTEGRATED_MODE =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.MultiCameraMode", Integer.class);

    public static CameraCharacteristics.Key<int[]> ISO_AVAILABLE_MODES =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.iso_available_modes", int[].class);
    public static CameraCharacteristics.Key<long[]> EXPOSURE_RANGE =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.exposure_time_range", long[].class);

    // manual WB color temperature and gains
    public static CameraCharacteristics.Key<int[]> WB_COLOR_TEMPERATURE_RANGE =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.manualWB.color_temperature_range", int[].class);
    public static CameraCharacteristics.Key<float[]> WB_RGB_GAINS_RANGE =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.manualWB.gains_range", float[].class);

    public static final CaptureRequest.Key<Byte> spatialVideo =
            new CaptureRequest.Key<>("com.qti.qualcomm.spatialVideo.SpatialVideoMode", byte.class);

    public static CaptureResult.Key<Integer> buckets =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.buckets", Integer.class);
    public static CameraCharacteristics.Key<Integer> maxCount =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.histogram.max_count", Integer.class);
    public static CaptureResult.Key<Integer> stats_type =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.stats_type",Integer.class);
    public static CaptureResult.Key<int[]> histogramStats =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.stats", int[].class);
    public static CaptureResult.Key<Integer> stats_width =
            new CaptureResult.Key<>("org.quic.camera2.statsVisualizer.StatsWidth",int.class);
    public static CaptureResult.Key<Integer> stats_height =
            new CaptureResult.Key<>("org.quic.camera2.statsVisualizer.StatsHeight",int.class);
    public static CaptureResult.Key<Integer> stats_bitdepth =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.bitDepth",int.class);

    public static CaptureResult.Key<int[]> bgRStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_grid.r_stats", int[].class);
    public static CaptureResult.Key<int[]> bgGStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_grid.g_stats", int[].class);
    public static CaptureResult.Key<int[]> bgBStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_grid.b_stats", int[].class);
    public static CaptureResult.Key<Integer> bgHeight =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_grid.height", int.class);
    public static CaptureResult.Key<Integer> bgWidth =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_grid.width", int.class);

    public static CaptureResult.Key<int[]> beRStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.r_stats", int[].class);
    public static CaptureResult.Key<int[]> beGStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.g_stats", int[].class);
    public static CaptureResult.Key<int[]> beBStats =
	new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.b_stats", int[].class);
    public static CaptureResult.Key<Integer> beHeight =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.height", int.class);
    public static CaptureResult.Key<Integer> beWidth =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.width", int.class);
    public static CaptureResult.Key<Float> roiBeX =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.roi_be_x", Float.class);
    public static CaptureResult.Key<Float> roiBeY =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.roi_be_y", Float.class);
    public static CaptureResult.Key<Float> roiBeWidth =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.roi_be_width", Float.class);
    public static CaptureResult.Key<Float> roiBeHeight =
        new CaptureResult.Key<>("org.codeaurora.qcamera3.bayer_exposure.roi_be_height", Float.class);

    public static CaptureResult.Key<Byte> isHdr =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.is_hdr_scene", Byte.class);
    public static CameraCharacteristics.Key<int[]> support_video_mfhdr_modes =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.supportedHDRmodes.HDRModes", int[].class);
    public static CameraCharacteristics.Key<Byte> support_auto_hdr_modes =
            new CameraCharacteristics.Key<>("org.quic.camera.AutoHDRSupport.isAutoHDRSupported", Byte.class);
    public static CameraCharacteristics.Key<Integer> support_swcapability_qll =
            new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.isQLLSupported", Integer.class);
    public static CameraCharacteristics.Key<Integer> support_insensor_zoom =
            new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.inSensorZoomCapability", Integer.class);
    public static CameraCharacteristics.Key<Integer> support_swcapability_vsr =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.platformCapabilities.EnableVSR", Integer.class);
    public static CameraCharacteristics.Key<int[]> support_dcg_modes =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.supportedHDRmodes.HDRDCGModes", int[].class);

    public static CameraCharacteristics.Key<Byte> logical_camera_type =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.logicalCameraType.logical_camera_type", Byte.class);

    public static CameraCharacteristics.Key<Byte> bsgcAvailable =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.stats.bsgc_available", Byte.class);
    public static CaptureResult.Key<byte[]> blinkDetected =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.blink_detected", byte[].class);
    public static CaptureResult.Key<byte[]> blinkDegree =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.blink_degree", byte[].class);
    public static CaptureResult.Key<byte[]> gazeAngle =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.gaze_angle", byte[].class);
    public static CaptureResult.Key<int[]> gazeDirection =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.gaze_direction",
                    int[].class);
    public static CaptureResult.Key<byte[]> gazeDegree =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.gaze_degree",
                    byte[].class);
    public static CaptureResult.Key<byte[]> contourPointsExtend =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.contour_results",
                    byte[].class);
    private static CaptureResult.Key<byte[]> facialMaskResults =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.mask_results",
                    byte[].class);
    private static CaptureResult.Key<byte[]> upperbodyResults =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.upperbody_results",
                    byte[].class);
    private static CaptureResult.Key<byte[]> petResults =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.pet_results",
                    byte[].class);
    private static CaptureResult.Key<byte[]> skinToneResults =
            new CaptureResult.Key<>("com.qualcomm.qti.fdResult.skinTone",
                    byte[].class);
    public static CaptureRequest.Key<Byte> facialContourVersion =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.contour_version",
                    Byte.class);
    public static CaptureRequest.Key<Byte> facialContourEnable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.contour_enable",
                    Byte.class);
    public static CaptureRequest.Key<Byte> gazeEnable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.gaze_enable",
                    Byte.class);
    public static CaptureRequest.Key<Byte> blinkEnable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.blink_enable",
                    Byte.class);
    private static final CaptureRequest.Key<Byte> faceMaskEnable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.face_mask_enable",
                    Byte.class);
    private static final CaptureRequest.Key<Byte> upperBodyEnable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.upperbody_enable",
                    Byte.class);
    public static final CaptureRequest.Key<Byte> petEnable  =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.pet_detection_mode",
                    Byte.class);
    public static final CaptureRequest.Key<Byte> skinToneEnable  =
            new CaptureRequest.Key<>("com.qualcomm.qti.fdMode.skinTone",
                    Byte.class);
    public static final CaptureRequest.Key<Byte> FACE_EXPRESSION_ENABLE =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.face_expression_enable",
                    Byte.class);
    public static final CaptureRequest.Key<Byte> facialContourVisib  =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.contour_visibility_mode",
                    Byte.class);


    public static final CaptureRequest.Key<Byte> GENDER_ENABLE =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.facial_attr.gender_enable",
                    Byte.class);

    public static  CaptureResult.Key<byte[]> GENDER =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.gender", byte[].class);

    public static final CaptureResult.Key<byte[]> FACE_EXPRESSION =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.stats.face_expression", byte[].class);

    public static CaptureResult.Key<Integer> ssmCaptureComplete =
            new CaptureResult.Key<>("com.qti.chi.superslowmotionfrc.CaptureComplete", Integer.class);
    public static CaptureResult.Key<Integer> ssmProcessingComplete =
            new CaptureResult.Key<>("com.qti.chi.superslowmotionfrc.ProcessingComplete", Integer.class);
    public static CaptureRequest.Key<Integer> ssmCaptureStart =
            new CaptureRequest.Key<>("com.qti.chi.superslowmotionfrc.CaptureStart", Integer.class);
    public static CaptureRequest.Key<Integer> ssmInterpFactor =
            new CaptureRequest.Key<>("com.qti.chi.superslowmotionfrc.InterpolationFactor", Integer.class);

    public static final CaptureRequest.Key<Integer> enableFRC =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableFRC", Integer.class);

    public static final CameraCharacteristics.Key<int[]> superBufferTable =
            new CameraCharacteristics.Key<>("org.quic.camera2.customhfrfps.info.CustomHFRConfigurations", int[].class);
    public static CaptureRequest.Key<Integer> outputBufferComb =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HALOutputBufferCombined", Integer.class);

    public static final CameraCharacteristics.Key<int[]> hfrFpsTable =
            new CameraCharacteristics.Key<>("org.quic.camera2.customhfrfps.info.CustomHFRFpsTable", int[].class);
    public static final CameraCharacteristics.Key<int[]> sensorModeTable  =
            new CameraCharacteristics.Key<>("org.quic.camera2.sensormode.info.SensorModeTable", int[].class);
    public static final CameraCharacteristics.Key<int[]> highSpeedVideoConfigs  =
            new CameraCharacteristics.Key<>("android.control.availableHighSpeedVideoConfigurations", int[].class);

    // AWB WarmStart gain and AWB WarmStart CCT
    private static final CaptureResult.Key<Float> awbFrame_control_rgain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AWBFrameControlRGain", Float.class);
    private static final CaptureResult.Key<Float> awbFrame_control_ggain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AWBFrameControlGGain", Float.class);
    private static final CaptureResult.Key<Float> awbFrame_control_bgain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AWBFrameControlBGain", Float.class);
    private static final CaptureResult.Key<Integer> awbFrame_control_cct =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AWBFrameControlCCT", Integer.class);
    private static final CaptureResult.Key<float[]> awbFrame_decision_after_tc =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AWBDecisionAfterTC", float[].class);
    private static final CaptureResult.Key<Float> aecFrame_dark_boost_gain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECCompenDarkBoostGain", Float.class);
    private static final CaptureResult.Key<Float> aecFrame_adrc_gain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECCompenADRCGain", Float.class);
    private static final CaptureResult.Key<Integer> exposure_count =
            new CaptureResult.Key<>("com.qti.stats_control.ExposureCount", Integer.class);

    private static final CaptureRequest.Key<Float[]> awbWarmStart_gain =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AWBWarmstartGain", Float[].class);
    private static final CaptureRequest.Key<Float> awbWarmStart_cct =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AWBWarmstartCCT", Float.class);
    private static final CaptureRequest.Key<Float[]> awbWarmStart_decision_after_tc =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AWBDecisionAfterTC", Float[].class);
    private static final CaptureRequest.Key<Float> awbWarmStart_dark_boost_gain =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AECWarmstartCompenDBGain", Float.class);
    private static final CaptureRequest.Key<Float> awbWarmStart_adrc_gain =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AECWarmstartCompenADRCGain", Float.class);

    //AEC warm start
    private static final CaptureResult.Key<float[]> aec_sensitivity =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECSensitivity", float[].class);
    private static final CaptureResult.Key<Float> aec_start_up_luxindex_result =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECLuxIndex", Float.class);

    private static final CaptureRequest.Key<Float[]> aec_start_up_sensitivity =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AECStartUpSensitivity", Float[].class);
    private static final CaptureRequest.Key<Float> aec_start_up_luxindex_request =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AECLuxIndex", Float.class);
    private static final CaptureResult.Key<long[]> aec_frame_control_exposure_time =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECExposureTime", long[].class);
    private static final CaptureResult.Key<float[]> aec_frame_control_linear_gain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECLinearGain", float[].class);
    private static final CaptureResult.Key<float[]> aec_frame_control_sensitivity =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECSensitivity", float[].class);
    private static final CaptureResult.Key<Float> aec_frame_control_lux_index =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECLuxIndex", Float.class);

    public static CaptureRequest.Key<Integer> statsVisualizerOptionMask =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.statsVisualizerOptionMask", Integer.class);
    private static CaptureRequest.Key<Integer> anti_banding_mode_request =
            new CaptureRequest.Key<>("org.quic.camera.afdData.AntiBandingMode", Integer.class);
    private static CaptureRequest.Key<Integer> isficker_detected_request =
            new CaptureRequest.Key<>("org.quic.camera.afdData.IsFlickerDetected", Integer.class);

    private static CaptureResult.Key<Integer> anti_banding_mode_result =
            new CaptureResult.Key<>("org.quic.camera.afdData.AntiBandingMode", Integer.class);
    private static CaptureResult.Key<Integer> isficker_detected_result =
            new CaptureResult.Key<>("org.quic.camera.afdData.IsFlickerDetected", Integer.class);

    //AFD infos
    private static final CaptureResult.Key<Integer> afd_hnum =
            new CaptureResult.Key<>("org.quic.camera.afdData.HNum", Integer.class);
    private static final CaptureResult.Key<Integer> afd_vnum =
            new CaptureResult.Key<>("org.quic.camera.afdData.VNum", Integer.class);
    private static final CaptureResult.Key<Float> afd_visible_bands =
            new CaptureResult.Key<>("org.quic.camera.afdData.NumberOfVisibleBands", Float.class);
    private static final CaptureResult.Key<float[]> afd_rs_time =
            new CaptureResult.Key<>("org.quic.camera.afdData.RowSumTime", float[].class);
    private static final CaptureResult.Key<Integer> afd_anti_banding_mode =
            new CaptureResult.Key<>("org.quic.camera.afdData.AntiBandingMode", Integer.class);
    private static final CaptureResult.Key<int[]> afd_lines_frame =
            new CaptureResult.Key<>("org.quic.camera.afdData.NumberOfLinesPerFrame", int[].class);
    private static final CaptureResult.Key<Float> avg_rolling_energy =
            new CaptureResult.Key<>("org.quic.camera.afdData.AvgRollingEnergyRatio", Float.class);
    private static final CaptureResult.Key<Float> avg_rolling_conf =
            new CaptureResult.Key<>("org.quic.camera.afdData.AvgRollingConfidenceScore", Float.class);
    private static final CaptureResult.Key<Float> avg_static_energy =
            new CaptureResult.Key<>("org.quic.camera.afdData.AvgStaticEnergyRatio", Float.class);
    private static final CaptureResult.Key<Float> avg_static_conf =
            new CaptureResult.Key<>("org.quic.camera.afdData.AvgStaticConfidenceScore", Float.class);
    //AF infos
    private static final CaptureResult.Key<Byte> isPDENABLE =
            new CaptureResult.Key<>("org.quic.camera.afData.isPDEnable", Byte.class);
    private static final CaptureResult.Key<Integer> pd_type =
            new CaptureResult.Key<>("org.quic.camera.afData.PDType", Integer.class);
    private static final CaptureResult.Key<Byte> isSparseHW =
            new CaptureResult.Key<>("org.quic.camera.afData.isSparseHW", Byte.class);
    private static final CaptureResult.Key<Byte> isDualPDHW =
            new CaptureResult.Key<>("org.quic.camera.afData.isDualPDHW", Byte.class);
    private static final CaptureResult.Key<Byte> isLCRHW =
            new CaptureResult.Key<>("org.quic.camera.afData.isLCRHW", Byte.class);
    private static final CaptureResult.Key<Byte> isLCRSW =
            new CaptureResult.Key<>("org.quic.camera.afData.isLCRSW", Byte.class);
    private static final CaptureResult.Key<Integer> lenspos =
            new CaptureResult.Key<>("org.quic.camera.afData.lenspos", Integer.class);
    public static final CaptureResult.Key<byte[]> autofocusroi =
            new CaptureResult.Key<>("org.quic.camera.afData.autofocusroi", byte[].class);
    private static final CaptureResult.Key<int[]> rsStats =
            new CaptureResult.Key<>("org.quic.camera.afData.rsStats", int[].class);
    //camera id && request id
    private static final CaptureResult.Key<Long> stats_visualizer_request_id =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECRequestID", Long.class);
    private static final CaptureRequest.Key<Integer> request_aec_camera_id =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.AECCameraID", Integer.class);
    private static final CaptureResult.Key<Integer> stats_visualizer_camera_id =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECCameraID", Integer.class);

    private static final CaptureResult.Key<Float> ratio_long_to_short =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.ratioLongtoShort", Float.class);

    private static final CaptureResult.Key<Float> ratio_long_to_safe =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.ratioLongtoSafe", Float.class);

    private static final CaptureResult.Key<Float> ratio_safe_to_short =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.ratioSafetoShort", Float.class);

    private static final CaptureResult.Key<Float> adrc_gain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.compenADRCGain", Float.class);

    private static final CaptureResult.Key<Float> dark_boost_gain =
            new CaptureResult.Key<>("org.quic.camera2.statsconfigs.compenDarkBoostGain", Float.class);

    //Variable fps
    private static final CaptureRequest.Key<float[]> dynamicFSPConfigKey =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.dynamicFPSConfig", float[].class);
    public static final CaptureResult.Key<float[]> getdynamicFSPConfigKey =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.sessionParameters.dynamicFPSConfig", float[].class);

    //Stats NN Result
    private static final CaptureRequest.Key<Byte> statsNNControl =
            new CaptureRequest.Key<>("org.quic.camera2.statsNNControl.Enable", Byte.class);
    private static final CaptureRequest.Key<Byte> qcam3NNControl =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSalinet", Byte.class);
    private static final CaptureResult.Key<Byte> stats_nn_result_width =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyWidth", Byte.class);
    private static final CaptureResult.Key<Byte> stats_nn_result_height =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyHeight", Byte.class);
    private static final CaptureResult.Key<Byte> stats_nn_result_mapdata =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyMapData", Byte.class);
    private static final CaptureResult.Key<Byte> stats_nn_result_numroi =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyNumROI", Byte.class);
    private static final CaptureResult.Key<int[]> stats_nn_result_roidata =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyROIData", int[].class);
    private static final CaptureResult.Key<Integer> stats_nn_result_roiweight =
            new CaptureResult.Key<>("org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyROIWeight", Integer.class);

    private static final CaptureRequest.Key<Byte> pdnet_control =
            new CaptureRequest.Key<>("org.quic.camera2.PDNetControl.Enable", Byte.class);

    public static final CaptureRequest.Key<Integer> sharpness_control = new CaptureRequest.Key<>(
            "org.codeaurora.qcamera3.sharpness.strength", Integer.class);
    public static final CaptureRequest.Key<Integer> exposure_metering = new CaptureRequest.Key<>(
            "org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode", Integer.class);
    public static final CaptureRequest.Key<Byte> eis_mode =
            new CaptureRequest.Key<>("org.quic.camera.eis3enable.EISV3Enable", byte.class);
    public static final CaptureRequest.Key<Byte> recording_end_stream =
            new CaptureRequest.Key<>("org.quic.camera.recording.endOfStream", byte.class);

    // Session Parameters vendorTag START
    public static final CaptureRequest.Key<Integer> earlyPCR =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numPCRsBeforeStreamOn", Integer.class);
    public static final CaptureRequest.Key<Integer> mcxMasterCb =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMCXMasterCb", Integer.class);
    public static final CaptureRequest.Key<Integer> extendedMaxZoom =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.ExtendedMaxZoom", Integer.class);
    public static final CaptureResult.Key<Integer> getExtendedMaxZoom =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.sessionParameters.ExtendedMaxZoom", Integer.class);
    public static final CaptureRequest.Key<Integer> numHDRexposure =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
    public static final CaptureRequest.Key<Byte> shading_correction =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableShadingCorrection", byte.class);
    public static final CaptureRequest.Key<Integer> mcxRawCbInfo =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.McxRawCallbackInfo", Integer.class);
    public static final CaptureRequest.Key<Byte> mctf =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableMCTFwithReferenceFrame", byte.class);
    public static final CaptureRequest.Key<Byte> enable_statsvisualizer =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableStatsVisualizer", byte.class);
    public static final CaptureRequest.Key<Integer> insensor_zoom_feature =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableInsensorZoom", Integer.class);

    public static final CaptureResult.Key<Integer> insensor_zoom_result =
            new CaptureResult.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableInsensorZoom", Integer.class);
    private static final CaptureRequest.Key<Byte> xcfa_optimization =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableXCFAOptimization", byte.class);
    private static final CaptureRequest.Key<Integer> horizon_level_control =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HorizonLevelControl", Integer.class);
    private static final CaptureRequest.Key<Integer> cinematic_mode_enable =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableCinematicMode", Integer.class);

    public static final CaptureRequest.Key<Integer> offline_dump_trigger_enabled =
            new CaptureRequest.Key<>("org.quic.camera.offlinedump.isEnabled", Integer.class);
    public static final CameraCharacteristics.Key<Byte> enable_shading_correction =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.shadingCorrection.enableShadingCorrection", byte.class);
    private static final CaptureResult.Key<Byte> is_depth_focus =
            new CaptureResult.Key<>("org.quic.camera.isDepthFocus.isDepthFocus", byte.class);
    private static final CaptureRequest.Key<Byte> capture_burst_fps =
            new CaptureRequest.Key<>("org.quic.camera.BurstFPS.burstfps", byte.class);
    public static final CameraCharacteristics.Key<Byte> is_camera_fd_supported = new CameraCharacteristics.Key<>(
            "org.quic.camera.FDRendering.isFDRenderingInCameraUISupported",byte.class);
    private static final CaptureRequest.Key<Byte> custom_noise_reduction =  new CaptureRequest.Key<>(
            "org.quic.camera.CustomNoiseReduction.CustomNoiseReduction", byte.class);

    // offline dump trigger
    public static final CaptureRequest.Key<Integer> offline_dump_trigger_trigger =
            new CaptureRequest.Key<>("org.quic.camera.offlinedump.OfflineDumpTrigger", Integer.class);
    public static final CaptureRequest.Key<Long> offline_dump_trigger_framenum =
            new CaptureRequest.Key<>("org.quic.camera.offlinedump.frameNum", Long.class);

    // extended max zoom
    public static CameraCharacteristics.Key<Float> extended_max_zoom =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.platformCapabilities.ExtendedMaxZoom", Float.class);

    public static final CaptureRequest.Key<Byte> sensor_mode_fs =
            new CaptureRequest.Key<>("org.quic.camera.SensorModeFS", byte.class);
    public static CameraCharacteristics.Key<Byte> fs_mode_support =
            new CameraCharacteristics.Key<>("org.quic.camera.SensorModeFS.isFastShutterModeSupported", Byte.class);

    // Touch Track Focus
    public static final CaptureRequest.Key<Byte> t2t_enable = new CaptureRequest.Key<>(
            "org.quic.camera2.objectTrackingConfig.Enable", Byte.class);
    public static final CaptureRequest.Key<int[]> t2t_register_roi = new CaptureRequest.Key<>(
            "org.quic.camera2.objectTrackingConfig.RegisterROI", int[].class);
    public static final CaptureRequest.Key<Integer> t2t_cmd_trigger = new CaptureRequest.Key<>(
            "org.quic.camera2.objectTrackingConfig.CmdTrigger", Integer.class);

    public static final CameraCharacteristics.Key<Byte> is_t2t_supported =
            new CameraCharacteristics.Key<>(
                    "org.quic.camera2.objectTrackingResults.TrackerEnable", byte.class);
    public static final CameraCharacteristics.Key<Byte> is_statsnn_supported =
            new CameraCharacteristics.Key<>(
                    "org.quic.camera2.statsNNSaliNetResults.statsNNSaliencyEnable", byte.class);
    private static final CaptureResult.Key<Integer> t2t_tracker_status =
            new CaptureResult.Key<>("org.quic.camera2.objectTrackingResults.TrackerStatus", Integer.class);
    private static final CaptureResult.Key<int[]> t2t_tracker_result_roi =
            new CaptureResult.Key<>("org.quic.camera2.objectTrackingResults.ResultROI", int[].class);
    private static final CaptureResult.Key<Integer> t2t_tracker_score =
            new CaptureResult.Key<>("org.quic.camera2.objectTrackingResults.TrackerScore", Integer.class);
    private static final CaptureRequest.Key<Integer> livePreview =
            new CaptureRequest.Key<>("com.qti.chi.livePreview.enable", Integer.class);
    public static final CaptureResult.Key<Integer> getLivePreview =
            new CaptureResult.Key<>("com.qti.chi.livePreview.enable", Integer.class);

    public static CaptureResult.Key<Integer> multiframe_burst_enable =
            new CaptureResult.Key<>("org.quic.camera.MultiFrame.MultiframeBurstEnable", Integer.class);

    public static final CameraCharacteristics.Key<Byte> swmctf =
            new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.SWMCTFEnable", byte.class);
    public static CaptureRequest.Key<Byte> isAfLock =
            new CaptureRequest.Key<>("org.quic.camera2.statsconfigs.isAFLock", Byte.class);

    private static final CaptureRequest.Key<byte[]> rawinfo_idealraw_request =
          new CaptureRequest.Key<>("com.qti.chi.rawcbinfo.IdealRaw", byte[].class);
    private static final CaptureResult.Key<Integer> metadataOwnerInfo =
            new CaptureResult.Key<>("com.qti.chi.metadataOwnerInfo.MetadataOwner", Integer.class);
    private static final CaptureResult.Key<Byte> masterCamera =
            new CaptureResult.Key<>("com.qti.chi.multicamerainfo.MasterCamera", Byte.class);
    private static final CaptureResult.Key<byte[]> ActiveCameraInfo =
            new CaptureResult.Key<>("com.qti.chi.multicamerainfo.ActiveCameraInfo", byte[].class);
    private static final CaptureResult.Key<byte[]> MultiCameraIds =
            new CaptureResult.Key<>("com.qti.chi.multicamerainfo.MultiCameraIds", byte[].class);

    public static final CameraCharacteristics.Key<Integer> MFNRType =
            new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.MFNRType", Integer.class);
    public static CameraCharacteristics.Key<int[]> HWMFNR_FRAME_RANGE =
            new CameraCharacteristics.Key<>("org.quic.camera.MultiFrameNodeReduction.MFFramesRange", int[].class);
    public static CaptureRequest.Key<Integer> mfnrFrameNO =
            new CaptureRequest.Key<>("org.quic.camera.MultiFrameNodeReduction.MFNumOfFrames", Integer.class);
    public static final CaptureResult.Key<Integer> isTorchHdr =
            new CaptureResult.Key<>("com.qti.stats_control.is_torch_hdr_snapshot", Integer.class);
    private static final CaptureRequest.Key<Integer> snapshotHDR =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.SnapshotHDRMode", Integer.class);

    private static final CaptureRequest.Key<Integer> enablePerReqSync =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enablePerReqSync", Integer.class);

    //vendor tag for AIDE2
    public static final CameraCharacteristics.Key<Byte> isAIDE2Supported =
            new CameraCharacteristics.Key<>("org.quic.camera.AIDE2Supported.isAIDE2Supported", byte.class);
    public static final CaptureRequest.Key<Byte> isAIDE2Enabled =
            new CaptureRequest.Key<>("org.quic.camera.HWMFNRandAIDenoiser.isAIDE2Enabled", byte.class);
    public static final CaptureResult.Key<byte[]> HWMFNRandAIDE2TuningParams =
            new CaptureResult.Key<>("org.quic.camera.HWMFNRandAIDenoiser.HWMFNRandAIDE2TuningParams", byte[].class);
    public static final CaptureResult.Key<byte[]> StreamCropInfo =
            new CaptureResult.Key<>("com.qti.camera.streamCropInfo.StreamCropInfo", byte[].class);
    public static final CameraCharacteristics.Key<Integer> EnableAICameraHSR =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.platformCapabilities.EnableAICameraHSR", Integer.class);
    public static CameraCharacteristics.Key<int[]> supportedAICameraModes =
            new CameraCharacteristics.Key<>("org.quic.camera.capabilities.supportedAICameraModes", int[].class);
    private static final CaptureResult.Key<Byte> VRSSkipSegment =
            new CaptureResult.Key<>("org.quic.camera.AICamera.VRSSkipSegment", byte.class);
    private static final CaptureRequest.Key<Byte> EnableAISnapshot =
            new CaptureRequest.Key<>("org.quic.camera.AICamera.EnableAISnapshot", byte.class);
    private static final CaptureRequest.Key<Integer> AICameraStrength =
            new CaptureRequest.Key<>("org.quic.camera.AICamera.AIStrength", Integer.class);
    private static final CaptureRequest.Key<Integer> blurShape =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurShape", Integer.class);
    private static final CaptureRequest.Key<Float> blurStrength =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurStrength", Float.class);
    private static final CaptureRequest.Key<Float> blurFocusDistance =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurFocusDistance", Float.class);
    private static final CaptureRequest.Key<Integer> blurEffect =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurEffect", Integer.class);
    private static final CaptureRequest.Key<Float> blurChromaSuppressionU =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurChromaSuppressionU", Float.class);
    private static final CaptureRequest.Key<Float> blurChromaSuppressionV =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurChromaSuppressionV", Float.class);
    private static final CaptureRequest.Key<Float> blurChromaSuppressionStrength =
            new CaptureRequest.Key<>("org.quic.camera.blurConfig.blurChromaSuppressionStrength", Float.class);
    public static CameraCharacteristics.Key<Byte> isMLVideoSupported =
            new CameraCharacteristics.Key<>("org.quic.camera.videoretouch.isVideoRetouchSupported", byte.class);

    public static final CaptureResult.Key<Byte> focusAssistEnable =
            new CaptureResult.Key<>("org.quic.camera.touchFocusAssist.touchFocusAssist", byte.class);

    public static final int CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY = 2;
    public static final int CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY = 1;
    public static final int CONTROL_AE_PRIORITY_MODE_OFF = 0;
    private static final long SCALER_AVAILABLE_STREAM_USE_CASES_VENDOR_START = 0x10000;
    private static final long SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV = 0x10001;
    private static final int TIMESTAMP_BASE_SENSOR = OutputConfiguration.TIMESTAMP_BASE_SENSOR;
    TotalCaptureResult mCaptureResult;
    float denoiseStrengthParam = 0.5f;
    float color_saturation = 0.0f;
    float tone = 0.0f;
    float detail_enhancement = 0.0f;
    float mEnhancefactor = 0.5f;
    byte mGainThresholdY = 0;
    byte mGainThresholdUV = 0;

    private Object mAideLock = new Object();
    private CameraUtil.IntegerLock mLockNums = new CameraUtil.IntegerLock(0);

    float mAideAdrcGain = 100;
    public static final CameraCharacteristics.Key<int[]> hdrMaxResolution =
            new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.HDRMaxResolutionCap.HDRMaxResolution", int[].class);

    private TouchTrackFocusRenderer mT2TFocusRenderer;
    private StateNNTrackFocusRenderer mStateNNFocusRenderer;
    private AFView mAFRenderer;
    private boolean mIsDepthFocus = false;
    private boolean mLastIsDepthFocus = false;
    private boolean[] mTakingPicture = new boolean[MAX_NUM_CAM];
    private boolean mIsLongExpTmCp = false;
    private long maxExpTime = 100000000;
    private long mLongExpTime = 1 ;
    private int mControlAFMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    private int mLastResultAFState = -1;
    private boolean isFlashRequiredInDriver = false;
    private Rect[] mCropRegion = new Rect[MAX_NUM_CAM];
    private Rect[] mOriginalCropRegion = new Rect[MAX_NUM_CAM];
    private boolean mAutoFocusRegionSupported;
    private boolean mAutoExposureRegionSupported;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    /*Histogram variables*/
    private Camera2GraphView mGraphViewR,mGraphViewGB,mGraphViewB;
    private Camera2RGBGraphView mGraphViewRGB;
    private Camera2BGBitMap    bgstats_view;
    private Camera2BEBitMap    bestats_view;
    private Camera2RSBitMap    rsstats_view;
    private TextView mBgStatsLabel;
    private TextView mBeStatsLabel;
    private TextView mRsStatsLabel;
    private DrawAutoHDR2 mDrawAutoHDR2;
    public boolean mAutoHdrEnable;
    private MFNRDrawer mMFNRDrawer;
    private TextView mMFNRSwitch;
    private SeekBar mMfnrSeekBar;
    private TextView mMFNRText;
    private TextView mBokehText;
    public boolean mMFNREnable;
    /*HDR Test*/
    private boolean mCaptureHDRTestEnable = false;
    boolean mHiston    = false;
    boolean mBGStatson = false;
    boolean mBEStatson = false;
    boolean mRSStatson = false;
    private boolean mFirstTimeInitialized;
    private boolean mCamerasOpened = false;
    private boolean mIsLinked = false;
    private long mCaptureStartTime;
    private boolean mPaused = true;
    private boolean mResumed = true;
    private Semaphore mSurfaceReadyLock = new Semaphore(1);
    private final Object mVideoStateLock = new Object();
    private VideoState mVideoState;
    private boolean mSurfaceReady = true;
    private boolean[] mCameraOpened = new boolean[MAX_NUM_CAM];
    private CameraDevice[] mCameraDevice = new CameraDevice[MAX_NUM_CAM];
    private String[] mCameraId = new String[MAX_NUM_CAM];
    private String[] mSelectableModes = {"Video", "Cinematic", "HFR", "Photo", "Bokeh", "SAT", "Pro", "Depth"};
    private ArrayList<SceneModule> mSceneCameraIds = new ArrayList<>();
    private Set<String> mQuadBayerPhysicalIds = new HashSet<>();
    public static boolean MCXMODE = false;
    private boolean switchedCameraId = false;

    private int mLogicalId = -1;
    private int mSingleRearId = -1;
    private SceneModule mCurrentSceneMode;
    private CameraMode mOldMode;
    private int mOldCameraId;
    private int mNextModeIndex = 1;
    private int mCurrentModeIndex = 1;
    private int mLastT2tTrackState = -1;
    private int mT2TTrackState = 0;
    public enum CameraMode {
        VIDEO,
        CINEMATIC,
        HFR,
        DEFAULT,
        RTB,
        SAT,
        PRO_MODE,
        DEPTH
    }
    private enum VideoState {
        VIDEO_INIT,
        VIDEO_PREVIEW,
        VIDEO_START,
        VIDEO_PAUSE,
        VIDEO_RESUME,
        VIDEO_STOP
    }
    private View mRootView;
    private CaptureUI mUI;
    private CameraActivity mActivity;
    private float mZoomValue = 1f;
    private int mAIStrengthValue = 0;
    private FocusStateListener mFocusStateListener;
    private LocationManager mLocationManager;
    public SettingsManager mSettingsManager;
    private long SECONDARY_SERVER_MEM;
    private boolean mLongshotActive = false;
    private long mLastLongshotTimestamp = 0;
    private boolean mBurstLimit = false;
    private CameraCharacteristics mMainCameraCharacteristics;
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private boolean mIsRefocus = false;
    private int mChosenImageFormat;
    private Toast mToast;

    private boolean mStartRecPending = false;
    private boolean mStopRecPending = false;

    boolean mUnsupportedResolution = false;
    private boolean mExistAWBVendorTag = true;
    private boolean mExistAECWarmTag = true;
    private boolean mExistAECFrameControlTag = true;
    private boolean mExistAECDarkGainTag = true;
    private boolean mExistAntiBandingModeTag = true;
    private boolean mExistIsFickerDetected = true;
    private boolean mExposureCountTag = true;
    private boolean mAECCameraIdTag = true;

    private static final long SDCARD_SIZE_LIMIT = 4000 * 1024 * 1024L;
    private static final String sTempCropFilename = "crop-temp";
    private static final int REQUEST_CROP = 1000;
    private int mIntentMode = INTENT_MODE_NORMAL;
    private boolean mIsVoiceTakePhote = false;
    private String mCropValue;
    private Uri mCurrentVideoUri;
    private final Set<Uri> mUrisInvalid = new HashSet<>();
    private boolean mTempHoldVideoInVideoIntent = false;
    private boolean mCurrentSessionClosed = false;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private Uri mSaveUri;
    private boolean mQuickCapture;
    private byte[] mJpegImageData;
    private boolean mSaveRaw = false;
    private boolean mYUV10bit = false;
    private boolean mYUV10BitWithMetadata = false;
    private boolean mSupportZoomCapture = true;
    private long mStartRecordingTime;
    private long mStopRecordingTime;

    private int mLastAeState = -1;
    private int mLastAfState = -1;
    private boolean mIsCanceled = false;
    private boolean mIsAutoFocusStarted = false;
    private boolean mIsAutoFlash = false;
    private int mSetAePrecaptureTriggerIdel = 0;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession[] mCaptureSession = new CameraCaptureSession[MAX_NUM_CAM];
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;
    private HandlerThread mImageAvailableThread;
    private HandlerThread mCaptureCallbackThread;
    private HandlerThread mMpoSaveThread;

    private boolean mBackgroundThreadFlag = false;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private PostProcessor mPostProcessor;
    private FrameProcessor mFrameProcessor;
    private CaptureResult mPreviewCaptureResult;
    private Face[] mPreviewFaces = null;
    private Face[] mStickyFaces = null;
    private ExtendedFace[] mExFaces = null;
    private ExtendedFace[] mStickyExFaces = null;
    private Rect mBayerCameraRegion;
    private Handler mCameraHandler;
    private Handler mImageAvailableHandler;
    private Handler mCaptureCallbackHandler;
    private Handler mMpoSaveHandler;
    private Handler mZoomHandler;
    private long mZoomTime;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader[] mImageReader = new ImageReader[MAX_NUM_CAM];
    private ImageReader[] mRawImageReader = new ImageReader[MAX_NUM_CAM];
    private ImageReader[] mYUV10bitImageReader = new ImageReader[MAX_NUM_CAM];
    private Size[] mPhysicalSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private Size[] mPhysicalRawSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private Size[] mPhysicalJpegRSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private String[] mPhysicalRawId = new String[PHYSICAL_CAMERA_COUNT];
    private Size[] mPhysicalVideoSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private Size[] mPhysicalVideoSnapshotSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private Size[] mPhysicalPreviewSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private Size mLogicalPreviewSize;
    private Size mLogicalVideoPreviewSize;
    private Size[] mPhysicalVideoPreviewSizes = new Size[PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mPhysicalYuvReader = new ImageReader[MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mPhysicalYuv10bitReader = new ImageReader[MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mPhysicalRawReader = new ImageReader[MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mPhysicalJpegReader = new ImageReader[PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mPhysicalJpegRReader = new ImageReader[MAX_LOGICAL_PHYSICAL_CAMERA_COUNT];

    private ImageReader mDepthImageReader = null;

    private final Object mDepthImageLock = new Object();
    private DepthRender mDepthRender = null;

    private long mDepthLastFrameTimeStamp = 0L;
    private int mDepthFrameCount = 0;
    private boolean is8KInMulti = false;
    //yuv raw images are for raw reprocess
    public int mRawReprocessType = 0;
    public boolean mMultiResReprocessEnabled = false;
    private int mYUVCount = 1;
    private Size[] mYUVsize = new Size[mYUVCount];
    private ImageReader[] mYUVImageReader = new ImageReader[mYUVCount];
    private int mRawCount = 1;
    private String mMasterCameraId;
    private ArrayList<Integer> mActiveCameraIds = new ArrayList<Integer>();
    private HashMap<Integer, Boolean> mAideActiveCameraIds = new HashMap<Integer, Boolean>();//<cameraid, isPrimal>
    //used for aide capture request and callback
    private int mCaptureRequestNum = 0;
    private float mAideAECLuxIndex = -1.0f;
    private ImageReader[] mAideFullImageReader = new ImageReader[PHYSICAL_CAMERA_COUNT];
    private ImageReader[] mAideDs4ImageReader = new ImageReader[PHYSICAL_CAMERA_COUNT];
    private Image mAideFullImage;
    private Image mAideDownImage;
    private Size[] mRawSize = new Size[mRawCount];
    private ImageReader[] mRAWImageReader = new ImageReader[mRawCount];
    private HeifWriter mInitHeifWriter;
    private OutputConfiguration mHeifOutput;
    private HeifImage mHeifImage;
    private HeifWriter mLiveShotInitHeifWriter;
    private OutputConfiguration mLiveShotOutput;
    private HeifImage mLiveShotImage;
    private NamedImages mNamedImages;
    private ContentResolver mContentResolver;
    private byte[] mLastJpegData;
    private int mJpegFileSizeEstimation;
    private boolean mFirstPreviewLoaded;
    private int[] mPrecaptureRequestHashCode = new int[MAX_NUM_CAM];
    private int[] mLockRequestHashCode = new int[MAX_NUM_CAM];
    private final Handler mHandler = new MainHandler();
    private CameraCaptureSession mCurrentSession;
    private OutputConfiguration mPreviewOutputConfiguration;

    private Size mDepthSize = null;
    private Size mPreviewSize;
    private Size mPictureSize;
    private Size mVideoPreviewSize;
    private Size mVideoSize;
    private Size mVideoSnapshotSize;
    private Size mPictureThumbSize;
    private Size mVideoSnapshotThumbSize;
    private MediaRecorder mMediaRecorder;
    public boolean mIsRecordingVideo = false;
    private boolean mIsPreviewingVideo = false;
    // The video duration limit. 0 means no limit.
    private int mMaxVideoDurationInMs;
    private boolean mIsMute = false;
    // Default 0. If it is larger than 0, the camcorder is in time lapse mode.
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;
    private boolean mCaptureTimeLapse = false;
    private CamcorderProfile mProfile;
    private static final int KEEP_SCREEN_ON = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int VOICE_INTERACTION_CAPTURE = 7;
    private ContentValues mCurrentVideoValues;
    private String mVideoFilename;
    private String mVideoFilePath;
    private boolean mRecordingPausing = false;
    private boolean mRecordingStarted = false;
    private boolean mRecordingStoped = true;
    private long mRecordingStartTime;
    private long mRecordingTotalTime;
    private long mRecordingPauseTime;
    private long mRecordingPausingTime;
    private long mHighRecordingPausingTime;
    private long mMaxDurationForCodec;
    private boolean mRecordingTimeCountsDown = false;
    private ImageReader mVideoSnapshotImageReader;
    private ImageReader[] mPhysicalSnapshotImageReaders = new ImageReader[PHYSICAL_CAMERA_COUNT];
    private MediaRecorder[] mPhysicalMediaRecorders = new MediaRecorder[PHYSICAL_CAMERA_COUNT];
    private final Uri[] mPhysicalUris = new Uri[PHYSICAL_CAMERA_COUNT];
    private Range mHighSpeedFPSRange;
    private Range mHighSpeedPreviewFPSRange;
    private boolean mHighSpeedCapture = false;
    private boolean mHighSpeedRecordingMode = false; //HFR-false HSR or SSM-true
    private int mHighSpeedCaptureRate;
    private boolean mSuperSlomoCapture = false;
    private int mInterpFactor = 1;
    private CaptureRequest.Builder mVideoRecordRequestBuilder;
    private CaptureRequest.Builder mVideoPreviewRequestBuilder;
    private Surface mVideoPreviewSurface;
    private Surface mVideoRecordingSurface;
    private Surface[] mPhysicalMediaSurfaces = new Surface[PHYSICAL_CAMERA_COUNT];
    private boolean mCameraModeSwitcherAllowed = true;

    private static int STATS_DATA = 768;
    public static int statsdata[] = new int[STATS_DATA];

    private boolean mInTAF = false;

    private String mStatsVisualEnable;
    private String mStatsVisualizer;
    //performance debug info
    public String mPerformanceDebugEnable;
    private long mOpenCameraLatency;
    private long mSettingInitLatency;
    private long mCreateSessionLatency;
    private long mFirstRequestLatency;
    private long mFlushLatency;
    private long mCloseCameraLatency;

    private long mSnapshotLatency;
    private long mShutterLag;
    private long mBurstStartTime = 0;
    private float mBurstFps = 0;
    private long mZoomLatency;
    private float mResultFPS = 0;
    public static final HashMap<Long, Float> mZoomTimeMap = new HashMap<>();
    public static final HashMap<Float, Rect> mZoomValueMap = new HashMap<>();
    long mAFConvergence = 0;
    long mAECConvergence = 0;
    long mAWBConvergence = 0;
    private static String[] mPerformanceDebugData = new String[CaptureUI.PERFORMANCE_DEBUG_TITLE.length];
    private Camera2RequestGapGraphView mGapGraphView;
    long mLastResultTime = 0;
    long mLastFPSCountTime = 0;
    long mCurrentFrameCount = 0;
    public static List<Long> mPerformanceGapData = new ArrayList<>();

    /*
     * MultiResolutionImageReader
     */
    private MultiResolutionImageReader mMultiResImageReader = null;
    // Max number of images can be accessed simultaneously from ImageReader.
    private static final int MAX_MULTIIMAGES = 8;
    private InputConfiguration mInputConfig = null;
    private Collection<OutputConfiguration> mOutConfigs = null;

    // BG stats
    private static int BGSTATS_DATA = 64*48;
    public static int BGSTATS_WIDTH = 240;
    public static int BGSTATS_HEIGHT = 320;
    public static int STATS_LENGTH = 5;
    public static int bg_statsdata[]   = new int[BGSTATS_DATA*STATS_LENGTH*STATS_LENGTH];
    public static int bg_r_statsdata[] = new int[BGSTATS_DATA];
    public static int bg_g_statsdata[] = new int[BGSTATS_DATA];
    public static int bg_b_statsdata[] = new int[BGSTATS_DATA];
    public static String bgstatsdata_string = new String();
    private static int STATS_DATA_BIT_SHIFT = 10;
    public static final int SCALE_STATS = 5;
    // BE stats
    private static int BESTATS_DATA = 64*48;
    public static int BESTATS_WIDTH = 240;
    public static int BESTATS_HEIGHT = 320;
    public static int be_statsdata[]   = new int[BESTATS_DATA*STATS_LENGTH*STATS_LENGTH];
    public static int be_r_statsdata[] = new int[BESTATS_DATA];
    public static int be_g_statsdata[] = new int[BESTATS_DATA];
    public static int be_b_statsdata[] = new int[BESTATS_DATA];
    private static int statsParametersUpdated = 0;
    public static final int STATS_PARAMETER_UPDATE = 5;

    // RS stats
    private static int RSSTATS_DATA = 32*32;
    public static int RSSTATS_WIDTH = 160;
    public static int RSSTATS_HEIGHT = 160;
    public static int rs_statsdata[]   = new int[RSSTATS_DATA*STATS_LENGTH*STATS_LENGTH];
    public static int rs_r_statsdata[] = new int[RSSTATS_DATA];
    public static int rs_g_statsdata[] = new int[RSSTATS_DATA];
    public static int rs_b_statsdata[] = new int[RSSTATS_DATA];

    // AWB Info
    private static String[] awbinfo_data = new String[4];
    // AEC Info
    private static String[] aecinfo_data = new String[15];
    // AFD Info
    private static String[] afdinfo_data = new String[16];
    // AF Info
    private static String[] afinfo_data = new String[7];
    private int[] mAFRoi = new int[4];
    // stats camera  Info
    private static String[] camerainfo_data = new String[2];

    private static final int SELFIE_FLASH_DURATION = 680;
    private static final int SESSION_CONFIGURE_TIMEOUT_MS = 3000;

    private SoundClips.Player mSoundPlayer;
    private Size mSupportedMaxPictureSize;
    private Size mSupportedRawPictureSize;
    private Size mYUVP010Size;
    private Size mPhysicalRawSize;
    private Size mPhysicalJPGSize;

    private long mIsoExposureTime;
    private int mIsoSensitivity;

    private CameraRender mCameraRender;

    private OutputConfiguration mFAOutputConfiguration;

    private Surface mPreviewSurface;

    private Surface mFASurface;

    private CamGLRenderer mRenderer;
    private boolean mDeepPortraitMode = false;
    private boolean mIsCloseCamera = true;
    TotalCaptureResult mRawInputMeta;
    private int mOpenCameraTimes = 3;
    private static final int LOCK_AF_AE_STATE_NONE = 0;
    private static final int LOCK_AF_AE_STATE_START = 1;
    private static final int LOCK_AF_AE_STATE_LOCK_DONE = 2;
    private int mLockAFAE = LOCK_AF_AE_STATE_NONE;
    private TextView mLockAFAEText;
    private int[] mClickPosition = new int[2];
    private boolean isManualAEC = false;
    private boolean mCaptureTorchTrigger = false;

    private class SelfieThread extends Thread {
        public void run() {
            try {
                Thread.sleep(SELFIE_FLASH_DURATION);
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        takePicture();
                    }
                });
            } catch(InterruptedException e) {
            }
            selfieThread = null;
        }
    }
    private SelfieThread selfieThread;

    private class MediaSaveNotifyThread extends Thread {
        private Uri uri;

        public MediaSaveNotifyThread(Uri uri) {
            this.uri = uri;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }

        public void run() {
            mActivity.runOnUiThread(new Runnable() {

                public void run() {
                    if (uri != null)
                        mActivity.notifyNewMedia(uri);
                    mActivity.updateStorageSpaceAndHint();
                    if (mLastJpegData != null) mActivity.updateThumbnail(mLastJpegData);
                }
            });
            mediaSaveNotifyThread = null;
        }
    }

    public void updateThumbnailJpegData(byte[] jpegData) {
        mLastJpegData = jpegData;
    }

    private MediaSaveNotifyThread mediaSaveNotifyThread;

    private final MediaSaveService.OnMediaSavedListener mOnVideoSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    Log.i(TAG, "mOnVideoSavedListener onMediaSaved uri :" + uri);
                    if(mSettingsManager.getValue(SettingsManager.KEY_C2PA) != null &&
                            mSettingsManager.getValue(SettingsManager.KEY_C2PA).equals("on")){
                        Location location = getLocationManager().getCurrentLocation();
                        double latitude = 0, longitude = 0, altitude = 0, accuracy = 0;
                        long time = 0;
                        if(location != null && !location.isMock()) {
                            Log.d(TAG, "start to do c2pa reprocess: " + location.toString());
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            altitude = location.getAltitude();
                            accuracy = location.getAccuracy();
                            time = location.getTime();
                        }
                        mPostProcessor.nativeC2paSignVideo(mVideoSize.getHeight(), mVideoSize.getWidth(), mVideoFilename, latitude, longitude, altitude, accuracy, time);
                    }
                    if (uri != null) {
                        mActivity.notifyNewMedia(uri);
                    }
                    mActivity.updateStorageSpaceAndHint();
                }
            };

    private MediaSaveService.OnMediaSavedListener mOnMediaSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    Log.i(TAG, "onMediaSaved uri :" + uri + ", mLongshotActive :" + mLongshotActive);
                    if (mLongshotActive) {
                        if (mediaSaveNotifyThread == null) {
                            mediaSaveNotifyThread = new MediaSaveNotifyThread(uri);
                            mediaSaveNotifyThread.start();
                        } else
                            mediaSaveNotifyThread.setUri(uri);
                    } else {
                        if (uri != null) {
                            mActivity.notifyNewMedia(uri);
                        }
                    }
                    mActivity.updateStorageSpaceAndHint();
                }
            };

    public MediaSaveService.OnMediaSavedListener getMediaSavedListener() {
        return mOnMediaSavedListener;
    }

    static abstract class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        int mCamId;

        ImageAvailableListener(int cameraId) {
            mCamId = cameraId;
        }
    }

    static abstract class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {
        int mCamId;

        CameraCaptureCallback(int cameraId) {
            mCamId = cameraId;
        }
    }

    private class ZoomHandler extends Handler{
        public static final int MSG_UPDATE_ZOOM = 0;
        public static final int MSG_UPDATE_ZOOM_INSTANT = 1;
        ZoomHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            int id = CURRENT_ID;
            switch(what) {
                case MSG_UPDATE_ZOOM:
                    applyZoomAndUpdate(id,false);
                    mUI.updateFaceViewCameraBound(mCropRegion[id]);
                    mUI.updateT2TCameraBound(mCropRegion[id]);
                    mUI.updateStatsNNCameraBound(mCropRegion[id]);
                    mUI.updateAFBound(mCropRegion[id]);
                    break;

                case MSG_UPDATE_ZOOM_INSTANT:
                    removeMessages(MSG_UPDATE_ZOOM);
                    applyZoomAndUpdate(id,true);
                    sendEmptyMessageDelayed(MSG_UPDATE_ZOOM,30);
                    break;
            }
        }
    }

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder[] mPreviewRequestBuilder = new CaptureRequest.Builder[MAX_NUM_CAM];
    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int[] mState = new int[MAX_NUM_CAM];
    /**
     * A {@link Semaphore} make sure the camera open callback happens first before closing the
     * camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    public Face[] getPreviewFaces() {
        return mPreviewFaces;
    }

    public Face[] getStickyFaces() {
        return mStickyFaces;
    }
    /*----------- Add for autoTest start ------------*/
    private HashMap<String,Long> mHasMapTimes = new HashMap<>(); ;
    private boolean mFromOnOpened;
    private long mLockFocusTime;
    private long mPreCaptureTime;
    private long mLockAETime;
    private long mStartSnapShotTime;
    private long mClosedCamTime;
    private long mStartedTime;
    private long mSessionAfterRecord;
    private boolean mIsAutoSetting;
    private int[] mHDRModes;
    private int mHDRValues;
    public List<String> mLongImgTitle;
    public List<String> mImgType;
    public List<ExifInterface> mImagExif;
    public List<ExifInterface> getImagExif(){
        return mImagExif;
    }
    public void setImagExif(List<ExifInterface> exif){
        mImagExif = exif;
    }
    public CaptureResult getPreviewCaptureResult() {
        return mPreviewCaptureResult;
    }
    public TotalCaptureResult getCaptureResult(){
        return mCaptureResult;
    }
    public boolean isSessionClosed(int id){
        return (mCaptureSession[id] ==null);
    }
    public CameraCaptureSession getCurrentSession(int id){
        return mCaptureSession[id];
    }
    public List<String> getLongImageTitle(){return mLongImgTitle;}
    public List<String> getImgType(){return mImgType;}
    public OneUICameraControls getmCameraControls(){
        return mUI.getmCameraControls();
    }
    public Size getVideoSnapSize(){
        return mVideoSnapshotSize;
    }
    public Uri getVideoUri(){return mCurrentVideoUri;}
    public String getVideoFilePath(){
        return mVideoFilePath;
    }
    public void setAutoSetting(boolean auto){
        mIsAutoSetting = auto;
    }
    public boolean getAutoSetting(){
        return mIsAutoSetting;
    }
    public int[] getHDRModes(){
        return mHDRModes;
    }
    public int getHDRValues(){
        return mHDRValues;
    }
    public void setHDRModes(int[] value){
        mHDRModes = value;
    }
    public void setHDRValues(int value){
        mHDRValues = value;
    }
    public Size getSnapShotRawSize(){
        if(mSaveRaw && !isPhysicalRaw()) {
            return mPhysicalRawSize;
        }else{
            return mSupportedRawPictureSize;
        }
    }
    public Size getSnapShotJPGSize(){
        if(mSaveRaw && !isPhysicalRaw()) {
            return mPhysicalJPGSize;
        }else{
            return mPictureSize;
        }
    }
    public Size getYUVP010Size(){
        return mYUVP010Size;
    }
    float[] dynamicFpsConfig;
    public Range dynamicRange;
    public float[] getDynamicFpsConfig(){
        return dynamicFpsConfig;
    }
    public Range getFPSRange(){
        if (mHighSpeedCapture && !isVariableFPSEnabled()) {
            return mHighSpeedFPSRange;
        }else if(!isVariableFPSEnabled()){
            return new Range(30, 30);
        }
        return dynamicRange;
    }

    public CaptureUI getCaptureUI(){
        return mUI;
    }
    public void setVideFilePath(String path){
        mVideoFilePath = path;
    }
    public void setCaptureResult(TotalCaptureResult  result){
        mCaptureResult = result;
    }
    public void setPreviewCaptureResult(CaptureResult  result){
        mPreviewCaptureResult = result;
    }
    public void setLongImageTitle(List<String>  title){
        mLongImgTitle = title;
    }
    public void setImgType(List<String>  type){
        mImgType = type;
    }
    public void setStartedTime(long time){
        mStartedTime = time;
    }
    public long getStartedTime(){
        return mStartedTime;
    }
    public Rect getCameraRegion() {
        return mBayerCameraRegion;
    }
    public HashMap<String,Long> getHashMapTimes(){
        return mHasMapTimes;
    }
    public void resetHashMapTimes(){
        mHasMapTimes.clear();
        mStartedTime = 0;
        mActivity.mColdOpenCameraTime = 0;
    }
    public boolean getPaused(){
        return mPaused;
    }
    /*----------- Add for autoTest end ------------*/
    private void detectHDRMode(CaptureResult result, int id) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        String autoHdr = mSettingsManager.getValue(SettingsManager.KEY_AUTO_HDR);
        Byte hdrScene = result.get(CaptureModule.isHdr);
        if (value == null || hdrScene == null) return;
        mAutoHdrEnable = false;
        if (autoHdr != null && "enable".equals(autoHdr) && "0".equals(value) && hdrScene == 1) {
            mAutoHdrEnable = true;
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (mDrawAutoHDR2 != null) {
                        mDrawAutoHDR2.setVisibility(View.VISIBLE);
                        mDrawAutoHDR2.AutoHDR();
                    }
                }
            });
            return;
        } else {
            mActivity.runOnUiThread( new Runnable() {
                public void run () {
                    if (mDrawAutoHDR2 != null) {
                        mDrawAutoHDR2.setVisibility (View.INVISIBLE);
                    }
                }
            });
        }
    }



    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void processCaptureResult(CaptureResult result) {
            if("preview".equals(String.valueOf(result.getRequest().getTag()))){
                return;
            }
            int id = getIdFromTag(result.getRequest().getTag());
            if (id == getMainCameraId()) {
                mPreviewCaptureResult = result;
            }
            if (!mFirstPreviewLoaded) {
                String tag_ = String.valueOf(result.getRequest().getTag());
                int mainCameraId = getMainCameraId();
                String curTag = mainCameraId + "-" + getCurrenCameraMode().name();
                boolean shouldHideCover = curTag.equals(tag_);
                Log.i(TAG, "shouldHideCover " + shouldHideCover +
                        ", request tag " + tag_ + ", curTag " + curTag);
                if (shouldHideCover) {
                    mHandler.postDelayed(() -> {
                        mUI.hidePreviewCover();
                    }, 33L);
                    mFirstPreviewLoaded = true;
                    mUI.enableShutter(true);
                }
            }

            updateCaptureStateMachine(id, result);
            Integer ssmStatus = result.get(ssmCaptureComplete);
            if (ssmStatus != null) {
                Log.d(TAG, "ssmStatus: CaptureComplete is " + ssmStatus);
                updateProgressBar(true);
            }
            Integer procComplete = result.get(ssmProcessingComplete);
            if (procComplete != null && ++mCaptureCompleteCount == 1) {
                Log.d(TAG, "ssmStatus: ProcessingComplete is " + procComplete);
                mCaptureCompleteCount = 0;
                mSSMCaptureCompleteFlag = true;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopRecordingVideo(getMainCameraId());
                    }
                });
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            if("preview".equals(String.valueOf(partialResult.getRequest().getTag()))){
                return;
            }
            int id = getIdFromTag(partialResult.getRequest().getTag());
            if (id == getMainCameraId()) {
                Face[] faces = partialResult.get(CaptureResult.STATISTICS_FACES);
                Log.d(FD_TAG,BIG_LOG," Detected Face size = " + Integer.toString(faces == null? 0 : faces.length));
                if (faces != null && mSettingsManager.isFDRenderingAtPreview() && isCinematicDebugOn()){
                    boolean bsgEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_SMILE)||
                            mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GAZE)||
                            mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_BLINK);
                    boolean contourEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FACIAL_CONTOUR);
                    if (bsgEnable || contourEnable || isFacePointOn()
                            || mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_FACE_EXPRESSION)
                            || mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GENDER)) {
                        updateFaceView(faces, getBsgcInfo(partialResult, faces));
                    } else {
                        updateFaceView(faces, null);
                    }
                }
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            if("preview".equals(String.valueOf(result.getRequest().getTag())) || mPaused){
                return;
            }

            int id = getIdFromTag(result.getRequest().getTag());
            mVideoFrameNumber = result.getFrameNumber();
            updatePerformanceUIInfo(result);
            if (id == getMainCameraId()) {
                updateFocusStateChange(result);
                updateAWBCCTAndgains(result);
                updateAECGainAndExposure(result);
                updateAntiBandingMode(result);
                updateIsFickerDetected(result);
                String physical_id = mSettingsManager.getSinglePhysicalCamera();
                Face[] faces;
                if (physical_id != null &&
                        !SettingsManager.LOGICAL_AND_PHYSICAL.equals(physical_id)) {
                    faces = result.getPhysicalCameraResults().get(physical_id).get(
                            CaptureResult.STATISTICS_FACES);
                } else {
                    faces = result.get(CaptureResult.STATISTICS_FACES);
                }
                Log.d(FD_TAG,BIG_LOG,"onCaptureCompleted Detected Face size = " + Integer.toString(
                    faces == null ? 0 : faces.length) + ", Frame Number  :" + mVideoFrameNumber);
                if (faces != null && mSettingsManager.isFDRenderingAtPreview() && isCinematicDebugOn()
                        && !mSettingsManager.isMultiCameraEnabled()) {
                    boolean bsgEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_SMILE)||
                            mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GAZE)||
                            mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_BLINK);
                    boolean contourEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FACIAL_CONTOUR);
                    if (bsgEnable || contourEnable || isFacePointOn()
                            || mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_FACE_EXPRESSION)
                            || mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GENDER)
                            || mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_SKIN_TONE)) {
                        updateFaceView(faces, getBsgcInfo(result, faces));
                    } else {
                        updateFaceView(faces, null);
                    }
                    if (mIsFacialMaskSupported && mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FACE_MASK)) {
                        updateFacialMask(result);
                    }
                    if (mIsUpperBodySupported && mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_UPPER_BODY_DETECTION)) {
                        updateUpperBodyDetection(result);
                    }
                    if (mIsPetDetectionSupported && mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_PET_DETECTION)) {
                        updatePetDetection(result);
                    }
                }
                updateT2tTrackerView(result);
                mActivity.runOnUiThread(() -> {
                    mUI.updateLowLightText(result);
                });
            }

            detectHDRMode(result, id);
            processCaptureResult(result);
            mPostProcessor.onMetaAvailable(result);
            if (statsParametersUpdated <= STATS_PARAMETER_UPDATE &&
                    !mSettingsManager.isMultiCameraEnabled()) {

                updateStatsParameters(result);
            }
            String stats_visualizer = mSettingsManager.getValue(
                    SettingsManager.KEY_STATS_VISUALIZER_VALUE);
            if (mStatsVisualizer != null && mStatsVisualEnable.equals("1")
                    && stats_visualizer != null && !mSettingsManager.isMultiCameraEnabled()) {
                updateStatsView(stats_visualizer, result);
            } else {
                mUI.updateAWBInfoVisibility(View.GONE);
                mUI.updateAECInfoVisibility(View.GONE);
                updateBGStatsVisibility(View.GONE);
                updateBEStatsVisibility(View.GONE);
                updateGraghViewVisibility(View.GONE);
                updateRGBGraghViewVisibility(View.GONE);
                mUI.updateAFDInfoVisibility(View.GONE);
                mUI.updateAFInfoVisibility(View.GONE);
                mRSStatson = false;
                updateRSStatsVisibility(View.GONE);
            }
            if(mStatsVisualEnable.equals("1") && !mSettingsManager.isMultiCameraEnabled()){
                updateStatsAecIdsView(result);
            } else {
                mUI.updateAECIdInfoVisibility(View.GONE);
            }
            if (isSateNNFocusSettingOn() && isCinematicDebugOn()
                    && !mSettingsManager.isMultiCameraEnabled()) {
                updateStatsNNView(result);
            } else {
                mUI.updateStatsNNVisibility(View.GONE);
            }
            if(!mSettingsManager.isAICameraDisable()){
                updateVRSView(result);
            }
            if(isAIDE2Enabled()){
                try {
                    mAideAdrcGain = result.get(adrc_gain);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG,EXCEPTION_LOG,"no adrc_gain tag");
                }
                byte[] multiCameraIds = result.get(MultiCameraIds);
                if(multiCameraIds != null) {
                    mMasterCameraId = String.valueOf(byteArray2Int(multiCameraIds, 8));
                }
            }
            if(isAIDE2Enabled() || mSaveRaw) {
                byte[] ActiveCameraInfos = result.get(ActiveCameraInfo);
                synchronized (mActiveCameraIds) {
                    mActiveCameraIds.clear();
                    if (ActiveCameraInfos != null) {
                        int num = byteArray2Int(ActiveCameraInfos, 0);
                        for (int y = 1; y <= num; y++) {
                            int activeId = byteArray2Int(ActiveCameraInfos, y * 4);
                            mActiveCameraIds.add(activeId);
                        }
                    }
                }
            }
        }
    };

    private void updatePerformanceUIInfo(TotalCaptureResult result){
        if(mPerformanceDebugEnable != null && mPerformanceDebugEnable.equals("on")){
            if(mFirstRequestLatency != 0) {
                mFirstRequestLatency = System.currentTimeMillis() - mFirstRequestLatency;
                updatePerformanceDebugAllValue();
                if(mActivity.getPerformenceTest()) {
                    mHasMapTimes.put("FirstRequest->onCaptureCompleted",mFirstRequestLatency);
                    if(mActivity.mColdOpenCameraTime != 0){
                        mHasMapTimes.put("Total", System.currentTimeMillis() - mActivity.mColdOpenCameraTime);
                        mActivity.mColdOpenCameraTime = 0;
                    }else {
                        mHasMapTimes.put("Total", System.currentTimeMillis() - mStartedTime);
                    }

                }
                mFirstRequestLatency = 0;
            }
            Float zoomValue = result.getRequest().get(CaptureRequest.CONTROL_ZOOM_RATIO);
            synchronized (mPerformanceDebugData) {
                if (!mUI.getZoomFixedSupport()) {
                    Rect region = result.getRequest().get(CaptureRequest.SCALER_CROP_REGION);
                    Iterator<Float> it = mZoomValueMap.keySet().iterator();
                    while (it.hasNext()) {
                        Float ele = it.next();
                        if(mZoomValueMap.get(ele).left == region.left &&
                                mZoomValueMap.get(ele).top == region.top &&
                                mZoomValueMap.get(ele).bottom == region.bottom &&
                                mZoomValueMap.get(ele).right == region.right) {
                            zoomValue = ele;
                            it.remove();
                        }
                    }
                }
                Iterator<Long> it = mZoomTimeMap.keySet().iterator();
                while (it.hasNext()) {
                    Long ele = it.next();
                    if (Math.abs(zoomValue - mZoomTimeMap.get(ele)) < 0.01) {
                        mZoomLatency = System.currentTimeMillis() - ele;
                        updatePerformanceDebugValue(9, Long.toString(mZoomLatency));
                        it.remove();
                    }
                }
            }
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
            if(afState != null) {
                if (afState == CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN && mAFConvergence == 0) {
                    mAFConvergence = System.currentTimeMillis();
                } else if ((afState == CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED || afState == CaptureRequest.CONTROL_AF_STATE_PASSIVE_UNFOCUSED) && mAFConvergence != 0) {
                    mAFConvergence = System.currentTimeMillis() - mAFConvergence;
                    updatePerformanceDebugValue(10, Long.toString(mAFConvergence));
                    mAFConvergence = 0;
                }
            }
            if(aeState != null) {
                if (aeState == CaptureRequest.CONTROL_AE_STATE_SEARCHING && mAECConvergence == 0) {
                    mAECConvergence = System.currentTimeMillis();
                } else if (aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED && mAECConvergence != 0) {
                    mAECConvergence = System.currentTimeMillis() - mAECConvergence;
                    updatePerformanceDebugValue(11, Long.toString(mAECConvergence));
                    mAECConvergence = 0;
                }
            }
            if(awbState != null) {
                if (awbState == CaptureRequest.CONTROL_AWB_STATE_SEARCHING && mAWBConvergence == 0) {
                    mAWBConvergence = System.currentTimeMillis();
                } else if (awbState == CaptureRequest.CONTROL_AWB_STATE_CONVERGED && mAWBConvergence != 0) {
                    mAWBConvergence = System.currentTimeMillis() - mAWBConvergence;
                    updatePerformanceDebugValue(12, Long.toString(mAWBConvergence));
                    mAWBConvergence = 0;
                }
            }
            synchronized (mPerformanceGapData) {
                if (mLastResultTime != 0) {
                    if (mPerformanceGapData.size() > 100) {
                        mPerformanceGapData.remove(0);
                    }
                    mPerformanceGapData.add(System.currentTimeMillis() - mLastResultTime);
                }
                mLastResultTime = System.currentTimeMillis();
            }
            calculateResultFPS();
            updateGapGraghViewVisibility(View.VISIBLE);
            updateGapGraghView();
        } else {
            mUI.updatePerformanceDebugInfoVisibility(View.GONE);
            updateGapGraghViewVisibility(View.GONE);
            if((mActivity.getPerformenceTest()) && mFirstRequestLatency != 0) {
                String tag_ = String.valueOf(result.getRequest().getTag());
                int mainCameraId = getMainCameraId();
                String curTag = mainCameraId + "-" + getCurrenCameraMode().name();
                if(curTag.equals(tag_)) {
                    mFirstRequestLatency = System.currentTimeMillis() - mFirstRequestLatency;
                    mHasMapTimes.put("FirstRequest->onCaptureCompleted", mFirstRequestLatency);
                    mFirstRequestLatency = 0;
                    if (mActivity.mColdOpenCameraTime != 0) {
                        mHasMapTimes.put("Total", System.currentTimeMillis() - mActivity.mColdOpenCameraTime);
                        mActivity.mColdOpenCameraTime = 0;
                    } else {
                        mHasMapTimes.put("Total", System.currentTimeMillis() - mStartedTime);
                    }
                }
            }

        }
    }

    private void calculateResultFPS(){
        long currentTime = System.currentTimeMillis();
        long elapsedTime;

        if (0 == mLastFPSCountTime){
            mLastFPSCountTime  = currentTime;
            mCurrentFrameCount = 0;
        }else{
            mCurrentFrameCount++;
        }
        elapsedTime = currentTime - mLastFPSCountTime;

        if (elapsedTime > (5 * 1000)){
            float fps = mCurrentFrameCount * 1000 / (float) (elapsedTime);
            mResultFPS = (float)(Math.round(fps*100))/100;
            updatePerformanceDebugValue(13, Float.toString(mResultFPS));
            mCurrentFrameCount = 0;
            mLastFPSCountTime  = currentTime;
        }
    }
    public int byteArray2Int(byte[] src, int offset) {
        int value = 0;
        try {
            value = (int) ((src[offset] & 0xFF)
                    | ((src[offset + 1] & 0xFF) << 8)
                    | ((src[offset + 2] & 0xFF) << 16)
                    | ((src[offset + 3] & 0xFF) << 24));
        }catch(Exception e){
            Log.e(TAG,"src ="+src+",exception is "+e);
        }

        return value;
    }
    public float byteArray2float(byte[] arr, int index) {
        int i;
        i = arr[index + 0]&0xff;
        i |= ((long) arr[index + 1] << 8)&0xffff;
        i |= ((long) arr[index + 2] << 16)&0xffffff;
        i |= ((long) arr[index + 3] << 24);
        return Float.intBitsToFloat(i);
    }
    private void getHWMFandAIDETuningParams(CaptureResult result){
        try {
            byte[] param = result.get(HWMFNRandAIDE2TuningParams);
            if(param != null){
                denoiseStrengthParam = byteArray2float(param, 0);
                color_saturation = byteArray2float(param, 4);
                tone = byteArray2float(param, 8);
                detail_enhancement = byteArray2float(param, 12);
                mEnhancefactor = byteArray2float(param, 16);
                mGainThresholdY = param[20];
                mGainThresholdUV =  param[21];
                Log.d(TAG,"denoiseStrengthParam:" + denoiseStrengthParam + ",color_saturation:" + color_saturation + ",tone:" + tone
                +"detail_enhancement:" + detail_enhancement + ",mEnhancefactor:" + mEnhancefactor +",mGainThresholdY:" + mGainThresholdY + ",mGainThresholdUV:" + mGainThresholdUV);
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, EXCEPTION_LOG,"no SWMFandAIDETuningParams");
        }
    }
    private void updateT2TTracking() {
        mT2TFocusRenderer.setOriginalCameraBound(
                mSettingsManager.getSensorActiveArraySize(getMainCameraId()));
        mT2TFocusRenderer.setMirror(mSettingsManager.isFacingFront(getMainCameraId()));
        mT2TFocusRenderer.setDisplayOrientation(mDisplayOrientation);
        mT2TFocusRenderer.setCameraBound(mCropRegion[getMainCameraId()]);
        mT2TFocusRenderer.setZoomRationSupported(mUI.getZoomFixedSupport());
    }

    private void updateStatsNNTracking() {
        mStateNNFocusRenderer.setOriginalCameraBound(
                mSettingsManager.getSensorActiveArraySize(getMainCameraId()));
        mStateNNFocusRenderer.setMirror(mSettingsManager.isFacingFront(getMainCameraId()));
        mStateNNFocusRenderer.setDisplayOrientation(mDisplayOrientation);
        mStateNNFocusRenderer.setCameraBound(mCropRegion[getMainCameraId()]);
        mStateNNFocusRenderer.setZoomRationSupported(mUI.getZoomFixedSupport());
    }

    private void updateAFracking() {
        mAFRenderer.setOriginalCameraBound(
                mSettingsManager.getSensorActiveArraySize(getMainCameraId()));
        mAFRenderer.setMirror(mSettingsManager.isFacingFront(getMainCameraId()));
        mAFRenderer.setDisplayOrientation(mDisplayOrientation);
        mAFRenderer.setCameraBound(mCropRegion[getMainCameraId()]);
        mAFRenderer.setZoomRationSupported(mUI.getZoomFixedSupport());
    }

    private void updateTouchFocusState(int t2tTrigger) {
        int id = getMainCameraId();
        try {
            if (mPreviewRequestBuilder[id] != null) {
                mPreviewRequestBuilder[id].set(CaptureModule.t2t_cmd_trigger, t2tTrigger);
                if (mCurrentSceneMode.mode == CameraMode.HFR && mCurrentSession != null &&
                        mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                    if (mCurrentSession != null) {
                        List requestList = getHighSpeedList ((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,
                                mPreviewRequestBuilder[id]);
                        mCurrentSession.setRepeatingBurst(requestList, mCaptureCallback,
                                mCameraHandler);
                    }
                } else {
                    if (mCurrentSession != null) {
                        mCurrentSession.setRepeatingRequest(mPreviewRequestBuilder[id].build(),
                                mCaptureCallback, mCameraHandler);
                    }
                }
                    Log.v(TAG, "updateTouchFocusState is called");
            }
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
           Log.d(TAG,EXCEPTION_LOG,e.toString());
        }
    }

    private void updateT2tTrackerView(CaptureResult result) {
        int[] resultROI = null;
        int trackerScore = -1;
        if (mT2TFocusRenderer == null) {
            mT2TFocusRenderer = getT2TFocusRenderer();
            updateT2TTracking();
        }
        if (mT2TFocusRenderer.isShown()) {
            updateT2TTracking();
            try{
                if (result.get(t2t_tracker_status) != null) {
                    mT2TTrackState = result.get(t2t_tracker_status);
                    if (mLastT2tTrackState != mT2TTrackState) {
                        if (mT2TTrackState >= TouchTrackFocusRenderer.TRACKER_CMD_REG) {
                            // as long as State is 1 (registering) or 2 (registered) or 3 (tracking)
                            // we should be able to turn the "trigger" back to 0 (SYNC)
                            updateTouchFocusState(TouchTrackFocusRenderer.TRACKER_CMD_SYNC);
                        }
                        mLastT2tTrackState = mT2TTrackState;
                    }
                }
                if (result.get(t2t_tracker_score) != null) {
                    trackerScore = result.get(t2t_tracker_score);
                }
                if (result.get(t2t_tracker_result_roi) != null) {
                    resultROI = result.get(t2t_tracker_result_roi);
                    mT2TFocusRenderer.updateTrackerRect(resultROI, trackerScore);
                }
                    Log.v(TAG, BIG_LOG,"mT2TTrackState :" + mT2TTrackState +
                            ", trackerScore :" +trackerScore+", resultROI :" + resultROI);
            } catch (IllegalArgumentException | BufferUnderflowException e) {
                Log.d(TAG,EXCEPTION_LOG,e.toString());
            }
        }
    }

    private void updateStatsNNView(CaptureResult result) {
        if (result != null) {
            try {
                Byte statsNNWidthObj = result.get(stats_nn_result_width);
                Byte statsNNHeightObj = result.get(stats_nn_result_height);
                Byte statsNNMapdataObj = result.get(stats_nn_result_mapdata);
                Byte statsNNNumroiObj = result.get(stats_nn_result_numroi);
                int[] statsNNRoiData = result.get(stats_nn_result_roidata);
                Integer statsNNRoiWeightObj = result.get(stats_nn_result_roiweight);

                byte statsNNWidth = (statsNNWidthObj != null) ? statsNNWidthObj : 0;
                byte statsNNHeight = (statsNNHeightObj != null) ? statsNNHeightObj : 0;
                byte statsNNMapdata = (statsNNMapdataObj != null) ? statsNNMapdataObj : 0;
                byte statsNNNumroi = (statsNNNumroiObj != null) ? statsNNNumroiObj : 0;

                int statsNNRoiWeight = (statsNNRoiWeightObj != null) ? statsNNRoiWeightObj : 0;
                Log.d(TAG, BIG_LOG, "statsNNWidth:" + statsNNWidth + ",statsNNHeight:" + statsNNHeight + ",statsNNMapdata:" + statsNNMapdata +
                        ",statsNNNumroi:" + statsNNNumroi + ",statsNNRoiData:" + statsNNRoiData + ",statsNNRoiWeight:" + statsNNRoiWeight);

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (statsNNWidthObj == null || statsNNHeightObj == null || statsNNMapdataObj == null ||
                        statsNNNumroiObj == null  || statsNNRoiData == null || statsNNRoiWeightObj == null || statsNNRoiData.length < 4) {
                            return;
                        }
                        if (statsNNNumroi == 1) {
                            mUI.updateStatsNNVisibility(View.VISIBLE);
                            mStateNNFocusRenderer.setVisible(true);
                            mUI.updateStatsNNResultText(statsNNWidth, statsNNHeight, statsNNMapdata, statsNNNumroi, statsNNRoiData, statsNNRoiWeight);
                        } else {
                            mUI.updateStatsNNVisibility(View.INVISIBLE);
                            mStateNNFocusRenderer.setVisible(false);
                        }
                    }
                });
                if (mStateNNFocusRenderer == null) {
                    mStateNNFocusRenderer = getStatsNNFocusRenderer();
                    updateStatsNNTracking();
                }
                if (mStateNNFocusRenderer.isShown()) {
                    updateStatsNNTracking();
                    mStateNNFocusRenderer.updateTrackerRect(statsNNRoiData);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG, EXCEPTION_LOG, e.toString());
            }
        }
    }
    private void updateStatsAecIdsView(CaptureResult result){
        //camera id && request id info
        for (int i = 0; i < camerainfo_data.length;i++)
            camerainfo_data[i] = "";
        try{
            camerainfo_data[0] = Long.toString(result.get(stats_visualizer_request_id));
            camerainfo_data[1] = Integer.toString(result.get(stats_visualizer_camera_id));;
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.w(TAG,EXCEPTION_LOG,e.toString());
        }
        synchronized (camerainfo_data) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUI.updateAECIdInfoVisibility(View.VISIBLE);
                    mUI.updateAecIdsInfoText(camerainfo_data);
                }
            });
        }
    }

    private void updateGapGraghView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGapGraphView != null) {
                    mGapGraphView.PreviewChanged();
                }
            }
        });
    }

    private void updateGapGraghViewVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGapGraphView != null) {
                    mGapGraphView.setVisibility(visibility);
                }
            }
        });
    }

    private void updateVRSView(CaptureResult result){
        try{
            if(result.get(VRSSkipSegment) != -1) {
                updateVRSText(new StringBuilder("AIVRS ").append(result.get(VRSSkipSegment)).toString());
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.w(TAG,EXCEPTION_LOG,e.toString());
            updateVRSText("");
        }
    }

    private void updateVRSText(String text){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUI.updateVRSText(text);
            }
        });
    }
    private void updatePerformanceDebugView() {
        synchronized (mPerformanceDebugData) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUI.updatePerformanceDebugInfoVisibility(View.VISIBLE);
                    mUI.updatePerformanceDebugInfoText(mPerformanceDebugData);
                }
            });
        }
    }
    private void updatePerformanceDebugValue(int i, String value) {
        synchronized (mPerformanceDebugData) {
            mPerformanceDebugData[i] = value;
            updatePerformanceDebugView();
        }
    }

    private void updatePerformanceDebugAllValue() {
        synchronized (mPerformanceDebugData) {
            mPerformanceDebugData[0]=Long.toString(mFlushLatency);
            mPerformanceDebugData[1]=Long.toString(mCloseCameraLatency);
            mPerformanceDebugData[2]=Long.toString(mOpenCameraLatency);
            mPerformanceDebugData[3]=Long.toString(mSettingInitLatency);
            mPerformanceDebugData[4]=Long.toString(mCreateSessionLatency);
            mPerformanceDebugData[5]=Long.toString(mFirstRequestLatency);
            mPerformanceDebugData[6]=Long.toString(mSnapshotLatency);
            mPerformanceDebugData[7]=Long.toString(mShutterLag);
            mPerformanceDebugData[8]=Float.toString(mBurstFps);
            mPerformanceDebugData[9]=Long.toString(mZoomLatency);
            mPerformanceDebugData[10]=Long.toString(mAFConvergence);
            mPerformanceDebugData[11]=Long.toString(mAECConvergence);
            mPerformanceDebugData[12]=Long.toString(mAWBConvergence);
            mPerformanceDebugData[13]=Float.toString(mResultFPS);
            updatePerformanceDebugView();
        }
    }
    private void updateStatsView(String stats_visualizer,CaptureResult result) {
        int r, g, b, index;
        if (stats_visualizer.contains("2")) {
            try {
                int[] histogramStats = result.get(CaptureModule.histogramStats);
                if (histogramStats != null && mHiston) {
                    /*The first element in the array stores max hist value . Stats data begin
                    from second value*/
                    STATS_DATA = histogramStats.length;
                    statsdata = new int[STATS_DATA];
                    synchronized (statsdata) {
                        System.arraycopy(histogramStats, 0, statsdata, 0, STATS_DATA);
                    }
                    int binCount = result.get(CaptureModule.buckets);
                    int statsType = result.get(CaptureModule.stats_type);
                    Log.d(TAG, "binCount:" + binCount + ",statsType:" + statsType + ",data length:" + histogramStats.length);
                    if (statsType == 6 && binCount == 256) {
                        updateRGBGraghViewVisibility(View.INVISIBLE);
                        updateGraghViewVisibility(View.VISIBLE);
                        updateGraghView();
                    } else if (binCount !=0){
                        updateGraghViewVisibility(View.INVISIBLE);
                        updateRGBGraghViewVisibility(View.VISIBLE);
                        updateRGBGraghView();
                    }
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }

        // BG stats display
        if (stats_visualizer.contains("0")) {
            int[] bgRStats = null;
            int[] bgGStats = null;
            int[] bgBStats = null;
            try{
                bgRStats = result.get(CaptureModule.bgRStats);
                bgGStats = result.get(CaptureModule.bgGStats);
                bgBStats = result.get(CaptureModule.bgBStats);
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
            Log.d(TAG,"BG, bgRStats:" + bgRStats + ",bgGStats:" + bgGStats + ",bgBStats:" + bgBStats + ",mBGStatson:" + mBGStatson);

            if (bgRStats != null && bgGStats != null && bgBStats != null && mBGStatson) {
                synchronized (bg_r_statsdata) {
                    if(bg_r_statsdata.length != bgRStats.length) {
                        Log.d(TAG,"be_r_statsdata.length="+bg_r_statsdata.length +",beRStats.length="+bgRStats.length);
                        updateStatsParameters(result);
                    }
                    System.arraycopy(bgRStats, 0, bg_r_statsdata, 0, bgRStats.length);
                    System.arraycopy(bgGStats, 0, bg_g_statsdata, 0, bgGStats.length);
                    System.arraycopy(bgBStats, 0, bg_b_statsdata, 0, bgBStats.length);

                    int width = BGSTATS_WIDTH / STATS_LENGTH;
                    int height = BGSTATS_HEIGHT / STATS_LENGTH;
                    for (int el = 0; el < BGSTATS_DATA; el++) {
                        r = bg_r_statsdata[el] >> STATS_DATA_BIT_SHIFT;
                        g = bg_g_statsdata[el] >> STATS_DATA_BIT_SHIFT;
                        b = bg_b_statsdata[el] >> STATS_DATA_BIT_SHIFT;

                        for (int hi = 0; hi < SCALE_STATS; hi++) {
                            for (int wi = 0; wi < SCALE_STATS; wi++) {
                                index = SCALE_STATS * (int) (el / height) + width * SCALE_STATS * hi + width * SCALE_STATS * SCALE_STATS * (el % height) + wi;
                                bg_statsdata[index] = Color.argb(255, r, g, b);
                            }
                        }
                    }
                }
                updateBGStatsView();
            }
        }

        // BE stats display
        if (stats_visualizer.contains("1")) {
            int[] beRStats = null;
            int[] beGStats = null;
            int[] beBStats = null;
            float norm_roi_x = 0.0f;
            float norm_roi_y = 0.0f;
            float norm_roi_dx = 0.0f;
            float norm_roi_dy = 0.0f;
            try {
                beRStats = result.get(CaptureModule.beRStats);
                beGStats = result.get(CaptureModule.beGStats);
                beBStats = result.get(CaptureModule.beBStats);
                Log.d(TAG,"BE, beRStats:" + beRStats + ",beGStats:" + beGStats + ",beBStats:" + beBStats + ",mBEStatson:" + mBEStatson);
                norm_roi_x = result.get(CaptureModule.roiBeX);
                norm_roi_y = result.get(CaptureModule.roiBeY);
                norm_roi_dx = result.get(CaptureModule.roiBeWidth);
                norm_roi_dy = result.get(CaptureModule.roiBeHeight);
            } catch (IllegalArgumentException | NullPointerException e ) {
                Log.w(TAG, EXCEPTION_LOG," read vendor roiBeX/roiBeY/roiBeWidth/roiBeHeight exception="+e
                +",CaptureModule.roiBeX ="+result.get(CaptureModule.roiBeX));
            }

            if (beRStats != null && beGStats != null && beBStats != null && mBEStatson) {

                synchronized (be_r_statsdata) {
                    if(be_r_statsdata.length != beRStats.length) {
                        Log.d(TAG,"be_r_statsdata.length="+be_r_statsdata.length +",beRStats.length="+beRStats.length);
                        updateStatsParameters(result);
                    }
                    System.arraycopy(beRStats, 0, be_r_statsdata, 0, beRStats.length);
                    System.arraycopy(beGStats, 0, be_g_statsdata, 0, beRStats.length);
                    System.arraycopy(beBStats, 0, be_b_statsdata, 0, beRStats.length);

                    int width = BESTATS_WIDTH / STATS_LENGTH;
                    int height = BESTATS_HEIGHT / STATS_LENGTH;
                    int roi_x = (int)(norm_roi_x * height);
                    int roi_y = (int)(norm_roi_y * width);
                    int roi_w = (int)((norm_roi_x + norm_roi_dx) * height);
                    int roi_h = (int)((norm_roi_y + norm_roi_dy) * width);

                    for (int el = 0; el < BESTATS_DATA; el++) {
                        r = be_r_statsdata[el] >> STATS_DATA_BIT_SHIFT;
                        g = be_g_statsdata[el] >> STATS_DATA_BIT_SHIFT;
                        b = be_b_statsdata[el] >> STATS_DATA_BIT_SHIFT;

                        for (int hi = 0; hi < SCALE_STATS; hi++) {
                            for (int wi = 0; wi < SCALE_STATS; wi++) {
                                index = SCALE_STATS * (int) (el / height) + width * SCALE_STATS * hi + width * SCALE_STATS * SCALE_STATS * (el % height) + wi;
                                be_statsdata[index] = Color.argb(255, r, g, b);
                                if (roi_w > 0 && roi_h > 0 &&
                                        ((el % height == roi_x && el / height >= roi_y && el / height <= roi_h)
                                                || (el % height == roi_w && el / height >= roi_y && el / height <= roi_h) ||
                                                (el / height == roi_y && el % height >= roi_x && el % height <= roi_w) ||
                                                (el / height == roi_h && el % height >= roi_x && el % height <= roi_w))) {
                                    // red color for ROI border
                                    be_statsdata[index] = Color.argb(255, 255, 0, 0);
                                }
                            }
                        }
                    }
                }
                updateBEStatsView();
            }
        }

        // RS stats display
        if (stats_visualizer.contains("3")) {
            mRSStatson = true;
            int[] rsRStats = null;
            int[] rsGStats = null;
            int[] rsBStats = null;
            try {
                rsRStats = result.get(CaptureModule.rsStats);
                rsGStats = result.get(CaptureModule.rsStats);
                rsBStats = result.get(CaptureModule.rsStats);
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG, EXCEPTION_LOG, e.toString());
            }

            if (rsRStats != null && rsGStats != null && beBStats != null && mRSStatson) {
                synchronized (rs_r_statsdata) {
                    System.arraycopy(rsRStats, 0, rs_r_statsdata, 0, rsRStats.length);
                    System.arraycopy(rsGStats, 0, rs_g_statsdata, 0, rsGStats.length);
                    System.arraycopy(rsBStats, 0, rs_b_statsdata, 0, rsBStats.length);
                    int width = RSSTATS_WIDTH / STATS_LENGTH;
                    int height = RSSTATS_HEIGHT / STATS_LENGTH;
                    for (int el = 0; el < RSSTATS_DATA; el++) {
                        r = rs_r_statsdata[el];
                        g = rs_g_statsdata[el];
                        b = rs_b_statsdata[el];
                        for (int hi = 0; hi < SCALE_STATS; hi++) {
                            for (int wi = 0; wi < SCALE_STATS; wi++) {
                                index = SCALE_STATS * (int) (el / height) + width * SCALE_STATS * hi + width * SCALE_STATS * SCALE_STATS * (el % height) + wi;
                                rs_statsdata[index] = Color.argb(255, r, g, b);
                            }
                        }
                    }
                }
                updateRSStatsVisibility(View.VISIBLE);
                updateRSStatsView();
            }
        }

        // AWB Info display
        if (stats_visualizer.contains("5")) {
            try{
                awbinfo_data[0] = Float.toString(mRGain);
                awbinfo_data[2] = Float.toString(mBGain);
                awbinfo_data[3] = Float.toString(mCctAWB);
                synchronized (awbinfo_data) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.updateAWBInfoVisibility(View.VISIBLE);
                            mUI.updateAwbInfoText(awbinfo_data);
                        }
                    });
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        } else {
            mUI.updateAWBInfoVisibility(View.GONE);
        }

        // AEC Info display
        if (stats_visualizer.contains("4")) {
            for (int i = 0; i < aecinfo_data.length;i++)
                aecinfo_data[i] = "";
            try {
                aecinfo_data[0] = Float.toString(mAecFramecontrolLuxIndex);
                aecinfo_data[1] = String.format("%.5f", mAecFramecontrolLinearGain[0]);
                aecinfo_data[2] = String.format("%.5f", mAecFramecontrolLinearGain[2]);
                aecinfo_data[3] = String.format("%.5f", mAecFramecontrolLinearGain[1]);
                aecinfo_data[4] = String.format("%.2E", mAecFramecontrolSensitivity[0]);
                aecinfo_data[5] = String.format("%.2E", mAecFramecontrolSensitivity[2]);
                aecinfo_data[6] = String.format("%.2E", mAecFramecontrolSensitivity[1]);
                aecinfo_data[7] = Long.toString(mAecFramecontrolExosureTime[0]);
                aecinfo_data[8] = Long.toString(mAecFramecontrolExosureTime[2]);
                aecinfo_data[9] = Long.toString(mAecFramecontrolExosureTime[1]);
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }

            try{
                aecinfo_data[10] = Float.toString(result.get(ratio_long_to_short));
                aecinfo_data[11] = Float.toString(result.get(ratio_long_to_safe));
                aecinfo_data[12] = Float.toString(result.get(ratio_safe_to_short));
                aecinfo_data[13] = Float.toString(result.get(aecFrame_adrc_gain));
                aecinfo_data[14] = Float.toString(result.get(aecFrame_dark_boost_gain));
            }catch (NullPointerException|IllegalArgumentException e){
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }

            synchronized (aecinfo_data) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUI.updateAECInfoVisibility(View.VISIBLE);
                        mUI.updateAecInfoText(aecinfo_data);
                    }
                });
            }
        } else {
            mUI.updateAECInfoVisibility(View.GONE);
        }
        // AFD Info display
        if (stats_visualizer.contains("6")) {
            for (int i = 0; i < afdinfo_data.length;i++)
                afdinfo_data[i] = "";
            try {
                float[] afdRSTime = result.get(afd_rs_time);
                int[] afdLinesFrame = result.get(afd_lines_frame);
                afdinfo_data[0] = Integer.toString(result.get(afd_hnum));
                afdinfo_data[1] = Integer.toString(result.get(afd_vnum));
                afdinfo_data[2] = Float.toString(result.get(afd_visible_bands));
                afdinfo_data[3] = String.format("%.5f", afdRSTime[0]);
                afdinfo_data[4] = String.format("%.5f", afdRSTime[1]);
                afdinfo_data[5] = String.format("%.5f", afdRSTime[2]);
                afdinfo_data[6] = String.format("%.5f", afdRSTime[3]);
                afdinfo_data[7] = Integer.toString(result.get(afd_anti_banding_mode));
                afdinfo_data[8] = Integer.toString(afdLinesFrame[0]);
                afdinfo_data[9] = Integer.toString(afdLinesFrame[1]);
                afdinfo_data[10] = Integer.toString(afdLinesFrame[2]);
                afdinfo_data[11] = Integer.toString(afdLinesFrame[3]);
                afdinfo_data[12] =  String.format("%.5f",result.get(avg_rolling_conf));
                afdinfo_data[13] = String.format("%.5f",result.get(avg_static_conf));
                afdinfo_data[14] = String.format("%.5f",result.get(avg_rolling_energy));
                afdinfo_data[15] = String.format("%.5f",result.get(avg_static_energy));
            }catch (NullPointerException|IllegalArgumentException e){
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
            synchronized (afdinfo_data) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUI.updateAFDInfoVisibility(View.VISIBLE);
                        mUI.updateAfdInfoText(afdinfo_data);
                    }
                });
            }
        } else {
            mUI.updateAFDInfoVisibility(View.GONE);
        }
        // AF Info display
        if (stats_visualizer.contains("7")) {
            for (int i = 0; i < afinfo_data.length;i++)
                afinfo_data[i] = "";
            try {
                afinfo_data[0] = Byte.toString(result.get(isPDENABLE));
                afinfo_data[1] = Integer.toString(result.get(pd_type));
                afinfo_data[2] = Byte.toString(result.get(isSparseHW));
                afinfo_data[3] = Byte.toString(result.get(isDualPDHW));
                afinfo_data[4] = Byte.toString(result.get(isLCRHW));
                afinfo_data[5] = Byte.toString(result.get(isLCRSW));
                afinfo_data[6] = Integer.toString(result.get(lenspos));
                byte[] roi = result.get(autofocusroi);
                //autofocusroi is left,top,width, height, transfer to left,top,right, bottom
                mAFRoi[0] = byteArray2Int(roi, 0);
                mAFRoi[1] = byteArray2Int(roi, 4);
                mAFRoi[2] = byteArray2Int(roi, 0) + byteArray2Int(roi, 8);
                mAFRoi[3] = byteArray2Int(roi, 4) + byteArray2Int(roi, 12);
                Log.d(TAG,"mAFRoi[0]:" + mAFRoi[0] +"mAFRoi[1]:" + mAFRoi[1] +"mAFRoi[2]:" + mAFRoi[2] + "mAFRoi[3]:" + mAFRoi[3]);
            }catch (NullPointerException|IllegalArgumentException e){
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
            synchronized (afinfo_data) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUI.updateAFInfoVisibility(View.VISIBLE);
                        mUI.updateAfInfoText(afinfo_data);
                    }
                });
            }
            if (mAFRenderer == null) {
                mAFRenderer = mUI.getAFRenderer();
                updateAFracking();
            }
            if (mAFRenderer.isShown()) {
                updateAFracking();
                mAFRenderer.updateTrackerRect(mAFRoi);
            }
        } else {
            mUI.updateAFInfoVisibility(View.GONE);
        }
    }

    public boolean isSateAFSettingOn(){
        String stats_visualizer = mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        if(stats_visualizer != null && stats_visualizer.contains("7")){
            return true;
        }
        return false;
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mOpenCameraLatency = System.currentTimeMillis() - mOpenCameraLatency;
            if(mActivity.getPerformenceTest()) {
                mHasMapTimes.put("openCamera->onOpened",mOpenCameraLatency);
                mFromOnOpened = true;
            }
            mSettingInitLatency = System.currentTimeMillis();
            int id = Integer.parseInt(cameraDevice.getId());
            Log.i(TAG, "onOpened " + id);
            mCameraOpenCloseLock.release();
            if (mPaused) {
                return;
            }

            mCameraDevice[id] = cameraDevice;
            mCameraOpened[id] = true;
            mIsCloseCamera = false;

            if (isBackCamera() && getCameraMode() == DUAL_MODE && id == BAYER_ID) {
                Message msg = mCameraHandler.obtainMessage(OPEN_CAMERA, MONO_ID, 0);
                mCameraHandler.sendMessage(msg);
            } else {
                mCamerasOpened = true;
                createSessions();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.i(TAG, "onDisconnected " + id);
            cameraDevice.close();
            mCameraDevice[id] = null;
            mCameraOpenCloseLock.release();
            mCamerasOpened = false;
            mIsCloseCamera = true;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.e(TAG, "CameraDevice onError " + id + " " + error);
            mCameraOpenCloseLock.release();
            mCamerasOpened = false;
            if((error == 1 || error == 2) && mOpenCameraTimes >0){
                Log.i(TAG," mOpenCameraTimes="+mOpenCameraTimes);
                mOpenCameraTimes --;
                Message msg = mCameraHandler.obtainMessage(OPEN_CAMERA, getMainCameraId(), 0);
                mCameraHandler.sendMessageDelayed(msg,300);
                return;
            }
            if (null != mActivity) {
                String errmsg = ""+error;
                switch(error) {
                    case 1:
                        errmsg = "ERROR_CAMERA_IN_USE:" +
                                "please close the application who is using camera.";
                        break;
                    case 2:
                        errmsg = "ERROR_MAX_CAMERAS_IN_USE:" +
                                "More camera devices cannot be opened until previous instances are closed.";
                        break;
                    case 3:
                        errmsg = "ERROR_CAMERA_DISABLED:" +
                                "Please check which application USES_POLICY_DISABLE_CAMERA.";
                        break;
                    case 4:
                        errmsg = "ERROR_CAMERA_DEVICE:The camera device needs to be re-opened to be used again.";
                        break;
                    case 5:
                        errmsg = "ERROR_CAMERA_SERVICE:" +
                                "The Android device may need to be shut down and restarted" +
                                " to restore camera function, or there may be a persistent hardware problem.";
                        break;
                }
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage("Open camera error!Camera id:"+ id+"\nError reason is "+errmsg);
                Dialog dialog = alert.show();
                mHandler.postDelayed(() -> {
                    dialog.dismiss();
                    System.exit(0);
                }, 5000L);
            } else {
                System.exit(0);
            }
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.i(TAG,"onClosed mCameraDevice[id]="+mCameraDevice[id]+",id="+id+",cameraDevice="+cameraDevice
            +",getMainCameraId()="+getMainCameraId()+",CURRENT_ID="+CURRENT_ID);
            if((mCameraDevice[id] == null || mCameraDevice[id].equals(cameraDevice))
                    && id == CURRENT_ID){
                mCloseCameraLatency = System.currentTimeMillis() - mCloseCameraLatency;
                if (mActivity.getPerformenceTest()) {
                    mClosedCamTime = System.currentTimeMillis();
                    mHasMapTimes.put("closeCamera->onClosed",mCloseCameraLatency);
                }
                mCameraDevice[id] = null;
                mCameraOpenCloseLock.release();
                mCamerasOpened = false;
                mIsCloseCamera = true;
            }
        }

    };

    public MultiResolutionImageReader initOutputMultiImageReader(int format) {
        MultiResolutionStreamConfigurationMap multiResolutionMap = mMainCameraCharacteristics.get(
                CameraCharacteristics.SCALER_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP);
        int[] formats = multiResolutionMap.getOutputFormats();
        if (!CameraUtil.contains(formats, format)) {
            Log.w(TAG, "Camera " + getMainCameraId() + " doesn't support multi-resolution output "
                    + " stream for format " + format + ". Supported formats are "
                    + Arrays.toString(formats));
        }
        Collection<MultiResolutionStreamInfo> multiResolutionStreams =
                multiResolutionMap.getOutputInfo(format);
        Log.d(TAG, "MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP: " + multiResolutionMap.toString());
        return new MultiResolutionImageReader(multiResolutionStreams, format, MAX_MULTIIMAGES);
    }

    private void initReprocessMultiImageReader(int format) {
        MultiResolutionStreamConfigurationMap multiResolutionMap = mMainCameraCharacteristics.get(
                CameraCharacteristics.SCALER_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP);
        int[] formats = multiResolutionMap.getInputFormats();
        if (!CameraUtil.contains(formats, format)) {
            Log.w(TAG, "Camera " + getMainCameraId() + " doesn't support multi-resolution input "
                    + " stream for format " + format + ". Supported formats are "
                    + Arrays.toString(formats));
        }
        Collection<MultiResolutionStreamInfo> multiResolutionStreams =
                multiResolutionMap.getInputInfo(format);
        mInputConfig = new InputConfiguration(multiResolutionStreams, format);
        mMultiResImageReader = new MultiResolutionImageReader(multiResolutionStreams, format, MAX_IMAGEREADERS);
        mMultiResImageReader.setOnImageAvailableListener(mPostProcessor.getImageHandler(),
                new HandlerExecutor(mImageAvailableHandler));
        mPostProcessor.onMultiImageReaderReady(mMultiResImageReader);
    }

    private void initRepocessImageReader(int format) {
        int i = getMainCameraId();
        mImageReader[i] = ImageReader.newInstance(mPictureSize.getWidth(),
                mPictureSize.getHeight(), format, MAX_IMAGEREADERS + 2);
        if (mSaveRaw) {
            mRawImageReader[i] = ImageReader.newInstance(mSupportedRawPictureSize.getWidth(),
                    mSupportedRawPictureSize.getHeight(), mSettingsManager.getRawFormat(), MAX_IMAGEREADERS + 2);
            mPostProcessor.setRawImageReader(mRawImageReader[i]);
        }
        mImageReader[i].setOnImageAvailableListener(mPostProcessor.getImageHandler(), mImageAvailableHandler);
        mPostProcessor.onImageReaderReady(mImageReader[i], mSupportedMaxPictureSize, mPictureSize);
    }

    private void uninitMultiResolutionImageReader() {
        if (mMultiResImageReader != null) {
            mMultiResImageReader.close();
            mMultiResImageReader = null;
        }
    }

    private void updateCaptureStateMachine(int id, CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        Log.d(TAG,BIG_LOG,"mState[id]="+mState[id]+ " afState:" + afState + " aeState:"+aeState
                +",result.getRequest().hashCode()="+result.getRequest().hashCode());
        switch (mState[id]) {
            case STATE_PREVIEW: {
                break;
            }
            case STATE_WAITING_AF_LOCK: {
                Log.d(TAG, "STATE_WAITING_AF_LOCK id: " + id + " afState:" + afState + " aeState:"
                        + aeState+",LockRequestHashCode[id]="+mLockRequestHashCode[id]+
                        ",result.getRequest().hashCode()="+result.getRequest().hashCode());
                // AF_PASSIVE is added for continous auto focus mode
                if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState ||
                        CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState ||
                        (mLockRequestHashCode[id] == result.getRequest().hashCode() &&
                                afState == CaptureResult.CONTROL_AF_STATE_INACTIVE)) {
                    if(id == MONO_ID && getCameraMode() == DUAL_MODE && isBackCamera()) {
                        // in dual mode, mono AE dictated by bayer AE.
                        // if not already locked, wait for lock update from bayer
                        if(aeState == CaptureResult.CONTROL_AE_STATE_LOCKED)
                            checkAfAeStatesAndCapture(id);
                        else
                            mState[id] = STATE_WAITING_AE_LOCK;
                    } else {
                        if ((mLockRequestHashCode[id] == result.getRequest().hashCode()) || (mLockRequestHashCode[id] == 0)) {

                            // CONTROL_AE_STATE can be null on some devices
                            if(aeState == null || (aeState == CaptureResult
                                    .CONTROL_AE_STATE_CONVERGED) && isFlashOff(id)) {
                                lockExposure(id);
                            } else {
                                runPrecaptureSequence(id);
                                if(mCaptureTorchTrigger){
                                    //after AE_PRECAPTURE_TRIGGER 1, AEMode:1 and flashMode:2 and continue the value till captureIntent:2
                                    mPreviewRequestBuilder[id].set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                    mPreviewRequestBuilder[id].set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                                    try {
                                        mCaptureSession[id].setRepeatingRequest(
                                                mPreviewRequestBuilder[id].build(), mCaptureCallback,
                                                mCameraHandler);
                                    } catch (CameraAccessException | IllegalStateException e) {
                                        Log.e(TAG,e);
                                    }
                                }
                            }
                        }
                    }
                } else if (mLockRequestHashCode[id] == result.getRequest().hashCode()){
                    Log.i(TAG, "AF lock request result received, but not focused");
                    mLockRequestHashCode[id] = 0;
                } else if (mSettingsManager.isFixedFocus(id) || mUI.getCurrentProMode() == ProMode.MANUAL_MODE) {
                    // CONTROL_AE_STATE can be null on some devices
                    if(aeState == null || (aeState == CaptureResult
                             .CONTROL_AE_STATE_CONVERGED) && isFlashOff(id)) {
                        lockExposure(id);
                    } else {
                        runPrecaptureSequence(id);
                    }
                }
                break;
            }
            case STATE_WAITING_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Log.d(TAG, "STATE_WAITING_PRECAPTURE id: " + id + " afState: " + afState + " aeState:"
                        + aeState+",mPrecaptureRequestHashCode[id]="+mPrecaptureRequestHashCode[id]
                        +",result.getRequest().hashCode()="+result.getRequest().hashCode());

                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED ||
                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    if (mCaptureTorchTrigger) {
                        if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            //after snapshot request, app should set AEMode=2/3(based on user selection in APP), flash 0 again
                            checkAfAeStatesAndCapture(id);
                            applyFlash(mPreviewRequestBuilder[id], getMainCameraId());
                            try {
                                mCaptureSession[id].setRepeatingRequest(
                                        mPreviewRequestBuilder[id].build(), mCaptureCallback,
                                        mCameraHandler);
                            } catch (CameraAccessException | IllegalStateException e) {
                                Log.e(TAG, e);
                            }
                        }
                    } else {
                        if ((mPrecaptureRequestHashCode[id] == result.getRequest().hashCode()) || (mPrecaptureRequestHashCode[id] == 0)) {
                            if ((mLongshotActive && isFlashOn(id))) {
                                checkAfAeStatesAndCapture(id);
                            } else {
                                lockExposure(id);
                            }
                        }
                    }
                } else if (aeState == CaptureResult.CONTROL_AE_STATE_INACTIVE ||
                        aeState == CaptureResult.CONTROL_AE_STATE_LOCKED) {
                    if ((mPrecaptureRequestHashCode[id] == result.getRequest().hashCode()) || (mPrecaptureRequestHashCode[id] == 0)) {
                        // AE Mode is OFF, the AE state is always CONTROL_AE_STATE_INACTIVE
                        // then begain capture and ignore lock AE.
                        checkAfAeStatesAndCapture(id);
                    }
                } else if (mPrecaptureRequestHashCode[id] ==  result.getRequest().hashCode()) {
                    Log.i(TAG, "AE trigger request result received, but not converged");
                    mPrecaptureRequestHashCode[id] = 0;
                }
                break;
            }
            case STATE_WAITING_AE_LOCK: {
                // CONTROL_AE_STATE can be null on some devices
                Log.d(TAG, "STATE_WAITING_AE_LOCK id: " + id + " afState: " + afState + " aeState:" + aeState + ",mLockAFAE:" + mLockAFAE);
                if(mLockAFAE == LOCK_AF_AE_STATE_START || mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
                    mState[id] = STATE_AF_AE_LOCKED;
                    break;
                }
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_LOCKED) {
                    checkAfAeStatesAndCapture(id);
                }
                break;
            }
            case STATE_AF_AE_LOCKED: {
                Log.d(TAG, "STATE_AF_AE_LOCKED id: " + id + " afState:" + afState + " aeState:" + aeState + "mLockAFAE" + mLockAFAE);
                if(afState != null && CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState && aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_LOCKED && mLockAFAE == LOCK_AF_AE_STATE_START){
                    if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE && mCurrentSceneMode.mode == CameraMode.VIDEO){
                        applyIsAfLock(true);
                    }
                    mLockAFAE = LOCK_AF_AE_STATE_LOCK_DONE;
                }
                break;
            }
            case STATE_WAITING_TOUCH_FOCUS: {
                Log.d(TAG, "STATE_WAITING_TOUCH_FOCUS id: " + id + " afState:" + afState + " aeState:" + aeState);
                try {
                    if (mIsAutoFocusStarted) {
                        if (mIsCanceled && mSetAePrecaptureTriggerIdel == 1) {
                            Log.i(TAG, "STATE_WAITING_TOUCH_FOCUS SET CONTROL_AE_PRECAPTURE_TRIGGER_IDLE");
                            mPreviewRequestBuilder[id].set(
                                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
                            mCaptureSession[id].setRepeatingRequest(
                                    mPreviewRequestBuilder[id].build(), mCaptureCallback,
                                    mCameraHandler);
                            mSetAePrecaptureTriggerIdel = 0;
                        }
                        if (mPreviewRequestBuilder[id] != null && mLastAeState != -1
                                && (mLastAeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                                && (aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
                                || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED))
                                && mIsAutoFlash
                                && !mIsCanceled) {
                            Log.i(TAG, "SET CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL START");
                            mPreviewRequestBuilder[id].set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
                            mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id].build(),
                                    mCaptureCallback, mCameraHandler);
                            mSetAePrecaptureTriggerIdel++;
                            mIsCanceled = true;
                            Log.i(TAG, "SET CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL END");

                        }
                    }
                    if(afState!= null && aeState != null && CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState && aeState == CaptureResult.CONTROL_AE_STATE_LOCKED && mLockAFAE == LOCK_AF_AE_STATE_START){
                        mState[id] = STATE_AF_AE_LOCKED;
                    }
                } catch (CameraAccessException | IllegalStateException e) {
                    Log.e(TAG,e);
                }
                break;
            }
            case STATE_WAITING_AF_LOCKING: {
                parallelLockFocusExposure(getMainCameraId());
                break;
            }
            case STATE_WAITING_AE_PRECAPTURE: {
                Log.d(TAG, "STATE_WAITING_AE_PRECAPTURE id: " + id + " aeState:" + aeState);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED ||
                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    parallelLockFocusExposure(getMainCameraId());
                }
                break;
            }
            case STATE_WAITING_AF_AE_LOCK: {
                Log.d(TAG, "STATE_WAITING_AF_AE_LOCK id: " + id + " afState: " + afState +
                        " aeState:" + aeState);
                if ((aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED)) {
                    if (isFlashOn(id)) {
                        // if flash is on and AE state is CONVERGED then lock AE
                        lockExposure(id);
                    }
                }
                if ((CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) &&
                        (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_LOCKED)) {
                    checkAfAeStatesAndCapture(id);
                } else if (mSettingsManager.isFixedFocus(id)) {
                    // CONTROL_AE_STATE can be null on some devices
                    if(aeState == null || (aeState == CaptureResult
                            .CONTROL_AE_STATE_CONVERGED) && isFlashOff(id)) {
                        lockExposure(id);
                    } else {
                        runPrecaptureSequence(id);
                    }
                }
                break;
            }
            case STATE_WAITING_AF_AE_RELEASE: {
                Log.d(TAG, "STATE_WAITING_AF_AE_RELEASE id: " + id + " afState: " + afState +
                        " aeState:" + aeState);
                if(afState != null && CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED != afState && aeState != null && aeState != CaptureResult.CONTROL_AE_STATE_LOCKED){
                    if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE && mCurrentSceneMode.mode == CameraMode.VIDEO){
                        applyIsAfLock(true);
                    }
                    mInTAF = true;
                    mUI.onFocusStarted();
                    mUI.setFocusPosition(mClickPosition[0], mClickPosition[1]);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.showEvSeekbar(mClickPosition[0], mClickPosition[1]);
                        }
                    });
                    lockExposure(mCurrentSceneMode.getCurrentId());
                    triggerFocusAtPoint(mClickPosition[0], mClickPosition[1], mCurrentSceneMode.getCurrentId());
                }
                break;
            }
        }
        if (aeState == null) {
            return;
        }
        if (aeState == null) {
            mLastAeState = -1;
        } else {
            mLastAeState = aeState;
        }
    }

    private void checkAfAeStatesAndCapture(int id) {
        if(mPaused || !mCamerasOpened) {
            return;
        }
        if(isBackCamera() && getCameraMode() == DUAL_MODE) {
            mState[id] = STATE_AF_AE_LOCKED;
            try {
                // stop repeating request once we have AF/AE lock
                // for mono when mono preview is off.
                if(id == MONO_ID && !canStartMonoPreview()) {
                    mCaptureSession[id].stopRepeating();
                }
            } catch (CameraAccessException e) {
                Log.w(TAG,e.toString());
            }

            if(mState[BAYER_ID] == STATE_AF_AE_LOCKED &&
                    mState[MONO_ID] == STATE_AF_AE_LOCKED) {
                mState[BAYER_ID] = STATE_PICTURE_TAKEN;
                mState[MONO_ID] = STATE_PICTURE_TAKEN;
                captureStillPicture(BAYER_ID);
                captureStillPicture(MONO_ID);
            }
        } else {
            mState[id] = STATE_PICTURE_TAKEN;
            captureStillPicture(id);
            captureStillPictureForHDRTest(id);
        }
    }

    private void captureStillPictureForHDRTest(int id) {
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (SettingsManager.getInstance().isCamera2HDRSupport()
                && scene != null && scene.equals("18")){
            mCaptureHDRTestEnable = true;
            captureStillPicture(id);
        }
        mCaptureHDRTestEnable = false;
    }

    private boolean canStartMonoPreview() {
        return getCameraMode() == MONO_MODE ||
                (getCameraMode() == DUAL_MODE && isMonoPreviewOn());
    }

    private boolean isMonoPreviewOn() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_MONO_PREVIEW);
        if (value == null) return false;
        if (value.equals("on")) return true;
        else return false;
    }

    public boolean isBackCamera() {
        String value = mSettingsManager.mPreferences.getGlobal().getString(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE, "rear");
        return value.equals("rear");
    }

    public boolean isBLEConnected() {
        String audioSelected = mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER);
        if (PersistUtil.needAudioEncoder() && !audioSelected.equals("off")) {
            AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] allDeviceInputInfo = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
            for (AudioDeviceInfo deviceInfo : allDeviceInputInfo) {
                Log.i(TAG, "BLE, AudioDevice type " + deviceInfo.getType());
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                    int[] channelCounts = deviceInfo.getChannelCounts();
                    mBleInputDevice = deviceInfo;
                    Log.i(TAG, "BLE, found ble device, channel count " +
                            Arrays.toString(channelCounts));
                    return true;
                }
            }
        }
        return false;
    }

    public int getCameraMode() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (value != null && value.equals(SettingsManager.SCENE_MODE_DUAL_STRING)) return DUAL_MODE;
        value = mSettingsManager.getValue(SettingsManager.KEY_MONO_ONLY);
        if (value == null || !value.equals("on")) return BAYER_MODE;
        return MONO_MODE;
    }

    private boolean isClearSightOn() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_CLEARSIGHT);
        if (value == null) return false;
        return isBackCamera() && getCameraMode() == DUAL_MODE && value.equals("on");
    }
    private boolean isFacePointOn() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_FACE_DETECTION_MODE);
        if (value == null) return false;
        return  value.equals(String.valueOf(CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL));
    }

    private void updateImageFormatKey() {
        mSaveRaw = false;
        mYUV10bit = false;
        mYUV10BitWithMetadata = false;
        String value = mSettingsManager.getValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
        if (value == null) return;
        if (value.equals("10") || value.equals("16")) {
            mSaveRaw = true;
        } else if (value.equals("54")) {
            mYUV10bit = true;
        } else if (value.equals("99")) {
            mYUV10BitWithMetadata = true;
        }
        if (mCurrentSceneMode.mode != CameraMode.DEFAULT || mSettingsManager.isMultiCameraEnabled()) {
            mSaveRaw = false;
        }
        Log.v(TAG, " updateImageFormatKey mSaveRaw :" + mSaveRaw + ", mYUV10bit :" + mYUV10bit
                + ", mYUV10BitWithMetadata :" + mYUV10BitWithMetadata);
    }

    public boolean isDeepPortraitMode() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (value == null) return false;
        return Integer.valueOf(value) == SettingsManager.SCENE_MODE_DEEPPORTRAIT_INT;
    }

    private boolean isMpoOn() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_MPO);
        if (value == null) return false;
        return isBackCamera() && getCameraMode() == DUAL_MODE && value.equals("on");
    }

    public static int getQualityNumber(String jpegQuality) {
        if (jpegQuality == null) {
            return 85;
        }
        try {
            int qualityPercentile = Integer.parseInt(jpegQuality);
            if (qualityPercentile >= 0 && qualityPercentile <= 100)
                return qualityPercentile;
            else
                return 85;
        } catch (NumberFormatException nfe) {
            //chosen quality is not a number, continue
        }
        int value = 0;
        switch (jpegQuality) {
            case "superfine":
                value = CameraProfile.QUALITY_HIGH;
                break;
            case "fine":
                value = CameraProfile.QUALITY_MEDIUM;
                break;
            case "normal":
                value = CameraProfile.QUALITY_LOW;
                break;
            default:
                return 85;
        }
        return CameraProfile.getJpegEncodingQualityParameter(value);
    }

    public CamGLRenderer getCamGLRender() {
        return  mRenderer;
    }

    public GLCameraPreview getGLCameraPreview() {
        return  mUI.getGLCameraPreview();
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    private void initHEIFWriter() {
        //inti heifWriter and get input surface
        if (mSettingsManager.isHeifWriterEncoding()) {
            String tmpPath = mActivity.getCacheDir().getPath() + "/" + "heif.tmp";
            if (mInitHeifWriter != null) {
                mInitHeifWriter.close();
            }
            mInitHeifWriter = createHEIFEncoder(tmpPath, mPictureSize.getWidth(),
                    mPictureSize.getHeight(), 0,1, 85);
        }
    }

    private void initializeFirstTime() {
        if (mFirstTimeInitialized || mPaused) {
            return;
        }

        //Todo: test record location. Jack to provide instructions
        // Initialize location service.
        boolean recordLocation = getRecordLocation();
        mLocationManager.recordLocation(recordLocation);

        mUI.initializeFirstTime();
        MediaSaveService s = mActivity.getMediaSaveService();
        // We set the listener only when both service and shutterbutton
        // are initialized.
        if (s != null) {
            s.setListener(this);
            if (isClearSightOn()) {
                ClearSightImageProcessor.getInstance().setMediaSaveService(s);
            }
        }

        mNamedImages = new NamedImages();
        mGraphViewR = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_r);
        mGraphViewGB = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_gb);
        mGraphViewB = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_b);
        mGraphViewRGB = (Camera2RGBGraphView) mRootView.findViewById(R.id.graph_view_rgb);
        bgstats_view = (Camera2BGBitMap) mRootView.findViewById(R.id.bg_stats_graph);
        bestats_view = (Camera2BEBitMap) mRootView.findViewById(R.id.be_stats_graph);
        rsstats_view = (Camera2RSBitMap) mRootView.findViewById(R.id.rs_stats_graph);
        mBgStatsLabel = (TextView) mRootView.findViewById(R.id.bg_stats_graph_label);
        mBeStatsLabel = (TextView) mRootView.findViewById(R.id.be_stats_graph_label);
        mRsStatsLabel = (TextView) mRootView.findViewById(R.id.rs_stats_graph_label);
        mDrawAutoHDR2 = (DrawAutoHDR2 )mRootView.findViewById(R.id.autohdr_view);
        mMFNRDrawer = (MFNRDrawer) mRootView.findViewById(R.id.mfnr_view);
        mMFNRSwitch = (TextView) mRootView.findViewById(R.id.mfnr_switch);
        mMFNRText = (TextView) mRootView.findViewById(R.id.mfnr_text);
        mMfnrSeekBar = (SeekBar) mRootView.findViewById(R.id.mfnr_seekbar);
        mLockAFAEText = (TextView) mRootView.findViewById(R.id.lock_af_ae_label);
        mGapGraphView = (Camera2RequestGapGraphView) mRootView.findViewById(R.id.graph_view_gap);
        mBokehText = (TextView) mRootView.findViewById(R.id.bokeh_text);
        if (mGapGraphView != null){
            mGapGraphView.setCaptureModuleObject(this);
        }
        mGraphViewR.setDataSection(0,256);
        mGraphViewGB.setDataSection(256,512);
        mGraphViewB.setDataSection(512,768);
        mGraphViewRGB.setDataSection(0,768);
        if (mGraphViewRGB != null){
            mGraphViewRGB.setCaptureModuleObject(this);
        }
        if (mGraphViewR != null){
            mGraphViewR.setCaptureModuleObject(this);
        }
        if (mGraphViewGB != null){
            mGraphViewGB.setCaptureModuleObject(this);
        }
        if (mGraphViewB != null){
            mGraphViewB.setCaptureModuleObject(this);
        }
        if (bgstats_view != null){
            bgstats_view.setCaptureModuleObject(this);
        }
        if (bestats_view != null){
            bestats_view.setCaptureModuleObject(this);
        }
        if (rsstats_view != null){
            rsstats_view.setCaptureModuleObject(this);
        }
        if (mDrawAutoHDR2 != null) {
            mDrawAutoHDR2.setCaptureModuleObject(this);
        }
        if (mMFNRDrawer != null) {
            mMFNRDrawer.setCaptureModuleObject(this);
        }
        if (mBokehText != null) {
            mBokehText.setText("Bokeh Off");
            mBokehText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBokehText.getText().equals("Bokeh On")) {
                        mBokehText.setText("Bokeh Off");
                        applyBokehMode(false);
                    } else {
                        mBokehText.setText("Bokeh On");
                        applyBokehMode(true);
                    }
                }
            });
        }
        if(mMFNRSwitch != null){
            if(isMFNREnabled()) mMFNRSwitch.setText("ON");
            else mMFNRSwitch.setText("OFF");
            mMFNRSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isMFNREnabled()){
                        mSettingsManager.setValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE,"0");
                        mMFNRSwitch.setText("OFF");
                        mMfnrSeekBar.setVisibility(View.INVISIBLE);
                        mMFNRText.setVisibility(View.INVISIBLE);
                    }else{
                        mSettingsManager.setValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE,"1");
                        mMFNRSwitch.setText("ON");
                        mMfnrSeekBar.setVisibility(View.VISIBLE);
                        mMFNRText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        mFirstTimeInitialized = true;
    }

    private void initializeSecondTime() {
        // Start location update if needed.
        boolean recordLocation = getRecordLocation();
        mLocationManager.recordLocation(recordLocation);
        MediaSaveService s = mActivity.getMediaSaveService();
        if (s != null) {
            s.setListener(this);
            if (isClearSightOn()) {
                ClearSightImageProcessor.getInstance().setMediaSaveService(s);
            }
        }
        mNamedImages = new NamedImages();
    }

    public ArrayList<ImageFilter> getFrameFilters() {
        if(mFrameProcessor == null) {
            return new ArrayList<ImageFilter>();
        } else {
            return mFrameProcessor.getFrameFilters();
        }
    }

    private void applyFocusDistance(CaptureRequest.Builder builder, String value) {
        if (value == null) return;
        float valueF = Float.valueOf(value);
        if (valueF < 0) return;
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, valueF);
        if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
            mLockAFAE = LOCK_AF_AE_STATE_NONE;
            applySettingsForUnlockExposure(builder, mCurrentSceneMode.getCurrentId());
            updateLockAFAEVisibility();
        }
    }

    private void createSessions() {
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSessions1");
        if (mPaused || !mCamerasOpened || mTempHoldVideoInVideoIntent) return;
        final int cameraId = getMainCameraId();
        Log.i(TAG,"Current SceneMode is " + mCurrentSceneMode.mode + ", current cameraId is " + cameraId);
        switch (mCurrentSceneMode.mode) {
            case VIDEO:
                createSessionForVideo(cameraId);
                break;
            case HFR:
                if (!HFR_RATE.equals("")) {
                    mSettingsManager.setValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE, HFR_RATE);
                }
                createSessionForVideo(cameraId);
                break;
            case CINEMATIC:
                createSessionForVideo(cameraId);
                break;
            default:
                createSession(cameraId);
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private CaptureRequest.Builder getRequestBuilder(int id) throws CameraAccessException {
        int templateType = CameraDevice.TEMPLATE_PREVIEW;
        if(mPostProcessor.isZSLEnabled() && id == getMainCameraId()) {
            templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG;
        } else if ((mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) && mIsRecordingVideo) {
            templateType = CameraDevice.TEMPLATE_RECORD;
        } else {
            templateType = CameraDevice.TEMPLATE_PREVIEW;
        }
        CaptureRequest.Builder builder;
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- createCaptureRequest");

        if (mSettingsManager.getPhysicalCameraId()!= null){
            Set<String> physical_ids = mSettingsManager.getPhysicalCameraId();
            builder = mCameraDevice[id].createCaptureRequest(templateType,physical_ids);
        } else {
            builder = mCameraDevice[id].createCaptureRequest(templateType);
        }
        if (TRACE_DEBUG) Trace.endSection();
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- applySessionParameters");
        if (builder != null){
            applySessionParameters(builder);
        }
        if (TRACE_DEBUG) Trace.endSection();
        return builder;
    }

    private CaptureRequest.Builder getRequestBuilder(int templateType,int id, Set<String> physicalIds)
            throws CameraAccessException{
        CaptureRequest.Builder builder = null;
        if (physicalIds != null){
            builder = mCameraDevice[id].createCaptureRequest(
                    templateType,physicalIds);
        } else {
            builder = mCameraDevice[id].createCaptureRequest(
                    templateType);
        }
        if (builder != null){
            applySessionParameters(builder);
        }
        return builder;
    }

    private void waitForPreviewSurfaceReady() {
        if (mPaused) return;
        try {
            if (!mSurfaceReady) {
                if (!mSurfaceReadyLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                    if (mPaused) {
                        Log.d(TAG, "mPaused status occur Time out waiting for surface.");
                        mSurfaceReadyLock.release();
                        throw new IllegalStateException("Paused Time out waiting for surface.");
                    } else {
                        Log.d(TAG, "Time out waiting for surface.");
                        mSurfaceReadyLock.release();
                        throw new RuntimeException("Time out waiting for surface.");
                    }
                }
                mSurfaceReadyLock.release();
            }
        } catch (InterruptedException e) {
            Log.e(TAG,e);
        }
    }

    private void
    updatePreviewSurfaceReadyState(boolean rdy) {
        if (rdy != mSurfaceReady) {
            if (rdy) {
                Log.i(TAG, "Preview Surface is ready!");
                mSurfaceReadyLock.release();
                mSurfaceReady = true;
            } else {
                try {
                    Log.i(TAG, "Preview Surface is not ready!");
                    mSurfaceReady = false;
                    mSurfaceReadyLock.acquire();
                } catch (InterruptedException e) {
                    Log.e(TAG,e);
                }
            }
        } else if (!rdy) {
            if (mSurfaceReadyLock.tryAcquire()) {
                Log.i(TAG, "Preview Surface is not ready and tryAcquire Lock");
            }
        }
    }

    public class DetachClickListener implements DialogInterface.OnClickListener {

        private DialogInterface.OnClickListener mDelegate;

        public DetachClickListener(DialogInterface.OnClickListener delegate) {
            this.mDelegate = delegate;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mDelegate != null) {
                mDelegate.onClick(dialog, which);
            }
        }

        public void clearOnDetach(AlertDialog dialog) {
            dialog.getWindow()
                    .getDecorView()
                    .getViewTreeObserver()
                    .addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                        @Override
                        public void onWindowAttached() {
                        }

                        @Override
                        public void onWindowDetached() {
                            mDelegate = null;
                        }
                    });
        }
    }
    public boolean isHwMfnrEnabled(){
        String value = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
        if(value != null &&  !value.equals("disable")&& Integer.parseInt(value) == 1 && mSettingsManager.isHWMFNRSupport()){
            return true;
        }
        return false;
    }

    public boolean isAIDE2Enabled(){
        String value = mSettingsManager.getValue(SettingsManager.KEY_AI_DENOISER);
        if(value != null &&  !value.equals("disable")&& Integer.parseInt(value) == 1 && isHwMfnrEnabled() && AideUtil.isAide2Supported()){
            return true;
        }
        return false;
    }

    private void setTag(@NonNull CaptureRequest.Builder builder, @NonNull Object tag) {
        Log.d(TAG, "setTag " + tag);
        builder.setTag(tag);
    }

    private int getIdFromTag(Object tag) {
        String tag_ = String.valueOf(tag);
        int id  = 0;
        try {
            id = Integer.parseInt(tag_.substring(0, tag_.indexOf("-")));
        } catch (Exception e) {

        }
        return id;
    }

    private int mDepthMode = 0;

    private void applyDepthMode(CaptureRequest.Builder builder) {
        if (CameraMode.DEPTH != mCurrentSceneMode.mode) {
            return;
        }
        if (mDepthMode == -1) {
            mDepthMode = mSettingsManager.getDepthMode();
        }
        VendorTagUtil.setDepthMode(builder, (int) mDepthMode);
    }

    private void applyITofTuningSet(CaptureRequest.Builder builder) {
        if (CameraMode.DEPTH != mCurrentSceneMode.mode) {
            return;
        }
        String tuning_set = mSettingsManager.getValue(SettingsManager.KEY_ITOF_TUNING_SET);
        int set = Integer.parseInt(tuning_set);
        VendorTagUtil.setITofTuningSet(builder, set);
    }

    public void onDepthEngineChanged(int mode) {
        mDepthMode = mode;
        restartSession(false);
    }

    public void onDepthFocusChanged(int focus) {
        if (mDepthRender != null) {
            mDepthRender.setDepthFocus(focus);
        }
    }

    private void createSession(final int id) {
        Log.d(TAG, "createSession,id: " + id + ",mPaused:" + mPaused + ",mCameraOpened:"
                + mCameraOpened[id] + ",mCameraDevice:"+ mCameraDevice[id] + ", mChosenImageFormat :" + mChosenImageFormat);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession");
        if (mPaused || !mCameraOpened[id] || (mCameraDevice[id] == null)) return;
        List<Surface> list = new LinkedList<Surface>();
        mState[id] = STATE_PREVIEW;
        mControlAFMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        mUI.enableShutter(false);
        setCameraModeSwitcherAllowed(false);
        mPreviewOutputConfiguration = null;
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder[id] = getRequestBuilder(id);
            setTag(mPreviewRequestBuilder[id], "" + id + "-" + getCurrenCameraMode().name());

            CameraCaptureSession.StateCallback captureSessionCallback =
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (mPaused || null == mCameraDevice[id] ||
                                    cameraCaptureSession == null) {
                                return;
                            }
                            mCreateSessionLatency = System.currentTimeMillis() - mCreateSessionLatency;
                            Log.i(TAG, "capturesession - onConfigured "+ id);
                            if(mActivity.getPerformenceTest()) {
                                mHasMapTimes.put("createSession->onConfigured",mCreateSessionLatency);
                            }
                            mCurrentSessionClosed = false;

                            if(mPreviewOutputConfiguration != null) {
                                Surface previewSur = getPreviewSurfaceForSession(id);
                                waitForPreviewSurfaceReady();
                                if (mSurfaceReady && previewSur.isValid()) {
                                    mPreviewOutputConfiguration.addSurface(previewSur);
                                    try {
                                        List<OutputConfiguration> finalizeOutputConfigs = new ArrayList<>();
                                        finalizeOutputConfigs.add(mPreviewOutputConfiguration);
                                        cameraCaptureSession.finalizeOutputConfigurations(finalizeOutputConfigs);
                                    } catch (Exception e) {
                                        Log.e(TAG, "finalizeOutputConfigurations with exception:" + e.toString());
                                    }
                                }
                            }
                            setCameraModeSwitcherAllowed(true);
                            // When the session is ready, we start displaying the preview.
                            mCaptureSession[id] = cameraCaptureSession;
                            if(id == getMainCameraId()) {
                                mCurrentSession = cameraCaptureSession;
                            }
                            initializePreviewConfiguration(id);
                            //APP could  check if  afState is anything other than INACTIVE , it should change the focus circle and skip the passive transient state.
                            if (mLastResultAFState != CaptureResult.CONTROL_AF_STATE_INACTIVE && mFocusStateListener != null) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFocusStateListener.onFocusStatusUpdate(CaptureResult.CONTROL_AF_STATE_INACTIVE);
                                    }
                                });
                            }
                            setDisplayOrientation();
                            updateFaceDetection();
                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mUI.updateGridLine();
                                }
                            });
                            if(!mSettingsManager.isLogicalEnable() && mSettingsManager.getSinglePhysicalCamera() == null){
                                mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        mUI.hideLogicalSurface();
                                    }
                                });
                            }
                            mFirstPreviewLoaded = false;
                            try {

                                if (isBackCamera() && getCameraMode() == DUAL_MODE) {
                                    linkBayerMono(id);
                                    mIsLinked = true;
                                }
                                // Finally, we start displaying the camera preview.
                                // for cases where we are in dual mode with mono preview off,
                                // don't set repeating request for mono
                                mFirstRequestLatency = System.currentTimeMillis();
                                if(id == MONO_ID && !canStartMonoPreview()
                                        && getCameraMode() == DUAL_MODE) {
                                    mCaptureSession[id].capture(mPreviewRequestBuilder[id]
                                            .build(), mCaptureCallback, mCameraHandler);
                                } else {
                                    mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                                            .build(), mCaptureCallback, mCameraHandler);
                                }
                                if (mIntentMode == INTENT_MODE_STILL_IMAGE_CAMERA &&
                                        mIsVoiceTakePhote) {
                                    mHandler.sendEmptyMessageDelayed(VOICE_INTERACTION_CAPTURE, 500);
                                }
                                if (isClearSightOn()) {
                                    ClearSightImageProcessor.getInstance().onCaptureSessionConfigured(id == BAYER_ID, cameraCaptureSession);
                                } else if (mChosenImageFormat == ImageFormat.PRIVATE && id == getMainCameraId()) {
                                    mPostProcessor.onSessionConfigured(mCameraDevice[id], mCaptureSession[id]);
                                } else if (mRawReprocessType != 0 || mMultiResReprocessEnabled) {
                                    mPostProcessor.onSessionConfigured(mCameraDevice[id], mCaptureSession[id]);
                                }

                            } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                              Log.e(TAG,"createSession exception= "+ e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "cameracapturesession - onConfigureFailed "+ id);
                            setCameraModeSwitcherAllowed(true);
                            if (mActivity.isFinishing()) {
                                return;
                            }
                            Toast.makeText(mActivity, "Camera Initialization Failed",
                                    Toast.LENGTH_SHORT).show();
                            mCurrentSessionClosed = false;
                        }

                        @Override
                        public void onClosed(CameraCaptureSession session) {
                            Log.i(TAG, "cameracapturesession - onClosed");
                            setCameraModeSwitcherAllowed(true);
                        }
                    };

            Surface surface = null;
            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createsession--getsurface");
            if(needWaitSurface()) {
                try {
                    waitForPreviewSurfaceReady();
                } catch (RuntimeException e) {
                    Log.v(TAG,
                            "createSession: normal status occur Time out waiting for surface ");
                }
            }
            if(mPaused) return;
            if(!CaptureUI.USE_TEXTURE_VIEW_TO_PREVIEW && mUI.getSurfaceHolder() == null){
                mUI.setSurfaceHolder();
            }
            surface = getPreviewSurfaceForSession(id);

            if(id == getMainCameraId()) {
                mFrameProcessor.setOutputSurface(surface);
                mFrameProcessor.setVideoOutputSurface(null);
            }
            if (TRACE_DEBUG) Trace.endSection();
            if(isClearSightOn()) {
                if (surface != null) {
                    mPreviewRequestBuilder[id].addTarget(surface);
                    list.add(surface);
                }
                ClearSightImageProcessor.getInstance().createCaptureSession(
                        id == BAYER_ID, mCameraDevice[id], list, captureSessionCallback);
            } else if (id == getMainCameraId()) {
                if(mFrameProcessor.isFrameFilterEnabled() && !mDeepPortraitMode) {
                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            SurfaceHolder surfaceHolder = mUI.getSurfaceHolder();
                            if (surfaceHolder != null) {
                                surfaceHolder.setFixedSize(
                                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
                            }
                        }
                    });
                }

                List<OutputConfiguration> outputConfigurations = new ArrayList<OutputConfiguration>();
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createsession--getalllist");
                if (mSettingsManager.getPhysicalCameraId() != null) {
                    mUI.buildPhysicalSurfaces();
                    List<OutputConfiguration> physicalOutput = getPhysicalOutputConfiguration();
                    outputConfigurations.addAll(physicalOutput);
                    List<Surface> previewSurfaces = mUI.getPhysicalSurfaces();
                    if(previewSurfaces != null){
                        if (mSettingsManager.isLogicalEnable()) {
                            List<Surface> surfaceList = new ArrayList<>();
                            surfaceList.add(previewSurfaces.get(0));
                            mPreviewRequestBuilder[id].addTarget(previewSurfaces.get(0));
                            if (mSettingsManager.isLogicalFeatureEnable(
                                    SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK)){
                                surfaceList.add(mImageReader[id].getSurface());
                            }
                            for (Surface s : surfaceList) {
                                outputConfigurations.add(new OutputConfiguration(s));
                            }
                            Log.d(TAG,"add logical surface list size="+surfaceList.size());
                        }
                        int i=1;
                        for (String physical : mSettingsManager.getPhysicalCameraId()){
                            Log.d(TAG,"add surface physical id="+physical);
                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mUI.hideSurfaceView();
                                }
                            });
                            OutputConfiguration outputConfiguration =
                                    new OutputConfiguration(previewSurfaces.get(i));
                            outputConfiguration.setPhysicalCameraId(physical);
                            outputConfigurations.add(outputConfiguration);
                            mPreviewRequestBuilder[id].addTarget(previewSurfaces.get(i));
                            i++;
                        }
                    }
                } else {
                    if ((mSettingsManager.isMultiCameraEnabled() &&
                            mSettingsManager.isLogicalEnable()) || (mSaveRaw && mRawReprocessType == 0 && !isPhysicalRaw())) {
                        List<OutputConfiguration> physicalOutput =
                                getPhysicalOutputConfiguration();
                        if (physicalOutput.size() != 0) {
                            outputConfigurations.addAll(physicalOutput);
                        }
                    }
                    List<Surface> surfaces = mFrameProcessor.getInputSurfaces();
                    for (Surface surs : surfaces) {
                        mPreviewRequestBuilder[id].addTarget(surs);
                        list.add(surs);
                    }
                    mPreviewRequestBuilder[id].addTarget(surface);
                    if (DEPTH_CAM_ID != null &&
                            mCurrentSceneMode.mode == CameraMode.DEPTH &&
                            mSettingsManager.isBackCamera(id)) {
                        mDepthLastFrameTimeStamp = 0L;
                        mHandler.post(() -> {
                            mUI.updateDepthFps(0f);
                        });
                        if (mDepthRender == null) {
                            mDepthRender = new DepthRender();
                            mDepthRender.setColorLut(mActivity);
                        }
                        mDepthRender.init();
                        Log.i(TAG, "depth image reader " + mDepthSize);
                        mDepthImageReader = ImageReader.newInstance(mDepthSize.getWidth(),
                                mDepthSize.getHeight(), ImageFormat.DEPTH16, MAX_IMAGEREADERS);
                        mDepthImageReader.setOnImageAvailableListener(reader -> {
                            synchronized (mDepthImageLock) {
                                Image image = reader.acquireNextImage();
                                if (image == null) {
                                    return;
                                }
                                if (mDepthLastFrameTimeStamp == 0L) {
                                    mDepthLastFrameTimeStamp = System.currentTimeMillis();
                                    mDepthFrameCount = 1;
                                } else {
                                    if (++mDepthFrameCount == 50) {
                                        long now = System.currentTimeMillis();
                                        long diff = now - mDepthLastFrameTimeStamp;
                                        final float fps = 1000f / (diff / 50f);
                                        mHandler.post(() -> {
                                            mUI.updateDepthFps(fps);
                                        });
                                        mDepthLastFrameTimeStamp = now;
                                        mDepthFrameCount = 0;
                                    }
                                }
                                int width = image.getWidth();
                                int height = image.getHeight();
                                int rowStride = image.getPlanes()[0].getRowStride();
                                int pixStride = image.getPlanes()[0].getPixelStride();
                                ByteBuffer imageBuffer = image.getPlanes()[0].getBuffer();
                                mDepthRender.setDepthBuffer(imageBuffer, width, height, rowStride, pixStride);
                                image.close();
                            }
                        }, mImageAvailableHandler);

                        Surface depthSurface = mDepthImageReader.getSurface();
                        list.add(depthSurface);
                        mPreviewRequestBuilder[id].addTarget(depthSurface);

                        mHandler.post(() -> {
                            mUI.showDepthView(mDepthRender);
                        });
                    }

                    if (!mSettingsManager.isHeifWriterEncoding() && mRawReprocessType != 1) {
                        if (!isMultiResolutionImageReaderEnabled() &&
                                (mCurrentSceneMode.mode != CameraMode.DEPTH)) {
                            if(!isAIDE2Enabled()) {
                                Log.i(TAG, "add blob configure stream except aide case");
                                list.add(mImageReader[id].getSurface());
                            }
                        }
                    }
                    if ((mSettingsManager.isMultiCameraEnabled() &&
                            !mSettingsManager.isLogicalFeatureEnable(
                                    SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK) )) {
                        list.remove(mImageReader[id].getSurface());
                    }
                    if (mSaveRaw && mRawReprocessType == 0 && isPhysicalRaw()) {
                        list.add(mRawImageReader[id].getSurface());
                    }
                    if (mYUV10bit || mYUV10BitWithMetadata) {
                        list.add(mYUV10bitImageReader[id].getSurface());
                    }

                    for (Surface s : list) {
                        if (s == surface) {
                            if(CaptureUI.USE_TEXTURE_VIEW_TO_PREVIEW || s.isValid()) {
                                String physical_id = mSettingsManager.getSinglePhysicalCamera();
                                OutputConfiguration out = new OutputConfiguration(s);
                                if (physical_id != null) {
                                    boolean enableLogical =
                                            SettingsManager.LOGICAL_AND_PHYSICAL.equals(physical_id);
                                    if (enableLogical){
                                        mUI.buildPhysicalSurfaces();
                                    } else {
                                        out.setPhysicalCameraId(physical_id);
                                    }
                                    outputConfigurations.add(out);
                                    List<Surface> physicalSurfaces = mUI.getPhysicalSurfaces();
                                    Set<String> allPhysicalIds =
                                            mSettingsManager.getAllPhysicalCameraId();
                                    if (enableLogical){
                                        int i = 1;
                                        for (String physical : allPhysicalIds) {
                                            if (!physical_id.equals(physical)) {
                                                OutputConfiguration o = new OutputConfiguration(
                                                        physicalSurfaces.get(i));
                                                o.setPhysicalCameraId(physical);
                                                outputConfigurations.add(o);
                                                mPreviewRequestBuilder[id].addTarget(physicalSurfaces.get(i));
                                                i++;
                                            }
                                        }
                                    }
                                } else {
                                    if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                                        out.addSensorPixelModeUsed(
                                                CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
                                        Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_DEFAULT");
                                    }
                                    String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
                                    Log.v(TAG, "OutputConfiguration set previewProfile :" + previewProfile);
                                    if (previewProfile != null && !previewProfile.equals("0")) {
                                        out.setDynamicRangeProfile(Long.parseLong(previewProfile));
                                    }
                                    outputConfigurations.add(out);
                                }
                            } else {
                                mPreviewOutputConfiguration = new OutputConfiguration(
                                        new android.util.Size(mPreviewSize.getWidth(), mPreviewSize.getHeight()),
                                        SurfaceHolder.class);
                                mPreviewOutputConfiguration.enableSurfaceSharing();
                                if (isTouchFocusAssistSupported()) {
                                    mFAOutputConfiguration = mPreviewOutputConfiguration;
                                    mFASurfaceConfigured = false;
                                }
                                Log.v(TAG, "add mPreviewOutputConfiguration");
                                String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
                                Log.v(TAG, "OutputConfiguration previewProfile :" + previewProfile);
                                if (previewProfile != null && !previewProfile.equals("0")) {
                                    mPreviewOutputConfiguration.setDynamicRangeProfile(Long.parseLong(previewProfile));
                                }
                                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                                    mPreviewOutputConfiguration.addSensorPixelModeUsed(
                                            CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
                                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_DEFAULT for preview");
                                }
                                outputConfigurations.add(mPreviewOutputConfiguration);
                            }
                        } else {
                            OutputConfiguration outputConfiguration = new OutputConfiguration(s);
                            if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                                outputConfiguration.addSensorPixelModeUsed(
                                        CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                                Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                            }
                            if ((mYUV10bit || mYUV10BitWithMetadata) &&
                                    s == mYUV10bitImageReader[id].getSurface()) {
                                String captureProfile = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_PROFILE);
                                Log.v(TAG, "OutputConfiguration set captureProfile :" + captureProfile);
                                if (captureProfile != null && !captureProfile.equals("0")) {
                                    outputConfiguration.setDynamicRangeProfile(Long.parseLong(captureProfile));
                                }
                            }
                            Log.i(TAG," mImageReader[id] " + mImageReader[id] + "mRawImageReader[id] :" + mRawImageReader[id]);
                            if(s == mImageReader[id].getSurface() || (mRawImageReader[id] != null &&
                                    s == mRawImageReader[id].getSurface())) {
                                String physicalCameraId = mSettingsManager.getQuadBayerPhysicalId(Integer.toString(getMainCameraId()));
                                if(physicalCameraId != null){
                                    Log.i(TAG," set physical id " + physicalCameraId + "for image reader stream");
                                    outputConfiguration.setPhysicalCameraId(physicalCameraId);
                                }
                                if(mRawImageReader[id] != null && s == mRawImageReader[id].getSurface()){
                                    applyCroppedRaw(outputConfiguration, getMainCameraId());
                                }
                                if(s == mImageReader[id].getSurface() && (mSettingsManager.getSavePictureFormat() == mSettingsManager.JPEG_R_FORMAT
                                        || mSettingsManager.getSavePictureFormat() == mSettingsManager.HEIC_TENBIT_FORMAT)) {
                                    Log.v(TAG, "OutputConfiguration set captureProfile :" + 2);
                                    outputConfiguration.setDynamicRangeProfile(2);
                                }
                            }
                            outputConfigurations.add(outputConfiguration);
                        }
                    }
                    Set<String> physical_ids = mSettingsManager.getAllPhysicalCameraId();
                    if(isAIDE2Enabled()) {
                        if(physical_ids != null && physical_ids.size() != 0){
                            for (String physicalId : physical_ids){
                                Log.i(TAG,"configure for physical streams, physicalId:" + physicalId);
                                OutputConfiguration configuration = new OutputConfiguration(mAideFullImageReader[getIndexByPhysicalId(physicalId)].getSurface());
                                configuration.setPhysicalCameraId(physicalId);
                                outputConfigurations.add(configuration);
                                OutputConfiguration ds4configuration = new OutputConfiguration(mAideDs4ImageReader[getIndexByPhysicalId(physicalId)].getSurface());
                                ds4configuration.setPhysicalCameraId(physicalId);
                                outputConfigurations.add(ds4configuration);
                            }
                        } else {
                            OutputConfiguration configuration = new OutputConfiguration(mAideFullImageReader[getMainCameraId()].getSurface());
                            outputConfigurations.add(configuration);
                            OutputConfiguration ds4configuration = new OutputConfiguration(mAideDs4ImageReader[getMainCameraId()].getSurface());
                            outputConfigurations.add(ds4configuration);
                        }
                    }
                    if (mSettingsManager.isHeifWriterEncoding()) {
                        if (mInitHeifWriter != null) {
                            mHeifOutput = new OutputConfiguration(mInitHeifWriter.getInputSurface());
                            mHeifOutput.enableSurfaceSharing();
                            outputConfigurations.add(mHeifOutput);
                        }
                    }

                    if (mRawReprocessType != 0) {
                        for (int i = 0; i < mRawCount; i++) {
                            OutputConfiguration configuration = new OutputConfiguration(mRAWImageReader[i].getSurface());
                            if(mSettingsManager.getRawReprocessPhysicalId() != null && !mSettingsManager.getRawReprocessPhysicalId().equals("logical")) {
                                configuration.setPhysicalCameraId(mSettingsManager.getRawReprocessPhysicalId());
                            }
                            outputConfigurations.add(configuration);
                        }
                        for (int i = 0; i < mYUVCount; i++) {
                            OutputConfiguration configuration = new OutputConfiguration(mYUVImageReader[i].getSurface());
                            if(mSettingsManager.getRawReprocessPhysicalId() != null && !mSettingsManager.getRawReprocessPhysicalId().equals("logical")) {
                                configuration.setPhysicalCameraId(mSettingsManager.getRawReprocessPhysicalId());
                            }
                            outputConfigurations.add(configuration);
                        }
                    }

                    if (isTouchFocusAssistSupported() &&
                            (CaptureUI.USE_TEXTURE_VIEW_TO_PREVIEW || surface != null)) {
                        for (OutputConfiguration configuration : outputConfigurations) {
                            if (surface.equals(configuration.getSurface())) {
                                Log.d(TAG, "enable preview surface output configuration sharing");
                                configuration.enableSurfaceSharing();
                                mFAOutputConfiguration = configuration;
                                mFASurfaceConfigured = false;
                            }
                        }

                    }
                }
                if (TRACE_DEBUG) Trace.endSection();
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createsession--createsession");
                if(mChosenImageFormat == ImageFormat.YUV_420_888 || mChosenImageFormat == ImageFormat.PRIVATE) {
                    if (mPostProcessor.isZSLEnabled()) {
                        if (isMultiResolutionImageReaderEnabled()) {
                            Log.d(TAG, "Add input multiresImageReader surface");
                            mPreviewRequestBuilder[id].addTarget(mMultiResImageReader.getSurface());
                            Collection<OutputConfiguration> outputConfigs =
                                    OutputConfiguration.createInstancesForMultiResolutionOutput(
                                            mPostProcessor.getZSLReprocessMultiImageReader());
                            outputConfigurations.addAll(outputConfigs);
                            Collection<OutputConfiguration> inputConfigs =
                                    OutputConfiguration.createInstancesForMultiResolutionOutput(
                                            mMultiResImageReader);
                            outputConfigurations.addAll(inputConfigs);
                            createCameraSessionWithSessionConfiguration(id, outputConfigurations, mInputConfig,
                                    captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                        } else {
                            mPreviewRequestBuilder[id].addTarget(mImageReader[id].getSurface());
                            if (mSaveRaw) {
                                mPreviewRequestBuilder[id].addTarget(mRawImageReader[id].getSurface());
                            }
                            if (mYUV10bit || mYUV10BitWithMetadata) {
                                mPreviewRequestBuilder[id].addTarget(mYUV10bitImageReader[id].getSurface());
                            }
                            InputConfiguration inputConfig = new InputConfiguration(mImageReader[id].getWidth(),
                                    mImageReader[id].getHeight(), mImageReader[id].getImageFormat());
                            outputConfigurations.add(new OutputConfiguration(
                                    mPostProcessor.getZSLReprocessImageReader().getSurface()));
                            createCameraSessionWithSessionConfiguration(id, outputConfigurations, inputConfig,
                                    captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                        }
                    } else if (outputConfigurations != null){
                        createCameraSessionWithSessionConfiguration(id, outputConfigurations, null,
                                captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                    }
                } else {
                    if (outputConfigurations != null) {
                        Log.i(TAG,"list size:" + list.size() + ",mRawReprocessType:" + mRawReprocessType);
                        if(mRawReprocessType != 0) {
                            InputConfiguration inputConfig = new InputConfiguration(mRAWImageReader[0].getWidth(),
                                    mRAWImageReader[0].getHeight(), mRAWImageReader[0].getImageFormat());
                            createCameraSessionWithSessionConfiguration(id, outputConfigurations, inputConfig,
                                    captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                        }else {
                            if (isMultiResolutionImageReaderEnabled()) {
                                if(!mMultiResReprocessEnabled) {
                                    Collection<OutputConfiguration> outConfigs = OutputConfiguration.
                                            createInstancesForMultiResolutionOutput(mMultiResImageReader);
                                    outputConfigurations.addAll(outConfigs);
                                    createCameraSessionWithSessionConfiguration(id, outputConfigurations, null,
                                            captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                                }else{
                                    Log.d(TAG, "Add input multiresImageReader surface for reprocess case");
                                    Collection<OutputConfiguration> outputConfigs =
                                            OutputConfiguration.createInstancesForMultiResolutionOutput(
                                                    mPostProcessor.getZSLReprocessMultiImageReader());
                                    outputConfigurations.addAll(outputConfigs);
                                    Collection<OutputConfiguration> inputConfigs =
                                            OutputConfiguration.createInstancesForMultiResolutionOutput(
                                                    mMultiResImageReader);
                                    outputConfigurations.addAll(inputConfigs);
                                    createCameraSessionWithSessionConfiguration(id, outputConfigurations, mInputConfig,
                                            captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                                }
                            }else{
                                createCameraSessionWithSessionConfiguration(id, outputConfigurations, null,
                                        captureSessionCallback, mCameraHandler, mPreviewRequestBuilder[id]);
                            }
                        }
                    } else {
                        mCameraDevice[id].createCaptureSession(list, captureSessionCallback, mCameraHandler);
                    }
                }
                if (TRACE_DEBUG) Trace.endSection();
            } else {
                if (surface != null) {
                    mPreviewRequestBuilder[id].addTarget(surface);
                    list.add(surface);
                }
                list.add(mImageReader[id].getSurface());
                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice[id].createCaptureSession(list, captureSessionCallback, mCameraHandler);
            }
        } catch (CameraAccessException | NullPointerException | IllegalStateException |IllegalArgumentException e) {
           Log.e(TAG,"createSession exception = "+ e);
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private int addPhysicalCaptureTarget(CaptureRequest.Builder builder) {
        int targetCount = 0;
        Set<String> jpeg_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
        if (jpeg_ids != null) {
            Iterator<String> id=jpeg_ids.iterator();
            for (ImageReader reader : mPhysicalJpegReader) {
                if (reader != null){
                    builder.addTarget(reader.getSurface());
                    targetCount++;
                    Log.d(TAG,"add jpeg target id="+id.next()+" size="
                            +reader.getWidth()+"x"+reader.getHeight());
                }
            }
        }
        Set<String> jpegR_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
        if (jpegR_ids != null) {
            Iterator<String> id=jpegR_ids.iterator();
            for (ImageReader reader : mPhysicalJpegRReader) {
                if (reader != null){
                    builder.addTarget(reader.getSurface());
                    targetCount++;
                    Log.d(TAG,"add jpeg R target id="+id.next()+" size="
                            +reader.getWidth()+"x"+reader.getHeight());
                }
            }
        }
        Set<String> yuv_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
        if(yuv_ids != null){
            Iterator<String> id=yuv_ids.iterator();
            for (ImageReader reader : mPhysicalYuvReader) {
                if (reader != null){
                    builder.addTarget(reader.getSurface());
                    targetCount++;
                    Log.d(TAG,"add yuv target id="+id.next()+" size="
                            +reader.getWidth()+"x"+reader.getHeight());
                }
            }
        }
        Set<String> yuv10bit_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
        if(yuv10bit_ids != null){
            Iterator<String> id=yuv10bit_ids.iterator();
            for (ImageReader reader : mPhysicalYuv10bitReader) {
                if (reader != null){
                    builder.addTarget(reader.getSurface());
                    targetCount++;
                    Log.d(TAG,"add yuv 10bit target id="+id.next()+" size="
                            +reader.getWidth()+"x"+reader.getHeight());
                }
            }
        }
        Set<String> raw_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
        if(raw_ids != null){
            Iterator<String> id=raw_ids.iterator();
            for (ImageReader reader : mPhysicalRawReader) {
                if (reader != null) {
                    builder.addTarget(reader.getSurface());
                    targetCount++;
                    Log.d(TAG,"add raw target id="+id.next()+" size="
                            +reader.getWidth()+"x"+reader.getHeight());
                }
            }
        }else if(mSaveRaw){
            int physicalId = mActiveCameraIds.get(0);
            Log.d(TAG," mActiveCameraIds="+physicalId);
                    for( int i = 0;i < mPhysicalRawId.length;i++){
                        if(Integer.parseInt(mPhysicalRawId[i]) == physicalId ){
                            builder.addTarget(mPhysicalRawReader[i].getSurface());
                            mPhysicalRawSize = new Size(mPhysicalRawReader[i].getWidth(),mPhysicalRawReader[i].getHeight());
                            targetCount++;
                            if(mSettingsManager.JPEG_FORMAT == mSettingsManager.getSavePictureFormat()) {
                                builder.addTarget(mPhysicalJpegReader[i].getSurface());
                                mPhysicalJPGSize = new Size(mPhysicalJpegReader[i].getWidth(),mPhysicalJpegReader[i].getHeight());
                                targetCount++;
                            }
                            break;
                        }
                    }
        }
        return targetCount;
    }

    private boolean isLogicalId(String id){
        return id != null && id.contains("logical");
    }

    private List<OutputConfiguration> getPhysicalVideoOutputConfiguration() {
        List<OutputConfiguration> outputConfigurations = new ArrayList<>();
        Set<String> physical_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_CAMCORDER);
        if (physical_ids != null){
            int i = 0;
            for (String id : physical_ids){
                if (mPhysicalMediaRecorders[i] != null){
                    OutputConfiguration configuration = new OutputConfiguration(
                            mPhysicalMediaSurfaces[i]);
                    configuration.setPhysicalCameraId(id);
                    outputConfigurations.add(configuration);
                    Log.d(TAG, " add output for physical recording physicalId=" + id);
                }
                if (mPhysicalSnapshotImageReaders[i] != null){
                    OutputConfiguration configuration = new OutputConfiguration(
                            mPhysicalSnapshotImageReaders[i].getSurface());
                    configuration.setPhysicalCameraId(id);
                    outputConfigurations.add(configuration);
                    Log.d(TAG, " add output for physical live shot format=jpeg physicalId=" + id);
                }
                i++;
            }
        }

        return outputConfigurations;
    }

    private void setStreamUseCase(int cameraId,long caseId, OutputConfiguration configuration) {
        try {
            Log.d(TAG, " setStreamUseCase cameraid="+cameraId+",caseId="+caseId);
            if (mSettingsManager.isStreamUseCaseEnabled() && mSettingsManager.isAvailableUseCase(cameraId, caseId)) {
                Log.d(TAG, " setStreamUseCase ");
                configuration.setStreamUseCase(caseId);
            }
        } catch (IllegalArgumentException | NoSuchFieldError e) {
            Log.w(TAG, EXCEPTION_LOG,"exception  e= "+e);
        }
    }
    private List<OutputConfiguration> getPhysicalOutputConfiguration(){
        if (!mSettingsManager.isMultiCameraEnabled() && !mSaveRaw)
            return null;
        List<OutputConfiguration> outputConfigurations = new ArrayList<>();

        Set<String> jpeg_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
        Log.d(TAG,"getPhysicalOutputConfiguration jpeg_ids="+jpeg_ids);
        if (jpeg_ids != null){
            int i = 0;
            for (String id : jpeg_ids){
                OutputConfiguration configuration = new OutputConfiguration(
                        mPhysicalJpegReader[i].getSurface());
                configuration.setPhysicalCameraId(id);
                setStreamUseCase(Integer.parseInt(id),SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV,configuration);
                outputConfigurations.add(configuration);
                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    configuration.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION for physical jpeg");
                }

                Log.d(TAG,"add output format=jpeg physicalId="+id+" size="
                        +mPhysicalJpegReader[i].getWidth()+"x"+mPhysicalJpegReader[i].getHeight());
                i++;
            }
        }

        Set<String> jpegR_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
        Log.d(TAG,"getPhysicalOutputConfiguration jpegR_ids="+jpegR_ids);
        if (jpegR_ids != null){
            int i = 0;
            for (String id : jpegR_ids){
                OutputConfiguration configuration = new OutputConfiguration(
                        mPhysicalJpegRReader[i].getSurface());
                if (!isLogicalId(id)){
                    configuration.setPhysicalCameraId(id);
                    setStreamUseCase(Integer.parseInt(id),SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV,configuration);
                }
                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    configuration.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION for physical jpegR");
                }
                configuration.setDynamicRangeProfile(2);
                outputConfigurations.add(configuration);
                Log.d(TAG,"add output format=jpeg R physicalId="+id+" size="
                        +mPhysicalJpegRReader[i].getWidth()+"x"+mPhysicalJpegRReader[i].getHeight());
                i++;
            }
        }

        Set<String> yuv_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
        if (yuv_ids != null){
            int i =0;
            for (String id:yuv_ids){
                OutputConfiguration configuration = new OutputConfiguration(
                        mPhysicalYuvReader[i].getSurface());
                if (!isLogicalId(id)){
                    configuration.setPhysicalCameraId(id);
                    setStreamUseCase(Integer.parseInt(id),SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV,configuration);
                }
                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    configuration.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION for physical YUV");
                }
                outputConfigurations.add(configuration);
                Log.d(TAG,"add output format=yuv physicalId="+id+" size="
                        +mPhysicalYuvReader[i].getWidth()+"x"+mPhysicalYuvReader[i].getHeight());
                i++;
            }
        }

        Set<String> yuv10bit_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
        if (yuv10bit_ids != null){
            int i =0;
            for (String id:yuv10bit_ids){
                OutputConfiguration configuration = new OutputConfiguration(
                        mPhysicalYuv10bitReader[i].getSurface());
                if (!isLogicalId(id)){
                    configuration.setPhysicalCameraId(id);
                    setStreamUseCase(Integer.parseInt(id),SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV,configuration);
                }
                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    configuration.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION for physical YUV10");
                }
                outputConfigurations.add(configuration);
                Log.d(TAG,"add output format=yuv 10bit physicalId="+id+" size="
                        +mPhysicalYuv10bitReader[i].getWidth()+"x"+mPhysicalYuv10bitReader[i].getHeight());
                i++;
            }
        }

        Set<String> raw_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
        if (raw_ids != null){
            int i =0;
            for (String id:raw_ids){
                OutputConfiguration configuration = new OutputConfiguration(
                        mPhysicalRawReader[i].getSurface());
                if (!isLogicalId(id)){
                    configuration.setPhysicalCameraId(id);
                    setStreamUseCase(Integer.parseInt(id),SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV,configuration);
                    applyCroppedRaw(configuration, Integer.parseInt(id));
                }else{
                    applyCroppedRaw(configuration, getMainCameraId());
                }
                if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    configuration.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION for physical RAW");
                }
                outputConfigurations.add(configuration);
                i++;
            }
        }else if(mSaveRaw) {
            for (int i = 0; i < mPhysicalRawId.length; i++) {
                if (mPhysicalRawReader[i] == null) {
                    break;
                }
                String id = mPhysicalRawId[i];
                OutputConfiguration configuration = new OutputConfiguration(mPhysicalRawReader[i].getSurface());
                if (!isLogicalId(id)) {
                    configuration.setPhysicalCameraId(id);
                    setStreamUseCase(Integer.parseInt(id), SCALER_AVAILABLE_STREAM_USE_CASES_FULL_FOV, configuration);
                    applyCroppedRaw(configuration, Integer.parseInt(id));
                }else {
                    applyCroppedRaw(configuration, getMainCameraId());
                }

                outputConfigurations.add(configuration);
                Log.d(TAG, "add raw output physicalId=" + id + " size="
                        + mPhysicalRawReader[i].getWidth() + "x" + mPhysicalRawReader[i].getHeight() + ",mPhysicalRawReader[i].getSurface()=" + mPhysicalRawReader[i].getSurface());
                if (mSettingsManager.JPEG_FORMAT == mSettingsManager.getSavePictureFormat()) {
                    OutputConfiguration configuration_jpeg = new OutputConfiguration(
                            mPhysicalJpegReader[i].getSurface());
                    configuration_jpeg.setPhysicalCameraId(id);

                    outputConfigurations.add(configuration_jpeg);
                    Log.d(TAG, "add jpeg output format=jpeg physicalId=" + id + " size="
                            + mPhysicalJpegReader[i].getWidth() + "x" + mPhysicalJpegReader[i].getHeight() + ",mPhysicalJpegReader[i].getSurface()=" + mPhysicalJpegReader[i].getSurface());
                }
            }
        }
        return outputConfigurations;
    }

    private void createSessionForVideo(final int cameraId) {
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSessionForVideo");
        try {
            setCameraModeSwitcherAllowed(false);
            mStopRecPending = false;
            mVideoRecordRequestBuilder = null;
            setVideoState(VideoState.VIDEO_INIT);
            setupRecordingCommonSettings(cameraId);
            Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER);
            if (ids != null && ids.size() != 0){
                for (int i=0;i<ids.size();i++) {
                    mPhysicalMediaSurfaces[i] = MediaCodec.createPersistentInputSurface();
                }
            }
            setUpPhysicalMediaRecorder();
            if (PersistUtil.enableMediaRecorder()) {
                mVideoRecordingSurface = MediaCodec.createPersistentInputSurface();
                if(!is8KInMulti && !setupMediaRecorder(cameraId)){
                    return;
                }
            } else {
                mOnlyVideoEncoder = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle myExtras = mActivity.getIntent().getExtras();
                        mOutputFileInit = false;
                        setVideoOutputFile(myExtras);
                        setOrientationHint(cameraId);
                        mOutputFileInit = true;
                    }
                }).start();
                if (!mCaptureTimeLapse && (!mHighSpeedCapture || mHighSpeedRecordingMode)
                        && !mSuperSlomoCapture) {
                    mOnlyVideoEncoder = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mAudioCodecInit = false;
                            try {
                                setupMediaCodecAudio();
                            }catch (Exception e){
                                Log.w(TAG,"get exif failed");
                            }
                            mAudioCodecInit = true;
                        }
                    }).start();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mAudioRecorderInit = false;
                            setupAudioRecorder();
                            mAudioRecorderInit = true;
                        }
                    }).start();
                }
                setupMediaCodecVideo(cameraId);
            }
            mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[cameraId]);
            mState[cameraId] = STATE_PREVIEW;
            if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                mControlAFMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUI.resetTrackingFocus();
                }
            });
            try {
                waitForPreviewSurfaceReady();
            } catch (RuntimeException e) {
                Log.v(TAG,
                        "createSession: normal status occur Time out waiting for surface ");
            }
            if (mPaused) {
                if (PersistUtil.enableMediaRecorder()) {
                    releaseMediaRecorder();
                } else {
                    stopCodecThreads();
                    releaseMediaCodec();
                }
                releaseAudioFocus();
                return;
            }
            Surface surface = getPreviewSurfaceForSession(cameraId);
            mFrameProcessor.onOpen(getFrameProcFilterId(), mVideoSize);
            if (getFrameProcFilterId().size() == 1 && getFrameProcFilterId().get(0) ==
                    FrameProcessor.FILTER_MAKEUP) {
                setUpVideoPreviewRequestBuilder(mFrameProcessor.getInputSurfaces().get(0),cameraId);
            } else {
                setUpVideoPreviewRequestBuilder(surface, cameraId);
            }
            if(mFrameProcessor.isFrameFilterEnabled()) {
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        SurfaceHolder surfaceHolder = mUI.getSurfaceHolder();
                        if (surfaceHolder != null) {
                            surfaceHolder.setFixedSize(
                                    mVideoSize.getHeight(), mVideoSize.getWidth());
                        }
                    }
                });
            }
            mVideoPreviewSurface = surface;
            mFrameProcessor.setOutputSurface(surface);
            createVideoSnapshotImageReader();
            createPhysicalVideoSnapshotImageReader();
            if (!PersistUtil.enableMediaRecorder()) {
                mVideoRecordingSurface = mVideoEncoder.createInputSurface();

            }
            mFrameProcessor.setVideoOutputSurface(mVideoRecordingSurface);
            setUpVideoCaptureRequestBuilder(cameraId);
            if (mVideoPreviewSurface != null) {
                mVideoRecordRequestBuilder.addTarget(mVideoPreviewSurface);
            }
            mPreviewRequestBuilder[cameraId] = mVideoRecordRequestBuilder;
            if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                mPreviewRequestBuilder[cameraId].set(CaptureRequest.SENSOR_PIXEL_MODE,
                        CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                Log.v(TAG, " video preview OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
            }
            mIsPreviewingVideo = true;
            if (isHighSpeedRateCapture()) {
                createHighSpeedSession(cameraId);
            } else {
                createRegularSession(cameraId);
            }
        } catch (CameraAccessException | IOException | IllegalArgumentException |
                NullPointerException | IllegalStateException e) {
            Log.e(TAG,e.toString());
            if (mIsCloseCamera && mCameraDevice[cameraId] == null) {
                Log.w(TAG, "activity may be onPause, no need to pop up error msg.");
            } else {
                mCaptureSession[cameraId] = null;
                quitVideoToPhotoWithError(e.getMessage());
            }
        }
        mCurrentSessionClosed = false;
        if (TRACE_DEBUG) Trace.endSection();
    }

    private int getSensorTableHFRRange() {
        int optimalSizeIndex = -1;
        int[] table = mSettingsManager.getSensorModeTable(getMainCameraId());
        if (table == null) {
            Log.w(TAG, "Sensor table hfr array got is null");
            return optimalSizeIndex;
        }
        String videoSizeString = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        if (videoSizeString == null) {
            Log.w(TAG, "KEY_VIDEO_QUALITY is null");
            return optimalSizeIndex;
        }
        Size videoSize = parsePictureSize(videoSizeString);
        String rateValue = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (rateValue == null || rateValue.substring(0, 3).equals("off")) {
            Log.w(TAG, "KEY_VIDEO_HIGH_FRAME_RATE is null");
            return optimalSizeIndex;
        }
        int frameRate = Integer.parseInt(rateValue.substring(3));
        for (int i = 2; i < table.length; i += table[1]) {
            if (table[i] == videoSize.getWidth()
                    && table[i + 1] == videoSize.getHeight()
                    && table[i + 2] == frameRate) {
                if (i != table.length) {
                    return (i - 2) / table[1] + 1;
                }
            }
        }

        // if does not query the index from (widthxheight, fps),
        // app will find the  closest to set the index according to fps
        int minDiff = Integer.MAX_VALUE;
        Point point = new Point(videoSize.getWidth(), videoSize.getHeight());
        int targetHeight = Math.min(point.x, point.y);
        // Try to find an size match aspect ratio and size
        for (int i = 2; i < table.length; i += table[1]) {
            if (table[i + 2] == frameRate) {
                Point size = new Point(table[i], table[i+1]);
                int miniSize = Math.min(size.x, size.y);
                int heightDiff = Math.abs(miniSize - targetHeight);
                if (heightDiff < minDiff) {
                    if (i != table.length) {
                        optimalSizeIndex = (i - 2) / table[1] + 1;
                    }
                    minDiff = Math.abs(miniSize - targetHeight);
                }
            }
        }

        return optimalSizeIndex;
    }

    public void setAFModeToPreview(int id, int afMode) {
        if (!checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id]) || !mCameraModeSwitcherAllowed) {
            Log.i(TAG,"return , mCaptureSession[id]="+mCaptureSession[id]+",mPreviewRequestBuilder[id]"+
                    mPreviewRequestBuilder[id]+",mCameraModeSwitcherAllowed="+mCameraModeSwitcherAllowed);
            return;
        }
        mPreviewRequestBuilder[id].set(CaptureRequest.CONTROL_AF_MODE, afMode);
        applyAFRegions(mPreviewRequestBuilder[id], id);
        applyAERegions(mPreviewRequestBuilder[id], id);
        setTag(mPreviewRequestBuilder[id], "" + id + "-" + getCurrenCameraMode().name());
        Log.d(TAG, "setAFModeToPreview ,preview:" + mPreviewRequestBuilder[id].toString());
        try {
            if (isSSMEnabled() && (mIsPreviewingVideo || mIsRecordingVideo)) {
                if (!checkSessionAndBuilder(mCaptureSession[id], mVideoRecordRequestBuilder)) {
                    return;
                }
                mCaptureSession[id].setRepeatingBurst(createSSMBatchRequest(mVideoRecordRequestBuilder),
                        mCaptureCallback, mCameraHandler);
            } else if (mCaptureSession[id] instanceof CameraConstrainedHighSpeedCaptureSession) {
                CameraConstrainedHighSpeedCaptureSession session =
                        (CameraConstrainedHighSpeedCaptureSession) mCaptureSession[id];
                if (!checkSessionAndBuilder(session, mVideoRecordRequestBuilder)) {
                    return;
                }
                List requestList =getHighSpeedList(session,mVideoRecordRequestBuilder);
                session.setRepeatingBurst(requestList, mCaptureCallback, mCameraHandler);
            } else {
                if (!PersistUtil.enableMediaRecorder() && mIsRecordingVideo) {
                    //Add video buffer for media codec recording to avoid only preview buffer
                    //will cause hang in EIS. 
                    mPreviewRequestBuilder[id].addTarget(mVideoRecordingSurface);
                    mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
                    mPreviewRequestBuilder[id].removeTarget(mVideoRecordingSurface);
                } else {
                    mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
                }
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.w(TAG,e);
        }
    }

    public void setFocusDistanceToPreview(int id, float fd) {
        if (!checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            return;
        }
        mPreviewRequestBuilder[id].set(CaptureRequest.LENS_FOCUS_DISTANCE, fd);
        setTag(mPreviewRequestBuilder[id], "" + id + "-" + getCurrenCameraMode().name());
        try {
            if (id == MONO_ID && !canStartMonoPreview()) {
                mCaptureSession[id].capture(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            } else {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e.toString());
        }
    }

    private void reinitSceneMode() {
        mCurrentSceneMode = mSceneCameraIds.get(mNextModeIndex);
        mCurrentModeIndex = mNextModeIndex;
        CURRENT_MODE = mCurrentSceneMode.mode;
        CURRENT_ID = mCurrentSceneMode.getNextCameraId(CURRENT_MODE);
        Log.i(TAG, "reinitSceneMode: CURRENT_ID :" + CURRENT_ID);
    }

    public void reinit() {
        CURRENT_ID = mCurrentSceneMode.getNextCameraId(CURRENT_MODE);
        CURRENT_MODE = mCurrentSceneMode.mode;
        Log.i(TAG,"reinit: CURRENT_ID camera id " + CURRENT_ID);
        mSettingsManager.init();
    }

    private boolean frontIsAllowed() {
        return mCurrentSceneMode.mode == CameraMode.DEFAULT ||
                mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR;
    }

    public boolean isRefocus() {
        return mIsRefocus;
    }

    public boolean getRecordLocation() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_RECORD_LOCATION);
        if (value == null) value = RecordLocationPreference.VALUE_NONE;
        return RecordLocationPreference.VALUE_ON.equals(value);
    }

    @Override
    public void init(CameraActivity activity, View parent) {
        mActivity = activity;
        mRootView = parent;
        mSettingsManager = SettingsManager.getInstance();
        mSettingsManager.createCaptureModule(this);
        mSettingsManager.registerListener(this);
        mFirstPreviewLoaded = false;
        Log.d(TAG, "init");
        for (int i = 0; i < MAX_NUM_CAM; i++) {
            mCameraOpened[i] = false;
            mTakingPicture[i] = false;
        }
        for (int i = 0; i < MAX_NUM_CAM; i++) {
            mState[i] = STATE_PREVIEW;
        }
        SceneModule module;
        for (int i = 0; i < mSelectableModes.length; i++) {
            module = new SceneModule();
            module.mode = CameraMode.values()[i];
            mSceneCameraIds.add(module);
        }
        initModeByIntent();
        initCameraIds();
        CURRENT_ID = mCurrentSceneMode.getNextCameraId(CURRENT_MODE);
        CURRENT_MODE = mCurrentSceneMode.mode;
        mSettingsManager.init();
        mPostProcessor = new PostProcessor(mActivity, this);
        if(mPostProcessor.isJniAPISupported()) {
            mPostProcessor.nativeEnablePerfLock();
            mPostProcessor.nativeC2paSetUp();
        }
        mFrameProcessor = new FrameProcessor(mActivity, this);
        mContentResolver = mActivity.getContentResolver();
        mLocationManager = new LocationManager(mActivity, this);
        mCameraRender = new CameraRender();
    }

    @Override
    public void onCreateAfterSuper() {
        mUI = mActivity.getCaptureUI();
        mUI.initializeControlByIntent();
        mFocusStateListener = new FocusStateListener(mUI);
        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
        mActivity.registerReceiver(mBTConnectReceiver, btFilter);
    }

    public void restoreCameraIds(){
        CURRENT_ID = mCurrentSceneMode.getCurrentId();
    }
    public void resetZoom(){
        if (mCurrentSceneMode.mode == CameraMode.RTB || (isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
            float[] zoomRatioRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                    getMainCameraId());
            if (zoomRatioRange != null && zoomRatioRange[0] == zoomRatioRange[1]) {
                mZoomValue = zoomRatioRange[0];
            }
        }else{
            mZoomValue = 1.0f;
        }
    }

    private void initCameraIds() {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        boolean isFirstDefault = true;
        boolean[] removeList = new boolean[mSelectableModes.length];
        for (int i = 0; i < mSelectableModes.length; i++) {
            removeList[i] = true;
        }
        String[] cameraIdList = null;
        try {
            cameraIdList = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        }
        if (cameraIdList == null || cameraIdList.length == 0) {
            return;
        }
        boolean foundDepth = false;
        for (int i = 0; i < cameraIdList.length; i++) {
            boolean isLogicalCamera = false;
            String cameraId = cameraIdList[i];
            CameraCharacteristics characteristics;
            try {
                characteristics = manager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                Log.e(TAG,e.toString());
                continue;
            }
             List<CaptureRequest.Key<?>> availableSessionKeys = characteristics.getAvailableSessionKeys();
            for (CaptureRequest.Key<?> key : availableSessionKeys) {
                if (key != null) {
                    Log.d(TAG,  BIG_LOG,"availableSessionKeys: " + key + " in camera " + cameraId);
                }
            }
            int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

            for (int capability : capabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                    Log.i(TAG, "Found depth camera with id " + cameraId);
                    //foundDepth = true;
                }
                if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA == capability) {
                    Log.d(TAG, "Found logical multi camera with id " + cameraId);
                    isLogicalCamera = true;
                    try {
                        Byte type = characteristics.get(logical_camera_type);
                        if (type == TYPE_DEFAULT) {
                            MCXMODE = true;
                            Log.d(TAG, "set MCXMode true since logical_camera_type is " + type);
                        }
                    } catch (IllegalArgumentException e) {
                        MCXMODE = true;
                        Log.d(TAG, EXCEPTION_LOG,"set MCXMode true since no vendorTag logical_camera_type for " + cameraId);
                    }
                }
            }
            if (!foundDepth) {
                Size[] sizes = mSettingsManager.getSupportedOutputSize(Integer.valueOf(cameraId), ImageFormat.DEPTH16);
                if(sizes != null){
                    Log.i(TAG,"getdepthsize ="+sizes[0].getWidth()+"*"+sizes[0].getHeight());
                    mDepthSize = sizes[0];
                }
                if(mDepthSize == null) {
                    mDepthSize = mSettingsManager.getSupportedDepthSize(characteristics);
                }
                foundDepth = mDepthSize != null;
                Log.i(TAG,"mDepthSize="+mDepthSize+",foundDepth="+foundDepth);
            }

            initQuadBayerPhsicalCameraIds(isLogicalCamera, cameraId, manager, characteristics, capabilities);

            if(foundDepth) {
                DEPTH_CAM_ID = cameraId;
//                continue;
            }
            mCameraId[i] = cameraId;
            isFirstDefault = setUpLocalMode(i, characteristics, removeList,
                    isFirstDefault, cameraId);
        }
        if (mCurrentSceneMode == null) {
            int index = mIntentMode == INTENT_MODE_VIDEO ?
                    CameraMode.VIDEO.ordinal() : CameraMode.DEFAULT.ordinal();

            mCurrentModeIndex =  mNextModeIndex = index;
            mCurrentSceneMode = mSceneCameraIds.get(index);
        }
        for (int i = 0; i < removeList.length; i++) {
            if (!removeList[i]) {
                continue;
            }
            for (SceneModule sceneModule : mSceneCameraIds) {
                if (sceneModule.mode.ordinal() == i) {
                    mSceneCameraIds.remove(sceneModule);
                    break;
                }
            }
        }
    }

    private void initQuadBayerPhsicalCameraIds(boolean isLogicalCamera, String cameraId,
                                                      CameraManager manager,
                                                      CameraCharacteristics characteristics,
                                                      int[] capabilities ) {
        if (isLogicalCamera) {
            Set<String> physicalIds = characteristics.getPhysicalCameraIds();
            if (physicalIds != null) {
                for (String physicalId : physicalIds) {
                    CameraCharacteristics characters;
                    try {
                        characters = manager.getCameraCharacteristics(physicalId);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    int[] physicalCapabilities = characters.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    for (int capability : physicalCapabilities) {
                        if(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR == capability){
                            Log.d(TAG, "Found QuadBayerSensor for camera: " +  cameraId + ",physical id:" + physicalId);
                            mQuadBayerPhysicalIds.add(physicalId);
                            mQuadBayerPhysicalIds.add(cameraId);
                        }
                    }
                }
            }
        }
    }
    public void updateFlashIcon(){
        String qll = mSettingsManager.getValue(SettingsManager.KEY_QLL);
        if(isLongShotSettingEnabled() || mSettingsManager.isMultiCameraEnabled() || (qll != null && qll.equals("1"))){
            mUI.updateFlashButton(false);
        }else{
            mUI.updateFlashButton(true);
        }
    }
    private void updateSettingDependencyId(){
        List<String> supported = mSettingsManager.getSupportedVideoSize(mLogicalId);
        if(!MCXMODE || supported.size() <= 0){
            mSceneCameraIds.get(CameraMode.VIDEO.ordinal()).rearCameraId = mSingleRearId;
        }
        if (!mSettingsManager.isHFRSupported()) { // filter HFR mode
            for (SceneModule sceneModule : mSceneCameraIds) {
                if (sceneModule.mode.ordinal() == CameraMode.HFR.ordinal() ) {
                    mSceneCameraIds.remove(sceneModule);
                    break;
                }
            }
        }
    }

    private boolean setUpLocalMode(int camereIdIndex, CameraCharacteristics characteristics,
                                boolean[] removeList, boolean isFirstDefault, String cameraId) {
        Byte type = 0;
        try {
            type = characteristics.get(logical_camera_type);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"setUpLocalMode no vendorTag logical_camera_type:" + logical_camera_type);
        }
        Set<String> physical_ids = characteristics.getPhysicalCameraIds();
        int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        Log.d(TAG,"init cameraId " + camereIdIndex + " | logical_camera_type = " + type +
                " | physical id = " + cameraId + " | physical_ids : " + physical_ids
                + " | facing:" + facing);
        switch (type) {
            case TYPE_DEFAULT:// default
                removeList[CameraMode.DEFAULT.ordinal()] = false;
                removeList[CameraMode.VIDEO.ordinal()] = false;
                removeList[CameraMode.CINEMATIC.ordinal()] = false;
                removeList[CameraMode.PRO_MODE.ordinal()] = false;
                if (DEPTH_CAM_ID != null) {
                    removeList[CameraMode.DEPTH.ordinal()] = false;
                }

                if (physical_ids != null && physical_ids.size() == 0 &&
                        facing != CameraCharacteristics.LENS_FACING_FRONT){
                    if (mSingleRearId == -1) {
                        mSingleRearId = camereIdIndex;
                        Log.d(TAG, "mSingleRearId:" + camereIdIndex);
                    }
                } else if (physical_ids != null && physical_ids.size() != 0 && MCXMODE){
                    mLogicalId = camereIdIndex;
                    LOGICAL_ID = mLogicalId;
                    Log.d(TAG,"mLogicalId:" + camereIdIndex);
                    removeList[CameraMode.RTB.ordinal()] = false;
                    if (mSceneCameraIds.get(CameraMode.RTB.ordinal()).rearCameraId >= 0) {
                        break;
                    }
                    Log.d(TAG,"RTB rear Id:" + camereIdIndex);
                    mSceneCameraIds.get(CameraMode.RTB.ordinal()).rearCameraId = camereIdIndex;
                }
                if (physical_ids != null && physical_ids.size() == 0 &&
                        facing == CameraCharacteristics.LENS_FACING_FRONT && CaptureModule.FRONT_ID != -1) {
                    CaptureModule.FRONT_ID = camereIdIndex;
                    Log.d(TAG,"FRONT_ID:" + camereIdIndex);
                    mSceneCameraIds.get(CameraMode.DEFAULT.ordinal()).frontCameraId = camereIdIndex;
                    mSceneCameraIds.get(CameraMode.VIDEO.ordinal()).frontCameraId = camereIdIndex;
                    mSceneCameraIds.get(CameraMode.HFR.ordinal()).frontCameraId = camereIdIndex;
                    mSceneCameraIds.get(CameraMode.PRO_MODE.ordinal()).frontCameraId = camereIdIndex;
                } else {
                    if (!isFirstDefault && physical_ids != null && physical_ids.size() == 0) {
                        mSceneCameraIds.get(CameraMode.DEFAULT.ordinal()).auxCameraId = camereIdIndex;
                        isFirstDefault = true;
                    } else {
                        isFirstDefault = false;
                    }

                    int defaultId = mLogicalId;
                    if(!MCXMODE){
                        defaultId = mSingleRearId;
                    }
                    mSceneCameraIds.get(CameraMode.DEFAULT.ordinal()).rearCameraId = defaultId;
                    //update video camera after setting init done
                    mSceneCameraIds.get(CameraMode.VIDEO.ordinal()).rearCameraId = defaultId;
                    mSceneCameraIds.get(CameraMode.CINEMATIC.ordinal()).rearCameraId = mSingleRearId;
                    mSceneCameraIds.get(CameraMode.PRO_MODE.ordinal()).rearCameraId = defaultId;
                    if (DEPTH_CAM_ID != null) {
                        mSceneCameraIds.get(CameraMode.DEPTH.ordinal()).rearCameraId = defaultId;
                    }
                    //default HFR is support, will remove after setting manager init
                    if(!PersistUtil.lookaheadEnabled() || PersistUtil.enableMediaRecorder()) {
                        removeList[CameraMode.HFR.ordinal()] = false;
                    }
                    mSceneCameraIds.get(CameraMode.HFR.ordinal()).rearCameraId = mSingleRearId;
                    if (mCurrentSceneMode == null) {
                        int index = mIntentMode == INTENT_MODE_VIDEO ?
                                CameraMode.VIDEO.ordinal() : CameraMode.DEFAULT.ordinal();
                        mCurrentModeIndex = mNextModeIndex = index;
                        mCurrentSceneMode = mSceneCameraIds.get(index);
                    }
                }
                break;
            case TYPE_RTB:// RTB
                removeList[CameraMode.RTB.ordinal()] = false;
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mSceneCameraIds.get(CameraMode.RTB.ordinal()).frontCameraId = camereIdIndex;
                } else {
                    if (mSceneCameraIds.get(CameraMode.RTB.ordinal()).rearCameraId >= 0) {
                        break;
                    }
                    mSceneCameraIds.get(CameraMode.RTB.ordinal()).rearCameraId = camereIdIndex;
                    //add default front camera id, need to change if front RTB available
                    mSceneCameraIds.get(CameraMode.RTB.ordinal()).frontCameraId = FRONT_ID;
                }
                break;
            case TYPE_SAT:// SAT
                removeList[CameraMode.SAT.ordinal()] = false;
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mSceneCameraIds.get(CameraMode.SAT.ordinal()).frontCameraId = camereIdIndex;
                } else {
                    if (mSceneCameraIds.get(CameraMode.SAT.ordinal()).rearCameraId >= 0) {
                        break;
                    }
                    // if dual camera is enabled, video rear camera will be changed to SAT
                    mSceneCameraIds.get(CameraMode.VIDEO.ordinal()).rearCameraId = camereIdIndex;
                    mSceneCameraIds.get(CameraMode.SAT.ordinal()).rearCameraId = camereIdIndex;
                    //add default front camera id, need to change if front SAT available
                    mSceneCameraIds.get(CameraMode.SAT.ordinal()).frontCameraId = FRONT_ID;
                }
                break;
            case TYPE_VR360:// VR 360
                Log.w(TAG, "VR 360 is not supported from APP side now");
                break;
            default:// indicate error
                Log.w(TAG, "Type error: indicate error");
                break;
        }
        return isFirstDefault;
    }

    private void initModeByIntent() {
        String action = mActivity.getIntent().getAction();
        Log.i(TAG, " initModeByIntent: " + action);
        Bundle bundle = mActivity.getIntent().getExtras();
        if (bundle != null) {
            Log.v(TAG, " initModeByIntent bundle :" + bundle.toString());
        }
        if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)) {
            mIntentMode = INTENT_MODE_STILL_IMAGE_CAMERA;
            Set<String> categories = mActivity.getIntent().getCategories();
            if (categories != null) {
                for(String categorie: categories) {
                    Log.v(TAG, " initModeByIntent categorie :" + categorie);
                    if(categorie.equals("android.intent.category.VOICE")) {
                        mIsVoiceTakePhote = true;
                    }
                }
            }
            boolean isOpenOnly = mActivity.getIntent().getBooleanExtra(
                    "com.google.assistant.extra.CAMERA_OPEN_ONLY", false);
            if (isOpenOnly) {
                mIsVoiceTakePhote = false;
            }
            Log.v(TAG, " initModeByIntent isOpenOnly :" + isOpenOnly + ", mIsVoiceTakePhote :"
                    + mIsVoiceTakePhote);
        }
        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)) {
            mIntentMode = INTENT_MODE_CAPTURE;
        } else if (CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
            mIntentMode = INTENT_MODE_CAPTURE_SECURE;
        } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action)) {
            mIntentMode = INTENT_MODE_VIDEO;
        }
        mQuickCapture = mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        Bundle myExtras = mActivity.getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            mCropValue = myExtras.getString("crop");
        }
    }

    public boolean isQuickCapture() {
        return mQuickCapture;
    }

    public void setJpegImageData(byte[] data) {
        mJpegImageData = data;
    }

    public void showCapturedReview(final byte[] jpegData, final int orientation) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUI.showCapturedImageForReview(jpegData, orientation);
            }
        });
    }


    public int getCurrentIntentMode() {
        return mIntentMode;
    }

    public void cancelCapture() {
        mActivity.finish();
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        Log.i(TAG, "takePicture");
        if(!getCameraModeSwitcherAllowed() || !mUI.isShutterEnabled() || mCurrentSessionClosed || mPreviewCaptureResult == null){
            Log.d(TAG, "mode switch not finished or shutter button is not enabled or session is closed, can not take snapshot");
            return;
        }
        mUI.enableShutter(false);
        int cameraId = getMainCameraId();
        String flashMode = mSettingsManager.getValue(SettingsManager.KEY_FLASH_MODE);
        Integer aeState = CameraMetadata.CONTROL_AE_STATE_INACTIVE;
        if (mPreviewCaptureResult != null) {
            aeState = mPreviewCaptureResult.get(CaptureResult.CONTROL_AE_STATE);
        }
        isflashRequired = aeState ==  CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED ||
                (flashMode != null && flashMode.equalsIgnoreCase("on"));
        if(mSettingsManager.isTorchHDREnabled(isflashRequired,mPreviewCaptureResult)){
            mCaptureTorchTrigger = true;
            applyFlash(mPreviewRequestBuilder[cameraId], getMainCameraId());
            try{
                mCaptureSession[cameraId].setRepeatingRequest(
                mPreviewRequestBuilder[cameraId].build(), mCaptureCallback,mCameraHandler);
            } catch (CameraAccessException | IllegalStateException e) {
               Log.e(TAG,e.toString());
            }
        }

        if ((mSettingsManager.isZSLInHALEnabled() || isActionImageCapture()) &&
                !isFlashOn(cameraId) && (aeState != CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED &&
                mPreviewCaptureResult.getRequest().get(CaptureRequest.CONTROL_AE_LOCK) != Boolean.TRUE || mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE)) {
            // Flash mode is off ==> then send capture intent: 2
            if (isFlashOff(cameraId)) {
                takeZSLPictureInHAL();
            // if AE state is PRECAPTURE 5  ==> then send AEC lock true.
            } else if (aeState != null && aeState == CameraMetadata.CONTROL_AE_STATE_PRECAPTURE) {
                lockExposure(cameraId);
            // if AE state is INACTIVE 0 SEARCHING 1 CONVERGED 2 or LOCKED 3 ==> then send capture intent: 2
            } else {
                takeZSLPictureInHAL();
            }
        } else {
            if (takeZSLPicture(cameraId)) {
                return;
            }
            if (mUI.getCurrentProMode() == ProMode.MANUAL_MODE && !isFlashOn(getMainCameraId())) {
                captureStillPicture(cameraId);
            } else {
                if (mLongshotActive) {
                    parallelLockFocusExposure(cameraId);
                } else{
                    if (mPreviewCaptureResult != null) {
                        isFlashRequiredInDriver = aeState != null &&
                                aeState == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
                    }
                    lockFocus(cameraId);
                }
            }
        }
    }

    private boolean isActionImageCapture() {
        return mIntentMode == INTENT_MODE_CAPTURE;
    }

    private boolean takeZSLPicture(int cameraId) {
        if(mPostProcessor.isZSLEnabled() && mPostProcessor.takeZSLPicture()) {
            checkAndPlayShutterSound(getMainCameraId());
            mTakingPicture[cameraId] = false;
            mUI.enableShutter(true);
            mUI.enableZoomSeekBar(true);
            return true;
        }
        return false;
    }

    private void takeZSLPictureInHAL() {
        Log.i(TAG, "takeHALZSLPicture");
        captureStillPicture(getMainCameraId());
    }

    public boolean isLongShotActive() {
        return mLongshotActive;
    }

    private void parallelLockFocusExposure(int id) {
        if (mActivity == null || mCameraDevice[id] == null
                || !checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            enableShutterAndVideoOnUiThread(id);
            warningToast("Camera is not ready yet to take a picture.");
            return;
        }
        Log.i(TAG, "parallelLockFocusExposure " + id);

        mTakingPicture[id] = true;
        if (mState[id] == STATE_WAITING_TOUCH_FOCUS) {
            String value = mSettingsManager.getValue(mCurrentSceneMode.mode == CameraMode.PRO_MODE ?
                    SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
            if (value != null && value.equals("on")) {
                mState[id] = STATE_WAITING_AE_PRECAPTURE;
            } else {
                mState[id] = STATE_WAITING_AF_LOCKING;
            }
            mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[id]);
            mLockRequestHashCode[id] = 0;
            return;
        }

        try {
            // start repeating request to get AF/AE state updates
            // for mono when mono preview is off.
            if(id == MONO_ID && !canStartMonoPreview()) {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            }
        } catch (CameraAccessException | IllegalStateException  e) {
            Log.e(TAG,e);
        }
        try {
            mState[id] = STATE_WAITING_AF_AE_LOCK;
            CaptureRequest.Builder builder = getRequestBuilder(id);
            setTag(builder, "" + id + "-" + getCurrenCameraMode().name());
            addPreviewSurface(builder, null, id);
            // lock AF and Precapture
            applySettingsForLockAndPrecapture(builder, id);
            CaptureRequest request = builder.build();
            mLockRequestHashCode[id] = request.hashCode();;
            mCaptureSession[id].capture(request, mCaptureCallback, mCameraHandler);

            // if flash is on, does not lock AE until the AE state is CONTROL_AE_STATE_CONVERGED.
            // if flash is off, lock AE now.
            if (!isFlashOn(id)) {
                applySettingsForLockExposure(mPreviewRequestBuilder[id], id);
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id].build(),
                        mCaptureCallback, mCameraHandler);
            } else {
                // for longshot flash, need to re-configure the preview flash mode.
                applyFlash(mPreviewRequestBuilder[id], id);
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            }

            if(mHiston) {
                updateGraghViewVisibility(View.INVISIBLE);
                updateRGBGraghViewVisibility(View.INVISIBLE);
            }

            if(mBGStatson) {
                updateBGStatsVisibility(View.INVISIBLE);
            }

            if(mBEStatson) {
                updateBEStatsVisibility(View.INVISIBLE);
            }

            if(mRSStatson) {
                updateRSStatsVisibility(View.INVISIBLE);
            }
            if(mPerformanceDebugEnable.equals("on")){
                updateGapGraghViewVisibility(View.INVISIBLE);
            }
        } catch (CameraAccessException | IllegalStateException e) {
           Log.e(TAG,e);
        }

    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus(int id) {
        if (mActivity == null || mCameraDevice[id] == null
                || !checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            enableShutterAndVideoOnUiThread(id);
            warningToast("Camera is not ready yet to take a picture.");
            return;
        }
        Log.i(TAG, "lockFocus " + id+",mState[id]="+mState[id]+",mLockAFAE="+mLockAFAE);
        if(mActivity.getPerformenceTest()) {
            mLockFocusTime = System.currentTimeMillis();
            mHasMapTimes.put("buttonClick->lockFocus", mLockFocusTime - mStartedTime);
        }

        try {
            // start repeating request to get AF/AE state updates
            // for mono when mono preview is off.
            if(id == MONO_ID && !canStartMonoPreview()) {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            } else {
                // for longshot flash, need to re-configure the preview flash mode.
                if (mLongshotActive && isFlashOn(id)) {
                    mCaptureSession[id].stopRepeating();
                    applyFlash(mPreviewRequestBuilder[id], id);
                    mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                                            .build(), mCaptureCallback, mCameraHandler);
                }
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e);
        }

        mTakingPicture[id] = true;
        if (mState[id] == STATE_WAITING_TOUCH_FOCUS) {
            mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[id]);
            mState[id] = STATE_WAITING_AF_LOCK;
            mLockRequestHashCode[id] = 0;
            return;
        }

        try {
            CaptureRequest.Builder builder = getRequestBuilder(id);
            setTag(builder, "" + id + "-" + getCurrenCameraMode().name());
            addPreviewSurface(builder, null, id);

            if (mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                applySettingsForLockFocus(builder, id);
            }
            CaptureRequest request = builder.build();

            if (mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                mLockRequestHashCode[id] =  request.hashCode();
                mCaptureSession[id].capture(request, mCaptureCallback, mCameraHandler);
            }else{
                mLockRequestHashCode[id] = 0;
            }
            mState[id] = STATE_WAITING_AF_LOCK;
            Log.d(TAG,"mState[id]="+mState[id] +",end capture mLockRequestHashCode[id]="+mLockRequestHashCode[id]+
                    ",id="+id+",mLockAFAE="+mLockAFAE);
            if (mHiston) {
                updateGraghViewVisibility(View.INVISIBLE);
                updateRGBGraghViewVisibility(View.INVISIBLE);
            }

            if (mBGStatson) {
                updateBGStatsVisibility(View.INVISIBLE);
            }

            if (mBEStatson) {
                updateBEStatsVisibility(View.INVISIBLE);
            }

            if (mRSStatson) {
                updateRSStatsVisibility(View.INVISIBLE);
            }
            if(mPerformanceDebugEnable.equals("on")){
                updateGapGraghViewVisibility(View.INVISIBLE);
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e);
        }
    }

    private void autoFocusTrigger(int id) {
        Log.d(TAG, "autoFocusTrigger " + id);
        if (null == mActivity || null == mCameraDevice[id]
                || !checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            warningToast("Camera is not ready yet to take a picture.");
            mInTAF = false;
            return;
        }
        try {
            CaptureRequest.Builder builder = getRequestBuilder(id);
            setTag(builder, "" + id + "-" + getCurrenCameraMode().name());
            if ((mCurrentSceneMode.mode == CameraMode.VIDEO ||
                    mCurrentSceneMode.mode == CameraMode.HFR ||
                    mCurrentSceneMode.mode == CameraMode.CINEMATIC)) {
                Surface surface = getPreviewSurfaceForSession(id);
                builder.addTarget(surface);
                if (mIsRecordingVideo) {
                     builder.addTarget(mVideoRecordingSurface);
                }
            } else {
                addPreviewSurface(builder, null, id);
            }

            mControlAFMode = CaptureRequest.CONTROL_AF_MODE_AUTO;
            mIsAutoFocusStarted = true;
            mIsCanceled = false;
            applySettingsForAutoFocus(builder, id);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mState[id] = STATE_WAITING_TOUCH_FOCUS;
            if (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                    mCurrentSceneMode.mode == CameraMode.HFR ||
                    mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
                applyVideoFlash(builder, id); //apply flash mode for video/HFR
            } else {
                applyFlash(builder, id); //apply flash mode and AEmode for this temp builder
            }
            if (mSettingsManager.isMaxConfigureSize(id, mVideoSize)) {
                builder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                        CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                Log.v(TAG, " set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
            }
            if (mCurrentSceneMode.mode == CameraMode.HFR && isHighSpeedRateCapture()) {
                List<CaptureRequest> tafBuilderList = isSSMEnabled() ?
                        createSSMBatchRequest(builder) :
                        getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCaptureSession[id],builder);
                mCaptureSession[id].captureBurst(tafBuilderList, mCaptureCallback, mCameraHandler);
            } else {
                mCaptureSession[id].capture(builder.build(), mCaptureCallback, mCameraHandler);
            }
            setAFModeToPreview(id, mControlAFMode);
            Log.i(TAG,"autoFocusTrigger,mLockAFAE:" + mLockAFAE);
            if(mLockAFAE == LOCK_AF_AE_STATE_NONE) {
                Message message =
                        mCameraHandler.obtainMessage(CANCEL_TOUCH_FOCUS, id, 0, mCameraId[id]);
                sendFocusCancelMsg(message);
            }
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG,e);
        }
    }

    public void linkBayerMono(int id) {
        Log.d(TAG, "linkBayerMono " + id);
        if (id == BAYER_ID) {
            mPreviewRequestBuilder[id].set(BayerMonoLinkEnableKey, (byte) 1);
            mPreviewRequestBuilder[id].set(BayerMonoLinkMainKey, (byte) 1);
            mPreviewRequestBuilder[id].set(BayerMonoLinkSessionIdKey, MONO_ID);
        } else if (id == MONO_ID) {
            mPreviewRequestBuilder[id].set(BayerMonoLinkEnableKey, (byte) 1);
            mPreviewRequestBuilder[id].set(BayerMonoLinkMainKey, (byte) 0);
            mPreviewRequestBuilder[id].set(BayerMonoLinkSessionIdKey, BAYER_ID);
        }
    }

    public void unLinkBayerMono(int id) {
        Log.d(TAG, "unlinkBayerMono " + id);
        if (id == BAYER_ID) {
            mPreviewRequestBuilder[id].set(BayerMonoLinkEnableKey, (byte) 0);
        } else if (id == MONO_ID) {
            mPreviewRequestBuilder[id].set(BayerMonoLinkEnableKey, (byte) 0);
        }
    }

    public PostProcessor getPostProcessor() {
        return mPostProcessor;
    }

    private void setSensorMode(CaptureRequest.Builder captureBuilder){
        Log.v(TAG, "captureStillPicture set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION " );
        if(mSettingsManager.isMultiCameraEnabled()){
            Set<String> ids = mSettingsManager.getQuadBayerPhysicalStreamIds();
            if (ids != null && ids.size() != 0) {
                for (String physicalId : ids) {
                    Log.i(TAG, "setPhysicalCameraKey id: " + physicalId);
                    captureBuilder.setPhysicalCameraKey(CaptureRequest.SENSOR_PIXEL_MODE,
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION, physicalId);
                }
            }
        }else{
            captureBuilder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }

    private void captureStillPicture(final int id) {
        Log.i(TAG, "captureStillPicture " + id+",isflashRequired="+isflashRequired);
        mJpegImageData = null;
        mIsRefocus = false;
        if (isDeepZoom()) mSupportZoomCapture = false;
        if (mPaused) {
            return;
        }
        try {
            if (null == mActivity || null == mCameraDevice[id]
                    || !checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
                enableShutterAndVideoOnUiThread(id);
                mLongshotActive = false;
                mTakingPicture[id] = false;
                if (mCurrentSceneMode.mode != CameraMode.PRO_MODE)
                    mUI.enableZoomSeekBar(true);
                warningToast("Camera is not ready yet to take a picture.");
                return;
            }
            if (mCurrentSceneMode.mode == CameraMode.PRO_MODE && mLongExpTime > maxExpTime) {
                mIsLongExpTmCp = true;
            }
            Set<String> physicalIds = mSettingsManager.getPhysicalCameraId();
            if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                physicalIds = mSettingsManager.getQuadBayerPhysicalStreamIds();
            }
            CaptureRequest.Builder captureBuilder = getRequestBuilder(
                CameraDevice.TEMPLATE_STILL_CAPTURE, id, physicalIds);
            if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
                applySettingsForLockExposure(captureBuilder, id);
            }
            if ((mSettingsManager.isZSLInHALEnabled() || isActionImageCapture()) && !isLongExpTmCaptrure()) {
                Log.d(TAG," set CONTROL_ENABLE_ZSL true");
                captureBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            } else {
                Log.d(TAG," set CONTROL_ENABLE_ZSL false");
                captureBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL, false);
            }
            if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                setSensorMode(captureBuilder);
            }
            String rawcbinfoVaule = mSettingsManager.getValue(SettingsManager.KEY_RAWINFO_TYPE);
            if(rawcbinfoVaule != null && !rawcbinfoVaule.equals("disable") && !rawcbinfoVaule.equals("off")) {
                byte[] rawType;
                if(Integer.parseInt(rawcbinfoVaule) == 0){ //mipi reaw
                    rawType = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,(byte) 0x00, (byte) 0x00,(byte) 0x00};
                    captureBuilder.set(CaptureModule.rawinfo_idealraw_request, rawType);
                }else if(Integer.parseInt(rawcbinfoVaule) == 1){//ife ideal raw
                    rawType =  new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,(byte) 0x00, (byte) 0x00,(byte) 0x00};
                    captureBuilder.set(CaptureModule.rawinfo_idealraw_request, rawType);
                }else if(Integer.parseInt(rawcbinfoVaule) == 2){//bsp ideal raw
                    rawType = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,(byte) 0x00, (byte) 0x00,(byte) 0x00};
                    captureBuilder.set(CaptureModule.rawinfo_idealraw_request, rawType);
                }
            }
            applySettingsForJpegInformation(captureBuilder, id);
            applyAFRegions(captureBuilder, id);
            applyAERegions(captureBuilder, id);
            applySettingsForCapture(captureBuilder, id);
            if (!mLongshoting) {
                VendorTagUtil.setCdsMode(captureBuilder, 2);// CDS 0-OFF, 1-ON, 2-AUTO
                applyCaptureMFNR(captureBuilder);
            }
            applyCaptureBurstFps(captureBuilder);
            applyAICameraSnapshot(captureBuilder);
            applyFlashMode(captureBuilder);

            String valueFS2 = mSettingsManager.getValue(SettingsManager.KEY_SENSOR_MODE_FS2_VALUE);
            int fs2Value = 0;
            if (valueFS2 != null) {
                fs2Value = Integer.parseInt(valueFS2);
            }
            if (!mSettingsManager.isMultiCameraEnabled() && !mMultiResReprocessEnabled) {
                if (!(isDeepZoom() || (fs2Value ==1) ||
                        mSettingsManager.getQuadBayerSensorPrefEnabled())) {
                    addPreviewSurface(captureBuilder, null, id);
                }
            }
            if(mRawReprocessType != 0 && !PersistUtil.isRawReprocessQcfa() && !mMultiResReprocessEnabled){
                addPreviewSurface(captureBuilder, null, id);
            }
            if (mUI.getCurrentProMode() == ProMode.MANUAL_MODE) {
                applyFocusDistance(captureBuilder, String.valueOf(
                        mSettingsManager.getCalculatedFocusDistance()));
            }
            lux_index_threadhold = PersistUtil.getLuxIdxThreadhold();
            mAideAECLuxIndex = mAECLuxIndex;
            Log.i(TAG, "set aide tags, mAideAECLuxIndex: " + mAideAECLuxIndex + ",lux_index_threadhold:" + lux_index_threadhold + ",isAIDE2Enabled: "+ isAIDE2Enabled());

            //apply hwmfnr and aide2 param
            try {
                captureBuilder.set(CaptureModule.isAIDE2Enabled, (byte)(isAIDE2Enabled() && mAideAECLuxIndex >= lux_index_threadhold ? 0x01 : 0x00));
            } catch (IllegalArgumentException e) {
                Log.w(TAG,EXCEPTION_LOG,"can not read aide2 enable tag");
            }
            if (isDeepZoom()) mSupportZoomCapture = true;
            if(isClearSightOn()) {
                captureStillPictureForClearSight(id);
            } else if(id == getMainCameraId() && mPostProcessor.isFilterOn()) { // Case of post filtering
                captureStillPictureForFilter(captureBuilder, id);
            } else {
                if (mSettingsManager.isMultiCameraEnabled()) {
                    int count = addPhysicalCaptureTarget(captureBuilder);
                    if (mSettingsManager.isLogicalFeatureEnable(
                            SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK)) {
                        captureBuilder.addTarget(mImageReader[id].getSurface());
                    } else {
                        if (count == 0) {
                            warningToast("No output is selected");
                            unlockFocus(id);
                            return;
                        }
                    }
                }else {
                    if (mSettingsManager.isHeifWriterEncoding()) {
                        long captureTime = System.currentTimeMillis();
                        mNamedImages.nameNewImage(captureTime);
                        NamedEntity name = mNamedImages.getNextNameEntity();
                        String title = (name == null) ? null : name.title;
                        long date = (name == null) ? -1 : name.date;
                        String pictureFormat = mLongshotActive ? "heics" : "heic";
                        String path = Storage.generateFilepath(title, pictureFormat);
                        String value = mSettingsManager.getValue(SettingsManager.KEY_JPEG_QUALITY);
                        int quality = getQualityNumber(value);
                        int orientation = CameraUtil.getJpegRotation(id, mOrientation);
                        int imageCount = mLongshotActive ? PersistUtil.getLongshotShotLimit() : 1;
                        HeifWriter writer = createHEIFEncoder(path, mPictureSize.getWidth(), mPictureSize.getHeight(),
                                orientation, imageCount, quality);
                        if (writer != null) {
                            mHeifImage = new HeifImage(writer, path, title, date, orientation, quality);
                            Surface input = writer.getInputSurface();
                            Log.d(TAG, "Add HeifWriter image reader surface input=."+input);
                            mHeifOutput.addSurface(input);
                            try {
                                mCaptureSession[id].updateOutputConfiguration(mHeifOutput);
                                captureBuilder.addTarget(input);
                                writer.start();
                            } catch (IllegalStateException | IllegalArgumentException e) {
                                Log.e(TAG,e.toString());
                            }
                        }
                    } else {
                        if (mRawReprocessType == 0) {
                            if (isMultiResolutionImageReaderEnabled()) {
                                Log.d(TAG, "Add multi image reader surface to snapshot req.");
                                captureBuilder.addTarget(mMultiResImageReader.getSurface());
                            } else {
                                if ((mYUV10bit || mYUV10BitWithMetadata) &&
                                        mYUV10bitImageReader[id] != null) {
                                    captureBuilder.addTarget(mYUV10bitImageReader[id].getSurface());
                                }
                                if (mImageReader[id] != null && !isAIDE2Enabled()) {
                                    captureBuilder.addTarget(mImageReader[id].getSurface());
                                }
                            }
                            if (mSaveRaw && !isPhysicalRaw() ){
                                addPhysicalCaptureTarget(captureBuilder);
                                if(mSettingsManager.JPEG_FORMAT == mSettingsManager.getSavePictureFormat()) {
                                    captureBuilder.removeTarget(mImageReader[id].getSurface());
                                }
                            } else if (mSaveRaw && isPhysicalRaw()) {
                                captureBuilder.addTarget(mRawImageReader[id].getSurface());
                                captureBuilder.addTarget(mImageReader[id].getSurface());
                            }
                        }
                        if(mRawReprocessType != 0){
                            Log.i(TAG, " add raw image for first capture request- "+mRAWImageReader[0].getSurface());
                            captureBuilder.addTarget(mRAWImageReader[0].getSurface());
                        }
                        if (isAIDE2Enabled()) {
                            mCaptureRequestNum = 0;
                            Set<String> physical_ids = mSettingsManager.getAllPhysicalCameraId();
                            if (physical_ids != null && physical_ids.size() != 0) {
                                synchronized (mActiveCameraIds) {
                                    mAideActiveCameraIds.clear();
                                    if (mActiveCameraIds.size() > 1) {
                                        for (int activeId : mActiveCameraIds) {
                                            if (activeId != Integer.valueOf(mMasterCameraId)) {
                                                Log.i(TAG, "add Aux full yuv for dual zone" + activeId);
                                                captureBuilder.addTarget(mAideFullImageReader[getIndexByPhysicalId(Integer.toString(activeId))].getSurface());
                                                mCaptureRequestNum++;
                                                mAideActiveCameraIds.put(activeId, false);
                                            } else {
                                                Log.i(TAG, "add master full yuv for dual zone " + activeId);
                                                mAideActiveCameraIds.put(activeId, true);
                                                captureBuilder.addTarget(mAideFullImageReader[getIndexByPhysicalId(Integer.toString(activeId))].getSurface());
                                                mCaptureRequestNum++;
                                                if (mAideAECLuxIndex >= lux_index_threadhold) {//for low light, only HWMFNR, will not add ds image
                                                    Log.i(TAG, "add master ds yuv for dual zone " + activeId);
                                                    captureBuilder.addTarget(mAideDs4ImageReader[getIndexByPhysicalId(Integer.toString(activeId))].getSurface());
                                                    mCaptureRequestNum++;
                                                }
                                            }
                                        }
                                    } else if (mActiveCameraIds.size() == 1) {
                                        mAideActiveCameraIds.put(mActiveCameraIds.get(0), true);
                                        Log.i(TAG, "add active full yuv for single zone " + mActiveCameraIds.get(0));
                                        captureBuilder.addTarget(mAideFullImageReader[getIndexByPhysicalId(Integer.toString(mActiveCameraIds.get(0)))].getSurface());
                                        mCaptureRequestNum++;
                                        if (mAideAECLuxIndex >= lux_index_threadhold) {//for low light, only HWMFNR, will not add ds image
                                            Log.i(TAG, "add active ds yuv for single zone " + mActiveCameraIds.get(0));
                                            captureBuilder.addTarget(mAideDs4ImageReader[getIndexByPhysicalId(Integer.toString(mActiveCameraIds.get(0)))].getSurface());
                                            mCaptureRequestNum++;
                                        }
                                    }
                                }
                            } else {
                                captureBuilder.addTarget(mAideFullImageReader[getMainCameraId()].getSurface());
                                mCaptureRequestNum++;
                                if (mAideAECLuxIndex >= lux_index_threadhold) {//for low light, only HWMFNR, will not add ds image
                                    captureBuilder.addTarget(mAideDs4ImageReader[getMainCameraId()].getSurface());
                                    mCaptureRequestNum++;
                                }
                            }
                        }
                    }
                }
                if (mPaused || !mCamerasOpened) {
                    //for avoid occurring crash when click back before capture finished.
                    //CameraDevice was already closed
                    return;
                }
                if (mLongshotActive) {
                    captureStillPictureForLongshot(captureBuilder, id);
                } else {
                    captureStillPictureForCommon(captureBuilder, id);
                }
               enableShutterAnimal(id);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Capture still picture has failed = "+ e);
        } catch (IllegalArgumentException e) {
            if (mSettingsManager.isMultiCameraEnabled()) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("Invalid physical camera id")||
                    (errorMsg.contains("unconfigured Input/Output Surface")))){
                    warningToast("Please enable physical cameras of outputs first");
                    unlockFocus(id);
                }
            }
            Log.e(TAG,e.toString());
        }
    }

    private void captureStillPictureForClearSight(int id) throws CameraAccessException{
        CaptureRequest.Builder captureBuilder =
                ClearSightImageProcessor.getInstance().createCaptureRequest(mCameraDevice[id]);

        if(mSettingsManager.isZSLInHALEnabled()) {
            captureBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
        }else{
            captureBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL, false);
        }
        applySettingsForJpegInformation(captureBuilder, id);
        addPreviewSurface(captureBuilder, null, id);
        VendorTagUtil.setCdsMode(captureBuilder, 2); // CDS 0-OFF, 1-ON, 2-AUTO
        applySettingsForCapture(captureBuilder, id);
        applySettingsForLockExposure(captureBuilder, id);
        checkAndPlayShutterSound(id);
        if(mPaused || !mCamerasOpened) {
            //for avoid occurring crash when click back before capture finished.
            //CameraDevice was already closed
            return;
        }
        ClearSightImageProcessor.getInstance().capture(
                id==BAYER_ID, mCaptureSession[id], captureBuilder, mCaptureCallbackHandler);
    }

    private void captureStillPictureForFilter(CaptureRequest.Builder captureBuilder, int id) throws CameraAccessException{
        applySettingsForLockExposure(captureBuilder, id);
        checkAndPlayShutterSound(id);
        if(mPaused || !mCamerasOpened) {
            //for avoid occurring crash when click back before capture finished.
            //CameraDevice was already closed
            return;
        }
        if (!isDeepZoom()) {
            mCaptureSession[id].stopRepeating();
        }
        captureBuilder.addTarget(mImageReader[id].getSurface());
        if (mSaveRaw) {
            captureBuilder.addTarget(mRawImageReader[id].getSurface());
        }
        if ((mYUV10bit || mYUV10BitWithMetadata) && mYUV10bitImageReader[id] != null) {
            captureBuilder.addTarget(mYUV10bitImageReader[id].getSurface());
        }
        mPostProcessor.onStartCapturing();
        if(mPostProcessor.isManualMode()) {
            mPostProcessor.manualCapture(captureBuilder, mCaptureSession[id], mCaptureCallbackHandler);
        } else {
            List<CaptureRequest> captureList = mPostProcessor.setRequiredImages(captureBuilder);
            mCaptureSession[id].captureBurst(captureList, mPostProcessor.getCaptureCallback(), mCaptureCallbackHandler);
        }
    }

    public void doShutterAnimation() {
        if (mUI != null) {
            mUI.doShutterAnimation();
        }
    }

    private CameraCaptureSession.CaptureCallback mLongshotCallBack= new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session,
                                           CaptureRequest request,
                                           TotalCaptureResult result) {
                mCaptureResult = result;
                if (mPaused) {
                    return;
                }
                String requestTag = String.valueOf(request.getTag());
                if (requestTag.equals("preview")) {
                    updateT2tTrackerView(result);
                    return;
                }
                mNumFramesArrived.incrementAndGet();
                if(mPerformanceDebugEnable != null && mPerformanceDebugEnable.equals("on")){
                    if (mNumFramesArrived.get() == 1) {
                        mBurstStartTime = System.currentTimeMillis();
                    }
                    mBurstFps = (float)((System.currentTimeMillis() - mBurstStartTime)/mNumFramesArrived.get());
                    updatePerformanceDebugValue(8, Float.toString(mBurstFps));
                }
                Log.d(TAG, "captureStillPictureForLongshot onCaptureCompleted: "
                        + mNumFramesArrived.get() + " " + mShotNum);
                if (mLongshotActive) {
                    checkAndPlayShutterSound(getMainCameraId());
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.doShutterAnimation();
                        }
                    });
                }
                if (mBurstLimit) {
                    boolean burst_limit = "capture-limit".equals(String.valueOf(request.getTag()));

                    int burst_enable = -1;
                    try{
                        burst_enable = result.get(CaptureModule.multiframe_burst_enable);
                        Log.d(TAG,"burst_enable ="+burst_enable);
                    } catch (IllegalArgumentException | NullPointerException e){
                    }

                    if (burst_limit && burst_enable == 1) {
                        try{
                            session.capture(request,this,mCaptureCallbackHandler);
                            Log.d(TAG,"burst_limit send one request");
                        } catch (CameraAccessException e){
                            Log.e(TAG,e.toString());
                        }
                    }
                }

            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session,
                                            CaptureRequest request, CaptureResult partialResult) {
                Log.d(TAG," onCaptureProgressed");
            }

            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                    long timestamp, long frameNumber) {
                if (mPaused) {
                    return;
                }
                String requestTag = String.valueOf(request.getTag());
                if (requestTag.equals("preview")) {
                    return;
                }
                mLongshoting = true;
                Log.d(TAG, "captureStillPictureForLongshot onCaptureStarted: " + mNumFramesArrived.get());
            }

            @Override
            public void onCaptureBufferLost(CameraCaptureSession session,
                   CaptureRequest request, Surface target, long frameNumber) {
                String requestTag = String.valueOf(request.getTag());
                if (requestTag.equals("preview")) {
                    return;
                }
                Log.d(TAG, "captureStillPictureForLongShot onCaptureBufferLost: frameNumber is "
                        + frameNumber);

            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureFailure result) {
                if (mPaused) {
                    return;
                }
                String requestTag = String.valueOf(request.getTag());
                if (requestTag.equals("preview")) {
                    return;
                }
                Log.d(TAG, "captureStillPictureForLongshot onCaptureFailed ." + mNumFramesArrived.get());
            }

            @Override
            public void onCaptureSequenceCompleted(CameraCaptureSession session, int
                            sequenceId, long frameNumber) {
                Log.i(TAG,"onCaptureSequenceCompleted, " + mNumFramesArrived.get());
                mTakingPicture[getMainCameraId()] = false;
                if (mPaused) {
                    return;
                }
                if (mSettingsManager.isHeifWriterEncoding()) {
                    mLongshotActive = false;
                    if (mHeifImage != null) {
                        try {
                            mHeifImage.getWriter().stop(5000);
                            mHeifImage.getWriter().close();
                            mActivity.getMediaSaveService().addHEIFImage(mHeifImage.getPath(),
                                    mHeifImage.getTitle(),mHeifImage.getDate(),null,mPictureSize.getWidth(),mPictureSize.getHeight(),
                                    mHeifImage.getOrientation(),null,mContentResolver,mOnMediaSavedListener,mHeifImage.getQuality(),"heics");
                        } catch (Exception e) {
                            Log.e(TAG,e.toString());
                        } finally {
                            try{
                                mHeifOutput.removeSurface(mHeifImage.getInputSurface());
                                session.updateOutputConfiguration(mHeifOutput);
                                mHeifImage = null;
                            }catch (CameraAccessException e) {
                                Log.e(TAG,e);
                            }catch (Exception e) {
                                Log.e(TAG,e.toString());
                            }
                        }
                    }

                }

                mLongshoting = false;
                if (mNumFramesArrived.get() < mShotNum && mLongshotActive && !mBurstLimit && !mPaused) {
                    captureStillPicture(CURRENT_ID);
                }else {
                    unlockFocus(getMainCameraId());
                }
            }
        };
    private void captureStillPictureForLongshot(CaptureRequest.Builder captureBuilder, int id) throws CameraAccessException{
        mBurstLimit = "1".equals(mSettingsManager.getValue(SettingsManager.KEY_BURST_LIMIT));
        if (!mBurstLimit) {
            List<CaptureRequest> burstList = new ArrayList<>();
            float previewProportion = 0f;
            float fps = mSettingsManager.getFps(mPictureSize);
            if(mSettingsManager.getSavePictureFormat() == SettingsManager.HEIF_FORMAT || mSettingsManager.getSavePictureFormat() == SettingsManager.HEIC_TENBIT_FORMAT) {
                mShotNum = (int)fps *2;
            }
            Log.i(TAG,"max fps:" + fps + ",mShotNum:" + mShotNum);
            if (fps > 0) {
                previewProportion = 30f / fps - 1f;
            }
            Log.i(TAG, "burstShot, previewProportion:" + previewProportion);
            float captureProportion = 1.0f;
            int previewCount = 0;
            int captureCount = 0;
            for (int i = 0; i < 30; i++) {
                if ((captureProportion - previewProportion) >= 0f) {
                    captureBuilder.setTag("capture");
                    burstList.add(captureBuilder.build());
                    captureProportion -= previewProportion;
                    captureCount++;
                } else {
                    mPreviewRequestBuilder[id].setTag("preview");
                    burstList.add(mPreviewRequestBuilder[id].build());
                    captureProportion++;
                    previewCount++;
                }
            }
            Log.d(TAG, "burstShot, previewCount " + previewCount + ", captureCount " + captureCount);
            mCaptureSession[id].setRepeatingBurst(burstList, mLongshotCallBack, mCaptureCallbackHandler);
            if(mSettingsManager.getSavePictureFormat() == SettingsManager.HEIF_FORMAT || mSettingsManager.getSavePictureFormat() == SettingsManager.HEIC_TENBIT_FORMAT) {
                mHandler.postDelayed(() -> {
                    stopBurstShot();
                }, 2000);
            }
        } else {
            captureBuilder.setTag("capture-limit");
            mCaptureSession[id].capture(captureBuilder.build(),mLongshotCallBack,mCaptureCallbackHandler);
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUI.enableVideo(false);
            }
        });
    }

    CameraCaptureSession.CaptureCallback mAideV2CaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            Log.d(TAG, "onCaptureCompleted");
            getHWMFandAIDETuningParams(result);
            mCaptureResult = result;
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request,
                                    CaptureFailure result) {
            Log.d(TAG, "onCaptureFailed");
            unlockFocus(getMainCameraId());
            enableShutterButtonOnMainThread(getMainCameraId());
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int
                sequenceId, long frameNumber) {
            Log.d(TAG, "onCaptureSequenceCompleted:" + frameNumber);
            mNamedImages.nameNewImage(System.currentTimeMillis());
            NamedEntity namedEntity = mNamedImages.getNextNameEntity();
            String title = (namedEntity == null) ? null : namedEntity.title;
            int id = getMainCameraId();
            int orientation = CameraUtil.getJpegRotation(id,mOrientation);
            int quality = getQualityNumber(mSettingsManager.getValue(SettingsManager.KEY_JPEG_QUALITY));
            unlockFocus(id);
            enableShutterButtonOnMainThread(id);
            AIDenoiserService aiDenoiserService = mActivity.getAIDenoiserService();
            String format = mSettingsManager.getValue(SettingsManager.KEY_AI_DENOISER_FORMAT);
            if(mAideAECLuxIndex < lux_index_threadhold){//high light only do HWMFNR and no need to crop
                aiDenoiserService.wantImagesNum(mCaptureRequestNum);
                Size yuvSize = new Size(mAideFullImage.getWidth(), mAideFullImage.getHeight());
                Log.i(TAG,"save jpeg for mfnr aide start, yuv size:" + yuvSize.toString());
                byte[] yuv = getYUVFromImage(mAideFullImage);
                int stride = mAideFullImage.getPlanes()[0].getRowStride();
                if (TRACE_DEBUG) Trace.beginSection("save jpeg for aide2");
                Rect rect = aiDenoiserService.getCropRegion(yuvSize.getWidth(), yuvSize.getHeight(), mPictureSize.getWidth(), mPictureSize.getHeight());
                if(mAideFullImage.getWidth() != rect.width() || mAideFullImage.getHeight() != rect.height()) {
                    yuv = aiDenoiserService.cropYuvImage(yuv, stride, yuvSize.getWidth(), yuvSize.getHeight(), rect);
                }
                Bitmap bitmap = aiDenoiserService.yuvToRgbAndResize(yuv,rect.width(), rect.height(), stride,
                        mPictureSize.getWidth(), mPictureSize.getHeight(), Integer.parseInt(format));
                byte[] jpeg = aiDenoiserService.bitmapToJpeg(bitmap, orientation, mCaptureResult, quality);
                mActivity.getMediaSaveService().addImage(
                        jpeg, title, 0L, null,
                        mPictureSize.getWidth(),mPictureSize.getHeight(),
                        orientation, null, getMediaSavedListener(),
                        mActivity.getContentResolver(), "jpeg");
                mActivity.updateThumbnail(jpeg);
                if (TRACE_DEBUG) Trace.endSection();
                mAideFullImage.close();
                mAideFullImage = null;
                return;
            }
            Log.d(TAG,"wait " + mCaptureRequestNum + " YUVs");
            aiDenoiserService.wantImagesNum(mCaptureRequestNum);
            Rect cropRegion = cropRegionForAideV2Zoom();
            //getimagedata
            int[] inputFrameDim = {mAideFullImage.getWidth(), mAideFullImage.getHeight(), mAideFullImage.getPlanes()[0].getRowStride(), mAideFullImage.getPlanes()[2].getRowStride()};
            Log.d(TAG,"full image width " + inputFrameDim[0] + ", height:" + inputFrameDim[1] + ",stride:" + inputFrameDim[2]);
            int[] downFrameDim = {mAideDownImage.getWidth(), mAideDownImage.getHeight(), mAideDownImage.getPlanes()[0].getRowStride(), mAideDownImage.getPlanes()[2].getRowStride()};
            Log.d(TAG,"ds image width " + downFrameDim[0] + ", height:" + downFrameDim[1] + ",stride:" + downFrameDim[2]);
            ByteBuffer inputY= mAideFullImage.getPlanes()[0].getBuffer();
            ByteBuffer inputC = mAideFullImage.getPlanes()[2].getBuffer();
            inputY.rewind();
            inputC.rewind();
            ByteBuffer srcInputY = ByteBuffer.allocateDirect(inputY.remaining());
            srcInputY.put(inputY);
            ByteBuffer srcInputUV = ByteBuffer.allocateDirect(inputC.remaining());
            srcInputUV.put(inputC);

            ByteBuffer dsinputY= mAideDownImage.getPlanes()[0].getBuffer();
            ByteBuffer dsinputC = mAideDownImage.getPlanes()[2].getBuffer();
            dsinputY.rewind();
            dsinputC.rewind();
            ByteBuffer srcDsInputY = ByteBuffer.allocateDirect(dsinputY.remaining());
            srcDsInputY.put(dsinputY);
            ByteBuffer srcDsInputUV = ByteBuffer.allocateDirect(dsinputC.remaining());
            srcDsInputUV.put(dsinputC);

            String mode = mSettingsManager.getValue(SettingsManager.KEY_AI_DENOISER_MODE);
            if(mode.equals("0")){
                mBGain = mGGain*detail_enhancement*4;
            }
            Log.i(TAG,"mAideV2CaptureCallback, mRGain:" + mRGain + ",mGGain:" + mGGain + ",detail_enhancement:" + detail_enhancement + ",mBGain:" + mBGain
                    + ",mEnhancefactor:" + mEnhancefactor + ",mGainThresholdY:" + mGainThresholdY + ",mGainThresholdUV:" + mGainThresholdUV);
            AIDEV2ProcessFrameArgs aideV2Args = new AIDEV2ProcessFrameArgs(inputFrameDim, downFrameDim, srcInputY, srcInputUV, srcDsInputY, srcDsInputUV,
                    title, cropRegion, mCaptureResult, mPictureSize, denoiseStrengthParam, mAideAdrcGain, (int)(mRGain*1024), (int)(mBGain*1024), (int)(mGGain*1024), orientation, quality);
            mAideFullImage.close();
            mAideFullImage = null;
            mAideDownImage.close();
            mAideDownImage = null;
            namedEntity = null;

            //process aidev2
            Log.d(TAG, " mAideV2CaptureCallback, start to call aide lib");
            synchronized (mAideLock) {
                if (TRACE_DEBUG) Trace.beginSection("aide2 process");
                aiDenoiserService.startAideV2Process(aideV2Args.getsrcInputY(), aideV2Args.getsrcInputUV(), aideV2Args.getsrcDsInputY(),aideV2Args.getsrcDsInputUV(),
                        aideV2Args.getInputFrameDim(), aideV2Args.getdownFrameDim(), 100000, 100, aideV2Args.getdenoiseStrengthParam(), aideV2Args.getadrcGain(), aideV2Args.getrGain(),
                        aideV2Args.getbGain(), aideV2Args.getgGain(), Integer.parseInt(format), Integer.parseInt(mode), mEnhancefactor, mGainThresholdY, mGainThresholdUV);
                if (TRACE_DEBUG) Trace.endSection();
                if (TRACE_DEBUG) Trace.beginSection("save jpeg for aide2");
                byte[] srcImage = aiDenoiserService.generateAideV2Image(mActivity, aideV2Args.getorientation(), aideV2Args.getpictureSize(), aideV2Args.getcropRegion(), aideV2Args.getcaptureResult(), aideV2Args.getquality(), Integer.parseInt(format));
                mActivity.getMediaSaveService().addImage(
                        srcImage, aideV2Args.gettitle(), 0L, null,
                        aideV2Args.getpictureSize().getWidth(),
                        aideV2Args.getpictureSize().getHeight(),
                        aideV2Args.getorientation(), null, getMediaSavedListener(),
                        mActivity.getContentResolver(), "jpeg");
                mActivity.updateThumbnail(srcImage);
                if (TRACE_DEBUG) Trace.endSection();
            }
        }
    };

    public Rect cropRegionForAideV2Zoom() {
        Rect originalCropRegion = new Rect();
        Set<String> physical_ids = mSettingsManager.getAllPhysicalCameraId();
        int masterCamera = getMainCameraId();
        if(physical_ids != null && physical_ids.size() != 0){
            String physicalId = mMasterCameraId;
            for(Integer key : mAideActiveCameraIds.keySet()){
                if(mAideActiveCameraIds.get(key)){
                    physicalId = Integer.toString(key);
                }
            }
            Log.d(TAG,"frame number: " + mCaptureResult.getFrameNumber());
            CaptureResult physicalMetaData = mCaptureResult.getPhysicalCameraResults().get(physicalId);
            masterCamera = Integer.parseInt(physicalId);
            originalCropRegion = physicalMetaData.get(CaptureResult.SCALER_CROP_REGION);
        }else {
            originalCropRegion = mCaptureResult.get(CaptureResult.SCALER_CROP_REGION);
        }
        Rect activeRegion = mSettingsManager.getSensorActiveArraySize(masterCamera);
        Log.d(TAG,"crop region from hal:" + originalCropRegion.toString());
        Log.d(TAG,"crop region for preview:" + mCropRegion[getMainCameraId()].toString());
        Log.d(TAG,"mastercamera:" +masterCamera + ",sensor active array:" + activeRegion.toString());
        //map preview crop to aide yuv size
        int left = originalCropRegion.left*mAideFullImage.getWidth()/activeRegion.width();
        int right = originalCropRegion.right*mAideFullImage.getWidth()/activeRegion.width();
        int top = originalCropRegion.top *mAideFullImage.getHeight()/activeRegion.height();
        int bottom = originalCropRegion.bottom *mAideFullImage.getHeight()/activeRegion.height();
        originalCropRegion.set(left, top, right, bottom);
        Log.d(TAG,"crop region map to yuv size:" + originalCropRegion.toString());
        //output yuv and final picture have the different resolution ratio
        Rect cropRegion = new Rect();
        if(originalCropRegion.right > mAideFullImage.getWidth() ||
                originalCropRegion.bottom > mAideFullImage.getHeight()){
            originalCropRegion.right = mAideFullImage.getWidth();
            originalCropRegion.bottom = mAideFullImage.getHeight();
        }
        int width = originalCropRegion.width();
        int height = originalCropRegion.height();
        Log.d(TAG, "cropRegionForAideV2Zoom  width: " +  width + ",height:" + height);
        if(width > mAideFullImage.getWidth() || height > mAideFullImage.getHeight()){
            width = mAideFullImage.getWidth();
            height = mAideFullImage.getHeight();
        }

        float aideRatio = (float) mAideFullImage.getWidth() / mAideFullImage.getHeight();
        float pictureRatio = (float) mPictureSize.getWidth() / mPictureSize.getHeight();
        if(aideRatio > pictureRatio){
            width = mPictureSize.getWidth()/mPictureSize.getHeight()*height;
        } else if (aideRatio < pictureRatio){
            height = width * mPictureSize.getHeight() /mPictureSize.getWidth();
        }
        Log.d(TAG, "cropRegionForAideV2Zoom current ratio width: " +  width + ",height:" + height);
        int xCenter = originalCropRegion.width() / 2 + originalCropRegion.left;
        int yCenter = originalCropRegion.height() / 2 + originalCropRegion.top;
        int xDelta = (int) (width / 2);
        int yDelta = (int) (height / 2);
        cropRegion.set(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
        Log.d(TAG, "cropRegionForAideV2Zoom  cropRegion: " +  cropRegion);
        return cropRegion;
    }
    private void captureStillPictureForCommon(CaptureRequest.Builder captureBuilder, int id) throws CameraAccessException{
        Log.i(TAG,"captureStillPictureForCommon, captureBuilder:" + captureBuilder.toString());
        checkAndPlayShutterSound(id);
        if (isMpoOn()) {
            mCaptureStartTime = System.currentTimeMillis();
            mMpoSaveHandler.obtainMessage(MpoSaveHandler.MSG_CONFIGURE,
                    Long.valueOf(mCaptureStartTime)).sendToTarget();
        }
        if(mChosenImageFormat == ImageFormat.YUV_420_888 || mChosenImageFormat == ImageFormat.PRIVATE) { // Case of ZSL, FrameFilter, SelfieMirror
            mPostProcessor.onStartCapturing();
            mCaptureSession[id].capture(captureBuilder.build(), mPostProcessor.getCaptureCallback(), mCaptureCallbackHandler);
        } else {
            if(isAIDE2Enabled()) {
                mActivity.getAIDenoiserService().resetImagesNum();
                mCaptureSession[id].capture(captureBuilder.build(), mAideV2CaptureCallback, mCaptureCallbackHandler);
                return;
            }
            mRawInputMeta =null;
            mSnapshotLatency = System.currentTimeMillis();
            if(mActivity.getPerformenceTest()) {
                mStartSnapShotTime = System.currentTimeMillis();
                if(isFlashOn(id)) {
                    mHasMapTimes.put("lockExposure->capture", mSnapshotLatency - mLockAETime);
                }else{
                    mHasMapTimes.put("buttonClick->capture", mSnapshotLatency - mStartedTime);
                }
            }
            if(mIsCloseCamera){
                return;
            }
            mCaptureSession[id].capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted (CameraCaptureSession session,
                                              CaptureRequest request,
                                              long timestamp,
                                              long frameNumber){
                    mShutterLag = System.currentTimeMillis() - mSnapshotLatency;
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    Log.i(TAG, "captureStillPictureForCommon onCaptureCompleted: " + id  + ",metadataOwnerInfo:" + result.get(CaptureModule.metadataOwnerInfo)+",result="+result);
                    mRawInputMeta = result;
                    mCaptureResult = result;
                    if (mYUV10BitWithMetadata) {
                        List<String> metaDataLists = new ArrayList<String>();
                        long sensorTimeStamp = result.get(CaptureResult.SENSOR_TIMESTAMP).longValue();
                        Log.v(TAG, " Save metaData title :" + sensorTimeStamp);

                        List<CaptureResult.Key<?>> keys = result.getKeys();
                        // Move to CameraMetadata#toString
                        for (CaptureResult.Key<?> key : keys) {
                            metaDataLists.add ("Key:");
                            metaDataLists.add (String.format("%s\n", key.getName()));
                            metaDataLists.add ("value:");
                            metaDataLists.add (String.format("%s\n", String.format("%s\n",
                                    metadataValueToString(result.get(key)))));
                        }

                        // Save YUV10bit metaData
                        String filePath = AutoTestUtil.createFile(mActivity,
                                String.valueOf(sensorTimeStamp));
                        AutoTestUtil.writeFileContent(filePath, metaDataLists);
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session,
                                            CaptureRequest request,
                                            CaptureFailure result) {
                    Log.i(TAG, "captureStillPictureForCommon onCaptureFailed: " + id);
                    mTakingPicture[id] = false;
                    mUI.enableShutter(true);
                }

                @Override
                public void onCaptureBufferLost(CameraCaptureSession session,
                                                CaptureRequest request, Surface target,
                                                long frameNumber) {
                    Log.i(TAG, "captureStillPictureForCommon onCaptureBufferLost: frameNumber is "
                            + frameNumber);
                    if (!mPaused && isOnCaptureBufferLostHintOn()) {
                        showToast("Capture failed: buffer lost!");
                    }
                }

                @Override
                public void onCaptureSequenceCompleted(CameraCaptureSession session, int
                        sequenceId, long frameNumber) {
                    Log.i(TAG, "captureStillPictureForCommon onCaptureSequenceCompleted: " + id);
                    if(mActivity.getPerformenceTest()){
                        mHasMapTimes.put("capture->onCaptureSequenceCompleted",System.currentTimeMillis() - mStartSnapShotTime);
                        mHasMapTimes.put("Total",System.currentTimeMillis() - mStartedTime);
                    }
                    if (mUI.getCurrentProMode() != ProMode.MANUAL_MODE) {
                        unlockFocus(id);
                    } else {
                        mTakingPicture[id] = false;
                        enableShutterAndVideoOnUiThread(id);
                    }
                    Log.d(TAG,"onShutterButtonRelease");
                    if (mSettingsManager.isHeifWriterEncoding()) {
                        if (mHeifImage != null) {
                            try {
                                mHeifImage.getWriter().stop(3000);
                                mHeifImage.getWriter().close();
                                mActivity.getMediaSaveService().addHEIFImage(mHeifImage.getPath(),
                                        mHeifImage.getTitle(),mHeifImage.getDate(),null,mPictureSize.getWidth(),mPictureSize.getHeight(),
                                        mHeifImage.getOrientation(),null,mContentResolver,mOnMediaSavedListener,mHeifImage.getQuality(),"heic");
                            } catch (Exception e) {
                                Log.e(TAG,e.toString());
                            } finally {
                                try{
                                    mHeifOutput.removeSurface(mHeifImage.getInputSurface());
                                    mCaptureSession[id].updateOutputConfiguration(mHeifOutput);
                                    mHeifImage = null;
                                } catch (CameraAccessException e) {
                                    Log.e(TAG,e.toString());
                                } catch (Exception e) {
                                    Log.e(TAG,e.toString());
                                }
                            }
                        }
                    }
                }
            }, mCaptureCallbackHandler);
        }
    }

    private static String metadataValueToString(Object object) {
        if (object == null) {
            return "<null>";
        }
        if (object.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            int length = Array.getLength(object);
            for (int i = 0; i < length; ++i) {
                Object item = Array.get(object, i);
                builder.append(metadataValueToString(item));

                if (i != length - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
            return builder.toString();
        } else {
            if (object instanceof LensShadingMap) {
                return ((LensShadingMap) object).toString();
            } else if (object instanceof Pair) {
                return ((Pair<?, ?>) object).toString();
            }
            return object.toString();
        }
    }

    private void captureVideoSnapshot(final int id) {
        Log.d(TAG, "captureVideoSnapshot cameraid = " + id);
        try {
            if (null == mActivity || null == mCameraDevice[id] || mCurrentSession == null) {
                warningToast("Camera is not ready yet to take a video snapshot.");
                return;
            }
            CaptureRequest.Builder captureBuilder = getRequestBuilder(
                    CameraDevice.TEMPLATE_VIDEO_SNAPSHOT,id,mSettingsManager.getPhysicalCameraId());

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtil.getJpegRotation(id, mOrientation));
            captureBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, mVideoSnapshotThumbSize);
            captureBuilder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte)80);
            applyVideoSnapshot(captureBuilder, id);
            if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
                applySettingsForLockExposure(captureBuilder, id);
            }
            if (mUI.getZoomFixedSupport()) {
                applyZoomRatio(captureBuilder, mZoomValue, id);
            } else {
                applyZoom(captureBuilder, id);
            }
            if (mHighSpeedCapture && !isVariableFPSEnabled()) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mHighSpeedFPSRange);
            }

            applyAntiBandingLevel(captureBuilder);
            // send snapshot stream together with preview and video stream for snapshot request
            // stream is the surface for the app
            List<Surface> surfaces = new ArrayList<>();
            if(!is8KInMulti) {
                addPreviewSurface(captureBuilder, surfaces, id);
            }
            if(mSettingsManager.getPhysicalCameraId() != null || mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER) != null){
                int count = addPhysicalVideoCaptureTarget(captureBuilder);
                if (mSettingsManager.isLogicalEnable() && !is8KInMulti){
                    captureBuilder.addTarget(mVideoSnapshotImageReader.getSurface());
                } else {
                    if(count == 0){
                        warningToast("No output is selected");
                        return;
                    }
                }

            } else {
                if (mSettingsManager.isHeifWriterEncoding()) {
                    long captureTime = System.currentTimeMillis();
                    mNamedImages.nameNewImage(captureTime);
                    NamedEntity name = mNamedImages.getNextNameEntity();
                    String title = (name == null) ? null : name.title;
                    long date = (name == null) ? -1 : name.date;
                    String path = Storage.generateFilepath(title, "heif");
                    String value = mSettingsManager.getValue(SettingsManager.KEY_JPEG_QUALITY);
                    int quality = getQualityNumber(value);
                    int orientation = CameraUtil.getJpegRotation(id,mOrientation);
                    HeifWriter writer = createHEIFEncoder(path,mVideoSize.getWidth(),
                            mVideoSize.getHeight(),orientation,1,quality);
                    if (writer != null) {
                        mLiveShotImage = new HeifImage(writer,path,title,date,orientation,quality);
                        Surface input = writer.getInputSurface();
                        mLiveShotOutput.addSurface(input);
                        try{
                            mCurrentSession.updateOutputConfiguration(mLiveShotOutput);
                            captureBuilder.addTarget(input);
                            writer.start();
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            Log.e(TAG,e.toString());
                        }
                    }
                } else {
                    captureBuilder.addTarget(mVideoSnapshotImageReader.getSurface());
                }

                if (mSettingsManager.isMaxConfigureSize(id, mVideoSize)) {
                    captureBuilder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, "VideoSnapshot builder set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                }

                Surface surface = getPreviewSurfaceForSession(id);
                if (getFrameProcFilterId().size() == 1 && getFrameProcFilterId().get(0) ==
                        FrameProcessor.FILTER_MAKEUP) {
                    captureBuilder.addTarget(mFrameProcessor.getInputSurfaces().get(0));
                } else {
                    captureBuilder.addTarget(surface);
                }
            }
            mSnapshotLatency = System.currentTimeMillis();
            mCurrentSession.capture(captureBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {

                        @Override
                        public void onCaptureStarted (CameraCaptureSession session,
                                                      CaptureRequest request,
                                                      long timestamp,
                                                      long frameNumber){
                            mShutterLag = System.currentTimeMillis() - mSnapshotLatency;
                        }

                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session,
                                                       CaptureRequest request,
                                                       TotalCaptureResult result) {
                            Log.i(TAG, "captureVideoSnapshot onCaptureCompleted: " + id);
                            mCaptureResult = result;
                        }

                        @Override
                        public void onCaptureFailed(CameraCaptureSession session,
                                                    CaptureRequest request,
                                                    CaptureFailure result) {
                            Log.i(TAG, "captureVideoSnapshot onCaptureFailed: " + id);
                        }

                        @Override
                        public void onCaptureBufferLost(CameraCaptureSession session,
                                                        CaptureRequest request, Surface target,
                                                        long frameNumber) {
                            Log.i(TAG, "captureVideoshot onCaptureBufferLost: frameNumber is "
                                    + frameNumber);
                            if (!mPaused && isOnCaptureBufferLostHintOn()) {
                                showToast("Capture failed: buffer lost!");
                            }
                        }

                        @Override
                        public void onCaptureSequenceCompleted(CameraCaptureSession session, int
                                sequenceId, long frameNumber) {
                            Log.i(TAG, "captureVideoSnapshot onCaptureSequenceCompleted: " + id);
                            if (mSettingsManager.isHeifWriterEncoding()) {
                                if (mLiveShotImage != null) {
                                    try {
                                        mLiveShotImage.getWriter().stop(3000);
                                        mLiveShotImage.getWriter().close();
                                        mLiveShotOutput.removeSurface(mLiveShotImage.getInputSurface());
                                        mCurrentSession.updateOutputConfiguration(mLiveShotOutput);
                                        mActivity.getMediaSaveService().addHEIFImage(mLiveShotImage.getPath(),
                                                mLiveShotImage.getTitle(),mLiveShotImage.getDate(),
                                                null,mVideoSize.getWidth(),mVideoSize.getHeight(),
                                                mLiveShotImage.getOrientation(),null,
                                                mContentResolver,mOnMediaSavedListener,
                                                mLiveShotImage.getQuality(),"heic");
                                        mLiveShotImage = null;
                                    } catch (TimeoutException | IllegalStateException e) {
                                        Log.e(TAG,e.toString());
                                    } catch (Exception e) {
                                        Log.e(TAG,e.toString());
                                    }
                                }
                            }
                        }
                    }, mCaptureCallbackHandler);
        } catch (CameraAccessException |IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "captureVideoSnapshot failed = "+ e);
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence(int id) {
        Log.i(TAG, "runPrecaptureSequence: " + id+",mLockAFAE="+mLockAFAE);
        if (!checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            return;
        }
        try {
            CaptureRequest.Builder builder = getRequestBuilder(id);
            setTag(builder, "" + id + "-" + getCurrenCameraMode().name());
            addPreviewSurface(builder, null, id);
            if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
                applySettingsForLockExposure(builder, id);
            }
            applySettingsForPrecapture(builder, id);
            CaptureRequest request = builder.build();
            mPrecaptureRequestHashCode[id] =  request.hashCode();
            mState[id] = STATE_WAITING_PRECAPTURE;
            if(mActivity.getPerformenceTest()) {
                mPreCaptureTime = System.currentTimeMillis();
                mHasMapTimes.put("lockFocus->preCapture", mPreCaptureTime - mLockFocusTime);
            }
            mCaptureSession[id].capture(request, mCaptureCallback, mCameraHandler);
            Log.d(TAG, "end capture mPrecaptureRequestHashCode: " + mPrecaptureRequestHashCode[id]);
            isFlashRequiredInDriver = false;
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e.toString());
        }
    }

    public CameraCharacteristics getMainCameraCharacteristics() {
        return mMainCameraCharacteristics;
    }
    private void saveRawImg(byte[] bytes,Image image,NamedEntity name,String title) {
        long date = (name == null) ? -1 : name.date;
        int orientation = 0;
        ExifInterface exif = null;
        orientation = CameraUtil.getJpegRotation(getMainCameraId(), mOrientation);
        try {
            exif = new ExifInterface(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            Log.w(TAG,"get exif failed");
        }
        long imglen = bytes.length;
        int imageFormat = image.getFormat();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        Log.d(TAG, "saveRawImg-onImageAvailable width=" + imageWidth + ",height=" + imageHeight + ",stride=" + image.getPlanes()[0].getRowStride() + ",imglen=" + imglen);
        if (imageFormat == ImageFormat.RAW_SENSOR && mRawReprocessType == 0) {
            TotalCaptureResult mRawMeta = waitForRawMetaData();
            int setsucess = setInfoForDng(mRawMeta);
            Log.d(TAG, "saveRawImg- mRawMeta=" + mRawMeta + ",setsucess=" + setsucess);
            if (setsucess == 0) {
                if(PersistUtil.isFuncTestRunning()) {
                    mImgType.add("dng");
                }
                mActivity.getMediaSaveService().addDng(image, imglen, title, date, null, imageWidth, imageHeight, orientation, null,
                        mOnMediaSavedListener, mContentResolver, "dng");
            } else {
                if(PersistUtil.isFuncTestRunning()) {
                    mImgType.add("raw");
                }
                mActivity.getMediaSaveService().addRawImage(bytes, title, "raw");
                image.close();
            }
        } else {
            if(PersistUtil.isFuncTestRunning()) {
                mImgType.add("raw");
            }
            mActivity.getMediaSaveService().addRawImage(bytes, title, "raw");
            if (mRawReprocessType == 0) {
                image.close();
            }
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int imageFormat) {
        Log.d(TAG, "imageFormat is " + imageFormat);
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            //init heifWriter and get input surface
            initHEIFWriter();
            String[] cameraIdList = manager.getCameraIdList();

            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                boolean foundDepth = false;
                for (int capability : capabilities) {
                    if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                        DEPTH_CAM_ID = cameraId;
                        Log.i(TAG, "Found depth camera with id " + cameraId);
                        foundDepth = true;
                    }
                }

                if(foundDepth) {
                    mCameraId[i] = "-1";
                    continue;
                }
                if(i == getMainCameraId()) {
                    mBayerCameraRegion = characteristics.get(CameraCharacteristics
                            .SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    mMainCameraCharacteristics = characteristics;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                mCameraId[i] = cameraId;
                if (isClearSightOn()) {
                    if(i == getMainCameraId()) {
                        ClearSightImageProcessor.getInstance().init(map, mActivity,
                                mOnMediaSavedListener);
                        ClearSightImageProcessor.getInstance().setCallback(this);
                    }
                } else {
                    if ((imageFormat == ImageFormat.YUV_420_888 || imageFormat == ImageFormat.PRIVATE)
                            && i == getMainCameraId()) {
                        if (isMultiResolutionImageReaderEnabled()) {
                            String input = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESREPROCESS_INPUT);
                            int format = imageFormat;
                            if(input != null) {
                                format = Integer.parseInt(input);
                            }
                            initReprocessMultiImageReader(format);
                        } else {
                            initRepocessImageReader(imageFormat);
                        }
                    } else if (i == getMainCameraId()) {
                        for (int y = 0; y< mYUVCount; y++) {
                            mYUVImageReader[y] = ImageReader.newInstance(mYUVsize[y].getWidth(),mYUVsize[y].getHeight(),
                                    ImageFormat.YUV_420_888,3);
                        }
                        for (int y =0; y<mRawCount; y++) {
                            mRAWImageReader[y] = ImageReader.newInstance(mRawSize[y].getWidth(),mRawSize[y].getHeight(),mSettingsManager.getRawFormat(),3);
                        }
                        ImageAvailableListener listener = new ImageAvailableListener(i) {
                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                if(mSnapshotLatency != 0) {
                                    mSnapshotLatency = System.currentTimeMillis() - mSnapshotLatency;
                                    if(mActivity.getPerformenceTest()){
                                        mHasMapTimes.put("capture->onImageAvailable",mSnapshotLatency);
                                    }
                                }
                                if(mPerformanceDebugEnable != null && mPerformanceDebugEnable.equals("on")){
                                    updatePerformanceDebugValue(6, Long.toString(mSnapshotLatency));
                                    updatePerformanceDebugValue(7, Long.toString(mShutterLag));
                                }
                                if (captureWaitImageReceive()) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, "image available for cam enable shutter button " );
                                            mUI.enableShutter(true);
                                        }
                                    });
                                }

                                Image image = reader.acquireNextImage();
                                Log.i(TAG, "imageFormat is " + image.getFormat());
                                if ((mLongshotActive || mNumFramesArrived.get() > 0)) {
                                    Log.d(TAG, "long shot image available num " + mNumImageArrived.get());
                                    if (mNumImageArrived.get() < mShotNum &&
                                            mActivity.getMediaSaveService().isQueueFull()) {
                                        Log.w(TAG, "long shot image available, but queue is full");
                                        image.close();
                                        return;
                                    }
                                    mNumImageArrived.incrementAndGet();
                                    if (mNumImageArrived.get() > mShotNum) {
                                        image.close();
                                        Log.i(TAG, "long shot image available, image arrived over limit mNumFramesArrived.get()" +
                                                "="+mNumFramesArrived.get());
                                        if (mLongshotActive && mNumFramesArrived.get() >= mShotNum) {
                                            mLongshotActive = false;
                                            mHandler.post(() -> {
                                                mUI.enableVideo(true);
                                                stopBurstShot();
                                            });
                                        }
                                        return;
                                    }
                                }
                                if(mMultiResReprocessEnabled){
                                    waitForRawMetaData();
                                    Log.i(TAG, "start reprocess-image");
                                    mPostProcessor.setMultiReader(reader);
                                    mPostProcessor.reprocessImage(image, mRawInputMeta);
                                    image.close();
                                    return;
                                }
                                if (isMpoOn()) {
                                    mMpoSaveHandler.obtainMessage(
                                            MpoSaveHandler.MSG_NEW_IMG, mCamId, 0, image).sendToTarget();
                                } else {
                                    mCaptureStartTime = System.currentTimeMillis();
                                    mNamedImages.nameNewImage(mCaptureStartTime);
                                    NamedEntity name = mNamedImages.getNextNameEntity();
                                    String title = (name == null) ? null : name.title;
                                    if(image.getFormat() == ImageFormat.YCBCR_P010) {
                                        long timeStamp = image.getTimestamp();
                                        title = String.valueOf(timeStamp);
                                    }
                                    long date = (name == null) ? -1 : name.date;
                                    byte[] bytes = getJpegData(image);
                                    int orientation = 0;
                                    ExifInterface exif = null;
                                    orientation = CameraUtil.getJpegRotation(getMainCameraId(), mOrientation);
                                    try {
                                        exif = new ExifInterface(new ByteArrayInputStream(bytes));
                                    } catch (IOException e) {
                                        Log.w(TAG,"get exif failed");
                                    }
                                    long imglen = bytes.length;
                                    int imageFormat = image.getFormat();
                                    int imageWidth = image.getWidth();
                                    int imageHeight = image.getHeight();
                                    Log.i(TAG,"mLongshotActive="+mLongshotActive+",titile="+title+
                                            ",mNumImageArrived.get()="+mNumImageArrived.get()+
                                            ",imageWidth="+imageWidth+",imageHeight="+imageHeight+",imageFormat="+imageFormat
                                            +",mImagExif="+exif+",mImgType="+mImgType);
                                    if(PersistUtil.isFuncTestRunning() && mImagExif != null && mLongImgTitle!= null) {
                                        mImagExif.add(exif);
                                        mLongImgTitle.add(title);
                                    }
                                    if(mPostProcessor.isJPEGC2PAEnabled()){
                                        mPostProcessor.reprocessC2PAImage(title, bytes, image.getWidth(), image.getHeight(), date, orientation,
                                                mOnMediaSavedListener, mContentResolver);
                                        image.close();
                                        return;
                                    }
                                    if (image.getFormat() == ImageFormat.RAW10 || image.getFormat() == ImageFormat.RAW_SENSOR) {
                                        saveRawImg(bytes, image, name, title);
                                        if (mRawReprocessType != 0) {
                                            waitForRawMetaData();
                                            Log.i(TAG, "start reprocess-image");
                                            if(mSettingsManager.getRawReprocessPhysicalId() != null && !mSettingsManager.getRawReprocessPhysicalId().equals("logical")){
                                                String physicalId = mSettingsManager.getRawReprocessPhysicalId();
                                                TotalCaptureResult physicalMetaData = mRawInputMeta.getPhysicalCameraTotalResults().get(physicalId);
                                                mPostProcessor.reprocessImage(image, physicalMetaData);
                                            } else {
                                                mPostProcessor.reprocessImage(image, mRawInputMeta);
                                            }
                                            image.close();
                                        }

                                    } else if (image.getFormat() == ImageFormat.YUV_420_888) {
                                        Log.d(TAG,"YUV buffer received from camera id =" + mCameraId);
                                        image.close();
                                    } else if (image.getFormat() == ImageFormat.YCBCR_P010) {
                                        byte[] yuv = getYUV10BitFromImage(image);
                                        Log.d(TAG,"YUV10bit received from camera format =" +
                                                image.getFormat() + ", title :" + title + ".yuv");
                                        if(PersistUtil.isFuncTestRunning()) {
                                            mImgType.add("yuv");
                                        }
                                        mActivity.getMediaSaveService().addRawImage(yuv,
                                                title,"yuv");
                                        image.close();
                                    } else {
                                        if (exif != null) {
                                            orientation = CameraUtil.getOrientation(exif);
                                        }
                                        if (mIntentMode != CaptureModule.INTENT_MODE_NORMAL &&
                                                mIntentMode != INTENT_MODE_STILL_IMAGE_CAMERA) {
                                            mJpegImageData = bytes;
                                            if (!mQuickCapture) {
                                                showCapturedReview(bytes, orientation);
                                            } else {
                                                onCaptureDone();
                                            }
                                        } else {
                                            String pictureFormat = "jpeg";
                                            if (image.getFormat() == ImageFormat.HEIC) {
                                                pictureFormat = "heic";
                                            }
                                            if(PersistUtil.isFuncTestRunning()) {
                                                mImgType.add(pictureFormat);
                                            }

                                            mActivity.getMediaSaveService().addImage(bytes, title, date,
                                                    null, image.getWidth(), image.getHeight(), orientation, null,
                                                    mOnMediaSavedListener, mContentResolver,pictureFormat);

                                            if (mLongshotActive) {
                                                mLastJpegData = bytes;
                                            } else {
                                                //if (imageFormat != ImageFormat.HEIC){
                                                    mActivity.updateThumbnail(bytes);
                                                //}
                                            }
                                        }
                                        image.close();
                                    }
                                }
                            }
                        };
                        if (isMultiResolutionImageReaderEnabled()) {
                            if(mMultiResReprocessEnabled){
                                String input = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESREPROCESS_INPUT);
                                int format = imageFormat;
                                if(input != null) {
                                    format = Integer.parseInt(input);
                                }
                                Log.i(TAG,"init for multi res reader for format:" + format);
                                initReprocessMultiImageReader(format);
                            }else{
                                mMultiResImageReader = initOutputMultiImageReader(imageFormat);
                            }
                        } else {
                            mImageReader[i] = ImageReader.newInstance(mPictureSize.getWidth(),
                                    mPictureSize.getHeight(), imageFormat, MAX_IMAGEREADERS);
                        }
                        if (isMultiResolutionImageReaderEnabled()) {
                            mMultiResImageReader.setOnImageAvailableListener(listener,
                                    new HandlerExecutor(mImageAvailableHandler));
                        } else {
                            mImageReader[i].setOnImageAvailableListener(listener, mImageAvailableHandler);
                        }
                        if (mRawReprocessType != 0){
                            for (int y=0; y< mRawCount ;y++){
                                mRAWImageReader[y].setOnImageAvailableListener(listener, mImageAvailableHandler);
                            }
                            if(mRawReprocessType == 2 || mRawReprocessType == 3 || mRawReprocessType == 5){
                                Log.i(TAG,"set reprocess image yuv/jpeg/heic:" + mImageReader[i].getImageFormat());
                                mPostProcessor.onImageReaderReady(mImageReader[i], mSupportedMaxPictureSize, mPictureSize);
                            } else {
                                mPostProcessor.onImageReaderReady(mYUVImageReader[0], mSupportedMaxPictureSize, mPictureSize);
                            }
                        }

                        if(isAIDE2Enabled()){
                            Set<String> physical_ids = mSettingsManager.getAllPhysicalCameraId();
                            if(physical_ids != null && physical_ids.size() != 0){
                                Iterator<String> iterator = physical_ids.iterator();
                                for (String id : physical_ids){
                                    final String pyhsicalId = iterator.next();
                                    Size fullYuvSize = getFullYUVSize(Integer.parseInt(pyhsicalId));
                                    Log.i(TAG,"create aide images, id:" + id + ",:fullYuvSize:" + fullYuvSize.toString());
                                    mAideFullImageReader[getIndexByPhysicalId(id)] = ImageReader.newInstance(fullYuvSize.getWidth(),fullYuvSize.getHeight(),ImageFormat.YUV_420_888,MAX_IMAGEREADERS);
                                    mAideFullImageReader[getIndexByPhysicalId(id)].setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                                        @Override
                                        public void onImageAvailable(ImageReader reader) {
                                            Log.d(TAG,"new aide full image from physical id="+pyhsicalId);
                                            if(mAideActiveCameraIds.get(Integer.valueOf(pyhsicalId))){
                                                mAideFullImage = reader.acquireNextImage();
                                                byte[] yuv = getYUVFromImage(mAideFullImage);
                                                mActivity.getMediaSaveService().addRawImage(yuv,"fullyuv","yuv");
                                            }
                                            mActivity.getAIDenoiserService().increment();
                                        }
                                    }, mImageAvailableHandler);
                                    mAideDs4ImageReader[getIndexByPhysicalId(id)] = ImageReader.newInstance(getDsxYUVSize(fullYuvSize).getWidth(),getDsxYUVSize(fullYuvSize).getHeight(),ImageFormat.YUV_420_888,MAX_IMAGEREADERS);
                                    mAideDs4ImageReader[getIndexByPhysicalId(id)].setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                                        @Override
                                        public void onImageAvailable(ImageReader reader) {
                                            Log.d(TAG,"new aide ds4 image from physical id="+pyhsicalId);
                                            mAideDownImage = reader.acquireNextImage();
                                            byte[] yuv = getYUVFromImage(mAideDownImage);
                                            mActivity.getMediaSaveService().addRawImage(yuv,"dsyuv","yuv");
                                            mActivity.getAIDenoiserService().increment();
                                        }
                                    }, mImageAvailableHandler);
                                }
                            }else {
                                Size fullYuvSize = getFullYUVSize(getMainCameraId());
                                Log.i(TAG,"create aide images for single, id:" + getMainCameraId() + ",:fullYuvSize:" + fullYuvSize.toString());
                                mAideFullImageReader[getMainCameraId()] = ImageReader.newInstance(fullYuvSize.getWidth(),fullYuvSize.getHeight(),ImageFormat.YUV_420_888,MAX_IMAGEREADERS);
                                mAideFullImageReader[getMainCameraId()].setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                                    @Override
                                    public void onImageAvailable(ImageReader reader) {
                                        Log.d(TAG,"new aide full image ");
                                        mAideFullImage = reader.acquireNextImage();
                                        byte[] yuv = getYUVFromImage(mAideFullImage);
                                        mActivity.getMediaSaveService().addRawImage(yuv,"fullyuv","yuv");
                                        mActivity.getAIDenoiserService().increment();
                                    }
                                }, mImageAvailableHandler);
                                mAideDs4ImageReader[getMainCameraId()] = ImageReader.newInstance(getDsxYUVSize(fullYuvSize).getWidth(),getDsxYUVSize(fullYuvSize).getHeight(),ImageFormat.YUV_420_888,MAX_IMAGEREADERS);
                                mAideDs4ImageReader[getMainCameraId()].setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                                    @Override
                                    public void onImageAvailable(ImageReader reader) {
                                        Log.d(TAG,"new aide ds4 image");
                                        mAideDownImage = reader.acquireNextImage();
                                        byte[] yuv = getYUVFromImage(mAideDownImage);
                                        mActivity.getMediaSaveService().addRawImage(yuv,"dsyuv","yuv");
                                        mActivity.getAIDenoiserService().increment();
                                    }
                                }, mImageAvailableHandler);
                            }
                        }
                        if (mSaveRaw) {
                            mRawImageReader[i] = ImageReader.newInstance(mSupportedRawPictureSize.getWidth(),
                                    mSupportedRawPictureSize.getHeight(), mSettingsManager.getRawFormat(), MAX_IMAGEREADERS);
                            mRawImageReader[i].setOnImageAvailableListener(listener, mImageAvailableHandler);
                        }
                        if (mYUV10bit || mYUV10BitWithMetadata) {
                            Size[] yuvSizes = mSettingsManager.getSupportedOutputSize(getMainCameraId(), ImageFormat.YCBCR_P010);
                            List<Size> yuvSizeList = Arrays.asList(yuvSizes);
                            yuvSizeList.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
                            if(mSettingsManager.getQuadBayerSensorPrefEnabled()){
                                yuvSizeList = mSettingsManager.getSupportedQCFAMaxPictureSizeList(String.valueOf(getMainCameraId()), ImageFormat.YCBCR_P010);
                                yuvSizeList.sort((o1,o2)->o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
                            }
                            Log.d(TAG, "create mYUV10bitImageReader yuv size : " +
                                    yuvSizeList.get(0).getWidth() + " x " +
                                    yuvSizeList.get(0).getHeight());
                            mYUV10bitImageReader[i] = ImageReader.newInstance(yuvSizeList.get(0).getWidth(),
                                    yuvSizeList.get(0).getHeight(), ImageFormat.YCBCR_P010, MAX_IMAGEREADERS);
                            mYUV10bitImageReader[i].setOnImageAvailableListener(listener, mImageAvailableHandler);
                            mYUVP010Size = yuvSizeList.get(0);
                        }
                    }
                }
            }
            setUpPhysicalOutput();
            mAutoFocusRegionSupported = mSettingsManager.isAutoFocusRegionSupported(getMainCameraId());
            mAutoExposureRegionSupported = mSettingsManager.isAutoExposureRegionSupported(getMainCameraId());
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        }
    }

    private boolean isPhysicalRaw() {
        if (switchedCameraId || getMainCameraId() == 1 || isSingleCameraMode() || mSettingsManager.getQuadBayerSensorPrefEnabled())  {
            return true;
        } else {
            return false;
        }
    }
    public int setInfoForDng(TotalCaptureResult mRawMeta){
        try{
            CameraCharacteristics characteristics;
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            String activeCameraId = String.valueOf(getMainCameraId());
            if(isPhysicalRaw()) {
                String qcfaId = mSettingsManager.getQuadBayerPhysicalId(Integer.toString(getMainCameraId()));
                if(qcfaId != null) activeCameraId = qcfaId;
            } else{
                activeCameraId = String.valueOf(mActiveCameraIds.get(0));
            }
            characteristics= manager.getCameraCharacteristics(activeCameraId);
            Log.d(TAG,"setInfoForDng mRawMeta="+mRawMeta+",characteristics="+characteristics);
            mActivity.getMediaSaveService().setCharacteristics(characteristics);
            mActivity.getMediaSaveService().setResult(mRawMeta);
        } catch (CameraAccessException | IllegalArgumentException | NullPointerException e) {
           Log.e(TAG,e.toString());
            return -1;
        }
        return 0;
    }
    private TotalCaptureResult waitForRawMetaData() {
        int timeout = 10; //500ms
        while(timeout > 0) {
            if (mRawInputMeta != null) {
                    return mRawInputMeta;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            timeout--;
        }
       return null;
    }
    private Size getDsxYUVSize(Size fullSize){
        Size dsxYuvSize;
        float a = fullSize.getWidth()/1008;
        float b = fullSize.getHeight()/756;
        float factor = a >b ? a : b;
        if(a>b){
            dsxYuvSize = new Size(1008, 566);
        }else if (a < b){
            dsxYuvSize = new Size(756, 756);
        }else {
           dsxYuvSize = new Size(1008, 756);
        }
        Log.i(TAG,"dsxYuvSize:" + dsxYuvSize.toString());
        return dsxYuvSize;
    }

    private boolean waitForAudioPrepare() {
        int timeout = 10; //500ms
        while(timeout > 0) {
            if (mOutputFileInit && mAudioCodecInit && mAudioRecorderInit) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            timeout--;
        }
        return false;
    }

    private List<OutputConfiguration> getPhysicalPreviewOutput(){
        List<OutputConfiguration> ret = new ArrayList<>();
        int i=1;
        List<Surface> previewSurfaces = mUI.getPhysicalSurfaces();
        for (String physical : mSettingsManager.getPhysicalCameraId()){
            OutputConfiguration outputConfiguration =
                    new OutputConfiguration(previewSurfaces.get(i));
            outputConfiguration.setPhysicalCameraId(physical);
            ret.add(outputConfiguration);
            Log.d(TAG,"add preview for physical camera ="+physical);
            i++;
        }
        return ret;
    }


    public int getIndexByPhysicalId(String id){
        Set<String> ids = mSettingsManager.getAllPhysicalCameraId();
        int index = 0;
        int ret = -1;
        Iterator<String> iterator = ids.iterator();
        while (index < ids.size()){
            if(id.equals(iterator.next())){
                ret = index;
                break;
            }
            index++;
        }
        Log.d(TAG,"getIndexByPhysicalId ids="+ids.toString()+" id="+id+" ret="+ret);
        return ret;
    }

    private void cleanPhysicalImageReaders() {
        for (int i= 0; i < PHYSICAL_CAMERA_COUNT; i++){
            mPhysicalJpegReader[i] = null;
        }

        for (int i=0; i< MAX_LOGICAL_PHYSICAL_CAMERA_COUNT; i++) {
            mPhysicalYuvReader[i] = null;
            mPhysicalRawReader[i] = null;
            mPhysicalJpegRReader[i] = null;
        }
    }


    private void setUpPhysicalOutput() {
        if (!mSettingsManager.isMultiCameraEnabled()&& !mSaveRaw)
            return;
        cleanPhysicalImageReaders();
        Set<String> jpeg_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
        if (jpeg_ids != null) {
            int i = 0;
            for (String id : jpeg_ids) {
                int index = getIndexByPhysicalId(id);
                Size size;
                if (index != -1) {
                    size = mPhysicalSizes[index];
                } else {
                    size = mPictureSize;
                }
                setPhysicalJpegImgReader(size,id,i);
                i++;
            }
        }

        setPhysicalJpegRReader();

        Set<String> yuv_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
        if (yuv_ids != null) {
            int i = 0;
            for (String id : yuv_ids) {
                int index = getIndexByPhysicalId(id);
                Size size;
                if (index != -1) {
                    size = mPhysicalSizes[index];
                } else {
                    size = mPictureSize;
                }
                mPhysicalYuvReader[i] = ImageReader.newInstance(size.getWidth(),
                        size.getHeight(), ImageFormat.YUV_420_888, 3);
                Log.d(TAG, "YUV imageReader i=" + i + " id=" + id + " index=" + index +
                        " size=" + size.toString());
                PhysicalImageListener yuvListener = new PhysicalImageListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.d(TAG, "new yuv image from physical camera " + id);
                        releaseShutterButton();
                        Image image = reader.acquireNextImage();
                        byte[] yuv = getYUVFromImage(image);
                        mNamedImages.nameNewImage(System.currentTimeMillis());
                        NamedEntity name = mNamedImages.getNextNameEntity();
                        String title = (name == null) ? null : name.title;
                        title = title + "_phy_" + id;
                        long date = (name == null) ? -1 : name.date;
                        mActivity.getMediaSaveService().addRawImage(yuv, title, "yuv");
                        image.close();
                    }
                };
                yuvListener.setCamId(id);
                mPhysicalYuvReader[i].setOnImageAvailableListener(yuvListener, mImageAvailableHandler);
                i++;
            }
        }

        Set<String> yuv10bit_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
        if (yuv10bit_ids != null){
            int i =0;
            for (String id:yuv10bit_ids){
                int index = getIndexByPhysicalId(id);
                Size size;
                if (index != -1){
                    size = mPhysicalSizes[index];
                } else {
                    size = mPictureSize;
                }
                mPhysicalYuv10bitReader[i] = ImageReader.newInstance(size.getWidth(),
                        size.getHeight(),ImageFormat.YCBCR_P010,3);
                Log.d(TAG,"YUV 10bit imageReader i="+i+" id="+id+" index="+index+
                        " size="+size.toString());
                PhysicalImageListener yuv10bitListener = new PhysicalImageListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.d(TAG, "new yuv 10bit image from physical camera "+id);
                        releaseShutterButton();
                        Image image = reader.acquireNextImage();
                        byte[] yuv = getYUV10BitFromImage(image);
                        mNamedImages.nameNewImage(System.currentTimeMillis());
                        NamedEntity name = mNamedImages.getNextNameEntity();
                        String title = (name == null) ? null : name.title;
                        title = title+"_phy_10bit_"+id;
                        long date = (name == null) ? -1 : name.date;
                        mActivity.getMediaSaveService().addRawImage(yuv,title,"yuv");
                        image.close();
                    }
                };
                yuv10bitListener.setCamId(id);
                mPhysicalYuv10bitReader[i].setOnImageAvailableListener(yuv10bitListener,mImageAvailableHandler);
                i++;
            }
        }

        Set<String> raw_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
        if (raw_ids != null) {
            int i = 0;
            for (String id : raw_ids) {
                int index = getIndexByPhysicalId(id);
                Size size;
                if (index != -1) {
                    size = mPhysicalRawSizes[index];
                    if(mSettingsManager.isMcxQcfaMode()){
                        List<Size> allSize = mSettingsManager.getSupportedQCFAMaxPictureSizeList(id, ImageFormat.RAW10);
                        allSize.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
                        if(allSize.size() != 0){
                            size = allSize.get(0);
                        }
                    }
                } else {
                    size = mSupportedRawPictureSize;
                }
                setPhysicalImgReader(size,id,i);
                Log.d(TAG,"raw imageReader i="+i+" id="+id+" index="+index+ " size="+size.toString());
                i++;
            }
        } else if (mSaveRaw && mRawReprocessType == 0) {
            for (int i = 0; i < mPhysicalRawSizes.length; i++) {
                Size size = mPhysicalRawSizes[i];
                String id = mPhysicalRawId[i];
                if (size != null) setPhysicalImgReader(size, id, i);
            }
            for(int i =0 ;i < mPhysicalSizes.length; i++){
                Size size = mPhysicalSizes[i];
                String id = mPhysicalRawId[i];
                if (size != null) setPhysicalJpegImgReader(size, id, i);
            }
        }
    }

    private void releaseShutterButton(){
        if (captureWaitImageReceive()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "image available for cam enable shutter button " );
                    mUI.enableShutter(true);
                }
            });
        }
    }

    private void setPhysicalImgReader(Size size, String id, int i) {
        if(mSaveRaw)
        mPhysicalRawReader[i] = ImageReader.newInstance(size.getWidth(),
                size.getHeight(), mSettingsManager.getRawFormat(), MAX_IMAGEREADERS + 2);
        else mPhysicalRawReader[i] = ImageReader.newInstance(size.getWidth(),
                size.getHeight(), ImageFormat.RAW10, MAX_IMAGEREADERS + 2);
        PhysicalImageListener rawListener = new PhysicalImageListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                releaseShutterButton();
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] raw = new byte[buffer.remaining()];
                buffer.get(raw);
                mNamedImages.nameNewImage(System.currentTimeMillis());
                NamedEntity name = mNamedImages.getNextNameEntity();
                String title = (name == null) ? null : name.title;
                title = title + "_phy_" + id;
                if(PersistUtil.isFuncTestRunning() && mImagExif != null && mLongImgTitle!= null){
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(new ByteArrayInputStream(raw));
                    } catch (IOException e) {
                        Log.w(TAG,"get exif failed");
                    }
                    mImagExif.add(exif);
                    mLongImgTitle.add(title);
                }


                saveRawImg(raw, image, name, title);
            }
        };
        rawListener.setCamId(id);
        mPhysicalRawReader[i].setOnImageAvailableListener(rawListener, mImageAvailableHandler);
    }

    private void setPhysicalJpegRReader(){
        Set<String> jpegR_ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
        if (jpegR_ids != null) {
            int i = 0;
            for (String id : jpegR_ids) {
                int index = getIndexByPhysicalId(id);
                Size size;
                if (index != -1) {
                    size = mPhysicalJpegRSizes[index];
                } else {
                    size = mPictureSize;
                }
                mPhysicalJpegRReader[i] = ImageReader.newInstance(size.getWidth(),
                        size.getHeight(), ImageFormat.JPEG_R, 3);
                Log.d(TAG, "JPEG R imageReader i=" + i + " id=" + id + " index=" + index +
                        " size=" + size.toString());

                PhysicalImageListener jpegRListener = new PhysicalImageListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.d(TAG, "new jpeg R image from physical camera " + id);
                        releaseShutterButton();
                        Image image = reader.acquireNextImage();
                        mNamedImages.nameNewImage(System.currentTimeMillis());
                        NamedEntity name = mNamedImages.getNextNameEntity();
                        String title = (name == null) ? null : name.title;
                        title = title + "_phy_" + id;
                        long date = (name == null) ? -1 : name.date;

                        byte[] bytes = getJpegData(image);
                        int orientation = 0;
                        ExifInterface exif = null;
                        try {
                            exif = new ExifInterface(new ByteArrayInputStream(bytes));
                        } catch (IOException e) {
                            Log.w(TAG,"get exif failed");
                        }
                        if (image.getFormat() != ImageFormat.HEIC && exif != null) {
                            orientation = CameraUtil.getOrientation(exif);
                        } else {
                            orientation = CameraUtil.getJpegRotation(getMainCameraId(), mOrientation);
                        }

                        mActivity.getMediaSaveService().addImage(bytes, title, date,
                                null, image.getWidth(), image.getHeight(), orientation, null,
                                mOnMediaSavedListener, mContentResolver, "jpeg");
                        image.close();
                    }
                };
                jpegRListener.setCamId(id);
                mPhysicalJpegRReader[i].setOnImageAvailableListener(jpegRListener, mImageAvailableHandler);
                i++;
            }
        }
    }

    private void setPhysicalJpegImgReader(Size size, String id, int i){
        mPhysicalJpegReader[i] = ImageReader.newInstance(size.getWidth(),
                size.getHeight(), ImageFormat.JPEG, 3);
        Log.d(TAG, "setJpegImgReader i=" + i + " id=" + id +
                " size=" + size.toString());
        PhysicalImageListener jpegListener = new PhysicalImageListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "new jpeg image from physical camera " + id);
                releaseShutterButton();
                Image image = reader.acquireNextImage();
                mNamedImages.nameNewImage(System.currentTimeMillis());
                NamedEntity name = mNamedImages.getNextNameEntity();
                String title = (name == null) ? null : name.title;
                title = title + "_phy_" + id;
                long date = (name == null) ? -1 : name.date;

                byte[] bytes = getJpegData(image);
                int orientation = 0;
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(new ByteArrayInputStream(bytes));
                } catch (IOException e) {
                    Log.w(TAG,"get exif failed");
                }
                if (image.getFormat() != ImageFormat.HEIC && exif != null) {
                    orientation = CameraUtil.getOrientation(exif);
                } else {
                    orientation = CameraUtil.getJpegRotation(getMainCameraId(), mOrientation);
                }
                if(PersistUtil.isFuncTestRunning() && mImagExif != null && mLongImgTitle!= null){
                    mImagExif.add(exif);
                    mLongImgTitle.add(title);
                    mImgType.add("jpeg");
                }
                mActivity.getMediaSaveService().addImage(bytes, title, date,
                        null, image.getWidth(), image.getHeight(), orientation, null,
                        mOnMediaSavedListener, mContentResolver, "jpeg");
                image.close();
            }
        };
        jpegListener.setCamId(id);
        mPhysicalJpegReader[i].setOnImageAvailableListener(jpegListener, mImageAvailableHandler);
    }

    public static byte[] getYUVFromImage(Image image) {
        try{
            int height = image.getHeight();
            int stride = image.getPlanes()[0].getRowStride();
            ByteBuffer dataY= image.getPlanes()[0].getBuffer();
            ByteBuffer dataUV = image.getPlanes()[2].getBuffer();
            dataY.rewind();
            dataUV.rewind();
            byte[] bytesY = new byte[dataY.remaining()];
            dataY.get(bytesY);
            byte[] bytesUV = new byte[dataUV.remaining()];
            dataUV.get(bytesUV);
            byte[] data = new byte[stride*height*3/2];
            System.arraycopy(bytesY,0,data,0,bytesY.length);
            System.arraycopy(bytesUV,0,data,stride*height,bytesUV.length);
            return data;
        }catch (IllegalStateException e) {
            return null;
        }
    }

    private byte[] getYUV10BitFromImage(Image image) {
        try{
            int height = image.getHeight();
            int stride = image.getPlanes()[0].getRowStride();
            ByteBuffer dataY= image.getPlanes()[0].getBuffer();
            ByteBuffer dataUV = image.getPlanes()[1].getBuffer();
            dataY.rewind();
            dataUV.rewind();
            byte[] bytesY = new byte[dataY.remaining()];
            dataY.get(bytesY);
            byte[] bytesUV = new byte[dataUV.remaining()];
            dataUV.get(bytesUV);
            byte[] data = new byte[stride*height*3/2];
            System.arraycopy(bytesY,0,data,0,bytesY.length);
            System.arraycopy(bytesUV,0,data,stride*height,bytesUV.length);
            return data;
        }catch (IllegalStateException e) {
            return null;
        }
    }

    public static HeifWriter createHEIFEncoder(String path, int width, int height,
                                               int orientation, int imageCount, int quality) {
        HeifWriter heifWriter = null;
        try {
            HeifWriter.Builder builder =
                    new HeifWriter.Builder(path, width, height, HeifWriter.INPUT_MODE_SURFACE);
            builder.setQuality(quality);
            builder.setMaxImages(imageCount);
            builder.setPrimaryIndex(0);
            builder.setRotation(orientation);
            builder.setGridEnabled(true);
            heifWriter = builder.build();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG,e.toString());
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        return heifWriter;
    }

    private void createPhysicalVideoSnapshotImageReader(){
        if (mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_CAMCORDER) != null){
            Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER);
            int count = ids.size();
            Iterator<String> iterator = ids.iterator();
            for (int i =0;i<count;i++){
                mPhysicalSnapshotImageReaders[i] = ImageReader.newInstance(
                        mPhysicalVideoSnapshotSizes[i].getWidth(),
                        mPhysicalVideoSnapshotSizes[i].getHeight(),
                        ImageFormat.JPEG, 2);
                final String id = iterator.next();
                mPhysicalSnapshotImageReaders[i].setOnImageAvailableListener(
                        new ImageReader.OnImageAvailableListener() {
                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                Log.d(TAG,"new live shot image from physical id="+id);
                                Image image = reader.acquireNextImage();
                                mCaptureStartTime = System.currentTimeMillis();
                                mNamedImages.nameNewImage(mCaptureStartTime);
                                NamedEntity name = mNamedImages.getNextNameEntity();
                                String title = (name == null) ? null : name.title;
                                title = title + "_phy_"+ id;
                                long date = (name == null) ? -1 : name.date;
                                byte[] bytes = getJpegData(image);
                                int orientation = 0;
                                ExifInterface exif = null;
                                try {
                                    exif = new ExifInterface(new ByteArrayInputStream(bytes));
                                } catch (IOException e) {
                                    Log.w(TAG,"get exif failed");
                                }
                                if (image.getFormat() != ImageFormat.HEIC && exif != null){
                                    orientation = CameraUtil.getOrientation(exif);
                                } else {
                                    orientation = CameraUtil.getJpegRotation(
                                            getMainCameraId(),mOrientation);
                                }
                                mActivity.getMediaSaveService().addImage(bytes, title, date,
                                        null, image.getWidth(), image.getHeight(), orientation, null,
                                        mOnMediaSavedListener, mContentResolver, "jpeg");
                                image.close();
                            }
                        }, mImageAvailableHandler);
            }
        }
    }

    private int addPhysicalVideoCaptureTarget(CaptureRequest.Builder builder){
        if (mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_CAMCORDER) != null){
            int count = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER).size();
            int ret = 0;
            int nums = PersistUtil.getPhysicalLiveShotNum();
            if (nums > 0 && nums <= count) {
                count = nums;
            }
            for (int i =0;i<count;i++){
                if (mPhysicalSnapshotImageReaders[i] != null){
                    builder.addTarget(mPhysicalSnapshotImageReaders[i].getSurface());
                    ret++;
                }
            }
            return ret;
        }
        return 0;
    }

    private void createVideoSnapshotImageReader() {
        if (mVideoSnapshotImageReader != null) {
            mVideoSnapshotImageReader.close();
        }
        if (mSettingsManager.isHeifWriterEncoding()) {
            String tmpPath = mActivity.getCacheDir().getPath() + "/" + "liveshot_heif.tmp";
            mLiveShotInitHeifWriter = createHEIFEncoder(tmpPath,mVideoSize.getWidth(),
                    mVideoSize.getHeight(),0, 1,85);
            return;
        }
        int format = ImageFormat.JPEG;
        if (mSettingsManager.isHeifHALEncoding()) {
            format = ImageFormat.HEIC;
        }else if(mSettingsManager.getSavePictureFormat() == mSettingsManager.JPEG_R_FORMAT){
            format = ImageFormat.JPEG_R;
        }
        mVideoSnapshotImageReader = ImageReader.newInstance(mVideoSnapshotSize.getWidth(),
                mVideoSnapshotSize.getHeight(),format, 2);
        mVideoSnapshotImageReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        if(mSnapshotLatency != 0) {
                            mSnapshotLatency = System.currentTimeMillis() - mSnapshotLatency;
                        }
                        if(mPerformanceDebugEnable != null && mPerformanceDebugEnable.equals("on")){
                            updatePerformanceDebugValue(6, Long.toString(mSnapshotLatency));
                            updatePerformanceDebugValue(7, Long.toString(mShutterLag));
                        }
                        Image image = reader.acquireNextImage();
                        mCaptureStartTime = System.currentTimeMillis();
                        mNamedImages.nameNewImage(mCaptureStartTime);
                        NamedEntity name = mNamedImages.getNextNameEntity();
                        String title = (name == null) ? null : name.title;
                        long date = (name == null) ? -1 : name.date;

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);

                        int orientation = 0;
                        ExifInterface exif = null;
                        try {
                            exif = new ExifInterface(new ByteArrayInputStream(bytes));
                        } catch (IOException e) {
                            Log.w(TAG,"get exif failed");
                        }

                        if (image.getFormat() != ImageFormat.HEIC && exif != null){
                            orientation = CameraUtil.getOrientation(exif);
                        } else {
                            orientation = CameraUtil.getJpegRotation(getMainCameraId(),mOrientation);
                        }

                        String saveFormat = image.getFormat() == ImageFormat.HEIC? "heic" : "jpeg";
                        if(PersistUtil.isFuncTestRunning() && mImagExif != null && mLongImgTitle!= null) {;
                            mLongImgTitle.add(title);
                            mImgType.add(saveFormat);
                            mImagExif.add(exif);
                        }
                        mActivity.getMediaSaveService().addImage(bytes, title, date,
                                null, image.getWidth(), image.getHeight(), orientation, null,
                                mOnMediaSavedListener, mContentResolver, saveFormat);
                        mActivity.updateThumbnail(bytes);
                        image.close();
                    }
                }, mImageAvailableHandler);
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    public void unlockFocus(int id) {
        Log.i(TAG, "unlockFocus " + id );
        if(isLongExpTmCaptrure()) {
            mIsLongExpTmCp = false;
            mUI.stopShutterAnim();
        }
        isFlashRequiredInDriver = false;
        if (!checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id]) || mCurrentSceneMode.mode == CameraMode.VIDEO) {
            return;
        }
        if (mState[id] == STATE_WAITING_TOUCH_FOCUS) {
            mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[id]);
        }
        try {
            if (mUI.getCurrentProMode() != ProMode.MANUAL_MODE) {
                CaptureRequest.Builder builder = getRequestBuilder(id);
                setTag(builder, "" + id + "-" + getCurrenCameraMode().name());
                addPreviewSurface(builder, null, id);
                applySettingsForUnlockFocus(builder, id);
                if (mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
                    applySettingsForLockExposure(builder, id);
                }
                // Set EIS vendor tag back to 0 after snapshot request
                if (isMFNREnabled()) {
                    builder.set(custom_noise_reduction, (byte) 0x00);
                }
                if (mCaptureSession[id] instanceof CameraConstrainedHighSpeedCaptureSession) {
                    List requestList = getHighSpeedList ((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,
                            builder);
                    mCaptureSession[id].captureBurst(requestList, mCaptureCallback, mCameraHandler);
                } else {
                    mCaptureSession[id].capture(builder.build(), mCaptureCallback, mCameraHandler);
                }
            }

            mState[id] = STATE_PREVIEW;
            if (id == getMainCameraId()) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mUI.getCurrentProMode() != ProMode.MANUAL_MODE && mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE)
                            mUI.clearFocus();
                        if (mCurrentSceneMode.mode != CameraMode.PRO_MODE)
                            mUI.enableZoomSeekBar(true);
                    }
                });
            }
            if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                mControlAFMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                mIsAutoFocusStarted = false;
            }
            mCaptureTorchTrigger =false;
            applyFlash(mPreviewRequestBuilder[id], id);
            if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                applySettingsForUnlockExposure(mPreviewRequestBuilder[id], id);
            }
            if (isDevOptionSetting()) {
                applyCommonSettings(mPreviewRequestBuilder[id], id);
            }
            if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                int afMode = (isDevOptionSetting() && getDevAfMode() != -1) ?
                        getDevAfMode() : mControlAFMode;
                setAFModeToPreview(id, mUI.getCurrentProMode() == ProMode.MANUAL_MODE ?
                        CaptureRequest.CONTROL_AF_MODE_OFF : afMode);
            }
            mTakingPicture[id] = false;
            enableShutterAndVideoOnUiThread(id);
        } catch (NullPointerException | IllegalStateException | CameraAccessException | IllegalArgumentException e) {
            Log.w(TAG, "Session is already closed or session had been changed");
        }
    }
private boolean isDevOptionSetting(){
    return (mSettingsManager.isDeveloperEnabled() || mActivity.getDevOption());
}
    public void enableShutterButtonOnMainThread(int id) {
        if (id == getMainCameraId()) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (captureWaitImageReceive()) {
                        Log.d(TAG, "image available then enable shutter button " );
                        mUI.enableShutter(true);
                    }
                    if(isLongExpTmCaptrure()) {
                        mUI.stopShutterAnim();
                    }
                }
            });
        }
    }
   private void enableShutterAnimal(int id){
       if (id == getMainCameraId()) {
           mActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   if(isLongExpTmCaptrure()) {
                       double tmpValue = 1000000;
                       double time = mLongExpTime / tmpValue;
                       int expTime = new Double(time).intValue();
                       mUI.startShutterAnim(expTime);
                   }
               }
           });
       }
   }
    private void enableShutterAndVideoOnUiThread(int id) {
        if (id == getMainCameraId()) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUI.stopSelfieFlash();
                    if (!captureWaitImageReceive()) {

                        mUI.enableShutter(true);
                    }
                    if (mDeepPortraitMode) {
                        mUI.enableVideo(false);
                    } else {
                        mUI.enableVideo(true);
                    }
                    if(isLongExpTmCaptrure()) {
                        mUI.stopShutterAnim();
                    }
                }
            });
        }
    }

    private void enableVideoButton(boolean enable) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUI.enableVideo(enable);
            }
        });
    }

    public boolean isMFNREnabled() {
        boolean mfnrEnable = false;
        if (mSettingsManager != null && showMFNR()) {
            String mfnrValue = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
            if (mfnrValue != null) {
                mfnrEnable = mfnrValue.equals("1");
            }
        }
        return mfnrEnable;
    }


    public boolean isVariableFPSEnabled() {
        boolean variableFPSEnable = false;
        if (mSettingsManager != null) {
            String variableFPSValue = mSettingsManager.getValue(SettingsManager.KEY_VARIABLE_FPS);
            if (variableFPSValue != null && mHighSpeedCaptureRate == 60) {
                variableFPSEnable = variableFPSValue.equals("1");
            }
        }
        return variableFPSEnable;
    }

    private boolean isHDREnable() {
        boolean hdrEnable = false;
        if (mSettingsManager != null) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
            if (value != null) {
                int mode = Integer.parseInt(value);
                hdrEnable = (mode == CaptureRequest.CONTROL_SCENE_MODE_HDR);
            }
        }
        return hdrEnable;
    }

    private boolean captureWaitImageReceive() {
        return isMFNREnabled() || isHDREnable();
    }

    private Size parsePictureSize(String value) {
        int indexX = value.indexOf('x');
        int width = Integer.parseInt(value.substring(0, indexX));
        int height = Integer.parseInt(value.substring(indexX + 1));
        return new Size(width, height);
    }

    private void closeProcessors() {
        if(mPostProcessor != null) {
            mPostProcessor.onClose();
        }

        if(mFrameProcessor != null) {
            mFrameProcessor.onClose();
        }
    }

    public boolean isAllSessionClosed() {
        for (int i = MAX_NUM_CAM - 1; i >= 0; i--) {
            if (mCaptureSession[i] != null) {
                return false;
            }
        }
        return true;
    }

    private void closeSessions() {
        for (int i = MAX_NUM_CAM-1; i >= 0; i--) {
            if (null != mCaptureSession[i]) {
                if (mCamerasOpened) {
                    try {
                        if (!(mCurrentSceneMode.mode == CameraMode.HFR && isHighSpeedRateCapture())) {
                            mCaptureSession[i].capture(mPreviewRequestBuilder[i].build(), null, mCameraHandler);
                        }
                    } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
                       Log.e(TAG,e);
                    }
                }
                mCaptureSession[i].close();
                mCaptureSession[i] = null;
            }

            if (null != mImageReader[i]) {
                mImageReader[i].close();
                mImageReader[i] = null;
            }

            if (null != mRawImageReader[i]){
                mRawImageReader[i].close();
                mRawImageReader[i] = null;
            }
            uninitMultiResolutionImageReader();
        }
        closePhysicalImageReaders();
        synchronized (mDepthImageLock) {
            if (mDepthImageReader != null) {
                mDepthImageReader.close();
                mDepthImageReader = null;
            }
        }
    }

    private void closePhysicalImageReaders(){
        for (int i = mPhysicalYuvReader.length-1; i>=0 ;i--){
            if (mPhysicalYuvReader[i] != null){
                mPhysicalYuvReader[i].close();
                mPhysicalYuvReader[i] =null;
            }
        }
        for (int i = mPhysicalRawReader.length-1; i>=0 ;i--){
            if (mPhysicalRawReader[i] != null){
                mPhysicalRawReader[i].close();
                mPhysicalRawReader[i] =null;
            }
        }
    }

    private void resetAudioMute() {
        if (isAudioMute()) {
            setMute(false, true);
        }
    }

    private void closeImageReader() {
        for (int i = MAX_NUM_CAM - 1; i >= 0; i--) {
            if (null != mImageReader[i]) {
                mImageReader[i].close();
                mImageReader[i] = null;
            }
            if (null != mRawImageReader[i]){
                mRawImageReader[i].close();
                mRawImageReader[i] = null;
            }
        }
        if (null != mVideoSnapshotImageReader) {
            mVideoSnapshotImageReader.close();
            mVideoSnapshotImageReader = null;
        }

        synchronized (mDepthImageLock) {
            if (null != mDepthImageReader) {
                mDepthImageReader.close();
                mDepthImageReader = null;
            }
        }

    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        Log.i(TAG, "closeCamera");

        closeProcessors();

        /* no need to set this in the callback and handle asynchronously. This is the same
        reason as why we release the semaphore here, not in camera close callback function
        as we don't have to protect the case where camera open() gets called during camera
        close(). The low level framework/HAL handles the synchronization for open()
        happens after close() */

        try {
            // Close camera starting with AUX first
            for (int i = MAX_NUM_CAM-1; i >= 0; i--) {
                if (null != mCameraDevice[i]) {
                    if (!mCameraOpenCloseLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                        Log.d(TAG, "Time out waiting to lock camera closing.");
                        throw new RuntimeException("Time out waiting to lock camera closing");
                    }
                    Log.i(TAG, "Closing camera: " + mCameraDevice[i].getId());

                    // session was closed here if intentMode is INTENT_MODE_VIDEO
                    if (mIntentMode != INTENT_MODE_VIDEO) {
                        try {
                            if (isAbortCapturesEnable() && mCaptureSession[i] != null) {
                                mFlushLatency = System.currentTimeMillis();
                                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,abortCaptures");
                                mCaptureSession[i].abortCaptures();
                                if (TRACE_DEBUG) Trace.endSection();
                                mFlushLatency = System.currentTimeMillis() - mFlushLatency;
                                Log.d(TAG, "Closing camera call abortCaptures ");
                                if (mActivity.getPerformenceTest() ) {
                                    mHasMapTimes.put("abortCaptures", mFlushLatency);
                                }
                            }
                            if (isSendRequestAfterFlushEnable() && mCaptureSession[i] != null) {
                                Log.i(TAG, "Closing camera call setRepeatingRequest");
                                mCaptureSession[i].setRepeatingRequest(mPreviewRequestBuilder[i].build(),
                                        mCaptureCallback, mCameraHandler);
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG,e);
                        }
                    }
                    mCloseCameraLatency = System.currentTimeMillis();
                    if (mActivity.getPerformenceTest() && mStartedTime != 0) {
                        mHasMapTimes.put("switchTrigger->closeCamera", mCloseCameraLatency - mStartedTime);
                    }
                    if (TRACE_DEBUG) Trace.beginSection("SnapCamera,camera close");
                    mCameraDevice[i].close();
                    if (TRACE_DEBUG) Trace.endSection();
                    mCameraDevice[i] = null;
                    mCameraOpened[i] = false;
                    mCaptureSession[i] = null;
                }
            }

            closePhysicalImageReaders();

            mIsLinked = false;
            if (PersistUtil.enableMediaRecorder()) {
                releaseMediaRecorder();
            } else {
                stopCodecThreads();
                releaseMediaCodec();
            }
        } catch (InterruptedException e) {
            mCameraOpenCloseLock.release();
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } catch (CameraAccessException | IllegalStateException e) {
         Log.e(TAG,e);
        } finally {
            mCurrentSessionClosed = true;
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Lock the exposure for capture
     */
    private void lockExposure(int id) {
        if (!checkSessionAndBuilder(mCaptureSession[id], mPreviewRequestBuilder[id])) {
            return;
        }
        Log.i(TAG, "lockExposure: " + id);
        try {
            applySettingsForLockExposure(mPreviewRequestBuilder[id], id);
            mState[id] = STATE_WAITING_AE_LOCK;
            if(mActivity.getPerformenceTest()) {
                mLockAETime = System.currentTimeMillis();
                mHasMapTimes.put("preCapture->lockExposure", mLockAETime - mPreCaptureTime);
            }
            if (isHighSpeedRateCapture()) {
                List<CaptureRequest> slowMoRequests = mSuperSlomoCapture ?
                    createSSMBatchRequest(mVideoRecordRequestBuilder) :
                        getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,
                                mVideoRecordRequestBuilder);
                mCaptureSession[id].setRepeatingBurst(slowMoRequests, mCaptureCallback,
                        mCameraHandler);
            } else {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id].build(),
                        mCaptureCallback, mCameraHandler);
            }
        } catch (CameraAccessException | IllegalStateException e) {
           Log.e(TAG,e);
        }
    }

    private void applySettingsForLockFocus(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        if(!mCaptureTorchTrigger) {
            applyFlash(builder, id);
        }
        applyAFRegions(builder, id);
        applyAERegions(builder, id);
        applyCommonSettings(builder, id);
    }

    private void applySettingsForCapture(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        applyJpegQuality(builder);
        if(!mCaptureTorchTrigger) {
            applyFlash(builder, id);
        }
        applyCommonSettings(builder, id);
        applySensorModeFS2(builder);
    }

    private void applySettingsForPrecapture(CaptureRequest.Builder builder, int id) {
        String redeye = mSettingsManager.getValue(SettingsManager.KEY_REDEYE_REDUCTION);
        if (redeye != null && redeye.equals("on") && !isFlashRequiredInDriver || mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
            Log.d(TAG, "Red Eye Reduction is On. " +
                    "Don't set CONTROL_AE_PRECAPTURE_TRIGGER to Start");
        } else {
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            if(mSettingsManager.isOpenManualFlash() && !isManualAEC){
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
            }
        }

        // For long shot, torch mode is used
        if (!mLongshotActive) {
            if(mCaptureTorchTrigger) {
                //for AE_PRECAPTURE_TRIGGER 1 request, AEMode:1 and flashMode:2
                applyFlashMode(builder);
            }else{
                applyFlash(builder, id);
            }
        }

        applyCommonSettings(builder, id);
    }

    private void applySettingsForLockAndPrecapture(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        applyAFRegions(builder, id);
        applyAERegions(builder, id);
        if(mLockAFAE == LOCK_AF_AE_STATE_NONE){
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        }

        applyFlash(builder, id);
        applyCommonSettings(builder, id);
    }

    private void applySettingsForLockExposure(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.TRUE);
    }

    private void applySettingsForUnlockExposure(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.FALSE);
    }

    private void applySettingsForUnlockFocus(CaptureRequest.Builder builder, int id) {
        if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        }
        applyFlash(builder, id);
        applyCommonSettings(builder, id);
    }

    private void applySettingsForAutoFocus(CaptureRequest.Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_START);
        if (mCurrentSceneMode.mode == CameraMode.HFR ||
                ((mCurrentSceneMode.mode == CameraMode.VIDEO ||
                        mCurrentSceneMode.mode == CameraMode.CINEMATIC) &&
                        !isVariableFPSEnabled())) {
            Range fpsRange = mHighSpeedCapture ? mHighSpeedFPSRange : new Range(30, 30);

            if(!mIsRecordingVideo && mHighSpeedCapture && mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS){
                fpsRange = mHighSpeedPreviewFPSRange;
            }
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
        }
        applyAFRegions(builder, id);
        applyAERegions(builder, id);
        if (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            applyVideoCommentSettings(builder, id);
        } else {
            applyCommonSettings(builder, id);
        }
    }

    private void applySettingsForJpegInformation(CaptureRequest.Builder builder, int id) {
        Location location = mLocationManager.getCurrentLocation();
        if(location != null) {
            // make copy so that we don't alter the saved location since we may re-use it
            location = new Location(location);
            // workaround for Google bug. Need to convert timestamp from ms -> sec
            location.setTime(location.getTime()/1000);
            builder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
            Log.d(TAG, "gps: " + location.toString());
        } else {
            Log.d(TAG, "no location - getRecordLocation: " + getRecordLocation());
        }

        builder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtil.getJpegRotation(id, mOrientation));
        builder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, mPictureThumbSize);
        builder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte)80);
    }

    private void applyVideoSnapshot(CaptureRequest.Builder builder, int id) {
        if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            applyAFRegions(builder, id);
            applyAERegions(builder, id);
        }else{
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            applySettingsForLockExposure(builder, id);
        }
        applyFaceDetection(builder);
        applyColorEffect(builder);
        applyVideoFlash(builder, id);
        applyExposure(builder);
        applyAICameraSnapshot(builder);
    }

    private void applyPerReqSync(CaptureRequest.Builder builder) {
        Log.d(TAG, "set PerReqSYnc enabled by default");
        builder.set(enablePerReqSync, 1);
    }

    private void applySessionParameters(CaptureRequest.Builder builder){
        if(CURRENT_MODE == CameraMode.RTB && MCXMODE){
            Log.i(TAG,"set bokeh mode for captureBuilder");
            builder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_STILL_CAPTURE);
        }
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        Log.d(TAG,"selectMode : " +selectMode);
        if (selectMode != null && (selectMode.equals("rtb") || selectMode.equals("single_rear_aibokeh"))){
            builder.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_CONTINUOUS);
        }
        if (!mSettingsManager.isMultiCameraEnabled()) {
            applyEarlyPCR(builder);
            applyVariableFPS(builder);
            applyManualHDR(builder);
            applySnapshotHDR(builder);
            applyFaceContourVersion(builder);
            applyExtendMaxZoom(builder);
            applyMctf(builder);
            applyQLL(builder);
            applyInSensorZoom(builder);
            applyIntegratedMode(builder);
            applyEnableStatsVisualizer(builder);
            applyShadingCorrection(builder);
            applyNumHDRExposure(builder);
            applyStatsVisualizerOptionMask(builder);
            applyStatsNNControl(builder);
            applyMFNRAIDEMode(builder);
            applyAICameraParam(builder);
            applyAICameraBlurModeParam(builder);
            applyAICameraHSR(builder);
            applyXCFAOptimization(builder);
            applyeHardSwitchParam(builder);
            applyMLVideoParam(builder);
            if (!isSingleCameraMode() && (mCurrentSceneMode.mode == CameraMode.RTB || mCurrentSceneMode.mode
                    == CameraMode.VIDEO || mCurrentSceneMode.mode == CameraMode.DEFAULT)
                    && PersistUtil.getPerReqSyncEnable()) {
                 applyPerReqSync(builder);
            }
        }
        Set<String> raw_ids = mSettingsManager.getPhysicalFeatureEnableId(SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
        if(raw_ids != null && raw_ids.size() > 0){
            applyMcxRawCbInfo(builder);
        }
        if (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            if (!mSettingsManager.isMultiCameraEnabled()) {
                applyOfflineDumpTrigger(builder);
            }
            if(mCurrentSceneMode.mode == CameraMode.HFR){
                applyBufferMode(builder);
            }else if(mCurrentSceneMode.mode == CameraMode.VIDEO){
                applySpatialVideo(builder);
            }
        }
        if (mCurrentSceneMode.mode == CameraMode.DEFAULT
                || mCurrentSceneMode.mode == CameraMode.VIDEO
                || mCurrentSceneMode.mode == CameraMode.HFR
                || mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            if (!mSettingsManager.isMultiCameraEnabled()) {
                applyVIULL(builder);
                applyVSR(builder);
                applyEISHorizonLevelEnable(builder);
            }
            applyEIS(builder);
        }
        if (mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            applyEnableCinematic(builder);
        }
        applyDepthMode(builder);
        applyITofTuningSet(builder);
        applyDcgModes(builder);
        setSessionParamFromFile(builder);
    }

    private void setSessionParamFromFile(CaptureRequest.Builder builder) {
        JSONObject mObj = CameraUtil.getJsonObj("system/etc/CameraVendorTags.json");
        //JSONObject mObj = CameraUtil.getJsonObj("system/etc/test.json");
        if (mObj == null) {
            Log.e(TAG, "read json system/etc/CameraVendorTags.json is null");
            return;
        }
        try {
            boolean isArray = mObj.getBoolean("IsArray");
            boolean isSessionParameters = mObj.getBoolean("IsSessionParameters");
            String name = mObj.getString("Name");
            String type = mObj.getString("Type");
            JSONArray valueArray = mObj.getJSONArray("Value");
            Log.i(TAG, "name=" + name + ",type=" + type + ",isSessionParameters=" +
                    isSessionParameters+",valueArray=" + valueArray + ",valueArray.length()=" + valueArray.length());
            if("Byte".equals(type)) {
                List<Byte> byteList = new ArrayList<>();
                for (int i = 0; i < valueArray.length(); i++) {
                    String valueString = valueArray.getString(i);
                    String[] byteStrings = valueString.split(",");
                    for (String value : byteStrings) {
                        Byte bytevalue = (byte) (Integer.parseInt(value));
                        byteList.add(bytevalue);
                    }
                }
                byte[] byteArray = new byte[byteList.size()];
                for (int i = 0; i < byteList.size(); i++) {
                    byteArray[i] = byteList.get(i);
                }
                CaptureRequest.Key<byte[]> key = new CaptureRequest.Key<byte[]>(name, byte[].class);
                Log.i(TAG, "set byteArray=" + Arrays.toString(byteArray) + ",length=" + byteArray.length
                        + ",key=" + key);
                builder.set(key,byteArray);
            }else{
                List<Integer> intList = new ArrayList<>();
                for (int i = 0; i < valueArray.length(); i++) {
                    String valueString = valueArray.getString(i);
                    String[] byteStrings = valueString.split(",");
                    for (String value : byteStrings) {
                        int intvalue = Integer.parseInt(value);
                        intList.add(intvalue);
                    }
                }
                int[] intArray = new int[intList.size()];
                for (int i = 0; i < intList.size(); i++) {
                    intArray[i] = intList.get(i);
                }
                CaptureRequest.Key<int[]> key = new CaptureRequest.Key<int[]>(name, int[].class);
                Log.i(TAG, "set intArray=" + Arrays.toString(intArray) + ",length=" + intArray.length
                        + ",key=" + key);
                builder.set(key,intArray);
            }
            Log.i(TAG," set sucess");
        } catch (Exception e) {
            Log.i(TAG, "exception =" + e);
        }
    }

    private void applyDcgModes(CaptureRequest.Builder builder){
        int value = mSettingsManager.getDcgMode();
        Log.d(TAG,"set applyDcgModes: " + value);
        VendorTagUtil.enableDcgMode(builder, value);
    }

    private void applyeHardSwitchParam(CaptureRequest.Builder builder){
        VendorTagUtil.enableHardSwitch(builder, (byte)(PersistUtil.getHardSwitchEnabled() ? 0x01 : 0x00));
    }

    private void applyMLVideoParam(CaptureRequest.Builder builder){
        String value = mSettingsManager.getValue(SettingsManager.KEY_ML_VIDEO);
        Log.d(TAG,"applyMLVideoParam, value:" + value);
        VendorTagUtil.enableMLVideo(builder, (byte)(value != null && value.equals("on") ? 0x01 : 0x00));
    }

    private void applyAICameraHSR(CaptureRequest.Builder builder){
        String value = mSettingsManager.getValue(SettingsManager.KEY_AI_CAMERA_HSR);
        if(value != null &&  !value.equals("disable")){
            Log.d(TAG,"set applyAICameraHSR: " + value);
            VendorTagUtil.setAICameraHSR(builder, Integer.parseInt(value));
        }
    }
    private void applyAICameraParam(CaptureRequest.Builder builder){
        String value = mSettingsManager.getValue(SettingsManager.KEY_AI_CAMERA);
        if(mCurrentSceneMode.mode != CameraMode.VIDEO && mCurrentSceneMode.mode != CameraMode.DEFAULT){
            //change aicamera value to 0 at RTB/HFR
            value = "0";
        }
        if(value != null && !value.equals("disable")){
            Log.d(TAG,"set applyeAiCameraTag: " + value);
            VendorTagUtil.setAICamera(builder, Integer.parseInt(value));
        }
    }

    private void applyAICameraBlurModeParam(CaptureRequest.Builder builder){
        String value = mSettingsManager.getValue(SettingsManager.KEY_AI_CAMERA_BLURMODE);
        if(value != null &&  !value.equals("disable")){
            Log.d(TAG,"set applyAICameraBlurModeParam: " + value);
            VendorTagUtil.setAICameraBlurMode(builder, Integer.parseInt(value));
        }
    }

    private void applyXCFAOptimization(CaptureRequest.Builder builder) {
        byte value = 0;
        if(mSettingsManager.getQuadBayerSensorPrefEnabled()) {
            value = 1;
        }
        try {
            builder.set(CaptureModule.xcfa_optimization, value);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "hal no vendorTag : " + xcfa_optimization);
        }
    }

    private void applyAICameraSnapshot(CaptureRequest.Builder builder) {
        try {
            builder.set(EnableAISnapshot, (byte)(mSettingsManager.isAICameraSnapshotEnabeld() ? 0x01 : 0x00));
            Log.v(TAG, " applyAICameraSnapshot value :" + mSettingsManager.isAICameraSnapshotEnabeld());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + EnableAISnapshot.toString());
        }
    }

    private void applyFlashMode(CaptureRequest.Builder builder) {
        String flashMode = mSettingsManager.getValue(SettingsManager.KEY_FLASH_MODE);
        Log.i(TAG,"isflashRequired:" + isflashRequired  + ",mCaptureTorchTrigger:" + mCaptureTorchTrigger + ",isCaptureBrustMode():" + isCaptureBrustMode() + ",mCaptureTorchTrigger:" + mCaptureTorchTrigger);
        if(isCaptureBrustMode() || "off".equals(flashMode)){
            return;
        }
        if(mCaptureTorchTrigger){
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        } else if (isflashRequired){
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
        }
        isflashRequired = false;
    }
    public void updateFlashMode(boolean inThumbnail){
        if(mCurrentSceneMode.mode == CameraMode.CINEMATIC || mCurrentSceneMode.mode == CameraMode.PRO_MODE){
            return;
        }
        CaptureRequest.Builder captureRequest = mPreviewRequestBuilder[CURRENT_ID];
        if (!checkSessionAndBuilder(mCaptureSession[CURRENT_ID], captureRequest) || mCurrentSessionClosed
                ||mPaused) {
            return;
        }
        boolean videoFlash = getCurrenCameraMode() == CaptureModule.CameraMode.VIDEO ||
                getCurrenCameraMode() == CaptureModule.CameraMode.HFR;
        String flashMode = mSettingsManager.getValue(videoFlash ? SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
        if((flashMode != null && ((videoFlash && !flashMode.equals("on")) || (!videoFlash && !flashMode.equals("alwayson")))) || flashMode == null){
            return;
        }
        if(videoFlash) {
            if (mVideoRecordRequestBuilder != null) {
                try {
                    mVideoRecordRequestBuilder.set(CaptureRequest.FLASH_MODE, inThumbnail ?
                            CaptureRequest.FLASH_MODE_OFF : CaptureRequest.FLASH_MODE_TORCH);
                    if (isHighSpeedRateCapture()) {
                        List<CaptureRequest> slowMoRequests = mSuperSlomoCapture ?
                                createSSMBatchRequest(mVideoRecordRequestBuilder) :
                                getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession, mVideoRecordRequestBuilder);
                        mCurrentSession.setRepeatingBurst(slowMoRequests, mCaptureCallback,
                                mCameraHandler);
                    } else {
                        mCurrentSession.setRepeatingRequest(mVideoRecordRequestBuilder.build(),
                                mCaptureCallback, mCameraHandler);
                    }
                } catch (CameraAccessException | IllegalStateException e) {
                    Log.i(TAG, "updateFlashMode error inThumbnail= " + inThumbnail, e);
                }
            }
        }else {
            if (captureRequest != null) {
                try {
                    captureRequest.set(CaptureRequest.FLASH_MODE, inThumbnail ?
                            CaptureRequest.FLASH_MODE_OFF : CaptureRequest.FLASH_MODE_TORCH);
                    mCurrentSession.setRepeatingRequest(captureRequest.build(),
                            mCaptureCallback, mCameraHandler);
                } catch (CameraAccessException | IllegalStateException e) {
                    Log.i(TAG, "updateFlashMode error inThumbnail= " + inThumbnail, e);
                }
            }
        }
    }
    private void applyMFNRAIDEMode(CaptureRequest.Builder builder){
        if (isAIDE2Enabled()) {
            VendorTagUtil.enableMFNRAIDEMode(builder, (byte)0x01);
        }
    }

    private void applyMctf(CaptureRequest.Builder builder){
        //add for mctf tag
        if(mSettingsManager.isSwMctfSupported() && (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC)){
            int mctfVaule = PersistUtil.mctfValue();
            try {
                builder.set(CaptureModule.mctf, (byte)(mctfVaule == 1 ? 0x01 : 0x00));
            } catch (IllegalArgumentException e) {
                Log.d(TAG, EXCEPTION_LOG,"mctf no vendor tag");
            }
        }
    }
    private void applySnapshotHDR(CaptureRequest.Builder builder) {
       String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
       String value = mSettingsManager.getValue(SettingsManager.KEY_SNAPSHOT_HDRMODE);
       if (value != null) {
           int hdrMode = 0;
           if (value.equals("mfhdr") && scene.equals("18")) {
               hdrMode = 1;
           }
           try {
               builder.set(snapshotHDR, hdrMode);
           } catch (IllegalArgumentException e) {
               Log.d(TAG, EXCEPTION_LOG,"vendor tag(" + snapshotHDR + ") is not available.");
           }
       }
    }

    private void applyCommonSettings(CaptureRequest.Builder builder, int id) {
        Log.d(TAG, "applyCommonSettings ZoomFixedSupport: " + mUI.getZoomFixedSupport() + ", mZoomValue :" + mZoomValue);
        if (mUI.getZoomFixedSupport()) {
            applyZoomRatio(builder, mZoomValue, id);
        } else {
            applyZoom(builder, id);
        }
        if (!mSettingsManager.isMultiCameraEnabled()) {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AF_MODE, mControlAFMode);
            applyAfModes(builder);
            applyFaceDetection(builder);
            applyTouchTrackFocus(builder);
            applyIsoAndExposureTime(builder);
            applySceneMode(builder);
            applyInstantAEC(builder);
            applySaturationLevel(builder);
            applyAntiBandingLevel(builder);
            applySharpnessControlModes(builder);
            applyExposureMeteringModes(builder);
            applyHistogram(builder);
            applyAWBCCTAndAgain(builder);
            applyBGStats(builder);
            applyBEStats(builder);
            applyWbColorTemperature(builder);
            applyToneMapping(builder);
            applyLivePreview(builder);
            applyPdnetToggle(builder);
            applyAICameraStrength(builder);
            applyTargetZoom(builder, 0f);
            applyInStantZoom(builder);
            //applyLowLightBoost(builder);
        }
        applyColorEffect(builder);
        applyWhiteBalance(builder);
        applyExposure(builder);
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        if (mBackgroundThreadFlag) {
            Log.w(TAG, "background thread has been running");
            return;
        }
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mImageAvailableThread = new HandlerThread("CameraImageAvailable");
        mImageAvailableThread.start();
        mCaptureCallbackThread = new HandlerThread("CameraCaptureCallback");
        mCaptureCallbackThread.start();
        mMpoSaveThread = new HandlerThread("MpoSaveHandler");
        mMpoSaveThread.start();

        mCameraHandler = new MyCameraHandler(mCameraThread.getLooper());
        mImageAvailableHandler = new Handler(mImageAvailableThread.getLooper());
        mCaptureCallbackHandler = new Handler(mCaptureCallbackThread.getLooper());
        mMpoSaveHandler = new MpoSaveHandler(mMpoSaveThread.getLooper());
        mZoomHandler = new ZoomHandler(mCaptureCallbackThread.getLooper());
        mBackgroundThreadFlag = true;
        Log.d(TAG, "startBackgroundThread");
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (!mBackgroundThreadFlag) {
            Log.w(TAG, "background thread has not been running");
            return;
        }
        Log.i(TAG, "stopBackgroundThread");
        if (mCameraThread == null) {
            return;
        }
        mCameraThread.quitSafely();
        mImageAvailableThread.quitSafely();
        mCaptureCallbackThread.quitSafely();
        mMpoSaveThread.quitSafely();

        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
        }
        try {
            mImageAvailableThread.join();
            mImageAvailableThread = null;
            mImageAvailableHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
        }
        try {
            mCaptureCallbackThread.join();
            mCaptureCallbackThread = null;
            mCaptureCallbackHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
        }
        try {
            mMpoSaveThread.join();
            mMpoSaveThread = null;
            mMpoSaveHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
        }
        mBackgroundThreadFlag = false;
    }

    private void openCamera(int id) {
        if (mPaused) {
            return;
        }
        Log.i(TAG, "openCamera " + id);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,openCamera");
        CameraManager manager;
        try {
            manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            mCameraId[id] = manager.getCameraIdList()[id];
            if (!mCameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                Log.d(TAG, "Time out waiting to lock camera opening.");
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            Log.i(TAG, "start to open cameraid  is " + mCameraId[id] );
            mOpenCameraLatency = System.currentTimeMillis();
            if(mActivity.getPerformenceTest() &&  mActivity.mColdOpenCameraTime != 0) {
                mHasMapTimes.put("onCreate->openCamera",mOpenCameraLatency - mActivity.mColdOpenCameraTime);
            }else if(mActivity.getPerformenceTest() && mClosedCamTime != 0){
                mHasMapTimes.put("onClosed->openCamera",mOpenCameraLatency - mClosedCamTime);
                mClosedCamTime = 0;
            }
            manager.openCamera(mCameraId[id], mStateCallback, mCameraHandler);
        } catch (CameraAccessException | InterruptedException e) {
            Log.e(TAG,e);
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    @Override
    public void onPreviewFocusChanged(boolean previewFocused) {
        mUI.onPreviewFocusChanged(previewFocused);
    }
    public void LongShotAbortCapture() {
        if (mCurrentSession != null && mIsLongExpTmCp) {
            try {
                mCurrentSession.abortCaptures();
                mIsLongExpTmCp = false;
            } catch (Exception e) {
                Log.e(TAG, e);
            }
        }
    }

    @Override
    public void onPauseBeforeSuper() {
        mShutterLag = 0;
        mSnapshotLatency = 0;
        mZoomLatency = 0;
        mBurstFps = 0;
        mActivity.mColdOpenCameraTime = 0;
        mPerformanceGapData.clear();
        isflashRequired = false;
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onPauseBeforeSuper");
        cancelTouchFocus();
        mActivity.runOnUiThread(() -> mUI.clearFocus());
        mPaused = true;
        if (mSurfaceReadyLock.availablePermits() == 0) {
            mSurfaceReadyLock.release();
        }
        mToast = null;
        onFocusAssistModeStop();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mUI.onPause();
            }
        });

        if (mIsRecordingVideo) {
            stopRecordingVideo(getMainCameraId());
        } else if (!mIsCloseCamera){
            if (mIsPreviewingVideo && !mIsRecordingVideo) {
                setVideoFlashOff();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mCurrentSession != null) {
                        try {
                            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,flush");
                            mFlushLatency = System.currentTimeMillis();
                            mCurrentSession.abortCaptures();
                            mFlushLatency = System.currentTimeMillis() - mFlushLatency;
                            mCurrentSession.stopRepeating();
                            if (mIsPreviewingVideo && !mIsRecordingVideo) {
                                closePreviewSession();
                            }
                            mLockNums.incrementAndGet(1);
                            if (TRACE_DEBUG) Trace.endSection();
                        } catch (CameraAccessException|IllegalStateException e) {
                            Log.w(TAG,"onPauseBeforeSuper -abortCaptures stopRepeating exception=" + e);
                            mLockNums.incrementAndGet(1);
                        }
                    }else{
                        mLockNums.incrementAndGet(1);
                    }
                }
            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mIsPreviewingVideo && !mIsRecordingVideo) {
                        exitVideoModule();
                        mLockNums.incrementAndGet(1);
                    } else {
                        //this is for photo switch to video modeq
                        mLockNums.incrementAndGet(1);
                    }
                }
            }).start();
        }
        mSettingInitLatency = System.currentTimeMillis();
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
            mSoundPlayer = null;
        }
        if (selfieThread != null) {
            selfieThread.interrupt();
        }
        resetScreenOn();
        mActivity.runOnUiThread(() -> mUI.stopSelfieFlash());
        if (TRACE_DEBUG) Trace.endSection();
    }

    @Override
    public void onPauseAfterSuper() {
        onPauseAfterSuper(true);
    }

    private void onPauseAfterSuper(boolean isExitCamera) {
        Log.i(TAG, "onPause " + (isExitCamera ? "exit camera" : "") + ",mIsCloseCamera=" + mIsCloseCamera);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onPauseAfterSuper");
        if (isExitCamera) {
            if (mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
                mLockAFAE = LOCK_AF_AE_STATE_NONE;
                updateLockAFAEVisibility();
                mUI.initFlashButton();
            }
            mIsCloseCamera = true;
        }
        writeXMLForWarmAwb();
        if (mLocationManager != null) mLocationManager.recordLocation(false);
        if (isClearSightOn()) {
            ClearSightImageProcessor.getInstance().close();
        }
        if (mInitHeifWriter != null) {
            mInitHeifWriter.close();
        }

        mActivity.runOnUiThread(() -> {
            mUI.showPreviewCover();
            mUI.hideEvSeekbar();
        });

        if (isExitCamera || mIsCloseCamera) {
            closeCamera();
        } else {
            closeProcessors();
        }
        resetAudioMute();
        mUI.releaseSoundPool();
        if (mUI.getGLCameraPreview() != null) {
            mUI.getGLCameraPreview().onPause();
        }
        mUI.hidePhysicalSurfaces();
        mUI.hideDepthView();
        mPreviewOutputConfiguration = null;
        mOldMode = mCurrentSceneMode.mode;
        mOldCameraId = CURRENT_ID;
        if (isExitCamera || mIsCloseCamera) {
            stopBackgroundThread();
            closeImageReader();
        }
        mActivity.runOnUiThread(() -> {
            setProModeVisible();
            seBlurConfigSlideVisible();
        });

        closeVideoFileDescriptor();
        if (mIntentMode != CaptureModule.INTENT_MODE_NORMAL
                && isExitCamera && mJpegImageData != null) {
            //mActivity.setResultEx(Activity.RESULT_CANCELED, new Intent());
            mActivity.finish();
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    @Override
    public void onResumeBeforeSuper() {
        onResumeBeforeSuper(false);
        //dont need to do enroll at app side, has auto enroll feature
//        if((mPostProcessor.isJPEGC2PAEnabled() || mPostProcessor.isYUVC2PAEnabled()) && mPostProcessor.isJniAPISupported() && mEnrollResult != 0){
//            mEnrollResult = mPostProcessor.nativeC2paEnroll("lens_test_PBivUzTH6Ci2SkEi5TrMpLSAw5PQ4GY8KQc32_UqUjRyuAscVYpdxeNltRmQ0Q4g", "/vendor/etc/ssg/license.txt");
//        }
    }

    public void onResumeBeforeSuper(boolean resumeFromRestartAll) {
        statsParametersUpdated = 0;//need to reload bg/be width&height
        if(!resumeFromRestartAll){
            mIsCloseCamera = true;
        }
        mSettingsManager.createCaptureModule(this);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onResumeBeforeSuper");
        // must change cameraId before "mPaused = false;"
        int facingOfIntentExtras = CameraUtil.getFacingOfIntentExtras(mActivity);
        String action = mActivity.getIntent().getAction();
        Bundle extra = mActivity.getIntent().getExtras();
        boolean isVoiceQuery = false;
        boolean noUiQuery = false;
        if(extra != null ) {
            try {
                isVoiceQuery = (boolean) extra.getBoolean("isVoiceQuery");
                noUiQuery = (boolean) extra.getBoolean("NoUiQuery");
                Log.d(TAG,"action="+action+",NoUiQuery="+noUiQuery+",isVoiceQuery="+isVoiceQuery);
            }catch (Exception e){
            }
        }
        if (facingOfIntentExtras != -1 && !resumeFromRestartAll) {
            mCurrentSceneMode.setSwithCameraId(facingOfIntentExtras,true);
        }else if(facingOfIntentExtras == -1  && ((isVoiceQuery && noUiQuery)
                || (action != null && action.equals(CameraUtil.GTS_TEST_ACTION))) && !resumeFromRestartAll) {
            mCurrentSceneMode.setSwithCameraId(facingOfIntentExtras,true);
            mSettingsManager.setValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE, "rear");
        }
        if(!CameraApp.isColdStart || mActivity.mColdOpenCameraTime == 0){
            reinit();
        }
        CameraApp.isColdStart = false;
        mPaused = false;
        mStatsVisualEnable = mSettingsManager.getValue(
                SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
        mPerformanceDebugEnable = mSettingsManager.getValue(
                SettingsManager.KEY_PERFORMANCE_DEBUG);
        mStatsVisualizer = mSettingsManager.getValue(
                SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        for (int i = 0; i < MAX_NUM_CAM; i++) {
            if(mIsCloseCamera) {
                mCameraOpened[i] = false;
            }
            mTakingPicture[i] = false;
        }
        mActivity.runOnUiThread(() -> mUI.showZoomSeekBar());
        for (int i = 0; i < MAX_NUM_CAM; i++) {
            mState[i] = STATE_PREVIEW;
        }
        mLongshotActive = false;
        if(!resumeFromRestartAll && !mUI.isPreviewSurfaceValid()) {
            updatePreviewSurfaceReadyState(false);
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void cancelTouchFocus() {
        if (getCameraMode() == DUAL_MODE) {
            if(mState[BAYER_ID] == STATE_WAITING_TOUCH_FOCUS) {
                cancelTouchFocus(BAYER_ID);
            } else if (mState[MONO_ID] == STATE_WAITING_TOUCH_FOCUS) {
                cancelTouchFocus(MONO_ID);
            }
        } else {
            if (mState[getMainCameraId()] == STATE_WAITING_TOUCH_FOCUS ||
                    mState[getMainCameraId()] == STATE_PREVIEW || mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
                cancelTouchFocus(getMainCameraId());
            }
        }
    }

    private ArrayList<Integer> getFrameProcFilterId() {
        ArrayList<Integer> filters = new ArrayList<Integer>();

        if(mDeepPortraitMode) {
            filters.add(FrameProcessor.FILTER_DEEP_PORTRAIT);
            return filters;
        }

        String scene = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        if(scene != null && !scene.equalsIgnoreCase("0")) {
            filters.add(FrameProcessor.FILTER_MAKEUP);
        }
        if(isTrackingFocusSettingOn()) {
            filters.add(FrameProcessor.LISTENER_TRACKING_FOCUS);
        }
        return filters;
    }

    public boolean isT2TFocusSettingOn() {
        try {
            String value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS);
            if(mCurrentSceneMode.mode == CameraMode.CINEMATIC){
                value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS_FOR_CINEMATIC);
            }
            if (value != null && value.equals("on")) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isSateNNFocusSettingOn() {
        try {
            String stats_nn_control = mSettingsManager.getValue(SettingsManager.KEY_STATSNN_CONTROL);
            String stats_nn_control_for_cinematic = mSettingsManager.getValue(SettingsManager.KEY_STATSNN_CONTROL_FOR_CINEMATIC);
            if (stats_nn_control != null && Integer.parseInt(stats_nn_control) == 1) {
                return true;
            }
            if (mCurrentSceneMode.mode == CameraMode.CINEMATIC &&
                    stats_nn_control_for_cinematic != null &&
                    Integer.parseInt(stats_nn_control_for_cinematic) == 1) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isCinematicDebugOn() {
        if (mCurrentSceneMode.mode != CameraMode.CINEMATIC) {
            return true;
        }
        try {
            String cinematic_debug = mSettingsManager.getValue(SettingsManager.KEY_CINEMATIC_DEBUG);
            if (cinematic_debug != null && Integer.parseInt(cinematic_debug) == 1) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isTrackingFocusSettingOn() {
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        try {
            int mode = Integer.parseInt(scene);
            if (mode == SettingsManager.SCENE_MODE_TRACKINGFOCUS_INT) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void setRefocusLastTaken(final boolean value) {
        mIsRefocus = value;
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mUI.showRefocusToast(value);
            }
        });
    }

    private int getPostProcFilterId(int mode) {
        if (mode == SettingsManager.SCENE_MODE_OPTIZOOM_INT) {
            return PostProcessor.FILTER_OPTIZOOM;
        } else if (mode == SettingsManager.SCENE_MODE_NIGHT_INT && StillmoreFilter.isSupportedStatic()) {
            return PostProcessor.FILTER_STILLMORE;
        } else if (mode == SettingsManager.SCENE_MODE_CHROMAFLASH_INT && ChromaflashFilter.isSupportedStatic()) {
            return PostProcessor.FILTER_CHROMAFLASH;
        } else if (mode == SettingsManager.SCENE_MODE_BLURBUSTER_INT && BlurbusterFilter.isSupportedStatic()) {
            return PostProcessor.FILTER_BLURBUSTER;
        } else if (mode == SettingsManager.SCENE_MODE_UBIFOCUS_INT && UbifocusFilter.isSupportedStatic()) {
            return PostProcessor.FILTER_UBIFOCUS;
        } else if (mode == SettingsManager.SCENE_MODE_SHARPSHOOTER_INT && SharpshooterFilter.isSupportedStatic()) {
            return PostProcessor.FILTER_SHARPSHOOTER;
        } else if (mode == SettingsManager.SCENE_MODE_BESTPICTURE_INT) {
            return PostProcessor.FILTER_BESTPICTURE;
        } else if (mode == SettingsManager.SCENE_MODE_DEEPZOOM_INT) {
            return PostProcessor.FILTER_DEEPZOOM;
        }
        return PostProcessor.FILTER_NONE;
    }

    private void initializeValues() {
        mMultiResReprocessEnabled = mSettingsManager.isMultiResReprocessEnabled();
        updateImageFormatKey();
        initYUVCallbackParam();
        updatePictureSize();
        updatePhysicalSize();
        updateVideoSize();
        updatePhysicalVideoSize();
        updateVideoSnapshotSize();
        updatePhysicalVideoSnapshotSize();
        updateTimeLapseSetting();
        estimateJpegFileSize();
        updateMaxVideoDuration();
        mSettingsManager.filterPictureFormatByIntent(mIntentMode);
        mSettingsManager.updatePrefByIntent(mIntentMode);

    }

    public void updateStatsParameters(CaptureResult result) {
        int[] info = mSettingsManager.getStatsInfo(result);
        if (info != null) {
            int bg_width = info[0];
            int bg_height = info[1];
            int be_width = info[2];
            int be_height = info[3];
            int depth = info[4];
            Log.d(TAG,"getStatsInfo, bg_width:" + bg_width + ",bg_height:" + bg_height + ",be_width:" +be_width + ",be_height:" +be_height + ",depth:" +depth);
            if (bg_width > 0 && bg_height > 0 && bg_width > 0 && bg_height > 0){
                BGSTATS_DATA = bg_width*bg_height;
                BGSTATS_WIDTH = bg_width*STATS_LENGTH;
                BGSTATS_HEIGHT = bg_height*STATS_LENGTH;
                bg_statsdata = new int[BGSTATS_DATA*STATS_LENGTH*STATS_LENGTH];
                bg_r_statsdata = new int[BGSTATS_DATA];
                bg_g_statsdata = new int[BGSTATS_DATA];
                bg_b_statsdata = new int[BGSTATS_DATA];
                bgstats_view.updateViewSize();
            }
            if(be_width > 0 && be_height > 0 && be_width > 0 && be_height > 0) {
                BESTATS_DATA = be_width*be_height;
                BESTATS_WIDTH = be_width*STATS_LENGTH;
                BESTATS_HEIGHT = be_height*STATS_LENGTH;
                be_statsdata   = new int[BESTATS_DATA*STATS_LENGTH*STATS_LENGTH];
                be_r_statsdata = new int[BESTATS_DATA];
                be_g_statsdata = new int[BESTATS_DATA];
                be_b_statsdata = new int[BESTATS_DATA];
                bestats_view.updateViewSize();
            }

            if (depth != -1 && depth != 0) {
                STATS_DATA_BIT_SHIFT = depth - 8;
                statsParametersUpdated = STATS_PARAMETER_UPDATE;
            }
        }
        statsParametersUpdated ++;
        Log.d(TAG,"BGSTATS_WIDTH="+BGSTATS_WIDTH+" BESTATS_HEIGHT="+BESTATS_HEIGHT+
                " STATS_DATA_BIT_SHIFT="+STATS_DATA_BIT_SHIFT);

    }

    private void updatePhysicalSize(){
        if (!mSettingsManager.isMultiCameraEnabled() &&  !mSaveRaw)
                return;
        mLogicalPreviewSize = getOptimalPhysicalPreviewSize(mPictureSize,
                mSettingsManager.getSupportedOutputSize(
                Integer.valueOf(getMainCameraId()),SurfaceHolder.class));
        Set<String> ids = mSettingsManager.getAllPhysicalCameraId();
        Set<String> quadBayerIds = new HashSet<>();
        if(mSettingsManager.isMcxQcfaMode()){
            quadBayerIds = mSettingsManager.getQuadBayerPhysicalList();
        }
        if (ids != null) {
            int i = 0;
            int x = 0;
            for (String id : ids){
                if (i >= PHYSICAL_CAMERA_COUNT)
                    break;
                String pictureSize = null;
                if(quadBayerIds.size() != 0){
                    if(quadBayerIds.contains(id)) {
                        pictureSize = mSettingsManager.getValue(SettingsManager.KEY_PHYSICAL_SIZE[x]);
                        x++;
                    }
                }else{
                    pictureSize = mSettingsManager.getValue(SettingsManager.KEY_PHYSICAL_SIZE[i]);
                }
                if (pictureSize != null){
                    mPhysicalSizes[i] = parsePictureSize(pictureSize);
                } else {
                    mPhysicalSizes[i] = mPictureSize;
                }
                mPhysicalPreviewSizes[i] = getOptimalPhysicalPreviewSize(mPhysicalSizes[i],
                        mSettingsManager.getSupportedOutputSize(
                                Integer.valueOf(id),SurfaceHolder.class));
                if (mPhysicalPreviewSizes[i] == null){
                    mPhysicalPreviewSizes[i] = mPreviewSize;
                }
                Log.d(TAG,"updatePhysicalSize set Physical "+ id+ " capture size="+mPhysicalSizes[i].toString()
                         +" preview size="+mPhysicalPreviewSizes[i].toString());

                Size[] rawSizes;
                if(mSaveRaw) rawSizes  = mSettingsManager.getSupportedOutputSize(Integer.valueOf(id),
                        mSettingsManager.getRawFormat() );
                else rawSizes  = mSettingsManager.getSupportedOutputSize(Integer.valueOf(id),
                        ImageFormat.RAW10 );
                if (rawSizes != null){
                    mPhysicalRawSizes[i] = rawSizes[0];
                } else {
                    mPhysicalRawSizes[i] = mSupportedRawPictureSize;
                }
                Size[] jpegRSizes = mSettingsManager.getSupportedOutputSize(Integer.valueOf(id),
                        ImageFormat.JPEG_R);
                if (jpegRSizes != null) {
                    mPhysicalJpegRSizes[i] = jpegRSizes[0];
                }
                mPhysicalRawId[i] = id;
                i++;
            }
        }
    }

    private void updatePhysicalVideoSize(){
        is8KInMulti = false;
        if (!mSettingsManager.isMultiCameraEnabled())
            return;
        mLogicalVideoPreviewSize = getOptimalPhysicalPreviewSize(mVideoSize,
                mSettingsManager.getSupportedOutputSize(
                        Integer.valueOf(getMainCameraId()),SurfaceHolder.class));
        Set<String> ids = mSettingsManager.getAllPhysicalCameraId();
        if (ids != null) {
            int i = 0;
            for (String id : ids){
                if (i >= PHYSICAL_CAMERA_COUNT)
                    break;
                String videoSize = mSettingsManager.getValue(SettingsManager.KEY_PHYSICAL_VIDEO_SIZE[i]);
                if (videoSize != null){
                    mPhysicalVideoSizes[i] = parsePictureSize(videoSize);
                    if(videoSize.equals("7680x4320")){
                        is8KInMulti = true;
                    }
                } else {
                    mPhysicalVideoSizes[i] = mVideoSize;
                }
                mPhysicalVideoPreviewSizes[i] = getOptimalPhysicalPreviewSize(mPhysicalVideoSizes[i],
                        mSettingsManager.getSupportedOutputSize(
                                Integer.valueOf(id),SurfaceHolder.class));
                if (mPhysicalVideoPreviewSizes[i] == null){
                    mPhysicalVideoPreviewSizes[i] = mVideoPreviewSize;
                }
                Log.d(TAG,"set Physical "+ id+ " video size="+ mPhysicalVideoSizes[i].toString()
                        +" preview size="+mPhysicalVideoPreviewSizes[i].toString());
                i++;
            }
        }
    }

    private void initYUVCallbackParam(){
        String reprocessType = mSettingsManager.getValue(SettingsManager.KEY_RAW_REPROCESS_TYPE);
        if(reprocessType != null && !reprocessType.equals("disable") && !reprocessType.equals("off")) mRawReprocessType = Integer.valueOf(reprocessType);
        if(mRawReprocessType == 1|| mRawReprocessType == 4 || mRawReprocessType == 5) {
            mYUVCount = 1;
        }else {
            mYUVCount = 0;
        }
        if(mRawReprocessType != 0 && mSettingsManager.getRawReprocessPhysicalId() != null){
            mRawCount = 1;
        } else{
            mRawCount = 0;
        }
    }

    public boolean isRawReprocess(){
        return mRawReprocessType != 0;
    }

    private void updatePreviewSize() {
        int width = mPreviewSize.getWidth();
        int height = mPreviewSize.getHeight();

        String makeup = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
        boolean makeupOn = makeup != null && !makeup.equals("0");
        if (makeupOn) {
            width = mVideoSize.getWidth();
            height = mVideoSize.getHeight();
        }

        Point previewSize = PersistUtil.getCameraPreviewSize();
        if (previewSize != null) {
            width = previewSize.x;
            height = previewSize.y;
        }

        mPreviewSize = new Size(width, height);
        if (mCurrentSceneMode.mode == CameraMode.VIDEO || mCurrentSceneMode.mode == CameraMode.HFR
                || mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            mUI.setPreviewSize(mVideoPreviewSize.getWidth(), mVideoPreviewSize.getHeight());
        } else if (!mDeepPortraitMode) {
            mUI.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        }
    }

    private void openProcessors() {
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        boolean isFlashOn = false;
        boolean isMakeupOn = false;
        boolean isSelfieMirrorOn = false;
        updateImageFormatKey();
        if(mPostProcessor != null) {
            String selfieMirror = mSettingsManager.getValue(SettingsManager.KEY_SELFIEMIRROR);
            if(selfieMirror != null && selfieMirror.equalsIgnoreCase("on")) {
                isSelfieMirrorOn = true;
            }
            String makeup = mSettingsManager.getValue(SettingsManager.KEY_MAKEUP);
            if(makeup != null && !makeup.equals("0")) {
                isMakeupOn = true;
            }
            String flashMode = mSettingsManager.getValue(SettingsManager.KEY_FLASH_MODE);
            if(flashMode != null && flashMode.equalsIgnoreCase("on")) {
                isFlashOn = true;
            }
            int filterMode = PostProcessor.FILTER_NONE;
            if (scene != null) {
                int mode = Integer.parseInt(scene);
                filterMode = getPostProcFilterId(mode);
                Log.d(TAG, "Chosen postproc filter id : " + filterMode);
                if (mode == SettingsManager.SCENE_MODE_DEEPZOOM_INT) {
                    String maxSize = mSettingsManager.getEntryValues(
                            SettingsManager.KEY_PICTURE_SIZE)[0].toString();
                    mSettingsManager.setValue(SettingsManager.KEY_PICTURE_SIZE, maxSize);
                }
            }
            mPostProcessor.onOpen(filterMode, isFlashOn, isTrackingFocusSettingOn(),
                    isT2TFocusSettingOn(), isMakeupOn, isSelfieMirrorOn,mSaveRaw,
                    mDeepPortraitMode);
        }
        if(mFrameProcessor != null) {
            mFrameProcessor.onOpen(getFrameProcFilterId(), mPreviewSize);
        }

        if(mPostProcessor.isZSLEnabled() && !isActionImageCapture()) {
            mChosenImageFormat = ImageFormat.PRIVATE;
        } else if(needYUVStream()) {
            mChosenImageFormat = ImageFormat.YUV_420_888;
        } else if(mSettingsManager.isHeifHALEncoding() || mRawReprocessType == 3) {
            Log.d(TAG, "set output format to HEIC");
            mChosenImageFormat = ImageFormat.HEIC;
        }else if(mSettingsManager.getSavePictureFormat() == mSettingsManager.JPEG_R_FORMAT){
            mChosenImageFormat = ImageFormat.JPEG_R;
        } else {
            mChosenImageFormat = ImageFormat.JPEG;
        }
        setUpCameraOutputs(mChosenImageFormat);
    }

    private boolean needYUVStream() {
        if (mPostProcessor.isFilterOn() || getFrameFilters().size() != 0 || mPostProcessor.isSelfieMirrorOn() || (mPostProcessor.isYUVC2PAEnabled())) {
            return true;
        }
        return false;
    }
    private void loadSoundPoolResource() {
        String timer = mSettingsManager.getValue(SettingsManager.KEY_TIMER);
        int seconds = Integer.parseInt(timer);
        if (seconds > 0) {
            mUI.initCountDownView();
        }
    }

    @Override
    public void onResumeAfterSuper() {
        onResumeAfterSuper(false);
    }

    private void onResumeAfterSuper(boolean resumeFromRestartAll) {
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onResumeAfterSuper");
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onResumeAfterSuper  -- init");
        Log.i(TAG, "onResume " + (mCurrentSceneMode != null ? mCurrentSceneMode.mode : "null")
                + (resumeFromRestartAll ? " isResumeFromRestartAll" : "")+",mIsCloseCamera="+mIsCloseCamera);
        if(mCurrentSceneMode.mode == CameraMode.VIDEO || mCurrentSceneMode.mode == CameraMode.HFR){
            enableVideoButton(false);//disable the video button before media recorder is ready
        }
        mUI.showControlUI();
        mUI.enableShutter(false);
        mHighSpeedCapture = false;
        if(!MCXMODE) {
            checkRTBCameraId();
        }
        if (!isBackCamera() && !frontIsAllowed()) {
            Log.d(TAG, "Current Mode " + mCurrentSceneMode.mode + "not support Front camera");
            if (!resumeFromRestartAll && mIsCloseCamera) {
                startBackgroundThread();
            }
            mUI.switchToPhotoModeDueToError(true);
            return;
        }
        mDeepPortraitMode = isDeepPortraitMode();
        initializeValues();
        updatePreviewSize();
        if (getCurrenCameraMode() == CameraMode.VIDEO ||
                getCurrenCameraMode() == CameraMode.CINEMATIC){
            mUI.initPhysicalSurfaces(mLogicalVideoPreviewSize,mPhysicalVideoPreviewSizes);
        } else {
            mUI.initPhysicalSurfaces(mLogicalPreviewSize,mPhysicalPreviewSizes);
        }

        // Set up sound playback for shutter button, video record and video stop
        if (mSoundPlayer == null) {
            mSoundPlayer = SoundClips.getPlayer(mActivity);
        }
        updateSaveStorageState();
        setDisplayOrientation();
        startBackgroundThread();
        if (TRACE_DEBUG) Trace.endSection();
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,onResumeAfterSuper  -- init2");
        openProcessors();
        loadSoundPoolResource();
        if (mDeepPortraitMode) {
            mUI.startDeepPortraitMode(mPreviewSize);
            if (mUI.getGLCameraPreview() != null) {
                mUI.getGLCameraPreview().onResume();
            }
            mUI.enableVideo(false);
        } else {
            mActivity.runOnUiThread(() -> mUI.showSurfaceView());
            mUI.stopDeepPortraitMode();
        }
        if (!mFirstTimeInitialized) {
            initializeFirstTime();
        } else {
            initializeSecondTime();
        }
        mActivity.runOnUiThread(() -> {
            updateZoom();
            mUI.reInitUI();
            setProModeVisible();
            seBlurConfigSlideVisible();
            updateZoomSeekBarVisible();
            updateAICameraSeekBar();
            updateMFNRText();//this must before showRelatedIcons, color filter based on mfnr
            updateBokehText();
            mUI.showRelatedIcons(mCurrentSceneMode.mode);
            updateFlashIcon();
            mUI.updateFlashBar();
        });
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mActivity.updateStorageSpaceAndHint();
            }
        });
        if (TRACE_DEBUG) Trace.endSection();
        if(mIsCloseCamera && !PersistUtil.isTorchMode()) {
            mOpenCameraTimes = 3;
            openCamera(getMainCameraId());
        }else if (PersistUtil.isTorchMode()){
                mUI.showTorchUI();
                mUI.hideUIinTorchMode();
        }
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (Integer.parseInt(scene) != SettingsManager.SCENE_MODE_UBIFOCUS_INT) {
            setRefocusLastTaken(false);
        }
        if(isPanoSetting(scene)) {
            if (mIntentMode != CaptureModule.INTENT_MODE_NORMAL) {
                mSettingsManager.setValue(
                        SettingsManager.KEY_SCENE_MODE, ""+SettingsManager.SCENE_MODE_AUTO_INT);
                showToast("Pano Capture is not supported in this mode");
            } else {
                mActivity.onModuleSelected(ModuleSwitcher.PANOCAPTURE_MODULE_INDEX);
            }
        }
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mUI.hideGridLineView();
            }
        });
        if(!mIsCloseCamera){
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCurrentSessionClosed = true;
                    mLockNums.waitUntilIs(2);
                    createSessions();
                }
            });
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void checkRTBCameraId() {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics;
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(CURRENT_ID));
            Byte cameraType = characteristics.get(CaptureModule.logical_camera_type);
            Log.v(TAG, "checkRTBCameraId cameraType :" + cameraType);
            if (cameraType != null) {
                switch (cameraType) {
                    case CaptureModule.TYPE_DEFAULT:
                    case CaptureModule.TYPE_SAT:
                    case CaptureModule.TYPE_VR360:
                        mIsRTBCameraId = false;
                        break;
                    case CaptureModule.TYPE_RTB:
                        mIsRTBCameraId = true;
                        break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"checkRTBCameraId no vendorTag logical_camera_type:" + logical_camera_type);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.v(TAG, "onConfigurationChanged");
        setDisplayOrientation();
    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {
        if(mFrameProcessor != null){
            mFrameProcessor.onDestory();
        }
        mSettingsManager.unregisterListener(this);
        mSettingsManager.unregisterListener(mUI);
        mUI.getmCameraControls().unRegisterListener();
        mActivity.unregisterReceiver(mBTConnectReceiver);
        mSettingsManager.destroyCaptureModule();
        if (mCameraRender != null) {
            mCameraRender.destroy();
        }
        if(mPostProcessor.isJniAPISupported()) {
            mPostProcessor.nativePerfLockRelease(1);
            mPostProcessor.nativeC2paTearDown();
        }
    }

    @Override
    public void installIntentFilter() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public boolean onBackPressed() {
        if (mIsInFocusAssistMode) {
            onFocusAssistModeStop();
            return true;
        }
        return mUI.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (CameraUtil.volumeKeyShutterDisable(mActivity)) {
                    return false;
                }
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized) {
                    if (event.getRepeatCount() == 0) {
                        onShutterButtonFocus(true);
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_CAMERA:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_RECORD:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    onVideoButtonClick();
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mFirstTimeInitialized
                        && !CameraUtil.volumeKeyShutterDisable(mActivity) && event.getRepeatCount() == 0) {
                    if(getCurrenCameraMode() != CameraMode.VIDEO &&
                            getCurrenCameraMode() != CameraMode.HFR &&
                            getCurrenCameraMode() != CameraMode.CINEMATIC){
                        onShutterButtonClick();
                    } else {
                        onVideoButtonClick();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized) {
                    onShutterButtonFocus(false);
                }
                return true;
        }
        return false;
    }

    @Override
    public int onZoomChanged(int requestedZoom) {
        return 0;
    }

    @Override
    public boolean onZoomChanged(float requestedZoom) {
        if (mIsRTBCameraId || isTakingPicture()) return false;
        mZoomValue = requestedZoom;
        mUI.updateZoomSeekBar(mZoomValue);
        applyZoomAndUpdate();
        return true;
    }

    public boolean updateZoomChanged(float requestedZoom) {
        Log.d(TAG,"updateZoomChanged,mPaused:" + mPaused + ",mResumed:" +mResumed+",requestedZoom="+requestedZoom);
        if (mIsRTBCameraId || isTakingPicture() || !mResumed) return false;
        mZoomValue = requestedZoom;
        applyZoomAndUpdate();
        return true;
    }

    public void onZoomEnd() {
        if (mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
            mUI.setFocusPosition(mClickPosition[0], mClickPosition[1]);
            mUI.onFocusStarted();
            mUI.onFocusSucceeded(false);
        }
    }

    public void updateZoomSmooth(float from, float to, int frame) {
        if (isTakingPicture() || mIsRTBCameraId) {
            return;
        }
        float delta = (to - from) / frame;
        for (int i = 0; i < frame; i++) {
            float zoom = mZoomValue + delta;
            if (from > to && zoom <= to) {
                mZoomValue = to;
            } else if (from < to && zoom >= to){
                mZoomValue = to;
            } else {
                mZoomValue = zoom;
            }
            applyZoomAndUpdate(getMainCameraId(),true, to);
        }
        applyZoomAndUpdate();
    }

    public void updateOfflineDumpTriggerStatus(int trigger) {
        Log.v(TAG, "updateOfflineDumpTriggerStatus trigger :" + trigger
                + ", mVideoFrameNumber :" + mVideoFrameNumber);
        try {
            mVideoRecordRequestBuilder.set(offline_dump_trigger_trigger, trigger);
            mVideoRecordRequestBuilder.set(offline_dump_trigger_framenum, mVideoFrameNumber + 1);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG,"updateOfflineDumpTriggerStatus no vendorTag :" +
                    offline_dump_trigger_trigger);
            Log.v(TAG, EXCEPTION_LOG,"updateOfflineDumpTriggerStatus no vendorTag :" +
                    offline_dump_trigger_framenum);
        }
        try {
            if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                List requestList = getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,
                                mVideoRecordRequestBuilder);
                mCurrentSession.captureBurst(requestList, mCaptureCallback, mCameraHandler);
            } else if (isSSMEnabled()) {
                mCurrentSession.captureBurst(createSSMBatchRequest(mVideoRecordRequestBuilder),
                        mCaptureCallback, mCameraHandler);
            } else {
                mCurrentSession.capture(mVideoRecordRequestBuilder.build(), mCaptureCallback,
                        mCameraHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        }
    }

    private boolean isInMode(int cameraId) {
        if (isBackCamera()) {
            switch (getCameraMode()) {
                case DUAL_MODE:
                    return cameraId == BAYER_ID || cameraId == MONO_ID;
                case BAYER_MODE:
                    return cameraId == BAYER_ID;
                case MONO_MODE:
                    return cameraId == MONO_ID;
                case SWITCH_MODE:
                    return cameraId == SWITCH_ID;
            }
        } else if (SWITCH_ID != -1) {
            return cameraId == SWITCH_ID;
        } else {
            return cameraId == FRONT_ID;
        }
        return false;
    }

    @Override
    public boolean isImageCaptureIntent() {
        return false;
    }

    @Override
    public boolean isCameraIdle() {
        return true;
    }

    @Override
    public void onCaptureDone() {
        Log.i(TAG," onCaptureDone mPaused="+mPaused);
        if (mPaused) {
            return;
        }
        byte[] data = mJpegImageData;
        if(data == null ){
            Log.e(TAG,"mJpegImageData is null ,return.mJpegImageData= "+mJpegImageData);
            return;
        }
        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    outputStream.write(data);
                    outputStream.close();
                    mActivity.setResultEx(Activity.RESULT_OK);
                    mActivity.finish();
                } catch (IOException ex) {
                    // ignore exception
                } finally {
                    CameraUtil.closeSilently(outputStream);
                }
            } else {
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    Log.w(TAG,"get exif failed");
                }
                int orientation = 0;
                if (exif != null) {
                    orientation = CameraUtil.getOrientation(exif);
                } else {
                    orientation = CameraUtil.getJpegRotation(getMainCameraId(),mOrientation);
                }
                Bitmap bitmap = CameraUtil.makeBitmap(data, 50 * 1024);
                bitmap = CameraUtil.rotate(bitmap, orientation);
                mActivity.setResultEx(Activity.RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                mActivity.finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = mActivity.getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = mActivity.openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                mActivity.setResultEx(Activity.RESULT_CANCELED);
                mActivity.finish();
                return;
            } catch (IOException ex) {
                mActivity.setResultEx(Activity.RESULT_CANCELED);
                mActivity.finish();
                return;
            } finally {
                CameraUtil.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean(CameraUtil.KEY_RETURN_DATA, true);
            }
            if (mActivity.isSecureCamera()) {
                newExtras.putBoolean(CameraUtil.KEY_SHOW_WHEN_LOCKED, true);
            }

            // TODO: Share this constant.
            final String CROP_ACTION = "com.android.camera.action.CROP";
            Intent cropIntent = new Intent(CROP_ACTION);

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    public void onRecordingDone(boolean valid) {
        mStopRecPending = false;
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = Activity.RESULT_OK;
            resultIntent.setData(mCurrentVideoUri);
            resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            resultCode = Activity.RESULT_CANCELED;
        }
        mActivity.setResultEx(resultCode, resultIntent);
        mActivity.finish();
    }

    public void onRetakeVideo() {
        mTempHoldVideoInVideoIntent = false;
        createSessions();
    }

    @Override
    public void onCaptureCancelled() {

    }

    @Override
    public void onCaptureRetake() {

    }

    @Override
    public void cancelAutoFocus() {

    }

    @Override
    public void stopPreview() {

    }

    @Override
    public int getCameraState() {
        return 0;
    }

    private boolean mFASurfaceConfigured = false;
    private void configureFASurface() {
        if (!isTouchFocusAssistSupported() || mIsInFocusAssistMode || !getCameraModeSwitcherAllowed()) {
            return;
        }
        if (mFASurfaceConfigured) {
            return;
        }
        Log.d(TAG, "configureFASurface");
        if (mFAOutputConfiguration != null && mCurrentSession != null) {
            int id = mCurrentSceneMode.getCurrentId();
            if (mFASurface != null) {
                try {
                    mFAOutputConfiguration.removeSurface(mFASurface);
//                    mCaptureSession[id].updateOutputConfiguration(mFAOutputConfiguration);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "", e.fillInStackTrace());
                }
            }
            mCameraRender.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            SurfaceTexture st = mCameraRender.getSurfaceTexture();
            st.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mFASurface = new Surface(st);
            mPreviewSurface = mFAOutputConfiguration.getSurface();
            mFAOutputConfiguration.addSurface(mFASurface);
            try {
                mCaptureSession[id].updateOutputConfiguration(mFAOutputConfiguration);
                mFASurfaceConfigured = true;
            } catch (Exception e) {
                Log.w(TAG, "", e.fillInStackTrace());
                mFASurfaceConfigured = false;
            }
        }
    }

    public CameraRender getCameraRender() {
        return mCameraRender;
    }

    private boolean mIsInFocusAssistMode = false;
    private boolean mWasInFocusAssistMode = false;
    private float   mCropX = 0.0f;
    private float   mCropY = 0.0f;

    public void onFocusAssistStartPointChange(float xs, float ys) {
        mCropX = xs;
        mCropY = ys;
        mCameraRender.setCropRegionStartPoint(xs, ys);
    }

    public void onFocusAssistFocusPointChange(float x, float y) {
        Log.d(TAG, "onFocusAssistFocusPointChange " + x + " " + y);
        int previewW = mPreviewSize.getHeight();
        int previewH = mPreviewSize.getWidth();
        float xf = (x / previewW - mCropX) / 0.5f;
        float yf = 1.0f - ((y / previewH - 1.0f + mCropY + 0.5f) / 0.5f);
        mCameraRender.setFocusPoint(xf, yf);
    }

    public void onFocusAssistFocusPointChangeFA(float x, float y) {
        Log.d(TAG, "onFocusAssistFocusPointChangeFA " + x + " " + y);
        int previewW = mPreviewSize.getHeight();
        int previewH = mPreviewSize.getWidth();
        float xf = (x / previewW - mCropX) / 0.5f;
        float yf = 1.0f - ((y / previewH - 1.0f + mCropY + 0.5f) / 0.5f);
        mCameraRender.setFocusPointFA(xf, yf);
    }

    public PointF onFocusAssistCenter(final int x, final int y) {
        Log.d(TAG, "onFocusAssistCenter " + x + " " + y);
        int previewW = mPreviewSize.getHeight();
        int previewH = mPreviewSize.getWidth();
        float x_ = 1.0f * x / previewW;
        float y_ = 1.0f - 1.0f * y / previewH;
        float xs = Math.max(Math.min(x_, 0.75f) - 0.25f, 0f);
        float ys = Math.max(Math.min(y_, 0.75f) - 0.25f, 0f);
        Log.d(TAG, "onFocusAssistCenter " + xs + " " + ys);
        onFocusAssistStartPointChange(xs, ys);
        onFocusAssistFocusPointChange(x, y);
        return new PointF(xs, ys);
    }

    public void onFocusAssistModeStart(final int x, final int y) {
        Log.d(TAG, "onFocusAssistModeStart " + x + " " + y);
        if (!isTouchFocusAssistSupported()) {
            return;
        }
        PointF start = onFocusAssistCenter(x, y);
        mUI.showFocusAssistView(mCameraRender, start.x, start.y);
        if (mFASurface != null && mPreviewSurface != null) {
            int id = mCurrentSceneMode.getCurrentId();
            mPreviewRequestBuilder[id].addTarget(mFASurface);
            mPreviewRequestBuilder[id].removeTarget(mPreviewSurface);
            try {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException | IllegalStateException e) {
                Log.e(TAG, "onFocusAssistModeStart ", e.fillInStackTrace());
            }
        }
        mIsInFocusAssistMode = true;
        mWasInFocusAssistMode = false;
    }

    public void onFocusAssistModeStop() {
        Log.d(TAG, "onFocusAssistModeStop " + mIsInFocusAssistMode);
        mActivity.runOnUiThread(() -> mUI.hideFocusAssistText());
        if (!mIsInFocusAssistMode) {
            return;
        }
        mUI.hideFocusAssistView();
        int id = mCurrentSceneMode.getCurrentId();
        if (mFASurface != null && mPreviewSurface != null && mPreviewRequestBuilder[id] != null) {
            mPreviewRequestBuilder[id].addTarget(mPreviewSurface);
            mPreviewRequestBuilder[id].removeTarget(mFASurface);
            try {
                mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                        .build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException | IllegalStateException e) {
                Log.e(TAG, "onFocusAssistModeStart ", e.fillInStackTrace());
            }
        }
        mIsInFocusAssistMode = false;
        mWasInFocusAssistMode = true;
    }

    public void onFocusAssistRefocus(float x, float y) {
        Log.d(TAG, "onFocusAssistReFocus " + x + " " + y);
        Point point = mUI.getPointInScreen((int)x, (int)y);
        onSingleTapUp(null, point.x, point.y);
        onFocusAssistFocusPointChangeFA(x, y);
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {

        if (mPaused || !mCamerasOpened || !mFirstTimeInitialized || !mAutoFocusRegionSupported
                || !mAutoExposureRegionSupported || !isTouchToFocusAllowed()
                || mCaptureSession[getMainCameraId()] == null || mCurrentSessionClosed
                || mSettingsManager.getPhysicalCameraId() != null) {
            return;
        }
        mUI.hideFocusAssistText();
        tapUpFrameNumber = mVideoFrameNumber;
        Log.i(TAG, "onSingleTapUp " + x + " " + y + " " + tapUpFrameNumber);
        if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
            mLockAFAE = LOCK_AF_AE_STATE_NONE;
            applyIsAfLock(false);
            cancelTouchFocus(mCurrentSceneMode.getCurrentId());
            applySettingsForUnlockExposure(mPreviewRequestBuilder[mCurrentSceneMode.getCurrentId()], mCurrentSceneMode.getCurrentId());
            updateLockAFAEVisibility();
            mUI.initFlashButton();
        }
        int[] newXY = {x, y};
        if (mUI.isOverControlRegion(newXY)) return;
        if (!mUI.isOverSurfaceView(newXY)) return;

        if (mT2TFocusRenderer != null && mT2TFocusRenderer.isShown()) {
            mT2TFocusRenderer.onSingleTapUp(x, y);
            triggerTouchFocus(x, y, TouchTrackFocusRenderer.TRACKER_CMD_REG);
            return;
        }

        mUI.setFocusPosition(x, y);
        mUI.showEvSeekbar(x,y);
        int x_ = newXY[0];
        int y_ = newXY[1];
        mUI.setFocusPointInPreview(x_, y_);
        x = newXY[0];
        y = newXY[1];
        mInTAF = true;
        mUI.onFocusStarted();
        if (mIsInFocusAssistMode)
            triggerFocusAtPointFA(x, y, getMainCameraId());
        else
            triggerFocusAtPoint(x, y, getMainCameraId());

    }

    @Override
    public void onLongPress(View view, int x, int y) {
        if (mPaused || !mCamerasOpened || !mFirstTimeInitialized || !mAutoFocusRegionSupported
                || !mAutoExposureRegionSupported || !isTouchToFocusAllowed()) {
            return;
        }
        Log.d(TAG, "onLongPress " + x + " " + y);
        int[] newXY = {x, y};
        if (mUI.isOverControlRegion(newXY)) return;
        if (!mUI.isOverSurfaceView(newXY)) return;
        mClickPosition[0] = x;
        mClickPosition[1] = y;
        mUI.hideFlashButton();
        if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
            Log.d(TAG,"set af lock start");
            applyIsAfLock(false);
            mLockAFAE = LOCK_AF_AE_STATE_START;
            applySettingsForUnlockExposure(mPreviewRequestBuilder[mCurrentSceneMode.getCurrentId()], mCurrentSceneMode.getCurrentId());
            cancelTouchFocus(mCurrentSceneMode.getCurrentId());
            mState[mCurrentSceneMode.getCurrentId()] = STATE_WAITING_AF_AE_RELEASE;
            updateLockAFAEVisibility();
        } else{
            mLockAFAE = LOCK_AF_AE_STATE_START;
            applyIsAfLock(true);
            mUI.setFocusPosition(x, y);
            mUI.showEvSeekbar(x,y);
            x = newXY[0];
            y = newXY[1];
            mUI.onFocusStarted();
            triggerFocusAtPoint(x, y, mCurrentSceneMode.getCurrentId());
            lockExposure(mCurrentSceneMode.getCurrentId());

        }
    }

    private void updateLockAFAEVisibility() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mLockAFAEText.setVisibility(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    public int getMainCameraId() {
        if (CaptureModule.FRONT_ID != mCurrentSceneMode.getCurrentId()) {
            String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
            if (selectMode != null && (selectMode.equals("single_rear_cameraid")|| selectMode.equals("single_rear_aibokeh")) && mSingleRearId != -1) {
                return mSingleRearId;
            } else if (selectMode != null && selectMode.equals("sat") && mLogicalId != -1) {
                return mLogicalId;
            } else if (mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
                return mSingleRearId;
            }
        }
        return mCurrentSceneMode.getCurrentId();
    }
    public boolean isSingleCameraMode(){
        if(CaptureModule.FRONT_ID==mCurrentSceneMode.getCurrentId())
            return true;
        String selectMode=mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        if (selectMode!=null && (selectMode.equals("single_rear_cameraid") || selectMode.equals("single_rear_aibokeh"))
                || mSingleRearId == getMainCameraId()) {
            return true;
        } else {
            return false;
        }
    }
    public boolean isLongExpTmCaptrure(){
        if(mCurrentSceneMode.mode == CameraMode.PRO_MODE && isTakingPicture() && mIsLongExpTmCp && mLongExpTime >maxExpTime) return true;
        else return false;
    }
    public boolean isLongExptime(){
        Log.d(TAG,"mLongExpTime="+mLongExpTime);
        if(mCurrentSceneMode.mode == CameraMode.PRO_MODE  && mLongExpTime > maxExpTime) return true;
        else return false;
    }
    public boolean isTakingPicture() {
        for (int i = 0; i < mTakingPicture.length; i++) {
            if (mTakingPicture[i]) return true;
        }
        return false;
    }

    public boolean isRTBModeInSelectMode() {
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        if(selectMode != null && selectMode.equals("rtb")){
            return true;
        }
        return false;
    }

    private boolean isTouchToFocusAllowed() {
        if (isTakingPicture() &&
                !(mT2TFocusRenderer != null && mT2TFocusRenderer.isShown())) {
            return false;
        }
        return true;
    }

    private boolean isTouchAfEnabledSceneMode() {
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (scene == null) return false;
        int mode = Integer.parseInt(scene);
        if (mode != CaptureRequest.CONTROL_SCENE_MODE_DISABLED
                && mode < SettingsManager.SCENE_MODE_CUSTOM_START)
            return true;
        return false;
    }

    private ExtendedFace[] getBsgcInfo(CaptureResult captureResult, Face[] faces) {
        final int size = faces.length;
        if (captureResult == null || size == 0) {
            Log.d(FD_TAG, FD_LOG, "extendface size =" + size);
            return null;
        }
        ExtendedFace[] extendedFaces = new ExtendedFace[size];
        boolean bsgEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_SMILE) ||
                mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GAZE) ||
                mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_BLINK);
        boolean contourEnable = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FACIAL_CONTOUR);
        boolean facePointEnable = isFacePointOn();

        if (bsgEnable) {
            try {
                byte[] blinkDetectedArray = captureResult.get(blinkDetected);
                Log.d(FD_TAG, FD_LOG, "blinkDetectedArray=" + Arrays.toString(blinkDetectedArray));
                byte[] blinkDegreesArray = captureResult.get(blinkDegree);
                Log.d(FD_TAG, FD_LOG, "blinkDegreesArray=" + Arrays.toString(blinkDegreesArray));
                byte[] gazeDirectionArray = captureResult.get(gazeDegree);
                Log.d(FD_TAG, FD_LOG, "gazeDirectionArray=" + Arrays.toString(gazeDirectionArray));
                byte[] gazeAngleArray = captureResult.get(gazeAngle);

                Log.d(FD_TAG, FD_LOG, "gazeAngleArray=" + Arrays.toString(gazeAngleArray));
                for (int i = 0; i < size; i++) {
                    ExtendedFace tmp = new ExtendedFace(faces[i].getId());
                    try {
                        if (gazeDirectionArray != null && (2 * i + 1) < gazeDirectionArray.length) {
                            tmp.setGazeDirection(gazeDirectionArray[2 * i], gazeDirectionArray[2 * i + 1]);
                        }
                        if (gazeAngleArray != null && i < gazeAngleArray.length) {
                            tmp.setGazeAngle(gazeAngleArray[i]);
                        }
                        if (blinkDetectedArray != null && i < blinkDetectedArray.length) {
                            tmp.setBlinkDetected(blinkDetectedArray[i]);
                        }
                        if (blinkDegreesArray != null && (2 * i + 1) < blinkDegreesArray.length) {
                            tmp.setBlinkDegree(blinkDegreesArray[2 * i + 1], blinkDegreesArray[2 * i]);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }
                    extendedFaces[i] = tmp;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG, "getBsgcInfo =" + e);
            }
        }
        if (contourEnable || facePointEnable) {
            try {
                String contourMode = mSettingsManager.getValue(SettingsManager.KEY_FACIAL_CONTOUR);
                byte[] contour_all = null;
                byte[] contourPoints = null;
                int[] visibility = null;
                int[] points = null;
                int[] visib = null;
                if ("5".equals(contourMode) || "6".equals(contourMode) ||
                        "7".equals(contourMode) || "8".equals(contourMode)) {
                    contourPoints = captureResult.get(CaptureModule.contourPointsExtend);
                    contour_all = captureResult.get(CaptureModule.contourPointsExtend);
                    int faceContour = PersistUtil.getPersistFaceContourHeaderSize();
                    int numPointsPerFace = byteArray2Int(contour_all, 8);
                    int numFaces = byteArray2Int(contour_all, 12);
                    int offSet = byteArray2Int(contour_all, 16);
                    Log.d(FD_TAG, FD_LOG, "FaceContour result header size is " + faceContour +
                            ",contour_all.length=" + contour_all.length + ",numPointsPerFace=" + numPointsPerFace
                            + ",numFaces=" + numFaces + ",offset=" + offSet);

                    int arrayindex = faceContour * 4;
                    points = new int[numPointsPerFace * numFaces * 2];

                    for (int i = 0; i < numPointsPerFace * numFaces * 2; i++) {
                        points[i] = byteArray2Int(contour_all, arrayindex);
                        arrayindex += 4;
                    }
                    Log.d(FD_TAG, FD_LOG, "000Version=V " + contourMode + ",points=" + Arrays.toString(points)
                            + ",point.len=" + points.length);
                    if (mSettingsManager.isFdFeatureDisplay(mSettingsManager.KEY_FACIAL_CONTOUR_VISIBILITY) && offSet > 0) {
                        visib = new int[numPointsPerFace * numFaces];
                        for (int i = 0; i < numPointsPerFace * numFaces; i++) {
                            if(arrayindex < contour_all.length) {
                                visib[i] = contour_all[arrayindex];
                                arrayindex += 1;
                            }else{
                                break;
                            }
                        }
                        Log.d(FD_TAG, FD_LOG, ",visibility=" + Arrays.toString(visib)
                                + ",point.len=" + points.length + ",visib.len=" + visib.length);
                    }
                }
                int[] landmarkPoints = new int[6 * faces.length];
                try {
                    for (int i = 0; i < faces.length; i++) {
                        landmarkPoints[6 * i] = faces[i].getLeftEyePosition().x;
                        landmarkPoints[6 * i + 1] = faces[i].getLeftEyePosition().y;
                        landmarkPoints[6 * i + 2] = faces[i].getRightEyePosition().x;
                        landmarkPoints[6 * i + 3] = faces[i].getRightEyePosition().y;
                        landmarkPoints[6 * i + 4] = faces[i].getMouthPosition().x;
                        landmarkPoints[6 * i + 5] = faces[i].getMouthPosition().y;
                    }
                } catch (Exception e) {
                }
                Log.d(FD_TAG, FD_LOG, "landmarkPoints=" + Arrays.toString(landmarkPoints));
                ExtendedFace tmp;
                if (extendedFaces[0] == null) {
                    tmp = new ExtendedFace(faces[0].getId());
                    extendedFaces[0] = tmp;
                } else {
                    tmp = extendedFaces[0];
                }
                tmp.setVisibility(visib);
                tmp.setContour(points);
                tmp.setLandMarks(landmarkPoints);
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG, "getContour exception=" + e);
            }
        }


        if (mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_GENDER)) {
            try {
                byte[] genderArray = captureResult.get(GENDER);
                Log.d(FD_TAG, FD_LOG, "genderArray=" + Arrays.toString(genderArray));
                if (genderArray == null) {
                    throw new RuntimeException("gender result is null");
                }
                int arrayIndex = 0;
                final int version = byteArray2Int(genderArray, arrayIndex);
                arrayIndex += 4;
                Log.d(FD_TAG, FD_LOG, "fd gender version " + version);
                final int faceNum = byteArray2Int(genderArray, arrayIndex);
                arrayIndex += 12;
                Log.d(FD_TAG, FD_LOG, "fd gender faceNum " + faceNum);
                for (int i = 0; i < faceNum; i++) {
                    final int gender = byteArray2Int(genderArray, arrayIndex);
                    arrayIndex += 4;
                    Log.d(FD_TAG, FD_LOG, "fd gender index " + gender);
                    final int face_id = byteArray2Int(genderArray, arrayIndex);
                    arrayIndex += 4;
                    Log.d(FD_TAG, FD_LOG, "fd gender face_id " + face_id);
                    int genderCount = ExtendedFace.FDGenderIndex.values().length;
                    int[] confidences = new int[genderCount];
                    for (int j = 0; j < genderCount; j++) {
                        confidences[j] = byteArray2Int(genderArray, arrayIndex);
                        arrayIndex += 4;
                        Log.d(FD_TAG, FD_LOG, "fd gender confidence " + j + " " + confidences[j]);
                    }
                    ExtendedFace tmp = null;
                    int k_ = 0;
                    for (int k = 0; k < faces.length; k++) {
                        if (faces[k] != null && face_id == faces[k].getId()) {
                            k_ = k;
                            break;
                        }
                    }
                    tmp = extendedFaces[k_];
                    if (tmp == null) {
                        tmp = new ExtendedFace(face_id);
                    }
                    tmp.setGender(gender);
                    tmp.setGenderConfidence(confidences);
                    extendedFaces[k_] = tmp;
                }
            } catch (Exception e) {
                Log.w(TAG, "GENDER exception = " + e.fillInStackTrace());
            }

        }

        if (mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_FACE_EXPRESSION)) {
            try {
                byte[] expressionArray = captureResult.get(FACE_EXPRESSION);
                Log.d(FD_TAG, FD_LOG, "expressionArray=" + Arrays.toString(expressionArray));
                int expressionCount = ExtendedFace.FDExpressionIndex.values().length;
                int arrayIndex = 0;
                final int version = byteArray2Int(expressionArray, arrayIndex);
                arrayIndex += 4;
                Log.d(FD_TAG, FD_LOG, "fd expression version " + version);
                final int faceNum = byteArray2Int(expressionArray, arrayIndex);
                arrayIndex += 12;
                Log.d(FD_TAG, FD_LOG, "fd expression faceNum " + faceNum);
                for (int i = 0; i < faceNum; i++) {
                    final int faceExpression = byteArray2Int(expressionArray, arrayIndex);
                    arrayIndex += 4;
                    Log.d(FD_TAG, FD_LOG, "fd expression index " + faceExpression);
                    final int face_id = byteArray2Int(expressionArray, arrayIndex);
                    arrayIndex += 4;
                    Log.d(FD_TAG, FD_LOG, "fd expression face_id " + face_id);
                    int[] confidences = new int[expressionCount];
                    for (int j = 0; j < expressionCount; j++) {
                        confidences[j] = byteArray2Int(expressionArray, arrayIndex);
                        arrayIndex += 4;
                        Log.d(FD_TAG, FD_LOG, "fd expression confidence " + j + " " + confidences[j]);
                    }
                    ExtendedFace tmp = null;
                    int k_ = 0;
                    for (int k = 0; k < faces.length; k++) {
                        if (faces[k] != null && face_id == faces[k].getId()) {
                            k_ = k;
                            break;
                        }
                    }
                    tmp = extendedFaces[k_];
                    if (tmp == null) {
                        tmp = new ExtendedFace(face_id);
                    }
                    tmp.setFaceExpression(faceExpression);
                    tmp.setFaceExpressionConfidences(confidences);
                    extendedFaces[k_] = tmp;
                }
            } catch (Exception e) {
                Log.w(TAG, "FACE_EXPRESSION = " + e.fillInStackTrace());
            }
        }
            if (mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_FD_SKIN_TONE)) {
                try {
                    byte[] skinToneArray = captureResult.get(skinToneResults);
                    Log.d(FD_TAG, FD_LOG, "skinToneArray=" + Arrays.toString(skinToneArray));
                    int skinToneCount = ExtendedFace.FDSkineToneIndex.values().length;
                    int arrayIndex = 0;
                    final int version = byteArray2Int(skinToneArray, arrayIndex);
                    arrayIndex += 4;
                    Log.d(FD_TAG, FD_LOG, "fd skinTone version " + version);
                    final int faceNum = byteArray2Int(skinToneArray, arrayIndex);
                    arrayIndex += 12;
                    Log.d(FD_TAG, FD_LOG, "fd skinTone faceNum:" + faceNum);
                    for (int i = 0; i < faceNum; i++) {
                        final int face_id = byteArray2Int(skinToneArray, arrayIndex);
                        arrayIndex += 4;
                        Log.d(FD_TAG, FD_LOG, "fd skinTone face_id:" + face_id);
                        final int faceSkinTone = byteArray2Int(skinToneArray, arrayIndex);
                        arrayIndex += 4;
                        Log.d(FD_TAG, FD_LOG, "fd skinTone:  " + faceSkinTone);
                        int[] confidences = new int[skinToneCount];
                        for (int j = 0; j < skinToneCount; j++) {
                            confidences[j] = byteArray2Int(skinToneArray, arrayIndex);
                            arrayIndex += 4;
                            Log.d(FD_TAG, FD_LOG, "fd skinTone confidence " + j + " " + confidences[j]);
                        }
                        ExtendedFace tmp = null;
/*                        int k_ = 0;
                        for (int k = 0; k < faces.length; k++) {
                            if (faces[k] != null && face_id == faces[k].getId()) {
                                k_ = k;
                                break;
                            }
                        }*/
                        tmp = extendedFaces[i];
                        if (tmp == null) {
                            tmp = new ExtendedFace(i);
                        }
                        Log.d(FD_TAG, FD_LOG,"set skinetone="+faceSkinTone+",extendedFaces i="+i);
                        tmp.setFaceSkinTone(faceSkinTone);
                        extendedFaces[i] = tmp;
                    }
                } catch (Exception e) {
                    Log.w(TAG,"FACE_SKIN_TONE = " + e.fillInStackTrace());
                }
            }

        return extendedFaces;
    }



    private void updateFacialMask(CaptureResult result) {
        byte[] facialMasks = null;
        int[] facialMaskInts = null;
        int maskNums = 0;
        try {
            facialMasks = result.get(facialMaskResults);
        } catch (IllegalArgumentException e) {
            mIsFacialMaskSupported = false;
            Log.w(TAG, "can`t get vendorTag facialMaskResults :" + facialMaskResults);
        } catch (NullPointerException e) {
            Log.w(TAG, "updateFacialMask facialMasks get NULL");
        }

        if (facialMasks != null) {
            try {
                int size = facialMasks.length / 4;
                facialMaskInts = new int[40];
                Log.w(TAG, " onCaptureCompleted size :" + size);
                int j = 0;
                // why int i = 44
                // struct FDMetaDataMaskResults
                // {
                //     UINT32         numMasks;(4 byte data)
                //     INT32          faceID[FDMaxFaceCount];(40 byte data)
                //     FDROIRegion    maskROI[FDMaxFaceCount];(160 byte data)
                // }
                maskNums = byteArray2Int(facialMasks, 0);
                for (int i = 44; i < facialMasks.length; i += 4) {
                    facialMaskInts[j] = byteArray2Int(facialMasks, i);
                    Log.w(TAG, " onCaptureCompleted j :" + j + ", i :" + i + " facialMaskInts[j] :" + facialMaskInts[j]);
                    j++;
                }
            } catch (Exception e) {
                Log.e(TAG, " updateFacialMask byteArray2Int occur exception");
            }
        }

        Log.w(TAG, " onCaptureCompleted maskNums :" + maskNums);
        try {
            mUI.onFacialMaskDetection(facialMaskInts, maskNums);
        } catch (Exception e) {
            Log.e(TAG, " updateFacialMask occur exception");
        }
    }

    private void updateUpperBodyDetection(CaptureResult result) {
        byte[] upperBodys = null;
        int[] headInts = null;
        int[] torsoValidInts = null;
        int[] torsoInts = null;
        int headNums = 0;
        try {
            upperBodys = result.get(upperbodyResults);
            Log.d(FD_TAG,FD_LOG,"upperbodyResults="+Arrays.toString(upperBodys));
        } catch (IllegalArgumentException e) {
            mIsUpperBodySupported = false;
            Log.w(TAG, "can`t get vendorTag upperbodyResults :" + upperbodyResults);
        } catch (NullPointerException e) {
            Log.w(TAG, "updateUpperBodyDetection upperBodys get NULL");
        }
        if (upperBodys != null) {
            int size = upperBodys.length / 4;
            headInts = new int[40];
            torsoValidInts = new int[10];
            torsoInts = new int[40];
            Log.d(FD_TAG,FD_LOG," updateUpperBodyDetection size :" + size);
            int j = 0;
            // why int i = 44
            // struct FDMetadataUpperBodyResults
            //{
            //    UINT32       numHead; (4 byte)
            //    INT32        linkedFaceId[FDMaxFaceCount];  (40 bytes)
            //    FDROIRegion  headROI[FDMaxFaceCount];  (160 bytes)
            //    BOOL         torsoValid[FDMaxFaceCount];  (40 bytes)
            //    FDROIRegion  torsoROI[FDMaxFaceCount];  (160 bytes)
            //}

            try {
                headNums = byteArray2Int(upperBodys, 0);
                for (int i = 44; i < upperBodys.length; i += 4) {
                    if (j == headInts.length) {
                        break;
                    }
                    headInts[j] = byteArray2Int(upperBodys, i);
                    Log.d(FD_TAG,FD_LOG, " updateUpperBodyDetection head j :" + j + ", i :" + i + " headInts[j] :" + headInts[j]);
                    j++;
                }

                j = 0;
                for (int i = 204; i < upperBodys.length; i += 4) {
                    if (j == torsoValidInts.length) {
                        break;
                    }
                    torsoValidInts[j] = byteArray2Int(upperBodys, i);
                    Log.d(FD_TAG,FD_LOG, " updateUpperBodyDetection torsoValid j :" + j + ", i :" + i + " torsoValidInts[j] :" + torsoValidInts[j]);
                    j++;
                }

                j = 0;
                for (int i = 244; i < upperBodys.length; i += 4) {
                    if (j == torsoInts.length) {
                        break;
                    }
                    torsoInts[j] = byteArray2Int(upperBodys, i);
                    Log.d(FD_TAG,FD_LOG," updateUpperBodyDetection torso j :" + j + ", i :" + i + " torsoInts[j] :" + torsoInts[j]);
                    j++;
                }
            } catch (Exception e) {
                Log.e(TAG, " updateUpperBodyDetection byteArray2Int occur exception");
                e.printStackTrace();
            }
        }
        Log.d(FD_TAG,FD_LOG, " updateUpperBodyDetection headNums :" + headNums);
        try {
            int num =0;
            if(headNums == 0){
                num = (mIsValidNum == 0 )? 1:2;
                mIsValidNum = num;
            }else{
                mIsValidNum = 0;
            }
            if(headNums !=0 || (headNums == 0 && mIsValidNum <2)) {
                mUI.onUpperBodyDetection(headNums, headInts, torsoValidInts, torsoInts);
            }
        } catch(Exception e) {
            Log.e(TAG, " updateUpperBodyDetection occur exception");
        }
    }



    private void updatePetDetection(CaptureResult result) {
        byte[] petresults = null;
        int[] headInts = null;
        int[] markInts = null;
        int[] torsoInts = null;
        int headNums = 0;
        try {
            petresults = result.get(petResults);
            Log.d(FD_TAG,FD_LOG,"petresults="+Arrays.toString(petresults));
        } catch (IllegalArgumentException e) {
            mIsPetDetectionSupported = false;
            Log.w(TAG, "can`t get vendorTag petResults :" + petResults);
        } catch (NullPointerException e) {
            Log.w(TAG, " petResults get NULL");
        }
        if (petresults != null) {
            int size = petresults.length;
            headInts = new int[40];
            torsoInts = new int[40];
            markInts = new int[60];
            Log.d(FD_TAG,FD_LOG, " petresults size :" + size);
            int j = 0;
              /// @brief Metadata for pet detection ROI results with respect to active array.
          /* struct FDMetadataPetResults
          //  {
                INT32         numHeads;                         /// 4 byte< Number of the detected head(s)
                INT32         petID[FDMaxFaceCount];            ///40 byte< ID of the pet ROI
                FDROIData     headROI[FDMaxFaceCount];          /// (4+4+16)*10=240< Array of stabilized pet head ROI data.
                FDROIData     fullROI[FDMaxFaceCount];          /// 240< Array of stabilized whole pet ROI data.
                FDPetLandmark faceLandmark[FDMaxFaceCount];     ///(4+24)*10=280 < Points that indicate pet face sparse landmarks.
            }
            */
          /*  struct FDROIData
            {
                BOOL        valid;      ///4 byte< indicator of whether pet head/body is detected.
                UINT32      confidence; ///4 byte< Score of the confidence for a detected pet.
                FDROIRegion ROIRegion;  ///4*4=16 byte < A detected pet region.
            } */
/*
            struct FDPetLandmark
            {
                BOOL    valid;                             /// 4 byte< Point that indicates the left eye is centered.
                FDPoint points[FDPetFaceSparseLMPointMax]; ///4*6=24 byte< Array of points where pet landmarks are centered.
            }
            */
            try {
                headNums = byteArray2Int(petresults, 0);
                Log.d(FD_TAG,FD_LOG,"headNums="+headNums);
                for (int i = 44; i < 284; i += 24) {
                    if (j == 40){
                        break;
                    }
                    int  valid = byteArray2Int(petresults, i);
                    Log.d(FD_TAG,FD_LOG,"valid="+valid+",i="+i);
                    if (valid > 0){
                        headInts[j] = byteArray2Int(petresults, i+8);
                        headInts[j+1] = byteArray2Int(petresults, i+12);
                        headInts[j+2] = byteArray2Int(petresults, i+16);
                        headInts[j+3] = byteArray2Int(petresults, i+20);
                        Log.d(FD_TAG,FD_LOG,"  head j :" + j + ", i :" + i + ", headInts[j] :" + headInts[j]
                        +","+headInts[j+1]+","+headInts[j+2]+","+headInts[j+3]+",petresults="+
                                petresults[i+8]+","+ petresults[i+12]+","+ petresults[i+16]+","+ petresults[i+20]);
                        j = j+4;
                    }
                }

                j = 0;
                for (int i = 284; i < 524; i += 24) {
                    if (j == 40){
                        break;
                    }
                    int  valid = byteArray2Int(petresults, i);
                    Log.d(FD_TAG,FD_LOG,"valid="+valid+",i="+i);
                    if (valid > 0){
                        torsoInts[j] = byteArray2Int(petresults, i+8);
                        torsoInts[j+1] = byteArray2Int(petresults, i+12);
                        torsoInts[j+2] = byteArray2Int(petresults, i+16);
                        torsoInts[j+3] = byteArray2Int(petresults, i+20);
                        Log.d(FD_TAG,FD_LOG, "  head j :" + j + ", i :" + i + ", torsoInts[j] :" + torsoInts[j]
                                +","+torsoInts[j+1]+","+torsoInts[j+2]+","+torsoInts[j+3]+",petresults="+
                                petresults[i+8]+","+ petresults[i+12]+","+ petresults[i+16]+","+ petresults[i+20]);;
                        j = j+4;
                    }
                }
                j = 0;
                markInts = new int[6*headNums];
                int inum = 524+6*headNums;
                for (int i = 524; i < inum; i += 28) {
                    if (j == 6*headNums){
                        break;
                    }
                    markInts[j] =byteArray2Int(petresults, i+4);
                    markInts[j+1] =byteArray2Int(petresults, i+8);
                    markInts[j+2] =byteArray2Int(petresults, i+12);
                    markInts[j+3] =byteArray2Int(petresults, i+16);
                    markInts[j+4] =byteArray2Int(petresults, i+20);
                    markInts[j+5] =byteArray2Int(petresults, i+24);
                    Log.d(FD_TAG,FD_LOG, " head j :" + j + ", i :" + i + " markInts[j] :" + markInts[j]
                            +","+markInts[j+1]+","+markInts[j+2]+","+markInts[j+3]
                            +","+markInts[j+4]+","+markInts[j+5]);
                    j=j+6;
                }
            } catch (Exception e) {
                Log.e(TAG, "  byteArray2Int occur exception e="+e);
                e.printStackTrace();
            }
        }
        try {
            mUI.onPetDetection(headInts, torsoInts,markInts);
        } catch(Exception e) {
            Log.e(TAG, "  occur exception e="+e);
        }
    }


    private void updateFaceView(final Face[] faces, final ExtendedFace[] extendedFaces) {
        mPreviewFaces = faces;
        mExFaces = extendedFaces;
        if (faces != null) {
            if (faces.length != 0) {
                    for (int i = 0; i < faces.length; i++){
                        if (faces[i] != null){
                            Log.d(FD_TAG,FD_LOG,"face i="+i+" ROI="+faces[i].getBounds().toString());
                        }
                    }
                mStickyFaces = faces;
                mStickyExFaces = extendedFaces;
            }
            if(extendedFaces != null){
                Log.d(FD_TAG,FD_LOG,"extendedFaces len="+extendedFaces.length+" faces.len="
                        +faces.length);
            }

            mUI.onFaceDetection(faces, extendedFaces);
        }
    }

    public boolean isSelfieFlash() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SELFIE_FLASH);
        return value != null && value.equals("on") && getMainCameraId() == FRONT_ID;
    }

    private void checkSelfieFlashAndTakePicture() {
        if (isSelfieFlash()) {
            mUI.startSelfieFlash();
            if (selfieThread == null) {
                selfieThread = new SelfieThread();
                selfieThread.start();
            }
        } else {
            takePicture();
        }
    }

    @Override
    public void onCountDownFinished() {
        mUI.enableShutter(true);
        checkSelfieFlashAndTakePicture();
        mUI.showUIAfterCountDown();
    }

    @Override
    public void onScreenSizeChanged(int width, int height) {

    }

    @Override
    public void onPreviewRectChanged(Rect previewRect) {

    }

    @Override
    public void updateCameraOrientation() {
        if (mDisplayRotation != CameraUtil.getDisplayRotation(mActivity)) {
            setDisplayOrientation();
        }
    }

    @Override
    public void waitingLocationPermissionResult(boolean result) {
        mLocationManager.waitingLocationPermissionResult(result);
    }

    @Override
    public void enableRecordingLocation(boolean enable) {
        String value = (enable ? RecordLocationPreference.VALUE_ON
                               : RecordLocationPreference.VALUE_OFF);
        mSettingsManager.setValue(SettingsManager.KEY_RECORD_LOCATION, value);
        mLocationManager.recordLocation(enable);
    }

    @Override
    public void setPreferenceForTest(String key, String value) {
        mSettingsManager.setValue(key, value);
        if (SettingsManager.KEY_ZOOM.equals(key)){
            mUI.setZoomTextSelect(Float.parseFloat(value));
            onZoomChanged(Float.parseFloat(value));
            return;
        }
        if(mCurrentSceneMode.mode == CameraMode.PRO_MODE) {
            if (key.equals(SettingsManager.KEY_FOCUS_DISTANCE)) {
                mSettingsManager.setProModeSliderValueForAutTest(key, value);
            }
            mUI.updateProUIForTest(key, value);
        }
    }

    @Override
    public void onPreviewUIReady() {
        updatePreviewSurfaceReadyState(true);
        if (mPaused || mIsRecordingVideo) {
            return;
        }
    }

    @Override
    public void onPreviewUIDestroyed() {
        updatePreviewSurfaceReadyState(false);
    }

    @Override
    public void onPreviewTextureCopied() {

    }

    @Override
    public void onCaptureTextureCopied() {

    }

    @Override
    public void onUserInteraction() {

    }

    @Override
    public boolean updateStorageHintOnResume() {
        return false;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
        int oldOrientation = mOrientation;
        mOrientation = CameraUtil.roundOrientation(orientation, mOrientation);
        Log.d(TAG,"oldOrientation="+oldOrientation+",mOrientation="+mOrientation);
        if (oldOrientation != mOrientation) {
            mUI.onOrientationChanged();
            mUI.setOrientation(mOrientation, true);
            if (mGapGraphView != null) {
                mGapGraphView.setRotation(-mOrientation);
            }
            if (mGraphViewRGB != null) {
                mGraphViewRGB.setRotation(-mOrientation);
            }
            if (mGraphViewR != null) {
                mGraphViewR.setRotation(-mOrientation);
            }
            if (mGraphViewGB != null) {
                mGraphViewGB.setRotation(-mOrientation);
            }
            if (mGraphViewB != null) {
                mGraphViewB.setRotation(-mOrientation);
            }
        }

        // need to re-initialize mGraphView to show histogram on rotate
        mGapGraphView = (Camera2RequestGapGraphView) mRootView.findViewById(R.id.graph_view_gap);
        mGraphViewRGB  = (Camera2RGBGraphView) mRootView.findViewById(R.id.graph_view_rgb);
        mGraphViewR  = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_r);
        mGraphViewGB = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_gb);
        mGraphViewB  = (Camera2GraphView) mRootView.findViewById(R.id.graph_view_b);
        bgstats_view = (Camera2BGBitMap) mRootView.findViewById(R.id.bg_stats_graph);
        bestats_view = (Camera2BEBitMap) mRootView.findViewById(R.id.be_stats_graph);
        rsstats_view = (Camera2RSBitMap) mRootView.findViewById(R.id.rs_stats_graph);
        mGraphViewR.setDataSection(0,256);
        mGraphViewGB.setDataSection(256,512);
        mGraphViewB.setDataSection(512,768);
        if(mGapGraphView != null){
            mGapGraphView.setAlpha(0.75f);
            mGapGraphView.setCaptureModuleObject(this);
            mGapGraphView.PreviewChanged();
        }
        mGraphViewRGB.setDataSection(0,768);
        if(mGraphViewRGB != null){
            mGraphViewRGB.setAlpha(0.75f);
            mGraphViewRGB.setCaptureModuleObject(this);
            mGraphViewRGB.PreviewChanged();
        }
        if(mGraphViewR != null){
            mGraphViewR.setAlpha(0.75f);
            mGraphViewR.setCaptureModuleObject(this);
            mGraphViewR.PreviewChanged();
        }
        if(mGraphViewGB != null){
            mGraphViewGB.setAlpha(0.75f);
            mGraphViewGB.setCaptureModuleObject(this);
            mGraphViewGB.PreviewChanged();
        }
        if(mGraphViewB != null){
            mGraphViewB.setAlpha(0.75f);
            mGraphViewB.setCaptureModuleObject(this);
            mGraphViewB.PreviewChanged();
        }
        if(bgstats_view != null){
            bgstats_view.setAlpha(1.0f);
            bgstats_view.setCaptureModuleObject(this);
            bgstats_view.PreviewChanged();
        }
        if(bestats_view != null){
            bestats_view.setAlpha(1.0f);
            bestats_view.setCaptureModuleObject(this);
            bestats_view.PreviewChanged();
        }
        if(rsstats_view != null){
            rsstats_view.setAlpha(1.0f);
            rsstats_view.setCaptureModuleObject(this);
            rsstats_view.PreviewChanged();
        }
    }

    public int getDisplayOrientation() {
        return mOrientation;
    }

    public int getSensorOrientation() {
        int degree = 0;
        if(getMainCameraCharacteristics() != null) {
            degree = getMainCameraCharacteristics().
                    get(CameraCharacteristics.SENSOR_ORIENTATION);
        }
        return degree;
    }

    @Override
    public void onShowSwitcherPopup() {

    }

    @Override
    public void onMediaSaveServiceConnected(MediaSaveService s) {
        if (mFirstTimeInitialized) {
            s.setListener(this);
            if (isClearSightOn()) {
                ClearSightImageProcessor.getInstance().setMediaSaveService(s);
            }
        }
    }

    @Override
    public boolean arePreviewControlsVisible() {
        return false;
    }

    @Override
    public void resizeForPreviewAspectRatio() {

    }

    @Override
    public void onSwitchSavePath() {
        mSettingsManager.setValue(SettingsManager.KEY_CAMERA_SAVEPATH, "1");
        RotateTextToast.makeText(mActivity, R.string.on_switch_save_path_to_sdcard,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        if (!pressed && mLongshotActive) {
            Log.d(TAG, "Longshot button up");
            mLongshotActive = false;
            if (mPostProcessor.isZSLEnabled()) {
                mPostProcessor.stopLongShot();
            } else {
                stopBurstShot();
            }
            mUI.enableVideo(!mLongshotActive);
        }
    }

    private void stopBurstShot() {
        Log.i(TAG, "stopBurstShot");
        try {
            int id = getMainCameraId();
            enableShutterAndVideoOnUiThread(id);
            if (mCaptureSession[id] == null) {
                return;
            }
            mCaptureSession[id].stopRepeating();
            mCaptureSession[id].setRepeatingRequest(mPreviewRequestBuilder[id]
                    .build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void updatePictureSize() {
        String pictureSize = mSettingsManager.getValue(SettingsManager.KEY_PICTURE_SIZE);
        int currentId = getMainCameraId();
        int rawFormat = mSettingsManager.getRawFormat() ;
        mPictureSize = parsePictureSize(pictureSize);
        String physicalId = "0";
        if(mSettingsManager.getRawReprocessPhysicalId() != null && !mSettingsManager.getRawReprocessPhysicalId().equals("logical")){
            physicalId = mSettingsManager.getRawReprocessPhysicalId();
        }
        if(PersistUtil.isRawReprocessQcfa()){
            List<Size> sizes = mSettingsManager.getSupportedQCFAMaxPictureSizeList(physicalId, ImageFormat.PRIVATE);
            if(sizes != null && sizes.size() != 0){
                mPictureSize = sizes.get(0);
            }
        }
        Size[] prevSizes = mSettingsManager.getSupportedOutputSize(currentId,
                SurfaceHolder.class);
        List<Size> prevSizeList = Arrays.asList(prevSizes);
        prevSizeList.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
        mSupportedMaxPictureSize = prevSizeList.get(0);
        Size[] yuvSizes = mSettingsManager.getSupportedOutputSize(currentId, ImageFormat.YUV_420_888);
        List<Size> yuvSizeList = Arrays.asList(yuvSizes);
        yuvSizeList.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
        for (int i = 0; i< mYUVCount; i++) {
            if(PersistUtil.isRawReprocessQcfa()){
                List<Size> sizes = mSettingsManager.getSupportedQCFAMaxPictureSizeList(physicalId, ImageFormat.YUV_420_888);
                if(sizes != null && sizes.size() != 0){
                    mYUVsize[i] = sizes.get(0);
                }
            }else{
                mYUVsize[i] = yuvSizeList.get(0);
            }
        }
        if( mRawCount == 1){
            Size[] rawSize = mSettingsManager.getSupportedOutputSize(Integer.parseInt(physicalId), rawFormat);
            if(PersistUtil.isRawReprocessQcfa()){
                List<Size> sizes = mSettingsManager.getSupportedQCFAMaxPictureSizeList(physicalId, rawFormat);
                if(sizes != null && sizes.size() != 0){
                    mRawSize[0] = sizes.get(0);
                }
            }else{
                mRawSize[0] = rawSize[0];
            }
        }
        mSupportedRawPictureSize = null;
        if(mSettingsManager.getQuadBayerSensorPrefEnabled() && mSaveRaw){
                mSupportedRawPictureSize = mSettingsManager.getQCFARawSize(String.valueOf(currentId),rawFormat);
        }
        if(mSupportedRawPictureSize == null) {
            Size[] rawSize = mSettingsManager.getSupportedOutputSize(currentId, rawFormat);
            Log.d(TAG, " rawsize==null? :" + (rawSize == null) + ",mSaveRaw=" + mSaveRaw + ",mSettingsManager.getRawFormat()=" + rawFormat);
            if ((rawSize == null || rawSize.length == 0 || rawFormat == 0)) {
                mSaveRaw = false;
            }
            if (!mSaveRaw) {
                rawSize = mSettingsManager.getSupportedOutputSize(currentId, ImageFormat.RAW10);
            }
            Size maxRawSize = getMaxRawSize();
            if (maxRawSize != null && (rawFormat == ImageFormat.RAW10 ||
                    (rawFormat == ImageFormat.RAW_SENSOR && isRawReprocess()))) {
                mSupportedRawPictureSize = maxRawSize;
            } else if ((mSupportedRawPictureSize == null || (rawFormat == ImageFormat.RAW_SENSOR && !isRawReprocess())) && rawSize != null) {
                mSupportedRawPictureSize = rawSize[0];
            }
        }
        if (mSupportedRawPictureSize != null) {
         Log.i(TAG, " supported " +  (rawFormat == ImageFormat.RAW_SENSOR ? "RAW16" :"RAW10") +" size is  "+  mSupportedRawPictureSize.toString());
        }
        mPreviewSize = getOptimalPreviewSize(mPictureSize, prevSizes);
        Size[] thumbSizes = mSettingsManager.getSupportedThumbnailSizes(currentId);
        mPictureThumbSize = getOptimalPreviewSize(mPictureSize, thumbSizes); // get largest thumb size
    }

    private Size getFullYUVSize(int id){
        Size[] yuvSizes = mSettingsManager.getSupportedOutputSize(id, ImageFormat.YUV_420_888);
        List<Size> yuvSizeList = Arrays.asList(yuvSizes);
        yuvSizeList.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
        return yuvSizeList.get(0);
    }

    private Size getMaxRawSize(){
        Set<String> physical_ids = mSettingsManager.getAllPhysicalCameraId();
        List<Size> allRawSize = new ArrayList<>();
        if(physical_ids != null && physical_ids.size() != 0) {
            for (String physicalId : physical_ids){
                Size[] rawSize = mSettingsManager.getSupportedOutputSize(Integer.parseInt(physicalId), ImageFormat.RAW10);
                if (rawSize != null && rawSize.length != 0) {
                    Log.d(TAG, "getMaxRawSize=" + rawSize[0].toString());
                    allRawSize.add(rawSize[0]);
                }
            }
            allRawSize.sort((o1,o2) -> o2.getWidth()*o2.getHeight() - o1.getWidth()*o1.getHeight());
            if(allRawSize.size() != 0){
                return allRawSize.get(0);
            }
        }
        return null;
    }

    public Size getThumbSize() {
        return mPictureThumbSize;
    }

    public boolean isRecordingVideo() {
        return mIsRecordingVideo;
    }

    public void setMute(boolean enable, boolean isValue) {
        if (!PersistUtil.needAudioEncoder()) return;
        AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        am.setMicrophoneMute(enable);
        if (isValue) {
            mIsMute = enable;
        }
    }

    public boolean isAudioMute() {
        return mIsMute;
    }

    private void updateVideoSize() {
        Intent intent = mActivity.getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int size = 0;
            int extraVideoQuality =
                    intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                size = CamcorderProfile.QUALITY_HIGH;
            } else {
                size = CamcorderProfile.QUALITY_LOW;
            }
            if (mSettingsManager.hasProfile(getMainCameraId(), size)) {
                mProfile = CamcorderProfile.get(getMainCameraId(), size);
            }
            mVideoSize = new Size(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        } else {
            String videoSize = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
            if (videoSize != null) {
                mVideoSize = parsePictureSize(videoSize);
            } else {
                mVideoSize = new Size(1920, 1080);
            }
            Point videoSize2 = PersistUtil.getCameraVideoSize();
            if (videoSize2 != null) {
                mVideoSize = new Size(videoSize2.x, videoSize2.y);
            }
        }
        Size[] prevSizes = mSettingsManager.getSupportedOutputSize(getMainCameraId(),
                MediaRecorder.class);
        mVideoPreviewSize = getOptimalVideoPreviewSize(mVideoSize, prevSizes);
        Point previewSize = PersistUtil.getCameraPreviewSize();
        if (previewSize != null) {
            mVideoPreviewSize = new Size(previewSize.x, previewSize.y);
        }
        Log.i(TAG, " final video Preview size = " + mVideoPreviewSize.toString()
                + ",videoSize is " + mVideoSize.toString());
    }

    private void updateVideoSnapshotSize() {
        mVideoSnapshotSize = getMaxPictureSizeLiveshot(getMainCameraId(),mVideoSize.getWidth(),
                mVideoSize.getHeight());
        if (getCurrenCameraMode() == CameraMode.CINEMATIC){
            mVideoSnapshotSize = mVideoSize;
        }
        String mlVideo = mSettingsManager.getValue(SettingsManager.KEY_ML_VIDEO);
        if(mlVideo != null && mlVideo.equals("on")){
            mVideoSnapshotSize = mVideoPreviewSize;
        }
        String videoSnapshot = PersistUtil.getVideoSnapshotSize();
        String[] sourceStrArray = videoSnapshot.split("x");
        if (sourceStrArray != null && sourceStrArray.length >= 2) {
            int width = Integer.parseInt(sourceStrArray[0]);
            int height = Integer.parseInt(sourceStrArray[1]);
            mVideoSnapshotSize = new Size(width, height);
        }
        Log.i(TAG, "updateVideoSnapshotSize final video snapShot size = " + mVideoSnapshotSize.toString());
        Size[] thumbSizes = mSettingsManager.getSupportedThumbnailSizes(getMainCameraId());
        mVideoSnapshotThumbSize = getOptimalPreviewSize(mVideoSnapshotSize, thumbSizes); // get largest thumb size
    }

    private void updatePhysicalVideoSnapshotSize() {
        if (!mSettingsManager.isMultiCameraEnabled())
            return;
        Set<String> ids = mSettingsManager.getAllPhysicalCameraId();
        String videoSnapshot = PersistUtil.getVideoSnapshotSize();
        String[] sourceStrArray = videoSnapshot.split("x");
        Size persistSize = null;
        if (sourceStrArray != null && sourceStrArray.length >= 2) {
            int width = Integer.parseInt(sourceStrArray[0]);
            int height = Integer.parseInt(sourceStrArray[1]);
            persistSize = new Size(width, height);
        }
        if (ids != null) {
            int i = 0;
            for (String id : ids){
                if (i >= PHYSICAL_CAMERA_COUNT)
                    break;
                if (persistSize != null){
                    mPhysicalVideoSnapshotSizes[i] = persistSize;
                } else {
                    if (mQuadBayerPhysicalIds.size() != 0 && mQuadBayerPhysicalIds.contains(id)) {
                        mPhysicalVideoSnapshotSizes[i] = mPhysicalVideoSizes[i];
                    } else {
                        mPhysicalVideoSnapshotSizes[i] = getMaxPictureSizeLiveshot(Integer.valueOf(id),
                                mPhysicalVideoSizes[i].getWidth(),mPhysicalVideoSizes[i].getHeight());
                    }
                }
                Log.d(TAG,"set Physical "+ id + " video snapshot size="+
                        mPhysicalVideoSnapshotSizes[i].toString());
                i++;
            }
        }

    }

    private boolean is4kSize(Size size) {
        return (size.getHeight() >= 2160 || size.getWidth() >= 3840);
    }

    private Size getMaxPictureSizeLiveshot(int cameraId, int videoWidth, int videoHeight) {
        Size[] sizes = mSettingsManager.getAllSupportedOutputSize(cameraId,
                mSettingsManager.isMaxConfigureSize(cameraId, new Size(videoWidth, videoHeight)));
        float ratio = (float) videoWidth / videoHeight;
        Size optimalSize = null;
        for (Size size : sizes) {
            float pictureRatio = (float) size.getWidth() / size.getHeight();
            if (Math.abs(pictureRatio - ratio) > 0.01) continue;
            if (optimalSize == null || size.getWidth() > optimalSize.getWidth()) {
                optimalSize = size;
            }
        }

        // Cannot find one that matches the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "getMaxPictureSizeLiveshot: no picture size match the aspect ratio");
            for (Size size : sizes) {
                if (optimalSize == null || size.getWidth() > optimalSize.getWidth()) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }

    private boolean isVideoSize1080P(Size size) {
        return (size.getHeight() == 1080 && size.getWidth() == 1920);
    }

    private void updateMaxVideoDuration() {
        String minutesStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_DURATION);
        int minutes = Integer.parseInt(minutesStr);
        if (minutes == -1) {
            // User wants lowest, set 30s */
            mMaxVideoDurationInMs = 30000;
        } else {
            // 1 minute = 60000ms
            mMaxVideoDurationInMs = 60000 * minutes;
        }
        mMaxDurationForCodec = mMaxVideoDurationInMs;
    }

    public void updateDeepZoomIndex(float zoom) {
        mZoomValue = zoom;
        applyZoomAndUpdate();
    }

    private void applyZoomAndUpdate() {
        long current = System.currentTimeMillis();
        if(current - mZoomTime > 24 && mZoomHandler != null){
            mZoomHandler.sendEmptyMessage(ZoomHandler.MSG_UPDATE_ZOOM_INSTANT);
            mZoomTime = current;
        }

    }

    private void updateZoom() {
        String zoomStr = mSettingsManager.getValue(SettingsManager.KEY_ZOOM);
        float zoom = Float.parseFloat(zoomStr);
        Log.d(TAG,"mZoomValue="+mZoomValue+",mOldMode"+mOldMode
        +",mCurrentSceneMode.mode="+mCurrentSceneMode.mode+",CURRENT_ID="+CURRENT_ID+
                ",mOldCameraId = "+mOldCameraId+",zoomStr="+zoomStr);
        if ( zoom > 0 ) {
            mZoomValue = zoom;
            mUI.updateZoomSeekBar(mZoomValue);
        }else if( zoom == 0 || (zoom < 0 && (mOldMode == null || !mOldMode.equals(mCurrentSceneMode.mode)
        || (mOldCameraId != CURRENT_ID)))){
            mZoomValue = 1.0f;
        }
        if (isDeepZoom()) {
            mZoomValue = mUI.getDeepZoomValue();
        }
        float[] zoomRatioRange = mSettingsManager.getSupportedRatioZoomRange(getMainCameraId());
        if (mCurrentSceneMode.mode == CameraMode.RTB || (isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
            zoomRatioRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                    getMainCameraId());
        }
            if (zoomRatioRange != null && zoomRatioRange[0] == zoomRatioRange[1]) {
                mZoomValue = zoomRatioRange[0];
            } else if (zoomRatioRange != null && zoomRatioRange[0] != zoomRatioRange[1]) {
                if (mZoomValue < zoomRatioRange[0]) {
                    mZoomValue = zoomRatioRange[0];
                }else if(mZoomValue > zoomRatioRange[1]){
                    mZoomValue = zoomRatioRange[1];
                }
            }
    }

    private List<CaptureRequest> createSSMBatchRequest(CaptureRequest.Builder requestBuilder) {
        List<CaptureRequest> ssmRequests = new ArrayList<CaptureRequest>();
        requestBuilder.removeTarget(mVideoPreviewSurface);
        requestBuilder.removeTarget(mVideoRecordingSurface);
        requestBuilder.addTarget(mVideoPreviewSurface);
        ssmRequests.add(requestBuilder.build());
        requestBuilder.removeTarget(mVideoPreviewSurface);
        requestBuilder.addTarget(mVideoRecordingSurface);
        int mSSMBatchSize = CameraUtil.getHighSpeedVideoConfigsLists(getMainCameraId());
        Log.d(TAG, "mSSMBatchSize is " + mSSMBatchSize);
        for (int i = 1; i < mSSMBatchSize; i++) {
            ssmRequests.add(requestBuilder.build());
        }
        return ssmRequests;
    }

    private final CameraCaptureSession.StateCallback mCCSSateCallback = new CameraCaptureSession
            .StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "recordingVideo session onConfigured");
            setCameraModeSwitcherAllowed(true);
            int cameraId = getMainCameraId();
            mCurrentSession = cameraCaptureSession;
            mCaptureSession[cameraId] = cameraCaptureSession;
            //APP could  check if  afState is anything other than INACTIVE , it should change the focus circle and skip the passive transient state.
            if (mLastResultAFState != CaptureResult.CONTROL_AF_STATE_INACTIVE && mFocusStateListener != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFocusStateListener.onFocusStatusUpdate(CaptureResult.CONTROL_AF_STATE_INACTIVE);
                    }
                });
            }
            updateFaceDetection();
            try {
                setUpVideoCaptureRequestBuilder(cameraId);
                mCurrentSession.setRepeatingRequest(mVideoRecordRequestBuilder.build(),
                        mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException | IllegalStateException e) {
             Log.e(TAG,e);
            }
            if (!mFrameProcessor.isFrameListnerEnabled() && !startVideoRecording()) {
                startRecordingFailed();
                return;
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            setCameraModeSwitcherAllowed(true);
            Log.i(TAG, " session mCCSSateCallback failed");
            Toast.makeText(mActivity, "Video Failed", Toast.LENGTH_SHORT).show();
        }
    };

    private void limitPreviewFPS() {
        try {
            List<CaptureRequest> burstList = new ArrayList<>();
            int fps = mSettingsManager.getVideoPreviewFPS();
            Log.d(TAG,"limit preview fps:" + PersistUtil.getPreviewFps() + ",fps" + fps + ",mHighSpeedCaptureRate:" + mHighSpeedCaptureRate);
            if((fps == 30 && mHighSpeedCaptureRate == 60) || (fps == 15 && mHighSpeedCaptureRate == 0)) {
                burstList.add(mVideoRecordRequestBuilder.build());
                mVideoRecordRequestBuilder.removeTarget(mVideoPreviewSurface);
                burstList.add(mVideoRecordRequestBuilder.build());
            }else if(fps == 15 && mHighSpeedCaptureRate == 60){
                burstList.add(mVideoRecordRequestBuilder.build());
                mVideoRecordRequestBuilder.removeTarget(mVideoPreviewSurface);
                burstList.add(mVideoRecordRequestBuilder.build());
                burstList.add(mVideoRecordRequestBuilder.build());
                burstList.add(mVideoRecordRequestBuilder.build());
            }else if(fps == 45){
                burstList.add(mVideoRecordRequestBuilder.build());
                burstList.add(mVideoRecordRequestBuilder.build());
                burstList.add(mVideoRecordRequestBuilder.build());
                mVideoRecordRequestBuilder.removeTarget(mVideoPreviewSurface);
                burstList.add(mVideoRecordRequestBuilder.build());
            }
            mCurrentSession.setRepeatingBurst(burstList, mCaptureCallback, mCameraHandler);
            mVideoRecordRequestBuilder.addTarget(mVideoPreviewSurface);
        } catch (CameraAccessException e) {
            Log.e(TAG, "limit preview fps failed.");
        }
    }
    private List<CaptureRequest> getHighSpeedList(CameraConstrainedHighSpeedCaptureSession session,CaptureRequest.Builder builder) throws CameraAccessException {
        List<CaptureRequest> highrequest = null;
        CaptureRequest request = builder.build();
        if (request == null) {
            throw new IllegalArgumentException("Input capture request must not be null");
        }
        String buffermode = mSettingsManager.getValue(SettingsManager.KEY_HFR_BUFFER_MODE);
        if(buffermode != null && buffermode.equals("1") && mSettingsManager.isSupportedSuperBuffer(getMainCameraId())){
            highrequest = createMyHighSpeedRequestList(request);
        }else{
            highrequest = session.createHighSpeedRequestList(request);
        }
        return highrequest;
    }
    private boolean surfaceForHwVideoEncoder(Surface sur){
        try {
            Class SurfaceUtilsClass = Class.forName("android.hardware.camera2.utils.SurfaceUtils");
            Method isSurfaceForHwMethod = SurfaceUtilsClass.getDeclaredMethod(
                    "isSurfaceForHwVideoEncoder", Surface.class);
            isSurfaceForHwMethod.setAccessible(true);
            boolean isHwSur = (boolean) isSurfaceForHwMethod.invoke(SurfaceUtilsClass, sur);
            return isHwSur;
        }catch(Exception e){
            Log.e(TAG,"Failed to invoke SurfaceUtils e="+e);
        }
        return false;
    }
    private Collection<Surface> getRequestTargets(CaptureRequest request){
        try {
            Class CaptureReqestClass = Class.forName("android.hardware.camera2.CaptureRequest");
            Method getTargetMethod = CaptureReqestClass.getDeclaredMethod("getTargets");
            getTargetMethod.setAccessible(true);
            Collection<Surface> surs = (Collection<Surface>) getTargetMethod.invoke(request);
            return surs;
        }catch(Exception e){
        Log.e(TAG,"e="+e);
            return null;
        }

    }
    private void setCHSRequestList(CaptureRequest.Builder builder,boolean value){
        try{
        Class CaptureReqestBuilderClass = Class.forName("android.hardware.camera2.CaptureRequest$Builder");
        Method setRequestListMethod = CaptureReqestBuilderClass.getDeclaredMethod
                ("setPartOfCHSRequestList",boolean.class);
        setRequestListMethod.setAccessible(true);
        //Object obj = CaptureReqestClass.getConstructor().newInstance();
        setRequestListMethod.invoke(builder,value);
        }catch(Exception e){
            Log.e(TAG,"e="+e);
        }
    }
    private Object getMetadataObj(Object cpy){
        try{
        Class CameraMetadataClass = Class.forName("android.hardware.camera2.impl.CameraMetadataNative");
        Object obj = CameraMetadataClass.getConstructor(CameraMetadataClass).newInstance(CameraMetadataClass.cast(cpy));
        return obj;
        }catch(Exception e){
            Log.e(TAG,"e="+e);
            return null;
        }
    }
     private Object getNativeCopyObj(CaptureRequest request){
        try{
         Class CaptureReqestClass = Class.forName("android.hardware.camera2.CaptureRequest");
         //Class CameraMetadataClass = Class.forName("android.hardware.camera2.impl.CameraMetadataNative");
         Method getNativeCpMethod = CaptureReqestClass.getDeclaredMethod("getNativeCopy");
         getNativeCpMethod.setAccessible(true);
         Object obj = getNativeCpMethod.invoke(request);
         return obj;
        }catch(Exception e){
            Log.e(TAG,"e="+e);
            return null;
        }
     }
     private String getLogicalCameraId(CaptureRequest request){
        try{
         Class CaptureReqestClass = Class.forName("android.hardware.camera2.CaptureRequest");
         Method getLogicalIdMethod = CaptureReqestClass.getDeclaredMethod("getLogicalCameraId");
         getLogicalIdMethod.setAccessible(true);
         String id =(String)getLogicalIdMethod.invoke(request);
         return id;
        }catch(Exception e){
            Log.e(TAG,"e="+e);
            return null;
        }
     }
     private CaptureRequest.Builder requestBuilder(Object meta,boolean reprocess,int sessionid,String cameraid,Set<String>cameraidset){
        try{
         Class CameraMetadataClass = Class.forName("android.hardware.camera2.impl.CameraMetadataNative");
         Class CaptureReqestBuilderClass = Class.forName("android.hardware.camera2.CaptureRequest$Builder");
         CaptureRequest.Builder builder = (CaptureRequest.Builder)CaptureReqestBuilderClass.getConstructor(CameraMetadataClass,boolean.class,
                 int.class,String.class,Set.class).newInstance(meta,reprocess,sessionid,cameraid,cameraidset);
         return builder;
        }catch(Exception e){
            Log.e(TAG,"e="+e);
            return null;
        }
     }
    private List<CaptureRequest> createMyHighSpeedRequestList(CaptureRequest request)
            throws CameraAccessException {
        if (request == null) {
            throw new IllegalArgumentException("Input capture request must not be null");
        }
        int requestListSize = 1;
        Collection<Surface> outputSurfaces = getRequestTargets(request);//request.getTargets();
        List<CaptureRequest> requestList = new ArrayList<CaptureRequest>();

        // Prepare the Request builders: need carry over the request controls.
        // First, create a request builder that will only include preview or recording target.
        //CameraMetadataNative requestMetadata = new CameraMetadataNative(request.getNativeCopy());
        // Note that after this step, the requestMetadata is mutated (swapped) and can not be used
        // for next request builder creation.
 /*       CaptureRequest.Builder singleTargetRequestBuilder = new CaptureRequest.Builder(
                getMetadataObj(getNativeCopyObj(request)), *//*reprocess*//*false, -1,
                getLogicalCameraId(request), *//*physicalCameraIdSet*//* null);*/

        CaptureRequest.Builder singleTargetRequestBuilder = requestBuilder(getMetadataObj(getNativeCopyObj(request)), false, -1,
                getLogicalCameraId(request),null);
        // Carry over userTag, as native metadata doesn't have this field.
        singleTargetRequestBuilder.setTag(request.getTag());
        // Overwrite the capture intent to make sure a good value is set.
        Iterator<Surface> iterator = outputSurfaces.iterator();
        Surface firstSurface = iterator.next();
        Surface secondSurface = null;

        if (outputSurfaces.size() == 1 && surfaceForHwVideoEncoder(firstSurface)) {
            singleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
        } else {
            // Video only, or preview + video
            singleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);
        }
        setCHSRequestList(singleTargetRequestBuilder,true);
       // singleTargetRequestBuilder.setPartOfCHSRequestList(/*partOfCHSList*/true);//keep

        // Second, Create a request builder that will include both preview and recording targets.
        CaptureRequest.Builder doubleTargetRequestBuilder = null;
        if (outputSurfaces.size() == 2) {
            // Have to create a new copy, the original one was mutated after a new
            // CaptureRequest.Builder creation.
           // requestMetadata = new CameraMetadataNative(request.getNativeCopy());
/*            doubleTargetRequestBuilder = new CaptureRequest.Builder(
                    getMetadataObj(getNativeCopyObj(request)), *//*reprocess*//*false, -1,
                    getLogicalCameraId(request), *//*physicalCameraIdSet*//*null);*/

            doubleTargetRequestBuilder = requestBuilder(getMetadataObj(getNativeCopyObj(request)),false,-1, getLogicalCameraId(request),null);
            doubleTargetRequestBuilder.setTag(request.getTag());
            doubleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);
            doubleTargetRequestBuilder.addTarget(firstSurface);
            secondSurface = iterator.next();
            doubleTargetRequestBuilder.addTarget(secondSurface);
            setCHSRequestList(doubleTargetRequestBuilder,true);
            //doubleTargetRequestBuilder.setPartOfCHSRequestList(/*partOfCHSList*/true);
            // Make sure singleTargetRequestBuilder contains only recording surface for
            // preview + recording case.
            Surface recordingSurface = firstSurface;
            if (!surfaceForHwVideoEncoder(recordingSurface)) {
                recordingSurface = secondSurface;
            }
            singleTargetRequestBuilder.addTarget(recordingSurface);
        } else {
            // Single output case: either recording or preview.
            singleTargetRequestBuilder.addTarget(firstSurface);
        }
        // Generate the final request list.
        for (int i = 0; i < requestListSize; i++) {
            if (i == 0 && doubleTargetRequestBuilder != null) {
                // First request should be recording + preview request
                requestList.add(doubleTargetRequestBuilder.build());
            } else {
                requestList.add(singleTargetRequestBuilder.build());
            }
        }
        return Collections.unmodifiableList(requestList);
    }
    private final CameraCaptureSession.StateCallback mSessionListener = new CameraCaptureSession
            .StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "mSessionListener session onConfigured");
            mCreateSessionLatency = System.currentTimeMillis()- mCreateSessionLatency;
            if(mActivity.getPerformenceTest()) {
                mHasMapTimes.put("createSession->onConfigured",mCreateSessionLatency);
                if(mSessionAfterRecord != 0){
                    mHasMapTimes.put("Total",System.currentTimeMillis() - mStartedTime);
                }
            }
            if(!PersistUtil.enableMediaRecorder() && !mOnlyVideoEncoder && !waitForAudioPrepare()){
                quitVideoToPhotoWithError("media codec prepare failed");
                return;
            }
            setCameraModeSwitcherAllowed(true);
            if(!mSettingsManager.isLogicalEnable() && mSettingsManager.getSinglePhysicalCamera() == null){
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mUI.hideLogicalSurface();
                    }
                });
            }
            int cameraId = getMainCameraId();
            mCurrentSession = cameraCaptureSession;
            mCaptureSession[cameraId] = cameraCaptureSession;
            //APP could  check if  afState is anything other than INACTIVE , it should change the focus circle and skip the passive transient state.
            if (mLastResultAFState != CaptureResult.CONTROL_AF_STATE_INACTIVE && mFocusStateListener != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFocusStateListener.onFocusStatusUpdate(CaptureResult.CONTROL_AF_STATE_INACTIVE);
                    }
                });
            }
            updateFaceDetection();
            mFirstPreviewLoaded = false;
            // Create slow motion request list
            List<CaptureRequest> slowMoRequests = null;
            try {
                setUpVideoCaptureRequestBuilder(cameraId);
                if (mPaused || mCurrentSession == null || mCameraDevice[cameraId] == null) {
                    return;
                }
                applyAICameraStrengthAndUpdate();

                if (isHighSpeedRateCapture()) {
                    slowMoRequests = mSuperSlomoCapture ?
                            createSSMBatchRequest(mVideoRecordRequestBuilder) :
                    getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,mVideoRecordRequestBuilder);
                    mCurrentSession.setRepeatingBurst(slowMoRequests, mCaptureCallback,
                            mCameraHandler);
                } else {
                    int previewFPS = mSettingsManager.getVideoPreviewFPS();
                    if ((previewFPS != 60 && mHighSpeedCaptureRate == 60) || (mHighSpeedCaptureRate == 0 && previewFPS == 15)) {
                        if (PersistUtil.enableMediaRecorder()) {
                            mVideoRecordRequestBuilder.addTarget(mVideoRecordingSurface);
                        }
                        limitPreviewFPS();
                        if (PersistUtil.enableMediaRecorder()) {
                            mVideoRecordRequestBuilder.removeTarget(mVideoRecordingSurface);
                        }
                        if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                            mVideoRecordRequestBuilder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                            Log.v(TAG, " onConfigured mVideoRecordRequestBuilder set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                        }
                    } else {
                        if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                            mVideoRecordRequestBuilder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                            Log.v(TAG, " onConfigured 2 mVideoRecordRequestBuilder set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                        }
                        mCurrentSession.setRepeatingRequest(mVideoRecordRequestBuilder.build(),
                                mCaptureCallback, mCameraHandler);
                    }
                }
                if(mSessionAfterRecord == 0) {
                    mFirstRequestLatency = System.currentTimeMillis();
                }else{
                    mSessionAfterRecord = 0;
                }
                setVideoState(VideoState.VIDEO_PREVIEW);
                enableVideoButton(true);
                if (getCurrenCameraMode() == CameraMode.CINEMATIC) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.hideFlashButton();
                        }
                    });
                }
                if(mIntentMode == mIntentMode) {
                    mActivity.runOnUiThread(()->{
                        seBlurConfigSlideVisible();
                    });
                }

            } catch (CameraAccessException | IllegalStateException e) {
                Log.w(TAG, "video-setRepeatingRequest fail=",  e.fillInStackTrace());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            enableVideoButton(true);
            setCameraModeSwitcherAllowed(true);
            Log.i(TAG, "mSessionListener session failed");
            Toast.makeText(mActivity, "Video Failed", Toast.LENGTH_SHORT).show();
        }
    };

    private void createCameraSessionWithSessionConfiguration(int cameraId,
                 List<OutputConfiguration> outConfigurations, InputConfiguration inputConfig, CameraCaptureSession.StateCallback listener,
                 Handler handler, CaptureRequest.Builder initialRequest) {
        getOptMode();
        Log.i(TAG, "mStreamConfigOptMode: " + mStreamConfigOptMode);
        createCaptureSessionWithSessionConfiguration(mCameraDevice[cameraId], mStreamConfigOptMode, outConfigurations, inputConfig, listener, handler, initialRequest);
    }

    private void getOptMode() {
        mStreamConfigOptMode = 0;
        String zzHDR = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HDR_VALUE);
        boolean zzHdrStatue = zzHDR.equals("1");
        if (zzHdrStatue) {
            mStreamConfigOptMode = STREAM_CONFIG_MODE_ZZHDR;
        }
        String value = mSettingsManager.getValue(SettingsManager.KEY_FOVC_VALUE);
        if (value != null && Boolean.parseBoolean(value)) {
            mStreamConfigOptMode = mStreamConfigOptMode | STREAM_CONFIG_MODE_FOVC;
        }
        String valueFS2 = mSettingsManager.getValue(SettingsManager.KEY_SENSOR_MODE_FS2_VALUE);
        if (valueFS2 != null) {
            int intValue = Integer.parseInt(valueFS2);
            if (intValue == 1) {
                mStreamConfigOptMode |= STREAM_CONFIG_MODE_FS2;
                Log.v(TAG, "createRegularSession valueFS2 OptMode:" + mStreamConfigOptMode);
            }
        }
         Log.v(TAG, "createRegularSession OptMode:" + mStreamConfigOptMode);
    }

    private void createRegularSession(int cameraId) throws CameraAccessException {
        List<OutputConfiguration> outConfigurations = new ArrayList<>();
        OutputConfiguration videoSnapshotConfig = new OutputConfiguration(
                mVideoSnapshotImageReader.getSurface());
        if( mSettingsManager.getSavePictureFormat() == mSettingsManager.JPEG_R_FORMAT){
            videoSnapshotConfig.setDynamicRangeProfile(2);
        }
        Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_CAMCORDER);
        if (mSettingsManager.getPhysicalCameraId() != null || (ids != null && ids.size() != 0)) {
           if(mSettingsManager.getPhysicalCameraId() != null){
                mVideoRecordRequestBuilder.removeTarget(mVideoPreviewSurface);
                if (mSettingsManager.isLogicalEnable()) {
                    mVideoRecordRequestBuilder.addTarget(mUI.getPhysicalSurfaces().get(0));
                    outConfigurations.add(new OutputConfiguration(mUI.getPhysicalSurfaces().get(0)));
                    if(!is8KInMulti) {
                        outConfigurations.add(videoSnapshotConfig);
                        outConfigurations.add(new OutputConfiguration(mVideoRecordingSurface));
                    }
                }
                List<Surface> previewSurfaces = mUI.getPhysicalSurfaces();
                if (previewSurfaces.size() != 0) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.hideSurfaceView();
                        }
                    });
                }
                for (int i = 1; i < mUI.getPhysicalSurfaces().size(); i++) {
                    mVideoRecordRequestBuilder.addTarget(previewSurfaces.get(i));
                }
               outConfigurations.addAll(getPhysicalPreviewOutput());
            } else {
               mVideoRecordRequestBuilder.addTarget(mVideoPreviewSurface);
               OutputConfiguration videoPrevConfig = new OutputConfiguration(mVideoPreviewSurface);
               if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                   videoPrevConfig.addSensorPixelModeUsed(
                           CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
                   videoPrevConfig.addSensorPixelModeUsed(
                           CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                   Log.v(TAG, " video preview OutputConfiguration set SENSOR_PIXEL_MODE_DEFAULT and " +
                           "SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
               }
               String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
               if (previewProfile != null && !previewProfile.equals("0")) {
                   videoPrevConfig.setDynamicRangeProfile(Long.parseLong(previewProfile));
               }
               outConfigurations.add(videoPrevConfig);
            }
            for (int i =0; i < mPhysicalMediaRecorders.length; i++) {
                if (mPhysicalMediaRecorders[i] != null) {
                    mVideoRecordRequestBuilder.removeTarget(mPhysicalMediaSurfaces[i]);
                }
            }
            outConfigurations.addAll(getPhysicalVideoOutputConfiguration());
        } else {
            mVideoRecordRequestBuilder.addTarget(mVideoPreviewSurface);
            if (mSettingsManager.isHeifWriterEncoding() && mLiveShotInitHeifWriter != null) {
                mLiveShotOutput = new OutputConfiguration(
                        mLiveShotInitHeifWriter.getInputSurface());
                mLiveShotOutput.enableSurfaceSharing();
                outConfigurations.add(mLiveShotOutput);
            } else {
                String encoder  = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER);
                if (!("mvhevc").equals(encoder)) {
                    if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                        videoSnapshotConfig.addSensorPixelModeUsed(
                                CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                        Log.v(TAG, " videoSnapShot OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                    }
                    outConfigurations.add(videoSnapshotConfig);
                }
            }
            if (mVideoRecordingSurface != null) {
                OutputConfiguration videoConfig = new OutputConfiguration(mVideoRecordingSurface);
                if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                    videoConfig.addSensorPixelModeUsed(
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                    Log.v(TAG, " video OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                }
                if (mSettingsManager.isDynamicRangeTenBitSupported()) {
                    String encoderProfile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
                    if (encoderProfile != null) {
                        String profile = SettingsManager.VIDEO_ENCODER_PROFILE_MAP.get(encoderProfile);
                        Log.v(TAG, "OutputConfiguration set video encoderProfile :" +
                                encoderProfile + ", profile :" + profile);
                        if (!profile.equals("0")) {
                            videoConfig.setDynamicRangeProfile(Long.parseLong(profile));
                        }
                    }
                }
                if ((("dolby").equals(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER))) && mIntentMode == INTENT_MODE_NORMAL) {
                    Log.i(TAG, " setting DOLBY for video stream in non-HFR");
                    videoConfig.setDynamicRangeProfile(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM);
                }
                outConfigurations.add(videoConfig);
            }
            OutputConfiguration videoPrevConfig = new OutputConfiguration(mVideoPreviewSurface);
            if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                videoPrevConfig.addSensorPixelModeUsed(
                        CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
                videoPrevConfig.addSensorPixelModeUsed(
                        CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                Log.v(TAG, " video preview OutputConfiguration set SENSOR_PIXEL_MODE_DEFAULT and " +
                        "SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
            }
            String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
            if (previewProfile != null && !previewProfile.equals("0")) {
                videoPrevConfig.setDynamicRangeProfile(Long.parseLong(previewProfile));
            }
            outConfigurations.add(videoPrevConfig);
        }
        getOptMode();
        setTimeStamp(outConfigurations,TIMESTAMP_BASE_SENSOR);
        mSettingInitLatency = System.currentTimeMillis() - mSettingInitLatency;
        if(mActivity.getPerformenceTest()) {
            if (mIsCloseCamera || mFromOnOpened) {
                mHasMapTimes.put("onOpened->createSession", mSettingInitLatency);
                mFromOnOpened = false;
            } else if(mSessionAfterRecord == 0){
                mHasMapTimes.put("swipeMode->createSession", System.currentTimeMillis() - mStartedTime);
            }else{
                mHasMapTimes.put("endStop->createSession", System.currentTimeMillis() - mSessionAfterRecord);
            }
        }
        try {

            SessionConfiguration sessionConfig = new SessionConfiguration(
                    SESSION_REGULAR | mStreamConfigOptMode, outConfigurations,
                    new HandlerExecutor(mCameraHandler), mSessionListener);
            sessionConfig.setSessionParameters(mVideoRecordRequestBuilder.build());
            String colorSpace = mSettingsManager.getValue(SettingsManager.KEY_COLOR_SPACE);
            if (colorSpace != null && !colorSpace.equals("0")) {
                sessionConfig.setColorSpace(SettingsManager.COLOR_SPACE_MAP.get(colorSpace));
            }
            boolean isSessionSupported = checkSessionSupported(sessionConfig);
            if(isSessionSupported) {
                mCreateSessionLatency = System.currentTimeMillis();
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- call createCaptureSession");
                mCameraDevice[cameraId].createCaptureSession(sessionConfig);
                if (TRACE_DEBUG) Trace.endSection();
            }else{
                setCameraModeSwitcherAllowed(true);
            }
        } catch (Exception e) {
            Log.e(TAG,e);
        }
    }
    private void setTimeStamp(List<OutputConfiguration> outConfigurations,int timestamp){
        if(PersistUtil.isSetTimeStamp()){
            if(CameraMode.VIDEO == mCurrentSceneMode.mode && mSettingsManager.getVideoFPS() > 30){
                return;
            }
            try{
                Log.d(TAG,"setTimeStamp outConfigurations.size()="+outConfigurations.size());
                for(int i = 0; i < outConfigurations.size(); i++) {
                    OutputConfiguration config = outConfigurations.get(i);
                    config.setTimestampBase(timestamp);
                }
            }catch (IllegalArgumentException | NoSuchMethodError e){
                Log.d(TAG,"setTimeStamp exception ="+e);
            }
        }
    }
    private boolean needWaitSurface(){
        String colorSpace = mSettingsManager.getValue(SettingsManager.KEY_COLOR_SPACE);
        if((colorSpace != null && !colorSpace.equals("0")) ||
                (mSettingsManager.getPhysicalCameraId() != null ||
                mSettingsManager.getSinglePhysicalCamera() != null ||
                isClearSightOn()
                || CaptureUI.USE_TEXTURE_VIEW_TO_PREVIEW || needYUVStream()
                )){
            return  true;
        }
        return false;
    }
    private void createCaptureSessionWithSessionConfiguration(CameraDevice camera, int opMode,
                                                              List<OutputConfiguration> outConfigurations,
                                                              InputConfiguration inputConfig,
                                                              CameraCaptureSession.StateCallback listener,
                                                              Handler handler,
                                                              CaptureRequest.Builder initialRequest) {
        mSettingInitLatency = System.currentTimeMillis() - mSettingInitLatency;
        if(mActivity.getPerformenceTest() && (mIsCloseCamera || mFromOnOpened)) {
            mHasMapTimes.put("onOpened->createSession",mSettingInitLatency);
            mFromOnOpened = false;
        }else if(mActivity.getPerformenceTest()){
            mHasMapTimes.put("swipeMode->createSession",System.currentTimeMillis() - mStartedTime);
        }
        setTimeStamp(outConfigurations,TIMESTAMP_BASE_SENSOR);
        SessionConfiguration sessionConfig = new SessionConfiguration(opMode, outConfigurations,
                new HandlerExecutor(handler), listener);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- set capture session param");
        sessionConfig.setSessionParameters(initialRequest.build());
        if (TRACE_DEBUG) Trace.endSection();
        String colorSpace = mSettingsManager.getValue(SettingsManager.KEY_COLOR_SPACE);
        if (colorSpace != null && !colorSpace.equals("0")) {
            sessionConfig.setColorSpace(SettingsManager.COLOR_SPACE_MAP.get(colorSpace));
        }
        if (inputConfig != null) {
            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- setInputConfiguration");
            sessionConfig.setInputConfiguration(inputConfig);
            if (TRACE_DEBUG) Trace.endSection();
        }

      boolean sessionSupported = checkSessionSupported(sessionConfig);

        if(sessionSupported) {
            try {
                mCreateSessionLatency = System.currentTimeMillis();
                camera.createCaptureSession(sessionConfig);
            } catch (CameraAccessException e) {
                Log.e(TAG, "createCaptureSession  error:"+ e);
            }
        }else{
            setCameraModeSwitcherAllowed(true);
        }
    }

    private boolean checkSessionSupported(SessionConfiguration sessionConfig) {
        String cameraId = String.valueOf(getMainCameraId());
        boolean session_supported = true;
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            boolean supportSessionConfigurationQuery = characteristics.get(
                    CameraCharacteristics.INFO_SESSION_CONFIGURATION_QUERY_VERSION)
                    > Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
            if (!supportSessionConfigurationQuery) {
                Log.i(TAG, "Camera " + cameraId + " doesn't support session configuration query");
                return true;
            }
        } catch (CameraAccessException e) {
        }
        String errorTitle = "isSessionConfigurationSupported False";
        try {
            CameraDeviceSetup cameraDeviceSetup = manager.getCameraDeviceSetup(cameraId);
            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,createSession -- isSessionConfigurationSupported");
            session_supported = cameraDeviceSetup.isSessionConfigurationSupported(sessionConfig);
            if (TRACE_DEBUG) Trace.endSection();
            Log.i(TAG, " isSessionConfigurationSupported :" + session_supported + ",cameraid is " + cameraId);
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.w(TAG, " check isSessionConfigurationSupported exception =" + e);
            StringBuilder errstr = new StringBuilder();
            errstr.append("Catch exception: ");
            if (e instanceof CameraAccessException) {
                errstr.append("CameraAccessException,camera device is no longer connected or has encountered a fatal error");
            } else if (e instanceof IllegalArgumentException) {
                errstr.append("IllegalArgumentException, session configuration is invalid, including, if it " +
                        "contains certain non-supported features queryable via CameraCharacteristics.");
            }
            Log.i(TAG, "isSessionConfigurationSupported exception:" + errstr);
            CameraUtil.showErrorDialog(mActivity, errstr.toString(),errorTitle);
            return false;
        }
        if (!session_supported) {
            CameraUtil.showErrorDialog(mActivity, "Unsupported stream/feature combination, please modify Settings.",errorTitle);
        }

        return session_supported;
    }


    private void createHighSpeedSession(int cameraID) throws CameraAccessException {
        int optionMode = isSSMEnabled() ? STREAM_CONFIG_SSM : SESSION_HIGH_SPEED;
        List<OutputConfiguration> outConfigurations = new ArrayList<>();
        OutputConfiguration videoPreviewConfig = new OutputConfiguration(mVideoPreviewSurface);
        OutputConfiguration videoRecordConfig = new OutputConfiguration(mVideoRecordingSurface);
        if (mSettingsManager.isMaxConfigureSize(cameraID, mVideoSize)) {
            videoPreviewConfig.addSensorPixelModeUsed(
                    CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
            videoPreviewConfig.addSensorPixelModeUsed(
                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
            Log.v(TAG, " video preview OutputConfiguration set SENSOR_PIXEL_MODE_DEFAULT and " +
                    "SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
            videoRecordConfig.addSensorPixelModeUsed(
                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
            Log.v(TAG, " video record OutputConfiguration set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
        }
        if (mSettingsManager.isDynamicRangeTenBitSupported()) {
            String encoderProfile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
            if (encoderProfile != null) {
                String profile = SettingsManager.VIDEO_ENCODER_PROFILE_MAP.get(encoderProfile);
                Log.v(TAG, "OutputConfiguration set video encoderProfile :" +
                        encoderProfile + ", profile :" + profile);
                if (!profile.equals("0")) {
                    videoRecordConfig.setDynamicRangeProfile(Long.parseLong(profile));
                }
            }
        }
        if (("dolby").equals(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER))) {
            Log.i(TAG, " setting DOLBY for video stream in HFR");
            videoRecordConfig.setDynamicRangeProfile(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM);
        }
        outConfigurations.add(videoPreviewConfig);
        outConfigurations.add(videoRecordConfig);
        setTimeStamp(outConfigurations,TIMESTAMP_BASE_SENSOR);
        mSettingInitLatency = System.currentTimeMillis() - mSettingInitLatency;
        if(mActivity.getPerformenceTest()) {
            if ((mIsCloseCamera || mFromOnOpened) && mSessionAfterRecord == 0 ) {
                mHasMapTimes.put("onOpened->createSession", mSettingInitLatency);
                mFromOnOpened = false;
            } else if(mSessionAfterRecord == 0){
                mHasMapTimes.put("swipeMode->createSession", System.currentTimeMillis() - mStartedTime);
            } else {
                mHasMapTimes.put("endStop->createSession", System.currentTimeMillis() - mSessionAfterRecord);
            }
        }

        try {
            SessionConfiguration sessionConfig = new SessionConfiguration(optionMode,
                    outConfigurations, new HandlerExecutor(mCameraHandler), mSessionListener);
            sessionConfig.setSessionParameters(mVideoRecordRequestBuilder.build());
            String colorSpace = mSettingsManager.getValue(SettingsManager.KEY_COLOR_SPACE);
            if (colorSpace != null && !colorSpace.equals("0")) {
                sessionConfig.setColorSpace(SettingsManager.COLOR_SPACE_MAP.get(colorSpace));
            }
            boolean sessionSupported = checkSessionSupported(sessionConfig);
            if (sessionSupported) {
                mCreateSessionLatency = System.currentTimeMillis();
                mCameraDevice[cameraID].createCaptureSession(sessionConfig);
            } else {
                setCameraModeSwitcherAllowed(true);
            }
        } catch (Exception exception) {
            Log.e(TAG,exception);
        }
    }

    private class HandlerExecutor implements Executor {
        private final Handler ihandler;

        public HandlerExecutor(Handler handler) {
            ihandler = handler;
        }

        @Override
        public void execute(Runnable runCmd) {
            ihandler.post(runCmd);
        }
    }

    public boolean isAFLocked(){
        return mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE;
    }

    private boolean triggerVideoRecording(final int cameraId) {
        if (null == mCameraDevice[cameraId] || mCurrentSession == null || mCurrentSessionClosed) {
            return false;
        }
        mStartRecordingTime = System.currentTimeMillis();
        mRecordingPausingTime = 0;
        Log.i(TAG, "triggerVideoRecording " + cameraId);

        mActivity.updateStorageSpaceAndHint();
        if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.w(TAG, "Storage issue, ignore the start request");
            mStartRecPending = false;
            mIsRecordingVideo = false;
            mIsPreviewingVideo = true;
            mRecordingStoped = true;
            Toast.makeText(mActivity, "Storage space is not enough", Toast.LENGTH_SHORT).show();
            return false;
        }
        mStartRecPending = true;
        mIsRecordingVideo = true;
        mRecordingPausing = false;
        mIsPreviewingVideo = false;
        mSSMCaptureCompleteFlag = false;
        mRecordingStoped = false;
        checkAndPlayRecordSound(cameraId, true);

        try {
            if (mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                mUI.clearFocus();
            }
            mUI.hideUIwhileRecording();
            Set<String> physicalRecorderId = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER);
            Log.i(TAG, " physicalRecorderId=" + physicalRecorderId + ",is8KInMulti=" + is8KInMulti);
            if (physicalRecorderId != null && physicalRecorderId.size() > 0) {
                cleanupEmptyFile();
                if (!is8KInMulti) {
                    setupMediaRecorder(getMainCameraId());
                }
                setUpPhysicalMediaRecorder();
                Set<String> physicalId = mSettingsManager.getPhysicalCameraId();
                if (!is8KInMulti) {
                    if ((physicalRecorderId != null && physicalId == null) ||
                            (physicalRecorderId != null && physicalId != null && !physicalId.containsAll(physicalRecorderId))) {
                        mStartRecPending = false;
                        mIsRecordingVideo = false;
                        mIsPreviewingVideo = true;
                        mRecordingStoped = true;
                        warningToast("Please enable physical cameras of outputs first");
                        return false;
                    }
                    if (mSettingsManager.isLogicalEnable()) {
                        mVideoRecordRequestBuilder.addTarget(mVideoRecordingSurface);
                    }
                } else {
                    if (physicalId != null) {
                        mStartRecPending = false;
                        mIsRecordingVideo = false;
                        mIsPreviewingVideo = true;
                        mRecordingStoped = true;
                        warningToast("8K video only support one logical preview with 1080");
                        return false;
                    }
                }
                for (int i = 0; i < mPhysicalMediaRecorders.length; i++) {
                    if (mPhysicalMediaRecorders[i] != null) {
                        mVideoRecordRequestBuilder.addTarget(mPhysicalMediaSurfaces[i]);
                    }
                }
            } else if (!mSettingsManager.isMultiCameraEnabled() || (
                    mSettingsManager.isMultiCameraEnabled() && mSettingsManager.isLogicalEnable()
            )) {
                if (PersistUtil.enableMediaRecorder()) {
                    cleanupEmptyFile();
                    setupMediaRecorder(getMainCameraId());
                    mVideoRecordRequestBuilder.addTarget(mVideoRecordingSurface);
                    if(mHighSpeedCapture && !isVariableFPSEnabled() && mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS) {
                        mVideoRecordRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                mHighSpeedFPSRange);
                    }
                    if (mSettingsManager.isMaxConfigureSize(cameraId, mVideoSize)) {
                        // SENSOR_PIXEL_MODE_DEFAULT
                        mVideoRecordRequestBuilder.set(CaptureRequest.SENSOR_PIXEL_MODE,
                                CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
                        Log.v(TAG, "VideoRecordRequestBuilder set SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION");
                    }
                } else {
                    mVideoRecordRequestBuilder.addTarget(mVideoRecordingSurface);
                }
            } else {
                mStartRecPending = false;
                mIsRecordingVideo = false;
                mIsPreviewingVideo = true;
                mRecordingStoped = true;
                warningToast("Please enable physical cameras of outputs first");
                return false;
            }


            int previewFPS = mSettingsManager.getVideoPreviewFPS();
            if ((previewFPS != 60 && mHighSpeedCaptureRate == 60) || (mHighSpeedCaptureRate == 0 && previewFPS == 15)) {
                limitPreviewFPS();
            } else {
                if (isHighSpeedRateCapture()) {
                    List<CaptureRequest> requests = mSuperSlomoCapture ?
                            createSSMBatchRequest(mVideoRecordRequestBuilder) :
                            getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,mVideoRecordRequestBuilder);
                    mCurrentSession.setRepeatingBurst(requests, mCaptureCallback,
                            mCameraHandler);
                } else {
                    mCurrentSession.setRepeatingRequest(mVideoRecordRequestBuilder.build(),
                            mCaptureCallback, mCameraHandler);
                }
            }
            mVideoFilePath = mVideoFilename;
            mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[cameraId]);
            if (!mFrameProcessor.isFrameListnerEnabled() && !startVideoRecording() ||
                    !mIsRecordingVideo) {
                startRecordingFailed();
                return false;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                        mUI.clearFocus();
                    }
                    mUI.resetPauseButton();
                    mRecordingTotalTime = 0L;
                    mRecordingStartTime = SystemClock.uptimeMillis();
                    String encoder = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER);
                    if (isHighSpeedRateCapture() || ("mvhevc".equals(encoder))) {
                        mUI.enableShutter(false);
                    } else {
                        mUI.enableShutter(true);
                    }
                    mUI.showRecordingUI(true, false);
                    updateRecordingTime();
                    keepScreenOn();
                }
            });
            mUI.setSoundEffectsForRecording(false);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException | CameraAccessException | IOException e) {
            Log.e(TAG, e.toString());
            quitRecordingWithError("IllegalArgumentException");
        }
        mStartRecPending = false;
        return true;
    }

    private void startRecordingFailed() {
        if (PersistUtil.enableMediaRecorder()) {
            releaseMediaRecorder();
        } else {
            stopCodecThreads();
            releaseMediaCodec();
        }
        mHandler.post(new Runnable() {
             @Override
             public void run() {
                 mUI.showUIafterRecording();
                 mFrameProcessor.setVideoOutputSurface(null);
                 restartSession(false);
             }
        });
    }


    private void quitVideoToPhotoWithError(String msg) {
        setCameraModeSwitcherAllowed(true);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivity,"Could not start video record.\n " +
                        msg, Toast.LENGTH_LONG).show();
            }
        });
        if (PersistUtil.enableMediaRecorder()) {
            releaseMediaRecorder();
        } else {
            stopCodecThreads();
            releaseMediaCodec();
        }
        releaseAudioFocus();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mUI.switchToPhotoModeDueToError(true);
            }
        });
    }

    private void quitRecordingWithError(String msg) {
        Toast.makeText(mActivity,"Could not start recording.\n " +
                msg, Toast.LENGTH_LONG).show();
        if (PersistUtil.enableMediaRecorder()) {
            releaseMediaRecorder();
        } else {
            stopCodecThreads();
            releaseMediaCodec();
        }
        releaseAudioFocus();
        mStartRecPending = false;
        mIsRecordingVideo = false;
        mRecordingStoped = true;
        mUI.showUIafterRecording();
        mFrameProcessor.setVideoOutputSurface(null);
        if(mCameraModeSwitcherAllowed) {
            restartSession(true);
        }
    }

    private boolean sendSSMRequestBuilder() {
        try {
            mVideoRecordRequestBuilder.set(ssmInterpFactor, mInterpFactor);
            mVideoRecordRequestBuilder.set(ssmCaptureStart, 1);
            mCurrentSession.captureBurst(createSSMBatchRequest(mVideoRecordRequestBuilder),
                    mCaptureCallback, mCameraHandler);
            mVideoRecordRequestBuilder.set(ssmCaptureStart, 0);
            mCurrentSession.setRepeatingBurst(createSSMBatchRequest(mVideoRecordRequestBuilder),
                    mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.e(TAG,e);
            return false;
        }
        return true;
    }

    private boolean startMediaRecorder() {
        if (mMediaRecorder == null && !mSettingsManager.isMultiCameraEnabled()) {
            Log.e(TAG, "Fail to initialize media recorder");
            mStartRecPending = false;
            mIsRecordingVideo = false;
            mRecordingStoped = true;
            return false;
        }
        long startMediaRecord = System.currentTimeMillis();

        try {
            if (mMediaRecorder != null)
                mMediaRecorder.start(); // Recording is now started
            startPhysicalRecorder();
            mRecordingStarted = true;
            if(mActivity.getPerformenceTest()){
                mHasMapTimes.put("startRecorder->endStart",System.currentTimeMillis() - startMediaRecord);
                mHasMapTimes.put("Total",System.currentTimeMillis() - mStartedTime);
            }
            Log.i(TAG, "StartRecordingVideo done. Time=" +
                    (System.currentTimeMillis() - mStartRecordingTime) + "ms");
        } catch (RuntimeException e) {
            Toast.makeText(mActivity, "Could not start recording.\n " +
                    "Can't start video recording.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Can't start video recording =", e.fillInStackTrace());
            releaseMediaRecorder();
            releaseAudioFocus();
            mStartRecPending = false;
            mIsRecordingVideo = false;
            mRecordingStoped = true;
            return false;
        }
        if (isSSMEnabled() && !sendSSMRequestBuilder()) {
            return false;
        }
        return true;
    }

    public boolean startVideoRecording() {
        if (mUnsupportedResolution == true ) {
            Log.v(TAG, "Unsupported Resolution according to target");
            mStartRecPending = false;
            mIsRecordingVideo = false;
            mRecordingStoped = true;
            return false;
        }
        int[] list = {0x40800000, 0X3AC};
        if(mPostProcessor.isJniAPISupported() && mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE).equals("HEVCProfileMain10HDR10Plus"))
            mPostProcessor.nativePerfLockAcq(2, 0, list, list.length);
        requestAudioFocus();
        if (PersistUtil.enableMediaRecorder()) {
            if (!startMediaRecorder()) {
                startRecordingFailed();
                return false;
            }
        } else {
            mVideoEncoder.start();
            //start threads of MediaCodec
            if (!mOnlyVideoEncoder){
                mAudioEncoder.start();
                mAudioRecord.startRecording();
                startAudioDecoder();
                startAudioEncoder();
            }
            startVideoEncoder();
            Log.i(TAG, "StartRecordingVideo done. Time=" +
                    (System.currentTimeMillis() - mStartRecordingTime) + "ms");
            setVideoState(VideoState.VIDEO_START);
        }
        mRecordingStarted = true;
        return true;
    }

    private void startPhysicalRecorder() throws RuntimeException{
        if (mSettingsManager.getPhysicalFeatureEnableId
                (SettingsManager.KEY_PHYSICAL_CAMCORDER) != null) {
            Log.i(TAG,"startPhysicalRecorder");
            for (MediaRecorder recorder:mPhysicalMediaRecorders){
                if (recorder != null){
                    recorder.start();
                }
            }
        }
    }

    private void stopPhysicalRecorder() throws RuntimeException{
        if (mSettingsManager.getPhysicalFeatureEnableId
                (SettingsManager.KEY_PHYSICAL_CAMCORDER) != null) {
            Log.i(TAG,"stopPhysicalRecorder");
            for (MediaRecorder recorder:mPhysicalMediaRecorders){
                if (recorder != null){
                    recorder.setOnErrorListener(null);
                    recorder.setOnInfoListener(null);
                    recorder.stop();
                    recorder.reset();
                }
            }
        }
    }

    private void releasePhysicalRecorder() throws RuntimeException{
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder");
        if (mSettingsManager.getPhysicalFeatureEnableId
                (SettingsManager.KEY_PHYSICAL_CAMCORDER) != null) {
            Log.d(TAG,"releasePhysicalRecorder");
            for (MediaRecorder recorder:mPhysicalMediaRecorders){
                if (recorder != null){
                    recorder.reset();
                    recorder.release();
                    recorder = null;
                }
            }
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void updateTimeLapseSetting() {
        String value = mSettingsManager.getValue(SettingsManager
                .KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        if (value == null) return;
        int time = Integer.parseInt(value);
        mTimeBetweenTimeLapseFrameCaptureMs = time;
        mCaptureTimeLapse = mTimeBetweenTimeLapseFrameCaptureMs != 0;
        mUI.showTimeLapseUI(mCaptureTimeLapse);
    }

    private void updateHFRSetting() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (value == null) return;
        if (value.equals("off")) {
            mHighSpeedCapture = false;
            mHighSpeedCaptureRate = 0;
            mSuperSlomoCapture = false;
        } else {
            mHighSpeedCapture = true;
            String mode = value.substring(0, 3);
            mSuperSlomoCapture = mode.equals("2x_") || mode.equals("4x_");
            mHighSpeedRecordingMode = mode.equals("hsr") || mSuperSlomoCapture;
            if (mSuperSlomoCapture) {
                mInterpFactor = Integer.parseInt(value.substring(0, 1));
            }
            mHighSpeedCaptureRate = Integer.parseInt(value.substring(3));
        }
    }

    public boolean isHSRMode() {
        return mHighSpeedRecordingMode && !mSuperSlomoCapture;
    }

    public int getHighSpeedCaptureRate() {
        return mHighSpeedCaptureRate;
    }

    private int calculateBitRate(int width, int height) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        int fps = 30;
        if (value != null && (!value.equals("off"))) {
            fps = mHighSpeedCaptureRate;
        }

        double bitrate = Math.round(((double)884 * width * height * fps)/((double) 3840 * 2160 * 30));
        Log.i(TAG, "calculate bitrate for apv is " + bitrate);
        return ((int)bitrate > 2000) ? 2000 *1000 *1000 : (int)bitrate *1000 *1000;
    }

    private void updateProgressBar(boolean show) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUI.toggleProgressBar(show);
            }
        });
    }

    private void setUpVideoCaptureRequestBuilder(int cameraId) throws CameraAccessException{
        if(mVideoRecordRequestBuilder == null){
            Log.d(TAG, "mVideoRecordRequestBuilder is null.");
            mVideoRecordRequestBuilder = getRequestBuilder(CameraDevice.TEMPLATE_RECORD,
                    cameraId,mSettingsManager.getPhysicalCameraId());
        }
        setTag(mVideoRecordRequestBuilder, "" + cameraId + "-" + getCurrenCameraMode().name());
        if (mHighSpeedCapture && !isVariableFPSEnabled()) {
            if (mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS) {
                mVideoRecordRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        mHighSpeedPreviewFPSRange);
            } else {
                mVideoRecordRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mHighSpeedFPSRange);
            }

        }
        if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
            mVideoRecordRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            applyVideoCommentSettings(mVideoRecordRequestBuilder, cameraId);
        } else {
            //relock af ae when lock
            autoFocusTrigger(cameraId);
            applyAERegions(mVideoRecordRequestBuilder, cameraId);
            applySettingsForLockExposure(mVideoRecordRequestBuilder, cameraId);
            applyAntiBandingLevel(mVideoRecordRequestBuilder);
            applyNoiseReduction(mVideoRecordRequestBuilder);
            applyColorEffect(mVideoRecordRequestBuilder);
            applyVideoFlash(mVideoRecordRequestBuilder, cameraId);
            applyFaceDetection(mVideoRecordRequestBuilder);
            applyZoom(mVideoRecordRequestBuilder, cameraId);
            applyTouchTrackFocus(mVideoRecordRequestBuilder);
            applyToneMapping(mVideoRecordRequestBuilder);
        }
    }

    private void setUpVideoPreviewRequestBuilder(Surface surface, int cameraId) {
        try {
            mVideoPreviewRequestBuilder = getRequestBuilder(
                    CameraDevice.TEMPLATE_PREVIEW,cameraId,mSettingsManager.getPhysicalCameraId());
        } catch (CameraAccessException e) {
            Log.w(TAG, "setUpVideoPreviewRequestBuilder, Camera access failed");
            return;
        }
        setTag(mVideoPreviewRequestBuilder, "" + cameraId + "-" + getCurrenCameraMode().name());
        if (mSettingsManager.getPhysicalCameraId() != null) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    mUI.hideSurfaceView();
                }
            });
            mUI.buildPhysicalSurfaces();
            List<Surface> previewSurfaces = mUI.getPhysicalSurfaces();
            if(mSettingsManager.isLogicalEnable()){
                mVideoPreviewRequestBuilder.addTarget(previewSurfaces.get(0));
            }
            for (int i=1;i < mUI.getPhysicalSurfaces().size();i++){
                mVideoPreviewRequestBuilder.addTarget(previewSurfaces.get(i));
            }
        } else {
            mVideoPreviewRequestBuilder.addTarget(surface);
        }
        if (mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
            mVideoPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mControlAFMode);
        }
        if (!isVariableFPSEnabled()) {
            if (mHighSpeedCapture) {
                if (mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS) {
                    mVideoPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            mHighSpeedPreviewFPSRange);
                } else {
                    mVideoPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            mHighSpeedFPSRange);
                }
            } else {
                Range fps = new Range(30, 30);
                mVideoPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
            }

        }
        applyVideoCommentSettings(mVideoPreviewRequestBuilder, cameraId);
    }

    private void applyVariableFPS(CaptureRequest.Builder builder) {
        if (isVariableFPSEnabled()) {
            try {
                dynamicFpsConfig = new float[]{2.0f, 30.0f, 60.0f, 0.0f , 0.0f};
                builder.set(dynamicFSPConfigKey, dynamicFpsConfig);
                dynamicRange = new Range(30, 60);
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, dynamicRange);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void lockAfAeForRequestBuilder(CaptureRequest.Builder builder, int id){
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_CANCEL);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_START);
        applyAFRegions(builder, id);
        applyAERegions(builder, id);
        applySettingsForLockExposure(builder, id);
    }

    private void applyVideoCommentSettings(CaptureRequest.Builder builder, int cameraId) {
        if (mUI.getZoomFixedSupport()) {
            applyZoomRatio(builder, mZoomValue, cameraId);
        } else {
            applyZoom(builder, cameraId);
        }
        if (!mSettingsManager.isMultiCameraEnabled()) {
            if(mLockAFAE != LOCK_AF_AE_STATE_LOCK_DONE) {
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                String value = mSettingsManager.getValue(SettingsManager.KEY_LOWLIGHT_BOOST);
                if(value == null || !value.equals("1")) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                }
            }else{
                lockAfAeForRequestBuilder(builder, cameraId);
            }
            applyAntiBandingLevel(builder);
            applyNoiseReduction(builder);
            applyVideoFlash(builder, cameraId);
            applyFaceDetection(builder);
            applyTouchTrackFocus(builder);
            applyToneMapping(builder);
            applyHistogram(builder);
            applyBGStats(builder);
            applyBEStats(builder);
            applyPdnetToggle(builder);
            applyAWBCCTAndAgain(builder);
            applyAIBlurConfigs(builder);
            applyExposure(builder);
            applyInStantZoom(builder);
            applyAICameraStrength(builder);
            applyIsoAndExposureTime(builder);
        }
        applyColorEffect(builder);
    }

    private void applyCaptureMFNR(CaptureRequest.Builder builder) {
        if (mSettingsManager.isMultiCameraEnabled()) {
            Set<String> mfnr_ids = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_MFNR);
            int noiseReduMode = CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY;
            if (mfnr_ids != null) {
                builder.set(custom_noise_reduction, (byte) 0x01);
                for (String id:mfnr_ids) {
                    try {
                        builder.setPhysicalCameraKey(CaptureRequest.NOISE_REDUCTION_MODE,
                                noiseReduMode,id);
                    } catch (Exception e) {
                        Log.w(TAG, EXCEPTION_LOG,"capture can`t find vendor NOISE_REDUCTION_MODE tag");
                    }
                }
                Set<String> allPhysicalIds = mSettingsManager.getAllPhysicalCameraId();
                String value = mSettingsManager.getValue(SettingsManager.KEY_PHYSICAL_MFNR);
                for (String physical : allPhysicalIds) {
                    if (!value.contains(physical)) {
                        try {
                            builder.setPhysicalCameraKey(CaptureRequest.NOISE_REDUCTION_MODE,
                                    CameraMetadata.NOISE_REDUCTION_MODE_FAST,physical);
                        } catch (Exception e) {
                            Log.w(TAG, EXCEPTION_LOG,"capture can`t find vendor NOISE_REDUCTION_MODE tag");
                        }
                    }
                }
            } else {
                noiseReduMode = CameraMetadata.NOISE_REDUCTION_MODE_FAST;
            }
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE,noiseReduMode);
        } else {

            int noiseReduMode = (isMFNREnabled() ? CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY :
                    CameraMetadata.NOISE_REDUCTION_MODE_FAST);
            String frameStr = mSettingsManager.getKeyValue(mSettingsManager.KEY_CAPTURE_MFNR_FRAME);
            int frameValue = 3;
            frameValue = CameraUtil.strToInt(frameStr,frameValue);
            Log.i(TAG, "applyCaptureMFNR mfnrEnable :" + isMFNREnabled() + ", noiseReduMode :"
                    + noiseReduMode +",frameStr="+frameStr+",framevalue="+frameValue);
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, noiseReduMode);
            if (isMFNREnabled()) {
                try {
                    builder.set(custom_noise_reduction, (byte) 0x01);
                    builder.set(CaptureModule.mfnrFrameNO, frameValue);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, EXCEPTION_LOG,"capture can`t find vendor tag:MFNumOfFrames or custom_noise_reduction");
                }
            }
        }
    }

    private void applyIsAfLock(boolean isLock){
        CaptureRequest.Builder builder = mPreviewRequestBuilder[mCurrentSceneMode.getCurrentId()];
        int id = mCurrentSceneMode.getCurrentId();
        if (!checkSessionAndBuilder(mCaptureSession[id], builder)) {
            return;
        }
        try {
            builder.set(CaptureModule.isAfLock, (byte)(isLock? 0x01 : 0x00));
            if(id == MONO_ID && !canStartMonoPreview()) {
                mCaptureSession[id].capture(builder.build(), mCaptureCallback, mCameraHandler);
            } else {
                CameraCaptureSession session = mCaptureSession[id];
                if (session instanceof CameraConstrainedHighSpeedCaptureSession) {
                    List list = getHighSpeedList((CameraConstrainedHighSpeedCaptureSession)session,builder);
                    ((CameraConstrainedHighSpeedCaptureSession) session).setRepeatingBurst(list
                            , mCaptureCallback, mCameraHandler);
                } else if (isSSMEnabled()) {
                    session.setRepeatingBurst(createSSMBatchRequest(builder),
                            mCaptureCallback, mCameraHandler);
                } else {
                    mCaptureSession[id].setRepeatingRequest(builder
                            .build(), mCaptureCallback, mCameraHandler);
                }

            }
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG,e);
        }
    }

    private void applyLivePreview(CaptureRequest.Builder builder) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_LIVE_PREVIEW);
        Log.v(TAG, "applyLivePreview livePreviewValue :" + value );
        if (value != null) {
            int intValue = Integer.parseInt(value);
            try {
                builder.set(CaptureModule.livePreview, intValue);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "cannot find vendor tag: " + livePreview.toString());
            }
        }
    }

    private void applyStatsNNControl(CaptureRequest.Builder builder) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_STATSNN_CONTROL);
        String valueForCinematic = mSettingsManager.getValue(SettingsManager.KEY_STATSNN_CONTROL_FOR_CINEMATIC);
        Log.v(TAG, "applyStatsNNControl statsnn control :" + value );
        if (value != null) {
            byte statsnn = (byte)(Integer.parseInt(value) == 1 ? 0x01 : 0x00);
            try {
                builder.set(CaptureModule.qcam3NNControl, statsnn);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + CaptureModule.qcam3NNControl);
                try{
                    builder.set(CaptureModule.statsNNControl, statsnn);
                }catch (IllegalArgumentException ex) {
                    Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + CaptureModule.statsNNControl);
                }
            }
        }
        if (valueForCinematic != null && mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            byte statsnn = (byte)(Integer.parseInt(valueForCinematic) == 1 ? 0x01 : 0x00);
            try {
                builder.set(CaptureModule.qcam3NNControl, statsnn);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + CaptureModule.qcam3NNControl);
                try{
                    builder.set(CaptureModule.statsNNControl, statsnn);
                }catch (IllegalArgumentException ex) {
                    Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + CaptureModule.statsNNControl);
                }
            }
        }
    }

    private void applyPdnetToggle(CaptureRequest.Builder builder) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_PDNET_TOGGLE);
        Log.v(TAG, "applyPdnet control :" + value );
        if (value != null) {
            byte pdnet = (byte)(Integer.parseInt(value) == 1 ? 0x01 : 0x00);
            try {
                builder.set(pdnet_control, pdnet);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, EXCEPTION_LOG,"cannot find vendor tag: " + livePreview.toString());
            }
        }
    }

    private void applyCaptureBurstFps(CaptureRequest.Builder builder) {
        try {
            Log.v(TAG, " applyCaptureBurstFps burst fps mLongshotActive :" + mLongshotActive +
                    ", value :" + (byte)(mLongshotActive ? 0x01 : 0x00));
            builder.set(CaptureModule.capture_burst_fps, (byte)(mLongshotActive ? 0x01 : 0x00));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "cannot find vendor tag: " + capture_burst_fps.toString());
        }
    }

    private void setOpModeForVideoStream(int cameraId) {
        int index = getSensorTableHFRRange();
        if (index != -1) {
            Log.v(TAG, " getSensorTableHFRRange index :" + index);
            Method method_setVendorStreamConfigMode = null;
            try {
                if (method_setVendorStreamConfigMode == null) {
                    method_setVendorStreamConfigMode = CameraDevice.class.getDeclaredMethod(
                            "setVendorStreamConfigMode", int.class);
                }
                method_setVendorStreamConfigMode.invoke(mCameraDevice[cameraId], index);
            } catch (Exception exception) {
                Log.w(TAG, EXCEPTION_LOG,"setOpModeForVideoStream method is not exist");
            }
        }
    }

    private void updateVideoFlash(int id) {
        if (!mIsRecordingVideo && !mIsPreviewingVideo || mCurrentSessionClosed) return;
        applyVideoFlash(mVideoRecordRequestBuilder, id);
        applyVideoFlash(mVideoPreviewRequestBuilder, id);
        CaptureRequest captureRequest = null;
        try {
            captureRequest = mVideoRecordRequestBuilder.build();
            if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                CameraConstrainedHighSpeedCaptureSession session =
                        (CameraConstrainedHighSpeedCaptureSession) mCurrentSession;
                List requestList = getHighSpeedList(session,mVideoRecordRequestBuilder);
                session.setRepeatingBurst(requestList, mCaptureCallback, mCameraHandler);
            } else if (isSSMEnabled()) {
                mCurrentSession.setRepeatingBurst(createSSMBatchRequest(mIsRecordingVideo ?
                                mVideoRecordRequestBuilder : mVideoPreviewRequestBuilder),
                                mCaptureCallback, mCameraHandler);
            } else {
                mCurrentSession.setRepeatingRequest(captureRequest, mCaptureCallback,
                        mCameraHandler);
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e);
        }
    }

    private void applyVideoFlash(CaptureRequest.Builder builder, int id) {
        if (mSettingsManager.isFlashSupported(id)) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_FLASH_MODE);
            if (value == null) return;
            builder.set(CaptureRequest.FLASH_MODE, value.equals("on") && mUI.getFilmstripLayout().getVisibility() != View.VISIBLE ?
                    CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
           setFlashLevel(builder);
        } else {
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
        applyLowLightBoost(builder);

    }

    private void applyNoiseReduction(CaptureRequest.Builder builder) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_NOISE_REDUCTION);
        if (value == null) return;
        int noiseReduction = SettingTranslation.getNoiseReduction(value);
        builder.set(CaptureRequest.NOISE_REDUCTION_MODE, noiseReduction);
    }

    private void applyVideoStabilization(CaptureRequest.Builder builder, boolean isDisabled) {
        String value = isDisabled ? "off" : "on";
        Log.i(TAG, "applyEIS set CONTROL_VIDEO_STABILIZATION_MODE to " + value);
        if (isDisabled) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest
                    .CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        } else {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest
                    .CONTROL_VIDEO_STABILIZATION_MODE_ON);
        }
    }

    private void applyEISHorizonLevelEnable(CaptureRequest.Builder builder) {
        if (mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            return;
        }
        try {
            int enable = 1;
            String eisHorizonLevelEnable = mSettingsManager.getValue(
                    SettingsManager.KEY_EIS_HORIZON_LEVEL_ENABLE);
            String photoEIS = mSettingsManager.getValue(SettingsManager.KEY_PHOTO_EIS_VALUE);
            if ("0".equals(eisHorizonLevelEnable) ||
                    (isEISDisable() && mCurrentSceneMode.mode == CameraMode.VIDEO) ||
                    (isPhotoEISDisable() && mCurrentSceneMode.mode == CameraMode.DEFAULT)) {
                enable = 0;
            }
            Log.v(TAG, "applyEISHorizonLevelEnable enable :" + enable);
            builder.set(horizon_level_control, enable);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            Log.w(TAG, EXCEPTION_LOG,"applyEISHorizonLevelEnable no vendorTag: " + horizon_level_control);
        }
    }

    private boolean isVideoEncoderProfileSupported() {
        return !mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE).equals("off");
    }

    private long getTimeLapseVideoLength(long deltaMs) {
        // For better approximation calculate fractional number of frames captured.
        // This will update the video time at a higher resolution.
        double numberOfFrames = (double) deltaMs / mTimeBetweenTimeLapseFrameCaptureMs;
        return (long) (numberOfFrames / mProfile.videoFrameRate * 1000);
    }

    private void updateRecordingTime() {
        if (!mIsRecordingVideo) {
            return;
        }
        if (mRecordingPausing) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime + mRecordingTotalTime;


        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0
                && delta >= mMaxVideoDurationInMs - 60000);

        long deltaAdjusted = delta;
        if (countdownRemainingTime) {
            deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
        }
        String text;

        long targetNextUpdateDelay;
        if (!mCaptureTimeLapse) {
            text = CameraUtil.millisecondToTimeString(deltaAdjusted, false);
            targetNextUpdateDelay = 1000;
        } else {
            // The length of time lapse video is different from the length
            // of the actual wall clock time elapsed. Display the video length
            // only in format hh:mm:ss.dd, where dd are the centi seconds.
            text = CameraUtil.millisecondToTimeString(getTimeLapseVideoLength(delta), true);
            targetNextUpdateDelay = mTimeBetweenTimeLapseFrameCaptureMs;
        }
        mUI.setRecordingTime(text);
        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = mActivity.getResources().getColor(countdownRemainingTime
                    ? R.color.recording_time_remaining_text
                    : R.color.recording_time_elapsed_text);

            mUI.setRecordingTimeTextColor(color);
        }

        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    private void pauseVideoRecording() {
        Log.v(TAG, "pauseVideoRecording");
        if(!PersistUtil.enableMediaRecorder()){
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, 1);
            if(!mOnlyVideoEncoder) {
                mAudioEncoder.setParameters(params);
            }
            mVideoEncoder.setParameters(params);
        }
        mRecordingPausing = true;
        mRecordingPauseTime = SystemClock.uptimeMillis();
        mRecordingTotalTime += mRecordingPauseTime - mRecordingStartTime;
        String value = mSettingsManager.getValue(SettingsManager.KEY_EIS_VALUE);
        boolean noNeedEndofStreamWhenPause = value != null && value.equals("V3");
        // As EIS is not supported for HFR case (>=120 )
        // and FOVC also currently don’t require this for >=120 case
        // so use noNeedEndOfStreamInHFR to control
        boolean noNeedEndOfStreamInHFR = mHighSpeedCapture &&
                ((int)mHighSpeedFPSRange.getUpper() >= HIGH_SESSION_MAX_FPS);
        if (noNeedEndofStreamWhenPause || noNeedEndOfStreamInHFR) {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                mMediaRecorder.pause();
                int previewFPS = mSettingsManager.getVideoPreviewFPS();
                if ((previewFPS != 60 && mHighSpeedCaptureRate == 60) || (mHighSpeedCaptureRate == 0 && previewFPS == 15)) {
                    limitPreviewFPS();
                }
            } else {
                setVideoState(VideoState.VIDEO_PAUSE);

            }
            for (MediaRecorder mediaRecorder: mPhysicalMediaRecorders) {
                if (mediaRecorder != null) {
                    mediaRecorder.pause();
                }
            }
        } else {
            setEndOfStream(false, false);
        }
        applyZoomAndUpdate();
    }

    private void resumeVideoRecording() {
        Log.v(TAG, "resumeVideoRecording");
        if(!PersistUtil.enableMediaRecorder()){
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, 0);
            if(!mOnlyVideoEncoder) {
                mAudioEncoder.setParameters(params);
            }
            mVideoEncoder.setParameters(params);
        }
        mRecordingPausing = false;
        mRecordingStartTime = SystemClock.uptimeMillis();
        mRecordingPausingTime += mRecordingStartTime - mRecordingPauseTime;
        if (mHighSpeedCapture && !mHighSpeedRecordingMode) {
            mHighRecordingPausingTime = mRecordingPausingTime * mHighSpeedCaptureRate / 30;
            Log.d(TAG, "HFR pause time is " + mHighRecordingPausingTime);
        }

        updateRecordingTime();
        setEndOfStream(true, false);

        if (PersistUtil.enableMediaRecorder()) {
            if (!ApiHelper.HAS_RESUME_SUPPORTED) {
                if (mMediaRecorder != null)
                    mMediaRecorder.start();
                for (MediaRecorder mediaRecorder: mPhysicalMediaRecorders){
                   if (mediaRecorder != null){
                       mediaRecorder.start();
                   }
                }
                Log.d(TAG, "resumeVideoRecording done.");
            } else {
                try {
                    Method resumeRec = Class.forName("android.media.MediaRecorder").getMethod("resume");
                    resumeRec.invoke(mMediaRecorder);
                } catch (Exception e) {
                    Log.v(TAG, "resume method not implemented");
                }
            }
        } else {
            setVideoState(VideoState.VIDEO_RESUME);
        }
        try {
            Method resumeRec = Class.forName("android.media.MediaRecorder").getMethod("resume");
            for (MediaRecorder mediaRecorder: mPhysicalMediaRecorders){
                if (mediaRecorder != null){
                    resumeRec.invoke(mediaRecorder);
                }
            }
        } catch (Exception e) {
            Log.v(TAG, "resume method not implemented");
        }
        applyZoomAndUpdate();
    }

    private void setEndOfStream(boolean isResume, boolean isStopRecord) {
        CaptureRequest.Builder captureRequestBuilder = mVideoRecordRequestBuilder;
        try {
            if (isResume) {
                try {
                    if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                        CameraConstrainedHighSpeedCaptureSession session =
                                (CameraConstrainedHighSpeedCaptureSession) mCurrentSession;
                        List requestList = getHighSpeedList(session,mVideoRecordRequestBuilder);
                        mCurrentSession.setRepeatingBurst(requestList, mCaptureCallback, mCameraHandler);
                    }else {
                        captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                        Log.i(TAG, "Set endofstream TAG to 0");
                        mCurrentSession.setRepeatingRequest(captureRequestBuilder.build(),
                                mCaptureCallback, mCameraHandler);
                    }
                } catch(IllegalArgumentException | IllegalStateException e) {
                    Log.w(TAG, "can not find vendor tag: org.quic.camera.recording.endOfStream");
                }
            } else {
                if ((mRecordingPausing || mStopRecPending) && (mCurrentSession != null) && mCameraDevice[getMainCameraId()] != null) {
                    mCurrentSession.stopRepeating();
                    try {

                        captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x01);
                        Log.i(TAG, "Set endofstream TAG to 1");
                    } catch (IllegalArgumentException illegalArgumentException) {
                        Log.w(TAG, "can not find vendor tag: org.quic.camera.recording.endOfStream");
                    }
                    if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                        CameraConstrainedHighSpeedCaptureSession session =
                                (CameraConstrainedHighSpeedCaptureSession) mCurrentSession;
                        List requestList = getHighSpeedList(session,mVideoRecordRequestBuilder);
                        mCurrentSession.setRepeatingBurst(requestList, mCaptureCallback, mCameraHandler);
                    }else {
                        if (isSSMEnabled()) {
                            mCurrentSession.captureBurst(createSSMBatchRequest(captureRequestBuilder),
                                    mCaptureCallback, mCameraHandler);
                        } else {
                            mCurrentSession.capture(captureRequestBuilder.build(), mCaptureCallback,
                                    mCameraHandler);
                        }
                    }
                    Log.i(TAG, "Set endofstream TAG is done from APP");
                    captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                }
                if (!isStopRecord) {
                    //is pause record
                    if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                        mMediaRecorder.pause();
                    } else {
                        setVideoState(VideoState.VIDEO_PAUSE);
                    }
                    for (MediaRecorder mediaRecorder : mPhysicalMediaRecorders) {
                        if (mediaRecorder != null) {
                            mediaRecorder.pause();
                        }
                    }
                }
                captureRequestBuilder = mVideoPreviewRequestBuilder;
                captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                Log.d(TAG, "Set endofstream TAG to 0");
                if( (mCurrentSession != null) && mCameraDevice[getMainCameraId()] != null) {
                    if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                        CameraConstrainedHighSpeedCaptureSession session =
                                (CameraConstrainedHighSpeedCaptureSession) mCurrentSession;
                        List requestList = getHighSpeedList(session,mVideoRecordRequestBuilder);
                        mCurrentSession.setRepeatingBurst(requestList, mCaptureCallback, mCameraHandler);
                    }else {
                        mCurrentSession.setRepeatingRequest(captureRequestBuilder.build(),
                                mCaptureCallback, mCameraHandler);
                    }
                }
            }
        } catch (CameraAccessException | IllegalStateException | NullPointerException |
                IllegalArgumentException e) {
            Log.e(TAG,e);
        }
    }

    public void onButtonPause() {
        if (!isRecorderReady() || !isRecordingVideo())
            return;
        pauseVideoRecording();
    }

    public void onButtonContinue() {
        if (!isRecorderReady())
            return;
        resumeVideoRecording();
    }

    private boolean isEISDisable() {
        boolean result = true;
        String value = mSettingsManager.getValue(SettingsManager.KEY_EIS_VALUE);
        if (value != null) {
            result = value.equals(mActivity.getResources().getString(
                    R.string.pref_camera2_eis_entry_value_disable));
        } else {
            result = false;
        }
        Log.v(TAG, "isEISDisable :" + result);
        return result;
    }

    private boolean isPhotoEISDisable() {
        boolean result = true;
        String value = mSettingsManager.getValue(SettingsManager.KEY_PHOTO_EIS_VALUE);
        if (value != null) {
            result = value.equals(mActivity.getResources().getString(
                    R.string.pref_camera2_photo_eis_entry_value_disable));
        } else {
            result = false;
        }
        Log.v(TAG, "isPhotoEISDisable :" + result);
        return result;
    }

    private boolean isAbortCapturesEnable() {
        boolean result = true;
        String value = mSettingsManager.getValue(SettingsManager.KEY_ABORT_CAPTURES);
        if (value != null) {
            result = value.equals(mActivity.getResources().getString(
                    R.string.pref_camera2_abort_captures_entry_value_enable));
        } else {
            result = false;
        }
        Log.v(TAG, "isAbortCapturesEnable :" + result);
        return result;
    }

    public boolean isExtendedMaxZoomEnable() {
        boolean result = true;
        String value = mSettingsManager.getValue(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
        if (value != null) {
            result = value.equals(mActivity.getResources().getString(
                    R.string.pref_camera2_extended_max_zoom_entry_value_enable));
        } else {
            result = false;
        }
        Log.v(TAG, "isExtendedMaxZoomEnable :" + result);
        return result;
    }

    private boolean isSendRequestAfterFlushEnable() {
        return PersistUtil.isSendRequestAfterFlush();
    }

    private void exitVideoModule(){
        Log.i(TAG, "exitVideoModule ");
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,exitVideoModule");
        if (mVideoEncoder != null && mIsRecordingVideo) {
            mVideoEncoder.signalEndOfInputStream();
        }
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,exitVideoModule--1");
        mFrameProcessor.setVideoOutputSurface(null);
        mFrameProcessor.onClose();
        mIsRecordingVideo = false;
        mIsPreviewingVideo = false;
        mRecordingStoped = true;
        mHighSpeedCaptureRate = 0;
        // release media recorder
        if (TRACE_DEBUG) Trace.endSection();
        if (PersistUtil.enableMediaRecorder()) {
            releaseMediaRecorder();
        } else {
            stopCodecThreads();
            releaseMediaCodec();
        }
        releaseAudioFocus();
        mIsPreviewingVideo = false;
         if (TRACE_DEBUG) Trace.endSection();
    }

    private boolean stopMediaReleated(){
        Log.i(TAG,"stopMediaReleated");
        boolean shouldAddToMediaStoreNow = false;
        long stopMediaRecorder = System.currentTimeMillis();
        if(mActivity.getPerformenceTest()){
            mHasMapTimes.put("buttonClick->stopRecorder",stopMediaRecorder - mStartedTime);
        }
        // Stop recording
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,media recorder stop");

        if (PersistUtil.enableMediaRecorder()) {
            try {
                if (mMediaRecorder != null){
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                }
                stopPhysicalRecorder();
                shouldAddToMediaStoreNow = true;
            } catch (RuntimeException e) {
                Log.w(TAG, "MediaRecoder stop fail =" + e + ",mCurrentVideoUri=" + mCurrentVideoUri);
                try {
                    if (mCurrentVideoUri != null) {
                        mContentResolver.delete(mCurrentVideoUri, null);
                        mCurrentVideoUri = null;
                    }
                    for (int i = 0; i < mPhysicalUris.length; i++) {
                        if (mPhysicalUris[i] != null) {
                            mContentResolver.delete(mPhysicalUris[i], null);
                            mPhysicalUris[i] = null;
                        }
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "delete failed ex=", ex);
                }

            }catch (Exception e){
                Log.w(TAG, " MediaRecoder stop exception=", e);
            }
        } else {
            setVideoState(VideoState.VIDEO_STOP);
            stopCodecThreads();
            try{
                stopPhysicalRecorder();

            } catch (RuntimeException e){
                Log.w(TAG, "MediaRecoder stop fail =" + e);
                for (int i = 0; i < mPhysicalUris.length; i++) {
                    if (mPhysicalUris[i] != null) {
                        mContentResolver.delete(mPhysicalUris[i], null);
                        mPhysicalUris[i] = null;
                    }
                }
            }
            shouldAddToMediaStoreNow = true;
            if(System.currentTimeMillis() - mStartRecordingTime < 1500){
                shouldAddToMediaStoreNow = false;
                warningToast("Recording time is too short, don't save file");
            }
        }
        if (mActivity.getPerformenceTest()) {
            stopMediaRecorder = System.currentTimeMillis() - stopMediaRecorder;
            mHasMapTimes.put("stopRecorder->endStop", stopMediaRecorder);
        }
        mRecordingStoped = true;
        if (TRACE_DEBUG) Trace.endSection();
        return shouldAddToMediaStoreNow;
    }
    private void stopRecordingVideo(int cameraId) {
        Log.i(TAG, "stopRecordingVideo " + cameraId);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,stopRecordingVideo");
        mStopRecordingTime = System.currentTimeMillis();
        if (isSSMEnabled()) {
            updateProgressBar(false);
            if (!mSSMCaptureCompleteFlag) {
                warningToast("Super Slow Motion is not finished");
            }
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.signalEndOfInputStream();
        }
        mUI.setSoundEffectsForRecording(true);
        checkAndPlayRecordSound(cameraId, false);
        mStopRecPending = true;
        mRecordingPausing = false;
        mIsRecordingVideo = false;
        mRecordingStoped = false;

        if (PersistUtil.enableMediaRecorder()) {
            mIsPreviewingVideo = true;
        } else {
            mIsPreviewingVideo = false;
        }
        mRecordingStarted = false;
        boolean shouldAddToMediaStoreNow = false;
        String value = mSettingsManager.getValue(SettingsManager.KEY_FRC_MODE);
        if(value != null && Integer.valueOf(value) == 0){
            shouldAddToMediaStoreNow = stopMediaReleated();
        }

        String profile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
        if (PersistUtil.needEndOfStream() && !profile.equals("HEVCProfileMain10HDR10Plus")) {
            setEndOfStream(false, true);
        }
        for (int i =0; i < mPhysicalMediaRecorders.length; i++) {
            if (mPhysicalMediaRecorders[i] != null) {
                mVideoRecordRequestBuilder.removeTarget(mPhysicalMediaSurfaces[i]);
            }
        }
            mVideoRecordRequestBuilder.removeTarget(mVideoRecordingSurface);
        if(mHighSpeedCapture && !isVariableFPSEnabled() && mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS) {
            mVideoRecordRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    mHighSpeedPreviewFPSRange);
        }
        if (!PersistUtil.enableMediaRecorder()) {
            mFrameProcessor.setVideoOutputSurface(null);
            mFrameProcessor.onClose();
            if (mLiveShotInitHeifWriter != null) {
                mLiveShotInitHeifWriter.close();
            }

        } else {
            //stop without config stream
            if( (mCurrentSession != null) && mCameraDevice[getMainCameraId()] != null) {
                try {
                    if (isHighSpeedRateCapture()) {
                        List<CaptureRequest> requests = mSuperSlomoCapture ?
                                createSSMBatchRequest(mVideoRecordRequestBuilder) :
                                getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCurrentSession,mVideoRecordRequestBuilder);
                        mCurrentSession.setRepeatingBurst(requests, mCaptureCallback,
                                mCameraHandler);

                    } else {
                        Log.i(TAG,"setRepeatingRequest");
                        mCurrentSession.setRepeatingRequest(mVideoPreviewRequestBuilder.build(),
                                mCaptureCallback, mCameraHandler);
                    }
                } catch (CameraAccessException | IllegalStateException e) {
                    Log.w(TAG, "stopRecordingVideo: " + e);
                }
            }
        }

        if(value != null && Integer.valueOf(value) != 0){
            shouldAddToMediaStoreNow = stopMediaReleated();
        }

        if (!mPaused) {
            if (!PersistUtil.enableMediaRecorder()) {
                setVideoFlashOff();
                closePreviewSession();
            } else {
                applyZoomAndUpdate();
            }
        }
        Log.i(TAG, "stopRecordingVideo done. Time=" +
                (System.currentTimeMillis() - mStopRecordingTime) + "ms"+
                ",shouldAddToMediaStoreNow="+shouldAddToMediaStoreNow);

        AccessibilityUtils.makeAnnouncement(mUI.getVideoButton(),
                mActivity.getString(R.string.video_recording_stopped));
        if (shouldAddToMediaStoreNow) {
            saveVideo();
        }
        keepScreenOnAwhile();
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,media recorder release");
        // release media recorder
        if (PersistUtil.enableMediaRecorder()) {
            releaseMediaRecorder();
        } else {
            releaseMediaCodec();
        }
        resetAudioMute();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mUI.showRecordingUI(false, false);
                mUI.enableShutter(true);
                if (mIntentMode == INTENT_MODE_VIDEO) {
                    if (isQuickCapture()) {
                        onRecordingDone(true);
                    } else {
                        mTempHoldVideoInVideoIntent = true;
                        Bitmap thumbnail = getVideoThumbnail();
                        mUI.showRecordVideoForReview(thumbnail);
                    }
                }
            }
        });
        if(mFrameProcessor != null) {
            mFrameProcessor.onOpen(getFrameProcFilterId(), mPreviewSize);
        }
             if (TRACE_DEBUG) Trace.endSection();

        if (mIntentMode != INTENT_MODE_VIDEO && !mPaused) {
            if (!PersistUtil.enableMediaRecorder()) {
                releaseAudioFocus();
                if(mActivity.getPerformenceTest()) {
                    mSessionAfterRecord = System.currentTimeMillis();
                }
                    createSessions();

            }
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mUI.showUIafterRecording();
                mUI.resetTrackingFocus();
            }
        });
        if(mIntentMode != INTENT_MODE_VIDEO) {
            mStopRecPending = false;
        }
        if(mPostProcessor.isJniAPISupported() && mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE).equals("HEVCProfileMain10HDR10Plus"))
            mPostProcessor.nativePerfLockRelease(2);
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void setVideoFlashOff() {
        Log.d(TAG, "setVideoFlashOff: currentsession:" + mCurrentSession +
                ",currentclosed:" + mCurrentSessionClosed );
        if (mCurrentSession == null || mCurrentSessionClosed) {
            return;
        }
        try {
            mVideoRecordRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        } catch (IllegalArgumentException e) {
            Log.w(TAG,e.toString());
        }
        try {
            mCurrentSession.stopRepeating();
            if (mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                List requestList = getHighSpeedList((CameraConstrainedHighSpeedCaptureSession)mCurrentSession,
                        mVideoRecordRequestBuilder);
                mCurrentSession.captureBurst(requestList, mCaptureCallback, mCameraHandler);
            } else if (!isSSMEnabled()){
                mCurrentSession.capture(mVideoRecordRequestBuilder.build(), mCaptureCallback,
                        mCameraHandler);
            }
        } catch (CameraAccessException | IllegalArgumentException | IllegalStateException  e) {
            Log.e(TAG,e.toString());
        }
    }

    private void closePreviewSession() {
        Log.i(TAG, "closePreviewSession: currentsession:" +mCurrentSession + ",currentclosed:" + mCurrentSessionClosed );
        if (mCurrentSession == null || mCurrentSessionClosed) {
            return;
        }
        //if have this, video switch to photo, photo will have no preview
        //mCurrentSession.close();
        mCurrentSessionClosed = true;
        mCurrentSession = null;
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mActivity.getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    private String generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = title + CameraUtil.convertOutputFormatToFileExt(outputFileFormat);
        String mime = CameraUtil.convertOutputFormatToMimeType(outputFileFormat);

        String path;
        if (Storage.isSaveSDCard() && SDCard.instance().isWriteable()) {
            path = SDCard.instance().getDirectory() + '/' + filename;
        } else {
            path = Storage.DIRECTORY + '/' + filename;
        }
        Log.d(TAG," title="+title+",filename="+filename+",mime="+mime+",path="+path+
                ",Storage.DIRECTORY="+Storage.DIRECTORY+",Storage.isSaveSDCard()="+Storage.isSaveSDCard());
        mCurrentVideoValues = new ContentValues(13);
        mCurrentVideoValues.put(MediaStore.Video.Media.TITLE, title);
        mCurrentVideoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mCurrentVideoValues.put(MediaStore.Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues.put(MediaStore.Video.Media.RESOLUTION,
                "" + mVideoSize.getWidth() + "x" + mVideoSize.getHeight());
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            mCurrentVideoValues.put(MediaStore.Video.Media.LATITUDE, loc.getLatitude());
            mCurrentVideoValues.put(MediaStore.Video.Media.LONGITUDE, loc.getLongitude());
        }
        if (ApiHelper.isAndroidROrHigher()) {
            mCurrentVideoValues.put(MediaStore.Video.Media.IS_PENDING, 1);
            mCurrentVideoValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
        }
        mVideoFilename = path;
        return path;
    }

    private Uri generatePhysicalVideoFilename(int outputFileFormat,int cameraId) {
        if (!mIsRecordingVideo) {
            return null;
        }
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken)+"_phy_"+cameraId;
        String filename = title + CameraUtil.convertOutputFormatToFileExt(outputFileFormat);
        String mime = CameraUtil.convertOutputFormatToMimeType(outputFileFormat);
        String path;
        if (Storage.isSaveSDCard() && SDCard.instance().isWriteable()) {
            path = SDCard.instance().getDirectory() + '/' + filename;
        } else {
            path = Storage.DIRECTORY + '/' + filename;
        }
        ContentValues currentVideoValues = new ContentValues(11);
        currentVideoValues.put(MediaStore.Video.Media.TITLE, title);
        currentVideoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        currentVideoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        currentVideoValues.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        currentVideoValues.put(MediaStore.Video.Media.MIME_TYPE, mime);
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            currentVideoValues.put(MediaStore.Video.Media.LATITUDE, loc.getLatitude());
            currentVideoValues.put(MediaStore.Video.Media.LONGITUDE, loc.getLongitude());
        }
        if (ApiHelper.isAndroidROrHigher()) {
            currentVideoValues.put(MediaStore.Video.Media.IS_PENDING, 1);
            currentVideoValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
        }
        Uri videoTable = Storage.getVideoBaseUri();
        Uri videoUri = mContentResolver.insert(videoTable, currentVideoValues);
        Log.d(TAG, "path " + path +", videoUri " + videoUri);
        return videoUri;
    }

    private void saveVideo() {
        Log.i(TAG,"start to save video mCurrentVideoUri="+mCurrentVideoUri);
        long startSaveVideo = System.currentTimeMillis();
        if (mSettingsManager.isMultiCameraEnabled()) {
            Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_CAMCORDER);
            Iterator<String> iterator = null;
            if (ids != null && ids.size() >0){
                iterator = ids.iterator();
            }
            for (Uri videoUri : mPhysicalUris){
                if (videoUri != null) {
                    String physicalId = null;
                    if (iterator != null && iterator.hasNext()){
                        physicalId = iterator.next();
                    }
                    ContentValues contentValues = new ContentValues(2);
                    if (physicalId != null){
                        int index = getIndexByPhysicalId(physicalId);
                        contentValues.put(MediaStore.Video.Media.RESOLUTION,
                                mPhysicalVideoSizes[index].toString());
                    }
                    if (ApiHelper.isAndroidROrHigher()) {
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
                    }
                    mActivity.getMediaSaveService().updateVideo(videoUri, contentValues,
                            mOnVideoSavedListener, mContentResolver);
                    Log.i(TAG, "save video successfully");
                    mUrisInvalid.remove(videoUri);
                }
            }
        }
        if (mVideoFileDescriptor != null && mCurrentVideoUri != null && mCurrentVideoValues != null) {
            long duration = 0L;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(mVideoFileDescriptor.getFileDescriptor());
                duration = Long.parseLong(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION));
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "retriever file exception "+e);
            }
            mCurrentVideoValues.put(MediaStore.Video.Media.DURATION, duration);
            if (ApiHelper.isAndroidROrHigher()) {
                mCurrentVideoValues.put(MediaStore.Video.Media.IS_PENDING, 0);
            }
            mActivity.getMediaSaveService().updateVideo(mCurrentVideoUri,
                    mCurrentVideoValues,
                    mOnVideoSavedListener, mContentResolver);
            mUrisInvalid.remove(mCurrentVideoUri);
        }
        mCurrentVideoValues = null;
        Log.i(TAG,"end save video ");
        if(mActivity.getPerformenceTest()){
            mHasMapTimes.put("startSaveVideo->endSave",System.currentTimeMillis() - startSaveVideo);
        }
    }

    private void updateBitrateForNonHFR(int videoEncoder, int bitRate, int height, int width) {
        MediaCodecList allCodecs = new MediaCodecList(MediaCodecList.ALL_CODECS);
        if(PersistUtil.getBitRate() != -1){
            bitRate = PersistUtil.getBitRate();
        }
        for (MediaCodecInfo info : allCodecs.getCodecInfos()) {
            if (!info.isEncoder() || info.getName().contains("google")) continue;
            for (String type : info.getSupportedTypes()) {
                if ((videoEncoder == MediaRecorder.VideoEncoder.MPEG_4_SP && type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4))
                        || (videoEncoder == MediaRecorder.VideoEncoder.H263 && type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263)))
                {
                    CodecCapabilities codecCapabilities = info.getCapabilitiesForType(type);
                    VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                    try {
                        if (videoCapabilities != null) {
                            Log.d(TAG, "updateBitrate type is " + type + " " + info.getName());
                            int maxBitRate = videoCapabilities.getBitrateRange().getUpper().intValue();
                            Log.d(TAG, "maxBitRate is " + maxBitRate + ", profileBitRate is " + bitRate);
                            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                                mMediaRecorder.setVideoEncodingBitRate(Math.min(bitRate, maxBitRate));
                            } else {
                                mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, Math.min(bitRate, maxBitRate));
                            }
                            return;
                        }
                    } catch (IllegalArgumentException e) {

                    }
                }
            }
        }
        Log.d(TAG, "updateBitrate video bitrate: "+ bitRate);
        if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
            mMediaRecorder.setVideoEncodingBitRate(bitRate);
        } else {
            if (mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER).equals("apv")) {
                bitRate = calculateBitRate(width, height);
            }
            mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        }
    }

    private void setUpPhysicalMediaRecorder() throws IOException {
        Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                SettingsManager.KEY_PHYSICAL_CAMCORDER);
        if (ids == null || ids.size() == 0) {
            return;
        }
        releasePhysicalRecorder();
        int count = ids.size();
        Log.i(TAG,"setUpPhysicalMediaRecorder count="+count);
        CamcorderProfile profile;
        Object[] idsArray =ids.toArray();
        for (int i=0;i<count;i++) {
            int index = getIndexByPhysicalId((String)idsArray[i]);
            String videoSize;
            if (index != -1){
                videoSize = mSettingsManager.getValue(SettingsManager.KEY_PHYSICAL_VIDEO_SIZE[i]);
            } else {
                videoSize = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
            }
            int size = CameraSettings.VIDEO_QUALITY_TABLE.get(videoSize);
            String physicalid = (String)idsArray[i];
            int recordid = Integer.valueOf(physicalid);
            if (mSettingsManager.hasProfile(recordid, size)) {
                profile = CamcorderProfile.get(recordid, size);
                if (profile != null) {
                    MediaRecorder recorder = new MediaRecorder();
                    recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    recorder.setAudioSource(PersistUtil.getAudioSource());
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    Uri videoUri = generatePhysicalVideoFilename(
                            MediaRecorder.OutputFormat.MPEG_4, recordid);
                    mPhysicalUris[i] = videoUri;
                    if (videoUri == null) {
                        File cacheDir = mActivity.getExternalCacheDir();
                        if (cacheDir != null) {
                            cacheDir.mkdirs();
                            String tmpFileName = cacheDir.getAbsolutePath() + "/tmp.mp4";
                            Log.d(TAG, "setOutputFile, tmp " + tmpFileName);
                            recorder.setOutputFile(tmpFileName);
                        } else {
                            Log.e(TAG, "getExternalCacheDir return null");
                            releaseMediaRecorder();
                            throw new RuntimeException("getExternalCacheDir return null");
                        }
                    } else {
                        try {
                            ParcelFileDescriptor parcelFileDescriptor = mContentResolver.openFileDescriptor(videoUri, "rw");
                            recorder.setOutputFile(parcelFileDescriptor.getFileDescriptor());
                            Log.d(TAG, "add invalid uri " + videoUri);
                            mUrisInvalid.add(videoUri);
                        } catch (IOException e) {
                            Log.e(TAG, "openFileDescriptor failed for " + videoUri, e);
                            releaseMediaRecorder();
                            throw new RuntimeException(e);
                        }
                    }
                    recorder.setVideoEncodingBitRate(profile.videoBitRate);
                    recorder.setVideoFrameRate(profile.videoFrameRate);
                    recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                    recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    int rotation = CameraUtil.getJpegRotation(recordid, mOrientation);
                    recorder.setOrientationHint(rotation);
                    recorder.setInputSurface(mPhysicalMediaSurfaces[i]);
                    mPhysicalMediaRecorders[i] = recorder;
                    try {
                        recorder.prepare();
                    } catch (IOException e) {
                        Log.e(TAG, "prepare failed for " + videoUri, e);
                        releaseMediaRecorder();
                        throw new RuntimeException(e);
                    }
                    recorder.setOnErrorListener(this);
                    recorder.setOnInfoListener(this);
                }
            } else {
                warningToast(R.string.error_app_unsupported_profile);
                throw new IllegalArgumentException("error_app_unsupported_profile");
            }
        }
    }

    private void setupRecordingCommonSettings(int cameraId) {
        enableVideoButton(false);
        String videoSize = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        int size = CameraSettings.VIDEO_QUALITY_TABLE.get(videoSize);

        Intent intent = mActivity.getIntent();
        if (intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
            int extraVideoQuality =
                    intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                size = CamcorderProfile.QUALITY_HIGH;
            } else {
                size = CamcorderProfile.QUALITY_LOW;
            }
        }
        if (mCaptureTimeLapse) {
            size = CameraSettings.getTimeLapseQualityFor(size);
        }
        closeVideoFileDescriptor();
        if (mSettingsManager.hasProfile(cameraId, size)) {
            mProfile = CamcorderProfile.get(cameraId, size);
        } else {
            warningToast(R.string.error_app_unsupported_profile);
            throw new IllegalArgumentException("error_app_unsupported_profile");
        }
        if (PersistUtil.enableMediaRecorder()) {
            mProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        } else {
            mProfile.fileFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
        }
        updateHFRSetting();
    }

    private void isSupportedResolution(int videoEncoder) {
        //check if codec supports the resolution, otherwise throw toast
        int videoWidth = mProfile.videoFrameWidth;
        int videoHeight = mProfile.videoFrameHeight;
        Log.d(TAG,"mProfile.videoCodec="+mProfile.videoCodec);
        String type = SettingTranslation.getVideoEncoderType(mProfile.videoCodec);
        Log.d(TAG,"codec type="+type);
        MediaCodecList allCodecs = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaFormat format = MediaFormat.createVideoFormat(type,videoWidth,videoHeight);
        try{
            String encodeName = allCodecs.findEncoderForFormat(format);
            Log.d(TAG,"encodeName="+encodeName);
        } catch (IllegalArgumentException| NullPointerException e){
            Log.w(TAG,e.toString());
            mUnsupportedResolution = true;
        } finally {
            if (mUnsupportedResolution) {
                warningToast(R.string.error_app_unsupported);
                return;
            }
        }
    }

    private void setMaxFileSize(long requestLimit) {
        long maxFileSize = mActivity.getStorageSpaceBytes() - Storage.LOW_STORAGE_THRESHOLD_BYTES;
        if (requestLimit > 0 && requestLimit < maxFileSize) {
            maxFileSize = requestLimit;
        }

        if (Storage.isSaveSDCard() && maxFileSize > SDCARD_SIZE_LIMIT) {
            maxFileSize = SDCARD_SIZE_LIMIT;
        }
        try {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                Log.d(TAG, "MediaRecorder setMaxFileSize: " + maxFileSize);
                mMediaRecorder.setMaxFileSize(maxFileSize);
            } else {
                //CODEC:
            }
        } catch (RuntimeException exception) {
            // We are going to ignore failure of setMaxFileSize here, as
            // a) The composer selected may simply not support it, or
            // b) The underlying media framework may not handle 64-bit range
            // on the size restriction.
        }
    }

    private void setOrientationHint(int cameraId) {
        int rotation = CameraUtil.getJpegRotation(cameraId, mOrientation);
        String videoRotation = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ROTATION);
        if (videoRotation != null) {
            rotation += Integer.parseInt(videoRotation);
            rotation = rotation % 360;
        }
        if(mFrameProcessor.isFrameFilterEnabled()) {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                mMediaRecorder.setOrientationHint(0);
            } else {
                mMuxer.setOrientationHint(0);
            }
        } else {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                mMediaRecorder.setOrientationHint(rotation);
            } else {
                mMuxer.setOrientationHint(rotation);
            }
        }
    }

    private void setLocation() {
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                mMediaRecorder.setLocation((float) loc.getLatitude(),
                        (float) loc.getLongitude());
            } else {
                mMuxer.setLocation((float) loc.getLatitude(),
                        (float) loc.getLongitude());
            }
        }
    }

    private synchronized void setVideoState(VideoState state) {
        synchronized (mVideoStateLock) {
            mVideoState = state;
        }
    }

    //---------------------MediaCodec related start--------------------------

    private AudioRecord mAudioRecord;
    private MediaCodec mVideoEncoder, mAudioEncoder;
    private MediaFormat mAudioFormat, mVideoFormat;
    private MediaMuxer mMuxer;
    private int mNumTracksAdded = 0;
    private int mTrackAudioIndex = 0;
    private int mTrackVideoIndex = 0;
    private int mAudioBufferSize = 0;
    private final int TOTAL_NUM_TRACKS = 2;
    //private final int SAMPLES_PER_FRAME = 1024; // AAC
    private static final int IFRAME_INTERVAL = 1;
    private int mAudioFormatNumber = AudioFormat.ENCODING_PCM_16BIT;
    private static final int TIMEOUT_USEC = 10000;
    private boolean mMuxerStart = false;
    private boolean mMuxerVideoStop = false;
    private boolean mMuxerAudioStop = false;
    private boolean mOnlyVideoEncoder = false;
    private boolean mOutputFileInit = false;
    private boolean mAudioCodecInit = false;
    private boolean mAudioRecorderInit = false;
    private Thread mVideoEncodeThread;
    private Thread mAudioEncodeThread;
    private Thread mAudioDecodeThread;
    private JSONArray dynamicSettingsArray;
    private class DynamicSetting {
        public int frameNumber;
        public String name;
        public String type;
        public String value;
    }
    private ArrayList<DynamicSetting> mDynamicSettings;

    private void applyVideoSettings() {
        JSONArray jsonArray = mSettingsManager.getVideoSettings();
        mDynamicSettings.clear();
        if (jsonArray == null)return;
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jb = jsonArray.getJSONObject(i);
                boolean isStatic = jb.getBoolean("StaticTag");
                String name = jb.getString("Name");
                String type = jb.getString("Type");
                DynamicSetting ds = new DynamicSetting();
                switch (type) {
                    case "Integer":
                        int iValue = jb.getInt("Value");
                        if (isStatic) {
                            mVideoFormat.setInteger(name, iValue);
                            Log.d(TAG + "_videoformat", "set " + name + " " + iValue);
                        } else {
                            ds.value = String.valueOf(iValue);
                        }
                        break;
                    case "String":
                        String sValue = jb.getString("Value");
                        if (isStatic) {
                            mVideoFormat.setString(name, sValue);
                            Log.d(TAG + "_videoformat", "set " + name + " " + sValue);
                        } else {
                            ds.value = sValue;
                        }
                        break;
                    case "Float":
                        String fValue = jb.getString("Value");
                        if (isStatic) {
                            mVideoFormat.setFloat(name, Float.parseFloat(fValue));
                            Log.d(TAG + "_videoformat", "set " + name + " " + fValue);
                        } else {
                            ds.value = fValue;
                        }
                        break;
                    case "Long":
                        Long lValue = jb.getLong("Value");
                        if (isStatic) {
                            mVideoFormat.setLong(name, lValue);
                            Log.d(TAG + "_videoformat", "set " + name + " " + lValue);
                        } else {
                            ds.value = String.valueOf(lValue);
                        }
                        break;
                    default:
                        Log.d(TAG, "No matched type of video format");
                        break;
                }
                if (!isStatic) {
                    ds.frameNumber = jb.getInt("FrameNum");
                    ds.type = type;
                    ds.name = name;
                    mDynamicSettings.add(ds);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG,"JSONException="+e);
        }
    }

    private void startAudioEncoder() {
        mAudioEncodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, MEDIACODEC_AUDIO_LOG,"Encording audio thread starts");
                // Feed encoder output into the muxer until recording stops.
                doAudioEncoding();
                Log.v(TAG, MEDIACODEC_AUDIO_LOG,"Encording audio thread completes");
                return;
            }
        }, "AudioEncoder Thread");
        mAudioEncodeThread.start();
    }

    private void startAudioDecoder() {
        mAudioDecodeThread = new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, MEDIACODEC_AUDIO_LOG,"Decoding audio thread starts");
                doAudioDecoding();
                Log.v(TAG, MEDIACODEC_AUDIO_LOG,"Decoding Audio thread completes");
                return;
            }
        }, "AudioDecorder Thread");
        mAudioDecodeThread.start();
    }

    private void startVideoEncoder() {
        mVideoEncodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG,MEDIACODEC_VIDEO_LOG,"Encording video thread starts");
                doVideoEncoding();
                Log.v(TAG, MEDIACODEC_VIDEO_LOG,"Encording video thread completes");
                return;
            }
        }, "VideoEncorder Thread");
        mVideoEncodeThread.start();
    }

    private synchronized void stopCodecThreads() {
        // Wait until recording thread stop
        try {
            if (mAudioDecodeThread != null)
                mAudioDecodeThread.join();
            if (mAudioEncodeThread != null)
                mAudioEncodeThread.join();
            if (mVideoEncodeThread != null)
                mVideoEncodeThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Stop recording failed", e);
        }
        if ((mMuxerAudioStop || mOnlyVideoEncoder) && mMuxerVideoStop) {
            releaseMuxer();
        }
    }

    private void releaseMuxer() {
        Log.v(TAG, "releasing muxer");
        if (mMuxer != null) {
            if (mMuxerStart) mMuxer.stop();
            mMuxer.release();
            mMuxerStart = false;
            mMuxer = null;
        }
    }

    private String getEncoderForamtVideo(String encoderSelected) {
        if(encoderSelected.equals("h263")) {
            return MediaFormat.MIMETYPE_VIDEO_H263;
        } else if (encoderSelected.equals("h264")) {
            return  MediaFormat.MIMETYPE_VIDEO_AVC;
        } else if (encoderSelected.equals("h265")) {
            return MediaFormat.MIMETYPE_VIDEO_HEVC;
        } else if (encoderSelected.equals("mpeg-4-sp")) {
            return MediaFormat.MIMETYPE_VIDEO_MPEG4;
        } else if (encoderSelected.equals("vp8")) {
            return MediaFormat.MIMETYPE_VIDEO_VP8;
        } else if (encoderSelected.equals("apv")) {
            return "video/apv";
        } else
            return MediaFormat.MIMETYPE_VIDEO_AVC;
    }

    private String getEncoderFormatAudio(String encoderSelected) {
        if (encoderSelected.equals("aac")) {
            return MediaFormat.MIMETYPE_AUDIO_AAC;
        } else if (encoderSelected.equals("aac-eld")){
            return MediaFormat.MIMETYPE_AUDIO_AAC;
        } else if (encoderSelected.equals("he-aac")){
            return MediaFormat.MIMETYPE_AUDIO_AAC;
        } else if (encoderSelected.equals("amr-nb")) {
            return  MediaFormat.MIMETYPE_AUDIO_AMR_NB;
        } else if (encoderSelected.equals("amr-wb")) {
            return MediaFormat.MIMETYPE_AUDIO_AMR_WB;
        } else if (encoderSelected.equals("vorbis")) {
            return MediaFormat.MIMETYPE_AUDIO_VORBIS;
        } else
            return MediaFormat.MIMETYPE_AUDIO_AAC;
    }

    private void setupMediaCodecVideo(int cameraId) throws IOException {
        mNumTracksAdded = 0;
        mDynamicSettings = new ArrayList<>();
        String sVideoEncoder = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER);
        int iVideoEncoder = SettingTranslation.getVideoEncoder(sVideoEncoder);
        String encoder = getEncoderForamtVideo(sVideoEncoder);
        mVideoFormat = MediaFormat.createVideoFormat(encoder, mProfile.videoFrameWidth,
                mProfile.videoFrameHeight);
        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        if (!mHighSpeedCapture) {
            updateBitrateForNonHFR(iVideoEncoder, mProfile.videoBitRate, mProfile.videoFrameHeight,
                    mProfile.videoFrameWidth);
        }
        isSupportedResolution(iVideoEncoder);
        if (isVideoEncoderProfileSupported()) {
            int videoEncoderProfile = SettingTranslation.getVideoEncoderProfile(
                    mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE));
            Log.d(TAG, "setVideoEncodingProfileLevel: " + videoEncoderProfile + " " +
                    MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
            final Bundle params = new Bundle();
            mVideoFormat.setInteger(MediaFormat.KEY_PROFILE, videoEncoderProfile);
            mVideoFormat.setInteger(MediaFormat.KEY_LEVEL,
                    MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
        }
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mProfile.videoFrameRate);
        Log.i(TAG, "Profile video frame rate: "+ mProfile.videoFrameRate);
        if (mCaptureTimeLapse) {
            float fps = 1000 / (float) mTimeBetweenTimeLapseFrameCaptureMs;
            mVideoFormat.setFloat(MediaFormat.KEY_CAPTURE_RATE, fps);
        }  else if (mHighSpeedCapture) {
            mHighSpeedFPSRange = new Range(mHighSpeedCaptureRate, mHighSpeedCaptureRate);
            mHighSpeedPreviewFPSRange =  new Range(30, mHighSpeedCaptureRate);
            int fps = (int) mHighSpeedFPSRange.getUpper();
            int targetRate = mHighSpeedRecordingMode ? fps : 30;
            mVideoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, fps);
            mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, targetRate);
            mVideoFormat.setInteger(MediaFormat.KEY_OPERATING_RATE, fps);
            Log.i(TAG, "Capture rate: "+fps+", Target rate: "+targetRate);
            int scaledBitrate = mSettingsManager.getHighSpeedVideoEncoderBitRate(mProfile, targetRate, fps);
            if (PersistUtil.getBitRate() != -1) {
                scaledBitrate = PersistUtil.getBitRate();
            } else if (mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER).equals("apv")) {
                scaledBitrate = calculateBitRate(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            }
            Log.i(TAG, "Scaled video bitrate : " + scaledBitrate);
            mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, scaledBitrate);
        }
        if (mCaptureTimeLapse) {
            mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL - 1);
        } else {
            mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        }
        //It's mandated to set this key for any video session since S
        mVideoFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
        applyVideoFlip();
        applyVideoSettings();
        if (PersistUtil.lookaheadEnabled()) {
            Log.i(TAG + "_videoformat", "set lookahead enable.");
            mVideoFormat.setInteger("vendor.qti-ext-encoding-mode.value", 4);
        }
        mVideoEncoder = MediaCodec.createEncoderByType(encoder);
        if (PersistUtil.isProSightEnabled()) {
            try {
                if (QMediaCodecCapabilities.isProSightSupported(mVideoEncoder)) {
                    Log.i(TAG, "isprosightsupported: True");
                    mVideoFormat = QMediaCodecCapabilities.enableProSight(mVideoFormat);
                    Log.i(TAG, "enabling ProSight mode..");
                } else {
                    Log.i(TAG, "isprosightsupported: False");
                }
            } catch (Exception e) {
                Log.e(TAG, "enableProSight faild:"+e);
            }
        }
        if(mSettingsManager.getValue(mSettingsManager.KEY_HDR10P_STATS_KEY) != null &&
                mSettingsManager.getValue(mSettingsManager.KEY_HDR10P_STATS_KEY).equals("on")){
            Log.d(TAG,"set hdr10p status enable value");
            mVideoFormat.setInteger("vendor.qti-ext-enc-hdr10plus-stats-gen.value", 1);

        }
        mVideoEncoder.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

    }

    private void applyVideoFlip() {
        if (PersistUtil.enableMediaRecorder() || mCurrentSceneMode.mode != CameraMode.VIDEO
                || (mVideoSize.getWidth() > 1920) || (mVideoSize.getHeight() > 1080)){
            return;
        }
        if (mSettingsManager != null) {
            String videoFlipValue = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_FLIP);
            Log.d(TAG, "video flip is " + videoFlipValue);
            if (videoFlipValue != null && videoFlipValue.equals("1")) {
                 mVideoFormat.setInteger("vendor.qti-ext-enc-preprocess-mirror.flip", 1);
            }
        }
    }

    private void doVideoEncoding() {
        boolean notDone = true;
        long startPtsUs = 0;
        long prevPtsUs = 0;
        long frameGap = 0;
        int frameNumber = 0;
        int endCounter = 0;
        boolean stopRec = false;
        MediaFormat originalFormat = null;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (notDone && !stopRec) {
            if (!mIsRecordingVideo && !mIsPreviewingVideo ) {

                if (endCounter < 5){
                    endCounter++;
                    //wait 100ms one time
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG,"InterruptedException ="+e);
                    }
                } else {
                    mMuxerVideoStop = true;
                    notDone = false;
                }
            }
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                /**
                 * should happen before receiving buffers, and should only
                 * happen once
                 */
                if (mMuxerStart) {
                    throw new IllegalStateException("video format changed twice");
                }
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                Log.v(TAG + "_video",MEDIACODEC_VIDEO_LOG, "encoder output format changed: " + newFormat);
                if (originalFormat == null) {
                    mTrackVideoIndex = mMuxer.addTrack(newFormat);
                    originalFormat = newFormat;
                    mNumTracksAdded++;
                    mMuxerVideoStop = false;
                    mMuxer.start();
                    mMuxerStart = true;
                    Log.d(TAG + "_video", "mNumTracksAdded is " + mNumTracksAdded+",originalFormat="+originalFormat);
                } else if (!originalFormat.equals(newFormat)) {
                    Log.w(TAG + "_video", "video format changed again, ignoring it");
                }
                if (mOnlyVideoEncoder || (mNumTracksAdded == TOTAL_NUM_TRACKS))  {
                    enableVideoButton(true);
                    mVideoRecordRequestBuilder.removeTarget(mVideoRecordingSurface);
                }
            } else if (encoderStatus < 0) {
                   Log.w(TAG + "_video", MEDIACODEC_VIDEO_LOG,"unexpected result from OutputBuffer: "+ encoderStatus);
            } else {
                // Normal flow: get output encoded buffer, send to muxer.
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus);
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    /**
                     * The codec config data was pulled out and fed to the muxer
                     * when we got the INFO_OUTPUT_FORMAT_CHANGED status. Ignore
                     * it.
                     */
                    Log.v(TAG + "_video",MEDIACODEC_VIDEO_LOG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

                if (mIsRecordingVideo && !mRecordingPausing && mMuxerStart
                        && bufferInfo.size != 0) {
                    /**
                     * It's usually necessary to adjust the ByteBuffer values to
                     * match BufferInfo.
                     */
                    if (bufferInfo.presentationTimeUs > 0 && startPtsUs == 0) {
                        startPtsUs = bufferInfo.presentationTimeUs + 1;
                    }
                    if (mRecordingPausingTime > 0) {
                        if (mHighSpeedCapture && !mHighSpeedRecordingMode) {
                            bufferInfo.presentationTimeUs -= mHighRecordingPausingTime*1000;
                        } else {
                            if (mCaptureTimeLapse) {
                                bufferInfo.presentationTimeUs -= (mRecordingPausingTime * 1000L
                                        * 1000L / (long) mTimeBetweenTimeLapseFrameCaptureMs  / 30L);
                            } else {
                                bufferInfo.presentationTimeUs -= mRecordingPausingTime*1000;
                            }
                        }
                    }
                    frameNumber++;
                    if (mDynamicSettings.size() > 0) {
                        String name, value;
                        for (int i = 0; i < mDynamicSettings.size(); i++) {
                            if (mDynamicSettings.get(i).frameNumber == frameNumber) {
                                name = mDynamicSettings.get(i).name;
                                value = mDynamicSettings.get(i).value;
                                final Bundle bundle = new Bundle();
                                switch (mDynamicSettings.get(i).type) {
                                    case "Integer":
                                        Log.d(TAG + "_videoformat", "dynamic set " + name + " " + value);
                                        bundle.putInt(name, Integer.parseInt(value));
                                        mVideoEncoder.setParameters(bundle);
                                        break;
                                    case "String":
                                        Log.d(TAG + "_videoformat", "dynamic set " + name + " " + value);
                                        bundle.putString(name, value);
                                        mVideoEncoder.setParameters(bundle);
                                        break;
                                    case "Float":
                                        Log.d(TAG + "_videoformat", "dynamic set " + name + " " + value);
                                        bundle.putFloat(name, Float.parseFloat(value));
                                        mVideoEncoder.setParameters(bundle);
                                        break;
                                    case "Long":
                                        Log.d(TAG + "_videoformat", "dynamic set " + name + " " + value);
                                        bundle.putLong(name, Long.parseLong(value));
                                        mVideoEncoder.setParameters(bundle);
                                        break;
                                }
                            }
                        }
                    }
                    Log.d(TAG + "_video", MEDIACODEC_VIDEO_LOG,"prevPts is " + prevPtsUs);
                    if (bufferInfo.presentationTimeUs > prevPtsUs) {
                        frameGap = bufferInfo.presentationTimeUs - prevPtsUs;
                        prevPtsUs = bufferInfo.presentationTimeUs;
                    }
                    Log.d(TAG + "_video", MEDIACODEC_VIDEO_LOG,"presPts is " + prevPtsUs + ",gap is " + frameGap
                                + ",maxduration is " + mMaxDurationForCodec + ",nowduration is "
                                + (bufferInfo.presentationTimeUs - startPtsUs));
                    if ((mMaxDurationForCodec != 0) && (bufferInfo.presentationTimeUs - startPtsUs
                            >= mMaxDurationForCodec*1000) && !stopRec) {
                        stopRec = true;
                        // stop video
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               if(mIsRecordingVideo) {
                                   stopRecordingVideo(getMainCameraId());
                               }
                            }
                        });
                    }
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    try {
                        mMuxer.writeSampleData(mTrackVideoIndex, encodedData, bufferInfo);
                        Log.v(TAG + "_video", MEDIACODEC_VIDEO_LOG, "sent " + bufferInfo.size +
                                " bytes to muxer, timestamp is " + bufferInfo.presentationTimeUs);
                    }catch (Exception e){
                        Log.i(TAG," exception ="+e.getMessage()+", bufferInfo.size="+ bufferInfo.size+",stopRec="+stopRec
                                +",mIsRecordingVideo="+mIsRecordingVideo);
                        if(!stopRec && mIsRecordingVideo) {
                            stopRec = true;
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    stopRecordingVideo(getMainCameraId());
                                    }

                            });
                        }
                    }
                }
                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG + "_video", "end of video stream reached");
                    mMuxerVideoStop = true;
                    notDone = false;
                }
            }
        }
    }

    private void configureDefaultEncoder(String encoder) throws IOException {
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mProfile.audioBitRate);
        //mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);
        mAudioEncoder = MediaCodec.createEncoderByType(encoder);
        mAudioEncoder.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


    }

    private void configureAACAudioEncoder(String encoder) throws IOException {
        int aacProfileLevel = -1;
        try {
            aacProfileLevel = mAudioFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
        } catch (NullPointerException e) {
            Log.w(TAG, "", e.fillInStackTrace());
        }
        switch(aacProfileLevel) {
            case MediaCodecInfo.CodecProfileLevel.AACObjectLC:
            {
                int numCodecs = MediaCodecList.getCodecCount();
                boolean isVendorHwAACEncoderFound = false;
                for (int i = 0; i < numCodecs; i++) {
                    MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                    if (!info.isEncoder() || !info.isVendor()) {
                         continue;
                    }
                    Log.d("QCAAC", "inifo name is " + info.getName());
                    for(String type : info.getSupportedTypes()) {
                        Log.d("QCAAC", "type:" + type + ", ishwAcc:" + info.isHardwareAccelerated());
                        if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC) &&
                               info.isHardwareAccelerated()) {
                           Log.d("QCAAC", "found qc aac.");
                           mAudioEncoder = MediaCodec.createByCodecName(info.getName());
                           mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mProfile.audioBitRate);
                           mAudioEncoder.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                           isVendorHwAACEncoderFound = true;
                           break;
                        }
                    }
                    if(isVendorHwAACEncoderFound)
                        break;
                }
                if(!isVendorHwAACEncoderFound){
                        Log.d("QCAAC", "QC HW AAC Encoder not found.");
                        Log.d("QCAAC", "Fallback to default encoder");
                        configureDefaultEncoder(encoder);
                }
                break;
            }
            case MediaCodecInfo.CodecProfileLevel.AACObjectHE:
            case MediaCodecInfo.CodecProfileLevel.AACObjectELD:
            default:
            {
                configureDefaultEncoder(encoder);
                break;
            }
        }
    }

    private void setupMediaCodecAudio() throws IOException {
        String encoderSelected = mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER);
        mProfile.audioCodec  = SettingTranslation.getAudioEncoder(encoderSelected);
        if (mProfile.audioCodec == MediaRecorder.AudioEncoder.AMR_NB) {
            if (!PersistUtil.enableMediaRecorder()) {
                mProfile.fileFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP;
            }
        }
        String encoder = getEncoderFormatAudio(encoderSelected);
        mAudioFormat = MediaFormat.createAudioFormat(encoder, mProfile.audioSampleRate,
                mProfile.audioChannels);
        if (encoderSelected.equals("aac-eld")) {
            mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD);
        } else if (encoderSelected.equals("he-aac")) {
            mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        } else if (encoderSelected.equals("aac")) {
            mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        }
        configureAACAudioEncoder(encoder);
    }

    private void doAudioEncoding() {
        boolean notDone = true;
        long prevPtsUs = 0;
        long frameGap = 0;
        boolean stopRec = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while(notDone && !stopRec){
            int encoderStatus = mAudioEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mAudioEncoder.getOutputFormat();
                Log.d(TAG + "_audio",MEDIACODEC_AUDIO_LOG, "received output format: " + newFormat);
                // should happen before receiving buffers, and should only happen once
                mTrackAudioIndex = mMuxer.addTrack(newFormat);
                mNumTracksAdded++;
                mMuxerAudioStop = false;
                Log.d(TAG + "_audio", "mNumTracksAdded is " + mNumTracksAdded);
                if (mNumTracksAdded == TOTAL_NUM_TRACKS) {
                    enableVideoButton(true);
                }
            } else if (encoderStatus < 0) {
                    Log.w(TAG + "_audio",MEDIACODEC_AUDIO_LOG, "unexpected result from encoder.dequeueOutputBuffer: "
                            + encoderStatus);
            } else {
                ByteBuffer encodedData;
                encodedData = mAudioEncoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("audio encoderOutputBuffer " + encoderStatus);
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                    Log.d(TAG + "_audio",MEDIACODEC_AUDIO_LOG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }
                if (mIsRecordingVideo && !mRecordingPausing && mMuxerStart
                        && bufferInfo.size != 0) {
                    //bufferInfo.presentationTimeUs = System.nanoTime()/1000;
                    Log.d(TAG + "_audio", MEDIACODEC_AUDIO_LOG,"prevPts is " + prevPtsUs);
                    if (mRecordingPausingTime > 0) {
                        if (mHighSpeedCapture && !mHighSpeedRecordingMode) {
                            bufferInfo.presentationTimeUs -= mHighRecordingPausingTime*1000;
                        } else {
                            bufferInfo.presentationTimeUs -= mRecordingPausingTime * 1000;
                        }
                    }
                    if ((bufferInfo.presentationTimeUs - prevPtsUs) > 0 ) {
                        frameGap = bufferInfo.presentationTimeUs - prevPtsUs;
                        prevPtsUs = bufferInfo.presentationTimeUs;
                        Log.d(TAG + "_audio",MEDIACODEC_AUDIO_LOG, "presPts is " + prevPtsUs + ",gap is " + frameGap);
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        try{

                        mMuxer.writeSampleData(mTrackAudioIndex, encodedData, bufferInfo);
                        Log.d(TAG + "_audio",MEDIACODEC_AUDIO_LOG, "sent " + bufferInfo.size +
                                    " bytes to muxer, ts=" + bufferInfo.presentationTimeUs);
                        }catch (Exception e){
                            Log.i(TAG,"e="+e.getMessage()+",bufferInfo.size="+bufferInfo.size+
                                    " bytes to muxer, ts=" + bufferInfo.presentationTimeUs
                                    +",stopRec="+stopRec);

                            if(!stopRec && mIsRecordingVideo) {
                                stopRec = true;
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopRecordingVideo(getMainCameraId());
                                    }
                                });
                            }
                        }

                    }
                }
                mAudioEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // reached EOS
                    Log.d(TAG + "_audio", MEDIACODEC_AUDIO_LOG,"end of audio stream reached");
                    mMuxerAudioStop = true;
                    notDone = false;
                }
            }
        }

    }

    private void setupAudioRecorder() {
        mAudioBufferSize = AudioRecord.getMinBufferSize(mProfile.audioSampleRate,
                mProfile.audioChannels, mAudioFormatNumber);
        //int iBufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
        // Ensure buffer is adequately sized for the AudioRecord
        // object to initialize
        //if (iBufferSize < iMinBufferSize)
        //    iBufferSize = ((iMinBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        Log.d(TAG + "_audio", MEDIACODEC_AUDIO_LOG,"setupAudioRecorder: buffer size=" + mAudioBufferSize +
                    ",audioChannels=" + mProfile.audioChannels + ",audioSampleRate="
                    + mProfile.audioSampleRate);
        int iAudioFormat = AudioFormat.CHANNEL_IN_STEREO;
        if (mProfile.audioChannels == 1) {
            iAudioFormat = AudioFormat.CHANNEL_IN_MONO;
        }

        mAudioRecord =  new AudioRecord.Builder()
                .setAudioFormat((new AudioFormat.Builder().setChannelMask(iAudioFormat))
                        .setSampleRate(mProfile.audioSampleRate)
                        .setEncoding(mAudioFormatNumber)
                        .build())
                .setAudioSource(PersistUtil.getAudioSource())
                .setBufferSizeInBytes(mAudioBufferSize*2)
                .build();
    }

    private void doAudioDecoding() {
        boolean endOfStream = false;
        // finished recording -> send it to the encoder
        while (!endOfStream) {
            ByteBuffer inputBuffer;
            byte[] mTempBuffer = new byte[mAudioBufferSize];
            int iReadResult = mAudioRecord.read(mTempBuffer, 0, mAudioBufferSize);
            if (iReadResult == AudioRecord.ERROR_BAD_VALUE
                    || iReadResult == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.d(TAG + "_audioinput",MEDIACODEC_AUDIO_LOG, "audio buffer read error: " + iReadResult);
                continue;
            }
            endOfStream = !mIsRecordingVideo && !mIsPreviewingVideo;
                try {
                    int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        inputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(mTempBuffer);
                        Log.d(TAG + "_audio", MEDIACODEC_AUDIO_LOG,"decode length:" + mTempBuffer.length + ",limit:"
                                    + inputBuffer.limit() + ",timestamp:" + System.nanoTime() / 1000);
                        mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, mTempBuffer.length,
                                System.nanoTime() / 1000,
                                endOfStream ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    }
                } catch (Throwable t) {
                    Log.e(TAG + "_audio", "sendFrameToAudioEncoder exception");
                    t.printStackTrace();
                }
        }
    };

    private void createMediaMuxer(FileDescriptor fd) {
        /**
         * Create a MediaMuxer. We can't add the video track and start() the
         * muxer until the encoder starts and notifies the new media format.
         */
        try {
            mMuxer = new MediaMuxer(fd, mProfile.fileFormat);
            mMuxerStart = false;
        } catch (IOException ioe) {
            throw new IllegalStateException("MediaMuxer create failed", ioe);
        }
    }

    private void createMediaMuxer(String outputFileName) {
        /**
         * Create a MediaMuxer. We can't add the video track and start() the
         * muxer until the encoder starts and notifies the new media format.
         */
        try {
            mMuxer = new MediaMuxer(outputFileName, mProfile.fileFormat);
            mMuxerStart = false;
        } catch (IOException ioe) {
            throw new IllegalStateException("MediaMuxer create failed", ioe);
        }
    }

    private void releaseMediaCodec() {
        // Release encoder
        if (mAudioRecord != null) {
            Log.v(TAG, "releasing audio recorder");
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        if (mVideoEncoder != null) {
            Log.v(TAG, "releasing video encoder");
            try {
                mVideoEncoder.stop();
                mVideoEncoder.release();
            } catch (MediaCodec.CodecException e) {
                Log.w(TAG, "releasing video encoder has exception.");
            }
            if (mVideoRecordingSurface != null) {
                mVideoRecordingSurface.release();
                mVideoRecordingSurface = null;
            }
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            Log.v(TAG, "releasing audio encoder");
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
        cleanupEmptyFile();
        deleteInvalidUri();
    }
    //------------------------------------------end-----------------------------------------
    private AudioDeviceInfo mBleInputDevice;

    private void setDefaultHDRParameters(AudioManager am) {
        // Set default values for HDR/3D Audio settings
        long startsetDefaultHDRParam = System.currentTimeMillis();
        am.setParameters("hdr_record_on=false");
        am.setParameters("wnr_on=false");
        am.setParameters("ans_on=false");
        // Set orientation and inverted based on current display orientation
        am.setParameters((mOrientation == 90 || mOrientation == 180)
                            ? "inverted=true" : "inverted=false");
        am.setParameters((mOrientation == 90 || mOrientation == 270)
                            ? "orientation=landscape" : "orientation=portrait");
        am.setParameters("facing=none");
        am.setParameters("hdr_audio_channel_count=0");
        am.setParameters("hdr_audio_sampling_rate=0");
        if(mActivity.getPerformenceTest()) {
            long time = System.currentTimeMillis() - startsetDefaultHDRParam;
            mHasMapTimes.put("startSetDefaultHDRParam->endSetDefaultHDRParam", System.currentTimeMillis() - startsetDefaultHDRParam);
        }
    }

    private final BroadcastReceiver mBTConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BLE, onReceive: " + intent.getAction());
            if (mCurrentSceneMode.mode != CameraMode.VIDEO) return;
            String action = intent.getAction();
            if (BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED.equals(action)) {
                if (mSettingsManager.getValue(SettingsManager.KEY_AUDIO_BLE).equals("On") && isBLEConnected()) {
                    boolean result = mMediaRecorder.setPreferredDevice(mBleInputDevice);
                    Log.i(TAG, "BLE, setPreferredDevice ble " + result);
                }
            }
        }
    };

    private void configurateAudio(int camId) {
        int audioEncoder = SettingTranslation
                .getAudioEncoder(mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER));
        // Get audio recording mode from SettingTranslation. 0 default, 1 hdr
        int audioRecordingMode = SettingTranslation
                .getAudioRecordingMode(mSettingsManager.getValue(SettingsManager.KEY_AUDIO_RECORDING_MODE));

        // Get HDR WNR mode. (0 off, 1 on)
        int hdrWnr = SettingTranslation
                .getHdrWnrMode(mSettingsManager.getValue(SettingsManager.KEY_HDR_WNR_MODE));

        // Get HDR ANS mode. (0 off, 1 on)
        int hdrAns = SettingTranslation
                .getHdrAnsMode(mSettingsManager.getValue(SettingsManager.KEY_HDR_ANS_MODE));

        boolean hfr = mHighSpeedCapture && !mHighSpeedRecordingMode;

        AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,configurateAudio -- setDefaultHDRParameters");
        setDefaultHDRParameters(am);
        if(audioRecordingMode == SettingTranslation.AudioRecordingModeHDR) {
            am.setParameters("hdr_record_on=true");
            am.setParameters((hdrWnr == 0) ? "wnr_on=false" : "wnr_on=true");
            am.setParameters((hdrAns == 0) ? "ans_on=false" : "ans_on=true");
            am.setParameters("hdr_audio_channel_count=4");
            am.setParameters("hdr_audio_sampling_rate=48000");
            Log.d(TAG, "cameraId is " + mSettingsManager.isFacingFront(camId) +
                    ", mOrientation is " + mOrientation);
            am.setParameters(mSettingsManager.isFacingFront(camId) ? "facing=front" : "facing=back");
            am.setParameters((mOrientation == 90 || mOrientation == 180)
                    ? "inverted=true" : "inverted=false");
            am.setParameters((mOrientation == 90 || mOrientation == 270)
                    ? "orientation=landscape" : "orientation=portrait");
        }
        if (TRACE_DEBUG) Trace.endSection();


        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,configurateAudio -- mMediaRecorder set source");
        if (!mCaptureTimeLapse && !hfr && !mSuperSlomoCapture && (-1 != audioEncoder)) {
            String value = SystemProperties.get("vendor.audio.hdr.spf.record.enable", "false");
            Log.i(TAG, "HDR Enabled on SPF: " + value);
            // Set audio source as unprocessed if HDR enabled and SPF property not set
            if(value.equals("false") && audioRecordingMode == SettingTranslation.AudioRecordingModeHDR) {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
            } else {
                mMediaRecorder.setAudioSource(PersistUtil.getAudioSource());
            }
            mProfile.audioCodec = audioEncoder;
            if (mProfile.audioCodec == MediaRecorder.AudioEncoder.AMR_NB) {
                mProfile.fileFormat = MediaRecorder.OutputFormat.THREE_GPP;
            }
        }
        if (TRACE_DEBUG) Trace.endSection();
        if (mSettingsManager.getValue(SettingsManager.KEY_AUDIO_BLE).equals("On")) {
            if (mBleInputDevice != null) {
                boolean result = mMediaRecorder.setPreferredDevice(mBleInputDevice);
                Log.i(TAG, "BLE, setPreferredDevice ble " + result);
            }
        }
    }

    private boolean setupMediaRecorder(int cameraId) throws IOException {
        long startSetMedia = System.currentTimeMillis();

        if (mSettingsManager.isMultiCameraEnabled() && !mSettingsManager.isLogicalEnable()){
            mMediaRecorder = null;
            return true;
        }

        Bundle myExtras = mActivity.getIntent().getExtras();
        if (mMediaRecorder == null) mMediaRecorder = new MediaRecorder();
        long startResetMedia = System.currentTimeMillis();

        mMediaRecorder.reset();
        if(mActivity.getPerformenceTest()) {
            mHasMapTimes.put("buttonClick->startResetMedia",startResetMedia - mStartedTime);
            mHasMapTimes.put("startResetMedia->endResetMedia", System.currentTimeMillis() - startResetMedia);
        }

        int videoWidth = mProfile.videoFrameWidth;
        int videoHeight = mProfile.videoFrameHeight;
        mUnsupportedResolution = false;

        int videoEncoder = SettingTranslation
                .getVideoEncoder(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER));
        if(("dolby").equals(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER)) && mIntentMode == INTENT_MODE_VIDEO){
            videoEncoder = MediaRecorder.VideoEncoder.H264;
        }
        Log.d(TAG,"videoEncoder="+ videoEncoder+
                " settings="+mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER));
        mProfile.videoCodec = videoEncoder;

        String audioSelected = mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER);
        int audioEncoder = -1;
        if (PersistUtil.needAudioEncoder() && !audioSelected.equals("off")) {
            long startconfigurateAudio = System.currentTimeMillis();
            configurateAudio(cameraId);
            if(mActivity.getPerformenceTest()) {
                mHasMapTimes.put("startConfigurateAudio->endConfigurateAudio", System.currentTimeMillis() - startconfigurateAudio);
            }
        }
        if (isVideoEncoderProfileSupported()) {
            int videoEncoderProfile = SettingTranslation.getVideoEncoderProfile(
                    mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE));
            Log.d(TAG, "setVideoEncodingProfileLevel: " + videoEncoderProfile + " " + MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
            mMediaRecorder.setVideoEncodingProfileLevel(videoEncoderProfile,
                    MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
        } else if (("dolby").equals(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER)) && mIntentMode == INTENT_MODE_NORMAL) {
            Log.i(TAG, "set dolby profile.");
            mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt,
                    MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelFhd30);
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        setVideoOutputFile(myExtras);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        if (!mHighSpeedCapture) {
            updateBitrateForNonHFR(videoEncoder, mProfile.videoBitRate, videoHeight, videoWidth);
        }
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncoder(videoEncoder);
        Log.d(TAG," mMediaRecorder.setVideoEncoder=" + videoEncoder);
        if (PersistUtil.needAudioEncoder() && !audioSelected.equals("off")) {
            audioEncoder = SettingTranslation
                    .getAudioEncoder(mSettingsManager.getValue(SettingsManager.KEY_AUDIO_ENCODER));
        }
        if (!mCaptureTimeLapse && !(mHighSpeedCapture && !mHighSpeedRecordingMode)
                && !mSuperSlomoCapture && (-1 != audioEncoder)) {
            mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
            mMediaRecorder.setAudioChannels(mProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(audioEncoder);
        }
        mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);

        Log.d(TAG, "Profile video frame rate: "+ mProfile.videoFrameRate);
        if (mCaptureTimeLapse) {
            double fps = 1000 / (double) mTimeBetweenTimeLapseFrameCaptureMs;
            mMediaRecorder.setCaptureRate(fps);
        }  else if (mHighSpeedCapture) {
            mHighSpeedFPSRange = new Range(mHighSpeedCaptureRate, mHighSpeedCaptureRate);
            mHighSpeedPreviewFPSRange =  new Range(30, mHighSpeedCaptureRate);
            int fps = (int) mHighSpeedFPSRange.getUpper();
            int targetRate = mSuperSlomoCapture ? 30 : (mHighSpeedRecordingMode ? fps : 30);
            mMediaRecorder.setCaptureRate(mSuperSlomoCapture ? 30 : fps);
            mMediaRecorder.setVideoFrameRate(targetRate);
            int scaledBitrate = mSettingsManager.getHighSpeedVideoEncoderBitRate(mProfile, targetRate, fps);
            if (PersistUtil.getBitRate() != -1){
                scaledBitrate = PersistUtil.getBitRate();
            }
            Log.i(TAG, "Capture rate: "+fps+", Target rate: "+targetRate+", Scaled video bitrate : " + scaledBitrate);
            mMediaRecorder.setVideoEncodingBitRate(scaledBitrate);
        }

        long requestedSizeLimit = 0;
        if (isVideoCaptureIntent() && myExtras != null) {
            requestedSizeLimit = myExtras.getLong(MediaStore.EXTRA_SIZE_LIMIT);
        }
        setMaxFileSize(requestedSizeLimit);
        setOrientationHint(cameraId);
        //check if codec supports the resolution, otherwise throw toast
        Log.d(TAG,"mProfile.videoCodec="+mProfile.videoCodec);
        String type = SettingTranslation.getVideoEncoderType(mProfile.videoCodec);
        Log.d(TAG,"codec type="+type);
        MediaCodecList allCodecs = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaFormat format = MediaFormat.createVideoFormat(type,videoWidth,videoHeight);
        try{
            String encodeName = allCodecs.findEncoderForFormat(format);
            Log.d(TAG,"encodeName="+encodeName);
        } catch (IllegalArgumentException| NullPointerException e){
            Log.d(TAG,"error="+e.getLocalizedMessage());
            mUnsupportedResolution = true;
        } finally {
            if (mUnsupportedResolution) {
                warningToast(R.string.error_app_unsupported);
                return false;
            }
        }
        mMediaRecorder.setInputSurface(mVideoRecordingSurface);
        long startPrepareMedia = System.currentTimeMillis();
        boolean preparemedia = prepareMediaRecorder();
        if(mActivity.getPerformenceTest()) {
            mHasMapTimes.put("startSetUpMedia->endSetUpMedia", System.currentTimeMillis() - startSetMedia);
        }
        return preparemedia;
    }

    private boolean prepareMediaRecorder() {
        try {
            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,prepareMediaRecorder");
            long startime = System.currentTimeMillis();
            mMediaRecorder.prepare();
            if(mActivity.getPerformenceTest()) {
                mHasMapTimes.put("startPrepareMedia->endPrepareMedia", System.currentTimeMillis()- startime);
            }
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setOnInfoListener(this);
            if (mSettingsManager.getValue(SettingsManager.KEY_AUDIO_BLE).equals("On")
                    && mBleInputDevice != null) {
                List<MicrophoneInfo> microphoneInfos =  mMediaRecorder.getActiveMicrophones();
                for (MicrophoneInfo microphoneInfo : microphoneInfos) {
                    Log.i(TAG, "BLE On, Active microphone info " + microphoneInfo.getType());
                }
            }
            if (TRACE_DEBUG) Trace.endSection();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "prepare failed for " + mVideoFilename + e);
            if (mCurrentVideoUri != null) {
                mContentResolver.delete(mCurrentVideoUri, null);
                mCurrentVideoUri = null;
            }
            releaseMediaRecorder();
            mCaptureSession[getMainCameraId()] = null;
            mCurrentSession = null;
            quitVideoToPhotoWithError(e.getMessage());
            return false;
        }
    }

    private void setVideoOutputFile(Bundle myExtras) {
        if (mIntentMode == CaptureModule.INTENT_MODE_VIDEO && myExtras != null) {
            Uri saveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            if (saveUri != null) {
                Log.v(TAG, "setVideoOutputFile, extra uri " + saveUri);
                try {
                    mVideoFileDescriptor =
                            mContentResolver.openFileDescriptor(saveUri, "rw");
                    mCurrentVideoUri = saveUri;
                } catch (java.io.FileNotFoundException ex) {
                    // invalid uri
                    Log.e(TAG, ex.toString());
                }
            } else {
                generateVideoOutputFile();
            }
        } else {
            generateVideoOutputFile();
        }
        if (mVideoFileDescriptor != null) {
            if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                mMediaRecorder.setOutputFile(mVideoFileDescriptor.getFileDescriptor());
            } else {
                createMediaMuxer(mVideoFileDescriptor.getFileDescriptor());
            }
        } else {
            File cacheDir = mActivity.getExternalCacheDir();
            if (cacheDir != null) {
                cacheDir.mkdirs();
                String tmpFileName = cacheDir.getAbsolutePath() + "/tmp.mp4";
                Log.d(TAG, "setOutputFile, tmp " + tmpFileName);
                if (PersistUtil.enableMediaRecorder() && mMediaRecorder != null) {
                    mMediaRecorder.setOutputFile(tmpFileName);
                } else {
                    createMediaMuxer(tmpFileName);
                }
            } else {
                Log.e(TAG, "getExternalCacheDir return null");
            }
        }
    }

    private void generateVideoOutputFile() {
        if (mIsRecordingVideo
                || (mHighSpeedCapture && mHighSpeedCaptureRate > NORMAL_SESSION_MAX_FPS)
                || !PersistUtil.enableMediaRecorder()) {
            String fileName = generateVideoFilename(mProfile.fileFormat);
            Uri videoTable = Storage.getVideoBaseUri();
            long startInsertVideo = System.currentTimeMillis();
            Uri videoUri = mContentResolver.insert(videoTable, mCurrentVideoValues);
            if(mActivity.getPerformenceTest()) {
                mHasMapTimes.put("startInsertVideoTable->endInsertVideoTable", System.currentTimeMillis() - startInsertVideo);
            }
            Log.i(TAG, "New video filename: " + fileName + ",new video uri: " + videoUri);

            try {
                mVideoFileDescriptor =
                        mContentResolver.openFileDescriptor(videoUri, "rw");
                mCurrentVideoUri = videoUri;
                Log.d(TAG, "add invalid uri " + mCurrentVideoUri);
                mUrisInvalid.add(mCurrentVideoUri);
            } catch (java.io.FileNotFoundException ex) {
                // invalid uri
                Log.d(TAG, "remove invalid uri " + mCurrentVideoUri);
                mUrisInvalid.remove(mCurrentVideoUri);
                mContentResolver.delete(videoUri, null, null);
                mCurrentVideoUri = null;
                Log.e(TAG, ex.toString());
            }
        }
    }

    public void onVideoButtonClick() {
        if (!isRecorderReady() || getCameraMode() == DUAL_MODE || (
                getCurrenCameraMode() != CameraMode.VIDEO &&
                        getCurrenCameraMode() != CameraMode.HFR &&
                        getCurrenCameraMode() != CameraMode.CINEMATIC)) return;
        if(mActivity.getPerformenceTest()) {
            mStartedTime = System.currentTimeMillis();
            Log.i(TAG," onVideoButtonClick mIsRecordingVideo="+mIsRecordingVideo);
        }
        if (!mIsRecordingVideo && mRecordingStoped) {
            if (!triggerVideoRecording(getMainCameraId())) {
                // Show ui when start recording failed.
                mUI.showUIafterRecording();
                if (PersistUtil.enableMediaRecorder()) {
                    mFrameProcessor.setVideoOutputSurface(null);
                } else {
                    stopCodecThreads();
                    releaseMediaCodec();
                }
            }
        } else if (mRecordingStarted) {
            stopRecordingVideo(getMainCameraId());
        }else{
            warningToast("recording has not ready.");
        }
    }

    @Override
    public void onShutterButtonClick() {
        if(mActivity.getPerformenceTest()) {
            mStartedTime = System.currentTimeMillis();
        }
        if (mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.i(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            return;
        }
        if (TRACE_DEBUG) Trace.beginSection("onShutterButtonClick");
        mLongshoting = false;
        mNumFramesArrived.getAndSet(0);
        Log.i(TAG,"onShutterButtonClick");
        int id = getMainCameraId();
        if (mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            if (!isHighSpeedRateCapture()){
                if (mUI.isShutterEnabled() && mRecordingStarted) {
                    captureVideoSnapshot(id);
                }
            }
            return;
        }
        String timer = mSettingsManager.getValue(SettingsManager.KEY_TIMER);
        int seconds = Integer.parseInt(timer);
        // When shutter button is pressed, check whether the previous countdown is
        // finished. If not, cancel the previous countdown and start a new one.
        if (mUI.isCountingDown()) {
            mUI.cancelCountDown();
        }
        if (seconds > 0) {
            mUI.startCountDown(seconds, true);
        } else {
            if (mChosenImageFormat == ImageFormat.YUV_420_888 && mPostProcessor.isItBusy()) {
                warningToast("It's still busy processing previous scene mode request.");
                return;
            }
            mTakingPicture[id] = true;
            if (mCurrentSceneMode.mode != CameraMode.PRO_MODE)
                mUI.enableZoomSeekBar(false);
            checkSelfieFlashAndTakePicture();
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void warningToast(final String msg) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                RotateTextToast.makeText(mActivity, msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void warningToast(final int sourceId) {
        warningToast(sourceId, true);
    }

    private void warningToast(final int sourceId, boolean isLongShow) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                RotateTextToast.makeText(mActivity, sourceId,
                        isLongShow ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isLongShotSettingEnabled() {
        String longshot = mSettingsManager.getValue(SettingsManager.KEY_LONGSHOT);
        if(longshot.equals("on")) {
            return true;
        }
        return false;
    }

    @Override
    public void onShutterButtonLongClick() {
        if (isBackCamera() && getCameraMode() == DUAL_MODE) return;

        if (isLongShotSettingEnabled()) {
            //Cancel the previous countdown when long press shutter button for longshot.
            if (mUI.isCountingDown()) {
                mUI.cancelCountDown();
            }
            //check whether current memory is enough for longshot.
            mActivity.updateStorageSpaceAndHint();

            long storageSpace = mActivity.getStorageSpaceBytes();
            int mLongShotCaptureCountLimit = PersistUtil.getLongshotShotLimit();

            if (storageSpace <= Storage.LOW_STORAGE_THRESHOLD_BYTES + mLongShotCaptureCountLimit
                    * mJpegFileSizeEstimation) {
                Log.i(TAG, "Not enough space or storage not ready. remaining=" + storageSpace);
                return;
            }

            if (isLongshotNeedCancel()) {
                mLongshotActive = false;
                mUI.enableVideo(!mLongshotActive);
                return;
            }
            Log.i(TAG, "Start Longshot");
            mLongshotActive = true;
            mTakingPicture[getMainCameraId()] = true;
            mNumFramesArrived.getAndSet(0);
            mNumImageArrived.getAndSet(0);
            mUI.enableVideo(!mLongshotActive);
            if (mCurrentSceneMode.mode != CameraMode.PRO_MODE) {
                mUI.enableZoomSeekBar(false);
            }
            checkSelfieFlashAndTakePicture();
        } else {
            RotateTextToast.makeText(mActivity, "Long shot not support", Toast.LENGTH_SHORT).show();
        }
    }

    private void estimateJpegFileSize() {
        String quality = mSettingsManager.getValue(SettingsManager
            .KEY_JPEG_QUALITY);
        int[] ratios = mActivity.getResources().getIntArray(R.array.jpegquality_compression_ratio);
        String[] qualities = mActivity.getResources().getStringArray(
                R.array.pref_camera_jpegquality_entryvalues);
        int ratio = 0;
        for (int i = ratios.length - 1; i >= 0; --i) {
            if (qualities[i].equals(quality)) {
                ratio = ratios[i];
                break;
            }
        }
        String pictureSize = mSettingsManager.getValue(SettingsManager
                .KEY_PICTURE_SIZE);

        Size size = parsePictureSize(pictureSize);
        if (ratio == 0) {
            Log.d(TAG, "mJpegFileSizeEstimation 0");
        } else {
            mJpegFileSizeEstimation =  size.getWidth() * size.getHeight() * 3 / ratio;
            Log.d(TAG, "mJpegFileSizeEstimation " + mJpegFileSizeEstimation);
        }

    }

    private boolean isLongshotNeedCancel() {
        if (PersistUtil.getSkipMemoryCheck()) {
            return false;
        }

        if (Storage.getAvailableSpace() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.w(TAG, "current storage is full");
            return true;
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long remainMemory = maxMemory - totalMemory;

        if (remainMemory <= LONGSHOT_CANCEL_THRESHOLD) {
            Log.e(TAG, "cancel longshot: free=" + remainMemory
                    + " threshold=" + LONGSHOT_CANCEL_THRESHOLD);
            RotateTextToast.makeText(mActivity, R.string.msg_cancel_longshot_for_limited_memory,
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if ( mIsRecordingVideo ) {
            Log.e(TAG, " cancel longshot:not supported when recording");
            return true;
        }
        return false;
    }

    private boolean isFlashOff(int id) {
        if (!mSettingsManager.isFlashSupported(id)) return true;
        return mSettingsManager.getValue(mCurrentSceneMode.mode == CameraMode.PRO_MODE ?
                SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE).equals("off");
    }

    private boolean isFlashOn(int id) {
        if (!mSettingsManager.isFlashSupported(id)) return false;
        String value = mSettingsManager.getValue(mCurrentSceneMode.mode == CameraMode.PRO_MODE ?
                SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
        return value.equals("on");
    }

    private void initializePreviewConfiguration(int id) {
        mPreviewRequestBuilder[id].set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_IDLE);
        applyFlash(mPreviewRequestBuilder[id], id);
        applyCommonSettings(mPreviewRequestBuilder[id], id);
        if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
            Log.v(TAG, "initializePreviewConfiguration set SENSOR_PIXEL_MODE as default " );
            mPreviewRequestBuilder[id].set(CaptureRequest.SENSOR_PIXEL_MODE,
                    CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT);
        }
    }

    public float getZoomValue() {
        return mZoomValue;
    }

    public Rect cropRegionForZoom(int id, boolean isMaxPixelMode) {
        Log.d(TAG, "cropRegionForZoom " + id);
        Rect activeRegion = null;
        if (isMaxPixelMode) {
            activeRegion = mSettingsManager.getSensorActiveMaxArraySize(id);
            if (activeRegion == null) {
                activeRegion = mSettingsManager.getSensorActiveArraySize(id);
            }
        } else {
            activeRegion = mSettingsManager.getSensorActiveArraySize(id);
        }
        Rect cropRegion = new Rect();

        int xCenter = activeRegion.width() / 2;
        int yCenter = activeRegion.height() / 2;
        int xDelta = (int) (activeRegion.width() / (2 * mZoomValue));
        int yDelta = (int) (activeRegion.height() / (2 * mZoomValue));
        cropRegion.set(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
        if (mZoomValue == 1f) {
            mOriginalCropRegion[id] = cropRegion;
        } else {
            if (mOriginalCropRegion[id] == null) {
                Rect originalRegion = new Rect();
                int xOrigDelata = (int) (activeRegion.width() / 2);
                int yOrigDelata = (int) (activeRegion.height() / 2);
                originalRegion.set(xCenter - xOrigDelata, yCenter - yOrigDelata,
                        xCenter + xOrigDelata, yCenter + yOrigDelata);
                mOriginalCropRegion[id] = originalRegion;
            }
        }
        if (mZoomValue < 1.0f) {
            mCropRegion[id] = mOriginalCropRegion[id];
        } else {
            mCropRegion[id] = cropRegion;
        }
        Log.d(TAG, "cropRegionForZoom  mCropRegion[id] " +  mCropRegion[id]);
        synchronized (mPerformanceDebugData) {
            mZoomValueMap.put(mZoomValue, mCropRegion[id]);
        }
        return mCropRegion[id];
    }

    private void applyZoomRatio(CaptureRequest.Builder request, float zoomValue, int id) {
        try {
            if (request.get(CaptureRequest.SENSOR_PIXEL_MODE) != null &&
                    request.get(CaptureRequest.SENSOR_PIXEL_MODE) ==
                            CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION) {
                cropRegionForZoom(id, true);
            } else {
                cropRegionForZoom(id, false);
            }
            Log.i(TAG,"applyzoomratio="+zoomValue);
            mZoomValue = zoomValue;
            request.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomValue);
        } catch(IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG," there is no vendorTag CONTROL_ZOOM_RATIO");
        } catch (NoSuchFieldError e) {
            Log.w(TAG, EXCEPTION_LOG,"applyZoomRatio NoSuchFieldError CONTROL_ZOOM_RATIO");
        }
    }

    private void applyZoom(CaptureRequest.Builder request, int id) {
        if (!mSupportZoomCapture) return;
        if (request.get(CaptureRequest.SENSOR_PIXEL_MODE) != null &&
                request.get(CaptureRequest.SENSOR_PIXEL_MODE) ==
                CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION) {
            request.set(CaptureRequest.SCALER_CROP_REGION, cropRegionForZoom(id, true));
        } else {
            request.set(CaptureRequest.SCALER_CROP_REGION, cropRegionForZoom(id, false));
        }
    }

    private void applyInstantAEC(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_INSTANT_AEC);
        if (value == null || value.equals("0"))
            return;
        int intValue = Integer.parseInt(value);
        request.set(CaptureModule.INSTANT_AEC_MODE, intValue);
    }

    private void applyIntegratedMode(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_INTEGRATED_MODE);
        if (value == null || !mSettingsManager.isIntegratedModeSupported()) 
            return;
        int intValue = (value.equals("Off") ? 0 : 2);
        request.set(INTEGRATED_MODE, intValue);
    }

    private void applySaturationLevel(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SATURATION_LEVEL);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            request.set(CaptureModule.SATURATION, intValue);
        }
    }

    private void applyAntiBandingLevel(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_ANTI_BANDING_LEVEL);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            request.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, intValue);
        }
    }

    private void applyBokehMode(boolean enable) {
        CaptureRequest.Builder captureRequest = mPreviewRequestBuilder[getMainCameraId()];
        if (!checkSessionAndBuilder(mCaptureSession[getMainCameraId()], captureRequest) ||
                mCurrentSessionClosed ||mPaused) {
            return;
        }
        try {
            if (enable) {
                captureRequest.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE,
                        CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_CONTINUOUS);
            } else {
                captureRequest.set(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE,
                        CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_DISABLED);
            }
            mCaptureSession[getMainCameraId()].setRepeatingRequest(captureRequest.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException| IllegalArgumentException | UnsupportedOperationException | IllegalStateException e) {
            Log.e(TAG, "Camera Exception in applyBokehMode, apply failed e="+e);
        }
    }

    private void applyBufferMode(CaptureRequest.Builder request){
        if(!mSettingsManager.isSupportedSuperBuffer(getMainCameraId())){
            return;
        }
        try {
            String buffermode = mSettingsManager.getValue(SettingsManager.KEY_HFR_BUFFER_MODE);
            if(buffermode != null && buffermode.equals("1")) {
                request.set(CaptureModule.outputBufferComb, 1);
            }else{
                request.set(CaptureModule.outputBufferComb, 0);
            }
        }catch (IllegalArgumentException e){
            Log.w(TAG,EXCEPTION_LOG,"exception e="+e);
        }
    }
    private void applySharpnessControlModes(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SHARPNESS_CONTROL_MODE);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            try {
                request.set(CaptureModule.sharpness_control, intValue);
            } catch (IllegalArgumentException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
    }

    private void applyAfModes(CaptureRequest.Builder request) {
        if (getDevAfMode() != -1) {
            request.set(CaptureRequest.CONTROL_AF_MODE, getDevAfMode());
        }
    }

    private int getDevAfMode() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_AF_MODE);
        int intValue = -1;
        if (value != null) {
            intValue = Integer.parseInt(value);
        }
        return intValue;
    }

    private void applyEIS(CaptureRequest.Builder request) {
        String key = SettingsManager.KEY_PHOTO_EIS_VALUE;
        if (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.HFR ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) {
            key = SettingsManager.KEY_EIS_VALUE;
        }
        String value = mSettingsManager.getValue(key);

        Log.d(TAG,  "applyEIS key: " + key + ", value: " + value);
        boolean previewStabilizationOn = false;
        if (value != null) {
            if (value.equals("V2") || value.equals("dynamic")) {
                previewStabilizationOn = "enable".equals(mSettingsManager.
                        getValue(SettingsManager.KEY_PREVIEW_STABILIZATION));
            }
            Log.d(TAG,  "applyEIS previewStabilizationOn: " + previewStabilizationOn );
            if (!previewStabilizationOn) {
                try {
                    applyVideoStabilization(request, value.equals("disable"));
                    if (value.equals("V2")) {
                        VendorTagUtil.setEISModeForSessionParameter(request, 1);// EISModeRealTime == 1
                    } else if (value.equals("V3")) {
                        VendorTagUtil.setEISModeForSessionParameter(request, 0);// EISModeLookAhead == 0
                    } else if (value.equals("dynamic")) {
                        VendorTagUtil.setEISModeForSessionParameter(request, 2);// EISModeDynamicMargin == 2
                    }
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, EXCEPTION_LOG, e.toString());
                }
            } else {
                try {
                    Log.d(TAG, "applyEIS PREVIEW_STABILIZATION");
                    request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION);
                    if (value.equals("dynamic")) {
                        VendorTagUtil.setEISModeForSessionParameter(request, 2);// EISModeDynamicMargin == 2
                    }
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, e.toString());
                }
            }
        }
    }

    private void applyEarlyPCR(CaptureRequest.Builder request) {
        try {
            String value = mSettingsManager.getValue(SettingsManager.KEY_EARLY_PCR_NUM);
            Log.v(TAG, " KEY_EARLY_PCR_NUM value :" + value);
            if(value == null) {
                request.set(CaptureModule.earlyPCR, 1);
            }else{
                int valueint = CameraUtil.strToInt(value,1);
                request.set(CaptureModule.earlyPCR, valueint);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void applyEnableStatsVisualizer(CaptureRequest.Builder request) {
        try {
            byte value = 0;
            String enable = mSettingsManager.getValue(
                    SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
            Log.v(TAG, " applyEnableStatsVisualizer enable :" + enable);
            if ("1".equals(enable)) {
                value = 1;
            }
            request.set(enable_statsvisualizer, value);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG," there is no vendorTag enable_statsvisualizer");

        }
    }

    private void applyShadingCorrection(CaptureRequest.Builder request) {
        if (!mSettingsManager.isShadingCorrectionSupported())
            return;
        try {
            byte value = 1;
            String shadingCorrection = mSettingsManager.getValue(
                    SettingsManager.KEY_SHADING_CORRECTION);
            if ("0".equals(shadingCorrection)){
                value = 0;
            }
            request.set(CaptureModule.shading_correction, value);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG," applyShadingCorrection no vendorTag: " + shading_correction);
        }
    }

    private void applyOfflineDumpTrigger(CaptureRequest.Builder request) {
        try {
            int value = 1;
            String offlineDumpTrigger = mSettingsManager.getValue(
                    SettingsManager.KEY_OFFLINE_DUMP_TRIGGER);
            if ("0".equals(offlineDumpTrigger)){
                value = 0;
            }
            request.set(offline_dump_trigger_enabled, value);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG,"applyOfflineDumpTrigger no vendorTag: " + offline_dump_trigger_enabled);
        }
    }

    private void applyNumHDRExposure(CaptureRequest.Builder request) {
        if(CURRENT_MODE != CameraMode.DEFAULT)
            return;

        try {
            int value = -1;
            String autoHDR = mSettingsManager.getValue(SettingsManager.KEY_AUTO_HDR);
            Log.v(TAG, " applyNumHDRExposure value :" + value + ", autoHDR :" + autoHDR);
            if ("enable".equals(autoHDR)){
                final SharedPreferences pref = mActivity.getSharedPreferences(
                        ComboPreferences.getGlobalSharedPreferencesName(mActivity),
                        Context.MODE_PRIVATE);
                value = pref.getInt(SettingsManager.KEY_WARM_START_EXPOSURE_COUNT, -1);
                Log.v(TAG, " applyNumHDRExposure value :" + value);
                if (value != -1) {
                    request.set(numHDRexposure, value);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"applyNumHDRExposure hal no vendorTag : " + numHDRexposure);
        }
    }

    private void applyMcxMasterCb(CaptureRequest.Builder request) {
        try {
            request.set(CaptureModule.mcxMasterCb, 1);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + mcxMasterCb);
        }
    }

    private void applyMcxRawCbInfo(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_RAW_CB_INFO);
        try {
            request.set(CaptureModule.mcxRawCbInfo, value != null ? Integer.parseInt(value) : 0);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + mcxRawCbInfo);
        }
    }

    private void applyExtendMaxZoom(CaptureRequest.Builder request) {
        int enableMaxZoom = 0;
        if (isExtendedMaxZoomEnable()) {
            Log.v(TAG, "applyExtendMaxZoom enableMaxZoom = 1" );
            enableMaxZoom = 1;
        }
        try {
            request.set(CaptureModule.extendedMaxZoom, enableMaxZoom);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + extendedMaxZoom);
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal UnsupportedOperationException : " + extendedMaxZoom);
        }

    }

    private void applySensorModeFS2(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SENSOR_MODE_FS2_VALUE);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            byte fs2 =(byte)((intValue == 0) ? 0x00 : 0x01);
            Log.v(TAG, "applySensorModeFS2 intValue : " + intValue + ", fs2 :" + fs2);
            try {
                request.set(CaptureModule.sensor_mode_fs, fs2);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + sensor_mode_fs);
            }
        }
    }

    private void applyExposureMeteringModes(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE_METERING_MODE);
        if (value != null) {
            int intValue = Integer.parseInt(value);
            request.set(CaptureModule.exposure_metering, intValue);
        }
    }

    private void applyHistogram(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        if (value != null ) {
            if (value.contains("2")) {
                final byte enable = 1;
                request.set(CaptureModule.histMode, enable);
                mHiston = true;
                return;
            }
        }
        mHiston = false;
        updateGraghViewVisibility(View.GONE);
    }

    private void applyBGStats(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        if (value != null ) {
            if (value.contains("0")){
                final byte enable = 1;
                try{
                    request.set(CaptureModule.bgStatsMode, enable);
                    mBGStatson = true;
                } catch (IllegalArgumentException e) {
                    mBGStatson = false;
                }
                if (mBGStatson) {
                    updateBGStatsVisibility(View.VISIBLE);
                    updateBGStatsView();
                }
                return;
            }
        }
        mBGStatson = false;
        updateBGStatsVisibility(View.GONE);
    }

    private void applyBEStats(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        if (value != null ) {
            if (value.contains("1")){
                final byte enable = 1;
                try{
                    request.set(CaptureModule.beStatsMode, enable);
                    mBEStatson = true;
                }catch (IllegalArgumentException e) {
                    mBEStatson = false;
                }
                if (mBEStatson) {
                    updateBEStatsVisibility(View.VISIBLE);
                    updateBEStatsView();
                }
                return;
            }
        }
        mBEStatson = false;
        updateBEStatsVisibility(View.GONE);
    }

    private void applyWbColorTemperature(CaptureRequest.Builder request) {
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        mSettingsManager.getCurrentPrepNameKey()), Context.MODE_PRIVATE);
        String manualWBMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_WB);
        String cctMode = mActivity.getString(
                R.string.pref_camera_manual_wb_cct);
        if (manualWBMode.equals(cctMode)) {
            int colorTempValue = CameraUtil.strToInt(pref.getString(
                    SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, "5000"),5000);
            int colorTintValue = CameraUtil.strToInt(pref.getString(
                    SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, "0"),0);
            VendorTagUtil.setWbCCT(request,colorTempValue, colorTintValue);
        }
    }

    private void applyToneMapping(CaptureRequest.Builder request) {
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        mSettingsManager.getCurrentPrepNameKey()), Context.MODE_PRIVATE);
        String mode = mSettingsManager.getValue(SettingsManager.KEY_TONE_MAPPING);

        String darkBoost = mActivity.getString(R.string.pref_camera_tone_mapping_value_dark_boost_offset);
        String fourthTone = mActivity.getString(R.string.pref_camera_tone_mapping_value_fourth_tone_anchor);
        String userSetting = mActivity.getString(R.string.pref_camera_tone_mapping_value_user_setting);

        float currentDarkBoostValue = -1.0f;
        float currentFourthToneValue = -1.0f;
        if (mode.equals(darkBoost)) {
            currentDarkBoostValue = pref.getFloat(SettingsManager.KEY_TONE_MAPPING_DARK_BOOST, -1.0f);
            VendorTagUtil.setToneMappingDarkBoostValue(request, currentDarkBoostValue);
        } else if (mode.equals(fourthTone)) {
            currentFourthToneValue = pref.getFloat(SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE, -1.0f);
            VendorTagUtil.setToneMappingFourthToneValue(request, currentFourthToneValue);
        }else if (mode.equals(userSetting)){
            currentDarkBoostValue = pref.getFloat(SettingsManager.KEY_TONE_MAPPING_DARK_BOOST, -1.0f);
            VendorTagUtil.setToneMappingDarkBoostValue(request, currentDarkBoostValue);
            currentFourthToneValue = pref.getFloat(SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE, -1.0f);
            VendorTagUtil.setToneMappingFourthToneValue(request, currentFourthToneValue);
        } else {
            VendorTagUtil.setToneMappingDisableMode(request);
        }
        Log.d(TAG,"applyToneMapping, mode:" + mode + ",currentDarkBoostValue:" + currentDarkBoostValue + ",currentFourthToneValue:" + currentFourthToneValue);
    }


    private void applyManualHDR(CaptureRequest.Builder request) {
        String hdrmode = mSettingsManager.getVideoHdrMode();
        if (hdrmode != null ) {
            if (hdrmode.equals("auto")) {
                VendorTagUtil.setAudoHDRMode(request, 1);
            } else if(!hdrmode.equals("off")){
                String[] modeLists = hdrmode.split(" ");
                int[] modes = new int [3];
                int value = 0;
                for (int i = 0; i < modeLists.length; i ++) {
                    modes[i] = SettingsManager.KEY_HDR_MODES_ORDER.get(modeLists[i]);
                    if(modeLists[i].equals("MFHDR")) {
                        value |= 2;
                    } else if (modeLists[i].equals("SHDR")) {
                        value |= 1;
                    } else if (modeLists[i].equals("QHDR")) {
                        value |= 4;
                    }
                }
                mHDRModes = modes;
                mHDRValues = value;
                VendorTagUtil.setHDRModes(request, value);
                VendorTagUtil.setHDRModesPreference(request, modes);
            }
        }
    }

    private void applyQLL(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_QLL);
        if (value != null ) {
            Log.v(TAG, " applyQLL value :" + value);
            if (value.equals("1")) {
                VendorTagUtil.setQLLMode(request, 1);
            }
        }
    }

    private void applyInSensorZoom(CaptureRequest.Builder request) {
        Log.v(TAG, " applyInSensorZoom supported :" + !mSettingsManager.isInSensorZoomSupported());
        if (!mSettingsManager.isInSensorZoomSupported())
            return;
        try {
            int value = 0;
            String inSensorZoom = mSettingsManager.getValue(
                    SettingsManager.KEY_INSENSOR_ZOOM);
            Log.v(TAG, " applyInSensorZoom inSensorZoom :" + inSensorZoom);
            if ("0".equals(inSensorZoom)){
                request.set(CaptureModule.insensor_zoom_feature, value);
            } else {
                value = 1;
                request.set(CaptureModule.insensor_zoom_feature, value);
            }
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG," applyInSensorZoom didn`t exist vendorTag :" + insensor_zoom_feature);
        }
    }

    private void applyInStantZoom(CaptureRequest.Builder request) {
        try {
            String inStantZoom = mSettingsManager.getValue(
                    SettingsManager.KEY_INSTANT_ZOOM);
            Log.v(TAG, " applyInStantZoom inSensorZoom :" + inStantZoom);
            if ("on".equals(inStantZoom)) {
                request.set(CaptureRequest.CONTROL_SETTINGS_OVERRIDE,
                        CameraMetadata.CONTROL_SETTINGS_OVERRIDE_ZOOM);
            } else {
                request.set(CaptureRequest.CONTROL_SETTINGS_OVERRIDE,
                        CameraMetadata.CONTROL_SETTINGS_OVERRIDE_OFF);
            }
        } catch (IllegalArgumentException | NoSuchFieldError e) {
            Log.v(TAG, EXCEPTION_LOG, " applyInStantZoom didn`t exist CONTROL_SETTINGS_OVERRIDE");
        }
    }

    private void applyCroppedRaw(OutputConfiguration configuration, int cameraId) {
        try {
            Log.d(TAG,"set cropped raw for raw steam:" + cameraId);
            long useCaseId = CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW;
            if(mSettingsManager.isAvailableUseCase(cameraId, useCaseId)){
                configuration.setStreamUseCase(useCaseId);
            }
        } catch (IllegalArgumentException | NoSuchFieldError e) {
            Log.v(TAG, EXCEPTION_LOG," applyCroppedRaw didn`t find SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW");
        }
    }

    private void applyVSR(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_VSR);
        if (value != null ) {
            int mode = Integer.parseInt(value);
            Log.v(TAG, " applyVSR mode :" + mode);
            VendorTagUtil.setVSRMode(request, mode);
        }
    }

    private void applyVIULL(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_VIULL);
        if (value != null ) {
            int mode = -1;
            try  {
                mode = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                //
            }
            Log.v(TAG, " applyVIULL mode :" + mode);
            if (mode != -1) {
                VendorTagUtil.setVIULLMode(request, mode);
            }
        }
    }
    private void applyLowLightBoost(CaptureRequest.Builder request){
        String value = mSettingsManager.getValue(SettingsManager.KEY_LOWLIGHT_BOOST);
        if(value != null && value.equals("1")){
            request.set(CaptureRequest.CONTROL_AE_MODE,
                                     CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY);
        }

    }
    private void updateRGBGraghViewVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGraphViewRGB != null) {
                    mGraphViewRGB.setVisibility(visibility);
                }
            }
        });
    }

    private void applyEnableCinematic(CaptureRequest.Builder request) {
        try {
            int value = 1;
            Log.v(TAG, " applyEnableCinematic value :" + value);
            request.set(cinematic_mode_enable, value);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, EXCEPTION_LOG," there is no vendorTag cinematic_mode_enable");
        }
    }

    private void updateGraghViewVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGraphViewR != null) {
                    mGraphViewR.setVisibility(visibility);
                }
                if(mGraphViewGB != null) {
                    mGraphViewGB.setVisibility(visibility);
                }
                if(mGraphViewB != null) {
                    mGraphViewB.setVisibility(visibility);
                }
            }
        });
    }

    private void updateMFNRText() {
        if (PersistUtil.showMFNRswitch() && showMFNR()) mUI.initMfnrSeekBar();
        if (showMFNR()) {
            if (isMFNREnabled() || PersistUtil.showMFNRswitch()) mMFNREnable = true;
            else if (!PersistUtil.showMFNRswitch() && !isMFNREnabled()) mMFNREnable = false;
            if (mMFNRDrawer != null && mMFNREnable) {
                mMFNRDrawer.setVisibility(View.VISIBLE);
                mMFNRDrawer.refleshMFNR();
                if (PersistUtil.showMFNRswitch()) {
                    mUI.showMFNRtext();
                }
            } else if (!mMFNREnable) {
                mMFNREnable = false;
                if (mMFNRDrawer != null) {
                    mMFNRDrawer.setVisibility(View.INVISIBLE);
                    if (PersistUtil.showMFNRswitch()) mUI.hidenMFNRtext();
                }
            }
        } else {
            mMFNREnable = false;
            if (mMFNRDrawer != null) {
                mMFNRDrawer.setVisibility(View.INVISIBLE);
                if (PersistUtil.showMFNRswitch()) mUI.hidenMFNRtext();
            }
        }
    }

    public boolean showMFNR(){
        if ((mCurrentSceneMode.mode == CameraMode.DEFAULT || mCurrentSceneMode.mode == CameraMode.RTB)
                && !mPostProcessor.isSelfieMirrorOn() && !mSettingsManager.isZSLInAppEnabled())
            return true;
        else
            return false;
    }

    private void updateRGBGraghView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGraphViewRGB != null) {
                    mGraphViewRGB.PreviewChanged();
                }
            }
        });
    }

    private void updateGraghView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(mGraphViewR != null) {
                    mGraphViewR.PreviewChanged();
                }
                if(mGraphViewGB != null) {
                    mGraphViewGB.PreviewChanged();
                }
                if(mGraphViewB != null) {
                    mGraphViewB.PreviewChanged();
                }
            }
        });
    }

    // BG stats
    private void updateBGStatsVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(bgstats_view != null) {
                    bgstats_view.setVisibility(visibility);
                    mBgStatsLabel.setVisibility(visibility);
                }
            }
        });
    }

    private void updateBGStatsView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(bgstats_view != null) {
                    bgstats_view.PreviewChanged();
                }
            }
        });
    }

    //BE stats
    private void updateBEStatsVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(bestats_view != null) {
                    bestats_view.setVisibility(visibility);
                    mBeStatsLabel.setVisibility(visibility);
                }
            }
        });
    }

    private void updateBEStatsView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(bestats_view != null) {
                    bestats_view.PreviewChanged();
                }
            }
        });
    }

    //RS stats
    private void updateRSStatsVisibility(final int visibility) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(rsstats_view != null) {
                    rsstats_view.setVisibility(visibility);
                    mRsStatsLabel.setVisibility(visibility);
                }
            }
        });
    }

    private void updateRSStatsView(){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if(rsstats_view != null) {
                    rsstats_view.PreviewChanged();
                }
            }
        });
    }

    private boolean applyPreferenceToPreview(int cameraId, String key, String value) {
        if (!checkSessionAndBuilder(mCaptureSession[cameraId], mPreviewRequestBuilder[cameraId])) {
            return false;
        }
        boolean updatePreview = false;
        switch (key) {
            case SettingsManager.KEY_WHITE_BALANCE:
                updatePreview = true;
                applyWhiteBalance(mPreviewRequestBuilder[cameraId]);
                break;
            case SettingsManager.KEY_COLOR_EFFECT:
                updatePreview = true;
                applyColorEffect(mPreviewRequestBuilder[cameraId]);
                break;
            case SettingsManager.KEY_SCENE_MODE:
                updatePreview = true;
                applySceneMode(mPreviewRequestBuilder[cameraId]);
                applySnapshotHDR(mPreviewRequestBuilder[cameraId]);
                break;
            case SettingsManager.KEY_EXPOSURE:
                updatePreview = true;
                applyExposure(mPreviewRequestBuilder[cameraId]);
                break;
            case SettingsManager.KEY_ISO:
            case SettingsManager.KEY_MANUAL_EXPOSURE_VALUE:
                updatePreview = true;
                applyFlashForUIChange(mPreviewRequestBuilder[cameraId],getMainCameraId());
                applyIsoAndExposureTime(mPreviewRequestBuilder[cameraId]);
                applyExposure(mPreviewRequestBuilder[cameraId]);
                break;

            case SettingsManager.KEY_FACE_DETECTION:
                updatePreview = true;
                applyFaceDetection(mPreviewRequestBuilder[cameraId]);
                break;
            case SettingsManager.KEY_FOCUS_DISTANCE:
                updatePreview = true;
                if (mUI.getCurrentProMode() == ProMode.MANUAL_MODE) {
                    if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE) {
                        mLockAFAE = LOCK_AF_AE_STATE_NONE;
                        applyIsAfLock(false);
                        cancelTouchFocus(mCurrentSceneMode.getCurrentId());
                        applySettingsForUnlockExposure(mPreviewRequestBuilder[mCurrentSceneMode.getCurrentId()], mCurrentSceneMode.getCurrentId());
                        updateLockAFAEVisibility();
                        mUI.initFlashButton();
                    }
                    applyFocusDistance(mPreviewRequestBuilder[cameraId],
                            String.valueOf(mSettingsManager.getCalculatedFocusDistance()));
                } else {
                    //set AF mode when manual mode is off
                    mPreviewRequestBuilder[cameraId].set(
                            CaptureRequest.CONTROL_AF_MODE, mControlAFMode);
                }
        }
        return updatePreview;
    }

    private void applyTargetZoom(CaptureRequest.Builder builder, float targetZoom) {
        Log.d(TAG, "applyTargetZoom, " + targetZoom);
        VendorTagUtil.setTargetZoom(builder, targetZoom);
    }

    private void applyZoomAndUpdate(int id, boolean instant) {
        applyZoomAndUpdate(id, instant, 0f);
    }

    private void applyZoomAndUpdate(int id, boolean instant, float targetZoom) {
        CaptureRequest.Builder captureRequest = mPreviewRequestBuilder[id];
        if (!checkSessionAndBuilder(mCaptureSession[id], captureRequest) || mCurrentSessionClosed
                ||mPaused) {
            return;
        }
        Log.d(TAG,"applyZoomAndUpdate, mRecordingPausing:" + mRecordingPausing+",mState[id]="+mState[id]);
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        boolean isUseVideoPreview = true;
        if (mCurrentSceneMode.mode == CameraMode.HFR ) {
            if(selectMode != null && selectMode.equals("default") && isHighSpeedRateCapture()){
                isUseVideoPreview = false;
            }
        }
        if (mCurrentSceneMode.mode == CameraMode.VIDEO ) {
            isUseVideoPreview = false;
        }
        if (mRecordingPausing && isUseVideoPreview) {
            captureRequest = mVideoPreviewRequestBuilder;
            String value = mSettingsManager.getValue(SettingsManager.KEY_EIS_VALUE);
            boolean noNeedEndofStreamWhenPause = value != null && value.equals("V3");
            // app use preview + video buffers when select EIS V3 usecase
            if (noNeedEndofStreamWhenPause) {
                captureRequest = mVideoRecordRequestBuilder;
            }
            if (mUI.getZoomFixedSupport()) {
                applyZoomRatio(captureRequest, mZoomValue, id);
            } else {
                applyZoom(captureRequest, id);
            }
        }

        if (mState[id] == STATE_WAITING_TOUCH_FOCUS) {
            cancelTouchFocus(id);
        }
        if (mUI.getZoomFixedSupport()) {
            applyZoomRatio(captureRequest, mZoomValue, id);
        } else {
            applyZoom(captureRequest, id);
        }

        applyTargetZoom(captureRequest, targetZoom);

        try {
            if(id == MONO_ID && !canStartMonoPreview()) {
                mCaptureSession[id].capture(captureRequest
                        .build(), mCaptureCallback, mCameraHandler);
            } else {
                CameraCaptureSession session = mCaptureSession[id];
                if (instant){
                    session.stopRepeating();
                }

                synchronized (mPerformanceDebugData) {
                    mZoomTimeMap.put(System.currentTimeMillis(), mZoomValue);
                }
                if (session instanceof CameraConstrainedHighSpeedCaptureSession) {
                    List list = getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) session
                            ,captureRequest);
                    if(!instant) {
                        ((CameraConstrainedHighSpeedCaptureSession) session).setRepeatingBurst(list
                                , mCaptureCallback, mCameraHandler);
                    } else {
                        ((CameraConstrainedHighSpeedCaptureSession) session).captureBurst(list
                                , mCaptureCallback, mCameraHandler);
                    }
                } else if (isSSMEnabled()) {
                    session.setRepeatingBurst(createSSMBatchRequest(captureRequest),
                            mCaptureCallback, mCameraHandler);
                } else {
                    int previewFPS = mSettingsManager.getVideoPreviewFPS();
                    if ((previewFPS != 60 && mHighSpeedCaptureRate == 60) || (mHighSpeedCaptureRate == 0 && previewFPS == 15)) {
                        if (mUI.getZoomFixedSupport()) {
                            applyZoomRatio(mVideoRecordRequestBuilder, mZoomValue, id);
                        } else {
                            applyZoom(mVideoRecordRequestBuilder, id);
                        }
                        if (PersistUtil.enableMediaRecorder() && mIsPreviewingVideo) {
                            mVideoRecordRequestBuilder.addTarget(mVideoRecordingSurface);
                        }
                        limitPreviewFPS();
                        if (PersistUtil.enableMediaRecorder() && mIsPreviewingVideo) {
                            mVideoRecordRequestBuilder.removeTarget(mVideoRecordingSurface);
                        }
                    }
                    Integer aeState = mPreviewCaptureResult.get(CaptureResult.CONTROL_AE_STATE);
                    if(aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE){
                        captureRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
                        mSetAePrecaptureTriggerIdel ++;
                    }
                    if (instant) {
                        session.capture(captureRequest
                                .build(), mCaptureCallback, mCameraHandler);
                    } else {
                        session.setRepeatingRequest(captureRequest
                                .build(), mCaptureCallback, mCameraHandler);
                    }
                    if(mSetAePrecaptureTriggerIdel >0) {
                        captureRequest.set(
                                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
                        session.setRepeatingRequest(captureRequest
                                .build(), mCaptureCallback, mCameraHandler);
                        mSetAePrecaptureTriggerIdel = 0;
                    }
                }
            }
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG,e.toString());
        }
    }

    private void applyJpegQuality(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_JPEG_QUALITY);
        int jpegQuality = getQualityNumber(value);
        request.set(CaptureRequest.JPEG_QUALITY, (byte) jpegQuality);
    }

    private void applyAFRegions(CaptureRequest.Builder request, int id) {
        if (mControlAFMode == CaptureRequest.CONTROL_AF_MODE_AUTO) {
            request.set(CaptureRequest.CONTROL_AF_REGIONS, mAFRegions[id]);
        } else {
            request.set(CaptureRequest.CONTROL_AF_REGIONS, ZERO_WEIGHT_3A_REGION);
        }
    }

    private void applyAERegions(CaptureRequest.Builder request, int id) {
        if (mControlAFMode == CaptureRequest.CONTROL_AF_MODE_AUTO) {
            request.set(CaptureRequest.CONTROL_AE_REGIONS, mAERegions[id]);
        } else {
            request.set(CaptureRequest.CONTROL_AE_REGIONS, ZERO_WEIGHT_3A_REGION);
        }
    }

    private void applySceneMode(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        String autoHdr = mSettingsManager.getValue(SettingsManager.KEY_AUTO_HDR);
        if (value == null) return;
        int mode = Integer.parseInt(value);
        if (autoHdr != null && "enable".equals(autoHdr) && "0".equals(value)) {
                request.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);
                request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
        }
        if(getPostProcFilterId(mode) != PostProcessor.FILTER_NONE) {
            request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
            request.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
            return;
        }
        if (mode != CaptureRequest.CONTROL_SCENE_MODE_DISABLED
                && mode != SettingsManager.SCENE_MODE_DUAL_INT
                && mode != SettingsManager.SCENE_MODE_PROMODE_INT && !mCaptureHDRTestEnable) {
            request.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
            request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
        } else {
            request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        }

        if (mSettingsManager.getPhysicalCameraId() != null){
            Set<String> ids = mSettingsManager.getPhysicalFeatureEnableId(
                    SettingsManager.KEY_PHYSICAL_HDR);
            if (ids != null){
                for (String id : ids) {
                    request.setPhysicalCameraKey(CaptureRequest.CONTROL_SCENE_MODE,
                            CaptureRequest.CONTROL_SCENE_MODE_HDR,id);
                    request.setPhysicalCameraKey(CaptureRequest.CONTROL_MODE,
                            CaptureRequest.CONTROL_MODE_USE_SCENE_MODE,id);
                }
            }
        }
    }

    public void updateAIStrengthValue(int value){
        mAIStrengthValue = value;
        applyAICameraStrengthAndUpdate();
    }

    private void applyAICameraStrengthAndUpdate(){
        if (mCurrentSessionClosed || mPreviewRequestBuilder[getMainCameraId()] == null || mCaptureSession[getMainCameraId()] == null || mSettingsManager.isAICameraDisable() ||
                (CaptureModule.CURRENT_MODE != CaptureModule.CameraMode.VIDEO && CaptureModule.CURRENT_MODE != CaptureModule.CameraMode.DEFAULT)) return;
        Log.d(TAG, "applyAICameraStrengthAndUpdate: " + mAIStrengthValue);
        try {
            applyAICameraStrength(mPreviewRequestBuilder[getMainCameraId()]);
            mCaptureSession[getMainCameraId()].setRepeatingRequest(mPreviewRequestBuilder[getMainCameraId()].build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException| IllegalArgumentException | UnsupportedOperationException | IllegalStateException e) {
            Log.e(TAG, "Camera Access Exception in applyAICameraStrengthAndUpdate, apply failed e="+e);
        }
    }
    private void applyAICameraStrength(CaptureRequest.Builder builder){
        if(!mSettingsManager.isAICameraDisable()) {
            Log.d(TAG, "applyAICameraStrength: " + mAIStrengthValue);
            builder.set(CaptureModule.AICameraStrength, mAIStrengthValue);
        }
    }

    private void applyAIBlurConfigs(CaptureRequest.Builder builder){
        if (!mIsRecordingVideo && !mIsPreviewingVideo || builder == null) return;
        String mode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        boolean isBokehMode = mode != null && (mode.equals("rtb") || mode.equals("single_rear_aibokeh"));
        if(isBokehMode) {
            applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_SHAPE, builder);
            applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_STRENGTH, builder);
            applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_DISTANCE, builder);
            applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_LUMA, builder);
            String value =  mSettingsManager.getValue(SettingsManager.KEY_AI_BLUR_LUMA);
            if (value != null && value.equals("2")) {
                applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_CHROMAU, builder);
                applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_CHROMAV, builder);
            }
            if(value != null && !value.equals("0")){
                applyAIBlurConfig(SettingsManager.KEY_AI_BLUR_CHROMASTRENGTH, builder);
            }
        }
    }

    private void applyAIBlurConfig(String key, CaptureRequest.Builder builder){
        String mode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        boolean isBokehMode = mode != null && (mode.equals("rtb") || mode.equals("single_rear_aibokeh"));
        if(isBokehMode) {
            try {
                if (key.equals(SettingsManager.KEY_AI_BLUR_SHAPE)) {
                    String value = mSettingsManager.getValue(SettingsManager.KEY_AI_BLUR_SHAPE);
                    Log.d(TAG, "applyAIblurShape: " + value);
                    if (value == null) return;
                    int intValue = Integer.parseInt(value);
                    builder.set(CaptureModule.blurShape, intValue);
                }else if (key.equals(SettingsManager.KEY_AI_BLUR_LUMA)) {
                    String value =  mSettingsManager.getValue(SettingsManager.KEY_AI_BLUR_LUMA);
                    Log.d(TAG, "applyAIblurEffect: " + value);
                    if (value == null) return;
                    int intValue = Integer.parseInt(value);
                    builder.set(CaptureModule.blurEffect, intValue);
                }else{
                    float value = Float.valueOf(mSettingsManager.geBlurSliderValue(key));
                    if(key.equals(SettingsManager.KEY_AI_BLUR_CHROMAU) || key.equals(SettingsManager.KEY_AI_BLUR_CHROMAV)){
                        value = value-0.5f;
                    }
                    Log.d(TAG, "applyAIBlurConfig: " + value + ",key:" + key);
                    if(key.equals(SettingsManager.KEY_AI_BLUR_STRENGTH)){
                        builder.set(CaptureModule.blurStrength, value);
                    }else if(key.equals(SettingsManager.KEY_AI_BLUR_DISTANCE)){
                        builder.set(CaptureModule.blurFocusDistance, value);
                    }else if(key.equals(SettingsManager.KEY_AI_BLUR_CHROMAU)){
                        builder.set(CaptureModule.blurChromaSuppressionU, value);
                    }else if(key.equals(SettingsManager.KEY_AI_BLUR_CHROMAV)) {
                        builder.set(CaptureModule.blurChromaSuppressionV, value);
                    }else if(key.equals(SettingsManager.KEY_AI_BLUR_CHROMASTRENGTH)) {
                        builder.set(CaptureModule.blurChromaSuppressionStrength, value);
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
    }

    private void applyExposure(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE);

        if (value == null) return;
        int intValue = Integer.parseInt(value);
        Log.d(TAG,"applyev value="+value+",intvalue="+intValue);
        request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, intValue);
    }

    private void applyIsoAndExposureTime(CaptureRequest.Builder request) {
        if (applyManualIsoExposure(request)) return;
        String isovalue = mSettingsManager.getValue(SettingsManager.KEY_ISO);
        String exposuretime = mSettingsManager.getKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE);
        if (isovalue == null || exposuretime == null) return;
        if (exposuretime.equals("") ) exposuretime = "auto";
        boolean promode = mCurrentSceneMode.mode == CameraMode.PRO_MODE;
        long previewExpTime = maxExpTime;
        if (!exposuretime.equals("auto") && promode) {
                mLongExpTime =  CameraUtil.strToLong(exposuretime,maxExpTime);
                if(mLongExpTime <= maxExpTime) {
                    previewExpTime = mLongExpTime;
                }else if(mLongExpTime > maxExpTime){
                    if(!isLongExpTmCaptrure()) previewExpTime = maxExpTime;
                    else if (isTakingPicture()) previewExpTime = mLongExpTime;
                }
        }
        Log.d(TAG,"applyIsoAndExposureTime-iso="+isovalue+",exposuretime="+exposuretime+",isLongExpTmCaptrure()="+isLongExpTmCaptrure()
        +",previewExpTime="+previewExpTime+",mLongExpTime="+mLongExpTime+",promode="+promode);
        if (promode && exposuretime.equals("auto") && !isovalue.equals("auto")) {
            setIsoValue(request, isovalue);
        } else if (promode && !exposuretime.equals("auto") && isovalue.equals("auto")) {
           setExposureTime(request,String.valueOf(previewExpTime));
            if(!mSettingsManager.isFlashSupported(getMainCameraId())){
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            }
        } else if (promode && !exposuretime.equals("auto") && !isovalue.equals("auto")) {
            setIsoAndExposureTime(request, isovalue, String.valueOf(previewExpTime));
        }else {
            request.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CONTROL_AE_PRIORITY_MODE_OFF);
        }
    }

    private boolean setExposureTime(CaptureRequest.Builder request, String exposuretime) {
        long[] expTimeRange = mSettingsManager.getExposureRangeValues(getMainCameraId());
        long value = CameraUtil.strToLong(exposuretime,expTimeRange[0]);
        request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, value);
        request.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY);
        if (!mSettingsManager.isFlashSupported(getMainCameraId())) {
            request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        }
        return true;
    }

    private void setIsoValue(CaptureRequest.Builder request, String isoValue) {
        int value = CameraUtil.strToInt(isoValue,100);
        request.set(CaptureRequest.SENSOR_SENSITIVITY, value);
        request.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY);
        if(!mSettingsManager.isFlashSupported(getMainCameraId())) {
            request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        }
    }

    private void setIsoAndExposureTime(CaptureRequest.Builder request, String isoValue, String exposureTime) {
        int iso = CameraUtil.strToInt(isoValue,100);
        long[] expTimeRange = mSettingsManager.getExposureRangeValues(getMainCameraId());
        long exptime = CameraUtil.strToLong(exposureTime,expTimeRange[0]);
        request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        if(!mSettingsManager.isOpenManualFlash()) {
            request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
        request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exptime);
        request.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
    }
    private boolean applyManualIsoExposure(CaptureRequest.Builder request) {
        boolean result = false;
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        mSettingsManager.getCurrentPrepNameKey()), Context.MODE_PRIVATE);
        String isoPriority = mActivity.getString(
                R.string.pref_camera_manual_exp_value_ISO_priority);
        String expTimePriority = mActivity.getString(
                R.string.pref_camera_manual_exp_value_exptime_priority);
        String userSetting = mActivity.getString(
                R.string.pref_camera_manual_exp_value_user_setting);
        String gainsPriority = mActivity.getString(
                R.string.pref_camera_manual_exp_value_gains_priority);
        String manualExposureMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_EXPOSURE);
        isManualAEC = false;
        if (manualExposureMode == null) return result;
        if (manualExposureMode.equals(isoPriority)) {
            String isoValue =pref.getString(SettingsManager.KEY_MANUAL_ISO_VALUE,"100");
            setIsoValue(request, isoValue);
            result = true;
        } else if (manualExposureMode.equals(expTimePriority)) {
            String expTime = pref.getString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, "auto");
            result = setExposureTime(request, expTime);
        } else if (manualExposureMode.equals(userSetting)) {
            String isoValue =pref.getString(SettingsManager.KEY_MANUAL_ISO_VALUE,"100");
            String expTime = pref.getString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, "auto");
            setIsoAndExposureTime(request, isoValue, expTime);
            isManualAEC = true;
            result = true;
        } else if (manualExposureMode.equals(gainsPriority)) {
            float gains = pref.getFloat(SettingsManager.KEY_MANUAL_GAINS_VALUE, 0f);
            if(gains <= 0){
                result = false;
                return result;
            }
            int[] isoRange = mSettingsManager.getIsoRangeValues(getMainCameraId());
            VendorTagUtil.setIsoExpPrioritySelectPriority(request, 0);
            int isoValue = 100;
            if (isoRange != null) {
                isoValue = (int) (gains * isoRange[0]);
            }
            long intValue = SettingsManager.KEY_ISO_INDEX.get(
                    SettingsManager.MAUNAL_ABSOLUTE_ISO_VALUE);
            VendorTagUtil.setIsoExpPriority(request, intValue);
            VendorTagUtil.setUseIsoValues(request, isoValue);
            Log.v(TAG,  "manual Gain value :" + isoValue);
            if (request.get(CaptureRequest.SENSOR_EXPOSURE_TIME) != null) {
                mIsoExposureTime = request.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
            }
            if (request.get(CaptureRequest.SENSOR_SENSITIVITY) != null) {
                mIsoSensitivity = request.get(CaptureRequest.SENSOR_SENSITIVITY);
            }
            request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, null);
            request.set(CaptureRequest.SENSOR_SENSITIVITY, null);
            result = true;
        }else if ("off".equals(manualExposureMode)){
            request.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CONTROL_AE_PRIORITY_MODE_OFF);
        }
        return result;
    }

    private boolean applyAWBCCTAndAgain(CaptureRequest.Builder request) {
        boolean result = false;
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        String.valueOf(CURRENT_ID)), Context.MODE_PRIVATE);
        float awbDefault = -1f;
        float rGain = pref.getFloat(SettingsManager.KEY_AWB_RAGIN_VALUE, awbDefault);
        float gGain = pref.getFloat(SettingsManager.KEY_AWB_GAGIN_VALUE, awbDefault);
        float bGain = pref.getFloat(SettingsManager.KEY_AWB_BAGIN_VALUE, awbDefault);
        float cct = pref.getFloat(SettingsManager.KEY_AWB_CCT_VALUE, awbDefault);
        float tc0 = pref.getFloat(SettingsManager.KEY_AWB_DECISION_AFTER_TC_0, awbDefault);
        float tc1 = pref.getFloat(SettingsManager.KEY_AWB_DECISION_AFTER_TC_1, awbDefault);
        float aec0 = pref.getFloat(SettingsManager.KEY_AEC_SENSITIVITY_0, awbDefault);
        float aec1 = pref.getFloat(SettingsManager.KEY_AEC_SENSITIVITY_1, awbDefault);
        float aec2 = pref.getFloat(SettingsManager.KEY_AEC_SENSITIVITY_2, awbDefault);
        float luxIndex = pref.getFloat(SettingsManager.KEY_AEC_LUX_INDEX, awbDefault);
        float adrcGain = pref.getFloat(SettingsManager.KEY_AEC_ADRC_GAIN, awbDefault);
        float darkBoostGain = pref.getFloat(SettingsManager.KEY_AEC_DARK_BOOST_GAIN, awbDefault);
        int aecCameraId = pref.getInt(SettingsManager.KEY_WARM_START_AEC_CAMERA_ID, -1);
        int antBandingMode = pref.getInt(SettingsManager.KEY_ANT_BANDING_MODE, -1);
        int isFickerDetected = pref.getInt(SettingsManager.KEY_IS_FICKER_DETECTED, -1);
        if (rGain != awbDefault && gGain != awbDefault && gGain != bGain) {
            Float[] awbGains = {rGain, gGain, bGain};
            Float[] tcs = {tc0, tc1};
            try {
                request.set(CaptureModule.awbWarmStart_gain, awbGains);
                if (cct != awbDefault) {
                    request.set(CaptureModule.awbWarmStart_cct, cct);
                }
                request.set(CaptureModule.awbWarmStart_decision_after_tc, tcs);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
        if (aec0 != awbDefault && aec1 != awbDefault && aec2 != awbDefault) {
            Float[] aecGains = {aec0, aec1, aec2};
            try {
                request.set(CaptureModule.aec_start_up_sensitivity, aecGains);
                result = true;
            } catch (IllegalArgumentException e) {
               Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        } else {
            Log.v(TAG, " applyAWBCCTAndAgain aec0 :" + aec0 + " " + aec1 + " " + aec2);
        }
        if (luxIndex != awbDefault) {
            try {
                request.set(CaptureModule.aec_start_up_luxindex_request, luxIndex);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, EXCEPTION_LOG," applyAWBCCTAndAgain there is no vendorTag :" +
                        aec_start_up_luxindex_request);
            }
        }
        if (adrcGain != awbDefault) {
            try {
                request.set(awbWarmStart_adrc_gain, adrcGain);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, EXCEPTION_LOG,"applyAWBCCTAndAgain there is no vendorTag :" + awbWarmStart_adrc_gain);
            }
        }
        if (darkBoostGain != awbDefault) {
            try {
                request.set(awbWarmStart_dark_boost_gain, darkBoostGain);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, EXCEPTION_LOG,"applyAWBCCTAndAgain there is no vendorTag :" +
                        awbWarmStart_dark_boost_gain);
            }
        }
        if (aecCameraId != -1) {
            try {
                request.set(request_aec_camera_id, aecCameraId);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "applyAWBCCTAndAgain AECCameraId vendor tag missing:" +
                        request_aec_camera_id);
            }
        }
        if (antBandingMode != -1) {
            try {
                request.set(anti_banding_mode_request, antBandingMode);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "applyAWBCCTAndAgain anti_banding_mode_request vendor tag missing:" +
                        anti_banding_mode_request);
            }
        }
        if (isFickerDetected != -1) {
            try {
                request.set(isficker_detected_request, isFickerDetected);
                result = true;
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "applyAWBCCTAndAgain isficker_detected_request vendor tag missing:" +
                        isficker_detected_request);
            }
        }
        return result;
    }

    private boolean updateAWBCCTAndgains(CaptureResult captureResult) {
        if (captureResult != null) {
            try {
                if (mExistAWBVendorTag) {
                    mRGain = captureResult.get(CaptureModule.awbFrame_control_rgain);
                    mGGain = captureResult.get(CaptureModule.awbFrame_control_ggain);
                    mBGain = captureResult.get(CaptureModule.awbFrame_control_bgain);
                    mCctAWB = captureResult.get(CaptureModule.awbFrame_control_cct);
                    mAWBDecisionAfterTC = captureResult.get(CaptureModule.awbFrame_decision_after_tc);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                mExistAWBVendorTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
            try {
                if (mExistAECWarmTag) {
                    mAECSensitivity = captureResult.get(CaptureModule.aec_sensitivity);
                    mAECLuxIndex = captureResult.get(aec_start_up_luxindex_result);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                mExistAECWarmTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }

            try {
                if (mExposureCountTag) {
                    mExposureCount = captureResult.get(CaptureModule.exposure_count);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                mExposureCountTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }

            try {
                if (mAECCameraIdTag) {
                    mAECCameraId = captureResult.get(stats_visualizer_camera_id);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                mAECCameraIdTag = false;
                e.printStackTrace();
            }

            try {
                if (mExistAECDarkGainTag) {
                    mDarkBoostGain = captureResult.get(aecFrame_dark_boost_gain);
                    mAdrcGain = captureResult.get(aecFrame_adrc_gain);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                mExistAECDarkGainTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
        return mExistAWBVendorTag && mExistAECWarmTag && mExistAECDarkGainTag;
    }

    private boolean updateAECGainAndExposure(CaptureResult captureResult) {
        boolean result = false;
        if (captureResult != null) {
            try {
                if (mExistAECFrameControlTag) {
                    mAecFramecontrolExosureTime = captureResult.get(aec_frame_control_exposure_time);
                    mAecFramecontrolLinearGain = captureResult.get(aec_frame_control_linear_gain);
                    mAecFramecontrolSensitivity = captureResult.get(aec_frame_control_sensitivity);
                    mAecFramecontrolLuxIndex = captureResult.get(aec_frame_control_lux_index);
                }
                result = true;
            } catch (IllegalArgumentException|NullPointerException e) {
                mExistAECFrameControlTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
        return result;
    }

    private boolean updateAntiBandingMode(CaptureResult captureResult) {
        if (captureResult != null) {
            try {
                if (mExistAntiBandingModeTag) {
                    mAntiBandingMode = captureResult.get(anti_banding_mode_result);
                }
            } catch (IllegalArgumentException|NullPointerException e) {
                mExistAntiBandingModeTag = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
        return mExistAntiBandingModeTag;
    }

    private boolean updateIsFickerDetected(CaptureResult captureResult) {
        if (captureResult != null) {
            try {
                if (mExistIsFickerDetected) {
                    mIsFickerDetected = captureResult.get(isficker_detected_result);
                }
            } catch (IllegalArgumentException|NullPointerException e) {
                mExistIsFickerDetected = false;
                Log.w(TAG,EXCEPTION_LOG,e.toString());
            }
        }
        return mExistIsFickerDetected;
    }

    public void writeXMLForWarmAwb() {
        final SharedPreferences pref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        String.valueOf(CURRENT_ID)), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(mExistAWBVendorTag) {
            editor.putFloat(SettingsManager.KEY_AWB_RAGIN_VALUE, mRGain);
            editor.putFloat(SettingsManager.KEY_AWB_GAGIN_VALUE, mGGain);
            editor.putFloat(SettingsManager.KEY_AWB_BAGIN_VALUE, mBGain);
            editor.putFloat(SettingsManager.KEY_AWB_CCT_VALUE, mCctAWB);
            editor.putFloat(SettingsManager.KEY_AWB_DECISION_AFTER_TC_0, mAWBDecisionAfterTC[0]);
            editor.putFloat(SettingsManager.KEY_AWB_DECISION_AFTER_TC_1, mAWBDecisionAfterTC[1]);
        }
        if(mExposureCountTag) {
            editor.putInt(SettingsManager.KEY_WARM_START_EXPOSURE_COUNT, mExposureCount);
        }
        if(mAECCameraIdTag) {
            editor.putInt(SettingsManager.KEY_WARM_START_AEC_CAMERA_ID, mAECCameraId);
        }
        if (mExistAECWarmTag) {
            if (mAECSensitivity.length == 3) {
                editor.putFloat(SettingsManager.KEY_AEC_SENSITIVITY_0, mAECSensitivity[0]);
                editor.putFloat(SettingsManager.KEY_AEC_SENSITIVITY_1, mAECSensitivity[1]);
                editor.putFloat(SettingsManager.KEY_AEC_SENSITIVITY_2, mAECSensitivity[2]);
            }
            if (mAECLuxIndex != -1.0f) {
                editor.putFloat(SettingsManager.KEY_AEC_LUX_INDEX, mAECLuxIndex);
            }
        }
        if (mExistAECDarkGainTag) {
            if (mAdrcGain != -1.0f) {
                editor.putFloat(SettingsManager.KEY_AEC_ADRC_GAIN, mAdrcGain);
            }
            if (mDarkBoostGain != -1.0f) {
                editor.putFloat(SettingsManager.KEY_AEC_DARK_BOOST_GAIN, mDarkBoostGain);
            }
        }
        if (mExistAntiBandingModeTag) {
            if (mAntiBandingMode != -1) {
                editor.putInt(SettingsManager.KEY_ANT_BANDING_MODE, mAntiBandingMode);
            }
        }
        if (mExistIsFickerDetected) {
            if (mIsFickerDetected != -1) {
                editor.putInt(SettingsManager.KEY_IS_FICKER_DETECTED, mIsFickerDetected);
            }
        }
        editor.apply();
    }

    private void applyColorEffect(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_COLOR_EFFECT);
        if (value == null) return;
        int mode = Integer.parseInt(value);
        request.set(CaptureRequest.CONTROL_EFFECT_MODE, mode);
    }

    private void applyWhiteBalance(CaptureRequest.Builder request) {
        String manualWBMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_WB);
        if(manualWBMode == null ||  manualWBMode.equals("off")) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_WHITE_BALANCE);
            if (value == null) return;
            int mode = Integer.parseInt(value);
            request.set(CaptureRequest.CONTROL_AWB_MODE, mode);
        }
    }

    private void applySnapshotFlash(CaptureRequest.Builder request, String value) {
        Log.i(TAG,  "applySnapshotFlash: " + value);
        String redeye = mSettingsManager.getValue(SettingsManager.KEY_REDEYE_REDUCTION);
        mIsAutoFlash = false;
        if (redeye != null && redeye.equals("on") && !mLongshotActive) {
            request.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
        } else if (value != null) {
            setFlashMode(request, value);
        }
    }

    private boolean isIsoAndE() {
        if (mCurrentSceneMode.mode != CameraMode.PRO_MODE) return false;
        String isovalue = mSettingsManager.getValue(SettingsManager.KEY_ISO);
        String exposuretime = mSettingsManager.getKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE);
        if (exposuretime.equals("") || isovalue.equals("") || exposuretime.equals("auto") || isovalue.equals("auto"))
            return false;
        else return true;


    }
    //response to switch flash mode options in UI, repeat request as soon as switching
    private void applyFlashForUIChange(CaptureRequest.Builder request, int id) {
        if (!checkSessionAndBuilder(mCaptureSession[id], request) || mCurrentSessionClosed) {
            return;
        }
        String redeye = mSettingsManager.getValue(SettingsManager.KEY_REDEYE_REDUCTION);
        if (redeye != null && redeye.equals("on") && !mLongshotActive) {
            Log.w(TAG, "redeye mode is on, can't set android.flash.mode");
            return;
        }
        if (!mSettingsManager.isFlashSupported(id)) {
            Log.w(TAG, "flash not supported, can't set android.flash.mode");
            return;
        }

        String value = mSettingsManager.getValue(mCurrentSceneMode.mode == CameraMode.PRO_MODE ?
                SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
        mIsAutoFlash = false;
        setFlashMode(request, value);
        try {
            mCaptureSession[id].setRepeatingRequest(request
                    .build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "Camera Access Exception in applyFlashForUIChange, apply failed");
        }
    }

    private void setFlashMode(CaptureRequest.Builder request, String flashMode) {
        if (request == null || flashMode == null || isIsoAndE()) return;
        boolean isCaptureBurst = isCaptureBrustMode();
        Log.d(TAG,"setflashmode flashmode="+flashMode+",captureburst="+isCaptureBurst);
        switch (flashMode) {
            case "on":
                if (isCaptureBurst) {
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                } else if(mCaptureTorchTrigger) {
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                }else{
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    setFlashLevel(request);
                }
                break;
            case "auto":
                mIsAutoFlash = true;
                if (isCaptureBurst) {
                    // When long shot is active, turn off the flash in auto mode
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                } else if(mCaptureTorchTrigger){
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                }else{
                    request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                }
                break;
            case "off":
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case "alwayson":
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                request.set(CaptureRequest.FLASH_MODE, mUI.getFilmstripLayout().getVisibility() != View.VISIBLE ?
                        CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
                setFlashLevel(request);
                break;
        }
        if(!(mSettingsManager.isOpenManualFlash() && "on".equals(flashMode)) && !"alwayson".equals(flashMode)) {
            request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
        applyLowLightBoost(request);
    }
    private void setFlashLevel(CaptureRequest.Builder request) {
        if (!mSettingsManager.isOpenManualFlash()) {
            return;
        }
        String level = mSettingsManager.getValue(mSettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL);
        try {
            request.set(CaptureRequest.FLASH_STRENGTH_LEVEL, CameraUtil.strToInt(level,1));
            if (!isManualAEC) {
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            } else {
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            }
            Log.i(TAG, "setFlashLevel level=" + level+",isManualAEC="+isManualAEC);
        } catch (NoSuchFieldError e) {
            Log.i(TAG, "e=" + e);
        }
    }
    private void applyTouchTrackFocus(CaptureRequest.Builder request) {
        boolean t2tSupported = false;
        String value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS);
        if(mCurrentSceneMode.mode == CameraMode.CINEMATIC){
            value = mSettingsManager.getValue(SettingsManager.KEY_TOUCH_TRACK_FOCUS_FOR_CINEMATIC);
        }
        if (value != null && value.equals("on")) {
            t2tSupported = true;
        } else {
            t2tSupported = false;
        }
        // set vendorTag according to mT2TSupported
        byte t2tValues =(byte)((t2tSupported) ? 0x01 : 0x00);
        try {
            request.set(CaptureModule.t2t_enable, t2tValues );
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"applyTouchTrackFocus hal no vendorTag : " + t2t_enable);
        }
    }

    private void applyFaceContourVersion(CaptureRequest.Builder request) {
        String facialContour = mSettingsManager.getValue(SettingsManager.KEY_FACIAL_CONTOUR);
        byte facialContour_version = 0;
        if ("1".equals(facialContour) || "5".equals(facialContour)) {
            facialContour_version = 1;
        } else if ("2".equals(facialContour) || "6".equals(facialContour)) {
            facialContour_version = 2;
        } else if ("3".equals(facialContour)  || "7".equals(facialContour)) {
            facialContour_version = 3;
        } else if ("4".equals(facialContour)  || "8".equals(facialContour)) {
            facialContour_version = 4;
        }
        try {
            Log.d(FD_TAG,FD_LOG,"face detection set facialContourVersion ="+facialContour_version);
            request.set(CaptureModule.facialContourVersion, facialContour_version);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + facialContour_version);
        }
    }

    private void applySpatialVideo(CaptureRequest.Builder request) {
        String value = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER);
        try {
            if (value != null && value.equals("mvhevc")) {
                request.set(spatialVideo, (byte) 1);
            } else {
                request.set(spatialVideo, (byte) 0);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"no vendorTag : " + spatialVideo);
        }
    }

    private void applyStatsVisualizerOptionMask(CaptureRequest.Builder request) {
        String stats_visualizer = mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        int optionMask = 0;
        if(stats_visualizer != null){
            String[] strArray = stats_visualizer.split(";");
            for (String value : strArray) {
                if(value != null && !value.equals("")) {
                    optionMask |= 1<< Integer.parseInt(value);
                }
            }
        }
        Log.d(TAG, "optionMask: " + optionMask);
        try {
            request.set(CaptureModule.statsVisualizerOptionMask, optionMask);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + statsVisualizerOptionMask);
        }
    }
    private void setFaceFeature(CaptureRequest.Builder request,String setkey,CaptureRequest.Key<Byte> requestkey){
        String keyvalue = mSettingsManager.getValue(setkey);
        try {
            if (keyvalue == null || keyvalue.equals("disable")) {
                request.set(requestkey, (byte) 0);
            } else if (keyvalue.equals("enable")) {
                request.set(requestkey, (byte) 1);
            } else if (keyvalue.equals("display")) {
                request.set(requestkey, (byte) 2);
            }
        }catch (IllegalArgumentException e) {
            Log.w(TAG, EXCEPTION_LOG,"hal no vendorTag : " + requestkey);
        }
    }

    private void applyFaceDetection(CaptureRequest.Builder request) {
        if (CURRENT_MODE == CameraMode.DEPTH) {
            return;
        }
        String value = mSettingsManager.getValue(SettingsManager.KEY_FACE_DETECTION);
        String mode = mSettingsManager.getValue(SettingsManager.KEY_FACE_DETECTION_MODE);
        String facialContour = mSettingsManager.getValue(SettingsManager.KEY_FACIAL_CONTOUR);
        String facialMask = mSettingsManager.getValue(SettingsManager.KEY_FACE_MASK);
        Log.d(FD_TAG,FD_LOG,"face detection mode="+mode+" facialContour="+facialContour);
        boolean bsgc = mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FD_SMILE)||
                mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FD_GAZE)||
                mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FD_BLINK);
        if (value != null) {
            try {
                boolean FdEnable = value.equals("on");
                int modeValue = CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE;
                if (FdEnable){
                    if (mode != null)
                        modeValue = Integer.valueOf(mode);
                } else {
                    modeValue = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
                }

                request.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                        modeValue);

                if (bsgc) {
                    final byte bsgc_enable;
                    if (FdEnable) {
                        bsgc_enable = 1;
                        request.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);
                    } else {
                        bsgc_enable = 0;
                    }
                    Log.d(FD_TAG,FD_LOG,"face detection set gazeEnable and blinkEnable ="+bsgc_enable);
                    request.set(CaptureModule.gazeEnable, bsgc_enable);
                    request.set(CaptureModule.blinkEnable, bsgc_enable);
                }
                byte maskEnable = (byte)(mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FACE_MASK) ? 1 : 0);
                Log.d(FD_TAG,FD_LOG,"face detection maskEnable is ="+maskEnable);
                try {
                    request.set(CaptureModule.faceMaskEnable, maskEnable);
                } catch (IllegalArgumentException e) {
                }

                byte upperBodyEnabled = (byte)(mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_UPPER_BODY_DETECTION) ? 1 : 0);
                Log.d(FD_TAG,FD_LOG,"face detection upperBodyEnabled is ="+upperBodyEnabled);
                try {
                    request.set(CaptureModule.upperBodyEnable, upperBodyEnabled);
                } catch (IllegalArgumentException e) {
                }

                if (mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FD_GENDER)) {
                    Log.d(FD_TAG,FD_LOG,"face detection set GENDER_ENABLE");
                    request.set(CaptureModule.GENDER_ENABLE, (byte)1);
                }

                if (mSettingsManager.isFdFeatureEnable(SettingsManager.KEY_FD_FACE_EXPRESSION)) {
                    Log.d(FD_TAG,FD_LOG,"face detection set FACE_EXPRESSION_ENABLE");
                    request.set(CaptureModule.FACE_EXPRESSION_ENABLE, (byte)1);
                }
                setFaceFeature(request,SettingsManager.KEY_FACIAL_CONTOUR_VISIBILITY,CaptureModule.facialContourVisib);
                setFaceFeature(request,SettingsManager.KEY_PET_DETECTION,CaptureModule.petEnable);
                setFaceFeature(request,SettingsManager.KEY_FD_SKIN_TONE,CaptureModule.skinToneEnable);
                if (facialContour != null) {
                    final byte facialContour_enable;
                    int contour = -1;
                    if (!facialContour.equals("disable")){
                        contour = Integer.valueOf(facialContour);
                    }
                    if (FdEnable) {
                        if (contour >= 0) {
                            facialContour_enable = 1;
                            Log.d(FD_TAG,FD_LOG,"face detection set facialContourEnable");
                            request.set(CaptureModule.facialContourEnable, facialContour_enable);
                        } else {
                            facialContour_enable = 0;
                            request.set(CaptureModule.facialContourEnable, facialContour_enable);
                        }
                    }

                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void applyFlash(CaptureRequest.Builder request, int id) {
        if (mSettingsManager.isFlashSupported(id)) {
            String value = mSettingsManager.getValue(mCurrentSceneMode.mode == CameraMode.PRO_MODE ?
                    SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
            applySnapshotFlash(request, value);
        } else {
            request.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
    }

    private void addPreviewSurface(CaptureRequest.Builder builder, List<Surface> surfaceList, int id) {
        if (mSettingsManager.getPhysicalCameraId() != null) {
            List<Surface> previews = mUI.getPhysicalSurfaces();
            if(mSettingsManager.isLogicalEnable()){
                builder.addTarget(previews.get(0));
                if (surfaceList != null){
                    surfaceList.add(previews.get(0));
                }
            }
            for (int i =1;i <=mSettingsManager.getPhysicalCameraId().size();i++){
                builder.addTarget(previews.get(i));
            }
        } else if (isBackCamera() && getCameraMode() == DUAL_MODE && id == MONO_ID) {
            if(surfaceList != null) {
                surfaceList.add(mUI.getMonoDummySurface());
            }
            builder.addTarget(mUI.getMonoDummySurface());
            return;
        } else {
            List<Surface> surfaces = mFrameProcessor.getInputSurfaces();
            for(Surface surface : surfaces) {
                if(surfaceList != null) {
                    surfaceList.add(surface);
                }
                builder.addTarget(surface);
            }
            return;
        }
    }

    private void checkAndPlayRecordSound(int id, boolean isStarted) {
        if (!mIsRecordingVideo) {
            return;
        }
        if (id == getMainCameraId()) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_SHUTTER_SOUND);
            if (value != null && value.equals("on") && mSoundPlayer != null && !PersistUtil.isPerfTestRunning()) {
                mSoundPlayer.play(isStarted? SoundClips.START_VIDEO_RECORDING
                        : SoundClips.STOP_VIDEO_RECORDING);

            }
        }
    }

    public void checkAndPlayShutterSound(int id) {
        if (id == getMainCameraId()) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_SHUTTER_SOUND);
            if (value != null && value.equals("on") && mSoundPlayer != null) {
                mSoundPlayer.play(SoundClips.SHUTTER_CLICK);
            }
        }
    }
    public Surface getPreviewSurfaceForSession(int id) {
        if (isBackCamera()) {
            if (getCameraMode() == DUAL_MODE && id == MONO_ID) {
                return mUI.getMonoDummySurface();
            } else {
                return mUI.getPreviewSurface();
            }
        } else {
            return mUI.getPreviewSurface();
        }
    }

    @Override
    public void onQueueStatus(final boolean full) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!full && (!getCameraModeSwitcherAllowed() || mCurrentSessionClosed || mPreviewCaptureResult == null)){
                    return;
                }
                mUI.enableShutter(!full);
            }
        });
    }

    public void triggerTouchFocus(int x, int y, int t2tTrigger) {
        int id = getMainCameraId();
        int[] registerRect = new int[4];
        int[] newXY = {x, y};
        if (mUI.isOverControlRegion(newXY)) return;
        if (!mUI.isOverSurfaceView(newXY)) return;
        x = newXY[0];
        y = newXY[1];
        Log.d(TAG,  "triggerTouchFocus, after trim: x:" + x + " y:" + y
                    + ", t2tTrigger :" + t2tTrigger);
        transformTouchCoords(x, y, id);
        try {
            if (mPreviewRequestBuilder[id] != null) {
                setTag(mPreviewRequestBuilder[id], "" + id + "-" + getCurrenCameraMode().name());
                registerRect[0] = mT2TrackRegions[id][0].getX();
                registerRect[1] = mT2TrackRegions[id][0].getY();
                registerRect[2] = mT2TrackRegions[id][0].getWidth();
                registerRect[3] = mT2TrackRegions[id][0].getHeight();
                mPreviewRequestBuilder[id].set(CaptureModule.t2t_register_roi, registerRect);
                mPreviewRequestBuilder[id].set(CaptureModule.t2t_cmd_trigger, t2tTrigger);
                if (mCurrentSceneMode.mode == CameraMode.HFR && mCurrentSession != null &&
                        mCurrentSession instanceof CameraConstrainedHighSpeedCaptureSession) {
                    if (mCurrentSession != null) {
                        List requestList = getHighSpeedList(
                                (CameraConstrainedHighSpeedCaptureSession) mCurrentSession,
                                mPreviewRequestBuilder[id]);
                        mCurrentSession.setRepeatingBurst(requestList, mCaptureCallback,
                                mCameraHandler);
                    }
                } else {
                    if (mCurrentSession != null) {
                        mCurrentSession.setRepeatingRequest(mPreviewRequestBuilder[id].build(),
                                mCaptureCallback, mCameraHandler);
                    }
                }
                Log.v(TAG,  "triggerTouchFocus is called. ROI " + registerRect[0] + " "
                            + registerRect[1] + " " + registerRect[2] + " " + registerRect[3]);
            }
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
         Log.w(TAG,EXCEPTION_LOG,e.toString());
        }
    }

    private void transformTouchCoords(float x, float y, int id) {
        if (mCropRegion[id] == null) {
            Log.d(TAG, "transformTouchCoords crop region is null at " + id);
            mInTAF = false;
            return;
        }
        Point p = mUI.getSurfaceViewSize();
        int width = p.x;
        int height = p.y;
            Log.d(TAG,  "transformTouchCoords crop region w: " + mCropRegion[id].width() +
                    " h: " + mCropRegion[id].height()+"surfaceViewP w:" + p.x +" h: " + p.y);
        if (width * mCropRegion[id].width() != height * mCropRegion[id].height()) {
            Point displayPoint = mUI.getDisplaySize();
            if (width > displayPoint.x) {
                height = width * mCropRegion[id].width() / mCropRegion[id].height();
            }
            if (height > displayPoint.y) {
                width = height * mCropRegion[id].height() / mCropRegion[id].width();
            }
        }
        x += (width - p.x) / 2;
        y += (height - p.y) / 2;
        mT2TrackRegions[id] = afaeRectangle(x, y, width, height, 1.25f, mCropRegion[id], id);
        Log.d(TAG, "transformTouchCoords " + mT2TrackRegions[id][0].getX() + " " +
                    mT2TrackRegions[id][0].getY() + " " + mT2TrackRegions[id][0].getWidth() +
                    " " + mT2TrackRegions[id][0].getHeight());
    }

    public void triggerFocusAtPoint(float x, float y, int id) {
        Log.d(TAG, "triggerFocusAtPoint " + x + " " + y + " " + id);
        if (mCropRegion[id] == null) {
            Log.d(TAG, "crop region is null at " + id);
            mInTAF = false;
            return;
        }
        Point p = mUI.getSurfaceViewSize();
        int width = p.x;
        int height = p.y;
        mAFRegions[id] = afaeRectangle(x, y, width, height, 1f, mCropRegion[id], id);
        mAERegions[id] = afaeRectangle(x, y, width, height, 1.5f, mCropRegion[id], id);
        mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[id]);
        autoFocusTrigger(id);
    }

    public void triggerFocusAtPointFA(float x, float y, int id) {
        Log.d(TAG, "triggerFocusAtPoint " + x + " " + y + " " + id);
        if (mCropRegion[id] == null) {
            Log.d(TAG, "crop region is null at " + id);
            mInTAF = false;
            return;
        }
        Point p = mUI.getSurfaceViewSize();
        int width = p.x;
        int height = p.y;
        if (width * mCropRegion[id].width() != height * mCropRegion[id].height()) {
            Point displayPoint = mUI.getDisplaySize();
            if (width >= displayPoint.x) {
                height = width * mCropRegion[id].width() / mCropRegion[id].height();
            }
            if (height >= displayPoint.y) {
                width = height * mCropRegion[id].height() / mCropRegion[id].width();
            }
        }
        x += (width - p.x) / 2;
        y += (height - p.y) / 2;
        mAFRegions[id] = afaeRectangle(x, y, width, height, 0.5f, mCropRegion[id], id);
        mAERegions[id] = afaeRectangle(x, y, width, height, 1.5f, mCropRegion[id], id);
        mCameraHandler.removeMessages(CANCEL_TOUCH_FOCUS, mCameraId[id]);
        autoFocusTrigger(id);
    }

    private void cancelTouchFocus(int id) {
        if(mPaused)
            return;
        Log.v(TAG, "cancelTouchFocus " + id+",mLockAFAE="+mLockAFAE+",mUI.isShutterEnabled()="
                +mUI.isShutterEnabled()+",isflashRequired="+isflashRequired
        +",mUI.isShutterEnabled()="+mUI.isShutterEnabled());
        if(isflashRequired && !mUI.isShutterEnabled()){
            return;
        }
        mInTAF = false;
        mState[id] = STATE_PREVIEW;
        if(mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
            return;
        }
        mControlAFMode = (mCurrentSceneMode.mode == CameraMode.VIDEO ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC) ?
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO :
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        mIsAutoFocusStarted = false;
        setAFModeToPreview(id, (isDevOptionSetting() && getDevAfMode() != -1) ?
                getDevAfMode() : mControlAFMode);
    }

    private MeteringRectangle[] afaeRectangle(float x, float y, int width, int height,
                                              float multiple, Rect cropRegion, int id) {
        int side = (int) (Math.max(width, height) / 8 * multiple);
        RectF meteringRegionF = new RectF(x - side / 2, y - side / 2, x + side / 2, y + side / 2);

        // inverse of matrix1 will translate from touch to (-1000 to 1000), which is camera1
        // coordinates, while accounting for orientation and mirror
        Matrix matrix1 = new Matrix();
        CameraUtil.prepareMatrix(matrix1, !isBackCamera(), mDisplayOrientation, width, height);
        matrix1.invert(matrix1);

        // inverse of matrix2 will translate from (-1000 to 1000) to camera 2 coordinates
        Matrix matrix2 = new Matrix();
        boolean postZoomFov = mUI.getZoomFixedSupport() && PersistUtil.isCameraPostZoomFOV();
        if (postZoomFov) {
            cropRegion = mOriginalCropRegion[id];
            if (mOriginalCropRegion[id].width() * width < mOriginalCropRegion[id].height() * height) {
                int heightNew = mOriginalCropRegion[id].width() * width / height;
                cropRegion = new Rect(0, 0 , mOriginalCropRegion[id].width(), heightNew);
            }else if(mOriginalCropRegion[id].width() * width > mOriginalCropRegion[id].height() * height){
                int widthNew = mOriginalCropRegion[id].height() * height / width;
                cropRegion = new Rect(0, 0 , widthNew, mOriginalCropRegion[id].height());
            }
        }
        Log.d(TAG,"width:" + width + ",height:" + height + ",mOriginalCropRegion[id]:" +mOriginalCropRegion[id].toString() + ",rect1:" + cropRegion.toString());
        matrix2.preTranslate(-cropRegion.width() / 2f,
                -cropRegion.height() / 2f);
        matrix2.postScale(2000f / cropRegion.width(),
                2000f / cropRegion.height());
        matrix2.invert(matrix2);

        matrix1.mapRect(meteringRegionF);
        matrix2.mapRect(meteringRegionF);

        if (!postZoomFov) {
            meteringRegionF.left = meteringRegionF.left * cropRegion.width()
                    / mOriginalCropRegion[id].width() + cropRegion.left;
            meteringRegionF.top = meteringRegionF.top * cropRegion.height()
                    / mOriginalCropRegion[id].height() + cropRegion.top;
            meteringRegionF.right = meteringRegionF.right * cropRegion.width()
                    / mOriginalCropRegion[id].width() + cropRegion.left;
            meteringRegionF.bottom = meteringRegionF.bottom * cropRegion.height()
                    / mOriginalCropRegion[id].height() + cropRegion.top;
        }
        Rect meteringRegion = new Rect((int) meteringRegionF.left, (int) meteringRegionF.top,
                (int) meteringRegionF.right, (int) meteringRegionF.bottom);
        Log.v(TAG, " meteringRegion left :" + meteringRegion.left + ", top:" +
                    meteringRegion.top + " right :" + meteringRegion.right +
                    ", bottom :" + meteringRegion.bottom +" cropRegion left :" + cropRegion.left + ", top:" +
                    cropRegion.top + " right :" + cropRegion.right +
                    ", bottom :" + cropRegion.bottom);
        int offsetY = 0;
        int offsetX = 0;
        offsetY = (mOriginalCropRegion[id].height()- cropRegion.height())/2;
        offsetX = (mOriginalCropRegion[id].width()- cropRegion.width())/2;
        meteringRegion = new Rect((int) meteringRegionF.left + offsetX, (int) (meteringRegionF.top + offsetY),
                (int) meteringRegionF.right + offsetX, (int) (meteringRegionF.bottom + offsetY));

        meteringRegion.left = CameraUtil.clamp(meteringRegion.left, mOriginalCropRegion[id].left,
                mOriginalCropRegion[id].right);
        meteringRegion.top = CameraUtil.clamp(meteringRegion.top, mOriginalCropRegion[id].top,
                mOriginalCropRegion[id].bottom);
        meteringRegion.right = CameraUtil.clamp(meteringRegion.right, mOriginalCropRegion[id].left,
                mOriginalCropRegion[id].right);
        meteringRegion.bottom = CameraUtil.clamp(meteringRegion.bottom, mOriginalCropRegion[id].top,
                mOriginalCropRegion[id].bottom);
        Log.v(TAG, " modify meteringRegion left :" + meteringRegion.left +
                    ", top:" + meteringRegion.top + " right :" + meteringRegion.right +
                    ", bottom :" + meteringRegion.bottom);
        MeteringRectangle[] meteringRectangle = new MeteringRectangle[1];
        meteringRectangle[0] = new MeteringRectangle(meteringRegion, 1);
        return meteringRectangle;
    }

    private void updateFocusStateChange(CaptureResult result) {
        Integer resultAFState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (resultAFState == null) return;
        try {
            Byte isDepthFocus = result.get(CaptureModule.is_depth_focus);
            if (isDepthFocus != null) {
                if (isDepthFocus == 1) {
                    mIsDepthFocus = true;
                } else {
                    mIsDepthFocus = false;
                }
            }
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            mIsDepthFocus = false;
            Log.w(TAG,EXCEPTION_LOG,e.toString());
        }
        if(resultAFState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED && mLockAFAE == LOCK_AF_AE_STATE_LOCK_DONE){
            updateLockAFAEVisibility();
        }
        if (mIsDepthFocus && !mInTAF) {
            mUI.showFocusCircle(false);
        }else{
            mUI.showFocusCircle(true);
        }
        final Integer afState = resultAFState;
        // Report state change when AF state has changed.

        Log.d(TAG,BIG_LOG,"resultAFState="+resultAFState+",mLastResultAFState="+mLastResultAFState+",mIsDepthFocus="+mIsDepthFocus);
        if ((resultAFState != mLastResultAFState
                || mUI.isChangeFocus()
                || (!mIsDepthFocus && mLastIsDepthFocus))
                && mFocusStateListener != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFocusStateListener.onFocusStatusUpdate(afState);
                }
            });
        }
        mLastResultAFState = resultAFState;
        mLastIsDepthFocus = mIsDepthFocus;

        Log.d(TAG, BIG_LOG, "resultAFState " + resultAFState + " mWasInFocusAssistMode " + mWasInFocusAssistMode);
        if (resultAFState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED &&
                !mWasInFocusAssistMode && isTouchFocusAssistSupported() && mVideoFrameNumber > tapUpFrameNumber + 10) {
            checkTouchFocusAssistEnable(result);
        }
    }

    private boolean isTouchFocusAssistSupported() {
        return (mCurrentSceneMode.mode == CameraMode.DEFAULT) && isBackCamera() && !isHDREnable();
    }

    private void checkTouchFocusAssistEnable(CaptureResult result) {
        if (mIsInFocusAssistMode) {
            return;
        }
        boolean enable = false;
        try {
            enable = result.get(focusAssistEnable) == 1;
        } catch (Exception e) {}
        if (enable) {
            mHandler.post(() -> {
                if (!mPaused) {
                    configureFASurface();
                    if (mFASurfaceConfigured) {
                        mUI.showFocusAssistText();
                    }
                }
            });
        }
    }

    private void setDisplayOrientation() {
        mDisplayRotation = CameraUtil.getDisplayRotation(mActivity);
        mDisplayOrientation = CameraUtil.getDisplayOrientationForCamera2(
                mDisplayRotation, getMainCameraId());
    }

    @Override
    public void onSettingsChanged(List<SettingsManager.SettingState> settings) {
        if (mPaused) return;
        boolean updatePreviewBayer = false;
        boolean updatePreviewMono = false;
        boolean updatePreviewFront = false;
        boolean updatePreviewLogical = false;
        int count = 0;
        for (SettingsManager.SettingState settingState : settings) {
            String key = settingState.key;
            SettingsManager.Values values = settingState.values;
            String value;
            if (values.overriddenValue != null) {
                value = values.overriddenValue;
            } else {
                value = values.value;
            }
            Log.i(TAG, " CaptureModule onSettingsChanged, key = " + key+",value="+value);
            switch (key) {
                case SettingsManager.KEY_CAMERA_SAVEPATH:
                    Storage.setSaveSDCard(value.equals("1"));
                    mActivity.updateStorageSpaceAndHint();
                    continue;
                case SettingsManager.KEY_JPEG_QUALITY:
                    estimateJpegFileSize();
                    continue;
                case SettingsManager.KEY_VIDEO_DURATION:
                    updateMaxVideoDuration();
                    continue;
                case SettingsManager.KEY_VIDEO_QUALITY:
                    updateVideoSize();
                    continue;
                case SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL:
                    updateTimeLapseSetting();
                    continue;
                case SettingsManager.KEY_FACE_DETECTION:
                    updateFaceDetection();
                    break;
                case SettingsManager.KEY_MONO_ONLY:
                case SettingsManager.KEY_CLEARSIGHT:
                case SettingsManager.KEY_MONO_PREVIEW:
                case SettingsManager.KEY_PHYSICAL_CAMERA:
                case SettingsManager.KEY_FORCE_AUX:
                    if (count == 0) restartAll();
                    return;
                case SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE:
                    mCurrentSceneMode.setSwithCameraId(-1,false);
                    if (count == 0) restartAll();
                    return;
                case SettingsManager.KEY_SWITCH_CAMERA:
                    int id = CameraUtil.strToInt(value,-1);
                    mCurrentSceneMode.setSwithCameraId(id,false);
                    restartAll();
                    return;
                    case SettingsManager.KEY_CAMERA_MANUALFLASH_LEVEL:
                        if(CameraMode.VIDEO == mCurrentSceneMode.mode){
                            updateVideoFlash(getMainCameraId());
                            break;
                        } else if(CameraMode.DEFAULT == mCurrentSceneMode.mode) {
                            applyFlashForUIChange(mPreviewRequestBuilder[getMainCameraId()],
                                    getMainCameraId());
                        }
                case SettingsManager.KEY_VIDEO_FLASH_MODE:
                    switch (mCurrentSceneMode.mode) {
                        case PRO_MODE:
                            applyFlashForUIChange(mPreviewRequestBuilder[getMainCameraId()],
                                    getMainCameraId());
                            break;
                        case VIDEO:
                        case HFR:
                            mUI.updateFlashBar();
                            updateVideoFlash(getMainCameraId());
                            break;
                    }
                    return;
                case SettingsManager.KEY_FLASH_MODE:
                    mUI.updateFlashBar();
                    applyFlashForUIChange(mPreviewRequestBuilder[getMainCameraId()],
                    getMainCameraId());
                    return;
                case SettingsManager.KEY_CAMERA_MANUALFLASH:
                case SettingsManager.KEY_MANUAL_EXPOSURE:
                    String manualExposureMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_EXPOSURE);
                    String manualFlashMode = mSettingsManager.getValue(SettingsManager.KEY_CAMERA_MANUALFLASH);
                    String flashMode =  mSettingsManager.getValue(SettingsManager.KEY_FLASH_MODE);
                    if(manualExposureMode.equals("user-setting") &&
                            (manualFlashMode != null && manualFlashMode.equals("1")) &&
                            mCurrentSceneMode.mode == CameraMode.DEFAULT &&
                       "auto".equals(flashMode)){
                        mUI.changeFlashMode(false);
                    }
                    return;
                case SettingsManager.KEY_ZSL:
                case SettingsManager.KEY_AUTO_HDR:
                case SettingsManager.KEY_RAW_FORMAT_TYPE:
                case SettingsManager.KEY_HDR:
                    if (count == 0) restartSession(false);
                    return;
                case SettingsManager.KEY_SCENE_MODE:
                    restartAll();
                    return;
                case SettingsManager.KEY_AI_BLUR_SHAPE:
                case SettingsManager.KEY_AI_BLUR_STRENGTH:
                case SettingsManager.KEY_AI_BLUR_DISTANCE:
                case SettingsManager.KEY_AI_BLUR_LUMA:
                case SettingsManager.KEY_AI_BLUR_CHROMAU:
                case SettingsManager.KEY_AI_BLUR_CHROMAV:
                case SettingsManager.KEY_AI_BLUR_CHROMASTRENGTH:
                    applyAIBlurConfig(key,mVideoRecordRequestBuilder);
                    applyAIBlurConfig(key,mVideoPreviewRequestBuilder);
                    try {
                        mCaptureSession[getMainCameraId()].setRepeatingRequest(mPreviewRequestBuilder[getMainCameraId()].build(), mCaptureCallback, mCameraHandler);
                    } catch (CameraAccessException | IllegalStateException e) {
                        Log.e(TAG, "Camera Access Exception in applyAIBlurConfig, apply failed");
                    }
                    return;
            }
            updatePreviewLogical |= applyPreferenceToPreview(getMainCameraId(),
                    key, value);
            count++;
        }
        if (updatePreviewBayer) {
            try {
                if (checkSessionAndBuilder(mCaptureSession[BAYER_ID],
                        mPreviewRequestBuilder[BAYER_ID])) {
                    if (mIsRecordingVideo && mHighSpeedCapture) {
                        List requestList =getHighSpeedList((CameraConstrainedHighSpeedCaptureSession) mCaptureSession[BAYER_ID],
                                mPreviewRequestBuilder[BAYER_ID]);
                        mCaptureSession[BAYER_ID].setRepeatingBurst(requestList, mCaptureCallback,
                                mCameraHandler);
                    } else {
                        mCaptureSession[BAYER_ID].setRepeatingRequest(mPreviewRequestBuilder[BAYER_ID]
                                .build(), mCaptureCallback, mCameraHandler);
                    }
                }
            } catch (CameraAccessException | IllegalStateException e) {
                Log.w(TAG,e.toString());
            }
        }
        if (updatePreviewMono) {
            try {
                if (checkSessionAndBuilder(mCaptureSession[MONO_ID],
                        mPreviewRequestBuilder[MONO_ID])) {
                    if (canStartMonoPreview()) {
                        mCaptureSession[MONO_ID].setRepeatingRequest(mPreviewRequestBuilder[MONO_ID]
                                .build(), mCaptureCallback, mCameraHandler);
                    } else {
                        mCaptureSession[MONO_ID].capture(mPreviewRequestBuilder[MONO_ID]
                                .build(), mCaptureCallback, mCameraHandler);
                    }
                }
            } catch (CameraAccessException | IllegalStateException e) {
                Log.w(TAG,e.toString());
            }
        }
        if (updatePreviewFront) {
            try {
                if (checkSessionAndBuilder(mCaptureSession[FRONT_ID],
                        mPreviewRequestBuilder[FRONT_ID])) {
                    mCaptureSession[FRONT_ID].setRepeatingRequest(mPreviewRequestBuilder[FRONT_ID]
                            .build(), mCaptureCallback, mCameraHandler);
                }
            } catch (CameraAccessException | IllegalStateException e) {
                Log.w(TAG,e.toString());
            }
        }

        if (updatePreviewLogical) {
            try {
                int cameraId = getMainCameraId();
                if (checkSessionAndBuilder(mCaptureSession[cameraId],
                        mPreviewRequestBuilder[cameraId])) {
                    if (mCaptureSession[cameraId] instanceof CameraConstrainedHighSpeedCaptureSession) {
                        List<CaptureRequest> list = getHighSpeedList((CameraConstrainedHighSpeedCaptureSession)
                                mCaptureSession[cameraId],
                                        mPreviewRequestBuilder[cameraId]);
                        mCaptureSession[cameraId].setRepeatingBurst(list, mCaptureCallback,
                                mCameraHandler);
                    } else if (isSSMEnabled()) {
                        mCaptureSession[cameraId].setRepeatingBurst(createSSMBatchRequest(
                                mPreviewRequestBuilder[cameraId]), mCaptureCallback, mCameraHandler);
                    } else {
                        mCaptureSession[cameraId].setRepeatingRequest(mPreviewRequestBuilder[cameraId]
                                .build(), mCaptureCallback, mCameraHandler);
                    }
                }
            } catch (CameraAccessException | IllegalStateException e) {
                Log.e(TAG,e.toString());
            }
        }
    }

    private boolean isPanoSetting(String value) {
        try {
            int mode = Integer.parseInt(value);
            if(mode == SettingsManager.SCENE_MODE_PANORAMA_INT) {
                return true;
            }
        } catch(Exception e) {
        }
        return false;
    }

    private boolean isCaptureBrustMode() {
        boolean isCaptureBrustMode = false;
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (value != null) {
            int mode = Integer.parseInt(value);
            if(mode == SettingsManager.SCENE_MODE_NIGHT_INT ||
                    mode == SettingsManager.SCENE_MODE_SHARPSHOOTER_INT ||
                    mode == SettingsManager.SCENE_MODE_BLURBUSTER_INT ||
                    mode == SettingsManager.SCENE_MODE_BESTPICTURE_INT ||
                    mode == SettingsManager.SCENE_MODE_OPTIZOOM_INT ||
                    mode == SettingsManager.SCENE_MODE_UBIFOCUS_INT) {
                isCaptureBrustMode = true;
            }
        }

        return isCaptureBrustMode || mLongshotActive;
    }

    public boolean isDeepZoom() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        try {
            int mode = Integer.parseInt(value);
            if(mode == SettingsManager.SCENE_MODE_DEEPZOOM_INT) {
                return true;
            }
        } catch(Exception e) {
        }
        return false;
    }

    private void updateFaceDetection() {
        final String value = mSettingsManager.getValue(SettingsManager.KEY_FACE_DETECTION);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == null || value.equals("off")
                        || !mSettingsManager.isFDRenderingAtPreview())
                    mUI.onStopFaceDetection();
                else {
                    String physical_id = mSettingsManager.getSinglePhysicalCamera();
                    if (physical_id != null &&
                            !SettingsManager.LOGICAL_AND_PHYSICAL.equals(physical_id)) {
                        int id = Integer.valueOf(physical_id);
                        cropRegionForZoom(id, false);
                        mUI.onStartFaceDetection(mDisplayOrientation,
                                mSettingsManager.isFacingFront(getMainCameraId()),
                                mCropRegion[id],
                                mSettingsManager.getSensorActiveArraySize(id));
                    } else {
                        mUI.onStartFaceDetection(mDisplayOrientation,
                                mSettingsManager.isFacingFront(getMainCameraId()),
                                mCropRegion[getMainCameraId()],
                                mSettingsManager.getSensorActiveArraySize(getMainCameraId()));
                    }
                }
            }
        });
    }

    public void restartAll() {
        int duration = 3000;
        int[] list = {0x40400000, 0x1, 0x40C00000, 0x1, 0x40804000, 0X687, 0x40800000, 0X687,
                0x40804100, 0X660, 0x40800100, 0X660, 0x40800200, 0X8C6, 0x40804200, 0X8C6};
        if(mPostProcessor.isJniAPISupported())
            mPostProcessor.nativePerfLockAcq(1, duration, list, list.length);
        mLockNums.set(0);
        mResumed = false;
        int nextCameraId = getNextScreneModeId(mNextModeIndex);
        Log.i(TAG, "restart all CURRENT_ID :" + CURRENT_ID + " nextCameraId :" + nextCameraId +",mLockAFAE :" +  mLockAFAE);
        if(mLockAFAE != LOCK_AF_AE_STATE_NONE) {
            mLockAFAE = LOCK_AF_AE_STATE_NONE;
            applyIsAfLock(false);
            mUI.clearFocus();
            applySettingsForUnlockExposure(mPreviewRequestBuilder[CURRENT_ID], CURRENT_ID);
            updateLockAFAEVisibility();
        }
        if(CURRENT_ID == nextCameraId && mCameraDevice[CURRENT_ID] != null){
            mIsCloseCamera = false;
        }else{
            mIsCloseCamera = true;
        }
        if(!mIsCloseCamera){
            mOpenCameraLatency = 0;
            mCloseCameraLatency = 0;
        }
        onPauseBeforeSuper();
        onPauseAfterSuper(false);
        reinitSceneMode();
        onResumeBeforeSuper(true);
        onResumeAfterSuper(true);
        mResumed = true;
        setRefocusLastTaken(false);
    }

    public void restartSession(boolean isSurfaceChanged) {
        Log.i(TAG, "restartSession isSurfaceChanged = " + isSurfaceChanged+",mIsCloseCamera="+mIsCloseCamera);
        if (isAllSessionClosed()) return;
        if(!mIsCloseCamera) {
            reinit();
        }
        closeProcessors();
        closeSessions();
        if(isSurfaceChanged) {
            //run in UI thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mUI.hideSurfaceView();
                    mUI.showSurfaceView();
                }
            });
        }
        if(!mIsCloseCamera) {
            updatePreviewSurfaceReadyState(false);
        }
        initializeValues();
        updatePreviewSize();
        openProcessors();
        createSessions();
        mActivity.runOnUiThread(() -> {
            if(isTrackingFocusSettingOn()) {
                mUI.resetTrackingFocus();
            }
            if (isT2TFocusSettingOn()) {
                mUI.resetTouchTrackingFocus();
            }
            if (isSateNNFocusSettingOn()) {
                mUI.resetStatsNNTrackingFocus();
            }
            if (isSateAFSettingOn()) {
                mUI.resetAFRender();
            }
        });
        resetStateMachine();
    }

    private void resetStateMachine() {
        for (int i = 0; i < MAX_NUM_CAM; i++) {
            mState[i] = STATE_PREVIEW;
        }
        mUI.enableShutter(true);
    }

    public Size getOptimalPhysicalPreviewSize(Size pictureSize, Size[] prevSizes) {
        Point[] points = new Point[prevSizes.length];
        DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();
        double targetRatio = (double) pictureSize.getWidth() / pictureSize.getHeight();

        int index = 0;
        int point_max[]  = new int[]{dm.heightPixels/2,dm.widthPixels/2};
        int max_size = -1;
        if (point_max != null){
            max_size = point_max[0] * point_max[1];
        }
        for (Size s : prevSizes) {
            if (max_size != -1){
                int size = s.getWidth() * s.getHeight();
                if (s.getWidth() == s.getHeight()){
                    if (s.getWidth() > Math.min(point_max[0],point_max[1]))
                        continue;
                } else if (size > max_size || size == 0 || s.getHeight() > point_max[1]) {
                    continue;
                }
            }
            points[index++] = new Point(s.getWidth(), s.getHeight());
        }

        int optimalPickIndex = CameraUtil.getOptimalPreviewSize(mActivity, points, targetRatio);
        Size ret = (optimalPickIndex == -1) ? null :
                new Size(points[optimalPickIndex].x,points[optimalPickIndex].y);
        if (ret.getWidth() == ret.getHeight() && ret.getWidth() > dm.widthPixels/2){
            ret = new Size(dm.widthPixels/2,dm.widthPixels/2);
        }
        return ret;
    }

    private Size getOptimalPreviewSize(Size pictureSize, Size[] prevSizes) {
        if (prevSizes == null)return null;
        Point[] points = new Point[prevSizes.length];
        double targetRatio = (double) pictureSize.getWidth() / pictureSize.getHeight();
        int index = 0;
        for (Size s : prevSizes) {
            if(s.getWidth()*s.getHeight() < pictureSize.getWidth() * pictureSize.getHeight()) {
                points[index++] = new Point(s.getWidth(), s.getHeight());
            }
        }

        int optimalPickIndex = CameraUtil.getOptimalPreviewSize(mActivity, points, targetRatio);
        return (optimalPickIndex == -1) ? null :
                new Size(points[optimalPickIndex].x,points[optimalPickIndex].y);
    }

    private Size getOptimalVideoPreviewSize(Size VideoSize, Size[] prevSizes) {
        Point[] points = new Point[prevSizes.length];

        int index = 0;
        for (Size s : prevSizes) {
            points[index++] = new Point(s.getWidth(), s.getHeight());
        }

        int optimalPickIndex = CameraUtil.getOptimalVideoPreviewSize(mActivity, points, VideoSize);
        return (optimalPickIndex == -1) ? null :
                new Size(points[optimalPickIndex].x,points[optimalPickIndex].y);
    }

    public TrackingFocusRenderer getTrackingForcusRenderer() {
        return mUI.getTrackingFocusRenderer();
    }

    public TouchTrackFocusRenderer getT2TFocusRenderer() {
        return mUI.getT2TFocusRenderer();
    }

    public StateNNTrackFocusRenderer getStatsNNFocusRenderer() {
        return mUI.getStatsNNFocusRenderer();
    }

    private class MyCameraHandler extends Handler {

        public MyCameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int id = msg.arg1;
            switch (msg.what) {
                case OPEN_CAMERA:
                    openCamera(id);
                    break;
                case CANCEL_TOUCH_FOCUS:
                    if(!mUI.getIsEvChanging()) {
                        cancelTouchFocus(id);
                    }else{
                        Message message =
                                mCameraHandler.obtainMessage(CANCEL_TOUCH_FOCUS);
                        sendFocusCancelMsg(message);
                    }
                    break;
            }
        }
    }

    private void sendFocusCancelMsg(Message message) {
        mWasInFocusAssistMode = false;
        if (CANCEL_TOUCH_FOCUS_DELAY <= 0) {
            return;
        }
        mCameraHandler.sendMessageDelayed(message, CANCEL_TOUCH_FOCUS_DELAY);
    }

    private class MpoSaveHandler extends Handler {
        static final int MSG_CONFIGURE = 0;
        static final int MSG_NEW_IMG = 1;

        private Image monoImage;
        private Image bayerImage;
        private Long captureStartTime;

        public MpoSaveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_CONFIGURE:
                captureStartTime = (Long) msg.obj;
                break;
            case MSG_NEW_IMG:
                processNewImage(msg);
                break;
            }
        }

        private void processNewImage(Message msg) {
            Log.d(TAG, "MpoSaveHandler:processNewImage for cam id: " + msg.arg1);
            if(msg.arg1 == MONO_ID) {
                monoImage = (Image)msg.obj;
            } else if(bayerImage == null){
                bayerImage = (Image)msg.obj;
            }

            if(monoImage != null && bayerImage != null) {
                saveMpoImage();
            }
        }

        private void saveMpoImage() {
            mNamedImages.nameNewImage(captureStartTime);
            NamedEntity namedEntity = mNamedImages.getNextNameEntity();
            String title = (namedEntity == null) ? null : namedEntity.title;
            long date = (namedEntity == null) ? -1 : namedEntity.date;
            int width = bayerImage.getWidth();
            int height = bayerImage.getHeight();
            byte[] bayerBytes = getJpegData(bayerImage);
            byte[] monoBytes = getJpegData(monoImage);

            ExifInterface exif = null;
            try {
                exif = new ExifInterface(new ByteArrayInputStream(bayerBytes));
            } catch (IOException e) {
                Log.w(TAG,"get exif failed");
            }
            int orientation = 0;
            if (exif != null) {
                orientation = CameraUtil.getOrientation(exif);
            } else {
                orientation = CameraUtil.getJpegRotation(getMainCameraId(),mOrientation);
            }
            mActivity.getMediaSaveService().addMpoImage(
                    null, bayerBytes, monoBytes, width, height, title,
                    date, null, orientation, mOnMediaSavedListener, mContentResolver, "jpeg");

            mActivity.updateThumbnail(bayerBytes);

            bayerImage.close();
            bayerImage = null;
            monoImage.close();
            monoImage = null;
            namedEntity = null;
        }
    }

    @Override
    public void onReleaseShutterLock() {
        Log.d(TAG, "onReleaseShutterLock");
        unlockFocus(BAYER_ID);
        unlockFocus(MONO_ID);
    }

    @Override
    public void onClearSightSuccess(byte[] thumbnailBytes) {
        Log.d(TAG, "onClearSightSuccess");
        onReleaseShutterLock();
        if(thumbnailBytes != null) mActivity.updateThumbnail(thumbnailBytes);
        warningToast(R.string.clearsight_capture_success, false);
    }

    @Override
    public void onClearSightFailure(byte[] thumbnailBytes) {
        Log.d(TAG, "onClearSightFailure");
        if(thumbnailBytes != null) mActivity.updateThumbnail(thumbnailBytes);
        warningToast(R.string.clearsight_capture_fail, false);
        onReleaseShutterLock();
    }

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KEEP_SCREEN_ON:
                    mActivity.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                case CLEAR_SCREEN_DELAY: {
                    mActivity.getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }
                case UPDATE_RECORD_TIME: {
                    updateRecordingTime();
                    break;
                }
                case VOICE_INTERACTION_CAPTURE: {
                    if (mIntentMode == INTENT_MODE_STILL_IMAGE_CAMERA && mIsVoiceTakePhote) {
                        onShutterButtonClick();
                        mIsVoiceTakePhote = false;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onErrorListener(int error) {
        enableRecordingLocation(false);
    }

    // from MediaRecorder.OnErrorListener
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (mRecordingStarted) {
            stopRecordingVideo(getMainCameraId());
        }
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            // We may have run out of space on the sdcard.
            mActivity.updateStorageSpaceAndHint();
        } else {
           warningToast("MediaRecorder error. what=" + what + ". extra=" + extra);
        }
    }

    // from MediaRecorder.OnInfoListener
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.w(TAG, " MediaRecorder MEDIA_RECORDER_INFO_MAX_DURATION_REACHED mIsRecordingVideo="+mIsRecordingVideo);
            if (mIsRecordingVideo) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopRecordingVideo(getMainCameraId());
                    }
                });
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            Log.w(TAG, " MediaRecorder MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED mIsRecordingVideo="+mIsRecordingVideo);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mIsRecordingVideo) {
                        stopRecordingVideo(getMainCameraId());
                    }
                     // Show the toast.
                    RotateTextToast.makeText(mActivity, R.string.video_reach_size_limit,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private byte[] getJpegData(Image image) {
        Log.v(TAG, "getJpegData image :" + image);
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        Log.v(TAG, "getJpegData buffer :" + buffer);
        byte[] bytes = new byte[buffer.remaining()];
        Log.v(TAG, "getJpegData bytes :" + bytes);
        buffer.get(bytes);
        return bytes;
    }

    private void updateSaveStorageState() {
        Storage.setSaveSDCard(mSettingsManager.getValue(SettingsManager
                .KEY_CAMERA_SAVEPATH).equals("1"));
    }

    public void startPlayVideoActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(mCurrentVideoUri,
                CameraUtil.convertOutputFormatToMimeType(mProfile.fileFormat));
        try {
            mActivity
                    .startActivityForResult(intent, CameraActivity.REQ_CODE_DONT_SWITCH_TO_PREVIEW);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + mCurrentVideoUri + ex);
        }
    }

    private void closeVideoFileDescriptor() {
        if (mVideoFileDescriptor != null) {
            try {
                mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd" + e);
            }
            mVideoFileDescriptor = null;
        }
    }

    private Bitmap getVideoThumbnail() {
        Bitmap bitmap = null;
        if (mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(mVideoFileDescriptor.getFileDescriptor(),
                    mVideoPreviewSize.getWidth());
        } else if (mCurrentVideoUri != null) {
            try {
                mVideoFileDescriptor = mContentResolver.openFileDescriptor(mCurrentVideoUri, "r");
                bitmap = Thumbnail.createVideoThumbnailBitmap(
                        mVideoFileDescriptor.getFileDescriptor(), mVideoPreviewSize.getWidth());
            } catch (java.io.FileNotFoundException ex) {
                // invalid uri
                Log.e(TAG, ex.toString());
            }
        }

        if (bitmap != null) {
            // MetadataRetriever already rotates the thumbnail. We should rotate
            // it to match the UI orientation (and mirror if it is front-facing camera).
            boolean mirror = mPostProcessor.isSelfieMirrorOn();
            bitmap = CameraUtil.rotateAndMirror(bitmap, 0, mirror);
        }
        return bitmap;
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    private void releaseMediaRecorder() {
        Log.i(TAG, "Releasing media recorder mMediaRecorder="+mMediaRecorder);
        long releaseMedia = System.currentTimeMillis();
        deleteInvalidUri();
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder");
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder -- clean");
        cleanupEmptyFile();
        if (TRACE_DEBUG) Trace.endSection();
        if (mMediaRecorder != null) {
            try{
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder -- reset");
                mMediaRecorder.reset();
                if (TRACE_DEBUG) Trace.endSection();
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder-- release");
                mMediaRecorder.release();
                if (TRACE_DEBUG) Trace.endSection();
                if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder-- releasePhysical");
                releasePhysicalRecorder();
                if (TRACE_DEBUG) Trace.endSection();
            }catch (RuntimeException e) {
                Log.e(TAG,e.toString());
            }
            mMediaRecorder = null;
            for (int i=0;i<mPhysicalMediaRecorders.length;i++){
                mPhysicalMediaRecorders[i] = null;
            }
            if(mActivity.getPerformenceTest()){
                mHasMapTimes.put("startReleaseMedia->endReleaseMedia",System.currentTimeMillis() - releaseMedia);
                if(PersistUtil.enableMediaRecorder()) {
                    mHasMapTimes.put("Total", System.currentTimeMillis() - mStartedTime);
                }
            }
        }
        if (PersistUtil.needAudioEncoder()) {
            AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
            // Set default values for HDR settings
            if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseMediaRecorder -- setDefaultHDRParameters");
            setDefaultHDRParameters(am);
            if (TRACE_DEBUG) Trace.endSection();
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private void cleanupEmptyFile() {
        if (mVideoFileDescriptor != null) {
            try {
                mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.w(TAG,e.toString());
            }
            mVideoFileDescriptor = null;
        }
        Arrays.fill(mPhysicalUris, null);
    }

    private void deleteInvalidUri() {
        for (Uri uri : mUrisInvalid) {
            if (uri != null) {
                try {
                    Log.d(TAG, "deleteInvalidUri " + uri);
                    mContentResolver.delete(uri, null);
                } catch (Exception e) {}
            }
        }
        mUrisInvalid.clear();
    }

    private void showToast(String tips) {
        if (mToast == null) {
            mToast = Toast.makeText(mActivity, tips, Toast.LENGTH_LONG);
            mToast.setGravity(Gravity.CENTER, 0, 0);
        }
        mToast.setText(tips);
        mToast.show();
    }

    private boolean isRecorderReady() {
        if ((mStartRecPending == true || mStopRecPending == true))
            return false;
        else
            return true;
    }

    /*
     * Make sure we're not recording music playing in the background, ask the
     * MediaPlaybackService to pause playback.
     */
    private void requestAudioFocus() {
        if (!PersistUtil.needAudioEncoder()) return;
        AudioManager am = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
        // Send request to obtain audio focus. This will stop other
        // music stream.
        int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.v(TAG, "Audio focus request failed");
        }
    }

    private void releaseAudioFocus() {
        if (!PersistUtil.needAudioEncoder()) return;
        if (TRACE_DEBUG) Trace.beginSection("SnapCamera,releaseAudioFocus");
        AudioManager am = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
        int result = am.abandonAudioFocus(null);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.v(TAG, "Audio focus release failed");
        }
        if (TRACE_DEBUG) Trace.endSection();
    }

    private boolean isVideoCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return (MediaStore.ACTION_VIDEO_CAPTURE.equals(action));
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.runOnUiThread(() -> mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mHandler.sendEmptyMessage(KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    private void setProModeVisible() {
        boolean promode = mCurrentSceneMode.mode == CameraMode.PRO_MODE;
        mUI.initializeProMode(!mPaused && promode);
    }

    private void seBlurConfigSlideVisible() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        boolean isBokeh = value != null && (value.equals("rtb") || value.equals("single_rear_aibokeh"));
        mUI.initializeBlurConfigSlide(!mPaused && isBokeh);
    }

    boolean checkSessionAndBuilder(CameraCaptureSession session, CaptureRequest.Builder builder) {
        return session != null && builder != null;
    }

    public boolean isSSMEnabled() {
        return mSuperSlomoCapture && (int)mHighSpeedFPSRange.getUpper() > NORMAL_SESSION_MAX_FPS
                && (mCurrentSceneMode.mode == CameraMode.HFR);
    }

    /**
     * if it is HFR or HSR recording and rate > 60
     * @return if it is high speed rate recording
     */
    public boolean isHighSpeedRateCapture() {
        return mHighSpeedCapture && mHighSpeedFPSRange != null && (int)mHighSpeedFPSRange.getUpper() > NORMAL_SESSION_MAX_FPS;
    }

    public void onRenderComplete(DPImage dpimage, boolean isError) {
        dpimage.mImage.close();
        if(isError) {
            getGLCameraPreview().requestRender();
        }
    }

    public void onRenderSurfaceCreated() {
        updatePreviewSurfaceReadyState(true);
        mUI.initThumbnail();
        if (getFrameFilters().size() == 0) {
            Log.d(TAG,"DeepPortraitFilter is not in frame filter list.");
            return;
        }
        mRenderer = getGLCameraPreview().getRendererInstance();
        DeepPortraitFilter filter = (DeepPortraitFilter)getFrameFilters().get(0);
        if (filter != null) {
            if (filter.getDPInitialized()) {
                int degree = getSensorOrientation();
                int adjustedRotation = ( degree - getDisplayOrientation() + 360 ) % 360;
                int surfaceRotation =
                        90 * mActivity.getWindowManager().getDefaultDisplay().getRotation();
                mRenderer.setMaskResolution(filter.getDpMaskWidth(),filter.getDpMaskHieght());
                mRenderer.setRotationDegree(
                        adjustedRotation, (degree - surfaceRotation + 360) % 360);
            }
        }
    }
    public void onRenderSurfaceDestroyed() {
        mRenderer = null;
    }

    public static class HeifImage {
        private HeifWriter mWriter;
        private String mPath;
        private String mTitle;
        private long mDate;
        private int mQuality;
        private int mOrientation;
        private Surface mInputSurface;

        public HeifImage(HeifWriter writer, String path, String title, long date, int orientation, int quality) {
            mWriter = writer;
            mPath = path;
            mTitle = title;
            mDate = date;
            mQuality = quality;
            mOrientation = orientation;
            mInputSurface = writer.getInputSurface();
        }

        public HeifWriter getWriter() {
            return mWriter;
        }

        public String getPath() {
            return mPath;
        }

        public String getTitle() {
            return mTitle;
        }

        public long getDate() {
            return mDate;
        }

        public int getQuality() {
            return mQuality;
        }

        public Surface getInputSurface() {
            return mInputSurface;
        }

        public int getOrientation() {
            return mOrientation;
        }
    }

    public CameraMode getCurrenCameraMode() {
        if (mCurrentSceneMode == null) {
            Log.d(TAG,"getCurrenCameraMode mCurrentSceneMode is NULL retrun CameraMode.DEFAULT");
            return CameraMode.DEFAULT;
        } else {
            return mCurrentSceneMode.mode;
        }
    }
    public OnItemClickListener getModeItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public int onItemClick(int mode) {
                if (!getCameraModeSwitcherAllowed()) {
                    return -1;
                }
                if(mActivity.getPerformenceTest()) {
                    mStartedTime = System.currentTimeMillis();
                }
                mUI.smoothSelectedPosition(mode);
                return selectCameraMode(mode);
            }
        };
    }

    public int selectCameraMode(int mode) {
        if (mCurrentSceneMode.mode == mSceneCameraIds.get(mode).mode || mRecordingStarted) {
            return -1;
        }
        setCameraModeSwitcherAllowed(false);
        setNextSceneMode(mode);
        SceneModule nextSceneMode = mSceneCameraIds.get(mode);
        String value = mSettingsManager.getValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
        if (value != null && value.equals("front") &&
                (nextSceneMode.mode == CameraMode.RTB ||
                 nextSceneMode.mode == CameraMode.SAT ||
                 nextSceneMode.mode == CameraMode.CINEMATIC ||
                 nextSceneMode.mode == CameraMode.PRO_MODE ||
                 nextSceneMode.mode == CameraMode.DEPTH ||
                 (nextSceneMode.mode == CameraMode.HFR &&
                         !mSettingsManager.isFrontIDHFRSupported()))) {
            mSettingsManager.setValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE, "rear");
        } else {
            restartAll();
        }
        updateZoomSeekBarVisible();
        return 1;
    }

    public void updateZoomSeekBarVisible() {
        String multiCam = mSettingsManager.getValue(SettingsManager.KEY_MULTI_CAMERAS_MODE);
        if (mCurrentSceneMode.mode == CameraMode.PRO_MODE  || mCurrentSceneMode.mode == CameraMode.DEPTH ||
                mCurrentSceneMode.mode == CameraMode.CINEMATIC || mIsRTBCameraId ||
                mCurrentSceneMode.mode == CameraMode.RTB || (isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
            if (mCurrentSceneMode.mode == CameraMode.RTB || (isRTBModeInSelectMode() && !mSettingsManager.isAICameraOn())) {
                float[] zoomRatioRange = mSettingsManager.getSupportedBokenRatioZoomRange(
                        getMainCameraId());
                if (zoomRatioRange == null || (zoomRatioRange[0] == zoomRatioRange[1])) {
                    Log.v(TAG, "updateZoomSeekBarVisible mZoomValue :" + mZoomValue);
                    mUI.hideZoomSeekBar(true);
                } else if (zoomRatioRange != null && zoomRatioRange[0] != zoomRatioRange[1]) {
                    mUI.updateZoombarValue(mZoomValue);
                    mUI.showZoomSeekBar();
                    Log.v(TAG, "updateZoomSeekBarVisible showZoomSeekBar mZoomValue :" + mZoomValue);
                }
            }else{
                mUI.hideZoomSeekBar(false);
            }
        } else if(multiCam != null && multiCam.equals("on")) {
            mUI.hideZoomSeekBar(true);
        }else {
            mUI.showZoomSeekBar();
        }

    }

    private void updateAICameraSeekBar(){
        if(!mSettingsManager.isAICameraDisable()){
            mUI.showAICameraSeekBar();
        }else{
            mUI.hideAICameraSeekBar();
        }
    }

    private void updateBokehText() {
        if(mSettingsManager.isIntegratedModeSupported()) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_INTEGRATED_MODE);
            if (value != null && value.equals("On")) {
                mBokehText.setVisibility(View.VISIBLE);
                mBokehText.setText("Bokeh Off");
            } else {
                mBokehText.setVisibility(View.INVISIBLE);
            }
        }
    }

    private int getNextScreneModeId(int mode) {
        SceneModule nextSceneModule = mSceneCameraIds.get(mode);
        return nextSceneModule.getNextCameraId(nextSceneModule.mode);
    }

    public void setNextSceneMode(int index) {
        mNextModeIndex = index;
    }

    public int getCurrentModeIndex() {
        return mCurrentModeIndex;
    }

    public List<String> getCameraModeList() {
        ArrayList<String> cameraModes = new ArrayList<>();
        for (SceneModule sceneModule : mSceneCameraIds) {
            cameraModes.add(mSelectableModes[sceneModule.mode.ordinal()]);
        }
        return cameraModes;
    }

    public String[] getSelectableModes() {
        return mSelectableModes;
    }

    private class SceneModule {
        CameraMode mode = CameraMode.DEFAULT;
        public int rearCameraId = -1;
        public int frontCameraId = -1;
        public int auxCameraId = 0;
        public int swithCameraId = -1;
        int getCurrentId() {
            int cameraId = isBackCamera() ? rearCameraId : frontCameraId;
            cameraId = isForceAUXOn(this.mode) ? auxCameraId : cameraId;
            if ((this.mode == CameraMode.DEFAULT || this.mode == CameraMode.VIDEO ||
                      this.mode == CameraMode.HFR || this.mode == CameraMode.PRO_MODE)
                    && (isDevOptionSetting() || swithCameraId != -1)) {
                String value = mSettingsManager.getValue(SettingsManager.KEY_SWITCH_CAMERA);
                if (value != null && !value.equals("-1")) {
                    cameraId = Integer.valueOf(value);
                    switchedCameraId = true;
                }else{
                    switchedCameraId = false;
                }
            }
            if (swithCameraId != -1){
                cameraId = swithCameraId;
            }
            String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
            if (selectMode != null && (selectMode.equals("single_rear_cameraid") || selectMode.equals("single_rear_aibokeh")) && mSingleRearId !=-1) {
                cameraId = mSingleRearId;
            } else if (selectMode != null && selectMode.equals("sat") && mLogicalId != -1) {
                cameraId = mLogicalId;
            }
            String quadBayer =  mSettingsManager.getValue(SettingsManager.KEY_QUAD_BAYER_SENSOR);
            if (quadBayer != null && !quadBayer.equals("-1")) {
                cameraId = Integer.parseInt(quadBayer);
            }
            return checkCameraId(cameraId);
        }

        public int getNextCameraId(CameraMode nextMode) {
            int cameraId = isBackCamera() ? rearCameraId : frontCameraId;
            cameraId = isForceAUXOn(this.mode) ? auxCameraId : cameraId;
            Log.i(TAG, " getNextCameraId cameraId=" + cameraId + ",mode=" + this.mode +
                    ",swithCameraId=" + swithCameraId);
            if ((this.mode == CameraMode.DEFAULT || this.mode == CameraMode.VIDEO ||
                    this.mode == CameraMode.HFR || this.mode == CameraMode.PRO_MODE)
                    && (isDevOptionSetting() || swithCameraId != -1)) {
                final SharedPreferences pref = mActivity.getSharedPreferences(
                        ComboPreferences.getLocalSharedPreferencesName(mActivity,
                                mSettingsManager.getNextPrepNameKey(nextMode)), Context.MODE_PRIVATE);
                String switchValue = pref.getString(SettingsManager.KEY_SWITCH_CAMERA, null);
                if (switchValue != null && !switchValue.equals("-1")) {
                    cameraId = Integer.valueOf(switchValue);
                    switchedCameraId = true;
                } else {
                    switchedCameraId = false;
                }
                String selectMode = pref.getString(SettingsManager.KEY_SELECT_MODE, null);
                if (selectMode != null && (selectMode.equals("single_rear_cameraid") || selectMode.equals("single_rear_aibokeh")) && mSingleRearId != -1) {
                    return mSingleRearId;
                } else if (selectMode != null && selectMode.equals("sat") && mLogicalId != -1) {
                    return mLogicalId;
                }
                String quadBayer = pref.getString(SettingsManager.KEY_QUAD_BAYER_SENSOR, null);
                if (quadBayer != null && !quadBayer.equals("-1")) {
                    return Integer.parseInt(quadBayer);
                }
                if (swithCameraId != -1) {
                    cameraId = swithCameraId;
                }

            }
            Log.d(TAG,"cameraId ="+cameraId+",checkCameraId(cameraId)");
            return checkCameraId(cameraId);
        }

        private int checkCameraId(int cameraId) {
            int retId = cameraId;
            if (retId == -1) {
                if (rearCameraId != -1) {
                    retId = rearCameraId;
                } else if (frontCameraId != -1) {
                    retId =  frontCameraId;
                }
            }
            return retId;
        }

        public void setSwithCameraId(int swithCameraId,boolean switcher) {
            this.swithCameraId = swithCameraId;
            Log.i(TAG,"swithCameraId="+swithCameraId);
            if(swithCameraId == CaptureModule.FRONT_ID && switcher) {
               mSettingsManager.setValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE, "front");
            }
        }
    }

    private boolean isOnCaptureBufferLostHintOn() {
        String value = mSettingsManager.getValue(SettingsManager.KEY_ONCAPTUREBUFFERLOST_HINT);
        return value != null && value.equals("on");
    }

    public boolean isMultiResolutionImageReaderEnabled() {
        if (!PersistUtil.isMultiResolutionImageReaderEnabled()) return false;
        String value = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESIMAGEREADER);
        return value != null && "1".equals(value);
    }

    private boolean isForceAUXOn(CameraMode mode) {
        if (mode == CameraMode.DEFAULT) {
            final SharedPreferences pref = mActivity.getSharedPreferences(
                    ComboPreferences.getLocalSharedPreferencesName(mActivity,
                            mSettingsManager.getNextPrepNameKey(mode)), Context.MODE_PRIVATE);
            String auxValue = pref.getString(SettingsManager.KEY_FORCE_AUX, "off");
            return auxValue != null && auxValue.equals("on");
        }
        return false;
    }

    public void setCameraModeSwitcherAllowed(boolean allow) {
        mCameraModeSwitcherAllowed = allow;
    }

    public boolean getCameraModeSwitcherAllowed() {
        return mCameraModeSwitcherAllowed;
    }
}

class Camera2RequestGapGraphView extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private float   mScale = (float)100;
    private float   mWidth;
    private float   mHeight;
    private CaptureModule mCaptureModule;
    private float scaled;
    private static final int STATS_SIZE = 100;
    private static final String TAG = "Camera2RequestGapGraphView";

    public Camera2RequestGapGraphView(Context context, AttributeSet attrs) {
        super(context,attrs);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if(mCaptureModule == null || (mCaptureModule.mPerformanceDebugEnable != null &&
                mCaptureModule.mPerformanceDebugEnable.equals("off"))) {
            Log.e(TAG, "returning as performance debug is off ");
            return;
        }

        if (mBitmap != null) {
            final Paint paint = mPaint;
            final Canvas cavas = mCanvas;
            final float border = 5;
            float graphheight = mHeight - (2 * border);
            float graphwidth = mWidth - (2 * border);
            float left, top, right, bottom;
            float bargap = 0.0f;
            float barwidth = graphwidth / STATS_SIZE;

            cavas.drawColor(0xFFAAAAAA);
            paint.setColor(Color.BLACK);

            for (int k = 0; k <= (graphheight / 32); k++) {
                float y = (float) (32 * k) + border;
                cavas.drawLine(border, y, graphwidth + border, y, paint);
            }
            for (int j = 0; j <= (graphwidth / 32); j++) {
                float x = (float) (32 * j) + border;
                cavas.drawLine(x, border, x, graphheight + border, paint);
            }
            synchronized(CaptureModule.mPerformanceGapData) {
                for(int i=0 ; i < CaptureModule.mPerformanceGapData.size() ; i++)  {
                    scaled = (CaptureModule.mPerformanceGapData.get(i)/mScale)*graphheight;
                    left = (bargap * (i + 1)) + (barwidth * (i)) + border;
                    top = graphheight + border;
                    right = left + barwidth;
                    bottom = top - scaled;
                    Log.d(TAG,"i:" + i + ",value:" + CaptureModule.mPerformanceGapData.get(i) +
                            ",left:" + left +",top:" +top+",right:" + right +",bottom:" + bottom);
                    cavas.drawRect(left, top, right, bottom, mPaintRect);
                }
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
            paint.setColor(Color.RED);
            paint.setTextSize(30);
            cavas.drawLine(0, border, 3*border, border, paint);
            canvas.drawText(Float.toString(mScale), 3*border, 20, paint);
            cavas.drawLine(0, graphheight/4*3 + border, 3*border, graphheight/4*3 + border, paint);
            canvas.drawText(Float.toString(mScale/4), 3*border, graphheight/4*3 + 15, paint);
            cavas.drawLine(0, graphheight/2 + border, 3*border, graphheight/2 + border, paint);
            canvas.drawText(Float.toString(mScale/2), 3*border, graphheight/2 + 15, paint);
            cavas.drawLine(0, graphheight/4 + border, 3*border, graphheight/4 + border, paint);
            canvas.drawText(Float.toString(mScale/4*3), 3*border, graphheight/4 + 15, paint);
        }
    }
    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }
}

class Camera2RGBGraphView extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private float   mScale = (float)3;
    private float   mWidth;
    private float   mHeight;
    private int mStart, mEnd;
    private CaptureModule mCaptureModule;
    private float scaled;
    private static int STATS_SIZE = 768;
    private static final String TAG = "Camera2RGBGraphView";

    public Camera2RGBGraphView(Context context, AttributeSet attrs) {
        super(context,attrs);

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    void setDataSection(int start, int end){
        mStart =  start;
        mEnd = end;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        String value = mCaptureModule.mSettingsManager.getValue(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        if(value == null || !value.contains("2")) {
            Log.d(TAG, "returning as histogram is off ");
            return;
        }

        if (mBitmap != null) {
            final Paint paint = mPaint;
            final Canvas cavas = mCanvas;
            final float border = 5;
            float graphheight = mHeight - (2 * border);
            float graphwidth = mWidth - (2 * border);
            float left, top, right, bottom;
            float bargap = 0.0f;

            cavas.drawColor(0xFFAAAAAA);
            paint.setColor(Color.BLACK);
            for (int k = 0; k <= (graphheight / 32); k++) {
                float y = (float) (32 * k) + border;
                cavas.drawLine(border, y, graphwidth + border, y, paint);
            }
            for (int j = 0; j <= (graphwidth / 32); j++) {
                float x = (float) (32 * j) + border;
                cavas.drawLine(x, border, x, graphheight + border, paint);
            }
            synchronized(CaptureModule.statsdata) {
                STATS_SIZE = CaptureModule.statsdata.length;
                float barwidth = graphwidth / STATS_SIZE;
                mEnd = CaptureModule.statsdata.length;
                int maxValue = Integer.MIN_VALUE;
                for ( int i = mStart ; i < mEnd ; i++ ) {
                    if ( maxValue < CaptureModule.statsdata[i] ) {
                        maxValue = CaptureModule.statsdata[i];
                    }
                }
                mScale = ( float ) maxValue;
                for(int i=mStart ; i < mEnd ; i++)  {
                    scaled = (CaptureModule.statsdata[i]/mScale)*graphheight;
                    if(scaled >= (float)STATS_SIZE)
                        scaled = (float)STATS_SIZE;
                    left = (bargap * (i - mStart + 1)) + (barwidth * (i - mStart)) + border;
                    top = graphheight + border;
                    right = left + barwidth;
                    bottom = top - scaled;
                    cavas.drawRect(left, top, right, bottom, mPaintRect);
                }
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }
    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }
}

class Camera2GraphView extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private float   mScale = (float)3;
    private float   mWidth;
    private float   mHeight;
    private int mStart, mEnd;
    private CaptureModule mCaptureModule;
    private float scaled;
    private static final int STATS_SIZE = 256;
    private static final String TAG = "GraphView";


    public Camera2GraphView(Context context, AttributeSet attrs) {
        super(context,attrs);

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    void setDataSection(int start, int end){
        mStart =  start;
        mEnd = end;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if(mCaptureModule == null && !mCaptureModule.mHiston) {
            Log.e(TAG, "returning as histogram is off ");
            return;
        }

        if (mBitmap != null) {
            final Paint paint = mPaint;
            final Canvas cavas = mCanvas;
            final float border = 5;
            float graphheight = mHeight - (2 * border);
            float graphwidth = mWidth - (2 * border);
            float left, top, right, bottom;
            float bargap = 0.0f;
            float barwidth = graphwidth / STATS_SIZE;

            cavas.drawColor(0xFFAAAAAA);
            paint.setColor(Color.BLACK);

            for (int k = 0; k <= (graphheight / 32); k++) {
                float y = (float) (32 * k) + border;
                cavas.drawLine(border, y, graphwidth + border, y, paint);
            }
            for (int j = 0; j <= (graphwidth / 32); j++) {
                float x = (float) (32 * j) + border;
                cavas.drawLine(x, border, x, graphheight + border, paint);
            }
            synchronized(CaptureModule.statsdata) {
                int maxValue = Integer.MIN_VALUE;
                for ( int i = mStart ; i < mEnd ; i++ ) {
                    if ( maxValue < CaptureModule.statsdata[i] ) {
                        maxValue = CaptureModule.statsdata[i];
                    }
                }
                mScale = ( float ) maxValue;
                for(int i=mStart ; i < mEnd ; i++)  {
                    scaled = (CaptureModule.statsdata[i]/mScale)*STATS_SIZE;
                    if(scaled >= (float)STATS_SIZE)
                        scaled = (float)STATS_SIZE;
                    left = (bargap * (i - mStart + 1)) + (barwidth * (i - mStart)) + border;
                    top = graphheight + border;
                    right = left + barwidth;
                    bottom = top - scaled;
                    cavas.drawRect(left, top, right, bottom, mPaintRect);
                }
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }
    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }
}

class Camera2BGBitMap extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private Paint   mTextPaint = new Paint();
    private int   mWidth;
    private int   mHeight;
    private CaptureModule mCaptureModule;
    private static final String TAG = "BG GraphView";


    public Camera2BGBitMap(Context context, AttributeSet attrs) {
        super(context,attrs);
        mWidth = CaptureModule.BGSTATS_WIDTH;
        mHeight = CaptureModule.BGSTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mCaptureModule == null && !mCaptureModule.mBGStatson) {
            Log.e(TAG, "returning as BG stats is off");
            return;
        }

        if (mBitmap != null) {
            final Canvas cavas = mCanvas;
            cavas.drawColor(0xFFAAAAAA);
            synchronized(CaptureModule.bg_statsdata){
                mBitmap.setPixels(CaptureModule.bg_statsdata, 0, CaptureModule.BGSTATS_WIDTH,
                        0, 0,CaptureModule.BGSTATS_WIDTH, CaptureModule.BGSTATS_HEIGHT);
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getLayoutParams();
        params.width = mWidth;
        params.height = mHeight;
        setLayoutParams(params);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }

    public void updateViewSize(){
        mWidth = CaptureModule.BGSTATS_WIDTH;
        mHeight = CaptureModule.BGSTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }
}

class Camera2BEBitMap extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private int  mWidth;
    private int  mHeight;
    private CaptureModule mCaptureModule;
    private static final String TAG = "BE GraphView";


    public Camera2BEBitMap(Context context, AttributeSet attrs) {
        super(context,attrs);
        mWidth = CaptureModule.BESTATS_WIDTH;
        mHeight = CaptureModule.BESTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getLayoutParams();
        params.width = mWidth;
        params.height = mHeight;
        setLayoutParams(params);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mCaptureModule == null && !mCaptureModule.mBEStatson) {
            Log.e(TAG, "returning as BE stats is off");
            return;
        }

        if (mBitmap != null) {
            final Canvas cavas = mCanvas;
            cavas.drawColor(0xFFAAAAAA);
            synchronized(CaptureModule.be_statsdata){
            mBitmap.setPixels(CaptureModule.be_statsdata, 0, CaptureModule.BESTATS_WIDTH,
                    0, 0, CaptureModule.BESTATS_WIDTH, CaptureModule.BESTATS_HEIGHT);
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }
    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }

    public void updateViewSize(){
        mWidth = CaptureModule.BESTATS_WIDTH;
        mHeight = CaptureModule.BESTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }
}

class Camera2RSBitMap extends View {
    private Bitmap  mBitmap;
    private Paint   mPaint = new Paint();
    private Paint   mPaintRect = new Paint();
    private Canvas  mCanvas = new Canvas();
    private int  mWidth;
    private int  mHeight;
    private CaptureModule mCaptureModule;
    private static final String TAG = "RS GraphView";


    public Camera2RSBitMap(Context context, AttributeSet attrs) {
        super(context,attrs);
        mWidth = CaptureModule.RSSTATS_WIDTH;
        mHeight = CaptureModule.RSSTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintRect.setColor(0xFFFFFFFF);
        mPaintRect.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getLayoutParams();
        params.width = mWidth;
        params.height = mHeight;
        setLayoutParams(params);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mCaptureModule == null && !mCaptureModule.mRSStatson) {
            Log.e(TAG, "returning as RS stats is off");
            return;
        }

        if (mBitmap != null) {
            final Canvas cavas = mCanvas;
            cavas.drawColor(0xFFAAAAAA);
            synchronized(CaptureModule.rs_statsdata){
            mBitmap.setPixels(CaptureModule.rs_statsdata, 0, CaptureModule.RSSTATS_WIDTH,
                    0, 0, CaptureModule.RSSTATS_WIDTH, CaptureModule.RSSTATS_HEIGHT);
            }
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }
    public void PreviewChanged() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }

    public void updateViewSize(){
        mWidth = CaptureModule.RSSTATS_WIDTH;
        mHeight = CaptureModule.RSSTATS_HEIGHT;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }
}

class DrawAutoHDR2 extends View {

    private static final String TAG = "AutoHdrView";
    private CaptureModule mCaptureModule;

    public DrawAutoHDR2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCaptureModule == null)
            return;
        if (mCaptureModule.mAutoHdrEnable) {
            Paint autoHDRPaint = new Paint();
            autoHDRPaint.setColor(Color.WHITE);
            autoHDRPaint.setAlpha(0);
            canvas.drawPaint(autoHDRPaint);
            autoHDRPaint.setStyle(Paint.Style.STROKE);
            autoHDRPaint.setStrokeWidth(1);
            autoHDRPaint.setTextSize(32);
            autoHDRPaint.setAlpha(255);
            canvas.drawText("HDR On", 200, 100, autoHDRPaint);
        } else {
            super.onDraw(canvas);
            return;
        }
    }

    public void AutoHDR() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }
}

class MFNRDrawer extends View {

    private static final String TAG = "MFNRDrawer";
    private CaptureModule mCaptureModule;

    public MFNRDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCaptureModule == null)
            return;
        if (mCaptureModule.mMFNREnable) {
            Paint mfnrPaint = new Paint();
            mfnrPaint.setColor(Color.WHITE);
            mfnrPaint.setAlpha(0);
            canvas.drawPaint(mfnrPaint);
            mfnrPaint.setStyle(Paint.Style.STROKE);
            mfnrPaint.setStrokeWidth(1);
            mfnrPaint.setTextSize(32);
            mfnrPaint.setAlpha(255);
            canvas.drawText("MFNR", 50, 100, mfnrPaint);
        } else {
            super.onDraw(canvas);
            return;
        }
    }

    public void refleshMFNR() {
        invalidate();
    }

    public void setCaptureModuleObject(CaptureModule captureModule) {
        mCaptureModule = captureModule;
    }
}
abstract class PhysicalImageListener
        implements ImageReader.OnImageAvailableListener {
    private String mCamId;

    public void setCamId(String camId) {
        this.mCamId = camId;
    }
}
