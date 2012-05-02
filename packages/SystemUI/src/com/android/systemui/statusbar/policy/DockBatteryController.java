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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class DockBatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.DockBatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    private static final int BATTERY_STYLE_NORMAL  = 0;
    private static final int BATTERY_STYLE_TEXT    = 1;
    private static final int BATTERY_STYLE_GONE    = 2;

    private static final int BATTERY_ICON_STYLE_NORMAL      = R.drawable.stat_sys_kb_battery;
    private static final int BATTERY_ICON_STYLE_CHARGE      = R.drawable.stat_sys_kb_battery_charge;

    private boolean mDockStatus = false;
    private boolean mDockCharging = false;
    private int mBatteryIcon = BATTERY_ICON_STYLE_NORMAL;

    public DockBatteryController(Context context) {
        mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_DOCK_LEVEL, 0);
            mDockCharging = intent.getIntExtra(BatteryManager.EXTRA_DOCK_STATUS, 0) == BatteryManager.DOCK_STATE_CHARGING;
            mDockStatus = intent.getIntExtra(BatteryManager.EXTRA_DOCK_STATUS, 0) != BatteryManager.DOCK_STATE_UNDOCKED;

            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            updateBattery();
        }
    }

    private void updateBattery() {
        int mIcon = View.GONE;
        int mText = View.GONE;
        int mIconStyle = BATTERY_ICON_STYLE_NORMAL;

        mIcon = mDockStatus ? (View.VISIBLE) : (View.GONE);
        mIconStyle = mDockCharging ? BATTERY_ICON_STYLE_CHARGE
                    : BATTERY_ICON_STYLE_NORMAL;

        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setVisibility(mIcon);
            v.setImageResource(mIconStyle);
        }
    }
}
