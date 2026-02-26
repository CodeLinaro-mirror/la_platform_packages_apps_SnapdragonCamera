/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

public class YuvFrame {
    public final byte[] data;
    public final long timestampUs;
    public final int width;
    public final int height;

    public YuvFrame(byte[] data, long timestampUs, int width, int height) {
        this.data = data;
        this.timestampUs = timestampUs;
        this.width = width;
        this.height = height;
    }
}
