/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.camera.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;

import com.android.camera.util.Log;

import java.io.InputStream;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL11;

/**
 * An utility class for data in content provider.
 */
public class FilmstripItemUtils {

    private static final String TAG = "LocalDataUtil";

    /**
     * @param mimeType The MIME type to check.
     * @return Whether the MIME is a video type.
     */
    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Checks whether the MIME type represents an image media item.
     *
     * @param mimeType The MIME type to check.
     * @return Whether the MIME is a image type.
     */
    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }


    /**
     * Decodes the dimension of a bitmap.
     *
     * @param is An input stream with the data of the bitmap.
     * @return The decoded width/height is stored in Point.x/Point.y
     *         respectively.
     */
    public static Point decodeBitmapDimension(InputStream is) {
        Point size = null;
        BitmapFactory.Options justBoundsOpts = new BitmapFactory.Options();
        justBoundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, justBoundsOpts);
        if (justBoundsOpts.outWidth > 0 && justBoundsOpts.outHeight > 0) {
            size = new Point(justBoundsOpts.outWidth, justBoundsOpts.outHeight);
        } else {
            Log.e(TAG, "Bitmap dimension decoding failed");
        }
        return size;
    }

    /**
     * Loads the thumbnail of a video.
     *
     * @param path The path to the video file.
     * @return {@code null} if the loading failed.
     */
    public static Bitmap loadVideoThumbnail(String path) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] data = retriever.getEmbeddedPicture();
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "MediaMetadataRetriever.setDataSource() fail:" + e.getMessage());
        }
		try {
			retriever.release();
		} catch (IOException e) {
        }
        return bitmap;
    }
}
