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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.galu.dev.xemu65.machineconfig.MachineConfig;

/**
 * Created by gitGalu on 2017-12-08.
 */

public class ConfigUtils {

    public static final String TAG_STEREO = "[STEREO]";
    public static final String TAG_128K = "[128K]";
    public static final String TAG_192K = "[192K]";
    public static final String TAG_256K = "[256K]";
    public static final String TAG_320K = "[320K]";
    public static final String TAG_BASIC = "[BASIC]";
    public static final String TAG_OS_A = "[REQ OSA]";
    public static final String TAG_OS_B = "[REQ OSB]";
    public static final String TAG_130XE = "(130XE)";

    private ConfigUtils() {
    }

    public static MachineConfig guessMachineConfig(String fileName) {
        Pattern p = Pattern.compile("\\[([^]]+)\\]|\\(([^)]+)\\)");
        Matcher m = p.matcher(fileName);

        MachineConfig cfg = MachineConfig.getDefaultConfig();

        while (m.find()) {
            switch (m.group().toUpperCase()) {
                case TAG_BASIC:
                    cfg.getBasicConfig().setBasicRequired(true);
                    break;
                case TAG_130XE:
                case TAG_128K:
                    cfg.setMemConfig(MachineConfig.MemConfig.RAM_128K);
                    break;
                case TAG_192K:
                case TAG_256K:
                case TAG_320K:
                    cfg.setMemConfig(MachineConfig.MemConfig.RAM_320K);
                    break;
                case TAG_OS_A:
                    cfg.setOsConfig(MachineConfig.OsConfig.OS_OSA);
                    break;
                case TAG_OS_B:
                    cfg.setOsConfig(MachineConfig.OsConfig.OS_OSB);
                    break;
                case TAG_STEREO:
                    cfg.setSoundConfig(MachineConfig.SoundConfig.POKEY_STEREO);
                    break;
            }
        }

        return cfg;
    }

}