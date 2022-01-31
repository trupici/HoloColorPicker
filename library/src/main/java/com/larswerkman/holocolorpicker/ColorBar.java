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
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public abstract class ColorBar extends View {

    /*
     * Constants used to save/restore the instance state.
     */
    protected static final String STATE_PARENT = "parent";
    protected static final String STATE_COLOR = "color";
    protected static final String STATE_ORIENTATION = "orientation";

    /**
     * Constants used to identify orientation.
     */
    protected static final boolean ORIENTATION_HORIZONTAL = true;
    protected static final boolean ORIENTATION_VERTICAL = false;

    /**
     * Default orientation of the bar.
     */
    protected static final boolean ORIENTATION_DEFAULT = ORIENTATION_HORIZONTAL;

    /**
     * The thickness of the bar.
     */
    protected int mBarThickness;

    /**
     * The length of the bar.
     */
    protected int mBarLength;
    protected int mPreferredBarLength;

    /**
     * The radius of the pointer.
     */
    protected int mBarPointerRadius;

    /**
     * The radius of the halo of the pointer.
     */
    protected int mBarPointerHaloRadius;

    /**
     * The position of the pointer on the bar.
     */
    protected int mBarPointerPosition;

    /**
     * {@code Paint} instance used to draw the bar.
     */
    protected Paint mBarPaint;

    /**
     * {@code Paint} instance used to draw the pointer.
     */
    protected Paint mBarPointerPaint;

    /**
     * {@code Paint} instance used to draw the halo of the pointer.
     */
    protected Paint mBarPointerHaloPaint;

    /**
     * The rectangle enclosing the bar.
     */
    protected RectF mBarRect = new RectF();

    /**
     * {@code Shader} instance used to fill the shader of the paint.
     */
    protected Shader shader;

    /**
     * {@code true} if the user clicked on the pointer to start the move mode. <br>
     * {@code false} once the user stops touching the screen.
     *
     * @see #onTouchEvent(MotionEvent)
     */
    protected boolean mIsMovingPointer;

    /**
     * The ARGB value of the currently selected color.
     */
    protected int mColor;

    /**
     * An array of floats that can be build into a {@code Color} <br>
     * Where we can extract the color from.
     */
    protected float[] mHSVColor = new float[3];

    /**
     * Factor used to calculate the position to the value on the bar.
     */
    protected float mPosToValueFactor;

    /**
     * Factor used to calculate the value to the postion on the bar.
     */
    protected float mValueToPosFactor;

    /**
     * {@code ColorPicker} instance used to control the ColorPicker.
     */
    protected ColorPicker mPicker = null;

    /**
     * Used to toggle orientation between vertical and horizontal.
     */
    protected boolean mOrientation;

    /**
     * Bar pointer color to use for transparent color
     */
    protected int mBarPointerTransparencyColor;

    /**
     * Default bar color
     */
    protected int mBarPointerDefaultColor;

    /**
     * Set the bar color. <br>
     * <br>
     * Its discouraged to use this method.
     *
     * @param color
     */
    public abstract void setColor(int color);

    public ColorBar(Context context) {
        super(context);
        init(null, 0);
    }

    public ColorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ColorBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    protected void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ColorBar, defStyle, 0);
        final Resources b = getContext().getResources();

        mBarThickness = a.getDimensionPixelSize(
                R.styleable.ColorBar_bar_thickness,
                b.getDimensionPixelSize(R.dimen.bar_thickness));
        mBarLength = a.getDimensionPixelSize(R.styleable.ColorBar_bar_length,
                b.getDimensionPixelSize(R.dimen.bar_length));
        mPreferredBarLength = mBarLength;
        mBarPointerRadius = a.getDimensionPixelSize(
                R.styleable.ColorBar_bar_pointer_radius,
                b.getDimensionPixelSize(R.dimen.bar_pointer_radius));
        mBarPointerHaloRadius = a.getDimensionPixelSize(
                R.styleable.ColorBar_bar_pointer_halo_radius,
                b.getDimensionPixelSize(R.dimen.bar_pointer_halo_radius));
        mOrientation = a.getBoolean(
                R.styleable.ColorBar_bar_orientation_horizontal, ORIENTATION_DEFAULT);
        mBarPointerTransparencyColor = a.getColor(
                R.styleable.ColorBar_bar_pointer_halo_color,
                b.getColor(R.color.bar_pointer_halo_color, null));
        mBarPointerDefaultColor = a.getColor(
                R.styleable.ColorBar_bar_pointer_default_color,
                b.getColor(R.color.bar_pointer_default_color, null));
        a.recycle();

        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint.setShader(shader);

        mBarPointerPosition = mBarLength + mBarPointerHaloRadius;

        mBarPointerHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPointerHaloPaint.setColor(mBarPointerTransparencyColor);

        mBarPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPointerPaint.setColor(mBarPointerDefaultColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int intrinsicSize = mPreferredBarLength
                + (mBarPointerHaloRadius * 2);

        // Variable orientation
        int measureSpec;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            measureSpec = widthMeasureSpec;
        } else {
            measureSpec = heightMeasureSpec;
        }
        int lengthMode = MeasureSpec.getMode(measureSpec);
        int lengthSize = MeasureSpec.getSize(measureSpec);

        int length;
        if (lengthMode == MeasureSpec.EXACTLY) {
            length = lengthSize;
        } else if (lengthMode == MeasureSpec.AT_MOST) {
            length = Math.min(intrinsicSize, lengthSize);
        } else {
            length = intrinsicSize;
        }

        int barPointerHaloRadiusx2 = mBarPointerHaloRadius * 2;
        mBarLength = length - barPointerHaloRadiusx2;
        if (mOrientation == ORIENTATION_VERTICAL) {
            setMeasuredDimension(barPointerHaloRadiusx2,
                    (mBarLength + barPointerHaloRadiusx2));
        } else {
            setMeasuredDimension((mBarLength + barPointerHaloRadiusx2),
                    barPointerHaloRadiusx2);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Fill the rectangle instance based on orientation
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            mBarLength = w - (mBarPointerHaloRadius * 2);
            mBarRect.set(mBarPointerHaloRadius,
                    (mBarPointerHaloRadius - (mBarThickness / 2f)),
                    (mBarLength + (mBarPointerHaloRadius)),
                    (mBarPointerHaloRadius + (mBarThickness / 2f)));
        } else {
            mBarLength = h - (mBarPointerHaloRadius * 2);
            mBarRect.set((mBarPointerHaloRadius - (mBarThickness / 2f)),
                    mBarPointerHaloRadius,
                    (mBarPointerHaloRadius + (mBarThickness / 2f)),
                    (mBarLength + (mBarPointerHaloRadius)));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Draw the bar.
        canvas.drawRect(mBarRect, mBarPaint);

        // Calculate the center of the pointer.
        int cX, cY;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            cX = mBarPointerPosition;
            cY = mBarPointerHaloRadius;
        } else {
            cX = mBarPointerHaloRadius;
            cY = mBarPointerPosition;
        }

        // Draw the pointer halo.
        canvas.drawCircle(cX, cY, mBarPointerHaloRadius, mBarPointerHaloPaint);
        // Draw the pointer.
        canvas.drawCircle(cX, cY, mBarPointerRadius, mBarPointerPaint);
    }

    /**
     * Get the currently selected color.
     *
     * @return The ARGB value of the currently selected color.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Adds a {@code ColorPicker} instance to the bar. <br>
     * <br>
     * WARNING: Don't change the color picker. it is done already when the bar
     * is added to the ColorPicker
     *
     * @param picker
     * @see ColorPicker#addSVBar(SVBar)
     */
    public void setColorPicker(ColorPicker picker) {
        mPicker = picker;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable(STATE_PARENT, superState);
        state.putFloatArray(STATE_COLOR, mHSVColor);
        state.putBoolean(STATE_ORIENTATION, mOrientation);

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;

        Parcelable superState = savedState.getParcelable(STATE_PARENT);
        super.onRestoreInstanceState(superState);

        setColor(Color.HSVToColor(savedState.getFloatArray(STATE_COLOR)));

        mOrientation = savedState.getBoolean(STATE_ORIENTATION, ORIENTATION_DEFAULT);
    }

    protected Point getTopLeft() {
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            return new Point(
                    (mBarLength + mBarPointerHaloRadius),
                    mBarThickness
            );
        } else {
            return new Point(
                    mBarThickness,
                    (mBarLength + mBarPointerHaloRadius)
            );
        }
    }
}