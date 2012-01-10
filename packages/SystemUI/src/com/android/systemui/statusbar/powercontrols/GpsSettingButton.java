package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

public final class GpsSettingButton extends PowerControls
{
  private GpsObserver mGpsObserver = new GpsObserver();

  public GpsSettingButton(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  private void setGPSEnabled(boolean paramBoolean)
  {
    Settings.Secure.setLocationProviderEnabled(this.mContext.getContentResolver(), "gps", paramBoolean);
    Intent localIntent = new Intent("com.android.internal.location.intent.action.LBS_ENABLED_CHANGE");
    if (!paramBoolean)
    {
      localIntent.putExtra("lbsEnabled", true);
      this.mContext.sendBroadcast(localIntent);
    }
  }

  private void updateIcons()
  {
    View localView = getRootView();
    ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_gps_btn_icon);
    ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_gps_btn_status_icon);
    switch (getActivateStatus())
    {
      default:
      case 1:
        pcIcon.setImageResource(R.drawable.pc_gps_on);
        pcStatus.setImageResource(R.drawable.pc_icon_on);
        break;
      case 0:
        pcIcon.setImageResource(R.drawable.pc_gps_off);
        pcStatus.setImageResource(R.drawable.pc_icon_off);
        break;
    }
  }

  private void updateStatus()
  {
    if (Settings.Secure.isLocationProviderEnabled(this.mContext.getContentResolver(), "gps"))
    {
      setActivateStatus(1);
      updateIcons();
    }
    else
    {
      setActivateStatus(0);
      updateIcons();
    }
  }

  public void activate()
  {
    Log.e("GpsSettingButton", "activate()");
    setGPSEnabled(true);
  }

  public void deactivate()
  {
    Log.e("GpsSettingButton", "deactivate()");
    setGPSEnabled(false);
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("GpsSettingButton", "onAttachedToWindow()");
    this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), false, this.mGpsObserver);
    updateStatus();
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("GpsSettingButton", "onDetachedFromWindow()");
    this.mContext.getContentResolver().unregisterContentObserver(this.mGpsObserver);
  }

  public void updateResources()
  {
    setText(R.string.pc_gps_text);
  }

  private class GpsObserver extends ContentObserver
  {
    public GpsObserver()
    {
      super(new Handler());
    }

    public void onChange(boolean paramBoolean)
    {
      GpsSettingButton.this.updateStatus();
    }
  }
}
