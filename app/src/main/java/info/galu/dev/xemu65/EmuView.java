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

package info.galu.dev.xemu65;

import android.content.Context;
import android.opengl.GLSurfaceView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import info.galu.dev.xemu65.behavior.ShrinkBehavior;

/**
 * Created by gitGalu on 2017-11-08.
 */
@CoordinatorLayout.DefaultBehavior(ShrinkBehavior.class)
public final class EmuView extends GLSurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "EmuView";
    private EmuRenderer renderer;
    private SurfaceHolder surfaceHolder;

    public EmuView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        renderer = new EmuRenderer();
        setRenderer(renderer);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    public void pause(boolean p) {
        setRenderMode(p ? GLSurfaceView.RENDERMODE_WHEN_DIRTY :
                GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public EmuRenderer getRenderer() {
        return this.renderer;
    }

}
