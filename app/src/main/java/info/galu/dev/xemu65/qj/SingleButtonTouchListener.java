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

public class SingleButtonTouchListener implements View.OnTouchListener {

    private static final long VIBRATION_DURATION = 10;

    private DigitalJoyDirection direction;

    private int prevState;
    private int prevEventType;
    private int joyState;

    private final DigitalJoyCallback callback;
    private final Vibrator vibra;

    public SingleButtonTouchListener(DigitalJoyCallback callback, DigitalJoyDirection direction) {
        this(callback, direction, null);
    }

    public SingleButtonTouchListener(DigitalJoyCallback callback, DigitalJoyDirection direction, Vibrator vibra) {
        this.callback = callback;
        this.direction = direction;
        this.vibra = vibra;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int actionType = event.getAction();

        if (joyState == prevState && actionType == prevEventType) {
            return true;
        }

        prevEventType = actionType;
        prevState = joyState;

        if (actionType == MotionEvent.ACTION_DOWN
                || actionType == MotionEvent.ACTION_MOVE) {
            joyState = 1;
        } else if (actionType == MotionEvent.ACTION_UP) {
            joyState = 0;
        }

        if (vibra != null && joyState != prevState) {
            vibra.vibrate(VIBRATION_DURATION);
        }

        switch (joyState) {
            case 0:
                callback.stateChanged(direction, DigitalJoyState.LEAVE);
                return true;
            case 1:
                callback.stateChanged(direction, DigitalJoyState.ENTER);
                return true;
        }

        return true;
    }

}
