package com.larswerkman.holocolorpicker;

import android.graphics.Paint;

/**
 * Simple Paint extension with restricted paint color option
 */
public class FixedColorPaint extends Paint {
    private final int defaultColor;
    private final boolean useFixedColor;

    public FixedColorPaint(int defaultColor, boolean useFixedColor) {
        this.defaultColor = defaultColor;
        this.useFixedColor = useFixedColor;
        super.setColor(defaultColor);
    }

    @Override
    public void setColor(int color) {
        super.setColor(useFixedColor ? defaultColor : color);
    }
}
