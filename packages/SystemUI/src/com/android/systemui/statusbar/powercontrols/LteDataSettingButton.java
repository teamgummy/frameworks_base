package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;
import com.android.internal.telephony.Phone;

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
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;

public final class LteDataSettingButton extends PowerControls {

    private LteDataObserver mLteDataObserver = new LteDataObserver();

    private static final Mode SCREEN_MODE = Mode.MULTIPLY;

    private void setLTEDataEnabled(boolean paramBoolean) {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            Log.i("LteDataSettingButton", "setLTEDataEnabled: set to  = " + paramBoolean);
            tm.LTEtoggle(paramBoolean);
            Log.i("LteDataSettingButton", "setLTEDataEnabled : TelephonyManager = null");
        }
    }

    public LteDataSettingButton(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    private void updateIcons() {
        View localView = getRootView();
        ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_lte_btn_icon);
        ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_lte_btn_status_icon);
        switch (getActivateStatus()) {
            default:
            case 1:
                pcIcon.setImageResource(R.drawable.pc_lte_on);
                pcStatus.setImageResource(R.drawable.pc_icon_on);
                pcStatus.setColorFilter(mStatusColor, SCREEN_MODE);
                break;
            case 0:
                pcIcon.setImageResource(R.drawable.pc_lte_off);
                pcStatus.setColorFilter(null);
                pcStatus.setImageResource(R.drawable.pc_icon_off);
                break;
        }
    }

    private void updateStatus() {
        if (Settings.System.getInt(this.mContext.getContentResolver(), "lte_toggle", 1) == 1) {
            setActivateStatus(1);
            updateIcons();
        } else { 
            setActivateStatus(0);
            updateIcons();
        }
    }

    public void activate() {
        Log.e("LteDataSettingButton", "activate()");
        Settings.System.putInt(this.mContext.getContentResolver(), "lte_toggle", 1);
        setLTEDataEnabled(true);
    }

    public void deactivate() {
        Log.e("LteDataSettingButton", "deactivate()");
        Settings.System.putInt(this.mContext.getContentResolver(), "lte_toggle", 0);
        setLTEDataEnabled(false);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.e("LteDataSettingButton", "onAttachedToWindow()");
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("preferred_network_mode"), false, this.mLteDataObserver);
        updateStatus();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e("LteDataSettingButton", "onDetachedFromWindow()");
        this.mContext.getContentResolver().unregisterContentObserver(this.mLteDataObserver);
    }

    public void updateResources() {
        setText(R.string.pc_lte_text);
    }

    private class LteDataObserver extends ContentObserver {
        public LteDataObserver() {
            super(new Handler());
        }

        public void onChange(boolean paramBoolean) {
            LteDataSettingButton.this.updateStatus();
        }
    }

    @Override
    protected void updateSettings() {
        super.updateSettings();
        updateIcons();
    }
}
