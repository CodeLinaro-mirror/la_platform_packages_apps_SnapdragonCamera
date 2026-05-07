/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera.multi;

public interface MultiCamera {

    boolean openCamera();

    void startPreview();

    void closeSession();

    void closeCamera();

    void onShutterButtonClick(String[] ids);

    void onVideoButtonClick(String[] ids);

    void onPause();

    void onResume();

    void onButtonPause(String[] ids);

    void onButtonContinue(String[] ids);

    void onOrientationChanged(int orientation);

    boolean isRecordingVideo();

    String[] getCameraIdList();
}