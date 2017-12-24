/*
 * androidinput.c - handle touch & keyboard events from android
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

#include <string.h>
#include <pthread.h>

#include "../input.h"
#include "../akey.h"
#include "../pokey.h"

#include "androidinput.h"
#include "keys.inc"


#define KBD_MAXKEYS (1 << 4)
#define KBD_MASK    (KBD_MAXKEYS - 1)

enum {
    PTRSTL = -1,
    PTRJOY = 0,
    PTRTRG,
    MAXPOINTERS
};

static const int derot_lut[2][4] =
        {
                {KEY_RIGHT, KEY_LEFT,  KEY_UP,   KEY_DOWN},    /* derot left */
                {KEY_LEFT,  KEY_RIGHT, KEY_DOWN, KEY_UP}    /* derot right */
        };
UBYTE softjoymap[SOFTJOY_MAXKEYS + SOFTJOY_MAXACTIONS][2] =
        {
                {KEY_LEFT,    INPUT_STICK_LEFT},
                {KEY_RIGHT,   INPUT_STICK_RIGHT},
                {KEY_UP,      INPUT_STICK_FORWARD},
                {KEY_DOWN,    INPUT_STICK_BACK},
                {'2', 0},
                {ACTION_NONE, AKEY_NONE},
                {ACTION_NONE, AKEY_NONE},
                {ACTION_NONE, AKEY_NONE}
        };

static int key_head = 0, key_tail = 0;
static int Android_key_control;
static pthread_mutex_t key_mutex = PTHREAD_MUTEX_INITIALIZER;
static key_last = AKEY_NONE;

static int Android_Keyboard[KBD_MAXKEYS];

UWORD Android_PortStatus;
UBYTE Android_TrigStatus;

void Input_Initialize(void) {
    int i;

    Android_PortStatus = 0xFFFF;
    Android_TrigStatus = 0xF;

    static int Android_key_control;

    for (i = 0; i < KBD_MAXKEYS; Android_Keyboard[i] = AKEY_NONE, i++);
    INPUT_key_consol = INPUT_CONSOL_NONE;
    INPUT_key_shift = FALSE;
    Android_key_control = 0;
}

void Android_SingleKeyPress(int k) {
    if (k == -11 || k == -12) {
        Keyboard_Enqueue(k);
    } else {
        Keyboard_Enqueue(skeyxlat[k]);
        Keyboard_Enqueue(AKEY_NONE);
    }
}

void Android_KeyEvent(int k, int s) {
    int i, shft;

    switch (k) {
        case KEY_SHIFT:
            INPUT_key_shift = (s) ? AKEY_SHFT : 0;
            break;
        case KEY_CONTROL:
            Android_key_control = (s) ? AKEY_CTRL : 0;
            break;
        case KEY_FIRE:
            Android_TrigStatus = Android_TrigStatus & (~(s != 0)) | (s == 0);
            break;
        default:
            if (k >= STATIC_MAXKEYS)
                Log_print("Unmappable key %d", k);
            else {
                if (k == '+' || k == '<' || k == '>' || k == '*')
                    shft = 0;
                else
                    shft == INPUT_key_shift;
                Keyboard_Enqueue((s) ? skeyxlat[k] : AKEY_NONE);

                Log_print("MAPUJE %d",
                          (s) ? (skeyxlat[k] | Android_key_control | shft) : AKEY_NONE);
            }
    }
}


void Android_NativeJoy(int k, int s) {
    if (k == 0) {
        Android_TrigStatus = Android_TrigStatus & (~(s != 0)) | (s == 0);
        return;
    }

    int KEY;
    switch (k) {
        case 1:
            KEY = INPUT_STICK_LEFT;
            break;
        case 2:
            KEY = INPUT_STICK_RIGHT;
            break;
        case 3:
            KEY = INPUT_STICK_FORWARD;
            break;
        case 4:
            KEY = INPUT_STICK_BACK;
            break;
    }

    if (s)
        Android_PortStatus &= 0xFFF0 | KEY;
    else
        Android_PortStatus |= ~KEY;
    return;
}

void Android_NativeSpecial(int key) {
    switch (key) {
        case -1:
            INPUT_key_consol = INPUT_CONSOL_NONE;
            break;
        case 0:
            INPUT_key_consol = INPUT_CONSOL_NONE ^ INPUT_CONSOL_START;
            break;
        case 1:
            INPUT_key_consol = INPUT_CONSOL_NONE ^ INPUT_CONSOL_SELECT;
            break;
        case 2:
            INPUT_key_consol = INPUT_CONSOL_NONE ^ INPUT_CONSOL_OPTION;
            break;
        case 3:
            Keyboard_Enqueue(AKEY_HELP);
            break;
            /* RESET is handled at the overlay update */
        default:
            break;
    }
}


void Keyboard_Enqueue(int key) {
    pthread_mutex_lock(&key_mutex);

    if ((key_head + 1) & KBD_MASK == key_tail)
        key_head = key_tail;        /* on overflow, discard previous keys */
    Android_Keyboard[key_head++] = key;
    key_head &= KBD_MASK;

    pthread_mutex_unlock(&key_mutex);
}

int Keyboard_Dequeue(void) {
    pthread_mutex_lock(&key_mutex);

    if (key_head != key_tail) {
        key_last = Android_Keyboard[key_tail++];
        key_tail &= KBD_MASK;
    }

    pthread_mutex_unlock(&key_mutex);

    return key_last;
}