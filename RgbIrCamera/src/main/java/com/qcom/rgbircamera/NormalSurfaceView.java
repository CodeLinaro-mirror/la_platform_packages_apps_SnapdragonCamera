/*=============================================================================
Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
SPDX-License-Identifier: BSD-3-Clause-Clear
=============================================================================*/
package com.qcom.rgbircamera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class NormalSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private Surface surface;
    private OnSurfaceReadyListener listener;

    public NormalSurfaceView(Context context) {
        super(context);
        init();
    }

    public NormalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    public void setOnSurfaceReadyListener(OnSurfaceReadyListener listener) {
        this.listener = listener;
        if (surface != null && listener != null) {
            listener.onSurfaceReady(surface);
        }
    }

    public Surface getSurface() {
        return surface;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        if (listener != null) {
            listener.onSurfaceReady(surface);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surface = null;
        if (listener != null) {
            listener.onSurfaceReady(null);
        }
    }

    public interface OnSurfaceReadyListener {
        void onSurfaceReady(Surface surface);
    }
}
