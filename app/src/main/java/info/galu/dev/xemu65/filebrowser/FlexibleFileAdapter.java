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

package info.galu.dev.xemu65.filebrowser;

import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.FileUtils;

/**
 * Created by gitGalu on 2017-11-24.
 */

public class FlexibleFileAdapter<T extends IFlexible> extends FlexibleAdapter {

    private File currentDir;
    private FileBrowser context;
    private List<FileWrapper> items;
    private int selectableBgId;

    FlexibleFileAdapter(FileBrowser context, String initialDir) {
        super(null);
        this.context = context;

        goDirectory(initialDir);

        TypedValue tValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, tValue, true);
        selectableBgId = tValue.resourceId;
    }

    public void goDirectory(String dir) {
        items = new ArrayList();
        items = getDirectory(dir);
        updateDataSet(items);
    }

    public void goHistory(String path, String file) {
        context.goHistory(path, file);
    }

    public void goFile(String file) {
        context.goFile(file);
    }

    private List<FileWrapper> getDirectory(String path) {
        this.currentDir = new File(path);

        List<FileWrapper> currentFiles = new ArrayList<>();

        final List<File> tempDirs = new ArrayList<>();

        if (currentDir.getParentFile() != null) {
            currentFiles.add(new FileWrapper(currentDir.getPath(), FileWrapper.Type.PARENT_DIRECTORY, null, selectableBgId));
        }

        final String[] SUPPORTED_EXTS = {".XEX", ".ATR", ".A8SAV"};

        File[] files = this.currentDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    tempDirs.add(file);
                    return false;
                }
                String nameAllCaps = file.getName().toUpperCase();
                for (String supportedExtension : SUPPORTED_EXTS) {
                    if (nameAllCaps.endsWith(supportedExtension)) return true;
                }
                return false;
            }
        });

        Collections.sort(tempDirs);
        if (files != null) {
            Arrays.sort(files);
        }

        for (File dir : tempDirs) {
            currentFiles.add(new FileWrapper(dir.getPath(), FileWrapper.Type.DIRECTORY, null, selectableBgId));
        }

        if (files != null) {
            boolean hasSaves = false;
            FileWrapper current = null;
            for (File file : files) {
                if (file.getName().toUpperCase().endsWith(".A8SAV")) {
                    if (current != null && hasSaves == false) {
                        String saveFileRegex = "^(" + Pattern.quote(current.getName()) + ").((\\d){13}|(qs(\\d){3}))(.a8sav)$";
                        final Pattern saveFilePattern = Pattern.compile(saveFileRegex);
                        if (saveFilePattern.matcher(file.getName()).matches()) {
                            hasSaves = true;
                            current.setHistoryAvailable(true);
                        }
                    }
                } else {
                    if (current != null) {
                        currentFiles.add(current);
                    }
                    hasSaves = false;
                    current = new FileWrapper(file.getPath(), FileWrapper.Type.FILE, getFileExtension(file), selectableBgId);
                }
            }

            if (current != null) {
                currentFiles.add(current);
            }
        }

        return currentFiles;
    }

    private FileWrapper.FileExtension getFileExtension(File file) {
        String fileName = file.getName();
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        if (fileExt != null) {
            fileExt = fileExt.toUpperCase();
            switch (fileExt) {
                case ".XEX":
                    return FileWrapper.FileExtension.XEX;
                case ".ATR":
                    return FileWrapper.FileExtension.ATR;
            }
        }
        return FileWrapper.FileExtension.NOT_FOUND;
    }

    @Override
    public String onCreateBubbleText(int position) {
        FileWrapper fileWrapper = items.get(position);

        if (fileWrapper.getType() == FileWrapper.Type.DIRECTORY || fileWrapper.getType() == FileWrapper.Type.PARENT_DIRECTORY) {
            return null;
        }
        Character c = fileWrapper.getName().charAt(0);
        if (Character.isDigit(c)) {
            return "#";
        }
        return "" + c;
    }


}