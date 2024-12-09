/*
 * Copyright (c) 2022,2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.camera.util.PersistUtil;

import org.codeaurora.snapcam.R;

import java.lang.ref.WeakReference;

public class FocusAssistLayout extends FrameLayout implements GestureDetector.OnGestureListener {

    private static final String TAG = FocusAssistLayout.class.getSimpleName();

    private final GestureDetector mGestureDetector = new GestureDetector(this.getContext(), this);

    private Listener mListener;

    private final RectF mPreviewRect = new RectF();
    private final RectF mThumbnailRect = new RectF();;

    private int mPreviewTexWidth;
    private int mPreviewTexHeight;

    private float mCropRegionXs;
    private float mCropRegionYs;

    private int mWidth;
    private int mHeight;

    private final H mHandler = new H(Looper.getMainLooper(), this);

    private static class H extends Handler {

        public static final int MSG_TIMEOUT = 1;

        private WeakReference<FocusAssistLayout> mOuter;

        public H(Looper looper, FocusAssistLayout outer) {
            super(looper);
            mOuter = new WeakReference<>(outer);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            FocusAssistLayout outer = mOuter.get();
            if (outer == null) {
                return;
            }
            switch (msg.what) {
                case MSG_TIMEOUT:
                    if (outer.mListener != null) {
                        outer.mListener.onExit();
                    }
                    break;
                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        }
    }

    private void sendTimeoutMsg() {
        final int timeout = PersistUtil.getFocusAssistModeTimeout();
        Log.d(TAG, "sendTimeoutMsg " + timeout);
        if (timeout <= 0) {
            return;
        }
        mHandler.sendEmptyMessageDelayed(H.MSG_TIMEOUT, timeout);
    }

    public FocusAssistLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public FocusAssistLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Button closeButton = findViewById(R.id.focus_assist_close_button);
        closeButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onExitWithTask(() -> {
                    mHandler.removeMessages(H.MSG_TIMEOUT);
                });
            }
        });
    }

    private long mLastTouchTimeStamp = -1L;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        Log.d(TAG, "onTouchEvent " + event.getAction() + " (" + event.getX() + ", " + event.getY() + ")");
        long now = System.currentTimeMillis();
        if (now - mLastTouchTimeStamp > 1000L) {
            mLastTouchTimeStamp = now;
            mHandler.removeMessages(H.MSG_TIMEOUT);
            sendTimeoutMsg();
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.d(TAG, "onLayout " + left + " " + top + " " + right + " " + bottom);
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;
        post(() -> {
            int w = mWidth;
            int h = mHeight;
            Log.d(TAG, "layout " + w + " " + h);
            int diff = h - mPreviewTexHeight;
            mPreviewRect.left = 0f;
            mPreviewRect.top = diff / 2.0f;
            mPreviewRect.right = mPreviewRect.left + mPreviewTexWidth;
            mPreviewRect.bottom = mPreviewRect.top + mPreviewTexHeight;
            Log.d(TAG, "mPreviewRect " + mPreviewRect);
            mThumbnailRect.left = w / 2.0f - 1.0f * mPreviewTexWidth / mPreviewTexHeight * diff / 2.0f / 2.0f;
            mThumbnailRect.top = h - diff / 2.0f;
            mThumbnailRect.right = mThumbnailRect.left + 1.0f * mPreviewTexWidth / mPreviewTexHeight * diff / 2.0f;
            mThumbnailRect.bottom = h;
            Log.d(TAG, "mThumbnailRect " + mThumbnailRect);
        });
        sendTimeoutMsg();
    }

    public void setPreviewTexSize(int texW, int texH) {
        Log.d(TAG, "setPreviewTexSize " + texW + " " + texH);
        mPreviewTexWidth = texW;
        mPreviewTexHeight = texH;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setCropRegionStartPoint(float xs, float ys) {
        Log.d(TAG, "setCropRegion " + xs + " " + ys);
        mCropRegionXs = xs;
        mCropRegionYs = ys;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Log.d(TAG, "onSingleTapUp " + x + " " + y + " " + mWidth + " " + mHeight);
        if (mThumbnailRect != null && mThumbnailRect.contains(x, y)) {
            if (mListener != null) {
                mListener.onExitWithTask(() -> {
                    mHandler.removeMessages(H.MSG_TIMEOUT);
                });
            }
            return true;
        }

        if (mPreviewRect.contains(x, y)) {
            x = x - mPreviewRect.left;//mWidth == mPreviewRect.width()
            y = y - mPreviewRect.top;
            Log.d(TAG, "onSingleTapUp, adjust at " + x + " " + y);
            float x_ = (mCropRegionXs + x / mPreviewRect.width() * 0.5f) * mPreviewTexWidth;
            float y_ = (1.0f - mCropRegionYs - 0.5f + y / mPreviewRect.height() * 0.5f) * mPreviewTexHeight;
            Log.d(TAG, "onSingleTapUp, map at  " + x_ + " " + y_);
            if (mListener != null) {
                mListener.onFocus(x_, y_);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float x = e1.getX();
        float y = e1.getY();
        if (!mPreviewRect.contains(x, y)) {
            return true;
        }
        Log.d(TAG, "onScroll " + distanceX + " " + distanceY);
        mCropRegionXs += distanceX / mPreviewTexWidth;
        mCropRegionYs -= distanceY / mPreviewTexHeight;
        mCropRegionXs = Math.min(Math.max(mCropRegionXs, 0f), 0.5f);
        mCropRegionYs = Math.min(Math.max(mCropRegionYs, 0f), 0.5f);
        Log.d(TAG, "onStartPointChange " + mCropRegionXs + " " + mCropRegionYs);
        if (mListener != null) {
            mListener.onStartPointChange(mCropRegionXs, mCropRegionYs);
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public interface Listener {
        void onExit();

        void onFocus(float x, float y);

        void onStartPointChange(float xs, float ys);

        default void onExitWithTask(Runnable runnable) {
            runnable.run();
            onExit();
        }
    }
}
