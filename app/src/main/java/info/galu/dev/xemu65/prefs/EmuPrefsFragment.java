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

package info.galu.dev.xemu65.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import info.galu.dev.xemu65.Codes;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.FileUtils;
import info.galu.dev.xemu65.util.PermissionUtil;

import static info.galu.dev.xemu65.util.PermissionUtil.REQ_PERM_CODE;
import static info.galu.dev.xemu65.util.PermissionUtil.requiredPermissions;

/**
 * Created by gitGalu on 2017-12-28.
 */
public class EmuPrefsFragment extends PreferenceFragmentCompat {

    private PreferenceCategory prefCategory;
    private Preference scanRomsPreference;
    private Preference romsPreference;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.app_preferences);

        prefCategory = (PreferenceCategory) findPreference("pref_category_atari");
        scanRomsPreference = findPreference("scan_roms");
        romsPreference = findPreference("pref_rom");

        SharedPreferences sp = getActivity().getSharedPreferences(Codes.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        if (!sp.getBoolean(Codes.ORIGINAL_ROMS_AVAILABLE, false)) {
            prefCategory.removePreference(romsPreference);
            scanRomsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.rom_set_message))
                            .setTitle("Scan for ROMs");

                    builder.setPositiveButton("Scan", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            boolean b = requestPermissionIfNeeded(getActivity());
                            if (b) {
                                scanRoms();
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    return false;
                }
            });
        } else {
            prefCategory.removePreference(scanRomsPreference);
        }
    }

    private boolean scanForRom() {
        String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File candidate = new File(dirPath + "/" + "xf25.zip");

        if (candidate.exists()) {
            String tmpDirName = "" +  UUID.randomUUID().toString();
            File tmpDir = new File(getActivity().getCacheDir(), tmpDirName);
            boolean success = tmpDir.mkdir();
            if (success) {
                FileUtils.unZip(tmpDir.getAbsolutePath(), candidate);
                Map<String, String> reqFiles = new HashMap();
                reqFiles.put("ATARIBAS.ROM", "3693c9cb9bf3b41bae1150f7a8264992468fc8c0");
                reqFiles.put("ATARIOSB.ROM", "f1f0741b1d34fb4350cf7cb8ab3b6ea11cdd8174");
                reqFiles.put("ATARIXL.ROM", "ae4f523ba08b6fd59f3cae515a2b2410bbd98f55");

                File targetDir = new File(getActivity().getFilesDir().getPath() + "/" + "atariroms");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                for (Map.Entry<String, String> entry : reqFiles.entrySet()) {
                    File f = new File(tmpDir.getAbsolutePath() + "/" + entry.getKey());
                    File target = new File(getActivity().getFilesDir().getPath() + "/" + "atariroms" + "/" + entry.getKey().toLowerCase());
                    if (f.exists()) {
                        byte[] checksum = FileUtils.checksum(f);
                        String fingerPrint = FileUtils.bytesToHex(
                                checksum);
                        if (!entry.getValue().equals(fingerPrint)) return false;
                        try {
                            FileUtils.copyFile(f, target);
                        } catch (IOException e) {
                            return false;
                        }

                    } else {
                        return false;
                    }
                }
                SharedPreferences sp = getActivity().getSharedPreferences(Codes.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(Codes.ORIGINAL_ROMS_AVAILABLE, true);
                editor.commit();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.permissions_storage_message)
                        .setTitle(R.string.permissions_storage_title);

                builder.setPositiveButton(R.string.permissions_storage_button_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        // ???
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
               scanRoms();
            }
        }
    }

    private void scanRoms() {
        boolean success = scanForRom();
        if (success) {
            prefCategory.removePreference(scanRomsPreference);
            prefCategory.addPreference(romsPreference);
        } else {
            Snackbar snackbar = Snackbar.make(getView(), "ROMs not found.", Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            snackbar.show();
        }
    }

    private boolean requestPermissionIfNeeded(Activity activity) {
        boolean allGranted = PermissionUtil.allPermissionsGranted(activity.getApplicationContext());
        if (!allGranted) {
            requestPermissions(requiredPermissions, REQ_PERM_CODE);
        }
        return allGranted;
    }
}
