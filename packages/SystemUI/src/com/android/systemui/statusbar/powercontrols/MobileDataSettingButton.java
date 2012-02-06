package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;

public final class MobileDataSettingButton extends PowerControls {

    private MobileDataObserver mMobileDataObserver = new MobileDataObserver();

    private static final Mode SCREEN_MODE = Mode.MULTIPLY;

    public MobileDataSettingButton(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    private void setMobileDataEnabled(boolean paramBoolean) {
        ConnectivityManager localConnectivityManager = (ConnectivityManager)this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (localConnectivityManager != null) {
            Log.i("MobileDataSettingButton", "setMobileDataEnabled: set to  = " + paramBoolean);
            localConnectivityManager.setMobileDataEnabled(paramBoolean);
            Log.i("MobileDataSettingButton", "setMobileDataEnabled : connectivityManager = null");
        }
    }

    private void updateIcons() {
        View localView = getRootView();
        ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_data_btn_icon);
        ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_data_btn_status_icon);
        switch (getActivateStatus()) {
            default:
            case 1:
                pcIcon.setImageResource(R.drawable.pc_data_on);
                pcStatus.setImageResource(R.drawable.pc_icon_on);
                pcStatus.setColorFilter(mStatusColor, SCREEN_MODE);
                break;
            case 0:
                pcIcon.setImageResource(R.drawable.pc_data_off);
                pcStatus.setColorFilter(null);
                pcStatus.setImageResource(R.drawable.pc_icon_off);
                break;
        }
    }

    private void updateStatus() {
        if (1 == Settings.Secure.getInt(this.mContext.getContentResolver(), "mobile_data", 1)) {
            setActivateStatus(1);
            updateIcons();
        } else {
            setActivateStatus(0);
            updateIcons();
        }
    }

    public void activate() {
        Log.e("MobileDataSettingButton", "activate()");
        setMobileDataEnabled(true);
    }

    public void deactivate() {
        Log.e("MobileDataSettingButton", "deactivate()");
        setMobileDataEnabled(false);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.e("MobileDataSettingButton", "onAttachedToWindow()");
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("mobile_data"), false, this.mMobileDataObserver);
        updateStatus();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e("MobileDataSettingButton", "onDetachedFromWindow()");
        this.mContext.getContentResolver().unregisterContentObserver(this.mMobileDataObserver);
    }

    public void updateResources() {
        setText(R.string.pc_data_text);
    }

    private class MobileDataObserver extends ContentObserver {

        public MobileDataObserver() {
            super(new Handler());
        }

        public void onChange(boolean paramBoolean) {
            MobileDataSettingButton.this.updateStatus();
        }
    }

    @Override
    protected void updateSettings() {
        super.updateSettings();
        updateIcons();
    }
}
