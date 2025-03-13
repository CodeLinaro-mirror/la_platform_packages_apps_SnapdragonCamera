
/*
/*
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */


package com.android.camera;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Rule;
import org.junit.runner.RunWith;

import android.view.KeyEvent;

import com.android.camera.CameraActivity;
import com.android.camera.CaptureModule;

import android.support.test.rule.ActivityTestRule;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import com.android.camera.util.CameraUtil;
import com.android.camera.util.Log;

@RunWith(AndroidJUnit4.class)
public class KeyValueTest extends TestBase {
    private String TAG = "autoTest_KeyValueTest";
    private static final int CYCLE_TIMES = 1;
    private static LinkedHashMap<String, String> mKeySetting;
    private static HashMap<String, String> mKeySecondSetting;
    private static final String testFile = "/data/data/org.codeaurora.snapcam/files/keyTestParam.txt";
    private static final String RESULT_JSON = "/data/data/org.codeaurora.snapcam/files/testResult.json";
    private boolean isResetSetting;
    private static final String KEY_JSON = "/data/data/org.codeaurora.snapcam/files/keyTest.json";
    private static boolean resetSetting = false;
    StringBuffer passStrBuffer;
    StringBuffer failStrBuffer;
    StringBuffer setStrBuffer;

    private static String testMode = "Photo";
    private static boolean isFrontCamera;
    private String cameraId = "0";
    private CaptureModule.CameraMode mode;

    @BeforeClass
    public static void initJson() throws Exception {
        Log.i("autotest_initJson", "initJson beforeTest");
        mKeySetting = new LinkedHashMap<>();
        mKeySecondSetting = new HashMap<>();
        String testStr = CameraUtil.readFile(testFile);
        String[] str_key = testStr.split("-");
        Log.i("autotest_initJson", "initJson str_key.length=" + str_key.length + ",testStr=" + testStr);
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        if (str_key != null && str_key.length > 0) {
            for (int i = 0; i < str_key.length; i++) {
                if (str_key[i] != null) {
                    Log.i("autotest_initJson", "initJson str_key[i]=" + str_key[i].toString() + ",i=" + i);
                    if (str_key[i].trim().equalsIgnoreCase("reset")) {
                        Log.i("autotest_initJson", " reset is true");
                        resetSetting = true;
                        jsonObject.put("Reset", true);
                    } else if (str_key[i].trim().equalsIgnoreCase("front")) {
                        isFrontCamera = true;
                        jsonObject.put("isFront", true);
                    } else {
                        String[] str_value = str_key[i].split(",");
                        if (str_value.length > 1) {
                            Log.i("autotest_initJson", "set key =" + str_value[0] + ",value is " + str_value[1]);
                            if (str_value[0].equals("mode")) {
                                testMode = str_value[1].trim();
                                jsonObject.put("testMode", testMode);
                            } else {
                                mKeySetting.put(str_value[0].trim(), str_value[1].trim());
                                jsonObject.put(str_value[0].trim(), str_value[1].trim());
                                if (str_value.length > 2) {
                                    for (int k = 0; k < str_value.length; k++) {
                                        mKeySecondSetting.put(str_value[0].trim(), str_value[k].trim());
                                        jsonObject.put(str_value[0].trim(), str_value[k].trim());
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        saveJson(KEY_JSON, jsonObject, false);
    }

    @AfterClass
    public static void resetTestItem() {

    }

    @Before
    public void beforeEachTest() throws Exception {
        Log.i(TAG, "beforeEachTest");
        init();
        OpenCamera();
        isPerformenceTest = true;
    }

    @After
    public void afterEachTest() throws Exception {
        Log.i(TAG, "afterEachTest");
        mActivityRule.finishActivity();
    }

    private void goModeSetting() throws Exception {
        passStrBuffer = new StringBuffer();
        failStrBuffer = new StringBuffer();
        int[] modeLoc = mModeIconL.get(testMode);
        if (modeLoc == null) {
            modeLoc = mModeIconR.get(testMode);
        }
        mode = getModeName(testMode);
        if (mode != mCaptureModule.getCurrenCameraMode()) {
            executeShellCommand("input tap " + modeLoc[0] + " " + modeLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
        }
        Log.i(TAG,"zcl mCaptureModule.getMainCameraId()="+mCaptureModule.getMainCameraId()+",isFrontCamera="+isFrontCamera);
        if (isFrontCamera && mCaptureModule.getMainCameraId() != 1) {
            cameraId = "1";
            executeShellCommand("input tap " + mSwitchLoc[0] + " " + mSwitchLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
        } else if (mode == CaptureModule.CameraMode.HFR || mode == CaptureModule.CameraMode.CINEMATIC) {
            cameraId = "2";
        }
        checkPreview(cameraId, mode);
    }

    private void checkKeyValue() {
        for (Map.Entry<String, String> entry : mKeySetting.entrySet()) {
            String key_title = entry.getKey();
            String key = mSettingsManager.getKeyFromTitle(key_title);
            String value = entry.getValue();
            String keyvalue = mSettingsManager.getEntry(key_title);
            if (!keyvalue.equalsIgnoreCase(value)) {
                failStrBuffer.append(key_title + " checkKeyValue Fail: set key value is " + value +
                        ",get key value is " + keyvalue + ";");
            } else {
                passStrBuffer.append(key_title + " checkKeyValue Pass: set key value is " + value +
                         ",get key value is " + keyvalue + ";");
            }
            Log.i(TAG,"zcl key_title.toLowerCase()="+key_title.toLowerCase()+",value="+value);
            switch (key_title.toLowerCase()) {
                case "mixed hdr":
                    if (value.equalsIgnoreCase("Manual HDR")) {
                        String hdrmode = mSettingsManager.getVideoHdrMode();
                        String secondValue = mKeySecondSetting.get(key_title);
                        String[] sec_value = secondValue.split("\\+");
                        String[] set_value = hdrmode.split(" ");
                        if (set_value.length != sec_value.length) {
                            failStrBuffer.append("checkSettingValue length Fail:value in setting is " + hdrmode +
                                    "we need it is " + secondValue + ";");
                            return;
                        }
                        for (int i = 0; i < sec_value.length; i++) {
                            if (!set_value[i].equals(sec_value[i])) {
                                failStrBuffer.append("check each SettingValue Fail: value in setting is " + hdrmode +
                                        ",we need it is " + secondValue + ";");
                                return;
                            }
                        }
                    }
                    testResult = true;
                    checkMixedHDR();
                    if (testResult) {
                        passStrBuffer.append("checkMixedHDR Pass:captureResult check Pass;");
                    } else {
                        String failstr = getTestFail(testFail);
                        if (failstr == null) {
                            failstr = "captureResult check fail";
                        }
                        failStrBuffer.append("checkMixedHDR Fail:" + failstr + ";");
                    }
                    break;
                case "quad bayer sensor":
                    boolean checkid = checkCameraId(key);
                    Log.i(TAG,"checkeddi="+checkid);
                    if (checkid) {
                        passStrBuffer.append("CheckCameraId Pass: MainCameraId is " + mCaptureModule.getMainCameraId() + ";");
                    } else {
                        passStrBuffer.append("CheckCameraId Fail: MainCameraId is " + mCaptureModule.getMainCameraId()
                                + "set id is " + value + ";");
                    }
                    break;
                case "insensor zoom":
                    int zoomInResult = mCurrentCaptureResult.get(CaptureModule.insensor_zoom_result);
                    Log.i(TAG,"zcl zoomInResult="+zoomInResult);
                    if (keyvalue.equals("enable")){
                     if (zoomInResult == 1) {
                        passStrBuffer.append("zoomInResult check Pass:zoomInResult is " + zoomInResult + ";");
                    }else{
                         failStrBuffer.append("zoomInResult check Fail:zoomInResult is " + zoomInResult + ";");
                     }
                }else if(keyvalue.equals("disable")){
                        if (zoomInResult == 0) {
                            passStrBuffer.append("zoomInResult check Pass:zoomInResult is " + zoomInResult +";");
                        }else{
                            failStrBuffer.append("zoomInResult check Fail:zoomInResult is " + zoomInResult + ";");
                        }
                }
                break;

            }

        }
    }

    private String getTestFail(String failstr) {
        String fail = null;
        if (failstr != null && failstr.indexOf(":") > 0) {
            String[] str = failstr.split(":");
            fail = str[1];
        }
        return fail;
    }

    @Test
    public void testKeyValue() throws Exception {
        testResult = true;
        if (resetSetting) {
            goSettings();
            mSettingsManager.restoreSettings();
            backSettings();
        }
        goModeSetting();
        if (!testResult) {
            String failstr = getTestFail(testFail);
            if (failstr == null) {
                failstr = "";
            }
            failStrBuffer.append("goModeAndCameraid Fail:" + failstr + ",mode is " + testMode + ",cameraid is " + cameraId + ";");
        } else {
            passStrBuffer.append("goModeAndCameraid Pass: mode is " + testMode + ",cameraid is " + cameraId + ";");
        }
        if (!mCaptureModule.getPaused()) {
            executeShellCommand("input tap " + mSettingLoc[0] + " " + mSettingLoc[1]);
            Thread.sleep(SMALL_WAIT_DURATION);
        }

        mActivity.runOnUiThread(() -> {
            if (mCaptureModule.getPaused()) {
                passStrBuffer.append("go to Setting Pass:CameraActivity is  paused;");
            } else {
                failStrBuffer.append("go to Setting Fail:CameraActivity is not paused;");
            }
            for (Map.Entry<String, String> entry : mKeySetting.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                boolean set_result = mSettingsManager.setTitleEntry(key, value);
                if (set_result) {
                    passStrBuffer.append("setTitleEntry Pass: key is " + key + ",value is " + value + ";");
                } else {
                    failStrBuffer.append("setTitleEntry Fail: key is " + key + ",value is " + value + ";");
                }
                Log.i(TAG, " key=" + key + ",value=" + value + ",set_result=" + set_result);
                if (key.equalsIgnoreCase("Mixed HDR") && value.equalsIgnoreCase("Manual HDR")) {
                    String secondValue = mKeySecondSetting.get(key);
                    Log.i(TAG, "setTitleEntry_secondValue=" + secondValue);
                    String[] sec_value = secondValue.split("\\+");
                    for (int i = 0; i < sec_value.length; i++) {
                        boolean setchecked = mSettingsManager.setPreferenceChecked(sec_value[i]);
                        if (setchecked) {
                            passStrBuffer.append("setPreferenceChecked " + sec_value[i] + " Pass:value is true;");
                        } else {
                            failStrBuffer.append("setPreferenceChecked " + sec_value[i] + "Fail:set fail;");
                        }

                    }
                }
            }
        });
        Thread.sleep(OPEN_CAMERA_DURATION);

        if (mCaptureModule.getPaused()) {
            executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
            Thread.sleep(SMALL_WAIT_DURATION);
        }
        if (mCaptureModule.getPaused()) {
            Log.i(TAG, "click backkey=" + KeyEvent.KEYCODE_BACK);
            executeShellCommand("input keyevent " + KeyEvent.KEYCODE_BACK);
            Thread.sleep(SMALL_WAIT_DURATION);
        }
        if (mCaptureModule.getPaused()) {
            failStrBuffer.append("backToPreview after setting Fail:CameraActivity is paused;");
        } else {
            passStrBuffer.append("backToPreview after setting Pass:CameraActivity is not paused;");
        }
        testResult = true;
        clickShutterButton(mode);
        if (testResult) {
            passStrBuffer.append("SnapShot or Recording Pass: Checking pass;");
        } else {
            String failstr = getTestFail(testFail);
            if (failstr == null) {
                failstr = "Checking fail";
            }
            failStrBuffer.append("SnapShot or Recording Fail :" + failstr + ";");
        }
        checkKeyValue();
        Log.i(TAG, "fail is " + failStrBuffer + ",pass is " + passStrBuffer);
        saveTestResut();
    }


    private void saveTestResut() throws Exception {
        JSONObject json_Object = new JSONObject();
        if (failStrBuffer.length() > 0) {
            String[] str = failStrBuffer.toString().split(";");
            for (int i = 0; i < str.length; i++) {
                String[] fail = str[i].split(":");
                if (fail.length > 1) {
                    json_Object.put(fail[0], fail[1]);
                } else {
                    json_Object.put(fail[0], "FAIL");
                }
            }
        }
        if (passStrBuffer.length() > 0) {
            String[] pass_str = passStrBuffer.toString().split(";");
            for (int i = 0; i < pass_str.length; i++) {
                String[] pass = pass_str[i].split(":");
                if (pass.length > 1) {
                    json_Object.put(pass[0], pass[1]);
                } else {
                    json_Object.put(pass[0], "FAIL");
                }
            }
        }
        saveJson(RESULT_JSON, json_Object, false);
    }
}