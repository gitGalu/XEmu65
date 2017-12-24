/*
 * Copyright (C) 2017 Michal Galinski
 *
 * This file is part of XEmu65, an Atari 8-bit computer emulator for Android.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package info.galu.dev.xemu65.qj;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.JoyUtils;

/**
 * Created by gitGalu on 2017-11-08.
 */

public class DualButton extends View {

    private final static Integer LABEL_SIZE = 24;

    private float labelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, LABEL_SIZE, getResources().getDisplayMetrics());

    private boolean vertical;
    private Paint paint;
    private Paint textPaint;
    private int labelAlpha;
    private String caption1;
    private String caption2;
    private boolean isStringAvailable;

    public DualButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DualButton,
                0, 0);
        try {
            vertical = a.getBoolean(R.styleable.DualButton_vertical, false);
            labelAlpha = a.getInt(R.styleable.DualButton_labelAlpha, 127);
        } finally {
            a.recycle();
        }

        preparePaint();
        isStringAvailable = false;
    }

    public void setCaption(String caption1, String caption2) {
        this.isStringAvailable = true;
        this.caption1 = caption1;
        this.caption2 = caption2;
    }

    void preparePaint() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(labelSize);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int u = 5;

        Rect rect;

        if (vertical) {
            rect = new Rect(u, u, w - u, h / 2 - u);
        } else {
            rect = new Rect(u, u, w / 2 - u, h - u);
        }
        RectF rectF = new RectF(rect);
        canvas.drawRoundRect(rectF, labelSize, labelSize, paint);

        if (vertical) {
            rect = new Rect(u, h / 2 + u, w - u, h - u);
        } else {
            rect = new Rect(w / 2 + u, u, w - u, h - u);
        }
        RectF rectF2 = new RectF(rect);
        canvas.drawRoundRect(rectF2, labelSize, labelSize, paint);

        if (isStringAvailable) {
            PointF p = JoyUtils.getTextCenterToDraw("A", rectF, textPaint);
            PointF p2 = JoyUtils.getTextCenterToDraw("A", rectF2, textPaint);
            if (vertical) {
                canvas.drawText(caption1, w / 2, p.y, textPaint);
                canvas.drawText(caption2, w / 2, p2.y, textPaint);
            } else {
                canvas.drawText(caption1, w / 4, p.y, textPaint);
                canvas.drawText(caption2, 3 * w / 4, p2.y, textPaint);
            }
        }
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
        invalidate();
        requestLayout();
    }

    public int getLabelAlpha() {
        return labelAlpha;
    }

    public void setLabelAlpha(int labelAlpha) {
        this.labelAlpha = labelAlpha;
        textPaint.setAlpha(labelAlpha);
        invalidate();
        requestLayout();
    }
}

