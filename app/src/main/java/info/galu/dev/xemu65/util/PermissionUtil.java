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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by gitGalu on 2017-12-28.
 */

public class PermissionUtil {

    public static final String[] requiredPermissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    public static final int REQ_PERM_CODE = 1010;

    private PermissionUtil() {
    }

    public static boolean requestPermissionIfNeeded(Activity activity) {
        boolean allGranted = PermissionUtil.allPermissionsGranted(activity.getApplicationContext());
        if (!allGranted) {
            ActivityCompat.requestPermissions(activity,
                    requiredPermissions, REQ_PERM_CODE);
        }
        return allGranted;
    }

    public static boolean allPermissionsGranted(Context context) {
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
