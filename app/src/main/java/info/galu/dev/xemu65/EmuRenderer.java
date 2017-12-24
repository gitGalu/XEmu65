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

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gitGalu on 2017-11-07.
 */
public class EmuRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "EmuRenderer";

    private int _frameret;
    protected int w;
    protected int h;
    protected float scale;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
        if (scale != 0.0f) {
            NativeResize((int) (scale * w), (int) (scale * h));
        } else {
            NativeResize(w, h);
        }
    }

    public void hack(int w, int h, float scale) {
        NativeResize((int) (scale * w), (int) (scale * h));
        this.scale = scale;
        this.w = (int) (scale * w);
        this.h = (int) (scale * h);
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        _frameret = NativeRunFrame();
    }

    private native int NativeRunFrame();

    private native void NativeResize(int w, int h);
}
