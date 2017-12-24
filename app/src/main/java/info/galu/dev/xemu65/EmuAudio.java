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

/**
 * Created by gitGalu on 2017-11-07.
 */
public final class EmuAudio extends Thread {
    private static final String TAG = "A800Audio";

    public EmuAudio() {
        NativeOSLSoundInit();
        NativeOSLSound();
        return;
    }

    public void pause(boolean p) {
        NativeOSLSoundPause(p);
        return;
    }

    public void run() {
        NativeOSLSoundPause(false);
        return;
    }

    public void interrupt() {
        NativeOSLSoundExit();
        return;
    }

    private native boolean NativeOSLSound();

    private native void NativeOSLSoundInit();

    private native void NativeOSLSoundExit();

    private native void NativeOSLSoundPause(boolean paused);
}
