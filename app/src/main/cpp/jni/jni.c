/*
 * jni.c - native functions exported to java
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

#include <stddef.h>
#include <pthread.h>
#include <jni.h>
#include <malloc.h>


#include "../log.h"
#include "../atari.h"
#include "../input.h"
#include "../afile.h"
#include "../screen.h"
#include "../cpu.h"
#include "../antic.h"
#include "../memory.h"	/* override system header */ //../.. gal
#include "../sio.h"
#include "../sysrom.h"
#include "../devices.h"
#include "../cartridge.h"

#include "graphics.h"
#include "androidinput.h"

/* exports/imports */
extern void Android_SoundInit(int rate, int sizems, int bit16, int hq, int disableOSL);

//extern void Android_Joy(int direction, int state);
//extern void Android_Special(int key);
extern void Sound_Exit(void);

extern void Sound_Pause(void);

extern void Sound_Continue(void);

extern int Android_osl_sound;

extern int Screen_SaveScreenshot(const char *filename, int interlaced);

struct audiothread {
    UBYTE *sndbuf;
    jbyteArray sndarray;
};
static pthread_key_t audiothread_data;

static char devb_url[512];

static void JNICALL NativeResize(JNIEnv *env, jobject this, jint w, jint h) {
    Log_print("Screen resize: %dx%d", w, h);
    Android_ScreenW = w;
    Android_ScreenH = h;
    Android_InitGraphics();
}

static void JNICALL NativeClearDevB(JNIEnv *env, jobject this) {
    dev_b_status.ready = FALSE;
    memset(devb_url, 0, sizeof(devb_url));
}

static jstring JNICALL NativeInit(JNIEnv *env, jobject this) {
    int ac = 1;
    char av = '\0';
    char *avp = &av;

    pthread_key_create(&audiothread_data, NULL);
    pthread_setspecific(audiothread_data, NULL);

    NativeClearDevB(env, this);

    Atari800_Initialise(&ac, &avp);

    return (*env)->NewStringUTF(env, Atari800_TITLE);
}

static jobjectArray JNICALL NativeGetDrvFnames(JNIEnv *env, jobject this) {
    jobjectArray arr;
    int i;
    char tmp[FILENAME_MAX + 3], fname[FILENAME_MAX];
    jstring str;

    arr = (*env)->NewObjectArray(env, 4, (*env)->FindClass(env, "java/lang/String"), NULL);
    for (i = 0; i < 4; i++) {
        Util_splitpath(SIO_filename[i], NULL, fname);
        sprintf(tmp, "D%d:%s", i + 1, fname);
        str = (*env)->NewStringUTF(env, tmp);
        (*env)->SetObjectArrayElement(env, arr, i, str);
        (*env)->DeleteLocalRef(env, str);
    }

    return arr;
}

static void JNICALL NativeUnmountAll(JNIEnv *env, jobject this) {
    int i;

    for (i = 1; i <= 4; i++)
        SIO_DisableDrive(i);
}

static jboolean JNICALL NativeIsDisk(JNIEnv *env, jobject this, jstring img) {
    const jbyte *img_utf = NULL;
    int type;

    img_utf = (*env)->GetStringUTFChars(env, img, NULL);
    type = AFILE_DetectFileType(img_utf);
    (*env)->ReleaseStringUTFChars(env, img, img_utf);
    switch (type) {
        case AFILE_ATR:
        case AFILE_ATX:
        case AFILE_XFD:
        case AFILE_ATR_GZ:
        case AFILE_XFD_GZ:
        case AFILE_DCM:
        case AFILE_PRO:
            return JNI_TRUE;
        default:
            return JNI_FALSE;
    }
}

static jboolean JNICALL NativeSaveState(JNIEnv *env, jobject this, jstring fname) {
    const jbyte *fname_utf = NULL;
    int ret;

    fname_utf = (*env)->GetStringUTFChars(env, fname, NULL);
    ret = StateSav_SaveAtariState(fname_utf, "wb", TRUE);
    Log_print("Saved state %s with return %d", fname_utf, ret);
    (*env)->ReleaseStringUTFChars(env, fname, fname_utf);
    return ret;
}


static jboolean JNICALL NativeLoadState(JNIEnv *env, jobject this, jstring fname) {
    const jbyte *fname_utf = NULL;
    int ret;
    fname_utf = (*env)->GetStringUTFChars(env, fname, NULL);
    ret = StateSav_ReadAtariState(fname_utf, "rb");
    Log_print("Loaded state %s with return %d", fname_utf, ret);
    (*env)->ReleaseStringUTFChars(env, fname, fname_utf);
    return ret;
}


static jboolean JNICALL NativeScreenShot(JNIEnv *env, jobject this, jstring fname) {
    const jbyte *fname_utf = NULL;
    int ret;

    fname_utf = (*env)->GetStringUTFChars(env, fname, NULL);
    Screen_SaveScreenshot(fname_utf, 0);
    Log_print("Saved screenshot %s", fname_utf);
    (*env)->ReleaseStringUTFChars(env, fname, fname_utf);
    return ret;
}

static jint JNICALL NativeRunAtariProgram(JNIEnv *env, jobject this,
                                          jstring img, jint drv, jint reboot) {
    static char const *const cart_descriptions[CARTRIDGE_LAST_SUPPORTED + 1] = {
            NULL,
            CARTRIDGE_STD_8_DESC,
            CARTRIDGE_STD_16_DESC,
            CARTRIDGE_OSS_034M_16_DESC,
            CARTRIDGE_5200_32_DESC,
            CARTRIDGE_DB_32_DESC,
            CARTRIDGE_5200_EE_16_DESC,
            CARTRIDGE_5200_40_DESC,
            CARTRIDGE_WILL_64_DESC,
            CARTRIDGE_EXP_64_DESC,
            CARTRIDGE_DIAMOND_64_DESC,
            CARTRIDGE_SDX_64_DESC,
            CARTRIDGE_XEGS_32_DESC,
            CARTRIDGE_XEGS_07_64_DESC,
            CARTRIDGE_XEGS_128_DESC,
            CARTRIDGE_OSS_M091_16_DESC,
            CARTRIDGE_5200_NS_16_DESC,
            CARTRIDGE_ATRAX_DEC_128_DESC,
            CARTRIDGE_BBSB_40_DESC,
            CARTRIDGE_5200_8_DESC,
            CARTRIDGE_5200_4_DESC,
            CARTRIDGE_RIGHT_8_DESC,
            CARTRIDGE_WILL_32_DESC,
            CARTRIDGE_XEGS_256_DESC,
            CARTRIDGE_XEGS_512_DESC,
            CARTRIDGE_XEGS_1024_DESC,
            CARTRIDGE_MEGA_16_DESC,
            CARTRIDGE_MEGA_32_DESC,
            CARTRIDGE_MEGA_64_DESC,
            CARTRIDGE_MEGA_128_DESC,
            CARTRIDGE_MEGA_256_DESC,
            CARTRIDGE_MEGA_512_DESC,
            CARTRIDGE_MEGA_1024_DESC,
            CARTRIDGE_SWXEGS_32_DESC,
            CARTRIDGE_SWXEGS_64_DESC,
            CARTRIDGE_SWXEGS_128_DESC,
            CARTRIDGE_SWXEGS_256_DESC,
            CARTRIDGE_SWXEGS_512_DESC,
            CARTRIDGE_SWXEGS_1024_DESC,
            CARTRIDGE_PHOENIX_8_DESC,
            CARTRIDGE_BLIZZARD_16_DESC,
            CARTRIDGE_ATMAX_128_DESC,
            CARTRIDGE_ATMAX_1024_DESC,
            CARTRIDGE_SDX_128_DESC,
            CARTRIDGE_OSS_8_DESC,
            CARTRIDGE_OSS_043M_16_DESC,
            CARTRIDGE_BLIZZARD_4_DESC,
            CARTRIDGE_AST_32_DESC,
            CARTRIDGE_ATRAX_SDX_64_DESC,
            CARTRIDGE_ATRAX_SDX_128_DESC,
            CARTRIDGE_TURBOSOFT_64_DESC,
            CARTRIDGE_TURBOSOFT_128_DESC,
            CARTRIDGE_ULTRACART_32_DESC,
            CARTRIDGE_LOW_BANK_8_DESC,
            CARTRIDGE_SIC_128_DESC,
            CARTRIDGE_SIC_256_DESC,
            CARTRIDGE_SIC_512_DESC,
            CARTRIDGE_STD_2_DESC,
            CARTRIDGE_STD_4_DESC,
            CARTRIDGE_RIGHT_4_DESC,
            CARTRIDGE_BLIZZARD_32_DESC,
            CARTRIDGE_MEGAMAX_2048_DESC,
            CARTRIDGE_THECART_128M_DESC,
            CARTRIDGE_MEGA_4096_DESC,
            CARTRIDGE_MEGA_2048_DESC,
            CARTRIDGE_THECART_32M_DESC,
            CARTRIDGE_THECART_64M_DESC,
            CARTRIDGE_XEGS_8F_64_DESC,
            CARTRIDGE_ATRAX_128_DESC
    };

    const jbyte *img_utf = NULL;
    int ret = 0, r, kb, i, cnt = 0;
    jclass cls, scls;
    jfieldID fid;
    jobjectArray arr, xarr;
    jstring str;
    char tmp[128];

    if (reboot) {
        NativeUnmountAll(env, this);
        CARTRIDGE_Remove();
    }

    img_utf = (*env)->GetStringUTFChars(env, img, NULL);
    r = AFILE_OpenFile(img_utf, reboot, drv, FALSE);
    if ((r & 0xFF) == AFILE_ROM && (r >> 8) != 0) {
        kb = r >> 8;
        scls = (*env)->FindClass(env, "java/lang/String");
        cls = (*env)->GetObjectClass(env, this);
        fid = (*env)->GetFieldID(env, cls, "_cartTypes", "[[Ljava/lang/String;");
        for (i = 1; i <= CARTRIDGE_LAST_SUPPORTED; i++)
            if (CARTRIDGE_kb[i] == kb) cnt++;
        xarr = (*env)->NewObjectArray(env, 2, scls, NULL);
        arr = (*env)->NewObjectArray(env, cnt, (*env)->GetObjectClass(env, xarr), NULL);
        for (cnt = 0, i = 1; i <= CARTRIDGE_LAST_SUPPORTED; i++)
            if (CARTRIDGE_kb[i] == kb) {
                sprintf(tmp, "%d", i);
                str = (*env)->NewStringUTF(env, tmp);
                (*env)->SetObjectArrayElement(env, xarr, 0, str);
                (*env)->DeleteLocalRef(env, str);
                str = (*env)->NewStringUTF(env, cart_descriptions[i]);
                (*env)->SetObjectArrayElement(env, xarr, 1, str);
                (*env)->DeleteLocalRef(env, str);
                (*env)->SetObjectArrayElement(env, arr, cnt++, xarr);
                (*env)->DeleteLocalRef(env, xarr);
                xarr = (*env)->NewObjectArray(env, 2, scls, NULL);
            }
        (*env)->SetObjectField(env, this, fid, arr);
        ret = -2;
    } else if (r == AFILE_ERROR) {
        Log_print("Cannot start image: %s", img_utf);
        ret = -1;
    } else
        CPU_cim_encountered = FALSE;

    (*env)->ReleaseStringUTFChars(env, img, img_utf);
    return ret;
}

static void JNICALL NativeExit(JNIEnv *env, jobject this) {
    Atari800_Exit(FALSE);
}

static jint JNICALL NativeRunFrame(JNIEnv *env, jobject this) {
    static int old_cim = FALSE;
    int ret = 0;

    do {
        INPUT_key_code = PLATFORM_Keyboard();

        if (!CPU_cim_encountered)
            Atari800_Frame();
        else
            Atari800_display_screen = TRUE;

        if (Atari800_display_screen || CPU_cim_encountered)
            PLATFORM_DisplayScreen();

        if (!old_cim && CPU_cim_encountered)
            ret = 1;

        old_cim = CPU_cim_encountered;
    } while (!Atari800_display_screen);

    if (dev_b_status.ready && devb_url[0] == '\0') if (strlen(dev_b_status.url)) {
        strncpy(devb_url, dev_b_status.url, sizeof(devb_url));
        Log_print("Received b: device URL: %s", devb_url);
        ret |= 2;
    } else
        Log_print("Device b: signalled with zero-length url");

    return ret;
}

static void JNICALL NativePrefGfx(JNIEnv *env, jobject this, int aspect, jboolean bilinear,
                                  int artifact, int frameskip, jboolean collisions, int crophoriz,
                                  int cropvert) {
    Android_Aspect = aspect;
    Android_Bilinear = bilinear;
    ANTIC_artif_mode = artifact;
    ANTIC_UpdateArtifacting();
    if (frameskip == 0) {
        Atari800_refresh_rate = 1;
        Atari800_auto_frameskip = TRUE;
    } else {
        Atari800_auto_frameskip = FALSE;
        Atari800_refresh_rate = frameskip;
    }
    Atari800_collisions_in_skipped_frames = collisions;
    Android_CropScreen[0] = (SCANLINE_LEN - crophoriz) / 2;
    Android_CropScreen[2] = crophoriz;
    Android_CropScreen[1] = SCREEN_HEIGHT - (SCREEN_HEIGHT - cropvert) / 2;
    Android_CropScreen[3] = -cropvert;
    Screen_visible_x1 = SCANLINE_START + Android_CropScreen[0];
    Screen_visible_x2 = Screen_visible_x1 + crophoriz;
    Screen_visible_y1 = SCREEN_HEIGHT - Android_CropScreen[1];
    Screen_visible_y2 = Screen_visible_y1 + cropvert;
}

static jboolean JNICALL NativePrefMachine(JNIEnv *env, jobject this, int nummac, jboolean ntsc) {
    struct tSysConfig {
        int type;
        int ram;
    };
    static const struct tSysConfig machine[] = {
            {Atari800_MACHINE_800,  16}, //0
            {Atari800_MACHINE_800,  48}, //1
            {Atari800_MACHINE_800,  52}, //2
            {Atari800_MACHINE_800,  16}, //3
            {Atari800_MACHINE_800,  48}, //4
            {Atari800_MACHINE_800,  52}, //5
            {Atari800_MACHINE_XLXE, 16},
            {Atari800_MACHINE_XLXE, 64}, //7 - default
            {Atari800_MACHINE_XLXE, 128}, //8
            {Atari800_MACHINE_XLXE, 192}, //9
            {Atari800_MACHINE_XLXE, MEMORY_RAM_320_RAMBO}, //10
            {Atari800_MACHINE_XLXE, MEMORY_RAM_320_COMPY_SHOP},
            {Atari800_MACHINE_XLXE, 576},
            {Atari800_MACHINE_XLXE, 1088},
            {Atari800_MACHINE_5200, 16} //14
    };

    Atari800_SetMachineType(machine[nummac].type);
    MEMORY_ram_size = machine[nummac].ram;
    /* Temporary hack to allow choosing OS rev. A/B and XL/XE features.
       Delete after adding proper support for choosing system settings. */
    if (nummac < 3)
        SYSROM_os_versions[Atari800_MACHINE_800] = ntsc ? SYSROM_A_NTSC : SYSROM_A_PAL;
    else if (nummac >= 3 && nummac < 6)
        /* If no OSB NTSC ROM present, try the "custom" 400/800 ROM. */
        SYSROM_os_versions[Atari800_MACHINE_800] =
                SYSROM_roms[SYSROM_B_NTSC].filename[0] == '\0' ?
                SYSROM_800_CUSTOM :
                SYSROM_B_NTSC;
    else if (Atari800_machine_type == Atari800_MACHINE_XLXE) {
        Atari800_builtin_basic = TRUE;
        Atari800_keyboard_leds = FALSE;
        Atari800_f_keys = FALSE;
        Atari800_jumper = FALSE;
        Atari800_builtin_game = FALSE;
        Atari800_keyboard_detached = FALSE;
    }
    /* End of hack */

    Atari800_SetTVMode(ntsc ? Atari800_TV_NTSC : Atari800_TV_PAL);
    CPU_cim_encountered = FALSE;
    return Atari800_InitialiseMachine();
}

static void JNICALL NativePrefEmulation(JNIEnv *env, jobject this, jboolean basic, jboolean speed,
                                        jboolean disk, jboolean sector, jboolean browser) {
    Atari800_disable_basic = basic;
    Screen_show_atari_speed = speed;
    Screen_show_disk_led = disk;
    Screen_show_sector_counter = sector;
    Devices_enable_b_patch = browser;
    Devices_UpdatePatches();
}

static void JNICALL NativePrefSound(JNIEnv *env, jobject this, int mixrate, int bufsizems,
                                    jboolean sound16bit, jboolean hqpokey, jboolean disableOSL) {
    Android_SoundInit(mixrate, bufsizems, sound16bit, hqpokey, disableOSL);
}

static jboolean JNICALL NativeSetROMPath(JNIEnv *env, jobject this, jstring path) {
    const jbyte *utf = NULL;
    jboolean ret = JNI_FALSE;

    utf = (*env)->GetStringUTFChars(env, path, NULL);
    SYSROM_FindInDir(utf, FALSE);
    Log_print("sysrom %s %d", utf, SYSROM_FindInDir(utf, FALSE));
    ret |= chdir(utf);
    Log_print("sysrom %s %d", utf, SYSROM_FindInDir(utf, FALSE));
    ret |= Atari800_InitialiseMachine();
    (*env)->ReleaseStringUTFChars(env, path, utf);

    return ret;
}

static jboolean JNICALL NativeOSLSound(JNIEnv *env, jobject this) {
    return Android_osl_sound;
}

static jboolean JNICALL NativeOSLSoundPause(JNIEnv *env, jobject this, jboolean pause) {
    if (pause)
        Sound_Pause();
    else
        Sound_Continue();
}

static void JNICALL NativeOSLSoundInit(JNIEnv *env, jobject this) {
    Sound_Initialise(0, NULL);
}

static void JNICALL NativeOSLSoundExit(JNIEnv *env, jobject this) {
    Sound_Exit();
}

static void JNICALL NativeJoy(JNIEnv *env, jobject this, jint direction, jint state) {
    Android_NativeJoy(direction, state);
}

static void JNICALL NativeSpecial(JNIEnv *env, jobject this, jint key) {
    Android_NativeSpecial(key);
}

static void JNICALL NativeKey(JNIEnv *env, jobject this, jint k, jint s) {
    Android_KeyEvent(k, s);
}

static void JNICALL NativeKeySinglePress(JNIEnv *env, jobject this, jint k) {
    Android_SingleKeyPress(k);
}

jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNINativeMethod main_methods[] = {
            {"NativeExit",            "()V",                     NativeExit},
            {"NativeRunAtariProgram", "(Ljava/lang/String;II)I", NativeRunAtariProgram},
            {"NativePrefGfx",         "(IZIIZII)V",              NativePrefGfx},
            {"NativePrefMachine",     "(IZ)Z",                   NativePrefMachine},
            {"NativePrefEmulation",   "(ZZZZZ)V",                NativePrefEmulation},
            {"NativeJoy",             "(II)V",                   NativeJoy},
            {"NativeKey",             "(II)V",                   NativeKey},
            {"NativeKeySinglePress",  "(I)V",                    NativeKeySinglePress},
            {"NativeSpecial",         "(I)V",                    NativeSpecial},
            {"NativePrefSound",       "(IIZZZ)V",                NativePrefSound},
            {"NativeSetROMPath",      "(Ljava/lang/String;)Z",   NativeSetROMPath},
            {"NativeInit",            "()Ljava/lang/String;",    NativeInit},
            {"NativeSaveState",       "(Ljava/lang/String;)Z",   NativeSaveState},
            {"NativeLoadState",       "(Ljava/lang/String;)Z",   NativeLoadState},
            {"NativeScreenShot",      "(Ljava/lang/String;)Z",   NativeScreenShot}
    };
    JNINativeMethod snd_methods[] = {
            {"NativeOSLSound",      "()Z",  NativeOSLSound},
            {"NativeOSLSoundInit",  "()V",  NativeOSLSoundInit},
            {"NativeOSLSoundExit",  "()V",  NativeOSLSoundExit},
            {"NativeOSLSoundPause", "(Z)V", NativeOSLSoundPause},
    };
    JNINativeMethod render_methods[] = {
            {"NativeRunFrame", "()I",   NativeRunFrame},
            {"NativeResize",   "(II)V", NativeResize},
    };

    JNIEnv *env;
    jclass cls;

    if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_2))
        return JNI_ERR;

    cls = (*env)->FindClass(env, "info/galu/dev/xemu65/EmuActivity");
    (*env)->RegisterNatives(env, cls, main_methods, sizeof(main_methods) / sizeof(JNINativeMethod));
    cls = (*env)->FindClass(env, "info/galu/dev/xemu65/EmuAudio");
    (*env)->RegisterNatives(env, cls, snd_methods, sizeof(snd_methods) / sizeof(JNINativeMethod));
    cls = (*env)->FindClass(env, "info/galu/dev/xemu65/EmuRenderer");
    (*env)->RegisterNatives(env, cls, render_methods,
                            sizeof(render_methods) / sizeof(JNINativeMethod));

    return JNI_VERSION_1_2;
}
