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

public class SVBar extends ColorBar {

    /*
     * Constants used to save/restore the instance state.
     */
    private static final String STATE_SATURATION = "saturation";
    private static final String STATE_VALUE = "value";


    public SVBar(Context context) {
        super(context);
    }

    public SVBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SVBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(AttributeSet attrs, int defStyle) {
        super.init(attrs, defStyle);

        mBarPointerPosition = (mBarLength / 2) + mBarPointerHaloRadius;

        mPosToValueFactor = 1 / ((float) mBarLength / 2);
        mValueToPosFactor = ((float) mBarLength / 2) / 1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Update variables that depend of mBarLength.
        Point topLeft = getTopLeft();
        if (!isInEditMode()) {
            shader = new LinearGradient(mBarPointerHaloRadius, 0,
                    topLeft.x, topLeft.y, new int[]{
                    0xffffffff, Color.HSVToColor(mHSVColor), 0xff000000},
                    null, Shader.TileMode.CLAMP);
        } else {
            shader = new LinearGradient(mBarPointerHaloRadius, 0,
                    topLeft.x, topLeft.y, new int[]{
                    0xffffffff, 0xff81ff00, 0xff000000}, null,
                    Shader.TileMode.CLAMP);
            Color.colorToHSV(0xff81ff00, mHSVColor);
        }

        mBarPaint.setShader(shader);
        mPosToValueFactor = 1 / ((float) mBarLength / 2);
        mValueToPosFactor = ((float) mBarLength / 2) / 1;
        float[] hsvColor = new float[3];
        Color.colorToHSV(mColor, hsvColor);
        if (hsvColor[1] < hsvColor[2]) {
            mBarPointerPosition = Math.round((mValueToPosFactor * hsvColor[1])
                    + mBarPointerHaloRadius);
        } else {
            mBarPointerPosition = Math
                    .round((mValueToPosFactor * (1 - hsvColor[2]))
                            + mBarPointerHaloRadius + (mBarLength / 2));
        }
        if (isInEditMode()) {
            mBarPointerPosition = (mBarLength / 2) + mBarPointerHaloRadius;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);

        // Convert coordinates to our internal coordinate system
        float dimen;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            dimen = event.getX();
        } else {
            dimen = event.getY();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsMovingPointer = true;
                // Check whether the user pressed on the pointer
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
                        mColor = Color.WHITE;
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
                break;
            case MotionEvent.ACTION_UP:
                mIsMovingPointer = false;
                break;
        }
        return true;
    }

    /**
     * Set the pointer on the bar. With the saturation value.
     *
     * @param saturation float between 0 and 1
     */
    public void setSaturation(float saturation) {
        mBarPointerPosition = Math.round((mValueToPosFactor * saturation)
                + mBarPointerHaloRadius);
        calculateColor(mBarPointerPosition);
        mBarPointerPaint.setColor(mColor);
        // Check whether the Saturation/Value bar is added to the ColorPicker
        // wheel
        if (mPicker != null) {
            mPicker.setNewCenterColor(mColor);
            mPicker.changeOpacityBarColor(mColor);
        }
        invalidate();
    }

    /**
     * Set the pointer on the bar. With the Value value.
     *
     * @param value float between 0 and 1
     */
    public void setValue(float value) {
        mBarPointerPosition = Math.round((mValueToPosFactor * (1 - value))
                + mBarPointerHaloRadius + (mBarLength / 2f));
        calculateColor(mBarPointerPosition);
        mBarPointerPaint.setColor(mColor);
        // Check whether the Saturation/Value bar is added to the ColorPicker
        // wheel
        if (mPicker != null) {
            mPicker.setNewCenterColor(mColor);
            mPicker.changeOpacityBarColor(mColor);
        }
        invalidate();
    }

    @Override
    public void setColor(int color) {
        Point topLeft = getTopLeft();

        Color.colorToHSV(color, mHSVColor);
        shader = new LinearGradient(mBarPointerHaloRadius, 0,
                topLeft.x, topLeft.y, new int[]{Color.WHITE, color, Color.BLACK}, null,
                Shader.TileMode.CLAMP);
        mBarPaint.setShader(shader);
        calculateColor(mBarPointerPosition);
        mBarPointerPaint.setColor(mColor);
        if (mPicker != null) {
            mPicker.setNewCenterColor(mColor);
            if (mPicker.hasOpacityBar())
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
        if (coord > (mBarLength / 2) && (coord < mBarLength)) {
            mColor = Color
                    .HSVToColor(new float[]{
                            mHSVColor[0], 1f, 1 - (mPosToValueFactor * (coord - (mBarLength / 2)))
                    });
        } else if (coord > 0 && coord < mBarLength) {
            mColor = Color.HSVToColor(new float[]{
                    mHSVColor[0], (mPosToValueFactor * coord), 1f
            });
        } else if (coord == (mBarLength / 2)) {
            mColor = Color.HSVToColor(new float[]{
                    mHSVColor[0], 1f, 1f
            });
        } else if (coord <= 0) {
            mColor = Color.WHITE;
        } else if (coord >= mBarLength) {
            mColor = Color.BLACK;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = (Bundle) super.onSaveInstanceState();

        float[] hsvColor = new float[3];
        Color.colorToHSV(mColor, hsvColor);
        if (hsvColor[1] < hsvColor[2]) {
            state.putFloat(STATE_SATURATION, hsvColor[1]);
        } else {
            state.putFloat(STATE_VALUE, hsvColor[2]);
        }

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        Bundle savedState = (Bundle) state;
        setColor(Color.HSVToColor(savedState.getFloatArray(STATE_COLOR)));
        if (savedState.containsKey(STATE_SATURATION)) {
            setSaturation(savedState.getFloat(STATE_SATURATION));
        } else {
            setValue(savedState.getFloat(STATE_VALUE));
        }
    }
}