/*
Copyright (c) 2021 Qualcomm Innovation Center, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted (subject to the limitations in the
disclaimer below) provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;

import com.android.camera.ExtendedFace;
import com.android.camera.SettingsManager;
import com.android.camera.ui.MultiAutoFitSurfaceView;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PersistUtil;

import java.util.ArrayList;

public class MultiCameraFaceView extends FaceView {

    private final int smile_threashold_no_smile = 30;
    private final int smile_threashold_small_smile = 60;
    private final int blink_threshold = 60;

    private int[] mUncroppedWidths = new int[4];
    private int[] mUncroppedHeights = new int[4];

    private int[] mDisplayOrientations = new int[2];

    private Face[] mFaces;
    private ExtendedFace[] mExFaces;
    private Face[] mPendingFaces;
    private ExtendedFace[] mPendingExFaces;
    private Rect[] mCameraBounds = new Rect[4];
    private Rect[] mOriginalCameraBounds = new Rect[4];
    private float mZoom = 1.0f;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SWITCH_FACES:
                    mStateSwitchPending = false;
                    mFaces = mPendingFaces;
                    mExFaces = mPendingExFaces;
                    invalidate();
                    break;
            }
        }
    };
    private boolean mFacePointsEnable = false;
    private boolean mFacialContourEnable = false;
    private boolean mFdSmileEnable = false;
    private boolean mFdGazeEnable = false;
    private boolean mFdBlinkEnable = false;
    private boolean mPostZoomFov = false;
    private boolean mZoomRationSupported = false;
    private int mIndex;

    private ArrayList<MultiAutoFitSurfaceView> mPhysicalSurfaceViewList = new ArrayList();

    public MultiCameraFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initMode() {
        mFacialContourEnable = !"disable".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FACIAL_CONTOUR));
        mFacePointsEnable = "2".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FACE_DETECTION_MODE));
        mFdSmileEnable = "enable".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FD_SMILE));
        mFdGazeEnable = "enable".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FD_GAZE));
        mFdBlinkEnable = "enable".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FD_BLINK));
        mPostZoomFov = PersistUtil.isCameraPostZoomFOV();
    }

    public void setCameraBound(int index,  Rect cameraBound) {
        mCameraBounds[index] = cameraBound;
    }

    public void setDisplayOrientation(int index, int orientation) {
        mDisplayOrientations[index] = orientation;
        if (LOGV) Log.v(TAG, "mDisplayOrientation=" + orientation + ", index :" + index);
    }

    public void setOriginalCameraBound(int index, Rect originalCameraBound) {
        mOriginalCameraBounds[index] = originalCameraBound;
    }

    public void setZoom(float zoom) {
        mZoom = zoom;
    }

    public void setZoomRationSupported(boolean supported) {
        mZoomRationSupported = supported;
    }

    public void setFaces(int index, Face[] faces, ExtendedFace[] extendedFaces) {
        if (LOGV) Log.v(TAG, "Num of faces=" + faces.length);
        if (mPause) return;
        if (mFaces != null) {
            if ((faces.length > 0 && mFaces.length == 0)
                    || (faces.length == 0 && mFaces.length > 0)) {
                mPendingFaces = faces;
                mPendingExFaces = extendedFaces;
                if (!mStateSwitchPending) {
                    mStateSwitchPending = true;
                    mHandler.sendEmptyMessageDelayed(MSG_SWITCH_FACES, SWITCH_DELAY);
                }
                return;
            }
        }
        if (mStateSwitchPending) {
            mStateSwitchPending = false;
            mHandler.removeMessages(MSG_SWITCH_FACES);
        }
        mFaces = faces;
        mExFaces = extendedFaces;
        mIndex = index;
        if (!mBlocked && (mFaces != null) && (mFaces.length > 0) && mCameraBounds[mIndex] != null) {
            invalidate();
        }
    }

    private boolean isFDRectOutOfBound(Rect faceRect) {
        boolean result = false;
        if(mZoomRationSupported && mPostZoomFov) {
            result = mOriginalCameraBounds[mIndex].left > faceRect.left ||
                    mOriginalCameraBounds[mIndex].top > faceRect.top ||
                    faceRect.right > mOriginalCameraBounds[mIndex].right ||
                    faceRect.bottom > mOriginalCameraBounds[mIndex].bottom;
        } else {
            result = mCameraBounds[mIndex].left > faceRect.left || mCameraBounds[mIndex].top > faceRect.top ||
                    faceRect.right > mCameraBounds[mIndex].right || faceRect.bottom > mCameraBounds[mIndex].bottom;
        }
        return result;
    }

    public void onSurfaceTextureSizeChanged(int index, int uncroppedWidth, int uncroppedHeight) {
        mUncroppedWidths[index] = uncroppedWidth;
        mUncroppedHeights[index] = uncroppedHeight;
    }

    public void setPhysicalSurfaceViewList(ArrayList<MultiAutoFitSurfaceView> surfaceViewArrayList) {
        if (mPhysicalSurfaceViewList.size() == 0) {
            for (MultiAutoFitSurfaceView view : surfaceViewArrayList) {
                mPhysicalSurfaceViewList.add(view);
            }
        }
    }

    @Override
    public boolean faceExists() {
        return (mFaces != null && mFaces.length > 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mBlocked && (mFaces != null) && (mFaces.length > 0) && mCameraBounds[mIndex] != null) {
            int rw, rh;
            rw = mUncroppedWidths[mIndex];
            rh = mUncroppedHeights[mIndex];

            int width = mUncroppedWidths[mIndex];
            int height = mUncroppedHeights[mIndex];
            if (((rh > rw) && ((mDisplayOrientations[mIndex] == 0) || (mDisplayOrientations[mIndex] == 180)))
                    || ((rw > rh) && ((mDisplayOrientations[mIndex] == 90) || (mDisplayOrientations[mIndex] == 270)))) {
                int temp = rw;
                rw = rh;
                rh = temp;
            }
            if (rw * mCameraBounds[mIndex].width() != mCameraBounds[mIndex].height() * rh) {
                if (rw == rh || (rh * 288 == rw * 352)) {
                    rh = rw * mCameraBounds[mIndex].width() / mCameraBounds[mIndex].height();
                } else {
                    //rw = rh * mCameraBound.height() / mCameraBound.width();
                    int tmp_w = rh * mCameraBounds[mIndex].height() / mCameraBounds[mIndex].width();
                    int tmp_h = rw * mCameraBounds[mIndex].width() / mCameraBounds[mIndex].height();
                    if(tmp_w > rw) {
                        rw = tmp_w;
                    }
                    if(tmp_h > rh){
                        rh = tmp_h;
                    }
                }
            }
            CameraUtil.prepareMatrix(mMatrix, mMirror, mDisplayOrientations[mIndex], rw, rh);

            // mMatrix assumes that the face coordinates are from -1000 to 1000.
            // so translate the face coordination to match the assumption.
            Matrix translateMatrix = new Matrix();
            if(mZoomRationSupported && mPostZoomFov) {
                translateMatrix.preTranslate(-mOriginalCameraBounds[mIndex].width() / 2f,
                        -mOriginalCameraBounds[mIndex].height() / 2f);
                translateMatrix.postScale(2000f / mOriginalCameraBounds[mIndex].width(),
                        2000f / mOriginalCameraBounds[mIndex].height());
            } else {
                translateMatrix.preTranslate(-mCameraBounds[mIndex].width() / 2f,
                        -mCameraBounds[mIndex].height() / 2f);
                translateMatrix.postScale(2000f / mCameraBounds[mIndex].width(),
                        2000f / mCameraBounds[mIndex].height());
            }

            if (LOGV) {
                Log.v(TAG, "onDraw w * H :" + mCameraBounds[mIndex].width() + " x " + mCameraBounds[mIndex].height());
                Log.v(TAG, "onDraw getWidth() :" + getWidth() + " x getHeight() :" + getHeight());
            }
            int dx = 0;
            int dy = 0;
            if (mIndex == 0) {
                dx = (getWidth() / 2 - mUncroppedWidths[mIndex]) / 2;
                dx -= (rw - mUncroppedWidths[mIndex]);
            } else if (mIndex == 1) {
                dx = (getWidth() / 2) + (getWidth() / 2 - mUncroppedWidths[mIndex]) / 2;
                dx -= (rw - mUncroppedWidths[mIndex]);
            }
            dy = (getHeight() - mUncroppedHeights[mIndex]) / 2;
            dy -= (rh - mUncroppedHeights[mIndex]) / 2;
            if (LOGV) {
                Log.v(TAG, "onDraw mUncroppedWidths x heights :" + mUncroppedWidths[mIndex] + " x " + mUncroppedHeights[mIndex]);
                Log.v(TAG, "onDraw rw x rh :" + rw + " x " + rh);
                Log.v(TAG, "onDraw dx * dy :" + dx + " x " + dy);
            }

            Matrix pointTranslateMatrix = new Matrix();
            pointTranslateMatrix.postTranslate(dx,dy);

            // Focus indicator is directional. Rotate the matrix and the canvas
            // so it looks correctly in all orientations.
            canvas.save();
            mMatrix.postRotate(mOrientation); // postRotate is clockwise
            canvas.rotate(-mOrientation); // rotate is counter-clockwise (for canvas)

            int extendFaceSize = 0;
            extendFaceSize = mExFaces == null? 0 : mExFaces.length;

            for (int i = 0; i < mFaces.length; i++) {
                if (mFaces[i].getScore() < 50) continue;
                Rect faceBound = mFaces[i].getBounds();
                faceBound.offset(-mOriginalCameraBounds[mIndex].left, -mOriginalCameraBounds[mIndex].top);
                if (isFDRectOutOfBound(faceBound)) continue;
                mRect.set(faceBound);
                if (mZoom != 1.0f && !(mZoomRationSupported && mPostZoomFov)) {
                    mRect.left = mRect.left - mCameraBounds[mIndex].left;
                    mRect.right = mRect.right - mCameraBounds[mIndex].left;
                    mRect.top = mRect.top - mCameraBounds[mIndex].top;
                    mRect.bottom = mRect.bottom - mCameraBounds[mIndex].top;
                }
                translateMatrix.mapRect(mRect);
                if (LOGV) CameraUtil.dumpRect(mRect, "Original rect");
                mMatrix.mapRect(mRect);
                if (LOGV) CameraUtil.dumpRect(mRect, "Transformed rect");
                mPaint.setColor(mColor);
                mRect.offset(dx, dy);
                canvas.drawRect(mRect, mPaint);
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    @Override
    public void clear() {
        // Face indicator is displayed during preview. Do not clear the
        // drawable.
        mFaces = null;
        mExFaces = null;
        invalidate();
    }
}
