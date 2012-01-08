package com.android.systemui.statusbar.powercontrols;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public abstract class PowerControls extends TextView
  implements View.OnClickListener
{
  private int mActivateStatus;
  private BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      PowerControls.this.updateResources();
    }
  };
  private View mRootView = null;

  public PowerControls(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  abstract void activate();

  abstract void deactivate();

  protected int getActivateStatus()
  {
    return this.mActivateStatus;
  }

  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    Log.e("PowerControls", "onAttachedToWindow()");
    this.mRootView = getRootView();
    setOnClickListener(this);
    this.mContext.registerReceiver(this.mIntentReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"), null, null);
  }

  public void onClick(View paramView)
  {
    if (1 == this.mActivateStatus)
    {
      deactivate();
    }
    else
      activate();
  }

  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Log.e("PowerControls", "onDetachedFromWindow()");
  }

  protected void setActivateStatus(int paramInt)
  {
    this.mActivateStatus = paramInt;
  }

  abstract void updateResources();
}
