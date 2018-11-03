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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;

import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.AnimUtils;

/**
 * Created by gitGalu on 2017-11-10.
 */

public class QuickJoyImpl extends RelativeLayout {

    public static final float TARGET_ALPHA = 0.1f;

    public static final String LABEL_LEFT = "LEFT";
    public static final String LABEL_RIGHT = "RIGHT";
    public static final String LABEL_UP = "UP";
    public static final String LABEL_DOWN = "DOWN";
    public static final String LABEL_FIRE1 = "FIRE";
    public static final String LABEL_START = "START";

    private DigitalJoyCallback callback;

    private DualButton lr;
    private DualButton ud;
    private SingleButton fire1;
    private SingleButton startBtn;

    private boolean isRunning = false;

    public QuickJoyImpl(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.quickjoy, this, true);

        this.setAlpha(TARGET_ALPHA);

        lr = findViewById(R.id.lr);
        ud = findViewById(R.id.ud);
        fire1 = findViewById(R.id.fire1);
        startBtn = findViewById(R.id.startButton);

        lr.setCaption(LABEL_LEFT, LABEL_RIGHT);
        ud.setCaption(LABEL_UP, LABEL_DOWN);
        fire1.setCaption(LABEL_FIRE1);
        startBtn.setCaption(LABEL_START, 24);
    }

    public void configure(DigitalJoyCallback callback, Vibrator hapticFeedback) {
        lr.setOnTouchListener(new DualButtonTouchListener(callback, DigitalJoyDirection.LEFT, DigitalJoyDirection.RIGHT, false, hapticFeedback));
        ud.setOnTouchListener(new DualButtonTouchListener(callback, DigitalJoyDirection.UP, DigitalJoyDirection.DOWN, true, hapticFeedback));
        fire1.setOnTouchListener(new SingleButtonTouchListener(callback, DigitalJoyDirection.FIRE_1, hapticFeedback));
        startBtn.setOnTouchListener(new SingleButtonTouchListener(callback, DigitalJoyDirection.START, hapticFeedback));
    }

    public void showJoy(boolean show) {
        int visibility;
        if (show) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        lr.setVisibility(visibility);
        ud.setVisibility(visibility);
        fire1.setVisibility(visibility);
    }

    public void attract() {
        startAnim();
    }

    private void startAnim() {
        if (this.isRunning) {
            return;
        } else {
            this.isRunning = true;
        }

        ObjectAnimator animFadeIn = AnimUtils.getAnimator(ObjectAnimator.ofFloat(this, "alpha", TARGET_ALPHA, 0.5f, 0.5f, 0.5f, TARGET_ALPHA), new AccelerateDecelerateInterpolator(), 0, 5_000);

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(animFadeIn);

        animSet.start();

        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isRunning = false;
            }
        });
    }
}
