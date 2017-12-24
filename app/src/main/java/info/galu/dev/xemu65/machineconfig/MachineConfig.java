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

package info.galu.dev.xemu65.machineconfig;

/**
 * Created by gitGalu on 2017-12-08.
 */

public class MachineConfig {

    public enum MemConfig {

        RAM_16K(0), RMA_48K(4), RAM_64K(7), RAM_128K(8), RAM_192K(9), RAM_320K(10);

        private int num;

        MemConfig(int num) {
            this.num = num;
        }

        public int getNum() {
            return this.num;
        }
    }

    public enum OsConfig {
        OS_OSA, OS_OSB, OS_XL;
    }

    public enum SoundConfig {
        POKEY_MONO, POKEY_STEREO;
    }

    public class BasicConfig {
        boolean isBasicRequired;

        private BasicConfig() {
            this.isBasicRequired = false;
        }

        public void setBasicRequired(boolean isBasicRequired) {
            this.isBasicRequired = isBasicRequired;
        }

        public boolean isBasicRequired() {
            return isBasicRequired;
        }
    }

    private MemConfig memConfig;
    private OsConfig osConfig;
    private SoundConfig soundConfig;
    private BasicConfig basicConfig;

    private MachineConfig() {
        this.memConfig = MemConfig.RAM_64K;
        this.osConfig = OsConfig.OS_XL;
        this.soundConfig = SoundConfig.POKEY_MONO;
        this.basicConfig = new BasicConfig();
    }

    public static MachineConfig getDefaultConfig() {
        return new MachineConfig();
    }

    public MemConfig getMemConfig() {
        return memConfig;
    }

    public OsConfig getOsConfig() {
        return osConfig;
    }

    public SoundConfig getSoundConfig() {
        return soundConfig;
    }

    public BasicConfig getBasicConfig() {
        return basicConfig;
    }

    public void setMemConfig(MemConfig memConfig) {
        this.memConfig = memConfig;
    }

    public void setOsConfig(OsConfig osConfig) {
        this.osConfig = osConfig;
    }

    public void setSoundConfig(SoundConfig soundConfig) {
        this.soundConfig = soundConfig;
    }

    public void setBasicConfig(BasicConfig basicConfig) {
        this.basicConfig = basicConfig;
    }
}
