/*
 * graphics.c - android drawing
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

#include <malloc.h>
#include <string.h>
#include <GLES/gl.h>
#include <GLES/glext.h>

#include "../atari.h"
#include "../screen.h"
#include "../colours.h"
#include "../akey.h"
#include "../cpu.h"

#include "androidinput.h"
#include "graphics.h"

#define TEXTURE_WIDTH  512
#define TEXTURE_HEIGHT 256

int Android_ScreenW = 0;
int Android_ScreenH = 0;
int Android_Aspect;
int Android_CropScreen[] = {0, SCREEN_HEIGHT, SCANLINE_LEN, -SCREEN_HEIGHT};
static struct RECT screenrect;
static int screenclear;
int Android_Bilinear;

/* graphics conversion */
static UWORD *palette = NULL;
static UWORD *hicolor_screen = NULL;

/* standard gl textures */
enum {
    TEX_SCREEN = 0,
    TEX_OVL,
    TEX_MAXNAMES
};
static GLuint texture[TEX_MAXNAMES];

void Android_PaletteUpdate(void) {
    int i;

    if (!palette) {
        if (!(palette = malloc(256 * sizeof(UWORD)))) {
            Log_print("Cannot allocate memory for palette conversion.");
            return;
        }
    }
    memset(palette, 0, 256 * sizeof(UWORD));

    for (i = 0; i < 256; i++)
        palette[i] = ((Colours_GetR(i) & 0xf8) << 8) |
                     ((Colours_GetG(i) & 0xfc) << 3) |
                     ((Colours_GetB(i) & 0xf8) >> 3);
    /* force full redraw */
    Screen_EntireDirty();
}

int Android_InitGraphics(void) {
    const UWORD poly[] = {0, 16, 24, 16, 32, 0, 8, 0};
    int i, tmp, w, h;
    float tmp2, tmp3;
    struct RECT *r;

    /* Allocate stuff */
    if (!hicolor_screen) {
        if (!(hicolor_screen = malloc(TEXTURE_WIDTH * TEXTURE_HEIGHT * sizeof(UWORD)))) {
            Log_print("Cannot allocate memory for hicolor screen.");
            return FALSE;
        }
    }
    memset(hicolor_screen, 0, TEXTURE_WIDTH * TEXTURE_HEIGHT * sizeof(UWORD));

    /* Setup GL */
    glEnable(GL_TEXTURE_2D);
    glDisable(GL_DEPTH_TEST);
    glGenTextures(TEX_MAXNAMES, texture);
    glPixelStorei(GL_PACK_ALIGNMENT, 8);

    /* overlays texture */
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    /* playfield texture */
    glBindTexture(GL_TEXTURE_2D, texture[TEX_SCREEN]);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
                    Android_Bilinear ? GL_LINEAR : GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                    Android_Bilinear ? GL_LINEAR : GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, Android_CropScreen);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, GL_RGB,
                 GL_UNSIGNED_SHORT_5_6_5, hicolor_screen);

    /* Setup view for console key polygons */
    glLoadIdentity();
    glOrthof(0, Android_ScreenW, Android_ScreenH, 0, 0, 1);
    glEnableClientState(GL_VERTEX_ARRAY);

    glClear(GL_COLOR_BUFFER_BIT);
    /* Finsh GL init with an error check */
    if (glGetError() != GL_NO_ERROR) {
        Log_print("Cannot initialize OpenGL");
        return FALSE;
    }

    /* Aspect correct scaling */
    memset(&screenrect, 0, sizeof(struct RECT));
    if (((Android_ScreenW > Android_ScreenH) + 1) & Android_Aspect) {
        w = Android_CropScreen[2];
        h = -Android_CropScreen[3];
        /* fit horizontally */
        tmp2 = ((float) Android_ScreenW) / ((float) w);
        screenrect.h = tmp2 * h;
        if (screenrect.h > Android_ScreenH) {
            /* fit vertically */
            tmp2 = ((float) Android_ScreenH) / ((float) h);
            screenrect.h = Android_ScreenH;
        }
        screenrect.w = tmp2 * w;
        /* center */
        tmp = (Android_ScreenW - screenrect.r + 1) / 2;
        screenrect.l += tmp;
        h = Android_ScreenH;
        if (Android_ScreenH > Android_ScreenW)
            h >>= 1;    /* assume keyboard takes up half the height in portrait */
        tmp = (h - screenrect.b + 1) / 2;
        if (tmp < 0)
            tmp = 0;
        tmp = (Android_ScreenH - h) + tmp;
        screenrect.t += tmp;
        screenclear = TRUE;
    } else {
        screenrect.t = screenrect.l = 0;
        screenrect.w = Android_ScreenW;
        screenrect.h = Android_ScreenH;
        screenclear = FALSE;
    }

    /* Initialize palette */
    Android_PaletteUpdate();

    return TRUE;
}

void Android_ConvertScreen(void) {
    int x, y;
    UBYTE *src, *src_line;
    UWORD *dst, *dst_line;
#ifdef DIRTYRECT
    UBYTE *dirty, *dirty_line;
#endif

#ifdef DIRTYRECT
    dirty_line = Screen_dirty + SCANLINE_START / 8;
#endif
    src_line = ((UBYTE *) Screen_atari) + SCANLINE_START;
    dst_line = hicolor_screen;

    for (y = 0; y < SCREEN_HEIGHT; y++) {
#ifdef DIRTYRECT
        dirty = dirty_line;
#else
        src = src_line;
        dst = dst_line;
#endif
        for (x = 0; x < SCANLINE_LEN; x += 8) {
#ifdef DIRTYRECT
            if (*dirty) {
                src = src_line + x;
                dst = dst_line + x;
                do {
#endif
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
                    *dst++ = palette[*src++];
#ifdef DIRTYRECT
                    *dirty++ = 0;
                    x += 8;
                } while (*dirty && x < SCANLINE_LEN);
            }
            dirty++;
#endif
        }
#ifdef DIRTYRECT
        dirty_line += SCREEN_WIDTH / 8;
#endif
        src_line += SCREEN_WIDTH;
        dst_line += SCANLINE_LEN;
    }
}

void Android_Render(void) {
    const static int crop_lbl[][4] = {{65, 64, 40, -9},
                                      {65, 24, 40, -9},
                                      {65, 34, 40, -9},
                                      {65, 44, 40, -9},
                                      {65, 54, 40, -9}};
    const static int crop_all[] = {0, 64, 128, -64};
    const struct RECT *r;
    const struct POINT *p;
    int i;

    if (screenclear)
        glClear(GL_COLOR_BUFFER_BIT);

    /* --------------------- playfield --------------------- */
    glBindTexture(GL_TEXTURE_2D, texture[TEX_SCREEN]);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, SCANLINE_LEN, SCREEN_HEIGHT, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, hicolor_screen);
    r = &screenrect;
    glDrawTexiOES(r->l, r->t, 0, r->w, r->h);
    if (glGetError() != GL_NO_ERROR) Log_print("OpenGL error at playfield");

    /* --------------------- overlays --------------------- */
    glEnable(GL_BLEND);            /* enable blending */
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_BLEND);

    glDisable(GL_BLEND);        /* disable blending */
}

void Android_ExitGraphics(void) {
    if (hicolor_screen)
        free(hicolor_screen);
    hicolor_screen = NULL;

    if (palette)
        free(palette);
    palette = NULL;

    glDeleteTextures(TEX_MAXNAMES, texture);
}


GLuint testFunc(void) {
    return texture[TEX_SCREEN];
}