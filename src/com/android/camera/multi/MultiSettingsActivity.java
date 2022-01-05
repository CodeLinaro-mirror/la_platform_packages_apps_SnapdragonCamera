/*
 * Copyright (c) 2016-2017, 2021, The Linux Foundation. All rights reserved.
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


package com.android.camera.multi;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.VideoCapabilities;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.ArraySet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.codeaurora.snapcam.R;
import com.android.camera.ComboPreferences;
import com.android.camera.CameraSettings;
import com.android.camera.SettingsManager;
import com.android.camera.util.SettingTranslation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class MultiSettingsActivity extends PreferenceActivity {

    private static final String TAG = "MultiSettingsActivity";

    public static final String CAMERA_MODULE = "camera_module";

    // Preview Size settings
    public static final int PREVIEW_WIDTH = 540;
    public static final int PREVIEW_WIDTH_4_3 = 720;
    public static final int PREVIEW_WIDTH_16_9 = 960;
    public static final int PREVIEW_HIEGHT_1_1 = 540;
    public static final int PREVIEW_HIEGHT_4_3 = 480;
    public static final int PREVIEW_HIEGHT_16_9 = 540;

    // capture settings
    public static final String KEY_HAL_ZAL = "pref_multi_camera_hal_zsl_key";
    public static final String KEY_MULTI_FACE_DETECTION = "pref_multi_camera_facedetection_key";
    public static final String KEY_PICTURE_SIZE_ = "Picture_size_of_camera_";
    public static final String KEY_PICTURE_SIZE_1 = "pref_multi_camera_picturesize1_key";
    public static final String KEY_PICTURE_SIZE_2 = "pref_multi_camera_picturesize2_key";
    public static final String KEY_PICTURE_SIZE_3 = "pref_multi_camera_picturesize3_key";
    public static final String KEY_PICTURE_SIZE_4 = "pref_multi_camera_picturesize4_key";
    public static final String KEY_PICTURE_QUALITY = "pref_multi_camera_jpegquality_key";
    public static final String KEY_SHUTTER_SOUND = "pref_multi_camera_shutter_sound_key";
    public static final String KEY_CAPTURE_MFNR_VALUE = "pref_multi_camera_capture_mfnr_key";

    // video settings
    public static final String KEY_VIDEO_SIZE_ = "Video_size_of_camera_";
    public static final String KEY_VIDEO_SIZE_1 = "pref_multi_camera_video_quality1_key";
    public static final String KEY_VIDEO_SIZE_2 = "pref_multi_camera_video_quality2_key";
    public static final String KEY_VIDEO_SIZE_3 = "pref_multi_camera_video_quality3_key";
    public static final String KEY_VIDEO_SIZE_4 = "pref_multi_camera_video_quality4_key";
    public static final String KEY_VIDEO_ENCODER_ = "Video_Encoder_";
    public static final String KEY_VIDEO_ENCODER_1 = "pref_multi_camera_videoencoder1_key";
    public static final String KEY_VIDEO_ENCODER_2 = "pref_multi_camera_videoencoder2_key";
    public static final String KEY_VIDEO_ENCODER_PROFILE_ = "Encoder_Profile_";
    public static final String KEY_VIDEO_ENCODER_PROFILE_1 = "pref_multi_camera_videoencoderprofile1_key";
    public static final String KEY_VIDEO_ENCODER_PROFILE_2 = "pref_multi_camera_videoencoderprofile2_key";
    public static final String KEY_VIDEO_DURATION = "pref_multi_camera_video_duration_key";
    public static final String KEY_AUDIO_ENCODER = "pref_multi_camera_audioencoder_key";
    public static final String KEY_VIDEO_ROTATION = "pref_multi_camera_video_rotation_key";
    public static final String KEY_VIDEO_EIS = "pref__multi_camera_eis_key";

    private static final String KEY_RESTORE_DEFAULT = "pref_multi_camera_restore_default";
    private static final String KEY_VERSION_INFO = "multi_camera_version_info";
    public static final String KEY_MULTI_CAMERAS_MODE = "pref_camera2_multi_cameras_key";
    public static final String KEY_CONCURRENT_CAMERA = "pref_camera2_concurrent_camera_key";
    public static final HashMap<Integer, String> KEY_PICTURE_SIZES = new HashMap<Integer, String>();
    public static final HashMap<Integer, String> KEY_VIDEO_SIZES = new HashMap<Integer, String>();
    private static Map<String, Set<String>> VIDEO_ENCODER_PROFILE_TABLE = new HashMap<>();

    private SharedPreferences mSharedPreferences;
    private SharedPreferences mLocalSharedPref;
    private SettingsManager mSettingsManager;
    private MultiCameraModule.CameraMode mMultiCameraMode;

    private ArrayList<CameraCharacteristics> mCharacteristics;

    public Set<Set<String>> mConcurrentCameraIdCombinations;
    public CharSequence[] mConcurrentEntries;
    public CharSequence[] mConcurrentEntryValues;
    public String[] mCameraIds;
    public String[] mConcurrentIds;

    static {
        Set<String> h265 = new HashSet<>();
        h265.add("HEVCProfileMain10");
        h265.add("HEVCProfileMain10HDR10");
        h265.add("HEVCProfileMain10HDR10Plus");
        VIDEO_ENCODER_PROFILE_TABLE.put("h265", h265);
    }


    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            Preference p = findPreference(key);
            if (p == null) return;

            Log.v(TAG, " onSharedPreferenceChanged key :" + key );
            SharedPreferences.Editor editor = mLocalSharedPref.edit();
            String value;
            if (p instanceof SwitchPreference) {
                boolean checked = ((SwitchPreference) p).isChecked();
                value = checked ? "on" : "off";
                editor.putBoolean(key, checked);
            } else if (p instanceof ListPreference){
                value = ((ListPreference) p).getValue();
                String title = (String)((ListPreference) p).getTitle();
                if (key.contains("pref_multi_camera_picturesize") ||
                        key.contains("pref_multi_camera_video_quality") ||
                        key.contains("pref_multi_camera_videoencoder") ||
                        key.contains("pref_multi_camera_videoencoderprofile")) {
                    if (key.contains("pref_multi_camera_videoencoder") && value.equals("h264")) {
                        String id = title.substring(title.length() - 1);
                        editor.putString(KEY_VIDEO_ENCODER_PROFILE_ + id, "off");
                    }
                    editor.putString(title, value);
                } else {
                    editor.putString(key, value);
                }
            } else if (p instanceof MultiSelectListPreference) {
                Set<String> valueSet = ((MultiSelectListPreference)p).getValues();
                if (key.equals(KEY_CONCURRENT_CAMERA)){
                    if (checkIfConcurrentIdsSupported(valueSet) || valueSet.size() == 1){
                        editor.putStringSet(key,valueSet);
                        mConcurrentIds = new String[valueSet.size()];
                        mConcurrentIds = valueSet.toArray(mConcurrentIds);
                        initializePictureSize1();
                        initializePictureSize2();
                        initializePictureSize3();
                        initializePictureSize4();

                        initializeVideoSize1();
                        initializeVideoSize2();
                        initializeVideoSize3();
                        initializeVideoSize4();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.pref_camera2_concurrent_camera_not_support,
                                Toast.LENGTH_SHORT).show();
                        Set<String> orignalValeus = mLocalSharedPref.getStringSet(
                                KEY_CONCURRENT_CAMERA,null);
                        if (orignalValeus != null)
                            ((MultiSelectListPreference)p).setValues(orignalValeus);
                    }
                }
            }

            if (key.equals(KEY_VIDEO_ENCODER_1)) {
                updateVideoEncoderProfile(0, KEY_VIDEO_ENCODER_1);
            } else if (key.equals(KEY_VIDEO_ENCODER_2)) {
                updateVideoEncoderProfile(1, KEY_VIDEO_ENCODER_2);
            }
            editor.commit();

            if (key.equals(KEY_VIDEO_EIS)) {
                updateVideQuality();
            }

            if (key.equals(KEY_MULTI_CAMERAS_MODE)) {
                String multiEnable = ((ListPreference) p).getValue();
                String multiCamerasOff = MultiSettingsActivity.this.getResources().getString(
                        R.string.pref_camera2_multi_cameras_value_off);
                if (multiEnable.equals(multiCamerasOff)) {
                    mSettingsManager.setValue(KEY_MULTI_CAMERAS_MODE, multiCamerasOff);
                }
            }
        }
    };

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

        mSettingsManager = SettingsManager.getInstance();
        if (mSettingsManager == null) {
            finish();
            return;
        }

        mMultiCameraMode = (MultiCameraModule.CameraMode) getIntent().getSerializableExtra(
                CAMERA_MODULE);
        mSharedPreferences = getPreferenceManager().getSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        mLocalSharedPref = this.getSharedPreferences(
                ComboPreferences.getLocalSharedPreferencesName(this,
                        "multi" + mMultiCameraMode), Context.MODE_PRIVATE);


        initializeCameraCharacteristics();
        addPreferencesFromResource(R.xml.multi_setting_menu_preferences);
        filterPreferences();
        initializePreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    private void setShowInLockScreen() {
        // Change the window flags so that secure camera can show when locked
        Window win = getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        win.setAttributes(params);
    }

    private void initializeCameraCharacteristics() {
        mCharacteristics = new ArrayList<>();
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            mConcurrentCameraIdCombinations = manager.getConcurrentCameraIds();
            Log.d(TAG,"mConcurrentCameraIdCombinations="+mConcurrentCameraIdCombinations.toString());
            mCameraIds = cameraIdList;
            boolean isFirstBackCameraId = true;
            boolean isRearCameraPresent = false;
            int index = 0;
            int size = 0;
            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                for (Set<String> set: mConcurrentCameraIdCombinations) {
                    if(set.contains(cameraId)) {
                        size ++;
                        break;
                    }
                }
            }
            mConcurrentEntries = new CharSequence[size];
            mConcurrentEntryValues = new CharSequence[size];
            Log.d(TAG, "cameraIdList size =" + cameraIdList.length + ", size :" + size);
            for (int i = 0; i < cameraIdList.length; i++) {
                String cameraId = cameraIdList[i];
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);
                Log.d(TAG, " cameraId :" + cameraId + ", i :" + i + ", index :" + index);
                for (Set<String> set: mConcurrentCameraIdCombinations) {
                    if(set.contains(cameraId)) {
                        mCharacteristics.add(index, characteristics);
                        mConcurrentEntries[index] = "cameraId: "+cameraId+" facing:"+(
                                characteristics.get(CameraCharacteristics.LENS_FACING) ==
                                        CameraCharacteristics.LENS_FACING_FRONT ? "front" : "back");
                        mConcurrentEntryValues[index] = cameraId;
                        Log.d(TAG, " add cameraId :" + cameraId + ", index :" + index);
                        index ++;
                        break;
                    }
                }
            }

            for (Set<String> set: mConcurrentCameraIdCombinations){
                Log.d(TAG,"set="+set.toString());
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initializePreferences() {
        setOnPreferenceClick();

        initializeConcurrentCameraIds();

        // capture settings
        initializeFaceDetection();
        initializeHalZSLPref();
        initializePictureQuality();
        initializePictureSize1();
        initializePictureSize2();
        initializePictureSize3();
        initializePictureSize4();
        initializeShutterSound();

        // video settins
        initializeVideoSize1();
        initializeVideoSize2();
        initializeVideoSize3();
        initializeVideoSize4();
        initializeVideoDuration();
        initializeAudioEncoder();
        initializeVideoRotation();

        initializeVersionInfo();
    }

    private void filterPreferences() {
        PreferenceGroup developer = (PreferenceGroup) findPreference("developer");
        PreferenceGroup photoPre = (PreferenceGroup) findPreference("photo");
        PreferenceGroup videoPre = (PreferenceGroup) findPreference("video");
        PreferenceScreen parentPre = getPreferenceScreen();

        switch (mMultiCameraMode) {
            case DEFAULT:
                removePreferenceGroup("video", parentPre);
                break;
            case VIDEO:
                removePreferenceGroup("photo", parentPre);
                break;
            default:
                //don't filter
                break;
        }
        filterVideoEncoderOptions();
    }

    private void updateVideoEncoderProfile(int index, String key) {
        ListPreference pref = (ListPreference) findPreference(key);
        if (pref == null) {
            return;
        }
        filterVideoEncoderProfileOptions(index, pref.getValue());
    }

    private void filterVideoEncoderOptions() {
        ListPreference videoEncoder_1 = (ListPreference)findPreference(KEY_VIDEO_ENCODER_1);
        ListPreference videoEncoder_2 = (ListPreference)findPreference(KEY_VIDEO_ENCODER_2);
        String videoSize = null;
        String cameraId = "0";
        String defaultSize = this.getString(R.string.pref_multi_camera_picturesize_default);
        if (mConcurrentIds != null && mConcurrentIds.length >0 && mConcurrentIds[0] != null){
            cameraId = mConcurrentIds[0];
        }
        if (mLocalSharedPref != null) {
            videoSize = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, defaultSize);
        }

        if (videoEncoder_1 != null) {
            filterUnsupported(KEY_VIDEO_ENCODER_1, getSupportedVideoEncoders(videoSize));
        }

        if (mConcurrentIds != null && mConcurrentIds.length >1 && mConcurrentIds[0] != null){
            cameraId = mConcurrentIds[1];
        }
        if (mLocalSharedPref != null) {
            videoSize = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, defaultSize);
        }

        if (videoEncoder_2 != null) {
            filterUnsupported(KEY_VIDEO_ENCODER_2, getSupportedVideoEncoders(videoSize));
        }
    }

    private List<String> getSupportedVideoEncoders(String videoSize) {
        ArrayList<String> supported = new ArrayList<String>();
        supported.add(SettingTranslation.getVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT));
        String str = null;
        MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = list.getCodecInfos();
        for (MediaCodecInfo info: codecInfos) {
            if (!info.isEncoder() || info.getName().contains("google")) continue;
            Log.d(TAG, "name= "+info.getName());
            if (info.getSupportedTypes().length > 0 && info.getSupportedTypes()[0] != null){
                for (String t : info.getSupportedTypes()){
                    Log.d(TAG, "type= "+t);
                }
                int type = SettingTranslation.getVideoEncoderType(info.getSupportedTypes()[0]);
                if (type != -1){
                    str = SettingTranslation.getVideoEncoder(type);
                    Log.d(TAG,"type="+type+" str="+str);
                    if (isCurrentVideoResolutionSupportedByEncoder(info, videoSize)) {
                        supported.add(str);
                    }
                }
            }
        }
        return supported;
    }

    private boolean isCurrentVideoResolutionSupportedByEncoder(MediaCodecInfo info, String videoSizeStr) {
        boolean supported = false;
        if (videoSizeStr != null) {
            Size videoSize = parseSize(videoSizeStr);
            String[] supportedTypes = info.getSupportedTypes();
            MediaCodecInfo.VideoCapabilities capabilities = null;
            for (String type : supportedTypes) {
                if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4)
                        || type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263)
                        || type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)
                        || type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                    capabilities = info.getCapabilitiesForType(type).getVideoCapabilities();
                    if (capabilities == null ||
                            !capabilities.getSupportedWidths().contains(videoSize.getWidth()) ||
                            !capabilities.getSupportedWidths().contains(videoSize.getHeight())) {
                        return false;
                    } else {
                        supported = true;
                    }
                }
            }
        }
        return supported;
    }

    private Size parseSize(String value) {
        int indexX = value.indexOf('x');
        int width = Integer.parseInt(value.substring(0, indexX));
        int height = Integer.parseInt(value.substring(indexX + 1));
        return new Size(width, height);
    }

    private void filterVideoEncoderProfileOptions(int index, String videoEncoder) {
        if (index == 0) {
            ListPreference videoEncoderProfilePref1 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_PROFILE_1);
            ListPreference videoEncoderPref1 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_1);
            if ( videoEncoderProfilePref1 != null && videoEncoderPref1 != null ) {
                filterUnsupported(KEY_VIDEO_ENCODER_PROFILE_1,
                        getSupportedVideoEncoderProfile(videoEncoder),
                        R.array.pref_camera2_videoencoderprofile_entry,
                        R.array.pref_camera2_videoencoderprofile_entryvalues);
            }
        }

        if (index == 1) {
            ListPreference videoEncoderProfilePref2 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_PROFILE_2);
            ListPreference videoEncoderPref2 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_2);
            if ( videoEncoderProfilePref2 != null && videoEncoderPref2 != null ) {
                filterUnsupported(KEY_VIDEO_ENCODER_PROFILE_2,
                        getSupportedVideoEncoderProfile(videoEncoder),
                        R.array.pref_camera2_videoencoderprofile_entry,
                        R.array.pref_camera2_videoencoderprofile_entryvalues);
            }
        }
    }

    public List<String> getSupportedVideoEncoderProfile(String videoEncoder) {
        List<String> profile = new ArrayList<>();
        profile.add("off");
        if ( VIDEO_ENCODER_PROFILE_TABLE.containsKey(videoEncoder) ) {
            profile.addAll(VIDEO_ENCODER_PROFILE_TABLE.get(videoEncoder));
        }
        return profile;
    }

    private void setOnPreferenceClick() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(i);
            for (int j = 0; j < category.getPreferenceCount(); j++) {
                Preference pref = category.getPreference(j);
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (preference.getKey().equals(KEY_RESTORE_DEFAULT)) {
                            onRestoreDefaultSettingsClick();
                        }
                        return false;
                    }

                });
            }
        }
    }

    private void initializeConcurrentCameraIds(){
        Set<String> cameraIds = new ArraySet<>();
        if (mLocalSharedPref != null){
            cameraIds = mLocalSharedPref.getStringSet(KEY_CONCURRENT_CAMERA,null);
        }
        MultiSelectListPreference concurrentCameraIds = (MultiSelectListPreference)findPreference(
                KEY_CONCURRENT_CAMERA);
        if (concurrentCameraIds != null){
            concurrentCameraIds.setEntries(mConcurrentEntries);
            concurrentCameraIds.setEntryValues(mConcurrentEntryValues);
            if (cameraIds != null && cameraIds.size() != 0) {
                concurrentCameraIds.setValues(cameraIds);
                mConcurrentIds = new String[cameraIds.size()];
                mConcurrentIds = cameraIds.toArray(mConcurrentIds);
            } else {
                if (cameraIds == null) {
                    cameraIds = new ArraySet<>();
                }
                cameraIds.add("0");
                concurrentCameraIds.setValues(cameraIds);
            }
        }
    }

    private boolean checkIfConcurrentIdsSupported(Set<String> ids){
        boolean ret = false;
        if (mConcurrentCameraIdCombinations != null){
            if (mConcurrentCameraIdCombinations.contains(ids))
                ret = true;
        }
        return ret;
    }

    private void initializeHalZSLPref() {
        boolean isCheck = false;
        if (mLocalSharedPref != null) {
            isCheck = mLocalSharedPref.getBoolean(KEY_HAL_ZAL, false);
        }
        SwitchPreference halZSLPref = (SwitchPreference)findPreference(KEY_HAL_ZAL);
        if (halZSLPref != null) {
            halZSLPref.setChecked(isCheck);
        }
    }

    private void initializeFaceDetection() {
        boolean isCheck = false;
        if (mLocalSharedPref != null) {
            isCheck = mLocalSharedPref.getBoolean(KEY_MULTI_FACE_DETECTION, false);
        }
        SwitchPreference faceDetection = (SwitchPreference)findPreference(KEY_MULTI_FACE_DETECTION);
        if (faceDetection != null) {
            faceDetection.setChecked(isCheck);
        }
    }

    private boolean isFaceDetectionSupported(int id) {
        int[] faceDetection = mCharacteristics.get(id).get
                (CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        if (faceDetection != null) {
            for (int value: faceDetection) {
                if (value == CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE)
                    return true;
            }
        }
        return false;
    }

    private void initializePictureQuality() {
        String quality = this.getString(R.string.pref_camera_jpegquality_default);
        if (mLocalSharedPref != null) {
            quality = mLocalSharedPref.getString(KEY_PICTURE_QUALITY, quality);
        }
        ListPreference pictureQualityPref = (ListPreference) findPreference(KEY_PICTURE_QUALITY);
        if (pictureQualityPref != null) {
            pictureQualityPref.setValue(quality);
        }
    }

    private void initializePictureSize1() {
        String size = null;
        String cameraId = "0";
        ListPreference pictureSizePref = (ListPreference) findPreference(KEY_PICTURE_SIZE_1);
        if (mConcurrentIds != null && mConcurrentIds.length >0 && mConcurrentIds[0] != null){
            cameraId = mConcurrentIds[0];
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_picturesize_default);
        filterUnsupported(KEY_PICTURE_SIZE_1, getSupportedPictureSize(Integer.valueOf(cameraId)));

        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, defaultSize);
        }
        if (pictureSizePref != null) {
            pictureSizePref.setTitle(KEY_PICTURE_SIZE_+cameraId);
            try {
                pictureSizePref.setValue(size);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializePictureSize2() {
        String size = null;
        String cameraId = "1";
        ListPreference pictureSizePref = (ListPreference) findPreference(KEY_PICTURE_SIZE_2);
        if (mConcurrentIds != null && mConcurrentIds.length >1 && mConcurrentIds[1] != null){
            cameraId = mConcurrentIds[1];
        } else {
            if (pictureSizePref != null)
                pictureSizePref.setEnabled(false);
            return;
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_picturesize_default);
        filterUnsupported(KEY_PICTURE_SIZE_2, getSupportedPictureSize(Integer.valueOf(cameraId)));

        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, defaultSize);
        }
        if (pictureSizePref != null) {
            pictureSizePref.setEnabled(true);
            pictureSizePref.setTitle(KEY_PICTURE_SIZE_ + cameraId);
            try {
                pictureSizePref.setValue(size);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializePictureSize3() {
        String size = null;
        String cameraId = "2";
        ListPreference pictureSizePref = (ListPreference) findPreference(KEY_PICTURE_SIZE_3);
        PreferenceGroup photoPre = (PreferenceGroup) findPreference("photo");
        if (mConcurrentIds != null && mConcurrentIds.length >2 && mConcurrentIds[2] != null){
            cameraId = mConcurrentIds[2];
        } else {
            if (pictureSizePref != null) {
                pictureSizePref.setEnabled(false);
                removePreferenceGroup(KEY_PICTURE_SIZE_3, photoPre);
            }
            return;
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_picturesize_default);
        filterUnsupported(KEY_PICTURE_SIZE_3, getSupportedPictureSize(Integer.valueOf(cameraId)));
        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, size);
        }
        if (pictureSizePref != null) {
            pictureSizePref.setEnabled(true);
            pictureSizePref.setTitle(KEY_PICTURE_SIZE_ + cameraId);
            try {
                if (size == null) {
                    pictureSizePref.setValue(defaultSize);
                } else {
                    pictureSizePref.setValue(size);
                }
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializePictureSize4() {
        String size = null;
        String cameraId = "3";
        ListPreference pictureSizePref = (ListPreference) findPreference(KEY_PICTURE_SIZE_4);
        PreferenceGroup photoPre = (PreferenceGroup) findPreference("photo");
        if (mConcurrentIds != null && mConcurrentIds.length >3 && mConcurrentIds[3] != null){
            cameraId = mConcurrentIds[3];
        } else {
            if (pictureSizePref != null) {
                pictureSizePref.setEnabled(false);
                removePreferenceGroup(KEY_PICTURE_SIZE_4, photoPre);
            }
            return;
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_picturesize_default);
        filterUnsupported(KEY_PICTURE_SIZE_4, getSupportedPictureSize(Integer.valueOf(cameraId)));
        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_PICTURE_SIZE_ + cameraId, size);
        }
        if (pictureSizePref != null) {
            pictureSizePref.setEnabled(true);
            pictureSizePref.setTitle(KEY_PICTURE_SIZE_ + cameraId);
            try {
                if (size == null) {
                    pictureSizePref.setValue(defaultSize);
                } else {
                    pictureSizePref.setValue(size);
                }
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeShutterSound() {
        boolean isCheck = false;
        if (mLocalSharedPref != null) {
            isCheck = mLocalSharedPref.getBoolean(KEY_SHUTTER_SOUND, true);
        }
        SwitchPreference shutterSoundPref = (SwitchPreference)findPreference(KEY_SHUTTER_SOUND);
        if (shutterSoundPref != null) {
            shutterSoundPref.setChecked(isCheck);
        }
    }

    private void updateVideQuality() {
        String cameraId = "0";
        ListPreference videoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE_1);
        ListPreference videoSizePref2 = (ListPreference) findPreference(KEY_VIDEO_SIZE_2);
        if (mConcurrentIds != null && mConcurrentIds.length >0 && mConcurrentIds[0] != null){
            cameraId = mConcurrentIds[0];
            filterUnsupported(KEY_VIDEO_SIZE_1, getSupportedVideoSize(Integer.valueOf(cameraId)),
                    R.array.pref_camera2_video_quality_entries,
                    R.array.pref_camera2_video_quality_entryvalues);
        }
        if (mConcurrentIds != null && mConcurrentIds.length >1 && mConcurrentIds[1] != null) {
            cameraId = mConcurrentIds[1];
            filterUnsupported(KEY_VIDEO_SIZE_2, getSupportedVideoSize(Integer.valueOf(cameraId)),
                    R.array.pref_camera2_video_quality_entries,
                    R.array.pref_camera2_video_quality_entryvalues);
        }
    }

    private void initializeVideoSize1() {
        String size = null;
        String encoder = null;
        String encoderProfile = null;
        String cameraId = "0";

        ListPreference videoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE_1);
        ListPreference videoEncoder_1 = (ListPreference)findPreference(KEY_VIDEO_ENCODER_1);
        ListPreference videoEncoderProfilePref1 = (ListPreference) findPreference(
                KEY_VIDEO_ENCODER_PROFILE_1);

        if (mConcurrentIds != null && mConcurrentIds.length >0 && mConcurrentIds[0] != null){
            cameraId = mConcurrentIds[0];
        }

        filterUnsupported(KEY_VIDEO_SIZE_1, getSupportedVideoSize(Integer.valueOf(cameraId)));
        String defaultSize = this.getString(R.string.pref_multi_camera_video_quality_default);
        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_VIDEO_SIZE_ + cameraId, defaultSize);
        }
        if (videoSizePref != null) {
            videoSizePref.setTitle(KEY_VIDEO_SIZE_ + cameraId);
            try {
                videoSizePref.setValue(size);
            } catch(IndexOutOfBoundsException e) {
            }
        }

        // Video Encoder
        String defaultEncoder = this.getString(R.string.pref_camera_videoencoder_default);
        if (mLocalSharedPref != null) {
            encoder = mLocalSharedPref.getString(KEY_VIDEO_ENCODER_ + cameraId, defaultEncoder);
        }
        if (videoEncoder_1 != null) {
            filterVideoEncoderProfileOptions(Integer.parseInt(cameraId), encoder);
            videoEncoder_1.setTitle(KEY_VIDEO_ENCODER_ + cameraId);
            try {
                videoEncoder_1.setValue(encoder);
            } catch(IndexOutOfBoundsException e) {
            }
        }

        // Video Encoder Profile
        String defaultProfile = this.getString(R.string.pref_camera2_videoencoderprofile_default);
        if (mLocalSharedPref != null) {
            encoderProfile = mLocalSharedPref.getString(KEY_VIDEO_ENCODER_PROFILE_ + cameraId,
                    defaultProfile);
        }
        if (videoEncoderProfilePref1 != null) {
            videoEncoderProfilePref1.setTitle(KEY_VIDEO_ENCODER_PROFILE_ + cameraId);
            try {
                videoEncoderProfilePref1.setValue(encoderProfile);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeVideoSize2() {
        String size = null;
        String encoder = null;
        String encoderProfile = null;
        String cameraId = "1";
        PreferenceGroup videoPre = (PreferenceGroup) findPreference("video");
        ListPreference videoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE_2);
        ListPreference videoEncoder_2 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_2);
        ListPreference videoEncoderProfilePref2 = (ListPreference) findPreference(KEY_VIDEO_ENCODER_PROFILE_2);
        if (mConcurrentIds != null && mConcurrentIds.length >1 && mConcurrentIds[1] != null) {
            cameraId = mConcurrentIds[1];

            String defaultSize = this.getString(R.string.pref_multi_camera_video_quality_default);
            filterUnsupported(KEY_VIDEO_SIZE_2, getSupportedVideoSize(Integer.valueOf(cameraId)));
            if (mLocalSharedPref != null) {
                size = mLocalSharedPref.getString(KEY_VIDEO_SIZE_ + cameraId, defaultSize);
            }
            if (videoSizePref != null) {
                videoSizePref.setEnabled(true);
                videoSizePref.setTitle(KEY_VIDEO_SIZE_ + cameraId);
                try {
                    videoSizePref.setValue(size);
                } catch(IndexOutOfBoundsException e) {
                }
            }

            // Video Encoder
            String defaultEncoder = this.getString(R.string.pref_camera_videoencoder_default);
            if (mLocalSharedPref != null) {
                encoder = mLocalSharedPref.getString(KEY_VIDEO_ENCODER_ + cameraId, defaultEncoder);
            }
            if (videoEncoder_2 != null) {
                videoEncoder_2.setEnabled(true);
                videoEncoder_2.setTitle(KEY_VIDEO_ENCODER_ + cameraId);
                filterVideoEncoderProfileOptions(Integer.parseInt(cameraId), encoder);
                try {
                    videoEncoder_2.setValue(encoder);
                } catch(IndexOutOfBoundsException e) {
                }
            }

            // Video Encoder Profile
            String defaultProfile = this.getString(R.string.pref_camera2_videoencoderprofile_default);
            if (mLocalSharedPref != null) {
                encoderProfile = mLocalSharedPref.getString(KEY_VIDEO_ENCODER_PROFILE_ + cameraId,
                        defaultProfile);
            }
            if (videoEncoderProfilePref2 != null) {
                videoEncoderProfilePref2.setTitle(KEY_VIDEO_ENCODER_PROFILE_ + cameraId);
                try {
                    videoEncoderProfilePref2.setValue(encoderProfile);
                } catch(IndexOutOfBoundsException e) {
                }
            }

        } else {
            if (videoSizePref != null) {
                videoSizePref.setEnabled(false);
                videoSizePref.setTitle(KEY_VIDEO_SIZE_);
            }
            if (videoEncoder_2 != null) {
                videoEncoder_2.setEnabled(false);
                videoEncoder_2.setTitle(KEY_VIDEO_ENCODER_);
            }
            if (videoEncoderProfilePref2 != null) {
                videoEncoderProfilePref2.setEnabled(false);
                videoEncoderProfilePref2.setTitle(KEY_VIDEO_ENCODER_PROFILE_);
            }
        }
    }

    private void initializeVideoSize3() {
        String size = null;
        String cameraId = "2";
        ListPreference videoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE_3);
        PreferenceGroup videoPre = (PreferenceGroup) findPreference("video");

        if (mConcurrentIds != null && mConcurrentIds.length >2 && mConcurrentIds[2] != null){
            cameraId = mConcurrentIds[2];
        } else {
            if (videoSizePref != null) {
                videoSizePref.setEnabled(false);
                removePreferenceGroup(KEY_VIDEO_SIZE_3, videoPre);
            }
            return;
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_video_quality_default);
        filterUnsupported(KEY_VIDEO_SIZE_3, getSupportedVideoSize(Integer.valueOf(cameraId)));
        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_VIDEO_SIZE_ + cameraId, size);
        }
        if (videoSizePref != null) {
            videoSizePref.setEnabled(true);
            videoSizePref.setTitle(KEY_VIDEO_SIZE_ + cameraId);
            try {
                if (size == null) {
                    videoSizePref.setValue(defaultSize);
                } else {
                    videoSizePref.setValue(size);
                }
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeVideoSize4() {
        String size = null;
        String cameraId = "3";
        ListPreference videoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE_4);
        PreferenceGroup videoPre = (PreferenceGroup) findPreference("video");

        if (mConcurrentIds != null && mConcurrentIds.length >3 && mConcurrentIds[3] != null){
            cameraId = mConcurrentIds[3];
        } else {
            if (videoSizePref != null) {
                videoSizePref.setEnabled(false);
                removePreferenceGroup(KEY_VIDEO_SIZE_4, videoPre);
            }
            return;
        }
        String defaultSize = this.getString(R.string.pref_multi_camera_video_quality_default);
        filterUnsupported(KEY_VIDEO_SIZE_4, getSupportedVideoSize(Integer.valueOf(cameraId)));
        if (mLocalSharedPref != null) {
            size = mLocalSharedPref.getString(KEY_VIDEO_SIZE_ + cameraId, size);
        }
        if (videoSizePref != null) {
            videoSizePref.setEnabled(true);
            videoSizePref.setTitle(KEY_VIDEO_SIZE_ + cameraId);
            try {
                if (size == null) {
                    videoSizePref.setValue(defaultSize);
                } else {
                    videoSizePref.setValue(size);
                }
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeVideoDuration() {
        filterUnsupported(KEY_VIDEO_DURATION, getSupportedVideoDuration());
        String duration = MultiSettingsActivity.this.getResources().getString(
                R.string.pref_camera_video_duration_default);
        if (mLocalSharedPref != null) {
            duration = mLocalSharedPref.getString(KEY_VIDEO_DURATION, duration);
        }
        ListPreference videoDurationPref = (ListPreference) findPreference(KEY_VIDEO_DURATION);
        if (videoDurationPref != null) {
            try {
                videoDurationPref.setValue(duration);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeAudioEncoder() {
        String audioEncoder = MultiSettingsActivity.this.getResources().getString(
                R.string.pref_camera_audioencoder_default);
        if (mLocalSharedPref != null) {
            audioEncoder = mLocalSharedPref.getString(KEY_AUDIO_ENCODER, audioEncoder);
        }
        ListPreference audioEncoderPref = (ListPreference) findPreference(KEY_AUDIO_ENCODER);
        if (audioEncoderPref != null) {
            try {
                audioEncoderPref.setValue(audioEncoder);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeVideoRotation() {
        String videoRotation = MultiSettingsActivity.this.getResources().getString(
                R.string.pref_camera_video_rotation_default);
        if (mLocalSharedPref != null) {
            videoRotation = mLocalSharedPref.getString(KEY_VIDEO_ROTATION, videoRotation);
        }
        ListPreference videoRotationPref = (ListPreference) findPreference(KEY_VIDEO_ROTATION);
        if (videoRotationPref != null) {
            try {
                videoRotationPref.setValue(videoRotation);
            } catch(IndexOutOfBoundsException e) {
            }
        }
    }

    private void initializeVersionInfo() {
        // Version Info
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int index = versionName.indexOf(' ');
            versionName = versionName.substring(0, index);
            findPreference(KEY_VERSION_INFO).setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private List<String> getSupportedPictureSize(int cameraId) {
        List<String> res = new ArrayList<>();
        try {
            StreamConfigurationMap map = mCharacteristics.get(cameraId).get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            Size[] highResSizes = map.getHighResolutionOutputSizes(ImageFormat.JPEG);

            if (sizes != null) {
                for (int i = 0; i < sizes.length; i++) {
                    if (sizes[i].getWidth() == sizes[i].getHeight()) continue;
                    res.add(sizes[i].toString());
                }
            }

            if (highResSizes != null) {
                for (int i = 0; i < highResSizes.length; i++) {
                    if (sizes[i].getWidth() == sizes[i].getHeight()) continue;
                    res.add(highResSizes[i].toString());
                }
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return res;
    }

    private List<String> getSupportedVideoSize(int cameraId) {
        List<String> res = new ArrayList<>();
        String defaultValue = this.getResources().getString(R.string.pref_camera2_eis_default);
        String eisValue = mLocalSharedPref.getString(KEY_VIDEO_EIS, defaultValue);
        boolean isEISV3Enabled = "V3".equals(eisValue);
        try {
            StreamConfigurationMap map = mCharacteristics.get(cameraId).get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(MediaRecorder.class);
            if (sizes == null) return res;
            for (int i = 0; i < sizes.length; i++) {
                if (CameraSettings.VIDEO_QUALITY_TABLE.containsKey(sizes[i].toString())) {
                    Integer profile = CameraSettings.VIDEO_QUALITY_TABLE.get(sizes[i].toString());

                    if (isEISV3Enabled && Math.min(sizes[i].getWidth(),sizes[i].getHeight()) < 720) {
                        //video size should't be larger than 720p when EIS V3 is enabled
                        continue;
                    }

                    if (profile != null && CamcorderProfile.hasProfile(cameraId, profile)) {
                        res.add(sizes[i].toString());
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return res;
    }

    private List<String> getSupportedVideoDuration() {
        int[] videoDurations = {-1, 10, 30, 0};
        List<String> modes = new ArrayList<>();
        for (int i : videoDurations) {
            modes.add(""+i);
        }
        return  modes;
    }

    private void filterUnsupported(String key, List<String> supported) {
        ListPreference listPref = (ListPreference) findPreference(key);
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        if (listPref != null) {
            CharSequence[] listEntries = listPref.getEntries();
            CharSequence[] listEntryValues = listPref.getEntryValues();
            for (int i = 0, len = listEntryValues.length; i < len; i++) {
                if (supported.indexOf(listEntryValues[i].toString()) >= 0) {
                    entries.add(listEntries[i]);
                    entryValues.add(listEntryValues[i]);
                }
            }
            int size = entries.size();
            listPref.setEntries(entries.toArray(new CharSequence[size]));
            listPref.setEntryValues(entryValues.toArray(new CharSequence[size]));
            if (size == 1) {
                listPref.setEnabled(false);
            } else {
                listPref.setEnabled(true);
            }
        }
    }

    private void filterUnsupported(String key, List<String> supported, int entriesId, int  entryValuesId) {
        ListPreference listPref = (ListPreference) findPreference(key);
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        if (listPref != null) {
            String[] listEntries = this.getResources().getStringArray(entriesId);
            String[] listEntryValues = this.getResources().getStringArray(entryValuesId);
            for (int i = 0, len = listEntryValues.length; i < len; i++) {
                if (supported.indexOf(listEntryValues[i]) >= 0) {
                    entries.add(listEntries[i]);
                    entryValues.add(listEntryValues[i]);
                }
            }

            int size = entries.size();
            listPref.setEntries(entries.toArray(new CharSequence[size]));
            listPref.setEntryValues(entryValues.toArray(new CharSequence[size]));
            if (size == 1) {
                listPref.setEnabled(false);
            } else {
                listPref.setEnabled(true);
            }
        }
    }

    private boolean removePreference(String key, PreferenceScreen parentPreferenceScreen) {
        PreferenceGroup removePreference = (PreferenceGroup) findPreference(key);
        if (removePreference != null && parentPreferenceScreen != null) {
            parentPreferenceScreen.removePreference(removePreference);
            return true;
        }
        return false;
    }

    private boolean removePreferenceGroup(String key, PreferenceGroup parentPreferenceGroup) {
        Preference removePreference = findPreference(key);
        if (removePreference != null && parentPreferenceGroup != null) {
            parentPreferenceGroup.removePreference(removePreference);
            return true;
        }
        return false;
    }

    private void onRestoreDefaultSettingsClick() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.pref_camera2_restore_default_hint)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        restoreSettings();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restoreSettings() {
        clearPerCameraPreferences();
        filterPreferences();
        initializePreferences();
    }

    private void clearPerCameraPreferences() {
        ArrayList<String> prepNameKeys = new ArrayList<>();
        prepNameKeys.add("multi" + String.valueOf(MultiCameraModule.CameraMode.DEFAULT));
        prepNameKeys.add("multi" + String.valueOf(MultiCameraModule.CameraMode.VIDEO));
        String[] preferencesNames = ComboPreferences.getSharedPreferencesNames(this, prepNameKeys);
        for ( String name : preferencesNames ) {
            SharedPreferences.Editor editor =
                    getSharedPreferences(name, Context.MODE_PRIVATE).edit();
            editor.clear();
            editor.commit();
        }
    }
}
