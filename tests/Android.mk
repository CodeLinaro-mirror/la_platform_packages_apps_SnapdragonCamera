ifneq ($(strip $(SOONG_CONFIG_qticamera_apk)),true)
    LOCAL_PATH := $(call my-dir)
    include $(CLEAR_VARS)
    LOCAL_MODULE_TAGS := optional
    LOCAL_JAVA_LIBRARIES := android.test.runner android.test.base
    LOCAL_STATIC_JAVA_LIBRARIES := \
        junit android-support-test \
        guava
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_VENDOR_MODULE := true
    LOCAL_PRIVILEGED_MODULE := true
    LOCAL_SDK_VERSION := current
    LOCAL_SRC_FILES := $(call all-java-files-under, src)
    LOCAL_PACKAGE_NAME := SdCameraTests
    LOCAL_INSTRUMENTATION_FOR := SnapdragonCamera
    include $(BUILD_PACKAGE)
endif
