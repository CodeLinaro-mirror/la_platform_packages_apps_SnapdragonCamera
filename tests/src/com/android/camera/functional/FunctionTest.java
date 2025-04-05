/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.functional;
import  com.android.camera.TestBase;
import com.android.camera.CameraActivity;
import com.android.camera.SettingsManager;
import com.android.camera.CaptureModule;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CameraMetadata;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.test.ActivityInstrumentationTestCase2;

import android.test.suitebuilder.annotation.LargeTest;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.util.Log;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.runner.RunWith;


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
import android.support.test.InstrumentationRegistry;
import org.codeaurora.snapcam.R;
import android.view.KeyEvent;
import android.support.test.rule.ActivityTestRule;
import com.android.camera.util.CameraUtil;



@RunWith(AndroidJUnit4.class)
public class FunctionTest extends TestBase  {
    private String TAG = "autoTest_FunctionTest";
    private static final String testFile = "/data/data/org.codeaurora.snapcam/files/fuctionTestParam.txt";

    @BeforeClass
    public static void initJson(){
        Log.i("autotest_initJson","initJson beforeTest");
        init();
        updateAndSavejson(INPUT_JSON,null,null,null);
        String testStr = CameraUtil.readFile(testFile);
        Log.i("autotest_initJson","initJson testStr="+testStr);
        getTestItem(testStr);
    }
    @AfterClass
    public static void resetTestItem(){
        Log.i("autotest_initJson","resetTestItem AfterTest");
        functionTestMode = null;
        functionTestItem = null;
        functionTestItemDel = null;
    }

    @Before
    public void beforeEachTest()throws Exception {
        Log.i(TAG, "beforeEachTest");
        OpenAndResetCamera();
    }
    @After
    public void afterEachTest() throws Exception{
        Log.i(TAG, "AfterClass");
        mCaptureModule.setLongImageTitle(null);
        mCaptureModule.setImagExif(null);
        mCaptureModule.setImgType(null);
        isOpenFromIntent = false;
        mActivityRule.finishActivity();

    }
    @Test
    public void testInPhoto() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInPhoto"))
               ||(functionTestModeDel != null && functionTestModeDel.contains("testInPhoto"))){
            return;
        }
        Log.i(TAG, "testInPhotoModule");
        //checkPreview("0", CaptureModule.CameraMode.DEFAULT);
        runPhotoCase("0",CaptureModule.CameraMode.DEFAULT);
    }
    @Test
    public void testInFrontPhoto() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInFrontPhoto"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInFrontPhoto"))){
            return;
        }
        Log.i(TAG, "testInPhotoModule");
        executeShellCommand("input tap "+ mSwitchLoc[0]  +" "+mSwitchLoc[1]);
        runPhotoCase("1",CaptureModule.CameraMode.DEFAULT);
    }
    @Test
    public void testInBokeh() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInBokeh"))
              ||(functionTestModeDel != null && functionTestModeDel.contains("testInBokeh"))){
            return;
        }
        int[] loc = mModeIconL.get("Bokeh");
        if(loc == null){
            Log.e(TAG,"Cannot find Bokeh mode");
            return;
        }
        Log.i(TAG, "testInBokeh loc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        // checkPreview("0",CaptureModule.CameraMode.RTB);
        runPhotoCase("0",CaptureModule.CameraMode.RTB);
    }

    @Test
    public void testInPro() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInPro"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInPro"))){
            return;
        }
        Log.i(TAG, "testInProModule");

        int[] loc = mModeIconR.get("Pro");
        if(loc == null){
            loc = mModeIconL.get("Pro");
        }else{
            switchModeTextToR(true);
        }
        if(loc == null){
            Log.e(TAG,"Cannot find Pro mode");
            return;
        }
        Log.i(TAG, "testInProModule loc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        // checkPreview("0",CaptureModule.CameraMode.PRO_MODE);
        getIconLoctionInPro();
        getKeyValue();
        runPhotoCase("0",CaptureModule.CameraMode.PRO_MODE);
    }

    @Test
    public void testInVideoMode() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInVideoMode"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInVideoMode"))){
            return;
        }
        int[] loc = mModeIconL.get("Video");
        if(loc == null){
            Log.e(TAG,"Cannot find Video mode");
            return;
        }
        Log.i(TAG, "testInVideoModule mVideoModeLoc="+loc[0]+"*"+loc[1]);
        // swipFromRTL(3);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        //checkPreview("0",CaptureModule.CameraMode.VIDEO);
        runVideoCase("0",CaptureModule.CameraMode.VIDEO);
    }
    @Test
    public void testInFrontVideo() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInFrontVideo"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInFrontVideo"))){
            return;
        }
        int[] loc = mModeIconL.get("Video");
        if(loc == null){
            Log.e(TAG,"Cannot find Video mode");
            return;
        }
        Log.i(TAG, "testInFrontVideo mVideoModeLoc="+loc[0]+"*"+loc[1]);
        // swipFromRTL(3);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        checkPreview("0",CaptureModule.CameraMode.VIDEO);
        executeShellCommand("input tap "+ mSwitchLoc[0]  +" "+mSwitchLoc[1]);
        runVideoCase("1",CaptureModule.CameraMode.VIDEO);
    }
    @Test
    public void testInHFR() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInHFR"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInHFR"))){
            return;
        }
        int[] loc = mModeIconL.get("HFR");
        if(loc == null){
            Log.e(TAG,"Cannot find HFR mode");
            return;
        }
        Log.i(TAG, "testInHFR loc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        runVideoCase("2",CaptureModule.CameraMode.HFR);
    }
    @Test
    public void testInFrontHFR() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInFrontHFR"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInFrontHFR"))){
            return;
        }
        int[] loc = mModeIconL.get("HFR");
        if(loc == null){
            Log.e(TAG,"Cannot find HFR mode");
            return;
        }
        Log.i(TAG, "testInFrontHFR loc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        checkPreview("2",CaptureModule.CameraMode.HFR);
        executeShellCommand("input tap "+ mSwitchLoc[0]  +" "+mSwitchLoc[1]);
        runVideoCase("1",CaptureModule.CameraMode.HFR);
    }
    @Test
    public void testInCinema() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInCinema"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInCinema"))){
            return;
        }
        int[] loc = mModeIconL.get("Cinematic");
        if(loc == null){
            Log.e(TAG,"Cannot find Cinematic mode");
            return;
        }
        Log.i(TAG, "testInCinema loc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        //checkPreview("2",CaptureModule.CameraMode.HFR);
        runVideoCase("2",CaptureModule.CameraMode.CINEMATIC);
    }
    @Test
    public void testInDepth() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInDepth"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInDepth"))){
            return;
        }
        int[] loc = mModeIconR.get("Depth");
        if(loc == null){
            loc = mModeIconL.get("Depth");
        }else{
            switchModeTextToR(true);
        }
        if(loc == null){
            Log.i(TAG,"Cannot find this mode");
            return;
        }
        Log.i(TAG, "testInCinema mdepthLoc="+loc[0]+"*"+loc[1]);
        executeShellCommand("input tap "+ loc[0]  +" "+loc[1]);
        //checkPreview("2",CaptureModule.CameraMode.HFR);
        runPhotoCase("0",CaptureModule.CameraMode.DEPTH);
    }

    @Test
    public void testInVideoIntent() throws Exception {
        //setActivityIntent(mIntent);
        //recordVideo();
        if((functionTestMode != null && !functionTestMode.contains("testInVideoIntent"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInVideoIntent"))){
            return;
        }
        Log.i(TAG," testInVideoIntent");
        mActivityRule.finishActivity();
        Thread.sleep(OPEN_CAMERA_DURATION);
        openCameraByIntent(mVideoIntent);
        isOpenFromIntent = true;
        runVideoCase("0",CaptureModule.CameraMode.VIDEO);
    }
    @Test
    public void testInImageIntent() throws Exception {
        if((functionTestMode != null && !functionTestMode.contains("testInImageIntent"))
                ||(functionTestModeDel != null && functionTestModeDel.contains("testInImageIntent"))){
            return;
        }
        Log.i(TAG," testInImageIntent");
        //setActivityIntent(mIntent);
        //recordVideo();
        mActivityRule.finishActivity();
        Thread.sleep(OPEN_CAMERA_DURATION);

        openCameraByIntent(mImageIntent);
        isOpenFromIntent = true;
        runPhotoCase("0",CaptureModule.CameraMode.DEFAULT);
    }
}
