/*
 * platform.c - platform interface implementation for android
 *
 * Copyright (C) 2017 Michal Galinski
 * Copyright (C) 2010 Kostas Nakos
 * Copyright (C) 2010 Atari800 development team (see DOC/CREDITS)
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

#include "../atari.h"
#include "../cpu.h"
#include "../input.h"
#include "../devices.h"

#include "graphics.h"
#include "androidinput.h"

int PLATFORM_Initialise(int *argc, char *argv[]) {
    /* Android_InitGraphics() is deferred until GL surface is created */
    /* Sound_Initialise() not needed */
    Log_print("Core init");

    Input_Initialize();

    Devices_enable_h_patch = FALSE;
    INPUT_direct_mouse = TRUE;

    return TRUE;
}

int PLATFORM_Exit(int run_monitor) {
    if (CPU_cim_encountered) {
        Log_print("CIM encountered");
        return TRUE;
    }

    Log_print("Core_exit");

    Android_ExitGraphics();

    return FALSE;
}

int PLATFORM_Keyboard(void) {
    return Keyboard_Dequeue();
}

void PLATFORM_DisplayScreen(void) {
    Android_ConvertScreen();
    Android_Render();
}

void PLATFORM_PaletteUpdate(void) {
    Android_PaletteUpdate();
}

int PLATFORM_PORT(int num) {
    return (Android_PortStatus >> (num << 3)) & 0xFF;
}

int PLATFORM_TRIG(int num) {
    return (Android_TrigStatus >> num) & 0x1;
}
