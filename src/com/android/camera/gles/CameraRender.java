/*
 * Copyright 2013 Google Inc. All rights reserved.
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

 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2022, 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.gles;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

public class CameraRender implements TextureView.SurfaceTextureListener {

    private static final String TAG = CameraRender.class.getSimpleName();

    // Receives the output from the camera preview.
    private SurfaceTexture mSurfaceTexture;

    private HandlerThread mRenderThread;

    private RenderHandler mHandler;

    private boolean mActive = false;

    private int mPreviewWidth;
    private int mPreviewHeight;

    public CameraRender() {
        init();
    }

    public SurfaceTexture getDisplaySurfaceTexture() {
        return mSurfaceTexture;
    }

    public SurfaceTexture getSurfaceTexture() {
        if (mHandler == null || !mActive) {
            Log.d(TAG, "getSurfaceTexture null or not in active");
            return null;
        }
        return mHandler.getSurfaceTexture();
    }

    public void setPreviewSize(int width, int height) {
        Log.d(TAG, "setPreviewSize " + width + " " + height);
        mPreviewWidth = width;
        mPreviewHeight = height;
        if (mHandler != null) {
            mHandler.sendSetTextureSize(mPreviewHeight, mPreviewWidth);
        }
    }

    public void setCropRegionStartPoint(float xs, float ys) {
        Log.d(TAG, "setCropRegionStartPoint " + xs + " " + ys +", mHandler is not null " + (mHandler != null));
        if (mHandler != null) {
            mHandler.setCropRegionStartPoint(xs, ys);
        }
    }

    public void setFocusPoint(float x, float y) {
        Log.d(TAG, "setFocusPoint " + x + " " + y +", mHandler is not null " + (mHandler != null));
        if (mHandler != null) {
            mHandler.setFocusPoint(x, y);
        }
    }

    public void setFocusPointFA(float x, float y) {
        Log.d(TAG, "setFocusPointFA " + x + " " + y +", mHandler is not null " + (mHandler != null));
        if (mHandler != null) {
            mHandler.setFocusPointFA(x, y);
        }
    }

    private void init() {
        Log.i(TAG, "create");
        startRenderThread();
        mActive = true;
    }

    public void destroy() {
        Log.i(TAG, "destroy");
        mActive = false;
        stopRenderThread();
    }


    private void startRenderThread() {
        mRenderThread = new HandlerThread("CameraRenderThread");
        mRenderThread.start();

        mHandler = new RenderHandler(mRenderThread.getLooper(), this);
        mHandler.sendInitEGL();
    }

    private void stopRenderThread() {
        mHandler.sendRelease();
        try {
            if (mRenderThread != null) {
                mRenderThread.quitSafely();
                mRenderThread.join();
                mRenderThread = null;
            }
        } catch (InterruptedException e) {

        }
        mHandler = null;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable " + width + " " +  height);
        mSurfaceTexture = surface;
        mHandler.sendSurfaceAvailable(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged " + width + " " +  height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed");
        if (mHandler != null) {
            mHandler.sendSurfaceDestroyed();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
//        Log.i(TAG, "onSurfaceTextureUpdated");
    }

    public Rect getViewport() {
        return mHandler.mViewport;
    }


    private static class RenderHandler extends Handler implements SurfaceTexture.OnFrameAvailableListener {

        private static final int MSG_INIT_EGL = 0;
        private static final int MSG_RELEASE = 1;
        private static final int MSG_SURFACE_AVAILABLE =  2;
        private static final int MSG_SURFACE_CHANGED = 3;
        private static final int MSG_SURFACE_DESTROYED = 4;
        private static final int MSG_FRAME_AVAILABLE = 5;
        private static final int MSG_SET_TEXTURE_SIZE = 6;

        private static final boolean DEBUG_DISPLAY_REGION = true;

        private final WeakReference<CameraRender> mOuter;

        private int mWidth;
        private int mHeight;

        private int mTexW;
        private int mTexH;

        public Rect mViewport = new Rect();

        private boolean mInit = false;

        private EglBase mEglBase;
        private WindowEglSurface mWindowSurface;

        private OffscreenProgram mOffscreenProgram;

        private int mOffscreenTexId;

        private TextureProgram mProgram;

        private int mTextureId;

        private FullRectFrame mOffscreenRectFrame;
        private FullRectFrame mRectFrame;
        private FullRectFrame mThumbnailFrame;

        private SurfaceTexture mCameraTexture;

        private final float[] mDisplayProjectionMatrix = new float[16];

        private final Object mLock = new Object();

        private boolean mCanDraw = false;

        private FloatBuffer mTexCoordArray;

        private Basic2d mDisplayRegionRect;

        private Basic2d mTouchRegionRect;

        private FlatShadedProgram mDisplayRegionProgram;

        private float[] mDisplayRegionMatrix = new float[16];

        public RenderHandler(Looper looper, CameraRender outer) {
            super(looper);
            mOuter = new WeakReference<>(outer);
        }

        public SurfaceTexture getSurfaceTexture() {
            Log.d(TAG, "getSurfaceTexture @1 " + mCameraTexture);
            synchronized (mLock) {
                if (mCameraTexture == null) {
                    try {
                        mLock.wait(2000L);
                    } catch (InterruptedException e) {

                    }
                }
            }
            Log.d(TAG, "getSurfaceTexture @2 " + mCameraTexture);
            return mCameraTexture;
        }

        public void waitDone() {
            final Object waitDoneLock = new Object();
            final Runnable unlockRunnable = () -> {
                synchronized (waitDoneLock) {
                    waitDoneLock.notifyAll();
                }
            };
            synchronized (waitDoneLock) {
                post(unlockRunnable);
                try {
                    waitDoneLock.wait();
                } catch (InterruptedException ex) {
                    Log.v(TAG, "waitDone interrupted");
                }
            }
        }

        public void sendSetTextureSize(int w, int h) {
            sendMessage(obtainMessage(MSG_SET_TEXTURE_SIZE, w, h));
        }

        public void sendInitEGL() {
            sendMessage(obtainMessage(MSG_INIT_EGL));
//            waitDone();
        }

        public void sendRelease() {
            sendMessage(obtainMessage(MSG_RELEASE));
            waitDone();
        }

        public void sendSurfaceAvailable(int width, int height) {
            sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE, width, height));
            waitDone();
        }

        public void sendSurfaceDestroyed() {
            sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
            waitDone();
        }

        public void sendFrameAvailable() {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
        }


        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//            Log.d(TAG, "onFrameAvailable " + surfaceTexture);

            /*if (!mCanDraw) {
                return;
            }*/
            sendFrameAvailable();
        }

        public void setCropRegionStartPoint(float xs, float ys) {

            final float[] tex_coords = {
                    xs, ys,     // 0 bottom left
                    xs + 0.5f, ys,     // 1 bottom right
                    xs, ys + 0.5f,     // 2 top left
                    xs + 0.5f, ys + 0.5f      // 3 top right
            };
            if (DEBUG_DISPLAY_REGION) {
                mDisplayRegionRect.setScale(0.25f, 0.25f);
                mDisplayRegionRect.setPosition(xs + 0.25f, ys + 0.25f);
                Matrix.orthoM(mDisplayRegionMatrix, 0, 0f, 1f, 0f, 1.0f, -1f, 1f);
            }
            post(() -> {
                mTexCoordArray = GLUtil.createFloatBuffer(tex_coords);
            });
        }

        public void setFocusPoint(float x, float y) {
            if (DEBUG_DISPLAY_REGION) {
                mTouchRegionRect.setScale(0.166f, 0.125f);
                mTouchRegionRect.setPosition(x, y);
            }
        }

        public void setFocusPointFA(float x, float y) {
            if (DEBUG_DISPLAY_REGION) {
                mTouchRegionRect.setScale(0.083f, 0.0625f);
                mTouchRegionRect.setPosition(x, y);
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            CameraRender outer = mOuter.get();
            if (outer == null) {
                return;
            }
            switch (msg.what) {

                case MSG_INIT_EGL:
                    initEGL();
                    break;

                case MSG_RELEASE:
                    release();
                    break;

                case MSG_SURFACE_AVAILABLE:
                    surfaceAvailable(msg.arg1, msg.arg2);
                    break;

                case MSG_SURFACE_DESTROYED:
                    surfaceDestroyed();
                    break;
                case MSG_FRAME_AVAILABLE:
                    frameAvailable();
                    break;
                case MSG_SET_TEXTURE_SIZE:
                    setTextureSize(msg.arg1, msg.arg2);
                    break;

                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        }

        private void initEGL() {
            Log.d(TAG, "initEGL");
            mInit = true;
            mEglBase = new EglBase(null, EglBase.FLAG_TRY_GLES3);
            mEglBase.makeCurrent( EGL14.EGL_NO_SURFACE);
            mOffscreenProgram = new OffscreenProgram();
            mOffscreenRectFrame = new FullRectFrame(mOffscreenProgram, new BasicDrawable(BasicDrawable.SHAPE.FULL_RECTANGLE));
            mTextureId = mOffscreenProgram.createTextureObject();
            synchronized (mLock) {
                mCameraTexture = new SurfaceTexture(mTextureId);
                mLock.notifyAll();
            }
            mCameraTexture.setOnFrameAvailableListener(this);
            if (DEBUG_DISPLAY_REGION) {
                mDisplayRegionRect = new Basic2d(new BasicDrawable(BasicDrawable.SHAPE.BASIC_RECTANGLE));
                mDisplayRegionRect.setColor(1.0f, 0.1f, 0.1f);
                mDisplayRegionProgram = new FlatShadedProgram();
                mTouchRegionRect = new Basic2d(new BasicDrawable(BasicDrawable.SHAPE.BASIC_RECTANGLE));
                mTouchRegionRect.setColor(1.0f, 1.0f, 0.1f);
            }
        }

        private void surfaceAvailable(int width, int height) {
            Log.d(TAG, "surfaceAvailable " + width + " " + height);
            mWidth = width;
            mHeight = height;
            int diff = mHeight - mTexH;
            mViewport.set(0, diff / 2, mWidth,  diff / 2 + mTexH);
            CameraRender outer = mOuter.get();
            EglBase eglBase = mEglBase;
            SurfaceTexture surfaceTexture = outer.mSurfaceTexture;
            mWindowSurface = new WindowEglSurface(eglBase, surfaceTexture);
            mWindowSurface.makeCurrent();
            mProgram = new TextureProgram();
            mRectFrame = new FullRectFrame(mProgram, new BasicDrawable(BasicDrawable.SHAPE.RECTANGLE));

            mThumbnailFrame = new FullRectFrame(mProgram, new BasicDrawable(BasicDrawable.SHAPE.FULL_RECTANGLE));
            mCanDraw = true;
        }

        private void surfaceDestroyed() {
            Log.d(TAG, "surfaceDestroyed");
            mCanDraw = false;
            mTexCoordArray = null;
            GLUtil.checkGlError("releaseGl start");
            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }

            if (mProgram != null) {
                mProgram.release();
                mProgram = null;
            }

            if (mRectFrame != null) {
                mRectFrame.release(false);
            }

            if (mThumbnailFrame != null) {
                mThumbnailFrame.release(false);
            }

            GLUtil.checkGlError("releaseGl done");

            mEglBase.makeCurrent( EGL14.EGL_NO_SURFACE);
        }

        private void release() {
            Log.d(TAG, "release " + mInit);

            if (DEBUG_DISPLAY_REGION) {
                if (mDisplayRegionProgram != null) {
                    mDisplayRegionProgram.release();
                }
            }

            if (mOffscreenProgram != null) {
                mOffscreenProgram.release();
            }

            if (mOffscreenRectFrame != null) {
                mOffscreenRectFrame.release(false);
            }

            if (mCameraTexture != null) {
                mCameraTexture.setOnFrameAvailableListener(null);
                mCameraTexture.release();
            }

            if (mTextureId > 0) {
                GLES20.glDeleteTextures(1, new int[] {
                        mTextureId
                }, 0);
            }

            if (mEglBase != null) {
                mEglBase.release();
            }
            mInit = false;
        }

        private void frameAvailable() {
//            Log.d(TAG, "frameAvailable");
            try {
                mCameraTexture.updateTexImage();
            } catch (Exception e) {}
            draw();
        }

        private void setTextureSize(int width, int height) {
            mTexW = width;
            mTexH = height;
            mOffscreenProgram.prepareFramebuffer(width, height);
            mOffscreenTexId = mOffscreenProgram.getOffscreenTexture();
        }

        /**
         * Draws the scene and submits the buffer.
         */
        private void draw() {
            if (!mCanDraw) {
                return;
            }
            GLUtil.checkGlError("draw start");
            mCameraTexture.getTransformMatrix(mDisplayProjectionMatrix);

            GLES20.glViewport(0,0, mTexW, mTexH);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            mOffscreenRectFrame.drawFrame(mTextureId, mDisplayProjectionMatrix);

            GLES20.glViewport(0,0, mWidth, mHeight);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (mTexCoordArray != null) {
                mRectFrame.setTexCoordArray(mTexCoordArray);
            }

            int diff = mHeight - mTexH;

            GLES20.glViewport(0,diff / 2, mWidth, mTexH);

            mRectFrame.drawFrame(mOffscreenTexId, GLUtil.IDENTITY_MATRIX);

            if (DEBUG_DISPLAY_REGION) {
                mTouchRegionRect.draw(mDisplayRegionProgram, mDisplayRegionMatrix);
            }

            int thumbnailHeight = diff / 2;
            int thumbnailWidth = (int)(1.0f * mTexW / mTexH * diff / 2.0f);

            GLES20.glViewport(mWidth / 2 - thumbnailWidth / 2,0, thumbnailWidth, thumbnailHeight);

            mThumbnailFrame.drawFrame(mOffscreenTexId, GLUtil.IDENTITY_MATRIX);

            if (DEBUG_DISPLAY_REGION) {
                mDisplayRegionRect.draw(mDisplayRegionProgram, mDisplayRegionMatrix);
            }

            mWindowSurface.swapBuffers();

            GLUtil.checkGlError("draw done");
        }

    }
}
