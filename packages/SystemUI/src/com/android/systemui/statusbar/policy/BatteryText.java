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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.graphics.*;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;

public class BatteryText extends TextView {

    private boolean mIsAttached;
    private boolean mBattText;
    private boolean mBattColorAllow;
    private int mBattTextColor;

    Handler mHandler;

    public BatteryText(Context context) {
        this(context, null);
    }

    public BatteryText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    } 

    public BatteryText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mIsAttached) {
            mIsAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mIsAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mIsAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getAction();
            if (str.equals(Intent.ACTION_BATTERY_CHANGED)) {
                updateTextColor(intent);
                updateText(intent);
            }
        }
    };

    // Get the battery text %
    final void updateText(Intent intent) {
        int level = intent.getIntExtra("level", 0);
        String battery = Integer.toString(level) + "%";

        SpannableStringBuilder BattText = new SpannableStringBuilder(battery);
        int percent = battery.indexOf("%");
        CharacterStyle style = new RelativeSizeSpan(0.7f);
        BattText.setSpan(style, percent, percent + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        setText(BattText);
    }

    // Get text colors
    final void updateTextColor(Intent intent) {
        ContentResolver resolver = mContext.getContentResolver();
        int level = intent.getIntExtra("level", 0);
        boolean plugged = intent.getIntExtra("plugged", 0) != 0;

        if (!mBattColorAllow) {
            mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_STOCK, 0xFF33B5E5);
        } else if (plugged) {
            mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_CHARGE, 0xFF00FF00);
        } else if (level > 15) {
            mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_NORMAL, 0xFF33B5E5);
        } else {
            mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_LOW, 0xFFFF0000);
        }
        setTextColor(mBattTextColor);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT_COLOR_ALLOWED), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT_COLOR_STOCK), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT_COLOR_CHARGE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT_COLOR_NORMAL), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.BATTERY_TEXT_COLOR_LOW), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mBattColorAllow = (Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_ALLOWED, 0) == 1);

        if (!mBattColorAllow) {
            mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_STOCK, 0xFF33B5E5);
            setTextColor(mBattTextColor);
        } else { 
	      Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	      int level = batteryIntent.getIntExtra("level", 0);
	      boolean plugged = batteryIntent.getIntExtra("plugged", 0) != 0;

              if (plugged) {
                  mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_CHARGE, 0xFF00FF00);
              } else if (level > 15) {
                  mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_NORMAL, 0xFF33B5E5);
              } else {
                  mBattTextColor = Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT_COLOR_LOW, 0xFFFF0000);
              }
              setTextColor(mBattTextColor);
        }

        mBattText = (Settings.System.getInt(resolver, Settings.System.BATTERY_TEXT, 0) == 1);

        if (mBattText)
          setVisibility(View.VISIBLE);
        else
          setVisibility(View.GONE);
    }
}

