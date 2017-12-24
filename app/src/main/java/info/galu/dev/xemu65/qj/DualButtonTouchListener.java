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

import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by gitGalu on 2017-11-08.
 */


public class DualButtonTouchListener implements View.OnTouchListener {

    private final static int VIBRATION_DURATION = 10;

    private final DigitalJoyCallback callback;
    private DigitalJoyDirection direction1;
    private DigitalJoyDirection direction2;
    private final boolean isVertical;
    private final Vibrator vibra;

    private int currentState;
    private int prevEventType;
    private int prevState;

    public DualButtonTouchListener(DigitalJoyCallback callback, DigitalJoyDirection direction1, DigitalJoyDirection direction2, boolean isVertical) {
        this(callback, direction1, direction2, isVertical, null);
    }

    public DualButtonTouchListener(DigitalJoyCallback callback, DigitalJoyDirection direction1, DigitalJoyDirection direction2, boolean isVertical, Vibrator vibra) {
        super();
        this.callback = callback;
        this.direction1 = direction1;
        this.direction2 = direction2;
        this.isVertical = isVertical;
        this.vibra = vibra;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int actionType = event.getAction();

        if (actionType == MotionEvent.ACTION_DOWN || actionType == MotionEvent.ACTION_MOVE) {
            if (isVertical) {
                float sY = v.getMeasuredHeight() / 2;
                float tY = event.getY();

                if (tY < sY) {
                    currentState = 1;
                } else if (tY >= sY) {
                    currentState = 2;
                }
            } else {
                float sX = v.getMeasuredWidth() / 2;
                float tX = event.getX();

                if (tX < sX) {
                    currentState = 1;
                } else if (tX >= sX) {
                    currentState = 2;
                }
            }
        } else if (actionType == MotionEvent.ACTION_UP) {
            currentState = 0;
        }

        if (currentState == prevState && actionType == prevEventType) {
            return true;
        } else {
            prevEventType = actionType;
        }

        if (vibra != null) {
            vibra.vibrate(VIBRATION_DURATION);
        }

        switch (currentState) {
            case 0:
                if (prevState == 1) {
                    callback.stateChanged(direction1, DigitalJoyState.LEAVE);
                } else if (prevState == 2) {
                    callback.stateChanged(direction2, DigitalJoyState.LEAVE);
                }
                prevState = currentState;
                return true;
            case 1:
                if (prevState == 2) {
                    callback.stateChanged(direction2, DigitalJoyState.LEAVE);
                }
                callback.stateChanged(direction1, DigitalJoyState.ENTER);
                prevState = currentState;
                return true;
            case 2:
                if (prevState == 1) {
                    callback.stateChanged(direction1, DigitalJoyState.LEAVE);
                }
                callback.stateChanged(direction2, DigitalJoyState.ENTER);
                prevState = currentState;
                return true;
        }
        return true;
    }

}
