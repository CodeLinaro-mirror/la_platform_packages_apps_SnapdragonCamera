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
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2022-2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.ColorSpaceProfiles;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.Window;
import android.view.WindowManager;
import com.android.camera.util.Log;
import android.util.ArraySet;
import android.util.Size;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ImageView;
import org.codeaurora.snapcam.R;
import com.android.camera.util.CameraUtil;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.util.PersistUtil;
import android.view.KeyEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.camera.CaptureModule.CameraMode.DEFAULT;
import static com.android.camera.CaptureModule.CameraMode.DEPTH;
import static com.android.camera.CaptureModule.CameraMode.HFR;
import static com.android.camera.CaptureModule.CameraMode.RTB;
import static com.android.camera.CaptureModule.CameraMode.SAT;
import static com.android.camera.CaptureModule.CameraMode.VIDEO;

import android.widget.ExpandableListView;

import com.android.camera.FdExpandListView.FdExpandListViewAdapter;
public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "SnapCam_SettingsActivity";

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
    private ExpandableListView expandableListView = null;
    private FdExpandListViewAdapter expandableAdapter = null;
    private ExpandableListView fdFLExpandableListView = null;
    private FdExpandListViewAdapter fdFLExpandableAdapter = null;
    private ExpandableListView fdFacialExpandableListView = null;
    private FdExpandListViewAdapter fdFacialExpandableAdapter = null;
    private boolean mIsSingleCameraMode = false;
    private boolean mShowAllDevOption = false;
    AlertDialog mManualHDRDialog = null;
    private ArrayList<String> mSearchSettingList;
    private ArrayList<CameraCharacteristics> mCharacteristics;
    private boolean mViullEnabled = true;
    private boolean mClickChanged = false;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            Preference preference = findPreference(key);
            Log.i(TAG, "onSharedPreferenceChanged key:" + key+",mClickChanged="+mClickChanged);
            if (preference == null || null == key) return ;
            if(mClickChanged) {
                if (preference instanceof ListPreference) {
                    String newValue = ((ListPreference) preference).getValue();
                    if (key.equals(SettingsManager.KEY_MANUAL_HDR)) {
                        if (newValue.equals("manual")) {
                            updateManualHDRSetting();
                        }
                    } else if (key.equals(SettingsManager.KEY_MANUAL_EXPOSURE)) {
                        if (!newValue.equals("off")) {
                            UpdateManualExposureSettings(newValue.toString());
                        }
                    } else if (key.equals(SettingsManager.KEY_TONE_MAPPING)) {
                        if (!newValue.equals("off")) {
                            updateToneMappingSettings(newValue.toString());
                        }
                    } else if (key.equals(SettingsManager.KEY_MANUAL_WB)) {
                        if (!newValue.equals("off")) {
                            updateManualWBSettings(newValue.toString());
                        }
                    }
                }
                mClickChanged = false;
            }
            String value;
            if (preference instanceof SwitchPreference) {
                boolean checked = ((SwitchPreference) preference).isChecked();
                value = checked ? "on" : "off";
                mSettingsManager.setValue(key, value);
            } else if (preference instanceof ListPreference){
                value = ((ListPreference) preference).getValue();
                mSettingsManager.setValue(key, value);
            } else if (preference instanceof MultiSelectListPreference) {
                Set<String> valueSet = ((MultiSelectListPreference)preference).getValues();
                mSettingsManager.setValue(key,valueSet);
            }
            List<String> list = mSettingsManager.getDependentKeys(key);
            if (list != null) {
                for (String dependentKey : list) {
                    Log.i(TAG, "onSharedPreferenceChanged dependentKey:" + dependentKey);
                    updatePreferenceButton(dependentKey);
                }
            }
        }
    };

    private SettingsManager.Listener mListener = new SettingsManager.Listener() {
        @Override
        public void onSettingsChanged(List<SettingsManager.SettingState> settings) {
            Map<String, SettingsManager.Values> map = mSettingsManager.getValuesMap();
            CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
            String key;
            String value;
            for (SettingsManager.SettingState state : settings) {
                SettingsManager.Values values = map.get(state.key);
                boolean enabled = true;
                if (values != null) enabled = values.overriddenValue == null;
                Preference pref = findPreference(state.key);
                if (pref == null) continue;
                pref.setEnabled(enabled);
                key = state.key;
                value = mSettingsManager.getValue(key);
              //  value = values.overriddenValue == null values.value : values.overriddenValue;
                Log.i(TAG, "onSettingsChanged key :" + key + ", enabled :" + enabled
                        + "values.overriddenValue=" + values.overriddenValue + ",values.value="
                        + values.value+",value="+value);
                switch (key) {
                    case SettingsManager.KEY_MULTI_CAMERA_MODE:
                        recreate();
                    case SettingsManager.KEY_PHYSICAL_CAMERA:
                        updateMultiVideoFPSPreference();
                        updatePreviewStabilizationPreference();
                        break;
                    case SettingsManager.KEY_PHYSICAL_CAMCORDER:
                        updateMultiVideoFPSPreference();
                        updateEISPreference();
                        break;
                    case SettingsManager.KEY_MULTIRESREPROCESS:
                        updateZslPreference();
                        updateMultiResReprocess();
                    case SettingsManager.KEY_RAW_REPROCESS_TYPE:
                        updatePreference(SettingsManager.KEY_PHYSICAL_RAW_REPROCESS);
                        updateRawInfoPref();
                        updatePreference(SettingsManager.KEY_RAW_FORMAT_TYPE);
                        updatePictureFormatPreference();
                        break;
                    case SettingsManager.KEY_MULTIRESIMAGEREADER:
                        //when multiresolutionimagereader enabled, disable KEY_PICTURE_SIZE
                        value = mSettingsManager.getValue(SettingsManager.KEY_MULTIRESIMAGEREADER);
                        Preference picSize = findPreference(SettingsManager.KEY_PICTURE_SIZE);
                        ListPreference picFormat = (ListPreference) findPreference(SettingsManager.KEY_PICTURE_FORMAT);
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
                        break;
                    case SettingsManager.KEY_RAW_FORMAT_TYPE:
                        updateRawInfoPref();
                        updateVideoMFHDRPreference();
                        updateCaptureProfilePref();
                        updateColorSpacePreference();
                        break;
                    case SettingsManager.KEY_SELECT_MODE:
                        updatePdnetTogglePreference();
                        updateViullPreference();
                        updateAICameraPerf();
                        updateT2TPreference();
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        mSettingsManager.filterVideoEncoderProfileOptions();
                        updatePreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
                        updateVideoHfrFpsPreference();
                        updateEISPreference();
                        updateZoomPreference();
                        break;
                    case SettingsManager.KEY_VIDEO_QUALITY:
                        updateVideoVariableFpsPreference();
                        updateVideoHfrFpsPreference();
                        mSettingsManager.filterVideoEncoderProfileOptions();
                        updatePreference(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
                        updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
                        updatePreference(SettingsManager.KEY_VIDEO_ENCODER);
                        updateAICameraPerf();
                        updateEISPreference();
                        updateVideoMFHDRPreference();
                        updateVideoFlipPreference();
                        updateViullPreference();
                        updateVSRPreference();
                        break;
                    case SettingsManager.KEY_VIDEO_ENCODER:
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        updateVideoHfrFpsPreference();
                        updateVideoEncoderProfile();
                        updateBitrateCQModePref();
                        break;
                    case SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE:
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
                        updateVIULLDefaultValue();
                        break;
                    case SettingsManager.KEY_VIDEO_ENCODER_PROFILE:
                        if (mode == CaptureModule.CameraMode.VIDEO) {
                            updateSwitchIDInModePreference(true);
                        }
                        updateViullPreference();
                        break;
                    case SettingsManager.KEY_BURST_LIMIT:
                        // If Enable KEY_BURST_LIMIT, KEY_CAPTURE_MFNR_VALUE and KEY_LONGSHOT can same use
                        // if diable KEY_BURST_LIMIT, enable KEY_CAPTURE_MFNR_VALUE, KEY_LONGSHOT is diable
                        updateLongShotPreference();
                        updateMFNRPreference();
                        break;
                    case SettingsManager.KEY_CAPTURE_MFNR_VALUE:
                        updateLongShotPreference();
                        updateZslPreference();
                        updatePictureFormatPreference();
                        updateHDRSceneDetection();
                        if (isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE)) {
                            if (CaptureModule.CameraMode.RTB == mode) {
                                mSettingsManager.setValueIndex(SettingsManager.KEY_SCENE_MODE, 0);
                            }
                        }
                        break;
                    case SettingsManager.KEY_LONGSHOT:
                        updateMFNRPreference();
                        break;
                    case SettingsManager.KEY_REMOSAIC_REPROCESSING:
                        updateRawFormatPref();
                        break;
                    case SettingsManager.KEY_PREVIEW_PROFILE:
                        updateViullPreference();
                        updateColorSpacePreference();
                        break;
                    case SettingsManager.KEY_CAPTURE_PROFILE:
                        updateColorSpacePreference();
                        break;
                    case SettingsManager.KEY_PICTURE_FORMAT:
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        updateColorSpacePreference();
                        updateRawFormatPref();
                        break;
                    case SettingsManager.KEY_EIS_VALUE:
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        updateVideoMFHDRPreference();
                        updatePreviewStabilizationPreference();
                        break;
                    case SettingsManager.KEY_INSENSOR_ZOOM:
                        updateVideoMFHDRPreference();
                    case SettingsManager.KEY_PHOTO_EIS_VALUE:
                    case SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK:
                        updatePreviewStabilizationPreference();
                        break;
                    case SettingsManager.KEY_MANUAL_HDR:
                        updateHdrRefOp();
                        updateQuadBayerPreference();
                        updateQLLPreference();
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        if(PersistUtil.isFuncTestRunning() && "manual".equals(value)){
                            updateManualHDRSetting();
                        }
                        break;
                    case SettingsManager.KEY_QUAD_BAYER_SENSOR:
                        mSettingsManager.updatePictureAndVideoSize();
                        mSettingsManager.updateHDRSceneMode();
                        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                        updatePreference(SettingsManager.KEY_SCENE_MODE);
                        updateZslPreference();
                        updateLongShotPreference();
                        updatePictureFormatPreference();
                        updateVideoMFHDRPreference();
                        updateInSensorZoom();
                        updateSwitchIDInModePreference(false);
                        if (mSettingsManager.isMultiCameraEnabled()) {
                            recreate();
                        }
                        break;
                    case SettingsManager.KEY_HFR_BUFFER_MODE:
                        updateVideoHfrFpsPreference();
                        break;
                    case SettingsManager.KEY_SCENE_MODE:
                        String scene = mSettingsManager.getValue(SettingsManager.KEY_SCENE_MODE);
                        if (!scene.equals("18")) {
                            ListPreference lp = (ListPreference) findPreference(
                                    SettingsManager.KEY_SNAPSHOT_HDRMODE);
                            if (lp != null) {
                                lp.setValue("default");
                                lp.setEnabled(false);
                            }
                        }
                        updateViullPreference();
                        updateVideoMFHDRPreference();
                        break;
                    case SettingsManager.KEY_SWITCH_CAMERA:
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        checkExposurTimeValue();
                        updateZoomPreference();
                        break;
                    case SettingsManager.KEY_VSR:
                        updateVideoHfrFpsPreference();
                        mSettingsManager.updatePictureAndVideoSize();
                        updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                        updateViullPreference();
                        break;
                    case SettingsManager.KEY_AI_CAMERA:
                        updateSwitchIDInModePreference(true);
                        updateEISPreference();
                        break;
                    case SettingsManager.KEY_EXTENDED_MAX_ZOOM:
                        updateZoomPreference();
                        break;
                    case SettingsManager.KEY_LOWLIGHT_BOOST:
                        updateViullPreference();
                        break;
                    case SettingsManager.KEY_VIULL:
                        updateLowLightBoostPreference();
                        break;
                    case SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK:
                        recreate();
                        break;

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
            CharSequence[]entries = key_zsl.toArray(new CharSequence[key_zsl.size()]);
            CharSequence[] values = value_zsl.toArray(new CharSequence[value_zsl.size()]);
            ZSLPref.setEntries(entries);
            ZSLPref.setEntryValues(values);
            mSettingsManager.setEntries(SettingsManager.KEY_ZSL,entries);
            mSettingsManager.setValues(SettingsManager.KEY_ZSL,values);
            int idx = ZSLPref.findIndexOfValue(ZSLPref.getValue());;
            if (idx < 0 ) {
                idx = 0;
            }
            ZSLPref.setValueIndex(idx);
        }
    }

    private void UpdateManualExposureSettings(String manualExposureMode) {
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
        long[] exposureRange = mSettingsManager.getExposureRangeValues(cameraId);

        int[] isoRange = mSettingsManager.getIsoRangeValues(cameraId);
        String isoSet = pref.getString(SettingsManager.KEY_MANUAL_ISO_VALUE, "100");
        ISOtext.setText("If enter value is invalid,use current value " + isoSet +
                ",default is 100");
        String expTimeSet = pref.getString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, String.valueOf(exposureRange[0]));
        ExpTimeText.setText("If enter value is invalid,use current value "+expTimeSet +
                ",default is "+exposureRange[0]);
        Log.d(TAG, "manual Exposure Mode selected = " + manualExposureMode +
                ",isoSet="+isoSet+",currentExpTime="+expTimeSet);
        if (manualExposureMode.equals(isoPriority)) {
            alert.setMessage("Enter ISO in the range of " + isoRange[0] + " to " + isoRange[1]);
            linear.addView(ISOinput);
            linear.addView(ISOtext);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newISO = -1;
                    String iso = ISOinput.getText().toString();
                    String value = getISOValue(iso,isoRange,pref);
                        editor.putString(SettingsManager.KEY_MANUAL_ISO_VALUE, value);
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
                    String expTime = ExpTimeInput.getText().toString();
                    String value = getExpTimeValue(expTime,exposureRange,pref);
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, value);
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
                    String isoValue = getISOValue(iso,isoRange,pref);
                        editor.putString(SettingsManager.KEY_MANUAL_ISO_VALUE, isoValue);
                        editor.apply();
                    String expTime = ExpTimeInput.getText().toString();
                    String expTimeValue = getExpTimeValue(expTime,exposureRange,pref);
                        editor.putString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, expTimeValue);
                        editor.apply();
                }
            });
            alert.show();
        } else if (manualExposureMode.equals(gainsPriority)){
            handleManualGainsPriority(linear, ISOtext, ExpTimeInput, pref);
        }
    }
    private String getExpTimeValue(String exptime,long[] exposureRange ,SharedPreferences pref) {
        long newValue = exposureRange[0];
        String valueStr = pref.getString(SettingsManager.KEY_MANUAL_EXPOSURE_VALUE, String.valueOf(newValue));
        long value = Long.valueOf(valueStr);
        if (exptime.length() > 0) {
            try {
                newValue = Long.valueOf(exptime);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Input value " + exptime + " is incorrect value entered ");
                newValue = value;
                RotateTextToast.makeText(SettingsActivity.this,
                        "Input exptime " + exptime + " is invalid,use current value: " + newValue,
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            newValue = Long.valueOf(valueStr);
        }
        if (newValue <= exposureRange[1] && newValue >= exposureRange[0]) {
            return String.valueOf(newValue);
        } else {
            Log.i(TAG,"makeText newValue="+newValue);
            RotateTextToast.makeText(SettingsActivity.this,
                    "Input newValue " + newValue + " is out of range,use current value: " + valueStr,
                    Toast.LENGTH_SHORT).show();
            return valueStr;
        }
    }
    private String getISOValue(String iso,int[] isoRange,SharedPreferences pref){
        int newISO = 100;
        String currentISO = pref.getString(SettingsManager.KEY_MANUAL_ISO_VALUE, "100");
        int isoValue = Integer.parseInt(currentISO);
        if (iso.length() > 0) {
            try {
                newISO = Integer.parseInt(iso);
            } catch(NumberFormatException e) {
                Log.w(TAG, "Input iso : "+iso + "is incorrect value entered ");
                newISO = isoValue;
                RotateTextToast.makeText(SettingsActivity.this,
                        "Input iso "+iso +" is invalid,use current value:"+newISO,
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            newISO = Integer.parseInt(currentISO);
        }
        if (newISO <= isoRange[1] && newISO >= isoRange[0]) {
           return String.valueOf(newISO);
        }else{
            RotateTextToast.makeText(SettingsActivity.this,
                    "Input iso "+iso +" is out of range,use current value:"+currentISO,
                    Toast.LENGTH_SHORT).show();
            return currentISO;
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

    private void updateManualWBSettings(String manualWBMode ) {
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

        String colorTempe = this.getString(
                R.string.pref_camera_manual_wb_value_color_temperature);
        String cctMode = this.getString(
                R.string.pref_camera_manual_wb_cct);
        String tintMode = this.getString(
                R.string.pref_camera_manual_wb_value_color_tint);
        String currentWBTemp = mLocalSharedPref.getString(
                SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, "5000");
        String currentTint = mLocalSharedPref.getString(
                SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, "0");
        Log.v(TAG, "manualWBMode selected = " + manualWBMode+",currentWBTemp="+currentWBTemp+
                ",currentTint="+currentTint);
        final int[] wbRange = mSettingsManager.getWBColorTemperatureRangeValues(cameraId);
        if (manualWBMode.equals(cctMode)) {
            final TextView CCTtext = new TextView(SettingsActivity.this);
            final TextView enterTemperature = new TextView(SettingsActivity.this);
            final TextView enterTint = new TextView(SettingsActivity.this);
            final EditText CCTinput = new EditText(SettingsActivity.this);
            alert.setMessage("CCT Mode-Enter both colorTemperature and Tint value");
            CCTinput.setInputType(InputType.TYPE_CLASS_NUMBER);
            CCTtext.setText("If enter value is invalid,use current value " + currentWBTemp+",default is 5000");
            if (wbRange == null) {
                enterTemperature.setText(" Enter colorTemperature,range is  null");
            } else {
                enterTemperature.setText(" Enter colorTemperature in the range of  " +wbRange[0]+" to "+wbRange[1]);
            }
            final TextView Tinttext = new TextView(SettingsActivity.this);
            final EditText Tintinput = new EditText(SettingsActivity.this);
            Tintinput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            enterTint.setText("Enter Tint in the range of -50 to 50 " );
            Tinttext.setText("If enter value is invalid,use current value " + currentTint+",default is 0");
            linear.addView(enterTemperature);
            linear.addView(CCTinput);
            linear.addView(CCTtext);
            linear.addView(enterTint);
            linear.addView(Tintinput);
            linear.addView(Tinttext);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newCCT = Integer.parseInt(currentWBTemp);
                    String cct = CCTinput.getText().toString();
                    if (cct.length() > 0) {
                        try {
                            newCCT = Integer.parseInt(cct);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "CCTinput type incorrect value ");
                            RotateTextToast.makeText(SettingsActivity.this, "Invalid CCT,use current value:"+currentWBTemp,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        cct = currentWBTemp;
                    }
                    if (wbRange == null || (newCCT <= wbRange[1] && newCCT >= wbRange[0])) {
                        Log.v(TAG, "Setting CCT value : " + cct);
                        //0 corresponds to manual CCT mode
                        editor.putString(SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, cct);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_WB_TEMPERATURE_VALUE, currentWBTemp);
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "CCT out of range,use current value:"+currentWBTemp,
                                Toast.LENGTH_SHORT).show();
                    }

                    int newValue = Integer.parseInt(currentTint);
                    String tint = Tintinput.getText().toString();
                    if (tint.length() > 0) {
                        try {
                            newValue = Integer.parseInt(tint);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Tintinput type incorrect value ");
                            tint = currentTint;
                            RotateTextToast.makeText(SettingsActivity.this, "Invalid tint,use current value:"+currentTint,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        tint = currentTint;
                    }
                    if (newValue <= 50 && newValue >= -50) {
                        Log.v(TAG, "Setting tint value : " + tint);
                        //0 corresponds to manual CCT mode
                        editor.putString(SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, tint);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, currentTint);
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "Tint out of range,use current value:"+currentTint,
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
        } else if(manualWBMode.equals(tintMode)){
            final TextView Tinttext = new TextView(SettingsActivity.this);
            final EditText Tintinput = new EditText(SettingsActivity.this);
            Tintinput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            //refresh camera parameters to get latest CCT value
            Tinttext.setText(" Current Tint is " + currentTint+",default value is 0");
            alert.setMessage("Enter CCT value in the range of -50 to 50 ");
            linear.addView(Tintinput);
            linear.addView(Tinttext);
            alert.setView(linear);
            alert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface Dialog,int id) {
                    int newValue = -1;
                    String tint = Tintinput.getText().toString();
                    if (tint.length() > 0) {
                        try {
                            newValue = Integer.parseInt(tint);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Tintinput type incorrect value ");
                            newValue = Integer.parseInt(currentTint);
                            RotateTextToast.makeText(SettingsActivity.this, "Invalid tint,use curren value:"+currentTint,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (newValue <= 50 && newValue >= -50) {
                        Log.v(TAG, "Setting tint value : " + tint);
                        //0 corresponds to manual CCT mode
                        editor.putString(SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, tint);
                        editor.apply();
                    } else {
                        editor.putString(SettingsManager.KEY_MANUAL_COLOR_TINT_VALUE, currentTint);
                        editor.apply();
                        RotateTextToast.makeText(SettingsActivity.this, "Tint out of range,use current value:"+currentTint,
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
        }
    }

    private void updateToneMappingSettings(String toneMappingMode) {
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

    public class RadioListAdapter extends BaseAdapter{
        Context context;
        List<String> listItems;
        LayoutInflater mInflater;
        public RadioListAdapter(Context context,List<String> mList){
            this.context = context;
            this.listItems = mList;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return listItems.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            RadioListViewHolder viewHolder = null;
            if(convertView == null){
                convertView = mInflater.inflate(R.layout.radio_button_list_item,parent,false);
                viewHolder = new RadioListViewHolder();
                viewHolder.name = (TextView)convertView.findViewById(R.id.tv_item);
                viewHolder.select = (RadioButton)convertView.findViewById(R.id.rb_item);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (RadioListViewHolder)convertView.getTag();
            }
            viewHolder.name.setText(listItems.get(position));
            viewHolder.name.setEnabled(parent.isEnabled());
            if(getPositionForMode(mSettingsManager.getDcgMode())  == position){
                viewHolder.select.setChecked(true);
            }
            else{
                viewHolder.select.setChecked(false);
            }
            return convertView;
        }
    }

    private int getPositionForMode(int mode){
        int position = -1;
        int[] dcgModes = mSettingsManager.getsupportedDcgModes();
        if(dcgModes != null) {
            for (int i = 0; i < dcgModes.length; i++) {
                if (mode == dcgModes[i]) {
                    position = i;
                    break;
                }
            }
        }
        return position;
    }
    public class RadioListViewHolder {
        TextView name;
        RadioButton select;
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    public String parseDCGModes(int mode) {
        byte[] bytes = intToBytes(mode);
        StringBuilder value = new StringBuilder();
        for(int i=bytes.length-1; i>0; i--){
            if(i ==3){
                if (bytes[i] == 1) {
                    value.append(SettingsManager.KEY_MANUAL_DCG1_4);
                } else if (bytes[i] == 2) {
                    value.append(SettingsManager.KEY_MANUAL_DCG1_8);
                } else if (bytes[i] == 3) {
                    value.append(SettingsManager.KEY_MANUAL_DCG1_16);
                } else if (bytes[i] == 4) {
                    value.append(SettingsManager.KEY_MANUAL_DCGDirect);
                } else if (bytes[i] == 5) {
                    value.append(SettingsManager.KEY_MANUAL_DCGVS);
                }
            }else if(i ==1|| i ==2){
                if(bytes[i] != 0) {
                    value.append("/");
                    value.append(bytes[i]);
                }
            }
        }
        return value.toString();
    }
    private void updateManualHDRSetting() {
        List<String> listData = new ArrayList<String>();
        int[] modes = mSettingsManager.isManualHDRSupported();
        int[] dcgModes = mSettingsManager.getsupportedDcgModes();
        StringBuilder defaultHDROrder = new StringBuilder();
        CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);

        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == 1 && mode != RTB) {
                listData.add(SettingsManager.KEY_MANUAL_SHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_SHDR).append("#");
            } else if (modes[i] == 2 && !mSettingsManager.isAIBokehMode() && mode != RTB) {
                listData.add(SettingsManager.KEY_MANUAL_MFHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_MFHDR).append("#");
            } else if (modes[i] == 3) {
                listData.add(SettingsManager.KEY_MANUAL_QHDR);
                defaultHDROrder.append(SettingsManager.KEY_MANUAL_QHDR);
            }
        }
        if(dcgModes != null && dcgModes.length > 0) {
            listData.add(SettingsManager.KEY_MANUAL_DCG);
            defaultHDROrder.append(SettingsManager.KEY_MANUAL_DCG);
        }
        final SharedPreferences.Editor editor = mLocalSharedPref.edit();
        String orderLists = mLocalSharedPref.getString(SettingsManager.KEY_MIXED_HDR_ORDER, null);
        if (orderLists != null) {
            listData.clear();
            for (String title : orderLists.split("#")) {
                listData.add(title);
            }
        }else{
            editor.putString(SettingsManager.KEY_MIXED_HDR_ORDER, defaultHDROrder.toString());
            editor.apply();
        }
        View view = View.inflate(getApplicationContext(), R.layout.manual_hdr_layout, null);
        ListView dcgItems = (ListView)view.findViewById(R.id.dcg_list);
        List<String> dcgData = new ArrayList<String>();
        if(dcgModes != null && dcgModes.length > 0) {
            for (int i = 0; i < dcgModes.length; i++) {
                dcgData.add(parseDCGModes(dcgModes[i]));
            }
        }
        RadioListAdapter arrayDapter = new RadioListAdapter(this, dcgData);
        dcgItems.setAdapter(arrayDapter);
        dcgItems.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mSettingsManager.setDcgMode(dcgModes[i]);
                arrayDapter.notifyDataSetChanged();
            }
        });
        setDCGListStatus(dcgItems, mSettingsManager.isDCGEnable());
        final DragonListView listView = (DragonListView)view.findViewById(R.id.dragon_list);
        listView.setContext(SettingsActivity.this);
        DragListViewAdapter adapter = new DragListViewAdapter(this, listData);
        listView.setAdapter(adapter);
        adapter.setChecked(new CheckBoxChanged() {
            @Override
            public void onCheckedChanged(int position, String title, boolean isChecked) {
                editor.putBoolean(title, isChecked);
                editor.commit();
                updateHdrRefOp();
                mSettingsManager.updatePictureAndVideoSize();
                updatePreference(SettingsManager.KEY_PICTURE_SIZE);
                updatePreference(SettingsManager.KEY_VIDEO_QUALITY);
                if(title.equals(SettingsManager.KEY_MANUAL_DCG)) {
                    setDCGListStatus(dcgItems, isChecked);
                    if(!isChecked){
                        mSettingsManager.setDcgMode(0);
                    }
                    dcgItems.setSelection(getPositionForMode(mSettingsManager.getDcgMode()));
                    arrayDapter.notifyDataSetChanged();
                }
            }
        });
        final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        alert.setTitle("MANUAL HDR Settings");
        alert.setView(view);
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
                editor.putString(SettingsManager.KEY_MIXED_HDR_ORDER, mixedHDROrder.toString());
                editor.apply();
            }
        });
        mManualHDRDialog = alert.create();
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mManualHDRDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        mManualHDRDialog.show();
    }

    private void setDCGListStatus(ListView dcgItems, boolean enable){
        if(enable) {
            dcgItems.setEnabled(true);
            dcgItems.setClickable(true);

        }else{
            dcgItems.setClickable(false);
            dcgItems.setEnabled(false);
        }
    }

    private void updateHdrRefOp(){
        CaptureModule.CameraMode mode =
                    (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        if (mode == CaptureModule.CameraMode.VIDEO){
            mSettingsManager.filterHFROptions();
            updatePreference(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
            updateVideoVariableFpsPreference();
            updateVideoHfrFpsPreference();
            updateViullPreference();
        }else if (mode == CaptureModule.CameraMode.DEFAULT){
            updateRawFormatPref();
            updateViullPreference();
            updateQuadBayerPreference();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        View actionView = searchItem.getActionView();
        ImageView searchIcon = (ImageView)actionView.findViewById(R.id.gosearch);
        AutoCompleteTextView autoCompTextView = actionView.findViewById(R.id.autoCompleteTextView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, mSearchSettingList);
        autoCompTextView.setAdapter(adapter);
        autoCompTextView.setThreshold(1);
        setSearchView(autoCompTextView,searchIcon);
        return true;

    }

    private void setSearchView(AutoCompleteTextView autoCompTextView,ImageView searchIcon){

        searchIcon.setOnClickListener(view -> {
            String selectedText = autoCompTextView.getText().toString();
            if(selectedText != null) {
                scrollToPreference(selectedText);
                autoCompTextView.setText("");
                searchIcon.setVisibility(View.INVISIBLE);
            }
        });
        autoCompTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchIcon.setVisibility(View.VISIBLE);
            }
        });
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
        mDeveloperMenuEnabled = mDeveloperMenuEnabled || mShowAllDevOption || PersistUtil.isKeyTestRunning();
        filterPreferences();
        initializePreferences(false);
        mSearchSettingList = new ArrayList<>();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(i);
            for (int j = 0; j < category.getPreferenceCount(); j++) {
                Preference pref = category.getPreference(j);
                mSearchSettingList.add(pref.getTitle().toString());
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                       String key = preference.getKey();
                        Log.i(TAG,"onPreferenceClick preference.getKey()="+key);
                        mClickChanged = true;
                        if (!mDeveloperMenuEnabled) {
                            if (key.equals("version_info")) {
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
                        if ( key.equals(SettingsManager.KEY_RESTORE_DEFAULT) ) {
                            onRestoreDefaultSettingsClick();
                        }
                        if( key.equals(SettingsManager.KEY_FD_SETTING)) {
                            View listView = (SettingsActivity.this).getLayoutInflater().inflate(
                                    R.layout.expandlistview, null);
                            final AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                            alert.setTitle("FD Features");
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
                        if(preference instanceof ListPreference) {
                            String value = ((ListPreference) preference).getValue();
                            if (key.equals(SettingsManager.KEY_MANUAL_HDR)) {
                                if (value.equals("manual")) {
                                    updateManualHDRSetting();
                                }
                            } else if (key.equals(SettingsManager.KEY_MANUAL_EXPOSURE)) {
                                if (!value.equals("off")) {
                                    UpdateManualExposureSettings(value);
                                }
                            } else if (key.equals(SettingsManager.KEY_TONE_MAPPING)) {
                                if (!value.equals("off")) {
                                    updateToneMappingSettings(value);
                                }
                            } else if (key.equals(SettingsManager.KEY_MANUAL_WB)) {
                                if (!value.equals("off")) {
                                    updateManualWBSettings(value);
                                }
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
                add(SettingsManager.KEY_VIDEO_FLIP);
                add(SettingsManager.KEY_PHYSICAL_CAMCORDER);
                add(SettingsManager.KEY_OFFLINE_DUMP_TRIGGER);
                for (String key: SettingsManager.KEY_PHYSICAL_VIDEO_SIZE)
                    add(key);
                add(SettingsManager.KEY_AUDIO_RECORDING_MODE);
                add(SettingsManager.KEY_FRC_MODE);
                add(SettingsManager.KEY_AI_CAMERA_BLURMODE);
                add(SettingsManager.KEY_ML_VIDEO);
                add(SettingsManager.KEY_HDR10P_STATS_KEY);
                add(SettingsManager.KEY_AI_CAMERA_HSR);
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
            removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS_FOR_CINEMATIC, videoPre);
        }
        boolean isStatsNN = mSettingsManager.isStatsNNSupported();
        if (!isStatsNN) {
            removePreference(SettingsManager.KEY_STATSNN_CONTROL, photoPre);
        }
        if(!mSettingsManager.isCctModeSupported()){
            removePreference(SettingsManager.KEY_MANUAL_WB, photoPre);
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
            //removePreference(SettingsManager.KEY_FACE_DETECTION_MODE, developer);
            removePreference(SettingsManager.KEY_FD_GENDER, developer);
            removePreference(SettingsManager.KEY_FD_FACE_EXPRESSION, developer);
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
        if (mode != DEPTH) {
            removePreference(SettingsManager.KEY_ITOF_TUNING_SET, developer);
        }

        switch (mode) {
            case DEFAULT:
                removePreferenceGroup("video", parentPre);
                if (mDeveloperMenuEnabled && developer != null) {
                    removePreference(SettingsManager.KEY_CINEMATIC_DEBUG, developer);
                    removePreference(SettingsManager.KEY_STATSNN_CONTROL_FOR_CINEMATIC, developer);
                    removePreference(SettingsManager.KEY_AUDIO_BLE, developer);
                    for (String removeKey : videoOnlyList) {
                        removePreference(removeKey, developer);
                    }
                    if (!mSettingsManager.isIntegratedModeSupported()) {
                        removePreference(SettingsManager.KEY_INTEGRATED_MODE, developer);
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
                if(!mSettingsManager.isFlashAvailable()){
                    removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, videoPre);
                }
                break;
            case VIDEO:
            case HFR:
                removePreferenceGroup("photo", parentPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> videoAddList = new ArrayList<>();
                    videoAddList.add(SettingsManager.KEY_ZOOM);
                    videoAddList.add(SettingsManager.KEY_SWITCH_CAMERA);
                    videoAddList.addAll(videoOnlyList);
                    videoAddList.add(SettingsManager.KEY_ANTI_BANDING_LEVEL);
                    videoAddList.add(SettingsManager.KEY_EXPOSURE_METERING_MODE);
                    if (!PersistUtil.enableMediaRecorder()) {
                        videoAddList.add(SettingsManager.KEY_BITRATE_CQMODE);
                    }
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
                        videoAddList.add(SettingsManager.KEY_MANUAL_EXPOSURE);
                        videoAddList.add(SettingsManager.KEY_EARLY_PCR_NUM);
                        if (PersistUtil.enableMediaRecorder()) {
                            videoAddList.remove(SettingsManager.KEY_VIDEO_FLIP);
                            if (mSettingsManager.isBLEConnected()) {
                               videoAddList.add(SettingsManager.KEY_AUDIO_BLE);
                            }
                        }
                        videoAddList.add(SettingsManager.KEY_AI_CAMERA);
                        videoAddList.add(SettingsManager.KEY_AI_CAMERA_SNAPSHOT);
                        videoAddList.add(SettingsManager.KEY_PREVIEW_STABILIZATION);
                        videoAddList.add(SettingsManager.KEY_SENSOR_MODE_FS2_VALUE);
                        videoAddList.add(SettingsManager.KEY_VIULL);
                        videoAddList.add(SettingsManager.KEY_LOWLIGHT_BOOST);
                        videoAddList.add(SettingsManager.KEY_INSENSOR_ZOOM);
                        videoAddList.add(SettingsManager.KEY_C2PA);
                        videoAddList.add(SettingsManager.KEY_OVERRIDE_RESOURCE);
                    } else {
                        videoAddList.add(SettingsManager.KEY_FD_SETTING);
                        videoAddList.remove(SettingsManager.KEY_AI_CAMERA_BLURMODE);
                        videoAddList.remove(SettingsManager.KEY_VARIABLE_FPS);
                        videoAddList.remove(SettingsManager.KEY_VIDEO_FLIP);
                        videoAddList.remove(SettingsManager.KEY_ML_VIDEO);
                    }
                    videoAddList.add(SettingsManager.KEY_PREVIEW_PROFILE);
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
                    removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, videoPre);
                    if(!mSettingsManager.isSupportedSuperBuffer(mSettingsManager.getCurrentCameraId())){
                        removePreference(SettingsManager.KEY_HFR_BUFFER_MODE, videoPre);
                    }
                    Preference p1 = findPreference(SettingsManager.KEY_PICTURE_FORMAT);
                    if (p1 != null){
                        PreferenceGroup general = (PreferenceGroup)findPreference("general");
                        general.removePreference(p1);
                    }
                }else {
                    removePreference(SettingsManager.KEY_HFR_BUFFER_MODE, videoPre);
                    if(!mSettingsManager.isFlashAvailable()){
                        removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, videoPre);
                    }
                }
                removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS_FOR_CINEMATIC, videoPre);
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
                removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, videoPre);
                removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, videoPre);
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
                removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, photoPre);
                if (mDeveloperMenuEnabled) {
                    ArrayList<String> RTBList = new ArrayList<>(multiCameraSettingList);
                    RTBList.add(SettingsManager.KEY_CAPTURE_MFNR_VALUE);
                    RTBList.add(SettingsManager.KEY_MANUAL_HDR);
                    RTBList.add(SettingsManager.KEY_INSENSOR_ZOOM);
                    RTBList.add(SettingsManager.KEY_INSTANT_ZOOM);
                    RTBList.add(SettingsManager.KEY_FD_SETTING);
                    RTBList.add(SettingsManager.KEY_FD_FL_SETTING);
                    RTBList.add(SettingsManager.KEY_FD_FACIAL_SETTING);
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
                removePreference(SettingsManager.KEY_CAMERA_MANUALFLASH, photoPre);
                removePreference(SettingsManager.KEY_TOUCH_TRACK_FOCUS, photoPre);
                if (mDeveloperMenuEnabled) {
                    proModeOnlyList.add(SettingsManager.KEY_SWITCH_CAMERA);
                    proModeOnlyList.add(SettingsManager.KEY_STATS_VISUALIZER_ENABLE);
                    proModeOnlyList.add(SettingsManager.KEY_STATS_VISUALIZER_VALUE);
                    proModeOnlyList.add(SettingsManager.KEY_EXTENDED_MAX_ZOOM);
                    proModeOnlyList.add(SettingsManager.KEY_TONE_MAPPING);
                    proModeOnlyList.add(SettingsManager.KEY_QUAD_BAYER_SENSOR);
                    addDeveloperOptions(developer, proModeOnlyList);
                }
                break;
            case DEPTH:
                removePreferenceGroup("general", parentPre);
                removePreferenceGroup("photo", parentPre);
                removePreferenceGroup("video", parentPre);
                if (mDeveloperMenuEnabled && developer != null) {
                    ArrayList<String> depthList = new ArrayList<>();
                    depthList.add(SettingsManager.KEY_ITOF_TUNING_SET);
                    addDeveloperOptions(developer, depthList);
                }
                break;
            default:
                //don't filter
                break;
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
        if(aiCamera != null) {
            aiCamera.setEnabled(true);
        }
        if ( mode == VIDEO  && mSettingsManager.getCurrentCameraId() != CaptureModule.FRONT_ID){
            if(aiCamera != null) {
                if(mSettingsManager.getPerfValue(SettingsManager.KEY_SELECT_MODE).equals("rtb") || is8KVideo()) {
                    aiCamera.setValue("0");
                    aiCamera.setEnabled(false);
                }else if(mSettingsManager.getPerfValue(SettingsManager.KEY_SELECT_MODE).equals("single_rear_aibokeh")){
                    aiCamera.setValue("2");
                    aiCamera.setEnabled(false);
                }
            }
        }
        if (mSettingsManager.getPerfValue(SettingsManager.KEY_SELECT_MODE).equals("rtb") && mode == VIDEO  &&
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
    private void clearKeyValue(String key){
        String value = mSettingsManager.getValue(key);
        if(value != null && !value.equals("")){
            Set<String> valueSet = new HashSet<>();
            mSettingsManager.setValue(key,valueSet);
        }
    }
    private void updatePhysicalPreferences() {
        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        CaptureModule.CameraMode mode =
                (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);

        final ArrayList<String> multiCameraPhotoList = new ArrayList<String>() {
            {
                add(SettingsManager.KEY_PHYSICAL_CAMERA);
                add(SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
                Set<String> jpegR_ids = mSettingsManager.getPhysicalFeatureEnableId(
                        SettingsManager.KEY_PHYSICAL_JPEG_R_CALLBACK);
                if (jpegR_ids == null) {
                    add(SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
                    add(SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
                    add(SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
                    add(SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
                }else{
                    clearKeyValue(SettingsManager.KEY_PHYSICAL_JPEG_CALLBACK);
                    clearKeyValue(SettingsManager.KEY_PHYSICAL_YUV_CALLBACK);
                    clearKeyValue(SettingsManager.KEY_PHYSICAL_YUV10BIT_CALLBACK);
                    clearKeyValue(SettingsManager.KEY_PHYSICAL_RAW_CALLBACK);
                }
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
                multiCameraPhotoList.add(SettingsManager.KEY_OVERRIDE_RESOURCE);
                addDeveloperOptions(developer,multiCameraPhotoList);
            } else {
                multiCameraPhotoList.remove(SettingsManager.KEY_ZSL);
                multiCameraPhotoList.remove(SettingsManager.KEY_OVERRIDE_RESOURCE);
                for (String removeKey : multiCameraPhotoList){
                    removePreference(removeKey,developer);
                }
            }
        } else if (mode == VIDEO){
            if (mSettingsManager.isMultiCameraEnabled()){
                multiCameraVideoList.add(SettingsManager.KEY_MULTI_CAMERA_MODE);
                multiCameraVideoList.add(SettingsManager.KEY_EIS_VALUE);
                multiCameraVideoList.add(SettingsManager.KEY_PREVIEW_STABILIZATION);
                multiCameraVideoList.add(SettingsManager.KEY_OVERRIDE_RESOURCE);
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
            if(index >= 0) {
                versionName = versionName.substring(0, index);
            }
            findPreference("version_info").setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG,e.toString());
        }

        updateZslPreference();
        updateVideoEncoderProfile();
        updateBitrateCQModePref();
        updateSwitchIDInModePreference(true);
        updateTimeLapsePreference();
        updateAudioEncoderPreference();
        updateVideoFlipPreference();
        updateAIDEPreference();
        updatePdnetTogglePreference();
        updateRawFormatPref();
        updateRawInfoPref();
        updatePictureSizePreferenceButton();
        updateCaptureProfilePref();
        updateMultiResReprocess();
        updatePreviewStabilizationPreference();
        updateViullPreference();
        updateLowLightBoostPreference();
        updateHfrBufferMode();
        updateInSensorZoom();
        updateVSRPreference();
    }
    public void updateHfrBufferMode() {
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_HFR_BUFFER_MODE);
        if (pref == null) {
            return;
        }
        if (mSettingsManager.isSupportedSuperBuffer(mSettingsManager.getCurrentCameraId())) {
            pref.setEnabled(true);
        } else {
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

    private void updateBitrateCQModePref() {
        String encoder  = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER);
        ListPreference btCQPref = (ListPreference)findPreference(SettingsManager.KEY_BITRATE_CQMODE);
        if (btCQPref != null) {
            if (("apv").equals(encoder)) {
                btCQPref.setEnabled(true);
            } else {
                btCQPref.setEnabled(false);
            }
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
            //AIBokeh is not supported on KNP
/*            if (mode == CaptureModule.CameraMode.VIDEO && mSettingsManager.getCurrentCameraId() != CaptureModule.FRONT_ID) {
                key.add("Single Rear AIbokeh");
                value.add("single_rear_aibokeh");
            }
            if((mSettingsManager.getCurrentCameraId() == CaptureModule.FRONT_ID || !CaptureModule.MCXMODE) && mode == CaptureModule.CameraMode.VIDEO){
                key = new ArrayList<String>(Arrays.asList("Default", "RTB"));
                value = new ArrayList<String>(Arrays.asList( "default", "rtb"));
            }

 */
            if((mSettingsManager.getCurrentCameraId() == CaptureModule.FRONT_ID || !CaptureModule.MCXMODE) && mode == CaptureModule.CameraMode.VIDEO){
                key = new ArrayList<String>(Arrays.asList("Default"));
                value = new ArrayList<String>(Arrays.asList( "default"));
            }
            CharSequence[]entries = key.toArray(new CharSequence[key.size()]);
            CharSequence[] values = value.toArray(new CharSequence[value.size()]);
            pref.setEntries(entries);
            pref.setEntryValues(values);
            mSettingsManager.setEntries(SettingsManager.KEY_SELECT_MODE,entries);
            mSettingsManager.setValues(SettingsManager.KEY_SELECT_MODE,values);
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
            } else if (mSettingsManager.getQuadBayerSensorPrefEnabled()) {
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
        if (mSettingsManager.isLimitedHDR()) {
            pref.setEnabled(false);
            pref.setValue("-1");
        }
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
                CharSequence[]entries = list.toArray(new CharSequence[list.size()]);
                CharSequence[] value = values.toArray(new CharSequence[values.size()]);
                colorSpacePref.setEntries(entries);
                colorSpacePref.setEntryValues(value);
                mSettingsManager.setEntries(SettingsManager.KEY_COLOR_SPACE,entries);
                mSettingsManager.setValues(SettingsManager.KEY_COLOR_SPACE,value);
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
        if(mSettingsManager.getValueIndex(SettingsManager.KEY_SCENE_MODE) == 1 ) {
            pref.setValue("off");
            pref.setEnabled(false);
            return;
        }

        String videoSizeStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        String hdrmode = mSettingsManager.getVideoHdrMode();
        int videoSize = CameraUtil.getSize(videoSizeStr);
        if(videoSize >= 7680*4320 && hdrmode != null && (hdrmode.indexOf("MFHDR")>=0)){
            pref.setValue("off");
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
                mSettingsManager.setPreferenceValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE_ENABLED,"false");
                return;
            } else if (mode == CaptureModule.CameraMode.VIDEO) {
                String hdrmode = mSettingsManager.getVideoHdrMode();
                if (hdrmode.toLowerCase().contains("mfhdr")) {
                    pref.setValue("off");
                    pref.setEnabled(false);
                    mSettingsManager.setPreferenceValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE_ENABLED,"false");
                    return;
                }
            }
            ListPreference lapsepref = (ListPreference)findPreference(
                    SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
            if(lapsepref != null){
                String lapsvalue = lapsepref.getValue();
                if(lapsvalue != null && !lapsvalue.equals("0")){
                    pref.setValue("off");
                    pref.setEnabled(false);
                    mSettingsManager.setPreferenceValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE_ENABLED,"false");
                    return;
                }
            }
        }
        if (pref.isEnabled()) {
            mSettingsManager.setPreferenceValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE_ENABLED,"true");
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

    private void disableVIULLOption(ListPreference pref){
        mSettingsManager.setPreferenceValue(SettingsManager.KEY_VIULL_ORIGINAL_VALUE, pref.getValue());
        pref.setValue("0");
        pref.setEnabled(false);
        mViullEnabled = false;
    }

    private boolean is8KVideo(){
        String videoSizeStr = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_QUALITY);
        int videoSize = CameraUtil.getSize(videoSizeStr);
        if(videoSize == 7680*4320){
            return true;
        }
        return false;
    }
    private void updateVSRPreference(){
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_VSR);
        if (pref == null) return;
        if(is8KVideo()){
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }
        pref.setEnabled(true);
    }
    private void updateViullPreference() {
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_VIULL);
        if (pref == null) return;
        CaptureModule.CameraMode mode = (CaptureModule.CameraMode) getIntent().getSerializableExtra(CAMERA_MODULE);
        String selectMode = mSettingsManager.getValue(mSettingsManager.KEY_SELECT_MODE);
        if (selectMode.equals("rtb") && mode == CaptureModule.CameraMode.VIDEO) {
            disableVIULLOption(pref);
            return;
        }

        String profile = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_ENCODER_PROFILE);
        String previewProfile = mSettingsManager.getValue(SettingsManager.KEY_PREVIEW_PROFILE);
        if (profile != null && previewProfile != null && !(SettingsManager.VIDEO_ENCODER_PROFILE_MAP.get(profile).equals(previewProfile))) {
            disableVIULLOption(pref);
            return;
        }

        String qllStr = mSettingsManager.getValue(SettingsManager.KEY_QLL);
        if (qllStr != null && qllStr.equals("1")) {
            disableVIULLOption(pref);
            return;
        }
        String videoFps = mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE);
        String vsr = mSettingsManager.getValue(SettingsManager.KEY_VSR);
        if (mode == CaptureModule.CameraMode.VIDEO && videoFps != null && !videoFps.equals("off") && !("1").equals(vsr)) {
            disableVIULLOption(pref);
            return;
        }

        if(mode == CaptureModule.CameraMode.VIDEO && is8KVideo()){
            disableVIULLOption(pref);
            return;
        }
        Log.i(TAG,"set viull original value:" + mSettingsManager.getPerfValue(SettingsManager.KEY_VIULL_ORIGINAL_VALUE));
        if(!mSettingsManager.getPerfValue(SettingsManager.KEY_VIULL_ORIGINAL_VALUE).equals("") &&
                !mSettingsManager.getPerfValue(SettingsManager.KEY_VIULL_ORIGINAL_VALUE).equals("disable") &&
                !mViullEnabled) {
            pref.setValue(mSettingsManager.getPerfValue(SettingsManager.KEY_VIULL_ORIGINAL_VALUE));
        }
        pref.setEnabled(true);
        mViullEnabled = true;
    }

    public void updateVIULLDefaultValue(){
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_VIULL);
        if (pref == null || !pref.isEnabled()) return;
        if(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE).equals("hsr60")||
                mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE).equals("hfr60")) {
            pref.setValue("0");
        }else if(mSettingsManager.getValue(SettingsManager.KEY_VIDEO_HIGH_FRAME_RATE).equals("off")){
            pref.setValue("1");
        }
    }

    private  void updateLowLightBoostPreference(){
        ListPreference pref = (ListPreference) findPreference(SettingsManager.KEY_LOWLIGHT_BOOST);
        if (pref == null) return;
        if(!mSettingsManager.isLowLightBoostSupported()){
            pref.setValue("0");
            pref.setEnabled(false);
            return;
        }
        String viull = mSettingsManager.getValue(SettingsManager.KEY_VIULL);
        if (viull.equals("0")) {
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
        if (!changeEIS) {
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
                CharSequence[]entries = list.toArray(new CharSequence[list.size()]);
                CharSequence[] value = values.toArray(new CharSequence[values.size()]);
                eisPref.setEntries(entries);
                eisPref.setEntryValues(value);
                mSettingsManager.setEntries(SettingsManager.KEY_EIS_VALUE,entries);
                mSettingsManager.setValues(SettingsManager.KEY_EIS_VALUE,value);
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
        zoomEntriesLists.add("Keep zoom when pause");
        zoomEntriesLists.add("Reset zoom when pause");
        for (int i = 2; i< zoomLevelLists.size(); i++) {
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
            if(rawFormatPref != null) {
                rawFormatPref.setEnabled(true);
                CharSequence[] fullEntries = getResources().getStringArray(
                        R.array.pref_camera2_raw_format_entries);
                CharSequence[] fullEntryValues = getResources().getStringArray(
                        R.array.pref_camera2_raw_format_entryvalues);
                if(mSettingsManager.getSavePictureFormat() == SettingsManager.JPEG_R_FORMAT) {
                    fullEntries = new CharSequence[]{"off", "Raw10", "Raw16(DNG)"};
                    fullEntryValues = new CharSequence[]{"0", "10", "16"};
                }
                rawFormatPref.setEntries(fullEntries);
                rawFormatPref.setEntryValues(fullEntryValues);
                int idx = rawFormatPref.findIndexOfValue(rawFormatPref.getValue());;
                if (idx < 0 ) {
                    idx = 0;
                }
                rawFormatPref.setValueIndex(idx);
            }
        }

    }
    private void updateInSensorZoom(){
        ListPreference inSenorZoomPref = (ListPreference)findPreference(SettingsManager.KEY_INSENSOR_ZOOM);
        if(inSenorZoomPref == null) return;
        if(inSenorZoomPref != null  && mSettingsManager.getQuadBayerSensorPrefEnabled()){
            inSenorZoomPref.setValue("0");
            inSenorZoomPref.setEnabled(false);
            return;
        }
        inSenorZoomPref.setEnabled(true);
    }

    private void updateRawInfoPref(){
        String reprocessType = mSettingsManager.getValue(SettingsManager.KEY_RAW_REPROCESS_TYPE);
        ListPreference rawInfoPref = (ListPreference)findPreference(SettingsManager.KEY_RAWINFO_TYPE);
        if(reprocessType != null && !reprocessType.equals("disable") && !reprocessType.equals("off") && Integer.valueOf(reprocessType) != 0){
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
        }else{
            if (rawInfoPref != null) {
                rawInfoPref.setValue("0");
                rawInfoPref.setEnabled(false);
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
        updatePreference(SettingsManager.KEY_PICTURE_FORMAT);
        if (mSettingsManager.getQuadBayerSensorPrefEnabled() ||(CaptureModule.CameraMode.RTB == mode && isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE))) {
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
                if (isPrefEnabled(SettingsManager.KEY_CAPTURE_MFNR_VALUE)) {
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
        if(mManualHDRDialog != null && mManualHDRDialog.isShowing()) {
            mManualHDRDialog.dismiss();
        }
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


    private void scrollToPreference(String key) {
                int position = findPreferencePosition(key);
                if (position != -1) {
                    ListView listView = getListView();
                    listView.setSelection(position);
                    listView.setItemChecked(position, true);
                }
        }


    private int findPreferencePosition(String str) {
        PreferenceGroup preferenceGroup = getPreferenceScreen();
        int position = 1;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(i);
            for (int j = 0; j < category.getPreferenceCount(); j++) {
                Preference pref = category.getPreference(j);
                if (pref.getTitle().equals(str)) {
                    return position;
                }
                position ++;
            }
            position ++;
        }
        return -1;
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
                                "when Video FPS >=60 or enabled SaveRaw " +
                                " or quadBayerSensor or videoSize >=8k");
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
