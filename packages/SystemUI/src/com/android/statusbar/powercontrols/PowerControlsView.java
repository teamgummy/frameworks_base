package com.android.systemui.statusbar.powercontrols;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class PowerControlsView extends LinearLayout
{
  public PowerControlsView(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
  }

  public int getSuggestedMinimumHeight()
  {
    return 0;
  }

  protected void onFinishInflate()
  {
    super.onFinishInflate();
  }
}
