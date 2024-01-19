package com.android.camera.util;

import android.content.Context;
import android.graphics.Rect;

import com.android.camera.exif.ExifInterface;

import java.util.HashMap;
import java.util.List;

public class UsageStatistics {

    public static final String COMPONENT_GALLERY = "Gallery";
    public static final String COMPONENT_CAMERA = "Camera";
    public static final String COMPONENT_EDITOR = "Editor";
    public static final String COMPONENT_IMPORTER = "Importer";
    public static final String COMPONENT_LIGHTCYCLE = "Lightcycle";
    public static final String COMPONENT_PANORAMA = "Panorama";
    public static final String COMPONENT_GCAM = "GCam";

    public static final String TRANSITION_BACK_BUTTON = "BackButton";
    public static final String TRANSITION_UP_BUTTON = "UpButton";
    public static final String TRANSITION_PINCH_IN = "PinchIn";
    public static final String TRANSITION_PINCH_OUT = "PinchOut";
    public static final String TRANSITION_INTENT = "Intent";
    public static final String TRANSITION_ITEM_TAP = "ItemTap";
    public static final String TRANSITION_MENU_TAP = "MenuTap";
    public static final String TRANSITION_BUTTON_TAP = "ButtonTap";
    public static final String TRANSITION_SWIPE = "Swipe";

    public static final String ACTION_CAPTURE_START = "CaptureStart";
    public static final String ACTION_CAPTURE_FAIL = "CaptureFail";
    public static final String ACTION_CAPTURE_DONE = "CaptureDone";

    public static final String ACTION_STITCHING_START = "StitchingStart";
    public static final String ACTION_STITCHING_DONE = "StitchingDone";

    public static final String ACTION_FOREGROUNDED = "Foregrounded";
    public static final String ACTION_OPEN_FAIL = "OpenFailure";
    public static final String ACTION_START_PREVIEW_FAIL = "StartPreviewFailure";
    public static final String ACTION_SCREEN_CHANGED = "ScreenChanged";
    public static final String ACTION_FILMSTRIP = "Filmstrip";
    public static final String ACTION_TOUCH_FOCUS= "TouchFocus";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_GALLERY = "Gallery";
    public static final String ACTION_EDIT= "Edit";
    public static final String ACTION_CROP= "Crop";
    public static final String ACTION_PLAY_VIDEO= "PlayVideo";

    public static final String CATEGORY_LIFECYCLE = "AppLifecycle";
    public static final String CATEGORY_BUTTON_PRESS = "ButtonPress";

    public static final String LIFECYCLE_START = "Start";

    public static final String ACTION_SHARE = "Share";

    public static final long VIEW_TIMEOUT_MILLIS = 0;
    public static final int NONE = -1;

    private static UsageStatistics sInstance;

    public static void initialize(Context context) {}
    public static void setPendingTransitionCause(String cause) {}
    public static void onContentViewChanged(String screenComponent, String screenName) {}
    public static void onEvent(String category, String action, String label) {};
    public static void onEvent(String category, String action, String label, long optionalValue) {};
    public static void onEvent(String category, String action, String label, long optionalValue,
                               String fileNameHash) {};
    public static void onEvent(String category, String action, String label,
                               long optionalValue, String fileNameHash, String parameters) {}
    public static String hashFileName(String fileName) {
        return "";
    }
	
    public static UsageStatistics instance() {
        if (sInstance == null) {
            sInstance = new UsageStatistics();
        }
        return sInstance;
    }

    public void mediaInteraction(String ref, int interactionType, int cause, float age) {
    }

    public void mediaView(String ref, long modifiedMillis, float zoom) {
    }

    public void foregrounded(int source, int mode, boolean isKeyguardLocked,
                             boolean isKeyguardSecure, boolean startupOnCreate,
                             long controlTime) {
    }

    public void backgrounded() {
    }

    public void cameraFrameDrop(double deltaMs, double previousDeltaMs) {
    }

    public void jankDetectionEnabled() {
    }

    public void storageWarning(long storageSpace) {
    }

    public void videoCaptureDoneEvent(String ref, long durationMsec, boolean front,
                                      float zoom, int width, int height, long size,
                                      String flashSetting, boolean gridLinesOn) {
    }


    public void cameraFailure(int cause, String info, int agentAction, int agentState) {
    }

    public void changeScreen(int newScreen, Integer interactionCause) {
    }

    public void controlUsed(int control) {
    }

    public void reportMemoryConsumed(HashMap memoryData, String reportType) {
    }
}
