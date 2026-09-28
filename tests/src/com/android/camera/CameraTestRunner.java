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
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.camera;
import com.android.camera.functional.FunctionTest;
import android.support.test.runner.AndroidJUnitRunner;
import android.os.Bundle;

public class CameraTestRunner  extends AndroidJUnitRunner {
    public ClassLoader getLoader() {
        return CameraTestRunner.class.getClassLoader();
    }
    @Override
    public void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
