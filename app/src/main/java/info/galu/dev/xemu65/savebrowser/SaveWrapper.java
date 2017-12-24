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

package info.galu.dev.xemu65.savebrowser;

import java.io.File;

/**
 * Created by gitGalu on 2017-11-23.
 */

public class SaveWrapper {
    private String bitmapSrc;
    private String desc;
    private File sourceFile;
    private boolean toDelete = false;

    public SaveWrapper(String bitmapSrc, String desc, File sourceFile) {
        this.bitmapSrc = bitmapSrc;
        this.desc = desc;
        this.sourceFile = sourceFile;
    }

    public String getBitmapSrc() {
        return bitmapSrc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isToDelete() {
        return toDelete;
    }

    public void setToDelete(boolean b) {
        this.toDelete = b;
    }

    public File getSourceFile() {
        return sourceFile;
    }
}
