/*
  * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
  * SPDX-License-Identifier: BSD-3-Clause-Clear
*/

package com.android.camera.ui;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import com.android.camera.util.Log;
import org.codeaurora.snapcam.R;
import java.text.DecimalFormat;

public class ZoomBarView extends View {

    private float startAngle = 180f;
    private final float startLineAngle = 180f;
    private final float selectedLineAngle = 270f;
    private final int decimalPalce = 100;
    private final float zoomMoveValue = 0.01f;
    private final float sweepAngle = 90f;
    private float maxAngle = startAngle + sweepAngle;
    private float minAngle = startAngle;
    private int outerTotalDial = 5;
    private final int innerDial = 4;
    private final float extendMaxZoom = 100f;
    private int totalDial = (int) (10 - 0.67) * decimalPalce;
    private int uwDial = (int) ((1 - 0.67) * decimalPalce);
    private int wDial = (int) ((2.5 - 1) * decimalPalce);
    private float moveAnglePre = (float) sweepAngle / totalDial;
    private float moveAngle;
    private float extendZoomPre_Min,extendZoomPre_Max;
    private float[] outZoomValue = new float[]{0.67f, 1f, 2f, 2.5f, 3f, 10f};

    private OnZoomChangedListener mListener;
    private float  mCurrentZoom;
    float[] zoomRatioRange;
    private boolean isTouchChange;
    private String TAG = "SnapCam_ZoomBar";
    private int outerRadius;
    private final float radiusPart = 0.5f;
    private int innerLineHeight, smallLineHeight, outerLineHeight, zoomTextLen;
    private float mCenterX, mCenterY;
    private Context mContext;
    private Paint outerLinePaint, innerLinePaint, selectLinePaint, outerBgPaint, textPaint;
    private boolean zoomEnabled,mTouched;
    DecimalFormat zoomDf = new DecimalFormat("#.##");


    public void setZoomEnable(boolean enabled){
        zoomEnabled = enabled;
    }
    public interface OnZoomChangedListener {
        void onZoomValueChanged(float value);
    }

    public void setOnZoomChangeListener(OnZoomChangedListener listener) {
        mListener = listener;
    }
    public void setZoomValue(float value) {
        Log.d(TAG, " mCurrentZoom=" + mCurrentZoom + ",value=" + value);
        if (mCurrentZoom == value) {
            return;
        }
        mCurrentZoom = value;
        float angle = getAngleOfZoom(mCurrentZoom);
        if (angle != -1) {
            startAngle = startLineAngle + (selectedLineAngle - angle);
            Log.d(TAG, "angle=" + angle + ",startAngle=" + startAngle);
        }
        isTouchChange = false;
        invalidate();
    }

    private float getAngleOfZoom(float value) {
        float angle;
        int dial_ = outZoomValue.length - 1;
        for (int i = 0; i < outZoomValue.length - 1; i++) {
            if (value >= outZoomValue[i] && value < outZoomValue[i + 1]) {
                if (value == outZoomValue[i]) {
                    float zangle = startLineAngle + (sweepAngle / dial_) * i;
                    Log.d(TAG, "zangle=" + zangle + ",i=" + i);
                    return zangle;
                }
                float start_angle = startLineAngle + (sweepAngle / dial_) * i;
                float end_angle = startLineAngle + (sweepAngle / dial_) * (i + 1);
                int count = (int) ((outZoomValue[i + 1] - outZoomValue[i]) * decimalPalce);
                float eangle = (end_angle - start_angle) / count;
                float zoom = outZoomValue[i] + zoomMoveValue;
                for (int j = 1; j < count; j++) {
                    angle = start_angle + j * eangle;
                    String str = zoomDf.format(zoom);
                    zoom = Float.valueOf(str);
                    if (zoom == value) {
                        Log.d(TAG, "zoom=" + zoom + ",angle=" + angle + ",j=" + j);
                        return angle;
                    }
                    zoom = zoom + zoomMoveValue;
                }
            }
        }
        if (value == outZoomValue[dial_]) {
            return (startLineAngle + sweepAngle);
        }
        return -1f;
    }

    public void setZoomRange(float[] value) {
        zoomRatioRange = value;
        outerTotalDial = outZoomValue.length;
        outZoomValue[outerTotalDial - 1] = zoomRatioRange[1];
        if (outZoomValue[0] != zoomRatioRange[0] && zoomRatioRange[0] < outZoomValue[1]) {
            outZoomValue[0] = zoomRatioRange[0];
        }
        totalDial = (int) (zoomRatioRange[1] - outZoomValue[0]) * decimalPalce;
        moveAnglePre = sweepAngle / totalDial;
        float angle_max = getAngleOfZoom(zoomRatioRange[1]);
        float angle_min = getAngleOfZoom(zoomRatioRange[0]);
        maxAngle = startLineAngle + (angle_max - angle_min);
        if(zoomRatioRange[1] == extendMaxZoom){
            extendZoomPre_Min = getMaxZoomAnglePre(false);
            extendZoomPre_Max = getMaxZoomAnglePre(true);
        }
    }

    public ZoomBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        Resources res = getResources();
        outerLineHeight = (int) res.getDimension(R.dimen.big_zoom_len);
        smallLineHeight = (int) res.getDimension(R.dimen.small_zoom_len);
        innerLineHeight = (int) res.getDimension(R.dimen.inner_zoom_len);
        zoomTextLen = (int) res.getDimension(R.dimen.zoom_text_dis);
        outerLinePaint = new Paint();
        outerLinePaint.setColor(Color.WHITE);
        outerLinePaint.setAntiAlias(true);
        outerLinePaint.setStrokeWidth(res.getDimension(R.dimen.big_zoom_line));

        innerLinePaint = new Paint();
        innerLinePaint.setColor(Color.WHITE);
        innerLinePaint.setAntiAlias(true);
        innerLinePaint.setStrokeWidth(res.getDimension(R.dimen.small_zoom_line));

        selectLinePaint = new Paint();
        selectLinePaint.setColor(res.getColor(R.color.holo_blue_light));
        selectLinePaint.setAntiAlias(true);
        selectLinePaint.setStrokeWidth(res.getDimension(R.dimen.small_zoom_line));

        outerBgPaint = new Paint();
        outerBgPaint.setColor(Color.WHITE);
        outerBgPaint.setAlpha(30);
        outerBgPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStrokeWidth(1);
        textPaint.setTextSize(22);
        textPaint.setAntiAlias(true);
    }


    private float dx = 0, dy = 0, mx = 0, my = 0;
    private boolean mIsDrawDial = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dx = event.getX();
                dy = event.getY();
                if (dy < mCenterY - outerRadius) {
                    Log.d(TAG, " touch positon should below the zoom bar,return");
                    return false;
                }
                mTouched = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if(!mTouched || !zoomEnabled){
                    break;
                }
                mx = event.getX();
                my = event.getY();
                float md = mx - dx;
                if (md > 0 && startAngle < maxAngle) {
                    if (outZoomValue[outerTotalDial - 1] == extendMaxZoom) {
                        if (mCurrentZoom <= outZoomValue[outerTotalDial - 2]) {
                            moveAnglePre = extendZoomPre_Min;
                        } else {
                            moveAnglePre = extendZoomPre_Max;
                        }
                        moveAngle = extendZoomPre_Min;
                    }else{
                        moveAngle = moveAnglePre;
                    }
                    startAngle = Math.min(startAngle + (Math.min(md, 100) * moveAngle), maxAngle);
                    isTouchChange = true;
                    invalidate();
                } else if (md < 0 && startAngle > minAngle) {
                    if (outZoomValue[outerTotalDial - 1] == extendMaxZoom) {
                        if (mCurrentZoom < outZoomValue[outerTotalDial - 2]) {
                            moveAnglePre = extendZoomPre_Min;
                        } else {
                            moveAnglePre = extendZoomPre_Max;
                        }
                        moveAngle = extendZoomPre_Min;
                    }else {
                        moveAngle = moveAnglePre;
                    }
                    startAngle = Math.max(startAngle + (Math.min(md, 100) * moveAngle), minAngle);
                    isTouchChange = true;
                    invalidate();
                }
                dx = mx;
                dy = my;
                break;
            case MotionEvent.ACTION_UP:
                mx = event.getX();
                my = event.getY();
                mTouched = false;
                break;
        }
        return true;
    }

    float getMaxZoomAnglePre(boolean max) {
        int dial_ = outZoomValue.length - 1;
        int count;
        float angle_ = startLineAngle + (sweepAngle / dial_) * (dial_ - 1);
        float angle;
        if (!max) {
            count = (int) ((outZoomValue[dial_ - 1] - outZoomValue[0]) * decimalPalce);
            angle = (angle_ - startLineAngle) / count;
        } else {
            count = (int) ((outZoomValue[dial_] - outZoomValue[dial_ - 1]) * decimalPalce);
            float angle_max = startLineAngle + sweepAngle;
            angle = (angle_max - angle_) / count;
        }
        return angle;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!mIsDrawDial) {
            mIsDrawDial = true;
            outerRadius = (int) (getWidth() * radiusPart);
            mCenterX = getWidth() * radiusPart;
            mCenterY = getHeight();
            float centliney = mCenterY - outerRadius;
            float outerBgRadius = outerRadius + 5;
            canvas.drawArc(mCenterX - outerBgRadius, mCenterY - outerBgRadius, mCenterX + outerBgRadius, mCenterY + outerBgRadius, 180, 180f, true, outerBgPaint);
            drawDial(startAngle, sweepAngle, outerRadius, canvas);
            canvas.drawLine(mCenterX, centliney, mCenterX, centliney + outerLineHeight, selectLinePaint);
            mIsDrawDial = false;
        }
    }

    private void drawDial(float startAngle, float sweepAngle, int radius, Canvas canvas) {
        float angle;
        int dialCount = outZoomValue.length;
        Log.d(TAG, "sweepAngle= " + sweepAngle + ",startAngle=" + startAngle + ",dialCount=" + dialCount);
        float each_angle = sweepAngle / ((dialCount - 1) * 1f);
        float selecteDiff = -1;
        for (int i = 0; i < dialCount; i++) {
            selecteDiff = -1;
            if (zoomRatioRange[0] - outZoomValue[i] > 0.1) {
                continue;
            }
            angle = each_angle * i + startAngle;
            float[] startP = getPointFromAngleAndRadius(angle, radius);
            float[] endP = getPointFromAngleAndRadius(angle, radius - outerLineHeight);
            canvas.drawLine(startP[0], startP[1], endP[0], endP[1], outerLinePaint);
            float[] textP = getPointFromAngleAndRadius(angle, radius - outerLineHeight - zoomTextLen);
            String text = String.valueOf(outZoomValue[i]);
            canvas.drawText(text, textP[0], textP[1], textPaint);
            float diff = Math.abs(angle - selectedLineAngle);
            if (diff < moveAnglePre) {
                Log.d(TAG, "current zoom is " + text +
                        ",startP[0]=" + startP[0] + ",endP[0]=" + endP[0] +
                        ",outZoomValue[i]=" + outZoomValue[i] + ",mCurrentZoom=" + mCurrentZoom
                +",moveAnglePre="+moveAnglePre+",angle="+angle);
                if (isTouchChange) {
                    mListener.onZoomValueChanged(outZoomValue[i]);
                    mCurrentZoom = outZoomValue[i];
                    selecteDiff = diff;
                }
            }
            if (i < dialCount - 1) {
                float angle2 = each_angle * (i + 1) + startAngle;
                float diffangle = angle2 - angle;
                float sRadius = radius - outerLineHeight / 4f;
                drawUWDial(angle, diffangle, outZoomValue[i], outZoomValue[i + 1], sRadius, canvas,selecteDiff);
            }
        }
    }

    private void drawUWDial(float startAngle, float sweepAngle, float startZoom, float endZoom, float radius, Canvas canvas,float selecteDiff){
        float angle;
        float[] startP, endP;
        int dialCount = (int) ((endZoom - startZoom) * decimalPalce);
        int step = dialCount / innerDial;
        int num = 1;
        float each_angle = (sweepAngle) / (dialCount * 1f);
        Log.d(TAG, " sweepAngle= " + sweepAngle + ",startAngle=" + startAngle + ",dialCount=" + dialCount
                + ",radius=" + radius + ",startZoom=" + startZoom + ",endzoom=" + endZoom + ",step=" + step + ",each_angle=" + each_angle);
        startZoom = startZoom + zoomMoveValue;
        for (int i = 1; i < dialCount; i++) {
            angle = each_angle * i + startAngle;
            String text = zoomDf.format(startZoom);
            startZoom = Float.valueOf(text);
            if (i % step == 0) {
                startP = getPointFromAngleAndRadius(angle, radius);
                endP = getPointFromAngleAndRadius(angle, radius - innerLineHeight);
                float[] textP = getPointFromAngleAndRadius(angle, radius - innerLineHeight - zoomTextLen);
                if (num < innerDial ||(num == innerDial && dialCount <= 10)) {
                    canvas.drawLine(startP[0], startP[1], endP[0], endP[1], innerLinePaint);
                }
                if (num == innerDial / 2) {
                    canvas.drawText(text, textP[0], textP[1], textPaint);
                }
                num++;
            }
            float diff = Math.abs(angle - selectedLineAngle);
            if ((diff < each_angle && selecteDiff == -1) || (selecteDiff >= 0 && diff < selecteDiff)){
                if (isTouchChange) {
                    mListener.onZoomValueChanged(startZoom);
                    mCurrentZoom = startZoom;
                    Log.d(TAG,"mCurrentZoom="+mCurrentZoom+",angle="+angle+",each_angle="+each_angle+",selecteDiff="+selecteDiff);
                }
                selecteDiff = diff;
            }
            startZoom = startZoom + zoomMoveValue;

        }
    }
    private float[] getPointFromAngleAndRadius(float angle, float radius) {
        double x = radius * Math.cos(angle * Math.PI / 180) + mCenterX;
        double y = radius * Math.sin(angle * Math.PI / 180) + mCenterY;
        return new float[]{(float) x, (float) y};
    }
}