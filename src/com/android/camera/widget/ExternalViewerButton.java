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

package com.android.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;

import com.android.camera.CaptureUI;
import com.android.camera.util.Log;
import org.codeaurora.snapcam.R;

/**
 * This is a custom image button that launches an external viewer. It changes its
 * image resource based on the current viewer type (photosphere, refocus, etc).
 * Also, it tracks whether it is shown by tracking the visibility change of all
 * its ancestors, and keep the visibility of the clings that are registered to the
 * button in sync.
 */
public class ExternalViewerButton extends ImageButton {
    private static final String TAG = "ExtViewerButton";
    private int mState = CaptureUI.BottomPanel.VIEWER_NONE;
    private final SparseArray<Cling> mClingMap;

    public ExternalViewerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mClingMap = new SparseArray<Cling>();
        updateClingVisibility();
    }

    @Override
    protected void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (mClingMap == null) {
            return;
        }
        updateClingVisibility();
    }

    /**
     * Sets cling of the given viewer type for external viewer button.
     */
    public void setClingForViewer(int viewerType, Cling cling) {
        if (cling == null) {
            Log.w(TAG, "Cannot set a null cling for viewer");
            return;
        }
        mClingMap.put(viewerType, cling);
        cling.setReferenceView(this);
    }

    /**
     * Clears cling of the given viewer type for external viewer button.
     */
    public void clearClingForViewer(int viewerType) {
        Cling cling = mClingMap.get(viewerType);
        if (cling == null) {
            Log.w(TAG, "Cling does not exist for the given viewer type: " + viewerType);
        }
        cling.setReferenceView(null);
        mClingMap.remove(viewerType);
    }

    /**
     * Returns a cling for the specified viewer type.
     */
    public Cling getClingForViewer(int viewerType) {
        return mClingMap.get(viewerType);
    }

    /**
     * Sets all the clings to be invisible.
     */
    public void hideClings() {
        for (int i = 0; i < mClingMap.size(); i++) {
            mClingMap.valueAt(i).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Updates the visibility of clings based on whether the button is currently
     * shown.
     */
    public void updateClingVisibility() {
        hideClings();
        if (isShown()) {
            Cling cling = mClingMap.get(mState);
            if (cling != null) {
                cling.adjustPosition();
                cling.setVisibility(View.VISIBLE);
            }
        }
    }
}
