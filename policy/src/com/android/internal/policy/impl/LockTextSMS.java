/*
 * Copyright 2012 Adam Fisch of Team Gummy
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

package com.android.internal.policy.impl;

import com.android.internal.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.graphics.*;
import android.telephony.SmsMessage;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.lang.IllegalArgumentException;


public class LockTextSMS extends TextView {

    private boolean mIsAttached;
    private Handler mHandler;
    
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "LockTextSMS";
    
    private String body;
    private String caller;
    private String findName;    

    public LockTextSMS(Context context) {
        this(context, null);
    }

    public LockTextSMS(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    } 

    public LockTextSMS(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        updateSettings();
        
        keepMyBoxUp();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mIsAttached) {
            mIsAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(SMS_RECEIVED);
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
            Log.i(TAG, "Intent recieved: " + intent.getAction());

            if (intent.getAction() == SMS_RECEIVED) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                	getYourText(bundle);
                }
            }
        }
    };
    
    private void keepMyBoxUp() {
    	boolean showTexts = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.LOCKSCREEN_SMS_CROSS, 1) == 0);
    	if (showTexts) {
    		String name = null;
        	String msg = null;
        	Uri uri = Uri.parse("content://sms/inbox");
        	Cursor cursor1 = mContext.getContentResolver().query(uri,new String[] { "_id", "thread_id", "address", "person", "date","body", "type" }, null, null, null);
        	String[] columns = new String[] { "address", "body"};
        	if (cursor1.getCount() > 0) {
        	   if (cursor1.moveToFirst()){
        	       name = cursor1.getString(cursor1.getColumnIndex(columns[0]));
        	       msg = cursor1.getString(cursor1.getColumnIndex(columns[1]));
        	    }
        	}
        	if (msg != null && name != null) {
            	Uri personUri = Uri.withAppendedPath( ContactsContract.PhoneLookup.CONTENT_FILTER_URI, name);
            	Cursor cur = mContext.getContentResolver().query(personUri, new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null );
            	if (cur.moveToFirst()) {
            		int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            		caller = cur.getString(nameIndex);
            	}
            	if (caller == null) {
            		caller = name;
            	}
            	body = msg;
            	updateCurrentText(body, caller);
            	setBackgroundResource(R.drawable.ic_lockscreen_player_background_old);
        	}
    	}
    }
    
    private void updateCurrentText(String textBody, String callerID) {
    	String newewstText = callerID + ": " + textBody;
    	
    	SpannableStringBuilder SMSText = new SpannableStringBuilder(newewstText);
    	if (textBody != null && callerID != null) {
    		SMSText.setSpan(new ForegroundColorSpan(0xffffffff), 0, newewstText.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	}
    	
    	setText(SMSText);
    }
    
    private void getYourText(Bundle bundle) {
    	boolean showTexts = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.LOCKSCREEN_SHOW_TEXTS, 0) == 1);
    	if (showTexts) {
        	try {
        		Object[] pdus = (Object[])bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                }
                if (messages.length > -1) {
                	for (int i = 0; i< pdus.length; i++) {
                    	body = messages[i].getDisplayMessageBody();
                    	findName = messages[i].getOriginatingAddress();
                    	Uri personUri = Uri.withAppendedPath( ContactsContract.PhoneLookup.CONTENT_FILTER_URI, findName);
                    	Cursor cur = mContext.getContentResolver().query(personUri, new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null );
                    	if (cur.moveToFirst()) {
                    		int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    		caller = cur.getString(nameIndex);
                    	}
                    	if (caller == null){
                    		caller = findName;
                    	}
                        updateCurrentText(body, caller);
                        setBackgroundResource(R.drawable.ic_lockscreen_player_background_old);
                        Settings.System.putInt(mContext.getContentResolver(), Settings.System.LOCKSCREEN_SMS_CROSS, 0);
                        setVisibility(View.VISIBLE);
                	}
                }
        	} catch (IllegalArgumentException e) {
        	}
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.LOCKSCREEN_SHOW_TEXTS), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        
        setBackgroundResource(0);
        
        boolean showTexts = (Settings.System.getInt(resolver, Settings.System.LOCKSCREEN_SHOW_TEXTS, 0) == 1);
        
        if (showTexts) {
        	setVisibility(View.VISIBLE);
        } else {
        	setVisibility(View.GONE);
        }
    }
}