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

import android.content.res.AssetManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by gitGalu on 2017-11-08.
 */

public class FileUtils {

    private static final String FILE_NAME_SAVE = "/save.sav";
    private static final String SLASH = "/";
    private static final String[] SUPPORTED_FILE_EXTENSIONS = {".XEX", ".ATR"};

    private FileUtils() {
    }

    public static boolean copyAssetFolderToDataPath(AssetManager assetManager,
                                                    String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAssetToDataPath(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolderToDataPath(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAssetToDataPath(AssetManager assetManager,
                                               String fromAssetPath, String toPath) {
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public static void zip(String dstPath, File... srcFile) {
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(dstPath);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            int BUFFER = 1024 * 10;
            byte data[] = new byte[BUFFER];

            for (int i = 0; i < srcFile.length; i++) {
                FileInputStream fi = new FileInputStream(srcFile[i]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(srcFile[i].getAbsolutePath().substring(srcFile[i].getAbsolutePath().lastIndexOf(SLASH) + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean unZip(String dstPath, File srcFile) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(srcFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();

                if (ze.isDirectory()) {
                    File fmd = new File(dstPath + SLASH + filename); //??
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(dstPath + SLASH + filename);

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void clearDir(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            FileUtils.deleteDir(file);
        }
    }

    public static String formatSaveFileName(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(FILE_NAME_SAVE);
        return sb.toString();
    }

    public static File[] getSaveStateFiles(String currentPath, String currentFile) {
        String saveFileRegex = "^(" + Pattern.quote(currentFile) + ").((\\d){13}|(qs(\\d){3}))(.a8sav)$";
        final Pattern saveFilePattern = Pattern.compile(saveFileRegex);

        File dir = new File(currentPath);

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return saveFilePattern.matcher(name).matches();
            }
        });
        return foundFiles;
    }

    public static boolean getSaveStateFilesAvailability(String currentPath, String currentFile) {
        File[] saveStateFiles = getSaveStateFiles(currentPath, currentFile);
        if (saveStateFiles != null) {
            return (saveStateFiles.length > 0);
        } else {
            return false;
        }
    }

    public static boolean isSupportedExtension(String name) {
        String nameAllCaps = name.toUpperCase();
        for (String supportedExtension : SUPPORTED_FILE_EXTENSIONS) {
            if (nameAllCaps.endsWith(supportedExtension)) return true;
        }
        return false;
    }

    public static byte[] checksum(File input) {
        try (InputStream in = new FileInputStream(input)) {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}
