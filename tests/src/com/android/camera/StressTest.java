/*
Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
SPDX-License-Identifier: BSD-3-Clause-Clear
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
import com.android.camera.util.Log;
import android.graphics.Point;
import java.util.ArrayList;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


@RunWith(AndroidJUnit4.class)
public class StressTest extends TestBase {
    private String TAG = "autoTest_StressTest";
    private static final int TEST_TIMES = 100;
    private static final String ICON_LOC_JSON = "/data/data/org.codeaurora.snapcam/files/iconLocation.json";
    public HashMap<String,String> loctionValues = new HashMap<String,String>();
    @BeforeClass
    public static void initJson(){
        init();
    }
    @Before
    public void beforeEachTest() throws Exception {
        openCameraByIntent(mMainIntent);
        //readIconLoc();
    }

    @After
    public void afterTest() throws Exception {
        Log.i(TAG, "afterEachTest");
        mActivityRule.finishActivity();
    }

    @Test
    public void testAll() throws Exception {
        switchModeTextToR(false);
        List<String> keyList = new ArrayList<String>(mIconLoc.keySet());
        for(int i = 0 ;i < TEST_TIMES * keyList.size();i++){
                if (mActivity.isFinishing()) {
                    Log.i(TAG, "activity finish getPaused=" + mCaptureModule.getPaused());
                    openCameraByIntent(mMainIntent);
                }
                int randomIndex = new Random().nextInt(keyList.size());
                String randomKey = keyList.get(randomIndex);
                int[] loc = mIconLoc.get(randomKey);
                Log.i(TAG, "click  key=" + randomKey + ",loc=" + loc[0] + "," + loc[1]);
                int changevalue = 0;
                switch (randomKey) {
                    case "SwipeModeL":
                        changevalue = loc[0] - SWIPE_STEP;
                        executeShellCommand("input swipe " + loc[0] + " " + loc[1] + " " + changevalue + " " + loc[1]);
                        switchModeTextToR(false);
                        break;
                    case "SwipeModeR":
                        changevalue = loc[0] + SWIPE_STEP;
                        executeShellCommand("input swipe " + loc[0] + " " + loc[1] + " " + changevalue + " " + loc[1]);
                        break;
                    case "Pro":
                        stressTestInPro();
                        break;
                    default:
                        executeShellCommand("input tap " + loc[0] + " " + loc[1]);
                }
                Thread.sleep(SMALL_WAIT_DURATION);
                clickBackCode(randomKey);
        }
    }
    private void clickBackCode(String randomKey)throws Exception{
        if((randomKey.equals("Setting") || randomKey.equals("Thumb")) && mCaptureModule.getPaused()){
            executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
            Log.i(TAG,"click back icon");
            Thread.sleep(OPEN_CAMERA_DURATION);
        }
    }
    private void stressTestInPro()throws Exception{
        clickRightMode("Pro");
        List<String> keyList = new ArrayList<String>(mProLoc.keySet());
        for(int i = 0 ;i < keyList.size();i++) {
            if (mActivity.isFinishing()) {
                Log.i(TAG, "activity finish getPaused=" + mCaptureModule.getPaused());
                openCameraByIntent(mMainIntent);
            }
            int randomIndex = new Random().nextInt(keyList.size());
            String randomKey = keyList.get(randomIndex);
            int[] loc = mProLoc.get(randomKey);
            Log.i(TAG, "click  key=" + randomKey + ",loc=" + loc[0] + "," + loc[1]);
            executeShellCommand("input tap " + loc[0] + " " + loc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
            clickBackCode(randomKey);
        }
        switchModeTextToR(false);
    }

    private void clickRightMode(String mode)throws Exception{
        switchModeTextToR(true);
        int[] proloc = mModeIconR.get(mode);
        executeShellCommand("input tap " + proloc[0] + " " + proloc[1]);
        Thread.sleep(SMALL_WAIT_DURATION);

    }
    @Test
    public void readIconLoc()throws Exception{
        //getModeLoc();
        clickRightMode("Pro");
        getIconLoctionInPro();
        clickRightMode("Depth");
        getDepthUILoc();
        JSONObject saveObj = new JSONObject();
        addObject(saveObj,"iconLoc",mIconLoc);
        addObject(saveObj,"proLoc",mProLoc);
        addObject(saveObj,"modeRLoc",mModeIconR);
        addObject(saveObj,"modeLLoc",mModeIconL);
        addObject(saveObj,"recordLoc",mRecordLoc);
        addObject(saveObj,"depthLoc",mDepthLoc);
        saveJson(ICON_LOC_JSON,saveObj);
    }
    private void addObject(JSONObject obj,String arrayStr, HashMap<String, int[]> mapkey)throws Exception{
        JSONObject mapObj = new JSONObject();
        for(String map:mapkey.keySet()){
            int[] maploc = mapkey.get(map);
            String mapstr = maploc[0]+","+maploc[1];
            mapObj.put(map,mapstr);
        }
        JSONArray jsonarray = new JSONArray();
        jsonarray.put(mapObj);
        obj.put(arrayStr,jsonarray);
    }
}