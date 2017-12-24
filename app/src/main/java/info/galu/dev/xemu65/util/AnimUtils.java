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

package info.galu.dev.xemu65.util;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.animation.Interpolator;

/**
 * Created by gitGalu on 2017-11-22.
 */
public class AnimUtils {

    private AnimUtils() {
    }

    public static ObjectAnimator getAnimator(ObjectAnimator o, Interpolator interpolator, int startDelay, int duration) {
        o.setStartDelay(startDelay);
        o.setDuration(duration);
        o.setInterpolator(interpolator);
        return o;
    }

    public static ActivityOptionsCompat getActivityTransitionParams(Context context) {
        return ActivityOptionsCompat.makeCustomAnimation(context,
                android.R.anim.fade_in, android.R.anim.fade_out);
    }

}
