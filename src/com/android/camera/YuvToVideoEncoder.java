/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Size;

import com.android.camera.util.CameraUtil;
import com.android.camera.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class YuvToVideoEncoder {
    private static final String TAG = "Snapcam_YuvToVideoEncoder";
    private static final int IFRAME_INTERVAL = 1;
    private static final int DEFAULT_BITRATE = 6000000;
    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private int mTrackIndex = -1;
    private boolean mMuxerStarted = false;
    private final BlockingQueue<YuvFrame> encodeQueue = new ArrayBlockingQueue<>(100);
    private volatile boolean mIsEncoding = true;
    private Thread mEncodeThread;
    private Thread mVideoEncodeThread;

    private OnEncodingCompleteListener listener;
    private static final int TIMEOUT_USEC = 10000;
    private static final int TOTAL_RECORD_FRAMES = 90;
    private int mYuvCount = 0;

    public YuvToVideoEncoder() {

    }
    public interface OnEncodingCompleteListener {
        void onEncodingComplete();
    }

    public void setOnEncodingCompleteListener(OnEncodingCompleteListener listener) {
        this.listener = listener;
    }

    public void prepare(Size videoSize, FileDescriptor fd, int rotation) {
        Log.i(TAG,"prepare video encoder");
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoSize.getWidth(), videoSize.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, DEFAULT_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        format.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);

        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMuxer = new MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxer.setOrientationHint(rotation);
            mYuvCount = 0;
        } catch (IOException e) {
            Log.w(TAG, "prepare, exception  e= "+e);
        }
        Log.i(TAG,"prepare video encoder done");
    }

    public void startVideoThread(){
        mIsEncoding = true;
        if(mEncoder != null) {
            mEncoder.start();
            startEncodeThread();
            startVideoEncoder();
        }
    }
    private void startEncodeThread() {
        mEncodeThread = new Thread(() -> {
            while (mIsEncoding) {
                try {
                    YuvFrame frame = encodeQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (frame == null) {
                        continue;
                    }
                    if (mEncoder == null) {
                        Log.e(TAG, "Encoder is null, cannot encode frame");
                        break;
                    }
                    int inputIndex = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                    Log.d(TAG,"startEncodeThread, take image from encode queue, inputIndex:" + inputIndex + ",frame.data: " + frame.data + ",,mYuvCount:" + mYuvCount);
                    if (inputIndex >= 0 && frame.data != null) {
                        ByteBuffer buffer = mEncoder.getInputBuffer(inputIndex);
                        if (buffer != null) {
                            Log.d(TAG,"frame.data.length:" + frame.data.length + ",buffer.capacity(): " + buffer.capacity());
                            buffer.clear();
                            if (frame.data.length > buffer.capacity()) {
                                Log.e(TAG, "Frame too large! Max: " + buffer.capacity());
                            } else {
                                buffer.put(frame.data);
                                mEncoder.queueInputBuffer(inputIndex, 0, frame.data.length, frame.timestampUs,
                                        mYuvCount == TOTAL_RECORD_FRAMES - 1 ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                            }
                        }
                        mYuvCount ++;
                        if(mYuvCount == TOTAL_RECORD_FRAMES){
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "startEncodeThread, exception  e= "+e);
                }
            }
        });
        mEncodeThread.start();
    }

    private void startVideoEncoder() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        mVideoEncodeThread = new Thread(() -> {
            while (mIsEncoding) {
                if (mEncoder == null) {
                    Log.w(TAG, "Encoder is null, stopping video encoding");
                    break;
                }
                int encoderStatus = mEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                Log.d(TAG, "startVideoEncoder, encoderStatus: " + encoderStatus);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    if (!mMuxerStarted) {
                        try {
                            mTrackIndex = mMuxer.addTrack(newFormat);
                            mMuxer.start();
                            mMuxerStarted = true;
                            Log.i(TAG, "Muxer started, track index: " + mTrackIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to add track or start muxer", e);
                            mIsEncoding = false;
                            break;
                        }
                    }
                } else if (encoderStatus >= 0) {
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(encoderStatus);
                    Log.d(TAG, "startVideoEncoder, mMuxerStarted: " + mMuxerStarted + ",mMuxerStarted:" + mMuxerStarted + ",mTrackIndex:" + mTrackIndex + ",outputBuffer:" + outputBuffer + ",info.size:" + info.size);
                    if (mMuxerStarted && mTrackIndex != -1 && outputBuffer != null && info.size > 0) {
                        try {
                            outputBuffer.position(info.offset);
                            outputBuffer.limit(info.offset + info.size);
                            mMuxer.writeSampleData(mTrackIndex, outputBuffer, info);
                        } catch (Exception e) {
                            mIsEncoding = false;
                            Log.e(TAG, "Failed to write sample data", e);
                        }
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.v(TAG, "end of video stream reached");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onEncodingComplete();
                                }
                            }
                        }).start();
                        mIsEncoding = false;
                        Log.i(TAG,"call onEncodingComplete");
                        break;
                    }
                }
            }
        });
        mVideoEncodeThread.start();
    }

    public void setEncodingDone(){
        encodeQueue.clear();
        mIsEncoding = false;
    }

    public void encodeFrame(YuvFrame frame) {
        if (mIsEncoding) {
            encodeQueue.offer(frame);
        }
    }

    public void stop() {
        setEncodingDone();
        Log.i(TAG,"stop mEncodeThread" );
        if (mEncodeThread != null) {
            try {
                mEncodeThread.join(TIMEOUT_USEC);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Log.i(TAG,"stop VideoEncodeThread" );
        if (mVideoEncodeThread != null) {
            try {
                mVideoEncodeThread.join(TIMEOUT_USEC);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Log.i(TAG,"stop mEncoder" );
        if (mEncoder != null) {
            try {
                mEncoder.stop();
                mEncoder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mEncoder = null;
        }
        Log.i(TAG,"stop mMuxer" );
        if (mMuxer != null) {
            try {
                if (mMuxerStarted) {
                    mMuxer.stop();
                }
                mMuxer.release();
                mMuxerStarted = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMuxer = null;
        }
        Log.i(TAG,"stop all video encode done" );
        mTrackIndex = -1;
        mYuvCount = 0;
    }
}
