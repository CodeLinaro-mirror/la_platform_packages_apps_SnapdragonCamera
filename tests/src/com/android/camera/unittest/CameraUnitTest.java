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
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera.unittest;

import com.android.camera.util.CameraUtil;

import android.graphics.Matrix;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import junit.framework.TestCase;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)

@LargeTest
public class CameraUnitTest{
	private String TAG = "CameraUnitTest";
    @Before
    public void setUp(){
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(){
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   @Test
    public void testRoundOrientation() {
        int h = CameraUtil.ORIENTATION_HYSTERESIS;
        assertEquals(0, CameraUtil.roundOrientation(0, 0));
        assertEquals(0, CameraUtil.roundOrientation(359, 0));
        assertEquals(0, CameraUtil.roundOrientation(0 + 44 + h, 0));
        assertEquals(90, CameraUtil.roundOrientation(0 + 45 + h, 0));
        assertEquals(0, CameraUtil.roundOrientation(360 - 44 - h, 0));
        assertEquals(270, CameraUtil.roundOrientation(360 - 45 - h, 0));

        assertEquals(90, CameraUtil.roundOrientation(90, 90));
        assertEquals(90, CameraUtil.roundOrientation(90 + 44 + h, 90));
        assertEquals(180, CameraUtil.roundOrientation(90 + 45 + h, 90));
        assertEquals(90, CameraUtil.roundOrientation(90 - 44 - h, 90));
        assertEquals(0, CameraUtil.roundOrientation(90 - 45 - h, 90));

        assertEquals(180, CameraUtil.roundOrientation(180, 180));
        assertEquals(180, CameraUtil.roundOrientation(180 + 44 + h, 180));
        assertEquals(270, CameraUtil.roundOrientation(180 + 45 + h, 180));
        assertEquals(180, CameraUtil.roundOrientation(180 - 44 - h, 180));
        assertEquals(90, CameraUtil.roundOrientation(180 - 45 - h, 180));

        assertEquals(270, CameraUtil.roundOrientation(270, 270));
        assertEquals(270, CameraUtil.roundOrientation(270 + 44 + h, 270));
        assertEquals(0, CameraUtil.roundOrientation(270 + 45 + h, 270));
        assertEquals(270, CameraUtil.roundOrientation(270 - 44 - h, 270));
        assertEquals(180, CameraUtil.roundOrientation(270 - 45 - h, 270));

        assertEquals(90, CameraUtil.roundOrientation(90, 0));
        assertEquals(180, CameraUtil.roundOrientation(180, 0));
        assertEquals(270, CameraUtil.roundOrientation(270, 0));

        assertEquals(0, CameraUtil.roundOrientation(0, 90));
        assertEquals(180, CameraUtil.roundOrientation(180, 90));
        assertEquals(270, CameraUtil.roundOrientation(270, 90));

        assertEquals(0, CameraUtil.roundOrientation(0, 180));
        assertEquals(90, CameraUtil.roundOrientation(90, 180));
        assertEquals(270, CameraUtil.roundOrientation(270, 180));

        assertEquals(0, CameraUtil.roundOrientation(0, 270));
        assertEquals(90, CameraUtil.roundOrientation(90, 270));
        assertEquals(180, CameraUtil.roundOrientation(180, 270));
    }
    @Test
    public void testPrepareMatrix() {
        Matrix matrix = new Matrix();
        float[] points;
        int[] expected;
        CameraUtil.prepareMatrix(matrix, false, 0, 800, 480);
        points = new float[] {-1000, -1000, 0, 0, 1000, 1000, 0, 1000, -750, 250};
        expected = new int[] {0, 0, 400, 240, 800, 480, 400, 480, 100, 300};
        matrix.mapPoints(points);
        assertEqualsArry(expected, points);

        CameraUtil.prepareMatrix(matrix, false, 90, 800, 480);
        points = new float[] {-1000, -1000,   0,   0, 1000, 1000, 0, 1000, -750, 250};
        expected = new int[] {800, 0, 400, 240, 0, 480, 0, 240, 300, 60};
        matrix.mapPoints(points);
        assertEqualsArry(expected, points);

        CameraUtil.prepareMatrix(matrix, false, 180, 800, 480);
        points = new float[] {-1000, -1000, 0, 0, 1000, 1000, 0, 1000, -750, 250};
        expected = new int[] {800, 480, 400, 240, 0, 0, 400, 0, 700, 180};
        matrix.mapPoints(points);
        assertEqualsArry(expected, points);

        CameraUtil.prepareMatrix(matrix, true, 180, 800, 480);
        points = new float[] {-1000, -1000, 0, 0, 1000, 1000, 0, 1000, -750, 250};
        expected = new int[] {0, 480, 400, 240, 800, 0, 400, 0, 100, 180};
        matrix.mapPoints(points);
        assertEqualsArry(expected, points);
    }

    private void assertEqualsArry(int expected[], float[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Array index " + i + " mismatch", expected[i], Math.round(actual[i]));
        }
    }
}
