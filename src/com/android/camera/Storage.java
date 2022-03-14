/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.android.camera.app.CameraApp;
import com.android.camera.data.LocalData;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.ApiHelper;
import androidx.heifwriter.HeifWriter;
import android.graphics.ImageFormat;

import android.media.Image;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.DngCreator;
import java.io.OutputStream;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import com.android.camera.CameraActivity;



public class Storage {
    private static final String TAG = "CameraStorage";

    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY = DCIM + "/Camera";
    public static final String RAW_DIRECTORY = DCIM + "/Camera/raw";
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String HEIF_POSTFIX = ".heic";
    public static final String DNG_POSTFIX = ".dng";

    // Match the code in MediaProvider.computeBucketValues().
    public static final String BUCKET_ID =
            String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 60 * 1024 * 1024;

    public static Uri sImageBaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    public static Uri sVideoBaseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

    public static Uri getImageBaseUri() {
        if (sSaveSDCard && SDCard.sSdcardImageBaseUri != null) {
            return SDCard.sSdcardImageBaseUri;
        }
        return sImageBaseUri;
    }

    public static Uri getVideoBaseUri() {
        if (sSaveSDCard && SDCard.sSdcardVideoBaseUri != null) {
            return SDCard.sSdcardVideoBaseUri;
        }
        return sVideoBaseUri;
    }

    private static boolean sSaveSDCard = false;

    public static boolean isSaveSDCard() {
        return sSaveSDCard;
    }

    public static void setSaveSDCard(boolean saveSDCard) {
        sSaveSDCard = saveSDCard;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    public static int writeFile(String path, byte[] jpeg, ExifInterface exif,
            String mimeType) {
        if (exif != null && (mimeType == null ||
            mimeType.equalsIgnoreCase("jpeg"))) {
            try {
                return exif.writeExif(jpeg, path);
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else if (jpeg != null) {
            if (!(mimeType.equalsIgnoreCase("jpeg") || mimeType == null)) {
                 File dir = new File(RAW_DIRECTORY);
                 dir.mkdirs();
            }
            writeFile(path, jpeg);
            return jpeg.length;
        }
        return 0;
    }

    public static void writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
    }

    private static void writeFile(Uri uri, ExifInterface exif, byte[] jpeg, String mimeType, ContentResolver resolver)
            throws IOException {
        Log.d(TAG, "writeFile uri " + uri);
        if (uri == null) {
            return;
        }
        OutputStream os = resolver.openOutputStream(uri);
        if (exif != null && (mimeType == null || mimeType.equalsIgnoreCase("jpeg"))) {
            exif.writeExif(jpeg, os);
        } else {
            os.write(jpeg);
        }
        os.close();

        if (ApiHelper.isAndroidROrHigher()) {
            ContentValues publishValues = new ContentValues();
            publishValues.put(Images.Media.IS_PENDING, 0);
            resolver.update(uri, publishValues, null, null);
            Log.i(TAG, "Image with uri: " + uri + " was published to the MediaStore");
        }
    }


    // Save the image with a given mimeType and add it the MediaStore.
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height, String mimeType) {

        String path = generateFilepath(title, mimeType);
        return addImageToMediaStore(resolver, title, date, location, orientation, exif, jpeg,
                path, width, height, mimeType);
    }

    /**
     * Add the entry for the media file to media store.
     *
     * @param resolver The The content resolver to use.
     * @param title The title of the media file.
     * @param date The date for the media file.
     * @param location The location of the media file.
     * @param orientation The orientation of the media file.
     * @param exif The exif of the image.
     * @param jpeg The picture data.
     * @param path The picture file path to save
     * @param width The width of the media file after the orientation is
     *            applied.
     * @param height The height of the media file after the orientation is
     *            applied.
     * @param mimeType The MIME type of the data.
     * @return The content URI of the inserted media file or null, if the image
     *         could not be added.
     */
    public static Uri addImageToMediaStore(ContentResolver resolver, String title, long date,
                                           Location location, int orientation, ExifInterface exif, byte[] jpeg,
                                           String path, int width, int height, String mimeType) {
        // Insert into MediaStore.
        ContentValues values = getContentValuesForData(title, date, location, orientation, exif, 0,
                path, width, height, mimeType, true);

        Uri uri = null;
        try {
            Uri baseUri = getImageBaseUri();
            uri = resolver.insert(baseUri, values);
            if (jpeg != null) {
                writeFile(uri, exif, jpeg, mimeType, resolver);
            }
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore " + uri, th.fillInStackTrace());
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            return null;
        }
        return uri;
    }

     public static Uri addDng(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, Image image, int width,
            int height, String mimeType,String path,CameraCharacteristics mCharacteristics,TotalCaptureResult mCaptureResult) {
         Uri uri = addImageToMediaStore(resolver, title, date, location, orientation, exif, null,
                 path, width, height, mimeType);
         if (uri != null) {
             writeDngFile(image, uri, resolver, mCharacteristics, mCaptureResult);
         } else {
             Log.e(TAG, "addDng uri is null");
             return null;
         }

         ContentValues publishValues = new ContentValues();
         if (ApiHelper.isAndroidROrHigher()) {
             publishValues.put(Images.Media.IS_PENDING, 0);
         }
         resolver.update(uri, publishValues, null, null);
         Log.i(TAG, "Image with uri: " + uri + " was published to the MediaStore");

         return uri;
    }

    // Get a ContentValues object for the given photo data
    public static ContentValues getContentValuesForData(String title,
            long date, Location location, int orientation, ExifInterface exif, int jpegLength,
            String path, int width, int height, String mimeType, boolean isPending) {
        // Insert into MediaStore.
        ContentValues values = new ContentValues(11);
        values.put(ImageColumns.TITLE, title);
        String suffix = "";
        if (mimeType.equalsIgnoreCase("jpeg") ||
            mimeType.equalsIgnoreCase("image/jpeg") ||
                mimeType.equalsIgnoreCase("heic") ||
            mimeType == null) {

            if (mimeType.equalsIgnoreCase("heic")){
                values.put(ImageColumns.MIME_TYPE, "image/heic");
                suffix = ".heic";
            } else if(mimeType.equalsIgnoreCase("heics")){
                values.put(ImageColumns.MIME_TYPE, "image/heics");
                suffix = ".heics";
            } else {
                values.put(ImageColumns.MIME_TYPE, "image/jpeg");
                suffix = ".jpg";
            }

        } else if (mimeType.equalsIgnoreCase("raw")) {
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            suffix = ".raw";
        } else if (mimeType.equalsIgnoreCase("yuv")) {
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            suffix = ".yuv";
        } else if (mimeType.equalsIgnoreCase("dng")) {
            values.put(ImageColumns.MIME_TYPE, "image/x-adobe-dng");
            suffix = ".dng";
        }
        values.put(ImageColumns.DISPLAY_NAME, title + suffix);
        Log.d(TAG, "getContentValuesForData title = " + title + ", path = " + path + ", suffix = " + suffix);
        values.put(ImageColumns.DATE_TAKEN, date);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        } else if (exif != null) {
            double[] latlng = exif.getLatLongAsDoubles();
            if (latlng != null) {
                values.put(Images.Media.LATITUDE, latlng[0]);
                values.put(Images.Media.LONGITUDE, latlng[1]);
            }
        }
        if (ApiHelper.isAndroidROrHigher()) {
            String relativePath = "DCIM/Camera";
            if (suffix.equals(".dng") || suffix.equals(".raw") || suffix.equals(".yuv")) {
                relativePath = "DCIM/Camera/raw";
            }
            values.put(ImageColumns.RELATIVE_PATH, relativePath);
            values.put(ImageColumns.IS_PENDING, isPending ? 1 : 0);
        }
        return values;
    }

    public static Uri addImage(ContentResolver resolver, String title,
                               long date, Location location, int orientation,int jpegLength,
                               String path, int width, int height, String mimeType) {
        // Insert into MediaStore.
        ContentValues values =
                getContentValuesForData(title, date, location, orientation, null, jpegLength,
                        path, width, height, mimeType, false);

        return insertImage(resolver, values);
    }
    private static Bitmap decodeDNGImage(final String path) {
        final BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
        int w = opt.outWidth;
        int h = opt.outHeight;
        int d = w > h ? h : w;
        final int target =114;
        int sample = 1;
        if (d > target) {
            while (d / sample / 2 > target) {
                sample *= 2;
            }
        }
        int st = sample * target;
        final Rect rect = new Rect((w - st) / 2, (h - st) / 2, (w + st) / 2, (h + st) / 2);
        opt.inJustDecodeBounds = false;
        opt.inSampleSize = sample;
        bitmap = BitmapFactory.decodeFile(path, opt);
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        bitmap =  Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        return bitmap;
     }

    public static long writeDngFile(Image image,String path,CameraCharacteristics mCharacteristics,TotalCaptureResult mCaptureResult) {
        int size = 0;
        FileOutputStream output = null;
        try {
            DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
            File dir = new File(RAW_DIRECTORY);
            dir.mkdirs();
            output = new FileOutputStream(path);
            try{
                dngCreator.writeImage(output, image);
            } catch (AssertionError ae) {
            Log.d(TAG,"writeImage error-ae="+ae);
            }
            dngCreator.close();
        }catch (IOException e) {
            Log.e(TAG, "IO Exception while saving RAW image to a file", e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Misc Exception while saving RAW image to a file", e);
            return 0;
        } catch (AssertionError e) {
            Log.e(TAG, "Misc Assertion while saving RAW image to a file", e);
            return 0;
        } finally {
            image.close();
            closeOutput(output);
        }
        // Try to get the real image size after add exif.
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            size = (int) f.length();
        }
        return size;
    }

    public static void writeDngFile(Image image, Uri uri, ContentResolver resolver,
                                    CameraCharacteristics mCharacteristics, TotalCaptureResult mCaptureResult) {
        OutputStream os = null;
        try {
            DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
            os = resolver.openOutputStream(uri);
            try{
                dngCreator.writeImage(os, image);
            } catch (AssertionError ae) {
                Log.d(TAG,"writeImage error-ae="+ae);
            }
            dngCreator.close();
        }catch (IOException e) {
            Log.e(TAG, "IO Exception while saving RAW image to a file", e);
        } catch (Exception e) {
            Log.e(TAG, "Misc Exception while saving RAW image to a file", e);
        } catch (AssertionError e) {
            Log.e(TAG, "Misc Assertion while saving RAW image to a file", e);
        } finally {
            image.close();
            closeOutput(os);
        }
    }

    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException at outputStream close ", e);
            }
        }
    }

    public static Uri addRawImage(ContentResolver resolver, String title, long date, Location location,
                                  int orientation, ExifInterface exif, byte[] data, int width, int height,
                                  String mimeType) {
        String path = generateFilepath(title, mimeType);

        return addImageToMediaStore(resolver, title, date, location, orientation, exif, data,
                path, width, height, mimeType);
    }

    public static Uri addHeifImage(ContentResolver resolver, String title, long date,
                                   Location location, int orientation, ExifInterface exif, String path, int width,
                                   int height, int quality, String mimeType) {
        File f = new File(path);
        int size = 0;
        if (f.exists() && f.isFile()) {
            size = (int) f.length();
        }
        return addImage(resolver, title, date, location, orientation,
                size, path, width, height, mimeType);
    }

    // Overwrites the file and updates the MediaStore, or inserts the image if
    // one does not already exist.
    public static void updateImage(Uri imageUri, ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height, String mimeType) {
        String path = generateFilepath(title, mimeType);
        writeFile(path, jpeg, exif, mimeType);
        updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, path,
                width, height, mimeType);
    }

    // Updates the image values in MediaStore, or inserts the image if one does
    // not already exist.
    public static void updateImage(Uri imageUri, ContentResolver resolver, String title,
            long date, Location location, int orientation, int jpegLength,
            String path, int width, int height, String mimeType) {

        ContentValues values =
                getContentValuesForData(title, date, location, orientation, null, jpegLength, path,
                        width, height, mimeType, false);

        // Update the MediaStore
        int rowsModified = resolver.update(imageUri, values, null, null);

        if (rowsModified == 0) {
            // If no prior row existed, insert a new one.
            Log.w(TAG, "updateImage called with no prior image at uri: " + imageUri);
            insertImage(resolver, values);
        } else if (rowsModified != 1) {
            // This should never happen
            throw new IllegalStateException("Bad number of rows (" + rowsModified
                    + ") updated for uri: " + imageUri);
        }
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to delete image: " + uri);
        }
    }

    public static String generateFilepath(String title, String pictureFormat) {
        if (pictureFormat == null || pictureFormat.equalsIgnoreCase("jpeg")
                || pictureFormat.equalsIgnoreCase("heic")
                || pictureFormat.equalsIgnoreCase("heics")) {
            String suffix = ".jpg";
            if (pictureFormat.equalsIgnoreCase("heic")) {
                suffix = ".heic";
            }else if(pictureFormat.equalsIgnoreCase("heics")) {
                suffix = ".heics";
            }
            if (isSaveSDCard() && SDCard.instance().isWriteable()) {
                return SDCard.instance().getDirectory() + '/' + title + suffix;
            } else {
                return DIRECTORY + '/' + title + suffix;
            }
        } else if (pictureFormat.equalsIgnoreCase("yuv")){
            String suffix = ".yuv";
            if (isSaveSDCard() && SDCard.instance().isWriteable()) {
                return SDCard.instance().getDirectory() + '/' + title + suffix;
            } else {
                return DIRECTORY + '/' + title + suffix;
            }
        }  else if(pictureFormat.equalsIgnoreCase("dng")) {
            return DIRECTORY + '/' + title + ".dng";
        }else{
            return RAW_DIRECTORY + '/' + title + ".raw";
        }
    }

    private static void mkdir(String path) {
        ContentResolver resolver = CameraApp.sInstance.getContentResolver();
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(MediaColumns.MIME_TYPE, "image/jpeg");
        if (ApiHelper.isAndroidROrHigher()) {
            contentValues.put(MediaColumns.RELATIVE_PATH, path);
        }
        Uri uri = resolver.insert(getImageBaseUri(), contentValues);
        Log.d(TAG, "mkdir " + uri);
        if (uri != null) {
            resolver.delete(uri, null);
        }
    }

    private static long getSDCardAvailableSpace() {
        if (SDCard.instance().isWriteable() && SDCard.instance().getDirectory() != null) {
            File dir = new File(SDCard.instance().getDirectory());
            boolean r = dir.exists() || dir.mkdirs();
            if (!r) {
                Log.w(TAG, "File mkdir failed, using MediaStore to mkdir");
                mkdir("DCIM/Camera");
            }
            try {
                StatFs stat = new StatFs(SDCard.instance().getDirectory());
                long ret = stat.getAvailableBlocks() * (long) stat.getBlockSize();
                return ret;
            } catch (Exception e) {
            }
            return UNKNOWN_SIZE;
        }
        return UNKNOWN_SIZE;
    }

    private static long getInternalStorageAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        boolean r = dir.exists() || dir.mkdirs();
        if (!r) {
            Log.w(TAG, "File mkdir failed, using MediaStore to mkdir");
            mkdir("DCIM/Camera");
        }
        if (!dir.isDirectory()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Failed to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    public static long getAvailableSpace() {
        if (isSaveSDCard()) {
            return getSDCardAvailableSpace();
        } else {
            return getInternalStorageAvailableSpace();
        }
    }

    public static boolean switchSavePath() {
        if (!isSaveSDCard()
                && getInternalStorageAvailableSpace() <= LOW_STORAGE_THRESHOLD_BYTES
                && getSDCardAvailableSpace() > LOW_STORAGE_THRESHOLD_BYTES) {
            setSaveSDCard(true);
            return true;
        }
        return false;
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }

    private static Uri insertImage(ContentResolver resolver, ContentValues values) {
        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }
}
