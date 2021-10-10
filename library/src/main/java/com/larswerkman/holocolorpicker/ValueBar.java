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

public class ValueBar extends ColorBar {
	/*
	 * Constants used to save/restore the instance state.
	 */
	private static final String STATE_VALUE = "value";

    /**
     * Interface and listener so that changes in ValueBar are sent
     * to the host activity/fragment
     */
    private OnValueChangedListener onValueChangedListener;
    
	/**
     * Value of the latest entry of the onValueChangedListener.
     */
	private int oldChangedListenerValue;


    public interface OnValueChangedListener {
        public void onValueChanged(int value);
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.onValueChangedListener = listener;
    }

    public OnValueChangedListener getOnValueChangedListener() {
        return this.onValueChangedListener;
    }


	public ValueBar(Context context) {
		super(context);
	}

	public ValueBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ValueBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init(AttributeSet attrs, int defStyle) {
    	super.init(attrs, defStyle);

		mBarPointerPosition = mBarPointerHaloRadius;

		mPosToValueFactor = 1 / ((float) mBarLength);
		mValueToPosFactor = ((float) mBarLength) / 1;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Update variables that depend of mBarLength.
		Point topLeft = getTopLeft();
		if (!isInEditMode()) {
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					topLeft.x, topLeft.y,
					new int[] { Color.HSVToColor(0xFF, mHSVColor), Color.BLACK },
					null, Shader.TileMode.CLAMP);
		} else {
			shader = new LinearGradient(mBarPointerHaloRadius, 0,
					topLeft.x, topLeft.y,
					new int[] { 0xff81ff00, Color.BLACK }, null,
					Shader.TileMode.CLAMP);
			Color.colorToHSV(0xff81ff00, mHSVColor);
		}

		mBarPaint.setShader(shader);
		mPosToValueFactor = 1 / ((float) mBarLength);
		mValueToPosFactor = ((float) mBarLength) / 1;

		float[] hsvColor = new float[3];
		Color.colorToHSV(mColor, hsvColor);

		if (!isInEditMode()) {
			mBarPointerPosition = Math
					.round((mBarLength - (mValueToPosFactor * hsvColor[2]))
							+ mBarPointerHaloRadius);
		} else {
			mBarPointerPosition = mBarPointerHaloRadius;
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
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				} else if (dimen < mBarPointerHaloRadius) {
					mBarPointerPosition = mBarPointerHaloRadius;
					mColor = Color.HSVToColor(mHSVColor);
					mBarPointerPaint.setColor(mColor);
					if (mPicker != null) {
						mPicker.setNewCenterColor(mColor);
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				} else if (dimen > (mBarPointerHaloRadius + mBarLength)) {
					mBarPointerPosition = mBarPointerHaloRadius + mBarLength;
					mColor = Color.BLACK;
					mBarPointerPaint.setColor(mColor);
					if (mPicker != null) {
						mPicker.setNewCenterColor(mColor);
						mPicker.changeOpacityBarColor(mColor);
					}
					invalidate();
				}
			}
			if(onValueChangedListener != null && oldChangedListenerValue != mColor){
	            onValueChangedListener.onValueChanged(mColor);
	            oldChangedListenerValue = mColor;
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
						color, Color.BLACK }, null, Shader.TileMode.CLAMP);
		mBarPaint.setShader(shader);
		calculateColor(mBarPointerPosition);
		mBarPointerPaint.setColor(mColor);
		if (mPicker != null) {
			mPicker.setNewCenterColor(mColor);
			if(mPicker.hasOpacityBar())
				mPicker.changeOpacityBarColor(mColor);
		}
		invalidate();
	}

	/**
	 * Set the pointer on the bar. With the opacity value.
	 * 
	 * @param value float between 0 and 1
	 */
	public void setValue(float value) {
		mBarPointerPosition = Math
				.round((mBarLength - (mValueToPosFactor * value))
						+ mBarPointerHaloRadius);
		calculateColor(mBarPointerPosition);
		mBarPointerPaint.setColor(mColor);
		if (mPicker != null) {
			mPicker.setNewCenterColor(mColor);
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
	    mColor = Color.HSVToColor(new float[] { mHSVColor[0],
		    				    mHSVColor[1],
		    				    (float) (1 - (mPosToValueFactor * coord)) });
    }

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle state = (Bundle) super.onSaveInstanceState();

		float[] hsvColor = new float[3];
		Color.colorToHSV(mColor, hsvColor);
		state.putFloat(STATE_VALUE, hsvColor[2]);

		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);

		Bundle savedState = (Bundle) state;
		setColor(Color.HSVToColor(savedState.getFloatArray(STATE_COLOR)));
		setValue(savedState.getFloat(STATE_VALUE));
	}
}
