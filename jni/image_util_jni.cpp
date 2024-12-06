/*
Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#include <jni.h>
#include <assert.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <stdio.h>
#ifdef ENABLE_C2PA_LIB
#include <aidlcommonsupport/NativeHandle.h>
#include <aidl/android/hardware/common/Ashmem.h>
#include <aidl/vendor/qti/hardware/c2pa/BnC2PA.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <cutils/ashmem.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <utils/Log.h>
#include <vndk/hardware_buffer.h>
#include <map>

using ::aidl::android::hardware::common::NativeHandle;
using ::aidl::android::hardware::common::Ashmem;
using ::ndk::ScopedAIBinder_DeathRecipient;
using ::ndk::ScopedAStatus;
using ::ndk::SpAIBinder;
using namespace std;
using namespace ::aidl::vendor::qti::hardware::c2pa;
#endif

#ifdef __ANDROID__
#include "android/log.h"
#define printf(...) __android_log_print( ANDROID_LOG_ERROR, "ImageUtil", __VA_ARGS__ )
#endif

#ifdef ENABLE_C2PA_LIB
#define SIZE_2MB 0x200000

#define T_ERROR(error_code)                                                      \
    ret = (error_code);                                                          \
    ALOGE("%s::%d err=0x%x errno:%d(%s)", __func__, __LINE__, error_code, errno, \
          strerror(errno));                                                      \
    goto exit;

#define T_CHECK_ERR(cond, error_code) \
    if (!(cond)) {                    \
        T_ERROR(error_code)           \
    }

#define T_CHECK(cond)                        \
    if (!(cond)) {                           \
        ALOGE("%s::%d", __func__, __LINE__); \
        goto exit;                           \
    }

#define LOGD_PRINT(...)      \
    do {                     \
        ALOGD(__VA_ARGS__);  \
        printf(__VA_ARGS__); \
        printf("\n");        \
    } while (0)

#define LOGE_PRINT(...)      \
    do {                     \
        ALOGE(__VA_ARGS__);  \
        printf(__VA_ARGS__); \
        printf("\n");        \
    } while (0)
#endif
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_FrameProcessor_nativeRotateNV21
        (JNIEnv* env, jobject thiz, jobjectArray inBuf,
         jint imageWidth, jint imageHeight, jint degree, jobjectArray outBuf);
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_FrameProcessor_nativeNV21toRgb(
        JNIEnv *env, jobject thiz, jobjectArray yvuBuf, jobjectArray rgbBuf, jint width, jint height, jint stride);
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeFlipNV21(
        JNIEnv* env, jobject thiz, jbyteArray yvuBytes, jint stride, jint height, jint gap, jboolean isVertical);
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeResizeImage(
        JNIEnv* env, jobject thiz, jbyteArray oldBuf, jbyteArray newBuf, jint oldWidth, jint oldHeight, jint oldStride, jint newWidth, jint newHeight);
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeNV21Split(
        JNIEnv* env, jobject thiz, jbyteArray srcYVU, jobjectArray yBuf, jobjectArray vuBuf, jint width, jint height, jint srcStride, jint dstStride);
JNIEXPORT void JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeEnablePerfLock(JNIEnv* env, jobject thiz);
JNIEXPORT jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativePerfLockAcq(
        JNIEnv* env, jobject thiz, jint handle, jint duration, jintArray resource,jint numArgs);
JNIEXPORT void JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativePerfLockRelease(
        JNIEnv* env, jobject thiz, jint handle);
//API for C2PA
JNIEXPORT int JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSetUp(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paTearDown(JNIEnv* env, jobject thiz);
JNIEXPORT int JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paEnroll(JNIEnv* env, jobject thiz, jstring apiKey, jstring licenseFile);
JNIEXPORT jbyteArray JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSignMedia(
        JNIEnv* env, jobject thiz, jint imageType, jint height, jint width, jint stride, jint compression, jint maxThumbnailSize, jint thumbnailCompression, jstring jinputFile,
        jdouble latitude, jdouble longitude, jdouble altitude, jdouble accuracy, jlong time);
JNIEXPORT void JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSignVideo(
        JNIEnv* env, jobject thiz, jint height, jint width, jstring jinputFile, jdouble latitude, jdouble longitude, jdouble altitude, jdouble accuracy, jlong time);
JNIEXPORT int JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paValidateMedia(
        JNIEnv* env, jobject thiz, jint handle);
#ifdef __cplusplus
}
#endif

typedef unsigned char uint8_t;
void *perf_handle;
int (*perflock_acq)(int handle, int duration, int list[], int numArgs);
int (*perflock_rel)(int handle);
int (*perflock_hint)(int hint_id, char* package, int duration, int type);

#ifdef ENABLE_C2PA_LIB
shared_ptr<IC2PA> c2paService = nullptr;
ScopedAIBinder_DeathRecipient deathRecipient;
SpAIBinder c2paBinder;
vector<AHardwareBuffer*> mHwBufferList;
bool inPlaceUpdate = true;
#endif

void getPerfhandle(){
    perf_handle = dlopen("libqti-perfd-client.so", RTLD_NOW);
    if(perf_handle == NULL){
        printf("Unable to open perf lib: %s", dlerror());
        return ;
    }

    perflock_acq = (int (*)(int, int, int *, int))dlsym(perf_handle, "perf_lock_acq");
    if(!perflock_acq){
        printf("Unable to get perf_lock_acq handle");
    }

    perflock_rel = (int (*)(int))dlsym(perf_handle, "perf_lock_rel");
    if(!perflock_rel){
        printf("Unable to get perf_lock_rel handle");
    }

    perflock_hint = (int (*)(int, char*, int , int))dlsym(perf_handle, "perf_hint");
    if(!perflock_hint){
        printf("Unable to get perflock_hint handle");
    }
    printf("get perflock api successfully");
}

void perflockRelease(int handle){
    if (handle != -1) {
        printf("Releasing handle:%d", handle);
        if(perflock_rel)
            perflock_rel(handle);
    }
}

int perflockAcquire(int handle, int duration, int resource[] ,int numArgs){
    int handle_acq = -1;
    if(perflock_acq){
        printf("perflock_acq,handle=%d,duration=%d,numArgs=%d", handle, duration, numArgs);
        handle_acq = perflock_acq(handle, duration, resource, numArgs);
    }
    return handle_acq;
}
int perfhintEnable(int hint_id, char* pkg, int duration, int type){
    int handle_acq = 0;
    if(duration < 0)
    {
        return 0;
    }
    if(perflock_hint)
        handle_acq = perflock_hint(hint_id, pkg, duration, type);
    return handle_acq;
}

JNIEXPORT void Java_com_android_camera_imageprocessor_PostProcessor_nativeEnablePerfLock(JNIEnv* env, jobject thiz)
{
    getPerfhandle();
}
JNIEXPORT jint Java_com_android_camera_imageprocessor_PostProcessor_nativePerfLockAcq(
        JNIEnv* env, jobject thiz, jint handle, jint duration, jintArray resource,jint numArgs)
{
    int value =0;
    jint *cresource = env->GetIntArrayElements(resource, 0);
    value = perflockAcquire(handle, duration, cresource, numArgs);
    env->ReleaseIntArrayElements(resource, cresource, 0);
    return value;
}
JNIEXPORT void Java_com_android_camera_imageprocessor_PostProcessor_nativePerfLockRelease(
        JNIEnv* env, jobject thiz, jint handle)
{
    perflockRelease(handle);
}

void rotateBufAndMerge(uint8_t *in_buf, jint imageWidth, jint imageHeight, jint degree, uint8_t *out_buf)
{
    if(degree == 90) {
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                int offset = y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
            }
        }
        i = imageWidth * imageHeight;
        for (int x = 0; x < imageWidth; x += 2) {
            for (int y = imageHeight / 2 - 1; y >= 0; y--) {
                int offset = imageWidth*imageHeight + y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
                out_buf[i] = in_buf[offset + 1];
                i++;
            }
        }
    } else if(degree == 270) {
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                int offset = y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
            }
        }
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 2; x >= 0; x-=2) {
            for (int y = 0; y < imageHeight/2; y++) {
                int offset = imageWidth*imageHeight + y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
                out_buf[i] = in_buf[offset + 1];
                i++;
            }
        }
    } else if(degree == 180) {
        int i = 0;
        for (int y = imageHeight - 1; y >= 0; y--) {
            for (int x = imageWidth - 1; x >= 0 ; x--) {
                int offset = y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
            }
        }
        i = imageWidth * imageHeight;
        for (int y = imageHeight/2 - 1; y >= 0; y--) {
            for (int x = imageWidth - 2; x >= 0 ; x-=2) {
                int offset = imageWidth*imageHeight + y * imageWidth + x;
                out_buf[i] = in_buf[offset];
                i++;
                out_buf[i] = in_buf[offset + 1];
                i++;
            }
        }
    }
}

jint JNICALL Java_com_android_camera_imageprocessor_FrameProcessor_nativeRotateNV21(
        JNIEnv* env, jobject thiz, jobjectArray inBuf,
        jint imageWidth, jint imageHeight, jint degree, jobjectArray outBuf)
{
    (void)thiz;
    uint8_t *in_buf = (uint8_t *)env->GetDirectBufferAddress(inBuf);
    uint8_t *out_buf = (uint8_t *)env->GetDirectBufferAddress(outBuf);
    rotateBufAndMerge(in_buf, imageWidth, imageHeight, degree, out_buf);

    return 0;
}

jint JNICALL Java_com_android_camera_imageprocessor_FrameProcessor_nativeNV21toRgb(
        JNIEnv* env, jobject thiz, jobjectArray yvuBuf, jobjectArray rgbBuf, jint width, jint height)
{
    (void)thiz;
    uint8_t *in_buf = (uint8_t *)env->GetDirectBufferAddress(yvuBuf);
    uint8_t *rgb_buf = (uint8_t *)env->GetDirectBufferAddress(rgbBuf);
    int ysize = width * height;
    int y_value;
    int i, v, u, r, g, b;
    for(int x=0; x < width; x++) {
        for(int y=0; y < height; y++) {
            y_value = (in_buf[y*width+x] & 0xFF);
            i = ysize + (x/2*2) + ((y/2) * width);
            v = (in_buf[i] & 0xFF) - 128;
            u = (in_buf[i + 1] & 0xFF) - 128;
            r = (int)(1.164f * y_value + 1.596f * v);
            g = (int)(1.164f * y_value - 0.813f * v - 0.391f * u);
            b = (int)(1.164f * y_value + 2.018f * u);
            r = r > 255 ? 255 : r < 0 ? 0 : r;
            g = g > 255 ? 255 : g < 0 ? 0 : g;
            b = b > 255 ? 255 : b < 0 ? 0 : b;
            rgb_buf[(y*width + x) * 4 + 3] = (uint8_t)(0xFF);
            rgb_buf[(y*width + x) * 4 + 2] = (uint8_t)(b & 0xFF);
            rgb_buf[(y*width + x) * 4 + 1] = (uint8_t)(g & 0xFF);
            rgb_buf[(y*width + x) * 4 + 0] = (uint8_t)(r & 0xFF);
        }
    }
    return 0;
}

jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeFlipNV21(
        JNIEnv* env, jobject thiz, jbyteArray yvuBytes, jint stride, jint height, jint gap, jboolean isVertical)
{
    (void)thiz;
    jbyte* imageDataNV21Array = env->GetByteArrayElements(yvuBytes, NULL);
    uint8_t *buf = (uint8_t *)imageDataNV21Array;
    int ysize = stride * height;
    uint8_t temp1, temp2;

    if(isVertical) {
        for (int x = 0; x < stride; x++) {
            for (int y = 0; y < height / 2; y++) {
                temp1 = buf[y * stride + x];
                buf[y * stride + x] = buf[(height - 1 - y) * stride + x];
                buf[(height - 1 - y) * stride + x] = temp1;
            }
        }
        for (int x = 0; x < stride; x += 2) {
            for (int y = 0; y < height / 4; y++) {
                temp1 = buf[ysize + y * stride + x];
                temp2 = buf[ysize + y * stride + x + 1];
                buf[ysize + y * stride + x] = buf[ysize + (height / 2 - 1 - y) * stride + x];
                buf[ysize + y * stride + x + 1] = buf[ysize + (height / 2 - 1 - y) * stride + x + 1];
                buf[ysize + (height / 2 - 1 - y) * stride + x] = temp1;
                buf[ysize + (height / 2 - 1 - y) * stride + x + 1] = temp2;
            }
        }
    } else {
        int width = stride - gap;
        for (int x = 0; x < width/2; x++) {
            for (int y = 0; y < height; y++) {
                temp1 = buf[y * stride + x];
                buf[y * stride + x] = buf[y * stride + (width - 1 - x)];
                buf[y * stride + (width - 1 - x)] = temp1;
            }
        }
        for (int x = 0; x < width/2; x += 2) {
            for (int y = 0; y < height / 2; y++) {
                temp1 = buf[ysize + y * stride + x];
                temp2 = buf[ysize + y * stride + x + 1];
                buf[ysize + y * stride + x] = buf[ysize + y * stride + (width - 1 - x - 1)];
                buf[ysize + y * stride + x + 1] = buf[ysize + y * stride + (width - 1 - x)];
                buf[ysize + y * stride + (width - 1 - x - 1)] = temp1;
                buf[ysize + y * stride + (width - 1 - x)] = temp2;
            }
        }
    }

    env->ReleaseByteArrayElements(yvuBytes, imageDataNV21Array, JNI_ABORT);
    return 0;
}

jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeNV21Split(
        JNIEnv* env, jobject thiz, jbyteArray srcYVU, jobjectArray yBuf, jobjectArray vuBuf, jint width, jint height, jint srcStride, jint dstStride) {
    (void)thiz;
    uint8_t *old_buf = (uint8_t *) env->GetByteArrayElements(srcYVU, NULL);
    uint8_t *y_buf = (uint8_t *)env->GetDirectBufferAddress(yBuf);
    uint8_t *vu_buf = (uint8_t *)env->GetDirectBufferAddress(vuBuf);
    int ySize = srcStride*height;

    for(int j=0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            y_buf[j*dstStride+i] = old_buf[j*srcStride + i];
            if (j < height / 2) {
                vu_buf[j*dstStride + i] = old_buf[ySize + j*srcStride + i];
            }
        }
    }
    env->ReleaseByteArrayElements(srcYVU, (jbyte *)old_buf, JNI_ABORT);

    return 0;
}

jint JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeResizeImage(
        JNIEnv* env, jobject thiz, jbyteArray oldBuf, jbyteArray newBuf, jint oldWidth, jint oldHeight, jint oldStride, jint newWidth, jint newHeight) {
    (void)thiz;
    uint8_t *old_buf = (uint8_t *) env->GetByteArrayElements(oldBuf, NULL);
    uint8_t *new_buf = (uint8_t *) env->GetByteArrayElements(newBuf, NULL);
    int adjustedOldWidth = oldWidth;

    if((float)oldWidth/oldHeight != (float)newWidth/newHeight) {
        adjustedOldWidth = (int)(((float)newWidth/newHeight) * oldHeight);
    }

    int wR = adjustedOldWidth / newWidth;
    int hR = oldHeight / newHeight;
    if(wR < hR && adjustedOldWidth - newWidth*wR >= adjustedOldWidth/4) {
        wR++;
    }
    if(hR < wR && oldHeight - newHeight*hR >= oldHeight/4) {
        hR++;
    }
    int R = wR < hR ? wR : hR;
    int wC = oldWidth - (newWidth*R);
    int hC = oldHeight - (newHeight*R);
    unsigned int cv1, cv2;

    int index = 0;
    for(int j=hC/2; j < newHeight*R + hC/2; j+=R) {
        for(int i=wC/2; i < newWidth*R + wC/2; i+=R) {
            cv1 = 0;
            for(int y = 0; y < R; y++) {
                for (int x = 0; x < R; x++) {
                    cv1 += old_buf[(j+y)*oldStride + i+x];
                }
            }
            cv1 /= R*R;
            new_buf[index] = (unsigned char)cv1;
            index++;
        }
    }
    int ySize = oldStride*oldHeight;
    index = newWidth*newHeight;
    for(int j=hC/2; j < newHeight*R + hC/2; j+=R*2) {
        for(int i=wC/2; i < newWidth*R + wC/2; i+=R*2) {
            cv1 = 0;
            cv2 = 0;
            for(int y = 0; y < R*2; y+=2) {
                for (int x = 0; x < R*2; x+=2) {
                    cv1 += old_buf[ySize + (j+y)/2*oldStride + (i+x)/2*2];
                    cv2 += old_buf[ySize + (j+y)/2*oldStride + (i+x)/2*2 + 1];
                }
            }
            cv1 /= R*R;
            cv2 /= R*R;
            new_buf[index] = (unsigned char)cv1;
            index++;
            new_buf[index] = (unsigned char)cv2;
            index++;
        }
    }
    env->ReleaseByteArrayElements(oldBuf, (jbyte *)old_buf, JNI_ABORT);
    env->ReleaseByteArrayElements(newBuf, (jbyte *)new_buf, JNI_ABORT);

    return R;
}

#ifdef ENABLE_C2PA_LIB
void serviceDied(void* cookie) {
    ALOGI("C2PA AIDL died");
    c2paService = nullptr;
}

int32_t loadFile(string filePath, Ashmem& ashmemFile, uint32_t addOnSize)
{
    int32_t ret = 0;
    int32_t fd = -1, ashmemFd = -1, buffLen = 0;
    struct stat appStat = {};
    void *buffer = nullptr, *outBuffer = nullptr;
    size_t copied = 0;
    size_t fdSize = 0;

    AHardwareBuffer *hwBuffer = nullptr;
    AHardwareBuffer_Desc bufDesc;
    NativeHandle nativeHandleAIDL;
    const native_handle_t* nativeHandle;

    fd = open(filePath.c_str(), O_RDONLY);
    T_CHECK_ERR(fd > 0, fd);

    ret = fstat(fd, &appStat);
    T_CHECK_ERR(ret == 0, -1);

    fdSize = appStat.st_size;
    T_CHECK_ERR(fdSize > 0, -1);

    buffLen = fdSize + addOnSize;
    bufDesc.width = (buffLen + (SIZE_2MB -1)) & (~(SIZE_2MB - 1));
    bufDesc.height = 1;
    bufDesc.layers = 1;
    bufDesc.format = AHARDWAREBUFFER_FORMAT_BLOB;

    ret = AHardwareBuffer_allocate(&bufDesc, &hwBuffer);
    T_CHECK_ERR(ret == 0 && hwBuffer != nullptr, -1);

    nativeHandle = AHardwareBuffer_getNativeHandle(hwBuffer);
    T_CHECK_ERR(nativeHandle != nullptr, -1);

    nativeHandleAIDL = android::dupToAidl(nativeHandle);
    T_CHECK_ERR(!android::isAidlNativeHandleEmpty(nativeHandleAIDL), -1);

    buffer = malloc(fdSize);
    T_CHECK_ERR(buffer != nullptr, -1);

    ret = read(fd, buffer, fdSize);
    T_CHECK_ERR(ret == fdSize, -1);
    ret = 0;

    LOGD_PRINT("Load file of size :%d %d", fdSize, nativeHandleAIDL.fds[0].get());
    ashmemFd = dup(nativeHandleAIDL.fds[0].get());
    T_CHECK_ERR(ashmemFd > 0, fd);

    outBuffer = mmap(NULL, fdSize, PROT_READ | PROT_WRITE, MAP_SHARED, ashmemFd, 0);
    T_CHECK_ERR(outBuffer != MAP_FAILED, -1);

    memcpy(outBuffer, buffer, fdSize);
    ashmemFile.fd.set(ashmemFd);
    ashmemFile.size = fdSize;

    LOGD_PRINT("Successfully loaded file into ashmem memory");
    mHwBufferList.push_back(hwBuffer);

exit:
    if (ret != 0) {
        AHardwareBuffer_release(hwBuffer);
    }
    if (outBuffer != nullptr) {
        munmap(outBuffer, fdSize);
    }
    if (ret != 0 && ashmemFd >= 0) {
        close(ashmemFd);
    }
    if (buffer != NULL) {
        free(buffer);
    }
    if (fd > 0) {
        close(fd);
    }
    return ret;
}

uint8_t * dumpAshmemFile(Ashmem &inFile)
{
    size_t fileSize = (size_t) inFile.size;
    if(fileSize <= 0){
        return nullptr;
    }

    uint8_t * data = (uint8_t*) mmap(NULL, fileSize, PROT_READ, MAP_SHARED, inFile.fd.get(), 0);
    if (data == MAP_FAILED) {
        ALOGE("%s::%d mmap of buffer fd failed with error: %s", __func__, __LINE__, strerror(errno));
        return nullptr;
    }
    ALOGD("Succefully loaded ashmem file %d at vaddr = %x", inFile.fd.get(), data);
    return data;
}

int32_t dumpFile(string filePath, uint8_t *vaddr, uint32_t size)
{
    int32_t ret = 0, destFd = -1;

    ALOGD("%s::%d Write buffer at vaddr = %x to file %s", __func__, __LINE__,
    vaddr, filePath.c_str());

    destFd = open(filePath.c_str(), O_RDWR | O_CREAT, 0666);
    T_CHECK_ERR(destFd >= 0, -1);

    ret = write(destFd, vaddr, size);
    T_CHECK_ERR(ret == size, -1);
    ret = 0;

    ALOGD("%s::%d Successfully wrote %d bytes to file", __func__, __LINE__, size);
    exit:
    if (destFd >= 0) {
        close(destFd);
    }
    return ret;
}
#endif

JNIEXPORT int Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSetUp(JNIEnv* env, jobject thiz)
{
    int ret = -1;
#ifdef ENABLE_C2PA_LIB
    ScopedAStatus status = ScopedAStatus::ok();
    const string instance = string() + IC2PA::descriptor +"/default";

    if (!AServiceManager_isDeclared(instance.c_str())) {
        LOGE_PRINT("%s:%d AIDL service is not declared in VINTF manifest", __func__, __LINE__);
        return ret;
    }

    c2paBinder = ::ndk::SpAIBinder(AServiceManager_waitForService(instance.c_str()));
    T_CHECK_ERR(c2paBinder.get() != nullptr, -1);

    deathRecipient = ScopedAIBinder_DeathRecipient(AIBinder_DeathRecipient_new(&serviceDied));
    status = ScopedAStatus::fromStatus(AIBinder_linkToDeath(c2paBinder.get(),
                                                            deathRecipient.get(),(void*) serviceDied));

    T_CHECK_ERR(status.isOk(), -1);

    c2paService = IC2PA::fromBinder(c2paBinder);

    T_CHECK_ERR (c2paService != nullptr, -1);

    LOGD_PRINT("Connected to C2PA AIDL service");
#endif
exit:
    return ret;
}

JNIEXPORT void Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paTearDown(JNIEnv* env, jobject thiz)
{
#ifdef ENABLE_C2PA_LIB
    ScopedAStatus status = ScopedAStatus::ok();
    for (auto hwBuffer: mHwBufferList) {
        AHardwareBuffer_release(hwBuffer);
    }
    if(c2paService != nullptr) {
        status = ScopedAStatus::fromStatus(AIBinder_unlinkToDeath(c2paBinder.get(),
                                                                  deathRecipient.get(), (void*) serviceDied));
        if (!status.isOk()) {
            LOGD_PRINT("Failed to unlinking from death recipient %d: %s",
                   status.getStatus(), status.getMessage());
        }
        c2paService = nullptr;
    }
#endif
}

JNIEXPORT int Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paEnroll(JNIEnv* env, jobject thiz, jstring japiKey, jstring jlicenseFile)
{
    printf("nativeC2paEnroll start");
    int32_t ret = -1;
#ifdef ENABLE_C2PA_LIB
    const char *apikey = env->GetStringUTFChars(japiKey, 0);
    const char *licenseFile = env->GetStringUTFChars(jlicenseFile, 0);

    if (apikey == NULL || licenseFile == NULL) {
        return ret;
    }
    EnrollResponse result = EnrollResponse::ENROLL_RESPONSE_FAILED;
    ScopedAStatus status = ScopedAStatus::ok();

    vector<C2PADataTypePair> enrollParam;

    C2PADataTypePair outPair;
    outPair.key = "CLOUD_CREDENTIALS";
    outPair.value = C2PADataType::make<C2PADataType::stringValue>(apikey);
    enrollParam.push_back(std::move(outPair));

    outPair.key = "FEATURE_LICENSE";
    outPair.value = C2PADataType::make<C2PADataType::fdValue>();
    ret = loadFile(licenseFile, outPair.value.get<C2PADataType::fdValue>(), 0);
    T_CHECK_ERR(ret == 0, -1);
    enrollParam.push_back(std::move(outPair));

    status = c2paService->enroll(enrollParam, &result);
    T_CHECK_ERR(status.isOk() &&
                result == EnrollResponse::ENROLL_RESPONSE_SUCCESS, (int32_t) result);
    for (auto hwBuffer: mHwBufferList) {
        AHardwareBuffer_release(hwBuffer);
    }
    mHwBufferList.clear();
    env->ReleaseStringUTFChars(japiKey, apikey);
    env->ReleaseStringUTFChars(jlicenseFile, licenseFile);
#endif
exit:
    return ret;
}

JNIEXPORT jbyteArray Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSignMedia(JNIEnv* env, jobject thiz, jint imageType, jint height, jint width, jint stride, jint compression, jint maxThumbnailSize, jint thumbnailCompression, jstring jinputFile,
                                                                                              jdouble latitude, jdouble longitude, jdouble altitude, jdouble accuracy, jlong time)
{
    uint8_t *coutput;
    jbyteArray output;
    int32_t ret = 0, ingredientCount = 0, iter = 0, addOnSize = 0;
#ifdef ENABLE_C2PA_LIB
    const char *inputFile = env->GetStringUTFChars(jinputFile, 0);
    SignResponse result = SignResponse::SIGN_RESPONSE_FAILED;
    ScopedAStatus status = ScopedAStatus::ok();
    Ashmem imageFd, outImageFd;
    vector<C2PADataTypePair> inputParam;
    vector<C2PADataTypePair> assertion;
    string key, value;

    C2PADataTypePair outPair;
    outPair.key = "MEDIA_TYPE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(0);
    inputParam.push_back(std::move(outPair));
    outPair.key = "IMAGE_TYPE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(imageType);
    inputParam.push_back(std::move(outPair));
    outPair.key = "IMAGE_HEIGHT";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(height);
    inputParam.push_back(std::move(outPair));
    outPair.key = "IMAGE_WIDTH";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(width);
    inputParam.push_back(std::move(outPair));
    outPair.key = "IMAGE_STRIDE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(stride);
    inputParam.push_back(std::move(outPair));
    outPair.key = "IMAGE_COMPRESSION";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(compression);
    inputParam.push_back(std::move(outPair));
    outPair.key = "THUMBNAIL_MAX_SIZE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(maxThumbnailSize);
    inputParam.push_back(std::move(outPair));
    outPair.key = "THUMBNAIL_QUALITY";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(thumbnailCompression);
    inputParam.push_back(std::move(outPair));
    //for location start
    outPair.key = "LOCATION_LATITUDE";
    outPair.value = latitude;
    inputParam.push_back(std::move(outPair));
    outPair.key = "LOCATION_LONGITUDE";
    outPair.value = longitude;
    inputParam.push_back(std::move(outPair));
    outPair.key = "LOCATION_ALTITUDE";
    outPair.value = altitude;
    inputParam.push_back(std::move(outPair));
    outPair.key = "LOCATION_ACCURACY";
    outPair.value = accuracy;
    inputParam.push_back(std::move(outPair));
    outPair.key = "LOCATION_TIME";
    outPair.value = time;
    inputParam.push_back(std::move(outPair));
    //for location end
    if (inPlaceUpdate == true) {
        for (iter = 0; iter < assertion.size(); iter++) {
             T_CHECK_ERR(assertion[iter].value.getTag() == C2PADataType::stringValue, -1);
             value = assertion[iter].value.get<C2PADataType::stringValue>();
             key = assertion[iter].key.c_str();
             addOnSize += (key.size()+value.size())*2;
        }
        addOnSize = (addOnSize + (SIZE_2MB -1)) & (~(SIZE_2MB - 1));
        addOnSize = SIZE_2MB*2;
        addOnSize += SIZE_2MB*ingredientCount;

        outPair.key = "ADDON_SIZE_OUTPUT";
        outPair.value = C2PADataType::make<C2PADataType::intValue>(addOnSize);
        inputParam.push_back(std::move(outPair));

        outPair.key = "INPLACE_UPDATE";
        outPair.value = C2PADataType::make<C2PADataType::byteValue>(1);
        inputParam.push_back(std::move(outPair));
    }
    ret = loadFile(inputFile, imageFd, addOnSize);
    T_CHECK_ERR(ret == 0 && (imageFd.fd.get()) >= 0, -1);

    status = c2paService->signMedia(imageFd, inputParam, assertion, &outImageFd, &result);
    T_CHECK_ERR(status.isOk() && result == SignResponse::SIGN_RESPONSE_SUCCESS &&
                (outImageFd.fd.get()) > 0, (int32_t) result);
    ret = 0;

    LOGD_PRINT("Successfully signed media using AIDL service");
    coutput = dumpAshmemFile(outImageFd);
    if(coutput == nullptr) goto exit;
    output = env->NewByteArray((size_t) outImageFd.size);
    env->SetByteArrayRegion(output, 0, (size_t) outImageFd.size, (jbyte *)coutput);
    LOGD_PRINT("Successfully wrote signed media to file");
    env->ReleaseStringUTFChars(jinputFile, inputFile);
    LOGD_PRINT("sign result: %d", ret );
#endif
exit:
    return output;
}

JNIEXPORT void JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paSignVideo(
        JNIEnv* env, jobject thiz, jint height, jint width, jstring jinputFile, jdouble latitude, jdouble longitude, jdouble altitude, jdouble accuracy, jlong time)
{
#ifdef ENABLE_C2PA_LIB
    const char *inputFile = env->GetStringUTFChars(jinputFile, 0);
    int32_t ret = 0, ingredientCount = 0, iter = 0, addOnSize = 0;
    SignResponse result = SignResponse::SIGN_RESPONSE_FAILED;
    ScopedAStatus status = ScopedAStatus::ok();
    Ashmem imageFd, outImageFd;
    vector<C2PADataTypePair> inputParam;
    vector<C2PADataTypePair> assertion;
    uint8_t *coutput;
    string key, value;

    C2PADataTypePair outPair;
    outPair.key = "MEDIA_TYPE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(2);
    inputParam.push_back(std::move(outPair));

    outPair.key = "LOCATION_LATITUDE";
    outPair.value = latitude;
    inputParam.push_back(std::move(outPair));

    outPair.key = "LOCATION_LONGITUDE";
    outPair.value = longitude;
    inputParam.push_back(std::move(outPair));

    outPair.key = "LOCATION_ALTITUDE";
    outPair.value = altitude;
    inputParam.push_back(std::move(outPair));

    outPair.key = "LOCATION_ACCURACY";
    outPair.value = accuracy;
    inputParam.push_back(std::move(outPair));

    outPair.key = "LOCATION_TIME";
    outPair.value = time;
    inputParam.push_back(std::move(outPair));

    if (inPlaceUpdate == true) {
        for (iter = 0; iter < assertion.size(); iter++) {
             T_CHECK_ERR(assertion[iter].value.getTag() == C2PADataType::stringValue, -1);
             value = assertion[iter].value.get<C2PADataType::stringValue>();
             key = assertion[iter].key.c_str();
             addOnSize += (key.size()+value.size())*2;
        }
        addOnSize = (addOnSize + (SIZE_2MB -1)) & (~(SIZE_2MB - 1));
        addOnSize = SIZE_2MB*2;
        addOnSize += SIZE_2MB*ingredientCount;

        outPair.key = "ADDON_SIZE_OUTPUT";
        outPair.value = C2PADataType::make<C2PADataType::intValue>(addOnSize);
        inputParam.push_back(std::move(outPair));

        outPair.key = "INPLACE_UPDATE";
        outPair.value = C2PADataType::make<C2PADataType::byteValue>(1);
        inputParam.push_back(std::move(outPair));
    }

    ret = loadFile(inputFile, imageFd, addOnSize);
    T_CHECK_ERR(ret == 0 && (imageFd.fd.get()) >= 0, -1);

    status = c2paService->signMedia(imageFd, inputParam, assertion, &outImageFd, &result);
    T_CHECK_ERR(status.isOk() && result == SignResponse::SIGN_RESPONSE_SUCCESS &&
                (outImageFd.fd.get()) > 0, (int32_t) result);
    ret = 0;

    LOGD_PRINT("Successfully signed media using AIDL service");
    coutput = dumpAshmemFile(outImageFd);
    if(coutput == nullptr) goto exit;
    dumpFile(inputFile, coutput, (size_t) outImageFd.size);
    exit:
    LOGD_PRINT("sign result: %d", ret );
#endif
}

JNIEXPORT int JNICALL Java_com_android_camera_imageprocessor_PostProcessor_nativeC2paValidateMedia(JNIEnv* env, jobject thiz, jstring jinputFile)
{
    int32_t ret = -1;
#ifdef ENABLE_C2PA_LIB
    ValidateResponse result;
    vector<C2PADataTypePair> output;
    ScopedAStatus status = ScopedAStatus::ok();
    Ashmem imageFd;
    vector<C2PADataTypePair> inputParam;
    const char *inputFile = env->GetStringUTFChars(jinputFile, 0);

    C2PADataTypePair outPair;

    outPair.key = "MEDIA_TYPE";
    outPair.value = C2PADataType::make<C2PADataType::intValue>(0);
    inputParam.push_back(std::move(outPair));

    ret = loadFile(inputFile, imageFd, 0);
    T_CHECK_ERR(ret == 0 && (imageFd.fd.get()) >= 0, -1);

    status = c2paService->validateMedia(imageFd, inputParam, &output, &result);
    T_CHECK_ERR(result != ValidateResponse::VALIDATION_RESPONSE_FAILED, (uint32_t) result);
    LOGD_PRINT("Result of validate image output: %d", (uint8_t) result);
#endif
exit:
return ret;
}