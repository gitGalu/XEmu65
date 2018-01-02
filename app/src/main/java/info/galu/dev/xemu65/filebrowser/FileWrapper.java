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

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.util.List;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.FileUtils;

/**
 * Created by gitGalu on 2017-11-22.
 */

public class FileWrapper extends AbstractFlexibleItem<FileWrapper.FileWrapperViewHolder> implements Comparable<FileWrapper> {

    public enum FileExtension {
        ATR, XEX, CAS, NOT_FOUND;
    }

    public enum Type {
        PARENT_DIRECTORY, DIRECTORY, FILE;
    }

    private final String path;
    private final String name;
    private final FileExtension extension;
    private final Type type;
    private boolean isHistoryAvailable = false;
    private final int selectableBgId;

    public FileWrapper(String path, Type type, FileExtension extension, int selectableBgId) {
        this.path = path;
        this.type = type;
        this.extension = extension;
        this.selectableBgId = selectableBgId;
        this.name = new File(path).getName();
    }

    public String getPath() {
        return path;
    }

    public FileExtension getExtension() {
        return extension;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setHistoryAvailable(boolean isHistoryAvailable) {
        this.isHistoryAvailable = isHistoryAvailable;
    }

    public boolean isHistoryAvailable() {
        return isHistoryAvailable;
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof FileWrapper) {
            FileWrapper inItem = (FileWrapper) inObject;
            return this.path.equals(inItem.path) && this.extension.equals(inItem.extension);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode(); //TODO
    }


    @Override
    public int compareTo(FileWrapper that) { //TODO
        if (this.type == that.type) {
            return this.path.compareTo(that.path);
        }
        if (this.type == Type.DIRECTORY && that.type == Type.FILE) {
            return -1;
        }
        if (this.type == Type.FILE && that.type == Type.DIRECTORY) {
            return 1;
        }
        if (this.type == Type.PARENT_DIRECTORY) {
            return -1;
        }
        if (that.type == Type.PARENT_DIRECTORY) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.file_layout;
    }

    @Override
    public FileWrapperViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new FileWrapperViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, FileWrapperViewHolder holder, int position,
                               List payloads) {
        if (type == FileWrapper.Type.PARENT_DIRECTORY) {
            holder.icon.setImageResource(R.drawable.ic_subdirectory_arrow_left_white_24dp);
            holder.label.setText(path);
            holder.itemView.setBackgroundResource(R.drawable.file_wrapper_parent_row);
            return;
        } else {
            holder.itemView.setBackgroundResource(R.drawable.file_wrapper_bg);
            holder.label.setText(formatText());
            holder.historyBtn.setVisibility(isHistoryAvailable ? View.VISIBLE : View.INVISIBLE);
            switch (type) {
                case FILE:
                    switch (extension) {
                        case XEX:
                            holder.icon.setImageResource(R.mipmap.ic_xex);
                            return;
                        case ATR:
                            holder.icon.setImageResource(R.mipmap.ic_atr);
                            return;
                        case CAS:
                            holder.icon.setImageResource(R.mipmap.ic_cas);
                            return;
                    }
                    break;
                case DIRECTORY:
                    holder.icon.setImageResource(R.drawable.ic_folder_white_24dp);
                    return;
            }
        }
    }

    public class FileWrapperViewHolder extends FlexibleViewHolder {
        TextView label;
        ImageView icon;
        ImageButton historyBtn;

        public FileWrapperViewHolder(View view, final FlexibleAdapter adapter) {
            super(view, adapter);
            label = view.findViewById(R.id.textView);
            icon = view.findViewById(R.id.imageView);
            historyBtn = view.findViewById(R.id.historyBtn);
            this.historyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.setPressed(true);
                    FileWrapper item = (FileWrapper) adapter.getItem(getAdapterPosition());
                    ((FlexibleFileAdapter) adapter).goHistory(new File(item.getPath()).getParent(), item.getName());
                }
            });
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FileWrapper item = (FileWrapper) adapter.getItem(getAdapterPosition());
                    switch (item.getType()) {
                        case DIRECTORY:
                            ((FlexibleFileAdapter) adapter).goDirectory(item.getPath());
                            break;
                        case PARENT_DIRECTORY:
                            ((FlexibleFileAdapter) adapter).goDirectory(new File(item.getPath()).getParent());
                            break;
                        case FILE:
                            ((FlexibleFileAdapter) adapter).goFile(item.getPath());
                            break;
                    }
                }
            });
        }
    }

    private String formatText() {
        if (type == Type.FILE) {
            String fileName = new File(path).getName();
            int extensionPos = fileName.lastIndexOf(".");
            if (extensionPos > 0) {
                return fileName.substring(0, extensionPos);
            }
            return fileName;
        } else if (type == Type.DIRECTORY) {
            String fileName = new File(path).getName();
            return fileName;
        }
        return path;
    }

}
