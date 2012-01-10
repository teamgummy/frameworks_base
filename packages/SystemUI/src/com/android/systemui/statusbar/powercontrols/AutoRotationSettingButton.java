package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public final class AutoRotationSettingButton extends PowerControls
{
  private AutoRotationObserver mAutoRotationObserver = new AutoRotationObserver();

  public AutoRotationSettingButton(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  private void setRotationEnabled(int paramInt)
  {
    Settings.System.putInt(this.mContext.getContentResolver(), "accelerometer_rotation", paramInt);
  }

  private void updateIcons()
  {
    View localView = getRootView();
    ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_rotation_btn_icon);
    ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_rotation_btn_status_icon);
    switch (getActivateStatus())
    {
      default:
      case 1:
      pcIcon.setImageResource(R.drawable.pc_rotate_on);
      pcStatus.setImageResource(R.drawable.pc_icon_on);
      break;
      case 0:
      pcIcon.setImageResource(R.drawable.pc_rotate_off);
      pcStatus.setImageResource(R.drawable.pc_icon_off);
      break;
    }
  }

  private void updateStatus()
  {
    if (Settings.System.getInt(this.mContext.getContentResolver(), "accelerometer_rotation", 1) == 1)
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
    Log.e("AutoRotationSettingButton", "activate()");
    setRotationEnabled(1);
  }

  public void deactivate()
  {
    Log.e("AutoRotationSettingButton", "deactivate()");
    setRotationEnabled(0);
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("AutoRotationSettingButton", "onAttachedToWindow()");
    this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this.mAutoRotationObserver);
    updateStatus();
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("AutoRotationSettingButton", "onDetachedFromWindow()");
    this.mContext.getContentResolver().unregisterContentObserver(this.mAutoRotationObserver);
  }

  public void updateResources()
  {
    setText(R.string.pc_rotation_text);
  }

  private class AutoRotationObserver extends ContentObserver
  {
    public AutoRotationObserver()
    {
      super(new Handler());
    }

    public void onChange(boolean paramBoolean)
    {
      AutoRotationSettingButton.this.updateStatus();
    }
  }
}
