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

package info.galu.dev.xemu65.behavior;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ViewFlipper;

/**
 * Created by gitGalu on 2017-11-22.
 */

public class ButtonsBehavior extends CoordinatorLayout.Behavior<ViewFlipper> {

    private int lastBottom = -1;
    private int targetBottom = -1;

    public ButtonsBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, ViewFlipper child, View dependency) {
        if (dependency instanceof AppBarLayout) return true;
        return false;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, ViewFlipper child, View dependency) {
        int currentBottom = dependency.getBottom();

        if (lastBottom == -1) {
            targetBottom = currentBottom;
        }

        if ((currentBottom == 0 && lastBottom == targetBottom) || (currentBottom == targetBottom && lastBottom == 0)) {
            return true;
        }

        int bottom = currentBottom;
        child.setPadding(0, bottom, 0, 0);

        lastBottom = dependency.getBottom();

        return false;
    }
}