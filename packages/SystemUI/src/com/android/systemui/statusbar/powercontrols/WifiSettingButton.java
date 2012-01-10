package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public final class WifiSettingButton extends PowerControls
{
  private BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      int i = paramIntent.getIntExtra("wifi_state", 4);
      Log.e("WifiSettingButton", "onReceive()-S:" + i);
      WifiSettingButton.this.handleStateChanged(i);
    }
  };
  private WifiManager mWifiManager = null;

  public WifiSettingButton(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  private void handleStateChanged(int paramInt)
  {
    updateStatus(paramInt);
  }

  private void updateIconsAndTextColor()
  {
    View localView = getRootView();
    ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_wifi_btn_icon);
    ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_wifi_btn_status_icon);
    switch (getActivateStatus())
    {
    default:
    case 1:
      pcIcon.setImageResource(R.drawable.pc_wifi_on);
      pcStatus.setImageResource(R.drawable.pc_icon_on);
      break;
    case 0:
      pcIcon.setImageResource(R.drawable.pc_wifi_off);
      pcStatus.setImageResource(R.drawable.pc_icon_off);
      break;
    case 2:
      pcIcon.setImageResource(R.drawable.pc_wifi_on);
      pcStatus.setImageResource(R.drawable.pc_icon_ing);
      break;
    }
  }

  private void updateStatus(int paramInt)
  {
    switch (paramInt)
    {
    default:
    case 3:
      setActivateStatus(1);
      setSoundEffectsEnabled(true);
      updateIconsAndTextColor();
      break;
    case 1:
      setActivateStatus(0);
      setSoundEffectsEnabled(true);
      updateIconsAndTextColor();
      break;
    case 4:
      setActivateStatus(2);
      setSoundEffectsEnabled(false);
      updateIconsAndTextColor();
      break;
    case 0:
      setActivateStatus(2);
      setSoundEffectsEnabled(false);
      updateIconsAndTextColor();
    case 2:
      setActivateStatus(2);
      setSoundEffectsEnabled(false);
      updateIconsAndTextColor();
      break;
    }
  }

  public void activate()
  {
    Log.e("WifiSettingButton", "activate()");
    try
    {
      if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_enabled") == 0)
      {
        Log.d("WifiSettingButton", "Wifi is Disabled");
        Toast.makeText(this.mContext, "Security policy restricts use of the WLAN", 0).show();
      }
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
      if (this.mWifiManager != null)
        this.mWifiManager.setWifiEnabled(true);
    }
  }

  public void deactivate()
  {
    Log.e("WifiSettingButton", "deactivate()");
    if (this.mWifiManager != null)
      this.mWifiManager.setWifiEnabled(false);
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("WifiSettingButton", "onAttachedToWindow()");
    this.mContext.registerReceiver(this.mIntentReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"), null, null);
    this.mWifiManager = ((WifiManager)this.mContext.getSystemService("wifi"));
    if (this.mWifiManager != null)
      updateStatus(this.mWifiManager.getWifiState());
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("WifiSettingButton", "onDetachedFromWindow()");
    this.mContext.unregisterReceiver(this.mIntentReceiver);
  }

  public void updateResources()
  {
    setText(R.string.pc_wifi_text);
  }
}
