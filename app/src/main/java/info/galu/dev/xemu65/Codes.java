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
 * Created by gitGalu on 2017-11-22.
 */

public class Codes {

    public final static int REQUEST_SAVE_BROWSER = 5001;

    public final static int REQUEST_FILE_BROWSER = 6001;

    public final static int RESULT_SAVE_BROWSER_OK = 5101;
    public final static int RESULT_SAVE_BROWSER_CANCEL = 5102;

    public final static int RESULT_FILE_BROWSER_OK = 6101;
    public final static int RESULT_FILE_BROWSER_ERROR = 6102;

    public final static String SAVE_STATE_PATH = "SAVE_STATE_PATH";

    public final static String SHARED_PREFS_NAME = "info.galu.dev.xemu65.SHARED_PREFS";
    public final static String PREF_KEY_LAST_DIR = "LAST_DIR";
    public final static String ORIGINAL_ROMS_AVAILABLE = "ATARI_ROMS";
    public final static String PREF_ROM = "pref_rom";
    public final static String PREF_REGION = "pref_region";

    public final static String FILE_PATH = "FILE_PATH";
    public final static String FILE_NAME = "FILE_NAME";

    public final static String BUNDLE_EXTRA_EMU_VIEW_WIDTH = "emuViewWidth";
    public final static String BUNDLE_EXTRA_EMU_VIEW_HEIGHT = "emuViewHeight";

    public final static String BUNDLE_EXTRA_CURRENT_PATH = "currentPath";
    public final static String BUNDLE_EXTRA_CURRENT_FILE = "currentFile";

}
