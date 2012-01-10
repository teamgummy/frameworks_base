package com.android.systemui.statusbar.powercontrols;

import com.android.systemui.R;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public final class BluetoothSettingButton extends PowerControls
{
  private BluetoothAdapter mBluetoothAdapter = null;
  private BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      int i = paramIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
      Log.e("BluetoothSettingButton", "onReceive()-S:" + i);
      BluetoothSettingButton.this.handleStateChanged(i);
    }
  };

  public BluetoothSettingButton(Context paramContext, AttributeSet paramAttributeSet)
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
    ImageView pcIcon = (ImageView)localView.findViewById(R.id.pc_bt_btn_icon);
    ImageView pcStatus = (ImageView)localView.findViewById(R.id.pc_bt_btn_status_icon);
    switch (getActivateStatus())
    {
    default:
    case 0:
      pcIcon.setImageResource(R.drawable.pc_bluetooth_off);
      pcStatus.setImageResource(R.drawable.pc_icon_off);
      break;
    case 1:
      pcIcon.setImageResource(R.drawable.pc_bluetooth_on);
      pcStatus.setImageResource(R.drawable.pc_icon_on);
      break;
    case 2:
      pcIcon.setImageResource(R.drawable.pc_bluetooth_on);
      pcStatus.setImageResource(R.drawable.pc_icon_ing);
      break;
    }
  }

  private void updateStatus(int paramInt)
  {
    switch (paramInt)
    {
    default:
    case 12:
      setActivateStatus(1);
      setSoundEffectsEnabled(true);
      updateIconsAndTextColor();
      break;
    case 10:
      setActivateStatus(0);
      setSoundEffectsEnabled(true);
      updateIconsAndTextColor();
      break;
    case 11:
      setActivateStatus(2);
      setSoundEffectsEnabled(false);
      updateIconsAndTextColor();
      break;
    case 13:
      setActivateStatus(2);
      setSoundEffectsEnabled(false);
      updateIconsAndTextColor();
      break;
    }
  }

  public void activate()
  {
    Log.e("BluetoothSettingButton", "activate()");
    this.mBluetoothAdapter.enable();
  }

  public void deactivate()
  {
    Log.e("BluetoothSettingButton", "deactivate()");
    this.mBluetoothAdapter.disable();
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("BluetoothSettingButton", "onAttachedToWindow()");
    this.mContext.registerReceiver(this.mIntentReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), null, null);
    this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (this.mBluetoothAdapter != null)
      updateStatus(this.mBluetoothAdapter.getState());
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("BluetoothSettingButton", "onDetachedFromWindow()");
    this.mContext.unregisterReceiver(this.mIntentReceiver);
  }

  public void updateResources()
  {
    setText(R.string.pc_bluetooth_text);
  }
}
