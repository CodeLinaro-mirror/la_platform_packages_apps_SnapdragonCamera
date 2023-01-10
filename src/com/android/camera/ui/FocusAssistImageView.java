/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class FocusAssistImageView extends ImageView {

    public FocusAssistImageView(Context context) {
        super(context);
    }

    public FocusAssistImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusAssistImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    Paint paint = new Paint();
    {
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    private Bitmap mBitmap;

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, paint);
        } else {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }
}
