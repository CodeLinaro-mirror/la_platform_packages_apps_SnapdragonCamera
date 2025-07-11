/*=============================================================================
Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
SPDX-License-Identifier: BSD-3-Clause-Clear
=============================================================================*/
package com.qcom.rgbircamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;

public class CameraController {
    private static final String TAG = "RgbIrCamera_CameraController";
    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private static final int EXPOSURE_TIME_STEPS = 100;

    private static final CaptureRequest.Key<Long> EXPOSURE_TIME_IR =
            new CaptureRequest.Key<>("org.quic.camera.SensorIRExposureControl.SensorIRExposureTime", long.class);
    private static final CaptureRequest.Key<Integer> SENSITIVITY_IR =
            new CaptureRequest.Key<>("org.quic.camera.SensorIRExposureControl.SensorIRSensitivity", int.class);
    private static final CaptureRequest.Key<Byte> RGB_NIR =
            new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.RGBNIR", byte.class);

    //streams mode define.
    private static final byte RGBNIR = 1;
    private static final byte RGB = 2;
    private static final byte IR = 3;

    private final Activity mActivity;
    private final Handler mHandler;
    private final ExecutorService mCameraExecutor;

    private Surface mSurfaceRGB, mSurfaceIR;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReaderRGB, mImageReaderIR;
    private CameraCharacteristics mCameraCharacteristics;

    private int mMinIso = 100;
    private int mMaxIso = 3200;
    public int mCurrentIsoRGB = 400;
    public int mCurrentIsoIR = 400;
    private long mMinExposureTime = 0L;
    private long mMaxExposureTime = 0L;
    public long mCurrentExposureTimeRGB = 0L;
    public long mCurrentExposureTimeIR = 0L;

    private int mSurfaceRGBFrameCount = 0;
    private int mSurfaceIRFrameCount = 0;
    private Timer mFpsCountTimer = new Timer();

    private SeekBar mExposureTimeSeekBarRGB, mIsoSeekBarRGB, mExposureTimeSeekBarIR, mIsoSeekBarIR;

    private final ExecutorService mSaveExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean mSaveJpegEnabled = false;

    private final BlockingQueue<ImageSaveTask> mJpegSaveQueue = new LinkedBlockingQueue<>();
    private final Thread mJpegSaveThread;
    private boolean mIsFpsOpen = false;

    public CameraController(Activity activity) {
        this.mActivity = activity;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mCameraExecutor = Executors.newSingleThreadExecutor();

        mJpegSaveThread = new Thread (() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ImageSaveTask task = mJpegSaveQueue.take();
                    try {
                        saveJPEGFromYUV(task.image, task.prefix);
                    } catch (Exception ex) {
                        Log.e(TAG, "saveJPEGFromYUV error", ex);
                    } finally {
                        try {
                            task.image.close();
                        } catch (Exception ex) {
                            Log.e(TAG, "Image close error", ex);
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            clearJpegQueue();
        }, "jpegSaveThread");
        mJpegSaveThread.start();
        if (mIsFpsOpen) {
            mFpsCountTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "surfaceRGB preview FPS RGB: " + mSurfaceRGBFrameCount);
                    Log.d(TAG, "surfaceRGB preview FPS IR: " + mSurfaceIRFrameCount);
                    mSurfaceRGBFrameCount = 0;
                    mSurfaceIRFrameCount = 0;
                }
            }, 1000, 1000);
        }
    }

    /**
     * open YUV date save as Jpeg
     */
    public void setSaveJpegEnabled(boolean enable) {
        this.mSaveJpegEnabled = enable;
        if (!enable) {
            clearJpegQueue();
        }
    }

    /**
     * clearQueue and close image
     */
    private void clearJpegQueue() {
        ImageSaveTask task;
        while ((task = mJpegSaveQueue.poll()) != null) {
            try {
                task.image.close();
            } catch (Exception ex) {
                Log.e(TAG, "Image close error", ex);
            }
        }
    }

    /**
     * Set preview surfaces and SeekBars.
     */
    public void setSurfaceAndControls(Surface surfaceRGB, Surface surfaceIR,
                                      SeekBar exposureTimeSeekBarRGB, SeekBar isoSeekBarRGB,
                                      SeekBar exposureTimeSeekBarIR, SeekBar isoSeekBarIR) {
        this.mSurfaceRGB = surfaceRGB;
        this.mSurfaceIR = surfaceIR;
        this.mExposureTimeSeekBarRGB = exposureTimeSeekBarRGB;
        this.mIsoSeekBarRGB = isoSeekBarRGB;
        this.mExposureTimeSeekBarIR = exposureTimeSeekBarIR;
        this.mIsoSeekBarIR = isoSeekBarIR;
    }

    /**
     * Try to open camera when both surfaces are ready and permission os granted.
     */
    public void tryOpenCamera() {
        Log.d(TAG, "tryOpenCamera: RGB= " + (mSurfaceRGB != null) + ", IR= " + (mSurfaceIR != null));
        if (mSurfaceRGB != null && mSurfaceIR != null) {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
                return;
            }
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = manager.getCameraIdList()[0];
                mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);

                //get exposure time range, for manual exposure.
                Range<Long> exposureTimeRange =
                        mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                //get iso range, for manual exposure.
                Range<Integer> isoRange =
                        mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                if (exposureTimeRange != null) {
                    mMinExposureTime = exposureTimeRange.getLower();
                    mMaxExposureTime = exposureTimeRange.getUpper();
                    mExposureTimeSeekBarRGB.setMax(EXPOSURE_TIME_STEPS);
                    mExposureTimeSeekBarIR.setMax(EXPOSURE_TIME_STEPS);
                    mCurrentExposureTimeRGB = mMinExposureTime + (mMaxExposureTime - mMinExposureTime) / 2;
                    mExposureTimeSeekBarRGB.setProgress(EXPOSURE_TIME_STEPS / 2);
                    mCurrentExposureTimeIR = mMinExposureTime + (mMaxExposureTime - mMinExposureTime) / 2;
                    mExposureTimeSeekBarIR.setProgress(EXPOSURE_TIME_STEPS / 2);
                    if (isoRange != null) {
                        mMinIso = isoRange.getLower();
                        mMaxIso = isoRange.getUpper();
                        int defaultIso = (mMaxIso + mMinIso) / 2;
                        mIsoSeekBarRGB.setMax(mMaxIso - mMinIso);
                        mIsoSeekBarIR.setMax(mMaxIso - mMinIso);
                        mCurrentIsoRGB = defaultIso;
                        mIsoSeekBarRGB.setProgress(defaultIso-mMinIso);
                        mCurrentIsoIR = defaultIso;
                        mIsoSeekBarIR.setProgress(defaultIso- mMinIso);
                    } else {
                        mIsoSeekBarRGB.setEnabled(false);
                        mIsoSeekBarIR.setEnabled(false);
                        Toast.makeText(mActivity, "Manual Iso not supported", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mExposureTimeSeekBarRGB.setEnabled(false);
                    mExposureTimeSeekBarIR.setEnabled(false);
                    Toast.makeText(mActivity, "Manual exposure time not supported", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Log.e(TAG, "tryOpenCamera: exception: " + ex);
            }
            openCamera();
        }
    }

    /**
     * Open the camera and initialize ImageReaders and callbacks.
     */
    private void openCamera() {
        Log.d(TAG, "open camera start");
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            Size previewSize = new Size(1920, 1080);
            mImageReaderRGB = ImageReader.newInstance(previewSize.getWidth(),
                previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReaderIR = ImageReader.newInstance(previewSize.getWidth(),
                    previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            mImageReaderRGB.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireNextImage();
                    if (image != null) {
                        Log.d(TAG, "Image available from RGB : " + image.getTimestamp());
                        if (mSaveJpegEnabled) {
                            if (!mJpegSaveQueue.offer(new ImageSaveTask(image, "RGB"))) {
                                image.close();
                            }
                        } else {
                            image.close();
                        }
                    }
                } catch (Exception ex) {
                    if (image != null) {
                        image.close();
                    }
                    Log.e(TAG, "imageReaderRGB listener error", ex);
                }
            }, mHandler);

            mImageReaderIR.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireNextImage();
                    if (image != null) {
                        Log.d(TAG, "Image available from IR : " + image.getTimestamp());
                        if (mSaveJpegEnabled) {
                            if (!mJpegSaveQueue.offer(new ImageSaveTask(image, "IR"))) {
                                image.close();
                            }
                        } else {
                            image.close();
                        }
                    }
                } catch (Exception ex) {
                    if (image != null) {
                        image.close();
                    }
                    Log.e(TAG, "imageReaderIR listener error", ex);
                }
            }, mHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    createCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, mHandler);
        } catch (Exception ex) {
            Log.e(TAG, "openCamera exception: " + ex);
        }
    }

    /**
     * Create a capture session with four stream (two YUV + RGB preview + IR preview)
     */
    private void createCaptureSession() {
        try {
            List<Surface> surfaceTargets = Arrays.asList(
                    mSurfaceIR, mImageReaderIR.getSurface(), mSurfaceRGB, mImageReaderRGB.getSurface());
            List<OutputConfiguration> outConfiguration =
                    new ArrayList<>(surfaceTargets.size());
            for (Surface surface : surfaceTargets) {
                outConfiguration.add(new OutputConfiguration(surface));
            }
            CaptureRequest.Builder builder = getFourTargetsRequestBuilder();
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            SessionConfiguration sessionCfg = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outConfiguration,
                    Executors.newSingleThreadExecutor(),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            try {
                                session.setRepeatingRequest(builder.build(),
                                        null, mHandler);
                            } catch (CameraAccessException ex) {
                                Log.e(TAG, "onConfigured: CameraAccessException", ex);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "onConfiguredFailed: capture session failed");
                        }
             });

            sessionCfg.setSessionParameters(builder.build());
            mCameraDevice.createCaptureSession(sessionCfg);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "createCaptureSession: CameraAccessException", ex);
        }
    }

    //app preview fps get callback
    CameraCaptureSession.CaptureCallback fpsCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            //set rgb\ir tag when session create.
            Object tag = request.getTag();
            if ("rgb".equals(tag)) {
                mSurfaceRGBFrameCount++;
            } else if ("ir".equals(tag)) {
                mSurfaceIRFrameCount++;
            }
        }
    };

    /**
     * Update exposure time and ISO for manual exposure.
     */
    public void updateExposureCompensationTimeAndIso() {
        if (mCameraDevice == null || mCaptureSession == null) {
            return;
        }
        try {
            CaptureRequest.Builder builder = getFourTargetsRequestBuilder();
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mCurrentExposureTimeRGB);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, mCurrentIsoRGB);

            //todo: IR exposure time and iso settings use specific keys.
            builder.set(EXPOSURE_TIME_IR, mCurrentExposureTimeIR);
            builder.set(SENSITIVITY_IR, mCurrentIsoIR);

            mCaptureSession.setRepeatingRequest(builder.build(), null, mHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "updateExposureCompensationTimeAndIso : CameraAccessException", ex);
        }
    }

    /**
     * Release Camera resources.
     */
    public void release() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReaderRGB != null) {
            mImageReaderRGB.close();
            mImageReaderRGB = null;
        }
        if (mImageReaderIR != null) {
            mImageReaderIR.close();
            mImageReaderIR = null;
        }
        mCameraExecutor.shutdown();
        mSaveExecutor.shutdown();
        mJpegSaveThread.interrupt();
        clearJpegQueue();
        if (mIsFpsOpen) {
            if (mFpsCountTimer != null) {
                mFpsCountTimer.cancel();
                mFpsCountTimer = null;
            }
        }
    }

    /**
     * save jpeg image for test
     */
    private void saveJPEGFromYUV(Image image, String prefix) {
        if (image == null) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();

        //get YUV data
        byte[] nv21 = yuv420ToNv21(image);
        if (nv21 == null) {
            return;
        }
        File dir = new File(mActivity.getExternalFilesDir(null), "JPEGFrames");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "saveJPEGFromYUV: mkdirs failed: " + dir.getAbsolutePath());
            return;
        }
        String fileName = prefix + "_" + System.currentTimeMillis() + "_" + width + "x" + height + ".jpg";
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fos);
            Log.d(TAG, "JPEG saved: " + file.getAbsolutePath());
        } catch (IOException ex) {
            Log.e(TAG, "saveJPEGFromYUV : IOException ", ex);
        }
    }

    /**
     * yuv to nv21 for test
     */
    private byte[] yuv420ToNv21(Image image) {
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 2;
        byte[] nv21 = new byte[ySize + uvSize];

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int rowStrideY = planes[0].getRowStride();
        int pixelStrideY = planes[0].getPixelStride();
        int rowStrideU = planes[1].getRowStride();
        int pixelStrideU = planes[1].getPixelStride();
        int rowStrideV = planes[2].getRowStride();
        int pixelStrideV = planes[2].getPixelStride();

        //y
        int pos = 0;
        for (int row = 0; row < height; row++) {
            int yRowStart = row * rowStrideY;
            for (int col = 0; col < width; col++) {
                nv21[pos++] = yBuffer.get(yRowStart + col * pixelStrideY);
            }
        }

        //uv
        int uvHeight = height / 2;
        int uvWidth = width / 2;
        int uvPos = ySize;
        for (int row = 0; row < uvHeight; row++) {
            int uRowStart = row * rowStrideU;
            int vRowStart = row * rowStrideV;
            for (int col = 0; col < uvWidth; col++) {
                nv21[uvPos++] = vBuffer.get(vRowStart + col * pixelStrideV);
                nv21[uvPos++] = uBuffer.get(uRowStart + col * pixelStrideU);
            }
        }
        return nv21;
    }

    /**
     * createCaptureRequest and add four targets.
     */
    private CaptureRequest.Builder getFourTargetsRequestBuilder() throws CameraAccessException {
        CaptureRequest.Builder fourTargetsBuilder
                = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        fourTargetsBuilder.addTarget(mSurfaceIR);
        fourTargetsBuilder.addTarget(mImageReaderIR.getSurface());
        fourTargetsBuilder.addTarget(mSurfaceRGB);
        fourTargetsBuilder.addTarget(mImageReaderRGB.getSurface());
        if (fourTargetsBuilder != null) {
            applySessionParameters(fourTargetsBuilder);
        }
        return fourTargetsBuilder;
    }

    /**
     * set RGBNIR sessionParameter.
     */
    private void applySessionParameters(CaptureRequest.Builder builder) {
        builder.set(RGB_NIR, RGBNIR);
    }

    public long getMinExposureTime() {
        return mMinExposureTime;
    }

    public long getMaxExposureTime() {
        return mMaxExposureTime;
    }

    public int getMinIso() {
        return mMinIso;
    }

    public int getMaxIso() {
        return mMaxIso;
    }

    public long getExposureTimeRGB() {
        return mCurrentExposureTimeRGB;
    }

    public long getExposureTimeIR() {
        return mCurrentExposureTimeIR;
    }

    public int getIsoRGB() {
        return mCurrentIsoRGB;
    }

    public int getIsoIR() {
        return mCurrentIsoIR;
    }

    public void setExposureTimeRGB(long exposureTime) {
        this.mCurrentExposureTimeRGB = exposureTime;
    }

    public void setExposureTimeIR(long exposureTime) {
        this.mCurrentExposureTimeIR = exposureTime;
    }

    public void setIsoRGB(int iso) {
        this.mCurrentIsoRGB = iso;
    }

    public void setIsoIR(int iso) {
        this.mCurrentIsoIR = iso;
    }

    public void setFpsCountOpen(boolean open) {
        this.mIsFpsOpen = open;
    }

    public boolean getFpsCountOpen() {
        return mIsFpsOpen;
    }

    /**
     * save Task
     */
    private static class ImageSaveTask {
        final Image image;
        final String prefix;
        ImageSaveTask(Image image, String prefix) {
            this.image = image;
            this.prefix = prefix;
        }
    }
}
