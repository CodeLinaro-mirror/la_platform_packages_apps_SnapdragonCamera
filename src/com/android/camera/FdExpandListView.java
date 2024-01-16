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

import android.app.AlertDialog;
import android.content.Context;
import com.android.camera.util.Log;

import android.content.DialogInterface;
import android.graphics.Color;
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
    private int mSelectGroup = -1;
    private int mSelectChild = -1;
    private int mSelectContourV = -1;

    public FdExpandListView (Context context) {
        mContext = context;
        mSettingsManager = SettingsManager.getInstance();
        mExpandKey = new ArrayList<String>();
        mExpandMap = new HashMap<String, List<String>>();
    }

    public void initFDSettingsData() {
        mFDIndex = 0;
        mExpandKey.add("FD Mask Detection");
        mExpandKey.add("Upper Body Detection");

        mExpandKey.add("FD Facial contour");
        mExpandKey.add("FD Gaze Detection");
        mExpandKey.add("FD Blink Detection");

        mExpandKey.add("FD Expression");
        mExpandKey.add("FD Gender");

        for(int i=0;i<mExpandKey.size();i++){
            String str = mExpandKey.get(i);
            List<String> list = new ArrayList<String>();
            String key =getKeyStr(str);
            CharSequence[] fdEntry = mSettingsManager.getEntries(key);
            for(int j = 0; j< fdEntry.length;j++){
                list.add(fdEntry[j].toString());
            }
            mExpandMap.put(str, list);
        }
    }
    private int getSelected(String str){
        int selected =0;
        String key = getKeyStr(str);
        String value = mSettingsManager.getValue(key);
        if(value == null || value.equals("disable")) {
            selected =0;
        }else if(value.equals("enable")){
            selected = 1;
        }else if (value.equals("display")){
            selected =2;
        }else {
            selected = Integer.valueOf(value);
        }
        return selected;
    }

    private String getKeyStr(String str){
        String key ="";
        switch (str){
            case "FD Mask Detection":
                key = mSettingsManager.KEY_FACE_MASK;
                break;
            case "Upper Body Detection":
                key = mSettingsManager.KEY_UPPER_BODY_DETECTION;
                break;
            case "FD Facial contour":
                key =mSettingsManager.KEY_FACIAL_CONTOUR;
                break;
            case "FD Gaze Detection":
                key = mSettingsManager.KEY_FD_GAZE;
                break;
            case "FD Blink Detection":
                key = mSettingsManager.KEY_FD_BLINK;
                break;
            case "FD Expression":
                key = mSettingsManager.KEY_FD_FACE_EXPRESSION;
                break;
            case "FD Gender":
                key = mSettingsManager.KEY_FD_GENDER;
        }
        return key;
    }

    private void updateFDKey(String keystr ,int child) {
        String key = getKeyStr(keystr);
        mSettingsManager.setValueIndex(key,child);
    }
    public String getFDEntry(String keystr){
        String valuestr = "";
        CharSequence[] fdEntry =null;
        int valueIndx = 0;
        String key = getKeyStr(keystr);
        fdEntry = mSettingsManager.getEntries(key);
        valueIndx = mSettingsManager.getValueIndex(key);
        if(fdEntry != null) {
            valuestr = (fdEntry[valueIndx]).toString();
        }
        return valuestr;
    }

    public String getFDSummery(){
        StringBuilder summery=new StringBuilder();
        for (int i = 0;i < mExpandKey.size();i++){
            String str = mExpandKey.get(i);
            String key = getKeyStr(str);
            summery.append(str).append(":").append(getFDEntry(key)).append("; ");
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
            String keystr = mExpandKey.get(groupPosition);
            String info = mExpandMap.get(keystr).get(childPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.expandlist_child, parent, false);
            }
            TextView tv = (TextView) convertView
                    .findViewById(R.id.second_textview);
            tv.setText(info);
            mSelectChild = getSelected(keystr);
            if(childPosition == mSelectChild){
                tv.setTextColor(Color.BLUE);
            }else{
                tv.setTextColor(Color.BLACK);
            }
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectChild = childPosition;
                    mSelectGroup = groupPosition;
/*                    if(keystr.equals("FD Facial contour") && childPosition >0){
                        buildSelectDialog();
                    }*/
                    updateFDKey(keystr,childPosition);
                    //v.setBackgroundColor(0xff33b5e5);

                    notifyDataSetChanged();


                }
            });
            return convertView;
        }
        private void buildSelectDialog(){
            final CharSequence[] items = {"Enable","Display"};
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSelectContourV = which;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getWindow().setLayout(500,400);
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
            String keystr = mExpandKey.get(groupPosition);
            String info = getFDEntry(keystr);
            tv.setText(keystr);
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
