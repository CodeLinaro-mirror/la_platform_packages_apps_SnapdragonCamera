/*
 * Copyright (c) 2016-2017, 2021, The Linux Foundation. All rights reserved.
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
package com.android.camera.multi;

import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.Face;
import android.location.Location;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.android.camera.CameraActivity;
import com.android.camera.CaptureModule;
import com.android.camera.CameraSettings;
import com.android.camera.ComboPreferences;
import com.android.camera.Exif;
import com.android.camera.ExtendedFace;
import com.android.camera.exif.ExifInterface;
import com.android.camera.LocationManager;
import com.android.camera.MediaSaveService;
import com.android.camera.PhotoModule.NamedImages;
import com.android.camera.PhotoModule.NamedImages.NamedEntity;
import com.android.camera.SDCard;
import com.android.camera.SoundClips;
import com.android.camera.Storage;
import com.android.camera.Thumbnail;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PersistUtil;
import com.android.camera.util.SettingTranslation;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.util.VendorTagUtil;

import org.codeaurora.snapcam.R;

public class MultiVideoModule implements MultiCamera, LocationManager.Listener,
        MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private static final String TAG = "SnapCam_MultiVideoModule";
    private static final String FD_TAG = "MultiVideoModule_FD";
    private static final boolean FD_DEBUG = PersistUtil.getFdDebug();
    public static final boolean DEBUG =
            (PersistUtil.getCamera2Debug() == PersistUtil.CAMERA2_DEBUG_DUMP_LOG) ||
                    (PersistUtil.getCamera2Debug() == PersistUtil.CAMERA2_DEBUG_DUMP_ALL);

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final int WAIT_SURFACE = 0;
    private static final int OPEN_CAMERA = 1;
    private static final int CREATE_SESSION = 2;

    private int[] mDisplayRotations = new int[MAX_NUM_CAM];
    private int[] mDisplayOrientations = new int[MAX_NUM_CAM];
    private boolean[] mEisStopMediaRecords = new boolean[MAX_NUM_CAM];

    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int STOP_RECORD_EIS = 6;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private static final int MAX_NUM_CAM = 16;

    /** Add for EIS Configuration */
    private int mStreamConfigOptMode = 0;
    private static final int STREAM_CONFIG_MODE_QTIEIS_REALTIME = 0xF004;
    private static final int STREAM_CONFIG_MODE_QTIEIS_LOOKAHEAD = 0xF008;

    private int mCameraListIndex = 0;
    private int mLastCameraId;

    private Face[] mPreviewFaces = null;
    private Face[] mStickyFaces = null;
    private ExtendedFace[] mExFaces = null;
    private ExtendedFace[] mStickyExFaces = null;
    private Rect[] mCropRegion = new Rect[MAX_NUM_CAM];

    private static final CaptureRequest.Key<Byte> override_resource_cost_validation =
            new CaptureRequest.Key<>(
                    "org.codeaurora.qcamera3.sessionParameters.overrideResourceCostValidation",
                    byte.class);
    //HDRVideo MODE
    private static final CaptureRequest.Key<Integer> hdr_video_mode = new CaptureRequest.Key<>(
            "org.codeaurora.qcamera3.sessionParameters.HDRVideoMode", Integer.class);

    public static final CaptureResult.Key<Byte> result_end_stream =
            new CaptureResult.Key<>("org.quic.camera.recording.endOfStream", byte.class);

    private CameraActivity mActivity;
    private MultiCameraUI mMultiCameraUI;
    private MultiCameraModule mMultiCameraModule;
    private SharedPreferences mLocalSharedPref;
    private ArrayList<CameraCharacteristics> mCharacteristics;
    private CameraDevice[] mCameraDevices = new CameraDevice[MAX_NUM_CAM];
    private CameraCaptureSession[] mCameraPreviewSessions = new CameraCaptureSession[MAX_NUM_CAM];
    private ContentValues[] mCurrentVideoValues = new ContentValues[MAX_NUM_CAM];
    private ImageReader[] mImageReaders = new ImageReader[MAX_NUM_CAM];
    private MediaRecorder[] mMediaRecorders = new MediaRecorder[MAX_NUM_CAM];
    private String[] mNextVideoAbsolutePaths = new String[MAX_NUM_CAM];
    private boolean mPaused = true;

    private NamedImages mNamedImages;

    private Uri mCurrentVideoUri;

    private boolean[] mMediaRecorderPausings = new boolean[MAX_NUM_CAM];

    private boolean mRecordingTimeCountsDown = false;

    private LocationManager mLocationManager;
    private CamcorderProfile mProfile;

    private Size[] mVideoSize = new Size[MAX_NUM_CAM];
    private Size mPreviewSizes[] = new Size[MAX_NUM_CAM];

    private boolean mCaptureTimeLapse = false;
    // Default 0. If it is larger than 0, the camcorder is in time lapse mode.
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;

    private String[] mVideoFilenames = new String[MAX_NUM_CAM];

    private long mRecordingStartTime;
    private long mRecordingTotalTime;

    private ParcelFileDescriptor mVideoFileDescriptor;

    // The video duration limit. 0 means no limit.
    private int mMaxVideoDurationInMs;

    private int mAudioEncoder;
    private String mVideoRotation;

    private SoundClips.Player mSoundPlayer;

    /**
     * Whether the app is recording video now
     */
    private boolean[] mIsRecordingVideos = new boolean[MAX_NUM_CAM];

    private ArrayList<String> mCameraIDList = new ArrayList<>();

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder[] mPreviewRequestBuilders = new CaptureRequest.Builder[MAX_NUM_CAM];

    /**
     * {@link CaptureRequest.Builder} for the camera recording
     */
    private CaptureRequest.Builder[] mRecordRequestBuilders = new CaptureRequest.Builder[MAX_NUM_CAM];

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    private Map<String,SessionConfiguration> mConcurrentConfigurations = new HashMap<>();

    private ContentResolver mContentResolver;
    /**
     * A {@link Semaphore} make sure the camera open callback happens first before closing the
     * camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(3);

    public MultiVideoModule(CameraActivity activity, MultiCameraUI ui, MultiCameraModule module) {
        mActivity = activity;
        mMultiCameraUI = ui;
        mMultiCameraModule = module;
        mContentResolver = mActivity.getContentResolver();
        mLocationManager = new LocationManager(mActivity, this);
        mNamedImages = new NamedImages();
        mLocalSharedPref = mActivity.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(mActivity,
                        "multi" + mMultiCameraModule.getCurrenCameraMode()), Context.MODE_PRIVATE);
        startBackgroundThread();
        initializeCameraCharacteristics();
    }

    @Override
    public void onResume() {
        // Set up sound playback for video record and video stop
        if (mSoundPlayer == null) {
            mSoundPlayer = SoundClips.getPlayer(mActivity);
        }
        mPaused = false;
        initializeValues();
        startBackgroundThread();
        Set<String> concurrentIds = null;

        if (mLocalSharedPref != null){
            concurrentIds =
                    mLocalSharedPref.getStringSet(MultiSettingsActivity.KEY_CONCURRENT_CAMERA,null);
        }
        if (concurrentIds != null && concurrentIds.size() > 0){
            for (String id : concurrentIds){
                Log.d(TAG, " onResume openCamera id="+id);
                mCameraIDList.add(id);
                setDisplayOrientation(Integer.parseInt(id));
            }
        } else {
            Log.d(TAG, " onResume openCamera default 0");
            mCameraIDList.add("0");
            setDisplayOrientation(0);
        }

        mMultiCameraUI.hideSurfaceView();

        for (String cameraId : mCameraIDList){
            int id = Integer.valueOf(cameraId);
            updateVideoSize(id);
            cropRegionForZoom(id);
            int index = mCameraIDList.indexOf(cameraId);
            if (index != -1) {
                mMultiCameraUI.setPreviewSize(index, mPreviewSizes[id].getWidth(),
                        mPreviewSizes[id].getHeight());
            } else {
                mMultiCameraUI.setPreviewSize(index, mPreviewSizes[0].getWidth(),
                        mPreviewSizes[0].getHeight());
            }
        }
    }

    @Override
    public void onPause() {
        mPaused = true;
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
            mSoundPlayer = null;
        }
        for (String id : mCameraIDList) {
            int cameraId = Integer.parseInt(id);
            Log.d(TAG, " onPause id :" + cameraId + "  recording is :" +
                    (mIsRecordingVideos[cameraId] ? "STOPED" : "START"));
            if (mIsRecordingVideos[cameraId]) {
                stopRecordingVideo(cameraId);
            }
        }
        closeCamera();
        if (mCameraIDList != null) {
            mCameraIDList.clear();
        }
        mCameraListIndex = 0;
        stopBackgroundThread();
    }

    @Override
    public boolean openCamera() {
        Message msg = Message.obtain();
        msg.what = OPEN_CAMERA;
        if (mCameraHandler != null) {
            mCameraHandler.sendMessage(msg);
        }
        return true;
    }

    @Override
    public String[] getCameraIdList() {
        return (String[])mCameraIDList.toArray(new String[0]);
    }

    @Override
    public void startPreview() {

    }

    @Override
    public void closeSession() {

    }

    @Override
    public void closeCamera() {
        Log.d(TAG, "closeCamera");
        /* no need to set this in the callback and handle asynchronously. This is the same
        reason as why we release the semaphore here, not in camera close callback function
        as we don't have to protect the case where camera open() gets called during camera
        close(). The low level framework/HAL handles the synchronization for open()
        happens after close() */
        try {
            // Close camera starting with AUX first
            for (int i = MAX_NUM_CAM - 1; i >= 0; i--) {
                if (null != mCameraDevices[i]) {
                    if (!mCameraOpenCloseLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                        Log.d(TAG, "Time out waiting to lock camera closing.");
                        throw new RuntimeException("Time out waiting to lock camera closing");
                    }
                    Log.d(TAG, "Closing camera: " + mCameraDevices[i].getId());
                    mCameraDevices[i].close();
                    mCameraDevices[i] = null;
                    mCameraPreviewSessions[i] = null;
                }
                if (null != mImageReaders[i]) {
                    mImageReaders[i].close();
                    mImageReaders[i] = null;
                }
                if (mConcurrentConfigurations != null) {
                    mConcurrentConfigurations.clear();
                }
            }
        } catch (InterruptedException e) {
            mCameraOpenCloseLock.release();
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    public void onVideoButtonClick(String[] ids) {
        checkAndPlayShutterSound(mIsRecordingVideos[0]);
        for (String id : ids) {
            int cameraId = Integer.parseInt(id);
            Log.d(TAG, " onVideoButtonClick id :" + cameraId + "  recording is :" +
                    (mIsRecordingVideos[cameraId] ? "STOPED" : "START"));
            if (mIsRecordingVideos[cameraId]) {
                stopRecordingVideo(cameraId);
                if (cameraId == mLastCameraId) {
                    mMultiCameraUI.showModeSelectLayout(true);
                    mMultiCameraModule.setCameraModeSwitcherAllowed(true);
                }
            } else {
                mMultiCameraUI.showModeSelectLayout(false);
                mMultiCameraModule.setCameraModeSwitcherAllowed(false);
                startRecordingVideo(cameraId);
            }
        }
    }

    @Override
    public void onShutterButtonClick(String[] ids) {
        checkAndPlayCaptureSound();
        mMultiCameraUI.enableShutter(false);
        for (String id : ids) {
            Log.d(TAG, "onShutterButtonClick id :" + id);
            try {
                int cameraId = Integer.parseInt(id);
                if (null == mActivity || null == mCameraDevices[cameraId]) {
                    warningToast("Camera is not ready yet to take a video snapshot.");
                    return;
                }
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevices[cameraId].createCaptureRequest(
                                CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
                captureBuilder.addTarget(mImageReaders[cameraId].getSurface());
                int index = mCameraIDList.indexOf(String.valueOf(cameraId));
                captureBuilder.addTarget(mMultiCameraUI.getSurfaceViewList().get(
                        index).getHolder().getSurface());
                // Use the same AE and AF modes as the preview.
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY, (byte) 80);
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                applyVideoEIS(captureBuilder);
                applyFaceDetection(captureBuilder);

                // Orientation
                int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                        CameraUtil.getJpegRotation(cameraId, rotation));
                mCameraPreviewSessions[cameraId].capture(captureBuilder.build(),
                        mCaptureStillCallback, mMultiCameraModule.getMyCameraHandler());
                Log.d(TAG, " cameraCaptureSession" + id + " captured ");
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onButtonPause(String[] ids) {
        mRecordingTotalTime += SystemClock.uptimeMillis() - mRecordingStartTime;
        String defaultValue = mActivity.getString(R.string.pref_camera2_eis_default);
        String value = mLocalSharedPref.getString(MultiSettingsActivity.KEY_VIDEO_EIS, defaultValue);
        boolean noNeedEndofStreamWhenPause = value != null && value.equals("V3");
        for (String id : ids) {
            int cameraId = Integer.parseInt(id);
            mMediaRecorderPausings[cameraId] = true;
            if (noNeedEndofStreamWhenPause) {
                mMediaRecorders[cameraId].pause();
            } else {
                setEndOfStream(cameraId,false, false);
            }
        }
    }

    @Override
    public void onButtonContinue(String[] ids) {
        for (String id : ids) {
            int cameraId = Integer.parseInt(id);
            mMediaRecorderPausings[cameraId] = false;
            mMediaRecorders[cameraId].resume();
            mRecordingStartTime = SystemClock.uptimeMillis();
            updateRecordingTime(cameraId);
            setEndOfStream(cameraId,true, false);
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
        String[] ids = getCameraIdList();
        for (String id : ids) {
            int cameraId = Integer.parseInt(id);
            stopRecordingVideo(cameraId);
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
        String[] ids = getCameraIdList();
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            for (String id : ids) {
                int cameraId = Integer.parseInt(id);
                if (mIsRecordingVideos[cameraId]) {
                    stopRecordingVideo(cameraId);
                }
            }
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            for (String id : ids) {
                int cameraId = Integer.parseInt(id);
                if (mIsRecordingVideos[cameraId]) {
                    stopRecordingVideo(cameraId);
                }
            }
            // Show the toast.
            RotateTextToast.makeText(mActivity, R.string.video_reach_size_limit,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onOrientationChanged(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public boolean isRecordingVideo() {
        for (int i = 0; i < mIsRecordingVideos.length; i++) {
            if (mIsRecordingVideos[i]) return true;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.v(TAG, "onConfigurationChanged");
        String[] cameraIds = getCameraIdList();
        if (cameraIds != null) {
            for (String id : cameraIds) {
                int cameraId = Integer.parseInt(id);
                setDisplayOrientation(cameraId);
            }
        }
    }

    private void updateFaceView(final Face[] faces, final ExtendedFace[] extendedFaces,
                                final int index) {
        mPreviewFaces = faces;
        mExFaces = extendedFaces;
        if (faces != null) {
            if (faces.length != 0) {
                if (FD_DEBUG){
                    for (int i = 0; i < faces.length; i++){
                        if (faces[i] != null){
                            Log.d(FD_TAG,"face i="+i+" ROI="+faces[i].getBounds().toString());
                        }
                    }
                }
                mStickyFaces = faces;
                mStickyExFaces = extendedFaces;
            }
            mMultiCameraModule.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    mMultiCameraUI.onFaceDetection(faces, extendedFaces, index);
                }
            });
        }
    }

    private void updateFaceDetection(int id) {
        boolean faceDetection = mLocalSharedPref.getBoolean(
                MultiSettingsActivity.KEY_MULTI_FACE_DETECTION, false);
        Log.v(TAG, " updateFaceDetection faceDetection :" + faceDetection);
        int index = mCameraIDList.indexOf(String.valueOf(id));

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (faceDetection)
                    mMultiCameraUI.onStartFaceDetection(index, mDisplayOrientations[index],
                            isFacingFront(id), mCropRegion[id], mCropRegion[id]);
                else {
                    mMultiCameraUI.onStopFaceDetection();
                }
            }
        });
    }

    private void setDisplayOrientation(int id) {
        int index = mCameraIDList.indexOf(String.valueOf(id));
        mDisplayRotations[index] = CameraUtil.getDisplayRotation(mActivity);
        mDisplayOrientations[index] = CameraUtil.getDisplayOrientationForCamera2(
                mDisplayRotations[index], id);
    }

    private boolean isFacingFront(int id) {
        int facing = mCharacteristics.get(id).get(CameraCharacteristics.LENS_FACING);
        return facing == CameraCharacteristics.LENS_FACING_FRONT;
    }

    private Rect cropRegionForZoom(int id) {
        if (DEBUG) {
            Log.d(TAG, "cropRegionForZoom " + id);
        }
        Rect activeRegion = mCharacteristics.get(id).get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Rect cropRegion = new Rect();

        int xCenter = activeRegion.width() / 2;
        int yCenter = activeRegion.height() / 2;
        int xDelta = (int) (activeRegion.width() / (2 * 1.0f));
        int yDelta = (int) (activeRegion.height() / (2 * 1.0f));
        cropRegion.set(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
        Log.d(TAG, "cropRegionForZoom  mCropRegion[id] " +  mCropRegion[id]);
        mCropRegion[id] = cropRegion;
        return mCropRegion[id];
    }

    private void openCameraInSequence(String id) {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(id);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Log.d(TAG, "openCameraInSequence " + id + ", mSensorOrientation :" + mSensorOrientation);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                Log.d(TAG, "Time out waiting to lock camera opening.");
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            manager.openCamera(id, mStateCallback, mMultiCameraModule.getMyCameraHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        manager = null;
        characteristics = null;
    }

    private void initializeCameraCharacteristics() {
        mCharacteristics = new ArrayList<>();
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            Log.d(TAG, "cameraIdList size =" + cameraIdList.length);
            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);
                mCharacteristics.add(i, characteristics);
                int[] maxPreviewSize = null;
                try {
                    maxPreviewSize = characteristics.get(CaptureModule.max_preview_size);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "getMaxPreviewSize no vendorTag max_preview_size:");
                }
                if (maxPreviewSize != null) {
                    Log.d(TAG, " init cameraId :" + cameraId + ", i :" + i +
                            ", maxPreviewSize :" + maxPreviewSize[0]+ "x" + maxPreviewSize[1]);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("CameraBackground");
            mCameraThread.start();
        }
        if (mCameraHandler == null) {
            mCameraHandler = new MyCameraHandler(mCameraThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyCameraHandler extends Handler {

        public MyCameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WAIT_SURFACE:
                    int id = msg.arg1;
                    int index = mCameraIDList.indexOf(String.valueOf(id));
                    Log.v(TAG, "WAIT_SURFACE id :" + id + ", index :" + index);
                    if (index == -1) {
                        break;
                    }
                    Surface surface = mMultiCameraUI.getSurfaceViewList().get(index)
                            .getHolder().getSurface();
                    if (surface.isValid()) {
                        SessionConfiguration sessionConfiguration =
                                prepareCaptureSessions(id,surface);
                        Log.d(TAG, "prepareCaptureSessions id= "+id);
                        if (sessionConfiguration != null) {
                            mConcurrentConfigurations.put(String.valueOf(id),sessionConfiguration);
                        }
                        Message message = Message.obtain();
                        message.what = OPEN_CAMERA;
                        sendMessage(message);
                    } else {
                        Message message = new Message();
                        message.what = WAIT_SURFACE;
                        message.arg1 = id;
                        mCameraHandler.sendMessageDelayed(message, 200);
                        Log.v(TAG, "Surface is invalid, wait more 200ms surfaceCreated");
                    }
                    break;
                case OPEN_CAMERA:
                    if (mCameraListIndex == mCameraIDList.size()) {
                        mCameraListIndex = 0;
                        Message message = Message.obtain();
                        message.what = CREATE_SESSION;
                        sendMessage(message);
                        Log.d(TAG, "CREATE_SESSION");
                    } else {
                        String cameraId = mCameraIDList.get(mCameraListIndex);
                        openCameraInSequence(cameraId);
                        mCameraListIndex ++;
                        Log.v(TAG, " OPEN_CAMERA cameraId :" + cameraId + ", mCameraListIndex :"
                                + mCameraListIndex);
                    }
                    break;
                case CREATE_SESSION:
                    if (mConcurrentConfigurations != null) {
                        boolean createSession = true;
                        if (mCameraIDList != null){
                            for (String cameraId : mCameraIDList){
                                createSession = createSession &&
                                        mConcurrentConfigurations.containsKey(cameraId);
                            }
                        }
                        if (mPaused) {
                            return;
                        }
                        boolean supported =
                                mMultiCameraModule.checkConcurrentSessionConfigurationSupported(mConcurrentConfigurations);
                        Log.v(TAG, " CREATE_SESSION createSession :" + createSession +
                                " supported :" + supported + ", mCameraIDList :" + mCameraIDList);
                        if (createSession && supported) {
                            try{
                                for (String cameraId : mCameraIDList){
                                    mCameraDevices[Integer.valueOf(cameraId)].createCaptureSession(
                                            mConcurrentConfigurations.get(cameraId));
                                    Log.v(TAG, " CREATE_SESSION call createCaptureSession cameraId :" + cameraId);
                                }
                            } catch (CameraAccessException | IllegalArgumentException e){
                                e.printStackTrace();
                            }
                        } else {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mActivity.getApplicationContext(),
                                            R.string.pref_camera2_concurrent_session_not_support,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    break;
                case STOP_RECORD_EIS:
                    int cameraId = msg.arg1;
                    mMultiCameraModule.getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            keepScreenOnAwhile();
                            mMultiCameraUI.enableVideo(true);
                        }
                    });
                    stopMediaRecordAndSaveFile(cameraId);
                    break;
            }
        }
    }

    private void createVideoSnapshotImageReader(int id) {
        if (mImageReaders[id] != null) {
            mImageReaders[id].close();
        }
        mImageReaders[id] = ImageReader.newInstance(1920, 1080,
                ImageFormat.JPEG, /*maxImages*/2);
        mImageReaders[id].setOnImageAvailableListener(
                mOnImageAvailableListener, mMultiCameraModule.getMyCameraHandler());
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            if (mPaused) {
                return;
            }
            int id = Integer.parseInt(cameraDevice.getId());
            mCameraDevices[id] = cameraDevice;
            Log.d(TAG, "onOpened " + id);
            mCameraOpenCloseLock.release();
            createCameraPreviewSession(id, false);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMultiCameraUI.onCameraOpened(id);
                }
            });
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.d(TAG, "onDisconnected " + id);
            mCameraOpenCloseLock.release();
            mCameraDevices[id] = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.e(TAG, "onError " + id + " " + error);
            mCameraOpenCloseLock.release();

            if (null != mActivity) {
                Toast.makeText(mActivity,"open camera error id =" + id,
                        Toast.LENGTH_LONG).show();
                mActivity.finish();
            }
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            int id = Integer.parseInt(cameraDevice.getId());
            Log.d(TAG, "onClosed " + id);
            mCameraOpenCloseLock.release();
            mCameraDevices[id] = null;
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureStillCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            Log.d(TAG, " mCaptureCallback onCaptureCompleted ");
            mMultiCameraModule.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, " enable Shutter " );
                    mMultiCameraUI.enableShutter(true);
                }
            });
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request,
                                    CaptureFailure result) {
            Log.d(TAG, " mCaptureCallback onCaptureFailed  " );
        }


        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int
                sequenceId, long frameNumber) {
            Log.d(TAG, " mCaptureCallback onCaptureSequenceCompleted ");
        }
    };

    private MediaSaveService.OnMediaSavedListener mOnMediaSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
                        mActivity.notifyNewMedia(uri);
                    }
                }
            };

    public void enableRecordingLocation(boolean enable) {
        mLocationManager.recordLocation(enable);
    }

    private Size parsePictureSize(String value) {
        int indexX = value.indexOf('x');
        int width = Integer.parseInt(value.substring(0, indexX));
        int height = Integer.parseInt(value.substring(indexX + 1));
        return new Size(width, height);
    }

    private void updateVideoSize(int id) {
        String defaultSize = mActivity.getString(R.string.pref_multi_camera_video_quality_default);
        String videoSize = mLocalSharedPref.getString(
                MultiSettingsActivity.KEY_VIDEO_SIZE_ + id, defaultSize);
        mVideoSize[id] = parsePictureSize(videoSize);
        Log.v(TAG, " updateVideoSize id :" + id + ", size :" + mVideoSize[id].getWidth() +
                "x" + mVideoSize[id].getHeight());
        mPreviewSizes[id] = getOptimalVideoPreviewSize(id, mVideoSize[id]);
    }

    private Size getOptimalVideoPreviewSize(int id, Size VideoSize) {
        double targetRatio = (double) VideoSize.getWidth() / VideoSize.getHeight();
        final double ratio_1_1 = (double)1/1;
        final double ratio_4_3 = (double)4/3;
        final double ratio_16_9 = (double)16/9;
        Size previewSize = null;
        Log.v(TAG, "getOptimalPreviewSize (targetRatio == ratio_1_1) " + (targetRatio == ratio_1_1) +
                " , (targetRatio == ratio_4_3): " + (targetRatio == ratio_4_3) + ", (targetRatio == ratio_16_9) :" + (targetRatio == ratio_16_9));
        if (targetRatio == ratio_1_1) {
            previewSize = new Size(MultiSettingsActivity.PREVIEW_WIDTH,
                    MultiSettingsActivity.PREVIEW_HIEGHT_1_1);
        } else if (targetRatio == ratio_4_3) {
            previewSize = new Size(MultiSettingsActivity.PREVIEW_WIDTH_4_3,
                    MultiSettingsActivity.PREVIEW_HIEGHT_4_3);
        } else if (targetRatio == ratio_16_9) {
            previewSize = new Size(MultiSettingsActivity.PREVIEW_WIDTH_16_9,
                    MultiSettingsActivity.PREVIEW_HIEGHT_16_9);
        } else {
            previewSize = new Size(MultiSettingsActivity.PREVIEW_WIDTH,
                    MultiSettingsActivity.PREVIEW_HIEGHT_4_3);
        }
        Log.v(TAG, "getOptimalVideoPreviewSize previewSize " + previewSize.getWidth() + " x " + previewSize.getHeight());
        return previewSize;
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession(int id, boolean stopRecording) {
        // This is the output Surface we need to start preview.
        int index = mCameraIDList.indexOf(String.valueOf(id));
        Log.v(TAG, "createCameraPreviewSession id :" + id + ", index :" + index + ", mCameraIDList :" + mCameraIDList);
        Message message = Message.obtain();
        if (stopRecording) {
            // no need open camera again after stop recording
            message.what = CREATE_SESSION;
        } else {
            Surface surface = mMultiCameraUI.getSurfaceViewList().get(index).getHolder().getSurface();
            if (surface.isValid()) {
                SessionConfiguration sessionConfiguration = prepareCaptureSessions(id, surface);
                if (sessionConfiguration != null) {
                    mConcurrentConfigurations.put(String.valueOf(id), sessionConfiguration);
                }
                message.what = OPEN_CAMERA;
            } else {
                message.what = WAIT_SURFACE;
                message.arg1 = id;
            }
        }

        if (mCameraHandler != null) {
            if (message.what == WAIT_SURFACE) {
                Log.v(TAG, "Surface is invalid, wait surfaceCreated 200ms");
                mCameraHandler.sendMessageDelayed(message, 200);
            } else {
                mCameraHandler.sendMessage(message);
            }
        }
    }

    private SessionConfiguration prepareCaptureSessions(int id, Surface surface) {
        SessionConfiguration sessionConfiguration = null;
        Log.v(TAG, "prepareCaptureSessions id :" + id + " mCameraDevices[id] :" + mCameraDevices[id]);
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilders[id]
                    = mCameraDevices[id].createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilders[id].addTarget(surface);
            mPreviewRequestBuilders[id].setTag(id);

            CameraCaptureSession.StateCallback stateCallback =
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevices[id]) {
                                return;
                            }
                            Log.v(TAG, " mPreviewRequestBuilders onConfigured id :" + id);
                            // When the session is ready, we start displaying the preview.
                            mCameraPreviewSessions[id] = cameraCaptureSession;
                            applyFaceDetection(mPreviewRequestBuilders[id]);
                            updateFaceDetection(id);
                            setDisplayOrientation(id);
                            applyVideoEIS(mPreviewRequestBuilders[id]);
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilders[id].set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Finally, we start displaying the camera preview.
                                mPreviewRequestBuilders[id].setTag(id);
                                mCameraPreviewSessions[id].setRepeatingRequest(
                                        mPreviewRequestBuilders[id].build(),
                                        mCaptureCallback, mMultiCameraModule.getMyCameraHandler());
                            } catch (CameraAccessException | IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            showToast("onConfigureFailed");
                        }
                    };

            try {
                final byte enable = 1;
                mPreviewRequestBuilders[id].set(override_resource_cost_validation, enable);
                Log.v(TAG, " set" + override_resource_cost_validation + " is 1");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            List<OutputConfiguration> outConfigurations = new ArrayList<>(1);
            outConfigurations.add(new OutputConfiguration(surface));

            sessionConfiguration = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR | mStreamConfigOptMode, outConfigurations,
                    new HandlerExecutor(mCameraHandler), stateCallback);
            //applyVideoEncoderProfile(mPreviewRequestBuilders[id], id);
            sessionConfiguration.setSessionParameters(mPreviewRequestBuilders[id].build());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return sessionConfiguration;
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

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (DEBUG) {
                Log.v(TAG, "process afState :" + afState + ", aeState :" + aeState);
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);

            int id = (int) partialResult.getRequest().getTag();
            int index = mCameraIDList.indexOf(String.valueOf(id));
            Log.d(FD_TAG, "onCaptureProgressed id = " + id + ", index :" + index);
            Face[] faces = partialResult.get(CaptureResult.STATISTICS_FACES);
            if (FD_DEBUG)
                Log.d(FD_TAG,"onCaptureProgressed Detected Face size = " + Integer.toString(faces == null? 0 : faces.length));
            if (faces != null){
                updateFaceView(faces, null, index);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);

            int id = (int) result.getRequest().getTag();
            int index = mCameraIDList.indexOf(String.valueOf(id));
            Log.d(FD_TAG, "onCaptureCompleted id = " + id + ", index :" + index);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            if (FD_DEBUG)
                Log.d(FD_TAG, "onCaptureCompleted Detected Face size = " + Integer.toString(faces == null ? 0 : faces.length));
            if (faces != null) {
                updateFaceView(faces, null, index);
            }
            waitEISAndStopMediaRecorder(id, result);
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.v(TAG, "onImageAvailable ...");
            Image image = reader.acquireNextImage();
            long imageTime = System.currentTimeMillis();
            mNamedImages.nameNewImage(imageTime);
            NamedEntity name = mNamedImages.getNextNameEntity();
            String title = (name == null) ? null : name.title;
            long date = (name == null) ? -1 : name.date;

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            int orientation = 0;
            ExifInterface exif = Exif.getExif(bytes);
            orientation = Exif.getOrientation(exif);
            String saveFormat = "jpeg";
            mActivity.getMediaSaveService().addImage(bytes, title, date,
                    null, image.getWidth(), image.getHeight(), orientation, exif,
                    mOnMediaSavedListener, mContentResolver, saveFormat);
            mActivity.updateThumbnail(bytes);
            image.close();
            mMultiCameraModule.updateTakingPicture();
        }
    };

    private void initializeValues() {
        updateMaxVideoDuration();
        updateAudioEncoder();
        updateVideoRotation();
    }

    private void checkAndPlayShutterSound(boolean isStarted) {
        if (mSoundPlayer != null) {
            mSoundPlayer.play(isStarted? SoundClips.STOP_VIDEO_RECORDING
                    : SoundClips.START_VIDEO_RECORDING);
        }
    }

    private void checkAndPlayCaptureSound() {
        if (mSoundPlayer != null) {
            mSoundPlayer.play(SoundClips.SHUTTER_CLICK);
        }
    }

    private void closePreviewSession(int id) {
        if (mCameraPreviewSessions[id] != null) {
            Log.v(TAG, "closePreviewSession id :" + id);
            mCameraPreviewSessions[id].close();
            mCameraPreviewSessions[id] = null;
        }
    }

    private void startRecordingVideo(final int id) {
        final int index = mCameraIDList.indexOf(String.valueOf(id));
        if (null == mCameraDevices[id] ||
                !mMultiCameraUI.getSurfaceViewList().get(index).isEnabled()) {
            return;
        }
        Log.v(TAG, " startRecordingVideo " + id);
        try {
            closePreviewSession(id);
            setUpMediaRecorder(id);
            createVideoSnapshotImageReader(id);
            mRecordRequestBuilders[id] = mCameraDevices[id].createCaptureRequest(
                    CameraDevice.TEMPLATE_RECORD);
            if (true) {
                mRecordRequestBuilders[id].set(CaptureRequest.NOISE_REDUCTION_MODE,
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST);
            } else {
                mRecordRequestBuilders[id].set(CaptureRequest.NOISE_REDUCTION_MODE,
                        CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
            }
            applyVideoEIS(mRecordRequestBuilders[id]);
            applyFaceDetection(mRecordRequestBuilders[id]);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = mMultiCameraUI.getSurfaceViewList().get(index).getHolder()
                    .getSurface();
            surfaces.add(previewSurface);
            mRecordRequestBuilders[id].addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorders[id].getSurface();
            surfaces.add(recorderSurface);
            mRecordRequestBuilders[id].addTarget(recorderSurface);
            surfaces.add(mImageReaders[id].getSurface());

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevices[id].createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mCameraPreviewSessions[id] = cameraCaptureSession;
                    updateRecordingPreview(id);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIsRecordingVideos[id] = true;
                            // Start recording
                            updateFaceDetection(id);
                            setDisplayOrientation(id);
                            mMediaRecorders[id].start();
                            requestAudioFocus();
                            mRecordingTotalTime = 0L;
                            mRecordingStartTime = SystemClock.uptimeMillis();
                            mMediaRecorderPausings[id] = false;
                            mMultiCameraUI.resetPauseButton();
                            mMultiCameraUI.showRecordingUI(true);
                            updateRecordingTime(id);
                            mEisStopMediaRecords[index] = true;
                            Log.v(TAG, " startRecordingVideo done " + id);
                        }
                    });
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    if (null != mActivity) {
                        Toast.makeText(mActivity, "Configure Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mMultiCameraModule.getMyCameraHandler());
        } catch (CameraAccessException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingVideo(int id) {
        mIsRecordingVideos[id] = false;
        Log.v(TAG, " stopRecordingVideo " + id);
        try {
            if (PersistUtil.needEndOfStream() && !isVideoEISDisable()) {
                setEndOfStream(id, false, true);
            }
            if (isVideoEISDisable()) {
                mMediaRecorders[id].setOnErrorListener(null);
                mMediaRecorders[id].setOnInfoListener(null);
                // Stop recording
                mMediaRecorders[id].stop();
                mMediaRecorders[id].reset();
                saveVideo(id);

                // release media recorder
                releaseMediaRecorder(id);
                releaseAudioFocus();
            } else {
                mMultiCameraUI.enableVideo(false);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "MediaRecoder stop fail", e);
            if (mVideoFilenames[id] != null) deleteVideoFile(mVideoFilenames[id]);
        }

        mMultiCameraUI.showRecordingUI(false);
        if (null != mActivity) {
            Toast.makeText(mActivity, "Video saved: " + mNextVideoAbsolutePaths[id],
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePaths[id]);
        }
        mNextVideoAbsolutePaths[id] = null;
        if(!mPaused && isVideoEISDisable()) {
            createCameraPreviewSession(id, true);
        }
    }

    private void stopMediaRecordAndSaveFile(int id) {
        try {
            mMediaRecorders[id].setOnErrorListener(null);
            mMediaRecorders[id].setOnInfoListener(null);
            // Stop recording
            mMediaRecorders[id].stop();
            mMediaRecorders[id].reset();
            saveVideo(id);

            mMultiCameraModule.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    keepScreenOnAwhile();
                }
            });
            // release media recorder
            releaseMediaRecorder(id);
            releaseAudioFocus();
        } catch (RuntimeException e) {
            Log.w(TAG, "MediaRecoder stop fail", e);
            if (mVideoFilenames[id] != null) deleteVideoFile(mVideoFilenames[id]);
        }
        if(!mPaused) {
            createCameraPreviewSession(id, true);
        }
    }

    private final MediaSaveService.OnMediaSavedListener mOnVideoSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
                        mActivity.notifyNewMedia(uri);
                        mCurrentVideoUri = uri;
                    }
                }
    };

    private void waitEISAndStopMediaRecorder(int id, CaptureResult result) {
        boolean isEISV3Disable = isVideoEISDisable();
        if (!isEISV3Disable) {
            byte eisEndStream = 0;
            try {
                eisEndStream = result.get(result_end_stream);
            } catch(IllegalArgumentException e) {
                Log.e(TAG, " no vendorTag result_end_stream :" + result_end_stream);
            }
            if (DEBUG) {
                Log.v(TAG, " waitEISAndStopMediaRecorder eisEndStream :" + eisEndStream + ", id :" + id);
            }
            if (eisEndStream == 1) {
                int index = mCameraIDList.indexOf(String.valueOf(id));
                if (index != -1 && mEisStopMediaRecords[index]) {
                    Log.v(TAG, " waitEISAndStopMediaRecorder send message STOP_RECORD_EIS");
                    mEisStopMediaRecords[index] = false;
                    Message message = Message.obtain();
                    message.what = STOP_RECORD_EIS;
                    message.arg1 = id;
                    mCameraHandler.sendMessage(message);
                }
            }
        }
    }

    private void keepScreenOnAwhile() {
        mMultiCameraModule.getMainHandler().removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMultiCameraModule.getMainHandler().sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private boolean isVideoEISDisable() {
        boolean result = true;
        String defaultValue = mActivity.getString(R.string.pref_camera2_eis_default);
        String value = mLocalSharedPref.getString(MultiSettingsActivity.KEY_VIDEO_EIS, defaultValue);
        result = value != null && value.equals("disable");
        Log.d(TAG, "isVideoEISDisable " + result);
        return result;
    }

    private void setEndOfStream(int id, boolean isResume, boolean isStopRecord) {
        CaptureRequest.Builder captureRequestBuilder = mRecordRequestBuilders[id];
        captureRequestBuilder.setTag(id);
        try {
            if (isResume) {
                try {
                    captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                    Log.d(TAG, "Set camera id " + id + " endofstream TAG to 0 on Resume");
                    mCameraPreviewSessions[id].setRepeatingRequest(captureRequestBuilder.build(),
                            mCaptureCallback, mCameraHandler);
                } catch(IllegalArgumentException e) {
                    Log.w(TAG, "can not find vendor tag: org.quic.camera.recording.endOfStream");
                }
            } else {
                if ((mMediaRecorderPausings[id] || !mIsRecordingVideos[id]) && (mCameraPreviewSessions[id] != null)) {
                    mCameraPreviewSessions[id].stopRepeating();
                    try {
                        captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x01);
                        Log.d(TAG, "Set camera id " + id + " endofstream TAG to 1");
                    } catch (IllegalArgumentException illegalArgumentException) {
                        Log.w(TAG, "can not find vendor tag: org.quic.camera.recording.endOfStream");
                    }
                    mCameraPreviewSessions[id].capture(
                            captureRequestBuilder.build(), mCaptureCallback, mCameraHandler);
                    Log.d(TAG, "Set camera id " + id + " endofstream TAG is done from APP");
                    captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                }
                if (!isStopRecord) {
                    //is pause record
                    mMediaRecorders[id].pause();
                }
                captureRequestBuilder = mPreviewRequestBuilders[id];
                captureRequestBuilder.setTag(id);
                if (!isVideoEISDisable() && isStopRecord) {
                    captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x01);
                } else {
                    captureRequestBuilder.set(CaptureModule.recording_end_stream, (byte) 0x00);
                }
                Log.d(TAG, "Set camera id " + id + " setRepeatingRequest endofstream TAG done");
                mCameraPreviewSessions[id].setRepeatingRequest(captureRequestBuilder.build(),
                        mCaptureCallback, mCameraHandler);
            }
        } catch (CameraAccessException | IllegalStateException | NullPointerException |
                IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void releaseMediaRecorder(int id) {
        Log.v(TAG, "Releasing media recorder.");
        cleanupEmptyFile(id);
        if (mMediaRecorders[id] != null) {
            try{
                mMediaRecorders[id].reset();
                mMediaRecorders[id].release();
            }catch (RuntimeException e) {
                e.printStackTrace();
            }
            mMediaRecorders[id] = null;
        }
    }

    /*
     * Make sure we're not recording music playing in the background, ask the
     * MediaPlaybackService to pause playback.
     */
    private void requestAudioFocus() {
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
        AudioManager am = (AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);
        int result = am.abandonAudioFocus(null);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.v(TAG, "Audio focus release failed");
        }
    }

    private void cleanupEmptyFile(int id) {
        if (mVideoFilenames[id] != null) {
            File f = new File(mVideoFilenames[id]);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + mVideoFilenames[id]);
                mVideoFilenames[id] = null;
            }
        }
    }

    private void updateMaxVideoDuration() {
        String defaultValue = mActivity.getResources().getString(
                R.string.pref_camera_video_duration_default);
        String minutesStr = mLocalSharedPref.getString(MultiSettingsActivity.KEY_VIDEO_DURATION,
                defaultValue);
        int minutes = Integer.parseInt(minutesStr);
        if (minutes == -1) {
            // User wants lowest, set 30s */
            mMaxVideoDurationInMs = 30000;
        } else {
            // 1 minute = 60000ms
            mMaxVideoDurationInMs = 60000 * minutes;
        }
    }

    private void updateAudioEncoder() {
        String audioEncoderStr = mActivity.getResources().getString(
                R.string.pref_camera_audioencoder_default);
        if (mLocalSharedPref != null) {
            audioEncoderStr = mLocalSharedPref.getString(MultiSettingsActivity.KEY_AUDIO_ENCODER,
                    audioEncoderStr);
        }
        mAudioEncoder = SettingTranslation.getAudioEncoder(audioEncoderStr);
    }

    private void updateVideoRotation() {
        String defaultValue = mActivity.getResources().getString(
                R.string.pref_camera_video_rotation_default);
        if (mLocalSharedPref != null) {
            mVideoRotation = mLocalSharedPref.getString(MultiSettingsActivity.KEY_VIDEO_ROTATION,
                    defaultValue);
        }
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    private void saveVideo(int id) {
        File origFile = new File(mVideoFilenames[id]);
        if (!origFile.exists() || origFile.length() <= 0) {
            Log.e(TAG, "Invalid file");
            mCurrentVideoValues[id] = null;
            return;
        }

        long duration = 0L;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mVideoFilenames[id]);
            duration = Long.valueOf(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "cannot access the file");
        }
        retriever.release();
        mActivity.getMediaSaveService().addVideo(mVideoFilenames[id],
                duration, mCurrentVideoValues[id],
                mOnVideoSavedListener, mContentResolver);
        Log.v(TAG, "saveVideo mVideoFilenames[id] :" + mVideoFilenames[id]);
        mCurrentVideoValues[id] = null;
    }

    private void updateRecordingTime(int id) {
        if (!mIsRecordingVideos[id]) {
            return;
        }

        if (mMediaRecorderPausings[id]) {
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
        mMultiCameraUI.setRecordingTime(text);
        if (mRecordingTimeCountsDown != countdownRemainingTime) {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;

            int color = mActivity.getResources().getColor(countdownRemainingTime
                    ? R.color.recording_time_remaining_text
                    : R.color.recording_time_elapsed_text);

            mMultiCameraUI.setRecordingTimeTextColor(color);
        }
        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        mMultiCameraModule.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateRecordingTime(id);
            }
        }, actualNextUpdateDelay);
    }

    private long getTimeLapseVideoLength(long deltaMs) {
        // For better approximation calculate fractional number of frames captured.
        // This will update the video time at a higher resolution.
        double numberOfFrames = (double) deltaMs / mTimeBetweenTimeLapseFrameCaptureMs;
        return (long) (numberOfFrames / mProfile.videoFrameRate * 1000);
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updateRecordingPreview(int id) {
        if (null == mCameraDevices[id]) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mRecordRequestBuilders[id]);
            mRecordRequestBuilders[id].setTag(id);
            mCameraPreviewSessions[id].setRepeatingRequest(mRecordRequestBuilders[id].build(),
                    mCaptureCallback, mMultiCameraModule.getMyCameraHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private String generateVideoFilename(int outputFileFormat, int id) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = title + "_"+ id + CameraUtil.convertOutputFormatToFileExt(outputFileFormat);
        String mime = CameraUtil.convertOutputFormatToMimeType(outputFileFormat);
        String path;
        if (Storage.isSaveSDCard() && SDCard.instance().isWriteable()) {
            path = SDCard.instance().getDirectory() + '/' + filename;
        } else {
            path = Storage.DIRECTORY + '/' + filename;
        }
        mCurrentVideoValues[id] = new ContentValues(9);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.TITLE, title);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues[id].put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.DATA, path);
        mCurrentVideoValues[id].put(MediaStore.Video.Media.RESOLUTION,
                "" + mVideoSize[id].getWidth() + "x" + mVideoSize[id].getHeight());
        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            mCurrentVideoValues[id].put(MediaStore.Video.Media.LATITUDE, loc.getLatitude());
            mCurrentVideoValues[id].put(MediaStore.Video.Media.LONGITUDE, loc.getLongitude());
        }
        mVideoFilenames[id] = path;
        return path;
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mActivity.getString(R.string.video_file_name_format));
        return dateFormat.format(date);
    }

    private void setUpMediaRecorder(int id) throws IOException {
        if (null == mActivity) {
            return;
        }
        Log.v(TAG, " setUpMediaRecorder " + id);
        int size = CameraSettings.VIDEO_QUALITY_TABLE.get(mVideoSize[id].getWidth() + "x"
                + mVideoSize[id].getHeight());
        if (CamcorderProfile.hasProfile(id, size)) {
            mProfile = CamcorderProfile.get(id, size);
        } else {
            warningToast(R.string.error_app_unsupported_profile);
            throw new IllegalArgumentException("error_app_unsupported_profile");
        }

        if (mMediaRecorders[id] == null) {
            mMediaRecorders[id] = new MediaRecorder();
        }
        if (mAudioEncoder != -1) {
            mMediaRecorders[id].setAudioSource(MediaRecorder.AudioSource.MIC);
        }

        String defaultEncoder = mActivity.getString(R.string.pref_camera_videoencoder_default);
        String encoder = mLocalSharedPref.getString(
                MultiSettingsActivity.KEY_VIDEO_ENCODER_ + id, defaultEncoder);
        int videoEncoder = SettingTranslation.getVideoEncoder(encoder);
        if (DEBUG) Log.d(TAG,"setUpMediaRecorder encoder= "+ encoder + " videoEncoder=" + videoEncoder);
        mProfile.videoCodec = videoEncoder;

        mMediaRecorders[id].setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorders[id].setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePaths[id] == null || mNextVideoAbsolutePaths[id].isEmpty()) {
            mNextVideoAbsolutePaths[id] = generateVideoFilename(mProfile.fileFormat, id);
        }

        String encoderProfile = "Off";
        String defaultProfile = mActivity.getString(R.string.pref_camera2_videoencoderprofile_default);
        if (mLocalSharedPref != null) {
            encoderProfile = mLocalSharedPref.getString(
                    MultiSettingsActivity.KEY_VIDEO_ENCODER_PROFILE_ + id, defaultProfile);
        }
        boolean isVideoEncoderProfileSupported = !encoderProfile.equals("off");
        Log.d(TAG, "set encoderProfile: " + encoderProfile + " " + isVideoEncoderProfileSupported);
        if (isVideoEncoderProfileSupported &&
                VendorTagUtil.isHDRVideoModeSupported(mCameraDevices[id])) {
            int videoEncoderProfile = SettingTranslation.getVideoEncoderProfile(encoderProfile);
            Log.d(TAG, "setVideoEncodingProfileLevel: " + videoEncoderProfile + " " + MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
            mMediaRecorders[id].setVideoEncodingProfileLevel(videoEncoderProfile,
                    MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1);
        }

        mMediaRecorders[id].setMaxDuration(mMaxVideoDurationInMs);
        mMediaRecorders[id].setOutputFile(mNextVideoAbsolutePaths[id]);
        mMediaRecorders[id].setVideoEncodingBitRate(10000000);
        mMediaRecorders[id].setVideoFrameRate(30);
        mMediaRecorders[id].setVideoSize(mVideoSize[id].getWidth(), mVideoSize[id].getHeight());
        mMediaRecorders[id].setVideoEncoder(videoEncoder);
        if (DEBUG) Log.d(TAG," mMediaRecorder.setVideoEncoder="+videoEncoder);
        if (mAudioEncoder != -1) {
            mMediaRecorders[id].setAudioEncoder(mAudioEncoder);
        }
        int rotation = CameraUtil.getJpegRotation(id, mOrientation);
        if (mVideoRotation != null) {
            rotation += Integer.parseInt(mVideoRotation);
            rotation = rotation % 360;
        }
        mMediaRecorders[id].setOrientationHint(rotation);
        mMediaRecorders[id].prepare();
        mMediaRecorders[id].setOnErrorListener(this);
        mMediaRecorders[id].setOnInfoListener(this);
    }

    private void warningToast(final String msg) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                RotateTextToast.makeText(mActivity, msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFaceDetection(CaptureRequest.Builder request) {
        boolean FdEnable = mLocalSharedPref.getBoolean(
                MultiSettingsActivity.KEY_MULTI_FACE_DETECTION, false);
        Log.v(TAG, " applyFaceDetection FdEnable :" + FdEnable);
        try {
            int modeValue = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
            if (FdEnable){
                modeValue = CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE;
            }
            Log.v(TAG, " applyFaceDetection modeValue :" + modeValue);
            request.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, modeValue);
        } catch (IllegalArgumentException e) {
        }
    }

    private void applyVideoEncoderProfile(CaptureRequest.Builder builder, int cameraId) {
        String profile = mActivity.getString(R.string.pref_camera2_videoencoderprofile_default);
        if (mLocalSharedPref != null) {
            profile = mLocalSharedPref.getString(
                    MultiSettingsActivity.KEY_VIDEO_ENCODER_PROFILE_ + cameraId, profile);
        }
        int mode = 0;
        if (profile.equals("HEVCProfileMain10HDR10")) {
            mode = 2;
        } else if (profile.equals("HEVCProfileMain10")) {
            mode = 1;
        } else if (profile.equals("HEVCProfileMain10HDR10Plus")) {
            mode = 3;
        }
        Log.d(TAG, "applyVideoEncoderProfile set: " + mode);
        builder.set(hdr_video_mode, mode);
        VendorTagUtil.setHDRVideoMode(builder, (byte)mode);
    }

    private void applyVideoEIS(CaptureRequest.Builder request) {
        String value = mLocalSharedPref.getString(
                MultiSettingsActivity.KEY_VIDEO_EIS, "enable");

        if (DEBUG) {
            Log.d(TAG, "applyVideoEIS EIS select: " + value);
        }
        mStreamConfigOptMode = 0;
        if (value != null) {
            if (value.equals("V2")) {
                mStreamConfigOptMode = STREAM_CONFIG_MODE_QTIEIS_REALTIME;
            } else if (value.equals("V3")) {
                mStreamConfigOptMode = STREAM_CONFIG_MODE_QTIEIS_LOOKAHEAD;
            }
            byte byteValue = (byte) (value.equals("disable") ? 0x00 : 0x01);
            try {
                applyVideoStabilization(request, value.equals("disable"));
                request.set(CaptureModule.eis_mode, byteValue);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyVideoStabilization(CaptureRequest.Builder builder, boolean isDisabled) {
        if (isDisabled) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest
                    .CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        } else {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest
                    .CONTROL_VIDEO_STABILIZATION_MODE_ON);
        }
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

    /**
     * Shows a {@link Toast} on the UI thread.
     * @param text The message to show
     */
    private void showToast(final String text) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
