/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023-2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import com.android.camera.util.CameraUtil;
import android.app.UiAutomation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import com.android.camera.util.Log;
import android.util.Range;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import org.codeaurora.snapcam.R;
import android.view.KeyEvent;
import com.android.camera.CameraActivity;
import com.android.camera.SettingsManager;
import com.android.camera.CaptureModule;
import android.content.SharedPreferences;
import com.android.camera.CameraSettings;
import android.preference.PreferenceManager;
import android.content.Context;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CameraMetadata;
import com.android.camera.Storage;
import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.support.test.InstrumentationRegistry;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import android.view.View;
import android.media.ExifInterface;
import android.hardware.camera2.TotalCaptureResult;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PersistUtil;
import com.android.camera.ui.ProMode;
import android.util.DisplayMetrics;
import com.android.camera.util.VendorTagUtil;
import android.util.Size;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.Set;
import android.graphics.Rect;
import android.preference.ListPreference;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.FileWriter;
import com.android.camera.util.VendorTagUtil;
import android.hardware.camera2.CameraCaptureSession;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.graphics.Point;
import com.android.camera.Storage;
import android.hardware.camera2.CaptureRequest;
import com.android.camera.widget.FilmstripLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.LinearLayout;


public class TestBase{
    private String TAG = "autoTest_TestBase";
    private Map<String, String> mCameraIdMapping = new HashMap();
    private static UiAutomation uiAutomation;
    public static ActivityTestRule<CameraActivity> mActivityRule;
    public static CameraActivity mActivity;
    public static  String mNormalPath = "/storage/emulated/0/DCIM/Camera/";
    public static final String NORMAL_IMG = ".jpg";
    public static Intent mMainIntent = new Intent("android.intent.action.MAIN");
    public Intent mVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    public Intent mImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    public static final int SNAPSHOT_NORMAL_DURATION = 6000;
    public static final int SNAPSHOT_NORMAL_FLAH_OFF = 4000;
    public static final int SNAPSHOT_NORMAL_NUM = 1;
    public static final int OPEN_CAMERA_DURATION = 3000;
    public static final int SMALL_WAIT_DURATION = 1000;
    public static final int SAVE_VIDEO_DURATION = 3000;
    public static final int VIDEO_DURATION = 6000;
    public static final int VIDEO_LENGTH = 10240;
    public static final int NORMAL_PIC_LENG_MIN = 10;
    public static final int SWIPE_STEP = 500;
    public static final int FLASH_ON = 9;
    public static final int FLASH_OFF = 16;
    public static final int FLASH_AUTO_OFF = 24;
    public static final int FLASH_AUTO_ON = 25;
    public static final int CONTROL_MODE_HDR = 18;
    public static final int HDR_SCENE_OFF = 0;
    public static final int CONTROL_MODE_AUTO = 1;
    private static final String OUTPUT_JSON = "/data/data/org.codeaurora.snapcam/files/testResult.json";
    public static final String INPUT_JSON = "/data/data/org.codeaurora.snapcam/files/autoTest.json";
    public static final int FLASH_STATE_READY = 2;
    public static final int FLASH_STATE_FIRED = 3;
    public int[] mShutterLoc = new  int[2];
    public int[] mFlashLoc = new  int[2];

    public int[] mVideoPhotoSizeLoc = new  int[2];

    public int[] mVideoFpsLoc = new  int[2];
    public int[] mHdrLoc = new  int[2];
    public int[] mZoomBarLoc = new  int[2];
    public int[] mZoomValueLoc = new  int[2];
    public int[] mZoomTextLoc = new  int[2];
    public int[] mEVTextLoc = new  int[2];
    public int[] mZoomUWLoc = new  int[2];
    public int[] mZoomTelLoc = new  int[2];
    public int[] mVerticlEVMaxLoc = new  int[2];


    public int[] mSwitchLoc = new  int[2];
    public int[] mEVLoc = new  int[2];
    public int[] mFocDisLoc = new  int[2];
    public int[] mShutSpeedLoc = new  int[2];
    public int[] mWBLoc = new  int[2];
    public int[] mISOLoc = new  int[2];
    public int[] mVideoLoc = new  int[2];
    public int[]mPauseLoc = new  int[2];
    public int[]mModeLoc = new  int[2];
    public int[]mProLayoutLoc = new  int[2];
    public int[]mSettingLoc = new  int[2];
    public int[]mCancelLoc = new  int[2];

    public int[]mReviewCancelLoc = new  int[2];
    public int[]mDoneLoc = new  int[2];
    public int[]mRetakeLoc = new  int[2];
    public int[]mThumbLoc = new  int[2];
    public int[]mFilterLoc = new  int[2];
    private int[]mDepthSwitchLoc = new  int[2];
    private int[]mDepthSwitchRLoc = new  int[2];
    private int[]mDepthSettingLoc = new  int[2];
    private int[]mDepthBarLoc = new  int[2];
    private int[]mDepthBarMaxLoc = new  int[2];

    public int mCurrentImgNum;
    public int mZoomBarWidth;
    public int mZoomValueWidth;
    private String mPicWidInSet,mPicHeiInSet,mFrameRate,mVideoWidInSet,mVideoHeiInSet;
    private float mOldZoomstr = 0;
    private Rect mOldZoomRegion;
    private int mLongShotNum = 0;
    private ExifInterface mCurrentexif;
    private TotalCaptureResult mCurrentCaptureResult;
    private CaptureResult mCurrentPreviewResult;
    public CaptureModule mCaptureModule;
    public CaptureUI mCaptureUI;
    private ProMode mProMode;
    private SettingsManager mSettingsManager;
    private CharSequence[] isovalue,evvalue,wbvalue;
    public int swipevalue;
    private boolean updateJson = false;
    private String jsonChildNm = null;
    private String jsonParentNm = null;
    private boolean testResult = false;
    private boolean flashInZslResult = true;
    private boolean longshotInZslResult = true;
    private String flashvalue = "FAIL";
    private String longshotvalue = "FAIL";
    private boolean mSupported = true;
    private String testPass = "PASS";
    private String testFail = "FAIL";
    public HashMap<String,HashMap<String,Long>> performenceValues = new HashMap<String,HashMap<String,Long>>();
    public boolean isPerformenceTest = false;
    public boolean isOpenFromIntent = false;
    private Uri mUri;
    private int methodLevel = 5;
    private boolean checkfps = false;
    public HashMap<String, int[]> mModeIconL = new HashMap<>();
    public HashMap<String, int[]> mModeIconR = new HashMap<>();
    public HashMap<String, int[]> mIconLoc = new HashMap<>();
    public HashMap<String, int[]> mRecordLoc = new HashMap<>();
    public HashMap<String, int[]> mProLoc = new HashMap<>();
    public HashMap<String, int[]> mDepthLoc = new HashMap<>();
    public HashMap<String, int[]> mThumLoc = new HashMap<>();
    public static String  functionTestMode;
    public static String  functionTestItem;
    public static String  functionTestItemDel;
    public DisplayMetrics displayMetrics;
    private int checkModeIndex = 5;
    private int burstNum = 0;


    public static void init(){
        uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mActivityRule = new ActivityTestRule<>(CameraActivity.class);

    }

    public void OpenAndResetCamera() throws Exception{
        openCameraByIntent(mMainIntent);
        mActivity.mSettingsManager.restoreSettings();
        Thread.sleep(SMALL_WAIT_DURATION);
        //testSettingIcon("0", CaptureModule.CameraMode.DEFAULT);
        isOpenFromIntent = false;
    }
    public void OpenCamera() throws Exception{
        openCameraByIntent(mMainIntent);
        // checkPreview(cameraid, mode);
    }
    private int[] getViewLoction(View view){
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        int[] viewLoc = new int[2];
        view.getLocationInWindow(viewLoc);
        viewLoc[0] = viewLoc[0]+width/2;
        viewLoc[1] = viewLoc[1]+height/2;
        return viewLoc;

    }
    public void getUILoc(){
        if(mSettingLoc[0] != 0){
            return;
        }
        View mShutter = mActivity.findViewById(R.id.shutter_button);
        View mVideoPhotoSize = mActivity.findViewById(R.id.video_photo_size);
        View mVideoFps = mActivity.findViewById(R.id.video_fps);
        View mFlash = mActivity.findViewById(R.id.flash_button);
        View mHdr = mActivity.findViewById(R.id.scene_mode_hdr);
        View mZoomBar = mActivity.findViewById(R.id.zoom_seekbar);
        View mZoomValue = mActivity.findViewById(R.id.zoom_value_text);
        View mSwitch = mActivity.findViewById(R.id.front_back_switcher);
        View mVideoShutter = mActivity.findViewById(R.id.video_button);

        // View mModeItem = mActivity.findViewById(R.id.camera2_mode_item);
        View mSettingsButton = mActivity.findViewById(R.id.settings);
        View mThumbnail = mActivity.findViewById(R.id.preview_thumb);
        View mFilterwitcher = mActivity.findViewById(R.id.filter_mode_switcher);
        View mEVText =  mActivity.findViewById(R.id.ev_text);
        View mEVVerticalBar =  mActivity.findViewById(R.id.ev_verticalbar);
        mZoomBarWidth = mZoomBar.getWidth();
        mZoomValueWidth = mZoomValue.getWidth();
        mShutterLoc = getViewLoction(mShutter);
        mIconLoc.put("Shutter",mShutterLoc);
        mFlashLoc = getViewLoction(mFlash);
        mIconLoc.put("Flash",mFlashLoc);
        mHdrLoc = getViewLoction(mHdr);
        mIconLoc.put("Hdr",mHdrLoc);
        mZoomBar.getLocationInWindow(mZoomBarLoc);
       // mZoomBarLoc = getViewLoction(mZoomBar);
        mIconLoc.put("ZoomBarMin",mZoomBarLoc);
        int[]maxzoom = {mZoomBarWidth,mZoomBarLoc[1]};
        mIconLoc.put("ZoomBarMax", maxzoom);
        mZoomValueLoc = getViewLoction(mZoomValue);
        mIconLoc.put("ZoomValue",mZoomValueLoc);
        mSwitchLoc = getViewLoction(mSwitch);
        mIconLoc.put("SwitchCam",mSwitchLoc);
        mVideoLoc = getViewLoction(mVideoShutter);
        mSettingLoc = getViewLoction(mSettingsButton);
        mIconLoc.put("Setting",mSettingLoc);
        mFilterLoc = getViewLoction(mFilterwitcher);
        mThumbLoc = getViewLoction(mThumbnail);
        mIconLoc.put("Thumb",mThumbLoc);
        mRecordLoc.put("Flash",mFlashLoc);
        mVideoPhotoSizeLoc = getViewLoction(mVideoPhotoSize);
        mIconLoc.put("VideoPhotoSize",mVideoPhotoSizeLoc);
        mVideoFpsLoc = getViewLoction(mVideoFps);
        mIconLoc.put("VideoFps",mVideoFpsLoc);
        mRecordLoc.put("ZoomValue",mZoomValueLoc);
        mRecordLoc.put("ZoomBarMax", maxzoom);
        int[] backicon = new int[]{20,100};
        mThumLoc.put("BackIcon",backicon);
        View zoomText = mActivity.findViewById(R.id.zoom_text_layout);
        mZoomTextLoc = getViewLoction(zoomText);
        mEVTextLoc = getViewLoction(mEVText);
        TextView mZoomWText = (TextView) mActivity.findViewById(R.id.zoom_w);
        int wZoomlen = mZoomWText.getMeasuredWidth();
        int[] zoomUW =  new int[]{mZoomTextLoc[0] - wZoomlen ,mZoomTextLoc[1]};
        int[] zoomTel =  new int[]{mZoomTextLoc[0] + wZoomlen ,mZoomTextLoc[1]};
        mIconLoc.put("ZoomW",mZoomTextLoc);
        mIconLoc.put("ZoomUW",zoomUW);
        mIconLoc.put("ZoomTel",zoomTel);
        Log.i(TAG,"thumb="+mThumbLoc[0]+"*"+mThumbLoc[1]+",settingloc="+mSettingLoc[0]+"*"+mSettingLoc[1]
                +",mEVTextLoc="+mEVTextLoc[0]+"*"+mEVTextLoc[1]+"shutterloc="
                + mShutterLoc[0]+"*"+ mShutterLoc[1]);
        getModeLoc();
    }
    public void getDepthUILoc(){
        mDepthSettingLoc = mCaptureUI.getSettingMargin();
        mDepthBarLoc = mCaptureUI.getBarMargin();
        mDepthSettingLoc[0] = displayMetrics.widthPixels -mDepthSettingLoc[0]-10;
        mDepthBarLoc[1] = displayMetrics.heightPixels - mDepthBarLoc[1] + 100;
        mDepthBarLoc[1] = mModeLoc[1] - 50;
        mDepthBarLoc[0] = mDepthBarLoc[0]+200;
        mDepthBarMaxLoc[0] =  displayMetrics.widthPixels  - 200;
        mDepthBarMaxLoc[1] =  mDepthBarLoc[1];
        mDepthLoc.put("depthSetting",mDepthSettingLoc);
        mDepthLoc.put("depthMinBar",mDepthBarLoc);
        mDepthLoc.put("depthMaxBar",mDepthSwitchRLoc);
    }
    public void getIconLoctionInPro(){
        View mProLayout = mActivity.findViewById(R.id.pro_mode_layout);
        mProLayout.getLocationInWindow(mProLayoutLoc);
        String[]proList = {"EV","FocusDistance","ShutterSpeed","WB","ISO"};
        int proItem = proList.length;
        int prolen = CameraUtil.metrics.widthPixels/proItem;
        for(int j =0; j < proItem;j++){
            int x = j*prolen +prolen/2;
            int[] proloc = {x,mProLayoutLoc[1]};
            mProLoc.put(proList[j],proloc);
        }
        int[] promin = {mProMode.getCurveLeft(),mProMode.getCurveY()};
        int[]promid = {mProMode.getMidX(),mProMode.getMidY()};
        int[]promax = {mProMode.getCurveRight(),mProMode.getCurveY()};
        View mShutter = mActivity.findViewById(R.id.shutter_button);
        int[] snaploc  = new int[2];
        mShutter.getLocationInWindow(snaploc);
        mProLoc.put("proMinValue",promin);
        mProLoc.put("proMidValue",promid);
        mProLoc.put("proMaxValue",promax);
        mProLoc.put("Shutter",snaploc);
        mProLoc.put("Flash",mFlashLoc);
        mProLoc.put("Setting",mSettingLoc);
        mProLoc.put("Thumb",mThumbLoc);
    }
    public void getModeLoc(){
        View mCameraModeText = mActivity.findViewById(R.id.mode_text);
        View mModeLayout = mActivity.findViewById(R.id.mode_select_layout);
        mModeLayout.getLocationInWindow(mModeLoc);
        mCaptureModule.getCameraModeList().size();
        List<String> modeList = mCaptureModule.getCameraModeList();
        int modeListSize = modeList.size();
        int modelen = mCameraModeText.getMeasuredWidth();
        int modehigh = mCameraModeText.getMeasuredHeight();
        displayMetrics = CameraUtil.metrics;
        for(int i = 0;i <modeListSize; i++){
            int x = i * modelen + modelen/2;
            int[] modemloc = {x,mModeLoc[1]};
            String mode = modeList.get(i);
            if( x > displayMetrics.widthPixels){
                for(int j = 0;j < modeListSize;j++) {
                    int y = displayMetrics.widthPixels - modelen / 2 - j*modelen;
                    String modenm =  modeList.get(modeListSize-j-1);
                    if(modenm.equals(mode)){
                        int[] locr = {y,mModeLoc[1]};
                        mModeIconR.put(modenm,locr);
                        Log.d(TAG,"mModeIconR put mode="+modenm+",loc="+locr[0]+"*"+locr[1]);
                        mIconLoc.put(modenm,locr);
                        break;
                    }
                }
            }else {
                mModeIconL.put(mode, modemloc);
                mIconLoc.put(mode, modemloc);
            }


        }
        // mModeItem.getLocationInWindow(mModeItemLoc);

        int value = displayMetrics.widthPixels < displayMetrics.heightPixels ? displayMetrics.widthPixels : displayMetrics.heightPixels;
        swipevalue = value / 2 ;
        int[] modetext = {swipevalue,mModeLoc[1]};
        int[] modeswipe = {swipevalue,swipevalue};
        mModeIconR.put("SlideModeTxt",modetext);
        // mIconLoc.put("SwipeModeL",modeswipe);
        //mIconLoc.put("SwipeModeR",modeswipe);
        mIconLoc.put("Focus",modeswipe);
    }


    private void getRecordingLoc(){
        View mPauseButton = mActivity.findViewById(R.id.video_pause);
        mPauseButton.getLocationInWindow(mPauseLoc);
    }
    public void getIntentLoc(){
        View mCancelButn = mActivity.findViewById(R.id.cancel_button);
        View mReviewCancelButton = mActivity.findViewById(R.id.preview_btn_cancel);
        View mDoneButton = mActivity.findViewById(R.id.done_button);
        View mRetakeButton = mActivity.findViewById(R.id.preview_btn_retake);
        mCancelButn.getLocationInWindow(mCancelLoc);
        mReviewCancelButton.getLocationInWindow(mReviewCancelLoc);
        mDoneButton.getLocationInWindow(mDoneLoc);
        mRetakeButton.getLocationInWindow(mRetakeLoc);
    }

    public static void updateAndSavejson(String inputJson,String childNm,String parentNm,String value){
        JSONObject mObj= CameraUtil.getJsonObj(inputJson);
        if (mObj == null) {
            Log.e("autotest_updateAndSavejson","Json file not exit,will not save the test result,inputJson="+inputJson);
            return;
        }
        if(childNm != null && parentNm != null) {
            try {
                JSONArray array = mObj.getJSONArray("runTest");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject test = array.getJSONObject(i);
                    String name = test.getString("testCase");
                    if (name.equals(childNm)) {
                        Log.i("autotest_updateAndSavejson",childNm +"is found,will update the parentNm="+parentNm+" with" +
                                "value ="+value);
                        test.put(parentNm, value);
                        break;
                    }
                }

            } catch (Exception e) {
                Log.e("autotest_updateAndSavejson", "updatejsonobj e= " + e);
            }
        }
        try{
            FileWriter fileWriter = new FileWriter(OUTPUT_JSON);
            fileWriter.write(mObj.toString());
            fileWriter.flush();
        }catch (Exception e){
            Log.e("autotest_updateAndSavejson"," writejsonobj e= "+e);
        }
    }
    public  void updatePerformenceJson(String jsonFile,HashMap<String,HashMap<String,Long>> values,int times) {
        JSONObject mObj = CameraUtil.getJsonObj(jsonFile);
        if (mObj == null || values == null) {
            Log.e(TAG, "Json file not exit,will not save the test result.jsonFile is " + jsonFile
                    +"mobj="+mObj+",values="+values);
            return;
        }
        try {
            for (String jsonArray : values.keySet()) {
                JSONArray array = mObj.getJSONArray(jsonArray);
                Log.i(TAG,"jsonArray ="+jsonArray);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int count = obj.getInt("testTimes");
                    if (times == count) {
                        HashMap timeValue = values.get(jsonArray);
                        Set<String> keySet = timeValue.keySet();
                        for (String item :keySet) {
                            obj.put(item, timeValue.get(item));
                            Log.i(TAG, "put item=" + item + ",count=" + count + ",timeValue.get(item)=" + timeValue.get(item) );
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "updatejsonobj e= " + e);
        }
        saveJson(jsonFile, mObj);
    }

    public void saveJson(String jsonFile, JSONObject obj) {
        try {
            FileWriter fileWriter = new FileWriter(jsonFile);
            fileWriter.write(obj.toString());
            fileWriter.flush();
        } catch (Exception e) {
            Log.e(TAG, " writejsonobj e= " + e);
        }

    }
    public void runPhotoCase(String cameraId,CaptureModule.CameraMode mode) throws Exception {
        Log.i(TAG, "testphotocase mode=" + mode + ",id=" + cameraId);
        checkPreview(cameraId, mode);
        if (!testResult) {
            return;
        }
        if (testItem("testSettingIcon")) {
            if (isOpenFromIntent  && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testSettingIcon(cameraId, mode);
        }
        if (testItem("testHdr") ) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testHdr(mode);
        }
        if (testItem("testFlash")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testFlash(cameraId, mode, false);

        }
        if (testItem("testZoom")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testZoom(mode);
        }
        if (testItem("testVerticalEV")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testVerticalEV(mode);
        }
        if (testItem("testToggleBackFront")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            if (cameraId.equals("1")) {
                testToggleBackFront(mode, false);
            } else {
                testToggleBackFront(mode, true);
            }

        }
        if(testItem("testFilter")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testFilter(mode);
        }
        if(mode == CaptureModule.CameraMode.DEPTH) {
            if (testItem("testDepthUI")) {
                testDepthUI();
            }
            return;
        }
        if (testItem("testSnapshot") ) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testSnapshot(mode);
        }
        if (isOpenFromIntent) {
            return;
        }
        if (testItem("testResolutionInPrev")) {
            testResolutionInPrev(cameraId, mode);
        }
        if (testItem("testLongShot")) {
            testLongShot(cameraId,mode);
        }
        if (testItem("testPictureSize")) {
            testPictureSize(cameraId, mode);
        }

        if(testItem("testPicFormat")) {
            testPicFormat(mode);
        }

        if(testItem("testTAF") && !cameraId.equals("1")){
            testTAF(mode);
        }
        if(testItem("testThumbnail")){
            testThumbnail(cameraId ,mode);
        }

        if (mode != CaptureModule.CameraMode.PRO_MODE) {
            if (testItem("testMFNR")) {
                testMFNR(cameraId, mode);
            }
            if(testItem("testBurstLimit")){
                testBurstLimit(mode);
            }
            if(testItem("testMixedHDR")) {
                testMixedHDR(mode);
            }
        }else {
            if (testItem("testEV")) {
                testEV(mode);
            }
            if (testItem("testWB")) {
                testWB();
            }
            if (testItem("testISO")) {
                testISO();
            }
            if (testItem("testShutterSpeed")) {
                testShutterSpeed();
            }
            if (testItem("testFocusDistance")) {
                testFocusDistance();
            }
            if (testItem("testAllInPro")) {
                testAllInPro();
            }
            if (testItem("testCamID") && !cameraId.equals("1")) {
                testCamID(mode);//This testcase should be the last one due to the "checkpreview" will return if failed.
            }
        }
        if (mode == CaptureModule.CameraMode.DEFAULT) {

            if (testItem("testZSL")) {
                testZSL(cameraId, mode);
            }
            if (testItem("testEIS")) {
                testEIS(mode);
            }
            if(testItem("testSelectMode") && !cameraId.equals("1")) {
                testSelectMode(mode);
            }
            if(testItem("testSaveRaw")) {
                testSaveRaw(mode);
            }

            if(testItem("testLivePreview")){
                testLivePreview(mode);
            }
            if(testItem("testExtendZoom")){
                testExtendZoom(mode);
            }
            if (testItem("testCamID") && !cameraId.equals("1")) {
                testCamID(mode);//This testcase should be the last one due to the "checkpreview" will return if failed.
            }
        }


    }

    public void runVideoCase(String cameraId,CaptureModule.CameraMode mode) throws Exception {
        checkPreview(cameraId, mode);
        if (!testResult) {
            return;
        }
        if (testItem("testRecording")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testRecording(mode, true);
        }
        if (testItem("testSettingIcon")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testSettingIcon(cameraId, mode);
        }
        if (testItem("testHdr")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testHdr(mode);
        }
        if (testItem("testFlash")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testFlash(cameraId, mode, false);

        }
        if (testItem("testZoom")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testZoom(mode);
        }
        if (testItem("testVerticalEV")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mImageIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testVerticalEV(mode);
        }
        if (testItem("testVideoPauseAndResume")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testVideoPauseAndResume();

        }
        if (testItem("testToggleBackFront")) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            if (cameraId.equals("1")) {
                testToggleBackFront(mode, false);
            } else {
                testToggleBackFront(mode, true);
            }
        }
        if(testItem("testFilter") ) {
            if (isOpenFromIntent && mCaptureModule.getPaused()) {
                openCameraByIntent(mVideoIntent);
                Thread.sleep(OPEN_CAMERA_DURATION);
            }
            testFilter(mode);
        }

        if (isOpenFromIntent) {
            return;
        }
        if (testItem("testResolutionInPrev")) {
            testResolutionInPrev(cameraId, mode);
        }
        if (testItem("testFpsInPrev")) {
            testFpsInPrev(cameraId, mode);
        }
        if(testItem("testTAF") && !cameraId.equals("1") && mode != CaptureModule.CameraMode.CINEMATIC){
            testTAF(mode);
        }
        if(testItem("testThumbnail")){
            testThumbnail(cameraId ,mode);
        }

        if (testItem("testVideoSizeAndFrameRate")) {
            testVideoSizeAndFrameRate(cameraId, mode);
        }

        if (mode != CaptureModule.CameraMode.HFR && !cameraId.equals("1")
                && mode != CaptureModule.CameraMode.CINEMATIC) {
            if (testItem("testCamID")) {
                testCamID(mode);
            }
        }
        if(mode == CaptureModule.CameraMode.VIDEO) {
            if(testItem("testPicFormat")) {
                testPicFormat(mode);
            }
            if (testItem("testEIS")) {
                testEIS(mode);
            }
            if(testItem("testMixedHDR")) {
                testMixedHDR(mode);
            }
            if(testItem("testSelectMode") && !cameraId.equals("1")) {
                testSelectMode(mode);
            }
            if(testItem("testDynamicFPS")) {
                testDynamicFPS(mode);
            }
            if(testItem("testMediaCodec")){
                testMediaCodec(cameraId,mode);
            }
            if (testItem("testCamID") && !cameraId.equals("1")) {
                testCamID(mode);
            }
        }

    }
    private void testSnapshot(CaptureModule.CameraMode mode)throws Exception {
        testSnapshot(mode,true);
    }
    public void testSnapshot(CaptureModule.CameraMode mode,boolean pressDone) throws Exception {
        updateJson(6,null);
        //snapByKeyCode();
        //snapByButton();
        snapByLocation(mode);
        if(pressDone && isOpenFromIntent){
            pressDone();
        }
        if(testResult) updateJson(6,testPass);
        else{
            updateJson(6,testFail);
        }
    }
    public void testToggleBackFront(CaptureModule.CameraMode mode)throws Exception {
        testToggleBackFront(mode,true);
    }
    public void testToggleBackFront(CaptureModule.CameraMode mode,boolean fromBack)throws Exception{
        updateJson(5,null);
        if(mode == CaptureModule.CameraMode.CINEMATIC || mode == CaptureModule.CameraMode.RTB ||
                mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.DEPTH) {
            View mToggle = mActivity.findViewById(R.id.front_back_switcher);
            if (mToggle.getVisibility() != View.VISIBLE) {
                testResult = true;
                mSupported = false;
            } else {
                testFail = getFailStr("BackFontSwitch icon",mToggle.getVisibility(),View.INVISIBLE);
            }
            if(testResult) updateJson(5,testPass);
            else{
                updateJson(5,testFail);
            }
            return;
        }
        executeShellCommand("input tap "+ mSwitchLoc[0]  +" "+mSwitchLoc[1]);
        if(fromBack) {
            checkPreview("1", mode);
        }else{
            if(mode != CaptureModule.CameraMode.HFR) {
                checkPreview("0", mode);
            }else{
                checkPreview("2", mode);
            }
        }
        if(isOpenFromIntent){
            if(isVideoMode(mode)){
                testRecording(mode,true);
                openCameraByIntent(mVideoIntent);
            }else{
                testSnapshot(mode);
                openCameraByIntent(mImageIntent);
            }
            checkPreview("1",mode);
        }

        if(isPerformenceTest) {
            HashMap<String, Long> toggleBackToFront = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("backToFront", toggleBackToFront);
        }
        executeShellCommand("input tap "+ mSwitchLoc[0]  +" "+mSwitchLoc[1]);
        if(fromBack) {
            if (mode != CaptureModule.CameraMode.HFR) {
                checkPreview("0", mode);
            } else {
                checkPreview("2", mode);
            }
        }else{
            checkPreview("1", mode);
        }
        if(isOpenFromIntent){
            if(isVideoMode(mode)){
                testRecording(mode,true);
            }else{
                testSnapshot(mode);
            }
        }
        if(isPerformenceTest) {
            HashMap<String, Long> toggleFrontToBack = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("frontToBack", toggleFrontToBack);
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    private void testSettingIcon(String cameraid,CaptureModule.CameraMode mode) throws Exception{
        updateJson(5,null);
        if(mode == CaptureModule.CameraMode.DEPTH){
            View mSettingsButton = mActivity.findViewById(R.id.settings);
            if(mSettingsButton.getVisibility() != View.VISIBLE){
                testResult = true;
                mSupported = false;
            }else{
                testFail = getFailStr("settings.getVisibility",mSettingsButton.getVisibility(),"INVISIBLE");
            }
            if(testResult){
                updateJson(5,testPass);
            }else{
                updateJson(5,testFail);
            }
            return;
        }
        executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        Log.i(TAG,"click setting getPaused="+mCaptureModule.getPaused());
        if(!mCaptureModule.getPaused()){
            testFail = getFailStr("mPaused",mCaptureModule.getPaused(),true);
        }else {
            executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
            checkPreview(cameraid, mode);
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    private boolean isVideoMode(CaptureModule.CameraMode mode){
        if(mode == CaptureModule.CameraMode.VIDEO || mode == CaptureModule.CameraMode.HFR ||
                mode == CaptureModule.CameraMode.CINEMATIC ){
            return true;
        }else
            return false;
    }
    private void checkPictureSize(int width, int height){
        String wInExif = mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String hInExif = mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
        if(Integer.valueOf(wInExif) * height != Integer.valueOf(hInExif) * width){
            testResult = false;
            testFail = getFailStr("picture resolution doesnt match ",width,wInExif);
        }
    }
    public void testResolutionInPrev(String cameraid,CaptureModule.CameraMode mode) throws Exception {
        updateJson(5,null);
        Log.i(TAG,"start testResolutionInPrev, mode:" + mode + ",cameraId:" + cameraid + ",testResult:" + testResult);
        if(mode == CaptureModule.CameraMode.CINEMATIC || mode == CaptureModule.CameraMode.RTB){
            View resolution = mActivity.findViewById(R.id.video_photo_size);
            if(resolution.getVisibility() != View.VISIBLE){
                testResult = true;
                mSupported = false;
            }else{
                testFail = getFailStr("resolution.getVisibility",resolution.getVisibility(),"INVISIBLE");
            }
            if(testResult){
                updateJson(5,testPass);
            }else{
                updateJson(5,testFail);
            }
        }else {
            if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.DEFAULT) {
                boolean check11 = true;
                executeShellCommand("input tap " + mVideoPhotoSizeLoc[0] + " " + mVideoPhotoSizeLoc[1]);
                Thread.sleep(SMALL_WAIT_DURATION);
                snapByLocation();
                checkPictureSize(16,9);
                boolean check169 = testResult;
                if(!cameraid.equals("1")) {
                    executeShellCommand("input tap " + mVideoPhotoSizeLoc[0] + " " + mVideoPhotoSizeLoc[1]);
                    Thread.sleep(SMALL_WAIT_DURATION);
                    snapByLocation();
                    checkPictureSize(1, 1);
                    check11 = testResult;
                }
                executeShellCommand("input tap " + mVideoPhotoSizeLoc[0] + " " + mVideoPhotoSizeLoc[1]);
                Thread.sleep(SMALL_WAIT_DURATION);
                snapByLocation();
                checkPictureSize(4,3);
                boolean check43 = testResult;
                if(check169 && check11 && check43){
                    updateJson(5,testPass);
                }else{
                    updateJson(5,testFail);
                }
            } else if(mode == CaptureModule.CameraMode.HFR || mode == CaptureModule.CameraMode.VIDEO){
                testReolutionFpsInPrev(SettingsManager.KEY_VIDEO_QUALITY, mode, mVideoPhotoSizeLoc);
                if(testResult){
                    updateJson(5,testPass);
                }else{
                    updateJson(5,testFail);
                }
            }
        }
    }

    public void testFpsInPrev(String cameraid,CaptureModule.CameraMode mode) throws Exception {
        updateJson(5, null);
        if (mode != CaptureModule.CameraMode.VIDEO && mode != CaptureModule.CameraMode.HFR) {
            View fps = mActivity.findViewById(R.id.video_fps);
            if(fps.getVisibility() != View.VISIBLE){
                testResult = true;
                mSupported = false;
            }else{
                testFail = getFailStr("fps .getVisibility",fps.getVisibility(),"INVISIBLE");
            }
            if(testResult){
                updateJson(5,testPass);
            }else{
                updateJson(5,testFail);
            }
        }else {
            testReolutionFpsInPrev(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE, mode, mVideoFpsLoc);
            if(testResult){
                updateJson(5,testPass);
            }else{
                updateJson(5,testFail);
            }
        }
    }
    private void testReolutionFpsInPrev(String strkey, CaptureModule.CameraMode mode, int[] location) throws Exception {
        CharSequence[] Entryvalues = mSettingsManager.getEntryValues(strkey);
        if(Entryvalues == null || Entryvalues.length == 0){
            Log.i(TAG,"Did not find this setting or did not get its value,key is:"+strkey);
            mSupported = false;
            testResult = true;
        }
        for (int i = 0; i < Entryvalues.length; i++) {
            executeShellCommand("input tap " + location[0] + " " + location[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
            testVideo(mode);
            if(!testResult){
                break;
            }
        }
    }

    public void testFlash(String cameraid,CaptureModule.CameraMode mode,boolean isPerformenceTest) throws Exception {
        updateJson(5,null);
        if(cameraid.equals("1") || mode == CaptureModule.CameraMode.CINEMATIC || mode == CaptureModule.CameraMode.DEPTH){
            View mFlash = mActivity.findViewById(R.id.flash_button);
            if(mFlash.getVisibility() != View.VISIBLE){
                testResult = true;
                mSupported = false;
            }else{
                testFail = getFailStr("flash_button.getVisibility",mFlash.getVisibility(),"INVISIBLE");
            }
            if(testResult){
                updateJson(5,testPass);
            }else{
                updateJson(5,testFail);
            }
        }else {
            boolean intenton = false;
            boolean intentalwayson = false;
            boolean intentoff = false;
            boolean intentauto = false;
            executeShellCommand("input tap " + mFlashLoc[0] + " " + mFlashLoc[1]);
            if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.VIDEO ||
                    mode == CaptureModule.CameraMode.HFR) {
                clickShutterButton(mode);
                checkFlash("on");
                if(isPerformenceTest){
                    HashMap<String,Long> snapShotWithFlashOn = getHashMapValue(mCaptureModule.getHashMapTimes());
                    performenceValues.put("snapFlashOn",snapShotWithFlashOn);
                }
                boolean videoon = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intenton = videoon && testResult;
                    openCameraByIntent(mVideoIntent);
                    checkPreview("0",mode);
                    intenton = intenton && testResult;
                }

                executeShellCommand("input tap "+ mFlashLoc[0] +" "+mFlashLoc[1]);
                clickShutterButton(mode);
                checkFlash("off");
                if(isPerformenceTest){
                    HashMap<String,Long> snapShotWithFlashOff = getHashMapValue(mCaptureModule.getHashMapTimes());
                    performenceValues.put("snapFlashOff",snapShotWithFlashOff);
                }
                boolean videooff = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intentoff = videooff && testResult;
                    if(intenton && intentoff){
                        updateJson(5,testPass);
                    }else {
                        updateJson(5,testFail);
                    }
                    return;
                }
                if(videooff && videoon) updateJson(5,testPass);
                else{
                    updateJson(5,testFail);
                }
            } else {
                snapByLocation();
                checkFlash("on");
                boolean checkon = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intenton = checkon && testResult;
                    openCameraByIntent(mImageIntent);
                    checkPreview("0",mode);
                    intenton = intenton && testResult;
                }
/*                if(!testResult) {
                    return;
                }*/
                if(isPerformenceTest){
                    HashMap<String,Long> snapShotWithFlashOn = getHashMapValue(mCaptureModule.getHashMapTimes());
                    performenceValues.put("snapFlashOn",snapShotWithFlashOn);
                }
                executeShellCommand("input tap " + mFlashLoc[0] + " " + mFlashLoc[1]);
                snapByLocation();
                checkFlash("alwayson");
                boolean checkalwayson = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intentalwayson = checkalwayson && testResult;
                    openCameraByIntent(mImageIntent);
                    checkPreview("0",mode);
                    intentalwayson = intentalwayson && testResult;
                }
                if(isPerformenceTest){
                    HashMap<String,Long> snapShotWithFlashAlwaysOn = getHashMapValue(mCaptureModule.getHashMapTimes());
                    performenceValues.put("snapFlashAlwaysOn",snapShotWithFlashAlwaysOn);
                }
                executeShellCommand("input tap " + mFlashLoc[0] + " " + mFlashLoc[1]);
                snapByLocation();
                checkFlash("off");
                boolean checkoff = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intentoff = checkoff && testResult;
                    openCameraByIntent(mImageIntent);
                    checkPreview("0",mode);
                    intentoff = intentoff && testResult;
                }
                /*   if(!testResult)return;*/
                if(isPerformenceTest){
                    HashMap<String,Long> snapShotWithFlashOff = getHashMapValue(mCaptureModule.getHashMapTimes());
                    performenceValues.put("snapFlashOff",snapShotWithFlashOff);
                }
                executeShellCommand("input tap " + mFlashLoc[0] + " " + mFlashLoc[1]);
                snapByLocation();
                checkFlash("auto");
                boolean checkauto = testResult;
                if(isOpenFromIntent){
                    pressDone();
                    intentauto = checkauto && testResult;
                    if(intentauto && intenton && intentoff && intentalwayson){
                        updateJson(5,testPass);
                    }else {
                        updateJson(5,testFail);
                    }
                    return;
                }
                if(isPerformenceTest){
                    mCaptureModule.resetHashMapTimes();
                }
                if(checkauto && checkoff && checkon && checkalwayson){
                    updateJson(5,testPass);
                }
                else {
                    updateJson(5,testFail);
                }
            }
        }
    }

    public void testHdr()throws Exception {
        executeShellCommand("input tap "+ mHdrLoc[0] +" "+mHdrLoc[1]);
        snapByLocation();
        checkHdr(CONTROL_MODE_HDR);
        executeShellCommand("input tap "+ mHdrLoc[0] +" "+mHdrLoc[1]);
        snapByLocation();
        checkHdr(HDR_SCENE_OFF);
    }
    public void testHdr(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        View mHdr = mActivity.findViewById(R.id.scene_mode_hdr);
        if (!mCaptureUI.showHDRScene()) {
            mSupported = false;
            if(mHdr.getVisibility() != View.VISIBLE){
                testResult = true;
            }else{
                testResult =false;
            }
            if(testResult) updateJson(5,testPass);
            else{
                updateJson(5,testFail);
            }
            return;
        }
        executeShellCommand("input tap "+ mHdrLoc[0] +" "+mHdrLoc[1]);
        snapByLocation();
        checkHdr(CONTROL_MODE_HDR);
        boolean check1 = testResult;
        if(isOpenFromIntent){
            pressDone();
            openCameraByIntent(mImageIntent);
            checkPreview("0",mode);
        }
        executeShellCommand("input tap "+ mHdrLoc[0] +" "+mHdrLoc[1]);
        snapByLocation();
        checkHdr(HDR_SCENE_OFF);
        boolean check2 = testResult;
        if(isOpenFromIntent){
            pressDone();
        }
        if(check1 && check2) {
            updateJson(5,testPass);
        }else{
            updateJson(5,testFail);
        }
    }

    public void testPictureSize(String cameraid,CaptureModule.CameraMode mode) throws Exception{
        updateJson(5,null);
        checkSettingValue(SettingsManager.KEY_PICTURE_SIZE,null,mode,false);
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    public void testFrameRate(CaptureModule.CameraMode mode) throws Exception {
        int length = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE).length;
        //int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        for (int i = 0; i < length; i++) {
            executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE, i);
            executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
            Thread.sleep(OPEN_CAMERA_DURATION);
            testVideo(mode);
        }
        //mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE,defvalue);
    }
    public void testVideoSizeAndFrameRate(String cameraid,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        checkSettingValue(SettingsManager.KEY_VIDEO_QUALITY,SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE,mode,false);
      /*  int length = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_QUALITY).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_VIDEO_QUALITY);
        Map<Integer, CharSequence[]> frameRateList = new HashMap<Integer, CharSequence[]>();
        executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < length; i++) {
                    mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_QUALITY, i);
                    int length2 = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE).length;
                    CharSequence[]framerate = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
                    String videosize = mActivity.mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
                    frameRateList.put(i,framerate);
                }

            }
        });
        Thread.sleep(SMALL_WAIT_DURATION);
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        Thread.sleep(OPEN_CAMERA_DURATION);
        checkPreview(cameraid,mode);
        checkfps = true;
        boolean checkresult = true;
        for(int j = 0;j<length;j++){
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_QUALITY, j);
            int framelen = frameRateList.get(j).length;
            for(int k = 0;k <framelen;k++){
                mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE, k);
                testSettingIcon(cameraid, mode);
                if(testResult) {
                    testRecording(mode,false);
                    if(!testResult){
                        checkresult = false;
                        break;
                    }
                }else{
                    checkresult = false;
                    break;
                }

            }
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_QUALITY, defvalue);
        testSettingIcon(cameraid, mode);
        checkfps = false;*/
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
        //if(testResult) updateJson(5,testPass);
    }


    public void testLongShot(String cameraid,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        checkSettingValue(SettingsManager.KEY_LONGSHOT, null, mode, false);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void checkLongShot(boolean max)throws Exception{
        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        List<String> typearry = mActivity.getCaptureModule().getImgType();
        if(patharry == null || typearry == null){
            testFail = getFailStr("imagepath or imagetype is" + patharry +", imagetype is ",typearry,"NotNull");
            return;
        }
        // assertNotNull(patharry);
        if(patharry.size() > PersistUtil.getLongshotShotLimit() || patharry.size() ==0){
            testFail = getFailStr("image_num",patharry.size(),"below "+  PersistUtil.getLongshotShotLimit());
            return;
        }
        if( patharry.size() ==0){
            testFail = getFailStr("image_num",patharry.size(),"above 0");
            return;
        }
        String burstlimit = mSettingsManager.getValue(SettingsManager.KEY_BURST_LIMIT);
        if(mLongShotNum == patharry.size() && (burstlimit == null || burstlimit.equals("0")) && max){
            testFail = getFailStr("imag_num shoule different,before picture num is "+mLongShotNum+",current num is "
                    + patharry.size(),"mLongShotNum == patharry.size()","different");
            return;
        }

        for(int i=0;i<patharry.size();i++){
            List<ExifInterface> exif = mCaptureModule.getImagExif();
            String path = Storage.generateFilepath(patharry.get(i),typearry.get(i));
            checkSnapShot(path,exif.get(i));
        }
        //assertNotEquals(mLongShotNum,patharry.size());
        mLongShotNum = patharry.size();
    }
    public void testZSL(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String parentNm = stack[4].getMethodName();

        flashInZslResult = true;
        longshotInZslResult = true;
        updateAndSavejson(OUTPUT_JSON, "testLongShotInZSL", parentNm, longshotvalue);
        updateAndSavejson(OUTPUT_JSON, "testFlashInZSL", parentNm, flashvalue);
        jsonChildNm = "testZSL";
        jsonParentNm = parentNm;
        checkSettingValue(SettingsManager.KEY_ZSL, SettingsManager.KEY_LONGSHOT, mode,true);
        if (flashInZslResult) {
            updateAndSavejson(OUTPUT_JSON, "testFlashInZSL", parentNm, testPass);
        } else {
            updateAndSavejson(OUTPUT_JSON, "testFlashInZSL", parentNm, testFail);
        }
        if (longshotInZslResult) {
            updateAndSavejson(OUTPUT_JSON, "testLongShotInZSL", parentNm, testPass);
        } else {
            updateAndSavejson(OUTPUT_JSON, "testLongShotInZSL", parentNm, testFail);
        }
    }



    public void testMFNR(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        checkSettingValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE,null,mode,true);
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    private void clickShutterButton(CaptureModule.CameraMode mode)throws Exception{
        Thread.sleep(OPEN_CAMERA_DURATION);
        if(isVideoMode(mode)){
            testVideo(mode);
        }else {
            snapByLocation();
        }

    }
    private float getZoomFromText(TextView zoomText){
        String text= zoomText.getText().toString();
        int index = text.indexOf("x");
        if (index <= 0) {
            return -1;
        }
        String zoom = text.substring(0,index);
        return Float.parseFloat(zoom);
    }
    public void testZoom(CaptureModule.CameraMode mode) throws Exception {
        updateJson(5,null);
        mOldZoomstr = mActivity.getCaptureModule().getZoomValue();
        mOldZoomRegion = mCurrentPreviewResult.get(CaptureResult.SCALER_CROP_REGION);
        if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.CINEMATIC
            || mode == CaptureModule.CameraMode.DEPTH) {
            mSupported = false;
            View mZoomBar = mActivity.findViewById(R.id.zoom_seekbar);
            if (mZoomBar.getVisibility() == View.VISIBLE) {
                testFail = getFailStr("mZoomBar.getVisibility()",mZoomBar.getVisibility(),"INVISIBLE");
            } else {
                testResult = true;
            }
            if (testResult) updateJson(5, testPass);
            else{
                updateJson(5, testFail);
            }
            return;
        } else {
            TextView mZoomWText = (TextView) mActivity.findViewById(R.id.zoom_w);
            TextView mZoomUWText = (TextView) mActivity.findViewById(R.id.zoom_uw);
            TextView mZoomTelText = (TextView) mActivity.findViewById(R.id.zoom_tel);
            float uwZoom = getZoomFromText(mZoomUWText);
            float wZoom = getZoomFromText(mZoomWText);
            float telZoom = getZoomFromText(mZoomTelText);
            int wZoomlen = mZoomUWText.getMeasuredWidth();
            if (mode != CaptureModule.CameraMode.RTB) {
                int x_position = mZoomTextLoc[0];
                float selectZoom = wZoom;
                boolean checkChange = true;
                for (int i=0;i<3;i++){
                    if(i ==1){
                        x_position = mZoomValueLoc[0] + wZoomlen;
                        selectZoom = telZoom;
                    }else if(i == 2){
                        x_position = mZoomValueLoc[0] - wZoomlen;
                        selectZoom = uwZoom;
                    }else if(i == 0){
                        checkChange = false;
                    }
                    executeShellCommand("input tap " + x_position + " " + mZoomTextLoc[1]);
                    clickShutterButton(mode);
                    checkZoomValue(mode,selectZoom,checkChange);;
                    if (!testResult) break;
                    if(isOpenFromIntent) {
                        pressDone();
                        if (isVideoMode(mode)) {
                            openCameraByIntent(mVideoIntent);
                        } else {
                            openCameraByIntent(mImageIntent);
                        }
                        checkPreview("0", mode);
                    }
                }

            }
            int width = displayMetrics.widthPixels - 10;
            int height = displayMetrics.heightPixels - displayMetrics.widthPixels/2 + 10;
            TextView curretText;
            for (int j = 0; j < 2; j++) {
                if (isOpenFromIntent) {
                    mZoomWText = (TextView) mActivity.findViewById(R.id.zoom_w);
                    mZoomUWText = (TextView) mActivity.findViewById(R.id.zoom_uw);
                    mZoomTelText = (TextView) mActivity.findViewById(R.id.zoom_tel);
                }
                executeShellCommand("input swipe " + mZoomTextLoc[0] + " " + mZoomTextLoc[1]
                        +" " + mZoomTextLoc[0] + " " + mZoomTextLoc[1] + " " + 1000);
                Thread.sleep(SMALL_WAIT_DURATION);
                if (j == 0) {
                    executeShellCommand("input swipe " + width + " " + height+" "
                            + 10 + " " + height);
                    curretText =mZoomTelText;

                } else {
                    executeShellCommand("input swipe " + 10 + " " + height+" "
                            + width + " " + height);
                    curretText = mZoomUWText;
                }
                if(mode == CaptureModule.CameraMode.RTB){
                    curretText = mZoomWText;
                }
                Thread.sleep(SMALL_WAIT_DURATION);
                executeShellCommand("input tap " + width/2 + " " + width);
                float currentZoom = getZoomFromText(curretText);
                clickShutterButton(mode);
                checkZoomValue(mode, currentZoom, true);
                if (j == 0 && mCaptureModule.isExtendedMaxZoomEnable()) {
                    float zoomStr = mActivity.getCaptureModule().getZoomValue();
                    if (zoomStr < 10) {
                        testFail = getFailStr("max zoom is ", zoomStr, ">10");
                        break;
                    }
                }
                if (!testResult) break;
                if (isOpenFromIntent) {
                    pressDone();
                    if (isVideoMode(mode)) {
                        openCameraByIntent(mVideoIntent);
                    } else {
                        openCameraByIntent(mImageIntent);
                    }
                    checkPreview("0", mode);

                }
            }

        }
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    public void testCamID(CaptureModule.CameraMode mode) throws Exception {
        executeShellCommand("setprop persist.sys.camera.devoption.debug 100");
        mActivityRule.finishActivity();
        Thread.sleep(OPEN_CAMERA_DURATION);
        mActivityRule = new ActivityTestRule<>(CameraActivity.class);
        OpenCamera();
        Thread.sleep(OPEN_CAMERA_DURATION);
        String str = getTestMode(mode);
        int[] loc = mModeIconL.get(str);
        if(loc == null){
            loc = mModeIconR.get(str);
            switchModeTextToR(true);
        }
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        checkPreview("0",mode);
        updateJson(5,null);
        checkSettingValue(SettingsManager.KEY_SWITCH_CAMERA,null,mode,true);
        executeShellCommand("setprop persist.sys.camera.devoption.debug 0");
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    private String getEVFromText(){
        TextView mEVText =  mActivity.findViewById(R.id.ev_text);
        String text= mEVText.getText().toString();
        int index = text.indexOf(":");
        if (index <= 0) {
            return "0";
        }
        String str = text.substring(index+1);
       return str;
    }
    public void testVerticalEV(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.DEPTH) {
            mSupported = false;
            LinearLayout ev_layout = mActivity.findViewById(R.id.ev_layout);
            if (ev_layout.getVisibility() == View.VISIBLE) {
                testFail = getFailStr("ev_layout.getVisibility()",ev_layout.getVisibility(),"INVISIBLE");
            } else {
                testResult = true;
            }
            if (testResult) updateJson(5, testPass);
            else{
                updateJson(5, testFail);
            }
            return;
        } else {
            String ev_value =  mActivity.getResources().getString(
                    R.string.pref_exposure_default);
            CharSequence[] evvalues = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_EXPOSURE);
            String minValue = String.valueOf(evvalues[0]);
            String maxValue = String.valueOf(evvalues[evvalues.length - 1]);
            executeShellCommand("input tap "+ mEVTextLoc[0]  +" "+mEVTextLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
            View mEVVerticalBar =  mActivity.findViewById(R.id.ev_verticalbar);
            int evbar_height = mEVVerticalBar.getMeasuredHeight();
            int[] barloc = new  int[2];
            mEVVerticalBar.getLocationInWindow(barloc);
            int maxbar = barloc[1] + evbar_height - 10;
            for(int i =0;i <3 ;i++){
                if(i == 1){
                    executeShellCommand("input tap "+ barloc[0]   +" "+barloc[1]);
                    ev_value = maxValue;
                }else if(i == 2){
                    executeShellCommand("input tap "+ barloc[0]  +" "+maxbar);
                    ev_value = minValue;
                }
                Thread.sleep(SMALL_WAIT_DURATION);
                clickShutterButton(mode);
                checkEV(mode,ev_value,getEVFromText());
                if(isOpenFromIntent) {
                    pressDone();
                    if (isVideoMode(mode)) {
                        openCameraByIntent(mVideoIntent);
                    } else {
                        openCameraByIntent(mImageIntent);
                    }
                    checkPreview("0", mode);
                    executeShellCommand("input tap "+ mEVTextLoc[0]  +" "+mEVTextLoc[1]);
                }
            }
        }
        if(testResult)updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }

    }
    public void testEV(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        pressEVtext();
        //executeShellCommand("input tap "+ mEVLoc[0]  +" "+mEVLoc[1]);
        int defidx = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
        for(int i = 0;i < evvalue.length ;i++){
            mProMode.setIndex(i,true);
            snapByLocation();
            checkEV(mode,String.valueOf(evvalue[i]),null);
            if(!testResult)break;
        }
        mProMode.setIndex(defidx,true);
        if(testResult)updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    public void testWB() throws Exception {
        updateJson(5,null);
        pressWBtext();
        //executeShellCommand("input tap "+ mWBLoc[0]  +" "+mWBLoc[1]);
        int defidx = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_WHITE_BALANCE);
        for(int i = 0;i < wbvalue.length ;i++){
            mProMode.setIndex(i,true);
            snapByLocation();
            String value = String.valueOf(wbvalue[i]);
            if(i == 0){
                checkWB(true,value);
            }else {
                checkWB(false,value);
            }
            if(!testResult) break;
        }
        mProMode.setIndex(defidx,true);
        if(testResult)updateJson(5,testPass);
        else {
            updateJson(5,testFail);
        }
    }

    public void testISO() throws Exception {
        updateJson(5,null);
        pressISOtext();
        // executeShellCommand("input tap "+ mISOLoc[0]  +" "+mISOLoc[1]);
        int defidx = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_ISO);
        for(int i = 0;i < isovalue.length ;i++){
            String value = String.valueOf(isovalue[i]);
            mProMode.setIndex(i,true);
            snapByLocation();

            if(i == 0){
                checkISO(true,value);
            }else{
                checkISO(false,value);
            }
            if(!testResult) {
                break;
            }
        }
        mProMode.setIndex(defidx,true);
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    public void testFocusDistance()throws Exception {
        updateJson(5,null);
        pressFocusDistext();
        //executeShellCommand("input tap "+ mFocDisLoc[0]  +" "+mFocDisLoc[1]);
        float[] value = new float[] {0.1f,0.3f,0.5f,0.75f,1f};
        for(int i = 0 ;i <value.length;i++){
            float setvalue = value[i];
            mProMode.setSlider(setvalue,true);
            snapByLocation();
            checkFocusDistance(setvalue);
            if(!testResult) break;
        }
        if(testResult)updateJson(5,testPass);
        else {
            updateJson(5,testFail);
        }
    }
    public void testShutterSpeed()throws Exception {
        updateJson(5,null);
        pressShutterSpeedtext();
        //executeShellCommand("input tap "+ mShutterLoc[0]  +" "+mShutterLoc[1]);
        float[] value = new float[] {0.0f,0.25f,0.5f,0.75f,1f};
        for(int i = 0 ;i <value.length;i++){
            float setvalue = value[i];
            mProMode.setSlider(setvalue,true);
            snapByLocation();
            checkShutterSpeed(setvalue);
            if(!testResult) break;
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }

    private void checkShutterSpeed(float value) {
        if(!testResult) return;
        String setvalue = mProMode.getExposuretimeVaule(value);
        String valueInSet = mActivity.mSettingsManager.getKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE);
        String valueInExif = mCurrentexif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        long valueInResult = mActivity.getCaptureModule().getCaptureResult().get(CaptureResult.SENSOR_EXPOSURE_TIME);
        long longvalue = Long.valueOf(setvalue);
        String resultvalue = String.valueOf(valueInResult);
        double doubleexif = Double.valueOf(valueInExif) * mProMode.mExpTmConvert;
        long longexif = Math.round(doubleexif);
        long diffvalue = longvalue - longexif;
        long diffresultvalue = longvalue - valueInResult;

        assertEquals(setvalue, valueInSet);
        assertEquals(valueInResult, longexif);
        if(!setvalue.equals(valueInSet)){
            testFail = getFailStr("ExposuretimeVaule",valueInSet,setvalue);
            return;
        }
        if(valueInResult != longexif){
            testFail = getFailStr("Exposuretime in exif",longexif,valueInResult);
            return;

        }
        if(diffvalue > mProMode.mLongExpTm || diffvalue < -mProMode.mLongExpTm){
            testFail = getFailStr("Exposuretime value diff between value in setting and value in exif"
                    ,diffvalue,"less than "+mProMode.mLongExpTm);
            return;
        }

      /*  if(diffvalue > mProMode.mLongExpTm || (diffvalue < 0 && diffvalue > -mProMode.mLongExpTm)){
            throw new AssertionError("set shutterspeed value is " +
                    longvalue+",but iso in exif is "+ longexif);
        }
        if(diffresultvalue > mProMode.mLongExpTm || (diffresultvalue < 0 && diffresultvalue > -mProMode.mLongExpTm)){
            throw new AssertionError("set shutterspeed value is " +
                    longvalue+",but iso in result is "+ valueInResult);
        }*/

    }

    private void checkFocusDistance(float setvalue) {
        if(!testResult)return;
        float valueInSet = mActivity.mSettingsManager.getFocusSliderValue(SettingsManager.KEY_FOCUS_DISTANCE);
        float minFocus = mActivity.mSettingsManager.getMinimumFocusDistance(mActivity.getCaptureModule().getMainCameraId());
        float valueSet = minFocus * valueInSet;
        float valueInResult = mActivity.getCaptureModule().getCaptureResult().get(CaptureResult.LENS_FOCUS_DISTANCE);
        int mControlAFMode = mActivity.getCaptureModule().getCaptureResult().get(CaptureResult.CONTROL_AF_MODE);
        float valuediff = valueSet - valueInResult;
        String valueInExif = mCurrentexif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        //assertEquals(setvalue,valueInSet);
        //assertEquals(mControlAFMode,CaptureResult.CONTROL_AF_MODE_OFF);
        if (setvalue != valueInSet){
            testFail = getFailStr("KEY_FOCUS_DISTANCE",valueInSet,setvalue);
            return;
        }
        if(CaptureResult.CONTROL_AF_MODE_OFF != mControlAFMode){
            testFail = getFailStr("CONTROL_AF_MODE_OFF",mControlAFMode,CaptureResult.CONTROL_AF_MODE_OFF);
            return;
        }
        if(valuediff > 0.2 || valuediff < -0.2){
            testFail = getFailStr("KEY_FOCUS_DISTANCE diff with LENS_FOCUS_DISTANCE",valuediff,"less than 0.2");
            return;
        }
    }
    public void testAllInPro() throws Exception{
        updateJson(5,null);
        pressEVtext();
        mProMode.setIndex(evvalue.length - 1,true);

        pressWBtext();
        mProMode.setIndex(wbvalue.length - 1,true);

        pressISOtext();
        mProMode.setIndex(isovalue.length - 1,true);

        pressShutterSpeedtext();
        mProMode.setSlider(1,true);

        pressFocusDistext();
        mProMode.setSlider(1,true);
        snapByLocation();
        if(!testResult) return;
        checkEV(CaptureModule.CameraMode.PRO_MODE,"0",null);
        checkWB(false,String.valueOf(wbvalue[wbvalue.length - 1]));
        checkISO(false,String.valueOf(isovalue[isovalue.length -1]));
        checkShutterSpeed(1);
        checkFocusDistance(1);
        // testVideoFlash(true);
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }

    public void testCapturDone() throws Exception{
        executeShellCommand("input tap "+ mShutterLoc[0] +" "+mShutterLoc[1]);
        Thread.sleep(OPEN_CAMERA_DURATION);
        executeShellCommand("input tap "+ mDoneLoc[0] +" "+mDoneLoc[1]);

    }

    private void checkISO(boolean isauto,String setvalue){
        if(!testResult)return;
        String valueinset = mActivity.mSettingsManager.getValue(SettingsManager.KEY_ISO);
        int valueindex = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_ISO);
        String valueInExif= mCurrentexif.getAttribute(ExifInterface.TAG_ISO);
        int isoInResult =mCurrentCaptureResult.get(CaptureResult.SENSOR_SENSITIVITY);
        // assertNotNull(valueinset);
        //assertEquals(setvalue,valueinset);
        if(valueinset == null || !valueinset.equals(setvalue)){
            testFail =getFailStr("KEY_ISO",valueinset,setvalue);
            return;
        }
        if(!isauto) {
            // assertEquals(isoInResult,Integer.parseInt(valueInExif));
            int isoset = Integer.parseInt(setvalue);
            int isodiff = isoset - isoInResult;
            if(isoInResult != Integer.parseInt(valueInExif)){
                testFail =getFailStr("ExifInterface.TAG_ISO",valueInExif,isoInResult);
                return;
            }
            if(isodiff > 100 || isodiff < -100){
                testFail =getFailStr("SENSOR_SENSITIVITY diff with KEY_ISO",isodiff,"below 100");
            }
        }
    }

    private void checkWB(boolean isauto,String setvalue){
        if(!testResult)return;
        String valueinset = mActivity.mSettingsManager.getValue(SettingsManager.KEY_WHITE_BALANCE);
        String valueInExif= mCurrentexif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
        int wbInSet = Integer.parseInt(valueinset);
        int wbInResult =mCurrentCaptureResult.get(CaptureResult.CONTROL_AWB_MODE);
        if(!valueinset.equals(setvalue) ){
            testFail = getFailStr("KEY_WHITE_BALANCE",valueinset,setvalue);
            return;
        }
        if(wbInSet != wbInResult){
            testFail = getFailStr("CONTROL_AWB_MODE",wbInResult,wbInSet);
            return;
        }
        // assertNotNull(valueinset);
        // assertEquals(setvalue,valueinset);
        // assertEquals(wbInSet,wbInResult);
        if(isauto){
            if(!valueInExif.equals("0")){
                testFail = getFailStr("ExifInterface.TAG_WHITE_BALANCE",valueInExif,"0");
                return;
            }
            // assertEquals("0",valueInExif);
        }else{
            //assertEquals("1",valueInExif);
            if(!valueInExif.equals("1")){
                testFail = getFailStr("ExifInterface.TAG_WHITE_BALANCE",valueInExif,"1");
                return;
            }
        }
    }

    private void checkEV(CaptureModule.CameraMode mode,String indexvalue,String evText){
        if(!testResult) return;
        String value = mActivity.mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE);
        if(value == null){
            testFail = getFailStr("KEY_EXPOSURE",value,"NotNull");
            return;
        }
        if(evText != null && !value.equals(evText)){
            testFail = getFailStr("KEY_EXPOSURE is "+value +",evText is "+ evText,"Not equal","Equal");
            return;
        }
        // assertNotNull(value);
        int aeInSet = Integer.parseInt(value);

        int aeInPreview = mActivity.getCaptureModule().getPreviewCaptureResult().get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
        if(!indexvalue.equals(value)){
            testFail = getFailStr("KEY_EXPOSURE",value,indexvalue);
            return;
        }
        if(mode != CaptureModule.CameraMode.HFR && !isOpenFromIntent) {
            int aeInResult = mActivity.getCaptureModule().getCaptureResult().get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
            if (aeInSet != aeInResult) {
                testFail = getFailStr("CONTROL_AE_EXPOSURE_COMPENSATION in capture", aeInResult, aeInSet);
                return;
            }
        }
        if( aeInSet != aeInPreview){
            testFail = getFailStr("CONTROL_AE_EXPOSURE_COMPENSATION in preview",aeInPreview,aeInSet);
            return;
        }
        //assertEquals(indexvalue,value);
        // assertEquals(aeInSet,aeInResult);
    }

    public void snapByLocation(CaptureModule.CameraMode mode) throws Exception{
        if(mode != CaptureModule.CameraMode.PRO_MODE && !mCaptureModule.mIsRecordingVideo) {
            snapByLocation();
        }else{
            snapByButton();
        }
    }
    public void snapByLocation() throws Exception{
        if(mCaptureModule.getCurrenCameraMode() != CaptureModule.CameraMode.PRO_MODE
                && !mCaptureModule.mIsRecordingVideo) {
            snapByLocation(SNAPSHOT_NORMAL_DURATION);
        }else{
            snapByButton();
        }
    }

    private <T>void reportErrorInfo(String str,T currentvalue,T needvalue){
        String stackstr = jsonParentNm +"->"+jsonChildNm+" TestFail reason:";
        Log.e(TAG,"TestFail reason:"+stackstr+"the value of "+ str  +" is "+currentvalue
                +",but we need the value is "+needvalue);
    }
    private <T>String getFailStr(String str,T currentvalue,T needvalue){
        testResult = false;
        String failstr = "FAIL: \nThe value of "+ str  +" is "+currentvalue
                +",but we need the value is "+needvalue;
        reportErrorInfo(str,currentvalue,needvalue);
        return failstr;
    }
    public void snapByLocation(int time) throws Exception{
        Thread.sleep(OPEN_CAMERA_DURATION);
        resetCapture();
        // getPicSize();
        executeShellCommand("input tap "+ mShutterLoc[0] +" "+mShutterLoc[1]);
        Thread.sleep(time);
        snapShotCheck();
    }
    public void recordVideo() throws Exception {
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        Thread.sleep(VIDEO_DURATION);
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
    }
    public void testVideo(CaptureModule.CameraMode mode)throws Exception{
        if(isOpenFromIntent) {
            testRecording(mode, false);
        }else {
            testRecording(mode, false);
        }
    }
    public void testRecording(CaptureModule.CameraMode mode,boolean pressdone)throws Exception{
        updateJson(5,null);
        resetVideo();
        resetCapture();
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        long startime = System.currentTimeMillis();
        Thread.sleep(VIDEO_DURATION);
        if(isPerformenceTest){
            HashMap<String,Long> startVideo = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("startVideo",startVideo);
        }
        if(snapShotInVideo(mode)) {
            snapByLocation(mode);
        }
/*        if(!testResult){
            return;
        }*/
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        long endtime = System.currentTimeMillis();
        Thread.sleep(SAVE_VIDEO_DURATION);
        if(testResult) {
            checkVideo(endtime - startime);
        }
        if(isPerformenceTest){
            HashMap<String,Long> stopVideo = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("stopVideo",stopVideo);
        }
        if(isOpenFromIntent && pressdone){
            pressDone();
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }

    public void testVideoPauseAndResume()throws Exception{
        updateJson(5,null);
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        long startime = System.currentTimeMillis();
        Thread.sleep(VIDEO_DURATION);
        getRecordingLoc();
        executeShellCommand("input tap "+ mPauseLoc[0] +" "+mPauseLoc[1]);
        long endtime = System.currentTimeMillis();
        Thread.sleep(VIDEO_DURATION);
        executeShellCommand("input tap "+ mPauseLoc[0] +" "+mPauseLoc[1]);
        long restartime = System.currentTimeMillis();
        Thread.sleep(VIDEO_DURATION);
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        long reendtime = System.currentTimeMillis();
        long dutime = endtime - startime + reendtime - restartime;
        Thread.sleep(OPEN_CAMERA_DURATION);
        checkVideo(dutime);
        if(isOpenFromIntent){
            pressDone();
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }

    private void testFilter(CaptureModule.CameraMode mode)throws Exception{
        updateJson(5,null);
        View mFliter = mActivity.findViewById(R.id.filter_mode_switcher);
        if(mode == CaptureModule.CameraMode.PRO_MODE || !mSettingsManager.isFilterShow()
                || mode == CaptureModule.CameraMode.DEPTH) {
            if (mFliter.getVisibility() != View.VISIBLE) {
                testResult = true;
                mSupported = false;
            } else {
                testFail = getFailStr("filter_mode_switcher.getVisibility", mFliter.getVisibility(), "INVISIBLE");
            }
            if(testResult) updateJson(5,testPass);
            else{
                updateJson(5,testFail);
            }

        }else{
            executeShellCommand("input tap " + mFilterLoc[0] + " " + mFilterLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
            int width = 500;//mCaptureUI.getFilterWidth();
            int height = mCaptureUI.getFilterHight()+100;
            executeShellCommand("input tap " + width + " " + height);


            Thread.sleep(SMALL_WAIT_DURATION);

            if(mode == CaptureModule.CameraMode.DEFAULT || mode == CaptureModule.CameraMode.RTB){
                testSnapshot(mode);
            }else if (isVideoMode(mode)){
                testVideo(mode);
            }

            if(testResult) {
                String value  =mSettingsManager.getValue(SettingsManager.KEY_COLOR_EFFECT);
                checkFilter(mode,Integer.valueOf(value));
            }
            if(testResult) updateJson(5,testPass);
            else{
                updateJson(5,testFail);
            }
            //   executeShellCommand("input tap " + 0 + " " + height);//reset Filter 0
            if(isOpenFromIntent && mCaptureModule.getPaused()){
                if(isVideoMode(mode)) {
                    openCameraByIntent(mVideoIntent);
                }else{
                    openCameraByIntent(mImageIntent);
                }
                Thread.sleep(SMALL_WAIT_DURATION);
            }
            mSettingsManager.setValueIndex(SettingsManager.KEY_COLOR_EFFECT,0);
            executeShellCommand("input tap 500 500");
            Thread.sleep(SMALL_WAIT_DURATION);
        }
    }
    private String getTestMode(CaptureModule.CameraMode mode){
        String str = "";
        switch (mode){
            case HFR:
                str = "HFR";
                break;
            case VIDEO:
                str = "Video";
                break;
            case CINEMATIC:
                str = "Cinematic";
                break;
            case DEFAULT:
                str = "Photo";
                break;
            case RTB:
                str = "Bokeh";
                break;
            case PRO_MODE:
                str = "Pro";
                break;
            case DEPTH:
                str = "Depth";
                break;
        }
        return str;

    }
    private void testMediaCodec(String cameraid,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        executeShellCommand("setprop persist.sys.cameraapp.mediarecorder false");
        resetPreview();
        resetVideo();
        mActivityRule.finishActivity();
        Thread.sleep(OPEN_CAMERA_DURATION);
        mActivityRule = new ActivityTestRule<>(CameraActivity.class);
        OpenCamera();
        String str = getTestMode(mode);
        int[] loc = mModeIconL.get(str);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        Thread.sleep(OPEN_CAMERA_DURATION);
        if(mCaptureModule.getMainCameraId() != Integer.valueOf(cameraid)){
            testFail = getFailStr("getMainCameraId",mCaptureModule.getMainCameraId(),cameraid);
        }
        if(CaptureModule.CURRENT_MODE != mode){
            testFail = getFailStr("CaptureModule.CURRENT_MODE",CaptureModule.CURRENT_MODE,cameraid);
        }
        if(testResult) {
            testRecording(mode, false);
        }
        executeShellCommand("setprop persist.sys.cameraapp.mediarecorder true");
        mActivityRule.finishActivity();
        Thread.sleep(OPEN_CAMERA_DURATION);
        mActivityRule = new ActivityTestRule<>(CameraActivity.class);
        OpenCamera();
        executeShellCommand("input tap " + loc[0] + " " + loc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        if (mCaptureModule.getMainCameraId() != Integer.valueOf(cameraid)) {
            testFail = getFailStr("getMainCameraId", mCaptureModule.getMainCameraId(), cameraid);
        }
        if (CaptureModule.CURRENT_MODE != mode) {
            testFail = getFailStr("CaptureModule.CURRENT_MODE", CaptureModule.CURRENT_MODE, cameraid);
        }

        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testLivePreview(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_LIVE_PREVIEW;
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testBurstLimit(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_BURST_LIMIT;
        checkSettingValue(strkey,SettingsManager.KEY_LONGSHOT,mode,true);
        if (testResult) {
            updateJson(5, testPass+"(imgNum:"+burstNum+")");
            burstNum = 0;
        }
        else {
            updateJson(5, testFail);
        }
    }

    private void testPicFormat(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_PICTURE_FORMAT;
        checkSettingValue(strkey,null,mode,false);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testSaveRaw(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_RAW_FORMAT_TYPE;
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testMixedHDR(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_MANUAL_HDR;
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testSelectMode(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_SELECT_MODE;
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testDynamicFPS(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_VARIABLE_FPS;
        String subkey = SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE;
        checkSettingValue(strkey,subkey,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testExtendZoom(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_EXTENDED_MAX_ZOOM;
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testTAF(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        int x = displayMetrics.widthPixels/2;
        int y = displayMetrics.heightPixels/2;
        executeShellCommand("input tap "+ x +" " +y);
        Thread.sleep(SNAPSHOT_NORMAL_DURATION);
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        Integer resultAFState = mCurrentPreviewResult.get(CaptureResult.CONTROL_AF_STATE);
        if(resultAFState != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED){
            testFail = getFailStr("CONTROL_AF_STATE", resultAFState, "CONTROL_AF_STATE_FOCUSED_LOCKED");
        }
        x = x + 300;
        y = y + 300;
        executeShellCommand("input tap "+ x +" " +y);
        Thread.sleep(SNAPSHOT_NORMAL_DURATION);
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        resultAFState = mCurrentPreviewResult.get(CaptureResult.CONTROL_AF_STATE);
        if(resultAFState != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED){
            testFail = getFailStr("CONTROL_AF_STATE", resultAFState, "CONTROL_AF_STATE_FOCUSED_LOCKED");
        }
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testThumbnail(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String path = "";
        if(!isVideoMode(mode)) {
            testSnapshot(mode);
            List<String>  patharry = mActivity.getCaptureModule().getLongImageTitle();
            if(patharry.size() == 0){
                testFail = getFailStr("imagesize", 0, 1);
                return;
            }
            path = patharry.get(0);
        }else{
            testVideo(mode);
            path = mCaptureModule.getVideoFilePath();
            if(path == null){
                testFail = getFailStr("getVideoFilePath", null, "NotNull");
                return;
            }
            int index = path.lastIndexOf("/");
            int index1 = path.lastIndexOf(".");
            path = path.substring(index+1,index1);
        }
        executeShellCommand("input tap "+ mThumbLoc[0]  +" "+mThumbLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        if (mCaptureUI.mFilmstripLayout.getVisibility() != View.VISIBLE) {
            testFail = getFailStr("FilmstripLayout.getVisibility()", mCaptureUI.mFilmstripLayout.getVisibility(), "VISIBLE");
            return;
        }
        String title =mCaptureUI.getTitleFromFilm(0);
        if( null == title || !path.equals(title)){
            testFail = getFailStr("The first image title in Thumbnail is ", title, path);
        }
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        checkPreview(cameraId, mode);
        if (mCaptureUI.mFilmstripLayout.getVisibility() != View.INVISIBLE) {
            testFail = getFailStr("FilmstripLayout.getVisibility()", mCaptureUI.mFilmstripLayout.getVisibility(), "INVISIBLE");
        }
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }

    private void testEIS(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        String strkey = SettingsManager.KEY_PHOTO_EIS_VALUE;
        if (isVideoMode(mode)) {
            strkey = SettingsManager.KEY_EIS_VALUE;
        }
        String defvalue = mSettingsManager.getValue(strkey);
        if (mode == CaptureModule.CameraMode.DEFAULT) {
            if (!"disable".equals(defvalue)) {
                testFail = getFailStr("KEY_PHOTO_EIS_VALUE default—value", defvalue, "disable");
                updateJson(5, testFail);
                return;
            }
        } else if ((mode == CaptureModule.CameraMode.VIDEO)) {
            if (!"V3".equals(defvalue)) {
                testFail = getFailStr("KEY_PHOTO_EIS_VALUE default—value", defvalue, "V3");
                updateJson(5, testFail);
                return;
            }
        }
        checkSettingValue(strkey,null,mode,true);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testDepthUI()throws Exception {
        updateJson(5, null);
        getDepthUILoc();
        executeShellCommand("input tap " + mDepthBarLoc[0] + " " + mDepthBarLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        executeShellCommand("input tap " + mDepthBarMaxLoc[0] + " " + mDepthBarMaxLoc[1]);
        checkPreview("0",CaptureModule.CameraMode.DEPTH);
        executeShellCommand("input tap " + mDepthSettingLoc[0] + " " + mDepthSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        checkPreview("0",CaptureModule.CameraMode.DEPTH);
        if (testResult) updateJson(5, testPass);
        else {
            updateJson(5, testFail);
        }
    }
    private void testKeyValue(String strkey,CharSequence[] Entryvalues,CaptureModule.CameraMode mode,boolean devoption){

    }
    private boolean checkSettingValue(String strkey,String subkey,CaptureModule.CameraMode mode,boolean devoption) throws Exception {
        if(devoption){
            mActivity.setDevOption(true);
        }
       CharSequence[] Entryvalues = null;
       String defalutValue ="";
       String subdefValue="";
       if(!mCaptureModule.getPaused()) {
           executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
           Thread.sleep(SMALL_WAIT_DURATION);
       }

            Entryvalues = mSettingsManager.getEntryValues(strkey);
            defalutValue = mSettingsManager.getValue(strkey);
            if(subkey != null) {
                CharSequence[] subvalues = mSettingsManager.getEntryValues(subkey);
                subdefValue = mSettingsManager.getValue(subkey);
                Log.i(TAG, "subkey=" + subkey + ",subdefValue=" + subdefValue);
            }
        if(SettingsManager.KEY_LONGSHOT.equals(strkey)){
            List<String> values = new ArrayList<String>(Arrays.asList("off", "on"));
            Entryvalues = values.toArray(new CharSequence[values.size()]);
            defalutValue = "off";
        }
        if(Entryvalues == null || Entryvalues.length == 0){
            Log.i(TAG,"Did not find this setting or did not get its value,key is:"+strkey);
            mSupported = false;
            return true;
        }
        for (int i = 0; i < Entryvalues.length; i++) {
            final String setvalue = Entryvalues[i].toString();
            if(!mCaptureModule.getPaused()) {
                executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
                Thread.sleep(SMALL_WAIT_DURATION);
            }
            Log.i(TAG, "start to  setvalue=" + setvalue+",setkey="+strkey+
                    ",mCaptureModule.getPaused()="+mCaptureModule.getPaused()+",i="+i+",Entryvalues.len="+Entryvalues.length);
            if(!mCaptureModule.getPaused()) {
                testFail = getFailStr("Open Setting failed,mPaused", mCaptureModule.getPaused(), true);
            }
            mActivity.runOnUiThread(()->{
                Log.i(TAG,"goto mSettingsManager set value key="+strkey+",value="+setvalue);
                if(strkey.equals(SettingsManager.KEY_MANUAL_HDR)){
                    if(setvalue.equals("manual")){
                        mCaptureModule.setAutoSetting(true);
                    }
                }
                mSettingsManager.setValue(strkey,setvalue);
            });
            Thread.sleep(SMALL_WAIT_DURATION);
            if(subkey != null){
                checkSettingValue(subkey,null,mode,devoption);
               // testKeyValue(subkey,subvalues,mode,devoption);
            }else {
                if(mCaptureModule.getPaused()) {
                    Log.i(TAG, "click backkey=" + KeyEvent.KEYCODE_BACK);
                    executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
                }
                if(strkey.equals(SettingsManager.KEY_MANUAL_HDR) && setvalue.equals("manual")){
                    if(mCaptureModule.getPaused()) {
                        Log.i(TAG, " KEY_MANUAL_HDR click backkey=" + KeyEvent.KEYCODE_BACK);
                        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
                    }
                }
                Thread.sleep(SMALL_WAIT_DURATION);
                String endvalue = mActivity.mSettingsManager.getValue(strkey);
                if (!endvalue.equals(setvalue)) {
                    testFail = getFailStr("getValue from " + strkey + " is", endvalue, setvalue);
                    //updateJson(5, testFail);
                }
                if (!isVideoMode(mode)) {
                    snapByLocation();
                } else {
                    testVideo(mode);
                }
                if(!testResult){
                    continue;
                }
                String cameraId = String.valueOf(mCaptureModule.getMainCameraId());
                switch (strkey) {
                    case SettingsManager.KEY_PHOTO_EIS_VALUE:
                    case SettingsManager.KEY_EIS_VALUE:
                        checkEIS(mode,setvalue);
                        break;
                    case SettingsManager.KEY_SELECT_MODE:
                        checkSelectMode(setvalue);
                        break;
                    case SettingsManager.KEY_VARIABLE_FPS:
                    case SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE:
                        checkDynamicFPS(setvalue);
                        break;
                    case SettingsManager.KEY_LIVE_PREVIEW:
                        checkLivePreview(setvalue);
                        break;
                    case SettingsManager.KEY_BURST_LIMIT:
                        checkBurstLimit(setvalue);
                        break;
                    case SettingsManager.KEY_MANUAL_HDR:
                        checkMixedHDR(setvalue);
                        break;
                    case SettingsManager.KEY_EXTENDED_MAX_ZOOM:
                        checkExtendZoom(mode,setvalue);
                        break;
                    case SettingsManager.KEY_LONGSHOT:
                        if(testResult) {
                            if("on".equals(setvalue)) {
                                clickLongShot(mode);
                            }
                        }else{
                            if(longshotInZslResult){
                                longshotInZslResult = false;
                                longshotvalue = testFail;
                            }
                        }
                        break;
                    case SettingsManager.KEY_ZSL:
                        checkZSL();
                        if(flashInZslResult) {
                            testFlash(cameraId, mode, false);
                        }else{
                            flashInZslResult = false;
                            longshotvalue = testFail;
                        }
                        break;
                    case SettingsManager.KEY_CAPTURE_MFNR_VALUE:
                        checkMFNR();
                        break;
                    case SettingsManager.KEY_SWITCH_CAMERA:
                        checkCameraId(setvalue,cameraId);
                        break;
                }
            }
        }
        changeSettingInMain(strkey,defalutValue);
        if(subkey != null){
            changeSettingInMain(subkey,subdefValue);
        }
        mCaptureModule.setAutoSetting(false);
        if(devoption){
            mActivity.setDevOption(false);
        }
        return true;
    }
    private void changeSettingInMain(String key,String value)throws Exception{
        executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        mActivity.runOnUiThread(()-> {
            mSettingsManager.setValue(key,value);
        });
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        Thread.sleep(SMALL_WAIT_DURATION);
    }
    private void checkCameraId(String setid,String id){
        if(setid == null || id == null ){
            testFail = getFailStr("Current cameraid is ",null,"NotNull");
            return;
        }
        if(setid.equals("-1")){
            setid = "0";
        }
        if(!setid.equals(id)){
            testFail = getFailStr("Current cameraid is ",id,setid);
            return;
        }
    }
    private void clickLongShot(CaptureModule.CameraMode mode) throws Exception {
        mLongShotNum = 0;
        resetCapture();
        if (mode != CaptureModule.CameraMode.PRO_MODE) {
            executeShellCommand("input swipe " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + 1000);
            Thread.sleep(SNAPSHOT_NORMAL_FLAH_OFF * 3);
            checkLongShot(false);
            resetCapture();
            executeShellCommand("input swipe " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + 5000);
            Thread.sleep(SNAPSHOT_NORMAL_FLAH_OFF * 5);
            checkLongShot(true);
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mActivity.findViewById(R.id.shutter_button).performLongClick();
                }
            });
            Thread.sleep(SNAPSHOT_NORMAL_DURATION);
            checkLongShot(false);
        }
    }
    private void checkExtendZoom(CaptureModule.CameraMode mode,String setvalue)  throws Exception {
        testZoom(mode);
        int mMaxZoomP = mCurrentPreviewResult.get(CaptureModule.getExtendedMaxZoom);
        int mMaxZoomC = mCurrentCaptureResult.get(CaptureModule.getExtendedMaxZoom);
        int value = Integer.valueOf(setvalue);
        if(value != mMaxZoomP || value != mMaxZoomC){
            testFail = getFailStr("ExtendedMaxZoom in preview is" + mMaxZoomP + ",in capture is ",
                    mMaxZoomC, value);
            return;
        }
    }
    private void checkMixedHDR(String setvalue)  throws Exception {
        int mautoP = mCurrentPreviewResult.get(VendorTagUtil.get_autohdr_enable);
        int mautoC = mCurrentCaptureResult.get(VendorTagUtil.get_autohdr_enable);
        int mhdrP = mCurrentPreviewResult.get(VendorTagUtil.get_hdr_enable);
        int mhdrC = mCurrentCaptureResult.get(VendorTagUtil.get_hdr_enable);
        int[] mhdrmodeP = mCurrentPreviewResult.get(VendorTagUtil.get_hdr_modes);
        int[] mhdrmodeC = mCurrentCaptureResult.get(VendorTagUtil.get_hdr_modes);
        String hdrmode = mSettingsManager.getVideoHdrMode();
        Log.i(TAG, "mautoP=" + mautoP + ",mautoC=" + mautoC + ",mhdrP=" + mhdrP + ",mhdrC=" + mhdrC +
                ",mhdrmodeP=" + mhdrmodeP + ",mhdrmodeC=" + mhdrmodeC+",hdrmode="+hdrmode);
        if (hdrmode != null) {
            if (hdrmode.equals("auto")) {
                if (mautoP != 1 || mautoC != 1){
                    testFail = getFailStr("EnableAutoHDR in preview is" + mautoP + ",in capture is ",
                            mautoC, 1);
                    return;
                }
            } else if (!hdrmode.equals("off")) {
                String[] modeLists = hdrmode.split(" ");
                if(modeLists == null){
                    testFail = getFailStr("hdrmode in setting is" , modeLists ,"NotNull");
                    return;
                }
                int[] modes =mCaptureModule.getHDRModes();
                int value = mCaptureModule.getHDRValues();
                mCaptureModule.setHDRModes(null);
                mCaptureModule.setHDRValues(0);
                if (value != mhdrP || value != mhdrC) {
                    testFail = getFailStr("HDRMode in preview is" + mhdrP + ",in capture is ",
                            mhdrC, value);
                    return;
                }
                if(mhdrmodeP == null || mhdrmodeC == null || modes == null){
                    testFail = getFailStr("HDRModePreference in preview is" + mhdrmodeP + ",in capture is "
                                    +mhdrmodeC +",in setting is ",
                            modes, "NotNull");
                }
                for (int i =0;i<mhdrmodeP.length;i++) {
                    if(mhdrmodeP[i]!=modes[i] || mhdrmodeC[i]!=modes[i]){
                        testFail = getFailStr("hdrmode in preview is"+mhdrmodeP[i]+",in capture is" , mhdrmodeC[i] ,modes[i]);
                        return;
                    }
                }
            }else if (hdrmode.equals("off")) {
                if (mautoP != 0 || mautoC != 0){
                    testFail = getFailStr("EnableAutoHDR in preview is" + mautoP + ",in capture is ",
                            mautoC, 0);
                    return;
                }
                if (mhdrP != 0 || mhdrP != 0){
                    testFail = getFailStr("HDRMode in preview is" + mhdrP + ",in capture is ",
                            mhdrP, 0);
                    return;
                }
            }
        }
    }
    private void checkBurstLimit(String value)  throws Exception {
       // testLongShot(String.valueOf(mCaptureModule.getMainCameraId()), CaptureModule.CURRENT_MODE);
        mCurrentCaptureResult = mCaptureModule.getCaptureResult();
        Object burstag = mCurrentCaptureResult.getRequest().getTag();

        if (burstag == null) {
            testFail = getFailStr("request.getTag() is", burstag, "NotNull");
            return;
        }
        String tag = String.valueOf(burstag);
        if (value.equals("0") && !burstag.equals("capture")) {
            testFail = getFailStr("request.getTag() is", burstag, "capture");
            return;
        }
        if (value.equals("1") && !tag.equals("capture-limit")) {
            testFail = getFailStr("request.getTag() is", burstag, "capture-limit");
            return;
        }
    }
    private void checkLivePreview(String value){
        int intValue = Integer.parseInt(value);
        int mLiveModeP = mCurrentPreviewResult.get(mCaptureModule.getLivePreview);
        int mLiveModeC = mCurrentCaptureResult.get(mCaptureModule.getLivePreview);
        if (intValue != mLiveModeP || intValue != mLiveModeP){
            testFail = getFailStr("livePreview in preview is"+mLiveModeP+",in capture is ",
                    mLiveModeC,intValue);
        }
    }
    private void checkDynamicFPS(String value) {
        if (!mCaptureModule.isVariableFPSEnabled()) {
            return;
        }
        float[] setFpsConfig = mCaptureModule.getDynamicFpsConfig();
        Range setRange = mCaptureModule.getFPSRange();
        Range mRangeP = mCurrentPreviewResult.get(CaptureResult.CONTROL_AE_TARGET_FPS_RANGE);
        Range mRangeC = mCurrentCaptureResult.get(CaptureResult.CONTROL_AE_TARGET_FPS_RANGE);
        if (setRange.getLower() != mRangeP.getLower() || setRange.getLower() != mRangeC.getLower()
                || setRange.getUpper() != mRangeP.getUpper() || setRange.getUpper() != mRangeC.getUpper()) {
            testFail = getFailStr("When DynamicFPS is " + value + "CONTROL_AE_TARGET_FPS_RANGE in preview is" + mRangeP + ",in capture is ",
                    mRangeC, setRange);
            return;
        }
        float[] mConfigP = mCurrentPreviewResult.get(mCaptureModule.getdynamicFSPConfigKey);
        float[] mConfigC = mCurrentPreviewResult.get(mCaptureModule.getdynamicFSPConfigKey);
        if (setFpsConfig != null && (mConfigP == null || mConfigC == null)) {
            testFail = getFailStr("When DynamicFPS is " + value + "dynamicFPSConfig in preview is" + mConfigP + ",in capture is ",
                    mConfigC, setFpsConfig);
            return;
        }
        for (int i = 0; i < setFpsConfig.length; i++) {
            if (setFpsConfig[i] != mConfigP[i] || setFpsConfig[i] != mConfigC[i]) {
                testFail = getFailStr("Config i is" + i + " dynamicFPSConfig i in preview  is" + mConfigP[i] + ",in capture is ",
                        mConfigC[i], setFpsConfig[i]);
                return;
            }
        }
    }
    private void checkSelectMode(String value) {
        int cameraId = mCaptureModule.getMainCameraId();
        int mSceneModeP = mCurrentPreviewResult.get(CaptureResult.CONTROL_EXTENDED_SCENE_MODE);
        int mSceneModeC = mCurrentCaptureResult.get(CaptureResult.CONTROL_EXTENDED_SCENE_MODE);
        if (value.equals("single_rear_cameraid") || value.equals("single_rear_aibokeh")) {
            if (cameraId == 0) {
                testFail = getFailStr("cameraid   in " + value,cameraId, "!0");
            }
        } else {
            if (cameraId != 0) {
                testFail = getFailStr("cameraid  in " + value,cameraId, "0");
            }
        }
        if (value.equals("rtb") || value.equals("single_rear_aibokeh") ){
            if (mSceneModeP != CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_CONTINUOUS ||
                    mSceneModeC != CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_CONTINUOUS) {
                testFail = getFailStr("CONTROL_EXTENDED_SCENE_MODE in  " + value + "is" + mSceneModeP +
                        " in mCurrentCaptureResult is", mSceneModeC, CameraMetadata.CONTROL_EXTENDED_SCENE_MODE_BOKEH_CONTINUOUS);
            }
        }


    }

    private boolean snapShotInVideo(CaptureModule.CameraMode mode){
        if((mode == CaptureModule.CameraMode.VIDEO || mode == CaptureModule.CameraMode.CINEMATIC)
                && !isOpenFromIntent) {
            return true;
        }else{
            return false;
        }
    }
    private void checkFilter(CaptureModule.CameraMode mode,int value){
        int mFilterModeP = mCurrentPreviewResult.get(CaptureResult.CONTROL_EFFECT_MODE);
        if(snapShotInVideo(mode) ) {
            int mFilterModeC = mCurrentCaptureResult.get(CaptureResult.CONTROL_EFFECT_MODE);
            if (value != mFilterModeC || value != mFilterModeP) {
                testFail = getFailStr("CONTROL_EFFECT_MODE in preview is" + mFilterModeP + ",in capture is ",
                        mFilterModeC, value);
                return;
            }
        }else if(value != mFilterModeP) {
            testFail = getFailStr("CONTROL_EFFECT_MODE in preview is " , mFilterModeP,value);
            return;
        }
    }
    private boolean checkEIS(CaptureModule.CameraMode mode,String value) {
        int videoStabModeP = mCurrentPreviewResult.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE);
        int videoStabModeC = -1;
        if(isSupportSnapShot(mode)) {
            videoStabModeC = mCurrentCaptureResult.get(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE);
        }
        boolean previewStabilizationOn = false;
        String stablevalue = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_STABILIZATION);
        if (value.equals("V2") || value.equals("dynamic")) {
            previewStabilizationOn = "enable".equals(mSettingsManager.
                    getValue(SettingsManager.KEY_PREVIEW_STABILIZATION));
        }
        if (!previewStabilizationOn && "disable".equals(value) && (videoStabModeP != 0 ||
                (isSupportSnapShot(mode) && videoStabModeC != 0))) {
            testFail = getFailStr("CONTROL_VIDEO_STABILIZATION_MODE in  PreviewResult is "
                            + videoStabModeP+ ",in CaptureResult is ",videoStabModeC
                    , "disable");
            return false;
        }else if (previewStabilizationOn && (videoStabModeP != 2 ||
                (isSupportSnapShot(mode) && videoStabModeC != 2))){
            testFail = getFailStr("CONTROL_VIDEO_STABILIZATION_MODE in  PreviewResult is "+ videoStabModeP+
                    ", in CaptureResult is ", videoStabModeC,"2");
            return false;
        }else if (!previewStabilizationOn && !("disable".equals(value)) && (videoStabModeP != 1 ||
                (isSupportSnapShot(mode) && videoStabModeC != 1))){
            testFail = getFailStr("CONTROL_VIDEO_STABILIZATION_MODE in  PreviewResult is "+ videoStabModeP+
                    ", in CaptureResult is ", videoStabModeC,"1");
            return false;
        }
        if(!value.equals("disable")) {
            try {
                int eisModeP = mCurrentPreviewResult.get(VendorTagUtil.GET_EIS_MODE);
                int eisModeC = -1;
                if(isSupportSnapShot(mode)) {
                    eisModeC = mCurrentCaptureResult.get(VendorTagUtil.GET_EIS_MODE);
                }
                if (!previewStabilizationOn) {
                    if (value.equals("V2") && (eisModeP != 1 ||
                            (isSupportSnapShot(mode) && eisModeC != 1))) {
                        testFail = getFailStr("EISMode in  PreviewResult is " + eisModeP +
                                "EISMode in CaptureResult is ", eisModeC, "1");
                        return false;
                    } else if (value.equals("V3") && (eisModeP != 0 ||
                            (isSupportSnapShot(mode) && eisModeC != 0))) {
                        testFail = getFailStr("EISMode in  PreviewResult is " + eisModeP +
                                "EISMode in CaptureResult is ", eisModeC, "0");
                        return false;
                    } else if (value.equals("dynamic") && (eisModeP != 2 ||
                            (isSupportSnapShot(mode) && eisModeC != 2))) {
                        testFail = getFailStr("EISMode in  PreviewResult is " + eisModeP +
                                "EISMode in CaptureResult is ", eisModeC, "2");
                        return false;
                    }
                } else {
                    if (value.equals("dynamic") && (eisModeP != 2 ||
                            (isSupportSnapShot(mode) && eisModeC != 2))) {
                        testFail = getFailStr("EISMode in  PreviewResult is " + eisModeP +
                                "EISMode in CaptureResult is ", eisModeC, "2");
                        return false;
                    }
                }
            }catch(NullPointerException exception){
                Log.i(TAG," exception="+exception);
            }
        }
        return true;
    }
    private void snapByKeyCode()throws Exception{
        //mCurrentImgNum = getCameraDirectoryJpegAmount();
        Thread.sleep(OPEN_CAMERA_DURATION);
        // getPicSize();
        InstrumentationRegistry.getInstrumentation().sendCharacterSync(KeyEvent.KEYCODE_CAMERA);
        Thread.sleep(SNAPSHOT_NORMAL_DURATION);
        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        assertNotNull(patharry);
        assertEquals(1,patharry.size());
        List<ExifInterface> exif = mCaptureModule.getImagExif();
        checkSnapShot(patharry.get(0),exif.get(0));
    }
    private void snapShotCheck()throws Exception {
        mCurrentCaptureResult = mCaptureModule.getCaptureResult();
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        if (mCurrentPreviewResult == null || mCurrentCaptureResult == null) {
            testFail = getFailStr("CurrentPreviewResult is" + mCurrentPreviewResult +
                    ",mCurrentCaptureResult is  ", mCurrentCaptureResult, "NotNull");
            return;
        }
        if(isOpenFromIntent){
            return;
        }

        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        List<String> typearry = mActivity.getCaptureModule().getImgType();

        if (patharry == null || typearry == null) {
            testFail = getFailStr("imagepath is " +patharry+"imgetype is" , typearry, "NotNull");
            return;
        }
        int imgnm = getImageNum();
        if (patharry.size() < imgnm) {
            testFail = getFailStr("image_num", patharry.size(), imgnm);
            return;
        }
        if (typearry.size() < imgnm) {
            testFail = getFailStr("typearry_num", typearry.size(), imgnm);
            return;
        }
        String raw = mSettingsManager.getKeyValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
        for (int i = 0; i < imgnm; i++) {
            if(i == 0 && !raw.equals("0")){
                String type = typearry.get(i);
                if (raw.equals("10") && !type.equals("raw")) {
                    testFail = getFailStr("raw foramt  ",type,"raw");
                    return;
                } else if ((raw.equals("54") || raw.equals("99")) && !type.equals("yuv") ) {
                    testFail = getFailStr("raw foramt  ",type,"yuv");
                    return;
                } else if (raw.equals("16")&& !type.equals("dng")) {
                    testFail = getFailStr("raw foramt  ",type,"dng");
                    return;
                }
            }
            List<ExifInterface> exif = mCaptureModule.getImagExif();
            if (exif.size() < imgnm) {
                testFail = getFailStr("exifinfo", exif.size(), imgnm);
                return;
            }
            String path = Storage.generateFilepath(patharry.get(i),typearry.get(i));
            checkSnapShot(path, exif.get(i));
        }
    }

    private void snapByButton()throws Exception{
        //mCurrentImgNum = getCameraDirectoryJpegAmount();
        Thread.sleep(OPEN_CAMERA_DURATION);
        resetCapture();
/*        if(!mCaptureModule.mIsRecordingVideo) {
            getPicSize();
        }else{
            getVideoSize();
        }*/
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivity.findViewById(R.id.shutter_button).performClick();
            }
        });
        Thread.sleep(SNAPSHOT_NORMAL_DURATION);
        snapShotCheck();
    }
    private boolean verifyData(boolean isVideo) throws Exception {
        //assertTrue(activity.isFinishing());
        //assertEquals(Activity.RESULT_OK, activity.getResultCode());
        // Verify the video file
        if(!mActivity.isFinishing()){
            testFail = getFailStr("mActivity.isFinishing()",mActivity.isFinishing(),true);
            return false;
        }
        Intent resultData = mActivity.getResultData();
        if(resultData == null){
            testFail = getFailStr("getResultData",resultData,"NotNull");
            return false;
        }

        if(isVideo) {
            mUri = resultData.getData();
            if(mUri == null){
                testFail = getFailStr("mUri",mUri,"NotNull");
                return false;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mActivity, mUri);
            String duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null) {
                testFail = getFailStr("METADATA_KEY_DURATION",duration,"NotNull");
                return false;
            }
            //assertNotNull(duration);
            int durationValue = Integer.parseInt(duration);
            if (durationValue <= 0) {
                testFail = getFailStr("METADATA_KEY_DURATION",durationValue,"above 0");
                return false;
            }
        }else{
            Bundle bundle = resultData.getExtras();
            if (bundle == null) {
                testFail = getFailStr("resultData.getExtras()",bundle,"NotNull");
                return  false;
            }
            Bitmap bitmap = (Bitmap) bundle.getParcelable("data");
            if(bitmap == null ){
                testFail = getFailStr("bitmap",bitmap,"NotNull");
                return false;
            }
            if(bitmap.getWidth() <=0 ){
                testFail = getFailStr("bitmap.getWidth()",bitmap.getWidth(),"above 0");
                return false;
            }
            if(bitmap.getHeight()<=0){
                testFail = getFailStr("bitmap.getHeight()",bitmap.getHeight(),"above 0");
                return false;
            }
/*          assertNotNull(bitmap);
            assertTrue(bitmap.getWidth() > 0);
            assertTrue(bitmap.getHeight() > 0);*/
        }
        return true;
    }
    public void grantPermission() throws Exception{
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean isRequestShown = prefs.getBoolean(CameraSettings.KEY_REQUEST_PERMISSION, false);
        if(isRequestShown){
            executeShellCommand("pm grant org.codeaurora.snapcam android.permission.CAMERA");
            executeShellCommand("pm grant org.codeaurora.snapcam android.permission.RECORD_AUDIO");
            executeShellCommand("pm grant org.codeaurora.snapcam android.permission.ACCESS_COARSE_LOCATION");
            executeShellCommand("pm grant org.codeaurora.snapcam android.permission.ACCESS_FINE_LOCATION");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(CameraSettings.KEY_REQUEST_PERMISSION, false);
            editor.apply();
            Thread.sleep(2000);
            executeShellCommand("input tap 500 500");
            Thread.sleep(4000);
        }else{
            mActivity.mSettingsManager.restoreSettings();
            Thread.sleep(4000);
        }
        mActivity.getCaptureModule().restartAll();
    }

    public void openCameraByIntent(Intent intent)throws Exception{
        mActivityRule.launchActivity(intent);
        Thread.sleep(OPEN_CAMERA_DURATION);
        mActivity = mActivityRule.getActivity();
        mCaptureModule = mActivity.getCaptureModule();
        mCaptureUI = mCaptureModule.getCaptureUI();
        mSettingsManager = mActivity.mSettingsManager;
        mProMode = mCaptureModule.getmCameraControls().getmProMode();
        getUILoc();
    }

    public String executeShellCommand(String cmd) {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try (FileInputStream output = new FileInputStream(
                uiAutomation.executeShellCommand(cmd).getFileDescriptor())) {
            return new String(ByteStreams.toByteArray(output));
        }catch (IOException e){
            Log.i(TAG," e="+e);
            return null;
        }
    }
    private Map<String, String> getCameraIdMapping(){
        String raw = executeShellCommand("dumpsys media.camera").replace("\n", "");
        Matcher m = Pattern.compile("device@[\\d.]+/[\\w]+/(\\d+) [v\\d.()]* static.*?Facing: (Front|Back)").matcher(raw);
        this.mCameraIdMapping.clear();
        while (m.find()) {
            Log.d(TAG, "getCameraIdMapping m.group=" + m.group(1)+","+m.group(2)+",m="+m);
            this.mCameraIdMapping.put(m.group(1), m.group(2));
        }
        Log.d(TAG, "getCameraIdMapping mCameraIdMapping=" + mCameraIdMapping);
        return this.mCameraIdMapping;

    }
    public void getPicSize(){
        String pictureSize = mActivity.mSettingsManager.getValue(SettingsManager.KEY_PICTURE_SIZE);
        int indexX = pictureSize.indexOf('x');
        mPicWidInSet = pictureSize.substring(0, indexX);
        mPicHeiInSet = pictureSize.substring(indexX + 1);
    }

    public void getVideoSize(){
        String videoSize = mActivity.mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        int indexX = videoSize.indexOf('x');
        mVideoWidInSet = videoSize.substring(0, indexX);
        mVideoHeiInSet = videoSize.substring(indexX + 1);
/*        Size snapsize = mCaptureModule.getVideoSnapSize();
        mPicWidInSet = String.valueOf(snapsize.getWidth());
        mPicHeiInSet = String.valueOf(snapsize.getHeight());*/
    }

    public String getActiveCameraId()
    {
        String raw = executeShellCommand("dumpsys media.camera").replace("\n", "");
        Matcher m = Pattern.compile("Active Camera Clients.{3,}Camera ID: (\\d+)").matcher(raw);
        Log.d(TAG, "getActiveCameraFace m="+m );
        String cameraId = "";
        while (m.find()) {
            cameraId = m.group(1);
            return cameraId;
        }
        return null;
    }
    public int getCameraDirectoryJpegAmount()
    {
/*            List<String> results = Arrays.asList(executeShellCommand("find sdcard/DCIM/Camera/ -size +100k | grep -i '\\.jpg' | wc -l")
                    .trim().split("\n"));*/
        String raw = executeShellCommand("find "+mNormalPath+" -size +1M ").replace("\n", "");
        Matcher m = Pattern.compile(NORMAL_IMG).matcher(raw);
        int num = 0;
        while(m.find()){
            num++;
        }
        return num;//Integer.valueOf(file.size());

    }
    private void updateJson(int level,String value){
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int stacklen = stack.length;
        if(stacklen > level) {
            String oldChild = jsonChildNm;
            String oldParent = jsonParentNm;
            jsonChildNm = stack[level - 2].getMethodName();
            jsonParentNm = stack[level].getMethodName();
            if(jsonParentNm.length() > 6 && jsonParentNm.substring(0,6).equals("testIn")) {
                if(value == null) {
                    value = "FAIL";
                    mSupported = true;
                    testResult = true;
                    testFail = value;
                }
                if(!mSupported){
                    value = value+"(NotSupport)";
                }
                updateAndSavejson(OUTPUT_JSON, jsonChildNm, jsonParentNm,value);
            }else{
                jsonChildNm = oldChild;
                jsonParentNm = oldParent;
            }
        }
    }
    public void checkPreview(String id,CaptureModule.CameraMode mode)throws Exception{
        updateJson(5,null);
        if(id != null && id.equals("-1")){
            id = "0";
        }
        Thread.sleep(OPEN_CAMERA_DURATION);
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        if(mCurrentPreviewResult == null){
            testFail = getFailStr("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
            updateJson(5,testFail);
            return;
        }
        //assertNotNull(mCurrentPreviewResult);
        if(mActivity.isFinishing()){
            testFail = getFailStr("Activity_Finshing",mActivity.isFinishing(),false);
            updateJson(5,testFail);
            return;
        }
        //assertFalse(mActivity.isFinishing());
        if(mode != mCaptureModule.getCurrenCameraMode()){
            testFail = getFailStr("CurrenCameraMode",mCaptureModule.getCurrenCameraMode(),mode);
            updateJson(5,testFail);
            return;
        }
        // assertEquals(mode,mCaptureModule.getCurrenCameraMode());
        String mainId = String.valueOf(mCaptureModule.getMainCameraId());
        if(!(id.equals(mainId))){
            testFail = getFailStr("getMainCameraId",mainId,id);
            updateJson(5,testFail);
            return;
        }
        //assertEquals(id,mainId);
        //
        // assertEquals(id,mCurrentPreviewResult.getCameraId());
        String resid = mCurrentPreviewResult.getCameraId();
        if(!(id.equals(resid))){
            testFail = getFailStr("CurrentPreviewResult.getCameraId()",resid,id);
            updateJson(5,testFail);
            return;
        }
        CameraCaptureSession currentSession = mCaptureModule.getCurrentSession(Integer.valueOf(id));
        //boolean sessionclose = mCaptureModule.isSessionClosed(Integer.valueOf(id))
        //assertEquals(false,mCaptureModule.isSessionClosed(Integer.valueOf(mainId)));
        if(currentSession == null){
            testFail = getFailStr("CaptureSession",currentSession,"NotNull");
            updateJson(5,testFail);
            return;
        }
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
    public void resetCapture()throws Exception{
        if(mCaptureModule != null) {
            mCaptureModule.setCaptureResult(null);
            mCaptureModule.setLongImageTitle(new ArrayList<>());
            mCaptureModule.setImagExif(new ArrayList<>());
            mCaptureModule.setImgType(new ArrayList<>());
            mCurrentexif = null;
        }
    }
    public void resetPreview()throws Exception{
        if(mCaptureModule != null) {
            mCaptureModule.setPreviewCaptureResult(null);
            mCaptureModule.resetHashMapTimes();
        }
    }
    public void resetVideo()throws Exception{
        if(mCaptureModule != null) {
            mCaptureModule.setVideFilePath(null);
            mCaptureModule.setLongImageTitle(new ArrayList<>());
            mCaptureModule.setImgType(new ArrayList<>());
            mCaptureModule.setImagExif(new ArrayList<>());
        }
    }

    public void checkVideo(long time)throws Exception{
        getVideoSize();
        //assertFalse(mCaptureModule.mIsRecordingVideo);
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        //assertNotNull(mCurrentPreviewResult);
        String videopath = mCaptureModule.getVideoFilePath();
        // assertNotNull(videopath);
        boolean isRecording = mCaptureModule.mIsRecordingVideo;
        mFrameRate = mActivity.mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        Log.i(TAG,"videopath="+videopath+",mIsRecordingVideo="+mCaptureModule.mIsRecordingVideo+
                ",mCurrentPreviewResult=" +mCurrentPreviewResult+"Current videosize is "
                +mVideoWidInSet+"*"+mVideoHeiInSet+",frameRate in setting is "
                +mFrameRate);
        if(isRecording){
            testFail = getFailStr("IsRecordingVideo",isRecording,"false");
            return;
        }
        if(mCurrentPreviewResult == null){
            testFail = getFailStr("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
            return;
        }
        if(videopath == null){
            testFail = getFailStr("VideoFilePath",videopath,"NotNull");
            return;
        }
        File f = new File(videopath);

        if(!f.exists()){
            testFail = getFailStr("VideoFilePath.exit",f.exists(),"True");
            return;
        }
        if(f.length() < VIDEO_LENGTH){
            testFail = getFailStr("VideoFilePath.length",f.length(),"above "+ VIDEO_LENGTH);
            return;
        }
        /*assertTrue(f.exists());

        assertTrue(f.length() > VIDEO_LENGTH);
        assertTrue(f.length() > VIDEO_LENGTH);*/
        CameraUtil.getMp4Info(f);
        //TestUtil.getVideoInfo(videopath);
        int fps = 30;
        int fpsdiff = 0;
        int fpsInVideo = 0;
        //long timeshow = mCaptureModule.getRecordingTime() ;
        //assertNotNull(mFrameRate);
        if (mFrameRate != null && !mFrameRate.equals("off")) {
            String mode = mFrameRate.substring(0, 3);
            fps = Integer.parseInt(mFrameRate.substring(3));
            if (mode.equals("hfr")) {
                time = time * (fps / 30);
                fps = 30;
                fpsInVideo = CameraUtil.mFps;
            }else{
                fpsInVideo =Integer.parseInt(CameraUtil.mFrameRate);
            }
            fpsdiff = fps - fpsInVideo;
        }
        long timediff = time - CameraUtil.timeInMillisec;
        //long showdiff = timeshow - CameraUtil.timeInMillisec ;

        //assertEquals(mVideoWidInSet,CameraUtil.mWidth);
        //assertEquals(mVideoHeiInSet,CameraUtil.mHeight);
        if(!mVideoWidInSet.equals(CameraUtil.mWidth) || !mVideoHeiInSet.equals(CameraUtil.mHeight)){
            // reportErrorInfo("VideoSize.getWidth",CameraUtil.mWidth,mVideoWidInSet);
            testFail = getFailStr("VideoSize(width*height)",CameraUtil.mWidth+"*"+
                    CameraUtil.mHeight,mVideoWidInSet+"*"+mVideoHeiInSet);
            return;
        }
        if( checkfps && (fpsdiff >5 || fpsdiff <-5)){
            testFail = getFailStr("video FrameRate",fpsInVideo,fps);
            return;
        }
        /*
        if(timediff >1000 || timediff <-1000){
            throw new AssertionError("duration should be  " + time +
                    ",but  duration in the file is  "+CameraUtil.timeInMillisec);
        }
       if(showdiff >3000 || showdiff <-3000){
            throw new AssertionError("show time is  " + timeshow +
                    ",but  duration in the file is  "+TestUtil.mDuration);
        }*/

    }

    public void checkSnapShot(String path,ExifInterface exif) throws Exception {
        if(!testResult) return;
        if(!mCaptureModule.getCaptureUI().isShutterEnabled()){
            testFail = getFailStr("isShutterEnabled",mCaptureModule.getCaptureUI().isShutterEnabled(),true);
            return;
        }
        int index = path.indexOf(".");
        String type = path.substring(index);
        String value = mSettingsManager.getValue(SettingsManager.KEY_PICTURE_FORMAT);
        String format =".jpg";
        String raw = mSettingsManager.getKeyValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
        if(value.equals("1")){
            format = ".heic";
        }
        if(!format.equals(type) && raw.equals("0") ){
            testFail = getFailStr("image foramt ",type,format);
            return;
        }

        if(type.equals(".raw")|| type.equals(".yuv")){
            path = path + ".jpg";
        }
        File f = new File(path);
        if(isOpenFromIntent){
            if(f.exists()){
                testFail = getFailStr("file("+f+") is exit?",f.exists(),false);
            }
            return;
        }
        if(!f.exists()){
            testFail = getFailStr("file("+f+") is exit?",f.exists(),true);
            return;
        }
        if(f.length() < 1024){
            testFail = getFailStr("imagepath.length()",f.length(),"above 1024");
            return;
        }
        if(type.equals(".jpg") || type.equals(".heic") || type.equals(".dng")) {
            try {
                mCurrentexif = new ExifInterface(path);
            } catch (Exception e) {
                Log.i(TAG, "getexif e=" + e);
                mCurrentexif = exif;
            }
            if (mCurrentexif == null) {
                testFail = getFailStr("imagepath.exif", mCurrentexif, "NotNull");
                return;
            }

            String wInExif = mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            String hInExif = mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            Size size = getImgSize(path);
            if(mCaptureModule.isRecordingVideo()){
                size = mCaptureModule.getVideoSnapSize();
            }
            if (size.getWidth() != Integer.valueOf(wInExif) || size.getHeight() != Integer.valueOf(hInExif)) {
                testFail = getFailStr("ImageSize in exif", wInExif + "*" + hInExif,
                        size.getWidth() + "*" + size.getHeight());
                return;
            }
        }

    }
    private Size getImgSize(String path){
        String format = mSettingsManager.getKeyValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
        Size picSize = mCaptureModule.getSnapShotJPGSize();
        int suffixindex = path.indexOf(".");
        String suffix = path.substring(suffixindex);
        if(".raw.jpg".equals(suffix) || ".dng".equals(suffix)){
            picSize = mCaptureModule.getSnapShotRawSize();
        }else if(".yuv.jpg".equals(suffix) && (format != null && (format.equals("54") ||
                format.equals("99")))){
            picSize = mCaptureModule.getYUVP010Size();
        }
        return picSize;
    }
    private int getImageNum(){
        int imgnm = 1;
        String value = mSettingsManager.getKeyValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
        if(value != null && (value.equals("10") || value.equals("16"))){
            imgnm =2;
        }
        return imgnm;
    }
    private boolean isSupportSnapShot(CaptureModule.CameraMode mode) {
        if (mode == CaptureModule.CameraMode.HFR || (isOpenFromIntent && isVideoMode(mode))) {
            return false;
        }else{
            return true;
        }

    }
    public void checkFlash(String setvalue) throws Exception {
        if (!testResult) return;
        int flashInResult = 0;
        int aeInResult = 0;
        int flashInExif = 0;
        int flashStateInCap = 0;
        CaptureModule.CameraMode mode = mCaptureModule.getCurrenCameraMode();
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        mCurrentCaptureResult = mCaptureModule.getCaptureResult();
        boolean isVideoMode = mode == CaptureModule.CameraMode.HFR ||
                mode == CaptureModule.CameraMode.VIDEO;
        String flashinset = mActivity.mSettingsManager.getValue((isVideoMode ||
                mode == CaptureModule.CameraMode.PRO_MODE) ?
                SettingsManager.KEY_VIDEO_FLASH_MODE : SettingsManager.KEY_FLASH_MODE);
        if (isSupportSnapShot(mode)) {
            if (mCurrentexif == null && !isOpenFromIntent) {
                testFail = getFailStr("Currentexif", mCurrentexif, "NotNull");
                return;
            }
            flashInResult = mCurrentCaptureResult.get(CaptureResult.FLASH_MODE);
            aeInResult = mCurrentCaptureResult.get(CaptureResult.CONTROL_AE_MODE);
            if (!isOpenFromIntent) {
                flashInExif = mCurrentexif.getAttributeInt(ExifInterface.TAG_FLASH, 0);
            }
            flashStateInCap = mCurrentCaptureResult.get(CaptureResult.FLASH_STATE);
        }
        int flashInPreview = mCurrentPreviewResult.get(CaptureResult.FLASH_MODE);
        int aeInPreview = mCurrentPreviewResult.get(CaptureResult.CONTROL_AE_MODE);
        int flashStateInPre = mCurrentPreviewResult.get(CaptureResult.FLASH_STATE);

        boolean isTriggered = mCurrentPreviewResult.get(CaptureResult.CONTROL_AE_STATE) == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
        Log.i(TAG, "flashinset=" + flashinset + ",flashInResult=" + flashInResult + ",aeInResult=" + aeInResult + ",isTriggered=" + isTriggered
                + ",aeInPreview=" + aeInPreview + ",flashInPreview=" + flashInPreview + ",flashInExif=" + flashInExif
        +",flashStateInCap="+flashStateInCap+",flashStateInPre="+flashStateInPre);
        //assertEquals(setvalue,flashinset);
        if (!setvalue.equals(flashinset)) {
            testFail = getFailStr("flashinset", flashinset, setvalue);
            return;
        }

        switch (flashinset) {
            case "on":
                if (!isVideoMode) {
                    //assertEquals(CaptureResult.FLASH_MODE_SINGLE,flashInResult);
                    //assertEquals( CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH,aeInResult);
                    // assertTrue((flashInExif & FLASH_ON) > 0);
                    if (CaptureResult.FLASH_MODE_SINGLE != flashInResult) {
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_SINGLE);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH != aeInResult) {
                        testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    }
                  /*  if (!isOpenFromIntent && flashInExif != FLASH_ON) {
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_ON);
                        return;
                    }*/
                    if(FLASH_STATE_READY != flashStateInPre || FLASH_STATE_FIRED != flashStateInCap){
                        testFail = getFailStr("FLASH_STATE in preview is"+flashStateInPre+",incapture is "+flashStateInCap
                                , flashStateInCap, "in preview is"+FLASH_STATE_READY+",incapture is "+FLASH_STATE_FIRED);
                        return;
                    }
                } else {
                    if (CaptureResult.FLASH_MODE_TORCH != flashInPreview) {
                        testFail = getFailStr("FLASH_MODE in preview", flashInPreview, CaptureResult.FLASH_MODE_TORCH);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON != aeInPreview) {
                        testFail = getFailStr("CONTROL_AE_MODE in preview", aeInPreview, CaptureResult.CONTROL_AE_MODE_ON);
                        return;
                    }
                    if(FLASH_STATE_FIRED != flashStateInPre){
                        testFail = getFailStr("FLASH_STATE in preview is", flashStateInPre,FLASH_STATE_FIRED);
                        return;
                    }
                    //assertEquals(CaptureResult.FLASH_MODE_TORCH, flashInPreview);
                    // assertEquals(CaptureResult.CONTROL_AE_MODE_ON, aeInPreview);
                    if (isSupportSnapShot(mode)) {
                        // assertEquals(CaptureResult.FLASH_MODE_TORCH, flashInResult);
                        // assertEquals(CaptureResult.CONTROL_AE_MODE_ON, aeInResult);
                        //assertTrue((flashInExif & FLASH_ON) > 0);
                        if (CaptureResult.FLASH_MODE_TORCH != flashInResult) {
                            testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_TORCH);
                            return;
                        }
                        if (CaptureResult.CONTROL_AE_MODE_ON != aeInResult) {
                            testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON);
                        }
                        if(FLASH_STATE_FIRED != flashStateInCap){
                            testFail = getFailStr("FLASH_STATE in capture is", flashStateInCap,FLASH_STATE_FIRED);
                            return;
                        }
                      /*  if (!isOpenFromIntent) {
                            if (flashInExif != FLASH_ON) {
                                testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_ON);
                                return;
                            }
                        }*/
                    }
                }
                break;
            case "auto":
                //assertEquals(CaptureResult.FLASH_MODE_SINGLE,flashInResult);
                //assertEquals(CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,aeInResult);

                if (CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH != aeInResult) {
                    testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    return;
                }

                if(FLASH_STATE_READY != flashStateInPre){
                    testFail = getFailStr("FLASH_STATE in preview is", flashStateInPre,FLASH_STATE_FIRED);
                    return;
                }
                if (isTriggered) {
                    // assertTrue((flashInExif & FLASH_ON) > 0);
                    if (CaptureResult.FLASH_MODE_SINGLE != flashInResult) {
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_SINGLE);
                        return;
                    }
                    if(FLASH_STATE_FIRED != flashStateInCap){
                        testFail = getFailStr("FLASH_STATE in capture is", flashStateInCap,FLASH_STATE_FIRED);
                        return;
                    }
                   /* if (flashInExif != FLASH_AUTO_ON && !isOpenFromIntent) {
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_AUTO_ON);
                        return;
                    }*/
                } else {
                    // assertTrue((flashInExif & FLASH_OFF) > 0);
/*                    if (flashInExif != FLASH_AUTO_OFF && !isOpenFromIntent) {
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_AUTO_OFF);
                        return;
                    }*/
                    if(FLASH_STATE_READY != flashStateInCap){
                        testFail = getFailStr("FLASH_STATE in capture is", flashStateInCap,FLASH_STATE_FIRED);
                        return;
                    }
                }
                break;
            case "off":
                if (isSupportSnapShot(mode)) {
                    //assertEquals(CaptureResult.FLASH_MODE_OFF, flashInResult);
                    //assertEquals(CaptureResult.CONTROL_AE_MODE_ON, aeInResult);
                    //assertTrue((flashInExif & FLASH_OFF) > 0);
                    if (CaptureResult.FLASH_MODE_OFF != flashInResult) {
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_OFF);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON != aeInResult) {
                        testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON);
                        return;
                    }
                    if(FLASH_STATE_READY != flashStateInCap){
                        testFail = getFailStr("FLASH_STATE in capture is", flashStateInCap,FLASH_STATE_FIRED);
                        return;
                    }
/*                    if (!isOpenFromIntent && flashInExif != FLASH_OFF) {
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_OFF);
                        return;
                    }*/
                }
                //assertEquals(CaptureResult.FLASH_MODE_OFF,flashInPreview);
                //assertEquals(CaptureResult.CONTROL_AE_MODE_ON,aeInPreview);
                if (CaptureResult.FLASH_MODE_OFF != flashInPreview) {
                    testFail = getFailStr("FLASH_MODE in preview", flashInPreview, CaptureResult.FLASH_MODE_OFF);
                    return;
                }
                if (CaptureResult.CONTROL_AE_MODE_ON != aeInPreview) {
                    testFail = getFailStr("CONTROL_AE_MODE in preview", aeInPreview, CaptureResult.CONTROL_AE_MODE_ON);
                    return;
                }
                if(FLASH_STATE_READY != flashStateInPre){
                    testFail = getFailStr("FLASH_STATE in preview is", flashStateInPre,FLASH_STATE_FIRED);
                    return;
                }
                break;
            case "alwayson":
                if (CaptureResult.FLASH_MODE_TORCH != flashInPreview) {
                    testFail = getFailStr("FLASH_MODE in preview", flashInPreview, CaptureResult.FLASH_MODE_TORCH);
                    return;
                }
                if (CaptureResult.CONTROL_AE_MODE_ON != aeInPreview) {
                    testFail = getFailStr("CONTROL_AE_MODE in preview", aeInPreview, CaptureResult.CONTROL_AE_MODE_ON);
                    return;
                }
                if(FLASH_STATE_FIRED != flashStateInPre){
                    testFail = getFailStr("FLASH_STATE in preview is", flashStateInPre,FLASH_STATE_FIRED);
                    return;
                }
                if (isSupportSnapShot(mode)) {
                    if (CaptureResult.FLASH_MODE_TORCH != flashInResult) {
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_TORCH);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON != aeInResult) {
                        testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON);
                    }
                    if(FLASH_STATE_FIRED != flashStateInCap){
                        testFail = getFailStr("FLASH_STATE in capture is", flashStateInCap,FLASH_STATE_FIRED);
                        return;
                    }
                }
                break;
        }
    }


    public void checkHdr(int value){
        if(!testResult)return;
        String sceneValue = mActivity.mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        int setValue = Integer.parseInt(sceneValue);
        //assertEquals(value,setValue);
        if(value != setValue){
            testFail = getFailStr("KEY_SCENE_MODE",setValue,value);
            return;
        }
        if(value == HDR_SCENE_OFF){
            value = CONTROL_MODE_AUTO;
        }
        int sceneMode = mCurrentCaptureResult.get(CaptureResult.CONTROL_SCENE_MODE);
        if(value != sceneMode){
            testFail = getFailStr("CONTROL_SCENE_MODE",sceneMode,value);
            return;
        }
        //assertEquals(value,sceneMode);
    }
    private void checkMFNR(){
        if(!testResult)return;
        int noiseInResult = mCurrentCaptureResult.get(CaptureResult.NOISE_REDUCTION_MODE);
        int noiseReduMode = (mCaptureModule.isMFNREnabled() ? CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY :
                CameraMetadata.NOISE_REDUCTION_MODE_FAST);
        if(noiseReduMode != noiseInResult){
            //reportErrorInfo("noiseInResult",noiseInResult,noiseReduMode);
            testFail = getFailStr("NOISE_REDUCTION_MODE",noiseInResult,noiseReduMode);
            return;
        }
        // assertEquals(noiseReduMode,noiseInResult);
    }
    public void checkZoomValue(CaptureModule.CameraMode mode,float zoomText,boolean change)throws Exception{//HardSwitch with cameraid changed need hal support
        float zoomInPre = mCurrentPreviewResult.get(CaptureResult.CONTROL_ZOOM_RATIO );
        float zoomStr = mActivity.getCaptureModule().getZoomValue();
        if(change && zoomStr == mOldZoomstr){
            testFail = getFailStr("zoom should be changed,oldzoom is "+mOldZoomstr+
                    ",new zoom is the same ",zoomStr,"different");
            return;
        }
        if(zoomText != zoomStr){
            testFail = getFailStr("zoomText is  "+zoomText+
                    ",zoomvalue is  ",zoomStr,"same");
            return;
        }
        mOldZoomstr = zoomStr;
        // assertNotEquals(zoomStr,mOldZoomstr);
        if(mode != CaptureModule.CameraMode.HFR && !isOpenFromIntent){
            float zoomInCap = mCurrentCaptureResult.get(CaptureResult.CONTROL_ZOOM_RATIO);
            if(zoomStr != zoomInCap ){
                testFail = getFailStr("zoomInCapture:CONTROL_ZOOM_RATIO",zoomInCap,zoomStr);
                return;
            }
            if(zoomStr != zoomInPre){
                testFail = getFailStr("zoomInPreview:CONTROL_ZOOM_RATIO",zoomInPre,zoomStr);
                return;
            }
            // assertEquals(zoomStr,zoomInCap);
            // assertEquals(zoomStr,zoomInPre);
        }else{
            Rect mCropRegion = mCurrentPreviewResult.get(CaptureResult.SCALER_CROP_REGION);
            if(mOldZoomRegion == mCropRegion){
                testFail = getFailStr("SCALER_CROP_REGION should change","mCropRegion==mOldZoomRegion","different");
                return;
            }
            // assertNotEquals(mOldZoomRegion,mCropRegion);
            mOldZoomRegion = mCropRegion;
        }
    }
    public void checkZSL(){
        if(!testResult) return;
        boolean zslset = mActivity.mSettingsManager.isZSLInHALEnabled();
        boolean zslresult =  mCurrentCaptureResult.get(CaptureResult.CONTROL_ENABLE_ZSL);
        if(zslset != zslresult){
            testFail = getFailStr("CONTROL_ENABLE_ZSL",zslresult,zslset);
            return;
        }
        //assertEquals(zslset,zslresult);
    }
    public void swipFromLTR(int swipnum)throws Exception{
        int changevalue = swipevalue - 100;
        for(int i = 0;i < swipnum;i++){
            executeShellCommand("input swipe " +swipevalue + " "+swipevalue +" "+changevalue + " "+swipevalue);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
    }
    public void swipFromRTL(int swipnum)throws Exception{
        int changevalue = swipevalue + 100;
        for(int i = 0;i < swipnum;i++){
            executeShellCommand("input swipe " +swipevalue + " "+swipevalue +" "+changevalue + " "+swipevalue);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
    }

    public void getKeyValue() {
        isovalue = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_ISO);
        wbvalue = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_WHITE_BALANCE);
        evvalue = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_EXPOSURE);
    }
    public void pressDone(){
        CaptureModule.CameraMode mode = mCaptureModule.getCurrenCameraMode();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.done_button).performClick();
            }
        });
        try {
            Thread.sleep(OPEN_CAMERA_DURATION);
            boolean done = verifyData(isVideoMode(mode));
            Log.i(TAG,"press done testResult="+testResult+",done="+done);
            testResult = testResult && done;
        }catch (Exception e){
            Log.i(TAG," exception="+e);
        }
    }

    private void pressCancel() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.preview_btn_cancel).performClick();
            }
        });
    }


    private void pressSwitchIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.front_back_switcher).performClick();
            }
        });
    }

    private void pressFlash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.flash_button).performClick();
            }
        });
    }
    private void pressEVtext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.exposure_text).performClick();
            }
        });
    }
    private void pressWBtext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.whitebalance_text).performClick();
            }
        });
    }
    private void pressISOtext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.iso_text).performClick();
            }
        });
    }
    private void pressFocusDistext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.focusdistance_text).performClick();
            }
        });
    }
    private void pressShutterSpeedtext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().findViewById(R.id.shutterspeed_text).performClick();
            }
        });
    }
    public HashMap<String,Long>  getHashMapValue (HashMap<String,Long> inHash){
        HashMap<String,Long>  outHash = new HashMap();
        Iterator it = inHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            outHash.put((String)key, (Long)val);
        }
        inHash.clear();
        mCaptureModule.setStartedTime(0);
        mCaptureModule.resetHashMapTimes();
        return outHash;
    }

    public static String getIndexStr(String testStr,int index){
        String item ="";
        if(index >=0) {
            String test = testStr.substring(index);
            int tmp = test.indexOf("-");
            if (tmp > 0) {
                item = test.substring(0, tmp - 1);
            } else {
                item = test;
            }
            item = item.trim();
            return item;
        }else{
            return null;
        }
    }

    public static void getTestItem(String testStr) {
        if (testStr != null) {
            int indexM = testStr.indexOf("-m");
            int indexT = testStr.indexOf("-t");
            int indexNT = testStr.indexOf("-nt");
            Log.i("getTestItem", "initJson indexM=" + indexM + ",indexT=" + indexT+",indexNT="+indexNT);
            if(indexM != -1) {
                functionTestMode = getIndexStr(testStr, indexM + 2);
            }
            if(indexT != -1) {
                functionTestItem = getIndexStr(testStr, indexT + 2);
            }
            if(indexNT != -1) {
                functionTestItemDel = getIndexStr(testStr, indexNT + 3);
            }
            Log.i("getTestItem", "initJson " +
                    "functionTestItem=" + functionTestItem + ",functionTestItemDel=" + functionTestItemDel
                    + ",functionTestMode=" + functionTestMode);
        }
    }
    private boolean testItem(String testCase){
        if(functionTestItem != null && !functionTestItem.contains(testCase)){
            return false;
        }else if(functionTestItemDel != null && functionTestItemDel.contains(testCase)){
            return false;
        }
        return true;
    }

    public void switchModeTextToR(boolean lToR)throws Exception {
        int[] SlideModeTxt = mModeIconR.get("SlideModeTxt");
        int swipevalue = 0;
        if(lToR){
            swipevalue = SlideModeTxt[0] - SWIPE_STEP;
        }else{
            swipevalue = SlideModeTxt[0] + SWIPE_STEP;
        }
        executeShellCommand("input swipe " +SlideModeTxt[0] + " "+SlideModeTxt[1] +" "+ swipevalue + " "+SlideModeTxt[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
    }
}