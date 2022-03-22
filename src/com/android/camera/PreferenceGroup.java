/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * A collection of <code>CameraPreference</code>s. It may contain other
 * <code>PreferenceGroup</code> and form a tree structure.
 */
public class PreferenceGroup extends CameraPreference {
    private ArrayList<CameraPreference> list =
            new ArrayList<CameraPreference>();
    private Object lock = new Object();

    public PreferenceGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addChild(CameraPreference child) {
        synchronized (lock) {
            list.add(child);
        }
    }

    public void removePreference(int index) {
        synchronized (lock) {
            list.remove(index);
        }
    }

    public CameraPreference get(int index) {
        synchronized (lock) {
            return list.get(index);
        }
    }

    public int size() {
        synchronized (lock) {
            return list.size();
        }
    }

    @Override
    public void reloadValue() {
        synchronized (lock) {
            for (CameraPreference pref : list) {
                pref.reloadValue();
            }
        }
    }

    /**
     * Finds the preference with the given key recursively. Returns
     * <code>null</code> if cannot find.
     */
    public ListPreference findPreference(String key) {
        // Find a leaf preference with the given key. Currently, the base
        // type of all "leaf" preference is "ListPreference". If we add some
        // other types later, we need to change the code.
        synchronized (lock) {
            for (int i = 0; i < list.size(); i++) {
                CameraPreference pref = list.get(i);
                if (pref != null && pref instanceof ListPreference) {
                    ListPreference listPref = (ListPreference) pref;
                    if (listPref.getKey().equals(key)) return listPref;
                } else if (pref != null && pref instanceof PreferenceGroup) {
                    ListPreference listPref =
                            ((PreferenceGroup) pref).findPreference(key);
                    if (listPref != null) return listPref;
                }
            }
        }
        return null;
    }
}
