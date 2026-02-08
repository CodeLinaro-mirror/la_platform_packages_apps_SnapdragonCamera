/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import android.media.Image;
import android.net.Uri;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.options.SerializeOptions;
import com.android.camera.util.Log;
import com.android.camera.util.XmpUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class MotionPhoto {
    private static final String TAG = "snapcam_MotionPhoto";
    private static final String NS_GCAMERA = "http://ns.google.com/photos/1.0/camera/";
    private static final String NS_CONTAINER = "http://ns.google.com/photos/1.0/container/";
    private static final String NS_ITEM = "http://ns.google.com/photos/1.0/container/item/";
    public final static String PREFIX = "GCamera";
    public final static String CONTAINER_PREFIX = "Container";
    public final static String Item_PREFIX = "Item";
    private static final String KEY_DIRECTORY = "Directory";
    private static String containerPrefix;
    private static String itemPrefix;

    private byte[] mImage;

    private Uri mUri;

    private String mXMP;

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_GCAMERA, PREFIX);
        } catch (XMPException e) {
            Log.e(TAG, "Failed to register XMP namespaces", e);
        }
    }

    public MotionPhoto(byte[] image){
        this.mImage = image;
        this.mUri = null;
    }

    public void updateUri(Uri uri){
        mUri = uri;
    }

    public Uri getUri(){
        return mUri;
    }

    public String getXMP(){
        return mXMP;
    }

    public byte[] combineJpegVideo(byte[] videoBytes) {
        if (mImage == null || videoBytes == null) {
            Log.e(TAG, "Failed to get JPEG or video data");
            return null;
        }
        byte[] jpegWithXmp = null;
        XMPMeta xmpMeta = createMotionPhotoXMPV1(videoBytes.length/2, videoBytes.length);
        try {
            mXMP = XMPMetaFactory.serializeToString(xmpMeta, new SerializeOptions()
                    .setUseCompactFormat(true)
                    .setOmitPacketWrapper(true)
                    .setNewline("\n")
                    .setPadding(0));
        } catch (XMPException e) {
            throw new RuntimeException(e);
        }
        //write new xmp to jpeg
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ( XmpUtil.writeXMPMeta(new ByteArrayInputStream(mImage), baos, xmpMeta) ){
            jpegWithXmp = baos.toByteArray();
        }else{
            Log.e(TAG, "write xmp failure ");
        }
        if(jpegWithXmp != null && jpegWithXmp.length > 0 && videoBytes.length >0) {
            Log.i(TAG,"combine finial jpeg:" + jpegWithXmp.length + ",vidoe lenght:" + videoBytes.length);
            int totalLength = jpegWithXmp.length + videoBytes.length;
            byte[] result = new byte[totalLength];
            System.arraycopy(jpegWithXmp, 0, result, 0, jpegWithXmp.length);
            System.arraycopy(videoBytes, 0, result, jpegWithXmp.length, videoBytes.length);
            return result;
        }else {
            return null;
        }
    }

    public byte[] getJpegData(Image image) {
        if (image == null) {
            Log.d(TAG, "getJpegData - invalid param");
            return null;
        }
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int size = buffer.capacity();
        byte[] data = new byte[size];
        buffer.rewind();
        buffer.get(data, 0, size);

        return data;
    }

    public static XMPMeta createMotionPhotoXMPV1(
            long presentationTimestampUs,
            int videoLength) {
        XMPMeta xmp = XmpUtil.createXMPMeta();
        try {
            xmp.setProperty(NS_GCAMERA, "MicroVideo", "1");
            xmp.setProperty(NS_GCAMERA, "MicroVideoOffset", videoLength);
            xmp.setProperty(NS_GCAMERA, "MicroVideoVersion", 1);
            xmp.setProperty(NS_GCAMERA, "MicroVideoPresentationTimestampUs", presentationTimestampUs);
            return xmp;
        } catch (XMPException e) {
            throw new RuntimeException(e);
        }
    }

    public static XMPMeta createMotionPhotoXMPV2(
            long presentationTimestampUs,
            int videoLength) {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_GCAMERA, PREFIX);
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_CONTAINER, CONTAINER_PREFIX);
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NS_ITEM, Item_PREFIX);
        } catch (XMPException e) {
            Log.e(TAG, "Failed to register XMP namespaces", e);
        }
        Map<String, String> item1 = new LinkedHashMap<>();
        item1.put("Mime", "image/jpeg");
        item1.put("Semantic", "Primary");
        item1.put("Length", "0");
        item1.put("Padding", "0");

        Map<String, String> item2 = new LinkedHashMap<>();
        item2.put("Mime", "video/mp4");
        item2.put("Semantic", "MotionPhoto");
        item2.put("Length", String.valueOf(videoLength));
        item2.put("Padding", "0");

        XMPMeta xmp = XmpUtil.createXMPMeta();
        try {
            containerPrefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(NS_CONTAINER);
            itemPrefix = XMPMetaFactory.getSchemaRegistry().getNamespacePrefix(NS_ITEM);
            xmp.setProperty(NS_GCAMERA, "MotionPhoto", "1");
            xmp.setProperty(NS_GCAMERA, "MotionPhotoVersion", "1");
            xmp.setProperty(NS_GCAMERA, "MotionPhotoPresentationTimestampUs", presentationTimestampUs);
            xmp.setProperty(NS_CONTAINER, KEY_DIRECTORY, null, new PropertyOptions().setArrayOrdered(true));
            for (Map<String, String> item : Arrays.asList(item1, item2)) {
                appendItem(xmp, item);
            }
            return xmp;
        } catch (XMPException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendItem(XMPMeta xmp, Map<String, String> fields) throws XMPException {
        PropertyOptions structOpts = new PropertyOptions().setStruct(true);
        xmp.appendArrayItem(NS_CONTAINER, KEY_DIRECTORY, null, null, structOpts);
        int idx = xmp.countArrayItems(NS_CONTAINER, KEY_DIRECTORY);
        String itemPath = KEY_DIRECTORY + "[" + idx + "]/" + containerPrefix + ":Item";
        xmp.setProperty(NS_CONTAINER, itemPath, null, structOpts);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            xmp.setProperty(NS_CONTAINER, itemPath + "/" + itemPrefix + ":" + key, val, null);
        }
    }
}
