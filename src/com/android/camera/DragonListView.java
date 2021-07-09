/*
 * Copyright (c) 2021 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.camera.SettingsActivity.DragListViewAdapter;

/**
 * A type of <code>DragonListView</code> whose can drag the listview and swap the datas
 */
public class DragonListView extends ListView {
    private static final String Tag = "DragonListView";

    /**
     * Transparency of the drag snapshot(0.0f ~ 1.0f)。
     */
    private static final float DRAG_PHOTO_VIEW_ALPHA = 0.8f;

    /**
     * Time when scrolling up and down
     */
    private static final int SMOOTH_SCROLL_DURATION = 100;

    /**
     * The maximum distance when scrolling up and down can be set
     * @see #setMaxDistance(int)
     * @see #getMaxDistance()
     */
    private int mMaxDistance = 30;

    /**
     * Whether it is dragging
     */
    private boolean mIsDraging;

    /**
     * Coordinate position when pressed
     */
    private int mDownX;
    private int mDownY;

    /**
     * Coordinates when moving
     */
    private int mMoveX;
    private int mMoveY;

    /**
     * The native offset. That is, the position of the upper left corner of
     * the ListView relative to the screen
     */
    private int mRawOffsetX;
    private int mRawOffsetY;

    /**
     * Position in the entry
     */
    private int mItemOffsetX;
    private int mItemOffsetY;

    /**
     * Drag the vertical position range of the snapshot.
     * Determined according to the number of entries and the height of the ListView
     */
    private int mMinDragY;
    private int mMaxDragY;

    /**
     * The height of the dragged item
     */
    private int mDragItemHeight;

    /**
     * The position of the item being dragged
     */
    private int mDragPosition;

    /**
     * Entry position before moving
     */
    private int mFromPosition;

    /**
     * Moved item position
     */
    private int mToPosition;

    /**
     * Window manager for displaying snapshots of entries
     */
    private WindowManager mWindowManager;

    /**
     * Layout parameters for window management
     */
    private WindowManager.LayoutParams mWindowLayoutParams;

    /**
     * Snapshot image of dragged item
     */
    private Bitmap mDragPhotoBitmap;

    /**
     * Snapshot view of the item being dragged
     */
    private ImageView mDragPhotoView;

    public DragonListView(Context context) {
        super(context);
    }

    public DragonListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragonListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // get the Action of the first finger point
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) ev.getX();
                mDownY = (int) ev.getY();
                // get the item index corresponding to the current touch position
                mDragPosition = pointToPosition(mDownX, mDownY);
                // If the touched coordinates are not on the item, in the dividing line,
                // or in the outer area, the invalid value is -1;
                // the area to the right of the width 3/4 can be dragged
                if (mDragPosition == AdapterView.INVALID_POSITION || mDownX < (getWidth() * 1 / 5)) {
                    return super.onTouchEvent(ev);
                }
                mIsDraging = true;
                mToPosition = mFromPosition = mDragPosition;

                mRawOffsetX = (int) (ev.getRawX() - mDownX);
                mRawOffsetY = (int) (ev.getRawY() - mDownY);

                // Start the preliminary work of dragging: display item snapshots
                startDrag();
                break;

            case MotionEvent.ACTION_MOVE:
                mMoveX = (int) ev.getX();
                mMoveY = (int) ev.getY();
                if (mIsDraging) {
                    // Update snapshot location
                    updateDragView();
                    // Update the currently replaced position
                    updateItemView();
                } else {
                    return super.onTouchEvent(ev);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDraging) {
                    // Stop dragging
                    stopDrag();
                } else {
                    return super.onTouchEvent(ev);
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Start dragging
     */
    private boolean startDrag() {
        // The actual position in the ListView, because it involves the reuse of items
        final View itemView = getItemView(mDragPosition);
        if (itemView == null) {
            return false;
        }
        // Drawing cache
        itemView.setDrawingCacheEnabled(true);
        // Retrieve the pictures in the cache
        mDragPhotoBitmap = Bitmap.createBitmap(itemView.getDrawingCache());
        // Clear the drawing cache, otherwise the previous picture will appear when reusing.
        // Or use destroy destroyDrawingCache()
        itemView.setDrawingCacheEnabled(false);

        // hide, In order to prevent screen flicker when hiding,
        // use animation to remove flicker effect
        Animation aAnim = new AlphaAnimation(1f, DRAG_PHOTO_VIEW_ALPHA);
        aAnim.setDuration(50);
        aAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // There is a hidden function in Move. If you press and move quickly,
                // it will appear that the display is hidden again. So judge
                if (mIsDraging && mToPosition == mDragPosition) {
                    itemView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        itemView.startAnimation(aAnim);

        mItemOffsetX = mDownX - itemView.getLeft();
        mItemOffsetY = mDownY - itemView.getTop();
        mDragItemHeight = itemView.getHeight();
        mMinDragY = mRawOffsetY;
        // According to whether the display is complete,
        // set the maximum value that the snapshot can be dragged on the Y axis
        if (isShowAll()) {
            mMaxDragY = mRawOffsetY + getChildAt(getAdapter().getCount() - 1).getTop();
        } else {
            mMaxDragY = mRawOffsetY + getHeight() - mDragItemHeight;
        }
        createDragPhotoView();
        return true;
    }

    /**
     * Determine whether the ListView is all displayed,
     * that is the ListView cannot scroll up and down
     */
    private boolean isShowAll() {
        if (getChildCount() == 0) {
            return true;
        }
        View firstChild = getChildAt(0);
        int itemAllHeight = firstChild.getBottom() - firstChild.getTop() + getDividerHeight();
        return itemAllHeight * getAdapter().getCount() < getHeight();
    }

    /**
     * Create drag-and-drop snapshot
     */
    private void createDragPhotoView() {
        // Get the current window manager
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        // Create layout parameters
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.START;
        // The desired picture is semi-transparent, but setting other values does not see a different effect
        mWindowLayoutParams.format = PixelFormat.TRANSLUCENT;
        // The following parameters can help accurately locate the selected item click position
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowLayoutParams.windowAnimations = 0; // no Animation
        mWindowLayoutParams.alpha = DRAG_PHOTO_VIEW_ALPHA; // Slightly transparent

        mWindowLayoutParams.x = mDownX + mRawOffsetX - mItemOffsetX;
        mWindowLayoutParams.y = adjustDragY(mDownY + mRawOffsetY - mItemOffsetY);

        mDragPhotoView = new ImageView(getContext());
        mDragPhotoView.setImageBitmap(mDragPhotoBitmap);
        mWindowManager.addView(mDragPhotoView, mWindowLayoutParams);
    }

    /**
     * Correct the value of Drag to prevent it from crossing the boundary
     */
    private int adjustDragY(int y) {
        if (y < mMinDragY) {
            return mMinDragY;
        } else if (y > mMaxDragY) {
            return mMaxDragY;
        }
        return y;
    }

    /**
     * Get the entry corresponding to the ListView according to the position in the Adapter
     */
    private View getItemView(int position) {
        if (position < 0 || position >= getAdapter().getCount()) {
            return null;
        }
        int index = position - getFirstVisiblePosition();
        return getChildAt(index);
    }

    /**
     * Update the location of the snapshot
     */
    private void updateDragView() {
        if (mDragPhotoView != null) {
            mWindowLayoutParams.y = adjustDragY(mMoveY + mRawOffsetY - mItemOffsetY);
            mWindowManager.updateViewLayout(mDragPhotoView, mWindowLayoutParams);
        }
    }

    /**
     * Update item location, display, etc.
     */
    private void updateItemView() {
        int position = pointToPosition(mMoveX, mMoveY);
        if (position != AdapterView.INVALID_POSITION) {
            mToPosition = position;
        }

        // Change the position and change the display
        if (mFromPosition != mToPosition) {
            if (exchangePosition()) {
                View view = getItemView(mFromPosition);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                view = getItemView(mToPosition);
                if (view != null) {
                    view.setVisibility(View.INVISIBLE);
                }
                mFromPosition = mToPosition;
            }
        }

        // If there is less than one item at the current position,
        // scroll up or down. And set the scroll speed according to the distance from the boundary
        int dragY = mMoveY - mItemOffsetY;
        if (dragY < mDragItemHeight) {
            int value = Math.max(0, dragY); // Anti-cross-border
            float percent = estimatePercent(mDragItemHeight, 0, value);
            int distance = estimateInt(0, -mMaxDistance, percent);
            smoothScrollBy(distance, SMOOTH_SCROLL_DURATION);
        } else if (dragY > getHeight() - 2 * mDragItemHeight) {
            int value = Math.max(0, getHeight() - dragY - mDragItemHeight); // Anti-cross-border
            float percent = estimatePercent(mDragItemHeight, 0, value);
            int distance = estimateInt(0, mMaxDistance, percent);
            smoothScrollBy(distance, SMOOTH_SCROLL_DURATION);
        }
    }

    /**
     * Stop dragging
     */
    private void stopDrag() {
        // Show items on coordinates
        View view = getItemView(mToPosition);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        // Remove snapshot
        if (mDragPhotoView != null) {
            mWindowManager.removeView(mDragPhotoView);
            mDragPhotoView.setImageDrawable(null);
            mDragPhotoBitmap.recycle();
            mDragPhotoBitmap = null;
            mDragPhotoView = null;
        }
        mIsDraging = false;
    }

    /**
     * Reposition
     */
    private boolean exchangePosition() {
        int itemCount = getAdapter().getCount();

        if (mFromPosition >= 0 && mFromPosition < itemCount
                && mToPosition >= 0 && mToPosition < itemCount) {
            getAdapter().swapData(mFromPosition, mToPosition);
            return true;
        }
        return false;
    }


    /**
     * Based on the percentage, estimate the value within the specified range
     */
    private int estimateInt(int start ,int end, @FloatRange(from = 0.0f, to = 1.0f) float percent) {
        return (int) (start + percent * (end - start));
    }

    /**
     * Estimate the percentage of a given value within the specified range
     * @param start Starting value
     * @param end Last value
     * @param value Value to be estimated
     * @return 0.0f ~ 1.0f。If the range is not specified,
     *         or the given value is not within the range, -1 is returned
     */
    private float estimatePercent(float start, float end, float value) {
        if (start == end
                || (value < start && value < end)
                || (value > start && value > end)){
            return -1;
        }
        return (value - start) / (end - start);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof DragListViewAdapter)){
            throw new RuntimeException("Please use the DragListViewAdapter ");
        }
        super.setAdapter(adapter);
    }

    @Override
    public DragListViewAdapter getAdapter(){
        return (DragListViewAdapter) super.getAdapter();
    }
}
