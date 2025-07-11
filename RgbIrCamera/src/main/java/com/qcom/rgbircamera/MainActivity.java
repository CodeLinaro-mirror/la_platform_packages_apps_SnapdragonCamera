/*=============================================================================
Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
SPDX-License-Identifier: BSD-3-Clause-Clear
=============================================================================*/
package com.qcom.rgbircamera;

import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RgbIrCamera_MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private NormalSurfaceView mPreviewViewRGB, mPreviewViewIR;
    private Surface mSurfaceRGB, mSurfaceIR;
    private SeekBar mExposureTimeSeekBarRGB, mIsoSeekBarRGB, mExposureTimeSeekBarIR, mIsoSeekBarIR;
    private ToggleButton mSaveJpegToggle;
    private CameraController mCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewViewRGB = findViewById(R.id.preview_RGB);
        mPreviewViewIR = findViewById(R.id.preview_IR);
        mExposureTimeSeekBarRGB = findViewById(R.id.exposuretime_seekbar_RGB);
        mIsoSeekBarRGB = findViewById(R.id.iso_seekbar_RGB);
        mExposureTimeSeekBarIR = findViewById(R.id.exposuretime_seekbar_IR);
        mIsoSeekBarIR = findViewById(R.id.iso_seekbar_IR);
        mSaveJpegToggle = findViewById(R.id.save_jpeg_toggle);

        mCameraController = new CameraController(this);

        mPreviewViewRGB.setOnSurfaceReadyListener(surface -> {
            mSurfaceRGB = surface;
            if (surface == null) {
                Log.d(TAG, "RGB surface destroyed");
            } else {
                Log.d(TAG, "RGB surface ready");
                trySetupCamera();
            }
        });

        mPreviewViewIR.setOnSurfaceReadyListener(surface -> {
            mSurfaceIR = surface;
            if (surface == null) {
                Log.d(TAG, "IR surface destroyed");
            } else {
                Log.d(TAG, "IR surface ready");
                trySetupCamera();
            }
        });

        mExposureTimeSeekBarRGB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean formUser) {
                if (mCameraController != null) {
                    //get sensor exposureTime and support area.
                    long min = mCameraController.getMinExposureTime();
                    long max = mCameraController.getMaxExposureTime();
                    long exposureTimeRGB = min + (max - min) * progress / seekBar.getMax();
                    mCameraController.setExposureTimeRGB(exposureTimeRGB);
                    mCameraController.updateExposureCompensationTimeAndIso();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mIsoSeekBarRGB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean formUser) {
                if (mCameraController != null) {
                    int min = mCameraController.getMinIso();
                    int max = mCameraController.getMaxIso();
                    int isoRGB = min + (max - min) * progress / seekBar.getMax();
                    mCameraController.setIsoRGB(isoRGB);
                    mCameraController.updateExposureCompensationTimeAndIso();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mExposureTimeSeekBarIR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean formUser) {
                if (mCameraController != null) {
                    long min = mCameraController.getMinExposureTime();
                    long max = mCameraController.getMaxExposureTime();
                    long exposureTimeIR = min + (max - min) * progress / seekBar.getMax();
                    mCameraController.setExposureTimeIR(exposureTimeIR);
                    mCameraController.updateExposureCompensationTimeAndIso();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        mIsoSeekBarIR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean formUser) {
                if (mCameraController != null) {
                    int min = mCameraController.getMinIso();
                    int max = mCameraController.getMaxIso();
                    int isoIR = min + (max - min) * progress / seekBar.getMax();
                    mCameraController.setIsoIR(isoIR);
                    mCameraController.updateExposureCompensationTimeAndIso();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mSaveJpegToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCameraController != null) {
                mCameraController.setSaveJpegEnabled(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequsetCameraPermission();
    }

    private void checkAndRequsetCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            Log.d(TAG, "Permission granted");
            trySetupCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0]
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission callback granted");
            } else {
                Log.d(TAG, "Camera permission denied");
            }
        }
    }

    private void trySetupCamera() {
        if (mSurfaceRGB != null && mSurfaceIR != null) {
            mCameraController.setSurfaceAndControls(
                    mSurfaceRGB, mSurfaceIR,
                    mExposureTimeSeekBarRGB, mIsoSeekBarRGB,
                    mExposureTimeSeekBarIR, mIsoSeekBarIR);
            mCameraController.tryOpenCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraController != null) {
            mCameraController.release();
        }
    }
}
