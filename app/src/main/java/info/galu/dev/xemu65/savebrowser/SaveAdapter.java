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

package info.galu.dev.xemu65.savebrowser;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by gitGalu on 2017-11-23.
 */

public class SaveAdapter extends BaseAdapter {

    private static final int IMAGE_ALPHA_DELETE = 63;
    private static final int IMAGE_ALPHA_NORMAL = 255;

    private Context context;
    private List<SaveWrapper> items;
    private int width;
    private int height;

    public SaveAdapter(Context context, List<SaveWrapper> items, int width, int height) {
        this.context = context;
        this.items = items;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(0); // not used
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        SaveWrapper wrapper = items.get(i);
        ImageView imgView = new ImageView(context);

        imgView.setImageBitmap(BitmapFactory.decodeFile(items.get(i).getBitmapSrc() + "/" + "screen.png"));
        imgView.setLayoutParams(new Gallery.LayoutParams(width, height));
        imgView.setScaleType(ImageView.ScaleType.FIT_XY);

        if (wrapper.isToDelete()) {
            imgView.setImageAlpha(IMAGE_ALPHA_DELETE);
        } else {
            imgView.setImageAlpha(IMAGE_ALPHA_NORMAL);
        }

        return imgView;

    }
}
