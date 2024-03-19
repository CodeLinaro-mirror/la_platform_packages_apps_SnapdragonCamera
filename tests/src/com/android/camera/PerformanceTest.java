/*
 * Copyright (C) 2023 The Android Open Source Project
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
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera;

import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.runner.RunWith;
import android.view.KeyEvent;
import com.android.camera.CameraActivity;
import com.android.camera.CaptureModule;
import android.support.test.rule.ActivityTestRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Log;

@RunWith(AndroidJUnit4.class)
public class PerformanceTest extends TestBase {
    private String TAG = "autoTest_PerformanceTest";
    private static final int CYCLE_TIMES = 11;
    private HashMap<Integer, List<Long> > mSnapShotTimes = new HashMap<>();
    HashMap<String, List<Long>> timeValues = new HashMap<>();
    private String[]coldOpenCamera = {"onCreate->openCamera","openCamera->onOpened","onOpened->createSession",
    "createSession->onConfigured","onConfigured->FirstRequest","FirstRequest->onCaptureCompleted","Total"};
    private String[]snapShot = {"onShutterButtonClick->capture","capture->onImageAvailable","Total"};
    private static final String PERFORMENCE_PHOTO = "/data/data/org.codeaurora.snapcam/files/performence_photo.json";
    private static final String PERFORMENCE_BOKEH = "/data/data/org.codeaurora.snapcam/files/performence_bokeh.json";
    private static final String PERFORMENCE_HFR = "/data/data/org.codeaurora.snapcam/files/performence_hfr.json";
    private static final String PERFORMENCE_VIDEO = "/data/data/org.codeaurora.snapcam/files/performence_video.json";
    private static final String PERFORMENCE_PRO = "/data/data/org.codeaurora.snapcam/files/performence_pro.json";
    private static final String PERFORMENCE_DEPTH = "/data/data/org.codeaurora.snapcam/files/performence_depth.json";
    private static final String PERFORMENCE_CINEMA = "/data/data/org.codeaurora.snapcam/files/performence_cinema.json";
    private static final String testFile = "/data/data/org.codeaurora.snapcam/files/PerformanceTestParam.txt";
    private static String performenceTestMode;


    @BeforeClass
    public static void initJson(){
        Log.i("autotest_initJson","initJson beforeTest");
        String testStr = CameraUtil.readFile(testFile);
        int indexM = testStr.indexOf("-m");
        if(indexM != -1) {
            performenceTestMode = getIndexStr(testStr, indexM + 2);
        }
        Log.i("autotest_initJson","initJson testStr="+testStr+",indexm="+indexM+",performenceTestMode="+performenceTestMode);
    }
    @AfterClass
    public static void resetTestItem(){
        Log.i("autotest_initJson","resetTestItem AfterTest");
        functionTestMode = null;
    }
    @Before
    public void beforeEachTest() throws Exception {
        Log.i(TAG, "beforeEachTest");
        init();
        OpenAndResetCamera();
        isPerformenceTest = true;
    }

    @After
    public void afterEachTest() throws Exception {
        Log.i(TAG, "afterEachTest");
        isPerformenceTest = false;
        performenceValues.clear();
        mCaptureModule.resetHashMapTimes();
        mActivityRule.finishActivity();
    }

    @Test
    public void testPhoto() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testPhoto")){
            return;
        }
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                mActivityRule.finishActivity();
                Thread.sleep(OPEN_CAMERA_DURATION);
                mActivityRule = new ActivityTestRule<>(CameraActivity.class);
                OpenCamera();
                continue;
            }

            resetPreview();
            mActivityRule.finishActivity();
            Thread.sleep(OPEN_CAMERA_DURATION);
            mActivityRule = new ActivityTestRule<>(CameraActivity.class);
            OpenCamera();
            performenceValues = new HashMap<String,HashMap<String,Long>>();

            HashMap<String,Long>  coldStartHash = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("coldStart",coldStartHash);
            testToggleBackFront(CaptureModule.CameraMode.DEFAULT);
            testFlash("0",CaptureModule.CameraMode.DEFAULT,true);
            updatePerformenceJson(PERFORMENCE_PHOTO,performenceValues,i);

        }
    }
    @Test
    public void testBokeh() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testBokeh")){
            return;
        }
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                swipFromLTR(1);
                checkPreview("0", CaptureModule.CameraMode.RTB);
                swipFromRTL(1);
                checkPreview("0", CaptureModule.CameraMode.DEFAULT);
                continue;
            }
            resetPreview();
            swipFromLTR(1);
            checkPreview("0",CaptureModule.CameraMode.RTB);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            HashMap<String,Long>  photoToBokeh = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("photoToBokeh",photoToBokeh);
            testFlash("0",CaptureModule.CameraMode.DEFAULT,true);
            swipFromRTL(1);
            checkPreview("0",CaptureModule.CameraMode.DEFAULT);
            HashMap<String,Long>  bokehToPhoto = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("bokehToPhoto",bokehToPhoto);
            updatePerformenceJson(PERFORMENCE_BOKEH,performenceValues,i);
        }
    }
    @Test
    public void testHFR() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testHFR")){
            return;
        }
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                swipFromRTL(1);
                checkPreview("2", CaptureModule.CameraMode.HFR);
                swipFromLTR(1);
                checkPreview("0", CaptureModule.CameraMode.DEFAULT);
                continue;
            }
            resetPreview();
            swipFromRTL(1);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            checkPreview("2",CaptureModule.CameraMode.HFR);
            HashMap<String,Long> photoToHFR = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("photoToHFR",photoToHFR);
            testToggleBackFront(CaptureModule.CameraMode.HFR);
            testVideo(CaptureModule.CameraMode.HFR);
            swipFromLTR(1);
            checkPreview("0",CaptureModule.CameraMode.DEFAULT);
            HashMap<String,Long> hfrToPhoto = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("hfrToPhoto",hfrToPhoto);
            updatePerformenceJson(PERFORMENCE_HFR,performenceValues,i);
        }
    }
    @Test
    public void testVideo() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testVideo")){
            return;
        }
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                swipFromRTL(3);
                checkPreview("0", CaptureModule.CameraMode.VIDEO);
                swipFromLTR(1);
                checkPreview("2", CaptureModule.CameraMode.CINEMATIC);
                continue;
            }
            resetPreview();
            swipFromRTL(1);
            checkPreview("0",CaptureModule.CameraMode.VIDEO);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            HashMap<String,Long> cinemaToVideo = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("cinemaToVideo",cinemaToVideo);
            testToggleBackFront(CaptureModule.CameraMode.VIDEO);
            testVideo(CaptureModule.CameraMode.VIDEO);
            swipFromLTR(1);
            checkPreview("2",CaptureModule.CameraMode.CINEMATIC);
            HashMap<String,Long> videoToCinema = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("videoToCinema",videoToCinema);
            updatePerformenceJson(PERFORMENCE_VIDEO,performenceValues,i);
        }
    }
    @Test
    public void testCinema() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testCinema")){
            return;
        }
        int[] cinemaLoc = mModeIconR.get("Cinematic");
        int[] photoLoc = mModeIconL.get("Photo");
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                if(cinemaLoc == null){
                    cinemaLoc = mModeIconL.get("Cinematic");
                }else{
                    switchModeTextToR(true);
                }
                if(cinemaLoc == null){
                    return;
                }
                Log.i(TAG, "testCinema cinemaLoc="+cinemaLoc[0]+"*"+cinemaLoc[1]);
                executeShellCommand("input tap "+ cinemaLoc[0]  +" "+cinemaLoc[1]);
                checkPreview("2", CaptureModule.CameraMode.CINEMATIC);
                executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
                checkPreview("0", CaptureModule.CameraMode.DEFAULT);
                continue;
            }
            resetPreview();
            executeShellCommand("input tap "+ cinemaLoc[0]  +" "+cinemaLoc[1]);
            checkPreview("2", CaptureModule.CameraMode.CINEMATIC);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            HashMap<String,Long> photoToCinema = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("photoToCinema",photoToCinema);
            testVideo(CaptureModule.CameraMode.CINEMATIC);
            executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
            checkPreview("0", CaptureModule.CameraMode.DEFAULT);
            HashMap<String,Long> cinemaToPhoto = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("cinemaToPhoto",cinemaToPhoto);
            updatePerformenceJson(PERFORMENCE_CINEMA,performenceValues,i);
        }
    }
    @Test
    public void testDepth() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testDepth")){
            return;
        }
        int[] depthLoc = mModeIconR.get("Depth");
        int[] photoLoc = mModeIconL.get("Photo");
        boolean switchtext = false;
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                if(depthLoc == null){
                    depthLoc = mModeIconL.get("Depth");
                }else{
                    switchModeTextToR(true);
                    switchtext = true;
                }
                if(depthLoc == null){
                    Log.i(TAG,"Cannot find depth mode");
                    return;
                }
                Log.i(TAG, "testCinema depthLoc="+depthLoc[0]+"*"+depthLoc[1]);
                executeShellCommand("input tap "+ depthLoc[0]  +" "+depthLoc[1]);
                checkPreview("0", CaptureModule.CameraMode.DEPTH);
                if(switchtext) {
                    switchModeTextToR(false);
                }
                executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
                checkPreview("0", CaptureModule.CameraMode.DEFAULT);
                continue;
            }
            resetPreview();
            if(switchtext) {
                switchModeTextToR(true);
            }
            executeShellCommand("input tap "+ depthLoc[0]  +" "+depthLoc[1]);
            checkPreview("0", CaptureModule.CameraMode.DEPTH);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            HashMap<String,Long> photoToDepth = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("photoToDepth",photoToDepth);
            if(switchtext) {
                switchModeTextToR(false);
            }
            executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
            checkPreview("0", CaptureModule.CameraMode.DEFAULT);
            HashMap<String,Long> depthToPhoto = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("depthToPhoto",depthToPhoto);
            updatePerformenceJson(PERFORMENCE_DEPTH,performenceValues,i);
        }
    }
    @Test
    public void testPro() throws Exception {
        if(performenceTestMode != null && !performenceTestMode.contains("testPro")){
            return;
        }
        int[] proLoc = mModeIconR.get("Pro");
        int[] photoLoc = mModeIconL.get("Photo");
        boolean switchtext = false;
        for (int i = 0; i < CYCLE_TIMES; i++) {
            Log.i(TAG, "start this time i=" + i);
            if(i==0) {
                resetPreview();
                if(proLoc == null){
                    proLoc = mModeIconL.get("Pro");
                }else{
                    switchModeTextToR(true);
                    switchtext = true;
                }
                Log.i(TAG, "testPro loc="+proLoc[0]+"*"+proLoc[1]);
                executeShellCommand("input tap "+ proLoc[0]  +" "+proLoc[1]);
                checkPreview("0", CaptureModule.CameraMode.PRO_MODE);
                if(switchtext) {
                    switchModeTextToR(false);
                }
                executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
                checkPreview("0", CaptureModule.CameraMode.DEFAULT);
                continue;
            }
            resetPreview();
            if(switchtext) {
                switchModeTextToR(true);
            }
            executeShellCommand("input tap "+ proLoc[0]  +" "+proLoc[1]);
            checkPreview("0", CaptureModule.CameraMode.PRO_MODE);
            performenceValues = new HashMap<String,HashMap<String,Long>>();
            HashMap<String,Long> photoToPro = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("photoToPro",photoToPro);
            testFlash("0",CaptureModule.CameraMode.PRO_MODE,true);
            if(switchtext) {
                switchModeTextToR(false);
            }
            executeShellCommand("input tap "+ photoLoc[0]  +" "+photoLoc[1]);
            checkPreview("0", CaptureModule.CameraMode.DEFAULT);
            HashMap<String,Long> proToPhoto = getHashMapValue(mCaptureModule.getHashMapTimes());
            performenceValues.put("proToPhoto",proToPhoto);
            updatePerformenceJson(PERFORMENCE_PRO,performenceValues,i);
        }
    }


}