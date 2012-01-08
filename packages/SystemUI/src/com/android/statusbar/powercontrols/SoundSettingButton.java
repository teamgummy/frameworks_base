package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public final class SoundSettingButton extends PowerControls
{
  private static int mSoundProfile;
  private static int mSoundText;
  private static int mVibProfile;
  private AudioManager mAudioManager = null;
  private BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      String str = paramIntent.getAction();
      if ("android.media.RINGER_MODE_CHANGED".equals(str))
      {
        mSoundProfile = (paramIntent.getIntExtra("android.media.EXTRA_RINGER_MODE", 2));
        Log.e("SoundSettingButton", "onReceive()-S:" + SoundSettingButton.mSoundProfile);
        SoundSettingButton.this.updateStatus();
        if ((!"android.media.VIBRATE_SETTING_CHANGED".equals(str)) || (paramIntent.getIntExtra("android.media.EXTRA_VIBRATE_TYPE", 0) != 0))
        mVibProfile = (paramIntent.getIntExtra("android.media.EXTRA_VIBRATE_SETTING", 2));
        Log.e("SoundSettingButton", "onReceive()-V:" + SoundSettingButton.mVibProfile);
      }
    }
  };

  public SoundSettingButton(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  private void updateIconsAndText()
  {
    View localView = getRootView();
    ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_sound_btn_icon);
    ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_sound_btn_status_icon);
    setPadding(0, 82, 0, 0);
    switch (getActivateStatus())
    {
    default:
    case 1:
      if (1 == mVibProfile)
      {
        pcIcon.setImageResource(R.drawable.pc_sound_on);
        pcStatus.setImageResource(R.drawable.pc_icon_on);
        mSoundText = R.string.pc_sound_vibration_text;
        setPadding(0, 67, 0, 0);
      }
      else
      {
        pcIcon.setImageResource(R.drawable.pc_sound_on);
        pcStatus.setImageResource(R.drawable.pc_icon_on);
        mSoundText = R.string.pc_sound_text;
      }
      break;
    case 0:
      if (mSoundProfile == 0)
      {
        pcIcon.setImageResource(R.drawable.pc_sound_off);
        pcStatus.setImageResource(R.drawable.pc_icon_off);
        mSoundText = R.string.pc_silent_text;
      }
      else
      {
        pcIcon.setImageResource(R.drawable.pc_sound_vibrate);
        pcStatus.setImageResource(R.drawable.pc_icon_off);
        mSoundText = R.string.pc_vibration_text;
      }
    break;
    }
  }

  private void updateStatus()
  {
    if (2 == mSoundProfile)
    {
      setActivateStatus(1);
      updateIconsAndText();
    }
    else
    {
      setActivateStatus(0);
      updateIconsAndText();
    }
  }

  public void activate()
  {
    Log.e("SoundSettingButton", "activate()");
    this.mAudioManager.setRingerMode(2);
  }

  public void deactivate()
  {
    int i = 0;
    switch (mVibProfile)
    {
    default:
    case 0:
      Log.e("SoundSettingButton", "deactivate()-S:" + i + " V:" + mVibProfile);
      i = 0;
      this.mAudioManager.setRingerMode(i);
      break;
    case 1:
      if (Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_in_silent", 0) == 1)
      {
        i = 1;
      }
      else
        i = 0;
      this.mAudioManager.setRingerMode(i);
      break;
    case 2:
      i = 1;
      this.mAudioManager.setRingerMode(i);
      break;
    }
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("SoundSettingButton", "onAttachedToWindow()");
    IntentFilter localIntentFilter = new IntentFilter();
    localIntentFilter.addAction("android.media.RINGER_MODE_CHANGED");
    localIntentFilter.addAction("android.media.VIBRATE_SETTING_CHANGED");
    this.mContext.registerReceiver(this.mIntentReceiver, localIntentFilter, null, null);
    this.mAudioManager = ((AudioManager)this.mContext.getSystemService("audio"));
    if (this.mAudioManager != null)
    {
      mSoundProfile = this.mAudioManager.getRingerMode();
      mVibProfile = this.mAudioManager.getVibrateSetting(0);
      updateStatus();
      Log.e("SoundSettingButton", "mAudioManager is null");
    }
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("SoundSettingButton", "onDetachedFromWindow()");
    this.mContext.unregisterReceiver(this.mIntentReceiver);
  }

  public void updateResources()
  {
    setText(mSoundText);
  }
}
