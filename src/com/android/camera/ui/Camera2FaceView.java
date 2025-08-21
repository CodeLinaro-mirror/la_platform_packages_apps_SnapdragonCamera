/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import com.android.camera.util.Log;
import org.codeaurora.snapcam.R;
import com.android.camera.ExtendedFace;
import com.android.camera.SettingsManager;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PersistUtil;

import java.util.Arrays;

public class Camera2FaceView extends FaceView {

    private final int smile_threashold_no_smile = 30;
    private final int smile_threashold_small_smile = 60;
    private final int blink_threshold = 60;

    private Face[] mFaces;
    private ExtendedFace[] mExFaces;
    private Face[] mPendingFaces;
    private ExtendedFace[] mPendingExFaces;
    private Rect mCameraBound;
    private Rect mOriginalCameraBound;
    private float mZoom = 1.0f;

    private int[] mFacialMasks;
    private int mMaskNums = 0;

    private int mHeadNums = 0;
    private int[] mHeadInts;
    private int[] mTorsoValidInts;
    private int[] mTorsoInts;

    private int[] mPetHeadInts;
    private int[] mPetTorsoInts;
    private int[] mPetMarkInts;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SWITCH_FACES:
                    mStateSwitchPending = false;
                    mFaces = mPendingFaces;
                    mExFaces = mPendingExFaces;
                    postInvalidate();
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
    private boolean mDrawPetROI = false;
    private boolean mDrawPersonROI = false;

    private boolean mGenderEnable = false;

    private boolean mGenderConfidenceEnable = false;

    private boolean mFaceExpressionEnable = false;

    private boolean mFaceExpressionConfidenceEnable = false;
    private boolean mFaceSkineToneEnable = false;

    private Paint mTextPaint;
    private Paint mSkinTonePaint;

    private Paint mFaceExpressionConfidencePaint;

    public Camera2FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initMode() {

        SettingsManager mSettingsManager= SettingsManager.getInstance();
        mDrawPetROI = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_PET_DETECTION);
        mDrawPersonROI = mSettingsManager.isFdFeatureDisplay(SettingsManager.KEY_UPPER_BODY_DETECTION);
        String value =  mSettingsManager.getValue(SettingsManager.KEY_FACIAL_CONTOUR);
        mFacialContourEnable = value.equals("5") || value.equals("6")
                || value.equals("7") || value.equals("8");
        value = mSettingsManager.getValue(SettingsManager.KEY_FD_GAZE);
        mFdGazeEnable = "display".equals(value);
        value = mSettingsManager.getValue(SettingsManager.KEY_FD_BLINK);
        mFdBlinkEnable = "display".equals(value);
        value = mSettingsManager.getValue(SettingsManager.KEY_FD_GENDER);
        mGenderEnable = "display".equals(value);
        mGenderConfidenceEnable = mGenderEnable && PersistUtil.isGenderConfidenceOn();
        value = mSettingsManager.getValue(SettingsManager.KEY_FD_FACE_EXPRESSION);
        mFaceExpressionEnable = "display".equals(value);
        mFaceExpressionConfidenceEnable = mFaceExpressionEnable && PersistUtil.isFaceExpressionConfidenceOn();
        value = mSettingsManager.getValue(SettingsManager.KEY_FD_SKIN_TONE);
        mFaceSkineToneEnable = "display".equals(value);
        value = mSettingsManager.getValue(SettingsManager.KEY_FACE_MASK);

        mFacePointsEnable = "2".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FACE_DETECTION_MODE));

        mFdSmileEnable = "enable".equals(SettingsManager.getInstance().getValue(
                SettingsManager.KEY_FD_SMILE));


        mPostZoomFov = PersistUtil.isCameraPostZoomFOV();


        if ((mGenderEnable || mFaceExpressionEnable) && mTextPaint ==null) {
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(20 * getResources().getDisplayMetrics().density);
            mTextPaint.setStrokeWidth(2);
        }

        if ((mFaceExpressionConfidenceEnable || mGenderConfidenceEnable)
                && mFaceExpressionConfidencePaint == null) {
            mFaceExpressionConfidencePaint = new Paint();
            mFaceExpressionConfidencePaint.setAntiAlias(true);
            mFaceExpressionConfidencePaint.setTextSize(16 * getResources().getDisplayMetrics().density);
            mFaceExpressionConfidencePaint.setStrokeWidth(2);
        }

        if (mFaceSkineToneEnable  && mSkinTonePaint ==null) {
            mSkinTonePaint = new Paint();
            mSkinTonePaint.setAntiAlias(true);
            mSkinTonePaint.setTextSize(20 * getResources().getDisplayMetrics().density);
            mSkinTonePaint.setStrokeWidth(2);
            mSkinTonePaint.setColor(getResources().getColor(R.color.holo_blue_light));
        }
    }

    public void setCameraBound(Rect cameraBound) {
        mCameraBound = cameraBound;
    }

    public void setOriginalCameraBound(Rect originalCameraBound) {
        mOriginalCameraBound = originalCameraBound;
    }

    public void setZoom(float zoom) {
        mZoom = zoom;
    }

    public void setZoomRationSupported(boolean supported) {
        mZoomRationSupported = supported;
    }

    public void setFaces(Face[] faces, ExtendedFace[] extendedFaces) {
        Log.v(TAG, BIG_LOG,"Num of faces=" + faces.length);
        if (mPause) return;
        Face[] tmpFace = mFaces;
        if (tmpFace != null) {
            if ((faces.length > 0 && tmpFace.length == 0)
                    || (faces.length == 0 && tmpFace.length > 0)) {
                mPendingFaces = faces;
                mPendingExFaces = extendedFaces;
                if (!mStateSwitchPending) {
                    mStateSwitchPending = true;
                    mHandler.sendEmptyMessageDelayed(MSG_SWITCH_FACES, SWITCH_DELAY);
                }
                mFaces = faces;
                return;
            }
        }
        if (mStateSwitchPending) {
            mStateSwitchPending = false;
            mHandler.removeMessages(MSG_SWITCH_FACES);
        }
        mFaces = faces;
        mExFaces = extendedFaces;
        if (!mBlocked && mCameraBound != null) {
            postInvalidate();
        }
    }

    public void setFacialMasks(int[] facialMasks, int maskNums) {
        if (facialMasks != null) {
            mFacialMasks = facialMasks;
        }
        mMaskNums = maskNums;
        if (!mBlocked && (facialMasks != null) && (facialMasks.length > 0) &&
                mCameraBound != null && maskNums > 0) {
            postInvalidate();
        }
    }

    public void setPetParams(int[] heads, int[] tors, int[] mark) {
        if (heads != null) {
            mPetHeadInts = heads;
        }
        if (mark != null) {
            mPetMarkInts = mark;
        }
        if (tors != null) {
            mPetTorsoInts = tors;
        }
        if (!mBlocked &&  mCameraBound != null && ((heads != null && heads.length > 0 ) ||
                (tors != null && tors.length > 0) || (mark != null && mark.length >0))) {
            postInvalidate();
        }
    }
    public void setUpperBodys(int headNums, int[] headInts, int[] torsoValidInts, int[] torsoInts) {
        mHeadNums = headNums;
        if (headInts != null) {
            mHeadInts = headInts;
        }
        if (torsoValidInts != null) {
            mTorsoValidInts = torsoValidInts;
        }
        if (torsoInts != null) {
            mTorsoInts = torsoInts;
        }
        if (!mBlocked && (headInts != null) && (headInts.length > 0) &&
                mCameraBound != null) {
            postInvalidate();
        }
    }

    private boolean isFDRectOutOfBound(Rect faceRect) {
        boolean result = false;
        if(mZoomRationSupported && mPostZoomFov) {
            // change mOriginalCameraBound.left to 0 and mOriginalCameraBound.top to 0
            // This is for special sensor caused, camx can`t fixed, so app fixed.
            result = 0 > faceRect.left ||
                    0 > faceRect.top ||
                    faceRect.right > mOriginalCameraBound.right ||
                    faceRect.bottom > mOriginalCameraBound.bottom;
        } else {
            result = mCameraBound.left > faceRect.left || mCameraBound.top > faceRect.top ||
                    faceRect.right > mCameraBound.right || faceRect.bottom > mCameraBound.bottom;
        }
        return result;
    }

    @Override
    public boolean faceExists() {
        return (mFaces != null && mFaces.length > 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mBlocked && mCameraBound != null) {
            int rw, rh;
            rw = mUncroppedWidth;
            rh = mUncroppedHeight;
            if (((rh > rw) && ((mDisplayOrientation == 0) || (mDisplayOrientation == 180)))
                    || ((rw > rh) && ((mDisplayOrientation == 90) || (mDisplayOrientation == 270)))) {
                int temp = rw;
                rw = rh;
                rh = temp;
            }
            if (rw * mCameraBound.width() != mCameraBound.height() * rh) {
                if (rw == rh || (rh * 288 == rw * 352)) {
                    rh = rw * mCameraBound.width() / mCameraBound.height();
                } else {
                    //rw = rh * mCameraBound.height() / mCameraBound.width();
                    int tmp_w = rh * mCameraBound.height() / mCameraBound.width();
                    int tmp_h = rw * mCameraBound.width() / mCameraBound.height();
                    if(tmp_w > rw) {
                        rw = tmp_w;
                    }
                    if(tmp_h > rh){
                        rh = tmp_h;
                    }
                }
            }
            CameraUtil.prepareMatrix(mMatrix, mMirror, mDisplayOrientation, rw, rh);

            // mMatrix assumes that the face coordinates are from -1000 to 1000.
            // so translate the face coordination to match the assumption.
            Matrix translateMatrix = new Matrix();
            if(mZoomRationSupported && mPostZoomFov) {
                translateMatrix.preTranslate(-mOriginalCameraBound.width() / 2f,
                        -mOriginalCameraBound.height() / 2f);
                translateMatrix.postScale(2000f / mOriginalCameraBound.width(),
                        2000f / mOriginalCameraBound.height());
            } else {
                translateMatrix.preTranslate(-mCameraBound.width() / 2f,
                        -mCameraBound.height() / 2f);
                translateMatrix.postScale(2000f / mCameraBound.width(),
                        2000f / mCameraBound.height());
            }

            Log.v(TAG, FD_LOG,"onDraw w * H :" + mCameraBound.width() + " x " + mCameraBound.height());
            Matrix bsgcTranslateMatrix = new Matrix();
            if(mZoomRationSupported && mPostZoomFov) {
                bsgcTranslateMatrix.preTranslate(-mOriginalCameraBound.width() / 2f * mZoom,
                        -mOriginalCameraBound.height() / 2f * mZoom);
                bsgcTranslateMatrix.postScale(2000f / mOriginalCameraBound.width(),
                        2000f / mOriginalCameraBound.height());
            } else {
                bsgcTranslateMatrix.preTranslate(-mCameraBound.width() / 2f * mZoom,
                        -mCameraBound.height() / 2f * mZoom);
                bsgcTranslateMatrix.postScale(2000f / mCameraBound.width(),
                        2000f / mCameraBound.height());
            }

            int dx = (getWidth() - mUncroppedWidth) / 2;
            dx -= (rw - mUncroppedWidth) / 2;
            int dy = (getHeight() - mUncroppedHeight) / 2;
            dy -= (rh - mUncroppedHeight) / 2;
                Log.v(TAG, FD_LOG,"onDraw mUncroppedWidth x height :" + mUncroppedWidth + " x " + mUncroppedHeight +
                        " rw x rh :" + rw + " x " + rh + "onDraw dx * dy :" + dx + " x " + dy);
            Matrix pointTranslateMatrix = new Matrix();
            pointTranslateMatrix.postTranslate(dx,dy);

            // Focus indicator is directional. Rotate the matrix and the canvas
            // so it looks correctly in all orientations.
            canvas.save();
            mMatrix.postRotate(mOrientation); // postRotate is clockwise
            canvas.rotate(-mOrientation); // rotate is counter-clockwise (for canvas)

            int extendFaceSize = 0;
            extendFaceSize = mExFaces == null? 0 : mExFaces.length;
            if (mFacialContourEnable || mFacePointsEnable) {
                if (extendFaceSize != 0 && mExFaces[0] != null) {
                    int[] data = null;
                    int[] visibility = null;
                    if (mFacialContourEnable) {
                        data = mExFaces[0].getContour();
                    } else if (mFacePointsEnable) {
                        data = mExFaces[0].getLandMarks();
                    }
                    if (data != null && data.length != 0){
                        float[] points  = new float[data.length];
                        visibility = mExFaces[0].getVisibility();
                        int visnum = 0;
                        int invisnum = 0;
                        int unknnum = 0;
                        for (int i = 0; i < data.length; i++) {
                            points[i] = (float)data[i];
                        }
                        bsgcTranslateMatrix.mapPoints(points);
                        mMatrix.mapPoints(points);
                        pointTranslateMatrix.mapPoints(points);
                        drawFaceMark(bsgcTranslateMatrix,mMatrix,pointTranslateMatrix,mPetMarkInts,canvas);
                        if(visibility == null){
                            canvas.drawPoints(points,mPointPaint);
                        }else {
                            for (int i = 0; i < visibility.length; i++) {
                                if (visibility[i] == 1) {
                                    visnum++;
                                } else if (visibility[i] == 0) {
                                    invisnum++;
                                } else if (visibility[i] == -1) {
                                    unknnum++;
                                }
                            }
                            float[] points_visible = new float[visnum*2];
                            float[] points_invisible = new float[invisnum*2];
                            float[] points_unknown = new float[unknnum*2];
                            int visi = 0;
                            int invisi = 0;
                            int unknwi = 0;
                            int pointi = 0;
                            for (int i = 0; i < visibility.length && pointi < points.length - 1; i ++) {
                                if (visibility[i] == 1) {
                                    points_visible[visi] = points[pointi];
                                    points_visible[visi + 1] = points[pointi + 1];
                                    visi = visi + 2;
                                } else if (visibility[i] == 0) {
                                    points_invisible[invisi] = points[pointi];
                                    points_invisible[invisi + 1] = points[pointi + 1];
                                    invisi = invisi +2;
                                } else if (visibility[i] == -1) {
                                    points_unknown[unknwi] = points[pointi];
                                    points_unknown[unknwi + 1] = points[pointi + 1];
                                    unknwi = unknwi + 2;
                                }
                                pointi = pointi + 2;
                            }
                            canvas.drawPoints(points_visible, mPointPaint);
                            canvas.drawPoints(points_invisible, mPaintInvisible);
                            canvas.drawPoints(points_unknown, mPaintUnknown);
                        }
                    }
                }
            }

            if (mMaskNums > 0 && mFacialMasks != null && mFacialMasks.length > 4) {
                for (int i = 0; i < mFacialMasks.length; i += 4) {
                    if (mMaskNums == 0) {
                        break;
                    }
                    if ((mFacialMasks[i+2] - mFacialMasks[i])  > 0 &&
                            (mFacialMasks[i+3] - mFacialMasks[i+1]) > 0) {
                        Rect faceMask = new Rect(mFacialMasks[i], mFacialMasks[i+1],
                                mFacialMasks[i+2], mFacialMasks[i+3]);
                        faceMask.offset(0, 0);
                        if (isFDRectOutOfBound(faceMask)) continue;
                        mRect.set(faceMask);
                        if (mZoom != 1.0f && !(mZoomRationSupported && mPostZoomFov)) {
                            mRect.left = mRect.left - mCameraBound.left;
                            mRect.right = mRect.right - mCameraBound.left;
                            mRect.top = mRect.top - mCameraBound.top;
                            mRect.bottom = mRect.bottom - mCameraBound.top;
                        }
                        translateMatrix.mapRect(mRect);
                        CameraUtil.dumpRect(mRect, "Original Facial mask");
                        mMatrix.mapRect(mRect);
                        CameraUtil.dumpRect(mRect, "Transformed Facial mask");
                        mPaint.setColor(Color.BLUE);
                        mRect.offset(dx, dy);
                        canvas.drawRect(mRect, mPaint);
                    }
                }
            }
            if(mDrawPersonROI) {
                drawHeadTorsos(canvas, dx, dy, translateMatrix, false, false);
                drawHeadTorsos(canvas, dx, dy, translateMatrix, false, true);
            }
            if(mDrawPetROI) {
                drawHeadTorsos(canvas, dx, dy, translateMatrix, true, false);
                drawHeadTorsos(canvas, dx, dy, translateMatrix, true, true);
                drawFaceMark(bsgcTranslateMatrix,mMatrix,pointTranslateMatrix,mPetMarkInts,canvas);
            }
            if(faceExists()) {
                Face[] currentFaces = mFaces;
                for (int i = 0; i < currentFaces.length; i++) {
                    if (currentFaces[i].getScore() < 50) continue;
                    Rect faceBound = currentFaces[i].getBounds();
                    // This is for special sensor caused, camx can`t fixed, so app fixed.
                    //faceBound.offset(-mOriginalCameraBound.left, -mOriginalCameraBound.top);
                    faceBound.offset(0, 0);
                    if (isFDRectOutOfBound(faceBound)) continue;
                    mRect.set(faceBound);
                    if (mZoom != 1.0f && !(mZoomRationSupported && mPostZoomFov)) {
                        mRect.left = mRect.left - mCameraBound.left;
                        mRect.right = mRect.right - mCameraBound.left;
                        mRect.top = mRect.top - mCameraBound.top;
                        mRect.bottom = mRect.bottom - mCameraBound.top;
                    }
                    translateMatrix.mapRect(mRect);
                    CameraUtil.dumpRect(mRect, "Original rect");
                    mMatrix.mapRect(mRect);
                    CameraUtil.dumpRect(mRect, "Transformed rect");
                    mPaint.setColor(mColor);
                    mRect.offset(dx, dy);
                    canvas.drawRect(mRect, mPaint);

                    if (mExFaces != null  && i < mExFaces.length) {
                        Log.v(TAG, FD_LOG, "onDraw extendFaceSize " + extendFaceSize + ", mExFaces[" + i + "] " + mExFaces[i]);
                    }
//
                    if (mExFaces != null && i < mExFaces.length && mExFaces[i] != null) {

                        ExtendedFace exFace = mExFaces[i];
                        Face face = currentFaces[i];

                        float[] point = new float[4];
                        int delta_x = faceBound.width() / 12;
                        int delta_y = faceBound.height() / 12;

                        Log.e(TAG, "blink: (" + exFace.getLeyeBlink() + ", " +
                                exFace.getReyeBlink() + ")");
                        if (face.getLeftEyePosition() != null) {
                            if ((mDisplayRotation == 0) ||
                                    (mDisplayRotation == 180)) {
                                point[0] = face.getLeftEyePosition().x;
                                point[1] = face.getLeftEyePosition().y - delta_y / 2;
                                point[2] = face.getLeftEyePosition().x;
                                point[3] = face.getLeftEyePosition().y + delta_y / 2;
                            } else {
                                point[0] = face.getLeftEyePosition().x - delta_x / 2;
                                point[1] = face.getLeftEyePosition().y;
                                point[2] = face.getLeftEyePosition().x + delta_x / 2;
                                point[3] = face.getLeftEyePosition().y;
                            }
                            bsgcTranslateMatrix.mapPoints(point);
                            mMatrix.mapPoints(point);
                            if (mFdBlinkEnable && exFace.getLeyeBlink() >= blink_threshold) {
                                canvas.drawLine(point[0] + dx, point[1] + dy,
                                        point[2] + dx, point[3] + dy, mPaint);
                            }else if(exFace.getLeyeBlink() < blink_threshold && !mFdGazeEnable){
                                float[] circlpoint = new float[2];
                                if ((mDisplayRotation == 0) ||
                                        (mDisplayRotation == 180)) {
                                    circlpoint[0] = face.getLeftEyePosition().x;
                                    circlpoint[1] = face.getLeftEyePosition().y;
                                }else{
                                    circlpoint[0] = face.getLeftEyePosition().x ;
                                    circlpoint[1] = face.getLeftEyePosition().y;
                                }
                                bsgcTranslateMatrix.mapPoints(circlpoint);
                                mMatrix.mapPoints(circlpoint);
                                canvas.drawCircle(circlpoint[0]+dx,circlpoint[1]+dy,delta_x/8,mPaint);
                            }
                        }
                        if (face.getRightEyePosition() != null) {
                            if ((mDisplayRotation == 0) ||
                                    (mDisplayRotation == 180)) {
                                point[0] = face.getRightEyePosition().x;
                                point[1] = face.getRightEyePosition().y - delta_y / 2;
                                point[2] = face.getRightEyePosition().x;
                                point[3] = face.getRightEyePosition().y + delta_y / 2;
                            } else {
                                point[0] = face.getRightEyePosition().x - delta_x / 2;
                                point[1] = face.getRightEyePosition().y;
                                point[2] = face.getRightEyePosition().x + delta_x / 2;
                                point[3] = face.getRightEyePosition().y;
                            }
                            bsgcTranslateMatrix.mapPoints(point);
                            mMatrix.mapPoints(point);
                            if (mFdBlinkEnable && exFace.getReyeBlink() >= blink_threshold) {
                                //Add offset to the points if the rect has an offset
                                canvas.drawLine(point[0] + dx, point[1] + dy,
                                        point[2] + dx, point[3] + dy, mPaint);
                            }else if(exFace.getReyeBlink() < blink_threshold && !mFdGazeEnable){
                                float[] circlpoint = new float[2];
                                if ((mDisplayRotation == 0) ||
                                        (mDisplayRotation == 180)) {
                                    circlpoint[0] = face.getRightEyePosition().x;
                                    circlpoint[1] = face.getRightEyePosition().y;
                                }else{
                                    circlpoint[0] = face.getRightEyePosition().x ;
                                    circlpoint[1] = face.getRightEyePosition().y;
                                }
                                bsgcTranslateMatrix.mapPoints(circlpoint);
                                mMatrix.mapPoints(circlpoint);
                                canvas.drawCircle(circlpoint[0]+dx,circlpoint[1]+dy,delta_x/8,mPaint);
                            }
                        }
                        if ((exFace.getLeftrightGaze() != 0
                                || exFace.getTopbottomGaze() != 0)
                                && face.getLeftEyePosition() != null
                                && face.getRightEyePosition() != null) {

                           /* double length =
                                    Math.sqrt((face.getLeftEyePosition().x - face.getRightEyePosition().x) *
                                            (face.getLeftEyePosition().x - face.getRightEyePosition().x) +
                                            (face.getLeftEyePosition().y - face.getRightEyePosition().y) *
                                                    (face.getLeftEyePosition().y - face.getRightEyePosition().y)) / 2.0;
                            double nGazeYaw = -exFace.getLeftrightGaze();
                            double nGazePitch = -exFace.getTopbottomGaze();
                            float gazeRollX =
                                    (float) ((-Math.sin(nGazeYaw / 180.0 * Math.PI) *
                                            Math.cos(-exFace.getRollDirection() /
                                                    180.0 * Math.PI) +
                                            Math.sin(nGazePitch / 180.0 * Math.PI) *
                                                    Math.cos(nGazeYaw / 180.0 * Math.PI) *
                                                    Math.sin(-exFace.getRollDirection() /
                                                            180.0 * Math.PI)) *
                                            (-length) + 0.5);
                            float gazeRollY =
                                    (float) ((Math.sin(-nGazeYaw / 180.0 * Math.PI) *
                                            Math.sin(-exFace.getRollDirection() /
                                                    180.0 * Math.PI) -
                                            Math.sin(nGazePitch / 180.0 * Math.PI) *
                                                    Math.cos(nGazeYaw / 180.0 * Math.PI) *
                                                    Math.cos(-exFace.getRollDirection() /
                                                            180.0 * Math.PI)) *
                                            (-length) + 0.5);*/
                            float[]gazeXY = getGazeXY(exFace.getLeftrightGaze(),exFace.getTopbottomGaze());

                            if (mFdGazeEnable && exFace.getLeyeBlink() < blink_threshold) {
                                if ((mDisplayRotation == 90) ||
                                        (mDisplayRotation == 270)) {
                                    point[0] = face.getLeftEyePosition().x;
                                    point[1] = face.getLeftEyePosition().y;
                                    point[2] = face.getLeftEyePosition().x + gazeXY[0];
                                    point[3] = face.getLeftEyePosition().y + gazeXY[1];
                                } else {
                                    point[0] = face.getLeftEyePosition().x;
                                    point[1] = face.getLeftEyePosition().y;
                                    point[2] = face.getLeftEyePosition().x + gazeXY[0];
                                    point[3] = face.getLeftEyePosition().y + gazeXY[1];
                                }
                                bsgcTranslateMatrix.mapPoints(point);
                                mMatrix.mapPoints(point);
                                canvas.drawLine(point[0] + dx, point[1] + dy,
                                        point[2] + dx, point[3] + dy, mPaint);
                            }

                            if (mFdGazeEnable && exFace.getReyeBlink() < blink_threshold) {
                                if ((mDisplayRotation == 90) ||
                                        (mDisplayRotation == 270)) {
                                    point[0] = face.getRightEyePosition().x;
                                    point[1] = face.getRightEyePosition().y;
                                    point[2] = face.getRightEyePosition().x + gazeXY[0];
                                    point[3] = face.getRightEyePosition().y + gazeXY[1];
                                } else {
                                    point[0] = face.getRightEyePosition().x;
                                    point[1] = face.getRightEyePosition().y;
                                    point[2] = face.getRightEyePosition().x + gazeXY[0];
                                    point[3] = face.getRightEyePosition().y + gazeXY[1];
                                }
                                bsgcTranslateMatrix.mapPoints(point);
                                mMatrix.mapPoints(point);
                                canvas.drawLine(point[0] + dx, point[1] + dy,
                                        point[2] + dx, point[3] + dy, mPaint);
                            }
                        }

                        if (mGenderEnable) {
                            int gender = exFace.getGender();
                            if (gender != -1) {
                                ExtendedFace.FDGenderIndex genderIndex =
                                        ExtendedFace.FDGenderIndex.values()[gender];
                                String genderText = genderIndex.name();
                                canvas.drawText(genderText, mRect.left, mRect.top - mTextPaint.descent(), mTextPaint);
                            }
                        }

                        if (mGenderConfidenceEnable) {
                            int[] genderConfidence = exFace.getGenderConfidence();
                            if (genderConfidence != null) {
                                for (int j = 0; j < genderConfidence.length; j++) {
                                    try {
                                        ExtendedFace.FDGenderIndex genderIndex =
                                                ExtendedFace.FDGenderIndex.values()[j];
                                        String genderText = genderIndex.name() + " : " + genderConfidence[j];
                                        if (mFaceExpressionConfidencePaint != null) {
                                            float offset = mFaceExpressionConfidencePaint.getTextSize();
                                            canvas.drawText(genderText, mRect.left,
                                                    mRect.top - offset - mTextPaint.descent() - offset * j,
                                                    mFaceExpressionConfidencePaint);
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "" + e.fillInStackTrace());
                                    }
                                }
                            }
                        }

                        if (mFaceExpressionEnable) {
                            int expression = exFace.getFaceExpression();
                            if (expression != -1) {
                                ExtendedFace.FDExpressionIndex expressionIndex =
                                        ExtendedFace.FDExpressionIndex.values()[expression];
                                String expressionText = expressionIndex.name();
                                float textSize = mTextPaint.getTextSize();
                                canvas.drawText(expressionText, mRect.left, mRect.bottom + textSize, mTextPaint);
                            }
                        }

                        if (mFaceExpressionConfidenceEnable) {
                            int[] confidences = exFace.getFaceExpressionConfidences();
                            float offset = 0;
                            if (mTextPaint != null) {
                                offset = mTextPaint.getTextSize();
                            }
                            if (confidences != null) {

                                for (int j = 0; j < confidences.length; j++) {
                                    try {

                                        ExtendedFace.FDExpressionIndex expressionIndex =
                                                ExtendedFace.FDExpressionIndex.values()[j];
                                        String expressionInfoText = expressionIndex.name() + " : " + confidences[j];

                                        float textSize = mFaceExpressionConfidencePaint.getTextSize();
                                        canvas.drawText(expressionInfoText, mRect.left, mRect.bottom + offset + textSize * (j + 1), mFaceExpressionConfidencePaint);
                                    } catch (Exception e) {
                                        Log.w(TAG, "" + e.fillInStackTrace());
                                    }
                                }

                            }
                        }
                        if (mFaceSkineToneEnable) {
                            int skinTone = exFace.getFaceSkinTone();
                            Log.v(TAG, FD_LOG, " onDraw skinTone="+skinTone+",i="+i+","+"mFaces length="+mFaces.length);
                            if(i < mFaces.length) {
                                Log.v(TAG, FD_LOG, " onDraw mFaces[i].getid=" + mFaces[i].getId());
                            }
                            if (skinTone != -1) {
                                ExtendedFace.FDSkineToneIndex skineToneIndex =
                                        ExtendedFace.FDSkineToneIndex.values()[skinTone];
                                String expressionText = skineToneIndex.name();
                                float textSize = mSkinTonePaint.getTextSize();
                                canvas.drawText(expressionText, mRect.left - textSize, mRect.bottom , mSkinTonePaint);
                            }
                        }

                    }
                }
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }
    private float[] getGazeXY(float lrgaze,float tbgaze){
        float[]xy= new float[2];
        int length = 100;
        float leftrigt = (float) (lrgaze / 180.0 * Math.PI);
        float topbottom = (float) (tbgaze / 180.0 * Math.PI);
        float lrtan = (float) (Math.tan(leftrigt) * Math.tan(leftrigt));
        float tbtan = (float) (Math.tan(topbottom) * Math.tan(topbottom));
        xy[0] = (float) (Math.sqrt((length*length)/(1+tbtan+lrtan))*Math.tan(leftrigt));
        xy[1] = (float)(Math.sqrt((length*length)/(1+tbtan+lrtan))*Math.tan(topbottom));
        return xy;
    }
    private void drawFaceMark(Matrix bsgcMatrix,Matrix mMatrix,Matrix pointMatrix,int[]data,Canvas canvas) {
        if (data != null && data.length != 0) {
            float[] points = new float[data.length];
            for (int i = 0; i < data.length; i++) {
                points[i] = (float) data[i];
            }
            bsgcMatrix.mapPoints(points);
            mMatrix.mapPoints(points);
            pointMatrix.mapPoints(points);
            canvas.drawPoints(points, mPointPaint);
        }
    }

    private void drawHeadTorsos(Canvas canvas, int dx, int dy, Matrix translateMatrix,boolean isPet,boolean isHead){
        int datas[] = mHeadInts;
        if(!isPet){
            if(!isHead){
                datas = mTorsoInts;
                mHeadTorsePaint.setColor(0xFFCCCCCC);
            }else{
                mHeadTorsePaint.setColor(0xFFFF9900);
            }
        }else{
            if(isHead){
                datas = mPetHeadInts;
                mHeadTorsePaint.setColor(0xFF000099);
            }else{
                datas = mPetTorsoInts;
                mHeadTorsePaint.setColor(0xFF990099);
            }
        }
        if (datas != null && datas.length > 4) {
            for (int i = 0; i < datas.length; i += 4) {
                    Rect head = new Rect(datas[i], datas[i+1],
                            datas[i+2], datas[i+3]);
                    head.offset(0, 0);
                   // if (isFDRectOutOfBound(head)) continue;
                    mRect.set(head);
                    if (mZoom != 1.0f && !(mZoomRationSupported && mPostZoomFov)) {
                        mRect.left = mRect.left - mCameraBound.left;
                        mRect.right = mRect.right - mCameraBound.left;
                        mRect.top = mRect.top - mCameraBound.top;
                        mRect.bottom = mRect.bottom - mCameraBound.top;
                    }
                    translateMatrix.mapRect(mRect);
                    CameraUtil.dumpRect(mRect, "Original Head roi");
                    mMatrix.mapRect(mRect);
                    CameraUtil.dumpRect(mRect, "Transformed Head roi");
                    mRect.offset(dx, dy);
                    canvas.drawRect(mRect, mHeadTorsePaint);

            }
        }
    }


    @Override
    public void clear() {
        // Face indicator is displayed during preview. Do not clear the
        // drawable.
        mFaces = null;
        mExFaces = null;
        mMaskNums = 0;
        mHeadNums = 0;
        mTorsoInts = null;
        invalidate();
    }
    @Override
    public void reset() {
        // Face indicator is displayed during preview. Do not clear the
        // drawable.
        mFaces = null;
        mExFaces = null;
        mColor = mFocusingColor;
        mMaskNums = 0;
        mHeadNums = 0;
        mTorsoInts = null;
        invalidate();
    }
}
