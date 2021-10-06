/*
 * Copyright 2012 Lars Werkman
 * Copyright 2021 Juraj Antal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larswerkman.holocolorpicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SaturationBar extends ColorBar {

	/*
	 * Constants used to save/restore the instance state.
	 */
	private static final String STATE_SATURATION = "saturation";

	/**
	 * Saturation of the latest entry of the onSaturationChangedListener.
	 */
	private int oldChangedListenerSaturation;

    /**
     * Interface and listener so that changes in SaturationBar are sent
     * to the host activity/fragment
     */
    private OnSaturationChangedListener onSaturationChangedListener;


    public interface OnSaturationChangedListener {
        public void onSaturationChanged(int saturation);
    }

    public void setOnSaturationChangedListener(OnSaturationChangedListener listener) {
        this.onSaturationChangedListener = listener;
    }

    public OnSaturationChangedListener getOnSaturationChangedListener() {
        return this.onSaturationChangedListener;
    }


	public SaturationBar(Context context) {
		super(context);
	}

	public SaturationBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SaturationBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init(AttributeSet attrs, int defStyle) {
    	super.init(attrs, defStyle);

		mPosToValueFactor = 1 / ((float) mBarLength);
		mValueToPosFactor = ((float) mBarLength) / 1;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Update variables that depend of mBarLength.
		Point topLeft = getTopLeft();
		if (!isInEditMode()){
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					topLeft.x, topLeft.y, new int[] {
							Color.WHITE,
							Color.HSVToColor(0xFF, mHSVColor) }, null,
					Shader.TileMode.CLAMP);
		} else {
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					topLeft.x, topLeft.y, new int[] {
							Color.WHITE, 0xff81ff00 }, null, Shader.TileMode.CLAMP);
			Color.colorToHSV(0xff81ff00, mHSVColor);
		}
		
		mBarPaint.setShader(shader);
		mPosToValueFactor = 1 / ((float) mBarLength);
		mValueToPosFactor = ((float) mBarLength) / 1;
		
		float[] hsvColor = new float[3];
		Color.colorToHSV(mColor, hsvColor);
		
		if (!isInEditMode()){
			mBarPointerPosition = Math.round((mValueToPosFactor * hsvColor[1])
					+ mBarPointerHaloRadius);
		} else {
			mBarPointerPosition = mBarLength + mBarPointerHaloRadius;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		getParent().requestDisallowInterceptTouchEvent(true);

		// Convert coordinates to our internal coordinate system
		float dimen;
		if (mOrientation == ORIENTATION_HORIZONTAL) {
			dimen = event.getX();
		}
		else {
			dimen = event.getY();
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		    	mIsMovingPointer = true;
			// Check whether the user pressed on (or near) the pointer
			if (dimen >= (mBarPointerHaloRadius)
					&& dimen <= (mBarPointerHaloRadius + mBarLength)) {
				mBarPointerPosition = Math.round(dimen);
				calculateColor(Math.round(dimen));
				mBarPointerPaint.setColor(mColor);
				invalidate();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mIsMovingPointer) {
				// Move the the pointer on the bar.
				if (dimen >= mBarPointerHaloRadius
						&& dimen <= (mBarPointerHaloRadius + mBarLength)) {
					mBarPointerPosition = Math.round(dimen);
					calculateColor(Math.round(dimen));
					mBarPointerPaint.setColor(mColor);
					if (mPicker != null) {
						mPicker.setNewCenterColor(mColor);
						mPicker.changeValueBarColor(mColor);
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				} else if (dimen < mBarPointerHaloRadius) {
					mBarPointerPosition = mBarPointerHaloRadius;
					mColor = Color.WHITE;
					mBarPointerPaint.setColor(mColor);
					if (mPicker != null) {
						mPicker.setNewCenterColor(mColor);
						mPicker.changeValueBarColor(mColor);
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				} else if (dimen > (mBarPointerHaloRadius + mBarLength)) {
					mBarPointerPosition = mBarPointerHaloRadius + mBarLength;
					mColor = Color.HSVToColor(mHSVColor);
					mBarPointerPaint.setColor(mColor);
					if (mPicker != null) {
						mPicker.setNewCenterColor(mColor);
						mPicker.changeValueBarColor(mColor);
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				}
			}
			if(onSaturationChangedListener != null && oldChangedListenerSaturation != mColor){
	            onSaturationChangedListener.onSaturationChanged(mColor);
	            oldChangedListenerSaturation = mColor;
			}
			break;
		case MotionEvent.ACTION_UP:
			mIsMovingPointer = false;
			break;
		}
		return true;
	}

	@Override
	public void setColor(int color) {
		Point topLeft = getTopLeft();
		
		Color.colorToHSV(color, mHSVColor);
		shader = new LinearGradient(mBarPointerHaloRadius, 0,
				topLeft.x, topLeft.y, new int[] {
						Color.WHITE, color }, null,
				Shader.TileMode.CLAMP);
		mBarPaint.setShader(shader);
		calculateColor(mBarPointerPosition);
		mBarPointerPaint.setColor(mColor);
		if (mPicker != null) {
			mPicker.setNewCenterColor(mColor);
			if(mPicker.hasValueBar())
				mPicker.changeValueBarColor(mColor);
			else if(mPicker.hasOpacityBar())
				mPicker.changeOpacityBarColor(mColor);
		}
		invalidate();
	}

	/**
	 * Set the pointer on the bar. With the opacity value.
	 * 
	 * @param saturation float between 0 and 1
	 */
	public void setSaturation(float saturation) {
		mBarPointerPosition = Math.round((mValueToPosFactor * saturation))
				+ mBarPointerHaloRadius;
		calculateColor(mBarPointerPosition);
		mBarPointerPaint.setColor(mColor);
		if (mPicker != null) {
			mPicker.setNewCenterColor(mColor);
			mPicker.changeValueBarColor(mColor);
			mPicker.changeOpacityBarColor(mColor);
		}
		invalidate();
	}

	/**
	 * Calculate the color selected by the pointer on the bar.
	 *
	 * @param coord Coordinate of the pointer.
	 */
	private void calculateColor(int coord) {
	    coord = coord - mBarPointerHaloRadius;
	    if (coord < 0) {
	    	coord = 0;
	    } else if (coord > mBarLength) {
	    	coord = mBarLength;
	    }
	    mColor = Color.HSVToColor(
                new float[] { mHSVColor[0],(mPosToValueFactor * coord),1f });
    }

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle state = (Bundle) super.onSaveInstanceState();
		
		float[] hsvColor = new float[3];
		Color.colorToHSV(mColor, hsvColor);
		state.putFloat(STATE_SATURATION, hsvColor[1]);

		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);

		Bundle savedState = (Bundle) state;
		setColor(Color.HSVToColor(savedState.getFloatArray(STATE_COLOR)));
		setSaturation(savedState.getFloat(STATE_SATURATION));
	}
}
