/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.policy.impl;

import com.android.internal.R;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.RotarySelector;
import com.android.internal.widget.RingSelector;
import com.android.internal.widget.SenseLikeLock;
import com.android.internal.widget.SlidingTab;
import com.android.internal.widget.WaveView;
import com.android.internal.widget.multiwaveview.MultiWaveView;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.util.Log;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

/**
 * The screen within {@link LockPatternKeyguardView} that shows general
 * information about the device depending on its state, and how to get past it,
 * as applicable.
 */
class LockScreen extends LinearLayout implements KeyguardScreen {

	private static final int ON_RESUME_PING_DELAY = 500; // delay first ping
															// until the screen
															// is on
	private static final boolean DBG = false;
	private static final String TAG = "LockScreen";
	private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
	private static final int WAIT_FOR_ANIMATION_TIMEOUT = 0;
	private static final int STAY_ON_WHILE_GRABBED_TIMEOUT = 30000;

	private LockPatternUtils mLockPatternUtils;
	private KeyguardUpdateMonitor mUpdateMonitor;
	private KeyguardScreenCallback mCallback;
    
	// current configuration state of keyboard and display
	private int mKeyboardHidden;
	private int mCreationOrientation;

	private boolean mSilentMode;
	private AudioManager mAudioManager;
	private boolean mEnableMenuKeyInLockScreen;

	private KeyguardStatusViewManager mStatusViewManager;
	private UnlockWidgetCommonMethods mUnlockWidgetMethods;
	private UnlockWidgetCommonMethods mUnlockWidgetMethods2;
	private View mUnlockWidget;
	private View mUnlockWidget2;
	
	//stupid duct tape fix till i can make the layout see who is boss
    private boolean stupidFix = Settings.System.getInt(mContext.getContentResolver(), Settings.System.MUSIC_WIDGET_TYPE, 0) == 1;

	// lockscreen toggles
	private boolean mLockscreenCustom = (Settings.System.getInt(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_EXTRA_ICONS, 0) == 1);
	// camera or sound
	private boolean mForceSoundIcon = (Settings.System.getInt(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_FORCE_SOUND_ICON, 0) == 1);
	// toggles used between lockscreen types
	private boolean mUseSlider = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 1);
	private boolean mUseRotary = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 2);
	private boolean mRotaryRevamp = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 3);
	private boolean mUseHoneyComb = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 4);
	private boolean mUseRings = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 5);
	private boolean mUseSense = (Settings.System.getInt(
			mContext.getContentResolver(), Settings.System.LOCKSCREEN_TYPE, 0) == 6);

	// omg ring lock?!
	private String[] mCustomRingAppActivities = new String[] {
			Settings.System.getString(mContext.getContentResolver(),
					Settings.System.LOCKSCREEN_CUSTOM_RING_APP_ACTIVITIES[0]),
			Settings.System.getString(mContext.getContentResolver(),
					Settings.System.LOCKSCREEN_CUSTOM_RING_APP_ACTIVITIES[1]),
			Settings.System.getString(mContext.getContentResolver(),
					Settings.System.LOCKSCREEN_CUSTOM_RING_APP_ACTIVITIES[2]),
			Settings.System.getString(mContext.getContentResolver(),
					Settings.System.LOCKSCREEN_CUSTOM_RING_APP_ACTIVITIES[3]) };

	// custom apps made easy!
	private String mCustomOne = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_ONE));
	private String mCustomTwo = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_TWO));
	private String mCustomThree = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_THREE));
	private String mCustomFour = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_FOUR));
	private String mCustomFive = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_FIVE));
	private String mCustomSix = (Settings.System.getString(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_CUSTOM_SIX));
	
	private Drawable customAppIcon1;
	private Drawable customAppIcon2;
	private Drawable customAppIcon3;
	private Drawable customAppIcon4;
	private Drawable customAppIcon5;
	private Drawable customAppIcon6;

	// hide rotary arrows
	private boolean mHideArrows = (Settings.System.getInt(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_HIDE_ARROWS, 0) == 1);
	// rotary lock pull down
	private boolean mRotaryDown = (Settings.System.getInt(
			mContext.getContentResolver(),
			Settings.System.LOCKSCREEN_ROTARY_UNLOCK_DOWN, 0) == 1);

	private Bitmap mCustomAppIcon;
	private String mCustomAppName;
	private Bitmap[] mCustomRingAppIcons = new Bitmap[4];
	private Intent[] mCustomApps = new Intent[4];

	private interface UnlockWidgetCommonMethods {
		// Update resources based on phone state
		public void updateResources();

		// Get the view associated with this widget
		public View getView();

		// Reset the view
		public void reset(boolean animate);

		// Animate the widget if it supports ping()
		public void ping();
	}

	class SlidingTabMethods implements SlidingTab.OnTriggerListener,
			UnlockWidgetCommonMethods {
		private final SlidingTab mSlidingTab;

		SlidingTabMethods(SlidingTab slidingTab) {
			mSlidingTab = slidingTab;
		}

		public void updateResources() {
			boolean vibe = mSilentMode
					&& (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);
			if (!mForceSoundIcon) {
				mSlidingTab.setRightTabResources(R.drawable.ic_jog_dial_camera,
						R.drawable.jog_tab_target_gray,
						R.drawable.jog_tab_bar_right_generic,
						R.drawable.jog_tab_right_generic);
			} else {
				mSlidingTab.setRightTabResources(
						mSilentMode ? (vibe ? R.drawable.ic_jog_dial_vibrate_on
								: R.drawable.ic_jog_dial_sound_off)
								: R.drawable.ic_jog_dial_sound_on,
						mSilentMode ? R.drawable.jog_tab_target_yellow
								: R.drawable.jog_tab_target_gray,
						mSilentMode ? R.drawable.jog_tab_bar_right_sound_on
								: R.drawable.jog_tab_bar_right_sound_off,
						mSilentMode ? R.drawable.jog_tab_right_sound_on
								: R.drawable.jog_tab_right_sound_off);
			}
		}

		/** {@inheritDoc} */
		public void onTrigger(View v, int whichHandle) {
			if (whichHandle == SlidingTab.OnTriggerListener.LEFT_HANDLE) {
				mCallback.goToUnlockScreen();
			} else if (whichHandle == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
				if (!mForceSoundIcon) {
					// Start the Camera
					Intent intent = new Intent(
							MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(intent);
					mCallback.goToUnlockScreen();
				} else {
					toggleRingMode();
					mUnlockWidgetMethods.updateResources();
					mCallback.pokeWakelock();
				}
			}
		}

		/** {@inheritDoc} */
		public void onGrabbedStateChange(View v, int grabbedState) {
			if (grabbedState == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
				mSilentMode = isSilentMode();
				if (!mForceSoundIcon) {
					mSlidingTab
							.setRightHintText(R.string.zzlockscreen_camera_label);
				} else {
					mSlidingTab
							.setRightHintText(mSilentMode ? R.string.lockscreen_sound_on_label
									: R.string.lockscreen_sound_off_label);
				}
			}
			// Don't poke the wake lock when returning to a state where the
			// handle is
			// not grabbed since that can happen when the system (instead of the
			// user)
			// cancels the grab.
			if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
				mCallback.pokeWakelock();
			}
		}

		public View getView() {
			return mSlidingTab;
		}

		public void reset(boolean animate) {
			mSlidingTab.reset(animate);
		}

		public void ping() {
		}
	}

	class SlidingTabMethods2 implements SlidingTab.OnTriggerListener,
			UnlockWidgetCommonMethods {
		private final SlidingTab mSlidingTab2;

		SlidingTabMethods2(SlidingTab slidingTab) {
			mSlidingTab2 = slidingTab;
		}

		public void updateResources() {
		}

		/** {@inheritDoc} */
		public void onTrigger(View v, int whichHandle) {
			if (whichHandle == SlidingTab.OnTriggerListener.LEFT_HANDLE) {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getContext().startActivity(intent);
				mCallback.goToUnlockScreen();
			} else if (whichHandle == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
				if (mCustomOne != null) {
					runActivity(mCustomOne);
				}
			}
		}

		/** {@inheritDoc} */
		public void onGrabbedStateChange(View v, int grabbedState) {
			mCallback.pokeWakelock();
		}

		public View getView() {
			return mSlidingTab2;
		}

		public void reset(boolean animate) {
			mSlidingTab2.reset(animate);
		}

		public void ping() {
		}
	}

	class RotarySelectorMethods implements
			RotarySelector.OnDialTriggerListener, UnlockWidgetCommonMethods {
		private final RotarySelector mRotarySelector;

		RotarySelectorMethods(RotarySelector rotarySelector) {
			mRotarySelector = rotarySelector;
		}

		public void updateResources() {
			boolean vibe = mSilentMode
					&& (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);
			if (!mForceSoundIcon) {
				mRotarySelector
						.setRightHandleResource(R.drawable.ic_jog_dial_camera);
			} else {
				mRotarySelector
						.setRightHandleResource(mSilentMode ? (vibe ? R.drawable.ic_jog_dial_vibrate_on
								: R.drawable.ic_jog_dial_sound_off)
								: R.drawable.ic_jog_dial_sound_on);
			}

		}

		/** {@inheritDoc} */
		public void onDialTrigger(View v, int whichHandle) {
			boolean mUnlockTrigger = false;
			boolean mCustomAppTrigger = false;

			if (whichHandle == RotarySelector.OnDialTriggerListener.LEFT_HANDLE) {
				if (mRotaryDown)
					mCustomAppTrigger = true;
				else
					mUnlockTrigger = true;
			}

			if (whichHandle == RotarySelector.OnDialTriggerListener.MID_HANDLE) {
				if (mRotaryDown)
					mUnlockTrigger = true;
				else
					mCustomAppTrigger = true;
			}

			if (mUnlockTrigger) {
				mCallback.goToUnlockScreen();
			} else if (mCustomAppTrigger) {
				if (mCustomOne != null) {
					runActivity(mCustomOne);
				}
			} else if (whichHandle == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
				if (!mForceSoundIcon) {
					// Start the Camera
					Intent intent = new Intent(
							MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(intent);
					mCallback.goToUnlockScreen();
				} else {
					toggleRingMode();
					mUnlockWidgetMethods.updateResources();
					mCallback.pokeWakelock();
				}

			}
		}

		/** {@inheritDoc} */
		public void onGrabbedStateChange(View v, int grabbedState) {
			if (grabbedState == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
				mSilentMode = isSilentMode();
			}
		}

		public View getView() {
			return mRotarySelector;
		}

		public void reset(boolean animate) {
			mRotarySelector.reset();
		}

		public void ping() {
		}
	}

	class WaveViewMethods implements WaveView.OnTriggerListener,
			UnlockWidgetCommonMethods {

		private final WaveView mWaveView;

		WaveViewMethods(WaveView waveView) {
			mWaveView = waveView;
		}

		/** {@inheritDoc} */
		public void onTrigger(View v, int whichHandle) {
			if (whichHandle == WaveView.OnTriggerListener.CENTER_HANDLE) {
				requestUnlockScreen();
			}
		}

		/** {@inheritDoc} */
		public void onGrabbedStateChange(View v, int grabbedState) {
			// Don't poke the wake lock when returning to a state where the
			// handle is
			// not grabbed since that can happen when the system (instead of the
			// user)
			// cancels the grab.
			if (grabbedState == WaveView.OnTriggerListener.CENTER_HANDLE) {
				mCallback.pokeWakelock(STAY_ON_WHILE_GRABBED_TIMEOUT);
			}
		}

		public void updateResources() {
		}

		public View getView() {
			return mWaveView;
		}

		public void reset(boolean animate) {
			mWaveView.reset();
		}

		public void ping() {
		}
	}

	class MultiWaveViewMethods implements MultiWaveView.OnTriggerListener,
			UnlockWidgetCommonMethods {

		private final MultiWaveView mMultiWaveView;
		private boolean mCameraDisabled;

		MultiWaveViewMethods(MultiWaveView multiWaveView) {
			mMultiWaveView = multiWaveView;
			final boolean cameraDisabled = mLockPatternUtils
					.getDevicePolicyManager().getCameraDisabled(null);
			if (cameraDisabled || mForceSoundIcon) {
				Log.v(TAG, "Camera disabled by Device Policy");
				mCameraDisabled = true;
			} else {
				// Camera is enabled if resource is initially defined for
				// MultiWaveView
				// in the lockscreen layout file
				mCameraDisabled = mMultiWaveView.getTargetResourceId() != R.array.lockscreen_targets_with_camera;
			}
		}

		public void updateResources() {
			int resId;
			if (mCameraDisabled) {
				// Fall back to showing ring/silence if camera is disabled by
				// DPM...
				if (mLockscreenCustom) {
					resId = mSilentMode ? R.array.zzlockscreen_when_silent
							: R.array.zzlockscreen_extra_apps_soundon;

				} else {
					resId = mSilentMode ? R.array.lockscreen_targets_when_silent
							: R.array.lockscreen_targets_when_soundon;
				}
			} else if (!mCameraDisabled && mLockscreenCustom) {
				resId = R.array.zzlockscreen_extra_apps;
			} else {
				resId = R.array.lockscreen_targets_with_camera;
			}
			
			if (mLockscreenCustom) {
				mMultiWaveView.setTargetResources(getDrawIcons(resId));
			} else {
				mMultiWaveView.setTargetResources(resId);
			}

		}

		public void onGrabbed(View v, int handle) {

		}

		public void onReleased(View v, int handle) {

		}

		public void onTrigger(View v, int target) {
			if (mLockscreenCustom) {
				if (target == 0) { // 0 = unlock on the right
					mCallback.goToUnlockScreen();
				} else if (target == 1) { // 1 = Custom App, no need for default since null = blank icon
					if (mCustomOne != null) {
						runActivity(mCustomOne);
					}
				} else if (target == 2) { // 2 = Custom App, no need for default since null = blank icon
					if (mCustomTwo != null) {
						runActivity(mCustomTwo);
					}
				} else if (target == 3) { // 3 = Custom App, no default, shows blank when not used
					if (mCustomThree != null) {
						runActivity(mCustomThree);
					}
				} else if (target == 4) { //4 = Camera/Sound toggle
					if (!mCameraDisabled) {
						// Start the Camera
						Intent intent = new Intent(
								MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mContext.startActivity(intent);
						mCallback.goToUnlockScreen();
					} else {
						toggleRingMode();
						mUnlockWidgetMethods.updateResources();
						mCallback.pokeWakelock();
					}
				} else if (target == 5) {
					if (mCustomFour != null) {
						runActivity(mCustomFour);
					}
				} else if (target == 6) {
					if (mCustomFive != null) {
						runActivity(mCustomFive);
					}
				} else if (target == 7) {
					if (mCustomSix != null) {
						runActivity(mCustomSix);
					}
				}
			} else {
				if (target == 0 || target == 1) { // 0 = unlock/portrait, 1 =
													// unlock/landscape
					mCallback.goToUnlockScreen();
				} else if (target == 2 || target == 3) { // 2 = alt/portrait, 3
															// = alt/landscape
					if (!mCameraDisabled) {
						// Start the Camera
						Intent intent = new Intent(
								MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mContext.startActivity(intent);
						mCallback.goToUnlockScreen();
					} else {
						toggleRingMode();
						mUnlockWidgetMethods.updateResources();
						mCallback.pokeWakelock();
					}
				}
			}
		}

		public void onGrabbedStateChange(View v, int handle) {
			// Don't poke the wake lock when returning to a state where the
			// handle is
			// not grabbed since that can happen when the system (instead of the
			// user)
			// cancels the grab.
			if (handle != MultiWaveView.OnTriggerListener.NO_HANDLE) {
				mCallback.pokeWakelock();
			}
		}

		public View getView() {
			return mMultiWaveView;
		}

		public void reset(boolean animate) {
			mMultiWaveView.reset(animate);
		}

		public void ping() {
			mMultiWaveView.ping();
		}
	}

	class SenseLikeLockMethods implements
			SenseLikeLock.OnSenseLikeSelectorTriggerListener,
			UnlockWidgetCommonMethods {
		private final SenseLikeLock mSenseLikeLock;

		SenseLikeLockMethods(SenseLikeLock senseLikeLock) {
			mSenseLikeLock = senseLikeLock;
		}

		public void updateResources() {
		}

		/** {@inheritDoc} */
		public void onSenseLikeSelectorTrigger(View v, int Trigger) {
			mCallback.goToUnlockScreen();

			switch (Trigger) {
			case SenseLikeLock.OnSenseLikeSelectorTriggerListener.LOCK_ICON_SHORTCUT_ONE_TRIGGERED: {
				runActivity(mCustomRingAppActivities[0]);
				mCallback.goToUnlockScreen();
				break;
			}

			case SenseLikeLock.OnSenseLikeSelectorTriggerListener.LOCK_ICON_SHORTCUT_TWO_TRIGGERED: {
				runActivity(mCustomRingAppActivities[1]);
				mCallback.goToUnlockScreen();
				break;
			}

			case SenseLikeLock.OnSenseLikeSelectorTriggerListener.LOCK_ICON_SHORTCUT_THREE_TRIGGERED: {
				runActivity(mCustomRingAppActivities[2]);
				mCallback.goToUnlockScreen();
				break;
			}

			case SenseLikeLock.OnSenseLikeSelectorTriggerListener.LOCK_ICON_SHORTCUT_FOUR_TRIGGERED: {
				runActivity(mCustomRingAppActivities[3]);
				mCallback.goToUnlockScreen();
				break;
			}

			case SenseLikeLock.OnSenseLikeSelectorTriggerListener.LOCK_ICON_TRIGGERED: {
				mCallback.goToUnlockScreen();
				break;
			}
			}
		}

		/** {@inheritDoc} */
		public void OnSenseLikeSelectorGrabbedStateChanged(View v,
				int grabbedState) {
			mCallback.pokeWakelock();
		}

		public View getView() {
			return mSenseLikeLock;
		}

		public void reset(boolean animate) {
		}

		public void ping() {
		}
	}

	class RingSelectorMethods implements RingSelector.OnRingTriggerListener,
			UnlockWidgetCommonMethods {
		private final RingSelector mRingSelector;

		RingSelectorMethods(RingSelector ringSelector) {
			mRingSelector = ringSelector;
		}

		public void updateResources() {
			boolean vibe = mSilentMode
					&& (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);

			if (!mForceSoundIcon) {
				mRingSelector.setRightRingResources(
						R.drawable.ic_jog_dial_camera,
						R.drawable.jog_tab_target_gray,
						R.drawable.jog_ring_ring_gray);
			} else {
				mRingSelector.setRightRingResources(
						mSilentMode ? (vibe ? R.drawable.ic_jog_dial_vibrate_on
								: R.drawable.ic_jog_dial_sound_off)
								: R.drawable.ic_jog_dial_sound_on,
						mSilentMode ? R.drawable.jog_tab_target_yellow
								: R.drawable.jog_tab_target_gray,
						mSilentMode ? R.drawable.jog_ring_ring_yellow
								: R.drawable.jog_ring_ring_gray);
			}
		}

		/** {@inheritDoc} */
		public void onRingTrigger(View v, int whichRing, int whichApp) {
			if (whichRing == RingSelector.OnRingTriggerListener.LEFT_RING) {
				mCallback.goToUnlockScreen();
			} else if (whichRing == RingSelector.OnRingTriggerListener.RIGHT_RING) {
				if (!mForceSoundIcon) {
					// Start the Camera
					Intent intent = new Intent(
							MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(intent);
					mCallback.goToUnlockScreen();
				} else {
					toggleRingMode();
					mUnlockWidgetMethods.updateResources();
					mCallback.pokeWakelock();
				}
			} else if (whichRing == RingSelector.OnRingTriggerListener.MIDDLE_RING) {
				if (mCustomRingAppActivities[whichApp] != null) {
					runActivity(mCustomRingAppActivities[whichApp]);
				}
			}
		}

		/** {@inheritDoc} */
		public void onGrabbedStateChange(View v, int grabbedState) {
			if (grabbedState == RingSelector.OnRingTriggerListener.RIGHT_RING) {
				mSilentMode = isSilentMode();
			}
			// Don't poke the wake lock when returning to a state where the
			// handle is
			// not grabbed since that can happen when the system (instead of the
			// user)
			// cancels the grab.
			if (grabbedState != RingSelector.OnRingTriggerListener.NO_RING) {
				mCallback.pokeWakelock();
			}
		}

		public View getView() {
			return mRingSelector;
		}

		public void reset(boolean animate) {
			mRingSelector.reset(animate);
		}

		public void ping() {
		}
	}

	private void requestUnlockScreen() {
		// Delay hiding lock screen long enough for animation to finish
		postDelayed(new Runnable() {
			public void run() {
				mCallback.goToUnlockScreen();
			}
		}, WAIT_FOR_ANIMATION_TIMEOUT);
	}

	private void toggleRingMode() {
		// toggle silent mode
		mSilentMode = !mSilentMode;
		Handler handler = new Handler();
		if (mSilentMode) {
			final boolean vibe = (Settings.System.getInt(
					mContext.getContentResolver(),
					Settings.System.VIBRATE_IN_SILENT, 1) == 1);

			mAudioManager.setRingerMode(vibe ? AudioManager.RINGER_MODE_VIBRATE
					: AudioManager.RINGER_MODE_SILENT);
			// add a Toastbox to popup for sound on/off
			mStatusViewManager.toastMessage(0);
			// add delay to switch back to normal
			handler.postDelayed(new Runnable() {
				public void run() {
					mStatusViewManager.toastMessage(2);
				}
			}, 500);
		} else {
			mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			// add a Toastbox to popup for sound on/off
			mStatusViewManager.toastMessage(1);
			// add delay to switch back to normal
			handler.postDelayed(new Runnable() {
				public void run() {
					mStatusViewManager.toastMessage(2);
				}
			}, 500);
		}
	}

	private void runActivity(String uri) {
		try {
			Intent i = Intent.parseUri(uri, 0);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			mContext.startActivity(i);
			mCallback.goToUnlockScreen();
		} catch (URISyntaxException e) {
		} catch (ActivityNotFoundException e) {
		}
	}

	/**
	 * In general, we enable unlocking the insecure key guard with the menu key.
	 * However, there are some cases where we wish to disable it, notably when
	 * the menu button placement or technology is prone to false positives.
	 * 
	 * @return true if the menu key should be enabled
	 */
	private boolean shouldEnableMenuKey() {
		final Resources res = getResources();
		final boolean configDisabled = res
				.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
		final boolean isTestHarness = ActivityManager.isRunningInTestHarness();
		final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
		return !configDisabled || isTestHarness || fileOverride;
	}

	/**
	 * Setup the sense lockscreen, clean this up later because it is
	 * disgustingly messy Need to recode to get rid of the mCustomApps[] Intent
	 * as I have removed it from being needed in widget setup to start the
	 * activity. Also thank you OMFGB for this :)
	 */
	private void setupSenseLikeRingShortcuts(Context context,
			SenseLikeLock senseringselector) {

		int numapps = 0;
		Intent intent = new Intent();
		PackageManager pm = context.getPackageManager();
		Drawable[] shortcutsicons;

		Log.d(TAG, "Seting up sense ring");
		for (int i = 0; i < mCustomRingAppActivities.length; i++) {
			if (mCustomRingAppActivities[i] != null) {
				numapps++;
			}
		}

		Log.d(TAG, "Setting intents");
		if (numapps != 4) {
			Log.d(TAG, "Seting defaults");
			mCustomApps = senseringselector.setDefaultIntents();
			for (int i = 0; i < 4; i++) {
				if (mCustomRingAppActivities[i] != null) {
					Log.d(TAG, "Setting custom intent #" + i);
					try {
						intent = Intent
								.parseUri(mCustomRingAppActivities[i], 0);
					} catch (java.net.URISyntaxException ex) {
						Log.w(TAG, "Invalid hotseat intent: "
								+ mCustomRingAppActivities[i]);
						// bogus; leave intent=null
					}

				}
			}

			numapps = 4;
		} else
			for (int i = 0; i < numapps; i++) {
				Log.d(TAG, "Setting custom intent #" + i);
				try {
					intent = Intent.parseUri(mCustomRingAppActivities[i], 0);
				} catch (java.net.URISyntaxException ex) {
					Log.w(TAG, "Invalid hotseat intent: "
							+ mCustomRingAppActivities[i]);
					ex.printStackTrace();
				}

				ResolveInfo bestMatch = pm.resolveActivity(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
				List<ResolveInfo> allMatches = pm.queryIntentActivities(intent,
						PackageManager.MATCH_DEFAULT_ONLY);

				if (DBG) {
					Log.d(TAG, "Best match for intent: " + bestMatch);
					Log.d(TAG, "All matches: ");
					for (ResolveInfo ri : allMatches) {
						Log.d(TAG, " --> " + ri);
					}
				}

				ComponentName com = new ComponentName(
						bestMatch.activityInfo.applicationInfo.packageName,
						bestMatch.activityInfo.name);

				mCustomApps[i] = new Intent(Intent.ACTION_MAIN)
						.setComponent(com);
			}

		shortcutsicons = new Drawable[numapps];
		float iconScale = getResources().getDisplayMetrics().density;

		for (int i = 0; i < numapps; i++) {
			try {
				shortcutsicons[i] = pm.getActivityIcon(mCustomApps[i]);
				shortcutsicons[i] = scaledDrawable(shortcutsicons[i], context,
						iconScale);
			} catch (ArrayIndexOutOfBoundsException ex) {
				Log.w(TAG, "Missing shortcut_icons array item #" + i);
				shortcutsicons[i] = null;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
				shortcutsicons[i] = null;
				// Do-Nothing
			}
		}

		// Finnally set the images
		senseringselector.setShortCutsDrawables(shortcutsicons[0],
				shortcutsicons[1], shortcutsicons[2], shortcutsicons[3]);

	}

	private Drawable scaledDrawable(Drawable icon, Context context, float scale) {
		final Resources resources = context.getResources();
		int sIconHeight = (int) resources
				.getDimension(android.R.dimen.app_icon_size);
		int sIconWidth = sIconHeight;

		int width = sIconWidth;
		int height = sIconHeight;
		Bitmap original;
		try {
			original = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
		} catch (OutOfMemoryError e) {
			return icon;
		}

		Canvas canvas = new Canvas(original);
		canvas.setBitmap(original);
		icon.setBounds(0, 0, width, height);
		icon.draw(canvas);

		try {
			Bitmap endImage = Bitmap.createScaledBitmap(original,
					(int) (width * scale), (int) (height * scale), true);
			original.recycle();
			return new FastBitmapDrawable(endImage);
		} catch (OutOfMemoryError e) {
			return icon;
		}
	}

	private Drawable[] getDrawIcons(int resId) {
		Resources res = getContext().getResources();
		TypedArray array = res.obtainTypedArray(resId);
		Drawable[] icons = new Drawable[8];
		// i really need to make this better one day....
		Drawable d0 = array.getDrawable(0);
		icons[0] = d0;
		if (customAppIcon1 != null) {
			icons[1] = customAppIcon1;
		} else {
			Drawable d1 = array.getDrawable(1);
			icons[1] = d1;
		}

		if (customAppIcon2 != null) {
			icons[2] = customAppIcon2;
		} else {
			Drawable d2 = array.getDrawable(2);
			icons[2] = d2;
		}
		
		if (customAppIcon3 != null) {
			icons[3] = customAppIcon3;
		} else {
			Drawable d3 = array.getDrawable(3);
			icons[3] = d3;
		}

		Drawable d4 = array.getDrawable(4);
		icons[4] = d4;
		
		if (customAppIcon4 != null) {
			icons[5] = customAppIcon4;
		} else {
			Drawable d5 = array.getDrawable(5);
			icons[5] = d5;
		}
		
		if (customAppIcon5 != null) {
			icons[6] = customAppIcon5;
		} else {
			Drawable d6 = array.getDrawable(6);
			icons[6] = d6;
		}
		
		if (customAppIcon6 != null) {
			icons[7] = customAppIcon6;
		} else {
			Drawable d7 = array.getDrawable(7);
			icons[7] = d7;
		}

		return icons;
	}

	/**
	 * @param context
	 *            Used to setup the view.
	 * @param configuration
	 *            The current configuration. Used to use when selecting layout,
	 *            etc.
	 * @param lockPatternUtils
	 *            Used to know the state of the lock pattern settings.
	 * @param updateMonitor
	 *            Used to register for updates on various keyguard related
	 *            state, and query the initial state at setup.
	 * @param callback
	 *            Used to communicate back to the host keyguard view.
	 */
	LockScreen(Context context, Configuration configuration,
			LockPatternUtils lockPatternUtils,
			KeyguardUpdateMonitor updateMonitor, KeyguardScreenCallback callback) {
		super(context);
		mLockPatternUtils = lockPatternUtils;
		mUpdateMonitor = updateMonitor;
		mCallback = callback;

		mEnableMenuKeyInLockScreen = shouldEnableMenuKey();

		mCreationOrientation = configuration.orientation;

		mKeyboardHidden = configuration.hardKeyboardHidden;

		if (mCustomAppIcon == null)
			mCustomAppIcon = BitmapFactory.decodeResource(getContext()
					.getResources(), R.drawable.ic_jog_dial_custom);

		if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
			Log.v(TAG, "***** CREATING LOCK SCREEN", new RuntimeException());
			Log.v(TAG, "Cur orient=" + mCreationOrientation + " res orient="
					+ context.getResources().getConfiguration().orientation);
		}

		final LayoutInflater inflater = LayoutInflater.from(context);
		if (DBG)
			Log.v(TAG, "Creation orientation = " + mCreationOrientation);
		if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
			if (mUseSlider)
				if (stupidFix)
				inflater.inflate(R.layout.keyguard_screen_slider_unlock_new, this,
						true);
				else
					inflater.inflate(R.layout.keyguard_screen_slider_unlock, this,
							true);
			else if (mUseRotary || mRotaryRevamp)
				if (stupidFix)
				    inflater.inflate(R.layout.keyguard_screen_rotary_unlock_new, this,
						    true);
				else
					inflater.inflate(R.layout.keyguard_screen_rotary_unlock, this,
						    true);
			else if (mUseHoneyComb)
				if (stupidFix)
				    inflater.inflate(R.layout.keyguard_screen_honeycomb_unlock_new,
						    this, true);
				else
					inflater.inflate(R.layout.keyguard_screen_honeycomb_unlock,
							this, true);
			else if (mUseRings)
				if (stupidFix)
				    inflater.inflate(R.layout.keyguard_screen_ring_unlock_new, this,
						    true);
				else
					inflater.inflate(R.layout.keyguard_screen_ring_unlock, this,
						    true);
			else if (mUseSense)
				if (stupidFix)
				    inflater.inflate(R.layout.keyguard_screen_sense_unlock_new, this,
						    true);
				else
					inflater.inflate(R.layout.keyguard_screen_sense_unlock, this,
							true);
			else
				if (stupidFix)
				    inflater.inflate(R.layout.keyguard_screen_tab_unlock_new, this,
						    true);
				else
					inflater.inflate(R.layout.keyguard_screen_tab_unlock, this,
							true);
		} else {
			if (mUseSlider)
				inflater.inflate(R.layout.keyguard_screen_slider_unlock_land,
						this, true);
			else if (mUseRotary || mRotaryRevamp)
				inflater.inflate(R.layout.keyguard_screen_rotary_unlock_land,
						this, true);
			else if (mUseHoneyComb)
				inflater.inflate(
						R.layout.keyguard_screen_honeycomb_unlock_land, this,
						true);
			else if (mUseRings)
				inflater.inflate(R.layout.keyguard_screen_ring_unlock_land,
						this, true);
			else if (mUseSense)
				inflater.inflate(R.layout.keyguard_screen_sense_unlock_land,
						this, true);
			else
				inflater.inflate(R.layout.keyguard_screen_tab_unlock_land,
						this, true);
		}

        setBackground(mContext, (ViewGroup) findViewById(R.id.root));

		mStatusViewManager = new KeyguardStatusViewManager(this,
				mUpdateMonitor, mLockPatternUtils, mCallback, false);

		setFocusable(true);
		setFocusableInTouchMode(true);
		setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		mSilentMode = isSilentMode();

		mUnlockWidget = findViewById(R.id.unlock_widget);
		mUnlockWidget2 = findViewById(R.id.unlock_widget2);

		if (mUnlockWidget instanceof SlidingTab) {
			SlidingTab slidingTabView = (SlidingTab) mUnlockWidget;
			slidingTabView.setHoldAfterTrigger(true, false);
			slidingTabView.setLeftHintText(R.string.lockscreen_unlock_label);
			slidingTabView.setLeftTabResources(R.drawable.ic_jog_dial_unlock,
					R.drawable.jog_tab_target_green,
					R.drawable.jog_tab_bar_left_unlock,
					R.drawable.jog_tab_left_unlock);
			SlidingTabMethods slidingTabMethods = new SlidingTabMethods(
					slidingTabView);
			slidingTabView.setOnTriggerListener(slidingTabMethods);
			mUnlockWidgetMethods = slidingTabMethods;
			SlidingTab slidingTabView2 = (SlidingTab) mUnlockWidget2;
			slidingTabView2.setHoldAfterTrigger(true, false);
			slidingTabView2.setLeftHintText(R.string.zzlockscreen_phone_label);
			slidingTabView2.setLeftTabResources(R.drawable.ic_jog_dial_answer,
					R.drawable.jog_tab_target_green,
					R.drawable.jog_tab_bar_left_generic,
					R.drawable.jog_tab_left_generic);
			slidingTabView2
					.setRightHintText(R.string.zzlockscreen_custom_label);
			slidingTabView2.setRightTabResources(R.drawable.ic_jog_dial_custom,
					R.drawable.jog_tab_target_green,
					R.drawable.jog_tab_bar_right_generic,
					R.drawable.jog_tab_right_generic);
			SlidingTabMethods2 slidingTabMethods2 = new SlidingTabMethods2(
					slidingTabView2);
			slidingTabView2.setOnTriggerListener(slidingTabMethods2);
			mUnlockWidgetMethods2 = slidingTabMethods2;
			if (mLockscreenCustom)
				slidingTabView2.setVisibility(View.VISIBLE);
			else
				slidingTabView2.setVisibility(View.GONE);
		} else if (mUnlockWidget instanceof RotarySelector) {
			RotarySelector rotarySelectorView = (RotarySelector) mUnlockWidget;
			if (!mRotaryDown) {
				rotarySelectorView
						.setLeftHandleResource(R.drawable.ic_jog_dial_unlock);
				rotarySelectorView.setMidHandleResource(mCustomAppIcon);
			} else {
				rotarySelectorView.setLeftHandleResource(mCustomAppIcon);
				rotarySelectorView
						.setMidHandleResource(R.drawable.ic_jog_dial_unlock);
			}
			rotarySelectorView.enableCustomAppDimple(mRotaryRevamp);
			rotarySelectorView.setRevamped(mRotaryRevamp);
			if (mHideArrows)
				rotarySelectorView.hideArrows(true);
			RotarySelectorMethods rotarySelectorMethods = new RotarySelectorMethods(
					rotarySelectorView);
			rotarySelectorView.setOnDialTriggerListener(rotarySelectorMethods);
			mUnlockWidgetMethods = rotarySelectorMethods;
		} else if (mUnlockWidget instanceof WaveView) {
			WaveView waveView = (WaveView) mUnlockWidget;
			WaveViewMethods waveViewMethods = new WaveViewMethods(waveView);
			waveView.setOnTriggerListener(waveViewMethods);
			mUnlockWidgetMethods = waveViewMethods;
		} else if (mUnlockWidget instanceof MultiWaveView) {
			PackageManager pm = context.getPackageManager();
			float density = getResources().getDisplayMetrics().density;
			final Resources resources = context.getResources();
			int iconSize = 48;
			if (mLockscreenCustom) {
				if (mCustomOne != null) {
					try {
						Intent i = Intent.parseUri(mCustomOne, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon1 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomOne);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomOne);
				    }
				}
				
				if (mCustomTwo != null) {
					try {
						Intent i = Intent.parseUri(mCustomTwo, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon2 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomTwo);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomTwo);
				    }
				}
				
				if (mCustomThree != null) {
					try {
						Intent i = Intent.parseUri(mCustomThree, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon3 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomThree);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomThree);
				    }
				}
				
				if (mCustomFour != null) {
					try {
						Intent i = Intent.parseUri(mCustomFour, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon4 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomFour);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomFour);
				    }
				}
				
				if (mCustomFive != null) {
					try {
						Intent i = Intent.parseUri(mCustomFive, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon5 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomFive);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomFive);
				    }
				}
				
				if (mCustomSix != null) {
					try {
						Intent i = Intent.parseUri(mCustomSix, 0);
						Drawable d = pm.getActivityIcon(i);
						Bitmap bit = ((BitmapDrawable) d).getBitmap();
						Bitmap bitIcon = Bitmap.createScaledBitmap(bit,
								(int) (density * iconSize),
								(int) (density * iconSize), true);
						customAppIcon6 = new BitmapDrawable(context.getResources(),
								bitIcon);
				    } catch (PackageManager.NameNotFoundException e) {
				    	Log.e(TAG, "NameNotFoundException: " + mCustomSix);
				    } catch (URISyntaxException e) {
				    	Log.e(TAG, "URISyntaxException: " + mCustomSix);
				    }
				}
			}
			MultiWaveView multiWaveView = (MultiWaveView) mUnlockWidget;
			MultiWaveViewMethods multiWaveViewMethods = new MultiWaveViewMethods(
					multiWaveView);
			multiWaveView.setOnTriggerListener(multiWaveViewMethods);
			mUnlockWidgetMethods = multiWaveViewMethods;
		} else if (mUnlockWidget instanceof RingSelector) {
			RingSelector ringSelectorView = (RingSelector) mUnlockWidget;
			float density = getResources().getDisplayMetrics().density;
			int ringAppIconSize = context.getResources().getInteger(
					R.integer.config_ringSecIconSizeDIP);
			for (int q = 0; q < 4; q++) {
				if (mCustomRingAppActivities[q] != null) {
					ringSelectorView.showSecRing(q);
					try {
						Intent i = Intent.parseUri(mCustomRingAppActivities[q],
								0);
						PackageManager pm = context.getPackageManager();
						ActivityInfo ai = i.resolveActivityInfo(pm,
								PackageManager.GET_ACTIVITIES);
						if (ai != null) {
							Bitmap iconBmp = ((BitmapDrawable) ai.loadIcon(pm))
									.getBitmap();
							mCustomRingAppIcons[q] = Bitmap.createScaledBitmap(
									iconBmp, (int) (density * ringAppIconSize),
									(int) (density * ringAppIconSize), true);
							ringSelectorView.setSecRingResources(q,
									mCustomRingAppIcons[q],
									R.drawable.jog_ring_secback_normal);
						}
					} catch (URISyntaxException e) {
					}
				} else {
					ringSelectorView.hideSecRing(q);
				}
			}
			ringSelectorView.enableMiddleRing(mLockscreenCustom);
			ringSelectorView.setLeftRingResources(
					R.drawable.ic_jog_dial_unlock,
					R.drawable.jog_tab_target_green,
					R.drawable.jog_ring_ring_green);
			ringSelectorView.setMiddleRingResources(
					R.drawable.ic_jog_dial_custom,
					R.drawable.jog_tab_target_green,
					R.drawable.jog_ring_ring_green);
			RingSelectorMethods ringSelectorMethods = new RingSelectorMethods(
					ringSelectorView);
			ringSelectorView.setOnRingTriggerListener(ringSelectorMethods);
			mUnlockWidgetMethods = ringSelectorMethods;
		} else if (mUnlockWidget instanceof SenseLikeLock) {
			SenseLikeLock senseLikeLockView = (SenseLikeLock) mUnlockWidget;
			setupSenseLikeRingShortcuts(context, senseLikeLockView);
			SenseLikeLockMethods senseLikeLockMethods = new SenseLikeLockMethods(
					senseLikeLockView);
			senseLikeLockView
					.setOnSenseLikeSelectorTriggerListener(senseLikeLockMethods);
			mUnlockWidgetMethods = senseLikeLockMethods;
		} else {
			throw new IllegalStateException("Unrecognized unlock widget: "
					+ mUnlockWidget);
		}

		// Update widget with initial ring state
		mUnlockWidgetMethods.updateResources();

		if (DBG)
			Log.v(TAG,
					"*** LockScreen accel is "
							+ (mUnlockWidget.isHardwareAccelerated() ? "on"
									: "off"));
	}

    static void setBackground(Context context, ViewGroup layout) {
        String lockBack = Settings.System.getString(context.getContentResolver(), Settings.System.LOCKSCREEN_BACKGROUND);	
        if (lockBack != null) {
            if (!lockBack.isEmpty()) {
                try {
                    layout.setBackgroundColor(Integer.parseInt(lockBack));
                } catch(NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Context settingsContext = context.createPackageContext("com.android.settings", 0);	
                    String wallpaperFile = settingsContext.getFilesDir() + "/lockwallpaper";	
                    Bitmap background = BitmapFactory.decodeFile(wallpaperFile);	
                    layout.setBackgroundDrawable(new BitmapDrawable(background));	
                } catch (NameNotFoundException e) {
                }
            }
        }
    }

	private boolean isSilentMode() {
		return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU && mEnableMenuKeyInLockScreen) {
			mCallback.goToUnlockScreen();
		}
		return false;
	}

	void updateConfiguration() {
		Configuration newConfig = getResources().getConfiguration();
		if (newConfig.orientation != mCreationOrientation) {
			mCallback.recreateMe(newConfig);
		} else if (newConfig.hardKeyboardHidden != mKeyboardHidden) {
			mKeyboardHidden = newConfig.hardKeyboardHidden;
			final boolean isKeyboardOpen = mKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
			if (mUpdateMonitor.isKeyguardBypassEnabled() && isKeyboardOpen) {
				mCallback.goToUnlockScreen();
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
			Log.v(TAG, "***** LOCK ATTACHED TO WINDOW");
			Log.v(TAG, "Cur orient=" + mCreationOrientation + ", new config="
					+ getResources().getConfiguration());
		}
		updateConfiguration();
	}

	/** {@inheritDoc} */
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
			Log.w(TAG, "***** LOCK CONFIG CHANGING", new RuntimeException());
			Log.v(TAG, "Cur orient=" + mCreationOrientation + ", new config="
					+ newConfig);
		}
		updateConfiguration();
	}

	/** {@inheritDoc} */
	public boolean needsInput() {
		return false;
	}

	/** {@inheritDoc} */
	public void onPause() {
		mStatusViewManager.onPause();
		mUnlockWidgetMethods.reset(false);
	}

	private final Runnable mOnResumePing = new Runnable() {
		public void run() {
			mUnlockWidgetMethods.ping();
		}
	};

	/** {@inheritDoc} */
	public void onResume() {
		mStatusViewManager.onResume();
		postDelayed(mOnResumePing, ON_RESUME_PING_DELAY);
	}

	/** {@inheritDoc} */
	public void cleanUp() {
		mUpdateMonitor.removeCallback(this); // this must be first
		mLockPatternUtils = null;
		mUpdateMonitor = null;
		mCallback = null;
	}

	/** {@inheritDoc} */
	public void onRingerModeChanged(int state) {
		boolean silent = AudioManager.RINGER_MODE_NORMAL != state;
		if (silent != mSilentMode) {
			mSilentMode = silent;
			mUnlockWidgetMethods.updateResources();
		}
	}

	public void onPhoneStateChanged(String newState) {
	}

	public class FastBitmapDrawable extends Drawable {
		private Bitmap mBitmap;
		private int mWidth;
		private int mHeight;

		public FastBitmapDrawable(Bitmap b) {
			mBitmap = b;
			if (b != null) {
				mWidth = mBitmap.getWidth();
				mHeight = mBitmap.getHeight();
			} else {
				mWidth = mHeight = 0;
			}
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawBitmap(mBitmap, 0.0f, 0.0f, null);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		@Override
		public int getIntrinsicWidth() {
			return mWidth;
		}

		@Override
		public int getIntrinsicHeight() {
			return mHeight;
		}

		@Override
		public int getMinimumWidth() {
			return mWidth;
		}

		@Override
		public int getMinimumHeight() {
			return mHeight;
		}

		public void setBitmap(Bitmap b) {
			mBitmap = b;
			if (b != null) {
				mWidth = mBitmap.getWidth();
				mHeight = mBitmap.getHeight();
			} else {
				mWidth = mHeight = 0;
			}
		}

		public Bitmap getBitmap() {
			return mBitmap;
		}
	}
}
