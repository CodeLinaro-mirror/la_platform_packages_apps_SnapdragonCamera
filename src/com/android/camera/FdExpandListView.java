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

package com.android.camera;

import android.content.Context;
import android.util.Log;
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
    List<String> expandKey = null;
    Map<String, List<String>> expandMap = null;
    private ExpandableListView expandableListView = null;
    private ExpandableListAdapter expandableAdapter = null;
    private Context mContext;

    public FdExpandListView (Context context) {
        mContext = context;
        mSettingsManager = SettingsManager.getInstance();
    }

    public void initExpandData() {
        expandKey = new ArrayList<String>();
        expandKey.add("FD Smile Detection");
        expandKey.add("FD Gaze Detection");
        expandKey.add("FD Blink Detection");
        expandKey.add("FD Facial contour");
        expandKey.add("FD Face detction mode");
        expandMap = new HashMap<String, List<String>>();
        List<String> list = new ArrayList<String>();
        CharSequence[] fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_SMILE);
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        expandMap.put( (String) expandKey.get(0), list);

        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GAZE);
        list = new ArrayList<String>();
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        expandMap.put((String) expandKey.get(1), list);

        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_BLINK);
        list = new ArrayList<String>();
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        expandMap.put((String) expandKey.get(2), list);

        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACIAL_CONTOUR);
        list = new ArrayList<String>();
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        expandMap.put((String) expandKey.get(3), list);

        fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_DETECTION_MODE);
        list = new ArrayList<String>();
        for(int i = 0; i< fdEntry.length;i++){
            list.add(fdEntry[i].toString());
        }
        expandMap.put((String) expandKey.get(4), list);
    }

    private void updateFDKey(int group ,int child){
        switch(group){
            case 0:
                mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_SMILE,child);
                break;
            case 1:
                mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_GAZE,child);
                break;
            case 2:
                mSettingsManager.setValueIndex(mSettingsManager.KEY_FD_BLINK,child);
                break;
            case 3:
                mSettingsManager.setValueIndex(mSettingsManager.KEY_FACIAL_CONTOUR,child);
                break;
            case 4:
                mSettingsManager.setValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE,child);
                break;
        }
    }
    public String getFDEntry(int group){
        String valuestr = "";
        CharSequence[] fdEntry =null;
        int valueIndx = 0;
        switch (group) {
            case 0:
                fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_SMILE);
                valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_SMILE);
                break;
            case 1:
                fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_GAZE);
                valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_GAZE);
                break;
            case 2:
                fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FD_BLINK);
                valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FD_BLINK);
                break;
            case 3:
                fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACIAL_CONTOUR);
                valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACIAL_CONTOUR);
                break;
            case 4:
                fdEntry = mSettingsManager.getEntries(mSettingsManager.KEY_FACE_DETECTION_MODE);
                valueIndx = mSettingsManager.getValueIndex(mSettingsManager.KEY_FACE_DETECTION_MODE);
                break;
        }
        valuestr = (fdEntry[valueIndx]).toString();
        return valuestr;
    }
    public String getFDSummery(){
        StringBuilder summery=new StringBuilder();
        for (int i = 0;i < expandKey.size();i++){
            summery.append(expandKey.get(i)).append(":").append(getFDEntry(i)).append("; ");
        }
        return summery.toString();
    }

  public  class FdExpandListViewAdapter extends BaseExpandableListAdapter {
        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String key = expandKey.get(groupPosition);
            return (expandMap.get(key).get(childPosition));
        }
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            String key = expandKey.get(groupPosition);
            String info = expandMap.get(key).get(childPosition);
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
            String key = expandKey.get(groupPosition);
            int size=expandMap.get(key).size();
            return size;
        }
        @Override
        public Object getGroup(int groupPosition) {
            return expandKey.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return expandKey.size();
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
            String key = expandKey.get(groupPosition);
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
