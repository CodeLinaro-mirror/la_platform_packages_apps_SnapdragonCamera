/*
        Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.

        Redistribution and use in source and binary forms, with or without
        modification, are permitted (subject to the limitations in the
        disclaimer below) provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.

        * Redistributions in binary form must reproduce the above
        copyright notice, this list of conditions and the following
        disclaimer in the documentation and/or other materials provided
        with the distribution.

        * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
        contributors may be used to endorse or promote products derived
        from this software without specific prior written permission.

        NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
        GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
        HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
        WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
        MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
        IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
        ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
        GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
        INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
        IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
        OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
        IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.camera;

import android.content.Context;
import com.android.camera.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.camera.SettingsActivity;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListAdapter;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.ViewGroup;
import java.util.ArrayList;
import org.codeaurora.snapcam.R;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.content.Context;

public class FdExpandListView  {
    private static final String TAG = "ExpandListView";
    private SettingsManager mSettingsManager;
    List<String> mExpandKey = null;
    Map<String, List<String>> mExpandMap = null;
    private ExpandableListView expandableListView = null;
    private ExpandableListAdapter expandableAdapter = null;
    private Context mContext;
    private int mFDIndex = 0;

    public FdExpandListView (Context context) {
        mContext = context;
        mSettingsManager = SettingsManager.getInstance();
        mExpandKey = new ArrayList<String>();
        mExpandMap = new HashMap<String, List<String>>();
    }

    public void initFDSettingsData() {
        mFDIndex = 0;
        mExpandKey.add("FD Face Detection Mode");
        mExpandKey.add("FD Mask Detection");
        mExpandKey.add("Upper Body Detection");

        List<String> list = new ArrayList<String>();
        CharSequence[] fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_DETECTION_MODE);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(0), list);

        list = new ArrayList<String>();
        CharSequence[] fdMaskEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_MASK);
        for(int i = 0; i< fdMaskEntry.length;i++){
            list.add(fdMaskEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(1), list);

        list = new ArrayList<String>();
        CharSequence[] upperBodyDetectionEntry = mSettingsManager.getEntries(
                mSettingsManager.KEY_UPPER_BODY_DETECTION);
        for(int i = 0; i< upperBodyDetectionEntry.length;i++){
            list.add(upperBodyDetectionEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(2), list);
        list = null;
    }

    public void initFDFLData() {
        mFDIndex = 1;
        mExpandKey.add("FD Facial contour");
        mExpandKey.add("FD Gaze Detection");
        mExpandKey.add("FD Blink Detection");

        List<String> list = new ArrayList<String>();
        CharSequence[] fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACIAL_CONTOUR);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(0), list);

        list = new ArrayList<String>();
        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GAZE);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(1), list);

        list = new ArrayList<String>();
        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_BLINK);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(2), list);
        list = null;
    }

    public void initFDFacialData() {
        mFDIndex = 2;
        mExpandKey.add("FD Expression");
        mExpandKey.add("FD Gender");

        List<String> list = new ArrayList<String>();
        CharSequence[] fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_FACE_EXPRESSION);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(0), list);

        list = new ArrayList<String>();
        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GENDER);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        mExpandMap.put((String) mExpandKey.get(1), list);
        list = null;
    }

    private void updateFDKey(int group ,int child) {
        if (mFDIndex == 0) {
            switch(group) {
                case 0:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE,child);
                    break;
                case 1:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FACE_MASK,child);
                    break;
                case 2:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_UPPER_BODY_DETECTION,child);
                    break;
            }
        } else if (mFDIndex == 1) {
            switch(group) {
                case 0:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FACIAL_CONTOUR,child);
                    break;
                case 1:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_GAZE,child);
                    break;
                case 2:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_BLINK,child);
                    break;
            }
        } else if (mFDIndex == 2) {
            switch(group) {
                case 0:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_FACE_EXPRESSION,child);
                    break;
                case 1:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_GENDER,child);
                    break;
            }
        } else {
            switch(group) {
                case 0:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE,child);
                    break;
                case 1:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_FACE_MASK,child);
                    break;
                case 2:
                    mSettingsManager.setValueIndex(mSettingsManager.KEY_UPPER_BODY_DETECTION,child);
                    break;
            }
        }
    }
    public String getFDEntry(int group){
        String valuestr = "";
        CharSequence[] fdEntry =null;
        int valueIndx = 0;
        if (mFDIndex == 0) {
            switch(group) {
                case 0:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_DETECTION_MODE);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE);
                    break;
                case 1:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_MASK);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACE_MASK);
                    break;
                case 2:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_UPPER_BODY_DETECTION);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_UPPER_BODY_DETECTION);
                    break;
            }
        } else if (mFDIndex == 1) {
            switch(group) {
                case 0:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACIAL_CONTOUR);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACIAL_CONTOUR);
                    break;
                case 1:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GAZE);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_GAZE);
                    break;
                case 2:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_BLINK);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_BLINK);
                    break;
            }
        } else if (mFDIndex == 2) {
            switch(group) {
                case 0:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_FACE_EXPRESSION);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_FACE_EXPRESSION);
                    break;
                case 1:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GENDER);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_GENDER);
                    break;
            }
        } else {
            switch(group) {
                case 0:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_DETECTION_MODE);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE);
                    break;
                case 1:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_MASK);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACE_MASK);
                    break;
                case 2:
                    fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_UPPER_BODY_DETECTION);
                    valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_UPPER_BODY_DETECTION);
                    break;
            }
        }
        valuestr = (fdEntry[valueIndx]).toString();
        return valuestr;
    }

    public String getFDSummery(){
        StringBuilder summery=new StringBuilder();
        for (int i = 0;i < mExpandKey.size();i++){
            summery.append(mExpandKey.get(i)).append(":").append(getFDEntry(i)).append("; ");
        }
        return summery.toString();
    }

  public  class FdExpandListViewAdapter extends BaseExpandableListAdapter {

        public void FdExpandListViewAdapter() {
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String key = mExpandKey.get(groupPosition);
            return (mExpandMap.get(key).get(childPosition));
        }
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            String key = mExpandKey.get(groupPosition);
            String info = mExpandMap.get(key).get(childPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.expandlist_child, parent, false);
            }
            TextView tv = (TextView) convertView
                    .findViewById(R.id.second_textview);
            tv.setText(info);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateFDKey(groupPosition,childPosition);
                    notifyDataSetChanged();
                }
            });
            return convertView;
        }
        @Override
        public int getChildrenCount(int groupPosition) {
            String key = mExpandKey.get(groupPosition);
            int size=mExpandMap.get(key).size();
            return size;
        }
        @Override
        public Object getGroup(int groupPosition) {
            return mExpandKey.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return mExpandKey.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.expandlist_parant, parent, false);
            }
            TextView tv = (TextView) convertView
                    .findViewById(R.id.parent_textview);
            String key = mExpandKey.get(groupPosition);
            String info = getFDEntry(groupPosition);
            tv.setText(key);
            TextView expandinfo =(TextView) convertView.findViewById(R.id.parent_info);
            expandinfo.setText(info);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }
}
