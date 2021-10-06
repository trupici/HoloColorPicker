package com.larswerkman.holocolorpicker;

/*
 * Copyright (C) 2015 Daniel Nilsson
 * Copyright 2021 Juraj Antal
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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * This drawable will draw a simple white and gray chessboard pattern.
 * It's pattern you will often see as a background behind
 * a partly transparent image in many applications.
 *
 * @author Daniel Nilsson
 */
public class AlphaPatternDrawable extends Drawable {

    private final Paint mPaint;
    private final Paint mWhitePaint;
    private final Paint mGrayPaint;
    private final int mSquareSize;

    // alpha pattern caching Bitmap
    private Bitmap mBitmap;

    public AlphaPatternDrawable(int squareSize) {
        mSquareSize = squareSize;

        mPaint = new Paint();

        mWhitePaint = new Paint();
        mWhitePaint.setColor(Color.WHITE);

        mGrayPaint = new Paint();
        mGrayPaint.setColor(Color.GRAY);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (getBounds().isEmpty()) {
            return;
        }

        // recreate bitmap with the pattern

        int numHorizSquares = (int) Math.ceil(bounds.width() / (float)mSquareSize);
        int numVertSquares = (int) Math.ceil(bounds.height() / (float)mSquareSize);

        mBitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);

        Rect rect = new Rect();
        for (int i = 0; i <= numVertSquares; i++) {

            rect.top = i * mSquareSize;
            rect.bottom = rect.top + mSquareSize;

            boolean isWhite = (i & 0x02) == 0;
            for (int j = 0; j <= numHorizSquares; j++) {
                rect.left = j * mSquareSize;
                rect.right = rect.left + mSquareSize;

                canvas.drawRect(rect, isWhite ? mWhitePaint : mGrayPaint);
                isWhite = !isWhite;
            }
        }
    }

}
