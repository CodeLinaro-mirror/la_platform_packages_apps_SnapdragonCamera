
/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.gles;

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_INFO_LOG_LENGTH;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexSubImage2D;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES30.GL_MAP_READ_BIT;
import static android.opengl.GLES30.GL_MAP_WRITE_BIT;
import static android.opengl.GLES30.GL_R16UI;
import static android.opengl.GLES30.GL_R32UI;
import static android.opengl.GLES30.GL_RGBA16UI;
import static android.opengl.GLES30.GL_RGBA8UI;
import static android.opengl.GLES30.GL_RGBA_INTEGER;
import static android.opengl.GLES30.glBindBufferBase;
import static android.opengl.GLES30.glMapBufferRange;
import static android.opengl.GLES30.glReadBuffer;
import static android.opengl.GLES30.glReadPixels;
import static android.opengl.GLES30.glTexStorage2D;
import static android.opengl.GLES30.glUniform1ui;
import static android.opengl.GLES30.glUnmapBuffer;
import static android.opengl.GLES31.GL_COMPUTE_SHADER;
import static android.opengl.GLES31.GL_READ_WRITE;
import static android.opengl.GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static android.opengl.GLES31.GL_SHADER_STORAGE_BUFFER;
import static android.opengl.GLES31.glBindImageTexture;
import static android.opengl.GLES31.glDispatchCompute;
import static android.opengl.GLES31.glMemoryBarrier;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class DepthRender implements TextureView.SurfaceTextureListener{

    private static final String TAG = "SnapCam_DepthRender";

    private boolean mActive = false;

    private Bitmap mColorLut;

    public DepthRender() {

    }

    public void init() {
        if (mActive) {
            Log.w(TAG, "init, has active");
            return;
        }
        Log.i(TAG, "init");
        startRenderThread();
        mActive = true;
    }

    public void destroy() {
        if (!mActive) {
            Log.w(TAG, "destroy, has not active");
            return;
        }
        Log.i(TAG, "destroy");
        mActive = false;
        stopRenderThread();
    }

    public void setDepthBuffer(ByteBuffer buffer, int width, int height, int rowStride, int pixStride) {
        if (mHandler == null) {
            return;
        }
        mHandler.sendDepthBufferAvailable(new DepthBufferWrapper(buffer, width, height, rowStride, pixStride));
    }

    public void setColorLut(Context context) {
        AssetManager assetManager = context.getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open("colorscale_rainbow.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
//            bitmap = Bitmap.createScaledBitmap(bitmap, 8192, 1, false);
            mColorLut = bitmap;
        } catch (IOException e) {
        }
    }

    public void setColorLut(Bitmap bitmap) {
        mColorLut = bitmap;
    }

    private int mDepthFocus = 1000;

    public void setDepthFocus(int focus) {
        mDepthFocus = focus;
    }

    private class DepthBufferWrapper {
        ByteBuffer buffer;
        int width;
        int height;
        int rowStride;
        int pixStride;
        public DepthBufferWrapper(ByteBuffer buffer, int width, int height, int rowStride, int pixStride) {
//            this.buffer = ByteBuffer.allocateDirect(buffer.capacity());
//            this.buffer.put(buffer);
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            this.rowStride = rowStride;
            this.pixStride = pixStride;
        }
    }

    private HandlerThread mRenderThread;

    private RenderHandler mHandler;


    private void startRenderThread() {
        mRenderThread = new HandlerThread("CameraDepthRenderThread");
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

    private SurfaceTexture mSurfaceTexture;
    private int mWidth;
    private int mHeight;

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        init();
        mSurfaceTexture = surface;
        mWidth = width;
        mHeight = height;
        mHandler.sendSurfaceAvailable();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        mWidth = width;
        mHeight = height;
        mHandler.sendSurfaceChange();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        mHandler.sendSurfaceDestroyed();
        destroy();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private static class RenderHandler extends Handler {

        private static final int MSG_INIT_EGL = 0;
        private static final int MSG_RELEASE = 1;
        private static final int MSG_SURFACE_AVAILABLE = 2;
        private static final int MSG_SURFACE_CHANGED = 3;
        private static final int MSG_SURFACE_DESTROYED = 4;
        private static final int MSG_BUFFER_AVAILABLE = 5;
        private static final int MSG_DRAW = 6;

        private final WeakReference<DepthRender> mOuter;
        public RenderHandler(Looper looper, DepthRender outer) {
            super(looper);
            mOuter = new WeakReference<>(outer);
        }

        public void sendInitEGL() {
            sendMessage(obtainMessage(MSG_INIT_EGL));
        }

        public void sendRelease() {
            sendMessage(obtainMessage(MSG_RELEASE));
            waitDone();
        }

        public void sendSurfaceAvailable() {
            sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE));
            waitDone();
        }

        public void sendSurfaceChange() {
            sendMessage(obtainMessage(MSG_SURFACE_CHANGED));
            waitDone();
        }

        public void sendSurfaceDestroyed() {
            sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
            waitDone();
        }

        public void sendDepthBufferAvailable(DepthBufferWrapper bufferWrapper) {
            sendMessage(obtainMessage(MSG_BUFFER_AVAILABLE, bufferWrapper));
            waitDone();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            DepthRender outer = mOuter.get();
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
                    surfaceAvailable(outer.mSurfaceTexture, outer.mWidth, outer.mHeight);
                    break;

                case MSG_SURFACE_CHANGED:
                    surfaceChanged(outer.mWidth, outer.mHeight);
                    break;

                case MSG_SURFACE_DESTROYED:
                    surfaceDestroyed();
                    break;

                case MSG_BUFFER_AVAILABLE:
                    depthBufferAvailable((DepthBufferWrapper) msg.obj);
                    break;

                case MSG_DRAW:
                    draw();
                    break;

                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        }

        private static final String COMPUTER_SHADER_DECODE_DEPTH =
                                "#version 320 es\n" +
                                "layout(location = 0) uniform uint width;\n" +
                                "layout(location = 1) uniform uint height;\n" +
                                "layout(location = 2) uniform uint focus;\n" +
                                "layout(local_size_x = 1024) in;\n" +
                                "layout(std430, binding = 0) readonly buffer Input {\n" +
                                "    uint data[];\n" +
                                "} _input;\n" +
                                "uniform layout(rgba8ui, binding = 1) writeonly highp uimage2D output_;\n" +
                                "\n" +
                                "uint high_int(uint v) {\n" +
                                "   return (v >> 16u);\n" +
                                "}\n" +
                                "uint low_int(uint v) {\n" +
                                "   return (v & 0xffffu);\n" +
                                "}\n" +
                                "\n" +
                                "uint map_depth_range(uint depthRange) {\n" +
                                "    uint lower = focus - 500u;\n" +
                                "    uint upper = focus + 500u;\n" +
                                "    if (depthRange < lower) {\n" +
                                "        return depthRange * 500u / lower;\n" +
                                "    } else if (depthRange < upper) {\n" +
                                "        return 500u + (depthRange - lower) * 500u / (upper - lower);\n" +
                                "    } else {\n" +
                                "        return 1000u + (depthRange - upper) * 275u / (8191u - upper);\n" +
                                "    }\n" +
                                "}\n" +
                                "\n" +
                                "uvec4 get_pix_from_depth(uint depth) {\n" +
                                "    uint depthRange = depth & 0x1fffu;\n" +
                                "    uint depthConfidence = (depth >> 13u) & 0x7u;\n" +
                                "    float depthPercentage = 0.0;\n" +
                                "    if (depthConfidence == 0u) {\n" +
                                "        depthPercentage = 1.0;\n" +
                                "    } else {\n" +
                                "        depthPercentage = float(depthConfidence - 1u) / 7.0;\n" +
                                "    }\n" +
                                "    if (depthPercentage < 0.1) {\n" +
                                "        return uvec4(0u, 0u, 0u, 0u);\n" +
                                "    }\n" +
                                "    uint depthRangeScale = map_depth_range(depthRange);\n" +
                                "    uint r = 0u;\n" +
                                "    uint g = 0u;\n" +
                                "    uint b = 0u;\n" +
                                "    if (depthRangeScale <= 255u ) {\n" +
                                "        r = 255u;\n" +
                                "        g = 255u - depthRangeScale;\n" +
                                "        b = 255u - depthRangeScale;\n" +
                                "    } else if (depthRangeScale > 255u && depthRangeScale <= 510u) {\n" +
                                "        r = 255u;\n" +
                                "        g = 0u + (depthRangeScale - 255u);\n" +
                                "        b = 0u;\n" +
                                "    } else if (depthRangeScale > 510u && depthRangeScale <= 765u) {\n" +
                                "        r = 255u - (depthRangeScale - 510u);\n" +
                                "        g = 255u;\n" +
                                "        b = 0u;\n" +
                                "    } else if (depthRangeScale > 765u && depthRangeScale <= 1020u) {\n" +
                                "        r = 0u;\n" +
                                "        g = 255u;\n" +
                                "        b = 0u + (depthRangeScale - 765u);\n" +
                                "    } else if (depthRangeScale > 1020u && depthRangeScale <= 1275u) {\n" +
                                "        r = 0u;\n" +
                                "        g = 255u - (depthRangeScale - 1020u);\n" +
                                "        b = 255u;\n" +
                                "    }\n" +
                                "    uvec4 pix = uvec4(r, g, b, 0u);;\n" +
                                "    return pix;\n" +
                                "}\n" +
                                "\n" +
                                "void main()\n" +
                                "{\n" +
                                "    uint idx = gl_GlobalInvocationID.x;\n" +
                                "    uint idx0_ = idx * 2u;\n" +
                                "    uint idx1_ = idx0_ + 1u;\n" +
                                "    uint width_ = width * 2u;\n" +
                                "    ivec2 pos0_ = ivec2(int(idx0_ % width_), int(idx0_ / width_));\n" +
                                "    ivec2 pos1_ = ivec2(int(idx1_ % width_), int(idx1_ / width_));\n" +
                                "    uint coord_y = idx / width;\n" +
                                "    uint coord_x = idx % width;\n" +
                                "    uint depth = _input.data[idx];\n" +
                                "    uint depth0 = low_int(depth);\n" +
                                "    uint depth1 = high_int(depth);\n" +
                                "    uvec4 pixel_ = get_pix_from_depth(depth0);\n" +
                                "    imageStore(output_, pos0_, pixel_);\n" +
                                "    pixel_ = get_pix_from_depth(depth1);\n" +
                                "    imageStore(output_, pos1_, pixel_);\n" +
                                "}";

        private int mComputeProgram = -1;
        private int[] mBuffers = new int[2];
        private EglBase mEglBase;

        private int mOffscreenTextureId = -1;
        private int mFrameBuffer = -1;

        private final float[] mCameraProjectionMatrix = new float[16];

        private void initEGL() {
            mEglBase = new EglBase(null, EglBase.FLAG_TRY_GLES3);
            mEglBase.makeCurrent(EGL14.EGL_NO_SURFACE);
            Log.i(TAG, "initEGL version " + mEglBase.getGlVersion());
            mComputeProgram = createComputeProgram(COMPUTER_SHADER_DECODE_DEPTH);
            if (mComputeProgram == 0) {
                Log.e(TAG, "create compute program error");
                throw new RuntimeException("create compute program error");
            }
            GLUtil.checkGlError("create compute program");
            Log.i(TAG, "initEGL, mComputeProgram " + mComputeProgram);
            glGenBuffers(2, mBuffers, 0);

            setColorLut(mOuter.get().mColorLut);
        }

        private void release() {
            if (mComputeProgram > 0) {
                glDeleteProgram(mComputeProgram);
            }
            glDeleteBuffers(2, mBuffers, 0);
            if (mEglBase != null) {
                mEglBase.release();
            }
        }

        private int mLutTexture;
        private void setColorLut(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Log.i(TAG, "setColorLut " + width + " " + height + " " + bitmap.getByteCount());

            ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount()).order(ByteOrder.LITTLE_ENDIAN);
            bitmap.copyPixelsToBuffer(buffer);
            buffer.rewind();

            mLutTexture = GLUtil.createImageTexture(buffer, width, height, GL_RGBA);
        }

        private WindowEglSurface mWindowSurface;
        private BaseProgram mProgram;
        private FullRectFrame mRectFrame;

        private int mWidth;
        private int mHeight;

        private boolean mCanDraw = false;

        private void surfaceAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "surfaceAvailable " + width + " " + height);
            mWidth = width;
            mHeight = height;
            EglBase eglBase = mEglBase;
            mWindowSurface = new WindowEglSurface(eglBase, surfaceTexture);
            mWindowSurface.makeCurrent();
            mProgram = new ImageProgram();
            ((ImageProgram) mProgram).setLutImageTexture(mLutTexture);
            mRectFrame = new FullRectFrame(mProgram, new BasicDrawable(BasicDrawable.SHAPE.FULL_RECTANGLE));

            final float[] tex_coords = {
                    1.0f, 1.0f,     // 0 bottom left
                    0.0f, 1.0f,     // 1 bottom right
                    1.0f, 0.0f,     // 2 top left
                    0.0f, 0.0f      // 3 top right
            };

            FloatBuffer texCoordArray = GLUtil.createFloatBuffer(tex_coords);
            mRectFrame.setTexCoordArray(texCoordArray);
            Matrix.setIdentityM(mCameraProjectionMatrix, 0);
            Matrix.scaleM(mCameraProjectionMatrix, 0, -1.0f, 1.0f, 1.0f);
            Matrix.rotateM(mCameraProjectionMatrix, 0, 90f, 0.0f, 0.0f, 1.0f);
            mCanDraw = true;
        }

        private void surfaceChanged(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        private void surfaceDestroyed() {
            Log.i(TAG, "surfaceDestroyed");
            mCanDraw = false;
            GLUtil.checkGlError("releaseGl start");

            releaseFrameBuffer();

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

            int[] values = new int[1];
            if (mLutTexture > 0) {
                values[0] = mLutTexture;
                glDeleteTextures(1, values, 0);
                mLutTexture = -1;
            }

            GLUtil.checkGlError("releaseGl done");

            mEglBase.makeCurrent( EGL14.EGL_NO_SURFACE);
        }

        private int mTexWidth = 0;
        private int mTexHeight = 0;
        private void depthBufferAvailable(DepthBufferWrapper bufferWrapper) {
            if (!mCanDraw) {
                return;
            }
            DepthRender outer = mOuter.get();
            if (outer == null) {
                return;
            }
            int focus = outer.mDepthFocus;
            ByteBuffer buffer = bufferWrapper.buffer;
            int width = bufferWrapper.width;
            int height = bufferWrapper.height;
            int rowStride = bufferWrapper.rowStride;
            int pixStride = bufferWrapper.pixStride;
//            Log.i(TAG, "depthBufferAvailable " + rowStride);

            if (mTexWidth != width || mTexHeight != height) {
                mTexWidth = width;
                mTexHeight = height;
                releaseFrameBuffer();
                prepareFrameBuffer(width, height);
            }

            buffer.rewind();
            int bufferLength = buffer.remaining();
            int outputLength = width * height * 4;
            GLUtil.checkGlError("start compute");
            mEglBase.makeCurrent(EGL14.EGL_NO_SURFACE);
            glUseProgram(mComputeProgram);

            glUniform1ui(0, rowStride / 4);
//            glUniform1ui(1, height);//the shader does not use the height, so the location will be -1, this line will cause error.
            glUniform1ui(2, focus);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, mBuffers[0]);
            glBufferData(GL_SHADER_STORAGE_BUFFER, bufferLength, buffer.asIntBuffer(), GL_DYNAMIC_DRAW);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, mBuffers[0]);
            glBindImageTexture(1, mOffscreenTextureId, 0, false, 0, GL_READ_WRITE, GL_RGBA8UI);
            GLUtil.checkGlError("glBindImageTexture");

            glDispatchCompute(bufferLength / 1024,1,1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            GLUtil.checkGlError("glDispatchCompute");

            glBindTexture(GL_TEXTURE_2D, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            glUseProgram(0);
            GLUtil.checkGlError("end compute");
            sendMessage(obtainMessage(MSG_DRAW));
//            draw();
        }

        private void draw() {
            if (!mCanDraw) {
                return;
            }
            mWindowSurface.makeCurrent();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, mWidth, mHeight);
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            mRectFrame.drawFrame(mOffscreenTextureId, mCameraProjectionMatrix);

            mWindowSurface.swapBuffers();

            glFinish();

            GLUtil.checkGlError("draw done");
        }

        private void prepareFrameBuffer(int width, int height) {
            int[] textureHandles = new int[1];
            int textureHandle;

            glGenTextures(1, textureHandles, 0);
            textureHandle = textureHandles[0];
            glBindTexture(GL_TEXTURE_2D, textureHandle);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8UI, width, height);
            /*GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);*/
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D, 0);
            mOffscreenTextureId = textureHandle;
        }

        private void releaseFrameBuffer() {
            int[] values = new int[1];
            if (mOffscreenTextureId > 0) {
                values[0] = mOffscreenTextureId;
                glDeleteTextures(1, values, 0);
                mOffscreenTextureId = -1;
            }
            if (mFrameBuffer > 0) {
                values[0] = mFrameBuffer;
                glDeleteFramebuffers(1, values, 0);
                mFrameBuffer = -1;
            }
        }

        private void waitDone() {
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

        private int loadShader(int shaderType, String shaderSource) {
            int shader = glCreateShader(shaderType);
            if (shader > 0) {
                glShaderSource(shader, shaderSource);
                glCompileShader(shader);
                int[] status = new int[1];
                glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
                if (status[0] == 0) {
                    glGetShaderiv(shader, GL_INFO_LOG_LENGTH, status, 0);
                    if (status[0] != 0) {
                        String log = glGetShaderInfoLog(shader);
                        Log.e(TAG, "Error: Compiler log:\n" + log);
                    }
                    glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createComputeProgram(String computeSource) {
            int computeShader = loadShader(GL_COMPUTE_SHADER, computeSource);
            if (computeShader == 0) {
                return 0;
            }

            int program = glCreateProgram();
            if (program > 0) {
                glAttachShader(program, computeShader);
                glLinkProgram(program);
                int[] status = new int[1];
                glGetProgramiv(program, GL_LINK_STATUS, status, 0);
                if (status[0] == 0) {
                    glGetProgramiv(program, GL_INFO_LOG_LENGTH, status, 0);
                    if (status[0] != 0) {
                        String log = glGetProgramInfoLog(program);
                        Log.e(TAG, "Error: Link log: \n" + log);
                    }
                    glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }
    }
}
