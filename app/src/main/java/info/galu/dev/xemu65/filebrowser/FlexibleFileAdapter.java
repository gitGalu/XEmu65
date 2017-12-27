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
import java.util.Collections;
import java.util.List;

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

        if (currentDir.getParentFile() != null) {
            currentFiles.add(new FileWrapper(currentDir.getPath(), FileWrapper.Type.PARENT_DIRECTORY, null, selectableBgId));
        }

        File[] files = this.currentDir.listFiles(new FileFilter() {
            public boolean accept(File name) {
                if (name.isDirectory()) {
                    return true;
                }
                // TODO supported file extensions
                return FileUtils.isSupportedExtension(name.getName());
            }
        });

        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    currentFiles.add(new FileWrapper(file.getPath(), FileWrapper.Type.DIRECTORY, null, selectableBgId));
                } else {
                    currentFiles.add(new FileWrapper(file.getPath(), FileWrapper.Type.FILE, getFileExtension(file), selectableBgId));
                }
            }
        }
        Collections.sort(currentFiles);

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