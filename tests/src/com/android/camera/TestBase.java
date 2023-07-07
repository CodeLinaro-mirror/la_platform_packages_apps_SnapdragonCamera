/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
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

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CameraMetadata;

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

import android.hardware.camera2.CameraCaptureSession;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Bundle;



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
    public static final int FLASH_ON = 9;
    public static final int FLASH_OFF = 16;
    public static final int FLASH_AUTO_OFF = 24;
    public static final int FLASH_AUTO_ON = 25;
    public static final int CONTROL_MODE_HDR = 18;
    public static final int HDR_SCENE_OFF = 0;
    public static final int CONTROL_MODE_AUTO = 1;
    private static final String OUTPUT_JSON = "/data/data/org.codeaurora.snapcam/files/testResult.json";
    public static final String INPUT_JSON = "/data/data/org.codeaurora.snapcam/files/autoTest.json";


    public int[] mShutterLoc = new  int[2];
    public int[] mFlashLoc = new  int[2];
    public int[] mHdrLoc = new  int[2];
    public int[] mZoomBarLoc = new  int[2];
    public int[] mZoomValueLoc = new  int[2];

    public int[] mSwitchLoc = new  int[2];
    public int[] mEVLoc = new  int[2];
    public int[] mFocDisLoc = new  int[2];
    public int[] mShutSpeedLoc = new  int[2];
    public int[] mWBLoc = new  int[2];
    public int[] mISOLoc = new  int[2];
    public int[] mVideoLoc = new  int[2];
    public int[]mPauseLoc = new  int[2];
    public int[]mModeLoc = new  int[2];
    public int[]mSettingLoc = new  int[2];
    public int[]mCancelLoc = new  int[2];

    public int[]mReviewCancelLoc = new  int[2];
    public int[]mDoneLoc = new  int[2];
    public int[]mRetakeLoc = new  int[2];

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
    private ProMode mProMode;
    private CharSequence[] isovalue,evvalue,wbvalue;
    private int swipevalue;
    private boolean updateJson = false;
    private String jsonChildNm = null;
    private String jsonParentNm = null;
    private boolean testResult = false;
    private boolean flashInZslResult = true;
    private boolean longshotInZslResult = true;
    private boolean mSupported = true;
    private String testPass = "PASS";
    private String testFail = "FAIL";
    public HashMap<String,HashMap<String,Long>> performenceValues = new HashMap<String,HashMap<String,Long>>();
    public boolean isPerformenceTest = false;
    public boolean isOpenFromIntent = false;
    private Uri mUri;
    private int methodLevel = 5;
    private boolean checkfps = false;

    public static void init(){
        uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mActivityRule = new ActivityTestRule<>(CameraActivity.class);
    }

    public void OpenAndResetCamera() throws Exception{
        openCameraByIntent(mMainIntent);
        mActivity.mSettingsManager.restoreSettings();
        Thread.sleep(SMALL_WAIT_DURATION);
        mActivity.getCaptureModule().restartAll();
        checkPreview("0", CaptureModule.CameraMode.DEFAULT);
        isOpenFromIntent = false;
    }
    public void OpenCamera() throws Exception{
        openCameraByIntent(mMainIntent);
        checkPreview("0", CaptureModule.CameraMode.DEFAULT);
    }
    public static void updateAndSavejson(String inputJson,String childNm,String parentNm,String value){
        JSONObject mObj= CameraUtil.getJsonObj(inputJson);
        if (mObj == null) {
            Log.e("autotest_updateAndSavejson","Json file not exit,will not save the test result");
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
    public  void updatePerformenceJson(String jsonFile,String[]item,String jsonArray,
                                             List<Long>values,int times){
        JSONObject mObj= CameraUtil.getJsonObj(jsonFile);
        if (mObj == null) {
            Log.e(TAG,"Json file not exit,will not save the test result.jsonFile is "+jsonFile);
            return;
        }
        if(values != null && jsonArray != null) {
            try {
                JSONArray array = mObj.getJSONArray(jsonArray);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int count = obj.getInt("testTimes");
                    if (times == count) {
                        for(int j = 0;j < item.length;j++){
                            obj.put(item[j],values.get(j));
                        }
                        break;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "updatejsonobj e= " + e);
            }
        }
        saveJson(jsonFile,mObj);
    }

    public  void updatePerformenceJson(String jsonFile,String[]item,String jsonArray,
                                       HashMap<Integer, List<Long> > values){
        JSONObject mObj= CameraUtil.getJsonObj(jsonFile);
        if (mObj == null) {
            Log.e(TAG,"Json file not exit,will not save the test result.jsonFile is "+jsonFile);
            return;
        }
        if(values != null && jsonArray != null) {
            try {
                JSONArray array = mObj.getJSONArray(jsonArray);
                for(int j = 0;j <values.size();j++) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int count = obj.getInt("testTimes");
                        if (j == count) {
                            for (int k = 0; k < item.length; j++) {
                                obj.put(item[k], values.get(j).get(k));
                            }
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "updatejsonobj e= " + e);
            }
        }
        saveJson(jsonFile,mObj);
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

    private void saveJson(String jsonFile, JSONObject obj) {
        try {
            FileWriter fileWriter = new FileWriter(jsonFile);
            fileWriter.write(obj.toString());
            fileWriter.flush();
        } catch (Exception e) {
            Log.e(TAG, " writejsonobj e= " + e);
        }

    }
    public void runPhotoCase(String cameraId,CaptureModule.CameraMode mode) throws Exception {
        Log.i(TAG,"testphotocase mode="+mode+",id="+cameraId);
        checkPreview(cameraId,mode);
        if(!testResult){
            return;
        }
        testSnapshot(mode);
        if(isOpenFromIntent){
            openCameraByIntent(mImageIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testHdr(mode);
        if(isOpenFromIntent){
            openCameraByIntent(mImageIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testFlash(cameraId,mode,false);
        if(isOpenFromIntent){
            openCameraByIntent(mImageIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testZoomBar(mode);
        if(isOpenFromIntent){
            openCameraByIntent(mImageIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testSettingIcon(cameraId,mode);
        if(isOpenFromIntent){
            openCameraByIntent(mImageIntent);
            checkPreview("0",mode);
        }
        if(cameraId.equals("1")) {
            testToggleBackFront(mode,false);
        }else{
            testToggleBackFront(mode,true);
        }
        if(isOpenFromIntent){
            return;
        }
        testLongShot(cameraId,mode);
        testPictureSize(cameraId,mode);
        if (mode != CaptureModule.CameraMode.PRO_MODE) {
            testMFNR(cameraId,mode);
            if(mode != CaptureModule.CameraMode.RTB){
                testZSL(cameraId,mode);
                if(!cameraId.equals("1")) {
                    testCamID(mode);//This testcase should be the last one due to the "checkpreview" will return if failed.
                }
            }
        } else {
            testEV();
            testWB();
            testISO();
            testShutterSpeed();
            testFocusDistance();
            testAllInPro();
        }
    }

    public void runVideoCase(String cameraId,CaptureModule.CameraMode mode) throws Exception {
        checkPreview(cameraId,mode);
        if(!testResult){
            return;
        }
        testVideo(mode,true);
        if(isOpenFromIntent){
            openCameraByIntent(mVideoIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testHdr(mode);
        testFlash(cameraId,mode,false);
        if(isOpenFromIntent){
            openCameraByIntent(mVideoIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testZoomBar(mode);
        if(isOpenFromIntent){
            openCameraByIntent(mVideoIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testVideoPauseAndResume();
        if(isOpenFromIntent){
            openCameraByIntent(mVideoIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        testSettingIcon(cameraId,mode);
        if(isOpenFromIntent){
            openCameraByIntent(mVideoIntent);
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
        if(cameraId.equals("1")) {
            testToggleBackFront(mode,false);
        }else{
            testToggleBackFront(mode,true);
        }
        if(isOpenFromIntent){
            return;
        }
        testVideoSizeAndFrameRate(cameraId,mode);
        if (mode != CaptureModule.CameraMode.HFR &&  !cameraId.equals("1")
        && mode != CaptureModule.CameraMode.CINEMATIC) {
            testCamID(mode);
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
                mode == CaptureModule.CameraMode.PRO_MODE) {
            View mToggle = mActivity.findViewById(R.id.front_back_switcher);
            if (mToggle.getVisibility() != View.VISIBLE) {
                testResult = true;
                mSupported = false;
            } else {
                testResult = false;
                Log.e(TAG, "TestFail reason:BackFontSwitch icon should not be visible");
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
                testVideo(mode,true);
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
                testVideo(mode,true);
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
        executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
/*        if(!mActivity.isFinishing()){
            testResult = false;
            Log.e(TAG,"TestFail reason:cameraactivity should finished.activity.isFinishing()=" +mActivity.isFinishing());
            return;
        }*/
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        checkPreview(cameraid,mode);
        if(!testResult){
            updateJson(5,testFail);
        }
        if(isVideoMode(mode)){
            testVideo(mode,true);
        }else{
            testSnapshot(mode);
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
    public void testFlash(String cameraid,CaptureModule.CameraMode mode,boolean isPerformenceTest) throws Exception {
        updateJson(5,null);
        if(cameraid.equals("1") || mode == CaptureModule.CameraMode.CINEMATIC){
            View mFlash = mActivity.findViewById(R.id.flash_button);
            if(mFlash.getVisibility() != View.VISIBLE){
                testResult = true;
                mSupported = false;
            }else{
               testFail = getFailStr("flash_button.getVisibility",mFlash.getVisibility(),"INVISIBLE");
            }
            if(testResult){
                updateJson(5,testPass);
            }
        }else {
            boolean intenton = false;
            boolean intentoff = false;
            boolean intentauto = false;
            executeShellCommand("input tap " + mFlashLoc[0] + " " + mFlashLoc[1]);
            if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.VIDEO ||
                    mode == CaptureModule.CameraMode.HFR) {
                clickShutterButton(mode);
                checkFlash("on");
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
                    if(intentauto && intenton && intentoff){
                        updateJson(5,testPass);
                    }else {
                        updateJson(5,testFail);
                    }
                    return;
                }
                if(isPerformenceTest){
                    mCaptureModule.resetHashMapTimes();
                }
                if(checkauto && checkoff && checkon){
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
    private boolean showHdr(CaptureModule.CameraMode mode){
        boolean show = true;
        if(mode != CaptureModule.CameraMode.DEFAULT && mode != CaptureModule.CameraMode.RTB){
            show = false;
        }
        return show;
    }
    public void testHdr(CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        View mHdr = mActivity.findViewById(R.id.scene_mode_hdr);
        if (!showHdr(mode)) {
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
        }
    }

    public void testPictureSize(String cameraid,CaptureModule.CameraMode mode) throws Exception{
        updateJson(5,null);
        executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);
        executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
        checkPreview(cameraid,mode);
        int length =  mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_PICTURE_SIZE).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_PICTURE_SIZE);
        boolean checkresult = true;
        for(int i = 0;i < length; i++ ){
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_PICTURE_SIZE,i);
            testSettingIcon(cameraid,mode);
            if(!testResult) {
                checkresult = false;
            }
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_PICTURE_SIZE,defvalue);
        testSettingIcon(cameraid,mode);
        if(testResult && checkresult) updateJson(5,testPass);
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
        int length = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_QUALITY).length;
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
                if(!testResult) {
                    checkresult = false;
                    break;
                }
            }
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_QUALITY, defvalue);
        testSettingIcon(cameraid, mode);
        checkfps = false;
        if(checkresult && testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
        //if(testResult) updateJson(5,testPass);
    }

/*    public void testVideoSize(CaptureModule.CameraMode mode) throws Exception {
        int length = mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_VIDEO_QUALITY).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_VIDEO_QUALITY);
        for (int i = 0; i < length; i++) {
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_VIDEO_QUALITY, i);
            String videosize = mActivity.mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
            List<String> framerate = mActivity.mSettingsManager.getSupportedHighFrameRate(mode,videosize,mCaptureModule.getMainCameraId());
            if(framerate.size() < 1){
                throw new AssertionError("There is no framerate for this size "+videosize);
            }
            for(int j = 0;j < framerate.size();j++){
                mFrameRate = framerate.get(j);
                mActivity.mSettingsManager.setValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE,mFrameRate);
                mCaptureModule.restartSession(false);
                Thread.sleep(OPEN_CAMERA_DURATION);
                testVideo(mode);
            }
        }
    }*/
    public void testLongShot(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5, null);
        mActivity.mSettingsManager.setValue(SettingsManager.KEY_LONGSHOT, "on");
        testSettingIcon(cameraId,mode);
        resetCapture();
        mLongShotNum = 0;
        if (mode != CaptureModule.CameraMode.PRO_MODE) {
            executeShellCommand("input swipe " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + 1000);
            Thread.sleep(SNAPSHOT_NORMAL_FLAH_OFF * 2);
            checkLongShot(false);
            resetCapture();
            if(!testResult){
                mActivity.mSettingsManager.setValue(SettingsManager.KEY_LONGSHOT, "off");
                return;
            }
            executeShellCommand("input swipe " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + mShutterLoc[0] + " " + mShutterLoc[1] + " " + 5000);
            Thread.sleep(SNAPSHOT_NORMAL_FLAH_OFF * 5);
            checkLongShot(true);
            resetCapture();
        }else{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mActivity.findViewById(R.id.shutter_button).performLongClick();
                }
            });
            Thread.sleep(SNAPSHOT_NORMAL_DURATION);
            checkLongShot(false);
        }
        mActivity.mSettingsManager.setValue(SettingsManager.KEY_LONGSHOT, "off");
        if (testResult) updateJson(5, testPass);
    }
    private void checkLongShot(boolean max)throws Exception{
        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        if(patharry == null){
/*            testResult = false;
            Log.e(TAG,"TestFail reason:imagtitle is null");*/
            testFail = getFailStr("imagepath",patharry,"NotNull");
            return;
        }
       // assertNotNull(patharry);

        if(patharry.size() > PersistUtil.getLongshotShotLimit() || patharry.size() ==0){
           // throw new AssertionError("longshot image num shoud not 0 and > getLongshotShotLimit" +
            //        ",but ten num is "+patharry.size());
      /*      Log.e(TAG,"TestFail reason:patharry.size ="+patharry.size());
            testResult = false;*/
            testFail = getFailStr("image_num",patharry.size(),"below "+  PersistUtil.getLongshotShotLimit());
            return;
        }
        if( patharry.size() ==0){
            testFail = getFailStr("image_num",patharry.size(),"above 0");
            return;
        }
/*        if(max && patharry.size() != PersistUtil.getLongshotShotLimit()){
            Log.e(TAG,"TestFail reason:With long click for a long time,patharry.size should be " +
                    "getLongshotShotLimit ="+PersistUtil.getLongshotShotLimit()+
                    ",but patharry.size() is )"+patharry.size());
            testResult = false;
            return;
        }*/
        if(mLongShotNum == patharry.size()){
            testFail = getFailStr("imag_num shoule different","mLongShotNum == patharry.size()","different");
            return;
        }
        for(int i=0;i<patharry.size();i++){
            String path = mNormalPath + patharry.get(i)+NORMAL_IMG;
            checkSnapShot(path);
        }
        //assertNotEquals(mLongShotNum,patharry.size());
        mLongShotNum = patharry.size();
    }
    public void testZSL(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String parentNm = stack[4].getMethodName();
        String flashvalue ="FAIL";
        String longshotvalue = "FAIL";
        updateAndSavejson(OUTPUT_JSON, "testFlashInZSL", parentNm,flashvalue);
        updateAndSavejson(OUTPUT_JSON, "testLongShotInZSL", parentNm,longshotvalue);
        mActivity.setDevOption(true);
        testResult = true;
        flashInZslResult = true;
        longshotInZslResult = true;
        int length =  mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_ZSL).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_ZSL);
        for(int i = 0;i <length; i++ ){
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_ZSL,i);
            testSettingIcon(cameraId,mode);
            //snapByLocation();
            if(!testResult) {
                flashInZslResult = false;
                longshotInZslResult = false;
                break;
            }
            checkZSL();
            if(!testResult) {
                flashInZslResult = false;
                longshotInZslResult = false;
                break;
            }
            testFlash(cameraId,mode,false);
            flashInZslResult &= testResult;
            testLongShot(cameraId,mode);
            longshotInZslResult &= testResult;
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_ZSL,defvalue);
        snapByLocation();
        checkZSL();
        if(flashInZslResult){
            flashvalue = "PASS";
        }
        if(longshotInZslResult){
            longshotvalue = "PASS";
        }
        mActivity.setDevOption(false);
        updateAndSavejson(OUTPUT_JSON, "testFlashInZSL", parentNm,flashvalue);
        updateAndSavejson(OUTPUT_JSON, "testLongShotInZSL", parentNm,longshotvalue);
    }
    public void testMFNR(String cameraId,CaptureModule.CameraMode mode)throws Exception {
        updateJson(5,null);
        mActivity.setDevOption(true);
        int length =  mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_CAPTURE_MFNR_VALUE).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
        for(int i = 0;i <length; i++ ){
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_CAPTURE_MFNR_VALUE,i);
            testSettingIcon(cameraId,mode);
            checkMFNR();
            if(!testResult) {
                break;
            }
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_CAPTURE_MFNR_VALUE,defvalue);
        mActivity.setDevOption(false);
        if(testResult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }
private void clickShutterButton(CaptureModule.CameraMode mode)throws Exception{
    Thread.sleep(OPEN_CAMERA_DURATION);
    if(mode == CaptureModule.CameraMode.VIDEO || mode == CaptureModule.CameraMode.HFR){
        testVideo(mode);
    }else {
        snapByLocation();
    }

}
    public void testZoomBar(CaptureModule.CameraMode mode) throws Exception {
        boolean checkloc1 = false;
        boolean checkloc2 = false;
        boolean checkloc3 = false;
        updateJson(5, null);
        mOldZoomstr = mActivity.getCaptureModule().getZoomValue();
        mOldZoomRegion = mCurrentPreviewResult.get(CaptureResult.SCALER_CROP_REGION);
        if (mode == CaptureModule.CameraMode.PRO_MODE || mode == CaptureModule.CameraMode.CINEMATIC) {
            mSupported = false;
            View mZoomBar = mActivity.findViewById(R.id.zoom_seekbar);
            if (mZoomBar.getVisibility() == View.VISIBLE) {
/*                testResult = false;
                Log.e(TAG, "TestFail reason:Don't show zoom bar in proMode");*/
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
            if (mode != CaptureModule.CameraMode.RTB) {
                executeShellCommand("input tap " + mZoomValueLoc[0] + " " + mZoomValueLoc[1]);
                clickShutterButton(mode);
                checkZoomValue(mode);
                checkloc1 = testResult;

              /*  if (!testResult) return;*/
                if(isOpenFromIntent){
                    pressDone();
                    boolean checkintent1 = testResult;
                    if(isVideoMode(mode)) {
                        openCameraByIntent(mVideoIntent);
                    }else{
                        openCameraByIntent(mImageIntent);
                    }
                    checkPreview("0",mode);
                    executeShellCommand("input tap " + mZoomBarWidth + " " + mZoomBarLoc[1]);
                    clickShutterButton(mode);
                    checkZoomValue(mode);
                    pressDone();
                    boolean checkintent2 = testResult;
                    if (checkintent1 && checkintent2) updateJson(5, testPass);
                    else{
                        updateJson(5, testFail);
                    }
                    return;
                }
                executeShellCommand("input tap " + mZoomValueLoc[0] + " " + mZoomValueLoc[1]);
                clickShutterButton(mode);
                checkZoomValue(mode);
                checkloc2 = testResult;
                /*if (!testResult) return;*/
                executeShellCommand("input tap " + mZoomValueLoc[0] + " " + mZoomValueLoc[1]);
                clickShutterButton(mode);
                checkZoomValue(mode);
                checkloc3 = testResult;
                /*if (!testResult) return;*/
            }
            executeShellCommand("input tap " + mZoomBarLoc[0] + 5 + " " + mZoomBarLoc[1]);
            clickShutterButton(mode);
            checkZoomValue(mode);
            boolean checkbar1 = testResult;
           /* if (!testResult) return;*/
            executeShellCommand("input tap " + mZoomBarWidth / 3 + " " + mZoomBarLoc[1]);
            clickShutterButton(mode);
            checkZoomValue(mode);
            boolean checkbar2 = testResult;
            /*if (!testResult) return;*//**/
            executeShellCommand("input tap " + mZoomBarWidth + " " + mZoomBarLoc[1]);
            clickShutterButton(mode);
            checkZoomValue(mode);
            boolean checkbar3 = testResult;

            if(mode != CaptureModule.CameraMode.RTB){
                if(checkloc1 && checkloc2 && checkloc3 && checkbar1 && checkbar2 && checkbar3){
                    updateJson(5, testPass);
                }else{
                    updateJson(5, testFail);
                }

            }else{
                if (checkbar1 && checkbar2 && checkbar3) {
                    updateJson(5, testPass);
                }
            }
        }
    }
    public void testCamID(CaptureModule.CameraMode mode) throws Exception {
        updateJson(5,null);
        mActivity.setDevOption(true);
        executeShellCommand("setprop persist.sys.camera.devoption.debug 100");
        boolean checkresult = true;
        int length =  mActivity.mSettingsManager.getEntryValues(SettingsManager.KEY_SWITCH_CAMERA).length;
        int defvalue = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_SWITCH_CAMERA);
        for(int i = 0;i <length; i++ ){
            mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_SWITCH_CAMERA,i);
            Thread.sleep(OPEN_CAMERA_DURATION);
            String value = mActivity.mSettingsManager.getValue(SettingsManager.KEY_SWITCH_CAMERA);
            testSettingIcon(value,mode);
           // checkPreview(value,mode);
            if(mode == CaptureModule.CameraMode.DEFAULT) {
                snapByLocation();
            }else if(mode == CaptureModule.CameraMode.VIDEO){
                testVideo(mode);
            }
            if(!testResult){
                checkresult = testResult;
            }
        }
        mActivity.mSettingsManager.setValueIndex(SettingsManager.KEY_SWITCH_CAMERA,defvalue);
        checkPreview(mActivity.mSettingsManager.getValue(SettingsManager.KEY_SWITCH_CAMERA),mode);
        mActivity.setDevOption(false);
        executeShellCommand("setprop persist.sys.camera.devoption.debug 0");
        if(testResult && checkresult) updateJson(5,testPass);
        else{
            updateJson(5,testFail);
        }
    }

    public void testEV()throws Exception {
        updateJson(5,null);
        pressEVtext();
        //executeShellCommand("input tap "+ mEVLoc[0]  +" "+mEVLoc[1]);
        int defidx = mActivity.mSettingsManager.getValueIndex(SettingsManager.KEY_EXPOSURE);
        for(int i = 0;i < evvalue.length ;i++){
            mProMode.setIndex(i,true);
            snapByLocation();
            checkEV(String.valueOf(evvalue[i]));
            if(!testResult)break;
        }
        mProMode.setIndex(defidx,true);
        if(testResult)updateJson(5,testPass);
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
        if (!setvalue.equals(valueInSet) || valueInResult != longexif ||
                diffvalue > mProMode.mLongExpTm || diffvalue < -mProMode.mLongExpTm) {
            testResult = false;
            Log.e(TAG, "TestFail reason:checkShutterSpeed setvalue =" + setvalue + ",valueInSet=" + valueInSet
                    + ",valueInExif=" + valueInExif + ",valueinresult=" + valueInResult + ",doubleexif=" + doubleexif +
                    ",longexif=" + longexif + ",diff=" + diffvalue + ",longvalue=" + longvalue);
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
        if (setvalue != valueInSet || CaptureResult.CONTROL_AF_MODE_OFF != mControlAFMode ||
                valuediff > 0.2 || valuediff < -0.2) {
            testResult = false;
            Log.e(TAG, "TestFail reason:valueSet =" + valueSet + ",valueInResult=" + valueInResult
                    + ",mControlAFMode=" + mControlAFMode + ",setvalue=" + setvalue + ",valueInSet" + ",valueInExif=" + valueInExif + ",valuediff=" + valuediff);
            return;
        }
/*    if(valuediff > 0.2 ||( valuediff < -0.2)){
        throw new AssertionError("set focsdistance value is " +
                valueSet+",but value in result is "+ valueInResult);
    }*/
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
        checkEV("0");
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
            testResult = false;
            testFail =getFailStr("KEY_ISO",valueinset,setvalue);
            return;
        }
        if(!isauto) {
           // assertEquals(isoInResult,Integer.parseInt(valueInExif));
            int isoset = Integer.parseInt(setvalue);
            int isodiff = isoset - isoInResult;
            if(isoInResult != Integer.parseInt(valueInExif)){
      /*          testResult = false;
                Log.e(TAG,"TestFail reason: isovalue="+isovalue+",valueInExif="+valueInExif+",isoInResult="+isoInResult
                        +",isauto="+isauto+",isodiff="+isodiff);*/
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
/*            testResult = false;
            Log.e(TAG,"TestFail reason: wbInSet="+wbInSet+",valueinset="+valueinset+",setvalue="+setvalue
                    +",wbInResult="+wbInResult+",valueInExif="+valueInExif);*/
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
/*                testResult = false;
                Log.e(TAG,"TestFail reason:exif value is "+valueInExif);*/
                testFail = getFailStr("ExifInterface.TAG_WHITE_BALANCE",valueInExif,"0");
                return;
            }
           // assertEquals("0",valueInExif);
        }else{
            //assertEquals("1",valueInExif);
            if(!valueInExif.equals("1")){
/*                testResult = false;
                Log.e(TAG,"TestFail reason:exif value is "+valueInExif);*/
                testFail = getFailStr("ExifInterface.TAG_WHITE_BALANCE",valueInExif,"1");
                return;
            }
        }
    }

    private void checkEV(String indexvalue){
        if(!testResult) return;
        String value = mActivity.mSettingsManager.getValue(SettingsManager.KEY_EXPOSURE);
        if(value == null){
/*            testResult = false;
            Log.e(TAG,"TestFail reason:KEY_EXPOSURE is null");*/
            testFail = getFailStr("KEY_EXPOSURE",value,"NotNull");
            return;
        }
       // assertNotNull(value);
        int aeInSet = Integer.parseInt(value);
        int aeInResult = mActivity.getCaptureModule().getCaptureResult().get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
        if(!indexvalue.equals(value)){
/*            testResult = false;
            Log.e(TAG,"TestFail reason:value in set is "+aeInSet+",aeInResult is "+aeInResult
            +",indexvalue="+indexvalue+",value="+value);*/
            testFail = getFailStr("KEY_EXPOSURE",value,indexvalue);
            return;
        }
        if( aeInSet != aeInResult){
            testFail = getFailStr("CONTROL_AE_EXPOSURE_COMPENSATION",aeInResult,aeInSet);
            return;
        }
        //assertEquals(indexvalue,value);
       // assertEquals(aeInSet,aeInResult);
    }

    public void snapByLocation(CaptureModule.CameraMode mode) throws Exception{
        resetCapture();
        if(mode != CaptureModule.CameraMode.PRO_MODE && !mCaptureModule.mIsRecordingVideo) {
            snapByLocation(SNAPSHOT_NORMAL_DURATION, NORMAL_IMG);
        }else{
            snapByButton();
        }
    }
    public void snapByLocation() throws Exception{
        resetCapture();
        if(mCaptureModule.getCurrenCameraMode() != CaptureModule.CameraMode.PRO_MODE
                && !mCaptureModule.mIsRecordingVideo) {
            snapByLocation(SNAPSHOT_NORMAL_DURATION, NORMAL_IMG);
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
    public void snapByLocation(int time,String format) throws Exception{
        Thread.sleep(OPEN_CAMERA_DURATION);
        getPicSize();
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
        testVideo(mode,false);
    }
    public void testVideo(CaptureModule.CameraMode mode,boolean pressdone)throws Exception{
        updateJson(5,null);
        resetVideo();
        executeShellCommand("input tap "+ mVideoLoc[0] +" "+mVideoLoc[1]);
        long startime = System.currentTimeMillis();
        Thread.sleep(VIDEO_DURATION);
        if(isPerformenceTest){
            HashMap<String,Long> startVideo = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("startVideo",startVideo);
        }
        if(mode == CaptureModule.CameraMode.VIDEO && !isOpenFromIntent) {
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


    private void snapByKeyCode()throws Exception{
        //mCurrentImgNum = getCameraDirectoryJpegAmount();
        Thread.sleep(OPEN_CAMERA_DURATION);
        getPicSize();
        InstrumentationRegistry.getInstrumentation().sendCharacterSync(KeyEvent.KEYCODE_CAMERA);
        Thread.sleep(SNAPSHOT_NORMAL_DURATION);
        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        assertNotNull(patharry);
        assertEquals(1,patharry.size());
        String path =mNormalPath + patharry.get(0) + NORMAL_IMG;
        checkSnapShot(path);
    }
    private void snapShotCheck()throws Exception{
        mCurrentCaptureResult = mCaptureModule.getCaptureResult();
        mCurrentPreviewResult = mCaptureModule.getPreviewCaptureResult();
        if( mCurrentPreviewResult == null){
            testFail = getFailStr("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
            return;
        }
        if(mCurrentCaptureResult == null ){
            testResult = false;
            testFail = getFailStr("mCurrentCaptureResult",mCurrentCaptureResult,"NotNull");
        }
        List<String> patharry = mActivity.getCaptureModule().getLongImageTitle();
        if(patharry == null && !isOpenFromIntent){
            testResult = false;
            return;
        }
        if(patharry.size() != 1 && !isOpenFromIntent){
            testFail = getFailStr("image_num",patharry.size(),1);
            return;
        }
        String path =mNormalPath + patharry.get(0) + NORMAL_IMG;
        checkSnapShot(path);
    }
    private void snapByButton()throws Exception{
        //mCurrentImgNum = getCameraDirectoryJpegAmount();
        Thread.sleep(OPEN_CAMERA_DURATION);
        if(!mCaptureModule.mIsRecordingVideo) {
            getPicSize();
        }else{
            getVideoSize();
        }
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
/*            testResult = false;
            Log.e(TAG,"TestFail reason:activity.isFinishing()=" +mActivity.isFinishing());*/
            testFail = getFailStr("mActivity.isFinishing()",mActivity.isFinishing(),true);
            return false;
        }
        Intent resultData = mActivity.getResultData();
        if(resultData == null){
/*            testResult = false;
            Log.e(TAG,"TestFail reason:resultData=" +resultData);*/
            testFail = getFailStr("getResultData",resultData,"NotNull");
            return false;
        }

        if(isVideo) {
            mUri = resultData.getData();
            if(mUri == null){
                testResult = false;
                Log.e(TAG,"TestFail reason:mUri=" +mUri);
                testFail = getFailStr("mUri",mUri,"NotNull");
                return false;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mActivity, mUri);
            String duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null) {
/*                testResult = false;
                Log.e(TAG, "TestFail reason:duration=" + duration);*/
                testFail = getFailStr("METADATA_KEY_DURATION",duration,"NotNull");
                return false;
            }
            //assertNotNull(duration);
            int durationValue = Integer.parseInt(duration);
            if (durationValue <= 0) {
               // Log.e(TAG, "TestFail reason:durationValue=" + durationValue);
                testFail = getFailStr("METADATA_KEY_DURATION",durationValue,"above 0");
                return false;
            }
        }else{
            Bundle bundle = resultData.getExtras();
            if (bundle == null) {
                //Log.e(TAG, "TestFail reason: bundle=" + bundle.toString());
                testFail = getFailStr("resultData.getExtras()",bundle,"NotNull");
                return  false;
            }
            Bitmap bitmap = (Bitmap) bundle.getParcelable("data");
            if(bitmap == null ){
                //Log.e(TAG, "TestFail reason:bitmap=" + bitmap);
                testFail = getFailStr("bitmap",bitmap,"NotNull");
                return false;
            }
            if(bitmap.getWidth() <=0 ){
                /*Log.e(TAG, "TestFail reason:bitmap.getWidth()=" + bitmap.getWidth()
                +",bitmap.getHeight()="+bitmap.getHeight());*/
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
        mActivity.setAutoTest(true);
        executeShellCommand("input tap 500 500");
        mActivity = mActivityRule.getActivity();
        mCaptureModule = mActivity.getCaptureModule();
    }
    public void initsetting(){
        View mShutter = mActivity.findViewById(R.id.shutter_button);
        View mFlash = mActivity.findViewById(R.id.flash_button);
        View mHdr = mActivity.findViewById(R.id.scene_mode_hdr);
        View mZoomBar = mActivity.findViewById(R.id.zoom_seekbar);
        View mZoomValue = mActivity.findViewById(R.id.zoom_value_text);
        View mSwitch = mActivity.findViewById(R.id.front_back_switcher);
        View mVideoShutter = mActivity.findViewById(R.id.video_button);

        View mModeLayout = mActivity.findViewById(R.id.mode_select_layout);
       // View mModeItem = mActivity.findViewById(R.id.camera2_mode_item);
        View mSettingsButton = mActivity.findViewById(R.id.settings);


        mZoomBarWidth = mZoomBar.getWidth();
        mZoomValueWidth = mZoomValue.getWidth();
        mShutter.getLocationInWindow(mShutterLoc);
        mFlash.getLocationInWindow(mFlashLoc);
        mHdr.getLocationInWindow(mHdrLoc);
        mZoomBar.getLocationInWindow(mZoomBarLoc);
        mZoomValue.getLocationInWindow(mZoomValueLoc);
        mSwitch.getLocationInWindow(mSwitchLoc);
        mVideoShutter.getLocationInWindow(mVideoLoc);
        mSettingsButton.getLocationInWindow(mSettingLoc);


        mModeLayout.getLocationInWindow(mModeLoc);
       // mModeItem.getLocationInWindow(mModeItemLoc);

        DisplayMetrics metrics = CameraUtil.metrics;
        int value = metrics.widthPixels < metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;
        swipevalue = value / 2 ;
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
    public void getIconLoctionInPro(){
        View mEVtext = mActivity.findViewById(R.id.exposure_text);
        View mFocustext = mActivity.findViewById(R.id.focusdistance_text);
        View mShuttertext = mActivity.findViewById(R.id.shutterspeed_text);
        View mWBtext = mActivity.findViewById(R.id.whitebalance_text);
        View mISOtext = mActivity.findViewById(R.id.iso_text);
        mEVtext.getLocationInWindow(mEVLoc);
        mFocustext.getLocationInWindow(mFocDisLoc);
        mShuttertext.getLocationInWindow(mShutterLoc);
        mWBtext.getLocationInWindow(mWBLoc);
        mISOtext.getLocationInWindow(mISOLoc);
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
        Size snapsize = mCaptureModule.getVideoSnapSize();
        mPicWidInSet = String.valueOf(snapsize.getWidth());
        mPicHeiInSet = String.valueOf(snapsize.getHeight());
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
            jsonChildNm = stack[level - 2].getMethodName();
            jsonParentNm = stack[level].getMethodName();
            if(jsonParentNm.length() > 6 && jsonParentNm.substring(0,6).equals("testIn")) {
                if(value == null) {
                    value = "FAIL";
                    mSupported = true;
                    testFail = "FAIL";
                    testResult = true;
                }
                if(!mSupported){
                    value = value+"(INVISIBLE)";
                }
                updateAndSavejson(OUTPUT_JSON, jsonChildNm, jsonParentNm,value);
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
        mProMode = mCaptureModule.getmCameraControls().getmProMode();
        executeShellCommand("input tap 500 500");
        Thread.sleep(OPEN_CAMERA_DURATION);
        if(mCurrentPreviewResult == null){
           testFail = getFailStr("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
        }
        //assertNotNull(mCurrentPreviewResult);
        if(mActivity.isFinishing()){
            testFail = getFailStr("Activity_Finshing",mActivity.isFinishing(),false);
        }
        //assertFalse(mActivity.isFinishing());
        if(mode != mCaptureModule.getCurrenCameraMode()){
            testFail = getFailStr("CurrenCameraMode",mCaptureModule.getCurrenCameraMode(),mode);
        }
       // assertEquals(mode,mCaptureModule.getCurrenCameraMode());
        String mainId = String.valueOf(mCaptureModule.getMainCameraId());
        if(!(id.equals(mainId))){
            testFail = getFailStr("getMainCameraId",mainId,id);
        }
        //assertEquals(id,mainId);
        //
        // assertEquals(id,mCurrentPreviewResult.getCameraId());
        String resid = mCurrentPreviewResult.getCameraId();
        if(!(id.equals(resid))){
            testFail = getFailStr("CurrentPreviewResult.getCameraId()",resid,id);
        }
        CameraCaptureSession currentSession = mCaptureModule.getCurrentSession(Integer.valueOf(id));
        //boolean sessionclose = mCaptureModule.isSessionClosed(Integer.valueOf(id))
        //assertEquals(false,mCaptureModule.isSessionClosed(Integer.valueOf(mainId)));
        if(currentSession == null){
            testFail = getFailStr("CaptureSession",currentSession,"NotNull");
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
            mCurrentexif = null;
        }
    }
    public void resetPreview()throws Exception{
        if(mCaptureModule != null) {
            mCaptureModule.setPreviewCaptureResult(null);
            mCaptureModule.setLongImageTitle(new ArrayList<>());
            mCaptureModule.resetHashMapTimes();
        }
    }
    public void resetVideo()throws Exception{
        if(mCaptureModule != null) {
            mCaptureModule.setVideFilePath(null);
            mCaptureModule.setLongImageTitle(new ArrayList<>());
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
        Log.i(TAG,"videopath="+videopath+",mIsRecordingVideo="+mCaptureModule.mIsRecordingVideo+
                ",mCurrentPreviewResult=" +mCurrentPreviewResult+"Current videosize is "
                +mVideoWidInSet+"*"+mVideoHeiInSet+",frameRate in setting is "
                +mFrameRate);
/*        if(isRecording || mCurrentPreviewResult == null ||
                videopath == null){
            testResult = false;
            Log.e(TAG,"TestFail reason:mIsRecordingVideo="+mCaptureModule.mIsRecordingVideo+
                    ",mCurrentPreviewResult=" +mCurrentPreviewResult+ ",videopath="+videopath);
            return;
        }*/
        if(isRecording){
            testResult = false;
            //reportErrorInfo("IsRecordingVideo",isRecording,"false");
            testFail = getFailStr("IsRecordingVideo",isRecording,"false");
            return;
        }
        if(mCurrentPreviewResult == null){
            testResult = false;
            //reportErrorInfo("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
            testFail = getFailStr("CurrentPreviewResult",mCurrentPreviewResult,"NotNull");
            return;
        }
        if(videopath == null){
            testResult = false;
            //reportErrorInfo("VideoFilePath",videopath,"NotNull");
            testFail = getFailStr("VideoFilePath",videopath,"NotNull");
            return;
        }
        File f = new File(videopath);
        mFrameRate = mActivity.mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if(!f.exists()){
            testResult = false;
            //reportErrorInfo("VideoFilePath.exit",f.exists(),"True");
            testFail = getFailStr("VideoFilePath.exit",f.exists(),"True");
            return;
        }
        if(f.length() < VIDEO_LENGTH){
            testResult = false;
            //reportErrorInfo("VideoFilePath.length",f.length(),"above "+ VIDEO_LENGTH);
            testFail = getFailStr("VideoFilePath.length",f.length(),"above "+ VIDEO_LENGTH);
            return;
        }

/*        if(!f.exists() || f.length() < VIDEO_LENGTH){
            testResult = false;
            Log.e(TAG,"TestFail reason:videopath is  f.exists()="+f.exists()+
                    ",f.length()="+f.length()+",mFrameRate="+mFrameRate);
            return;
        }*/
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
            testResult = false;
           // reportErrorInfo("VideoSize.getWidth",CameraUtil.mWidth,mVideoWidInSet);
            testFail = getFailStr("VideoSize(width*height)",CameraUtil.mWidth+"*"+
                    CameraUtil.mHeight,mVideoWidInSet+"*"+mVideoHeiInSet);
            return;
        }
        if( checkfps && (fpsdiff >5 || fpsdiff <-5)){
            testResult = false;
            testFail = getFailStr("video FrameRate",fpsInVideo,fps);
            return;
        }
/*        if(timediff >2000 || timediff <-2000){
            testResult = false;
            reportErrorInfo("timeInMillisec(diff(time in video compare with recording time) " +
                    "should < 2000)",CameraUtil.timeInMillisec,time);
            return;
        }*/
/*        if(!mVideoWidInSet.equals(CameraUtil.mWidth) || !mVideoHeiInSet.equals(CameraUtil.mHeight) ||
                fpsdiff >5 || fpsdiff <-5 || timediff >2000 || timediff <-2000 ){
            testResult = false;
            Log.e(TAG,"TestFail reason:time="+time+",mVideoWidInSet="+mVideoWidInSet+",mVideoHeiInSet="+mVideoHeiInSet
                    +",timediff shoud <2s,timediff="+timediff+",fpsdiff shoud <5, fpsdiff="+fpsdiff+",mFrameRate="+mFrameRate);
            return;
        }*/
       /* if(fpsdiff >5 || fpsdiff <-5){
            throw new AssertionError("video fps should be  " + fps +
                    ",but  mFrameRate is  "+CameraUtil.mFrameRate+",mfps is "+ CameraUtil.mFps);
        }
        if(timediff >1000 || timediff <-1000){
            throw new AssertionError("duration should be  " + time +
                    ",but  duration in the file is  "+CameraUtil.timeInMillisec);
        }*/
/*        if(showdiff >3000 || showdiff <-3000){
            throw new AssertionError("show time is  " + timeshow +
                    ",but  duration in the file is  "+TestUtil.mDuration);
        }*/

    }
    public void checkSnapShot(String path) throws Exception {
        if(!testResult) return;
        if(!mCaptureModule.getCaptureUI().isShutterEnabled()){
            testResult = false;
            //Log.e(TAG,"TestFail reason:Shutter button is not enable");
            testFail = getFailStr("isShutterEnabled",mCaptureModule.getCaptureUI().isShutterEnabled(),true);
            return;
        }
        //assertTrue(mCaptureModule.getCaptureUI().isShutterEnabled());
/*        mCurrentImgNum = getCameraDirectoryJpegAmount();
        assertEquals(preJpgNum,mCurrentImgNum - picnum);*/
        File f = new File(path);
        //assertTrue(f.exists());
        //assertTrue(f.length() > 1024);
        //assertTrue(f.length() > 1024);
        if(isOpenFromIntent){
            if(f.exists()){
                testResult = false;
                //Log.e(TAG,"TestFail reason:isOpenFromIntent,snapshot f.exists()="+f.exists() +
                //        " f.length()="+f.length());
                testFail = getFailStr("imagepath.exit()",f.exists(),false);
            }
            return;
        }
        if(!f.exists()){
            testResult = false;
            testFail = getFailStr("imagepath.exit()",f.exists(),true);
            return;
        }
        if(f.length() < 1024){
            testResult = false;
            testFail = getFailStr("imagepath.length()",f.length(),"above 1024");
            return;
        }
    mCurrentexif = new ExifInterface(path);
        if(mCurrentexif == null ){
            testResult = false;
            //Log.e(TAG,"TestFail reason:mCurrentexif="+mCurrentexif);
            testFail = getFailStr("imagepath.exif",mCurrentexif,"NotNull");
            return;
        }
       // assertNotNull(mCurrentexif);
        String wInExif= mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String hInExif= mCurrentexif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
        if(!mPicWidInSet.equals(wInExif) || !mPicHeiInSet.equals(hInExif)){
            testResult = false;
/*            Log.e(TAG,"TestFail reason:picture size is wrong with exif info ,picturesize in set ="+mPicWidInSet
                    +"*"+mPicHeiInSet+",but size in exif is "+wInExif+"*"+hInExif);*/
            testFail = getFailStr("ImageSize in exif",wInExif+"*"+hInExif,
                    mPicWidInSet+"*"+mPicHeiInSet);
            return;
        }
        //assertEquals(mPicWidInSet,wInExif);
        //assertEquals(mPicHeiInSet,hInExif);

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
                testResult = false;
                //Log.e(TAG,"TestFail reason:mCurrentexif= "+mCurrentexif);
                testFail = getFailStr("Currentexif", mCurrentexif, "NotNull");
                return;
            }
            flashInResult = mCurrentCaptureResult.get(CaptureResult.FLASH_MODE);
            aeInResult = mCurrentCaptureResult.get(CaptureResult.CONTROL_AE_MODE);
            if (!isOpenFromIntent) {
                flashInExif = mCurrentexif.getAttributeInt(ExifInterface.TAG_FLASH, 0);
            }
        }
        int flashInPreview = mCurrentPreviewResult.get(CaptureResult.FLASH_MODE);
        int aeInPreview = mCurrentPreviewResult.get(CaptureResult.CONTROL_AE_MODE);
        boolean isTriggered = mCurrentPreviewResult.get(CaptureResult.CONTROL_AE_STATE) == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
        Log.i(TAG, "flashinset=" + flashinset + ",flashInResult=" + flashInResult + ",aeInResult=" + aeInResult + ",isTriggered=" + isTriggered
                + ",aeInPreview=" + aeInPreview + ",flashInPreview=" + flashInPreview + ",flashInExif=" + flashInExif);
        //assertEquals(setvalue,flashinset);
        if (!setvalue.equals(flashinset)) {
            testResult = false;
            //Log.e(TAG,"TestFail reason:setflash is "+setvalue+",but flash is setting is "+flashinset);
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
                        //testResult = false;
                        //Log.e(TAG, "TestFail reason:flashInResult is "+flashInResult+",aeInResult is "+aeInResult);
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_SINGLE);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH != aeInResult) {
                        testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    }
                    if (!isOpenFromIntent && flashInExif != FLASH_ON) {
/*                        testResult = false;
                        Log.e(TAG, "TestFail reason: flashInExif is "+flashInExif);*/
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_ON);
                        return;
                    }
                } else {
                    if (CaptureResult.FLASH_MODE_TORCH != flashInPreview) {
/*                        testResult = false;
                        Log.e(TAG, "TestFail reason:flashInPreview is "+flashInPreview+",aeInPreview is "+aeInPreview);*/
                        testFail = getFailStr("FLASH_MODE in preview", flashInPreview, CaptureResult.FLASH_MODE_TORCH);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON != aeInPreview) {
                        testFail = getFailStr("CONTROL_AE_MODE in preview", aeInPreview, CaptureResult.CONTROL_AE_MODE_ON);
                    }
                    //assertEquals(CaptureResult.FLASH_MODE_TORCH, flashInPreview);
                    // assertEquals(CaptureResult.CONTROL_AE_MODE_ON, aeInPreview);
                    if (isSupportSnapShot(mode)) {
                        // assertEquals(CaptureResult.FLASH_MODE_TORCH, flashInResult);
                        // assertEquals(CaptureResult.CONTROL_AE_MODE_ON, aeInResult);
                        //assertTrue((flashInExif & FLASH_ON) > 0);
                        if (CaptureResult.FLASH_MODE_TORCH != flashInResult) {
/*                            testResult = false;
                            Log.e(TAG, "TestFail reason:flashInResult is "+flashInResult+",aeInResult is "+aeInResult);*/
                            testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_TORCH);
                            return;
                        }
                        if (CaptureResult.CONTROL_AE_MODE_ON != aeInResult) {
                            testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON);
                        }
                        if (!isOpenFromIntent) {
                            if (flashInExif != FLASH_ON) {
/*                                testResult = false;
                                Log.e(TAG, "TestFail reason:flashInExif is "+flashInExif);*/
                                testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_ON);
                                return;
                            }
                        }
                    }
                }
                break;
            case "auto":
                //assertEquals(CaptureResult.FLASH_MODE_SINGLE,flashInResult);
                //assertEquals(CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,aeInResult);
                if (CaptureResult.FLASH_MODE_SINGLE != flashInResult) {
/*                    testResult = false;
                    Log.e(TAG,"TestFail reason:flashInResult is"+flashInResult+",aeInResult is"+aeInResult);*/
                    testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_SINGLE);
                    return;
                }
                if (CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH != aeInResult) {
                    testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    return;
                }
                if (isTriggered) {
                    // assertTrue((flashInExif & FLASH_ON) > 0);
                    if (flashInExif != FLASH_AUTO_ON && !isOpenFromIntent) {
/*                        testResult = false;
                        Log.e(TAG,"TestFail reason:flashInResult is"+flashInResult+",aeInResult is"+aeInResult+
                                ",flashInExif is "+flashInExif);*/
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_AUTO_ON);
                        return;
                    }
                } else {
                    // assertTrue((flashInExif & FLASH_OFF) > 0);
                    if (flashInExif != FLASH_AUTO_OFF && !isOpenFromIntent) {
/*                        testResult = false;
                        Log.e(TAG,"TestFail reason:flashInResult is"+flashInResult+",aeInResult is"+aeInResult+
                                ",flashInExif is "+flashInExif);*/
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_AUTO_OFF);
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
/*                        testResult = false;
                        Log.e(TAG,"TestFail reason:flashInResult is"+flashInResult+",aeInResult is"+aeInResult);*/
                        testFail = getFailStr("FLASH_MODE in reslut", flashInResult, CaptureResult.FLASH_MODE_OFF);
                        return;
                    }
                    if (CaptureResult.CONTROL_AE_MODE_ON != aeInResult) {
                        testFail = getFailStr("CONTROL_AE_MODE in reslut", aeInResult, CaptureResult.CONTROL_AE_MODE_ON);
                        return;
                    }
                    if (!isOpenFromIntent && flashInExif != FLASH_OFF) {
                        testResult = false;
                        //Log.e(TAG, "TestFail reason: flashInExif is "+flashInExif);
                        testFail = getFailStr("ExifInterface.TAG_FLASH", flashInExif, FLASH_OFF);
                        return;
                    }
                }
                //assertEquals(CaptureResult.FLASH_MODE_OFF,flashInPreview);
                //assertEquals(CaptureResult.CONTROL_AE_MODE_ON,aeInPreview);
                if (CaptureResult.FLASH_MODE_OFF != flashInPreview) {
                       /* testResult = false;
                        Log.e(TAG, "TestFail reason:flashInPreview is "+flashInPreview+",aeInPreview is "+aeInPreview);*/
                    testFail = getFailStr("FLASH_MODE in preview", flashInPreview, CaptureResult.FLASH_MODE_OFF);
                    return;
                }
                if (CaptureResult.CONTROL_AE_MODE_ON != aeInPreview) {
                    testFail = getFailStr("CONTROL_AE_MODE in preview", aeInPreview, CaptureResult.CONTROL_AE_MODE_ON);
                    return;
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
/*            Log.e(TAG,"TestFail reason:value is not equals with the value in setting," +
                    "value is "+value+",value in setting is "+setValue);*/
            testResult = false;
            testFail = getFailStr("KEY_SCENE_MODE",setValue,value);
            return;
        }
        if(value == HDR_SCENE_OFF){
            value = CONTROL_MODE_AUTO;
        }
        int sceneMode = mCurrentCaptureResult.get(CaptureResult.CONTROL_SCENE_MODE);
        if(value != sceneMode){
            testResult =false;
/*            Log.e(TAG,"TestFail reason:value is not equals with the value in result," +
                    "value is "+value+",value in result is "+sceneMode);*/
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
            testResult = false;
            //reportErrorInfo("noiseInResult",noiseInResult,noiseReduMode);
            testFail = getFailStr("NOISE_REDUCTION_MODE",noiseInResult,noiseReduMode);
            return;
        }
       // assertEquals(noiseReduMode,noiseInResult);
    }
    public void checkZoomValue(CaptureModule.CameraMode mode)throws Exception{//HardSwitch with cameraid changed need hal support
        if(!testResult) return;
        float zoomInPre = mCurrentPreviewResult.get(CaptureResult.CONTROL_ZOOM_RATIO );
        float zoomStr = mActivity.getCaptureModule().getZoomValue();
        if(zoomStr == mOldZoomstr){
            testResult = false;
          /*  Log.e(TAG,"TestFail reason:zoom is not changed,current zoom is "
                    +zoomStr+",the before zoom is "+mOldZoomstr);*/
            testFail = getFailStr("zoom should be changed","currentzoom == beforezoom","different");
            return;
        }
        mOldZoomstr = zoomStr;
       // assertNotEquals(zoomStr,mOldZoomstr);
        if(mode != CaptureModule.CameraMode.HFR && !isOpenFromIntent){
            float zoomInCap = mCurrentCaptureResult.get(CaptureResult.CONTROL_ZOOM_RATIO);
            if(zoomStr != zoomInCap ){
                testResult = false;
               /* Log.e(TAG,"TestFail reason:zoom value is wrong,zoomstr is "+zoomStr+
                        ",zoom in preview is"+zoomInPre+",zoom in capture is "+zoomInCap);*/
                testFail = getFailStr("zoomInCapture:CONTROL_ZOOM_RATIO",zoomInCap,zoomStr);
                return;
            }
            if(zoomStr != zoomInPre){
                testResult = false;
                testFail = getFailStr("zoomInPreview:CONTROL_ZOOM_RATIO",zoomInPre,zoomStr);
                return;
            }
           // assertEquals(zoomStr,zoomInCap);
           // assertEquals(zoomStr,zoomInPre);
        }else{
            Rect mCropRegion = mCurrentPreviewResult.get(CaptureResult.SCALER_CROP_REGION);
            if(mOldZoomRegion == mCropRegion){
                testResult = false;
                Log.e(TAG,"TestFail reason:zoom value is not changed ,oldregion  is "+mOldZoomRegion+
                        ",new region is"+mCropRegion);
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
           testResult = false;
           //Log.e(TAG,"TestFail reason:zsl in setting is "+zslset+",but zsl in capturereslut is "+zslresult);
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
            Thread.sleep(SMALL_WAIT_DURATION);
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
}