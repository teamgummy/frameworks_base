package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class HomeButton extends KeyButtonView {

    IStatusBarService mStatusBarService;
    private boolean mLongPress;
    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.LONG_PRESS_HOME), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SOFT_KEY_COLOR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
            longClicker();
        }
    }

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed())
                performLongClick();
        }
    };

    public HomeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    } 

    public HomeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        mLongPress = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.LONG_PRESS_HOME, 0) == 1);

        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        updateSettings();
        longClicker();
    }

    private void longClicker() {
        mLongPress = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.LONG_PRESS_HOME, 0) == 1);
        if (mLongPress) 
            mSupportsLongpress = true;     
        else
            mSupportsLongpress = false;

        setOnLongClickListener(mLongClickListener);
    }  

    private OnLongClickListener mLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            try {
                mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService(Context.STATUS_BAR_SERVICE));
                mStatusBarService.toggleRecentApps();
            } catch (RemoteException e) {
            }
            return true;
        }
    };

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mLongPress = (Settings.System.getInt(resolver, Settings.System.LONG_PRESS_HOME, 0) == 1);

        try {
            setColorFilter(null);
            setColorFilter(Settings.System.getInt(resolver, Settings.System.SOFT_KEY_COLOR));
        } catch (SettingNotFoundException e) {
        }

    }
}
