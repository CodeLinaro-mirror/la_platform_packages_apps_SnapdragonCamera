/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
Not a contribution.
*/

/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ColorSpace;
import android.graphics.ColorSpace.Named;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.ColorSpaceProfiles;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import androidx.annotation.NonNull;
import android.view.Window;
import android.view.WindowManager;
import com.android.camera.util.Log;
import android.util.ArraySet;
import android.util.Size;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;

import org.codeaurora.snapcam.R;
import com.android.camera.util.CameraUtil;
import com.android.camera.CaptureModule.CameraMode;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.util.PersistUtil;
import com.android.camera.DragonListView;

import org.codeaurora.snapcam.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.camera.CaptureModule.CameraMode.DEFAULT;
import static com.android.camera.CaptureModule.CameraMode.HFR;
import static com.android.camera.CaptureModule.CameraMode.RTB;
import static com.android.camera.CaptureModule.CameraMode.SAT;
import static com.android.camera.CaptureModule.CameraMode.VIDEO;
import android.app.Dialog;

import android.widget.CheckBox;
import com.android.camera.FdExpandListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListAdapter;
import android.widget.RelativeLayout;
import com.android.camera.FdExpandListView.FdExpandListViewAdapter;
public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "SettingsActivity";

    private static final boolean DEV_LEVEL_ALL =
            PersistUtil.getDevOptionLevel() == PersistUtil.CAMERA2_DEV_OPTION_ALL  ;
    public static final String CAMERA_MODULE = "camera_module";
    public static final String IS_SIGNGLE_CAMERA_MODULE = "is_single_camera_mode";
    public static final String OPEN_DEVOPTION = "open_devoption";
    private SettingsManager mSettingsManager;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mLocalSharedPref;
    private boolean mDeveloperMenuEnabled;
    private int privateCounter = 0;
    private final int DEVELOPER_MENU_TOUCH_COUNT = 10;
    private FdExpandListView fdExpandListView;
    private FdExpandListView fdFLExpandListView;
    private FdExpandListView fdFacialExpandListView;
    private ExpandableListView expandableListView = null;
    private FdExpandListViewAdapter expandableAdapter = null;
    private ExpandableListView fdFLExpandableListView = null;
    private FdExpandListViewAdapter fdFLExpandableAdapter = null;
    private ExpandableListView fdFacialExpandableListView = null;
    private FdExpandListViewAdapter fdFacialExpandableAdapter = null;
    private boolean mIsSingleCameraMode = false;
    private boolean mShowAllDevOption = false;

    private ArrayList<CameraCharacteristics> mCharacteristics;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            Preference p = findPreference(key);
            Log.i(TAG, "onSharedPreferenceChanged key:" + key);
            if (p == null || null == key) return;
            String value;
            if (p instanceof SwitchPreference) {
                boolean checked = ((SwitchPreference) p).isChecked();
                value = checked ? "on" : "off";
                mSettingsManager.setValue(key, value);
            } else if (p instanceof ListPreference){
                value = ((ListPreference) p).getValue();
                mSettingsManager.setValue(key, value);
            } else if (p instanceof MultiSelectListPreference) {
                Set<String> valueSet = ((MultiSelectListPreference)p).getValues();
                mSettingsManager.setValue(key,valueSet);
                if (key.equals(SettingsManager.KEY_PHYSICAL_CAMERA) ||
                        key.equals(SettingsManager.KEY_PHYSICAL_CAMCORDER)) {
                    updateMultiVideoFPSPreference();
                }
                if (key.equals(SettingsManager.KEY_PHYSICAL_CAMCORDER)) {
                    updateEISPreference();
                }
            }
            if (key.equals(SettingsManager.KEY_VIDEO_QUALITY)) {
                updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
                updatePreference(SettingsManager.KEY_VIDEO_ENCODER);
                updateVideoMFHDRPreference();
                updateVideoFlipPreference();
                updateVsrPreference();
                updateViullPreference();
            } else if (key.equals(SettingsManager.KEY_VIDEO_ENCODER)) {
                updateVideoEncoderProfile();
            } else if (key.equals(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE)) {
                value = ((ListPreference) p).getValue();
                if (!value.equals("off")) {
                    int fpsRate = Integer.parseInt(value.substring(3));
                    if (fpsRate == 480) {
                        mSettingsManager.filterVideoDurationFor480fps();
                    } else {
                        mSettingsManager.filterVideoDuration();
                    }
                } else {
                    mSettingsManager.filterVideoDuration();
                }
                updateVideoVariableFpsPreference();
                updatePreference(SettingsManager.KEY_VIDEO_DURATION);
                updateVideoMFHDRPreference();
                updateViullPreference();
                updateVsrPreference();
            } else if (key.equals(SettingsManager.KEY_SELECT_MODE)) {
                CaptureModule.CameraMode mode = (CaptureModule.CameraMode)
                        getIntent().getSerializableExtra(CAMERA_MODULE);
                updatePdnetTogglePreference();
                updateViullPreference();
            } else if (key.equals(SettingsManager.KEY_MULTIRESIMAGEREADER)) {
                //when multiresolutionimagereader enabled, disable KEY_PICTURE_SIZE
                value = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESIMAGEREADER);
                Preference picSize = findPreference(SettingsManager.KEY_PICTURE_SIZE);
                ListPreference picFormat = (ListPreference)findPreference(SettingsManager.KEY_PICTURE_FORMAT);
                if (PersistUtil.isMultiResolutionImageReaderEnabled() && value != null && "1".equals(value)) {
                    picSize.setEnabled(false);
                    if (picFormat != null) {
                        picFormat.setValue("0");
                        picFormat.setEnabled(false);
                    }
                } else {
                    picSize.setEnabled(true);
                    if (picFormat != null) {
                        picFormat.setEnabled(true);
                    }
                }
            } else if (key.equals(SettingsManager.KEY_VIDEO_ENCODER_PROFILE)){
                CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
                if(mode == CaptureModule.CameraMode.VIDEO) {
                    updateSwitchIDInModePreference(true);
                }
                updateViullPreference();
            }
            List<String> list = mSettingsManager.getDependentKeys(key);
            if (list != null) {
                for (String dependentKey : list) {
                    Log.i(TAG, "onSharedPreferenceChanged dependentKey:" + dependentKey);
                    updatePreferenceButton(dependentKey);
                }
            }
            // If Enable KEY_BURST_LIMIT, KEY_CAPTURE_MFNR_VALUE and KEY_LONGSHOT can same use
            // if diable KEY_BURST_LIMIT, enable KEY_CAPTURE_MFNR_VALUE, KEY_LONGSHOT is diable
            if (key.equals(SettingsManager.KEY_BURST_LIMIT) ||
                    key.equals(SettingsManager.KEY_CAPTURE_MFNR_VALUE)) {
                updateLongShotPreference();
            }

            if (key.equals(SettingsManager.KEY_RAW_FORMAT_TYPE)) {
                updateCaptureProfilePref();
            }

            if (key.equals(SettingsManager.KEY_MANUAL_HDR)) {
                value = ((ListPreference) p).getValue();
                if (value.equals("manual")) {
                    updateManualHDRSetting();
                }
                updateHdrRefOp();
                updateQuadBayerPreference();
            }

            if (key.equals(SettingsManager.KEY_MANUAL_HDR) ||
                    key.equals(SettingsManager.KEY_QLL)) {
                updateQLLPreference();
            }

            if (key.equals(SettingsManager.KEY_PICTURE_FORMAT) ||
                    key.equals(SettingsManager.KEY_PREVIEW_PROFILE) ||
                    key.equals(SettingsManager.KEY_CAPTURE_PROFILE) ||
                    key.equals(SettingsManager.KEY_RAW_FORMAT_TYPE)) {
                updateColorSpacePreference();
            }

            if (key.equals(SettingsManager.KEY_RAW_REPROCESS_TYPE)) {
                updatePreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS);
                updatePreference(SettingsManager.KEY_RAWINFO_TYPE);
                updatePreference(SettingsManager.KEY_RAW_FORMAT_TYPE);
                updatePictureFormatPreference();
            }
            if (key.equals(SettingsManager.KEY_REMOSAIC_REPROCESSING)) {
                updateRawFormatPref();
            }
            if (key.equals(SettingsManager.KEY_RAW_FORMAT_TYPE)) {
                updateRawInfoPref();
            }
            if (key.equals(SettingsManager.KEY_RAW_FORMAT_TYPE)){
                updateVideoMFHDRPreference();
            }
            if(key.equals(SettingsManager.KEY_INSENSOR_ZOOM)){
                updateVideoMFHDRPreference();
            }
            if (SettingsManager.KEY_PREVIEW_PROFILE.equals(key)) {
                updateViullPreference();
            }
            if (SettingsManager.KEY_LONGSHOT.equals(key)) {
                updateMFNRPreference();
            }
        }
    };

    private SettingsManager.Listener mListener = new SettingsManager.Listener(){
        @Override
        public void onSettingsChanged(List<SettingsManager.SettingState> settings){
            Map<String, SettingsManager.Values> map = mSettingsManager.getValuesMap();
            for( SettingsManager.SettingState state : settings) {
                SettingsManager.Values values = map.get(state.key);
                boolean enabled = true;
                if(values != null) enabled = values.overriddenValue == null;
                Preference pref = findPreference(state.key);
                if (pref == null) continue;
                pref.setEnabled(enabled);

                Log.i(TAG, "onSettingsChanged key :" + state.key + ", enabled :" + enabled);

                if (pref.getKey().equals(SettingsManager.KEY_MANUAL_EXPOSURE)) {
                    UpdateManualExposureSettings();
                }
                if (pref.getKey().equals(SettingsManager.KEY_PICTURE_FORMAT) ||
                        pref.getKey().equals(SettingsManager.KEY_EIS_VALUE)) {
                    mSettingsManager.updatePictureAndVideoSize();
                    updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                    updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                    updateCaptureProfilePref();
                }

                if (pref.getKey().equals(SettingsManager.KEY_MANUAL_WB)) {
                    updateManualWBSettings();
                }

                if(pref.getKey().equals(SettingsManager.KEY_CAPTURE_MFNR_VALUE)) {
                    updateZslPreference();
                    updatePictureFormatPreference();
                    updateHDRSceneDetection();
                    if(isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE)){
                        CaptureModule.CameraMode mode =
                                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
                        if(CaptureModule.CameraMode.RTB == mode) {
                            mSettingsManager.setValueIndex(SettingsManager.KEY_SCENE_MODE, 0);
                        }
                    }
                }
                if(pref.getKey().equals(SettingsManager.KEY_MULTIRESREPROCESS)) {
                    updateZslPreference();
                    updateMultiResReprocess();
                }
                if(pref.getKey().equals(SettingsManager.KEY_QUAD_BAYER_SENSOR)) {
                    mSettingsManager.updatePictureAndVideoSize();
                    mSettingsManager.updateHDRSceneMode();
                    updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                    updatePreference(SettingsManager.KEY_SCENE_MODE);
                    updateZslPreference();
                    updateForceAUXPreference();
                    updateLongShotPreference();
                    updatePictureFormatPreference();
                    updateVideoMFHDRPreference();
                    updateSwitchIDInModePreference(false);
                    if(mSettingsManager.isMultiCameraEnabled()){
                        mSettingsManager.buildMultiCameraPreference();
                        recreate();
                    }
                }

                if (pref.getKey().equals(SettingsManager.KEY_TONE_MAPPING)) {
                    updateToneMappingSettings();
                }

                if (pref.getKey().equals(SettingsManager.KEY_MULTI_CAMERA_MODE)){
                    recreate();
                }

                if (pref.getKey().equals(SettingsManager.KEY_VIDEO_QUALITY) ||
                        pref.getKey().equals(SettingsManager.KEY_SELECT_MODE)){
                    updateVideoVariableFpsPreference();
                    updateVideoHfrFpsPreference();
                    CaptureModule.CameraMode mode =
                            (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
                    if(mode == CaptureModule.CameraMode.VIDEO) {
                        mSettingsManager.filterVideoEncoderProfileOptions();
                        updatePreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
                    }
                    updateForceAUXPreference();
                    updateAICameraPerf();
                    updateEISPreference();
                }
                if(pref.getKey().equals(SettingsManager.KEY_HFR_BUFFER_MODE)){
                    updateVideoHfrFpsPreference();
                }

                if (pref.getKey().equals(SettingsManager.KEY_SCENE_MODE)) {
                   String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
                   if (!scene.equals("18")) {
                       ListPreference lp = (ListPreference)findPreference(
                               SettingsManager.KEY_SNAPSHOT_HDRMODE);
                       if (lp != null) {
                           lp.setValue("default");
                           lp.setEnabled(false);
                       }
                   }
                    updateViullPreference();
                    updateVideoMFHDRPreference();
                }

                if (pref.getKey().equals(SettingsManager.KEY_MANUAL_HDR) ||
                        pref.getKey().equals(SettingsManager.KEY_SELECT_MODE) ||
                        pref.getKey().equals(SettingsManager.KEY_SWITCH_CAMERA)) {
                    mSettingsManager.updatePictureAndVideoSize();
                    updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                    updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                }
                if (pref.getKey().equals(SettingsManager.KEY_RAW_REPROCESS_TYPE)) {
                    updatePreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS);
                    updatePreference(SettingsManager.KEY_RAWINFO_TYPE);
                    updatePreference(SettingsManager.KEY_RAW_FORMAT_TYPE);
                    updatePictureFormatPreference();
                }
                if (pref.getKey().equals(SettingsManager.KEY_RAW_FORMAT_TYPE)) {
                    updateRawInfoPref();
                }
                if(pref.getKey().equals(SettingsManager.KEY_VSR)){
                    updateVideoHfrFpsPreference();
                    mSettingsManager.updatePictureAndVideoSize();
                    updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                    updateViullPreference();
                }
                if (pref.getKey().equals(SettingsManager.KEY_AI_CAMERA)){
                    updateSwitchIDInModePreference(true);
                    updateEISPreference();
                }
                if (pref.getKey().equals(SettingsManager.KEY_EIS_VALUE)) {
                    updateVideoMFHDRPreference();
                }

                if (SettingsManager.KEY_PHOTO_EIS_VALUE.equals(pref.getKey())
                        || SettingsManager.KEY_EIS_VALUE.equals(pref.getKey()) ||
                        SettingsManager.KEY_PHYSICAL_CAMERA.equals(pref.getKey()) ||
                        SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK.equals(pref.getKey())) {
                    updatePreviewStabilizationPreference();
                }
                if (mSettingsManager.KEY_FACE_DETECTION.equals(pref.getKey()) ||
                        mSettingsManager.KEY_SELECT_MODE.equals(pref.getKey())) {
                    updateT2TPreference();
                }
                if(mSettingsManager.KEY_SWITCH_CAMERA .equals(pref.getKey())){
                    checkExposurTimeValue();
                }
                if(mSettingsManager.KEY_EXTENDED_MAX_ZOOM .equals(pref.getKey())){
                    updateZoomPreference();
                }
            }
        }
    };
    private void checkExposurTimeValue(){
        String exposuretime = mSettingsManager.getKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE);
        long []mExposureTime = mSettingsManager.getExposureRangeValues();
        if(exposuretime != null && !exposuretime.equals("") && !exposuretime.equals("auto")){
            long exptime = CameraUtil.strToLong(exposuretime,100000000);
            if(mExposureTime[1] < exptime){
                mSettingsManager.setKeyValue(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE,true,String.valueOf(mExposureTime[1]));
            }
        }
    }
    private boolean isPrefEnabled(String key) {
        boolean result = false;
        String prefValue = mSettingsManager.getValue(key);
        if (prefValue != null) {
            result = prefValue.equals("1");
        }
        return result;
    }

    private void updateZslPreference() {
        boolean isInSATOrRTBMode = false;
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode != null && (mode == RTB || mode ==SAT)) {
            isInSATOrRTBMode = true;
        }
        ListPreference ZSLPref = (ListPreference) findPreference(SettingsManager.KEY_ZSL);
        List<String> key_zsl = new ArrayList<String>(Arrays.asList("Off", "HAL-ZSL" ));
        List<String> value_zsl = new ArrayList<String>(Arrays.asList( "disable", "hal-zsl"));
        if (ZSLPref != null) {
            if (!isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE) &&
                    !mSettingsManager.getQuadBayerSensorPrefEnabled() &&
                    !isInSATOrRTBMode &&
                    !isPrefEnabled(SettingsManager.KEY_MULTIRESREPROCESS)) {
                key_zsl.add("APP-ZSL");
                value_zsl.add("app-zsl");
            }
            ZSLPref.setEntries(key_zsl.toArray(new CharSequence[key_zsl.size()]));
            ZSLPref.setEntryValues(value_zsl.toArray(new CharSequence[value_zsl.size()]));
            int idx = ZSLPref.findIndexOfValue(ZSLPref.getValue());;
            if (idx < 0 ) {
                idx = 0;
            }
            ZSLPref.setValueIndex(idx);
        }
    }

    private void UpdateManualExposureSettings() {
        //dismiss all popups first, because we need to show edit Dialog
        int cameraId = mSettingsManager.getCurrentCameraId();
        final SharedPreferences pref = SettingsActivity.this.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(SettingsActivity.this,
                        mSettingsManager.getCurrentPrepNameKey()), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        LinearLayout linear = new LinearLayout(SettingsActivity.this);
        linear.setOrientation(1);
        final TextView ISOtext = new TextView(SettingsActivity.this);
        final EditText ISOinput = new EditText(SettingsActivity.this);
        final TextView ExpTimeText = new TextView(SettingsActivity.this);
        final EditText ExpTimeInput = new EditText(SettingsActivity.this);
        ISOinput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ExpTimeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setTitle("Manual Exposure Settings");
        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });
        String isoPriority = this.getString(
                R.string.pref_camera_manual_exp_value_ISO_priority);
        String expTimePriority = this.getString(
                R.string.pref_camera_manual_exp_value_exptime_priority);
        String userSetting = this.getString(
                R.string.pref_camera_manual_exp_value_user_setting);
        String gainsPriority = this.getString(
                R.string.pref_camera_manual_exp_value_gains_priority);
        String manualExposureMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_EXPOSURE);
        String currentISO = pref.getString(SettingsManager.KEY_MANUAL_ISO_VALUE, "-1");
        long[] exposureRange = mSettingsManager.getExposureRangeValues(cameraId);

        int[] isoRange = mSettingsManager.getIsoRangeValues(cameraId);
        if (!currentISO.equals("-1")) {
            ISOtext.setText("Current ISO is " + currentISO);
        }
        String currentExpTime = pref.getString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, "-1");
        if (!currentExpTime.equals("-1")) {
            ExpTimeText.setText("Current exposure time is " + currentExpTime);
        }
        Log.v(TAG, "manual Exposure Mode selected = " + manualExposureMode);
        if (manualExposureMode.equals(isoPriority)) {
            alert.setMessage("Enter ISO in the range of " + isoRange[0] + " to " + isoRange[1]);
            linear.addView(ISOinput);
            linear.addView(ISOtext);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newISO = -1;
                    String iso = ISOinput.getText().toString();
                    Log.v(TAG, "string iso length " + iso.length() + ", iso :" + iso);
                    if (iso.length() > 0) {
                        try {
                            newISO = Integer.parseInt(iso);
                        } catch(NumberFormatException e) {
                            Log.w(TAG, "ISOinput type incorrect value entered ");
                        }
                    }
                    if (newISO <= isoRange[1] && newISO >= isoRange[0]) {
                        editor.putString(SettingsManager.KEY_MANUAL_ISO_VALUE, iso);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "Invalid ISO",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                    editor.apply();
                }
            });
            alert.show();
        } else if (manualExposureMode.equals(expTimePriority)) {
            if (exposureRange == null) {
                alert.setMessage("Get Exposure time range is NULL ");
            } else {
                alert.setMessage("Enter exposure time in the range of " + exposureRange[0]
                        + "ns to " + exposureRange[1] + "ns");
            }
            linear.addView(ExpTimeInput);
            linear.addView(ExpTimeText);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    double newExpTime = -1;
                    String expTime = ExpTimeInput.getText().toString();
                    if (expTime.length() > 0) {
                        try {
                            newExpTime = Double.parseDouble(expTime);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Input expTime " + expTime + " is invalid");
                            newExpTime = Double.parseDouble(expTime) + 1f;
                        }
                    }
                    if (exposureRange != null &&
                            newExpTime <= exposureRange[1] && newExpTime >= exposureRange[0]) {
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, expTime);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "Invalid exposure time",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                    editor.apply();
                }
            });
            alert.show();
        } else if (manualExposureMode.equals(userSetting)) {
            alert.setMessage("Full manual mode - Enter both ISO and Exposure Time");
            final TextView ISORangeText = new TextView(this);
            final TextView ExpTimeRangeText = new TextView(this);
            ISORangeText.setText("Enter ISO in the range of " + isoRange[0] + " to " + isoRange[1]);
            if (exposureRange == null) {
                ExpTimeRangeText.setText("Get Exposure time range is NULL ");
            } else {
                ExpTimeRangeText.setText("Enter exposure time in the range of " + exposureRange[0]
                        + "ns to " + exposureRange[1] + "ns");
            }
            linear.addView(ISORangeText);
            linear.addView(ISOinput);
            linear.addView(ISOtext);
            linear.addView(ExpTimeRangeText);
            linear.addView(ExpTimeInput);
            linear.addView(ExpTimeText);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newISO = -1;
                    String iso = ISOinput.getText().toString();
                    Log.v(TAG, "string iso length " + iso.length() + ", iso :" + iso);
                    if (iso.length() > 0) {
                        try {
                            newISO = Integer.parseInt(iso);
                        } catch(NumberFormatException e) {
                            Log.w(TAG, "ISOinput type incorrect value entered ");
                        }
                    }
                    if (newISO <= isoRange[1] && newISO >= isoRange[0]) {
                        editor.putString(SettingsManager.KEY_MANUAL_ISO_VALUE, iso);
                        editor.apply();
                    } else {
                        RotateTextToast.makeText(SettingsActivity.this, "Invalid ISO",
                                Toast.LENGTH_SHORT).show();
                    }

                    double newExpTime = -1;
                    String expTime = ExpTimeInput.getText().toString();
                    if (expTime.length() > 0) {
                        try {
                            newExpTime = Double.parseDouble(expTime);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Input expTime " + expTime + " is invalid");
                            newExpTime = Double.parseDouble(expTime) + 1f;
                        }
                    }
                    if (exposureRange != null &&
                            newExpTime <= exposureRange[1] && newExpTime >= exposureRange[0]) {
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, expTime);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "Invalid exposure time",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                    editor.apply();
                }
            });
            alert.show();
        } else if (manualExposureMode.equals(gainsPriority)){
            handleManualGainsPriority(linear, ISOtext, ExpTimeInput, pref);
        }
    }

    private void handleManualGainsPriority(final LinearLayout linear, final TextView gainsText,
        final EditText gainsInput, final SharedPreferences pref) {
        SharedPreferences.Editor editor = pref.edit();
        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        int cameraId = mSettingsManager.getCurrentCameraId();
        int[] isoRange = mSettingsManager.getIsoRangeValues(cameraId);
        float[] gainsRange = new float[2];
        gainsRange[0] = 1.0f;
        gainsRange[1] = (float) isoRange[1]/isoRange[0];
        float currentGains = pref.getFloat(SettingsManager.KEY_MANUAL_GAINS_VALUE, -1.0f);
        if (currentGains != -1.0f) {
            gainsText.setText(" Current Gains is " + currentGains);
        } else {
            gainsText.setText(" Please enter gains value ");
        }
        alert.setMessage("Enter gains in the range of " + gainsRange[0] + " to " + gainsRange[1]);
        linear.addView(gainsInput);
        linear.addView(gainsText);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog, int id) {
                float newGain = -1;
                String gain = gainsInput.getText().toString();
                Log.v(TAG, "string gain length " + gain.length() + ", gain :" + gain);
                if (gain.length() > 0) {
                    try {
                        newGain = Float.parseFloat(gain);
                    } catch(NumberFormatException e) {
                        Log.w(TAG, "gainsInput type incorrect value ");
                    }
                }
                if (newGain <= gainsRange[1] && newGain >= gainsRange[0]) {
                    editor.putFloat(SettingsManager.KEY_MANUAL_GAINS_VALUE, newGain);
                    editor.apply();
                } else {
                    editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                    editor.apply();
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid GAINS",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog,int id) {
                editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE, "off");
                editor.apply();
            }
        });
        alert.show();
    }

    private void showManualWBGainDialog(final LinearLayout linear,
                                        final AlertDialog.Builder alert) {
        SharedPreferences.Editor editor = mLocalSharedPref.edit();
        final TextView rGainTtext = new TextView(SettingsActivity.this);
        final TextView rGainValue = new TextView(SettingsActivity.this);
        final EditText rGainInput = new EditText(SettingsActivity.this);
        final TextView gGainTtext = new TextView(SettingsActivity.this);
        final TextView gGainValue = new TextView(SettingsActivity.this);
        final EditText gGainInput = new EditText(SettingsActivity.this);
        final TextView bGainTtext = new TextView(SettingsActivity.this);
        final TextView bGainValue = new TextView(SettingsActivity.this);
        final EditText bGainInput = new EditText(SettingsActivity.this);
        int floatType = InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER;
        rGainInput.setInputType(floatType);
        gGainInput.setInputType(floatType);
        bGainInput.setInputType(floatType);

        float rGain = mLocalSharedPref.getFloat(SettingsManager.KEY_MANUAL_WB_R_GAIN, -1.0f);
        float gGain = mLocalSharedPref.getFloat(SettingsManager.KEY_MANUAL_WB_G_GAIN, -1.0f);
        float bGain = mLocalSharedPref.getFloat(SettingsManager.KEY_MANUAL_WB_B_GAIN, -1.0f);

        if (rGain == -1.0) {
            rGainValue.setText(" Current rGain is " );
        } else {
            rGainValue.setText(" Current rGain is " + rGain);
        }
        if (rGain == -1.0) {
            gGainValue.setText(" Current gGain is " );
        } else {
            gGainValue.setText(" Current gGain is " + gGain);
        }
        if (rGain == -1.0) {
            bGainValue.setText(" Current bGain is ");
        } else {
            bGainValue.setText(" Current bGain is " + bGain);
        }
        int cameraId = mSettingsManager.getCurrentCameraId();
        final float[] gainsRange = mSettingsManager.getWBGainsRangeValues(cameraId);
        //refresh camera parameters to get latest CCT value
        if (gainsRange == null) {
            alert.setMessage("Enter gains value in the range get is NULL ");
        } else {
            alert.setMessage("Enter gains value in the range of " + gainsRange[0]+ " to " + gainsRange[1]);
        }
        linear.addView(rGainTtext);
        linear.addView(rGainInput);
        linear.addView(rGainValue);
        linear.addView(gGainTtext);
        linear.addView(gGainInput);
        linear.addView(gGainValue);
        linear.addView(bGainTtext);
        linear.addView(bGainInput);
        linear.addView(bGainValue);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog, int id) {
                float rGain = -1.0f;
                float gGain = -1.0f;
                float bGain = -1.0f;
                String rgainStr = rGainInput.getText().toString();
                String ggainStr = gGainInput.getText().toString();
                String bgainStr = bGainInput.getText().toString();
                if (rgainStr.length() > 0) {
                    try {
                        rGain = Float.parseFloat(rgainStr);
                    } catch(NumberFormatException e) {
                        Log.w(TAG, "rGainInput type incorrect value ");
                    }
                }
                if (ggainStr.length() > 0) {
                    try {
                        gGain = Float.parseFloat(ggainStr);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "gGainInput type incorrect value ");
                    }
                }
                if (bgainStr.length() > 0) {
                    try {
                        bGain = Float.parseFloat(bgainStr);
                    } catch(NumberFormatException e) {
                        Log.w(TAG, "bGainInput type incorrect value ");
                    }
                }
                if (gainsRange == null) {
                    RotateTextToast.makeText(SettingsActivity.this, "Gains Range is NULL, " +
                            "Invalid gains", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rGain <= gainsRange[1] && rGain >= gainsRange[0]) {
                    Log.v(TAG, "Setting rGain value : " + rGain);
                    editor.putFloat(SettingsManager.KEY_MANUAL_WB_R_GAIN, rGain);
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid rGain value:",
                            Toast.LENGTH_SHORT).show();
                }
                if (gGain <= gainsRange[1] && gGain >= gainsRange[0]) {
                    Log.v(TAG, "Setting gGain value : " + gGain);
                    editor.putFloat(SettingsManager.KEY_MANUAL_WB_G_GAIN, gGain);
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid gGain value:",
                            Toast.LENGTH_SHORT).show();
                }
                if (bGain <= gainsRange[1] && bGain >= gainsRange[0]) {
                    Log.v(TAG, "Setting bGain value : " + bGain);
                    editor.putFloat(SettingsManager.KEY_MANUAL_WB_B_GAIN, bGain);
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid bGain value:",
                            Toast.LENGTH_SHORT).show();
                }
                editor.apply();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                editor.putString(SettingsManager.KEY_MANUAL_WB, "off");
                editor.apply();
                dialog.cancel();
            }
        });
        alert.show();
    }

    private void updateManualWBSettings() {
        int cameraId = mSettingsManager.getCurrentCameraId();
        SharedPreferences.Editor editor = mLocalSharedPref.edit();
        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        LinearLayout linear = new LinearLayout(SettingsActivity.this);
        linear.setOrientation(1);
        alert.setTitle("Manual White Balance Settings");
        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });

        String cctMode = this.getString(
                R.string.pref_camera_manual_wb_value_color_temperature);
        String rgbGainMode = this.getString(
                R.string.pref_camera_manual_wb_value_rbgb_gains);
        String currentWBTemp = mLocalSharedPref.getString(
                SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, "-1");
        final String manualWBMode = mSettingsManager.getValue(SettingsManager.KEY_MANUAL_WB);
        Log.v(TAG, "manualWBMode selected = " + manualWBMode);
        final int[] wbRange = mSettingsManager.getWBColorTemperatureRangeValues(cameraId);
        if (manualWBMode.equals(cctMode)) {
            final TextView CCTtext = new TextView(SettingsActivity.this);
            final EditText CCTinput = new EditText(SettingsActivity.this);
            CCTinput.setInputType(InputType.TYPE_CLASS_NUMBER);

            //refresh camera parameters to get latest CCT value
            if (currentWBTemp.equals("-1")) {
                CCTtext.setText(" Current CCT is ");
            } else {
                CCTtext.setText(" Current CCT is " + currentWBTemp);
            }
            if (wbRange == null) {
                alert.setMessage("Enter CCT value is get NULL ");
            } else {
                alert.setMessage("Enter CCT value in the range of " + wbRange[0]+ " to " + wbRange[1]);
            }
            linear.addView(CCTinput);
            linear.addView(CCTtext);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newCCT = -1;
                    String cct = CCTinput.getText().toString();
                    if (cct.length() > 0) {
                        try {
                            newCCT = Integer.parseInt(cct);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "CCTinput type incorrect value ");
                        }
                    }
                    if (wbRange == null) {
                        RotateTextToast.makeText(SettingsActivity.this, "CCT Range is NULL, " +
                                        "Invalid CCT", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newCCT <= wbRange[1] && newCCT >= wbRange[0]) {
                        Log.v(TAG, "Setting CCT value : " + newCCT);
                        //0 corresponds to manual CCT mode
                        editor.putString(SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, cct);
                        editor.apply();
                    } else {
                        RotateTextToast.makeText(SettingsActivity.this, "Invalid CCT",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    editor.putString(SettingsManager.KEY_MANUAL_WB, "off");
                    editor.apply();
                    dialog.cancel();
                }
            });
            alert.show();
        } else if (manualWBMode.equals(rgbGainMode)) {
            showManualWBGainDialog(linear, alert);
        } else {
            // user select off, nothing to do.
        }
    }

    private void updateToneMappingSettings() {
        Log.i(TAG,"updateToneMappingSettings");
        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        LinearLayout linear = new LinearLayout(SettingsActivity.this);
        linear.setOrientation(1);
        alert.setTitle("TONE MAPPING Settings");
        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });

        String offMode = this.getString(R.string.pref_camera_tone_mapping_value_off);
        String userSettingMode = this.getString(R.string.pref_camera_tone_mapping_value_user_setting);

        final String toneMappingMode = mSettingsManager.getValue(SettingsManager.KEY_TONE_MAPPING);

        Log.v(TAG, "toneMappingMode selected = " + toneMappingMode);
        if (!offMode.equals(toneMappingMode) && !userSettingMode.equals(toneMappingMode)) {
            showToneMappingDialog(linear, alert, toneMappingMode);
        } else if(userSettingMode.equals(toneMappingMode)){
            showToneMappingUserSettingDialog(linear, alert);
        }
    }
    private void showToneMappingUserSettingDialog(LinearLayout linear, AlertDialog.Builder alert){
        SharedPreferences.Editor editor = mLocalSharedPref.edit();
        final TextView darkBoostText = new TextView(SettingsActivity.this);
        final TextView darkBoostValue = new TextView(SettingsActivity.this);
        final EditText darkBoostInput = new EditText(SettingsActivity.this);
        final TextView fourthToneText = new TextView(SettingsActivity.this);
        final TextView fourthToneValue = new TextView(SettingsActivity.this);
        final EditText fourthToneInput = new EditText(SettingsActivity.this);
        int floatType = InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER;
        darkBoostInput.setInputType(floatType);
        fourthToneInput.setInputType(floatType);

        float darkBoost = mLocalSharedPref.getFloat(SettingsManager.KEY_TONE_MAPPING_DARK_BOOST, -1.0f);
        float fourthTone = mLocalSharedPref.getFloat(SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE, -1.0f);
        if (darkBoost == -1.0) {
            darkBoostValue.setText(" Current Dark boost offset is " );
        } else {
            darkBoostValue.setText(" Current Dark boost offset is " + darkBoost);
        }
        if (fourthTone == -1.0) {
            fourthToneValue.setText(" Current Fourth tone anchor is " );
        } else {
            fourthToneValue.setText(" Current Fourth tone anchor is " + fourthTone);
        }
        final float[] toneMappingRange = {0.0f, 1.0f};
        alert.setMessage("Enter tone mapping value in the range of " + toneMappingRange[0]+ " to " + toneMappingRange[1]);
        linear.addView(darkBoostText);
        linear.addView(darkBoostInput);
        linear.addView(darkBoostValue);
        linear.addView(fourthToneText);
        linear.addView(fourthToneInput);
        linear.addView(fourthToneValue);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog, int id) {
                float darkBoost = -1.0f;
                float fourthTone = -1.0f;
                String darkBoostStr = darkBoostInput.getText().toString();
                String fourthToneStr = fourthToneInput.getText().toString();
                if (darkBoostStr.length() > 0) {
                    try {
                        darkBoost = Float.parseFloat(darkBoostStr);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "darkBoostInput type incorrect value ");
                    }
                }
                if (fourthToneStr.length() > 0) {
                    try {
                        fourthTone = Float.parseFloat(fourthToneStr);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "fourthToneInput type incorrect value ");
                    }
                }

                if (darkBoost <= toneMappingRange[1] && darkBoost >= toneMappingRange[0]) {
                    Log.v(TAG, "Setting darkBoost value : " + darkBoost);
                    editor.putFloat(SettingsManager.KEY_TONE_MAPPING_DARK_BOOST, darkBoost);
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid darkBoost value:",
                            Toast.LENGTH_SHORT).show();
                }
                if (fourthTone <= toneMappingRange[1] && fourthTone >= toneMappingRange[0]) {
                    Log.v(TAG, "Setting fourthTone value : " + fourthTone);
                    editor.putFloat(SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE, fourthTone);
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid fourthTone value:",
                            Toast.LENGTH_SHORT).show();
                }
                editor.apply();
            }
        });
        alert.show();
    }

    private void updateMFNRPreference() {
        ListPreference mfnrPref = (ListPreference)findPreference(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
        String longshotValue = mSettingsManager.getValue(SettingsManager.KEY_LONGSHOT);
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (longshotValue.equals("on") &&(!isPrefEnabled(SettingsManager.KEY_BURST_LIMIT) ||
                mode == CaptureModule.CameraMode.RTB ) ) {
            if (mfnrPref != null) {
                mfnrPref.setValue("0");
            } else {
                mSettingsManager.setKeyValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE, true, "0");
            }
        } else {
            if (mfnrPref != null) {
                mfnrPref.setValue("1");
            } else {
                mSettingsManager.setKeyValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE, true, "1");
            }
        }
    }

    private void showToneMappingDialog(LinearLayout linear, AlertDialog.Builder alert, String mode){
        SharedPreferences.Editor editor = mLocalSharedPref.edit();
        final TextView toneMappingText = new TextView(SettingsActivity.this);
        final TextView toneMappingValue = new TextView(SettingsActivity.this);
        final EditText toneMappingInput = new EditText(SettingsActivity.this);
        int floatType = InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER;
        toneMappingInput.setInputType(floatType);
        float currentToneValue = -1.0f;
        String darkBoost = this.getString(R.string.pref_camera_tone_mapping_value_dark_boost_offset);
        String fourthTone = this.getString(R.string.pref_camera_tone_mapping_value_fourth_tone_anchor);
        String toastString = "tone mapping";

        if (mode.equals(darkBoost)) {
            currentToneValue = mLocalSharedPref.getFloat(
                    SettingsManager.KEY_TONE_MAPPING_DARK_BOOST, -1.0f);
            toastString = this.getString(R.string.pref_camera_tone_mapping_entry_dark_boost_offset);
        } else if (mode.equals(fourthTone)) {
            currentToneValue = mLocalSharedPref.getFloat(
                    SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE, -1.0f);
            toastString = this.getString(R.string.pref_camera_tone_mapping_entry_fourth_tone_anchor);
        }

        if (currentToneValue == -1.0) {
            toneMappingValue.setText(" Current " + toastString + " is " );
        } else {
            toneMappingValue.setText(" Current " + toastString + " is " + currentToneValue);
        }

        final float[] toneMappingRange = {0.0f, 1.0f};
        alert.setMessage("Enter " + toastString+ " in the range of " + toneMappingRange[0]+ " to " + toneMappingRange[1]);
        linear.addView(toneMappingText);
        linear.addView(toneMappingInput);
        linear.addView(toneMappingValue);
        alert.setView(linear);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog, int id) {
                float toneMapping = -1.0f;
                String toneMappingStr = toneMappingInput.getText().toString();
                if (toneMappingStr.length() > 0) {
                    toneMapping = Float.parseFloat(toneMappingStr);
                }

                if (toneMapping <= toneMappingRange[1] && toneMapping >= toneMappingRange[0]) {
                    Log.v(TAG, "Setting toneMapping value : " + toneMapping);
                    if (mode.equals(darkBoost)) {
                        final String key = SettingsManager.KEY_TONE_MAPPING_DARK_BOOST;
                        editor.putFloat(key, toneMapping);
                    } else if (mode.equals(fourthTone)) {
                        final String key = SettingsManager.KEY_TONE_MAPPING_FOURTH_TONE;
                        editor.putFloat(key, toneMapping);
                    }
                    editor.apply();
                } else {
                    RotateTextToast.makeText(SettingsActivity.this, "Invalid toneMapping value:",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        alert.show();
    }

    private void updateManualHDRSetting() {
        List<String> listData = new ArrayList<String>();
        int[] modes = mSettingsManager.isManualHDRSupported();
        StringBuilder defaultHDROrder = new StringBuilder();
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        final SharedPreferences.Editor editor = mLocalSharedPref.edit();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == 1) {
                listData.add(SettingsManager.KEY_MANUAL_SHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_SHDR).append("#");
            } else if (modes[i] == 2 && !mSettingsManager.isAIBokehMode()) {
                listData.add(SettingsManager.KEY_MANUAL_MFHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_MFHDR).append("#");
            } else if (modes[i] == 3) {
                listData.add(SettingsManager.KEY_MANUAL_QHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_QHDR);
            }
        }
        String videoSizeStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        int videoSize = CameraUtil.getSize(videoSizeStr);
        if(mSettingsManager.isHvxMFHDRSupported()) {
            if(mIsSingleCameraMode && mode == VIDEO && videoSize <= 1920*1080) {
                listData.add(SettingsManager.KEY_MANUAL_HVX_MFHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_HVX_MFHDR);
            }else {
                editor.putBoolean(SettingsManager.KEY_MANUAL_HVX_MFHDR, false);
                editor.commit();
            }
        }
        if(mSettingsManager.isHvxShdrSupported()) {
            if(mIsSingleCameraMode && mode == DEFAULT) {
                listData.add(SettingsManager.KEY_MANUAL_HVX_SHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_HVX_SHDR);
            }else{
                editor.putBoolean(SettingsManager.KEY_MANUAL_HVX_SHDR, false);
                editor.commit();
            }
        }
        String orderLists = mLocalSharedPref.getString(SettingsManager.KEY_MIXED_HDR_ORDER, null);
        Log.v(TAG, " updateManualHDRSetting orderLists:" + orderLists);
        final DragonListView listView = new DragonListView(SettingsActivity.this);
        DragListViewAdapter adapter = new DragListViewAdapter(this, listData);
        listView.setAdapter(adapter);
        adapter.setChecked(new CheckBoxChanged() {
            @Override
            public void onCheckedChanged(int position, String title, boolean isChecked) {
                Log.v(TAG, " save title :" + title + ", isChecked :" + isChecked + ", position :" + position);
                editor.putBoolean(title, isChecked);
                editor.commit();
                updateHdrRefOp();
                mSettingsManager.updatePictureAndVideoSize();
                updatePreference(SettingsManager.KEY_PICTURE_SIZE);
            }
        });

        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        alert.setTitle("MANUAL HDR Settings");
        alert.setView(listView);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface Dialog, int id) {
            }
        });
        alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface Dialog) {
                List<String> listData = adapter.getmDragDatas();
                StringBuilder mixedHDROrder = new StringBuilder();
                for (String item : listData) {
                    mixedHDROrder.append(item);
                    mixedHDROrder.append("#");
                }
                Log.v(TAG, " onDismiss mixedHDROrder:" + mixedHDROrder.toString());
                editor.putString(SettingsManager.KEY_MIXED_HDR_ORDER, mixedHDROrder.toString());
                editor.apply();
            }
        });
        alert.show();
    }
    private void updateHdrRefOp(){
        CaptureModule.CameraMode mode =
                    (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.VIDEO){
            mSettingsManager.filterHFROptions();
            updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
            updateVideoVariableFpsPreference();
            updateVideoHfrFpsPreference();
            updateInSensorZoom();
            updateViullPreference();
        }else if (mode == CaptureModule.CameraMode.DEFAULT){
            updateRawFormatPref();
            updateInSensorZoom();
            updateViullPreference();
            updateQuadBayerPreference();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window window = getWindow();
        window.setFlags(flag, flag);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getResources().getString(R.string.settings_title));
        }
        final boolean isSecureCamera = getIntent().getBooleanExtra(
                CameraUtil.KEY_IS_SECURE_CAMERA, false);
        if (isSecureCamera) {
            setShowInLockScreen();
        }
        mCharacteristics = new ArrayList<>();
        initCharacteristics();
        mIsSingleCameraMode = getIntent().getBooleanExtra(IS_SIGNGLE_CAMERA_MODULE, false);
        mShowAllDevOption =  getIntent().getBooleanExtra(OPEN_DEVOPTION, false);
        mSettingsManager = SettingsManager.getInstance();
        if (mSettingsManager == null) {
            finish();
            return;
        }

        int cameraId = mSettingsManager.getCurrentCameraId();
        mLocalSharedPref = this.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(this,
                        mSettingsManager.getCurrentPrepNameKey()), Context.MODE_PRIVATE);
        mSettingsManager.registerListener(mListener);
        addPreferencesFromResource(R.xml.setting_menu_preferences);
        mSharedPreferences = getPreferenceManager().getSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        mDeveloperMenuEnabled = mSharedPreferences.getBoolean(SettingsManager.KEY_DEVELOPER_MENU, false);
        mDeveloperMenuEnabled = mDeveloperMenuEnabled || mShowAllDevOption;
        filterPreferences();
        initializePreferences(false);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(i);
            for (int j = 0; j < category.getPreferenceCount(); j++) {
                Preference pref = category.getPreference(j);
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!mDeveloperMenuEnabled) {
                            if (preference.getKey().equals("version_info")) {
                                privateCounter++;
                                if (privateCounter >= DEVELOPER_MENU_TOUCH_COUNT) {
                                    mDeveloperMenuEnabled = true;
                                    mSharedPreferences.edit().putBoolean(SettingsManager.KEY_DEVELOPER_MENU, true).apply();
                                    SharedPreferences sp = SettingsActivity.this.getSharedPreferences(
                                            ComboPreferences.getGlobalSharedPreferencesName(SettingsActivity.this),
                                            Context.MODE_PRIVATE);
                                    sp.edit().putBoolean(SettingsManager.KEY_DEVELOPER_MENU, true).apply();
                                    Toast.makeText(SettingsActivity.this, "Camera developer option is enabled now", Toast.LENGTH_SHORT).show();
                                    recreate();
                                }
                            } else {
                                privateCounter = 0;
                            }
                        }

                        if ( preference.getKey().equals(SettingsManager.KEY_RESTORE_DEFAULT) ) {
                            onRestoreDefaultSettingsClick();
                        }
                        if( preference.getKey().equals(SettingsManager.KEY_FD_SETTING)) {
                            View listView = (SettingsActivity.this).getLayoutInflater().inflate(
                                    R.layout.expandlistview, null);
                            final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                            alert.setTitle("FD Settings");
                            alert.setView(listView);
                            expandableListView = (ExpandableListView) listView.findViewById(R.id.main_expandablelistview);
                            expandableAdapter = fdExpandListView.new FdExpandListViewAdapter();
                            expandableListView.setAdapter(expandableAdapter);
                            alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });
                            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface Dialog) {
                                    preference.setSummary(fdExpandListView.getFDSummery());
                                }
                            });
                            alert.show();
                        }

                        if( preference.getKey().equals(SettingsManager.KEY_FD_FL_SETTING)) {
                            View listView = (SettingsActivity.this).getLayoutInflater().inflate(
                                    R.layout.expandlistview, null);
                            final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                            alert.setTitle("FD FL Attributes");
                            alert.setView(listView);
                            fdFLExpandableListView = (ExpandableListView) listView.findViewById(R.id.main_expandablelistview);
                            fdFLExpandableAdapter = fdFLExpandListView.new FdExpandListViewAdapter();
                            fdFLExpandableListView.setAdapter(fdFLExpandableAdapter);
                            alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });
                            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface Dialog) {
                                    preference.setSummary(fdFLExpandListView.getFDSummery());
                                }
                            });
                            alert.show();
                        }

                        if( preference.getKey().equals(SettingsManager.KEY_FD_FACIAL_SETTING)) {
                            View listView = (SettingsActivity.this).getLayoutInflater().inflate(
                                    R.layout.expandlistview, null);
                            final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                            alert.setTitle("FD Facial Attributes");
                            alert.setView(listView);
                            fdFacialExpandableListView = (ExpandableListView) listView.findViewById(R.id.main_expandablelistview);
                            fdFacialExpandableAdapter = fdFacialExpandListView.new FdExpandListViewAdapter();
                            fdFacialExpandableListView.setAdapter(fdFacialExpandableAdapter);
                            alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });
                            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface Dialog) {
                                    preference.setSummary(fdFacialExpandListView.getFDSummery());
                                }
                            });
                            alert.show();
                        }

                        if (preference.getKey().equals(SettingsManager.KEY_MANUAL_HDR)) {
                            String value = ((ListPreference) preference).getValue();
                            if (value.equals("manual")) {
                                updateManualHDRSetting();
                            }
                        }

                        return false;
                    }

                });
            }
        }
    }

    private void initCharacteristics(){
        if(mCharacteristics.size() >0) {
            return;
        }
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            Log.i(TAG,"cameraIdList size ="+cameraIdList.length);
            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);
                mCharacteristics.add(i, characteristics);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG,e.toString());
        }
    }

    private Set<ColorSpace.Named> getSupportedColorSpaces(int cameraId, int imageFormat,
            long previewProfile, long captureProfile) {
        ColorSpaceProfiles colorSpaceProfiles = mCharacteristics.get(cameraId).get(
                CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES);
        Log.v(TAG, " getSupportedColorSpaces imageFormat :" + imageFormat + ", previewProfile :" + previewProfile + ", captureProfile :" + captureProfile);
        if (colorSpaceProfiles == null) {
            return new ArraySet<ColorSpace.Named>();
        }
        Set<ColorSpace.Named> colorSpaces = new ArraySet<ColorSpace.Named>();
        if (captureProfile == 0) {
            colorSpaces = colorSpaceProfiles.getSupportedColorSpaces(imageFormat);
            Log.v(TAG, " capture colorSpaces :" + colorSpaces);
        } else {
            colorSpaces = colorSpaceProfiles.getSupportedColorSpacesForDynamicRange(imageFormat, captureProfile);
            Log.v(TAG, " capture ForDynamicRange :" + colorSpaces);
        }
        if (colorSpaces.size() != 0) {
            if (previewProfile == 0) {
                colorSpaces = colorSpaceProfiles.getSupportedColorSpaces(imageFormat);
                Log.v(TAG, " previewProfile colorSpaces :" + colorSpaces);
            } else {
                colorSpaces = colorSpaceProfiles.getSupportedColorSpacesForDynamicRange(imageFormat, previewProfile);
                Log.v(TAG, " preview ForDynamicRange :" + colorSpaces);
            }
        }
        return colorSpaces;
    }

    private void filterPreferences() {
        String[] categories = {"photo", "video", "general", "developer"};
        Set<String> set = mSettingsManager.getFilteredKeys();
        if (!mDeveloperMenuEnabled) {
            if (set != null) {
                set.add(SettingsManager.KEY_MONO_PREVIEW);
                set.add(SettingsManager.KEY_MONO_ONLY);
                set.add(SettingsManager.KEY_CLEARSIGHT);
            }

            PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
            //Before restore settings,if current is not developer mode,the developer
            // preferenceGroup has been removed when enter camera by default .So duplicate remove
            // it will cause crash.
            if (developer != null) {
                PreferenceScreen parent = getPreferenceScreen();
                parent.removePreference(developer);
            }
        }

        CharSequence[] entries = mSettingsManager.getEntries(SettingsManager.KEY_SCENE_MODE);
        if (entries != null) {
            List<CharSequence> list = Arrays.asList(entries);
            if (mDeveloperMenuEnabled && list != null && !list.contains("HDR")){
                Preference p = findPreference("pref_camera2_hdr_key");
                if (p != null){
                    PreferenceGroup developer = (PreferenceGroup)findPreference("developer");
                    developer.removePreference(p);
                }
            }
        }

        if (set != null) {
            for (String key : set) {
                Preference p = findPreference(key);
                if (p == null) continue;

                for (int i = 0; i < categories.length; i++) {
                    PreferenceGroup group = (PreferenceGroup) findPreference(categories[i]);
                    if (group != null && group.removePreference(p)) break;
                }
            }
        }

        if (!mDeveloperMenuEnabled) {
            Preference p = findPreference(SettingsManager.KEY_PERFORMANCE_DEBUG);
            if (p != null){
                PreferenceGroup general = (PreferenceGroup)findPreference("general");
                general.removePreference(p);
            }
        }

        final ArrayList<String> videoOnlyList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_EIS_VALUE);
                add(SettingsManager.KEY_FOVC_VALUE);
                add(SettingsManager.KEY_VARIABLE_FPS);
                //add(SettingsManager.KEY_VIDEO_HDR_VALUE);
                add(SettingsManager.KEY_VIDEO_FLIP);
                add(SettingsManager.KEY_PHYSICAL_CAMCORDER);
                add(SettingsManager.KEY_OFFLINE_DUMP_TRIGGER);
                for (String key: SettingsManager.KEY_PHYSICAL_VIDEO_SIZE)
                    add(key);
                add(SettingsManager.KEY_AUDIO_RECORDING_MODE);
                add(SettingsManager.KEY_HDR_WNR_MODE);
                add(SettingsManager.KEY_HDR_ANS_MODE);
                add(SettingsManager.KEY_AI_CAMERA_BLURMODE);
                add(SettingsManager.KEY_ML_VIDEO);
            }
        };
        final ArrayList<String> multiCameraSettingList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_SATURATION_LEVEL);
                add(SettingsManager.KEY_ANTI_BANDING_LEVEL);
                add(SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
                add(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
                add(SettingsManager.KEY_AUTO_HDR);
                add(SettingsManager.KEY_MANUAL_EXPOSURE);
                add(SettingsManager.KEY_SHARPNESS_CONTROL_MODE);
                add(SettingsManager.KEY_AF_MODE);
                add(SettingsManager.KEY_EXPOSURE_METERING_MODE);
                add(SettingsManager.KEY_ABORT_CAPTURES);
                add(SettingsManager.KEY_INSTANT_AEC);
                add(SettingsManager.KEY_MANUAL_WB);
                add(SettingsManager.KEY_AF_MODE);
                add(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
                add(SettingsManager.KEY_FACE_DETECTION_MODE);
                add(SettingsManager.KEY_FD_SMILE);
                add(SettingsManager.KEY_FD_GAZE);
                add(SettingsManager.KEY_FD_BLINK);
                add(SettingsManager.KEY_FACIAL_CONTOUR);
                add(SettingsManager.KEY_ZSL);
                add(SettingsManager.KEY_SNAPSHOT_HDRMODE);
                add(SettingsManager.KEY_TONE_MAPPING);
                add(SettingsManager.KEY_ONCAPTUREBUFFERLOST_HINT);
                add(SettingsManager.KEY_BURST_LIMIT);
            }
        };
        final ArrayList<String> proModeOnlyList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_EXPOSURE_METERING_MODE);
            }
        };

        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        PreferenceGroup photoPre = (PreferenceGroup) findPreference("photo");
        PreferenceGroup videoPre = (PreferenceGroup) findPreference("video");
        PreferenceScreen parentPre = getPreferenceScreen();

        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);

        boolean isSupportedT2T = mSettingsManager.isT2TSupported();
        if (mSettingsManager.getInitialCameraId() == CaptureModule.FRONT_ID || !isSupportedT2T) {
            removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, photoPre);
            removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, videoPre);
        }
        boolean isStatsNN = mSettingsManager.isStatsNNSupported();
        if (!isStatsNN) {
            removePreference(SettingsManager.KEY_STATSNN_CONTROL, photoPre);
        }
        Preference p = findPreference(SettingsManager.KEY_FD_SETTING);
        if(p != null) {
            fdExpandListView = new FdExpandListView(SettingsActivity.this);
            fdExpandListView.initFDSettingsData();
            p.setSummary(fdExpandListView.getFDSummery());
            removePreference(SettingsManager.KEY_FD_SMILE, developer);
            removePreference(SettingsManager.KEY_FD_GAZE, developer);
            removePreference(SettingsManager.KEY_FD_BLINK, developer);
            removePreference(SettingsManager.KEY_FACIAL_CONTOUR, developer);
            removePreference(SettingsManager.KEY_FACE_DETECTION_MODE, developer);
            removePreference(SettingsManager.KEY_FD_GENDER, developer);
            removePreference(SettingsManager.KEY_FD_FACE_EXPRESSION, developer);
        }
        Preference pdFL = findPreference(SettingsManager.KEY_FD_FL_SETTING);
        if(pdFL != null) {
            fdFLExpandListView = new FdExpandListView(SettingsActivity.this);
            fdFLExpandListView.initFDFLData();
            pdFL.setSummary(fdFLExpandListView.getFDSummery());
        }
        Preference fdFacial = findPreference(SettingsManager.KEY_FD_FACIAL_SETTING);
        if(fdFacial != null) {
            fdFacialExpandListView = new FdExpandListView(SettingsActivity.this);
            fdFacialExpandListView.initFDFacialData();
            fdFacial.setSummary(fdFacialExpandListView.getFDSummery());
        }
        if(!PersistUtil.isRawReprocessEnable() && developer != null){
            removePreference(SettingsManager.KEY_RAW_REPROCESS_TYPE, developer);
            removePreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS, developer);
            removePreference(SettingsManager.KEY_RAWINFO_TYPE, developer);
            removePreference(SettingsManager.KEY_RAWINFO_TYPE, developer);
        }

        if(!PersistUtil.isRawCbInfoSupported()&& developer != null){
            removePreference(SettingsManager.KEY_RAW_CB_INFO, developer);
        }
        removePreference(SettingsManager.KEY_VIDEO_HDR_VALUE, developer);
        switch (mode) {
            case DEFAULT:
                removePreferenceGroup("video", parentPre);
                if (mDeveloperMenuEnabled && developer != null) {
                    removePreference(SettingsManager.KEY_CINEMATIC_DEBUG, developer);
                    removePreference(SettingsManager.KEY_STATSNN_CONTROL_FOR_CINEMATIC, developer);
                    if (!(DEV_LEVEL_ALL)) {
                        removePreference(SettingsManager.KEY_SWITCH_CAMERA, developer);
                    }
                    for (String removeKey : videoOnlyList) {
                        removePreference(removeKey, developer);
                    }
                    if (!PersistUtil.isMultiResolutionImageReaderEnabled() ||
                            !mSettingsManager.isMultiResolutionSupported()) {
                        removePreference(SettingsManager.KEY_MULTIRESIMAGEREADER, developer);
                        removePreference(SettingsManager.KEY_MULTIRESREPROCESS, developer);
                        removePreference(SettingsManager.KEY_MULTIRESREPROCESS_INPUT, developer);
                        removePreference(SettingsManager.KEY_MULTIRESREPROCESS_OUTPUT, developer);
                    } else {
                        updateMultiResolutionRealted();
                    }
                }
                break;
            case VIDEO:
            case HFR:
                removePreferenceGroup("photo", parentPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> videoAddList = new ArrayList<>();
                    videoAddList.add(SettingsManager.KEY_ZOOM);
                    if (DEV_LEVEL_ALL) {
                        videoAddList.add(SettingsManager.KEY_SWITCH_CAMERA);
                    }
                    videoAddList.addAll(videoOnlyList);
                    videoAddList.add(SettingsManager.KEY_ANTI_BANDING_LEVEL);
                    if (mode == VIDEO) {
                        videoAddList.add(SettingsManager.KEY_FD_SMILE);
                        videoAddList.add(SettingsManager.KEY_FD_GAZE);
                        videoAddList.add(SettingsManager.KEY_FD_BLINK);
                        videoAddList.add(SettingsManager.KEY_FACE_DETECTION_MODE);
                        videoAddList.add(SettingsManager.KEY_FACIAL_CONTOUR);
                        videoAddList.add(SettingsManager.KEY_FD_SETTING);
                        videoAddList.add(SettingsManager.KEY_FD_FL_SETTING);
                        videoAddList.add(SettingsManager.KEY_FD_FACIAL_SETTING);
                        videoAddList.add(SettingsManager.KEY_EIS_HORIZON_LEVEL_ENABLE);
                        videoAddList.add(SettingsManager.KEY_MULTI_CAMERA_MODE);
                        videoAddList.add(SettingsManager.KEY_PHYSICAL_CAMERA);
                        videoAddList.add(SettingsManager.KEY_MANUAL_HDR);
                        videoAddList.add(SettingsManager.KEY_VSR);
                        videoAddList.add(SettingsManager.KEY_ONCAPTUREBUFFERLOST_HINT);
                        if (PersistUtil.enableMediaRecorder()) {
                            videoAddList.remove(SettingsManager.KEY_VIDEO_FLIP);
                        }
                        videoAddList.add(SettingsManager.KEY_AI_CAMERA);
                        videoAddList.add(SettingsManager.KEY_AI_CAMERA_SNAPSHOT);
                        videoAddList.add(SettingsManager.KEY_PREVIEW_STABILIZATION);
                        videoAddList.add(SettingsManager.KEY_PREVIEW_PROFILE);
                        videoAddList.add(SettingsManager.KEY_SENSOR_MODE_FS2_VALUE);
                        videoAddList.add(SettingsManager.KEY_VIULL);
                        videoAddList.add(SettingsManager.KEY_INSENSOR_ZOOM);
                    } else {
                        videoAddList.add(SettingsManager.KEY_FD_SETTING);
                        videoAddList.remove(SettingsManager.KEY_AI_CAMERA_BLURMODE);
                        videoAddList.remove(SettingsManager.KEY_VARIABLE_FPS);
                        videoAddList.remove(SettingsManager.KEY_VIDEO_FLIP);
                        videoAddList.remove(SettingsManager.KEY_ML_VIDEO);
                    }
                    videoAddList.add(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
                    videoAddList.add(SettingsManager.KEY_TONE_MAPPING);
                    videoAddList.add(SettingsManager.KEY_SELECT_MODE);
                    if (isStatsNN) {
                        videoAddList.add(SettingsManager.KEY_STATSNN_CONTROL);
                    }
                    videoAddList.add(SettingsManager.KEY_PDNET_TOGGLE);

                    videoAddList.add(SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
                    videoAddList.add(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
                    videoAddList.add(SettingsManager.KEY_INSTANT_ZOOM);
                    videoAddList.add(SettingsManager.KEY_COLOR_SPACE);
                    addDeveloperOptions(developer, videoAddList);
                }
                if (mode != VIDEO) {
                    removePreference(SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL, videoPre);
                    if(mode == HFR && !mSettingsManager.isSupportedSuperBuffer(mSettingsManager.getCurrentCameraId())){
                        removePreference(SettingsManager.KEY_HFR_BUFFER_MODE, videoPre);
                    }
                    Preference p1 = findPreference(SettingsManager.KEY_PICTURE_FORMAT);
                    if (p1 != null){
                        PreferenceGroup general = (PreferenceGroup)findPreference("general");
                        general.removePreference(p1);
                    }
                }else {
                    removePreference(SettingsManager.KEY_HFR_BUFFER_MODE, videoPre);
                }
                break;
            case CINEMATIC:
                removePreferenceGroup("photo", parentPre);
                removePreference(SettingsManager.KEY_NOISE_REDUCTION, videoPre);
                removePreference(SettingsManager.KEY_VIDEO_ENCODER, videoPre);
                removePreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE, videoPre);
                removePreference(SettingsManager.KEY_AUDIO_ENCODER, videoPre);
                removePreference(SettingsManager.KEY_VIDEO_ROTATION, videoPre);
                removePreference(SettingsManager.KEY_VIDEO_DURATION, videoPre);
                removePreference(SettingsManager.KEY_PICTURE_FORMAT, videoPre);
                removePreference(SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL, videoPre);
                removePreference(SettingsManager.KEY_HFR_BUFFER_MODE, videoPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> cinematicList = new ArrayList<>();
                    cinematicList.add(SettingsManager.KEY_STATSNN_CONTROL_FOR_CINEMATIC);
                    cinematicList.add(SettingsManager.KEY_CINEMATIC_DEBUG);
                    cinematicList.add(SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
                    cinematicList.add(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
                    addDeveloperOptions(developer, cinematicList);
                }
                break;
            case RTB:
                removePreferenceGroup("video", parentPre);
                removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, photoPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> RTBList = new ArrayList<>(multiCameraSettingList);
                    RTBList.add(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
                    RTBList.add(SettingsManager.KEY_INSENSOR_ZOOM);
                    RTBList.add(SettingsManager.KEY_FD_SETTING);
                    RTBList.add(SettingsManager.KEY_FD_FL_SETTING);
                    RTBList.add(SettingsManager.KEY_FD_FACIAL_SETTING);
                    RTBList.add(SettingsManager.KEY_INSTANT_ZOOM);
                    addDeveloperOptions(developer, RTBList);
                }
                break;
            case SAT:
                removePreferenceGroup("video", parentPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> SATList = new ArrayList<>(multiCameraSettingList);
                    SATList.add(SettingsManager.KEY_HDR);
                    SATList.add(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
                    SATList.add(SettingsManager.KEY_PDNET_TOGGLE);
                    addDeveloperOptions(developer, SATList);
                }
                break;
            case PRO_MODE:
                removePreferenceGroup("video", parentPre);
                removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, photoPre);
                if (mDeveloperMenuEnabled) {
                    if (DEV_LEVEL_ALL) {
                        proModeOnlyList.add(SettingsManager.KEY_SWITCH_CAMERA);

                    }
                    proModeOnlyList.add(SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
                    proModeOnlyList.add(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
                    proModeOnlyList.add(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
                    proModeOnlyList.add(SettingsManager.KEY_TONE_MAPPING);
                    proModeOnlyList.add(SettingsManager.KEY_QUAD_BAYER_SENSOR);
                    addDeveloperOptions(developer, proModeOnlyList);
                }
                break;
            default:
                //don't filter
                break;
        }
        Preference longshotPref = findPreference(SettingsManager.KEY_LONGSHOT);
        if (longshotPref != null && !mSettingsManager.isBurstShotSupported() && photoPre != null){
            photoPre.removePreference(longshotPref);
        }
    }

    private boolean removePreference(String key, PreferenceGroup parentPreferenceGroup) {
        Preference removePreference = findPreference(key);
        if (removePreference != null && parentPreferenceGroup != null) {
            parentPreferenceGroup.removePreference(removePreference);
            return true;
        }
        return false;
    }

    private boolean removePreferenceGroup(String key, PreferenceScreen parentPreferenceScreen) {
        PreferenceGroup removePreference = (PreferenceGroup) findPreference(key);
        if (removePreference != null && parentPreferenceScreen != null) {
            parentPreferenceScreen.removePreference(removePreference);
            return true;
        }
        return false;
    }

    private void addDeveloperOptions(PreferenceGroup developer, List<String> keyList) {
        if (developer == null) {
            Log.d(TAG, "can't find developer PreferenceGroup");
            return;
        }
        ArrayList<Preference> addList = new ArrayList<>();
        for (String key : keyList) {
            Preference p = findPreference(key);
            if (p != null) {
                addList.add(p);
            } else {
                Log.d(TAG, "can't find key " + key);
            }
        }
        developer.removeAll();
        for (Preference addItem : addList) {
            developer.addPreference(addItem);
        }
    }

    private void updateMultiResolutionRealted() {
        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        final ArrayList<String> multiResList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_MULTIRESIMAGEREADER);
                add(SettingsManager.KEY_ZSL);
                add(SettingsManager.KEY_MULTIRESREPROCESS);
                add(SettingsManager.KEY_MULTIRESREPROCESS_INPUT);
                add(SettingsManager.KEY_MULTIRESREPROCESS_OUTPUT);
            }
        };
        if(mode == DEFAULT){
            addDeveloperOptions(developer, multiResList);
        }
    }

    private void updateMultiResReprocess(){
        ListPreference inpref = (ListPreference)findPreference(SettingsManager.KEY_MULTIRESREPROCESS_INPUT);
        ListPreference outpref = (ListPreference)findPreference(SettingsManager.KEY_MULTIRESREPROCESS_OUTPUT);
        String value = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESREPROCESS);
        ListPreference zslValue = (ListPreference)findPreference(SettingsManager.KEY_ZSL);
        if(inpref != null && outpref != null){
            if(value != null && "1".equals(value)) {
                inpref.setEnabled(true);
                outpref.setEnabled(true);
                mSettingsManager.updateMultiReprocessInputOutput();
                updatePreference(SettingsManager.KEY_MULTIRESREPROCESS_INPUT);
                updatePreference(SettingsManager.KEY_MULTIRESREPROCESS_OUTPUT);
            }else {
                inpref.setEnabled(false);
                outpref.setEnabled(false);
            }
        }
    }
    private void updateAICameraPerf(){
        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        ArrayList<String> aiCameraList = new ArrayList<String>();
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        ListPreference aiCamera = (ListPreference)findPreference(SettingsManager.KEY_AI_CAMERA);
        Log.d(TAG,"isAICameraOn:" + mSettingsManager.isAICameraOn() + ",selectMode: " + selectMode);
        if (selectMode.equals("rtb") && mode == VIDEO  &&
                mSettingsManager.getCurrentCameraId() != CaptureModule.FRONT_ID){
            if(aiCamera != null) {
                aiCamera.setValue("0");
                aiCamera.setEnabled(false);
            }
        }else{
            if(aiCamera != null) {
                aiCamera.setEnabled(true);
            }
        }
        if (selectMode.equals("rtb") && mode == VIDEO  &&
                mSettingsManager.getCurrentCameraId() == CaptureModule.FRONT_ID){
            aiCameraList.add(SettingsManager.KEY_AI_CAMERA);
            aiCameraList.add(SettingsManager.KEY_AI_CAMERA_SNAPSHOT);
            aiCameraList.add(SettingsManager.KEY_AI_CAMERA_BLURMODE);
            aiCameraList.add(SettingsManager.KEY_EIS_VALUE);
            aiCameraList.add(SettingsManager.KEY_MANUAL_HDR);
            aiCameraList.add(SettingsManager.KEY_SELECT_MODE);
            addDeveloperOptions(developer,aiCameraList);
        }
    }

    private void updatePhysicalPreferences() {
        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);

        final ArrayList<String> multiCameraPhotoList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_PHYSICAL_CAMERA);
                add(SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
                add(SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
                add(SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
                add(SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
                add(SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
                if(!mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    add(SettingsManager.KEY_PHYSICAL_HDR);
                    add(SettingsManager.KEY_PHYSICAL_MFNR);
                    add(SettingsManager.KEY_ZSL);
                }
                int i=0;
                int maxSize = 3;
                if(mSettingsManager.isMcxQcfaMode() && mSettingsManager.getQuadBayerPhysicalList() != null){
                    maxSize = mSettingsManager.getQuadBayerPhysicalList().size();
                }
                for(String id : SettingsManager.KEY_PHYSICAL_SIZE){
                    if(i < maxSize) {
                        add(id);
                        i++;
                    }
                }
                add(SettingsManager.KEY_STREAM_USECASE);
            }
        };

        final ArrayList<String> multiCameraVideoList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_PHYSICAL_CAMERA);
                add(SettingsManager.KEY_PHYSICAL_CAMCORDER);
                for(String id : SettingsManager.KEY_PHYSICAL_VIDEO_SIZE){
                    add(id);
                }
            }
        };

        if(mode == DEFAULT){
            if (mSettingsManager.isMultiCameraEnabled()){
                multiCameraPhotoList.add(SettingsManager.KEY_QUAD_BAYER_SENSOR);
                multiCameraPhotoList.add(SettingsManager.KEY_MULTI_CAMERA_MODE);
                multiCameraPhotoList.add(SettingsManager.KEY_PHOTO_EIS_VALUE);
                multiCameraPhotoList.add(SettingsManager.KEY_PREVIEW_STABILIZATION);
                addDeveloperOptions(developer,multiCameraPhotoList);
            } else {
                multiCameraPhotoList.remove(SettingsManager.KEY_ZSL);
                for (String removeKey : multiCameraPhotoList){
                    removePreference(removeKey,developer);
                }
            }
        } else if (mode == VIDEO){
            if (mSettingsManager.isMultiCameraEnabled()){
                multiCameraVideoList.add(SettingsManager.KEY_MULTI_CAMERA_MODE);
                multiCameraVideoList.add(SettingsManager.KEY_EIS_VALUE);
                multiCameraVideoList.add(SettingsManager.KEY_PREVIEW_STABILIZATION);
                addDeveloperOptions(developer,multiCameraVideoList);
            } else {
                for (String removeKey : multiCameraVideoList){
                    removePreference(removeKey,developer);
                }
            }
        }
    }

    private void initializePhysicalPreferences(){
        updatePreference(SettingsManager.KEY_SINGLE_PHYSICAL_CAMERA);
        updatePreference(SettingsManager.KEY_MULTI_CAMERA_MODE);
        updatePreference(SettingsManager.KEY_STREAM_USECASE);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_CAMERA);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_CAMCORDER);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_HDR);
        updateMultiPreference(SettingsManager.KEY_PHYSICAL_MFNR);
        Set<String> physicalIds = mSettingsManager.getAllPhysicalCameraId();
        if(mSettingsManager.isMcxQcfaMode()){
            physicalIds = mSettingsManager.getQuadBayerPhysicalList();
        }
        if (physicalIds != null){
            int i = 0;
            String imageSizeTitle = getResources().getString(
                    R.string.pref_camera2_physical_size_title);
            String videoSizeTitle = getResources().getString(
                    R.string.pref_camera2_physical_quality_title);
            for (String id : physicalIds){
                if (i >= CaptureModule.PHYSICAL_CAMERA_COUNT)
                    break;
                ListPreference photo = (ListPreference)
                        findPreference(SettingsManager.KEY_PHYSICAL_SIZE[i]);
                if (photo != null){
                    photo.setTitle(imageSizeTitle + " " + id);
                }
                ListPreference video = (ListPreference)
                        findPreference(SettingsManager.KEY_PHYSICAL_VIDEO_SIZE[i]);
                if (video != null){
                    video.setTitle(videoSizeTitle + " " + id);
                }
                updatePreference(SettingsManager.KEY_PHYSICAL_SIZE[i]);
                updatePreference(SettingsManager.KEY_PHYSICAL_VIDEO_SIZE[i]);
                i++;
            }
        }

    }

    private void initializePreferences(boolean fromRestore) {
        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
        updatePreference(SettingsManager.KEY_PICTURE_FORMAT);
        updatePreference(SettingsManager.KEY_EXPOSURE);
        updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        updatePreference(SettingsManager.KEY_VIDEO_ENCODER);
        updatePreference(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
        updatePreference(SettingsManager.KEY_SWITCH_CAMERA);
        updatePreference(SettingsManager.KEY_QUAD_BAYER_SENSOR);
        updatePreference(SettingsManager.KEY_TONE_MAPPING);
        updatePreference(SettingsManager.KEY_LIVE_PREVIEW);
        updatePreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS);
        updatePreference(SettingsManager.KEY_PREVIEW_PROFILE);
        updatePreference(SettingsManager.KEY_CAPTURE_PROFILE);
        updateMultiPreference(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
        updateVideoVariableFpsPreference();
        updateVideoMFHDRPreference();
        updateQuadBayerPreference();
        updateStoragePreference();
        initializePhysicalPreferences();
        updatePhysicalPreferences();
        updateAICameraPerf();
        updateVideoHfrFpsPreference();
        updateEISPreference();
        updateT2TPreference();
        updateZoomPreference();
        updatePreference(SettingsManager.KEY_VIDEO_DURATION);
        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
        updatePictureFormatPreference();
        updateLongShotPreference();
        updateHDRSceneDetection();
        updateQLLPreference();
        updateColorSpacePreference();
        Map<String, SettingsManager.Values> map = mSettingsManager.getValuesMap();
        if (map == null) return;
        Set<Map.Entry<String, SettingsManager.Values>> set = map.entrySet();

        for (Map.Entry<String, SettingsManager.Values> entry : set) {
            String key = entry.getKey();
            Preference p = findPreference(key);
            if (p == null) continue;

            SettingsManager.Values values = entry.getValue();
            boolean disabled = values.overriddenValue != null;
            String value = disabled ? values.overriddenValue : values.value;
            boolean enable = p.isEnabled();
            if (p instanceof SwitchPreference) {
                ((SwitchPreference) p).setChecked(isOn(value));
                ((SwitchPreference) p).setEnabled(enable && !disabled);
            } else if (p instanceof ListPreference) {
                ListPreference pref = (ListPreference) p;
                if (enable) {
                    pref.setEnabled(true);
                }
                pref.setValue(value);
                if (pref.getEntryValues().length == 1) {
                    pref.setEnabled(false);
                }
            }
            if (disabled) p.setEnabled(false);
        }
        // when enable deepzoom, disable the KEY_PICTURE_SIZE
        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
        if (scene != null) {
            int mode = Integer.parseInt(scene);
            if (mode == SettingsManager.SCENE_MODE_DEEPZOOM_INT) {
                Preference p = findPreference(SettingsManager.KEY_PICTURE_SIZE);
                p.setEnabled(false);
            } else if (mode != 18) {
                ListPreference pref = (ListPreference)findPreference(
                        SettingsManager.KEY_SNAPSHOT_HDRMODE);
                if (pref != null) {
                    pref.setEnabled(false);
                    pref.setValue("default");
                }
            }
        }
        // when get RAW10 size is null, disable the KEY_SAVERAW
        int cameraId = mSettingsManager.getCurrentCameraId();
        Size[] rawSize = mSettingsManager.getSupportedOutputSize(cameraId,
                mSettingsManager.getRawFormat());
        if (rawSize == null && mSettingsManager.getRawFormat() > 0 ) {
            Preference p = findPreference(SettingsManager.KEY_RAW_FORMAT_TYPE);
            if (p != null) {
                p.setEnabled(false);
            }
        }
        String reprocessType = mSettingsManager.getValue(SettingsManager.KEY_RAW_REPROCESS_TYPE);
        if (reprocessType == null || reprocessType.equals("disable") || reprocessType.equals("off") || Integer.valueOf(reprocessType) == 0 ) {
            Preference p = findPreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS);
            if (p != null) {
                p.setEnabled(false);
            }
        }
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int index = versionName.indexOf(' ');
            versionName = versionName.substring(0, index);
            findPreference("version_info").setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG,e.toString());
        }

        updateZslPreference();
        updateForceAUXPreference();
        updateVideoEncoderProfile();
        updateSwitchIDInModePreference(true);
        updateTimeLapsePreference();
        updateAudioEncoderPreference();
        updateVideoFlipPreference();
        updateAIDEPreference();
        updatePdnetTogglePreference();
        updateRawFormatPref();
        updateRawInfoPref();
        updatePictureSizePreferenceButton();
        updateVsrPreference();
        updateCaptureProfilePref();
        updateMultiResReprocess();
        updatePreviewStabilizationPreference();
        updateViullPreference();
        updateCinematicOptions(fromRestore);
        updateHfrBufferMode();
    }
    public void updateHfrBufferMode(){
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_HFR_BUFFER_MODE);
        if(pref == null){
            return;
        }
        if (mSettingsManager.isSupportedSuperBuffer(mSettingsManager.getCurrentCameraId())){
                pref.setEnabled(true);
            }else{
                pref.setEnabled(false);
                pref.setValue("0");
            }
    }
    private void updateAudioEncoderPreference() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_AUDIO_ENCODER);
        String hdr_mode = mSettingsManager.getValue(SettingsManager.KEY_AUDIO_RECORDING_MODE);
        if (pref == null) {
            return;
        }
        ListPreference multiCamerPref = (ListPreference)findPreference(
                SettingsManager.KEY_MULTI_CAMERA_MODE);
        if (multiCamerPref != null) {
            String enable = multiCamerPref.getValue();
            if (enable != null && enable.equals("1")) {
                pref.setEnabled(false);
                pref.setValue("aac");
                return;
            }
        }
        if (PersistUtil.enableMediaRecorder()) {
            if (hdr_mode.equals("hdr")) {
                pref.setEnabled(false);
                pref.setValue("aac");
            } else {
                pref.setEnabled(true);
            }
        } else {
            pref.setEnabled(false);
            pref.setValue("aac");
        }
    }

    private void updateStoragePreference() {
        boolean isWrite = SDCard.instance().isWriteable();
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_CAMERA_SAVEPATH);
        if (pref == null) {
            return;
        }
        pref.setEnabled(isWrite);
        if (!isWrite) {
            updatePreference(SettingsManager.KEY_CAMERA_SAVEPATH);
        }
    }

    private void updateVideoEncoderProfile() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
        if (pref == null) {
            return;
        }
        updatePreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
    }

    private void updateTimeLapsePreference() {
        ListPreference pref = (ListPreference)findPreference(
                SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        if (pref == null) {
            return;
        }

        ListPreference multiCamerPref = (ListPreference)findPreference(
                SettingsManager.KEY_MULTI_CAMERA_MODE);
        if (multiCamerPref != null) {
            String enable = multiCamerPref.getValue();
            if (enable != null && enable.equals("1")) {
                pref.setValue("0");
                pref.setEnabled(false);
                return;
            }
        }
        if (PersistUtil.enableMediaRecorder()) {
            pref.setEnabled(true);
        } else {
            pref.setValue("0");
            pref.setEnabled(false);
        }
    }

    private void updateSwitchIDInModePreference(boolean isShowRTB){
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_SELECT_MODE);
        List<String> key = new ArrayList<String>(Arrays.asList("Single rear cameraID", "SAT", "Default" ));
        List<String> value = new ArrayList<String>(Arrays.asList( "single_rear_cameraid", "sat", "default"));
        boolean isBack = false;
        String profile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
        if (pref != null) {
            CaptureModule.CameraMode mode =
                    (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
            if (mode == CaptureModule.CameraMode.VIDEO && isShowRTB && ( profile == null || (profile != null && !profile.equals("HEVCProfileMain10HDR10Plus")))) {
                key.add("RTB");
                value.add("rtb");
            }else if (mode == CaptureModule.CameraMode.HFR ){
                key.remove("SAT");
                value.remove("sat");
            }
            if (mSettingsManager.isAICameraOn() && mode == CaptureModule.CameraMode.VIDEO && mSettingsManager.getCurrentCameraId() != CaptureModule.FRONT_ID) {
                key.add("Single Rear AIbokeh");
                value.add("single_rear_aibokeh");
            }
            if((mSettingsManager.getCurrentCameraId() == CaptureModule.FRONT_ID || !CaptureModule.MCXMODE) && mode == CaptureModule.CameraMode.VIDEO){
                key = new ArrayList<String>(Arrays.asList("Default", "RTB"));
                value = new ArrayList<String>(Arrays.asList( "default", "rtb"));
            }
            pref.setEntries(key.toArray(new CharSequence[key.size()]));
            pref.setEntryValues(value.toArray(new CharSequence[value.size()]));
            int idx = pref.findIndexOfValue(pref.getValue());;
            if (idx < 0 ) {
                idx = 0;
            }
            String cameraValue = mSettingsManager.getValue(SettingsManager.KEY_FRONT_REAR_SWITCHER_VALUE);
            if (cameraValue != null && cameraValue.equals("rear")) isBack = true;
            boolean perfEnable = false;
            if((CaptureModule.MCXMODE && isBack && !mSettingsManager.getQuadBayerSensorPrefEnabled()) ||
                    ((mSettingsManager.getCurrentCameraId() == CaptureModule.FRONT_ID ||
                            !CaptureModule.MCXMODE) && mSettingsManager.isAICameraOn() && (mode == CaptureModule.CameraMode.VIDEO))){
                perfEnable = true;
            }
            if(!perfEnable){
                pref.setValue("default");
            } else {
                pref.setValueIndex(idx);
            }
            pref.setEnabled(perfEnable);
        }
    }

    public boolean isHwMfnrDisabled(){
        String value = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
        if(value != null &&  !value.equals("disable")&& Integer.parseInt(value) == 0 && mSettingsManager.isHWMFNRSupport()){
            return true;
        }
        return false;
    }

    private void updateAIDEPreference() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_AI_DENOISER);
        if (pref == null) {
            return;
        }
        if(isHwMfnrDisabled() || (mSettingsManager.isHWMFNRSupport() && !isHwMfnrDisabled() && !mSettingsManager.isAIDE2Supported())){
            pref.setEnabled(false);
        }
    }
    private void updateQuadBayerPreference(){
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_QUAD_BAYER_SENSOR);
        if (pref == null) {
            return;
        }
        pref.setEnabled(true);
    }
    private void updateQLLPreference() {
        ListPreference mixHDRPref = (ListPreference)findPreference(SettingsManager.KEY_MANUAL_HDR);
        ListPreference qllPref = (ListPreference)findPreference(SettingsManager.KEY_QLL);
        if (mixHDRPref != null && mixHDRPref.getValue().equals("auto")) {
            if (qllPref != null) {
                qllPref.setValue("0");
                qllPref.setEnabled(false);
            }
        } else {
            if (qllPref != null) {
                qllPref.setEnabled(true);
            }
        }
    }
    private void updateColorSpacePreference() {
        ListPreference colorSpacePref = (ListPreference)findPreference(SettingsManager.KEY_COLOR_SPACE);
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.VIDEO || mode == CaptureModule.CameraMode.HFR) {
            List<String> list = new ArrayList<String>(Arrays.asList("DISABLE", "SRGB"));
            List<String> values = new ArrayList<String>(Arrays.asList("0", "1"));
            if (colorSpacePref != null) {
                colorSpacePref.setEntries(list.toArray(new CharSequence[list.size()]));
                colorSpacePref.setEntryValues(values.toArray(new CharSequence[values.size()]));
            }
            ColorSpaceProfiles colorSpaceProfiles = mCharacteristics.get(mSettingsManager.getCurrentCameraId()).get(
                    CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES);
            if (colorSpaceProfiles == null && colorSpacePref != null) {
                 colorSpacePref.setValue("0");
                 colorSpacePref.setEnabled(false);
            }
        } else if (mode == CaptureModule.CameraMode.DEFAULT) {
            int cameraId = mSettingsManager.getCurrentCameraId();
            Set<ColorSpace.Named> colorSpaceSet = null;
            String format = mSettingsManager.getValue(SettingsManager.KEY_PICTURE_FORMAT);
            long captureProfileLong = 0;
            String captureProfile = mSettingsManager.getValue(SettingsManager.KEY_CAPTURE_PROFILE);
            if (captureProfile != null && !captureProfile.equals("0")) {
                captureProfileLong = Long.parseLong(captureProfile);
            }
            long previewProfileLong = 0;
            String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
            if (previewProfile != null && !previewProfile.equals("0")) {
                previewProfileLong = Long.parseLong(previewProfile);
            }
            if (format != null) {
                colorSpaceSet = getSupportedColorSpaces(cameraId,
                        SettingsManager.KEY_IMAGE_FORMAT_INDEX.get(format), previewProfileLong,
                        captureProfileLong);
            }

            String rawFormat = mSettingsManager.getValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
            Log.v(TAG, " format :" + format + ", rawFormat : " + rawFormat);

            if ((colorSpaceSet == null || colorSpaceSet.size() == 0) ||
                    (rawFormat != null && (rawFormat.equals("10") || rawFormat.equals("16")))) {
                if (colorSpacePref != null) {
                    colorSpacePref.setValue("0");
                    colorSpacePref.setEnabled(false);
                }
            } else {
                if (colorSpacePref != null) {
                    colorSpacePref.setEnabled(true);
                }
            }
        }

    }
    private void updateVideoMFHDRPreference() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_MANUAL_HDR);
        if (pref == null) {
            return;
        }
        int[] modes = mSettingsManager.isManualHDRSupported();
        pref.setEnabled(true);
        if (modes != null && modes.length >= 1 && mSettingsManager.getRawFormat() == 0) {
            pref.setEnabled(true);
            mSettingsManager.filterVideoMaunalHDRModes(modes);
            updatePreference(SettingsManager.KEY_MANUAL_HDR);
        } else {
            pref.setEnabled(false);
            pref.setValue("off");
            return;
        }
        ListPreference eisPref = (ListPreference)findPreference(SettingsManager.KEY_EIS_VALUE);
        if(mSettingsManager.isAIBokehMode()) {
            if (eisPref != null && eisPref.getValue() != null && eisPref.getValue().equals("disable")) {
                pref.setValue("off");
                pref.setEnabled(false);
                return;
            }
        }

        String videoSizeStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        String hdrmode = mSettingsManager.getVideoHdrMode();
        int videoSize = CameraUtil.getSize(videoSizeStr);
        if((videoSize >= 7680*4320 && hdrmode != null && (hdrmode.indexOf("MFHDR")>=0)) ||
                (videoSize > 1920*1080 && hdrmode != null && (hdrmode.indexOf("HVX_MFHDR")>=0))){
            pref.setValue("off");
        }
    }

    private void updateForceAUXPreference() {
        ListPreference forceAUX = (ListPreference)findPreference(SettingsManager.KEY_FORCE_AUX);
        if (forceAUX == null) return;
        String selectMode = mSettingsManager.getValue(SettingsManager.KEY_SELECT_MODE);
        if ((selectMode != null && selectMode.equals("single_rear_cameraid")) || mSettingsManager.getQuadBayerSensorPrefEnabled()) {
            forceAUX.setValue("off");
            forceAUX.setEnabled(false);
        } else {
            forceAUX.setEnabled(true);
        }
    }

    private void updateMultiVideoFPSPreference() {
        boolean changeFPS = true;
        MultiSelectListPreference physicalCameraPref = (MultiSelectListPreference) findPreference(
                SettingsManager.KEY_PHYSICAL_CAMERA);
        if (physicalCameraPref != null) {
            Set<String> physicalCameraSet = physicalCameraPref.getValues();
            if (physicalCameraSet != null) {
                for (String str : physicalCameraSet) {
                    if ("logical".equals(str)) {
                        changeFPS &= true;
                    } else {
                        if(!"".equals(str)) {
                            changeFPS &= false;
                        }
                    }
                }
            }
        }
        MultiSelectListPreference camCorderPref = (MultiSelectListPreference) findPreference(
                SettingsManager.KEY_PHYSICAL_CAMCORDER);
        if (camCorderPref != null) {
            Set<String> camCorderSet = camCorderPref.getValues();
            if (camCorderSet != null) {
                for (String str : camCorderSet) {
                    if (!"".equals(str)) {
                        changeFPS &= false;
                    }
                }
            }
        }
        ListPreference pref = (ListPreference)findPreference(
                SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (pref != null) {
            if (changeFPS) {
                pref.setEnabled(true);
            } else {
                pref.setValue("off");
                pref.setEnabled(false);
            }
        }
    }

    public void updateVideoFlipPreference() {
        if (PersistUtil.enableMediaRecorder()) {
            return;
        }
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_VIDEO_QUALITY);
        ListPreference flipPref = (ListPreference)findPreference(SettingsManager.KEY_VIDEO_FLIP);
        if (pref != null && flipPref != null) {
            String videoSize = pref.getValue();
            boolean enabled = false;
            if (videoSize != null) {
                int indexX = videoSize.indexOf('x');
                int width = Integer.parseInt(videoSize.substring(0, indexX));
                int height = Integer.parseInt(videoSize.substring(indexX + 1));
                if (width <= 1920 && height <= 1080) {
                    enabled = true;
                }
            }
            flipPref.setEnabled(enabled);
        }
    }

    private void updateVideoHfrFpsPreference() {
        ListPreference pref = (ListPreference)findPreference(
                SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (pref == null) {
            return;
        }
        pref.setEnabled(true);
        CaptureModule.CameraMode mode = (CaptureModule.CameraMode)getIntent().getSerializableExtra(
                CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.VIDEO || mode == CaptureModule.CameraMode.CINEMATIC) {
            pref.setDialogTitle("Video FrameRate");
            pref.setTitle("Video FrameRate");
        }
        mSettingsManager.filterHFROptions();
        updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (pref != null) {
            if (pref.getEntries() != null && pref.getEntries().length == 1) {
                pref.setEnabled(false);
            } else if (mode == CaptureModule.CameraMode.VIDEO) {
                String hdrmode = mSettingsManager.getVideoHdrMode();
                if (hdrmode.toLowerCase().contains("mfhdr")) {
                    pref.setValue("off");
                    pref.setEnabled(false);
                }
            }
        }
        String videoSize = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        String fps = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        String vsr = mSettingsManager.getValue(SettingsManager.KEY_VSR);
        int size = CameraUtil.getSize(videoSize);
        if (vsr != null && vsr.equals("1") && size >= 3840*2160 ){
            pref.setValue("off");
            pref.setEnabled(false);
        }
        if (pref.isEnabled()) {
            updateMultiVideoFPSPreference();
        }
    }

    private void updatePdnetTogglePreference() {
        ListPreference pref = (ListPreference)findPreference(
                SettingsManager.KEY_PDNET_TOGGLE);
        boolean isSingleRear = false;
        ListPreference selectModePref = (ListPreference) findPreference(SettingsManager.KEY_SELECT_MODE);
        if (selectModePref != null) {
            if (selectModePref.getValue().equals("single_rear_cameraid")) {
                isSingleRear = true;
            }
        }
        if (pref != null) {
            pref.setEnabled(mIsSingleCameraMode || isSingleRear);
        }
    }

    private void updateVideoVariableFpsPreference() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_VARIABLE_FPS);
        if (pref == null) {
            return;
        }
        ListPreference hfrPref = (ListPreference)findPreference(
                SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (hfrPref != null) {
            String value = hfrPref.getValue();
            if (!value.equals("off")) {
                int fpsRate = Integer.parseInt(value.substring(3));
                if (fpsRate == 60) {
                    pref.setEnabled(true);
                } else {
                    pref.setEnabled(false);
                }
            }
        }
    }
    private void updateVsrPreference(){
         ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_VSR);
         if(pref == null) return;
         ListPreference Vieopref = (ListPreference)findPreference(SettingsManager.KEY_VIDEO_QUALITY);
         if(Vieopref != null){
             String videoSize = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
             String fps = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
             int size = CameraUtil.getSize(videoSize);
             if(size >= 7680*4320 || (size >= 3840*2160 && fps != null && !fps.equals("off")) ){
                 pref.setValue("0");
                 pref.setEnabled(false);
                 return;
             }
         }
         pref.setEnabled(true);
    }

    private void updateCaptureProfilePref() {
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_CAPTURE_PROFILE);
        if(pref == null) return;
        ListPreference rawFormat = (ListPreference)findPreference(SettingsManager.KEY_RAW_FORMAT_TYPE);
        String yuv10bit = this.getString(R.string.pref_camera2_saveformat_value_yuv10bit);
        String yuv10bitWithMetadata = this.getString(
                R.string.pref_camera2_saveformat_value_yuv10bit_withmetedata);
        String value = "";
        if(rawFormat != null){
            value = rawFormat.getValue();
        }
        if(value.equals(yuv10bit) || value.equals(yuv10bitWithMetadata)){
            pref.setEnabled(true);
        } else {
            pref.setValue("0");
            pref.setEnabled(false);
        }
    }

    private void updateViullPreference() {
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_VIULL);
        if (pref == null) return;
        ListPreference videoQualityPref = (ListPreference) findPreference(SettingsManager.KEY_VIDEO_QUALITY);
        if (videoQualityPref != null) {
            CharSequence videQuality = videoQualityPref.getEntry();
            if (videQuality != null && !videQuality.toString().contains("1080p") && !videQuality.toString().contains("4k")) {
                pref.setValue("0");
                pref.setEnabled(false);
                return;
            }
        }

        ListPreference hfrPref = (ListPreference) findPreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        if (hfrPref != null) {
            String value = hfrPref.getValue();
            if (!"off".equals(value)) {
                pref.setValue("0");
                pref.setEnabled(false);
                return;
            }
        }

        CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        String selectMode = mSettingsManager.getValue(mSettingsManager.KEY_SELECT_MODE);
        if (selectMode.equals("rtb") && mode == CaptureModule.CameraMode.VIDEO) {
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }

        String videoHdrMode = mSettingsManager.getVideoHdrMode();
        if (videoHdrMode != null && videoHdrMode.toLowerCase().contains("mfhdr")) {
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }

        String profile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
        if ("HEVCProfileMain10HDR10Plus".equals(profile)) {
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }

        String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
        if (previewProfile != null && !"0".equals(previewProfile)) {
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }

        String qllStr = mSettingsManager.getValue(SettingsManager.KEY_QLL);
        if (qllStr != null && qllStr.equals("1")) {
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }

        pref.setEnabled(true);

    }

    private void updatePreferenceButton(String key) {
        Preference pref = findPreference(key);
        if (pref != null ) {
            pref.setEnabled(false);
            if( pref instanceof ListPreference) {
                ListPreference pref2 = (ListPreference) pref;
                if (pref2.getEntryValues().length > 1) {
                    updatePreference(key);
                }
            }
        }
    }

    private void updatePictureSizePreferenceButton() {
        Preference picturePref =  findPreference(SettingsManager.KEY_PICTURE_SIZE);
        if (picturePref == null) return;
        String multiResEnabled = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESIMAGEREADER);
        if ((PersistUtil.isMultiResolutionImageReaderEnabled() && multiResEnabled != null
                && "1".equals(multiResEnabled))) {
            picturePref.setEnabled(false);
        } else {
            picturePref.setEnabled(true);
        }
    }

    private void updatePreviewStabilizationPreference() {
        CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode != VIDEO && mode != DEFAULT) {
            return;
        }
        ListPreference pref = (ListPreference)findPreference(SettingsManager.KEY_PREVIEW_STABILIZATION);
        if (pref == null) {
            return;
        }
        if (mode == VIDEO) {
            String value = mSettingsManager.getValue(SettingsManager.KEY_EIS_VALUE);
            if (value != null) {
                if (!value.equals("V2")) {
                    pref.setValueIndex(0);
                    pref.setEnabled(false);
                    return;
                } else {
                    pref.setEnabled(true);
                    return;
                }
            }
        } else {
            String value = mSettingsManager.getValue(SettingsManager.KEY_PHOTO_EIS_VALUE);
            if (value != null) {
                if (value.equals("V2")) {
                    pref.setValueIndex(1);
                    pref.setEnabled(false);
                    return;
                } else if ("dynamic".equals(value)) {
                    Set<String> jpeg_ids = mSettingsManager.getPhysicalFeatureEnableId(
                            SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
                    if (mSettingsManager.getPhysicalCameraId() != null || jpeg_ids != null) {
                        pref.setValueIndex(0);
                        pref.setEnabled(false);
                        return;
                    }
                    pref.setEnabled(true);
                    return;
                } else {
                    pref.setValueIndex(0);
                    pref.setEnabled(false);
                    return;
                }
            }
        }

        pref.setEnabled(true);
    }

    private void updateEISPreference() {
        ListPreference eisPref = (ListPreference)findPreference(
                SettingsManager.KEY_EIS_VALUE);
        if (eisPref == null) return;
        boolean changeEIS = true;
        MultiSelectListPreference camCorderPref = (MultiSelectListPreference) findPreference(
                SettingsManager.KEY_PHYSICAL_CAMCORDER);
        if (camCorderPref != null) {
            Set<String> camCorderSet = camCorderPref.getValues();
            if (camCorderSet != null) {
                for (String str : camCorderSet) {
                    if (!"".equals(str)) {
                        changeEIS &= false;
                    }
                }
            }
        }
        if (!mSettingsManager.isEISSupported(mSettingsManager.getVideoSize(),
                mSettingsManager.getVideoFPS()) || !changeEIS) {
            if (eisPref != null) {
                eisPref.setValue("disable");
                eisPref.setEnabled(false);
            }
        } else {
            if (eisPref != null) {
                eisPref.setEnabled(true);
            }
        }
        if (mSettingsManager.isAIBokehMode()) {
            //remove v2 case
            List<String> list = new ArrayList<String>(Arrays.asList("disable", "V3"));
            List<String> values = new ArrayList<String>(Arrays.asList("disable", "V3"));
            if (eisPref != null) {
                eisPref.setEntries(list.toArray(new CharSequence[list.size()]));
                eisPref.setEntryValues(values.toArray(new CharSequence[values.size()]));
            }
        }
    }

    private void updateT2TPreference() {
        CaptureModule.CameraMode mode = (CaptureModule.CameraMode)
                getIntent().getSerializableExtra(CAMERA_MODULE);
        SwitchPreference t2TFocus = (SwitchPreference) findPreference(
                SettingsManager.KEY_TOUCH_TRACK_FOCUS);
        String selectMode = mSettingsManager.getValue(mSettingsManager.KEY_SELECT_MODE);
        if (t2TFocus != null && mode == CaptureModule.CameraMode.VIDEO) {
            if (selectMode.equals("rtb")) {
                t2TFocus.setEnabled(false);
                t2TFocus.setChecked(false);
            } else {
                t2TFocus.setEnabled(true);
            }
        }
    }

    private void updateZoomPreference() {
        ListPreference zoomPref = (ListPreference)findPreference(SettingsManager.KEY_ZOOM);
        ListPreference extendMaxPref = (ListPreference)findPreference(
                SettingsManager.KEY_EXTENDED_MAX_ZOOM);
        int cameraId = mSettingsManager.getCurrentCameraId();
        List<String> zoomLevelLists = mSettingsManager.getSupportedZoomLevel(cameraId);
        if (extendMaxPref != null && extendMaxPref.getValue().equals("1")) {
            int maxZoom = (int)mSettingsManager.getSupportedExtendedMaxZoom(cameraId);
            zoomLevelLists.add(String.valueOf(maxZoom));
        }
        List<String> zoomEntriesLists = new ArrayList<String>();
        zoomEntriesLists.add("Default");
        for (int i = 1; i< zoomLevelLists.size(); i++) {
            zoomEntriesLists.add(zoomLevelLists.get(i) + "x");
        }
        if(zoomPref != null) {
            zoomPref.setEntries(zoomEntriesLists.toArray(new CharSequence[zoomEntriesLists.size()]));
            zoomPref.setEntryValues(zoomLevelLists.toArray(new CharSequence[zoomLevelLists.size()]));
            int idx = zoomPref.findIndexOfValue(zoomPref.getValue());
            if (idx < 0) {
                idx = 0;
            }
            zoomPref.setValueIndex(idx);
        }
    }

    private void updateRawFormatPref() {
        ListPreference rawFormatPref = (ListPreference)findPreference(
                SettingsManager.KEY_RAW_FORMAT_TYPE);
        ListPreference zslPref = (ListPreference)findPreference(
                SettingsManager.KEY_ZSL);
        ListPreference qllPref = (ListPreference)findPreference(
                SettingsManager.KEY_QLL);
        if ((zslPref != null && zslPref.getValue().equals("app-zsl")) ||
                mSettingsManager.isLimitedHDR() ||
                (qllPref != null && qllPref.getValue().equals("1"))) {
            if (rawFormatPref != null) {
                rawFormatPref.setValue("0");
                rawFormatPref.setEnabled(false);;
            }
        } else {
            if(rawFormatPref != null)
                rawFormatPref.setEnabled(true);
        }

    }
    private void updateInSensorZoom(){
        ListPreference inSenorZoomPref = (ListPreference)findPreference(SettingsManager.KEY_INSENSOR_ZOOM);
        if(inSenorZoomPref == null) return;
        if(inSenorZoomPref != null && mSettingsManager.isLimitedHDR()){
            inSenorZoomPref.setValue("0");
            inSenorZoomPref.setEnabled(false);
            return;
        }
        inSenorZoomPref.setEnabled(true);
    }

    private void updateRawInfoPref(){
        String reprocessType = mSettingsManager.getValue(SettingsManager.KEY_RAW_REPROCESS_TYPE);
        if(reprocessType != null && !reprocessType.equals("disable") && !reprocessType.equals("off") && Integer.valueOf(reprocessType) != 0){
            ListPreference rawInfoPref = (ListPreference)findPreference(SettingsManager.KEY_RAWINFO_TYPE);
            String rawFormat = mSettingsManager.getValue(SettingsManager.KEY_RAW_FORMAT_TYPE);
            int rawFormatType = (rawFormat != null && !rawFormat.equals("disable")&& !rawFormat.equals("off")) ? Integer.parseInt(rawFormat) : 0;
            if(rawFormatType == 10){
                if (rawInfoPref != null) {
                    rawInfoPref.setValue("0");
                    rawInfoPref.setEnabled(false);
                }
            } else if(rawFormatType == 16){
                List<String> key = new ArrayList<String>(Arrays.asList("mipiraw", "BPS Ideal raw" ));
                List<String> value = new ArrayList<String>(Arrays.asList( "0", "2"));
                if (rawInfoPref != null) {
                    rawInfoPref.setEntries(key.toArray(new CharSequence[key.size()]));
                    rawInfoPref.setEntryValues(value.toArray(new CharSequence[value.size()]));
                    int idx = rawInfoPref.findIndexOfValue(rawInfoPref.getValue());;
                    if (idx < 0 ) {
                        idx = 0;
                    }
                    rawInfoPref.setValueIndex(idx);
                    rawInfoPref.setEnabled(true);
                }
            }
        }
    }

    private void updatePictureFormatPreference(){
       ListPreference pictureFormatPref = (ListPreference)findPreference(SettingsManager.KEY_PICTURE_FORMAT);
       if (pictureFormatPref == null) {
           return;
       }
       CaptureModule.CameraMode mode =
               (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
       mSettingsManager.filterPictureFormat();
       updatePreference(SettingsManager.KEY_PICTURE_FORMAT);
       if ((CaptureModule.CameraMode.RTB == mode && isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE))) {
           pictureFormatPref.setValue("0");
           pictureFormatPref.setEnabled(false);
       } else {
           pictureFormatPref.setEnabled(true);
       }
    }

    private void updateLongShotPreference() {
        SwitchPreference longShot = (SwitchPreference) findPreference(
                SettingsManager.KEY_LONGSHOT);
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (longShot != null) {
            if(isPrefEnabled(SettingsManager.KEY_BURST_LIMIT) ){
                longShot.setEnabled(true);
            } else {
                if (isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE) ||
                        mSettingsManager.getQuadBayerSensorPrefEnabled()) {
                    longShot.setChecked(false);
                }
            }
            if(mode == CaptureModule.CameraMode.RTB && isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE)){
                longShot.setChecked(false);
            }
            if(mSettingsManager.isOverriden(SettingsManager.KEY_LONGSHOT)){
                longShot.setChecked(false);
                longShot.setEnabled(false);
            }
        }
    }


    private void updateHDRSceneDetection() {
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.RTB) {
            String captureMFNRDef = this.getString(R.string.pref_camera2_capture_mfnr_default);
            String captureMFNR = mLocalSharedPref.getString(
                    SettingsManager.KEY_CAPTURE_MFNR_VALUE, captureMFNRDef);
            ListPreference hdrSceneDetection = (ListPreference) findPreference(
                    SettingsManager.KEY_AUTO_HDR);
            if (captureMFNR.equals("1")) {
                if (hdrSceneDetection != null) {
                    hdrSceneDetection.setValue("disable");
                    hdrSceneDetection.setEnabled(false);
                }
            } else {
                if (hdrSceneDetection != null) {
                    hdrSceneDetection.setEnabled(true);
                }
            }
        }
    }

    private void updateCinematicOptions(boolean fromRestore) {
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.CINEMATIC) {
            String t2t = mLocalSharedPref.getString(SettingsManager.KEY_TOUCH_TRACK_FOCUS, "on");
            SwitchPreference t2TFocus = (SwitchPreference) findPreference(
                    SettingsManager.KEY_TOUCH_TRACK_FOCUS);
            if (t2t.equals("on") || fromRestore) {
                t2TFocus.setChecked(true);
            }
        }

    }

    private void updatePreference(String key) {
        ListPreference pref = (ListPreference) findPreference(key);
        if (pref != null) {
            if (mSettingsManager.getEntries(key) != null) {
                pref.setEntries(mSettingsManager.getEntries(key));
                pref.setEntryValues(mSettingsManager.getEntryValues(key));
                int idx = mSettingsManager.getValueIndex(key);
                if (idx < 0 ) {
                    idx = 0;
                }
                pref.setValueIndex(idx);
                mSettingsManager.setValueIndex(key, idx);
            }
        }
    }

    private void updateMultiPreference(String key) {

        MultiSelectListPreference pref = (MultiSelectListPreference) findPreference(key);
        if (pref != null) {
            if (mSettingsManager.getEntries(key) != null) {
                pref.setEntries(mSettingsManager.getEntries(key));
                pref.setEntryValues(mSettingsManager.getEntryValues(key));
                String values = mSettingsManager.getValue(key);
                CharSequence[] entryvalue = mSettingsManager.getEntryValues(key);
                Set<String> valueSet = new HashSet<String>();
                if (values != null) {
                    String[] splitValues = values.trim().split(";");
                    for (String str : splitValues) {
                        for(int i=0;i <entryvalue.length ;i++){
                            if(str.equals(entryvalue[i])){
                                valueSet.add(str);
                                break;
                            }
                        }

                    }
                }
                pref.setValues(valueSet);
            }
        }
    }

    private boolean isOn(String value) {
        return value.equals("on") || value.equals("enable");
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSettingsManager.unregisterListener(mListener);
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    private void setShowInLockScreen() {
        // Change the window flags so that secure camera can show when locked
        Window win = getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        win.setAttributes(params);
    }
    private
    void onRestoreDefaultSettingsClick() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.pref_camera2_restore_default_hint)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface Dialog, int which) {
                        restoreSettings();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restoreSettings() {
        mSettingsManager.restoreSettings();
        filterPreferences();
        initializePreferences(true);
    }


    public class DragListViewAdapter extends BaseAdapter {
        private Context mContext;
        private List<String> mDragDatas;
        private CheckBoxChanged mCheckBoxChanged;

        public DragListViewAdapter(Context context, List<String> dataList) {
            mContext = context;
            mDragDatas = dataList;
        }

        @Override
        public int getCount() {
            return mDragDatas == null ? 0 : mDragDatas.size();
        }

        @Override
        public String getItem(int position) {
            return mDragDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(
                        R.layout.drag_list_view_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(mDragDatas.get(position));
            viewHolder.checkBox.setTag(mDragDatas.get(position));
            String hdr = mDragDatas.get(position);
            boolean ischecked = mLocalSharedPref.getBoolean(hdr,false);
            boolean dialogShowed = false;
            if(ischecked && isNotSupportedHdr(hdr)){
                viewHolder.checkBox.setChecked(false);
                viewHolder.checkBox.setSelected(false);
            }else {
                viewHolder.checkBox.setChecked(mLocalSharedPref.getBoolean(hdr, false));
            }

            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String title = String.valueOf(buttonView.getTag());
                    if( isChecked && isNotSupportedHdr(title)){
                        viewHolder.checkBox.setSelected(false);
                        viewHolder.checkBox.setChecked(false);
                        isChecked=false;
                        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                        alert.setMessage("Donnot support "+title+" " +
                                "when Video FPS >=60 or enabled SaveRaw or inSensor zoom" +
                                " or quadBayerSensor or videoSize >=8k in MCX mode");
                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                            }
                        });
                        alert.show();
                    }
                    if (mCheckBoxChanged != null){
                        mCheckBoxChanged.onCheckedChanged(position, title, isChecked);
                    }
                }
            });
            return convertView;
        }
        private boolean isNotSupportedHdr(String hdr){
            String fpsStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
            if((hdr.equalsIgnoreCase("mfhdr") || hdr.equalsIgnoreCase("qhdr"))
                    && (!mSettingsManager.isSupportedMixHdr() || (fpsStr != null && !fpsStr.equals("off")))){
                return true;
            }
            return  false;
        }
        public void setChecked(CheckBoxChanged checked) {
            this.mCheckBoxChanged = checked;
        }

        public void swapData(int from, int to){
            Collections.swap(mDragDatas, from, to);
            notifyDataSetChanged();
        }

        public List<String> getmDragDatas() {
            return mDragDatas;
        }

        class ViewHolder{
            CheckBox checkBox;
            TextView title;
        }

    }
   public static interface CheckBoxChanged {
       public void onCheckedChanged(int position, String title, boolean isChecked);
   }
}
