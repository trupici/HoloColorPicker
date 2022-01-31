/*
 * Copyright 2012 Lars Werkman
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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class OpacityBar extends ColorBar {
    /*
     * Constants used to save/restore the instance state.
     */
    private static final String STATE_OPACITY = "opacity";

    /**
     * Interface and listener so that changes in OpacityBar are sent
     * to the host activity/fragment
     */
    private OnOpacityChangedListener onOpacityChangedListener;

    /**
     * Opacity of the latest entry of the onOpacityChangedListener.
     */
    private int oldChangedListenerOpacity;

    /**
     * Size of the transparency chessboard square
     */
    private int mBarAlphaSquareSize;

    /**
     * Drawable to draw transparency chessboard pattern on the bar
     */
    private AlphaPatternDrawable mAlphaPatternDrawable;

    public interface OnOpacityChangedListener {
        public void onOpacityChanged(int opacity);
    }

    public void setOnOpacityChangedListener(OnOpacityChangedListener listener) {
        this.onOpacityChangedListener = listener;
    }

    public OnOpacityChangedListener getOnOpacityChangedListener() {
        return this.onOpacityChangedListener;
    }


    public OpacityBar(Context context) {
        super(context);
    }

    public OpacityBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpacityBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(AttributeSet attrs, int defStyle) {
        super.init(attrs, defStyle);

        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ColorBar, defStyle, 0);
        final Resources b = getContext().getResources();

        mBarAlphaSquareSize = a.getDimensionPixelSize(
                R.styleable.ColorBar_bar_transparency_square_size,
                b.getDimensionPixelSize(R.dimen.bar_transparency_square_size));
        a.recycle();

        mPosToValueFactor = 0xFF / ((float) mBarLength);
        mValueToPosFactor = ((float) mBarLength) / 0xFF;

        mAlphaPatternDrawable = new AlphaPatternDrawable(mBarAlphaSquareSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int x1, y1;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            x1 = (mBarLength + mBarPointerHaloRadius);
            y1 = mBarThickness;
        } else {
            x1 = mBarThickness;
            y1 = (mBarLength + mBarPointerHaloRadius);
        }

        // Update variables that depend of mBarLength.
        if (!isInEditMode()) {
            shader = new LinearGradient(mBarPointerHaloRadius, 0,
                    x1, y1, new int[]{
                    Color.HSVToColor(0x00, mHSVColor),
                    Color.HSVToColor(0xFF, mHSVColor)}, null,
                    Shader.TileMode.CLAMP);
        } else {
            shader = new LinearGradient(mBarPointerHaloRadius, 0,
                    x1, y1, new int[]{
                    0x0081ff00, 0xff81ff00}, null, Shader.TileMode.CLAMP);
            Color.colorToHSV(0xff81ff00, mHSVColor);
        }

        mBarPaint.setShader(shader);
        mPosToValueFactor = 0xFF / ((float) mBarLength);
        mValueToPosFactor = ((float) mBarLength) / 0xFF;

        float[] hsvColor = new float[3];
        Color.colorToHSV(mColor, hsvColor);

        if (!isInEditMode()) {
            mBarPointerPosition = Math.round((mValueToPosFactor * Color.alpha(mColor))
                    + mBarPointerHaloRadius);
        } else {
            mBarPointerPosition = mBarLength + mBarPointerHaloRadius;
        }

        mAlphaPatternDrawable.setBounds(
                Math.round(mBarRect.left),
                Math.round(mBarRect.top),
                Math.round(mBarRect.right),
                Math.round(mBarRect.bottom));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the bar.
        mAlphaPatternDrawable.draw(canvas);
        super.onDraw(canvas);
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
                        }
                        invalidate();
                    } else if (dimen < mBarPointerHaloRadius) {
                        mBarPointerPosition = mBarPointerHaloRadius;
                        mColor = Color.TRANSPARENT;
                        mBarPointerPaint.setColor(mColor);
                        if (mPicker != null) {
                            mPicker.setNewCenterColor(mColor);
                        }
                        invalidate();
                    } else if (dimen > (mBarPointerHaloRadius + mBarLength)) {
                        mBarPointerPosition = mBarPointerHaloRadius + mBarLength;
                        mColor = Color.HSVToColor(mHSVColor);
                        mBarPointerPaint.setColor(mColor);
                        if (mPicker != null) {
                            mPicker.setNewCenterColor(mColor);
                        }
                        invalidate();
                    }
                }
                if (onOpacityChangedListener != null && oldChangedListenerOpacity != getOpacity()) {
                    onOpacityChangedListener.onOpacityChanged(getOpacity());
                    oldChangedListenerOpacity = getOpacity();
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
                topLeft.x, topLeft.y, new int[]{
                Color.HSVToColor(0x00, mHSVColor), color}, null,
                Shader.TileMode.CLAMP);
        mBarPaint.setShader(shader);
        calculateColor(mBarPointerPosition);
        mBarPointerPaint.setColor(mColor);
        if (mPicker != null) {
            mPicker.setNewCenterColor(mColor);
        }
        invalidate();
    }

    /**
     * Set the pointer on the bar. With the opacity value.
     *
     * @param opacity float between 0 and 255
     */
    public void setOpacity(int opacity) {
        mBarPointerPosition = Math.round((mValueToPosFactor * opacity))
                + mBarPointerHaloRadius;
        calculateColor(mBarPointerPosition);
        mBarPointerPaint.setColor(mColor);
        if (mPicker != null) {
            mPicker.setNewCenterColor(mColor);
        }
        invalidate();
    }

    /**
     * Get the currently selected opacity.
     *
     * @return The int value of the currently selected opacity.
     */
    public int getOpacity() {
        int opacity = Math
                .round((mPosToValueFactor * (mBarPointerPosition - mBarPointerHaloRadius)));
        return opacity & 0xFF;
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
                Math.round(mPosToValueFactor * coord),
                mHSVColor);
    		if (Color.alpha(mColor) > 250) {
    		    mColor = Color.HSVToColor(mHSVColor);
    		} else if (Color.alpha(mColor) < 5) {
    		    mColor = Color.TRANSPARENT;
    		}
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = (Bundle) super.onSaveInstanceState();
        state.putInt(STATE_OPACITY, getOpacity());
        return state;
    }

    @Override

    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        setOpacity(((Bundle) state).getInt(STATE_OPACITY));
    }

}